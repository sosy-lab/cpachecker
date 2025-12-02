// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2025 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.cfa.parser.svlib;

import com.google.common.base.Verify;
import com.google.common.collect.FluentIterable;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.ImmutableSetMultimap;
import java.util.Map.Entry;
import java.util.Optional;
import org.sosy_lab.common.log.LogManager;
import org.sosy_lab.cpachecker.cfa.CFACreationUtils;
import org.sosy_lab.cpachecker.cfa.ast.FileLocation;
import org.sosy_lab.cpachecker.cfa.ast.svlib.SvLibFunctionCallAssignmentStatement;
import org.sosy_lab.cpachecker.cfa.ast.svlib.SvLibFunctionCallExpression;
import org.sosy_lab.cpachecker.cfa.ast.svlib.SvLibFunctionDeclaration;
import org.sosy_lab.cpachecker.cfa.ast.svlib.SvLibIdTerm;
import org.sosy_lab.cpachecker.cfa.ast.svlib.SvLibIdTermTuple;
import org.sosy_lab.cpachecker.cfa.ast.svlib.SvLibSimpleDeclaration;
import org.sosy_lab.cpachecker.cfa.ast.svlib.SvLibTerm;
import org.sosy_lab.cpachecker.cfa.ast.svlib.SvLibTermAssignmentCfaStatement;
import org.sosy_lab.cpachecker.cfa.ast.svlib.specification.SvLibTagProperty;
import org.sosy_lab.cpachecker.cfa.ast.svlib.specification.SvLibTagReference;
import org.sosy_lab.cpachecker.cfa.model.BlankEdge;
import org.sosy_lab.cpachecker.cfa.model.CFAEdge;
import org.sosy_lab.cpachecker.cfa.model.CFANode;
import org.sosy_lab.cpachecker.cfa.model.FunctionExitNode;
import org.sosy_lab.cpachecker.cfa.model.svlib.SvLibAssumeEdge;
import org.sosy_lab.cpachecker.cfa.model.svlib.SvLibBlankChoiceEdge;
import org.sosy_lab.cpachecker.cfa.model.svlib.SvLibStatementEdge;
import org.sosy_lab.cpachecker.cfa.parser.svlib.ast.SvLibProcedureDeclaration;
import org.sosy_lab.cpachecker.cfa.parser.svlib.ast.SvLibSimpleParsingDeclaration;
import org.sosy_lab.cpachecker.cfa.parser.svlib.ast.statements.SvLibAssignmentStatement;
import org.sosy_lab.cpachecker.cfa.parser.svlib.ast.statements.SvLibAssumeStatement;
import org.sosy_lab.cpachecker.cfa.parser.svlib.ast.statements.SvLibBreakStatement;
import org.sosy_lab.cpachecker.cfa.parser.svlib.ast.statements.SvLibChoiceStatement;
import org.sosy_lab.cpachecker.cfa.parser.svlib.ast.statements.SvLibContinueStatement;
import org.sosy_lab.cpachecker.cfa.parser.svlib.ast.statements.SvLibGotoStatement;
import org.sosy_lab.cpachecker.cfa.parser.svlib.ast.statements.SvLibHavocStatement;
import org.sosy_lab.cpachecker.cfa.parser.svlib.ast.statements.SvLibIfStatement;
import org.sosy_lab.cpachecker.cfa.parser.svlib.ast.statements.SvLibLabelStatement;
import org.sosy_lab.cpachecker.cfa.parser.svlib.ast.statements.SvLibProcedureCallStatement;
import org.sosy_lab.cpachecker.cfa.parser.svlib.ast.statements.SvLibReturnStatement;
import org.sosy_lab.cpachecker.cfa.parser.svlib.ast.statements.SvLibSequenceStatement;
import org.sosy_lab.cpachecker.cfa.parser.svlib.ast.statements.SvLibStatement;
import org.sosy_lab.cpachecker.cfa.parser.svlib.ast.statements.SvLibStatementVisitor;
import org.sosy_lab.cpachecker.cfa.parser.svlib.ast.statements.SvLibWhileStatement;
import org.sosy_lab.cpachecker.exceptions.NoException;

public class SvLibStatementToCfaVisitor
    implements SvLibStatementVisitor<Optional<CFANode>, NoException> {

  // The current starting node for the next statement to be processed.
  // Is only updated when there is a need to chain statements.
  private CFANode currentStartingNode;
  private final SvLibProcedureDeclaration procedure;
  private final LogManager logger;
  private final FunctionExitNode functionExitNode;
  private final ImmutableSetMultimap.Builder<CFANode, SvLibTagProperty> nodeToTagAnnotations;

  // Matches the outermost loop head when processing nested loops.
  private Optional<CFANode> outermostLoopHead = Optional.empty();

  private Optional<CFANode> outermostLoopExitNode = Optional.empty();

  private final ImmutableSetMultimap.Builder<CFANode, SvLibTagReference> nodeToTagReferences;
  private final ImmutableMap.Builder<CFANode, String> gotoNodesToLabels;
  private final ImmutableMap.Builder<String, CFANode> labelsToNodes;
  private final ImmutableSet.Builder<CFANode> allNodesCollector;
  private final ImmutableSetMultimap<String, SvLibTagProperty> tagReferencesToAnnotations;
  // Required to reconstruct violation witnesses properly
  private final ImmutableMap.Builder<CFANode, SvLibHavocStatement> nodesToActualHavocStatementEnd;

  public SvLibStatementToCfaVisitor(
      CFANode pInitialNode,
      SvLibProcedureDeclaration pProcedure,
      LogManager pLogger,
      FunctionExitNode pFunctionExitNode,
      ImmutableSetMultimap.Builder<CFANode, SvLibTagProperty> pNodeToTagAnnotations,
      ImmutableSetMultimap.Builder<CFANode, SvLibTagReference> pNodeToTagReferences,
      ImmutableMap.Builder<CFANode, String> pGotoNodesToLabels,
      ImmutableMap.Builder<String, CFANode> pLabelsToNodes,
      ImmutableSet.Builder<CFANode> pAllNodesCollector,
      ImmutableSetMultimap<String, SvLibTagProperty> pTagReferencesToAnnotations,
      ImmutableMap.Builder<CFANode, SvLibHavocStatement> pNodesToActualHavocStatementEnd) {
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
    nodesToActualHavocStatementEnd = pNodesToActualHavocStatementEnd;
  }

  private CFANode getNewNode() {
    CFANode newNode = new CFANode(procedure.toSimpleDeclaration());
    allNodesCollector.add(newNode);
    return newNode;
  }

  private void trackTagPropertiesForStatementStartingWithNode(
      SvLibStatement pStatement, CFANode pStartNode) {
    nodeToTagAnnotations.putAll(pStartNode, pStatement.getTagAttributes());
    for (SvLibTagReference ref : pStatement.getTagReferences()) {
      ImmutableSet<SvLibTagProperty> properties = tagReferencesToAnnotations.get(ref.getTagName());
      nodeToTagAnnotations.putAll(pStartNode, properties);
      nodeToTagReferences.put(pStartNode, ref);
    }
  }

  @Override
  public Optional<CFANode> visit(SvLibAssignmentStatement pSvLibAssignmentStatement)
      throws NoException {
    trackTagPropertiesForStatementStartingWithNode(pSvLibAssignmentStatement, currentStartingNode);
    // Handle assignment statements, by splitting them into multiple edges if necessary.

    CFANode currentNode = currentStartingNode;
    for (Entry<SvLibSimpleParsingDeclaration, SvLibTerm> assignment :
        pSvLibAssignmentStatement.getAssignments().entrySet()) {
      CFANode newNode = getNewNode();
      SvLibSimpleDeclaration variable = assignment.getKey().toSimpleDeclaration();
      SvLibTerm value = assignment.getValue();
      SvLibTermAssignmentCfaStatement singleAssignmentStatement =
          new SvLibTermAssignmentCfaStatement(
              new SvLibIdTerm(variable, FileLocation.DUMMY),
              value,
              pSvLibAssignmentStatement.getFileLocation());
      CFAEdge edge =
          new SvLibStatementEdge(
              singleAssignmentStatement.toASTString(),
              singleAssignmentStatement,
              singleAssignmentStatement.getFileLocation(),
              currentNode,
              newNode);
      CFACreationUtils.addEdgeToCFA(edge, logger);
      currentNode = newNode;
    }

    return Optional.of(currentNode);
  }

  @Override
  public Optional<CFANode> visit(SvLibProcedureCallStatement pSvLibProcedureCallStatement)
      throws NoException {
    trackTagPropertiesForStatementStartingWithNode(
        pSvLibProcedureCallStatement, currentStartingNode);
    // Rewrite this to calling the function and assigning return values.
    CFANode newNode = getNewNode();

    SvLibProcedureDeclaration procedureDeclaration =
        pSvLibProcedureCallStatement.getProcedureDeclaration();
    SvLibFunctionDeclaration functionDeclaration = procedureDeclaration.toSimpleDeclaration();
    SvLibFunctionCallAssignmentStatement functionCallAssignmentStatement =
        new SvLibFunctionCallAssignmentStatement(
            FileLocation.DUMMY,
            new SvLibIdTermTuple(
                FileLocation.DUMMY,
                FluentIterable.from(pSvLibProcedureCallStatement.getReturnVariables())
                    .transform(SvLibSimpleParsingDeclaration::toSimpleDeclaration)
                    .transform(decl -> new SvLibIdTerm(decl, FileLocation.DUMMY))
                    .toList()),
            new SvLibFunctionCallExpression(
                FileLocation.DUMMY,
                functionDeclaration.getType(),
                new SvLibIdTerm(functionDeclaration, FileLocation.DUMMY),
                pSvLibProcedureCallStatement.getArguments(),
                functionDeclaration));

    CFAEdge edge =
        new SvLibStatementEdge(
            pSvLibProcedureCallStatement.toASTString(),
            functionCallAssignmentStatement,
            pSvLibProcedureCallStatement.getFileLocation(),
            currentStartingNode,
            newNode);
    CFACreationUtils.addEdgeToCFA(edge, logger);
    return Optional.of(newNode);
  }

  @Override
  public Optional<CFANode> visit(SvLibHavocStatement pSvLibHavocStatement) throws NoException {
    trackTagPropertiesForStatementStartingWithNode(pSvLibHavocStatement, currentStartingNode);

    CFANode newNode = getNewNode();
    CFANode currentNode = currentStartingNode;
    // Rewrite this into a function call assignment with a special "havoc" function.
    for (SvLibSimpleParsingDeclaration variable : pSvLibHavocStatement.getVariables()) {
      SvLibFunctionDeclaration havocFunctionDeclaration =
          SvLibFunctionDeclaration.nondetFunctionWithReturnType(variable.getType());

      SvLibFunctionCallAssignmentStatement havocCallAssignmentStatement =
          new SvLibFunctionCallAssignmentStatement(
              FileLocation.DUMMY,
              new SvLibIdTermTuple(
                  FileLocation.DUMMY,
                  ImmutableList.of(
                      new SvLibIdTerm(variable.toSimpleDeclaration(), FileLocation.DUMMY))),
              new SvLibFunctionCallExpression(
                  FileLocation.DUMMY,
                  havocFunctionDeclaration.getType(),
                  new SvLibIdTerm(havocFunctionDeclaration, FileLocation.DUMMY),
                  ImmutableList.of(),
                  havocFunctionDeclaration));

      CFAEdge edge =
          new SvLibStatementEdge(
              havocCallAssignmentStatement.toASTString(),
              havocCallAssignmentStatement,
              havocCallAssignmentStatement.getFileLocation(),
              currentNode,
              newNode);
      CFACreationUtils.addEdgeToCFA(edge, logger);
      currentNode = newNode;
    }

    nodesToActualHavocStatementEnd.put(currentNode, pSvLibHavocStatement);

    return Optional.of(currentNode);
  }

  @Override
  public Optional<CFANode> visit(SvLibSequenceStatement pSvLibSequenceStatement)
      throws NoException {
    trackTagPropertiesForStatementStartingWithNode(pSvLibSequenceStatement, currentStartingNode);

    // Handle sequence statements.
    for (SvLibStatement subStatement : pSvLibSequenceStatement.getStatements()) {
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
  public Optional<CFANode> visit(SvLibAssumeStatement pSvLibAssumeStatement) throws NoException {
    trackTagPropertiesForStatementStartingWithNode(pSvLibAssumeStatement, currentStartingNode);

    // We do not need to split the assumption into multiple edges, since there is no
    // short-circuiting
    CFANode newNode = getNewNode();
    CFAEdge edge =
        new SvLibAssumeEdge(
            pSvLibAssumeStatement.toASTString(),
            pSvLibAssumeStatement.getFileLocation(),
            currentStartingNode,
            newNode,
            pSvLibAssumeStatement.getTerm(),
            true,
            false,
            false);
    CFACreationUtils.addEdgeToCFA(edge, logger);
    return Optional.of(newNode);
  }

  @Override
  public Optional<CFANode> visit(SvLibWhileStatement pSvLibWhileStatement) throws NoException {
    // Create the loop head and exit nodes
    CFANode loopHeadNode = getNewNode();
    loopHeadNode.setLoopStart();
    CFANode exitNode = getNewNode();

    // For while loops we need to track tag properties for the starting loop node
    trackTagPropertiesForStatementStartingWithNode(pSvLibWhileStatement, loopHeadNode);

    // Update the outermost loop head, and exit node
    Optional<CFANode> previousOutermostLoopHead = outermostLoopHead;
    Optional<CFANode> previousOutermostLoopExitNode = outermostLoopExitNode;

    outermostLoopHead = Optional.of(loopHeadNode);
    outermostLoopExitNode = Optional.of(exitNode);

    // Connect the predecessor to the loop head
    CFACreationUtils.addEdgeToCFA(
        new BlankEdge(
            "",
            pSvLibWhileStatement.getFileLocation(),
            currentStartingNode,
            loopHeadNode,
            "entering loop"),
        logger);

    // Create the edges for the condition
    CFANode trueConditionNode = getNewNode();

    CFAEdge trueEdge =
        new SvLibAssumeEdge(
            pSvLibWhileStatement.getCondition().toASTString(),
            pSvLibWhileStatement.getFileLocation(),
            loopHeadNode,
            trueConditionNode,
            pSvLibWhileStatement.getCondition(),
            true,
            false,
            false);
    CFAEdge falseEdge =
        new SvLibAssumeEdge(
            "!(" + pSvLibWhileStatement.getCondition().toASTString() + ")",
            pSvLibWhileStatement.getFileLocation(),
            loopHeadNode,
            exitNode,
            pSvLibWhileStatement.getCondition(),
            false,
            false,
            false);
    CFACreationUtils.addEdgeToCFA(trueEdge, logger);
    CFACreationUtils.addEdgeToCFA(falseEdge, logger);

    // Handle the body of the while loop
    currentStartingNode = trueConditionNode;
    Optional<CFANode> bodyNode = pSvLibWhileStatement.getBody().accept(this);

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
  public Optional<CFANode> visit(SvLibIfStatement pSvLibIfStatement) throws NoException {
    trackTagPropertiesForStatementStartingWithNode(pSvLibIfStatement, currentStartingNode);

    CFANode exitNode = getNewNode();

    // Handle conditions
    CFANode trueConditionNode = getNewNode();
    CFANode falseConditionNode = getNewNode();

    CFAEdge trueEdge =
        new SvLibAssumeEdge(
            pSvLibIfStatement.getCondition().toASTString(),
            pSvLibIfStatement.getFileLocation(),
            currentStartingNode,
            trueConditionNode,
            pSvLibIfStatement.getCondition(),
            true,
            false,
            false);
    CFAEdge falseEdge =
        new SvLibAssumeEdge(
            "!(" + pSvLibIfStatement.getCondition().toASTString() + ")",
            pSvLibIfStatement.getFileLocation(),
            currentStartingNode,
            falseConditionNode,
            pSvLibIfStatement.getCondition(),
            false,
            false,
            false);
    CFACreationUtils.addEdgeToCFA(trueEdge, logger);
    CFACreationUtils.addEdgeToCFA(falseEdge, logger);

    // Handle the then branch
    currentStartingNode = trueConditionNode;
    Optional<CFANode> thenBranchNode = pSvLibIfStatement.getThenBranch().accept(this);

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
    if (pSvLibIfStatement.getElseBranch().isPresent()) {
      currentStartingNode = falseConditionNode;
      Optional<CFANode> elseBranchNode =
          pSvLibIfStatement.getElseBranch().orElseThrow().accept(this);
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
              pSvLibIfStatement.getFileLocation(),
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
  public Optional<CFANode> visit(SvLibBreakStatement pSvLibBreakStatement) throws NoException {
    trackTagPropertiesForStatementStartingWithNode(pSvLibBreakStatement, currentStartingNode);

    Verify.verify(outermostLoopExitNode.isPresent());
    CFAEdge edge =
        new BlankEdge(
            "break",
            pSvLibBreakStatement.getFileLocation(),
            currentStartingNode,
            outermostLoopExitNode.orElseThrow(),
            "break");
    CFACreationUtils.addEdgeToCFA(edge, logger);

    // In case someone
    return Optional.empty();
  }

  @Override
  public Optional<CFANode> visit(SvLibContinueStatement pSvLibContinueStatement)
      throws NoException {
    Verify.verify(outermostLoopHead.isPresent());

    trackTagPropertiesForStatementStartingWithNode(pSvLibContinueStatement, currentStartingNode);

    CFAEdge edge =
        new BlankEdge(
            "break",
            pSvLibContinueStatement.getFileLocation(),
            currentStartingNode,
            outermostLoopHead.orElseThrow(),
            "break");
    CFACreationUtils.addEdgeToCFA(edge, logger);

    // In case someone
    return Optional.empty();
  }

  @Override
  public Optional<CFANode> visit(SvLibReturnStatement pSvLibReturnStatement) throws NoException {
    trackTagPropertiesForStatementStartingWithNode(pSvLibReturnStatement, currentStartingNode);

    CFACreationUtils.addEdgeToCFA(
        new BlankEdge(
            "return",
            pSvLibReturnStatement.getFileLocation(),
            currentStartingNode,
            functionExitNode,
            "return"),
        logger);
    return Optional.empty();
  }

  @Override
  public Optional<CFANode> visit(SvLibGotoStatement pSvLibGotoStatement) throws NoException {
    trackTagPropertiesForStatementStartingWithNode(pSvLibGotoStatement, currentStartingNode);

    // We may not have all the labels yet, so we just store the information for later.
    String label = pSvLibGotoStatement.getLabel();
    gotoNodesToLabels.put(currentStartingNode, label);
    return Optional.empty();
  }

  @Override
  public Optional<CFANode> visit(SvLibLabelStatement pSvLibLabelStatement) throws NoException {
    trackTagPropertiesForStatementStartingWithNode(pSvLibLabelStatement, currentStartingNode);

    labelsToNodes.put(pSvLibLabelStatement.getLabel(), currentStartingNode);
    return Optional.of(currentStartingNode);
  }

  @Override
  public Optional<CFANode> visit(SvLibChoiceStatement pSvLibChoiceStatement) throws NoException {
    CFANode startingNode = currentStartingNode;
    CFANode endNode = getNewNode();
    boolean atLeastOneChoiceHasEndNode = false;

    for (int i = 0; i < pSvLibChoiceStatement.getChoices().size(); i++) {
      SvLibStatement choice = pSvLibChoiceStatement.getChoices().get(i);
      // For each choice, we need to create a branch from the current starting node
      CFANode choiceStartingNode = getNewNode();
      CFACreationUtils.addEdgeToCFA(
          new SvLibBlankChoiceEdge(
              // This is definitely not the nicest solution, but it
              "choice",
              pSvLibChoiceStatement.getFileLocation(),
              startingNode,
              choiceStartingNode,
              "choice branch",
              i),
          logger);

      // Process the choice
      currentStartingNode = choiceStartingNode;
      Optional<CFANode> choiceEndNode = choice.accept(this);

      if (choiceEndNode.isPresent()) {
        // Connect the end of the choice back to the CFA
        CFACreationUtils.addEdgeToCFA(
            new BlankEdge(
                "",
                FileLocation.DUMMY,
                choiceEndNode.orElseThrow(),
                endNode,
                "end of choice branch"),
            logger);
        atLeastOneChoiceHasEndNode = true;
      }
    }

    if (atLeastOneChoiceHasEndNode) {
      return Optional.of(endNode);
    } else {
      return Optional.empty();
    }
  }
}
