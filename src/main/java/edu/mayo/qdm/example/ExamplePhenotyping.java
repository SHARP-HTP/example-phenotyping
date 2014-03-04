package edu.mayo.qdm.example;

import edu.mayo.qdm.executor.Executor;
import edu.mayo.qdm.executor.ExecutorFactory;
import edu.mayo.qdm.executor.MeasurementPeriod;
import edu.mayo.qdm.executor.Results;
import edu.mayo.qdm.patient.Concept;
import edu.mayo.qdm.patient.Diagnosis;
import edu.mayo.qdm.patient.Encounter;
import edu.mayo.qdm.patient.Patient;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang.StringUtils;
import org.joda.time.DateTime;
import org.springframework.core.io.ClassPathResource;

import java.util.*;

/*
An example of QDM Phenotyping Executor usage.
 */
public class ExamplePhenotyping {

    public static void main(String[] args) throws Exception {
        new ExamplePhenotyping().run();
    }

    private void run() throws Exception {
        Executor executor = ExecutorFactory.instance().getExecutor();

        // build an iterator of patients from the CSV
        Iterable<Patient> patients = this.parseCsv();

        // get the QDM XML for NQF 0036
        String qdmXml = IOUtils.toString(new ClassPathResource("0036.xml").getInputStream());

        // set the measurement period for the current year
        MeasurementPeriod currentYear = MeasurementPeriod.getCalendarYear(new Date());

        System.out.println("Initializing Executor... please wait.");

        // start the executor given the 'patients' from above
        Results results = executor.execute(patients, qdmXml, currentYear, null);

        System.out.println("=============================");
        System.out.println("RESULTS:");
        System.out.println("--------");
        System.out.println(results.asMap());
        System.out.println("=============================");
    }

    /*
    The QDM Phenotyping executor requires an Iterable<Patient> object -- however that is obtained
    is up to the client. Here, we parse a CSV to get the results. This is incomplete, but it is
    the general idea.
     */
    private Iterable<Patient> parseCsv() throws Exception {
        List<String> lines = IOUtils.readLines(
                new ClassPathResource("AsthmaMedicationClincalDataFromMimmic.csv").getInputStream());

        //remove header
        lines.remove(0);

        Map<String,Patient> patients = new HashMap<String,Patient>();

        // loop through the CSV lines and build the patients
        for(String line : lines){
            if(StringUtils.isNotBlank(line)){
                String[] cols = StringUtils.split(line, ",");

                String patientId = cols[0];

                // keep the patients in a map, as it looks like there can be multiple rows per patient.
                if(! patients.containsKey(patientId)){
                    patients.put(patientId, new Patient(patientId));
                }

                Patient patient = patients.get(patientId);

                // add the diagnosis
                patient.addDiagnosis(new Diagnosis(new Concept(cols[2], "ICD9", null)));
            }
        }

        Collection<Patient> returnList = new ArrayList<Patient>(patients.values());

        // add in a fake test patient to show an algorithm hit
        returnList.add(this.buildTestPatient());

        return returnList;
    }

    /*
    This is a fake patient, just to demonstrate a hit on this algorithm.
     */
    private Patient buildTestPatient(){
        Patient patient = new Patient("test");
        patient.setBirthdate(new DateTime(1980,1,1,1,1).toDate());

        // Persistent Asthma Diagnosis
        Diagnosis persistentAshtma = new Diagnosis(new Concept("426656000", "SNOMED-CT", null), new Date(), new Date());
        patient.addDiagnosis(persistentAshtma);

        Encounter officeVisit = new Encounter("encounterId", new Concept("99201", "CPT", null), new Date(), new Date());
        patient.addEncounter(officeVisit);

        return patient;
    }
}
