package de.unihd.dbs.geoparser.core;

import static org.hamcrest.CoreMatchers.*;
import static org.junit.Assert.*;

import org.junit.BeforeClass;
import org.junit.Test;
import de.unihd.dbs.geoparser.gazetteer.models.Footprint;
import de.unihd.dbs.geoparser.gazetteer.models.Place;

import com.vividsolutions.jts.geom.Geometry;
import com.vividsolutions.jts.geom.GeometryFactory;
import com.vividsolutions.jts.geom.PrecisionModel;
import com.vividsolutions.jts.io.ParseException;
import com.vividsolutions.jts.io.WKTReader;

import nl.jqno.equalsverifier.EqualsVerifier;
import nl.jqno.equalsverifier.Warning;

public class ResolvedLocationTest {

	private static final WKTReader fromText = new WKTReader(
			new GeometryFactory(new PrecisionModel(), Footprint.REFERENCE_SYSTEM_SRID));

	private static Geometry location;
	private static Footprint footprint;
	private static Place place;

	@BeforeClass
	public static void initResolvedLocationInfo() throws ParseException {
		location = fromText.read("POINT(50 50)");
		footprint = new Footprint(location, 0.01d, null, null, null);
		place = new Place();
		place.addFootprint(footprint);
	}

	private static class PlaceWithId extends Place {

		public PlaceWithId(final Long id) {
			super();
			setId(id);
		}
	}

	@Test
	public void testResolvedLocationConstructor() throws ParseException {
		ResolvedLocation resolvedLocation = new ResolvedLocation(place);
		assertThat(resolvedLocation.location, equalTo(location));
		assertThat(resolvedLocation.gazetteerEntry, equalTo(place));

		resolvedLocation = new ResolvedLocation(location);
		assertThat(resolvedLocation.location, equalTo(location));
		assertThat(resolvedLocation.gazetteerEntry, nullValue());

		final Geometry otherLocation = fromText.read("POINT(50 50)");
		resolvedLocation = new ResolvedLocation(place, otherLocation);
		assertThat(resolvedLocation.location, equalTo(otherLocation));
		assertThat(resolvedLocation.gazetteerEntry, equalTo(place));
	}

	@Test(expected = IllegalArgumentException.class)
	public void testtestResolvedLocationConstructorNoFootprint() {
		new ResolvedLocation(new Place());
	}

	@Test
	public void testEqualsContract() throws ParseException {
		final PlaceWithId placeA = new PlaceWithId(1L);
		final PlaceWithId placeB = new PlaceWithId(2L);
		final Geometry locationA = fromText.read("POINT(50 50)");
		final Geometry locationB = fromText.read("POINT(100 50)");

		EqualsVerifier.forClass(ResolvedLocation.class).usingGetClass().withPrefabValues(Place.class, placeA, placeB)
				.withPrefabValues(Geometry.class, locationA, locationB).suppress(Warning.NULL_FIELDS).verify();
	}

	@Test
	public void testToString() {
		// perform simple smoke test
		new ResolvedLocation(place, location).toString();
	}

}
