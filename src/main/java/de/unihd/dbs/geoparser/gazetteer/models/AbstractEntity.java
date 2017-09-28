package de.unihd.dbs.geoparser.gazetteer.models;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

import javax.persistence.CascadeType;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.Inheritance;
import javax.persistence.InheritanceType;
import javax.persistence.OneToMany;
import javax.persistence.SequenceGenerator;
import javax.persistence.Table;
import javax.persistence.Temporal;
import javax.persistence.TemporalType;
import javax.persistence.Transient;

import org.hibernate.Hibernate;
import de.unihd.dbs.geoparser.gazetteer.util.GazetteerPersistenceManager;

/**
 * This is the abstract base class of the gazetteer data model. It is the superclass for any features that describe a
 * place. The only classes that do not inherit from {@link AbstractEntity} are {@link Provenance} as well as
 * {@link Type} and its descendants.
 * <p>
 * Each {@link AbstractEntity} instance has a unique Id. The Id is assigned during JPA persistence with the help of a
 * sequence generator. Note that the Id-sequence may have holes (due to pre-allocation of multiple sequence numbers for
 * performance reasons). Before the initial persistence of an entity, the Id is set to <code>null</code>. The Id should
 * not be manually changed, since JPA takes care of that and will get confused if you do so... Furthermore, each
 * instance may optionally have a valid time and an associated {@link Provenance} instance.
 * <p>
 * <b>Important:</b> Every inheriting class must call one of {@link AbstractEntity}'s constructors for correct
 * initialization!
 * <p>
 * <b>Note:</b> Due to technical reasons, {@link #equals} and {@link #hashCode} only work correctly within a persistence
 * session, if the same entity is not loaded from the JPA entity manager after it has been initially persisted!
 *
 * @author lrichter
 */
@Entity
@Inheritance(strategy = InheritanceType.JOINED)
@Table(name = "entity")
@SequenceGenerator(name = "entity_id_sequence", sequenceName = "entity_id_sequence", initialValue = 1,
		allocationSize = GazetteerPersistenceManager.SEQUENCE_ALLOCATION_SIZE)
public abstract class AbstractEntity {

	/**
	 * A helper class to store valid time information.
	 * <p>
	 * A valid time consists of a {@link #startDate} and {@link #endDate} value, both of which may be <code>null</code>.
	 *
	 * @author lrichter
	 */
	public static class ValidTime {
		public Calendar startDate;
		public Calendar endDate;

		public ValidTime(final Calendar startDate, final Calendar endDate) {
			this.startDate = startDate;
			this.endDate = endDate;
		}
	}

	@Id
	@GeneratedValue(generator = "entity_id_sequence", strategy = GenerationType.SEQUENCE)
	private Long id;

	// we need a business-id to correctly implement hashCode() and equalsTo()! http://stackoverflow.com/a/5103360
	@Transient
	private UUID uuid;
	// XXX: currently, we don't persist the UUID for simplicity and storage size reasons. Not sure if this is sufficient
	// to
	// make hashCode() and equalsTo() work correctly (same issue with classes Provenance and Type); the current solution
	// should work as long as the same entity is not loaded multiple times from the EntityManager (e.g., creating it,
	// persisting it, detaching it and then reloading it into another variable will fail on equals()!)
	// @Column(columnDefinition="text")
	// private UUID uuid = UUID.randomUUID();

	@Column(name = "valid_time_start")
	@Temporal(TemporalType.DATE)
	private Calendar validTimeStartDate;

	@Column(name = "valid_time_end")
	@Temporal(TemporalType.DATE)
	private Calendar validTimeEndDate;

	// made this a OneToMany to circumvent bug with orphanRemoval = true, optional = false. See comments in Provenance
	// class.
	// we only allow a single associated instance anyway...
	@OneToMany(cascade = { CascadeType.ALL }, fetch = FetchType.LAZY, mappedBy = "entity", orphanRemoval = true)
	private Set<Provenance> provenance;

	/**
	 * Create an instance of {@link AbstractEntity} with no assigned information.
	 */
	protected AbstractEntity() {
		super();
		init();
	}

	/**
	 * Create an instance of {@link AbstractEntity} with the given parameters.
	 *
	 * @param validTime an associated {@link ValidTime} instance.
	 * @param provenance an associated {@link Provenance} instance.
	 */
	protected AbstractEntity(final ValidTime validTime, final Provenance provenance) {
		this();
		setProvenance(provenance);
		setValidTime(validTime);
	}

	private void init() {
		provenance = new HashSet<>();
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
	 * @param id the new entity id.
	 */
	protected void setId(final Long id) {
		this.id = id;
	}

	public Calendar getValidTimeStartDate() {
		return validTimeStartDate;
	}

	public void setValidTimeStartDate(final Calendar validTimeStartDate) {
		this.validTimeStartDate = validTimeStartDate;
	}

	public Calendar getValidTimeEndDate() {
		return validTimeEndDate;
	}

	public void setValidTimeEndDate(final Calendar validTimeEndDate) {
		this.validTimeEndDate = validTimeEndDate;
	}

	public ValidTime getValidTime() {
		return new ValidTime(getValidTimeStartDate(), getValidTimeEndDate());
	}

	public void setValidTime(final ValidTime validTime) {
		if (validTime != null) {
			setValidTimeStartDate(validTime.startDate);
			setValidTimeEndDate(validTime.endDate);
		}
		else {
			setValidTimeStartDate(null);
			setValidTimeEndDate(null);
		}
	}

	public Provenance getProvenance() {
		if (provenance.isEmpty()) {
			return null;
		}
		else {
			return provenance.iterator().next();
		}
	}

	/**
	 * Set the {@link Provenance} instance that is related to this entity.
	 * <p>
	 * <b>Note:</b> If not <code>null</code>, the passed {@link Provenance} instance is automatically adjusted such that
	 * it links to this entity.<br>
	 * <b>Note:</b> If the previous {@link Provenance} instance was not <code>null</code>, its link to this entity is
	 * <b>NOT</b> removed, since our JPA data model requires it to be non-null. Since orphan-removal is activated for
	 * this relationship, the previous {@link Provenance} instance is automatically removed from the persistence store
	 * by the JPA provider. However, this currently does not work, if it is set to <code>null</code> from an existing
	 * {@link Provenance} instance. Use {@link GazetteerPersistenceManager#removeProvenanceFromEntity} instead.
	 *
	 * @param provenance the new {@link Provenance} instance this entity is associated with.
	 */
	public void setProvenance(final Provenance provenance) {
		if (provenance == null) {
			this.provenance.clear();
		}
		else if (!this.provenance.contains(provenance)) {
			this.provenance.clear();
			this.provenance.add(provenance);
			provenance.setEntity(this);
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
		// bit). Therefore, the likelihood for consecutive ids to collide is considerable (had a problem with a
		// artificial test due to this issue)
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
		return getUUID().equals(((AbstractEntity) obj).getUUID());
	}

	@Override
	public String toString() {
		final SimpleDateFormat formater = new SimpleDateFormat("yyyy/MM/dd");
		final String validTimeStartDateStr = validTimeStartDate != null ? formater.format(validTimeStartDate.getTime())
				: "null";
		final String validTimeEndDateStr = validTimeEndDate != null ? formater.format(validTimeEndDate.getTime())
				: "null";
		return "AbstractEntity [id=" + id + ", validTime=[" + validTimeStartDateStr + ", " + validTimeEndDateStr
				+ "], provenance=" + provenance + "]";
	}

}
