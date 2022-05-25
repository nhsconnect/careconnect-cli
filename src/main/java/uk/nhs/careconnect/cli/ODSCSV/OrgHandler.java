package uk.nhs.careconnect.cli.ODSCSV;

import org.apache.commons.csv.CSVRecord;
import org.hl7.fhir.r4.model.*;
import uk.org.hl7.fhir.core.Stu3.CareConnectSystem;

public class OrgHandler implements ODSUploader.IRecordHandler {

    private String typeSncCT = "";

    private String typeDisplay = "";

    private String typeSystem = "";

    OrgHandler(ODSUploader _odsUploader,String typeSystem , String typeSncCT, String typeDisplay) {
        this.typeSncCT = typeSncCT;
        this.typeDisplay = typeDisplay;
        this.typeSystem = typeSystem;
        this.odsUploader = _odsUploader;
    };

    ODSUploader odsUploader;

    public void setType(String type) {
        this.typeSncCT = type;
    }

    @Override
    public void accept(CSVRecord theRecord) throws InterruptedException {
        //v  System.out.println(theRecord.toString());
        Organization organization = new Organization();

        organization.setId("dummy");

        organization.addIdentifier()
                .setSystem(CareConnectSystem.ODSOrganisationCode)
                .setValue(theRecord.get("OrganisationCode"));


        organization.setName(odsUploader.Inicaps(theRecord.get("Name")));

        if (!theRecord.get("ContactTelephoneNumber").isEmpty()) {
            organization.addTelecom()
                    .setUse(ContactPoint.ContactPointUse.WORK)
                    .setValue(theRecord.get("ContactTelephoneNumber"))
                    .setSystem(ContactPoint.ContactPointSystem.PHONE);
        }
        if (!theRecord.get("Commissioner").isEmpty()) {
            organization.setPartOf(new Reference().setIdentifier(new Identifier().setValue(theRecord.get("Commissioner")).setSystem(CareConnectSystem.ODSOrganisationCode)));
        }
        organization.setActive(true);
        if (!theRecord.get("CloseDate").isEmpty()) {
            organization.setActive(false);
        }
        if (typeSncCT != null) {
            organization.addType().addCoding().setDisplay(typeDisplay)
                    .setSystem(CareConnectSystem.OrganisationRole)
                    .setCode(typeSncCT);


        } else {
            organization.addType().addCoding()
                    .setSystem(CareConnectSystem.OrganisationType)
                    .setCode("prov")
                    .setDisplay("Healthcare Provider");
        }

        organization.addAddress()
                .setUse(Address.AddressUse.WORK)
                .addLine(odsUploader.Inicaps(theRecord.get("AddressLine_1")))
                .addLine(odsUploader.Inicaps(theRecord.get("AddressLine_2")))
                .addLine(odsUploader.Inicaps(theRecord.get("AddressLine_3")))
                .setCity(odsUploader.Inicaps(theRecord.get("AddressLine_4")))
                .setDistrict(odsUploader.Inicaps(theRecord.get("AddressLine_5")))
                .setPostalCode(theRecord.get("Postcode"));
        odsUploader.orgs.add(organization);
        //System.out.println(ctx.newXmlParser().setPrettyPrint(true).encodeResourceToString(organization));
        odsUploader.uploadOrganisation();

    }
}
