package de.unihd.dbs.geoparser.core;

import java.util.Objects;

import de.unihd.dbs.geoparser.gazetteer.models.Place;

import com.vividsolutions.jts.geom.Geometry;

/**
 * Representation for a resolved location.
 * <p>
 * It must have an assigned location and may have an optional gazetteer entry that is associated with the location.
 * 
 * @author lrichter
 *
 */
public class ResolvedLocation {

	public final Place gazetteerEntry;
	public final Geometry location;

	/**
	 * Create a {@link ResolvedLocation} with the given parameters.
	 * 
	 * @param gazetteerEntry the gazetteer entry for the location. Must not be <code>null</code>.
	 * @param location the location. Must not be <code>null</code>.
	 */
	public ResolvedLocation(final Place gazetteerEntry, final Geometry location) {
		super();
		Objects.requireNonNull(gazetteerEntry);
		Objects.requireNonNull(location);
		this.gazetteerEntry = gazetteerEntry;
		this.location = location;
	}

	/**
	 * Create a {@link ResolvedLocation} with the given gazetteer entry.
	 * <p>
	 * The geometry of its first footprint entry is taken as location.
	 * 
	 * @param gazetteerEntry the gazetteer entry for the location. Must not be <code>null</code>.
	 */
	public ResolvedLocation(final Place gazetteerEntry) {
		super();
		Objects.requireNonNull(gazetteerEntry);
		if (gazetteerEntry.getFootprints().isEmpty()) {
			throw new IllegalArgumentException("The given place does not have a footprint!");
		}

		this.gazetteerEntry = gazetteerEntry;
		this.location = this.gazetteerEntry.getFootprints().iterator().next().getGeometry();
	}

	/**
	 * Create a {@link ResolvedLocation} with the given location.
	 * <p>
	 * The gazetteer entry is set to null.
	 * 
	 * @param location the location. Must not be <code>null</code>.
	 */
	public ResolvedLocation(final Geometry location) {
		super();
		Objects.requireNonNull(location);
		this.location = location;
		this.gazetteerEntry = null;
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + ((gazetteerEntry == null) ? 0 : gazetteerEntry.hashCode());
		result = prime * result + location.hashCode();
		return result;
	}

	@Override
	public boolean equals(final Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (getClass() != obj.getClass())
			return false;
		final ResolvedLocation other = (ResolvedLocation) obj;
		if (gazetteerEntry == null) {
			if (other.gazetteerEntry != null)
				return false;
		}
		else if (!gazetteerEntry.equals(other.gazetteerEntry))
			return false;
		if (!location.equals(other.location))
			return false;
		return true;
	}

	@Override
	public String toString() {
		final String gazetteerEntryStr;
		if (gazetteerEntry != null) {
			gazetteerEntryStr = "[Place id=" + gazetteerEntry.getId() + ", firstPreferredName="
					+ gazetteerEntry.getPreferredPlaceNames().iterator().next().getName();
		}
		else {
			gazetteerEntryStr = "null";
		}
		return "ResolvedLocation [gazetteerEntry=" + gazetteerEntryStr + ", location=" + location + "]";
	}

}
