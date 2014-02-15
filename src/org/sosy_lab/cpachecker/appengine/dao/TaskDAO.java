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
import java.util.Collection;
import java.util.Collections;
import java.util.Date;
import java.util.List;

import org.sosy_lab.cpachecker.appengine.entity.Task;
import org.sosy_lab.cpachecker.appengine.entity.Task.Status;
import org.sosy_lab.cpachecker.appengine.entity.TaskFile;
import org.sosy_lab.cpachecker.appengine.entity.TaskStatistic;
import org.sosy_lab.cpachecker.appengine.server.GAETaskQueueTaskRunner;
import org.sosy_lab.cpachecker.appengine.server.common.TaskRunnerResource;
import org.sosy_lab.cpachecker.appengine.util.DefaultOptions;

import com.google.appengine.api.log.AppLogLine;
import com.google.appengine.api.log.LogQuery;
import com.google.appengine.api.log.LogServiceFactory;
import com.google.appengine.api.log.RequestLogs;
import com.google.appengine.api.taskqueue.Queue;
import com.google.appengine.api.taskqueue.QueueFactory;
import com.google.apphosting.api.ApiProxy.RequestTooLargeException;
import com.google.common.base.Preconditions;
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
      return load(taskKey);
    } catch (IllegalArgumentException e) {
      return null;
    }
  }

  /**
   * Retrieves and returns a {@link Task} with the given key.
   *
   * @param key The key of the desired {@link Task}
   * @return The desired {@link Task} or null if it cannot be found
   */
  public static Task load(Key<Task> key) {
    return sanitizeStateAndSetStatistics(ofy().load().key(key).now());
  }

  /**
   * Returns a {@link List} of {@link Task}s.
   *
   * @param keys A {@link List} of keys of the {@link Task}s to retrieve
   * @return A {@link List} of {@link Task}s
   */
  public static List<Task> load(List<String> keys) {
    List<Key<Task>> taskKeys = new ArrayList<>();
    for (String key : keys) {
      Key<Task> taskKey = Key.create(key);
      taskKeys.add(taskKey);
    }
    return sanitizeStateAndSetStatistics(ofy().load().keys(taskKeys).values());
  }

  /**
   * Returns a list containing all available {@link Task}.
   * @return
   */
  public static List<Task> tasks() {
    return sanitizeStateAndSetStatistics(ofy().load().type(Task.class).list());
  }

  /**
   * Returns a {@link List} of {@link Task}s whose status is {@link Status#DONE}.
   *
   * @param keys The {@link List} containing the keys of the {@link Task}s
   * @return A {@link List} of {@link Task}s
   */
  public static List<Task> finishedTasks(List<String> keys) {
    return tasksWithStatus(keys, true);
  }

  /**
   * Returns a {@link List} of {@link Task}s whose status is not {@link Status#DONE}.
   *
   * @param keys The {@link List} containing the keys of the {@link Task}s
   * @return A {@link List} of {@link Task}s
   */
  public static List<Task> unfinishedTasks(List<String> keys) {
    return tasksWithStatus(keys, false);
  }

  private static List<Task> tasksWithStatus(List<String> keys, boolean statusDone) {
    List<Task> tasks = new ArrayList<>();
    for (Task task : load(keys)) {
      if (statusDone
          && (task.getStatus() == Status.DONE
          || task.getStatus() == Status.ERROR
          || task.getStatus() == Status.TIMEOUT)) {
        tasks.add(task);
      }

      if (!statusDone
          && (task.getStatus() == Status.PENDING
          || task.getStatus() == Status.RUNNING)) {
        tasks.add(task);
      }
    }

    return sanitizeStateAndSetStatistics(tasks);
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
      Queue queue = QueueFactory.getQueue(GAETaskQueueTaskRunner.QUEUE_NAME);
      queue.purge();
    } catch (Exception _) {
      /*
       * it does not matter if the queue could be purged or not
       * since tasks will disappear anyway after they've been run.
       */
    }
  }

  /**
   * Validates the given {@link Task} and the given {@link TaskFile} and saves
   * them if the validation succeeds.
   * The given {@link TaskFile} will be set as the {@link Task}s program.
   * @see Task#setProgram(TaskFile)
   *
   * @param task The {@link Task} to validate and save
   * @param program The {@link TaskFile} to validate and set as program
   * @return A list of errors or an empty list if none occurred.
   *         Each error is {@link String} representing a key in the resource bundle.
   */
  public static List<String> validateAndSave(Task task, TaskFile program) {
    Preconditions.checkNotNull(task);
    Preconditions.checkNotNull(program);

    List<String> errors = new ArrayList<>();

    if ((task.getSpecification() == null
        || task.getSpecification().isEmpty())
        && (task.getConfiguration() == null
        || task.getConfiguration().isEmpty())) {
      errors.add("task.specOrConf.IsBlank");
    }

    if (errors.isEmpty()) {
      if (task.getSpecification() != null
          && !task.getSpecification().isEmpty()
          && !DefaultOptions.getSpecifications().contains(task.getSpecification())) {
        errors.add("task.spec.DoesNotExist");
      }

      try {
        if (task.getConfiguration() != null
            && !task.getConfiguration().isEmpty()
            && !DefaultOptions.getConfigurations().contains(task.getConfiguration())) {
          errors.add("task.conf.DoesNotExist");
        }
      } catch (IOException e) {
        errors.add("task.conf.DoesNotExist");
      }
    }

    if (program.getContent() == null
        || program.getContent().isEmpty()) {
      errors.add("task.program.IsBlank");
    }

    if (program.getPath() == null
        || program.getPath().isEmpty()) {
      errors.add("task.program.NameIsBlank");
    }

    if (errors.isEmpty()) {
      try {
        task.setId(allocateKey().getId());
        program.setTask(task);
        TaskFileDAO.save(program);
        task.setProgram(program);
        save(task);
      } catch (IOException e) {
        if (e.getCause() instanceof RequestTooLargeException) {
          errors.add("task.program.TooLarge");
        } else {
          errors.add("task.program.CouldNotUpload");
        }
      }
    }

    return errors;
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

    /*
     * Handle the case where the task has never started but no retries are left
     * due to internal errors on behalf of GAE.
     */
    if (task.getRequestID() == null && task.getStatus() == Status.PENDING) {
      Date now = new Date();
      LogQuery query = LogQuery.Builder
          .withStartTimeMillis(task.getCreationDate().getTime())
          .endTimeMillis(now.getTime())
          .batchSize(Integer.MAX_VALUE);

      int amountOfDetectedRecords = 0;
      RequestLogs lastRecord = null;
      for (RequestLogs record : LogServiceFactory.getLogService().fetch(query)) {
        if (record.isFinished()
            && record.getTaskName() != null
            && record.getStatus() == 500
            && record.getTaskName().equals(task.getTaskName())) {
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
        setStatistics(task, lastRecord);
        return save(task);
      }
    }

    // task has started and no statistics were set
    if (task.getRequestID() != null && task.getStatistic() == null) {
      LogQuery query = LogQuery.Builder.withRequestIds(Collections.singletonList(task.getRequestID()));
      for (RequestLogs record : LogServiceFactory.getLogService().fetch(query)) {
        if (record.isFinished()) {
          if (task.getStatus() == Status.PENDING || task.getStatus() == Status.RUNNING) {
            TaskDAO.reset(task);
            task.setStatus(Status.ERROR);
            task.setStatusMessage("The task's request has finished but the task's status did not reflect this.");
          }

          if (record.getStatus() == 500 && task.getStatus() != Status.TIMEOUT && task.getStatus() != Status.DONE) {
            if (task.getRetries() < TaskRunnerResource.MAX_RETRIES) {
              TaskDAO.reset(task);
              task.setStatus(Status.PENDING);
              task.setStatusMessage("Waiting for retry. Already failed " + (task.getRetries() + 1) + " times.");
            } else {
              TaskDAO.reset(task);
              task.setStatus(Status.ERROR);
              task.setStatusMessage("The task's request has finished, the task's status did not reflect this and no retries are left");

              TaskFile errorFile = TaskFileDAO.loadByName(TaskRunnerResource.ERROR_FILE_NAME, task);
              if (errorFile == null) {
                errorFile = new TaskFile(TaskRunnerResource.ERROR_FILE_NAME, task);
              }
              StringBuilder errorString = new StringBuilder();
              errorString.append(record.getCombined());
              for (AppLogLine line : record.getAppLogLines()) {
                errorString.append(line.getLogMessage());
              }
              errorString.append(errorFile.getContent());
              errorFile.setContent(errorString.toString());
              try {
                TaskFileDAO.save(errorFile);
              } catch (IOException e) {
                // too bad, no information about the error can be saved.
              }
            }
          }

          if (task.getStatus() == Status.DONE || task.getStatus() == Status.ERROR || task.getStatus() == Status.TIMEOUT) {
            setStatistics(task, record);
          }

          task = save(task);
        }
      }
    }

    return task;
  }

  private static List<Task> sanitizeStateAndSetStatistics(Collection<Task> tasks) {
    List<Task> sanitizedTasks = new ArrayList<>();
    for (Task task : tasks) {
      sanitizedTasks.add(sanitizeStateAndSetStatistics(task));
    }
    return sanitizedTasks;
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
