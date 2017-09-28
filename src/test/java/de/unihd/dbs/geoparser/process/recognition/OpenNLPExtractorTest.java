package de.unihd.dbs.geoparser.process.recognition;

import static org.hamcrest.Matchers.*;
import static org.junit.Assert.*;

import java.io.IOException;
import java.util.Arrays;
import java.util.List;

import org.junit.BeforeClass;
import org.junit.Test;
import de.unihd.dbs.geoparser.core.Document;
import de.unihd.dbs.geoparser.core.GeoparserConfig;
import de.unihd.dbs.geoparser.core.GeoparserConfig.UnknownConfigLabelException;
import de.unihd.dbs.geoparser.core.NamedEntity;
import de.unihd.dbs.geoparser.core.NamedEntityType;
import de.unihd.dbs.geoparser.demo.GeoparsingPipelineFactory;
import de.unihd.dbs.geoparser.util.GeoparserUtil;

import edu.stanford.nlp.pipeline.AnnotationPipeline;

public class OpenNLPExtractorTest {

	private static GeoparserConfig config;
	private static AnnotationPipeline pipeline;

	@BeforeClass
	public static void setupPipeline() throws IOException, UnknownConfigLabelException {
		config = new GeoparserConfig();
		pipeline = GeoparsingPipelineFactory.buildOpenNLPRecognitionPipeline(config);
	}

	@Test(expected = NullPointerException.class)
	public void testOpenNLPExtractorWithNullConfig() throws UnknownConfigLabelException, IOException {
		new OpenNLPExtractor(null);
	}

	@Test
	public void testOpenNLPExtractorWithManualPaths() throws IOException, UnknownConfigLabelException {
		new OpenNLPExtractor(config.getConfigStringByLabel(OpenNLPExtractor.CONFIG_OPENNLP_NER_MODEL_PATH_LABEL),
				config.getConfigStringByLabel(OpenNLPExtractor.CONFIG_OPENNLP_TOKENIZER_MODEL_PATH_LABEL),
				config.getConfigStringByLabel(OpenNLPExtractor.CONFIG_OPENNLP_SENTENCE_DETECTOR_MODEL_PATH_LABEL));
	}

	@Test(expected = IOException.class)
	public void testNERModelFilesDoNotExist() throws IOException {
		new OpenNLPExtractor("fake_ner_model_file", "fake_tokenizer_file", "fake_sentence_detector_file");
	}

	@Test(expected = IOException.class)
	public void testTokenizerModelFileDoesNotExist() throws IOException, UnknownConfigLabelException {
		new OpenNLPExtractor(config.getConfigStringByLabel(OpenNLPExtractor.CONFIG_OPENNLP_NER_MODEL_PATH_LABEL),
				"fake_tokenizer_file", "fake_sentence_detector_file");
	}

	@Test(expected = IOException.class)
	public void testSentenceDetectorModelFileDoesNotExist() throws IOException, UnknownConfigLabelException {
		new OpenNLPExtractor(config.getConfigStringByLabel(OpenNLPExtractor.CONFIG_OPENNLP_NER_MODEL_PATH_LABEL),
				config.getConfigStringByLabel(OpenNLPExtractor.CONFIG_OPENNLP_TOKENIZER_MODEL_PATH_LABEL),
				"fake_sentence_detector_file");
	}

	@Test
	public void testRecognizeSimpleEntities() {
		// @formatter:off
		//                           0         10        20        30        40        50        60
		//                           012345678901234567890123456789012345678901234567890123456789012345
		// @formatter:on
		final String documentText = "My name is Ludwig, I was born in Munich and studied in Germany.";
		final Document document = new Document(documentText);
		final List<NamedEntity> expectedNamedEntities = Arrays.asList(
				new NamedEntity("Munich", 33, 39, NamedEntityType.LOCATION, null),
				new NamedEntity("Germany", 55, 62, NamedEntityType.LOCATION, null));

		pipeline.annotate(document);
		final List<NamedEntity> actualNamedEntities = GeoparserUtil.getNamedEntities(document);

		assertThat(actualNamedEntities, equalTo(expectedNamedEntities));
	}

	@Test
	public void testRecognizeMultiNameEntities() {
		// @formatter:off
		//      					 0         10        20        30        40        50        60
		//                           0123456789012345678901234567890123456789012345678901234567890123
		// @formatter:on
		final String documentText = "I was born in New York, a city in the United States of America.";
		final Document document = new Document(documentText);
		final List<NamedEntity> expectedNamedEntities = Arrays.asList(
				new NamedEntity("New York", 14, 22, NamedEntityType.LOCATION, null),
				new NamedEntity("United States of America", 38, 62, NamedEntityType.LOCATION, null));

		pipeline.annotate(document);
		final List<NamedEntity> actualNamedEntities = GeoparserUtil.getNamedEntities(document);

		assertThat(actualNamedEntities, equalTo(expectedNamedEntities));
	}

}
