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

import com.vividsolutions.jts.geom.Geometry;

/**
 * This class models the footprint of a {@link Place} entity.
 * <p>
 * Each {@link Footprint} instance has a non-null {@link Place} entity, a non-null geometry, and an optional precision
 * value. The constraints are not enforced by the class accessors itself, but by the JPA provider.
 * 
 * @author lrichter
 *
 */
@Entity
@Table(name = "footprint")
@PrimaryKeyJoinColumn(foreignKey = @ForeignKey(name = "footprint_entity_id_fk") )
public class Footprint extends AbstractEntity {

	/**
	 * The Spatial Reference System that is used for all geometries. 4326 is a widely used standard for geographical
	 * information spanning the whole world.
	 */
	public final static int REFERENCE_SYSTEM_SRID = 4326;

	@Column(name = "prec") // "precision" is a reserved word in SQL standard
	private Double precision;

	@Column(name = "geom", columnDefinition = "Geometry(Geometry," + REFERENCE_SYSTEM_SRID + ")", nullable = false)
	private Geometry geometry;

	@ManyToOne(fetch = FetchType.LAZY, optional = false)
	@JoinColumn(name = "place_id", foreignKey = @ForeignKey(name = "footprint_place_fk") )
	private Place place;

	/**
	 * Create an instance of {@link Footprint} with no assigned information.
	 */
	protected Footprint() {
		super();
	}

	/**
	 * Create an instance of {@link Footprint} with the given parameters.
	 * 
	 * @param geometry the geometry of the footprint.
	 * @param precision the precision estimate for the geometry.
	 * @param place the associated {@link Place} entity.
	 * @param validTime an associated {@link ValidTime} entity.
	 * @param provenance an associated {@link Provenance} entity.
	 */
	public Footprint(final Geometry geometry, final Double precision, final Place place, final ValidTime validTime,
			final Provenance provenance) {
		super(validTime, provenance);
		setGeometry(geometry);
		setPlace(place);
		setPrecision(precision);
	}

	public Double getPrecision() {
		return precision;
	}

	public void setPrecision(final Double precision) {
		this.precision = precision;
	}

	public Geometry getGeometry() {
		return geometry;
	}

	public void setGeometry(final Geometry geometry) {
		this.geometry = geometry;
	}

	public Place getPlace() {
		return place;
	}

	/**
	 * Set the {@link Place} entity to which this footprint is related to.
	 * <p>
	 * <b>Note:</b> If not <code>null</code>, automatically adjusts the passed {@link Place} instance such that it links
	 * to this footprint.<br>
	 * <b>Note:</b> If the previous {@link Place} instance was not <code>null</code>, its link to this footprint is
	 * removed.
	 * 
	 * @param place the new {@link Place} instance this entity is associated with.
	 */
	public void setPlace(final Place place) {
		if (this.place != place) {
			if (this.place != null) {
				this.place.removeFootprint(this);
			}

			this.place = place;

			if (place != null) {
				place.addFootprint(this);
			}
		}
	}

	@Override
	public String toString() {
		return "Footprint [geometry=" + geometry + ", place_id=" + ((place != null) ? place.getId() : "null")
				+ ", precision=" + precision + ", " + super.toString() + "]";
	}

}
