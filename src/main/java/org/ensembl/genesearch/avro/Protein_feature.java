/**
 * Autogenerated by Avro
 * 
 * DO NOT EDIT DIRECTLY
 */
package org.ensembl.genesearch.avro;  
@SuppressWarnings("all")
@org.apache.avro.specific.AvroGenerated
public class Protein_feature extends org.apache.avro.specific.SpecificRecordBase implements org.apache.avro.specific.SpecificRecord {
  public static final org.apache.avro.Schema SCHEMA$ = new org.apache.avro.Schema.Parser().parse("{\"type\":\"record\",\"name\":\"Protein_feature\",\"namespace\":\"org.ensembl.genesearch.avro\",\"fields\":[{\"name\":\"start\",\"type\":\"int\"},{\"name\":\"end\",\"type\":\"int\"},{\"name\":\"dbname\",\"type\":\"string\"},{\"name\":\"interpro_ac\",\"type\":\"string\"},{\"name\":\"description\",\"type\":\"string\"},{\"name\":\"name\",\"type\":\"string\"}]}");
  public static org.apache.avro.Schema getClassSchema() { return SCHEMA$; }
  @Deprecated public int start;
  @Deprecated public int end;
  @Deprecated public java.lang.CharSequence dbname;
  @Deprecated public java.lang.CharSequence interpro_ac;
  @Deprecated public java.lang.CharSequence description;
  @Deprecated public java.lang.CharSequence name;

  /**
   * Default constructor.  Note that this does not initialize fields
   * to their default values from the schema.  If that is desired then
   * one should use <code>newBuilder()</code>. 
   */
  public Protein_feature() {}

  /**
   * All-args constructor.
   */
  public Protein_feature(java.lang.Integer start, java.lang.Integer end, java.lang.CharSequence dbname, java.lang.CharSequence interpro_ac, java.lang.CharSequence description, java.lang.CharSequence name) {
    this.start = start;
    this.end = end;
    this.dbname = dbname;
    this.interpro_ac = interpro_ac;
    this.description = description;
    this.name = name;
  }

  public org.apache.avro.Schema getSchema() { return SCHEMA$; }
  // Used by DatumWriter.  Applications should not call. 
  public java.lang.Object get(int field$) {
    switch (field$) {
    case 0: return start;
    case 1: return end;
    case 2: return dbname;
    case 3: return interpro_ac;
    case 4: return description;
    case 5: return name;
    default: throw new org.apache.avro.AvroRuntimeException("Bad index");
    }
  }
  // Used by DatumReader.  Applications should not call. 
  @SuppressWarnings(value="unchecked")
  public void put(int field$, java.lang.Object value$) {
    switch (field$) {
    case 0: start = (java.lang.Integer)value$; break;
    case 1: end = (java.lang.Integer)value$; break;
    case 2: dbname = (java.lang.CharSequence)value$; break;
    case 3: interpro_ac = (java.lang.CharSequence)value$; break;
    case 4: description = (java.lang.CharSequence)value$; break;
    case 5: name = (java.lang.CharSequence)value$; break;
    default: throw new org.apache.avro.AvroRuntimeException("Bad index");
    }
  }

  /**
   * Gets the value of the 'start' field.
   */
  public java.lang.Integer getStart() {
    return start;
  }

  /**
   * Sets the value of the 'start' field.
   * @param value the value to set.
   */
  public void setStart(java.lang.Integer value) {
    this.start = value;
  }

  /**
   * Gets the value of the 'end' field.
   */
  public java.lang.Integer getEnd() {
    return end;
  }

  /**
   * Sets the value of the 'end' field.
   * @param value the value to set.
   */
  public void setEnd(java.lang.Integer value) {
    this.end = value;
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
   * Gets the value of the 'interpro_ac' field.
   */
  public java.lang.CharSequence getInterproAc() {
    return interpro_ac;
  }

  /**
   * Sets the value of the 'interpro_ac' field.
   * @param value the value to set.
   */
  public void setInterproAc(java.lang.CharSequence value) {
    this.interpro_ac = value;
  }

  /**
   * Gets the value of the 'description' field.
   */
  public java.lang.CharSequence getDescription() {
    return description;
  }

  /**
   * Sets the value of the 'description' field.
   * @param value the value to set.
   */
  public void setDescription(java.lang.CharSequence value) {
    this.description = value;
  }

  /**
   * Gets the value of the 'name' field.
   */
  public java.lang.CharSequence getName() {
    return name;
  }

  /**
   * Sets the value of the 'name' field.
   * @param value the value to set.
   */
  public void setName(java.lang.CharSequence value) {
    this.name = value;
  }

  /** Creates a new Protein_feature RecordBuilder */
  public static org.ensembl.genesearch.avro.Protein_feature.Builder newBuilder() {
    return new org.ensembl.genesearch.avro.Protein_feature.Builder();
  }
  
  /** Creates a new Protein_feature RecordBuilder by copying an existing Builder */
  public static org.ensembl.genesearch.avro.Protein_feature.Builder newBuilder(org.ensembl.genesearch.avro.Protein_feature.Builder other) {
    return new org.ensembl.genesearch.avro.Protein_feature.Builder(other);
  }
  
  /** Creates a new Protein_feature RecordBuilder by copying an existing Protein_feature instance */
  public static org.ensembl.genesearch.avro.Protein_feature.Builder newBuilder(org.ensembl.genesearch.avro.Protein_feature other) {
    return new org.ensembl.genesearch.avro.Protein_feature.Builder(other);
  }
  
  /**
   * RecordBuilder for Protein_feature instances.
   */
  public static class Builder extends org.apache.avro.specific.SpecificRecordBuilderBase<Protein_feature>
    implements org.apache.avro.data.RecordBuilder<Protein_feature> {

    private int start;
    private int end;
    private java.lang.CharSequence dbname;
    private java.lang.CharSequence interpro_ac;
    private java.lang.CharSequence description;
    private java.lang.CharSequence name;

    /** Creates a new Builder */
    private Builder() {
      super(org.ensembl.genesearch.avro.Protein_feature.SCHEMA$);
    }
    
    /** Creates a Builder by copying an existing Builder */
    private Builder(org.ensembl.genesearch.avro.Protein_feature.Builder other) {
      super(other);
      if (isValidValue(fields()[0], other.start)) {
        this.start = data().deepCopy(fields()[0].schema(), other.start);
        fieldSetFlags()[0] = true;
      }
      if (isValidValue(fields()[1], other.end)) {
        this.end = data().deepCopy(fields()[1].schema(), other.end);
        fieldSetFlags()[1] = true;
      }
      if (isValidValue(fields()[2], other.dbname)) {
        this.dbname = data().deepCopy(fields()[2].schema(), other.dbname);
        fieldSetFlags()[2] = true;
      }
      if (isValidValue(fields()[3], other.interpro_ac)) {
        this.interpro_ac = data().deepCopy(fields()[3].schema(), other.interpro_ac);
        fieldSetFlags()[3] = true;
      }
      if (isValidValue(fields()[4], other.description)) {
        this.description = data().deepCopy(fields()[4].schema(), other.description);
        fieldSetFlags()[4] = true;
      }
      if (isValidValue(fields()[5], other.name)) {
        this.name = data().deepCopy(fields()[5].schema(), other.name);
        fieldSetFlags()[5] = true;
      }
    }
    
    /** Creates a Builder by copying an existing Protein_feature instance */
    private Builder(org.ensembl.genesearch.avro.Protein_feature other) {
            super(org.ensembl.genesearch.avro.Protein_feature.SCHEMA$);
      if (isValidValue(fields()[0], other.start)) {
        this.start = data().deepCopy(fields()[0].schema(), other.start);
        fieldSetFlags()[0] = true;
      }
      if (isValidValue(fields()[1], other.end)) {
        this.end = data().deepCopy(fields()[1].schema(), other.end);
        fieldSetFlags()[1] = true;
      }
      if (isValidValue(fields()[2], other.dbname)) {
        this.dbname = data().deepCopy(fields()[2].schema(), other.dbname);
        fieldSetFlags()[2] = true;
      }
      if (isValidValue(fields()[3], other.interpro_ac)) {
        this.interpro_ac = data().deepCopy(fields()[3].schema(), other.interpro_ac);
        fieldSetFlags()[3] = true;
      }
      if (isValidValue(fields()[4], other.description)) {
        this.description = data().deepCopy(fields()[4].schema(), other.description);
        fieldSetFlags()[4] = true;
      }
      if (isValidValue(fields()[5], other.name)) {
        this.name = data().deepCopy(fields()[5].schema(), other.name);
        fieldSetFlags()[5] = true;
      }
    }

    /** Gets the value of the 'start' field */
    public java.lang.Integer getStart() {
      return start;
    }
    
    /** Sets the value of the 'start' field */
    public org.ensembl.genesearch.avro.Protein_feature.Builder setStart(int value) {
      validate(fields()[0], value);
      this.start = value;
      fieldSetFlags()[0] = true;
      return this; 
    }
    
    /** Checks whether the 'start' field has been set */
    public boolean hasStart() {
      return fieldSetFlags()[0];
    }
    
    /** Clears the value of the 'start' field */
    public org.ensembl.genesearch.avro.Protein_feature.Builder clearStart() {
      fieldSetFlags()[0] = false;
      return this;
    }

    /** Gets the value of the 'end' field */
    public java.lang.Integer getEnd() {
      return end;
    }
    
    /** Sets the value of the 'end' field */
    public org.ensembl.genesearch.avro.Protein_feature.Builder setEnd(int value) {
      validate(fields()[1], value);
      this.end = value;
      fieldSetFlags()[1] = true;
      return this; 
    }
    
    /** Checks whether the 'end' field has been set */
    public boolean hasEnd() {
      return fieldSetFlags()[1];
    }
    
    /** Clears the value of the 'end' field */
    public org.ensembl.genesearch.avro.Protein_feature.Builder clearEnd() {
      fieldSetFlags()[1] = false;
      return this;
    }

    /** Gets the value of the 'dbname' field */
    public java.lang.CharSequence getDbname() {
      return dbname;
    }
    
    /** Sets the value of the 'dbname' field */
    public org.ensembl.genesearch.avro.Protein_feature.Builder setDbname(java.lang.CharSequence value) {
      validate(fields()[2], value);
      this.dbname = value;
      fieldSetFlags()[2] = true;
      return this; 
    }
    
    /** Checks whether the 'dbname' field has been set */
    public boolean hasDbname() {
      return fieldSetFlags()[2];
    }
    
    /** Clears the value of the 'dbname' field */
    public org.ensembl.genesearch.avro.Protein_feature.Builder clearDbname() {
      dbname = null;
      fieldSetFlags()[2] = false;
      return this;
    }

    /** Gets the value of the 'interpro_ac' field */
    public java.lang.CharSequence getInterproAc() {
      return interpro_ac;
    }
    
    /** Sets the value of the 'interpro_ac' field */
    public org.ensembl.genesearch.avro.Protein_feature.Builder setInterproAc(java.lang.CharSequence value) {
      validate(fields()[3], value);
      this.interpro_ac = value;
      fieldSetFlags()[3] = true;
      return this; 
    }
    
    /** Checks whether the 'interpro_ac' field has been set */
    public boolean hasInterproAc() {
      return fieldSetFlags()[3];
    }
    
    /** Clears the value of the 'interpro_ac' field */
    public org.ensembl.genesearch.avro.Protein_feature.Builder clearInterproAc() {
      interpro_ac = null;
      fieldSetFlags()[3] = false;
      return this;
    }

    /** Gets the value of the 'description' field */
    public java.lang.CharSequence getDescription() {
      return description;
    }
    
    /** Sets the value of the 'description' field */
    public org.ensembl.genesearch.avro.Protein_feature.Builder setDescription(java.lang.CharSequence value) {
      validate(fields()[4], value);
      this.description = value;
      fieldSetFlags()[4] = true;
      return this; 
    }
    
    /** Checks whether the 'description' field has been set */
    public boolean hasDescription() {
      return fieldSetFlags()[4];
    }
    
    /** Clears the value of the 'description' field */
    public org.ensembl.genesearch.avro.Protein_feature.Builder clearDescription() {
      description = null;
      fieldSetFlags()[4] = false;
      return this;
    }

    /** Gets the value of the 'name' field */
    public java.lang.CharSequence getName() {
      return name;
    }
    
    /** Sets the value of the 'name' field */
    public org.ensembl.genesearch.avro.Protein_feature.Builder setName(java.lang.CharSequence value) {
      validate(fields()[5], value);
      this.name = value;
      fieldSetFlags()[5] = true;
      return this; 
    }
    
    /** Checks whether the 'name' field has been set */
    public boolean hasName() {
      return fieldSetFlags()[5];
    }
    
    /** Clears the value of the 'name' field */
    public org.ensembl.genesearch.avro.Protein_feature.Builder clearName() {
      name = null;
      fieldSetFlags()[5] = false;
      return this;
    }

    @Override
    public Protein_feature build() {
      try {
        Protein_feature record = new Protein_feature();
        record.start = fieldSetFlags()[0] ? this.start : (java.lang.Integer) defaultValue(fields()[0]);
        record.end = fieldSetFlags()[1] ? this.end : (java.lang.Integer) defaultValue(fields()[1]);
        record.dbname = fieldSetFlags()[2] ? this.dbname : (java.lang.CharSequence) defaultValue(fields()[2]);
        record.interpro_ac = fieldSetFlags()[3] ? this.interpro_ac : (java.lang.CharSequence) defaultValue(fields()[3]);
        record.description = fieldSetFlags()[4] ? this.description : (java.lang.CharSequence) defaultValue(fields()[4]);
        record.name = fieldSetFlags()[5] ? this.name : (java.lang.CharSequence) defaultValue(fields()[5]);
        return record;
      } catch (Exception e) {
        throw new org.apache.avro.AvroRuntimeException(e);
      }
    }
  }
}