// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2026 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.cfa.ast.acsl.parser;

import com.google.common.base.Verify;
import com.google.common.collect.FluentIterable;
import java.util.List;
import java.util.Optional;
import java.util.OptionalInt;
import org.sosy_lab.cpachecker.cfa.CFA;
import org.sosy_lab.cpachecker.cfa.ast.FileLocation;
import org.sosy_lab.cpachecker.cfa.ast.acsl.parser.AcslComment.AcslCommentType;
import org.sosy_lab.cpachecker.cfa.ast.acsl.parser.AcslParser.AcslParseException;
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
  public static Optional<CFANode> addAcslToNodeMapping(AcslComment pComment, CFA pCFA)
      throws AcslParseException, AcslMetadataException.AcslNodeMappingException {

    AcslCommentType commentType = AcslParser.acslCommentToCommentType(pComment.getComment());
    CFANode n;
    switch (commentType) {
      case ASSERTION -> n = nodeForAssertion(pComment, pCFA.getAstCfaRelation());
      case LOOP_ANNOTATION -> n = nodeForLoopAnnotation(pComment, pCFA.getAstCfaRelation());
      case FUNCTION_CONTRACT ->
          n = nodeForFunctionContract(pComment, pCFA, pCFA.getAstCfaRelation());
      case null ->
          throw new AcslMetadataException.AcslNodeMappingException(
              "Annotation " + pComment + " has no Antlr context.");
      default ->
          throw new AcslMetadataException.AcslNodeMappingException(
              "Unexpected annotation: "
                  + pComment
                  + ". Parsing is currently supported for assertions, loop annotations,"
                  + " function contracts.");
    }
    return Optional.of(n);
  }

  /**
   * Finds the next CFA node for an acsl assertion
   *
   * @param pComment The comment containing the assertion.
   * @param pAstCfaRelation The current AstCfaRelation
   * @return The CFA node for the tightest c statement for the assertion. Throws an exception, if no
   *     matching node can be found.
   */
  private static CFANode nodeForAssertion(AcslComment pComment, AstCfaRelation pAstCfaRelation) {
    FileLocation location = pComment.fileLocation();

    Optional<ASTElement> tightestStatement =
        pAstCfaRelation.getElemForStarting(
            location.getStartingLineNumber(), OptionalInt.of(location.getStartColumnInLine()));

    if (tightestStatement.isPresent()
        && !tightestStatement
            .orElseThrow(
                () ->
                    new AcslMetadataException.AcslNodeMappingException(
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
    throw new AcslMetadataException.AcslNodeMappingException(
        "Acsl assertion: " + pComment + " has no CFA node");
  }

  /**
   * Finds the loop head for an acsl loop annotation
   *
   * @param pComment The comment containing the annotation
   * @param pAstCfaRelation The current AstCfaRelation
   * @return The next CFA node with the 'isLoopHead' flag set to true. Throws an exception if no
   *     matching node can be found.
   */
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
          () ->
              new AcslMetadataException.AcslNodeMappingException(
                  "Loop annotation: " + pComment + "has no loop head."));
    }
    throw new AcslMetadataException.AcslNodeMappingException(
        "Loop annotation: " + pComment + "has no loop head.");
  }

  /**
   * Finds the next function entry node for a function contract.
   *
   * @param pComment An AcslComment that is possibly a function contract
   * @param pAstCfaRelation The current Ast Cfa Relation
   * @return - The next Function Entry Node if pComment is a function contract - Throws an
   *     exception, otherwise.
   */
  private static CFANode nodeForFunctionContract(
      AcslComment pComment, CFA pCFA, AstCfaRelation pAstCfaRelation) {

    FileLocation nextLocation =
        pAstCfaRelation.nextStartStatementLocation(pComment.fileLocation().getNodeOffset());

    Optional<CFANode> nextNode =
        pAstCfaRelation.getNodeForStatementLocation(
            nextLocation.getStartingLineNumber(), nextLocation.getStartColumnInLine());

    if (nextNode.isPresent()) {
      String functionName =
          nextNode
              .orElseThrow(
                  () ->
                      new AcslMetadataException.AcslNodeMappingException(
                          "Could not find function entry node for function contract\n" + pComment))
              .getFunctionName();
      return pCFA.getFunctionHead(functionName);
    }

    throw new AcslMetadataException.AcslNodeMappingException(
        "Could not find function entry node for function contract\n"
            + pComment
            + "\nStatement contracts are not supported.");
  }
}
