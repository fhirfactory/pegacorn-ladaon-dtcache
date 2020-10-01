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
import org.hl7.fhir.r4.model.IdType;
import org.hl7.fhir.r4.model.Practitioner;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.enterprise.context.ApplicationScoped;
import java.util.UUID;

@ApplicationScoped
public class PractitionerCache extends DTCacheResourceCache {
    private static final Logger LOG = LoggerFactory.getLogger(PractitionerCache.class);

    public PractitionerCache(){
        super();
    }

    public Practitioner getPractitioner(IdType id){
        LOG.debug(".getPractitioner(): Entry, id (IdType) --> {}", id);
        if(id==null){
            return(null);
        }
        Practitioner retrievedPractitioner = (Practitioner)getResource(id);
        LOG.debug(".getPractitioner(): Exit, retrievedPractitioner (Practitioner) --> {}", retrievedPractitioner);
        return(retrievedPractitioner);
    }

    public IdType addPractitioner(Practitioner practitionerToAdd){
        LOG.debug(".addPractitioner(): practitionerToAdd (Practitioner) --> {}", practitionerToAdd);
        if( !practitionerToAdd.hasId()){
            String newID = "Practitioner:" + UUID.randomUUID().toString();
            practitionerToAdd.setId(newID);
        }
        addResource(practitionerToAdd);
        IdType practitionerId = new IdType(practitionerToAdd.getId());
        LOG.debug(".addPractitioner(): Practitioner inserted, id --> {}",practitionerId);
        return(practitionerId);
    }

    public IdType removePractitioner(Practitioner practitionerToRemove){
        LOG.debug(".removePractitioner(): practitionerToRemove (Practitioner) --> {}", practitionerToRemove);
        String id;
        if(practitionerToRemove.hasId()){
            id = practitionerToRemove.getId();
        } else {
            id = "No ID";
        }
        removeResource(practitionerToRemove);
        IdType removedResourceID = new IdType(id);
        LOG.debug(".removePractitioner(): Practitioner removed, id (String) --> {}", removedResourceID);
        return(removedResourceID);
    }
}
