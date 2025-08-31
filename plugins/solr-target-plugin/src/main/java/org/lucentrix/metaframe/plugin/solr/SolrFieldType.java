package org.lucentrix.metaframe.plugin.solr;



import java.util.Objects;

public class SolrFieldType {

    public static final String INDEX_NULL_VALUE = Character.toString((char)0);

    private Boolean missingFirst;
    private Boolean missingLast;
    private SolrType fieldType;
    private String name;
    private String typeClass;
    private Boolean multivalued;

    public static final SolrFieldType NOT_EXISTING = new SolrFieldType(null);

    public SolrFieldType(String name) {
        this.name = name;
    }

    public SolrFieldType(String name, SolrType fieldType, String typeClass, Boolean multivalued,
                         Boolean missingFirst, Boolean missingLast)
    {
        this.missingFirst = missingFirst;
        this.missingLast = missingLast;
        this.fieldType = fieldType;
        this.name = name;
        this.typeClass = typeClass;
        this.multivalued = multivalued;
    }

    public boolean isMultivalued() {
        return multivalued == null ? false : multivalued;
    }

    public void setMultivalued(Boolean multivalued) {
        this.multivalued = multivalued;
    }

    public void setMissingFirst(Boolean missingFirst) {
        this.missingFirst = missingFirst;
    }

    public void setMissingLast(Boolean missingLast) {
        this.missingLast = missingLast;
    }

    public void setFieldType(SolrType fieldType) {
        this.fieldType = fieldType;
    }

    public void setName(String name) {
        this.name = name;
    }

    public void setTypeClass(String typeClass) {
        this.typeClass = typeClass;
    }

    public Boolean isMissingFirst() {
        return missingFirst;
    }

    public Boolean isMissingLast() {
        return missingLast;
    }

    public SolrType getType() {
        return fieldType;
    }

    public String getName() {
        return name;
    }

    public String getTypeClass() {
        return typeClass;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof SolrFieldType)) return false;
        SolrFieldType that = (SolrFieldType) o;
        return Objects.equals(missingFirst, that.missingFirst) && Objects.equals(missingLast, that.missingLast) && fieldType == that.fieldType && Objects.equals(name, that.name) && Objects.equals(typeClass, that.typeClass) && Objects.equals(multivalued, that.multivalued);
    }

    @Override
    public int hashCode() {
        return Objects.hash(missingFirst, missingLast, fieldType, name, typeClass, multivalued);
    }

    @Override
    public String toString() {
        return "IndexFieldType{" +
                "missingFirst=" + missingFirst +
                ", missingLast=" + missingLast +
                ", fieldType=" + fieldType +
                ", name='" + name + '\'' +
                ", typeClass='" + typeClass + '\'' +
                ", multivalued=" + multivalued +
                ", type=" + getType() +
                '}';
    }

    public static class Builder {
        private String name;
        private Boolean missingFirst;
        private Boolean missingLast;
        private SolrType fieldType;
        private String typeClass;
        private Boolean multivalued = false;

        public Builder() {

        }

        public Builder(SolrFieldType fieldType) {
            if(fieldType != null) {
                this.name = fieldType.getName();
                this.missingFirst = fieldType.isMissingFirst();
                this.missingLast = fieldType.isMissingLast();
                this.fieldType = fieldType.getType();
                this.typeClass = fieldType.getTypeClass();
                this.multivalued = fieldType.isMultivalued();
            }
        }

        public Builder setName(String name) {
            this.name = name;
            return this;
        }

        public Builder setMissingFirst(Boolean missingFirst) {
            this.missingFirst = missingFirst;
            return this;
        }

        public Builder setMissingLast(Boolean missingLast) {
            this.missingLast = missingLast;
            return this;
        }

        public Builder setFieldType(SolrType fieldType) {
            this.fieldType = fieldType;
            return this;
        }

        public Builder setTypeClass(String typeClass) {
            this.typeClass = typeClass;
            return this;
        }

        public Builder setMultivalued(Boolean multivalued) {
            this.multivalued = multivalued;
            return this;
        }

        public SolrFieldType build() {
            return new SolrFieldType(name, fieldType, typeClass, multivalued, missingFirst, missingLast);
        }
    }
}
