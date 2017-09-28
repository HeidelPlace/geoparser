package de.unihd.dbs.geoparser.gazetteer.query.predicates;

import javax.persistence.criteria.Expression;

import org.hibernate.query.criteria.internal.CriteriaBuilderImpl;
import org.hibernate.query.criteria.internal.ParameterRegistry;
import org.hibernate.query.criteria.internal.Renderable;
import org.hibernate.query.criteria.internal.compile.RenderingContext;
import org.hibernate.query.criteria.internal.predicate.AbstractSimplePredicate;

/**
 * Implementation of a {@link AbstractSimplePredicate} that checks if the Levensthein distance between two strings is
 * bellow a defined threshold. The distance counts the number of required insertions, deletions and substitutions to
 * match two strings.
 * <p>
 * <b>Note:</b> This class requires PostgreSQL and the fuzzystrmatch package
 * (https://www.postgresql.org/docs/current/static/fuzzystrmatch.html)!
 *
 * @author lrichter
 *
 */
public class LevenshteinDistancePredicate extends AbstractSimplePredicate {

	private static final long serialVersionUID = -6593053690949863382L;
	private final Expression<String> matchExpression;
	private final String searchString;
	private final int maxDistance;

	public LevenshteinDistancePredicate(final CriteriaBuilderImpl criteriaBuilder, final String name,
			final Expression<String> matchExpression, final int maxDistance) {
		super(criteriaBuilder);
		if (maxDistance < 0) {
			throw new IllegalArgumentException("The maximum distance must be a non-negative number!");
		}
		this.maxDistance = maxDistance;
		this.matchExpression = matchExpression;
		this.searchString = name;
	}

	public Expression<String> getMatchExpression() {
		return matchExpression;
	}

	public String getSearchString() {
		return searchString;
	}

	public int getMaxDistance() {
		return maxDistance;
	}

	@Override
	public void registerParameters(final ParameterRegistry registry) {
		// Nothing to register
	}

	@Override
	public String render(final boolean isNegated, final RenderingContext renderingContext) {

		final StringBuilder buffer = new StringBuilder();
		buffer.append(" levenshtein('").append(getSearchString()).append("', ")
				.append(((Renderable) getMatchExpression()).render(renderingContext)).append(") ")
				.append(isNegated ? " > " : " <= ").append(getMaxDistance());
		return buffer.toString();
	}
}
