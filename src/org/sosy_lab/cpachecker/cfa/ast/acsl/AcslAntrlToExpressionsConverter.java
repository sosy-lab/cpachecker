// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2025 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.cfa.ast.acsl;

import com.google.common.collect.FluentIterable;
import com.google.common.collect.ImmutableList;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.Objects;
import javax.annotation.Nullable;
import org.antlr.v4.runtime.tree.ParseTree;
import org.sosy_lab.cpachecker.cfa.CProgramScope;
import org.sosy_lab.cpachecker.cfa.ast.FileLocation;
import org.sosy_lab.cpachecker.cfa.ast.acsl.AcslBinaryExpression.AcslBinaryExpressionOperator;
import org.sosy_lab.cpachecker.cfa.ast.acsl.AcslBinaryTerm.AcslBinaryTermOperator;
import org.sosy_lab.cpachecker.cfa.ast.acsl.AcslBinaryTermComparisonExpression.AcslBinaryTermComparisonExpressionOperator;
import org.sosy_lab.cpachecker.cfa.ast.acsl.AcslUnaryTerm.AcslUnaryTermOperator;
import org.sosy_lab.cpachecker.cfa.ast.acsl.generated.AcslGrammarBaseVisitor;
import org.sosy_lab.cpachecker.cfa.ast.acsl.generated.AcslGrammarParser.AtTermContext;
import org.sosy_lab.cpachecker.cfa.ast.acsl.generated.AcslGrammarParser.BinOpContext;
import org.sosy_lab.cpachecker.cfa.ast.acsl.generated.AcslGrammarParser.BinaryPredOpContext;
import org.sosy_lab.cpachecker.cfa.ast.acsl.generated.AcslGrammarParser.BinaryPredicateContext;
import org.sosy_lab.cpachecker.cfa.ast.acsl.generated.AcslGrammarParser.CConstantContext;
import org.sosy_lab.cpachecker.cfa.ast.acsl.generated.AcslGrammarParser.ComparisonPredContext;
import org.sosy_lab.cpachecker.cfa.ast.acsl.generated.AcslGrammarParser.FalseConstantContext;
import org.sosy_lab.cpachecker.cfa.ast.acsl.generated.AcslGrammarParser.Label_idContext;
import org.sosy_lab.cpachecker.cfa.ast.acsl.generated.AcslGrammarParser.LogicalFalsePredContext;
import org.sosy_lab.cpachecker.cfa.ast.acsl.generated.AcslGrammarParser.LogicalTruePredContext;
import org.sosy_lab.cpachecker.cfa.ast.acsl.generated.AcslGrammarParser.OldPredContext;
import org.sosy_lab.cpachecker.cfa.ast.acsl.generated.AcslGrammarParser.OldTermContext;
import org.sosy_lab.cpachecker.cfa.ast.acsl.generated.AcslGrammarParser.RelOpContext;
import org.sosy_lab.cpachecker.cfa.ast.acsl.generated.AcslGrammarParser.ResultTermContext;
import org.sosy_lab.cpachecker.cfa.ast.acsl.generated.AcslGrammarParser.TrueConstantContext;
import org.sosy_lab.cpachecker.cfa.ast.acsl.generated.AcslGrammarParser.UnaryOpContext;
import org.sosy_lab.cpachecker.cfa.ast.acsl.generated.AcslGrammarParser.UnaryOpTermContext;
import org.sosy_lab.cpachecker.cfa.ast.acsl.generated.AcslGrammarParser.VariableTermContext;
import org.sosy_lab.cpachecker.cfa.ast.c.CSimpleDeclaration;
import org.sosy_lab.cpachecker.cfa.ast.c.CVariableDeclaration;
import org.sosy_lab.cpachecker.cfa.types.c.CType;

class AcslAntrlToExpressionsConverter extends AcslGrammarBaseVisitor<AcslAstNode> {

  private final CProgramScope cProgramScope;
  private final AcslScope acslScope;

  protected AcslAntrlToExpressionsConverter(CProgramScope pCProgramScope, AcslScope pAcslScope) {
    cProgramScope = pCProgramScope;
    acslScope = pAcslScope;
  }

  private AcslAstNode getVariableForName(String pName) {
    @Nullable CSimpleDeclaration cVariableDeclaration = cProgramScope.lookupVariable(pName);
    @Nullable AcslSimpleDeclaration acslVariableDeclaration = acslScope.lookupVariable(pName);
    if (cVariableDeclaration != null && acslVariableDeclaration == null) {
      if (cVariableDeclaration instanceof CVariableDeclaration var) {
        return new AcslCVariableDeclaration(var);
      } else {
        throw new RuntimeException(
            "Expected a C variable declaration, but got: " + cVariableDeclaration);
      }
    } else if (acslVariableDeclaration != null && cVariableDeclaration == null) {
      return acslVariableDeclaration;
    } else {
      throw new RuntimeException(
          "Variable " + pName + " is not declared in neither the C program nor the ACSL scope.");
    }
  }

  @Override
  public AcslAstNode visitVariableTerm(VariableTermContext ctx) {
    String identifierName = ctx.getText();
    AcslAstNode variable = getVariableForName(identifierName);
    if (!(variable instanceof AcslDeclaration pDeclaration)) {
      throw new RuntimeException(
          "Expected a variable declaration, but got: "
              + variable
              + " for identifier: "
              + identifierName);
    }

    return new AcslIdTerm(FileLocation.DUMMY, pDeclaration);
  }

  private AcslAstNode getConstantForString(String pStringValue) {
    // This is a hack around the problem that I cannot get ANTLR to correctly
    // give me the type of the C constant it parsed
    try {
      return new AcslIntegerLiteralTerm(
          FileLocation.DUMMY, AcslBuiltinLogicType.INTEGER, new BigInteger(pStringValue));
    } catch (NumberFormatException e) {
      try {
        // It is not an integer, so we try a float
        return new AcslRealLiteralTerm(
            FileLocation.DUMMY, AcslBuiltinLogicType.REAL, new BigDecimal(pStringValue));

      } catch (NumberFormatException e2) {
        // This is a character constant
        if (pStringValue.length() != 1) {
          throw new RuntimeException(
              "Character constant should be of length 1, but was: " + pStringValue);
        }
        return new AcslCharLiteralTerm(
            FileLocation.DUMMY, AcslBuiltinLogicType.INTEGER, pStringValue.charAt(0));
      }
    }
  }

  @Override
  public AcslAstNode visitCConstant(CConstantContext ctx) {
    String stringValue = ctx.getText();
    return getConstantForString(stringValue);
  }

  @Override
  public AcslAstNode visitRelOp(RelOpContext ctx) {
    String operator = ctx.getText();
    return AcslBinaryTermComparisonExpressionOperator.of(operator);
  }

  @Override
  public AcslAstNode visitBinOp(BinOpContext ctx) {
    String operator = ctx.getText();

    return AcslBinaryTermOperator.of(operator);
  }

  @Override
  public AcslAstNode visitUnaryOp(UnaryOpContext ctx) {
    String operator = ctx.getText();
    return AcslUnaryTermOperator.of(operator);
  }

  @Override
  public AcslAstNode visitUnaryOpTerm(UnaryOpTermContext ctx) {
    AcslAstNode operator = visit(ctx.children.get(0));
    AcslAstNode term = visit(ctx.children.get(1));
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

    if (!(term instanceof AcslTerm pAcslTerm)) {
      throw new RuntimeException("Expected a term in term unary operator");
    }

    if (!(operator instanceof AcslUnaryTermOperator pAcslUnaryTermOperator)) {
      throw new RuntimeException("Expected a term unary operator");
    }

    // TODO: The type here is definitely wrong for pointer dereferences and so on
    return new AcslUnaryTerm(
        FileLocation.DUMMY, pAcslTerm.getExpressionType(), pAcslTerm, pAcslUnaryTermOperator);
  }

  @Override
  public AcslAstNode visitOldTerm(OldTermContext ctx) {
    // The second child is the term inside the old, the others are
    // '\old' '(' term ')'
    AcslAstNode child = visit(ctx.children.get(2));

    if (child instanceof AcslTerm expression) {
      return new AcslOldTerm(FileLocation.DUMMY, expression);
    } else {
      throw new RuntimeException("Expected a term in old term");
    }
  }

  @Override
  public AcslAstNode visitOldPred(OldPredContext ctx) {
    // The second child is the term inside the old, the others are
    // '\old' '(' term ')'
    AcslAstNode child = visit(ctx.children.get(2));

    if (child instanceof AcslExpression expression) {
      return new AcslOldExpression(FileLocation.DUMMY, expression);
    } else {
      throw new RuntimeException("Expected an expression in old expression");
    }
  }

  @Override
  public AcslAstNode visitResultTerm(ResultTermContext ctx) {
    // We need to find out the type of the result, for which we need to be inside a function
    String currentFunction = cProgramScope.getCurrentFunctionName();
    CType functionResultType =
        Objects.requireNonNull(cProgramScope.lookupFunction(currentFunction))
            .getType()
            .getReturnType();
    return new AcslResultTerm(FileLocation.DUMMY, new AcslCType(functionResultType));
  }

  @Override
  public AcslAstNode visitLogicalTruePred(LogicalTruePredContext ctx) {
    return new AcslBooleanLiteralExpression(FileLocation.DUMMY, true);
  }

  @Override
  public AcslAstNode visitLogicalFalsePred(LogicalFalsePredContext ctx) {
    return new AcslBooleanLiteralExpression(FileLocation.DUMMY, false);
  }

  @Override
  public AcslAstNode visitTrueConstant(TrueConstantContext ctx) {
    return new AcslBooleanLiteralTerm(FileLocation.DUMMY, true);
  }

  @Override
  public AcslAstNode visitFalseConstant(FalseConstantContext ctx) {
    return new AcslBooleanLiteralTerm(FileLocation.DUMMY, false);
  }

  @Override
  public AcslAstNode visitBinaryPredOp(BinaryPredOpContext ctx) {
    String operator = ctx.getText();
    return AcslBinaryExpressionOperator.of(operator);
  }

  @Override
  public AcslAstNode visitBinaryPredicate(BinaryPredicateContext ctx) {
    AcslAstNode left = visit(ctx.children.get(0));
    AcslAstNode operator = visit(ctx.children.get(1));
    AcslAstNode right = visit(ctx.children.get(2));

    if (!(left instanceof AcslExpression leftExpression)) {
      throw new RuntimeException("Expected a left expression in binary predicate");
    }

    if (!(right instanceof AcslExpression rightExpression)) {
      throw new RuntimeException("Expected a right expression in binary predicate");
    }

    if (!(operator instanceof AcslBinaryExpressionOperator binaryOperator)) {
      throw new RuntimeException("Expected a binary operator in binary predicate");
    }

    return new AcslBinaryExpression(
        FileLocation.DUMMY,
        AcslBuiltinLogicType.BOOLEAN,
        leftExpression,
        rightExpression,
        binaryOperator);
  }

  @Override
  public AcslAstNode visitLabel_id(Label_idContext ctx) {
    String identifierName = ctx.getText();
    if (FluentIterable.from(AcslBuiltinLabel.values())
        .transform(AcslBuiltinLabel::getLabel)
        .anyMatch(label -> label.equals(identifierName))) {
      return AcslBuiltinLabel.of(identifierName);
    }

    return new AcslProgramLabel(identifierName, FileLocation.DUMMY);
  }

  @Override
  public AcslAstNode visitAtTerm(AtTermContext ctx) {
    // The parsing gives the following structure:
    // ['\at', '(', term, ',', label, ')']
    AcslAstNode termNode = visit(ctx.children.get(2));
    AcslAstNode labelNode = visit(ctx.children.get(4));

    if (!(labelNode instanceof AcslLabel label)) {
      throw new RuntimeException("Expected a label in at term");
    }

    if (!(termNode instanceof AcslTerm term)) {
      throw new RuntimeException("Expected a term in at term");
    }

    return new AcslAtTerm(FileLocation.DUMMY, term, label);
  }

  @Override
  public AcslAstNode visitComparisonPred(ComparisonPredContext ctx) {
    ImmutableList.Builder<AcslAstNode> childrenBuilder = new ImmutableList.Builder<>();
    for (ParseTree child : ctx.children) {
      childrenBuilder.add(visit(child));
    }

    // We need to cast the nodes from AcslAstNode to AcslExpression
    ImmutableList<AcslAstNode> children = FluentIterable.from(childrenBuilder.build()).toList();

    // In ACSL it is allowed to write something like `0 <= i <= n`, which we get as the list
    // [`0`, `<=`, `i`, `<=`, `n`]
    AcslExpression currentExpression = null;
    int i;
    for (i = 0; i + 1 < children.size(); i += 2) {
      if (i + 3 < children.size()) {
        throw new RuntimeException(
            "Unexpected number of children when creating comparison operator");
      }

      AcslAstNode leftNode = children.get(i);
      AcslAstNode operatorNode = children.get(i + 1);
      AcslAstNode rightNode = children.get(i + 2);

      if (!(leftNode instanceof AcslTerm leftTerm)) {
        throw new RuntimeException("Expected a term on the left side of the comparison operator");
      }

      if (!(rightNode instanceof AcslTerm righTerm)) {
        throw new RuntimeException("Expected a term on the right side of the comparison operator");
      }

      if (!(operatorNode instanceof AcslBinaryTermComparisonExpressionOperator operatorTerm)) {
        throw new RuntimeException("Expected a binary operator");
      }

      AcslExpression newComparison =
          new AcslBinaryTermComparisonExpression(
              FileLocation.DUMMY, AcslBuiltinLogicType.BOOLEAN, leftTerm, righTerm, operatorTerm);

      if (currentExpression == null) {
        currentExpression = newComparison;
      } else {
        currentExpression =
            new AcslBinaryExpression(
                FileLocation.DUMMY,
                AcslBuiltinLogicType.BOOLEAN,
                currentExpression,
                newComparison,
                AcslBinaryExpressionOperator.AND);
      }
    }

    if (currentExpression == null) {
      throw new RuntimeException("Expected at least one comparison for a comparison predicate");
    }

    if (i + 1 != children.size()) {
      throw new RuntimeException(
          "Not all children were considered during parsing a comparison predicate");
    }

    return currentExpression;
  }
}
