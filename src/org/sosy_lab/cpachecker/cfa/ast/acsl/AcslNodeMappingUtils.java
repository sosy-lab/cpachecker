// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2026 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.cfa.ast.acsl;

import com.google.common.base.Verify;
import com.google.common.collect.FluentIterable;
import com.google.common.collect.ImmutableList;
import java.util.List;
import java.util.Optional;
import java.util.OptionalInt;
import org.antlr.v4.runtime.tree.ParseTree;
import org.sosy_lab.cpachecker.cfa.ast.FileLocation;
import org.sosy_lab.cpachecker.cfa.ast.acsl.parser.AcslComment;
import org.sosy_lab.cpachecker.cfa.ast.acsl.parser.AcslParser;
import org.sosy_lab.cpachecker.cfa.ast.acsl.parser.AcslParser.AcslParseException;
import org.sosy_lab.cpachecker.cfa.ast.acsl.parser.generated.AcslGrammarParser.AssertionContext;
import org.sosy_lab.cpachecker.cfa.ast.acsl.parser.generated.AcslGrammarParser.FunctionContractContext;
import org.sosy_lab.cpachecker.cfa.ast.acsl.parser.generated.AcslGrammarParser.LoopAnnotContext;
import org.sosy_lab.cpachecker.cfa.model.CFAEdge;
import org.sosy_lab.cpachecker.cfa.model.CFANode;
import org.sosy_lab.cpachecker.cfa.model.FunctionEntryNode;
import org.sosy_lab.cpachecker.cfa.model.FunctionExitNode;
import org.sosy_lab.cpachecker.util.ast.ASTElement;
import org.sosy_lab.cpachecker.util.ast.AstCfaRelation;

public class AcslNodeMappingUtils {

  /**
   * Finds the Cfa node that represents an acsl comment in the Cfa
   *
   * @param pAstCfaRelation The current AstCfaRelation
   * @return An updated version of pResult where each acsl comment now has a Cfa node that
   *     represents the comment location in the Cfa.
   */
  public static CFANode addAcslToNodeMapping(
      AcslComment pComment, List<AcslComment> pAllComments, AstCfaRelation pAstCfaRelation)
      throws AcslParseException, AcslNodeMappingException {

    ParseTree ctx = AcslParser.acslCommentToContext(pComment.getComment());
    CFANode n;
    switch (ctx) {
      case AssertionContext ignored ->
          n =
              nodeForAssertion(pComment.getFileLocation(), pAstCfaRelation)
                  .orElseThrow(
                      () ->
                          new AcslNodeMappingException(
                              "Could not find CFA node for assertion" + pComment));
      case LoopAnnotContext ignored ->
          n =
              nodeForLoopAnnotation(pComment.getFileLocation(), pAstCfaRelation)
                  .orElseThrow(
                      () ->
                          new AcslNodeMappingException(
                              "Could not find loop head for loop annotation " + pComment));
      case FunctionContractContext ignored ->
          n =
              nodeForFunctionContract(pComment, pAstCfaRelation, pAllComments)
                  .orElseThrow(
                      () ->
                          new AcslNodeMappingException(
                              "Could not find function entry node for function contract\n"
                                  + pComment
                                  + "\nStatement contracts are not supported."));
      case null ->
          throw new AcslNodeMappingException("Annotation " + pComment + " has no Antlr context.");
      default ->
          throw new AcslNodeMappingException(
              "Unexpected annotation: "
                  + pComment
                  + ". Parsing is currently supported for assertions, loop annotations,"
                  + " function contracts.");
    }
    return n;
  }

  private static Optional<CFANode> nodeForAssertion(
      FileLocation pLocation, AstCfaRelation pAstCfaRelation) {

    Optional<ASTElement> tightestStatement =
        pAstCfaRelation.getElemForStarting(
            pLocation.getStartingLineNumber(), OptionalInt.of(pLocation.getStartColumnInLine()));

    if (tightestStatement.isPresent() && !tightestStatement.orElseThrow().edges().isEmpty()) {

      FluentIterable<CFANode> predecessors =
          FluentIterable.from(tightestStatement.orElseThrow().edges())
              .transform(e -> e.getPredecessor());
      FluentIterable<CFANode> successors =
          FluentIterable.from(tightestStatement.orElseThrow().edges())
              .transform(e -> e.getSuccessor());
      List<CFANode> nodesForComment =
          successors
              .filter(n -> !predecessors.contains(n) && !(n instanceof FunctionExitNode))
              .toList();

      return Optional.of(nodesForComment.getFirst());
    }
    return Optional.empty();
  }

  private static Optional<CFANode> nodeForLoopAnnotation(
      FileLocation pLocation, AstCfaRelation pAstCfaRelation) {

    FileLocation nextStatement =
        pAstCfaRelation.nextStartStatementLocation(pLocation.getNodeOffset());
    Optional<CFANode> iterationNode =
        pAstCfaRelation.getNodeForIterationStatementLocation(
            nextStatement.getStartingLineNumber(), nextStatement.getStartColumnInLine());

    if (iterationNode.isPresent()) {
      Verify.verify(iterationNode.orElseThrow().isLoopStart());
      return iterationNode;
    }

    return Optional.empty();
  }

  /**
   * @param pComment An AcslComment that is possibly a function contract
   * @param pAstCfaRelation The current Ast Cfa Relation
   * @param pAllComments All Acsl Comments of the current Parse Result
   * @return - The next Function Entry Node if pComment is a function contract - Optional.empty()
   *     otherwise.
   */
  private static Optional<CFANode> nodeForFunctionContract(
      AcslComment pComment, AstCfaRelation pAstCfaRelation, List<AcslComment> pAllComments) {

    FileLocation nextLocation =
        pAstCfaRelation.nextStartStatementLocation(pComment.getFileLocation().getNodeOffset());

    if (nextLocation.isRealLocation() && noCommentInBetween(pComment, nextLocation, pAllComments)) {
      Optional<CFANode> nextNode =
          pAstCfaRelation.getNodeForStatementLocation(
              nextLocation.getStartingLineNumber(), nextLocation.getStartColumnInLine());

      if (nextNode.isPresent()) {
        ImmutableList<CFAEdge> edges =
            nextNode
                .orElseThrow()
                .getEnteringEdges()
                .filter(e -> e.getPredecessor() instanceof FunctionEntryNode)
                .toList();
        if (edges.size() == 1 && edges.getFirst().getPredecessor() instanceof FunctionEntryNode f) {
          return Optional.of(f);
        }
      }
    }

    return Optional.empty();
  }

  public static boolean noCommentInBetween(
      AcslComment pComment, FileLocation nextStatement, List<AcslComment> otherComments) {
    for (AcslComment other : otherComments) {
      if (!other.equals(pComment)
          && other.getFileLocation().getNodeOffset()
              > pComment.getFileLocation().getNodeOffset()
                  + pComment.getFileLocation().getNodeLength()
          && other.getFileLocation().getNodeOffset() + other.getFileLocation().getNodeLength()
              < nextStatement.getNodeOffset()) {
        // There is an annotation inbetween the comment and the statement
        return false;
      }
    }
    return true;
  }
}
