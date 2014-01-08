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

import java.util.ArrayList;
import java.util.List;

import org.sosy_lab.cpachecker.appengine.entity.Job;
import org.sosy_lab.cpachecker.appengine.entity.JobFile;

import com.googlecode.objectify.Key;
import com.googlecode.objectify.ObjectifyService;
import com.googlecode.objectify.Ref;
import com.googlecode.objectify.VoidWork;


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

  public static void delete(final Job job) {
    ofy().transact(new VoidWork() {

      @Override
      public void vrun() {
        List<Key<JobFile>> keys = new ArrayList<>();
        for (Ref<JobFile> file : job.getFiles()) {
          keys.add(file.getKey());
        }

        ofy().delete().keys(keys).now();
        ofy().delete().entities(job).now();
      }
    });
  }

  public static Key<Job> allocateKey() {
    return ObjectifyService.factory().allocateId(Job.class);
  }

}
