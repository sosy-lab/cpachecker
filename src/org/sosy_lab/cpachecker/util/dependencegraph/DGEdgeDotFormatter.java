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

import org.sosy_lab.cpachecker.util.dependencegraph.edges.ControlDependenceEdge;
import org.sosy_lab.cpachecker.util.dependencegraph.edges.FlowDependenceEdge;

/** Formats a {@link DGEdge} for GraphViz dot output. */
class DGEdgeDotFormatter implements DGEdgeVisitor<String> {

  private final DGNodeDotFormatter nodeFormatter = new DGNodeDotFormatter();

  @Override
  public String visit(final FlowDependenceEdge pEdge) {
    return getNodeRepresentation(pEdge.getStart()) + " -> " + getNodeRepresentation(pEdge.getEnd());
  }

  @Override
  public String visit(final ControlDependenceEdge pEdge) {
    return getNodeRepresentation(pEdge.getStart())
        + " -> "
        + getNodeRepresentation(pEdge.getEnd())
        + " [style=dashed]";
  }

  private String getNodeRepresentation(final DGNode pNode) {
    return nodeFormatter.getNodeRepresentation(pNode);
  }
}
