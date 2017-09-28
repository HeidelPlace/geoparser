package de.unihd.dbs.geoparser.process.recognition;

import edu.stanford.nlp.ie.NERClassifierCombiner;
import edu.stanford.nlp.ie.crf.CRFClassifier;
import edu.stanford.nlp.ling.CoreAnnotations;
import edu.stanford.nlp.ling.CoreLabel;
import edu.stanford.nlp.pipeline.Annotation;
import edu.stanford.nlp.pipeline.Annotator;
import edu.stanford.nlp.pipeline.LabeledChunkIdentifier;
import edu.stanford.nlp.pipeline.Annotator.Requirement;
import edu.stanford.nlp.time.TimeAnnotations;
import edu.stanford.nlp.time.Timex;
import edu.stanford.nlp.util.ArraySet;
import edu.stanford.nlp.util.CoreMap;
import edu.stanford.nlp.util.Pair;

import java.io.IOException;
import java.io.InputStream;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.Properties;
import java.util.Set;
import java.util.function.Function;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import de.unihd.dbs.geoparser.core.GeoparserConfig;
import de.unihd.dbs.geoparser.core.GeoparserConfig.UnknownConfigLabelException;

/**
 * Implementation of {@link ToponymRecognizer} that extracts named entities from unstructured text documents using the
 * Stanford NER software: http://nlp.stanford.edu/software/CRF-NER.shtml.
 * 
 * @author lrichter
 * 
 */
// XXX: currently we use LabeledChunkIdentifier manually instead of the more feature-rich EntityMentionsAnnotator
// since it is very odd to use (mixes annotation logic with processing logic)
public class StanfordNER extends ToponymRecognizer {

	public static final String CONFIG_STANFORD_NER_PROP_PATH_LABEL = "stanford.ner.prop.path";
	public static final String CONFIG_STANFORD_NER_MODEL_PATH_LABEL = "stanford.ner.model.path";

	private static final Logger logger = LoggerFactory.getLogger(StanfordNER.class);

	private NERClassifierCombiner namedEntityRecognizer;
	private final LabeledChunkIdentifier chunkIdentifier;

	/**
	 * Create a {@link StanfordNER} by instantiating the Stanford NER with the language model and parameters specified
	 * in the given Geoparser Configuration.
	 * 
	 * @param config the Geoparser configuration.
	 * @throws IOException if reading the language model files failed.
	 * @throws UnknownConfigLabelException if the configuration string labels are invalid.
	 */
	public StanfordNER(final GeoparserConfig config) throws IOException, UnknownConfigLabelException {
		// TODO: make applyNumericalClassifiers and useSUTime configurable
		this(config.getConfigStringByLabel(CONFIG_STANFORD_NER_MODEL_PATH_LABEL),
				config.getConfigStringByLabel(CONFIG_STANFORD_NER_PROP_PATH_LABEL), false, false);
	}

	/**
	 * Create a {@link StanfordNER} by instantiating the Stanford NER with the specified language model and parameter
	 * files.
	 * <p>
	 * <b>Note</b>if the model property source is given as relative path, it needs to be located under "resources", else
	 * it should be specified as absolute path. The model path must be a relative path within the JAR file!
	 * 
	 * @param NERmodelPath path to Stanford NER language model.
	 * @param NERpropPath path to property file for the Stanford NER.
	 * @param applyNumericalClassifiers if <code>true</code>, numerical classifiers (NumberSequenceClassifier,
	 *            QuantifiableEntityNormalizer) are also applied.
	 * @param useSUTime if <code>true</code>, the date/time classifier SUTime is also applied.
	 * @throws IOException if reading the language model files failed.
	 */
	public StanfordNER(final String NERmodelPath, final String NERpropPath, final boolean applyNumericalClassifiers,
			final boolean useSUTime) throws IOException {
		chunkIdentifier = new LabeledChunkIdentifier();
		initStanfordNER(NERmodelPath, NERpropPath, applyNumericalClassifiers, useSUTime);
	}

	private void initStanfordNER(final String NERmodelPath, final String NERpropPath,
			final boolean applyNumericalClassifiers, final boolean useSUTime) throws IOException {
		logger.debug("Initializing StanfordNER with model '" + NERmodelPath + "' and properties '" + NERpropPath + "'");
		final Properties properties = new Properties();
		try (final InputStream propStream = getClass().getClassLoader().getResourceAsStream(NERpropPath)) {
			if (propStream == null) {
				throw new IOException("Couldn't find model file " + NERpropPath + "!");
			}
			properties.load(propStream);
			namedEntityRecognizer = new NERClassifierCombiner(applyNumericalClassifiers, useSUTime,
					CRFClassifier.getClassifier(NERmodelPath, properties));
			logger.debug("Successfully initialized StanfordNER");
		}
		catch (ClassCastException | ClassNotFoundException e) {
			throw new IOException("Failed to load model file!", e);
		}
	}

	@Override
	public Set<Requirement> requires() {
		// from NERCombinerAnnotator source code:
		// TODO: we could check the models to see which ones use lemmas and which ones use pos tags
		if (namedEntityRecognizer.usesSUTime() || namedEntityRecognizer.appliesNumericClassifiers()) {
			return Annotator.TOKENIZE_SSPLIT_POS_LEMMA;
		}
		else {
			return Annotator.TOKENIZE_AND_SSPLIT;
		}
	}

	@Override
	public Set<Requirement> requirementsSatisfied() {
		return Collections.unmodifiableSet(
				new ArraySet<>(ToponymRecognitionAnnotator.TOPONYM_RECOGNITION_REQUIREMENT, Annotator.NER_REQUIREMENT));
	}

	@Override
	public List<CoreMap> recognize(final List<CoreLabel> tokens, final Annotation document, final CoreMap sentence) {
		final List<CoreLabel> output = namedEntityRecognizer.classifySentenceWithGlobalInformation(tokens, document,
				sentence);

		final int tokenOffset = sentence == null ? 0 : sentence.get(CoreAnnotations.TokenBeginAnnotation.class);

		final List<CoreMap> chunkedMentions = chunkIdentifier.getAnnotatedChunks(output, tokenOffset,
				CoreAnnotations.TextAnnotation.class, CoreAnnotations.NamedEntityTagAnnotation.class,
				IS_TOKENS_COMPATIBLE);

		return chunkedMentions;
	}

	/*
	 * Code from: edu.stanford.nlp.pipeline.EntityMentionsAnnotator
	 */
	// TODO: copied code without understanding it... Could at least replace hardcoded strings
	private static final Function<Pair<CoreLabel, CoreLabel>, Boolean> IS_TOKENS_COMPATIBLE = in -> {
		// First argument is the current token
		final CoreLabel cur = in.first;
		// Second argument the previous token
		final CoreLabel prev = in.second;

		if (cur == null || prev == null) {
			return false;
		}

		// Get NormalizedNamedEntityTag and say two entities are incompatible if they are different
		final String v1 = cur.get(CoreAnnotations.NormalizedNamedEntityTagAnnotation.class);
		final String v2 = prev.get(CoreAnnotations.NormalizedNamedEntityTagAnnotation.class);
		if (!checkStrings(v1, v2)) {
			return false;
		}

		// This duplicates logic in the QuantifiableEntityNormalizer (but maybe we will get rid of that class)
		final String nerTag = cur.get(CoreAnnotations.NamedEntityTagAnnotation.class);
		if ("NUMBER".equals(nerTag) || "ORDINAL".equals(nerTag)) {
			// Get NumericCompositeValueAnnotation and say two entities are incompatible if they are different
			final Number n1 = cur.get(CoreAnnotations.NumericCompositeValueAnnotation.class);
			final Number n2 = prev.get(CoreAnnotations.NumericCompositeValueAnnotation.class);
			if (!checkNumbers(n1, n2)) {
				return false;
			}
		}

		// Check timex...
		if ("TIME".equals(nerTag) || "SET".equals(nerTag) || "DATE".equals(nerTag) || "DURATION".equals(nerTag)) {
			final Timex timex1 = cur.get(TimeAnnotations.TimexAnnotation.class);
			final Timex timex2 = prev.get(TimeAnnotations.TimexAnnotation.class);
			final String tid1 = (timex1 != null) ? timex1.tid() : null;
			final String tid2 = (timex2 != null) ? timex2.tid() : null;
			if (!checkStrings(tid1, tid2)) {
				return false;
			}
		}

		return true;
	};

	private static boolean checkStrings(final String s1, final String s2) {
		if (s1 == null || s2 == null) {
			return Objects.equals(s1, s2);
		}
		else {
			return s1.equals(s2);
		}
	}

	private static boolean checkNumbers(final Number n1, final Number n2) {
		if (n1 == null || n2 == null) {
			return Objects.equals(n1, n2);
		}
		else {
			return n1.equals(n2);
		}
	}

}