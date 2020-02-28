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

import com.google.common.collect.ImmutableMap;
import java.util.Map;
import java.util.Objects;
import org.sosy_lab.cpachecker.core.interfaces.AbstractStateWithEdge;
import org.sosy_lab.cpachecker.cpa.usage.CompatibleState;

public class ThreadTMStateWithEdge extends ThreadTMState implements AbstractStateWithEdge {

  private final ThreadAbstractEdge edge;

  ThreadTMStateWithEdge(
      String pCurrent,
      Map<String, ThreadStatus> Tset,
      ImmutableMap<ThreadLabel, ThreadStatus> Rset,
      ThreadAbstractEdge pEdge) {
    super(pCurrent, Tset, Rset);
    edge = pEdge;
  }

  @Override
  public boolean hasEmptyEffect() {
    return edge == null;
  }

  @Override
  public ThreadAbstractEdge getAbstractEdge() {
    return edge;
  }

  @Override
  public boolean isProjection() {
    return false;
  }

  @Override
  public int hashCode() {
    return 31 * super.hashCode() + Objects.hash(edge);
  }

  @Override
  public boolean equals(Object obj) {
    if (this == obj) {
      return true;
    }
    if (!super.equals(obj)) {
      return false;
    }
    ThreadTMStateWithEdge other = (ThreadTMStateWithEdge) obj;
    return Objects.equals(edge, other.edge);
  }

  @Override
  public int compareTo(CompatibleState pOther) {
    int result = super.compareTo(pOther);
    ThreadTMStateWithEdge other = (ThreadTMStateWithEdge) pOther;

    if (result != 0) {
      return result;
    }
    result = this.edge.getAction().getSecond().compareTo(other.edge.getAction().getSecond());
    return result;
  }
}

