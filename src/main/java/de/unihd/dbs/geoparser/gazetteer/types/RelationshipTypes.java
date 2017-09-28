package de.unihd.dbs.geoparser.gazetteer.types;

import de.unihd.dbs.geoparser.gazetteer.models.PlaceRelationship;

/**
 * Type definitions for {@link PlaceRelationship} entities.
 *
 * @author lrichter
 *
 */
public enum RelationshipTypes {
	//@formatter:off
	SUBDIVISION("subdivision", "A left-Side place is a administrative subdivision of a right-side place"),
	WITHIN_DIVISION("within_devision", "A left-Side place is within the administrative division defined by a right-side place"),
	CAPITAL_OF("capital_of", "A left-Side place is a capital of the political entity defined by a right-side place"),
	SEAT_OF("seat_of", "A left-Side place is a seat of the administrative division defined by a right-side place");
	//@formatter:on
	public final String typeName;
	public final String description;
	public final RelationshipTypes parent;

	private RelationshipTypes(final String typeName, final String typeDescription) {
		this(typeName, typeDescription, null);
	}

	private RelationshipTypes(final String typeName, final String typeDescription, final RelationshipTypes parent) {
		this.parent = parent;
		this.typeName = typeName;
		this.description = typeDescription;
	}

	public static RelationshipTypes getByName(final String typeName) {
		for (final RelationshipTypes type : RelationshipTypes.values()) {
			if (type.typeName.equals(typeName)) {
				return type;
			}
		}
		throw new IllegalArgumentException("No enum constant RelationshipTypes with typeName " + typeName + "!");
	}
}