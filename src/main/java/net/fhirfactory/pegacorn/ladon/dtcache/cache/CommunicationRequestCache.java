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
import org.hl7.fhir.r4.model.CommunicationRequest;
import org.hl7.fhir.r4.model.IdType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.enterprise.context.ApplicationScoped;
import java.util.UUID;

@ApplicationScoped
public class CommunicationRequestCache extends DTCacheResourceCache {
    private static final Logger LOG = LoggerFactory.getLogger(CommunicationRequestCache.class);
    public CommunicationRequestCache(){
        super();
    }

    public CommunicationRequest getCommunicationRequest(IdType id){
        LOG.debug(".getCommunicationRequest(): Entry, id (IdType) --> {}", id);
        if(id==null){
            return(null);
        }
        CommunicationRequest retrievedCommunicationRequest = (CommunicationRequest)getResource(id);
        LOG.debug(".getCommunicationRequest(): Exit, retrievedCommunicationRequest (CommunicationRequest) --> {}", retrievedCommunicationRequest);
        return(retrievedCommunicationRequest);
    }

    public IdType addCommunicationRequest(CommunicationRequest communicationrequestToAdd){
        LOG.debug(".addCommunicationRequest(): communicationrequestToAdd (CommunicationRequest) --> {}", communicationrequestToAdd);
        if( !communicationrequestToAdd.hasId()){
            String newID = "CommunicationRequest:" + UUID.randomUUID().toString();
            communicationrequestToAdd.setId(newID);
        }
        addResource(communicationrequestToAdd);
        IdType addedResourceId = communicationrequestToAdd.getIdElement();
        LOG.debug(".addCommunicationRequest(): CommunicationRequest inserted, addedResourceId (IdType) --> {}", addedResourceId);
        return(addedResourceId);
    }

    public IdType removeCommunicationRequest(CommunicationRequest communicationrequestToRemove){
        LOG.debug(".removeCommunicationRequest(): communicationrequestToRemove (CommunicationRequest) --> {}", communicationrequestToRemove);
        String id;
        if(communicationrequestToRemove.hasId()){
            id = communicationrequestToRemove.getId();
        } else {
            id = "No ID";
        }
        removeResource(communicationrequestToRemove);
        IdType removedResourceId = new IdType(id);
        LOG.debug(".removeCommunicationRequest(): CommunicationRequest removed, removedResourceId (IdType) --> {}", removedResourceId);
        return(removedResourceId);
    }
}
