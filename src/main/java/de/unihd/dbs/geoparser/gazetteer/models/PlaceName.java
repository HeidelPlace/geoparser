package de.unihd.dbs.geoparser.gazetteer.models;

import java.util.EnumSet;
import java.util.Objects;

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
 * This class models a place name that is assigned to a {@link Place} entity.
 * <p>
 * Each {@link PlaceName} instance has a non-null {@link Place} entity, a non-null name, an optional language as well as
 * a set of {@link NameFlag}s. Each supported flag may be <code>null</code>, <code>true</code>, or <code>false</code>.
 * The constraints are not enforced by the class accessors itself, but by the JPA provider.
 * 
 * @author lrichter
 */
@Entity
@Table(name = "place_name")
@PrimaryKeyJoinColumn(foreignKey = @ForeignKey(name = "place_name_entity_id_fk") )
public class PlaceName extends AbstractEntity {

	/**
	 * Set of supported flags used to classify a place name.
	 * 
	 * @author lrichter
	 */
	// IMPORTANT: if you change the enum, you need to add/remove the respective boolean-fields and you need to adjust
	// getNameFlags() and setNameFlags() accordingly
	public enum NameFlag {
		IS_PREFERRED, IS_COLLOQUIAL, IS_HISTORICAL, IS_ABBREVIATION, IS_OFFICIAL, IS_NOT_PREFERRED, IS_NOT_COLLOQUIAL,
		IS_NOT_HISTORICAL, IS_NOT_ABBREVIATION, IS_NOT_OFFICIAL,
	}

	@Column(name = "name", nullable = false, columnDefinition = "text")
	private String name;

	@Column(name = "iso_language", columnDefinition = "text")
	private String language;

	@Column(name = "is_abbreviation")
	private Boolean isAbbreviation;

	@Column(name = "is_colloquial")
	private Boolean isColloquial;

	@Column(name = "is_historical")
	private Boolean isHistorical;

	@Column(name = "is_official")
	private Boolean isOfficial;

	@Column(name = "is_preferred")
	private Boolean isPreferred;

	@ManyToOne(fetch = FetchType.LAZY, optional = false)
	@JoinColumn(name = "place_id", foreignKey = @ForeignKey(name = "place_name_place_fk") )
	private Place place;

	/**
	 * Create an instance of {@link PlaceName} with no assigned information.
	 */
	public PlaceName() {
		super();
	}

	/**
	 * Create an instance of {@link PlaceName} with the given parameters.
	 * 
	 * @param name the place name.
	 * @param language the language, in which the name is used.
	 * @param nameFlags a set of {@link NameFlag}s.
	 * @param place the associated {@link Place} entity.
	 * @param validTime an associated {@link ValidTime} entity.
	 * @param provenance an associated {@link Provenance} entity.
	 */
	public PlaceName(final String name, final String language, final EnumSet<NameFlag> nameFlags, final Place place,
			final ValidTime validTime, final Provenance provenance) {
		super(validTime, provenance);
		setLanguage(language);
		setName(name);
		setNameFlags(nameFlags);
		setPlace(place);
	}

	public String getName() {
		return name;
	}

	public void setName(final String name) {
		this.name = name;
	}

	public String getLanguage() {
		return language;
	}

	public void setLanguage(final String language) {
		this.language = language;
	}

	public EnumSet<NameFlag> getNameFlags() {
		final EnumSet<NameFlag> flags = EnumSet.noneOf(NameFlag.class);
		if (isAbbreviation != null) {
			flags.add(isAbbreviation ? NameFlag.IS_ABBREVIATION : NameFlag.IS_NOT_ABBREVIATION);
		}
		if (isColloquial != null) {
			flags.add(isColloquial ? NameFlag.IS_COLLOQUIAL : NameFlag.IS_NOT_COLLOQUIAL);
		}
		if (isHistorical != null) {
			flags.add(isHistorical ? NameFlag.IS_HISTORICAL : NameFlag.IS_NOT_HISTORICAL);
		}
		if (isOfficial != null) {
			flags.add(isOfficial ? NameFlag.IS_OFFICIAL : NameFlag.IS_NOT_OFFICIAL);
		}
		if (isPreferred != null) {
			flags.add(isPreferred ? NameFlag.IS_PREFERRED : NameFlag.IS_NOT_PREFERRED);
		}

		return flags;
	}

	public void setNameFlags(final EnumSet<NameFlag> nameFlags) {
		Objects.requireNonNull(nameFlags);

		if (nameFlags.contains(NameFlag.IS_ABBREVIATION)) {
			this.isAbbreviation = true;
		}
		else if (nameFlags.contains(NameFlag.IS_NOT_ABBREVIATION)) {
			this.isAbbreviation = false;
		}
		else {
			this.isAbbreviation = null;
		}

		if (nameFlags.contains(NameFlag.IS_COLLOQUIAL)) {
			this.isColloquial = true;
		}
		else if (nameFlags.contains(NameFlag.IS_NOT_COLLOQUIAL)) {
			this.isColloquial = false;
		}
		else {
			this.isColloquial = null;
		}

		if (nameFlags.contains(NameFlag.IS_HISTORICAL)) {
			this.isHistorical = true;
		}
		else if (nameFlags.contains(NameFlag.IS_NOT_HISTORICAL)) {
			this.isHistorical = false;
		}
		else {
			this.isHistorical = null;
		}

		if (nameFlags.contains(NameFlag.IS_OFFICIAL)) {
			this.isOfficial = true;
		}
		else if (nameFlags.contains(NameFlag.IS_NOT_OFFICIAL)) {
			this.isOfficial = false;
		}
		else {
			this.isOfficial = null;
		}

		if (nameFlags.contains(NameFlag.IS_PREFERRED)) {
			this.isPreferred = true;
		}
		else if (nameFlags.contains(NameFlag.IS_NOT_PREFERRED)) {
			this.isPreferred = false;
		}
		else {
			this.isPreferred = null;
		}
	}

	public Place getPlace() {
		return place;
	}

	/**
	 * Set the {@link Place} entity to which this place name is related to.
	 * <p>
	 * <b>Note:</b> If not <code>null</code>, automatically adjusts the passed {@link Place} instance such that it links
	 * to this place name.<br>
	 * <b>Note:</b> If the previous {@link Place} instance was not <code>null</code>, its link to this place name is
	 * removed.
	 * 
	 * @param place the new {@link Place} instance this entity is associated with.
	 */
	public void setPlace(final Place place) {
		if (this.place != place) {
			if (this.place != null) {
				this.place.removePlaceName(this);
			}

			this.place = place;

			if (place != null) {
				place.addPlaceName(this);
			}
		}
	}

	@Override
	public String toString() {
		return "PlaceName [name=" + name + ", language=" + language + ", isPreferred=" + isPreferred + ", isColloquial="
				+ isColloquial + ", isHistoric=" + isHistorical + ", isAbbreviation=" + isAbbreviation + ", isOfficial="
				+ isOfficial + ", place_id=" + ((place != null) ? place.getId() : "null") + ", " + super.toString()
				+ "]";
	}

}
