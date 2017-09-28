package de.unihd.dbs.geoparser.gazetteer.query.predicates;

import javax.persistence.criteria.Expression;

import org.hibernate.query.criteria.internal.CriteriaBuilderImpl;
import org.hibernate.query.criteria.internal.ParameterRegistry;
import org.hibernate.query.criteria.internal.Renderable;
import org.hibernate.query.criteria.internal.compile.RenderingContext;
import org.hibernate.query.criteria.internal.expression.LiteralExpression;
import org.hibernate.query.criteria.internal.predicate.AbstractSimplePredicate;

import com.vividsolutions.jts.geom.Geometry;

/**
 * Implementation of a {@link AbstractSimplePredicate} that checks if a {@link Geometry} is within a bounding box.
 * <p>
 * <b>Note:</b> This class requires PostgreSQL and PostGIS!
 * <p>
 * Based on http://stackoverflow.com/a/25023379
 *
 * @author lrichter
 *
 */
public class WithinPredicate extends AbstractSimplePredicate {

	private static final long serialVersionUID = -3267367410000170393L;

	private final Expression<Geometry> matchExpression;
	private final Expression<Geometry> bbox;

	public WithinPredicate(final CriteriaBuilderImpl criteriaBuilder, final Expression<Geometry> matchExpression,
			final Geometry area) {
		this(criteriaBuilder, matchExpression, new LiteralExpression<>(criteriaBuilder, area));
	}

	public WithinPredicate(final CriteriaBuilderImpl criteriaBuilder, final Expression<Geometry> matchExpression,
			final Expression<Geometry> box) {
		super(criteriaBuilder);
		this.matchExpression = matchExpression;
		this.bbox = box;
	}

	public Expression<Geometry> getMatchExpression() {
		return matchExpression;
	}

	public Expression<Geometry> getArea() {
		return bbox;
	}

	@Override
	public void registerParameters(final ParameterRegistry registry) {
		// Nothing to register
	}

	@Override
	public String render(final boolean isNegated, final RenderingContext renderingContext) {

		final StringBuilder buffer = new StringBuilder();
		buffer.append(" within(").append(((Renderable) getMatchExpression()).render(renderingContext)).append(", ")
				.append(((Renderable) getArea()).render(renderingContext)).append(") =")
				.append(isNegated ? " false " : " true ");
		return buffer.toString();
	}
}