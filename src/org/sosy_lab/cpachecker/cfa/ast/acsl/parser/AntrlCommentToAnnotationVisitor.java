// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2026 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.cfa.ast.acsl.parser;

import org.antlr.v4.runtime.ParserRuleContext;
import org.sosy_lab.cpachecker.cfa.ast.acsl.parser.generated.AcslGrammarBaseVisitor;
import org.sosy_lab.cpachecker.cfa.ast.acsl.parser.generated.AcslGrammarParser.AcslCommentContext;
import org.sosy_lab.cpachecker.cfa.ast.acsl.parser.generated.AcslGrammarParser.AssertionContext;
import org.sosy_lab.cpachecker.cfa.ast.acsl.parser.generated.AcslGrammarParser.Function_contractContext;
import org.sosy_lab.cpachecker.cfa.ast.acsl.parser.generated.AcslGrammarParser.Loop_invariantContext;
import org.sosy_lab.cpachecker.cfa.ast.acsl.parser.generated.AcslGrammarParser.Statement_contractContext;

public class AntrlCommentToAnnotationVisitor extends AcslGrammarBaseVisitor<ParserRuleContext> {

  @Override
  public ParserRuleContext visitAcslComment(AcslCommentContext ctx) {
    return super.visitAcslComment(ctx);
  }

  @Override
  public AssertionContext visitAssertion(AssertionContext ctx) {
    return ctx;
  }

  @Override
  public Loop_invariantContext visitLoop_invariant(Loop_invariantContext ctx) {
    return ctx;
  }

  @Override
  public Function_contractContext visitFunction_contract(Function_contractContext ctx) {
    return ctx;
  }

  @Override
  public Statement_contractContext visitStatement_contract(Statement_contractContext ctx) {
    return ctx;
  }
}
