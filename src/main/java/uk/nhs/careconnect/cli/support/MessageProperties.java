package uk.nhs.careconnect.cli.support;

import ca.uhn.fhir.context.ConfigurationException;
import com.google.common.annotations.VisibleForTesting;

import java.io.FileInputStream;
import java.io.InputStream;
import java.util.Properties;

public class MessageProperties {

    private static final org.slf4j.Logger log = org.slf4j.LoggerFactory.getLogger(MessageProperties.class);


    static final String HAPI_PROPERTIES = "message.properties";


    static final String SERVER_FACILITY = "server.facility";
    static final String SERVER_APPLICATION = "server.application";

    static final String HL7_ROUTE_EXCEPTION = "hl7.route.exception";
    static final String HL7_ROUTE_FILE_IN = "hl7.route.fileIn";
    static final String HL7_ROUTE_FILE_OUT = "hl7.route.fileOut";
    static final String HL7_ROUTE_MLLP = "hl7.route.MLLP";

    static final String AWS_CLIENT_ID = "aws.clientId";
    static final String AWS_CLIENT_SECRET = "aws.clientSecret";
    static final String AWS_TOKEN_URL = "aws.tokenUrl";
    static final String AWS_CLIENT_USER = "aws.user";
    static final String AWS_CLIENT_PASS = "aws.pass";
    static final String AWS_API_KEY = "aws.apiKey";

    static final String AWS_QUEUE_NAME = "aws.queueName";
    static final String CDR_FHIR_SERVER = "cdr.fhirServer";

    private static Properties properties;

    /*
     * Force the configuration to be reloaded
     */
    public static void forceReload() {
        properties = null;
        getProperties();
    }

    /**
     * This is mostly here for unit tests. Use the actual properties file
     * to set values
     */
    @VisibleForTesting
    public static void setProperty(String theKey, String theValue) {
        getProperties().setProperty(theKey, theValue);
    }

    public static Properties getProperties() {
        if (properties == null) {
            // Load the configurable properties file
            try (InputStream in = MessageProperties.class.getClassLoader().getResourceAsStream(HAPI_PROPERTIES)){
                MessageProperties.properties = new Properties();
                MessageProperties.properties.load(in);
            } catch (Exception e) {
                throw new ConfigurationException("Could not load HAPI properties", e);
            }

            Properties overrideProps = loadOverrideProperties();
            if(overrideProps != null) {
                properties.putAll(overrideProps);
            }
        }

        return properties;
    }

    /**
     * If a configuration file path is explicitly specified via -Dfhir.properties=<path>, the properties there will
     * be used to override the entries in the default fhir.properties file (currently under WEB-INF/classes)
     * @return properties loaded from the explicitly specified configuraiton file if there is one, or null otherwise.
     */
    private static Properties loadOverrideProperties() {
        String confFile = System.getProperty(HAPI_PROPERTIES);
        if(confFile != null) {
            try {
                Properties props = new Properties();
                props.load(new FileInputStream(confFile));
                return props;
            }
            catch (Exception e) {
                throw new ConfigurationException("Could not load HAPI properties file: " + confFile, e);
            }
        }

        return null;
    }

    private static String getProperty(String propertyName) {

        return getProperty(propertyName,null);
    }

    private static String getProperty(String propertyName, String defaultValue) {
        Properties properties = MessageProperties.getProperties();
        log.trace("Looking for property = {}", propertyName);
        if (System.getenv(propertyName)!= null) {
            String value= System.getenv(propertyName);
            log.debug("System Environment property Found {} = {}", propertyName, value);
            return value;
        }
        if (System.getProperty(propertyName)!= null) {
            String value= System.getenv(propertyName);
            log.debug("System Property Found {} = {}" , propertyName, value);
            return value;
        }
        if (properties != null) {
            String value = properties.getProperty(propertyName);

            if (value != null && value.length() > 0) {
                return value;
            }
        }

        return defaultValue;
    }

    private static Boolean getPropertyBoolean(String propertyName, Boolean defaultValue) {
        String value = MessageProperties.getProperty(propertyName);

        if (value == null || value.length() == 0) {
            return defaultValue;
        }

        return Boolean.parseBoolean(value);
    }

    private static Integer getPropertyInteger(String propertyName, Integer defaultValue) {
        String value = MessageProperties.getProperty(propertyName);

        if (value == null || value.length() == 0) {
            return defaultValue;
        }

        return Integer.parseInt(value);
    }

    private static <T extends Enum> T getPropertyEnum(String thePropertyName, Class<T> theEnumType, T theDefaultValue) {
        String value = getProperty(thePropertyName, theDefaultValue.name());
        return (T) Enum.valueOf(theEnumType, value);
    }






    public static Boolean getEmailEnabled() {
        return MessageProperties.getPropertyBoolean("email.enabled", false);
    }

    public static String getEmailHost() {
        return MessageProperties.getProperty("email.host");
    }

    public static Integer getEmailPort() {
        return MessageProperties.getPropertyInteger("email.port", 0);
    }

    public static String getEmailUsername() {
        return MessageProperties.getProperty("email.username");
    }

    public static String getEmailPassword() {
        return MessageProperties.getProperty("email.password");
    }


    private static Properties loadProperties() {
        // Load the configurable properties file
        Properties properties;
        try (InputStream in = MessageProperties.class.getClassLoader().getResourceAsStream(HAPI_PROPERTIES)) {
            properties = new Properties();
            properties.load(in);
        } catch (Exception e) {
            throw new ConfigurationException("Could not load HAPI properties", e);
        }

        Properties overrideProps = loadOverrideProperties();
        if (overrideProps != null) {
            properties.putAll(overrideProps);
        }
        return properties;
    }


    public static String getServerFacility() {
        return MessageProperties.getProperty(SERVER_FACILITY,"ODS_CODE");
    }

    public static String getServerApplication() {
        return MessageProperties.getProperty(SERVER_APPLICATION, "EPR");
    }

     public static String getHl7RouteException() {
        return MessageProperties.getProperty(HL7_ROUTE_EXCEPTION,"file:///HL7v2/Error");
    }

    public static String getHl7RouteFileIn() {
        return MessageProperties.getProperty(HL7_ROUTE_FILE_IN,"");
    }

    public static String getHl7RouteFileOut() {
        return MessageProperties.getProperty(HL7_ROUTE_FILE_OUT,"file:///HL7v2/Out");
    }

    public static String getHl7RouteMllp() {
        return MessageProperties.getProperty(HL7_ROUTE_MLLP);
    }

    public static String getAwsClientId(){
        return MessageProperties.getProperty(AWS_CLIENT_ID);
    }
    public static String getAwsClientSecret(){
        return MessageProperties.getProperty(AWS_CLIENT_SECRET);
    }
    public static String getAwsTokenUrl(){
        return MessageProperties.getProperty(AWS_TOKEN_URL);
    }
    public static String getAwsClientUser(){
        return MessageProperties.getProperty(AWS_CLIENT_USER);
    }
    public static String getAwsClientPass(){
        return MessageProperties.getProperty(AWS_CLIENT_PASS);
    }
    public static String getAwsQueueName(){
        return MessageProperties.getProperty(AWS_QUEUE_NAME);
    }
    public static String getAwsApiKey(){
        return MessageProperties.getProperty(AWS_API_KEY);
    }
    public static String getCdrFhirServer(){
        return MessageProperties.getProperty(CDR_FHIR_SERVER);
    }
}
