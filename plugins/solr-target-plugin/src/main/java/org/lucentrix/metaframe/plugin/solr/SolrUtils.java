package org.lucentrix.metaframe.plugin.solr;

import org.apache.commons.lang3.StringUtils;
import org.apache.http.auth.AuthScope;
import org.apache.http.auth.UsernamePasswordCredentials;
import org.apache.http.client.CredentialsProvider;
import org.apache.http.client.HttpClient;
import org.apache.http.impl.client.BasicCredentialsProvider;
import org.apache.http.impl.client.HttpClientBuilder;
import org.apache.solr.client.solrj.SolrClient;
import org.apache.solr.client.solrj.SolrRequest;
import org.apache.solr.client.solrj.SolrServerException;
import org.apache.solr.client.solrj.impl.CloudHttp2SolrClient;
import org.apache.solr.client.solrj.impl.CloudSolrClient;
import org.apache.solr.client.solrj.impl.Http2SolrClient;
import org.apache.solr.client.solrj.request.QueryRequest;
import org.apache.solr.client.solrj.response.QueryResponse;
import org.apache.solr.common.SolrDocument;
import org.apache.solr.common.SolrDocumentList;
import org.apache.solr.common.SolrInputDocument;
import org.apache.solr.common.params.CommonParams;
import org.apache.solr.common.params.ModifiableSolrParams;
import org.apache.solr.common.params.SolrParams;
import org.lucentrix.metaframe.LxDocument;
import org.lucentrix.metaframe.metadata.FieldObjectMap;
import org.lucentrix.metaframe.metadata.field.Field;
import org.lucentrix.metaframe.metadata.field.TypeId;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.time.Instant;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.function.Supplier;

import static org.lucentrix.metaframe.plugin.solr.SolrFieldType.NOT_EXISTING;

public class SolrUtils {
    private static final Logger logger = LoggerFactory.getLogger(SolrUtils.class);

    public static final Set<String> READ_ONLY_FIELDS = Set.of("_version_", "_root_");

    public static void uploadDocuments(SolrClient solrClient, InputStream is) {
        try {

            List<SolrInputDocument> inputDocuments = getSolrInputDocumentListFromXml(is);

            solrClient.add(inputDocuments);
            solrClient.commit();
        } catch (Exception ex) {

            throw new RuntimeException(ex);
        }
    }

    public static SolrDocumentList getByIds(SolrClient solrClient, Collection<String> ids, SolrParams params)
            throws SolrServerException, IOException {
        if (ids == null || ids.isEmpty()) {
            throw new IllegalArgumentException("Must provide an identifier of a document to retrieve.");
        }

        ModifiableSolrParams reqParams = new ModifiableSolrParams(params);
        if (StringUtils.isEmpty(reqParams.get(CommonParams.QT))) {
            reqParams.set(CommonParams.QT, "/get");
        }
        reqParams.set("ids", ids.toArray(new String[0]));

        QueryResponse queryResponse = new QueryRequest(reqParams, SolrRequest.METHOD.POST).process(solrClient);

        return queryResponse.getResults();
    }

    public static SolrInputDocument toSolrInputDocument(LxDocument document, SolrIndexSchema schema, SolrFieldMapper mapper) {
        if (document == null) {
            return null;
        }

        SolrInputDocument solrDocument = lxFieldsToSolrDocument(document, schema, mapper);

        if (document.getChildren() != null) {
            for (FieldObjectMap child : document.getChildren()) {

                SolrInputDocument solrChild = lxFieldsToSolrDocument(child, schema, mapper);

                solrDocument.addChildDocument(solrChild);
            }
        }

        return solrDocument;
    }


    public static Object replaceNullString(Object value) {
        if (value == null) {
            return null;
        }

        if (value instanceof Collection<?> source) {
            Collection<Object> copy;

            if (value instanceof Set) {
                copy = new HashSet<>();
            } else {
                copy = new ArrayList<>();
            }

            for (Object item : source) {
                copy.add(replaceNullString(item));
            }

            return copy;
        }

        return SolrFieldType.INDEX_NULL_VALUE.equals(value) ? null : value;
    }


    private static SolrInputDocument lxFieldsToSolrDocument(FieldObjectMap inputDocument, SolrIndexSchema schema, SolrFieldMapper mapper) {
        SolrInputDocument solrInputDocument = new SolrInputDocument();


        for (Map.Entry<Field<?>, Object> entry : inputDocument) {
            Set<String> solrFields = mapper.toSolrFields(entry.getKey());

            for(String solrField : solrFields) {
                if (READ_ONLY_FIELDS.contains(solrField)) {
                    continue;
                }

                Object value = entry.getValue();
                if (value != null && TypeId.DATETIME.equals(entry.getKey().getType().getTypeId())) {
                    Instant time = (Instant) value;
                    value = time.atOffset(ZoneOffset.UTC).format(DateTimeFormatter.ISO_OFFSET_DATE_TIME);
                }

                setSolrField(solrInputDocument, schema, solrField, value);
            }
        }

        return solrInputDocument;
    }

    protected static void setSolrField(SolrInputDocument document, SolrIndexSchema schema, String solrField, Object fieldValue) {
        SolrFieldType solrFieldType = getTypeFromSchema(schema, solrField);

        Object solrValue;
        try {
            solrValue = toSolrValue(solrFieldType, fieldValue);
        } catch (Exception ex) {
            throw new RuntimeException(String.format("Error converting %s field value %s with Solr type=%s",
                    solrField, fieldValue, solrFieldType), ex);
        }
        if (solrValue == null) {
            logger.debug("Solr document field {} value converted to null", solrField);
        } else {
            logger.debug("Solr document field {} value converted to {} value: {}",
                    solrField, solrValue.getClass().getName(), solrValue);
        }

        try {
            document.setField(solrField, solrValue);
        } catch (Exception ex) {
            throw new RuntimeException(String.format("Error setting Solr field %s to value %s (original value: %s) with Solr type=%s",
                    solrField, solrValue, fieldValue, solrFieldType), ex);
        }
    }

    protected static SolrFieldType getTypeFromSchema(SolrIndexSchema indexSchema, String fieldName) {
        SolrFieldType indexFieldType = indexSchema.getFieldType(fieldName);
        if (indexFieldType == null || indexFieldType.getType() == null || NOT_EXISTING.equals(indexFieldType)) {
            throw new RuntimeException("Index field type not found in schema: " + fieldName);
        }
        return indexFieldType;
    }


    private static Object toSolrValue(SolrFieldType indexFieldType, Object fieldValue) {
        SolrType solrType = indexFieldType.getType();

        if (fieldValue instanceof Collection<?>) {
            Collection collection = (Collection) fieldValue;

            if (indexFieldType.isMultivalued()) {
                return toSolrCollectionValue(solrType, collection);
            } else {
                if (collection.isEmpty()) {
                    return toSolrSingleValue(solrType, null);
                }
                if (collection.size() == 1) {
                    return toSolrSingleValue(solrType, collection.iterator().next());
                }
                throw new RuntimeException("Solr index type " + indexFieldType + "is not multi-value: " + solrType + "=" + fieldValue);
            }
        } else {
            return toSolrSingleValue(solrType, fieldValue);
        }
    }

    private static Object toSolrSingleValue(SolrType targetType, Object value) {
        if (value == null) {
            if (SolrType.STRING.equals(targetType) || SolrType.TEXT.equals(targetType)) {
                return SolrFieldType.INDEX_NULL_VALUE;
            } else {
                return null;
            }
        } else {
            switch (targetType) {
                case DATE:
                case DATETIME: {
                    if (value instanceof Date) {
                        return value;
                    } else if (value instanceof Instant) {
                        return value;
                    } else if (value instanceof Long) {
                        return new Date(((Long) value));
                    } else {
                        return Date.from(Instant.parse(String.valueOf(value)));
                    }
                }
                case UUID: {
                    if (value instanceof UUID) {
                        return value;
                    } else {
                        return UUID.fromString(String.valueOf(value));
                    }
                }
                case BINARY: {
                    if (value instanceof byte[]) {
                        return value;
                    } else {
                        return Base64.getDecoder().decode(String.valueOf(value));
                    }
                }
                case BOOLEAN: {
                    if (value instanceof Boolean) {
                        return value;
                    } else {
                        return Boolean.parseBoolean(String.valueOf(value));
                    }
                }
                case INT: {
                    if (value instanceof Number) {
                        return ((Number) value).intValue();
                    } else {
                        return Integer.parseInt(String.valueOf(value));
                    }
                }
                case LONG: {
                    if (value instanceof Number) {
                        return ((Number) value).longValue();
                    } else {
                        return Long.parseLong(String.valueOf(value));
                    }
                }
                case FLOAT: {
                    if (value instanceof Number) {
                        return ((Number) value).floatValue();
                    } else {
                        return Float.parseFloat(String.valueOf(value));
                    }
                }
                case DOUBLE: {
                    if (value instanceof Number) {
                        return ((Number) value).doubleValue();
                    } else {
                        return Double.parseDouble(String.valueOf(value));
                    }
                }
                default: {

                    return String.valueOf(value);
                }
            }
        }
    }

    private static Object toSolrCollectionValue(SolrType targetType, Object value) {
        if (value == null) {
            if (SolrType.STRING.equals(targetType) || SolrType.TEXT.equals(targetType)) {
                return SolrFieldType.INDEX_NULL_VALUE;
            } else {
                return null;
            }
        } else if (value instanceof Collection) {
            Collection sourceCollection = (Collection) value;
            ArrayList converted = new ArrayList(sourceCollection.size());
            for (Object sourceValue : sourceCollection) {
                converted.add(toSolrSingleValue(targetType, sourceValue));
            }
            return converted;
        } else {
            return Collections.singleton(toSolrSingleValue(targetType, value));
        }
    }


    public static SolrInputDocument toSolrInputDocument(SolrDocument solrDocument) {
        SolrInputDocument solrInputDocument = new SolrInputDocument();

        for (String name : solrDocument.getFieldNames()) {
            if (name != null && !name.startsWith("_")) {
                solrInputDocument.addField(name, solrDocument.getFieldValue(name));
            }
        }

        if (solrDocument.getChildDocuments() != null) {
            for (SolrDocument childDocument : solrDocument.getChildDocuments()) {
                solrInputDocument.addChildDocument(toSolrInputDocument(childDocument));
            }
        }

        return solrInputDocument;
    }

    public static List<SolrInputDocument> getSolrInputDocumentListFromXml(InputStream xmlDataStream) {

        ArrayList<SolrInputDocument> solrDocList = new ArrayList<>();

        try {
            DocumentBuilderFactory dbFactory = DocumentBuilderFactory.newInstance();
            DocumentBuilder dBuilder = dbFactory.newDocumentBuilder();
            Document doc = dBuilder.parse(xmlDataStream);

            NodeList docList = doc.getElementsByTagName("doc");

            for (int docIdx = 0; docIdx < docList.getLength(); docIdx++) {

                Node docNode = docList.item(docIdx);

                if (docNode.getNodeType() == Node.ELEMENT_NODE) {

                    SolrInputDocument solrInputDoc = new SolrInputDocument();

                    Element docElement = (Element) docNode;

                    NodeList fieldsList = docElement.getChildNodes();

                    for (int fieldIdx = 0; fieldIdx < fieldsList.getLength(); fieldIdx++) {

                        Node fieldNode = fieldsList.item(fieldIdx);

                        if (fieldNode.getNodeType() == Node.ELEMENT_NODE) {

                            Element fieldElement = (Element) fieldNode;

                            String fieldName = fieldElement.getAttribute("name");
                            String fieldValue = fieldElement.getTextContent();

                            solrInputDoc.addField(fieldName, fieldValue);
                        }

                    }

                    solrDocList.add(solrInputDoc);
                }
            }

            return solrDocList;
        } catch (Exception e) {
            throw new RuntimeException(e);
        }

    }


    public static SolrClient createCloudHttp2SolrClient(SolrConfig solrConfig) {
        if (solrConfig == null) {
            throw new IllegalArgumentException("Solr connection configuration is null");
        }

        UsernamePasswordCredentials credentials = null;

        if (StringUtils.isNotBlank(solrConfig.getUser())) {
            logger.info("Creating http authentication provider for user {}", solrConfig.getUser());
            credentials = new UsernamePasswordCredentials(solrConfig.getUser(), solrConfig.getPassword());
        }

        if (solrConfig.getSolrUrls() != null) {
            if (solrConfig.getSolrUrls().size() == 1) {
                return createCloudHttp2SolrClient(solrConfig.getSolrUrls().get(0), credentials);
            } else {
                if (StringUtils.isBlank(solrConfig.getCollection())) {
                    throw new RuntimeException("Invalid solr cluster configuration: collection id is null: " + solrConfig);
                }

                return createCloudHttp2SolrClient(solrConfig.getSolrUrls(), credentials);
            }
        } else if (solrConfig.getZkHosts() != null) {
            return createCloudHttp2SolrClient(solrConfig.getZkHosts(), solrConfig.getZkChroot(), credentials);
        } else {
            throw new RuntimeException("No SOLR URL and Zookeeper URL provided to SOLR 8 client builder");
        }
    }

    public static SolrClient createCloudHttp2SolrClient(String solrUrl) {
        return createCloudHttp2SolrClient(solrUrl, null);
    }

    public static SolrClient createCloudHttp2SolrClient(String solrUrl, UsernamePasswordCredentials credentials) {
        return wrapLocalContextClassLoader(() -> {
            logger.info("Creating single node Solr client for solr URL={}", solrUrl);
            try {
                new URL(solrUrl);
                logger.debug("Creating single node Solr client for solr URL={}", solrUrl);

                Http2SolrClient.Builder builder = new Http2SolrClient.Builder(solrUrl);
                if (credentials != null) {
                    builder = builder.withHttpClient(createHttp2Client(credentials));
                }

                return builder.build();
            } catch (Exception error) {
                logger.error("Error creating single node Solr client for URL: " + solrUrl, error);
                throw new RuntimeException(error);
            }
        });
    }

    private static Http2SolrClient createHttp2Client(UsernamePasswordCredentials credentials) {
        return wrapLocalContextClassLoader(() -> {
            Http2SolrClient.Builder builder = new Http2SolrClient.Builder();
            builder.withBasicAuthCredentials(credentials.getUserName(), credentials.getPassword());

            return builder.build();
        });
    }

    private static HttpClient createHttpClient(UsernamePasswordCredentials credentials) {
        return wrapLocalContextClassLoader(() -> {
            CredentialsProvider provider = new BasicCredentialsProvider();
            provider.setCredentials(AuthScope.ANY, credentials);

            return HttpClientBuilder.create().setDefaultCredentialsProvider(provider).build();
        });
    }

    private static Http2SolrClient createHttp2SolrClient(UsernamePasswordCredentials credentials) {
        return wrapLocalContextClassLoader(() -> {
            Http2SolrClient.Builder builder = new Http2SolrClient.Builder();
            if (credentials != null) {
                logger.info("Adding authentication credentials to Http2SolrClient builder");
                builder.withBasicAuthCredentials(credentials.getUserName(), credentials.getPassword());
            }

            return builder.build();
        });
    }

    public static SolrClient createCloudHttp2SolrClient(List<String> zkHosts, String zkChroot, UsernamePasswordCredentials credentials) {
        return wrapLocalContextClassLoader(() -> {
            // Try cloud zookeeper client
            //"zkServerA:2181,zkServerB:2181,zkServerC:2181/solr";
            logger.info("Creating cloud Solr client for solr zkHosts={}, zkChroot={}", zkHosts, zkChroot);
            try {
                logger.debug("Creating cloud Solr client for zookeper URLs={}, zhRoot={}", zkHosts, zkChroot);
                CloudSolrClient.Builder builder = new CloudSolrClient.Builder(zkHosts, Optional.ofNullable(zkChroot));

                if (credentials != null) {
                    return builder.withHttpClient(createHttp2Client(credentials)).build();
                }

                return builder.build();
            } catch (Exception error) {
                logger.error("Error creating cloud Solr client for zkHosts " + zkHosts, error);
                throw error;
            }
        });
    }


    private static <T> T wrapLocalContextClassLoader(Supplier<T> supplier) {
        ClassLoader packageClassLoader = SolrUtils.class.getClassLoader();

        ClassLoader restoreClassLoader = Thread.currentThread().getContextClassLoader();
        Thread.currentThread().setContextClassLoader(packageClassLoader);

        try {
            return supplier.get();
        } finally {
            Thread.currentThread().setContextClassLoader(restoreClassLoader);
        }
    }

    public static SolrClient createCloudHttp2SolrClient(List<String> solrUrls, UsernamePasswordCredentials credentials) {
        return wrapLocalContextClassLoader(() -> {
            logger.info("Creating cloud Solr client for solr URLs={}", solrUrls);
            try {
                CloudHttp2SolrClient.Builder builder = new CloudHttp2SolrClient.Builder(solrUrls);

                if (credentials != null && StringUtils.isNotBlank(credentials.getUserName())) {
                    logger.info("Configuring basic authentication in solr client");
                    Http2SolrClient solrClient = createHttp2SolrClient(credentials);
                    builder = builder.withHttpClient(solrClient);
                }

                return builder.build();
            } catch (Exception error) {
                logger.error("Error creating cloud Solr client for URLs " + solrUrls, error);
                throw error;
            }
        });
    }

    public static void deleteAllDocumentsFromSOLR(SolrClient solrClient) {
        // stream.body=<delete><query>*:*</query></delete>&commit=true
        try {

            solrClient.deleteByQuery("*:*");

            solrClient.commit();
        } catch (Exception ex) {

            throw new RuntimeException(ex);
        }
    }

    public static List<LxDocument> toLxDocuments(List<SolrInputDocument> solrInputDocuments, SolrFieldSerde fieldSchema) {
        if (solrInputDocuments == null) {
            return null;
        }
        List<LxDocument> inputDocuments = new ArrayList<>(solrInputDocuments.size());

        for (SolrInputDocument solrInputDocument : solrInputDocuments) {
            inputDocuments.add(toLxDocument(solrInputDocument, fieldSchema));
        }

        return inputDocuments;
    }

    public static LxDocument toLxDocument(SolrDocument solrDocument) {
        if (solrDocument == null) {
            return null;
        }

        LxDocument.LxDocumentBuilder<?, ?> builder = LxDocument.builder();

        Object value;
        for (String fieldName : solrDocument.getFieldNames()) {
            value = solrDocument.getFieldValue(fieldName);
            builder.field(SolrFieldSerde.INSTANCE.fromString(fieldName), replaceNullString(value));
        }

        if (solrDocument.getChildDocuments() != null) {
            for (SolrDocument solrChild : solrDocument.getChildDocuments()) {
                LxDocument.LxDocumentBuilder<?, ?> child = LxDocument.builder();

                for (String fieldName : solrChild.getFieldNames()) {
                    value = solrDocument.getFieldValue(fieldName);
                    child.field(SolrFieldSerde.INSTANCE.fromString(fieldName), replaceNullString(value));
                }

                builder.addChild(child.build());
            }
        }

        return builder.build();
    }

    private static LxDocument toLxDocument(SolrInputDocument solrInputDocument, SolrFieldSerde fieldSchema) {
        if (solrInputDocument == null) {
            return null;
        }

        LxDocument.LxDocumentBuilder<?, ?> builder = LxDocument.builder();

        for (String fieldName : solrInputDocument.getFieldNames()) {
            Object value = solrInputDocument.getFieldValue(fieldName);

            builder.field(fieldSchema.fromString(fieldName), replaceNullString(value));
        }

        return builder.build();
    }


}
