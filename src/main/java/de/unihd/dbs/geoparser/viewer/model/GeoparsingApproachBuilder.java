package de.unihd.dbs.geoparser.viewer.model;

import java.io.IOException;
import java.net.URISyntaxException;

import de.unihd.dbs.geoparser.Geoparser;
import de.unihd.dbs.geoparser.core.GeoparserConfig;
import de.unihd.dbs.geoparser.core.GeoparserConfig.UnknownConfigLabelException;
import de.unihd.dbs.geoparser.demo.GeoparsingPipelineFactory;
import de.unihd.dbs.geoparser.gazetteer.Gazetteer;
import de.unihd.dbs.geoparser.gazetteer.models.PlaceType;
import de.unihd.dbs.geoparser.gazetteer.types.PlaceTypes;
import de.unihd.dbs.geoparser.process.disambiguation.ToponymDisambiguationAnnotator;
import de.unihd.dbs.geoparser.process.linking.ToponymLinkingAnnotator;
import de.unihd.dbs.geoparser.process.recognition.GazetteerLookupRecognizer;
import de.unihd.dbs.geoparser.process.recognition.ToponymRecognitionAnnotator;
import de.unihd.dbs.geoparser.util.StopWordProvider;

import edu.stanford.nlp.pipeline.AnnotationPipeline;
import edu.stanford.nlp.pipeline.Annotator;
import edu.stanford.nlp.pipeline.TokenizerAnnotator.TokenizerType;

public class GeoparsingApproachBuilder {

	private static final int MAX_MATCHES = 500;

	// we keep instances of once created geoparsing modules for performant reuse
	private Annotator tokenizerAnnotator;
	private Annotator sentenceAnnotator;
	private Annotator posAnnotator;
	private ToponymRecognitionAnnotator stanfordNERAnnotator;
	private ToponymRecognitionAnnotator openNLPAnnotator;
	private ToponymRecognitionAnnotator gazetteerLookupRecognitionAnnotator;
	private ToponymLinkingAnnotator exactLinkingAnnotator;
	private ToponymDisambiguationAnnotator firstMatchDisamiguationAnnotator;
	private ToponymDisambiguationAnnotator highestPopulationDisamiguationAnnotator;
	private ToponymDisambiguationAnnotator highestAdminLevelDisamiguationAnnotator;
	private ToponymDisambiguationAnnotator contextDisamiguationAnnotator;
	private ToponymDisambiguationAnnotator advancedContextDisamiguationAnnotator;
	private final Gazetteer gazetteer;
	private final GeoparserConfig config;

	public GeoparsingApproachBuilder(final GeoparserConfig config, final Gazetteer gazetteer) {
		this.gazetteer = gazetteer;
		this.config = config;
	}

	public void initGeoparser(final GeoparsingApproach geoparsingApproach) {
		final AnnotationPipeline recognitionPipeline = new AnnotationPipeline();
		final AnnotationPipeline linkingPipeline = new AnnotationPipeline();
		final AnnotationPipeline disambiguationPipeline = new AnnotationPipeline();
		final AnnotationPipeline spatialInferrencePipeline = new AnnotationPipeline();

		recognitionPipeline.addAnnotator(initTokenizerAnnotator());
		recognitionPipeline.addAnnotator(initSentenceAnnotator());
		switch (geoparsingApproach.recognitionModule) {
		case GAZETTEER_LOOKUP:
			recognitionPipeline.addAnnotator(initPOSAnnotator());
			recognitionPipeline.addAnnotator(initGazetteerRecognitionAnnotator());
			break;
		case OPEN_NLP:
			recognitionPipeline.addAnnotator(initOpenNLPAnnotator());
			break;
		case STANFORD_NER:
			recognitionPipeline.addAnnotator(initPOSAnnotator());
			recognitionPipeline.addAnnotator(initStanfordNERAnnotator());
			break;
		default:
			break;
		}

		switch (geoparsingApproach.linkingModule) {
		case GAZETTEER_LOOKUP_EXACT:
			linkingPipeline.addAnnotator(initExactLinkingAnnotator());
			break;
		default:
			break;
		}

		switch (geoparsingApproach.disambiguationModule) {
		case FIRST_MATCH_DISAMBIGUATION:
			disambiguationPipeline.addAnnotator(initFirstMatchDisambiguationAnnotator());
			break;
		case HIGHEST_POPULATION_DISAMBIGUATION:
			disambiguationPipeline.addAnnotator(initHighestPopulationDisambiguationAnnotator());
			break;
		case HIGHEST_ADMIN_LEVEL_DISAMBIGUATION:
			disambiguationPipeline.addAnnotator(initHighestAdminLevelDisambiguationAnnotator());
			break;
		case CONTEXT_TOPONYM_DISAMBIGUATION:
			disambiguationPipeline.addAnnotator(initContextDisambiguationAnnotator());
			break;
		case ADVANCED_CONTEXT_TOPONYM_DISAMBIGUATION:
			disambiguationPipeline.addAnnotator(initAdvancedContextDisambiguationAnnotator());
			break;
		default:
			break;

		}
		geoparsingApproach.geoparser = new Geoparser(recognitionPipeline, linkingPipeline, disambiguationPipeline,
				spatialInferrencePipeline);
	}

	private Annotator initTokenizerAnnotator() {
		if (tokenizerAnnotator == null) {
			tokenizerAnnotator = GeoparsingPipelineFactory.buildStanfordTokenizerAnnotator(TokenizerType.English);
		}

		return tokenizerAnnotator;
	}

	private Annotator initSentenceAnnotator() {
		if (sentenceAnnotator == null) {
			sentenceAnnotator = GeoparsingPipelineFactory.buildWordsToSentencesAnnotator();
		}

		return sentenceAnnotator;
	}

	private Annotator initPOSAnnotator() {
		if (posAnnotator == null) {
			try {
				posAnnotator = GeoparsingPipelineFactory.buildStanfordPOSAnnotator(config);
			}
			catch (final UnknownConfigLabelException e) {
				throw new RuntimeException(e);
			}
		}

		return posAnnotator;
	}

	private Annotator initStanfordNERAnnotator() {
		if (stanfordNERAnnotator == null) {
			try {
				stanfordNERAnnotator = GeoparsingPipelineFactory.buildStanfordNERAnnotator(config);
			}
			catch (UnknownConfigLabelException | IOException e) {
				throw new RuntimeException(e);
			}
		}

		return stanfordNERAnnotator;
	}

	private Annotator initOpenNLPAnnotator() {
		if (openNLPAnnotator == null) {
			try {
				openNLPAnnotator = GeoparsingPipelineFactory.buildOpenNLPToponymRecognitionAnnotator(config);
			}
			catch (UnknownConfigLabelException | IOException e) {
				throw new RuntimeException(e);
			}
		}

		return openNLPAnnotator;
	}

	private Annotator initGazetteerRecognitionAnnotator() {
		if (gazetteerLookupRecognitionAnnotator == null) {
			gazetteerLookupRecognitionAnnotator = GeoparsingPipelineFactory
					.buildGazetteerLookupRecognitionAnnotator(gazetteer);
			final GazetteerLookupRecognizer recognizer = (GazetteerLookupRecognizer) gazetteerLookupRecognitionAnnotator
					.getRecognitionModule();
			StopWordProvider stopWordProvider;
			try {
				stopWordProvider = new StopWordProvider(config);
			}
			catch (IOException | UnknownConfigLabelException | URISyntaxException e) {
				throw new RuntimeException(e);
			}
			recognizer.setFilterStopWords(false);
			recognizer.setStopWords(stopWordProvider.getStopWordsForLanguage("english"));

		}

		return gazetteerLookupRecognitionAnnotator;
	}

	private Annotator initExactLinkingAnnotator() {
		if (exactLinkingAnnotator == null) {
			exactLinkingAnnotator = GeoparsingPipelineFactory.buildGazetteerExactToponymLinkerAnnotator(gazetteer,
					MAX_MATCHES);
		}

		return exactLinkingAnnotator;
	}

	private Annotator initFirstMatchDisambiguationAnnotator() {
		if (firstMatchDisamiguationAnnotator == null) {
			firstMatchDisamiguationAnnotator = GeoparsingPipelineFactory.buildFirstMatchDisambiguationAnnotator();
		}

		return firstMatchDisamiguationAnnotator;
	}

	private Annotator initHighestPopulationDisambiguationAnnotator() {
		if (highestPopulationDisamiguationAnnotator == null) {
			highestPopulationDisamiguationAnnotator = GeoparsingPipelineFactory
					.buildHighestPopulationDisambiguationAnnotator();
		}

		return highestPopulationDisamiguationAnnotator;
	}

	private Annotator initHighestAdminLevelDisambiguationAnnotator() {
		if (highestAdminLevelDisamiguationAnnotator == null) {
			final PlaceType adminEntity = (PlaceType) gazetteer.getType(PlaceTypes.ADMINISTRATIVE_DIVISION.typeName);
			highestAdminLevelDisamiguationAnnotator = GeoparsingPipelineFactory
					.buildAdminLevelDisambiguationAnnotator(adminEntity);
		}

		return highestAdminLevelDisamiguationAnnotator;
	}

	private Annotator initContextDisambiguationAnnotator() {
		if (contextDisamiguationAnnotator == null) {
			contextDisamiguationAnnotator = GeoparsingPipelineFactory.buildContextDisambiguationAnnotator();
		}

		return contextDisamiguationAnnotator;
	}

	private Annotator initAdvancedContextDisambiguationAnnotator() {
		if (advancedContextDisamiguationAnnotator == null) {
			advancedContextDisamiguationAnnotator = GeoparsingPipelineFactory.buildAdvancedContextDisambiguationAnnotator();
		}

		return advancedContextDisamiguationAnnotator;
	}

}
