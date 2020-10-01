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

import ca.uhn.fhir.context.FhirContext;
import ca.uhn.fhir.parser.IParser;
import net.fhirfactory.pegacorn.ladon.dtcache.cache.common.DTCacheResourceCache;
import org.hl7.fhir.r4.model.IdType;
import org.hl7.fhir.r4.model.Patient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.enterprise.context.ApplicationScoped;
import java.util.UUID;

@ApplicationScoped
public class PatientCache extends DTCacheResourceCache {
    private static final Logger LOG = LoggerFactory.getLogger(PatientCache.class);


    public PatientCache(){
        super();
    }
    

    public Patient getPatient(IdType id){
        LOG.debug(".getPatient(): Entry, id (IdType) --> {}", id);
        if(id==null){
            return(null);
        }
        Patient retrievedPatient = (Patient)getResource(id);
        LOG.debug(".getPatient(): Exit, retrievedPatient (Patient) --> {}", retrievedPatient);
        return(retrievedPatient);
    }

    public IdType addPatient(Patient patientToAdd){
        LOG.debug(".addPatient(): patientToAdd (Patient) --> {}", patientToAdd);
        if( !patientToAdd.hasId()){
            String newID = "Patient:" + UUID.randomUUID().toString();
            patientToAdd.setId(new IdType(newID));
        }
        addResource(patientToAdd);
        IdType newPatientId = new IdType(patientToAdd.getId());
        LOG.debug(".addPatient(): Patient inserted, id --> {}", patientToAdd.getId());
        return(newPatientId);
    }

    public IdType removePatient(Patient patientToRemove){
        LOG.debug(".removePatient(): patientToRemove (Patient) --> {}", patientToRemove);
        IdType id;
        if(patientToRemove.hasId()){
            id = new IdType(patientToRemove.getId());
        } else {
            id = new IdType("No ID");
        }
        removeResource(patientToRemove);
        LOG.debug(".removePatient(): Patient removed, id (String) --> {}", id);
        return(id);
    }

    /**
     * This class updates a patient resource.
     * // TODO We should really look at a Union strategy to be applied to the existing Patient resource?
     * @param patientToUpdate the new Patient resource.
     * @return a copy of the updated Patient resource
     */
    public Patient updatePatient(Patient patientToUpdate){
        LOG.debug(".updatePatient(): patientToUpdate (Patient) --> {}", patientToUpdate);
        Patient oriPatient = getPatient(new IdType(patientToUpdate.getId()));
        removePatient(oriPatient);
        addPatient(patientToUpdate);
        return(patientToUpdate);
    }
}
