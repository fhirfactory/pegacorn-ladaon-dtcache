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
package net.fhirfactory.pegacorn.ladon.dtcache.cache.common;

import java.util.Collection;
import java.util.Enumeration;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

import javax.annotation.PostConstruct;

import org.hl7.fhir.r4.model.IdType;
import org.hl7.fhir.r4.model.Resource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import ca.uhn.fhir.parser.IParser;
import net.fhirfactory.pegacorn.util.FhirUtil;

public class DTCacheResourceCache {

    private static final Logger LOG = LoggerFactory.getLogger(DTCacheResourceCache.class);
    private ConcurrentHashMap<IdType, Resource> resourceCache;
    IParser parserR4;
    boolean isInitialised;

    protected DTCacheResourceCache() {
        resourceCache = new ConcurrentHashMap<>();
        this.isInitialised = false;
    }

    @PostConstruct
    protected void initialise() {
        if (!this.isInitialised) {
            LOG.debug(".initialise(): Initialising the FHIR Parser framework");
            parserR4 = FhirUtil.getInstance().getJsonParser();
            this.isInitialised = true;
        }
    }

    protected IdType addResource(Resource resourceToAdd) {
        if (resourceToAdd.hasId()) {
            resourceCache.put(resourceToAdd.getIdElement(), resourceToAdd);
        } else {
            String newID = resourceToAdd.getResourceType().toString() + ":" + UUID.randomUUID().toString();
            IdType newResourceID = new IdType(newID);
            resourceToAdd.setId(newResourceID);
            resourceCache.put(newResourceID, resourceToAdd);
        }
        return(resourceToAdd.getIdElement());
    }

    protected void removeResource(Resource resourceToRemove) {
        if (resourceToRemove.hasId()) {
            if (resourceCache.containsKey(resourceToRemove.getIdElement())) {
                resourceCache.remove(resourceToRemove.getIdElement());
            }
        }
        if (resourceCache.containsValue(resourceToRemove)) {
            Enumeration<IdType> keySet = resourceCache.keys();
            while (keySet.hasMoreElements()) {
                IdType currentKey = keySet.nextElement();
                if (resourceToRemove.equalsDeep(resourceCache.get(currentKey))) {
                    resourceCache.remove(currentKey);
                    break;
                }
            }
        }
    }

    public Resource getResource(IdType id) {
        if (resourceCache.containsKey(id)) {
            return (resourceCache.get(id));
        } else {
            return (null);
        }
    }

    public Collection<Resource> getAllResources(){
        return(resourceCache.values());
    }
}
