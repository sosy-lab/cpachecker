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
package org.sosy_lab.cpachecker.util.dependencegraph.edges;

import java.io.Serializable;
import org.sosy_lab.cpachecker.util.dependencegraph.DGEdge;
import org.sosy_lab.cpachecker.util.dependencegraph.DGEdgeVisitor;
import org.sosy_lab.cpachecker.util.dependencegraph.DGNode;
import org.sosy_lab.cpachecker.util.dependencegraph.DependenceGraph;

/**
 * Dependence edge that represents a control dependence.
 *
 * <p>A node i is control dependent on a node j, iff:
 *
 * <ul>
 *   <li>there is a path from j to i on which each node but j is post-dominated by i, and
 *   <li>j is not post-dominated by i
 * </ul>
 *
 * @see DependenceGraph
 */
public class ControlDependenceEdge extends DGEdge implements Serializable {

  private static final long serialVersionUID = -4062423413603427004L;

  public ControlDependenceEdge(DGNode pStart, DGNode pEnd) {
    super(pStart, pEnd);
  }

  @Override
  public <T> T accept(DGEdgeVisitor<T> pVisitor) {
    return pVisitor.visit(this);
  }
}
