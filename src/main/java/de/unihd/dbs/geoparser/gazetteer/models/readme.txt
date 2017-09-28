============================================================================
| YOU SHOULD READ ALL OF THIS BEFORE YOU TOUCH THE MODEL IMPLEMENTATION!!! |
============================================================================

!!!!
Details on the data model and gazetteer implementation can be found in Section 3.2 and 4.1. in the Master Thesis by 
Ludwig Richter
!!!!

General:
- we use the Annotation-style on fields for JPA Mapping
- we provide a default constructor for each annotated class as well as getters and setters for all annotated fields to 
  comply with JPA requirements
- we name all foreign key constraints to make them more readable in the schema
- in some cases, we assume that we are working with Hibernate and PostgreSQL!

AbstractEntity:
- currently, we use InheritanceType.JOINED as @Inheritance strategy
  => a downside is that bulk insertion is slow, since no real batch-insertion is supported by Hibernate (see comments in
  /GeoParser/src/main/java/org/unihd/dbs/geoparser/gazetteer/util/GazetteerPersistenceManager.java)
  => another downside is that automatic, superfluous joins are used in subqueries used by JPA Criteria that may decrease
     query performance - especially since Indexes might not be used if LIMIT is used!
  => using InheritanceType.TABLE_PER_CLASS is problematic, since it is not JPA vendor independent and the modeling with
  provenance is not clean anymore

String-Fields:
- we map string-fields to TEXT-Datatype assuming that we work with PostgreSQL!
- HibernateSearch might come in handy for efficient queries?
 * http://stackoverflow.com/questions/11249635/finding-similar-strings-with-postgresql-quickly/11250001#11250001
 * http://hibernate.org/search/documentation/ 
 * http://rachbelaid.com/postgres-full-text-search-is-good-enough/
 => for now Postgres-functionality should be sufficient enough

DateRange:
- there is a Postgres datatype for daterange but it was too complicated to write a wrapper for Hibernate. Also, it
  wouldn't be platform independent and having two dates defining the range should be good enough anyway...
  
Footprint data type:
- we use Geometry as a data type, since it is supported by Hibernate and is mapped to PostGIS Geometry type
- using Geography might provide better performance for distance calculations; may be use a (materialized) view?
  however, it is not supported directly by Hibernate...
- index-generation must be done manually (see GazetteerPersistenceManager)
- HibernateSpatial might come in handy for efficient queries?

Ids:
- we use sequence-generators for id-fields to automatically set the ID when persisting a new entity to the database
  --> we use separate sequences for different tables to keep the IDs a bit more linear within a table
  --> we don't use an increment generator due to performance and usage issues
  --> DO NOT SET AUTOMATICALLY GENERATED ID FIELDS MANUALLY! SEE BELOW...
- setters for Id-Fields are made protected: http://solutiondesign.com/blog/-/blogs/protecting-your-hibernate-i-1/
  --> you should not change the Id-Field manually!

OneToMany-Relations:
- if an entity contains a collection of other entities, the collection is ensured to be always non-null. 
- if setting a collection to "null" with a setter, the collection will actually only be emptied and no values inserted

OneToMany-Relations acting as Composition:
- if adding an entity to a collection that mimics a composition, the entity will be automatically removed from the 
  previously owning collection, if such a collection exists
- if an entity holding other entities via composition is deleted, it should be ensured that the associated entities
  are also deleted. We do so using the Cascade-Mechanism
- it is possible to also enable such a feature at database level for PostgreSQL, BUT it does not seem to work correctly 
  for our setup (ids of deleted footprints remained in the entity-table):
  // if Hibernate creates the Database Table Schema, let it add ON DELETE CASCADE to the Foreign Key Constraint
  // (Note: does not work for all DBS, here we assume PostgreSQL)
  @OnDelete(action = OnDeleteAction.CASCADE)
- there is a bug that might cause problems: https://hibernate.atlassian.net/browse/HHH-10123
    --> "Enabling orphanRemoval on oneToMany relationship causes constraint violation"
    
OneToOne-Relations:
- the owning side of the relation must not be null when inserting it to the database. For convenience, it is allowed to
  set it to null in Java, but it will raise an exception in Hibernate if it is not set when persisting the entity
- Known Bug with orphan-removal and optional=false and setting to null: https://hibernate.atlassian.net/browse/HHH-6484
  possible solution: make it a onetomany: https://hibernate.atlassian.net/browse/HHH-5559
- http://stackoverflow.com/questions/31470414/orphan-removal-does-not-work-for-onetoone-relations
  
Type Model:
- if a type is removed, it is removed from the type-hierarchy and similar-type relations are removed, but it does not 
  cascade the delete!
  
Provenance, Footprint, PlaceName, PlaceProperty, PlaceRelationship, PlaceTypeAssignment:
- these are all components of AbstractEntity or Place and are therefore removed automatically by Hibernate if not
  used anymore (orphan-removal). Also, deletion of a AbstractEntity or Place cascades deletion to these entities
  
EqualsTo / hashCode: http://stackoverflow.com/questions/5031614/the-jpa-hashcode-equals-dilemma
- currently we implement a transient usage of a UUID