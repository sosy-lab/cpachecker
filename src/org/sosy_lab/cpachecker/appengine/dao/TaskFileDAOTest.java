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

import org.junit.Test;
import org.sosy_lab.cpachecker.appengine.common.DatabaseTest;
import org.sosy_lab.cpachecker.appengine.entity.Task;
import org.sosy_lab.cpachecker.appengine.entity.TaskFile;

import com.googlecode.objectify.Key;


public class TaskFileDAOTest extends DatabaseTest {

  private Task task;

  @Override
  public void setUp() {
    super.setUp();
    task = new Task();
    TaskDAO.save(task);
  }

  @Test
  public void shouldLoadByString() throws Exception {
    TaskFile file = new TaskFile("test", task);
    TaskFileDAO.save(file);
    TaskFile loaded = TaskFileDAO.load(file.getKey());

    assertEquals(file, loaded);
  }

  @Test
  public void shouldLoadByKey() throws Exception {
    TaskFile file = new TaskFile("test", task);
    TaskFileDAO.save(file);
    Key<TaskFile> key = Key.create(file.getKey());
    TaskFile loaded = TaskFileDAO.load(key);

    assertEquals(file, loaded);
  }

  @Test
  public void shouldLoadByPath() throws Exception {
    TaskFile file = new TaskFile("test", task);
    TaskFileDAO.save(file);
    TaskFile loaded = TaskFileDAO.loadByPath("test", task);

    assertEquals(file, loaded);
  }

  @Test
  public void shouldLoadByNameAndTask() throws Exception {
    TaskFile file = new TaskFile("foo/test.txt", task);
    TaskFileDAO.save(file);
    TaskFile loaded = TaskFileDAO.loadByName("test.txt", task);

    assertEquals(file, loaded);
  }

  @Test
  public void shouldLoadByNameAndTaskKeyString() throws Exception {
    TaskFile file = new TaskFile("foo/test.txt", task);
    TaskFileDAO.save(file);
    TaskFile loaded = TaskFileDAO.loadByName("test.txt", task.getKey());

    assertEquals(file, loaded);
  }

  @Test
  public void shouldLoadAllFiles() throws Exception {
    TaskFile file = new TaskFile("testA.txt", task);
    TaskFileDAO.save(file);
    file = new TaskFile("testB.txt", task);
    TaskFileDAO.save(file);

    assertEquals(2, TaskFileDAO.files(task).size());
  }

  @Test
  public void shouldSave() throws Exception {
    TaskFile file = new TaskFile("test", task);
    TaskFileDAO.save(file);

    assertNotNull(file.getId());
  }

  @Test
  public void shouldDelete() throws Exception {
    TaskFile file = new TaskFile("test", task);
    TaskFileDAO.save(file);

    String key = file.getKey();
    TaskFileDAO.delete(file);

    assertNull(TaskFileDAO.load(key));
  }

  @Test
  public void shouldDeleteAll() throws Exception {
    TaskFile file = new TaskFile("testA.txt", task);
    TaskFileDAO.save(file);
    file = new TaskFile("testB.txt", task);
    TaskFileDAO.save(file);

    TaskFileDAO.deleteAll(task);

    assertTrue(TaskFileDAO.files(task).isEmpty());
  }
}
