// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2025 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.cfa.ast.acsl;

import com.google.common.collect.ImmutableList;
import java.util.Objects;
import org.sosy_lab.cpachecker.cfa.CProgramScope;
import org.sosy_lab.cpachecker.cfa.ast.FileLocation;
import org.sosy_lab.cpachecker.cfa.ast.acsl.AcslBinaryTerm.AcslBinaryTermOperator;
import org.sosy_lab.cpachecker.cfa.ast.acsl.AcslUnaryTerm.AcslUnaryTermOperator;
import org.sosy_lab.cpachecker.cfa.ast.acsl.generated.AcslGrammarParser.ArrayAccessTermTernaryConditionBodyContext;
import org.sosy_lab.cpachecker.cfa.ast.acsl.generated.AcslGrammarParser.AtTermTernaryConditionBodyContext;
import org.sosy_lab.cpachecker.cfa.ast.acsl.generated.AcslGrammarParser.BinaryOpTermTernaryConditionBodyContext;
import org.sosy_lab.cpachecker.cfa.ast.acsl.generated.AcslGrammarParser.CConstantContext;
import org.sosy_lab.cpachecker.cfa.ast.acsl.generated.AcslGrammarParser.FuncApplicationTermTernaryConditionBodyContext;
import org.sosy_lab.cpachecker.cfa.ast.acsl.generated.AcslGrammarParser.OldTermTernaryConditionBodyContext;
import org.sosy_lab.cpachecker.cfa.ast.acsl.generated.AcslGrammarParser.ParenthesesTermTernaryConditionBodyContext;
import org.sosy_lab.cpachecker.cfa.ast.acsl.generated.AcslGrammarParser.ResultTermTernaryConditionBodyContext;
import org.sosy_lab.cpachecker.cfa.ast.acsl.generated.AcslGrammarParser.UnaryOpTermTernaryConditionBodyContext;
import org.sosy_lab.cpachecker.cfa.ast.acsl.generated.AcslGrammarParser.VariableTermTernaryConditionBodyContext;
import org.sosy_lab.cpachecker.cfa.types.c.CType;

public class AntlrTermTernaryConditionBodyToTermConverter
    extends AntlrToInternalAbstractConverter<AcslTerm> {

  private final AntlrLabelToLabelConverter labelConverter;

  protected AntlrTermTernaryConditionBodyToTermConverter(
      CProgramScope pCProgramScope, AcslScope pAcslScope) {
    super(pCProgramScope, pAcslScope);
    labelConverter = new AntlrLabelToLabelConverter(pCProgramScope, pAcslScope);
  }

  @Override
  public AcslTerm visitArrayAccessTermTernaryConditionBody(
      ArrayAccessTermTernaryConditionBodyContext ctx) {
    // The parsing gives the following structure:
    // [term, '[', term, ']']
    AcslTerm array = visit(ctx.getChild(0));
    AcslTerm index = visit(ctx.getChild(2));

    return new AcslArraySubscriptTerm(
        FileLocation.DUMMY,
        findResultTypeAfterUnaryOperation(
            AcslUnaryTermOperator.POINTER_DEREFERENCE, array.getExpressionType()),
        array,
        index);
  }

  @Override
  public AcslTerm visitFuncApplicationTermTernaryConditionBody(
      FuncApplicationTermTernaryConditionBodyContext ctx) {
    // The parsing gives the following structure:
    // [term, '(', term ',' term ')']
    // TODO: In general this is wrong if this is a function pointer
    String functionName = ctx.getChild(0).getText();
    ImmutableList.Builder<AcslTerm> arguments = ImmutableList.builder();
    for (int i = 2; i < ctx.getChildCount() - 1; i += 2) {
      arguments.add(visit(ctx.getChild(i)));
    }

    AcslFunctionDeclaration functionDeclaration =
        Objects.requireNonNull(getAcslScope().lookupFunction(functionName));

    return new AcslFunctionCallTerm(
        FileLocation.DUMMY,
        (AcslType) functionDeclaration.getType().getReturnType(),
        new AcslIdTerm(FileLocation.DUMMY, functionDeclaration),
        arguments.build(),
        functionDeclaration);
  }

  @Override
  public AcslTerm visitVariableTermTernaryConditionBody(
      VariableTermTernaryConditionBodyContext ctx) {
    String identifierName = ctx.getText();
    AcslSimpleDeclaration variable = getVariableDeclarationForName(identifierName);
    return new AcslIdTerm(FileLocation.DUMMY, variable);
  }

  @Override
  public AcslTerm visitCConstant(CConstantContext ctx) {
    String stringValue = ctx.getText();
    return getConstantForString(stringValue);
  }

  @Override
  public AcslTerm visitParenthesesTermTernaryConditionBody(
      ParenthesesTermTernaryConditionBodyContext ctx) {
    return visit(ctx.getChild(1));
  }

  @Override
  public AcslTerm visitUnaryOpTermTernaryConditionBody(UnaryOpTermTernaryConditionBodyContext ctx) {
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
  public AcslTerm visitOldTermTernaryConditionBody(OldTermTernaryConditionBodyContext ctx) {
    // The second child is the term inside the old, the others are
    // '\old' '(' term ')'
    AcslTerm term = visit(ctx.getChild(2));

    return new AcslOldTerm(FileLocation.DUMMY, term);
  }

  @Override
  public AcslTerm visitBinaryOpTermTernaryConditionBody(
      BinaryOpTermTernaryConditionBodyContext ctx) {
    AcslTerm leftTerm = visit(ctx.getChild(0));
    AcslBinaryTermOperator operator = AcslBinaryTermOperator.of(ctx.getChild(1).getText());
    AcslTerm rightTerm = visit(ctx.getChild(2));

    AcslType resultType =
        AcslType.mostGeneralType(leftTerm.getExpressionType(), rightTerm.getExpressionType());
    return new AcslBinaryTerm(FileLocation.DUMMY, resultType, leftTerm, rightTerm, operator);
  }

  @Override
  public AcslTerm visitResultTermTernaryConditionBody(ResultTermTernaryConditionBodyContext ctx) {
    // We need to find out the type of the result, for which we need to be inside a function
    String currentFunction = getCProgramScope().getCurrentFunctionName();
    CType functionResultType =
        Objects.requireNonNull(getCProgramScope().lookupFunction(currentFunction))
            .getType()
            .getReturnType();
    return new AcslResultTerm(FileLocation.DUMMY, new AcslCType(functionResultType));
  }

  @Override
  public AcslTerm visitAtTermTernaryConditionBody(AtTermTernaryConditionBodyContext ctx) {
    // The parsing gives the following structure:
    // ['\at', '(', term, ',', label, ')']
    AcslTerm term = visit(ctx.getChild(2));
    AcslLabel label = labelConverter.visit(ctx.getChild(4));

    return new AcslAtTerm(FileLocation.DUMMY, term, label);
  }
}
