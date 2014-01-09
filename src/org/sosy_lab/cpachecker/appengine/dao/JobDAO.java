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

import java.util.List;

import org.sosy_lab.cpachecker.appengine.entity.Job;
import org.sosy_lab.cpachecker.appengine.entity.Job.Status;

import com.google.appengine.api.taskqueue.Queue;
import com.google.appengine.api.taskqueue.QueueFactory;
import com.googlecode.objectify.Key;
import com.googlecode.objectify.ObjectifyService;
import com.googlecode.objectify.Work;


public class JobDAO {

  public static Job load(String key) {
    Key<Job> jobKey = Key.create(key);
    return load(jobKey);
  }

  public static Job load(Key<Job> key) {
    return ofy().load().key(key).now();
  }

  public static List<Job> jobs() {
    return ofy().load().type(Job.class).list();
  }

  public static Job save(Job job) {
    ofy().save().entity(job).now();
    return job;
  }

  /**
   * Tries to delete a job and indicates if it was possible.
   * Also deletes all associated files.
   * If the job is in pending state removal of the corresponding task will be tried.
   * If the job is in running state the job cannot be deleted.
   *
   * @param job The job to delete.
   * @return True, if the job could be deleted, false otherwise.
   */
  public static boolean delete(final Job job) {
    return ofy().transact(new Work<Boolean>() {

      @Override
      public Boolean run() {

        if (job.getStatus() == Status.PENDING) {
          try {
            Queue queue = QueueFactory.getQueue(job.getQueueName());
            boolean couldDeleteTask = queue.deleteTask(job.getTaskName());

            if (!couldDeleteTask) {
              return false;
            }
          } catch (Exception e) {
            return false;
          }
        }

        if (job.getStatus() == Status.RUNNING) {
          return false;
        }

        ofy().delete().entities(job.getFiles()).now();
        ofy().delete().entities(job).now();

        return true;
      }
    });
  }

  public static boolean delete(String key) {
    Key<Job> jobKey = Key.create(key);
    return delete(jobKey);
  }

  public static boolean delete(Key<Job> key) {
    return delete(load(key));
  }

  public static Key<Job> allocateKey() {
    return ObjectifyService.factory().allocateId(Job.class);
  }

}
