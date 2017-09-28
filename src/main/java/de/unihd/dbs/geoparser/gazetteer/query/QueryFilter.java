package de.unihd.dbs.geoparser.gazetteer.query;

import javax.persistence.criteria.CriteriaBuilder;
import javax.persistence.criteria.CriteriaQuery;
import javax.persistence.criteria.Predicate;
import javax.persistence.criteria.Root;

/**
 * Base class for defining a gazetteer query filter that returns entities of type <code>T</code> wrt. to some filter
 * conditions.
 * 
 * @param <T> the root type to be used for the query
 * 
 * @author lrichter
 * 
 */
public abstract class QueryFilter<T> {

	private final boolean exclusive;

	/**
	 * Create a JPA {@link Predicate} that represents a entity filter wrt. to the implemented query filter logic using
	 * the given {@link CriteriaBuilder}.
	 * 
	 * @param criterialBuilder the {@link CriteriaBuilder} to be used for construction.
	 * @param query the {@link CriteriaQuery} that is constructed.
	 * @param rootEntity the {@link Root} of the query.
	 * @return a {@link Predicate} that represents an entity filter wrt. to the implemented query filter logic.
	 */
	public abstract Predicate applyFilterCriteria(final CriteriaBuilder criterialBuilder, final CriteriaQuery<?> query,
			final Root<T> rootEntity);

	/**
	 * Indicate if a query filter should be inclusive ("exists") or exclusive (i.e., "not exists").
	 * 
	 * @return if <code>true</code> the filter should be exclusive, otherwise inclusive
	 */
	public boolean isExclusive() {
		return exclusive;
	}

	protected QueryFilter(final boolean exclusive) {
		this.exclusive = exclusive;
	}
}
