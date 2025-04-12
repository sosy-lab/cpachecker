// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2025 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.cfa.ast.acsl;

import static com.google.common.base.Preconditions.checkNotNull;

import java.io.Serial;
import org.antlr.v4.runtime.ANTLRInputStream;
import org.antlr.v4.runtime.CharStreams;
import org.antlr.v4.runtime.CommonTokenStream;
import org.antlr.v4.runtime.misc.ParseCancellationException;
import org.antlr.v4.runtime.tree.ParseTree;
import org.sosy_lab.cpachecker.cfa.CProgramScope;
import org.sosy_lab.cpachecker.cfa.ast.acsl.generated.AcslGrammarLexer;
import org.sosy_lab.cpachecker.cfa.ast.acsl.generated.AcslGrammarParser;
import org.sosy_lab.cpachecker.util.ltl.LtlParseException;

public class AcslParser {

  public static AcslExpression parsePredicate(
      String pRaw, CProgramScope pCProgramScope, AcslScope pAcslScope)
      throws AcslParseException, LtlParseException {
    checkNotNull(pRaw);

    try {
      ANTLRInputStream input = new ANTLRInputStream();
      // create a lexer that feeds off of input CharStream
      AcslGrammarLexer lexer = new AcslGrammarLexer(CharStreams.fromString(pRaw));
      // create a buffer of tokens pulled from the lexer
      CommonTokenStream tokens = new CommonTokenStream(lexer);
      // create a parser that feeds off the tokens buffer
      AcslGrammarParser parser = new AcslGrammarParser(tokens);

      ParseTree tree = parser.pred();

    } catch (ParseCancellationException e) {
      throw new AcslParseException(e.getMessage(), e);
    }

    return tree;
  }

  private static class AcslParseException extends Exception {
    @Serial private static final long serialVersionUID = -8907490123042996735L;

    public AcslParseException(String pMsg) {
      super(checkNotNull(pMsg));
    }

    public AcslParseException(String pMsg, Throwable pCause) {
      super(checkNotNull(pMsg), checkNotNull(pCause));
    }
  }
}
