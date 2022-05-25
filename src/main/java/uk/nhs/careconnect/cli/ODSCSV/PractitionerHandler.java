package uk.nhs.careconnect.cli.ODSCSV;

import org.apache.commons.csv.CSVRecord;
import org.hl7.fhir.r4.model.*;
import uk.org.hl7.fhir.core.Stu3.CareConnectSystem;

public class PractitionerHandler implements ODSUploader.IRecordHandler {
    PractitionerHandler(ODSUploader _odsUploader) {
        this.odsUploader = _odsUploader;
    };

    ODSUploader odsUploader;

    @Override
    public void accept(CSVRecord theRecord) throws InterruptedException {

        Practitioner practitioner = new Practitioner();
        practitioner.setId("dummy");

        if (theRecord.get("OrganisationCode").startsWith("C")) {
            practitioner.addIdentifier()
                    .setSystem(CareConnectSystem.GMCNumber)
                    .setValue(theRecord.get("OrganisationCode"));
        } else {
            practitioner.addIdentifier()
                    .setSystem(CareConnectSystem.GMPNumber)
                    .setValue(theRecord.get("OrganisationCode"));
        }


        if (!theRecord.get("ContactTelephoneNumber").isEmpty()) {
            practitioner.addTelecom()
                    .setUse(ContactPoint.ContactPointUse.WORK)
                    .setValue(theRecord.get("ContactTelephoneNumber"))
                    .setSystem(ContactPoint.ContactPointSystem.PHONE);
        }
        practitioner.setActive(true);
        if (!theRecord.get("CloseDate").isEmpty()) {
            practitioner.setActive(false);
        }
        practitioner.addAddress()
                .setUse(Address.AddressUse.WORK)
                .addLine(odsUploader.Inicaps(theRecord.get("AddressLine_1")))
                .addLine(odsUploader.Inicaps(theRecord.get("AddressLine_2")))
                .addLine(odsUploader.Inicaps(theRecord.get("AddressLine_3")))
                .setCity(odsUploader.Inicaps(theRecord.get("AddressLine_4")))
                .setDistrict(odsUploader.Inicaps(theRecord.get("AddressLine_5")))
                .setPostalCode(theRecord.get("Postcode"));

        if (!theRecord.get("Name").isEmpty()) {
            String[] nameStr = theRecord.get("Name").split(" ");

            if (nameStr.length>0) {
                HumanName name = new HumanName();
                practitioner.getName().add(name);
                name.setFamily(odsUploader.Inicaps(nameStr[0]));
                name.addPrefix("Dr");
                String foreName = "";
                for (Integer f=1; f<nameStr.length;f++) {
                    if (f==1) {
                        foreName = nameStr[1];
                    } else {
                        foreName = foreName + " " + nameStr[f];
                    }
                }
                if (!foreName.isEmpty()) {
                    name.addGiven(foreName);
                }
            }
        }
        odsUploader.docs.add(practitioner);

        PractitionerRole role = new PractitionerRole();

        if (!theRecord.get("Commissioner").isEmpty()) {
            role.setOrganization(new Reference().setIdentifier(new Identifier().setValue(theRecord.get("Commissioner")).setSystem(CareConnectSystem.ODSOrganisationCode)));
        }
        role.addIdentifier()
                .setSystem(CareConnectSystem.IDOrgComb)
                .setValue(theRecord.get("OrganisationCode")+theRecord.get("Commissioner"));
        // Make a note of the practitioner. Will need to change to correct code
        role.setPractitioner(new Reference().setIdentifier(practitioner.getIdentifierFirstRep()));
        if (!theRecord.get("OrganisationSubTypeCode").isEmpty()) {
            switch (theRecord.get("OrganisationSubTypeCode")) {
                case "O":
                case "P":
                    CodeableConcept concept = new CodeableConcept();
                    concept.addCoding()
                            .setSystem(CareConnectSystem.SNOMEDCT)
                            .setCode("62247001")
                            .setDisplay("General practitioner");
                    role.getCode().add(concept);
            }
        }
        role.setActive(true);
        if (!theRecord.get("CloseDate").isEmpty()) {
            role.setActive(false);
        }
        CodeableConcept specialty = new CodeableConcept();
        specialty.addCoding()
                .setSystem(CareConnectSystem.SNOMEDCT)
                .setCode("394814009")
                .setDisplay("General practice (specialty) (qualifier value)");
        role.getSpecialty().add(specialty);
        // System.out.println(ctx.newXmlParser().setPrettyPrint(true).encodeResourceToString(role));
        odsUploader.roles.add(role);
        odsUploader.uploadPractitioner();

    }
}
