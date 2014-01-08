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

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.sosy_lab.cpachecker.appengine.entity.Job;
import org.sosy_lab.cpachecker.appengine.entity.JobFile;

import com.google.appengine.tools.development.testing.LocalDatastoreServiceTestConfig;
import com.google.appengine.tools.development.testing.LocalServiceTestHelper;
import com.googlecode.objectify.Key;
import com.googlecode.objectify.ObjectifyService;


public class JobDAOTest {

  private final LocalServiceTestHelper helper = new LocalServiceTestHelper(new LocalDatastoreServiceTestConfig());

  static {
    ObjectifyService.register(Job.class);
    ObjectifyService.register(JobFile.class);
  }

  @Before
  public void setUp() {
    helper.setUp();
  }

  @After
  public void tearDown() {
    helper.tearDown();
  }

  @Test
  public void shouldSaveJob() {
    Job job = new Job();
    JobDAO.save(job);

    assertTrue(job.getId() != null);
  }

  @Test
  public void shouldLoadJob() throws Exception {
    Job job = new Job();
    JobDAO.save(job);
    Job loaded = JobDAO.load(JobDAO.key(job));

    assertEquals(job, loaded);
  }

  @Test
  public void shouldDeleteJob() throws Exception {
    Job job = new Job(1L);
    JobFile file = new JobFile("", job);
    JobFileDAO.save(file);
    job.addFile(file);
    JobDAO.save(job);
    Key<Job> jobKey = Key.create(job);
    Key<JobFile> fileKey = Key.create(file);
    JobDAO.delete(job);

    assertTrue(JobDAO.load(jobKey) == null);
    assertTrue(JobFileDAO.load(fileKey) == null);
  }
}