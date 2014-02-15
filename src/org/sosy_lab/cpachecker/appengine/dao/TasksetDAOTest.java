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

import java.util.Collection;

import org.junit.Test;
import org.sosy_lab.cpachecker.appengine.common.DatastoreTest;
import org.sosy_lab.cpachecker.appengine.entity.Task;
import org.sosy_lab.cpachecker.appengine.entity.Taskset;


public class TasksetDAOTest extends DatastoreTest {

  private Taskset taskset;

  @Override
  public void setUp() {
    super.setUp();
    taskset = new Taskset();
    TasksetDAO.save(taskset);
  }

  @Test
  public void shouldLoadTasksetByString() throws Exception {
    Taskset loaded = TasksetDAO.load(taskset.getKey());
    assertEquals(taskset, loaded);
  }

  @Test
  public void shouldSaveTaskset() throws Exception {
    Taskset taskset = new Taskset();
    TasksetDAO.save(taskset);

    assertNotNull(taskset.getId());
  }

  @Test
  public void shouldLoadAllTasks() throws Exception {
    putTasksIntoTaskset(2, taskset);

    Collection<Task> tasks = TasksetDAO.tasks(taskset);
    assertEquals(2, tasks.size());
  }

  @Test
  public void shouldLoadProcessedTasks() throws Exception {
    Task[] tasks = putTasksIntoTaskset(1, taskset);
    taskset.setProcessed(tasks[0].getKey());
    TasksetDAO.save(taskset);

    assertEquals(1, TasksetDAO.tasks(taskset, true).size());
    assertEquals(0, TasksetDAO.tasks(taskset, false).size());
  }

  @Test
  public void shouldLoadUnProcessedTasks() throws Exception {
    putTasksIntoTaskset(1, taskset);

    assertEquals(1, TasksetDAO.tasks(taskset, false).size());
    assertEquals(0, TasksetDAO.tasks(taskset, true).size());
  }

  @Test
  public void shouldDeleteTaskset() throws Exception {
    String key = taskset.getKey();
    TasksetDAO.delete(taskset);

    assertNull(TasksetDAO.load(key));
  }

  private Task[] putTasksIntoTaskset(int amount, Taskset tasket) {
    Task[] tasks = new Task[amount];

    for (int i = 0; i < amount; i++) {
      Task task = new Task();
      TaskDAO.save(task);
      taskset.addTask(task);
      TasksetDAO.save(tasket);
      tasks[i] = task;
    }

    return tasks;
  }
}
