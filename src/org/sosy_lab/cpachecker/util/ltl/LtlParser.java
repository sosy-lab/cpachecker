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

import org.antlr.v4.runtime.CharStream;
import org.antlr.v4.runtime.CommonTokenStream;
import org.antlr.v4.runtime.ConsoleErrorListener;
import org.antlr.v4.runtime.tree.ParseTree;
import org.sosy_lab.cpachecker.util.ltl.formulas.LtlFormula;
import org.sosy_lab.cpachecker.util.ltl.generated.LtlGrammarBaseVisitor;
import org.sosy_lab.cpachecker.util.ltl.generated.LtlGrammarLexer;
import org.sosy_lab.cpachecker.util.ltl.generated.LtlGrammarParser;

abstract class LtlParser extends LtlGrammarBaseVisitor<LtlFormula> {

  private final CharStream input;

  LtlParser(CharStream input) {
    this.input = input;
  }

  abstract ParseTree getParseTree(LtlGrammarParser parser);

  LtlFormula doParse() {
    // Tokenize the stream
    LtlGrammarLexer lexer = new LtlGrammarLexer(input);
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
    return visitor.visit(tree);
  }
}
