// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2025 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.cfa.ast.acsl;

import java.util.Objects;
import org.sosy_lab.cpachecker.cfa.CProgramScope;
import org.sosy_lab.cpachecker.cfa.ast.FileLocation;
import org.sosy_lab.cpachecker.cfa.ast.acsl.AcslBinaryTerm.AcslBinaryTermOperator;
import org.sosy_lab.cpachecker.cfa.ast.acsl.AcslUnaryTerm.AcslUnaryTermOperator;
import org.sosy_lab.cpachecker.cfa.ast.acsl.generated.AcslGrammarParser.AtTermContext;
import org.sosy_lab.cpachecker.cfa.ast.acsl.generated.AcslGrammarParser.BinaryOpTermContext;
import org.sosy_lab.cpachecker.cfa.ast.acsl.generated.AcslGrammarParser.CConstantContext;
import org.sosy_lab.cpachecker.cfa.ast.acsl.generated.AcslGrammarParser.FalseConstantContext;
import org.sosy_lab.cpachecker.cfa.ast.acsl.generated.AcslGrammarParser.OldTermContext;
import org.sosy_lab.cpachecker.cfa.ast.acsl.generated.AcslGrammarParser.ParenthesesTermContext;
import org.sosy_lab.cpachecker.cfa.ast.acsl.generated.AcslGrammarParser.ResultTermContext;
import org.sosy_lab.cpachecker.cfa.ast.acsl.generated.AcslGrammarParser.TernaryCondTermContext;
import org.sosy_lab.cpachecker.cfa.ast.acsl.generated.AcslGrammarParser.TrueConstantContext;
import org.sosy_lab.cpachecker.cfa.ast.acsl.generated.AcslGrammarParser.UnaryOpTermContext;
import org.sosy_lab.cpachecker.cfa.ast.acsl.generated.AcslGrammarParser.VariableTermContext;
import org.sosy_lab.cpachecker.cfa.types.c.CType;

class AntlrTermToTermConverter extends AntlrToInternalAbstractConverter<AcslTerm> {

  private final AntlrLabelToLabelConverter labelConverter;
  private final AntlrTermPredTernaryConditionToExpressionConverter
      termPredTernaryConditionToExpressionConverter;

  protected AntlrTermToTermConverter(CProgramScope pCProgramScope, AcslScope pAcslScope) {
    super(pCProgramScope, pAcslScope);
    labelConverter = new AntlrLabelToLabelConverter(pCProgramScope, pAcslScope);
    termPredTernaryConditionToExpressionConverter =
        new AntlrTermPredTernaryConditionToExpressionConverter(pCProgramScope, pAcslScope);
  }

  @Override
  public AcslTerm visitVariableTerm(VariableTermContext ctx) {
    String identifierName = ctx.getText();
    AcslAstNode variable = getVariableDeclarationForName(identifierName);
    if (!(variable instanceof AcslDeclaration pDeclaration)) {
      throw new RuntimeException(
          "Expected a variable declaration, but got: "
              + variable
              + " for identifier: "
              + identifierName);
    }

    return new AcslIdTerm(FileLocation.DUMMY, pDeclaration);
  }

  @Override
  public AcslTerm visitCConstant(CConstantContext ctx) {
    String stringValue = ctx.getText();
    return getConstantForString(stringValue);
  }

  @Override
  public AcslTerm visitParenthesesTerm(ParenthesesTermContext ctx) {
    return visit(ctx.getChild(1));
  }

  @Override
  public AcslTerm visitUnaryOpTerm(UnaryOpTermContext ctx) {
    AcslUnaryTermOperator operator = AcslUnaryTermOperator.of(ctx.getChild(0).getText());
    AcslTerm term = visit(ctx.getChild(1));
    if (term instanceof AcslIntegerLiteralTerm integerTerm
        && operator.equals(AcslUnaryTermOperator.MINUS)) {
      return new AcslIntegerLiteralTerm(
          integerTerm.getFileLocation(),
          integerTerm.getExpressionType(),
          integerTerm.getValue().negate());
    } else if (term instanceof AcslRealLiteralTerm realTerm
        && operator.equals(AcslUnaryTermOperator.MINUS)) {
      return new AcslRealLiteralTerm(
          realTerm.getFileLocation(), realTerm.getExpressionType(), realTerm.getValue().negate());
    }

    AcslType resultType = findResultTypeAfterUnaryOperation(operator, term.getExpressionType());
    return new AcslUnaryTerm(FileLocation.DUMMY, resultType, term, operator);
  }

  @Override
  public AcslTerm visitOldTerm(OldTermContext ctx) {
    // The second child is the term inside the old, the others are
    // '\old' '(' term ')'
    AcslTerm term = visit(ctx.getChild(2));

    return new AcslOldTerm(FileLocation.DUMMY, term);
  }

  @Override
  public AcslTerm visitResultTerm(ResultTermContext ctx) {
    // We need to find out the type of the result, for which we need to be inside a function
    String currentFunction = getCProgramScope().getCurrentFunctionName();
    CType functionResultType =
        Objects.requireNonNull(getCProgramScope().lookupFunction(currentFunction))
            .getType()
            .getReturnType();
    return new AcslResultTerm(FileLocation.DUMMY, new AcslCType(functionResultType));
  }

  @Override
  public AcslTerm visitTrueConstant(TrueConstantContext ctx) {
    return new AcslBooleanLiteralTerm(FileLocation.DUMMY, true);
  }

  @Override
  public AcslTerm visitFalseConstant(FalseConstantContext ctx) {
    return new AcslBooleanLiteralTerm(FileLocation.DUMMY, false);
  }

  @Override
  public AcslTerm visitTernaryCondTerm(TernaryCondTermContext ctx) {
    // The parsing gives the following structure:
    // [cond, '?', if_true, ':', if_false]

    AcslExpression condition = termPredTernaryConditionToExpressionConverter.visit(ctx.getChild(0));
    AcslTerm ifTrue = visit(ctx.getChild(2));
    AcslTerm ifFalse = visit(ctx.getChild(4));

    return new AcslTernaryTermExpression(FileLocation.DUMMY, condition, ifTrue, ifFalse);
  }

  @Override
  public AcslTerm visitBinaryOpTerm(BinaryOpTermContext ctx) {
    AcslTerm leftTerm = visit(ctx.getChild(0));
    AcslBinaryTermOperator operator = AcslBinaryTermOperator.of(ctx.getChild(1).getText());
    AcslTerm rightTerm = visit(ctx.getChild(2));

    AcslType resultType =
        AcslType.mostGeneralType(leftTerm.getExpressionType(), rightTerm.getExpressionType());
    return new AcslBinaryTerm(FileLocation.DUMMY, resultType, leftTerm, rightTerm, operator);
  }

  @Override
  public AcslTerm visitAtTerm(AtTermContext ctx) {
    // The parsing gives the following structure:
    // ['\at', '(', term, ',', label, ')']
    AcslTerm term = visit(ctx.getChild(2));
    AcslLabel label = labelConverter.visit(ctx.getChild(4));

    return new AcslAtTerm(FileLocation.DUMMY, term, label);
  }
}
