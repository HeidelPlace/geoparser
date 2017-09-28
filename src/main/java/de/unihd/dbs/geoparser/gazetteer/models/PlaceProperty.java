package de.unihd.dbs.geoparser.gazetteer.models;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.ForeignKey;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.PrimaryKeyJoinColumn;
import javax.persistence.Table;

import de.unihd.dbs.geoparser.gazetteer.models.AbstractEntity.ValidTime;

/**
 * This class models a property that is assigned to a {@link Place} entity.
 * <p>
 * Each {@link PlaceProperty} instance is associated with a non-null {@link Place} entity and a non-null
 * {@link PlacePropertyType} instance. An optional value may be assigned. The constraints are not enforced by the class
 * accessors itself, but by the JPA provider.
 * 
 * @author lrichter
 */
@Entity
@Table(name = "place_property")
@PrimaryKeyJoinColumn(foreignKey = @ForeignKey(name = "property_entity_fk") )
public class PlaceProperty extends AbstractEntity {

	@Column(name = "value", columnDefinition = "text")
	private String value;

	@ManyToOne(fetch = FetchType.LAZY, optional = false)
	@JoinColumn(name = "type_id", foreignKey = @ForeignKey(name = "place_property_place_property_type_fk") )
	private PlacePropertyType type;

	@ManyToOne(fetch = FetchType.LAZY, optional = false)
	@JoinColumn(name = "place_id", foreignKey = @ForeignKey(name = "place_property_place_fk") )
	private Place place;

	/**
	 * Create an instance of {@link PlaceProperty} with no assigned information.
	 */
	public PlaceProperty() {
		super();
	}

	/**
	 * Create an instance of {@link PlaceProperty} with the given parameters.
	 * 
	 * @param value the value assigned to the property.
	 * @param type the associated {@link PlacePropertyType} entity.
	 * @param place the associated {@link Place} entity.
	 * @param validTime an associated {@link ValidTime} entity.
	 * @param provenance an associated {@link Provenance} entity.
	 */
	public PlaceProperty(final String value, final PlacePropertyType type, final Place place, final ValidTime validTime,
			final Provenance provenance) {
		super(validTime, provenance);
		setPlace(place);
		setType(type);
		setValue(value);
	}

	public String getValue() {
		return value;
	}

	public void setValue(final String value) {
		this.value = value;
	}

	public PlacePropertyType getType() {
		return type;
	}

	public void setType(final PlacePropertyType type) {
		this.type = type;
	}

	public Place getPlace() {
		return place;
	}

	/**
	 * Set the {@link Place} entity to which this property is related to.
	 * <p>
	 * <b>Note</b>: If not <code>null</code>, automatically adjusts the passed {@link Place} instance such that it links
	 * to this property.<br>
	 * <b>Note</b>: If the previous {@link Place} instance was not <code>null</code>, its link to this property is
	 * removed.
	 * 
	 * @param place the new {@link Place} instance this entity is associated with.
	 */
	public void setPlace(final Place place) {
		if (this.place != place) {
			if (this.place != null) {
				this.place.removePlaceProperty(this);
			}

			this.place = place;

			if (place != null) {
				place.addProperty(this);
			}
		}
	}

	@Override
	public String toString() {
		return "PlaceProperty [value=" + value + ", type=" + type + ", place_id="
				+ ((place != null) ? place.getId() : "null") + ", " + super.toString() + "]";
	}

}