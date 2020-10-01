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
import org.hl7.fhir.r4.model.DocumentReference;
import org.hl7.fhir.r4.model.IdType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.enterprise.context.ApplicationScoped;
import java.util.UUID;

@ApplicationScoped
public class DocumentReferenceCache extends DTCacheResourceCache {
    private static final Logger LOG = LoggerFactory.getLogger(DocumentReferenceCache.class);
    public DocumentReferenceCache(){
        super();
    }

    public DocumentReference getDocumentReference(IdType id){
        LOG.debug(".getDocumentReference(): Entry, id (IdType) --> {}", id);
        if(id==null){
            return(null);
        }
        DocumentReference retrievedDocumentReference = (DocumentReference)getResource(id);
        LOG.debug(".getDocumentReference(): Exit, retrievedDocumentReference (DocumentReference) --> {}", retrievedDocumentReference);
        return(retrievedDocumentReference);
    }

    public IdType addDocumentReference(DocumentReference documentreferenceToAdd){
        LOG.debug(".addDocumentReference(): documentreferenceToAdd (CommunicationRequest) --> {}", documentreferenceToAdd);
        if( !documentreferenceToAdd.hasId()){
            String newID = "DocumentReference:" + UUID.randomUUID().toString();
            documentreferenceToAdd.setId(newID);
        }
        addResource(documentreferenceToAdd);
        IdType newDocRefId = new IdType(documentreferenceToAdd.getId());
        LOG.debug(".addDocumentReference(): DocumentReference inserted, newDocRefId (IdType) --> {}", newDocRefId);
        return(newDocRefId);
    }

    public IdType removeDocumentReference(DocumentReference documentreferenceToRemove){
        LOG.debug(".removeDocumentReference(): documentreferenceToRemove (DocumentReference) --> {}", documentreferenceToRemove);
        String id;
        if(documentreferenceToRemove.hasId()){
            id = documentreferenceToRemove.getId();
        } else {
            id = "No ID";
        }
        removeResource(documentreferenceToRemove);
        IdType removedResourceId = new IdType(id);
        LOG.debug(".removeDocumentReference(): DocumentReference removed, removedResourceId (IdType) --> {}", removedResourceId);
        return(removedResourceId);
    }

}
