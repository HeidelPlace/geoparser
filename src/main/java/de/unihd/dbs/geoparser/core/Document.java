package de.unihd.dbs.geoparser.core;

import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

import edu.stanford.nlp.ling.CoreAnnotation;
import edu.stanford.nlp.ling.CoreAnnotations;
import edu.stanford.nlp.ling.CoreLabel;
import edu.stanford.nlp.pipeline.Annotation;
import edu.stanford.nlp.pipeline.TextOutputter;
import edu.stanford.nlp.util.CoreMap;

/**
 * A document that represents a text document that should be/is/was geoparsed.
 *
 * @author lrichter
 *
 */
public class Document extends Annotation {

	private static final long serialVersionUID = -5552951943303895442L;

	/**
	 * Create a {@link Document} instance and initialize it with the given text and no metadata (title, author, id,
	 * date).
	 *
	 * @param originalText the document text
	 */
	public Document(final String originalText) {
		this(originalText, null, null, null, null);
	}

	/**
	 * Create a {@link Document} instance and initialize it with the given text and further metadata about the document.
	 *
	 * @param originalText the document text
	 * @param title the document title
	 * @param author the author of the document
	 * @param documentId an application dependent document id
	 * @param documentDate the date of document creation (is used by some processors like SUTime)
	 */
	public Document(final String originalText, final String title, final String author, final String documentId,
			final String documentDate) {
		super(originalText);
		setAuthor(author);
		setDocumentDate(documentDate);
		setDocumentId(documentId);
		setOriginalText(originalText);
		setTitle(title);
	}

	public String getAuthor() {
		return this.get(CoreAnnotations.AuthorAnnotation.class);
	}

	public void setAuthor(final String author) {
		setOrRemoveIfNull(CoreAnnotations.AuthorAnnotation.class, author);
	}

	public String getDocumentDate() {
		return this.get(CoreAnnotations.DocDateAnnotation.class);
	}

	public void setDocumentDate(final String documentDate) {
		setOrRemoveIfNull(CoreAnnotations.DocDateAnnotation.class, documentDate);
	}

	public String getDocumentId() {
		return this.get(CoreAnnotations.DocIDAnnotation.class);
	}

	public void setDocumentId(final String documentId) {
		setOrRemoveIfNull(CoreAnnotations.DocIDAnnotation.class, documentId);
	}

	public String getOriginalText() {
		return this.get(CoreAnnotations.OriginalTextAnnotation.class);
	}

	public void setOriginalText(final String originalText) {
		setOrRemoveIfNull(CoreAnnotations.OriginalTextAnnotation.class, originalText);
	}

	public String getTitle() {
		return this.get(CoreAnnotations.DocTitleAnnotation.class);
	}

	public void setTitle(final String title) {
		setOrRemoveIfNull(CoreAnnotations.DocTitleAnnotation.class, title);
	}

	/**
	 * Set the value for a given annotation class, or removes the annotation class if the value is null.
	 *
	 * @param annotationClass the annotation class
	 * @param value the new value
	 */
	public <T> void setOrRemoveIfNull(final Class<? extends CoreAnnotation<T>> annotationClass, final T value) {
		if (value == null) {
			if (this.containsKey(annotationClass)) {
				this.remove(annotationClass);
			}
		}
		else {
			this.set(annotationClass, value);
		}
	}

	/**
	 * Simplified version of the hidden and complicated to handle {@link TextOutputter#print} for pretty-printing
	 * {@link Annotation}s.
	 * <p>
	 * Returns a String instead of writing to a stream and only prints out annotations that are relevant to the
	 * geoparsing process.
	 *
	 * @param annotation the {@link Annotation} instance to pretty print
	 * @return a human-readable String representing the given {@link Annotation} instance
	 */
	public static String prettyPrintAnnotation(final Annotation annotation) {
		final StringBuilder sb = new StringBuilder();

		printDocumentMetaData(sb, annotation);
		printDocumentStatistics(sb, annotation);
		printSentences(sb, annotation);

		return sb.toString();
	}

	private static void printDocumentMetaData(final StringBuilder sb, final Annotation annotation) {
		appendAnnotationValueIfNotNull(sb, "Document ID: %s%n", annotation.get(CoreAnnotations.DocIDAnnotation.class));
		appendAnnotationValueIfNotNull(sb, "Document Title: %s%n",
				annotation.get(CoreAnnotations.DocTitleAnnotation.class));
		appendAnnotationValueIfNotNull(sb, "Document Author: %s%n",
				annotation.get(CoreAnnotations.AuthorAnnotation.class));
		appendAnnotationValueIfNotNull(sb, "Document Date: %s%n",
				annotation.get(CoreAnnotations.DocDateAnnotation.class));
	}

	private static void printDocumentStatistics(final StringBuilder sb, final Annotation annotation) {
		final List<CoreMap> sentences = ensureNonNullList(annotation.get(CoreAnnotations.SentencesAnnotation.class));
		final List<CoreLabel> allTokens = ensureNonNullList(annotation.get(CoreAnnotations.TokensAnnotation.class));

		sb.append(String.format("Document statistics: %d sentences, %d tokens%n", sentences.size(), allTokens.size()));
	}

	private static void printSentences(final StringBuilder sb, final Annotation annotation) {
		int sentenceIndex = 0;
		for (final CoreMap sentence : ensureNonNullList(annotation.get(CoreAnnotations.SentencesAnnotation.class))) {
			sentenceIndex++;
			printSentence(sb, sentence, sentenceIndex);
		}
	}

	private static void printSentence(final StringBuilder sb, final CoreMap sentence, final int sentenceIndex) {
		final List<CoreLabel> tokens = ensureNonNullList(sentence.get(CoreAnnotations.TokensAnnotation.class));
		final List<CoreMap> mentions = ensureNonNullList(sentence.get(CoreAnnotations.MentionsAnnotation.class));

		sb.append(String.format("Sentence #%d (%d tokens, %d mentions): %n", sentenceIndex, tokens.size(),
				mentions.size()));
		appendAnnotationValueIfNotNull(sb, "Text: %s%n", sentence.get(CoreAnnotations.TextAnnotation.class));
		printCoreMapList(sb, "Tokens", tokens, new String[] { "Text", "PartOfSpeech", "NamedEntityTag",
				"CharacterOffsetBegin", "CharacterOffsetEnd", "NormalizedNamedEntityTag" });
		printCoreMapList(sb, "Mentions", mentions, new String[] { "Text", "NamedEntityTag", "CharacterOffsetBegin",
				"CharacterOffsetEnd", "GazetteerEntries", "ResolvedLocation" });
	}

	private static void printCoreMapList(final StringBuilder sb, final String label,
			final List<? extends CoreMap> coreMaps, final String[] annotations) {
		sb.append(label).append(": {\n").append(coreMaps.stream()
				.map(coreMap -> "\t" + coreMap.toShorterString(annotations)).collect(Collectors.joining(",\n")))
				.append("\n}\n");
	}

	private static void appendAnnotationValueIfNotNull(final StringBuilder sb, final String formatString,
			final Object value) {
		if (value != null) {
			sb.append(String.format(formatString, value));
		}
	}

	// Refactoring lrichter 16.03.2017: this method could go to a Utility class since seems to be pratical in general
	private static <T> List<T> ensureNonNullList(final List<T> list) {
		if (list == null) {
			return Collections.emptyList();
		}
		return list;
	}

}
