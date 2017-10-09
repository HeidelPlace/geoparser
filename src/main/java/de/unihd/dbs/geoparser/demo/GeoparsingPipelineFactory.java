package de.unihd.dbs.geoparser.demo;

import java.io.IOException;

import de.unihd.dbs.geoparser.core.GeoparserConfig;
import de.unihd.dbs.geoparser.core.GeoparserConfig.UnknownConfigLabelException;
import de.unihd.dbs.geoparser.gazetteer.Gazetteer;
import de.unihd.dbs.geoparser.gazetteer.models.PlaceType;
import de.unihd.dbs.geoparser.process.disambiguation.FirstMatchToponymDisambiguator;
import de.unihd.dbs.geoparser.process.disambiguation.HighestAdminLevelDisambiguator;
import de.unihd.dbs.geoparser.process.disambiguation.HighestPopulationDisambiguator;
import de.unihd.dbs.geoparser.process.disambiguation.ToponymDisambiguationAnnotator;
import de.unihd.dbs.geoparser.process.disambiguation.ContextToponymDisambiguator;
import de.unihd.dbs.geoparser.process.disambiguation.AdvancedContextToponymDisambiguator;
import de.unihd.dbs.geoparser.process.disambiguation.WikipediaLocationNetworkDisambiguator;
import de.unihd.dbs.geoparser.process.linking.GazetteerExactToponymLinker;
import de.unihd.dbs.geoparser.process.linking.ToponymLinkingAnnotator;
import de.unihd.dbs.geoparser.process.recognition.GazetteerLookupRecognizer;
import de.unihd.dbs.geoparser.process.recognition.OpenNLPExtractor;
import de.unihd.dbs.geoparser.process.recognition.StanfordNER;
import de.unihd.dbs.geoparser.process.recognition.ToponymRecognitionAnnotator;

import edu.stanford.nlp.pipeline.AnnotationPipeline;
import edu.stanford.nlp.pipeline.Annotator;
import edu.stanford.nlp.pipeline.POSTaggerAnnotator;
import edu.stanford.nlp.pipeline.TokenizerAnnotator;
import edu.stanford.nlp.pipeline.TokenizerAnnotator.TokenizerType;
import edu.stanford.nlp.pipeline.WordsToSentencesAnnotator;

/**
 * This factory aids in creating annotation pipelines useful for the geoparsing process.
 *
 * @author lrichter
 *
 */
// Refactoring lrichter 16.03.2017: this factory is really clumsy to use. should rework this!! quite in demo state...
public class GeoparsingPipelineFactory extends AnnotationPipeline {

	public static final String CONFIG_STANFORD_POS_MODEL_LABEL = "stanford.pos.model.path";

	public static AnnotationPipeline buildCommonPreprocessingPipeline(final GeoparserConfig config) {
		final AnnotationPipeline pipeline = new AnnotationPipeline();
		pipeline.addAnnotator(buildStanfordTokenizerAnnotator(TokenizerType.English));
		pipeline.addAnnotator(buildWordsToSentencesAnnotator());

		return pipeline;
	}

	public static AnnotationPipeline buildStanfordNERPipeline(final GeoparserConfig config)
			throws UnknownConfigLabelException, IOException {
		final AnnotationPipeline pipeline = buildCommonPreprocessingPipeline(config);
		pipeline.addAnnotator(buildStanfordPOSAnnotator(config));
		pipeline.addAnnotator(buildStanfordNERAnnotator(config));

		return pipeline;
	}

	public static AnnotationPipeline buildOpenNLPRecognitionPipeline(final GeoparserConfig config)
			throws UnknownConfigLabelException, IOException {
		final AnnotationPipeline pipeline = buildCommonPreprocessingPipeline(config);
		pipeline.addAnnotator(buildOpenNLPToponymRecognitionAnnotator(config));

		return pipeline;
	}

	public static AnnotationPipeline buildGazetteerLookupRecognizerPipeline(final GeoparserConfig config,
			final Gazetteer gazetteer) throws UnknownConfigLabelException {
		final AnnotationPipeline pipeline = buildCommonPreprocessingPipeline(config);
		pipeline.addAnnotator(buildStanfordPOSAnnotator(config));
		pipeline.addAnnotator(buildGazetteerLookupRecognitionAnnotator(gazetteer));

		return pipeline;
	}

	public static AnnotationPipeline buildGazetteerLookupRecognizerAndExactLinkingPipeline(final GeoparserConfig config,
			final Gazetteer gazetteer) throws UnknownConfigLabelException {
		final AnnotationPipeline pipeline = buildCommonPreprocessingPipeline(config);
		pipeline.addAnnotator(buildStanfordPOSAnnotator(config));
		pipeline.addAnnotator(buildGazetteerLookupRecognitionAnnotator(gazetteer));
		pipeline.addAnnotator(buildGazetteerExactToponymLinkerAnnotator(gazetteer, 5));

		return pipeline;
	}

	public static Annotator buildWordsToSentencesAnnotator() {
		return new WordsToSentencesAnnotator(false);
	}

	public static Annotator buildStanfordTokenizerAnnotator(final TokenizerType tokenizerType) {
		return new TokenizerAnnotator(false, tokenizerType);
	}

	public static Annotator buildStanfordPOSAnnotator(final GeoparserConfig config) throws UnknownConfigLabelException {
		return buildStanfordPOSAnnotator(config.getConfigStringByLabel(CONFIG_STANFORD_POS_MODEL_LABEL));
	}

	public static Annotator buildStanfordPOSAnnotator(final String POSmodelPath) {
		return new POSTaggerAnnotator(POSmodelPath, false);
	}

	public static Annotator buildStanfordNERAnnotator(final String NERmodelPath, final String NERpropPath)
			throws IOException {
		return new ToponymRecognitionAnnotator(new StanfordNER(NERmodelPath, NERpropPath, true, true));
	}

	public static ToponymRecognitionAnnotator buildStanfordNERAnnotator(final GeoparserConfig config)
			throws UnknownConfigLabelException, IOException {
		return new ToponymRecognitionAnnotator(new StanfordNER(config));
	}

	public static ToponymRecognitionAnnotator buildOpenNLPToponymRecognitionAnnotator(final GeoparserConfig config)
			throws UnknownConfigLabelException, IOException {
		return new ToponymRecognitionAnnotator(new OpenNLPExtractor(config));
	}

	public static ToponymRecognitionAnnotator buildGazetteerLookupRecognitionAnnotator(final Gazetteer gazetteer) {
		return new ToponymRecognitionAnnotator(new GazetteerLookupRecognizer(gazetteer));
	}

	public static ToponymLinkingAnnotator buildGazetteerExactToponymLinkerAnnotator(final Gazetteer gazetteer,
			final int maxMatches) {
		return new ToponymLinkingAnnotator(new GazetteerExactToponymLinker(gazetteer, maxMatches));
	}

	public static ToponymDisambiguationAnnotator buildFirstMatchDisambiguationAnnotator() {
		return new ToponymDisambiguationAnnotator(new FirstMatchToponymDisambiguator());
	}

	public static ToponymDisambiguationAnnotator buildHighestPopulationDisambiguationAnnotator() {
		return new ToponymDisambiguationAnnotator(new HighestPopulationDisambiguator());
	}

	public static ToponymDisambiguationAnnotator buildAdminLevelDisambiguationAnnotator(
			final PlaceType adminLevelRootType) {
		return new ToponymDisambiguationAnnotator(new HighestAdminLevelDisambiguator(adminLevelRootType));
	}

	public static ToponymDisambiguationAnnotator buildContextDisambiguationAnnotator() {
		return new ToponymDisambiguationAnnotator(new ContextToponymDisambiguator());
	}
	public static ToponymDisambiguationAnnotator buildAdvancedContextDisambiguationAnnotator() {
		return new ToponymDisambiguationAnnotator(new AdvancedContextToponymDisambiguator());
	}
	public static ToponymDisambiguationAnnotator buildWikipediaLocationNetworkDisambiguatorAnnotator() {
		return new ToponymDisambiguationAnnotator(new WikipediaLocationNetworkDisambiguator());
	}
}
