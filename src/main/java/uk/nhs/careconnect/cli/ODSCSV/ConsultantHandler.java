package uk.nhs.careconnect.cli.ODSCSV;

import org.apache.commons.csv.CSVRecord;
import org.hl7.fhir.r4.model.*;
import uk.org.hl7.fhir.core.Stu3.CareConnectSystem;

public class ConsultantHandler implements ODSUploader.IRecordHandler {


    public ConsultantHandler(ODSUploader _odsUploader) {
        this.odsUploader = _odsUploader;
    };

    ODSUploader odsUploader;

    @Override
    public void accept(CSVRecord theRecord) throws InterruptedException {
        // System.out.println(theRecord.toString());
        Practitioner practitioner = new Practitioner();
        practitioner.setId("dummy");

        if (theRecord.get(1).startsWith("C")) {
            practitioner.addIdentifier()
                    .setSystem(CareConnectSystem.GMCNumber)
                    .setValue(theRecord.get(1));
        } else {
            practitioner.addIdentifier()
                    .setSystem(CareConnectSystem.GMPNumber)
                    .setValue(theRecord.get(1));
        }

        practitioner.setActive(true);

        if (!theRecord.get(2).isEmpty()) {

            HumanName name = new HumanName();
            practitioner.getName().add(name);
            name.setFamily(odsUploader.Inicaps(theRecord.get(2)));
            name.addPrefix("Dr");

            if (!theRecord.get(3).isEmpty()) {
                name.addGiven(theRecord.get(3));
            }
        }
        if (!theRecord.get(4).isEmpty()) {
            switch (theRecord.get(4)) {
                case "M" : practitioner.setGender(Enumerations.AdministrativeGender.MALE);
                    break;
                case "F" : practitioner.setGender(Enumerations.AdministrativeGender.FEMALE);
                    break;
            }
        }


        odsUploader.docs.add(practitioner);

        // TODO Missing addition of specialty field 5 and organisation field 7



        PractitionerRole role = new PractitionerRole();

        if (!theRecord.get(7).isEmpty()) {
            Organization parentOrg = odsUploader.getOrganisationODS(theRecord.get(7));

            if (parentOrg != null) {
                role.setOrganization(new Reference().setIdentifier(new Identifier().setValue(parentOrg.getId()).setSystem(CareConnectSystem.ODSOrganisationCode)).setDisplay(parentOrg.getName()));
            }
        }
        role.addIdentifier()
                .setSystem(CareConnectSystem.IDOrgComb)
                .setValue(theRecord.get(1));
        // Make a note of the practitioner. Will need to change to correct code
        CodeableConcept concept = new CodeableConcept();
        concept.addCoding()
                .setSystem(CareConnectSystem.SNOMEDCT)
                .setCode("768839008")
                .setDisplay("Consultant");
        role.getCode().add(concept);
        role.setPractitioner(new Reference().setIdentifier(practitioner.getIdentifierFirstRep()));
        role.setActive(true);
        /* TODO basic ConceptMapping */

        if (!theRecord.get(5).isEmpty()) {
            CodeableConcept specialty = new CodeableConcept();
            specialty.addCoding()
                    .setSystem(CareConnectSystem.NHSDictionaryClinicalSpecialty)
                    .setCode(theRecord.get(5));
            role.getSpecialty().add(specialty);
        }
        odsUploader.roles.add(role);
    }



}
