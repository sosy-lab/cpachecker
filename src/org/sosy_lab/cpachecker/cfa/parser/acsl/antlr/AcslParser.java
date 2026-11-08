// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2025 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.cfa.parser.acsl.antlr;

import static com.google.common.base.Preconditions.checkNotNull;

import com.google.common.base.Function;
import java.io.Serial;
import java.util.Objects;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.antlr.v4.runtime.BaseErrorListener;
import org.antlr.v4.runtime.CharStreams;
import org.antlr.v4.runtime.CommonTokenStream;
import org.antlr.v4.runtime.RecognitionException;
import org.antlr.v4.runtime.Recognizer;
import org.antlr.v4.runtime.misc.ParseCancellationException;
import org.antlr.v4.runtime.tree.ParseTree;
import org.jspecify.annotations.NonNull;
import org.sosy_lab.cpachecker.cfa.CProgramScope;
import org.sosy_lab.cpachecker.cfa.ast.FileLocation;
import org.sosy_lab.cpachecker.cfa.ast.acsl.AcslLogicDefinition;
import org.sosy_lab.cpachecker.cfa.ast.acsl.AcslPredicate;
import org.sosy_lab.cpachecker.cfa.ast.acsl.annotations.AAcslAnnotation;
import org.sosy_lab.cpachecker.cfa.parser.acsl.AcslComment.AcslCommentType;
import org.sosy_lab.cpachecker.cfa.parser.acsl.antlr.generated.AcslGrammarLexer;
import org.sosy_lab.cpachecker.cfa.parser.acsl.antlr.generated.AcslGrammarParser;

public class AcslParser {

  /** Reports syntax errors with an exception, because ANTLR only prints them by default. */
  private static final class ThrowingErrorListener extends BaseErrorListener {

    private static final ThrowingErrorListener INSTANCE = new ThrowingErrorListener();

    @Override
    public void syntaxError(
        Recognizer<?, ?> pRecognizer,
        Object pOffendingSymbol,
        int pLine,
        int pCharPositionInLine,
        String pMsg,
        RecognitionException pException) {
      throw new ParseCancellationException(
          "line " + pLine + ":" + pCharPositionInLine + " " + pMsg, pException);
    }
  }

  private static ParseTree generateParseTree(
      String pInput, Function<@NonNull AcslGrammarParser, ParseTree> pRuleToBeApplied)
      throws AcslParseException {
    checkNotNull(pInput);

    AcslGrammarLexer lexer = new AcslGrammarLexer(CharStreams.fromString(pInput));
    lexer.removeErrorListeners();
    lexer.addErrorListener(ThrowingErrorListener.INSTANCE);
    AcslGrammarParser parser = new AcslGrammarParser(new CommonTokenStream(lexer));
    parser.removeErrorListeners();
    parser.addErrorListener(ThrowingErrorListener.INSTANCE);

    try {
      return Objects.requireNonNull(pRuleToBeApplied.apply(parser));
    } catch (ParseCancellationException e) {
      throw new AcslParseException(
          "Syntax error in ACSL annotation '" + pInput + "': " + e.getMessage(), e);
    }
  }

  public static AcslPredicate parsePredicate(
      String pInput,
      CProgramScope pCProgramScope,
      AcslScope pAcslScope,
      FileLocation pInitialFileLocation)
      throws AcslParseException {

    ParseTree tree = generateParseTree(pInput, pParser -> pParser.pred());
    try {
      return new PredicateToAstConverter(
              pCProgramScope, AcslScope.mutableCopy(pAcslScope), pInitialFileLocation)
          .visit(tree);
    } catch (AcslParseRuntimeException e) {
      throw new AcslParseException(e.getMessage(), e);
    }
  }

  public static AcslLogicDefinition parseLogicalDefinition(
      String pInput, AcslScope pAcslScope, FileLocation pInitialFileLocation)
      throws AcslParseException {

    ParseTree tree = generateParseTree(pInput, pParser -> pParser.logicDef());
    try {
      return new LogicDefinitionToAstConverter(
              AcslScope.mutableCopy(pAcslScope), pInitialFileLocation)
          .visit(tree);
    } catch (AcslParseRuntimeException e) {
      throw new AcslParseException(e.getMessage(), e);
    }
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
  public static AAcslAnnotation parseAcslComment(
      String pInput, FileLocation pFileLocation, CProgramScope pCProgramScope, AcslScope pAcslScope)
      throws AcslParseException {
    ParseTree tree =
        generateParseTree(stripCommentMarker(pInput), pParser -> pParser.acslComment());
    try {
      return new AnnotationToAstConverter(pCProgramScope, pAcslScope, pFileLocation).visit(tree);
    } catch (AcslParseRuntimeException e) {
      throw new AcslParseException(e.getMessage(), e);
    }
  }

  /**
   * This method returns the type of annotation (assertion, function contract, loop annotation,
   * logic definition) for an acsl comment without having to parse the entire comment. This is
   * useful to differntiate between different types of annotations before the program scope, that is
   * necessary for parsing, has been built up.
   *
   * @param pInput The string containing the acsl comment
   * @return An Enum that indicates the type of the acsl comment (assertion, loop annotation,
   *     function contract, logic definition)
   * @throws AcslParseException When no parse tree can be generated for the comment.
   */
  public static AcslCommentType acslCommentToCommentType(String pInput) throws AcslParseException {
    ParseTree tree =
        generateParseTree(stripCommentMarker(pInput), pParser -> pParser.acslComment());
    return new CommentTypeVisitor().visit(tree);
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

  public static final class AcslParseException extends Exception {
    @Serial private static final long serialVersionUID = -8907490123042996735L;

    private AcslParseException(String pMsg, Throwable pCause) {
      super(checkNotNull(pMsg), checkNotNull(pCause));
    }
  }
}
