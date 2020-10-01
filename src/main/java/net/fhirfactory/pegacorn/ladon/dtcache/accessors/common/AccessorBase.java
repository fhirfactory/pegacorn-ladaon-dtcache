package net.fhirfactory.pegacorn.ladon.dtcache.accessors.common;

import javax.annotation.PostConstruct;
import javax.inject.Inject;

import org.hl7.fhir.r4.model.DomainResource;
import org.hl7.fhir.r4.model.IdType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import ca.uhn.fhir.parser.IParser;
import net.fhirfactory.pegacorn.common.model.FDN;
import net.fhirfactory.pegacorn.common.model.RDN;
import net.fhirfactory.pegacorn.datasets.fhir.r4.internal.topics.FHIRElementTopicIDBuilder;
import net.fhirfactory.pegacorn.deployment.topology.manager.DeploymentTopologyIM;
import net.fhirfactory.pegacorn.ladon.processingplant.LadonProcessingPlant;
import net.fhirfactory.pegacorn.petasos.audit.model.PetasosParcelAuditTrailEntry;
import net.fhirfactory.pegacorn.petasos.core.sta.brokers.PetasosSTAServicesAuditOnlyBroker;
import net.fhirfactory.pegacorn.petasos.model.topics.TopicToken;
import net.fhirfactory.pegacorn.petasos.model.topology.NodeElement;
import net.fhirfactory.pegacorn.petasos.model.topology.NodeElementFunctionToken;
import net.fhirfactory.pegacorn.petasos.model.topology.NodeElementIdentifier;
import net.fhirfactory.pegacorn.petasos.model.topology.NodeElementTypeEnum;
import net.fhirfactory.pegacorn.petasos.model.uow.UoW;
import net.fhirfactory.pegacorn.petasos.model.uow.UoWPayload;
import net.fhirfactory.pegacorn.petasos.model.uow.UoWProcessingOutcomeEnum;
import net.fhirfactory.pegacorn.petasos.model.wup.WUPIdentifier;
import net.fhirfactory.pegacorn.petasos.model.wup.WUPJobCard;
import net.fhirfactory.pegacorn.util.FhirUtil;

abstract public class AccessorBase {

    private static final Logger LOG = LoggerFactory.getLogger(AccessorBase.class);

    private static final String FHIR_VERSION = "4.0.1";

    private NodeElementFunctionToken accessorFunctionToken;
    private WUPIdentifier accessorIdentifier;
    private String accessorName;
    private WUPJobCard accessorJobCard;
    private NodeElement node;
    private String version;
    private boolean isInitialised;

    private IParser parserR4;

    public AccessorBase() {
        isInitialised = false;
        this.accessorName = specifyAccessorName();
        this.version = specifyVersion();
    }

    abstract protected String specifyAccessorName();

    abstract protected String specifyVersion();

    @Inject
    private PetasosSTAServicesAuditOnlyBroker servicesBroker;

    @Inject
    private DeploymentTopologyIM topologyProxy;

    @Inject
    private FHIRElementTopicIDBuilder topicIDBuilder;

    @Inject
    private LadonProcessingPlant ladonPlant;

    @PostConstruct
    protected void initialise() {
        LOG.debug(".initialise(): Entry");
        if (!isInitialised) {
            LOG.trace(".initialise(): AccessBase is NOT initialised");
            this.parserR4 = FhirUtil.getInstance().getJsonParser();
            this.isInitialised = true;
            ladonPlant.initialisePlant();
            this.node = specifyNode();
            this.accessorFunctionToken = this.node.getNodeFunctionToken();
            this.accessorIdentifier = new WUPIdentifier(this.node.getNodeInstanceID());
        }
    }

    public void initialiseServices() {
        initialise();
    }

    protected PetasosParcelAuditTrailEntry beginTransaction(IdType resourceId, DomainResource fhirResource, AccessorActionTypeEnum action) {
        LOG.debug(".beginTransaction(): Entry, fhriResource --> {}, action --> {}", fhirResource, action);
        LOG.trace(".beginTransaction(): Create the UoW for accessor utilisation");
        UoWPayload payload = new UoWPayload();
        switch (action) {
            case GET: {
                LOG.trace(".beginTransaction(): It is a GET request, so no actual resource --> log the request only");
                payload.setPayload(resourceId.asStringValue());
                TopicToken payloadToken = topicIDBuilder.createTopicToken("IdType", FHIR_VERSION);
                payload.setPayloadTopicID(payloadToken);
                break;
            }
            case ADD:
            case DELETE:
            case UPDATE: {
                LOG.trace(".beginTransaction(): Converting FHIR element into a (JSON) String");
                String resourceAsString = null;
                try {
                    LOG.trace(".beginTransaction(): Using IParser --> {}", parserR4);
                    resourceAsString = parserR4.encodeResourceToString(fhirResource);
                } catch (Exception Ex) {
                    LOG.trace(".beginTransaction(): Failed to Encode --> {}", Ex.toString());
                }
                LOG.trace(".beginTransaction(): Add JSON String (encoded FHIR element) to the UoWPayload");
                payload.setPayload(resourceAsString);
                LOG.trace(".beginTransaction(): Construct a TopicToken to describe the payload & add it to the Payload");
                String resourceType = fhirResource.getResourceType().toString();
                TopicToken payloadToken = topicIDBuilder.createTopicToken(resourceType, FHIR_VERSION);
                payload.setPayloadTopicID(payloadToken);
            }
        }

        LOG.trace(".beginTransaction(): Instantiate the UoW with the fhirResource/TopicToken as the Ingres Payload");
        UoW theUoW = new UoW(payload);
        theUoW.setProcessingOutcome(UoWProcessingOutcomeEnum.UOW_OUTCOME_NOTSTARTED);
        LOG.trace(".beginTransaction(): Construct an ActivityID/AcitityID for the activity (even though we don't need it really) ");
        PetasosParcelAuditTrailEntry currentTransaction = servicesBroker.transactionAuditEntry(this.accessorIdentifier, action.toString(), theUoW, null);
        LOG.debug(".beginTransaction(): Exit --> Registration aftermath: currentTransaction (PetasosParcelAuditTrailEntry) --> {}", currentTransaction);
        return (currentTransaction);
    }

    protected void endTransaction(IdType resourceId, DomainResource fhirResource, AccessorActionTypeEnum action, boolean success, PetasosParcelAuditTrailEntry startingTransaction) {
        LOG.debug(".endTransaction(): Entry");
        UoW updatedUoW = startingTransaction.getActualUoW();
        String resourceAsString = null;
        if (success) {
            switch (action) {
                case GET:
                case UPDATE:
                case ADD: {
                    LOG.trace(".endTransaction(): It is a GET, UPDATE or ADD request, so log the outcome");
                    resourceAsString = parserR4.encodeResourceToString(fhirResource);
                    UoWPayload newPayload = new UoWPayload();
                    newPayload.setPayload(resourceAsString);
                    TopicToken payloadToken = topicIDBuilder.createTopicToken(fhirResource.getResourceType().toString(), version);
                    newPayload.setPayloadTopicID(payloadToken);
                    updatedUoW.getEgressContent().addPayloadElement(newPayload);
                    break;
                }
                case DELETE: {
                    LOG.trace(".endTransaction(): It is a DELETE request, so there is nothing to LOG (the Resource has been Deleted)!");
                }
            }
            updatedUoW.setProcessingOutcome(UoWProcessingOutcomeEnum.UOW_OUTCOME_SUCCESS);
            LOG.trace(".endTransaction(): Calling the Audit Trail Generator ");
            PetasosParcelAuditTrailEntry currentTransaction = servicesBroker.transactionAuditEntry(this.accessorIdentifier, action.toString(), updatedUoW, startingTransaction);
        } else {
            updatedUoW.setProcessingOutcome(UoWProcessingOutcomeEnum.UOW_OUTCOME_FAILED);
            LOG.trace(".endTransaction(): Calling the Audit Trail Generator ");
            PetasosParcelAuditTrailEntry currentTransaction = servicesBroker.transactionAuditEntry(this.accessorIdentifier, action.toString(), updatedUoW, startingTransaction);
        }
        LOG.debug(".endTransaction(): exit, my work is done!");
    }

    /**
     * This function builds the Deployment Topology node (a WUP) for the
     * Accessor.
     * <p>
     * It uses the Name (specifyAccessorName()) defined in the subclass as part
     * of the Identifier and then registers with the Topology Services.
     *
     * @return The NodeElement representing the WUP which this code-set is
     * fulfilling.
     */
    private NodeElement specifyNode() {
        LOG.debug(".specifyNode(): Entry");
        NodeElementIdentifier ladonInstanceIdentifier = ladonPlant.getProcessingPlantNodeId();
        LOG.trace(".specifyNode(): retrieved Ladon-ProcessingPlant Identifier --> {}", ladonInstanceIdentifier);
        if (ladonInstanceIdentifier == null) {
            LOG.debug(".specifyNode(): Oh No!");
        }
        FDN dtcacheFDN = new FDN(ladonInstanceIdentifier);
        dtcacheFDN.appendRDN(new RDN(NodeElementTypeEnum.WORKSHOP.getNodeElementType(), "DTCache"));
        NodeElementIdentifier dtcacheId = new NodeElementIdentifier(dtcacheFDN.getToken());
        LOG.trace(".specifyNode(): Retrieving DTCache Node");
        NodeElement dtcache = topologyProxy.getNode(dtcacheId);
        LOG.trace(".specifyNode(): dtcache node (NodeElement) --> {}", dtcache);
        FDN accessorInstanceFDN = new FDN(dtcacheFDN);
        accessorInstanceFDN.appendRDN(new RDN(NodeElementTypeEnum.WUP.getNodeElementType(), "Accessor-" + specifyAccessorName()));
        NodeElementIdentifier accessorInstanceIdentifier = new NodeElementIdentifier(accessorInstanceFDN.getToken());
        LOG.trace(".specifyNode(): Now construct the Work Unit Processing Node");
        NodeElement accessor = new NodeElement();
        LOG.trace(".specifyNode(): Constructing WUP Node, Setting Version Number");
        accessor.setVersion(this.version);
        LOG.trace(".specifyNode(): Constructing WUP Node, Setting Node Instance");
        accessor.setNodeInstanceID(dtcacheId);
        LOG.trace(".specifyNode(): Constructing WUP Node, Setting Concurrency Mode");
        accessor.setConcurrencyMode(dtcache.getConcurrencyMode());
        LOG.trace(".specifyNode(): Constructing WUP Node, Setting Resillience Mode");
        accessor.setResilienceMode(dtcache.getResilienceMode());
        LOG.trace(".specifyNode(): Constructing WUP Node, Setting inPlace Status");
        accessor.setInstanceInPlace(true);
        LOG.trace(".specifyNode(): Constructing WUP Node, Setting Containing Element Identifier");
        accessor.setContainingElementID(dtcache.getNodeInstanceID());
        LOG.trace(".specifyNode(): Now registering the Node");
        topologyProxy.registerNode(accessor);
        LOG.debug(".specifyNode(): Exit, accessorInstanceIdentifier (NodeElementIdentifier) --> {}", accessorInstanceIdentifier);
        return (accessor);
    }
}
