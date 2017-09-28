package de.unihd.dbs.geoparser.gazetteer;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;

import javax.persistence.EntityManager;
import javax.persistence.TypedQuery;
import javax.persistence.criteria.CriteriaBuilder;
import javax.persistence.criteria.CriteriaQuery;
import javax.persistence.criteria.Predicate;
import javax.persistence.criteria.Root;
import javax.persistence.criteria.Selection;

import org.hibernate.Session;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import de.unihd.dbs.geoparser.gazetteer.models.AbstractEntity;
import de.unihd.dbs.geoparser.gazetteer.models.Place;
import de.unihd.dbs.geoparser.gazetteer.models.Type;
import de.unihd.dbs.geoparser.gazetteer.query.QueryFilter;
import de.unihd.dbs.geoparser.gazetteer.util.GazetteerPersistenceManager;

/**
 * Implementation of a gazetteer service that provides functionality for accessing a gazetteer database via JPA.
 * <p>
 * The gazetteer connects to a gazetteer database via JPA using a {@link GazetteerPersistenceManager}. The gazetteer
 * database schema is expected to be built according to the model defined by the classes in
 * {@link de.unihd.dbs.geoparser.gazetteer.models}. The gazetteer service provides various methods to perform gazetteer
 * lookups.
 * <p>
 * Currently, the gazetteer does not support multithreading, since {@link EntityManager} is not thread-safe. You might
 * want to create multiple {@link Gazetteer} instances and run queries on them in parallel, but this was not tested
 * yet...
 * 
 * @author lrichter
 *
 */
public class Gazetteer implements AutoCloseable {

	private static final Logger logger = LoggerFactory.getLogger(Gazetteer.class);

	private final EntityManager entityManager;

	/**
	 * Create a {@link Gazetteer} instance connected to the gazetteer database via the given JPA entity manager.
	 * 
	 * @param entityManager the JPA entity manager. Must not be <code>null</code>.
	 */
	public Gazetteer(final EntityManager entityManager) {
		Objects.requireNonNull(entityManager);
		logger.debug("Starting gazetteer");
		this.entityManager = entityManager;
	}

	/**
	 * Stop the gazetteer service and release allocated resources.
	 */
	public void stop() {
		logger.debug("Tearing down gazetteer");
		if (entityManager != null && entityManager.isOpen()) {
			entityManager.close();
		}
	}

	@Override
	public void close() throws Exception {
		stop();
	}

	/**
	 * Cancel currently running queries. Use with care!
	 */
	public void cancelQuery() {
		entityManager.unwrap(Session.class).cancelQuery();
	}

	/**
	 * Get the {@link EntityManager} instance that is used to manage the gazetteer connection.
	 * <p>
	 * <b>Note:</b> This provides low-level access to the gazetteer infrastructure. Be aware that this may reduce code
	 * readability and maintainability!
	 * 
	 * @return the {@link EntityManager} instance used by the gazetteer.
	 */
	public EntityManager getEntityManger() {
		return entityManager;
	}

	/**
	 * Retrieve the {@link Place} instance with the given Id.
	 * 
	 * @param id the Id of the requested {@link Place} instance.
	 * @return the requested {@link Place} instance.
	 */
	public Place getPlace(final Long id) {
		return entityManager.createQuery("FROM Place WHERE id = :id", Place.class).setParameter("id", id)
				.getSingleResult();
	}

	/**
	 * Retrieve the {@link AbstractEntity} derived entity with the given Id.
	 * 
	 * @param id the Id of the requested entity.
	 * @return the requested entity.
	 */
	public AbstractEntity getEntity(final Long id) {
		return entityManager.createQuery("FROM AbstractEntity WHERE id = :id", AbstractEntity.class)
				.setParameter("id", id).getSingleResult();
	}

	/**
	 * Retrieve the {@link Type} instance with the given Id.
	 * 
	 * @param id the Id of the requested {@link Type} instance.
	 * @return the requested {@link Type} instance.
	 */
	public Type getType(final Long id) {
		return entityManager.createQuery("FROM Type WHERE id = :id", Type.class).setParameter("id", id)
				.getSingleResult();
	}

	/**
	 * Retrieve the {@link Type} instance with the given name.
	 * 
	 * @param name the name of the requested {@link Type} instance.
	 * @return the requested {@link Type} instance.
	 */
	public Type getType(final String name) {
		return entityManager.createQuery("FROM Type WHERE name = :name", Type.class).setParameter("name", name)
				.getSingleResult();
	}

	/**
	 * Retrieve all {@link Type} instances.
	 * 
	 * @return all {@link Type} instances.
	 */
	public Set<Type> getAllTypes() {
		return new HashSet<>(entityManager.createQuery("FROM Type", Type.class).getResultList());
	}

	/**
	 * Retrieve all {@link Type} instances of the given class.
	 * 
	 * @param typeClass the class of types to load.
	 * @return all {@link Type} instances.
	 */
	public Set<Type> getAllTypes(final Class<? extends Type> typeClass) {
		return new HashSet<>(entityManager.createQuery("FROM Type t WHERE TYPE(t) = :class", Type.class)
				.setParameter("class", typeClass).getResultList());
	}

	/**
	 * Retrieve places from the gazetteer using a {@link GazetteerQuery}.
	 *
	 * @param queryData configuration parameters for the query. Must not be <code>null</code>.
	 * @return set of matched {@link Place} instances.
	 */
	public List<Place> getPlaces(final GazetteerQuery<Place> queryData) {
		Objects.requireNonNull(queryData);
		final TypedQuery<Place> query = buildPlaceQuery(queryData);

		if (queryData.maxResults > 0) {
			query.setMaxResults(queryData.maxResults);
		}

		return query.getResultList();
	}

	/**
	 * Retrieve places Ids from the gazetteer using a {@link GazetteerQuery}.
	 *
	 * @param queryData configuration parameters for the query. Must not be <code>null</code>.
	 * @return set of Ids for matched {@link Place} instances.
	 */
	public List<Long> getPlaceIds(final GazetteerQuery<Place> queryData) {
		Objects.requireNonNull(queryData);
		final TypedQuery<Long> query = buildPlaceIdQuery(queryData);

		if (queryData.maxResults > 0) {
			query.setMaxResults(queryData.maxResults);
		}

		return query.getResultList();
	}

	/**
	 * Count how many places would be retrieved from the gazetteer using a {@link GazetteerQuery}.
	 * 
	 * @param queryData configuration parameters for the query. Must not be <code>null</code>.
	 * @return number of matched {@link Place} instances.
	 */
	public Long countPlaces(final GazetteerQuery<Place> queryData) {
		Objects.requireNonNull(queryData);

		final TypedQuery<Long> query = buildCountQuery(queryData);

		if (queryData.maxResults > 0) {
			query.setMaxResults(queryData.maxResults);
		}

		return query.getSingleResult();
	}

	@FunctionalInterface
	public interface PlaceFeatureSelectionBuilder {
		Selection<? extends Object[]> buildSelectClause(final CriteriaBuilder criteriaBuilder,
				final Root<Place> queryRoot);
	}

	@FunctionalInterface
	public interface AdditionalPredicateBuilder {
		List<Predicate> buildPredicates(final CriteriaBuilder criteriaBuilder, final Root<Place> queryRoot);
	}

	/**
	 * Retrieve selected place features from the gazetteer using a {@link GazetteerQuery}.
	 * <p>
	 * This method allows to create flexible result sets for speeding up queries where only certain information are
	 * required. The usual query parameters can be still used, but additional joins, predicates and selects can be added
	 * to the CriteriaQuery. In contrast to simply using SQL-queries, this method allows to incorporate existing
	 * filters, etc.
	 * <p>
	 * <b>Warning:</b> This method allows rather low-level query modifications. Be aware that this is at the cost of
	 * code readability and maintainability. Only use if performance can be significantly improved!
	 * 
	 * @param queryData configuration parameters for the query. Must not be <code>null</code>.
	 * @param selectBuilder a method that creates the select-clause for the query.
	 * @param predicateBuilder a method that creates additional predicates for the where-clause. Useful to add joins!
	 * @return a list of features per matched place.
	 */
	public List<Object[]> getSelectedPlaceFeatures(final GazetteerQuery<Place> queryData,
			final PlaceFeatureSelectionBuilder selectBuilder, final AdditionalPredicateBuilder predicateBuilder) {
		Objects.requireNonNull(queryData);
		final TypedQuery<Object[]> query = buildSelectedPlaceFeaturesQuery(queryData, selectBuilder, predicateBuilder);

		if (queryData.maxResults > 0) {
			query.setMaxResults(queryData.maxResults);
		}

		return query.getResultList();
	}

	private TypedQuery<Place> buildPlaceQuery(final GazetteerQuery<Place> queryData) {
		final CriteriaBuilder criteriaBuilder = entityManager.getCriteriaBuilder();
		final CriteriaQuery<Place> query = criteriaBuilder.createQuery(Place.class);
		final Root<Place> queryRoot = query.from(Place.class);

		buildWhereClause(query, queryData.filters, criteriaBuilder, Place.class, queryRoot, null);
		query.select(queryRoot);

		return entityManager.createQuery(query);
	}

	private TypedQuery<Long> buildPlaceIdQuery(final GazetteerQuery<Place> queryData) {
		final CriteriaBuilder criteriaBuilder = entityManager.getCriteriaBuilder();
		final CriteriaQuery<Long> query = criteriaBuilder.createQuery(Long.class);
		final Root<Place> queryRoot = query.from(Place.class);

		buildWhereClause(query, queryData.filters, criteriaBuilder, Place.class, queryRoot, null);
		query.select(queryRoot.get("id"));

		return entityManager.createQuery(query);
	}

	private TypedQuery<Long> buildCountQuery(final GazetteerQuery<Place> queryData) {
		final CriteriaBuilder criteriaBuilder = entityManager.getCriteriaBuilder();
		final CriteriaQuery<Long> query = criteriaBuilder.createQuery(Long.class);
		final Root<Place> queryRoot = query.from(Place.class);
		buildWhereClause(query, queryData.filters, criteriaBuilder, Place.class, queryRoot, null);
		query.select(criteriaBuilder.count(queryRoot));

		return entityManager.createQuery(query);
	}

	private TypedQuery<Object[]> buildSelectedPlaceFeaturesQuery(final GazetteerQuery<Place> queryData,
			final PlaceFeatureSelectionBuilder selectBuilder, final AdditionalPredicateBuilder predicateBuilder) {
		final CriteriaBuilder criteriaBuilder = entityManager.getCriteriaBuilder();
		final CriteriaQuery<Object[]> query = criteriaBuilder.createQuery(Object[].class);
		final Root<Place> queryRoot = query.from(Place.class);
		buildWhereClause(query, queryData.filters, criteriaBuilder, Place.class, queryRoot,
				predicateBuilder == null ? null : predicateBuilder.buildPredicates(criteriaBuilder, queryRoot));
		query.select(selectBuilder.buildSelectClause(criteriaBuilder, queryRoot));

		return entityManager.createQuery(query);
	}

	private static <S, T> void buildWhereClause(final CriteriaQuery<S> query, final List<QueryFilter<T>> filters,
			final CriteriaBuilder criteriaBuilder, final Class<T> clazz, final Root<T> queryRoot,
			final List<Predicate> defaultPredicates) {
		final List<Predicate> predicates = defaultPredicates == null ? new ArrayList<>()
				: new ArrayList<>(defaultPredicates);

		filters.forEach(filter -> predicates.add(filter.applyFilterCriteria(criteriaBuilder, query, queryRoot)));
		query.where(predicates.toArray(new Predicate[predicates.size()]));
	}

}
