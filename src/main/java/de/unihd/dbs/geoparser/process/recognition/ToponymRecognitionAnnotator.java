package de.unihd.dbs.geoparser.process.recognition;

import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Properties;
import java.util.Set;

import edu.stanford.nlp.ie.regexp.NumberSequenceClassifier;
import edu.stanford.nlp.ling.CoreAnnotations;
import edu.stanford.nlp.ling.CoreAnnotations.MentionsAnnotation;
import edu.stanford.nlp.ling.CoreAnnotations.NamedEntityTagAnnotation;
import edu.stanford.nlp.ling.CoreAnnotations.NormalizedNamedEntityTagAnnotation;
import edu.stanford.nlp.ling.CoreAnnotations.NumericCompositeTypeAnnotation;
import edu.stanford.nlp.ling.CoreAnnotations.NumericCompositeValueAnnotation;
import edu.stanford.nlp.ling.CoreAnnotations.SentencesAnnotation;
import edu.stanford.nlp.ling.CoreAnnotations.TokensAnnotation;
import edu.stanford.nlp.ling.CoreLabel;
import edu.stanford.nlp.pipeline.Annotation;
import edu.stanford.nlp.pipeline.Annotator;
import edu.stanford.nlp.time.TimeAnnotations.TimexAnnotation;
import edu.stanford.nlp.util.ArraySet;
import edu.stanford.nlp.util.CoreMap;
import edu.stanford.nlp.util.concurrent.MulticoreWrapper;
import edu.stanford.nlp.util.concurrent.ThreadsafeProcessor;

/**
 * This {@link Annotator} implementation adds recognized toponyms to an {@link Annotation} instance using a
 * {@link ToponymRecognizer} implementation.
 * <p>
 * The assumptions made regarding available annotations is specified by the {@link ToponymRecognizer} implementation via
 * {@link ToponymRecognizer#requires}. E.g., it might be assumed that the annotation already contains the tokenized
 * words (in form of {@link CoreLabel}s) under the key with class {@link TokensAnnotation}, which are split into
 * sentences that are stored under the key with class {@link SentencesAnnotation}.
 * <p>
 * For each token, the NER class is stored via the key {@link NamedEntityTagAnnotation}. Depending on the used
 * classifier, additional NE information per token may be stored under the keys
 * {@link NormalizedNamedEntityTagAnnotation}, {@link NumericCompositeValueAnnotation},
 * {@link NumericCompositeTypeAnnotation}, and {@link TimexAnnotation}.
 * <p>
 * Furthermore, for each named entity (possibly spanning multiple tokens) a {@link MentionsAnnotation} is added on
 * sentence level.
 * 
 * @author lrichter
 * 
 */
public class ToponymRecognitionAnnotator implements Annotator {

	private final ToponymRecognizer recognizer;
	private final int nThreads;

	public static final String TOPONYM_RECOGNITION = "toponym_recognition";
	public static final Requirement TOPONYM_RECOGNITION_REQUIREMENT = new Requirement(TOPONYM_RECOGNITION);

	private Annotation annotation; // required for multi-threaded processing

	public ToponymRecognitionAnnotator(final ToponymRecognizer recognizer) {
		this(recognizer, 1);
	}

	/**
	 * Constructor to support Annotation loading via reflection.
	 * 
	 * @param name ?
	 * @param props ?
	 */
	public ToponymRecognitionAnnotator(final String name, final Properties props) {
		throw new UnsupportedOperationException();
	}

	public ToponymRecognitionAnnotator(final ToponymRecognizer recognizer, final int nThreads) {
		Objects.requireNonNull(recognizer);
		this.recognizer = recognizer;
		this.nThreads = nThreads;
	}

	public ToponymRecognizer getRecognitionModule() {
		return this.recognizer;
	}

	@Override
	public void annotate(final Annotation annotation) {
		try {
			this.annotation = annotation;

			if (!annotation.has(CoreAnnotations.SentencesAnnotation.class)) {
				throw new RuntimeException("No sentence found in " + annotation);
			}

			if (nThreads == 1) {
				for (final CoreMap sentence : annotation.get(CoreAnnotations.SentencesAnnotation.class)) {
					doOneSentence(annotation, sentence);
				}
			}
			else {
				final MulticoreWrapper<CoreMap, CoreMap> wrapper = new MulticoreWrapper<>(nThreads,
						new RecognitionProcessor());
				for (final CoreMap sentence : annotation.get(CoreAnnotations.SentencesAnnotation.class)) {
					wrapper.put(sentence);
					while (wrapper.peek()) {
						wrapper.poll();
					}
				}
				wrapper.join();
				while (wrapper.peek()) {
					wrapper.poll();
				}
			}
		}
		finally {
			this.annotation = null;
		}

	}

	private class RecognitionProcessor implements ThreadsafeProcessor<CoreMap, CoreMap> {
		@Override
		public CoreMap process(final CoreMap sentence) {
			return doOneSentence(annotation, sentence);
		}

		@Override
		public ThreadsafeProcessor<CoreMap, CoreMap> newInstance() {
			return this;
		}
	}

	private CoreMap doOneSentence(final Annotation annotation, final CoreMap sentence) {
		final List<CoreLabel> tokens = sentence.get(CoreAnnotations.TokensAnnotation.class);
		final List<CoreMap> output = recognizer.recognize(tokens, annotation, sentence);

		// ensure that all tokens get a label - may be necessary to be compatible with default Stanford NER behavior
		// tokens.forEach(token -> token.set(CoreAnnotations.NamedEntityTagAnnotation.class,
		// NamedEntityType.NONE.name));

		for (final CoreMap mention : output) {
			final List<CoreLabel> mentionTokens = mention.get(CoreAnnotations.TokensAnnotation.class);
			for (int i = 0; i < mentionTokens.size(); i++) {
				final CoreLabel mentionToken = mentionTokens.get(i);
				final CoreLabel sourceToken = tokens.get(mentionToken.get(CoreAnnotations.IndexAnnotation.class) - 1);
				sourceToken.set(CoreAnnotations.NamedEntityTagAnnotation.class,
						mentionToken.get(CoreAnnotations.NamedEntityTagAnnotation.class));
				if (mentionToken.has(CoreAnnotations.NormalizedNamedEntityTagAnnotation.class)) {
					sourceToken.set(CoreAnnotations.NormalizedNamedEntityTagAnnotation.class,
							mentionToken.get(CoreAnnotations.NormalizedNamedEntityTagAnnotation.class));
				}
				NumberSequenceClassifier.transferAnnotations(mentionToken, sourceToken);
				mentionTokens.set(i, sourceToken);
			}
		}
		sentence.set(CoreAnnotations.MentionsAnnotation.class, output);

		return sentence;
	}

	@Override
	public Set<Requirement> requires() {
		final Set<Requirement> requirements = new HashSet<>(Annotator.TOKENIZE_AND_SSPLIT);
		requirements.addAll(recognizer.requires());
		return Collections.unmodifiableSet(requirements);
	}

	@Override
	public Set<Requirement> requirementsSatisfied() {
		final Set<Requirement> requirements = new ArraySet<>(TOPONYM_RECOGNITION_REQUIREMENT);
		requirements.addAll(recognizer.requirementsSatisfied());
		return Collections.unmodifiableSet(requirements);
	}

}
