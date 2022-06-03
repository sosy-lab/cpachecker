// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2007-2020 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.core.counterexample;

import com.google.common.collect.ImmutableCollection;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableSet;
import java.util.Objects;
import java.util.Set;
import org.checkerframework.checker.nullness.qual.Nullable;
import org.sosy_lab.cpachecker.cfa.ast.AExpressionStatement;
import org.sosy_lab.cpachecker.cfa.ast.c.CExpressionStatement;
import org.sosy_lab.cpachecker.cfa.model.CFAEdge;

/**
 * Contains assumptions {@link AExpressionStatement} for a given statement, which is represented as
 * cfa edge {@link CFAEdge}, in the error path.
 */
public class CFAEdgeWithAssumptions {

  private final CFAEdge edge;
  private final ImmutableList<AExpressionStatement> expressionStmts;
  private final String comment;

  /**
   * Creates a edge {@link CFAEdgeWithAssumptions} that contains concrete assumptions along the
   * error path.
   *
   * @param pEdge The CFAEdge that represents a part of the errorpath.
   * @param pExpStmt The concrete assumptions represented as expression statements
   * @param pComment Further comments that should be given to the user about this part of the path
   *     but can't be represented as assumption.
   */
  public CFAEdgeWithAssumptions(
      CFAEdge pEdge, ImmutableCollection<AExpressionStatement> pExpStmt, String pComment) {
    edge = Objects.requireNonNull(pEdge);
    expressionStmts = pExpStmt.asList();
    comment = Objects.requireNonNull(pComment);
  }

  private CFAEdgeWithAssumptions(CFAEdgeWithAssumptions pEdgeWA, CFAEdgeWithAssumptions pEdgeWA2) {
    assert pEdgeWA.edge.equals(pEdgeWA2.edge);

    /*
     * Constructor used when merging to edges.
     */
    edge = pEdgeWA.edge;

    Set<AExpressionStatement> expStmts1 = ImmutableSet.copyOf(pEdgeWA.getExpStmts());
    ImmutableList.Builder<AExpressionStatement> result = ImmutableList.builder();
    result.addAll(pEdgeWA.getExpStmts());

    for (AExpressionStatement expStmt2 : pEdgeWA2.getExpStmts()) {
      if (!expStmts1.contains(expStmt2)) {
        result.add(expStmt2);
      }
    }

    comment = pEdgeWA.comment;
    expressionStmts = result.build();
  }

  public ImmutableList<AExpressionStatement> getExpStmts() {
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

    if (expressionStmts.isEmpty()) {
      return "";
    }

    StringBuilder result = new StringBuilder();

    for (AExpressionStatement expressionStmt : expressionStmts) {
      if (expressionStmt instanceof CExpressionStatement) {
        result.append(
            ((CExpressionStatement) expressionStmt)
                .accept(CStatementToOriginalCodeVisitor.INSTANCE));
      } else {
        return "";
      }
    }

    return result.toString();
  }

  /**
   * Print code for user output only. Typedefs are not resolved. Should not be parsed.
   *
   * @param numberOfTabsPerLine the number of tabs per line.
   * @return pretty-printed code
   */
  public String prettyPrintCode(int numberOfTabsPerLine) {

    if (expressionStmts.isEmpty()) {
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

  @Override
  public String toString() {
    StringBuilder str = new StringBuilder(edge.toString());
    for (AExpressionStatement assum : expressionStmts) {
      str.append("\n\t").append(assum);
    }
    return str.toString();
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

  @Override
  public int hashCode() {
    return Objects.hash(comment, edge, expressionStmts);
  }

  @Override
  public boolean equals(Object obj) {
    if (this == obj) {
      return true;
    }
    if (obj instanceof CFAEdgeWithAssumptions) {
      CFAEdgeWithAssumptions other = (CFAEdgeWithAssumptions) obj;
      return comment.equals(other.comment)
          && edge.equals(other.edge)
          && expressionStmts.equals(other.expressionStmts);
    }
    return false;
  }
}
