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

import java.io.IOException;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import org.restlet.data.MediaType;
import org.restlet.data.Status;
import org.restlet.ext.wadl.WadlServerResource;
import org.restlet.representation.Representation;
import org.restlet.representation.StringRepresentation;
import org.restlet.resource.ResourceException;
import org.sosy_lab.common.configuration.Configuration;
import org.sosy_lab.common.configuration.InvalidConfigurationException;
import org.sosy_lab.cpachecker.appengine.dao.TaskDAO;
import org.sosy_lab.cpachecker.appengine.dao.TasksetDAO;
import org.sosy_lab.cpachecker.appengine.entity.Task;
import org.sosy_lab.cpachecker.appengine.entity.TaskFile;
import org.sosy_lab.cpachecker.appengine.entity.TaskStatistic;
import org.sosy_lab.cpachecker.appengine.entity.Taskset;
import org.sosy_lab.cpachecker.appengine.json.TaskFileMixinAnnotations;
import org.sosy_lab.cpachecker.appengine.json.TaskMixinAnnotations;
import org.sosy_lab.cpachecker.appengine.json.TaskStatisticMixinAnnotations;
import org.sosy_lab.cpachecker.appengine.server.TaskQueueTaskRunner;
import org.sosy_lab.cpachecker.appengine.server.common.TasksetTasksResource;
import org.sosy_lab.cpachecker.appengine.util.TaskBuilder;

import com.fasterxml.jackson.core.JsonParseException;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.ObjectMapper;


public class TasksetTasksServerResource extends WadlServerResource implements TasksetTasksResource {

  private Taskset taskset;

  @Override
  protected void doInit() throws ResourceException {
    super.doInit();
    taskset = TasksetDAO.load(getAttribute("tasksetKey"));

    if (taskset == null) {
      getResponse().setStatus(Status.CLIENT_ERROR_NOT_FOUND);
      getResponse().commit();
    }
  }

  @SuppressWarnings("unchecked")
  @Override
  public Representation createTasksFromJson(Representation entity) {
    TaskBuilder taskBuilder = new TaskBuilder();
    List<String> errors = new LinkedList<>();
    Map<Task, String> tasks = null;
    try {
      tasks = taskBuilder.fromJsonList(entity.getStream());
      errors = taskBuilder.getErrors();
    } catch (IOException e) {
      errors.add("error.requestBodyNotRead");
    }

    Map<String, String> taskKeys = new HashMap<>();
    if (errors.isEmpty()) {
      for (Entry<Task, String> taskPair : tasks.entrySet()) {
        Task task = taskPair.getKey();
        try {
          Configuration config = Configuration.builder()
              .setOptions(task.getOptions()).build();
          new TaskQueueTaskRunner(config).run(task);

          taskKeys.put(task.getKey(), taskPair.getValue());
          taskset.addTask(task);
        } catch (InvalidConfigurationException e) {
          // nothing to do about it
        }
      }
    }

    ObjectMapper mapper = new ObjectMapper();
    try {
      if (!errors.isEmpty()) {
        getResponse().setStatus(Status.CLIENT_ERROR_BAD_REQUEST);
        return new StringRepresentation(mapper.writeValueAsString(errors), MediaType.APPLICATION_JSON);
      }

      TasksetDAO.save(taskset);

      getResponse().setStatus(Status.SUCCESS_CREATED);
      return new StringRepresentation(mapper.writeValueAsString(taskKeys), MediaType.APPLICATION_JSON);
    } catch (JsonProcessingException e) {
      getResponse().setStatus(Status.SERVER_ERROR_INTERNAL);
      return getResponseEntity();
    }
  }

  @Override
  public Representation markTasksAsProcessed(Representation entity) {

    ObjectMapper mapper = new ObjectMapper();

    List<String> errors = new LinkedList<>();
    List<String> keys = null;
    try {
      if (entity != null) {
        keys = mapper.readValue(entity.getStream(), new TypeReference<List<String>>() {});
      }
    } catch (JsonParseException e) {
      errors.add("error.jsonNotWellFormed");
    } catch (JsonMappingException e) {
      errors.add("error.jsonNotMapped");
    } catch (IOException e) {
      errors.add("error.requestBodyNotRead");
    }

    try {
      if (!errors.isEmpty()) {
        getResponse().setStatus(Status.CLIENT_ERROR_BAD_REQUEST);
        return new StringRepresentation(mapper.writeValueAsString(errors), MediaType.APPLICATION_JSON);
      }

      List<Task> tasks = TaskDAO.load(keys);
      for (Task task : tasks) {
        task.setProcessed(true);
      }
      TaskDAO.save(tasks);
      getResponse().setStatus(Status.SUCCESS_NO_CONTENT);
    } catch (JsonProcessingException e) {
      getResponse().setStatus(Status.SERVER_ERROR_INTERNAL);
    }

    return getResponseEntity();
  }

  @Override
  public Representation getTasks() {

    int limit = -1;
    if (getQueryValue("limit") != null) {
      try {
        limit = Integer.parseInt(getQueryValue("limit"));
      } catch (NumberFormatException e) {
        limit = -1;
      }
    }

    List<Task> allTasks = null;

    if (taskset.getTasks() != null && !taskset.getTasks().isEmpty()) {
      if (getQueryValue("finished") != null) {
        if (getQueryValue("finished").equals("true")) {
         allTasks = TaskDAO.finishedTasks(taskset, limit);
        } else if (getQueryValue("finished").equals("false")) {
          allTasks = TaskDAO.unfinishedTasks(taskset, limit);
         }
      }
    }

    // TODO add support for processed/unprocessed

    if (allTasks == null) {
      allTasks = TaskDAO.load(taskset.getTasks());
    }

    ObjectMapper mapper = new ObjectMapper();
    mapper.addMixInAnnotations(Task.class, TaskMixinAnnotations.Full.class);
    mapper.addMixInAnnotations(TaskStatistic.class, TaskStatisticMixinAnnotations.Full.class);
    mapper.addMixInAnnotations(TaskFile.class, TaskFileMixinAnnotations.Minimal.class);

    try {
      getResponse().setStatus(Status.SUCCESS_OK);
      return new StringRepresentation(mapper.writeValueAsString(allTasks), MediaType.APPLICATION_JSON);
    } catch (JsonProcessingException e) {
      getResponse().setStatus(Status.SERVER_ERROR_INTERNAL);
      return getResponseEntity();
    }
  }
}
