// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2007-2020 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.util.ltl;

import static com.google.common.base.Preconditions.checkNotNull;

import com.google.common.annotations.VisibleForTesting;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import org.antlr.v4.runtime.CharStream;
import org.antlr.v4.runtime.CharStreams;
import org.antlr.v4.runtime.CommonTokenStream;
import org.antlr.v4.runtime.ConsoleErrorListener;
import org.antlr.v4.runtime.misc.ParseCancellationException;
import org.antlr.v4.runtime.tree.ParseTree;
import org.sosy_lab.cpachecker.util.ltl.formulas.LabelledFormula;
import org.sosy_lab.cpachecker.util.ltl.formulas.LtlFormula;
import org.sosy_lab.cpachecker.util.ltl.generated.LtlGrammarParser;
import org.sosy_lab.cpachecker.util.ltl.generated.LtlGrammarParserBaseVisitor;
import org.sosy_lab.cpachecker.util.ltl.generated.LtlLexer;

/**
 * Class to transform ltl-formulas (either given as plain string or in a file) into strongly typed
 * {@link LtlFormula}s. These formulas can be either given in plain ltl syntax, or instead as a
 * syntax as specified in the rules for the SVComp (see <a
 * href="https://sv-comp.sosy-lab.org/2018/rules.php">rules for SVComp 2018</a>).
 *
 * <p>Examples for property-files can be found <a href=
 * "https://github.com/ultimate-pa/ultimate/tree/dev/trunk/examples/LTL/svcomp17format/ltl-eca">
 * within the ultimate-repository</a>
 */
public abstract class LtlParser extends LtlGrammarParserBaseVisitor<LtlFormula> {

  private final CharStream input;

  private LtlParser(CharStream pInput) {
    input = checkNotNull(pInput);
  }

  /**
   * Parse a ltl property given as string and transform it into a {@link LtlFormula}.
   *
   * @param pRaw the ltl property in valid ltl syntax
   * @return a {@link LabelledFormula} containing the {@link LtlFormula} and its atomic propositions
   * @throws LtlParseException if the syntax of the ltl-property is invalid.
   */
  public static LabelledFormula parseProperty(String pRaw) throws LtlParseException {
    checkNotNull(pRaw);
    return new LtlFormulaParser(CharStreams.fromString(pRaw)).doParse();
  }

  /**
   * Parse a ltl property from a file into a {@link LtlFormula}. The property is required to be in
   * SVComp-format (i.e., <code>CHECK( init( FUNCTION ), LTL( FORMULA )) )</code>, where FORMULA is
   * a valid ltl property and FUNCTION a valid function name for code written in C.
   *
   * @param pPath path to a file containing a ltl property in valid SVComp syntax.
   * @return a {@link LabelledFormula} which contains the {@link LtlFormula} and its atomic
   *     propositions
   * @throws LtlParseException if the syntax of the ltl-property is invalid.
   */
  public static LabelledFormula parseSpecificationFromFile(Path pPath) throws LtlParseException {
    checkNotNull(pPath);

    try {
      return new LtlPropertyFileParser(CharStreams.fromStream(Files.newInputStream(pPath)))
          .doParse();
    } catch (IOException e) {
      throw new LtlParseException(e.getMessage(), e);
    }
  }

  /**
   * Parse a string of the format <code>CHECK( init(func()), LTL( FORMULA ))</code>, where FORMULA
   * is a valid ltl formula. This method should be used for testing only.
   *
   * @param pRaw a string with a valid SVComp syntax
   * @return a strongly typed {@link LtlFormula}
   * @throws LtlParseException thrown when a syntactically wrong ltl-formula is submitted
   */
  @VisibleForTesting
  static LabelledFormula parseSpecification(String pRaw) throws LtlParseException {
    checkNotNull(pRaw);

    return new LtlPropertyFileParser(CharStreams.fromString(pRaw)).doParse();
  }

  abstract ParseTree getParseTree(LtlGrammarParser pParser);

  LabelledFormula doParse() throws LtlParseException {
    try {
      // Tokenize the stream
      LtlLexer lexer = new LtlLexer(input);
      // Raise an exception instead of printing long error messages on the console
      // For more informations, see https://stackoverflow.com/a/26573239/8204996
      lexer.removeErrorListener(ConsoleErrorListener.INSTANCE);
      // Add a fail-fast behavior for token errors
      lexer.addErrorListener(LtlParserErrorListener.INSTANCE);

      CommonTokenStream tokens = new CommonTokenStream(lexer);

      // Parse the tokens
      LtlGrammarParser parser = new LtlGrammarParser(tokens);
      parser.removeErrorListeners();
      parser.addErrorListener(LtlParserErrorListener.INSTANCE);

      LtlFormulaTreeVisitor visitor = new LtlFormulaTreeVisitor();
      ParseTree tree = getParseTree(parser);

      return LabelledFormula.of(visitor.visit(tree), visitor.getAPs());
    } catch (ParseCancellationException e) {
      throw new LtlParseException(e.getMessage(), e);
    }
  }

  private static class LtlFormulaParser extends LtlParser {

    LtlFormulaParser(CharStream input) {
      super(input);
    }

    @Override
    ParseTree getParseTree(LtlGrammarParser parser) {
      return parser.formula();
    }
  }

  private static class LtlPropertyFileParser extends LtlParser {

    LtlPropertyFileParser(CharStream input) {
      super(input);
    }

    @Override
    ParseTree getParseTree(LtlGrammarParser parser) {
      return parser.property();
    }
  }
}
