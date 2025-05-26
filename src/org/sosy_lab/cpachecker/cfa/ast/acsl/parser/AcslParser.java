// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2025 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.cfa.ast.acsl.parser;

import static com.google.common.base.Preconditions.checkNotNull;

import com.google.common.base.Function;
import java.io.Serial;
import java.util.Objects;
import org.antlr.v4.runtime.CharStreams;
import org.antlr.v4.runtime.CommonTokenStream;
import org.antlr.v4.runtime.misc.ParseCancellationException;
import org.antlr.v4.runtime.tree.ParseTree;
import org.jspecify.annotations.NonNull;
import org.sosy_lab.cpachecker.cfa.CProgramScope;
import org.sosy_lab.cpachecker.cfa.ast.acsl.AcslLogicDefinition;
import org.sosy_lab.cpachecker.cfa.ast.acsl.AcslPredicate;
import org.sosy_lab.cpachecker.cfa.ast.acsl.AcslScope;
import org.sosy_lab.cpachecker.cfa.ast.acsl.parser.generated.AcslGrammarLexer;
import org.sosy_lab.cpachecker.cfa.ast.acsl.parser.generated.AcslGrammarParser;

public class AcslParser {

  private static ParseTree generateParseTree(
      String pInput, Function<@NonNull AcslGrammarParser, ParseTree> pRuleToBeApplied)
      throws AcslParseException {
    checkNotNull(pInput);

    ParseTree tree;
    try {
      // create a lexer that feeds off of input CharStream
      AcslGrammarLexer lexer = new AcslGrammarLexer(CharStreams.fromString(pInput));
      // create a buffer of tokens pulled from the lexer
      CommonTokenStream tokens = new CommonTokenStream(lexer);
      // create a parser that feeds off the tokens buffer
      AcslGrammarParser parser = new AcslGrammarParser(tokens);

      tree = pRuleToBeApplied.apply(parser);

    } catch (ParseCancellationException e) {
      throw new AcslParseException(e.getMessage(), e);
    }
    return Objects.requireNonNull(tree);
  }

  public static AcslPredicate parsePredicate(
      String pInput, CProgramScope pCProgramScope, AcslScope pAcslScope) throws AcslParseException {

    ParseTree tree = generateParseTree(pInput, pParser -> pParser.pred());
    AntrlPredicateToPredicateConverter converter =
        new AntrlPredicateToPredicateConverter(pCProgramScope, AcslScope.mutableCopy(pAcslScope));

    AcslPredicate expression = converter.visit(tree);

    return expression;
  }

  public static AcslLogicDefinition parseLogicalDefinition(String pInput, AcslScope pAcslScope)
      throws AcslParseException {

    ParseTree tree = generateParseTree(pInput, pParser -> pParser.logicDef());

    AntlrLogicalDefinitionToLogicalDefinitionConverter converter =
        new AntlrLogicalDefinitionToLogicalDefinitionConverter(AcslScope.mutableCopy(pAcslScope));

    AcslLogicDefinition definition = converter.visit(tree);

    return definition;
  }

  public static class AcslParseException extends Exception {
    @Serial private static final long serialVersionUID = -8907490123042996735L;

    public AcslParseException(String pMsg) {
      super(checkNotNull(pMsg));
    }

    public AcslParseException(String pMsg, Throwable pCause) {
      super(checkNotNull(pMsg), checkNotNull(pCause));
    }
  }

  public static class AntlrToInternalNotImplementedException extends RuntimeException {
    private static final long serialVersionUID = 14291283541286835L;

    public AntlrToInternalNotImplementedException(String message) {
      super(message);
    }
  }
}
