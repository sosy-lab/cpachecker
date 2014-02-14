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
package org.sosy_lab.cpachecker.appengine.entity;

import java.util.HashMap;
import java.util.Map;

import com.googlecode.objectify.Key;
import com.googlecode.objectify.annotation.EmbedMap;
import com.googlecode.objectify.annotation.Entity;
import com.googlecode.objectify.annotation.Id;
import com.googlecode.objectify.annotation.OnLoad;

@Entity
public class Taskset {

  @Id
  private Long id;
  @EmbedMap // key: task's key, value: is fully processed?
  private Map<String, Boolean> tasks = new HashMap<>();

  public String getKey() {
    return Key.create(Taskset.class, getId()).getString();
  }

  public long getId() {
    return id;
  }

  public void setId(long pId) {
    id = pId;
  }

  public Map<String, Boolean> getTasks() {
    return tasks;
  }

  public void setTasks(Map<String, Boolean> pTasks) {
    tasks = pTasks;
  }

  public void addTask(Task task) {
    addTask(task.getKey(), false);
  }

  public void setProcessed(String key) {
    addTask(key, true);
  }

  private void addTask(String key, boolean processed) {
    tasks.put(key, processed);
  }

  @OnLoad
  void instantiateMap() {
    if (tasks == null) {
      tasks = new HashMap<>();
    }
  }
}
