package de.unihd.dbs.geoparser.gazetteer.query;

import java.util.Objects;
import java.util.Set;

import javax.persistence.criteria.CriteriaBuilder;
import javax.persistence.criteria.CriteriaQuery;
import javax.persistence.criteria.Predicate;
import javax.persistence.criteria.Root;
import javax.persistence.criteria.Subquery;

import de.unihd.dbs.geoparser.gazetteer.models.Place;
import de.unihd.dbs.geoparser.gazetteer.models.PlaceType;
import de.unihd.dbs.geoparser.gazetteer.models.PlaceTypeAssignment;

/**
 * Implementation of a {@link QueryFilter} that filters {@link Place}s based on their type.
 * 
 * @author lrichter
 * 
 */
public class PlaceTypePlaceFilter extends QueryFilter<Place> {

	private Set<PlaceType> placeTypes;

	/**
	 * Create a {@link PlacePropertyPlaceFilter} with the given parameters that filters places based on a list of values
	 * for a property.
	 * 
	 * @param placeTypes the {@link PlaceType}s the place must have.
	 * @param exclusive if <code>true</code>, places that are of any of the given type do not pass the filter, otherwise
	 *            only those do.
	 */
	public PlaceTypePlaceFilter(final Set<PlaceType> placeTypes, final boolean exclusive) {
		super(exclusive);
		setPlaceTypes(placeTypes);
	}

	public Set<PlaceType> getPlaceTypes() {
		return placeTypes;
	}

	public void setPlaceTypes(final Set<PlaceType> placeTypes) {
		Objects.requireNonNull(placeTypes);
		this.placeTypes = placeTypes;
	}

	@Override
	public Predicate applyFilterCriteria(final CriteriaBuilder criteriaBuilder, final CriteriaQuery<?> query,
			final Root<Place> placeRoot) {
		final Subquery<PlaceTypeAssignment> subQuery = query.subquery(PlaceTypeAssignment.class);
		final Root<PlaceTypeAssignment> typeAssignments = subQuery.from(PlaceTypeAssignment.class);

		Predicate predicate = typeAssignments.get("type").as(PlaceType.class).in(placeTypes);

		predicate = criteriaBuilder.exists(subQuery.select(typeAssignments).where(criteriaBuilder
				.and(criteriaBuilder.equal(placeRoot.get("id"), typeAssignments.get("place").get("id")), predicate)));

		if (isExclusive()) {
			return criteriaBuilder.not(predicate);
		}
		else {
			return predicate;
		}
	}

}
