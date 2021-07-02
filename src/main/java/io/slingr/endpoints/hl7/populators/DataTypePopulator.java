package io.slingr.endpoints.hl7.populators;

import ca.uhn.hl7v2.model.AbstractMessage;
import ca.uhn.hl7v2.model.DataTypeException;
import ca.uhn.hl7v2.model.Varies;
import ca.uhn.hl7v2.model.v24.datatype.CK;
import ca.uhn.hl7v2.model.v24.datatype.CN;
import ca.uhn.hl7v2.model.v281.datatype.*;
import io.slingr.endpoints.utils.Json;

import static io.slingr.endpoints.hl7.jsonHelper.JsonHelper.jsonOrValuePropertyParse;

public class DataTypePopulator {
    public static void populateCweField(CWE cwe, Json cweValues) throws DataTypeException {
        cwe.getIdentifier().setValue(
                //CWE must always have an identifier, so if its not a property, its the value, whose key is "mainValue"
                cweValues.contains("identifier") ? cweValues.string("identifier") : cweValues.contains("mainValue") ? cweValues.string("mainValue") : ""
        );
        cwe.getText().setValue(
                cweValues.contains("text") ? cweValues.string("text") : ""
        );
        cwe.getNameOfCodingSystem().setValue(
                cweValues.contains("nameOfCodingSystem") ? cweValues.string("nameOfCodingSystem") : ""
        );
        cwe.getAlternateIdentifier().setValue(
                cweValues.contains("alternateIdentifier") ? cweValues.string("alternateIdentifier") : ""
        );
        cwe.getAlternateText().setValue(
                cweValues.contains("alternateText") ? cweValues.string("alternateText") : ""
        );
        cwe.getNameOfAlternateCodingSystem().setValue(
                cweValues.contains("nameOfAlternateCodingSystem") ? cweValues.string("alternateText") : ""
        );
        cwe.getCodingSystemVersionID().setValue(
                cweValues.contains("codingSystemVersionId") ? cweValues.string("codingSystemVersionId") : ""
        );
        cwe.getAlternateCodingSystemVersionID().setValue(
                cweValues.contains("alternateCodingSystemVersionID") ? cweValues.string("alternateCodingSystemVersionID") : ""
        );
        cwe.getOriginalText().setValue(
                cweValues.contains("originalText") ? cweValues.string("originalText") : ""
        );
        cwe.getSecondAlternateIdentifier().setValue(
                cweValues.contains("secondAlternateIdentifier") ? cweValues.string("secondAlternateIdentifier") : ""
        );
        cwe.getSecondAlternateText().setValue(
                cweValues.contains("secondAlternateText") ? cweValues.string("secondAlternateText") : ""
        );
        cwe.getNameOfSecondAlternateCodingSystem().setValue(
                cweValues.contains("nameOfSecondAlternateCodingSystem") ? cweValues.string("nameOfSecondAlternateCodingSystem") : ""
        );
        cwe.getSecondAlternateCodingSystemVersionID().setValue(
                cweValues.contains("secondAlternateCodingSystemVersionID") ? cweValues.string("secondAlternateCodingSystemVersionID") : ""
        );
        cwe.getCodingSystemOID().setValue(
                cweValues.contains("codingSystemOID") ? cweValues.string("codingSystemOID") : ""
        );
        cwe.getValueSetOID().setValue(
                cweValues.contains("valueSetOID") ? cweValues.string("valueSetOID") : ""
        );
        cwe.getValueSetVersionID().setValue(
                cweValues.contains("valueSetVersionID") ? cweValues.string("valueSetVersionID") : ""
        );
        cwe.getAlternateCodingSystemOID().setValue(
                cweValues.contains("alternateCodingSystemOID") ? cweValues.string("alternateCodingSystemOID") : ""
        );
        cwe.getAlternateValueSetOID().setValue(
                cweValues.contains("AlternateValueSetOID") ? cweValues.string("AlternateValueSetOID") : ""
        );
        cwe.getAlternateValueSetVersionID().setValue(
                cweValues.contains("alternateValueSetVersionID") ? cweValues.string("alternateValueSetVersionID") : ""
        );
        cwe.getSecondAlternateCodingSystemOID().setValue(
                cweValues.contains("secondAlternateCodingSystemOID") ? cweValues.string("secondAlternateCodingSystemOID") : ""
        );
        cwe.getSecondAlternateValueSetOID().setValue(
                cweValues.contains("secondAlternateValueSetOID") ? cweValues.string("secondAlternateValueSetOID") : ""
        );
        cwe.getSecondAlternateValueSetVersionID().setValue(
                cweValues.contains("secondAlternateValueSetVersionID") ? cweValues.string("secondAlternateValueSetVersionID") : ""
        );
    }

    public static void populateXonField(XON xon, Json xonValues) throws DataTypeException{
        //Populate XON.1 "Organization Name" component
        xon.getOrganizationName().setValue(
                xonValues.contains("organizationName") ? xonValues.string("organizationName") : xonValues.contains("mainValue") ? xonValues.string("mainValue") : ""
        );
        //Populate XON.2 "Organization Name Type Code" component
        if(xonValues.contains("organizationNameTypeCode")){
            Json organizationNameTypeCode = jsonOrValuePropertyParse("organizationNameTypeCode",xonValues.string("organizationNameTypeCode"));
            populateCweField(xon.getOrganizationNameTypeCode(),organizationNameTypeCode);
        }
        //Populate XON.3 "Id Number" withdrawn
        //Populate XON.4 "Identifier Check Digit" withdrawn
        //Populate XON.5 "Check Digit Scheme" withdrawn
        //Populate XON.6 "Assigning Authority" component
        if(xonValues.contains("assigningAuthority")){
            Json assigningAuthority = jsonOrValuePropertyParse("patientPrimaryFacility.assigningAuthority",xonValues.string("assigningAuthority"));
            populateHdField(xon.getAssigningAuthority(),assigningAuthority);
        }
        //Populate XON.7 "Identifier Type Code" component
        xon.getIdentifierTypeCode().setValue(
                xonValues.contains("identifierTypeCode") ? xonValues.string("identifierTypeCode") : ""
        );
        //Populate XON.8 "Assigning Facility" component
        if(xonValues.contains("assigningFacility")){
            Json assigningFacility = jsonOrValuePropertyParse("patientPrimaryFacility.assigningFacility",xonValues.string("assigningFacility"));
            populateHdField(xon.getAssigningFacility(),assigningFacility);
        }
        //Populate XON.9 "Name Representation Code" component
        xon.getNameRepresentationCode().setValue(
                xonValues.contains("nameRepresentationCode") ? xonValues.string("nameRepresentationCode") : ""
        );
        //Populate XON.10 "Organization Identifier" component
        xon.getOrganizationIdentifier().setValue(
                xonValues.contains("organizationIdentifier") ? xonValues.string("organizationIdentifier") : ""
        );
    }

    public static void populateHdField(HD hd, Json hdValues) throws DataTypeException {
        hd.getNamespaceID().setValue(
                hdValues.contains("namespaceId") ? hdValues.string("namespaceId") : hdValues.contains("mainValue") ? hdValues.string("mainValue") : ""
        );
        hd.getUniversalID().setValue(
                hdValues.contains("universalId") ? hdValues.string("universalId") : ""
        );
        hd.getUniversalIDType().setValue(
                hdValues.contains("universalIdType") ? hdValues.string("universalIdType") : ""
        );
    }



    public static void populateCxField(CX cx, Json cxValues) throws DataTypeException {
        cx.getIDNumber().setValue(
                cxValues.contains("idNumber") ? cxValues.string("idNumber") : ""
        );
        cx.getIdentifierCheckDigit().setValue(
                cxValues.contains("identifierCheckDigit") ? cxValues.string("identifierCheckDigit") : ""
        );
        cx.getCheckDigitScheme().setValue(
                cxValues.contains("checkDigitScheme") ? cxValues.string("checkDigitScheme") : ""
        );
        if(cxValues.contains("assigningAuthority")) {
            Json assigningAuthority = jsonOrValuePropertyParse("duplicatePatient.assigningAuthority", cxValues.string("assigningAuthority"));
            populateHdField(cx.getAssigningAuthority(), assigningAuthority);
        }
        cx.getIdentifierTypeCode().setValue(
                cxValues.contains("identifierTypeCode") ? cxValues.string("identifierTypeCode") : ""
        );
        if(cxValues.contains("assigningFacility")){
            Json assigningFacility = jsonOrValuePropertyParse("duplicatePatient.assigningAuthority",cxValues.string("assigningFacility"));
            populateHdField(cx.getAssigningFacility(), assigningFacility);
        }
        cx.getEffectiveDate().setValue(
                cxValues.contains("effectiveDate") ? cxValues.string("effectiveDate") : ""
        );
        cx.getExpirationDate().setValue(
                cxValues.contains("expirationDate") ? cxValues.string("expirationDate") : ""
        );
        if(cxValues.contains("assigningJurisdiction")){
            Json assigningJurisdiction = jsonOrValuePropertyParse("duplicatePatient.assigningJurisdiction",cxValues.string("assigningJurisdiction"));
            populateCweField(cx.getAssigningJurisdiction(),assigningJurisdiction);
        }
        if(cxValues.contains("assigningAgencyOrDepartment")){
            Json assigningAgencyOrDepartment = jsonOrValuePropertyParse("duplicatePatient.assigningAgencyOrDepartment",cxValues.string("assigningAgencyOrDepartment"));
            populateCweField(cx.getAssigningAgencyOrDepartment(),assigningAgencyOrDepartment);
        }
        cx.getSecurityCheck().setValue(
                cxValues.contains("securityCheck") ? cxValues.string("securityCheck") : ""
        );
        cx.getSecurityCheckScheme().setValue(
                cxValues.contains("securityCheckScheme") ? cxValues.string("securityCheckScheme") : ""
        );
    }

    public static void populateXpnField(XPN xpn, Json xpnValues) throws DataTypeException {
        xpn.getFamilyName().getSurname().setValue(
                xpnValues.contains("familyName") ? xpnValues.string("familyName") : xpnValues.contains("mainValue") ? xpnValues.string("mainValue") : ""
        );
        xpn.getGivenName().setValue(
                xpnValues.contains("givenName") ? xpnValues.string("givenName") : ""
        );
        xpn.getSecondAndFurtherGivenNamesOrInitialsThereof().setValue(
                xpnValues.contains("secondAndFurtherGivenNames") ? xpnValues.string("secondAndFurtherGivenNames") : ""
        );
        xpn.getSuffixEgJRorIII().setValue(
                xpnValues.contains("suffix") ? xpnValues.string("suffix") : ""
        );
        xpn.getPrefixEgDR().setValue(
                xpnValues.contains("prefix") ? xpnValues.string("prefix") : ""
        );
        xpn.getDegreeEgMD().setValue(
                xpnValues.contains("degree") ? xpnValues.string("degree") : ""
        );
        xpn.getNameTypeCode().setValue(
                xpnValues.contains("nameTypeCode") ? xpnValues.string("nameTypeCode") : ""
        );
        xpn.getNameRepresentationCode().setValue(
                xpnValues.contains("nameRepresentationCode") ? xpnValues.string("nameRepresentationCode") : ""
        );
        if(xpnValues.contains("nameContext")){
            Json nameContext = jsonOrValuePropertyParse("nameContext",xpnValues.string("nameContext"));
            populateCweField(xpn.getNameContext(),nameContext);
        }
        //PID.5.10 "Name Validity Range" Withdrawn
        xpn.getNameAssemblyOrder().setValue(
                xpnValues.contains("nameAssemblyOrder") ? xpnValues.string("nameAssemblyOrder") : ""
        );
        xpn.getNameTypeCode().setValue(
                xpnValues.contains("nameTypeCode") ? xpnValues.string("nameTypeCode") : ""
        );
        xpn.getEffectiveDate().setValue(
                xpnValues.contains("effectiveDate") ? xpnValues.string("effectiveDate") : ""
        );
        xpn.getExpirationDate().setValue(
                xpnValues.contains("expirationDate") ? xpnValues.string("expirationDate") : ""
        );
        xpn.getProfessionalSuffix().setValue(
                xpnValues.contains("professionalSuffix") ? xpnValues.string("professionalSuffix") : ""
        );
        xpn.getCalledBy().setValue(
                xpnValues.contains("calledBy") ? xpnValues.string("calledBy") : ""
        );
    }

    public static void populateXadField(XAD xad, Json xadValues) throws DataTypeException {
        //Populate XAD.1 "Street Address"
        if(xadValues.contains("streetAddress") || xadValues.contains("mainValue")){
            String streetAddressValue = xadValues.contains("streetAddress") ? xadValues.string("streetAddress") : xadValues.string("mainValue");
            Json streetAddress = jsonOrValuePropertyParse("streetAddress",streetAddressValue);
            populateSadField(xad.getStreetAddress(),streetAddress);
        }
        //Populate XAD.2 "Other Designation"
        xad.getOtherDesignation().setValue(
                xadValues.contains("otherDesignation") ? xadValues.string("otherDesignation") : ""
        );
        //Populate XAD.3 "City"
        xad.getCity().setValue(
                xadValues.contains("city") ? xadValues.string("city") : ""
        );
        //Populate XAD.4 "State Or Province"
        xad.getStateOrProvince().setValue(
                xadValues.contains("stateOrProvince") ? xadValues.string("stateOrProvince") : ""
        );
        //Populate XAD.5 "Zip Or Postal Code"
        xad.getZipOrPostalCode().setValue(
                xadValues.contains("zipOrPostalCode") ? xadValues.string("zipOrPostalCode") : ""
        );
        //Populate XAD.6 "Country"
        xad.getCountry().setValue(
                xadValues.contains("country") ? xadValues.string("country") : ""
        );
        //Populate XAD.7 "Address Type"
        xad.getAddressType().setValue(
                xadValues.contains("addressType") ? xadValues.string("addressType") : ""
        );
        //Populate XAD.8 "Other Geographic Designation"
        xad.getOtherGeographicDesignation().setValue(
                xadValues.contains("otherGeographicDesignation") ? xadValues.string("otherGeographicDesignation") : ""
        );
        //Populate XAD.9 "County/Parish Code"
        if(xadValues.contains("countryParishCode")){
            Json countyParishCode = jsonOrValuePropertyParse("countyParishCode",xadValues.string("countyParishCode"));
            populateCweField(xad.getCountyParishCode(),countyParishCode);
        }
        //Populate XAD.10 "Census Tract"
        if(xadValues.contains("censusTract")){
            Json censusTract = jsonOrValuePropertyParse("censusTract",xadValues.string("censusTract"));
            populateCweField(xad.getCensusTract(),censusTract);
        }
        //Populate XAD.11 "Address Representation Code"
        xad.getAddressRepresentationCode().setValue(
                xadValues.contains("addressRepresentationCode") ? xadValues.string("addressRepresentationCode") : ""
        );
        //Populate XAD.12 "Address Validity Range"
        xad.getAddressValidityRange().setValue(
                xadValues.contains("addressValidityRange") ? xadValues.string("addressValidityRange") : ""
        );
        //Populate XAD.13 "Effective Date"
        xad.getEffectiveDate().setValue(
                xadValues.contains("effectiveDate") ? xadValues.string("effectiveDate") : ""
        );
        //Populate XAD.14 "Expiration Date"
        xad.getExpirationDate().setValue(
                xadValues.contains("expirationDate") ? xadValues.string("expirationDate") : ""
        );
        //Populate XAD.15 "Expiration Reason"
        if(xadValues.contains("expirationReason")){
            Json expirationReason = jsonOrValuePropertyParse("expirationReason",xadValues.string("expirationReason"));
            populateCweField(xad.getExpirationReason(),expirationReason);
        }
        //Populate XAD.16 "Temporary Indicator"
        xad.getTemporaryIndicator().setValue(
                xadValues.contains("temporaryIndicator") ? xadValues.string("temporaryIndicator") : ""
        );
        //Populate XAD.17 "Bad Address Indicator"
        xad.getBadAddressIndicator().setValue(
                xadValues.contains("badAddressIndicator") ? xadValues.string("badAddressIndicator") : ""
        );
        //Populate XAD.18 "Address Usage"
        xad.getAddressUsage().setValue(
                xadValues.contains("addressUsage") ? xadValues.string("addressUsage") : ""
        );
        //Populate XAD.19 "Addressee"
        xad.getAddressee().setValue(
                xadValues.contains("addressee") ? xadValues.string("addressee") : ""
        );
        //Populate XAD.20 "Comment"
        xad.getComment().setValue(
                xadValues.contains("comment") ? xadValues.string("comment") : ""
        );
        //Populate XAD.21 "Comment"
        xad.getPreferenceOrder().setValue(
                xadValues.contains("preferenceOrder") ? xadValues.string("preferenceOrder") : ""
        );
        //Populate XAD.22 "Protection Code"
        if(xadValues.contains("protectionCode")){
            Json protectionCode = jsonOrValuePropertyParse("protectionCode", xadValues.string("protectionCode"));
            populateCweField(xad.getProtectionCode(),protectionCode);
        }
        //Populate XAD.23 "Address Identifier"
        if(xadValues.contains("addressIdentifier")){
            Json addressIdentifier = jsonOrValuePropertyParse("addressIdentifier", xadValues.string("addressIdentifier"));
            populateEiField(xad.getAddressIdentifier(),addressIdentifier);
        }
    }

    public static void populateSadField(SAD sad, Json sadValues) throws DataTypeException {
        sad.getStreetOrMailingAddress().setValue(
                sadValues.contains("streetOrMailingAddress") ? sadValues.string("streetOrMailingAddress") : sadValues.string("mainValue")
        );
        sad.getStreetName().setValue(
                sadValues.contains("streetName") ? sadValues.string("streetName") : ""
        );
        sad.getDwellingNumber().setValue(
                sadValues.contains("dwellingNumber") ? sadValues.string("dwellingNumber") : ""
        );
    }
    public static void populateEiField(EI ei, Json eiValues) throws DataTypeException {
        //Populate EI.1 "Entity Identifier"
        ei.getEntityIdentifier().setValue(
                eiValues.contains("entityIdentifier") ? eiValues.string("entityIdentifier") : eiValues.contains("mainValue") ? eiValues.string("mainValue") : ""
        );
        //Populate EI.2 "Namespace Id"
        ei.getNamespaceID().setValue(
                eiValues.contains("namespaceId") ? eiValues.string("namespaceId") : ""
        );
        //Populate EI.3 "Universal Id"
        ei.getUniversalID().setValue(
                eiValues.contains("universalId") ? eiValues.string("universalId") : ""
        );
        //Populate EI.4 "Universal Id Type"
        ei.getUniversalIDType().setValue(
                eiValues.contains("universalIdType") ? eiValues.string("universalIdType") : ""
        );
    }

    public static void populateXtnField(XTN xtn, Json xtnValues) throws DataTypeException {
        xtn.getTelephoneNumber().setValue(
                xtnValues.contains("telephoneNumber") ? xtnValues.string("telephoneNumber") : ""
        );
        xtn.getTelecommunicationUseCode().setValue(
                xtnValues.contains("telecommunicationUseCode") ? xtnValues.string("telecommunicationUseCode") : ""
        );
        xtn.getTelecommunicationEquipmentType().setValue(
                xtnValues.contains("telecommunicationEquipmentType") ? xtnValues.string("telecommunicationEquipmentType") : ""
        );
        xtn.getCommunicationAddress().setValue(
                xtnValues.contains("communicationAddress") ? xtnValues.string("communicationAddress") : ""
        );
        xtn.getCountryCode().setValue(
                xtnValues.contains("countryCode") ? xtnValues.string("countryCode") : ""
        );
        xtn.getAreaCityCode().setValue(
                xtnValues.contains("areaCityCode") ? xtnValues.string("areaCityCode") : ""
        );
        xtn.getLocalNumber().setValue(
                xtnValues.contains("localNumber") ? xtnValues.string("localNumber") : ""
        );
        xtn.getExtension().setValue(
                xtnValues.contains("extension") ? xtnValues.string("extension") : ""
        );
        xtn.getAnyText().setValue(
                xtnValues.contains("anyText") ? xtnValues.string("anyText") : ""
        );
        xtn.getExtensionPrefix().setValue(
                xtnValues.contains("extensionPrefix") ? xtnValues.string("extensionPrefix") : ""
        );
        xtn.getSpeedDialCode().setValue(
                xtnValues.contains("speedDialCode") ? xtnValues.string("speedDialCode") : ""
        );
        xtn.getUnformattedTelephoneNumber().setValue(
                xtnValues.contains("unformattedTelephoneNumber") ? xtnValues.string("unformattedTelephoneNumber") : ""
        );
        xtn.getEffectiveStartDate().setValue(
                xtnValues.contains("effectiveStartDate") ? xtnValues.string("effectiveStartDate") : ""
        );
        xtn.getExpirationDate().setValue(
                xtnValues.contains("expirationDate") ? xtnValues.string("expirationDate") : ""
        );
        if(xtnValues.contains("expirationReason")){
            Json expirationReason = jsonOrValuePropertyParse("expirationReason",xtnValues.string("expirationReason"));
            populateCweField(xtn.getExpirationReason(),expirationReason);
        }
        if(xtnValues.contains("protectionCode")){
            Json protectionCode = jsonOrValuePropertyParse("protectionCode",xtnValues.string("protectionCode"));
            populateCweField(xtn.getProtectionCode(),protectionCode);
        }
        if(xtnValues.contains("sharedTelecommunicationIdentifier")){
            Json sharedTelecommunicationIdentifier = jsonOrValuePropertyParse("sharedTelecommunicationIdentifier",xtnValues.string("sharedTelecommunicationIdentifier"));
            populateEiField(xtn.getSharedTelecommunicationIdentifier(),sharedTelecommunicationIdentifier);
        }
        xtn.getPreferenceOrder().setValue(
                xtnValues.contains("preferenceOrder") ? xtnValues.string("preferenceOrder") : ""
        );
    }

    public static void populateJccField(JCC jcc, Json jccValues) throws DataTypeException {
        //Populate JCC.1 "Job Code" subcomponent
        if(jccValues.contains("jobCode")){
            Json jobCode = jsonOrValuePropertyParse("jobCode",jccValues.string("jobCode"));
            populateCweField(jcc.getJobCode(),jobCode);
        }
        //Populate JCC.2 "Job Class" subcomponent
        if(jccValues.contains("jobClass")){
            Json jobClass = jsonOrValuePropertyParse("jobClass",jccValues.string("jobClass"));
            populateCweField(jcc.getJobClass(),jobClass);
        }
        //Populate JCC.3 "Job Description Text" subcomponent
        jcc.getJobDescriptionText().setValue(
                jccValues.contains("jobDescriptionText") ? jccValues.string("jobDescriptionText") : ""
        );
    }

    public static void populatePlField(PL pl, Json plValues) throws DataTypeException{
        //Populate PL.1 "Point Of Care" subcomponent
        if(plValues.contains("pointOfCare")){
            Json pointOfCare = jsonOrValuePropertyParse("pointOfCare",plValues.string("pointOfCare"));
            populateHdField(pl.getPointOfCare(),pointOfCare);
        }
        //Populate PL.2 "Room" subcomponent
        if(plValues.contains("room")){
            Json room = jsonOrValuePropertyParse("room",plValues.string("room"));
            populateHdField(pl.getRoom(),room);
        }
        //Populate PL.3 "Bed" subcomponent
        if(plValues.contains("bed")){
            Json bed = jsonOrValuePropertyParse("bed",plValues.string("bed"));
            populateHdField(pl.getBed(),bed);
        }
        //Populate PL.4 "Facility" subcomponent
        if(plValues.contains("facility")){
            Json facility = jsonOrValuePropertyParse("facility",plValues.string("facility"));
            populateHdField(pl.getFacility(),facility);
        }
        //Populate PL.5 "Location Status" subcomponent
        pl.getLocationStatus().setValue(
                plValues.contains("locationStatus") ? plValues.string("locationStatus") : ""
        );
        //Populate PL.6 "Person Location Type" subcomponent
        pl.getPersonLocationType().setValue(
                plValues.contains("personLocationType") ? plValues.string("personLocationType") : plValues.contains("mainValue") ? plValues.string("mainValue") : ""
        );
        //Populate PL.7 "Building" subcomponent
        if(plValues.contains("building")){
            Json building = jsonOrValuePropertyParse("building",plValues.string("building"));
            populateHdField(pl.getBuilding(),building);
        }
        //Populate PL.8 "Floor" subcomponent
        if(plValues.contains("floor")){
            Json floor = jsonOrValuePropertyParse("floor",plValues.string("floor"));
            populateHdField(pl.getFloor(),floor);
        }
        //Populate PL.9 "Location Description" subcomponent
        pl.getLocationDescription().setValue(
                plValues.contains("locationDescription") ? plValues.string("locationDescription") : ""
        );
        //Populate PL.10 "Comprehensive Location Identifier" subcomponent
        if(plValues.contains("comprehensiveLocationIdentifier")){
            Json comprehensiveLocationIdentifier = jsonOrValuePropertyParse("comprehensiveLocationIdentifier",plValues.string("comprehensiveLocationIdentifier"));
            populateEiField(pl.getComprehensiveLocationIdentifier(),comprehensiveLocationIdentifier);
        }
        //Populate PL.11 "Assigning Authority For Location" subcomponent
        if(plValues.contains("assigningAuthorityForLocation")){
            Json assigningAuthorityForLocation = jsonOrValuePropertyParse("assigningAuthorityForLocation",plValues.string("assigningAuthorityForLocation"));
            populateHdField(pl.getAssigningAuthorityForLocation(),assigningAuthorityForLocation);
        }
    }

    public static void populateXcnField(XCN xcn, Json xcnValues) throws DataTypeException {
        //Populate XCN.1 "Person Identifier" component
        xcn.getPersonIdentifier().setValue(
                xcnValues.contains("personIdentifier") ? xcnValues.string("personIdentifier") : ""
        );
        //Populate XCN.2 "Family Name" component
        if(xcnValues.contains("familyName")){
            Json familyName = jsonOrValuePropertyParse("familyName",xcnValues.string("familyName"));
            populateFnField(xcn.getFamilyName(),familyName);
        }
        //Populate XCN.3 "Given Name" component
        xcn.getGivenName().setValue(
                xcnValues.contains("givenName") ? xcnValues.string("givenName") : ""
        );
        //Populate XCN.4 "Second And Further Given Names Or Initials Thereof" component
        xcn.getSecondAndFurtherGivenNamesOrInitialsThereof().setValue(
                xcnValues.contains("secondAndFurtherGivenNames") ? xcnValues.string("secondAndFurtherGivenNames") : ""
        );
        //Populate XCN.5 "Suffix (e.g., Jr Or Iii)" component
        xcn.getSuffixEgJRorIII().setValue(
                xcnValues.contains("suffix") ? xcnValues.string("suffix") : ""
        );
        //Populate XCN.6 "Prefix (e.g., Dr)" component
        xcn.getPrefixEgDR().setValue(
                xcnValues.contains("prefix") ? xcnValues.string("prefix") : ""
        );
        //Populate XCN.7 "Degree (e.g., Md)" component
        xcn.getDegreeEgMD().setValue(
                xcnValues.contains("degree") ? xcnValues.string("degree") : ""
        );
        //Populate XCN.8 "Source Table" component
        if (xcnValues.contains("sourceTable")){
            Json sourceTable = jsonOrValuePropertyParse("sourceTable",xcnValues.string("sourceTable"));
            populateCweField(xcn.getSourceTable(),sourceTable);
        }
        //Populate XCN.9 "Assigning Authority" component
        if (xcnValues.contains("assigningAuthority")){
            Json assigningAuthority = jsonOrValuePropertyParse("assigningAuthority",xcnValues.string("assigningAuthority"));
            populateHdField(xcn.getAssigningAuthority(),assigningAuthority);
        }
        //Populate XCN.10 "Name Type Code" component
        xcn.getNameTypeCode().setValue(
                xcnValues.contains("nameTypeCode") ? xcnValues.string("nameTypeCode") : ""
        );
        //Populate XCN.11 "Identifier Check Digit" component
        xcn.getIdentifierCheckDigit().setValue(
                xcnValues.contains("identifierCheckDigit") ? xcnValues.string("identifierCheckDigit") : ""
        );
        //Populate XCN.12 "Check Digit Scheme" component
        xcn.getCheckDigitScheme().setValue(
                xcnValues.contains("checkDigitScheme") ? xcnValues.string("checkDigitScheme") : ""
        );
        //Populate XCN.13 "Identifier Type Code" component
        xcn.getIdentifierTypeCode().setValue(
                xcnValues.contains("identifierTypeCode") ? xcnValues.string("identifierTypeCode") : ""
        );
        //Populate XCN.14 "Assigning Facility" component
        if (xcnValues.contains("assigningFacility")){
            Json assigningFacility = jsonOrValuePropertyParse("assigningFacility",xcnValues.string("assigningFacility"));
            populateHdField(xcn.getAssigningFacility(),assigningFacility);
        }
        //Populate XCN.15 "Name Representation Code" component
        xcn.getNameRepresentationCode().setValue(
                xcnValues.contains("nameRepresentationCode") ? xcnValues.string("nameRepresentationCode") : ""
        );
        //Populate XCN.16 "Name Context" component
        if(xcnValues.contains("nameContext")){
            Json nameContext = jsonOrValuePropertyParse("nameContext",xcnValues.string("nameContext"));
            populateCweField(xcn.getNameContext(),nameContext);
        }
        //XCN.17 "Name Validity Range" Withdrawn
        //Populate XCN.18 "Name Assembly Order" component
        xcn.getNameAssemblyOrder().setValue(
                xcnValues.contains("nameAssemblyOrder") ? xcnValues.string("nameAssemblyOrder") : ""
        );
        //Populate XCN.19 "Effective Date" component
        xcn.getEffectiveDate().setValue(
                xcnValues.contains("effectiveDate") ? xcnValues.string("effectiveDate") : ""
        );
        //Populate XCN.19 "Effective Date" component
        xcn.getEffectiveDate().setValue(
                xcnValues.contains("effectiveDate") ? xcnValues.string("effectiveDate") : ""
        );
        //Populate XCN.20 "Expiration Date" component
        xcn.getExpirationDate().setValue(
                xcnValues.contains("expirationDate") ? xcnValues.string("expirationDate") : ""
        );
        //Populate XCN.21 "Professional Suffix" component
        xcn.getProfessionalSuffix().setValue(
                xcnValues.contains("professionalSuffix") ? xcnValues.string("professionalSuffix") : ""
        );
        //Populate XCN.22 "Assigning Jurisdiction" component
        if(xcnValues.contains("assigningJurisdiction")){
            Json assigningJurisdiction = jsonOrValuePropertyParse("assigningJurisdiction",xcnValues.string("assigningJurisdiction"));
            populateCweField(xcn.getAssigningJurisdiction(),assigningJurisdiction);
        }
        //Populate XCN.23 "Assigning Agency Or Department" component
        if(xcnValues.contains("assigningAgencyOrDepartment")){
            Json assigningAgencyOrDepartment = jsonOrValuePropertyParse("assigningAgencyOrDepartment",xcnValues.string("assigningAgencyOrDepartment"));
            populateCweField(xcn.getAssigningAgencyOrDepartment(),assigningAgencyOrDepartment);
        }
        //Populate XCN.24 " Security Check" component
        xcn.getSecurityCheck().setValue(
                xcnValues.contains("securityCheck") ? xcnValues.string("securityCheck") : ""
        );
        //Populate XCN.25 "Security Check Scheme" component
        xcn.getSecurityCheckScheme().setValue(
                xcnValues.contains("securityCheckScheme") ? xcnValues.string("securityCheckScheme") : ""
        );
    }

    public static void populateFnField(FN fn, Json fnValues) throws DataTypeException {
        fn.getSurname().setValue(
                fnValues.contains("surname") ? fnValues.string("surname") : ""
        );
        fn.getOwnSurnamePrefix().setValue(
                fnValues.contains("ownSurnamePrefix") ? fnValues.string("ownSurnamePrefix") : ""
        );
        fn.getOwnSurname().setValue(
                fnValues.contains("ownSurname") ? fnValues.string("ownSurname") : ""
        );
        fn.getSurnamePrefixFromPartnerSpouse().setValue(
                fnValues.contains("surnamePrefixFromPartnerSpouse") ? fnValues.string("surnamePrefixFromPartnerSpouse") : ""
        );
        fn.getSurnameFromPartnerSpouse().setValue(
                fnValues.contains("surnameFromPartnerSpouse") ? fnValues.string("surnameFromPartnerSpouse") : ""
        );
    }

    public static void populateFcField(FC fc, Json fcValues) throws DataTypeException {
        //Populate FC.1 "Financial Class Code" component
        if (fcValues.contains("financialClassCode")){
            Json financialClassCode = jsonOrValuePropertyParse("financialClassCode", fcValues.string("financialClassCode"));
            populateCweField(fc.getFinancialClassCode(),financialClassCode);
        }
        //Populate FC.2 "Effective Date" component
        fc.getEffectiveDate().setValue(
                fcValues.contains("effectiveDate") ? fcValues.string("effectiveDate") : ""
        );
    }

    public static void populateDldField(DLD dld, Json dldValues) throws DataTypeException {
        //Populate DLD.1 "Discharge To Location" component
        if (dldValues.contains("dischargeToLocation")){
            Json dischargeToLocation = jsonOrValuePropertyParse("dischargeToLocation", dldValues.string("dischargeToLocation"));
            populateCweField(dld.getDischargeToLocation(),dischargeToLocation);
        }
        //Populate DLD.2 "Effective Date" component
        dld.getEffectiveDate().setValue(
                dldValues.contains("effectiveDate") ? dldValues.string("effectiveDate") : ""
        );
    }

    public static void populateAdField(AD ad, Json adValues) throws DataTypeException {
        //Populate AD.1 "Street Address"
        ad.getStreetAddress().setValue(
                adValues.contains("streetAddress") ? adValues.string("streetAddress") : ""
        );
        //Populate AD.2 "Other Designation"
        ad.getOtherDesignation().setValue(
                adValues.contains("otherDesignation") ? adValues.string("otherDesignation") : ""
        );
        //Populate ad.3 "City"
        ad.getCity().setValue(
                adValues.contains("city") ? adValues.string("city") : ""
        );
        //Populate ad.4 "State Or Province"
        ad.getStateOrProvince().setValue(
                adValues.contains("stateOrProvince") ? adValues.string("stateOrProvince") : ""
        );
        //Populate ad.5 "Zip Or Postal Code"
        ad.getZipOrPostalCode().setValue(
                adValues.contains("zipOrPostalCode") ? adValues.string("zipOrPostalCode") : ""
        );
        //Populate ad.6 "Country"
        ad.getCountry().setValue(
                adValues.contains("country") ? adValues.string("country") : ""
        );
        //Populate ad.7 "Address Type"
        ad.getAddressType().setValue(
                adValues.contains("addressType") ? adValues.string("addressType") : ""
        );
        //Populate ad.8 "Other Geographic Designation"
        ad.getOtherGeographicDesignation().setValue(
                adValues.contains("otherGeographicDesignation") ? adValues.string("otherGeographicDesignation") : ""
        );
    }
    public static void populateCneField(CNE cne, Json cneValues) throws DataTypeException {
        //Populate CNE.1 "Identifier"
        cne.getIdentifier().setValue(
                cneValues.contains("identifier") ? cneValues.string("identifier") : cneValues.contains("mainValue") ? cneValues.string("mainValue") : ""
        );
        //Populate CNE.2 "Text"
        cne.getText().setValue(
                cneValues.contains("text") ? cneValues.string("text") : ""
        );
        //Populate CNE.3 "Name Of Coding System"
        cne.getNameOfCodingSystem().setValue(
                cneValues.contains("nameOfCodingSystem") ? cneValues.string("nameOfCodingSystem") : ""
        );
        //Populate CNE.4 "Alternate Identifier"
        cne.getAlternateIdentifier().setValue(
                cneValues.contains("alternateIdentifier") ? cneValues.string("alternateIdentifier") : ""
        );
        //Populate CNE.5 "Alternate Text"
        cne.getAlternateText().setValue(
                cneValues.contains("alternateText") ? cneValues.string("alternateText") : ""
        );
        //Populate CNE.6 "Name Of Alternate Coding System"
        cne.getNameOfAlternateCodingSystem().setValue(
                cneValues.contains("nameOfAlternateCodingSystem") ? cneValues.string("nameOfAlternateCodingSystem") : ""
        );
        //Populate CNE.7 "Coding System Version Id"
        cne.getCodingSystemVersionID().setValue(
                cneValues.contains("codingSystemVersionId") ? cneValues.string("codingSystemVersionId") : ""
        );
        //Populate CNE.8 "Alternate Coding System Version Id"
        cne.getAlternateCodingSystemVersionID().setValue(
                cneValues.contains("alternateCodingSystemVersionId") ? cneValues.string("alternateCodingSystemVersionId") : ""
        );
        //Populate CNE.9 "Original Text"
        cne.getOriginalText().setValue(
                cneValues.contains("originalText") ? cneValues.string("originalText") : ""
        );
        //Populate CNE.10 "Second Alternate Identifier"
        cne.getSecondAlternateIdentifier().setValue(
                cneValues.contains("secondAlternateIdentifier") ? cneValues.string("secondAlternateIdentifier") : ""
        );
        //Populate CNE.11 "Second Alternate Text"
        cne.getSecondAlternateText().setValue(
                cneValues.contains("secondAlternateText") ? cneValues.string("secondAlternateText") : ""
        );
        //Populate CNE.12 "Name Of Second Alternate Coding System"
        cne.getNameOfSecondAlternateCodingSystem().setValue(
                cneValues.contains("nameOfSecondAlternateCodingSystem") ? cneValues.string("nameOfSecondAlternateCodingSystem") : ""
        );
        //Populate CNE.13 "Second Alternate Coding System Version Id"
        cne.getSecondAlternateCodingSystemVersionID().setValue(
                cneValues.contains("secondAlternateCodingSystemVersionId") ? cneValues.string("secondAlternateCodingSystemVersionId") : ""
        );
        //Populate CNE.14 "Coding System Oid"
        cne.getCodingSystemOID().setValue(
                cneValues.contains("codingSystemOid") ? cneValues.string("codingSystemOid") : ""
        );
        //Populate CNE.15 "Value Set Oid"
        cne.getValueSetOID().setValue(
                cneValues.contains("valueSetOid") ? cneValues.string("valueSetOid") : ""
        );
        //Populate CNE.16 "Value Set Version Id"
        cne.getValueSetVersionID().setValue(
                cneValues.contains("valueSetVersionId") ? cneValues.string("valueSetVersionId") : ""
        );
        //Populate CNE.17 "Alternate Coding System Oid"
        cne.getAlternateCodingSystemOID().setValue(
                cneValues.contains("alternateCodingSystemOid") ? cneValues.string("alternateCodingSystemOid") : ""
        );
        //Populate CNE.18 "Alternate Value Set Oid"
        cne.getAlternateValueSetOID().setValue(
                cneValues.contains("alternateValueSetOid") ? cneValues.string("alternateValueSetOid") : ""
        );
        //Populate CNE.19 "Alternate Value Set Version Id"
        cne.getAlternateValueSetVersionID().setValue(
                cneValues.contains("alternateValueSetVersionId") ? cneValues.string("alternateValueSetVersionId") : ""
        );
        //Populate CNE.20 "Second Alternate Coding System Oid"
        cne.getSecondAlternateCodingSystemOID().setValue(
                cneValues.contains("secondAlternateCodingSystemOid") ? cneValues.string("secondAlternateCodingSystemOid") : ""
        );
        //Populate CNE.21 "Second Alternate Value Set Oid"
        cne.getSecondAlternateValueSetOID().setValue(
                cneValues.contains("secondAlternateValueSetOid") ? cneValues.string("secondAlternateValueSetOid") : ""
        );
        //Populate CNE.22 "Second Alternate Value Set Version Id"
        cne.getSecondAlternateValueSetVersionID().setValue(
                cneValues.contains("secondAlternateValueSetVersionId") ? cneValues.string("secondAlternateValueSetVersionId") : ""
        );
    }

    public static void populateCfField(CF cf, Json cfValues) throws DataTypeException {
        //Populate CF.1 "Identifier"
        cf.getIdentifier().setValue(
                cfValues.contains("identifier") ? cfValues.string("identifier") : ""
        );
        //Populate CF.2 "Formatted Text"
        cf.getFormattedText().setValue(
                cfValues.contains("formattedText") ? cfValues.string("formattedText") : ""
        );
        //Populate CF.3 "Name Of Coding System"
        cf.getNameOfCodingSystem().setValue(
                cfValues.contains("nameOfCodingSystem") ? cfValues.string("nameOfCodingSystem") : ""
        );
        //Populate CF.4 "Alternate Identifier"
        cf.getAlternateIdentifier().setValue(
                cfValues.contains("alternateIdentifier") ? cfValues.string("alternateIdentifier") : ""
        );
        //Populate CF.5 "Alternate Formatted Text"
        cf.getAlternateFormattedText().setValue(
                cfValues.contains("alternateFormattedText") ? cfValues.string("alternateFormattedText") : ""
        );
        //Populate CF.6 "Name Of Alternate Coding System"
        cf.getNameOfAlternateCodingSystem().setValue(
                cfValues.contains("nameOfAlternateCodingSystem") ? cfValues.string("nameOfAlternateCodingSystem") : ""
        );
        //Populate CF.7 "Coding System Version Id"
        cf.getCodingSystemVersionID().setValue(
                cfValues.contains("codingSystemVersionId") ? cfValues.string("codingSystemVersionId") : ""
        );
        //Populate CF.8 "Alternate Coding System Version Id"
        cf.getAlternateCodingSystemVersionID().setValue(
                cfValues.contains("alternateCodingSystemVersionId") ? cfValues.string("alternateCodingSystemVersionId") : ""
        );
        //Populate CF.9 "Original Text"
        cf.getOriginalText().setValue(
                cfValues.contains("originalText") ? cfValues.string("originalText") : ""
        );
        //Populate CF.10 "Second Alternate Identifier"
        cf.getSecondAlternateIdentifier().setValue(
                cfValues.contains("secondAlternateIdentifier") ? cfValues.string("secondAlternateIdentifier") : ""
        );
        //Populate CF.11 "Second Alternate Formatted Text"
        cf.getSecondAlternateFormattedText().setValue(
                cfValues.contains("secondAlternateFormattedText") ? cfValues.string("secondAlternateFormattedText") : ""
        );
        //Populate CF.12 "Name Of Second Alternate Coding System"
        cf.getNameOfSecondAlternateCodingSystem().setValue(
                cfValues.contains("nameOfSecondAlternateCodingSystem") ? cfValues.string("nameOfSecondAlternateCodingSystem") : ""
        );
        //Populate CF.13 "Second Alternate Coding System Version Id"
        cf.getSecondAlternateCodingSystemVersionID().setValue(
                cfValues.contains("secondAlternateCodingSystemVersionId") ? cfValues.string("secondAlternateCodingSystemVersionId") : ""
        );
        //Populate CF.14 "Coding System Oid"
        cf.getCodingSystemOID().setValue(
                cfValues.contains("codingSystemOid") ? cfValues.string("codingSystemOid") : ""
        );
        //Populate CF.15 "Value Set Oid"
        cf.getValueSetOID().setValue(
                cfValues.contains("valueSetOid") ? cfValues.string("valueSetOid") : ""
        );
        //Populate CF.16 "Value Set Version Id"
        cf.getValueSetVersionID().setValue(
                cfValues.contains("valueSetVersionId") ? cfValues.string("valueSetVersionId") : ""
        );
        //Populate CF.17 "Alternate Coding System Oid"
        cf.getAlternateCodingSystemOID().setValue(
                cfValues.contains("alternateCodingSystemOid") ? cfValues.string("alternateCodingSystemOid") : ""
        );
        //Populate CF.18 "Alternate Value Set Oid"
        cf.getAlternateValueSetOID().setValue(
                cfValues.contains("alternateValueSetOid") ? cfValues.string("alternateValueSetOid") : ""
        );
        //Populate CF.19 "Alternate Value Set Version Id"
        cf.getAlternateValueSetVersionID().setValue(
                cfValues.contains("alternateValueSetVersionId") ? cfValues.string("alternateValueSetVersionId") : ""
        );
        //Populate CF.20 "Second Alternate Coding System Oid"
        cf.getSecondAlternateCodingSystemOID().setValue(
                cfValues.contains("secondAlternateCodingSystemOid") ? cfValues.string("secondAlternateCodingSystemOid") : ""
        );
        //Populate CF.21 "Second Alternate Value Set Oid"
        cf.getSecondAlternateValueSetOID().setValue(
                cfValues.contains("secondAlternateValueSetOid") ? cfValues.string("secondAlternateValueSetOid") : ""
        );
        //Populate CF.22 "Second Alternate Value Set Version Id"
        cf.getSecondAlternateValueSetVersionID().setValue(
                cfValues.contains("secondAlternateValueSetVersionId") ? cfValues.string("secondAlternateValueSetVersionId") : ""
        );
    }

    public static void populateCpField(CP cp, Json cpValues) throws DataTypeException {
        //Populate CP.1 "Price"
        if(cpValues.contains("price")){
            Json price = jsonOrValuePropertyParse("price",cpValues.string("price"));
            populateMoField(cp.getPrice(),price);
        }
        //Populate CP.2 "Price"
        cp.getPriceType().setValue(
                cpValues.contains("priceType") ? cpValues.string("priceType") : ""
        );
        //Populate CP.3 "From Value"
        cp.getFromValue().setValue(
                cpValues.contains("fromValue") ? cpValues.string("fromValue") : ""
        );
        //Populate CP.4 "To Value"
        cp.getToValue().setValue(
                cpValues.contains("toValue") ? cpValues.string("toValue") : ""
        );
        //Populate CP.5 "Range Units"
        if(cpValues.contains("rangeUnits")){
            Json rangeUnits = jsonOrValuePropertyParse("rangeUnits",cpValues.string("rangeUnits"));
            populateCweField(cp.getRangeUnits(),rangeUnits);
        }
        //Populate CP.6 "Range Type"
        cp.getRangeType().setValue(
                cpValues.contains("rangeType") ? cpValues.string("rangeType") : ""
        );
    }

    public static void populateMoField(MO mo, Json moValues) throws DataTypeException {
        //Populate MO.1 "Quantity"
        mo.getQuantity().setValue(
                moValues.contains("quantity") ? moValues.string("quantity") : ""
        );
        //Populate MO.2 "Denomination"
        mo.getDenomination().setValue(
                moValues.contains("denomination") ? moValues.string("denomination") : ""
        );
    }

    public static void populateDrField(DR dr, Json drValues) throws DataTypeException {
        //Populate DR.1 "Range Start Date/Time"
        dr.getRangeStartDateTime().setValue(
                drValues.contains("rangeStartDateTime") ? drValues.string("rangeStartDateTime") : drValues.contains("mainValue") ? drValues.string("mainValue") : ""
        );
        //Populate DR.2 "Range End Date/Time"
        dr.getRangeEndDateTime().setValue(
                drValues.contains("rangeEndDateTime") ? drValues.string("rangeEndDateTime") : ""
        );
    }

    public static void populateEdField(ED ed, Json edValues) throws DataTypeException {
        //Populate ED.1 "Source Application"
        if(edValues.contains("sourceApplication")){
            Json sourceApplication = jsonOrValuePropertyParse("sourceApplication", edValues.string("sourceApplication"));
            populateHdField(ed.getSourceApplication(),sourceApplication);
        }
        //Populate ED.2 "Type Of Data"
        ed.getTypeOfData().setValue(
                edValues.contains("typeOfData") ? edValues.string("typeOfData") : ""
        );
        //Populate ED.3 "Data Subtype"
        ed.getDataSubtype().setValue(
                edValues.contains("dataSubtype") ? edValues.string("dataSubtype") : ""
        );
        //Populate DR.4 "Encoding"
        ed.getEncoding().setValue(
                edValues.contains("encoding") ? edValues.string("encoding") : ""
        );
        //Populate DR.5 "Data"
        ed.getData().setValue(
                edValues.contains("data") ? edValues.string("data") : ""
        );
    }

    public static void populateMaField(MA ma, Json maValues) throws DataTypeException {
        //Populate MA.1 "Sample Y From Channel 1"
        ma.getSampleYFromChannel1().setValue(
                maValues.contains("sampleYFromChannel1") ? maValues.string("sampleYFromChannel1") : ""
        );
        //Populate MA.2 "Sample Y From Channel 2"
        ma.getSampleYFromChannel2().setValue(
                maValues.contains("sampleYFromChannel2") ? maValues.string("sampleYFromChannel2") : ""
        );
        //Populate MA.3 "Sample Y From Channel 3"
        ma.getSampleYFromChannel3().setValue(
                maValues.contains("sampleYFromChannel3") ? maValues.string("sampleYFromChannel3") : ""
        );
        //Populate MA.4 "Sample Y From Channel 4"
        ma.getSampleYFromChannel4().setValue(
                maValues.contains("sampleYFromChannel4") ? maValues.string("sampleYFromChannel4") : ""
        );
    }

    public static void populateNaField(NA na, Json naValues) throws DataTypeException {
        //Populate NA.1 "Value 1"
        na.getValue1().setValue(
                naValues.contains("value1") ? naValues.string("value1") : ""
        );
        //Populate NA.2 "Value 2"
        na.getValue2().setValue(
                naValues.contains("sampleYFromChannel2") ? naValues.string("sampleYFromChannel2") : ""
        );
        //Populate NA.3 "Value 3"
        na.getValue3().setValue(
                naValues.contains("sampleYFromChannel3") ? naValues.string("sampleYFromChannel3") : ""
        );
        //Populate NA.4 "Value 4"
        na.getValue4().setValue(
                naValues.contains("sampleYFromChannel4") ? naValues.string("sampleYFromChannel4") : ""
        );

    }

    public static void populateRpField(RP rp, Json rpValues) throws DataTypeException {
        //Populate RP.1 "Pointer"
        rp.getPointer().setValue(
                rpValues.contains("pointer") ? rpValues.string("pointer") : ""
        );
        //Populate RP.2 "Application Id"
        if(rpValues.contains("applicationId")){
            Json applicationId = jsonOrValuePropertyParse("applicationId",rpValues.string("applicationId"));
            populateHdField(rp.getApplicationID(),applicationId);
        }
        //Populate RP.3 "Type Of Data"
        rp.getTypeOfData().setValue(
                rpValues.contains("typeOfData") ? rpValues.string("typeOfData") : ""
        );
        //Populate RP.4 "Subtype"
        rp.getSubtype().setValue(
                rpValues.contains("subtype") ? rpValues.string("subtype") : ""
        );
    }

    public static void populateSnField(SN sn, Json snValues) throws DataTypeException {
        //Populate SN.1 "Comparator"
        sn.getComparator().setValue(
                snValues.contains("comparator") ? snValues.string("comparator") : ""
        );
        //Populate SN.2 "Num1"
        sn.getNum1().setValue(
                snValues.contains("num1") ? snValues.string("num1") : ""
        );
        //Populate SN.3 "Separator/Suffix"
        sn.getSeparatorSuffix().setValue(
                snValues.contains("separatorSuffix") ? snValues.string("separatorSuffix") : ""
        );
        //Populate RP.4 "Num2"
        sn.getNum2().setValue(
                snValues.contains("num2") ? snValues.string("subtype") : ""
        );
    }

    public static void populateAuiField(AUI aui, Json auiValues) throws DataTypeException {
        //Populate AUI.1 "Authorization Number"
        aui.getAuthorizationNumber().setValue(
                auiValues.contains("authorizationNumber") ? auiValues.string("authorizationNumber") : ""
        );
        //Populate AUI.2 "Date"
        aui.getDate().setValue(
                auiValues.contains("date") ? auiValues.string("date") : ""
        );
        //Populate AUI.3 "Source"
        aui.getSource().setValue(
                auiValues.contains("source") ? auiValues.string("source") : ""
        );
    }
    public static void populateRmcField(RMC rmc, Json rmcValues) throws DataTypeException {
        //Populate RMC.1 "Room Type"
        if(rmcValues.contains("roomType")){
            Json roomType = jsonOrValuePropertyParse("roomType",rmcValues.string("roomType"));
            populateCweField(rmc.getRoomType(),roomType);
        }
        //Populate RMC.2 "Amount Type"
        if(rmcValues.contains("amountType")){
            Json amountType = jsonOrValuePropertyParse("amountType",rmcValues.string("amountType"));
            populateCweField(rmc.getAmountType(),amountType);
        }
        //Populate RMC.3 "Coverage Amount"
        rmc.getCoverageAmount().setValue(
                rmcValues.contains("coverageAmount") ? rmcValues.string("coverageAmount") : ""
        );
        //Populate RMC.4 "Money Or Percentage"
        if(rmcValues.contains("moneyOrPercentage")){
            Json moneyOrPercentage = jsonOrValuePropertyParse("moneyOrPercentage",rmcValues.string("moneyOrPercentage"));
            populateMopField(rmc.getMoneyOrPercentage(),moneyOrPercentage);
        }
    }

    public static void populateMopField(MOP mop, Json mopValues) throws DataTypeException {
        //Populate MOP.1 "Money Or Percentage Indicator"
        mop.getMoneyOrPercentageIndicator().setValue(
                mopValues.contains("moneyOrPercentageIndicator") ? mopValues.string("moneyOrPercentageIndicator") : ""
        );
        //Populate MOP.2 "Money Or Percentage Quantity"
        mop.getMoneyOrPercentageQuantity().setValue(
                mopValues.contains("moneyOrPercentageQuantity") ? mopValues.string("moneyOrPercentageQuantity") : ""
        );
        //Populate MOP.3 "Monetary Denomination"
        mop.getMonetaryDenomination().setValue(
                mopValues.contains("monetaryDenomination") ? mopValues.string("monetaryDenomination") : ""
        );
    }

    public static void populatePtaField(PTA pta, Json ptaValues) throws DataTypeException {
        //Populate PTA.1 "Policy Type"
        if(ptaValues.contains("policyType")){
            Json policyType = jsonOrValuePropertyParse("policyType",ptaValues.string("policyType"));
            populateCweField(pta.getPolicyType(),policyType);
        }
        //Populate PTA.2 "Amount Class"
        if(ptaValues.contains("amountClass")){
            Json amountClass = jsonOrValuePropertyParse("amountClass",ptaValues.string("amountClass"));
            populateCweField(pta.getPolicyType(),amountClass);
        }
        //Populate PTA.3 "Money Or Percentage Quantity"
        pta.getMoneyOrPercentageQuantity().setValue(
                ptaValues.contains("moneyOrPercentageQuantity") ? ptaValues.string("monetaryDenomination") : ""
        );
        //Populate PTA.4 "Money Or Percentage"
        if(ptaValues.contains("moneyOrPercentage")){
            Json moneyOrPercentage = jsonOrValuePropertyParse("moneyOrPercentage",ptaValues.string("moneyOrPercentage"));
            populateMopField(pta.getMoneyOrPercentage(),moneyOrPercentage);
        }
    }

    public static void populateDdiField(DDI ddi, Json ddiValues) throws DataTypeException {
        //Populate DDI.1 "Delay Days"
        ddi.getDelayDays().setValue(
                ddiValues.contains("delayDays") ? ddiValues.string("delayDays") : ""
        );
        //Populate DDI.2 "Monetary Amount"
        if(ddiValues.contains("monetaryAmount")){
            Json monetaryAmount = jsonOrValuePropertyParse("monetaryAmount",ddiValues.string("monetaryAmount"));
            populateMoField(ddi.getMonetaryAmount(),monetaryAmount);
        }
        //Populate DDI.3 "Number Of Days"
        ddi.getNumberOfDays().setValue(
                ddiValues.contains("numberOfDays") ? ddiValues.string("numberOfDays") : ""
        );
    }

    public static void populateDtnField(DTN dtn, Json dtnValues) throws DataTypeException {
        //Populate DTN.1 "Day Type"
        if(dtnValues.contains("dayType")){
            Json dayType = jsonOrValuePropertyParse("dayType",dtnValues.string("dayType"));
            populateCweField(dtn.getDayType(),dayType);
        }
        //Populate DTN.2 - Number Of Days
        dtn.getNumberOfDays().setValue(
                dtnValues.contains("numberOfDays") ? dtnValues.string("numberOfDays") : ""
        );
    }

    public static void populateIcdField(ICD icd, Json icdValues) throws DataTypeException {
        //Populate ICD.1 - Certification Patient Type
        if(icdValues.contains("certificationPatientType")){
            Json certificationPatientType = jsonOrValuePropertyParse("certificationPatientType",icdValues.string("certificationPatientType"));
            populateCweField(icd.getCertificationPatientType(),certificationPatientType);
        }
        //Populate ICD.2 - Certification Required
        icd.getCertificationRequired().setValue(
                icdValues.contains("certificationRequired") ? icdValues.string("certificationRequired") : ""
        );
        //Populate ICD.3 - Date/Time Certification Required
        icd.getDateTimeCertificationRequired().setValue(
                icdValues.contains("dateTimeCertificationRequired") ? icdValues.string("dateTimeCertificationRequired") : ""
        );
    }

    public static void populateCqField(CQ cq, Json cqValues) throws DataTypeException {
        //Populate CQ.1 - Quantity
        cq.getQuantity().setValue(
                cqValues.contains("quantity") ? cqValues.string("quantity") : ""
        );
        //Populate CQ.2 - Units
        if(cqValues.contains("units")){
            Json units = jsonOrValuePropertyParse("units",cqValues.string("units"));
            populateCweField(cq.getUnits(),units);
        }
    }

    public static void populateVariesDataType(AbstractMessage message, String dataType, Varies varies, Json variesValues) throws DataTypeException {
        switch (dataType){
            case "AD":
                AD ad = new AD(message);
                populateAdField(ad, variesValues);
                varies.setData(ad);
                break;
            case "XAD":
                XAD xad = new XAD(message);
                populateXadField(xad, variesValues);
                varies.setData(xad);
                break;
            case "CF":
                CF cf = new CF(message);
                populateCfField(cf, variesValues);
                varies.setData(cf);
                break;
            case "CK":
                CK ck = new CK(message);
                //populateCkField(ck, variesValues);
                varies.setData(ck);
                break;
            case "CN":
                CN cn = new CN(message);
                //populateCnField(cn, variesValues);
                varies.setData(cn);
                break;
            case "CNE":
                CNE cne = new CNE(message);
                populateCneField(cne, variesValues);
                varies.setData(cne);
                break;
            case "CP":
                CP cp = new CP(message);
                populateCpField(cp, variesValues);
                varies.setData(cp);
                break;
            case "CWE":
                CWE cwe = new CWE(message);
                populateCweField(cwe, variesValues);
                varies.setData(cwe);
                break;
            case "CX":
                CX cx = new CX(message);
                populateCxField(cx, variesValues);
                varies.setData(cx);
                break;
            case "DR":
                DR dr = new DR(message);
                populateDrField(dr, variesValues);
                varies.setData(dr);
                break;
            case "DT":
                DT dt = new DT(message);
                dt.setValue(variesValues.string("mainValue"));
                varies.setData(dt);
                break;
            case "DTM":
                DTM dtm = new DTM(message);
                dtm.setValue(variesValues.string("mainValue"));
                varies.setData(dtm);
                break;
            case "ED":
                ED ed = new ED(message);
                populateEdField(ed, variesValues);
                varies.setData(ed);
                break;
            case "FT":
                FT ft = new FT(message);
                ft.setValue(variesValues.string("mainValue"));
                varies.setData(ft);
                break;
            case "ID":
                ID id = new ID(message);
                id.setValue(variesValues.string("mainValue"));;
                varies.setData(id);
                break;
            case "IS":
                IS is = new IS(message);
                is.setValue(variesValues.string("mainValue"));;
                varies.setData(is);
                break;
            case "MA":
                MA ma = new MA(message);
                populateMaField(ma, variesValues);
                varies.setData(ma);
                break;
            case "MO":
                MO mo = new MO(message);
                populateMoField(mo, variesValues);
                varies.setData(mo);
                break;
            case "NA":
                NA na = new NA(message);
                populateNaField(na, variesValues);
                varies.setData(na);
                break;
            case "NM":
                NM nm = new NM(message);
                nm.setValue(variesValues.string("mainValue"));
                varies.setData(nm);
                break;
            case "PN":
            case "XPN":
                XPN xpn = new XPN(message);
                populateXpnField(xpn, variesValues);
                varies.setData(xpn);
                break;
            case "RP":
                RP rp = new RP(message);
                populateRpField(rp, variesValues);
                varies.setData(rp);
                break;
            case "SN":
                SN sn = new SN(message);
                populateSnField(sn, variesValues);
                varies.setData(sn);
                break;
            case "ST":
                ST st = new ST(message);
                st.setValue(variesValues.string("mainValue"));
                varies.setData(st);
                break;
            case "TM":
                TM tm = new TM(message);
                tm.setValue(variesValues.string("mainValue"));
                varies.setData(tm);
                break;
            case "TN":
            case "XTN":
                XTN xtn = new XTN(message);
                populateXtnField(xtn, variesValues);
                varies.setData(xtn);
                break;
            case "TX":
                TX tx = new TX(message);
                tx.setValue(variesValues.string("mainValue"));
                varies.setData(tx);
                break;
            case "XCN":
                XCN xcn = new XCN(message);
                populateXcnField(xcn, variesValues);
                varies.setData(xcn);
                break;
            case "XON":
                XON xon = new XON(message);
                populateXonField(xon, variesValues);
                varies.setData(xon);
                break;
        }
    }
}
