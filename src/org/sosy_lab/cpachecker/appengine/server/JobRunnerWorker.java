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
package org.sosy_lab.cpachecker.appengine.server;

import static com.googlecode.objectify.ObjectifyService.ofy;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.logging.Handler;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.sosy_lab.common.LogManager;
import org.sosy_lab.common.configuration.Configuration;
import org.sosy_lab.common.configuration.Configuration.Builder;
import org.sosy_lab.common.configuration.FileOption;
import org.sosy_lab.common.configuration.InvalidConfigurationException;
import org.sosy_lab.common.configuration.converters.FileTypeConverter;
import org.sosy_lab.common.io.AbstractPathFactory;
import org.sosy_lab.common.io.Path;
import org.sosy_lab.common.io.Paths;
import org.sosy_lab.cpachecker.appengine.common.GAELogHandler;
import org.sosy_lab.cpachecker.appengine.common.GAELogManager;
import org.sosy_lab.cpachecker.appengine.dao.JobDAO;
import org.sosy_lab.cpachecker.appengine.entity.Job;
import org.sosy_lab.cpachecker.appengine.entity.Job.Status;
import org.sosy_lab.cpachecker.appengine.io.GAEPathFactory;
import org.sosy_lab.cpachecker.core.CPAchecker;
import org.sosy_lab.cpachecker.core.CPAcheckerResult;
import org.sosy_lab.cpachecker.core.ShutdownNotifier;
import org.sosy_lab.cpachecker.core.algorithm.ProofGenerator;

import com.googlecode.objectify.Key;

@SuppressWarnings("serial")
public class JobRunnerWorker extends HttpServlet {

  @Override
  protected void doPost(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
    Key<Job> jobKey = Key.create(request.getParameter("jobKey"));
    Job job = ofy().load().key(jobKey).now();

    AbstractPathFactory pathFactory = new GAEPathFactory(job);
    Paths.setFactory(pathFactory);

    job.setExecutionDate(new Date());
    job.setStatus(Status.RUNNING);
    JobDAO.save(job);

    // TODO use default spec if none is provided
    Path specificationFile = Paths.get("WEB-INF/specifications/", job.getSpecification());
    Path configurationFile = Paths.get("WEB-INF/configurations/", job.getConfiguration());

    Builder configBuilder = Configuration.builder();
    try {
      configBuilder.loadFromFile(configurationFile);
    } catch (InvalidConfigurationException e) {
      // TODO handle correctly
      e.printStackTrace();
    }

    configBuilder
      .setOptions(job.getOptions())
      .setOptions(job.getDefaultOptions())
      .setOption("specification", specificationFile.getOriginalPath());

    Configuration configuration = null;
    try {
      configuration = configBuilder.build();
    } catch (InvalidConfigurationException e) {
      // TODO set error state on job and return appropriate HTTP response
      e.printStackTrace();
    }

    FileTypeConverter fileTypeConverter = null;
    try {
      fileTypeConverter = new FileTypeConverter(configuration);
    } catch (InvalidConfigurationException e) {
      // TODO handle correctly
      e.printStackTrace();
    }

    Configuration config = null;
    try {
      config = Configuration.builder()
                .copyFrom(configuration)
                .addConverter(FileOption.class, fileTypeConverter)
                .build();
    } catch (InvalidConfigurationException e) {
      // TODO handle correctly
      e.printStackTrace();
    }

    Configuration.getDefaultConverters().put(FileOption.class, fileTypeConverter);

    List<String> logMessages = new ArrayList<>();
    Handler handler = new GAELogHandler(logMessages);
    LogManager logManager = new GAELogManager(handler);

    // TODO use and register appropriate notifier
    ShutdownNotifier shutdownNotifier = ShutdownNotifier.create();

    // TODO dump configuration

    // TODO use only one try block for all InvalidConfigException
    CPAchecker cpaChecker = null;
    try {
      cpaChecker = new CPAchecker(config, logManager, shutdownNotifier);
    } catch (InvalidConfigurationException e) {
      // TODO handle correctly
      e.printStackTrace();
    }

    ProofGenerator proofGenerator = null;
    try {
      proofGenerator = new ProofGenerator(config, logManager, shutdownNotifier);
    } catch (InvalidConfigurationException e) {
      // TODO handle correctly
      e.printStackTrace();
    }

    CPAcheckerResult result = cpaChecker.run("program.c");

    // disabled for now due to file system writes
//    proofGenerator.generateProof(result);

    // TODO save result.status in job entity

    job.setTerminationDate(new Date());
    job.setStatus(Status.DONE);
    job.setLog(logMessages.toString());
    JobDAO.save(job);
  }
}
