package de.unihd.dbs.geoparser.gazetteer.importers;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import javax.persistence.EntityManager;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.vividsolutions.jts.geom.GeometryFactory;
import com.vividsolutions.jts.geom.PrecisionModel;
import com.vividsolutions.jts.io.ParseException;
import com.vividsolutions.jts.io.WKTReader;

import de.unihd.dbs.geoparser.core.GeoparserConfig;
import de.unihd.dbs.geoparser.gazetteer.importers.geonames.FeatureClass;
import de.unihd.dbs.geoparser.gazetteer.importers.geonames.FeatureCode;
import de.unihd.dbs.geoparser.gazetteer.models.Footprint;
import de.unihd.dbs.geoparser.gazetteer.models.Place;
import de.unihd.dbs.geoparser.gazetteer.models.PlaceName;
import de.unihd.dbs.geoparser.gazetteer.models.PlaceName.NameFlag;
import de.unihd.dbs.geoparser.gazetteer.models.PlaceProperty;
import de.unihd.dbs.geoparser.gazetteer.models.PlacePropertyType;
import de.unihd.dbs.geoparser.gazetteer.models.PlaceType;
import de.unihd.dbs.geoparser.gazetteer.models.PlaceTypeAssignment;
import de.unihd.dbs.geoparser.gazetteer.models.Provenance;
import de.unihd.dbs.geoparser.gazetteer.models.Type;
import de.unihd.dbs.geoparser.gazetteer.types.PlaceTypes;
import de.unihd.dbs.geoparser.gazetteer.types.PropertyTypes;
import de.unihd.dbs.geoparser.gazetteer.types.RelationshipTypes;
import de.unihd.dbs.geoparser.gazetteer.util.GazetteerPersistenceManager;
import de.unihd.dbs.geoparser.util.dbconnectors.PostgreSQLConnector;

/**
 * Implementation of an importer for GeoNames data into our gazetteer schema, which is defined by the classes in
 * {@link de.unihd.dbs.geoparser.gazetteer.models}. The GeoNames data is expected to be provided in the database schema
 * described in "/GeoParser/docu/geonames_installation.pdf".
 * <p>
 * Since all places must be stored in the gazetteer before they can be related via relationships, the import needs two
 * steps. First, place are imported via {@link #importPlaces()}. Afterwards, they can be linked via
 * {@link #linkPlaces()}.
 * <p>
 * <b>Notes</b>:
 * <ul>
 * <li>Currently, only provenance for each place is addded, but not for each entity to avoid significantly more
 * memory consumption. For merging different imports, it would be interesting to implement a feature that only adds
 * provenance to places if it is different from the place's provenance)
 * <li>timezone currently not considered (needs to be transfered from string to UTM using extra Geonames table)
 *
 * </ul>
 *
 * @author lrichter
 *
 */
public class GeoNamesImporter implements AutoCloseable {

	// database connection details for the existing GeoNames database
	private static final String CONFIG_GEONAMES_SOURCE = "geonames.source";

	// if true, provenance is added to each entity whose content was retrieved from GeoNames. Else, provenance is only
	// added to the place itself or if the provenance is different to the place provenance
	private static final boolean CREATE_DETAILED_PROVENANCE = false;

	// label used for the aggregation tool when generating provenance for imported data
	private static final String AGGREGATION_TOOL_NAME = GeoNamesImporter.class.getSimpleName();

	// configuration for server-side cursor used for dealing with large queries
	private static final int POSTGRES_QUERY_FETCH_SIZE = 100;

	// query for basic information about places.
	// checking for non-null latitude/longitude values not necessary, since always non-null in GeoNames.
	// we ignore the timezone value, since it causes a lot of duplicate data. if you still want it, use a shape-file
	// like http://efele.net/maps/tz/world/ to check in which time-zone a place is located in
	private static final String geonameStmtBasicStr = "SELECT g.geonameid, g.name, latitude, longitude, population, "
			+ "fclass, fcode, gtopo30, a.admin4 FROM geoname g, admin4codes a WHERE g.geonameid = a.geonameid ";

	// list of alternate name language codes that should be ignored - cf. Internship Report by Ludwig Richter, p.13f.
	// and http://download.geonames.org/export/dump/readme.txt
	// NOTE: we don't ignore "abbr" since we use it as flag for an abbreviated name
	private static final Set<String> altNameLanguagesToIgnore = new HashSet<>(
			Arrays.asList("post", "iata", "icao", "faac", "link"));
	private static final String altNameLanguagesToIgnoreSQLStr = altNameLanguagesToIgnore.stream()
			.map(alternateName -> "'" + alternateName + "'").collect(Collectors.joining(", "));
	private static final String altNameExclusionStr = altNameLanguagesToIgnoreSQLStr.length() > 0
			? "AND isolanguage NOT IN (" + altNameLanguagesToIgnoreSQLStr + ")"
			: "";

	// query for place name aliases for a given place
	private static final String altNameStmtStr = "SELECT alternatename, isolanguage, ispreferredname, isshortname, "
			+ "iscolloquial, ishistoric FROM alternatename WHERE geonameid = ? " + altNameExclusionStr;

	// query for wikipedia links for a given place
	private static final String wikipediaLinkStmtStr = "SELECT url_decode(alternatename) FROM alternatename "
			+ "WHERE geonameid = ? AND isolanguage = 'link' AND alternatename ILIKE '%.wikipedia.org/wiki/%' ";

	// query for wikipedia links for all places of given fclasses and fcodes
	private static final String wikipediaLinksStmtStr =
	// @formatter:off
				"SELECT a.geonameid, url_decode(alternatename) FROM alternatename a, geoname g " +
				"WHERE isolanguage = 'link' AND alternatename ILIKE '%.wikipedia.org/wiki/%' AND g.:fclasses AND g.:fcodes";
	// @formatter:on

	// query for the id of the parent place for a given place
	// for administrative divisions, we only use ADM type hierarchy entries to avoid duplicates.
	// for other features we need to analyze the admin code, since they are not reflected in the hierarchy table:
	// quote from http://download.geonames.org/export/dump/readme.txt: "The relation toponym-adm hierarchy is not
	// included in the file, it can instead be built from the admincodes of the toponym."
	private static final String parentIdFromHiearchyStmtStr = "SELECT parentid FROM hierarchy "
			+ "WHERE childid = ? AND type = 'ADM' ";

	// query for the ids of a list of given eligible parents for all places of given fclasses and fcodes
	// this construct was necessary since querying the ids for each place does not exploit efficient indexes. However,
	// more memory is now required, since we need to store all id-matchings in memory.
	private static final String parentIdsFromAdminCodesStmtStr =
	// @formatter:off
			"SELECT c.geonameid, p.geonameid FROM geoname c, geoname p, admin4codes a1, admin4codes a2 " +
			"WHERE c.:child_fclasses AND c.:child_fcodes AND c.geonameid = a1.geonameid AND a1.admin4 = a2.admin4 " +
			"AND a2.geonameid = p.geonameid AND p.:parent_fclasses AND p.:parent_fcodes ";
	// @formatter:on;

	// query for the ids of a list of given eligible parents for all places of given capital fcodes
	private static final String capitalParentIdsFromAdminCodesStmtStr =
	// @formatter:off
			"SELECT c.geonameid, p.geonameid FROM geoname c, geoname p, admin4codes a " +
			"WHERE c.:child_fclasses AND c.:child_fcodes AND a.geonameid = p.geonameid " +
			"AND p.:parent_fclasses AND p.:parent_fcodes AND a.admin4 LIKE (c.country || '.__...')";
	// @formatter:on;

	// feature codes that we want to import, listed per place-type we currently support
	private static final FeatureCode CONTINENT_ENTITY = FeatureCode.CONT;

	private static final Set<FeatureCode> POLITICAL_ENTITIES = EnumSet.of(
	// @formatter:off
			FeatureCode.PCL, FeatureCode.PCLF, FeatureCode.PCLI, FeatureCode.PCLIX, // independent
			FeatureCode.PCLD, FeatureCode.PCLS, // dependent
			FeatureCode.TERR, // only very few not so representative entries, may be ignore?
			FeatureCode.PCLH  // historical entities
			// FeatureCode.ZN, FeatureCode.ZNB --> special cases which we ignore for now
			// @formatter:on
	);

	private static final Set<FeatureCode> ADMIN_ENTITIES = EnumSet.of(
	// @formatter:off
            FeatureCode.ADM1, FeatureCode.ADM2, FeatureCode.ADM3, FeatureCode.ADM4, FeatureCode.ADM5, FeatureCode.ADMD,
            // historical stuff
            FeatureCode.ADM1H, FeatureCode.ADM2H, FeatureCode.ADM3H, FeatureCode.ADM4H, FeatureCode.ADMDH
			// @formatter:on
	);

	private static final Set<FeatureCode> ADMIN_ENTITY_SEATS = EnumSet.of(
	// @formatter:off
			FeatureCode.PPLA, FeatureCode.PPLA2, FeatureCode.PPLA3, FeatureCode.PPLA4,
			FeatureCode.PPLC, FeatureCode.PPLCH, FeatureCode.PPLG
			// @formatter:on
	);

	private static final EnumSet<FeatureCode> SEAT_PARENTS = EnumSet.of(FeatureCode.ADM1, FeatureCode.ADM2,
			FeatureCode.ADM3, FeatureCode.ADM4);
	private static final EnumSet<FeatureCode> CAPITALS = EnumSet.of(FeatureCode.PPLC, FeatureCode.PPLCH);

	private static final Set<FeatureCode> POPULATED_PLACES = EnumSet.of(
	// @formatter:off
			FeatureCode.PPL, FeatureCode.PPLX, FeatureCode.PPLS, FeatureCode.PPLL,
			FeatureCode.PPLF, FeatureCode.PPLR,
			FeatureCode.PPLQ, FeatureCode.PPLH, FeatureCode.PPLW,
			FeatureCode.STLMT
			// @formatter:on
	);

	private static final Set<FeatureCode> POI_PLACES = EnumSet.of(
	// @formatter:off
			FeatureCode.AIRP,
			FeatureCode.CSTL,
			FeatureCode.HTL
			// @formatter:on
	);

	private static final Logger logger = LoggerFactory.getLogger(GeoNamesImporter.class);
	private static final int loggingVerbosity = 1000;

	// WKT Reader needed for creating geometries from lat/lon coordinates
	private final static WKTReader wktReader = new WKTReader(
			new GeometryFactory(new PrecisionModel(), Footprint.REFERENCE_SYSTEM_SRID));

	// globally available map of place-type names to their place-type object for faster querying
	private final Map<String, Type> allTypes;

	// resources we need to connect to for data processing; resource management done by constructor and close()
	private final EntityManager em; // current JPA session
	private final PostgreSQLConnector connector; // connection to the GeoNames database

	/*
	 * ****************************************************************************************************************
	 * GENERAL CODE
	 * ****************************************************************************************************************
	 */

	/**
	 * Create a {@link GeoNamesImporter} and initialize all required resources.
	 *
	 * @param config the {@link GeoparserConfig} instance containing the connection details for GeoNames
	 * @param entityManager the {@link EntityManager} instance connected to the gazetteer
	 *
	 * @throws Exception if something went wrong while initializing the required resources
	 */
	public GeoNamesImporter(final GeoparserConfig config, final EntityManager entityManager) throws Exception {
		logger.debug("Connecting to GeoNames database");
		connector = new PostgreSQLConnector(
				config.getDBConnectionInfoByLabel(config.getConfigStringByLabel(CONFIG_GEONAMES_SOURCE)));
		connector.connect();
		defineURLDecodeFunction();
		allTypes = new HashMap<>();
		em = entityManager;
	}

	@Override
	public void close() throws Exception {
		logger.debug("Closing connection to the GeoNames database");
		if (connector != null) {
			try {
				undefineURLDecodeFunction();
			}
			finally {
				connector.disconnect();
			}
		}
	}

	private void defineURLDecodeFunction() throws SQLException {
		logger.debug("Creating url_decode method on GeoNames database");
		/*
		 * This method is used when reading a Wikipedia-link from GeoNames, since it provides encoded URLs which aren't
		 * always human readable
		 */
		// source: http://postgres.cz/wiki/PostgreSQL_SQL_Tricks#Function_for_decoding_of_url_code
		// @formatter:off
		try(final Statement statement = connector.getConnection().createStatement()) {
			statement.execute(""
				+ "CREATE OR REPLACE FUNCTION url_decode(input text) RETURNS text "
				+ "LANGUAGE plpgsql IMMUTABLE STRICT AS $$ "
				+ "DECLARE "
				+ " bin bytea = ''; "
				+ "byte text; "
				+ "BEGIN "
				+ " FOR byte IN (select (regexp_matches(input, '(%..|.)', 'g'))[1]) LOOP "
				+ "   IF length(byte) = 3 THEN "
				+ "     bin = bin || decode(substring(byte, 2, 2), 'hex'); "
				+ "   ELSE "
				+ "     bin = bin || byte::bytea; "
				+ "   END IF; "
				+ " END LOOP; "
				+ " byte = convert_from(bin, 'utf8'); "
				+ " RETURN byte; "
				+ "EXCEPTION WHEN others THEN "
				+ "  RAISE NOTICE 'caught error when processing %', input; "
				+ "  RETURN null; "
				+ "END "
				+ "$$;");
		}
		// @formatter:on
	}

	private void undefineURLDecodeFunction() throws SQLException {
		logger.debug("Removing url_decode method from GeoNames database");
		try (final Statement statement = connector.getConnection().createStatement()) {
			statement.execute("DROP FUNCTION IF EXISTS url_decode(text);");
		}
	}

	private static Provenance createProvenance(final Integer geonamesId, final boolean forPlace) {
		logger.trace("Creating provenance for place with geoNamesId " + geonamesId + " (forPlace=" + forPlace + ")");
		if (CREATE_DETAILED_PROVENANCE || forPlace) {
			return new Provenance("www.geonames.org/" + geonamesId, AGGREGATION_TOOL_NAME, null);
		}
		else {
			return null;
		}
	}

	/*
	 * ****************************************************************************************************************
	 * TYPE SPECIFIC CODE
	 * ****************************************************************************************************************
	 */

	public void loadTypes() {
		logger.info("Loading all existing gazetteer Types into memory");
		allTypes.clear();
		final Set<Type> types = new HashSet<>(em.createQuery("FROM Type", Type.class).getResultList());
		types.forEach(type -> allTypes.put(type.getName(), type));
	}

	public void deletePropertyTypesSpecficForGeonamesImport() {
		logger.info("Deleting property types that are specific for geonames import only");
		final Set<Type> types = allTypes.get(PropertyTypes.GEONAMES_SUPPLDATA.typeName).getAllChildren();
		types.add(allTypes.get(PropertyTypes.GEONAMES_SUPPLDATA.typeName));
		final List<Long> typeIds = types.stream().map(type -> type.getId()).collect(Collectors.toList());

		em.getTransaction().begin();
		em.createQuery("DELETE FROM PlaceProperty WHERE type_id IN :type_ids").setParameter("type_ids", typeIds)
				.executeUpdate();
		types.forEach(type -> GazetteerPersistenceManager.removeType(em, type));
		em.getTransaction().commit();
	}

	/*
	 * ****************************************************************************************************************
	 * PLACE SPECIFIC CODE
	 * ****************************************************************************************************************
	 */

	public void importPlaces() throws SQLException, ParseException {
		logger.info("=== importing places ===");
		importContinents();
		importPoliticalEntities();
		importAdminPlaces();
		importAdminEntitySeats();
		importPopulatedPlaces();
		importPOIs();
		logger.info("- DONE -");
	}

	private static String featureCodeEnumToIncludingSQLStr(final Set<FeatureCode> fcodes) {
		return "fcode IN (" + fcodes.stream().map(entity -> "'" + entity.name() + "'").collect(Collectors.joining(", "))
				+ ")";
	}

	private static String featureCodeEnumFeatureClassesToIncludingSQLStr(final Set<FeatureCode> fcodes) {
		final Set<FeatureClass> fclasses = fcodes.stream().map(entity -> entity.getFeatureClass())
				.collect(Collectors.toSet());
		return "fclass IN ("
				+ fclasses.stream().map(entity -> "'" + entity.name() + "'").collect(Collectors.joining(", ")) + ")";
	}

	private void importContinents() throws SQLException, ParseException {
		importPlacesByFeatureCode(new HashSet<>(Arrays.asList(CONTINENT_ENTITY)), false, false, "continents");
	}

	private void importPoliticalEntities() throws SQLException, ParseException {
		importPlacesByFeatureCode(POLITICAL_ENTITIES, false, false, "political entities");
	}

	private void importAdminPlaces() throws SQLException, ParseException {
		importPlacesByFeatureCode(ADMIN_ENTITIES, false, false, "admin places");
	}

	private void importAdminEntitySeats() throws SQLException, ParseException {
		importPlacesByFeatureCode(ADMIN_ENTITY_SEATS, true, true, "admin entity seat places");
	}

	private void importPopulatedPlaces() throws SQLException, ParseException {
		importPlacesByFeatureCode(POPULATED_PLACES, true, false, "populated places");
	}

	private void importPOIs() throws SQLException, ParseException {
		importPlacesByFeatureCode(POI_PLACES, true, false, "POI places");
	}

	private void importPlacesByFeatureCode(final Set<FeatureCode> fcodes, final boolean deriveParentFromAdminLevel,
			final boolean deriveSeatPlace, final String importedDataLabel) throws SQLException, ParseException {
		logger.info("Loading " + importedDataLabel);

		final String fclassesInclusionStr = " AND " + featureCodeEnumFeatureClassesToIncludingSQLStr(fcodes);
		final String fcodeInclusionStr = " AND " + featureCodeEnumToIncludingSQLStr(fcodes);
		final String geonameStmtStr = geonameStmtBasicStr + fclassesInclusionStr + " " + fcodeInclusionStr;

		// type shortcuts...
		// final PlacePropertyType wikiLinkType = (PlacePropertyType)
		// allTypes.get(PropertyTypes.WIKIPEDIA_LINK.typeName);
		final PlacePropertyType adminParentType = (PlacePropertyType) allTypes
				.get(PropertyTypes.GEONAMES_ADMIN_PARENT.typeName);
		final PlacePropertyType seatOfType = (PlacePropertyType) allTypes
				.get(PropertyTypes.GEONAMES_GOV_SEAT_OF.typeName);
		final PlacePropertyType capitalOfType = (PlacePropertyType) allTypes
				.get(PropertyTypes.GEONAMES_CAPITAL_OF.typeName);

		// gather information required before processing each place
		// final Map<Integer, List<String>> wikiLinksPerPlace = getWikiLinksPerPlace(fcodes);
		logger.debug("loading admin parents for places");
		final Map<Integer, List<Integer>> parentIdsPerPlace = getAdminParentIdsFromAdminCodes(fcodes);
		logger.debug("loading admin parents for seats");
		final Map<Integer, List<Integer>> parentIdsPerSeatPlace = deriveSeatPlace
				? getAdminParentIdsFromAdminCodes(
						fcodes.stream().filter(fcode -> !CAPITALS.contains(fcode)).collect(Collectors.toSet()))
				: Collections.emptyMap();
		logger.debug("loading admin parents for capitals");
		final Map<Integer, List<Integer>> parentIdsPerCapitalPlace = deriveSeatPlace
				? getCapitalParentIdsFromAdminCodes()
				: Collections.emptyMap();

		// go for it!
		try (final PreparedStatement geonameStmt = connector.getConnection().prepareStatement(geonameStmtStr);
				final PreparedStatement altnameStmt = connector.getConnection().prepareStatement(altNameStmtStr);
				final PreparedStatement wikipediaLinkStmt = connector.getConnection()
						.prepareStatement(wikipediaLinkStmtStr);
				final PreparedStatement parentIdFromHiearchyStmt = connector.getConnection()
						.prepareStatement(parentIdFromHiearchyStmtStr)) {
			// set the fetch size high so we will collect a lot of data at once!
			geonameStmt.setFetchSize(POSTGRES_QUERY_FETCH_SIZE);
			altnameStmt.setFetchSize(POSTGRES_QUERY_FETCH_SIZE);
			wikipediaLinkStmt.setFetchSize(POSTGRES_QUERY_FETCH_SIZE);
			parentIdFromHiearchyStmt.setFetchSize(POSTGRES_QUERY_FETCH_SIZE);

			logger.debug("starting query for " + importedDataLabel);
			final long startTime = System.currentTimeMillis();
			int count = 0;

			try (final ResultSet rs = geonameStmt.executeQuery()) {
				em.getTransaction().begin();

				while (rs.next()) {
					if (count % loggingVerbosity == 0) {
						logger.debug("loading record #" + count);
					}

					logger.trace("retrieving basic place data");
					final Place newPlace = resultSetToPlace(rs);

					final Integer geonamesId = Integer.valueOf(newPlace
							.getPropertiesByType(PropertyTypes.GEONAMES_ID.typeName).iterator().next().getValue());
					final FeatureCode fcode = FeatureCode.valueOf(newPlace
							.getPropertiesByType(PropertyTypes.GEONAMES_FCODE.typeName).iterator().next().getValue());

					logger.trace("retrieving alternate names for place");
					final Set<PlaceName> alternateNames = getAlternateNames(geonamesId, altnameStmt);
					// if an alternate name is equal to the default name, remove the default name
					removeDefaultPlaceName(newPlace, alternateNames);
					newPlace.addPlaceNames(alternateNames);

					logger.trace("retrieving wikipedia links");
					// no real performance benefit... requires shit loads of memory.
					// if (wikiLinksPerPlace.containsKey(geonamesId)) {
					// wikiLinksPerPlace.get(geonamesId)
					// .forEach(wikiLink -> newPlace.addProperty(new PlaceProperty(wikiLink, wikiLinkType,
					// null, null, createProvenance(geonamesId, false))));
					// }
					newPlace.addProperties(getWikipediaLinks(geonamesId, wikipediaLinkStmt));

					if (!deriveParentFromAdminLevel) {
						logger.trace("retrieving administrative parents of administrative place");
						newPlace.addProperties(getParentIdsFromHierarchy(geonamesId, parentIdFromHiearchyStmt));
						// if we can't find a parent in the hierarchy-table, consider the admin code
						if (newPlace.getPropertiesByType(PropertyTypes.GEONAMES_ADMIN_PARENT.typeName).isEmpty()) {
							if (parentIdsPerPlace.containsKey(geonamesId)) {
								parentIdsPerPlace.get(geonamesId)
										.forEach(parentId -> newPlace.addProperty(new PlaceProperty(parentId.toString(),
												adminParentType, null, null, createProvenance(geonamesId, false))));
							}
						}
					}
					else if (parentIdsPerPlace.containsKey(geonamesId)) {
						logger.trace("retrieving administrative parents of non-administrative place");
						parentIdsPerPlace.get(geonamesId)
								.forEach(parentId -> newPlace.addProperty(new PlaceProperty(parentId.toString(),
										adminParentType, null, null, createProvenance(geonamesId, false))));
					}

					if (deriveSeatPlace) {
						if (CAPITALS.contains(fcode)) {
							logger.trace("retrieving administrative parents of capital");
							if (parentIdsPerCapitalPlace.containsKey(geonamesId)) {
								parentIdsPerCapitalPlace.get(geonamesId)
										.forEach(parentId -> newPlace.addProperty(new PlaceProperty(parentId.toString(),
												capitalOfType, null, null, createProvenance(geonamesId, false))));
							}
						}
						else {
							logger.trace("retrieving administrative parents of seat");
							if (parentIdsPerSeatPlace.containsKey(geonamesId)) {
								parentIdsPerSeatPlace.get(geonamesId)
										.forEach(parentId -> newPlace.addProperty(new PlaceProperty(parentId.toString(),
												seatOfType, null, null, createProvenance(geonamesId, false))));
							}
						}
					}

					logger.trace("setting place types");
					setPlaceTypes(newPlace, fcode);

					em.persist(newPlace);

					// https://docs.jboss.org/hibernate/orm/5.1/userguide/html_single/Hibernate_User_Guide.html#batch-jdbcbatch
					if (GazetteerPersistenceManager.JBDC_BATCH_SIZE > 0) {
						if (count % GazetteerPersistenceManager.JBDC_BATCH_SIZE == 0 && count > 0) {
							em.flush();
							em.clear();
						}
					}

					count += 1;
				}

				em.getTransaction().commit();
			}

			final long endTime = System.currentTimeMillis();
			final long processingTime = endTime - startTime;
			logger.info("Imported " + count + " places in " + processingTime + " ms ("
					+ count / (processingTime / (double) 1000) + " places/s)");
		}

	}

	private Place resultSetToPlace(final ResultSet resultSet) throws SQLException, ParseException {
		// NOTE: the resultSet is expected to come from executing a query with `geonameStmtBasicStr` as basis

		// we assume that the id always exists, since it's a primary key...
		final Integer geonamesId = resultSet.getInt(1);
		final Place newPlace = new Place(null, null, null, null, null, null, null, createProvenance(geonamesId, true));

		// --- Name ---
		final String mainPlaceName = resultSet.getString(2);
		final EnumSet<NameFlag> mainNameFlags = EnumSet.of(NameFlag.IS_PREFERRED);
		final PlaceName placeName = new PlaceName(mainPlaceName, null, mainNameFlags, newPlace, null,
				createProvenance(geonamesId, false));
		newPlace.addPlaceName(placeName);

		// --- Coordinate ---
		// we can trust that the following values are not null, since GeoNames does not contain null values for them
		final Double latitude = resultSet.getDouble(3);
		final Double longitude = resultSet.getDouble(4);
		final Footprint footprint = new Footprint(wktReader.read("POINT(" + longitude + " " + latitude + ")"), null,
				newPlace, null, createProvenance(geonamesId, false));
		newPlace.addFootprint(footprint);

		// --- GeoNames Id ---
		final PlaceProperty geonamesIdProp = new PlaceProperty(geonamesId.toString(),
				(PlacePropertyType) allTypes.get(PropertyTypes.GEONAMES_ID.typeName), newPlace, null,
				createProvenance(geonamesId, false));
		newPlace.addProperty(geonamesIdProp);

		// --- Population ---
		final Long population = resultSet.getLong(5);
		// population values are set to 0, if none exists
		if (!resultSet.wasNull() && population > 0) {
			final PlaceProperty populationProp = new PlaceProperty(population.toString(),
					(PlacePropertyType) allTypes.get(PropertyTypes.POPULATION.typeName), newPlace, null,
					createProvenance(geonamesId, false));
			newPlace.addProperty(populationProp);
		}

		// --- Geonames FClass, FCode ---
		final String fclass = resultSet.getString(6);
		if (fclass != null) {
			final PlaceProperty fclassProp = new PlaceProperty(fclass,
					(PlacePropertyType) allTypes.get(PropertyTypes.GEONAMES_FCLASS.typeName), newPlace, null,
					createProvenance(geonamesId, false));
			newPlace.addProperty(fclassProp);
		}
		final String fcode = resultSet.getString(7);
		if (fcode != null) {
			final PlaceProperty fcodeProp = new PlaceProperty(fcode,
					(PlacePropertyType) allTypes.get(PropertyTypes.GEONAMES_FCODE.typeName), newPlace, null,
					createProvenance(geonamesId, false));
			newPlace.addProperty(fcodeProp);
		}

		// --- Elevation ---
		// we import the gtopo30 values as elevation
		final Integer elevation = resultSet.getInt(8);
		// -9999 is defined as null according to http://www.geonames.org/export/web-services.html
		if (!resultSet.wasNull() && elevation > -9999) {
			final PlaceProperty elevationProp = new PlaceProperty(elevation.toString(),
					(PlacePropertyType) allTypes.get(PropertyTypes.ELEVATION.typeName), newPlace, null,
					createProvenance(geonamesId, false));
			newPlace.addProperty(elevationProp);
		}

		// --- Admin4 code ---
		final String adminCode = resultSet.getString(9);
		if (adminCode != null) {
			final PlaceProperty adminCodeProp = new PlaceProperty(adminCode,
					(PlacePropertyType) allTypes.get(PropertyTypes.GEONAMES_ADMIN_CODE.typeName), newPlace, null,
					createProvenance(geonamesId, false));
			newPlace.addProperty(adminCodeProp);
		}

		return newPlace;
	}

	private static Set<PlaceName> getAlternateNames(final Integer geonamesId, final PreparedStatement altnameStmt)
			throws SQLException {
		// NOTE: the altnameStmt is expected to be based on `altNameStmtStr`
		final Set<PlaceName> altNames = new HashSet<>();
		altnameStmt.setInt(1, geonamesId);
		try (final ResultSet rs = altnameStmt.executeQuery()) {
			while (rs.next()) {
				final PlaceName altName = resultSetToAlternativeName(geonamesId, rs);
				altNames.add(altName);
			}
		}

		return altNames;
	}

	private static PlaceName resultSetToAlternativeName(final Integer geonamesId, final ResultSet resultSet)
			throws SQLException {
		// NOTE: the resultSet is expected to come from executing a query with `altNameStmtStr` as basis
		final String name = resultSet.getString(1);
		String language = resultSet.getString(2);
		final EnumSet<NameFlag> nameFlags = EnumSet.noneOf(NameFlag.class);

		final Boolean preferred = resultSet.getBoolean(3);
		if (!resultSet.wasNull()) {
			nameFlags.add(preferred ? NameFlag.IS_PREFERRED : NameFlag.IS_NOT_PREFERRED);
		}

		// isshortname is not equivalent to our understanding of abbreviation; therefore we ignore the flag for now
		// Boolean abbrev = resultSet.getBoolean(4);
		// if (resultSet.wasNull()) {
		// abbrev = null;
		// }
		// else {
		// nameFlags.add(abbrev ? NameFlag.IS_ABBREVIATION : NameFlag.IS_NOT_ABBREVIATION);
		// }

		final Boolean colloquial = resultSet.getBoolean(5);
		if (!resultSet.wasNull()) {
			nameFlags.add(colloquial ? NameFlag.IS_COLLOQUIAL : NameFlag.IS_NOT_COLLOQUIAL);
		}

		final Boolean historical = resultSet.getBoolean(6);
		if (!resultSet.wasNull()) {
			nameFlags.add(historical ? NameFlag.IS_HISTORICAL : NameFlag.IS_NOT_HISTORICAL);
		}

		if (language.equals("abbr")) {
			nameFlags.add(NameFlag.IS_ABBREVIATION);
			language = null;
		}

		return new PlaceName(name, language, nameFlags, null, null, createProvenance(geonamesId, false));
	}

	private static void removeDefaultPlaceName(final Place place, final Set<PlaceName> alternateNames) {
		if (place.getPlaceNames().isEmpty()) {
			return;
		}
		final PlaceName defaultName = place.getPlaceNames().iterator().next();

		for (final PlaceName placeName : alternateNames) {
			if (placeName.getName().equals(defaultName.getName())) {
				place.getPlaceNames().remove(defaultName);
				return;
			}
		}
	}

	private Set<PlaceProperty> getWikipediaLinks(final Integer geonamesId, final PreparedStatement wikipediaLinkStmt)
			throws SQLException {
		// NOTE: the wikipediaLinkStmt is expected to be based on `wikipediaLinkStmtStr`
		final Set<PlaceProperty> wikipediaLinks = new HashSet<>();
		wikipediaLinkStmt.setInt(1, geonamesId);
		logger.trace(wikipediaLinkStmt.toString());

		try (final ResultSet rs = wikipediaLinkStmt.executeQuery()) {
			while (rs.next()) {
				wikipediaLinks.add(resultSetToWikipediaLinkPlaceProperty(geonamesId, rs));
			}
		}

		return wikipediaLinks;
	}

	private PlaceProperty resultSetToWikipediaLinkPlaceProperty(final Integer geonamesId, final ResultSet rs)
			throws SQLException {
		// NOTE: the resultSet is expected to come from executing a query with `wikipediaLinkStmtStr` as basis
		final String wikipediaLink = rs.getString(1);
		return new PlaceProperty(wikipediaLink, (PlacePropertyType) allTypes.get(PropertyTypes.WIKIPEDIA_LINK.typeName),
				null, null, createProvenance(geonamesId, false));

	}

	private Set<PlaceProperty> getParentIdsFromHierarchy(final Integer geonamesId,
			final PreparedStatement parentIdFromHierarchyStmt) throws SQLException {
		final Set<PlaceProperty> parentIds = new HashSet<>();
		parentIdFromHierarchyStmt.setInt(1, geonamesId);
		logger.trace(parentIdFromHierarchyStmt.toString());

		try (final ResultSet rs = parentIdFromHierarchyStmt.executeQuery()) {
			while (rs.next()) {
				parentIds.add(resultSetToParentIdProperty(geonamesId, rs));
			}
		}

		return parentIds;
	}

	private PlaceProperty resultSetToParentIdProperty(final Integer geonamesId, final ResultSet rs)
			throws SQLException {
		final Integer parentId = rs.getInt(1);
		if (rs.wasNull()) {
			return null;
		}

		return new PlaceProperty(parentId.toString(),
				(PlacePropertyType) allTypes.get(PropertyTypes.GEONAMES_ADMIN_PARENT.typeName), null, null,
				createProvenance(geonamesId, false));
	}

	@SuppressWarnings("unused")
	private Map<Integer, List<String>> getWikiLinksPerPlace(final Set<FeatureCode> fcodes) throws SQLException {
		final String statementStr = wikipediaLinksStmtStr
				.replace(":fclasses", featureCodeEnumFeatureClassesToIncludingSQLStr(fcodes))
				.replace(":fcodes", featureCodeEnumToIncludingSQLStr(fcodes));
		logger.debug("loading wikipedia links");
		final Map<Integer, List<String>> wikiLinksPerPlace = new HashMap<>();

		try (final PreparedStatement wikipediaLinksStmt = connector.getConnection().prepareStatement(statementStr)) {
			wikipediaLinksStmt.setFetchSize(POSTGRES_QUERY_FETCH_SIZE);
			try (final ResultSet rs = wikipediaLinksStmt.executeQuery()) {
				int count = 0;

				while (rs.next()) {
					if (count % loggingVerbosity == 0) {
						logger.trace("loading wikilink #" + count);
					}

					final Integer geonameId = rs.getInt(1);
					final String wikiLink = rs.getString(2);
					if (!wikiLinksPerPlace.containsKey(geonameId)) {
						wikiLinksPerPlace.put(geonameId, new ArrayList<>());
					}
					wikiLinksPerPlace.get(geonameId).add(wikiLink);

					count += 1;
				}

				logger.debug("found " + count + " parents");
			}
		}

		return wikiLinksPerPlace;
	}

	private Map<Integer, List<Integer>> getAdminParentIdsFromAdminCodes(final Set<FeatureCode> fcodes)
			throws SQLException {
		final String statementStr = parentIdsFromAdminCodesStmtStr
				.replace(":child_fclasses", featureCodeEnumFeatureClassesToIncludingSQLStr(fcodes))
				.replace(":child_fcodes", featureCodeEnumToIncludingSQLStr(fcodes))
				.replace(":parent_fclasses", featureCodeEnumFeatureClassesToIncludingSQLStr(SEAT_PARENTS))
				.replace(":parent_fcodes", featureCodeEnumToIncludingSQLStr(SEAT_PARENTS));

		final Map<Integer, List<Integer>> parentIdsPerPlace = new HashMap<>();

		try (final PreparedStatement parentIdsFromAdminCodesStmt = connector.getConnection()
				.prepareStatement(statementStr)) {
			parentIdsFromAdminCodesStmt.setFetchSize(POSTGRES_QUERY_FETCH_SIZE);
			try (final ResultSet rs = parentIdsFromAdminCodesStmt.executeQuery()) {
				int count = 0;

				while (rs.next()) {
					if (count % loggingVerbosity == 0) {
						logger.trace("loading parent #" + count);
					}

					final Integer childId = rs.getInt(1);
					final Integer parentId = rs.getInt(2);
					if (!parentIdsPerPlace.containsKey(childId)) {
						parentIdsPerPlace.put(childId, new ArrayList<>());
					}
					parentIdsPerPlace.get(childId).add(parentId);

					count += 1;
				}

				logger.debug("found " + count + " parents");
			}
		}

		return parentIdsPerPlace;
	}

	private Map<Integer, List<Integer>> getCapitalParentIdsFromAdminCodes() throws SQLException {
		final String statementStr = capitalParentIdsFromAdminCodesStmtStr
				.replace(":child_fclasses", featureCodeEnumFeatureClassesToIncludingSQLStr(CAPITALS))
				.replace(":child_fcodes", featureCodeEnumToIncludingSQLStr(CAPITALS))
				.replace(":parent_fclasses", featureCodeEnumFeatureClassesToIncludingSQLStr(POLITICAL_ENTITIES))
				.replace(":parent_fcodes", featureCodeEnumToIncludingSQLStr(POLITICAL_ENTITIES));

		final Map<Integer, List<Integer>> parentIdsPerPlace = new HashMap<>();

		try (final PreparedStatement parentIdsFromAdminCodesStmt = connector.getConnection()
				.prepareStatement(statementStr)) {
			parentIdsFromAdminCodesStmt.setFetchSize(POSTGRES_QUERY_FETCH_SIZE);
			try (final ResultSet rs = parentIdsFromAdminCodesStmt.executeQuery()) {
				int count = 0;

				while (rs.next()) {
					if (count % loggingVerbosity == 0) {
						logger.trace("loading capital parent #" + count);
					}

					final Integer childId = rs.getInt(1);
					final Integer parentId = rs.getInt(2);
					if (!parentIdsPerPlace.containsKey(childId)) {
						parentIdsPerPlace.put(childId, new ArrayList<>());
					}
					parentIdsPerPlace.get(childId).add(parentId);

					count += 1;
				}

				logger.debug("found " + count + " capital parents");
			}
		}

		return parentIdsPerPlace;
	}

	private void setPlaceTypes(final Place place, final FeatureCode fcode) {
		final EnumSet<PlaceTypes> placeTypes = EnumSet.noneOf(PlaceTypes.class);

		if (fcode.isHistorical()) {
			placeTypes.add(PlaceTypes.HISTORICAL);
		}

		if (fcode.equals(FeatureCode.CONT)) {
			placeTypes.add(PlaceTypes.CONTINENT);
		}

		if (POLITICAL_ENTITIES.contains(fcode)) {
			placeTypes.add(PlaceTypes.POLITICAL_ENTITY);
		}

		if (POPULATED_PLACES.contains(fcode)) {
			placeTypes.add(PlaceTypes.POPULATED_PLACE);
		}

		if (ADMIN_ENTITIES.contains(fcode)) {
			switch (fcode) {
			case ADM1:
			case ADM1H:
				placeTypes.add(PlaceTypes.ADMIN1);
				break;
			case ADM2:
			case ADM2H:
				placeTypes.add(PlaceTypes.ADMIN2);
				break;
			case ADM3:
			case ADM3H:
				placeTypes.add(PlaceTypes.ADMIN3);
				break;
			case ADM4:
			case ADM4H:
				placeTypes.add(PlaceTypes.ADMIN4);
				break;
			case ADM5:
				placeTypes.add(PlaceTypes.ADMIN5);
				break;
			default:
				placeTypes.add(PlaceTypes.ADMINISTRATIVE_DIVISION);
				break;
			}
		}

		if (CAPITALS.contains(fcode)) {
			placeTypes.add(PlaceTypes.CAPITAL);
		}
		else if (ADMIN_ENTITY_SEATS.contains(fcode)) {
			placeTypes.add(PlaceTypes.ADMINISTRATIVE_DIVISION_SEAT);
		}

		if (POI_PLACES.contains(fcode)) {
			switch (fcode) {
			case AIRP:
				placeTypes.add(PlaceTypes.AIRPORT);
				break;
			case CSTL:
				placeTypes.add(PlaceTypes.CASTLE);
				break;
			case HTL:
				placeTypes.add(PlaceTypes.HOTEL);
				break;
			default:
				placeTypes.add(PlaceTypes.POI);
				break;
			}
		}

		placeTypes.forEach(placeType -> place.addPlaceTypeAssignment(
				new PlaceTypeAssignment((PlaceType) allTypes.get(placeType.typeName), place, null, null)));

	}

	/*
	 * ****************************************************************************************************************
	 * PLACE LINKING SPECIFIC CODE
	 * ****************************************************************************************************************
	 */

	public void linkPlaces() {
		// NOTE: this code is very fast, but it only works if foreign-key constraints are turned off!
		// using the following index greatly increases performance
		GazetteerInstaller.ensureIndexExists(em, "place_property_place_id_type_id_idx",
				"ON place_property USING btree (place_id, type_id)");
		logger.info("=== linking places ===");
		final long startTime = System.currentTimeMillis();

		int count = 0;

		count += linkRelatedPlacesByGeoNamesId(allTypes.get(RelationshipTypes.SUBDIVISION.typeName).getId(),
				allTypes.get(PropertyTypes.GEONAMES_ADMIN_PARENT.typeName).getId(),
				" = '" + FeatureClass.A.name() + "'", "subdivisions");
		count += linkRelatedPlacesByGeoNamesId(allTypes.get(RelationshipTypes.WITHIN_DIVISION.typeName).getId(),
				allTypes.get(PropertyTypes.GEONAMES_ADMIN_PARENT.typeName).getId(),
				" != '" + FeatureClass.A.name() + "'", "within division");
		count += linkRelatedPlacesByGeoNamesId(allTypes.get(RelationshipTypes.SEAT_OF.typeName).getId(),
				allTypes.get(PropertyTypes.GEONAMES_GOV_SEAT_OF.typeName).getId(), " = '" + FeatureClass.P.name() + "'",
				"admin seats");
		count += linkRelatedPlacesByGeoNamesId(allTypes.get(RelationshipTypes.CAPITAL_OF.typeName).getId(),
				allTypes.get(PropertyTypes.GEONAMES_CAPITAL_OF.typeName).getId(), " = '" + FeatureClass.P.name() + "'",
				"capitals");

		final long endTime = System.currentTimeMillis();
		final long processingTime = endTime - startTime;
		logger.info("Linked " + count + " places in " + processingTime + " ms ("
				+ count / (processingTime / (double) 1000) + " places/s)");
		logger.info("- DONE -");
	}

	private int linkRelatedPlacesByGeoNamesId(final Long relationId, final Long relationIndicatorId,
			final String fclassCondition, final String info) {
		em.getTransaction().begin();
		final javax.persistence.Query query = em
				.createNativeQuery("INSERT INTO place_relationship (id, left_place_id, right_place_id, type_id, value) "
						+ "SELECT nextval('entity_id_sequence'), c.place_id, p.place_id, :rel_type_id, null "
						+ "FROM place_property p, place_property c "
						+ "WHERE c.type_id = :parent_type_id AND p.type_id = :geonameid_type_id AND p.value = c.value  "
						+ "      AND EXISTS (SELECT * FROM place_property fclass WHERE fclass.place_id = c.place_id AND "
						+ "			fclass.type_id = :fclass_type_id AND fclass.value " + fclassCondition + "); "
						+ "INSERT INTO entity (id, valid_time_end, valid_time_start) "
						+ "SELECT id, null, null FROM place_relationship WHERE type_id = :rel_type_id ; ")
				.setParameter("rel_type_id", relationId).setParameter("parent_type_id", relationIndicatorId)
				.setParameter("fclass_type_id", allTypes.get(PropertyTypes.GEONAMES_FCLASS.typeName).getId())
				.setParameter("geonameid_type_id", allTypes.get(PropertyTypes.GEONAMES_ID.typeName).getId());
		final int count = query.executeUpdate();
		em.getTransaction().commit();

		logger.debug("linked " + count + " " + info);
		return count;
	}

}
