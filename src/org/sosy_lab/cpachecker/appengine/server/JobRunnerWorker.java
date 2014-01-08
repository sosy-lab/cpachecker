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

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.io.PrintStream;
import java.io.Writer;
import java.util.Date;
import java.util.logging.Formatter;
import java.util.logging.Handler;
import java.util.logging.Level;
import java.util.logging.LogRecord;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.sosy_lab.common.LogManager;
import org.sosy_lab.common.concurrency.Threads;
import org.sosy_lab.common.configuration.Configuration;
import org.sosy_lab.common.configuration.FileOption;
import org.sosy_lab.common.configuration.InvalidConfigurationException;
import org.sosy_lab.common.configuration.converters.FileTypeConverter;
import org.sosy_lab.common.io.AbstractPathFactory;
import org.sosy_lab.common.io.Path;
import org.sosy_lab.common.io.Paths;
import org.sosy_lab.common.log.FileLogFormatter;
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

import com.google.appengine.api.ThreadManager;
import com.google.common.base.Charsets;

@SuppressWarnings("serial")
public class JobRunnerWorker extends HttpServlet {

  @Override
  protected void doPost(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
    Threads.setThreadFactory(ThreadManager.currentRequestThreadFactory());

    Job job = JobDAO.load(request.getParameter("jobKey"));

    AbstractPathFactory pathFactory = new GAEPathFactory(job);
    Paths.setFactory(pathFactory);

    job.setExecutionDate(new Date());
    job.setStatus(Status.RUNNING);
    JobDAO.save(job);

    Configuration config = buildConfiguration(job);
    Boolean outputDisabled = Boolean.valueOf(config.getProperty("output.disable"));

    // setup logging
    Writer logFileWriter = Paths.get("CPALog.txt").asCharSink(Charsets.UTF_8).openBufferedStream();
    Formatter fileLogFormatter = new FileLogFormatter();
    Level logLevel = Level.parse(config.getProperty("log.level"));
    GAELogHandler logHandler = new GAELogHandler(logFileWriter, fileLogFormatter, logLevel);

    LogManager logManager = null;
    try {
        logManager = new GAELogManager(config, new DummyHandler(), logHandler);
    } catch (InvalidConfigurationException e1) {
      // TODO Auto-generated catch block
      e1.printStackTrace();
    }

    // TODO use and register appropriate notifier
    ShutdownNotifier shutdownNotifier = ShutdownNotifier.create();

    if (!outputDisabled) {
      dumpConfiguration(config);
    }

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

    saveResult(result, job);

    if (!outputDisabled) {
      dumpStatistics(result, config);
    }

    // disabled for now due to file system writes
    //    proofGenerator.generateProof(result);

    // close log file to make sure it will be saved
    logHandler.flushAndClose();

    job.setTerminationDate(new Date());
    job.setStatus(Status.DONE);
    JobDAO.save(job);
  }

  /**
   * Writes the configuration to the file specified in configuration.dumpFile
   * Only writes the file if configuration.dumpFile is not null.
   *
   * @param config The configuration to dump.
   *
   * @throws IOException
   */
  private void dumpConfiguration(Configuration config) throws IOException {
    Path configurationDumpFile = Paths.get(config.getProperty("configuration.dumpFile"));
    if (configurationDumpFile != null) {
      configurationDumpFile.asCharSink(Charsets.UTF_8).write(config.asPropertiesString());
    }
  }

  /**
   * Writes the statistics to the file specified in statistics.file
   * Only writes the statistics if statistics.export is set to true.
   *
   * @param result The result containing the statistics.
   * @param config The configuration.
   *
   * @throws IOException
   */
  private void dumpStatistics(CPAcheckerResult result, Configuration config) throws IOException {
    if (config.getProperty("statistics.export").equals("false")) {
      return;
    }

    Path statisticsDumpFile = Paths.get(config.getProperty("statistics.file"));
    OutputStream out = statisticsDumpFile.asByteSink().openBufferedStream();
    PrintStream stream = new PrintStream(out);
    result.printStatistics(stream);

    stream.flush();
    out.close();
  }

  /**
   * Saves the outcome of a checker run to the job entity.
   *
   * @param result The result.
   * @param job The job.
   */
  private void saveResult(CPAcheckerResult result, Job job) {
    ByteArrayOutputStream out = new ByteArrayOutputStream();
    PrintStream stream = new PrintStream(out);
    result.printResult(stream);
    stream.flush();

    job.setResultMessage(new String(out.toByteArray()));
    job.setResultOutcome(result.getResult());
    JobDAO.save(job);
  }

  /**
   * Returns the necessary configuration to run CPAchecker for the given job.
   *
   * @param job The job to build the configuration for
   * @return The configuration
   *
   * @throws IOException
   */
  private Configuration buildConfiguration(Job job) throws IOException {
    String specificationFile =
        (job.getSpecification() == null) ? "default.spc" : job.getSpecification();

    Configuration configuration = null;
    try {
      configuration = Configuration.builder()
          .setOption("specification", "WEB-INF/specifications/" + specificationFile)
          .loadFromFile(Paths.get("WEB-INF", "configurations", job.getConfiguration()))
          .loadFromFile(Paths.get("WEB-INF", "default-options.properties"))
          .setOptions(job.getOptions())
          .build();
    } catch (InvalidConfigurationException e) {
      // TODO set error state on job and return appropriate HTTP response
      e.printStackTrace();
    }

    Configuration config = null;
    try {
      FileTypeConverter fileTypeConverter = new FileTypeConverter(configuration);

      config = Configuration.builder()
          .copyFrom(configuration)
          .addConverter(FileOption.class, fileTypeConverter)
          .build();

      Configuration.getDefaultConverters().put(FileOption.class, fileTypeConverter);
    } catch (InvalidConfigurationException e) {
      // TODO handle correctly
      e.printStackTrace();
    }

    return config;
  }

  private class DummyHandler extends Handler {
    @Override
    public void publish(LogRecord pRecord) {}
    @Override
    public void flush() {}
    @Override
    public void close() throws SecurityException {}
  }
}
