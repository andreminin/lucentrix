package org.lucentrix.metaframe.plugin.solr;

import org.apache.commons.lang3.StringUtils;
import org.apache.solr.client.solrj.SolrClient;
import org.apache.solr.client.solrj.request.schema.SchemaRequest;
import org.apache.solr.client.solrj.response.schema.FieldTypeRepresentation;
import org.apache.solr.client.solrj.response.schema.SchemaResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

public class SolrIndexSchema {
    protected static final Logger logger = LoggerFactory.getLogger(SolrIndexSchema.class);

    protected Map<String, SolrFieldType> fieldTypes = new ConcurrentHashMap<>();
    protected Map<String, SolrFieldType> dynamicFieldTypes = new HashMap<>();

    protected List<SolrDynamicField> dynamicFields = new ArrayList<>();

    protected volatile boolean initialized;
    private static final SolrIndexSchema defaultSchema;


    static {
        defaultSchema = new SolrIndexSchema() {
        };

        Map<String, SolrFieldType> fieldTypes = new HashMap<>();
        Map<String, SolrFieldType> dynamicFieldTypes = new HashMap<>();
        List<SolrDynamicField> dynamicFields = new ArrayList<>();

        fieldTypes.put("id", new SolrFieldType.Builder().setName("string").setFieldType(SolrType.STRING).setMultivalued(false).build());

        dynamicFieldTypes.put("*_time", new SolrFieldType.Builder().setName("pdate_time").setFieldType(SolrType.DATETIME).setMultivalued(false).build());
        dynamicFieldTypes.put("*_d", new SolrFieldType.Builder().setName("pdate_d").setFieldType(SolrType.DATETIME).setMultivalued(false).build());
        dynamicFieldTypes.put("*_dt", new SolrFieldType.Builder().setName("pdate_dt").setFieldType(SolrType.DATETIME).setMultivalued(false).build());
        dynamicFields.add(new SolrDynamicField("*_time"));
        dynamicFields.add(new SolrDynamicField("*_d"));
        dynamicFields.add(new SolrDynamicField("*_dt"));

        dynamicFieldTypes.put("*_i", new SolrFieldType.Builder().setName("pint").setFieldType(SolrType.INT).setMultivalued(false).build());
        dynamicFieldTypes.put("*_l", new SolrFieldType.Builder().setName("plong").setFieldType(SolrType.LONG).setMultivalued(false).build());
        dynamicFieldTypes.put("*_f", new SolrFieldType.Builder().setName("pfloat").setFieldType(SolrType.FLOAT).setMultivalued(false).build());
        dynamicFieldTypes.put("*_s", new SolrFieldType.Builder().setName("string").setFieldType(SolrType.STRING).setMultivalued(false).build());
        dynamicFieldTypes.put("*_tt", new SolrFieldType.Builder().setName("tt_general").setFieldType(SolrType.TEXT).setMultivalued(false).build());
        dynamicFieldTypes.put("*_text", new SolrFieldType.Builder().setName("text_general").setFieldType(SolrType.TEXT).setMultivalued(false).build());
        dynamicFieldTypes.put("*_txt", new SolrFieldType.Builder().setName("txt_general").setFieldType(SolrType.TEXT).setMultivalued(false).build());
        dynamicFields.add(new SolrDynamicField("*_i"));
        dynamicFields.add(new SolrDynamicField("*_l"));
        dynamicFields.add(new SolrDynamicField("*_f"));
        dynamicFields.add(new SolrDynamicField("*_s"));
        dynamicFields.add(new SolrDynamicField("*_tt"));
        dynamicFields.add(new SolrDynamicField("*_txt"));
        dynamicFields.add(new SolrDynamicField("*_text"));

        dynamicFieldTypes.put("*_dts", new SolrFieldType.Builder().setName("pdate_dtw").setFieldType(SolrType.DATETIME).setMultivalued(true).build());
        dynamicFieldTypes.put("*_is", new SolrFieldType.Builder().setName("pinta").setFieldType(SolrType.INT).setMultivalued(true).build());
        dynamicFieldTypes.put("*_ls", new SolrFieldType.Builder().setName("plongs").setFieldType(SolrType.LONG).setMultivalued(true).build());
        dynamicFieldTypes.put("*_fs", new SolrFieldType.Builder().setName("pfloats").setFieldType(SolrType.FLOAT).setMultivalued(true).build());
        dynamicFieldTypes.put("*_ss", new SolrFieldType.Builder().setName("strings").setFieldType(SolrType.STRING).setMultivalued(true).build());
        dynamicFields.add(new SolrDynamicField("*_dts"));
        dynamicFields.add(new SolrDynamicField("*_is"));
        dynamicFields.add(new SolrDynamicField("*_ls"));
        dynamicFields.add(new SolrDynamicField("*_fs"));
        dynamicFields.add(new SolrDynamicField("*_ss"));

        defaultSchema.init(fieldTypes, dynamicFieldTypes, dynamicFields);
    }

    public static SolrIndexSchema getDefault() {
        return defaultSchema;
    }


    public synchronized void init(SolrClient solrClient, String collection, String user, String password) {
        logger.info("Initializing index schema for collection {}", collection);
        fieldTypes.clear();
        dynamicFields.clear();

        Map<String, Set<NameWithAttributes>> fieldNameByType = new HashMap<>();

        SchemaRequest.Fields fieldsRequest = new SchemaRequest.Fields();
        if (StringUtils.isNotBlank(user)) {
            fieldsRequest.setBasicAuthCredentials(user, password);
        }
        SchemaResponse.FieldsResponse fieldsResponse;
        try {
            fieldsResponse = fieldsRequest.process(solrClient, collection);
        } catch (Exception e) {
            throw new RuntimeException("Index schema (collection \"" + collection + "\") initialization error", e);
        }

        List<Map<String, Object>> schemaFields = fieldsResponse.getFields();

        for (Map<String, Object> schemaField : schemaFields) {
            Map<String, Object> attributes = new HashMap<>(schemaField);

            String fieldName = String.valueOf(attributes.remove("name"));
            String fieldTypeName = String.valueOf(attributes.remove("type"));
            Set<NameWithAttributes> names = fieldNameByType.computeIfAbsent(fieldTypeName, k -> new HashSet<>());
            names.add(NameWithAttributes.builder()
                    .name(fieldName)
                    .attributes(attributes)
                    .build());
            fieldNameByType.put(fieldTypeName, names);
        }

        Map<String, Set<NameWithAttributes>> dynamicFieldNameByType = new HashMap<>();
        SchemaRequest.DynamicFields dynFieldsRequest = new SchemaRequest.DynamicFields();
        if (StringUtils.isNotBlank(user)) {
            dynFieldsRequest.setBasicAuthCredentials(user, password);
        }
        SchemaResponse.DynamicFieldsResponse dynFieldsResponse;
        try {
            dynFieldsResponse = dynFieldsRequest.process(solrClient, collection);
        } catch (Exception e) {
            throw new RuntimeException("Index schema initializaton error", e);
        }

        schemaFields = dynFieldsResponse.getDynamicFields();

        for (Map<String, Object> schemaField : schemaFields) {
            Map<String, Object> attributes = new HashMap<>(schemaField);

            String fieldName = String.valueOf(attributes.remove("name"));
            String fieldTypeName = String.valueOf(attributes.remove("type"));
            Set<NameWithAttributes> names = dynamicFieldNameByType.computeIfAbsent(fieldTypeName, k -> new HashSet<>());
            names.add(NameWithAttributes.builder()
                    .name(fieldName)
                    .attributes(attributes)
                    .build());
            dynamicFieldNameByType.put(fieldTypeName, names);
        }

        SchemaRequest.FieldTypes fieldTypesRequest = new SchemaRequest.FieldTypes();

        if (StringUtils.isNotBlank(user)) {
            fieldTypesRequest.setBasicAuthCredentials(user, password);
        }
        SchemaResponse.FieldTypesResponse fieldTypesResponse;
        try {
            fieldTypesResponse = fieldTypesRequest.process(solrClient, collection);
        } catch (Exception e) {
            throw new RuntimeException("Index schema initialization error", e);
        }

        List<FieldTypeRepresentation> fieldTypeDefinitions = fieldTypesResponse.getFieldTypes();

        for (FieldTypeRepresentation fieldTypeDefinition : fieldTypeDefinitions) {
            Map<String, Object> fieldTypeAttributes = fieldTypeDefinition.getAttributes();

            String fieldTypeName = String.valueOf(fieldTypeAttributes.get("name"));
            Set<NameWithAttributes> names = fieldNameByType.get(fieldTypeName);

            if (names != null) {
                for (NameWithAttributes name : names) {
                    Map<String, Object> attributes = new HashMap<>(fieldTypeAttributes);
                    attributes.putAll(name.getAttributes());

                    SolrFieldType solrFieldType = createIndexFieldType(attributes);
                    addFieldType(name.getName(), solrFieldType);
                }
            }

            names = dynamicFieldNameByType.get(fieldTypeName);
            if (names != null) {
                for (NameWithAttributes name : names) {
                    Map<String, Object> attributes = new HashMap<>(fieldTypeAttributes);
                    attributes.putAll(name.getAttributes());

                    SolrFieldType solrFieldType = createIndexFieldType(attributes);

                    addDynamicFieldType(name.getName(), solrFieldType);
                }
            }
        }

        initialized = true;

        logger.info("Solr9 index schema initialized.");
    }

    public synchronized void init(Map<String, SolrFieldType> fieldTypes, Map<String, SolrFieldType> dynamicFieldTypes,
                                  List<SolrDynamicField> dynamicFields) {
        this.fieldTypes = fieldTypes;
        this.dynamicFieldTypes = dynamicFieldTypes;
        this.dynamicFields = dynamicFields;
        this.initialized = true;
    }

    public boolean isInitialized() {
        return initialized;
    }

    public void addFieldType(String fieldName, SolrFieldType fieldType) {
        this.fieldTypes.put(fieldName, fieldType);
        this.initialized = true;
    }

    public void addDynamicFieldType(String fieldName, SolrFieldType fieldType) {
        this.dynamicFieldTypes.put(fieldName, fieldType);
        addDynamicField(fieldName);

        this.initialized = true;
    }

    public void addDynamicField(String regex) {
        if (StringUtils.isBlank(regex)) {
            throw new IllegalArgumentException("Regex parameter can't be blank");
        }
        this.dynamicFields.add(new SolrDynamicField(regex));
        this.initialized = true;
    }

    public SolrFieldType getFieldType(String fieldName) {
        return fieldTypes.computeIfAbsent(fieldName, name -> {
            SolrFieldType fieldType = getFieldTypeInternal(name);
            if (fieldType == null) {
                fieldType = new SolrFieldType(null);
            }
            return fieldType;
        });
    }

    private SolrFieldType getFieldTypeInternal(String fieldName) {
        // String fieldName = fieldNameMapper.toInternalName(repoFieldName);

        SolrFieldType fieldType = fieldTypes.get(fieldName);

        if (fieldType == null) {
            for (SolrDynamicField dynamicField : dynamicFields) {
                if (dynamicField.matches(fieldName)) {
                    String fieldRegex = dynamicField.getRegex();
                    fieldType = dynamicFieldTypes.get(fieldRegex);
                    break;
                }
            }
        }

        if (fieldType == null) {
            //        fieldType.setName(repoFieldName);
            //  } else {
            logger.debug("Schema field is not found: {}", fieldName);
        }

        return fieldType;
    }

    protected SolrFieldType createIndexFieldType(Map<String, Object> attributes) {
        Object objValue = attributes.get("sortMissingLast");
        boolean missingLast = objValue != null && Boolean.parseBoolean(String.valueOf(objValue));

        objValue = attributes.get("sortMissingFirst");
        boolean missingFirst = objValue != null && Boolean.parseBoolean(String.valueOf(objValue));

        boolean multivalued = Boolean.parseBoolean(String.valueOf(attributes.get("multiValued")));

        String name = String.valueOf(attributes.get("name"));

        String typeClass = String.valueOf(attributes.get("class"));
        SolrType type;

        switch (typeClass) {
            case "solr.StrField":
                type = SolrType.STRING;
                break;
            case "solr.TextField":
                type = SolrType.TEXT;
                break;
            case "solr.DateTime":
                type = SolrType.DATETIME;
                break;
            case "solr.DateField":
            case "solr.TrieDateField":
            case "solr.DatePointField":
                type = SolrType.DATE;
                break;
            case "solr.FloatField":
            case "solr.TrieFloatField":
            case "solr.FloatPointField":
                type = SolrType.FLOAT;
                break;
            case "solr.IntField":
            case "solr.TrieIntField":
            case "solr.IntPointField":
                type = SolrType.INT;
                break;
            case "solr.DoubleField":
            case "solr.TrieDoubleField":
            case "solr.DoublePointField":
                type = SolrType.DOUBLE;
                break;
            case "solr.LongField":
            case "solr.TrieLongField":
            case "solr.LongPointField":
                type = SolrType.LONG;
                break;
            case "solr.BoolField":
                type = SolrType.BOOLEAN;
                break;
            case "solr.CurrencyField":
                type = SolrType.STRING;
                break;
            case "solr.LatLonType":
                type = SolrType.STRING;
                break;
            case "solr.BinaryField":
                type = SolrType.BINARY;
                break;
            default:
                type = SolrType.UNKNOWN;
                break;
        }

        SolrFieldType indexFieldType = new SolrFieldType(name);
        indexFieldType.setFieldType(type);
        indexFieldType.setMissingFirst(missingFirst);
        indexFieldType.setMissingLast(missingLast);
        indexFieldType.setTypeClass(typeClass);
        indexFieldType.setMultivalued(multivalued);

        return indexFieldType;
    }

    //TODO Review and fix later, make it configurable
    public String getIdField() {
        return "id";
    }
}
