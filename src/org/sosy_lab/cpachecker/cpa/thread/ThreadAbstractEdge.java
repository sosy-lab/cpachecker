/*
 *  CPAchecker is a tool for configurable software verification.
 *  This file is part of CPAchecker.
 *
 *  Copyright (C) 2007-2019  Dirk Beyer
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
 */
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
  public boolean equals(Object obj) {
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
