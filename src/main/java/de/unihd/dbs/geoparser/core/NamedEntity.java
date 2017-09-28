package de.unihd.dbs.geoparser.core;

import edu.stanford.nlp.ling.CoreAnnotations.CharacterOffsetBeginAnnotation;
import edu.stanford.nlp.ling.CoreAnnotations.CharacterOffsetEndAnnotation;
import edu.stanford.nlp.ling.CoreAnnotations.MentionsAnnotation;
import edu.stanford.nlp.ling.CoreAnnotations.NamedEntityTagAnnotation;
import edu.stanford.nlp.ling.CoreAnnotations.TextAnnotation;
import edu.stanford.nlp.pipeline.EntityMentionsAnnotator;

import edu.stanford.nlp.util.CoreMap;

/**
 * General representation of a named entity that is described by a {@link CoreMap} instance.
 * <p>
 * Convenience class that abstracts away low-level {@link CoreMap}s, which represent {@link MentionsAnnotation} values.
 * This is done for better conciseness, implying certain assumptions about a {@link CoreMap} instance. In particular,
 * the map is expected to be a {@link CoreMap} that was created by {@link EntityMentionsAnnotator}. Consequently, the
 * map describes named entities (detected by NER) or quantifiable entities (e.g., by TimeAnnotator). These entities may
 * span multiple tokens, thus {@link MentionsAnnotation} are document or sentence level annotations.
 * <p>
 * Equality of {@link NamedEntity} instances is defined over the fields type, text, beginPosition, endPosition (i.e.,
 * sourceAnnotation is excluded).
 * 
 * @author lrichter
 *
 */
public class NamedEntity {

	public final NamedEntityType type;
	public final String text;
	public final int beginPosition;
	public final int endPosition;
	public final CoreMap sourceAnnotation;

	/**
	 * Create a {@link NamedEntity} instance based on the given {@link MentionsAnnotation} value.
	 * <p>
	 * The sourceAnnotation must contain the following annotations:
	 * <ul>
	 * <li>{@link TextAnnotation}: the text covered by the named entity
	 * <li>{@link CharacterOffsetBeginAnnotation}: the start position in the source document
	 * <li>{@link CharacterOffsetEndAnnotation}: the end position in the source document
	 * <li>{@link NamedEntityTagAnnotation}: the named entity type
	 * </ul>
	 * 
	 * @param sourceAnnotation the {@link CoreMap} that is the information source for this named entity.
	 */
	public NamedEntity(final CoreMap sourceAnnotation) {
		this.text = sourceAnnotation.get(TextAnnotation.class);
		if (this.text == null) {
			throw new IllegalArgumentException("No TextAnnotation available!");
		}
		final Integer beginPosition = sourceAnnotation.get(CharacterOffsetBeginAnnotation.class);
		if (beginPosition == null) {
			throw new IllegalArgumentException("No CharacterOffsetBeginAnnotation available!");
		}
		else {
			this.beginPosition = beginPosition.intValue();
		}
		final Integer endPosition = sourceAnnotation.get(CharacterOffsetEndAnnotation.class);
		if (endPosition == null) {
			throw new IllegalArgumentException("No CharacterOffsetEndAnnotation available!");
		}
		else {
			this.endPosition = endPosition.intValue();
		}
		final String neType = sourceAnnotation.get(NamedEntityTagAnnotation.class);
		if (neType == null) {
			throw new IllegalArgumentException("No NamedEntityTagAnnotation available!");
		}
		type = NamedEntityType.getByName(neType);
		this.sourceAnnotation = sourceAnnotation;
	}

	/**
	 * Create a {@link NamedEntity} instance based on the given parameters.
	 * 
	 * @param text the textual representation of the named entity
	 * @param beginPosition the starting character offset
	 * @param endPosition the ending character offset
	 * @param type the named entity type
	 * @param sourceAnnotation the sourceAnnotation behind the named entity.
	 */
	public NamedEntity(final String text, final int beginPosition, final int endPosition, final NamedEntityType type,
			final CoreMap sourceAnnotation) {
		this.text = text;
		this.type = type;
		this.beginPosition = beginPosition;
		this.endPosition = endPosition;
		this.sourceAnnotation = sourceAnnotation;
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + beginPosition;
		result = prime * result + endPosition;
		result = prime * result + ((text == null) ? 0 : text.hashCode());
		result = prime * result + ((type == null) ? 0 : type.hashCode());
		return result;
	}

	@Override
	public boolean equals(final Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (getClass() != obj.getClass())
			return false;
		final NamedEntity other = (NamedEntity) obj;
		if (beginPosition != other.beginPosition)
			return false;
		if (endPosition != other.endPosition)
			return false;
		if (text == null) {
			if (other.text != null)
				return false;
		}
		else if (!text.equals(other.text))
			return false;
		if (type != other.type)
			return false;
		return true;
	}

	@Override
	public String toString() {
		return "NamedEntity [type=" + type + ", text='" + text + "', beginPosition=" + beginPosition + ", endPosition="
				+ endPosition + ", sourceAnnotation="
				+ (sourceAnnotation != null ? sourceAnnotation.toShorterString() : "null") + "]";
	}

}
