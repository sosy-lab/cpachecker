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

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map.Entry;

import org.sosy_lab.cpachecker.appengine.entity.Task;
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
   * Returns a collection of {@link Task}s that are associated with the given {@link Taskset}
   *
   * @param taskset The {@link Taskset} to retrieve the {@link Task}s for
   * @return A collection of {@link Task}s
   */
  public static Collection<Task> tasks(Taskset taskset) {
    List<Key<Task>> keys = new ArrayList<>();
    for (String key : taskset.getTasks().keySet()) {
      Key<Task> taskKey = Key.create(key);
      keys.add(taskKey);
    }
    return ofy().load().keys(keys).values();
  }

  /**
   * Returns a collection of {@link Task}s that are associated with the given {@link Taskset}
   * and are either marked as processed or not.
   *
   * @param taskset The {@link Taskset} to retrieve the {@link Task}s for
   * @param processed True, if only processed {@link Task}s will be retrieved,
   *                  false if only un-processed {@link Task}s will be retrieved
   * @return
   */
  public static Collection<Task> tasks(Taskset taskset, boolean processed) {
    List<Key<Task>> keys = new ArrayList<>();
    for (Entry<String, Boolean> entry : taskset.getTasks().entrySet()) {
      if (entry.getValue() == processed) {
        Key<Task> taskKey = Key.create(entry.getKey());
        keys.add(taskKey);
      }
    }
    return ofy().load().keys(keys).values();
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
