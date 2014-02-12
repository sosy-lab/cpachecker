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
import org.sosy_lab.cpachecker.appengine.entity.Job;
import org.sosy_lab.cpachecker.appengine.entity.JobFile;

import com.googlecode.objectify.Key;


public class JobFileDAOTest extends DatabaseTest {

  private Job job;

  @Override
  public void setUp() {
    super.setUp();
    job = new Job();
    JobDAO.save(job);
  }

  @Test
  public void shouldLoadByString() throws Exception {
    JobFile file = new JobFile("test", job);
    JobFileDAO.save(file);
    JobFile loaded = JobFileDAO.load(file.getKey());

    assertEquals(file, loaded);
  }

  @Test
  public void shouldLoadByKey() throws Exception {
    JobFile file = new JobFile("test", job);
    JobFileDAO.save(file);
    Key<JobFile> key = Key.create(file.getKey());
    JobFile loaded = JobFileDAO.load(key);

    assertEquals(file, loaded);
  }

  @Test
  public void shouldLoadByPath() throws Exception {
    JobFile file = new JobFile("test", job);
    JobFileDAO.save(file);
    JobFile loaded = JobFileDAO.loadByPath("test", job);

    assertEquals(file, loaded);
  }

  @Test
  public void shouldLoadByNameAndJob() throws Exception {
    JobFile file = new JobFile("foo/test.txt", job);
    JobFileDAO.save(file);
    JobFile loaded = JobFileDAO.loadByName("test.txt", job);

    assertEquals(file, loaded);
  }

  @Test
  public void shouldLoadByNameAndJobKeyString() throws Exception {
    JobFile file = new JobFile("foo/test.txt", job);
    JobFileDAO.save(file);
    JobFile loaded = JobFileDAO.loadByName("test.txt", job.getKey());

    assertEquals(file, loaded);
  }

  @Test
  public void shouldLoadAllFiles() throws Exception {
    JobFile file = new JobFile("testA.txt", job);
    JobFileDAO.save(file);
    file = new JobFile("testB.txt", job);
    JobFileDAO.save(file);

    assertEquals(2, JobFileDAO.files(job).size());
  }

  @Test
  public void shouldSave() throws Exception {
    JobFile file = new JobFile("test", job);
    JobFileDAO.save(file);

    assertNotNull(file.getId());
  }

  @Test
  public void shouldDelete() throws Exception {
    JobFile file = new JobFile("test", job);
    JobFileDAO.save(file);

    String key = file.getKey();
    JobFileDAO.delete(file);

    assertNull(JobFileDAO.load(key));
  }

  @Test
  public void shouldDeleteAll() throws Exception {
    JobFile file = new JobFile("testA.txt", job);
    JobFileDAO.save(file);
    file = new JobFile("testB.txt", job);
    JobFileDAO.save(file);

    JobFileDAO.deleteAll(job);

    assertTrue(JobFileDAO.files(job).isEmpty());
  }
}
