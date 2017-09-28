package de.unihd.dbs.geoparser.gazetteer.query;

import java.util.Objects;

import javax.persistence.criteria.CriteriaBuilder;
import javax.persistence.criteria.CriteriaQuery;
import javax.persistence.criteria.Predicate;
import javax.persistence.criteria.Root;
import javax.persistence.criteria.Subquery;

import org.hibernate.query.criteria.internal.CriteriaBuilderImpl;
import de.unihd.dbs.geoparser.gazetteer.models.Footprint;
import de.unihd.dbs.geoparser.gazetteer.models.Place;
import de.unihd.dbs.geoparser.gazetteer.query.predicates.WithinPredicate;

import com.vividsolutions.jts.geom.Geometry;

/**
 * Implementation of a {@link QueryFilter} that filters {@link Place}s whose footprints are within a bounding box.
 * 
 * @author lrichter
 *
 */
public class BoundingBoxPlaceFilter extends QueryFilter<Place> {

	private Geometry boundingBox;

	/**
	 * Create a {@link BoundingBoxPlaceFilter} with the given parameters.
	 * 
	 * @param boundingBox the bounding box to check for.
	 * @param exclusive if <code>true</code> all places outside `boundingBox` pass the filter. Otherwise, only places
	 *            within `boundingBox` pass the filter.
	 */
	public BoundingBoxPlaceFilter(final Geometry boundingBox, final boolean exclusive) {
		super(exclusive);
		setBoundingBox(boundingBox);
	}

	public Geometry getBoundingBox() {
		return boundingBox;
	}

	public void setBoundingBox(final Geometry boundingBox) {
		Objects.requireNonNull(boundingBox);
		this.boundingBox = boundingBox;
	}

	@Override
	public Predicate applyFilterCriteria(final CriteriaBuilder criteriaBuilder, final CriteriaQuery<?> query,
			final Root<Place> placeRoot) {
		final Subquery<Footprint> subQuery = query.subquery(Footprint.class);
		final Root<Footprint> footprints = subQuery.from(Footprint.class);

		Predicate predicate;

		predicate = new WithinPredicate((CriteriaBuilderImpl) criteriaBuilder, footprints.get("geometry"), boundingBox);

		predicate = criteriaBuilder.exists(subQuery.select(footprints).where(criteriaBuilder
				.and(criteriaBuilder.equal(placeRoot.get("id"), footprints.get("place").get("id")), predicate)));

		if (isExclusive()) {
			return criteriaBuilder.not(predicate);
		}
		else {
			return predicate;
		}
	}

}
