package de.unihd.dbs.geoparser.gazetteer.models;

import java.util.Collections;
import java.util.HashSet;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

import javax.persistence.CascadeType;
import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.ForeignKey;
import javax.persistence.OneToMany;
import javax.persistence.PrimaryKeyJoinColumn;
import javax.persistence.Table;

import de.unihd.dbs.geoparser.gazetteer.models.PlaceName.NameFlag;

/**
 * This class models a place in the gazetteer data model.
 * <p>
 * Each {@code Place} instance must have at least on place name and footprint, but may have any number properties, place
 * type assignments and relationships. The constraints are not enforced by the class accessors itself, but by the JPA
 * provider. If any of the assigned entity instances is changed to another instance, the previous instance is
 * automatically removed from the database via JPA during persistence (orphanRemoval)!
 * <p>
 *
 * @author lrichter
 */
@Entity
@Table(name = "place")
@PrimaryKeyJoinColumn(foreignKey = @ForeignKey(name = "place_entity_id_fk"))
public class Place extends AbstractEntity {

	@OneToMany(cascade = { CascadeType.ALL }, fetch = FetchType.LAZY, mappedBy = "place", orphanRemoval = true)
	private Set<Footprint> footprints;

	@OneToMany(cascade = { CascadeType.ALL }, fetch = FetchType.LAZY, mappedBy = "place", orphanRemoval = true)
	private Set<PlaceName> placeNames;

	@OneToMany(cascade = { CascadeType.ALL }, fetch = FetchType.LAZY, mappedBy = "place", orphanRemoval = true)
	private Set<PlaceProperty> properties;

	@OneToMany(cascade = { CascadeType.ALL }, fetch = FetchType.LAZY, mappedBy = "place", orphanRemoval = true)
	private Set<PlaceTypeAssignment> placeTypeAssignments;

	@OneToMany(cascade = { CascadeType.ALL }, fetch = FetchType.LAZY, mappedBy = "leftPlace", orphanRemoval = true)
	private Set<PlaceRelationship> leftPlaceRelationships;

	@OneToMany(cascade = { CascadeType.ALL }, fetch = FetchType.LAZY, mappedBy = "rightPlace", orphanRemoval = true)
	private Set<PlaceRelationship> rightPlaceRelationships;

	/**
	 * Create an instance of {@link Place} without any information.
	 */
	public Place() {
		super();
		init();
	}

	/**
	 * Create an instance of {@link Place} with the given parameters.
	 *
	 * @param footprints the footprints of the place.
	 * @param names the place names.
	 * @param properties the place properties.
	 * @param placeTypeAssignments the place type assignments.
	 * @param leftRelationships the left place relationships.
	 * @param rightRelationships the right place relationships.
	 * @param validTime the valid time for the place.
	 * @param provenance the provenance for the place.
	 */
	public Place(final Set<Footprint> footprints, final Set<PlaceName> names, final Set<PlaceProperty> properties,
			final Set<PlaceTypeAssignment> placeTypeAssignments, final Set<PlaceRelationship> leftRelationships,
			final Set<PlaceRelationship> rightRelationships, final ValidTime validTime, final Provenance provenance) {
		super(validTime, provenance);
		init();
		setFootprints(footprints);
		setPlaceNames(names);
		setPlaceTypeAssignments(placeTypeAssignments);
		setProperties(properties);
		setLeftPlaceRelationships(leftRelationships);
		setRightPlaceRelationships(rightRelationships);
	}

	private void init() {
		this.footprints = new HashSet<>();
		this.placeNames = new HashSet<>();
		this.properties = new HashSet<>();
		this.placeTypeAssignments = new HashSet<>();
		this.leftPlaceRelationships = new HashSet<>();
		this.rightPlaceRelationships = new HashSet<>();
	}

	// --- FOOTPRINTS ---

	/**
	 * Get unmodifiable view of footprints.
	 *
	 * @return all footprints of the place.
	 */
	public Set<Footprint> getFootprints() {
		return Collections.unmodifiableSet(footprints);
	}

	/**
	 * Add a footprint to the place.
	 *
	 * @param footprint the footprint to add. Must not be <code>null</code>.
	 */
	public void addFootprint(final Footprint footprint) {
		Objects.requireNonNull(footprint);
		if (!this.footprints.contains(footprint)) {
			this.footprints.add(footprint);
			footprint.setPlace(this);
		}
	}

	/**
	 * Remove the given footprint from the place.
	 * <p>
	 * <b>Note:</b> the footprint's attribute linking to this place is not removed due to JPA provided orphanRemoval!
	 *
	 * @param footprint the footprint to remove. Must not be <code>null</code>.
	 */
	public void removeFootprint(final Footprint footprint) {
		Objects.requireNonNull(footprint);
		this.footprints.remove(footprint);
	}

	/**
	 * Add a set of footprints to the place.
	 *
	 * @param footprints the footprints to add. Must not be <code>null</code>.
	 */
	public void addFootprints(final Set<Footprint> footprints) {
		Objects.requireNonNull(footprints);
		for (final Footprint footprint : footprints) {
			addFootprint(footprint);
		}
	}

	/**
	 * Set the footprints of the place.
	 *
	 * @param footprints the new footprints. If <code>null</code>, handled like an empty set.
	 */
	public void setFootprints(final Set<Footprint> footprints) {
		if (!this.footprints.equals(footprints)) { // not comparing with != since we never expose the original set!
			this.footprints.clear();
			if (footprints != null) {
				addFootprints(footprints);
			}
		}
	}

	/**
	 * Get the footprints of a specific geometry type.
	 *
	 * @param geomType the geometry type.
	 * @return the place's footprints of given geometry type.
	 */
	public Set<Footprint> getFootprintsByGeomType(final String geomType) {
		return footprints.stream().filter(f -> f.getGeometry().getGeometryType().equals(geomType))
				.collect(Collectors.toSet());
	}

	// --- PLACE NAMES ---

	/**
	 * Get unmodifiable view of all place names.
	 *
	 * @return all place names of the place.
	 */
	public Set<PlaceName> getPlaceNames() {
		return placeNames;
	}

	/**
	 * Add a place name to the place.
	 *
	 * @param placeName the place name to add. Must not be <code>null</code>.
	 */
	public void addPlaceName(final PlaceName placeName) {
		Objects.requireNonNull(placeName);
		if (!this.placeNames.contains(placeName)) {
			this.placeNames.add(placeName);
			placeName.setPlace(this);
		}
	}

	/**
	 * Remove the given place name from the place.
	 * <p>
	 * <b>Note:</b> the place names' attribute linking to this place is not removed due to JPA provided orphanRemoval!
	 *
	 * @param placeName the place name to remove. Must not be <code>null</code>.
	 */
	public void removePlaceName(final PlaceName placeName) {
		Objects.requireNonNull(placeName);
		this.placeNames.remove(placeName);
	}

	/**
	 * Add a set of place names to the place.
	 *
	 * @param placeNames the place names to add. Must not be <code>null</code>.
	 */
	public void addPlaceNames(final Set<PlaceName> placeNames) {
		Objects.requireNonNull(placeNames);
		for (final PlaceName placeName : placeNames) {
			addPlaceName(placeName);
		}
	}

	/**
	 * Set the place names of the place.
	 *
	 * @param placeNames the new place names. If <code>null</code>, handled like an empty set.
	 */
	public void setPlaceNames(final Set<PlaceName> placeNames) {
		if (!this.placeNames.equals(placeNames)) { // not comparing with != since we never expose the original set!
			this.placeNames.clear();
			if (placeNames != null) {
				addPlaceNames(placeNames);
			}
		}
	}

	/**
	 * Get place names with a {@link NameFlag#IS_PREFERRED} flag.
	 *
	 * @return the place's preferred names.
	 */
	public Set<PlaceName> getPreferredPlaceNames() {
		return placeNames.stream().filter(n -> n.getNameFlags().contains(NameFlag.IS_PREFERRED))
				.collect(Collectors.toSet());
	}

	/**
	 * Get all place names for the given language.
	 *
	 * @param language the language.
	 * @return the place's names for the given language.
	 */
	public Set<PlaceName> getPlaceNamesForLanguage(final String language) {
		return placeNames.stream().filter(n -> Objects.equals(n.getLanguage(), language)).collect(Collectors.toSet());
	}

	// --- PROPERTIES ---

	/**
	 * Get unmodifiable view of all place properties.
	 *
	 * @return all place properties of the place.
	 */
	public Set<PlaceProperty> getProperties() {
		return Collections.unmodifiableSet(properties);
	}

	/**
	 * Add a place property to the place.
	 *
	 * @param property the place property to add. Must not be <code>null</code>.
	 */
	public void addProperty(final PlaceProperty property) {
		Objects.requireNonNull(property);
		if (!this.properties.contains(property)) {
			this.properties.add(property);
			property.setPlace(this);
		}
	}

	/**
	 * Remove the given place property from the place.
	 * <p>
	 * <b>Note:</b> the place property's attribute linking to this place is not removed due to JPA provided
	 * orphanRemoval!
	 *
	 * @param property the place property to remove. Must not be <code>null</code>.
	 */
	public void removePlaceProperty(final PlaceProperty property) {
		Objects.requireNonNull(property);
		this.properties.remove(property);
	}

	/**
	 * Add a set of place properties to the place.
	 *
	 * @param properties the place properties to add. Must not be <code>null</code>.
	 */
	public void addProperties(final Set<PlaceProperty> properties) {
		Objects.requireNonNull(properties);
		for (final PlaceProperty property : properties) {
			addProperty(property);
		}
	}

	/**
	 * Set the place properties of the place.
	 *
	 * @param properties the new place properties. If <code>null</code>, handled like an empty set.
	 */
	public void setProperties(final Set<PlaceProperty> properties) {
		if (!this.properties.equals(properties)) { // not comparing with != since we never expose the original set!
			this.properties.clear();
			if (properties != null) {
				addProperties(properties);
			}
		}
	}

	/**
	 * Get the properties of a specific type.
	 *
	 * @param type the property type to search for.
	 * @return the place's properties of given type.
	 */
	public Set<PlaceProperty> getPropertiesByType(final PlacePropertyType type) {
		return properties.stream().filter(p -> Objects.equals(p.getType(), type)).collect(Collectors.toSet());
	}

	/**
	 * Get the properties of a specific type.
	 *
	 * @param placePropertyTypeName the name of the property type to search for.
	 * @return the place's properties of given type.
	 */
	public Set<PlaceProperty> getPropertiesByType(final String placePropertyTypeName) {
		return properties.stream()
				.filter(p -> p.getType() != null && Objects.equals(p.getType().getName(), placePropertyTypeName))
				.collect(Collectors.toSet());
	}

	/**
	 * Get the properties with a specific value.
	 *
	 * @param value the value to search for.
	 * @return the place's properties with given value.
	 */
	public Set<PlaceProperty> getPropertiesByValue(final String value) {
		return properties.stream().filter(p -> Objects.equals(p.getValue(), value)).collect(Collectors.toSet());
	}

	// --- PLACE TYPE ASSIGNMENTS ---

	/**
	 * Get unmodifiable view of all place type assignments.
	 *
	 * @return all place type assignments of the place.
	 */
	public Set<PlaceTypeAssignment> getPlaceTypeAssignments() {
		return Collections.unmodifiableSet(placeTypeAssignments);
	}

	/**
	 * Add a place type assignments to the place.
	 *
	 * @param placeTypeAssignment the place type assignment to add. Must not be <code>null</code>.
	 */
	public void addPlaceTypeAssignment(final PlaceTypeAssignment placeTypeAssignment) {
		Objects.requireNonNull(placeTypeAssignment);
		if (!this.placeTypeAssignments.contains(placeTypeAssignment)) {
			this.placeTypeAssignments.add(placeTypeAssignment);
			placeTypeAssignment.setPlace(this);
		}
	}

	/**
	 * Remove the given place type assignment from the place. *
	 * <p>
	 * <b>Note:</b> the place assignment's attribute linking to this place is not removed due to JPA provided
	 * orphanRemoval!
	 *
	 * @param placeTypeAssignment the place type assignment to remove. Must not be <code>null</code>.
	 */
	public void removePlaceTypeAssignment(final PlaceTypeAssignment placeTypeAssignment) {
		Objects.requireNonNull(placeTypeAssignment);
		this.placeTypeAssignments.remove(placeTypeAssignment);
	}

	/**
	 * Add a set of place type assignments to the place.
	 *
	 * @param placeTypeAssignments the place type assignments to add. Must not be <code>null</code>.
	 */
	public void addPlaceTypeAssignments(final Set<PlaceTypeAssignment> placeTypeAssignments) {
		Objects.requireNonNull(placeTypeAssignments);
		for (final PlaceTypeAssignment placeTypeAssignment : placeTypeAssignments) {
			addPlaceTypeAssignment(placeTypeAssignment);
		}
	}

	/**
	 * Set the place type assignments of the place.
	 *
	 * @param placeTypeAssignments the new place type assignments. If <code>null</code>, handled like an empty set.
	 */
	public void setPlaceTypeAssignments(final Set<PlaceTypeAssignment> placeTypeAssignments) {
		if (!this.placeTypeAssignments.equals(placeTypeAssignments)) { // not comparing with != since we never expose
																		// the original set!
			this.placeTypeAssignments.clear();
			if (placeTypeAssignments != null) {
				addPlaceTypeAssignments(placeTypeAssignments);
			}
		}
	}

	/**
	 * Get the place type assignments of a specific type.
	 *
	 * @param type the place type to search for.
	 * @return the place's type assignments of given type.
	 */
	public Set<PlaceTypeAssignment> getPlaceTypeAssignmentsByType(final PlaceType type) {
		return placeTypeAssignments.stream().filter(p -> Objects.equals(p.getType(), type)).collect(Collectors.toSet());
	}

	/**
	 * Get the place type assignments of a specific type.
	 *
	 * @param placeTypeName the name of the place type to search for.
	 * @return the place's type assignments of given type.
	 */
	public Set<PlaceTypeAssignment> getPlaceTypeAssignmentsByType(final String placeTypeName) {
		return placeTypeAssignments.stream()
				.filter(p -> p.getType() != null && Objects.equals(p.getType().getName(), placeTypeName))
				.collect(Collectors.toSet());
	}

	// --- PLACE RELATIONSHIPS ---

	/**
	 * Get unmodifiable view of all left place relationships.
	 *
	 * @return all left place relationships of the place.
	 */
	public Set<PlaceRelationship> getLeftPlaceRelationships() {
		return Collections.unmodifiableSet(leftPlaceRelationships);
	}

	/**
	 * Add a left place relationship to the place.
	 *
	 * @param leftPlaceRelationship the left place relationship to add. Must not be <code>null</code>.
	 */
	public void addLeftPlaceRelationship(final PlaceRelationship leftPlaceRelationship) {
		Objects.requireNonNull(leftPlaceRelationship);
		if (!this.leftPlaceRelationships.contains(leftPlaceRelationship)) {
			this.leftPlaceRelationships.add(leftPlaceRelationship);
			leftPlaceRelationship.setLeftPlace(this);
		}
	}

	/**
	 * Remove the given left place relationship from the place. *
	 * <p>
	 * <b>Note:</b> the place relationship's attribute linking to this place is not removed due to JPA provided
	 * orphanRemoval!
	 *
	 * @param leftPlaceRelationship the left place relationship to remove. Must not be <code>null</code>.
	 */
	public void removeLeftPlaceRelationship(final PlaceRelationship leftPlaceRelationship) {
		Objects.requireNonNull(leftPlaceRelationship);
		this.leftPlaceRelationships.remove(leftPlaceRelationship);
	}

	/**
	 * Add a set of left place relationships to the place.
	 *
	 * @param leftPlaceRelationships the left place relationships to add. Must not be <code>null</code>.
	 */
	public void addLeftPlaceRelationships(final Set<PlaceRelationship> leftPlaceRelationships) {
		Objects.requireNonNull(leftPlaceRelationships);
		for (final PlaceRelationship leftPlaceRelationship : leftPlaceRelationships) {
			addLeftPlaceRelationship(leftPlaceRelationship);
		}
	}

	/**
	 * Set the left place relationships of the place.
	 *
	 * @param leftPlaceRelationships the new left place relationships. If <code>null</code>, handled like an empty set.
	 */
	public void setLeftPlaceRelationships(final Set<PlaceRelationship> leftPlaceRelationships) {
		if (!this.leftPlaceRelationships.equals(leftPlaceRelationships)) { // not comparing with != since we never
																			// expose the original set!
			this.leftPlaceRelationships.clear();
			if (leftPlaceRelationships != null) {
				addLeftPlaceRelationships(leftPlaceRelationships);
			}
		}
	}

	/**
	 * Get unmodifiable view of all right place relationships.
	 *
	 * @return all right place relationships of the place.
	 */
	public Set<PlaceRelationship> getRightPlaceRelationships() {
		return Collections.unmodifiableSet(rightPlaceRelationships);
	}

	/**
	 * Add a right place relationship to the place.
	 *
	 * @param rightPlaceRelationship the right place relationship to add. Must not be <code>null</code>.
	 */
	public void addRightPlaceRelationship(final PlaceRelationship rightPlaceRelationship) {
		Objects.requireNonNull(rightPlaceRelationship);
		if (!this.rightPlaceRelationships.contains(rightPlaceRelationship)) {
			this.rightPlaceRelationships.add(rightPlaceRelationship);
			rightPlaceRelationship.setRightPlace(this);
		}
	}

	/**
	 * Remove the given right place relationship from the place. *
	 * <p>
	 * <b>Note:</b> the place relationship's attribute linking to this place is not removed due to JPA provided
	 * orphanRemoval!
	 *
	 * @param rightPlaceRelationship the right place relationship to remove. Must not be <code>null</code>.
	 */
	public void removeRightPlaceRelationship(final PlaceRelationship rightPlaceRelationship) {
		Objects.requireNonNull(rightPlaceRelationship);
		this.rightPlaceRelationships.remove(rightPlaceRelationship);
	}

	/**
	 * Add a set of right place relationships to the place.
	 *
	 * @param rightPlaceRelationships the right place relationships to add. Must not be <code>null</code>.
	 */
	public void addRightPlaceRelationships(final Set<PlaceRelationship> rightPlaceRelationships) {
		Objects.requireNonNull(rightPlaceRelationships);
		for (final PlaceRelationship rightPlaceRelationship : rightPlaceRelationships) {
			addRightPlaceRelationship(rightPlaceRelationship);
		}
	}

	/**
	 * Set the right place relationships of the place.
	 *
	 * @param rightPlaceRelationships the new right place relationships. If <code>null</code>, handled like an empty
	 *            set.
	 */
	public void setRightPlaceRelationships(final Set<PlaceRelationship> rightPlaceRelationships) {
		if (!this.rightPlaceRelationships.equals(rightPlaceRelationships)) { // not comparing with != since we never
																				// expose the original set!
			this.rightPlaceRelationships.clear();
			if (rightPlaceRelationships != null) {
				addRightPlaceRelationships(rightPlaceRelationships);
			}
		}
	}

	@Override
	public String toString() {
		return "Place [footprints=" + footprints + ", placeNames=" + placeNames + ", properties=" + properties
				+ ", placeTypeAssignments=" + placeTypeAssignments + ", leftPlaceRelationships="
				+ leftPlaceRelationships + ", rightPlaceRelationships=" + rightPlaceRelationships + ", "
				+ super.toString() + "]";
	}

}