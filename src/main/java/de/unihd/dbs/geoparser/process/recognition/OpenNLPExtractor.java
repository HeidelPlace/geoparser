package de.unihd.dbs.geoparser.process.recognition;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import de.unihd.dbs.geoparser.core.GeoparserConfig;
import de.unihd.dbs.geoparser.core.NamedEntityType;
import de.unihd.dbs.geoparser.core.GeoparserConfig.UnknownConfigLabelException;

import edu.stanford.nlp.ling.CoreAnnotations;
import edu.stanford.nlp.ling.CoreLabel;
import edu.stanford.nlp.pipeline.Annotation;
import edu.stanford.nlp.pipeline.Annotator;
import edu.stanford.nlp.pipeline.Annotator.Requirement;
import edu.stanford.nlp.util.ArrayCoreMap;
import edu.stanford.nlp.util.ArraySet;
import edu.stanford.nlp.util.CoreMap;
import opennlp.tools.namefind.NameFinderME;
import opennlp.tools.namefind.TokenNameFinderModel;
import opennlp.tools.util.Span;

/**
 * Implementation of {@link ToponymRecognitionAnnotator} that extracts toponyms from unstructured text documents using
 * the Apache OpenNLP Name Finder.
 * <p>
 * Note: Patterned after com.bericotech.clavin.extractor.ApacheExtractor
 * 
 * @author CLAVIN, lrichter
 *
 */
// TODO: either remove tokenizer and sentence detector model stuff completely or provide separate annotation modules...
// Its just trash here, originating from the CLAVIN code
public class OpenNLPExtractor extends ToponymRecognizer {

	public static final String CONFIG_OPENNLP_SENTENCE_DETECTOR_MODEL_PATH_LABEL = "opennlp.sentence_detector.model.path";
	public static final String CONFIG_OPENNLP_TOKENIZER_MODEL_PATH_LABEL = "opennlp.tokenizer.model.path";
	public static final String CONFIG_OPENNLP_NER_MODEL_PATH_LABEL = "opennlp.ner.model.path";

	private static final Logger logger = LoggerFactory.getLogger(OpenNLPExtractor.class);

	private NameFinderME nameFinder;
	// private TokenizerME tokenizer;
	// private SentenceDetectorME sentenceDetector;

	/**
	 * Create an {@link OpenNLPExtractor} by instantiating the OpenNLP Name Finder and Tokenizer with the language model
	 * parameters specified in the given GeoParser configuration.
	 * 
	 * @param config the GeoParser configuration.
	 * @throws IOException if reading the language model files failed.
	 * @throws UnknownConfigLabelException if the configuration string labels are invalid.
	 */
	public OpenNLPExtractor(final GeoparserConfig config) throws UnknownConfigLabelException, IOException {
		Objects.requireNonNull(config);

		init(config.getConfigStringByLabel(CONFIG_OPENNLP_NER_MODEL_PATH_LABEL),
				config.getConfigStringByLabel(CONFIG_OPENNLP_TOKENIZER_MODEL_PATH_LABEL),
				config.getConfigStringByLabel(CONFIG_OPENNLP_SENTENCE_DETECTOR_MODEL_PATH_LABEL));
	}

	/**
	 * Create a {@link OpenNLPExtractor} by instantiating the OpenNLP Name Finder and Tokenizer with the specified model
	 * parameters.
	 * <p>
	 * <b>Note</b>If the model parameter sources are given as relative path, they need to be located under "resources",
	 * else they should be specified as absolute path.
	 * 
	 * @param NERmodelPath path to OpenNLP NER language model.
	 * @param tokenizerModelPath path to OpenNLP Tokenizer model.
	 * @param sentenceDetectorModelPath path to OpenNLP Sentence Detector model.
	 * @throws IOException if reading the language model files failed.
	 */
	public OpenNLPExtractor(final String NERmodelPath, final String tokenizerModelPath,
			final String sentenceDetectorModelPath) throws IOException {
		init(NERmodelPath, tokenizerModelPath, sentenceDetectorModelPath);
	}

	private void init(final String NERmodelPath, final String tokenizerModelPath,
			final String sentenceDetectorModelPath) throws IOException {
		logger.debug("Initializing OpenNLPExtractor with model '" + NERmodelPath + "', tokenizer '" + tokenizerModelPath
				+ "' and detector '" + sentenceDetectorModelPath + "'");
		// see http://stackoverflow.com/a/14739608 for why we use getClassLoader()...
		try (final InputStream nameFinderStream = getClass().getClassLoader().getResourceAsStream(NERmodelPath);
				final InputStream tokenizerStream = getClass().getClassLoader().getResourceAsStream(tokenizerModelPath);
				final InputStream sentenceDetectorStream = getClass().getClassLoader()
						.getResourceAsStream(sentenceDetectorModelPath);) {
			if (nameFinderStream == null) {
				throw new IOException("Couldn't find model file " + NERmodelPath + "!");
			}
			if (tokenizerStream == null) {
				throw new IOException("Couldn't find model file " + tokenizerModelPath + "!");
			}
			if (sentenceDetectorStream == null) {
				throw new IOException("Couldn't find model file " + sentenceDetectorModelPath + "!");
			}

			nameFinder = new NameFinderME(new TokenNameFinderModel(nameFinderStream));
			// tokenizer = new TokenizerME(new TokenizerModel(tokenizerStream));
			// sentenceDetector = new SentenceDetectorME(new SentenceModel(sentenceDetectorStream));
		}

		logger.debug("Successfully initialized OpenNLPExtractor");
	}

	@Override
	public Set<Requirement> requires() {
		return Annotator.TOKENIZE_AND_SSPLIT;
	}

	@Override
	public Set<Requirement> requirementsSatisfied() {
		return Collections.unmodifiableSet(
				new ArraySet<>(ToponymRecognitionAnnotator.TOPONYM_RECOGNITION_REQUIREMENT, Annotator.NER_REQUIREMENT));
	}

	@Override
	public List<CoreMap> recognize(final List<CoreLabel> tokens, final Annotation document, final CoreMap sentence) {
		final List<CoreMap> mentions = new ArrayList<>();
		final String[] openNlpTokens = tokens.stream().map(token -> token.word()).collect(Collectors.toList())
				.toArray(new String[tokens.size()]);
		// the values used in these Spans are NOT string character offsets, they are indices into 'openNlpTokens'
		final Span names[] = nameFinder.find(openNlpTokens);
		for (final Span name : names) {
			// FIXME: currently we do not adjust the tokens itself
			final CoreMap mention = new ArrayCoreMap();
			mention.set(CoreAnnotations.CharacterOffsetBeginAnnotation.class,
					tokens.get(name.getStart()).beginPosition());
			mention.set(CoreAnnotations.CharacterOffsetEndAnnotation.class,
					tokens.get(name.getEnd() - 1).endPosition());
			mention.set(CoreAnnotations.TokensAnnotation.class, tokens.subList(name.getStart(), name.getEnd()));
			mention.set(CoreAnnotations.TokenBeginAnnotation.class, tokens.get(name.getStart()).index());
			mention.set(CoreAnnotations.TokenEndAnnotation.class, tokens.get(name.getEnd() - 1).index());
			final String coveredText = tokens.subList(name.getStart(), name.getEnd()).stream()
					.map(token -> token.word()).collect(Collectors.joining(" "));
			mention.set(CoreAnnotations.TextAnnotation.class, coveredText);
			mention.set(CoreAnnotations.NamedEntityTagAnnotation.class,
					NamedEntityType.fromOpenNLPtag(name.getType()).name);

			mentions.add(mention);
		}

		// this is necessary to maintain consistent results across multiple runs on the same data, which is what we want
		nameFinder.clearAdaptiveData();

		return mentions;
	}

}