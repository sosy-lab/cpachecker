// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2007-2020 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.cfa.model;

import java.util.Optional;
import org.sosy_lab.cpachecker.cfa.ast.AAstNode;
import org.sosy_lab.cpachecker.cfa.ast.AExpression;
import org.sosy_lab.cpachecker.cfa.ast.FileLocation;
import org.sosy_lab.cpachecker.core.interfaces.ConfigurableProgramAnalysis;
import org.sosy_lab.cpachecker.core.interfaces.TransferRelation;

public class AssumeEdge extends AbstractCFAEdge {

  private static final long serialVersionUID = 1953381509820035275L;
  private final boolean truthAssumption;
  private final boolean swapped;
  private final boolean artificialIntermediate;
  protected final AExpression expression;

  /**
   * Create instance.
   *
   * @param pTruthAssumption If set to false, the expression is assumed to be negated.
   * @param pSwapped {@code true} if the value of {@code pTruthAssumption} corresponds inversely to
   *     the then/else branches of the branching statement in the source code.
   * @param pArtificialIntermediate {@code true} if the edge is an artificially introduced
   *     intermediate edge belonging to a larger assume condition.
   */
  protected AssumeEdge(
      String pRawStatement,
      FileLocation pFileLocation,
      CFANode pPredecessor,
      CFANode pSuccessor,
      AExpression pExpression,
      boolean pTruthAssumption,
      boolean pSwapped,
      boolean pArtificialIntermediate) {

    super("[" + pRawStatement + "]", pFileLocation, pPredecessor, pSuccessor);
    truthAssumption = pTruthAssumption;
    expression = pExpression;
    swapped = pSwapped;
    artificialIntermediate = pArtificialIntermediate;
  }

  @Override
  public CFAEdgeType getEdgeType() {
    return CFAEdgeType.AssumeEdge;
  }

  public boolean getTruthAssumption() {
    return truthAssumption;
  }

  public AExpression getExpression() {
    return expression;
  }

  @Override
  public String getCode() {
    if (truthAssumption) {
      return expression.toASTString();
    }
    return "!(" + expression.toASTString() + ")";
  }

  @Override
  public String getDescription() {
    return "[" + getCode() + "]";
  }

  /**
   * TODO Warning: for instances with {@link #getTruthAssumption()} == false, the return value of
   * this method does not represent exactly the return value of {@link #getRawStatement()} (it
   * misses the outer negation of the expression).
   */
  @Override
  public Optional<AAstNode> getRawAST() {
    return Optional.of(expression);
  }

  /**
   * {@code true} if and only if the value of {@code pTruthAssumption} corresponds inversely to the
   * then/else branches of the branching statement in the source code.
   *
   * <p>You will <em>never</em> need to call this method to implement the {@link TransferRelation}
   * of a {@link ConfigurableProgramAnalysis}; instead, you are looking for {@link
   * #getTruthAssumption()}.
   *
   * <p><em>Only</em> call this method if your use case requires you to map this specific edge back
   * to a specific branch in the source code. Valid use cases are exporting counterexample
   * information to the user, e.g. as a witness, or reading such information back in, e.g. for
   * witness validation.
   *
   * @return {@code true} if and only if the value of {@code pTruthAssumption} corresponds inversely
   *     to the then/else branches of the branching statement in the source code.
   */
  public boolean isSwapped() {
    return swapped;
  }

  /**
   * {@code true} if the edge was artificially introduced as an intermediate edge belonging to a
   * larger assume condition, such as a conjunction or disjunction.
   *
   * <p>You will <em>never</em> need to call this method to implement the {@link TransferRelation}
   * of a {@link ConfigurableProgramAnalysis}; instead, you are looking for {@link
   * #getTruthAssumption()}.
   *
   * <p><em>Only</em> call this method if your use case requires you to determine whether it is
   * possible to map this specific edge back to a specific branch in the source code. Valid use
   * cases are exporting counterexample information to the user, e.g. as a witness, or reading such
   * information back in, e.g. for witness validation.
   *
   * @return {@code true} if the edge was artificially introduced.
   */
  public boolean isArtificialIntermediate() {
    return artificialIntermediate;
  }
}
