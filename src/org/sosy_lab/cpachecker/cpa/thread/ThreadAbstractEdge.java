// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2007-2020 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.cpa.thread;

import java.util.Objects;
import org.sosy_lab.cpachecker.core.interfaces.AbstractEdge;
import org.sosy_lab.cpachecker.util.Pair;

public class ThreadAbstractEdge implements AbstractEdge {
  public static enum ThreadAction {
    CREATE,
    JOIN
  }

  // Environment action
  private final Pair<ThreadAction, String> action;

  ThreadAbstractEdge(ThreadAction pAction, String pThread) {
    action = Pair.of(pAction, pThread);
  }

  public Pair<ThreadAction, String> getAction() {
    return action;
  }

  @Override
  public int hashCode() {
    return Objects.hash(action);
  }

  @Override
  @SuppressWarnings("EqualsGetClass") // on purpose, case-class structure with single equals()
  public final boolean equals(Object obj) {
    if (this == obj) {
      return true;
    }
    if (!(obj instanceof ThreadAbstractEdge)) {
      return false;
    }
    ThreadAbstractEdge other = (ThreadAbstractEdge) obj;
    return Objects.equals(action, other.action);
  }

  @Override
  public String toString() {
    return action.toString();
  }

}
