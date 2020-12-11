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
package net.fhirfactory.pegacorn.ladon.virtualdb.persistence;

import ca.uhn.fhir.rest.api.MethodOutcome;
import ca.uhn.fhir.rest.client.api.IGenericClient;
import net.fhirfactory.pegacorn.ladon.model.virtualdb.businesskey.VirtualDBKeyManagement;
import net.fhirfactory.pegacorn.ladon.model.virtualdb.operations.VirtualDBMethodOutcome;
import net.fhirfactory.pegacorn.ladon.virtualdb.persistence.common.PersistenceServiceBase;
import net.fhirfactory.pegacorn.ladon.virtualdb.persistence.servers.ClinicalSummaryPersistenceServerSecureAccessor;
import net.fhirfactory.pegacorn.platform.restfulapi.PegacornInternalFHIRClientServices;
import org.hl7.fhir.r4.model.Identifier;
import org.hl7.fhir.r4.model.Procedure;
import org.hl7.fhir.r4.model.Resource;
import org.hl7.fhir.r4.model.ResourceType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;

@ApplicationScoped
public class ProcedurePersistenceService extends PersistenceServiceBase {
    private static final Logger LOG = LoggerFactory.getLogger(ProcedurePersistenceService.class);

    @Inject
    private ClinicalSummaryPersistenceServerSecureAccessor persistenceServerSecureAccessor;

    @Inject
    VirtualDBKeyManagement virtualDBKeyResolver;

    @Override
    protected String specifyPersistenceServiceName() {
        return ("ProcedurePersistenceService");
    }

    @Override
    protected String specifyPersistenceServiceVersion() {
        return ("4.0.1");
    }

    @Override
    protected Logger getLogger() {
        return (LOG);
    }

    @Override
    protected PegacornInternalFHIRClientServices getFHIRClientServices() {
        return (persistenceServerSecureAccessor);
    }

    @Override
    public VirtualDBMethodOutcome synchroniseResource(ResourceType resourceType, Resource resource) {
        return null;
    }

    @Override
    protected Identifier getBestIdentifier(MethodOutcome outcome) {
        if(outcome == null){
            return(null);
        }
        Resource containedResource = (Resource)outcome.getResource();
        if(containedResource == null){
            return(null);
        }
        Procedure actualResource = (Procedure) containedResource;
        if(actualResource.hasIdentifier()){
            Identifier bestIdentifier = virtualDBKeyResolver.getBestIdentifier(actualResource.getIdentifier());
            return(bestIdentifier);
        }
        return(null);
    }
}
