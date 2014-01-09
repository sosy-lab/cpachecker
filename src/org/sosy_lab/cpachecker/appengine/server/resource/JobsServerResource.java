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
package org.sosy_lab.cpachecker.appengine.server.resource;

import java.io.IOException;
import java.io.InputStream;
import java.io.StringWriter;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.commons.fileupload.FileItemIterator;
import org.apache.commons.fileupload.FileItemStream;
import org.apache.commons.fileupload.FileUploadException;
import org.apache.commons.fileupload.servlet.ServletFileUpload;
import org.apache.commons.fileupload.util.Streams;
import org.apache.commons.io.IOUtils;
import org.restlet.ext.servlet.ServletUtils;
import org.restlet.ext.wadl.WadlServerResource;
import org.restlet.representation.Representation;
import org.sosy_lab.cpachecker.appengine.common.FreemarkerUtil;
import org.sosy_lab.cpachecker.appengine.common.GAETaskQueueJobRunner;
import org.sosy_lab.cpachecker.appengine.common.JobRunner;
import org.sosy_lab.cpachecker.appengine.dao.JobDAO;
import org.sosy_lab.cpachecker.appengine.dao.JobFileDAO;
import org.sosy_lab.cpachecker.appengine.entity.Job;
import org.sosy_lab.cpachecker.appengine.entity.JobFile;
import org.sosy_lab.cpachecker.appengine.server.common.JobsResource;

import com.google.common.base.Charsets;


public class JobsServerResource extends WadlServerResource implements JobsResource {

  @Override
  public void createJobAndRedirectToJob(Representation input) {
    Job job = new Job(JobDAO.allocateKey().getId());
    JobFile program = new JobFile("program.c", job);

    Map<String, String> options = new HashMap<>();
    options.put("output.disable", "true");
    options.put("statistics.export", "false");
    options.put("log.usedOptions.export", "false");


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
          case "enableOutput":
            options.put("output.disable", "false");
            break;
          case "exportStatistics":
            options.put("statistics.export", "true");
            break;
          case "logUsedOptions":
            options.put("log.usedOptions.export", "true");
            break;
          case "logLevel":
            options.put("log.level", value);
            break;
          case "programText":
            if (program.getContent() == null || program.getContent().isEmpty()) {
              program.setContent(value);
            }
            break;
          default:
            break;
          }
        }
        else {
          if (program.getContent() == null || program.getContent().isEmpty()) {
            // files will always be treated as text/plain
            StringWriter writer = new StringWriter();
            IOUtils.copy(stream, writer, Charsets.UTF_8);
            program.setContent(writer.toString());
          }
        }
      }
    } catch (FileUploadException | IOException e) {
      e.printStackTrace();
      // TODO handle errors
    }

    JobFileDAO.save(program);
    job.addFile(program);

    // TODO validate!

    // TODO move into validation
    if (job.getSpecification() == null && job.getConfiguration() == null) {
      // TODO error: at least spec or config
    }

    job.setOptions(options);
    JobDAO.save(job);

    JobRunner jobRunner = new GAETaskQueueJobRunner();
    job = jobRunner.run(job);

    getResponse().redirectSeeOther("/jobs/"+job.getKey());
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

}
