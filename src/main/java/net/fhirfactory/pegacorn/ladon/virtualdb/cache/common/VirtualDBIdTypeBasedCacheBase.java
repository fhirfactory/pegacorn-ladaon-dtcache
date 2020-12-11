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
package net.fhirfactory.pegacorn.ladon.virtualdb.cache.common;

import net.fhirfactory.pegacorn.ladon.model.virtualdb.cache.CacheResourceEntry;
import net.fhirfactory.pegacorn.ladon.model.virtualdb.operations.VirtualDBActionStatusEnum;
import net.fhirfactory.pegacorn.ladon.model.virtualdb.operations.VirtualDBActionTypeEnum;
import net.fhirfactory.pegacorn.ladon.model.virtualdb.operations.VirtualDBMethodOutcome;
import net.fhirfactory.pegacorn.ladon.model.virtualdb.businesskey.VirtualDBKeyManagement;
import net.fhirfactory.pegacorn.ladon.model.virtualdb.operations.VirtualDBMethodOutcomeFactory;
import org.hl7.fhir.r4.model.*;
import org.slf4j.Logger;

import javax.annotation.PostConstruct;
import javax.inject.Inject;
import java.time.Instant;
import java.util.Enumeration;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

public abstract class VirtualDBIdTypeBasedCacheBase {

    @Inject
    private VirtualDBKeyManagement virtualDBKeyManagement;

    @Inject
    private VirtualDBMethodOutcomeFactory outcomeFactory;

    private ConcurrentHashMap<IdType, CacheResourceEntry> resourceCacheById;
    private ConcurrentHashMap<IdType, Object> resourceCacheLockSet;
    boolean isInitialised;

    protected VirtualDBIdTypeBasedCacheBase() {
        resourceCacheById = new ConcurrentHashMap<>();
        resourceCacheLockSet = new ConcurrentHashMap<>();
        this.isInitialised = false;
    }

    protected abstract Logger getLogger();
    protected abstract int specifyCacheElementRetirementInSeconds();
    protected abstract boolean areTheSame(Resource a, Resource b);
    protected abstract List<Identifier> resolveIdentifierSet(Resource resourceToAdd);
    protected abstract void addIdentifierToResource(Identifier identifierToAdd, Resource resource);
    protected abstract String getCacheClassName();
    protected abstract Resource createClonedResource(Resource resource);

    @PostConstruct
    protected void initialise() {
        if (!this.isInitialised) {
            getLogger().debug(".initialise(): Initialising the FHIR Parser framework");
            this.isInitialised = true;
        }
    }

    /**
     * The function adds a Resource to the Resource Cache. It wraps the Resource in a CacheResourceEntry,
     * which enables the cache management functions to ascertain the age of the cache entry for clean-up
     * purposes.
     *
     * @param resourceToAdd A FHIR::Resource that is to be added to the Cache.
     * @return A VirtualDBMethodOutcome instance detailing the success (or otherwise) of the Resource
     * addition to the Cache.
     */
    private VirtualDBMethodOutcome addResourceToCache(Resource resourceToAdd){
        // Perform house-keeping on the Cache
        purgeResourcesFromCache();
        // House-keeping done
        String activityLocation = getCacheClassName() + "::addResourceToCache()";
        if(resourceToAdd == null) {
            getLogger().error(".addResourceToCache(): resourceToAdd (Resource) is null, failing out");
            VirtualDBMethodOutcome vdbOutcome = outcomeFactory.generateBadAttributeOutcome(activityLocation, VirtualDBActionTypeEnum.CREATE, VirtualDBActionStatusEnum.CREATION_FAILURE, "Parameter resourceToAdd (Resource) content is invalid");
            return (vdbOutcome);
        }
        IdType resourceId = resourceToAdd.getIdElement();
        if(!resourceToAdd.hasId()){
            String newID = resourceToAdd.getResourceType().toString() + ":" + UUID.randomUUID().toString();
            resourceToAdd.setId(newID);
            resourceId = new IdType(newID);
        }
        if(resourceCacheById.containsKey(resourceId)){
            CacheResourceEntry resourceEntry = resourceCacheById.get(resourceId);
            if(resourceEntry != null){
                Resource existingResource = resourceEntry.getResource();
                if(areTheSame(existingResource, resourceToAdd)){
                    VirtualDBMethodOutcome vdbOutcome = outcomeFactory.createResourceActivityOutcome(resourceId, VirtualDBActionStatusEnum.CREATION_NOT_REQUIRED, activityLocation);
                    return(vdbOutcome);
                }
            }
            resourceCacheById.remove(resourceId);
            resourceCacheLockSet.remove(resourceId);
        }
        resourceCacheLockSet.put(resourceId, new Object());
        CacheResourceEntry newEntry = new CacheResourceEntry(resourceToAdd);
        resourceCacheById.put(resourceId, newEntry);
        VirtualDBMethodOutcome vdbOutcome = outcomeFactory.createResourceActivityOutcome(resourceId, VirtualDBActionStatusEnum.CREATION_FINISH, activityLocation);
        vdbOutcome.setResource(resourceToAdd);
        return(vdbOutcome);
    }

    /**
     * This function removes a Resource from the Resource Cache using the provided id.
     *
     * @param id
     * @return A VirtualDBMethodOutcome instance detailing the success (or otherwise) of the Resource removal activity.
     */
    private VirtualDBMethodOutcome deleteResourceFromCache(IdType id){
        String activityLocation = getCacheClassName() + "::deleteResourceFromCache()";
        if(id == null){
            getLogger().debug(".deleteResourceFromCache(): id (IdType) is null, failing out");
            VirtualDBMethodOutcome vdbOutcome = outcomeFactory.generateBadAttributeOutcome(activityLocation, VirtualDBActionTypeEnum.DELETE, VirtualDBActionStatusEnum.DELETE_FAILURE, "Parameter identifier (Identifier) content is invalid");
            return(vdbOutcome);
        }
        if(resourceCacheById.containsKey(id)) {
            resourceCacheById.remove(id);
            resourceCacheLockSet.remove(id);
            VirtualDBMethodOutcome vdbOutcome = outcomeFactory.createResourceActivityOutcome(id, VirtualDBActionStatusEnum.DELETE_FINISH, activityLocation);
            return (vdbOutcome);
        } else {
            VirtualDBMethodOutcome vdbOutcome = outcomeFactory.createResourceActivityOutcome(id, VirtualDBActionStatusEnum.DELETE_FAILURE, activityLocation);
            return (vdbOutcome);
        }
    }

    /**
     *
     * @param identifier
     * @return
     */
    private VirtualDBMethodOutcome getResourceFromCache(Identifier identifier){
        getLogger().debug(".getResourceFromCache(): Entry, identifier (Identifier) --> {}", identifier);
        String activityLocation = getCacheClassName() + "::getResourceFromCache()";
        if(identifier == null){
            getLogger().error(".getResourceFromCache(): identifier (Identifier) is null, failing out");
            VirtualDBMethodOutcome vdbOutcome = outcomeFactory.generateBadAttributeOutcome(activityLocation, VirtualDBActionTypeEnum.REVIEW, VirtualDBActionStatusEnum.REVIEW_FAILURE, "Parameter identifier (Identifier) content is invalid");
            return(vdbOutcome);
        }
        CacheResourceEntry foundResourceEntry = null;
        for(CacheResourceEntry currentResourceEntry: resourceCacheById.values()){
            boolean found = false;
            List<Identifier> identifiers = resolveIdentifierSet(currentResourceEntry.getResource());
            for(Identifier currentIdentifier: identifiers){
                boolean systemIsSame = identifier.getSystem().equals(currentIdentifier.getSystem());
                boolean valueIsSame = identifier.getValue().equals(currentIdentifier.getValue());
                if(systemIsSame && valueIsSame){
                    found = true;
                    foundResourceEntry = currentResourceEntry;
                    break;
                }
            }
            if(found){
                break;
            }
        }
        if(foundResourceEntry == null) {
            VirtualDBMethodOutcome vdbOutcome = new VirtualDBMethodOutcome();
            vdbOutcome.setCreated(false);
            vdbOutcome.setIdentifier(identifier);
            vdbOutcome.setCausalAction(VirtualDBActionTypeEnum.REVIEW);
            vdbOutcome.setStatusEnum(VirtualDBActionStatusEnum.REVIEW_RESOURCE_NOT_IN_CACHE);
            OperationOutcome opOutcome = new OperationOutcome();
            OperationOutcome.OperationOutcomeIssueComponent newOutcomeComponent = new OperationOutcome.OperationOutcomeIssueComponent();
            newOutcomeComponent.setCode(OperationOutcome.IssueType.NOTFOUND);
            newOutcomeComponent.setSeverity(OperationOutcome.IssueSeverity.WARNING);
            CodeableConcept details = new CodeableConcept();
            Coding detailsCoding = new Coding();
            detailsCoding.setSystem("https://www.hl7.org/fhir/codesystem-operation-outcome.html");
            detailsCoding.setCode("MSG_NO_EXIST");
            detailsCoding.setDisplay("Resource Id ("+ identifier +") does not exist");
            details.setText("Resource Id ("+ identifier +") does not exist");
            details.addCoding(detailsCoding);
            newOutcomeComponent.setDiagnostics(getCacheClassName() + "::getResourceFromCache()");
            newOutcomeComponent.setDetails(details);
            opOutcome.addIssue(newOutcomeComponent);
            vdbOutcome.setOperationOutcome(opOutcome);
            getLogger().debug(".getResourceFromCache(): exit, could not find resource");
            return (vdbOutcome);
        } else {
            VirtualDBMethodOutcome vdbOutcome = new VirtualDBMethodOutcome();
            vdbOutcome.setCreated(false);
            vdbOutcome.setIdentifier(identifier);
            vdbOutcome.setResource(foundResourceEntry.getResource());
            vdbOutcome.setCausalAction(VirtualDBActionTypeEnum.REVIEW);
            vdbOutcome.setStatusEnum(VirtualDBActionStatusEnum.REVIEW_FINISH);
            OperationOutcome opOutcome = new OperationOutcome();
            OperationOutcome.OperationOutcomeIssueComponent newOutcomeComponent = new OperationOutcome.OperationOutcomeIssueComponent();
            newOutcomeComponent.setCode(OperationOutcome.IssueType.INFORMATIONAL);
            newOutcomeComponent.setSeverity(OperationOutcome.IssueSeverity.INFORMATION);
            CodeableConcept details = new CodeableConcept();
            Coding detailsCoding = new Coding();
            detailsCoding.setSystem("https://www.hl7.org/fhir/codesystem-operation-outcome.html");
            detailsCoding.setCode("MSG_RESOURCE_RETRIEVED"); // TODO Pegacorn specific encoding --> need to check validity
            detailsCoding.setDisplay("Resource Id ("+ identifier +") has been retrieved");
            details.setText("Resource Id ("+ identifier +") has been retrieved");
            details.addCoding(detailsCoding);
            newOutcomeComponent.setDiagnostics(getCacheClassName() + "::getResourceFromCache()");
            newOutcomeComponent.setDetails(details);
            opOutcome.addIssue(newOutcomeComponent);
            vdbOutcome.setOperationOutcome(opOutcome);
            getLogger().debug(".getResourceFromCache(): exit, resource found... retrieved resource --> {}", foundResourceEntry.getResource());
            return (vdbOutcome);
        }
    }

    /**
     *
     * @param id
     * @return
     */
    private VirtualDBMethodOutcome getResourceFromCache(IdType id) {
        getLogger().debug(".getResourceFromCache(): Entry, id (IdType) --> {}", id);
        String activityLocation = getCacheClassName() + "::getResourceFromCache()";
        if (id == null) {
            getLogger().error(".getResourceFromCache(): identifier (Identifier) is null, failing out");
            VirtualDBMethodOutcome vdbOutcome = outcomeFactory.generateBadAttributeOutcome(activityLocation, VirtualDBActionTypeEnum.REVIEW, VirtualDBActionStatusEnum.REVIEW_FAILURE, "Parameter identifier (Identifier) content is invalid");
            return (vdbOutcome);
        }
        if(resourceCacheById.containsKey(id)){
            VirtualDBMethodOutcome vdbOutcome = new VirtualDBMethodOutcome();
            vdbOutcome.setCreated(false);
            vdbOutcome.setId(id);
            vdbOutcome.setCausalAction(VirtualDBActionTypeEnum.REVIEW);
            vdbOutcome.setStatusEnum(VirtualDBActionStatusEnum.REVIEW_FAILURE);
            OperationOutcome opOutcome = new OperationOutcome();
            OperationOutcome.OperationOutcomeIssueComponent newOutcomeComponent = new OperationOutcome.OperationOutcomeIssueComponent();
            newOutcomeComponent.setCode(OperationOutcome.IssueType.NOTFOUND);
            newOutcomeComponent.setSeverity(OperationOutcome.IssueSeverity.WARNING);
            CodeableConcept details = new CodeableConcept();
            Coding detailsCoding = new Coding();
            detailsCoding.setSystem("https://www.hl7.org/fhir/codesystem-operation-outcome.html");
            detailsCoding.setCode("MSG_NO_EXIST");
            detailsCoding.setDisplay("Resource Id ("+ id +") does not exist");
            details.setText("Resource Id ("+ id +") does not exist");
            details.addCoding(detailsCoding);
            newOutcomeComponent.setDiagnostics(activityLocation);
            newOutcomeComponent.setDetails(details);
            opOutcome.addIssue(newOutcomeComponent);
            vdbOutcome.setOperationOutcome(opOutcome);
            getLogger().debug(".getResourceFromCache(): exit, could not find resource");
            return (vdbOutcome);
        } else {
            VirtualDBMethodOutcome vdbOutcome = new VirtualDBMethodOutcome();
            CacheResourceEntry resourceEntry = resourceCacheById.get(id);
            vdbOutcome.setCreated(false);
            vdbOutcome.setId(id);
            vdbOutcome.setResource(resourceEntry.getResource());
            vdbOutcome.setCausalAction(VirtualDBActionTypeEnum.REVIEW);
            vdbOutcome.setStatusEnum(VirtualDBActionStatusEnum.REVIEW_FINISH);
            OperationOutcome opOutcome = new OperationOutcome();
            OperationOutcome.OperationOutcomeIssueComponent newOutcomeComponent = new OperationOutcome.OperationOutcomeIssueComponent();
            newOutcomeComponent.setCode(OperationOutcome.IssueType.INFORMATIONAL);
            newOutcomeComponent.setSeverity(OperationOutcome.IssueSeverity.INFORMATION);
            CodeableConcept details = new CodeableConcept();
            Coding detailsCoding = new Coding();
            detailsCoding.setSystem("https://www.hl7.org/fhir/codesystem-operation-outcome.html");
            detailsCoding.setCode("MSG_RESOURCE_RETRIEVED"); // TODO Pegacorn specific encoding --> need to check validity
            detailsCoding.setDisplay("Resource Id ("+ id +") has been retrieved");
            details.setText("Resource Id ("+ id +") has been retrieved");
            details.addCoding(detailsCoding);
            newOutcomeComponent.setDiagnostics(getCacheClassName() + "::getResourceFromCache()");
            newOutcomeComponent.setDetails(details);
            opOutcome.addIssue(newOutcomeComponent);
            vdbOutcome.setOperationOutcome(opOutcome);
            getLogger().debug(".getResourceFromCache(): exit, resource found... retrieved resource --> {}", resourceEntry.getResource());
            return (vdbOutcome);
        }
    }


    /**
     * This is a helper method, and is not intended for use outside of finding Resources
     * @return A collection of ALL the Resources within the Cache
     */
    public Collection<Resource> getAllResourcesFromCache(){
        getLogger().debug(".getAllResourcesFromCache(): Entry");
        ArrayList<Resource> resourceSet = new ArrayList<>();
        for(CacheResourceEntry resourceEntry: resourceCacheById.values() ){
            resourceSet.add(resourceEntry.getResource());
        }
        getLogger().debug(".getAllResourcesFromCache(): Exit");
        return(resourceSet);
    }

    /**
     * The method is called after every add/remove to clear Resources from the cache that have expired. It's not an
     * ideal solution but will keep the cache to a manageable size during the first few releases.
     *
     * TODO Need to improve the efficiency and mechanism used to clear content from the cache.
     */
    private void purgeResourcesFromCache(){
        getLogger().debug(".purgeResourcesFromCache(): Entry");
        Enumeration<IdType> idEnumeration = resourceCacheById.keys();
        while(idEnumeration.hasMoreElements()){
            IdType id = idEnumeration.nextElement();
            CacheResourceEntry resourceEntry = resourceCacheById.get(id);
            if(!isStillValidCacheResource(resourceEntry)){
                getLogger().trace(".purgeResourcesFromCache(): deleting resource --> {}", id);
                deleteResourceFromCache(id);
            }
        }
        getLogger().debug(".purgeResourcesFromCache(): Exit");
    }

    /**
     * This method does a simple comparison between the age of the resource entry in the cache and the
     * age-threshold and returns true if the resource is still "young enough".
     *
     * @param testEntry The Cache Entry to be tested to see if it shouldn't be flushed.
     * @return True if the Cache Entry is still valid, false if it should be flushed.
     */

    private boolean isStillValidCacheResource(CacheResourceEntry testEntry){
        Long resourceEntryAge = Date.from(Instant.now()).getTime() - testEntry.getTouchDate().getTime();
        Long ageThreshold = Long.valueOf(specifyCacheElementRetirementInSeconds()) * 1000;
        if(resourceEntryAge > ageThreshold){
            return(false);
        } else {
            return(true);
        }
    }

    /**
     * This method is a simple facade to the VirtualDBKeyManagement method of the same name.
     *
     * This method cycles through all the Identifiers and attempts to return "the best"!
     *
     * Order of preference is: OFFICIAL --> USUAL --> SECONDARY --> TEMP --> OLD --> ANY
     *
     * @param identifierSet The list of Identifiers contained within a Resource
     * @return The "Best" identifier from the set.
     */
    protected Identifier getBestIdentifier(List<Identifier> identifierSet){
        Identifier bestIdentifier = virtualDBKeyManagement.getBestIdentifier(identifierSet);
        return(bestIdentifier);
    }

    //
    // Public Cache Methods
    //

    public VirtualDBMethodOutcome getResource(Identifier identifier){
        getLogger().debug(".getResource(): Entry, id (Identifier) --> {}", identifier);
        VirtualDBMethodOutcome retrievedResource = getResourceFromCache(identifier);
        getLogger().debug(".getResource(): Exit, outcome --> {}", retrievedResource);
        return(retrievedResource);
    }

    public VirtualDBMethodOutcome getResource(IdType id){
        getLogger().debug(".getResource(): Entry, id (IdType) --> {}", id);
        VirtualDBMethodOutcome retrievedResource = getResourceFromCache(id);
        getLogger().debug(".getResource(): Exit, outcome --> {}", retrievedResource);
        return(retrievedResource);
    }

    public VirtualDBMethodOutcome createResource(Resource resourceToAdd){
        getLogger().debug(".createResource(): resourceToAdd --> {}", resourceToAdd);
        VirtualDBMethodOutcome outcome = addResourceToCache(resourceToAdd);
        getLogger().debug(".createResource(): Resource inserted, outcome (VirtualDBMethodOutcome) --> {}", outcome);
        return(outcome);
    }

    public VirtualDBMethodOutcome deleteResource(Resource resourceToRemove){
        getLogger().debug(".removeResource(): resourceToRemove --> {}", resourceToRemove);
        VirtualDBMethodOutcome outcome = deleteResourceFromCache(resourceToRemove.getIdElement());
        getLogger().debug(".removeResource(): Resource removed, outcome (VirtualDBMethodOutcome) --> {}", outcome);
        return(outcome);
    }

    public VirtualDBMethodOutcome updateResource(Resource resourceToUpdate){
        getLogger().debug(".updateResource(): resourceToUpdate --> {}", resourceToUpdate);
        VirtualDBMethodOutcome deleteOutcome = deleteResourceFromCache(resourceToUpdate.getIdElement());
        VirtualDBMethodOutcome updateOutcome = addResourceToCache(resourceToUpdate);
        getLogger().debug(".updateResource(): Resource updated, outcome (VirtualDBMethodOutcome) --> {}", updateOutcome);
        return(updateOutcome);
    }

    public VirtualDBMethodOutcome syncResource(Resource resourceToSync){
        String activityLocation = getCacheClassName() + "::" + "syncResource()";
        if(resourceToSync == null){
            VirtualDBMethodOutcome vdbOutcome = outcomeFactory.generateBadAttributeOutcome(activityLocation, VirtualDBActionTypeEnum.SYNC, VirtualDBActionStatusEnum.SYNC_FAILURE, "Parameter resourceToSync (Resource) content is invalid");
            return(vdbOutcome);
        }
        if(!resourceToSync.hasId()){
            String newID = resourceToSync.getResourceType().toString() + ":" + UUID.randomUUID().toString();
            resourceToSync.setId(newID);
        }
        IdType resourceId = resourceToSync.getIdElement();
        if(resourceCacheById.containsKey(resourceId)){
            Object lockObject = resourceCacheLockSet.get(resourceId);
            if(lockObject == null){
                lockObject = new Object();
                resourceCacheLockSet.put(resourceToSync.getIdElement(), lockObject);
            }
            CacheResourceEntry cacheEntry = resourceCacheById.get(resourceId);
            Resource cacheResource = cacheEntry.getResource();
            synchronized(lockObject) {
                deleteResourceFromCache(resourceId);
                addResourceToCache(resourceToSync);
            }
            VirtualDBMethodOutcome outcome = new VirtualDBMethodOutcome();
            outcome.setCreated(false);
            outcome.setCausalAction(VirtualDBActionTypeEnum.SYNC);
            outcome.setStatusEnum(VirtualDBActionStatusEnum.SYNC_FINISHED);
            CodeableConcept details = new CodeableConcept();
            Coding detailsCoding = new Coding();
            detailsCoding.setSystem("https://www.hl7.org/fhir/codesystem-operation-outcome.html");
            detailsCoding.setCode("MSG_RESOURCE_SYNCHRONISED"); // TODO Pegacorn specific encoding --> need to check validity
            String text = "Resource Id ("+ resourceId +") has been synchronised";
            detailsCoding.setDisplay(text);
            details.setText(text);
            details.addCoding(detailsCoding);
            OperationOutcome opOutcome = new OperationOutcome();
            OperationOutcome.OperationOutcomeIssueComponent newOutcomeComponent = new OperationOutcome.OperationOutcomeIssueComponent();
            newOutcomeComponent.setDiagnostics(activityLocation);
            newOutcomeComponent.setDetails(details);
            newOutcomeComponent.setCode(OperationOutcome.IssueType.INFORMATIONAL);
            newOutcomeComponent.setSeverity(OperationOutcome.IssueSeverity.INFORMATION);
            opOutcome.addIssue(newOutcomeComponent);
            outcome.setOperationOutcome(opOutcome);
            return(outcome);
        } else {
            VirtualDBMethodOutcome outcome = addResourceToCache(resourceToSync);
            outcome.setCausalAction(VirtualDBActionTypeEnum.SYNC);
            outcome.setStatusEnum(VirtualDBActionStatusEnum.SYNC_FINISHED);
            return(outcome);
        }
    }

    public Object getResourceLock(IdType resourceId){
        if(resourceCacheLockSet.containsKey(resourceId)){
            return(resourceCacheLockSet.get(resourceId));
        } else {
            Object newLock = new Object();
            resourceCacheLockSet.put(resourceId,newLock);
            return(newLock);
        }
    }

    public Resource startResourceAttributeUpdate(Resource resourceToModify){
        Resource modifiableResource = createClonedResource(resourceToModify);
        return(modifiableResource);
    }

    public void finaliseResourceAttributeUpdate(Resource modifiedResource){
        syncResource(modifiedResource);
    }
}
