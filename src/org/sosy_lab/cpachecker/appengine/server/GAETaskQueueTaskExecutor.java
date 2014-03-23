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
package org.sosy_lab.cpachecker.appengine.server;

import org.sosy_lab.common.configuration.Configuration;
import org.sosy_lab.common.configuration.InvalidConfigurationException;
import org.sosy_lab.common.configuration.Option;
import org.sosy_lab.common.configuration.Options;
import org.sosy_lab.cpachecker.appengine.common.TaskExecutor;
import org.sosy_lab.cpachecker.appengine.dao.TaskDAO;
import org.sosy_lab.cpachecker.appengine.entity.Task;

import com.google.appengine.api.backends.BackendServiceFactory;
import com.google.appengine.api.taskqueue.Queue;
import com.google.appengine.api.taskqueue.QueueFactory;
import com.google.appengine.api.taskqueue.TaskHandle;
import com.google.appengine.api.taskqueue.TaskOptions;

@Options
public class GAETaskQueueTaskExecutor implements TaskExecutor {

  public static final int MAX_RETRIES = 1; // see queue.xml

  public enum InstanceType {
    /**
     * A front-end instance will be used.
     * A time limit applies.
     */
    FRONTEND,
    /**
     * A back-end instance will be used.
     * This instance type imposes no time limit. Therefore limits.time.wall may
     * be set to anything.
     */
    BACKEND
  }

  public static final String QUEUE_NAME = "cpachecker";
  public static final String WORKER_PATH = "/workers/execute-task";
  public static final String BACKEND_NAME = "task-worker-b1";

  @Option(name = "gae.instanceType",
      description = "The instance type to use when executing CPAchecker on Google App Engine."
          + "Frontend instances have a wall time limit of 9 minutes. Backends may run for up to 24 hours."
          + "However, instance hours on backends are limited",
      values = { "FRONTEND", "BACKEND" })
  private InstanceType instanceType = InstanceType.FRONTEND;

  /**
   * Constructs a new instance.
   * The {@link Task} submitted via {@link #execute(Task)} will be enqueued immediately.
   */
  public GAETaskQueueTaskExecutor() {
    instanceType = InstanceType.FRONTEND;
  }

  /**
   * Constructs a new instance that enqueues the {@link Task} submitted via
   * {@link #execute(Task)} immediately.
   *
   * @param instancyType The instance type to use for processing the {@link Task}.
   */
  public GAETaskQueueTaskExecutor(Configuration config) throws InvalidConfigurationException {
    config.inject(this);
  }

  @Override
  public Task execute(Task task) {
    String taskKey = task.getKey();

    Queue queue = QueueFactory.getQueue(QUEUE_NAME);
    TaskOptions builder = TaskOptions.Builder
      .withUrl(WORKER_PATH)
      .taskName("task-" + taskKey)
      .param("taskKey", taskKey);

    if (instanceType == InstanceType.BACKEND) {
      builder.header("Host", BackendServiceFactory.getBackendService().getBackendAddress(BACKEND_NAME));
    }

    TaskHandle taskHandle = queue.add(builder);

    task.setQueueName(taskHandle.getQueueName());
    task.setTaskName(taskHandle.getName());
    TaskDAO.save(task);

    return task;
  }

}
