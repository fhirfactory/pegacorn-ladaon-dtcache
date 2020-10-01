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
package net.fhirfactory.pegacorn.ladon.dtcache.cache;

import net.fhirfactory.pegacorn.ladon.dtcache.cache.common.DTCacheResourceCache;
import org.hl7.fhir.r4.model.HealthcareService;
import org.hl7.fhir.r4.model.IdType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.enterprise.context.ApplicationScoped;
import java.util.UUID;

@ApplicationScoped
public class HealthcareServiceCache extends DTCacheResourceCache {
    private static final Logger LOG = LoggerFactory.getLogger(HealthcareServiceCache.class);

    public HealthcareServiceCache(){
        super();
    }

    public HealthcareService getHealthcareService(IdType id){
        LOG.debug(".getHealthcareService(): Entry, id (IdType) --> {}", id);
        if(id==null){
            return(null);
        }
        HealthcareService retrievedHealthCareService = (HealthcareService)getResource(id);
        LOG.debug(".getHealthcareService(): Exit, retrievedGroup (Group) --> {}", retrievedHealthCareService);
        return(retrievedHealthCareService);
    }

    public IdType addHealthcareService(HealthcareService healthcareserviceToAdd){
        LOG.debug(".addHealthcareService(): healthcareserviceToAdd (HealthcareService) --> {}", healthcareserviceToAdd);
        if( !healthcareserviceToAdd.hasId()){
            String newID = "HealthcareService:" + UUID.randomUUID().toString();
            healthcareserviceToAdd.setId(newID);
        }
        addResource(healthcareserviceToAdd);
        IdType addedResourceId = healthcareserviceToAdd.getIdElement();
        LOG.debug(".addHealthcareService(): HealthcareService inserted, addedResourceId (IdType) --> {}", addedResourceId);
        return(addedResourceId);
    }

    public IdType removeHealthcareService(HealthcareService healthcareserviceToRemove){
        LOG.debug(".removeHealthcareService(): healthcareserviceToRemove (HealthcareService) --> {}", healthcareserviceToRemove);
        String id;
        if(healthcareserviceToRemove.hasId()){
            id = healthcareserviceToRemove.getId();
        } else {
            id = "No ID";
        }
        removeResource(healthcareserviceToRemove);
        IdType removedResourceId = new IdType(id);
        LOG.debug(".removeHealthcareService(): HealthcareService removed, removedResourceId (IdType) --> {}", removedResourceId);
        return(removedResourceId);
    }
}
