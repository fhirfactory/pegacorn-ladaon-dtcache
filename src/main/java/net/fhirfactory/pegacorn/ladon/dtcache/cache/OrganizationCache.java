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
import org.hl7.fhir.r4.model.Organization;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.enterprise.context.ApplicationScoped;
import java.util.UUID;

@ApplicationScoped
public class OrganizationCache extends DTCacheResourceCache {
    private static final Logger LOG = LoggerFactory.getLogger(OrganizationCache.class);

    public OrganizationCache(){
        super();
    }

    public Organization getOrganization(IdType id){
        LOG.debug(".getOrganization(): Entry, id (IdType) --> {}", id);
        if(id==null){
            return(null);
        }
        Organization retrievedOrganization = (Organization)getResource(id);
        LOG.debug(".getOrganization(): Exit, retrievedOrganization (Organization) --> {}", retrievedOrganization);
        return(retrievedOrganization);
    }

    public IdType addOrganization(Organization organizationToAdd){
        LOG.debug(".addOrganization(): organizationToAdd (Organization) --> {}", organizationToAdd);
        if( !organizationToAdd.hasId()){
            String newID = "Organization:" + UUID.randomUUID().toString();
            organizationToAdd.setId(newID);
        }
        addResource(organizationToAdd);
        IdType addedResourceId = organizationToAdd.getIdElement();
        LOG.debug(".addOrganization(): Location inserted, id (IdType) --> {}", addedResourceId);
        return(addedResourceId);
    }

    public IdType removeOrganization(Organization organizationToRemove){
        LOG.debug(".removeOrganization(): organizationToRemove (Organization) --> {}", organizationToRemove);
        String id;
        if(organizationToRemove.hasId()){
            id = organizationToRemove.getId();
        } else {
            id = "No ID";
        }
        removeResource(organizationToRemove);
        IdType removedResourceId = new IdType(id);
        LOG.debug(".removeOrganization(): Organization removed, id (IdType) --> {}", removedResourceId);
        return(removedResourceId);
    }
}
