package de.unihd.dbs.geoparser.core;

import static org.junit.Assert.*;

import org.junit.BeforeClass;

import static org.hamcrest.CoreMatchers.*;

import org.junit.Test;
import de.unihd.dbs.geoparser.core.GeoparsingAnnotations.ResolvedLocationAnnotation;
import de.unihd.dbs.geoparser.gazetteer.models.Footprint;
import de.unihd.dbs.geoparser.gazetteer.models.Place;

import com.vividsolutions.jts.geom.Geometry;
import com.vividsolutions.jts.geom.GeometryFactory;
import com.vividsolutions.jts.geom.PrecisionModel;
import com.vividsolutions.jts.io.ParseException;
import com.vividsolutions.jts.io.WKTReader;

import edu.stanford.nlp.ling.CoreAnnotations.CharacterOffsetBeginAnnotation;
import edu.stanford.nlp.ling.CoreAnnotations.CharacterOffsetEndAnnotation;
import edu.stanford.nlp.ling.CoreAnnotations.NamedEntityTagAnnotation;
import edu.stanford.nlp.ling.CoreAnnotations.TextAnnotation;
import edu.stanford.nlp.util.ArrayCoreMap;
import edu.stanford.nlp.util.CoreMap;
import nl.jqno.equalsverifier.EqualsVerifier;

public class ResolvedToponymTest {

	private static final WKTReader fromText = new WKTReader(
			new GeometryFactory(new PrecisionModel(), Footprint.REFERENCE_SYSTEM_SRID));

	private static final int beginPosition = 0;
	private static final int endPosition = 17;
	private static final String text = "some named entity";
	private static final NamedEntityType type = NamedEntityType.LOCATION;
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

	private static CoreMap createSourceAnnotation(final String text, final Integer beginPosition,
			final Integer endPosition, final NamedEntityType type, final Place place, final Geometry location) {
		final CoreMap sourceAnnotation = new ArrayCoreMap();
		if (text != null) {
			sourceAnnotation.set(TextAnnotation.class, text);
		}
		if (beginPosition != null) {
			sourceAnnotation.set(CharacterOffsetBeginAnnotation.class, beginPosition);
		}
		if (endPosition != null) {
			sourceAnnotation.set(CharacterOffsetEndAnnotation.class, endPosition);
		}
		if (type != null) {
			sourceAnnotation.set(NamedEntityTagAnnotation.class, type.name);
		}

		if (place != null || location != null) {
			ResolvedLocation resolvedLocation;
			if (place == null) {
				resolvedLocation = new ResolvedLocation(location);
			}
			else if (location == null) {
				resolvedLocation = new ResolvedLocation(place);
			}
			else {
				resolvedLocation = new ResolvedLocation(place, location);
			}
			sourceAnnotation.set(ResolvedLocationAnnotation.class, resolvedLocation);
		}

		return sourceAnnotation;
	}

	@Test
	public void testResolvedToponymConstructor() {
		final CoreMap sourceAnnotation = createSourceAnnotation(text, beginPosition, endPosition, type, place,
				location);
		final ResolvedToponym toponym = new ResolvedToponym(sourceAnnotation);
		assertThat(toponym.beginPosition, equalTo(beginPosition));
		assertThat(toponym.endPosition, equalTo(endPosition));
		assertThat(toponym.sourceAnnotation, equalTo(sourceAnnotation));
		assertThat(toponym.text, equalTo(text));
		assertThat(toponym.type, equalTo(type));
		assertThat(toponym.resolvedLocation.gazetteerEntry, equalTo(place));
		assertThat(toponym.resolvedLocation.location, equalTo(location));
	}

	@Test(expected = IllegalArgumentException.class)
	public void testLinkedToponymConstructorNoResolvedLocationAnnotation() {
		new ResolvedToponym(
				createSourceAnnotation(text, beginPosition, endPosition, NamedEntityType.LOCATION, null, null));
	}

	private static class PlaceWithId extends Place {

		public PlaceWithId(final Long id) {
			super();
			setId(id);
		}
	}

	@Test
	public void testEqualsContract() throws ParseException {
		final PlaceWithId placeA = new PlaceWithId(1L);
		final PlaceWithId placeB = new PlaceWithId(2L);
		final Geometry locationA = fromText.read("POINT(50 50)");
		final Geometry locationB = fromText.read("POINT(100 50)");

		EqualsVerifier.forClass(ResolvedToponym.class).withRedefinedSuperclass().usingGetClass()
				.withIgnoredFields("sourceAnnotation").withPrefabValues(Place.class, placeA, placeB)
				.withPrefabValues(Geometry.class, locationA, locationB).verify();
	}

	@Test
	public void testToString() {
		// perform simple smoke test
		new ResolvedToponym(createSourceAnnotation(text, beginPosition, endPosition, type, place, location)).toString();
	}

}
