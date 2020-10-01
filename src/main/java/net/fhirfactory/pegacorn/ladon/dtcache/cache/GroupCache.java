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
import org.hl7.fhir.r4.model.Group;
import org.hl7.fhir.r4.model.IdType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.enterprise.context.ApplicationScoped;
import java.util.UUID;

@ApplicationScoped
public class GroupCache extends DTCacheResourceCache {
    private static final Logger LOG = LoggerFactory.getLogger(GroupCache.class);
    public GroupCache(){
        super();
    }

    public Group getGroup(IdType id){
        LOG.debug(".getGroup(): Entry, id (IdType) --> {}", id);
        if(id==null){
            return(null);
        }
        Group retrievedGroup = (Group)getResource(id);
        LOG.debug(".getGroup(): Exit, retrievedGroup (Group) --> {}", retrievedGroup);
        return(retrievedGroup);
    }

    public IdType addGroup(Group groupToAdd){
        LOG.debug(".addGroup(): groupToAdd (Group) --> {}", groupToAdd);
        if( !groupToAdd.hasId()){
            String newID = "Group:" + UUID.randomUUID().toString();
            groupToAdd.setId(newID);
        }
        addResource(groupToAdd);
        IdType addedResourceId = groupToAdd.getIdElement();
        LOG.debug(".addGroup(): Group inserted, addedResourceId (IdType) --> {}", addedResourceId);
        return(addedResourceId);
    }

    public IdType removeGroup(Group groupToRemove){
        LOG.debug(".removeGroup(): groupToRemove (Group) --> {}", groupToRemove);
        String id;
        if(groupToRemove.hasId()){
            id = groupToRemove.getId();
        } else {
            id = "No ID";
        }
        removeResource(groupToRemove);
        IdType removedResourceId = new IdType(id);
        LOG.debug(".removeGroup(): Group removed, removedResourceId (IdType) --> {}", removedResourceId);
        return(removedResourceId);
    }
}
