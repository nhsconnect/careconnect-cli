package uk.nhs.careconnect.itksrp;

import ca.uhn.fhir.context.FhirContext;
import org.apache.commons.lang3.math.NumberUtils;
import org.hl7.fhir.dstu3.model.Enumerations;
import org.hl7.fhir.dstu3.model.CodeSystem;
import uk.gov.hscic.schema.VocabularyIndex;


public class VocabularyToFHIRCodeSystem {

	public VocabularyToFHIRCodeSystem(FhirContext ctx)
	{
		this.ctx = ctx;
	}
	private FhirContext ctx;
	

	public CodeSystem process(Vocabulary vocab, VocabularyIndex vocabularyIndex, String prefix)  {

       CodeSystem codeSystem = new CodeSystem();
		
        String vocabName = vocab.getName();

		if (vocab.getId() != null) {
			//System.out.println(vocab.getId() + " size = "+vi.getVocabulary().size());
			for(VocabularyIndex.Vocabulary vocIndex :vocabularyIndex.getVocabulary()) {
				if (vocab.getId().equals(vocIndex.getId())) {
					//System.out.println("Correct name = " + vocIndex.getName());
					vocabName = vocIndex.getName().replace("/","-");
				}
			}
		}

        String system = "https://hl7.nhs.uk/"+prefix+"/"+vocabName;

        codeSystem.setUrl(system);

        codeSystem.setName(vocab.name);
        String desc = vocab.getDescription();
        codeSystem.setDescription(desc);
        codeSystem.setVersion(vocab.getVersion());

        switch(vocab.getStatus())
        {
            case "active" :
            case "Active" :
            case "created" :
                codeSystem.setStatus(Enumerations.PublicationStatus.DRAFT);
                break;
            case "superseded" :
                codeSystem.setStatus(Enumerations.PublicationStatus.RETIRED);
                break;
            default:
                codeSystem.setStatus(Enumerations.PublicationStatus.NULL);
        }

		
        
		if (vocab.getDescription() != null) {
			codeSystem.setDescription(vocab.getDescription());
		} else {
			codeSystem.setDescription("HSCIC Interoperability Specifications Reference Pack export.");
		}

		codeSystem.setPublisher("NHS Digital");

		codeSystem.setCopyright("Copyright 2019 © NHS Digital");


        for (Vocabulary.Concept vocabconcept : vocab.getConcept())
		{
				CodeSystem.ConceptDefinitionComponent concept = new CodeSystem.ConceptDefinitionComponent();
				
				concept.setCode(vocabconcept.getCode());
				for (Vocabulary.Concept.DisplayName displayName :
					 vocabconcept.getDisplayName())
				{
					if (displayName.getType() != null)
					{
						if (displayName.getType().equals("PT"))
						{
							concept.setDisplay(displayName.getValue());
						}
					}
					else
					{
						concept.setDisplay(displayName.getValue());
					}
				}
				codeSystem.addConcept(concept);
			}

		

		return codeSystem;
	}

}
