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
import org.hl7.fhir.r4.model.ValueSet;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.enterprise.context.ApplicationScoped;
import java.util.UUID;

@ApplicationScoped
public class ValueSetCache extends DTCacheResourceCache {
    private static final Logger LOG = LoggerFactory.getLogger(ValueSetCache.class);

    public ValueSetCache(){
        super();
    }

    public ValueSet getValueSet(IdType id){
        LOG.debug(".getValueSet(): Entry, id (IdType) --> {}", id);
        if(id==null){
            return(null);
        }
        ValueSet retrievedValueSet = (ValueSet)getResource(id);
        LOG.debug(".getValueSet(): Exit, retrievedTask (ValueSet) --> {}", retrievedValueSet);
        return(retrievedValueSet);
    }

    public IdType addValueSet(ValueSet valuesetToAdd){
        LOG.debug(".addValueSet(): valuesetToAdd (ValueSet) --> {}", valuesetToAdd);
        if( !valuesetToAdd.hasId()){
            String newID = "ValueSet:" + UUID.randomUUID().toString();
            valuesetToAdd.setId(newID);
        }
        addResource(valuesetToAdd);
        IdType newResourceId = valuesetToAdd.getIdElement();
        LOG.debug(".addValueSet(): ValueSet inserted, id (IdType) --> {}", newResourceId);
        return(newResourceId);
    }

    public IdType removeValueSet(ValueSet valuesetToRemove){
        LOG.debug(".removeValueSet(): valuesetToRemove (ValueSet) --> {}", valuesetToRemove);
        String id;
        if(valuesetToRemove.hasId()){
            id = valuesetToRemove.getId();
        } else {
            id = "No ID";
        }
        removeResource(valuesetToRemove);
        IdType removedResourceId = new IdType(id);
        LOG.debug(".removeValueSet(): ValueSet removed, id (IdType) --> {}", removedResourceId);
        return(removedResourceId);
    }
}
