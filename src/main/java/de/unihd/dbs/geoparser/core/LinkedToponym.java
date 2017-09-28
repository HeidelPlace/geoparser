package de.unihd.dbs.geoparser.core;

import java.util.List;
import java.util.stream.Collectors;

import de.unihd.dbs.geoparser.core.GeoparsingAnnotations.GazetteerEntriesAnnotation;
import de.unihd.dbs.geoparser.gazetteer.models.Place;

import edu.stanford.nlp.ling.CoreAnnotations.CharacterOffsetBeginAnnotation;
import edu.stanford.nlp.ling.CoreAnnotations.CharacterOffsetEndAnnotation;
import edu.stanford.nlp.ling.CoreAnnotations.MentionsAnnotation;
import edu.stanford.nlp.ling.CoreAnnotations.NamedEntityTagAnnotation;
import edu.stanford.nlp.ling.CoreAnnotations.TextAnnotation;
import edu.stanford.nlp.util.CoreMap;

/**
 * Specialized variant of {@link Toponym} that represents a linked toponym.
 * <p>
 * Like {@link NamedEntity}, this class was introduced to make code more readable and to communicate certain assumptions
 * about {@link CoreMap}s. Despite the requirements of {@link NamedEntity} and {@link Toponym}, it must have a
 * {@link GazetteerEntriesAnnotation} that contains no, one, or more {@link Place} instances.
 * <p>
 * Equality of {@link LinkedToponym} instances extends the equality definition of {@link NamedEntity} by also including
 * the gazetteerEntries field.
 * 
 * @author lrichter
 * 
 */
public final class LinkedToponym extends Toponym {

	public final List<Place> gazetteerEntries;

	/**
	 * Create a {@link LinkedToponym} instance based on the given {@link MentionsAnnotation} value.
	 * <p>
	 * The sourceAnnotation must contain the following annotations:
	 * <ul>
	 * <li>{@link TextAnnotation}: the text covered by the named entity
	 * <li>{@link CharacterOffsetBeginAnnotation}: the start position in the source document
	 * <li>{@link CharacterOffsetEndAnnotation}: the end position in the source document
	 * <li>{@link NamedEntityTagAnnotation}: the named entity type, with its value being equal to the name of
	 * {@link NamedEntityType#LOCATION}
	 * <li>{@link GazetteerEntriesAnnotation}: the gazetteer entries linked to the toponym
	 * </ul>
	 * 
	 * @param sourceAnnotation the {@link CoreMap} that is the information source for this linked toponym.
	 */
	public LinkedToponym(final CoreMap sourceAnnotation) {
		super(sourceAnnotation);
		gazetteerEntries = sourceAnnotation.get(GazetteerEntriesAnnotation.class);
		if (gazetteerEntries == null) {
			throw new IllegalArgumentException("No GazetteerEntryAnnotation available!");
		}
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = super.hashCode();
		result = prime * result + ((gazetteerEntries == null) ? 0 : gazetteerEntries.hashCode());
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
		final LinkedToponym other = (LinkedToponym) obj;
		if (gazetteerEntries == null) {
			if (other.gazetteerEntries != null)
				return false;
		}
		else if (!gazetteerEntries.equals(other.gazetteerEntries))
			return false;
		return true;
	}

	@Override
	public String toString() {
		return "LinkedToponym [text='" + text + "', beginPosition=" + beginPosition + ", endPosition=" + endPosition
				+ ", gazetteerEntries=["
				+ gazetteerEntries.stream().map(place -> place.getId() != null ? place.getId().toString() : null)
						.collect(Collectors.joining(", "))
				+ "], sourceAnnotation=" + sourceAnnotation.toShorterString() + "]";
	}

}
