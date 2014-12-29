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
package org.sosy_lab.cpachecker.core.counterexample;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;

import org.sosy_lab.cpachecker.cfa.ast.AAssignment;
import org.sosy_lab.cpachecker.cfa.ast.AIdExpression;
import org.sosy_lab.cpachecker.cfa.ast.ALeftHandSide;
import org.sosy_lab.cpachecker.cfa.model.CFAEdge;
import org.sosy_lab.cpachecker.cfa.model.MultiEdge;

import com.google.common.collect.ImmutableList;

/**
 * This class is used if the error path contains multi edges {@link MultiEdge}.
 * Every edge {@link CFAEdge} of a multi edge has its own assignment edge {@link CFAEdgeWithAssignments}.
 */
public final class CFAMultiEdgeWithAssignments extends CFAEdgeWithAssignments implements Iterable<CFAEdgeWithAssignments> {

  private final List<CFAEdgeWithAssignments> edgesWithAssignment;

  private CFAMultiEdgeWithAssignments(MultiEdge pEdge, List<AAssignment> pAssignments,
      List<CFAEdgeWithAssignments> pEdges, String pComments) {
    super(pEdge, pAssignments, pComments);
    edgesWithAssignment = ImmutableList.copyOf(pEdges);
  }

  @Override
  public Iterator<CFAEdgeWithAssignments> iterator() {
    return getEdgesWithAssignment().iterator();
  }

  public List<CFAEdgeWithAssignments> getEdgesWithAssignment() {
    return edgesWithAssignment;
  }

  @Override
  public CFAEdgeWithAssignments mergeEdge(CFAEdgeWithAssignments pEdge) {

    if (!(pEdge instanceof CFAMultiEdgeWithAssignments)) {
      return this;
    }

    CFAMultiEdgeWithAssignments other = (CFAMultiEdgeWithAssignments) pEdge;

    assert pEdge.getCFAEdge().equals(getCFAEdge());
    assert other.edgesWithAssignment.size() == this.edgesWithAssignment.size();

    List<CFAEdgeWithAssignments> result = new ArrayList<>();
    Iterator<CFAEdgeWithAssignments> otherIt = other.iterator();

    for (CFAEdgeWithAssignments thisEdge : edgesWithAssignment) {
      result.add(thisEdge.mergeEdge(otherIt.next()));
    }

    return valueOf((MultiEdge) pEdge.getCFAEdge(), result);
  }

  public static final CFAMultiEdgeWithAssignments valueOf(MultiEdge pEdge, List<CFAEdgeWithAssignments> pEdges) {
    // In MultiEdges, it is possible to write the same variable multiple times.
    // This would produce illegal assumptions,
    // thus we filter out assignments which write to the same Address.
    LinkedHashMap<ALeftHandSide, AAssignment> assignments = new LinkedHashMap<>();

    for (CFAEdgeWithAssignments edge : pEdges) {
      for (AAssignment assignment : edge.getAssignments()) {
        ALeftHandSide leftHandSide = assignment.getLeftHandSide();
        // We don't evaluate addresses in c
        if (isDistinct(leftHandSide)) {
          assignments.put(assignment.getLeftHandSide(), assignment);
        }
      }
    }

    /*Comments only make sense in the exact location of an path*/
    return new CFAMultiEdgeWithAssignments(pEdge,
        ImmutableList.copyOf(assignments.values()), pEdges, null);
  }

  private static boolean isDistinct(ALeftHandSide pLeftHandSide) {
    //TODO Can be made more precise
    return pLeftHandSide instanceof AIdExpression;
  }
}