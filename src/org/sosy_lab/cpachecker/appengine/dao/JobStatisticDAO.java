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

import org.sosy_lab.cpachecker.appengine.entity.JobStatistic;

import com.googlecode.objectify.Key;


public class JobStatisticDAO {

  public static JobStatistic load(String key) {
    try {
      Key<JobStatistic> statsKey = Key.create(key);
      return load(statsKey);
    } catch (IllegalArgumentException e) {
      return null;
    }
  }

  public static JobStatistic load(Key<JobStatistic> key) {
    return ofy().load().key(key).now();
  }

  public static void save(JobStatistic stats) {
    ofy().save().entity(stats).now();
  }
}
