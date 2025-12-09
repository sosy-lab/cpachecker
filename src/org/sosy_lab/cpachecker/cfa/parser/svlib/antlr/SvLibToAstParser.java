// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2025 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.cfa.parser.svlib.antlr;

import static com.google.common.base.Preconditions.checkNotNull;

import com.google.common.base.Function;
import com.google.common.collect.ImmutableMap;
import java.io.IOException;
import java.io.Serial;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Objects;
import java.util.Optional;
import org.antlr.v4.runtime.BailErrorStrategy;
import org.antlr.v4.runtime.CharStreams;
import org.antlr.v4.runtime.CommonTokenStream;
import org.antlr.v4.runtime.misc.ParseCancellationException;
import org.antlr.v4.runtime.tree.ParseTree;
import org.jspecify.annotations.NonNull;
import org.sosy_lab.cpachecker.cfa.ast.svlib.specification.SvLibTagReference;
import org.sosy_lab.cpachecker.cfa.parser.svlib.antlr.generated.SvLibLexer;
import org.sosy_lab.cpachecker.cfa.parser.svlib.antlr.generated.SvLibParser;
import org.sosy_lab.cpachecker.cfa.parser.svlib.ast.SvLibScript;
import org.sosy_lab.cpachecker.cfa.parser.svlib.ast.SvLibWitness;

public class SvLibToAstParser {

  public record SvLibParsingResult(
      SvLibScript script, ImmutableMap<SvLibTagReference, SvLibScope> tagReferenceScopes) {
    public SvLibParsingResult {
      checkNotNull(script);
      checkNotNull(tagReferenceScopes);
    }
  }

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

      parser.setErrorHandler(new BailErrorStrategy());

      tree = pRuleToBeApplied.apply(parser);

    } catch (ParseCancellationException e) {
      String message = e.getMessage();
      if (message == null || message.isEmpty()) {
        message = "Unknown parsing error.";
      }

      throw new SvLibAstParseException(message, e);
    }
    return Objects.requireNonNull(tree);
  }

  private static SvLibParsingResult parseScript(String pInput, Optional<Path> pFilePath)
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

    SvLibParsingResult script = converter.visit(tree);

    return script;
  }

  public static SvLibParsingResult parseScript(String pInput) throws SvLibAstParseException {
    return parseScript(pInput, Optional.empty());
  }

  public static SvLibParsingResult parseScript(Path pFilename, String pInput)
      throws SvLibAstParseException {
    return parseScript(pInput, Optional.of(pFilename));
  }

  public static SvLibParsingResult parseScript(Path pFilePath) throws SvLibAstParseException {
    String programString;
    try {
      programString = Files.readString(pFilePath);
    } catch (IOException e) {
      throw new SvLibAstParseException("Could not read input file: " + pFilePath, e);
    }

    return parseScript(programString, Optional.of(pFilePath));
  }

  private static SvLibWitness parseWitness(String pInput, Optional<Path> pFilePath)
      throws SvLibAstParseException {
    // For some reason the ANTLR grammar expects at least one white-space after a comment
    String inputFixedComments = pInput.replaceAll(";", "; ");
    ParseTree tree = generateParseTree(inputFixedComments, pParser -> pParser.witness());
    WitnessToAstConverter converter;

    SvLibCurrentScope scope = new SvLibCurrentScope();
    if (pFilePath.isEmpty()) {
      converter = new WitnessToAstConverter(scope);
    } else {
      converter = new WitnessToAstConverter(scope, pFilePath.orElseThrow());
    }

    SvLibWitness witness = converter.visit(tree);

    return witness;
  }

  public static SvLibWitness parseWitness(String pInput) throws SvLibAstParseException {
    return parseWitness(pInput, Optional.empty());
  }

  public static SvLibWitness parseWitness(Path pFilename, String pInput)
      throws SvLibAstParseException {
    return parseWitness(pInput, Optional.of(pFilename));
  }

  public static SvLibWitness parseWitness(Path pFilePath) throws SvLibAstParseException {
    String programString;
    try {
      programString = Files.readString(pFilePath);
    } catch (IOException e) {
      throw new SvLibAstParseException("Could not read input file: " + pFilePath, e);
    }

    return parseWitness(programString, Optional.of(pFilePath));
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
