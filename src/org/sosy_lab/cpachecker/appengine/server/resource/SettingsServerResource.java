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
import java.util.Map;

import org.restlet.data.MediaType;
import org.restlet.ext.wadl.WadlServerResource;
import org.restlet.representation.Representation;
import org.restlet.representation.StringRepresentation;
import org.sosy_lab.cpachecker.appengine.entity.DefaultOptions;
import org.sosy_lab.cpachecker.appengine.server.CPAcheckerApplication;
import org.sosy_lab.cpachecker.appengine.server.common.TaskRunnerResource;
import org.sosy_lab.cpachecker.appengine.server.common.SettingsResource;
import org.sosy_lab.cpachecker.core.CPAchecker;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;


public class SettingsServerResource extends WadlServerResource implements SettingsResource {

  @Override
  public Representation getSettingsAsJson() throws IOException {
    ObjectMapper mapper = new ObjectMapper();
    Map<String, Object> settings = new HashMap<>();
    settings.put("timeLimit", DefaultOptions.DEFAUL_WALLTIME_LIMIT);
    settings.put("retries", String.valueOf(TaskRunnerResource.MAX_RETRIES));
    settings.put("errorFileName", TaskRunnerResource.ERROR_FILE_NAME);
    settings.put("statisticsFileName", DefaultOptions.getImmutableOptions().get("statistics.file"));
    settings.put("cpacheckerVersion", CPAchecker.getVersion());
    settings.put("cpacheckerOnGAEVersion", CPAcheckerApplication.getVersion());
    settings.put("CPUSpeed", "600Mhz"); // see appengine-web.xml
    settings.put("RAM", "128M"); // see appengine-web.xml
    settings.put("defaultOptions", DefaultOptions.getImmutableOptions());
    settings.put("specifications", DefaultOptions.getSpecifications());
    settings.put("configurations", DefaultOptions.getConfigurations());
    settings.put("unsupportedConfigurations", DefaultOptions.getUnsupportedConfigurations());

    try {
      return new StringRepresentation(mapper.writeValueAsString(settings), MediaType.APPLICATION_JSON);
    } catch (JsonProcessingException e) {
      throw new IOException(e);
    }
  }
}
