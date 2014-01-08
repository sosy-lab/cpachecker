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

import java.io.File;
import java.io.FilenameFilter;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;

import org.restlet.ext.wadl.WadlServerResource;
import org.restlet.representation.Representation;
import org.sosy_lab.common.io.Path;
import org.sosy_lab.common.io.Paths;
import org.sosy_lab.cpachecker.appengine.common.FreemarkerUtil;
import org.sosy_lab.cpachecker.appengine.server.common.RootResource;


public class RootServerResource extends WadlServerResource implements RootResource {

  @Override
  public Representation getRootHtml() {
    Path specificationDir = Paths.get("WEB-INF/specifications");
    File[] specifications = specificationDir.toFile().listFiles(new FilenameFilter() {

      @Override
      public boolean accept(File pDir, String pName) {
        // exclude directories from the list
        return pName.endsWith(".spc");
      }
    });

    Path configurationDir = Paths.get("WEB-INF/configurations");
    File[] configurations = configurationDir.toFile().listFiles(new FilenameFilter() {

      @Override
      public boolean accept(File pDir, String pName) {
        // exclude directories from the list
        return pName.endsWith(".properties");
      }
    });

    Map<String, String> defaultOptions = new HashMap<>();
    Properties defaultProperties = new Properties();
    try {
      defaultProperties.load(Paths.get("WEB-INF", "default-options.properties").asByteSource().openStream());
    } catch (IOException e) {
      // TODO handle this correctly
      e.printStackTrace();
    }

    for (String key : defaultProperties.stringPropertyNames()) {
      defaultOptions.put(key, defaultProperties.getProperty(key));
    }
    return FreemarkerUtil.templateBuilder()
        .context(getContext())
        .addData("defaultOptions", defaultOptions)
        .addData("specifications", specifications)
        .addData("configurations", configurations)
        .templateName("root.ftl")
        .build();
  }
}
