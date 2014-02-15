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

import org.sosy_lab.cpachecker.appengine.entity.Taskset;

import com.googlecode.objectify.Key;

/**
 * This class provides methods for loading, saving and deletion of {@link Taskset}
 * instances.
 */
public class TasksetDAO {

  /**
   * Retrieves and returns a {@link Taskset} with the given key.
   *
   * @param key The key of the desired {@link Taskset}
   * @return The desired {@link Taskset} or null if it cannot be found
   */
  public static Taskset load(String key) {
    try {
      Key<Taskset> taskKey = Key.create(key);
      return ofy().load().key(taskKey).now();
    } catch (IllegalArgumentException e) {
      return null;
    }
  }

  /**
   * Saves the given {@link Taskset}.
   *
   * @param task The {@link Taskset} to save
   * @return The saved {@link Taskset}
   */
  public static Taskset save(Taskset taskset) {
    ofy().save().entity(taskset).now();
    return taskset;
  }

  /**
   * Deletes the given {@link Taskset}.
   *
   * @param taskset The {@link Taskset} to delete
   */
  public static void delete(Taskset taskset) {
    ofy().delete().entity(taskset).now();
  }
}
