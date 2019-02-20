package uk.nhs.careconnect.itksrp;

import ca.uhn.fhir.context.FhirContext;
import org.hl7.fhir.dstu3.model.CodeSystem;
import org.hl7.fhir.dstu3.model.Enumerations;
import org.hl7.fhir.dstu3.model.NamingSystem;
import uk.gov.hscic.schema.VocabularyIndex;


public class VocabularyToFHIRNamingSystem {

	public VocabularyToFHIRNamingSystem(FhirContext ctx)
	{
		this.ctx = ctx;
	}
	private FhirContext ctx;
	

	public NamingSystem process(Vocabulary vocab, VocabularyIndex vocabularyIndex, String prefix, CodeSystem codeSystem)  {

       NamingSystem namingSystem = new NamingSystem();


        String vocabName = vocab.getName().replace(" ","");

		if (vocab.getId() != null) {

			for(VocabularyIndex.Vocabulary vocIndex :vocabularyIndex.getVocabulary()) {
				if (vocab.getId().equals(vocIndex.getId())) {
					//System.out.println("Correct name = " + vocIndex.getName());
					vocabName = vocIndex.getName().replace("/","-");
				}
			}
		}

        String system = "https://hl7.nhs.uk/"+prefix+"/NamingSystem/"+vocabName;


        namingSystem.setUrl(system);

        namingSystem.setName(vocab.name);

        namingSystem.setKind(NamingSystem.NamingSystemType.CODESYSTEM);

       

        namingSystem.setVersion(vocab.getVersion());

		if (vocab.getDescription() != null) {
			namingSystem.setDescription(vocab.getDescription());
		} else {
			namingSystem.setDescription("HSCIC Interoperability Specifications Reference Pack export.");
		}

		namingSystem.setPublisher("NHS Digital");
		

        switch(vocab.getStatus())
        {
            case "active" :
            case "Active" :
            case "created" :
                namingSystem.setStatus(Enumerations.PublicationStatus.ACTIVE);
                break;
            case "superseded" :
                namingSystem.setStatus(Enumerations.PublicationStatus.RETIRED);
                break;
            default:
                namingSystem.setStatus(Enumerations.PublicationStatus.NULL);
        }

        namingSystem.addUniqueId()
				.setType(NamingSystem.NamingSystemIdentifierType.OID)
				.setValue(vocab.getId());

		namingSystem.addUniqueId()
				.setType(NamingSystem.NamingSystemIdentifierType.URI)
				.setValue(codeSystem.getUrl())
				.setPreferred(true);


		return namingSystem;
	}

}
