package de.unihd.dbs.geoparser.gazetteer.query;

import java.util.Objects;
import java.util.Set;

import javax.persistence.criteria.CriteriaBuilder;
import javax.persistence.criteria.CriteriaQuery;
import javax.persistence.criteria.Expression;
import javax.persistence.criteria.Predicate;
import javax.persistence.criteria.Root;
import javax.persistence.criteria.Subquery;

import de.unihd.dbs.geoparser.gazetteer.models.Place;
import de.unihd.dbs.geoparser.gazetteer.models.PlaceProperty;
import de.unihd.dbs.geoparser.gazetteer.models.PlacePropertyType;

/**
 * Implementation of a {@link QueryFilter} that filters {@link Place}s based on a property.
 * <p>
 * The filtering can be based on the existence of a property or also its value.
 * 
 * @param <T> the property value type
 *
 * @author lrichter
 *
 */
public class PlacePropertyPlaceFilter<T> extends QueryFilter<Place> {

	private final Class<T> valueType;
	private final Class<? extends Number> rangeValueType;
	private final PlacePropertyType propertyType;
	private final Set<T> values;
	private final T minValue;
	private final T maxValue;
	private final boolean checkOnlyExisistence;
	private final boolean rangeCheck;

	/**
	 * Create a {@link PlacePropertyPlaceFilter} with the given parameters that filters places based on a list of values
	 * for a property.
	 * 
	 * @param propertyType the {@link PlacePropertyType} the filter property must have
	 * @param values the values the property must have
	 * @param valueType class of the property type; required since type of T is lost at runtime
	 * @param exclusive if <code>true</code>, places that have a property of type `propertyType` with its value being in
	 *            `values` do not pass the filter, otherwise only those do
	 */
	public PlacePropertyPlaceFilter(final PlacePropertyType propertyType, final Set<T> values, final Class<T> valueType,
			final boolean exclusive) {
		super(exclusive);
		Objects.requireNonNull(propertyType);
		Objects.requireNonNull(values);
		Objects.requireNonNull(valueType);
		this.propertyType = propertyType;
		this.valueType = valueType;
		this.values = values;
		this.checkOnlyExisistence = false;
		this.maxValue = null;
		this.minValue = null;
		this.rangeValueType = null;
		this.rangeCheck = false;
	}

	/**
	 * Create a {@link PlacePropertyPlaceFilter} with the given parameters that filters places based on a range of
	 * values for a property.
	 * <p>
	 * <b>Note:</b> the generic type <code>T</code> must extend {@link Number}!
	 *
	 * @param propertyType the {@link PlacePropertyType} the filter property must have
	 * @param minValue the minimum value the property must have. May be <code>null</code>, if `maxValue` is not
	 *            <code>null</code>.
	 * 
	 * @param maxValue the maximum value the property must have. May be <code>null</code>, if `minValue` is not
	 *            <code>null</code>.
	 * @param rangeValueType class of the property type; required since type of T is lost at runtime
	 * @param exclusive if <code>true</code>, places that have a property of type `propertyType` with its value being in
	 *            range `minValue`-`maxValue` are filtered, otherwise places that do not have a property of type
	 *            `propertyType` with its value being in the range are filtered
	 */
	public PlacePropertyPlaceFilter(final PlacePropertyType propertyType, final T minValue, final T maxValue,
			final Class<? extends Number> rangeValueType, final boolean exclusive) {
		super(exclusive);
		Objects.requireNonNull(propertyType);
		Objects.requireNonNull(rangeValueType);
		this.propertyType = propertyType;
		this.valueType = null;
		this.values = null;
		this.checkOnlyExisistence = false;
		if (minValue == null && maxValue == null) {
			throw new IllegalArgumentException("At least one of `minValue` and `maxValue` must not be null!");
		}
		this.maxValue = maxValue;
		this.minValue = minValue;
		this.rangeValueType = rangeValueType;
		this.rangeCheck = true;
	}

	/**
	 * Create a {@link PlacePropertyPlaceFilter} with the given parameters that filters places based on the existence of
	 * a property.
	 * 
	 * @param propertyType the {@link PlacePropertyType} the filter property must have
	 * @param exclusive if <code>true</code>, places that have a property of type `propertyType` are filtered, otherwise
	 *            places that do not have a property of type `propertyType` are filtered
	 */
	public PlacePropertyPlaceFilter(final PlacePropertyType propertyType, final boolean exclusive) {
		super(exclusive);
		Objects.requireNonNull(propertyType);
		this.propertyType = propertyType;
		this.valueType = null;
		this.values = null;
		this.checkOnlyExisistence = true;
		this.maxValue = null;
		this.minValue = null;
		this.rangeValueType = null;
		this.rangeCheck = false;
	}

	@Override
	public Predicate applyFilterCriteria(final CriteriaBuilder criteriaBuilder, final CriteriaQuery<?> query,
			final Root<Place> placeRoot) {
		final Subquery<PlaceProperty> subQuery = query.subquery(PlaceProperty.class);
		final Root<PlaceProperty> properties = subQuery.from(PlaceProperty.class);

		Predicate predicate = criteriaBuilder.equal(properties.get("type"), propertyType);
		if (!checkOnlyExisistence) {
			if (rangeCheck) {
				final Expression<? extends Number> value = properties.get("value").as(rangeValueType);
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
				final Predicate valuePredicate = properties.get("value").as(valueType).in(values);
				predicate = criteriaBuilder.and(predicate, valuePredicate);
			}

		}

		predicate = criteriaBuilder.exists(subQuery.select(properties).where(criteriaBuilder
				.and(criteriaBuilder.equal(placeRoot.get("id"), properties.get("place").get("id")), predicate)));

		if (isExclusive()) {
			return criteriaBuilder.not(predicate);
		}
		else {
			return predicate;
		}
	}

}
