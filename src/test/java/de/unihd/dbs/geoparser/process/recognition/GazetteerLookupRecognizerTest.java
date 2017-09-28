package de.unihd.dbs.geoparser.process.recognition;

import static org.hamcrest.Matchers.*;
import static org.junit.Assert.*;

import java.util.Arrays;
import java.util.List;

import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Ignore;
import org.junit.Test;
import de.unihd.dbs.geoparser.core.Document;
import de.unihd.dbs.geoparser.core.GeoparserConfig;
import de.unihd.dbs.geoparser.core.NamedEntity;
import de.unihd.dbs.geoparser.core.NamedEntityType;
import de.unihd.dbs.geoparser.demo.GeoparsingPipelineFactory;
import de.unihd.dbs.geoparser.gazetteer.Gazetteer;
import de.unihd.dbs.geoparser.gazetteer.util.GazetteerPersistenceManager;
import de.unihd.dbs.geoparser.util.GeoparserUtil;

import edu.stanford.nlp.ling.CoreAnnotations.PartOfSpeechAnnotation;
import edu.stanford.nlp.ling.CoreAnnotations.TokensAnnotation;
import edu.stanford.nlp.ling.CoreLabel;
import edu.stanford.nlp.pipeline.AnnotationPipeline;

public class GazetteerLookupRecognizerTest {

	private static boolean VERBOSE = false;

	public static final String WORKING_GAZETTEER_PERSISTENCE_UNIT_NAME_LABEL = "gazetteer.working.persistence_unit.name";
	public static final String WORKING_GAZETTEER_PERSISTENCE_UNIT_DB_CONNECTION_INFO_LABEL = "gazetteer.working.persistence_unit.db_source";

	private static GazetteerPersistenceManager gpm;
	private static Gazetteer gazetteer;
	private static AnnotationPipeline pipeline;

	private static void printPOSTags(final Document document) {
		if (VERBOSE) {
			final List<CoreLabel> tokens = document.get(TokensAnnotation.class);
			for (final CoreLabel token : tokens) {
				System.out.println(token.word() + ": " + token.get(PartOfSpeechAnnotation.class));
			}
		}
	}

	private static void printNEs(final List<NamedEntity> namedEntities) {
		if (VERBOSE) {
			namedEntities.forEach(ne -> System.out.println(ne.text));
		}
	}

	@BeforeClass
	public static void setUpBeforeClass() throws Exception {
		final GeoparserConfig config = new GeoparserConfig();
		gpm = new GazetteerPersistenceManager(
				config.getConfigStringByLabel(WORKING_GAZETTEER_PERSISTENCE_UNIT_NAME_LABEL),
				config.getDBConnectionInfoByLabel(
						config.getConfigStringByLabel(WORKING_GAZETTEER_PERSISTENCE_UNIT_DB_CONNECTION_INFO_LABEL)));
		gazetteer = new Gazetteer(gpm.getEntityManager());
		pipeline = GeoparsingPipelineFactory.buildGazetteerLookupRecognizerPipeline(config, gazetteer);
	}

	@AfterClass
	public static void tearDownAfterClass() throws Exception {
		try {
			if (gazetteer != null) {
				gazetteer.close();
			}
		}
		finally {
			gpm.close();
		}
	}

	@Test
	public void testRecognizeSingleProperNounNames() {
		// @formatter:off
		//                           0         10        20        30        40        50        60
		//                           0123456789012345678901234567890123456789012345678901234567890123456789
		// @formatter:on
		final String documentText = "My name is Ludwig, I was born in Munich and studied in Germany.";
		final Document document = new Document(documentText);
		final List<NamedEntity> expectedNamedEntities = Arrays.asList(
				new NamedEntity("Ludwig", 11, 17, NamedEntityType.LOCATION, null),
				new NamedEntity("Munich", 33, 39, NamedEntityType.LOCATION, null),
				new NamedEntity("Germany", 55, 62, NamedEntityType.LOCATION, null));

		pipeline.annotate(document);
		printPOSTags(document);

		final List<NamedEntity> actualNamedEntities = GeoparserUtil.getNamedEntities(document);
		printNEs(actualNamedEntities);

		assertThat(actualNamedEntities, equalTo(expectedNamedEntities));
	}

	@Test
	public void testRecognizeTwoProperNounNames() {
		// @formatter:off
		//      					 0         10        20        30        40        50        60
		//                           0123456789012345678901234567890123456789012345678901234567890123456789
		// @formatter:on
		final String documentText = "I was born in New York.";
		final Document document = new Document(documentText);
		final List<NamedEntity> expectedNamedEntities = Arrays
				.asList(new NamedEntity("New York", 14, 22, NamedEntityType.LOCATION, null));
		pipeline.annotate(document);
		printPOSTags(document);

		final List<NamedEntity> actualNamedEntities = GeoparserUtil.getNamedEntities(document);
		printNEs(actualNamedEntities);

		assertThat(actualNamedEntities, equalTo(expectedNamedEntities));
	}

	@Test
	@Ignore
	// currently not working :/
	public void testRecognizeThreeProperNounNames() {
		// @formatter:off
		//      					 0         10        20        30        40        50        60
		//                           0123456789012345678901234567890123456789012345678901234567890123456789
		// @formatter:on
		final String documentText = "I work for New York Times.";
		final Document document = new Document(documentText);
		pipeline.annotate(document);
		printPOSTags(document);

		final List<NamedEntity> actualNamedEntities = GeoparserUtil.getNamedEntities(document);
		printNEs(actualNamedEntities);

		assertThat(actualNamedEntities, empty());
	}

	@Test
	public void testRecognizeMultiNameEntitiesWithOf() {
		// @formatter:off
		//      					 0         10        20        30        40        50        60
		//                           0123456789012345678901234567890123456789012345678901234567890123456789
		// @formatter:on
		final String documentText = "The United States of America!";
		final Document document = new Document(documentText);
		final List<NamedEntity> expectedNamedEntities = Arrays
				.asList(new NamedEntity("United States of America", 4, 28, NamedEntityType.LOCATION, null));
		pipeline.annotate(document);
		printPOSTags(document);

		final List<NamedEntity> actualNamedEntities = GeoparserUtil.getNamedEntities(document);
		printNEs(actualNamedEntities);

		assertThat(actualNamedEntities, equalTo(expectedNamedEntities));
	}

	@Test
	public void testRecognizeHardMultiNameEntities() {
		// @formatter:off
		//      					 0         10        20        30        40        50        60
		//                           0123456789012345678901234567890123456789012345678901234567890123456789
		// @formatter:on
		final String documentText = "Equatorial Guinea?";
		final Document document = new Document(documentText);
		final List<NamedEntity> expectedNamedEntities = Arrays
				.asList(new NamedEntity("Equatorial Guinea", 0, 17, NamedEntityType.LOCATION, null));
		pipeline.annotate(document);
		printPOSTags(document);

		final List<NamedEntity> actualNamedEntities = GeoparserUtil.getNamedEntities(document);
		printNEs(actualNamedEntities);

		assertThat(actualNamedEntities, equalTo(expectedNamedEntities));
	}

}
