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
import com.google.common.collect.ImmutableList;
import java.io.Serial;
import java.util.Objects;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import jdk.jshell.spi.ExecutionControl.NotImplementedException;
import org.antlr.v4.runtime.CharStreams;
import org.antlr.v4.runtime.CommonTokenStream;
import org.antlr.v4.runtime.misc.ParseCancellationException;
import org.antlr.v4.runtime.tree.ParseTree;
import org.jspecify.annotations.NonNull;
import org.sosy_lab.cpachecker.cfa.CProgramScope;
import org.sosy_lab.cpachecker.cfa.ast.FileLocation;
import org.sosy_lab.cpachecker.cfa.ast.acsl.AcslLogicDefinition;
import org.sosy_lab.cpachecker.cfa.ast.acsl.AcslPredicate;
import org.sosy_lab.cpachecker.cfa.ast.acsl.AcslScope;
import org.sosy_lab.cpachecker.cfa.ast.acsl.annotations.AAcslAnnotation;
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
    AntlrPredicateToPredicateConverter converter =
        new AntlrPredicateToPredicateConverter(pCProgramScope, AcslScope.mutableCopy(pAcslScope));

    return converter.visit(tree);
  }

  public static AcslLogicDefinition parseLogicalDefinition(String pInput, AcslScope pAcslScope)
      throws AcslParseException {

    ParseTree tree = generateParseTree(pInput, pParser -> pParser.logicDef());

    AntlrLogicalDefinitionToLogicalDefinitionConverter converter =
        new AntlrLogicalDefinitionToLogicalDefinitionConverter(AcslScope.mutableCopy(pAcslScope));

    AcslLogicDefinition definition = converter.visit(tree);

    return definition;
  }

  /**
   * Splits an acsl comment string into individual acsl statement string. The acsl comment can be
   * surrounded by comment markers and contain multiple acsl statement.
   *
   * @param pInput An acsl comment string that contains one or multiple acsl statements. Individual
   *     acsl statements are seperated by a semicolon
   * @param pFileLocation The location where the acsl comment occurs in the source
   * @param pCProgramScope the CProgramScope of the source program
   * @param pAcslScope the AcslScope of the source
   * @return A List of AAcslAnnotations for all acsl statements within the acsl comment
   * @throws AcslParseException when one of the acsl statements of the comment is not of type
   *     assertion, loop invariant, ensures or requires
   */
  public static ImmutableList<AAcslAnnotation> parseAcslComment(
      String pInput, FileLocation pFileLocation, CProgramScope pCProgramScope, AcslScope pAcslScope)
      throws AcslParseException {

    ImmutableList.Builder<AAcslAnnotation> annotations = ImmutableList.builder();

    String comment = stripCommentMarker(pInput);
    ImmutableList<String> statements = splitAnnotation(comment);

    for (String s : statements) {
      AAcslAnnotation annotation;
      try {
        annotation = parseSingleAcslStatement(s, pFileLocation, pCProgramScope, pAcslScope);
      } catch (NotImplementedException e) {
        throw new AcslParseException(e.getMessage());
      }
      annotations.add(annotation);
    }

    return annotations.build();
  }

  /**
   * Parses an acsl statement into an AAcslAnnotation.
   *
   * @param pInput A string that contains a single acsl statement of the type assertion, loop
   *     invariant, ensures or requires
   * @param pFileLocation The location where the acsl comment that contains this statement appears
   *     in the source
   * @param pCProgramScope The CProgramScope of the source
   * @param pAcslScope The AcslScope of the source
   * @return An AAcslAnnotation from the input statement
   */
  public static AAcslAnnotation parseSingleAcslStatement(
      String pInput, FileLocation pFileLocation, CProgramScope pCProgramScope, AcslScope pAcslScope)
      throws AcslParseException, NotImplementedException {
    ParseTree tree = generateParseTree(pInput, pParser -> pParser.acslStatement());
    AntlrAnnotationToAnnotationVisitor converter =
        new AntlrAnnotationToAnnotationVisitor(pCProgramScope, pAcslScope, pFileLocation);
    AAcslAnnotation result = converter.visit(tree);
    if (result == null) {
      throw new AntlrToInternalNotImplementedException(
          "Parsing of: "
              + pInput
              + " at: "
              + pFileLocation
              + " failed. Currently only 'assert', 'ensures', 'assigns' and 'loop invariant' are"
              + " supported.");
    }
    return result;
  }

  /**
   * @param pInput An acsl comment string that contains one or multiple acsl statements. Individual
   *     acsl statements are seperated by semicolons
   * @return A list of the individual acsl statement strings
   */
  private static ImmutableList<String> splitAnnotation(String pInput) {
    ImmutableList.Builder<String> statements = ImmutableList.builder();
    Pattern pattern = Pattern.compile("(?<statement>\\w+[^;]*;)");
    Matcher matcher = pattern.matcher(pInput);
    while (matcher.find()) {
      statements.add(matcher.group("statement"));
    }
    return statements.build();
  }

  /**
   * Removes the comment markers from an acsl comment, if they are present
   *
   * @param pCommentString An acsl comment string that might contain acsl comment markers
   * @return the acsl comment string without comment markers
   */
  public static String stripCommentMarker(String pCommentString) {
    String commentString = pCommentString;
    if (pCommentString.startsWith("//@")) {
      Pattern pattern = Pattern.compile("(//@(\\s)*)(?<content>.*)");
      Matcher matcher = pattern.matcher(pCommentString);
      if (matcher.matches()) {
        commentString = matcher.group("content");
      }
    } else if (pCommentString.startsWith("/*@")) {
      Pattern pattern = Pattern.compile("/\\*@\\s*(?<content>(.*\\s*)*)\\*/");
      Matcher matcher = pattern.matcher(commentString);
      if (matcher.matches()) {
        commentString = matcher.group("content");
      }
    }

    return commentString;
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
