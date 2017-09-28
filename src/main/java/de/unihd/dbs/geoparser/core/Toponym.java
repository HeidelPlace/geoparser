package de.unihd.dbs.geoparser.core;

import edu.stanford.nlp.ling.CoreAnnotations.CharacterOffsetBeginAnnotation;
import edu.stanford.nlp.ling.CoreAnnotations.CharacterOffsetEndAnnotation;
import edu.stanford.nlp.ling.CoreAnnotations.MentionsAnnotation;
import edu.stanford.nlp.ling.CoreAnnotations.NamedEntityTagAnnotation;
import edu.stanford.nlp.ling.CoreAnnotations.TextAnnotation;
import edu.stanford.nlp.util.CoreMap;

/**
 * Specialized variant of {@link NamedEntity} that represents a toponym.
 * <p>
 * Like {@link NamedEntity}, this class was introduced to make code more readable and to communicate certain assumptions
 * about {@link CoreMap}s. Despite the requirements of {@link NamedEntity},it must have a
 * {@link NamedEntityTagAnnotation} with its value being equal to the name of {@link NamedEntityType#LOCATION}.
 * 
 * @author lrichter
 * 
 */
public class Toponym extends NamedEntity {

	/**
	 * Create a {@link Toponym} instance based on the given {@link MentionsAnnotation} value.
	 * <p>
	 * The sourceAnnotation must contain the following annotations:
	 * <ul>
	 * <li>{@link TextAnnotation}: the text covered by the named entity
	 * <li>{@link CharacterOffsetBeginAnnotation}: the start position in the source document
	 * <li>{@link CharacterOffsetEndAnnotation}: the end position in the source document
	 * <li>{@link NamedEntityTagAnnotation}: the named entity type, with its value being equal to the name of
	 * {@link NamedEntityType#LOCATION}
	 * </ul>
	 * 
	 * @param sourceAnnotation the {@link CoreMap} that is the information source for this toponym.
	 */
	public Toponym(final CoreMap sourceAnnotation) {
		super(sourceAnnotation);
		if (!this.type.equals(NamedEntityType.LOCATION)) {
			throw new IllegalArgumentException("Named entity type must be `" + NamedEntityType.LOCATION.name + "`!");
		}
	}

	@Override
	public String toString() {
		return "Toponym [text='" + text + "', beginPosition=" + beginPosition + ", endPosition=" + endPosition
				+ ", sourceAnnotation=" + sourceAnnotation.toShorterString() + "]";
	}

}