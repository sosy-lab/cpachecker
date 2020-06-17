// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
// 
// SPDX-FileCopyrightText: 2007-2020 Dirk Beyer <https://www.sosy-lab.org>
// 
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.cfa.parser.cta;

import com.google.common.collect.ImmutableList;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;
import org.antlr.v4.runtime.CharStreams;
import org.antlr.v4.runtime.CommonTokenStream;
import org.antlr.v4.runtime.ConsoleErrorListener;
import org.antlr.v4.runtime.tree.ParseTree;
import org.sosy_lab.common.log.LogManager;
import org.sosy_lab.common.time.Timer;
import org.sosy_lab.cpachecker.cfa.ParseResult;
import org.sosy_lab.cpachecker.cfa.Parser;
import org.sosy_lab.cpachecker.cfa.parser.cta.generated.CTAGrammarParser;
import org.sosy_lab.cpachecker.cfa.parser.cta.generated.CTALexer;
import org.sosy_lab.cpachecker.exceptions.ParserException;

/** Parser for a markup language for timed automata. */
class CTAParser implements Parser {

  private final LogManager logger;
  private final Timer parseTimer = new Timer();
  private final Timer cfaCreationTimer = new Timer();

  public CTAParser(final LogManager pLogger) {
    logger = pLogger;
  }

  @Override
  public ParseResult parseFile(String pFilename)
      throws ParserException, IOException, InterruptedException {

    logger.log(Level.INFO, "Start parsing timed automaton...");
    try (var input = Files.newInputStream(Paths.get(pFilename))) {
      parseTimer.start();
      CTALexer lexer = new CTALexer(CharStreams.fromStream(input));
      lexer.removeErrorListener(ConsoleErrorListener.INSTANCE);
      lexer.addErrorListener(ParserErrorListener.INSTANCE);

      CommonTokenStream tokens = new CommonTokenStream(lexer);

      CTAGrammarParser parser = new CTAGrammarParser(tokens);
      parser.removeErrorListeners();
      parser.addErrorListener(ParserErrorListener.INSTANCE);
      ParseTree tree = parser.specification();

      TCFABuilder builder = new TCFABuilder();
      builder.setFileName(pFilename);

      parseTimer.stop();
      cfaCreationTimer.start();
      builder.visit(tree);

      var nodes = builder.getNodesByAutomatonMap();
      var entryNodes = builder.getEntryNodesByAutomatonMap();

      cfaCreationTimer.stop();

      List<Path> input_file = ImmutableList.of(Paths.get(pFilename));
      return new ParseResult(entryNodes, nodes, new ArrayList<>(), input_file);
    }
  }

  @Override
  public ParseResult parseString(String pFilename, String pCode)
      throws ParserException, InterruptedException {
    return null;
  }

  @Override
  public Timer getParseTime() {
    return parseTimer;
  }

  @Override
  public Timer getCFAConstructionTime() {
    return cfaCreationTimer;
  }
}
