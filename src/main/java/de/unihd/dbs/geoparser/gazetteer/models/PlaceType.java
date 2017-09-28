package de.unihd.dbs.geoparser.gazetteer.models;

import java.util.Set;

import javax.persistence.DiscriminatorValue;
import javax.persistence.Entity;

/**
 * This class models types that are used to classify {@link PlaceTypeAssignment} instances.
 * <p>
 * It inherits all functionality from the base class {@link Type}. For each {@link PlaceType} instance, the
 * discriminator field "type" is set to <code>PLACE_TYPE</code>.
 * 
 * @author lrichter
 */
@Entity
@DiscriminatorValue("PLACE_TYPE")
public class PlaceType extends Type {

	public PlaceType() {
		super();
	}

	public PlaceType(final String name, final String description, final Set<Type> childTypes, final Type parentType,
			final Set<Type> similiarTypes) {
		super(name, description, childTypes, parentType, similiarTypes);
	}

}