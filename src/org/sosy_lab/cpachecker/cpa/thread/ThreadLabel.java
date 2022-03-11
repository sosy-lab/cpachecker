// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2007-2020 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

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
    if (obj == null || getClass() != obj.getClass()) {
      return false;
    }
    ThreadLabel other = (ThreadLabel) obj;
    return Objects.equals(threadName, other.threadName) && Objects.equals(varName, other.varName);
  }

  @Override
  public int compareTo(ThreadLabel pO) {
    int result = threadName.compareTo(pO.threadName);
    if (result != 0) {
      return result;
    }
    return varName.compareTo(pO.varName);
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
