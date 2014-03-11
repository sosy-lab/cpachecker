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
import java.io.InputStream;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;

import org.apache.commons.fileupload.FileItemIterator;
import org.apache.commons.fileupload.FileItemStream;
import org.apache.commons.fileupload.FileUploadException;
import org.apache.commons.fileupload.servlet.ServletFileUpload;
import org.apache.commons.fileupload.util.Streams;
import org.apache.commons.io.IOUtils;
import org.restlet.data.MediaType;
import org.restlet.data.Status;
import org.restlet.ext.servlet.ServletUtils;
import org.restlet.ext.wadl.WadlServerResource;
import org.restlet.representation.Representation;
import org.restlet.representation.StringRepresentation;
import org.sosy_lab.common.configuration.Configuration;
import org.sosy_lab.common.configuration.InvalidConfigurationException;
import org.sosy_lab.cpachecker.appengine.dao.TaskDAO;
import org.sosy_lab.cpachecker.appengine.entity.Task;
import org.sosy_lab.cpachecker.appengine.json.TaskMixinAnnotations;
import org.sosy_lab.cpachecker.appengine.server.GAETaskQueueTaskExecutor;
import org.sosy_lab.cpachecker.appengine.server.common.TasksResource;
import org.sosy_lab.cpachecker.appengine.util.DefaultOptions;
import org.sosy_lab.cpachecker.appengine.util.FreemarkerUtil;
import org.sosy_lab.cpachecker.appengine.util.TaskBuilder;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;


public class TasksServerResource extends WadlServerResource implements TasksResource {

  @Override
  public Representation createTaskFromHtml(Representation input) throws IOException {
    List<String> errors = new LinkedList<>();
    Map<String, Object> settings = new HashMap<>();
    Map<String, String> options = new HashMap<>();

    ServletFileUpload upload = new ServletFileUpload();
    try {
      FileItemIterator iter = upload.getItemIterator(ServletUtils.getRequest(getRequest()));
      while (iter.hasNext()) {
        FileItemStream item = iter.next();
        InputStream stream = item.openStream();
        if (item.isFormField()) {
          String value = Streams.asString(stream);
          switch (item.getFieldName()) {
          case "specification":
            value = (value.equals("")) ? null : value;
            settings.put("specification", value);
            break;
          case "configuration":
            value = (value.equals("")) ? null : value;
            settings.put("configuration", value);
            break;
          case "disableOutput":
            options.put("output.disable", "true");
            break;
          case "disableExportStatistics":
            options.put("statistics.export", "false");
            break;
          case "dumpConfig":
            options.put("configuration.dumpFile", "UsedConfiguration.properties");
            break;
          case "logLevel":
            options.put("log.level", value);
            break;
          case "machineModel":
            options.put("analysis.machineModel", value);
            break;
          case "wallTime":
            options.put("limits.time.wall", value);
            break;
          case "instanceType":
            options.put("gae.instanceType", value);
            break;
          case "programText":
            if (!value.isEmpty()) {
              settings.put("programName", "program.c");
              settings.put("programText", value);
            }
            break;
          }
        }
        else {
          if (settings.get("programText") == null) {
            settings.put("programName", item.getName());
            settings.put("programText", IOUtils.toString(stream));
          }
        }
      }
    } catch (FileUploadException | IOException e) {
      getLogger().log(Level.WARNING, "Could not upload program file.", e);
      errors.add("task.program.CouldNotUpload");
    }

    settings.put("options", options);

    Task task = null;
    if (errors.isEmpty()) {
      TaskBuilder taskBuilder = new TaskBuilder();
      task = taskBuilder.fromMap(settings);
      errors = taskBuilder.getErrors();
    }

    if (errors.isEmpty()) {
      try {
        Configuration config = Configuration.builder()
            .setOptions(task.getOptions()).build();
        new GAETaskQueueTaskExecutor(config).execute(task);
      } catch (InvalidConfigurationException e) {
        errors.add("error.invalidConfiguration");
      }
    }

    if (errors.isEmpty()) {
      getResponse().setStatus(Status.SUCCESS_CREATED);
      redirectSeeOther("/tasks/" + task.getKey());
      return getResponseEntity();
    }

    getResponse().setStatus(Status.CLIENT_ERROR_BAD_REQUEST);
    return FreemarkerUtil.templateBuilder()
        .context(getContext())
        .addData("task", task)
        .addData("errors", errors)
        .addData("allowedOptions", DefaultOptions.getDefaultOptions())
        .addData("defaultOptions", DefaultOptions.getImmutableOptions())
        .addData("specifications", DefaultOptions.getSpecifications())
        .addData("configurations", DefaultOptions.getConfigurations())
        .templateName("root.ftl")
        .build();
  }

  @Override
  public Representation tasksAsHtml() {
    List<Task> tasks = TaskDAO.tasks();
    return FreemarkerUtil.templateBuilder()
        .context(getContext())
        .templateName("tasks.ftl")
        .addData("tasks", tasks)
        .build();
  }

  @Override
  public Representation createTaskFromJson(Representation entity) {
    TaskBuilder taskBuilder = new TaskBuilder();
    List<String> errors = new LinkedList<>();
    Task task = null;
    try {
      task = taskBuilder.fromJson(entity.getStream());
      errors = taskBuilder.getErrors();
    } catch (IOException e) {
      errors.add("error.requestBodyNotRead");
    }

    if (errors.isEmpty()) {
      try {
        Configuration config = Configuration.builder()
            .setOptions(task.getOptions()).build();
        new GAETaskQueueTaskExecutor(config).execute(task);
      } catch (InvalidConfigurationException e) {
        errors.add("error.invalidConfiguration");
      }
    }

    if (!errors.isEmpty()) {
      getResponse().setStatus(Status.CLIENT_ERROR_BAD_REQUEST);
      ObjectMapper mapper = new ObjectMapper();
      try {
        return new StringRepresentation(mapper.writeValueAsString(errors), MediaType.APPLICATION_JSON);
      } catch (JsonProcessingException e) {
        getResponse().setStatus(Status.SERVER_ERROR_INTERNAL);
        return getResponseEntity();
      }
    }

    getResponse().setStatus(Status.SUCCESS_CREATED);
    getResponse().setLocationRef("/tasks/" + task.getKey());
    return new StringRepresentation(task.getKey());
  }

  @Override
  public Representation tasksAsJson() {
    ObjectMapper mapper = new ObjectMapper();
    mapper.enable(SerializationFeature.INDENT_OUTPUT);
    mapper.addMixInAnnotations(Task.class, TaskMixinAnnotations.Minimal.class);

    try {
      return new StringRepresentation(mapper.writeValueAsString(TaskDAO.tasks()), MediaType.APPLICATION_JSON);
    } catch (JsonProcessingException e) {
      return null;
    }
  }

  @Override
  public void deleteAll() {
    TaskDAO.deleteAll();
  }
}
