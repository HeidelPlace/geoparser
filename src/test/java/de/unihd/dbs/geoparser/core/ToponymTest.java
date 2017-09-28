package de.unihd.dbs.geoparser.core;

import static org.junit.Assert.*;

import static org.hamcrest.CoreMatchers.*;
import org.junit.Test;
import de.unihd.dbs.geoparser.core.NamedEntityType;
import de.unihd.dbs.geoparser.core.Toponym;

import edu.stanford.nlp.ling.CoreAnnotations.CharacterOffsetBeginAnnotation;
import edu.stanford.nlp.ling.CoreAnnotations.CharacterOffsetEndAnnotation;
import edu.stanford.nlp.ling.CoreAnnotations.NamedEntityTagAnnotation;
import edu.stanford.nlp.ling.CoreAnnotations.TextAnnotation;
import edu.stanford.nlp.util.ArrayCoreMap;
import edu.stanford.nlp.util.CoreMap;

public class ToponymTest {

	private static final int beginPosition = 0;
	private static final int endPosition = 17;
	private static final String text = "some named entity";
	private static final NamedEntityType type = NamedEntityType.LOCATION;

	private static CoreMap createSourceAnnotation(final String text, final Integer beginPosition,
			final Integer endPosition, final NamedEntityType type) {
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

		return sourceAnnotation;
	}

	@Test
	public void testToponymConstructor() {
		final CoreMap sourceAnnotation = createSourceAnnotation(text, beginPosition, endPosition, type);
		final Toponym toponym = new Toponym(sourceAnnotation);
		assertThat(toponym.beginPosition, equalTo(beginPosition));
		assertThat(toponym.endPosition, equalTo(endPosition));
		assertThat(toponym.sourceAnnotation, equalTo(sourceAnnotation));
		assertThat(toponym.text, equalTo(text));
		assertThat(toponym.type, equalTo(type));
	}

	@Test(expected = IllegalArgumentException.class)
	public void testToponymConstructorNoLocationNE() {
		new Toponym(createSourceAnnotation(text, beginPosition, endPosition, NamedEntityType.MISC));
	}

	@Test
	public void testToString() {
		// perform simple smoke test
		new Toponym(createSourceAnnotation(text, beginPosition, endPosition, type)).toString();
	}

}
