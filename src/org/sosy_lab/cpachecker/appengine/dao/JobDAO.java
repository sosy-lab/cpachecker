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

import com.google.appengine.api.log.LogQuery;
import com.google.appengine.api.log.LogServiceFactory;
import com.google.appengine.api.log.RequestLogs;
import com.google.appengine.api.taskqueue.Queue;
import com.google.appengine.api.taskqueue.QueueFactory;
import com.googlecode.objectify.Key;
import com.googlecode.objectify.ObjectifyService;
import com.googlecode.objectify.VoidWork;


public class JobDAO {

  public static Job load(String key) {
    try {
      Key<Job> jobKey = Key.create(key);
      return load(jobKey);
    } catch (IllegalArgumentException e) {
      return null;
    }
  }

  public static Job load(Key<Job> key) {
    Job job = ofy().load().key(key).now();
    return retrieveAndSetStats(job);
  }

  public static List<Job> jobs() {
    return ofy().load().type(Job.class).list();
  }

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

  public static void delete(Key<Job> key) {
    delete(load(key));
  }

  public static Key<Job> allocateKey() {
    return ObjectifyService.factory().allocateId(Job.class);
  }

  private static Job retrieveAndSetStats(Job job) {
    if (job == null) {
      return job;
    }

    if (job.getRequestID() != null && job.getStatisticReference() == null) {
      List<String> reqIDs = Collections.singletonList(job.getRequestID());
      LogQuery query = LogQuery.Builder.withRequestIds(reqIDs);
      for (RequestLogs record : LogServiceFactory.getLogService().fetch(query)) {
        if (record.isFinished()) {

          // clear error file if the job has been retried but now succeeded
          if (job.getStatus() == Status.DONE && job.getRetries() > 0) {
            JobFile errorFile = JobFileDAO.loadByName(JobRunnerResource.ERROR_FILE_NAME, job);
            if (errorFile != null) {
              JobFileDAO.delete(errorFile);
            }
          }

          // Update status if job is done but the status does not reflect this
          if (job.getStatus() == Status.PENDING
              || job.getStatus() == Status.RUNNING) {
            job.setStatus(Status.ERROR);
            job.setStatusMessage(String.format("Running the job is done but the status did not reflect this."
                + "Therefore the status was set to %s.", Status.ERROR));
            JobFile error = new JobFile(JobRunnerResource.ERROR_FILE_NAME, job);
            error.setContent(record.getCombined());
            try {
              JobFileDAO.save(error);
            } catch (IOException e) {
              // well, then there will be no explanation about the error.
            }
          }

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
          job = save(job);
        }
      }
    }

    return job;
  }

}
