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
import org.hl7.fhir.r4.model.PractitionerRole;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.enterprise.context.ApplicationScoped;
import java.util.UUID;

@ApplicationScoped
public class PractitionerRoleCache extends DTCacheResourceCache {
    private static final Logger LOG = LoggerFactory.getLogger(PractitionerRoleCache.class);
    public PractitionerRoleCache(){
        super();
    }

    public PractitionerRole getPractitionerRole(IdType id){
        LOG.debug(".getPractitionerRole(): Entry, id (IdType) --> {}", id);
        if(id==null){
            return(null);
        }
        PractitionerRole retrievedPractitionerRole = (PractitionerRole)getResource(id);
        LOG.debug(".getPractitionerRole(): Exit, retrievedPractitionerRole (PractitionerRole) --> {}", retrievedPractitionerRole);
        return(retrievedPractitionerRole);
    }

    public IdType addPractitionerRole(PractitionerRole practitionerroleToAdd){
        LOG.debug(".addPractitionerRole(): practitionerroleToAdd (PractitionerRole) --> {}", practitionerroleToAdd);
        if( !practitionerroleToAdd.hasId()){
            String newID = "PractitionerRole:" + UUID.randomUUID().toString();
            practitionerroleToAdd.setId(newID);
        }
        addResource(practitionerroleToAdd);
        IdType addedResourceId = practitionerroleToAdd.getIdElement();
        LOG.debug(".addPractitionerRole(): PractitionerRole inserted, id (IdType) --> {}", addedResourceId);
        return(addedResourceId);
    }

    public IdType removePractitionerRole(PractitionerRole practitionerroleToRemove){
        LOG.debug(".removePractitionerRole(): practitionerroleToRemove (PractitionerRole) --> {}", practitionerroleToRemove);
        String id;
        if(practitionerroleToRemove.hasId()){
            id = practitionerroleToRemove.getId();
        } else {
            id = "No ID";
        }
        removeResource(practitionerroleToRemove);
        IdType removedResourceId = new IdType(id);
        LOG.debug(".removePractitionerRole(): PractitionerRole removed, id (IdType) --> {}", removedResourceId);
        return(removedResourceId);
    }
}
