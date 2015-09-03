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

import org.restlet.Restlet;
import org.restlet.ext.wadl.WadlApplication;
import org.restlet.routing.Router;
import org.sosy_lab.cpachecker.appengine.server.resource.RootServerResource;
import org.sosy_lab.cpachecker.appengine.server.resource.SettingsServerResource;
import org.sosy_lab.cpachecker.appengine.server.resource.TaskExecutorServerResource;
import org.sosy_lab.cpachecker.appengine.server.resource.TaskFileServerResource;
import org.sosy_lab.cpachecker.appengine.server.resource.TaskServerResource;
import org.sosy_lab.cpachecker.appengine.server.resource.TaskStatisticServerResource;
import org.sosy_lab.cpachecker.appengine.server.resource.TasksServerResource;
import org.sosy_lab.cpachecker.appengine.server.resource.TasksetServerResource;
import org.sosy_lab.cpachecker.appengine.server.resource.TasksetTasksServerResource;
import org.sosy_lab.cpachecker.appengine.util.ObjectifyRegistry;

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
    router.attach("/settings", SettingsServerResource.class);

    router.attach("/tasks", TasksServerResource.class);
    router.attach("/tasks/{taskKey}", TaskServerResource.class);
    router.attach("/tasks/{taskKey}/statistics", TaskStatisticServerResource.class);
    router.attach("/tasks/{taskKey}/files/{fileKey}", TaskFileServerResource.class);

    router.attach("/tasksets", TasksetServerResource.class);
    router.attach("/tasksets/{tasksetKey}", TasksetServerResource.class);
    router.attach("/tasksets/{tasksetKey}/tasks", TasksetTasksServerResource.class);

    router.attach("/workers/execute-task", TaskExecutorServerResource.class);

    CapabilitiesFilter capabilitiesFilter = new CapabilitiesFilter(getContext());
    capabilitiesFilter.setNext(router);

    return capabilitiesFilter;
  }

  static {
    ObjectifyRegistry.register();
  }
}
