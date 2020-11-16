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
package net.fhirfactory.pegacorn.ladon.virtualdb.cache;

import net.fhirfactory.pegacorn.deployment.properties.LadonDefaultDeploymentProperties;
import net.fhirfactory.pegacorn.ladon.virtualdb.cache.common.VirtualDBIdTypeBasedCacheBase;
import org.hl7.fhir.r4.model.Endpoint;
import org.hl7.fhir.r4.model.Identifier;
import org.hl7.fhir.r4.model.Resource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;
import java.util.ArrayList;
import java.util.List;

@ApplicationScoped
public class EndpointCache extends VirtualDBIdTypeBasedCacheBase {
    private static final Logger LOG = LoggerFactory.getLogger(EndpointCache.class);
    @Inject
    LadonDefaultDeploymentProperties ladonProperties;

    public EndpointCache(){
        super();
    }

    @Override
    protected Logger getLogger() {
        return (LOG);
    }

    @Override
    protected int specifyCacheElementRetirementInSeconds() {
        return (ladonProperties.getEndpointCacheAgeThreshold());
    }

    @Override
    protected boolean areTheSame(Resource a, Resource b) {
        if( a == null && b == null){
            return(true);
        }
        if( a == null || b == null){
            return(false);
        }
        if( a == b ){
            return(true);
        }
        Endpoint endpointA = (Endpoint)a;
        Endpoint endpointB = (Endpoint)b;
        if(endpointA.equalsDeep(endpointB)){
            return(true);
        }
        return(false);
    }

    @Override
    protected List<Identifier> resolveIdentifierSet(Resource resourceToAdd) {
        if(resourceToAdd == null){
            return(new ArrayList<>());
        }
        Endpoint ep = (Endpoint)resourceToAdd;
        if(!ep.hasIdentifier()){
            return(new ArrayList<>());
        }
        List<Identifier> identifierList = ep.getIdentifier();
        return(identifierList);
    }

    @Override
    protected void addIdentifierToResource(Identifier identifierToAdd, Resource resource) {
        if(identifierToAdd == null || resource == null){
            return;
        }
        Endpoint ep = (Endpoint)resource;
        ep.addIdentifier(identifierToAdd);
    }

    @Override
    protected String getCacheClassName() {
        return ("Endpoint");
    }

    @Override
    protected Resource createClonedResource(Resource resource) {
        Endpoint originalDocRef = (Endpoint)resource;
        Endpoint clonedDocRef = new Endpoint();
        originalDocRef.copyValues(clonedDocRef);
        return(clonedDocRef);
    }

}
