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
package org.sosy_lab.cpachecker.appengine.entity;

import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.googlecode.objectify.Ref;
import com.googlecode.objectify.annotation.Entity;
import com.googlecode.objectify.annotation.Id;
import com.googlecode.objectify.annotation.Ignore;
import com.googlecode.objectify.annotation.Serialize;

@Entity
public class Job {

  public enum Status {
    PENDING, RUNNING, DONE, ABORTED, TIMEOUT
  }

  @Id private Long id;
  private Date creationDate;
  private Date executionDate;
  private Date terminationDate;
  private Status status;
  @Serialize private Map<String, String> options; // serialize to avoid problems with '.' in the keys
  private String specification;
  private String configuration;
  private List<Ref<JobFile>> files;
  @Ignore private Map<String, JobFile> fileLookupTable;
  private String queueName;
  private String taskName;

  // FIXME remove this stuff. It's only here for debugging just now.
  private String log;
  public void setLog(String pLog) {
    log = pLog;
  }
  public String getLog() {
    return log;
  }

  @Ignore
  private Map<String, String> defaultOptions;

  public Job() {
    init();
  }

  public Job(Long id) {
    init();
    this.id = id;
  }

  private void init() {
    defaultOptions = new HashMap<>();
//    defaultOptions.put("output.disable", "true");
    defaultOptions.put("cpa.predicate.solver", "smtinterpol");
    defaultOptions.put("statistics.export", "false");
    defaultOptions.put("statistics.memory", "false");
    defaultOptions.put("limits.time.cpu", "-1");
    defaultOptions.put("limits.time.wall", "-1");
    defaultOptions.put("cpa.conditions.global.time.wall", "-1");
    defaultOptions.put("cpa.conditions.global.memory.heap", "-1");
    defaultOptions.put("cpa.conditions.global.memory.process", "-1");
    defaultOptions.put("cpa.conditions.global.reached.size", "-1");
    defaultOptions.put("cpa.conditions.global.time.cpu", "-1");
    defaultOptions.put("cpa.conditions.global.time.cpu.hardlimit", "-1");
    defaultOptions.put("cpa.conditions.global.time.wall", "-1");
    defaultOptions.put("cpa.conditions.global.time.wall.hardlimit", "-1");
    defaultOptions.put("cpa.conditions.path.assignments.threshold", "-1");
    defaultOptions.put("cpa.conditions.path.assumeedges.limit", "-1");
    defaultOptions.put("cpa.conditions.path.length.limit", "-1");
    defaultOptions.put("cpa.conditions.path.repetitions.limit", "-1");
    defaultOptions.put("cpa.monitor.limit", "0");
    defaultOptions.put("cpa.monitor.pathcomputationlimit", "0");
    defaultOptions.put("cpa.predicate.refinement.timelimit", "0");
    defaultOptions.put("analysis.useProofCheckAlgorithm", "false");

    files = new ArrayList<>();
    fileLookupTable = new HashMap<>();

    status = Status.PENDING;
    creationDate = new Date();
  }

  public Map<String, String> getDefaultOptions() {
    return defaultOptions;
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


  public Map<String, String> getOptions() {
    return options;
  }


  public void setOptions(Map<String, String> pOptions) {
    options = pOptions;
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

  public Long getId() {
    return id;
  }


  public void setId(Long pId) {
    id = pId;
  }


  public Date getCreationDate() {
    return creationDate;
  }

  public List<Ref<JobFile>> getFiles() {
    return files;
  }

  /**
   * Returns the file with an ID of the given path.
   *
   * @param path The file's path.
   * @return The file or null if the file cannot be found
   */
  public JobFile getFile(String path) {
    if (fileLookupTable.containsKey(path)) {
      return fileLookupTable.get(path);
    } else if (fileLookupTable.size() == files.size()) {
      return null;
    }

    for (Ref<JobFile> ref : files) {
      JobFile file = ref.get();
      fileLookupTable.put(file.getPath(), file);

      if ((file.getPath().equals(path))) {
        return  file;
      }
    }

    return null;
  }

  public void addFile(JobFile file) {
    files.add(Ref.create(file));
    fileLookupTable.put(file.getPath(), file);
  }

  public void removeFile(JobFile file) {
    files.remove(Ref.create(file));
    fileLookupTable.remove(file.getPath());
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
}
