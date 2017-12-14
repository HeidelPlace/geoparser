package de.unihd.dbs.geoparser.process.disambiguation;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.Set;

import de.unihd.dbs.geoparser.core.GeoparsingAnnotations;
import de.unihd.dbs.geoparser.core.NamedEntityType;
import de.unihd.dbs.geoparser.core.ResolvedLocation;
import de.unihd.dbs.geoparser.gazetteer.models.Place;
import de.unihd.dbs.geoparser.gazetteer.models.PlaceProperty;
import de.unihd.dbs.geoparser.gazetteer.types.PropertyTypes;
import de.unihd.dbs.geoparser.process.linking.ToponymLinkingAnnotator;

import edu.stanford.nlp.ling.CoreAnnotations;
import edu.stanford.nlp.pipeline.Annotation;
import edu.stanford.nlp.pipeline.Annotator.Requirement;
import edu.stanford.nlp.util.CoreMap;

/**
 * Implementation of {@link ToponymDisambiguator} that disambiguates toponyms by taking the gazetteer entry with the
 * highest population number as the correct match.
 *
 * @author lrichter
 *
 */
public class HighestPopulationDisambiguator extends ToponymDisambiguator {

	// private final PlacePropertyType populationNumberType;

	@Override
	public Set<Requirement> requires() {
		return Collections.singleton(ToponymLinkingAnnotator.TOPONYM_LINKING_REQUIREMENT);
	}

	@Override
	public Set<Requirement> requirementsSatisfied() {
		return Collections.emptySet();
	}

	// public PopulationNumberDisambiguator(final PlacePropertyType populationNumberType) {
	// Objects.requireNonNull(populationNumberType);
	// this.populationNumberType = populationNumberType;
	// }

	@Override
	public List<ResolvedLocation> disambiguate(final List<CoreMap> namedEntities, final Annotation document,
			final CoreMap sentence) {
		final List<ResolvedLocation> output = new ArrayList<>(namedEntities.size());

		for (final CoreMap namedEntity : namedEntities) {
			if (!Objects.equals(namedEntity.get(CoreAnnotations.NamedEntityTagAnnotation.class),
					NamedEntityType.LOCATION.name)) {
				output.add(null);
				continue;
			}

			final List<Place> linkedPlaces = namedEntity.get(GeoparsingAnnotations.GazetteerEntriesAnnotation.class);

			if (linkedPlaces == null || linkedPlaces.isEmpty()) {
				output.add(null);
				continue;
			}

			output.add(new ResolvedLocation(getPlaceWithHighestPopulation(linkedPlaces)));
		}

		return output;
	}

	public static Place getPlaceWithHighestPopulation(final List<Place> linkedPlaces) {
		Place highestPopulatedPlace = null;
		int highestPopulation = 0;

		for (final Place place : linkedPlaces) {
			final int placePopulation = getPlacePopulation(place);
			if (placePopulation > highestPopulation) {
				highestPopulatedPlace = place;
				highestPopulation = placePopulation;
			}
		}

		return highestPopulatedPlace;
	}

	private static int getPlacePopulation(final Place place) {
		final Set<PlaceProperty> populationNumbers = place.getPropertiesByType(PropertyTypes.POPULATION.typeName);

		int populationValue = 0;

		for (final PlaceProperty populationNumber : populationNumbers) {
			if (populationNumber.getValue() != null) {
				try {
					populationValue = Integer.parseInt(populationNumber.getValue());
				}
				catch (Exception e){
					populationValue = Integer.MAX_VALUE;
				}
				// good for now. could do more fancy stuff, if multiple population numbers are available...
				break;
			}
		}

		return populationValue;
	}

}