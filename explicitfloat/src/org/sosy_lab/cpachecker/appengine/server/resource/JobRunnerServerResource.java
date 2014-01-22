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
import java.io.OutputStream;
import java.io.PrintStream;
import java.util.Date;
import java.util.logging.Formatter;
import java.util.logging.Handler;
import java.util.logging.Level;
import java.util.logging.LogRecord;

import org.restlet.data.Form;
import org.restlet.ext.wadl.WadlServerResource;
import org.restlet.representation.Representation;
import org.sosy_lab.common.LogManager;
import org.sosy_lab.common.concurrency.Threads;
import org.sosy_lab.common.configuration.Configuration;
import org.sosy_lab.common.configuration.Configuration.Builder;
import org.sosy_lab.common.configuration.FileOption;
import org.sosy_lab.common.configuration.InvalidConfigurationException;
import org.sosy_lab.common.configuration.converters.FileTypeConverter;
import org.sosy_lab.common.io.Path;
import org.sosy_lab.common.io.Paths;
import org.sosy_lab.common.log.FileLogFormatter;
import org.sosy_lab.cpachecker.appengine.dao.JobDAO;
import org.sosy_lab.cpachecker.appengine.entity.DefaultOptions;
import org.sosy_lab.cpachecker.appengine.entity.Job;
import org.sosy_lab.cpachecker.appengine.entity.Job.Status;
import org.sosy_lab.cpachecker.appengine.io.GAEPathFactory;
import org.sosy_lab.cpachecker.appengine.log.GAELogHandler;
import org.sosy_lab.cpachecker.appengine.log.GAELogManager;
import org.sosy_lab.cpachecker.appengine.server.common.JobRunnerResource;
import org.sosy_lab.cpachecker.core.CPAchecker;
import org.sosy_lab.cpachecker.core.CPAcheckerResult;
import org.sosy_lab.cpachecker.core.ShutdownNotifier;
import org.sosy_lab.cpachecker.util.resources.ResourceLimitChecker;

import com.google.appengine.api.ThreadManager;
import com.google.common.base.Charsets;
import com.google.common.io.FileWriteMode;


public class JobRunnerServerResource extends WadlServerResource implements JobRunnerResource {

  private Configuration config;
  private LogManager logManager;
  private GAELogHandler logHandler;
  private Job job;
  private CPAcheckerResult result;
  private Level logLevel;
  private Path errorPath;

  private boolean outputEnabled = false;
  private boolean configDumped = false;
  private boolean statsDumped = false;
  private boolean logDumped = false;

  @Override
  public void runJob(Representation entity) throws Exception {
    Threads.setThreadFactory(ThreadManager.currentRequestThreadFactory());
    Form requestValues = new Form(entity);
    job = JobDAO.load(requestValues.getFirstValue("jobKey"));
    if (job.getStatus() != Status.PENDING) { return; }

    Paths.setFactory(new GAEPathFactory(job));
    errorPath = Paths.get(ERROR_FILE_NAME);

    job.setExecutionDate(new Date());
    job.setStatus(Status.RUNNING);
    JobDAO.save(job);

    buildConfiguration();
    setupLogging();
    dumpConfiguration();

    // TODO use and register appropriate notifier
    ShutdownNotifier shutdownNotifier = ShutdownNotifier.create();

    ResourceLimitChecker limits = null;
    limits = ResourceLimitChecker.fromConfiguration(config, logManager, shutdownNotifier);
    limits.start();

    CPAchecker cpaChecker = new CPAchecker(config, logManager, shutdownNotifier);
    result = cpaChecker.run("program.c");

    setResult();
    dumpStatistics();

    dumpLog();

    job.setTerminationDate(new Date());
    job.setStatus(Status.DONE);
    JobDAO.save(job);
  }

  @Override
  protected void doCatch(Throwable e) {
    // set status OK to pretend everything went fine so that the task will not be retried.
    getResponse().setStatus(org.restlet.data.Status.SUCCESS_OK);

    switch (e.getCause().getClass().getSimpleName()) {
    case "DeadlineExceededException":
      job.setStatus(Status.TIMEOUT);
      job.setStatusMessage("The task timed out. Results may be available however.");
      log(Level.WARNING, "Task timed out. Trying to rescue results.", e.getCause());
      break;
    case "InvalidConfigurationException":
      job.setStatusMessage("The given configuration is invalid.");
      log(Level.WARNING, "The given configuration is invalid.", e.getCause());
      break;
    case "IOException":
      job.setStatusMessage(String.format("An I/O error occurred: %s", e.getCause().getMessage()));
      log(Level.WARNING, "An I/O error occurred.", e.getCause());
      break;
    default:
      job.setStatusMessage(String.format("An error occured: %s", e.getCause().getMessage()));
      log(Level.WARNING, "There was an error", e.getCause());
    }

    if (job.getStatus() != Status.TIMEOUT) {
      job.setStatus(Status.ERROR);
    }

    try {
      saveStackTrace(e.getCause());
      setResult();
      job.setTerminationDate(new Date());
      JobDAO.save(job);

      dumpConfiguration();
      dumpStatistics();
      dumpLog();
    } catch (IOException _) {
      // we are already in an error state so ignore any further one
    }
  }

  private void log(Level level, Object... args) {
    if (logManager != null) {
      logManager.log(level, args);
    }
  }

  private void setResult() {
    if (result != null) {
      job.setResultMessage(result.getResultString());
      job.setResultOutcome(result.getResult());
    }
  }

  private void saveStackTrace(Throwable e) throws IOException {
    try (OutputStream out = errorPath.asByteSink(FileWriteMode.APPEND).openStream()) {
      PrintStream ps = new PrintStream(out);
      e.printStackTrace(ps);
      ps.flush();
    }
  }

  private void dumpLog() {
    if (logDumped) { return; }

    if (config.getProperty("statistics.export").equals("true") && logHandler != null) {
      if (logLevel != null && logLevel.intValue() > 0) {
        logHandler.flushAndClose();
        logDumped = true;
      }
    }
  }

  private void dumpConfiguration() throws IOException {
    if (configDumped) { return; }

    if (outputEnabled && config != null) {
      Path configurationDumpFile = Paths.get(config.getProperty("configuration.dumpFile"));
      if (configurationDumpFile != null) {
        configurationDumpFile.asCharSink(Charsets.UTF_8).write(config.asPropertiesString());
        configDumped = true;
      }
    }
  }

  private void dumpStatistics() throws IOException {
    if (statsDumped) { return; }

    if (!outputEnabled || config.getProperty("statistics.export").equals("false") || result == null) { return; }

    Path statisticsDumpFile = Paths.get(config.getProperty("statistics.file"));
    OutputStream out = statisticsDumpFile.asByteSink().openBufferedStream();
    PrintStream stream = new PrintStream(out);
    result.printStatistics(stream);

    stream.flush();
    out.close();
    statsDumped = true;
  }

  private void setupLogging() throws IOException, InvalidConfigurationException {
    if (config == null || config.getProperty("log.level") == null || config.getProperty("log.level").equals("")) {
      logLevel = Level.parse(DefaultOptions.getDefault("log.level"));
    } else {
      logLevel = Level.parse(config.getProperty("log.level"));
    }

    if (logLevel != Level.OFF) {
      Formatter fileLogFormatter = new FileLogFormatter();
      OutputStream logFileStream = Paths.get("CPALog.txt").asByteSink().openStream();
      logHandler = new GAELogHandler(logFileStream, fileLogFormatter, logLevel);
      logManager = new GAELogManager(config, new DummyHandler(), logHandler);
    } else {
      logManager = new GAELogManager(config, new DummyHandler(), new DummyHandler());
    }
  }

  private void buildConfiguration() throws IOException, InvalidConfigurationException {
    Builder configurationBuilder = Configuration.builder();
    configurationBuilder.setOptions(DefaultOptions.getDefaultOptions());

    if (job.getSpecification() != null) {
      configurationBuilder.setOption("specification", "WEB-INF/specifications/" + job.getSpecification());
    }
    if (job.getConfiguration() != null) {
      configurationBuilder.loadFromFile(Paths.get("WEB-INF", "configurations", job.getConfiguration()));
    }
    configurationBuilder
        .loadFromFile(Paths.get("WEB-INF", "default-options.properties"))
        .setOptions(job.getOptions());
    Configuration configuration = configurationBuilder.build();

    FileTypeConverter fileTypeConverter = new FileTypeConverter(configuration);

    config = Configuration.builder()
        .copyFrom(configuration)
        .addConverter(FileOption.class, fileTypeConverter)
        .build();

    outputEnabled = (config.getProperty("output.disable").equals("false"));
    Configuration.getDefaultConverters().put(FileOption.class, fileTypeConverter);
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
