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
		
		if (vocab.getId().contains("."))
		{
				if (vocab.getId().equals("2.16.840.1.113883.2.1.3.2.4.15"))
				{
					system="http://snomed.info/sct";
				}
		}
		else if (NumberUtils.isNumber(vocab.getId())) 
		{
			system = "http://snomed.info/sct";

		}
		else
		{
			// May not be robust
			system = "http://snomed.info/sct";
		}

		String idStr = vocabName;

      //  codeSystem.setId(idStr);

        codeSystem.setUrl(system);

     //   codeSystem.getCodeSystem().setSystem(system);
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


        for (int f=0;f<vocab.getConcept().size();f++)
		{
				CodeSystem.ConceptDefinitionComponent concept = new CodeSystem.ConceptDefinitionComponent();
				
				concept.setCode(vocab.getConcept().get(f).getCode().toString());
				for (int g=0;g<vocab.getConcept().get(f).getDisplayName().size();g++)
				{
					if (vocab.getConcept().get(f).getDisplayName().get(g).getType() != null)
					{
						if (vocab.getConcept().get(f).getDisplayName().get(g).getType().equals("PT"))
						{
							concept.setDisplay(vocab.getConcept().get(f).getDisplayName().get(g).getValue());
						}
					}
					else
					{
						concept.setDisplay(vocab.getConcept().get(f).getDisplayName().get(g).getValue());
					}
				}
				codeSystem.addConcept(concept);
			}

		

		return codeSystem;
	}

}
