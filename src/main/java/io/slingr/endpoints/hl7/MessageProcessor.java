package io.slingr.endpoints.hl7;

import ca.uhn.hl7v2.HL7Exception;
import ca.uhn.hl7v2.model.Group;
import ca.uhn.hl7v2.model.Message;
import ca.uhn.hl7v2.model.v281.message.OML_O21;
import ca.uhn.hl7v2.model.v281.segment.*;
import io.slingr.endpoints.utils.Json;
import io.slingr.endpoints.services.Events;

public class MessageProcessor {

    private final Events events;

    public MessageProcessor(Events events) {
        this.events = events;
    }

    public void processOmlMessage(Message omlMsg, String triggerEvent) throws HL7Exception {
        switch (triggerEvent){
            case "O21":
                Json ordersInfo = Json.list();
                OML_O21 oml = (OML_O21) omlMsg;
                for (Group orderGroup: oml.getORDERAll()) {
                    Json orderInfo = Json.map();

                    ORC orc = (ORC) orderGroup.get("ORC");
                    String orcPlacerOrderNumber = orc.getPlacerOrderNumber().getEntityIdentifier().getValue();
                    String orcFillerOrderNumber = orc.getFillerOrderNumber().getEntityIdentifier().getValue();
                    String orcOrderStatus = orc.getOrderStatus().getValue();

                    Group observation_request = (Group) orderGroup.get("OBSERVATION_REQUEST");
                    OBR obr = (OBR) observation_request.get("OBR");
                    String obrPlacerOrderNumber = obr.getPlacerOrderNumber().getEntityIdentifier().getValue();
                    String obrFillerOrderNumber = obr.getFillerOrderNumber().getEntityIdentifier().getValue();
                    if(orcPlacerOrderNumber != obrPlacerOrderNumber){
                        //Should be a more specific error
                        throw new Error();
                    } else if (orcFillerOrderNumber != obrFillerOrderNumber) {
                        //Should be a more specific error
                        throw new Error();
                    }

                    String externalId = orcPlacerOrderNumber.isEmpty() ?  obrPlacerOrderNumber : orcPlacerOrderNumber;
                    String orderId = orcFillerOrderNumber.isEmpty() ?  obrFillerOrderNumber : orcFillerOrderNumber;

                    Group specimen = (Group) observation_request.get("SPECIMEN");
                    SPM spm = (SPM) specimen.get("SPM");
                    String type = spm.getSpecimenType().getIdentifier().getValue();
                    String collectionPoint = spm.getSpecimenCollectionSite().getIdentifier().getValue() == null ? spm.getSpecimenCollectionSite().getIdentifier().getValue() : spm.getSpecimenCollectionSite().getText().getValue();
                    String collectionDate  = spm.getSpecimenCollectionDateTime().getRangeStartDateTime().getValue();

                    Group container = (Group) specimen.get("CONTAINER");
                    SAC sac = (SAC) container.get("SAC");
                    String deviceCode = sac.getContainerIdentifier().getEntityIdentifier().getValue();

                    TCD tcd = (TCD) observation_request.get("TCD");
                    String tcdUniversalServiceIdentifier = tcd.getUniversalServiceIdentifier().getIdentifier().getValue();

                    orderInfo.set("test",tcdUniversalServiceIdentifier);
                    orderInfo.set("externalId",externalId);
                    orderInfo.set("orderId",orderId);
                    orderInfo.set("orderStatus",orcOrderStatus);
                    orderInfo.set("type",type);
                    orderInfo.set("collectionPoint",collectionPoint);
                    orderInfo.set("collectionDate",collectionDate);
                    orderInfo.set("deviceCode",deviceCode);

                    ordersInfo.push(orderInfo);
                }
                System.out.println("THE BODY TO RETURN IS: "+ordersInfo.toString());
                events.send("messageArrived",ordersInfo);

                return;
        }
    }
}
