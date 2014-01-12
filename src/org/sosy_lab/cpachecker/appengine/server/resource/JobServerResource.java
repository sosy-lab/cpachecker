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

import java.util.List;

import org.restlet.data.MediaType;
import org.restlet.data.Status;
import org.restlet.ext.wadl.WadlServerResource;
import org.restlet.representation.Representation;
import org.restlet.representation.Variant;
import org.sosy_lab.cpachecker.appengine.common.FreemarkerUtil;
import org.sosy_lab.cpachecker.appengine.dao.JobDAO;
import org.sosy_lab.cpachecker.appengine.entity.Job;
import org.sosy_lab.cpachecker.appengine.entity.JobFile;
import org.sosy_lab.cpachecker.appengine.server.common.JobResource;


public class JobServerResource extends WadlServerResource implements JobResource {

  @Override
  public Representation jobAsHtml() {
    Job job = JobDAO.load(getAttribute("jobKey"));
    List<JobFile> files = job.getFilesLoaded();

    return FreemarkerUtil.templateBuilder()
        .context(getContext())
        .addData("job", job)
        .addData("files", files)
        .templateName("job.ftl")
        .build();
  }

  @Override
  public Representation deleteJob(Variant variant) {
    JobDAO.delete(getAttribute("jobKey"));
    getResponse().setStatus(Status.SUCCESS_OK);

    // only send redirect if it is a browser call
    if (variant == null || !variant.getMediaType().equals(MediaType.APPLICATION_JSON)) {
      getResponse().redirectSeeOther("/jobs");
    }
    return getResponseEntity();
  }

}
