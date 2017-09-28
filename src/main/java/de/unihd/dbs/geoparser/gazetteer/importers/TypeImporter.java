package de.unihd.dbs.geoparser.gazetteer.importers;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

import javax.persistence.EntityManager;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import de.unihd.dbs.geoparser.gazetteer.models.PlacePropertyType;
import de.unihd.dbs.geoparser.gazetteer.models.PlaceRelationshipType;
import de.unihd.dbs.geoparser.gazetteer.models.PlaceType;
import de.unihd.dbs.geoparser.gazetteer.models.Type;
import de.unihd.dbs.geoparser.gazetteer.types.PlaceTypes;
import de.unihd.dbs.geoparser.gazetteer.types.PropertyTypes;
import de.unihd.dbs.geoparser.gazetteer.types.RelationshipTypes;

/**
 * Implementation of an importer for type information into our gazetteer schema, which is defined by the classes in
 * {@link de.unihd.dbs.geoparser.gazetteer.models}.
 *
 * @author lrichter
 *
 */
public class TypeImporter implements AutoCloseable {

	private static final Logger logger = LoggerFactory.getLogger(TypeImporter.class);

	// resources we need to connect to for data processing; resource management done by constructor and close()
	private final EntityManager entityManager; // current JPA session

	/**
	 * Create a {@link TypeImporter} and initialize all required resources.
	 *
	 * @param entityManager the {@link EntityManager} instance connected to the gazetteer
	 */
	public TypeImporter(final EntityManager entityManager) {
		this.entityManager = entityManager;
	}

	@Override
	public void close() {
		//
	}

	public void importTypes() {
		logger.info("=== importing types ===");
		logger.info("Creating types...");
		final List<Type> newTypes = new ArrayList<>();
		newTypes.addAll(createPlaceTypes());
		newTypes.addAll(createPropertyTypes());
		newTypes.addAll(createRelationshipTypes());

		logger.info("Importing types...");
		entityManager.getTransaction().begin();
		newTypes.forEach(type -> entityManager.persist(type));
		entityManager.getTransaction().commit();

		logger.info("- DONE -");
	}

	private static List<PlaceType> createPlaceTypes() {
		logger.info("Creating place types...");
		final List<PlaceTypes> relevantTypes = Arrays.asList(PlaceTypes.values());
		final List<PlaceType> placeTypes = relevantTypes.stream()
				.map(placeType -> new PlaceType(placeType.typeName, placeType.description, null, null, null))
				.collect(Collectors.toList());

		// add parent information
		for (final PlaceType placeType : placeTypes) {
			final PlaceTypes parent = PlaceTypes.getByName(placeType.getName()).parent;
			if (parent != null) {
				placeType.setParentType(placeTypes.stream().filter(type -> type.getName().equals(parent.typeName))
						.collect(Collectors.toSet()).iterator().next());
			}
		}

		return placeTypes;
	}

	private static List<PlacePropertyType> createPropertyTypes() {
		logger.info("Creating property types...");
		final List<PropertyTypes> relevantTypes = Arrays.asList(PropertyTypes.values());
		final List<PlacePropertyType> propertyTypes = relevantTypes.stream()
				.map(propertyType -> new PlacePropertyType(propertyType.typeName, propertyType.description, null, null,
						null))
				.collect(Collectors.toList());

		// add parent information
		for (final PlacePropertyType propertyType : propertyTypes) {
			final PropertyTypes parent = PropertyTypes.getByName(propertyType.getName()).parent;
			if (parent != null) {
				propertyType.setParentType(propertyTypes.stream().filter(type -> type.getName().equals(parent.typeName))
						.collect(Collectors.toSet()).iterator().next());
			}
		}

		return propertyTypes;
	}

	private static List<PlaceRelationshipType> createRelationshipTypes() {
		logger.info("Creating relationship types...");
		final List<RelationshipTypes> relevantTypes = Arrays.asList(RelationshipTypes.CAPITAL_OF,
				RelationshipTypes.SEAT_OF, RelationshipTypes.SUBDIVISION, RelationshipTypes.WITHIN_DIVISION);
		final List<PlaceRelationshipType> relationTypes = relevantTypes.stream()
				.map(relationType -> new PlaceRelationshipType(relationType.typeName, relationType.description, null,
						null, null))
				.collect(Collectors.toList());

		// add parent information
		for (final PlaceRelationshipType relationType : relationTypes) {
			final RelationshipTypes parent = RelationshipTypes.getByName(relationType.getName()).parent;
			if (parent != null) {
				relationType.setParentType(relationTypes.stream().filter(type -> type.getName().equals(parent.typeName))
						.collect(Collectors.toSet()).iterator().next());
			}
		}

		return relationTypes;
	}

}
