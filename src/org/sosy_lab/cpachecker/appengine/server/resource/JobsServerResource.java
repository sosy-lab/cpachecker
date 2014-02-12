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
import org.sosy_lab.cpachecker.appengine.dao.JobDAO;
import org.sosy_lab.cpachecker.appengine.entity.DefaultOptions;
import org.sosy_lab.cpachecker.appengine.entity.Job;
import org.sosy_lab.cpachecker.appengine.entity.JobFile;
import org.sosy_lab.cpachecker.appengine.json.JobFileMixinAnnotations;
import org.sosy_lab.cpachecker.appengine.json.JobMixinAnnotations;
import org.sosy_lab.cpachecker.appengine.server.common.JobsResource;

import com.fasterxml.jackson.core.JsonParseException;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.google.common.base.Charsets;


public class JobsServerResource extends WadlServerResource implements JobsResource {

  @Override
  public Representation createJobFromHtml(Representation input) throws IOException {
    Map<String, String> options = new HashMap<>();
    Job job = new Job();
    JobFile program = new JobFile();
    List<String> errors = new ArrayList<>();

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
            job.setSpecification(value);
            break;
          case "configuration":
            value = (value.equals("")) ? null : value;
            job.setConfiguration(value);
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
            if (program.getContent().isEmpty()) {
              program.setPath("program.c");
              program.setContent(value);
            }
            break;
          }
        }
        else {
          if (program.getContent().isEmpty()) {
            // files will always be treated as text/plain
            StringWriter writer = new StringWriter();
            IOUtils.copy(stream, writer, Charsets.UTF_8);
            program.setPath(item.getName());
            program.setContent(writer.toString());
          }
        }
      }
    } catch (FileUploadException | IOException e) {
      getLogger().log(Level.WARNING, "Could not upload program file.", e);
      errors.add("error.couldNotUpload");
    }

    job.setOptions(options);

    if (errors.size() == 0) {
      errors = JobDAO.create(job, program);
    }

    if (errors != null && errors.size() == 0) {
      try {
        Configuration config = Configuration.builder()
            .setOptions(job.getOptions()).build();
        new GAETaskQueueJobRunner(config).run(job);
      } catch (InvalidConfigurationException e) {
        errors.add("error.invalidConfiguration");
      }
    }

    if (errors.size() == 0) {
      getResponse().setStatus(Status.SUCCESS_CREATED);
      redirectSeeOther("/tasks/" + job.getKey());
      return getResponseEntity();
    }

    getResponse().setStatus(Status.CLIENT_ERROR_BAD_REQUEST);
    return FreemarkerUtil.templateBuilder()
        .context(getContext())
        .addData("job", job)
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
    mapper.addMixInAnnotations(Job.class, JobMixinAnnotations.FromJSONAPI.class);
    mapper.addMixInAnnotations(JobFile.class, JobFileMixinAnnotations.FromJSONAPI.class);

    List<String> errors = new ArrayList<>();
    Job job = new Job();
    JobFile program = new JobFile();
    try {
      if (entity != null) {
        String json = entity.getText();
        job = mapper.readValue(json, Job.class);
        program = mapper.readValue(json, JobFile.class);
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
      errors = JobDAO.create(job, program);
    }

    if (errors != null && errors.size() == 0) {
      try {
        Configuration config = Configuration.builder()
            .setOptions(job.getOptions()).build();
        new GAETaskQueueJobRunner(config).run(job);
      } catch (InvalidConfigurationException e) {
        errors.add("error.invalidConfiguration");
      }
    }

    try {
      if (errors.size() > 0) {
        getResponse().setStatus(Status.CLIENT_ERROR_BAD_REQUEST);
        return new StringRepresentation(mapper.writeValueAsString(errors), MediaType.APPLICATION_JSON);
      } else {
        getResponse().setStatus(Status.SUCCESS_CREATED);
        getResponse().setLocationRef("/tasks/" + job.getKey());
        return getResponseEntity();
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

  @Override
  public void deleteAll() {
    JobDAO.deleteAll();
  }
}
