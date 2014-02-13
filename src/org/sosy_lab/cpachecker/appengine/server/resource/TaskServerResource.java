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

import java.util.List;

import org.restlet.data.MediaType;
import org.restlet.data.Status;
import org.restlet.ext.wadl.WadlServerResource;
import org.restlet.representation.Representation;
import org.restlet.representation.StringRepresentation;
import org.restlet.representation.Variant;
import org.restlet.resource.ResourceException;
import org.sosy_lab.cpachecker.appengine.common.FreemarkerUtil;
import org.sosy_lab.cpachecker.appengine.dao.TaskDAO;
import org.sosy_lab.cpachecker.appengine.entity.Task;
import org.sosy_lab.cpachecker.appengine.entity.TaskFile;
import org.sosy_lab.cpachecker.appengine.entity.TaskStatistic;
import org.sosy_lab.cpachecker.appengine.json.TaskFileMixinAnnotations;
import org.sosy_lab.cpachecker.appengine.json.TaskMixinAnnotations;
import org.sosy_lab.cpachecker.appengine.json.TaskStatisticMixinAnnotations;
import org.sosy_lab.cpachecker.appengine.server.common.TaskResource;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;


public class TaskServerResource extends WadlServerResource implements TaskResource {

  private Task task;

  @Override
  protected void doInit() throws ResourceException {
    super.doInit();
    task = TaskDAO.load(getAttribute("taskKey"));

    if (task == null) {
      getResponse().setStatus(Status.CLIENT_ERROR_NOT_FOUND);
      getResponse().commit();
    }
  }

  @Override
  public Representation taskAsHtml() {
    List<TaskFile> files = task.getFilesLoaded();

    return FreemarkerUtil.templateBuilder()
        .context(getContext())
        .addData("task", task)
        .addData("files", files)
        .templateName("task.ftl")
        .build();
  }

  @Override
  public Representation deleteTask(Variant variant) {
    TaskDAO.delete(task);
    getResponse().setStatus(Status.SUCCESS_OK);

    // only send redirect if it is a browser call
    if (variant == null || !variant.getMediaType().equals(MediaType.APPLICATION_JSON)) {
      getResponse().redirectSeeOther("/tasks");
    }
    return getResponseEntity();
  }

  @Override
  public Representation taskAsJson() {
    ObjectMapper mapper = new ObjectMapper();
    mapper.enable(SerializationFeature.INDENT_OUTPUT);
    mapper.addMixInAnnotations(Task.class, TaskMixinAnnotations.Full.class);
    mapper.addMixInAnnotations(TaskStatistic.class, TaskStatisticMixinAnnotations.Full.class);
    mapper.addMixInAnnotations(TaskFile.class, TaskFileMixinAnnotations.Minimal.class);

    try {
      return new StringRepresentation(mapper.writeValueAsString(task), MediaType.APPLICATION_JSON);
    } catch (JsonProcessingException e) {
      return null;
    }
  }

}
