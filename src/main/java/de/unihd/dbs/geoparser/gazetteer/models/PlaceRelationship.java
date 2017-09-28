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
import de.unihd.dbs.geoparser.gazetteer.util.GazetteerPersistenceManager;

/**
 * This class models a {@link PlaceRelationship} between two {@link Place} entities.
 * <p>
 * Each instance is associated with a non-null {@link Place} entity, a non-null {@link PlaceRelationshipType} instance
 * as well as a left-side and right- side {@link Place} entity. Furthermore, it may have an optional value assigned. The
 * constraints are not enforced by the class accessors itself, but by the JPA provider.
 * <p>
 * To remove a place relationship from the persistency store, you should use
 * {@link GazetteerPersistenceManager#removePlaceRelationship}!
 * 
 * @author lrichter
 */
@Entity
@Table(name = "place_relationship")
@PrimaryKeyJoinColumn(foreignKey = @ForeignKey(name = "place_relationship_entity_id_fk") )
public class PlaceRelationship extends AbstractEntity {

	@ManyToOne(fetch = FetchType.LAZY, optional = false)
	@JoinColumn(name = "left_place_id", foreignKey = @ForeignKey(name = "place_relationship_place_1_fk") )
	private Place leftPlace;

	@ManyToOne(fetch = FetchType.LAZY, optional = false)
	@JoinColumn(name = "right_place_id", foreignKey = @ForeignKey(name = "place_relationship_place_2_fk") )
	private Place rightPlace;

	@ManyToOne(fetch = FetchType.LAZY, optional = false)
	@JoinColumn(name = "type_id", foreignKey = @ForeignKey(name = "place_relationship_place_relationship_type_fk") )
	private PlaceRelationshipType type;

	@Column(name = "value", columnDefinition = "text")
	private String value;

	/**
	 * Create an instance of {@link PlaceRelationship} with no assigned information.
	 */
	public PlaceRelationship() {
		super();
	}

	/**
	 * Create an instance of {@link PlaceRelationship} with the given parameters.
	 * 
	 * @param leftPlace the left-side {@link Place} entity.
	 * @param rightPlace the right-side {@link Place} entity.
	 * @param value the value assigned to the relationship.
	 * @param type the associated {@link PlaceRelationshipType} entity.
	 * @param validTime an associated {@link ValidTime} entity.
	 * @param provenance an associated {@link Provenance} entity.
	 */
	public PlaceRelationship(final Place leftPlace, final Place rightPlace, final PlaceRelationshipType type,
			final String value, final ValidTime validTime, final Provenance provenance) {
		super(validTime, provenance);
		setLeftPlace(leftPlace);
		setRightPlace(rightPlace);
		setType(type);
		setValue(value);
	}

	public Place getLeftPlace() {
		return leftPlace;
	}

	/**
	 * Set the left-side {@link Place} entity.
	 * <p>
	 * <b>Note:</b> If not <code>null</code>, automatically adds this relationship as left-side relationship to the
	 * given {@link Place} instance.<br>
	 * <b>Note:</b> If the previous {@link Place} instance was not <code>null</code>, this relationship <b>IS</b>
	 * removed from its set of left-side relationships.
	 *
	 * @param leftPlace the new left-side {@link Place} instance.
	 */
	public void setLeftPlace(final Place leftPlace) {
		if (this.leftPlace != leftPlace) {
			if (this.leftPlace != null) {
				this.leftPlace.removeLeftPlaceRelationship(this);
			}

			this.leftPlace = leftPlace;

			if (leftPlace != null) {
				leftPlace.addLeftPlaceRelationship(this);
			}
		}
	}

	public Place getRightPlace() {
		return rightPlace;
	}

	/**
	 * Set the right-side {@link Place} entity.
	 * <p>
	 * <b>Note:</b> If not <code>null</code>, automatically adds this relationship as right-side relationship to the
	 * given {@link Place} instance.<br>
	 * <b>Note:</b> If the previous {@link Place} instance was not <code>null</code>, this relationship is removed from
	 * its set of right-side relationships.
	 *
	 * @param rightPlace the new right-side {@link Place} instance.
	 */
	public void setRightPlace(final Place rightPlace) {
		if (this.rightPlace != rightPlace) {
			if (this.rightPlace != null) {
				this.rightPlace.removeRightPlaceRelationship(this);
			}

			this.rightPlace = rightPlace;

			if (rightPlace != null) {
				rightPlace.addRightPlaceRelationship(this);
			}
		}
	}

	public PlaceRelationshipType getType() {
		return type;
	}

	public void setType(final PlaceRelationshipType type) {
		this.type = type;
	}

	public String getValue() {
		return value;
	}

	public void setValue(final String value) {
		this.value = value;
	}

	@Override
	public String toString() {
		return "PlaceRelationship [" + super.toString() + ", type=" + type + ", leftPlace_id="
				+ ((leftPlace != null) ? leftPlace.getId() : "null") + ", rightPlace_id="
				+ ((rightPlace != null) ? rightPlace.getId() : "null") + ", value=" + value + ", " + super.toString()
				+ "]";
	}

}
