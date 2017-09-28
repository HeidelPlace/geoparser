package de.unihd.dbs.geoparser.gazetteer.models;

import java.util.Set;

import javax.persistence.DiscriminatorValue;
import javax.persistence.Entity;

/**
 * This class models types that are used to classify {@link PlaceProperty} instances.
 * <p>
 * It inherits all functionality from the base class {@link Type}. For each {@link PlacePropertyType} instance, the
 * discriminator field "type" is set to <code>PROPERTY_TYPE</code>.
 * 
 * @author lrichter
 */
@Entity
@DiscriminatorValue("PROPERTY_TYPE")
public class PlacePropertyType extends Type {

	public PlacePropertyType() {
		super();
	}

	public PlacePropertyType(final String name, final String description, final Set<Type> childTypes,
			final Type parentType, final Set<Type> similiarTypes) {
		super(name, description, childTypes, parentType, similiarTypes);
	}

}
