package io.slingr.endpoints.hl7.populators;

import ca.uhn.hl7v2.HL7Exception;
import ca.uhn.hl7v2.model.AbstractGroup;
import ca.uhn.hl7v2.model.AbstractMessage;
import ca.uhn.hl7v2.model.DataTypeException;
import ca.uhn.hl7v2.model.v281.group.OML_O21_PATIENT;
import ca.uhn.hl7v2.model.v281.segment.*;
import io.slingr.endpoints.exceptions.EndpointException;
import io.slingr.endpoints.exceptions.ErrorCode;
import io.slingr.endpoints.hl7.Hl7Endpoint;
import io.slingr.endpoints.utils.Json;

import javax.swing.*;
import java.util.Date;
import java.util.List;
import java.util.Set;

import static io.slingr.endpoints.hl7.jsonHelper.JsonHelper.*;
import static io.slingr.endpoints.hl7.populators.DataTypePopulator.*;

public class SegmentPopulator {

    public static void populateMessage(AbstractMessage msg, Json params) throws HL7Exception {
        //The keys from the JSON
        Set<String> keys = params.keys();

        if (keys.isEmpty()) {
            throw EndpointException.permanent(ErrorCode.ARGUMENT,"The body cannot be empty");
        }

        for (String key : keys) {
            String propPath = key;
            String propValue = params.string(key);
            //We match each prop to every possible segment
            switch (key){
                //Populate MSH "Message Header" segment with extra info if included
                case "messageHeader":
                    MSH msh = (MSH) msg.get("MSH");
                    Json mshValues = singleJsonPropertyParse(propPath, propValue);
                    populateMshSegment(msh,mshValues);
                    break;

                //Populate SFT "Software Segment" segment, which is a repeatable segment
                case "softwareSegment":
                    List<Json> softwareSegmentList = arrayPropertyToJson(propPath, propValue);
                    for (int i = 0; i < softwareSegmentList.size(); i++) {
                        SFT sft = (SFT) msg.get("SFT",i);
                        Json softwareSegment = softwareSegmentList.get(i);
                        populateSftSegment(sft, softwareSegment);
                    }

                //Populate the UAC Segment
                case "userAuthenticationCredentialSegment":
                    UAC uac = (UAC) msg.get("UAC");
                    Json uacValues = singleJsonPropertyParse(propPath, propValue);
                    populateUacSegment(uac,uacValues);
                    break;

                case "notesAndComments":
                    List<Json> notesAndCommentsList = arrayPropertyToJson(propPath, propValue);
                    for (int i = 0; i < notesAndCommentsList.size(); i++) {
                        NTE nte = (NTE) msg.get("NTE",i);
                        Json notesAndComments = notesAndCommentsList.get(i);
                        populateNteSegment(nte, notesAndComments);
                    }
                    break;

                //Populate the EVN Segment with extra info if included
                case "eventType":
                    EVN evn = (EVN) msg.get("EVN");
                    Json evnValues = singleJsonPropertyParse(propPath, propValue);
                    populateEvnSegment(evn,evnValues);
                    break;

                // Populate the PID "Patient Identification" Segment
                case "patientIdentification":
                    PID pid = (PID) msg.get("PID");
                    Json patientIdentification = jsonOrValuePropertyParse(propPath, propValue);
                    populatePidSegment(pid, patientIdentification);
                    break;

                // Populate the PD1 "Patient Additional Demographic" Segment
                case "patientAdditionalDemographic":
                    PD1 pd1 = (PD1) msg.get("PD1");
                    Json patientAdditionalDemographic = jsonOrValuePropertyParse(propPath, propValue);
                    populatePd1Segment(pd1, patientAdditionalDemographic);
                    break;

                //Populate NK1 "Next Of Kin / Associated Parties" segment, which is a repeatable segment
                case "nextOfKin":
                    List<Json> nextOfKinList = arrayPropertyToJson(propPath, propValue);
                    for (int i = 0; i < nextOfKinList.size(); i++) {
                        NK1 nk1 = (NK1) msg.get("NK1",i);
                        Json nextOfKin = nextOfKinList.get(i);
                        populateNk1Segment(nk1, nextOfKin);
                    }
                    break;

                // Populate the PV1 ("Patient Visit") Segment
                case "patientVisit":
                    PV1 pv1 = (PV1) msg.get("PV1");
                    Json patientVisit = jsonOrValuePropertyParse(propPath, propValue);
                    populatePv1Segment(pv1, patientVisit);
                    break;

                // Populate the PV2 "Patient Visit - Additional Information" Segment
                case "patientVisitAdditionalInformation":
                    PV2 pv2 = (PV2) msg.get("PV2");
                    Json patientVisitAdditionalInformation = jsonOrValuePropertyParse(propPath, propValue);
                    populatePv2Segment(pv2, patientVisitAdditionalInformation);
                    break;

                // Populate the DB1 - Disability Segment
                case "disability":
                    List<Json> disabilityList = arrayPropertyToJson(propPath, propValue);
                    for (int i = 0; i < disabilityList.size(); i++) {
                        DB1 db1 = (DB1) msg.get("DB1",i);
                        Json disability = disabilityList.get(i);
                        populateDb1Segment(db1, disability);
                    }
                    break;

                // Populate the AL1 "Patient Allergy Information" Segment, which is repeatable
                case "patientAllergyInformation":
                    List<Json> patientAllergyInformationList = arrayPropertyToJson(propPath, propValue);
                    for (int i = 0; i < patientAllergyInformationList.size(); i++) {
                        AL1 al1 = (AL1) msg.get("AL1",i);
                        Json patientAllergyInformation = patientAllergyInformationList.get(i);
                        populateAl1Segment(al1, patientAllergyInformation);
                    }
                    break;

                // Populate the DG1 "Diagnosis" Segment, which is repeatable
                case "diagnosis":
                    List<Json> diagnosisList = arrayPropertyToJson(propPath, propValue);
                    for (int i = 0; i < diagnosisList.size(); i++) {
                        DG1 dg1 = (DG1) msg.get("DG1",i);
                        Json diagnosis = diagnosisList.get(i);
                        populateDg1Segment(dg1, diagnosis);
                    }
                    break;

                // Populate the PROCEDURE group, which is repeatable
                case "PROCEDURE":
                    //This List could be an array of PR1 or it could be and array of tuples(PR1,ROL)
                    List<Json> procedureList = multipleJsonPropertyParse(propPath, propValue);
                    for (int i = 0; i < procedureList.size(); i++) {
                        //As PROCEDURE is a group, we have to do this in order to generalize it for every possible PROCEDURE group, which could be adt_a01,a03,etc.
                        AbstractGroup procedureGroup = (AbstractGroup) msg.get("PROCEDURE",i);
                        PR1 pr1 = (PR1) procedureGroup.get("PR1");
                        Json procedure = procedureList.get(i);
                        //Here we know if it is the tuple or if it is just the PR1
                        if(procedure.contains("procedures")){
                            String propPath2 = propPath+".procedures["+i+"]";
                            Json procedures = singleJsonPropertyParse(propPath2, propValue);
                            //Populate the PR1 "Procedures" Segment
                            populatePr1Segment(pr1,procedures);
                            if(procedure.contains("role")){
                                propPath2 = propPath+".role["+i+"]";
                                List<Json> roleList = multipleJsonPropertyParse(propPath, propValue);
                                for (int j = 0; j < roleList.size(); j++) {
                                    ROL _PROCEDURE_rol = (ROL) procedureGroup.get("ROL",j);
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
                    List<Json> guarantorList = arrayPropertyToJson(propPath, propValue);
                    for (int i = 0; i < guarantorList.size(); i++) {
                        GT1 gt1 = (GT1) msg.get("GT1",i);
                        Json guarantor = guarantorList.get(i);
                        populateGt1Segment(gt1, guarantor);
                    }
                    break;

                // Populate the "INSURANCE" group, which is repeatable
                case  "INSURANCE":
                    //This List could be an array of IN1 or it could be and array of 6-tuples(IN1,IN2,IN3,ROL,AUT,RF1)
                    List<Json> insuranceList = multipleJsonPropertyParse(propPath, propValue);
                    for (int i = 0; i < insuranceList.size(); i++) {
                        //As INSURANCE is a group, we have to do this in order to generalize it for every possible INSURANCE group, which could be adt_a01,a03,etc.
                        AbstractGroup insuranceGroup = (AbstractGroup) msg.get("INSURANCE",i);
                        IN1 in1 = (IN1) insuranceGroup.get("IN1");
                        Json insurance = insuranceList.get(i);
                        //Here we know if it is the tuple or if it is just the IN1
                        if(insurance.contains("insurance")){
                            String propPath2 = propPath+".insurance";
                            Json innerInsurance = singleJsonPropertyParse(propPath2, propValue);
                            //Populate the PR1 "Procedures" Segment
                            populateIn1Segment(in1,innerInsurance);
                            if(insurance.contains("insuranceAdditionalInformation")){
                                propPath2 = propPath+".insuranceAdditionalInformation";
                                IN2 in2 = (IN2) insuranceGroup.get("IN2");
                                Json insuranceAdditionalInformation = singleJsonPropertyParse(propPath2, propValue);
                                populateIn2Segment(in2,insuranceAdditionalInformation);
                            }
                            if(insurance.contains("insuranceAdditionalInformationCertification")) {
                                propPath2 = propPath+".insuranceAdditionalInformationCertification";
                                List<Json> insuranceAdditionalInformationCertificationList = multipleJsonPropertyParse(propPath2, propValue);
                                for (Json insuranceAdditionalInformationCertification : insuranceAdditionalInformationCertificationList) {
                                    IN3 in3 = (IN3) insuranceGroup.get("IN3");
                                    populateIn3Segment(in3, insuranceAdditionalInformationCertification);
                                }
                            }
                            if(insurance.contains("role")){
                                propPath2 = propPath+".role";
                                List<Json> roleList = multipleJsonPropertyParse(propPath2, propValue);
                                for(int j = 0; j < roleList.size(); j++){
                                    ROL _INSURANCE_rol = (ROL) insuranceGroup.get("ROL",j);
                                    Json role = roleList.get(j);
                                    //Populate the ROL "Role" Segment
                                    populateRolSegment(_INSURANCE_rol,role);
                                }
                            }
                            if(insurance.contains("authorizationInformation")){
                                propPath2 = propPath+".authorizationInformation";
                                List<Json> authorizationInformationList = multipleJsonPropertyParse(propPath2, propValue);
                                for(int j = 0; j < authorizationInformationList.size(); j++){
                                    AUT aut = (AUT) insuranceGroup.get("AUT",j);
                                    Json authorizationInformation = authorizationInformationList.get(j);
                                    //Populate the ROL "Role" Segment
                                    populateAutSegment(aut,authorizationInformation);
                                }
                            }
                            if(insurance.contains("referralInformation")){
                                propPath2 = propPath+".referralInformation";
                                List<Json> referralInformationList = multipleJsonPropertyParse(propPath2, propValue);
                                for(int j = 0; j < referralInformationList.size(); j++){
                                    RF1 rf1 = (RF1) insuranceGroup.get("RF1",j);
                                    Json referralInformation = referralInformationList.get(j);
                                    //Populate the ROL "Role" Segment
                                    populateRf1Segment(rf1,referralInformation);
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
                    List<Json> observationResultList = arrayPropertyToJson(propPath, propValue);
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
                    Json patientDeathAndAutopsy = singleJsonPropertyParse(propPath, propValue);
                    populatePdaSegment(pda, patientDeathAndAutopsy);
                    break;

                // Populate the PATIENT group of OML_O21 messages
                case "PATIENT":
                    //As PATIENT is a group, we have to do this in order to generalize it for every possible PATIENT group, which could be OML_O21,O33,O35,etc.
                    AbstractGroup patientGroup = (AbstractGroup) msg.get("PATIENT");
                    PID patientGroupPid = (PID) patientGroup.get("PID");
                    Json patientGroupValues = singleJsonPropertyParse(propPath,propValue);
                    System.out.println("patientGroupValues: "+patientGroupValues.toString());
                    Set<String> patientGroupKeys = patientGroupValues.keys();
                    if (patientGroupKeys.contains("patientIdentification")){
                        for (String patientGroupKey: patientGroupKeys) {
                            String patientGroupPropPath = propPath + "." + patientGroupKey;
                            String patientGroupPropValue = patientGroupValues.string(patientGroupKey);

                            switch(patientGroupKey){
                                case "patientIdentification":
                                    Json patientGroupPatientIdentification = singleJsonPropertyParse(patientGroupPropPath,patientGroupPropValue);
                                    populatePidSegment(patientGroupPid,patientGroupPatientIdentification);
                                    break;
                                case "patientAdditionalDemographic":
                                    PD1 patientGroupPd1 = (PD1) patientGroup.get("PD1");
                                    Json patientGroupPatientAdditionalDemographic = singleJsonPropertyParse(patientGroupPropPath,patientGroupPropValue);
                                    populatePd1Segment(patientGroupPd1,patientGroupPatientAdditionalDemographic);
                                    break;
                                case "participationInformation":
                                    List<Json> patientGroupParticipationInformationList = multipleJsonPropertyParse(patientGroupPropPath,patientGroupPropValue);
                                    for (int i = 0; i < patientGroupParticipationInformationList.size(); i++) {
                                        PRT patientGroupPrt = (PRT) patientGroup.get("PRT",i);
                                        Json patientGroupParticipationInformation = patientGroupParticipationInformationList.get(i);
                                        //populatePrtSegment(patientGroupPrt,patientGroupParticipationInformation); TO BE IMPLEMENT
                                    }
                                    break;
                                case "notesAndComments":
                                    List<Json> patientGroupNotesAndCommentsList = arrayPropertyToJson(patientGroupPropPath,patientGroupPropValue);
                                    for (int i = 0; i < patientGroupNotesAndCommentsList.size(); i++) {
                                        NTE patientGroupNte = (NTE) patientGroup.get("NTE",i);
                                        Json patientGroupNotesAndComments = patientGroupNotesAndCommentsList.get(i);
                                        populateNteSegment(patientGroupNte,patientGroupNotesAndComments);
                                    }
                                    break;
                                case "nextOfKinAssociatedParties":
                                    List<Json> patientGroupNextOfKinAssociatedPartiesList = multipleJsonPropertyParse(patientGroupPropPath,patientGroupPropValue);
                                    for (int i = 0; i < patientGroupNextOfKinAssociatedPartiesList.size(); i++) {
                                        NK1 patientGroupNk1 = (NK1) patientGroup.get("NK1",i);
                                        Json patientGroupNextOfKinAssociatedParties = patientGroupNextOfKinAssociatedPartiesList.get(i);
                                        populateNk1Segment(patientGroupNk1,patientGroupNextOfKinAssociatedParties);
                                    }
                                    break;
                                case "accessRestriction":
                                    ARV patientGroupArv = (ARV) patientGroup.get("ARV");
                                    Json patientGroupAccessRestriction = singleJsonPropertyParse(patientGroupPropPath,patientGroupPropValue);
                                    //populateArvSegment(patientGroupArv,patientGroupAccessRestriction); TO BE IMPLEMENTED
                                    break;
                                case "PATIENT_VISIT":
                                    AbstractGroup patientVisitGroup = (AbstractGroup) patientGroup.get("PATIENT_VISIT");
                                    PV1 patientVisitGroupPv1 = (PV1) patientVisitGroup.get("PV1");
                                    Json patientVisitGroupValues = singleJsonPropertyParse(propPath,propValue);
                                    Set<String> patientVisitGroupKeys = patientVisitGroupValues.keys();
                                    if (patientVisitGroupKeys.contains("patientVisit")){
                                        for (String patientVisitGroupKey: patientVisitGroupKeys) {
                                            String patientVisitGroupPropPath = patientGroupPropPath + "." + patientVisitGroupKey;
                                            String patientVisitGroupPropValue = patientVisitGroupValues.string(patientVisitGroupKey);

                                            switch (patientGroupKey) {
                                                case "patientVisit":
                                                    Json patientVisitGroupPv1Values = singleJsonPropertyParse(patientVisitGroupPropPath,patientVisitGroupPropValue);
                                                    populatePv1Segment(patientVisitGroupPv1,patientVisitGroupPv1Values);
                                                    break;
                                                case "patientVisitAdditionalInformation":
                                                    PV2 patientVisitGroupPv2 = (PV2) patientVisitGroup.get("PV2");
                                                    Json patientVisitGroupPv2Values = singleJsonPropertyParse(patientVisitGroupPropPath,patientVisitGroupPropValue);
                                                    populatePv2Segment(patientVisitGroupPv2,patientVisitGroupPv2Values);
                                                    break;
                                                case "participationInformation":
                                                    List<Json> patientVisitGroupPrtValuesList = multipleJsonPropertyParse(patientVisitGroupPropPath,patientVisitGroupPropValue);
                                                    for (int i=0; i<patientVisitGroupPrtValuesList.size(); i++){
                                                        PRT patientVisitGroupPrt = (PRT) patientVisitGroup.get("PRT",i);
                                                        Json patientVisitGroupPrtValues = patientVisitGroupPrtValuesList.get(i);
                                                        //populatePrtSegment()
                                                    }
                                                    break;
                                                default:
                                                    throw EndpointException.permanent(ErrorCode.ARGUMENT,"The property ['"+patientGroupKey+"'] does not correspond with any possible PATIENT_VISIT segment");
                                            }
                                        }
                                    } else {
                                        populatePv1Segment(patientVisitGroupPv1,patientVisitGroupValues);
                                    }
                                    break;
                                case "INSURANCE":
                                    AbstractGroup patientInsuranceGroup = (AbstractGroup) patientGroup.get("INSURANCE");
                                    IN1 patientInsuranceGroupIn1 = (IN1) patientInsuranceGroup.get("IN1");
                                    List<Json> patientInsuranceGroupList = multipleJsonPropertyParse(patientGroupPropPath,patientGroupPropValue);
                                    for (int i=0 ; i < patientInsuranceGroupList.size() ; i++){
                                        Json patientInsuranceGroupPropValue = patientInsuranceGroupList.get(i);
                                        if(patientInsuranceGroupPropValue.contains("insurance")){
                                            Set<String>patientInsuranceGroupPropKeys = patientInsuranceGroupPropValue.keys();
                                            for (String patientInsuranceGroupPropKey: patientInsuranceGroupPropKeys) {
                                                String patientInsuranceGroupPropPath = patientGroupPropPath + "." + patientInsuranceGroupPropKey;
                                                String patientInsuranceGroupPropValueString = patientInsuranceGroupPropValue.string(patientInsuranceGroupPropKey);
                                                switch (patientInsuranceGroupPropKey) {
                                                    case "insurance":
                                                        Json patientVisitGroupIn1Values = singleJsonPropertyParse(patientInsuranceGroupPropPath,patientInsuranceGroupPropValueString);
                                                        populateIn1Segment(patientInsuranceGroupIn1,patientVisitGroupIn1Values);
                                                        break;
                                                    case "insuranceAdditionalInformation":
                                                        IN2 patientInsuranceGroupIn2 = (IN2) patientInsuranceGroup.get("IN2");
                                                        Json patientInsuranceGroupIn2Values = singleJsonPropertyParse(patientInsuranceGroupPropPath,patientInsuranceGroupPropValueString);
                                                        populateIn2Segment(patientInsuranceGroupIn2,patientInsuranceGroupIn2Values);
                                                        break;
                                                    case "insuranceAdditionalInformationCertification":
                                                        IN3 patientInsuranceGroupIn3 = (IN3) patientInsuranceGroup.get("IN3");
                                                        Json patientInsuranceGroupIn3Values = singleJsonPropertyParse(patientInsuranceGroupPropPath,patientInsuranceGroupPropValueString);
                                                        populateIn3Segment(patientInsuranceGroupIn3,patientInsuranceGroupIn3Values);
                                                        break;
                                                    default:
                                                        throw EndpointException.permanent(ErrorCode.ARGUMENT,"The property ['"+patientInsuranceGroupPropKey+"'] does not correspond with any possible INSURANCE segment");
                                                }
                                            }
                                        } else {
                                            populateIn1Segment(patientInsuranceGroupIn1,patientInsuranceGroupPropValue);
                                        }
                                    }
                                    break;
                                case "guarantor":
                                    GT1 gt1 = (GT1) patientGroup.get("GT1");
                                    Json gt1Values = singleJsonPropertyParse(patientGroupPropPath,patientGroupPropValue);
                                    populateGt1Segment(gt1,gt1Values);
                                    break;
                                case "patientAllergyInformation":
                                    List<Json> patientGroupPatientAllergyInformationList = multipleJsonPropertyParse(patientGroupPropPath,patientGroupPropValue);
                                    for (int i=0; i<patientGroupPatientAllergyInformationList.size(); i++){
                                        GT1 patientGroupGt1 = (GT1) patientGroup.get("GT1",i);
                                        Json patientGroupGt1Values = patientGroupPatientAllergyInformationList.get(i);
                                        populateGt1Segment(patientGroupGt1,patientGroupGt1Values);
                                    }
                                    break;
                                default:
                                    throw EndpointException.permanent(ErrorCode.ARGUMENT,"The property ['"+key+"'] does not correspond with any possible HL7 segment");
                            }
                        }
                    //If the keys dont have "patientIdentification" that means that the PATIENT json has the PID values directly
                    } else {
                        Json patientGroupPatientIdentification = singleJsonPropertyParse(propPath,propValue);
                        populatePidSegment(patientGroupPid,patientGroupPatientIdentification);
                    }
                    break;
                // Populate the PATIENT group of OML_O21 messages
                case "ORDER":
                    //This List could be an array of ORC or it could be and array of tuples(ORC,PRT)
                    List<Json> orderList = multipleJsonPropertyParse(propPath, propValue);
                    for (int i = 0; i < orderList.size(); i++) {
                        AbstractGroup orderGroup = (AbstractGroup) msg.get("ORDER",i);
                        ORC orc = (ORC) orderGroup.get("ORC");
                        Json order = orderList.get(i);
                        Set<String> orderKeys = order.keys();
                        if (orderKeys.contains("commonOrder")){
                            for (String orderKey: orderKeys) {
                                String orderPropPath = propPath + "." + orderKey;
                                String orderPropValue = order.string(orderKey);
                                switch (orderKey) {
                                    case "commonOrder":
                                        Json commonOrder = singleJsonPropertyParse(orderPropPath,orderPropValue);
                                        populateOrcSegment(orc,commonOrder);
                                        break;
                                    case "participationInformation":
                                        PRT prt = (PRT) orderGroup.get("PRT");
                                        List<Json> participationInformationList = multipleJsonPropertyParse(orderPropPath,orderPropValue);
                                        for (int j = 0; j < participationInformationList.size(); j++) {
                                            Json participationInformation = participationInformationList.get(j);
                                            //populatePrtSegment(prt,participationInformation);
                                        }
                                        break;
                                    case "TIMING":
                                        String orderTimingPath = orderPropPath+".TIMING";
                                        String orderTimingValues = order.string("TIMING");
                                        AbstractGroup timingGroup = (AbstractGroup) orderGroup.get("TIMING");
                                        List<Json> timingList = arrayPropertyToJson(orderTimingPath, orderTimingValues);
                                        for (int j = 0; j < timingList.size(); j++) {
                                            TQ1 orderTimingTq1 = (TQ1) timingGroup.get("TQ1");
                                            Json timingValue = timingList.get(j);
                                            Set<String> timingKeys = timingValue.keys();
                                            if (timingKeys.contains("timingQuantity")){
                                                for (String timingKey: timingKeys) {
                                                    String timingPath = orderTimingPath + "." + timingKey;
                                                    String timingValues = timingValue.string(timingKey);

                                                    switch(timingKey){
                                                        case "timingQuantity":
                                                            Json timingQuantity = singleJsonPropertyParse(timingPath,timingValues);
                                                            //populateTq1Segment(orderTimingTq1,timingQuantity);
                                                            break;
                                                        case "timingQuantityRelationship":
                                                            List<Json> timingQuantityRelationshipList = multipleJsonPropertyParse(timingPath,timingValues);
                                                            for (int k = 0; k < timingQuantityRelationshipList.size(); k++) {
                                                                TQ2 orderTimingTq2 = (TQ2) timingGroup.get("TQ2",k);
                                                                Json timingQuantityRelationship = timingQuantityRelationshipList.get(k);
                                                                //populateTq2Segment(orderTimingTq2,timingQuantityRelationship);
                                                            }
                                                            break;
                                                        default:
                                                            throw EndpointException.permanent(ErrorCode.ARGUMENT,"The property ['"+key+"'] does not correspond with any possible TIMING segment");
                                                    }
                                                }
                                            } else {
                                                //populateTq1Segment(orderTimingTq1,timingValue);
                                            }
                                        }
                                        break;
                                    case "OBSERVATION_REQUEST":
                                        break;
                                    case "Financial Transaction":
                                        break;
                                    case "Clinical Trial Identification":
                                        break;
                                    case "Billing":
                                        break;
                                    default:
                                        throw EndpointException.permanent(ErrorCode.ARGUMENT,"The property ['"+key+"'] does not correspond with any possible ORDER segment");
                                }
                            }
                        } else {
                            populateOrcSegment(orc,order);
                        }
                    }
                    break;
                default:
                    throw EndpointException.permanent(ErrorCode.ARGUMENT,"The property ['"+key+"'] does not correspond with any possible HL7 segment");
            }
        }
    }
    /*
    public static void  populateGroup(AbstractGroup group,String propPath, String repeatability, Json groupValues) throws DataTypeException {
        String[] propPathSegments =propPath.split(".");
        String groupName = propPathSegments[propPathSegments.length-1];
        switch (groupName){
            case "ORDER":

        }
    }
    */

    public static void  populateMshSegment(MSH msh, Json mshValues) throws DataTypeException {
        //Populate MSH.3 "Sending Application" component
        msh.getSendingApplication().getNamespaceID().setValue(
                Hl7Endpoint.appName
        );
        for (String key: mshValues.keys()) {
            String propPath = "messageHeader.";
            String propValue = mshValues.string(key);

            switch (key){
                case "messageType":
                case "triggerEvent":
                    continue;

                case "sendingFacility":
                    Json sendingFacility = jsonOrValuePropertyParse(propPath, propValue);
                    populateHdField(msh.getSendingFacility(),sendingFacility);
                    break;

                case "receivingApplication":
                    Json receivingApplication = jsonOrValuePropertyParse(propPath, propValue);
                    populateHdField(msh.getReceivingApplication(),receivingApplication);
                    break;

                case "receivingFacility":
                    Json receivingFacility = jsonOrValuePropertyParse(propPath, propValue);
                    populateHdField(msh.getReceivingFacility(),receivingFacility);
                    break;
                case "security":
                    msh.getSecurity().setValue(propValue);
                    break;
                //THERE ARE SOME OTHER COMPONENTS HERE, WE SHOULD EVALUATE IF ADDING THEM IS NECESSARY OR NOT
                default:
                    throw EndpointException.permanent(ErrorCode.ARGUMENT,"The property ['"+key+"'] does not correspond with any possible MSH field");
            }
        }
    }

    public static void populateSftSegment(SFT sft, Json sftValues) throws DataTypeException {
        for (String key: sftValues.keys()) {
            String propPath = "softwareSegment.";
            String propValue = sftValues.string(key);

            switch (key){
                case "softwareVendorOrganization":
                    Json softwareVendorOrganization = jsonOrValuePropertyParse(propPath, propValue);
                    populateXonField(sft.getSoftwareVendorOrganization(),softwareVendorOrganization);
                    break;
                case "softwareCertifiedVersionOrReleaseNumber":
                    sft.getSoftwareCertifiedVersionOrReleaseNumber().setValue(propValue);
                    break;
                case "softwareProductName":
                    sft.getSoftwareProductName().setValue(propValue);
                    break;
                case "softwareBinaryID":
                    sft.getSoftwareBinaryID().setValue(propValue);
                    break;
                case "softwareProductInformation":
                    sft.getSoftwareProductInformation().setValue(propValue);
                    break;
                case "softwareInstallDate":
                    sft.getSoftwareInstallDate().setValue(propValue);
                default:
                    throw EndpointException.permanent(ErrorCode.ARGUMENT,"The property ['"+key+"'] does not correspond with any possible SFT field");
            }
        }
    }

    public static void  populateUacSegment(UAC uac, Json uacValues) throws DataTypeException {
        for (String key: uacValues.keys()) {
            String propPath = "userAuthenticationCredentialSegment."+key;
            String propValue = uacValues.string(key);

            switch (key){
                //Populate UAC.1 - User Authentication Credential Type Code
                case "userAuthenticationCredentialTypeCode":
                    Json userAuthenticationCredentialTypeCode = jsonOrValuePropertyParse(propPath, propValue);
                    populateCweField(uac.getUserAuthenticationCredentialTypeCode(),userAuthenticationCredentialTypeCode);
                    break;
                //Populate UAC.2 - User Authentication Credential
                case "userAuthenticationCredential":
                    Json userAuthenticationCredential = singleJsonPropertyParse(propPath, propValue);
                    populateEdField(uac.getUserAuthenticationCredential(),userAuthenticationCredential);
                    break;
                default:
                    throw EndpointException.permanent(ErrorCode.ARGUMENT,"The property ['"+key+"'] does not correspond with any possible UAC field");
            }
        }
    }

    public static void  populateNteSegment(NTE nte, Json nteValues) throws DataTypeException {
        for (String key: nteValues.keys()) {
            String propPath = "notesAndComments." + key;
            String propValue = nteValues.string(key);

            switch (key) {
                case "mainValue":
                case "setId":
                    nte.getSetIDNTE().setValue(propValue);
                    break;
                case "sourceOfComment":
                    nte.getSourceOfComment().setValue(propValue);
                    break;
                case "comment":
                    List<String> commentList = multipleValuesPropertyParse(propPath, propValue);
                    for (int i = 0; i < commentList.size(); i++) {
                        String operatorId = commentList.get(i);
                        nte.getComment(i).setValue(operatorId);
                    }
                    break;
                case "commentType":
                    Json commentType = jsonOrValuePropertyParse(propPath, propValue);
                    populateCweField(nte.getCommentType(), commentType);
                    break;
                case "enteredBy":
                    Json enteredBy = jsonOrValuePropertyParse(propPath, propValue);
                    populateXcnField(nte.getEnteredBy(),enteredBy);
                    break;
                case "enteredDateTime":
                    nte.getEnteredDateTime().setValue(propValue);
                    break;
                case "effectiveStartDate":
                    nte.getEffectiveStartDate().setValue(propValue);
                    break;
                case "expirationDate":
                    nte.getExpirationDate().setValue(propValue);
                    break;
                default:
                    throw EndpointException.permanent(ErrorCode.ARGUMENT,"The property ['"+key+"'] does not correspond with any possible ETN field");
            }
        }
    }

    public static void populateEvnSegment(EVN evn, Json evnValues) throws DataTypeException {
        for (String key : evnValues.keys()) {
            String propPath = "eventType."+key;
            String propValue = evnValues.string(key);

            switch (key){
                //EVN.1 - Event Type Code withdrawn
                //EVN.2 - Recorded Date/Time already populated
                //Populate EVN.3 - Date/Time Planned Event
                case "dateTimePlannedEvent":
                    evn.getDateTimePlannedEvent().setValue(propValue);
                    break;
                //Populate EVN.4 - Event Reason Code
                case "eventReasonCode":
                    Json eventReasonCode = jsonOrValuePropertyParse(propPath, propValue);
                    populateCweField(evn.getEventReasonCode(),eventReasonCode);
                    break;
                //Populate EVN.5 - Operator Id
                case "operatorId":
                    List<Json> operatorIdList = multipleJsonPropertyParse(propPath, propValue);
                    for (int i = 0; i < operatorIdList.size(); i++) {
                        Json operatorId = operatorIdList.get(i);
                        populateXcnField(evn.getOperatorID(i),operatorId);
                    }
                    break;
                //Populate EVN.6 - Event Occurred
                case "eventOccurred":
                    evn.getEventOccurred().setValue(propValue);
                    break;
                //Populate EVN.7 - Event Facility
                case "eventFacility":
                    Json eventFacility = jsonOrValuePropertyParse(propPath, propValue);
                    populateHdField(evn.getEventFacility(),eventFacility);
                    break;
                default:
                    throw EndpointException.permanent(ErrorCode.ARGUMENT,"The property ['"+key+"'] does not correspond with any possible EVN field");
            }
        }
    }

    public static void populatePidSegment(PID pid, Json pidValues) throws DataTypeException {
        System.out.println("Las keys son: "+pidValues.keys());
        for (String key: pidValues.keys()) {
            String propPath = "patientIdentification."+key;
            String propValue = pidValues.string(key);

            switch (key){
                //Populate PID.1 - Set Id - Pid component
                case "setId":
                    pid.getSetIDPID().setValue(propValue);
                    break;
                //PID.2 "Patient Id" Withdrawn
                //Populate PID.3 "Patient Identifier List" component, which is repeatable
                case "patientIdentifierList":
                    List<Json> patientIdentifierList = multipleJsonPropertyParse(propPath, propValue);
                    for (int i = 0; i<patientIdentifierList.size();i++) {
                        Json currentPatientIdentifier = patientIdentifierList.get(i);
                        populateCxField(pid.getPatientIdentifierList(i),currentPatientIdentifier);
                    }
                    break;
                //PID.4 "Alternate Patient Id - Pid" Withdrawn
                //Populate PID.5 "Patient Name" component, which is repeatable
                case "patientName":
                    List<Json> patientNameList = arrayPropertyToJson(propPath, propValue);
                    for (int i = 0; i<patientNameList.size();i++) {
                        Json patientName = patientNameList.get(i);
                        populateXpnField(pid.getPatientName(i),patientName);
                    }
                    break;
                //Populate PID.6 "Mother's Maiden Name" component, which is repeatable
                case "mothersMaidenName":
                    List<Json> mothersMaidenNameList = arrayPropertyToJson(propPath, propValue);
                    for (int i = 0; i < mothersMaidenNameList.size(); i++) {
                        Json currentMothersMaidenName = mothersMaidenNameList.get(i);
                        populateXpnField(pid.getMotherSMaidenName(i),currentMothersMaidenName);
                    }
                    break;
                //Populate PID.7 "Date/Time Of Birth" component
                case "dateTimeOfBirth":
                    pid.getDateTimeOfBirth().setValue(propValue);
                    break;
                //Populate PID.8 "Administrative Sex" component
                case "administrativeSex":
                    Json administrativeSex = jsonOrValuePropertyParse(propPath, propValue);
                    populateCweField(pid.getAdministrativeSex(),administrativeSex);
                    break;
                //PID.9 "Patient Alias" Withdrawn
                //Populate PID.10 "Race" component, which is repeatable
                case "race":
                    List<Json> raceList = arrayPropertyToJson(propPath, propValue);
                    for (int i = 0; i < raceList.size(); i++) {
                        Json currentRace = raceList.get(i);
                        populateCweField(pid.getRace(i),currentRace);
                    }
                    break;
                //Populate PID.11 "Patient Address" component, which is repeatable
                case "patientAddress":
                    List<Json> patientAddressList = arrayPropertyToJson(propPath, propValue);
                    for (int i = 0; i < patientAddressList.size(); i++) {
                        Json currentPatientAddress = patientAddressList.get(i);
                        populateXadField(pid.getPatientAddress(i),currentPatientAddress);
                    }
                    break;
                //PID.12 "County Code" Withdrawn
                //Populate PID.13 "Phone Number - Home" component, which is repeatable
                case "phoneNumberHome":
                    List<Json> patientPhoneNumberHomeList = multipleJsonPropertyParse(propPath, propValue);
                    for (int i = 0; i < patientPhoneNumberHomeList.size(); i++) {
                        Json currentPatientPhoneNumberHome = patientPhoneNumberHomeList.get(i);
                        populateXtnField(pid.getPhoneNumberHome(i),currentPatientPhoneNumberHome);
                    }
                    break;
                //Populate PID.14 "Phone Number - Business" component, which is repeatable
                case "phoneNumberBusiness":
                    List<Json> patientPhoneNumberBusinessList = multipleJsonPropertyParse(propPath, propValue);
                    for (int i = 0; i < patientPhoneNumberBusinessList.size(); i++) {
                        Json currentPatientPhoneNumberHome = patientPhoneNumberBusinessList.get(i);
                        populateXtnField(pid.getPhoneNumberBusiness(i),currentPatientPhoneNumberHome);
                    }
                    break;
                //Populate PID.15 "Primary Language" component
                case "primaryLanguage":
                    Json primaryLanguage = jsonOrValuePropertyParse(propPath, propValue);
                    populateCweField(pid.getPrimaryLanguage(),primaryLanguage);
                    break;
                //Populate PID.16 "Marital Status" component
                case "maritalStatus":
                    Json maritalStatus = jsonOrValuePropertyParse(propPath, propValue);
                    populateCweField(pid.getMaritalStatus(),maritalStatus);
                    break;
                //Populate PID.17 "Religion" component
                case "religion":
                    Json religion = jsonOrValuePropertyParse(propPath, propValue);
                    populateCweField(pid.getReligion(),religion);
                    break;
                //Populate PID.18 "Patient Account Number" component
                case "patientAccountNumber":
                    Json patientAccountNumber = singleJsonPropertyParse(propPath, propValue);
                    populateCxField(pid.getPatientAccountNumber(),patientAccountNumber);
                    break;
                //PID.19 "Ssn Number - Patient" Withdrawn
                //PID.20 "Driver's License Number - Patient" Withdrawn
                //Populate PID.21 "Mother's Identifier" component, which is repeatable
                case "mothersIdentifier":
                    List<Json> mothersIdentifierList = multipleJsonPropertyParse(propPath, propValue);
                    for (int i = 0; i < mothersIdentifierList.size(); i++) {
                        Json currentMothersIdentifier = mothersIdentifierList.get(i);
                        populateCxField(pid.getMotherSIdentifier(i),currentMothersIdentifier);
                    }
                    break;
                //Populate PID.22 "Ethnic Group" component, which is repeatable
                case "ethnicGroup":
                    List<Json> ethnicGroupList = arrayPropertyToJson(propPath, propValue);
                    for (int i = 0; i < ethnicGroupList.size(); i++) {
                        Json currentEthnicGroup = ethnicGroupList.get(i);
                        populateCweField(pid.getEthnicGroup(i),currentEthnicGroup);
                    }
                    break;
                //Populate PID.23 "Birth Place" component
                case "birthPlace":
                    pid.getBirthPlace().setValue(propValue);
                    break;
                //Populate PID.24 "Multiple Birth Indicator" component
                case "multipleBirthIndicator":
                    pid.getMultipleBirthIndicator().setValue(propValue);
                    break;
                //Populate PID.25 "Birth Order" component
                case "birthOrder":
                    pid.getBirthOrder().setValue(propValue);
                    break;
                //Populate PID.26 "Citizenship" component, which is repeatable
                case "citizenship":
                    List<Json> patientCitizenshipList = arrayPropertyToJson(propPath, propValue);
                    for (int i = 0; i < patientCitizenshipList.size(); i++) {
                        Json currentCitizenship = patientCitizenshipList.get(i);
                        populateCweField(pid.getCitizenship(i),currentCitizenship);
                    }
                    break;
                //Populate PID.27 "Veterans Military Status" component
                case "veteransMilitaryStatus":
                    Json veteransMilitaryStatus = jsonOrValuePropertyParse(propPath, propValue);
                    populateCweField(pid.getVeteransMilitaryStatus(),veteransMilitaryStatus);
                    break;
                //PID.28 "Nationality" Withdrawn
                //Populate PID.29 "Patient Death Date And Time" component
                case "patientDeathDateAndTime":
                    pid.getPatientDeathDateAndTime().setValue(propValue);
                    break;
                //Populate PID.30 "Patient Death Indicator" component
                case "patientDeathIndicator":
                    pid.getPatientDeathIndicator().setValue(propValue);
                    break;
                //Populate PID.31 "Identity Unknown Indicator" component
                case "identityUnknownIndicator":
                    pid.getIdentityUnknownIndicator().setValue(propValue);
                    break;
                //Populate PID.32 "Identity Reliability Code" component, which is repeatable
                case "identityReliabilityCode":
                    List<Json> identityReliabilityCodeList = arrayPropertyToJson(propPath, propValue);
                    for (int i = 0; i < identityReliabilityCodeList.size(); i++) {
                        Json currentIdentityReliabilityCode = identityReliabilityCodeList.get(i);
                        populateCweField(pid.getIdentityReliabilityCode(i),currentIdentityReliabilityCode);
                    }
                    break;
                //Populate PID.33 "Last Update Date/Time" component
                case "lastUpdateDateTime":
                    pid.getLastUpdateDateTime().setValue(propValue);
                    break;
                //Populate PID.34 "Last Update Facility" component
                case "lastUpdateFacility":
                    Json lastUpdateFacility = jsonOrValuePropertyParse(propPath, propValue);
                    populateHdField(pid.getLastUpdateFacility(),lastUpdateFacility);
                    break;
                //Populate PID.35 "Taxonomic Classification Code" component
                case "taxonomicClassificationCode":
                    Json taxonomicClassificationCode = jsonOrValuePropertyParse(propPath, propValue);
                    populateCweField(pid.getTaxonomicClassificationCode(),taxonomicClassificationCode);
                    break;
                //Populate PID.36 "Breed Code" component
                case "breedCode":
                    Json breedCode = jsonOrValuePropertyParse(propPath, propValue);
                    populateCweField(pid.getBreedCode(),breedCode);
                    break;
                //Populate PID.37 "Strain" component
                case "strain":
                    pid.getStrain().setValue(propValue);
                    break;
                //Populate PID.38 "Production Class Code" component
                case "productionClassCode":
                    Json productionClassCode = jsonOrValuePropertyParse(propPath, propValue);
                    populateCweField(pid.getProductionClassCode(),productionClassCode);
                    break;
                //Populate PID.39 "Tribal Citizenship" component, which is repeatable
                case "tribalCitizenship":
                    List<Json> tribalCitizenshipList = arrayPropertyToJson(propPath, propValue);
                    for (int i = 0; i < tribalCitizenshipList.size(); i++) {
                        Json currentTribalCitizenship = tribalCitizenshipList.get(i);
                        populateCweField(pid.getTribalCitizenship(i),currentTribalCitizenship);
                    }
                    break;
                //Populate PID.40 "Patient Telecommunication Information" component, which is repeatable
                case "patientTelecommunicationInformation":
                    List<Json> patientTelecommunicationInformationList = multipleJsonPropertyParse(propPath, propValue);
                    for (int i = 0; i < patientTelecommunicationInformationList.size(); i++) {
                        Json currentPatientTelecommunicationInformation = patientTelecommunicationInformationList.get(i);
                        populateXtnField(pid.getPatientTelecommunicationInformation(i),currentPatientTelecommunicationInformation);
                    }
                    break;
                default:
                    throw EndpointException.permanent(ErrorCode.ARGUMENT,"The property ['"+key+"'] does not correspond with any possible PID field");
            }
        }
    }

    public static void populatePd1Segment(PD1 pd1, Json pd1Values) throws DataTypeException {
        for (String key: pd1Values.keys()) {
            String propPath = "patientAdditionalDemographic.";
            String propValue = pd1Values.string(key);

            switch (key){
                case "livingDependency":
                    List<Json> livingDependencyList = arrayPropertyToJson(propPath, propValue);
                    for (int i = 0; i<livingDependencyList.size();i++) {
                        Json currentLivingDependency = livingDependencyList.get(i);
                        populateCweField(pd1.getLivingDependency(i),currentLivingDependency);
                    }
                    break;
                //Populate PD1.2 "Living Arrangement" component
                case "livingArrangement":
                    List<Json> livingArrangementList = arrayPropertyToJson(propPath, propValue);
                    Json livingArrangement = livingArrangementList.get(0);
                    populateCweField(pd1.getLivingArrangement(),livingArrangement);
                    break;
                //Populate PD1.3 "Patient Primary Facility" component, which is repeatable
                case "patientPrimaryFacility":
                    List<Json> patientPrimaryFacilityList = arrayPropertyToJson(propPath, propValue);
                    for (int i = 0; i<patientPrimaryFacilityList.size();i++) {
                        Json currentPatientPrimaryFacility = patientPrimaryFacilityList.get(i);
                        populateXonField(pd1.getPatientPrimaryFacility(i),currentPatientPrimaryFacility);
                    }
                    break;
                //Component PD1.4 Withdrawn
                //Populate PD1.5 "Student Indicator" component
                case "studentIndicator":
                    Json studentIndicator = jsonOrValuePropertyParse(propPath, propValue);
                    populateCweField(pd1.getStudentIndicator(),studentIndicator);
                    break;
                //Populate PD1.6 "Handicap" component
                case "handicap":
                    Json handicap = jsonOrValuePropertyParse(propPath, propValue);
                    populateCweField(pd1.getHandicap(),handicap);
                    break;
                //Populate PD1.7 "Living Will Code" component
                case "livingWillCode":
                    Json livingWillCode = jsonOrValuePropertyParse(propPath, propValue);
                    populateCweField(pd1.getLivingWillCode(),livingWillCode);
                    break;
                //Populate PD1.8 "Organ Donor Code" component
                case "organDonorCode":
                    Json organDonorCode = jsonOrValuePropertyParse(propPath, propValue);
                    populateCweField(pd1.getOrganDonorCode(),organDonorCode);
                    break;
                //Populate PD1.9 "Separate Bill" component
                case "separateBill":
                    pd1.getSeparateBill().setValue(propValue);
                    break;
                //Populate PD1.10 "Duplicate Patient" component, which is repeatable
                case "duplicatePatient":
                    List<Json> duplicatePatientList = multipleJsonPropertyParse(propPath, propValue);
                    for (int i = 0; i<duplicatePatientList.size();i++) {
                        Json currentDuplicatePatient = duplicatePatientList.get(i);
                        populateCxField(pd1.getDuplicatePatient(i),currentDuplicatePatient);
                    }
                    break;
                //Populate PD1.11 "Publicity Code" component
                case "publicityCode":
                    Json publicityCode = jsonOrValuePropertyParse(propPath, propValue);
                    populateCweField(pd1.getPublicityCode(),publicityCode);
                    break;
                //Populate PD1.12 "Protection Indicator" component
                case "protectionIndicator":
                    pd1.getProtectionIndicator().setValue(propValue);
                    break;
                //Populate PD1.13 "Protection Indicator Effective Date" component
                case "protectionIndicatorEffectiveDate":
                    pd1.getProtectionIndicatorEffectiveDate().setValue(propValue);
                    break;
                //Populate PD1.14 "Place Of Worship" component, which is repeatable
                case "placeOfWorship":
                    List<Json> placeOfWorshipList = arrayPropertyToJson(propPath, propValue);
                    for (int i = 0; i<placeOfWorshipList.size();i++) {
                        Json currentPlaceOfWorship= placeOfWorshipList.get(i);
                        populateXonField(pd1.getPlaceOfWorship(i),currentPlaceOfWorship);
                    }
                    break;
                //Populate PD1.15 "Advance Directive Code" component, which is repeatable
                case "advanceDirectiveCode":
                    List<Json> advanceDirectiveCodeList = arrayPropertyToJson(propPath, propValue);
                    for (int i = 0; i<advanceDirectiveCodeList.size();i++) {
                        Json currentAdvanceDirectiveCode = advanceDirectiveCodeList.get(i);
                        populateCweField(pd1.getAdvanceDirectiveCode(i),currentAdvanceDirectiveCode);
                    }
                    break;
                //Populate PD1.16 "Immunization Registry Status" component
                case "immunizationRegistryStatus":
                    Json immunizationRegistryStatus = jsonOrValuePropertyParse(propPath, propValue);
                    populateCweField(pd1.getImmunizationRegistryStatus(),immunizationRegistryStatus);
                    break;

                //Populate PD1.17 "Immunization Registry Status Effective Date" component
                case "immunizationRegistryStatusEffectiveDate":
                    pd1.getImmunizationRegistryStatusEffectiveDate().setValue(propValue);
                    break;
                //Populate PD1.18 "Publicity Code Effective Date" component
                case "publicityCodeEffectiveDate":
                    pd1.getPublicityCodeEffectiveDate().setValue(propValue);
                    break;
                //Populate PD1.19 "Military Branch" component
                case "militaryBranch":
                    Json militaryBranch = jsonOrValuePropertyParse(propPath, propValue);
                    populateCweField(pd1.getMilitaryBranch(),militaryBranch);
                    break;
                //Populate PD1.20 "Military Rank/Grade" component
                case "militaryRankGrade":
                    Json militaryRankGrade = jsonOrValuePropertyParse(propPath, propValue);
                    populateCweField(pd1.getMilitaryRankGrade(),militaryRankGrade);
                    break;
                //Populate PD1.21 "Military Status" component
                case "militaryStatus":
                    Json militaryStatus = jsonOrValuePropertyParse(propPath, propValue);
                    populateCweField(pd1.getMilitaryStatus(),militaryStatus);
                    break;
                //Populate PD1.22 "Advance Directive Last Verified Date" component
                case "advanceDirectiveLastVerifiedDate":
                    pd1.getAdvanceDirectiveLastVerifiedDate().setValue(propValue);
                    break;
                default:
                    throw EndpointException.permanent(ErrorCode.ARGUMENT,"The property ['"+key+"'] does not correspond with any possible PD1 field");
            }
        }
    }

    public static void populateRolSegment(ROL rol, Json rolValues) throws DataTypeException {
        for (String key: rolValues.keys()) {
            String propPath = "role."+key;
            String propValue = rolValues.string(key);

            switch (key){
                //Populate ROL.1 - Role Instance Id
                case "roleInstanceId":
                    Json roleInstanceId = jsonOrValuePropertyParse(propPath, propValue);
                    populateEiField(rol.getRoleInstanceID(),roleInstanceId);
                    break;
                //Populate ROL.2 "Action Code" component
                case "actionCode":
                    rol.getActionCode().setValue(propValue);
                    break;
                //Populate ROL.3 - Role-rol
                case "roleRol":
                    Json roleRol = jsonOrValuePropertyParse(propPath, propValue);
                    populateCweField(rol.getRoleROL(),roleRol);
                    break;
                //Populate ROL.4 - Role Person
                case "rolePerson":
                    List<Json> rolePersonList = arrayPropertyToJson(propPath, propValue);
                    for (int i = 0; i<rolePersonList.size();i++) {
                        Json rolePerson = rolePersonList.get(i);
                        populateXcnField(rol.getRolePerson(i),rolePerson);
                    }
                    break;
                //Populate ROL.5 "Role Begin Date/Time" component
                case "roleBeginDateTime":
                    rol.getRoleBeginDateTime().setValue(propValue);
                    break;
                //Populate ROL.6 "Role End Date/Time" component
                case "roleEndDateTime" :
                    rol.getRoleEndDateTime().setValue(propValue);
                    break;
                //Populate ROL.7 - Role Duration
                case "roleDuration":
                    Json roleDuration = jsonOrValuePropertyParse(propPath, propValue);
                    populateCweField(rol.getRoleDuration(),roleDuration);
                    break;
                //Populate ROL.8 - Role Action Reason
                case "roleActionReason":
                    Json roleActionReason = jsonOrValuePropertyParse(propPath, propValue);
                    populateCweField(rol.getRoleActionReason(),roleActionReason);
                    break;
                //Populate ROL.9 - Provider Type
                case "providerType":
                    List<Json> providerTypeList = arrayPropertyToJson(propPath, propValue);
                    for (int i = 0; i<providerTypeList.size();i++) {
                        Json providerType = providerTypeList.get(i);
                        populateCweField(rol.getProviderType(i),providerType);
                    }
                    break;
                //Populate ROL.10 - Organization Unit Type
                case "organizationUnitType":
                    Json organizationUnitType = jsonOrValuePropertyParse(propPath, propValue);
                    populateCweField(rol.getOrganizationUnitType(),organizationUnitType);
                    break;
                //Populate ROL.11 - Office/Home Address/Birthplace
                case "officeHomeAddressBirthplace":
                    List<Json> officeHomeAddressBirthplaceList = arrayPropertyToJson(propPath, propValue);
                    for (int i = 0; i<officeHomeAddressBirthplaceList.size();i++) {
                        Json officeHomeAddressBirthplace = officeHomeAddressBirthplaceList.get(i);
                        populateXadField(rol.getOfficeHomeAddressBirthplace(i),officeHomeAddressBirthplace);
                    }
                    break;
                //Populate ROL.12 - Phone
                case "phone":
                    List<Json> phoneList = arrayPropertyToJson(propPath, propValue);
                    for (int i = 0; i<phoneList.size();i++) {
                        Json phone = phoneList.get(i);
                        populateXtnField(rol.getPhone(i),phone);
                    }
                    break;
                //Populate ROL.13 - Person's Location
                case "personsLocation":
                    Json personsLocation = jsonOrValuePropertyParse(propPath, propValue);
                    populatePlField(rol.getPersonSLocation(),personsLocation);
                    break;
                //Populate ROL.14 - Organization
                case "organization":
                    Json organization = jsonOrValuePropertyParse(propPath, propValue);
                    populateXonField(rol.getOrganization(),organization);
                    break;
                default:
                    throw EndpointException.permanent(ErrorCode.ARGUMENT,"The property ['"+key+"'] does not correspond with any possible ROL field");
            }
        }
    }

    public static void populateNk1Segment(NK1 nk1, Json nk1Values) throws DataTypeException {
        for (String key: nk1Values.keys()) {
            String propPath = "nextOfKinAssociatedParties."+key;
            String propValue = nk1Values.string(key);

            switch (key){
                //Populate NK1.1 "Set Id - Nk1" component
                case "setId":
                    nk1.getSetIDNK1().setValue(propValue);
                    break;
                //Populate NK1.2 "Name" component, which is repeatable
                case "name":
                    List<Json> nextOfKinNameList = arrayPropertyToJson(propPath, propValue);
                    for (int i = 0; i < nextOfKinNameList.size(); i++){
                        Json currentNextOfKinName = nextOfKinNameList.get(i);
                        populateXpnField(nk1.getNK1Name(i),currentNextOfKinName);
                    }
                    break;
                //Populate NK1.3 "Relationship" component
                case "relationship":
                    Json relationship = jsonOrValuePropertyParse(propPath, propValue);
                    populateCweField(nk1.getRelationship(),relationship);
                    break;
                //Populate NK1.4 "Address" component, which is repeatable
                case "address":
                    List<Json> addressList = arrayPropertyToJson(propPath, propValue);
                    for (int i=0; i < addressList.size(); i++){
                        Json currentAddress = addressList.get(i);
                        populateXadField(nk1.getAddress(i),currentAddress);
                    }
                    break;
                //Populate NK1.5 "Phone Number" component, which is repeatable
                case "phoneNumber":
                    List<Json> phoneNumberList = multipleJsonPropertyParse(propPath, propValue);
                    for (int i=0; i < phoneNumberList.size(); i++){
                        Json currentPhoneNumber = phoneNumberList.get(i);
                        populateXtnField(nk1.getPhoneNumber(i),currentPhoneNumber);
                    }
                    break;
                //Populate NK1.6 "Business Phone Number" component, which is repeatable
                case "businessPhoneNumber":
                    List<Json> businessPhoneNumberList = multipleJsonPropertyParse(propPath, propValue);
                    for (int i=0; i < businessPhoneNumberList.size(); i++){
                        Json currentBusinessPhoneNumber = businessPhoneNumberList.get(i);
                        populateXtnField(nk1.getBusinessPhoneNumber(i),currentBusinessPhoneNumber);
                    }
                    break;
                //Populate NK1.7 "Contact Role" component
                case "contactRole":
                    Json contactRole = jsonOrValuePropertyParse(propPath, propValue);
                    populateCweField(nk1.getContactRole(),contactRole);
                    break;
                //Populate NK1.8 "Start Date" component
                case "startDate":
                    nk1.getStartDate().setValue(propValue);
                    break;
                //Populate NK1.9 "End Date" component
                case "endDate":
                    nk1.getEndDate().setValue(propValue);
                    break;
                //Populate NK1.10 "Next Of Kin / Associated Parties Job Title" component
                case "nextOfKinAssociatedPartiesJobTitle":
                    nk1.getNextOfKinAssociatedPartiesJobTitle().setValue(propValue);
                    break;
                //Populate NK1.11 "Next Of Kin / Associated Parties Job Code/Class" component
                case "nextOfKinAssociatedPartiesJobCodeClass":
                    Json nextOfKinAssociatedPartiesJobCodeClass = jsonOrValuePropertyParse(propPath, propValue);
                    populateJccField(nk1.getNextOfKinAssociatedPartiesJobCodeClass(),nextOfKinAssociatedPartiesJobCodeClass);
                    break;
                //Populate NK1.12 " Next Of Kin / Associated Parties Employee Number" component
                case "nextOfKinAssociatedPartiesEmployeeNumber":
                    Json nextOfKinAssociatedPartiesEmployeeNumber = singleJsonPropertyParse(propPath, propValue);
                    populateCxField(nk1.getNextOfKinAssociatedPartiesEmployeeNumber(),nextOfKinAssociatedPartiesEmployeeNumber);
                    break;
                //Populate NK1.13 "Organization Name - Nk1" component, which is repeatable
                case "organizationNameNK1":
                    List<Json> organizationNameNK1List = arrayPropertyToJson(propPath, propValue);
                    for (int i=0; i < organizationNameNK1List.size(); i++){
                        Json currentOrganizationNameNK1 = organizationNameNK1List.get(i);
                        populateXonField(nk1.getOrganizationNameNK1(i),currentOrganizationNameNK1);
                    }
                    break;
                //Populate NK1.14 "Marital Status" component
                case "maritalStatus":
                    Json maritalStatus = jsonOrValuePropertyParse(propPath, propValue);
                    populateCweField(nk1.getMaritalStatus(),maritalStatus);
                    break;
                //Populate NK1.15 "Administrative Sex" component
                case "administrativeSex":
                    Json administrativeSex = jsonOrValuePropertyParse(propPath, propValue);
                    populateCweField(nk1.getAdministrativeSex(),administrativeSex);
                    break;
                //Populate NK1.16 "Date/Time Of Birth" component
                case "dateTimeOfBirth":
                    nk1.getDateTimeOfBirth().setValue(propValue);
                    break;
                //Populate NK1.17 "Living Dependency" component, which is repeatable
                case "livingDependency":
                    List<Json> livingDependencyList = arrayPropertyToJson(propPath, propValue);
                    for (int i=0; i < livingDependencyList.size(); i++){
                        Json currentLivingDependency = livingDependencyList.get(i);
                        populateCweField(nk1.getLivingDependency(i),currentLivingDependency);
                    }
                    break;
                //Populate NK1.18 "Ambulatory Status" component, which is repeatable
                case "ambulatoryStatus":
                    List<Json> ambulatoryStatusList = arrayPropertyToJson(propPath, propValue);
                    for (int i=0; i < ambulatoryStatusList.size(); i++){
                        Json currentAmbulatoryStatus = ambulatoryStatusList.get(i);
                        populateCweField(nk1.getAmbulatoryStatus(i),currentAmbulatoryStatus);
                    }
                    break;
                //Populate NK1.19 "Citizenship" component, which is repeatable
                case "citizenship":
                    List<Json> citizenshipList = arrayPropertyToJson(propPath, propValue);
                    for (int i=0; i < citizenshipList.size(); i++){
                        Json currentCitizenship = citizenshipList.get(i);
                        populateCweField(nk1.getCitizenship(i),currentCitizenship);
                    }
                    break;
                //Populate NK1.20 "Primary Language" component
                case "primaryLanguage":
                    Json primaryLanguage = jsonOrValuePropertyParse(propPath, propValue);
                    populateCweField(nk1.getPrimaryLanguage(),primaryLanguage);
                    break;
                //Populate NK1.21 "Living Arrangement" component
                case "livingArrangement":
                    Json livingArrangement = jsonOrValuePropertyParse(propPath, propValue);
                    populateCweField(nk1.getLivingArrangement(),livingArrangement);
                    break;
                //Populate NK1.22 "Publicity Code" component
                case "publicityCode":
                    Json publicityCode = jsonOrValuePropertyParse(propPath, propValue);
                    populateCweField(nk1.getPublicityCode(),publicityCode);
                    break;
                //Populate NK1.23 "Protection Indicator" component
                case "protectionIndicator":
                    nk1.getProtectionIndicator().setValue(propValue);
                    break;
                //Populate NK1.24 "Student Indicator" component
                case "studentIndicator":
                    Json studentIndicator = jsonOrValuePropertyParse(propPath, propValue);
                    populateCweField(nk1.getStudentIndicator(),studentIndicator);
                    break;
                //Populate NK1.25 "Religion" component
                case "religion":
                    Json religion = jsonOrValuePropertyParse(propPath, propValue);
                    populateCweField(nk1.getReligion(),religion);
                    break;
                //Populate NK1.26 "Mother's Maiden Name" component, which is repeatable
                case "mothersMaidenName":
                    List<Json> mothersMaidenNameList = arrayPropertyToJson(propPath, propValue);
                    for (int i=0; i < mothersMaidenNameList.size(); i++){
                        Json currentMothersMaidenName = mothersMaidenNameList.get(i);
                        populateXpnField(nk1.getMotherSMaidenName(i),currentMothersMaidenName);
                    }
                    break;
                //Populate NK1.27 "Nationality" component
                case "nationality":
                    Json nationality = jsonOrValuePropertyParse(propPath, propValue);
                    populateCweField(nk1.getNationality(),nationality);
                    break;
                //Populate NK1.28 "Ethnic Group" component, which is repeatable
                case "ethnicGroup":
                    List<Json> ethnicGroupList = arrayPropertyToJson(propPath, propValue);
                    for (int i = 0; i < ethnicGroupList.size(); i++) {
                        Json currentEthnicGroup = ethnicGroupList.get(i);
                        populateCweField(nk1.getEthnicGroup(i),currentEthnicGroup);
                    }
                    break;
                //Populate NK1.29 "Contact Reason" component, which is repeatable
                case "contactReason":
                    List<Json> contactReasonList = arrayPropertyToJson(propPath, propValue);
                    for (int i = 0; i < contactReasonList.size(); i++) {
                        Json currentContactReason = contactReasonList.get(i);
                        populateCweField(nk1.getContactReason(i),currentContactReason);
                    }
                    break;
                //Populate NK1.30 "Contact Person's Name" component, which is repeatable
                case "contactPersonsName":
                    List<Json> contactPersonsNameList = arrayPropertyToJson(propPath, propValue);
                    for (int i = 0; i < contactPersonsNameList.size(); i++) {
                        Json currentContactPersonsName = contactPersonsNameList.get(i);
                        populateXpnField(nk1.getContactPersonSName(i),currentContactPersonsName);
                    }
                    break;
                //Populate NK1.31 "Contact Person's Telephone Number" component, which is repeatable
                case "contactPersonsTelephoneNumber":
                    List<Json> contactPersonsTelephoneNumberList = multipleJsonPropertyParse(propPath, propValue);
                    for (int i = 0; i < contactPersonsTelephoneNumberList.size(); i++) {
                        Json currentContactPersonsName = contactPersonsTelephoneNumberList.get(i);
                        populateXtnField(nk1.getContactPersonSTelephoneNumber(i),currentContactPersonsName);
                    }
                    break;
                //Populate NK1.32 "Contact Person's Address" component, which is repeatable
                case "contactPersonsAddress":
                    List<Json> contactPersonsAddressList = arrayPropertyToJson(propPath, propValue);
                    for (int i = 0; i < contactPersonsAddressList.size(); i++) {
                        Json currentContactPersonsAddress = contactPersonsAddressList.get(i);
                        populateXadField(nk1.getContactPersonSAddress(i),currentContactPersonsAddress);
                    }
                    break;
                //Populate NK1.33 "Next Of Kin/Associated Party's Identifiers" component, which is repeatable
                case "nextOfKinAssociatedPartysIdentifiers":
                    List<Json> nextOfKinAssociatedPartysIdentifiersList = multipleJsonPropertyParse(propPath, propValue);
                    for (int i = 0; i < nextOfKinAssociatedPartysIdentifiersList.size(); i++) {
                        Json currentNextOfKinAssociatedPartysIdentifiers = nextOfKinAssociatedPartysIdentifiersList.get(i);
                        populateCxField(nk1.getNextOfKinAssociatedPartySIdentifiers(i),currentNextOfKinAssociatedPartysIdentifiers);
                    }
                    break;
                //Populate NK1.34 "Job Status" component
                case "jobStatus":
                    Json jobStatus = jsonOrValuePropertyParse(propPath, propValue);
                    populateCweField(nk1.getJobStatus(),jobStatus);
                    break;
                //Populate NK1.35 "Race" component, which is repeatable
                case "race":
                    List<Json> raceList = arrayPropertyToJson(propPath, propValue);
                    for (int i = 0; i < raceList.size(); i++) {
                        Json currentRace = raceList.get(i);
                        populateCweField(nk1.getRace(i),currentRace);
                    }
                    break;
                //Populate NK1.36 "Handicap" component
                case "handicap":
                    Json handicap = jsonOrValuePropertyParse(propPath, propValue);
                    populateCweField(nk1.getHandicap(),handicap);
                    break;
                //Populate NK1.37 "Contact Person Social Security Number" component
                case "contactPersonSocialSecurityNumber":
                    nk1.getContactPersonSocialSecurityNumber().setValue(propValue);
                    break;
                //Populate NK1.38 "Next Of Kin Birth Place" component
                case "nextOfKinBirthPlace":
                    nk1.getNextOfKinBirthPlace().setValue(propValue);
                    break;
                //Populate NK1.39 "Vip Indicator" component
                case "vipIndicator":
                    Json vipIndicator = jsonOrValuePropertyParse(propPath, propValue);
                    populateCweField(nk1.getVIPIndicator(),vipIndicator);
                    break;
                //Populate NK1.40 "Next Of Kin Telecommunication Information" component
                case "nextOfKinTelecommunicationInformation":
                    Json nextOfKinTelecommunicationInformation = singleJsonPropertyParse(propPath, propValue);
                    populateXtnField(nk1.getNextOfKinTelecommunicationInformation(),nextOfKinTelecommunicationInformation);
                    break;
                //Populate NK1.41 "Contact Person's Telecommunication Information" component
                case "ContactPersonSTelecommunicationInformation":
                    Json ContactPersonSTelecommunicationInformation = singleJsonPropertyParse(propPath, propValue);
                    populateXtnField(nk1.getContactPersonSTelecommunicationInformation(),ContactPersonSTelecommunicationInformation);
                    break;
                default:
                    throw EndpointException.permanent(ErrorCode.ARGUMENT,"The property ['"+key+"'] does not correspond with any possible NK1 field");
            }
        }
    }

    public static void populatePv1Segment(PV1 pv1, Json pv1Values) throws DataTypeException {
        for (String key: pv1Values.keys()) {
            String propPath = "patientVisit." + key;
            String propValue = pv1Values.string(key);

            switch (key) {
                //Populate PV1.1 "Set Id - Pv1" component
                case "setId":
                    pv1.getSetIDPV1().setValue(propValue);
                    break;
                //Populate PV1.2 "Patient Class" component
                case "patientClass":
                    Json patientClass = jsonOrValuePropertyParse(propPath, propValue);
                    populateCweField(pv1.getPatientClass(),patientClass);
                    break;
                //Populate PV1.3 "Assigned Patient Location" component
                case "assignedPatientLocation":
                    Json assignedPatientLocation = jsonOrValuePropertyParse(propPath, propValue);
                    populatePlField(pv1.getAssignedPatientLocation(),assignedPatientLocation);
                    break;
                //Populate PV1.4 "Admission Type" component
                case "admissionType":
                    Json admissionType = jsonOrValuePropertyParse(propPath, propValue);
                    populateCweField(pv1.getAdmissionType(),admissionType);
                    break;
                //Populate PV1.5 "Preadmit Number" component
                case "preadmitNumber":
                    Json preadmitNumber = singleJsonPropertyParse(propPath, propValue);
                    populateCxField(pv1.getPreadmitNumber(),preadmitNumber);
                    break;
                //Populate PV1.6 "Prior Patient Location" component
                case "priorPatientLocation":
                    Json priorPatientLocation = jsonOrValuePropertyParse(propPath, propValue);
                    populatePlField(pv1.getPriorPatientLocation(),priorPatientLocation);
                    break;
                //Populate PV1.7 "Attending Doctor" component, which is repeatable
                case "attendingDoctor":
                    List<Json> attendingDoctorList = arrayPropertyToJson(propPath, propValue);
                    for (int i = 0; i < attendingDoctorList.size(); i++) {
                        Json attendingDoctor = attendingDoctorList.get(i);
                        populateXcnField(pv1.getAttendingDoctor(i),attendingDoctor);
                    }
                    break;
                //Populate PV1.8 "Referring Doctor" component, which is repeatable
                case "referringDoctor":
                    List<Json> referringDoctorList = arrayPropertyToJson(propPath, propValue);
                    for (int i = 0; i < referringDoctorList.size(); i++) {
                        Json referringDoctor = referringDoctorList.get(i);
                        populateXcnField(pv1.getReferringDoctor(i),referringDoctor);
                    }
                    break;
                //Populate PV1.9 "Consulting Doctor" component, which is repeatable
                case "consultingDoctor":
                    List<Json> consultingDoctorList = arrayPropertyToJson(propPath, propValue);
                    for (int i = 0; i < consultingDoctorList.size(); i++) {
                        Json consultingDoctor = consultingDoctorList.get(i);
                        populateXcnField(pv1.getConsultingDoctor(i),consultingDoctor);
                    }
                    break;
                //Populate PV1.10 "Hospital Service" component
                case "hospitalService":
                    Json hospitalService = jsonOrValuePropertyParse(propPath, propValue);
                    populateCweField(pv1.getHospitalService(),hospitalService);
                    break;
                //Populate PV1.11 "Temporary Location" component
                case "temporaryLocation":
                    Json temporaryLocation = jsonOrValuePropertyParse(propPath, propValue);
                    populatePlField(pv1.getTemporaryLocation(),temporaryLocation);
                    break;
                //Populate PV1.12 "Preadmit Test Indicator" component
                case "preadmitTestIndicator":
                    Json preadmitTestIndicator = jsonOrValuePropertyParse(propPath, propValue);
                    populateCweField(pv1.getPreadmitTestIndicator(),preadmitTestIndicator);
                    break;
                //Populate PV1.13 "Re-admission Indicator" component
                case "readmissionIndicator":
                    Json readmissionIndicator = jsonOrValuePropertyParse(propPath, propValue);
                    populateCweField(pv1.getReAdmissionIndicator(),readmissionIndicator);
                    break;
                //Populate PV1.14 "Admit Source" component
                case "admitSource":
                    Json admitSource = jsonOrValuePropertyParse(propPath, propValue);
                    populateCweField(pv1.getAdmitSource(),admitSource);
                    break;
                //Populate PV1.15 "Ambulatory Status" component, which is repeatable
                case "ambulatoryStatus":
                    List<Json> ambulatoryStatusList = arrayPropertyToJson(propPath, propValue);
                    for (int i = 0; i < ambulatoryStatusList.size(); i++) {
                        Json ambulatoryStatus = ambulatoryStatusList.get(i);
                        populateCweField(pv1.getAmbulatoryStatus(i),ambulatoryStatus);
                    }
                    break;
                //Populate PV1.16 "Vip Indicator" component
                case "vipIndicator":
                    Json vipIndicator = jsonOrValuePropertyParse(propPath, propValue);
                    populateCweField(pv1.getVIPIndicator(),vipIndicator);
                    break;
                //Populate PV1.17 "Admitting Doctor" component, which is repeatable
                case "admittingDoctor":
                    List<Json> admittingDoctorList = arrayPropertyToJson(propPath, propValue);
                    for (int i = 0; i < admittingDoctorList.size(); i++) {
                        Json admittingDoctor = admittingDoctorList.get(i);
                        populateXcnField(pv1.getAdmittingDoctor(i),admittingDoctor);
                    }
                    break;
                //Populate PV1.18 "Patient Type" component
                case "patientType":
                    Json patientType = jsonOrValuePropertyParse(propPath, propValue);
                    populateCweField(pv1.getPatientType(),patientType);
                    break;
                //Populate PV1.19 "Visit Number" component
                case "visitNumber":
                    Json visitNumber = singleJsonPropertyParse(propPath, propValue);
                    populateCxField(pv1.getVisitNumber(),visitNumber);
                    break;
                //Populate PV1.20 "Financial Class" component, which is repeatable
                case "financialClass":
                    List<Json> financialClassList = arrayPropertyToJson(propPath, propValue);
                    for (int i = 0; i < financialClassList.size(); i++) {
                        Json financialClass = financialClassList.get(i);
                        populateFcField(pv1.getFinancialClass(i),financialClass);
                    }
                    break;
                //Populate PV1.21 "Charge Price Indicator" component
                case "chargePriceIndicator":
                    Json chargePriceIndicator = jsonOrValuePropertyParse(propPath, propValue);
                    populateCweField(pv1.getChargePriceIndicator(),chargePriceIndicator);
                    break;
                //Populate PV1.22 "Courtesy Code" component
                case "courtesyCode":
                    Json courtesyCode = jsonOrValuePropertyParse(propPath, propValue);
                    populateCweField(pv1.getCourtesyCode(),courtesyCode);
                    break;
                //Populate PV1.23 "Credit Rating" component
                case "creditRating":
                    Json creditRating = jsonOrValuePropertyParse(propPath, propValue);
                    populateCweField(pv1.getCreditRating(),creditRating);
                    break;
                //Populate PV1.24 "Contract Code" component, which is repeatable
                case "contractCode":
                    List<Json> contractCodeList = arrayPropertyToJson(propPath, propValue);
                    for (int i = 0; i < contractCodeList.size(); i++) {
                        Json contractCode = contractCodeList.get(i);
                        populateCweField(pv1.getContractCode(i),contractCode);
                    }
                    break;
                //Populate PV1.25 "Contract Effective Date" component, which is repeatable
                case "contractEffectiveDate":
                    List<Json> contractEffectiveDateList = arrayPropertyToJson(propPath, propValue);
                    for (int i = 0; i < contractEffectiveDateList.size(); i++) {
                        Json contractEffectiveDate = contractEffectiveDateList.get(i);
                        pv1.getContractEffectiveDate(i).setValue(
                                contractEffectiveDate.contains("mainValue") ? contractEffectiveDate.string("mainValue") : ""
                        );
                    }
                    break;
                //Populate PV1.26 "Contract Amount" component, which is repeatable
                case "contractAmount":
                    List<Json> contractAmountList = arrayPropertyToJson(propPath, propValue);
                    for (int i = 0; i < contractAmountList.size(); i++) {
                        Json contractAmount = contractAmountList.get(i);
                        pv1.getContractAmount(i).setValue(
                                contractAmount.contains("mainValue") ? contractAmount.string("mainValue") : ""
                        );
                    }
                    break;
                //Populate PV1.27 "Contract Period" component, which is repeatable
                case "contractPeriod":
                    List<Json> contractPeriodList = arrayPropertyToJson(propPath, propValue);
                    for (int i = 0; i < contractPeriodList.size(); i++) {
                        Json contractPeriod = contractPeriodList.get(i);
                        pv1.getContractPeriod(i).setValue(
                                contractPeriod.contains("mainValue") ? contractPeriod.string("mainValue") : ""
                        );
                    }
                    break;
                //Populate PV1.28 "Interest Code" component
                case "interestCode":
                    Json interestCode = jsonOrValuePropertyParse(propPath, propValue);
                    populateCweField(pv1.getInterestCode(),interestCode);
                    break;
                //Populate PV1.29 "Transfer To Bad Debt Code" component
                case "transferToBadDebtCode":
                    Json transferToBadDebtCode = jsonOrValuePropertyParse(propPath, propValue);
                    populateCweField(pv1.getTransferToBadDebtCode(),transferToBadDebtCode);
                    break;
                //Populate PV1.30 "Transfer To Bad Debt Date" component
                case "transferToBadDebtDate":
                    pv1.getTransferToBadDebtDate().setValue(propValue);
                    break;
                //Populate PV1.31 "Bad Debt Agency Code" component
                case "badDebtAgencyCode":
                    Json badDebtAgencyCode = jsonOrValuePropertyParse(propPath, propValue);
                    populateCweField(pv1.getBadDebtAgencyCode(),badDebtAgencyCode);
                    break;
                //Populate PV1.32 "Bad Debt Transfer Amount" component
                case "badDebtTransferAmount":
                    pv1.getBadDebtTransferAmount().setValue(propValue);
                    break;
                //Populate PV1.33 "Bad Debt Recovery Amount" component
                case "badDebtRecoveryAmount":
                    pv1.getBadDebtRecoveryAmount().setValue(propValue);
                    break;
                //Populate PV1.34 "Delete Account Indicator" component
                case "deleteAccountIndicator":
                    Json deleteAccountIndicator = jsonOrValuePropertyParse(propPath, propValue);
                    populateCweField(pv1.getDeleteAccountIndicator(),deleteAccountIndicator);
                    break;
                //Populate PV1.35 "Delete Account Date" component
                case "deleteAccountDate":
                    pv1.getDeleteAccountDate().setValue(propValue);
                    break;
                //Populate PV1.36 "Discharge Disposition" component
                case "dischargeDisposition":
                    Json dischargeDisposition = jsonOrValuePropertyParse(propPath, propValue);
                    populateCweField(pv1.getDischargeDisposition(),dischargeDisposition);
                    break;
                //Populate PV1.37 "Discharged To Location" component
                case "dischargedToLocation":
                    Json dischargedToLocation = jsonOrValuePropertyParse(propPath, propValue);
                    populateDldField(pv1.getDischargedToLocation(),dischargedToLocation);
                    break;
                //Populate PV1.38 "Diet Type" component
                case "dietType":
                    Json dietType = jsonOrValuePropertyParse(propPath, propValue);
                    populateCweField(pv1.getDietType(),dietType);
                    break;
                //Populate PV1.39 "Servicing Facility" component
                case "servicingFacility":
                    Json servicingFacility = jsonOrValuePropertyParse(propPath, propValue);
                    populateCweField(pv1.getServicingFacility(),servicingFacility);
                    break;
                //Populate PV1.40 "Bed Status" withdrawn
                //Populate PV1.41 "Account Status" component
                case "accountStatus":
                    Json accountStatus = jsonOrValuePropertyParse(propPath, propValue);
                    populateCweField(pv1.getAccountStatus(),accountStatus);
                    break;
                //Populate PV1.42 "Pending Location" component
                case "pendingLocation":
                    Json pendingLocation = jsonOrValuePropertyParse(propPath, propValue);
                    populatePlField(pv1.getPendingLocation(),pendingLocation);
                    break;
                //Populate PV1.43 "Prior Temporary Location" component
                case "priorTemporaryLocation":
                    Json priorTemporaryLocation = jsonOrValuePropertyParse(propPath, propValue);
                    populatePlField(pv1.getPriorTemporaryLocation(),priorTemporaryLocation);
                    break;
                //Populate PV1.44 "Admit Date/Time" component
                case "admitDateTime":
                    pv1.getAdmitDateTime().setValue(propValue);
                    break;
                //Populate PV1.45 "Discharge Date/Time" component
                case "dischargeDateTime":
                    pv1.getDischargeDateTime().setValue(propValue);
                    break;
                //Populate PV1.46 "Current Patient Balance" component
                case "currentPatientBalance":
                    pv1.getCurrentPatientBalance().setValue(propValue);
                    break;
                //Populate PV1.47 "Total Charges" component
                case "totalCharges":
                    pv1.getTotalCharges().setValue(propValue);
                    break;
                //Populate PV1.48 "Total Adjustments" component
                case "totalAdjustments":
                    pv1.getTotalAdjustments().setValue(propValue);
                    break;
                //Populate PV1.49 "Total Payments" component
                case "totalPayments":
                    pv1.getTotalPayments().setValue(propValue);
                    break;
                //Populate PV1.50 "Alternate Visit Id" component
                case "alternateVisitId":
                    Json alternateVisitId = singleJsonPropertyParse(propPath, propValue);
                    populateCxField(pv1.getAlternateVisitID(0),alternateVisitId);
                    break;
                //Populate PV1.51 "Visit Indicator" component
                case "visitIndicator":
                    Json visitIndicator = jsonOrValuePropertyParse(propPath, propValue);
                    populateCweField(pv1.getVisitIndicator(),visitIndicator);
                    break;
                //Populate PV1.52 "Other Healthcare Provider" withdrawn
                //Populate PV1.53 "Service Episode Description" component
                case "serviceEpisodeDescription":
                    pv1.getServiceEpisodeDescription().setValue(propValue);
                    break;
                //Populate PV1.54 "Service Episode Identifier" component
                case "serviceEpisodeIdentifier":
                    Json serviceEpisodeIdentifier = singleJsonPropertyParse(propPath, propValue);
                    populateCxField(pv1.getServiceEpisodeIdentifier(),serviceEpisodeIdentifier);
                    break;
                default:
                    throw EndpointException.permanent(ErrorCode.ARGUMENT,"The property ['"+key+"'] does not correspond with any possible PV1 field");
            }
        }
    }

    public static void populatePv2Segment(PV2 pv2, Json pv2Values) throws DataTypeException {
        for (String key: pv2Values.keys()) {
            String propPath = "patientVisitAdditionalInformation."+key;
            String propValue = pv2Values.string(key);

            switch (key){
                //Populate PV2.1 "Prior Pending Location" component
                case "priorPendingLocation":
                    Json priorPendingLocation = jsonOrValuePropertyParse(propPath, propValue);
                    populatePlField(pv2.getPriorPendingLocation(),priorPendingLocation);
                    break;
                //Populate PV2.2 "Accommodation Code" component
                case "accommodationCode":
                    Json accommodationCode = jsonOrValuePropertyParse(propPath, propValue);
                    populateCweField(pv2.getAccommodationCode(),accommodationCode);
                    break;
                //Populate PV2.3 "Admit Reason" component
                case "admitReason":
                    Json admitReason = jsonOrValuePropertyParse(propPath, propValue);
                    populateCweField(pv2.getAdmitReason(),admitReason);
                    break;
                //Populate PV2.4 "Transfer Reason" component
                case "transferReason":
                    Json transferReason = jsonOrValuePropertyParse(propPath, propValue);
                    populateCweField(pv2.getTransferReason(),transferReason);
                    break;
                //Populate PV2.5 "Patient Valuables" component, which is repeatable
                case "patientValuables":
                    List<String> patientValuablesList = multipleValuesPropertyParse(propPath, propValue);
                    for (int i = 0; i < patientValuablesList.size(); i++) {
                        String patientValuables = patientValuablesList.get(i);
                        pv2.getPatientValuables(i).setValue(patientValuables);
                    }
                    break;
                //Populate PV2.6 "Patient Valuables Location" component
                case "patientValuablesLocation":
                    pv2.getPatientValuablesLocation().setValue(propValue);
                    break;
                //Populate PV2.7 "Visit User Code" component, which is repeatable
                case "visitUserCode":
                    List<Json> visitUserCodeList = arrayPropertyToJson(propPath, propValue);
                    for (int i = 0; i < visitUserCodeList.size(); i++) {
                        Json visitUserCode = visitUserCodeList.get(i);
                        populateCweField(pv2.getVisitUserCode(i),visitUserCode);
                    }
                    break;
                //Populate PV2.8 "Expected Admit Date/Time" component
                case "expectedAdmitDateTime":
                    pv2.getExpectedAdmitDateTime().setValue(propValue);
                    break;
                //Populate PV2.9 "Expected Discharge Date/Time" component
                case "expectedDischargeDateTime":
                    pv2.getExpectedDischargeDateTime().setValue(propValue);
                    break;
                //Populate PV2.10 "Estimated Length Of Inpatient Stay" component
                case "estimatedLengthOfInpatientStay":
                    pv2.getEstimatedLengthOfInpatientStay().setValue(propValue);
                    break;
                //Populate PV2.11 "Actual Length Of Inpatient Stay" component
                case "actualLengthOfInpatientStay":
                    pv2.getActualLengthOfInpatientStay().setValue(propValue);
                    break;
                //Populate PV2.12 "Visit Description" component
                case "visitDescription":
                    pv2.getVisitDescription().setValue(propValue);
                    break;
                //Populate PV2.13 "Referral Source Code" component
                case "referralSourceCode":
                    List<Json> referralSourceCodeList = arrayPropertyToJson(propPath, propValue);
                    for (int i = 0; i < referralSourceCodeList.size(); i++) {
                        Json referralSourceCode = referralSourceCodeList.get(i);
                        populateXcnField(pv2.getReferralSourceCode(i),referralSourceCode);
                    }
                    break;
                //Populate PV2.14 "Previous Service Date" component
                case "previousServiceDate":
                    pv2.getPreviousServiceDate().setValue(propValue);
                    break;
                //Populate PV2.15 "Employment Illness Related Indicator" component
                case "employmentIllnessRelatedIndicator":
                pv2.getEmploymentIllnessRelatedIndicator().setValue(propValue);
                //Populate PV2.16 "Purge Status Code" component
                case "purgeStatusCode":
                    Json purgeStatusCode = jsonOrValuePropertyParse(propPath, propValue);
                    populateCweField(pv2.getPurgeStatusCode(),purgeStatusCode);
                    break;
                //Populate PV2.17 "Purge Status Date" component
                case "purgeStatusDate":
                    pv2.getPurgeStatusDate().setValue(propValue);
                    break;
                //Populate PV2.18 "Special Program Code" component
                case "specialProgramCode":
                    Json specialProgramCode = jsonOrValuePropertyParse(propPath, propValue);
                    populateCweField(pv2.getSpecialProgramCode(),specialProgramCode);
                    break;
                //Populate PV2.19 "Retention Indicator" component
                case "retentionIndicator":
                    pv2.getRetentionIndicator().setValue(propValue);
                    break;
                //Populate PV2.20 "Expected Number Of Insurance Plans" component
                case "expectedNumberOfInsurancePlans":
                pv2.getExpectedNumberOfInsurancePlans().setValue(propValue);
                //Populate PV2.21 "Visit Publicity Code" component
                case "visitPublicityCode":
                    Json visitPublicityCode = jsonOrValuePropertyParse(propPath, propValue);
                    populateCweField(pv2.getVisitPublicityCode(),visitPublicityCode);
                    break;
                //Populate PV2.22 "Visit Protection Indicator" component
                case "visitProtectionIndicator":
                    pv2.getVisitProtectionIndicator().setValue(propValue);
                    break;
                //Populate PV2.23 "Clinic Organization Name" component
                case "clinicOrganizationName":
                    List<Json> clinicOrganizationNameList = arrayPropertyToJson(propPath, propValue);
                    for (int i = 0; i < clinicOrganizationNameList.size(); i++) {
                        Json clinicOrganizationName = clinicOrganizationNameList.get(i);
                        populateXonField(pv2.getClinicOrganizationName(i),clinicOrganizationName);
                    }
                    break;
                //Populate PV2.24 "Patient Status Code" component
                case "patientStatusCode":
                    Json patientStatusCode = jsonOrValuePropertyParse(propPath, propValue);
                    populateCweField(pv2.getPatientStatusCode(),patientStatusCode);
                    break;
                //Populate PV2.25 "Visit Priority Code" component
                case "visitPriorityCode":
                    Json visitPriorityCode = jsonOrValuePropertyParse(propPath, propValue);
                    populateCweField(pv2.getVisitPriorityCode(),visitPriorityCode);
                    break;
                //Populate PV2.26 "Previous Treatment Date" component
                case "previousTreatmentDate":
                    pv2.getPreviousTreatmentDate().setValue(propValue);
                    break;
                //Populate PV2.27 "Expected Discharge Disposition" component
                case "expectedDischargeDisposition":
                    Json expectedDischargeDisposition = jsonOrValuePropertyParse(propPath, propValue);
                    populateCweField(pv2.getExpectedDischargeDisposition(),expectedDischargeDisposition);
                    break;
                //Populate PV2.28 "Signature On File Date" component
                case "signatureOnFileDate":
                    pv2.getSignatureOnFileDate().setValue(propValue);
                    break;
                //Populate PV2.29 "First Similar Illness Date" component
                case "firstSimilarIllnessDate":
                    pv2.getFirstSimilarIllnessDate().setValue(propValue);
                    break;
                //Populate PV2.30 "Patient Charge Adjustment Code" component
                case "patientChargeAdjustmentCode":
                    Json patientChargeAdjustmentCode = jsonOrValuePropertyParse(propPath, propValue);
                    populateCweField(pv2.getPatientChargeAdjustmentCode(),patientChargeAdjustmentCode);
                    break;
                //Populate PV2.31 "Recurring Service Code" component
                case "recurringServiceCode":
                    Json recurringServiceCode = jsonOrValuePropertyParse(propPath, propValue);
                    populateCweField(pv2.getRecurringServiceCode(),recurringServiceCode);
                    break;
                //Populate PV2.32 "Billing Media Code" component
                case "billingMediaCode":
                    pv2.getBillingMediaCode().setValue(propValue);
                    break;
                //Populate PV2.33 "Expected Surgery Date And Time" component
                case "expectedSurgeryDateAndTime":
                    pv2.getExpectedSurgeryDateAndTime().setValue(propValue);
                    break;
                //Populate PV2.34 "Military Partnership Code" component
                case "militaryPartnershipCode":
                    pv2.getMilitaryPartnershipCode().setValue(propValue);
                    break;
                //Populate PV2.35 "Military Non-availability Code" component
                case "militaryNonAvailabilityCode":
                    pv2.getMilitaryNonAvailabilityCode().setValue(propValue);
                    break;
                //Populate PV2.36 "Newborn Baby Indicator" component
                case "newbornBabyIndicator":
                    pv2.getNewbornBabyIndicator().setValue(propValue);
                    break;
                //Populate PV2.37 "Baby Detained Indicator" component
                case "babyDetainedIndicator":
                    pv2.getBabyDetainedIndicator().setValue(propValue);
                    break;
                //Populate PV2.38 "Mode Of Arrival Code" component
                case "modeOfArrivalCode":
                    Json modeOfArrivalCode = jsonOrValuePropertyParse(propPath, propValue);
                    populateCweField(pv2.getModeOfArrivalCode(),modeOfArrivalCode);
                    break;
                //Populate PV2.39 "Recreational Drug Use Code" component
                case "recreationalDrugUseCode":
                    List<Json> recreationalDrugUseCodeList = arrayPropertyToJson(propPath, propValue);
                    for (int i = 0; i < recreationalDrugUseCodeList.size(); i++) {
                        Json recreationalDrugUseCode = recreationalDrugUseCodeList.get(i);
                        populateCweField(pv2.getRecreationalDrugUseCode(i),recreationalDrugUseCode);
                    }
                    break;
                //Populate PV2.40 "Admission Level Of Care Code" component
                case "admissionLevelOfCareCode":
                    Json admissionLevelOfCareCode = jsonOrValuePropertyParse(propPath, propValue);
                    populateCweField(pv2.getAdmissionLevelOfCareCode(),admissionLevelOfCareCode);
                    break;
                //Populate PV2.41 "Precaution Code" component
                case "precautionCode":
                    List<Json> precautionCodeList = arrayPropertyToJson(propPath, propValue);
                    for (int i = 0; i < precautionCodeList.size(); i++) {
                        Json precautionCode = precautionCodeList.get(i);
                        populateCweField(pv2.getPrecautionCode(i),precautionCode);
                    }
                    break;
                //Populate PV2.42 "Patient Condition Code" component
                case "patientConditionCode":
                    Json patientConditionCode = jsonOrValuePropertyParse(propPath, propValue);
                    populateCweField(pv2.getPatientConditionCode(),patientConditionCode);
                    break;
                //Populate PV2.43 "Living Will Code" component
                case "livingWillCode":
                    Json livingWillCode = jsonOrValuePropertyParse(propPath, propValue);
                    populateCweField(pv2.getLivingWillCode(),livingWillCode);
                    break;
                //Populate PV2.44 "Organ Donor Code" component
                case "organDonorCode":
                    Json organDonorCode = jsonOrValuePropertyParse(propPath, propValue);
                    populateCweField(pv2.getOrganDonorCode(),organDonorCode);
                    break;
                //Populate PV2.45 "Advance Directive Code" component
                case "advanceDirectiveCode":
                    List<Json> advanceDirectiveCodeList = arrayPropertyToJson(propPath, propValue);
                    for (int i = 0; i < advanceDirectiveCodeList.size(); i++) {
                        Json advanceDirectiveCode = advanceDirectiveCodeList.get(i);
                        populateCweField(pv2.getAdvanceDirectiveCode(i),advanceDirectiveCode);
                    }
                    break;
                //Populate PV2.46 "Patient Status Effective Date" component
                case "patientStatusEffectiveDate":
                    pv2.getPatientStatusEffectiveDate().setValue(propValue);
                    break;
                //Populate PV2.47 "Expected Loa Return Date/Time" component
                case "expectedLoaReturnDateTime":
                    pv2.getExpectedLOAReturnDateTime().setValue(propValue);
                    break;
                //Populate PV2.48 "Expected Pre-admission Testing Date/Time" component
                case "expectedPreAdmissionTestingDateTime":
                    pv2.getExpectedPreAdmissionTestingDateTime().setValue(propValue);
                    break;
                //Populate PV2.49 "Notify Clergy Code" component
                case "notifyClergyCode":
                    List<Json> notifyClergyCodeList = arrayPropertyToJson(propPath, propValue);
                    for (int i = 0; i < notifyClergyCodeList.size(); i++) {
                        Json notifyClergyCode = notifyClergyCodeList.get(i);
                        populateCweField(pv2.getNotifyClergyCode(i),notifyClergyCode);
                    }
                    break;
                //Populate PV2.50 "Advance Directive Last Verified Date" component
                case "advanceDirectiveLastVerifiedDate":
                    pv2.getAdvanceDirectiveLastVerifiedDate().setValue(propValue);
                    break;
                default:
                    throw EndpointException.permanent(ErrorCode.ARGUMENT,"The property ['"+key+"'] does not correspond with any possible PV1 field");
            }
        }
    }

    public static void populateDb1Segment(DB1 db1, Json db1Values) throws DataTypeException {
        for (String key : db1Values.keys()) {
            String propPath = "disability." + key;
            String propValue = db1Values.string(key);

            switch (key) {
                //Populate DB1.1 "Set Id - Db1" component
                case "setId":
                    db1.getSetIDDB1().setValue(propValue);
                    break;
                //Populate DB1.2 "Disabled Person Code" component
                case "disabledPersonCode":
                    Json disabledPersonCode = jsonOrValuePropertyParse(propPath, propValue);
                    populateCweField(db1.getDisabledPersonCode(), disabledPersonCode);
                    break;
                //Populate DB1.3 "Disabled Person Identifier" component
                case "disabledPersonIdentifier":
                    List<Json> disabledPersonIdentifierList = multipleJsonPropertyParse(propPath, propValue);
                    for (int i = 0; i < disabledPersonIdentifierList.size(); i++) {
                        Json disabledPersonIdentifier = disabledPersonIdentifierList.get(i);
                        populateCxField(db1.getDisabledPersonIdentifier(i), disabledPersonIdentifier);
                    }
                    break;
                //Populate DB1.4 "Disability Indicator" component
                case "disabilityIndicator":
                    db1.getDisabilityIndicator().setValue(propValue);
                    break;
                //Populate DB1.5 "Disability Start Date" component
                case "disabilityStartDate":
                    db1.getDisabilityStartDate().setValue(propValue);
                    break;
                //Populate DB1.6 "Disability End Date" component
                case "disabilityEndDate":
                    db1.getDisabilityEndDate().setValue(propValue);
                    break;
                //Populate DB1.1 "Disability Return To Work Date" component
                case "disabilityReturnToWorkDate":
                    db1.getDisabilityReturnToWorkDate().setValue(propValue);
                    break;
                //Populate DB1.1 "Disability Unable To Work Date" component
                case "disabilityUnableToWorkDate":
                    db1.getDisabilityUnableToWorkDate().setValue(propValue);
                    break;
                default:
                    throw EndpointException.permanent(ErrorCode.ARGUMENT, "The property ['" + key + "'] does not correspond with any possible DB1 field");
            }
        }
    }

    public static void populateObxSegment(AbstractMessage message, OBX obx, Json obxValues) throws DataTypeException {
        for (String key : obxValues.keys()) {
            String propPath = "observationResult." + key;
            String propValue = obxValues.string(key);

            switch (key) {
                //Populate OBX.1 "Set Id - Obx" component
                case "setId":
                    obx.getSetIDOBX().setValue(propValue);
                    break;
                //Populate OBX.2 "Value Type" component
                case "valueType":
                    obx.getValueType().setValue(propValue);
                    break;
                //Populate OBX.3 "Observation Identifier" component
                case "ObservationIdentifier":
                    Json observationIdentifier = jsonOrValuePropertyParse(propPath, propValue);
                    populateCweField(obx.getObservationIdentifier(),observationIdentifier);
                    break;
                //Populate OBX.4 "Observation Sub-id" component
                case "ObservationSubId":
                    obx.getObservationSubID().setValue(propValue);
                    break;
                //Populate OBX.5 "Observation Value" component, which is repeatable and depends on the field OBX.2
                case "observationValue":
                    List<Json> observationValueList = arrayPropertyToJson(propPath, propValue);
                    for (int i = 0; i < observationValueList.size(); i++) {
                        Json observationValue = observationValueList.get(i);
                        populateVariesDataType(message,obx.getValueType().getValue(),obx.getObservationValue(i),observationValue);
                    }
                    break;
                //Populate OBX.6 "Units" component
                case "units":
                    Json units = jsonOrValuePropertyParse(propPath, propValue);
                    populateCweField(obx.getUnits(),units);
                    break;
                //Populate OBX.7 "References Range" component
                case "referencesRange":
                    obx.getReferencesRange().setValue(propValue);
                    break;
                //Populate OBX.8 "Interpretation Codes" component, which is repeatable
                case "interpretationCodes":
                    List<Json> interpretationCodesList = arrayPropertyToJson(propPath, propValue);
                    for (int i = 0; i < interpretationCodesList.size(); i++) {
                        Json interpretationCodes = interpretationCodesList.get(i);
                        populateCweField(obx.getInterpretationCodes(i),interpretationCodes);
                    }
                    break;
                //Populate OBX.9 "Probability" component
                case "probability":
                    obx.getProbability().setValue(propValue);
                    break;
                //Populate OBX.10 "Nature Of Abnormal Test" component
                case "natureOfAbnormalTest":
                    List<String> natureOfAbnormalTestList = multipleValuesPropertyParse(propPath,propValue);
                    for (int i = 0; i < natureOfAbnormalTestList.size(); i++) {
                        String natureOfAbnormalTest = natureOfAbnormalTestList.get(i);
                        obx.getNatureOfAbnormalTest(i).setValue(propValue);
                    }
                    break;
                //Populate OBX.11 "Observation Result Status" component
                case "observationResultStatus":
                    obx.getObservationResultStatus().setValue(propValue);
                    break;
                //Populate OBX.12 "Effective Date Of Reference Range" component
                case "effectiveDateOfReferenceRange":
                    obx.getEffectiveDateOfReferenceRange().setValue(propValue);
                    break;
                //Populate OBX.13 "User Defined Access Checks" component
                case "userDefinedAccessChecks":
                    obx.getUserDefinedAccessChecks().setValue(propValue);
                    break;
                //Populate OBX.14 "Date/Time Of The Observation" component
                case "dateTimeOfTheObservation":
                    obx.getDateTimeOfTheObservation().setValue(propValue);
                    break;
                //Populate OBX.15 "Producer's Id" component
                case "producersId":
                    Json producersId = jsonOrValuePropertyParse(propPath, propValue);
                    populateCweField(obx.getProducerSID(),producersId);
                    break;
                //Populate OBX.16 "Responsible Observer" component
                case "responsibleObserver":
                    List<Json> responsibleObserverList = arrayPropertyToJson(propPath, propValue);
                    for (int i = 0; i < responsibleObserverList.size(); i++) {
                        Json responsibleObserver = responsibleObserverList.get(i);
                        populateXcnField(obx.getResponsibleObserver(i),responsibleObserver);
                    }
                    break;
                //Populate OBX.17 "Observation Method" component
                case "observationMethod":
                    List<Json> observationMethodList = arrayPropertyToJson(propPath, propValue);
                    for (int i = 0; i < observationMethodList.size(); i++) {
                        Json observationMethod = observationMethodList.get(i);
                        populateCweField(obx.getObservationMethod(i),observationMethod);
                    }
                    break;
                //Populate OBX.18 "Equipment Instance Identifier" component
                case "equipmentInstanceIdentifier":
                    List<Json> equipmentInstanceIdentifierList = arrayPropertyToJson(propPath, propValue);
                    for (int i = 0; i < equipmentInstanceIdentifierList.size(); i++) {
                        Json equipmentInstanceIdentifier = equipmentInstanceIdentifierList.get(i);
                        populateEiField(obx.getEquipmentInstanceIdentifier(i),equipmentInstanceIdentifier);
                    }
                    break;
                //Populate OBX.19 "Date/Time Of The Analysis" component
                case "dateTimeOfTheAnalysis":
                    obx.getDateTimeOfTheAnalysis().setValue(propValue);
                    break;
                //Populate OBX.20 "Observation Site" component
                case "observationSite":
                    List<Json> observationSiteList = arrayPropertyToJson(propPath, propValue);
                    for (int i = 0; i < observationSiteList.size(); i++) {
                        Json observationSite = observationSiteList.get(i);
                        populateCweField(obx.getObservationSite(i),observationSite);
                    }
                    break;
                //Populate OBX.21 "Observation Instance Identifier" component
                case "observationInstanceIdentifier":
                    Json observationInstanceIdentifier = jsonOrValuePropertyParse(propPath, propValue);
                    populateEiField(obx.getObservationInstanceIdentifier(),observationInstanceIdentifier);
                    break;
                //Populate OBX.22 "Mood Code" component
                case "moodCode":
                    Json moodCode = jsonOrValuePropertyParse(propPath, propValue);
                    populateCneField(obx.getMoodCode(),moodCode);
                    break;
                //Populate OBX.23 "Performing Organization Name" component
                case "performingOrganizationName":
                    Json performingOrganizationName = jsonOrValuePropertyParse(propPath, propValue);
                    populateXonField(obx.getPerformingOrganizationName(),performingOrganizationName);
                    break;
                //Populate OBX.24 "Performing Organization Address" component
                case "performingOrganizationAddress":
                    Json performingOrganizationAddress = jsonOrValuePropertyParse(propPath, propValue);
                    populateXadField(obx.getPerformingOrganizationAddress(),performingOrganizationAddress);
                    break;
                //Populate OBX.25 "Performing Organization Medical Director" component
                case "performingOrganizationMedicalDirector":
                    Json performingOrganizationMedicalDirector = jsonOrValuePropertyParse(propPath, propValue);
                    populateXcnField(obx.getPerformingOrganizationMedicalDirector(),performingOrganizationMedicalDirector);
                    break;
                //Populate OBX.26 "Patient Results Release Category" component
                case "patientResultsReleaseCategory":
                    obx.getPatientResultsReleaseCategory().setValue(propValue);
                    break;
                //Populate OBX.27 "Root Cause" component
                case "rootCause":
                    Json rootCause = jsonOrValuePropertyParse(propPath, propValue);
                    populateCweField(obx.getRootCause(),rootCause);
                    break;
                //Populate OBX.28 "Local Process Control" component
                case "localProcessControl":
                    List<Json> localProcessControlList = arrayPropertyToJson(propPath, propValue);
                    for (int i = 0; i < localProcessControlList.size(); i++) {
                        Json localProcessControl = localProcessControlList.get(i);
                        populateCweField(obx.getLocalProcessControl(i),localProcessControl);
                    }
                    break;
                default:
                    throw EndpointException.permanent(ErrorCode.ARGUMENT, "The property ['" + key + "'] does not correspond with any possible OBX field");
            }
        }
    }

    public static void populateAl1Segment(AL1 al1, Json al1Values) throws DataTypeException {
        for (String key : al1Values.keys()) {
            String propPath = "patientAllergyInformation." + key;
            String propValue = al1Values.string(key);

            switch (key) {
                //Populate AL1.1 "Set Id - Al1" component
                case "setId":
                    al1.getSetIDAL1().setValue(propValue);
                    break;
                //Populate AL1.2 "Allergen Type Code" component
                case "allergenTypeCode":
                    Json allergenTypeCode = jsonOrValuePropertyParse(propPath, propValue);
                    populateCweField(al1.getAllergenTypeCode(), allergenTypeCode);
                    break;
                //Populate AL1.3 "Allergen Code/Mnemonic/Description" component
                case "allergenCodeMnemonicDescription":
                    Json allergenCodeMnemonicDescription = jsonOrValuePropertyParse(propPath, propValue);
                    populateCweField(al1.getAllergenCodeMnemonicDescription(), allergenCodeMnemonicDescription);
                    break;
                //Populate AL1.4 "Allergy Severity Code" component
                case "allergySeverityCode":
                    Json allergySeverityCode = jsonOrValuePropertyParse(propPath, propValue);
                    populateCweField(al1.getAllergySeverityCode(), allergySeverityCode);
                    break;
                //Populate AL1.5 "Allergy Reaction Code" component
                case "allergyReactionCode":
                    List<String> allergyReactionCodeList = multipleValuesPropertyParse(propPath, propValue);
                    for (int i = 0; i < allergyReactionCodeList.size(); i++) {
                        String allergyReactionCode = allergyReactionCodeList.get(i);
                        al1.getAllergyReactionCode(i).setValue(allergyReactionCode);
                    }
                    break;
                //Populate AL1.6 "Identification Date" withdrawn
                default:
                    throw EndpointException.permanent(ErrorCode.ARGUMENT, "The property ['" + key + "'] does not correspond with any possible AL1 field");
            }
        }
    }

    public static void populateDg1Segment(DG1 dg1, Json dg1Values) throws DataTypeException {
        for (String key : dg1Values.keys()) {
            String propPath = "diagnosis." + key;
            String propValue = dg1Values.string(key);

            switch (key) {
                //Populate DG1.1 "Set Id - Dg1" component
                case "setId":
                    dg1.getSetIDDG1().setValue(propValue);
                    break;
                //DG1.2 "Diagnosis Coding Method" Withdrawn
                //Populate DG1.3 "Diagnosis Code - Dg1" component
                case "diagnosisCodeDg1":
                    Json diagnosisCodeDg1 = jsonOrValuePropertyParse(propPath, propValue);
                    populateCweField(dg1.getDiagnosisCodeDG1(), diagnosisCodeDg1);
                    break;
                //DG1.4 "Diagnosis Description" Withdrawn
                //Populate DG1.5 "Diagnosis Date/Time" component
                case "diagnosisDateTime":
                    dg1.getDiagnosisDateTime().setValue(propValue);
                    break;
                //Populate DG1.6 "Diagnosis Type" component
                case "diagnosisType":
                    Json diagnosisType = jsonOrValuePropertyParse(propPath, propValue);
                    populateCweField(dg1.getDiagnosisType(), diagnosisType);
                    break;
                //DG1.7 "Major Diagnostic Category" Withdrawn
                //DG1.8 "Diagnostic Related Group" Withdrawn
                //DG1.9 "Drg Approval Indicator" Withdrawn
                //DG1.10 "Drg Grouper Review Code" Withdrawn
                //DG1.11 "Outlier Type" Withdrawn
                //DG1.12 "Outlier Days" Withdrawn
                //DG1.13 "Outlier Cost" Withdrawn
                //DG1.14 "Grouper Version And Type" Withdrawn
                //Populate DG1.15 "Diagnosis Priority" component
                case "diagnosisPriority":
                    dg1.getDiagnosisPriority().setValue(propValue);
                    break;
                //Populate DG1.16 "Diagnosing Clinician" component
                case "diagnosingClinician":
                    List<Json> diagnosingClinicianList = arrayPropertyToJson(propPath, propValue);
                    for (int i = 0; i < diagnosingClinicianList.size(); i++) {
                        Json diagnosingClinician = diagnosingClinicianList.get(i);
                        populateXcnField(dg1.getDiagnosingClinician(i), diagnosingClinician);
                    }
                    break;
                //Populate DG1.17 "Diagnosis Classification" component
                case "diagnosisClassification":
                    Json diagnosisClassification = jsonOrValuePropertyParse(propPath, propValue);
                    populateCweField(dg1.getDiagnosisClassification(), diagnosisClassification);
                    break;
                //Populate DG1.18 "Confidential Indicator" component
                case "confidentialIndicator":
                    dg1.getConfidentialIndicator().setValue(propValue);
                    break;
                //Populate DG1.19 "Attestation Date/Time" component
                case "attestationDateTime":
                    dg1.getAttestationDateTime().setValue(propValue);
                    break;
                //Populate DG1.20 "Diagnosis Identifier" component
                case "diagnosisIdentifier":
                    Json diagnosisIdentifier = jsonOrValuePropertyParse(propPath, propValue);
                    populateEiField(dg1.getDiagnosisIdentifier(), diagnosisIdentifier);
                    break;
                //Populate DG1.21 "Diagnosis Action Code" component
                case "diagnosisActionCode":
                    dg1.getDiagnosisActionCode().setValue(propValue);
                    break;
                //Populate DG1.22 "Parent Diagnosis" component
                case "parentDiagnosis":
                    Json parentDiagnosis = jsonOrValuePropertyParse(propPath, propValue);
                    populateEiField(dg1.getParentDiagnosis(), parentDiagnosis);
                    break;
                //Populate DG1.23 "Drg Ccl Value Code" component
                case "drgCclValueCode":
                    Json drgCclValueCode = jsonOrValuePropertyParse(propPath, propValue);
                    populateCweField(dg1.getDRGCCLValueCode(), drgCclValueCode);
                    break;
                //Populate DG1.24 "Drg Grouping Usage" component
                case "drgGroupingUsage":
                    dg1.getDRGGroupingUsage().setValue(propValue);
                    break;
                //Populate DG1.25 "Drg Diagnosis Determination Status" component
                case "drgDiagnosisDeterminationStatus":
                    Json drgDiagnosisDeterminationStatus = jsonOrValuePropertyParse(propPath, propValue);
                    populateCweField(dg1.getDRGDiagnosisDeterminationStatus(), drgDiagnosisDeterminationStatus);
                    break;
                //Populate DG1.26 "Present On Admission (poa) Indicator" component
                case "presentOnAdmissionIndicator":
                    Json presentOnAdmissionIndicator = jsonOrValuePropertyParse(propPath, propValue);
                    populateCweField(dg1.getPresentOnAdmissionIndicator(), presentOnAdmissionIndicator);
                    break;
                default:
                    throw EndpointException.permanent(ErrorCode.ARGUMENT, "The property ['" + key + "'] does not correspond with any possible DG1 field");
            }
        }
    }

    public static void populateDrgSegment(DRG drg, Json drgValues) throws DataTypeException {
        for (String key : drgValues.keys()) {
            String propPath = "diagnosis." + key;
            String propValue = drgValues.string(key);

            switch (key) {
                //Populate DRG.1 - Diagnostic Related Group
                case "diagnosticRelatedGroup":
                    Json diagnosticRelatedGroup = jsonOrValuePropertyParse(propPath, propValue);
                    populateCneField(drg.getDiagnosticRelatedGroup(),diagnosticRelatedGroup);
                    break;
                //Populate DRG.2 - Drg Assigned Date/Time
                case "drgAssignedDateTime":
                    drg.getDRGAssignedDateTime().setValue(propValue);
                    break;
                //Populate DRG.3 - Drg Approval Indicator
                case "drgApprovalIndicator":
                    drg.getDRGApprovalIndicator().setValue(propValue);
                    break;
                //Populate DRG.4 - Drg Grouper Review Code
                case "drgGrouperReviewCode":
                    Json drgGrouperReviewCode = jsonOrValuePropertyParse(propPath, propValue);
                    populateCweField(drg.getDRGGrouperReviewCode(),drgGrouperReviewCode);
                    break;
                //Populate DRG.5 - Outlier Type
                case "outlierType":
                    Json outlierType = jsonOrValuePropertyParse(propPath, propValue);
                    populateCweField(drg.getOutlierType(),outlierType);
                    break;
                //Populate DRG.6 - Outlier Days
                case "outlierDays":
                    drg.getOutlierDays().setValue(propValue);
                    break;
                //Populate DRG.7 - Outlier Cost
                case "outlierCost":
                    Json outlierCost = jsonOrValuePropertyParse(propPath, propValue);
                    populateCpField(drg.getOutlierCost(),outlierCost);
                    break;
                //Populate DRG.8 - Drg Payor
                case "drgPayor":
                    Json drgPayor = jsonOrValuePropertyParse(propPath, propValue);
                    populateCweField(drg.getDRGPayor(),drgPayor);
                    break;
                //Populate DRG.9 - Outlier Reimbursement
                case "outlierReimbursement":
                    Json outlierReimbursement = jsonOrValuePropertyParse(propPath, propValue);
                    populateCpField(drg.getOutlierReimbursement(),outlierReimbursement);
                    break;
                //Populate DRG.10 - Confidential Indicator
                case "confidentialIndicator":
                    drg.getConfidentialIndicator().setValue(propValue);
                    break;
                //Populate DRG.11 - Drg Transfer Type
                case "drgTransferType":
                    Json drgTransferType = jsonOrValuePropertyParse(propPath, propValue);
                    populateCweField(drg.getDRGTransferType(),drgTransferType);
                    break;
                //Populate DRG.12 - Name Of Coder
                case "nameOfCoder":
                    Json nameOfCoder = jsonOrValuePropertyParse(propPath, propValue);
                    populateXpnField(drg.getNameOfCoder(),nameOfCoder);
                    break;
                //Populate DRG.13 - Grouper Status
                case "grouperStatus":
                    Json grouperStatus = jsonOrValuePropertyParse(propPath, propValue);
                    populateCweField(drg.getGrouperStatus(),grouperStatus);
                    break;
                //Populate DRG.14 - Pccl Value Code
                case "pcclValueCode":
                    Json pcclValueCode = jsonOrValuePropertyParse(propPath, propValue);
                    populateCweField(drg.getPCCLValueCode(),pcclValueCode);
                    break;
                //Populate DRG.15 - Effective Weight
                case "effectiveWeight":
                    drg.getEffectiveWeight().setValue(propValue);
                    break;
                //Populate DRG.16 - Monetary Amount
                case "monetaryAmount":
                    Json monetaryAmount = jsonOrValuePropertyParse(propPath, propValue);
                    populateMoField(drg.getMonetaryAmount(),monetaryAmount);
                    break;
                //Populate DRG.17 - Status Patient
                case "statusPatient":
                    Json statusPatient = jsonOrValuePropertyParse(propPath, propValue);
                    populateCweField(drg.getStatusPatient(),statusPatient);
                    break;
                //Populate DRG.18 - Grouper Software Name
                case "grouperSoftwareName":
                    drg.getGrouperSoftwareName().setValue(propValue);
                    break;
                //Populate DRG.19 - Grouper Software Version
                case "grouperSoftwareVersion":
                    drg.getGrouperSoftwareVersion().setValue(propValue);
                    break;
                //Populate DRG.20 - Status Financial Calculation
                case "statusFinancialCalculation":
                    Json statusFinancialCalculation = jsonOrValuePropertyParse(propPath, propValue);
                    populateCweField(drg.getStatusFinancialCalculation(),statusFinancialCalculation);
                    break;
                //Populate DRG.21 - Relative Discount/Surcharge
                case "relativeDiscountSurcharge":
                    Json relativeDiscountSurcharge = jsonOrValuePropertyParse(propPath, propValue);
                    populateMoField(drg.getRelativeDiscountSurcharge(),relativeDiscountSurcharge);
                    break;
                //Populate DRG.22 - Basic Charge
                case "basicCharge":
                    Json basicCharge = jsonOrValuePropertyParse(propPath, propValue);
                    populateMoField(drg.getBasicCharge(),basicCharge);
                    break;
                //Populate DRG.23 - Total Charge
                case "totalCharge":
                    Json totalCharge = jsonOrValuePropertyParse(propPath, propValue);
                    populateMoField(drg.getTotalCharge(),totalCharge);
                    break;
                //Populate DRG.24 - Discount/Surcharge
                case "discountSurcharge":
                    Json discountSurcharge = jsonOrValuePropertyParse(propPath, propValue);
                    populateMoField(drg.getDiscountSurcharge(),discountSurcharge);
                    break;
                //Populate DRG.25 - Calculated Days
                case "calculatedDays":
                    drg.getCalculatedDays().setValue(propValue);
                    break;
                //Populate DRG.26 - Status Gender
                case "statusGender":
                    Json statusGender = jsonOrValuePropertyParse(propPath, propValue);;
                    populateCweField(drg.getStatusGender(),statusGender);
                    break;
                //PopulateDRG.27 - Status Age
                case "statusAge":
                    Json statusAge = jsonOrValuePropertyParse(propPath, propValue);;
                    populateCweField(drg.getStatusAge(),statusAge);
               	break;
                //Populate DRG.28 - Status Length Of Stay
                case "statusLengthOfStay":
                    Json statusLengthOfStay = jsonOrValuePropertyParse(propPath, propValue);;
                    populateCweField(drg.getStatusLengthOfStay(),statusLengthOfStay);
               		break;
                //Populate DRG.29 - Status Same Day Flag
                case "statusSameDayFlag":
                    Json statusSameDayFlag = jsonOrValuePropertyParse(propPath, propValue);;
                    populateCweField(drg.getStatusSameDayFlag(),statusSameDayFlag);
               		break;
                //Populate DRG.30 - Status Separation Mode
                case "statusSeparationMode":
                    Json statusSeparationMode = jsonOrValuePropertyParse(propPath, propValue);;
                    populateCweField(drg.getStatusSeparationMode(),statusSeparationMode);
               		break;
                //Populate DRG.31 - Status Weight At Birth
                case "statusWeightAtBirth":
                    Json statusWeightAtBirth = jsonOrValuePropertyParse(propPath, propValue);;
                    populateCweField(drg.getStatusWeightAtBirth(),statusWeightAtBirth);
               		break;
                //Populate DRG.32 - Status Respiration Minutes
                case "statusRespirationMinutes":
                    Json statusRespirationMinutes = jsonOrValuePropertyParse(propPath, propValue);;
                    populateCweField(drg.getStatusRespirationMinutes(),statusRespirationMinutes);
               		break;
                //Populate DRG.33 - Status Admission
                case "statusAdmission":
                    Json statusAdmission = jsonOrValuePropertyParse(propPath, propValue);;
                    populateCweField(drg.getStatusAdmission(),statusAdmission);
               		break;
                default:
                    throw EndpointException.permanent(ErrorCode.ARGUMENT, "The property ['" + key + "'] does not correspond with any possible DRG field");
            }
        }
    }

    public static void populateIn1Segment(IN1 in1, Json in1Values) throws DataTypeException {
        for (String key : in1Values.keys()) {
            String propPath = "diagnosis." + key;
            String propValue = in1Values.string(key);

            switch (key) {
                //Populate IN1.1 "Set Id - In1" component
                case "setId":
                    in1.getSetIDIN1().setValue(propValue);
                    break;
                //Populate IN1.2 "Health Plan Id" component
                case "healthPlanId":
                    Json healthPlanId = jsonOrValuePropertyParse(propPath, propValue);
                    populateCweField(in1.getHealthPlanID(), healthPlanId);
                    break;
                //Populate IN1.3 "Insurance Company Id" component
                case "insuranceCompanyId":
                    List<Json> insuranceCompanyIdList = multipleJsonPropertyParse(propPath, propValue);
                    for (int i = 0; i < insuranceCompanyIdList.size(); i++) {
                        Json insuranceCompanyId = insuranceCompanyIdList.get(i);
                        populateCxField(in1.getInsuranceCompanyID(i), insuranceCompanyId);
                    }
                    break;
                //Populate IN1.4 "Insurance Company Name" component
                case "insuranceCompanyName":
                    List<Json> insuranceCompanyNameList = arrayPropertyToJson(propPath, propValue);
                    for (int i = 0; i < insuranceCompanyNameList.size(); i++) {
                        Json insuranceCompanyName = insuranceCompanyNameList.get(i);
                        populateXonField(in1.getInsuranceCompanyName(i), insuranceCompanyName);
                    }
                    break;
                //Populate IN1.5 "Insurance Company Address" component
                case "insuranceCompanyAddress":
                    List<Json> insuranceCompanyAddressList = arrayPropertyToJson(propPath, propValue);
                    for (int i = 0; i < insuranceCompanyAddressList.size(); i++) {
                        Json insuranceCompanyAddress = insuranceCompanyAddressList.get(i);
                        populateXadField(in1.getInsuranceCompanyAddress(i), insuranceCompanyAddress);
                    }
                    break;
                //Populate IN1.6 "Insurance Co Contact Person" component
                case "insuranceCoContactPerson":
                    List<Json> insuranceCoContactPersonList = arrayPropertyToJson(propPath, propValue);
                    for (int i = 0; i < insuranceCoContactPersonList.size(); i++) {
                        Json insuranceCoContactPerson = insuranceCoContactPersonList.get(i);
                        populateXpnField(in1.getInsuranceCoContactPerson(i), insuranceCoContactPerson);
                    }
                    break;
                //Populate IN1.7 "Insurance Co Phone Number" component
                case "insuranceCoPhoneNumber":
                    List<Json> insuranceCoPhoneNumberList = multipleJsonPropertyParse(propPath, propValue);
                    for (int i = 0; i < insuranceCoPhoneNumberList.size(); i++) {
                        Json insuranceCoPhoneNumber = insuranceCoPhoneNumberList.get(i);
                        populateXtnField(in1.getInsuranceCoPhoneNumber(i), insuranceCoPhoneNumber);
                    }
                    break;
                //Populate IN1.8 "Group Number" component
                case "groupNumber":
                    in1.getGroupNumber().setValue(propValue);
                    break;
                //Populate IN1.9 "Group Name" component
                case "groupName":
                    List<Json> groupNameList = arrayPropertyToJson(propPath, propValue);
                    for (int i = 0; i < groupNameList.size(); i++) {
                        Json groupName = groupNameList.get(i);
                        populateXonField(in1.getGroupName(i), groupName);
                    }
                    break;
                //Populate IN1.10 "Insured's Group Emp Id" component
                case "insuredsGroupEmpID":
                    List<Json> insuredSGroupEmpIDList = multipleJsonPropertyParse(propPath, propValue);
                    for (int i = 0; i < insuredSGroupEmpIDList.size(); i++) {
                        Json insuredSGroupEmpID = insuredSGroupEmpIDList.get(i);
                        populateCxField(in1.getInsuredSGroupEmpID(i), insuredSGroupEmpID);
                    }
                    break;
                //Populate IN1.11 "Insured's Group Emp Name" component
                case "insuredsGroupEmpName":
                    List<Json> insuredsGroupEmpNameList = arrayPropertyToJson(propPath, propValue);
                    for (int i = 0; i < insuredsGroupEmpNameList.size(); i++) {
                        Json insuredsGroupEmpName = insuredsGroupEmpNameList.get(i);
                        populateXonField(in1.getInsuredSGroupEmpName(i), insuredsGroupEmpName);
                    }
                    break;
                //Populate IN1.12 "Plan Effective Date" component
                case "planEffectiveDate":
                    in1.getPlanEffectiveDate().setValue(propValue);
                    break;
                //Populate IN1.13 "Plan Expiration Date" component
                case "planExpirationDate":
                    in1.getPlanExpirationDate().setValue(propValue);
                    break;
                //Populate IN1.14 "Authorization Information" component
                case "authorizationInformation":
                    Json authorizationInformation = jsonOrValuePropertyParse(propPath, propValue);
                    populateAuiField(in1.getAuthorizationInformation(), authorizationInformation);
                    break;
                //Populate IN1.15 "Plan Type" component
                case "planType":
                    Json planType = jsonOrValuePropertyParse(propPath, propValue);
                    populateCweField(in1.getPlanType(), planType);
                    break;
                //Populate IN1.16 "Name Of Insured" component
                case "nameOfInsured":
                    List<Json> nameOfInsuredList = arrayPropertyToJson(propPath, propValue);
                    for (int i = 0; i < nameOfInsuredList.size(); i++) {
                        Json nameOfInsured = nameOfInsuredList.get(i);
                        populateXpnField(in1.getNameOfInsured(i), nameOfInsured);
                    }
                    break;
                //Populate IN1.17 "Insured's Relationship To Patient" component
                case "insuredsRelationshipToPatient":
                    Json insuredsRelationshipToPatient = jsonOrValuePropertyParse(propPath, propValue);
                    populateCweField(in1.getInsuredSRelationshipToPatient(), insuredsRelationshipToPatient);
                    break;
                //Populate IN1.18 "Insured's Date Of Birth" component
                case "insuredsDateOfBirth":
                    in1.getInsuredSDateOfBirth().setValue(propValue);
                    break;
                //Populate IN1.19 "Insured's Address" component
                case "insuredsAddress":
                    List<Json> insuredsAddressList = arrayPropertyToJson(propPath, propValue);
                    for (int i = 0; i < insuredsAddressList.size(); i++) {
                        Json insuredsAddress = insuredsAddressList.get(i);
                        populateXadField(in1.getInsuredSAddress(i), insuredsAddress);
                    }
                    break;
                //Populate IN1.20 "Assignment Of Benefits" component
                case "assignmentOfBenefits":
                    Json assignmentOfBenefits = jsonOrValuePropertyParse(propPath, propValue);
                    populateCweField(in1.getAssignmentOfBenefits(), assignmentOfBenefits);
                    break;
                //Populate IN1.21 "Coordination Of Benefits" component
                case "coordinationOfBenefits":
                    Json coordinationOfBenefits = jsonOrValuePropertyParse(propPath, propValue);
                    populateCweField(in1.getCoordinationOfBenefits(), coordinationOfBenefits);
                    break;
                //Populate IN1.22 "Coord Of Ben. Priority" component
                case "coordOfBenPriority":
                    in1.getCoordOfBenPriority().setValue(propValue);
                    break;
                //Populate IN1.23 "Notice Of Admission Flag" component
                case "noticeOfAdmissionFlag":
                    in1.getNoticeOfAdmissionFlag().setValue(propValue);
                    break;
                //Populate IN1.24 "Notice Of Admission Date" component
                case "noticeOfAdmissionDate":
                    in1.getNoticeOfAdmissionDate().setValue(propValue);
                    break;
                //Populate IN1.25 "Report Of Eligibility Flag" component
                case "reportOfEligibilityFlag":
                    in1.getReportOfEligibilityFlag().setValue(propValue);
                    break;
                //Populate IN1.26 "Report Of Eligibility Date" component
                case "reportOfEligibilityDate":
                    in1.getReportOfEligibilityDate().setValue(propValue);
                    break;
                //Populate IN1.27 "Release Information Code" component
                case "releaseInformationCode":
                    Json releaseInformationCode = jsonOrValuePropertyParse(propPath, propValue);
                    populateCweField(in1.getReleaseInformationCode(), releaseInformationCode);
                    break;
                //Populate IN1.28 "Pre-admit Cert (pac)" component
                case "preAdmitCert":
                    in1.getPreAdmitCert().setValue(propValue);
                    break;
                //Populate IN1.29 "Verification Date/Time" component
                case "verificationDateTime":
                    in1.getVerificationDateTime().setValue(propValue);
                    break;
                //Populate IN1.30 "Verification By" component
                case "verificationBy":
                    List<Json> verificationByList = arrayPropertyToJson(propPath, propValue);
                    for (int i = 0; i < verificationByList.size(); i++) {
                        Json verificationBy = verificationByList.get(i);
                        populateXcnField(in1.getVerificationBy(i), verificationBy);
                    }
                    break;
                //Populate IN1.31 "Type Of Agreement Code" component
                case "typeOfAgreementCode":
                    Json typeOfAgreementCode = jsonOrValuePropertyParse(propPath, propValue);
                    populateCweField(in1.getTypeOfAgreementCode(), typeOfAgreementCode);
                    break;
                //Populate IN1.32 "Billing Status" component
                case "billingStatus":
                    Json billingStatus = jsonOrValuePropertyParse(propPath, propValue);
                    populateCweField(in1.getBillingStatus(), billingStatus);
                    break;
                //Populate IN1.33 "Lifetime Reserve Days" component
                case "lifetimeReserveDays":
                    in1.getLifetimeReserveDays().setValue(propValue);
                    break;
                //Populate IN1.34 "Delay Before L.r. Day" component
                case "delayBeforeLrDay":
                    in1.getDelayBeforeLRDay().setValue(propValue);
                    break;
                //Populate IN1.35 "Company Plan Code" component
                case "companyPlanCode":
                    Json companyPlanCode = jsonOrValuePropertyParse(propPath, propValue);
                    populateCweField(in1.getCompanyPlanCode(), companyPlanCode);
                    break;
                //Populate IN1.36 "Policy Number" component
                case "policyNumber":
                    in1.getPolicyNumber().setValue(propValue);
                    break;
                //Populate IN1.37 "Company Plan Code" component
                case "policyDeductible":
                    Json policyDeductible = jsonOrValuePropertyParse(propPath, propValue);
                    populateCpField(in1.getPolicyDeductible(), policyDeductible);
                    break;
                //Populate IN1.38 "Policy Limit - Amount" withdrawn
                //Populate IN1.39 "Policy Limit - Days" component
                case "policyLimitDays":
                    in1.getPolicyLimitDays().setValue(propValue);
                    break;
                //Populate IN1.40 "Room Rate - Semi-private" withdrawn
                //Populate IN1.41 "Room Rate - Private" withdrawn
                //Populate IN1.42 "Insured's Employment Status" component
                case "insuredsEmploymentStatus":
                    Json insuredsEmploymentStatus = jsonOrValuePropertyParse(propPath, propValue);
                    populateCweField(in1.getInsuredSEmploymentStatus(), insuredsEmploymentStatus);
                    break;
                //Populate IN1.43 "Insured's Administrative Sex" component
                case "insuredsAdministrativeSex":
                    Json insuredsAdministrativeSex = jsonOrValuePropertyParse(propPath, propValue);
                    populateCweField(in1.getInsuredSAdministrativeSex(), insuredsAdministrativeSex);
                    break;
                //Populate IN1.44 "Insured's Employer's Address" component
                case "insuredsEmployersAddress":
                    List<Json> insuredsEmployersAddressList = arrayPropertyToJson(propPath, propValue);
                    for (int i = 0; i < insuredsEmployersAddressList.size(); i++) {
                        Json insuredsEmployersAddress = insuredsEmployersAddressList.get(i);
                        populateXadField(in1.getInsuredSEmployerSAddress(i), insuredsEmployersAddress);
                    }
                    break;
                //Populate IN1.45 "Verification Status" component
                case "verificationStatus":
                    in1.getVerificationStatus().setValue(propValue);
                    break;
                //Populate IN1.46 "Prior Insurance Plan Id" component
                case "priorInsurancePlanId":
                    Json priorInsurancePlanId = jsonOrValuePropertyParse(propPath, propValue);
                    populateCweField(in1.getPriorInsurancePlanID(), priorInsurancePlanId);
                    break;
                //Populate IN1.47 "Coverage Type" component
                case "coverageType":
                    Json coverageType = jsonOrValuePropertyParse(propPath, propValue);
                    populateCweField(in1.getCoverageType(), coverageType);
                    break;
                //Populate IN1.48 "Handicap" component
                case "handicap":
                    Json handicap = jsonOrValuePropertyParse(propPath, propValue);
                    populateCweField(in1.getHandicap(), handicap);
                    break;
                //Populate IN1.49 "Insured's Id Number" component, which is repeatable
                case "insuredsIdNumber":
                    List<Json> insuredsIdNumberList = multipleJsonPropertyParse(propPath, propValue);
                    for (int i = 0; i < insuredsIdNumberList.size(); i++) {
                        Json insuredsIdNumber = insuredsIdNumberList.get(i);
                        populateCxField(in1.getInsuredSIDNumber(i), insuredsIdNumber);
                    }
                    break;
                //Populate IN1.50 "Signature Code" component
                case "signatureCode":
                    Json signatureCode = jsonOrValuePropertyParse(propPath, propValue);
                    populateCweField(in1.getSignatureCode(), signatureCode);
                    break;
                //Populate IN1.51 "Signature Code Date" component
                case "signatureCodeDate":
                    in1.getSignatureCodeDate().setValue(propValue);
                    break;
                //Populate IN1.52 "Insured's Birth Place" component
                case "insuredsBirthPlace":
                    in1.getInsuredSBirthPlace().setValue(propValue);
                    break;
                //Populate IN1.53 "Vip Indicator" component
                case "vipIndicator":
                    Json vipIndicator = jsonOrValuePropertyParse(propPath, propValue);
                    populateCweField(in1.getVIPIndicator(), vipIndicator);
                    break;
                //Populate IN1.54 "External Health Plan Identifiers" component, which is repeatable
                case "externalHealthPlanIdentifiers":
                    List<Json> externalHealthPlanIdentifiersList = multipleJsonPropertyParse(propPath, propValue);
                    for (int i = 0; i < externalHealthPlanIdentifiersList.size(); i++) {
                        Json externalHealthPlanIdentifiers = externalHealthPlanIdentifiersList.get(i);
                        populateCxField(in1.getExternalHealthPlanIdentifiers(i), externalHealthPlanIdentifiers);
                    }
                    break;
                //Populate IN1.55 "Insurance Action Code" component
                case "insuranceActionCode":
                    in1.getInsuranceActionCode().setValue(propValue);
                    break;
                default:
                    throw EndpointException.permanent(ErrorCode.ARGUMENT, "The property ['" + key + "'] does not correspond with any possible IN1 field");
            }
        }
    }

    public static void populateIn2Segment(IN2 in2, Json in2Values) throws DataTypeException {
        for (String key : in2Values.keys()) {
            String propPath = "insuranceAdditionalInformation." + key;
            String propValue = in2Values.string(key);

            switch (key) {
                //Populate IN2.1 "Insured's Employee Id" component, which is repeatable
                case "insuredsEmployeeId":
                    List<Json> insuredsEmployeeIdList = multipleJsonPropertyParse(propPath, propValue);
                    for (int i = 0; i < insuredsEmployeeIdList.size(); i++) {
                        Json insuredsEmployeeId = insuredsEmployeeIdList.get(i);
                        populateCxField(in2.getInsuredSEmployeeID(i), insuredsEmployeeId);
                    }
                    break;
                //Populate IN2.2 "Insured's Social Security Number" component
                case "insuredsSocialSecurityNumber":
                    in2.getInsuredSSocialSecurityNumber().setValue(propValue);
                    break;
                //Populate IN2.3 "Insured's Employer's Name And Id" component, which is repeatable
                case "insuredsEmployersNameAndId":
                    List<Json> insuredsEmployersNameAndIdList = arrayPropertyToJson(propPath, propValue);
                    for (int i = 0; i < insuredsEmployersNameAndIdList.size(); i++) {
                        Json insuredsEmployersNameAndId = insuredsEmployersNameAndIdList.get(i);
                        populateXcnField(in2.getInsuredSEmployerSNameAndID(i), insuredsEmployersNameAndId);
                    }
                    break;
                //Populate IN2.4 "Employer Information Data" component
                case "employerInformationData":
                    Json employerInformationData = jsonOrValuePropertyParse(propPath, propValue);
                    ;
                    populateCweField(in2.getEmployerInformationData(), employerInformationData);
                    break;
                //Populate IN2.5 "Mail Claim Party" component, which is repeatable
                case "mailClaimParty":
                    List<Json> mailClaimPartyList = arrayPropertyToJson(propPath, propValue);
                    for (int i = 0; i < mailClaimPartyList.size(); i++) {
                        Json mailClaimParty = mailClaimPartyList.get(i);
                        populateCweField(in2.getMailClaimParty(i), mailClaimParty);
                    }
                    break;
                //Populate IN2.6 "Medicare Health Ins Card Number" component
                case "medicareHealthInsCardNumber":
                    in2.getMedicareHealthInsCardNumber().setValue(propValue);
                    break;
                //Populate IN2.7 "Medicaid Case Name" component, which is repeatable
                case "medicaidCaseName":
                    List<Json> medicaidCaseNameList = arrayPropertyToJson(propPath, propValue);
                    for (int i = 0; i < medicaidCaseNameList.size(); i++) {
                        Json medicaidCaseName = medicaidCaseNameList.get(i);
                        populateXpnField(in2.getMedicaidCaseName(i), medicaidCaseName);
                    }
                    break;
                //Populate IN2.8 "Medicaid Case Number" component
                case "medicaidCaseNumber":
                    in2.getMedicaidCaseNumber().setValue(propValue);
                    break;
                //Populate IN2.9 "Military Sponsor Name" component, which is repeatable
                case "militarySponsorName":
                    List<Json> militarySponsorNameList = arrayPropertyToJson(propPath, propValue);
                    for (int i = 0; i < militarySponsorNameList.size(); i++) {
                        Json militarySponsorName = militarySponsorNameList.get(i);
                        populateXpnField(in2.getMilitarySponsorName(i), militarySponsorName);
                    }
                    break;
                //Populate IN2.10 "Military Id Number" component
                case "militaryIdNumber":
                    in2.getMilitaryIDNumber().setValue(propValue);
                    break;
                //Populate IN2.11 "Dependent Of Military Recipient" component
                case "dependentOfMilitaryRecipient":
                    Json dependentOfMilitaryRecipient = jsonOrValuePropertyParse(propPath, propValue);
                    populateCweField(in2.getDependentOfMilitaryRecipient(), dependentOfMilitaryRecipient);
                    break;
                //Populate IN2.12 "Military Organization" component
                case "militaryOrganization":
                    in2.getMilitaryOrganization().setValue(propValue);
                    break;
                //Populate IN2.13 "Military Station" component
                case "militaryStation":
                    in2.getMilitaryStation().setValue(propValue);
                    break;
                //Populate IN2.14 "Military Service" component
                case "militaryService":
                    Json militaryService = jsonOrValuePropertyParse(propPath, propValue);
                    populateCweField(in2.getMilitaryService(), militaryService);
                    break;
                //Populate IN2.15 "Military Rank/Grade" component
                case "militaryRankGrade":
                    Json militaryRankGrade = jsonOrValuePropertyParse(propPath, propValue);
                    populateCweField(in2.getMilitaryRankGrade(), militaryRankGrade);
                    break;
                //Populate IN2.16 "Military Status" component
                case "militaryStatus":
                    Json militaryStatus = jsonOrValuePropertyParse(propPath, propValue);
                    populateCweField(in2.getMilitaryStatus(), militaryStatus);
                    break;
                //Populate IN2.17 "Military Retire Date" component
                case "militaryRetireDate":
                    in2.getMilitaryRetireDate().setValue(propValue);
                    break;
                //Populate IN2.18 "Military Non-avail Cert On File" component
                case "militaryNonAvailCertOnFile":
                    in2.getMilitaryNonAvailCertOnFile().setValue(propValue);
                    break;
                //Populate IN2.19 "Baby Coverage" component
                case "babyCoverage":
                    in2.getBabyCoverage().setValue(propValue);
                    break;
                //Populate IN2.20 "Combine Baby Bill" component
                case "combineBabyBill":
                    in2.getCombineBabyBill().setValue(propValue);
                    break;
                //Populate IN2.21 "Blood Deductible" component
                case "bloodDeductible":
                    in2.getBloodDeductible().setValue(propValue);
                    break;
                //Populate IN2.22 "Special Coverage Approval Name" component, which is repeatable
                case "specialCoverageApprovalName":
                    List<Json> specialCoverageApprovalNameList = arrayPropertyToJson(propPath, propValue);
                    for (int i = 0; i < specialCoverageApprovalNameList.size(); i++) {
                        Json specialCoverageApprovalName = specialCoverageApprovalNameList.get(i);
                        populateXpnField(in2.getSpecialCoverageApprovalName(i), specialCoverageApprovalName);
                    }
                    break;
                //Populate IN2.23 "Special Coverage Approval Title" component
                case "specialCoverageApprovalTitle":
                    in2.getSpecialCoverageApprovalTitle().setValue(propValue);
                    break;
                //Populate IN2.24 "Non-covered Insurance Code" component, which is repeatable
                case "nonCoveredInsuranceCode":
                    List<Json> nonCoveredInsuranceCodeList = arrayPropertyToJson(propPath, propValue);
                    for (int i = 0; i < nonCoveredInsuranceCodeList.size(); i++) {
                        Json nonCoveredInsuranceCode = nonCoveredInsuranceCodeList.get(i);
                        populateCweField(in2.getNonCoveredInsuranceCode(i), nonCoveredInsuranceCode);
                    }
                    break;
                //Populate IN2.25 "Payor Id" component, which is repeatable
                case "payorId":
                    List<Json> payorIdList = multipleJsonPropertyParse(propPath, propValue);
                    for (int i = 0; i < payorIdList.size(); i++) {
                        Json payorId = payorIdList.get(i);
                        populateCxField(in2.getPayorID(i), payorId);
                    }
                    break;
                //Populate IN2.26 "Payor Subscriber Id" component, which is repeatable
                case "payorSubscriberId":
                    List<Json> payorSubscriberIdList = multipleJsonPropertyParse(propPath, propValue);
                    for (int i = 0; i < payorSubscriberIdList.size(); i++) {
                        Json payorSubscriberId = payorSubscriberIdList.get(i);
                        populateCxField(in2.getPayorSubscriberID(i), payorSubscriberId);
                    }
                    break;
                //Populate IN2.27 "Eligibility Source" component
                case "eligibilitySource":
                    Json eligibilitySource = jsonOrValuePropertyParse(propPath, propValue);
                    ;
                    populateCweField(in2.getEligibilitySource(), eligibilitySource);
                    break;
                //Populate IN2.28 "Room Coverage Type/Amount" component, which is repeatable
                case "roomCoverageTypeAmount":
                    List<Json> roomCoverageTypeAmountList = arrayPropertyToJson(propPath, propValue);
                    for (int i = 0; i < roomCoverageTypeAmountList.size(); i++) {
                        Json roomCoverageTypeAmount = roomCoverageTypeAmountList.get(i);
                        populateRmcField(in2.getRoomCoverageTypeAmount(i), roomCoverageTypeAmount);
                    }
                    break;
                //Populate IN2.29 "Policy Type/Amount" component, which is repeatable
                case "policyTypeAmount":
                    List<Json> policyTypeAmountList = arrayPropertyToJson(propPath, propValue);
                    for (int i = 0; i < policyTypeAmountList.size(); i++) {
                        Json policyTypeAmount = policyTypeAmountList.get(i);
                        populatePtaField(in2.getPolicyTypeAmount(i), policyTypeAmount);
                    }
                    break;
                //Populate IN2.30 "Daily Deductible" component
                case "dailyDeductible":
                    Json dailyDeductible = jsonOrValuePropertyParse(propPath, propValue);
                    ;
                    populateDdiField(in2.getDailyDeductible(), dailyDeductible);
                    break;
                //Populate IN2.31 "Living Dependency" component
                case "livingDependency":
                    Json livingDependency = jsonOrValuePropertyParse(propPath, propValue);
                    ;
                    populateCweField(in2.getLivingDependency(), livingDependency);
                    break;
                //Populate IN2.32 "Ambulatory Status" component, which is repeatable
                case "ambulatoryStatus":
                    List<Json> ambulatoryStatusList = arrayPropertyToJson(propPath, propValue);
                    for (int i = 0; i < ambulatoryStatusList.size(); i++) {
                        Json ambulatoryStatus = ambulatoryStatusList.get(i);
                        populateCweField(in2.getAmbulatoryStatus(i), ambulatoryStatus);
                    }
                    break;
                //Populate IN2.33 "Citizenship" component, which is repeatable
                case "citizenship":
                    List<Json> citizenshipList = arrayPropertyToJson(propPath, propValue);
                    for (int i = 0; i < citizenshipList.size(); i++) {
                        Json citizenship = citizenshipList.get(i);
                        populateCweField(in2.getCitizenship(i), citizenship);
                    }
                    break;
                //Populate IN2.34 "Primary Language" component
                case "primaryLanguage":
                    Json primaryLanguage = jsonOrValuePropertyParse(propPath, propValue);
                    populateCweField(in2.getPrimaryLanguage(), primaryLanguage);
                    break;
                //Populate IN2.35 "Living Arrangement" component
                case "livingArrangement":
                    Json livingArrangement = jsonOrValuePropertyParse(propPath, propValue);
                    populateCweField(in2.getLivingArrangement(), livingArrangement);
                    break;
                //Populate IN2.36 "Publicity Code" component
                case "publicityCode":
                    Json publicityCode = jsonOrValuePropertyParse(propPath, propValue);
                    populateCweField(in2.getPublicityCode(), publicityCode);
                    break;
                //Populate IN2.37 "Protection Indicator" component
                case "protectionIndicator":
                    in2.getProtectionIndicator().setValue(propValue);
                    break;
                //Populate IN2.38 "Student Indicator" component
                case "studentIndicator":
                    Json studentIndicator = jsonOrValuePropertyParse(propPath, propValue);
                    populateCweField(in2.getStudentIndicator(), studentIndicator);
                    break;
                //Populate IN2.39 "Religion" component
                case "religion":
                    Json religion = jsonOrValuePropertyParse(propPath, propValue);
                    populateCweField(in2.getReligion(), religion);
                    break;
                //Populate IN2.40 "Mother's Maiden Name" component, which is repeatable
                case "mothersMaidenName":
                    List<Json> mothersMaidenNameList = arrayPropertyToJson(propPath, propValue);
                    for (int i = 0; i < mothersMaidenNameList.size(); i++) {
                        Json mothersMaidenName = mothersMaidenNameList.get(i);
                        populateXpnField(in2.getMotherSMaidenName(i), mothersMaidenName);
                    }
                    break;
                //Populate IN2.41 "Nationality" component
                case "nationality":
                    Json nationality = jsonOrValuePropertyParse(propPath, propValue);
                    populateCweField(in2.getNationality(), nationality);
                    break;
                //Populate IN2.42 "Ethnic Group" component, which is repeatable
                case "ethnicGroup":
                    List<Json> ethnicGroupList = arrayPropertyToJson(propPath, propValue);
                    for (int i = 0; i < ethnicGroupList.size(); i++) {
                        Json ethnicGroup = ethnicGroupList.get(i);
                        populateCweField(in2.getEthnicGroup(i), ethnicGroup);
                    }
                    break;
                //Populate IN2.43 "Marital Status" component, which is repeatable
                case "maritalStatus":
                    List<Json> maritalStatusList = arrayPropertyToJson(propPath, propValue);
                    for (int i = 0; i < maritalStatusList.size(); i++) {
                        Json maritalStatus = maritalStatusList.get(i);
                        populateCweField(in2.getMaritalStatus(i), maritalStatus);
                    }
                    break;
                //Populate IN2.44 "Insured's Employment Start Date" component
                case "insuredsEmploymentStartDate":
                    in2.getInsuredSEmploymentStartDate().setValue(propValue);
                    break;
                case "employmentStopDate":
                    //Populate IN2.45 "Employment Stop Date" component
                    in2.getEmploymentStopDate().setValue(propValue);
                    break;
                case "jobTitle":
                    //Populate IN2.46 "Job Title" component
                    in2.getJobTitle().setValue(propValue);
                    break;
                //Populate IN2.47 "Job Code/Class" component
                case "jobCodeClass":
                    Json jobCodeClass = jsonOrValuePropertyParse(propPath, propValue);
                    populateJccField(in2.getJobCodeClass(), jobCodeClass);
                    break;
                //Populate IN2.48 "Job Status" component
                case "jobStatus":
                    Json jobStatus = jsonOrValuePropertyParse(propPath, propValue);
                    populateCweField(in2.getJobStatus(), jobStatus);
                    break;
                //Populate IN2.49 "Employer Contact Person Name" component, which is repeatable
                case "employerContactPersonName":
                    List<Json> employerContactPersonNameList = arrayPropertyToJson(propPath, propValue);
                    for (int i = 0; i < employerContactPersonNameList.size(); i++) {
                        Json employerContactPersonName = employerContactPersonNameList.get(i);
                        populateXpnField(in2.getEmployerContactPersonName(i), employerContactPersonName);
                    }
                    break;
                //Populate IN2.50 "Employer Contact Person Phone Number" component, which is repeatable
                case "employerContactPersonPhoneNumber":
                    List<Json> employerContactPersonPhoneNumberList = multipleJsonPropertyParse(propPath, propValue);
                    for (int i = 0; i < employerContactPersonPhoneNumberList.size(); i++) {
                        Json employerContactPersonPhoneNumber = employerContactPersonPhoneNumberList.get(i);
                        populateXtnField(in2.getEmployerContactPersonPhoneNumber(i), employerContactPersonPhoneNumber);
                    }
                    break;
                //Populate IN2.51 "Employer Contact Reason" component
                case "employerContactReason":
                    Json employerContactReason = jsonOrValuePropertyParse(propPath, propValue);
                    populateCweField(in2.getEmployerContactReason(), employerContactReason);
                    break;
                //Populate IN2.52 "Insured's Contact Person's Name" component, which is repeatable
                case "insuredsContactPersonsName":
                    List<Json> insuredsContactPersonsNameList = arrayPropertyToJson(propPath, propValue);
                    for (int i = 0; i < insuredsContactPersonsNameList.size(); i++) {
                        Json insuredsContactPersonsName = insuredsContactPersonsNameList.get(i);
                        populateXpnField(in2.getInsuredSContactPersonSName(i), insuredsContactPersonsName);
                    }
                    break;
                //Populate IN2.53 "Insured's Contact Person Phone Number" component, which is repeatable
                case "insuredsContactPersonPhoneNumber":
                    List<Json> insuredsContactPersonPhoneNumberList = multipleJsonPropertyParse(propPath, propValue);
                    for (int i = 0; i < insuredsContactPersonPhoneNumberList.size(); i++) {
                        Json insuredsContactPersonPhoneNumber = insuredsContactPersonPhoneNumberList.get(i);
                        populateXtnField(in2.getInsuredSContactPersonPhoneNumber(i), insuredsContactPersonPhoneNumber);
                    }
                    break;
                //Populate IN2.54 "Insured's Contact Person Reason" component, which is repeatable
                case "insuredsContactPersonReason":
                    List<Json> insuredsContactPersonReasonList = arrayPropertyToJson(propPath, propValue);
                    for (int i = 0; i < insuredsContactPersonReasonList.size(); i++) {
                        Json insuredsContactPersonReason = insuredsContactPersonReasonList.get(i);
                        populateCweField(in2.getInsuredSContactPersonReason(i), insuredsContactPersonReason);
                    }
                    break;
                //Populate IN2.55 "Relationship To The Patient Start Date" component
                case "relationshipToThePatientStartDate":
                    in2.getRelationshipToThePatientStartDate().setValue(propValue);
                    break;
                //Populate IN2.56 "Relationship To The Patient Stop Date" component, which is repeatable
                case "relationshipToThePatientStopDate":
                    List<Json> relationshipToThePatientStopDateList = arrayPropertyToJson(propPath, propValue);
                    for (int i = 0; i < relationshipToThePatientStopDateList.size(); i++) {
                        Json relationshipToThePatientStopDate = relationshipToThePatientStopDateList.get(i);
                        in2.getRelationshipToThePatientStopDate(i).setValue(
                                relationshipToThePatientStopDate.string("mainValue")
                        );
                    }
                    break;
                //Populate IN2.57 "Insurance Co Contact Reason" component
                case "insuranceCoContactReason":
                    Json insuranceCoContactReason = jsonOrValuePropertyParse(propPath, propValue);
                    populateCweField(in2.getInsuranceCoContactReason(), insuranceCoContactReason);
                    break;
                //Populate IN2.58 "Insurance Co Contact Phone Number" component
                case "insuranceCoContactPhoneNumber":
                    List<Json> insuranceCoContactPhoneNumberList = multipleJsonPropertyParse(propPath, propValue);
                    for (int i = 0; i < insuranceCoContactPhoneNumberList.size(); i++) {
                        Json insuranceCoContactPhoneNumber = insuranceCoContactPhoneNumberList.get(i);
                        populateXtnField(in2.getInsuranceCoContactPhoneNumber(i), insuranceCoContactPhoneNumber);
                    }
                    break;
                //Populate IN2.59 "Policy Scope" component
                case "policyScope":
                    Json policyScope = jsonOrValuePropertyParse(propPath, propValue);
                    populateCweField(in2.getPolicyScope(), policyScope);
                    break;
                //Populate IN2.60 "Policy Source" component
                case "policySource":
                    Json policySource = jsonOrValuePropertyParse(propPath, propValue);
                    populateCweField(in2.getPolicySource(), policySource);
                    break;
                //Populate IN2.61 "Patient Member Number" component
                case "patientMemberNumber":
                    Json patientMemberNumber = singleJsonPropertyParse(propPath, propValue);
                    populateCxField(in2.getPatientMemberNumber(), patientMemberNumber);
                    break;
                //Populate IN2.62 "Guarantor's Relationship To Insured" component
                case "guarantorsRelationshipToInsured":
                    Json guarantorsRelationshipToInsured = jsonOrValuePropertyParse(propPath, propValue);
                    populateCweField(in2.getGuarantorSRelationshipToInsured(), guarantorsRelationshipToInsured);
                    break;
                //Populate IN2.63 "Insured's Phone Number - Home" component
                case "insuredsPhoneNumberHome":
                    List<Json> insuredsPhoneNumberHomeList = multipleJsonPropertyParse(propPath, propValue);
                    for (int i = 0; i < insuredsPhoneNumberHomeList.size(); i++) {
                        Json insuredsPhoneNumberHome = insuredsPhoneNumberHomeList.get(i);
                        populateXtnField(in2.getInsuredSPhoneNumberHome(i), insuredsPhoneNumberHome);
                    }
                    break;
                //Populate IN2.64 "Insured's Employer Phone Number" component
                case "insuredsEmployerPhoneNumber":
                    List<Json> insuredsEmployerPhoneNumberList = multipleJsonPropertyParse(propPath, propValue);
                    for (int i = 0; i < insuredsEmployerPhoneNumberList.size(); i++) {
                        Json insuredsEmployerPhoneNumber = insuredsEmployerPhoneNumberList.get(i);
                        populateXtnField(in2.getInsuredSEmployerPhoneNumber(i), insuredsEmployerPhoneNumber);
                    }
                    break;
                //Populate IN2.65 "Military Handicapped Program" component
                case "militaryHandicappedProgram":
                    Json militaryHandicappedProgram = jsonOrValuePropertyParse(propPath, propValue);
                    ;
                    populateCweField(in2.getMilitaryHandicappedProgram(), militaryHandicappedProgram);
                    break;
                //Populate IN2.66 "Suspend Flag" component
                case "suspendFlag":
                    in2.getSuspendFlag().setValue(propValue);
                    break;
                //Populate IN2.67 "Copay Limit Flag" component
                case "copayLimitFlag":
                    in2.getCopayLimitFlag().setValue(propValue);
                    break;
                //Populate IN2.68 "Stoploss Limit Flag" component
                case "stoplossLimitFlag":
                    in2.getStoplossLimitFlag().setValue(propValue);
                    break;
                //Populate IN2.69 "Insured Organization Name And Id" component
                case "insuredOrganizationNameAndId":
                    List<Json> insuredOrganizationNameAndIdList = arrayPropertyToJson(propPath, propValue);
                    for (int i = 0; i < insuredOrganizationNameAndIdList.size(); i++) {
                        Json insuredOrganizationNameAndId = insuredOrganizationNameAndIdList.get(i);
                        populateXonField(in2.getInsuredOrganizationNameAndID(i), insuredOrganizationNameAndId);
                    }
                    break;
                //Populate IN2.70 "Insured Employer Organization Name And Id" component
                case "insuredEmployerOrganizationNameAndId":
                    List<Json> insuredEmployerOrganizationNameAndIdList = arrayPropertyToJson(propPath, propValue);
                    for (int i = 0; i < insuredEmployerOrganizationNameAndIdList.size(); i++) {
                        Json insuredEmployerOrganizationNameAndId = insuredEmployerOrganizationNameAndIdList.get(i);
                        populateXonField(in2.getInsuredEmployerOrganizationNameAndID(i), insuredEmployerOrganizationNameAndId);
                    }
                    break;
                //Populate IN2.71 "Race" component
                case "race":
                    List<Json> raceList = arrayPropertyToJson(propPath, propValue);
                    for (int i = 0; i < raceList.size(); i++) {
                        Json race = raceList.get(i);
                        populateCweField(in2.getRace(i), race);
                    }
                    break;
                //Populate IN2.72 "Patient's Relationship To Insured" component
                case "patientsRelationshipToInsured":
                    Json patientsRelationshipToInsured = jsonOrValuePropertyParse(propPath, propValue);
                    populateCweField(in2.getPatientSRelationshipToInsured(), patientsRelationshipToInsured);
                    break;
            }
        }
    }

    public static void populateIn3Segment(IN3 in3, Json in3Values) throws DataTypeException {
        for (String key : in3Values.keys()) {
            String propPath = "insuranceAdditionalInformationCertification." + key;
            String propValue = in3Values.string(key);

            switch (key) {
                //Populate IN3.1 - Set Id - In3
                case "setId":
                    in3.getSetIDIN3().setValue(propValue);
                    break;
                //Populate IN3.2 - Certification Number
                case "certificationNumber":
                    Json certificationNumber = singleJsonPropertyParse(propPath, propValue);
                    populateCxField(in3.getCertificationNumber(), certificationNumber);
                    break;
                //Populate IN3.3 - Certified By
                case "certifiedBy":
                    List<Json> certifiedByList = arrayPropertyToJson(propPath, propValue);
                    for (int i = 0; i < certifiedByList.size(); i++) {
                        Json certifiedBy = certifiedByList.get(i);
                        populateXcnField(in3.getCertifiedBy(i), certifiedBy);
                    }
                    break;
                //Populate IN3.4 - Certification Required
                case "certificationRequired":
                    in3.getCertificationRequired().setValue(propValue);
                    break;
                //Populate IN3.5 - Penalty
                case "penalty":
                    Json penalty = singleJsonPropertyParse(propPath, propValue);
                    populateMopField(in3.getPenalty(), penalty);
                    break;
                //Populate IN3.6 - Certification Date/Time
                case "certificationDateTime":
                    in3.getCertificationDateTime().setValue(propValue);
                    break;
                //Populate IN3.7 - Certification Modify Date/Time
                case "certificationModifyDateTime":
                    in3.getCertificationModifyDateTime().setValue(propValue);
                    break;
                //Populate IN3.8 - Operator
                case "operator":
                    List<Json> operatorList = arrayPropertyToJson(propPath, propValue);
                    for (int i = 0; i < operatorList.size(); i++) {
                        Json operator = operatorList.get(i);
                        populateXcnField(in3.getOperator(i), operator);
                    }
                    break;
                //Populate IN3.9 - Certification Begin Date
                case "certificationBeginDate":
                    in3.getCertificationBeginDate().setValue(propValue);
                    break;
                //Populate IN3.10 - Certification End Date
                case "certificationEndDate":
                    in3.getCertificationEndDate().setValue(propValue);
                    break;
                //Populate IN3.11 - Days
                case "days":
                    Json days = singleJsonPropertyParse(propPath, propValue);
                    populateDtnField(in3.getDays(), days);
                    break;
                //Populate IN3.12 - Non-concur Code/Description
                case "nonConcurCodeDescription":
                    Json nonConcurCodeDescription = jsonOrValuePropertyParse(propPath, propValue);
                    ;
                    populateCweField(in3.getNonConcurCodeDescription(), nonConcurCodeDescription);
                    break;
                //Populate IN3.13 - Non-concur Effective Date/Time
                case "nonConcurEffectiveDateTime":
                    in3.getNonConcurEffectiveDateTime().setValue(propValue);
                    break;
                //Populate IN3.14 - Physician Reviewer
                case "physicianReviewer":
                    List<Json> physicianReviewerList = arrayPropertyToJson(propPath, propValue);
                    for (int i = 0; i < physicianReviewerList.size(); i++) {
                        Json physicianReviewer = physicianReviewerList.get(i);
                        populateXcnField(in3.getPhysicianReviewer(i), physicianReviewer);
                    }
                    break;
                //Populate IN3.15 - Certification Contact
                case "certificationContact":
                    in3.getCertificationContact().setValue(propValue);
                    break;
                //Populate IN3.16 - Certification Contact Phone Number
                case "certificationContactPhoneNumber":
                    List<Json> certificationContactPhoneNumberList = multipleJsonPropertyParse(propPath, propValue);
                    for (int i = 0; i < certificationContactPhoneNumberList.size(); i++) {
                        Json certificationContactPhoneNumber = certificationContactPhoneNumberList.get(i);
                        populateXtnField(in3.getCertificationContactPhoneNumber(i), certificationContactPhoneNumber);
                    }
                    break;
                //Populate IN3.17 - Appeal Reason
                case "appealReason":
                    Json appealReason = jsonOrValuePropertyParse(propPath, propValue);
                    ;
                    populateCweField(in3.getNonConcurCodeDescription(), appealReason);
                    break;
                //Populate IN3.18 - Certification Agency
                case "certificationAgency":
                    Json certificationAgency = jsonOrValuePropertyParse(propPath, propValue);
                    populateCweField(in3.getCertificationAgency(), certificationAgency);
                    break;
                //Populate IN3.19 - Certification Agency Phone Number
                case "certificationAgencyPhoneNumber":
                    List<Json> certificationAgencyPhoneNumberList = multipleJsonPropertyParse(propPath, propValue);
                    for (int i = 0; i < certificationAgencyPhoneNumberList.size(); i++) {
                        Json certificationAgencyPhoneNumber = certificationAgencyPhoneNumberList.get(i);
                        populateXtnField(in3.getCertificationAgencyPhoneNumber(i), certificationAgencyPhoneNumber);
                    }
                    break;
                //Populate IN3.20 - Pre-certification Requirement
                case "preCertificationRequirement":
                    List<Json> preCertificationRequirementList = multipleJsonPropertyParse(propPath, propValue);
                    for (int i = 0; i < preCertificationRequirementList.size(); i++) {
                        Json preCertificationRequirement = preCertificationRequirementList.get(i);
                        populateIcdField(in3.getPreCertificationRequirement(i), preCertificationRequirement);
                    }
                    break;
                //Populate IN3.21 - Case Manager
                case "caseManager":
                    in3.getCaseManager().setValue(propValue);
                    break;
                //Populate IN3.22 - Second Opinion Date
                case "secondOpinionDate":
                    in3.getSecondOpinionDate().setValue(propValue);
                    break;
                //Populate IN3.23 - Second Opinion Status
                case "secondOpinionStatus":
                    Json secondOpinionStatus = jsonOrValuePropertyParse(propPath, propValue);
                    populateCweField(in3.getSecondOpinionStatus(), secondOpinionStatus);
                    break;
                //Populate IN3.24 - Second Opinion Documentation Received
                case "secondOpinionDocumentationReceived":
                    List<Json> secondOpinionDocumentationReceivedList = arrayPropertyToJson(propPath, propValue);
                    for (int i = 0; i < secondOpinionDocumentationReceivedList.size(); i++) {
                        Json secondOpinionDocumentationReceived = secondOpinionDocumentationReceivedList.get(i);
                        populateCweField(in3.getSecondOpinionDocumentationReceived(i), secondOpinionDocumentationReceived);
                    }
                    break;
                //Populate IN3.25 - Second Opinion Physician
                case "secondOpinionPhysician":
                    List<Json> secondOpinionPhysicianList = arrayPropertyToJson(propPath, propValue);
                    for (int i = 0; i < secondOpinionPhysicianList.size(); i++) {
                        Json secondOpinionPhysician = secondOpinionPhysicianList.get(i);
                        populateXcnField(in3.getSecondOpinionPhysician(i), secondOpinionPhysician);
                    }
                    break;
                //Populate IN3.26 - Certification Type
                case "certificationType":
                    Json certificationType = jsonOrValuePropertyParse(propPath, propValue);
                    populateCweField(in3.getCertificationType(), certificationType);
                    break;
                //Populate IN3.27 - Certification Category
                case "certificationCategory":
                    Json certificationCategory = jsonOrValuePropertyParse(propPath, propValue);
                    populateCweField(in3.getCertificationCategory(), certificationCategory);
                    break;
                default:
                    throw EndpointException.permanent(ErrorCode.ARGUMENT, "The property ['" + key + "'] does not correspond with any possible IN3 field");
            }
        }
    }

    public static void populatePr1Segment(PR1 pr1, Json pr1Values) throws DataTypeException {
        for (String key : pr1Values.keys()) {
            String propPath = "procedures." + key;
            String propValue = pr1Values.string(key);

            switch (key) {
            //Populate PR1.1 "Set Id - Pr1"
                case "setId":
                    pr1.getSetIDPR1().setValue(propValue);
                    break;
                //Populate PR1.2 "Procedure Coding Method" withdrawn
                //Populate PR1.3 "Procedure Code"
                case "procedureCode":
                    Json procedureCode = jsonOrValuePropertyParse(propPath, propValue);
                    ;
                    populateCneField(pr1.getProcedureCode(), procedureCode);
                    break;
                //Populate PR1.4 "Procedure Description" withdrawn
                //Populate PR1.5 "Procedure Date/Time"
                case "procedureDateTime":
                    pr1.getProcedureDateTime().setValue(propValue);
                    break;
                //Populate PR1.6 "Procedure Functional Type"
                case "procedureFunctionalType":
                    Json procedureFunctionalType = jsonOrValuePropertyParse(propPath, propValue);
                    ;
                    populateCweField(pr1.getProcedureFunctionalType(), procedureFunctionalType);
                    break;
                //Populate PR1.7 "Procedure Minutes"
                case "procedureMinutes":
                    pr1.getProcedureMinutes().setValue(propValue);
                    break;
                //Populate PR1.8 "Anesthesiologist" withdrawn
                //Populate PR1.9 "Anesthesia Code Type"
                case "anesthesiaCode":
                    Json anesthesiaCode = jsonOrValuePropertyParse(propPath, propValue);
                    populateCweField(pr1.getAnesthesiaCode(), anesthesiaCode);
                    break;
                //Populate PR1.10 "Anesthesia Minutes"
                case "anesthesiaMinutes":
                    pr1.getAnesthesiaMinutes().setValue(propValue);
                    break;
                //Populate PR1.11 "Surgeon" withdrawn
                //Populate PR1.12 "Procedure Practitioner" withdrawn
                //Populate PR1.13 "Consent Code"
                case "consentCode":
                    Json consentCode = jsonOrValuePropertyParse(propPath, propValue);
                    populateCweField(pr1.getConsentCode(), consentCode);
                    break;
                //Populate PR1.14 "Procedure Priority"
                case "procedurePriority":
                    pr1.getProcedurePriority().setValue(propValue);
                    break;
                //Populate PR1.15 "Associated Diagnosis Code"
                case "associatedDiagnosisCode":
                    Json associatedDiagnosisCode = jsonOrValuePropertyParse(propPath, propValue);
                    populateCweField(pr1.getAssociatedDiagnosisCode(), associatedDiagnosisCode);
                    break;
                //Populate PR1.16 "Procedure Code Modifier" component
                case "procedureCodeModifier":
                    List<Json> procedureCodeModifierList = arrayPropertyToJson(propPath, propValue);
                    for (int i = 0; i < procedureCodeModifierList.size(); i++) {
                        Json procedureCodeModifier = procedureCodeModifierList.get(i);
                        populateCneField(pr1.getProcedureCodeModifier(i), procedureCodeModifier);
                    }
                    break;
                //Populate PR1.17 "Procedure Drg Type"
                case "procedureDrgType":
                    Json procedureDrgType = jsonOrValuePropertyParse(propPath, propValue);
                    populateCweField(pr1.getProcedureDRGType(), procedureDrgType);
                    break;
                //Populate PR1.18 "Tissue Type Code" component
                case "tissueTypeCode":
                    List<Json> tissueTypeCodeList = arrayPropertyToJson(propPath, propValue);
                    for (int i = 0; i < tissueTypeCodeList.size(); i++) {
                        Json tissueTypeCode = tissueTypeCodeList.get(i);
                        populateCweField(pr1.getTissueTypeCode(i), tissueTypeCode);
                    }
                    break;
                //Populate PR1.19 "Procedure Identifier"
                case "procedureIdentifier":
                    Json procedureIdentifier = jsonOrValuePropertyParse(propPath, propValue);
                    populateEiField(pr1.getProcedureIdentifier(), procedureIdentifier);
                    break;
                //Populate PR1.20 "Procedure Action Code"
                case "procedureActionCode":
                    pr1.getProcedureActionCode().setValue(propValue);
                    break;
                //Populate PR1.21 "Drg Procedure Determination Status"
                case "drgProcedureDeterminationStatus":
                    Json drgProcedureDeterminationStatus = jsonOrValuePropertyParse(propPath, propValue);
                    populateCweField(pr1.getDRGProcedureDeterminationStatus(), drgProcedureDeterminationStatus);
                    break;
                //Populate PR1.22 "Drg Procedure Relevance"
                case "drgProcedureRelevance":
                    Json drgProcedureRelevance = jsonOrValuePropertyParse(propPath, propValue);
                    populateCweField(pr1.getDRGProcedureRelevance(), drgProcedureRelevance);
                    break;
                //Populate PR1.23 "Treating Organizational Unit"
                case "treatingOrganizationalUnit":
                    List<Json> treatingOrganizationalUnitList = arrayPropertyToJson(propPath, propValue);
                    for (int i = 0; i < treatingOrganizationalUnitList.size(); i++) {
                        Json treatingOrganizationalUnit = treatingOrganizationalUnitList.get(i);
                        populatePlField(pr1.getTreatingOrganizationalUnit(i), treatingOrganizationalUnit);
                    }
                    break;
                //Populate PR1.24 "Respiratory Within Surgery" component
                case "respiratoryWithinSurgery":
                    pr1.getRespiratoryWithinSurgery().setValue(propValue);
                    break;
                //Populate PR1.25 "Parent Procedure Id"
                case "parentProcedureId":
                    Json parentProcedureId = jsonOrValuePropertyParse(propPath, propValue);
                    populateEiField(pr1.getParentProcedureID(), parentProcedureId);
                    break;
                default:
                    throw EndpointException.permanent(ErrorCode.ARGUMENT, "The property ['" + key + "'] does not correspond with any possible PR1 field");
            }
        }
    }

    public static void  populateGt1Segment(GT1 gt1, Json gt1Values) throws DataTypeException {
        for (String key : gt1Values.keys()) {
            String propPath = "guarantor." + key;
            String propValue = gt1Values.string(key);

            switch (key) {
                //Populate GT1.1 - Set Id - Gt1
                case "setId":
                    gt1.getSetIDGT1().setValue(propValue);
                    break;
                //Populate GT1.2 - Guarantor Number
                case "guarantorNumber":
                    List<Json> guarantorNumberList = multipleJsonPropertyParse(propPath, propValue);
                    for (int i = 0; i < guarantorNumberList.size(); i++) {
                        Json guarantorNumber = guarantorNumberList.get(i);
                        populateCxField(gt1.getGuarantorNumber(i), guarantorNumber);
                    }
                    break;
                //Populate GT1.3 - Guarantor Name
                case "guarantorName":
                    List<Json> guarantorNameList = multipleJsonPropertyParse(propPath, propValue);
                    for (int i = 0; i < guarantorNameList.size(); i++) {
                        Json guarantorName = guarantorNameList.get(i);
                        populateXpnField(gt1.getGuarantorName(i), guarantorName);
                    }
                    break;
                //Populate GT1.4 - Guarantor Spouse Name
                case "guarantorSpouseName":
                    List<Json> guarantorSpouseNameList = multipleJsonPropertyParse(propPath, propValue);
                    for (int i = 0; i < guarantorSpouseNameList.size(); i++) {
                        Json guarantorSpouseName = guarantorSpouseNameList.get(i);
                        populateXpnField(gt1.getGuarantorSpouseName(i), guarantorSpouseName);
                    }
                    break;
                //Populate GT1.5 - Guarantor Address
                case "guarantorAddress":
                    List<Json> guarantorAddressList = multipleJsonPropertyParse(propPath, propValue);
                    for (int i = 0; i < guarantorAddressList.size(); i++) {
                        Json guarantorAddress = guarantorAddressList.get(i);
                        populateXadField(gt1.getGuarantorAddress(i), guarantorAddress);
                    }
                    break;
                //Populate GT1.6 - Guarantor Ph Num - Home
                case "guarantorPhNumHome":
                    List<Json> guarantorPhNumHomeList = multipleJsonPropertyParse(propPath, propValue);
                    for (int i = 0; i < guarantorPhNumHomeList.size(); i++) {
                        Json guarantorPhNumHome = guarantorPhNumHomeList.get(i);
                        populateXtnField(gt1.getGuarantorPhNumHome(i), guarantorPhNumHome);
                    }
                    break;
                //Populate GT1.7 - Guarantor Ph Num - Business
                case "guarantorPhNumBusiness":
                    List<Json> guarantorPhNumBusinessList = multipleJsonPropertyParse(propPath, propValue);
                    for (int i = 0; i < guarantorPhNumBusinessList.size(); i++) {
                        Json guarantorPhNumBusiness = guarantorPhNumBusinessList.get(i);
                        populateXtnField(gt1.getGuarantorPhNumBusiness(i), guarantorPhNumBusiness);
                    }
                    break;
                //Populate GT1.8 - Guarantor Date/Time Of Birth
                case "guarantorDateTimeOfBirth":
                    gt1.getGuarantorDateTimeOfBirth().setValue(propValue);
                    break;
                //Populate GT1.9 - Guarantor Administrative Sex
                case "guarantorAdministrativeSex":
                    Json guarantorAdministrativeSex = jsonOrValuePropertyParse(propPath, propValue);
                    populateCweField(gt1.getGuarantorAdministrativeSex(), guarantorAdministrativeSex);
                    break;
                //Populate GT1.10 - Guarantor Type
                case "guarantorType":
                    Json guarantorType = jsonOrValuePropertyParse(propPath, propValue);
                    populateCweField(gt1.getGuarantorType(), guarantorType);
                    break;
                //Populate GT1.11 - Guarantor Relationship
                case "guarantorRelationship":
                    Json guarantorRelationship = jsonOrValuePropertyParse(propPath, propValue);
                    populateCweField(gt1.getGuarantorRelationship(), guarantorRelationship);
                    break;
                //Populate GT1.12 - Guarantor Ssn
                case "guarantorSsn":
                    gt1.getGuarantorSSN().setValue(propValue);
                    break;
                //Populate GT1.13 - Guarantor Date - Begin
                case "guarantorDateBegin":
                    gt1.getGuarantorDateBegin().setValue(propValue);
                    break;
                //Populate GT1.14 - Guarantor Date - End
                case "guarantorDateEnd":
                    gt1.getGuarantorDateEnd().setValue(propValue);
                    break;
                //Populate GT1.15 - Guarantor Priority
                case "guarantorPriority":
                    gt1.getGuarantorPriority().setValue(propValue);
                    break;
                //Populate GT1.16 - Guarantor Employer Name
                case "guarantorEmployerName":
                    List<Json> guarantorEmployerNameList = arrayPropertyToJson(propPath, propValue);
                    for (int i = 0; i < guarantorEmployerNameList.size(); i++) {
                        Json guarantorEmployerName = guarantorEmployerNameList.get(i);
                        populateXpnField(gt1.getGuarantorEmployerName(i), guarantorEmployerName);
                    }
                    break;
                //Populate GT1.17 - Guarantor Employer Address
                case "guarantorEmployerAddress":
                    List<Json> guarantorEmployerAddressList = arrayPropertyToJson(propPath, propValue);
                    for (int i = 0; i < guarantorEmployerAddressList.size(); i++) {
                        Json guarantorEmployerAddress = guarantorEmployerAddressList.get(i);
                        populateXadField(gt1.getGuarantorEmployerAddress(i), guarantorEmployerAddress);
                    }
                    break;
                //Populate GT1.18 - Guarantor Employer Phone Number
                case "guarantorEmployerPhoneNumber":
                    List<Json> guarantorEmployerPhoneNumberList = multipleJsonPropertyParse(propPath, propValue);
                    for (int i = 0; i < guarantorEmployerPhoneNumberList.size(); i++) {
                        Json guarantorEmployerPhoneNumber = guarantorEmployerPhoneNumberList.get(i);
                        populateXtnField(gt1.getGuarantorEmployerPhoneNumber(i), guarantorEmployerPhoneNumber);
                    }
                    break;
                //Populate GT1.19 - Guarantor Employee Id Number
                case "guarantorEmployeeIdNumber":
                    List<Json> guarantorEmployeeIdNumberList = multipleJsonPropertyParse(propPath, propValue);
                    for (int i = 0; i < guarantorEmployeeIdNumberList.size(); i++) {
                        Json guarantorEmployeeIdNumber = guarantorEmployeeIdNumberList.get(i);
                        populateCxField(gt1.getGuarantorEmployeeIDNumber(i), guarantorEmployeeIdNumber);
                    }
                    break;
                //Populate GT1.20 - Guarantor Employment Status
                case "guarantorEmploymentStatus":
                    Json guarantorEmploymentStatus = jsonOrValuePropertyParse(propPath, propValue);
                    ;
                    populateCweField(gt1.getGuarantorEmploymentStatus(), guarantorEmploymentStatus);
                    break;
                //Populate GT1.21 - Guarantor Organization Name
                case "guarantorOrganizationName":
                    List<Json> guarantorOrganizationNameList = arrayPropertyToJson(propPath, propValue);
                    for (int i = 0; i < guarantorOrganizationNameList.size(); i++) {
                        Json guarantorOrganizationName = guarantorOrganizationNameList.get(i);
                        populateXonField(gt1.getGuarantorOrganizationName(i), guarantorOrganizationName);
                    }
                    break;
                //Populate GT1.22 - Guarantor Billing Hold Flag
                case "guarantorBillingHoldFlag":
                    gt1.getGuarantorBillingHoldFlag().setValue(propValue);
                    break;
                //Populate GT1.23 - Guarantor Credit Rating Code
                case "guarantorCreditRatingCode":
                    Json guarantorCreditRatingCode = jsonOrValuePropertyParse(propPath, propValue);
                    populateCweField(gt1.getGuarantorCreditRatingCode(), guarantorCreditRatingCode);
                    break;
                //Populate GT1.24 - Guarantor Death Date And Time
                case "guarantorDeathDateAndTime":
                    gt1.getGuarantorDeathDateAndTime().setValue(propValue);
                    break;
                //Populate GT1.25 - Guarantor Death Flag
                case "guarantorDeathFlag":
                    gt1.getGuarantorDeathFlag().setValue(propValue);
                    break;
                //Populate GT1.26 - Guarantor Charge Adjustment Code
                case "guarantorChargeAdjustmentCode":
                    Json guarantorChargeAdjustmentCode = jsonOrValuePropertyParse(propPath, propValue);
                    populateCweField(gt1.getGuarantorEmploymentStatus(), guarantorChargeAdjustmentCode);
                    break;
                //Populate GT1.27 - Guarantor Household Annual Income
                case "guarantorHouseholdAnnualIncome":
                    Json guarantorHouseholdAnnualIncome = jsonOrValuePropertyParse(propPath, propValue);
                    populateCpField(gt1.getGuarantorHouseholdAnnualIncome(), guarantorHouseholdAnnualIncome);
                    break;
                //Populate GT1.28 - Guarantor Household Size
                case "guarantorHouseholdSize":
                    gt1.getGuarantorHouseholdSize().setValue(propValue);
                    break;
                //Populate GT1.29 - Guarantor Employer Id Number
                case "guarantorEmployerIdNumber":
                    List<Json> guarantorEmployerIdNumberList = multipleJsonPropertyParse(propPath, propValue);
                    for (int i = 0; i < guarantorEmployerIdNumberList.size(); i++) {
                        Json guarantorEmployerIdNumber = guarantorEmployerIdNumberList.get(i);
                        populateCxField(gt1.getGuarantorEmployerIDNumber(i), guarantorEmployerIdNumber);
                    }
                    break;
                //Populate GT1.30 - Guarantor Marital Status Code
                case "guarantorMaritalStatusCode":
                    Json guarantorMaritalStatusCode = jsonOrValuePropertyParse(propPath, propValue);
                    populateCweField(gt1.getGuarantorMaritalStatusCode(), guarantorMaritalStatusCode);
                    break;
                //Populate GT1.31 - Guarantor Hire Effective Date
                case "guarantorHireEffectiveDate":
                    gt1.getGuarantorHireEffectiveDate().setValue(propValue);
                    break;
                //Populate GT1.32 - Employment Stop Date
                case "employmentStopDate":
                    gt1.getEmploymentStopDate().setValue(propValue);
                    break;
                //Populate GT1.33 - Living Dependency
                case "livingDependency":
                    Json livingDependency = jsonOrValuePropertyParse(propPath, propValue);
                    populateCweField(gt1.getLivingDependency(), livingDependency);
                    break;
                //Populate GT1.34 - Ambulatory Status
                case "ambulatoryStatus":
                    List<Json> ambulatoryStatusList = arrayPropertyToJson(propPath, propValue);
                    for (int i = 0; i < ambulatoryStatusList.size(); i++) {
                        Json ambulatoryStatus = ambulatoryStatusList.get(i);
                        populateCweField(gt1.getAmbulatoryStatus(i), ambulatoryStatus);
                    }
                    break;
                //Populate GT1.35 - Citizenship
                case "citizenship":
                    List<Json> citizenshipList = arrayPropertyToJson(propPath, propValue);
                    for (int i = 0; i < citizenshipList.size(); i++) {
                        Json citizenship = citizenshipList.get(i);
                        populateCweField(gt1.getCitizenship(i), citizenship);
                    }
                    break;
                //Populate GT1.36 - Primary Language
                case "primaryLanguage":
                    Json primaryLanguage = jsonOrValuePropertyParse(propPath, propValue);
                    populateCweField(gt1.getPrimaryLanguage(), primaryLanguage);
                    break;
                //Populate GT1.37 - Living Arrangement
                case "livingArrangement":
                    Json livingArrangement = jsonOrValuePropertyParse(propPath, propValue);
                    populateCweField(gt1.getLivingArrangement(), livingArrangement);
                    break;
                //Populate GT1.38 - Publicity Code
                case "publicityCode":
                    Json publicityCode = jsonOrValuePropertyParse(propPath, propValue);
                    populateCweField(gt1.getPublicityCode(), publicityCode);
                    break;
                //Populate GT1.39 - Protection Indicator
                case "protectionIndicator":
                    gt1.getProtectionIndicator().setValue(propValue);
                    break;
                //Populate GT1.40 - Student Indicator
                case "studentIndicator":
                    Json studentIndicator = jsonOrValuePropertyParse(propPath, propValue);
                    populateCweField(gt1.getStudentIndicator(), studentIndicator);
                    break;
                //Populate GT1.41 - Religion
                case "religion":
                    Json religion = jsonOrValuePropertyParse(propPath, propValue);
                    populateCweField(gt1.getReligion(), religion);
                    break;
                //Populate GT1.42 - Mother's Maiden Name
                case "mothersMaidenName":
                    List<Json> mothersMaidenNameList = arrayPropertyToJson(propPath, propValue);
                    for (int i = 0; i < mothersMaidenNameList.size(); i++) {
                        Json mothersMaidenName = mothersMaidenNameList.get(i);
                        populateXpnField(gt1.getMotherSMaidenName(i), mothersMaidenName);
                    }
                    break;
                //Populate GT1.43 - Nationality
                case "nationality":
                    Json nationality = jsonOrValuePropertyParse(propPath, propValue);
                    populateCweField(gt1.getNationality(), nationality);
                    break;
                //Populate GT1.44 - Ethnic Group
                case "ethnicGroup":
                    List<Json> ethnicGroupList = arrayPropertyToJson(propPath, propValue);
                    for (int i = 0; i < ethnicGroupList.size(); i++) {
                        Json ethnicGroup = ethnicGroupList.get(i);
                        populateCweField(gt1.getEthnicGroup(i), ethnicGroup);
                    }
                    break;
                //Populate GT1.45 - Contact Person's Name
                case "contactPersonsName":
                    List<Json> contactPersonsNameList = arrayPropertyToJson(propPath, propValue);
                    for (int i = 0; i < contactPersonsNameList.size(); i++) {
                        Json contactPersonsName = contactPersonsNameList.get(i);
                        populateXpnField(gt1.getContactPersonSName(i), contactPersonsName);
                    }
                    break;
                //Populate GT1.46 - Contact Person's Telephone Number
                case "contactPersonsTelephoneNumber":
                    List<Json> contactPersonsTelephoneNumberList = multipleJsonPropertyParse(propPath, propValue);
                    for (int i = 0; i < contactPersonsTelephoneNumberList.size(); i++) {
                        Json contactPersonsTelephoneNumber = contactPersonsTelephoneNumberList.get(i);
                        populateXtnField(gt1.getContactPersonSTelephoneNumber(i), contactPersonsTelephoneNumber);
                    }
                    break;
                //Populate GT1.47 - Contact Reason
                case "contactReason":
                    Json contactReason = jsonOrValuePropertyParse(propPath, propValue);
                    populateCweField(gt1.getContactReason(), contactReason);
                    break;
                //Populate GT1.48 - Contact Relationship
                case "contactRelationship":
                    Json contactRelationship = jsonOrValuePropertyParse(propPath, propValue);
                    populateCweField(gt1.getContactRelationship(), contactRelationship);
                    break;
                //Populate GT1.49 - Job Title
                case "jobTitle":
                    gt1.getJobTitle().setValue(propValue);
                    break;
                //Populate GT1.50 - Job Code/Class
                case "jobCodeClass":
                    Json jobCodeClass = jsonOrValuePropertyParse(propPath, propValue);
                    populateJccField(gt1.getJobCodeClass(), jobCodeClass);
                    break;
                //Populate GT1.51 - Guarantor Employer's Organization Name
                case "guarantorEmployersOrganizationName":
                    List<Json> guarantorEmployersOrganizationNameList = arrayPropertyToJson(propPath, propValue);
                    for (int i = 0; i < guarantorEmployersOrganizationNameList.size(); i++) {
                        Json guarantorEmployersOrganizationName = guarantorEmployersOrganizationNameList.get(i);
                        populateXonField(gt1.getGuarantorEmployerSOrganizationName(i), guarantorEmployersOrganizationName);
                    }
                    break;
                //Populate GT1.52 - Handicap
                case "handicap":
                    Json handicap = jsonOrValuePropertyParse(propPath, propValue);
                    populateCweField(gt1.getHandicap(), handicap);
                    break;
                //Populate GT1.53 - Job Status
                case "jobStatus":
                    Json jobStatus = jsonOrValuePropertyParse(propPath, propValue);
                    populateCweField(gt1.getJobStatus(), jobStatus);
                    break;
                //Populate GT1.54 - Guarantor Financial Class
                case "guarantorFinancialClass":
                    Json guarantorFinancialClass = jsonOrValuePropertyParse(propPath, propValue);
                    populateFcField(gt1.getGuarantorFinancialClass(), guarantorFinancialClass);
                    break;
                //Populate GT1.55 - Guarantor Race
                case "guarantorRace":
                    List<Json> guarantorRaceList = arrayPropertyToJson(propPath, propValue);
                    for (int i = 0; i < guarantorRaceList.size(); i++) {
                        Json guarantorRace = guarantorRaceList.get(i);
                        populateCweField(gt1.getGuarantorRace(i), guarantorRace);
                    }
                    break;
                //Populate GT1.56 - Guarantor Birth Place
                case "guarantorBirthPlace":
                    gt1.getGuarantorBirthPlace().setValue(propValue);
                    break;
                //Populate GT1.57 - Vip Indicator
                case "vipIndicator":
                    Json vipIndicator = jsonOrValuePropertyParse(propPath, propValue);
                    populateCweField(gt1.getVIPIndicator(), vipIndicator);
                    break;
                default:
                    throw EndpointException.permanent(ErrorCode.ARGUMENT, "The property ['" + key + "'] does not correspond with any possible PR1 field");
            }
        }
    }

    public static void populateAutSegment(AUT aut, Json autValues) throws DataTypeException {
        for (String key : autValues.keys()) {
            String propPath = "authorizationInformation." + key;
            String propValue = autValues.string(key);

            switch (key) {
                //Populate AUT.1 - Authorizing Payor, Plan Id
                case "authorizingPayorPlanId":
                    Json authorizingPayorPlanId = jsonOrValuePropertyParse(propPath, propValue);
                    populateCweField(aut.getAuthorizingPayorPlanID(), authorizingPayorPlanId);
                    break;
                //Populate AUT.2 - Authorizing Payor, Company Id
                case "authorizingPayorCompanyId":
                    Json authorizingPayorCompanyId = jsonOrValuePropertyParse(propPath, propValue);
                    populateCweField(aut.getAuthorizingPayorCompanyID(), authorizingPayorCompanyId);
                    break;
                //Populate AUT.3 - Authorizing Payor, Company Name
                case "authorizingPayorCompanyName":
                    aut.getAuthorizingPayorCompanyName().setValue(propValue);
                    break;
                //Populate AUT.4 - Authorization Effective Date
                case "authorizationEffectiveDate":
                    aut.getAuthorizationEffectiveDate().setValue(propValue);
                    break;
                //Populate AUT.5 - Authorization Expiration Date
                case "authorizationExpirationDate":
                    aut.getAuthorizationExpirationDate().setValue(propValue);
                    break;
                //Populate AUT.6 - Authorization Identifier
                case "authorizationIdentifier":
                    Json authorizationIdentifier = jsonOrValuePropertyParse(propPath, propValue);
                    populateEiField(aut.getAuthorizationIdentifier(), authorizationIdentifier);
                    break;
                //Populate AUT.7 - Reimbursement Limit
                case "reimbursementLimit":
                    Json reimbursementLimit = jsonOrValuePropertyParse(propPath, propValue);
                    populateCpField(aut.getReimbursementLimit(), reimbursementLimit);
                    break;
                //Populate AUT.8 - Requested Number Of Treatments
                case "requestedNumberOfTreatments":
                    Json requestedNumberOfTreatments = jsonOrValuePropertyParse(propPath, propValue);
                    populateCqField(aut.getRequestedNumberOfTreatments(), requestedNumberOfTreatments);
                    break;
                //Populate AUT.9 - Authorized Number Of Treatments
                case "authorizedNumberOfTreatments":
                    Json authorizedNumberOfTreatments = jsonOrValuePropertyParse(propPath, propValue);
                    populateCqField(aut.getAuthorizedNumberOfTreatments(), authorizedNumberOfTreatments);
                    break;
                //Populate AUT.10 - Process Date
                case "processDate":
                    aut.getProcessDate().setValue(propValue);
                    break;
                //Populate AUT.11 - Requested Discipline(s)
                case "requestedDisciplines":
                    List<Json> requestedDisciplinesList = arrayPropertyToJson(propPath, propValue);
                    for (int i = 0; i < requestedDisciplinesList.size(); i++) {
                        Json requestedDisciplines = requestedDisciplinesList.get(i);
                        populateCweField(aut.getRequestedDisciplineS(i), requestedDisciplines);
                    }
                    break;
                //Populate AUT.12 - Authorized Discipline(s)
                case "authorizedDisciplines":
                    List<Json> authorizedDisciplinesList = arrayPropertyToJson(propPath, propValue);
                    for (int i = 0; i < authorizedDisciplinesList.size(); i++) {
                        Json authorizedDisciplines = authorizedDisciplinesList.get(i);
                        populateCweField(aut.getAuthorizedDisciplineS(i), authorizedDisciplines);
                    }
                    break;
                //Populate AUT.13 - Authorization Referral Type
                case "authorizationReferralType":
                    Json authorizationReferralType = jsonOrValuePropertyParse(propPath, propValue);
                    populateCweField(aut.getAuthorizationReferralType(), authorizationReferralType);
                    break;
                //Populate AUT.14 - Approval Status
                case "approvalStatus":
                    Json approvalStatus = jsonOrValuePropertyParse(propPath, propValue);
                    populateCweField(aut.getApprovalStatus(), approvalStatus);
                    break;
                //Populate AUT.15 - Planned Treatment Stop Date
                case "plannedTreatmentStopDate":
                    aut.getPlannedTreatmentStopDate().setValue(propValue);
                    break;
                //Populate AUT.16 - Clinical Service
                case "clinicalService":
                    Json clinicalService = jsonOrValuePropertyParse(propPath, propValue);
                    populateCweField(aut.getClinicalService(), clinicalService);
                    break;
                //Populate AUT.17 - Reason Text
                case "reasonText":
                    aut.getReasonText().setValue(propValue);
                    break;
                //Populate AUT.18 - Number of Authorized Treatments/Units
                case "numberOfAuthorizedTreatmentsUnits":
                    Json numberOfAuthorizedTreatmentsUnits = jsonOrValuePropertyParse(propPath, propValue);
                    populateCqField(aut.getNumberOfAuthorizedTreatmentsUnits(), numberOfAuthorizedTreatmentsUnits);
                    break;
                //Populate AUT.19 - Number of Used Treatments/Units
                case "numberOfUsedTreatmentsUnits":
                    Json numberOfUsedTreatmentsUnits = jsonOrValuePropertyParse(propPath, propValue);
                    populateCqField(aut.getNumberOfUsedTreatmentsUnits(), numberOfUsedTreatmentsUnits);
                    break;
                //Populate AUT.20 - Number of Schedule Treatments/Units
                case "numberOfScheduleTreatmentsUnits":
                    Json numberOfScheduleTreatmentsUnits = jsonOrValuePropertyParse(propPath, propValue);
                    populateCqField(aut.getNumberOfScheduleTreatmentsUnits(), numberOfScheduleTreatmentsUnits);
                    break;
                //Populate AUT.21 - Encounter Type
                case "encounterType":
                    Json encounterType = jsonOrValuePropertyParse(propPath, propValue);
                    populateCweField(aut.getEncounterType(), encounterType);
                    break;
                //Populate AUT.22 - Remaining Benefit Amount
                case "remainingBenefitAmount":
                    Json remainingBenefitAmount = jsonOrValuePropertyParse(propPath, propValue);
                    populateMoField(aut.getRemainingBenefitAmount(), remainingBenefitAmount);
                    break;
                //Populate AUT.23 - Authorized Provider
                case "authorizedProvider":
                    Json authorizedProvider = jsonOrValuePropertyParse(propPath, propValue);
                    populateXonField(aut.getAuthorizedProvider(), authorizedProvider);
                    break;
                //Populate AUT.24 - Authorized Health Professional
                case "authorizedHealthProfessional":
                    Json authorizedHealthProfessional = singleJsonPropertyParse(propPath, propValue);
                    populateXcnField(aut.getAuthorizedHealthProfessional(), authorizedHealthProfessional);
                    break;
                //Populate AUT.25 - Source Text
                case "sourceText":
                    aut.getSourceText().setValue(propValue);
                    break;
                //Populate AUT.26 - Source Date
                case "sourceDate":
                    aut.getSourceDate().setValue(propValue);
                    break;
                //Populate AUT.27 - Source Phone
                case "sourcePhone":
                    Json sourcePhone = singleJsonPropertyParse(propPath, propValue);
                    populateXtnField(aut.getSourcePhone(), sourcePhone);
                    break;
                //Populate AUT.28 - Comment
                case "comment":
                    aut.getComment().setValue(propValue);
                    break;
                //Populate AUT.29 - Action Code
                case "actionCode":
                    aut.getActionCode().setValue(propValue);
                    break;
                default:
                    throw EndpointException.permanent(ErrorCode.ARGUMENT, "The property ['" + key + "'] does not correspond with any possible AUT field");
            }
        }
    }

    public static void populateRf1Segment(RF1 rf1, Json rf1Values) throws DataTypeException {
        for (String key : rf1Values.keys()) {
            String propPath = "referralInformation." + key;
            String propValue = rf1Values.string(key);

            switch (key) {
                //Populate RF1.1 - Referral Status
                case "referralStatus":
                    Json referralStatus = jsonOrValuePropertyParse(propPath, propValue);
                    ;
                    populateCweField(rf1.getReferralStatus(), referralStatus);
                    break;
                //Populate RF1.2 - Referral Priority
                case "referralPriority":
                    Json referralPriority = jsonOrValuePropertyParse(propPath, propValue);
                    ;
                    populateCweField(rf1.getReferralPriority(), referralPriority);

                    break;
                //Populate RF1.3 - Referral Type
                case "referralType":
                    Json referralType = jsonOrValuePropertyParse(propPath, propValue);
                    ;
                    populateCweField(rf1.getReferralStatus(), referralType);
                    break;
                //Populate RF1.4 - Referral Disposition
                case "referralDisposition":
                    List<Json> referralDispositionList = arrayPropertyToJson(propPath, propValue);
                    for (int i = 0; i < referralDispositionList.size(); i++) {
                        Json referralDisposition = referralDispositionList.get(i);
                        populateCweField(rf1.getReferralDisposition(i), referralDisposition);
                    }
                    break;
                //Populate RF1.5 - Referral Category
                case "referralCategory":
                    Json referralCategory = jsonOrValuePropertyParse(propPath, propValue);
                    ;
                    populateCweField(rf1.getReferralCategory(), referralCategory);
                    break;
                //Populate RF1.6 - Originating Referral Identifier
                case "originatingReferralIdentifier":
                    Json originatingReferralIdentifier = jsonOrValuePropertyParse(propPath, propValue);
                    ;
                    populateEiField(rf1.getOriginatingReferralIdentifier(), originatingReferralIdentifier);
                    break;
                //Populate RF1.7 - Effective Date
                case "effectiveDate":
                    rf1.getEffectiveDate().setValue(propValue);
                    break;
                //Populate RF1.8 - Expiration Date
                case "expirationDate":
                    rf1.getExpirationDate().setValue(propValue);
                    break;
                //Populate RF1.9 - Process Date
                case "processDate":
                    rf1.getProcessDate().setValue(propValue);
                    break;
                //Populate RF1.10 - Referral Reason
                case "referralReason":
                    List<Json> referralReasonList = arrayPropertyToJson(propPath, propValue);
                    for (int i = 0; i < referralReasonList.size(); i++) {
                        Json referralReason = referralReasonList.get(i);
                        populateCweField(rf1.getReferralReason(i), referralReason);
                    }
                    break;
                //Populate RF1.11 - External Referral Identifier
                case "externalReferralIdentifier":
                    List<Json> externalReferralIdentifierList = arrayPropertyToJson(propPath, propValue);
                    for (int i = 0; i < externalReferralIdentifierList.size(); i++) {
                        Json externalReferralIdentifier = externalReferralIdentifierList.get(i);
                        populateEiField(rf1.getExternalReferralIdentifier(i), externalReferralIdentifier);
                    }
                    break;
                //Populate RF1.12 - Referral Documentation Completion Status
                case "referralDocumentationCompletionStatus":
                    Json referralDocumentationCompletionStatus = jsonOrValuePropertyParse(propPath, propValue);
                    ;
                    populateCweField(rf1.getReferralDocumentationCompletionStatus(), referralDocumentationCompletionStatus);
                    break;
                //Populate RF1.13 - Planned Treatment Stop Date
                case "plannedTreatmentStopDate":
                    rf1.getPlannedTreatmentStopDate().setValue(propValue);
                    break;
                //Populate RF1.14 - Referral Reason Text
                case "referralReasonText":
                    rf1.getReferralReasonText().setValue(propValue);
                    break;
                //Populate RF1.15 - Number of Authorized Treatments/Units
                case "numberOfAuthorizedTreatmentsUnits":
                    Json numberOfAuthorizedTreatmentsUnits = jsonOrValuePropertyParse(propPath, propValue);
                    ;
                    populateCqField(rf1.getNumberOfAuthorizedTreatmentsUnits(), numberOfAuthorizedTreatmentsUnits);
                    break;
                //Populate RF1.16 - Number of Used Treatments/Units
                case "numberOfUsedTreatmentsUnits":
                    Json numberOfUsedTreatmentsUnits = jsonOrValuePropertyParse(propPath, propValue);
                    ;
                    populateCqField(rf1.getNumberOfUsedTreatmentsUnits(), numberOfUsedTreatmentsUnits);
                    break;
                //Populate RF1.17 - Number of Schedule Treatments/Units
                case "numberOfScheduleTreatmentsUnits":
                    Json numberOfScheduleTreatmentsUnits = jsonOrValuePropertyParse(propPath, propValue);
                    ;
                    populateCqField(rf1.getNumberOfScheduleTreatmentsUnits(), numberOfScheduleTreatmentsUnits);
                    break;
                //RF1.18 - Remaining Benefit Amount doesn't exists in the HAPI library
                //Populate RF1.19 - Authorized Provider
                case "authorizedProvider":
                    Json authorizedProvider = jsonOrValuePropertyParse(propPath, propValue);
                    ;
                    populateXonField(rf1.getAuthorizedProvider(), authorizedProvider);
                    break;
                //Populate RF1.20 - Authorized Health Professional
                case "authorizedHealthProfessional":
                    Json authorizedHealthProfessional = singleJsonPropertyParse(propPath, propValue);
                    populateXcnField(rf1.getAuthorizedHealthProfessional(), authorizedHealthProfessional);
                    break;
                //Populate RF1.21 - Source Text
                case "sourceText":
                    rf1.getSourceText().setValue(propValue);
                    break;
                //Populate RF1.22 - Source Date
                case "sourceDate":
                    rf1.getSourceDate().setValue(propValue);
                    break;
                //Populate RF1.23 - Source Phone
                case "sourcePhone":
                    Json sourcePhone = singleJsonPropertyParse(propPath, propValue);
                    populateXtnField(rf1.getSourcePhone(), sourcePhone);
                    break;
                //Populate RF1.24 - Comment
                case "comment":
                    rf1.getComment().setValue(propValue);
                    break;
                //Populate RF1.25 - Action Code
                case "actionCode":
                    rf1.getActionCode().setValue(propValue);
                    break;
                default:
                    throw EndpointException.permanent(ErrorCode.ARGUMENT, "The property ['" + key + "'] does not correspond with any possible RF1 field");
            }
        }
    }

    public static void populatePdaSegment(PDA pda, Json pdaValues) throws DataTypeException {
        for (String key : pdaValues.keys()) {
            String propPath = "patientDeathAndAutopsy." + key;
            String propValue = pdaValues.string(key);

            switch (key) {
                //Populate PDA.1 - Death Cause Code
                case "deathCauseCode":
                    List<Json> deathCauseCodeList = arrayPropertyToJson(propPath, propValue);
                    for (int i = 0; i < deathCauseCodeList.size(); i++) {
                        Json deathCauseCode = deathCauseCodeList.get(i);
                        populateCweField(pda.getDeathCauseCode(i), deathCauseCode);
                    }
                    break;
                //Populate PDA.2 - Death Location
                case "deathLocation":
                    Json deathLocation = jsonOrValuePropertyParse(propPath, propValue);;
                    populatePlField(pda.getDeathLocation(),deathLocation);
                    break;
                //Populate PDA.3 - Death Certified Indicator
                case "deathCertifiedIndicator":
                    pda.getDeathCertifiedIndicator().setValue(propValue);
                    break;
                //Populate PDA.4 - Death Certificate Signed Date/Time
                case "deathCertificateSignedDateTime":
                    pda.getDeathCertificateSignedDateTime().setValue(propValue);
                    break;
                //Populate PDA.5 - Death Certified By
                case "deathCertifiedBy":
                    Json deathCertifiedBy = jsonOrValuePropertyParse(propPath, propValue);;
                    populateXcnField(pda.getDeathCertifiedBy(),deathCertifiedBy);
                    break;
                //Populate PDA.6 - Autopsy Indicator
                case "autopsyIndicator":
                    pda.getAutopsyIndicator().setValue(propValue);
                    break;
                //Populate PDA.7 - Autopsy Start And End Date/Time
                case "autopsyStartAndEndDateTime":
                    Json autopsyStartAndEndDateTime = jsonOrValuePropertyParse(propPath, propValue);;
                    populateDrField(pda.getAutopsyStartAndEndDateTime(),autopsyStartAndEndDateTime);
                    break;
                //Populate PDA.8 - Autopsy Performed By
                case "autopsyPerformedBy":
                    Json autopsyPerformedBy = jsonOrValuePropertyParse(propPath, propValue);;
                    populateXcnField(pda.getAutopsyPerformedBy(),autopsyPerformedBy);
                    break;
                //Populate PDA.9 - Coroner Indicator
                case "coronerIndicator":
                    pda.getCoronerIndicator().setValue(propValue);
                    break;
                default:
                    throw EndpointException.permanent(ErrorCode.ARGUMENT, "The property ['" + key + "'] does not correspond with any possible PDA field");
            }
        }
    }

    public static void populateOrcSegment(ORC orc, Json orcValues) throws DataTypeException {
        for (String key : orcValues.keys()) {
            String propPath = "commonOrder." + key;
            String propValue = orcValues.string(key);

            switch (key) {
                //Populate ORC.1 - Order Control
                case "orderControl":
                    orc.getOrderControl().setValue(propValue);
                    break;
                //Populate ORC.2 - Placer Order Number
                case "placerOrderNumber":
                    Json placerOrderNumber = jsonOrValuePropertyParse(propPath, propValue);;
                    populateEiField(orc.getPlacerOrderNumber(),placerOrderNumber);
                    break;
                //Populate ORC.3 - Filler Order Number
                case "fillerOrderNumber":
                    Json fillerOrderNumber = jsonOrValuePropertyParse(propPath, propValue);;
                    populateEiField(orc.getFillerOrderNumber(),fillerOrderNumber);
                    break;
                //Populate ORC.4 - Placer Group Number
                case "placerGroupNumber":
                    Json placerGroupNumber = jsonOrValuePropertyParse(propPath, propValue);;
                    populateEipField(orc.getPlacerGroupNumber(),placerGroupNumber);
                    break;
                //Populate ORC.5 - Order Status
                case "orderStatus":
                    orc.getOrderStatus().setValue(propValue);
                    break;
                //Populate ORC.6 - Response Flag
                case "responseFlag":
                    orc.getResponseFlag().setValue(propValue);
                    break;
                //Populate ORC.7 - Quantity/Timing Withdrawn
                //Populate ORC.8 - Parent
                case "parent":
                    Json parent = jsonOrValuePropertyParse(propPath, propValue);;
                    populateEipField(orc.getParentOrder(),parent);
                    break;
                //Populate ORC.9 - Date/Time Of Transaction
                case "dateTimeOfTransaction":
                    orc.getDateTimeOfTransaction().setValue(propValue);
                    break;
                //Populate ORC.10 - Entered By
                case "enteredBy":
                    List<Json> enteredByList = multipleJsonPropertyParse(propPath,propValue);
                    for (int i = 0; i < enteredByList.size(); i++) {
                        Json enteredBy = enteredByList.get(i);
                        populateXcnField(orc.getEnteredBy(i), enteredBy);
                    }
                    break;
                //Populate ORC.11 - Verified By
                case "verifiedBy":
                    List<Json> verifiedByList = multipleJsonPropertyParse(propPath,propValue);
                    for (int i = 0; i < verifiedByList.size(); i++) {
                        Json verifiedBy = verifiedByList.get(i);
                        populateXcnField(orc.getVerifiedBy(i), verifiedBy);
                    }
                    break;
                //Populate ORC.12 - Ordering Provider
                case "orderingProvider":
                    List<Json> orderingProviderList = multipleJsonPropertyParse(propPath,propValue);
                    for (int i = 0; i < orderingProviderList.size(); i++) {
                        Json orderingProvider = orderingProviderList.get(i);
                        populateXcnField(orc.getOrderingProvider(i), orderingProvider);
                    }
                    break;
                //Populate ORC.13 - Enterer's Location
                case "enterersLocation":
                    Json enterersLocation = jsonOrValuePropertyParse(propPath, propValue);;
                    populatePlField(orc.getEntererSLocation(),enterersLocation);
                    break;
                //Populate ORC.14 - Call Back Phone Number
                case "callBackPhoneNumber":
                    List<Json> callBackPhoneNumberList = multipleJsonPropertyParse(propPath,propValue);
                    for (int i = 0; i < callBackPhoneNumberList.size(); i++) {
                        Json callBackPhoneNumber = callBackPhoneNumberList.get(i);
                        populateXtnField(orc.getCallBackPhoneNumber(i), callBackPhoneNumber);
                    }
                    break;
                //Populate ORC.15 - Order Effective Date/Time
                case "orderEffectiveDateTime":
                    orc.getOrderEffectiveDateTime().setValue(propValue);
                    break;
                //Populate ORC.16 - Order Control Code Reason
                case "orderControlCodeReason":
                    Json orderControlCodeReason = jsonOrValuePropertyParse(propPath, propValue);;
                    populateCweField(orc.getOrderControlCodeReason(),orderControlCodeReason);
                    break;
                //Populate ORC.17 - Entering Organization
                case "enteringOrganization":
                    Json enteringOrganization = jsonOrValuePropertyParse(propPath, propValue);;
                    populateCweField(orc.getEnteringOrganization(),enteringOrganization);
                    break;
                //Populate ORC.18 - Entering Device
                case "enteringDevice":
                    Json enteringDevice = jsonOrValuePropertyParse(propPath, propValue);;
                    populateCweField(orc.getEnteringDevice(),enteringDevice);
                    break;
                //Populate ORC.19 - Action By
                case "actionBy":
                    List<Json> actionByList = multipleJsonPropertyParse(propPath,propValue);
                    for (int i = 0; i < actionByList.size(); i++) {
                        Json actionBy = actionByList.get(i);
                        populateXcnField(orc.getActionBy(i), actionBy);
                    }
                    break;
                //Populate ORC.20 - Advanced Beneficiary Notice Code
                case "advancedBeneficiaryNoticeCode":
                    Json advancedBeneficiaryNoticeCode = jsonOrValuePropertyParse(propPath, propValue);;
                    populateCweField(orc.getAdvancedBeneficiaryNoticeCode(),advancedBeneficiaryNoticeCode);
                    break;
                //Populate ORC.21 - Ordering Facility Name
                case "orderingFacilityName":
                    List<Json> orderingFacilityNameList = multipleJsonPropertyParse(propPath,propValue);
                    for (int i = 0; i < orderingFacilityNameList.size(); i++) {
                        Json orderingFacilityName = orderingFacilityNameList.get(i);
                        populateXonField(orc.getOrderingFacilityName(i), orderingFacilityName);
                    }
                    break;
                //Populate ORC.22 - Ordering Facility Address
                case "orderingFacilityAddress":
                    List<Json> orderingFacilityAddressList = multipleJsonPropertyParse(propPath,propValue);
                    for (int i = 0; i < orderingFacilityAddressList.size(); i++) {
                        Json orderingFacilityAddress = orderingFacilityAddressList.get(i);
                        populateXadField(orc.getOrderingFacilityAddress(i), orderingFacilityAddress);
                    }
                    break;
                //Populate ORC.23 - Ordering Facility Phone Number
                case "orderingFacilityPhoneNumber":
                    List<Json> orderingFacilityPhoneNumberList = multipleJsonPropertyParse(propPath,propValue);
                    for (int i = 0; i < orderingFacilityPhoneNumberList.size(); i++) {
                        Json orderingFacilityPhoneNumber = orderingFacilityPhoneNumberList.get(i);
                        populateXtnField(orc.getOrderingFacilityPhoneNumber(i), orderingFacilityPhoneNumber);
                    }
                    break;
                //Populate ORC.24 - Ordering Provider Address
                case "orderingProviderAddress":
                    List<Json> orderingProviderAddressList = multipleJsonPropertyParse(propPath,propValue);
                    for (int i = 0; i < orderingProviderAddressList.size(); i++) {
                        Json orderingProviderAddress = orderingProviderAddressList.get(i);
                        populateXadField(orc.getOrderingProviderAddress(i), orderingProviderAddress);
                    }
                    break;
                //Populate ORC.25 - Order Status Modifier
                case "orderStatusModifier":
                    Json orderStatusModifier = jsonOrValuePropertyParse(propPath, propValue);;
                    populateCweField(orc.getOrderStatusModifier(),orderStatusModifier);
                    break;
                //Populate ORC.26 - Advanced Beneficiary Notice Override Reason
                case "advancedBeneficiaryNoticeOverrideReason":
                    Json advancedBeneficiaryNoticeOverrideReason = jsonOrValuePropertyParse(propPath, propValue);;
                    populateCweField(orc.getAdvancedBeneficiaryNoticeOverrideReason(),advancedBeneficiaryNoticeOverrideReason);
                    break;
                //Populate ORC.27 - Filler's Expected Availability Date/Time
                case "fillersExpectedAvailabilityDate/time":
                    orc.getFillerSExpectedAvailabilityDateTime().setValue(propValue);
                    break;
                //Populate ORC.28 - Confidentiality Code
                case "confidentialityCode":
                    Json confidentialityCode = jsonOrValuePropertyParse(propPath, propValue);;
                    populateCweField(orc.getConfidentialityCode(),confidentialityCode);
                    break;
                //Populate ORC.29 - Order Type
                case "orderType":
                    Json orderType = jsonOrValuePropertyParse(propPath, propValue);;
                    populateCweField(orc.getOrderType(),orderType);
                    break;
                //Populate ORC.30 - Enterer Authorization Mode
                case "entererAuthorizationMode":
                    Json entererAuthorizationMode = jsonOrValuePropertyParse(propPath, propValue);;
                    populateCneField(orc.getEntererAuthorizationMode(),entererAuthorizationMode);
                    break;
                //Populate ORC.31 - Parent Universal Service Identifier
                case "parentUniversalServiceIdentifier":
                    Json parentUniversalServiceIdentifier = jsonOrValuePropertyParse(propPath, propValue);;
                    populateCweField(orc.getParentUniversalServiceIdentifier(),parentUniversalServiceIdentifier);
                    break;
                //Populate ORC.32 - Advanced Beneficiary Notice Date
                case "advancedBeneficiaryNoticeDate":
                    orc.getAdvancedBeneficiaryNoticeDate().setValue(propValue);
                    break;
                //Populate ORC.33 - Alternate Placer Order Number
                case "alternatePlacerOrderNumber":
                    List<Json> alternatePlacerOrderNumberList = multipleJsonPropertyParse(propPath, propValue);
                    for (int i = 0; i < alternatePlacerOrderNumberList.size(); i++) {
                        Json alternatePlacerOrderNumber = alternatePlacerOrderNumberList.get(i);
                        populateCxField(orc.getAlternatePlacerOrderNumber(i),alternatePlacerOrderNumber);
                    }
                    break;
                //Populate ORC.34 - Order Workflow Profile
                case "orderWorkflowProfile":
                    List<Json> orderWorkflowProfileList = arrayPropertyToJson(propPath, propValue);
                    for (int i = 0; i < orderWorkflowProfileList.size(); i++) {
                        Json orderWorkflowProfile = orderWorkflowProfileList.get(i);
                        populateCweField(orc.getOrderWorkflowProfile(i),orderWorkflowProfile);
                    }
                    break;
                default:
                    throw EndpointException.permanent(ErrorCode.ARGUMENT, "The property ['" + key + "'] does not correspond with any possible ORC field");
            }
        }
    }
}
