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

package net.fhirfactory.pegacorn.ladon.dtcache.accessors.common;

import org.hl7.fhir.r4.model.CodeableConcept;
import org.hl7.fhir.r4.model.Coding;
import org.hl7.fhir.r4.model.IdType;
import org.hl7.fhir.r4.model.OperationOutcome;

import javax.enterprise.context.ApplicationScoped;

@ApplicationScoped
public class OperationOutcomeGenerator {

    public OperationOutcome generateResourceNotFoundOutcome(IdType resourceId, OperationOutcomeSeverityEnum severityEnum){
        // Create Issue
        OperationOutcome.OperationOutcomeIssueComponent issue = new OperationOutcome.OperationOutcomeIssueComponent();
        issue.setSeverity(OperationOutcome.IssueSeverity.valueOf(severityEnum.getSeverityCode()));
        issue.setCode(OperationOutcome.IssueType.NOTFOUND);
        CodeableConcept details = new CodeableConcept();
        Coding detailsCoding = new Coding();
        detailsCoding.setCode("MSG_NO_EXIST");
        detailsCoding.setDisplay("Resource Id " + resourceId + " does not exist");
        detailsCoding.setSystem("http://hl7.org/fhir/ValueSet/operation-outcome");
        detailsCoding.setVersion("4.0.1");
        details.addCoding(detailsCoding);
        details.setText("Resource Id " + resourceId + " does not exist");
        issue.setDetails(details);
        // Create Outcome
        OperationOutcome outcome = new OperationOutcome();
        outcome.setId(resourceId);
        outcome.addIssue(issue);
        return(outcome);
    }

    public OperationOutcome generateResourceDeletedOutcome(IdType resourceId, OperationOutcomeSeverityEnum severityEnum){
        // Create Issue
        OperationOutcome.OperationOutcomeIssueComponent issue = new OperationOutcome.OperationOutcomeIssueComponent();
        issue.setSeverity(OperationOutcome.IssueSeverity.valueOf(severityEnum.getSeverityCode()));
        issue.setCode(OperationOutcome.IssueType.DELETED);
        CodeableConcept details = new CodeableConcept();
        Coding detailsCoding = new Coding();
        detailsCoding.setCode("MSG_DELETED");
        detailsCoding.setDisplay("The resource has been deleted");
        detailsCoding.setSystem("http://hl7.org/fhir/ValueSet/operation-outcome");
        detailsCoding.setVersion("4.0.1");
        details.addCoding(detailsCoding);
        details.setText("The resource has been deleted");
        issue.setDetails(details);
        // Create Outcome
        OperationOutcome outcome = new OperationOutcome();
        outcome.setId(resourceId);
        outcome.addIssue(issue);
        return(outcome);
    }

    public OperationOutcome generateResourceAddedOutcome(IdType resourceId, OperationOutcomeSeverityEnum severityEnum){
        // Create Issue
        OperationOutcome.OperationOutcomeIssueComponent issue = new OperationOutcome.OperationOutcomeIssueComponent();
        issue.setSeverity(OperationOutcome.IssueSeverity.valueOf(severityEnum.getSeverityCode()));
        issue.setCode(OperationOutcome.IssueType.INFORMATIONAL);
        CodeableConcept details = new CodeableConcept();
        Coding detailsCoding = new Coding();
        detailsCoding.setCode("MSG_CREATED");
        detailsCoding.setDisplay("New resource created");
        detailsCoding.setSystem("http://hl7.org/fhir/ValueSet/operation-outcome");
        detailsCoding.setVersion("4.0.1");
        details.addCoding(detailsCoding);
        details.setText("New resource created");
        issue.setDetails(details);
        // Create Outcome
        OperationOutcome outcome = new OperationOutcome();
        outcome.setId(resourceId);
        outcome.addIssue(issue);
        return(outcome);
    }

    public OperationOutcome generateResourceFailedAddOutcome(IdType resourceId, OperationOutcomeSeverityEnum severityEnum){
        // Create Issue
        OperationOutcome.OperationOutcomeIssueComponent issue = new OperationOutcome.OperationOutcomeIssueComponent();
        issue.setSeverity(OperationOutcome.IssueSeverity.valueOf(severityEnum.getSeverityCode()));
        issue.setCode(OperationOutcome.IssueType.EXCEPTION);
        CodeableConcept details = new CodeableConcept();
        Coding detailsCoding = new Coding();
        detailsCoding.setCode("MSG_BAD_SYNTAX");
        detailsCoding.setDisplay("Bad Syntax in " + resourceId);
        detailsCoding.setSystem("http://hl7.org/fhir/ValueSet/operation-outcome");
        detailsCoding.setVersion("4.0.1");
        details.addCoding(detailsCoding);
        details.setText("Bad Syntax in " + resourceId);
        issue.setDetails(details);
        // Create Outcome
        OperationOutcome outcome = new OperationOutcome();
        outcome.setId(resourceId);
        outcome.addIssue(issue);
        return(outcome);
    }
}
