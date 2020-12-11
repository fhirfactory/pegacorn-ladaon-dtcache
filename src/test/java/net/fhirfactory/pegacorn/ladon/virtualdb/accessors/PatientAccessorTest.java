package net.fhirfactory.pegacorn.ladon.virtualdb.accessors;

import static org.junit.Assert.assertTrue;

import java.io.File;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;

import javax.inject.Inject;

import net.fhirfactory.pegacorn.ladon.model.virtualdb.operations.VirtualDBActionStatusEnum;
import net.fhirfactory.pegacorn.ladon.model.virtualdb.operations.VirtualDBMethodOutcome;
import org.hl7.fhir.r4.model.Identifier;
import org.hl7.fhir.r4.model.Patient;
import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.junit.Arquillian;
import org.jboss.shrinkwrap.api.ArchivePath;
import org.jboss.shrinkwrap.api.Node;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.spec.WebArchive;
import org.jboss.shrinkwrap.resolver.api.maven.Maven;
import org.jboss.shrinkwrap.resolver.api.maven.MavenFormatStage;
import org.jboss.shrinkwrap.resolver.api.maven.MavenStrategyStage;
import org.jboss.shrinkwrap.resolver.api.maven.PomEquippedResolveStage;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import ca.uhn.fhir.parser.IParser;
import net.fhirfactory.pegacorn.deployment.topology.map.standalone.StandaloneSampleDeploymentSolution;
import net.fhirfactory.pegacorn.fhir.r4.samples.PatientSetFactory;
import net.fhirfactory.pegacorn.util.FHIRContextUtility;

@RunWith(Arquillian.class)
public class PatientAccessorTest {

    private static final Logger LOG = LoggerFactory.getLogger(PatientAccessorTest.class);

    @Inject
    PatientAccessor patientAccessor;

    @Inject
    PatientSetFactory patientSet;
    
    @Inject
    private FHIRContextUtility FHIRContextUtility;

    @Inject
    StandaloneSampleDeploymentSolution sampleSolution;

    @Deployment
    public static WebArchive createDeployment() {
        WebArchive testWAR;

        PomEquippedResolveStage pomEquippedResolver = Maven.resolver().loadPomFromFile("pom.xml");
        PomEquippedResolveStage pomEquippedResolverWithRuntimeDependencies = pomEquippedResolver.importRuntimeDependencies();
        MavenStrategyStage mavenResolver = pomEquippedResolverWithRuntimeDependencies.resolve();
        MavenFormatStage mavenFormat = mavenResolver.withTransitivity();
        File[] fileSet = mavenFormat.asFile();
        LOG.debug(".createDeployment(): ShrinkWrap Library Set for run-time equivalent, length --> {}", fileSet.length);
        for (int counter = 0; counter < fileSet.length; counter++) {
            File currentFile = fileSet[counter];
            LOG.trace(".createDeployment(): Shrinkwrap Entry --> {}", currentFile.getName());
        }
        testWAR = ShrinkWrap.create(WebArchive.class, "pegacorn-ladon-dtcache-test.war")
                .addAsLibraries(fileSet)
                .addPackages(true, "net.fhirfactory.pegacorn.ladon.dtcache")
                .addPackages(true, "net.fhirfactory.pegacorn.deployment.topology.map.samples.standalone")
                .addPackages(true, "net.fhirfactory.pegacorn.fhir.r4.samples")
                .addAsManifestResource("META-INF/beans.xml", "WEB-INF/beans.xml");
        if (LOG.isDebugEnabled()) {
            Map<ArchivePath, Node> content = testWAR.getContent();
            Set<ArchivePath> contentPathSet = content.keySet();
            Iterator<ArchivePath> contentPathSetIterator = contentPathSet.iterator();
            while (contentPathSetIterator.hasNext()) {
                ArchivePath currentPath = contentPathSetIterator.next();
                LOG.debug(".createDeployment(): pegacorn-ladon-dtcache-test Entry Path --> {}", currentPath.get());
            }
        }
        return (testWAR);
    }

    @Before
    public void setUp() {
        if (sampleSolution.hasBeenInitialised()) {
            LOG.debug(".setUp(): All Good!");
        } else {
            LOG.debug(".setUp(): Oh no! something is wrong with the sample topology data generation!");
        }
        patientAccessor.initialiseServices();
    }

    @Test
    public void addPatientResourceAndRetrieve() {
        LOG.debug(".addPatientResourceAndRetrieve(): Entry");

        LOG.trace(".addPatientResourceAndRetrieve(): Parser(s) Initiated!");
        HashSet<Identifier> patientIdSet = new HashSet<Identifier>();
        for (Patient patient : patientSet.getPateintSet()) {
            LOG.trace(".addPatientResourceAndRetrieve(): Adding Patient --> {}", patient);
            VirtualDBMethodOutcome outcome = patientAccessor.createResource(patient);
            if(outcome.getStatusEnum() == VirtualDBActionStatusEnum.CREATION_FINISH) {
                LOG.trace(".addPatientResourceAndRetrieve(): Resource Create was Successful");
                Identifier createdPatientIdentifier = outcome.getIdentifier();
                patientIdSet.add(createdPatientIdentifier);
            }
        }
        boolean testSuccess = true;
        IParser parserR4 = FHIRContextUtility.getJsonParser();
        int counter = 0;
        for (Identifier patientId : patientIdSet) {
            VirtualDBMethodOutcome outcome = patientAccessor.findResourceViaIdentifier(patientId);
            Patient retrievedPatient = (Patient)outcome.getResource();
            if (LOG.isTraceEnabled()) {
                String retrievedPatientAsString = parserR4.encodeResourceToString(retrievedPatient);
                LOG.trace(".addPatientResourceAndRetrieve(): Retrieved Patient [{}] --> {}", counter, retrievedPatientAsString);
                counter++;
            }
        }
        for (Patient patient : patientSet.getPateintSet()) {
            LOG.trace(".addPatientResourceAndRetrieve(): Deleting Patient --> {}", patient);
            VirtualDBMethodOutcome outcome = patientAccessor.deleteResource(patient);
        }
        for (Identifier patientIdentifier : patientIdSet) {
            LOG.trace(".addPatientResourceAndRetrieve(): Attempting to retrieve Patient --> {}", patientIdentifier);
            VirtualDBMethodOutcome outcome = patientAccessor.findResourceViaIdentifier(patientIdentifier);
            Patient retrievedPatient = (Patient)outcome.getResource();
            if(retrievedPatient != null){
                assertTrue(false);
            }
        }
    }
}
