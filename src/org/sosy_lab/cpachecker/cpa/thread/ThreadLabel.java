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

import java.util.Objects;

public final class ThreadLabel implements Comparable<ThreadLabel> {

  public enum LabelStatus {
    PARENT_THREAD,
    CREATED_THREAD,
    SELF_PARALLEL_THREAD;
  }

  private final String threadName;
  private final String varName;
  // private final LabelStatus status;

  public ThreadLabel(String name, String vName) {
    threadName = name;
    varName = vName;
  }

  @Override
  public int hashCode() {
    return Objects.hash(varName, threadName);
  }

  @Override
  public boolean equals(Object obj) {
    if (this == obj) {
      return true;
    }
    if (obj == null ||
        getClass() != obj.getClass()) {
      return false;
    }
    ThreadLabel other = (ThreadLabel) obj;
    return Objects.equals(threadName, other.threadName)
        && Objects.equals(varName, other.varName);
  }

  @Override
  public int compareTo(ThreadLabel pO) {
    int result = this.threadName.compareTo(pO.threadName);
    if (result != 0) {
      return result;
    }
    return this.varName.compareTo(pO.varName);
  }

  public String getName() {
    return threadName;
  }

  public String getVarName() {
    return varName;
  }

  @Override
  public String toString() {
    return "(" + threadName + ", " + varName + ")";
  }
}
