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
import de.unihd.dbs.geoparser.process.linking.ToponymLinkingAnnotator;

import edu.stanford.nlp.ling.CoreAnnotations;
import edu.stanford.nlp.pipeline.Annotation;
import edu.stanford.nlp.pipeline.Annotator.Requirement;
import edu.stanford.nlp.util.CoreMap;

/**
 * Implementation of {@link ToponymDisambiguator} that disambiguates toponyms by taking the first matched gazetteer
 * entry as the correct match.
 * 
 * @author lrichter
 * 
 */
public class FirstMatchToponymDisambiguator extends ToponymDisambiguator {

	@Override
	public Set<Requirement> requires() {
		return Collections.singleton(ToponymLinkingAnnotator.TOPONYM_LINKING_REQUIREMENT);
	}

	@Override
	public Set<Requirement> requirementsSatisfied() {
		return Collections.emptySet();
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

			output.add(new ResolvedLocation(linkedPlaces.get(0)));
		}

		return output;
	}

}
