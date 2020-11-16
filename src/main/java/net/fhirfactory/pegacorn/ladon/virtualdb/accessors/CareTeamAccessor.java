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
import net.fhirfactory.pegacorn.ladon.virtualdb.engine.CareTeamDBEngine;
import net.fhirfactory.pegacorn.ladon.virtualdb.engine.common.ResourceDBEngine;
import org.hl7.fhir.r4.model.CareTeam;
import org.hl7.fhir.r4.model.Identifier;
import org.hl7.fhir.r4.model.Resource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;
import java.util.ArrayList;
import java.util.List;

@ApplicationScoped
public class CareTeamAccessor extends AccessorBase {
    private static final Logger LOG = LoggerFactory.getLogger(CareTeamAccessor.class);
    private static final String ACCESSOR_VERSION = "4.0.1";
    private static final String ACCESSOR_RESOURCE = "CareTeam";

    @Inject
    private CareTeamDBEngine careTeamDBEngine;

    public CareTeamAccessor(){
        super();
    }

    public Class<CareTeam> getResourceType() {
        return (CareTeam.class);
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

    @Override
    protected ResourceDBEngine getResourceDBEngine() {
        return (careTeamDBEngine);
    }

    @Override
    protected List<Identifier> resolveIdentifierList(Resource resource) {
        if(resource == null){
            return(new ArrayList<>());
        }
        CareTeam careTeam = (CareTeam)resource;
        List<Identifier> identifierList = careTeam.getIdentifier();
        return(identifierList);
    }

    /**
     * This function is (primarily) used by the StateSpace framework to rapidly, and without an audit-trail,
     * access the specific Resource.
     *
     * @param identifier
     * @return The CareTeam resource associated within the identifier.
     */
    public CareTeam getCareTeam(Identifier identifier){
        VirtualDBMethodOutcome outcome = getResourceNoAudit(identifier);
        if(outcome.getStatusEnum() == VirtualDBActionStatusEnum.REVIEW_FINISH){
            CareTeam retrievedCareTeam = (CareTeam)outcome.getResource();
            return(retrievedCareTeam);
        }
        return(null);
    }
}
