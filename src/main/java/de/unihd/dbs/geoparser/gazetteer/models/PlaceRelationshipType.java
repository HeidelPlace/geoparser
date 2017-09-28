package de.unihd.dbs.geoparser.gazetteer.models;

import java.util.Set;

import javax.persistence.DiscriminatorValue;
import javax.persistence.Entity;

/**
 * This class models types that are used to classify {@link PlaceRelationship} instances.
 * <p>
 * It inherits all functionality from the base class {@link Type}. For each {@link PlaceRelationshipType} instance, the
 * discriminator field "type" is set to <code>PLACE_RELATIONSHIP_TYPE</code>.
 * 
 * @author lrichter
 */
@Entity
@DiscriminatorValue("PLACE_RELATIONSHIP_TYPE")
public class PlaceRelationshipType extends Type {

	public PlaceRelationshipType() {
		super();
	}

	public PlaceRelationshipType(final String name, final String description, final Set<Type> childTypes,
			final Type parentType, final Set<Type> similiarTypes) {
		super(name, description, childTypes, parentType, similiarTypes);
	}
}