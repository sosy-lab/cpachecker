// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2007-2020 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.util.ltl;

import com.google.common.collect.ImmutableList;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import org.antlr.v4.runtime.misc.ParseCancellationException;
import org.antlr.v4.runtime.tree.TerminalNode;
import org.sosy_lab.cpachecker.util.ltl.formulas.BooleanConstant;
import org.sosy_lab.cpachecker.util.ltl.formulas.Conjunction;
import org.sosy_lab.cpachecker.util.ltl.formulas.Disjunction;
import org.sosy_lab.cpachecker.util.ltl.formulas.Finally;
import org.sosy_lab.cpachecker.util.ltl.formulas.Globally;
import org.sosy_lab.cpachecker.util.ltl.formulas.Literal;
import org.sosy_lab.cpachecker.util.ltl.formulas.LtlFormula;
import org.sosy_lab.cpachecker.util.ltl.formulas.Next;
import org.sosy_lab.cpachecker.util.ltl.formulas.Release;
import org.sosy_lab.cpachecker.util.ltl.formulas.StrongRelease;
import org.sosy_lab.cpachecker.util.ltl.formulas.Until;
import org.sosy_lab.cpachecker.util.ltl.formulas.WeakUntil;
import org.sosy_lab.cpachecker.util.ltl.generated.LtlGrammarParser.AndExpressionContext;
import org.sosy_lab.cpachecker.util.ltl.generated.LtlGrammarParser.BinaryOpContext;
import org.sosy_lab.cpachecker.util.ltl.generated.LtlGrammarParser.BinaryOperationContext;
import org.sosy_lab.cpachecker.util.ltl.generated.LtlGrammarParser.BoolContext;
import org.sosy_lab.cpachecker.util.ltl.generated.LtlGrammarParser.BooleanContext;
import org.sosy_lab.cpachecker.util.ltl.generated.LtlGrammarParser.ExpressionContext;
import org.sosy_lab.cpachecker.util.ltl.generated.LtlGrammarParser.FormulaContext;
import org.sosy_lab.cpachecker.util.ltl.generated.LtlGrammarParser.LtlPropertyContext;
import org.sosy_lab.cpachecker.util.ltl.generated.LtlGrammarParser.NestedContext;
import org.sosy_lab.cpachecker.util.ltl.generated.LtlGrammarParser.OrExpressionContext;
import org.sosy_lab.cpachecker.util.ltl.generated.LtlGrammarParser.PropertyContext;
import org.sosy_lab.cpachecker.util.ltl.generated.LtlGrammarParser.QuotedVariableContext;
import org.sosy_lab.cpachecker.util.ltl.generated.LtlGrammarParser.UnaryOpContext;
import org.sosy_lab.cpachecker.util.ltl.generated.LtlGrammarParser.UnaryOperationContext;
import org.sosy_lab.cpachecker.util.ltl.generated.LtlGrammarParser.VariableContext;
import org.sosy_lab.cpachecker.util.ltl.generated.LtlGrammarParserBaseVisitor;

public class LtlFormulaTreeVisitor extends LtlGrammarParserBaseVisitor<LtlFormula> {

  private final List<Literal> apList; // atomic propositions

  public LtlFormulaTreeVisitor() {
    apList = new ArrayList<>();
  }

  public List<Literal> getAPs() {
    return ImmutableList.copyOf(apList);
  }

  @Override
  public LtlFormula visitProperty(PropertyContext ctx) {
    // For a valid syntax, the ctx-param has to provide the following expressions in the exact
    // order: CHECK LPAREN initFunction COMMA ltlProperty RPAREN EOF
    throwException_When_InvalidChildCount(ctx.getChildCount(), 7);

    // For now, we only want to retrieve the 'ltlProperty', so we ditch everything else
    return visit(ctx.getChild(4));
  }

  @Override
  public LtlFormula visitLtlProperty(LtlPropertyContext ctx) {
    throwException_When_InvalidChildCount(ctx.getChildCount(), 4);
    return visit(ctx.getChild(2));
  }

  @Override
  public LtlFormula visitFormula(FormulaContext ctx) {
    // Contains formula + EOF
    throwException_When_InvalidChildCount(ctx.getChildCount(), 2);
    return visit(ctx.getChild(0));
  }

  @Override
  public LtlFormula visitExpression(ExpressionContext ctx) {
    // Contains an orExpression only
    throwException_When_InvalidChildCount(ctx.getChildCount(), 1);
    return visit(ctx.getChild(0));
  }

  @Override
  public LtlFormula visitOrExpression(OrExpressionContext ctx) {
    // Contains a disjunction of conjunctions
    if (ctx.getChildCount() == 0) {
      throw new RuntimeException(
          "Invalid input provided. Expected at least 1 child-node in param 'ctx', however, nothing"
              + " could be found");
    }

    ImmutableList.Builder<LtlFormula> builder = ImmutableList.builder();
    for (int i = 0; i < ctx.getChildCount(); i++) {
      if (i % 2 == 0) {
        builder.add(visit(ctx.getChild(i)));
      } else {
        if (!(ctx.getChild(i) instanceof TerminalNode)) {
          throw new RuntimeException(
              String.format(
                  "Invalid input provided. Expected child at pos %d to be an instance of"
                      + " TerminalNode",
                  i));
        }
      }
    }

    return Disjunction.of(builder.build());
  }

  @Override
  public LtlFormula visitAndExpression(AndExpressionContext ctx) {
    // Contains a conjunction of binaryExpressions
    if (ctx.getChildCount() == 0) {
      throw new RuntimeException(
          "Invalid input provided. Expected at least 1 child-node in param 'ctx', however, nothing"
              + " could be found");
    }

    ImmutableList.Builder<LtlFormula> builder = ImmutableList.builder();
    for (int i = 0; i < ctx.getChildCount(); i++) {
      if (i % 2 == 0) {
        builder.add(visit(ctx.getChild(i)));
      } else {
        if (!(ctx.getChild(i) instanceof TerminalNode)) {
          throw new RuntimeException(
              String.format(
                  "Invalid input provided. Expected child at pos %d to be an instance of"
                      + " TerminalNode",
                  i));
        }
      }
    }

    return Conjunction.of(builder.build());
  }

  @Override
  public LtlFormula visitBinaryOperation(BinaryOperationContext ctx) {
    throwException_When_InvalidChildCount(ctx.getChildCount(), 3);
    Objects.requireNonNull(ctx.left);
    Objects.requireNonNull(ctx.right);

    BinaryOpContext binaryOp = ctx.binaryOp();
    LtlFormula left = visit(ctx.left);
    LtlFormula right = visit(ctx.right);

    if (binaryOp.EQUIV() != null) {
      return Disjunction.of(Conjunction.of(left, right), Conjunction.of(left.not(), right.not()));
    }

    if (binaryOp.IMP() != null) {
      return Disjunction.of(left.not(), right);
    }

    if (binaryOp.XOR() != null) {
      return Disjunction.of(Conjunction.of(left, right.not()), Conjunction.of(left.not(), right));
    }

    if (binaryOp.UNTIL() != null) {
      return Until.of(left, right);
    }

    if (binaryOp.WUNTIL() != null) {
      return WeakUntil.of(left, right);
    }

    if (binaryOp.RELEASE() != null) {
      return Release.of(left, right);
    }

    if (binaryOp.SRELEASE() != null) {
      return StrongRelease.of(left, right);
    }

    throw new ParseCancellationException("Unknown binary operator");
  }

  @Override
  public LtlFormula visitUnaryOperation(UnaryOperationContext ctx) {
    throwException_When_InvalidChildCount(ctx.getChildCount(), 2);

    UnaryOpContext unaryOp = ctx.unaryOp();
    LtlFormula operand = visit(ctx.inner);

    if (unaryOp.NOT() != null) {
      return operand.not();
    }

    if (unaryOp.FINALLY() != null) {
      return Finally.of(operand);
    }

    if (unaryOp.GLOBALLY() != null) {
      return Globally.of(operand);
    }

    if (unaryOp.NEXT() != null) {
      return Next.of(operand);
    }

    throw new ParseCancellationException("Unknown unary operator");
  }

  @Override
  public LtlFormula visitBoolean(BooleanContext ctx) {
    throwException_When_InvalidChildCount(ctx.getChildCount(), 1);

    BoolContext constant = ctx.bool();

    if (constant.FALSE() != null) {
      return BooleanConstant.FALSE;
    }

    if (constant.TRUE() != null) {
      return BooleanConstant.TRUE;
    }

    throw new ParseCancellationException("Unknown boolean constant");
  }

  @Override
  public LtlFormula visitQuotedVariable(QuotedVariableContext ctx) {
    // Consists of: QUOTATIONMARK_START var comp val (MATHOP VALUE)* QUOTATIONMARK_END
    if (ctx.getChildCount() < 5) {
      throw new RuntimeException(
          String.format(
              "Invalid input provided. Expected %d child-nodes in param 'ctx', however, %d were"
                  + " found",
              5, ctx.getChildCount()));
    }

    // Don't actually parse the quoted string -- only retrieve it and use CParserUtils-class
    // later to convert it into an AExpression
    StringBuilder sb = new StringBuilder();
    for (int i = 1; i < ctx.getChildCount() - 1; i++) {
      sb.append(ctx.getChild(i).getText());
    }
    Literal ap = Literal.of(sb.toString(), false);
    apList.add(ap);
    return ap;
  }

  @Override
  public LtlFormula visitVariable(VariableContext ctx) {
    throwException_When_InvalidChildCount(ctx.getChildCount(), 1);

    Literal ap = Literal.of(ctx.getText(), false);
    apList.add(ap);
    return ap;
  }

  @Override
  public LtlFormula visitNested(NestedContext ctx) {
    throwException_When_InvalidChildCount(ctx.getChildCount(), 3);
    return visit(ctx.nested);
  }

  private void throwException_When_InvalidChildCount(int pActual, int pExpected) {
    if (pActual == pExpected) {
      return;
    }

    throw new RuntimeException(
        String.format(
            "Invalid input provided. Expected %d child-nodes in param 'ctx', however, %d were"
                + " found",
            pExpected, pActual));
  }
}
