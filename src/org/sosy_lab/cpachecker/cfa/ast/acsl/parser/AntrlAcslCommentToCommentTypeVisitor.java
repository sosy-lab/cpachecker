// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2026 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.cfa.ast.acsl.parser;

import org.sosy_lab.cpachecker.cfa.ast.acsl.parser.AcslComment.AcslCommentType;
import org.sosy_lab.cpachecker.cfa.ast.acsl.parser.generated.AcslGrammarBaseVisitor;
import org.sosy_lab.cpachecker.cfa.ast.acsl.parser.generated.AcslGrammarParser.AcslCommentContext;
import org.sosy_lab.cpachecker.cfa.ast.acsl.parser.generated.AcslGrammarParser.AssertionContext;
import org.sosy_lab.cpachecker.cfa.ast.acsl.parser.generated.AcslGrammarParser.FunctionContractContext;
import org.sosy_lab.cpachecker.cfa.ast.acsl.parser.generated.AcslGrammarParser.LogicDefContext;
import org.sosy_lab.cpachecker.cfa.ast.acsl.parser.generated.AcslGrammarParser.LoopAnnotContext;

public class AntrlAcslCommentToCommentTypeVisitor extends AcslGrammarBaseVisitor<AcslCommentType> {

  @Override
  public AcslCommentType visitAcslComment(AcslCommentContext ctx) {
    AcslCommentType type = super.visitAcslComment(ctx);
    if (type == null) {
      return AcslCommentType.UNKNOWN;
    }
    return type;
  }

  @Override
  public AcslCommentType visitAssertion(AssertionContext ctx) {
    return AcslCommentType.ASSERTION;
  }

  @Override
  public AcslCommentType visitLoopAnnot(LoopAnnotContext ctx) {
    return AcslCommentType.LOOP_ANNOTATION;
  }

  @Override
  public AcslCommentType visitFunctionContract(FunctionContractContext ctx) {
    return AcslCommentType.FUNCTION_CONTRACT;
  }

  @Override
  public AcslCommentType visitLogicDef(LogicDefContext ctx) {
    return AcslCommentType.LOGIC_DEF;
  }
}
