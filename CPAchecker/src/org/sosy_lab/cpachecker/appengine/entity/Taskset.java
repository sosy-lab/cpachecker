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

import java.util.LinkedList;
import java.util.List;

import com.googlecode.objectify.Key;
import com.googlecode.objectify.annotation.Entity;
import com.googlecode.objectify.annotation.Id;

@Entity
public class Taskset {

  @Id
  private Long id;
  private List<String> tasks = new LinkedList<>();

  public String getKey() {
    return Key.create(Taskset.class, getId()).getString();
  }

  public long getId() {
    return id;
  }

  public void setId(long pId) {
    id = pId;
  }

  public List<String> getTasks() {
    return tasks;
  }

  public void setTasks(List<String> pTasks) {
    tasks = pTasks;
  }

  public void addTask(Task task) {
    tasks.add(task.getKey());
  }
}
