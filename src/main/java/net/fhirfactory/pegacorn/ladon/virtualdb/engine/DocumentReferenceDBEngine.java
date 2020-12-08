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
package net.fhirfactory.pegacorn.ladon.virtualdb.engine;

import net.fhirfactory.pegacorn.ladon.mdr.conduit.controller.DocumentReferenceSoTConduitController;
import net.fhirfactory.pegacorn.ladon.mdr.conduit.controller.common.ResourceSoTConduitController;
import net.fhirfactory.pegacorn.ladon.virtualdb.cache.DocumentReferenceCache;
import net.fhirfactory.pegacorn.ladon.virtualdb.cache.common.VirtualDBIdTypeBasedCacheBase;
import net.fhirfactory.pegacorn.ladon.virtualdb.engine.common.ResourceDBEngine;
import net.fhirfactory.pegacorn.ladon.virtualdb.persistence.DocumentReferencePersistenceService;
import net.fhirfactory.pegacorn.ladon.virtualdb.persistence.common.PersistenceServiceBase;
import org.hl7.fhir.r4.model.DocumentReference;
import org.hl7.fhir.r4.model.Identifier;
import org.hl7.fhir.r4.model.Resource;
import org.hl7.fhir.r4.model.ResourceType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;
import java.util.ArrayList;
import java.util.List;

@ApplicationScoped
public class DocumentReferenceDBEngine extends ResourceDBEngine  {
    private static final Logger LOG = LoggerFactory.getLogger(DocumentReferenceDBEngine.class);

    @Inject
    private DocumentReferenceCache myResourceCache;

    @Inject
    private DocumentReferenceSoTConduitController mySoTAggregator;

    @Inject
    private DocumentReferencePersistenceService persistenceService;

    @Override
    protected VirtualDBIdTypeBasedCacheBase specifyDBCache() {
        return (myResourceCache);
    }

    @Override
    protected ResourceSoTConduitController specifySourceOfTruthAggregator() {
        return (mySoTAggregator);
    }

    @Override
    protected Logger getLogger() {
        return (LOG);
    }

    @Override
    protected PersistenceServiceBase specifyPersistenceService(){return(persistenceService);}

    @Override
    protected ResourceType specifyResourceType(){return(ResourceType.DocumentReference);}

    @Override
    protected List<Identifier> resolveIdentifierSet(Resource resource) {
        if(resource == null){
            ArrayList<Identifier> identifierList = new ArrayList<>();
            return(identifierList);
        }
        DocumentReference actualResource = (DocumentReference) resource;
        if(actualResource.hasIdentifier()){
            return(actualResource.getIdentifier());
        } else {
            ArrayList<Identifier> identifierList = new ArrayList<>();
            return(identifierList);
        }
    }

}
