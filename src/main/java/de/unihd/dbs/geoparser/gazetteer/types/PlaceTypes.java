package de.unihd.dbs.geoparser.gazetteer.types;

import de.unihd.dbs.geoparser.gazetteer.models.PlaceTypeAssignment;

/**
 * Type definitions for {@link PlaceTypeAssignment} entities.
 *
 * @author lrichter
 *
 */
public enum PlaceTypes {
	//@formatter:off
	GEONAMES_ADMIN_TYPE("GeoNames Administrative Level Code", "Superclass for Geonames Administrative Level Codes"),
	ADMINISTRATIVE_DIVISION("admin_division", "An administrative division"),
	ADMINISTRATIVE_DIVISION_SEAT("admin_division_seat", "Seat of an administrative division"),
	CAPITAL("capital", "Capital of a political entity"),
	CONTINENT("continent", "A continent", ADMINISTRATIVE_DIVISION),
	POLITICAL_ENTITY("political", "A political entity as defined by GeoNames", CONTINENT),
	ADMIN1("geonames_adm1", "ADM1 as used by GeoNames", POLITICAL_ENTITY),
	ADMIN2("geonames_adm2", "ADM2 as used by GeoNames", ADMIN1),
	ADMIN3("geonames_adm3", "ADM3 as used by GeoNames", ADMIN2),
	ADMIN4("geonames_adm4", "ADM4 as used by GeoNames", ADMIN3),
	ADMIN5("geonames_adm5", "ADM5 as used by GeoNames", ADMIN4),
	HISTORICAL("historical", "A historical place"),
	POPULATED_PLACE("populated", "A populated place"),
	POI("poi", "Point of interest"),
	AIRPORT("airport", "an airport", POI),
	CASTLE("castle", "a castle", POI),
	HOTEL("hotel", "a hotel", POI);
	//@formatter:on

	public final String typeName;
	public final String description;
	public final PlaceTypes parent;

	private PlaceTypes(final String typeName, final String typeDescription) {
		this(typeName, typeDescription, null);
	}

	private PlaceTypes(final String typeName, final String typeDescription, final PlaceTypes parent) {
		this.parent = parent;
		this.typeName = typeName;
		this.description = typeDescription;
	}

	public static PlaceTypes getByName(final String typeName) {
		for (final PlaceTypes type : PlaceTypes.values()) {
			if (type.typeName.equals(typeName)) {
				return type;
			}
		}
		throw new IllegalArgumentException("No enum constant PlaceTypes with typeName " + typeName + "!");
	}

}
