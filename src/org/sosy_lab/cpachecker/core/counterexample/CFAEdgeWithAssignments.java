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
import java.util.List;

import javax.annotation.Nullable;

import org.sosy_lab.cpachecker.cfa.ast.AAssignment;
import org.sosy_lab.cpachecker.cfa.ast.c.CAssignment;
import org.sosy_lab.cpachecker.cfa.model.CFAEdge;
import org.sosy_lab.cpachecker.cfa.types.c.CArrayType;
import org.sosy_lab.cpachecker.cfa.types.c.CPointerType;

/**
 * Contains the concrete values of assignments {@link AAssignment} for a
 * given statement, which is represented as cfa edge {@link CFAEdge},
 * in the error path.
 */
public class CFAEdgeWithAssignments {

  private final CFAEdge edge;
  private final List<AAssignment> assignments;
  private final String comment;

  public CFAEdgeWithAssignments(CFAEdge pEdge, List<AAssignment> pAssignments, @Nullable String pComment) {
    assert pAssignments != null;
    edge = pEdge;
    assignments = pAssignments;
    comment = pComment;
  }

  private CFAEdgeWithAssignments(CFAEdgeWithAssignments pEdgeWA, CFAEdgeWithAssignments pEdgeWA2) {
    assert pEdgeWA.edge.equals(pEdgeWA2.edge);

    edge = pEdgeWA.edge;

    List<AAssignment> assignments1 = pEdgeWA.getAssignments();
    List<AAssignment> assignments2 = pEdgeWA2.getAssignments();

    List<AAssignment> result = new ArrayList<>(pEdgeWA.assignments);

    for (AAssignment assignment2 : assignments2) {
      if (!assignments1.contains(assignment2)) {

        //TODO ugly, ignoring addresses
        if (!(assignment2.getLeftHandSide().getExpressionType() instanceof CPointerType)
            && !(assignment2.getLeftHandSide().getExpressionType() instanceof CArrayType)) {
          result.add(assignment2);
        }
      }
    }

    comment = pEdgeWA.comment;
    assignments = result;
  }

  public List<AAssignment> getAssignments() {
    return assignments;
  }

  public CFAEdge getCFAEdge() {
    return edge;
  }

  @Nullable
  public String getAsCode() {

    if (assignments.size() == 0) {
      return null;
    }

    StringBuilder result = new StringBuilder();

    for (AAssignment assignment : assignments) {
      if (assignment instanceof CAssignment) {
        result.append(((CAssignment) assignment).accept(CStatementToOriginalCodeVisitor.INSTANCE));
      } else {
        return null;
      }
    }

    return result.toString();
  }

  /**
   * Print code for user output only. Typedefs are not resolved.
   * Should not be parsed.
   *
   * @param numberOfTabsPerLine the number of tabs per line.
   * @return pretty-printed code
   */
  @Nullable
  public String prettyPrintCode(int numberOfTabsPerLine) {

    if (assignments.size() == 0) {
      return null;
    }

    StringBuilder result = new StringBuilder();

    for (AAssignment assignment : assignments) {
      if (assignment instanceof CAssignment) {
        for (int c = 0; c < numberOfTabsPerLine; c++) {
          result.append("\t");
        }
        result.append(assignment.toASTString());
        result.append(System.lineSeparator());
      } else {
        return null;
      }
    }

    return result.toString();
  }

  public String prettyPrint() {
    String assignments = this.prettyPrintCode(0);
    String comment = this.getComment();

    String result = "";

    if (assignments != null) {
      result = assignments;
    }

    if (comment != null) {
      result = result + comment;
    }

    return result;
  }

  public String printForHTML() {
    return prettyPrint().replace(System.lineSeparator(), "\n");
  }

  @Override
  public String toString() {
    return edge.toString() + " " + assignments.toString();
  }

  @Nullable
  public String getComment() {
    return comment;
  }

  public CFAEdgeWithAssignments mergeEdge(CFAEdgeWithAssignments pEdge) {
    return new CFAEdgeWithAssignments(this, pEdge);
  }
}