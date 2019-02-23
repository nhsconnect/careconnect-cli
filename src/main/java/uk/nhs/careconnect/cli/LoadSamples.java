package uk.nhs.careconnect.cli;

import ca.uhn.fhir.context.FhirContext;
import ca.uhn.fhir.context.FhirVersionEnum;
import ca.uhn.fhir.rest.api.MethodOutcome;
import ca.uhn.fhir.rest.client.api.IGenericClient;
import ca.uhn.fhir.rest.server.exceptions.InternalErrorException;
import ca.uhn.fhir.rest.server.exceptions.UnprocessableEntityException;
import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.Option;
import org.apache.commons.cli.Options;
import org.apache.commons.cli.ParseException;
import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVParser;
import org.apache.commons.csv.CSVRecord;
import org.apache.commons.csv.QuoteMode;
import org.apache.commons.lang3.StringUtils;
import org.hl7.fhir.dstu3.model.*;
import org.hl7.fhir.instance.model.api.IBaseResource;
import org.hl7.fhir.instance.model.api.IIdType;
import uk.nhs.careconnect.itksrp.Vocabulary;
import uk.nhs.careconnect.itksrp.VocabularyToFHIRCodeSystem;
import uk.nhs.careconnect.itksrp.VocabularyToFHIRNamingSystem;
import uk.nhs.careconnect.itksrp.VocabularyToFHIRValueSet;
import uk.org.hl7.fhir.core.Stu3.CareConnectExtension;
import uk.org.hl7.fhir.core.Stu3.CareConnectProfile;
import uk.org.hl7.fhir.core.Stu3.CareConnectSystem;

import javax.xml.bind.JAXBContext;
import javax.xml.bind.Unmarshaller;
import java.io.*;
import java.math.BigDecimal;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.concurrent.TimeUnit;

import static org.apache.commons.lang3.StringUtils.isBlank;

public class LoadSamples extends BaseCommand {

	private static final org.slf4j.Logger ourLog = org.slf4j.LoggerFactory.getLogger(LoadSamples.class);

	private ArrayList<IIdType> myExcludes = new ArrayList<>();

    private ArrayList<IBaseResource> resources = new ArrayList<>();

    private Map<String,String> orgMap = new HashMap<>();

    private Map<String,String> docMap = new HashMap<>();

    private Map<String,Patient> patientMap = new HashMap<>();

    private Map<String, Address> addressMap = new HashMap<>();

    private ArrayList<PractitionerRole> roles = new ArrayList<>();

    FhirContext ctx ;

    IGenericClient client;

    IGenericClient odsClient;

/* PROGRAM ARGUMENTS

load-samples
-t
http://127.0.0.1:8080/careconnect-ri/STU3

    */

    private String Inicaps(String string) {
        String result = null;
        String[] array = string.split(" ");

        for (int f=0; f<array.length;f++) {
            if (f==0) {
                result = StringUtils.capitalize(StringUtils.lowerCase(array[f]));
            } else
            {
                result = result + " "+ StringUtils.capitalize(StringUtils.lowerCase(array[f]));
            }
        }
        return result;
    }

	@Override
	public String getCommandDescription() {
		return "Uploads sample resources.";
	}

	@Override
	public String getCommandName() {
		return "load-samples";
	}

	public String stripChar(String string) {
	    string =string.replace(" ","");
        string =string.replace(":","");
        string =string.replace("-","");
        return string;
    }

	@Override
	public Options getOptions() {
		Options options = new Options();
		Option opt;

		addFhirVersionOption(options);

		opt = new Option("t", "target", true, "Base URL for the target server (e.g. \"http://example.com/fhir\")");
		opt.setRequired(true);
		options.addOption(opt);

        opt = new Option("a", "all", false, "All load files");
        opt.setRequired(false);
        options.addOption(opt);


        return options;
	}

	@Override
	public void run(CommandLine theCommandLine) throws ParseException {
		String targetServer = theCommandLine.getOptionValue("t");
        TimeZone.setDefault(TimeZone.getTimeZone("UTC"));
		if (isBlank(targetServer)) {
			throw new ParseException("No target server (-t) specified");
		} else if (targetServer.startsWith("http") == false) {
			throw new ParseException("Invalid target server specified, must begin with 'http'");
		}

		ctx = getSpecVersionContext(theCommandLine);

        ClassLoader classLoader = getClass().getClassLoader();

		if (ctx.getVersion().getVersion() == FhirVersionEnum.DSTU3) {

            client = ctx.newRestfulGenericClient(targetServer);

            odsClient = ctx.newRestfulGenericClient("https://directory.spineservices.nhs.uk/STU3/");

            System.out.println("HAPI Client created");

            CapabilityStatement capabilityStatement = null;
            Integer retries = 15; // This is 15 mins before giving up
            while (capabilityStatement == null && retries > 0) {
                try {
                    capabilityStatement = client.fetchConformance().ofType(CapabilityStatement.class).execute();
                } catch (Exception ex) {
                    ourLog.warn("Failed to load conformance statement, error was: {}", ex.toString());
                    System.out.println("Sleeping for a minute");
                    retries--;
                    try {
                        TimeUnit.MINUTES.sleep(1);
                    } catch (Exception ex1) {

                    }
                }
            }
            if (capabilityStatement == null) {
                System.out.println("Max number of attempts to connect exceeded. Aborting");
                return;
            } else {
                try {
                    loadFolder("samples");
                } catch(Exception ex) {
                    System.out.println(ex.getMessage());
                }
            }
            Integer resourceCount = 0;

        }
	}


    public void loadFolder(String folder) throws Exception {
        List<String> filenames = new ArrayList<>();

        try (
                InputStream in = getResourceAsStream(folder);
                BufferedReader br = new BufferedReader(new InputStreamReader(in))) {
                String resource;

                while ((resource = br.readLine()) != null) {
                    filenames.add(resource);

                    if (resource.contains(".")) {
                        loadFile(folder,resource);
                    } else {
                        loadFolder(folder + "/" + resource);
                    }

                }
        }


    }

    public void loadFile(String folder, String filename) {
        System.out.println(folder + "/" +filename);
        InputStream inputStream =
                Thread.currentThread().getContextClassLoader().getResourceAsStream(folder + "/" +filename);
        Reader reader = new InputStreamReader(inputStream);

        try {
            Bundle bundle = null;
            try {
                bundle = ctx.newXmlParser().parseResource(Bundle.class, reader);
            } catch (Exception ex) {
                bundle = ctx.newJsonParser().parseResource(Bundle.class, reader);
            }

            try {
                MethodOutcome outcome = client.create().resource(bundle).execute();
            } catch (UnprocessableEntityException ex) {

               // System.out.println(ctx.newXmlParser().encodeResourceToString(ex.getOperationOutcome()));
                if (ex.getStatusCode()==422) {
                    System.out.println("Trying to update "+filename+ ": Bundle?identifier="+bundle.getIdentifier().getSystem()+"|"+bundle.getIdentifier().getValue());
                    MethodOutcome outcome = client.update().resource(bundle).conditionalByUrl("Bundle?identifier="+bundle.getIdentifier().getSystem()+"|"+bundle.getIdentifier().getValue()).execute();
                }
            }

        } catch (Exception ex) {
            System.out.println("ERROR");
            System.out.println(ex.getMessage());
        }

    }


    private List<String> getResourceFiles(String path ) throws IOException {
        List<String> filenames = new ArrayList<>();

        try(
                InputStream in = getResourceAsStream( path );
                BufferedReader br = new BufferedReader( new InputStreamReader( in ) ) ) {
            String resource;

            while( (resource = br.readLine()) != null ) {
                filenames.add( resource );
            }
        }

        return filenames;
    }

    private InputStream getResourceAsStream(String resource ) {
        final InputStream in
                = getContextClassLoader().getResourceAsStream( resource );

        return in == null ? getClass().getResourceAsStream( resource ) : in;
    }

    private ClassLoader getContextClassLoader() {
        return Thread.currentThread().getContextClassLoader();
    }




}

