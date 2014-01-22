/*
 *  CPAchecker is a tool for configurable software verification.
 *  This file is part of CPAchecker.
 *
 *  Copyright (C) 2007-2013  Dirk Beyer
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

import org.restlet.Restlet;
import org.restlet.ext.wadl.WadlApplication;
import org.restlet.routing.Router;
import org.sosy_lab.cpachecker.appengine.entity.Job;
import org.sosy_lab.cpachecker.appengine.entity.JobFile;
import org.sosy_lab.cpachecker.appengine.server.resource.JobFileServerResource;
import org.sosy_lab.cpachecker.appengine.server.resource.JobRunnerServerResource;
import org.sosy_lab.cpachecker.appengine.server.resource.JobServerResource;
import org.sosy_lab.cpachecker.appengine.server.resource.JobsServerResource;
import org.sosy_lab.cpachecker.appengine.server.resource.RootServerResource;

import com.googlecode.objectify.ObjectifyService;

import freemarker.log.Logger;

public class CPAcheckerApplication extends WadlApplication {

  @Override
  public Restlet createInboundRoot() {
//    getTunnelService().setExtensionsTunnel(true);
    getEncoderService().setEnabled(true);

    try {
      Logger.selectLoggerLibrary(Logger.LIBRARY_JAVA);
    } catch (ClassNotFoundException _) {
      // ignored because JUL logging will be available
    }

    Router router = new Router(getContext());

    // latest API
    router.attach("/", RootServerResource.class);
    router.attach("/jobs", JobsServerResource.class);
    router.attach("/jobs/{jobKey}", JobServerResource.class);
    router.attach("/jobs/{jobKey}/files/{fileKey}", JobFileServerResource.class);
    router.attach("/workers/run-job", JobRunnerServerResource.class);

    CapabilitiesFilter capabilitiesFilter = new CapabilitiesFilter(getContext());
    capabilitiesFilter.setNext(router);

    return capabilitiesFilter;
  }

  static {
    ObjectifyService.register(Job.class);
    ObjectifyService.register(JobFile.class);
  }
}
