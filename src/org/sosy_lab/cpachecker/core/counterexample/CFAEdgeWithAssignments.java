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

import java.util.List;

import javax.annotation.Nullable;

import org.sosy_lab.cpachecker.cfa.ast.IAssignment;
import org.sosy_lab.cpachecker.cfa.ast.c.CAssignment;
import org.sosy_lab.cpachecker.cfa.model.CFAEdge;


public class CFAEdgeWithAssignments {

  private final CFAEdge edge;
  private final List<IAssignment> assignments;
  private final String comment;

  public CFAEdgeWithAssignments(CFAEdge pEdge, List<IAssignment> pAssignments, @Nullable String pComment) {
    assert pAssignments != null;
    edge = pEdge;
    assignments = pAssignments;
    comment = pComment;
  }

  public List<IAssignment> getAssignments() {
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

    for (IAssignment assignment : assignments) {
      if (assignment instanceof CAssignment) {
        result.append(assignment.toASTString());
      } else {
        return null;
      }
    }

    return result.toString();
  }

  @Nullable
  public String prettyPrintCode(int numberOfTabsPerLine) {

    if (assignments.size() == 0) {
      return null;
    }

    StringBuilder result = new StringBuilder();

    for (IAssignment assignment : assignments) {
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

  @Override
  public String toString() {
    return edge.toString() + " " + assignments.toString();
  }

  @Nullable
  public String getComment() {
    return comment;
  }
}