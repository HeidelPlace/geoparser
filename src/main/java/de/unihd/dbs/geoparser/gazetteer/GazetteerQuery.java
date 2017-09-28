package de.unihd.dbs.geoparser.gazetteer;

import java.util.ArrayList;
import java.util.List;

import de.unihd.dbs.geoparser.gazetteer.query.QueryFilter;

/**
 * Structure of a query that can be run against a {@link Gazetteer}.
 * 
 * @param <T> the type of entities to be returned by the query.
 *
 * @author lrichter
 *
 */
public class GazetteerQuery<T> {

	/**
	 * Filters to be applied.
	 * <p>
	 * <b>Important:</b> The query order may influence the gazetteer lookup performance!
	 */
	public final List<QueryFilter<T>> filters;

	/**
	 * The maximum number of results. If 0, all results will be returned.
	 */
	public int maxResults;

	/**
	 * Create an instance of {@link GazetteerQuery} without a limit on the result set.
	 */
	public GazetteerQuery() {
		super();
		this.maxResults = 0;
		this.filters = new ArrayList<>();
	}

	/**
	 * Create an instance of {@link GazetteerQuery} with a limit on the result set.
	 * 
	 * @param maxResults The maximum number of results. If 0, all results will be returned.
	 */
	public GazetteerQuery(final int maxResults) {
		this();

		if (maxResults < 0) {
			throw new IllegalArgumentException("`maxResults must be a non-negative number`!");
		}

		this.maxResults = maxResults;
	}

	/**
	 * Create an instance of {@link GazetteerQuery} with the given list of filters.
	 * 
	 * @param filters the filters to apply. The filter order may be relevant for query performance!
	 */
	public GazetteerQuery(final List<QueryFilter<T>> filters) {
		this.filters = filters;
	}

	/**
	 * Create an instance of {@link GazetteerQuery} with the given parameters.
	 * 
	 * @param filters the filters to apply. The filter order may be relevant for query performance!
	 * @param maxResults The maximum number of results. If 0, all results will be returned.
	 */
	public GazetteerQuery(final List<QueryFilter<T>> filters, final int maxResults) {
		this.filters = filters;
		this.maxResults = maxResults;
	}
}
