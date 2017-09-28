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
import de.unihd.dbs.geoparser.gazetteer.models.PlaceType;
import de.unihd.dbs.geoparser.gazetteer.models.PlaceTypeAssignment;
import de.unihd.dbs.geoparser.gazetteer.models.Type;
import de.unihd.dbs.geoparser.process.linking.ToponymLinkingAnnotator;

import edu.stanford.nlp.ling.CoreAnnotations;
import edu.stanford.nlp.pipeline.Annotation;
import edu.stanford.nlp.pipeline.Annotator.Requirement;
import edu.stanford.nlp.util.CoreMap;

/**
 * Implementation of {@link ToponymDisambiguator} that disambiguates toponyms by taking the gazetteer entry with the
 * highest administrative level as the correct match. A {@link PlaceType} representing the administrative hierarchy root
 * must be specified. The highest admin level is the one that is closest to the root.
 *
 * @author lrichter
 *
 */
public class  HighestAdminLevelDisambiguator extends ToponymDisambiguator {

	private final PlaceType adminLevelRootType;

	@Override
	public Set<Requirement> requires() {
		return Collections.singleton(ToponymLinkingAnnotator.TOPONYM_LINKING_REQUIREMENT);
	}

	@Override
	public Set<Requirement> requirementsSatisfied() {
		return Collections.emptySet();
	}

	public HighestAdminLevelDisambiguator(final PlaceType adminLevelRootType) {
		super();
		Objects.requireNonNull(adminLevelRootType);
		this.adminLevelRootType = adminLevelRootType;
	}

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

			output.add(new ResolvedLocation(getPlaceWithHighestAdminLevel(linkedPlaces)));
		}

		return output;
	}

	private Place getPlaceWithHighestAdminLevel(final List<Place> linkedPlaces) {
		Place highestAdminLevelPlace = linkedPlaces.get(0);
		int highestAdminLevel = 0;

		for (final Place place : linkedPlaces) {
			final int adminLevel = getAdminLevel(place);
			if (adminLevel > highestAdminLevel) {
				highestAdminLevelPlace = place;
				highestAdminLevel = adminLevel;
			}
		}

		return highestAdminLevelPlace;
	}

	private int getAdminLevel(final Place place) {
		final Set<PlaceTypeAssignment> placeTypes = place.getPlaceTypeAssignments();

		int highestAdminLevel = Integer.MIN_VALUE;

		for (final PlaceTypeAssignment placeType : placeTypes) {
			if (!placeType.getType().isChildOf(adminLevelRootType))
				continue;

			final int adminLevel = getHierachyLevelDifference(adminLevelRootType, placeType.getType());

			highestAdminLevel = Math.max(adminLevel, highestAdminLevel);
		}

		return highestAdminLevel;
	}

	private static int getHierachyLevelDifference(final PlaceType root, final PlaceType child) {
		int level = 0;
		Type currentChild = child;
		while (currentChild != root) {
			currentChild = currentChild.getParentType();
			level++;
		}

		return level;

	}
}
