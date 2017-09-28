package de.unihd.dbs.geoparser.core;

import static org.hamcrest.Matchers.*;
import static org.junit.Assert.*;

import java.util.Arrays;

import org.junit.Test;
import de.unihd.dbs.geoparser.gazetteer.models.Place;

import edu.stanford.nlp.ling.CoreAnnotations;
import edu.stanford.nlp.ling.CoreLabel;
import edu.stanford.nlp.util.ArrayCoreMap;
import edu.stanford.nlp.util.CoreMap;

public class DocumentTest {

	@Test
	public void testDocument() {
		final String author = "JUnit Testing dude";
		final String documentId = "123";
		final String documentDate = "2016-09-28";
		final String originalText = "This is a test";
		final String title = "some title";

		final Document document = new Document(originalText, title, author, documentId, documentDate);

		assertThat(document.getAuthor(), equalTo(author));
		assertThat(document.getDocumentId(), equalTo(documentId));
		assertThat(document.getDocumentDate(), equalTo(documentDate));
		assertThat(document.getOriginalText(), equalTo(originalText));
		assertThat(document.getTitle(), equalTo(title));
	}

	@Test
	public void testDocumentOnlyOrginalText() {
		final String originalText = "This is a test";

		final Document document = new Document(originalText);

		assertThat(document.getAuthor(), nullValue());
		assertThat(document.getDocumentId(), nullValue());
		assertThat(document.getDocumentDate(), nullValue());
		assertThat(document.getOriginalText(), equalTo(originalText));
		assertThat(document.getTitle(), nullValue());
	}

	@Test
	public void testSetOrRemoveIfNull() {
		final Document document = new Document("");

		document.setOrRemoveIfNull(CoreAnnotations.TextAnnotation.class, "test");
		assertThat(document.get(CoreAnnotations.TextAnnotation.class), equalTo("test"));

		document.setOrRemoveIfNull(CoreAnnotations.TextAnnotation.class, null);
		assertFalse(document.containsKey(CoreAnnotations.TextAnnotation.class));
	}

	@Test
	public void testPrettyPrintAnnotationForEmptyDocumentSmokeTest() {
		final Document document = new Document(null);
		Document.prettyPrintAnnotation(document);
	}

	@Test
	public void testPrettyPrintAnnotationWithDocumentMetaDataSmokeTest() {
		final Document document = new Document("test document text", "some title", "tester", "123", "2016-09-28");
		Document.prettyPrintAnnotation(document);
	}

	@Test
	public void testPrettyPrintAnnotationSmokeTest() {
		final String tokenText = "Heidelberg";
		final String sentenceText = tokenText + " is testing...";
		final String documentText = sentenceText;
		final Document document = new Document(documentText);

		final CoreLabel word = new CoreLabel();
		word.set(CoreAnnotations.TextAnnotation.class, tokenText);
		word.set(CoreAnnotations.CharacterOffsetBeginAnnotation.class, 0);
		word.set(CoreAnnotations.CharacterOffsetEndAnnotation.class, tokenText.length());
		word.set(CoreAnnotations.NamedEntityTagAnnotation.class, NamedEntityType.LOCATION.name);
		word.set(CoreAnnotations.PartOfSpeechAnnotation.class, PartOfSpeechPTBType.PROPER_NOUN_SINGULAR.name);
		word.set(GeoparsingAnnotations.GazetteerEntriesAnnotation.class, Arrays.asList(new Place()));

		final CoreMap sentence = new ArrayCoreMap();
		sentence.set(CoreAnnotations.TextAnnotation.class, sentenceText);
		sentence.set(CoreAnnotations.CharacterOffsetBeginAnnotation.class, 0);
		sentence.set(CoreAnnotations.CharacterOffsetEndAnnotation.class, sentenceText.length());
		sentence.set(CoreAnnotations.TokensAnnotation.class, Arrays.asList(word, word));
		sentence.set(CoreAnnotations.MentionsAnnotation.class, Arrays.asList(word));

		document.set(CoreAnnotations.SentencesAnnotation.class, Arrays.asList(sentence));

		Document.prettyPrintAnnotation(document);

	}

}
