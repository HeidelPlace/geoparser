package de.unihd.dbs.geoparser.gazetteer.benchmark;

import java.math.BigInteger;
import java.util.List;

import de.unihd.dbs.geoparser.gazetteer.Gazetteer;
import de.unihd.dbs.geoparser.gazetteer.benchmark.Benchmark.BenchmarkMethod;
import de.unihd.dbs.geoparser.gazetteer.models.PlacePropertyType;
import de.unihd.dbs.geoparser.gazetteer.types.PropertyTypes;

/**
 * Generator for sample data used by {@link GazetteerBenchmarkRunner} and its {@link BenchmarkMethod} implementations.
 *
 * @author lrichter
 *
 */
public class BenchmarkSampleDataGenerator {

	private final List<String> placeNameSamples;
	private final List<BigInteger> placeIdSamples;
	private final List<Integer> geonamesIdSamplesAsInt;
	private final List<String> geonamesIdSamplesAsString;
	private final PlacePropertyType geonamesIdType;

	private final Gazetteer gazetteer;

	public BenchmarkSampleDataGenerator(final Gazetteer gazetteer, final int sampleCount) {
		placeNameSamples = getPlaceNameSamples(sampleCount);
		placeIdSamples = getPlaceIdSamples(sampleCount);
		geonamesIdType = getGeoNamesIdPropertyType();
		geonamesIdSamplesAsInt = getGeonamesIdSamplesAsInt(sampleCount, geonamesIdType.getId());
		geonamesIdSamplesAsString = getGeonamesIdSamplesAsString(sampleCount, geonamesIdType.getId());
		this.gazetteer = gazetteer;
	}

	private PlacePropertyType getGeoNamesIdPropertyType() {
		return (PlacePropertyType) gazetteer.getType(PropertyTypes.GEONAMES_ID.typeName);
	}

	@SuppressWarnings("unchecked")
	private List<String> getPlaceNameSamples(final int sampleCount) {
		return gazetteer.getEntityManger()
				.createNativeQuery("SELECT name FROM place_name ORDER BY id LIMIT " + sampleCount).getResultList();
	}

	@SuppressWarnings("unchecked")
	private List<BigInteger> getPlaceIdSamples(final int sampleCount) {
		return gazetteer.getEntityManger().createNativeQuery("SELECT id FROM place ORDER BY id LIMIT " + sampleCount)
				.getResultList();
	}

	@SuppressWarnings("unchecked")
	private List<Integer> getGeonamesIdSamplesAsInt(final int sampleCount, final Long typeId) {
		return gazetteer.getEntityManger()
				.createNativeQuery("SELECT CAST (value AS int) FROM place_property WHERE type_id = " + typeId
						+ " ORDER BY id LIMIT " + sampleCount)
				.getResultList();
	}

	@SuppressWarnings("unchecked")
	private List<String> getGeonamesIdSamplesAsString(final int sampleCount, final Long typeId) {
		return gazetteer.getEntityManger().createNativeQuery(
				"SELECT value FROM place_property WHERE type_id = " + typeId + " ORDER BY id LIMIT " + sampleCount)
				.getResultList();
	}

	public List<String> getPlaceNameSamples() {
		return placeNameSamples;
	}

	public List<BigInteger> getPlaceIdSamples() {
		return placeIdSamples;
	}

	public List<Integer> getGeonamesIdSamplesAsInt() {
		return geonamesIdSamplesAsInt;
	}

	public List<String> getGeonamesIdSamplesAsString() {
		return geonamesIdSamplesAsString;
	}

	public PlacePropertyType getGeonamesIdType() {
		return geonamesIdType;
	}
}
