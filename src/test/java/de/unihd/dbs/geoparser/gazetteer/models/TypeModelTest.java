package de.unihd.dbs.geoparser.gazetteer.models;

import static org.hamcrest.Matchers.*;
import static org.junit.Assert.*;

import java.io.IOException;
import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import javax.persistence.EntityManager;

import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import de.unihd.dbs.geoparser.core.GeoparserConfig;
import de.unihd.dbs.geoparser.core.GeoparserConfig.UnknownConfigLabelException;
import de.unihd.dbs.geoparser.gazetteer.util.GazetteerPersistenceManager;

import com.jcraft.jsch.JSchException;

public class TypeModelTest {

	private static GazetteerPersistenceManager persistenceManager;
	private EntityManager entityManager;

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

	@Before
	public void setupPresistenceContext() {
		entityManager = persistenceManager.getEntityManager();
	}

	@After
	public void tearDownPersistenceContext() {
		if (entityManager != null) {
			entityManager.close();
			entityManager = null;
		}
	}

	private void assertNoArtifactsRemain() {
		final List<Type> types = entityManager.createQuery("FROM Type", Type.class).getResultList();
		assertThat(types, emptyCollectionOf(Type.class));
	}

	@Test
	public void testSimpleTypeConstructors() {
		final PlaceType placeType = new PlaceType();
		assertThat(placeType.getSimilarTypes(), emptyCollectionOf(Type.class));
		assertThat(placeType.getChildTypes(), emptyCollectionOf(Type.class));

		final PlacePropertyType propertyType = new PlacePropertyType();
		assertThat(propertyType.getSimilarTypes(), emptyCollectionOf(Type.class));
		assertThat(propertyType.getChildTypes(), emptyCollectionOf(Type.class));

		final PlaceRelationshipType relationshipType = new PlaceRelationshipType();
		assertThat(relationshipType.getSimilarTypes(), emptyCollectionOf(Type.class));
		assertThat(relationshipType.getChildTypes(), emptyCollectionOf(Type.class));
	}

	@Test
	public void testTypes() {
		final String placeTypeName = "default_place_type";
		final String propertyTypeName = "default_prop_type";
		final String relationshipTypeName = "default_rel_type";
		final List<String> expectedNames = Arrays.asList(placeTypeName, propertyTypeName, relationshipTypeName);
		final String placeTypeDescription = "default place";
		final String propertyTypeDescription = "default prop";
		final String relationshipTypeDescription = "default rel";
		final List<String> expectedDescriptions = Arrays.asList(placeTypeDescription, propertyTypeDescription,
				relationshipTypeDescription);

		// test creation
		final PlaceType placeType = new PlaceType(placeTypeName, placeTypeDescription, null, null, null);
		final PlacePropertyType propertyType = new PlacePropertyType(propertyTypeName, propertyTypeDescription, null,
				null, null);
		final PlaceRelationshipType relationshipType = new PlaceRelationshipType(relationshipTypeName,
				relationshipTypeDescription, null, null, null);

		final List<Type> typeInstances = Arrays.asList(placeType, propertyType, relationshipType);

		// test persistence
		entityManager.getTransaction().begin();
		typeInstances.forEach(typeInstance -> entityManager.persist(typeInstance));
		entityManager.getTransaction().commit();

		// test attributes
		int i = 0;
		for (final Type typeInstance : typeInstances) {
			assertThat(typeInstance.getId(), not(nullValue())); // only valid after persistence!
			assertThat(typeInstance.getName(), equalTo(expectedNames.get(i)));
			assertThat(typeInstance.getDescription(), equalTo(expectedDescriptions.get(i)));
			assertThat(typeInstance.getParentType(), nullValue());
			assertThat(typeInstance.getAllChildren(), emptyCollectionOf(Type.class));
			assertThat(typeInstance.getAllParents(), emptyCollectionOf(Type.class));
			assertThat(typeInstance.getChildTypes(), emptyCollectionOf(Type.class));
			assertThat(typeInstance.getSimilarTypes(), emptyCollectionOf(Type.class));
			i++;
		}

		// smoke test of toString()
		placeType.toString();
		propertyType.toString();
		relationshipType.toString();

		// test updates
		final List<String> newExpectedNames = Arrays.asList("new_default_place_type", "new_default_prop_type",
				"new_default_rel_type");
		final List<String> newExpectedDescriptions = Arrays.asList("new default place", "new default prop",
				"new default rel");
		i = 0;
		entityManager.getTransaction().begin();
		for (final Type typeInstance : typeInstances) {
			typeInstance.setName(newExpectedNames.get(i));
			typeInstance.setDescription(newExpectedDescriptions.get(i));
			entityManager.merge(typeInstance);
			i++;
		}
		entityManager.getTransaction().commit();

		// test deletion
		entityManager.getTransaction().begin();
		typeInstances.forEach(typeInstance -> GazetteerPersistenceManager.removeType(entityManager, typeInstance));
		entityManager.getTransaction().commit();

		assertNoArtifactsRemain();
	}

	@Test
	public void testTypeHierarchy() throws InstantiationException, IllegalAccessException, IllegalArgumentException,
			InvocationTargetException, NoSuchMethodException, SecurityException {
		final List<Type> typeInstances = new ArrayList<>();

		final int width = 2;
		final int depth = 3;

		// recursively create type hierarchies
		final PlaceType rootPlaceType = (PlaceType) createTypeHierarchy(PlaceType.class, depth, 0, width,
				typeInstances);
		final PlacePropertyType rootPropertyType = (PlacePropertyType) createTypeHierarchy(PlacePropertyType.class,
				depth, 0, width, typeInstances);
		final PlaceRelationshipType rootRelationshipType = (PlaceRelationshipType) createTypeHierarchy(
				PlaceRelationshipType.class, depth, 0, width, typeInstances);

		final List<Type> rootTypes = Arrays.asList(rootPlaceType, rootPropertyType, rootRelationshipType);

		// run cascaded persistence
		entityManager.getTransaction().begin();
		rootTypes.forEach(rootType -> entityManager.persist(rootType));
		entityManager.getTransaction().commit();

		// check parent-child attributes
		final int expectedAllChildrenCount = (int) Math.pow(width, depth) - 2;
		for (final Type rootType : rootTypes) {
			rootTypes.forEach(rootType_ -> assertFalse(rootType.isChildOf(rootType_)));

			final Set<Type> children = rootType.getChildTypes();
			assertThat(children.size(), equalTo(width));

			children.forEach(child -> assertThat(child.getParentType(), equalTo(rootType)));

			final Set<Type> allChildren = rootType.getAllChildren();
			assertThat(allChildren.size(), equalTo(expectedAllChildrenCount));
			allChildren.forEach(child -> assertTrue(child.isChildOf(rootType)));
			allChildren.forEach(child -> assertTrue(rootType.hasChild(child, true)));
			allChildren.forEach(child -> assertThat(child.getAllParents(), not(emptyCollectionOf(Type.class))));

			// smoke test of toString() for a child
			children.iterator().next().toString();
		}

		// update attributes
		entityManager.getTransaction().begin();
		for (final Type rootType : rootTypes) {
			final Type someChild = rootType.getChildTypes().iterator().next();
			final int originalSize = rootType.getChildTypes().size();

			// add type that already exists
			rootType.addChildType(someChild);
			assertThat(rootType.getChildTypes().size(), equalTo(originalSize));

			// add same type
			rootType.addChildType(rootType);
			assertThat(rootType.getChildTypes().size(), equalTo(originalSize));

			// set same child types
			final Set<Type> childTypes = rootType.getChildTypes();
			rootType.setChildTypes(rootType.getChildTypes());
			assertThat(rootType.getChildTypes(), equalTo(childTypes));

			// remove existing child
			rootType.removeChildType(someChild);
			assertFalse(rootType.hasChild(someChild));
			assertFalse(someChild.isChildOf(rootType));

			// remove non-listed child
			rootType.removeChildType(someChild);

			// pass null set
			rootType.setChildTypes(null);
			assertThat(rootType.getChildTypes(), emptyCollectionOf(Type.class));
			assertThat(rootType.getAllChildren(), emptyCollectionOf(Type.class));
		}
		entityManager.getTransaction().commit();

		// remove types
		entityManager.getTransaction().begin();
		typeInstances.forEach(typeInstance -> GazetteerPersistenceManager.removeType(entityManager, typeInstance));
		entityManager.getTransaction().commit();

		assertNoArtifactsRemain();
	}

	private int typeId = 1; // counter needed to create unique type names

	private Type createTypeHierarchy(final Class<? extends Type> typeClass, final int depth, final int x,
			final int width, final List<Type> types) throws InstantiationException, IllegalAccessException,
			IllegalArgumentException, InvocationTargetException, NoSuchMethodException, SecurityException {
		final Constructor<?> constructor = typeClass.getConstructor(String.class, String.class, Set.class, Type.class,
				Set.class);

		final Set<Type> children = new HashSet<>();
		if (depth > 0) {
			for (int i = 0; i < width; i++) {
				children.add(createTypeHierarchy(typeClass, depth - 1, i, width, types));
			}
		}

		final Type node = (Type) constructor
				.newInstance(new Object[] { "Type " + depth + ":" + (x + 1) + "/" + width + " (" + typeId + ")",
						"Hierarchy Test Type", children, null, null });
		typeId++;
		types.add(node);
		return node;
	}

	@Test
	public void testSimilarTypes() {
		final PlaceType typeA = new PlaceType("place_type", "similar type A", null, null, null);
		final PlacePropertyType typeB = new PlacePropertyType("prop_type", "similar type B", null, null, null);
		final PlaceRelationshipType typeC = new PlaceRelationshipType("rel_type", "similar type C", null, null, null);
		final Set<Type> similarTypes = new HashSet<>(Arrays.asList(typeB, typeC));

		entityManager.getTransaction().begin();
		entityManager.persist(typeA);
		entityManager.persist(typeB);
		entityManager.persist(typeC);
		entityManager.getTransaction().commit();

		// establish relationship
		entityManager.getTransaction().begin();
		typeA.setSimilarTypes(similarTypes);
		assertThat(typeA.getSimilarTypes(), containsInAnyOrder(Arrays.asList(typeB, typeC).toArray()));
		entityManager.getTransaction().commit();

		// smoke test of toString()
		typeA.toString();
		typeB.toString();
		typeC.toString();

		// modify relationships
		entityManager.getTransaction().begin();
		final int orginalSize = typeA.getSimilarTypes().size();

		// add already existing type
		typeA.addSimilarType(typeB);
		assertThat(typeA.getSimilarTypes().size(), equalTo(orginalSize));

		// add self type
		typeA.addSimilarType(typeA);
		assertThat(typeA.getSimilarTypes().size(), equalTo(orginalSize));

		// set same similar type
		final Set<Type> similarTypesTmp = typeA.getSimilarTypes();
		typeA.setSimilarTypes(typeA.getSimilarTypes());
		assertThat(typeA.getSimilarTypes(), equalTo(similarTypesTmp));

		// remove existing similar type
		typeA.removeSimilarType(typeB);
		assertThat(typeA.getSimilarTypes(), not(contains(typeB)));

		// removing non-listed similar type
		typeA.removeSimilarType(typeB);

		// pass null set
		typeA.setSimilarTypes(null);
		assertThat(typeA.getSimilarTypes(), emptyCollectionOf(Type.class));

		entityManager.getTransaction().commit();

		// clean up
		entityManager.getTransaction().begin();
		entityManager.remove(typeA);
		entityManager.remove(typeB);
		entityManager.remove(typeC);
		entityManager.getTransaction().commit();

		assertNoArtifactsRemain();
	}

	@Test
	public void testTypeEquality() {
		final PlaceType typeA = new PlaceType("place_type_a", "similar type", null, null, null);
		final PlacePropertyType typeB = new PlacePropertyType("place_type_b", "similar type", null, null, null);

		assertThat(typeA, not(equalTo(typeB)));
		assertThat(typeB, not(equalTo(typeA)));
		assertThat(typeA, equalTo(typeA));

		entityManager.getTransaction().begin();
		entityManager.persist(typeA);
		entityManager.persist(typeB);
		entityManager.getTransaction().commit();

		assertThat(typeA, not(equalTo(typeB)));
		assertThat(typeB, not(equalTo(typeA)));
		assertThat(typeA, equalTo(typeA));
		assertThat(typeA, not(equalTo(null)));

		entityManager.detach(typeA);
		final PlaceType typeA_new = entityManager.find(PlaceType.class, typeA.getId());

		assertThat(typeA, equalTo(typeA_new));

		// clean up
		entityManager.getTransaction().begin();
		entityManager.remove(typeA_new);
		entityManager.remove(typeB);
		entityManager.getTransaction().commit();

		assertNoArtifactsRemain();
	}

}
