package de.unihd.dbs.geoparser.util;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

import de.unihd.dbs.geoparser.core.GeoparsingAnnotations.GazetteerEntriesAnnotation;
import de.unihd.dbs.geoparser.core.GeoparsingAnnotations.ResolvedLocationAnnotation;
import de.unihd.dbs.geoparser.core.LinkedToponym;
import de.unihd.dbs.geoparser.core.NamedEntity;
import de.unihd.dbs.geoparser.core.NamedEntityType;
import de.unihd.dbs.geoparser.core.ResolvedToponym;
import de.unihd.dbs.geoparser.core.Toponym;

import edu.stanford.nlp.ling.CoreAnnotations.MentionsAnnotation;
import edu.stanford.nlp.ling.CoreAnnotations.NamedEntityTagAnnotation;
import edu.stanford.nlp.ling.CoreAnnotations.SentencesAnnotation;
import edu.stanford.nlp.util.CoreMap;

public class GeoparserUtil {

	public static List<NamedEntity> getNamedEntities(final CoreMap document) {
		return getNamedEntityMentions(document).stream().map(token -> new NamedEntity(token))
				.collect(Collectors.toList());
	}

	public static List<CoreMap> getNamedEntityMentions(final CoreMap document) {
		final List<CoreMap> sentences = document.get(SentencesAnnotation.class);
		final List<CoreMap> mentions = new ArrayList<>();
		for (final CoreMap sentence : sentences) {
			if (!sentence.has(MentionsAnnotation.class)) {
				continue;
			}

			for (final CoreMap mention : sentence.get(MentionsAnnotation.class)) {
				if (mention.has(NamedEntityTagAnnotation.class)) {
					if (!Objects.equals(mention.get(NamedEntityTagAnnotation.class), NamedEntityType.NONE.name)) {
						mentions.add(mention);
					}
				}
			}
		}

		return mentions;
	}

	@Deprecated // should not be needed anymore since we now use lists instead of sets
	public static <T extends NamedEntity> List<T> sortNamedEntitiesByOccurrence(final Collection<T> namedEntities) {
		final List<T> sortedList = new ArrayList<>(namedEntities);
		Collections.sort(sortedList, new Comparator<NamedEntity>() {
			@Override
			public int compare(final NamedEntity entityA, final NamedEntity entityB) {
				return Integer.compare(entityA.beginPosition, entityB.beginPosition);
			}
		});
		return sortedList;
	}

	public static List<Toponym> getToponyms(final CoreMap document) {
		return getToponymMentions(document).stream().map(token -> new Toponym(token)).collect(Collectors.toList());
	}

	public static List<Toponym> getToponyms(final List<NamedEntity> namedEntities) {
		return namedEntities.stream().filter(namedEntity -> Objects.equals(namedEntity.type, NamedEntityType.LOCATION))
				.map(namedEntity -> new Toponym(namedEntity.sourceAnnotation)).collect(Collectors.toList());
	}

	public static List<CoreMap> getToponymMentions(final CoreMap document) {
		final List<CoreMap> sentences = document.get(SentencesAnnotation.class);
		final List<CoreMap> mentions = new ArrayList<>();
		for (final CoreMap sentence : sentences) {
			if (!sentence.has(MentionsAnnotation.class)) {
				continue;
			}

			for (final CoreMap mention : sentence.get(MentionsAnnotation.class)) {
				if (mention.has(NamedEntityTagAnnotation.class)) {
					if (Objects.equals(mention.get(NamedEntityTagAnnotation.class), NamedEntityType.LOCATION.name)) {
						mentions.add(mention);
					}
				}
			}
		}

		return mentions;
	}

	public static List<LinkedToponym> getLinkedToponyms(final CoreMap document) {
		return getLinkedToponymMentions(document).stream().map(token -> new LinkedToponym(token))
				.collect(Collectors.toList());
	}

	public static List<CoreMap> getLinkedToponymMentions(final CoreMap document) {
		final List<CoreMap> sentences = document.get(SentencesAnnotation.class);
		final List<CoreMap> mentions = new ArrayList<>();
		for (final CoreMap sentence : sentences) {
			if (!sentence.has(MentionsAnnotation.class)) {
				continue;
			}

			for (final CoreMap mention : sentence.get(MentionsAnnotation.class)) {
				// we also check for the named entity type, since in future a gazetteer entry might also match
				// non-places
				if (mention.has(NamedEntityTagAnnotation.class) && mention.has(GazetteerEntriesAnnotation.class)) {
					if (Objects.equals(mention.get(NamedEntityTagAnnotation.class), NamedEntityType.LOCATION.name)) {
						mentions.add(mention);
					}
				}
			}
		}

		return mentions;
	}

	public static List<ResolvedToponym> getResolvedToponyms(final CoreMap document) {
		return getResolvedToponymMentions(document).stream().map(token -> new ResolvedToponym(token))
				.collect(Collectors.toList());
	}

	public static List<CoreMap> getResolvedToponymMentions(final CoreMap document) {
		final List<CoreMap> sentences = document.get(SentencesAnnotation.class);
		final List<CoreMap> mentions = new ArrayList<>();
		for (final CoreMap sentence : sentences) {
			if (!sentence.has(MentionsAnnotation.class)) {
				continue;
			}

			for (final CoreMap mention : sentence.get(MentionsAnnotation.class)) {
				if (mention.has(ResolvedLocationAnnotation.class)) {
					mentions.add(mention);
				}
			}
		}

		return mentions;
	}

}
