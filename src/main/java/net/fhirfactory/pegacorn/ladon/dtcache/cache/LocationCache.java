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
import org.hl7.fhir.r4.model.Location;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.enterprise.context.ApplicationScoped;
import java.util.UUID;

@ApplicationScoped
public class LocationCache extends DTCacheResourceCache {
    private static final Logger LOG = LoggerFactory.getLogger(LocationCache.class);
    public LocationCache(){
        super();
    }

    public Location getLocation(IdType id){
        LOG.debug(".getLocation(): Entry, id (IdType) --> {}", id);
        if(id==null){
            return(null);
        }
        Location retrievedLocation = (Location)getResource(id);
        LOG.debug(".getLocation(): Exit, retrievedLocation (Location) --> {}", retrievedLocation);
        return(retrievedLocation);
    }

    public IdType addLocation(Location locationToAdd){
        LOG.debug(".addLocation(): locationToAdd (Location) --> {}", locationToAdd);
        if( !locationToAdd.hasId()){
            String newID = "Location:" + UUID.randomUUID().toString();
            locationToAdd.setId(newID);
        }
        addResource(locationToAdd);
        IdType addedResourceId = locationToAdd.getIdElement();
        LOG.debug(".addLocation(): Location inserted, id (IdType) --> {}", addedResourceId);
        return(addedResourceId);
    }

    public IdType removeLocation(Location locationToRemove){
        LOG.debug(".removeLocation(): locationToRemove (Location) --> {}", locationToRemove);
        String id;
        if(locationToRemove.hasId()){
            id = locationToRemove.getId();
        } else {
            id = "No ID";
        }
        removeResource(locationToRemove);
        IdType removedResourceId = new IdType(id);
        LOG.debug(".removeLocation(): Location removed, removedResourceId (ItType) --> {}", removedResourceId);
        return(removedResourceId);
    }
}
