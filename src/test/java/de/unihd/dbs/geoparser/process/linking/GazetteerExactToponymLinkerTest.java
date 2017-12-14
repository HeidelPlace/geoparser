package de.unihd.dbs.geoparser.process.linking;

import static org.hamcrest.Matchers.*;
import static org.junit.Assert.*;

import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Ignore;
import org.junit.Test;
import de.unihd.dbs.geoparser.core.Document;
import de.unihd.dbs.geoparser.core.GeoparserConfig;
import de.unihd.dbs.geoparser.core.GeoparserConfig.UnknownConfigLabelException;
import de.unihd.dbs.geoparser.core.LinkedToponym;
import de.unihd.dbs.geoparser.demo.GeoparsingPipelineFactory;
import de.unihd.dbs.geoparser.gazetteer.Gazetteer;
import de.unihd.dbs.geoparser.gazetteer.models.PlaceType;
import de.unihd.dbs.geoparser.gazetteer.query.PlaceTypePlaceFilter;
import de.unihd.dbs.geoparser.gazetteer.types.PlaceTypes;
import de.unihd.dbs.geoparser.gazetteer.util.GazetteerPersistenceManager;
import de.unihd.dbs.geoparser.util.GeoparserUtil;

import edu.stanford.nlp.pipeline.AnnotationPipeline;

public class GazetteerExactToponymLinkerTest {

	public static final String WORKING_GAZETTEER_PERSISTENCE_UNIT_NAME_LABEL = "gazetteer.persistence_unit.name";
	public static final String WORKING_GAZETTEER_PERSISTENCE_UNIT_DB_CONNECTION_INFO_LABEL = "gazetteer.persistence_unit.db_source";

	private static GazetteerPersistenceManager gpm;
	private static Gazetteer gazetteer;
	private static AnnotationPipeline pipeline;
	private static GeoparserConfig config;

	@BeforeClass
	public static void setUpBeforeClass() throws Exception {
		config = new GeoparserConfig();
		gpm = new GazetteerPersistenceManager(
				config.getConfigStringByLabel(WORKING_GAZETTEER_PERSISTENCE_UNIT_NAME_LABEL),
				config.getDBConnectionInfoByLabel(
						config.getConfigStringByLabel(WORKING_GAZETTEER_PERSISTENCE_UNIT_DB_CONNECTION_INFO_LABEL)));
		gazetteer = new Gazetteer(gpm.getEntityManager());
		pipeline = GeoparsingPipelineFactory.buildGazetteerLookupRecognizerAndExactLinkingPipeline(config, gazetteer);
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
	public void testLinkSimpleEntities() {
		// @formatter:off
		//                           0         10        20        30        40        50        60
		//                           012345678901234567890123456789012345678901234567890123456789012345
		// @formatter:on
		final String documentText = "My name is Ludwig, I was born in Munich and studied in Germany.";
		final Document document = new Document(documentText);
		final List<String> expectedLinkedToponymNames = Arrays.asList("Ludwig", "Munich", "Germany");

		pipeline.annotate(document);
		final List<LinkedToponym> actualLinkedToponyms = GeoparserUtil.getLinkedToponyms(document);

		assertThat(actualLinkedToponyms.stream().map(toponym -> toponym.text).collect(Collectors.toList()),
				equalTo(expectedLinkedToponymNames));
	}

	@Test
	@Ignore
	// the filtering logic should go somewhere else not in the linker itself.
	public void testFilterByPlaceType() throws UnknownConfigLabelException {
		// @formatter:off
		//                           0         10        20        30        40        50        60
		//                           012345678901234567890123456789012345678901234567890123456789012345
		// @formatter:on
		final String documentText = "My name is Ludwig, I was born in Munich and studied in Germany.";
		final Document document = new Document(documentText);
		final Set<PlaceType> placeTypes = new HashSet<>(
				Arrays.asList((PlaceType) gazetteer.getType(PlaceTypes.POLITICAL_ENTITY.typeName)));
		final PlaceTypePlaceFilter placeTypeFilter = new PlaceTypePlaceFilter(placeTypes, false);
		final List<String> expectedLinkedToponymNames = Arrays.asList("Germany");
		final AnnotationPipeline otherPipeline = GeoparsingPipelineFactory
				.buildGazetteerLookupRecognizerPipeline(config, gazetteer);
		otherPipeline.addAnnotator(
				new ToponymLinkingAnnotator(new GazetteerExactToponymLinker(gazetteer, 5, placeTypeFilter)));

		otherPipeline.annotate(document);
		final List<LinkedToponym> actualLinkedToponyms = GeoparserUtil.getLinkedToponyms(document);

		assertThat(actualLinkedToponyms.stream().map(toponym -> toponym.text).collect(Collectors.toList()),
				equalTo(expectedLinkedToponymNames));
	}

}
