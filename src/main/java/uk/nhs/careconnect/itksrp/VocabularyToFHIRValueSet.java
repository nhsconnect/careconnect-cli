package uk.nhs.careconnect.itksrp;

import ca.uhn.fhir.context.FhirContext;
import org.apache.commons.lang3.math.NumberUtils;
import org.hl7.fhir.dstu3.model.CodeSystem;
import org.hl7.fhir.dstu3.model.Enumerations;
import org.hl7.fhir.dstu3.model.ValueSet;
import uk.gov.hscic.schema.VocabularyIndex;


public class VocabularyToFHIRValueSet  {

	public VocabularyToFHIRValueSet(FhirContext ctx)
	{
		this.ctx = ctx;
	}
	private FhirContext ctx;
	

	public ValueSet process(Vocabulary vocab, VocabularyIndex vocabularyIndex, String prefix, CodeSystem codeSystem)  {

       ValueSet valueSet = new ValueSet();


        String vocabName = vocab.getName().replace(" ","");

		if (vocab.getId() != null) {
			//System.out.println(vocab.getId() + " size = "+vi.getVocabulary().size());
			for(VocabularyIndex.Vocabulary vocIndex :vocabularyIndex.getVocabulary()) {
				if (vocab.getId().equals(vocIndex.getId())) {
					//System.out.println("Correct name = " + vocIndex.getName());
					vocabName = vocIndex.getName().replace("/","-");
				}
			}
		}

        String system = "https://hl7.nhs.uk/"+prefix+"/ValueSet/"+vocabName;


        valueSet.setUrl(system);

        valueSet.setName(vocab.name);

        if (vocab.getId() != null) {
			valueSet.addIdentifier().setSystem("urn:ietf:rfc:3986").setValue("urn:oid:" + vocab.getId());
		}

        valueSet.setVersion(vocab.getVersion());

		if (vocab.getDescription() != null) {
			valueSet.setDescription(vocab.getDescription());
		} else {
			valueSet.setDescription("HSCIC Interoperability Specifications Reference Pack export.");
		}

		valueSet.setPublisher("NHS Digital");

		valueSet.setCopyright("Copyright 2019 © NHS Digital");

        switch(vocab.getStatus())
        {
            case "active" :
            case "Active" :
            case "created" :
                valueSet.setStatus(Enumerations.PublicationStatus.ACTIVE);
                break;
            case "superseded" :
                valueSet.setStatus(Enumerations.PublicationStatus.RETIRED);
                break;
            default:
                valueSet.setStatus(Enumerations.PublicationStatus.NULL);
        }


		ValueSet.ConceptSetComponent include =valueSet.getCompose().addInclude();

		if (codeSystem != null) {
			include.setSystem(codeSystem.getUrl());
		}
		else
		{

				include.setSystem("http://snomed.info/sct");
				if (vocab.getConcept() == null) return null;
				if (vocab.getConcept().size() == 0) return null;
				for (Vocabulary.Concept vocabconcept : vocab.getConcept())
				{
					ValueSet.ConceptReferenceComponent concept = new ValueSet.ConceptReferenceComponent();

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

					include.addConcept(concept);
				}



		}

		return valueSet;
	}

}
