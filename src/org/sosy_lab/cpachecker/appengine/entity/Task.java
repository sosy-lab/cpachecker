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
package org.sosy_lab.cpachecker.appengine.entity;

import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.sosy_lab.cpachecker.appengine.common.GAETaskQueueTaskRunner.InstanceType;
import org.sosy_lab.cpachecker.appengine.dao.TaskFileDAO;
import org.sosy_lab.cpachecker.core.CPAcheckerResult.Result;

import com.googlecode.objectify.Key;
import com.googlecode.objectify.Ref;
import com.googlecode.objectify.annotation.EmbedMap;
import com.googlecode.objectify.annotation.Entity;
import com.googlecode.objectify.annotation.Id;
import com.googlecode.objectify.annotation.Ignore;
import com.googlecode.objectify.annotation.Index;
import com.googlecode.objectify.annotation.OnLoad;
import com.googlecode.objectify.annotation.OnSave;

@Entity
public class Task {

  public enum Status {
    /**
     * The {@link Task} has not yet been started.
     */
    PENDING,
    /**
     * The {@link Task} is currently being run.
     */
    RUNNING,
    /**
     * The {@link Task} has successfully been run.
     */
    DONE,
    /**
     * The execution of the {@link Task} timed out.
     */
    TIMEOUT,
    /**
     * An error has occurred while running the {@link Task}.
     */
    ERROR
  }

  @Id
  private Long id;
  // ID to identify the associated request with the App Engine log file
  private String requestID;
  private Date creationDate;
  private Date executionDate;
  private Date terminationDate;
  @Index
  private Status status;
  private String statusMessage;
  private String specification;
  private String configuration;
  private String queueName;
  private String taskName;
  private InstanceType instanceType;
  private int retries;
  private Result resultOutcome;
  private String resultMessage;
  @EmbedMap
  private Map<String, String> options = new HashMap<>();
  private Ref<TaskFile> program;
  private TaskStatistic statistic;

  @Ignore
  private boolean optionsEscaped = false;

  public Task() {
    init();
  }

  public Task(Long id) {
    init();
    this.id = id;
  }

  private void init() {
    status = Status.PENDING;
    creationDate = new Date();
  }

  public String getKey() {
    return Key.create(Task.class, getId()).getString();
  }

  public String getRequestID() {
    return requestID;
  }

  public void setRequestID(String pRequestID) {
    requestID = pRequestID;
  }

  public Date getExecutionDate() {
    return executionDate;
  }

  public void setExecutionDate(Date pExecutionDate) {
    executionDate = pExecutionDate;
  }

  public Date getTerminationDate() {
    return terminationDate;
  }

  public void setTerminationDate(Date pTerminationDate) {
    terminationDate = pTerminationDate;
  }

  public Status getStatus() {
    return status;
  }

  public void setStatus(Status pStatus) {
    status = pStatus;
  }

  public String getStatusMessage() {
    return statusMessage;
  }

  public void setStatusMessage(String pStatusMessage) {
    statusMessage = pStatusMessage;
  }

  public Map<String, String> getOptions() {
    if (optionsEscaped) {
      unescapeOptions();
    }
    return options;
  }

  public void setOptions(Map<String, String> pOptions) {
    DefaultOptions defaultOptions = new DefaultOptions();
    defaultOptions.setOptions(pOptions);
    options = defaultOptions.getOptions();
  }

  /**
   * Since dots (.) must not be part of a key they are escaped upon saving.
   */
  @OnSave
  void escapeOptionKeys() {
    Map<String, String> escapedMap = new HashMap<>();
    for (String key : options.keySet()) {
      escapedMap.put(key.replace(".", "\\"), options.get(key));
    }
    setOptions(escapedMap);
    optionsEscaped = true;
  }

  /**
   * Since dots (.) must not be part of a key they were escaped upon saving
   * and therefore need to be unescaped after loading.
   */
  @OnLoad
  void unescapeOptionKeys() {
    unescapeOptions();
  }

  private void unescapeOptions() {
    Map<String, String> unescapedMap = new HashMap<>();
    for (String key : options.keySet()) {
      unescapedMap.put(key.replace("\\", "."), options.get(key));
    }
    setOptions(unescapedMap);
    optionsEscaped = false;
  }

  public String getSpecification() {
    return specification;
  }

  public void setSpecification(String pSpecification) {
    specification = pSpecification;
  }

  public String getConfiguration() {
    return configuration;
  }

  public void setConfiguration(String pConfiguration) {
    configuration = pConfiguration;
  }

  public TaskFile getProgram() {
    return program.get();
  }

  public void setProgram(TaskFile program) {
    this.program = Ref.create(program);
  }

  public Long getId() {
    return id;
  }

  public void setId(Long pId) {
    id = pId;
  }

  public Date getCreationDate() {
    return creationDate;
  }

  /**
   * Returns the file with an ID of the given path.
   *
   * @param path The file's path.
   * @return The file or null if the file cannot be found
   */
  public TaskFile getFile(String path) {
    return TaskFileDAO.loadByPath(path, this);
  }

  public List<TaskFile> getFilesLoaded() {
    return TaskFileDAO.files(this);
  }

  public String getQueueName() {
    return queueName;
  }

  public void setQueueName(String pQueueName) {
    queueName = pQueueName;
  }

  public String getTaskName() {
    return taskName;
  }

  public void setTaskName(String pTaskName) {
    taskName = pTaskName;
  }

  public InstanceType getInstanceType() {
    return instanceType;
  }

  public void setInstanceType(InstanceType pInstanceType) {
    instanceType = pInstanceType;
  }

  public int getRetries() {
    return retries;
  }

  public void setRetries(int pRetries) {
    retries = pRetries;
  }

  public Result getResultOutcome() {
    return resultOutcome;
  }

  public void setResultOutcome(Result pResultOutcome) {
    resultOutcome = pResultOutcome;
  }

  public String getResultMessage() {
    return resultMessage;
  }

  public void setResultMessage(String pResultMessage) {
    resultMessage = pResultMessage;
  }

  public TaskStatistic getStatistic() {
    return statistic;
  }

  public void setStatistic(TaskStatistic pStatistic) {
    statistic = pStatistic;
  }
}
