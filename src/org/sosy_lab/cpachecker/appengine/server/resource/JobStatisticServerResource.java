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
import org.sosy_lab.cpachecker.appengine.dao.JobDAO;
import org.sosy_lab.cpachecker.appengine.entity.Job;
import org.sosy_lab.cpachecker.appengine.entity.JobStatistic;
import org.sosy_lab.cpachecker.appengine.json.JobMixinAnnotations;
import org.sosy_lab.cpachecker.appengine.json.JobStatisticMixinAnnotations;
import org.sosy_lab.cpachecker.appengine.server.common.JobStatisticResource;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;


public class JobStatisticServerResource extends WadlServerResource implements JobStatisticResource {

  private JobStatistic stats = null;

  @Override
  protected void doInit() throws ResourceException {
    super.doInit();

    stats = JobDAO.load(getAttribute("jobKey")).getStatistic();

    if (stats == null) {
      getResponse().setStatus(Status.CLIENT_ERROR_NOT_FOUND);
      getResponse().commit();
    }
  }

  @Override
  public Representation statsAsJson() {
    ObjectMapper mapper = new ObjectMapper();
    mapper.enable(SerializationFeature.INDENT_OUTPUT);
    mapper.addMixInAnnotations(Job.class, JobMixinAnnotations.KeyOnly.class);
    mapper.addMixInAnnotations(JobStatistic.class, JobStatisticMixinAnnotations.Full.class);

    try {
      return new StringRepresentation(mapper.writeValueAsString(stats), MediaType.APPLICATION_JSON);
    } catch (JsonProcessingException e) {
      return null;
    }
  }

}
