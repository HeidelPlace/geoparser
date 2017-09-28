package de.unihd.dbs.geoparser.core;

import java.util.List;

import de.unihd.dbs.geoparser.gazetteer.models.Place;

import edu.stanford.nlp.ling.CoreAnnotation;
import edu.stanford.nlp.ling.CoreAnnotations;
import edu.stanford.nlp.ling.CoreAnnotations.MentionsAnnotation;
import edu.stanford.nlp.util.ErasureUtils;

/**
 * Extension of {@link CoreAnnotations}, which contains annotations specific to our geoparsing process.
 *
 * @author lrichter
 *
 */
public class GeoparsingAnnotations {

	private GeoparsingAnnotations() {
		// should not be instantiated
	}

	/**
	 * The key for resolved locations. Used for {@link MentionsAnnotation} entries of type
	 * {@link NamedEntityType#LOCATION}.
	 *
	 * @author lrichter
	 */
	public static class ResolvedLocationAnnotation implements CoreAnnotation<ResolvedLocation> {
		@Override
		public Class<ResolvedLocation> getType() {
			return ResolvedLocation.class;
		}
	}

	/**
	 * The key for linking to gazetteer entries. Used for {@link MentionsAnnotation} entries of type
	 * {@link NamedEntityType#LOCATION}.
	 *
	 * @author lrichter
	 */
	public static class GazetteerEntriesAnnotation implements CoreAnnotation<List<Place>> {
		@Override
		public Class<List<Place>> getType() {
			return ErasureUtils.uncheckedCast(List.class);
		}
	}

}
