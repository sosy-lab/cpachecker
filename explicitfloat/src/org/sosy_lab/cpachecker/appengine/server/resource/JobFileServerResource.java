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
import org.restlet.resource.ResourceException;
import org.sosy_lab.cpachecker.appengine.dao.JobFileDAO;
import org.sosy_lab.cpachecker.appengine.entity.Job;
import org.sosy_lab.cpachecker.appengine.entity.JobFile;
import org.sosy_lab.cpachecker.appengine.json.JobFileMixinAnnotations;
import org.sosy_lab.cpachecker.appengine.json.JobMixinAnnotations;
import org.sosy_lab.cpachecker.appengine.server.common.JobFileResource;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;


public class JobFileServerResource extends WadlServerResource implements JobFileResource {

  private JobFile file = null;

  @Override
  protected void doInit() throws ResourceException {
    super.doInit();
    file = JobFileDAO.load(getAttribute("fileKey"));

    if (file == null) {
      getResponse().setStatus(Status.CLIENT_ERROR_NOT_FOUND);
      getResponse().commit();
    }
  }

  @Override
  public Representation fileAsHtml() {
    String content = (file.getContent() == null) ? "" : file.getContent();
    return new StringRepresentation(content);
  }

  @Override
  public Representation fileAsJson() {
    ObjectMapper mapper = new ObjectMapper();
    mapper.enable(SerializationFeature.INDENT_OUTPUT);
    mapper.addMixInAnnotations(Job.class, JobMixinAnnotations.KeyOnly.class);
    mapper.addMixInAnnotations(JobFile.class, JobFileMixinAnnotations.Full.class);

    try {
      return new StringRepresentation(mapper.writeValueAsString(file), MediaType.APPLICATION_JSON);
    } catch (JsonProcessingException e) {
      return null;
    }
  }

}
