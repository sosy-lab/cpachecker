/*
 * CPAchecker is a tool for configurable software verification.
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
package org.sosy_lab.cpachecker.cpa.propertyscope;

import org.sosy_lab.cpachecker.cfa.model.CFAEdge;

import java.util.List;
import java.util.Objects;
import java.util.Set;

public class ScopeLocation {
  private final CFAEdge edge;
  private final List<String> callstack;
  private final Reason reason;

  public enum Reason {
    AUTOMATON_MATCH,
    ABS_FORMULA_VAR_CLASSIFICATION,
    ABS_FORMULA_VAR_CLASSIFICATION_FORMULA_CHANGE,
    ABS_FORMULA_IMPLICATION,
    ABS_FORMULA
  }

  public ScopeLocation(CFAEdge pEdge, List<String> pCallstack, Reason pReason) {
    edge = pEdge;
    callstack = pCallstack;
    reason = pReason;
  }

  public ScopeLocation copyWithReason(Reason pReason) {
    return new ScopeLocation(edge, callstack, pReason);
  }

  public CFAEdge getEdge() {
    return edge;
  }

  public List<String> getCallstack() {
    return callstack;
  }

  public Reason getReason() {
    return reason;
  }

  @Override
  public String toString() {
    return "ScopeLocation{" +
        "edge=" + edge +
        ", callstack=" + callstack +
        ", reason=" + reason +
        '}';
  }

  @Override
  public boolean equals(Object pO) {
    if (this == pO) {
      return true;
    }
    if (pO == null || getClass() != pO.getClass()) {
      return false;
    }
    ScopeLocation that = (ScopeLocation) pO;
    return Objects.equals(edge, that.edge) &&
        Objects.equals(callstack, that.callstack) &&
        reason == that.reason;
  }

  @Override
  public int hashCode() {
    return Objects.hash(edge, callstack, reason);
  }
}
