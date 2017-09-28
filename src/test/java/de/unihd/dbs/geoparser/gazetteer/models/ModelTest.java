package de.unihd.dbs.geoparser.gazetteer.models;

import static org.hamcrest.Matchers.*;
import static org.junit.Assert.*;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.EnumSet;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import javax.persistence.EntityManager;
import javax.persistence.TypedQuery;

import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Ignore;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TestRule;
import org.junit.rules.TestWatcher;
import org.junit.runner.Description;
import de.unihd.dbs.geoparser.core.GeoparserConfig;
import de.unihd.dbs.geoparser.core.GeoparserConfig.UnknownConfigLabelException;
import de.unihd.dbs.geoparser.gazetteer.models.AbstractEntity.ValidTime;
import de.unihd.dbs.geoparser.gazetteer.models.PlaceName.NameFlag;
import de.unihd.dbs.geoparser.gazetteer.util.GazetteerPersistenceManager;

import com.jcraft.jsch.JSchException;
import com.vividsolutions.jts.geom.Geometry;
import com.vividsolutions.jts.geom.GeometryFactory;
import com.vividsolutions.jts.geom.PrecisionModel;
import com.vividsolutions.jts.io.ParseException;
import com.vividsolutions.jts.io.WKTReader;

public class ModelTest {

	private static final WKTReader fromText = new WKTReader(
			new GeometryFactory(new PrecisionModel(), Footprint.REFERENCE_SYSTEM_SRID));

	private static GazetteerPersistenceManager persistenceManager;
	private EntityManager entityManager;

	// a few default place we need for creating a fully filled place
	final PlaceType placeType = new PlaceType("default_place_type", "default_place_type", null, null, null);
	final PlacePropertyType propertyType = new PlacePropertyType("default_prop_type", "default_prop_type", null, null,
			null);
	final PlaceRelationshipType relationshipType = new PlaceRelationshipType("default_rel_type", "default_rel_type",
			null, null, null);

	@BeforeClass
	public static void startPersistenceProvider() throws IllegalArgumentException, ExceptionInInitializerError,
			JSchException, UnknownConfigLabelException, IOException {
		persistenceManager = new GazetteerPersistenceManager(new GeoparserConfig());
	}

	@AfterClass
	public static void tearDownPersistenceProvider() {
		if (persistenceManager != null) {
			persistenceManager.close();
			persistenceManager = null;
		}
	}

	@Rule
	public TestRule watcher = new TestWatcher() {
		@Override
		protected void starting(final Description description) {
			System.out.println(" === Starting test '" + description.getMethodName() + "' ===");
		}
	};

	@Before
	public void setupPresistenceContext() {
		System.out.println("--- setting up ---");
		entityManager = persistenceManager.getEntityManager();
		createDefaultTypes();
	}

	@After
	public void tearDownPersistenceContext() {
		if (entityManager != null) {
			System.out.println("--- tearing down ---");
			try {
				removeDefaultTypes();
			}
			catch (final Exception e) {
				System.out.println("Removing default Types failed!");
				if (entityManager.getTransaction().isActive()) {
					entityManager.getTransaction().rollback();
				}
			}
			entityManager.close();
			entityManager = null;
		}
	}

	private void createDefaultTypes() {
		System.out.println("--- Creating Default Types ---");
		entityManager.getTransaction().begin();
		entityManager.persist(placeType);
		entityManager.persist(propertyType);
		entityManager.persist(relationshipType);
		entityManager.getTransaction().commit();
	}

	private void removeDefaultTypes() {
		System.out.println("--- Removing Default Types ---");
		entityManager.getTransaction().begin();
		entityManager.remove(placeType);
		entityManager.remove(propertyType);
		entityManager.remove(relationshipType);
		entityManager.getTransaction().commit();
	}

	private void assertNoArtifactsRemain() {
		final List<AbstractEntity> entities = entityManager.createQuery("FROM AbstractEntity", AbstractEntity.class)
				.getResultList();
		assertThat(entities, emptyCollectionOf(AbstractEntity.class));

		// Querying the above should be sufficient. If a more detailed query is needed, use the code below...
		// final List<Place> places = entityManager.createQuery("FROM Place", Place.class).getResultList();
		// assertThat(places, emptyCollectionOf(Place.class));
		// final List<PlaceName> placeNames = entityManager.createQuery("FROM PlaceName",
		// PlaceName.class).getResultList();
		// assertThat(placeNames, emptyCollectionOf(PlaceName.class));
		// final List<Footprint> footprints = entityManager.createQuery("FROM Footprint",
		// Footprint.class).getResultList();
		// assertThat(footprints, emptyCollectionOf(Footprint.class));
		// final List<Provenance> provenances = entityManager.createQuery("FROM Provenance",
		// Provenance.class).getResultList();
		// assertThat(provenances, emptyCollectionOf(Provenance.class));
		// final List<PlaceProperty> properties = entityManager.createQuery("FROM PlaceProperty",
		// PlaceProperty.class).getResultList();
		// assertThat(properties, emptyCollectionOf(PlaceProperty.class));
		// final List<PlaceRelationship> relationships = entityManager.createQuery("FROM PlaceRelationship",
		// PlaceRelationship.class).getResultList();
		// assertThat(relationships, emptyCollectionOf(PlaceRelationship.class));
		// final List<PlaceTypeAssignment> placeTypeAsgts = entityManager.createQuery("FROM PlaceTypeAssignment",
		// PlaceTypeAssignment.class).getResultList();
		// assertThat(placeTypeAsgts, emptyCollectionOf(PlaceTypeAssignment.class));

		final TypedQuery<Type> typeQuery = entityManager.createQuery("FROM Type WHERE id NOT IN (:id_1, :id_2, :id_3)",
				Type.class);
		typeQuery.setParameter("id_1", placeType.getId()).setParameter("id_2", propertyType.getId())
				.setParameter("id_3", relationshipType.getId());
		final List<Type> types = typeQuery.getResultList();
		assertThat(types, emptyCollectionOf(Type.class));
	}

	private static Footprint createFootprint(final String provenanceLabel) throws ParseException {
		final ValidTime validTime = new ValidTime(Calendar.getInstance(), Calendar.getInstance());
		final Provenance provenance = (provenanceLabel != null) ? new Provenance(provenanceLabel, "ModelTest", null)
				: null;
		return new Footprint(fromText.read("POINT(50 50)"), 0.01d, null, validTime, provenance);
	}

	private static PlaceName createPlaceName(final String provenanceLabel) {
		final ValidTime validTime = new ValidTime(Calendar.getInstance(), Calendar.getInstance());
		final Provenance provenance = (provenanceLabel != null) ? new Provenance(provenanceLabel, "ModelTest", null)
				: null;
		final EnumSet<NameFlag> nameFlags = EnumSet.of(NameFlag.IS_ABBREVIATION, NameFlag.IS_COLLOQUIAL,
				NameFlag.IS_HISTORICAL, NameFlag.IS_OFFICIAL, NameFlag.IS_PREFERRED);
		return new PlaceName("testName", "testLanguage", nameFlags, null, validTime, provenance);
	}

	private PlaceProperty createProperty(final String provenanceLabel) {
		final ValidTime validTime = new ValidTime(Calendar.getInstance(), Calendar.getInstance());
		final Provenance provenance = (provenanceLabel != null) ? new Provenance(provenanceLabel, "ModelTest", null)
				: null;
		return new PlaceProperty("test_value", propertyType, null, validTime, provenance);
	}

	private PlaceTypeAssignment createPlaceTypeAssignment(final String provenanceLabel) {
		final ValidTime validTime = new ValidTime(Calendar.getInstance(), Calendar.getInstance());
		final Provenance provenance = (provenanceLabel != null) ? new Provenance(provenanceLabel, "ModelTest", null)
				: null;
		return new PlaceTypeAssignment(placeType, null, validTime, provenance);
	}

	private Place createFullPlace(final String placeLabel) throws ParseException {
		System.out.println("--- Creating place ---");
		final ValidTime validTime = new ValidTime(Calendar.getInstance(), Calendar.getInstance());

		final Footprint footprint = createFootprint("test/footprint_" + placeLabel);
		final PlaceName placeName = createPlaceName("test/placename_" + placeLabel);
		final PlaceProperty property = createProperty("test/property_" + placeLabel);
		final PlaceTypeAssignment placeTypeAsg = createPlaceTypeAssignment("test/type_" + placeLabel);

		final Provenance placeProvenance = new Provenance("test/" + placeLabel, "ModelTest", null);
		final Set<Footprint> footprints = new HashSet<>(Arrays.asList(footprint));
		final Set<PlaceName> placeNames = new HashSet<>(Arrays.asList(placeName));
		final Set<PlaceProperty> properties = new HashSet<>(Arrays.asList(property));
		final Set<PlaceTypeAssignment> placeTypeAsgts = new HashSet<>(Arrays.asList(placeTypeAsg));
		final Set<PlaceRelationship> leftRelationships = null;
		final Set<PlaceRelationship> rightRelationships = null;

		final Place place = new Place(footprints, placeNames, properties, placeTypeAsgts, leftRelationships,
				rightRelationships, validTime, placeProvenance);

		return place;
	}

	@Test
	public void testPlaceSimpleConstructorInitialization() {
		final Place place = new Place();
		assertThat(place.getFootprints(), empty());
		assertThat(place.getPlaceNames(), empty());
		assertThat(place.getProperties(), empty());
		assertThat(place.getPlaceTypeAssignments(), empty());
		assertThat(place.getLeftPlaceRelationships(), empty());
		assertThat(place.getRightPlaceRelationships(), empty());
	}

	// === FOOTPRINT TESTS ===

	@Test
	public void testFootprintConstructor() throws ParseException {
		final Place place = new Place();
		final Geometry geometry = fromText.read("POINT(50 50)");
		final Double precision = 0.01d;

		final Footprint footprint = new Footprint(geometry, precision, place, null, null);

		assertThat(footprint.getGeometry(), equalTo(geometry));
		assertThat(footprint.getPrecision(), equalTo(precision));
		assertThat(footprint.getPlace(), equalTo(place));
		assertThat(place.getFootprints(), contains(footprint));
	}

	@Test
	public void testSetFootprintsWithPersistence() throws ParseException {
		final Place place = new Place();
		final Footprint footprintA = createFootprint("testA");
		final Footprint footprintB = createFootprint("testB");
		place.setFootprints(new HashSet<>(Arrays.asList(footprintA, footprintB)));

		entityManager.getTransaction().begin();
		entityManager.persist(place);
		entityManager.getTransaction().commit();

		assertThat(footprintA.getId(), notNullValue());
		assertThat(footprintB.getId(), notNullValue());

		entityManager.getTransaction().begin();
		entityManager.remove(place);
		entityManager.getTransaction().commit();

		assertNoArtifactsRemain();
	}

	@Test
	public void testSetFootprintPlaceWithPersistence() throws ParseException {
		final Place placeA = new Place();
		final Footprint footprint = createFootprint("testA");
		placeA.addFootprint(footprint);

		entityManager.getTransaction().begin();
		entityManager.persist(placeA);
		entityManager.getTransaction().commit();

		entityManager.getTransaction().begin();
		final Place placeB = new Place();
		footprint.setPlace(placeB);
		entityManager.getTransaction().commit();

		entityManager.getTransaction().begin();
		footprint.setPlace(null);
		entityManager.getTransaction().commit();

		entityManager.getTransaction().begin();
		entityManager.remove(placeA);
		entityManager.getTransaction().commit();

		assertNoArtifactsRemain();
	}

	@Test
	public void testSetFootprintsWithRemoveWithPersistence() throws ParseException {
		final Place place = new Place();
		final Footprint footprintA = createFootprint("testA");
		final Footprint footprintB = createFootprint("testB");
		place.addFootprint(footprintA);

		entityManager.getTransaction().begin();
		entityManager.persist(place);
		entityManager.getTransaction().commit();

		entityManager.getTransaction().begin();
		place.setFootprints(new HashSet<>(Arrays.asList(footprintB)));
		entityManager.getTransaction().commit();

		entityManager.getTransaction().begin();
		entityManager.remove(place);
		entityManager.getTransaction().commit();

		assertNoArtifactsRemain();
	}

	@Test
	public void testRemovingOrphanFootprints() throws ParseException {
		entityManager.getTransaction().begin();
		final Place place = createFullPlace("orphan_footprint_place");
		entityManager.persist(place);
		entityManager.getTransaction().commit();

		final List<Long> footprintIds = new ArrayList<>();
		place.getFootprints().forEach(footprint -> footprintIds.add(footprint.getId()));

		System.out.println("removing footprints");

		entityManager.getTransaction().begin();
		place.setFootprints(null);
		entityManager.getTransaction().commit();

		final TypedQuery<Footprint> query = entityManager
				.createQuery("FROM Footprint WHERE id IN (:ids)", Footprint.class).setParameter("ids", footprintIds);
		final List<Footprint> foundFootprints = query.getResultList();
		assertThat(foundFootprints, emptyCollectionOf(Footprint.class));

		System.out.println("removing places");

		entityManager.getTransaction().begin();
		entityManager.remove(place);
		entityManager.getTransaction().commit();

		assertNoArtifactsRemain();
	}

	@Ignore("Ignored due to bug. See Provenance.class for details... "
			+ "http://stackoverflow.com/questions/10656765/handling-collection-updates-with-jpa. "
			+ "Could be a problem with our join-table inheritance model!")
	@Test()
	public void testChangeFootprintsOwner() throws ParseException {
		entityManager.getTransaction().begin();
		final Place placeA = createFullPlace("reassign_footprint_place_a");
		final Place placeB = createFullPlace("reassign_footprint_place_b");
		entityManager.persist(placeA);
		entityManager.persist(placeB);
		entityManager.getTransaction().commit();

		System.out.println("removing all footprints from placeB");
		entityManager.getTransaction().begin();
		placeB.setFootprints(null);
		entityManager.getTransaction().commit();

		System.out.println("moving footprints from placeA to placeB");

		entityManager.getTransaction().begin();
		placeB.setFootprints(placeA.getFootprints());
		assertThat(placeA.getFootprints(), empty());
		entityManager.getTransaction().commit();

		System.out.println("removing places");

		entityManager.getTransaction().begin();
		entityManager.remove(placeA);
		entityManager.remove(placeB);
		entityManager.getTransaction().commit();

		assertNoArtifactsRemain();
	}

	// === PLACE NAME TESTS ===

	@Test
	public void testPlaceNameConstructor() {
		final Place place = new Place();
		final String name = "test";
		final String language = "junit";
		final EnumSet<NameFlag> flags = EnumSet.of(NameFlag.IS_ABBREVIATION, NameFlag.IS_COLLOQUIAL,
				NameFlag.IS_HISTORICAL, NameFlag.IS_OFFICIAL, NameFlag.IS_PREFERRED);

		final PlaceName placeName = new PlaceName(name, language, flags, place, null, null);

		assertThat(placeName.getName(), equalTo(name));
		assertThat(placeName.getLanguage(), equalTo(language));
		assertThat(placeName.getNameFlags(), equalTo(flags));
		assertThat(placeName.getPlace(), equalTo(place));
		assertThat(place.getPlaceNames(), contains(placeName));
	}

	@Test
	public void testPlaceNameFlagAccessors() {
		final EnumSet<NameFlag> positiveNameFlags = EnumSet.of(NameFlag.IS_ABBREVIATION, NameFlag.IS_COLLOQUIAL,
				NameFlag.IS_HISTORICAL, NameFlag.IS_OFFICIAL, NameFlag.IS_PREFERRED);
		final EnumSet<NameFlag> negativeNameFlags = EnumSet.of(NameFlag.IS_NOT_ABBREVIATION, NameFlag.IS_NOT_COLLOQUIAL,
				NameFlag.IS_NOT_HISTORICAL, NameFlag.IS_NOT_OFFICIAL, NameFlag.IS_NOT_PREFERRED);
		final EnumSet<NameFlag> emptyNameFlags = EnumSet.noneOf(NameFlag.class);
		final PlaceName placeName = createPlaceName("test");

		placeName.setNameFlags(positiveNameFlags);
		assertThat(placeName.getNameFlags(), equalTo(positiveNameFlags));
		placeName.setNameFlags(negativeNameFlags);
		assertThat(placeName.getNameFlags(), equalTo(negativeNameFlags));
		placeName.setNameFlags(emptyNameFlags);
		assertThat(placeName.getNameFlags(), equalTo(emptyNameFlags));
	}

	@Test
	public void testSetPlaceNamesWithPersistence() {
		final Place place = new Place();
		final PlaceName placeNameA = createPlaceName("testA");
		final PlaceName placeNameB = createPlaceName("testB");
		place.setPlaceNames(new HashSet<>(Arrays.asList(placeNameA, placeNameB)));

		entityManager.getTransaction().begin();
		entityManager.persist(place);
		entityManager.getTransaction().commit();

		assertThat(placeNameA.getId(), notNullValue());
		assertThat(placeNameB.getId(), notNullValue());

		entityManager.getTransaction().begin();
		entityManager.remove(place);
		entityManager.getTransaction().commit();

		assertNoArtifactsRemain();
	}

	@Test
	public void testSetPlaceNamePlaceWithPersistence() {
		final Place placeA = new Place();
		final PlaceName placeName = createPlaceName("testA");
		placeA.addPlaceName(placeName);

		entityManager.getTransaction().begin();
		entityManager.persist(placeA);
		entityManager.getTransaction().commit();

		entityManager.getTransaction().begin();
		final Place placeB = new Place();
		placeName.setPlace(placeB);
		entityManager.getTransaction().commit();

		entityManager.getTransaction().begin();
		placeName.setPlace(null);
		entityManager.getTransaction().commit();

		entityManager.getTransaction().begin();
		entityManager.remove(placeA);
		entityManager.getTransaction().commit();

		assertNoArtifactsRemain();
	}

	@Test
	public void testSetPlaceNamesWithRemoveWithPersistence() {
		final Place place = new Place();
		final PlaceName placeNameA = createPlaceName("testA");
		final PlaceName placeNameB = createPlaceName("testB");
		place.addPlaceName(placeNameA);

		entityManager.getTransaction().begin();
		entityManager.persist(place);
		entityManager.getTransaction().commit();

		entityManager.getTransaction().begin();
		place.setPlaceNames(new HashSet<>(Arrays.asList(placeNameB)));
		entityManager.getTransaction().commit();

		entityManager.getTransaction().begin();
		entityManager.remove(place);
		entityManager.getTransaction().commit();

		assertNoArtifactsRemain();
	}

	@Test
	public void testRemovingOrphanPlaceNames() throws ParseException {
		entityManager.getTransaction().begin();
		final Place place = createFullPlace("orphan_placename_place");
		entityManager.persist(place);
		entityManager.getTransaction().commit();

		final List<Long> placeNameIds = new ArrayList<>();
		place.getPlaceNames().forEach(placename -> placeNameIds.add(placename.getId()));

		System.out.println("removing placenames");

		entityManager.getTransaction().begin();
		place.setPlaceNames(null);
		entityManager.getTransaction().commit();

		final TypedQuery<PlaceName> query = entityManager
				.createQuery("FROM PlaceName WHERE id IN (:ids)", PlaceName.class).setParameter("ids", placeNameIds);
		final List<PlaceName> foundPlaceNames = query.getResultList();
		assertThat(foundPlaceNames, emptyCollectionOf(PlaceName.class));

		System.out.println("removing places");

		entityManager.getTransaction().begin();
		entityManager.remove(place);
		entityManager.getTransaction().commit();

		assertNoArtifactsRemain();
	}

	// === PLACE PROPERTY TESTS ===

	@Test
	public void testPlacePropertyConstructor() {
		final Place place = new Place();
		final String value = "testValue";
		final PlacePropertyType type = propertyType;
		final PlaceProperty property = new PlaceProperty(value, type, place, null, null);

		assertThat(property.getValue(), equalTo(value));
		assertThat(property.getType(), equalTo(type));
		assertThat(property.getPlace(), equalTo(place));
		assertThat(place.getProperties(), contains(property));
	}

	@Test
	public void testSetPlacePropertiesWithPersistence() {
		final Place place = new Place();
		final PlaceProperty typeA = createProperty("testA");
		final PlaceProperty typeB = createProperty("testB");

		place.setProperties(new HashSet<>(Arrays.asList(typeA, typeB)));

		entityManager.getTransaction().begin();
		entityManager.persist(place);
		entityManager.getTransaction().commit();

		assertThat(typeA.getId(), notNullValue());
		assertThat(typeB.getId(), notNullValue());

		entityManager.getTransaction().begin();
		entityManager.remove(place);
		entityManager.getTransaction().commit();

		assertNoArtifactsRemain();
	}

	@Test
	public void testSetPlacePropertyPlaceWithPersistence() {
		final Place placeA = new Place();
		final PlaceProperty type = createProperty("testA");
		placeA.addProperty(type);

		entityManager.getTransaction().begin();
		entityManager.persist(placeA);
		entityManager.getTransaction().commit();

		entityManager.getTransaction().begin();
		final Place placeB = new Place();
		type.setPlace(placeB);
		entityManager.getTransaction().commit();

		entityManager.getTransaction().begin();
		type.setPlace(null);
		entityManager.getTransaction().commit();

		entityManager.getTransaction().begin();
		entityManager.remove(placeA);
		entityManager.getTransaction().commit();

		assertNoArtifactsRemain();
	}

	@Test
	public void testSetPlacePropertyWithRemoveWithPersistence() {
		final Place place = new Place();
		final PlaceProperty typeA = createProperty("testA");
		final PlaceProperty typeB = createProperty("testB");
		place.addProperty(typeA);

		entityManager.getTransaction().begin();
		entityManager.persist(place);
		entityManager.getTransaction().commit();

		entityManager.getTransaction().begin();
		place.setProperties(new HashSet<>(Arrays.asList(typeB)));
		entityManager.getTransaction().commit();

		entityManager.getTransaction().begin();
		entityManager.remove(place);
		entityManager.getTransaction().commit();

		assertNoArtifactsRemain();
	}

	@Test
	public void testRemovingOrphanPlaceProperties() throws ParseException {
		entityManager.getTransaction().begin();
		final Place place = createFullPlace("orphan_property_place");
		entityManager.persist(place);
		entityManager.getTransaction().commit();

		final List<Long> propertyIds = new ArrayList<>();
		place.getProperties().forEach(property -> propertyIds.add(property.getId()));

		System.out.println("removing properties");

		entityManager.getTransaction().begin();
		place.setProperties(null);
		entityManager.getTransaction().commit();

		final TypedQuery<PlaceProperty> query = entityManager
				.createQuery("FROM PlaceProperty WHERE id IN (:ids)", PlaceProperty.class)
				.setParameter("ids", propertyIds);
		final List<PlaceProperty> foundPlaceNames = query.getResultList();
		assertThat(foundPlaceNames, emptyCollectionOf(PlaceProperty.class));

		System.out.println("removing places");

		entityManager.getTransaction().begin();
		entityManager.remove(place);
		entityManager.getTransaction().commit();

		assertNoArtifactsRemain();
	}

	// === PLACE TYPE ASSIGNMENT TESTS ===

	@Test
	public void testPlaceTypeAssignmentConstructor() {
		final Place place = new Place();
		final PlaceType type = placeType;
		final PlaceTypeAssignment placeTypeAsg = new PlaceTypeAssignment(type, place, null, null);

		assertThat(placeTypeAsg.getType(), equalTo(type));
		assertThat(placeTypeAsg.getPlace(), equalTo(place));
		assertThat(place.getPlaceTypeAssignments(), contains(placeTypeAsg));
	}

	@Test
	public void testSetPlaceTypeAssignmentsWithPersistence() {
		final Place place = new Place();
		final PlaceTypeAssignment asgA = createPlaceTypeAssignment("testA");
		final PlaceTypeAssignment asgB = createPlaceTypeAssignment("testB");

		place.setPlaceTypeAssignments(new HashSet<>(Arrays.asList(asgA, asgB)));

		entityManager.getTransaction().begin();
		entityManager.persist(place);
		entityManager.getTransaction().commit();

		assertThat(asgA.getId(), notNullValue());
		assertThat(asgB.getId(), notNullValue());

		entityManager.getTransaction().begin();
		entityManager.remove(place);
		entityManager.getTransaction().commit();

		assertNoArtifactsRemain();
	}

	@Test
	public void testSetPlaceTypeAssignmentPlaceWithPersistence() {
		final Place placeA = new Place();
		final PlaceTypeAssignment asg = createPlaceTypeAssignment("testA");
		placeA.addPlaceTypeAssignment(asg);

		entityManager.getTransaction().begin();
		entityManager.persist(placeA);
		entityManager.getTransaction().commit();

		entityManager.getTransaction().begin();
		final Place placeB = new Place();
		asg.setPlace(placeB);
		entityManager.getTransaction().commit();

		entityManager.getTransaction().begin();
		asg.setPlace(null);
		entityManager.getTransaction().commit();

		entityManager.getTransaction().begin();
		entityManager.remove(placeA);
		entityManager.getTransaction().commit();

		assertNoArtifactsRemain();
	}

	@Test
	public void testSetPlaceTypeAssignmentWithRemoveWithPersistence() {
		final Place place = new Place();
		final PlaceTypeAssignment asgA = createPlaceTypeAssignment("testA");
		final PlaceTypeAssignment asgB = createPlaceTypeAssignment("testB");
		place.addPlaceTypeAssignment(asgA);

		entityManager.getTransaction().begin();
		entityManager.persist(place);
		entityManager.getTransaction().commit();

		entityManager.getTransaction().begin();
		place.setPlaceTypeAssignments(new HashSet<>(Arrays.asList(asgB)));
		entityManager.getTransaction().commit();

		entityManager.getTransaction().begin();
		entityManager.remove(place);
		entityManager.getTransaction().commit();

		assertNoArtifactsRemain();
	}

	@Test
	public void testRemovingOrphanPlaceTypeAssignments() throws ParseException {
		entityManager.getTransaction().begin();
		final Place place = createFullPlace("orphan_type_asg_place");
		entityManager.persist(place);
		entityManager.getTransaction().commit();

		final List<Long> asgIds = new ArrayList<>();
		place.getPlaceTypeAssignments().forEach(asg -> asgIds.add(asg.getId()));

		System.out.println("removing place type assignments");

		entityManager.getTransaction().begin();
		place.setPlaceTypeAssignments(null);
		entityManager.getTransaction().commit();

		final TypedQuery<PlaceTypeAssignment> query = entityManager
				.createQuery("FROM PlaceTypeAssignment WHERE id IN (:ids)", PlaceTypeAssignment.class)
				.setParameter("ids", asgIds);
		final List<PlaceTypeAssignment> foundPlaceTypeAssignments = query.getResultList();
		assertThat(foundPlaceTypeAssignments, emptyCollectionOf(PlaceTypeAssignment.class));

		System.out.println("removing places");

		entityManager.getTransaction().begin();
		entityManager.remove(place);
		entityManager.getTransaction().commit();

		assertNoArtifactsRemain();
	}

	// === PLACE RELATIONSHIPS ===

	@Test
	public void testPlaceRelationshipConstructor() {
		final Place leftPlace = new Place();
		final Place rightPlace = new Place();
		final PlaceRelationshipType type = relationshipType;
		final String value = "testValue";
		final PlaceRelationship relationship = new PlaceRelationship(leftPlace, rightPlace, type, value, null, null);

		assertThat(relationship.getLeftPlace(), equalTo(leftPlace));
		assertThat(relationship.getRightPlace(), equalTo(rightPlace));
		assertThat(relationship.getType(), equalTo(type));
		assertThat(relationship.getValue(), equalTo(value));
	}

	@Test
	public void testPlaceRelationships() throws ParseException {
		final Place placeA = createFullPlace("relation_place_a");
		final Place placeB = createFullPlace("relation_place_b");
		final Place placeC = createFullPlace("relation_place_c");

		entityManager.getTransaction().begin();
		entityManager.persist(placeA);
		entityManager.persist(placeB);
		entityManager.persist(placeC);
		entityManager.getTransaction().commit();

		final PlaceRelationship relationA = new PlaceRelationship(placeA, placeB, relationshipType, "1.0", null, null);
		final PlaceRelationship relationB = new PlaceRelationship(placeC, placeA, relationshipType, "1.0", null, null);

		entityManager.getTransaction().begin();
		entityManager.persist(relationA);
		entityManager.persist(relationB);
		entityManager.getTransaction().commit();

		entityManager.getTransaction().begin();
		GazetteerPersistenceManager.removePlaceRelationship(entityManager, relationA);
		GazetteerPersistenceManager.removePlaceRelationship(entityManager, relationB);
		entityManager.getTransaction().commit();

		final PlaceRelationship foundRelationA = entityManager.find(PlaceRelationship.class, relationA.getId());
		final PlaceRelationship foundRelationB = entityManager.find(PlaceRelationship.class, relationB.getId());
		assertThat(foundRelationA, nullValue());
		assertThat(foundRelationB, nullValue());

		entityManager.getTransaction().begin();
		entityManager.remove(placeA);
		entityManager.remove(placeB);
		entityManager.remove(placeC);
		entityManager.getTransaction().commit();

		assertNoArtifactsRemain();
	}

	@Test
	public void testSetPlaceRelationships() {
		final Place placeA = new Place();
		final Place placeB = new Place();

		final PlaceRelationship relA = new PlaceRelationship(null, null, relationshipType, "1.0", null, null);
		final PlaceRelationship relB = new PlaceRelationship(null, null, relationshipType, "1.0", null, null);
		final Set<PlaceRelationship> leftRels = new HashSet<>(Arrays.asList(relA, relB));
		final Set<PlaceRelationship> rightRels = new HashSet<>(Arrays.asList(relA, relB));
		placeA.setLeftPlaceRelationships(leftRels);
		placeB.setRightPlaceRelationships(rightRels);

		assertThat(placeA.getLeftPlaceRelationships(), equalTo(leftRels));
		assertThat(placeB.getRightPlaceRelationships(), equalTo(rightRels));
	}

	// === PROVENANCE TESTS ===

	@Test
	public void testProvenanceConstructor() {
		final Place place = new Place();
		final String uri = "testuri";
		final String aggregationTool = "junit";
		final Provenance provenance = new Provenance(uri, aggregationTool, place);

		assertThat(provenance.getUri(), equalTo(uri));
		assertThat(provenance.getAggregationTool(), equalTo(aggregationTool));
		assertThat(provenance.getEntity(), equalTo(place));

		provenance.setEntity(null);
		assertThat(provenance.getEntity(), nullValue());
	}

	@Test
	public void testProvenanceEquality() {
		final Place place = new Place();
		final String uri = "testuri";
		final String aggregationTool = "junit";
		final Provenance provenanceA = new Provenance(uri, aggregationTool, place);
		final Provenance provenanceB = new Provenance(uri, aggregationTool, place);
		final Provenance provenanceC = new Provenance(uri, aggregationTool, null);
		final Provenance provenanceD = new Provenance(uri, aggregationTool, null);
		provenanceC.setId(1L);
		provenanceD.setId(1L);

		// test self reference
		assertThat(provenanceA, equalTo(provenanceA));

		// test null
		assertThat(provenanceA, not(equalTo(null)));

		// test other class
		assertThat(provenanceA, not(equalTo(place)));

		// test different instance
		assertThat(provenanceA, not(equalTo(provenanceB)));

		// test same id
		assertThat(provenanceC, equalTo(provenanceD));
	}

	@Test
	public void testRemovingOrphanProvenance() throws ParseException {
		entityManager.getTransaction().begin();
		final Place place = createFullPlace("orphan_provenance_place");
		entityManager.persist(place);
		entityManager.getTransaction().commit();

		System.out.println("setting new provenance, should delete old one");
		final long originalProvenanceId = place.getProvenance().getId();
		final Provenance newProvenance = new Provenance("test/new_provenance_orphan_provenance_place", "ModelTest",
				null);

		entityManager.getTransaction().begin();
		place.setProvenance(newProvenance);
		entityManager.getTransaction().commit();

		final Provenance foundOriginalProvenance = entityManager.find(Provenance.class, originalProvenanceId);
		assertThat(foundOriginalProvenance, nullValue());

		System.out.println("setting provenance to null, should remove the new one");

		entityManager.getTransaction().begin();
		place.setProvenance(null);
		entityManager.getTransaction().commit();

		final Provenance foundNewProvenance = entityManager.find(Provenance.class, newProvenance.getId());
		assertThat(foundNewProvenance, nullValue());

		System.out.println("removing place");

		entityManager.getTransaction().begin();
		entityManager.remove(place);
		entityManager.getTransaction().commit();

		assertNoArtifactsRemain();
	}

	@Test
	public void testReassigningProvenance() throws ParseException {
		entityManager.getTransaction().begin();
		final Place placeA = createFullPlace("reassign_provenance_place_a");
		final Place placeB = createFullPlace("reassign_provenance_place_b");
		entityManager.persist(placeA);
		entityManager.persist(placeB);
		entityManager.getTransaction().commit();

		System.out.println("moving provenance from placeA to placeB");

		entityManager.getTransaction().begin();
		placeB.setProvenance(placeA.getProvenance());
		entityManager.getTransaction().commit();

		System.out.println("removing places");

		entityManager.getTransaction().begin();
		entityManager.remove(placeA);
		entityManager.remove(placeB);
		entityManager.getTransaction().commit();

		assertNoArtifactsRemain();
	}

	// === ABSTRACT ENTITY TESTS ===

	private static class AbstractEntityDummy extends AbstractEntity {

		public AbstractEntityDummy() {
			super();
		}

		public AbstractEntityDummy(final ValidTime validTime, final Provenance provenance) {
			super(validTime, provenance);
		}
	}

	@Test
	public void testAbstractEntityDefaultConstructor() {
		final AbstractEntityDummy dummy = new AbstractEntityDummy();

		assertThat(dummy.getValidTimeStartDate(), nullValue());
		assertThat(dummy.getValidTimeEndDate(), nullValue());
		assertThat(dummy.getProvenance(), nullValue());
	}

	@Test
	public void testAbstractEntityConstructor() {
		final Calendar startDate = Calendar.getInstance();
		final Calendar endDate = Calendar.getInstance();
		final ValidTime validTime = new ValidTime(startDate, endDate);
		final Provenance provenance = new Provenance("test/relationship", "ModelTest", null);
		final AbstractEntityDummy dummy = new AbstractEntityDummy(validTime, provenance);

		assertThat(dummy.getValidTime().startDate, equalTo(startDate));
		assertThat(dummy.getValidTime().endDate, equalTo(endDate));
		assertThat(dummy.getValidTimeStartDate(), equalTo(startDate));
		assertThat(dummy.getValidTimeEndDate(), equalTo(endDate));
		assertThat(dummy.getProvenance(), equalTo(provenance));
	}

	@Test
	public void testAbstractEntityEquality() {
		final AbstractEntityDummy dummyA = new AbstractEntityDummy();
		final AbstractEntityDummy dummyB = new AbstractEntityDummy();
		final AbstractEntityDummy dummyC = new AbstractEntityDummy();
		final AbstractEntityDummy dummyD = new AbstractEntityDummy();
		final Place place = new Place();
		dummyC.setId(1L);
		dummyD.setId(1L);

		// test self reference
		assertThat(dummyA, equalTo(dummyA));

		// test null
		assertThat(dummyA, not(equalTo(null)));

		// test other class
		assertThat(dummyA, not(equalTo(place)));

		// test different instance
		assertThat(dummyA, not(equalTo(dummyB)));

		// test same id
		assertThat(dummyC, equalTo(dummyD));
	}

	// === PLACE TESTS ===

	@Test
	public void testInsertAndRemovePlaces() throws ParseException {
		final int PLACE_COUNT = 5;
		final List<Place> places = new ArrayList<>();

		entityManager.getTransaction().begin();
		for (int i = 0; i < PLACE_COUNT; i++) {
			final Place place = createFullPlace("place_" + i);
			places.add(place);
			entityManager.persist(place);
		}

		System.out.println("checking places persistence");

		for (final Place place : places) {
			final Place foundPlace = entityManager.find(Place.class, place.getId());
			assertThat(foundPlace, notNullValue());
			final List<PlaceName> placeNames = entityManager
					.createQuery("FROM PlaceName WHERE place = :place", PlaceName.class).setParameter("place", place)
					.getResultList();
			assertThat(placeNames, not(emptyCollectionOf(PlaceName.class)));
			final List<Footprint> footprints = entityManager
					.createQuery("FROM Footprint WHERE place = :place", Footprint.class).setParameter("place", place)
					.getResultList();
			assertThat(footprints, not(emptyCollectionOf(Footprint.class)));
			final List<Provenance> provenances = entityManager
					.createQuery("FROM Provenance WHERE entity = :place", Provenance.class).setParameter("place", place)
					.getResultList();
			assertThat(provenances, not(emptyCollectionOf(Provenance.class)));
			final List<PlaceProperty> properties = entityManager
					.createQuery("FROM PlaceProperty WHERE place = :place", PlaceProperty.class)
					.setParameter("place", place).getResultList();
			assertThat(properties, not(emptyCollectionOf(PlaceProperty.class)));
			final List<PlaceTypeAssignment> placeTypeAsgts = entityManager
					.createQuery("FROM PlaceTypeAssignment WHERE place = :place", PlaceTypeAssignment.class)
					.setParameter("place", place).getResultList();
			assertThat(placeTypeAsgts, not(emptyCollectionOf(PlaceTypeAssignment.class)));
		}

		System.out.println("removing places");
		for (final Place place : places) {
			entityManager.remove(place);
		}
		entityManager.getTransaction().commit();

		assertNoArtifactsRemain();
	}

	@Test
	public void testToStringMethods() throws ParseException {
		// this is just a simple smoke test to ensure no problems with recursive calls occur
		final Place placeA = createFullPlace("testPlaceA");
		final Place placeB = createFullPlace("testPlaceB");
		final ValidTime validTime = new ValidTime(Calendar.getInstance(), Calendar.getInstance());
		final Provenance placeProvenance = new Provenance("test/relationship", "ModelTest", null);
		final PlaceRelationship rel = new PlaceRelationship(placeA, placeB, relationshipType, "value", validTime,
				placeProvenance);
		placeA.addLeftPlaceRelationship(rel);
		placeA.toString();
		placeB.toString();

		// check plain entities, all relations should be null or empty collections
		new Footprint().toString();
		new Place().toString();
		new PlaceName().toString();
		new PlaceProperty().toString();
		new PlaceRelationship().toString();
		new PlaceTypeAssignment().toString();
		new Provenance().toString();
	}

	@Test
	public void testGetFootprintsByGeomType() throws ParseException {
		final Footprint pointA = new Footprint(fromText.read("POINT(50 50)"), 0.01d, null, null, null);
		final Footprint pointB = new Footprint(fromText.read("POINT(50 50)"), 0.01d, null, null, null);
		final Set<Footprint> points = new HashSet<>(Arrays.asList(pointA, pointB));
		final Footprint linestring = new Footprint(fromText.read("LINESTRING(50 50, 100 100)"), 0.01d, null, null,
				null);
		final Set<Footprint> linestrings = new HashSet<>(Arrays.asList(linestring));
		final Footprint polygon = new Footprint(fromText.read("POLYGON((50 50, 100 100, 100 150, 50 50))"), 0.01d, null,
				null, null);
		final Set<Footprint> polygons = new HashSet<>(Arrays.asList(polygon));
		final Place place = new Place();
		place.setFootprints(new HashSet<>(Arrays.asList(pointA, pointB, linestring, polygon)));

		assertThat(place.getFootprintsByGeomType("Point"), equalTo(points));
		assertThat(place.getFootprintsByGeomType("LineString"), equalTo(linestrings));
		assertThat(place.getFootprintsByGeomType("Polygon"), equalTo(polygons));
		assertThat(place.getFootprintsByGeomType("Multipolygon"), emptyCollectionOf(Footprint.class));
	}

	@Test
	public void testGetPreferredPlaceNames() {
		final PlaceName placeNameA = new PlaceName("A", "language", EnumSet.of(NameFlag.IS_PREFERRED), null, null,
				null);
		final PlaceName placeNameB = new PlaceName("B", "language", EnumSet.of(NameFlag.IS_PREFERRED), null, null,
				null);
		final PlaceName placeNameC = new PlaceName("C", "language", EnumSet.of(NameFlag.IS_NOT_PREFERRED), null, null,
				null);
		final PlaceName placeNameD = new PlaceName("D", "language", EnumSet.noneOf(NameFlag.class), null, null, null);
		final Place place = new Place();
		place.setPlaceNames(new HashSet<>(Arrays.asList(placeNameA, placeNameB, placeNameC, placeNameD)));
		final Set<PlaceName> expectedNames = new HashSet<>(Arrays.asList(placeNameA, placeNameB));

		final Set<PlaceName> actualNames = place.getPreferredPlaceNames();
		assertThat(actualNames, equalTo(expectedNames));
	}

	@Test
	public void testGetPlaceNamesForLanguage() {
		final String language = "languageA";
		final PlaceName placeNameA = new PlaceName("A", language, EnumSet.noneOf(NameFlag.class), null, null, null);
		final PlaceName placeNameB = new PlaceName("B", language, EnumSet.noneOf(NameFlag.class), null, null, null);
		final PlaceName placeNameC = new PlaceName("C", "languageB", EnumSet.noneOf(NameFlag.class), null, null, null);
		final Place place = new Place();
		place.setPlaceNames(new HashSet<>(Arrays.asList(placeNameA, placeNameB, placeNameC)));
		final Set<PlaceName> expectedNames = new HashSet<>(Arrays.asList(placeNameA, placeNameB));

		final Set<PlaceName> actualNames = place.getPlaceNamesForLanguage(language);
		assertThat(actualNames, equalTo(expectedNames));
	}

	@Test
	public void testGetPropertiesByType() {
		final String typeName = "A";
		final PlacePropertyType typeA = new PlacePropertyType(typeName, "", null, null, null);
		final PlacePropertyType typeB = new PlacePropertyType("B", "", null, null, null);

		final PlaceProperty propertyA = new PlaceProperty(null, typeA, null, null, null);
		final PlaceProperty propertyB = new PlaceProperty(null, typeA, null, null, null);
		final PlaceProperty propertyC = new PlaceProperty(null, typeB, null, null, null);
		final Place place = new Place();
		place.setProperties(new HashSet<>(Arrays.asList(propertyA, propertyB, propertyC)));

		final Set<PlaceProperty> expectedProperties = new HashSet<>(Arrays.asList(propertyA, propertyB));

		Set<PlaceProperty> actualProperties = place.getPropertiesByType(typeA);
		assertThat(actualProperties, equalTo(expectedProperties));
		actualProperties = place.getPropertiesByType(typeName);
		assertThat(actualProperties, equalTo(expectedProperties));
	}

	@Test
	public void testGetPropertiesByValue() {
		final String value = "A";

		final PlaceProperty propertyA = new PlaceProperty(value, null, null, null, null);
		final PlaceProperty propertyB = new PlaceProperty(value, null, null, null, null);
		final PlaceProperty propertyC = new PlaceProperty("B", null, null, null, null);
		final Place place = new Place();
		place.setProperties(new HashSet<>(Arrays.asList(propertyA, propertyB, propertyC)));

		final Set<PlaceProperty> expectedProperties = new HashSet<>(Arrays.asList(propertyA, propertyB));

		final Set<PlaceProperty> actualProperties = place.getPropertiesByValue(value);
		assertThat(actualProperties, equalTo(expectedProperties));
	}

	@Test
	public void testGetPlaceTypeAssignmentsByType() {
		final String typeName = "A";
		final PlaceType typeA = new PlaceType(typeName, "", null, null, null);
		final PlaceType typeB = new PlaceType("B", "", null, null, null);

		final PlaceTypeAssignment asgA = new PlaceTypeAssignment(typeA, null, null, null);
		final PlaceTypeAssignment asgB = new PlaceTypeAssignment(typeA, null, null, null);
		final PlaceTypeAssignment asgC = new PlaceTypeAssignment(typeB, null, null, null);
		final Place place = new Place();
		place.setPlaceTypeAssignments(new HashSet<>(Arrays.asList(asgA, asgB, asgC)));

		final Set<PlaceTypeAssignment> expectedAssignments = new HashSet<>(Arrays.asList(asgA, asgB));

		Set<PlaceTypeAssignment> actualProperties = place.getPlaceTypeAssignmentsByType(typeA);
		assertThat(actualProperties, equalTo(expectedAssignments));
		actualProperties = place.getPlaceTypeAssignmentsByType(typeName);
		assertThat(actualProperties, equalTo(expectedAssignments));
	}

}
