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
import org.hl7.fhir.r4.model.Communication;
import org.hl7.fhir.r4.model.IdType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.enterprise.context.ApplicationScoped;
import java.util.UUID;

@ApplicationScoped
public class CommunicationCache extends DTCacheResourceCache {
    private static final Logger LOG = LoggerFactory.getLogger(CommunicationCache.class);
    public CommunicationCache(){
        super();
    }

    public Communication getCommunication(IdType id){
        LOG.debug(".getCommunication(): Entry, id (IdType) --> {}", id);
        if(id==null){
            return(null);
        }
        Communication retrievedCommunication = (Communication)getResource(id);
        LOG.debug(".getCommunication(): Exit, retrievedCommunication (Communication) --> {}", retrievedCommunication);
        return(retrievedCommunication);
    }

    public IdType addCommunication(Communication communicationToAdd){
        LOG.debug(".addCommunication(): communicationToAdd (Communication) --> {}", communicationToAdd);
        if( !communicationToAdd.hasId()){
            String newID = "Communication:" + UUID.randomUUID().toString();
            communicationToAdd.setId(newID);
        }
        addResource(communicationToAdd);
        IdType addedResourceId = communicationToAdd.getIdElement();
        LOG.debug(".addCommunication(): Communication inserted, addedResourceId (IdType0 --> {}", addedResourceId);
        return(addedResourceId);
    }

    public IdType removeCommunication(Communication communicationToRemove){
        LOG.debug(".removeCommunication(): communicationToRemove (Communication) --> {}", communicationToRemove);
        String id;
        if(communicationToRemove.hasId()){
            id = communicationToRemove.getId();
        } else {
            id = "No ID";
        }
        removeResource(communicationToRemove);
        IdType removedResourceId = new IdType(id);
        LOG.debug(".removeCommunication(): Communication removed, removedResourceId (IdType) --> {}", removedResourceId);
        return(removedResourceId);
    }
}
