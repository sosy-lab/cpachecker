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

import static org.junit.Assert.*;

import java.util.ArrayList;
import java.util.List;

import org.junit.Test;
import org.sosy_lab.cpachecker.appengine.common.DatastoreTest;
import org.sosy_lab.cpachecker.appengine.entity.Task;
import org.sosy_lab.cpachecker.appengine.entity.Task.Status;
import org.sosy_lab.cpachecker.appengine.entity.TaskFile;
import org.sosy_lab.cpachecker.appengine.entity.Taskset;

import com.google.appengine.labs.repackaged.com.google.common.collect.Lists;
import com.googlecode.objectify.Key;


public class TaskDAOTest extends DatastoreTest {

  private Task task;

  @Override
  public void setUp() {
    super.setUp();
    task = new Task();
    TaskDAO.save(task);
  }

  @Test
  public void shouldLoadTaskByString() throws Exception {
    Task loaded = TaskDAO.load(task.getKey());
    assertEquals(task, loaded);
  }

  @Test
  public void shouldLoadTaskByKey() throws Exception {
    Key<Task> key = Key.create(task);
    Task loaded = TaskDAO.load(key);
    assertEquals(task, loaded);
  }

  @Test
  public void shouldLoadAllTasks() throws Exception {
    TaskDAO.save(new Task());
    assertEquals(2, TaskDAO.tasks().size());
  }

  @Test
  public void shouldLoadTasksByKeys() throws Exception {
    Task[] tasks = createTasks(2);
    List<String> keys = new ArrayList<>();
    keys.add(tasks[0].getKey());
    keys.add(tasks[1].getKey());

    assertEquals(2, TaskDAO.load(keys).size());
    assertTrue(TaskDAO.load(keys).contains(tasks[0]));
    assertTrue(TaskDAO.load(keys).contains(tasks[1]));
  }

  @Test
  public void shouldSaveTask() throws Exception {
    Task task = new Task();
    TaskDAO.save(task);

    assertNotNull(task.getId());
  }

  @Test
  public void shouldSaveTasks() throws Exception {
    Task taskA = new Task();
    Task taskB = new Task();
    List<Task> tasks = Lists.newArrayList(taskA, taskB);
    TaskDAO.save(tasks);

    assertNotNull(taskA.getId());
    assertNotNull(taskB.getId());
  }

  @Test
  public void shouldDeleteTask() throws Exception {
    TaskFile file = new TaskFile("", task);
    TaskFileDAO.save(file);

    String keyTask = task.getKey();
    String keyFile = file.getKey();

    TaskDAO.delete(task);

    assertNull(TaskDAO.load(keyTask));
    assertNull(TaskFileDAO.load(keyFile));
  }

  @Test
  public void shouldDeleteAllTasks() throws Exception {
    createTasks(502);
    TaskDAO.deleteAll();
    assertTrue(TaskDAO.tasks().size() < 500);
    TaskDAO.deleteAll();
    assertTrue(TaskDAO.tasks().isEmpty());
  }

  @Test
  public void shouldLoadAllFinishedTasks() throws Exception {
    Taskset taskset = TasksetDAO.save(new Taskset());
    Task[] tasks = createTasks(2);
    tasks[0].setStatus(Status.DONE);
    tasks[0].setTaskset(taskset);
    TaskDAO.save(tasks[0]);

    taskset.addTask(tasks[0]);
    taskset.addTask(tasks[1]);
    TasksetDAO.save(taskset);

    assertEquals(1, TaskDAO.finishedTasks(taskset, -1).size());
    assertTrue(TaskDAO.finishedTasks(taskset, -1).contains(tasks[0]));
  }

  @Test
  public void shouldLoadAllUnfinishedTasks() throws Exception {
    Taskset taskset = TasksetDAO.save(new Taskset());
    Task[] tasks = createTasks(1);
    tasks[0].setTaskset(taskset);
    TaskDAO.save(tasks[0]);

    taskset.addTask(tasks[0]);
    TasksetDAO.save(taskset);

    assertEquals(1, TaskDAO.unfinishedTasks(taskset, -1).size());
    assertTrue(TaskDAO.unfinishedTasks(taskset, -1).contains(tasks[0]));
  }

  private Task[] createTasks(int amount) {
    Task[] tasks = new Task[amount];

    for (int i = 0; i < amount; i++) {
      tasks[i] = TaskDAO.save(new Task());
    }

    return tasks;
  }
}