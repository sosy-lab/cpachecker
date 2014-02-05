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
package org.sosy_lab.cpachecker.appengine.dao;

import static com.googlecode.objectify.ObjectifyService.ofy;

import java.io.IOException;
import java.util.Collections;
import java.util.List;

import org.sosy_lab.cpachecker.appengine.common.GAETaskQueueJobRunner;
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

          if (job.getFiles() != null && job.getFiles().size() > 0) {
            ofy().delete().entities(job.getFiles()).now();
          }

          if (job.getStatistic() != null) {
            ofy().delete().entity(job.getStatistic()).now();
          }

          ofy().delete().entities(job).now();
        }
      });
    }
  }

  /**
   * Deletes all jobs, job files and purges the task queue.
   */
  public static void deleteAll() {
    List<Key<Job>> jobKeys = ofy().load().type(Job.class).keys().list();
    ofy().delete().keys(jobKeys).now();
    List<Key<JobFile>> fileKeys = ofy().load().type(JobFile.class).keys().list();
    ofy().delete().keys(fileKeys).now();
    List<Key<JobStatistic>> statKeys = ofy().load().type(JobStatistic.class).keys().list();
    ofy().delete().keys(statKeys).now();

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

    // job has started and no statistics were set
    if (job.getRequestID() != null && job.getStatisticReference() == null) {
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
            JobStatistic stats = new JobStatistic(job);
            stats.setCost(record.getCost());
            stats.setHost(record.getHost());
            stats.setLatency(record.getLatencyUsec());
            stats.setEndTime(record.getEndTimeUsec());
            stats.setStartTime(record.getStartTimeUsec());
            stats.setPendingTime(record.getPendingTimeUsec());
            stats.setMcycles(record.getMcycles());

            JobStatisticDAO.save(stats);
            job.setStatistic(stats);
          }

          job = save(job);
        }
      }
    }

    return job;
  }

}
