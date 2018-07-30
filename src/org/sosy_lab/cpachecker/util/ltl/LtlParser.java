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

import com.google.common.annotations.VisibleForTesting;
import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.Reader;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.util.Objects;
import org.antlr.v4.runtime.CharStream;
import org.antlr.v4.runtime.CharStreams;
import org.antlr.v4.runtime.CommonTokenStream;
import org.antlr.v4.runtime.ConsoleErrorListener;
import org.antlr.v4.runtime.tree.ParseTree;
import org.sosy_lab.cpachecker.util.ltl.formulas.LabelledFormula;
import org.sosy_lab.cpachecker.util.ltl.formulas.LtlFormula;
import org.sosy_lab.cpachecker.util.ltl.generated.LtlGrammarParser;
import org.sosy_lab.cpachecker.util.ltl.generated.LtlGrammarParserBaseVisitor;
import org.sosy_lab.cpachecker.util.ltl.generated.LtlLexer;

abstract class LtlParser extends LtlGrammarParserBaseVisitor<LtlFormula> {

  private final CharStream input;

  LtlParser(CharStream pInput) {
    this.input = pInput;
  }

  public static LabelledFormula parseString(String pRaw) {
    Objects.requireNonNull(pRaw);

    return new LtlFormulaParser(CharStreams.fromString(pRaw)).doParse();
  }

  /**
   * Parse a string in the format <code>CHECK( init(func()), LTL( FORMULA ))</code>, where FORMULA
   * is a valid ltl-formula. This method should be used for testing only.
   *
   * <p>The syntax is taken from <a href="https://github.com/ultimate-pa/ultimate/issues/119">issue
   * 119</a> from ultimate-pa
   *
   * <p>Examples for property-files can be found <a
   * href="https://github.com/ultimate-pa/ultimate/tree/dev/trunk/examples/LTL/svcomp17format/ltl-eca">
   * at the ultimate-repository</a>
   *
   * @param pRaw a string with a valid SVComp syntax
   * @return a strongly typed {@link LtlFormula}
   */
  @VisibleForTesting
  static LabelledFormula parseRaw_SVCompSyntax(String pRaw) {
    Objects.requireNonNull(pRaw);

    return new LtlPropertyFileParser(CharStreams.fromString(pRaw)).doParse();
  }

  public static LabelledFormula parsePropertyFromFile(String pPath) throws LtlParseException {
    Objects.requireNonNull(pPath);

    String raw = parseFile(pPath);
    // TODO: add logging information
    //    Logger.log(String.format("Ltl property retrieved from path: %s", raw));

    if (isPropertyFile(pPath)) {
      return new LtlPropertyFileParser(CharStreams.fromString(raw)).doParse();
    } else {
      return parseString(raw);
    }
  }

  abstract ParseTree getParseTree(LtlGrammarParser pParser);

  LabelledFormula doParse() {
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
  }

  private static boolean isPropertyFile(String pName) {
    // TODO: needs to be discussed how a file with a ltl-property is specified
    return pName.endsWith(".prp");
  }

  /**
   * Parses a file and returns the first non-empty line it finds. The returned string-object is
   * subject to change due to the specification of the property-file not being determined yet.
   *
   * @param pPath the relative path to the file
   * @return a string containing a ltl-property
   * @throws LtlParseException in case either the path to the file is invalid, or an IOException
   *     happens while opening or reading the file
   */
  private static String parseFile(String pPath) throws LtlParseException {

    File file = new File(pPath);
    if (!file.exists()) {
      throw new LtlParseException(
          String.format(
              "Path to file with ltl-property provided ('%s'), however, no such file could be found at %s",
              pPath, file.getAbsolutePath()));
    }

    // TODO: add logging information
    //    Logger.log(String.format("Reading from path %s", pPath));

    String ltlProperty = null;

    try (Reader reader = Files.newBufferedReader(file.toPath(), Charset.defaultCharset());
        BufferedReader bufferedreader = new BufferedReader(reader); ) {

      String line;
      while ((line = bufferedreader.readLine()) != null) {
        if (!line.isEmpty()) {
          ltlProperty = line;
          break;
        }
      }
    } catch (IOException e) {
      throw new LtlParseException(e.getMessage(), e);
    }

    if (ltlProperty == null) {
      throw new LtlParseException("LTL file provided, but no ltl-property could be found");
    }

    return ltlProperty;
  }
}
