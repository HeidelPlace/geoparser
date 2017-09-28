package de.unihd.dbs.geoparser.process.linking;

import java.util.ArrayList;
import java.util.Collections;
import java.util.EnumSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;

import de.unihd.dbs.geoparser.core.NamedEntityType;
import de.unihd.dbs.geoparser.gazetteer.Gazetteer;
import de.unihd.dbs.geoparser.gazetteer.GazetteerQuery;
import de.unihd.dbs.geoparser.gazetteer.models.Place;
import de.unihd.dbs.geoparser.gazetteer.models.PlaceName.NameFlag;
import de.unihd.dbs.geoparser.gazetteer.query.PlaceNamePlaceFilter;
import de.unihd.dbs.geoparser.gazetteer.query.PlaceNamePlaceFilter.MatchMode;
import de.unihd.dbs.geoparser.gazetteer.query.PlaceTypePlaceFilter;
import de.unihd.dbs.geoparser.process.recognition.ToponymRecognitionAnnotator;

import edu.stanford.nlp.ling.CoreAnnotations;
import edu.stanford.nlp.pipeline.Annotation;
import edu.stanford.nlp.pipeline.Annotator.Requirement;
import edu.stanford.nlp.util.CoreMap;

/**
 * Implementation of {@link ToponymLinker} that links toponyms to gazetteer entries by exact name matching.
 * 
 * @author lrichter
 * 
 */
// TODO: not clear where to put the place type filter!!! duplication for gazetteerLookup might be introduced. Possible
// to merge the functionality?
public class GazetteerExactToponymLinker extends ToponymLinker {

	private final Gazetteer gazetteer;
	private final int maxMatches;
	private final PlaceTypePlaceFilter placeTypeFilter;

	public GazetteerExactToponymLinker(final Gazetteer gazetteer, final int maxMatches) {
		this(gazetteer, maxMatches, null);
	}

	public GazetteerExactToponymLinker(final Gazetteer gazetteer, final int maxMatches,
			final PlaceTypePlaceFilter placeTypeFilter) {
		super();
		if (maxMatches < 0) {
			throw new IllegalArgumentException("maxMatches must be a non-negative number!");
		}
		Objects.requireNonNull(gazetteer);
		this.maxMatches = maxMatches;
		this.gazetteer = gazetteer;
		this.placeTypeFilter = placeTypeFilter;
	}

	@Override
	public Set<Requirement> requires() {
		return Collections.singleton(ToponymRecognitionAnnotator.TOPONYM_RECOGNITION_REQUIREMENT);
	}

	@Override
	public Set<Requirement> requirementsSatisfied() {
		return Collections.singleton(ToponymLinkingAnnotator.TOPONYM_LINKING_REQUIREMENT);
	}

	@Override
	public List<List<Place>> link(final List<CoreMap> namedEntities, final Annotation document,
			final CoreMap sentence) {
		final List<List<Place>> output = new ArrayList<>(namedEntities.size());

		final GazetteerQuery<Place> query = new GazetteerQuery<>(maxMatches);
		final PlaceNamePlaceFilter placeNameFilter = new PlaceNamePlaceFilter("initialValue", null,
				EnumSet.noneOf(NameFlag.class), false, MatchMode.EXACT, null, false);

		query.filters.add(placeNameFilter);
		if (placeTypeFilter != null) {
			System.out.println(placeTypeFilter.getPlaceTypes());
			query.filters.add(placeTypeFilter);
		}

		for (final CoreMap namedEntity : namedEntities) {
			if (!Objects.equals(namedEntity.get(CoreAnnotations.NamedEntityTagAnnotation.class),
					NamedEntityType.LOCATION.name)) {
				output.add(null);
				continue;
			}

			final String toponym = namedEntity.get(CoreAnnotations.TextAnnotation.class);
			placeNameFilter.setName(toponym);
			final List<Place> matchedPlaces = gazetteer.getPlaces(query);

			if (matchedPlaces.size() > 0) {
				output.add(matchedPlaces);
			}
			else {
				output.add(null);
			}
		}

		return output;
	}
}
