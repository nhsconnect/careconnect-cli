package uk.nhs.careconnect.itksrp;

import ca.uhn.fhir.context.FhirContext;
import org.apache.commons.lang3.math.NumberUtils;
import org.hl7.fhir.dstu3.model.Enumerations;
import org.hl7.fhir.dstu3.model.CodeSystem;


public class VocabularyToFHIRCodeSystem {

	public VocabularyToFHIRCodeSystem(FhirContext ctx)
	{
		this.ctx = ctx;
	}
	private FhirContext ctx;
	

	public CodeSystem process(Vocabulary vocab, String prefix)  {

       CodeSystem codeSystem = new CodeSystem();
		
        String vocabName = vocab.getName();
        String system = "https://hl7.nhs.uk/"+prefix+"/"+vocabName;
		
		String idStr = vocabName;

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
			codeSystem.setDescription("HSCIC Interoperability Specifications Reference Pack export. " + vocab.getDescription());
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
