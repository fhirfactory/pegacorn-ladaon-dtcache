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
import org.hl7.fhir.r4.model.Task;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.enterprise.context.ApplicationScoped;
import java.util.UUID;

@ApplicationScoped
public class TaskCache extends DTCacheResourceCache {
    private static final Logger LOG = LoggerFactory.getLogger(TaskCache.class);

    public TaskCache(){
        super();
    }

    public Task getTask(IdType id){
        LOG.debug(".getTask(): Entry, id (IdType) --> {}", id);
        if(id==null){
            return(null);
        }
        Task retrievedTask = (Task)getResource(id);
        LOG.debug(".getTask(): Exit, retrievedTask (Task) --> {}", retrievedTask);
        return(retrievedTask);
    }

    public IdType addTask(Task taskToAdd){
        LOG.debug(".addTask(): taskToAdd (Task) --> {}", taskToAdd);
        if( !taskToAdd.hasId()){
            String newID = "Task:" + UUID.randomUUID().toString();
            taskToAdd.setId(newID);
        }
        addResource(taskToAdd);
        IdType addedTaskId = taskToAdd.getIdElement();
        LOG.debug(".addTask(): Task inserted, id (IdType) --> {}", addedTaskId);
        return(addedTaskId);
    }

    public IdType removeTask(Task taskToRemove){
        LOG.debug(".removeTask(): taskToRemove (Task) --> {}", taskToRemove);
        String id;
        if(taskToRemove.hasId()){
            id = taskToRemove.getId();
        } else {
            id = "No ID";
        }
        removeResource(taskToRemove);
        IdType removedResourceId = new IdType(id);
        LOG.debug(".removeTask(): Task removed, id (IdType) --> {}", removedResourceId);
        return(removedResourceId);
    }
}
