package de.unihd.dbs.geoparser.gazetteer.benchmark;

import java.io.File;
import java.math.BigInteger;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.EnumSet;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import de.unihd.dbs.geoparser.core.GeoparserConfig;
import de.unihd.dbs.geoparser.gazetteer.Gazetteer;
import de.unihd.dbs.geoparser.gazetteer.GazetteerQuery;
import de.unihd.dbs.geoparser.gazetteer.benchmark.Benchmark.BenchmarkMethod;
import de.unihd.dbs.geoparser.gazetteer.models.Place;
import de.unihd.dbs.geoparser.gazetteer.models.PlaceName.NameFlag;
import de.unihd.dbs.geoparser.gazetteer.query.PlaceIdPlaceFilter;
import de.unihd.dbs.geoparser.gazetteer.query.PlaceNamePlaceFilter;
import de.unihd.dbs.geoparser.gazetteer.query.PlaceNamePlaceFilter.MatchMode;
import de.unihd.dbs.geoparser.gazetteer.query.PlacePropertyPlaceFilter;
import de.unihd.dbs.geoparser.gazetteer.query.QueryFilter;
import de.unihd.dbs.geoparser.gazetteer.util.GazetteerPersistenceManager;
import de.unihd.dbs.geoparser.util.dbconnectors.PostgreSQLConnector;

import com.bericotech.clavin.gazetteer.query.LuceneGazetteer;
import com.bericotech.clavin.gazetteer.query.QueryBuilder;
import com.bericotech.clavin.resolver.ResolvedLocation;

/**
 * Some simple Gazetteer Benchmarking methods to evaluate the runtime performance of the gazetteer service and to
 * compare it with CLAVIN.
 *
 * @author lrichter
 *
 */
// TODO: test application
// TODO: read through carefully what code does
// TODO: add some comments
public class GazetteerBenchmarkRunner {

	private static final Logger logger = LoggerFactory.getLogger(GazetteerBenchmarkRunner.class);

	/*
	 * Logging configuration
	 */
	public boolean PRINT_MEM_USAGE = false;

	/*
	 * Benchmark configuration
	 */
	public String PATH_TO_LUCENE_INDEX = "D:\\Programming\\CLAVIN 2.1\\IndexDirectory";
	public int SAMPLE_COUNT = 10000;

	private GazetteerPersistenceManager gpm;
	private Gazetteer gazetteer;
	private PostgreSQLConnector pgConnection;
	private com.bericotech.clavin.gazetteer.query.Gazetteer clavinGazetteer;

	private BenchmarkSampleDataGenerator sampleData;
	private List<Benchmark> benchmarks;

	public static void main(final String[] args) {
		logger.info(
				"You should ensure that the following indexes are set up (where X is the id of the GeoNamesId property type):\n"
						+ "\tCREATE INDEX place_property_geonames_id_int_idx ON place_property (cast(value as int4)) WHERE type_id = X;\n"
						+ "\tCREATE INDEX place_property_geonames_id_idx ON place_property (value) WHERE type_id = X");
		final GazetteerBenchmarkRunner benchmarker = new GazetteerBenchmarkRunner();
		benchmarker.run();
	}

	public void run() {
		try {
			connectToGazetteers();
			registerBenchmarks();
			sampleData = new BenchmarkSampleDataGenerator(gazetteer, SAMPLE_COUNT);
			runBenchmarks();
		}
		catch (final Exception e) {
			logger.error("Failed to initialize gazetteer resources!", e);
			throw new RuntimeException("Failed to initialize gazetteer resources!", e);
		}
		finally {
			try {
				disconnectFromGazetteers();
			}
			catch (final Exception e) {
				logger.error("Failed to release gazetter resources!", e);
				throw new RuntimeException("Failed to release gazetter resources!", e);
			}
		}
	}

	private void connectToGazetteers() throws Exception {
		final GeoparserConfig config = new GeoparserConfig();
		logger.debug("Connecting with Gazetteer via Hibernate...");
		gpm = new GazetteerPersistenceManager(config);
		gazetteer = new Gazetteer(gpm.getEntityManager());
		logger.debug("Connecting with CLAVIN's Gazetteer Lucene Index...");
		clavinGazetteer = new LuceneGazetteer(new File(PATH_TO_LUCENE_INDEX));
		logger.debug("Connecting with Gazetteer via PostgreSQL database");
		pgConnection = new PostgreSQLConnector(config.getDBConnectionInfoByLabel(
				config.getConfigStringByLabel(GazetteerPersistenceManager.PERSISTENCE_UNIT_DB_CONNECTION_INFO_LABEL)));
		pgConnection.connect();
	}

	private void disconnectFromGazetteers() throws Exception {
		try {
			logger.debug("Closing gazetteer connection...");
			if (gazetteer != null) {
				gazetteer.close();
			}
		}
		finally {
			logger.debug("Closing gazetteer persistence manager...");
			if (gpm != null) {
				gpm.close();
			}
			logger.debug("Closing plain gazetteer connection...");
			if (pgConnection != null) {
				pgConnection.disconnect();
			}
		}
	}

	private void registerBenchmarks() {
		benchmarks = new ArrayList<>();

		benchmarks.add(new Benchmark("ClavinIdFilterBenchmark", clavinIdFilterBenchmark));
		benchmarks.add(new Benchmark("ClavinExactNameFilterBenchmark", clavinExactNameFilterBenchmark));

		benchmarks.add(new Benchmark("PlaceByIdBenchmark", placeByIdBenchmark));
		benchmarks.add(new Benchmark("NoHibernatePlaceByIdBenchmark", noHibernatePlaceByIdBenchmark));
		benchmarks.add(new Benchmark("PlaceIdFilterBenchmark", placeIdFilterBenchmark));
		benchmarks.add(new Benchmark("NoHibernatePlaceIdFilterBenchmark", noHibernatePlaceIdFilterBenchmark));

		benchmarks.add(new Benchmark("ExactPlaceNameFilterBenchmark", exactPlaceNameFilterBenchmark));
		benchmarks.add(new Benchmark("NoHibernateExactNameFilterBenchmark", noHibernateExactNameFilterBenchmark));

		benchmarks.add(new Benchmark("PropertyFilterIntegerValueBenchmark", propertyFilterIntegerValueBenchmark));
		benchmarks.add(new Benchmark("NoHibernatePropertyFilterIntegerValueBenchmark",
				noHibernatePropertyFilterIntegerValueBenchmark));

		benchmarks.add(new Benchmark("PropertyFilterStringValueBenchmark", propertyFilterStringValueBenchmark));
		benchmarks.add(new Benchmark("NoHibernatePropertyFilterStringValueBenchmark",
				noHibernatePropertyFilterStringValueBenchmark));
	}

	private void runBenchmarks() {
		for (final Benchmark benchmark : benchmarks) {
			logger.info("Running " + benchmark.name + "...");

			printMemoryConsumption();

			final long searchStartTime = System.nanoTime();

			benchmark.method.benchmark();

			final long searchEndTime = System.nanoTime();
			final double elapsedTime = (double) (searchEndTime - searchStartTime) / 1_000_000;
			final double queriesPerSecond = SAMPLE_COUNT / (elapsedTime / 1_000);
			logger.info(String.format(Locale.ENGLISH, "Performance of %s: %.2f queries/sec", benchmark.name,
					queriesPerSecond));

			printMemoryConsumption();

			// clear entity manager so we don't exploit caching effects!
			gazetteer.getEntityManger().clear();
		}
	}

	private void printMemoryConsumption() {
		if (PRINT_MEM_USAGE) {
			final Runtime runtime = Runtime.getRuntime();
			final long allocatedMemory = runtime.totalMemory();
			final long freeMemory = runtime.freeMemory();
			logger.info(String.format("Memory consumption: free memory: %d, allocated memory: %d", freeMemory / 1024,
					allocatedMemory / 1024));
		}
	}

	/*
	 * HIBERNATE BENCHMARK IMPLEMENTATIONS
	 */

	private final BenchmarkMethod exactPlaceNameFilterBenchmark = () -> {
		int totalFoundPlaces = 0;

		final QueryFilter<Place> filter = new PlaceNamePlaceFilter("", null, EnumSet.noneOf(NameFlag.class), false,
				MatchMode.EXACT, 0.0, false);
		final GazetteerQuery<Place> query = new GazetteerQuery<>(Arrays.asList(filter));

		for (final String placeNameSample : sampleData.getPlaceNameSamples()) {
			((PlaceNamePlaceFilter) filter).setName(placeNameSample);
			final List<Long> placeIds = gazetteer.getPlaceIds(query);
			totalFoundPlaces += placeIds.size();
		}

		logger.debug(String.format("\tFound %d places", totalFoundPlaces));
	};

	private final BenchmarkMethod placeByIdBenchmark = () -> {
		for (final BigInteger placeIdSample : sampleData.getPlaceIdSamples()) {
			gazetteer.getPlace(placeIdSample.longValue());
		}
	};

	private final BenchmarkMethod placeIdFilterBenchmark = () -> {
		final HashSet<Long> ids = new HashSet<>();
		final QueryFilter<Place> filter = new PlaceIdPlaceFilter(ids, false);
		final GazetteerQuery<Place> query = new GazetteerQuery<>(Arrays.asList(filter));

		for (final BigInteger placeIdSample : sampleData.getPlaceIdSamples()) {
			ids.clear();
			ids.add(placeIdSample.longValue());
			gazetteer.getPlaceIds(query);
		}
	};

	private final BenchmarkMethod propertyFilterIntegerValueBenchmark = () -> {
		final HashSet<Integer> values = new HashSet<>();
		final QueryFilter<Place> filter = new PlacePropertyPlaceFilter<>(sampleData.getGeonamesIdType(), values,
				Integer.class, false);
		final GazetteerQuery<Place> query = new GazetteerQuery<>(Arrays.asList(filter));

		for (final Integer geonamesIdSample : sampleData.getGeonamesIdSamplesAsInt()) {
			values.clear();
			values.add(geonamesIdSample);
			gazetteer.getPlaceIds(query);
		}

	};

	private final BenchmarkMethod propertyFilterStringValueBenchmark = () -> {
		final HashSet<String> values = new HashSet<>();
		final QueryFilter<Place> filter = new PlacePropertyPlaceFilter<>(sampleData.getGeonamesIdType(), values,
				String.class, false);
		final GazetteerQuery<Place> query = new GazetteerQuery<>(Arrays.asList(filter));

		for (final String geonamesIdSample : sampleData.getGeonamesIdSamplesAsString()) {
			values.clear();
			values.add(geonamesIdSample);
			gazetteer.getPlaceIds(query);
		}
	};

	/*
	 * DIRECT DB-QUERY (NON-HIBERNATE) BENCHMARKING IMPLEMENTATIONS
	 */

	@FunctionalInterface
	private static interface SampleToSQLStatementParamsMapper<T> {
		public void map(T sample, PreparedStatement statement) throws SQLException;
	}

	private <T> void runSQLStatementBasedBenchmark(final String benchmarkName, final String query,
			final List<T> samples, final SampleToSQLStatementParamsMapper<T> mapper) {
		int totalFoundPlaces = 0;

		try (final PreparedStatement statement = pgConnection.getConnection().prepareStatement(query)) {
			final List<Integer> places = new ArrayList<>();

			for (final T sample : samples) {
				mapper.map(sample, statement);
				try (final ResultSet rs = statement.executeQuery()) {
					while (rs.next()) {
						places.add(rs.getInt(1));
					}
				}

				totalFoundPlaces += places.size();
			}
		}
		catch (final SQLException e) {
			throw new RuntimeException("Failed to run `" + benchmarkName + "`", e);
		}

		logger.debug(String.format("\tFound %d places", totalFoundPlaces));
	}

	private final BenchmarkMethod noHibernatePlaceByIdBenchmark = () -> {
		runSQLStatementBasedBenchmark("noHibernatePlaceByIdBenchmark", "SELECT p.id FROM place AS p WHERE p.id = ?",
				sampleData.getPlaceIdSamples(), (sample, mapper) -> mapper.setInt(1, sample.intValue()));
	};

	private final BenchmarkMethod noHibernatePlaceIdFilterBenchmark = () -> {
		runSQLStatementBasedBenchmark("noHibernatePlaceIdFilterBenchmark",
				"SELECT p.id FROM place AS p WHERE EXISTS "
						+ "(SELECT p2.id FROM place p2 WHERE p.id = p2.id AND p2.id = ?)",
				sampleData.getPlaceIdSamples(), (sample, mapper) -> mapper.setInt(1, sample.intValue()));
	};

	private final BenchmarkMethod noHibernateExactNameFilterBenchmark = () -> {
		runSQLStatementBasedBenchmark("noHibernateExactNameFilterBenchmark",
				"SELECT p.id FROM place AS p WHERE EXISTS "
						+ "(SELECT n.place_id FROM place_name n WHERE p.id = n.place_id AND name LIKE ?)",
				sampleData.getPlaceNameSamples(), (sample, mapper) -> mapper.setString(1, sample));
	};

	private final BenchmarkMethod noHibernatePropertyFilterIntegerValueBenchmark = () -> {
		runSQLStatementBasedBenchmark("noHibernatePropertyFilterIntegerValueBenchmark",
				"SELECT p.id FROM place AS p WHERE EXISTS "
						+ "(SELECT pp.place_id FROM place_property pp WHERE p.id = pp.place_id AND type_id = "
						+ sampleData.getGeonamesIdType().getId() + " AND CAST(value AS int4) IN (?))",
				sampleData.getGeonamesIdSamplesAsInt(), (sample, mapper) -> mapper.setInt(1, sample));
	};

	private final BenchmarkMethod noHibernatePropertyFilterStringValueBenchmark = () -> {
		runSQLStatementBasedBenchmark("noHibernatePropertyFilterStringValueBenchmark",
				"SELECT p.id FROM place AS p WHERE EXISTS "
						+ "(SELECT pp.place_id FROM place_property pp WHERE p.id = pp.place_id AND type_id = "
						+ sampleData.getGeonamesIdType().getId() + " AND value IN (?))",
				sampleData.getGeonamesIdSamplesAsString(), (sample, mapper) -> mapper.setString(1, sample));
	};

	/*
	 * CLAVIN GAZETTEER BENCHMARKING
	 */

	private final BenchmarkMethod clavinIdFilterBenchmark = () -> {
		for (final Integer geonamesIdSample : sampleData.getGeonamesIdSamplesAsInt()) {
			try {
				clavinGazetteer.getGeoName(geonamesIdSample);
			}
			catch (final Exception e) {
				throw new RuntimeException("Failed to query CLAVIN gazetteer", e);
			}
		}
	};

	private final BenchmarkMethod clavinExactNameFilterBenchmark = () -> {
		final QueryBuilder queryBuilder = new QueryBuilder();
		int totalFoundPlaces = 0;

		for (final String placeNameSample : sampleData.getPlaceNameSamples()) {
			queryBuilder.location(placeNameSample);
			try {
				final List<ResolvedLocation> places = clavinGazetteer.getClosestLocations(queryBuilder.build());
				totalFoundPlaces += places.size();
			}
			catch (final Exception e) {
				throw new RuntimeException("Failed to query CLAVIN gazetteer", e);
			}
		}

		logger.debug(String.format("\tFound %d places", totalFoundPlaces));
	};

}
