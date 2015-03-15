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

import org.sosy_lab.cpachecker.cfa.ast.ABinaryExpression;
import org.sosy_lab.cpachecker.cfa.ast.AExpression;
import org.sosy_lab.cpachecker.cfa.ast.AExpressionStatement;
import org.sosy_lab.cpachecker.cfa.ast.AIdExpression;
import org.sosy_lab.cpachecker.cfa.model.CFAEdge;
import org.sosy_lab.cpachecker.cfa.model.MultiEdge;

import com.google.common.collect.ImmutableList;

/**
 * This class is used if the error path contains multi edges {@link MultiEdge}.
 * Every edge {@link CFAEdge} of a multi edge has its own {@link CFAEdgeWithAssumptions} edge.
 */
public final class CFAMultiEdgeWithAssumptions extends CFAEdgeWithAssumptions implements Iterable<CFAEdgeWithAssumptions> {

  private final List<CFAEdgeWithAssumptions> edgesWithAssignment;

  private CFAMultiEdgeWithAssumptions(MultiEdge pEdge, List<AExpressionStatement> pAssignments,
      List<CFAEdgeWithAssumptions> pEdges, String pComments) {
    super(pEdge, pAssignments, pComments);
    edgesWithAssignment = ImmutableList.copyOf(pEdges);
  }

  @Override
  public Iterator<CFAEdgeWithAssumptions> iterator() {
    return getEdgesWithAssignment().iterator();
  }

  public List<CFAEdgeWithAssumptions> getEdgesWithAssignment() {
    return edgesWithAssignment;
  }

  @Override
  public CFAEdgeWithAssumptions mergeEdge(CFAEdgeWithAssumptions pEdge) {

    if (!(pEdge instanceof CFAMultiEdgeWithAssumptions)) {
      return this;
    }

    CFAMultiEdgeWithAssumptions other = (CFAMultiEdgeWithAssumptions) pEdge;

    assert pEdge.getCFAEdge().equals(getCFAEdge());
    assert other.edgesWithAssignment.size() == this.edgesWithAssignment.size();

    List<CFAEdgeWithAssumptions> result = new ArrayList<>();
    Iterator<CFAEdgeWithAssumptions> otherIt = other.iterator();

    for (CFAEdgeWithAssumptions thisEdge : edgesWithAssignment) {
      result.add(thisEdge.mergeEdge(otherIt.next()));
    }

    return valueOf((MultiEdge) pEdge.getCFAEdge(), result);
  }

  /**
   *  Imprecisely projects the assumptions of the edges in a multi edge {@link MultiEdge} to the end of the multi edge
   *  in the error path. Creates a edge that also contains the assumptions of the edges in the multi edge.
   *
   * @param pEdge The multi edge in the error path.
   * @param pEdges The edges and assumptions that are contained in the given multi edge.
   * @return A edge {@link CFAMultiEdgeWithAssumptions} that contain the assumptions of
   * the edges that are contained in the given multi edge.
   */
  public static CFAMultiEdgeWithAssumptions valueOf(MultiEdge pEdge, List<CFAEdgeWithAssumptions> pEdges) {
    // In MultiEdges, it is possible to write the same variable multiple times.
    // This would produce illegal assumptions,
    // thus we filter out assignments which write to the same Address.
    LinkedHashMap<AExpression, AExpressionStatement> assignments = new LinkedHashMap<>();

    for (CFAEdgeWithAssumptions edge : pEdges) {
      for (AExpressionStatement assignment : edge.getExpStmts()) {
        AExpression expression = assignment.getExpression();
        if (expression instanceof ABinaryExpression) {
          ABinaryExpression binExp = (ABinaryExpression) expression;
          // We don't evaluate addresses in c
          if (isDistinct(binExp)) {
            assignments.put(binExp.getOperand1(), assignment);
          }
        } else {
          throw new AssertionError("Assumption have to be binary expressions");
        }
      }
    }

    /*Comments only make sense in the exact location of an path*/
    return new CFAMultiEdgeWithAssumptions(pEdge,
        ImmutableList.copyOf(assignments.values()), pEdges, "");
  }

  private static boolean isDistinct(ABinaryExpression pBinExp) {
    //TODO Can be made more precise
    return pBinExp.getOperand1() instanceof AIdExpression;
  }
}