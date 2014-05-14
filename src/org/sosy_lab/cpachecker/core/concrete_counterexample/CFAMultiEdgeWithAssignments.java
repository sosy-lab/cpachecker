/*
 *  CPAchecker is a tool for configurable software verification.
 *  This file is part of CPAchecker.
 *
 *  Copyright (C) 2007-2014  Dirk Beyer
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
package org.sosy_lab.cpachecker.core.concrete_counterexample;

import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

import org.sosy_lab.cpachecker.cfa.model.MultiEdge;

import com.google.common.collect.ImmutableList;


public final class CFAMultiEdgeWithAssignments extends CFAEdgeWithAssignments implements Iterable<CFAEdgeWithAssignments> {

  private final List<CFAEdgeWithAssignments> edgesWithAssignment;

  private CFAMultiEdgeWithAssignments(MultiEdge pEdge, Set<Assignment> pAssignments,
      String pEdgeCode, List<CFAEdgeWithAssignments> pEdges) {
    super(pEdge, pAssignments, pEdgeCode);
    edgesWithAssignment = ImmutableList.copyOf(pEdges);
  }

  @Override
  public Iterator<CFAEdgeWithAssignments> iterator() {
    return getEdgesWithAssignment().iterator();
  }

  public List<CFAEdgeWithAssignments> getEdgesWithAssignment() {
    return edgesWithAssignment;
  }

  public static final CFAMultiEdgeWithAssignments valueOf(MultiEdge pEdge, List<CFAEdgeWithAssignments> pEdges) {

    Set<Assignment> assignments = new HashSet<>();
    StringBuilder edgeCodeBuilder = new StringBuilder();

    for (CFAEdgeWithAssignments edge : pEdges) {
      assignments.addAll(edge.getAssignments());
      /*In MultiEdges, it is possible to write the same variable multiple times.
       *This means, the order of the statements is essential.*/

      String singleEdgeCode = edge.getAsCode();

      if(singleEdgeCode != null) {
        edgeCodeBuilder = edgeCodeBuilder.append(edge.getAsCode() + "\n");
      }
    }

    String edgeCode = edgeCodeBuilder.toString();

    if (edgeCode.isEmpty()) {
      edgeCode = null;
    } else {
      edgeCode = edgeCodeBuilder.deleteCharAt(edgeCodeBuilder.length() - 1).toString();
    }

    return new CFAMultiEdgeWithAssignments(pEdge, assignments, edgeCode, pEdges);
  }
}