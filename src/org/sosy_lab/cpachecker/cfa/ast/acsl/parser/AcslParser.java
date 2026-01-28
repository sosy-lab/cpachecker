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
import org.sosy_lab.cpachecker.cfa.ast.acsl.annotations.AcslAssertion;
import org.sosy_lab.cpachecker.cfa.ast.acsl.annotations.AcslEnsures;
import org.sosy_lab.cpachecker.cfa.ast.acsl.annotations.AcslLoopInvariant;
import org.sosy_lab.cpachecker.cfa.ast.acsl.annotations.AcslRequires;
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

    AcslPredicate expression = converter.visit(tree);

    return expression;
  }

  public static AcslLogicDefinition parseLogicalDefinition(String pInput, AcslScope pAcslScope)
      throws AcslParseException {

    ParseTree tree = generateParseTree(pInput, pParser -> pParser.logicDef());

    AntlrLogicalDefinitionToLogicalDefinitionConverter converter =
        new AntlrLogicalDefinitionToLogicalDefinitionConverter(AcslScope.mutableCopy(pAcslScope));

    AcslLogicDefinition definition = converter.visit(tree);

    return definition;
  }

  public static ImmutableList<AAcslAnnotation> parseAcslComment(
      String pInput, FileLocation pFileLocation, CProgramScope pCProgramScope, AcslScope pAcslScope)
      throws AcslParseException {

    ImmutableList.Builder<AAcslAnnotation> annotations = ImmutableList.builder();

    String comment = stripCommentMarker(pInput);
    ImmutableList<String> statements = splitAnnotation(comment);

    for (String s : statements) {
      AAcslAnnotation annotation =
          parseAcslAnnotation(s, pFileLocation, pCProgramScope, pAcslScope);
      annotations.add(annotation);
    }

    return annotations.build();
  }

  public static AAcslAnnotation parseAcslAnnotation(
      String pInput, FileLocation pFileLocation, CProgramScope pCProgramScope, AcslScope pAcslScope)
      throws AcslParseException {
    if (pInput.startsWith("assert")) {
      return parseAcslAssertion(pInput, pFileLocation, pCProgramScope, pAcslScope);
    } else if (pInput.startsWith("loop invariant")) {
      return parseAcslLoopInvariant(pInput, pFileLocation, pCProgramScope, pAcslScope);
    } else if (pInput.startsWith("ensures")) {
      return parseAcslEnsures(pInput, pFileLocation, pCProgramScope, pAcslScope);
    } else if (pInput.startsWith("requires")) {
      return parseAcslRequires(pInput, pFileLocation, pCProgramScope, pAcslScope);
    }
    throw new AcslParseException(
        pFileLocation
            + ": Only assert, loop invariant, ensures and requires are allowed as annotations");
  }

  public static AcslAssertion parseAcslAssertion(
      String pInput, FileLocation pFileLocation, CProgramScope pCProgramScope, AcslScope pAcslScope)
      throws AcslParseException {
    ParseTree tree = generateParseTree(pInput, pParser -> pParser.assertion());
    AntlrAnnotationToAnnotationVisitor converter =
        new AntlrAnnotationToAnnotationVisitor(pCProgramScope, pAcslScope, pFileLocation);
    AAcslAnnotation assertion = converter.visit(tree);
    return (AcslAssertion) assertion;
  }

  public static AcslLoopInvariant parseAcslLoopInvariant(
      String pInput, FileLocation pFileLocation, CProgramScope pCProgramScope, AcslScope pAcslScope)
      throws AcslParseException {
    ParseTree tree = generateParseTree(pInput, pParser -> pParser.loop_invariant());
    AntlrAnnotationToAnnotationVisitor converter =
        new AntlrAnnotationToAnnotationVisitor(pCProgramScope, pAcslScope, pFileLocation);
    AAcslAnnotation loopInvariant = converter.visit(tree);
    return (AcslLoopInvariant) loopInvariant;
  }

  public static AcslEnsures parseAcslEnsures(
      String pInput, FileLocation pFileLocation, CProgramScope pCProgramScope, AcslScope pAcslScope)
      throws AcslParseException {
    ParseTree tree = generateParseTree(pInput, pParser -> pParser.ensures_clause());
    AntlrAnnotationToAnnotationVisitor converter =
        new AntlrAnnotationToAnnotationVisitor(pCProgramScope, pAcslScope, pFileLocation);
    AAcslAnnotation ensures = converter.visit(tree);
    return (AcslEnsures) ensures;
  }

  public static AcslRequires parseAcslRequires(
      String pInput, FileLocation pFileLocation, CProgramScope pCProgramScope, AcslScope pAcslScope)
      throws AcslParseException {
    ParseTree tree = generateParseTree(pInput, pParser -> pParser.requires_clause());
    AntlrAnnotationToAnnotationVisitor converter =
        new AntlrAnnotationToAnnotationVisitor(pCProgramScope, pAcslScope, pFileLocation);
    AAcslAnnotation requires = converter.visit(tree);
    return (AcslRequires) requires;
  }

  private static ImmutableList<String> splitAnnotation(String pInput) {
    ImmutableList.Builder<String> statements = ImmutableList.builder();
    Pattern pattern = Pattern.compile("(\\s*)(?<statement>\\S+.*;)");
    Matcher matcher = pattern.matcher(pInput);
    while (matcher.find()) {
      statements.add(matcher.group("statement"));
    }
    return statements.build();
  }

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
