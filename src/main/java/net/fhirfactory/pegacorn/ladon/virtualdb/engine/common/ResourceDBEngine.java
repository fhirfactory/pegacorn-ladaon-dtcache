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
package net.fhirfactory.pegacorn.ladon.virtualdb.engine.common;

import net.fhirfactory.pegacorn.ladon.mdr.conduit.common.ResourceSoTConduitController;
import net.fhirfactory.pegacorn.ladon.model.virtualdb.ResourceDBEngineInterface;
import net.fhirfactory.pegacorn.ladon.model.virtualdb.operations.VirtualDBActionStatusEnum;
import net.fhirfactory.pegacorn.ladon.model.virtualdb.operations.VirtualDBActionTypeEnum;
import net.fhirfactory.pegacorn.ladon.model.virtualdb.operations.VirtualDBMethodOutcome;
import net.fhirfactory.pegacorn.ladon.virtualdb.cache.common.VirtualDBIdTypeBasedCacheBase;
import net.fhirfactory.pegacorn.ladon.virtualdb.persistence.common.PersistenceServiceBase;
import org.hl7.fhir.r4.model.*;
import org.slf4j.Logger;

import java.io.Serializable;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public abstract class ResourceDBEngine implements ResourceDBEngineInterface {

    abstract protected VirtualDBIdTypeBasedCacheBase specifyDBCache();

    abstract protected ResourceSoTConduitController specifySourceOfTruthAggregator();
    
    abstract protected PersistenceServiceBase specifyPersistenceService();

    abstract protected Logger getLogger();

    abstract protected ResourceType specifyResourceType();

    abstract protected List<Identifier> resolveIdentifierSet(Resource resource);

    
    protected ResourceSoTConduitController getSourceOfTruthAggregator(){
        return(specifySourceOfTruthAggregator());
    }

    protected VirtualDBIdTypeBasedCacheBase getDBCache(){
        return(specifyDBCache());
    }

    protected PersistenceServiceBase getPersistenceService(){
        return(specifyPersistenceService());
    }

    protected ResourceType getResourceType(){return(specifyResourceType());}

    @Override
    public VirtualDBMethodOutcome createResource(Resource resourceToCreate) {
        getLogger().debug(".createResource(): Entry, resourceToCreate --> {}", resourceToCreate);
        if(!resourceToCreate.hasId()){
            getLogger().trace(".createResource(): Resource did not have an Id, so creating one!");
            IdType newId = new IdType(resourceToCreate.getResourceType().getPath(), UUID.randomUUID().toString());
            resourceToCreate.setId(newId);
            getLogger().trace(".createResource(): Resource Id created and added to Resource, Id --> {}", newId);
        }
        VirtualDBMethodOutcome outcome = getSourceOfTruthAggregator().createResource(resourceToCreate);
        if (outcome.getStatusEnum().equals(VirtualDBActionStatusEnum.CREATION_FINISH)) {
            getLogger().trace(".createResource(): Resource successfully created in the MDR (Set), now adding it to the Cache & VirtualDB");
            VirtualDBMethodOutcome virtualDBOutcome = getPersistenceService().standardCreateResource(resourceToCreate);
            VirtualDBMethodOutcome cacheCreateOutcome = getDBCache().createResource(resourceToCreate);
        }
        getLogger().debug(".createResource(): Resource created, exiting");
        return (outcome);
    }

    @Override
    public VirtualDBMethodOutcome getResource(Identifier identifier) {
        VirtualDBMethodOutcome outcome = getDBCache().getResource(identifier);
        if (outcome.getStatusEnum() == VirtualDBActionStatusEnum.REVIEW_FAILURE) {
            outcome = getSourceOfTruthAggregator().reviewResource(identifier);
        }
        return (outcome);
    }

    @Override
    public VirtualDBMethodOutcome getResource(IdType id){
        VirtualDBMethodOutcome outcome = getDBCache().getResource(id);
        if(outcome.getStatusEnum() == VirtualDBActionStatusEnum.REVIEW_FAILURE){
            VirtualDBMethodOutcome persistenceServiceOutcome = getPersistenceService().getResourceById(getResourceType().getPath(), id);
            if(persistenceServiceOutcome.getStatusEnum() == VirtualDBActionStatusEnum.REVIEW_FINISH){
                Resource persistenceServiceOriginatedResource = (Resource)persistenceServiceOutcome.getResource();
                List<Identifier> identifierList = resolveIdentifierSet(persistenceServiceOriginatedResource);
                if(identifierList.isEmpty()) {
                    outcome = generateEmptyGetResponse(id);
                } else {
                    outcome = getSourceOfTruthAggregator().reviewResource(identifierList);
                }
                return (outcome);
            } else {
                outcome = generateEmptyGetResponse(id);
                return(outcome);
            }
        } else {
            return(outcome);
        }
    }

    @Override
    public VirtualDBMethodOutcome updateResource(Resource resourceToUpdate) {
        VirtualDBMethodOutcome outcome = getSourceOfTruthAggregator().updateResource(resourceToUpdate);
        if (outcome.getStatusEnum() == VirtualDBActionStatusEnum.UPDATE_FINISH) {
            VirtualDBMethodOutcome cacheCreateOutcome = getDBCache().updateResource(resourceToUpdate);
        }
        return (outcome);
    }

    @Override
    public VirtualDBMethodOutcome deleteResource(Resource resourceToDelete) {
        VirtualDBMethodOutcome outcome = getSourceOfTruthAggregator().deleteResource(resourceToDelete);
        VirtualDBMethodOutcome cacheCreateOutcome = getDBCache().deleteResource(resourceToDelete);
        return (outcome);
    }

    @Override
    public VirtualDBMethodOutcome getResourcesViaSearchCriteria(ResourceType resourceType, Property attributeName, Element attributeValue) {
        VirtualDBMethodOutcome outcome = getSourceOfTruthAggregator().getResourcesViaSearchCriteria(resourceType, attributeName, attributeValue);
        updateCache(outcome);
        return(outcome);
    }

    @Override
    public VirtualDBMethodOutcome getResourcesViaSearchCriteria(ResourceType resourceType, Map<Property, Serializable> parameterSet) {
        VirtualDBMethodOutcome outcome = getSourceOfTruthAggregator().getResourcesViaSearchCriteria(resourceType, parameterSet);
        updateCache(outcome);
        return(outcome);
    }

    private void updateCache(VirtualDBMethodOutcome outcome){
        if(outcome.getStatusEnum() != VirtualDBActionStatusEnum.SEARCH_FINISHED) {
            return;
        }
        Bundle outcomeBundle = (Bundle)outcome.getResource();
        if(outcomeBundle == null){
            return;
        }
        if(outcomeBundle.getTotal() < 1){
            return;
        }
        for(Bundle.BundleEntryComponent entry: outcomeBundle.getEntry()){
            VirtualDBMethodOutcome cacheCreateOutcome = getDBCache().syncResource(entry.getResource());
        }
    }

    private VirtualDBMethodOutcome generateEmptyGetResponse(IdType id){
        VirtualDBMethodOutcome outcome = new VirtualDBMethodOutcome();
        outcome.setCreated(false);
        outcome.setCausalAction(VirtualDBActionTypeEnum.REVIEW);
        outcome.setStatusEnum(VirtualDBActionStatusEnum.REVIEW_FAILURE);
        CodeableConcept details = new CodeableConcept();
        Coding detailsCoding = new Coding();
        detailsCoding.setSystem("https://www.hl7.org/fhir/codesystem-operation-outcome.html");
        detailsCoding.setCode("MSG_NO_EXIST");
        String text = "Resource Id " + id.toString() + " does not exist";
        detailsCoding.setDisplay(text);
        details.setText(text);
        details.addCoding(detailsCoding);
        OperationOutcome opOutcome = new OperationOutcome();
        OperationOutcome.OperationOutcomeIssueComponent newOutcomeComponent = new OperationOutcome.OperationOutcomeIssueComponent();
        newOutcomeComponent.setDiagnostics(getResourceType().getPath());
        newOutcomeComponent.setDetails(details);
        newOutcomeComponent.setCode(OperationOutcome.IssueType.NOTFOUND);
        newOutcomeComponent.setSeverity(OperationOutcome.IssueSeverity.WARNING);
        opOutcome.addIssue(newOutcomeComponent);
        outcome.setOperationOutcome(opOutcome);
        return(outcome);
    }
}
