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
import java.util.List;
import java.util.Optional;
import java.util.OptionalInt;
import org.antlr.v4.runtime.tree.ParseTree;
import org.sosy_lab.cpachecker.cfa.CFA;
import org.sosy_lab.cpachecker.cfa.ast.FileLocation;
import org.sosy_lab.cpachecker.cfa.ast.acsl.parser.AcslComment;
import org.sosy_lab.cpachecker.cfa.ast.acsl.parser.AcslParser;
import org.sosy_lab.cpachecker.cfa.ast.acsl.parser.AcslParser.AcslParseException;
import org.sosy_lab.cpachecker.cfa.ast.acsl.parser.generated.AcslGrammarParser.AssertionContext;
import org.sosy_lab.cpachecker.cfa.ast.acsl.parser.generated.AcslGrammarParser.FunctionContractContext;
import org.sosy_lab.cpachecker.cfa.ast.acsl.parser.generated.AcslGrammarParser.LoopAnnotContext;
import org.sosy_lab.cpachecker.cfa.model.CFANode;
import org.sosy_lab.cpachecker.cfa.model.FunctionExitNode;
import org.sosy_lab.cpachecker.util.ast.ASTElement;
import org.sosy_lab.cpachecker.util.ast.AstCfaRelation;

public class AcslNodeMappingUtils {

  /**
   * Finds the Cfa node that represents an acsl comment in the Cfa
   *
   * @return An updated version of pResult where each acsl comment now has a Cfa node that
   *     represents the comment location in the Cfa.
   */
  public static CFANode addAcslToNodeMapping(
      AcslComment pComment, List<AcslComment> pAllComments, CFA pCFA)
      throws AcslParseException, AcslNodeMappingException {

    ParseTree ctx = AcslParser.acslCommentToContext(pComment.getComment());
    CFANode n;
    switch (ctx) {
      case AssertionContext ignored -> n = nodeForAssertion(pComment, pCFA.getAstCfaRelation());
      case LoopAnnotContext ignored ->
          n = nodeForLoopAnnotation(pComment, pCFA.getAstCfaRelation());
      case FunctionContractContext ignored ->
          n = nodeForFunctionContract(pComment, pCFA, pCFA.getAstCfaRelation(), pAllComments);
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

  private static CFANode nodeForAssertion(AcslComment pComment, AstCfaRelation pAstCfaRelation) {
    FileLocation location = pComment.fileLocation();

    Optional<ASTElement> tightestStatement =
        pAstCfaRelation.getElemForStarting(
            location.getStartingLineNumber(), OptionalInt.of(location.getStartColumnInLine()));

    if (tightestStatement.isPresent()
        && !tightestStatement
            .orElseThrow(
                () ->
                    new AcslNodeMappingException(
                        "No tightest statement found for acsl comment: " + pComment))
            .edges()
            .isEmpty()) {

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

      return nodesForComment.getFirst();
    }
    throw new AcslNodeMappingException("Acsl assertion: " + pComment + " has no CFA node");
  }

  private static CFANode nodeForLoopAnnotation(
      AcslComment pComment, AstCfaRelation pAstCfaRelation) {
    FileLocation location = pComment.fileLocation();

    FileLocation nextStatement =
        pAstCfaRelation.nextStartStatementLocation(location.getNodeOffset());
    Optional<CFANode> iterationNode =
        pAstCfaRelation.getNodeForIterationStatementLocation(
            nextStatement.getStartingLineNumber(), nextStatement.getStartColumnInLine());

    if (iterationNode.isPresent()) {
      Verify.verify(iterationNode.orElseThrow().isLoopStart());
      return iterationNode.orElseThrow(
          () -> new AcslNodeMappingException("Loop annotation: " + pComment + "has no loop head."));
    }
    throw new AcslNodeMappingException("Loop annotation: " + pComment + "has no loop head.");
  }

  /**
   * @param pComment An AcslComment that is possibly a function contract
   * @param pAstCfaRelation The current Ast Cfa Relation
   * @param pAllComments All Acsl Comments of the current Parse Result
   * @return - The next Function Entry Node if pComment is a function contract - Optional.empty()
   *     otherwise.
   */
  private static CFANode nodeForFunctionContract(
      AcslComment pComment,
      CFA pCFA,
      AstCfaRelation pAstCfaRelation,
      List<AcslComment> pAllComments) {

    FileLocation nextLocation =
        pAstCfaRelation.nextStartStatementLocation(pComment.fileLocation().getNodeOffset());

    if (nextLocation.isRealLocation() && noCommentInBetween(pComment, nextLocation, pAllComments)) {
      Optional<CFANode> nextNode =
          pAstCfaRelation.getNodeForStatementLocation(
              nextLocation.getStartingLineNumber(), nextLocation.getStartColumnInLine());

      if (nextNode.isPresent()) {
        String functionName =
            nextNode
                .orElseThrow(
                    () ->
                        new AcslNodeMappingException(
                            "Could not find function entry node for function contract\n"
                                + pComment))
                .getFunctionName();
        return pCFA.getFunctionHead(functionName);
      }
    }

    throw new AcslNodeMappingException(
        "Could not find function entry node for function contract\n"
            + pComment
            + "\nStatement contracts are not supported.");
  }

  public static boolean noCommentInBetween(
      AcslComment pComment, FileLocation nextStatement, List<AcslComment> otherComments) {
    for (AcslComment other : otherComments) {
      if (!other.equals(pComment)
          && other.fileLocation().getNodeOffset()
              > pComment.fileLocation().getNodeOffset() + pComment.fileLocation().getNodeLength()
          && other.fileLocation().getNodeOffset() + other.fileLocation().getNodeLength()
              < nextStatement.getNodeOffset()) {
        // There is an annotation inbetween the comment and the statement
        return false;
      }
    }
    return true;
  }
}
