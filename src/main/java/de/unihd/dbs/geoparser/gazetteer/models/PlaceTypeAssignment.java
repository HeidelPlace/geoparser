package de.unihd.dbs.geoparser.gazetteer.models;

import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.ForeignKey;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.PrimaryKeyJoinColumn;
import javax.persistence.Table;

import de.unihd.dbs.geoparser.gazetteer.models.AbstractEntity.ValidTime;

/**
 * This class models a {@link PlaceType} assignment to a {@link Place} entity.
 * <p>
 * Each instance is associated with a non-null {@link Place} entity and a non-null {@link PlaceType} instance. The
 * constraints are not enforced by the class accessors itself, but by the JPA provider.
 * 
 * @author lrichter
 */
@Entity
@Table(name = "place_type_assignment")
@PrimaryKeyJoinColumn(foreignKey = @ForeignKey(name = "place_type_assignment_entity_id_fk") )
public class PlaceTypeAssignment extends AbstractEntity {

	@ManyToOne(fetch = FetchType.LAZY, optional = false)
	@JoinColumn(name = "type_id", foreignKey = @ForeignKey(name = "type_assignment_type_fk") )
	private PlaceType type;

	@ManyToOne(fetch = FetchType.LAZY, optional = false)
	@JoinColumn(name = "place_id", foreignKey = @ForeignKey(name = "type_assignment_place_fk") )
	private Place place;

	/**
	 * Create an instance of {@link PlaceTypeAssignment} with no assigned information.
	 */
	public PlaceTypeAssignment() {
		super();
	}

	/**
	 * Create an instance of {@link PlaceTypeAssignment} with the given parameters.
	 * 
	 * @param type the associated {@link PlaceType} entity.
	 * @param place the associated {@link Place} entity.
	 * @param validTime an associated {@link ValidTime} entity.
	 * @param provenance an associated {@link Provenance} entity.
	 */
	public PlaceTypeAssignment(final PlaceType type, final Place place, final ValidTime validTime,
			final Provenance provenance) {
		super(validTime, provenance);
		setPlace(place);
		setType(type);
	}

	public PlaceType getType() {
		return type;
	}

	public void setType(final PlaceType type) {
		this.type = type;
	}

	public Place getPlace() {
		return place;
	}

	/**
	 * Set the {@link Place} entity to which this place type assignment is related to.
	 * <p>
	 * <b>Note</b>: If not <code>null</code>, automatically adjusts the passed {@link Place} instance such that it links
	 * to this place type assignment.<br>
	 * <b>Note</b>: If the previous {@link Place} instance was not <code>null</code>, its link to this place type
	 * assignment <b>IS</b> removed.
	 * 
	 * @param place the new {@link Place} instance this entity is associated with.
	 */
	public void setPlace(final Place place) {
		if (this.place != place) {
			if (this.place != null) {
				this.place.removePlaceTypeAssignment(this);
			}

			this.place = place;

			if (place != null) {
				place.addPlaceTypeAssignment(this);
			}
		}
	}

	@Override
	public String toString() {
		return "PlaceTypeAssignment [type=" + type + ", place_id=" + ((place != null) ? place.getId() : "null") + ", "
				+ super.toString() + "]";
	}

}
