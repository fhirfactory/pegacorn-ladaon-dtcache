/*
 * Copyright (c) 2020 Mark A. Hunter (ACT Health)
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
package net.fhirfactory.pegacorn.ladon.virtualdb.snapshot;

import net.fhirfactory.pegacorn.ladon.virtualdb.persistence.common.PersistenceServiceBase;
import org.hl7.fhir.r4.model.ResourceType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.enterprise.context.ApplicationScoped;
import java.util.Enumeration;
import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

@ApplicationScoped
public class FHIRPlaceShardSelector {
    private static final Logger LOG = LoggerFactory.getLogger(FHIRPlaceShardSelector.class);

    private ConcurrentHashMap<PersistenceServiceBase, Set<ResourceType>> fhirplaceInstanceMap;

    public FHIRPlaceShardSelector(){
        this.fhirplaceInstanceMap = new ConcurrentHashMap<>();
    }

    public void registerFHIRPlaceInstance(PersistenceServiceBase newFHIRPlaceInstance, Set<ResourceType> supportedResources){
        LOG.debug(".registerFHIRPlaceInstance(): Entry, newFHIRPlaceInstance --> {}, supportedResources --> {}", newFHIRPlaceInstance, supportedResources);
        if(newFHIRPlaceInstance == null){
            LOG.debug(".registerFHIRPlaceInstance(): Exit, newFHIRPlaceInstance is null!");
            return;
        }
        if(supportedResources == null){
            LOG.debug(".registerFHIRPlaceInstance(): Exit, supportedRsources is null!");
            return;
        }
        Enumeration<PersistenceServiceBase> fhirplaceList = fhirplaceInstanceMap.keys();
        while(fhirplaceList.hasMoreElements()){
            PersistenceServiceBase fhirplaceInstance = fhirplaceList.nextElement();
            Set<ResourceType> resourceTypes = fhirplaceInstanceMap.get(fhirplaceInstance);
            for(ResourceType currentType: resourceTypes){
                if(supportedResources.contains(currentType)){
                    if(fhirplaceInstance != newFHIRPlaceInstance) {
                        LOG.error(".registerFHIRPlaceInstance(): Cannot add ResourceType --> {} to FHIRPlaceInstance --> {}, as it is already bound to FHIRPlaceInstance --> {}", currentType, newFHIRPlaceInstance, fhirplaceInstance);
                        return;
                    }
                }
            }
        }
        if(fhirplaceInstanceMap.containsKey(newFHIRPlaceInstance)){
            fhirplaceInstanceMap.get(newFHIRPlaceInstance).addAll(supportedResources);
        } else {
            Set<ResourceType> newResourceTypeSet = new HashSet<ResourceType>();
            newResourceTypeSet.addAll(supportedResources);
            fhirplaceInstanceMap.put(newFHIRPlaceInstance, newResourceTypeSet);
        }
    }

    public PersistenceServiceBase getFHIRPlaceInstance(ResourceType resourceType){
        LOG.debug(".getFHIRPlaceInstance(): No FHIRPlace registered to support Resource --> {}", resourceType);
        Enumeration<PersistenceServiceBase> fhirplaceList = fhirplaceInstanceMap.keys();
        while(fhirplaceList.hasMoreElements()){
            PersistenceServiceBase fhirplaceInstance = fhirplaceList.nextElement();
            Set<ResourceType> resourceTypes = fhirplaceInstanceMap.get(fhirplaceInstance);
            if(resourceTypes.contains(resourceType)){
                return(fhirplaceInstance);
            }
        }
        LOG.error(".getFHIRPlaceInstance(): No FHIRPlace registered to support Resource --> {}", resourceType);
        return(null);
    }
}
