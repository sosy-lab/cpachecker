/*
 *  CPAchecker is a tool for configurable software verification.
 *  This file is part of CPAchecker.
 *
 *  Copyright (C) 2007-2018  Dirk Beyer
 *  All rights reserved.
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 *
 *
 *  CPAchecker web page:
 *    http://cpachecker.sosy-lab.org
 */
package org.sosy_lab.cpachecker.util.ltl;

import static com.google.common.base.Preconditions.checkNotNull;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.logging.Level;
import org.antlr.v4.runtime.CharStream;
import org.antlr.v4.runtime.CharStreams;
import org.antlr.v4.runtime.CommonTokenStream;
import org.antlr.v4.runtime.ConsoleErrorListener;
import org.antlr.v4.runtime.misc.ParseCancellationException;
import org.antlr.v4.runtime.tree.ParseTree;
import org.sosy_lab.common.log.LogManager;
import org.sosy_lab.cpachecker.util.ltl.formulas.LtlFormula;
import org.sosy_lab.cpachecker.util.ltl.generated.LtlGrammarParser;
import org.sosy_lab.cpachecker.util.ltl.generated.LtlGrammarParserBaseVisitor;
import org.sosy_lab.cpachecker.util.ltl.generated.LtlLexer;

/**
 * Class to transform ltl-formulas (either given as plain string or in a file) into strongly typed
 * {@link LtlFormula}s. These formulas require their syntax to be written exactly as specified in
 * the rules for the SVComp (see <a href="https://sv-comp.sosy-lab.org/2018/rules.php">rules for
 * SVComp 2018</a>).
 *
 * <p>Examples for property-files can be found <a
 * href="https://github.com/ultimate-pa/ultimate/tree/dev/trunk/examples/LTL/svcomp17format/ltl-eca">
 * within the ultimate-repository</a>
 */
public class LtlSpecificationParser extends LtlGrammarParserBaseVisitor<LtlFormula> {

  private static final String SVCOMP_SYNTAX = "CHECK( init(  ), LTL(  ) )";

  private final CharStream input;
  private final LogManager logger;

  private LtlSpecificationParser(CharStream pInput, LogManager pLogger) {
    input = checkNotNull(pInput);
    logger = pLogger;
  }

  /**
   * Checks whether the content of the file for a given path contains a string that matches exactly
   * the following: "<code>CHECK( init( ), LTL( ) )</code>". The entry function and the ltl property
   * are thereby left out from the check.
   *
   * <p>(To elaborate, the content between the parentheses after init and LTL are ignored and not
   * checked as to whether their syntax is valid.)
   *
   * @param pSpecFile a path to a file containing a ltl-property in SVComp format.
   * @return true if the property is in SVComp-format, false otherwise.
   */
  public static boolean hasValidSyntax(Path pSpecFile, LogManager pLogger) {
    try {
      return parseSyntaxFromFile(pSpecFile, pLogger).equals(SVCOMP_SYNTAX);
    } catch (LtlParseException e) {
      return false;
    }
  }

  /**
   * Parses a file for its content and returns the property as specified in the SVComp rules. The
   * resulting string is exactly of the form "<code>CHECK( init( ), LTL( ) )</code>". In particular,
   * the starting function point and the ltl property itself are left out from parsing. If the
   * property is not in the above form, a {@link LtlParseException} is thrown instead.
   *
   * <p>Note that this method is mainly thought to check whether the property of a file has a valid
   * syntax (i.e., a syntax as specified in the SVComp rules).
   *
   * @param pPath the path to a file containing a property as specified in the SVComp rules.
   * @return a string containing exactly the following sequence of statements: <code>
   *     CHECK( init( ), LTL(  ) )</code>
   * @throws LtlParseException if the property within the file is not in the above form.
   */
  private static String parseSyntaxFromFile(Path pPath, LogManager pLogger)
      throws LtlParseException {
    checkNotNull(pPath);

    try {
      return new LtlSpecificationParser(
              CharStreams.fromStream(Files.newInputStream(pPath)), pLogger)
          .getSyntaxString();
    } catch (IOException e) {
      throw new LtlParseException(e.getMessage(), e);
    }
  }

  private String getSyntaxString() throws LtlParseException {
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

      SyntaxStringTreeVisitor visitor = new SyntaxStringTreeVisitor();
      ParseTree tree = parser.property();
      String parsedSyntax = visitor.visit(tree);
      logger.log(Level.FINEST, String.format("Parsed '%s' from the input", parsedSyntax));
      return parsedSyntax;
    } catch (ParseCancellationException e) {
      throw new LtlParseException(e.getMessage(), e);
    }
  }
}
