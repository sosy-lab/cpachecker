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
import org.sosy_lab.cpachecker.cfa.ast.acsl.AcslUnaryTerm.AcslUnaryTermOperator;
import org.sosy_lab.cpachecker.cfa.ast.acsl.generated.AcslGrammarParser.AtTermTernaryConditionBodyContext;
import org.sosy_lab.cpachecker.cfa.ast.acsl.generated.AcslGrammarParser.CConstantContext;
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
  public AcslTerm visitVariableTermTernaryConditionBody(
      VariableTermTernaryConditionBodyContext ctx) {
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
