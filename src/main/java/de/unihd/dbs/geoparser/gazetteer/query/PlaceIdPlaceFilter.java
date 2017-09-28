package de.unihd.dbs.geoparser.gazetteer.query;

import java.util.Objects;
import java.util.Set;

import javax.persistence.criteria.CriteriaBuilder;
import javax.persistence.criteria.CriteriaQuery;
import javax.persistence.criteria.Predicate;
import javax.persistence.criteria.Root;

import de.unihd.dbs.geoparser.gazetteer.models.Place;

/**
 * Implementation of a {@link QueryFilter} that filters {@link Place}s based on their Id.
 * 
 * @author lrichter
 *
 */
public class PlaceIdPlaceFilter extends QueryFilter<Place> {

	private Set<Long> placeIds;

	/**
	 * Create a {@link PlaceIdPlaceFilter} with the given parameters.
	 *
	 * @param placeIds the place Ids to filter.
	 * @param exclusive if <code>true</code>, all places with Ids other than `placeIds` pass the filter. Otherwise, only
	 *            places whose Id is listed in `placeId` pass the filter.
	 */
	public PlaceIdPlaceFilter(final Set<Long> placeIds, final boolean exclusive) {
		super(exclusive);
		setPlaceIds(placeIds);
	}

	public Set<Long> getPlaceIds() {
		return placeIds;
	}

	public void setPlaceIds(final Set<Long> placeIds) {
		Objects.requireNonNull(placeIds);
		this.placeIds = placeIds;
	}

	@Override
	public Predicate applyFilterCriteria(final CriteriaBuilder criteriaBuilder, final CriteriaQuery<?> query,
			final Root<Place> placeRoot) {
		return placeRoot.get("id").as(Long.class).in(placeIds);
	}

}
