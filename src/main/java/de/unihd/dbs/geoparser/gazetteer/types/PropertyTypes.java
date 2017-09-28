package de.unihd.dbs.geoparser.gazetteer.types;

import de.unihd.dbs.geoparser.gazetteer.models.PlaceProperty;

/**
 * Type definitions for {@link PlaceProperty} entities.
 *
 * @author lrichter
 *
 */
public enum PropertyTypes {
	// @formatter:off
	ELEVATION("elevation", "number of meters above sea level a place is located at"),
	GEONAMES_ID("geonamesId", "GeoNames identifier of a place"),
	POPULATION("population", "number of inhabitants for a place"),
	// TIMEZONE("timezone", "timezone in which a place is located"),
	WIKIPEDIA_LINK("wikipediaLink", "link to a wikipedia article describing the place"),
	GEONAMES_SUPPLDATA("geonamesSupplData", "Superclass for supplementary data comming from GeoNames that may be deleted after data import/merge"),
	GEONAMES_FCLASS("geonamesFClass", "GeoNames feature class of a place; see http://www.geonames.org/export/codes.html", GEONAMES_SUPPLDATA),
	GEONAMES_FCODE("geonamesFCode", "GeoNames feature code of a place; see http://www.geonames.org/export/codes.html", GEONAMES_SUPPLDATA),
	GEONAMES_ADMIN_CODE("geonamesAdminCode", "Administrative level code extracted from GeoNames; compiled of country " +
	"and ADM1-4 columns. Allows to draw conclusions on administrative relationships.", GEONAMES_SUPPLDATA),
	GEONAMES_ADMIN_PARENT("geonamesAdminParent", "GeoNames identifier of a place that is the administrative parent o this place "
			+ "(wrt. the hierchary table by GeoNames or the admin1-4 fields in geoname table)", GEONAMES_SUPPLDATA),
	GEONAMES_GOV_SEAT_OF("geonamesGovSeatOf", "GeoNames identifier of the place, of which this place is a governmental seat of", GEONAMES_SUPPLDATA),
	GEONAMES_CAPITAL_OF("geonamesCapitalOf", "GeoNames identifier of the place, of which this place is the capital", GEONAMES_SUPPLDATA);
	// @formatter:on

	public final String typeName;
	public final String description;
	public final PropertyTypes parent;

	private PropertyTypes(final String typeName, final String typeDescription) {
		this(typeName, typeDescription, null);
	}

	private PropertyTypes(final String typeName, final String typeDescription, final PropertyTypes parent) {
		this.parent = parent;
		this.typeName = typeName;
		this.description = typeDescription;
	}

	public static PropertyTypes getByName(final String typeName) {
		for (final PropertyTypes type : PropertyTypes.values()) {
			if (type.typeName.equals(typeName)) {
				return type;
			}
		}
		throw new IllegalArgumentException("No enum constant PropertyTypes with typeName " + typeName + "!");
	}
}
