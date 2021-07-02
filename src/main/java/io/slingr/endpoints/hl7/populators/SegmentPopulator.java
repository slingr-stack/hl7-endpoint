package io.slingr.endpoints.hl7.populators;

import ca.uhn.hl7v2.HL7Exception;
import ca.uhn.hl7v2.model.AbstractGroup;
import ca.uhn.hl7v2.model.AbstractMessage;
import ca.uhn.hl7v2.model.DataTypeException;
import ca.uhn.hl7v2.model.Structure;
import ca.uhn.hl7v2.model.v281.group.ADT_A01_PROCEDURE;
import ca.uhn.hl7v2.model.v281.segment.*;
import io.slingr.endpoints.exceptions.EndpointException;
import io.slingr.endpoints.exceptions.ErrorCode;
import io.slingr.endpoints.hl7.Hl7Endpoint;
import io.slingr.endpoints.utils.Json;

import java.util.Date;
import java.util.List;
import java.util.Set;

import static io.slingr.endpoints.hl7.jsonHelper.JsonHelper.*;
import static io.slingr.endpoints.hl7.populators.DataTypePopulator.*;

public class SegmentPopulator {

    public static void populateMessage(AbstractMessage msg, Json params) throws HL7Exception {
        //We set the required EVN.2 "Recorded Date/Time" Field here as it is required, independently of the EVN segment having more info
        EVN evn = (EVN)msg.get("EVN");
        evn.getRecordedDateTime().setValue(new Date());

        //The keys from the JSON
        Set<String> keys = params.keys();

        if (keys.isEmpty()) {
            throw EndpointException.permanent(ErrorCode.ARGUMENT,"The body cannot be empty");
        }

        for (String key : keys) {
            //We match each prop to every possible segment
            switch (key){
                //Populate MSH "Message Header" segment with extra info if included
                case "messageHeader":
                    MSH msh = (MSH) msg.get("MSH");
                    Json mshValues = singleJsonPropertyParse("softwareSegment", params.string("softwareSegment"));
                    populateMshSegment(msh,mshValues);
                    break;

                //Populate SFT "Software Segment" segment, which is a repeatable segment
                case "softwareSegment":
                    List<Json> softwareSegmentList = arrayPropertyToJson("softwareSegment", params.string("softwareSegment"));
                    for (int i = 0; i < softwareSegmentList.size(); i++) {
                        SFT sft = (SFT) msg.get("SFT",i);
                        Json softwareSegment = softwareSegmentList.get(i);
                        populateSftSegment(sft, softwareSegment);
                    }

                //Populate the UAC Segment
                case "userAuthenticationCredentialSegment":
                    UAC uac = (UAC) msg.get("UAC");
                    Json uacValues = singleJsonPropertyParse("userAuthenticationCredentialSegment",params.string("userAuthenticationCredentialSegment"));
                    populateUacSegment(uac,uacValues);
                    break;

                //Populate the EVN Segment with extra info if included
                case "eventType":
                    Json evnValues = singleJsonPropertyParse("userAuthenticationCredentialSegment",params.string("userAuthenticationCredentialSegment"));
                    populateEvnSegment(evn,evnValues);
                    break;

                // Populate the PID "Patient Identification" Segment
                case "patientIdentification":
                    PID pid = (PID) msg.get("PID");
                    Json patientIdentification = jsonOrValuePropertyParse("patientIdentification", params.string("patientIdentification"));
                    populatePidSegment(pid, patientIdentification);
                    break;

                // Populate the PD1 "Patient Additional Demographic" Segment
                case "patientAdditionalDemographic":
                    PD1 pd1 = (PD1) msg.get("PD1");
                    Json patientAdditionalDemographic = jsonOrValuePropertyParse("patientAdditionalDemographic", params.string("patientAdditionalDemographic"));
                    populatePd1Segment(pd1, patientAdditionalDemographic);
                    break;

                //Populate NK1 "Next Of Kin / Associated Parties" segment, which is a repeatable segment
                case "nextOfKin":
                    List<Json> nextOfKinList = arrayPropertyToJson("nextOfKin", params.string("nextOfKin"));
                    for (int i = 0; i < nextOfKinList.size(); i++) {
                        NK1 nk1 = (NK1) msg.get("NK1",i);
                        Json nextOfKin = nextOfKinList.get(i);
                        populateNk1Segment(nk1, nextOfKin);
                    }
                    break;

                // Populate the PV1 ("Patient Visit") Segment
                case "patientVisit":
                    PV1 pv1 = (PV1) msg.get("PV1");
                    Json patientVisit = jsonOrValuePropertyParse("patientVisit", params.string("patientVisit"));
                    populatePv1Segment(pv1, patientVisit);
                    break;

                // Populate the PV2 "Patient Visit - Additional Information" Segment
                case "patientVisitAdditionalInformation":
                    PV2 pv2 = (PV2) msg.get("PV2");
                    Json patientVisitAdditionalInformation = jsonOrValuePropertyParse("patientVisitAdditionalInformation", params.string("patientVisitAdditionalInformation"));
                    populatePv2Segment(pv2, patientVisitAdditionalInformation);
                    break;

                // Populate the DB1 - Disability Segment
                case "disability":
                    List<Json> disabilityList = arrayPropertyToJson("disability", params.string("disability"));
                    for (int i = 0; i < disabilityList.size(); i++) {
                        DB1 db1 = (DB1) msg.get("DB1",i);
                        Json disability = disabilityList.get(i);
                        populateDb1Segment(db1, disability);
                    }
                    break;

                // Populate the AL1 "Patient Allergy Information" Segment, which is repeatable
                case "patientAllergyInformation":
                    List<Json> patientAllergyInformationList = arrayPropertyToJson("patientAllergyInformation", params.string("patientAllergyInformation"));
                    for (int i = 0; i < patientAllergyInformationList.size(); i++) {
                        AL1 al1 = (AL1) msg.get("AL1",i);
                        Json patientAllergyInformation = patientAllergyInformationList.get(i);
                        populateAl1Segment(al1, patientAllergyInformation);
                    }
                    break;

                // Populate the DG1 "Diagnosis" Segment, which is repeatable
                case "diagnosis":
                    List<Json> diagnosisList = arrayPropertyToJson("diagnosis", params.string("diagnosis"));
                    for (int i = 0; i < diagnosisList.size(); i++) {
                        DG1 dg1 = (DG1) msg.get("DG1",i);
                        Json diagnosis = diagnosisList.get(i);
                        populateDg1Segment(dg1, diagnosis);
                    }
                    break;

                // Populate the DG1 "Diagnosis" Segment, which is repeatable
                case "PROCEDURE":
                    //This List could be an array of PR1 or it could be and array of tuples(PR1,ROL)
                    List<Json> procedureList = multipleJsonPropertyParse("PROCEDURE",params.string("PROCEDURE"));
                    for (int i = 0; i < procedureList.size(); i++) {
                        //As PROCEDURE is a group, we have to do this in order to generalize it for every possible PROCEDURE group, which could be adt_a01,a03,etc.
                        AbstractGroup procedureGroup = (AbstractGroup) msg.get("PROCEDURE",i);
                        PR1 pr1 = (PR1) procedureGroup.get("PR1");
                        Json procedure = procedureList.get(i);
                        //Here we know if it is the tuple or if it is just the PR1
                        if(procedure.contains("procedures")){
                            Json procedures = singleJsonPropertyParse("PROCEDURE.procedures",procedure.string("procedures"));
                            //Populate the PR1 "Procedures" Segment
                            populatePr1Segment(pr1,procedures);
                            if(procedure.contains("role")){
                                List<Json> roleList = multipleJsonPropertyParse("PROCEDURE.role",procedure.string("role"));
                                for (int j = 0; j < roleList.size(); j++) {
                                    ROL _PROCEDURE_rol = (ROL) procedureGroup.get("ROL",i);
                                    Json role = roleList.get(j);
                                    //Populate the ROL "Role" Segment
                                    populateRolSegment(_PROCEDURE_rol,role);
                                }
                            }
                        } else {
                            //Populate the PR1 "Procedures" Segment
                            populatePr1Segment(pr1,procedure);
                        }
                    }
                    break;

                case "guarantor":
                    List<Json> guarantorList = arrayPropertyToJson("guarantor", params.string("guarantor"));
                    for (int i = 0; i < guarantorList.size(); i++) {
                        GT1 gt1 = (GT1) msg.get("GT1",i);
                        Json guarantor = guarantorList.get(i);
                        populateGt1Segment(gt1, guarantor);
                    }
                    break;

                // Populate the "INSURANCE" Segment
                case  "INSURANCE":
                        //This List could be an array of IN1 or it could be and array of 6-tuples(IN1,IN2,IN3,ROL,AUT,RF1)
                        List<Json> insuranceList = multipleJsonPropertyParse("INSURANCE",params.string("INSURANCE"));
                        for (int i = 0; i < insuranceList.size(); i++) {
                            //As INSURANCE is a group, we have to do this in order to generalize it for every possible PROCEDURE group, which could be adt_a01,a03,etc.
                            AbstractGroup insuranceGroup = (AbstractGroup) msg.get("INSURANCE",i);
                            IN1 in1 = (IN1) insuranceGroup.get("IN1");
                            Json insurance = insuranceList.get(i);
                            //Here we know if it is the tuple or if it is just the IN1
                            if(insurance.contains("insurance")){
                                Json innerInsurance = singleJsonPropertyParse("INSURANCE.insurance",insurance.string("insurance"));
                                //Populate the PR1 "Procedures" Segment
                                populateIn1Segment(in1,innerInsurance);
                                if(insurance.contains("insuranceAdditionalInformation")){
                                    IN2 in2 = (IN2) insuranceGroup.get("IN2");
                                    Json insuranceAdditionalInformation = singleJsonPropertyParse("INSURANCE.insuranceAdditionalInformation",insurance.string("insuranceAdditionalInformation"));
                                    populateIn2Segment(in2,insuranceAdditionalInformation);
                                }
                                if(insurance.contains("insuranceAdditionalInformationCertification")) {
                                    List<Json> insuranceAdditionalInformationCertificationList = multipleJsonPropertyParse("INSURANCE.insuranceAdditionalInformationCertification", params.string("insuranceAdditionalInformationCertification"));
                                    for (int j = 0; j < insuranceAdditionalInformationCertificationList.size(); j++) {
                                        IN3 in3 = (IN3) insuranceGroup.get("IN3");
                                        Json insuranceAdditionalInformationCertification = insuranceAdditionalInformationCertificationList.get(j);
                                        populateIn3Segment(in3,insuranceAdditionalInformationCertification);
                                    }
                                }
                                if(insurance.contains("role")){
                                    List<Json> roleList = multipleJsonPropertyParse("INSURANCE.role",insurance.string("role"));
                                    for(int j = 0; j < roleList.size(); j++){
                                        ROL _INSURANCE_rol = (ROL) insuranceGroup.get("ROL",i);
                                        Json role = roleList.get(j);
                                        //Populate the ROL "Role" Segment
                                        populateRolSegment(_INSURANCE_rol,role);
                                    }
                                }
                                if(insurance.contains("authorizationInformation")){
                                    List<Json> authorizationInformationList = multipleJsonPropertyParse("INSURANCE.authorizationInformation",insurance.string("authorizationInformation"));
                                    for(int j = 0; j < authorizationInformationList.size(); j++){
                                        AUT aut = (AUT) insuranceGroup.get("AUT",j);
                                        Json authorizationInformation = authorizationInformationList.get(j);
                                        //Populate the ROL "Role" Segment
                                        populateAutSegment(aut,authorizationInformation);
                                    }
                                }
                                if(insurance.contains("referralInformation")){
                                    List<Json> referralInformationList = multipleJsonPropertyParse("INSURANCE.referralInformation",insurance.string("referralInformation"));
                                    for(int j = 0; j < referralInformationList.size(); j++){
                                        RF1 rf1 = (RF1) insuranceGroup.get("RF1",j);
                                        Json referralInformation = referralInformationList.get(j);
                                        //Populate the ROL "Role" Segment
                                        //populateRf1Segment(adt_a01_rf1,referralInformation);
                                    }
                                }
                            } else {
                                //Populate the IN1 "Insurance" Segment
                                populateIn1Segment(in1,insurance);
                            }
                        }
                    break;

                // Populate the OBX "Observation/result" Segment, which is repeatable
                case "observationResult":
                    List<Json> observationResultList = arrayPropertyToJson("observationResult", params.string("observationResult"));
                    for (int i = 0; i < observationResultList.size(); i++) {
                        OBX obx = (OBX) msg.get("OBX",i);
                        Json observationResult = observationResultList.get(i);
                        //We pass the adt message because OBX is a segment that has a variable field
                        //and to set it we have to pass the message as argument
                        populateObxSegment(msg, obx, observationResult);
                    }
                    break;

                // Populate the PDA - Patient Death And Autopsy Segment
                case "patientDeathAndAutopsy":
                    PDA pda = (PDA) msg.get("PDA");
                    Json patientDeathAndAutopsy = singleJsonPropertyParse("patientVisitAdditionalInformation", params.string("patientVisitAdditionalInformation"));
                    populatePdaSegment(pda, patientDeathAndAutopsy);
                    break;

                default:
                    throw EndpointException.permanent(ErrorCode.ARGUMENT,"The property ['"+key+"'] does not correspond with any possible HL7 segment");
            }
        }
    }

    public static void  populateMshSegment(MSH msh, Json mshValues) throws DataTypeException {
        String parentProp = "messageHeader.";

        //Populate MSH.3 "Sending Application" component
        msh.getSendingApplication().getNamespaceID().setValue(
                Hl7Endpoint.appName
        );
        msh.getSendingFacility().getNamespaceID().setValue(
                mshValues.contains("sendingFacility") ? mshValues.string("sendingFacility") : ""
        );
        msh.getReceivingApplication().getNamespaceID().setValue(
                mshValues.contains("receivingApplication") ? mshValues.string("receivingApplication") : ""
        );
        msh.getReceivingFacility().getNamespaceID().setValue(
                mshValues.contains("receivingFacility") ? mshValues.string("receivingFacility") : ""
        );
        msh.getSecurity().setValue(
                mshValues.contains("security") ? mshValues.string("security") : ""
        );
        //THERE ARE SOME OTHER COMPONENTS HERE, WE SHOULD EVALUATE IF ADDING THEM IS NECESSARY OR NOT
    }

    public static void populateSftSegment(SFT sft, Json sftValues) throws DataTypeException {
        String parentProp = "softwareSegment.";

        if(sftValues.contains("softwareVendorOrganization")){
            Json softwareVendorOrganization = jsonOrValuePropertyParse("softwareVendorOrganization",sftValues.string("softwareVendorOrganization"));
            populateXonField(sft.getSoftwareVendorOrganization(),softwareVendorOrganization);
        }
        sft.getSoftwareCertifiedVersionOrReleaseNumber().setValue(
                sftValues.contains("softwareCertifiedVersionOrReleaseNumber") ? sftValues.string("softwareCertifiedVersionOrReleaseNumber") : ""
        );
        sft.getSoftwareProductName().setValue(
                sftValues.contains("softwareProductName") ? sftValues.string("softwareProductName") : ""
        );
        sft.getSoftwareBinaryID().setValue(
                sftValues.contains("softwareBinaryID") ? sftValues.string("softwareBinaryID") : ""
        );
        sft.getSoftwareProductInformation().setValue(
                sftValues.contains("softwareProductInformation") ? sftValues.string("softwareProductInformation") : ""
        );
        sft.getSoftwareInstallDate().setValue(
                sftValues.contains("softwareInstallDate") ? sftValues.string("softwareInstallDate") : ""
        );
    }

    public static void  populateUacSegment(UAC uac, Json uacValues) throws DataTypeException {
        String parentProp = "userAuthenticationCredentialSegment.";

        //Populate UAC.1 - User Authentication Credential Type Code
        if(uacValues.contains("userAuthenticationCredentialTypeCode")){
            Json userAuthenticationCredentialTypeCode = jsonOrValuePropertyParse("userAuthenticationCredentialTypeCode",uacValues.string("userAuthenticationCredentialTypeCode"));
            populateCweField(uac.getUserAuthenticationCredentialTypeCode(),userAuthenticationCredentialTypeCode);
        }
        //Populate UAC.2 - User Authentication Credential
        if(uacValues.contains("userAuthenticationCredential")){
            Json userAuthenticationCredential = singleJsonPropertyParse("userAuthenticationCredential",uacValues.string("userAuthenticationCredential"));
            populateEdField(uac.getUserAuthenticationCredential(),userAuthenticationCredential);
        }
    }

    public static void populateEvnSegment(EVN evn, Json evnValues) throws DataTypeException {
        String parentProp = "eventType.";
        //EVN.1 - Event Type Code withdrawn
        //EVN.2 - Recorded Date/Time already populated
        //Populate EVN.3 - Date/Time Planned Event
        evn.getDateTimePlannedEvent().setValue(
                evnValues.contains("dateTimePlannedEvent") ? evnValues.string("dateTimePlannedEvent") : ""
        );
        //Populate EVN.4 - Event Reason Code
        if(evnValues.contains("eventReasonCode")){
            Json eventReasonCode = jsonOrValuePropertyParse(parentProp+"eventReasonCode",evnValues.string("eventReasonCode"));
            populateCweField(evn.getEventReasonCode(),eventReasonCode);
        }
        //Populate EVN.5 - Operator Id
        if (evnValues.contains("operatorId")) {
            List<Json> operatorIdList = multipleJsonPropertyParse(parentProp+"operatorId", evnValues.string("operatorId"));
            for (int i = 0; i < operatorIdList.size(); i++) {
                Json operatorId = operatorIdList.get(i);
                populateXcnField(evn.getOperatorID(i),operatorId);
            }
        }
        //Populate EVN.6 - Event Occurred
        evn.getEventOccurred().setValue(
                evnValues.contains("eventOccurred") ? evnValues.string("eventOccurred") : ""
        );
        //Populate EVN.7 - Event Facility
        if(evnValues.contains("eventFacility")){
            Json eventFacility = jsonOrValuePropertyParse(parentProp+"eventFacility",evnValues.string("eventFacility"));
            populateHdField(evn.getEventFacility(),eventFacility);
        }
    }

    public static void populatePidSegment(PID pid, Json pidValues) throws DataTypeException {
        String parentProp = "patientIdentification.";

        //Populate PID.1 "Set Id - Pid" component
        pid.getSetIDPID().setValue(
                pidValues.contains("setIdPid") ? pidValues.string("setIdPid") : ""
        );
        //PID.2 "Patient Id" Withdrawn
        //Populate PID.3 "Patient Identifier List" component, which is repeatable
        if (pidValues.contains("patientIdentifierList")) {
            List<Json> patientIdentifierList = multipleJsonPropertyParse(parentProp+"patientIdentifierList",pidValues.string("patientIdentifierList"));
            for (int i = 0; i<patientIdentifierList.size();i++) {
                Json currentPatientIdentifier = patientIdentifierList.get(i);
                populateCxField(pid.getPatientIdentifierList(i),currentPatientIdentifier);
            }
        }
        //PID.4 "Alternate Patient Id - Pid" Withdrawn
        //Populate PID.5 "Patient Name" component, which is repeatable
        if (pidValues.contains("patientName")) {
            List<Json> patientNameList = arrayPropertyToJson(parentProp+"patientName",pidValues.string("patientName"));
            for (int i = 0; i<patientNameList.size();i++) {
                Json patientName = patientNameList.get(i);
                populateXpnField(pid.getPatientName(i),patientName);
            }
        }
        //Populate PID.6 "Mother's Maiden Name" component, which is repeatable
        if (pidValues.contains("mothersMaidenName")) {
            List<Json> mothersMaidenNameList = arrayPropertyToJson(parentProp+"mothersMaidenName", pidValues.string("mothersMaidenName"));
            for (int i = 0; i < mothersMaidenNameList.size(); i++) {
                Json currentMothersMaidenName = mothersMaidenNameList.get(i);
                populateXpnField(pid.getMotherSMaidenName(i),currentMothersMaidenName);
            }
        }
        //Populate PID.7 "Date/Time Of Birth" component
        pid.getDateTimeOfBirth().setValue(
                pidValues.contains("patientBirthDate") ? pidValues.string("patientBirthDate") : ""
        );
        //Populate PID.8 "Administrative Sex" component
        if (pidValues.contains("administrativeSex")) {
            Json administrativeSex = jsonOrValuePropertyParse(parentProp+"administrativeSex", pidValues.string("administrativeSex"));
            populateCweField(pid.getAdministrativeSex(),administrativeSex);
        }
        //PID.9 "Patient Alias" Withdrawn
        //Populate PID.10 "Race" component, which is repeatable
        if (pidValues.contains("race")) {
            List<Json> raceList = arrayPropertyToJson(parentProp+"race", pidValues.string("race"));
            for (int i = 0; i < raceList.size(); i++) {
                Json currentRace = raceList.get(i);
                populateCweField(pid.getRace(i),currentRace);
            }
        }
        //Populate PID.11 "Patient Address" component, which is repeatable
        if (pidValues.contains("patientAddress")) {
            List<Json> patientAddressList = arrayPropertyToJson(parentProp+"patientAddress", pidValues.string("patientAddress"));
            for (int i = 0; i < patientAddressList.size(); i++) {
                Json currentPatientAddress = patientAddressList.get(i);
                populateXadField(pid.getPatientAddress(i),currentPatientAddress);
            }
        }
        //PID.12 "County Code" Withdrawn
        //Populate PID.13 "Phone Number - Home" component, which is repeatable
        if (pidValues.contains("phoneNumberHome")) {
            List<Json> patientPhoneNumberHomeList = multipleJsonPropertyParse(parentProp+"phoneNumberHome", pidValues.string("phoneNumberHome"));
            for (int i = 0; i < patientPhoneNumberHomeList.size(); i++) {
                Json currentPatientPhoneNumberHome = patientPhoneNumberHomeList.get(i);
                populateXtnField(pid.getPhoneNumberHome(i),currentPatientPhoneNumberHome);
            }
        }
        //Populate PID.14 "Phone Number - Business" component, which is repeatable
        if (pidValues.contains("phoneNumberBusiness")) {
            List<Json> patientPhoneNumberBusinessList = multipleJsonPropertyParse(parentProp+"phoneNumberBusiness", pidValues.string("phoneNumberBusiness"));
            for (int i = 0; i < patientPhoneNumberBusinessList.size(); i++) {
                Json currentPatientPhoneNumberHome = patientPhoneNumberBusinessList.get(i);
                populateXtnField(pid.getPhoneNumberBusiness(i),currentPatientPhoneNumberHome);
            }
        }
        //Populate PID.15 "Primary Language" component
        if (pidValues.contains("primaryLanguage")) {
            Json primaryLanguage = jsonOrValuePropertyParse(parentProp+"primaryLanguage",pidValues.string("primaryLanguage"));
            populateCweField(pid.getPrimaryLanguage(),primaryLanguage);
        }

        //Populate PID.16 "Marital Status" component
        if (pidValues.contains("maritalStatus")) {
            Json maritalStatus = jsonOrValuePropertyParse("maritalStatus",pidValues.string("maritalStatus"));
            populateCweField(pid.getMaritalStatus(),maritalStatus);
        }
        //Populate PID.17 "Religion" component
        if (pidValues.contains("religion")) {
            Json religion = jsonOrValuePropertyParse("religion",pidValues.string("religion"));
            populateCweField(pid.getReligion(),religion);
        }
        //Populate PID.18 "Patient Account Number" component
        if (pidValues.contains("patientAccountNumber")) {
            Json patientAccountNumber = singleJsonPropertyParse("patientAccountNumber",pidValues.string("patientAccountNumber"));
            populateCxField(pid.getPatientAccountNumber(),patientAccountNumber);
        }
        //PID.19 "Ssn Number - Patient" Withdrawn
        //PID.20 "Driver's License Number - Patient" Withdrawn
        //Populate PID.21 "Mother's Identifier" component, which is repeatable
        if (pidValues.contains("mothersIdentifier")) {
            List<Json> mothersIdentifierList = multipleJsonPropertyParse("mothersIdentifier", pidValues.string("mothersIdentifier"));
            for (int i = 0; i < mothersIdentifierList.size(); i++) {
                Json currentMothersIdentifier = mothersIdentifierList.get(i);
                populateCxField(pid.getMotherSIdentifier(i),currentMothersIdentifier);
            }
        }
        //Populate PID.22 "Ethnic Group" component, which is repeatable
        if (pidValues.contains("ethnicGroup")) {
            List<Json> ethnicGroupList = arrayPropertyToJson("ethnicGroup", pidValues.string("ethnicGroup"));
            for (int i = 0; i < ethnicGroupList.size(); i++) {
                Json currentEthnicGroup = ethnicGroupList.get(i);
                populateCweField(pid.getEthnicGroup(i),currentEthnicGroup);
            }
        }

        //Populate PID.23 "Birth Place" component
        pid.getBirthPlace().setValue(
                pidValues.contains("birthPlace") ? pidValues.string("birthPlace") : ""
        );
        //Populate PID.24 "Multiple Birth Indicator" component
        pid.getMultipleBirthIndicator().setValue(
                pidValues.contains("multipleBirthIndicator") ? pidValues.string("multipleBirthIndicator") : ""
        );
        //Populate PID.25 "Birth Order" component
        pid.getBirthOrder().setValue(
                pidValues.contains("birthOrder") ? pidValues.string("birthOrder") : ""
        );
        //Populate PID.26 "Citizenship" component, which is repeatable
        if (pidValues.contains("citizenship")) {
            List<Json> patientCitizenshipList = arrayPropertyToJson("citizenship",pidValues.string("citizenship"));
            for (int i = 0; i < patientCitizenshipList.size(); i++) {
                Json currentCitizenship = patientCitizenshipList.get(i);
                populateCweField(pid.getCitizenship(i),currentCitizenship);
            }
        }
        //Populate PID.27 "Veterans Military Status" component
        if (pidValues.contains("veteransMilitaryStatus")) {
            Json veteransMilitaryStatus = jsonOrValuePropertyParse("veteransMilitaryStatus",pidValues.string("veteransMilitaryStatus"));
            populateCweField(pid.getVeteransMilitaryStatus(),veteransMilitaryStatus);
        }
        //PID.28 "Nationality" Withdrawn
        //Populate PID.29 "Patient Death Date And Time" component
        pid.getPatientDeathDateAndTime().setValue(
                pidValues.contains("patientDeathDateAndTime") ? pidValues.string("patientDeathDateAndTime") : ""
        );
        //Populate PID.30 "Patient Death Indicator" component
        pid.getPatientDeathIndicator().setValue(
                pidValues.contains("patientDeathIndicator") ? pidValues.string("patientDeathIndicator") : ""
        );
        //Populate PID.31 "Identity Unknown Indicator" component
        pid.getIdentityUnknownIndicator().setValue(
                pidValues.contains("identityUnknownIndicator") ? pidValues.string("identityUnknownIndicator") : ""
        );
        //Populate PID.32 "Identity Reliability Code" component, which is repeatable
        if (pidValues.contains("identityReliabilityCode")) {
            List<Json> identityReliabilityCodeList = arrayPropertyToJson("identityReliabilityCode",pidValues.string("identityReliabilityCode"));
            for (int i = 0; i < identityReliabilityCodeList.size(); i++) {
                Json currentIdentityReliabilityCode = identityReliabilityCodeList.get(i);
                populateCweField(pid.getIdentityReliabilityCode(i),currentIdentityReliabilityCode);
            }
        }
        //Populate PID.33 "Last Update Date/Time" component
        pid.getLastUpdateDateTime().setValue(
                pidValues.contains("lastUpdateDateTime") ? pidValues.string("lastUpdateDateTime") : ""
        );
        //Populate PID.34 "Last Update Facility" component
        if (pidValues.contains("lastUpdateFacility")) {
            Json lastUpdateFacility = jsonOrValuePropertyParse(parentProp+"lastUpdateFacility",pidValues.string("lastUpdateFacility"));
            populateHdField(pid.getLastUpdateFacility(),lastUpdateFacility);
        }
        //Populate PID.35 "Taxonomic Classification Code" component
        if (pidValues.contains("taxonomicClassificationCode")) {
            Json taxonomicClassificationCode = jsonOrValuePropertyParse("taxonomicClassificationCode",pidValues.string("taxonomicClassificationCode"));
            populateCweField(pid.getTaxonomicClassificationCode(),taxonomicClassificationCode);
        }
        //Populate PID.36 "Breed Code" component
        if (pidValues.contains("breedCode")) {
            Json breedCode = jsonOrValuePropertyParse("breedCode",pidValues.string("breedCode"));
            populateCweField(pid.getBreedCode(),breedCode);
        }
        //Populate PID.37 "Strain" component
        pid.getStrain().setValue(
                pidValues.contains("strain") ? pidValues.string("strain") : ""
        );
        //Populate PID.38 "Production Class Code" component
        if (pidValues.contains("productionClassCode")) {
            Json productionClassCode = jsonOrValuePropertyParse("productionClassCode",pidValues.string("productionClassCode"));
            populateCweField(pid.getProductionClassCode(),productionClassCode);
        }
        //Populate PID.39 "Tribal Citizenship" component, which is repeatable
        if (pidValues.contains("tribalCitizenship")) {
            List<Json> tribalCitizenshipList = arrayPropertyToJson("tribalCitizenship",pidValues.string("tribalCitizenship"));
            for (int i = 0; i < tribalCitizenshipList.size(); i++) {
                Json currentTribalCitizenship = tribalCitizenshipList.get(i);
                populateCweField(pid.getTribalCitizenship(i),currentTribalCitizenship);
            }
        }
        //Populate PID.40 "Patient Telecommunication Information" component, which is repeatable
        if (pidValues.contains("patientTelecommunicationInformation")) {
            List<Json> patientTelecommunicationInformationList = multipleJsonPropertyParse("patientTelecommunicationInformation",pidValues.string("patientTelecommunicationInformation"));
            for (int i = 0; i < patientTelecommunicationInformationList.size(); i++) {
                Json currentPatientTelecommunicationInformation = patientTelecommunicationInformationList.get(i);
                populateXtnField(pid.getPatientTelecommunicationInformation(i),currentPatientTelecommunicationInformation);
            }
        }
    }

    public static void populatePd1Segment(PD1 pd1, Json pd1Values) throws DataTypeException {
        //Populate PD1.1 "Living Dependency" component, which is repeatable
        if (pd1Values.contains("livingDependency")) {
            List<Json> livingDependencyList = arrayPropertyToJson("patientAdditionalDemographic.livingDependecy",pd1Values.string("livingDependency"));
            for (int i = 0; i<livingDependencyList.size();i++) {
                Json currentLivingDependency = livingDependencyList.get(i);
                populateCweField(pd1.getLivingDependency(i),currentLivingDependency);
            }
        }
        //Populate PD1.2 "Living Arrangement" component
        if (pd1Values.contains("livingArrangement")) {
            List<Json> livingArrangementList = arrayPropertyToJson("patientAdditionalDemographic.livingArrangement",pd1Values.string("livingArrangement"));
            Json livingArrangement = livingArrangementList.get(0);
            populateCweField(pd1.getLivingArrangement(),livingArrangement);
        }
        //Populate PD1.3 "Patient Primary Facility" component, which is repeatable
        if (pd1Values.contains("patientPrimaryFacility")) {
            List<Json> patientPrimaryFacilityList = arrayPropertyToJson("patientAdditionalDemographic.patientPrimaryFacility",pd1Values.string("patientPrimaryFacility"));
            for (int i = 0; i<patientPrimaryFacilityList.size();i++) {
                Json currentPatientPrimaryFacility = patientPrimaryFacilityList.get(i);
                populateXonField(pd1.getPatientPrimaryFacility(i),currentPatientPrimaryFacility);
            }
        }
        //Component PD1.4 Withdrawn
        //Populate PD1.5 "Student Indicator" component
        if(pd1Values.contains("studentIndicator")){
            Json studentIndicator = jsonOrValuePropertyParse("studentIndicator",pd1Values.string("studentIndicator"));
            populateCweField(pd1.getStudentIndicator(),studentIndicator);
        }
        //Populate PD1.6 "Handicap" component
        if(pd1Values.contains("handicap")){
            Json handicap = jsonOrValuePropertyParse("handicap",pd1Values.string("handicap"));
            populateCweField(pd1.getHandicap(),handicap);
        }
        //Populate PD1.7 "Living Will Code" component
        if(pd1Values.contains("livingWillCode")){
            Json livingWillCode = jsonOrValuePropertyParse("livingWillCode",pd1Values.string("livingWillCode"));
            populateCweField(pd1.getLivingWillCode(),livingWillCode);
        }
        //Populate PD1.8 "Organ Donor Code" component
        if(pd1Values.contains("organDonorCode")){
            Json organDonorCode = jsonOrValuePropertyParse("organDonorCode",pd1Values.string("organDonorCode"));
            populateCweField(pd1.getOrganDonorCode(),organDonorCode);
        }
        //Populate PD1.9 "Separate Bill" component
        pd1.getSeparateBill().setValue(
                pd1Values.contains("separateBill") ? pd1Values.string("separateBill") : ""
        );
        //Populate PD1.10 "Duplicate Patient" component, which is repeatable
        if(pd1Values.contains("duplicatePatient")){
            List<Json> duplicatePatientList = multipleJsonPropertyParse("duplicatePatient",pd1Values.string("duplicatePatient"));
            for (int i = 0; i<duplicatePatientList.size();i++) {
                Json currentDuplicatePatient = duplicatePatientList.get(i);
                populateCxField(pd1.getDuplicatePatient(i),currentDuplicatePatient);
            }
        }
        //Populate PD1.11 "Publicity Code" component
        if(pd1Values.contains("publicityCode")){
            Json publicityCode = jsonOrValuePropertyParse("publicityCode",pd1Values.string("publicityCode"));
            populateCweField(pd1.getPublicityCode(),publicityCode);
        }
        //Populate PD1.12 "Protection Indicator" component
        pd1.getProtectionIndicator().setValue(
                pd1Values.contains("protectionIndicator") ? pd1Values.string("protectionIndicator") : ""
        );
        //Populate PD1.13 "Protection Indicator Effective Date" component
        pd1.getProtectionIndicatorEffectiveDate().setValue(
                pd1Values.contains("protectionIndicatorEffectiveDate") ? pd1Values.string("protectionIndicatorEffectiveDate") : ""
        );
        //Populate PD1.14 "Place Of Worship" component, which is repeatable
        if(pd1Values.contains("placeOfWorship")){
            List<Json> placeOfWorshipList = arrayPropertyToJson("placeOfWorship",pd1Values.string("placeOfWorship"));
            for (int i = 0; i<placeOfWorshipList.size();i++) {
                Json currentPlaceOfWorship= placeOfWorshipList.get(i);
                populateXonField(pd1.getPlaceOfWorship(i),currentPlaceOfWorship);
            }
        }
        //Populate PD1.15 "Advance Directive Code" component, which is repeatable
        if(pd1Values.contains("advanceDirectiveCode")){
            List<Json> advanceDirectiveCodeList = arrayPropertyToJson("advanceDirectiveCode",pd1Values.string("advanceDirectiveCode"));
            for (int i = 0; i<advanceDirectiveCodeList.size();i++) {
                Json currentAdvanceDirectiveCode = advanceDirectiveCodeList.get(i);
                populateCweField(pd1.getAdvanceDirectiveCode(i),currentAdvanceDirectiveCode);
            }
        }
        //Populate PD1.16 "Immunization Registry Status" component
        if(pd1Values.contains("immunizationRegistryStatus")){
            Json immunizationRegistryStatus = jsonOrValuePropertyParse("immunizationRegistryStatus",pd1Values.string("immunizationRegistryStatus"));
            populateCweField(pd1.getImmunizationRegistryStatus(),immunizationRegistryStatus);
        }
        //Populate PD1.17 "Immunization Registry Status Effective Date" component
        pd1.getImmunizationRegistryStatusEffectiveDate().setValue(
                pd1Values.contains("immunizationRegistryStatusEffectiveDate") ? pd1Values.string("immunizationRegistryStatusEffectiveDate") : ""
        );
        //Populate PD1.18 "Publicity Code Effective Date" component
        pd1.getPublicityCodeEffectiveDate().setValue(
                pd1Values.contains("publicityCodeEffectiveDate") ? pd1Values.string("publicityCodeEffectiveDate") : ""
        );
        //Populate PD1.19 "Military Branch" component
        if(pd1Values.contains("militaryBranch")){
            Json militaryBranch = jsonOrValuePropertyParse("militaryBranch",pd1Values.string("militaryBranch"));
            populateCweField(pd1.getMilitaryBranch(),militaryBranch);
        }
        //Populate PD1.20 "Military Rank/Grade" component
        if(pd1Values.contains("militaryRankGrade")){
            Json militaryRankGrade = jsonOrValuePropertyParse("militaryRankGrade",pd1Values.string("militaryRankGrade"));
            populateCweField(pd1.getMilitaryRankGrade(),militaryRankGrade);
        }
        //Populate PD1.21 "Military Status" component
        if(pd1Values.contains("militaryStatus")){
            Json militaryStatus = jsonOrValuePropertyParse("militaryStatus",pd1Values.string("militaryStatus"));
            populateCweField(pd1.getMilitaryStatus(),militaryStatus);
        }
        //Populate PD1.22 "Advance Directive Last Verified Date" component
        pd1.getAdvanceDirectiveLastVerifiedDate().setValue(
                pd1Values.contains("advanceDirectiveLastVerifiedDate") ? pd1Values.string("advanceDirectiveLastVerifiedDate") : ""
        );
    }

    public static void populateRolSegment(ROL rol, Json rolValues) throws DataTypeException {
        String parentProp = "role.";
        //Populate ROL.1 - Role Instance Id
        if(rolValues.contains("roleInstanceId")){
            Json roleInstanceId = jsonOrValuePropertyParse(parentProp+"roleInstanceId",rolValues.string("roleInstanceId"));
            populateEiField(rol.getRoleInstanceID(),roleInstanceId);
        }
        //Populate ROL.2 "Action Code" component
        rol.getActionCode().setValue(
                rolValues.contains("actionCode") ? rolValues.string("actionCode") : ""
        );
        //Populate ROL.3 - Role-rol
        if(rolValues.contains("roleRol")){
            Json roleRol = jsonOrValuePropertyParse(parentProp+"roleRol",rolValues.string("roleRol"));
            populateCweField(rol.getRoleROL(),roleRol);
        }
        //Populate ROL.4 - Role Person
        if(rolValues.contains("rolePerson")){
            List<Json> rolePersonList = arrayPropertyToJson(parentProp+"rolePerson",rolValues.string("rolePerson"));
            for (int i = 0; i<rolePersonList.size();i++) {
                Json rolePerson = rolePersonList.get(i);
                populateXcnField(rol.getRolePerson(i),rolePerson);
            }
        }
        //Populate ROL.5 "Role Begin Date/Time" component
        rol.getRoleBeginDateTime().setValue(
                rolValues.contains("roleBeginDateTime") ? rolValues.string("roleBeginDateTime") : ""
        );
        //Populate ROL.6 "Role End Date/Time" component
        rol.getRoleEndDateTime().setValue(
                rolValues.contains("roleEndDateTime") ? rolValues.string("roleEndDateTime") : ""
        );
        //Populate ROL.7 - Role Duration
        if(rolValues.contains("roleDuration")){
            Json roleDuration = jsonOrValuePropertyParse(parentProp+"roleDuration",rolValues.string("roleDuration"));
            populateCweField(rol.getRoleDuration(),roleDuration);
        }
        //Populate ROL.8 - Role Action Reason
        if(rolValues.contains("roleActionReason")){
            Json roleActionReason = jsonOrValuePropertyParse(parentProp+"roleActionReason",rolValues.string("roleActionReason"));
            populateCweField(rol.getRoleActionReason(),roleActionReason);
        }
        //Populate ROL.9 - Provider Type
        if(rolValues.contains("providerType")){
            List<Json> providerTypeList = arrayPropertyToJson(parentProp+"providerType",rolValues.string("providerType"));
            for (int i = 0; i<providerTypeList.size();i++) {
                Json providerType = providerTypeList.get(i);
                populateXcnField(rol.getRolePerson(i),providerType);
            }
        }
        //Populate ROL.10 - Organization Unit Type
        if(rolValues.contains("organizationUnitType")){
            Json organizationUnitType = jsonOrValuePropertyParse(parentProp+"organizationUnitType",rolValues.string("organizationUnitType"));
            populateCweField(rol.getOrganizationUnitType(),organizationUnitType);
        }
        //Populate ROL.11 - Office/Home Address/Birthplace
        if(rolValues.contains("officeHomeAddressBirthplace")){
            List<Json> officeHomeAddressBirthplaceList = arrayPropertyToJson(parentProp+"officeHomeAddressBirthplace",rolValues.string("officeHomeAddressBirthplace"));
            for (int i = 0; i<officeHomeAddressBirthplaceList.size();i++) {
                Json officeHomeAddressBirthplace = officeHomeAddressBirthplaceList.get(i);
                populateXadField(rol.getOfficeHomeAddressBirthplace(i),officeHomeAddressBirthplace);
            }
        }
        //Populate ROL.12 - Phone
        if(rolValues.contains("phone")){
            List<Json> phoneList = arrayPropertyToJson(parentProp+"phone",rolValues.string("phone"));
            for (int i = 0; i<phoneList.size();i++) {
                Json phone = phoneList.get(i);
                populateXtnField(rol.getPhone(i),phone);
            }
        }
        //Populate ROL.13 - Person's Location
        if(rolValues.contains("personsLocation")){
            Json personsLocation = jsonOrValuePropertyParse(parentProp+"organizationUnitType",rolValues.string("organizationUnitType"));
            populateCweField(rol.getOrganizationUnitType(),personsLocation);
        }
        //Populate ROL.14 - Organization
        if(rolValues.contains("organization")){
            Json organization = jsonOrValuePropertyParse(parentProp+"organization",rolValues.string("organization"));
            populateXonField(rol.getOrganization(),organization);
        }
    }

    public static void populateNk1Segment(NK1 nk1, Json nk1Values) throws DataTypeException {
        //Populate NK1.1 "Set Id - Nk1" component
        nk1.getSetIDNK1().setValue(
                nk1Values.contains("setId") ? nk1Values.string("setId") : ""
        );
        //Populate NK1.2 "Name" component, which is repeatable
        if (nk1Values.contains("name")) {
            List<Json> nextOfKinNameList = arrayPropertyToJson("nextOfKin.name" ,nk1Values.string("name"));
            for (int i = 0; i < nextOfKinNameList.size(); i++){
                Json currentNextOfKinName = nextOfKinNameList.get(i);
                populateXpnField(nk1.getNK1Name(i),currentNextOfKinName);
            }
        }
        //Populate NK1.3 "Relationship" component
        if (nk1Values.contains("relationship")){
            Json relationship = jsonOrValuePropertyParse("nextOfKin.relationship",nk1Values.string("relationship"));
            populateCweField(nk1.getRelationship(),relationship);
        }
        //Populate NK1.4 "Address" component, which is repeatable
        if (nk1Values.contains("address")){
            List<Json> addressList = arrayPropertyToJson("nextOfKin.address",nk1Values.string("address"));
            for (int i=0; i < addressList.size(); i++){
                Json currentAddress = addressList.get(i);
                populateXadField(nk1.getAddress(i),currentAddress);
            }
        }
        //Populate NK1.5 "Phone Number" component, which is repeatable
        if (nk1Values.contains("phoneNumber")){
            List<Json> phoneNumberList = multipleJsonPropertyParse("nextOfKin.phoneNumber",nk1Values.string("phoneNumber"));
            for (int i=0; i < phoneNumberList.size(); i++){
                Json currentPhoneNumber = phoneNumberList.get(i);
                populateXtnField(nk1.getPhoneNumber(i),currentPhoneNumber);
            }
        }
        //Populate NK1.6 "Business Phone Number" component, which is repeatable
        if (nk1Values.contains("phoneNumber")){
            List<Json> businessPhoneNumberList = multipleJsonPropertyParse("nextOfKin.businessPhoneNumberList",nk1Values.string("businessPhoneNumberList"));
            for (int i=0; i < businessPhoneNumberList.size(); i++){
                Json currentBusinessPhoneNumber = businessPhoneNumberList.get(i);
                populateXtnField(nk1.getBusinessPhoneNumber(i),currentBusinessPhoneNumber);
            }
        }
        //Populate NK1.7 "Contact Role" component
        if (nk1Values.contains("contactRole")){
            Json contactRole = jsonOrValuePropertyParse("nextOfKin.contactRole",nk1Values.string("contactRole"));
            populateCweField(nk1.getContactRole(),contactRole);
        }
        //Populate NK1.8 "Start Date" component
        nk1.getStartDate().setValue(
                nk1Values.contains("startDate") ? nk1Values.string("startDate") : ""
        );
        //Populate NK1.9 "End Date" component
        nk1.getEndDate().setValue(
                nk1Values.contains("endDate") ? nk1Values.string("endDate") : ""
        );
        //Populate NK1.10 "Next Of Kin / Associated Parties Job Title" component
        nk1.getNextOfKinAssociatedPartiesJobTitle().setValue(
                nk1Values.contains("nextOfKinAssociatedPartiesJobTitle") ? nk1Values.string("nextOfKinAssociatedPartiesJobTitle") : ""
        );
        //Populate NK1.11 "Next Of Kin / Associated Parties Job Code/Class" component
        if (nk1Values.contains("nextOfKinAssociatedPartiesJobCodeClass")){
            Json nextOfKinAssociatedPartiesJobCodeClass = jsonOrValuePropertyParse("nextOfKin.nextOfKinAssociatedPartiesJobCodeClass",nk1Values.string("nextOfKinAssociatedPartiesJobCodeClass"));
            populateJccField(nk1.getNextOfKinAssociatedPartiesJobCodeClass(),nextOfKinAssociatedPartiesJobCodeClass);
        }
        //Populate NK1.12 " Next Of Kin / Associated Parties Employee Number" component
        if (nk1Values.contains("nextOfKinAssociatedPartiesEmployeeNumber")){
            Json nextOfKinAssociatedPartiesJobCodeClass = singleJsonPropertyParse("nextOfKin.nextOfKinAssociatedPartiesJobCodeClass",nk1Values.string("nextOfKinAssociatedPartiesJobCodeClass"));
            populateCxField(nk1.getNextOfKinAssociatedPartiesEmployeeNumber(),nextOfKinAssociatedPartiesJobCodeClass);
        }
        //Populate NK1.13 "Organization Name - Nk1" component, which is repeatable
        if (nk1Values.contains("organizationNameNK1")){
            List<Json> organizationNameNK1List = arrayPropertyToJson("nextOfKin.organizationNameNK1",nk1Values.string("organizationNameNK1"));
            for (int i=0; i < organizationNameNK1List.size(); i++){
                Json currentOrganizationNameNK1 = organizationNameNK1List.get(i);
                populateXonField(nk1.getOrganizationNameNK1(i),currentOrganizationNameNK1);
            }
        }
        //Populate NK1.14 "Marital Status" component
        if (nk1Values.contains("maritalStatus")){
            Json maritalStatus = jsonOrValuePropertyParse("nextOfKin.maritalStatus",nk1Values.string("maritalStatus"));
            populateCweField(nk1.getMaritalStatus(),maritalStatus);
        }
        //Populate NK1.15 "Administrative Sex" component
        if (nk1Values.contains("administrativeSex")){
            Json administrativeSex = jsonOrValuePropertyParse("nextOfKin.administrativeSex",nk1Values.string("administrativeSex"));
            populateCweField(nk1.getAdministrativeSex(),administrativeSex);
        }
        //Populate NK1.16 "Date/Time Of Birth" component
        nk1.getDateTimeOfBirth().setValue(
                nk1Values.contains("dateTimeOfBirth") ? nk1Values.string("dateTimeOfBirth") : ""
        );
        //Populate NK1.17 "Living Dependency" component, which is repeatable
        if (nk1Values.contains("livingDependency")){
            List<Json> livingDependencyList = arrayPropertyToJson("nextOfKin.livingDependency",nk1Values.string("livingDependency"));
            for (int i=0; i < livingDependencyList.size(); i++){
                Json currentLivingDependency = livingDependencyList.get(i);
                populateCweField(nk1.getLivingDependency(i),currentLivingDependency);
            }
        }
        //Populate NK1.18 "Ambulatory Status" component, which is repeatable
        if (nk1Values.contains("ambulatoryStatus")){
            List<Json> ambulatoryStatusList = arrayPropertyToJson("nextOfKin.ambulatoryStatus",nk1Values.string("ambulatoryStatus"));
            for (int i=0; i < ambulatoryStatusList.size(); i++){
                Json currentAmbulatoryStatus = ambulatoryStatusList.get(i);
                populateCweField(nk1.getAmbulatoryStatus(i),currentAmbulatoryStatus);
            }
        }
        //Populate NK1.19 "Citizenship" component, which is repeatable
        if (nk1Values.contains("citizenship")){
            List<Json> citizenshipList = arrayPropertyToJson("nextOfKin.citizenship",nk1Values.string("citizenship"));
            for (int i=0; i < citizenshipList.size(); i++){
                Json currentCitizenship = citizenshipList.get(i);
                populateCweField(nk1.getCitizenship(i),currentCitizenship);
            }
        }
        //Populate NK1.20 "Primary Language" component
        if (nk1Values.contains("primaryLanguage")){
            Json primaryLanguage = jsonOrValuePropertyParse("nextOfKin.primaryLanguage",nk1Values.string("primaryLanguage"));
            populateCweField(nk1.getPrimaryLanguage(),primaryLanguage);
        }
        //Populate NK1.21 "Living Arrangement" component
        if (nk1Values.contains("livingArrangement")){
            Json livingArrangement = jsonOrValuePropertyParse("nextOfKin.livingArrangement",nk1Values.string("livingArrangement"));
            populateCweField(nk1.getLivingArrangement(),livingArrangement);
        }
        //Populate NK1.22 "Publicity Code" component
        if (nk1Values.contains("publicityCode")){
            Json publicityCode = jsonOrValuePropertyParse("nextOfKin.publicityCode",nk1Values.string("publicityCode"));
            populateCweField(nk1.getPublicityCode(),publicityCode);
        }
        //Populate NK1.23 "Protection Indicator" component
        nk1.getProtectionIndicator().setValue(
                nk1Values.contains("protectionIndicator") ? nk1Values.string("protectionIndicator") : ""
        );
        //Populate NK1.24 "Student Indicator" component
        if (nk1Values.contains("studentIndicator")){
            Json studentIndicator = jsonOrValuePropertyParse("nextOfKin.studentIndicator",nk1Values.string("studentIndicator"));
            populateCweField(nk1.getStudentIndicator(),studentIndicator);
        }
        //Populate NK1.25 "Religion" component
        if (nk1Values.contains("religion")){
            Json religion = jsonOrValuePropertyParse("nextOfKin.religion",nk1Values.string("religion"));
            populateCweField(nk1.getReligion(),religion);
        }
        //Populate NK1.26 "Mother's Maiden Name" component, which is repeatable
        if (nk1Values.contains("mothersMaidenName")){
            List<Json> mothersMaidenNameList = arrayPropertyToJson("nextOfKin.mothersMaidenName",nk1Values.string("mothersMaidenName"));
            for (int i=0; i < mothersMaidenNameList.size(); i++){
                Json currentMothersMaidenName = mothersMaidenNameList.get(i);
                populateXpnField(nk1.getMotherSMaidenName(i),currentMothersMaidenName);
            }
        }
        //Populate NK1.27 "Nationality" component
        if (nk1Values.contains("nationality")){
            Json nationality = jsonOrValuePropertyParse("nextOfKin.nationality",nk1Values.string("nationality"));
            populateCweField(nk1.getNationality(),nationality);
        }
        //Populate NK1.28 "Ethnic Group" component, which is repeatable
        if (nk1Values.contains("ethnicGroup")) {
            List<Json> ethnicGroupList = arrayPropertyToJson("ethnicGroup", nk1Values.string("ethnicGroup"));
            for (int i = 0; i < ethnicGroupList.size(); i++) {
                Json currentEthnicGroup = ethnicGroupList.get(i);
                populateCweField(nk1.getEthnicGroup(i),currentEthnicGroup);
            }
        }
        //Populate NK1.29 "Contact Reason" component, which is repeatable
        if (nk1Values.contains("contactReason")) {
            List<Json> contactReasonList = arrayPropertyToJson("contactReason", nk1Values.string("contactReason"));
            for (int i = 0; i < contactReasonList.size(); i++) {
                Json currentContactReason = contactReasonList.get(i);
                populateCweField(nk1.getContactReason(i),currentContactReason);
            }
        }
        //Populate NK1.30 "Contact Person's Name" component, which is repeatable
        if (nk1Values.contains("contactPersonsName")) {
            List<Json> contactPersonsNameList = arrayPropertyToJson("contactPersonsName", nk1Values.string("contactPersonsName"));
            for (int i = 0; i < contactPersonsNameList.size(); i++) {
                Json currentContactPersonsName = contactPersonsNameList.get(i);
                populateXpnField(nk1.getContactPersonSName(i),currentContactPersonsName);
            }
        }
        //Populate NK1.31 "Contact Person's Telephone Number" component, which is repeatable
        if (nk1Values.contains("contactPersonsTelephoneNumber")) {
            List<Json> contactPersonsTelephoneNumberList = multipleJsonPropertyParse("contactPersonsTelephoneNumber", nk1Values.string("contactPersonsTelephoneNumber"));
            for (int i = 0; i < contactPersonsTelephoneNumberList.size(); i++) {
                Json currentContactPersonsName = contactPersonsTelephoneNumberList.get(i);
                populateXtnField(nk1.getContactPersonSTelephoneNumber(i),currentContactPersonsName);
            }
        }
        //Populate NK1.32 "Contact Person's Address" component, which is repeatable
        if (nk1Values.contains("contactPersonsAddress")) {
            List<Json> contactPersonsAddressList = arrayPropertyToJson("contactPersonsAddress", nk1Values.string("contactPersonsAddress"));
            for (int i = 0; i < contactPersonsAddressList.size(); i++) {
                Json currentContactPersonsAddress = contactPersonsAddressList.get(i);
                populateXadField(nk1.getContactPersonSAddress(i),currentContactPersonsAddress);
            }
        }
        //Populate NK1.33 "Next Of Kin/Associated Party's Identifiers" component, which is repeatable
        if (nk1Values.contains("nextOfKinAssociatedPartysIdentifiers")) {
            List<Json> nextOfKinAssociatedPartysIdentifiersList = multipleJsonPropertyParse("nextOfKinAssociatedPartysIdentifiers", nk1Values.string("nextOfKinAssociatedPartysIdentifiers"));
            for (int i = 0; i < nextOfKinAssociatedPartysIdentifiersList.size(); i++) {
                Json currentNextOfKinAssociatedPartysIdentifiers = nextOfKinAssociatedPartysIdentifiersList.get(i);
                populateCxField(nk1.getNextOfKinAssociatedPartySIdentifiers(i),currentNextOfKinAssociatedPartysIdentifiers);
            }
        }
        //Populate NK1.34 "Job Status" component
        if (nk1Values.contains("jobStatus")){
            Json jobStatus = jsonOrValuePropertyParse("nextOfKin.jobStatus",nk1Values.string("jobStatus"));
            populateCweField(nk1.getJobStatus(),jobStatus);
        }
        //Populate NK1.35 "Race" component, which is repeatable
        if (nk1Values.contains("race")) {
            List<Json> raceList = arrayPropertyToJson("race", nk1Values.string("race"));
            for (int i = 0; i < raceList.size(); i++) {
                Json currentRace = raceList.get(i);
                populateCweField(nk1.getRace(i),currentRace);
            }
        }
        //Populate NK1.36 "Handicap" component
        if(nk1Values.contains("handicap")){
            Json handicap = jsonOrValuePropertyParse("handicap",nk1Values.string("handicap"));
            populateCweField(nk1.getHandicap(),handicap);
        }
        //Populate NK1.37 "Contact Person Social Security Number" component
        nk1.getContactPersonSocialSecurityNumber().setValue(
                nk1Values.contains("contactPersonSocialSecurityNumber") ? nk1Values.string("contactPersonSocialSecurityNumber") : ""
        );
        //Populate NK1.38 "Next Of Kin Birth Place" component
        nk1.getNextOfKinBirthPlace().setValue(
                nk1Values.contains("nextOfKinBirthPlace") ? nk1Values.string("nextOfKinBirthPlace") : ""
        );
        //Populate NK1.39 "Vip Indicator" component
        if(nk1Values.contains("vipIndicator")){
            Json vipIndicator = jsonOrValuePropertyParse("vipIndicator",nk1Values.string("vipIndicator"));
            populateCweField(nk1.getVIPIndicator(),vipIndicator);
        }
        //Populate NK1.40 "Next Of Kin Telecommunication Information" component
        if(nk1Values.contains("nextOfKinTelecommunicationInformation")){
            Json nextOfKinTelecommunicationInformation = singleJsonPropertyParse("nextOfKinTelecommunicationInformation",nk1Values.string("nextOfKinTelecommunicationInformation"));
            populateXtnField(nk1.getNextOfKinTelecommunicationInformation(),nextOfKinTelecommunicationInformation);
        }
        //Populate NK1.41 "Contact Person's Telecommunication Information" component
        if(nk1Values.contains("ContactPersonSTelecommunicationInformation")){
            Json ContactPersonSTelecommunicationInformation = singleJsonPropertyParse("ContactPersonSTelecommunicationInformation",nk1Values.string("ContactPersonSTelecommunicationInformation"));
            populateXtnField(nk1.getContactPersonSTelecommunicationInformation(),ContactPersonSTelecommunicationInformation);
        }
    }

    public static void populatePv1Segment(PV1 pv1, Json pv1Values) throws DataTypeException {
        String parentProp = "patientVisit.";

        //Populate PV1.1 "Set Id - Pv1" component
        pv1.getSetIDPV1().setValue(
                pv1Values.contains("setId") ? pv1Values.string("setId") : ""
        );
        //Populate PV1.2 "Patient Class" component
        if (pv1Values.contains("patientClass")){
            Json patientClass = jsonOrValuePropertyParse("patientClass", pv1Values.string("patientClass"));
            populateCweField(pv1.getPatientClass(),patientClass);
        }
        //Populate PV1.3 "Assigned Patient Location" component
        if (pv1Values.contains("assignedPatientLocation")){
            Json assignedPatientLocation = jsonOrValuePropertyParse("assignedPatientLocation", pv1Values.string("assignedPatientLocation"));
            populatePlField(pv1.getAssignedPatientLocation(),assignedPatientLocation);
        }
        //Populate PV1.4 "Admission Type" component
        if (pv1Values.contains("admissionType")){
            Json admissionType = jsonOrValuePropertyParse("admissionType", pv1Values.string("admissionType"));
            populateCweField(pv1.getAdmissionType(),admissionType);
        }
        //Populate PV1.5 "Preadmit Number" component
        if (pv1Values.contains("preadmitNumber")){
            Json preadmitNumber = singleJsonPropertyParse("preadmitNumber", pv1Values.string("preadmitNumber"));
            populateCxField(pv1.getPreadmitNumber(),preadmitNumber);
        }
        //Populate PV1.6 "Prior Patient Location" component
        if (pv1Values.contains("priorPatientLocation")){
            Json priorPatientLocation = jsonOrValuePropertyParse("priorPatientLocation", pv1Values.string("priorPatientLocation"));
            populatePlField(pv1.getPriorPatientLocation(),priorPatientLocation);
        }
        //Populate PV1.7 "Attending Doctor" component, which is repeatable
        if (pv1Values.contains("attendingDoctor")) {
            List<Json> attendingDoctorList = arrayPropertyToJson("attendingDoctor", pv1Values.string("attendingDoctor"));
            for (int i = 0; i < attendingDoctorList.size(); i++) {
                Json attendingDoctor = attendingDoctorList.get(i);
                populateXcnField(pv1.getAttendingDoctor(i),attendingDoctor);
            }
        }
        //Populate PV1.8 "Referring Doctor" component, which is repeatable
        if (pv1Values.contains("referringDoctor")) {
            List<Json> referringDoctorList = arrayPropertyToJson("referringDoctor", pv1Values.string("referringDoctor"));
            for (int i = 0; i < referringDoctorList.size(); i++) {
                Json referringDoctor = referringDoctorList.get(i);
                populateXcnField(pv1.getAttendingDoctor(i),referringDoctor);
            }
        }
        //Populate PV1.9 "Consulting Doctor" component, which is repeatable
        if (pv1Values.contains("consultingDoctor")) {
            List<Json> consultingDoctorList = arrayPropertyToJson("consultingDoctor", pv1Values.string("consultingDoctor"));
            for (int i = 0; i < consultingDoctorList.size(); i++) {
                Json consultingDoctor = consultingDoctorList.get(i);
                populateXcnField(pv1.getAttendingDoctor(i),consultingDoctor);
            }
        }
        //Populate PV1.10 "Consulting Doctor" component
        if (pv1Values.contains("hospitalService")){
            Json hospitalService = jsonOrValuePropertyParse("hospitalService", pv1Values.string("hospitalService"));
            populateCweField(pv1.getHospitalService(),hospitalService);
        }
        //Populate PV1.11 "Temporary Location" component
        if (pv1Values.contains("temporaryLocation")){
            Json temporaryLocation = jsonOrValuePropertyParse("temporaryLocation", pv1Values.string("temporaryLocation"));
            populatePlField(pv1.getTemporaryLocation(),temporaryLocation);
        }
        //Populate PV1.12 "Preadmit Test Indicator" component
        if (pv1Values.contains("preadmitTestIndicator")){
            Json preadmitTestIndicator = jsonOrValuePropertyParse("preadmitTestIndicator", pv1Values.string("preadmitTestIndicator"));
            populateCweField(pv1.getPreadmitTestIndicator(),preadmitTestIndicator);
        }
        //Populate PV1.13 "Re-admission Indicator" component
        if (pv1Values.contains("readmissionIndicator")){
            Json readmissionIndicator = jsonOrValuePropertyParse("readmissionIndicator", pv1Values.string("readmissionIndicator"));
            populateCweField(pv1.getReAdmissionIndicator(),readmissionIndicator);
        }
        //Populate PV1.14 "Admit Source" component
        if (pv1Values.contains("admitSource")){
            Json admitSource = jsonOrValuePropertyParse("admitSource", pv1Values.string("admitSource"));
            populateCweField(pv1.getAdmitSource(),admitSource);
        }
        //Populate PV1.15 "Ambulatory Status" component, which is repeatable
        if (pv1Values.contains("ambulatoryStatus")) {
            List<Json> ambulatoryStatusList = arrayPropertyToJson("ambulatoryStatus", pv1Values.string("ambulatoryStatus"));
            for (int i = 0; i < ambulatoryStatusList.size(); i++) {
                Json ambulatoryStatus = ambulatoryStatusList.get(i);
                populateCweField(pv1.getAmbulatoryStatus(i),ambulatoryStatus);
            }
        }
        //Populate PV1.16 "Vip Indicator" component
        if (pv1Values.contains("vipIndicator")){
            Json vipIndicator = jsonOrValuePropertyParse("vipIndicator", pv1Values.string("vipIndicator"));
            populateCweField(pv1.getVIPIndicator(),vipIndicator);
        }
        //Populate PV1.17 "Admitting Doctor" component, which is repeatable
        if (pv1Values.contains("admittingDoctor")) {
            List<Json> admittingDoctorList = arrayPropertyToJson("admittingDoctor", pv1Values.string("admittingDoctor"));
            for (int i = 0; i < admittingDoctorList.size(); i++) {
                Json admittingDoctor = admittingDoctorList.get(i);
                populateXcnField(pv1.getAdmittingDoctor(i),admittingDoctor);
            }
        }
        //Populate PV1.18 "Patient Type" component
        if (pv1Values.contains("patientType")){
            Json patientType = jsonOrValuePropertyParse("patientType", pv1Values.string("patientType"));
            populateCweField(pv1.getPatientType(),patientType);
        }
        //Populate PV1.19 "Visit Number" component
        if (pv1Values.contains("visitNumber")){
            Json visitNumber = singleJsonPropertyParse("visitNumber", pv1Values.string("visitNumber"));
            populateCxField(pv1.getVisitNumber(),visitNumber);
        }
        //Populate PV1.20 "Financial Class" component, which is repeatable
        if (pv1Values.contains("financialClass")) {
            List<Json> financialClassList = arrayPropertyToJson("financialClass", pv1Values.string("financialClass"));
            for (int i = 0; i < financialClassList.size(); i++) {
                Json financialClass = financialClassList.get(i);
                populateFcField(pv1.getFinancialClass(i),financialClass);
            }
        }
        //Populate PV1.21 "Charge Price Indicator" component
        if (pv1Values.contains("chargePriceIndicator")){
            Json chargePriceIndicator = jsonOrValuePropertyParse("chargePriceIndicator", pv1Values.string("chargePriceIndicator"));
            populateCweField(pv1.getChargePriceIndicator(),chargePriceIndicator);
        }
        //Populate PV1.22 "Courtesy Code" component
        if (pv1Values.contains("courtesyCode")){
            Json courtesyCode = jsonOrValuePropertyParse("courtesyCode", pv1Values.string("courtesyCode"));
            populateCweField(pv1.getCourtesyCode(),courtesyCode);
        }
        //Populate PV1.23 "Credit Rating" component
        if (pv1Values.contains("creditRating")){
            Json creditRating = jsonOrValuePropertyParse("creditRating", pv1Values.string("creditRating"));
            populateCweField(pv1.getCreditRating(),creditRating);
        }
        //Populate PV1.24 "Contract Code" component, which is repeatable
        if (pv1Values.contains("contractCode")) {
            List<Json> contractCodeList = arrayPropertyToJson("contractCode", pv1Values.string("contractCode"));
            for (int i = 0; i < contractCodeList.size(); i++) {
                Json contractCode = contractCodeList.get(i);
                populateCweField(pv1.getContractCode(i),contractCode);
            }
        }
        //Populate PV1.25 "Contract Effective Date" component, which is repeatable
        if (pv1Values.contains("contractEffectiveDate")) {
            List<Json> contractEffectiveDateList = arrayPropertyToJson("contractEffectiveDate", pv1Values.string("contractEffectiveDate"));
            for (int i = 0; i < contractEffectiveDateList.size(); i++) {
                Json contractEffectiveDate = contractEffectiveDateList.get(i);
                pv1.getContractEffectiveDate(i).setValue(
                        contractEffectiveDate.contains("mainValue") ? contractEffectiveDate.string("mainValue") : ""
                );
            }
        }
        //Populate PV1.26 "Contract Amount" component, which is repeatable
        if (pv1Values.contains("contractAmount")) {
            List<Json> contractAmountList = arrayPropertyToJson("contractAmount", pv1Values.string("contractAmount"));
            for (int i = 0; i < contractAmountList.size(); i++) {
                Json contractAmount = contractAmountList.get(i);
                pv1.getContractAmount(i).setValue(
                        contractAmount.contains("mainValue") ? contractAmount.string("mainValue") : ""
                );
            }
        }
        //Populate PV1.27 "Contract Period" component, which is repeatable
        if (pv1Values.contains("contractPeriod")) {
            List<Json> contractPeriodList = arrayPropertyToJson("contractPeriod", pv1Values.string("contractPeriod"));
            for (int i = 0; i < contractPeriodList.size(); i++) {
                Json contractPeriod = contractPeriodList.get(i);
                pv1.getContractPeriod(i).setValue(
                        contractPeriod.contains("mainValue") ? contractPeriod.string("mainValue") : ""
                );
            }
        }
        //Populate PV1.28 "Interest Code" component
        if (pv1Values.contains("interestCode")){
            Json interestCode = jsonOrValuePropertyParse("interestCode", pv1Values.string("interestCode"));
            populateCweField(pv1.getInterestCode(),interestCode);
        }
        //Populate PV1.29 "Transfer To Bad Debt Code" component
        if (pv1Values.contains("transferToBadDebtCode")){
            Json transferToBadDebtCode = jsonOrValuePropertyParse("transferToBadDebtCode", pv1Values.string("transferToBadDebtCode"));
            populateCweField(pv1.getTransferToBadDebtCode(),transferToBadDebtCode);
        }
        //Populate PV1.30 "Transfer To Bad Debt Date" component
        pv1.getTransferToBadDebtDate().setValue(
                pv1Values.contains("transferToBadDebtDate") ? pv1Values.string("transferToBadDebtDate") : ""
        );
        //Populate PV1.31 "Bad Debt Agency Code" component
        if (pv1Values.contains("badDebtAgencyCode")){
            Json badDebtAgencyCode = jsonOrValuePropertyParse("badDebtAgencyCode", pv1Values.string("badDebtAgencyCode"));
            populateCweField(pv1.getBadDebtAgencyCode(),badDebtAgencyCode);
        }
        //Populate PV1.32 "Bad Debt Transfer Amount" component
        pv1.getBadDebtTransferAmount().setValue(
                pv1Values.contains("badDebtTransferAmount") ? pv1Values.string("badDebtTransferAmount") : ""
        );
        //Populate PV1.33 "Bad Debt Recovery Amount" component
        pv1.getBadDebtRecoveryAmount().setValue(
                pv1Values.contains("badDebtRecoveryAmount") ? pv1Values.string("badDebtRecoveryAmount") : ""
        );
        //Populate PV1.34 "Delete Account Indicator" component
        if (pv1Values.contains("deleteAccountIndicator")){
            Json deleteAccountIndicator = jsonOrValuePropertyParse("deleteAccountIndicator", pv1Values.string("deleteAccountIndicator"));
            populateCweField(pv1.getDeleteAccountIndicator(),deleteAccountIndicator);
        }
        //Populate PV1.35 "Delete Account Date" component
        pv1.getDeleteAccountDate().setValue(
                pv1Values.contains("deleteAccountDate") ? pv1Values.string("deleteAccountDate") : ""
        );
        //Populate PV1.36 "Discharge Disposition" component
        if (pv1Values.contains("dischargeDisposition")){
            Json dischargeDisposition = jsonOrValuePropertyParse("dischargeDisposition", pv1Values.string("dischargeDisposition"));
            populateCweField(pv1.getDischargeDisposition(),dischargeDisposition);
        }
        //Populate PV1.37 "Discharged To Location" component
        if (pv1Values.contains("dischargedToLocation")){
            Json dischargedToLocation = jsonOrValuePropertyParse("dischargedToLocation", pv1Values.string("dischargedToLocation"));
            populateDldField(pv1.getDischargedToLocation(),dischargedToLocation);
        }
        //Populate PV1.38 "Diet Type" component
        if (pv1Values.contains("dietType")){
            Json dietType = jsonOrValuePropertyParse("dietType", pv1Values.string("dietType"));
            populateCweField(pv1.getDietType(),dietType);
        }
        //Populate PV1.39 "Servicing Facility" component
        if (pv1Values.contains("servicingFacility")){
            Json servicingFacility = jsonOrValuePropertyParse("servicingFacility", pv1Values.string("servicingFacility"));
            populateCweField(pv1.getServicingFacility(),servicingFacility);
        }
        //Populate PV1.40 "Bed Status" withdrawn
        //Populate PV1.41 "Account Status" component
        if (pv1Values.contains("accountStatus")){
            Json accountStatus = jsonOrValuePropertyParse("accountStatus", pv1Values.string("accountStatus"));
            populateCweField(pv1.getAccountStatus(),accountStatus);
        }
        //Populate PV1.42 "Pending Location" component
        if (pv1Values.contains("pendingLocation")){
            Json pendingLocation = jsonOrValuePropertyParse("pendingLocation", pv1Values.string("pendingLocation"));
            populatePlField(pv1.getPendingLocation(),pendingLocation);
        }
        //Populate PV1.43 "Prior Temporary Location" component
        if (pv1Values.contains("priorTemporaryLocation")){
            Json priorTemporaryLocation = jsonOrValuePropertyParse("priorTemporaryLocation", pv1Values.string("priorTemporaryLocation"));
            populatePlField(pv1.getPriorTemporaryLocation(),priorTemporaryLocation);
        }
        //Populate PV1.44 "Admit Date/Time" component
        pv1.getAdmitDateTime().setValue(
                pv1Values.contains("admitDateTime") ? pv1Values.string("admitDateTime") : ""
        );
        //Populate PV1.45 "Discharge Date/Time" component
        pv1.getDischargeDateTime().setValue(
                pv1Values.contains("dischargeDateTime") ? pv1Values.string("dischargeDateTime") : ""
        );
        //Populate PV1.46 "Current Patient Balance" component
        pv1.getCurrentPatientBalance().setValue(
                pv1Values.contains("currentPatientBalance") ? pv1Values.string("currentPatientBalance") : ""
        );
        //Populate PV1.47 "Total Charges" component
        pv1.getTotalCharges().setValue(
                pv1Values.contains("totalCharges") ? pv1Values.string("totalCharges") : ""
        );
        //Populate PV1.48 "Total Adjustments" component
        pv1.getTotalAdjustments().setValue(
                pv1Values.contains("totalAdjustments") ? pv1Values.string("totalAdjustments") : ""
        );
        //Populate PV1.49 "Total Payments" component
        pv1.getTotalPayments().setValue(
                pv1Values.contains("totalPayments") ? pv1Values.string("totalPayments") : ""
        );
        //Populate PV1.50 "Alternate Visit Id" component
        if (pv1Values.contains("alternateVisitId")){
            Json alternateVisitId = singleJsonPropertyParse("alternateVisitId", pv1Values.string("alternateVisitId"));
            populateCxField(pv1.getAlternateVisitID(0),alternateVisitId);
        }
        //Populate PV1.51 "Visit Indicator" component
        if (pv1Values.contains("visitIndicator")){
            Json visitIndicator = jsonOrValuePropertyParse("visitIndicator", pv1Values.string("visitIndicator"));
            populateCweField(pv1.getVisitIndicator(),visitIndicator);
        }
        //Populate PV1.52 "Other Healthcare Provider" withdrawn
        //Populate PV1.53 "Service Episode Description" component
        pv1.getServiceEpisodeDescription().setValue(
                pv1Values.contains("serviceEpisodeDescription") ? pv1Values.string("serviceEpisodeDescription") : ""
        );
        //Populate PV1.54 "Service Episode Identifier" component
        if (pv1Values.contains("serviceEpisodeIdentifier")){
            Json serviceEpisodeIdentifier = singleJsonPropertyParse("serviceEpisodeIdentifier", pv1Values.string("serviceEpisodeIdentifier"));
            populateCxField(pv1.getServiceEpisodeIdentifier(),serviceEpisodeIdentifier);
        }
    }

    public static void populatePv2Segment(PV2 pv2, Json pv2Values) throws DataTypeException {
        //Populate PV2.1 "Prior Pending Location" component
        if (pv2Values.contains("priorPendingLocation")){
            Json priorPendingLocation = jsonOrValuePropertyParse("priorPendingLocation", pv2Values.string("priorPendingLocation"));
            populatePlField(pv2.getPriorPendingLocation(),priorPendingLocation);
        }
        //Populate PV2.2 "Accommodation Code" component
        if (pv2Values.contains("accommodationCode")){
            Json accommodationCode = jsonOrValuePropertyParse("accommodationCode", pv2Values.string("accommodationCode"));
            populateCweField(pv2.getAccommodationCode(),accommodationCode);
        }
        //Populate PV2.3 "Admit Reason" component
        if (pv2Values.contains("admitReason")){
            Json admitReason = jsonOrValuePropertyParse("admitReason", pv2Values.string("admitReason"));
            populateCweField(pv2.getAdmitReason(),admitReason);
        }
        //Populate PV2.4 "Transfer Reason" component
        if (pv2Values.contains("transferReason")){
            Json transferReason = jsonOrValuePropertyParse("transferReason", pv2Values.string("transferReason"));
            populateCweField(pv2.getTransferReason(),transferReason);
        }
        //Populate PV2.5 "Patient Valuables" component, which is repeatable
        if (pv2Values.contains("patientValuables")) {
            List<Json> patientValuablesList = arrayPropertyToJson("patientValuables", pv2Values.string("patientValuables"));
            for (int i = 0; i < patientValuablesList.size(); i++) {
                Json patientValuables = patientValuablesList.get(i);
                pv2.getPatientValuables(i).setValue(
                        patientValuables.contains("mainValue") ? patientValuables.string("mainValue") : ""
                );
            }
        }
        //Populate PV2.6 "Patient Valuables Location" component
        pv2.getPatientValuablesLocation().setValue(
                pv2Values.contains("patientValuablesLocation") ? pv2Values.string("patientValuablesLocation") : ""
        );
        //Populate PV2.7 "Visit User Code" component, which is repeatable
        if (pv2Values.contains("visitUserCode")) {
            List<Json> visitUserCodeList = arrayPropertyToJson("visitUserCode", pv2Values.string("visitUserCode"));
            for (int i = 0; i < visitUserCodeList.size(); i++) {
                Json visitUserCode = visitUserCodeList.get(i);
                populateCweField(pv2.getVisitUserCode(i),visitUserCode);
            }
        }
        //Populate PV2.8 "Expected Admit Date/Time" component
        pv2.getExpectedAdmitDateTime().setValue(
                pv2Values.contains("expectedAdmitDateTime") ? pv2Values.string("expectedAdmitDateTime") : ""
        );
        //Populate PV2.9 "Expected Discharge Date/Time" component
        pv2.getExpectedDischargeDateTime().setValue(
                pv2Values.contains("expectedDischargeDateTime") ? pv2Values.string("expectedDischargeDateTime") : ""
        );
        //Populate PV2.10 "Estimated Length Of Inpatient Stay" component
        pv2.getEstimatedLengthOfInpatientStay().setValue(
                pv2Values.contains("estimatedLengthOfInpatientStay") ? pv2Values.string("estimatedLengthOfInpatientStay") : ""
        );
        //Populate PV2.11 "Actual Length Of Inpatient Stay" component
        pv2.getActualLengthOfInpatientStay().setValue(
                pv2Values.contains("actualLengthOfInpatientStay") ? pv2Values.string("actualLengthOfInpatientStay") : ""
        );
        //Populate PV2.12 "Visit Description" component
        pv2.getVisitDescription().setValue(
                pv2Values.contains("visitDescription") ? pv2Values.string("visitDescription") : ""
        );
        //Populate PV2.13 "Referral Source Code" component
        if (pv2Values.contains("referralSourceCode")){
            List<Json> referralSourceCodeList = arrayPropertyToJson("referralSourceCode", pv2Values.string("referralSourceCode"));
            for (int i = 0; i < referralSourceCodeList.size(); i++) {
                Json referralSourceCode = referralSourceCodeList.get(i);
                populateXcnField(pv2.getReferralSourceCode(i),referralSourceCode);
            }
        }
        //Populate PV2.14 "Previous Service Date" component
        pv2.getPreviousServiceDate().setValue(
                pv2Values.contains("previousServiceDate") ? pv2Values.string("previousServiceDate") : ""
        );
        //Populate PV2.15 "Employment Illness Related Indicator" component
        pv2.getEmploymentIllnessRelatedIndicator().setValue(
                pv2Values.contains("employmentIllnessRelatedIndicator") ? pv2Values.string("employmentIllnessRelatedIndicator") : ""
        );
        //Populate PV2.16 "Purge Status Code" component
        if (pv2Values.contains("purgeStatusCode")){
            Json purgeStatusCode = jsonOrValuePropertyParse("purgeStatusCode", pv2Values.string("purgeStatusCode"));
            populateCweField(pv2.getPurgeStatusCode(),purgeStatusCode);
        }
        //Populate PV2.17 "Purge Status Date" component
        pv2.getPurgeStatusDate().setValue(
                pv2Values.contains("purgeStatusDate") ? pv2Values.string("purgeStatusDate") : ""
        );
        //Populate PV2.18 "Special Program Code" component
        if (pv2Values.contains("specialProgramCode")){
            Json specialProgramCode = jsonOrValuePropertyParse("specialProgramCode", pv2Values.string("specialProgramCode"));
            populateCweField(pv2.getSpecialProgramCode(),specialProgramCode);
        }
        //Populate PV2.19 "Retention Indicator" component
        pv2.getRetentionIndicator().setValue(
                pv2Values.contains("retentionIndicator") ? pv2Values.string("retentionIndicator") : ""
        );
        //Populate PV2.20 "Expected Number Of Insurance Plans" component
        pv2.getExpectedNumberOfInsurancePlans().setValue(
                pv2Values.contains("expectedNumberOfInsurancePlans") ? pv2Values.string("expectedNumberOfInsurancePlans") : ""
        );
        //Populate PV2.21 "Visit Publicity Code" component
        if (pv2Values.contains("visitPublicityCode")){
            Json visitPublicityCode = jsonOrValuePropertyParse("visitPublicityCode", pv2Values.string("visitPublicityCode"));
            populateCweField(pv2.getVisitPublicityCode(),visitPublicityCode);
        }
        //Populate PV2.22 "Visit Protection Indicator" component
        pv2.getVisitProtectionIndicator().setValue(
                pv2Values.contains("visitProtectionIndicator") ? pv2Values.string("visitProtectionIndicator") : ""
        );
        //Populate PV2.23 "Clinic Organization Name" component
        if (pv2Values.contains("clinicOrganizationName")){
            List<Json> clinicOrganizationNameList = arrayPropertyToJson("clinicOrganizationName", pv2Values.string("clinicOrganizationName"));
            for (int i = 0; i < clinicOrganizationNameList.size(); i++) {
                Json clinicOrganizationName = clinicOrganizationNameList.get(i);
                populateXonField(pv2.getClinicOrganizationName(i),clinicOrganizationName);
            }
        }
        //Populate PV2.24 "Patient Status Code" component
        if (pv2Values.contains("patientStatusCode")){
            Json patientStatusCode = jsonOrValuePropertyParse("patientStatusCode", pv2Values.string("patientStatusCode"));
            populateCweField(pv2.getPatientStatusCode(),patientStatusCode);
        }
        //Populate PV2.25 "Visit Priority Code" component
        if (pv2Values.contains("visitPriorityCode")){
            Json visitPriorityCode = jsonOrValuePropertyParse("visitPriorityCode", pv2Values.string("visitPriorityCode"));
            populateCweField(pv2.getVisitPriorityCode(),visitPriorityCode);
        }
        //Populate PV2.26 "Previous Treatment Date" component
        pv2.getPreviousTreatmentDate().setValue(
                pv2Values.contains("previousTreatmentDate") ? pv2Values.string("previousTreatmentDate") : ""
        );
        //Populate PV2.27 "Expected Discharge Disposition" component
        if (pv2Values.contains("expectedDischargeDisposition")){
            Json expectedDischargeDisposition = jsonOrValuePropertyParse("expectedDischargeDisposition", pv2Values.string("expectedDischargeDisposition"));
            populateCweField(pv2.getExpectedDischargeDisposition(),expectedDischargeDisposition);
        }
        //Populate PV2.28 "Signature On File Date" component
        pv2.getSignatureOnFileDate().setValue(
                pv2Values.contains("signatureOnFileDate") ? pv2Values.string("signatureOnFileDate") : ""
        );
        //Populate PV2.29 "First Similar Illness Date" component
        pv2.getFirstSimilarIllnessDate().setValue(
                pv2Values.contains("firstSimilarIllnessDate") ? pv2Values.string("firstSimilarIllnessDate") : ""
        );
        //Populate PV2.30 "Patient Charge Adjustment Code" component
        if (pv2Values.contains("patientChargeAdjustmentCode")){
            Json patientChargeAdjustmentCode = jsonOrValuePropertyParse("patientChargeAdjustmentCode", pv2Values.string("patientChargeAdjustmentCode"));
            populateCweField(pv2.getPatientChargeAdjustmentCode(),patientChargeAdjustmentCode);
        }
        //Populate PV2.31 "Recurring Service Code" component
        if (pv2Values.contains("recurringServiceCode")){
            Json recurringServiceCode = jsonOrValuePropertyParse("recurringServiceCode", pv2Values.string("recurringServiceCode"));
            populateCweField(pv2.getRecurringServiceCode(),recurringServiceCode);
        }
        //Populate PV2.32 "Billing Media Code" component
        pv2.getBillingMediaCode().setValue(
                pv2Values.contains("billingMediaCode") ? pv2Values.string("billingMediaCode") : ""
        );
        //Populate PV2.33 "Expected Surgery Date And Time" component
        pv2.getExpectedSurgeryDateAndTime().setValue(
                pv2Values.contains("expectedSurgeryDateAndTime") ? pv2Values.string("expectedSurgeryDateAndTime") : ""
        );
        //Populate PV2.34 "Military Partnership Code" component
        pv2.getMilitaryPartnershipCode().setValue(
                pv2Values.contains("militaryPartnershipCode") ? pv2Values.string("militaryPartnershipCode") : ""
        );
        //Populate PV2.35 "Military Non-availability Code" component
        pv2.getMilitaryNonAvailabilityCode().setValue(
                pv2Values.contains("militaryNonAvailabilityCode") ? pv2Values.string("militaryNonAvailabilityCode") : ""
        );
        //Populate PV2.36 "Newborn Baby Indicator" component
        pv2.getNewbornBabyIndicator().setValue(
                pv2Values.contains("newbornBabyIndicator") ? pv2Values.string("newbornBabyIndicator") : ""
        );
        //Populate PV2.37 "Baby Detained Indicator" component
        pv2.getBabyDetainedIndicator().setValue(
                pv2Values.contains("babyDetainedIndicator") ? pv2Values.string("babyDetainedIndicator") : ""
        );
        //Populate PV2.38 "Mode Of Arrival Code" component
        if (pv2Values.contains("modeOfArrivalCode")){
            Json modeOfArrivalCode = jsonOrValuePropertyParse("modeOfArrivalCode", pv2Values.string("modeOfArrivalCode"));
            populateCweField(pv2.getModeOfArrivalCode(),modeOfArrivalCode);
        }
        //Populate PV2.39 "Recreational Drug Use Code" component
        if (pv2Values.contains("recreationalDrugUseCode")){
            List<Json> recreationalDrugUseCodeList = arrayPropertyToJson("recreationalDrugUseCode", pv2Values.string("recreationalDrugUseCode"));
            for (int i = 0; i < recreationalDrugUseCodeList.size(); i++) {
                Json recreationalDrugUseCode = recreationalDrugUseCodeList.get(i);
                populateCweField(pv2.getRecreationalDrugUseCode(i),recreationalDrugUseCode);
            }
        }
        //Populate PV2.40 "Admission Level Of Care Code" component
        if (pv2Values.contains("admissionLevelOfCareCode")){
            Json admissionLevelOfCareCode = jsonOrValuePropertyParse("admissionLevelOfCareCode", pv2Values.string("admissionLevelOfCareCode"));
            populateCweField(pv2.getAdmissionLevelOfCareCode(),admissionLevelOfCareCode);
        }
        //Populate PV2.41 "Precaution Code" component
        if (pv2Values.contains("precautionCode")){
            List<Json> precautionCodeList = arrayPropertyToJson("precautionCode", pv2Values.string("precautionCode"));
            for (int i = 0; i < precautionCodeList.size(); i++) {
                Json precautionCode = precautionCodeList.get(i);
                populateCweField(pv2.getPrecautionCode(i),precautionCode);
            }
        }
        //Populate PV2.42 "Patient Condition Code" component
        if (pv2Values.contains("patientConditionCode")){
            Json patientConditionCode = jsonOrValuePropertyParse("patientConditionCode", pv2Values.string("patientConditionCode"));
            populateCweField(pv2.getPatientConditionCode(),patientConditionCode);
        }
        //Populate PV2.43 "Living Will Code" component
        if (pv2Values.contains("livingWillCode")){
            Json livingWillCode = jsonOrValuePropertyParse("livingWillCode", pv2Values.string("livingWillCode"));
            populateCweField(pv2.getLivingWillCode(),livingWillCode);
        }
        //Populate PV2.44 "Organ Donor Code" component
        if (pv2Values.contains("organDonorCode")){
            Json organDonorCode = jsonOrValuePropertyParse("organDonorCode", pv2Values.string("organDonorCode"));
            populateCweField(pv2.getOrganDonorCode(),organDonorCode);
        }
        //Populate PV2.45 "Advance Directive Code" component
        if (pv2Values.contains("advanceDirectiveCode")){
            List<Json> advanceDirectiveCodeList = arrayPropertyToJson("advanceDirectiveCode", pv2Values.string("advanceDirectiveCode"));
            for (int i = 0; i < advanceDirectiveCodeList.size(); i++) {
                Json advanceDirectiveCode = advanceDirectiveCodeList.get(i);
                populateCweField(pv2.getAdvanceDirectiveCode(i),advanceDirectiveCode);
            }
        }
        //Populate PV2.46 "Patient Status Effective Date" component
        pv2.getPatientStatusEffectiveDate().setValue(
                pv2Values.contains("patientStatusEffectiveDate") ? pv2Values.string("patientStatusEffectiveDate") : ""
        );
        //Populate PV2.47 "Expected Loa Return Date/Time" component
        pv2.getExpectedLOAReturnDateTime().setValue(
                pv2Values.contains("expectedLoaReturnDateTime") ? pv2Values.string("expectedLoaReturnDateTime") : ""
        );
        //Populate PV2.48 "Expected Pre-admission Testing Date/Time" component
        pv2.getExpectedPreAdmissionTestingDateTime().setValue(
                pv2Values.contains("expectedPreAdmissionTestingDateTime") ? pv2Values.string("expectedPreAdmissionTestingDateTime") : ""
        );
        //Populate PV2.49 "Notify Clergy Code" component
        if (pv2Values.contains("notifyClergyCode")){
            List<Json> notifyClergyCodeList = arrayPropertyToJson("notifyClergyCode", pv2Values.string("notifyClergyCode"));
            for (int i = 0; i < notifyClergyCodeList.size(); i++) {
                Json notifyClergyCode = notifyClergyCodeList.get(i);
                populateCweField(pv2.getNotifyClergyCode(i),notifyClergyCode);
            }
        }
        //Populate PV2.50 "Advance Directive Last Verified Date" component
        pv2.getAdvanceDirectiveLastVerifiedDate().setValue(
                pv2Values.contains("advanceDirectiveLastVerifiedDate") ? pv2Values.string("advanceDirectiveLastVerifiedDate") : ""
        );
    }

    public static void populateDb1Segment(DB1 db1, Json db1Values) throws DataTypeException {
        //Populate DB1.1 "Set Id - Db1" component
        db1.getSetIDDB1().setValue(
                db1Values.contains("setId") ? db1Values.string("setId") : ""
        );
        //Populate DB1.2 "Disabled Person Code" component
        if (db1Values.contains("disabledPersonCode")){
            Json disabledPersonCode = jsonOrValuePropertyParse("disabledPersonCode", db1Values.string("disabledPersonCode"));
            populateCweField(db1.getDisabledPersonCode(),disabledPersonCode);
        }
        //Populate DB1.3 "Disabled Person Identifier" component
        if (db1Values.contains("disabledPersonIdentifier")){
            List<Json> advanceDirectiveCodeList = multipleJsonPropertyParse("disabledPersonIdentifier", db1Values.string("disabledPersonIdentifier   "));
            for (int i = 0; i < advanceDirectiveCodeList.size(); i++) {
                Json advanceDirectiveCode = advanceDirectiveCodeList.get(i);
                populateCxField(db1.getDisabledPersonIdentifier(i),advanceDirectiveCode);
            }
        }
        //Populate DB1.4 "Disability Indicator" component
        db1.getDisabilityIndicator().setValue(
                db1Values.contains("disabilityIndicator") ? db1Values.string("disabilityIndicator") : ""
        );
        //Populate DB1.5 "Disability Start Date" component
        db1.getDisabilityStartDate().setValue(
                db1Values.contains("disabilityStartDate") ? db1Values.string("disabilityStartDate") : ""
        );
        //Populate DB1.6 "Disability End Date" component
        db1.getDisabilityEndDate().setValue(
                db1Values.contains("disabilityEndDate") ? db1Values.string("disabilityEndDate") : ""
        );
        //Populate DB1.1 "Disability Return To Work Date" component
        db1.getDisabilityReturnToWorkDate().setValue(
                db1Values.contains("disabilityReturnToWorkDate") ? db1Values.string("disabilityReturnToWorkDate") : ""
        );
        //Populate DB1.1 "Disability Unable To Work Date" component
        db1.getDisabilityUnableToWorkDate().setValue(
                db1Values.contains("disabilityUnableToWorkDate") ? db1Values.string("disabilityUnableToWorkDate") : ""
        );
    }
    public static void populateObxSegment(AbstractMessage message, OBX obx, Json obxValues) throws DataTypeException {
        //Populate OBX.1 "Set Id - Obx" component
        obx.getSetIDOBX().setValue(
                obxValues.contains("setId") ? obxValues.string("setId") : ""
        );
        //Populate OBX.2 "Value Type" component
        obx.getValueType().setValue(
                obxValues.contains("valueType") ? obxValues.string("valueType") : ""
        );
        //Populate OBX.3 "Observation Identifier" component
        if (obxValues.contains("ObservationIdentifier")){
            Json observationIdentifier = jsonOrValuePropertyParse("observationIdentifier", obxValues.string("observationIdentifier"));
            populateCweField(obx.getObservationIdentifier(),observationIdentifier);
        }
        //Populate OBX.4 "Observation Sub-id" component
        obx.getObservationSubID().setValue(
                obxValues.contains("ObservationSubId") ? obxValues.string("ObservationSubId") : ""
        );
        //Populate OBX.5 "Observation Value" component, which is repeatable and depends on the field OBX.2
        if (obxValues.contains("observationValue")){
            List<Json> observationValueList = arrayPropertyToJson("observationValue", obxValues.string("observationValue"));
            for (int i = 0; i < observationValueList.size(); i++) {
                Json observationValue = observationValueList.get(i);
                populateVariesDataType(message,obx.getValueType().getValue(),obx.getObservationValue(i),observationValue);
            }
        }
        //Populate OBX.6 "Units" component
        if (obxValues.contains("units")){
            Json units = jsonOrValuePropertyParse("units", obxValues.string("units"));
            populateCweField(obx.getUnits(),units);
        }
        //Populate OBX.7 "References Range" component
        obx.getReferencesRange().setValue(
                obxValues.contains("referencesRange") ? obxValues.string("referencesRange") : ""
        );
        //Populate OBX.8 "Interpretation Codes" component, which is repeatable
        if (obxValues.contains("interpretationCodes")){
            List<Json> interpretationCodesList = arrayPropertyToJson("interpretationCodes", obxValues.string("interpretationCodes"));
            for (int i = 0; i < interpretationCodesList.size(); i++) {
                Json interpretationCodes = interpretationCodesList.get(i);
                populateCweField(obx.getInterpretationCodes(i),interpretationCodes);
            }
        }
        //Populate OBX.9 "Probability" component
        obx.getProbability().setValue(
                obxValues.contains("probability") ? obxValues.string("probability") : ""
        );
        //Populate OBX.10 "Nature Of Abnormal Test" component
        if (obxValues.contains("natureOfAbnormalTest")){
            List<Json> natureOfAbnormalTestList = arrayPropertyToJson("natureOfAbnormalTest", obxValues.string("natureOfAbnormalTest"));
            for (int i = 0; i < natureOfAbnormalTestList.size(); i++) {
                Json natureOfAbnormalTest = natureOfAbnormalTestList.get(i);
                obx.getNatureOfAbnormalTest(i).setValue(
                        natureOfAbnormalTest.contains("mainValue") ? natureOfAbnormalTest.string("mainValue") : ""
                );
            }
        }
        //Populate OBX.11 "Observation Result Status" component
        obx.getObservationResultStatus().setValue(
                obxValues.contains("observationResultStatus") ? obxValues.string("observationResultStatus") : ""
        );
        //Populate OBX.12 "Effective Date Of Reference Range" component
        obx.getEffectiveDateOfReferenceRange().setValue(
                obxValues.contains("effectiveDateOfReferenceRange") ? obxValues.string("effectiveDateOfReferenceRange") : ""
        );
        //Populate OBX.13 "User Defined Access Checks" component
        obx.getUserDefinedAccessChecks().setValue(
                obxValues.contains("userDefinedAccessChecks") ? obxValues.string("userDefinedAccessChecks") : ""
        );
        //Populate OBX.14 "Date/Time Of The Observation" component
        obx.getDateTimeOfTheObservation().setValue(
                obxValues.contains("dateTimeOfTheObservation") ? obxValues.string("dateTimeOfTheObservation") : ""
        );
        //Populate OBX.15 "Producer's Id" component
        if (obxValues.contains("producersId")){
            Json producersId = jsonOrValuePropertyParse("producersId", obxValues.string("producersId"));
            populateCweField(obx.getProducerSID(),producersId);
        }
        //Populate OBX.16 "Responsible Observer" component
        if (obxValues.contains("responsibleObserver")){
            List<Json> responsibleObserverList = arrayPropertyToJson("responsibleObserver", obxValues.string("responsibleObserver"));
            for (int i = 0; i < responsibleObserverList.size(); i++) {
                Json responsibleObserver = responsibleObserverList.get(i);
                populateXcnField(obx.getResponsibleObserver(i),responsibleObserver);
            }
        }
        //Populate OBX.17 "Observation Method" component
        if (obxValues.contains("observationMethod")){
            List<Json> observationMethodList = arrayPropertyToJson("observationMethod", obxValues.string("observationMethod"));
            for (int i = 0; i < observationMethodList.size(); i++) {
                Json observationMethod = observationMethodList.get(i);
                populateCweField(obx.getObservationMethod(i),observationMethod);
            }
        }
        //Populate OBX.18 "Equipment Instance Identifier" component
        if (obxValues.contains("equipmentInstanceIdentifier")){
            List<Json> equipmentInstanceIdentifierList = arrayPropertyToJson("equipmentInstanceIdentifier", obxValues.string("equipmentInstanceIdentifier"));
            for (int i = 0; i < equipmentInstanceIdentifierList.size(); i++) {
                Json equipmentInstanceIdentifier = equipmentInstanceIdentifierList.get(i);
                populateEiField(obx.getEquipmentInstanceIdentifier(i),equipmentInstanceIdentifier);
            }
        }
        //Populate OBX.19 "Date/Time Of The Analysis" component
        obx.getDateTimeOfTheAnalysis().setValue(
                obxValues.contains("dateTimeOfTheAnalysis") ? obxValues.string("dateTimeOfTheAnalysis") : ""
        );
        //Populate OBX.20 "Observation Site" component
        if (obxValues.contains("observationSite")){
            List<Json> observationSiteList = arrayPropertyToJson("observationSite", obxValues.string("observationSite"));
            for (int i = 0; i < observationSiteList.size(); i++) {
                Json observationSite = observationSiteList.get(i);
                populateCweField(obx.getObservationSite(i),observationSite);
            }
        }
        //Populate OBX.21 "Observation Instance Identifier" component
        if (obxValues.contains("observationInstanceIdentifier")){
            Json observationInstanceIdentifier = jsonOrValuePropertyParse("observationInstanceIdentifier", obxValues.string("observationInstanceIdentifier"));
            populateEiField(obx.getObservationInstanceIdentifier(),observationInstanceIdentifier);
        }
        //Populate OBX.22 "Mood Code" component
        if (obxValues.contains("moodCode")){
            Json moodCode = jsonOrValuePropertyParse("moodCode", obxValues.string("moodCode"));
            populateCneField(obx.getMoodCode(),moodCode);
        }
        //Populate OBX.23 "Performing Organization Name" component
        if (obxValues.contains("performingOrganizationName")){
            Json performingOrganizationName = jsonOrValuePropertyParse("performingOrganizationName", obxValues.string("performingOrganizationName"));
            populateXonField(obx.getPerformingOrganizationName(),performingOrganizationName);
        }
        //Populate OBX.24 "Performing Organization Address" component
        if (obxValues.contains("performingOrganizationAddress")){
            Json performingOrganizationAddress = jsonOrValuePropertyParse("performingOrganizationAddress", obxValues.string("performingOrganizationAddress"));
            populateXadField(obx.getPerformingOrganizationAddress(),performingOrganizationAddress);
        }
        //Populate OBX.25 "Performing Organization Medical Director" component
        if (obxValues.contains("performingOrganizationMedicalDirector")){
            Json performingOrganizationMedicalDirector = jsonOrValuePropertyParse("performingOrganizationMedicalDirector", obxValues.string("performingOrganizationMedicalDirector"));
            populateXcnField(obx.getPerformingOrganizationMedicalDirector(),performingOrganizationMedicalDirector);
        }
        //Populate OBX.26 "Patient Results Release Category" component
        obx.getPatientResultsReleaseCategory().setValue(
                obxValues.contains("patientResultsReleaseCategory") ? obxValues.string("patientResultsReleaseCategory") : ""
        );
        //Populate OBX.27 "Root Cause" component
        if (obxValues.contains("rootCause")){
            Json rootCause = jsonOrValuePropertyParse("rootCause", obxValues.string("rootCause"));
            populateCweField(obx.getRootCause(),rootCause);
        }
        //Populate OBX.28 "Local Process Control" component
        if (obxValues.contains("localProcessControl")){
            List<Json> localProcessControlList = arrayPropertyToJson("localProcessControl", obxValues.string("localProcessControl"));
            for (int i = 0; i < localProcessControlList.size(); i++) {
                Json localProcessControl = localProcessControlList.get(i);
                populateCweField(obx.getLocalProcessControl(i),localProcessControl);
            }
        }
    }

    public static void populateAl1Segment(AL1 al1, Json al1Values) throws DataTypeException {
        String parentProp = "patientAllergyInformation.";

        //Populate AL1.1 "Set Id - Al1" component
        al1.getSetIDAL1().setValue(
                al1Values.contains("setId") ? al1Values.string("setId") : ""
        );
        //Populate AL1.2 "Allergen Type Code" component
        if (al1Values.contains("allergenTypeCode")){
            Json allergenTypeCode = jsonOrValuePropertyParse(parentProp+"allergenTypeCode", al1Values.string("allergenTypeCode"));
            populateCweField(al1.getAllergenTypeCode(),allergenTypeCode);
        }
        //Populate AL1.3 "Allergen Code/Mnemonic/Description" component
        if (al1Values.contains("allergenCodeMnemonicDescription")){
            Json allergenCodeMnemonicDescription = jsonOrValuePropertyParse(parentProp+"allergenCodeMnemonicDescription", al1Values.string("allergenCodeMnemonicDescription"));
            populateCweField(al1.getAllergenCodeMnemonicDescription(),allergenCodeMnemonicDescription);
        }
        //Populate AL1.4 "Allergy Severity Code" component
        if (al1Values.contains("allergySeverityCode")){
            Json allergySeverityCode = jsonOrValuePropertyParse(parentProp+"allergySeverityCode", al1Values.string("allergySeverityCode"));
            populateCweField(al1.getAllergySeverityCode(),allergySeverityCode);
        }
        //Populate AL1.5 "Allergy Reaction Code" component
        if (al1Values.contains("allergyReactionCode")){
            List<String> allergyReactionCodeList = multipleValuesPropertyParse("allergyReactionCode", al1Values.string("allergyReactionCode"));
            for (int i = 0; i < allergyReactionCodeList.size(); i++) {
                String allergyReactionCode = allergyReactionCodeList.get(i);
                al1.getAllergyReactionCode(i).setValue(allergyReactionCode);
            }
        }
        //Populate AL1.6 "Identification Date" withdrawn
    }

    public static void populateDg1Segment(DG1 dg1, Json dg1Values) throws DataTypeException {
        String parentProp = "diagnosis.";

        //Populate DG1.1 "Set Id - Dg1" component
        dg1.getSetIDDG1().setValue(
                dg1Values.contains("setId") ? dg1Values.string("setId") : ""
        );
        //DG1.2 "Diagnosis Coding Method" Withdrawn
        //Populate DG1.3 "Diagnosis Code - Dg1" component
        if (dg1Values.contains("diagnosisCodeDg1")){
            Json diagnosisCodeDg1 = jsonOrValuePropertyParse(parentProp+"diagnosisCodeDg1", dg1Values.string("diagnosisCodeDg1"));
            populateCweField(dg1.getDiagnosisCodeDG1(),diagnosisCodeDg1);
        }
        //DG1.4 "Diagnosis Description" Withdrawn
        //Populate DG1.5 "Diagnosis Date/Time" component
        dg1.getDiagnosisDateTime().setValue(
                dg1Values.contains("diagnosisDateTime") ? dg1Values.string("diagnosisDateTime") : ""
        );
        //Populate DG1.6 "Diagnosis Type" component
        if (dg1Values.contains("diagnosisType")){
            Json diagnosisType = jsonOrValuePropertyParse(parentProp+"diagnosisType", dg1Values.string("diagnosisType"));
            populateCweField(dg1.getDiagnosisType(),diagnosisType);
        }
        //DG1.7 "Major Diagnostic Category" Withdrawn
        //DG1.8 "Diagnostic Related Group" Withdrawn
        //DG1.9 "Drg Approval Indicator" Withdrawn
        //DG1.10 "Drg Grouper Review Code" Withdrawn
        //DG1.11 "Outlier Type" Withdrawn
        //DG1.12 "Outlier Days" Withdrawn
        //DG1.13 "Outlier Cost" Withdrawn
        //DG1.14 "Grouper Version And Type" Withdrawn
        //Populate DG1.15 "Diagnosis Priority" component
        dg1.getDiagnosisPriority().setValue(
                dg1Values.contains("diagnosisPriority") ? dg1Values.string("diagnosisPriority") : ""
        );
        //Populate DG1.16 "Diagnosing Clinician" component
        if (dg1Values.contains("diagnosingClinician")){
            List<Json> diagnosingClinicianList = arrayPropertyToJson(parentProp+"diagnosingClinician", dg1Values.string("diagnosingClinician"));
            for (int i = 0; i < diagnosingClinicianList.size(); i++) {
                Json diagnosingClinician = diagnosingClinicianList.get(i);
                populateXcnField(dg1.getDiagnosingClinician(i),diagnosingClinician);
            }
        }
        //Populate DG1.17 "Diagnosis Classification" component
        if (dg1Values.contains("diagnosisClassification")){
            Json diagnosisClassification = jsonOrValuePropertyParse(parentProp+"diagnosisClassification", dg1Values.string("diagnosisClassification"));
            populateCweField(dg1.getDiagnosisClassification(),diagnosisClassification);
        }
        //Populate DG1.18 "Confidential Indicator" component
        dg1.getConfidentialIndicator().setValue(
                dg1Values.contains("confidentialIndicator") ? dg1Values.string("confidentialIndicator") : ""
        );
        //Populate DG1.19 "Attestation Date/Time" component
        dg1.getAttestationDateTime().setValue(
                dg1Values.contains("attestationDateTime") ? dg1Values.string("attestationDateTime") : ""
        );
        //Populate DG1.20 "Diagnosis Identifier" component
        if (dg1Values.contains("diagnosisIdentifier")){
            Json diagnosisIdentifier = jsonOrValuePropertyParse(parentProp+"diagnosisIdentifier", dg1Values.string("diagnosisIdentifier"));
            populateEiField(dg1.getDiagnosisIdentifier(),diagnosisIdentifier);
        }
        //Populate DG1.21 "Diagnosis Action Code" component
        dg1.getDiagnosisActionCode().setValue(
                dg1Values.contains("diagnosisActionCode") ? dg1Values.string("diagnosisActionCode") : ""
        );
        //Populate DG1.22 "Parent Diagnosis" component
        if (dg1Values.contains("parentDiagnosis")){
            Json parentDiagnosis = jsonOrValuePropertyParse(parentProp+"parentDiagnosis", dg1Values.string("parentDiagnosis"));
            populateEiField(dg1.getDiagnosisIdentifier(),parentDiagnosis);
        }
        //Populate DG1.23 "Drg Ccl Value Code" component
        if (dg1Values.contains("drgCclValueCode")){
            Json drgCclValueCode = jsonOrValuePropertyParse(parentProp+"drgCclValueCode", dg1Values.string("drgCclValueCode"));
            populateCweField(dg1.getDRGCCLValueCode(),drgCclValueCode);
        }
        //Populate DG1.24 "Drg Grouping Usage" component
        dg1.getDRGGroupingUsage().setValue(
                dg1Values.contains("drgGroupingUsage") ? dg1Values.string("drgGroupingUsage") : ""
        );
        //Populate DG1.25 "Drg Diagnosis Determination Status" component
        if (dg1Values.contains("drgDiagnosisDeterminationStatus")){
            Json drgDiagnosisDeterminationStatus = jsonOrValuePropertyParse(parentProp+"drgDiagnosisDeterminationStatus", dg1Values.string("drgDiagnosisDeterminationStatus"));
            populateCweField(dg1.getDRGDiagnosisDeterminationStatus(),drgDiagnosisDeterminationStatus);
        }
        //Populate DG1.26 "Present On Admission (poa) Indicator" component
        if (dg1Values.contains("presentOnAdmissionIndicator")){
            Json presentOnAdmissionIndicator = jsonOrValuePropertyParse(parentProp+"presentOnAdmissionIndicator", dg1Values.string("presentOnAdmissionIndicator"));
            populateCweField(dg1.getPresentOnAdmissionIndicator(),presentOnAdmissionIndicator);
        }

    }

    public static void populateIn1Segment(IN1 in1, Json in1Values) throws DataTypeException {
        //Populate IN1.1 "Set Id - In1" component
        in1.getSetIDIN1().setValue(
                in1Values.contains("setId") ? in1Values.string("setId") : ""
        );
        //Populate IN1.2 "Health Plan Id" component
        if (in1Values.contains("healthPlanId")){
            Json healthPlanId = jsonOrValuePropertyParse("healthPlanId", in1Values.string("healthPlanId"));
            populateCweField(in1.getHealthPlanID(),healthPlanId);
        }
        //Populate IN1.3 "Insurance Company Id" component
        if (in1Values.contains("insuranceCompanyId")){
            List<Json> insuranceCompanyIdList = multipleJsonPropertyParse("insuranceCompanyId", in1Values.string("insuranceCompanyId"));
            for (int i = 0; i < insuranceCompanyIdList.size(); i++) {
                Json insuranceCompanyId = insuranceCompanyIdList.get(i);
                populateCxField(in1.getInsuranceCompanyID(i),insuranceCompanyId);
            }
        }
        //Populate IN1.4 "Insurance Company Name" component
        if (in1Values.contains("insuranceCompanyName")){
            List<Json> insuranceCompanyNameList = arrayPropertyToJson("insuranceCompanyName", in1Values.string("insuranceCompanyName"));
            for (int i = 0; i < insuranceCompanyNameList.size(); i++) {
                Json insuranceCompanyName = insuranceCompanyNameList.get(i);
                populateXonField(in1.getInsuranceCompanyName(i),insuranceCompanyName);
            }
        }
        //Populate IN1.5 "Insurance Company Address" component
        if (in1Values.contains("insuranceCompanyAddress")){
            List<Json> insuranceCompanyAddressList = arrayPropertyToJson("insuranceCompanyAddress", in1Values.string("insuranceCompanyAddress"));
            for (int i = 0; i < insuranceCompanyAddressList.size(); i++) {
                Json insuranceCompanyAddress = insuranceCompanyAddressList.get(i);
                populateXadField(in1.getInsuranceCompanyAddress(i),insuranceCompanyAddress);
            }
        }
        //Populate IN1.6 "Insurance Co Contact Person" component
        if (in1Values.contains("insuranceCoContactPerson")){
            List<Json> insuranceCoContactPersonList = arrayPropertyToJson("insuranceCoContactPerson", in1Values.string("insuranceCoContactPerson"));
            for (int i = 0; i < insuranceCoContactPersonList.size(); i++) {
                Json insuranceCoContactPerson = insuranceCoContactPersonList.get(i);
                populateXpnField(in1.getInsuranceCoContactPerson(i),insuranceCoContactPerson);
            }
        }
        //Populate IN1.7 "Insurance Co Phone Number" component
        if (in1Values.contains("insuranceCoPhoneNumber")){
            List<Json> insuranceCoPhoneNumberList = multipleJsonPropertyParse("insuranceCoPhoneNumber", in1Values.string("insuranceCoPhoneNumber"));
            for (int i = 0; i < insuranceCoPhoneNumberList.size(); i++) {
                Json insuranceCoPhoneNumber = insuranceCoPhoneNumberList.get(i);
                populateXtnField(in1.getInsuranceCoPhoneNumber(i),insuranceCoPhoneNumber);
            }
        }
        //Populate IN1.8 "Group Number" component
        in1.getGroupNumber().setValue(
                in1Values.contains("groupNumber") ? in1Values.string("groupNumber") : ""
        );
        //Populate IN1.9 "Group Name" component
        if (in1Values.contains("groupName")){
            List<Json> groupNameList = arrayPropertyToJson("groupName", in1Values.string("groupName"));
            for (int i = 0; i < groupNameList.size(); i++) {
                Json groupName = groupNameList.get(i);
                populateXonField(in1.getGroupName(i),groupName);
            }
        }
        //Populate IN1.10 "Insured's Group Emp Id" component
        if (in1Values.contains("insuredsGroupEmpID")){
            List<Json> insuredSGroupEmpIDList = multipleJsonPropertyParse("insuredSGroupEmpID", in1Values.string("insuredSGroupEmpID"));
            for (int i = 0; i < insuredSGroupEmpIDList.size(); i++) {
                Json insuredSGroupEmpID = insuredSGroupEmpIDList.get(i);
                populateCxField(in1.getInsuredSGroupEmpID(i),insuredSGroupEmpID);
            }
        }
        //Populate IN1.11 "Insured's Group Emp Name" component
        if (in1Values.contains("insuredsGroupEmpName")){
            List<Json> insuredsGroupEmpNameList = arrayPropertyToJson("insuredsGroupEmpName", in1Values.string("insuredsGroupEmpName"));
            for (int i = 0; i < insuredsGroupEmpNameList.size(); i++) {
                Json insuredsGroupEmpName = insuredsGroupEmpNameList.get(i);
                populateXonField(in1.getInsuredSGroupEmpName(i),insuredsGroupEmpName);
            }
        }
        //Populate IN1.12 "Plan Effective Date" component
        in1.getPlanEffectiveDate().setValue(
                in1Values.contains("planEffectiveDate") ? in1Values.string("planEffectiveDate") : ""
        );
        //Populate IN1.13 "Plan Expiration Date" component
        in1.getPlanExpirationDate().setValue(
                in1Values.contains("planExpirationDate") ? in1Values.string("planExpirationDate") : ""
        );
        //Populate IN1.14 "Authorization Information" component
        if (in1Values.contains("authorizationInformation")){
            Json authorizationInformation = jsonOrValuePropertyParse("authorizationInformation", in1Values.string("authorizationInformation"));
            populateAuiField(in1.getAuthorizationInformation(),authorizationInformation);
        }
        //Populate IN1.15 "Plan Type" component
        if (in1Values.contains("planType")){
            Json planType = jsonOrValuePropertyParse("planType", in1Values.string("planType"));
            populateCweField(in1.getPlanType(),planType);
        }
        //Populate IN1.16 "Name Of Insured" component
        if (in1Values.contains("nameOfInsured")){
            List<Json> nameOfInsuredList = arrayPropertyToJson("nameOfInsured", in1Values.string("nameOfInsured"));
            for (int i = 0; i < nameOfInsuredList.size(); i++) {
                Json nameOfInsured = nameOfInsuredList.get(i);
                populateXpnField(in1.getNameOfInsured(i),nameOfInsured);
            }
        }
        //Populate IN1.17 "Insured's Relationship To Patient" component
        if (in1Values.contains("insuredsRelationshipToPatient")){
            Json insuredsRelationshipToPatient = jsonOrValuePropertyParse("insuredsRelationshipToPatient", in1Values.string("insuredsRelationshipToPatient"));
            populateCweField(in1.getInsuredSRelationshipToPatient(),insuredsRelationshipToPatient);
        }
        //Populate IN1.18 "Insured's Date Of Birth" component
        in1.getInsuredSDateOfBirth().setValue(
                in1Values.contains("insuredsDateOfBirth") ? in1Values.string("insuredsDateOfBirth") : ""
        );
        //Populate IN1.19 "Insured's Address" component
        if (in1Values.contains("insuredsAddress")){
            List<Json> insuredsAddressList = arrayPropertyToJson("insuredsAddress", in1Values.string("insuredsAddress"));
            for (int i = 0; i < insuredsAddressList.size(); i++) {
                Json insuredsAddress = insuredsAddressList.get(i);
                populateXadField(in1.getInsuredSAddress(i),insuredsAddress);
            }
        }
        //Populate IN1.20 "Assignment Of Benefits" component
        if (in1Values.contains("assignmentOfBenefits")){
            Json assignmentOfBenefits = jsonOrValuePropertyParse("assignmentOfBenefits", in1Values.string("assignmentOfBenefits"));
            populateCweField(in1.getAssignmentOfBenefits(),assignmentOfBenefits);
        }
        //Populate IN1.21 "Coordination Of Benefits" component
        if (in1Values.contains("coordinationOfBenefits")){
            Json coordinationOfBenefits = jsonOrValuePropertyParse("coordinationOfBenefits", in1Values.string("coordinationOfBenefits"));
            populateCweField(in1.getCoordinationOfBenefits(),coordinationOfBenefits);
        }
        //Populate IN1.22 "Coord Of Ben. Priority" component
        in1.getCoordOfBenPriority().setValue(
                in1Values.contains("coordOfBenPriority") ? in1Values.string("coordOfBenPriority") : ""
        );
        //Populate IN1.23 "Notice Of Admission Flag" component
        in1.getNoticeOfAdmissionFlag().setValue(
                in1Values.contains("noticeOfAdmissionFlag") ? in1Values.string("noticeOfAdmissionFlag") : ""
        );
        //Populate IN1.24 "Notice Of Admission Date" component
        in1.getNoticeOfAdmissionDate().setValue(
                in1Values.contains("noticeOfAdmissionDate") ? in1Values.string("noticeOfAdmissionDate") : ""
        );
        //Populate IN1.25 "Report Of Eligibility Flag" component
        in1.getReportOfEligibilityFlag().setValue(
                in1Values.contains("reportOfEligibilityFlag") ? in1Values.string("reportOfEligibilityFlag") : ""
        );
        //Populate IN1.26 "Report Of Eligibility Date" component
        in1.getReportOfEligibilityDate().setValue(
                in1Values.contains("reportOfEligibilityDate") ? in1Values.string("reportOfEligibilityDate") : ""
        );
        //Populate IN1.27 "Release Information Code" component
        if (in1Values.contains("releaseInformationCode")){
            Json releaseInformationCode = jsonOrValuePropertyParse("releaseInformationCode", in1Values.string("releaseInformationCode"));
            populateCweField(in1.getReleaseInformationCode(),releaseInformationCode);
        }
        //Populate IN1.28 "Pre-admit Cert (pac)" component
        in1.getPreAdmitCert().setValue(
                in1Values.contains("preAdmitCert") ? in1Values.string("preAdmitCert") : ""
        );
        //Populate IN1.29 "Verification Date/Time" component
        in1.getVerificationDateTime().setValue(
                in1Values.contains("verificationDateTime") ? in1Values.string("verificationDateTime") : ""
        );
        //Populate IN1.30 "Verification By" component
        if (in1Values.contains("verificationBy")){
            List<Json> verificationByList = arrayPropertyToJson("verificationBy", in1Values.string("verificationBy"));
            for (int i = 0; i < verificationByList.size(); i++) {
                Json verificationBy = verificationByList.get(i);
                populateXcnField(in1.getVerificationBy(i),verificationBy);
            }
        }
        //Populate IN1.31 "Type Of Agreement Code" component
        if (in1Values.contains("typeOfAgreementCode")){
            Json typeOfAgreementCode = jsonOrValuePropertyParse("typeOfAgreementCode", in1Values.string("typeOfAgreementCode"));
            populateCweField(in1.getTypeOfAgreementCode(),typeOfAgreementCode);
        }
        //Populate IN1.32 "Billing Status" component
        if (in1Values.contains("billingStatus")){
            Json billingStatus = jsonOrValuePropertyParse("billingStatus", in1Values.string("billingStatus"));
            populateCweField(in1.getBillingStatus(),billingStatus);
        }
        //Populate IN1.33 "Lifetime Reserve Days" component
        in1.getLifetimeReserveDays().setValue(
                in1Values.contains("lifetimeReserveDays") ? in1Values.string("lifetimeReserveDays") : ""
        );
        //Populate IN1.34 "Delay Before L.r. Day" component
        in1.getDelayBeforeLRDay().setValue(
                in1Values.contains("delayBeforeLrDay") ? in1Values.string("delayBeforeLrDay") : ""
        );
        //Populate IN1.35 "Company Plan Code" component
        if (in1Values.contains("companyPlanCode")){
            Json companyPlanCode = jsonOrValuePropertyParse("companyPlanCode", in1Values.string("companyPlanCode"));
            populateCweField(in1.getCompanyPlanCode(),companyPlanCode);
        }
        //Populate IN1.36 "Policy Number" component
        in1.getPolicyNumber().setValue(
                in1Values.contains("policyNumber") ? in1Values.string("policyNumber") : ""
        );
        //Populate IN1.37 "Company Plan Code" component
        if (in1Values.contains("policyDeductible")){
            Json policyDeductible = jsonOrValuePropertyParse("policyDeductible", in1Values.string("policyDeductible"));
            populateCpField(in1.getPolicyDeductible(),policyDeductible);
        }
        //Populate IN1.38 "Policy Limit - Amount" withdrawn
        //Populate IN1.39 "Policy Limit - Days" component
        in1.getPolicyLimitDays().setValue(
                in1Values.contains("policyLimitDays") ? in1Values.string("policyLimitDays") : ""
        );
        //Populate IN1.40 "Room Rate - Semi-private" withdrawn
        //Populate IN1.41 "Room Rate - Private" withdrawn
        //Populate IN1.42 "Insured's Employment Status" component
        if (in1Values.contains("insuredsEmploymentStatus")){
            Json insuredsEmploymentStatus = jsonOrValuePropertyParse("insuredsEmploymentStatus", in1Values.string("insuredsEmploymentStatus"));
            populateCweField(in1.getInsuredSEmploymentStatus(),insuredsEmploymentStatus);
        }
        //Populate IN1.43 "Insured's Administrative Sex" component
        if (in1Values.contains("insuredsAdministrativeSex")){
            Json insuredsAdministrativeSex = jsonOrValuePropertyParse("insuredsAdministrativeSex", in1Values.string("insuredsAdministrativeSex"));
            populateCweField(in1.getInsuredSAdministrativeSex(),insuredsAdministrativeSex);
        }
        //Populate IN1.44 "Insured's Employer's Address" component
        if (in1Values.contains("insuredsEmployersAddress")){
            List<Json> insuredsEmployersAddressList = arrayPropertyToJson("insuredsEmployersAddress", in1Values.string("insuredsEmployersAddress"));
            for (int i = 0; i < insuredsEmployersAddressList.size(); i++) {
                Json insuredsEmployersAddress = insuredsEmployersAddressList.get(i);
                populateXadField(in1.getInsuredSEmployerSAddress(i),insuredsEmployersAddress);
            }
        }
        //Populate IN1.45 "Verification Status" component
        in1.getVerificationStatus().setValue(
                in1Values.contains("verificationStatus") ? in1Values.string("verificationStatus") : ""
        );
        //Populate IN1.46 "Prior Insurance Plan Id" component
        if (in1Values.contains("priorInsurancePlanId")){
            Json priorInsurancePlanId = jsonOrValuePropertyParse("priorInsurancePlanId", in1Values.string("priorInsurancePlanId"));
            populateCweField(in1.getPriorInsurancePlanID(),priorInsurancePlanId);
        }
        //Populate IN1.47 "Coverage Type" component
        if (in1Values.contains("coverageType")){
            Json coverageType = jsonOrValuePropertyParse("coverageType", in1Values.string("coverageType"));
            populateCweField(in1.getCoverageType(),coverageType);
        }
        //Populate IN1.48 "Handicap" component
        if (in1Values.contains("handicap")){
            Json handicap = jsonOrValuePropertyParse("handicap", in1Values.string("handicap"));
            populateCweField(in1.getHandicap(),handicap);
        }
        //Populate IN1.49 "Insured's Id Number" component, which is repeatable
        if (in1Values.contains("insuredsIdNumber")){
            List<Json> insuredsIdNumberList = multipleJsonPropertyParse("insuredsIdNumber", in1Values.string("insuredsIdNumber"));
            for (int i = 0; i < insuredsIdNumberList.size(); i++) {
                Json insuredsIdNumber = insuredsIdNumberList.get(i);
                populateCxField(in1.getInsuredSIDNumber(i),insuredsIdNumber);
            }
        }
        //Populate IN1.50 "Signature Code" component
        if (in1Values.contains("signatureCode")){
            Json signatureCode = jsonOrValuePropertyParse("signatureCode", in1Values.string("signatureCode"));
            populateCweField(in1.getSignatureCode(),signatureCode);
        }
        //Populate IN1.51 "Signature Code Date" component
        in1.getSignatureCodeDate().setValue(
                in1Values.contains("signatureCodeDate") ? in1Values.string("signatureCodeDate") : ""
        );
        //Populate IN1.52 "Insured's Birth Place" component
        in1.getInsuredSBirthPlace().setValue(
                in1Values.contains("insuredsBirthPlace") ? in1Values.string("insuredsBirthPlace") : ""
        );
        //Populate IN1.53 "Vip Indicator" component
        if (in1Values.contains("vipIndicator")){
            Json vipIndicator = jsonOrValuePropertyParse("vipIndicator", in1Values.string("vipIndicator"));
            populateCweField(in1.getVIPIndicator(),vipIndicator);
        }
        //Populate IN1.54 "External Health Plan Identifiers" component, which is repeatable
        if (in1Values.contains("externalHealthPlanIdentifiers")){
            List<Json> externalHealthPlanIdentifiersList = multipleJsonPropertyParse("externalHealthPlanIdentifiers", in1Values.string("externalHealthPlanIdentifiers"));
            for (int i = 0; i < externalHealthPlanIdentifiersList.size(); i++) {
                Json externalHealthPlanIdentifiers = externalHealthPlanIdentifiersList.get(i);
                populateCxField(in1.getExternalHealthPlanIdentifiers(i),externalHealthPlanIdentifiers);
            }
        }
        //Populate IN1.55 "Insurance Action Code" component
        in1.getInsuranceActionCode().setValue(
                in1Values.contains("insuranceActionCode") ? in1Values.string("insuranceActionCode") : ""
        );
    }

    public static void populateIn2Segment(IN2 in2, Json in2Values) throws DataTypeException {
        String parentProp = "insuranceAdditionalInformation.";

        //Populate IN2.1 "Insured's Employee Id" component, which is repeatable
        if (in2Values.contains("insuredsEmployeeId")){
            List<Json> insuredsEmployeeIdList = multipleJsonPropertyParse(parentProp+"insuredsEmployeeId", in2Values.string("insuredsEmployeeId"));
            for (int i = 0; i < insuredsEmployeeIdList.size(); i++) {
                Json insuredsEmployeeId = insuredsEmployeeIdList.get(i);
                populateCxField(in2.getInsuredSEmployeeID(i),insuredsEmployeeId);
            }
        }
        //Populate IN2.2 "Insured's Social Security Number" component
        in2.getInsuredSSocialSecurityNumber().setValue(
                in2Values.contains("insuredsSocialSecurityNumber") ? in2Values.string("insuredsSocialSecurityNumber") : ""
        );
        //Populate IN2.3 "Insured's Employer's Name And Id" component, which is repeatable
        if (in2Values.contains("insuredsEmployersNameAndId")){
            List<Json> insuredsEmployersNameAndIdList = arrayPropertyToJson(parentProp+"insuredsEmployersNameAndId", in2Values.string("insuredsEmployersNameAndId"));
            for (int i = 0; i < insuredsEmployersNameAndIdList.size(); i++) {
                Json insuredsEmployersNameAndId = insuredsEmployersNameAndIdList.get(i);
                populateXcnField(in2.getInsuredSEmployerSNameAndID(i),insuredsEmployersNameAndId);
            }
        }
        //Populate IN2.4 "Employer Information Data" component
        if (in2Values.contains("employerInformationData")){
            Json employerInformationData = jsonOrValuePropertyParse(parentProp+"employerInformationData", in2Values.string("employerInformationData"));
            populateCweField(in2.getEmployerInformationData(),employerInformationData);
        }
        //Populate IN2.5 "Mail Claim Party" component, which is repeatable
        if (in2Values.contains("mailClaimParty")){
            List<Json> mailClaimPartyList = arrayPropertyToJson(parentProp+"mailClaimParty", in2Values.string("mailClaimParty"));
            for (int i = 0; i < mailClaimPartyList.size(); i++) {
                Json mailClaimParty = mailClaimPartyList.get(i);
                populateCweField(in2.getMailClaimParty(i),mailClaimParty);
            }
        }
        //Populate IN2.6 "Medicare Health Ins Card Number" component
        in2.getMedicareHealthInsCardNumber().setValue(
                in2Values.contains("medicareHealthInsCardNumber") ? in2Values.string("medicareHealthInsCardNumber") : ""
        );
        //Populate IN2.7 "Medicaid Case Name" component, which is repeatable
            if (in2Values.contains("medicaidCaseName")){
            List<Json> medicaidCaseNameList = arrayPropertyToJson(parentProp+"medicaidCaseName", in2Values.string("medicaidCaseName"));
            for (int i = 0; i < medicaidCaseNameList.size(); i++) {
                Json medicaidCaseName = medicaidCaseNameList.get(i);
                populateXpnField(in2.getMedicaidCaseName(i),medicaidCaseName);
            }
        }
        //Populate IN2.8 "Medicaid Case Number" component
        in2.getMedicaidCaseNumber().setValue(
                in2Values.contains("medicaidCaseNumber") ? in2Values.string("medicaidCaseNumber") : ""
        );
        //Populate IN2.9 "Military Sponsor Name" component, which is repeatable
        if (in2Values.contains("militarySponsorName")){
            List<Json> militarySponsorNameList = arrayPropertyToJson(parentProp+"militarySponsorName", in2Values.string("militarySponsorName"));
            for (int i = 0; i < militarySponsorNameList.size(); i++) {
                Json militarySponsorName = militarySponsorNameList.get(i);
                populateXpnField(in2.getMilitarySponsorName(i),militarySponsorName);
            }
        }
        //Populate IN2.10 "Military Id Number" component
        in2.getMilitaryIDNumber().setValue(
                in2Values.contains("militaryIdNumber") ? in2Values.string("militaryIdNumber") : ""
        );
        //Populate IN2.11 "Dependent Of Military Recipient" component
        if (in2Values.contains("dependentOfMilitaryRecipient")){
            Json dependentOfMilitaryRecipient = jsonOrValuePropertyParse(parentProp+"dependentOfMilitaryRecipient", in2Values.string("dependentOfMilitaryRecipient"));
            populateCweField(in2.getDependentOfMilitaryRecipient(),dependentOfMilitaryRecipient);
        }
        //Populate IN2.12 "Military Organization" component
        in2.getMilitaryOrganization().setValue(
                in2Values.contains("militaryOrganization") ? in2Values.string("militaryOrganization") : ""
        );
        //Populate IN2.13 "Military Station" component
        in2.getMilitaryStation().setValue(
                in2Values.contains("militaryStation") ? in2Values.string("militaryStation") : ""
        );
        //Populate IN2.14 "Military Service" component
        if (in2Values.contains("militaryService")){
            Json militaryService = jsonOrValuePropertyParse(parentProp+"militaryService", in2Values.string("militaryService"));
            populateCweField(in2.getMilitaryService(),militaryService);
        }
        //Populate IN2.15 "Military Rank/Grade" component
        if (in2Values.contains("militaryRankGrade")){
            Json militaryRankGrade = jsonOrValuePropertyParse(parentProp+"militaryRankGrade", in2Values.string("militaryRankGrade"));
            populateCweField(in2.getMilitaryRankGrade(),militaryRankGrade);
        }
        //Populate IN2.16 "Military Status" component
        if (in2Values.contains("militaryStatus")){
            Json militaryStatus = jsonOrValuePropertyParse(parentProp+"militaryStatus", in2Values.string("militaryStatus"));
            populateCweField(in2.getMilitaryStatus(),militaryStatus);
        }
        //Populate IN2.17 "Military Retire Date" component
        in2.getMilitaryRetireDate().setValue(
                in2Values.contains("militaryRetireDate") ? in2Values.string("militaryRetireDate") : ""
        );
        //Populate IN2.18 "Military Non-avail Cert On File" component
        in2.getMilitaryNonAvailCertOnFile().setValue(
                in2Values.contains("militaryNonAvailCertOnFile") ? in2Values.string("militaryNonAvailCertOnFile") : ""
        );
        //Populate IN2.19 "Baby Coverage" component
        in2.getBabyCoverage().setValue(
                in2Values.contains("babyCoverage") ? in2Values.string("babyCoverage") : ""
        );
        //Populate IN2.20 "Combine Baby Bill" component
        in2.getCombineBabyBill().setValue(
                in2Values.contains("combineBabyBill") ? in2Values.string("combineBabyBill") : ""
        );
        //Populate IN2.21 "Blood Deductible" component
        in2.getBloodDeductible().setValue(
                in2Values.contains("bloodDeductible") ? in2Values.string("bloodDeductible") : ""
        );
        //Populate IN2.22 "Special Coverage Approval Name" component, which is repeatable
        if (in2Values.contains("specialCoverageApprovalName")){
            List<Json> specialCoverageApprovalNameList = arrayPropertyToJson(parentProp+"specialCoverageApprovalName", in2Values.string("specialCoverageApprovalName"));
            for (int i = 0; i < specialCoverageApprovalNameList.size(); i++) {
                Json specialCoverageApprovalName = specialCoverageApprovalNameList.get(i);
                populateXpnField(in2.getSpecialCoverageApprovalName(i),specialCoverageApprovalName);
            }
        }
        //Populate IN2.23 "Special Coverage Approval Title" component
        in2.getSpecialCoverageApprovalTitle().setValue(
                in2Values.contains("specialCoverageApprovalTitle") ? in2Values.string("specialCoverageApprovalTitle") : ""
        );
        //Populate IN2.24 "Non-covered Insurance Code" component, which is repeatable
        if (in2Values.contains("nonCoveredInsuranceCode")){
            List<Json> nonCoveredInsuranceCodeList = arrayPropertyToJson(parentProp+"nonCoveredInsuranceCode", in2Values.string("nonCoveredInsuranceCode"));
            for (int i = 0; i < nonCoveredInsuranceCodeList.size(); i++) {
                Json nonCoveredInsuranceCode = nonCoveredInsuranceCodeList.get(i);
                populateCweField(in2.getNonCoveredInsuranceCode(i),nonCoveredInsuranceCode);
            }
        }
        //Populate IN2.25 "Payor Id" component, which is repeatable
        if (in2Values.contains("payorId")){
            List<Json> payorIdList = multipleJsonPropertyParse(parentProp+"payorId", in2Values.string("payorId"));
            for (int i = 0; i < payorIdList.size(); i++) {
                Json payorId = payorIdList.get(i);
                populateCxField(in2.getPayorID(i),payorId);
            }
        }
        //Populate IN2.26 "Payor Subscriber Id" component, which is repeatable
        if (in2Values.contains("payorSubscriberId")){
            List<Json> payorSubscriberIdList = multipleJsonPropertyParse(parentProp+"payorSubscriberId", in2Values.string("payorSubscriberId"));
            for (int i = 0; i < payorSubscriberIdList.size(); i++) {
                Json payorSubscriberId = payorSubscriberIdList.get(i);
                populateCxField(in2.getPayorSubscriberID(i),payorSubscriberId);
            }
        }
        //Populate IN2.27 "Eligibility Source" component
        if (in2Values.contains("eligibilitySource")){
            Json eligibilitySource = jsonOrValuePropertyParse(parentProp+"eligibilitySource", in2Values.string("eligibilitySource"));
            populateCweField(in2.getEligibilitySource(),eligibilitySource);
        }
        //Populate IN2.28 "Room Coverage Type/Amount" component, which is repeatable
        if (in2Values.contains("roomCoverageTypeAmount")){
            List<Json> roomCoverageTypeAmountList = arrayPropertyToJson(parentProp+"roomCoverageTypeAmount", in2Values.string("roomCoverageTypeAmount"));
            for (int i = 0; i < roomCoverageTypeAmountList.size(); i++) {
                Json roomCoverageTypeAmount = roomCoverageTypeAmountList.get(i);
                populateRmcField(in2.getRoomCoverageTypeAmount(i),roomCoverageTypeAmount);
            }
        }
        //Populate IN2.29 "Policy Type/Amount" component, which is repeatable
        if (in2Values.contains("policyTypeAmount")){
            List<Json> policyTypeAmountList = arrayPropertyToJson(parentProp+"policyTypeAmount", in2Values.string("policyTypeAmount"));
            for (int i = 0; i < policyTypeAmountList.size(); i++) {
                Json policyTypeAmount = policyTypeAmountList.get(i);
                populatePtaField(in2.getPolicyTypeAmount(i),policyTypeAmount);
            }
        }
        //Populate IN2.30 "Daily Deductible" component
        if (in2Values.contains("dailyDeductible")){
            Json dailyDeductible = jsonOrValuePropertyParse(parentProp+"dailyDeductible", in2Values.string("dailyDeductible"));
            populateDdiField(in2.getDailyDeductible(),dailyDeductible);
        }
        //Populate IN2.31 "Living Dependency" component
        if (in2Values.contains("livingDependency")){
            Json livingDependency = jsonOrValuePropertyParse(parentProp+"livingDependency", in2Values.string("livingDependency"));
            populateCweField(in2.getLivingDependency(),livingDependency);
        }
        //Populate IN2.32 "Ambulatory Status" component, which is repeatable
        if (in2Values.contains("ambulatoryStatus")){
            List<Json> ambulatoryStatusList = arrayPropertyToJson(parentProp+"ambulatoryStatus", in2Values.string("ambulatoryStatus"));
            for (int i = 0; i < ambulatoryStatusList.size(); i++) {
                Json ambulatoryStatus = ambulatoryStatusList.get(i);
                populateCweField(in2.getAmbulatoryStatus(i),ambulatoryStatus);
            }
        }
        //Populate IN2.33 "Citizenship" component, which is repeatable
        if (in2Values.contains("citizenship")){
            List<Json> citizenshipList = arrayPropertyToJson(parentProp+"citizenship", in2Values.string("citizenship"));
            for (int i = 0; i < citizenshipList.size(); i++) {
                Json citizenship = citizenshipList.get(i);
                populateCweField(in2.getCitizenship(i),citizenship);
            }
        }
        //Populate IN2.34 "Primary Language" component
        if (in2Values.contains("primaryLanguage")){
            Json primaryLanguage = jsonOrValuePropertyParse(parentProp+"primaryLanguage", in2Values.string("primaryLanguage"));
            populateCweField(in2.getPrimaryLanguage(),primaryLanguage);
        }
        //Populate IN2.35 "Living Arrangement" component
        if (in2Values.contains("livingArrangement")){
            Json livingArrangement = jsonOrValuePropertyParse(parentProp+"livingArrangement", in2Values.string("livingArrangement"));
            populateCweField(in2.getLivingArrangement(),livingArrangement);
        }
        //Populate IN2.36 "Publicity Code" component
        if (in2Values.contains("publicityCode")){
            Json publicityCode = jsonOrValuePropertyParse(parentProp+"publicityCode", in2Values.string("publicityCode"));
            populateCweField(in2.getPublicityCode(),publicityCode);
        }
        //Populate IN2.37 "Protection Indicator" component
        in2.getProtectionIndicator().setValue(
                in2Values.contains("protectionIndicator") ? in2Values.string("protectionIndicator") : ""
        );
        //Populate IN2.38 "Student Indicator" component
        if (in2Values.contains("studentIndicator")){
            Json studentIndicator = jsonOrValuePropertyParse(parentProp+"studentIndicator", in2Values.string("studentIndicator"));
            populateCweField(in2.getStudentIndicator(),studentIndicator);
        }
        //Populate IN2.39 "Religion" component
        if (in2Values.contains("religion")){
            Json religion = jsonOrValuePropertyParse(parentProp+"religion", in2Values.string("religion"));
            populateCweField(in2.getReligion(),religion);
        }
        //Populate IN2.40 "Mother's Maiden Name" component, which is repeatable
        if (in2Values.contains("mothersMaidenName")){
            List<Json> mothersMaidenNameList = arrayPropertyToJson(parentProp+"mothersMaidenName", in2Values.string("mothersMaidenName"));
            for (int i = 0; i < mothersMaidenNameList.size(); i++) {
                Json mothersMaidenName = mothersMaidenNameList.get(i);
                populateXpnField(in2.getMotherSMaidenName(i),mothersMaidenName);
            }
        }
        //Populate IN2.41 "Nationality" component
        if (in2Values.contains("nationality")){
            Json nationality = jsonOrValuePropertyParse(parentProp+"nationality", in2Values.string("nationality"));
            populateCweField(in2.getNationality(),nationality);
        }
        //Populate IN2.42 "Ethnic Group" component, which is repeatable
        if (in2Values.contains("ethnicGroup")){
            List<Json> ethnicGroupList = arrayPropertyToJson(parentProp+"ethnicGroup", in2Values.string("ethnicGroup"));
            for (int i = 0; i < ethnicGroupList.size(); i++) {
                Json ethnicGroup = ethnicGroupList.get(i);
                populateCweField(in2.getEthnicGroup(i),ethnicGroup);
            }
        }
        //Populate IN2.43 "Marital Status" component, which is repeatable
        if (in2Values.contains("maritalStatus")){
            List<Json> maritalStatusList = arrayPropertyToJson(parentProp+"maritalStatus", in2Values.string("maritalStatus"));
            for (int i = 0; i < maritalStatusList.size(); i++) {
                Json maritalStatus = maritalStatusList.get(i);
                populateCweField(in2.getMaritalStatus(i),maritalStatus);
            }
        }
        //Populate IN2.44 "Insured's Employment Start Date" component
        in2.getInsuredSEmploymentStartDate().setValue(
                in2Values.contains("insuredsEmploymentStartDate") ? in2Values.string("insuredsEmploymentStartDate") : ""
        );
        //Populate IN2.45 "Employment Stop Date" component
        in2.getEmploymentStopDate().setValue(
                in2Values.contains("employmentStopDate") ? in2Values.string("employmentStopDate") : ""
        );
        //Populate IN2.46 "Job Title" component
        in2.getJobTitle().setValue(
                in2Values.contains("jobTitle") ? in2Values.string("jobTitle") : ""
        );
        //Populate IN2.47 "Job Code/Class" component
        if (in2Values.contains("jobCodeClass")){
            Json jobCodeClass = jsonOrValuePropertyParse(parentProp+"jobCodeClass", in2Values.string("jobCodeClass"));
            populateJccField(in2.getJobCodeClass(),jobCodeClass);
        }
        //Populate IN2.48 "Job Status" component
        if (in2Values.contains("jobStatus")){
            Json jobStatus = jsonOrValuePropertyParse(parentProp+"jobStatus", in2Values.string("jobStatus"));
            populateCweField(in2.getJobStatus(),jobStatus);
        }
        //Populate IN2.49 "Employer Contact Person Name" component, which is repeatable
        if (in2Values.contains("employerContactPersonName")){
            List<Json> employerContactPersonNameList = arrayPropertyToJson(parentProp+"employerContactPersonName", in2Values.string("employerContactPersonName"));
            for (int i = 0; i < employerContactPersonNameList.size(); i++) {
                Json employerContactPersonName = employerContactPersonNameList.get(i);
                populateXpnField(in2.getEmployerContactPersonName(i),employerContactPersonName);
            }
        }
        //Populate IN2.50 "Employer Contact Person Phone Number" component, which is repeatable
        if (in2Values.contains("employerContactPersonPhoneNumber")){
            List<Json> employerContactPersonPhoneNumberList = multipleJsonPropertyParse(parentProp+"employerContactPersonPhoneNumber", in2Values.string("employerContactPersonPhoneNumber"));
            for (int i = 0; i < employerContactPersonPhoneNumberList.size(); i++) {
                Json employerContactPersonPhoneNumber = employerContactPersonPhoneNumberList.get(i);
                populateXtnField(in2.getEmployerContactPersonPhoneNumber(i),employerContactPersonPhoneNumber);
            }
        }
        //Populate IN2.51 "Employer Contact Reason" component
        if (in2Values.contains("employerContactReason")){
            Json employerContactReason = jsonOrValuePropertyParse(parentProp+"employerContactReason", in2Values.string("employerContactReason"));
            populateCweField(in2.getEmployerContactReason(),employerContactReason);
        }
        //Populate IN2.52 "Insured's Contact Person's Name" component, which is repeatable
        if (in2Values.contains("insuredsContactPersonsName")){
            List<Json> insuredsContactPersonsNameList = arrayPropertyToJson(parentProp+"insuredsContactPersonsName", in2Values.string("insuredsContactPersonsName"));
            for (int i = 0; i < insuredsContactPersonsNameList.size(); i++) {
                Json insuredsContactPersonsName = insuredsContactPersonsNameList.get(i);
                populateXpnField(in2.getInsuredSContactPersonSName(i),insuredsContactPersonsName);
            }
        }
        //Populate IN2.53 "Insured's Contact Person Phone Number" component, which is repeatable
        if (in2Values.contains("insuredsContactPersonPhoneNumber")){
            List<Json> insuredsContactPersonPhoneNumberList = multipleJsonPropertyParse(parentProp+"insuredsContactPersonPhoneNumber", in2Values.string("insuredsContactPersonPhoneNumber"));
            for (int i = 0; i < insuredsContactPersonPhoneNumberList.size(); i++) {
                Json insuredsContactPersonPhoneNumber = insuredsContactPersonPhoneNumberList.get(i);
                populateXtnField(in2.getInsuredSContactPersonPhoneNumber(i),insuredsContactPersonPhoneNumber);
            }
        }
        //Populate IN2.54 "Insured's Contact Person Reason" component, which is repeatable
        if (in2Values.contains("insuredsContactPersonReason")){
            List<Json> insuredsContactPersonReasonList = arrayPropertyToJson(parentProp+"insuredsContactPersonReason", in2Values.string("insuredsContactPersonReason"));
            for (int i = 0; i < insuredsContactPersonReasonList.size(); i++) {
                Json insuredsContactPersonReason = insuredsContactPersonReasonList  .get(i);
                populateCweField(in2.getInsuredSContactPersonReason(i),insuredsContactPersonReason);
            }
        }
        //Populate IN2.55 "Relationship To The Patient Start Date" component
        in2.getRelationshipToThePatientStartDate().setValue(
                in2Values.contains("relationshipToThePatientStartDate") ? in2Values.string("relationshipToThePatientStartDate") : ""
        );
        //Populate IN2.56 "Relationship To The Patient Stop Date" component, which is repeatable
        if (in2Values.contains("relationshipToThePatientStopDate")){
            List<Json> relationshipToThePatientStopDateList = arrayPropertyToJson(parentProp+"relationshipToThePatientStopDate", in2Values.string("relationshipToThePatientStopDate"));
            for (int i = 0; i < relationshipToThePatientStopDateList.size(); i++) {
                Json relationshipToThePatientStopDate = relationshipToThePatientStopDateList.get(i);
                in2.getRelationshipToThePatientStopDate(i).setValue(
                        relationshipToThePatientStopDate.string("mainValue")
                );
            }
        }
        //Populate IN2.57 "Insurance Co Contact Reason" component
        if (in2Values.contains("insuranceCoContactReason")){
            Json insuranceCoContactReason = jsonOrValuePropertyParse(parentProp+"insuranceCoContactReason", in2Values.string("insuranceCoContactReason"));
            populateCweField(in2.getInsuranceCoContactReason(),insuranceCoContactReason);
        }
        //Populate IN2.58 "Insurance Co Contact Phone Number" component
        if (in2Values.contains("insuranceCoContactPhoneNumber")){
            List<Json> insuranceCoContactPhoneNumberList = multipleJsonPropertyParse(parentProp+"insuranceCoContactPhoneNumber", in2Values.string("insuranceCoContactPhoneNumber"));
            for (int i = 0; i < insuranceCoContactPhoneNumberList.size(); i++) {
                Json insuranceCoContactPhoneNumber = insuranceCoContactPhoneNumberList  .get(i);
                populateXtnField(in2.getInsuranceCoContactPhoneNumber(i),insuranceCoContactPhoneNumber);
            }
        }
        //Populate IN2.59 "Policy Scope" component
        if (in2Values.contains("policyScope")){
            Json policyScope = jsonOrValuePropertyParse(parentProp+"policyScope", in2Values.string("policyScope"));
            populateCweField(in2.getPolicyScope(),policyScope);
        }
        //Populate IN2.60 "Policy Source" component
        if (in2Values.contains("policySource")){
            Json policySource = jsonOrValuePropertyParse(parentProp+"policySource", in2Values.string("policySource"));
            populateCweField(in2.getPolicySource(),policySource);
        }
        //Populate IN2.61 "Patient Member Number" component
        if (in2Values.contains("patientMemberNumber")){
            Json patientMemberNumber = singleJsonPropertyParse(parentProp+"patientMemberNumber", in2Values.string("patientMemberNumber"));
            populateCxField(in2.getPatientMemberNumber(),patientMemberNumber);
        }
        //Populate IN2.62 "Guarantor's Relationship To Insured" component
        if (in2Values.contains("guarantorsRelationshipToInsured")){
            Json guarantorsRelationshipToInsured = jsonOrValuePropertyParse(parentProp+"guarantorsRelationshipToInsured", in2Values.string("guarantorsRelationshipToInsured"));
            populateCweField(in2.getGuarantorSRelationshipToInsured(),guarantorsRelationshipToInsured);
        }
        //Populate IN2.63 "Insured's Phone Number - Home" component
        if (in2Values.contains("insuredsPhoneNumberHome")){
            List<Json> insuredsPhoneNumberHomeList = multipleJsonPropertyParse(parentProp+"insuredsPhoneNumberHome", in2Values.string("insuredsPhoneNumberHome"));
            for (int i = 0; i < insuredsPhoneNumberHomeList.size(); i++) {
                Json insuredsPhoneNumberHome = insuredsPhoneNumberHomeList  .get(i);
                populateXtnField(in2.getInsuredSPhoneNumberHome(i),insuredsPhoneNumberHome);
            }
        }
        //Populate IN2.64 "Insured's Employer Phone Number" component
        if (in2Values.contains("insuredsEmployerPhoneNumber")){
            List<Json> insuredsEmployerPhoneNumberList = multipleJsonPropertyParse(parentProp+"insuredsEmployerPhoneNumber", in2Values.string("insuredsEmployerPhoneNumber"));
            for (int i = 0; i < insuredsEmployerPhoneNumberList.size(); i++) {
                Json insuredsEmployerPhoneNumber = insuredsEmployerPhoneNumberList  .get(i);
                populateXtnField(in2.getInsuredSEmployerPhoneNumber(i),insuredsEmployerPhoneNumber);
            }
        }
        //Populate IN2.65 "Military Handicapped Program" component
        if (in2Values.contains("militaryHandicappedProgram")){
            Json militaryHandicappedProgram = jsonOrValuePropertyParse(parentProp+"militaryHandicappedProgram", in2Values.string("militaryHandicappedProgram"));
            populateCweField(in2.getMilitaryHandicappedProgram(),militaryHandicappedProgram);
        }
        //Populate IN2.66 "Suspend Flag" component
        in2.getSuspendFlag().setValue(
                in2Values.contains("suspendFlag") ? in2Values.string("suspendFlag") : ""
        );
        //Populate IN2.67 "Copay Limit Flag" component
        in2.getCopayLimitFlag().setValue(
                in2Values.contains("copayLimitFlag") ? in2Values.string("copayLimitFlag") : ""
        );
        //Populate IN2.68 "Stoploss Limit Flag" component
        in2.getStoplossLimitFlag().setValue(
                in2Values.contains("stoplossLimitFlag") ? in2Values.string("stoplossLimitFlag") : ""
        );
        //Populate IN2.69 "Insured Organization Name And Id" component
        if (in2Values.contains("insuredOrganizationNameAndId")){
            List<Json> insuredOrganizationNameAndIdList = arrayPropertyToJson(parentProp+"insuredOrganizationNameAndId", in2Values.string("insuredOrganizationNameAndId"));
            for (int i = 0; i < insuredOrganizationNameAndIdList.size(); i++) {
                Json insuredOrganizationNameAndId = insuredOrganizationNameAndIdList  .get(i);
                populateXonField(in2.getInsuredOrganizationNameAndID(i),insuredOrganizationNameAndId);
            }
        }
        //Populate IN2.70 "Insured Employer Organization Name And Id" component
        if (in2Values.contains("insuredEmployerOrganizationNameAndId")){
            List<Json> insuredEmployerOrganizationNameAndIdList = arrayPropertyToJson(parentProp+"insuredEmployerOrganizationNameAndId", in2Values.string("insuredEmployerOrganizationNameAndId"));
            for (int i = 0; i < insuredEmployerOrganizationNameAndIdList.size(); i++) {
                Json insuredEmployerOrganizationNameAndId = insuredEmployerOrganizationNameAndIdList  .get(i);
                populateXonField(in2.getInsuredEmployerOrganizationNameAndID(i),insuredEmployerOrganizationNameAndId);
            }
        }
        //Populate IN2.71 "Race" component
        if (in2Values.contains("race")){
            List<Json> raceList = arrayPropertyToJson("race", in2Values.string("race"));
            for (int i = 0; i < raceList.size(); i++) {
                Json race = raceList  .get(i);
                populateCweField(in2.getRace(i),race);
            }
        }
        //Populate IN2.72 "Patient's Relationship To Insured" component
        if (in2Values.contains("patientsRelationshipToInsured")){
            Json patientsRelationshipToInsured = jsonOrValuePropertyParse(parentProp+"patientsRelationshipToInsured", in2Values.string("patientsRelationshipToInsured"));
            populateCweField(in2.getPatientSRelationshipToInsured(),patientsRelationshipToInsured);
        }
    }

    public static void populateIn3Segment(IN3 in3, Json in3Values) throws DataTypeException {
        String parentProp = "insuranceAdditionalInformationCertification.";

        //Populate IN3.1 - Set Id - In3
        in3.getSetIDIN3().setValue(
                in3Values.contains("setId") ? in3Values.string("setId") : ""
        );
        //Populate IN3.2 - Certification Number
        if (in3Values.contains("certificationNumber")) {
            Json certificationNumber = singleJsonPropertyParse(parentProp+"certificationNumber",in3Values.string("certificationNumber"));
            populateCxField(in3.getCertificationNumber(),certificationNumber);
        }
        //Populate IN3.3 - Certified By
        if (in3Values.contains("certifiedBy")){
            List<Json> certifiedByList = arrayPropertyToJson(parentProp+"certifiedBy", in3Values.string("certifiedBy"));
            for (int i = 0; i < certifiedByList.size(); i++) {
                Json certifiedBy = certifiedByList  .get(i);
                populateXcnField(in3.getCertifiedBy(i),certifiedBy);
            }
        }
        //Populate IN3.4 - Certification Required
        in3.getCertificationRequired().setValue(
                in3Values.contains("certificationRequired") ? in3Values.string("certificationRequired") : ""
        );
        //Populate IN3.5 - Penalty
        if (in3Values.contains("penalty")) {
            Json penalty = singleJsonPropertyParse(parentProp+"penalty",in3Values.string("penalty"));
            populateMopField(in3.getPenalty(),penalty);
        }
        //Populate IN3.6 - Certification Date/Time
        in3.getCertificationDateTime().setValue(
                in3Values.contains("certificationDateTime") ? in3Values.string("certificationDateTime") : ""
        );
        //Populate IN3.7 - Certification Modify Date/Time
        in3.getCertificationModifyDateTime().setValue(
                in3Values.contains("certificationModifyDateTime") ? in3Values.string("certificationModifyDateTime") : ""
        );
        //Populate IN3.8 - Operator
        if (in3Values.contains("operator")){
            List<Json> operatorList = arrayPropertyToJson(parentProp+"operator", in3Values.string("operator"));
            for (int i = 0; i < operatorList.size(); i++) {
                Json operator = operatorList  .get(i);
                populateXcnField(in3.getOperator(i),operator);
            }
        }
        //Populate IN3.9 - Certification Begin Date
        in3.getCertificationBeginDate().setValue(
                in3Values.contains("certificationBeginDate") ? in3Values.string("certificationBeginDate") : ""
        );
        //Populate IN3.10 - Certification End Date
        in3.getCertificationEndDate().setValue(
                in3Values.contains("certificationEndDate") ? in3Values.string("certificationEndDate") : ""
        );
        //Populate IN3.11 - Days
        if (in3Values.contains("days")) {
            Json days = singleJsonPropertyParse(parentProp+"days",in3Values.string("days"));
            populateDtnField(in3.getDays(),days);
        }
        //Populate IN3.12 - Non-concur Code/Description
        if (in3Values.contains("nonConcurCodeDescription")) {
            Json nonConcurCodeDescription = jsonOrValuePropertyParse(parentProp+"nonConcurCodeDescription",in3Values.string("nonConcurCodeDescription"));
            populateCweField(in3.getNonConcurCodeDescription(),nonConcurCodeDescription);
        }
        //Populate IN3.13 - Non-concur Effective Date/Time
        in3.getNonConcurEffectiveDateTime().setValue(
                in3Values.contains("nonConcurEffectiveDateTime") ? in3Values.string("nonConcurEffectiveDateTime") : ""
        );
        //Populate IN3.14 - Physician Reviewer
        if (in3Values.contains("physicianReviewer")){
            List<Json> physicianReviewerList = arrayPropertyToJson(parentProp+"physicianReviewer", in3Values.string("physicianReviewer"));
            for (int i = 0; i < physicianReviewerList.size(); i++) {
                Json physicianReviewer = physicianReviewerList  .get(i);
                populateXcnField(in3.getPhysicianReviewer(i),physicianReviewer);
            }
        }
        //Populate IN3.15 - Certification Contact
        in3.getCertificationContact().setValue(
                in3Values.contains("certificationContact") ? in3Values.string("certificationContact") : ""
        );
        //Populate IN3.16 - Certification Contact Phone Number
        if (in3Values.contains("certificationContactPhoneNumber")) {
            List<Json> certificationContactPhoneNumberList = multipleJsonPropertyParse(parentProp+"certificationContactPhoneNumber", in3Values.string("certificationContactPhoneNumber"));
            for (int i = 0; i < certificationContactPhoneNumberList.size(); i++) {
                Json certificationContactPhoneNumber = certificationContactPhoneNumberList.get(i);
                populateXtnField(in3.getCertificationContactPhoneNumber(i),certificationContactPhoneNumber);
            }
        }
        //Populate IN3.17 - Appeal Reason
        if (in3Values.contains("appealReason")) {
            Json appealReason = jsonOrValuePropertyParse(parentProp+"nonConcurCodeDescription",in3Values.string("nonConcurCodeDescription"));
            populateCweField(in3.getNonConcurCodeDescription(),appealReason);
        }
        //Populate IN3.18 - Certification Agency
        if (in3Values.contains("certificationAgency")) {
            Json certificationAgency = jsonOrValuePropertyParse(parentProp+"certificationAgency",in3Values.string("certificationAgency"));
            populateCweField(in3.getCertificationAgency(),certificationAgency);
        }
        //Populate IN3.19 - Certification Agency Phone Number
        if (in3Values.contains("certificationAgencyPhoneNumber")) {
            List<Json> certificationAgencyPhoneNumberList = multipleJsonPropertyParse(parentProp+"certificationAgencyPhoneNumber", in3Values.string("certificationAgencyPhoneNumber"));
            for (int i = 0; i < certificationAgencyPhoneNumberList.size(); i++) {
                Json certificationAgencyPhoneNumber = certificationAgencyPhoneNumberList.get(i);
                populateXtnField(in3.getCertificationAgencyPhoneNumber(i),certificationAgencyPhoneNumber);
            }
        }
        //Populate IN3.20 - Pre-certification Requirement
        if (in3Values.contains("preCertificationRequirement")) {
            List<Json> preCertificationRequirementList = multipleJsonPropertyParse(parentProp+"preCertificationRequirement", in3Values.string("preCertificationRequirement"));
            for (int i = 0; i < preCertificationRequirementList.size(); i++) {
                Json preCertificationRequirement = preCertificationRequirementList.get(i);
                populateIcdField(in3.getPreCertificationRequirement(i),preCertificationRequirement);
            }
        }
        //Populate IN3.21 - Case Manager
        in3.getCaseManager().setValue(
                in3Values.contains("caseManager") ? in3Values.string("caseManager") : ""
        );
        //Populate IN3.22 - Second Opinion Date
        in3.getSecondOpinionDate().setValue(
                in3Values.contains("secondOpinionDate") ? in3Values.string("secondOpinionDate") : ""
        );
        //Populate IN3.23 - Second Opinion Status
        if (in3Values.contains("secondOpinionStatus")) {
            Json secondOpinionStatus = jsonOrValuePropertyParse(parentProp+"secondOpinionStatus",in3Values.string("secondOpinionStatus"));
            populateCweField(in3.getSecondOpinionStatus(),secondOpinionStatus);
        }
        //Populate IN3.24 - Second Opinion Documentation Received
        if (in3Values.contains("secondOpinionDocumentationReceived")) {
            List<Json> secondOpinionDocumentationReceivedList = arrayPropertyToJson(parentProp+"secondOpinionDocumentationReceived", in3Values.string("secondOpinionDocumentationReceived"));
            for (int i = 0; i < secondOpinionDocumentationReceivedList.size(); i++) {
                Json secondOpinionDocumentationReceived = secondOpinionDocumentationReceivedList.get(i);
                populateCweField(in3.getSecondOpinionDocumentationReceived(i),secondOpinionDocumentationReceived);
            }
        }
        //Populate IN3.25 - Second Opinion Physician
        if (in3Values.contains("secondOpinionPhysician")){
            List<Json> secondOpinionPhysicianList = arrayPropertyToJson(parentProp+"secondOpinionPhysician", in3Values.string("secondOpinionPhysician"));
            for (int i = 0; i < secondOpinionPhysicianList.size(); i++) {
                Json secondOpinionPhysician = secondOpinionPhysicianList  .get(i);
                populateXcnField(in3.getSecondOpinionPhysician(i),secondOpinionPhysician);
            }
        }
        //Populate IN3.26 - Certification Type
        if (in3Values.contains("certificationType")) {
            Json certificationType = jsonOrValuePropertyParse(parentProp+"certificationType",in3Values.string("certificationType"));
            populateCweField(in3.getCertificationType(),certificationType);
        }
        //Populate IN3.27 - Certification Category
        if (in3Values.contains("certificationCategory")) {
            Json certificationCategory = jsonOrValuePropertyParse(parentProp+"certificationCategory",in3Values.string("certificationCategory"));
            populateCweField(in3.getCertificationCategory(),certificationCategory);
        }
    }

    public static void populatePr1Segment(PR1 pr1, Json pr1Values) throws DataTypeException {
        String parentProp = "procedures.";
        //Populate PR1.1 "Set Id - Pr1"
        pr1.getSetIDPR1().setValue(
                pr1Values.contains("setId") ? pr1Values.string("setId") : ""
        );
        //Populate PR1.2 "Procedure Coding Method" withdrawn
        //Populate PR1.3 "Procedure Code"
        if (pr1Values.contains("procedureCode")){
            Json procedureCode = jsonOrValuePropertyParse(parentProp+"procedureCode",pr1Values.string("procedureCode"));
            populateCneField(pr1.getProcedureCode(),procedureCode);
        }
        //Populate PR1.4 "Procedure Description" withdrawn
        //Populate PR1.5 "Procedure Date/Time"
        pr1.getProcedureDateTime().setValue(
                pr1Values.contains("procedureDateTime") ? pr1Values.string("procedureDateTime") : ""
        );
        //Populate PR1.6 "Procedure Functional Type"
        if (pr1Values.contains("procedureFunctionalType")){
            Json procedureFunctionalType = jsonOrValuePropertyParse(parentProp+"procedureFunctionalType",pr1Values.string("procedureFunctionalType"));
            populateCweField(pr1.getProcedureFunctionalType(),procedureFunctionalType);
        }
        //Populate PR1.7 "Procedure Minutes"
        pr1.getProcedureMinutes().setValue(
                pr1Values.contains("procedureMinutes") ? pr1Values.string("procedureMinutes") : ""
        );
        //Populate PR1.8 "Anesthesiologist" withdrawn
        //Populate PR1.9 "Anesthesia Code Type"
        if (pr1Values.contains("anesthesiaCode")){
            Json anesthesiaCode = jsonOrValuePropertyParse(parentProp+"anesthesiaCode",pr1Values.string("anesthesiaCode"));
            populateCweField(pr1.getAnesthesiaCode(),anesthesiaCode);
        }
        //Populate PR1.10 "Anesthesia Minutes"
        pr1.getAnesthesiaMinutes().setValue(
                pr1Values.contains("anesthesiaMinutes") ? pr1Values.string("anesthesiaMinutes") : ""
        );
        //Populate PR1.11 "Surgeon" withdrawn
        //Populate PR1.12 "Procedure Practitioner" withdrawn
        //Populate PR1.13 "Consent Code"
        if (pr1Values.contains("consentCode")){
            Json consentCode = jsonOrValuePropertyParse(parentProp+"consentCode",pr1Values.string("consentCode"));
            populateCweField(pr1.getConsentCode(),consentCode);
        }
        //Populate PR1.14 "Procedure Priority"
        pr1.getProcedurePriority().setValue(
                pr1Values.contains("procedurePriority") ? pr1Values.string("procedurePriority") : ""
        );
        //Populate PR1.15 "Associated Diagnosis Code"
        if (pr1Values.contains("associatedDiagnosisCode")){
            Json associatedDiagnosisCode = jsonOrValuePropertyParse(parentProp+"associatedDiagnosisCode",pr1Values.string("associatedDiagnosisCode"));
            populateCweField(pr1.getConsentCode(),associatedDiagnosisCode);
        }
        //Populate PR1.16 "Procedure Code Modifier" component
        if (pr1Values.contains("procedureCodeModifier")){
            List<Json> procedureCodeModifierList = arrayPropertyToJson("procedureCodeModifier", pr1Values.string("procedureCodeModifier"));
            for (int i = 0; i < procedureCodeModifierList.size(); i++) {
                Json procedureCodeModifier = procedureCodeModifierList.get(i);
                populateCneField(pr1.getProcedureCodeModifier(i),procedureCodeModifier);
            }
        }
        //Populate PR1.17 "Procedure Drg Type"
        if (pr1Values.contains("procedureDrgType")){
            Json procedureDrgType = jsonOrValuePropertyParse(parentProp+"procedureDrgType",pr1Values.string("procedureDrgType"));
            populateCweField(pr1.getProcedureDRGType(),procedureDrgType);
        }
        //Populate PR1.18 "Tissue Type Code" component
        if (pr1Values.contains("tissueTypeCode")){
            List<Json> tissueTypeCodeList = arrayPropertyToJson("tissueTypeCode", pr1Values.string("tissueTypeCode"));
            for (int i = 0; i < tissueTypeCodeList.size(); i++) {
                Json tissueTypeCode = tissueTypeCodeList.get(i);
                populateCweField(pr1.getTissueTypeCode(i),tissueTypeCode);
            }
        }
        //Populate PR1.19 "Procedure Identifier"
        if (pr1Values.contains("procedureIdentifier")){
            Json procedureIdentifier = jsonOrValuePropertyParse(parentProp+"procedureDrgType",pr1Values.string("procedureDrgType"));
            populateEiField(pr1.getProcedureIdentifier(),procedureIdentifier);
        }
        //Populate PR1.20 "Procedure Action Code"
        pr1.getProcedureActionCode().setValue(
                pr1Values.contains("procedureActionCode") ? pr1Values.string("procedureActionCode") : ""
        );
        //Populate PR1.21 "Drg Procedure Determination Status"
        if (pr1Values.contains("drgProcedureDeterminationStatus")){
            Json drgProcedureDeterminationStatus = jsonOrValuePropertyParse(parentProp+"drgProcedureDeterminationStatus",pr1Values.string("drgProcedureDeterminationStatus"));
            populateCweField(pr1.getDRGProcedureDeterminationStatus(),drgProcedureDeterminationStatus);
        }
        //Populate PR1.22 "Drg Procedure Relevance"
        if (pr1Values.contains("drgProcedureRelevance")){
            Json drgProcedureRelevance = jsonOrValuePropertyParse(parentProp+"drgProcedureRelevance",pr1Values.string("drgProcedureRelevance"));
            populateCweField(pr1.getDRGProcedureRelevance(),drgProcedureRelevance);
        }
        //Populate PR1.23 "Treating Organizational Unit"
        if (pr1Values.contains("treatingOrganizationalUnit")){
            List<Json> treatingOrganizationalUnitList = arrayPropertyToJson(parentProp+"drgProcedureRelevance",pr1Values.string("drgProcedureRelevance"));
            for (int i = 0; i < treatingOrganizationalUnitList.size(); i++) {
                Json treatingOrganizationalUnit = treatingOrganizationalUnitList.get(i);
                populatePlField(pr1.getTreatingOrganizationalUnit(i),treatingOrganizationalUnit);
            }
        }
        //Populate PR1.24 "Respiratory Within Surgery" component
        pr1.getRespiratoryWithinSurgery().setValue(
                pr1Values.contains("respiratoryWithinSurgery") ? pr1Values.string("respiratoryWithinSurgery") : ""
        );
        //Populate PR1.25 "Parent Procedure Id"
        if (pr1Values.contains("parentProcedureId")){
            Json parentProcedureId = jsonOrValuePropertyParse(parentProp+"parentProcedureId",pr1Values.string("parentProcedureId"));
            populateEiField(pr1.getParentProcedureID(),parentProcedureId);
        }
    }

    public static void  populateGt1Segment(GT1 gt1, Json gt1Values) throws DataTypeException {
        String parentProp = "guarantor.";
        //Populate GT1.1 - Set Id - Gt1
        gt1.getSetIDGT1().setValue(
                gt1Values.contains("setId") ? gt1Values.string("setId") : ""
        );
        //Populate GT1.2 - Guarantor Number
        if (gt1Values.contains("guarantorNumber")){
            List<Json> guarantorNumberList = multipleJsonPropertyParse(parentProp+"guarantorNumber",gt1Values.string("guarantorNumber"));
            for (int i = 0; i < guarantorNumberList.size(); i++) {
                Json guarantorNumber = guarantorNumberList.get(i);
                populateCxField(gt1.getGuarantorNumber(i),guarantorNumber);
            }
        }
        //Populate GT1.3 - Guarantor Name
        if (gt1Values.contains("guarantorName")){
            List<Json> guarantorNameList = multipleJsonPropertyParse(parentProp+"guarantorName",gt1Values.string("guarantorName"));
            for (int i = 0; i < guarantorNameList.size(); i++) {
                Json guarantorName = guarantorNameList.get(i);
                populateXpnField(gt1.getGuarantorName(i),guarantorName);
            }
        }
        //Populate GT1.4 - Guarantor Spouse Name
        if (gt1Values.contains("guarantorSpouseName")){
            List<Json> guarantorSpouseNameList = multipleJsonPropertyParse(parentProp+"guarantorSpouseName",gt1Values.string("guarantorSpouseName"));
            for (int i = 0; i < guarantorSpouseNameList.size(); i++) {
                Json guarantorSpouseName = guarantorSpouseNameList.get(i);
                populateXpnField(gt1.getGuarantorSpouseName(i),guarantorSpouseName);
            }
        }
        //Populate GT1.5 - Guarantor Address
        if (gt1Values.contains("guarantorAddress")){
            List<Json> guarantorAddressList = multipleJsonPropertyParse(parentProp+"guarantorAddress",gt1Values.string("guarantorSpouseName"));
            for (int i = 0; i < guarantorAddressList.size(); i++) {
                Json guarantorAddress = guarantorAddressList.get(i);
                populateXadField(gt1.getGuarantorAddress(i),guarantorAddress);
            }
        }
        //Populate GT1.6 - Guarantor Ph Num - Home
        if (gt1Values.contains("guarantorPhNumHome")){
            List<Json> guarantorPhNumHomeList = multipleJsonPropertyParse(parentProp+"guarantorPhNumHome",gt1Values.string("guarantorPhNumHome"));
            for (int i = 0; i < guarantorPhNumHomeList.size(); i++) {
                Json guarantorPhNumHome = guarantorPhNumHomeList.get(i);
                populateXtnField(gt1.getGuarantorPhNumHome(i),guarantorPhNumHome);
            }
        }
        //Populate GT1.7 - Guarantor Ph Num - Business
        if (gt1Values.contains("guarantorPhNumBusiness")){
            List<Json> guarantorPhNumBusinessList = multipleJsonPropertyParse(parentProp+"guarantorPhNumBusiness",gt1Values.string("guarantorPhNumBusiness"));
            for (int i = 0; i < guarantorPhNumBusinessList.size(); i++) {
                Json guarantorPhNumBusiness = guarantorPhNumBusinessList.get(i);
                populateXtnField(gt1.getGuarantorPhNumBusiness(i),guarantorPhNumBusiness);
            }
        }
        //Populate GT1.8 - Guarantor Date/Time Of Birth
        gt1.getGuarantorDateTimeOfBirth().setValue(
                gt1Values.contains("guarantorDateTimeOfBirth") ? gt1Values.string("guarantorDateTimeOfBirth") : ""
        );
        //Populate GT1.9 - Guarantor Administrative Sex
        if (gt1Values.contains("guarantorAdministrativeSex")){
            Json guarantorAdministrativeSex = jsonOrValuePropertyParse(parentProp+"guarantorAdministrativeSex",gt1Values.string("guarantorAdministrativeSex"));
            populateCweField(gt1.getGuarantorAdministrativeSex(),guarantorAdministrativeSex);
        }
        //Populate GT1.10 - Guarantor Type
        if (gt1Values.contains("guarantorType")){
            Json guarantorType = jsonOrValuePropertyParse(parentProp+"guarantorType",gt1Values.string("guarantorType"));
            populateCweField(gt1.getGuarantorType(),guarantorType);
        }
        //Populate GT1.11 - Guarantor Relationship
        if (gt1Values.contains("guarantorRelationship")){
            Json guarantorRelationship = jsonOrValuePropertyParse(parentProp+"guarantorRelationship",gt1Values.string("guarantorRelationship"));
            populateCweField(gt1.getGuarantorRelationship(),guarantorRelationship);
        }
        //Populate GT1.12 - Guarantor Ssn
        gt1.getGuarantorSSN().setValue(
                gt1Values.contains("guarantorSsn") ? gt1Values.string("guarantorSsn") : ""
        );
        //Populate GT1.13 - Guarantor Date - Begin
        gt1.getGuarantorDateBegin().setValue(
                gt1Values.contains("guarantorDateBegin") ? gt1Values.string("guarantorDateBegin") : ""
        );
        //Populate GT1.14 - Guarantor Date - End
        gt1.getGuarantorDateEnd().setValue(
                gt1Values.contains("guarantorDateEnd") ? gt1Values.string("guarantorDateEnd") : ""
        );
        //Populate GT1.15 - Guarantor Priority
        gt1.getGuarantorPriority().setValue(
                gt1Values.contains("guarantorPriority") ? gt1Values.string("guarantorPriority") : ""
        );
        //Populate GT1.16 - Guarantor Employer Name
        if (gt1Values.contains("guarantorEmployerName")){
            List<Json> guarantorEmployerNameList = arrayPropertyToJson(parentProp+"guarantorEmployerName",gt1Values.string("guarantorEmployerName"));
            for (int i = 0; i < guarantorEmployerNameList.size(); i++) {
                Json guarantorEmployerName = guarantorEmployerNameList.get(i);
                populateXpnField(gt1.getGuarantorEmployerName(i),guarantorEmployerName);
            }
        }
        //Populate GT1.17 - Guarantor Employer Address
        if (gt1Values.contains("guarantorEmployerAddress")){
            List<Json> guarantorEmployerAddressList = arrayPropertyToJson(parentProp+"guarantorEmployerName",gt1Values.string("guarantorEmployerName"));
            for (int i = 0; i < guarantorEmployerAddressList.size(); i++) {
                Json guarantorEmployerAddress = guarantorEmployerAddressList.get(i);
                populateXadField(gt1.getGuarantorEmployerAddress(i),guarantorEmployerAddress);
            }
        }
        //Populate GT1.18 - Guarantor Employer Phone Number
        if (gt1Values.contains("guarantorEmployerPhoneNumber")){
            List<Json> guarantorEmployerPhoneNumberList = multipleJsonPropertyParse(parentProp+"guarantorEmployerPhoneNumber",gt1Values.string("guarantorEmployerPhoneNumber"));
            for (int i = 0; i < guarantorEmployerPhoneNumberList.size(); i++) {
                Json guarantorEmployerPhoneNumber = guarantorEmployerPhoneNumberList.get(i);
                populateXtnField(gt1.getGuarantorEmployerPhoneNumber(i),guarantorEmployerPhoneNumber);
            }
        }
        //Populate GT1.19 - Guarantor Employee Id Number
        if (gt1Values.contains("guarantorEmployeeIdNumber")){
            List<Json> guarantorEmployeeIdNumberList = multipleJsonPropertyParse(parentProp+"guarantorEmployeeIdNumber",gt1Values.string("guarantorEmployeeIdNumber"));
            for (int i = 0; i < guarantorEmployeeIdNumberList.size(); i++) {
                Json guarantorEmployeeIdNumber = guarantorEmployeeIdNumberList.get(i);
                populateCxField(gt1.getGuarantorEmployeeIDNumber(i),guarantorEmployeeIdNumber);
            }
        }
        //Populate GT1.20 - Guarantor Employment Status
        if (gt1Values.contains("guarantorEmploymentStatus")){
            Json guarantorEmploymentStatus = jsonOrValuePropertyParse(parentProp+"guarantorRelationship",gt1Values.string("guarantorRelationship"));
            populateCweField(gt1.getGuarantorEmploymentStatus(),guarantorEmploymentStatus);
        }
        //Populate GT1.21 - Guarantor Organization Name
        if (gt1Values.contains("guarantorOrganizationName")){
            List<Json> guarantorOrganizationNameList = arrayPropertyToJson(parentProp+"guarantorOrganizationName",gt1Values.string("guarantorOrganizationName"));
            for (int i = 0; i < guarantorOrganizationNameList.size(); i++) {
                Json guarantorOrganizationName = guarantorOrganizationNameList.get(i);
                populateXonField(gt1.getGuarantorOrganizationName(i),guarantorOrganizationName);
            }
        }
        //Populate GT1.22 - Guarantor Billing Hold Flag
        gt1.getGuarantorBillingHoldFlag().setValue(
                gt1Values.contains("guarantorBillingHoldFlag") ? gt1Values.string("guarantorBillingHoldFlag") : ""
        );
        //Populate GT1.23 - Guarantor Credit Rating Code
        if (gt1Values.contains("guarantorCreditRatingCode")){
            Json guarantorCreditRatingCode = jsonOrValuePropertyParse(parentProp+"guarantorCreditRatingCode",gt1Values.string("guarantorCreditRatingCode"));
            populateCweField(gt1.getGuarantorCreditRatingCode(),guarantorCreditRatingCode);
        }
        //Populate GT1.24 - Guarantor Death Date And Time
        gt1.getGuarantorDeathDateAndTime().setValue(
                gt1Values.contains("guarantorDeathDateAndTime") ? gt1Values.string("guarantorDeathDateAndTime") : ""
        );
        //Populate GT1.24 - Guarantor Death Date And Time
        gt1.getGuarantorDeathDateAndTime().setValue(
                gt1Values.contains("guarantorDeathDateAndTime") ? gt1Values.string("guarantorDeathDateAndTime") : ""
        );
        //Populate GT1.25 - Guarantor Death Flag
        gt1.getGuarantorDeathFlag().setValue(
                gt1Values.contains("guarantorDeathFlag") ? gt1Values.string("guarantorDeathFlag") : ""
        );
        //Populate GT1.26 - Guarantor Charge Adjustment Code
        if (gt1Values.contains("guarantorChargeAdjustmentCode")){
            Json guarantorChargeAdjustmentCode = jsonOrValuePropertyParse(parentProp+"guarantorChargeAdjustmentCode",gt1Values.string("guarantorChargeAdjustmentCode"));
            populateCweField(gt1.getGuarantorEmploymentStatus(),guarantorChargeAdjustmentCode);
        }
        //Populate GT1.27 - Guarantor Household Annual Income
        if (gt1Values.contains("guarantorHouseholdAnnualIncome")){
            Json guarantorHouseholdAnnualIncome = jsonOrValuePropertyParse(parentProp+"guarantorHouseholdAnnualIncome",gt1Values.string("guarantorHouseholdAnnualIncome"));
            populateCpField(gt1.getGuarantorHouseholdAnnualIncome(),guarantorHouseholdAnnualIncome);
        }
        //Populate GT1.28 - Guarantor Household Size
        gt1.getGuarantorHouseholdSize().setValue(
                gt1Values.contains("guarantorHouseholdSize") ? gt1Values.string("guarantorHouseholdSize") : ""
        );
        //Populate GT1.29 - Guarantor Employer Id Number
        if (gt1Values.contains("guarantorEmployerIdNumber")){
            List<Json> guarantorEmployerIdNumberList = multipleJsonPropertyParse(parentProp+"guarantorEmployerIdNumber",gt1Values.string("guarantorEmployerIdNumber"));
            for (int i = 0; i < guarantorEmployerIdNumberList.size(); i++) {
                Json guarantorEmployerIdNumber = guarantorEmployerIdNumberList.get(i);
                populateCxField(gt1.getGuarantorEmployerIDNumber(i),guarantorEmployerIdNumber);
            }
        }
        //Populate GT1.30 - Guarantor Marital Status Code
        if (gt1Values.contains("guarantorMaritalStatusCode")){
            Json guarantorMaritalStatusCode = jsonOrValuePropertyParse(parentProp+"guarantorMaritalStatusCode",gt1Values.string("guarantorMaritalStatusCode"));
            populateCweField(gt1.getGuarantorMaritalStatusCode(),guarantorMaritalStatusCode);
        }
        //Populate GT1.31 - Guarantor Hire Effective Date
        gt1.getGuarantorHireEffectiveDate().setValue(
                gt1Values.contains("guarantorHireEffectiveDate") ? gt1Values.string("guarantorHireEffectiveDate") : ""
        );
        //Populate GT1.32 - Employment Stop Date
        gt1.getEmploymentStopDate().setValue(
                gt1Values.contains("employmentStopDate") ? gt1Values.string("employmentStopDate") : ""
        );
        //Populate GT1.33 - Living Dependency
        if (gt1Values.contains("livingDependency")){
            Json livingDependency = jsonOrValuePropertyParse(parentProp+"livingDependency",gt1Values.string("livingDependency"));
            populateCweField(gt1.getLivingDependency(),livingDependency);
        }
        //Populate GT1.34 - Ambulatory Status
        if (gt1Values.contains("ambulatoryStatus")){
            List<Json> ambulatoryStatusList = arrayPropertyToJson(parentProp+"ambulatoryStatus",gt1Values.string("ambulatoryStatus"));
            for (int i = 0; i < ambulatoryStatusList.size(); i++) {
                Json ambulatoryStatus = ambulatoryStatusList.get(i);
                populateCweField(gt1.getAmbulatoryStatus(i),ambulatoryStatus);
            }
        }
        //Populate GT1.35 - Citizenship
        if (gt1Values.contains("citizenship")){
            List<Json> citizenshipList = arrayPropertyToJson(parentProp+"citizenship",gt1Values.string("citizenship"));
            for (int i = 0; i < citizenshipList.size(); i++) {
                Json citizenship = citizenshipList.get(i);
                populateCweField(gt1.getCitizenship(i),citizenship);
            }
        }
        //Populate GT1.36 - Primary Language
        if (gt1Values.contains("primaryLanguage")){
            Json primaryLanguage = jsonOrValuePropertyParse(parentProp+"primaryLanguage",gt1Values.string("primaryLanguage"));
            populateCweField(gt1.getPrimaryLanguage(),primaryLanguage);
        }
        //Populate GT1.37 - Living Arrangement
        if (gt1Values.contains("livingArrangement")){
            Json livingArrangement = jsonOrValuePropertyParse(parentProp+"livingArrangement",gt1Values.string("livingArrangement"));
            populateCweField(gt1.getLivingArrangement(),livingArrangement);
        }
        //Populate GT1.38 - Publicity Code
        if (gt1Values.contains("publicityCode")){
            Json publicityCode = jsonOrValuePropertyParse(parentProp+"publicityCode",gt1Values.string("publicityCode"));
            populateCweField(gt1.getLivingDependency(),publicityCode);
        }
        //Populate GT1.39 - Protection Indicator
        gt1.getProtectionIndicator().setValue(
                gt1Values.contains("protectionIndicator") ? gt1Values.string("protectionIndicator") : ""
        );
        //Populate GT1.40 - Student Indicator
        if (gt1Values.contains("studentIndicator")){
            Json studentIndicator = jsonOrValuePropertyParse(parentProp+"studentIndicator",gt1Values.string("publicityCode"));
            populateCweField(gt1.getStudentIndicator(),studentIndicator);
        }
        //Populate GT1.41 - Religion
        if (gt1Values.contains("religion")){
            Json religion = jsonOrValuePropertyParse(parentProp+"religion",gt1Values.string("religion"));
            populateCweField(gt1.getReligion(),religion);
        }
        //Populate GT1.42 - Mother's Maiden Name
        if (gt1Values.contains("mothersMaidenName")){
            List<Json> mothersMaidenNameList = arrayPropertyToJson(parentProp+"mothersMaidenName",gt1Values.string("mothersMaidenName"));
            for (int i = 0; i < mothersMaidenNameList.size(); i++) {
                Json mothersMaidenName = mothersMaidenNameList.get(i);
                populateXpnField(gt1.getMotherSMaidenName(i),mothersMaidenName);
            }
        }
        //Populate GT1.43 - Nationality
        if (gt1Values.contains("nationality")){
            Json nationality = jsonOrValuePropertyParse(parentProp+"nationality",gt1Values.string("nationality"));
            populateCweField(gt1.getNationality(),nationality);
        }
        //Populate GT1.44 - Ethnic Group
        if (gt1Values.contains("ethnicGroup")){
            List<Json> ethnicGroupList = arrayPropertyToJson(parentProp+"ethnicGroup",gt1Values.string("ethnicGroup"));
            for (int i = 0; i < ethnicGroupList.size(); i++) {
                Json ethnicGroup = ethnicGroupList.get(i);
                populateCweField(gt1.getEthnicGroup(i),ethnicGroup);
            }
        }
        //Populate GT1.45 - Contact Person's Name
        if (gt1Values.contains("contactPersonsName")){
            List<Json> contactPersonsNameList = arrayPropertyToJson(parentProp+"contactPersonsName",gt1Values.string("contactPersonsName"));
            for (int i = 0; i < contactPersonsNameList.size(); i++) {
                Json contactPersonsName = contactPersonsNameList.get(i);
                populateXpnField(gt1.getContactPersonSName(i),contactPersonsName);
            }
        }
        //Populate GT1.46 - Contact Person's Telephone Number
        if (gt1Values.contains("contactPersonsTelephoneNumber")){
            List<Json> contactPersonsTelephoneNumberList = multipleJsonPropertyParse(parentProp+"contactPersonsTelephoneNumber",gt1Values.string("contactPersonsTelephoneNumber"));
            for (int i = 0; i < contactPersonsTelephoneNumberList.size(); i++) {
                Json contactPersonsTelephoneNumber = contactPersonsTelephoneNumberList.get(i);
                populateXtnField(gt1.getContactPersonSTelephoneNumber(i),contactPersonsTelephoneNumber);
            }
        }
        //Populate GT1.47 - Contact Reason
        if (gt1Values.contains("contactReason")){
            Json contactReason = jsonOrValuePropertyParse(parentProp+"contactReason",gt1Values.string("contactReason"));
            populateCweField(gt1.getContactReason(),contactReason);
        }
        //Populate GT1.48 - Contact Relationship
        if (gt1Values.contains("contactRelationship")){
            Json contactRelationship = jsonOrValuePropertyParse(parentProp+"contactRelationship",gt1Values.string("contactRelationship"));
            populateCweField(gt1.getContactRelationship(),contactRelationship);
        }
        //Populate GT1.49 - Job Title
        gt1.getJobTitle().setValue(
                gt1Values.contains("jobTitle") ? gt1Values.string("jobTitle") : ""
        );
        //Populate GT1.50 - Job Code/Class
        if (gt1Values.contains("jobCodeClass")){
            Json jobCodeClass = jsonOrValuePropertyParse(parentProp+"jobCodeClass",gt1Values.string("jobCodeClass"));
            populateJccField(gt1.getJobCodeClass(),jobCodeClass);
        }
        //Populate GT1.51 - Guarantor Employer's Organization Name
        if (gt1Values.contains("guarantorEmployersOrganizationName")){
            List<Json> guarantorEmployersOrganizationNameList = arrayPropertyToJson(parentProp+"guarantorEmployersOrganizationName",gt1Values.string("guarantorEmployersOrganizationName"));
            for (int i = 0; i < guarantorEmployersOrganizationNameList.size(); i++) {
                Json guarantorEmployersOrganizationName = guarantorEmployersOrganizationNameList.get(i);
                populateXonField(gt1.getGuarantorEmployerSOrganizationName(i),guarantorEmployersOrganizationName);
            }
        }
        //Populate GT1.52 - Handicap
        if (gt1Values.contains("handicap")){
            Json handicap = jsonOrValuePropertyParse(parentProp+"handicap",gt1Values.string("handicap"));
            populateCweField(gt1.getHandicap(),handicap);
        }
        //Populate GT1.53 - Job Status
        if (gt1Values.contains("jobStatus")){
            Json jobStatus = jsonOrValuePropertyParse(parentProp+"jobStatus",gt1Values.string("jobStatus"));
            populateCweField(gt1.getJobStatus(),jobStatus);
        }
        //Populate GT1.54 - Guarantor Financial Class
        if (gt1Values.contains("guarantorFinancialClass")){
            Json guarantorFinancialClass = jsonOrValuePropertyParse(parentProp+"guarantorFinancialClass",gt1Values.string("guarantorFinancialClass"));
            populateFcField(gt1.getGuarantorFinancialClass(),guarantorFinancialClass);
        }
        //Populate GT1.55 - Guarantor Race
        if (gt1Values.contains("guarantorRace")){
            List<Json> guarantorRaceList = arrayPropertyToJson(parentProp+"guarantorRace",gt1Values.string("guarantorRace"));
            for (int i = 0; i < guarantorRaceList.size(); i++) {
                Json guarantorRace = guarantorRaceList.get(i);
                populateCweField(gt1.getGuarantorRace(i),guarantorRace);
            }
        }
        //Populate GT1.56 - Guarantor Birth Place
        gt1.getGuarantorBirthPlace().setValue(
                gt1Values.contains("guarantorBirthPlace") ? gt1Values.string("guarantorBirthPlace") : ""
        );
        //Populate GT1.57 - Vip Indicator
        if (gt1Values.contains("vipIndicator")){
            Json vipIndicator = jsonOrValuePropertyParse(parentProp+"vipIndicator",gt1Values.string("vipIndicator"));
            populateCweField(gt1.getVIPIndicator(),vipIndicator);
        }
    }

    public static void populateAutSegment(AUT aut, Json autValues) throws DataTypeException {
        String parentProp = "authorizationInformation.";

        //Populate AUT.1 - Authorizing Payor, Plan Id
        if (autValues.contains("authorizingPayorPlanId")){
            Json authorizingPayorPlanId = jsonOrValuePropertyParse(parentProp+"authorizingPayorPlanId",autValues.string("authorizingPayorPlanId"));
            populateCweField(aut.getAuthorizingPayorPlanID(),authorizingPayorPlanId);
        }
        //Populate AUT.2 - Authorizing Payor, Company Id
        if (autValues.contains("authorizingPayorCompanyId")){
            Json authorizingPayorCompanyId = jsonOrValuePropertyParse(parentProp+"authorizingPayorCompanyId",autValues.string("authorizingPayorCompanyId"));
            populateCweField(aut.getAuthorizingPayorCompanyID(),authorizingPayorCompanyId);
        }
        //Populate AUT.3 - Authorizing Payor, Company Name
        aut.getAuthorizingPayorCompanyName().setValue(
                autValues.contains("authorizingPayorCompanyName") ? autValues.string("authorizingPayorCompanyName") : ""
        );
        //Populate AUT.4 - Authorization Effective Date
        aut.getAuthorizationEffectiveDate().setValue(
                autValues.contains("authorizationEffectiveDate") ? autValues.string("authorizationEffectiveDate") : ""
        );
        //Populate AUT.5 - Authorization Expiration Date
        aut.getAuthorizationExpirationDate().setValue(
                autValues.contains("authorizationExpirationDate") ? autValues.string("authorizationExpirationDate") : ""
        );
        //Populate AUT.6 - Authorization Identifier
        if (autValues.contains("authorizationIdentifier")){
            Json authorizationIdentifier = jsonOrValuePropertyParse(parentProp+"authorizationIdentifier",autValues.string("authorizationIdentifier"));
            populateEiField(aut.getAuthorizationIdentifier(),authorizationIdentifier);
        }
        //Populate AUT.7 - Reimbursement Limit
        if (autValues.contains("reimbursementLimit")){
            Json reimbursementLimit = jsonOrValuePropertyParse(parentProp+"reimbursementLimit",autValues.string("reimbursementLimit"));
            populateCpField(aut.getReimbursementLimit(),reimbursementLimit);
        }
        //Populate AUT.8 - Requested Number Of Treatments
        if (autValues.contains("requestedNumberOfTreatments")){
            Json requestedNumberOfTreatments = jsonOrValuePropertyParse(parentProp+"requestedNumberOfTreatments",autValues.string("requestedNumberOfTreatments"));
            populateCqField(aut.getRequestedNumberOfTreatments(),requestedNumberOfTreatments);
        }
        //Populate AUT.9 - Authorized Number Of Treatments
        if (autValues.contains("authorizedNumberOfTreatments")){
            Json authorizedNumberOfTreatments = jsonOrValuePropertyParse(parentProp+"authorizedNumberOfTreatments",autValues.string("authorizedNumberOfTreatments"));
            populateCqField(aut.getAuthorizedNumberOfTreatments(),authorizedNumberOfTreatments);
        }
        //Populate AUT.10 - Process Date
        aut.getProcessDate().setValue(
                autValues.contains("processDate") ? autValues.string("processDate") : ""
        );
        //Populate AUT.11 - Requested Discipline(s)
        if (autValues.contains("requestedDisciplines")){
            List<Json> requestedDisciplinesList = arrayPropertyToJson("requestedDisciplines", autValues.string("requestedDisciplines"));
            for (int i = 0; i < requestedDisciplinesList.size(); i++) {
                Json requestedDisciplines = requestedDisciplinesList.get(i);
                populateCweField(aut.getRequestedDisciplineS(i),requestedDisciplines);
            }
        }
        //Populate AUT.12 - Authorized Discipline(s)
        if (autValues.contains("authorizedDisciplines")){
            List<Json> authorizedDisciplinesList = arrayPropertyToJson("authorizedDisciplines", autValues.string("authorizedDisciplines"));
            for (int i = 0; i < authorizedDisciplinesList.size(); i++) {
                Json authorizedDisciplines = authorizedDisciplinesList.get(i);
                populateCweField(aut.getRequestedDisciplineS(i),authorizedDisciplines);
            }
        }
        //Populate AUT.13 - Authorization Referral Type
        if (autValues.contains("authorizationReferralType")){
            Json authorizationReferralType = jsonOrValuePropertyParse(parentProp+"authorizationReferralType",autValues.string("authorizationReferralType"));
            populateCweField(aut.getAuthorizationReferralType(),authorizationReferralType);
        }
        //Populate AUT.14 - Approval Status
        if (autValues.contains("approvalStatus")){
            Json approvalStatus = jsonOrValuePropertyParse(parentProp+"approvalStatus",autValues.string("approvalStatus"));
            populateCweField(aut.getApprovalStatus(),approvalStatus);
        }
        //Populate AUT.15 - Planned Treatment Stop Date
        aut.getPlannedTreatmentStopDate().setValue(
                autValues.contains("plannedTreatmentStopDate") ? autValues.string("plannedTreatmentStopDate") : ""
        );
        //Populate AUT.16 - Clinical Service
        if (autValues.contains("clinicalService")){
            Json clinicalService = jsonOrValuePropertyParse(parentProp+"clinicalService",autValues.string("clinicalService"));
            populateCweField(aut.getClinicalService(),clinicalService);
        }
        //Populate AUT.17 - Reason Text
        aut.getReasonText().setValue(
                autValues.contains("reasonText") ? autValues.string("reasonText") : ""
        );
        //Populate AUT.18 - Number of Authorized Treatments/Units
        if (autValues.contains("numberOfAuthorizedTreatmentsUnits")){
            Json numberOfAuthorizedTreatmentsUnits = jsonOrValuePropertyParse(parentProp+"numberOfAuthorizedTreatmentsUnits",autValues.string("numberOfAuthorizedTreatmentsUnits"));
            populateCqField(aut.getNumberOfAuthorizedTreatmentsUnits(),numberOfAuthorizedTreatmentsUnits);
        }
        //Populate AUT.19 - Number of Used Treatments/Units
        if (autValues.contains("numberOfUsedTreatmentsUnits")){
            Json numberOfUsedTreatmentsUnits = jsonOrValuePropertyParse(parentProp+"numberOfUsedTreatmentsUnits",autValues.string("numberOfUsedTreatmentsUnits"));
            populateCqField(aut.getNumberOfUsedTreatmentsUnits(),numberOfUsedTreatmentsUnits);
        }
        //Populate AUT.20 - Number of Schedule Treatments/Units
        if (autValues.contains("numberOfScheduleTreatmentsUnits")){
            Json numberOfScheduleTreatmentsUnits = jsonOrValuePropertyParse(parentProp+"numberOfScheduleTreatmentsUnits",autValues.string("numberOfScheduleTreatmentsUnits"));
            populateCqField(aut.getNumberOfScheduleTreatmentsUnits(),numberOfScheduleTreatmentsUnits);
        }
        //Populate AUT.21 - Encounter Type
        if (autValues.contains("encounterType")){
            Json encounterType = jsonOrValuePropertyParse(parentProp+"encounterType",autValues.string("encounterType"));
            populateCweField(aut.getEncounterType(),encounterType);
        }
        //Populate AUT.22 - Remaining Benefit Amount
        if(autValues.contains("remainingBenefitAmount")){
            Json remainingBenefitAmount = jsonOrValuePropertyParse(parentProp+"remainingBenefitAmount",autValues.string("remainingBenefitAmount"));
            populateMoField(aut.getRemainingBenefitAmount(),remainingBenefitAmount);
        }
        //Populate AUT.23 - Authorized Provider
        if(autValues.contains("authorizedProvider")){
            Json authorizedProvider = jsonOrValuePropertyParse(parentProp+"authorizedProvider",autValues.string("authorizedProvider"));
            populateXonField(aut.getAuthorizedProvider(),authorizedProvider);
        }
        //Populate AUT.24 - Authorized Health Professional
        if(autValues.contains("authorizedHealthProfessional")){
            Json authorizedHealthProfessional = singleJsonPropertyParse(parentProp+"authorizedHealthProfessional",autValues.string("authorizedHealthProfessional"));
            populateXcnField(aut.getAuthorizedHealthProfessional(),authorizedHealthProfessional);
        }
        //Populate AUT.25 - Source Text
        aut.getSourceText().setValue(
                autValues.contains("sourceText") ? autValues.string("sourceText") : ""
        );
        //Populate AUT.26 - Source Date
        aut.getSourceDate().setValue(
                autValues.contains("sourceDate") ? autValues.string("sourceDate") : ""
        );
        //Populate AUT.27 - Source Phone
        if(autValues.contains("sourcePhone")){
            Json sourcePhone = singleJsonPropertyParse(parentProp+"sourcePhone",autValues.string("sourcePhone"));
            populateXtnField(aut.getSourcePhone(),sourcePhone);
        }
        //Populate AUT.28 - Comment
        aut.getComment().setValue(
                autValues.contains("comment") ? autValues.string("comment") : ""
        );
        //Populate AUT.29 - Action Code
        aut.getActionCode().setValue(
                autValues.contains("actionCode") ? autValues.string("actionCode") : ""
        );
    }

    public static void populatePdaSegment(PDA pda, Json pdaValues) throws DataTypeException {
        String parentProp = "patientDeathAndAutopsy.";

        //Populate PDA.1 - Death Cause Code
        if (pdaValues.contains("deathCauseCode")) {
            List<Json> deathCauseCodeList = arrayPropertyToJson(parentProp+"deathCauseCode", pdaValues.string("deathCauseCode"));
            for (int i = 0; i < deathCauseCodeList.size(); i++) {
                Json deathCauseCode = deathCauseCodeList.get(i);
                populateCweField(pda.getDeathCauseCode(i), deathCauseCode);
            }
        }
        //Populate PDA.2 - Death Location
        if(pdaValues.contains("deathLocation")){
            Json deathLocation = jsonOrValuePropertyParse(parentProp+"deathLocation",pdaValues.string("deathLocation"));
            populatePlField(pda.getDeathLocation(),deathLocation);
        }
        //Populate PDA.3 - Death Certified Indicator
        pda.getDeathCertifiedIndicator().setValue(
                pdaValues.contains("deathCertifiedIndicator") ? pdaValues.string("deathCertifiedIndicator") : ""
        );
        //Populate PDA.4 - Death Certificate Signed Date/Time
        pda.getDeathCertificateSignedDateTime().setValue(
                pdaValues.contains("deathCertificateSignedDateTime") ? pdaValues.string("deathCertificateSignedDateTime") : ""
        );
        //Populate PDA.5 - Death Certified By
        if(pdaValues.contains("deathCertifiedBy")){
            Json deathCertifiedBy = jsonOrValuePropertyParse(parentProp+"deathCertifiedBy",pdaValues.string("deathCertifiedBy"));
            populateXcnField(pda.getDeathCertifiedBy(),deathCertifiedBy);
        }
        //Populate PDA.6 - Autopsy Indicator
        pda.getAutopsyIndicator().setValue(
                pdaValues.contains("autopsyIndicator") ? pdaValues.string("autopsyIndicator") : ""
        );
        //Populate PDA.7 - Autopsy Start And End Date/Time
        if(pdaValues.contains("autopsyStartAndEndDateTime")){
            Json autopsyStartAndEndDateTime = jsonOrValuePropertyParse(parentProp+"autopsyStartAndEndDateTime",pdaValues.string("autopsyStartAndEndDateTime"));
            populateDrField(pda.getAutopsyStartAndEndDateTime(),autopsyStartAndEndDateTime);
        }
        //Populate PDA.8 - Autopsy Performed By
        if(pdaValues.contains("autopsyPerformedBy")){
            Json autopsyPerformedBy = jsonOrValuePropertyParse(parentProp+"autopsyPerformedBy",pdaValues.string("autopsyPerformedBy"));
            populateXcnField(pda.getAutopsyPerformedBy(),autopsyPerformedBy);
        }
        //Populate PDA.9 - Coroner Indicator
        pda.getCoronerIndicator().setValue(
                pdaValues.contains("coronerIndicator") ? pdaValues.string("coronerIndicator") : ""
        );
    }
}
