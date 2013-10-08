package org.archiviststoolkit.plugin.utils.aspace;

import bsh.Interpreter;
import org.archiviststoolkit.model.*;
import org.archiviststoolkit.mydomain.DomainObject;
import org.archiviststoolkit.plugin.utils.RandomString;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.Set;

/**
 * Created by IntelliJ IDEA.
 * User: nathan
 * Date: 9/5/12
 * Time: 1:41 PM
 *
 * Class to map AT data model to ASPace JSON data model
 */
public class ASpaceMapper {
    // String used when mapping AT access class to groups
    public static final String ACCESS_CLASS_PREFIX = "_AccessClass_";

    // The utility class used to map to ASpace Enums
    private ASpaceEnumUtil enumUtil = new ASpaceEnumUtil();

    // used to map AT vocabularies to ASpace vocabularies
    public String vocabularyURI = "/vocabularies/1";

    // the bean shell interpreter for doing mapping using scripts
    private Interpreter bsi = null;

    // the script mapper script
    private String mapperScript = null;

    // these store the ids of all accessions, resources, and digital objects loaded so we can
    // check for uniqueness before copying them to the ASpace backend
    private ArrayList<String> digitalObjectIDs = new ArrayList<String>();
    private ArrayList<String> accessionIDs = new ArrayList<String>();
    private ArrayList<String> resourceIDs = new ArrayList<String>();
    private ArrayList<String> eadIDs = new ArrayList<String>();

    // variable names in bean shell script that will indicate whether it can override
    // the default mapping operation with itself
    private static final String SUBJECT_MAPPER = "@subject";
    private static final String NAME_MAPPER = "@name";
    private static final String REPOSITORY_MAPPER = "@repository";
    private static final String USER_MAPPER = "@user";
    private static final String ACCESSION_MAPPER = "@accession";
    private static final String RESOURCE_MAPPER = "@resource";
    private static final String COMPONENT_MAPPER = "@component";
    private static final String DIGITAL_OBJECT_MAPPER = "@digitalobject";
    private static final String GLOBAL_MAPPER = "@all";

    // boolean variables to see if to use a mapper script
    private boolean runGlobalMapperScript = false;
    private boolean runSubjectMapperScript = false;
    private boolean runNameMapperScript = false;
    private boolean runRepositoryMapperScript = false;
    private boolean runLocationMapperScript = false;
    private boolean runUserMapperScript = false;
    private boolean runAccessionMapperScript = false;
    private boolean runResourceMapperScript = false;
    private boolean runComponentMapperScript = false;
    private boolean runDigitalObjectMapperScript = false;

    // some code used for testing
    private boolean makeUnique = false;
    private boolean allowTruncation = false;

    // initialize the random string generators for use when unique ids are needed
    private RandomString randomString = new RandomString(3);
    private RandomString randomStringLong = new RandomString(6);

    // used when specifying the external ids
    private String connectionUrl = "";

    public static final String DUMMY_DATE = "1001";

    // used to store errors
    private ASpaceCopyUtil aspaceCopyUtil;

    // used when generating errors
    private String currentResourceRecordIdentifier;

    /**
     *  Main constructor
     */
    public ASpaceMapper() { }

    /**
     * Constructor that takes an aspace copy util object
     * @param aspaceCopyUtil
     */
    public ASpaceMapper(ASpaceCopyUtil aspaceCopyUtil) {
        this.aspaceCopyUtil = aspaceCopyUtil;
    }

    /**
     * Method to see the bean shell script to override the default mapping action
     *
     * @param script
     */
    public String setMapperScript(String script) {
        // see what mapping functionality the script supports
        if(script.contains(GLOBAL_MAPPER)) {
            runGlobalMapperScript = true;
        } else if(script.contains(SUBJECT_MAPPER)) {
            runSubjectMapperScript = true;
        } else if(script.contains(NAME_MAPPER)) {
            runNameMapperScript = true;
        } else if(script.contains(REPOSITORY_MAPPER)) {
            runRepositoryMapperScript = true;
        } else if(script.contains(USER_MAPPER)) {
            runUserMapperScript = true;
        } else if(script.contains(ACCESSION_MAPPER)) {
            runAccessionMapperScript = true;
        } else if(script.contains(RESOURCE_MAPPER)) {
            runResourceMapperScript = true;
        } else if(script.contains(COMPONENT_MAPPER)) {
            runComponentMapperScript = true;
        } else if(script.contains(DIGITAL_OBJECT_MAPPER)) {
            runDigitalObjectMapperScript = true;
        } else {
            return "No mapper functionality specified by script";
        }

        // initialize the bean shell mapper
        bsi = new Interpreter();
        mapperScript = script;

        return "mapper script set ...";
    }

    /**
     * Method to copy an AT record to ASpace record
     *
     * @param record
     * @return
     * @throws Exception
     */
    public Object convert(DomainObject record) throws Exception {
        if(record instanceof Subjects) {
            if(runGlobalMapperScript || runSubjectMapperScript) {
                return runMapperScript(record);
            } else {
                return convertSubject((Subjects)record);
            }
        } else if (record instanceof Names) {
            if(runGlobalMapperScript || runNameMapperScript) {
                return runMapperScript(record);
            } else {
                return convertName((Names) record);
            }
        } else if (record instanceof Repositories) {
            if(runGlobalMapperScript || runRepositoryMapperScript) {
                return runMapperScript(record);
            } else {
                return convertRepository((Repositories) record, null);
            }
        } else if (record instanceof Locations) {
            if(runGlobalMapperScript || runLocationMapperScript) {
                return runMapperScript(record);
            } else {
                return convertLocation((Locations) record);
            }
        } else if (record instanceof Users) {
            if(runGlobalMapperScript || runUserMapperScript) {
                return runMapperScript(record);
            } else {
                return convertUser((Users) record);
            }
        } else if (record instanceof Accessions) {
            if(runGlobalMapperScript || runAccessionMapperScript) {
                return runMapperScript(record);
            } else {
                return convertAccession((Accessions) record);
            }
        } else if (record instanceof Resources) {
            if(runGlobalMapperScript || runResourceMapperScript) {
                return runMapperScript(record);
            } else {
                return convertResource((Resources) record);
            }
        } else if (record instanceof ResourcesComponents) {
            if(runGlobalMapperScript || runComponentMapperScript) {
                return runMapperScript(record);
            } else {
                return convertResourceComponent((ResourcesComponents) record);
            }
        } else if (record instanceof DigitalObjects) {
            if(runGlobalMapperScript || runDigitalObjectMapperScript) {
                return runMapperScript(record);
            } else {
                return convertDigitalObject((DigitalObjects) record);
            }
        } else {
            return null;
        }
    }

    /**
     * Method to run a mapper script on a domain object record
     *
     * @param record
     * @return
     * @throws Exception
     */
    public Object runMapperScript(DomainObject record) throws Exception {
        bsi.set("record", record);
        bsi.eval(mapperScript);
        return bsi.get("result");
    }

    /**
     * Method to convert an AT subject record to
     *
     * @param record
     * @return
     * @throws Exception
     */
    public String convertSubject(Subjects record) throws Exception {
        // Main json object
        JSONObject json = new JSONObject();

        // add the AT database Id as an external ID
        addExternalId(record, json, "subject");

        // set the subject source
        String source = record.getSubjectSource();
        if(!source.isEmpty()) {
            source = enumUtil.getASpaceSubjectSource(record.getSubjectSource());
            json.put("source", source);
        }

        // set the subject terms and term type
        String terms = record.getSubjectTerm();
        String termType = enumUtil.getASpaceTermType(record.getSubjectTermType());

        String[] sa = terms.split("\\s*--\\s*");
        JSONArray termsJA = new JSONArray();

        for(String term: sa) {
            JSONObject termJS = new JSONObject();

            termJS.put("term", term);
            termJS.put("term_type",termType);
            termJS.put("vocabulary", vocabularyURI);

            termsJA.put(termJS);
        }

        json.put("terms", termsJA);
        json.put("vocabulary", vocabularyURI);

        return json.toString();
    }

    /**
     * Method to convert name record to ASpace agent record
     *
     * @param record
     * @return
     */
    public String convertName(Names record) throws Exception {
        // Main json object, agent_person.rb schema
        JSONObject agentJS = new JSONObject();

        // add the AT database Id as an external ID
        addExternalId(record, agentJS, "name");

        // hold name information
        JSONArray namesJA = new JSONArray();
        JSONObject namesJS = new JSONObject();

        //add the contact information
        JSONArray contactsJA = new JSONArray();
        JSONObject contactsJS = new JSONObject();

        //TODO 7/9/2013 -- There is currently no good way to map salutation so just migrate values that match what already there
        String salutation = enumUtil.getASpaceSalutation(record.getSalutation());
        if(!salutation.equals(ASpaceEnumUtil.UNMAPPED)) {
            contactsJS.put("salutation", salutation);
        }

        contactsJS.put("address_1", record.getContactAddress1());
        contactsJS.put("address_2", record.getContactAddress2());
        contactsJS.put("city", record.getContactCity());
        contactsJS.put("region", record.getContactRegion());
        contactsJS.put("country", record.getContactCountry());
        contactsJS.put("post_code", record.getContactMailCode());
        contactsJS.put("telephone", record.getContactPhone());
        contactsJS.put("fax", record.getContactFax());
        contactsJS.put("email", record.getContactEmail());

        // add the contact notes if any. All notes will be concatenated
        addNote(contactsJS, record.getContactNotes());

        contactsJA.put(contactsJS);
        agentJS.put("agent_contacts", contactsJA);

        // add the biog-history note to the agent object
        addNote(agentJS, record);

        // get the type of name
        String type = record.getNameType();

        // get the mapped name source and rules
        String nameSource = enumUtil.getASpaceNameSource(record.getNameSource());
        String nameRule = enumUtil.getASpaceNameRule(record.getNameRule());

        // set values for abstract_name.rb schema
        namesJS.put("authority_id", "unknown"); // not sure what this should be
        namesJS.put("dates", record.getPersonalDates());

        namesJS.put("qualifier", record.getQualifier());
        namesJS.put("source", nameSource);
        namesJS.put("rules", nameRule);
        namesJS.put("sort_name", fixEmptyString(record.getSortName()));

        if(type.equalsIgnoreCase(Names.PERSON_TYPE)) {
            // set the agent type
            agentJS.put("agent_type", "agent_person");

            // set the title to unknown if it is blank
            String title = fixEmptyString(record.getPersonalTitle(), null);

            String primaryName = fixEmptyString(record.getPersonalPrimaryName(), null);

            // set values for name_person.rb schema
            namesJS.put("primary_name", primaryName);
            namesJS.put("title", title);
            namesJS.put("name_order", "direct");
            namesJS.put("prefix", record.getPersonalPrefix());
            namesJS.put("rest_of_name", record.getPersonalRestOfName());
            namesJS.put("suffix", record.getPersonalSuffix());
            namesJS.put("fuller_form", record.getPersonalFullerForm());
            namesJS.put("number", record.getNumber()); // not sure this is correct

            // set the name value for the contact information
            contactsJS.put("name", primaryName);
        } else if(type.equalsIgnoreCase(Names.FAMILY_TYPE)) {
            // set the agent type
            agentJS.put("agent_type", "agent_family");

            // set values for name_family.rb schema
            String familyName = fixEmptyString(record.getFamilyName(), null);

            namesJS.put("family_name", familyName);
            namesJS.put("prefix", record.getFamilyNamePrefix());

            // set the contact name
            contactsJS.put("name", familyName);
        } else if(type.equalsIgnoreCase(Names.CORPORATE_BODY_TYPE)) {
            // set the agent type
            agentJS.put("agent_type", "agent_corporate_entity");

            String primaryName = fixEmptyString(record.getCorporatePrimaryName(), null);

            // set values for name_corporate_entity.rb schema
            namesJS.put("primary_name", primaryName);
            namesJS.put("subordinate_name_1", record.getCorporateSubordinate1());
            namesJS.put("subordinate_name_2", record.getCorporateSubordinate2());
            namesJS.put("number", record.getNumber()); // not sure this is correct

            // set the contact name
            contactsJS.put("name", primaryName);
        } else {
            String message = record.getSortName() + ":: Unknown name type: " + type + "\n";
            aspaceCopyUtil.addErrorMessage(message);
            return null;
        }

        // add the names array and names json objects to main record
        namesJA.put(namesJS);
        agentJS.put("names", namesJA);

        return agentJS.toString();
    }

    /**
     * Method to get the corporate agent object from a repository
     *
     * @param repository
     * @return
     */
    public String getCorporateAgent(Repositories repository) throws JSONException {
        // Main json object, agent_person.rb schema
        JSONObject agentJS = new JSONObject();
        agentJS.put("agent_type", "agent_corporate_entity");

        // hold name information
        JSONArray namesJA = new JSONArray();
        JSONObject namesJS = new JSONObject();

        //add the contact information
        JSONArray contactsJA = new JSONArray();
        JSONObject contactsJS = new JSONObject();

        contactsJS.put("name", repository.getRepositoryName());
        contactsJS.put("address_1", repository.getAddress1());
        contactsJS.put("address_2", repository.getAddress2());
        contactsJS.put("address_3", repository.getAddress3());
        contactsJS.put("city", repository.getCity());

        // add the country and country code together
        String country = repository.getCountry() + " "  + repository.getCountryCode();
        contactsJS.put("country", country.trim());

        contactsJS.put("post_code", repository.getMailCode());
        contactsJS.put("telephone", repository.getTelephone());
        contactsJS.put("fax", repository.getFax());
        contactsJS.put("email", repository.getEmail());

        contactsJA.put(contactsJS);
        agentJS.put("agent_contacts", contactsJA);

        // add the names object
        String primaryName = repository.getRepositoryName();
        namesJS.put("source", "local");
        namesJS.put("primary_name", primaryName);
        namesJS.put("sort_name", primaryName);

        namesJA.put(namesJS);
        agentJS.put("names", namesJA);

        return agentJS.toString();
    }

    /**
     * Method to convert an AT subject record to
     *
     * @param record
     * @return
     * @throws Exception
     */
    public String convertRepository(Repositories record, String agentURI) throws Exception {
        // Main json object
        JSONObject json = new JSONObject();

        // add the AT database Id as an external ID
        addExternalId(record, json, "repository");

        // get the repo code
        json.put("repo_code", record.getShortName());
        json.put("name", fixEmptyString(record.getRepositoryName()));
        json.put("org_code", record.getAgencyCode());
        json.put("parent_institution_name", record.getInstitutionName());
        json.put("url", fixUrl(record.getUrl()));

        if(agentURI != null) {
            json.put("agent_representation", getReferenceObject(agentURI));
        }

        return json.toString();
    }

    public String convertLocation(Locations record) throws Exception {
        // Main json object
        JSONObject json = new JSONObject();

        // add the AT database Id as an external ID
        addExternalId(record, json, "location");

        // create a json object that represents the location object
        json.put("building", fixEmptyString(record.getBuilding(), "Unknown Building"));
        json.put("floor", record.getFloor());
        json.put("room", record.getRoom());
        json.put("area", record.getArea());
        json.put("barcode", record.getBarcode());
        json.put("classification", record.getClassificationNumber());
        json.put("coordinate_1_label", record.getCoordinate1Label());
        json.put("coordinate_1_indicator", record.getCoordinate1());
        json.put("coordinate_2_label", record.getCoordinate2Label());
        json.put("coordinate_2_indicator", record.getCoordinate2());
        json.put("coordinate_3_label", record.getCoordinate3Label());
        json.put("coordinate_3_indicator", record.getCoordinate3());

        return json.toString();
    }

     /**
     * Method to convert an AT subject record to
     *
     * @param record
     * @return
     * @throws Exception
     */
    public String convertUser(Users record) throws Exception {
        // Main json object
        JSONObject json = new JSONObject();

        // add the AT database Id as an external ID
        addExternalId(record, json, "user");

        // get the username replacing spaces with underscores
        String username = record.getUserName().trim();
        //username = username.replaceAll("-", " ");

        json.put("username", username);

        // get the full name
        String name = fixEmptyString(record.getFullName(), "full name no entered");
        json.put("name", name);

        json.put("email", record.getEmail());
        json.put("title", record.getTitle());
        json.put("department", record.getDepartment());

        return json.toString();
    }

    /**
     * Method to convert an accession record to json ASpace JSON
     *
     * @param record
     * @return
     * @throws Exception
     */
    public JSONObject convertAccession(Accessions record) throws Exception {
        // Main json object
        JSONObject json = new JSONObject();

        // add the AT database Id as an external ID
        addExternalId(record, json, "accession");

        // check to make sure we have a title
        String title = fixEmptyString(record.getTitle(), null);
        Date date = record.getAccessionDate();

        if(date == null) {
            String message = "Invalid Accession Date for" + record.getAccessionNumber() + "\n";
            aspaceCopyUtil.addErrorMessage(message);
            return null;
        }

        json.put("title", title);
        json.put("accession_date", date);

        // get the ids and make them unique if we in DEBUG mode
        String[] cleanIds = cleanUpIds(ASpaceClient.ACCESSION_ENDPOINT, record.getAccessionNumber1().trim(),
                record.getAccessionNumber2().trim(), record.getAccessionNumber3().trim(),
                record.getAccessionNumber4().trim());

        String id_0 = cleanIds[0];
        String id_1 = cleanIds[1];
        String id_2 = cleanIds[2];
        String id_3 = cleanIds[3];

        if (makeUnique) {
            id_0 = randomString.nextString();
            id_1 = randomString.nextString();
            id_2 = randomString.nextString();
            id_3 = randomString.nextString();
        }

        json.put("id_0", id_0);
        json.put("id_1", id_1);
        json.put("id_2", id_2);
        json.put("id_3", id_3);

        json.put("content_description", record.getDescription());
        json.put("condition_description", record.getConditionNote());

        //json.put("disposition", record.?);
        json.put("inventory",record.getInventory());

        //json.put("provenance",record.?);

        /* add linked records (extents, dates, rights statement) */

        // add the extent array containing one object or many depending if we using multiple extents
        JSONArray extentJA = new JSONArray();
        JSONObject extentJS = new JSONObject();

        extentJS.put("portion", "whole");

        if (record.getExtentNumber() != null) {
            extentJS.put("number", record.getExtentNumber().toString());
        } else {
            extentJS.put("number", "0");
        }

        extentJS.put("extent_type", enumUtil.getASpaceExtentType(record.getExtentType()));
        extentJS.put("container_summary", record.getContainerSummary());
        extentJA.put(extentJS);

        Set<ArchDescriptionPhysicalDescriptions> physicalDescriptions = record.getPhysicalDesctiptions();
        convertPhysicalDescriptions(extentJA, physicalDescriptions);

        if(extentJA.length() > 0) {
            json.put("extents", extentJA);
        }

        // convert and add any accessions related dates here
        JSONArray dateJA = new JSONArray();

        // add the bulk dates
        addDate(dateJA, record, "other", "Accession: " + record.getAccessionNumber());

        // add the archdescription dates now
        Set<ArchDescriptionDates> archDescriptionDates = record.getArchDescriptionDates();
        convertArchDescriptionDates(dateJA, archDescriptionDates);

        // if there are any dates add them to the main json record
        if(dateJA.length() != 0) {
            json.put("dates", dateJA);
        }

        // add external documents
        JSONArray externalDocumentsJA = new JSONArray();
        Set<ExternalReference> externalDocuments = record.getRepeatingData(ExternalReference.class);

        if(externalDocuments != null && externalDocuments.size() != 0) {
            convertExternalDocuments(externalDocumentsJA, externalDocuments);
            json.put("external_documents", externalDocumentsJA);
        }

        // add the deaccessions
        Set<Deaccessions> deaccessions = record.getDeaccessions();
        if(deaccessions != null && deaccessions.size() != 0) {
            JSONArray deaccessionsJA = new JSONArray();
            convertDeaccessions(deaccessionsJA, deaccessions);
            json.put("deaccessions", deaccessionsJA);
        }

        // add a rights statement object
        addRightsStatementRecord(record, json);

        // add the collection management record now
        addCollectionManagementRecord(record, json);

        json.put("suppressed", record.getInternalOnly());

        json.put("acquisition_type", enumUtil.getASpaceAcquisitionType(record.getAcquisitionType()));

        json.put("resource_type", enumUtil.getASpaceAccessionResourceType(record.getResourceType()));

        json.put("restrictions_apply", record.getRestrictionsApply());

        json.put("retention_rule", record.getRetentionRule());

        json.put("general_note", record.getGeneralAccessionNote());

        json.put("access_restrictions", record.getAccessRestrictions());

        json.put("use_restrictions_note", record.getAccessRestrictionsNote());

        // add the user defined fields here
        addUserDefinedFields(json, record);

        return json;
    }

    /**
     * Method to add a right statement object
     *
     * @param record
     * @param json
     */
    private void addRightsStatementRecord(Accessions record, JSONObject json) throws Exception {
        if(record.getRightsTransferred() == null || !record.getRightsTransferred()) return;

        JSONArray rightsStatementJA = new JSONArray();
        JSONObject rightStatementJS = new JSONObject();
        rightStatementJS.put("rights_type", "intellectual_property");
        rightStatementJS.put("ip_status", "copyrighted");
        rightStatementJS.put("jurisdiction", "US"); // TODO 8/12/2013 this is not suppose to be required
        rightsStatementJA.put(rightStatementJS);
        json.put("rights_statements", rightsStatementJA);
    }

    /**
     * Method to get an event object Accession processed info
     *
     *
     * @param accession
     * @param accessionURI
     * @param agentURI
     * @return
     */
    public ArrayList<JSONObject> getAccessionEvents(Accessions accession, String agentURI, String accessionURI) throws Exception {
        ArrayList<JSONObject> eventsList = new ArrayList<JSONObject>();
        JSONObject eventJS;

        // grab the accession date incase we need a date for an event
        Date accessionDate = accession.getAccessionDate();

        if(accession.getAccessionProcessed() != null && accession.getAccessionProcessed()) {
            eventJS = new JSONObject();
            eventJS.put("event_type", "processed");
            addEventDate(eventJS, accession.getAccessionProcessedDate(), accessionDate, "single", "other");
            addEventLinkedRecordAndAgent(eventJS, agentURI, accessionURI);
            eventsList.add(eventJS);
        }

        if(accession.getAcknowledgementSent() != null && accession.getAcknowledgementSent()) {
            eventJS = new JSONObject();
            eventJS.put("event_type", "acknowledgement_sent");
            addEventDate(eventJS, accession.getAcknowledgementDate(), accessionDate, "single", "other");
            addEventLinkedRecordAndAgent(eventJS, agentURI, accessionURI);
            eventsList.add(eventJS);
        }

        if(accession.getAgreementReceived() != null && accession.getAgreementReceived()) {
            eventJS = new JSONObject();
            eventJS.put("event_type", "agreement_signed");
            addEventDate(eventJS, accession.getAgreementReceivedDate(), accessionDate, "single", "other");
            addEventLinkedRecordAndAgent(eventJS, agentURI, accessionURI);
            eventsList.add(eventJS);
        }

        if(accession.getAgreementSent() != null && accession.getAgreementSent()) {
            eventJS = new JSONObject();
            eventJS.put("event_type", "agreement_sent");
            addEventDate(eventJS, accession.getAgreementSentDate(), accessionDate, "single", "other");
            addEventLinkedRecordAndAgent(eventJS, agentURI, accessionURI);
            eventsList.add(eventJS);
        }

        if(accession.getCataloged() != null && accession.getCataloged()) {
            eventJS = new JSONObject();
            eventJS.put("event_type", "cataloged");
            addEventDate(eventJS, accession.getCatalogedDate(), accessionDate, "single", "other");
            addEventLinkedRecordAndAgent(eventJS, agentURI, accessionURI);
            eventsList.add(eventJS);
        }

        if(accession.getProcessingStartedDate() != null) {
            eventJS = new JSONObject();
            eventJS.put("event_type", "processing_started");
            addEventDate(eventJS, accession.getProcessingStartedDate(), accessionDate, "single", "other");
            addEventLinkedRecordAndAgent(eventJS, agentURI, accessionURI);
            eventsList.add(eventJS);
        }

        if(accession.getRightsTransferred() != null && accession.getRightsTransferred()) {
            eventJS = new JSONObject();
            eventJS.put("event_type", "copyright_transfer");
            eventJS.put("outcome_note", accession.getRightsTransferredNote());
            addEventDate(eventJS, accession.getRightsTransferredDate(), accessionDate, "single", "other");
            addEventLinkedRecordAndAgent(eventJS, agentURI, accessionURI);
            eventsList.add(eventJS);
        }

        return eventsList;
    }

        /**
     * Method to add a date object
     *
     * @param eventJS
     * @param date
     * @param dateType
     * @param dateLabel
     */
    private void addEventDate(JSONObject eventJS, Date date, Date alternativeDate, String dateType, String dateLabel) throws Exception {
        // see if to use the alternative date instead since a date is required
        if(date == null) {
            date = alternativeDate;
        }

        JSONObject dateJS = new JSONObject();
        dateJS.put("date_type", dateType);
        dateJS.put("label", dateLabel);
        dateJS.put("begin", date.toString());
        dateJS.put("end", date.toString());

        eventJS.put("date", dateJS);
    }

    /**
     * Method to add the event linked record
     *
     * @param uri
     * @param eventJS
     * @throws Exception
     */
    private void addEventLinkedRecordAndAgent(JSONObject eventJS, String agentURI, String uri) throws Exception {
        // add a dummy linked agent so record can save
        JSONArray linkedAgentsJA = new JSONArray();
        JSONObject linkedAgentJS = new JSONObject();

        linkedAgentJS.put("role", "recipient");
        linkedAgentJS.put("ref", agentURI);
        linkedAgentsJA.put(linkedAgentJS);

        eventJS.put("linked_agents", linkedAgentsJA);

        // add the linked to the record
        JSONArray linkedRecordsJA = new JSONArray();
        JSONObject linkedRecordJS = new JSONObject();

        linkedRecordJS.put("role", "source");
        linkedRecordJS.put("ref", uri);
        linkedRecordsJA.put(linkedRecordJS);

        eventJS.put("linked_records", linkedRecordsJA);
    }

    /**
     * Method to convert physical descriptions object to an extent object
     *
     * @param extentJA
     * @param physicalDescriptions
     * @throws JSONException
     */
    public void convertPhysicalDescriptions(JSONArray extentJA, Set<ArchDescriptionPhysicalDescriptions> physicalDescriptions) throws JSONException {
        if(physicalDescriptions == null || physicalDescriptions.size() == 0) return;

        // TODO 12/10/2012 Archivists needs to map this
        for (ArchDescriptionPhysicalDescriptions physicalDescription : physicalDescriptions) {
            JSONObject extentJS = new JSONObject();
            extentJS.put("portion", "part");

            if(physicalDescription.getExtentNumber() != null) {
                extentJS.put("number", physicalDescription.getExtentNumber().toString());
            } else {
                extentJS.put("number", "1.0");
            }

            extentJS.put("extent_type", enumUtil.getASpaceExtentType(physicalDescription.getExtentType()));
            extentJS.put("container_summary", physicalDescription.getContainerSummary());
            extentJS.put("physical_details", physicalDescription.getPhysicalDetail());
            extentJS.put("dimensions", physicalDescription.getDimensions());
            extentJA.put(extentJS);
        }
    }

    /**
     * Method to convert arch description dates to date json objects
     * @param dateJA
     * @param archDescriptionDates
     */
    public void convertArchDescriptionDates(JSONArray dateJA, Set<ArchDescriptionDates> archDescriptionDates) throws JSONException {
        if(archDescriptionDates == null && archDescriptionDates.size() == 0) return;

        // TODO 12/10/2012 Archivists needs to map this
        for (ArchDescriptionDates archDescriptionDate: archDescriptionDates) {
            JSONObject dateJS = new JSONObject();
            dateJS.put("date_type", enumUtil.getASpaceDateType(archDescriptionDate));
            dateJS.put("label", enumUtil.getASpaceDateEnum(archDescriptionDate.getDateType()));
            dateJS.put("uncertain", enumUtil.getASpaceDateUncertainty(archDescriptionDate));

            String dateExpression = archDescriptionDate.getDateExpression();
            dateJS.put("expression", dateExpression);

            String beginDate = archDescriptionDate.getIsoDateBegin().trim();
            String endDate = archDescriptionDate.getIsoDateEnd().trim();

            if(!beginDate.isEmpty() && !beginDate.equals("0")) {
                dateJS.put("begin", beginDate);

                if(!endDate.isEmpty() && !endDate.equals("0")) {
                    dateJS.put("end", endDate);
                } else {
                    dateJS.put("end", beginDate);
                }
            } else if(dateExpression == null || dateExpression.isEmpty()) {
                // check that there is a date expression. If we have no expression then
                // we need to add one since we have no start date
                dateJS.put("expression", "unspecified");
            }

            dateJS.put("era", enumUtil.getASpaceDateEra(archDescriptionDate.getEra()));
            dateJS.put("calender", enumUtil.getASpaceDateCalender(archDescriptionDate.getCalendar()));

            dateJA.put(dateJS);
        }
    }

    /**
     * Method to convert external documents to the aspace external document object
     *
     * @param externalDocumentsJA
     * @param externalDocuments
     */
    public void convertExternalDocuments(JSONArray externalDocumentsJA, Set<ExternalReference> externalDocuments) throws JSONException {
        for (ExternalReference externalDocument: externalDocuments) {
            JSONObject documentJS = new JSONObject();
            documentJS.put("title", fixEmptyString(externalDocument.getTitle()));
            documentJS.put("location", fixUrl(externalDocument.getHref()));
            externalDocumentsJA.put(documentJS);
        }
    }

    /**
     * Method to convert the deaccessions records to equivalent json records
     *
     * @param deaccessionsJA
     * @param deaccessions
     */
    public void convertDeaccessions(JSONArray deaccessionsJA, Set<Deaccessions> deaccessions) throws JSONException {
        // TODO 12/10/2012 Archivists needs to map this
        for (Deaccessions deaccession: deaccessions) {
            JSONObject deaccessionJS = new JSONObject();
            deaccessionJS.put("scope", "part");
            deaccessionJS.put("description", deaccession.getDescription());
            deaccessionJS.put("reason", deaccession.getReason());
            deaccessionJS.put("disposition", deaccession.getDisposition());
            deaccessionJS.put("notification", deaccession.getNotification());

            // add the date object
            JSONObject dateJS = new JSONObject();

            dateJS.put("date_type", "single");
            dateJS.put("label", "deaccession");
            dateJS.put("expression", deaccession.getDeaccessionDate().toString());
            dateJS.put("begin", deaccession.getDeaccessionDate().toString()); // This should not be needed
            dateJS.put("era", "ce");
            dateJS.put("calender", "gregorian");

            deaccessionJS.put("date", dateJS);

            // add the extent array object
            if(deaccession.getExtent() != null) {
                JSONArray extentJA = new JSONArray();
                JSONObject extentJS = new JSONObject();

                extentJS.put("portion", "whole");
                extentJS.put("number", deaccession.getExtent().toString());
                extentJS.put("extent_type", enumUtil.getASpaceExtentType(deaccession.getExtentType()));
                extentJS.put("container_summary", deaccession.getDescription());

                extentJA.put(extentJS);
                deaccessionJS.put("extents", extentJA);
            }

            // add this deaccession object to array
            deaccessionsJA.put(deaccessionJS);
        }
    }

    /**
     * Method to return a collection management record object from an accession
     *
     * @param record
     * @param recordJS
     * @return
     * @throws Exception
     */
    public void addCollectionManagementRecord(Accessions record, JSONObject recordJS) throws Exception {
        // Main json object
        JSONObject json = new JSONObject();

        json.put("cataloged_note", record.getCatalogedNote());
        json.put("processing_plan", record.getProcessingPlan());

        if(record.getProcessingPriority() != null && !record.getProcessingPriority().isEmpty()) {
            json.put("processing_priority", enumUtil.getASpaceCollectionManagementRecordProcessingPriority(record.getProcessingPriority()));
        }

        if(record.getProcessingStatus() != null && !record.getProcessingStatus().isEmpty()) {
            json.put("processing_status", enumUtil.getASpaceCollectionManagementRecordProcessingStatus(record.getProcessingStatus()));
        }

        json.put("processors", record.getProcessors());
        json.put("rights_determined", record.getRightsTransferred()); //TODO 12/11/2012 archivist must map

        recordJS.put("collection_management", json);
    }

    /**
     * Method to add user defined fields from Accessions, Resource etc to the JSON object
     *
     * @param json
     * @param domainObject
     */
    public void addUserDefinedFields(JSONObject json, DomainObject domainObject) throws Exception {
        JSONObject userDefinedJS = new JSONObject();

        if (domainObject instanceof Accessions) {
            Accessions record = (Accessions) domainObject;

            userDefinedJS.put("boolean_1", record.getUserDefinedBoolean1());
            userDefinedJS.put("boolean_2", record.getUserDefinedBoolean2());

            if(record.getUserDefinedInteger1() != null) userDefinedJS.put("integer_1", record.getUserDefinedInteger1().toString());
            if(record.getUserDefinedInteger2() != null) userDefinedJS.put("integer_2", record.getUserDefinedInteger2().toString());

            if(record.getUserDefinedReal1() != null) userDefinedJS.put("real_1", record.getUserDefinedReal1().toString());
            if(record.getUserDefinedReal2() != null) userDefinedJS.put("real_2", record.getUserDefinedReal2().toString());

            userDefinedJS.put("string_1", record.getUserDefinedString1());
            userDefinedJS.put("string_2", record.getUserDefinedString2());
            userDefinedJS.put("string_3", record.getUserDefinedString3());

            userDefinedJS.put("text_1", record.getUserDefinedText1());
            userDefinedJS.put("text_2", record.getUserDefinedText2());
            userDefinedJS.put("text_3", record.getUserDefinedText3());
            userDefinedJS.put("text_4", record.getUserDefinedText4());

            if(record.getUserDefinedDate1() != null) userDefinedJS.put("date_1", record.getUserDefinedDate1());
            if(record.getUserDefinedDate2() != null) userDefinedJS.put("date_2", record.getUserDefinedDate2());
        } else if (domainObject instanceof Resources) {
            Resources record = (Resources) domainObject;

            userDefinedJS.put("string_1", record.getUserDefinedString1());
            userDefinedJS.put("string_2", record.getUserDefinedString2());
        } else if (domainObject instanceof ArchDescriptionAnalogInstances) {
            ArchDescriptionAnalogInstances record = (ArchDescriptionAnalogInstances) domainObject;

            userDefinedJS.put("boolean_1", record.getUserDefinedBoolean1());
            userDefinedJS.put("boolean_2", record.getUserDefinedBoolean2());
            userDefinedJS.put("string_1", record.getUserDefinedString1());
            userDefinedJS.put("string_2", record.getUserDefinedString2());
        } else {
            return; // Record doesn't have user defined fields so just return
        }

        json.put("user_defined", userDefinedJS);
    }

    /**
     * Method to convert a digital object record
     *
     * @param record
     * @return
     */
    public JSONObject convertDigitalObject(DigitalObjects record) throws Exception {
        // Main json object
        JSONObject json = new JSONObject();

        // add the AT database Id as an external ID
        addExternalId(record, json, "digital_object");

        /* add the fields required for abstract_archival_object.rb */

        String title = record.getObjectLabel();
        json.put("title", fixEmptyString(title));

        addLanguageCode(json, record.getLanguageCode());

        // add the date object
        JSONArray dateJA = new JSONArray();
        addDate(dateJA, record, "digitized", "Digital Object: " + record.getMetsIdentifier());

        if(dateJA.length() != 0) {
            json.put("dates", dateJA);
        }

        /* add the fields required digital_object.rb */

        JSONArray fileVersionsJA = new JSONArray();
        convertFileVersions(fileVersionsJA, record.getFileVersions());
        json.put("file_versions", fileVersionsJA);

        json.put("digital_object_id", getUniqueID(ASpaceClient.DIGITAL_OBJECT_ENDPOINT, record.getMetsIdentifier(), null));

        // set the digital object type
        String type = record.getObjectType();
        if(type != null && !type.isEmpty()) {
            json.put("digital_object_type", enumUtil.getASpaceDigitalObjectType(type));
        }

        // set the restrictions apply
        json.put("restrictions", record.getRestrictionsApply());

        // add the notes
        JSONArray notesJA = new JSONArray();
        addNotes(notesJA, record);
        json.put("notes", notesJA);

        return json;
    }

    /**
     * Method to convert a digital object record into a aspace digital object component
     *
     * @param record
     * @return
     */
    public JSONObject convertToDigitalObjectComponent(DigitalObjects record) throws Exception {
        // Main json object
        JSONObject json = new JSONObject();

        /* add the fields required for abstract_archival_object.rb */
        String title = record.getObjectLabel();
        json.put("title", fixEmptyString(title));

        addLanguageCode(json, record.getLanguageCode());

        /* add fields required for digital object component*/
        JSONArray fileVersionsJA = new JSONArray();
        convertFileVersions(fileVersionsJA, record.getFileVersions());
        json.put("file_versions", fileVersionsJA);

        String label = record.getLabel();
        json.put("label", label);

        json.put("component_id", fixEmptyString(record.getComponentId(), "ID_" + randomString.nextString()));

        // add the date object
        JSONArray dateJA = new JSONArray();
        addDate(dateJA, record, "digitized", "Digital Object Component: " + record.getComponentId());

        if(dateJA.length() != 0) {
            json.put("dates", dateJA);
        }

        // add the notes
        JSONArray notesJA = new JSONArray();
        addNotes(notesJA, record);
        json.put("notes", notesJA);

        return json;
    }

    /**
     * Method to convert external documents to the aspace external document object
     *
     * @param fileVersionsJA
     * @param fileVersionSet
     */
    public void convertFileVersions(JSONArray fileVersionsJA, Set<FileVersions> fileVersionSet) throws JSONException {
        for (FileVersions fileVersion: fileVersionSet) {
            JSONObject fileVersionJS = new JSONObject();

            fileVersionJS.put("file_uri", fileVersion.getUri());
            fileVersionJS.put("use_statement", enumUtil.getASpaceFileVersionUseStatement(fileVersion.getUseStatement()));
            fileVersionJS.put("xlink_actuate_attribute", fileVersion.getEadDaoActuate());
            fileVersionJS.put("xlink_show_attribute", fileVersion.getEadDaoShow());

            fileVersionsJA.put(fileVersionJS);
        }
    }

    /**
     * Method to convert an resource record to json ASpace JSON
     *
     * @param record
     * @return
     * @throws Exception
     */
    public JSONObject convertResource(Resources record) throws Exception {
        // Main json object
        JSONObject json = new JSONObject();

        // add the AT database Id as an external ID
        addExternalId(record, json, "resource");

        /* Add fields needed for abstract_archival_object.rb */

        // check to make sure we have a title
        String title = fixEmptyString(record.getTitle());
        json.put("title", title);

        // add the language code
        addLanguageCode(json, record.getLanguageCode());

        // add the extent array containing one object or many depending if we using multiple extents
        JSONArray extentJA = new JSONArray();
        JSONObject extentJS = new JSONObject();

        extentJS.put("portion", "whole");

        if (record.getExtentNumber() != null) {
            extentJS.put("number", record.getExtentNumber().toString());
        } else {
            extentJS.put("number", "0");
        }

        extentJS.put("extent_type", enumUtil.getASpaceExtentType(record.getExtentType()));
        extentJS.put("container_summary", record.getContainerSummary());
        extentJA.put(extentJS);

        Set<ArchDescriptionPhysicalDescriptions> physicalDescriptions = record.getPhysicalDesctiptions();
        convertPhysicalDescriptions(extentJA, physicalDescriptions);

        json.put("extents", extentJA);

        // add the date array containing the dates json objects
        JSONArray dateJA = new JSONArray();

        addDate(dateJA, record, "other", "Resource: " + currentResourceRecordIdentifier);

        Set<ArchDescriptionDates> archDescriptionDates = record.getArchDescriptionDates();
        convertArchDescriptionDates(dateJA, archDescriptionDates);

        if(dateJA.length() != 0) {
            json.put("dates", dateJA);
        }

        // add external documents
        JSONArray externalDocumentsJA = new JSONArray();
        Set<ExternalReference> externalDocuments = record.getRepeatingData(ExternalReference.class);

        if(externalDocuments != null && externalDocuments.size() != 0) {
            convertExternalDocuments(externalDocumentsJA, externalDocuments);
            json.put("external_documents", externalDocumentsJA);
        }

        /* Add fields needed for resource.rb */

        // get the ids and make them unique if we in DEBUG mode
        String[] cleanIds = cleanUpIds(ASpaceClient.RESOURCE_ENDPOINT, record.getResourceIdentifier1().trim(),
                record.getResourceIdentifier2().trim(), record.getResourceIdentifier3().trim(),
                record.getResourceIdentifier4().trim());

        String id_0 = cleanIds[0];
        String id_1 = cleanIds[1];
        String id_2 = cleanIds[2];
        String id_3 = cleanIds[3];

        if(makeUnique) {
            id_0 = randomString.nextString();
            id_1 = randomString.nextString();
            id_2 = randomString.nextString();
            id_3 = randomString.nextString();
        }

        json.put("id_0", id_0);
        json.put("id_1", id_1);
        json.put("id_2", id_2);
        json.put("id_3", id_3);

        // get the level
        String level = enumUtil.getASpaceResourceLevel(record.getLevel());
        json.put("level", level);

        if(level.equals("otherlevel")) {
            json.put("other_level", fixEmptyString(record.getOtherLevel()));
        }

        // set the publish, restrictions, processing note, container summary
        json.put("publish", record.getInternalOnly());
        //json.put("restrictions", record.getRestrictionsApply());
        json.put("repository_processing_note", record.getRepositoryProcessingNote());
        json.put("container_summary", record.getContainerSummary());

        // add fields for EAD
        json.put("ead_id", getUniqueID("ead", record.getEadFaUniqueIdentifier(), null));
        json.put("ead_location", record.getEadFaLocation());
        json.put("finding_aid_title", record.getFindingAidTitle() + "\n" + record.getFindingAidSubtitle());
        json.put("finding_aid_date", record.getFindingAidDate());
        json.put("finding_aid_author", record.getAuthor());

        if(record.getDescriptionRules() != null) {
            json.put("finding_aid_description_rules", enumUtil.getASpaceFindingAidDescriptionRule(record.getDescriptionRules()));
        }

        json.put("finding_aid_language", record.getLanguageOfFindingAid());
        json.put("finding_aid_sponsor", record.getSponsorNote());
        json.put("finding_aid_edition_statement", record.getEditionStatement());
        json.put("finding_aid_series_statement", record.getSeries());
        json.put("finding_aid_revision_date", record.getRevisionDate());
        json.put("finding_aid_revision_description", record.getRevisionDescription());

        if(record.getFindingAidStatus() != null) {
            json.put("finding_aid_status", enumUtil.getASpaceFindingAidStatus(record.getFindingAidStatus()));
        }

        json.put("finding_aid_note", record.getFindingAidNote());

        // add the deaccessions
        JSONArray deaccessionsJA = new JSONArray();
        Set<Deaccessions> deaccessions = record.getDeaccessions();

        if(deaccessions != null && deaccessions.size() != 0) {
            convertDeaccessions(deaccessionsJA, deaccessions);
            json.put("deaccessions", deaccessionsJA);
        }

        // add the notes
        JSONArray notesJA = new JSONArray();
        addNotes(notesJA, record);
        json.put("notes", notesJA);

        return json;
    }

    /**
     * Method to convert a resource record into an archival object
     *
     * @param record
     * @return
     */
    private JSONObject convertResourceComponent(ResourcesComponents record) throws Exception {
        // Main json object
        JSONObject json = new JSONObject();

        // add the AT database Id as an external ID
        addExternalId(record, json, "resource_component");

        /* Add fields needed for abstract_archival_object.rb */

        // check to make sure we have a title
        String title = record.getTitle();
        json.put("title", title);

        // add the language code
        addLanguageCode(json, record.getLanguageCode());

        // add the date array containing the date json objects
        JSONArray dateJA = new JSONArray();

        String recordIdentifier = "Resource Component: " + currentResourceRecordIdentifier + "/"  + record.getPersistentId();
        addDate(dateJA, record, "other", recordIdentifier);

        Set<ArchDescriptionDates> archDescriptionDates = record.getArchDescriptionDates();
        convertArchDescriptionDates(dateJA, archDescriptionDates);

        if(dateJA.length() != 0) {
            json.put("dates", dateJA);
        } else if(title.isEmpty()) {
            json.put("title", "unspecified");
        }

        /* add field required for archival_object.rb */

        // make the ref id unique otherwise ASpace complains
        String refId = record.getPersistentId() + "_" + randomString.nextString();
        json.put("ref_id", refId);

        String level = enumUtil.getASpaceArchivalObjectLevel(record.getLevel());
        json.put("level", level);

        if(level.equals("otherlevel")) {
            json.put("other_level", fixEmptyString(record.getOtherLevel()));
        }

        if(record.getComponentUniqueIdentifier() != null && !record.getComponentUniqueIdentifier().isEmpty()) {
            json.put("component_id", record.getComponentUniqueIdentifier());
        }

        json.put("position", record.getSequenceNumber());

        // add the notes
        JSONArray notesJA = new JSONArray();
        addNotes(notesJA, record);
        json.put("notes", notesJA);

        // add the extent array containing one object or many depending if we using multiple extents
        JSONArray extentJA = new JSONArray();

        if(!record.getExtentType().isEmpty()) {
            JSONObject extentJS = new JSONObject();

            extentJS.put("portion", "whole");

            if(record.getExtentNumber() != null) {
                extentJS.put("number", record.getExtentNumber().toString());
            } else {
                extentJS.put("number", "0");
            }

            extentJS.put("extent_type", enumUtil.getASpaceExtentType(record.getExtentType()));
            extentJS.put("container_summary", record.getContainerSummary());
            extentJA.put(extentJS);
        } else if(!record.getContainerSummary().isEmpty()) {
            // some groups only put information in the container summary field
            // so place this has a note
            addNoteForContainerSummary(notesJA, record.getContainerSummary());
        }

        Set<ArchDescriptionPhysicalDescriptions> physicalDescriptions = record.getPhysicalDesctiptions();
        convertPhysicalDescriptions(extentJA, physicalDescriptions);

        if(extentJA.length() > 0) {
            json.put("extents", extentJA);
        }

        return json;
    }

    /**
     * Method to set the language code for a json record
     *
     * @param json
     * @param languageCode
     * @throws Exception
     */
    public void addLanguageCode(JSONObject json, String languageCode) throws Exception {
        if(languageCode != null && !languageCode.isEmpty()) {
            json.put("language", enumUtil.getASpaceLanguageCode(languageCode));
        }
    }

    /**
     * Method to add a date json object
     *
     * @param dateJA
     * @param record
     */
    public void addDate(JSONArray dateJA, ArchDescription record, String dateLabel, String recordIdentifier) throws Exception {
        JSONObject dateJS = new JSONObject();

        dateJS.put("date_type", "single");

        dateJS.put("label", dateLabel);

        String dateExpression = record.getDateExpression();
        dateJS.put("expression", dateExpression);

        Integer dateBegin = record.getDateBegin();
        Integer dateEnd = record.getDateEnd();

        if (dateBegin != null) {
            dateJS.put("date_type", "inclusive");

            dateJS.put("begin", dateBegin.toString());

            if (dateEnd != null) {
                if(dateEnd >= dateBegin) {
                    dateJS.put("end", dateEnd.toString());
                } else {
                    dateJS.put("end", dateBegin.toString());

                    String message = "End date: " + dateEnd + " before begin date: " + dateBegin + ", ignoring end date\n" + recordIdentifier;
                    aspaceCopyUtil.addErrorMessage(message);
                }
            } else {
                dateJS.put("end", dateBegin.toString());
            }
        }

        // see if to add this date now
        if((dateExpression != null && !dateExpression.isEmpty()) || dateBegin != null) {
            dateJA.put(dateJS);
        }

        // add the bulk dates begin and end if resource or resource component
        if(record instanceof AccessionsResourcesCommon) {
            AccessionsResourcesCommon resourcesCommon = (AccessionsResourcesCommon)record;
            Integer bulkDateBegin = resourcesCommon.getBulkDateBegin();
            Integer bulkDateEnd = resourcesCommon.getBulkDateEnd();

            if(bulkDateBegin != null) {
                dateJS = new JSONObject();
                dateJS.put("date_type", "bulk");
                dateJS.put("label", dateLabel);

                dateJS.put("begin", bulkDateBegin.toString());

                if (bulkDateEnd != null) {
                    if(bulkDateEnd >= bulkDateBegin) {
                        dateJS.put("end", bulkDateEnd.toString());
                    } else {
                        dateJS.put("end", bulkDateBegin.toString());

                        String message = "Bulk end date: " + bulkDateEnd + " before bulk begin date: " + bulkDateBegin + ", ignoring end date\n" + recordIdentifier;
                        aspaceCopyUtil.addErrorMessage(message);
                    }
                }

                dateJA.put(dateJS);
            }
        }
    }

    /**
     * Method to concat all the contact notes into a single note and add it to the
     * contactJS object.  This has the potential to throw a truncation error
     *
     * @param contactJS
     * @param contactNotes
     */
    public void addNote(JSONObject contactJS, Set<NameContactNotes> contactNotes) throws JSONException {
         if(contactNotes != null && contactNotes.size() > 0) {
             StringBuilder sb = new StringBuilder();

             for(NameContactNotes contactNote: contactNotes) {
                 String label = contactNote.getLabel();
                 String content = contactNote.getNoteText();

                 if(!label.isEmpty()) {
                     sb.append("Label: ").append(label).append("\n");
                 }

                 sb.append("Content: \n").append(content).append("\n\n");
             }

             contactJS.put("note", sb.toString());
         }
    }

    /**
     * Method to add a bioghist note agent object
     *
     * @param agentJS
     * @param record
     * @throws Exception
     */
    public void addNote(JSONObject agentJS, Names record) throws Exception {
        if (record.getDescriptionNote().isEmpty()) return;

        JSONArray notesJA = new JSONArray();
        JSONObject noteJS = new JSONObject();

        noteJS.put("jsonmodel_type", "note_bioghist");
        noteJS.put("label", enumUtil.getASpaceNameDescriptionType(record.getDescriptionType()));

        JSONArray subnotesJA = new JSONArray();

        JSONObject textNoteJS = new JSONObject();
        addTextNote(textNoteJS, record.getDescriptionNote());
        subnotesJA.put(textNoteJS);

        // add a subnote which holds the citation information
        if(record.getCitation() != null && !record.getCitation().isEmpty()) {
            JSONObject citationJS = new JSONObject();
            citationJS.put("jsonmodel_type", "note_citation");
            JSONArray contentJA = new JSONArray();
            contentJA.put(record.getCitation());
            citationJS.put("content", contentJA);
            subnotesJA.put(citationJS);
        }

        noteJS.put("subnotes", subnotesJA);
        notesJA.put(noteJS);
        agentJS.put("notes", notesJA);
    }

    /**
     * Method to add Notes to a json array
     *
     * @param notesJA
     * @param record
     */
    public void addNotes(JSONArray notesJA, ArchDescription record) throws Exception {
        Set<ArchDescriptionNotes> notes  = record.getRepeatingData(ArchDescriptionNotes.class);
        Set<ArchDescriptionStructuredData> structuredNotes = record.getRepeatingData(ArchDescriptionStructuredData.class);

        // process the none structured notes
        for(ArchDescriptionNotes note: notes) {
            // add the content for abstract_note.rb
            JSONObject noteJS = new JSONObject();

            noteJS.put("label", note.getTitle());

            // based on the note and record type, add the correct note
            String noteType = "";
            if(note.getNotesEtcType() != null) {
                noteType = note.getNotesEtcType().getNotesEtcName();
            }

            // create a content array incase we need need it for a note
            JSONArray contentJA = new JSONArray();
            contentJA.put(fixEmptyString(note.getContent(), "no content"));

            if(record instanceof DigitalObjects) {
                noteJS.put("jsonmodel_type", "note_digital_object");
                noteType = enumUtil.getASpaceDigitalObjectNoteType(noteType);
                noteJS.put("type", noteType);
                noteJS.put("content", contentJA);
            } else if(note.getMultiPart() != null && note.getMultiPart()) {
                addMultiPartNote(noteJS, note);
            } else {
                // even though it could be a single part note, based on the type it
                // needs to be a multi part note in ASpace
                noteType = enumUtil.getASpaceSinglePartNoteType(noteType);

                if(noteType.equals(ASpaceEnumUtil.UNMAPPED)) {
                    addMultiPartNote(noteJS, note);
                } else {
                    noteJS.put("jsonmodel_type", "note_singlepart");
                    noteJS.put("type", noteType);
                    noteJS.put("content", contentJA);
                }
            }

            notesJA.put(noteJS);
        }

        // process the structured notes
        for(ArchDescriptionStructuredData note: structuredNotes) {
            // add the content for abstract_note.rb
            JSONObject noteJS = new JSONObject();

            noteJS.put("label", note.getTitle());

            JSONArray contentJA = new JSONArray();
            contentJA.put(fixEmptyString(note.getContent(), "no content"));
            noteJS.put("content", contentJA);

            if(note instanceof Bibliography) {
                addBibliographyNote(noteJS, (Bibliography)note);
            } else if(note instanceof Index) {
                addIndexNote(noteJS, (Index)note);
            }

            notesJA.put(noteJS);
        }
    }

     /**
     * Add a multipart note
     *
     * @param noteJS
     * @param note
     * @throws Exception
     */
    private void addMultiPartNote(JSONObject noteJS, ArchDescriptionNotes note) throws Exception {
        // get the note type
        String noteType = "";
        if(note.getNotesEtcType() != null) {
            noteType = note.getNotesEtcType().getNotesEtcName();
        }

        // create the parent json object of this note
        noteJS.put("jsonmodel_type", "note_multipart");
        noteJS.put("type", enumUtil.getASpaceMultiPartNoteType(noteType));

        JSONArray subnotesJA = new JSONArray();

        // add the default text note
        JSONObject textNoteJS = new JSONObject();
        addTextNote(textNoteJS, fixEmptyString(note.getContent(), "multi-part note content"));
        subnotesJA.put(textNoteJS);

        // add the sub notes now
        for(ArchDescriptionRepeatingData childNote: note.getChildren()) {
            JSONObject subnoteJS = new JSONObject();

            if(childNote instanceof Bibliography) {
                addBibliographyNote(subnoteJS, (Bibliography)childNote);
            } else if(childNote instanceof ChronologyList) {
                addChronologyNote(subnoteJS, (ChronologyList)childNote);
            } else if(childNote instanceof Index) {
                addIndexNote(subnoteJS, (Index)childNote);
            } else if(childNote instanceof ListOrdered) {
                addOrderedListNote(subnoteJS, (ListOrdered)childNote);
            } else if(childNote instanceof ListDefinition) {
                addDefinedListNote(subnoteJS, (ListDefinition)childNote);
            } else { // must be text note
                addTextNote(subnoteJS, childNote.getContent());
            }

            subnotesJA.put(subnoteJS);
        }

        noteJS.put("subnotes", subnotesJA);
    }

    /**
     * Method to add an ordered list note
     *
     * @param noteJS
     * @param listOrdered
     * @throws Exception
     */
    private void addOrderedListNote(JSONObject noteJS, ListOrdered listOrdered) throws Exception {
        noteJS.put("jsonmodel_type", "note_orderedlist");
        noteJS.put("title", fixEmptyString(listOrdered.getTitle(), "Missing Title"));
        noteJS.put("enumeration", enumUtil.getASpaceOrderedListNoteEnumeration(listOrdered.getNumeration()));

        JSONArray itemsJA = new JSONArray();

        for(ArchDescriptionStructuredDataItems item: listOrdered.getListItems()) {
            ListOrderedItems listItem = (ListOrderedItems)item;
            itemsJA.put(listItem.getItemValue());
        }

        noteJS.put("items", itemsJA);
    }

    /**
     * Method to add a defined list note
     *
     * @param noteJS
     * @param listDefinition
     * @throws Exception
     */
    private void addDefinedListNote(JSONObject noteJS, ListDefinition listDefinition) throws Exception {
        noteJS.put("jsonmodel_type", "note_definedlist");
        noteJS.put("title", fixEmptyString(listDefinition.getTitle(), "Missing Title"));

        JSONArray itemsJA = new JSONArray();

        for(ArchDescriptionStructuredDataItems item: listDefinition.getListItems()) {
            ListDefinitionItems listItem = (ListDefinitionItems)item;
            JSONObject itemJS = new JSONObject();

            itemJS.put("label", listItem.getLabel());
            itemJS.put("value", listItem.getItemValue());

            itemsJA.put(itemJS);
        }

        noteJS.put("items", itemsJA);
    }

    /**
     * Method to add a chronology note
     * @param noteJS
     * @throws Exception
     */
    private void addChronologyNote(JSONObject noteJS, ChronologyList chronologyList) throws Exception {
        noteJS.put("jsonmodel_type", "note_chronology");
        noteJS.put("title", fixEmptyString(chronologyList.getTitle(), "Missing Title"));

        noteJS.put("ingest_problem", chronologyList.getEadIngestProblem());

        JSONArray itemsJA = new JSONArray();

        for(ArchDescriptionStructuredDataItems item: chronologyList.getChronologyItems()) {
            ChronologyItems chronologyItem = (ChronologyItems)item;
            JSONObject itemJS = new JSONObject();

            itemJS.put("event_date", chronologyItem.getEventDate());

            // add the individual events now
            JSONArray eventsJA = new JSONArray();
            for(Events event: chronologyItem.getEvents()) {
                eventsJA.put(event.getEventDescription());
            }

            itemJS.put("events", eventsJA);
            itemsJA.put(itemJS);
        }

        noteJS.put("items", itemsJA);
    }

    /**
     * Method to add a bibliography note
     *
     * @param noteJS
     * @param note
     * @throws Exception
     */
    private void addBibliographyNote(JSONObject noteJS, Bibliography note) throws Exception {
        noteJS.put("jsonmodel_type", "note_bibliography");
    }

    /**
     * Method to add an index note
     *
     * @param noteJS
     * @param index
     */
    private void addIndexNote(JSONObject noteJS, Index index) throws Exception {
        noteJS.put("jsonmodel_type", "note_index");

        JSONArray itemsJA = new JSONArray();

        for(ArchDescriptionStructuredDataItems item: index.getIndexItems()) {
            IndexItems indexItem = (IndexItems)item;
            JSONObject itemJS = new JSONObject();

            itemJS.put("value", indexItem.getItemValue());
            itemJS.put("type", indexItem.getItemType());
            itemJS.put("reference", indexItem.getReference());
            //itemJS.put("reference", fixEmptyString(indexItem.getReference(), null));
            itemJS.put("reference_text", indexItem.getReferenceText());
            //itemJS.put("reference_text", fixEmptyString(indexItem.getReferenceText(), null));

            itemsJA.put(itemJS);
        }

        noteJS.put("items", itemsJA);
    }

    /**
     * Method to add a text note
     *
     * @param noteJS
     * @param content
     * @throws Exception
     */
    private void addTextNote(JSONObject noteJS, String content) throws Exception {
        noteJS.put("jsonmodel_type", "note_text");
        noteJS.put("content", content);
    }

    /**
     * Method to add a physical description note for container summary
     *
     * @param notesJA
     * @param containerSummary
     */
    private void addNoteForContainerSummary(JSONArray notesJA, String containerSummary) throws Exception {
        JSONObject noteJS = new JSONObject();

        noteJS.put("label", "Container Summary");

        JSONArray contentJA = new JSONArray();
        contentJA.put(containerSummary);

        noteJS.put("jsonmodel_type", "note_singlepart");
        noteJS.put("type", "physdesc");
        noteJS.put("content", contentJA);

        notesJA.put(noteJS);
    }

    /**
     * Method to convert an analog instance to an equivalent ASpace instance
     *
     * @param analogInstance
     * @param locationURI
     * @return
     * @throws Exception
     */
    public JSONObject convertAnalogInstance(ArchDescriptionAnalogInstances analogInstance, String locationURI) throws Exception {
        JSONObject instanceJS = new JSONObject();

        // add the AT database Id as an external ID
        addExternalId(analogInstance, instanceJS, "analog_instance");

        // set the type
        String type = enumUtil.getASpaceInstanceType(analogInstance.getInstanceType());
        instanceJS.put("instance_type", type);

        // add the container now
        JSONObject containerJS = new JSONObject();

        containerJS.put("type_1", enumUtil.getASpaceInstanceContainerType(analogInstance.getContainer1Type()));
        containerJS.put("indicator_1", fixEmptyString(analogInstance.getContainer1Indicator(), "not specified"));
        containerJS.put("barcode_1", analogInstance.getBarcode());

        if(!analogInstance.getContainer2Type().isEmpty()) {
            containerJS.put("type_2", enumUtil.getASpaceInstanceContainerType(analogInstance.getContainer2Type()));
            containerJS.put("indicator_2", analogInstance.getContainer2Indicator());
        }

        if(!analogInstance.getContainer3Type().isEmpty()) {
            containerJS.put("type_3", enumUtil.getASpaceInstanceContainerType(analogInstance.getContainer3Type()));
            containerJS.put("indicator_3", analogInstance.getContainer3Indicator());
        }

        // add the location now if needed
        if(locationURI != null && !locationURI.isEmpty()) {
            Date date = new Date(); // this is need to have valid container_location json record

            JSONArray locationsJA = new JSONArray();

            JSONObject locationJS = new JSONObject();
            locationJS.put("status", "current");
            locationJS.put("start_date", date);
            locationJS.put("ref", locationURI);

            locationsJA.put(locationJS);
            containerJS.put("container_locations", locationsJA);
        }

        // TODO 4/16/2013 add the user defined fields
        //addUserDefinedFields(containerJS, analogInstance);

        instanceJS.put("container", containerJS);

        return instanceJS;
    }

    /**
     * Method to convert a digital instance to a json record
     *
     * @param digitalObjectURI
     * @return
     * @throws Exception
     */
    public JSONObject convertDigitalInstance(String digitalObjectURI) throws Exception {
        JSONObject instanceJS = new JSONObject();

        if(digitalObjectURI == null || digitalObjectURI.isEmpty()) return null;

        instanceJS.put("instance_type", "digital_object");
        instanceJS.put("digital_object", getReferenceObject(digitalObjectURI));

        return instanceJS;
    }

    /**
     * Method to create a dummy instance to old the location information
     *
     *
     * @param accession
     * @param locationNote
     * @return
     * @throws Exception
     */
    public JSONObject createAccessionInstance(Accessions accession, String locationURI, String locationNote) throws Exception {
        JSONObject instanceJS = new JSONObject();

        // set the type
        instanceJS.put("instance_type", "accession");

        // add the container now
        JSONObject containerJS = new JSONObject();

        containerJS.put("type_1", "item");
        containerJS.put("indicator_1", accession.getAccessionNumber());

        Date date = new Date(); // this is need to have valid container_location json record
        JSONArray locationsJA = new JSONArray();

        JSONObject locationJS = new JSONObject();
        locationJS.put("status", "current");
        locationJS.put("start_date", date);
        locationJS.put("ref", locationURI);
        locationJS.put("note", locationNote);

        locationsJA.put(locationJS);

        containerJS.put("container_locations", locationsJA);
        instanceJS.put("container", containerJS);

        return instanceJS;
    }

    /**
     * Method to get a reference object which points to another URI
     *
     * @param recordURI
     * @return
     * @throws Exception
     */
    public JSONObject getReferenceObject(String recordURI) throws Exception {
        JSONObject referenceJS = new JSONObject();
        referenceJS.put("ref", recordURI);
        return referenceJS;
    }

    /**
     * Method to add the AT internal database ID as an external ID for the ASpace object
     *
     * @param record
     * @param source
     */
    public void addExternalId(DomainObject record, JSONObject recordJS, String source) throws Exception {
        source = "Archivists Toolkit Database::" + source.toUpperCase();

        JSONArray externalIdsJA = new JSONArray();
        JSONObject externalIdJS = new JSONObject();

        externalIdJS.put("external_id", record.getIdentifier().toString());
        externalIdJS.put("source", source);

        externalIdsJA.put(externalIdJS);

        recordJS.put("external_ids", externalIdsJA);
    }

    /**
     * Method to set the hash map that holds the dynamic enums
     *
     * @param dynamicEnums
     */
    public void setASpaceDynamicEnums(HashMap<String, JSONObject> dynamicEnums) {
        enumUtil.setASpaceDynamicEnums(dynamicEnums);
    }

    /**
     * set whether to return the AT value or UNMAPPED default
     *
     * @param value
     */
    public void setReturnATValue(boolean value) {
        enumUtil.returnATValue = value;
    }

    /**
     * This method is used to map AT lookup list values into a dynamic enum.
     *
     * @param lookupList
     * @return
     */
    public JSONObject mapLookList(LookupList lookupList) throws Exception {
        // first we get the correct dynamic enum based on list. If it null then we just return null
        JSONObject dynamicEnumJS = enumUtil.getDynamicEnum(lookupList.getListName());

        if(dynamicEnumJS == null) return null;

        // add any values to this list if needed
        String enumListName = dynamicEnumJS.getString("name");
        JSONArray valuesJA = dynamicEnumJS.getJSONArray("values");

        for (LookupListItems lookupListItem: lookupList.getListItems()) {
            String atValue = lookupListItem.getListItem();
            String code = lookupListItem.getCode();

            if(!enumUtil.mapsToASpaceEnumValue(enumListName, atValue, code)) {
                valuesJA.put(atValue.toLowerCase());
            }
        }

        return dynamicEnumJS;
    }



    /**
     * Method to map the ASpace group to one or more AT access classes
     *
     * @param groupJS
     * @return
     */
    public void mapAccessClass(HashMap<String, JSONObject> repositoryGroupURIMap,
                               JSONObject groupJS, String repoURI) {
        try {
            String groupCode = (String)groupJS.get("group_code");
            String key = "";

            if (groupCode.equals("administrators")) { // map to access class 5
                key = repoURI + ACCESS_CLASS_PREFIX + "5";
                repositoryGroupURIMap.put(key, groupJS);
            } else if(groupCode.equals("repository-managers")) { // map to access class 4
                key = repoURI + ACCESS_CLASS_PREFIX + "4";
                repositoryGroupURIMap.put(key, groupJS);
            } else if(groupCode.equals("repository-archivists")) { // map to access class 3
                key = repoURI + ACCESS_CLASS_PREFIX + "3";
                repositoryGroupURIMap.put(key, groupJS);
            } else if(groupCode.equals("repository-advanced-data-entry")) { // map to access class 2
                key = repoURI + ACCESS_CLASS_PREFIX + "2";
                repositoryGroupURIMap.put(key, groupJS);
            } else if(groupCode.equals("repository-basic-data-entry")) { // map to access class 1
                key = repoURI + ACCESS_CLASS_PREFIX + "1";
                repositoryGroupURIMap.put(key, groupJS);
            } else if (groupCode.equals("repository-viewers")) { // map access class to access class 0 for now
                key = repoURI + ACCESS_CLASS_PREFIX + "0";
                repositoryGroupURIMap.put(key, groupJS);
            }
        } catch (JSONException e) {
            e.printStackTrace();
        }
    }

    /**
     * Method to prepend http:// to a url to prevent ASpace from complaining
     *
     * @param url
     * @return
     */
    private String fixUrl(String url) {
        if(url.isEmpty()) return "http://url.unspecified";

        String lowercaseUrl = url.toLowerCase();

        // check to see if its a proper uri format
        if(lowercaseUrl.contains("://")) {
            return url;
        } else if(lowercaseUrl.startsWith("/") || lowercaseUrl.contains(":\\")) {
            url = "file://" + url;
            return url;
        } else {
            url = "http://" + url;
            return  url;
        }
    }

    /**
     * Method to set a string that's empty to "unspecified"
     * @param text
     * @return
     */
    public String fixEmptyString(String text) {
        return fixEmptyString(text, null);
    }

    /**
     * Method to set a string that empty to "not set"
     * @param text
     * @return
     */
    private String fixEmptyString(String text, String useInstead) {
        if(text == null || text.trim().isEmpty()) {
            if(useInstead == null) {
                return "unspecified";
            } else {
                return useInstead;
            }
        } else {
            return text;
        }
    }

    /**
     * Method to truncate a string to a certain length
     *
     * @param text
     * @param maxLength
     * @return
     */
    private String truncateString(String text, int maxLength) {
        if(!allowTruncation) {
            return text;
        } else if(text.length() <= maxLength) {
            return text;
        } else {
            return text.substring(0, (maxLength -3)) + "...";
        }
    }

    /**
     * Method to set the language codes
     *
     * @param languageCodes
     */
    public void setLanguageCodes(HashMap<String, String> languageCodes) {
        enumUtil.setLanguageCodes(languageCodes);
    }

    /**
     * Method to set the name link creator codes
     *
     * @param nameLinkCreatorCodes
     */
    public void setNameLinkCreatorCodes(HashMap<String, String> nameLinkCreatorCodes) {
        enumUtil.setNameLinkCreatorCodes(nameLinkCreatorCodes);
    }

    /**
     * This functions shift the ids so that we don't have any blanks. The ASpace backend
     * throws an error if there are any blanks.
     *
     * @param id1
     * @param id2
     * @param id3
     * @param id4
     * @return
     */
    private String[] cleanUpIds(String recordType, String id1, String id2, String id3, String id4) {
        String[] ids = new String[]{"","","",""};
        int index = 0;

        // keeps track of whether an ID was shifted
        boolean shifted = false;

        if(!id1.isEmpty()) {
            ids[index] = id1;
            index++;
        }

        if(!id2.isEmpty()) {
            if(index < 1) { shifted = true; }
            ids[index] = id2;
            index++;
        }

        if(!id3.isEmpty()) {
            if(index < 2) { shifted = true; }
            ids[index] = id3;
            index++;
        }

        if(!id4.isEmpty()) {
            if(index < 3) { shifted = true; }
            ids[index] = id1;
        }

        // check to see if this id is unique, if it isn't then make it so
        getUniqueID(recordType, concatIdParts(ids), ids);

        // report any corrections
        if(shifted) {
            String message;
            String fullId = concatIdParts(ids);

            if(recordType.equals(ASpaceClient.ACCESSION_ENDPOINT)) {
                message = "Accession Id Cleaned Up: " + fullId + "\n";
            } else { // must be a resource record
                message = "Resource Id Cleaned Up: " + fullId + "\n";
            }

            aspaceCopyUtil.addErrorMessage(message);
        }

        return ids;
    }

    /**
     * Method to concat the id parts in a string array into a full id delimited by "."
     *
     * @param ids
     * @return
     */
    private String concatIdParts(String[] ids) {
        String fullId = "";
        for(int i = 0; i < ids.length; i++) {
            if(!ids[i].isEmpty() && i == 0) {
                fullId += ids[0];
            } else if(!ids[i].isEmpty()) {
                fullId += "."  + ids[i];
            }
        }

        return fullId;
    }

    /**
     * Method to return a unique id, in cases where ASpace needs a unique id but AT doesn't
     *
     * @param endpoint
     * @param id
     * @return
     */
    private String getUniqueID(String endpoint, String id, String[] idParts) {
        id = id.trim();

        if(endpoint.equals(ASpaceClient.DIGITAL_OBJECT_ENDPOINT)) {
            // if id is empty add text
            if(id.isEmpty()) {
                id = "Digital Object ID ##"+ randomStringLong.nextString();
            }

            if(!digitalObjectIDs.contains(id)) {
                digitalObjectIDs.add(id);
            } else {
                id += " ##" + randomStringLong.nextString();
                digitalObjectIDs.add(id);
            }

            return id;
        } else if(endpoint.equals(ASpaceClient.ACCESSION_ENDPOINT)) {
            String message = null;

            if(!accessionIDs.contains(id)) {
                accessionIDs.add(id);
            } else {
                String fullId = "";

                do {
                    idParts[0] += " ##" + randomString.nextString();
                    fullId = concatIdParts(idParts);
                } while(accessionIDs.contains(fullId));

                accessionIDs.add(fullId);

                message = "Duplicate Accession Id: "  + id  + " Changed to: " + fullId + "\n";
                aspaceCopyUtil.addErrorMessage(message);
            }

            // we don't need to return the new id here, since the idParts array
            // is being used to to store the new id
            return "not used";
        } else if(endpoint.equals(ASpaceClient.RESOURCE_ENDPOINT)) {
            String message = null;

            if(!resourceIDs.contains(id)) {
                resourceIDs.add(id);
            } else {
                String fullId = "";

                do {
                    idParts[0] += " ##" + randomString.nextString();
                    fullId = concatIdParts(idParts);
                } while(resourceIDs.contains(fullId));

                resourceIDs.add(fullId);

                message = "Duplicate Resource Id: "  + id  + " Changed to: " + fullId + "\n";
                aspaceCopyUtil.addErrorMessage(message);
            }

            // we don't need to return the new id here, since the idParts array
            // is being used to to store the new id
            return "not used";
        } else if(endpoint.equals("ead")) {
            if(id.isEmpty()) {
                return "";
            }

            if(!eadIDs.contains(id)) {
                eadIDs.add(id);
            } else {
                String nid = "";

                do {
                    nid = id + " ##" + randomString.nextString();
                } while(eadIDs.contains(nid));

                eadIDs.add(nid);

                String message = "Duplicate EAD Id: "  + id  + " Changed to: " + nid + "\n";
                aspaceCopyUtil.addErrorMessage(message);

                // assign id to new id
                id = nid;
            }

            return id;
        } else {
            return id;
        }
    }

    /**
     * Method to set the current resource record identifier. Usefull for error
     * message generation
     *
     * @param identifier
     */
    public void setCurrentResourceRecordIdentifier(String identifier) {
        this.currentResourceRecordIdentifier = identifier;
    }

    /**
     * Method to set the current connection url
     *
     * @param connectionUrl
     */
    public void setConnectionUrl(String connectionUrl) {
        this.connectionUrl = connectionUrl;
    }
}