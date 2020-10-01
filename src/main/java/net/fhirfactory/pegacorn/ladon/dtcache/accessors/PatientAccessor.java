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
package net.fhirfactory.pegacorn.ladon.dtcache.accessors;

import net.fhirfactory.pegacorn.ladon.processingplant.LadonProcessingPlant;
import net.fhirfactory.pegacorn.ladon.dtcache.accessors.common.AccessorActionTypeEnum;
import net.fhirfactory.pegacorn.ladon.dtcache.accessors.common.AccessorBase;
import net.fhirfactory.pegacorn.ladon.dtcache.cache.PatientCache;
import net.fhirfactory.pegacorn.petasos.model.resilience.activitymatrix.sta.STATransaction;
import net.fhirfactory.pegacorn.petasos.model.uow.UoWProcessingOutcomeEnum;
import org.hl7.fhir.r4.model.IdType;
import org.hl7.fhir.r4.model.Patient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;
import net.fhirfactory.pegacorn.petasos.audit.model.PetasosParcelAuditTrailEntry;

@ApplicationScoped
public class PatientAccessor extends AccessorBase {
    private static final Logger LOG = LoggerFactory.getLogger(PatientAccessor.class);
    private static final String ACCESSOR_VERSION = "1.0.0";
    private static final String ACCESSOR_RESOURCE = "Patient";
    
    public PatientAccessor(){
        super();
    }

    @Inject
    private PatientCache patientCache;

    public Class<Patient> getResourceType() {
        return (Patient.class);
    }

    public Patient getResourceById(IdType theId) {
        LOG.debug(".getResourceById(): Entry, theId (IdType) --> {}", theId);
        PetasosParcelAuditTrailEntry currentTransaction = this.beginTransaction(theId, null, AccessorActionTypeEnum.GET);
        Patient thePatient = patientCache.getPatient(theId);
        if(thePatient != null) {
            this.endTransaction(theId, thePatient, AccessorActionTypeEnum.GET, true, currentTransaction);
        } else {
            this.endTransaction(theId, null, AccessorActionTypeEnum.GET, false, currentTransaction);
        }
        LOG.debug(".getResourceById(): Exit, DocumentReference retrieved --> {}", thePatient);
        return (thePatient);
    }

    public IdType addResource(Patient newPatient){
        LOG.debug(".addResource(): Entry, newPatient (Patient) --> {}", newPatient);
        PetasosParcelAuditTrailEntry currentTransaction = this.beginTransaction(newPatient.getIdElement(), newPatient, AccessorActionTypeEnum.ADD);
        IdType newPatientId = patientCache.addPatient(newPatient);
        if(newPatientId != null) {
            this.endTransaction(newPatientId, newPatient, AccessorActionTypeEnum.ADD, true, currentTransaction);
        } else {
            this.endTransaction(newPatientId, null, AccessorActionTypeEnum.ADD, false, currentTransaction);
        }
        LOG.debug(".addResource(): Exit");
        return(newPatientId);
    }

    public IdType removeResource(Patient patientToRemove){
        LOG.debug(".removeResource(): Entry, patientToRemove (Patient) --> {}", patientToRemove);
        PetasosParcelAuditTrailEntry currentTransaction = this.beginTransaction(patientToRemove.getIdElement(), patientToRemove, AccessorActionTypeEnum.DELETE);
        IdType removedPatientId = patientCache.removePatient(patientToRemove);
        if(removedPatientId != null) {
            this.endTransaction(removedPatientId, null, AccessorActionTypeEnum.DELETE, true, currentTransaction);
        } else {
            this.endTransaction(removedPatientId, null, AccessorActionTypeEnum.DELETE, false, currentTransaction);
        }
        return(removedPatientId);
    }

    @Override
    protected String specifyAccessorName() {
        return (ACCESSOR_RESOURCE);
    }

    @Override
    protected String specifyVersion() {
        return (ACCESSOR_VERSION);
    }

}
