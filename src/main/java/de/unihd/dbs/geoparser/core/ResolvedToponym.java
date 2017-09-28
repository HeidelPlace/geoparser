package de.unihd.dbs.geoparser.core;

import de.unihd.dbs.geoparser.core.GeoparsingAnnotations.ResolvedLocationAnnotation;
import de.unihd.dbs.geoparser.gazetteer.models.Place;

import edu.stanford.nlp.ling.CoreAnnotations.CharacterOffsetBeginAnnotation;
import edu.stanford.nlp.ling.CoreAnnotations.CharacterOffsetEndAnnotation;
import edu.stanford.nlp.ling.CoreAnnotations.MentionsAnnotation;
import edu.stanford.nlp.ling.CoreAnnotations.NamedEntityTagAnnotation;
import edu.stanford.nlp.ling.CoreAnnotations.TextAnnotation;
import edu.stanford.nlp.util.CoreMap;

/**
 * Specialized variant of {@link Toponym} that represents a resolved toponym.
 * <p>
 * Like {@link NamedEntity}, this class was introduced to make code more readable and to communicate certain assumptions
 * about {@link CoreMap}s. Despite the requirements of {@link NamedEntity} and {@link Toponym}, it must have a
 * {@link ResolvedLocationAnnotation}, which contains the geometry of the resolved location and optionally the
 * associated {@link Place} instance from the gazetteer.
 * <p>
 * Equality of {@link ResolvedToponym} instances extends the equality definition of {@link NamedEntity} by also
 * including the resolvedLocation field.
 * 
 * @author lrichter
 * 
 */
public class ResolvedToponym extends Toponym {

	public final ResolvedLocation resolvedLocation;

	/**
	 * Create a {@link ResolvedToponym} instance based on the given {@link MentionsAnnotation} value.
	 * <p>
	 * The sourceAnnotation must contain the following annotations:
	 * <ul>
	 * <li>{@link TextAnnotation}: the text covered by the named entity
	 * <li>{@link CharacterOffsetBeginAnnotation}: the start position in the source document
	 * <li>{@link CharacterOffsetEndAnnotation}: the end position in the source document
	 * <li>{@link NamedEntityTagAnnotation}: the named entity type, with its value being equal to the name of
	 * {@link NamedEntityType#LOCATION}
	 * <li>{@link ResolvedLocationAnnotation}: the resolved location for the toponym
	 * </ul>
	 * 
	 * @param sourceAnnotation the {@link CoreMap} that is the information source for this resolved toponym.
	 */
	public ResolvedToponym(final CoreMap sourceAnnotation) {
		super(sourceAnnotation);
		resolvedLocation = sourceAnnotation.get(ResolvedLocationAnnotation.class);
		if (resolvedLocation == null) {
			throw new IllegalArgumentException("No ResolvedLocationAnnotation available!");
		}
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = super.hashCode();
		result = prime * result + ((resolvedLocation == null) ? 0 : resolvedLocation.hashCode());
		return result;
	}

	@Override
	public boolean equals(final Object obj) {
		if (this == obj)
			return true;
		if (!super.equals(obj))
			return false;
		if (getClass() != obj.getClass())
			return false;
		final ResolvedToponym other = (ResolvedToponym) obj;
		if (resolvedLocation == null) {
			if (other.resolvedLocation != null)
				return false;
		}
		else if (!resolvedLocation.equals(other.resolvedLocation))
			return false;
		return true;
	}

	@Override
	public String toString() {
		return "ResolvedToponym [resolvedLocation=" + resolvedLocation + ", text=" + text + ", beginPosition="
				+ beginPosition + ", endPosition=" + endPosition + ", sourceAnnotation=" + sourceAnnotation + "]";
	}

}
