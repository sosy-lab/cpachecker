// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2025 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.cfa.ast.acsl.parser;

import org.sosy_lab.cpachecker.cfa.CProgramScope;
import org.sosy_lab.cpachecker.cfa.ast.FileLocation;
import org.sosy_lab.cpachecker.cfa.ast.acsl.AcslBinaryPredicateExpression;
import org.sosy_lab.cpachecker.cfa.ast.acsl.AcslBinaryPredicateExpression.AcslBinaryPredicateExpressionOperator;
import org.sosy_lab.cpachecker.cfa.ast.acsl.AcslBinaryTermExpression;
import org.sosy_lab.cpachecker.cfa.ast.acsl.AcslBinaryTermExpression.AcslBinaryTermExpressionOperator;
import org.sosy_lab.cpachecker.cfa.ast.acsl.AcslBooleanLiteralExpression;
import org.sosy_lab.cpachecker.cfa.ast.acsl.AcslBuiltinLogicType;
import org.sosy_lab.cpachecker.cfa.ast.acsl.AcslExpression;
import org.sosy_lab.cpachecker.cfa.ast.acsl.AcslOldExpression;
import org.sosy_lab.cpachecker.cfa.ast.acsl.AcslScope;
import org.sosy_lab.cpachecker.cfa.ast.acsl.AcslTerm;
import org.sosy_lab.cpachecker.cfa.ast.acsl.AcslUnaryExpression;
import org.sosy_lab.cpachecker.cfa.ast.acsl.AcslUnaryExpression.AcslUnaryExpressionOperator;
import org.sosy_lab.cpachecker.cfa.ast.acsl.parser.generated.AcslGrammarParser.BinaryTermPredTernaryConditionContext;
import org.sosy_lab.cpachecker.cfa.ast.acsl.parser.generated.AcslGrammarParser.ComparisonTermPredTernaryConditionContext;
import org.sosy_lab.cpachecker.cfa.ast.acsl.parser.generated.AcslGrammarParser.LogicalFalseTermPredTernaryConditionContext;
import org.sosy_lab.cpachecker.cfa.ast.acsl.parser.generated.AcslGrammarParser.LogicalTrueTermPredTernaryConditionContext;
import org.sosy_lab.cpachecker.cfa.ast.acsl.parser.generated.AcslGrammarParser.OldTermPredTernaryConditionContext;
import org.sosy_lab.cpachecker.cfa.ast.acsl.parser.generated.AcslGrammarParser.ParenthesesTermPredTernaryConditionContext;
import org.sosy_lab.cpachecker.cfa.ast.acsl.parser.generated.AcslGrammarParser.UnaryTermPredTernaryConditionContext;

public class AntlrTermPredTernaryConditionToExpressionConverter
    extends AntlrToInternalAbstractConverter<AcslExpression> {

  private final AntlrTermTernaryConditionBodyToTermConverter
      antrlTermTernaryConditionToTermConverter;

  protected AntlrTermPredTernaryConditionToExpressionConverter(
      CProgramScope pCProgramScope, AcslScope pAcslScope) {
    super(pCProgramScope, pAcslScope);
    antrlTermTernaryConditionToTermConverter =
        new AntlrTermTernaryConditionBodyToTermConverter(pCProgramScope, pAcslScope);
  }

  @Override
  public AcslExpression visitOldTermPredTernaryCondition(OldTermPredTernaryConditionContext ctx) {
    // The second child is the term inside the old, the others are
    // '\old' '(' term ')'
    AcslExpression expression = visit(ctx.getChild(2));

    return new AcslOldExpression(FileLocation.DUMMY, expression);
  }

  @Override
  public AcslExpression visitLogicalTrueTermPredTernaryCondition(
      LogicalTrueTermPredTernaryConditionContext ctx) {
    return new AcslBooleanLiteralExpression(FileLocation.DUMMY, true);
  }

  @Override
  public AcslExpression visitLogicalFalseTermPredTernaryCondition(
      LogicalFalseTermPredTernaryConditionContext ctx) {
    return new AcslBooleanLiteralExpression(FileLocation.DUMMY, false);
  }

  @Override
  public AcslExpression visitParenthesesTermPredTernaryCondition(
      ParenthesesTermPredTernaryConditionContext ctx) {
    return visit(ctx.getChild(1));
  }

  @Override
  public AcslExpression visitUnaryTermPredTernaryCondition(
      UnaryTermPredTernaryConditionContext ctx) {
    AcslUnaryExpressionOperator operator =
        AcslUnaryExpressionOperator.of(ctx.getChild(0).getText());
    AcslExpression expression = visit(ctx.getChild(1));

    return new AcslUnaryExpression(
        FileLocation.DUMMY, AcslBuiltinLogicType.BOOLEAN, expression, operator);
  }

  @Override
  public AcslExpression visitBinaryTermPredTernaryCondition(
      BinaryTermPredTernaryConditionContext ctx) {
    AcslExpression leftExpression = visit(ctx.getChild(0));
    AcslBinaryPredicateExpressionOperator operator =
        AcslBinaryPredicateExpressionOperator.of(ctx.getChild(1).getText());
    AcslExpression rightExpression = visit(ctx.getChild(2));

    return new AcslBinaryPredicateExpression(
        FileLocation.DUMMY,
        AcslBuiltinLogicType.BOOLEAN,
        leftExpression,
        rightExpression,
        operator);
  }

  @Override
  public AcslExpression visitComparisonTermPredTernaryCondition(
      ComparisonTermPredTernaryConditionContext ctx) {
    // In ACSL it is allowed to write something like `0 <= i <= n`, which we get as the list
    // [`0`, `<=`, `i`, `<=`, `n`]
    AcslExpression currentExpression = null;
    int amountOfChildren = ctx.getChildCount();
    int i;
    for (i = 0; i + 1 < amountOfChildren; i += 2) {
      if (i + 3 < amountOfChildren) {
        throw new RuntimeException(
            "Unexpected number of children when creating comparison operator");
      }

      AcslTerm leftTerm = antrlTermTernaryConditionToTermConverter.visit(ctx.getChild(i));
      AcslBinaryTermExpressionOperator operator =
          AcslBinaryTermExpressionOperator.of(ctx.getChild(i + 1).getText());
      AcslTerm righTerm = antrlTermTernaryConditionToTermConverter.visit(ctx.getChild(i + 2));

      AcslExpression newComparison =
          new AcslBinaryTermExpression(
              FileLocation.DUMMY, AcslBuiltinLogicType.BOOLEAN, leftTerm, righTerm, operator);

      if (currentExpression == null) {
        currentExpression = newComparison;
      } else {
        currentExpression =
            new AcslBinaryPredicateExpression(
                FileLocation.DUMMY,
                AcslBuiltinLogicType.BOOLEAN,
                currentExpression,
                newComparison,
                AcslBinaryPredicateExpressionOperator.AND);
      }
    }

    if (currentExpression == null) {
      throw new RuntimeException("Expected at least one comparison for a comparison predicate");
    }

    if (i + 1 != amountOfChildren) {
      throw new RuntimeException(
          "Not all children were considered during parsing a comparison predicate");
    }

    return currentExpression;
  }
}
