/*
 * Copyright 2020-2022 University of Oxford and Health and Social Care Information Centre, also known as NHS Digital
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 * SPDX-License-Identifier: Apache-2.0
 */
package uk.ac.ox.softeng.maurodatamapper.plugins.research.rest.transport

import grails.validation.Validateable
import org.owasp.html.HtmlPolicyBuilder
import org.owasp.html.PolicyFactory

class Contact implements Validateable {

    PolicyFactory policy = new HtmlPolicyBuilder().toFactory()

    String firstName
    String lastName
    String organisation
    String subject
    String message
    String emailAddress

    static constraints = {
        firstName nullable: true
        lastName nullable: true
        organisation nullable: true
        subject nullable: true
        message nullable: true
        emailAddress nullable: true
    }

    void setFirstName(String firstName) {
        this.firstName = policy.sanitize(firstName)
    }

    void setLastName(String lastName) {
        this.lastName = policy.sanitize(lastName)
    }

    void setOrganisation(String organisation) {
        this.organisation = policy.sanitize(organisation)
    }

    void setSubject(String subject) {
        this.subject = policy.sanitize(subject)
    }

    void setMessage(String message) {
        this.message = policy.sanitize(message)
    }

    void setEmailAddress(String emailAddress) {
        this.emailAddress = policy.sanitize(emailAddress)
    }
}
