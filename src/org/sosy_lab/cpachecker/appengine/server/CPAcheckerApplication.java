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

import java.io.IOException;

import org.restlet.Restlet;
import org.restlet.ext.wadl.WadlApplication;
import org.restlet.routing.Router;
import org.sosy_lab.common.io.Paths;
import org.sosy_lab.cpachecker.appengine.entity.Job;
import org.sosy_lab.cpachecker.appengine.entity.JobFile;
import org.sosy_lab.cpachecker.appengine.server.resource.JobFileServerResource;
import org.sosy_lab.cpachecker.appengine.server.resource.JobRunnerServerResource;
import org.sosy_lab.cpachecker.appengine.server.resource.JobServerResource;
import org.sosy_lab.cpachecker.appengine.server.resource.JobStatisticServerResource;
import org.sosy_lab.cpachecker.appengine.server.resource.JobsServerResource;
import org.sosy_lab.cpachecker.appengine.server.resource.RootServerResource;
import org.sosy_lab.cpachecker.appengine.server.resource.SettingsServerResource;

import com.google.common.base.Charsets;
import com.googlecode.objectify.ObjectifyService;

import freemarker.log.Logger;

public class CPAcheckerApplication extends WadlApplication {


  @Override
  public Restlet createInboundRoot() {
    getEncoderService().setEnabled(true);

    try {
      Logger.selectLoggerLibrary(Logger.LIBRARY_JAVA);
    } catch (ClassNotFoundException _) {
      // ignored because JUL logging is available
    }

    Router router = new Router(getContext());

    router.attach("/", RootServerResource.class);
    router.attach("/tasks", JobsServerResource.class);
    router.attach("/tasks/{jobKey}", JobServerResource.class);
    router.attach("/tasks/{jobKey}/statistics", JobStatisticServerResource.class);
    router.attach("/tasks/{jobKey}/files/{fileKey}", JobFileServerResource.class);
    router.attach("/workers/run-job", JobRunnerServerResource.class);
    router.attach("/settings", SettingsServerResource.class);

    CapabilitiesFilter capabilitiesFilter = new CapabilitiesFilter(getContext());
    capabilitiesFilter.setNext(router);

    return capabilitiesFilter;
  }

  /**
   * Returns the version of CPAchecker on Google App Engine
   *
   * @return The version string
   */
  public static String getVersion() {
    try {
      return Paths.get("WEB-INF/VERSION.txt").asCharSource(Charsets.UTF_8).read();
    } catch (IOException e) {
      return "(unknown version)";
    }
  }

  static {
    ObjectifyService.register(Job.class);
    ObjectifyService.register(JobFile.class);
  }
}
