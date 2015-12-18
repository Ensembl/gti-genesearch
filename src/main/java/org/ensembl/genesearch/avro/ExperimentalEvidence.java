/**
 * Autogenerated by Avro
 * 
 * DO NOT EDIT DIRECTLY
 */
package org.ensembl.genesearch.avro;  
@SuppressWarnings("all")
@org.apache.avro.specific.AvroGenerated
public class ExperimentalEvidence extends org.apache.avro.specific.SpecificRecordBase implements org.apache.avro.specific.SpecificRecord {
  public static final org.apache.avro.Schema SCHEMA$ = new org.apache.avro.Schema.Parser().parse("{\"type\":\"record\",\"name\":\"ExperimentalEvidence\",\"namespace\":\"org.ensembl.genesearch.avro\",\"fields\":[{\"name\":\"primary_id\",\"type\":\"string\"},{\"name\":\"dbname\",\"type\":\"string\"},{\"name\":\"display_id\",\"type\":\"string\"},{\"name\":\"source\",\"type\":{\"type\":\"array\",\"items\":{\"type\":\"record\",\"name\":\"Source\",\"fields\":[{\"name\":\"primary_id\",\"type\":\"string\"},{\"name\":\"dbname\",\"type\":\"string\"},{\"name\":\"display_id\",\"type\":\"string\"}]}}},{\"name\":\"rank\",\"type\":\"int\"}]}");
  public static org.apache.avro.Schema getClassSchema() { return SCHEMA$; }
  @Deprecated public java.lang.CharSequence primary_id;
  @Deprecated public java.lang.CharSequence dbname;
  @Deprecated public java.lang.CharSequence display_id;
  @Deprecated public java.util.List<org.ensembl.genesearch.avro.Source> source;
  @Deprecated public int rank;

  /**
   * Default constructor.  Note that this does not initialize fields
   * to their default values from the schema.  If that is desired then
   * one should use <code>newBuilder()</code>. 
   */
  public ExperimentalEvidence() {}

  /**
   * All-args constructor.
   */
  public ExperimentalEvidence(java.lang.CharSequence primary_id, java.lang.CharSequence dbname, java.lang.CharSequence display_id, java.util.List<org.ensembl.genesearch.avro.Source> source, java.lang.Integer rank) {
    this.primary_id = primary_id;
    this.dbname = dbname;
    this.display_id = display_id;
    this.source = source;
    this.rank = rank;
  }

  public org.apache.avro.Schema getSchema() { return SCHEMA$; }
  // Used by DatumWriter.  Applications should not call. 
  public java.lang.Object get(int field$) {
    switch (field$) {
    case 0: return primary_id;
    case 1: return dbname;
    case 2: return display_id;
    case 3: return source;
    case 4: return rank;
    default: throw new org.apache.avro.AvroRuntimeException("Bad index");
    }
  }
  // Used by DatumReader.  Applications should not call. 
  @SuppressWarnings(value="unchecked")
  public void put(int field$, java.lang.Object value$) {
    switch (field$) {
    case 0: primary_id = (java.lang.CharSequence)value$; break;
    case 1: dbname = (java.lang.CharSequence)value$; break;
    case 2: display_id = (java.lang.CharSequence)value$; break;
    case 3: source = (java.util.List<org.ensembl.genesearch.avro.Source>)value$; break;
    case 4: rank = (java.lang.Integer)value$; break;
    default: throw new org.apache.avro.AvroRuntimeException("Bad index");
    }
  }

  /**
   * Gets the value of the 'primary_id' field.
   */
  public java.lang.CharSequence getPrimaryId() {
    return primary_id;
  }

  /**
   * Sets the value of the 'primary_id' field.
   * @param value the value to set.
   */
  public void setPrimaryId(java.lang.CharSequence value) {
    this.primary_id = value;
  }

  /**
   * Gets the value of the 'dbname' field.
   */
  public java.lang.CharSequence getDbname() {
    return dbname;
  }

  /**
   * Sets the value of the 'dbname' field.
   * @param value the value to set.
   */
  public void setDbname(java.lang.CharSequence value) {
    this.dbname = value;
  }

  /**
   * Gets the value of the 'display_id' field.
   */
  public java.lang.CharSequence getDisplayId() {
    return display_id;
  }

  /**
   * Sets the value of the 'display_id' field.
   * @param value the value to set.
   */
  public void setDisplayId(java.lang.CharSequence value) {
    this.display_id = value;
  }

  /**
   * Gets the value of the 'source' field.
   */
  public java.util.List<org.ensembl.genesearch.avro.Source> getSource() {
    return source;
  }

  /**
   * Sets the value of the 'source' field.
   * @param value the value to set.
   */
  public void setSource(java.util.List<org.ensembl.genesearch.avro.Source> value) {
    this.source = value;
  }

  /**
   * Gets the value of the 'rank' field.
   */
  public java.lang.Integer getRank() {
    return rank;
  }

  /**
   * Sets the value of the 'rank' field.
   * @param value the value to set.
   */
  public void setRank(java.lang.Integer value) {
    this.rank = value;
  }

  /** Creates a new ExperimentalEvidence RecordBuilder */
  public static org.ensembl.genesearch.avro.ExperimentalEvidence.Builder newBuilder() {
    return new org.ensembl.genesearch.avro.ExperimentalEvidence.Builder();
  }
  
  /** Creates a new ExperimentalEvidence RecordBuilder by copying an existing Builder */
  public static org.ensembl.genesearch.avro.ExperimentalEvidence.Builder newBuilder(org.ensembl.genesearch.avro.ExperimentalEvidence.Builder other) {
    return new org.ensembl.genesearch.avro.ExperimentalEvidence.Builder(other);
  }
  
  /** Creates a new ExperimentalEvidence RecordBuilder by copying an existing ExperimentalEvidence instance */
  public static org.ensembl.genesearch.avro.ExperimentalEvidence.Builder newBuilder(org.ensembl.genesearch.avro.ExperimentalEvidence other) {
    return new org.ensembl.genesearch.avro.ExperimentalEvidence.Builder(other);
  }
  
  /**
   * RecordBuilder for ExperimentalEvidence instances.
   */
  public static class Builder extends org.apache.avro.specific.SpecificRecordBuilderBase<ExperimentalEvidence>
    implements org.apache.avro.data.RecordBuilder<ExperimentalEvidence> {

    private java.lang.CharSequence primary_id;
    private java.lang.CharSequence dbname;
    private java.lang.CharSequence display_id;
    private java.util.List<org.ensembl.genesearch.avro.Source> source;
    private int rank;

    /** Creates a new Builder */
    private Builder() {
      super(org.ensembl.genesearch.avro.ExperimentalEvidence.SCHEMA$);
    }
    
    /** Creates a Builder by copying an existing Builder */
    private Builder(org.ensembl.genesearch.avro.ExperimentalEvidence.Builder other) {
      super(other);
      if (isValidValue(fields()[0], other.primary_id)) {
        this.primary_id = data().deepCopy(fields()[0].schema(), other.primary_id);
        fieldSetFlags()[0] = true;
      }
      if (isValidValue(fields()[1], other.dbname)) {
        this.dbname = data().deepCopy(fields()[1].schema(), other.dbname);
        fieldSetFlags()[1] = true;
      }
      if (isValidValue(fields()[2], other.display_id)) {
        this.display_id = data().deepCopy(fields()[2].schema(), other.display_id);
        fieldSetFlags()[2] = true;
      }
      if (isValidValue(fields()[3], other.source)) {
        this.source = data().deepCopy(fields()[3].schema(), other.source);
        fieldSetFlags()[3] = true;
      }
      if (isValidValue(fields()[4], other.rank)) {
        this.rank = data().deepCopy(fields()[4].schema(), other.rank);
        fieldSetFlags()[4] = true;
      }
    }
    
    /** Creates a Builder by copying an existing ExperimentalEvidence instance */
    private Builder(org.ensembl.genesearch.avro.ExperimentalEvidence other) {
            super(org.ensembl.genesearch.avro.ExperimentalEvidence.SCHEMA$);
      if (isValidValue(fields()[0], other.primary_id)) {
        this.primary_id = data().deepCopy(fields()[0].schema(), other.primary_id);
        fieldSetFlags()[0] = true;
      }
      if (isValidValue(fields()[1], other.dbname)) {
        this.dbname = data().deepCopy(fields()[1].schema(), other.dbname);
        fieldSetFlags()[1] = true;
      }
      if (isValidValue(fields()[2], other.display_id)) {
        this.display_id = data().deepCopy(fields()[2].schema(), other.display_id);
        fieldSetFlags()[2] = true;
      }
      if (isValidValue(fields()[3], other.source)) {
        this.source = data().deepCopy(fields()[3].schema(), other.source);
        fieldSetFlags()[3] = true;
      }
      if (isValidValue(fields()[4], other.rank)) {
        this.rank = data().deepCopy(fields()[4].schema(), other.rank);
        fieldSetFlags()[4] = true;
      }
    }

    /** Gets the value of the 'primary_id' field */
    public java.lang.CharSequence getPrimaryId() {
      return primary_id;
    }
    
    /** Sets the value of the 'primary_id' field */
    public org.ensembl.genesearch.avro.ExperimentalEvidence.Builder setPrimaryId(java.lang.CharSequence value) {
      validate(fields()[0], value);
      this.primary_id = value;
      fieldSetFlags()[0] = true;
      return this; 
    }
    
    /** Checks whether the 'primary_id' field has been set */
    public boolean hasPrimaryId() {
      return fieldSetFlags()[0];
    }
    
    /** Clears the value of the 'primary_id' field */
    public org.ensembl.genesearch.avro.ExperimentalEvidence.Builder clearPrimaryId() {
      primary_id = null;
      fieldSetFlags()[0] = false;
      return this;
    }

    /** Gets the value of the 'dbname' field */
    public java.lang.CharSequence getDbname() {
      return dbname;
    }
    
    /** Sets the value of the 'dbname' field */
    public org.ensembl.genesearch.avro.ExperimentalEvidence.Builder setDbname(java.lang.CharSequence value) {
      validate(fields()[1], value);
      this.dbname = value;
      fieldSetFlags()[1] = true;
      return this; 
    }
    
    /** Checks whether the 'dbname' field has been set */
    public boolean hasDbname() {
      return fieldSetFlags()[1];
    }
    
    /** Clears the value of the 'dbname' field */
    public org.ensembl.genesearch.avro.ExperimentalEvidence.Builder clearDbname() {
      dbname = null;
      fieldSetFlags()[1] = false;
      return this;
    }

    /** Gets the value of the 'display_id' field */
    public java.lang.CharSequence getDisplayId() {
      return display_id;
    }
    
    /** Sets the value of the 'display_id' field */
    public org.ensembl.genesearch.avro.ExperimentalEvidence.Builder setDisplayId(java.lang.CharSequence value) {
      validate(fields()[2], value);
      this.display_id = value;
      fieldSetFlags()[2] = true;
      return this; 
    }
    
    /** Checks whether the 'display_id' field has been set */
    public boolean hasDisplayId() {
      return fieldSetFlags()[2];
    }
    
    /** Clears the value of the 'display_id' field */
    public org.ensembl.genesearch.avro.ExperimentalEvidence.Builder clearDisplayId() {
      display_id = null;
      fieldSetFlags()[2] = false;
      return this;
    }

    /** Gets the value of the 'source' field */
    public java.util.List<org.ensembl.genesearch.avro.Source> getSource() {
      return source;
    }
    
    /** Sets the value of the 'source' field */
    public org.ensembl.genesearch.avro.ExperimentalEvidence.Builder setSource(java.util.List<org.ensembl.genesearch.avro.Source> value) {
      validate(fields()[3], value);
      this.source = value;
      fieldSetFlags()[3] = true;
      return this; 
    }
    
    /** Checks whether the 'source' field has been set */
    public boolean hasSource() {
      return fieldSetFlags()[3];
    }
    
    /** Clears the value of the 'source' field */
    public org.ensembl.genesearch.avro.ExperimentalEvidence.Builder clearSource() {
      source = null;
      fieldSetFlags()[3] = false;
      return this;
    }

    /** Gets the value of the 'rank' field */
    public java.lang.Integer getRank() {
      return rank;
    }
    
    /** Sets the value of the 'rank' field */
    public org.ensembl.genesearch.avro.ExperimentalEvidence.Builder setRank(int value) {
      validate(fields()[4], value);
      this.rank = value;
      fieldSetFlags()[4] = true;
      return this; 
    }
    
    /** Checks whether the 'rank' field has been set */
    public boolean hasRank() {
      return fieldSetFlags()[4];
    }
    
    /** Clears the value of the 'rank' field */
    public org.ensembl.genesearch.avro.ExperimentalEvidence.Builder clearRank() {
      fieldSetFlags()[4] = false;
      return this;
    }

    @Override
    public ExperimentalEvidence build() {
      try {
        ExperimentalEvidence record = new ExperimentalEvidence();
        record.primary_id = fieldSetFlags()[0] ? this.primary_id : (java.lang.CharSequence) defaultValue(fields()[0]);
        record.dbname = fieldSetFlags()[1] ? this.dbname : (java.lang.CharSequence) defaultValue(fields()[1]);
        record.display_id = fieldSetFlags()[2] ? this.display_id : (java.lang.CharSequence) defaultValue(fields()[2]);
        record.source = fieldSetFlags()[3] ? this.source : (java.util.List<org.ensembl.genesearch.avro.Source>) defaultValue(fields()[3]);
        record.rank = fieldSetFlags()[4] ? this.rank : (java.lang.Integer) defaultValue(fields()[4]);
        return record;
      } catch (Exception e) {
        throw new org.apache.avro.AvroRuntimeException(e);
      }
    }
  }
}
