// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2025 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.cfa.ast.acsl.parser;

import java.math.BigInteger;
import org.sosy_lab.cpachecker.cfa.CProgramScope;
import org.sosy_lab.cpachecker.cfa.ast.FileLocation;
import org.sosy_lab.cpachecker.cfa.ast.acsl.AcslBinaryPredicateExpression;
import org.sosy_lab.cpachecker.cfa.ast.acsl.AcslBinaryPredicateExpression.AcslBinaryPredicateExpressionOperator;
import org.sosy_lab.cpachecker.cfa.ast.acsl.AcslBinaryTermExpression;
import org.sosy_lab.cpachecker.cfa.ast.acsl.AcslBinaryTermExpression.AcslBinaryTermExpressionOperator;
import org.sosy_lab.cpachecker.cfa.ast.acsl.AcslBooleanLiteralExpression;
import org.sosy_lab.cpachecker.cfa.ast.acsl.AcslBuiltinLogicType;
import org.sosy_lab.cpachecker.cfa.ast.acsl.AcslExpression;
import org.sosy_lab.cpachecker.cfa.ast.acsl.AcslIntegerLiteralTerm;
import org.sosy_lab.cpachecker.cfa.ast.acsl.AcslLabel;
import org.sosy_lab.cpachecker.cfa.ast.acsl.AcslMemoryLocationSet;
import org.sosy_lab.cpachecker.cfa.ast.acsl.AcslOldExpression;
import org.sosy_lab.cpachecker.cfa.ast.acsl.AcslScope;
import org.sosy_lab.cpachecker.cfa.ast.acsl.AcslTerm;
import org.sosy_lab.cpachecker.cfa.ast.acsl.AcslTernaryPredicateExpression;
import org.sosy_lab.cpachecker.cfa.ast.acsl.AcslUnaryExpression;
import org.sosy_lab.cpachecker.cfa.ast.acsl.AcslUnaryExpression.AcslUnaryExpressionOperator;
import org.sosy_lab.cpachecker.cfa.ast.acsl.AcslValidExpression;
import org.sosy_lab.cpachecker.cfa.ast.acsl.parser.generated.AcslGrammarParser.BinaryPredicateContext;
import org.sosy_lab.cpachecker.cfa.ast.acsl.parser.generated.AcslGrammarParser.ComparisonPredContext;
import org.sosy_lab.cpachecker.cfa.ast.acsl.parser.generated.AcslGrammarParser.LogicalFalsePredContext;
import org.sosy_lab.cpachecker.cfa.ast.acsl.parser.generated.AcslGrammarParser.LogicalTruePredContext;
import org.sosy_lab.cpachecker.cfa.ast.acsl.parser.generated.AcslGrammarParser.OldPredContext;
import org.sosy_lab.cpachecker.cfa.ast.acsl.parser.generated.AcslGrammarParser.ParenthesesPredContext;
import org.sosy_lab.cpachecker.cfa.ast.acsl.parser.generated.AcslGrammarParser.PredicateTermContext;
import org.sosy_lab.cpachecker.cfa.ast.acsl.parser.generated.AcslGrammarParser.TernaryConditionPredContext;
import org.sosy_lab.cpachecker.cfa.ast.acsl.parser.generated.AcslGrammarParser.UnaryPredContext;
import org.sosy_lab.cpachecker.cfa.ast.acsl.parser.generated.AcslGrammarParser.ValidPredContext;

class AntrlPredicateToExpressionsConverter
    extends AntlrToInternalAbstractConverter<AcslExpression> {

  private final AntlrTermToTermConverter antrlToTermConverter;
  private final AntlrTsetToMemorySetConverter antrlTsetToMemorySetConverter;
  private final AntlrLabelToLabelConverter labelConverter;

  protected AntrlPredicateToExpressionsConverter(
      CProgramScope pCProgramScope, AcslScope pAcslScope) {
    super(pCProgramScope, pAcslScope);
    antrlToTermConverter = new AntlrTermToTermConverter(pCProgramScope, pAcslScope);
    antrlTsetToMemorySetConverter = new AntlrTsetToMemorySetConverter(pCProgramScope, pAcslScope);
    labelConverter = new AntlrLabelToLabelConverter(pCProgramScope, pAcslScope);
  }

  @Override
  public AcslExpression visitOldPred(OldPredContext ctx) {
    // The second child is the term inside the old, the others are
    // '\old' '(' term ')'
    AcslExpression expression = visit(ctx.getChild(2));

    return new AcslOldExpression(FileLocation.DUMMY, expression);
  }

  @Override
  public AcslExpression visitLogicalTruePred(LogicalTruePredContext ctx) {
    return new AcslBooleanLiteralExpression(FileLocation.DUMMY, true);
  }

  @Override
  public AcslExpression visitLogicalFalsePred(LogicalFalsePredContext ctx) {
    return new AcslBooleanLiteralExpression(FileLocation.DUMMY, false);
  }

  @Override
  public AcslExpression visitTernaryConditionPred(TernaryConditionPredContext ctx) {
    // The parsing gives the following structure:
    // [condition, '?', if_true, ':', if_false]
    AcslExpression condition = visit(ctx.getChild(0));
    AcslExpression ifTrue = visit(ctx.getChild(2));
    AcslExpression ifFalse = visit(ctx.getChild(4));

    return new AcslTernaryPredicateExpression(FileLocation.DUMMY, condition, ifTrue, ifFalse);
  }

  @Override
  public AcslExpression visitParenthesesPred(ParenthesesPredContext ctx) {
    return visit(ctx.getChild(1));
  }

  @Override
  public AcslExpression visitUnaryPred(UnaryPredContext ctx) {
    AcslUnaryExpressionOperator operator =
        AcslUnaryExpressionOperator.of(ctx.getChild(0).getText());
    AcslExpression expression = visit(ctx.getChild(1));

    return new AcslUnaryExpression(
        FileLocation.DUMMY, AcslBuiltinLogicType.BOOLEAN, expression, operator);
  }

  @Override
  public AcslExpression visitPredicateTerm(PredicateTermContext ctx) {
    AcslTerm term = antrlToTermConverter.visit(ctx.getChild(0));
    return new AcslBinaryTermExpression(
        FileLocation.DUMMY,
        AcslBuiltinLogicType.BOOLEAN,
        term,
        new AcslIntegerLiteralTerm(
            FileLocation.DUMMY, AcslBuiltinLogicType.INTEGER, BigInteger.ZERO),
        AcslBinaryTermExpressionOperator.EQUALS);
  }

  @Override
  public AcslExpression visitValidPred(ValidPredContext ctx) {
    // The parsing gives the following structure:
    // [\valid, '(', term, ')'] or [\valid, label,  '(', term, ')']
    if (ctx.getChildCount() == 4) {
      // We are in the case: [\valid, '(', term, ')']
      AcslMemoryLocationSet memoryLocationSet =
          antrlTsetToMemorySetConverter.visit(ctx.getChild(2));
      return new AcslValidExpression(
          FileLocation.DUMMY, AcslBuiltinLogicType.BOOLEAN, memoryLocationSet);
    } else if (ctx.getChildCount() == 5) {
      // We are in the case: [\valid, ['{', label, '}'],  '(', term, ')']
      AcslLabel label = labelConverter.visit(ctx.getChild(1).getChild(1));
      AcslMemoryLocationSet memoryLocationSet =
          antrlTsetToMemorySetConverter.visit(ctx.getChild(3));
      return new AcslValidExpression(
          FileLocation.DUMMY, AcslBuiltinLogicType.BOOLEAN, memoryLocationSet, label);
    } else {
      throw new RuntimeException("Unexpected number of children when creating a valid predicate");
    }
  }

  @Override
  public AcslExpression visitBinaryPredicate(BinaryPredicateContext ctx) {
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
  public AcslExpression visitComparisonPred(ComparisonPredContext ctx) {
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

      AcslTerm leftTerm = antrlToTermConverter.visit(ctx.getChild(i));
      AcslBinaryTermExpressionOperator operator =
          AcslBinaryTermExpressionOperator.of(ctx.getChild(i + 1).getText());
      AcslTerm righTerm = antrlToTermConverter.visit(ctx.getChild(i + 2));

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
