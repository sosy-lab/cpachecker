/*
 *  CPAchecker is a tool for configurable software verification.
 *  This file is part of CPAchecker.
 *
 *  Copyright (C) 2007-2017  Dirk Beyer
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
package org.sosy_lab.cpachecker.cpa.arg.witnessexport;

import java.util.Objects;
import org.sosy_lab.cpachecker.cfa.model.CFAEdge;
import org.sosy_lab.cpachecker.cpa.arg.ARGState;

class DelayedAssignmentsKey {

  private final String from;

  private final CFAEdge edge;

  private final ARGState state;

  public DelayedAssignmentsKey(String pFrom, CFAEdge pEdge, ARGState pState) {
    this.from = pFrom;
    this.edge = pEdge;
    this.state = pState;
  }

  @Override
  public int hashCode() {
    return Objects.hash(from, edge, state);
  }

  @Override
  public boolean equals(Object pObj) {
    if (this == pObj) {
      return true;
    }
    if (pObj instanceof DelayedAssignmentsKey) {
      DelayedAssignmentsKey other = (DelayedAssignmentsKey) pObj;
      return Objects.equals(from, other.from)
          && Objects.equals(edge, other.edge)
          && Objects.equals(state, other.state);
    }
    return false;
  }
}
