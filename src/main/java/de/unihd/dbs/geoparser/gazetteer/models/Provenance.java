package de.unihd.dbs.geoparser.gazetteer.models;

import java.util.UUID;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.ForeignKey;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.SequenceGenerator;
import javax.persistence.Table;
import javax.persistence.Transient;

import org.hibernate.Hibernate;

/**
 * This class models the provenance information that can be assigned to any {@link AbstractEntity} instance.
 * <p>
 * Each {@link Provenance} instance has a unique Id. The Id is assigned during JPA persistence with the help of a
 * sequence generator. Note that the Id-sequence may have holes (due to pre-allocation of multiple sequence numbers for
 * performance reasons). Before the initial persistence of an entity, the Id is set to <code>null</code>. The Id should
 * not be manually changed, since JPA takes care of that and will get confused if you do so...<br>
 * Furthermore, each instance may have an optional field for storing an URI that links to the original data source and a
 * field to store the information about the aggregation tool that was used for data retrieval.
 * <p>
 * A {@link Provenance} instance <b>must</b> be linked to an {@link AbstractEntity} entity upon persistence!
 * <p>
 * <b>WARNING: </b> A known Bug with orphan-removal and optional=false causes trouble when deleting entities with
 * provenance! We need to set optional = false for performance reasons. If not, query speed would drop drastically!
 * Therefore, be aware that there are problems with orphan-removal of Provenance-entities. This may be also happen
 * indirectly if a complete entity should be deleted! See {@link ModelTest#testChangeFootprintsOwner} for example code
 * that would fail.
 * <p>
 * <b>Note:</b> Due to technical reasons, {@link #equals} and {@link #hashCode} only work correctly, if the same entity
 * is not loaded multiple times from the JPA entity manager, after it has been initially persisted!
 * 
 * @author lrichter
 */
@Entity
@Table(name = "provenance")
@SequenceGenerator(name = "provenance_id_sequence", sequenceName = "provenance_id_sequence", initialValue = 1,
		allocationSize = 1)
public class Provenance {

	@Id
	@GeneratedValue(generator = "provenance_id_sequence", strategy = GenerationType.SEQUENCE)
	@Column(name = "id")
	private Long id;

	@Transient
	private UUID uuid;

	@Column(name = "uri", columnDefinition = "text")
	private String uri;

	@Column(name = "aggregation_tool", columnDefinition = "text")
	private String aggregationTool;

	// XXX: known Bug with orphan-removal and optional=false (https://hibernate.atlassian.net/browse/HHH-6484) on
	// OneToOne causes trouble when deleting entities with provenance! Using ManyToOne currently works as workaround.
	// It cost me a lot of headache, so may be just accept this for now!
	// We need to set optional = false for performance reasons. If not, query speed would drop drastically!
	// XXX: Problem still arise indirectly, if a complete entity should be deleted!
	// See ModelTest#testChangeFootprintsOwner for example code that would fail.
	// A suggestion for onetomany: https:// hibernate.atlassian.net/browse/HHH-5559
	// Another report: http://stackoverflow.com/questions/31470414/orphan-removal-does-not-work-for-onetoone-relations
	@ManyToOne(fetch = FetchType.LAZY, optional = false)
	@JoinColumn(name = "entity_id", foreignKey = @ForeignKey(name = "provenance_entity_fk") )
	private AbstractEntity entity;

	/**
	 * Create an instance of {@link Provenance} with no assigned information.
	 */
	public Provenance() {
		super();
		this.uuid = null;
	}

	/**
	 * Create an instance of {@link Provenance} using the given parameters.
	 * 
	 * @param uri the URI linking to the original data source.
	 * @param aggregationTool the aggregation tool used for data retrieval.
	 * @param entity the {@link AbstractEntity} entity to which this entity is assigned to.
	 */
	public Provenance(final String uri, final String aggregationTool, final AbstractEntity entity) {
		this();
		setAggregationTool(aggregationTool);
		setEntity(entity);
		setUri(uri);
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

	public String getUri() {
		return uri;
	}

	public void setUri(final String uri) {
		this.uri = uri;
	}

	public String getAggregationTool() {
		return aggregationTool;
	}

	public void setAggregationTool(final String aggregationTool) {
		this.aggregationTool = aggregationTool;
	}

	public AbstractEntity getEntity() {
		return entity;
	}

	/**
	 * Set the {@link AbstractEntity} entity to which this provenance is related to.
	 * <p>
	 * <b>Note:</b> If not <code>null</code>, automatically adjusts the passed {@link AbstractEntity} instance such that
	 * it links to this provenance.<br>
	 * <b>Note:</b> If the previous {@link AbstractEntity} instance was not <code>null</code>, its link to this
	 * provenance <b>IS</b> removed.
	 * 
	 * @param entity the new {@link AbstractEntity} instance this entity is associated with.
	 */
	public void setEntity(final AbstractEntity entity) {
		if (this.entity != entity) {
			if (this.entity != null) {
				this.entity.setProvenance(null);
			}

			this.entity = entity;

			if (entity != null) {
				entity.setProvenance(this);
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
		return getUUID().equals(((Provenance) obj).getUUID());
	}

	@Override
	public String toString() {
		return "Provenance [id=" + id + ", uri=" + uri + ", aggregationTool=" + aggregationTool + ", entity_id="
				+ ((entity != null) ? entity.getId() : "null") + "]";
	}

}
