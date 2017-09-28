package de.unihd.dbs.geoparser.gazetteer.query;

import java.util.EnumSet;
import java.util.Objects;

import javax.persistence.criteria.CriteriaBuilder;
import javax.persistence.criteria.CriteriaQuery;
import javax.persistence.criteria.Expression;
import javax.persistence.criteria.Predicate;
import javax.persistence.criteria.Root;
import javax.persistence.criteria.Subquery;

import org.hibernate.query.criteria.internal.CriteriaBuilderImpl;
import de.unihd.dbs.geoparser.gazetteer.models.Place;
import de.unihd.dbs.geoparser.gazetteer.models.PlaceName;
import de.unihd.dbs.geoparser.gazetteer.models.PlaceName.NameFlag;
import de.unihd.dbs.geoparser.gazetteer.query.predicates.LevenshteinDistancePredicate;
import de.unihd.dbs.geoparser.util.StringUtil;

/**
 * Implementation of a {@link QueryFilter} that filters {@link Place}s based on their name.
 * <p>
 * The filtering may be limited to certain languages and {@link NameFlag} states. For string matching, capitalization
 * may be ignored (case insensitive) and fuzzy string matching may be performed.
 * 
 * @author lrichter
 * 
 */
/* @formatter:off
 * Possible future extensions:
 * <ul>
 * <li> XXX: provide unaccent feature: https://www.postgresql.org/docs/9.5/static/unaccent.html
 * <li> See http://stackoverflow.com/questions/11249635/finding-similar-strings-with-postgresql-quickly/11250001#11250001
 *   and http://rachbelaid.com/postgres-full-text-search-is-good-enough/
 * <li> evaluate performance of http://hibernate.org/search/documentation/
 * </ul>
 * @formatter:on
 */
public class PlaceNamePlaceFilter extends QueryFilter<Place> {

	/**
	 * Available string-matching modes.
	 * 
	 * @author lrichter
	 * 
	 */
	public enum MatchMode {
		/**
		 * Exact string matching.
		 */
		EXACT,

		/**
		 * Apply Levensthein matching and take all matches above a threshold (number of required modification for full
		 * match).
		 */
		FUZZY_LEVENSTHEIN,

		/**
		 * Fuzzy string matching that allows any prefix to match strings.
		 */
		FUZZY_PREFIX,

		/**
		 * Fuzzy string matching that allows any postfix to match strings.
		 */
		FUZZY_POSTFIX,

		/**
		 * Fuzzy string matching that allows any pre- and postfix to match strings.
		 */
		FUZZY_PREFIX_POSTFIX
	}

	private String name;
	private String language;
	private EnumSet<NameFlag> flags;
	private boolean ignoreCase;
	private MatchMode matchMode;
	private Double maxFuzzyDistanceThreshold;

	/**
	 * Create a {@link PlaceNamePlaceFilter} with the given parameters.
	 * 
	 * @param name the name of the place. Must not be null.
	 * @param language the language assigned to the name.
	 * @param flags the flags that must hold for the place name.
	 * @param ignoreCase if <code>true</code>, a case insensitive matching will be performed. Otherwise, the case does
	 *            matter.
	 * @param matchMode the string-matching mode to use. Must not be null.
	 * @param maxFuzzyDistanceThreshold the maximum distance for fuzzy matching modes (currently only applies to
	 *            {@link MatchMode#FUZZY_LEVENSTHEIN})
	 * @param exclusive if <code>true</code>, all places with matching name (language, flags) do not pass the filter.
	 *            Otherwise, only those do.
	 */
	public PlaceNamePlaceFilter(final String name, final String language, final EnumSet<NameFlag> flags,
			final boolean ignoreCase, final MatchMode matchMode, final Double maxFuzzyDistanceThreshold,
			final boolean exclusive) {
		super(exclusive);
		setName(name);
		setLanguage(language);
		setFlags(flags);
		setIgnoreCase(ignoreCase);
		setMatchMode(matchMode);
		setMaxFuzzyDistanceThreshold(maxFuzzyDistanceThreshold);
	}

	public String getName() {
		return name;
	}

	public void setName(final String name) {
		Objects.requireNonNull(name);
		this.name = name;
	}

	public String getLanguage() {
		return language;
	}

	public void setLanguage(final String language) {
		this.language = language;
	}

	public EnumSet<NameFlag> getFlags() {
		return flags;
	}

	public void setFlags(final EnumSet<NameFlag> flags) {
		this.flags = flags;
	}

	public boolean isIgnoreCase() {
		return ignoreCase;
	}

	public void setIgnoreCase(final boolean ignoreCase) {
		this.ignoreCase = ignoreCase;
	}

	public MatchMode getMatchMode() {
		return matchMode;
	}

	public void setMatchMode(final MatchMode matchMode) {
		Objects.requireNonNull(matchMode);
		this.matchMode = matchMode;
	}

	public Double getMaxFuzzyDistanceThreshold() {
		return maxFuzzyDistanceThreshold;
	}

	public void setMaxFuzzyDistanceThreshold(final Double maxFuzzyDistanceThreshold) {
		this.maxFuzzyDistanceThreshold = maxFuzzyDistanceThreshold;
	}

	@Override
	public Predicate applyFilterCriteria(final CriteriaBuilder criteriaBuilder, final CriteriaQuery<?> query,
			final Root<Place> placeRoot) {
		final Subquery<PlaceName> subQuery = query.subquery(PlaceName.class);
		final Root<PlaceName> names = subQuery.from(PlaceName.class);

		Predicate predicate;

		final Expression<String> nameExpr = (ignoreCase) ? criteriaBuilder.lower(names.get("name")) : names.get("name");
		// XXX: this way, Postgres cannot optimize the query with an index, since it does not know that the string is
		// guaranteed to be lower case. A separate predicate would help...
		final String name = (ignoreCase) ? StringUtil.toLowerCase(this.name) : this.name;

		switch (matchMode) {
		case EXACT:
			predicate = criteriaBuilder.like(nameExpr, name);
			break;
		case FUZZY_LEVENSTHEIN:
			predicate = new LevenshteinDistancePredicate((CriteriaBuilderImpl) criteriaBuilder, name, nameExpr,
					maxFuzzyDistanceThreshold.intValue());
			break;
		case FUZZY_POSTFIX:
			predicate = criteriaBuilder.like(nameExpr, name + "%");
			break;
		case FUZZY_PREFIX:
			predicate = criteriaBuilder.like(nameExpr, "%" + name);
			break;
		case FUZZY_PREFIX_POSTFIX:
			predicate = criteriaBuilder.like(nameExpr, "%" + name + "%");
			break;
		default:
			throw new IllegalArgumentException("Unknown matchMode!");
		}

		if (language != null) {
			predicate = criteriaBuilder.and(predicate, criteriaBuilder.like(names.get("language"), language));
		}
		for (final NameFlag flag : flags) {
			switch (flag) {
			case IS_ABBREVIATION:
				predicate = criteriaBuilder.and(predicate, criteriaBuilder.isTrue(names.get("isAbbreviation")));
				break;
			case IS_COLLOQUIAL:
				predicate = criteriaBuilder.and(predicate, criteriaBuilder.isTrue(names.get("isColloquial")));
				break;
			case IS_HISTORICAL:
				predicate = criteriaBuilder.and(predicate, criteriaBuilder.isTrue(names.get("isHistorical")));
				break;
			case IS_NOT_ABBREVIATION:
				predicate = criteriaBuilder.and(predicate,
						criteriaBuilder.not(criteriaBuilder.isTrue(names.get("isAbbreviation"))));
				break;
			case IS_NOT_COLLOQUIAL:
				predicate = criteriaBuilder.and(predicate,
						criteriaBuilder.not(criteriaBuilder.isTrue(names.get("isColloquial"))));
				break;
			case IS_NOT_HISTORICAL:
				predicate = criteriaBuilder.and(predicate,
						criteriaBuilder.not(criteriaBuilder.isTrue(names.get("isHistorical"))));
				break;
			case IS_NOT_OFFICIAL:
				predicate = criteriaBuilder.and(predicate,
						criteriaBuilder.not(criteriaBuilder.isTrue(names.get("isOfficial"))));
				break;
			case IS_NOT_PREFERRED:
				predicate = criteriaBuilder.and(predicate,
						criteriaBuilder.not(criteriaBuilder.isTrue(names.get("isPreferred"))));
				break;
			case IS_OFFICIAL:
				predicate = criteriaBuilder.and(predicate, criteriaBuilder.isTrue(names.get("isOfficial")));
				break;
			case IS_PREFERRED:
				predicate = criteriaBuilder.and(predicate, criteriaBuilder.isTrue(names.get("isPreferred")));
				break;
			default:
				break;
			}
		}

		predicate = criteriaBuilder.exists(subQuery.select(names).where(criteriaBuilder
				.and(criteriaBuilder.equal(placeRoot.get("id"), names.get("place").get("id")), predicate)));

		if (isExclusive()) {
			return criteriaBuilder.not(predicate);
		}
		else {
			return predicate;
		}

	}

}
