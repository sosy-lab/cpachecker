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
package org.sosy_lab.cpachecker.appengine.common;

import org.sosy_lab.cpachecker.appengine.dao.JobDAO;
import org.sosy_lab.cpachecker.appengine.entity.Job;

import com.google.appengine.api.taskqueue.Queue;
import com.google.appengine.api.taskqueue.QueueFactory;
import com.google.appengine.api.taskqueue.TaskHandle;
import com.google.appengine.api.taskqueue.TaskOptions;


public class GAETaskQueueJobRunner implements JobRunner {

  public static final String QUEUE_NAME = "cpachecker";
  public static final String WORKER_PATH = "/workers/run-job";

  /**
   * Constructs a new instance.
   * The job submitted via {@link #run(Job)} will be enqueued immediately.
   */
  public GAETaskQueueJobRunner() {}

  @Override
  public Job run(Job job) {
    String jobKey = JobDAO.key(job);

    Queue queue = QueueFactory.getQueue(QUEUE_NAME);
    TaskHandle task = queue.add(
        TaskOptions.Builder
        .withUrl(WORKER_PATH)
        .taskName("job-"+jobKey)
        .param("jobKey", jobKey));

    job.setQueueName(task.getQueueName());
    job.setTaskName(task.getName());
    JobDAO.save(job);

    return job;
  }

}
