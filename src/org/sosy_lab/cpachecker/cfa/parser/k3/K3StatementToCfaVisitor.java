// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2025 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.cfa.parser.k3;

import com.google.common.base.Verify;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.ImmutableSetMultimap;
import java.util.Optional;
import org.sosy_lab.common.log.LogManager;
import org.sosy_lab.cpachecker.cfa.CFACreationUtils;
import org.sosy_lab.cpachecker.cfa.ast.FileLocation;
import org.sosy_lab.cpachecker.cfa.ast.k3.K3AssignmentStatement;
import org.sosy_lab.cpachecker.cfa.ast.k3.K3AssumeStatement;
import org.sosy_lab.cpachecker.cfa.ast.k3.K3BreakStatement;
import org.sosy_lab.cpachecker.cfa.ast.k3.K3ContinueStatement;
import org.sosy_lab.cpachecker.cfa.ast.k3.K3GotoStatement;
import org.sosy_lab.cpachecker.cfa.ast.k3.K3HavocStatement;
import org.sosy_lab.cpachecker.cfa.ast.k3.K3IfStatement;
import org.sosy_lab.cpachecker.cfa.ast.k3.K3LabelStatement;
import org.sosy_lab.cpachecker.cfa.ast.k3.K3ProcedureCallStatement;
import org.sosy_lab.cpachecker.cfa.ast.k3.K3ProcedureDeclaration;
import org.sosy_lab.cpachecker.cfa.ast.k3.K3ReturnStatement;
import org.sosy_lab.cpachecker.cfa.ast.k3.K3SequenceStatement;
import org.sosy_lab.cpachecker.cfa.ast.k3.K3Statement;
import org.sosy_lab.cpachecker.cfa.ast.k3.K3StatementVisitor;
import org.sosy_lab.cpachecker.cfa.ast.k3.K3TagProperty;
import org.sosy_lab.cpachecker.cfa.ast.k3.K3TagReference;
import org.sosy_lab.cpachecker.cfa.ast.k3.K3WhileStatement;
import org.sosy_lab.cpachecker.cfa.model.BlankEdge;
import org.sosy_lab.cpachecker.cfa.model.CFAEdge;
import org.sosy_lab.cpachecker.cfa.model.CFANode;
import org.sosy_lab.cpachecker.cfa.model.FunctionExitNode;
import org.sosy_lab.cpachecker.cfa.model.k3.K3AssumeEdge;
import org.sosy_lab.cpachecker.cfa.model.k3.K3StatementEdge;
import org.sosy_lab.cpachecker.exceptions.NoException;

public class K3StatementToCfaVisitor implements K3StatementVisitor<Optional<CFANode>, NoException> {

  // The current starting node for the next statement to be processed.
  // Is only updated when there is a need to chain statements.
  private CFANode currentStartingNode;
  private final K3ProcedureDeclaration procedure;
  private final LogManager logger;
  private final FunctionExitNode functionExitNode;
  private final ImmutableSetMultimap.Builder<CFANode, K3TagProperty> nodeToTagAnnotations;

  // Matches the outermost loop head when processing nested loops.
  private Optional<CFANode> outermostLoopHead = Optional.empty();

  private Optional<CFANode> outermostLoopExitNode = Optional.empty();

  private final ImmutableSetMultimap.Builder<CFANode, K3TagReference> nodeToTagReferences;
  private final ImmutableMap.Builder<CFANode, String> gotoNodesToLabels;
  private final ImmutableMap.Builder<String, CFANode> labelsToNodes;
  private final ImmutableSet.Builder<CFANode> allNodesCollector;
  private final ImmutableSetMultimap<String, K3TagProperty> tagReferencesToAnnotations;

  public K3StatementToCfaVisitor(
      CFANode pInitialNode,
      K3ProcedureDeclaration pProcedure,
      LogManager pLogger,
      FunctionExitNode pFunctionExitNode,
      ImmutableSetMultimap.Builder<CFANode, K3TagProperty> pNodeToTagAnnotations,
      ImmutableSetMultimap.Builder<CFANode, K3TagReference> pNodeToTagReferences,
      ImmutableMap.Builder<CFANode, String> pGotoNodesToLabels,
      ImmutableMap.Builder<String, CFANode> pLabelsToNodes,
      ImmutableSet.Builder<CFANode> pAllNodesCollector,
      ImmutableSetMultimap<String, K3TagProperty> pTagReferencesToAnnotations) {
    currentStartingNode = pInitialNode;
    procedure = pProcedure;
    logger = pLogger;
    functionExitNode = pFunctionExitNode;
    nodeToTagAnnotations = pNodeToTagAnnotations;
    nodeToTagReferences = pNodeToTagReferences;
    gotoNodesToLabels = pGotoNodesToLabels;
    labelsToNodes = pLabelsToNodes;
    allNodesCollector = pAllNodesCollector;
    tagReferencesToAnnotations = pTagReferencesToAnnotations;
  }

  private CFANode getNewNode() {
    CFANode newNode = new CFANode(procedure);
    allNodesCollector.add(newNode);
    return newNode;
  }

  private void trackTagPropertiesForStatementStartingWithNode(
      K3Statement pStatement, CFANode pStartNode) {
    nodeToTagAnnotations.putAll(pStartNode, pStatement.getTagAttributes());
    for (K3TagReference ref : pStatement.getTagReferences()) {
      ImmutableSet<K3TagProperty> properties = tagReferencesToAnnotations.get(ref.getTagName());
      nodeToTagAnnotations.putAll(pStartNode, properties);
      nodeToTagReferences.put(pStartNode, ref);
    }
  }

  @Override
  public Optional<CFANode> visit(K3AssignmentStatement pK3AssignmentStatement) throws NoException {
    trackTagPropertiesForStatementStartingWithNode(pK3AssignmentStatement, currentStartingNode);
    // Handle assignment statements.
    CFANode newNode = getNewNode();
    CFAEdge edge =
        new K3StatementEdge(
            pK3AssignmentStatement.toASTString(),
            pK3AssignmentStatement,
            pK3AssignmentStatement.getFileLocation(),
            currentStartingNode,
            newNode);
    CFACreationUtils.addEdgeToCFA(edge, logger);
    return Optional.of(newNode);
  }

  @Override
  public Optional<CFANode> visit(K3ProcedureCallStatement pK3ProcedureCallStatement)
      throws NoException {
    trackTagPropertiesForStatementStartingWithNode(pK3ProcedureCallStatement, currentStartingNode);
    CFANode newNode = getNewNode();
    CFAEdge edge =
        new K3StatementEdge(
            pK3ProcedureCallStatement.toASTString(),
            pK3ProcedureCallStatement,
            pK3ProcedureCallStatement.getFileLocation(),
            currentStartingNode,
            newNode);
    CFACreationUtils.addEdgeToCFA(edge, logger);
    return Optional.of(newNode);
  }

  @Override
  public Optional<CFANode> visit(K3HavocStatement pK3HavocStatement) throws NoException {
    trackTagPropertiesForStatementStartingWithNode(pK3HavocStatement, currentStartingNode);

    CFANode newNode = getNewNode();
    CFAEdge egde =
        new K3StatementEdge(
            pK3HavocStatement.toASTString(),
            pK3HavocStatement,
            pK3HavocStatement.getFileLocation(),
            currentStartingNode,
            newNode);
    CFACreationUtils.addEdgeToCFA(egde, logger);
    return Optional.of(newNode);
  }

  @Override
  public Optional<CFANode> visit(K3SequenceStatement pK3SequenceStatement) throws NoException {
    trackTagPropertiesForStatementStartingWithNode(pK3SequenceStatement, currentStartingNode);

    // Handle sequence statements.
    for (K3Statement subStatement : pK3SequenceStatement.getStatements()) {
      Optional<CFANode> lastNodeWithAllNodes = subStatement.accept(this);
      if (lastNodeWithAllNodes.isEmpty()) {
        // We already connected back to the CFA in the sub-statement.
        // So there is nothing more to do here.
        return Optional.empty();
      }

      currentStartingNode = lastNodeWithAllNodes.orElseThrow();
    }
    return Optional.of(currentStartingNode);
  }

  @Override
  public Optional<CFANode> visit(K3AssumeStatement pK3AssumeStatement) throws NoException {
    trackTagPropertiesForStatementStartingWithNode(pK3AssumeStatement, currentStartingNode);

    // We do not need to split the assumption into multiple edges, since there is no
    // short-circuiting
    CFANode newNode = getNewNode();
    CFAEdge edge =
        new K3AssumeEdge(
            pK3AssumeStatement.toASTString(),
            pK3AssumeStatement.getFileLocation(),
            currentStartingNode,
            newNode,
            pK3AssumeStatement.getTerm(),
            true,
            false,
            false);
    CFACreationUtils.addEdgeToCFA(edge, logger);
    return Optional.of(newNode);
  }

  @Override
  public Optional<CFANode> visit(K3WhileStatement pK3WhileStatement) throws NoException {
    // Create the loop head and exit nodes
    CFANode loopHeadNode = getNewNode();
    loopHeadNode.setLoopStart();
    CFANode exitNode = getNewNode();

    // For while loops we need to track tag properties for the starting loop node
    trackTagPropertiesForStatementStartingWithNode(pK3WhileStatement, loopHeadNode);

    // Update the outermost loop head, and exit node
    Optional<CFANode> previousOutermostLoopHead = outermostLoopHead;
    Optional<CFANode> previousOutermostLoopExitNode = outermostLoopExitNode;

    outermostLoopHead = Optional.of(loopHeadNode);
    outermostLoopExitNode = Optional.of(exitNode);

    // Connect the predecessor to the loop head
    CFACreationUtils.addEdgeToCFA(
        new BlankEdge(
            "",
            pK3WhileStatement.getFileLocation(),
            currentStartingNode,
            loopHeadNode,
            "entering loop"),
        logger);

    // Create the edges for the condition
    CFANode trueConditionNode = getNewNode();

    CFAEdge trueEdge =
        new K3AssumeEdge(
            pK3WhileStatement.getCondition().toASTString(),
            pK3WhileStatement.getFileLocation(),
            loopHeadNode,
            trueConditionNode,
            pK3WhileStatement.getCondition(),
            true,
            false,
            false);
    CFAEdge falseEdge =
        new K3AssumeEdge(
            "!(" + pK3WhileStatement.getCondition().toASTString() + ")",
            pK3WhileStatement.getFileLocation(),
            loopHeadNode,
            exitNode,
            pK3WhileStatement.getCondition(),
            false,
            false,
            false);
    CFACreationUtils.addEdgeToCFA(trueEdge, logger);
    CFACreationUtils.addEdgeToCFA(falseEdge, logger);

    // Handle the body of the while loop
    currentStartingNode = trueConditionNode;
    Optional<CFANode> bodyNode = pK3WhileStatement.getBody().accept(this);

    if (bodyNode.isPresent()) {

      // Connect the end of the body back to the loop head
      CFACreationUtils.addEdgeToCFA(
          new BlankEdge(
              "", FileLocation.DUMMY, bodyNode.orElseThrow(), loopHeadNode, "Return to loop head"),
          logger);
    }

    // Reset the outermost loop head
    outermostLoopHead = previousOutermostLoopHead;
    outermostLoopExitNode = previousOutermostLoopExitNode;

    // Set the current starting node to the exit node
    // Should not be necessary, but better safe than sorry.
    currentStartingNode = exitNode;
    return Optional.of(exitNode);
  }

  @Override
  public Optional<CFANode> visit(K3IfStatement pK3IfStatement) throws NoException {
    trackTagPropertiesForStatementStartingWithNode(pK3IfStatement, currentStartingNode);

    CFANode exitNode = getNewNode();

    // Handle conditions
    CFANode trueConditionNode = getNewNode();
    CFANode falseConditionNode = getNewNode();

    CFAEdge trueEdge =
        new K3AssumeEdge(
            pK3IfStatement.getCondition().toASTString(),
            pK3IfStatement.getFileLocation(),
            currentStartingNode,
            trueConditionNode,
            pK3IfStatement.getCondition(),
            true,
            false,
            false);
    CFAEdge falseEdge =
        new K3AssumeEdge(
            "!(" + pK3IfStatement.getCondition().toASTString() + ")",
            pK3IfStatement.getFileLocation(),
            currentStartingNode,
            falseConditionNode,
            pK3IfStatement.getCondition(),
            false,
            false,
            false);
    CFACreationUtils.addEdgeToCFA(trueEdge, logger);
    CFACreationUtils.addEdgeToCFA(falseEdge, logger);

    // Handle the then branch
    currentStartingNode = trueConditionNode;
    Optional<CFANode> thenBranchNode = pK3IfStatement.getThenBranch().accept(this);

    if (thenBranchNode.isPresent()) {
      // The node is not connected back to the CFA yet, so we need to add it.
      currentStartingNode = falseConditionNode;
      CFACreationUtils.addEdgeToCFA(
          new BlankEdge(
              "",
              FileLocation.DUMMY,
              thenBranchNode.orElseThrow(),
              exitNode,
              "End of if-then(-else)"),
          logger);
    }

    // Handle the else branch if it exists
    if (pK3IfStatement.getElseBranch().isPresent()) {
      currentStartingNode = falseConditionNode;
      Optional<CFANode> elseBranchNode = pK3IfStatement.getElseBranch().orElseThrow().accept(this);
      if (elseBranchNode.isPresent()) {
        // The node is not connected back to the CFA yet, so we need to add it.
        CFACreationUtils.addEdgeToCFA(
            new BlankEdge(
                "",
                FileLocation.DUMMY,
                elseBranchNode.orElseThrow(),
                exitNode,
                "End of if-then(-else)"),
            logger);
      }
    } else {
      // No else branch, so we need to connect the false condition node to the exit node.
      CFACreationUtils.addEdgeToCFA(
          new BlankEdge(
              "",
              pK3IfStatement.getFileLocation(),
              falseConditionNode,
              exitNode,
              "Skipping if-then"),
          logger);
    }

    // To be save, we set the current starting node to the exit node.
    // Should not be necessary, but better safe than sorry.
    currentStartingNode = exitNode;
    return Optional.of(exitNode);
  }

  @Override
  public Optional<CFANode> visit(K3BreakStatement pK3BreakStatement) throws NoException {
    trackTagPropertiesForStatementStartingWithNode(pK3BreakStatement, currentStartingNode);

    Verify.verify(outermostLoopExitNode.isPresent());
    CFAEdge edge =
        new BlankEdge(
            "break",
            pK3BreakStatement.getFileLocation(),
            currentStartingNode,
            outermostLoopExitNode.orElseThrow(),
            "break");
    CFACreationUtils.addEdgeToCFA(edge, logger);

    // In case someone
    return Optional.empty();
  }

  @Override
  public Optional<CFANode> visit(K3ContinueStatement pK3ContinueStatement) throws NoException {
    Verify.verify(outermostLoopHead.isPresent());

    trackTagPropertiesForStatementStartingWithNode(pK3ContinueStatement, currentStartingNode);

    CFAEdge edge =
        new BlankEdge(
            "break",
            pK3ContinueStatement.getFileLocation(),
            currentStartingNode,
            outermostLoopHead.orElseThrow(),
            "break");
    CFACreationUtils.addEdgeToCFA(edge, logger);

    // In case someone
    return Optional.empty();
  }

  @Override
  public Optional<CFANode> visit(K3ReturnStatement pK3ReturnStatement) throws NoException {
    trackTagPropertiesForStatementStartingWithNode(pK3ReturnStatement, currentStartingNode);

    CFACreationUtils.addEdgeToCFA(
        new BlankEdge(
            "return",
            pK3ReturnStatement.getFileLocation(),
            currentStartingNode,
            functionExitNode,
            "return"),
        logger);
    return Optional.empty();
  }

  @Override
  public Optional<CFANode> visit(K3GotoStatement pK3GotoStatement) throws NoException {
    trackTagPropertiesForStatementStartingWithNode(pK3GotoStatement, currentStartingNode);

    // We may not have all the labels yet, so we just store the information for later.
    String label = pK3GotoStatement.getLabel();
    gotoNodesToLabels.put(currentStartingNode, label);
    return Optional.empty();
  }

  @Override
  public Optional<CFANode> visit(K3LabelStatement pK3LabelStatement) throws NoException {
    trackTagPropertiesForStatementStartingWithNode(pK3LabelStatement, currentStartingNode);

    labelsToNodes.put(pK3LabelStatement.getLabel(), currentStartingNode);
    return Optional.of(currentStartingNode);
  }
}
