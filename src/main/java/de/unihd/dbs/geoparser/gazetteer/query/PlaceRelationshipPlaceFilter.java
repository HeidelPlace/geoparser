package de.unihd.dbs.geoparser.gazetteer.query;

import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

import javax.persistence.criteria.CriteriaBuilder;
import javax.persistence.criteria.CriteriaQuery;
import javax.persistence.criteria.Expression;
import javax.persistence.criteria.Predicate;
import javax.persistence.criteria.Root;
import javax.persistence.criteria.Subquery;

import de.unihd.dbs.geoparser.gazetteer.models.Place;
import de.unihd.dbs.geoparser.gazetteer.models.PlaceRelationship;
import de.unihd.dbs.geoparser.gazetteer.models.PlaceRelationshipType;

/**
 * Implementation of a {@link QueryFilter} that filters {@link Place}s based on their relationship
 * <p>
 * The filtering can be based on the existence of a relationship or also its value..
 * 
 * @param <T> the relationship value type
 * 
 * @author lrichter
 * 
 */
public class PlaceRelationshipPlaceFilter<T> extends QueryFilter<Place> {

	public enum PlaceRelationshipDirection {
		LEFT_TO_RIGHT, RIGHT_TO_LEFT
	}

	private final PlaceRelationshipType relationshipType;
	private final PlaceRelationshipDirection relationationshipDirection;
	private final Set<Place> otherSidePlaces;
	private final Class<? extends Number> rangeValueType;
	private final Set<T> values;
	private final T minValue;
	private final T maxValue;
	private final boolean checkOnlyExisistence;
	private final boolean rangeCheck;

	public PlaceRelationshipPlaceFilter(final PlaceRelationshipType relationshipType,
			final PlaceRelationshipDirection relationationshipDirection, final Set<Place> otherSidePlaces,
			final Set<T> values, final Class<? extends Number> valueType, final boolean exclusive) {
		super(exclusive);
		Objects.requireNonNull(relationshipType);
		Objects.requireNonNull(relationationshipDirection);
		this.relationshipType = relationshipType;
		this.relationationshipDirection = relationationshipDirection;
		this.otherSidePlaces = otherSidePlaces;
		this.values = values;
		this.rangeValueType = valueType;
		this.checkOnlyExisistence = false;
		this.maxValue = null;
		this.minValue = null;
		this.rangeCheck = false;
	}

	public PlaceRelationshipPlaceFilter(final PlaceRelationshipType relationshipType,
			final PlaceRelationshipDirection relationationshipDirection, final Set<Place> otherSidePlaces,
			final T minValue, final T maxValue, final Class<? extends Number> rangeValueType, final boolean exclusive) {
		super(exclusive);
		this.relationshipType = relationshipType;
		this.relationationshipDirection = relationationshipDirection;
		this.otherSidePlaces = otherSidePlaces;
		this.values = null;
		this.rangeValueType = rangeValueType;
		this.checkOnlyExisistence = false;
		this.maxValue = maxValue;
		this.minValue = minValue;
		this.rangeCheck = true;
	}

	public PlaceRelationshipPlaceFilter(final PlaceRelationshipType relationshipType,
			final PlaceRelationshipDirection relationationshipDirection, final Set<Place> otherSidePlaces,
			final boolean exclusive) {
		super(exclusive);
		Objects.requireNonNull(relationshipType);
		Objects.requireNonNull(relationationshipDirection);
		this.relationshipType = relationshipType;
		this.relationationshipDirection = relationationshipDirection;
		this.otherSidePlaces = otherSidePlaces;
		this.values = null;
		this.rangeValueType = null;
		this.checkOnlyExisistence = true;
		this.maxValue = null;
		this.minValue = null;
		this.rangeCheck = false;
	}

	@Override
	public Predicate applyFilterCriteria(final CriteriaBuilder criteriaBuilder, final CriteriaQuery<?> query,
			final Root<Place> placeRoot) {
		final Subquery<PlaceRelationship> subQuery = query.subquery(PlaceRelationship.class);
		final Root<PlaceRelationship> relationships = subQuery.from(PlaceRelationship.class);

		Predicate predicate = criteriaBuilder.equal(relationships.get("type"), relationshipType);
		if (!checkOnlyExisistence) {
			if (rangeCheck) {
				final Expression<? extends Number> value = relationships.get("value").as(rangeValueType);
				if (minValue != null && maxValue != null) {
					predicate = criteriaBuilder.and(predicate, criteriaBuilder.ge(value, rangeValueType.cast(minValue)),
							criteriaBuilder.le(value, rangeValueType.cast(maxValue)));
				}
				else if (minValue != null) {
					predicate = criteriaBuilder.and(predicate,
							criteriaBuilder.ge(value, rangeValueType.cast(minValue)));
				}
				else {
					predicate = criteriaBuilder.and(predicate,
							criteriaBuilder.le(value, rangeValueType.cast(maxValue)));
				}
			}
			else {
				final Predicate valuePredicate = relationships.get("value").as(rangeValueType).in(values);
				predicate = criteriaBuilder.and(predicate, valuePredicate);
			}
		}

		final String placeField = relationationshipDirection.equals(PlaceRelationshipDirection.LEFT_TO_RIGHT)
				? "leftPlace" : "rightPlace";
		final String otherPlaceField = relationationshipDirection.equals(PlaceRelationshipDirection.LEFT_TO_RIGHT)
				? "rightPlace" : "leftPlace";

		if (otherSidePlaces != null) {
			predicate = criteriaBuilder.and(
					relationships.get(otherPlaceField).get("id")
							.in(otherSidePlaces.stream().map(place -> place.getId()).collect(Collectors.toList())),
					predicate);
		}

		predicate = criteriaBuilder.exists(subQuery.select(relationships).where(criteriaBuilder
				.and(criteriaBuilder.equal(placeRoot.get("id"), relationships.get(placeField).get("id")), predicate)));

		if (isExclusive()) {
			return criteriaBuilder.not(predicate);
		}
		else {
			return predicate;
		}
	}

}
