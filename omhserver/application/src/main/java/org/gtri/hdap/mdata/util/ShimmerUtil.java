package org.gtri.hdap.mdata.util;

/**
 * Created by es130 on 8/6/2018.
 */
public class ShimmerUtil {

    /*========================================================================*/
    /* Constants */
    /*========================================================================*/
    public static String OMH_ON_FHIR_CALLBACK_ENV = "OMH_ON_FHIR_CALLBACK";
    public static String OMH_ON_FHIR_LOGIN_ENV = "OMH_ON_FHIR_LOGIN";
    public static String PATIENT_RESOURCE_ID = "p";
    public static String PATIENT_IDENTIFIER_SYSTEM = "https://omh.org/shimmer/ids";
    public static String OBSERVATION_CATEGORY_SYSTEM = "http://hl7.org/fhir/observation-category";
    public static String OBSERVATION_CATEGORY_CODE = "physical-activity";
    public static String OBSERVATION_CATEGORY_DISPLAY = "Physical Activity";
    public static String OBSERVATION_CODE_SYSTEM = "http://loinc.org";
    public static String OBSERVATION_CODE_CODE = "55423-8";
    public static String OBSERVATION_CODE_DISPLAY = "Number of steps in unspecified time.";
    public static String OBSERVATION_COMPONENT_CODE_SYSTEM = "http://hl7.org/fhir/observation-statistics";
    public static String OBSERVATION_COMPONENT_CODE_CODE = "maximum";
    public static String OBSERVATION_COMPONENT_CODE_DISPLAY = "Maximum";
    public static String OBSERVATION_COMPONENT_CODE_TEXT = "Maximum";
    public static String OBSERVATION_COMPONENT_VALUE_CODE_UNIT = "/{tot}";
    public static String OBSERVATION_COMPONENT_VALUE_CODE_SYSTEM = "http://unitsofmeasure.org";
    public static String OBSERVATION_COMPONENT_VALUE_CODE_CODE = "{steps}/{tot}";
}
