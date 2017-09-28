package de.unihd.dbs.geoparser;

import org.junit.runner.RunWith;
import org.junit.runners.Suite;
import org.junit.runners.Suite.SuiteClasses;
import de.unihd.dbs.geoparser.core.DocumentTest;
import de.unihd.dbs.geoparser.core.GeoparserConfigTest;
import de.unihd.dbs.geoparser.core.LinkedToponymTest;
import de.unihd.dbs.geoparser.core.NamedEntityTest;
import de.unihd.dbs.geoparser.core.NamedEntityTypeTest;
import de.unihd.dbs.geoparser.core.PartOfSpeechPTBTypeTest;
import de.unihd.dbs.geoparser.core.PartOfSpeechSTTSTypeTest;
import de.unihd.dbs.geoparser.core.ResolvedLocationTest;
import de.unihd.dbs.geoparser.core.ResolvedToponymTest;
import de.unihd.dbs.geoparser.core.ToponymTest;
import de.unihd.dbs.geoparser.gazetteer.GazetteerTest;
import de.unihd.dbs.geoparser.gazetteer.models.ModelTest;
import de.unihd.dbs.geoparser.gazetteer.models.TypeModelTest;
import de.unihd.dbs.geoparser.process.linking.GazetteerExactToponymLinkerTest;
import de.unihd.dbs.geoparser.process.recognition.GazetteerLookupRecognizerTest;
import de.unihd.dbs.geoparser.process.recognition.OpenNLPExtractorTest;
import de.unihd.dbs.geoparser.process.recognition.StanfordNERTest;
import de.unihd.dbs.geoparser.util.StopWordProviderTest;
import de.unihd.dbs.geoparser.util.dbconnectors.AbstractDBConnectorTest;
import de.unihd.dbs.geoparser.util.dbconnectors.DBConnectionDataTest;
import de.unihd.dbs.geoparser.util.dbconnectors.DBConnectionInfoTest;
import de.unihd.dbs.geoparser.util.dbconnectors.PostgreSQLConnectorTest;
import de.unihd.dbs.geoparser.util.dbconnectors.SSHClientSessionFactoryTest;
import de.unihd.dbs.geoparser.util.dbconnectors.SSHConnectionDataTest;

@RunWith(Suite.class)
@SuiteClasses({ GeoparserConfigTest.class, AbstractDBConnectorTest.class, DBConnectionDataTest.class,
		DBConnectionInfoTest.class, PostgreSQLConnectorTest.class, SSHClientSessionFactoryTest.class,
		SSHConnectionDataTest.class, ModelTest.class, TypeModelTest.class, PartOfSpeechSTTSTypeTest.class,
		PartOfSpeechPTBTypeTest.class, NamedEntityTest.class, NamedEntityTypeTest.class, ToponymTest.class,
		LinkedToponymTest.class, ResolvedLocationTest.class, ResolvedToponymTest.class, DocumentTest.class,
		GazetteerTest.class, OpenNLPExtractorTest.class, StanfordNERTest.class, GazetteerLookupRecognizerTest.class,
		GazetteerExactToponymLinkerTest.class, StopWordProviderTest.class,
		// MongoDBConnectorTest.class,
		// WPArticleTest.class,
		// WPSentenceTest.class,
		// WPWikiLinkTest.class,
		// WPAnnotationTest.class
})

public class AllTests {

}
