/*
 *  CPAchecker is a tool for configurable software verification.
 *  This file is part of CPAchecker.
 *
 *  Copyright (C) 2007-2016  Dirk Beyer
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
package org.sosy_lab.cpachecker.cpa.thread;


public class ThreadLabel implements Comparable<ThreadLabel> {

  public enum LabelStatus {
    PARENT_THREAD,
    CREATED_THREAD,
    SELF_PARALLEL_THREAD;
  }

  private final String threadName;
  private final LabelStatus status;

  public ThreadLabel(String name, LabelStatus flag) {
    threadName = name;
    status = flag;
  }

  @Override
  public int hashCode() {
    final int prime = 31;
    int result = 1;
    result = prime * result + status.hashCode();
    result = prime * result + ((threadName == null) ? 0 : threadName.hashCode());
    return result;
  }

  @Override
  public boolean equals(Object obj) {
    if (this == obj) {
      return true;
    }
    if (obj == null) {
      return false;
    }
    if (getClass() != obj.getClass()) {
      return false;
    }
    ThreadLabel other = (ThreadLabel) obj;
    if (status != other.status) {
      return false;
    }
    if (threadName == null) {
      if (other.threadName != null) {
        return false;
      }
    } else if (!threadName.equals(other.threadName)) {
      return false;
    }
    return true;
  }

  @Override
  public int compareTo(ThreadLabel pO) {
    int result = this.threadName.compareTo(pO.threadName);
    if (result != 0) {
      return result;
    } else {
      return status.compareTo(pO.status);
    }
  }

  public boolean isCompatibleWith(ThreadLabel other) {
    if (status != LabelStatus.SELF_PARALLEL_THREAD && status == other.status) {
      return false;
    }
    if (threadName == null) {
      if (other.threadName != null) {
        return false;
      }
    } else if (!threadName.equals(other.threadName)) {
      return false;
    }
    return true;
  }

  public String getName() {
    return threadName;
  }

  public boolean isSelfParallel() {
    return status == LabelStatus.SELF_PARALLEL_THREAD;
  }

  public ThreadLabel toSelfParallelLabel() {
    return new ThreadLabel(threadName, LabelStatus.SELF_PARALLEL_THREAD);
  }

  @Override
  public String toString() {
    return threadName + ":" + status;
  }
}
