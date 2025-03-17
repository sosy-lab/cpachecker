// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2025 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.cfa.ast.c;

import java.util.Map;
import java.util.regex.Pattern;
import org.sosy_lab.cpachecker.exceptions.NoException;

/**
 * This class provides a visitor back insert ACSLFunctionCalls from a HashMap into a
 * CBinaryExpression.
 *
 * <p>It is only intended as temporary workarounds until a proper ACSL-parser has been implemented.
 * At this point these methods should not be used anywhere outside the parsing of lemmas from a YAML
 * witness for predicate abstraction.
 */
public class ACSLVisitor extends DefaultCExpressionVisitor<CExpression, NoException> {

  private final Map<String, ACSLFunctionCall> replacements;
  private final Pattern p;

  /**
   * Class constructor
   *
   * @param pReplacements A mapping of ACSLFunctionCalls to their replacement "lemma_tmp_i".
   */
  public ACSLVisitor(Map<String, ACSLFunctionCall> pReplacements) {
    this.replacements = pReplacements;
    p = Pattern.compile("lemma_tmp_\\d+");
  }

  /** Default visitor. */
  @Override
  protected CExpression visitDefault(CExpression exp) {
    return exp;
  }

  /**
   * Replaces all operands that match the attribute "p" with the corresponding ACSLFunction call
   * from the map "replacements".
   *
   * <p>This method is only intended as a * temporary workaround until a proper ACSL-parser has been
   * implemented. It should not be used * anywhere outside the parsing of lemmas from a YAML *
   * witness for predicate abstraction.
   *
   * @param pE A CBinaryExpression that potentially contains a CIDExpression "lemma_tmp_i" that
   *     replaces an ACSLFunctionCall
   */
  @Override
  public CExpression visit(final CBinaryExpression pE) {
    CBinaryExpression expression = pE;

    CExpression op1 = pE.getOperand1();
    CExpression op2 = pE.getOperand2();

    if (!(op1 instanceof CIdExpression)) {
      expression =
          new CBinaryExpression(
              expression.getFileLocation(),
              expression.getExpressionType(),
              expression.getCalculationType(),
              op1.accept(this),
              expression.getOperand2(),
              expression.getOperator());
    } else if (p.matcher(((CIdExpression) op1).getName()).find()) {
      expression =
          new CBinaryExpression(
              expression.getFileLocation(),
              expression.getExpressionType(),
              expression.getCalculationType(),
              replacements.get(((CIdExpression) op1).getName()),
              expression.getOperand2(),
              expression.getOperator());
    }

    if (!(op2 instanceof CIdExpression)) {
      expression =
          new CBinaryExpression(
              expression.getFileLocation(),
              expression.getExpressionType(),
              expression.getCalculationType(),
              expression.getOperand1(),
              op2.accept(this),
              expression.getOperator());
    } else if (p.matcher(((CIdExpression) op2).getName()).find()) {
      expression =
          new CBinaryExpression(
              expression.getFileLocation(),
              expression.getExpressionType(),
              expression.getCalculationType(),
              expression.getOperand1(),
              replacements.get(((CIdExpression) op2).getName()),
              expression.getOperator());
    }

    return expression;
  }
}
