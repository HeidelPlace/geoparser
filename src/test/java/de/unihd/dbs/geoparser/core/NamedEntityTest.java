package de.unihd.dbs.geoparser.core;

import static org.hamcrest.CoreMatchers.equalTo;
import static org.junit.Assert.assertThat;

import org.junit.Test;
import de.unihd.dbs.geoparser.core.NamedEntity;
import de.unihd.dbs.geoparser.core.NamedEntityType;

import edu.stanford.nlp.ling.CoreAnnotations.CharacterOffsetBeginAnnotation;
import edu.stanford.nlp.ling.CoreAnnotations.CharacterOffsetEndAnnotation;
import edu.stanford.nlp.ling.CoreAnnotations.NamedEntityTagAnnotation;
import edu.stanford.nlp.ling.CoreAnnotations.TextAnnotation;
import edu.stanford.nlp.util.ArrayCoreMap;
import edu.stanford.nlp.util.CoreMap;
import nl.jqno.equalsverifier.EqualsVerifier;

public class NamedEntityTest {

	private static final int beginPosition = 0;
	private static final int endPosition = 17;
	private static final String text = "some named entity";
	private static final NamedEntityType type = NamedEntityType.MISC;

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
	public void testNamedEntityConstructor1() {
		final CoreMap sourceAnnotation = createSourceAnnotation(text, beginPosition, endPosition, type);
		final NamedEntity namedEntity = new NamedEntity(sourceAnnotation);
		assertThat(namedEntity.beginPosition, equalTo(beginPosition));
		assertThat(namedEntity.endPosition, equalTo(endPosition));
		assertThat(namedEntity.sourceAnnotation, equalTo(sourceAnnotation));
		assertThat(namedEntity.text, equalTo(text));
		assertThat(namedEntity.type, equalTo(type));
	}

	@Test
	public void testNamedEntityConstructor2() {
		final CoreMap sourceAnnotation = createSourceAnnotation(text, beginPosition, endPosition, type);
		final NamedEntity namedEntity = new NamedEntity(text, beginPosition, endPosition, type, sourceAnnotation);
		assertThat(namedEntity.beginPosition, equalTo(beginPosition));
		assertThat(namedEntity.endPosition, equalTo(endPosition));
		assertThat(namedEntity.sourceAnnotation, equalTo(sourceAnnotation));
		assertThat(namedEntity.text, equalTo(text));
		assertThat(namedEntity.type, equalTo(type));
	}

	@Test(expected = IllegalArgumentException.class)
	public void testNamedEntityConstructorNoText() {
		new NamedEntity(createSourceAnnotation(null, beginPosition, endPosition, type));
	}

	@Test(expected = IllegalArgumentException.class)
	public void testNamedEntityConstructorNoBeginPosition() {
		new NamedEntity(createSourceAnnotation(text, null, endPosition, type));
	}

	@Test(expected = IllegalArgumentException.class)
	public void testNamedEntityConstructorNoEndPosition() {
		new NamedEntity(createSourceAnnotation(text, beginPosition, null, type));
	}

	@Test(expected = IllegalArgumentException.class)
	public void testNamedEntityConstructorNoType() {
		new NamedEntity(createSourceAnnotation(text, beginPosition, endPosition, null));
	}

	@Test
	public void testEqualsContract() {
		EqualsVerifier.forClass(NamedEntity.class).usingGetClass().withIgnoredFields("sourceAnnotation").verify();
	}

	@Test
	public void testToString() {
		// perform simple smoke test
		new NamedEntity(createSourceAnnotation(text, beginPosition, endPosition, type)).toString();
	}
}
