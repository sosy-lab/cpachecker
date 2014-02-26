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
import java.util.Collection;
import java.util.Collections;
import java.util.Date;
import java.util.LinkedList;
import java.util.List;

import org.sosy_lab.cpachecker.appengine.entity.Task;
import org.sosy_lab.cpachecker.appengine.entity.Task.Status;
import org.sosy_lab.cpachecker.appengine.entity.TaskFile;
import org.sosy_lab.cpachecker.appengine.entity.TaskStatistic;
import org.sosy_lab.cpachecker.appengine.entity.Taskset;
import org.sosy_lab.cpachecker.appengine.server.TaskQueueTaskRunner;
import org.sosy_lab.cpachecker.appengine.server.common.TaskRunnerResource;
import org.sosy_lab.cpachecker.appengine.util.DefaultOptions;

import com.google.appengine.api.log.LogQuery;
import com.google.appengine.api.log.LogServiceFactory;
import com.google.appengine.api.log.RequestLogs;
import com.google.appengine.api.taskqueue.Queue;
import com.google.appengine.api.taskqueue.QueueFactory;
import com.google.appengine.labs.repackaged.com.google.common.collect.Lists;
import com.googlecode.objectify.Key;
import com.googlecode.objectify.ObjectifyService;
import com.googlecode.objectify.VoidWork;


/**
 * This class provides methods for loading, saving and deletion of {@link Task}
 * instances.
 */
public class TaskDAO {

  /**
   * @see #load(Key)
   */
  public static Task load(String key) {
    try {
      Key<Task> taskKey = Key.create(key);
      return load(taskKey, false);
    } catch (IllegalArgumentException e) {
      return null;
    }
  }

  /**
   * Retrieves and returns a {@link Task} with the given key.
   * The task will be sanitized before returning.
   *
   * @param key The key of the desired {@link Task}
   * @return The desired {@link Task} or null if it cannot be found
   */
  public static Task load(Key<Task> key) {
    return load(key, false);
  }

  /**
   * @see #loadWithoutSanitizing(Key)
   */
  public static Task loadWithoutSanitizing(String key) {
    try {
      Key<Task> taskKey = Key.create(key);
      return load(taskKey, true);
    } catch (IllegalArgumentException e) {
      return null;
    }
  }

  /**
   * Retrieves and returns a {@link Task} with the given key.
   * The task will not be sanitized before returning.
   *
   * @param key The key of the desired {@link Task}
   * @return The desired {@link Task} or null if it cannot be found
   */
  public static Task loadWithoutSanitizing(Key<Task> key) {
    return load(key, true);
  }

  private static Task load(Key<Task> key, boolean unSanitized) {
    if (unSanitized) {
      return ofy().load().key(key).now();
    } else {
      return sanitizeStateAndSetStatistics(ofy().load().key(key).now());
    }
  }

  /**
   * Returns a {@link List} of {@link Task}s.
   *
   * @param keys A {@link List} of keys of the {@link Task}s to retrieve
   * @return A {@link List} of {@link Task}s
   */
  public static List<Task> load(List<String> keys) {
    List<Key<Task>> taskKeys = new LinkedList<>();
    for (String key : keys) {
      Key<Task> taskKey = Key.create(key);
      taskKeys.add(taskKey);
    }
    return sanitizeStateAndSetStatistics(ofy().load().keys(taskKeys).values());
  }

  /**
   * Returns a list containing all available {@link Task}s.
   * @return
   */
  public static List<Task> tasks() {
    return sanitizeStateAndSetStatistics(ofy().load().type(Task.class).list());
  }

  /**
   * Returns a {@link List} of {@link Task}s whose status is either
   * {@link Status#DONE}, {@link Status#ERROR} or {@link Status#TIMEOUT} and which
   * have not been marked as processed yet.
   *
   * @param taskset The {@link Taskset} to which the desired {@link Task}s belong.
   * @param limit The maximum number of {@link Task}s to retrieve. If -1 then no limit will be applied.
   * @return A {@link List} of {@link Task}s
   */
  public static List<Task> finishedTasks(Taskset taskset, int limit) {
    return tasksWithStatus(taskset, true, limit);
  }

  /**
   * Returns a {@link List} of {@link Task}s whose status is either
   * {@link Status#PENDING} of {@link Status#RUNNING} and which
   * have not been marked as processed yet.
   *
   * @param taskset The {@link Taskset} to which the desired {@link Task}s belong.
   * @param limit The maximum number of {@link Task}s to retrieve. If -1 then no limit will be applied.
   * @return A {@link List} of {@link Task}s
   */
  public static List<Task> unfinishedTasks(Taskset taskset, int limit) {
    return tasksWithStatus(taskset, false, limit);
  }

  private static List<Task> tasksWithStatus(Taskset taskset, boolean statusDone, int limit) {
    if (limit == -1) {
      limit = Integer.MAX_VALUE;
    }
    return sanitizeStateAndSetStatistics(ofy().load()
        .type(Task.class)
        .filter("taskset =", taskset)
        .filter("processed =", false)
        .filter("done =", statusDone)
        .limit(limit)
        .list());
  }

  /**
   * Saves the given {@link Task}.
   *
   * @param task The {@link Task} to save
   * @return The saved {@link Task}
   */
  public static Task save(Task task) {
    ofy().save().entity(task).now();
    return task;
  }

  /**
   * Saves the given {@link Task}s.
   *
   * @param tasks The {@link Task}s to save
   * @return The saved {@link Task}s
   */
  public static List<Task> save(List<Task> tasks) {
    ofy().save().entities(tasks).now();
    return tasks;
  }

  /**
   * Deletes the given {@link Task} and all associated {@link TaskFile}.
   *
   * @param task The {@link Task} to delete
   */
  public static void delete(final Task task) {
    if (task != null) {
      ofy().transact(new VoidWork() {

        @Override
        public void vrun() {
          try {
            Queue queue = QueueFactory.getQueue(task.getQueueName());
            queue.deleteTask(task.getTaskName());
          } catch (Exception _) {
            /*
             * it does not matter if it could be deleted or not
             * since it will disappear anyway after it's been run.
             */
          }

          TaskFileDAO.deleteAll(task);
          ofy().delete().entity(task).now();
        }
      });
    }
  }

  /**
   * Deletes 500 {@link Task}s and {@link TaskFile}s at once and purges the
   * task queue.
   * This method needs to be called multiple times since huge amounts of
   * entities in the data store cannot be deleted at once.
   */
  public static void deleteAll() {
    int limit = 500;
    List<Key<Task>> taskKeys = ofy().load().type(Task.class).limit(limit).keys().list();
    List<Key<TaskFile>> fileKeys = ofy().load().type(TaskFile.class).limit(limit).keys().list();

    ofy().delete().keys(taskKeys).now();
    ofy().delete().keys(fileKeys).now();

    try {
      Queue queue = QueueFactory.getQueue(TaskQueueTaskRunner.QUEUE_NAME);
      queue.purge();
    } catch (Exception _) {
      /*
       * it does not matter if the queue could be purged or not
       * since tasks will disappear anyway after they've been run.
       */
    }
  }

  /**
   * Sets the following properties to null. Does not save the {@link Task}!
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
   * @param task The {@link Task} to reset
   * @return The given {@link Task}
   */
  public static Task reset(Task task) {
    task.setExecutionDate(null);
    task.setResultMessage(null);
    task.setResultOutcome(null);
    task.setRequestID(null);
    task.setStatistic(null);
    task.setStatusMessage(null);
    task.setTerminationDate(null);
    return task;
  }

  /**
   * @see #delete(Task)
   */
  public static void delete(Key<Task> key) {
    delete(load(key));
  }

  /**
   * Allocates and returns a key that can be used to identify a {@link Task} instance.
   *
   * @return A key for a {@link Task}.
   */
  public static Key<Task> allocateKey() {
    return ObjectifyService.factory().allocateId(Task.class);
  }

  /**
   * Clears the entity cache.
   */
  public static void clearCache() {
    ObjectifyService.ofy().clear();
  }

  /**
   * Retrieves the log entry associated with the given {@link Task} and uses
   * the entry together with the {@link Task}'s state to fix the {@link Task} if
   * it is in an undefined state.
   * Also saves any statistics found in the log entry together with the {@link Task}.
   *
   * @param task The {@link Task} to sanitize.
   * @return The given {@link Task} instance
   */
  private static Task sanitizeStateAndSetStatistics(Task task) {
    if (task == null) { return task; }

    if (task.getRequestID() != null && task.getStatistic() == null) {
      LogQuery query = LogQuery.Builder
          .withRequestIds(Collections.singletonList(task.getRequestID()))
          .includeIncomplete(false);
      for (RequestLogs record : LogServiceFactory.getLogService().fetch(query)) {

        if (task.getStatus() == Status.DONE
            || task.getStatus() == Status.ERROR
            || task.getStatus() == Status.TIMEOUT) {

          if (task.getStatus() == Status.DONE) {
            TaskFile file = TaskFileDAO.loadByName(TaskRunnerResource.ERROR_FILE_NAME, task);
            if (file != null) {
              TaskFileDAO.delete(file);
            }
          }
          setStatistics(task, record);
          return save(task);
        }

        if (task.getStatus() == Status.PENDING || task.getStatus() == Status.RUNNING) {
          try {
            if (TaskFileDAO.loadByName(DefaultOptions.getImmutableOptions().get("statistics.file"), task) != null) {
              task.setStatus(Status.DONE);
              if (task.getTerminationDate() == null) {
                task.setTerminationDate(new Date());
              }
              setStatistics(task, record);
              return save(task);
            }
          } catch (IOException e) {
            // no stats file
          }

          TaskFile errorFile = TaskFileDAO.loadByName(TaskRunnerResource.ERROR_FILE_NAME, task);
          if (errorFile != null) {
            if (errorFile.getContent().contains("Deadline")
                || errorFile.getContent().contains("Timeout")) {
              task.setStatus(Status.TIMEOUT);
            } else {
              task.setStatus(Status.ERROR);
            }
            if (task.getTerminationDate() == null) {
              task.setTerminationDate(new Date());
            }
            setStatistics(task, record);
            return save(task);
          }

          // FIXME checking request might not work
          // if tried once and fails second time without starting then retries is always < max_retries
          if (record.getStatus() == 500 && task.getRetries() < TaskRunnerResource.MAX_RETRIES) {
            reset(task);
            task.setStatus(Status.PENDING);
            return save(task);
          }

          task.setStatus(Status.ERROR);
          task.setStatusMessage("The task's request has finished, the task's status did not reflect this and no retries are left");
          task.setTerminationDate(new Date());
          task.setRequestID(record.getRequestId());
          setStatistics(task, record);
          save(task);
        }
      }
    }

    if (task.getRequestID() == null && task.getStatus() == Status.PENDING) {

      Date now = new Date();
      LogQuery query = LogQuery.Builder
          .withStartTimeMillis(task.getCreationDate().getTime())
          .endTimeMillis(now.getTime())
          .includeIncomplete(false)
          .batchSize(Integer.MAX_VALUE);

      int amountOfDetectedRecords = 0;
      RequestLogs lastRecord = null;
      for (RequestLogs record : LogServiceFactory.getLogService().fetch(query)) {
        if (record.getTaskName() != null && record.getTaskName().equals(task.getTaskName())) {
          //&& record.getStatus() == 500
          amountOfDetectedRecords = amountOfDetectedRecords + 1;
          lastRecord = record;
        }
      }

      // retry count is 0-indexed, therefore e.g. 2 records for MAX_RETRIES == 1
      if (amountOfDetectedRecords > TaskRunnerResource.MAX_RETRIES) {
        TaskDAO.reset(task);
        task.setStatus(Status.ERROR);
        task.setStatusMessage("The task's request has finished, the task's status did not reflect this and no retries are left");
        task.setRequestID(lastRecord.getRequestId());
        task.setTerminationDate(new Date());
        setStatistics(task, lastRecord);
        return save(task);
      }
    }

    return task;
  }

  private static List<Task> sanitizeStateAndSetStatistics(Collection<Task> tasks) {
    for (Task task : tasks) {
      sanitizeStateAndSetStatistics(task);
    }
    return Lists.newLinkedList(tasks);
  }

  private static Task setStatistics(Task task, RequestLogs record) {
    TaskStatistic stats = new TaskStatistic();
    stats.setCost(record.getCost());
    stats.setHost(record.getHost());
    stats.setLatency(record.getLatencyUsec());
    stats.setEndTime(record.getEndTimeUsec());
    stats.setStartTime(record.getStartTimeUsec());
    stats.setPendingTime(record.getPendingTimeUsec());
    stats.setMcycles(record.getMcycles());

    task.setStatistic(stats);
    return task;
  }
}
