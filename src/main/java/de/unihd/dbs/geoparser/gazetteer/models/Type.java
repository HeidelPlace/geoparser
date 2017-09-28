package de.unihd.dbs.geoparser.gazetteer.models;

import java.util.Collections;
import java.util.HashSet;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;

import javax.persistence.CascadeType;
import javax.persistence.Column;
import javax.persistence.DiscriminatorColumn;
import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.ForeignKey;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.Inheritance;
import javax.persistence.InheritanceType;
import javax.persistence.JoinColumn;
import javax.persistence.JoinTable;
import javax.persistence.ManyToMany;
import javax.persistence.ManyToOne;
import javax.persistence.OneToMany;
import javax.persistence.SequenceGenerator;
import javax.persistence.Table;
import javax.persistence.Transient;

import org.hibernate.Hibernate;
import org.hibernate.annotations.DiscriminatorOptions;
import de.unihd.dbs.geoparser.gazetteer.util.GazetteerPersistenceManager;

/**
 * This is the abstract base class for modeling the type system of the gazetteer data model. Subclasses of the base
 * class are distinguished in the persistence store via different discriminator values for the discriminator column
 * "type".
 * <p>
 * Each {@link Type} instance has a unique name and Id. The Id is assigned during JPA persistence with the help of a
 * sequence generator. Note that the Id-sequence may have holes (due to pre-allocation of multiple sequence numbers for
 * performance reasons). Before the initial persistence of an entity, the Id is set to <code>null</code>. The Id should
 * not be manually changed, since JPA takes care of that and will get confused if you do so... <br>
 * Furthermore, each instance may have an optional description, a set of child types, a parent type, and a set of
 * similar types. <br>
 * <b>Note:</b> No cycle detection is performed on the parent-child relationship except for self references! <br>
 * <b>Note:</b>If a {@link Type} instance is removed, it is removed from the type hierarchy and similar-type relations
 * are removed, but it does not cascade the delete! Otherwise, complete type hierarchies could be accidentally
 * deleted... Use {@link GazetteerPersistenceManager#removeType} for removing a type from the persistence store.<br>
 * <b>Note:</b> Due to technical reasons, {@link #equals} and {@link #hashCode} only work correctly, if the same entity
 * is not loaded multiple times from the JPA entity manager after it has been initially persisted!
 *
 * @author lrichter
 */
// TODO: we do not check, if parentType, childTypes, or similarTypes are of same class as the owning type
// HINT: we might run into problems with HibernateProxies though, if we do so!
@Entity
@Inheritance(strategy = InheritanceType.SINGLE_TABLE)
@DiscriminatorColumn(name = "type", columnDefinition = "text")
@DiscriminatorOptions(force = true)
@Table(name = "type")
@SequenceGenerator(name = "type_id_sequence", sequenceName = "type_id_sequence", initialValue = 1, allocationSize = 1)
public abstract class Type {
	@Id
	@GeneratedValue(generator = "type_id_sequence", strategy = GenerationType.SEQUENCE)
	private Long id;

	@Transient
	private UUID uuid;

	@Column(name = "name", unique = true, nullable = false, columnDefinition = "text")
	private String name;

	@Column(name = "description", columnDefinition = "text")
	private String description;

	// we don't cascade deletions to avoid deleting the full type-hierarchy!
	@OneToMany(cascade = { CascadeType.PERSIST, CascadeType.DETACH, CascadeType.MERGE, CascadeType.REFRESH },
			fetch = FetchType.LAZY)
	@JoinTable(name = "type_hierarchy", joinColumns = @JoinColumn(name = "parent_id"),
			inverseJoinColumns = @JoinColumn(name = "child_id"),
			foreignKey = @ForeignKey(name = "parent_child_type_fk"),
			inverseForeignKey = @ForeignKey(name = "child_parent_type_fk"))
	private Set<Type> childTypes;

	@ManyToOne(cascade = { CascadeType.PERSIST, CascadeType.DETACH, CascadeType.MERGE, CascadeType.REFRESH },
			fetch = FetchType.LAZY)
	@JoinColumn(foreignKey = @ForeignKey(name = "parent_type_fk"))
	private Type parentType;

	// we don't cascade deletions to avoid deleting related types!
	@ManyToMany(cascade = { CascadeType.PERSIST, CascadeType.DETACH, CascadeType.MERGE, CascadeType.REFRESH },
			fetch = FetchType.LAZY)
	@JoinTable(name = "similar_types", joinColumns = @JoinColumn(name = "type_a_id"),
			inverseJoinColumns = @JoinColumn(name = "type_b_id"), foreignKey = @ForeignKey(name = "similar_type_a_fk"),
			inverseForeignKey = @ForeignKey(name = "similar_type_b_fk"))
	private Set<Type> similarTypes;

	/**
	 * Create an instance of {@link Type} with no assigned information. Must be called by any subclass!
	 */
	protected Type() {
		super();
		init();
	}

	/**
	 * Create an instance of {@link Type} with the given parameters.
	 *
	 * @param name a unique name for the type.
	 * @param description a description for the type.
	 * @param childTypes a set of child types.
	 * @param parentType a parent type.
	 * @param similiarTypes a set of similar types.
	 */
	protected Type(final String name, final String description, final Set<Type> childTypes, final Type parentType,
			final Set<Type> similiarTypes) {
		super();
		init();
		setChildTypes(childTypes);
		setDescription(description);
		setName(name);
		setParentType(parentType);
		setSimilarTypes(similiarTypes);
	}

	private void init() {
		this.childTypes = new HashSet<>();
		this.similarTypes = new HashSet<>();
	}

	/**
	 * Get the entity Id.
	 * <p>
	 * <b>Note:</b> Before the entity is persisted by JPA, the Id is <code>null</code>!
	 *
	 * @return the entity Id.
	 */
	public Long getId() {
		return id;
	}

	/**
	 * Set the Id for this entity.
	 * <p>
	 * <b>Warning:<b> Never manually change the id, unless you know exactly what you are doing! It will be set by JPA
	 * when persisting the instance.
	 *
	 * @param id the new entity Id.
	 */
	protected void setId(final Long id) {
		this.id = id;
	}

	public String getDescription() {
		return description;
	}

	public void setDescription(final String description) {
		this.description = description;
	}

	public String getName() {
		return name;
	}

	public void setName(final String name) {
		this.name = name;
	}

	// --- CHILD TYPES ---

	/**
	 * Get an unmodifiable view of the set of children.
	 *
	 * @return the child types.
	 */
	public Set<Type> getChildTypes() {
		return Collections.unmodifiableSet(childTypes);
	}

	/**
	 * Add the given {@link Type} instance as child.
	 * <p>
	 * If it is already enlisted as child, nothing happens. Self references are ignored.
	 * <p>
	 * <b>Note:</b> the parent-field of the child is automatically set to this type entity.
	 *
	 * @param childType the child type to add. Must not be <code>null</code>.
	 */
	public void addChildType(final Type childType) {
		Objects.requireNonNull(childType);
		if (this != childType && !this.hasChild(childType)) {
			this.childTypes.add(childType);
			childType.setParentType(this);
		}
	}

	/**
	 * Add the given set of {@link Type} instances as children.
	 * <p>
	 * Each entry is passed to {@link #addChildType}.
	 *
	 * @param childTypes set of types to add as children. Must not be <code>null</code>.
	 */
	public void addChildTypes(final Set<Type> childTypes) {
		Objects.requireNonNull(childTypes);
		childTypes.forEach(childType -> addChildType(childType));
	}

	/**
	 * Remove the given {@link Type} instance from the set of children.
	 * <p>
	 * <b>Note:</b> the parent of the child is automatically set to <code>null</code>.
	 *
	 * @param childType the child type to remove. Must not be <code>null</code>.
	 */
	public void removeChildType(final Type childType) {
		Objects.requireNonNull(childType);
		this.childTypes.remove(childType);
		childType.parentType = null;
	}

	/**
	 * Change the set of children to the given set of {@link Type} instances.
	 * <p>
	 * <b>Note:</b> Existing parent-child relationships are removed beforehand.
	 *
	 * @param childTypes the new set of children. If <code>null</code>, handled like an empty set.
	 */
	public void setChildTypes(final Set<Type> childTypes) {
		if (!this.childTypes.equals(childTypes)) { // not comparing with != since we never expose the original set!
			this.childTypes.forEach(childType -> childType.parentType = null);
			this.childTypes.clear();

			if (childTypes != null) {
				addChildTypes(childTypes);
			}
		}
	}

	/**
	 * Get all children of this type entity wrt. the type hierarchy (i.e, step down recursively).
	 *
	 * @return a set with all children in the type hierarchy.
	 */
	public Set<Type> getAllChildren() {
		final Set<Type> allChildren = new HashSet<>(childTypes);
		childTypes.forEach(child -> allChildren.addAll(child.getChildTypes()));
		return allChildren;
	}

	/**
	 * Check, if the given {@link Type} instance is a child of this type entity.
	 *
	 * @param childType the {@link Type} instance that should be checked.
	 * @param recursive if <code>true</code>, the complete type hierarchy of the place is searched downwards. Otherwise,
	 *            only the direct children are checked.
	 * @return <code>true</code>, if the given {@link Type} instance is a child, <code>false</code> otherwise.
	 */
	public boolean hasChild(final Type childType, final boolean recursive) {
		for (final Type child : childTypes) {
			if (child.equals(childType)) {
				return true;
			}
			else if (recursive && child.hasChild(childType, true)) {
				return true;
			}
		}

		return false;
	}

	/**
	 * Check, if the given {@link Type} instance is a direct child of this type entity.
	 *
	 * @param childType the {@link Type} instance that should be checked.
	 * @return <code>true</code>, if the given {@link Type} instance is a direct child, <code>false</code> otherwise.
	 */
	public boolean hasChild(final Type childType) {
		return hasChild(childType, false);
	}

	// --- PARENT TYPE ---

	public Type getParentType() {
		return parentType;
	}

	/**
	 * Set the parent {@link Type} instance.
	 * <p>
	 * <b>Note:</b> If not <code>null</code>, this type entity is automatically added as child to the passed
	 * {@link Type} instance. <br>
	 * <b>Note:</b> If the previous {@link Type} instance was not <code>null</code>, this type <b>IS</b> removed from
	 * its set of children.
	 *
	 * @param parentType the new {@link Type} instance.
	 */
	public void setParentType(final Type parentType) {
		if (this.parentType != parentType) {
			if (this.parentType != null) {
				this.parentType.removeChildType(this);
			}

			this.parentType = parentType;

			if (parentType != null) {
				parentType.addChildType(this);
			}
		}
	}

	/**
	 * Get all parents of this type entity in the type hierarchy (i.e., step up recursively).
	 *
	 * @return a set with all parents in the type hierarchy.
	 */
	public Set<Type> getAllParents() {
		final Set<Type> allParents = new HashSet<>();

		Type parent = parentType;
		while (parent != null) {
			allParents.add(parent);
			parent = parent.parentType;
		}

		return allParents;
	}

	/**
	 * Check if this type entity is a child of the given {@link Type} instance wrt. to the type hierarchy (i.e., step up
	 * recursively).
	 *
	 * @param parentType the {@link Type} instance that should be checked.
	 * @return <code>true</code>, if the given {@link Type} instance is a parent, <code>false</code> otherwise.
	 */
	public boolean isChildOf(final Type parentType) {
		if (this.parentType == null) {
			return false;
		}
		else if (this.parentType.equals(parentType)) {
			return true;
		}
		else {
			return this.parentType.isChildOf(parentType);
		}
	}

	// --- SIMILAR TYPES ---

	/**
	 * Get an unmodifiable view of similar types.
	 *
	 * @return the similar types.
	 */
	public Set<Type> getSimilarTypes() {
		return Collections.unmodifiableSet(similarTypes);
	}

	/**
	 * Check if the given type is registered as similar type.
	 *
	 * @param otherType the type to check.
	 * @return <code>true</code> if the type is known to be similar, <code>false</code> otherwise.
	 */
	public boolean isSimilarType(final Type otherType) {
		return this.similarTypes.contains(otherType);
	}

	/**
	 * Add the given {@link Type} instance as a similar type.
	 * <p>
	 * If it is already enlisted as similar type, nothing happens. Self references are ignored.
	 * <p>
	 * <b>Note:</b> this type entity is automatically added as similar type to the given {@link Type} instance.
	 *
	 * @param similarType the similar type to add. Must not be <code>null</code>.
	 */
	public void addSimilarType(final Type similarType) {
		Objects.requireNonNull(similarType);
		if (this != similarType && !isSimilarType(similarType)) {
			this.similarTypes.add(similarType);
			similarType.addSimilarType(this);
		}
	}

	/**
	 * Add the given set of {@link Type} instances as similar.
	 * <p>
	 * Each entry is passed to {@link #addSimilarType}.
	 *
	 * @param similarTypes set of types to add as similar types. Must not be <code>null</code>.
	 */
	public void addSimilarTypes(final Set<Type> similarTypes) {
		Objects.requireNonNull(similarTypes);
		similarTypes.forEach(similarType -> addSimilarType(similarType));
	}

	/**
	 * Remove the given {@link Type} instance from the set of similar types.
	 * <p>
	 * <b>Note:</b> this type is automatically removed as similar type from the given {@link Type} instance.
	 *
	 * @param similarType the {@link Type} instance to remove from the set of similar types. Must not be
	 *            <code>null</code>.
	 */
	public void removeSimilarType(final Type similarType) {
		Objects.requireNonNull(similarType);
		this.similarTypes.remove(similarType);
		similarType.similarTypes.remove(this);
	}

	/**
	 * Change the set of similar types to the given set of {@link Type} instances.
	 * <p>
	 * If the given set is <code>null</code>, it is interpreted as empty list for consistency. <br>
	 * <b>Note:</b> Existing similar-relationships are removed beforehand.
	 *
	 * @param similarTypes the new set of similar children. If <code>null</code>, handled like an empty set.
	 */
	public void setSimilarTypes(final Set<Type> similarTypes) {
		if (!this.similarTypes.equals(similarTypes)) { // not comparing with != since we never expose the original set!
			this.similarTypes.forEach(similarType -> similarType.similarTypes.remove(this));
			this.similarTypes.clear();

			if (similarTypes != null) {
				addSimilarTypes(similarTypes);
			}
		}
	}

	private UUID getUUID() {
		if (uuid == null) {
			if (id == null) {
				uuid = UUID.randomUUID();
			}
			else {
				uuid = new UUID(id, id);
			}
		}
		return uuid;
	}

	@Override
	public int hashCode() {
		// we use getLeastSignificantBits() to decrease the chance for collisions (a UUID has 128 bit and an int 32
		// bit). Therefore, the likelihood for consecutive ids to collide is considerable
		return Long.hashCode(getUUID().getLeastSignificantBits());
	}

	@Override
	public boolean equals(final Object obj) {
		if (this == obj) {
			return true;
		}
		if (obj == null) {
			return false;
		}
		// deal with Hibernate Proxies; see http://stackoverflow.com/a/23268438
		if (!Hibernate.getClass(this).equals(Hibernate.getClass(obj))) {
			return false;
		}
		// see comment on uuid-field...
		return getUUID().equals(((Type) obj).getUUID());
	}

	@Override
	public String toString() {
		return "Type [id=" + id + ", name=" + name + ", description=" + description + ", childTypes="
				+ typeSetToString(childTypes) + ", parentType="
				+ ((parentType != null) ? typeToShortString(parentType) : "null") + ", similarTypes="
				+ typeSetToString(similarTypes) + "]";
	}

	private static String typeSetToString(final Set<Type> typeSet) {
		final StringBuilder stringBuilder = new StringBuilder();

		stringBuilder.append("(");

		for (final Type type : typeSet) {
			stringBuilder.append(typeToShortString(type) + ",");
		}

		if (stringBuilder.length() > 1) {
			stringBuilder.replace(stringBuilder.length() - 1, stringBuilder.length() - 1, ")");
		}
		else {
			stringBuilder.append(")");
		}

		return stringBuilder.toString();
	}

	private static String typeToShortString(final Type type) {
		return "[id=" + type.getId() + ", name=" + type.getName() + "]";
	}

}
