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
import java.util.Collection;
import java.util.List;

import javax.annotation.Nullable;

import org.sosy_lab.cpachecker.cfa.ast.AExpressionStatement;
import org.sosy_lab.cpachecker.cfa.ast.c.CExpressionStatement;
import org.sosy_lab.cpachecker.cfa.model.CFAEdge;

/**
 * Contains assumptions {@link AExpressionStatement} for a
 * given statement, which is represented as cfa edge {@link CFAEdge},
 * in the error path.
 */
public class CFAEdgeWithAssumptions {

  private final CFAEdge edge;
  private final Collection<AExpressionStatement> expressionStmts;
  private final String comment;

  /**
   * Creates a edge {@link CFAEdgeWithAssumptions} that contains concrete assumptions along the error path.
   *
   * @param pEdge The CFAEdge that represents a part of the errorpath.
   * @param pExpStmt The concrete assumptions represented as expression statements
   * @param pComment Further comments that should be given to the user about this part of the path but can't be represented as assumption.
   */
  public CFAEdgeWithAssumptions(CFAEdge pEdge, Collection<AExpressionStatement> pExpStmt, String pComment) {
    assert pExpStmt != null;
    assert pComment != null;
    edge = pEdge;
    expressionStmts = pExpStmt;
    comment = pComment;
  }

  private CFAEdgeWithAssumptions(CFAEdgeWithAssumptions pEdgeWA, CFAEdgeWithAssumptions pEdgeWA2) {
    assert pEdgeWA.edge.equals(pEdgeWA2.edge);

    /*
     * Constructor used when merging to edges.
     */
    edge = pEdgeWA.edge;

    Collection<AExpressionStatement> expStmts1 = pEdgeWA.getExpStmts();
    Collection<AExpressionStatement> expStmts2 = pEdgeWA2.getExpStmts();

    List<AExpressionStatement> result = new ArrayList<>(pEdgeWA.expressionStmts);

    for (AExpressionStatement expStmt2 : expStmts2) {
      if (!expStmts1.contains(expStmt2)) {
        result.add(expStmt2);
      }
    }

    comment = pEdgeWA.comment;
    expressionStmts = result;
  }

  public Collection<AExpressionStatement> getExpStmts() {
    return expressionStmts;
  }

  public CFAEdge getCFAEdge() {
    return edge;
  }

  /**
   * Represents the concrete assumptions this edge as C Code that can be parsed.
   *
   * @return C Code that represents the concrete assumptions of this edge.
   */
  public String getAsCode() {

    if (expressionStmts.size() == 0) {
      return "";
    }

    StringBuilder result = new StringBuilder();

    for (AExpressionStatement expressionStmt : expressionStmts) {
      if (expressionStmt instanceof CExpressionStatement) {
        result.append(((CExpressionStatement) expressionStmt).accept(CStatementToOriginalCodeVisitor.INSTANCE));
      } else {
        return "";
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
  public String prettyPrintCode(int numberOfTabsPerLine) {

    if (expressionStmts.size() == 0) {
      return "";
    }

    StringBuilder result = new StringBuilder();

    for (AExpressionStatement expStmt : expressionStmts) {
      if (expStmt instanceof CExpressionStatement) {
        for (int c = 0; c < numberOfTabsPerLine; c++) {
          result.append("\t");
        }
        result.append(expStmt.toASTString());
        result.append(System.lineSeparator());
      } else {
        return "";
      }
    }

    return result.toString();
  }

  /**
   * Returns a message that contain information of the concrete values pertaining to
   * this edge of the error path.
   *
   * @return returns a message that contain information of the concrete values pertaining to
   * this edge of the error path.
   */
  public String prettyPrint() {
    String expStmt = this.prettyPrintCode(0);
    String comment = this.getComment();
    return expStmt + comment;
  }

  /**
   * Get a message that can be used inside of html.
   *
   * @return returns a message that contain information of the concrete values pertaining to
   * this edge of the error path for a html page.
   */
  public String printForHTML() {
    return prettyPrint().replace(System.lineSeparator(), "\n");
  }

  @Override
  public String toString() {
    return edge.toString() + " " + expressionStmts.toString();
  }

  @Nullable
  public String getComment() {
    return comment;
  }

  /**
   * Try to merge two different edges {@link CFAEdgeWithAssumptions}.
   *
   * @param pEdge the other edge to be merged with this edge.
   * @return A Edge that contain both assumptions of the merged edges.
   */
  public CFAEdgeWithAssumptions mergeEdge(CFAEdgeWithAssumptions pEdge) {
    // FIXME this method is not matured, it just combines all assumptions
    return new CFAEdgeWithAssumptions(this, pEdge);
  }
}