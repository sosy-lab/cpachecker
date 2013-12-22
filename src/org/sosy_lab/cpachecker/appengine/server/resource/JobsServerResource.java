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
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.commons.fileupload.FileItemIterator;
import org.apache.commons.fileupload.FileItemStream;
import org.apache.commons.fileupload.FileUploadException;
import org.apache.commons.fileupload.servlet.ServletFileUpload;
import org.apache.commons.fileupload.util.Streams;
import org.restlet.ext.servlet.ServletUtils;
import org.restlet.ext.wadl.WadlServerResource;
import org.restlet.representation.Representation;
import org.sosy_lab.cpachecker.appengine.common.GAETaskQueueJobRunner;
import org.sosy_lab.cpachecker.appengine.common.JobRunner;
import org.sosy_lab.cpachecker.appengine.dao.JobDAO;
import org.sosy_lab.cpachecker.appengine.entity.Job;
import org.sosy_lab.cpachecker.appengine.entity.JobFile;
import org.sosy_lab.cpachecker.appengine.server.common.JobsResource;

import com.google.common.base.Splitter;
import com.googlecode.objectify.Key;
import com.googlecode.objectify.ObjectifyService;


public class JobsServerResource extends WadlServerResource implements JobsResource {

  private static final String DEFAULT_PROGRAM_NAME = "program.c";

  @Override
  public void createJobAndRedirectToJob(Representation input) {
    // TODO move key-gen into DAO
    Key<Job> jobKey = ObjectifyService.factory().allocateId(Job.class);
    Job job = new Job(jobKey.getId());
    Map<String, String> options = new HashMap<>();

    ServletFileUpload upload = new ServletFileUpload();
    try {
      FileItemIterator iter = upload.getItemIterator(ServletUtils.getRequest(getRequest()));
      while (iter.hasNext()) {
        FileItemStream item = iter.next();
        InputStream stream = item.openStream();
        String value = Streams.asString(stream);
        if (item.isFormField()) {
          switch (item.getFieldName()) {
          case "specification":
            job.setSpecification(value);
            break;
          case "configuration":
            job.setConfiguration(value);
            break;
          case "options[]":
            List<String> parts = Splitter.on('=').splitToList(value);
            options.put(parts.get(0), parts.get(1));
            break;
          case "programText":
            // TODO make this prettier
            JobFile program = new JobFile(DEFAULT_PROGRAM_NAME);
            program.setContent(value);
            program.setJob(job);
            JobDAO.save(program);
            job.setProgram(program);
            break;
          default:
            break;
          }
        } else {
          // TODO save file
        }
      }
    } catch (FileUploadException | IOException e) {
      e.printStackTrace();
      // TODO handle errors
    }

    // TODO validate!
    options.putAll(job.getDefaultOptions());
    job.setOptions(options);
    JobDAO.save(job);

    JobRunner jobRunner = new GAETaskQueueJobRunner();
    job = jobRunner.run(job);

    getResponse().redirectSeeOther("/jobs/"+job.getKeyString());
  }

}
