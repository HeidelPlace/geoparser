package de.unihd.dbs.geoparser.gazetteer;

import static org.hamcrest.Matchers.*;
import static org.junit.Assert.*;

import java.util.Arrays;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import javax.persistence.NoResultException;
import javax.persistence.criteria.Join;

import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TestRule;
import org.junit.rules.TestWatcher;
import org.junit.runner.Description;
import de.unihd.dbs.geoparser.core.GeoparserConfig;
import de.unihd.dbs.geoparser.gazetteer.Gazetteer.AdditionalPredicateBuilder;
import de.unihd.dbs.geoparser.gazetteer.Gazetteer.PlaceFeatureSelectionBuilder;
import de.unihd.dbs.geoparser.gazetteer.models.AbstractEntity;
import de.unihd.dbs.geoparser.gazetteer.models.Footprint;
import de.unihd.dbs.geoparser.gazetteer.models.Place;
import de.unihd.dbs.geoparser.gazetteer.models.PlaceName;
import de.unihd.dbs.geoparser.gazetteer.models.PlaceName.NameFlag;
import de.unihd.dbs.geoparser.gazetteer.models.PlacePropertyType;
import de.unihd.dbs.geoparser.gazetteer.models.PlaceRelationshipType;
import de.unihd.dbs.geoparser.gazetteer.models.PlaceType;
import de.unihd.dbs.geoparser.gazetteer.models.Type;
import de.unihd.dbs.geoparser.gazetteer.query.BoundingBoxPlaceFilter;
import de.unihd.dbs.geoparser.gazetteer.query.JoinUtil;
import de.unihd.dbs.geoparser.gazetteer.query.PlaceIdPlaceFilter;
import de.unihd.dbs.geoparser.gazetteer.query.PlaceNamePlaceFilter;
import de.unihd.dbs.geoparser.gazetteer.query.PlaceNamePlaceFilter.MatchMode;
import de.unihd.dbs.geoparser.gazetteer.query.PlacePropertyPlaceFilter;
import de.unihd.dbs.geoparser.gazetteer.query.PlaceRelationshipPlaceFilter;
import de.unihd.dbs.geoparser.gazetteer.query.PlaceRelationshipPlaceFilter.PlaceRelationshipDirection;
import de.unihd.dbs.geoparser.gazetteer.query.PlaceTypePlaceFilter;
import de.unihd.dbs.geoparser.gazetteer.query.QueryFilter;
import de.unihd.dbs.geoparser.gazetteer.types.PlaceTypes;
import de.unihd.dbs.geoparser.gazetteer.types.PropertyTypes;
import de.unihd.dbs.geoparser.gazetteer.types.RelationshipTypes;
import de.unihd.dbs.geoparser.gazetteer.util.GazetteerPersistenceManager;

import com.vividsolutions.jts.geom.Coordinate;
import com.vividsolutions.jts.geom.Envelope;
import com.vividsolutions.jts.geom.GeometryFactory;
import com.vividsolutions.jts.geom.Point;
import com.vividsolutions.jts.geom.PrecisionModel;

public class GazetteerTest {

	public static final String WORKING_GAZETTEER_PERSISTENCE_UNIT_NAME_LABEL = "gazetteer.working.persistence_unit.name";
	public static final String WORKING_GAZETTEER_PERSISTENCE_UNIT_DB_CONNECTION_INFO_LABEL = "gazetteer.working.persistence_unit.db_source";

	private static GazetteerPersistenceManager gpm;
	private static Gazetteer gazetteer;
	private static Map<String, Type> allTypes;

	@BeforeClass
	public static void setUpBeforeClass() throws Exception {
		final GeoparserConfig config = new GeoparserConfig();
		gpm = new GazetteerPersistenceManager(
				config.getConfigStringByLabel(WORKING_GAZETTEER_PERSISTENCE_UNIT_NAME_LABEL),
				config.getDBConnectionInfoByLabel(
						config.getConfigStringByLabel(WORKING_GAZETTEER_PERSISTENCE_UNIT_DB_CONNECTION_INFO_LABEL)));
		gazetteer = new Gazetteer(gpm.getEntityManager());
		loadTypes();
	}

	private static void loadTypes() {
		allTypes = new HashMap<>();
		final Set<Type> types = gazetteer.getAllTypes();
		types.forEach(type -> allTypes.put(type.getName(), type));
	}

	@AfterClass
	public static void tearDownAfterClass() throws Exception {
		try {
			if (gazetteer != null) {
				gazetteer.close();
			}
		}
		finally {
			gpm.close();
		}
	}

	@Rule
	public TestRule watcher = new TestWatcher() {
		@Override
		protected void starting(final Description description) {
			System.out.println(" === Starting test '" + description.getMethodName() + "' ===");
		}
	};

	@Test(expected = NullPointerException.class)
	public void testGazetterWithNullGazetteerManager() throws Exception {
		try (final Gazetteer gazetteer = new Gazetteer(null)) {
		}
	}

	@Test
	public void testGetPlaceBeingValid() {
		assertThat(gazetteer.getPlace(1L), instanceOf(Place.class));
	}

	@Test(expected = NoResultException.class)
	public void testGetPlaceBeingInvalid() {
		gazetteer.getPlace(2L);
	}

	@Test
	public void testGetEntityBeingValid() {
		assertThat(gazetteer.getEntity(1L), instanceOf(AbstractEntity.class));
	}

	@Test(expected = NoResultException.class)
	public void testGetEntityBeingInvalid() {
		gazetteer.getEntity(Long.MAX_VALUE);
	}

	@Test
	public void testGetType() {
		final Type expectedType = allTypes.values().iterator().next();
		final Type actualType = gazetteer.getType(expectedType.getId());

		assertThat(actualType, equalTo(expectedType));
	}

	@Test
	public void testGetTypesOfClass() {
		final Set<Type> expectedTypes = allTypes.values().stream()
				.filter(type -> type.getClass().equals(PlaceType.class)).collect(Collectors.toSet());
		final Set<Type> actualTypes = gazetteer.getAllTypes(PlaceType.class);

		assertThat(actualTypes, equalTo(expectedTypes));
	}

	@Test
	public void testGetPlacesWithoutFiltersButLimit() {
		final int placeLimit = 5;
		final GazetteerQuery<Place> query = new GazetteerQuery<>(placeLimit);
		final List<Place> places = gazetteer.getPlaces(query);
		assertThat(places.size(), equalTo(placeLimit));
	}

	@Test
	public void testGetPlacesWithPlaceIdFilter() {
		final Set<Long> expectedPlaceIds = new HashSet<>(Arrays.asList(1L));
		final boolean excludeIds = false;
		final QueryFilter<Place> filter = new PlaceIdPlaceFilter(expectedPlaceIds, excludeIds);
		final GazetteerQuery<Place> query = new GazetteerQuery<>(Arrays.asList(filter));

		final List<Place> places = gazetteer.getPlaces(query);
		assertThat(places.size(), equalTo(expectedPlaceIds.size()));
		final Set<Long> actualPlaceIds = places.stream().map(place -> place.getId()).collect(Collectors.toSet());
		assertThat(actualPlaceIds, equalTo(expectedPlaceIds));
	}

	@Test(expected = IllegalArgumentException.class)
	public void testGetPlacesWithPropertyFilter() {
		final PlacePropertyType someType = (PlacePropertyType) allTypes.get(PropertyTypes.POPULATION.typeName);
		new PlacePropertyPlaceFilter<String>(someType, null, null, Integer.class, false);
	}

	@Test
	public void testGetPlacesWithPropertyFilterInclusive() {
		final int placeLimit = 5;
		final PlacePropertyType wikipediaLinkType = (PlacePropertyType) allTypes
				.get(PropertyTypes.WIKIPEDIA_LINK.typeName);
		final QueryFilter<Place> filter = new PlacePropertyPlaceFilter<String>(wikipediaLinkType, false);
		final GazetteerQuery<Place> query = new GazetteerQuery<>(Arrays.asList(filter), placeLimit);

		final List<Place> places = gazetteer.getPlaces(query);

		assertThat(places.size(), greaterThan(0));
	}

	@Test
	public void testGetPlacesWithPropertyFilterExclusive() {
		final int placeLimit = 5;
		final PlacePropertyType wikipediaLinkType = (PlacePropertyType) allTypes
				.get(PropertyTypes.WIKIPEDIA_LINK.typeName);
		final QueryFilter<Place> filter = new PlacePropertyPlaceFilter<String>(wikipediaLinkType, true);
		final GazetteerQuery<Place> query = new GazetteerQuery<>(Arrays.asList(filter), placeLimit);

		final List<Place> places = gazetteer.getPlaces(query);

		assertThat(places.size(), greaterThan(0));
	}

	@Test
	public void testGetPlacesWithGeonamesIdSetFilter() {
		final Set<Long> expectedGeonamesIds = new HashSet<>(Arrays.asList(3214104L, // Stadt Heidelberg
				3220720L) // Heidelberg
		);
		final boolean excludeIds = false;
		final PlacePropertyType geonamesIdType = (PlacePropertyType) allTypes.get(PropertyTypes.GEONAMES_ID.typeName);
		final QueryFilter<Place> filter = new PlacePropertyPlaceFilter<>(geonamesIdType, expectedGeonamesIds,
				Long.class, excludeIds);
		final GazetteerQuery<Place> query = new GazetteerQuery<>(Arrays.asList(filter));

		final List<Place> places = gazetteer.getPlaces(query);

		assertThat(places.size(), equalTo(expectedGeonamesIds.size()));
		// final Set<Long> actualPlaceIds = places.stream().map(place -> place.gazetteerLocation.)
		// .collect(Collectors.toSet());
		// assertThat(actualPlaceIds, equalTo(expectedGeonamesIds));
	}

	@Test
	public void testGetPlacesWithPopulationRangeFilter() {
		final int placeLimit = 5;
		final Long minPopulation = Long.valueOf(100_000);
		final Long maxPopulation = Long.valueOf(250_000);
		final PlacePropertyType populationType = (PlacePropertyType) allTypes.get(PropertyTypes.POPULATION.typeName);
		final QueryFilter<Place> filter = new PlacePropertyPlaceFilter<>(populationType, minPopulation, maxPopulation,
				Long.class, false);
		final GazetteerQuery<Place> query = new GazetteerQuery<>(Arrays.asList(filter), placeLimit);

		final List<Place> places = gazetteer.getPlaces(query);

		assertThat(places.size(), greaterThan(0));
	}

	@Test
	public void testGetPlacesWithPopulationMinValueFilter() {
		final int placeLimit = 5;
		final Long minPopulation = Long.valueOf(100_000);
		final Long maxPopulation = null;
		final PlacePropertyType populationType = (PlacePropertyType) allTypes.get(PropertyTypes.POPULATION.typeName);
		final QueryFilter<Place> filter = new PlacePropertyPlaceFilter<>(populationType, minPopulation, maxPopulation,
				Long.class, false);
		final GazetteerQuery<Place> query = new GazetteerQuery<>(Arrays.asList(filter), placeLimit);

		final List<Place> places = gazetteer.getPlaces(query);

		assertThat(places.size(), greaterThan(0));
	}

	@Test
	public void testGetPlacesWithPopulationMaxValueFilter() {
		final int placeLimit = 5;
		final Long minPopulation = null;
		final Long maxPopulation = Long.valueOf(250_000);
		final PlacePropertyType populationType = (PlacePropertyType) allTypes.get(PropertyTypes.POPULATION.typeName);
		final QueryFilter<Place> filter = new PlacePropertyPlaceFilter<>(populationType, minPopulation, maxPopulation,
				Long.class, false);
		final GazetteerQuery<Place> query = new GazetteerQuery<>(Arrays.asList(filter), placeLimit);

		final List<Place> places = gazetteer.getPlaces(query);

		assertThat(places.size(), greaterThan(0));
	}

	@Test
	public void testGetPlacesWithExactPlaceNameFilter() {
		final String placeName = "Heidelberg";
		final QueryFilter<Place> filter = new PlaceNamePlaceFilter(placeName, null, EnumSet.noneOf(NameFlag.class),
				false, MatchMode.EXACT, 0.0, false);
		final GazetteerQuery<Place> query = new GazetteerQuery<>(Arrays.asList(filter));

		final List<Place> places = gazetteer.getPlaces(query);

		assertThat(places.size(), greaterThan(0));
		for (final Place place : places) {
			assertThat(place.getPlaceNames().stream().map(name -> name.getName()).collect(Collectors.toSet()),
					hasItem(placeName));
		}
	}

	@Test
	public void testGetPlacesWithExactPlaceNameCaseSensitiveFilter() {
		final String placeName = "heidelberg";
		final QueryFilter<Place> filter = new PlaceNamePlaceFilter(placeName, null, EnumSet.noneOf(NameFlag.class),
				false, MatchMode.EXACT, 0.0, false);
		final GazetteerQuery<Place> query = new GazetteerQuery<>(Arrays.asList(filter));

		final List<Place> places = gazetteer.getPlaces(query);

		assertThat(places.size(), equalTo(0));
	}

	@Test
	public void testGetPlacesWithExactPlaceNameCaseInSensitiveFilter() {
		final String placeName = "heidelberg";
		final QueryFilter<Place> filter = new PlaceNamePlaceFilter(placeName, null, EnumSet.noneOf(NameFlag.class),
				true, MatchMode.EXACT, 0.0, false);
		final GazetteerQuery<Place> query = new GazetteerQuery<>(Arrays.asList(filter));

		final List<Place> places = gazetteer.getPlaces(query);

		assertThat(places.size(), greaterThan(0));
	}

	@Test
	public void testGetPlacesWithFuzzyPlaceNameFilterLevensthein() {
		final int placeLimit = 5;
		final String fuzzyName = "Heidelbärg";
		final QueryFilter<Place> filter = new PlaceNamePlaceFilter(fuzzyName, null, EnumSet.noneOf(NameFlag.class),
				false, MatchMode.FUZZY_LEVENSTHEIN, 2.0, false);
		final GazetteerQuery<Place> query = new GazetteerQuery<>(Arrays.asList(filter), placeLimit);

		final List<Place> places = gazetteer.getPlaces(query);

		assertThat(places.size(), greaterThan(0));
		for (final Place place : places) {
			System.out.println(place.getPlaceNames().stream().map(name -> name.getName()).collect(Collectors.toSet()));
		}
	}

	@Test
	public void testGetPlacesWithFuzzyPlaceNameFilterPreAndPostfix() {
		final int placeLimit = 5;
		final String fuzzyName = "eidelber";
		final QueryFilter<Place> filter = new PlaceNamePlaceFilter(fuzzyName, null, EnumSet.noneOf(NameFlag.class),
				false, MatchMode.FUZZY_PREFIX_POSTFIX, 0.0, false);
		final GazetteerQuery<Place> query = new GazetteerQuery<>(Arrays.asList(filter), placeLimit);

		final List<Place> places = gazetteer.getPlaces(query);

		assertThat(places.size(), greaterThan(0));
		for (final Place place : places) {
			System.out.println(place.getPlaceNames().stream().map(name -> name.getName()).collect(Collectors.toSet()));
		}
	}

	@Test
	public void testGetPlacesWithFuzzyPlaceNameFilterPrefix() {
		final int placeLimit = 5;
		final String fuzzyName = "eidelberg";
		final QueryFilter<Place> filter = new PlaceNamePlaceFilter(fuzzyName, null, EnumSet.noneOf(NameFlag.class),
				false, MatchMode.FUZZY_PREFIX, 0.0, false);
		final GazetteerQuery<Place> query = new GazetteerQuery<>(Arrays.asList(filter), placeLimit);

		final List<Place> places = gazetteer.getPlaces(query);

		assertThat(places.size(), greaterThan(0));
		for (final Place place : places) {
			System.out.println(place.getPlaceNames().stream().map(name -> name.getName()).collect(Collectors.toSet()));
		}
	}

	@Test
	public void testGetPlacesWithFuzzyPlaceNameFilterPostfix() {
		final int placeLimit = 5;
		final String fuzzyName = "Heidelber";
		final QueryFilter<Place> filter = new PlaceNamePlaceFilter(fuzzyName, null, EnumSet.noneOf(NameFlag.class),
				false, MatchMode.FUZZY_POSTFIX, 0.0, false);
		final GazetteerQuery<Place> query = new GazetteerQuery<>(Arrays.asList(filter), placeLimit);

		final List<Place> places = gazetteer.getPlaces(query);

		assertThat(places.size(), greaterThan(0));
		for (final Place place : places) {
			System.out.println(place.getPlaceNames().stream().map(name -> name.getName()).collect(Collectors.toSet()));
		}
	}

	@Test
	public void testGetPlacesWithPlaceNameLanguageFilterHits() {
		final String placeName = "Heidelberg";
		final String german = "de";
		final QueryFilter<Place> filter = new PlaceNamePlaceFilter(placeName, german, EnumSet.noneOf(NameFlag.class),
				false, MatchMode.EXACT, 0.0, false);
		final GazetteerQuery<Place> query = new GazetteerQuery<>(Arrays.asList(filter));

		final List<Place> places = gazetteer.getPlaces(query);

		assertThat(places.size(), greaterThan(0));
	}

	@Test
	public void testGetPlacesWithPlaceNameLanguageFilterNoHit() {
		final String placeName = "Heidelberg";
		final String klingon = "klingon";
		final QueryFilter<Place> filter = new PlaceNamePlaceFilter(placeName, klingon, EnumSet.noneOf(NameFlag.class),
				false, MatchMode.EXACT, 0.0, false);
		final GazetteerQuery<Place> query = new GazetteerQuery<>(Arrays.asList(filter));

		final List<Place> places = gazetteer.getPlaces(query);

		assertThat(places, empty());
	}

	@Test
	public void testGetPlacesWithPlaceNameFlagFilterHits() {
		final String placeName = "West Germany";
		final EnumSet<NameFlag> flags = EnumSet.of(NameFlag.IS_HISTORICAL);
		final QueryFilter<Place> filter = new PlaceNamePlaceFilter(placeName, null, flags, false, MatchMode.EXACT, 0.0,
				false);
		final GazetteerQuery<Place> query = new GazetteerQuery<>(Arrays.asList(filter));

		final List<Place> places = gazetteer.getPlaces(query);

		assertThat(places.size(), greaterThan(0));
	}

	@Test
	public void testGetPlacesWithPlaceNameFlagFilterNotHit() {
		final String placeName = "West Germany";
		final EnumSet<NameFlag> flags = EnumSet.of(NameFlag.IS_PREFERRED);
		final QueryFilter<Place> filter = new PlaceNamePlaceFilter(placeName, null, flags, false, MatchMode.EXACT, 0.0,
				false);
		final GazetteerQuery<Place> query = new GazetteerQuery<>(Arrays.asList(filter));

		final List<Place> places = gazetteer.getPlaces(query);

		assertThat(places.size(), equalTo(0));
	}

	@Test
	public void testGetPlacesWithBoundingBoxFilter() {
		final int placeLimit = 5;
		final GeometryFactory geomFactory = new GeometryFactory(new PrecisionModel(), Footprint.REFERENCE_SYSTEM_SRID);
		// Germany: NE 55.05814, 15.04205; SW 47.27021, 5.86624
		final Point northEast = geomFactory.createPoint(new Coordinate(15.04205, 55.05814));
		final Point southWest = geomFactory.createPoint(new Coordinate(5.86624, 47.27021));
		final Envelope bbox = new Envelope(northEast.getCoordinate(), southWest.getCoordinate());
		final boolean excludeBBox = false;
		final QueryFilter<Place> filter = new BoundingBoxPlaceFilter(geomFactory.toGeometry(bbox), excludeBBox);
		final GazetteerQuery<Place> query = new GazetteerQuery<>(Arrays.asList(filter), placeLimit);

		final List<Place> places = gazetteer.getPlaces(query);

		assertThat(places.size(), greaterThan(0));
	}

	@Test
	public void testGetPlacesWithPlaceTypeFilter() {
		final int placeLimit = 5;
		final PlaceType continentType = (PlaceType) allTypes.get(PlaceTypes.CONTINENT.typeName);
		final QueryFilter<Place> filter = new PlaceTypePlaceFilter(new HashSet<>(Arrays.asList(continentType)), false);
		final GazetteerQuery<Place> query = new GazetteerQuery<>(Arrays.asList(filter), placeLimit);

		final List<Place> places = gazetteer.getPlaces(query);

		assertThat(places.size(), greaterThan(0));
	}

	@Test
	public void testGetPlacesWithPlaceRelationshipFilter() {
		final int placeLimit = 5;
		final PlaceRelationshipType seatOfType = (PlaceRelationshipType) allTypes
				.get(RelationshipTypes.SEAT_OF.typeName);
		final PlaceRelationshipDirection direction = PlaceRelationshipDirection.LEFT_TO_RIGHT;
		final Set<Place> otherPlaces = null;
		final QueryFilter<Place> filter = new PlaceRelationshipPlaceFilter<>(seatOfType, direction, otherPlaces, false);
		final GazetteerQuery<Place> query = new GazetteerQuery<>(Arrays.asList(filter), placeLimit);

		final List<Place> places = gazetteer.getPlaces(query);

		assertThat(places.size(), greaterThan(0));
	}

	@Test
	public void testGetPlacesWithPlaceRelationshipFilterWithOtherPlaces() {
		final int placeLimit = 5;
		final PlaceRelationshipType seatOfType = (PlaceRelationshipType) allTypes
				.get(RelationshipTypes.SEAT_OF.typeName);
		final PlaceRelationshipDirection direction = PlaceRelationshipDirection.LEFT_TO_RIGHT;
		final Set<Place> otherPlaces = new HashSet<>(getHeidelbergPlace());
		final QueryFilter<Place> filter = new PlaceRelationshipPlaceFilter<>(seatOfType, direction, otherPlaces, false);
		final GazetteerQuery<Place> query = new GazetteerQuery<>(Arrays.asList(filter), placeLimit);

		final List<Place> places = gazetteer.getPlaces(query);

		assertThat(places.size(), greaterThan(0));
	}

	private static List<Place> getHeidelbergPlace() {
		final PlacePropertyType geonamesIdType = (PlacePropertyType) allTypes.get(PropertyTypes.GEONAMES_ID.typeName);
		final QueryFilter<Place> filter = new PlacePropertyPlaceFilter<>(geonamesIdType,
				new HashSet<>(Arrays.asList("6555638")), String.class, false);
		final GazetteerQuery<Place> query = new GazetteerQuery<>(Arrays.asList(filter), 1);
		return gazetteer.getPlaces(query);
	}

	@Test
	public void testGetPlacesWithMulitpleFilters() {
		final int placeLimit = 5;

		final String placeName = "Heidelberg";

		final Long minPopulation = Long.valueOf(100_000);
		final Long maxPopulation = Long.valueOf(250_000);
		final PlacePropertyType populationType = (PlacePropertyType) allTypes.get(PropertyTypes.POPULATION.typeName);

		final GeometryFactory geomFactory = new GeometryFactory(new PrecisionModel(), Footprint.REFERENCE_SYSTEM_SRID);
		// Germany: NE 55.05814, 15.04205; SW 47.27021, 5.86624
		final Point northEast = geomFactory.createPoint(new Coordinate(15.04205, 55.05814));
		final Point southWest = geomFactory.createPoint(new Coordinate(5.86624, 47.27021));
		final Envelope bbox = new Envelope(northEast.getCoordinate(), southWest.getCoordinate());
		final GazetteerQuery<Place> query = new GazetteerQuery<>(placeLimit);

		query.filters.add(new PlaceNamePlaceFilter(placeName, null, EnumSet.noneOf(NameFlag.class), false,
				MatchMode.EXACT, 0.0, false));
		query.filters.add(new BoundingBoxPlaceFilter(geomFactory.toGeometry(bbox), false));
		query.filters
				.add(new PlacePropertyPlaceFilter<>(populationType, minPopulation, maxPopulation, Long.class, false));

		final List<Place> places = gazetteer.getPlaces(query);
		assertThat(places.size(), greaterThan(0));
	}

	@Test
	public void testCountPlacesWithMulitpleFilters() {
		final int placeLimit = 5;

		final String placeName = "Heidelberg";

		final Long minPopulation = Long.valueOf(100_000);
		final Long maxPopulation = Long.valueOf(250_000);
		final PlacePropertyType populationType = (PlacePropertyType) allTypes.get(PropertyTypes.POPULATION.typeName);

		final GeometryFactory geomFactory = new GeometryFactory(new PrecisionModel(), Footprint.REFERENCE_SYSTEM_SRID);
		// Germany: NE 55.05814, 15.04205; SW 47.27021, 5.86624
		final Point northEast = geomFactory.createPoint(new Coordinate(15.04205, 55.05814));
		final Point southWest = geomFactory.createPoint(new Coordinate(5.86624, 47.27021));
		final Envelope bbox = new Envelope(northEast.getCoordinate(), southWest.getCoordinate());
		final GazetteerQuery<Place> query = new GazetteerQuery<>(placeLimit);

		query.filters.add(new PlaceNamePlaceFilter(placeName, null, EnumSet.noneOf(NameFlag.class), false,
				MatchMode.EXACT, 0.0, false));
		query.filters.add(new BoundingBoxPlaceFilter(geomFactory.toGeometry(bbox), false));
		query.filters
				.add(new PlacePropertyPlaceFilter<>(populationType, minPopulation, maxPopulation, Long.class, false));

		final Long placeCount = gazetteer.countPlaces(query);
		System.out.println(placeCount);
		assertThat(placeCount.intValue(), greaterThan(0));
	}

	@Test
	public void testGetSelectedPlaceFeatures() {
		final int resultLimit = 5;
		final GazetteerQuery<Place> query = new GazetteerQuery<>(resultLimit);
		final PlaceFeatureSelectionBuilder selectBuilder = (criteriaBuilder, queryRoot) -> {
			final Join<Place, ?> names = queryRoot.getJoins().iterator().next();
			return criteriaBuilder.array(queryRoot.get("id"), names.get("name"));
		};
		final AdditionalPredicateBuilder predicateBuilder = (criteriaBuilder, queryRoot) -> {
			final Join<Place, PlaceName> names = queryRoot.join("placeNames");
			return Arrays.asList(criteriaBuilder.or(criteriaBuilder.isNull(names.get("language")),
					criteriaBuilder.like(names.get("language"), "en")));
		};

		final List<Object[]> places = gazetteer.getSelectedPlaceFeatures(query, selectBuilder, predicateBuilder);
		for (final Object[] placeFeatures : places) {
			System.out.println(placeFeatures[0] + ": " + placeFeatures[1]);
		}
	}

	@Test
	public void testGetSelectedPlaceFeaturesWithQuery() {
		final int resultLimit = 5;
		final String placeName = "heidelberg";
		final QueryFilter<Place> filter = new PlaceNamePlaceFilter(placeName, null, EnumSet.noneOf(NameFlag.class),
				true, MatchMode.EXACT, 0.0, false);
		final GazetteerQuery<Place> query = new GazetteerQuery<>(Arrays.asList(filter), resultLimit);
		final PlaceFeatureSelectionBuilder selectBuilder = (criteriaBuilder, queryRoot) -> {
			final Join<Place, PlaceName> names = JoinUtil.getJoin(queryRoot, "placeNames");
			return criteriaBuilder.array(queryRoot.get("id"), names.get("name"));
		};
		final AdditionalPredicateBuilder predicateBuilder = (criteriaBuilder, queryRoot) -> {
			final Join<Place, PlaceName> names = JoinUtil.getJoin(queryRoot, "placeNames");
			return Arrays.asList(criteriaBuilder.or(criteriaBuilder.isNull(names.get("language")),
					criteriaBuilder.like(names.get("language"), "en")));
		};

		final List<Object[]> places = gazetteer.getSelectedPlaceFeatures(query, selectBuilder, predicateBuilder);
		for (final Object[] placeFeatures : places) {
			System.out.println(placeFeatures[0] + ": " + placeFeatures[1]);
		}
	}

}
