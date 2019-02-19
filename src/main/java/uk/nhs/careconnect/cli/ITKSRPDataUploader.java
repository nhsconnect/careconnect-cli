package uk.nhs.careconnect.cli;

import ca.uhn.fhir.context.FhirContext;
import ca.uhn.fhir.rest.client.api.IGenericClient;
import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.Option;
import org.apache.commons.cli.Options;
import org.apache.commons.cli.ParseException;
import org.apache.commons.io.IOUtils;
import org.hl7.fhir.dstu3.model.CodeSystem;
import org.hl7.fhir.dstu3.model.ValueSet;
import org.hl7.fhir.dstu3.model.Bundle;
import org.hl7.fhir.instance.model.api.IIdType;
import uk.nhs.careconnect.itksrp.Vocabulary;
import uk.nhs.careconnect.itksrp.VocabularyToFHIRCodeSystem;

import javax.xml.bind.JAXBContext;
import javax.xml.bind.Unmarshaller;
import java.io.*;
import java.util.ArrayList;
import java.util.List;

import static org.apache.commons.lang3.StringUtils.isBlank;
import static org.apache.commons.lang3.StringUtils.isNotBlank;

public class ITKSRPDataUploader extends BaseCommand {

	private static final org.slf4j.Logger ourLog = org.slf4j.LoggerFactory.getLogger(ITKSRPDataUploader.class);

	private ArrayList<IIdType> myExcludes = new ArrayList<>();

	private ArrayList<ValueSet> valueSets = new ArrayList<>();

    private FhirContext ctx;

	IGenericClient client;

	@Override
	public String getCommandDescription() {
		return "Uploads the conformance resources (StructureDefinition and ValueSet) from the official FHIR definitions.";
	}

	@Override
	public String getCommandName() {
		return "upload-itksrp";
	}

	@Override
	public Options getOptions() {
		Options options = new Options();
		Option opt;

		addFhirVersionOption(options);

		opt = new Option("t", "target", true, "Base URL for the target server (e.g. \"http://example.com/fhir\")");
		opt.setRequired(true);
		options.addOption(opt);

		opt = new Option("e", "exclude", true, "Exclude uploading the given resources, e.g. \"-e dicom-dcim,foo\"");
		opt.setRequired(false);
		options.addOption(opt);

		return options;
	}

	@Override
	public void run(CommandLine theCommandLine) throws Exception {
		String targetServer = theCommandLine.getOptionValue("t");
		if (isBlank(targetServer)) {
			throw new ParseException("No target server (-t) specified");
		} else if (targetServer.startsWith("http") == false) {
			throw new ParseException("Invalid target server specified, must begin with 'http'");
		}

		ctx = getSpecVersionContext(theCommandLine);
		String exclude = theCommandLine.getOptionValue("e");

		if (isNotBlank(exclude)) {
			for (String next : exclude.split(",")) {
				if (isNotBlank(next)) {
					IIdType id = ctx.getVersion().newIdType();
					id.setValue(next);
					myExcludes.add(id);
				}
			}
		}
		client = ctx.newRestfulGenericClient(targetServer);

		loadFolder("itk/v2");
		loadFolder("itk/v3");
		loadFolder("itk/sct");



	}

	public void loadFolder(String folder) throws Exception {
		List<String> filenames = new ArrayList<>();

		try (
				InputStream in = getResourceAsStream(folder);
				BufferedReader br = new BufferedReader(new InputStreamReader(in))) {
			String resource;

			while ((resource = br.readLine()) != null) {
				filenames.add(resource);

				loadFile(folder,resource);
			}
		}


	}

	private InputStream getResourceAsStream(String resource) {
		final InputStream in
				= getContextClassLoader().getResourceAsStream(resource);

		return in == null ? getClass().getResourceAsStream(resource) : in;
	}

	private ClassLoader getContextClassLoader() {
		return Thread.currentThread().getContextClassLoader();
	}

	public void loadFile(String folder, String filename) {
		InputStream inputStream =
				Thread.currentThread().getContextClassLoader().getResourceAsStream(folder + "/" +filename);
		Reader reader = new InputStreamReader(inputStream);
		VocabularyToFHIRCodeSystem converter = new VocabularyToFHIRCodeSystem(ctx);

		try {


			JAXBContext jcvocab = JAXBContext.newInstance(Vocabulary.class);
			Unmarshaller unmarshallerVocab = jcvocab.createUnmarshaller();
			Vocabulary vocab = (Vocabulary) unmarshallerVocab.unmarshal(inputStream);

			String name = vocab.getName();

			System.out.println(vocab.getName()+ " Version: "+vocab.getVersion());
			if ((vocab.getStatus().contains("active") || vocab.getStatus().contains("created") ) && name!=null) {

				// Don't process SNOMED
				if (!folder.contains("sct") && !vocab.getId().equals("2.16.840.1.113883.2.1.3.2.4.15")) {
					CodeSystem codeSystem = converter.process(vocab,folder);
					System.out.println(ctx.newXmlParser().setPrettyPrint(true).encodeResourceToString(codeSystem));

					Bundle results = client.search().forResource(CodeSystem.class).where(CodeSystem.URL.matches().value(codeSystem.getUrl())).returnBundle(Bundle.class).execute();
					if (results.getEntry().size()>0) {
						client.update().resource(codeSystem).withId(results.getEntry().get(0).getId()).execute();
					} else {
						client.create().resource(codeSystem).execute();
					}

				}


			}
		} catch (Exception ex) {
			System.out.println(ex.getMessage());
		}

	}




	private void uploadValueSetsStu3(String targetServer) throws CommandFailureException {
		IGenericClient client = newClient(ctx, targetServer);
		ourLog.info("Uploading ValueSets to server: " + targetServer);

		long start = System.currentTimeMillis();

		String vsContents;


		int count = 1;
		/*
		for (ValueSet next : valueSets) {
		    System.out.println(next.getCodeSystem().getSystem());
            client.update().resource(next).execute();

			count++;
		}
		*/

		try {
			vsContents = IOUtils.toString(ITKSRPDataUploader.class.getResourceAsStream("/org/hl7/fhir/instance/model/valueset/" + "v3-codesystems.xml"), "UTF-8");
		} catch (IOException e) {
			throw new CommandFailureException(e.toString());
		}

		ourLog.info("Finished uploading ValueSets");

	}

}
