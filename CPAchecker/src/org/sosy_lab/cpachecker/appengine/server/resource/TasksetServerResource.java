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
package org.sosy_lab.cpachecker.appengine.server.resource;

import org.restlet.data.MediaType;
import org.restlet.data.Status;
import org.restlet.ext.wadl.WadlServerResource;
import org.restlet.representation.Representation;
import org.restlet.representation.StringRepresentation;
import org.sosy_lab.cpachecker.appengine.dao.TasksetDAO;
import org.sosy_lab.cpachecker.appengine.entity.Taskset;
import org.sosy_lab.cpachecker.appengine.server.common.TasksetResource;


public class TasksetServerResource extends WadlServerResource implements TasksetResource {

  @Override
  public Representation createTaskset() {
    Taskset taskset = new Taskset();
    TasksetDAO.save(taskset);

    getResponse().setStatus(Status.SUCCESS_CREATED);
    getResponse().setLocationRef("/taskset/" + taskset.getKey());
    return new StringRepresentation(taskset.getKey(), MediaType.APPLICATION_JSON);
  }

  @Override
  public void deleteTaskset() {
    Taskset taskset = TasksetDAO.load(getAttribute("tasksetKey"));
    if (taskset != null) {
      TasksetDAO.delete(taskset);
      getResponse().setStatus(Status.SUCCESS_OK);
    } else {
      getResponse().setStatus(Status.CLIENT_ERROR_NOT_FOUND);
    }
  }
}
