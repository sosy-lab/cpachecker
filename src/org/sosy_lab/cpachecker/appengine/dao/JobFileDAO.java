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

import java.util.List;

import org.sosy_lab.cpachecker.appengine.entity.Job;
import org.sosy_lab.cpachecker.appengine.entity.JobFile;

import com.googlecode.objectify.Key;
import com.googlecode.objectify.VoidWork;


public class JobFileDAO {

  public static JobFile load(String key) {
    try {
      Key<JobFile> fileKey = Key.create(key);
      return load(fileKey);
    } catch (IllegalArgumentException e) {
      return null;
    }
  }

  public static JobFile load(Key<JobFile> key) {
    return ofy().load().key(key).now();
  }

  public static JobFile loadByPath(String path, Job parent) {
    return ofy().load().type(JobFile.class).ancestor(parent).filter("path", path).first().now();
  }

  public static List<JobFile> files(Job parent) {
    return ofy().load().type(JobFile.class).ancestor(parent).list();
  }

  public static void save(JobFile file) {
    ofy().save().entity(file).now();
  }

  public static void delete(final JobFile file) {
    final Job parent = file.getJob();
    parent.removeFile(file);

    ofy().transact(new VoidWork() {

      @Override
      public void vrun() {
        ofy().save().entity(parent).now();
        ofy().delete().entity(file).now();
      }
    });
  }
}
