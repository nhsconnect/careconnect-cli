package uk.nhs.careconnect.cli.ODSCSV;

import org.apache.commons.csv.CSVRecord;
import org.hl7.fhir.r4.model.*;
import uk.org.hl7.fhir.core.Stu3.CareConnectSystem;

public class LocationHandler implements ODSUploader.IRecordHandler {

    private String typeSncCT = "";

    private String typeDisplay = "";

    public LocationHandler(ODSUploader _odsUploader,String typeSncCT, String typeDisplay) {
        this.typeSncCT = typeSncCT;
        this.typeDisplay = typeDisplay;
        this.odsUploader = _odsUploader;
    };

    ODSUploader odsUploader;

    public void setType(String type) {
        this.typeSncCT = type;
    }

    @Override
    public void accept(CSVRecord theRecord) throws InterruptedException {
        Location location = new Location();
        location.setId("dummy");

        location.addIdentifier()
                .setSystem(CareConnectSystem.ODSSiteCode)
                .setValue(theRecord.get("OrganisationCode"));

        location.setName(odsUploader.Inicaps(theRecord.get("Name")));

        if (!theRecord.get("ContactTelephoneNumber").isEmpty()) {
            location.addTelecom()
                    .setUse(ContactPoint.ContactPointUse.WORK)
                    .setValue(theRecord.get("ContactTelephoneNumber"))
                    .setSystem(ContactPoint.ContactPointSystem.PHONE);
        }
        location.setStatus(Location.LocationStatus.ACTIVE);
        if (!theRecord.get("CloseDate").isEmpty()) {
            location.setStatus(Location.LocationStatus.ACTIVE);
        }
        location.getAddress()
                .setUse(Address.AddressUse.WORK)
                .addLine(odsUploader.Inicaps(theRecord.get("AddressLine_1")))
                .addLine(odsUploader.Inicaps(theRecord.get("AddressLine_2")))
                .addLine(odsUploader.Inicaps(theRecord.get("AddressLine_3")))
                .setCity(odsUploader.Inicaps(theRecord.get("AddressLine_4")))
                .setDistrict(odsUploader.Inicaps(theRecord.get("AddressLine_5")))
                .setPostalCode(theRecord.get("Postcode"));


        if (typeSncCT!=null) {
            location.getType().add(new CodeableConcept().addCoding(new Coding()
                    .setSystem(CareConnectSystem.SNOMEDCT)
                    .setCode(typeSncCT)
                    .setDisplay(typeDisplay)));
        }

        if (!theRecord.get("Commissioner").isEmpty()) {
                // System.out.println("Org Id = "+parentOrg.getId());
                location.setManagingOrganization(new Reference().setIdentifier(new Identifier().setValue(theRecord.get("Commissioner")).setSystem(CareConnectSystem.ODSOrganisationCode)));
        }

        odsUploader.locs.add(location);
    }

}
