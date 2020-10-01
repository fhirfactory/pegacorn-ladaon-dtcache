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
import org.hl7.fhir.r4.model.CareTeam;
import org.hl7.fhir.r4.model.IdType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.enterprise.context.ApplicationScoped;
import java.util.UUID;

@ApplicationScoped
public class CareTeamCache  extends DTCacheResourceCache {
    private static final Logger LOG = LoggerFactory.getLogger(CareTeamCache.class);

    public CareTeamCache(){
        super();
    }

    public CareTeam getCareTeam(IdType id){
        LOG.debug(".getCareTeam(): Entry, id (IdType) --> {}", id);
        if(id==null){
            return(null);
        }
        CareTeam retrievedCareTeam = (CareTeam)getResource(id);
        LOG.debug(".getCareTeam(): Exit, retrievedCareTeam (CareTeam) --> {}", retrievedCareTeam);
        return(retrievedCareTeam);
    }

    public IdType addCareTeam(CareTeam careteamToAdd){
        LOG.debug(".insertCareTeam(): careteamToAdd (CareTeam) --> {}", careteamToAdd);
        if( !careteamToAdd.hasId()){
            String newID = "CareTeam:" + UUID.randomUUID().toString();
            careteamToAdd.setId(newID);
        }
        addResource(careteamToAdd);
        IdType addedResourceId = careteamToAdd.getIdElement();
        LOG.debug(".insertCareTeam(): CareTerm inserted, addedResourceId (IdType) --> {}", addedResourceId);
        return(addedResourceId);
    }

    public IdType removeCareTeam(CareTeam careteamToRemove){
        LOG.debug(".removeCareTeam(): careteamToRemove (CareTeam) --> {}", careteamToRemove);
        String id;
        if(careteamToRemove.hasId()){
            id = careteamToRemove.getId();
        } else {
            id = "No ID";
        }
        removeResource(careteamToRemove);
        IdType removedResourceId = new IdType(id);
        LOG.debug(".removeCareTeam(): CareTerm removed, id (String) --> {}", removedResourceId);
        return(removedResourceId);
    }
}
