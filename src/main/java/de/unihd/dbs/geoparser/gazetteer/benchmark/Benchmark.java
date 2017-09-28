package de.unihd.dbs.geoparser.gazetteer.benchmark;

/**
 * Storage for information about {@link BenchmarkMethod} implementations.
 *
 * @author lrichter
 *
 */
public class Benchmark {

	@FunctionalInterface
	public static interface BenchmarkMethod {
		public void benchmark();
	}

	public String name;
	public BenchmarkMethod method;

	public Benchmark(final String name, final BenchmarkMethod method) {
		super();
		this.name = name;
		this.method = method;
	}
}
