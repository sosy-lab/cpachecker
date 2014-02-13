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
package org.sosy_lab.cpachecker.appengine.common;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ThreadFactory;

import org.sosy_lab.cpachecker.appengine.entity.Task;

import com.google.appengine.api.ThreadManager;

/**
 * This is a very specific {@link ThreadFactory} that maps every {@link Thread}
 * it creates to a specific {@link Task} instance.
 * The particular use case is the situation that every file system operation of
 * CPAchecker on Google App Engine depends on a {@link Task} instance.
 * Therefore a {@link Thread} can look up the {@link Task} it's mapped to when
 * it needs to do file system operations.
 */
public class TaskMappingThreadFactory implements ThreadFactory {

  private static final Object lock = new Object();
  private static volatile Map<Thread, Task> threadTaskMap = new HashMap<>();

  /**
   * Registers a {@link Task} with a {@link Thread}.
   *
   * @param task The {@link Task} to map to the {@link Thread}
   * @param thread The {@link Thread} to be mapped to the {@link Task}
   */
  public static void registerTaskWithThread(Task task, Thread thread) {
    synchronized (lock) {
      threadTaskMap.put(thread, task);
    }
  }

  /**
   * Returns the {@link Task} that is mapped to the {@link Thread}.
   *
   * @param thread The {@link Thread} that is mapped to the desired {@link Task}
   * @return The mapped {@link Task} or null if none is mapped
   */
  public static Task getTask(Thread thread) {
    synchronized (lock) {
      return threadTaskMap.get(thread);
    }
  }

  /**
   * Returns the map that contains the {@link Thread}/{@link Task} pairs.
   * @return The map containing {@link Thread}/{@link Task} pairs.
   */
  public static Map<Thread, Task> getMap() {
    return threadTaskMap;
  }

  /**
   * Returns a new {@link Thread} and maps it to the {@link Task} instance that
   * is mapped to the current {@link Thread}. Therefore creating a kind of a
   * parent-child relation where the child maps to the same {@link Task} as the parent.
   */
  @Override
  public Thread newThread(Runnable pR) {
    Thread thread = ThreadManager.currentRequestThreadFactory().newThread(pR);
    registerTaskWithThread(getTask(Thread.currentThread()), thread);
    return thread;
  }
}
