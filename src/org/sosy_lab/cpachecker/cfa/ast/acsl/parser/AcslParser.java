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
import java.io.Serial;
import java.util.Objects;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
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
import org.sosy_lab.cpachecker.cfa.ast.acsl.parser.AcslComment.AcslCommentType;
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
  public static AAcslAnnotation parseAcslComment(
      String pInput, FileLocation pFileLocation, CProgramScope pCProgramScope, AcslScope pAcslScope)
      throws AcslParseException {
    String comment = stripCommentMarker(pInput);
    ParseTree tree = generateParseTree(comment, pParser -> pParser.acslComment());
    AntlrAnnotationToAnnotationVisitor parser =
        new AntlrAnnotationToAnnotationVisitor(pCProgramScope, pAcslScope, pFileLocation);
    return parser.visit(tree);
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
    String comment = stripCommentMarker(pInput);
    ParseTree tree = generateParseTree(comment, pParser -> pParser.acslComment());
    AntrlAcslCommentToCommentTypeVisitor converter = new AntrlAcslCommentToCommentTypeVisitor();
    AcslCommentType type = converter.visit(tree);
    return type;
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
