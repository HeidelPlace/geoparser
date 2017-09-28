package de.unihd.dbs.geoparser.core;

import static org.hamcrest.CoreMatchers.*;
import static org.junit.Assert.*;

import java.util.Arrays;
import java.util.List;

import org.junit.Test;
import de.unihd.dbs.geoparser.core.GeoparsingAnnotations.GazetteerEntriesAnnotation;
import de.unihd.dbs.geoparser.gazetteer.models.Place;

import edu.stanford.nlp.ling.CoreAnnotations.CharacterOffsetBeginAnnotation;
import edu.stanford.nlp.ling.CoreAnnotations.CharacterOffsetEndAnnotation;
import edu.stanford.nlp.ling.CoreAnnotations.NamedEntityTagAnnotation;
import edu.stanford.nlp.ling.CoreAnnotations.TextAnnotation;
import edu.stanford.nlp.util.ArrayCoreMap;
import edu.stanford.nlp.util.CoreMap;
import nl.jqno.equalsverifier.EqualsVerifier;

public class LinkedToponymTest {

	private static final int beginPosition = 0;
	private static final int endPosition = 17;
	private static final String text = "some named entity";
	private static final NamedEntityType type = NamedEntityType.LOCATION;

	private static final Place place = new Place(null, null, null, null, null, null, null, null);
	private static final List<Place> places = Arrays.asList(place);

	private static CoreMap createSourceAnnotation(final String text, final Integer beginPosition,
			final Integer endPosition, final NamedEntityType type, final List<Place> places) {
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

		if (places != null) {
			sourceAnnotation.set(GazetteerEntriesAnnotation.class, places);
		}

		return sourceAnnotation;
	}

	@Test
	public void testLinkedToponymConstructor() {
		final CoreMap sourceAnnotation = createSourceAnnotation(text, beginPosition, endPosition, type, places);
		final LinkedToponym toponym = new LinkedToponym(sourceAnnotation);
		assertThat(toponym.beginPosition, equalTo(beginPosition));
		assertThat(toponym.endPosition, equalTo(endPosition));
		assertThat(toponym.sourceAnnotation, equalTo(sourceAnnotation));
		assertThat(toponym.text, equalTo(text));
		assertThat(toponym.type, equalTo(type));
		assertThat(toponym.gazetteerEntries, equalTo(places));
	}

	@Test(expected = IllegalArgumentException.class)
	public void testLinkedToponymConstructorNoGazetteerEntriesAnnotation() {
		new LinkedToponym(createSourceAnnotation(text, beginPosition, endPosition, NamedEntityType.LOCATION, null));
	}

	private static class PlaceWithId extends Place {

		public PlaceWithId(final Long id) {
			super();
			setId(id);
		}
	}

	@Test
	public void testEqualsContract() {
		final PlaceWithId placeA = new PlaceWithId(1L);
		final PlaceWithId placeB = new PlaceWithId(2L);

		EqualsVerifier.forClass(LinkedToponym.class).withRedefinedSuperclass().usingGetClass()
				.withIgnoredFields("sourceAnnotation").withPrefabValues(Place.class, placeA, placeB).verify();
	}

	@Test
	public void testToString() {
		// perform simple smoke test
		new LinkedToponym(createSourceAnnotation(text, beginPosition, endPosition, type, places)).toString();
	}
}
