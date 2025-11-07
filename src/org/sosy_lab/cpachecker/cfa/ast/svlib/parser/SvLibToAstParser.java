// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2025 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.cfa.ast.svlib.parser;

import static com.google.common.base.Preconditions.checkNotNull;

import com.google.common.base.Function;
import com.google.common.base.Joiner;
import java.io.IOException;
import java.io.Serial;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Objects;
import java.util.Optional;
import org.antlr.v4.runtime.CharStreams;
import org.antlr.v4.runtime.CommonTokenStream;
import org.antlr.v4.runtime.misc.ParseCancellationException;
import org.antlr.v4.runtime.tree.ParseTree;
import org.jspecify.annotations.NonNull;
import org.sosy_lab.cpachecker.cfa.ast.svlib.SvLibScript;
import org.sosy_lab.cpachecker.cfa.ast.svlib.parser.generated.SvLibLexer;
import org.sosy_lab.cpachecker.cfa.ast.svlib.parser.generated.SvLibParser;

public class SvLibToAstParser {
  private static ParseTree generateParseTree(
      String pInput, Function<@NonNull SvLibParser, ParseTree> pRuleToBeApplied)
      throws SvLibAstParseException {
    checkNotNull(pInput);

    ParseTree tree;
    try {
      // create a lexer that feeds off of input CharStream
      SvLibLexer lexer = new SvLibLexer(CharStreams.fromString(pInput));
      // create a buffer of tokens pulled from the lexer
      CommonTokenStream tokens = new CommonTokenStream(lexer);
      // create a parser that feeds off the tokens buffer
      SvLibParser parser = new SvLibParser(tokens);

      tree = pRuleToBeApplied.apply(parser);

    } catch (ParseCancellationException e) {
      throw new SvLibAstParseException(e.getMessage(), e);
    }
    return Objects.requireNonNull(tree);
  }

  private static SvLibScript parseScript(String pInput, Optional<Path> pFilePath)
      throws SvLibAstParseException {
    // For some reason the ANTLR grammar expects at least one white-space after a comment
    String inputFixedComments = pInput.replaceAll(";", "; ");
    ParseTree tree = generateParseTree(inputFixedComments, pParser -> pParser.script());
    ScriptToAstConverter converter;
    if (pFilePath.isEmpty()) {
      converter = new ScriptToAstConverter(new SvLibCurrentScope());
    } else {
      converter = new ScriptToAstConverter(new SvLibCurrentScope(), pFilePath.orElseThrow());
    }

    SvLibScript script = converter.visit(tree);

    return script;
  }

  public static SvLibScript parseScript(String pInput) throws SvLibAstParseException {
    return parseScript(pInput, Optional.empty());
  }

  public static SvLibScript parseScript(Path pFilename, String pInput)
      throws SvLibAstParseException {
    return parseScript(pInput, Optional.of(pFilename));
  }

  public static SvLibScript parseScript(Path pFilePath) throws SvLibAstParseException {
    String programString;
    try {
      programString = Joiner.on("\n").join(Files.readAllLines(pFilePath));
    } catch (IOException e) {
      throw new SvLibAstParseException("Could not read input file: " + pFilePath, e);
    }

    return parseScript(programString, Optional.of(pFilePath));
  }

  public static class SvLibAstParseException extends Exception {

    @Serial private static final long serialVersionUID = 45185700123046139L;

    public SvLibAstParseException(String pMsg) {
      super(checkNotNull(pMsg));
    }

    public SvLibAstParseException(String pMsg, Throwable pCause) {
      super(checkNotNull(pMsg), checkNotNull(pCause));
    }
  }

  public static class SvLibToAstNotImplementedException extends RuntimeException {

    @Serial private static final long serialVersionUID = -5496303044634390004L;

    public SvLibToAstNotImplementedException(String message) {
      super(message);
    }
  }
}
