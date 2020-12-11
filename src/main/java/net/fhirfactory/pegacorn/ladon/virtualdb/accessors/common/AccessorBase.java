/*
 * Copyright (c) 2020 Mark A. Hunter
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all
 * copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 */
package net.fhirfactory.pegacorn.ladon.virtualdb.accessors.common;

import javax.annotation.PostConstruct;
import javax.inject.Inject;

import net.fhirfactory.pegacorn.ladon.model.virtualdb.operations.VirtualDBActionStatusEnum;
import net.fhirfactory.pegacorn.ladon.model.virtualdb.operations.VirtualDBActionTypeEnum;
import net.fhirfactory.pegacorn.ladon.model.virtualdb.operations.VirtualDBMethodOutcome;
import net.fhirfactory.pegacorn.ladon.model.virtualdb.searches.SearchNameEnum;
import net.fhirfactory.pegacorn.ladon.virtualdb.audit.VirtualDBAuditEntryManager;
import net.fhirfactory.pegacorn.ladon.model.virtualdb.businesskey.VirtualDBKeyManagement;
import net.fhirfactory.pegacorn.ladon.virtualdb.engine.common.ResourceDBEngine;
import org.hl7.fhir.r4.model.*;
import org.slf4j.Logger;

import ca.uhn.fhir.parser.IParser;
import net.fhirfactory.pegacorn.common.model.FDN;
import net.fhirfactory.pegacorn.common.model.RDN;
import net.fhirfactory.pegacorn.datasets.fhir.r4.internal.topics.FHIRElementTopicIDBuilder;
import net.fhirfactory.pegacorn.deployment.topology.manager.DeploymentTopologyIM;
import net.fhirfactory.pegacorn.ladon.processingplant.LadonProcessingPlant;
import net.fhirfactory.pegacorn.petasos.audit.model.PetasosParcelAuditTrailEntry;
import net.fhirfactory.pegacorn.petasos.model.topology.NodeElement;
import net.fhirfactory.pegacorn.petasos.model.topology.NodeElementFunctionToken;
import net.fhirfactory.pegacorn.petasos.model.topology.NodeElementIdentifier;
import net.fhirfactory.pegacorn.petasos.model.topology.NodeElementTypeEnum;
import net.fhirfactory.pegacorn.petasos.model.wup.WUPIdentifier;
import net.fhirfactory.pegacorn.petasos.model.wup.WUPJobCard;
import net.fhirfactory.pegacorn.util.FHIRContextUtility;

import java.io.Serializable;
import java.util.List;
import java.util.Map;
import java.util.Set;

abstract public class AccessorBase {

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
        this.accessorName = specifyAccessorResourceTypeName();
        this.version = specifyAccessorResourceTypeVersion();
    }

    abstract protected String specifyAccessorResourceTypeName();

    abstract protected String specifyAccessorResourceTypeVersion();

    abstract protected Logger getLogger();

    abstract protected ResourceDBEngine getResourceDBEngine();

    abstract protected List<Identifier> resolveIdentifierList(Resource resource);

    protected String getResourceTypeName(){return(specifyAccessorResourceTypeName());}
    protected String getResourceTypeVersion(){return(specifyAccessorResourceTypeVersion());}


    @Inject
    private DeploymentTopologyIM topologyProxy;

    @Inject
    private FHIRElementTopicIDBuilder topicIDBuilder;

    @Inject
    private LadonProcessingPlant ladonPlant;

    @Inject
    private VirtualDBAuditEntryManager auditEntryManager;

    @Inject
    private VirtualDBKeyManagement virtualDBKeyManagement;
    
    @Inject
    private FHIRContextUtility FHIRContextUtility;

    @PostConstruct
    protected void initialise() {
        getLogger().debug(".initialise(): Entry");
        if (!isInitialised) {
            getLogger().trace(".initialise(): AccessBase is NOT initialised");
            this.parserR4 = FHIRContextUtility.getJsonParser();
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

    protected VirtualDBAuditEntryManager getAuditEntryManager(){
        return(auditEntryManager);
    }

    /**
     *
     * @param parameterSet
     * @param action
     * @return
     */
    protected PetasosParcelAuditTrailEntry beginTransaction(Map<Property, Serializable> parameterSet, VirtualDBActionTypeEnum action){
        String searchSummary = "Search Criteria(";
        Set<Property> propertySet = parameterSet.keySet();
        if(propertySet.isEmpty()){
            searchSummary = searchSummary + "empty)";
        } else {
            int remainingElements = propertySet.size();
            for(Property property: propertySet){
                searchSummary = searchSummary + property.getName() + "-->" + parameterSet.get(property).toString();
                if(remainingElements > 1){
                    searchSummary = searchSummary + ",";
                }
            }
            searchSummary = searchSummary + ")";
        }
        PetasosParcelAuditTrailEntry parcelEntry = auditEntryManager.beginTransaction(searchSummary, getResourceTypeName(), null, action, this.accessorIdentifier, this.version );
        return(parcelEntry);
    }

    /**
     *
     * @param resourceIdentifier
     * @param fhirResource
     * @param action
     * @return
     */
    protected PetasosParcelAuditTrailEntry beginTransaction(Identifier resourceIdentifier, Resource fhirResource, VirtualDBActionTypeEnum action){
        String resourceKey = virtualDBKeyManagement.generatePrintableInformationFromIdentifier(resourceIdentifier);
        PetasosParcelAuditTrailEntry parcelEntry = auditEntryManager.beginTransaction(resourceKey, getResourceTypeName(), fhirResource, action, this.accessorIdentifier, this.version );
        return(parcelEntry);
    }

    /**
     *
     * @param id
     * @param fhirResource
     * @param action
     * @return
     */
    protected PetasosParcelAuditTrailEntry beginTransaction(IdType id, Resource fhirResource, VirtualDBActionTypeEnum action){
        String resourceKey = id.asStringValue();
        PetasosParcelAuditTrailEntry parcelEntry = auditEntryManager.beginTransaction(resourceKey, getResourceTypeName(),  fhirResource, action, this.accessorIdentifier, this.version );
        return(parcelEntry);
    }

    /**
     *
     * @param resourceIdentifier
     * @param fhirResource
     * @param action
     * @param success
     * @param startingTransaction
     */
    protected void endTransaction(Identifier resourceIdentifier, Resource fhirResource, VirtualDBActionTypeEnum action, boolean success, PetasosParcelAuditTrailEntry startingTransaction){
        String resourceKey = resourceIdentifier.toString();
        auditEntryManager.endTransaction(resourceKey, getResourceTypeName(),fhirResource,action,success,startingTransaction,this.accessorIdentifier, this.version);
    }

    /**
     *
     * @param resultSet
     * @param returnedResourceCount
     * @param action
     * @param success
     * @param startingTransaction
     */
    protected void endSearchTransaction(Bundle resultSet, int returnedResourceCount, VirtualDBActionTypeEnum action, boolean success, PetasosParcelAuditTrailEntry startingTransaction){
        String searchAnswerCount = buildSearchResultString(resultSet);
        auditEntryManager.endTransaction(searchAnswerCount, getResourceTypeName(), null,action,success,startingTransaction,this.accessorIdentifier, this.version);
    }

    /**
     *
     * @param id
     * @param fhirResource
     * @param action
     * @param success
     * @param startingTransaction
     */
    protected void endTransaction(IdType id, Resource fhirResource, VirtualDBActionTypeEnum action, boolean success, PetasosParcelAuditTrailEntry startingTransaction){
        String resourceKey = id.asStringValue();
        auditEntryManager.endTransaction(resourceKey, getResourceTypeName(), fhirResource,action,success,startingTransaction,this.accessorIdentifier, this.version);
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
        getLogger().debug(".specifyNode(): Entry");
        NodeElementIdentifier ladonInstanceIdentifier = ladonPlant.getProcessingPlantNodeId();
        getLogger().info(".specifyNode(): retrieved Ladon-ProcessingPlant Identifier --> {}", ladonInstanceIdentifier);
        if (ladonInstanceIdentifier == null) {
            getLogger().error(".specifyNode(): Oh No!");
        }
        FDN virtualdbFDN = new FDN(ladonInstanceIdentifier);
        virtualdbFDN.appendRDN(new RDN(NodeElementTypeEnum.WORKSHOP.getNodeElementType(), "VirtualDB"));
        NodeElementIdentifier virtualdbId = new NodeElementIdentifier(virtualdbFDN.getToken());
        getLogger().trace(".specifyNode(): Retrieving VirtualDB Node");
        NodeElement virtualdb = topologyProxy.getNode(virtualdbId);
        getLogger().trace(".specifyNode(): virtualdb node (NodeElement) --> {}", virtualdb);
        FDN accessorInstanceFDN = new FDN(virtualdbFDN);
        accessorInstanceFDN.appendRDN(new RDN(NodeElementTypeEnum.WUP.getNodeElementType(), "Accessor-" + specifyAccessorResourceTypeName()));
        NodeElementIdentifier accessorInstanceIdentifier = new NodeElementIdentifier(accessorInstanceFDN.getToken());
        getLogger().trace(".specifyNode(): Now construct the Work Unit Processing Node");
        NodeElement accessor = new NodeElement();
        getLogger().trace(".specifyNode(): Constructing WUP Node, Setting Version Number");
        accessor.setVersion(this.version);
        getLogger().trace(".specifyNode(): Constructing WUP Node, Setting Node Instance");
        accessor.setNodeInstanceID(virtualdbId);
        getLogger().trace(".specifyNode(): Constructing WUP Node, Setting Concurrency Mode");
        accessor.setConcurrencyMode(virtualdb.getConcurrencyMode());
        getLogger().trace(".specifyNode(): Constructing WUP Node, Setting Resillience Mode");
        accessor.setResilienceMode(virtualdb.getResilienceMode());
        getLogger().trace(".specifyNode(): Constructing WUP Node, Setting inPlace Status");
        accessor.setInstanceInPlace(true);
        getLogger().trace(".specifyNode(): Constructing WUP Node, Setting Containing Element Identifier");
        accessor.setContainingElementID(virtualdb.getNodeInstanceID());
        getLogger().trace(".specifyNode(): Now registering the Node");
        topologyProxy.registerNode(accessor);
        getLogger().info(".specifyNode(): Exit, accessorInstanceIdentifier (NodeElementIdentifier) --> {}", accessorInstanceIdentifier);
        return (accessor);
    }

    public VirtualDBMethodOutcome getResource(IdType id) {
        getLogger().debug(".getResource(): Entry, id (IdType) --> {}", id);
        PetasosParcelAuditTrailEntry currentTransaction = this.beginTransaction(id, null, VirtualDBActionTypeEnum.REVIEW);
        VirtualDBMethodOutcome outcome = getResourceDBEngine().getResource(id);
        if(outcome.getStatusEnum() == VirtualDBActionStatusEnum.REVIEW_FINISH) {
            this.endTransaction(id, (Resource)outcome.getResource(), VirtualDBActionTypeEnum.REVIEW, true, currentTransaction);
        } else {
            this.endTransaction(id, null, VirtualDBActionTypeEnum.REVIEW, false, currentTransaction);
        }
        getLogger().debug(".getResource(): Exit, Resource retrieved, outcome --> {}", outcome);
        return (outcome);
    }

    public VirtualDBMethodOutcome getResourceNoAudit(IdType id) {
        getLogger().debug(".getResourceNoAudit(): Entry, id (Identifier) --> {}", id);
        VirtualDBMethodOutcome outcome = getResourceDBEngine().getResource(id);
        getLogger().debug(".getResourceNoAudit(): Exit, Resource retrieved, outcome --> {}", outcome);
        return (outcome);
    }

    public VirtualDBMethodOutcome createResource(Resource newResource){
        getLogger().debug(".createResource(): Entry, newResource (Resource) --> {}", newResource);
        Identifier bestIdentifier = virtualDBKeyManagement.getBestIdentifier(resolveIdentifierList(newResource));
        PetasosParcelAuditTrailEntry currentTransaction = this.beginTransaction(bestIdentifier, newResource, VirtualDBActionTypeEnum.CREATE);
        VirtualDBMethodOutcome outcome = getResourceDBEngine().createResource(newResource);
        if(outcome.getStatusEnum() == VirtualDBActionStatusEnum.CREATION_FINISH) {
            this.endTransaction(bestIdentifier, newResource, VirtualDBActionTypeEnum.CREATE, true, currentTransaction);
        } else {
            this.endTransaction(bestIdentifier, null, VirtualDBActionTypeEnum.CREATE, false, currentTransaction);
        }
        getLogger().debug(".createResource(): Exit, Resource Create, outcome --> {}", outcome);
        return(outcome);
    }

    public VirtualDBMethodOutcome deleteResource(Resource resourceToRemove){
        getLogger().debug(".deleteResource(): Entry, resourceToRemove --> {}", resourceToRemove);
        Identifier bestIdentifier = virtualDBKeyManagement.getBestIdentifier(resolveIdentifierList(resourceToRemove));
        PetasosParcelAuditTrailEntry currentTransaction = this.beginTransaction(bestIdentifier, resourceToRemove, VirtualDBActionTypeEnum.DELETE);
        VirtualDBMethodOutcome outcome  = getResourceDBEngine().deleteResource(resourceToRemove);
        if(outcome.getStatusEnum() != VirtualDBActionStatusEnum.DELETE_FINISH) {
            this.endTransaction(bestIdentifier, null, VirtualDBActionTypeEnum.DELETE, true, currentTransaction);
        } else {
            this.endTransaction(bestIdentifier, null, VirtualDBActionTypeEnum.DELETE, false, currentTransaction);
        }
        getLogger().debug(".deleteResource(): Exit, Resource Deleted, outcome --> {}", outcome);
        return(outcome);
    }

    public VirtualDBMethodOutcome updateResource(Resource resourceToUpdate){
        getLogger().debug(".updateResource(): Entry, resourceToUpdate --> {}", resourceToUpdate);
        Identifier bestIdentifier = virtualDBKeyManagement.getBestIdentifier(resolveIdentifierList(resourceToUpdate));
        PetasosParcelAuditTrailEntry currentTransaction = this.beginTransaction(bestIdentifier, resourceToUpdate, VirtualDBActionTypeEnum.UPDATE);
        VirtualDBMethodOutcome outcome  = getResourceDBEngine().deleteResource(resourceToUpdate);
        if(outcome.getStatusEnum() != VirtualDBActionStatusEnum.UPDATE_FINISH) {
            this.endTransaction(bestIdentifier, resourceToUpdate, VirtualDBActionTypeEnum.UPDATE, true, currentTransaction);
        } else {
            this.endTransaction(bestIdentifier, resourceToUpdate, VirtualDBActionTypeEnum.UPDATE, false, currentTransaction);
        }
        getLogger().debug(".updateResource(): Exit, Resource Updated, outcome --> {}", outcome);
        return(outcome);
    }

    /**
     *
     * @param resourceType
     * @param parameterSet
     * @return
     */
    public VirtualDBMethodOutcome searchUsingCriteria(ResourceType resourceType, SearchNameEnum searchName, Map<Property, Serializable> parameterSet) {
        getLogger().debug(".searchUsingCriteria(): Entry, Search Name --> {}, parameterSet --> {}", searchName, parameterSet);
        PetasosParcelAuditTrailEntry currentTransaction = this.beginTransaction(parameterSet, VirtualDBActionTypeEnum.SEARCH);
        VirtualDBMethodOutcome outcome = getResourceDBEngine().getResourcesViaSearchCriteria(resourceType, searchName, parameterSet);
        if(outcome.getStatusEnum() == VirtualDBActionStatusEnum.SEARCH_FAILURE) {
            endSearchTransaction(null, 0, VirtualDBActionTypeEnum.SEARCH, false, currentTransaction);
            return(outcome);
        }
        Resource searchResultResource = (Resource)outcome.getResource();
        if( searchResultResource == null){
            endSearchTransaction(null, 0, VirtualDBActionTypeEnum.SEARCH, false, currentTransaction);
            getLogger().debug(".searchUsingCriteria(): Exit, result set is null");
            return(outcome);
        }
        if(searchResultResource.getResourceType() == ResourceType.Bundle){
            Bundle searchResultBundle = (Bundle)searchResultResource;
            this.endSearchTransaction(searchResultBundle, searchResultBundle.getTotal(), VirtualDBActionTypeEnum.SEARCH, true, currentTransaction);
        } else {
            this.endSearchTransaction(null, 0, VirtualDBActionTypeEnum.SEARCH, false, currentTransaction);
        }
        getLogger().debug(".searchUsingCriteria(): Exit");
        return(outcome);
    }

    private String buildSearchResultString(Bundle searchResult){
        if(searchResult == null) {
            return("Search Failed");
        }
        int resultCount = searchResult.getTotal();
        if(resultCount == 0){
            return("Search Succeeded: Result Count = 0");
        }
        String resultString = "Search Succeeded: Result Count = " + resultCount + ": Entries --> ";
        for(Bundle.BundleEntryComponent currentBundleEntry: searchResult.getEntry()){
            Resource currentResource = currentBundleEntry.getResource();
            if(currentResource.hasId()){
                resultString = resultString + currentResource.getId();
            } else {
                resultString = resultString + "[Resource Has No Id]";
            }
            if(resultCount > 1) {
                resultString = resultString + ", ";
            }
            resultCount -= 1;
        }
        return(resultString);
    }

    public VirtualDBMethodOutcome findResourceViaIdentifier(Identifier identifier) {
        getLogger().debug(".findResourceViaIdentifier(): Entry, identifier (Identifier) --> {}", identifier);
        PetasosParcelAuditTrailEntry currentTransaction = this.beginTransaction(identifier, null, VirtualDBActionTypeEnum.REVIEW);
        VirtualDBMethodOutcome outcome = getResourceDBEngine().findResourceViaIdentifier(identifier);
        if(getLogger().isTraceEnabled()) {
            getLogger().trace(".findResourceViaIdentifier(): outcome.id --> {}", outcome.getId());
        }
        if(outcome.getStatusEnum().equals(VirtualDBActionStatusEnum.REVIEW_FINISH)) {
            Resource retrievedResource = (Resource)outcome.getResource();
            if(getLogger().isTraceEnabled()) {
                getLogger().trace(".findResourceViaIdentifier(): Review Finsihed, resource found!");
                getLogger().trace(".findResourceViaIdentifier(): retrievedResource.id (Resource) --> {}", retrievedResource.getId());
                getLogger().trace(".findResourceViaIdentifier(): retrievedResource.type --> {}", retrievedResource.getResourceType());
            }
            this.endTransaction(identifier, retrievedResource, VirtualDBActionTypeEnum.REVIEW, true, currentTransaction);
        } else {
            getLogger().debug(".findResourceViaIdentifier(): Review Finsihed, resource not found!");
            this.endTransaction(identifier, null, VirtualDBActionTypeEnum.REVIEW, false, currentTransaction);
        }
        getLogger().debug(".findResourceViaIdentifier(): Exit, Resource retrieved, outcome --> {}", outcome);
        return (outcome);
    }
    
    

}
