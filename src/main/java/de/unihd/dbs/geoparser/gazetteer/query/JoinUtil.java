package de.unihd.dbs.geoparser.gazetteer.query;

import javax.persistence.criteria.CriteriaQuery;
import javax.persistence.criteria.Join;
import javax.persistence.criteria.JoinType;
import javax.persistence.criteria.Root;

/**
 * Set of utility methods. Currently not clear if useful... Do not use if you don't know why you should... Fancy
 * Templating stuff that once was useful.
 * 
 * @author lrichter
 *
 */
/*
 * http://stackoverflow.com/questions/31529880/jpa-criteria-query-how-to-avoiding-duplicate-joins
 */
public class JoinUtil {

	public static <S, T> Join<T, S> getJoin(final Root<T> root, final String joinAttribute) {
		return getJoin(root, joinAttribute, JoinType.INNER);
	}

	@SuppressWarnings("unchecked")
	public static <S, T> Join<T, S> getJoin(final Root<T> root, final String joinAttribute, final JoinType joinType) {
		for (final Join<T, ?> join : root.getJoins()) {
			if (join.getAlias().equals(joinAttribute)) {
				return (Join<T, S>) join;
			}
		}

		final Join<T, ?> join = root.joinSet(joinAttribute, joinType);
		join.alias(joinAttribute);

		return (Join<T, S>) join;
	}

	public static <T> Root<?> getRoot(final CriteriaQuery<T> query, final Class<?> rootEntityClass) {
		return query.getRoots().stream().filter(root -> root.getModel().getJavaType().equals(rootEntityClass))
				.findFirst().get();
	}

}
