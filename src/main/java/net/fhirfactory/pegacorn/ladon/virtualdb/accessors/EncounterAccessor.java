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
package net.fhirfactory.pegacorn.ladon.virtualdb.accessors;

import net.fhirfactory.pegacorn.ladon.model.virtualdb.operations.VirtualDBActionStatusEnum;
import net.fhirfactory.pegacorn.ladon.model.virtualdb.operations.VirtualDBMethodOutcome;
import net.fhirfactory.pegacorn.ladon.virtualdb.accessors.common.AccessorBase;
import net.fhirfactory.pegacorn.ladon.virtualdb.engine.EncounterDBEngine;
import net.fhirfactory.pegacorn.ladon.virtualdb.engine.common.ResourceDBEngine;
import org.hl7.fhir.r4.model.Encounter;
import org.hl7.fhir.r4.model.Identifier;
import org.hl7.fhir.r4.model.Resource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;
import java.util.ArrayList;
import java.util.List;

@ApplicationScoped
public class EncounterAccessor extends AccessorBase {
    private static final Logger LOG = LoggerFactory.getLogger(EncounterAccessor.class);
    private static final String ACCESSOR_VERSION = "1.0.0";
    private static final String ACCESSOR_RESOURCE = "Encounter";

    @Inject
    EncounterDBEngine resourceDB;

    public EncounterAccessor(){
        super();
    }

    public Class<Encounter> getResourceType() {
        return (Encounter.class);
    }

    @Override
    protected ResourceDBEngine getResourceDBEngine() {
        return (resourceDB);
    }

    @Override
    protected List<Identifier> resolveIdentifierList(Resource resource) {
        if(resource == null){
            return(new ArrayList<>());
        }
        Encounter res = (Encounter)resource;
        List<Identifier> identifierList = res.getIdentifier();
        return(identifierList);
    }

    @Override
    protected String specifyAccessorResourceTypeName() {
        return (ACCESSOR_RESOURCE);
    }

    @Override
    protected String specifyAccessorResourceTypeVersion() {
        return (ACCESSOR_VERSION);
    }

    @Override
    protected Logger getLogger() {
        return (LOG);
    }
}
