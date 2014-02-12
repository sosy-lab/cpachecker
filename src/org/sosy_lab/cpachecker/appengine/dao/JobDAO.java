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
package org.sosy_lab.cpachecker.appengine.dao;

import static com.googlecode.objectify.ObjectifyService.ofy;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.logging.Level;

import org.sosy_lab.cpachecker.appengine.common.GAETaskQueueJobRunner;
import org.sosy_lab.cpachecker.appengine.common.GAETaskQueueJobRunner.InstanceType;
import org.sosy_lab.cpachecker.appengine.entity.DefaultOptions;
import org.sosy_lab.cpachecker.appengine.entity.Job;
import org.sosy_lab.cpachecker.appengine.entity.Job.Status;
import org.sosy_lab.cpachecker.appengine.entity.JobFile;
import org.sosy_lab.cpachecker.appengine.entity.JobStatistic;
import org.sosy_lab.cpachecker.appengine.server.common.JobRunnerResource;

import com.google.appengine.api.log.AppLogLine;
import com.google.appengine.api.log.LogQuery;
import com.google.appengine.api.log.LogServiceFactory;
import com.google.appengine.api.log.RequestLogs;
import com.google.appengine.api.taskqueue.Queue;
import com.google.appengine.api.taskqueue.QueueFactory;
import com.google.apphosting.api.ApiProxy.RequestTooLargeException;
import com.googlecode.objectify.Key;
import com.googlecode.objectify.ObjectifyService;
import com.googlecode.objectify.VoidWork;


/**
 * This class provides methods for loading, saving and deletion of {@link Job}
 * instances.
 */
public class JobDAO {

  /**
   * @see #load(Key)
   */
  public static Job load(String key) {
    try {
      Key<Job> jobKey = Key.create(key);
      return load(jobKey);
    } catch (IllegalArgumentException e) {
      return null;
    }
  }

  /**
   * Retrieves and returns a job with the given key.
   *
   * @param key The key of the desired job
   * @return The desired job or null if it cannot be found
   */
  public static Job load(Key<Job> key) {
    Job job = ofy().load().key(key).now();
    return sanitizeStateAndSetStatistics(job);
  }

  /**
   * Returns a list containing all available jobs.
   * @return
   */
  public static List<Job> jobs() {
    List<Job> jobs = ofy().load().type(Job.class).list();
    for (Job job : jobs) {
      sanitizeStateAndSetStatistics(job);
    }

    return jobs;
  }

  /**
   * Saves the given job.
   *
   * @param job The job to save
   * @return The saved job
   */
  public static Job save(Job job) {
    ofy().save().entity(job).now();
    return job;
  }

  /**
   * Deletes the given job, the associated statistic and all associated files.
   *
   * @param job The job to delete
   */
  public static void delete(final Job job) {
    if (job != null) {
      ofy().transact(new VoidWork() {

        @Override
        public void vrun() {
          try {
            Queue queue = QueueFactory.getQueue(job.getQueueName());
            queue.deleteTask(job.getTaskName());
          } catch (Exception _) {
            /*
             * it does not matter if the task could be deleted or not
             * since it will disappear anyway after it's been run.
             */
          }

          JobFileDAO.deleteAll(job);
          ofy().delete().entity(job).now();
        }
      });
    }
  }

  /**
   * Deletes 500 jobs and job files at once and purges the task queue.
   * This method needs to be called multiple times since huge amounts of entities
   * in the data store cannot be deleted at once.
   */
  public static void deleteAll() {
    List<Key<Job>> jobKeys = ofy().load().type(Job.class).limit(500).keys().list();
    List<Key<JobFile>> fileKeys = ofy().load().type(JobFile.class).limit(500).keys().list();

    ofy().delete().keys(jobKeys).now();
    ofy().delete().keys(fileKeys).now();

    try {
      Queue queue = QueueFactory.getQueue(GAETaskQueueJobRunner.QUEUE_NAME);
      queue.purge();
    } catch (Exception _) {
      /*
       * it does not matter if the queue could be purged or not
       * since tasks will disappear anyway after they've been run.
       */
    }
  }

  /**
   * Creates a new task from the given task an program.
   *
   * @param job The job
   * @param program The program
   * @return A pair with the first member being a list of errors and the second member being the created job
   */
  @SuppressWarnings("unchecked")
  public static List<String> create(Job job, JobFile program) {
    // merge existing errors
    List<String> errors = new ArrayList<>();

    if (job.getSpecification() == null && job.getConfiguration() == null) {
      errors.add("error.specOrConfigMissing");
    }

    if (job.getSpecification() != null) {
      if (!DefaultOptions.getSpecifications().contains(job.getSpecification())) {
        errors.add("error.specificationNotFound");
      }
    }

    if (job.getConfiguration() != null) {
      try {
        if (!DefaultOptions.getConfigurations().contains(job.getConfiguration())) {
          errors.add("error.configurationNotFound");
        }
      } catch (IOException e) {
        errors.add("error.configurationNotFound");
      }
    }

    if (program.getContent() == null || program.getContent().equals("")) {
      errors.add("error.noProgram");
    }

    if (job.getOptions().containsKey("log.level")) {
      try {
        Level.parse(job.getOptions().get("log.level"));
      } catch (IllegalArgumentException e) {
        errors.add("error.invalidLogLevel");
      }
    }

    if (job.getOptions().containsKey("gae.instanceType")) {
      try {
        InstanceType.valueOf(job.getOptions().get("gae.instanceType"));
      } catch (IllegalArgumentException e) {
        errors.add("error.invalidInstanceType");
      }
    }

    if (errors.size() == 0) {
      try {
        job.setId(allocateKey().getId());
        program.setJob(job);

        JobFileDAO.save(program);
        job.setProgram(program);
        JobDAO.save(job);
      } catch (IOException e) {
        if (e.getCause() instanceof RequestTooLargeException) {
          errors.add("error.programTooLarge");
        } else {
          errors.add("error.couldNotUpload");
        }
      }
    }

    return errors;
  }

  /**
   * Sets the following properties to null. Does not save the job!
   * <ul>
   * <li>executionDate</li>
   * <li>resultMessage</li>
   * <li>resultOutcome</li>
   * <li>requestID</li>
   * <li>statistics</li>
   * <li>statusMessage</li>
   * <li>terminationDate</li>
   * </ul>
   *
   * @param job The job to reset
   * @return The given job
   */
  public static Job reset(Job job) {
    job.setExecutionDate(null);
    job.setResultMessage(null);
    job.setResultOutcome(null);
    job.setRequestID(null);
    job.setStatistic(null);
    job.setStatusMessage(null);
    job.setTerminationDate(null);
    return job;
  }

  /**
   * @see #delete(Job)
   */
  public static void delete(Key<Job> key) {
    delete(load(key));
  }

  /**
   * Allocates and returns a key that can be used to identify a job instance.
   *
   * @return A key for a job.
   */
  public static Key<Job> allocateKey() {
    return ObjectifyService.factory().allocateId(Job.class);
  }

  /**
   * Retrieves the log entry associated with the given job and uses
   * the entry together with the job's state to fix the job if it is in an
   * undefined state.
   * Also saves any statistics found in the log entry together with the job.
   *
   * @param job The job to sanitize.
   * @return The given job instance
   */
  private static Job sanitizeStateAndSetStatistics(Job job) {
    if (job == null) { return job; }

    /*
     * Handle the case where the task has never started but no retries are left
     * due to internal errors on behalf of GAE.
     */
    if (job.getRequestID() == null && job.getStatus() == Status.PENDING) {
      Date now = new Date();
      LogQuery query = LogQuery.Builder
          .withStartTimeMillis(job.getCreationDate().getTime())
          .endTimeMillis(now.getTime());

      int amountOfDetectedRecords = 0;
      RequestLogs lastRecord = null;
      for (RequestLogs record : LogServiceFactory.getLogService().fetch(query)) {
        if (record.isFinished()
            && record.getTaskName() != null
            && record.getStatus() == 500
            && record.getTaskName().equals(job.getTaskName())) {
          amountOfDetectedRecords = amountOfDetectedRecords + 1;
          lastRecord = record;
        }
      }

      // retry count is 0-indexed, therefore e.g. 2 records for MAX_RETRIES == 1
      if (amountOfDetectedRecords > JobRunnerResource.MAX_RETRIES) {
        JobDAO.reset(job);
        job.setStatus(Status.ERROR);
        job.setStatusMessage("The task's request has finished, the task's status did not reflect this and no retries are left");
        job.setRequestID(lastRecord.getRequestId());
        save(job);
      }
    }

    // job has started and no statistics were set
    if (job.getRequestID() != null && job.getStatistic() == null) {
      LogQuery query = LogQuery.Builder.withRequestIds(Collections.singletonList(job.getRequestID()));
      for (RequestLogs record : LogServiceFactory.getLogService().fetch(query)) {
        if (record.isFinished()) {
          if (job.getStatus() == Status.PENDING || job.getStatus() == Status.RUNNING) {
            JobDAO.reset(job);
            job.setStatus(Status.ERROR);
            job.setStatusMessage("The task's request has finished but the task's status did not reflect this.");
          }

          if (record.getStatus() == 500 && job.getStatus() != Status.TIMEOUT && job.getStatus() != Status.DONE) {
            if (job.getRetries() < JobRunnerResource.MAX_RETRIES) {
              JobDAO.reset(job);
              job.setStatus(Status.PENDING);
              job.setStatusMessage("Waiting for retry. Already failed " + (job.getRetries() + 1) + " times.");
            } else {
              JobDAO.reset(job);
              job.setStatus(Status.ERROR);
              job.setStatusMessage("The task's request has finished, the task's status did not reflect this and no retries are left");

              JobFile errorFile = JobFileDAO.loadByName(JobRunnerResource.ERROR_FILE_NAME, job);
              if (errorFile == null) {
                errorFile = new JobFile(JobRunnerResource.ERROR_FILE_NAME, job);
              }
              StringBuilder errorString = new StringBuilder();
              errorString.append(record.getCombined());
              for (AppLogLine line : record.getAppLogLines()) {
                errorString.append(line.getLogMessage());
              }
              errorString.append(errorFile.getContent());
              errorFile.setContent(errorString.toString());
              try {
                JobFileDAO.save(errorFile);
              } catch (IOException e) {
                // too bad, no information about the error can be saved.
              }
            }
          }

          if (job.getStatus() == Status.DONE || job.getStatus() == Status.ERROR || job.getStatus() == Status.TIMEOUT) {
            JobStatistic stats = new JobStatistic();
            stats.setCost(record.getCost());
            stats.setHost(record.getHost());
            stats.setLatency(record.getLatencyUsec());
            stats.setEndTime(record.getEndTimeUsec());
            stats.setStartTime(record.getStartTimeUsec());
            stats.setPendingTime(record.getPendingTimeUsec());
            stats.setMcycles(record.getMcycles());

            job.setStatistic(stats);
          }

          job = save(job);
        }
      }
    }

    return job;
  }

}
