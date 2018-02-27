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
package org.sosy_lab.cpachecker.util.dependencegraph;

import java.io.Serializable;
import java.util.Objects;

/** Directed edge of a dependence graph. Connects two {@link DGNode DGNodes}. */
public abstract class DGEdge implements Serializable {

  private static final long serialVersionUID = -9021781132707333548L;

  private final DGNode start;
  private final DGNode end;

  public DGEdge(final DGNode pStart, final DGNode pEnd) {
    start = pStart;
    end = pEnd;
  }

  public DGNode getStart() {
    return start;
  }

  public DGNode getEnd() {
    return end;
  }

  @Override
  public boolean equals(Object pO) {
    if (this == pO) {
      return true;
    }
    if (pO == null || getClass() != pO.getClass()) {
      return false;
    }
    DGEdge dgEdge = (DGEdge) pO;
    return Objects.equals(start, dgEdge.start) && Objects.equals(end, dgEdge.end);
  }

  @Override
  public int hashCode() {
    return Objects.hash(start, end);
  }

  @Override
  public String toString() {
    return start + " -> " + end;
  }

  public abstract <T> T accept(DGEdgeVisitor<T> pVisitor);
}
