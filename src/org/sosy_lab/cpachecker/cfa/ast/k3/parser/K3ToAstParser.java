// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2025 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.cfa.ast.k3.parser;

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
import org.sosy_lab.cpachecker.cfa.ast.k3.K3Script;
import org.sosy_lab.cpachecker.cfa.ast.k3.parser.generated.K3Lexer;
import org.sosy_lab.cpachecker.cfa.ast.k3.parser.generated.K3Parser;

public class K3ToAstParser {
  private static ParseTree generateParseTree(
      String pInput, Function<@NonNull K3Parser, ParseTree> pRuleToBeApplied)
      throws K3AstParseException {
    checkNotNull(pInput);

    ParseTree tree;
    try {
      // create a lexer that feeds off of input CharStream
      K3Lexer lexer = new K3Lexer(CharStreams.fromString(pInput));
      // create a buffer of tokens pulled from the lexer
      CommonTokenStream tokens = new CommonTokenStream(lexer);
      // create a parser that feeds off the tokens buffer
      K3Parser parser = new K3Parser(tokens);

      tree = pRuleToBeApplied.apply(parser);

    } catch (ParseCancellationException e) {
      throw new K3AstParseException(e.getMessage(), e);
    }
    return Objects.requireNonNull(tree);
  }

  private static K3Script parseScript(String pInput, Optional<Path> pFilePath)
      throws K3AstParseException {
    // For some reason the ANTLR grammar expects at least one white-space after a comment
    String inputFixedComments = pInput.replaceAll(";", "; ");
    ParseTree tree = generateParseTree(inputFixedComments, pParser -> pParser.script());
    ScriptToAstConverter converter;
    if (pFilePath.isEmpty()) {
      converter = new ScriptToAstConverter(new K3CurrentScope());
    } else {
      converter = new ScriptToAstConverter(new K3CurrentScope(), pFilePath.orElseThrow());
    }

    K3Script script = converter.visit(tree);

    return script;
  }

  public static K3Script parseScript(String pInput) throws K3AstParseException {
    return parseScript(pInput, Optional.empty());
  }

  public static K3Script parseScript(Path pFilename, String pInput) throws K3AstParseException {
    return parseScript(pInput, Optional.of(pFilename));
  }

  public static K3Script parseScript(Path pFilePath) throws K3AstParseException {
    String programString;
    try {
      programString = Joiner.on("\n").join(Files.readAllLines(pFilePath));
    } catch (IOException e) {
      throw new K3AstParseException("Could not read input file: " + pFilePath, e);
    }

    return parseScript(programString, Optional.of(pFilePath));
  }

  public static class K3AstParseException extends Exception {

    @Serial private static final long serialVersionUID = 45185700123046139L;

    public K3AstParseException(String pMsg) {
      super(checkNotNull(pMsg));
    }

    public K3AstParseException(String pMsg, Throwable pCause) {
      super(checkNotNull(pMsg), checkNotNull(pCause));
    }
  }

  public static class K3ToAstNotImplementedException extends RuntimeException {

    @Serial private static final long serialVersionUID = -5496303044634390004L;

    public K3ToAstNotImplementedException(String message) {
      super(message);
    }
  }
}
