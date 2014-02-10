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
import java.io.StringWriter;
import java.util.ArrayList;
import java.util.HashMap;
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
import org.sosy_lab.cpachecker.appengine.common.FreemarkerUtil;
import org.sosy_lab.cpachecker.appengine.common.GAETaskQueueJobRunner;
import org.sosy_lab.cpachecker.appengine.common.GAETaskQueueJobRunner.InstanceType;
import org.sosy_lab.cpachecker.appengine.common.JobRunner;
import org.sosy_lab.cpachecker.appengine.dao.JobDAO;
import org.sosy_lab.cpachecker.appengine.dao.JobFileDAO;
import org.sosy_lab.cpachecker.appengine.entity.DefaultOptions;
import org.sosy_lab.cpachecker.appengine.entity.Job;
import org.sosy_lab.cpachecker.appengine.entity.JobFile;
import org.sosy_lab.cpachecker.appengine.json.JobMixinAnnotations;
import org.sosy_lab.cpachecker.appengine.json.JobsResourceJSONModule;
import org.sosy_lab.cpachecker.appengine.server.common.JobsResource;

import com.fasterxml.jackson.core.JsonParseException;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.google.apphosting.api.ApiProxy.RequestTooLargeException;
import com.google.common.base.Charsets;


public class JobsServerResource extends WadlServerResource implements JobsResource {

  private static String DEFAULT_FILENAME = "program.c";

  private Job createdJob = null;

  @Override
  public Representation createJobFromHtml(Representation input) throws IOException {
    DefaultOptions options = new DefaultOptions();
    Map<String, Object> settings = new HashMap<>();
    List<String> errors = new ArrayList<>();
    String program = null;

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
            options.setOption("output.disable", "true");
            break;
          case "disableExportStatistics":
            options.setOption("statistics.export", "false");
            break;
          case "dumpConfig":
            options.setOption("configuration.dumpFile", "UsedConfiguration.properties");
            break;
          case "logLevel":
            options.setOption("log.level", value);
            break;
          case "machineModel":
            options.setOption("analysis.machineModel", value);
            break;
          case "wallTime":
            options.setOption("limits.time.wall", value);
            break;
          case "instanceType":
            options.setOption("gae.instanceType", value);
            break;
          case "programText":
            if (program == null || program.isEmpty()) {
              settings.put("sourceFileName", DEFAULT_FILENAME);
              program = value;
            }
            break;
          }
        }
        else {
          if (program == null || program.isEmpty()) {
            settings.put("sourceFileName", item.getName());
            // files will always be treated as text/plain
            StringWriter writer = new StringWriter();
            IOUtils.copy(stream, writer, Charsets.UTF_8);
            program = writer.toString();
          }
        }
      }
    } catch (FileUploadException | IOException e) {
      getLogger().log(Level.WARNING, "Could not upload program file.", e);

      errors.add("error.couldNotUpload");
      settings.put("errors", errors);
    }

    settings.put("programText", program);
    settings.put("options", options.getUsedOptions());

    errors = createJob(settings);

    if (errors.size() == 0) {
      getResponse().setStatus(Status.SUCCESS_CREATED);
      redirectSeeOther("/tasks/" + createdJob.getKey());
      return getResponseEntity();
    }

    getResponse().setStatus(Status.CLIENT_ERROR_BAD_REQUEST);
    return FreemarkerUtil.templateBuilder()
        .context(getContext())
        .addData("job", createdJob)
        .addData("errors", errors)
        .addData("allowedOptions", DefaultOptions.getDefaultOptions())
        .addData("defaultOptions", DefaultOptions.getImmutableOptions())
        .addData("specifications", DefaultOptions.getSpecifications())
        .addData("configurations", DefaultOptions.getConfigurations())
        .templateName("root.ftl")
        .build();
  }

  @Override
  public Representation jobsAsHtml() {
    List<Job> jobs = JobDAO.jobs();
    return FreemarkerUtil.templateBuilder()
        .context(getContext())
        .templateName("jobs.ftl")
        .addData("jobs", jobs)
        .build();
  }

  @Override
  public Representation createJobFromJson(Representation entity) {
    ObjectMapper mapper = new ObjectMapper();
    mapper.registerModule(new JobsResourceJSONModule());
    List<String> errors = new ArrayList<>();
    Map<String, Object> settings = new HashMap<>();
    try {
      if (entity != null) {
        settings = mapper.readValue(entity.getStream(), new TypeReference<Map<String, Object>>() {});
      }
    } catch (JsonParseException e) {
      errors.add("error.jsonNotWellFormed");
    } catch (JsonMappingException e) {
      errors.add("error.jsonNotMapped");
    } catch (IOException e) {
      errors.add("error.requestBodyNotRead");
    }

    // do not attempt to create job if there are no useful settings
    if (errors.size() == 0) {
      errors = createJob(settings);
    }

    try {
      if (errors.size() > 0) {
        getResponse().setStatus(Status.CLIENT_ERROR_BAD_REQUEST);
        return new StringRepresentation(mapper.writeValueAsString(errors), MediaType.APPLICATION_JSON);
      } else {
        getResponse().setStatus(Status.SUCCESS_CREATED);
        getResponse().setLocationRef("/tasks/" + createdJob.getKey());
        return new StringRepresentation(mapper.writeValueAsString(createdJob), MediaType.APPLICATION_JSON);
      }
    } catch (JsonProcessingException e) {
      return null;
    }
  }

  @Override
  public Representation jobsAsJson() {
    ObjectMapper mapper = new ObjectMapper();
    mapper.enable(SerializationFeature.INDENT_OUTPUT);
    mapper.addMixInAnnotations(Job.class, JobMixinAnnotations.Minimal.class);

    try {
      return new StringRepresentation(mapper.writeValueAsString(JobDAO.jobs()), MediaType.APPLICATION_JSON);
    } catch (JsonProcessingException e) {
      return null;
    }
  }

  /**
   * Creates a new job from the given settings.
   *
   * @param settings The settings.
   * @return A list of errors if any occurred, an empty list otherwise.
   */
  @SuppressWarnings("unchecked")
  private List<String> createJob(Map<String, Object> settings) {
    // merge existing errors
    List<String> errors = new ArrayList<>();
    if (settings.get("errors") != null) {
      errors.addAll((List<String>) settings.get("errors"));
    }

    createdJob = new Job(JobDAO.allocateKey().getId());
    createdJob.setSpecification((String) settings.get("specification"));
    createdJob.setConfiguration((String) settings.get("configuration"));
    if (settings.get("options") != null) {
      createdJob.setOptions((Map<String, String>) settings.get("options"));
    }

    String fileName = (String) settings.get("sourceFileName");
    fileName = (fileName == null || fileName.equals("")) ? DEFAULT_FILENAME : fileName;
    createdJob.setSourceFileName(fileName);
    JobFile program = new JobFile(createdJob.getSourceFileName(), createdJob);
    program.setContent((String) settings.get("programText"));

    if (createdJob.getSpecification() == null && createdJob.getConfiguration() == null) {
      errors.add("error.specOrConfigMissing");
    }

    if (createdJob.getSpecification() != null) {
      if (!DefaultOptions.getSpecifications().contains(createdJob.getSpecification())) {
        errors.add("error.specificationNotFound");
      }
    }

    if (createdJob.getConfiguration() != null) {
      try {
        if (!DefaultOptions.getConfigurations().contains(createdJob.getConfiguration())) {
          errors.add("error.configurationNotFound");
        }
      } catch (IOException e) {
        errors.add("error.configurationNotFound");
      }
    }

    if (program.getContent() == null || program.getContent().equals("")) {
      errors.add("error.noProgram");
    }

    if (createdJob.getOptions().containsKey("log.level")) {
      try {
        Level.parse(createdJob.getOptions().get("log.level"));
      } catch (IllegalArgumentException e) {
        errors.add("error.invalidLogLevel");
      }
    }

    if (createdJob.getOptions().containsKey("gae.instanceType")) {
      try {
        InstanceType.valueOf(createdJob.getOptions().get("gae.instanceType"));
      } catch (IllegalArgumentException e) {
        errors.add("error.invalidInstanceType");
      }
    }

    if (errors.size() == 0) {
      try {
        JobFileDAO.save(program);
        createdJob.addFile(program);
        JobDAO.save(createdJob);
      } catch (IOException e) {
        if (e.getCause() instanceof RequestTooLargeException) {
          errors.add("error.programTooLarge");
        } else {
          errors.add("error.couldNotUpload");
        }
      }
    }

    if (errors.size() == 0) {
      try {
        Configuration config = Configuration.builder()
            .setOptions(createdJob.getOptions())
            .build();
        JobRunner jobRunner = new GAETaskQueueJobRunner(config);
        jobRunner.run(createdJob);
      } catch (InvalidConfigurationException e) {
        errors.add("error.invalidConfiguration");
      }
    }

    return errors;
  }

  @Override
  public void deleteAll() {
    JobDAO.deleteAll();
  }
}
