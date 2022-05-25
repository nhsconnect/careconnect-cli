package uk.nhs.careconnect.cli.ODSCSV;

import ca.uhn.fhir.context.FhirContext;
import ca.uhn.fhir.context.FhirVersionEnum;
import ca.uhn.fhir.rest.api.MethodOutcome;
import ca.uhn.fhir.rest.client.api.IGenericClient;
import ca.uhn.fhir.rest.server.exceptions.InternalErrorException;
import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.Option;
import org.apache.commons.cli.Options;
import org.apache.commons.cli.ParseException;
import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVParser;
import org.apache.commons.csv.CSVRecord;
import org.apache.commons.csv.QuoteMode;
import org.apache.commons.io.Charsets;
import org.apache.commons.io.IOUtils;
import org.apache.commons.io.input.BOMInputStream;
import org.apache.commons.lang3.StringUtils;
import org.hl7.fhir.instance.model.api.IIdType;
import org.hl7.fhir.r4.model.*;
import uk.nhs.careconnect.cli.BaseCommand;
import uk.nhs.careconnect.cli.CognitoIdpInterceptor;
import uk.nhs.careconnect.cli.CommandFailureException;
import uk.org.hl7.fhir.core.Stu3.CareConnectSystem;


import java.io.*;
import java.util.*;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

import static java.lang.Thread.sleep;
import static org.apache.commons.lang3.StringUtils.isBlank;
import static org.apache.commons.lang3.StringUtils.isNotBlank;

public class ODSUploader extends BaseCommand {

    public ODSUploader(String _apiKey,
                       String _userName,
                       String _password,
                       String _clientId) {
        this.apiKey = _apiKey;
        this.password = _password;
        this.userName = _userName;
        this.clientId = _clientId;
    }

    private String apiKey;
    private String userName;
    private String password;
    private String clientId;


	private static final org.slf4j.Logger ourLog = org.slf4j.LoggerFactory.getLogger(ODSUploader.class);

	private ArrayList<IIdType> myExcludes = new ArrayList<>();

	public ArrayList<Organization> orgs = new ArrayList<>();

    public ArrayList<Practitioner> docs = new ArrayList<>();

    public ArrayList<PractitionerRole> roles = new ArrayList<>();

    public ArrayList<Location> locs = new ArrayList<>();

    private Map<String,Organization> orgMap = new HashMap<>();

    private Map<String,Practitioner> docMap = new HashMap<>();

    private Map<String,PractitionerRole> roleMap = new HashMap<>();

    FhirContext ctx ;

    IGenericClient client;

/* PROGRAM ARGUMENTS

upload-ods
-t
http://127.0.0.1:8080/careconnect-ri/STU3

    */

	@Override
	public String getCommandDescription() {
		return "Uploads the ods/sds resources from NHS Digital.";
	}

	@Override
	public String getCommandName() {
		return "upload-ods";
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
	public void run(CommandLine theCommandLine) throws ParseException, InterruptedException {
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

		if (ctx.getVersion().getVersion() == FhirVersionEnum.R4) {
            client = ctx.newRestfulGenericClient(targetServer);
            client.registerInterceptor(new CognitoIdpInterceptor(apiKey,userName,password,clientId) );

            IRecordHandler handler = null;

            System.out.println("Pharmacy HQ");
            handler = new OrgHandler(this, CareConnectSystem.ODSOrganisationCode,"181","PHARMACY HEADQUARTER");
            uploadODSStu3(handler, targetServer, ctx, ',', QuoteMode.NON_NUMERIC, "epharmacyhq.zip", "epharmacyhq.csv");

            System.out.println("Dispensary");
            handler = new OrgHandler(this, CareConnectSystem.ODSOrganisationCode,"182","PHARMACY");
            uploadODSStu3(handler, targetServer, ctx, ',', QuoteMode.NON_NUMERIC, "edispensary.zip", "edispensary.csv");
            //uploadOrganisation();

            System.out.println("National Health Service Trust");
            handler = new OrgHandler(this,CareConnectSystem.ODSOrganisationCode, "197","NHS TRUST");
            uploadODSStu3(handler, targetServer, ctx, ',', QuoteMode.NON_NUMERIC, "etr.zip", "etr.csv");
            //uploadOrganisation();

            System.out.println("Health Authority (CCG)");
            handler = new OrgHandler(this,CareConnectSystem.ODSOrganisationCode, "98","CLINICAL COMMISSIONING GROUP");
            uploadODSStu3(handler, targetServer, ctx, ',', QuoteMode.NON_NUMERIC, "eccg.zip", "eccg.csv");
            //uploadOrganisation();

            System.out.println("General practice");
            handler = new OrgHandler(this,CareConnectSystem.ODSOrganisationCode,"76","GP PRACTICE");
            uploadODSStu3(handler, targetServer, ctx, ',', QuoteMode.NON_NUMERIC, "epraccur.zip", "epraccur.csv");uploadOrganisation();

            System.out.println("GP");
            handler = new PractitionerHandler(this);
            uploadODSStu3(handler, targetServer, ctx, ',', QuoteMode.NON_NUMERIC, "egpcur.zip", "egpcur.csv");
            //uploadPractitioner();

            System.out.println("Consultants");
            handler = new ConsultantHandler(this);
            uploadODSStu3(handler, targetServer, ctx, ',', QuoteMode.NON_NUMERIC, "econcur.zip", "econcur.csv");
            //uploadPractitioner();

/*
TODO?
            System.out.println("GP practice site");
            handler = new LocationHandler(odsUploader."394761003", "GP practice site");
            uploadODSStu3(handler, targetServer, ctx, ',', QuoteMode.NON_NUMERIC, "ebranchs.zip", "ebranchs.csv");
            uploadLocation();
*/
		}

	}

	public void uploadOrganisation() throws InterruptedException {
	    for (Organization organization : orgs) {
            Organization tempOrg = getOrganisationODS(organization.getIdentifier().get(0).getValue());

            if (organization.hasPartOf() && organization.getPartOf().hasIdentifier()) {
                Organization org = getOrganisationODS(organization.getPartOf().getIdentifier().getValue());
                if (org !=null) organization.getPartOf().setReference(organization.getId());
            }


            MethodOutcome outcome = null;
            if (tempOrg != null) {
                //Organization temp = (Organization)
                organization.setId(tempOrg.getId());
                if (checkUpdatedOrganization(organization, tempOrg)) {
                    Integer retry = 3;
                    while (retry > 0) {
                        try {
                            outcome = client.update().resource(organization).execute();
                            break;
                        } catch (Exception ex) {
                            // do nothing
                            ourLog.error(ex.getMessage());
                            retry--;
                        }

                    }
                }

            } else {
                Integer retry = 3;
                while (retry > 0) {
                    try {
                        outcome = client.create().resource(organization)
                                .execute();
                        break;
                    } catch (Exception ex) {
                        // do nothing
                        ourLog.error(ex.getMessage());
                        sleep(1000);
                        retry--;
                    }
                }
            }
            if (outcome != null & outcome.getId() != null ) {
                organization.setId(outcome.getId().getIdPart());

            }
        }
        orgs.clear();
    }
    private void uploadLocation() {
	    try {
            for (Location location : locs) {
                MethodOutcome outcome = client.update().resource(location)
                        .conditionalByUrl("Location?identifier=" + location.getIdentifier().get(0).getSystem() + "%7C" + location.getIdentifier().get(0).getValue())
                        .execute();

                if (outcome.getId() != null) {
                    location.setId(outcome.getId().getIdPart());
                }
            }
        } catch (Exception ex) {
	        System.out.println("Unable to upload Location. Does server support locations?");
        }
        locs.clear();
    }
    public void uploadPractitioner() throws InterruptedException {


        for (Practitioner practitioner : docs) {
            if (practitioner.getActive()) {
                Practitioner tempPractitioner = getPractitionerById(practitioner.getIdentifier().get(0).getSystem(), practitioner.getIdentifier().get(0).getValue());

                MethodOutcome outcome = null;
                if (tempPractitioner != null) {

                    practitioner.setId(tempPractitioner.getId());
                    // Need to preserve identifiers on AWS
                    if (practitioner.getIdentifier() != null) {
                        for (Identifier identifier : tempPractitioner.getIdentifier()) {
                            if (!identifier.getValue().equals(practitioner.getIdentifier().get(0).getValue())) {
                                practitioner.addIdentifier(identifier);
                            }
                        }
                    }
                    if (checkUpdatedPractitioner(practitioner, tempPractitioner)) {
                        Integer retry = 3;
                        while (retry > 0) {
                            try {
                                outcome = client.update().resource(practitioner).execute();
                                break;
                            } catch (Exception ex) {
                                // do nothing
                                ourLog.error(ex.getMessage());
                                retry--;
                            }

                        }
                    }
                } else {
                    Integer retry = 3;
                    while (retry > 0) {
                        try {
                             outcome = client.create().resource(practitioner)
                            .execute();
                            break;
                        } catch (Exception ex) {
                            // do nothing
                            ourLog.error(ex.getMessage());
                            sleep(1000);
                            retry--;
                        }
                    }

                }
                if (outcome != null && outcome.getId() != null) {
                    practitioner.setId(outcome.getId());
                }
            }
        }

        docs.clear();

        for (PractitionerRole practitionerRole : roles) {

            if (practitionerRole.getActive()) {

                PractitionerRole tempPractitionerRole = getPractitionerRoleById(practitionerRole.getIdentifier().get(0).getSystem(), practitionerRole.getIdentifier().get(0).getValue());

                if (practitionerRole.hasOrganization() && practitionerRole.getOrganization().hasIdentifier()) {
                    Organization organization = getOrganisationODS(practitionerRole.getOrganization().getIdentifier().getValue());
                    if (organization !=null) practitionerRole.getOrganization().setReference(organization.getId());
                }
                if (practitionerRole.hasPractitioner() && practitionerRole.getPractitioner().hasIdentifier()) {
                    Practitioner practitioner = getPractitionerById(practitionerRole.getPractitioner().getIdentifier().getSystem(), practitionerRole.getPractitioner().getIdentifier().getValue());
                    if (practitioner != null) practitionerRole.getPractitioner().setReference(practitioner.getId());
                }
                MethodOutcome outcome = null;
                if (tempPractitionerRole != null) {

                    practitionerRole.setId(tempPractitionerRole.getId());
                    // Need to preserve identifiers on AWS
                    if (practitionerRole.getIdentifier() != null) {
                        for (Identifier identifier : tempPractitionerRole.getIdentifier()) {
                            if (!identifier.getValue().equals(practitionerRole.getIdentifier().get(0).getValue())) {
                                practitionerRole.addIdentifier(identifier);
                            }
                        }
                    }
                    if (checkUpdatedPractitionerRole(practitionerRole, tempPractitionerRole)) {
                        Integer retry = 3;
                        while (retry > 0) {
                            try {
                                outcome = client.update().resource(practitionerRole).execute();
                                break;
                            } catch (Exception ex) {
                                // do nothing
                                ourLog.error(ex.getMessage());
                                sleep(1000);
                                retry--;
                            }
                        }
                    }

                } else {
                    Integer retry = 3;
                    while (retry > 0) {
                        try {
                            outcome = client.create().resource(practitionerRole)
                                    .execute();
                            break;
                        } catch (Exception ex) {
                            // do nothing
                            ourLog.error(ex.getMessage());
                            sleep(1000);
                            retry--;
                        }
                    }

                }
                if (outcome != null && outcome.getId() != null) {
                    practitionerRole.setId(outcome.getId());
                }
            }
        }
        roles.clear();

    }

    private boolean checkUpdatedPractitionerRole(PractitionerRole practitionerRole, PractitionerRole tempPractitionerRole) {
        if (tempPractitionerRole.getActive() != practitionerRole.getActive()) return true;
        if (tempPractitionerRole.hasPractitioner() && !tempPractitionerRole.getPractitioner().hasReference() && practitionerRole.hasPractitioner()) return true;
        return false;
    }

    private boolean checkUpdatedPractitioner(Practitioner practitioner, Practitioner tempPractitioner) {
        if (tempPractitioner.getActive() != practitioner.getActive()) return true;
        return false;
    }

    private boolean checkUpdatedOrganization(Organization organization, Organization tempOrganization) {
        if (tempOrganization.getActive() != organization.getActive()) return true;
        if (!tempOrganization.getTypeFirstRep().getCodingFirstRep().getCode().equals(organization.getTypeFirstRep().getCodingFirstRep().getCode())) return true;
        return false;
    }


    private void uploadODSStu3(IRecordHandler handler, String targetServer, FhirContext ctx, char theDelimiter, QuoteMode theQuoteMode, String fileName, String fileNamePart) throws CommandFailureException {


	    Boolean found = false;
	    try {
            ClassLoader classLoader = getClass().getClassLoader();

            List<byte[]> theZipBytes = new ArrayList<>();
            byte[] nextData = IOUtils.toByteArray(classLoader.getResourceAsStream("ods/"+fileName));
            theZipBytes.add(nextData);



            for (byte[] nextZipBytes : theZipBytes) {
                ZipInputStream zis = new ZipInputStream(new BufferedInputStream(new ByteArrayInputStream(nextZipBytes)));
                try {
                    for (ZipEntry nextEntry; (nextEntry = zis.getNextEntry()) != null; ) {
                        ZippedFileInputStream inputStream = new ZippedFileInputStream(zis);

                        String nextFilename = nextEntry.getName();
                        if (nextFilename.contains(fileNamePart)) {

                            ourLog.info("Processing file {}", nextFilename);
                            found = true;

                            Reader reader = null;
                            CSVParser parsed = null;
                            try {
                                reader = new InputStreamReader(new BOMInputStream(zis), Charsets.UTF_8);
                                CSVFormat format = CSVFormat
                                        .newFormat(theDelimiter)
                                        .withAllowMissingColumnNames()
                                        .withHeader("OrganisationCode"
                                                ,"Name"
                                                ,"NationalGrouping"
                                                ,"HighLevelHealthGeography"
                                                ,"AddressLine_1"
                                                ,"AddressLine_2"
                                                ,"AddressLine_3"
                                                ,"AddressLine_4"
                                                ,"AddressLine_5"
                                                ,"Postcode"
                                                ,"OpenDate"
                                                ,"CloseDate"
                                                ,"Fld13"
                                                ,"OrganisationSubTypeCode"
                                                ,"Commissioner"
                                                ,"Fld16"
                                                ,"Fld17"
                                                ,"ContactTelephoneNumber"
                                        );
                                if (theQuoteMode != null) {
                                    format = format.withQuote('"').withQuoteMode(theQuoteMode);
                                }
                                parsed = new CSVParser(reader, format);
                                Iterator<CSVRecord> iter = parsed.iterator();
                                ourLog.debug("Header map: {}", parsed.getHeaderMap());


                                int count = 0;

                                while (iter.hasNext()) {
                                    CSVRecord nextRecord = iter.next();
                                    handler.accept(nextRecord);
                                    count++;
                                    count++;
                                    if ((count % 20) ==0 ) {
                                        System.out.println(count);
                                        ourLog.info(" * Processed {} records in {}", count, nextFilename);
                                    }
                                }

                            } catch (IOException e) {
                                throw new InternalErrorException(e);
                            }
                        }
                    }
                } catch (Exception ex) {
                    System.out.println(ex.getMessage());
                }

            }
        } catch(Exception ex){
            System.out.println(ex.getMessage());
        }

	}

    public interface IRecordHandler {
        void accept(CSVRecord theRecord) throws InterruptedException;
    }
    public String Inicaps(String string) {
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



    private Practitioner getPractitionerById(String idSystem, String idCode ) throws InterruptedException {
        Practitioner practitioner = docMap.get(idCode);
        if (practitioner != null) return practitioner;
        Bundle bundle = null;
        Integer retry =3;
        while (retry > 0) {
            try {
                bundle = client.search()
                        .byUrl("Practitioner?identifier=" + idSystem + "%7C" +idCode)
                        .returnBundle(org.hl7.fhir.r4.model.Bundle.class)
                        .execute();
                if (bundle.getEntry().size()>0 && bundle.getEntry().get(0).getResource() instanceof Practitioner) {
                    practitioner = (Practitioner) bundle.getEntry().get(0).getResource();
                    docMap.put(idCode, practitioner);
                    return practitioner;
                } else {
                    return null;
                }
            } catch (Exception ex) {
                // do nothing
                ourLog.error(ex.getMessage());
                sleep(1000);
                retry--;
            }
        }

        return null;
    }

    private PractitionerRole getPractitionerRoleById(String idSystem, String idCode ) throws InterruptedException {
        PractitionerRole role = roleMap.get(idCode);
        if (role != null) return role;
        Bundle bundle = null;
        Integer retry =3;
        while (retry > 0) {
            try {
                bundle = client.search()
                    .byUrl("PractitionerRole?identifier=" + idSystem + "%7C" +idCode)
                    .returnBundle(org.hl7.fhir.r4.model.Bundle.class)
                    .execute();
                if (bundle.getEntry().size()>0 && bundle.getEntry().get(0).getResource() instanceof PractitionerRole) {
                    role = (PractitionerRole) bundle.getEntry().get(0).getResource();
                    roleMap.put(idCode, role);
                    return role;
                } else {
                    return null;
                }
            } catch (Exception ex) {
                // do nothing
                ourLog.error(ex.getMessage());
                sleep(1000);
                retry--;
            }

        }

        return null;
    }
    private Organization getOrganisationODS(String odsCode) throws InterruptedException {
        Organization organization = orgMap.get(odsCode);
        if (organization != null) return organization;
        Bundle bundle = null;
        Integer retry =3;
        while (retry > 0) {
            try {
                bundle = client.search()
                        .byUrl("Organization?identifier=" + CareConnectSystem.ODSOrganisationCode + "%7C" + odsCode)
                        .returnBundle(org.hl7.fhir.r4.model.Bundle.class)
                        .execute();
                if (bundle.getEntry().size()>0 && bundle.getEntry().get(0).getResource() instanceof Organization) {
                    organization = (Organization) bundle.getEntry().get(0).getResource();
                    orgMap.put(odsCode, organization);
                    return organization;
                } else {
                    return null;
                }
            } catch (Exception ex) {
                // do nothing
                ourLog.error(ex.getMessage());
                sleep(1000);
                retry--;
            }

        }

        return null;
    }

    private static class ZippedFileInputStream extends InputStream {

        private ZipInputStream is;

        public ZippedFileInputStream(ZipInputStream is) {
            this.is = is;
        }

        @Override
        public void close() throws IOException {
            is.closeEntry();
        }

        @Override
        public int read() throws IOException {
            return is.read();
        }
    }

}

