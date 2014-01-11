/*
 *  CPAchecker is a tool for configurable software verification.
 *  This file is part of CPAchecker.
 *
 *  Copyright (C) 2007-2014  Dirk Beyer
 *  All rights reserved.
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 *
 *
 *  CPAchecker web page:
 *    http://cpachecker.sosy-lab.org
 */
package org.sosy_lab.cpachecker.appengine.server;

import org.restlet.Context;
import org.restlet.Request;
import org.restlet.Response;
import org.restlet.data.Status;
import org.restlet.routing.Filter;

import com.google.appengine.api.capabilities.CapabilitiesService;
import com.google.appengine.api.capabilities.CapabilitiesServiceFactory;
import com.google.appengine.api.capabilities.Capability;
import com.google.appengine.api.capabilities.CapabilityStatus;


public class CapabilitiesFilter extends Filter {

  public CapabilitiesFilter(Context context) {
    super(context);
  }

  @Override
  protected int beforeHandle(Request request, Response response) {

    CapabilitiesService service = CapabilitiesServiceFactory.getCapabilitiesService();
    if (service.getStatus(Capability.DATASTORE).getStatus() == CapabilityStatus.DISABLED
        || service.getStatus(Capability.DATASTORE_WRITE).getStatus() == CapabilityStatus.DISABLED) {
      response.setStatus(Status.SERVER_ERROR_SERVICE_UNAVAILABLE, "The datastore is not available.");
      return STOP;
    }

    if (service.getStatus(Capability.TASKQUEUE).getStatus() == CapabilityStatus.DISABLED) {
      response.setStatus(Status.SERVER_ERROR_SERVICE_UNAVAILABLE, "The task queue is not available.");
      return STOP;
    }

    return CONTINUE;
  }
}
