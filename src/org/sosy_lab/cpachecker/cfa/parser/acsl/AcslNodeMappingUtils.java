// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2026 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.cfa.parser.acsl;

import com.google.common.base.Verify;
import com.google.common.collect.FluentIterable;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableSet;
import java.util.List;
import java.util.Optional;
import java.util.OptionalInt;
import org.sosy_lab.cpachecker.cfa.CFA;
import org.sosy_lab.cpachecker.cfa.ast.FileLocation;
import org.sosy_lab.cpachecker.cfa.model.CFAEdge;
import org.sosy_lab.cpachecker.cfa.model.CFANode;
import org.sosy_lab.cpachecker.cfa.model.FunctionEntryNode;
import org.sosy_lab.cpachecker.cfa.model.FunctionExitNode;
import org.sosy_lab.cpachecker.cfa.parser.acsl.AcslComment.AcslCommentType;
import org.sosy_lab.cpachecker.cfa.parser.acsl.antlr.AcslParser;
import org.sosy_lab.cpachecker.cfa.parser.acsl.antlr.AcslParser.AcslParseException;
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
    CFANode n =
        switch (commentType) {
          case ASSERTION -> nodeForAssertion(pComment, pCFA.getAstCfaRelation());
          case LOOP_ANNOTATION -> nodeForLoopAnnotation(pComment, pCFA, pCFA.getAstCfaRelation());
          case FUNCTION_CONTRACT ->
              isInsideFunction(pComment, pCFA)
                  ? nodesForStatementContract(pComment, pCFA).startNodes().iterator().next()
                  : nodeForFunctionContract(pComment, pCFA, pCFA.getAstCfaRelation());
          case STATEMENT_CONTRACT ->
              nodesForStatementContract(pComment, pCFA).startNodes().iterator().next();
          case null ->
              throw new AcslMetadataException.AcslNodeMappingException(
                  "Annotation " + pComment + " has no Antlr context.");
          default ->
              throw new AcslMetadataException.AcslNodeMappingException(
                  "Unexpected annotation: "
                      + pComment
                      + ". Parsing is currently supported for assertions, loop annotations,"
                      + " function contracts, and statement contracts.");
        };
    return Optional.of(n);
  }

  /** The nodes at which the execution of a statement starts and ends. */
  public record StatementNodes(
      FileLocation statementLocation,
      ImmutableSet<CFAEdge> edges,
      ImmutableSet<CFANode> startNodes,
      ImmutableSet<CFANode> endNodes) {}

  /** Whether the comment is inside the definition of a function, e.g., a statement contract. */
  public static boolean isInsideFunction(AcslComment pComment, CFA pCFA) {
    int offset = pComment.fileLocation().getNodeOffset();
    return FluentIterable.from(pCFA.entryNodes())
        .transform(FunctionEntryNode::getFileLocation)
        .anyMatch(
            location ->
                location.isRealLocation()
                    && location.getFileName().equals(pComment.fileLocation().getFileName())
                    && location.getNodeOffset() <= offset
                    && offset < location.getNodeOffset() + location.getNodeLength());
  }

  /**
   * Finds the nodes of the statement after a statement contract.
   *
   * @return the nodes at which the statement starts, where its pre-state is, and the nodes after
   *     the statement, where its post-state is
   */
  public static StatementNodes nodesForStatementContract(AcslComment pComment, CFA pCFA) {
    AstCfaRelation astCfaRelation = pCFA.getAstCfaRelation();
    FileLocation next =
        astCfaRelation.nextStartStatementLocation(pComment.fileLocation().getNodeOffset());
    ASTElement statement =
        astCfaRelation
            .getTightestStatementForStarting(
                next.getStartingLineNumber(), OptionalInt.of(next.getStartColumnInLine()))
            .orElseThrow(
                () ->
                    new AcslMetadataException.AcslNodeMappingException(
                        "Statement contract " + pComment + " has no statement."));
    ImmutableSet<CFAEdge> edges = statement.edges();
    // Blank edges inside the statement may have no location in it, so its nodes decide whether an
    // edge enters or leaves the statement
    ImmutableSet<CFANode> nodes =
        FluentIterable.from(edges)
            .transformAndConcat(
                edge -> ImmutableList.of(edge.getPredecessor(), edge.getSuccessor()))
            .toSet();
    ImmutableSet<CFANode> startNodes =
        FluentIterable.from(edges)
            .transform(CFAEdge::getPredecessor)
            .filter(
                node ->
                    node.getEnteringEdges()
                        .anyMatch(edge -> !nodes.contains(edge.getPredecessor())))
            .toSet();
    ImmutableSet<CFANode> endNodes =
        FluentIterable.from(edges)
            .transform(CFAEdge::getSuccessor)
            .filter(
                node ->
                    node.getNumLeavingEdges() == 0
                        || node.getLeavingEdges()
                            .anyMatch(edge -> !nodes.contains(edge.getSuccessor())))
            .toSet();
    if (startNodes.isEmpty() || endNodes.isEmpty()) {
      throw new AcslMetadataException.AcslNodeMappingException(
          "Statement contract " + pComment + " has no CFA nodes.");
    }
    return new StatementNodes(statement.location(), edges, startNodes, endNodes);
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
   * @param pCFA The CFA, whose loop structure is used to determine the loop head
   * @param pAstCfaRelation The current AstCfaRelation
   * @return The next CFA node with the 'isLoopHead' flag set to true. Throws an exception if no
   *     matching node can be found.
   */
  private static CFANode nodeForLoopAnnotation(
      AcslComment pComment, CFA pCFA, AstCfaRelation pAstCfaRelation) {
    FileLocation location = pComment.fileLocation();

    FileLocation nextStatement =
        pAstCfaRelation.nextStartStatementLocation(location.getNodeOffset());

    // The invariant of a do-while loop holds at the beginning of each iteration, i.e., before the
    // body, but for witnesses the loop head is before the controlling expression
    Optional<CFAEdge> doEdge =
        FluentIterable.from(pCFA.edges())
            .filter(
                edge ->
                    edge.getDescription().equals("do")
                        && edge.getFileLocation().getNodeOffset() == nextStatement.getNodeOffset()
                        && edge.getFileLocation().getFileName().equals(location.getFileName()))
            .first()
            .toJavaUtil();
    if (doEdge.isPresent()) {
      return doEdge.orElseThrow().getSuccessor();
    }

    Optional<CFANode> iterationNode =
        pCFA.getLoopStructure()
            .flatMap(
                loopStructure ->
                    pAstCfaRelation.getNodeForIterationStatementLocation(
                        nextStatement.getStartingLineNumber(),
                        nextStatement.getStartColumnInLine(),
                        loopStructure));

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
