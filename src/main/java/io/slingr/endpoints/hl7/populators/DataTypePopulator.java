package io.slingr.endpoints.hl7.populators;

import ca.uhn.hl7v2.model.AbstractMessage;
import ca.uhn.hl7v2.model.DataTypeException;
import ca.uhn.hl7v2.model.Varies;
import ca.uhn.hl7v2.model.v24.datatype.CK;
import ca.uhn.hl7v2.model.v24.datatype.CN;
import ca.uhn.hl7v2.model.v281.datatype.*;
import io.slingr.endpoints.exceptions.EndpointException;
import io.slingr.endpoints.exceptions.ErrorCode;
import io.slingr.endpoints.utils.Json;

import static io.slingr.endpoints.hl7.jsonHelper.JsonHelper.jsonOrValuePropertyParse;
import static io.slingr.endpoints.hl7.jsonHelper.JsonHelper.singleJsonPropertyParse;

public class DataTypePopulator {
    
    static String parentProp;

    public static void populateCweField(CWE cwe, Json cweValues) throws DataTypeException {
        for (String key : cweValues.keys()) {
            String propValue = cweValues.string(key);

            switch (key) {
                case "identifier":
                case "mainValue":
                    cwe.getIdentifier().setValue(propValue);
                    break;
                case "text":
                    cwe.getText().setValue(propValue);
                    break;
                case "nameOfCodingSystem":
                    cwe.getNameOfCodingSystem().setValue(propValue);
                    break;
                case "alternateIdentifier":
                    cwe.getAlternateIdentifier().setValue(propValue);
                    break;
                case "alternateText":
                    cwe.getAlternateText().setValue(propValue);
                    break;
                case "nameOfAlternateCodingSystem":
                    cwe.getNameOfAlternateCodingSystem().setValue(propValue);
                    break;
                case "codingSystemVersionId":
                    cwe.getCodingSystemVersionID().setValue(propValue);
                    break;
                case "alternateCodingSystemVersionID":
                    cwe.getAlternateCodingSystemVersionID().setValue(propValue);
                    break;
                case "originalText":
                    cwe.getOriginalText().setValue(propValue);
                    break;
                case "secondAlternateIdentifier":
                    cwe.getSecondAlternateIdentifier().setValue(propValue);
                    break;
                case "secondAlternateText":
                    cwe.getSecondAlternateText().setValue(propValue);
                    break;
                case "nameOfSecondAlternateCodingSystem":
                    cwe.getNameOfSecondAlternateCodingSystem().setValue(propValue);
                    break;
                case "secondAlternateCodingSystemVersionID":
                    cwe.getSecondAlternateCodingSystemVersionID().setValue(propValue);
                    break;
                case "codingSystemOID":
                    cwe.getCodingSystemOID().setValue(propValue);
                    break;
                case "valueSetOID":
                    cwe.getValueSetOID().setValue(propValue);
                    break;
                case "valueSetVersionID":
                    cwe.getValueSetVersionID().setValue(propValue);
                    break;
                case "alternateCodingSystemOID":
                    cwe.getAlternateCodingSystemOID().setValue(propValue);
                    break;
                case "AlternateValueSetOID":
                    cwe.getAlternateValueSetOID().setValue(propValue);
                    break;
                case "alternateValueSetVersionID":
                    cwe.getAlternateValueSetVersionID().setValue(propValue);
                    break;
                case "secondAlternateCodingSystemOID":
                    cwe.getSecondAlternateCodingSystemOID().setValue(propValue);
                    break;
                case "secondAlternateValueSetOID":
                    cwe.getSecondAlternateValueSetOID().setValue(propValue);
                    break;
                case "secondAlternateValueSetVersionID":
                    cwe.getSecondAlternateValueSetVersionID().setValue(propValue);
                    break;
                default:
                    throw EndpointException.permanent(ErrorCode.ARGUMENT, "The property ['" + key + "'] does not correspond with any possible CWE field");
            }
        }


    }

    public static void populateXonField(XON xon, Json xonValues) throws DataTypeException{
        for (String key : xonValues.keys()) {
            String propPath = parentProp + "." + key;
            String propValue = xonValues.string(key);

            switch (key) {
                //Populate XON.1 "Organization Name" component
                case "organizationName":
                case "mainValue":
                    xon.getOrganizationName().setValue(propValue);
                    break;
                //Populate XON.2 "Organization Name Type Code" component
                case "organizationNameTypeCode":
                    Json organizationNameTypeCode = jsonOrValuePropertyParse(propPath, propValue);
                    populateCweField(xon.getOrganizationNameTypeCode(),organizationNameTypeCode);
                    break;
                //Populate XON.3 "Id Number" withdrawn
                //Populate XON.4 "Identifier Check Digit" withdrawn
                //Populate XON.5 "Check Digit Scheme" withdrawn
                //Populate XON.6 "Assigning Authority" component
                case "assigningAuthority":
                    Json assigningAuthority = jsonOrValuePropertyParse(propPath, propValue);
                    populateHdField(xon.getAssigningAuthority(),assigningAuthority);
                    break;
                //Populate XON.7 "Identifier Type Code" component
                case "identifierTypeCode":
                    xon.getIdentifierTypeCode().setValue(propValue);
                    break;
                //Populate XON.8 "Assigning Facility" component
                case "assigningFacility":
                    Json assigningFacility = jsonOrValuePropertyParse(propPath, propValue);
                    populateHdField(xon.getAssigningFacility(),assigningFacility);
                    break;
                //Populate XON.9 "Name Representation Code" component
                case "nameRepresentationCode":
                    xon.getNameRepresentationCode().setValue(propValue);
                    break;
                //Populate XON.10 "Organization Identifier" component
                case "organizationIdentifier":
                    xon.getOrganizationIdentifier().setValue(propValue);
                    break;
                default:
                    throw EndpointException.permanent(ErrorCode.ARGUMENT, "The property ['" + key + "'] does not correspond with any possible XON field");
            }
        }
    }

    public static void populateHdField(HD hd, Json hdValues) throws DataTypeException {
        for (String key : hdValues.keys()) {
            String propPath = parentProp + "." + key;
            String propValue = hdValues.string(key);

            switch (key) {
                case "mainValue":
                    hd.getNamespaceID().setValue(propValue);
                    break;
                case "universalId":
                    hd.getUniversalID().setValue(propValue);
                    break;
                case "universalIdType":
                    hd.getUniversalIDType().setValue(propValue);
                    break;
                default:
                    throw EndpointException.permanent(ErrorCode.ARGUMENT, "The property ['" + propPath + "'] does not correspond with any possible HD field");
            }
        }

    }



    public static void populateCxField(CX cx, Json cxValues) throws DataTypeException {
        for (String key : cxValues.keys()) {
            String propPath = parentProp + "." + key;
            String propValue = cxValues.string(key);

            switch (key) {
                case "idNumber":
                    cx.getIDNumber().setValue(propValue);
                    break;
                case "identifierCheckDigit":
                    cx.getIdentifierCheckDigit().setValue(propValue);
                    break;
                case "checkDigitScheme":
                    cx.getCheckDigitScheme().setValue(propValue);
                    break;
                case "assigningAuthority":
                    Json assigningAuthority = jsonOrValuePropertyParse(propPath, propValue);
                    populateHdField(cx.getAssigningAuthority(), assigningAuthority);
                    break;
                case "identifierTypeCode":
                    cx.getIdentifierTypeCode().setValue(propValue);
                    break;
                case "assigningFacility":
                    Json assigningFacility = jsonOrValuePropertyParse(propPath, propValue);
                    populateHdField(cx.getAssigningFacility(), assigningFacility);
                    break;
                case "effectiveDate":
                    cx.getEffectiveDate().setValue(propValue);
                    break;
                case "expirationDate":
                    cx.getExpirationDate().setValue(propValue);
                    break;
                case "assigningJurisdiction":
                    Json assigningJurisdiction = jsonOrValuePropertyParse(propPath, propValue);
                    populateCweField(cx.getAssigningJurisdiction(),assigningJurisdiction);
                    break;
                case "assigningAgencyOrDepartment":
                    Json assigningAgencyOrDepartment = jsonOrValuePropertyParse(propPath, propValue);
                    populateCweField(cx.getAssigningAgencyOrDepartment(),assigningAgencyOrDepartment);
                    break;
                case "securityCheck":
                    cx.getSecurityCheck().setValue(propValue);
                    break;
                case "securityCheckScheme":
                    cx.getSecurityCheckScheme().setValue(propValue);
                    break;
                default:
                    throw EndpointException.permanent(ErrorCode.ARGUMENT, "The property ['" + propPath + "'] does not correspond with any possible CX field");
            }
        }
    }

    public static void populateXpnField(XPN xpn, Json xpnValues) throws DataTypeException {
        for (String key : xpnValues.keys()) {
            String propPath = parentProp + "." + key;
            String propValue = xpnValues.string(key);

            switch (key) {
                case "mainValue":
                case "familyName":
                    Json familyName = jsonOrValuePropertyParse(propPath,propValue);
                    populateFnField(xpn.getFamilyName(),familyName);
                case "givenName":
                    xpn.getGivenName().setValue(propValue);
                    break;
                case "secondAndFurtherGivenNames":
                    xpn.getSecondAndFurtherGivenNamesOrInitialsThereof().setValue(propValue);
                    break;
                case "suffix":
                    xpn.getSuffixEgJRorIII().setValue(propValue);
                    break;
                case "prefix":
                    xpn.getPrefixEgDR().setValue(propValue);
                    break;
                case "degree":
                    xpn.getDegreeEgMD().setValue(propValue);
                    break;
                case "nameTypeCode":
                    xpn.getNameTypeCode().setValue(propValue);
                    break;
                case "nameRepresentationCode":
                    xpn.getNameRepresentationCode().setValue(propValue);
                    break;
                case "nameContext":
                    Json nameContext = jsonOrValuePropertyParse(propPath, propValue);
                    populateCweField(xpn.getNameContext(),nameContext);
                    break;
                //PID.5.10 "Name Validity Range" Withdrawn
                case "nameAssemblyOrder":
                    xpn.getNameAssemblyOrder().setValue(propValue);
                    break;
                case "effectiveDate":
                    xpn.getEffectiveDate().setValue(propValue);
                    break;
                case "expirationDate":
                    xpn.getExpirationDate().setValue(propValue);
                    break;
                case "professionalSuffix":
                    xpn.getProfessionalSuffix().setValue(propValue);
                    break;
                case "calledBy":
                    xpn.getCalledBy().setValue(propValue);
                    break;
                default:
                    throw EndpointException.permanent(ErrorCode.ARGUMENT, "The property ['" + propPath + "'] does not correspond with any possible XPN field");
            }
        }

    }

    public static void populateXadField(XAD xad, Json xadValues) throws DataTypeException {
        for (String key : xadValues.keys()) {
            String propPath = parentProp + "." + key;
            String propValue = xadValues.string(key);

            switch (key) {
                //Populate XAD.1 "Street Address"
                case "streetAddress":
                case "mainValue":
                    Json streetAddress = jsonOrValuePropertyParse(propPath, propValue);
                    populateSadField(xad.getStreetAddress(),streetAddress);
                    break;
                //Populate XAD.2 "Other Designation"
                case "otherDesignation":
                    xad.getOtherDesignation().setValue(propValue);
                    break;
                //Populate XAD.3 "City"
                case "city":
                    xad.getCity().setValue(propValue);
                    break;
                //Populate XAD.4 "State Or Province"
                case "stateOrProvince":
                    xad.getStateOrProvince().setValue(propValue);
                    break;
                //Populate XAD.5 "Zip Or Postal Code"
                case "zipOrPostalCode":
                    xad.getZipOrPostalCode().setValue(propValue);
                    break;
                //Populate XAD.6 "Country"
                case "country":
                    xad.getCountry().setValue(propValue);
                    break;
                //Populate XAD.7 "Address Type"
                case "addressType":
                    xad.getAddressType().setValue(propValue);
                    break;
                //Populate XAD.8 "Other Geographic Designation"
                case "otherGeographicDesignation":
                    xad.getOtherGeographicDesignation().setValue(propValue);
                    break;
                //Populate XAD.9 "County/Parish Code"
                case "countryParishCode":
                    Json countyParishCode = jsonOrValuePropertyParse(propPath, propValue);
                    populateCweField(xad.getCountyParishCode(),countyParishCode);
                    break;
                //Populate XAD.10 "Census Tract"
                case "censusTract":
                    Json censusTract = jsonOrValuePropertyParse(propPath, propValue);
                    populateCweField(xad.getCensusTract(),censusTract);
                    break;
                //Populate XAD.11 "Address Representation Code"
                case "addressRepresentationCode":
                    xad.getAddressRepresentationCode().setValue(propValue);
                    break;
                //Populate XAD.12 "Address Validity Range"
                case "addressValidityRange":
                    xad.getAddressValidityRange().setValue(propValue);
                    break;
                //Populate XAD.13 "Effective Date"
                case "effectiveDate":
                    xad.getEffectiveDate().setValue(propValue);
                    break;
                //Populate XAD.14 "Expiration Date"
                case "expirationDate":
                    xad.getExpirationDate().setValue(propValue);
                    break;
                //Populate XAD.15 "Expiration Reason"
                case "expirationReason":
                    Json expirationReason = jsonOrValuePropertyParse(propPath, propValue);
                    populateCweField(xad.getExpirationReason(),expirationReason);
                    break;
                //Populate XAD.16 "Temporary Indicator"
                case "temporaryIndicator":
                    xad.getTemporaryIndicator().setValue(propValue);
                    break;
                //Populate XAD.17 "Bad Address Indicator"
                case "badAddressIndicator":
                    xad.getBadAddressIndicator().setValue(propValue);
                    break;
                //Populate XAD.18 "Address Usage"
                case "addressUsage":
                    xad.getAddressUsage().setValue(propValue);
                    break;
                //Populate XAD.19 "Addressee"
                case "addressee":
                    xad.getAddressee().setValue(propValue);
                    break;
                //Populate XAD.20 "Comment"
                case "comment":
                    xad.getComment().setValue(propValue);
                    break;
                //Populate XAD.21 "Comment"
                case "preferenceOrder":
                    xad.getPreferenceOrder().setValue(propValue);
                    break;
                //Populate XAD.22 "Protection Code"
                case "protectionCode":
                    Json protectionCode = jsonOrValuePropertyParse(propPath, propValue);
                    populateCweField(xad.getProtectionCode(),protectionCode);
                    break;
                //Populate XAD.23 "Address Identifier"
                case "addressIdentifier":
                    Json addressIdentifier = jsonOrValuePropertyParse(propPath, propValue);
                    populateEiField(xad.getAddressIdentifier(),addressIdentifier);
                    break;
                default:
                    throw EndpointException.permanent(ErrorCode.ARGUMENT, "The property ['" + propPath + "'] does not correspond with any possible XAD field");
            }
        }
    }


    public static void populateSadField(SAD sad, Json sadValues) throws DataTypeException {
        for (String key : sadValues.keys()) {
            String propPath = parentProp + "." + key;
            String propValue = sadValues.string(key);

            switch (key) {
                case "streetOrMailingAddress":
                    sad.getStreetOrMailingAddress().setValue(propValue);
                    break;
                case "streetName":
                    sad.getStreetName().setValue(propValue);
                    break;
                case "dwellingNumber":
                    sad.getDwellingNumber().setValue(propValue);
                    break;
                default:
                    throw EndpointException.permanent(ErrorCode.ARGUMENT, "The property ['" + propPath + "'] does not correspond with any possible SAD field");
            }
        }
    }

    public static void populateXtnField(XTN xtn, Json xtnValues) throws DataTypeException {
        for (String key : xtnValues.keys()) {
            String propPath = parentProp + "." + key;
            String propValue = xtnValues.string(key);

            switch (key) {
                case "telephoneNumber":
                    xtn.getTelephoneNumber().setValue(propValue);
                    break;
                case "telecommunicationUseCode":
                    xtn.getTelecommunicationUseCode().setValue(propValue);
                    break;
                case "telecommunicationEquipmentType":
                    xtn.getTelecommunicationEquipmentType().setValue(propValue);
                    break;
                case "communicationAddress":
                    xtn.getCommunicationAddress().setValue(propValue);
                    break;
                case "countryCode":
                    xtn.getCountryCode().setValue(propValue);
                    break;
                case "areaCityCode":
                    xtn.getAreaCityCode().setValue(propValue);
                    break;
                case "localNumber":
                    xtn.getLocalNumber().setValue(propValue);
                    break;
                case "extension":
                    xtn.getExtension().setValue(propValue);
                    break;
                case "anyText":
                    xtn.getAnyText().setValue(propValue);
                    break;
                case "extensionPrefix":
                    xtn.getExtensionPrefix().setValue(propValue);
                    break;
                case "speedDialCode":
                    xtn.getSpeedDialCode().setValue(propValue);
                    break;
                case "unformattedTelephoneNumber":
                    xtn.getUnformattedTelephoneNumber().setValue(propValue);
                    break;
                case "effectiveStartDate":
                    xtn.getEffectiveStartDate().setValue(propValue);
                    break;
                case "expirationDate":
                    xtn.getExpirationDate().setValue(propValue);
                    break;
                case "expirationReason":
                    Json expirationReason = jsonOrValuePropertyParse(propPath, propValue);
                    populateCweField(xtn.getExpirationReason(),expirationReason);
                    break;
                case "protectionCode":
                    Json protectionCode = jsonOrValuePropertyParse(propPath, propValue);
                    populateCweField(xtn.getProtectionCode(),protectionCode);
                    break;
                case "sharedTelecommunicationIdentifier":
                    Json sharedTelecommunicationIdentifier = jsonOrValuePropertyParse(propPath, propValue);
                    populateEiField(xtn.getSharedTelecommunicationIdentifier(),sharedTelecommunicationIdentifier);
                    break;
                case "preferenceOrder":
                    xtn.getPreferenceOrder().setValue(propValue);
                    break;
                default:
                    throw EndpointException.permanent(ErrorCode.ARGUMENT, "The property ['" + propPath + "'] does not correspond with any possible XTN field");
            }
        }
    }

    public static void populateJccField(JCC jcc, Json jccValues) throws DataTypeException {
        for (String key : jccValues.keys()) {
            String propPath = parentProp + "." + key;
            String propValue = jccValues.string(key);

            switch (key) {
                //Populate JCC.1 "Job Code" subcomponent
                case "jobCode":
                    Json jobCode = jsonOrValuePropertyParse(propPath, propValue);
                    populateCweField(jcc.getJobCode(),jobCode);
                    break;
                //Populate JCC.2 "Job Class" subcomponent
                case "jobClass":
                    Json jobClass = jsonOrValuePropertyParse(propPath, propValue);
                    populateCweField(jcc.getJobClass(),jobClass);
                    break;
                //Populate JCC.3 "Job Description Text" subcomponent
                case "jobDescriptionText":
                    jcc.getJobDescriptionText().setValue(propValue);
                    break;
                default:
                    throw EndpointException.permanent(ErrorCode.ARGUMENT, "The property ['" + propPath + "'] does not correspond with any possible XTN field");
            }
        }
    }

    public static void populatePlField(PL pl, Json plValues) throws DataTypeException {
        for (String key : plValues.keys()) {
            String propPath = parentProp + "." + key;
            String propValue = plValues.string(key);

            switch (key) {
                //Populate PL.1 "Point Of Care" subcomponent
                case "pointOfCare":
                    Json pointOfCare = jsonOrValuePropertyParse(propPath, propValue);
                    populateHdField(pl.getPointOfCare(), pointOfCare);
                    break;
                //Populate PL.2 "Room" subcomponent
                case "room":
                    Json room = jsonOrValuePropertyParse(propPath, propValue);
                    populateHdField(pl.getRoom(), room);
                    break;
                //Populate PL.3 "Bed" subcomponent
                case "bed":
                    Json bed = jsonOrValuePropertyParse(propPath, propValue);
                    populateHdField(pl.getBed(), bed);
                    break;
                //Populate PL.4 "Facility" subcomponent
                case "facility":
                    Json facility = jsonOrValuePropertyParse(propPath, propValue);
                    populateHdField(pl.getFacility(), facility);
                    break;
                //Populate PL.5 "Location Status" subcomponent
                case "locationStatus":
                    pl.getLocationStatus().setValue(propValue);
                    break;
                //Populate PL.6 "Person Location Type" subcomponent
                case "mainValue":
                    pl.getPersonLocationType().setValue(propValue);
                    break;
                //Populate PL.7 "Building" subcomponent
                case "building":
                    Json building = jsonOrValuePropertyParse(propPath, propValue);
                    populateHdField(pl.getBuilding(), building);
                    break;
                //Populate PL.8 "Floor" subcomponent
                case "floor":
                    Json floor = jsonOrValuePropertyParse(propPath, propValue);
                    populateHdField(pl.getFloor(), floor);
                    break;
                //Populate PL.9 "Location Description" subcomponent
                case "locationDescription":
                    pl.getLocationDescription().setValue(propValue);
                    break;
                //Populate PL.10 "Comprehensive Location Identifier" subcomponent
                case "comprehensiveLocationIdentifier":
                    Json comprehensiveLocationIdentifier = jsonOrValuePropertyParse(propPath, propValue);
                    populateEiField(pl.getComprehensiveLocationIdentifier(), comprehensiveLocationIdentifier);
                    break;
                //Populate PL.11 "Assigning Authority For Location" subcomponent
                case "assigningAuthorityForLocation":
                    Json assigningAuthorityForLocation = jsonOrValuePropertyParse(propPath, propValue);
                    populateHdField(pl.getAssigningAuthorityForLocation(), assigningAuthorityForLocation);
                    break;
                default:
                    throw EndpointException.permanent(ErrorCode.ARGUMENT, "The property ['" + propPath + "'] does not correspond with any possible XTN field");
            }
        }
    }

    public static void populateEiField(EI ei, Json eiValues) throws DataTypeException {
        for (String key : eiValues.keys()) {
            String propPath = parentProp + "." + key;
            String propValue = eiValues.string(key);

            switch (key) {
                //Populate EI.1 "Entity Identifier"
                case "mainValue":
                case "entityIdentifier":
                    ei.getEntityIdentifier().setValue(propValue);
                    break;
                //Populate EI.2 "Namespace Id"
                case "namespaceId":
                    ei.getNamespaceID().setValue(propValue);
                    break;
                //Populate EI.3 "Universal Id"
                case "universalId":
                    ei.getUniversalID().setValue(propValue);
                    break;
                //Populate EI.4 "Universal Id Type"
                case "universalIdType":
                    ei.getUniversalIDType().setValue(propValue);
                    break;
                default:
                    throw EndpointException.permanent(ErrorCode.ARGUMENT, "The property ['" + propPath + "'] does not correspond with any possible EI field");
            }
        }
    }

    public static void populateXcnField(XCN xcn, Json xcnValues) throws DataTypeException {
        for (String key : xcnValues.keys()) {
            String propPath = parentProp + "." + key;
            String propValue = xcnValues.string(key);

            switch (key) {
//Populate XCN.1 "Person Identifier" component
                case "personIdentifier":
                    xcn.getPersonIdentifier().setValue(propValue);
                    break;
                //Populate XCN.2 "Family Name" component
                case "familyName":
                    Json familyName = jsonOrValuePropertyParse(propPath, propValue);
                    populateFnField(xcn.getFamilyName(),familyName);
                    break;
                //Populate XCN.3 "Given Name" component
                case "givenName":
                    xcn.getGivenName().setValue(propValue);
                    break;
                //Populate XCN.4 "Second And Further Given Names Or Initials Thereof" component
                case "secondAndFurtherGivenNames":
                    xcn.getSecondAndFurtherGivenNamesOrInitialsThereof().setValue(propValue);
                    break;
                //Populate XCN.5 "Suffix (e.g., Jr Or Iii)" component
                case "suffix":
                    xcn.getSuffixEgJRorIII().setValue(propValue);
                    break;
                //Populate XCN.6 "Prefix (e.g., Dr)" component
                case "prefix":
                    xcn.getPrefixEgDR().setValue(propValue);
                    break;
                //Populate XCN.7 "Degree (e.g., Md)" component
                case "degree":
                    xcn.getDegreeEgMD().setValue(propValue);
                    break;
                //Populate XCN.8 "Source Table" component
                case "sourceTable":
                    Json sourceTable = jsonOrValuePropertyParse(propPath, propValue);
                    populateCweField(xcn.getSourceTable(),sourceTable);
                    break;
                //Populate XCN.9 "Assigning Authority" component
                case "assigningAuthority":
                    Json assigningAuthority = jsonOrValuePropertyParse(propPath, propValue);
                    populateHdField(xcn.getAssigningAuthority(),assigningAuthority);
                    break;
                //Populate XCN.10 "Name Type Code" component
                case "nameTypeCode":
                    xcn.getNameTypeCode().setValue(propValue);
                    break;
                //Populate XCN.11 "Identifier Check Digit" component
                case "identifierCheckDigit":
                    xcn.getIdentifierCheckDigit().setValue(propValue);
                    break;
                //Populate XCN.12 "Check Digit Scheme" component
                case "checkDigitScheme":
                    xcn.getCheckDigitScheme().setValue(propValue);
                    break;
                //Populate XCN.13 "Identifier Type Code" component
                case "identifierTypeCode":
                    xcn.getIdentifierTypeCode().setValue(propValue);
                    break;
                //Populate XCN.14 "Assigning Facility" component
                case "assigningFacility":
                    Json assigningFacility = jsonOrValuePropertyParse(propPath, propValue);
                    populateHdField(xcn.getAssigningFacility(),assigningFacility);
                    break;
                //Populate XCN.15 "Name Representation Code" component
                case "nameRepresentationCode":
                    xcn.getNameRepresentationCode().setValue(propValue);
                    break;
                //Populate XCN.16 "Name Context" component
                case "nameContext":
                    Json nameContext = jsonOrValuePropertyParse(propPath, propValue);
                    populateCweField(xcn.getNameContext(),nameContext);
                    break;
                //XCN.17 "Name Validity Range" Withdrawn
                //Populate XCN.18 "Name Assembly Order" component
                case "nameAssemblyOrder":
                    xcn.getNameAssemblyOrder().setValue(propValue);
                    break;
                //Populate XCN.19 "Effective Date" component
                case "effectiveDate":
                    xcn.getEffectiveDate().setValue(propValue);
                    break;
                //Populate XCN.20 "Expiration Date" component
                case "expirationDate":
                    xcn.getExpirationDate().setValue(propValue);
                    break;
                //Populate XCN.21 "Professional Suffix" component
                case "professionalSuffix":
                    xcn.getProfessionalSuffix().setValue(propValue);
                    break;
                //Populate XCN.22 "Assigning Jurisdiction" component
                case "assigningJurisdiction":
                    Json assigningJurisdiction = jsonOrValuePropertyParse(propPath, propValue);
                    populateCweField(xcn.getAssigningJurisdiction(),assigningJurisdiction);
                    break;
                //Populate XCN.23 "Assigning Agency Or Department" component
                case "assigningAgencyOrDepartment":
                    Json assigningAgencyOrDepartment = jsonOrValuePropertyParse(propPath, propValue);
                    populateCweField(xcn.getAssigningAgencyOrDepartment(),assigningAgencyOrDepartment);
                    break;
                //Populate XCN.24 " Security Check" component
                case "securityCheck":
                    xcn.getSecurityCheck().setValue(propValue);
                    break;
                //Populate XCN.25 "Security Check Scheme" component
                case "securityCheckScheme":
                    xcn.getSecurityCheckScheme().setValue(propValue);
                    break;
                default:
                    throw EndpointException.permanent(ErrorCode.ARGUMENT, "The property ['" + propPath + "'] does not correspond with any possible XCN field");
            }
        }
    }

    public static void populateFnField(FN fn, Json fnValues) throws DataTypeException {
        for (String key : fnValues.keys()) {
            String propPath = parentProp + "." + key;
            String propValue = fnValues.string(key);

            switch (key) {
                case "surname":
                case "mainValue":
                    fn.getSurname().setValue(propValue);
                    break;
                case "ownSurnamePrefix":
                    fn.getOwnSurnamePrefix().setValue(propValue);
                    break;
                case "ownSurname":
                    fn.getOwnSurname().setValue(propValue);
                    break;
                case "surnamePrefixFromPartnerSpouse":
                    fn.getSurnamePrefixFromPartnerSpouse().setValue(propValue);
                    break;
                case "surnameFromPartnerSpouse":
                    fn.getSurnameFromPartnerSpouse().setValue(propValue);
                    break;
                default:
                    throw EndpointException.permanent(ErrorCode.ARGUMENT, "The property ['" + propPath + "'] does not correspond with any possible FN field");
            }
        }

    }

    public static void populateFcField(FC fc, Json fcValues) throws DataTypeException {
        for (String key : fcValues.keys()) {
            String propPath = parentProp + "." + key;
            String propValue = fcValues.string(key);

            switch (key) {
                //Populate FC.1 "Financial Class Code" component
                case "financialClassCode":
                    Json financialClassCode = jsonOrValuePropertyParse(propPath, propValue);
                    populateCweField(fc.getFinancialClassCode(),financialClassCode);
                    break;
                //Populate FC.2 "Effective Date" component
                case "effectiveDate":
                    fc.getEffectiveDate().setValue(propValue);
                    break;
                default:
                    throw EndpointException.permanent(ErrorCode.ARGUMENT, "The property ['" + propPath + "'] does not correspond with any possible FC field");
            }
        }
    }

    public static void populateDldField(DLD dld, Json dldValues) throws DataTypeException {
        for (String key : dldValues.keys()) {
            String propPath = parentProp + "." + key;
            String propValue = dldValues.string(key);

            switch (key) {
                //Populate DLD.1 "Discharge To Location" component
                case "dischargeToLocation":
                    Json dischargeToLocation = jsonOrValuePropertyParse(propPath, propValue);
                    populateCweField(dld.getDischargeToLocation(),dischargeToLocation);
                    break;
                //Populate DLD.2 "Effective Date" component
                case "effectiveDate":
                    dld.getEffectiveDate().setValue(propValue);
                    break;
                default:
                    throw EndpointException.permanent(ErrorCode.ARGUMENT, "The property ['" + propPath + "'] does not correspond with any possible DL field");
            }
        }
    }

    public static void populateAdField(AD ad, Json adValues) throws DataTypeException {
        for (String key : adValues.keys()) {
            String propPath = parentProp + "." + key;
            String propValue = adValues.string(key);

            switch (key) {
//Populate AD.1 "Street Address"
                case "streetAddress":
                    ad.getStreetAddress().setValue(propValue);
                    break;
                //Populate AD.2 "Other Designation"
                case "otherDesignation":
                    ad.getOtherDesignation().setValue(propValue);
                    break;
                //Populate ad.3 "City"
                case "city":
                    ad.getCity().setValue(propValue);
                    break;
                //Populate ad.4 "State Or Province"
                case "stateOrProvince":
                    ad.getStateOrProvince().setValue(propValue);
                    break;
                //Populate ad.5 "Zip Or Postal Code"
                case "zipOrPostalCode":
                    ad.getZipOrPostalCode().setValue(propValue);
                    break;
                //Populate ad.6 "Country"
                case "country":
                    ad.getCountry().setValue(propValue);
                    break;
                //Populate ad.7 "Address Type"
                case "addressType":
                    ad.getAddressType().setValue(propValue);
                    break;
                //Populate ad.8 "Other Geographic Designation"
                case "otherGeographicDesignation":
                    ad.getOtherGeographicDesignation().setValue(propValue);
                    break;
                default:
                    throw EndpointException.permanent(ErrorCode.ARGUMENT, "The property ['" + propPath + "'] does not correspond with any possible AD field");
            }
        }
    }
    
    public static void populateCneField(CNE cne, Json cneValues) throws DataTypeException {
        for (String key : cneValues.keys()) {
            String propPath = parentProp + "." + key;
            String propValue = cneValues.string(key);

            switch (key) {
//Populate CNE.1 "Identifier"
                case "mainValue":
                    cne.getIdentifier().setValue(propValue);
                    break;
                //Populate CNE.2 "Text"
                case "text":
                    cne.getText().setValue(propValue);
                    break;
                //Populate CNE.3 "Name Of Coding System"
                case "nameOfCodingSystem":
                    cne.getNameOfCodingSystem().setValue(propValue);
                    break;
                //Populate CNE.4 "Alternate Identifier"
                case "alternateIdentifier":
                    cne.getAlternateIdentifier().setValue(propValue);
                    break;
                //Populate CNE.5 "Alternate Text"
                case "alternateText":
                    cne.getAlternateText().setValue(propValue);
                    break;
                //Populate CNE.6 "Name Of Alternate Coding System"
                case "nameOfAlternateCodingSystem":
                    cne.getNameOfAlternateCodingSystem().setValue(propValue);
                    break;
                //Populate CNE.7 "Coding System Version Id"
                case "codingSystemVersionId":
                    cne.getCodingSystemVersionID().setValue(propValue);
                    break;
                //Populate CNE.8 "Alternate Coding System Version Id"
                case "alternateCodingSystemVersionId":
                    cne.getAlternateCodingSystemVersionID().setValue(propValue);
                    break;
                //Populate CNE.9 "Original Text"
                case "originalText":
                    cne.getOriginalText().setValue(propValue);
                    break;
                //Populate CNE.10 "Second Alternate Identifier"
                case "secondAlternateIdentifier":
                    cne.getSecondAlternateIdentifier().setValue(propValue);
                    break;
                //Populate CNE.11 "Second Alternate Text"
                case "secondAlternateText":
                    cne.getSecondAlternateText().setValue(propValue);
                    break;
                //Populate CNE.12 "Name Of Second Alternate Coding System"
                case "nameOfSecondAlternateCodingSystem":
                    cne.getNameOfSecondAlternateCodingSystem().setValue(propValue);
                    break;
                //Populate CNE.13 "Second Alternate Coding System Version Id"
                case "secondAlternateCodingSystemVersionId":
                    cne.getSecondAlternateCodingSystemVersionID().setValue(propValue);
                    break;
                //Populate CNE.14 "Coding System Oid"
                case "codingSystemOid":
                    cne.getCodingSystemOID().setValue(propValue);
                    break;
                //Populate CNE.15 "Value Set Oid"
                case "valueSetOid":
                    cne.getValueSetOID().setValue(propValue);
                    break;
                //Populate CNE.16 "Value Set Version Id"
                case "valueSetVersionId":
                    cne.getValueSetVersionID().setValue(propValue);
                    break;
                //Populate CNE.17 "Alternate Coding System Oid"
                case "alternateCodingSystemOid":
                    cne.getAlternateCodingSystemOID().setValue(propValue);
                    break;
                //Populate CNE.18 "Alternate Value Set Oid"
                case "alternateValueSetOid":
                    cne.getAlternateValueSetOID().setValue(propValue);
                    break;
                //Populate CNE.19 "Alternate Value Set Version Id"
                case "alternateValueSetVersionId":
                    cne.getAlternateValueSetVersionID().setValue(propValue);
                    break;
                //Populate CNE.20 "Second Alternate Coding System Oid"
                case "secondAlternateCodingSystemOid":
                    cne.getSecondAlternateCodingSystemOID().setValue(propValue);
                    break;
                //Populate CNE.21 "Second Alternate Value Set Oid"
                case "secondAlternateValueSetOid":
                    cne.getSecondAlternateValueSetOID().setValue(propValue);
                    break;
                //Populate CNE.22 "Second Alternate Value Set Version Id"
                case "secondAlternateValueSetVersionId":
                    cne.getSecondAlternateValueSetVersionID().setValue(propValue);
                    break;
                default:
                    throw EndpointException.permanent(ErrorCode.ARGUMENT, "The property ['" + propPath + "'] does not correspond with any possible CNE field");
            }
        }
    }

    public static void populateCfField(CF cf, Json cfValues) throws DataTypeException {
        for (String key : cfValues.keys()) {
            String propPath = parentProp + "." + key;
            String propValue = cfValues.string(key);

            switch (key) {
                //Populate CF.1 "Identifier"
                case "identifier":
                    cf.getIdentifier().setValue(propValue);
                    break;
                //Populate CF.2 "Formatted Text"
                case "formattedText":
                    cf.getFormattedText().setValue(propValue);
                    break;
                //Populate CF.3 "Name Of Coding System"
                case "nameOfCodingSystem":
                    cf.getNameOfCodingSystem().setValue(propValue);
                    break;
                //Populate CF.4 "Alternate Identifier"
                case "alternateIdentifier":
                    cf.getAlternateIdentifier().setValue(propValue);
                    break;
                //Populate CF.5 "Alternate Formatted Text"
                case "alternateFormattedText":
                    cf.getAlternateFormattedText().setValue(propValue);
                    break;
                //Populate CF.6 "Name Of Alternate Coding System"
                case "nameOfAlternateCodingSystem":
                    cf.getNameOfAlternateCodingSystem().setValue(propValue);
                    break;
                //Populate CF.7 "Coding System Version Id"
                case "codingSystemVersionId":
                    cf.getCodingSystemVersionID().setValue(propValue);
                    break;
                //Populate CF.8 "Alternate Coding System Version Id"
                case "alternateCodingSystemVersionId":
                    cf.getAlternateCodingSystemVersionID().setValue(propValue);
                    break;
                //Populate CF.9 "Original Text"
                case "originalText":
                    cf.getOriginalText().setValue(propValue);
                    break;
                //Populate CF.10 "Second Alternate Identifier"
                case "secondAlternateIdentifier":
                    cf.getSecondAlternateIdentifier().setValue(propValue);
                    break;
                //Populate CF.11 "Second Alternate Formatted Text"
                case "secondAlternateFormattedText":
                    cf.getSecondAlternateFormattedText().setValue(propValue);
                    break;
                //Populate CF.12 "Name Of Second Alternate Coding System"
                case "nameOfSecondAlternateCodingSystem":
                    cf.getNameOfSecondAlternateCodingSystem().setValue(propValue);
                    break;
                //Populate CF.13 "Second Alternate Coding System Version Id"
                case "secondAlternateCodingSystemVersionId":
                    cf.getSecondAlternateCodingSystemVersionID().setValue(propValue);
                    break;
                //Populate CF.14 "Coding System Oid"
                case "codingSystemOid":
                    cf.getCodingSystemOID().setValue(propValue);
                    break;
                //Populate CF.15 "Value Set Oid"
                case "valueSetOid":
                    cf.getValueSetOID().setValue(propValue);
                    break;
                //Populate CF.16 "Value Set Version Id"
                case "valueSetVersionId":
                    cf.getValueSetVersionID().setValue(propValue);
                    break;
                //Populate CF.17 "Alternate Coding System Oid"
                case "alternateCodingSystemOid":
                    cf.getAlternateCodingSystemOID().setValue(propValue);
                    break;
                //Populate CF.18 "Alternate Value Set Oid"
                case "alternateValueSetOid":
                    cf.getAlternateValueSetOID().setValue(propValue);
                    break;
                //Populate CF.19 "Alternate Value Set Version Id"
                case "alternateValueSetVersionId":
                    cf.getAlternateValueSetVersionID().setValue(propValue);
                    break;
                //Populate CF.20 "Second Alternate Coding System Oid"
                case "secondAlternateCodingSystemOid":
                    cf.getSecondAlternateCodingSystemOID().setValue(propValue);
                    break;
                //Populate CF.21 "Second Alternate Value Set Oid"
                case "secondAlternateValueSetOid":
                    cf.getSecondAlternateValueSetOID().setValue(propValue);
                    break;
                //Populate CF.22 "Second Alternate Value Set Version Id"
                case "secondAlternateValueSetVersionId":
                    cf.getSecondAlternateValueSetVersionID().setValue(propValue);
                    break;
                default:
                    throw EndpointException.permanent(ErrorCode.ARGUMENT, "The property ['" + propPath + "'] does not correspond with any possible CF field");
            }
        }
    }

    public static void populateCpField(CP cp, Json cpValues) throws DataTypeException {
        for (String key : cpValues.keys()) {
            String propPath = parentProp + "." + key;
            String propValue = cpValues.string(key);

            switch (key) {
                //Populate CP.1 "Price"
                case "price":
                    Json price = jsonOrValuePropertyParse(propPath, propValue);
                    populateMoField(cp.getPrice(),price);
                    break;
                //Populate CP.2 "Price"
                case "priceType":
                    cp.getPriceType().setValue(propValue);
                    break;
                //Populate CP.3 "From Value"
                case "fromValue":
                    cp.getFromValue().setValue(propValue);
                    break;
                //Populate CP.4 "To Value"
                case "toValue":
                    cp.getToValue().setValue(propValue);
                    break;
                //Populate CP.5 "Range Units"
                case "rangeUnits":
                    Json rangeUnits = jsonOrValuePropertyParse(propPath, propValue);
                    populateCweField(cp.getRangeUnits(),rangeUnits);
                    break;
                //Populate CP.6 "Range Type"
                case "rangeType":
                    cp.getRangeType().setValue(propValue);
                    break;
                default:
                    throw EndpointException.permanent(ErrorCode.ARGUMENT, "The property ['" + propPath + "'] does not correspond with any possible CP field");
            }
        }
    }

    public static void populateMoField(MO mo, Json moValues) throws DataTypeException {
        for (String key : moValues.keys()) {
            String propPath = parentProp + "." + key;
            String propValue = moValues.string(key);

            switch (key) {
                //Populate MO.1 "Quantity"
                case "quantity":
                    mo.getQuantity().setValue(propValue);
                    break;
                //Populate MO.2 "Denomination"
                case "denomination":
                    mo.getDenomination().setValue(propValue);
                    break;
                default:
                    throw EndpointException.permanent(ErrorCode.ARGUMENT, "The property ['" + propPath + "'] does not correspond with any possible MO field");
            }
        }
    }

    public static void populateDrField(DR dr, Json drValues) throws DataTypeException {
        for (String key : drValues.keys()) {
            String propPath = parentProp + "." + key;
            String propValue = drValues.string(key);

            switch (key) {
//Populate DR.1 "Range Start Date/Time"
                case "mainValue":
                    dr.getRangeStartDateTime().setValue(propValue);
                    break;
                //Populate DR.2 "Range End Date/Time"
                case "rangeEndDateTime":
                    dr.getRangeEndDateTime().setValue(propValue);
                    break;
                default:
                    throw EndpointException.permanent(ErrorCode.ARGUMENT, "The property ['" + propPath + "'] does not correspond with any possible DR field");
            }
        }
    }

    public static void populateEdField(ED ed, Json edValues) throws DataTypeException {
        for (String key : edValues.keys()) {
            String propPath = parentProp + "." + key;
            String propValue = edValues.string(key);

            switch (key) {

                //Populate ED.1 "Source Application"
                case "sourceApplication":
                    Json sourceApplication = jsonOrValuePropertyParse(propPath, propValue);
                    populateHdField(ed.getSourceApplication(),sourceApplication);
                    break;
                //Populate ED.2 "Type Of Data"
                case "typeOfData":
                    ed.getTypeOfData().setValue(propValue);
                    break;
                //Populate ED.3 "Data Subtype"
                case "dataSubtype":
                    ed.getDataSubtype().setValue(propValue);
                    break;
                //Populate DR.4 "Encoding"
                case "encoding":
                    ed.getEncoding().setValue(propValue);
                    break;
                //Populate DR.5 "Data"
                case "data":
                    ed.getData().setValue(propValue);
                    break;
                default:
                    throw EndpointException.permanent(ErrorCode.ARGUMENT, "The property ['" + propPath + "'] does not correspond with any possible ED field");
            }
        }
    }

    public static void populateMaField(MA ma, Json maValues) throws DataTypeException {
        for (String key : maValues.keys()) {
            String propPath = parentProp + "." + key;
            String propValue = maValues.string(key);

            switch (key) {

                //Populate MA.1 "Sample Y From Channel 1"
                case "sampleYFromChannel1":
                    ma.getSampleYFromChannel1().setValue(propValue);
                    break;
                //Populate MA.2 "Sample Y From Channel 2"
                case "sampleYFromChannel2":
                    ma.getSampleYFromChannel2().setValue(propValue);
                    break;
                //Populate MA.3 "Sample Y From Channel 3"
                case "sampleYFromChannel3":
                    ma.getSampleYFromChannel3().setValue(propValue);
                    break;
                //Populate MA.4 "Sample Y From Channel 4"
                case "sampleYFromChannel4":
                    ma.getSampleYFromChannel4().setValue(propValue);
                    break;
                default:
                    throw EndpointException.permanent(ErrorCode.ARGUMENT, "The property ['" + propPath + "'] does not correspond with any possible MA field");
            }
        }
    }

    public static void populateNaField(NA na, Json naValues) throws DataTypeException {
        for (String key : naValues.keys()) {
            String propPath = parentProp + "." + key;
            String propValue = naValues.string(key);

            switch (key) {
                //Populate NA.1 "Value 1"
                case "value1":
                    na.getValue1().setValue(propValue);
                    break;
                //Populate NA.2 "Value 2"
                case "sampleYFromChannel2":
                    na.getValue2().setValue(propValue);
                    break;
                //Populate NA.3 "Value 3"
                case "sampleYFromChannel3":
                    na.getValue3().setValue(propValue);
                    break;
                //Populate NA.4 "Value 4"
                case "sampleYFromChannel4":
                    na.getValue4().setValue(propValue);
                    break;
                default:
                    throw EndpointException.permanent(ErrorCode.ARGUMENT, "The property ['" + propPath + "'] does not correspond with any possible NA field");
            }
        }
    }

    public static void populateRpField(RP rp, Json rpValues) throws DataTypeException {
        for (String key : rpValues.keys()) {
            String propPath = parentProp + "." + key;
            String propValue = rpValues.string(key);

            switch (key) {

                //Populate RP.1 "Pointer"
                case "pointer":
                    rp.getPointer().setValue(propValue);
                    break;
                //Populate RP.2 "Application Id"
                case "applicationId":
                    Json applicationId = jsonOrValuePropertyParse(propPath, propValue);
                    populateHdField(rp.getApplicationID(),applicationId);
                    break;
                //Populate RP.3 "Type Of Data"
                case "typeOfData":
                    rp.getTypeOfData().setValue(propValue);
                    break;
                //Populate RP.4 "Subtype"
                case "subtype":
                    rp.getSubtype().setValue(propValue);
                    break;
                default:
                    throw EndpointException.permanent(ErrorCode.ARGUMENT, "The property ['" + propPath + "'] does not correspond with any possible RP field");
            }
        }
    }

    public static void populateSnField(SN sn, Json snValues) throws DataTypeException {
        for (String key : snValues.keys()) {
            String propPath = parentProp + "." + key;
            String propValue = snValues.string(key);

            switch (key) {

                //Populate SN.1 "Comparator"
                case "comparator":
                    sn.getComparator().setValue(propValue);
                    break;
                //Populate SN.2 "Num1"
                case "num1":
                    sn.getNum1().setValue(propValue);
                    break;
                //Populate SN.3 "Separator/Suffix"
                case "separatorSuffix":
                    sn.getSeparatorSuffix().setValue(propValue);
                    break;
                //Populate RP.4 "Num2"
                case "num2":
                    sn.getNum2().setValue(propValue);
                    break;
                default:
                    throw EndpointException.permanent(ErrorCode.ARGUMENT, "The property ['" + propPath + "'] does not correspond with any possible SN field");
            }
        }
    }

    public static void populateAuiField(AUI aui, Json auiValues) throws DataTypeException {
        for (String key : auiValues.keys()) {
            String propPath = parentProp + "." + key;
            String propValue = auiValues.string(key);

            switch (key) {

                //Populate AUI.1 "Authorization Number"
                case "authorizationNumber":
                    aui.getAuthorizationNumber().setValue(propValue);
                    break;
                //Populate AUI.2 "Date"
                case "date":
                    aui.getDate().setValue(propValue);
                    break;
                //Populate AUI.3 "Source"
                case "source":
                    aui.getSource().setValue(propValue);
                    break;
                default:
                    throw EndpointException.permanent(ErrorCode.ARGUMENT, "The property ['" + propPath + "'] does not correspond with any possible AUI field");
            }
        }
    }
    
    public static void populateRmcField(RMC rmc, Json rmcValues) throws DataTypeException {
        for (String key : rmcValues.keys()) {
            String propPath = parentProp + "." + key;
            String propValue = rmcValues.string(key);

            switch (key) {

                //Populate RMC.1 "Room Type"
                case "roomType":
                    Json roomType = jsonOrValuePropertyParse(propPath, propValue);
                    populateCweField(rmc.getRoomType(),roomType);
                    break;
                //Populate RMC.2 "Amount Type"
                case "amountType":
                    Json amountType = jsonOrValuePropertyParse(propPath, propValue);
                    populateCweField(rmc.getAmountType(),amountType);
                    break;
                //Populate RMC.3 "Coverage Amount"
                case "coverageAmount":
                    rmc.getCoverageAmount().setValue(propValue);
                    break;
                //Populate RMC.4 "Money Or Percentage"
                case "moneyOrPercentage":
                    Json moneyOrPercentage = jsonOrValuePropertyParse(propPath, propValue);
                    populateMopField(rmc.getMoneyOrPercentage(),moneyOrPercentage);
                    break;
                default:
                    throw EndpointException.permanent(ErrorCode.ARGUMENT, "The property ['" + propPath + "'] does not correspond with any possible RMC field");
            }
        }
    }

    public static void populateMopField(MOP mop, Json mopValues) throws DataTypeException {
        for (String key : mopValues.keys()) {
            String propPath = parentProp + "." + key;
            String propValue = mopValues.string(key);

            switch (key) {

                //Populate MOP.1 "Money Or Percentage Indicator"
                case "moneyOrPercentageIndicator":
                    mop.getMoneyOrPercentageIndicator().setValue(propValue);
                    break;
                //Populate MOP.2 "Money Or Percentage Quantity"
                case "moneyOrPercentageQuantity":
                    mop.getMoneyOrPercentageQuantity().setValue(propValue);
                    break;
                //Populate MOP.3 "Monetary Denomination"
                case "monetaryDenomination":
                    mop.getMonetaryDenomination().setValue(propValue);
                    break;
                default:
                    throw EndpointException.permanent(ErrorCode.ARGUMENT, "The property ['" + propPath + "'] does not correspond with any possible MOP field");
            }
        }
    }

    public static void populatePtaField(PTA pta, Json ptaValues) throws DataTypeException {
        for (String key : ptaValues.keys()) {
            String propPath = parentProp + "." + key;
            String propValue = ptaValues.string(key);

            switch (key) {

                //Populate PTA.1 "Policy Type"
                case "policyType":
                    Json policyType = jsonOrValuePropertyParse(propPath, propValue);
                    populateCweField(pta.getPolicyType(),policyType);
                    break;
                //Populate PTA.2 "Amount Class"
                case "amountClass":
                    Json amountClass = jsonOrValuePropertyParse(propPath, propValue);
                    populateCweField(pta.getAmountClass(),amountClass);
                    break;
                //Populate PTA.3 "Money Or Percentage Quantity"
                case "moneyOrPercentageQuantity":
                    pta.getMoneyOrPercentageQuantity().setValue(propValue);
                    break;
                //Populate PTA.4 "Money Or Percentage"
                case "moneyOrPercentage":
                    Json moneyOrPercentage = jsonOrValuePropertyParse(propPath, propValue);
                    populateMopField(pta.getMoneyOrPercentage(),moneyOrPercentage);
                    break;
                default:
                    throw EndpointException.permanent(ErrorCode.ARGUMENT, "The property ['" + propPath + "'] does not correspond with any possible PTA field");
            }
        }
    }

    public static void populateDdiField(DDI ddi, Json ddiValues) throws DataTypeException {
        for (String key : ddiValues.keys()) {
            String propPath = parentProp + "." + key;
            String propValue = ddiValues.string(key);

            switch (key) {
                //Populate DDI.1 "Delay Days"
                case "delayDays":
                    ddi.getDelayDays().setValue(propValue);
                    break;
                //Populate DDI.2 "Monetary Amount"
                case "monetaryAmount":
                    Json monetaryAmount = jsonOrValuePropertyParse(propPath, propValue);
                    populateMoField(ddi.getMonetaryAmount(),monetaryAmount);
                    break;
                //Populate DDI.3 "Number Of Days"
                case "numberOfDays":
                    ddi.getNumberOfDays().setValue(propValue);
                    break;
                default:
                    throw EndpointException.permanent(ErrorCode.ARGUMENT, "The property ['" + propPath + "'] does not correspond with any possible DDI field");
            }
        }
    }

    public static void populateDtnField(DTN dtn, Json dtnValues) throws DataTypeException {
        for (String key : dtnValues.keys()) {
            String propPath = parentProp + "." + key;
            String propValue = dtnValues.string(key);

            switch (key) {

                //Populate DTN.1 "Day Type"
                case "dayType":
                    Json dayType = jsonOrValuePropertyParse(propPath, propValue);
                    populateCweField(dtn.getDayType(),dayType);
                    break;
                //Populate DTN.2 - Number Of Days
                case "numberOfDays":
                    dtn.getNumberOfDays().setValue(propValue);
                    break;
                default:
                    throw EndpointException.permanent(ErrorCode.ARGUMENT, "The property ['" + propPath + "'] does not correspond with any possible DTN field");
            }
        }
    }

    public static void populateIcdField(ICD icd, Json icdValues) throws DataTypeException {
        for (String key : icdValues.keys()) {
            String propPath = parentProp + "." + key;
            String propValue = icdValues.string(key);

            switch (key) {

                //Populate ICD.1 - Certification Patient Type
                case "certificationPatientType":
                    Json certificationPatientType = jsonOrValuePropertyParse(propPath, propValue);
                    populateCweField(icd.getCertificationPatientType(),certificationPatientType);
                    break;
                //Populate ICD.2 - Certification Required
                case "certificationRequired":
                    icd.getCertificationRequired().setValue(propValue);
                    break;
                //Populate ICD.3 - Date/Time Certification Required
                case "dateTimeCertificationRequired":
                    icd.getDateTimeCertificationRequired().setValue(propValue);
                    break;
                default:
                    throw EndpointException.permanent(ErrorCode.ARGUMENT, "The property ['" + propPath + "'] does not correspond with any possible ICD field");
            }
        }
    }

    public static void populateCqField(CQ cq, Json cqValues) throws DataTypeException {
        for (String key : cqValues.keys()) {
            String propPath = parentProp + "." + key;
            String propValue = cqValues.string(key);

            switch (key) {

                //Populate CQ.1 - Quantity
                case "quantity":
                    cq.getQuantity().setValue(propValue);
                    break;
                //Populate CQ.2 - Units
                case "units":
                    Json units = jsonOrValuePropertyParse(propPath, propValue);
                    populateCweField(cq.getUnits(),units);
                    break;
                default:
                    throw EndpointException.permanent(ErrorCode.ARGUMENT, "The property ['" + propPath + "'] does not correspond with any possible CQ field");
            }
        }
    }

    public static void populateEipField(EIP eip, Json eipValues) throws DataTypeException {
        for (String key : eipValues.keys()) {
            String propPath = parentProp + "." + key;
            String propValue = eipValues.string(key);

            switch (key) {
                //Populate EIP.1 - Placer Assigned Identifier
                case "placerAssignedIdentifier":
                    Json placerAssignedIdentifier = jsonOrValuePropertyParse(propPath,propValue);
                    populateEiField(eip.getPlacerAssignedIdentifier(),placerAssignedIdentifier);
                    break;
                //Populate EIP.2 - Filler Assigned Identifier
                case "fillerAssignedIdentifier":
                    Json fillerAssignedIdentifier = jsonOrValuePropertyParse(propPath, propValue);
                    populateEiField(eip.getFillerAssignedIdentifier(),fillerAssignedIdentifier);
                    break;
                default:
                    throw EndpointException.permanent(ErrorCode.ARGUMENT, "The property ['" + propPath + "'] does not correspond with any possible EIP field");
            }
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
            default:
                throw EndpointException.permanent(ErrorCode.ARGUMENT, "The value ['" + dataType + "'] on ["+parentProp+".observationValue] does not correspond with any possible possible HL7 data type");
        }
    }
}
