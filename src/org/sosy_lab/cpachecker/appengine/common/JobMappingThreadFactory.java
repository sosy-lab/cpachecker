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

import org.sosy_lab.cpachecker.appengine.entity.Job;

import com.google.appengine.api.ThreadManager;

/**
 * This is a very specific {@link ThreadFactory} that maps every thread it creates to
 * a specific {@link Job} instance.
 * The particular use case is the situation that every file system operation of
 * CPAchecker on Google App Engine depends on a Job instance. Therefore a Thread
 * can look up the Job it's mapped to when it needs to do file system operations.
 */
public class JobMappingThreadFactory implements ThreadFactory {

  private static final Object lock = new Object();
  private static volatile Map<Thread, Job> threadJobMap = new HashMap<>();

  /**
   * Registers a Job with a Thread.
   *
   * @param job The job to map to the Thread
   * @param thread The Thread to be mapped to the Job
   */
  public static void registerJobWithThread(Job job, Thread thread) {
    synchronized (lock) {
      threadJobMap.put(thread, job);
    }
  }

  /**
   * Returns the Job that is mapped to the Thread.
   *
   * @param thread The Thread that is mapped to the desired Job
   * @return The mapped Job or null if none is mapped
   */
  public static Job getJob(Thread thread) {
    synchronized (lock) {
      return threadJobMap.get(thread);
    }
  }

  /**
   * Returns the map that contains the Thread/Job pairs.
   * @return The map containing Thread/Job pairs.
   */
  public static Map<Thread, Job> getMap() {
    return threadJobMap;
  }

  /**
   * Returns a new Thread and maps it to the Job instance that is mapped to the
   * current Thread. Therefore creating kind of a parent-child relation where
   * the child maps to the same Job as the parent.
   */
  @Override
  public Thread newThread(Runnable pR) {
    Thread thread = ThreadManager.currentRequestThreadFactory().newThread(pR);
    registerJobWithThread(getJob(Thread.currentThread()), thread);
    return thread;
  }
}
