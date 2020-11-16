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

import net.fhirfactory.pegacorn.ladon.model.virtualdb.businesskey.VirtualDBKeyManagement;
import net.fhirfactory.pegacorn.ladon.model.virtualdb.cache.CacheResourceEntry;
import net.fhirfactory.pegacorn.ladon.model.virtualdb.operations.VirtualDBActionStatusEnum;
import net.fhirfactory.pegacorn.ladon.model.virtualdb.operations.VirtualDBActionTypeEnum;
import net.fhirfactory.pegacorn.ladon.model.virtualdb.operations.VirtualDBMethodOutcome;
import org.hl7.fhir.r4.model.*;
import org.slf4j.Logger;

import javax.annotation.PostConstruct;
import javax.inject.Inject;
import java.time.Instant;
import java.util.Enumeration;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

public abstract class VirtualDBIdentifierBasedCacheBase {

    @Inject
    private VirtualDBKeyManagement virtualDBKeyManagement;

    private ConcurrentHashMap<Identifier, CacheResourceEntry> resourceCacheByBusinessKey;
    private ConcurrentHashMap<String, Object> resourceCacheLockSet;
    boolean isInitialised;

    protected VirtualDBIdentifierBasedCacheBase() {
        resourceCacheByBusinessKey = new ConcurrentHashMap<>();
        resourceCacheLockSet = new ConcurrentHashMap<>();
        this.isInitialised = false;
    }

    protected abstract Logger getLogger();
    protected abstract int specifyCacheElementRetirementInSeconds();
    protected abstract boolean areTheSame(Resource a, Resource b);
    protected abstract List<Identifier> resolveIdentifierSet(Resource resourceToAdd);
    protected abstract void addIdentifierToResource(Identifier identifierToAdd, Resource resource);
    protected abstract String getCacheClassName();
    protected abstract void synchroniseResource(Resource cacheResource, Resource sourceResource);

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
     * @param identifier A FHIR::Identifier that has a use of either "OFFICIAL" or "USUAL".
     * @param resourceToAdd A FHIR::Resource that is to be added to the Cache.
     * @return A VirtualDBMethodOutcome instance detailing the success (or otherwise) of the Resource
     * addition to the Cache.
     */
    private VirtualDBMethodOutcome addResourceToCache(Identifier identifier, Resource resourceToAdd){
        // Perform house-keeping on the Cache
        purgeResourcesFromCache();
        // House-keeping done

        getLogger().debug(".addResourceToCache(): Entry, identifier (Identifier) --> {}, resourceToAdd (Resource) --> {}", identifier, resourceToAdd);
        if(identifier == null) {
            getLogger().error(".addResourceToCache(): identifier (Identifier) is null, failing out");
            VirtualDBMethodOutcome vdbOutcome = generateBadAttributeOutcome("addResourceToCache", VirtualDBActionTypeEnum.CREATE, VirtualDBActionStatusEnum.CREATION_FAILURE, "Parameter identifier (Identifier) content is invalid");
            return (vdbOutcome);
        }
        if(resourceToAdd == null) {
            getLogger().error(".addResourceToCache(): resourceToAdd (Resource) is null, failing out");
            VirtualDBMethodOutcome vdbOutcome = generateBadAttributeOutcome("addResourceToCache", VirtualDBActionTypeEnum.CREATE, VirtualDBActionStatusEnum.CREATION_FAILURE, "Parameter resourceToAdd (Resource) content is invalid");
            return (vdbOutcome);
        }
        if(!resourceToAdd.hasId()){
            String newID = resourceToAdd.getResourceType().toString() + ":" + UUID.randomUUID().toString();
            resourceToAdd.setId(newID);
        }
        boolean alreadyContainsIdentifier = false;
        for(Identifier resourceToAddIdentifier: resolveIdentifierSet(resourceToAdd)){
            if(identifier.equalsShallow(resourceToAddIdentifier)){
                alreadyContainsIdentifier = true;
                break;
            }
        }
        if(!alreadyContainsIdentifier){
            addIdentifierToResource(identifier, resourceToAdd);
        }
        if(resourceCacheByBusinessKey.containsKey(identifier)){
            CacheResourceEntry resourceEntry = resourceCacheByBusinessKey.get(identifier);
            if(resourceEntry != null){
                Resource existingResource = resourceEntry.getResource();
                if(areTheSame(existingResource, resourceToAdd)){
                    VirtualDBMethodOutcome vdbOutcome = new VirtualDBMethodOutcome();
                    vdbOutcome.setId(existingResource.getIdElement());
                    vdbOutcome.setCreated(false);
                    vdbOutcome.setResource(existingResource);
                    vdbOutcome.setCausalAction(VirtualDBActionTypeEnum.CREATE);
                    vdbOutcome.setStatusEnum(VirtualDBActionStatusEnum.CREATION_NOT_REQUIRED);
                    OperationOutcome opOutcome = new OperationOutcome();
                    opOutcome.setId(existingResource.getIdElement());
                    OperationOutcome.OperationOutcomeIssueComponent newOutcomeComponent = new OperationOutcome.OperationOutcomeIssueComponent();
                    newOutcomeComponent.setCode(OperationOutcome.IssueType.INFORMATIONAL);
                    newOutcomeComponent.setSeverity(OperationOutcome.IssueSeverity.INFORMATION);
                    CodeableConcept details = new CodeableConcept();
                    Coding detailsCoding = new Coding();
                    detailsCoding.setSystem("https://www.hl7.org/fhir/codesystem-operation-outcome.html");
                    detailsCoding.setCode("MSG_CREATED");
                    detailsCoding.setDisplay("New Resource Created");
                    details.setText("New Resource Created");
                    details.addCoding(detailsCoding);
                    newOutcomeComponent.setDiagnostics(getCacheClassName()+"::addResourceToCache()");
                    newOutcomeComponent.setDetails(details);
                    opOutcome.addIssue(newOutcomeComponent);
                    vdbOutcome.setOperationOutcome(opOutcome);
                    return(vdbOutcome);
                }
            }
        }
        boolean alreadyExists = false;
        Identifier existingKey = null;
        for(Identifier currentIdentifier: resourceCacheByBusinessKey.keySet()){
            CacheResourceEntry currentResourceEntry = resourceCacheByBusinessKey.get(currentIdentifier);
            if(currentResourceEntry != null){
                Resource currentResource = currentResourceEntry.getResource();
                for(Identifier currentResourceIdentifier: resolveIdentifierSet(currentResource)){
                    for(Identifier resourceToAddIdentifier: resolveIdentifierSet(resourceToAdd)){
                        if(currentIdentifier.equalsShallow(resourceToAddIdentifier)){
                            alreadyExists = true;
                            existingKey = currentIdentifier;
                            break;
                        }
                    }
                }
                if(alreadyExists){
                    break;
                }
            }
            if (alreadyExists) {
                break;
            }
        }
        VirtualDBMethodOutcome vdbOutcome = new VirtualDBMethodOutcome();
        OperationOutcome opOutcome = new OperationOutcome();
        if(alreadyExists){
            CacheResourceEntry existingResourceEntry = resourceCacheByBusinessKey.get(existingKey);
            resourceCacheByBusinessKey.put(identifier, existingResourceEntry);
            Resource existingResource = existingResourceEntry.getResource();
            vdbOutcome.setResource(existingResource);
            vdbOutcome.setId(existingResource.getIdElement());
            opOutcome.setId(existingResource.getIdElement());
            vdbOutcome.setCreated(false);
            vdbOutcome.setStatusEnum(VirtualDBActionStatusEnum.CREATION_NOT_REQUIRED);
        } else {
            CacheResourceEntry cacheEntry = new CacheResourceEntry(resourceToAdd);
            resourceCacheByBusinessKey.put(identifier, cacheEntry);
            resourceCacheLockSet.put(resourceToAdd.getId(), new Object());
            vdbOutcome.setId(resourceToAdd.getIdElement());
            vdbOutcome.setResource(resourceToAdd);
            opOutcome.setId(resourceToAdd.getIdElement());
            vdbOutcome.setCreated(true);
            vdbOutcome.setStatusEnum(VirtualDBActionStatusEnum.CREATION_FINISH);
        }
        vdbOutcome.setCausalAction(VirtualDBActionTypeEnum.CREATE);
        OperationOutcome.OperationOutcomeIssueComponent newOutcomeComponent = new OperationOutcome.OperationOutcomeIssueComponent();
        newOutcomeComponent.setCode(OperationOutcome.IssueType.INFORMATIONAL);
        newOutcomeComponent.setSeverity(OperationOutcome.IssueSeverity.INFORMATION);
        CodeableConcept details = new CodeableConcept();
        Coding detailsCoding = new Coding();
        detailsCoding.setSystem("https://www.hl7.org/fhir/codesystem-operation-outcome.html");
        detailsCoding.setCode("MSG_CREATED");
        detailsCoding.setDisplay("New Resource Created");
        details.setText("New Resource Created");
        details.addCoding(detailsCoding);
        newOutcomeComponent.setDiagnostics(getCacheClassName()+"::addResourceToCache()");
        newOutcomeComponent.setDetails(details);
        opOutcome.addIssue(newOutcomeComponent);
        vdbOutcome.setOperationOutcome(opOutcome);
        return(vdbOutcome);
    }

    /**
     * This function removes a Resource from the Resource Cache using the provided identifier. It also scans the
     * Cache for any other "reference" to the Resource from other identifiers.
     *
     * @param identifier
     * @param resourceToRemove
     * @return A VirtualDBMethodOutcome instance detailing the success (or otherwise) of the Resource removal activity.
     */
    private VirtualDBMethodOutcome deleteResourceFromCache(Identifier identifier, Resource resourceToRemove){
        // Perform house-keeping on the Cache
        purgeResourcesFromCache();
        // House-keeping done

        if(identifier == null){
            getLogger().error(".deleteResourceFromCache(): identifier (Identifier) is null, failing out");
            VirtualDBMethodOutcome vdbOutcome = generateBadAttributeOutcome("deleteResourceFromCache", VirtualDBActionTypeEnum.DELETE, VirtualDBActionStatusEnum.DELETE_FAILURE, "Parameter identifier (Identifier) content is invalid");
            return(vdbOutcome);
        }
        CacheResourceEntry resourceEntry = resourceCacheByBusinessKey.get(identifier);
        IdType deleteId = null;
        if(resourceEntry != null){
            deleteId = resourceEntry.getResource().getIdElement();
            resourceCacheByBusinessKey.remove(identifier);
            resourceCacheLockSet.remove(identifier);
        }
        Resource cacheResource = resourceEntry.getResource();
        if(!areTheSame(resourceToRemove, cacheResource)){
            for(Identifier currentIdentifier: resourceCacheByBusinessKey.keySet()){
                CacheResourceEntry currentResourceEntry = resourceCacheByBusinessKey.get(currentIdentifier);
                if(currentResourceEntry != null){
                    Resource currentResource = currentResourceEntry.getResource();
                    if(areTheSame(currentResource, cacheResource)){
                        resourceCacheByBusinessKey.remove(currentIdentifier);
                        resourceCacheLockSet.remove(currentIdentifier);
                        if(deleteId == null){
                            deleteId = cacheResource.getIdElement();
                        }
                    }
                }
            }
        }
        for(Identifier currentIdentifier: resourceCacheByBusinessKey.keySet()){
            CacheResourceEntry currentResourceEntry = resourceCacheByBusinessKey.get(currentIdentifier);
            if(currentResourceEntry != null){
                Resource currentResource = currentResourceEntry.getResource();
                if(areTheSame(currentResource, resourceToRemove)){
                    resourceCacheByBusinessKey.remove(currentIdentifier);
                    resourceCacheLockSet.remove(currentIdentifier);
                    if(deleteId == null){
                        deleteId = cacheResource.getIdElement();
                    }
                }
            }
        }
        VirtualDBMethodOutcome vdbOutcome = new VirtualDBMethodOutcome();
        vdbOutcome.setCreated(false);
        if(deleteId != null) {
            vdbOutcome.setId(deleteId);
        }
        vdbOutcome.setResource(resourceToRemove);
        vdbOutcome.setCausalAction(VirtualDBActionTypeEnum.DELETE);
        vdbOutcome.setStatusEnum(VirtualDBActionStatusEnum.DELETE_FINISH);
        OperationOutcome opOutcome = new OperationOutcome();
        OperationOutcome.OperationOutcomeIssueComponent newOutcomeComponent = new OperationOutcome.OperationOutcomeIssueComponent();
        newOutcomeComponent.setCode(OperationOutcome.IssueType.DELETED);
        if(deleteId != null) {
            opOutcome.setId(deleteId);
        }
        newOutcomeComponent.setSeverity(OperationOutcome.IssueSeverity.INFORMATION);
        CodeableConcept details = new CodeableConcept();
        Coding detailsCoding = new Coding();
        detailsCoding.setSystem("https://www.hl7.org/fhir/codesystem-operation-outcome.html");
        detailsCoding.setCode("MSG_DELETED");
        detailsCoding.setDisplay("This resource has been deleted");
        details.setText("This resource has been deleted");
        details.addCoding(detailsCoding);
        newOutcomeComponent.setDiagnostics(getCacheClassName()+"::deleteResourceFromCache()");
        newOutcomeComponent.setDetails(details);
        opOutcome.addIssue(newOutcomeComponent);
        vdbOutcome.setOperationOutcome(opOutcome);
        return(vdbOutcome);
    }

    /**
     *
     * @param identifier
     * @return
     */
    private VirtualDBMethodOutcome getResourceFromCache(Identifier identifier){
        getLogger().debug(".getResourceFromCache(): Entry, identifier (Identifier) --> {}", identifier);
        if(identifier == null){
            getLogger().error(".getResourceFromCache(): identifier (Identifier) is null, failing out");
            VirtualDBMethodOutcome vdbOutcome = generateBadAttributeOutcome("getResourceFromCache", VirtualDBActionTypeEnum.REVIEW, VirtualDBActionStatusEnum.REVIEW_FAILURE, "Parameter identifier (Identifier) content is invalid");
            return(vdbOutcome);
        }
        CacheResourceEntry retrievedResourceEntry = resourceCacheByBusinessKey.get(identifier);
        boolean noResource = false;
        if(retrievedResourceEntry == null){
            noResource = true;
        }
        // Check to see if the retrieved Resource has expired
        if(!isStillValidCacheResource(retrievedResourceEntry)){
            purgeResourcesFromCache();
            noResource = true;
        }
        // Check to see if there is an actual resource in the resource entry!
        Resource retrievedResource = null;
        if(!noResource) {
            retrievedResource = retrievedResourceEntry.getResource();
        }
        if(retrievedResource == null){
            noResource = true;
        }
        if(noResource) {
            VirtualDBMethodOutcome vdbOutcome = new VirtualDBMethodOutcome();
            vdbOutcome.setCreated(false);
            vdbOutcome.setIdentifier(identifier);
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
            vdbOutcome.setResource(retrievedResource);
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
            getLogger().debug(".getResourceFromCache(): exit, resource found... retrieved resource --> {}", retrievedResource);
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
        for(CacheResourceEntry resourceEntry: resourceCacheByBusinessKey.values() ){
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
        Enumeration<Identifier> identifierEnumeration = resourceCacheByBusinessKey.keys();
        while(identifierEnumeration.hasMoreElements()){
            Identifier identifier = identifierEnumeration.nextElement();
            CacheResourceEntry resourceEntry = resourceCacheByBusinessKey.get(identifier);
            if(!isStillValidCacheResource(resourceEntry)){
                getLogger().trace(".purgeResourcesFromCache(): deleting resource --> {}", identifier);
                deleteResourceFromCache(identifier, resourceEntry.getResource());
            }
        }
        getLogger().debug(".purgeResourcesFromCache(): Exit");
    }

    //
    // Shared Methods
    //

    private VirtualDBMethodOutcome generateBadAttributeOutcome(String method, VirtualDBActionTypeEnum action, VirtualDBActionStatusEnum actionStatus, String text){
        VirtualDBMethodOutcome vdbOutcome = new VirtualDBMethodOutcome();
        vdbOutcome.setCreated(false);
        vdbOutcome.setCausalAction(action);
        vdbOutcome.setStatusEnum(actionStatus);
        CodeableConcept details = new CodeableConcept();
        Coding detailsCoding = new Coding();
        detailsCoding.setSystem("https://www.hl7.org/fhir/codesystem-operation-outcome.html");
        detailsCoding.setCode("MSG_PARAM_INVALID");
        detailsCoding.setDisplay(text);
        details.setText(text);
        details.addCoding(detailsCoding);
        OperationOutcome opOutcome = new OperationOutcome();
        OperationOutcome.OperationOutcomeIssueComponent newOutcomeComponent = new OperationOutcome.OperationOutcomeIssueComponent();
        newOutcomeComponent.setDiagnostics(getCacheClassName() + "::" + method);
        newOutcomeComponent.setDetails(details);
        newOutcomeComponent.setCode(OperationOutcome.IssueType.INVALID);
        newOutcomeComponent.setSeverity(OperationOutcome.IssueSeverity.ERROR);
        opOutcome.addIssue(newOutcomeComponent);
        vdbOutcome.setOperationOutcome(opOutcome);
        return(vdbOutcome);
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
        VirtualDBMethodOutcome retrievedCareTeam = getResourceFromCache(identifier);
        getLogger().debug(".getResource(): Exit, outcome --> {}", retrievedCareTeam);
        return(retrievedCareTeam);
    }

    public VirtualDBMethodOutcome createResource(Resource resourceToAdd){
        getLogger().debug(".createResource(): resourceToAdd --> {}", resourceToAdd);
        Identifier identifier = getBestIdentifier(resolveIdentifierSet(resourceToAdd));
        VirtualDBMethodOutcome outcome = addResourceToCache(identifier, resourceToAdd);
        getLogger().debug(".createResource(): Resource inserted, outcome (VirtualDBMethodOutcome) --> {}", outcome);
        return(outcome);
    }

    public VirtualDBMethodOutcome deleteResource(Resource resourceToRemove){
        getLogger().debug(".removeResource(): resourceToRemove --> {}", resourceToRemove);
        Identifier defaultIdentifier = getBestIdentifier(resolveIdentifierSet(resourceToRemove));
        VirtualDBMethodOutcome outcome = deleteResourceFromCache(defaultIdentifier, resourceToRemove);
        getLogger().debug(".removeResource(): Resource removed, outcome (VirtualDBMethodOutcome) --> {}", outcome);
        return(outcome);
    }

    public VirtualDBMethodOutcome updateResource(Resource resourceToUpdate){
        getLogger().debug(".updateResource(): resourceToUpdate --> {}", resourceToUpdate);
        Identifier defaultIdentifier = getBestIdentifier(resolveIdentifierSet(resourceToUpdate));
        VirtualDBMethodOutcome deleteOutcome = deleteResourceFromCache(defaultIdentifier, resourceToUpdate);
        VirtualDBMethodOutcome updateOutcome = addResourceToCache(defaultIdentifier, resourceToUpdate);
        getLogger().debug(".updateResource(): Resource updated, outcome (VirtualDBMethodOutcome) --> {}", updateOutcome);
        return(updateOutcome);
    }

    public VirtualDBMethodOutcome syncResource(Resource resourceToSync){
        if(resourceToSync == null){
            VirtualDBMethodOutcome vdbOutcome = generateBadAttributeOutcome("syncResource", VirtualDBActionTypeEnum.SYNC, VirtualDBActionStatusEnum.SYNC_FAILURE, "Parameter resourceToSync (Resource) content is invalid");
            return(vdbOutcome);
        }
        List<Identifier> identifierSet = resolveIdentifierSet(resourceToSync);
        Identifier bestIdentifier = getBestIdentifier(identifierSet);
        if(bestIdentifier == null){
            VirtualDBMethodOutcome vdbOutcome = generateBadAttributeOutcome("syncResource", VirtualDBActionTypeEnum.SYNC, VirtualDBActionStatusEnum.SYNC_FAILURE, "Parameter identifier (Identifier) content is invalid");
            return(vdbOutcome);
        }
        if(!resourceToSync.hasId()){
            String newID = resourceToSync.getResourceType().toString() + ":" + UUID.randomUUID().toString();
            resourceToSync.setId(newID);
        }
        if(resourceCacheByBusinessKey.containsKey(bestIdentifier)){
            Object lockObject = resourceCacheLockSet.get(bestIdentifier);
            if(lockObject == null){
                lockObject = new Object();
                resourceCacheLockSet.put(resourceToSync.getId(), lockObject);
            }
            CacheResourceEntry cacheEntry = resourceCacheByBusinessKey.get(bestIdentifier);
            if(cacheEntry == null){
                VirtualDBMethodOutcome vdbOutcome = generateBadAttributeOutcome("syncResource", VirtualDBActionTypeEnum.SYNC, VirtualDBActionStatusEnum.SYNC_FAILURE, "Parameter identifier (Identifier) content is invalid");
                return(vdbOutcome);
            }
            Resource cacheResource = cacheEntry.getResource();
            if(cacheResource == null){
                VirtualDBMethodOutcome vdbOutcome = generateBadAttributeOutcome("syncResource", VirtualDBActionTypeEnum.SYNC, VirtualDBActionStatusEnum.SYNC_FAILURE, "Parameter identifier (Identifier) content is invalid");
                return(vdbOutcome);
            }
            synchronized(lockObject) {
                synchroniseResource(cacheResource, resourceToSync);
            }
            VirtualDBMethodOutcome outcome = new VirtualDBMethodOutcome();
            outcome.setCreated(false);
            outcome.setCausalAction(VirtualDBActionTypeEnum.SYNC);
            outcome.setStatusEnum(VirtualDBActionStatusEnum.SYNC_FINISHED);
            CodeableConcept details = new CodeableConcept();
            Coding detailsCoding = new Coding();
            detailsCoding.setSystem("https://www.hl7.org/fhir/codesystem-operation-outcome.html");
            detailsCoding.setCode("MSG_RESOURCE_SYNCHRONISED"); // TODO Pegacorn specific encoding --> need to check validity
            String text = "Resource Id ("+ bestIdentifier +") has been synchronised";
            detailsCoding.setDisplay(text);
            details.setText(text);
            details.addCoding(detailsCoding);
            OperationOutcome opOutcome = new OperationOutcome();
            OperationOutcome.OperationOutcomeIssueComponent newOutcomeComponent = new OperationOutcome.OperationOutcomeIssueComponent();
            newOutcomeComponent.setDiagnostics(getCacheClassName() + "::" + "syncResource()");
            newOutcomeComponent.setDetails(details);
            newOutcomeComponent.setCode(OperationOutcome.IssueType.INFORMATIONAL);
            newOutcomeComponent.setSeverity(OperationOutcome.IssueSeverity.INFORMATION);
            opOutcome.addIssue(newOutcomeComponent);
            outcome.setOperationOutcome(opOutcome);
            return(outcome);
        } else {
            VirtualDBMethodOutcome outcome = createResource(resourceToSync);
            outcome.setCausalAction(VirtualDBActionTypeEnum.SYNC);
            outcome.setStatusEnum(VirtualDBActionStatusEnum.SYNC_FINISHED);
            return(outcome);
        }
    }
}
