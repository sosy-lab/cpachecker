/*
 *  CPAchecker is a tool for configurable software verification.
 *  This file is part of CPAchecker.
 *
 *  Copyright (C) 2007-2019  Dirk Beyer
 *  All rights reserved.
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 *
 *
 *  CPAchecker web page:
 *    http://cpachecker.sosy-lab.org
 */
package org.sosy_lab.cpachecker.util.cwriter;

import static org.sosy_lab.cpachecker.cfa.model.CFAEdgeType.FunctionCallEdge;

import com.google.common.collect.ArrayListMultimap;
import com.google.common.collect.FluentIterable;
import com.google.common.collect.HashMultimap;
import com.google.common.collect.Iterables;
import com.google.common.collect.ListMultimap;
import com.google.common.collect.Multimap;
import java.io.IOException;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.Deque;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import javax.annotation.Nullable;
import org.sosy_lab.common.configuration.Configuration;
import org.sosy_lab.common.configuration.FileOption;
import org.sosy_lab.common.configuration.FileOption.Type;
import org.sosy_lab.common.configuration.InvalidConfigurationException;
import org.sosy_lab.common.configuration.Option;
import org.sosy_lab.common.configuration.Options;
import org.sosy_lab.cpachecker.cfa.CFA;
import org.sosy_lab.cpachecker.cfa.Language;
import org.sosy_lab.cpachecker.cfa.ast.c.CInitializerExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CVariableDeclaration;
import org.sosy_lab.cpachecker.cfa.model.CFAEdge;
import org.sosy_lab.cpachecker.cfa.model.CFAEdgeType;
import org.sosy_lab.cpachecker.cfa.model.CFANode;
import org.sosy_lab.cpachecker.cfa.model.CFATerminationNode;
import org.sosy_lab.cpachecker.cfa.model.FunctionEntryNode;
import org.sosy_lab.cpachecker.cfa.model.c.CAssumeEdge;
import org.sosy_lab.cpachecker.cfa.model.c.CDeclarationEdge;
import org.sosy_lab.cpachecker.cfa.model.c.CFunctionEntryNode;
import org.sosy_lab.cpachecker.cfa.model.c.CFunctionSummaryEdge;
import org.sosy_lab.cpachecker.cfa.model.c.CLabelNode;
import org.sosy_lab.cpachecker.cfa.types.c.CArrayType;
import org.sosy_lab.cpachecker.exceptions.CPAException;
import org.sosy_lab.cpachecker.util.CFATraversal;
import org.sosy_lab.cpachecker.util.CFAUtils;
import org.sosy_lab.cpachecker.util.cwriter.ARGToCTranslator.CompoundStatement;
import org.sosy_lab.cpachecker.util.cwriter.ARGToCTranslator.FunctionBody;
import org.sosy_lab.cpachecker.util.cwriter.ARGToCTranslator.SimpleStatement;
import org.sosy_lab.cpachecker.util.cwriter.ARGToCTranslator.Statement;

@Options(prefix = "cfa.exportToC")
public class CFAToCTranslator {

  // Use original, unqualified names for variables
  private static final boolean NAMES_QUALIFIED = false;

  private static class EmptyStatement extends Statement {

    @Override
    void translateToCode0(StringBuilder pBuffer, int pIndent) {
      // do nothing
    }
  }

  private static class NodeAndBlock {
    private final CFANode cfaNode;
    private final CompoundStatement currentBlock;

    public NodeAndBlock(CFANode pCfaNode, CompoundStatement pCurrentBlock) {
      cfaNode = pCfaNode;
      currentBlock = pCurrentBlock;
    }

    public CFANode getCfaNode() {
      return cfaNode;
    }

    public CompoundStatement getCurrentBlock() {
      return currentBlock;
    }

    @Override
    public boolean equals(Object pO) {
      if (this == pO) {
        return true;
      }
      if (pO == null || getClass() != pO.getClass()) {
        return false;
      }
      NodeAndBlock that = (NodeAndBlock) pO;
      return Objects.equals(cfaNode, that.cfaNode) &&
          Objects.equals(currentBlock, that.currentBlock);
    }

    @Override
    public int hashCode() {
      return Objects.hash(cfaNode, currentBlock);
    }
  }

  @Option(
      secure = true,
      name = "reduceToLabels",
      description =
          "Only write CFA parts syntactically reaching the labels listed in the given file."
              + " The file is expected to contain one label name per line.")
  @FileOption(Type.OPTIONAL_INPUT_FILE)
  private Path whitelistLabelFile = null;

  private final List<String> globalDefinitionsList = new ArrayList<>();
  private final Set<NodeAndBlock> discoveredElements = new HashSet<>();
  private final ListMultimap<CFANode, Statement> createdStatements = ArrayListMultimap.create();
  private Collection<FunctionBody> functions;

  private NodeAndBlock noProgressSince = null;

  public CFAToCTranslator(final Configuration pConfig) throws InvalidConfigurationException {
    pConfig.inject(this);
  }

  public String translateCfa(CFA pCfa)
      throws CPAException, InvalidConfigurationException, IOException {
    functions = new ArrayList<>(pCfa.getNumberOfFunctions());

    if (pCfa.getLanguage() != Language.C) {
      throw new InvalidConfigurationException(
          "CFA can only be written to C for C programs, at the moment");
    }

    Collection<CFAEdge> relevantEdges = null;
    Collection<CLabelNode> labelNodes = getAllLabelNodes(pCfa).collect(Collectors.toSet());
    if (whitelistLabelFile != null) {
      relevantEdges = getEdgesReachingLabels(pCfa, labelNodes, whitelistLabelFile);
    }

    for (FunctionEntryNode func : pCfa.getAllFunctionHeads()) {
      translate((CFunctionEntryNode) func, relevantEdges, labelNodes);
    }

    return generateCCode();
  }

  private Collection<CFAEdge> getEdgesReachingLabels(CFA pCfa, Collection<CLabelNode> pLabelNodes, Path pWhitelistLabelFile)
      throws IOException {

    // TODO: At the moment, this is function-agnostic; all labels with the given name are considered
    // Instead, it would be better to make this more precise and only state the labels that are
    // actually necessary
    final List<String> labels = Files.readAllLines(pWhitelistLabelFile, Charset.defaultCharset());
    final Multimap<String, CFANode> labelNodes = HashMultimap.create();

    pLabelNodes.forEach(n -> labelNodes.put(n.getLabel(), n));

    final Set<CFAEdge> reachableNodes = new HashSet<>();
    for (CFAEdge edgeToLabel :
        labels.stream()
            .flatMap(l -> labelNodes.get(l).stream())
            .flatMap(l -> CFAUtils.enteringEdges(l).stream())
            .collect(Collectors.toSet())) {

      if (reachableNodes.contains(edgeToLabel)) {
        continue;
      }

      reachableNodes.addAll(
          CFATraversal.dfs()
              .backwards()
              .ignoreEdges(reachableNodes)
              .collectEdgesReachableFrom(edgeToLabel.getPredecessor()));
    }

    getAllSummaryEdges(pCfa).forEach(reachableNodes::add);

    return reachableNodes;
  }

  private Stream<CLabelNode> getAllLabelNodes(CFA pCfa) {

    return pCfa.getAllNodes()
        .parallelStream()
        .filter(n -> n instanceof CLabelNode)
        .map(CLabelNode.class::cast);
  }

  private Stream<CFAEdge> getAllSummaryEdges(CFA pCfa) {
    return pCfa.getAllFunctionHeads().stream()
        .flatMap(h -> CFAUtils.enteringEdges(h).stream())
        .map(e -> e.getPredecessor().getLeavingSummaryEdge());
  }

  private String generateCCode() {
    StringBuilder buffer = new StringBuilder();

    for (String globalDef : globalDefinitionsList) {
      buffer.append(globalDef).append("\n");
    }
    buffer.append("\n");

    for (FunctionBody f : functions) {
      f.translateToCode(buffer, 0);
      buffer.append("\n");
    }

    return buffer.toString();
  }

  private void translate(CFunctionEntryNode pEntry, @Nullable Collection<CFAEdge> pRelevantEdges, Collection<CLabelNode> pLabelNodes)
      throws CPAException {
    Collection<String> existingLabels = pLabelNodes.stream()
        .map(n -> n.getLabel())
        .collect(Collectors.toSet());

    // waitlist for the edges to be processed
    Deque<NodeAndBlock> waitlist = new ArrayDeque<>();

    FunctionBody f = startFunction(pEntry);
    functions.add(f);

    offerWaitlist(waitlist, pEntry, f.getFunctionBody());

    while (!waitlist.isEmpty()) {
      NodeAndBlock nextNode = waitlist.poll();
      handleNode(nextNode, waitlist, pRelevantEdges, existingLabels);
    }
  }

  private void handleNode(
      NodeAndBlock pNode,
      Deque<NodeAndBlock> pWaitlist,
      @Nullable Collection<CFAEdge> pEdgesToHandle,
      Collection<String> pExistingLabels)
      throws CPAException {
    CFANode node = pNode.getCfaNode();
    CompoundStatement currentBlock = pNode.getCurrentBlock();

    if (createdStatements.containsKey(node)) {
      String label = getLabel(node, Collections.emptySet(), pExistingLabels);
      String gotoStatement = "goto " + label + ";";
      addStatement(currentBlock, createSimpleStatement(node, gotoStatement));
      return;
    }

    if (!areAllEnteringEdgesHandled(node) || isGotoAndLabelNotHandledYet(node)) {
      if (noProgressSince == null) {
        noProgressSince = pNode;
      } else if (noProgressSince.equals(pNode)) {
        throw new CPAException("No progress in C translation at node " + node);
      }
      offerWaitlist(pWaitlist, node, currentBlock);
      return;

    } else {
      noProgressSince = null;
    }

    assert !createdStatements.containsKey(node) : "Node was already handled";

    if (node instanceof CLabelNode) {
      String labelStmt = getLabelCode(((CLabelNode) node).getLabel());
      currentBlock.addStatement(createSimpleStatement(node, labelStmt));
    }
    if (node instanceof CFATerminationNode) {
      addStatement(currentBlock, createSimpleStatement(node, "abort();"));
    }

    // find the next elements to add to the waitlist
    Collection<CFAEdge> outgoingEdges = getRelevant(CFAUtils.allLeavingEdges(node)).toList();

    assert !(node instanceof CFATerminationNode) || outgoingEdges.isEmpty()
        : "Termination node has outgoing edges: " + outgoingEdges;

    if (outgoingEdges.size() > 0 && getRelevant(CFAUtils.allEnteringEdges(node)).size() > 1) {
      CompoundStatement joinBlock = getJoinBlock(node);
      if (joinBlock != null) {
        if (!joinBlock.equals(currentBlock)
            && !joinBlock.equals(currentBlock.getSurroundingBlock())) {
          offerWaitlist(pWaitlist, node, currentBlock);
          NodeAndBlock join = new NodeAndBlock(node, joinBlock);
          if (discoveredElements.contains(join)) {
            pWaitlist.remove(join);
          }
        }
        currentBlock = joinBlock;
      }
    }

    List<NodeAndBlock> nextNodes;
    if (outgoingEdges.size() >= 2) {
      nextNodes = handleBranching(outgoingEdges, currentBlock, pEdgesToHandle);

    } else if (outgoingEdges.size() == 1) {
      assert !(Iterables.getOnlyElement(outgoingEdges) instanceof CAssumeEdge)
          : "The worst case happened: A single assume edge!";

      nextNodes =
          Collections.singletonList(
              handleEdge(Iterables.getOnlyElement(outgoingEdges), currentBlock, pEdgesToHandle));
    } else {
      nextNodes = Collections.emptyList();
    }

    for (NodeAndBlock next : nextNodes) {
      offerWaitlistIfNew(pWaitlist, next.getCfaNode(), next.getCurrentBlock());
    }
  }

  private void offerWaitlistIfNew(
      Deque<NodeAndBlock> pWaitlist, CFANode pCfaNode, CompoundStatement pCurrentBlock) {

    NodeAndBlock next = new NodeAndBlock(pCfaNode, pCurrentBlock);
    if (!discoveredElements.contains(next)) {
      discoveredElements.add(next);
      offerWaitlist(pWaitlist, pCfaNode, pCurrentBlock);
    }
  }

  private boolean isGotoAndLabelNotHandledYet(CFANode pNode) {
    if (pNode.getNumLeavingEdges() == 1) {
      CFAEdge leavingEdge = pNode.getLeavingEdge(0);
      if (isGotoEdge(leavingEdge)
          && !(CFAUtils.enteringEdges(leavingEdge.getSuccessor()).allMatch(n -> isGotoEdge(n)))) {
        return !createdStatements.containsKey(leavingEdge.getSuccessor());
      }
    }
    return false;
  }

  private boolean isGotoEdge(CFAEdge pEdge) {
    return pEdge.getEdgeType().equals(CFAEdgeType.BlankEdge)
        && pEdge.getDescription().startsWith("Goto:")
        && pEdge.getSuccessor() instanceof CLabelNode;
  }

  private FunctionBody startFunction(CFunctionEntryNode pFunctionStartNode) {
    String lFunctionHeader =
        pFunctionStartNode.getFunctionDefinition().toASTString(NAMES_QUALIFIED).replace(";", "");
    return new FunctionBody(lFunctionHeader, createCompoundStatement(pFunctionStartNode, null));
  }

  private List<NodeAndBlock> handleBranching(
      Collection<CFAEdge> branchingEdges, CompoundStatement currentBlock, @Nullable Collection<CFAEdge> pRelevantEdges) {

    assert branchingEdges.size() == 2
        : "branches with more than two options not supported yet (was the program prepocessed with CIL?)"; // TODO: why not btw?

    // collect edges of condition branch
    List<NodeAndBlock> result = new ArrayList<>(2);
    int ind = 0;
    boolean previousTruthAssumption = false;
    String elseCond = null;
    for (CFAEdge edgeToChild : branchingEdges) {
      assert edgeToChild instanceof CAssumeEdge
          : "something wrong: branch without condition: " + edgeToChild;
      CAssumeEdge assumeEdge = (CAssumeEdge) edgeToChild;
      boolean truthAssumption = getRealTruthAssumption(assumeEdge);

      String cond = "";

      if (truthAssumption) {
        if (assumeEdge.getTruthAssumption()) {
          cond = "if (" + assumeEdge.getExpression().toASTString(NAMES_QUALIFIED) + ")";
        } else {
          cond = "if (!(" + assumeEdge.getExpression().toASTString(NAMES_QUALIFIED) + "))";
        }
      } else {
        cond = "else ";
      }

      if (ind > 0 && truthAssumption == previousTruthAssumption) {
        throw new AssertionError(
            "Two assume edges with same truth value, thus, cannot generate C program from ARG.");
      }

      ind++;

      // create a new block starting with this condition
      CompoundStatement newBlock;
      if (ind == 1 && !truthAssumption) {
        newBlock = createCompoundStatement(edgeToChild.getPredecessor(), currentBlock);
        elseCond = cond;
      } else {
        newBlock = addIfStatement(assumeEdge, currentBlock, cond);
      }

      if (truthAssumption && elseCond != null) {
        addStatement(currentBlock, createSimpleStatement(edgeToChild.getPredecessor(), elseCond));
        addStatement(currentBlock, result.get(0).getCurrentBlock());
      }

      addStopIfNotRelevant(edgeToChild, newBlock, pRelevantEdges);

      NodeAndBlock newNode = new NodeAndBlock(edgeToChild.getSuccessor(), newBlock);
      if (truthAssumption) {
        result.add(0, newNode);
      } else {
        result.add(newNode);
      }

      previousTruthAssumption = truthAssumption;
    }
    assert result.size() == branchingEdges.size()
        : "Less edges going out than came in: " + result + " vs. " + branchingEdges;
    return result;
  }

  private void addStatement(CompoundStatement pCurrentBlock, Statement pStatement) {
    assert !(pStatement instanceof SimpleStatement)
        || pStatement.getSurroundingBlock() == null
        : "Statement already assigned to some block: " + pStatement;
    pCurrentBlock.addStatement(pStatement);
    assert pStatement.getSurroundingBlock() == pCurrentBlock;
  }

  private SimpleStatement createSimpleStatement(CFANode pNode, String pStatement) {
    SimpleStatement st = new SimpleStatement(pStatement);
    createdStatements.put(pNode, st);
    return st;
  }

  private CompoundStatement createCompoundStatement(
      CFANode pStart, @Nullable CompoundStatement pOuterBlock) {
    CompoundStatement st;
    if (pOuterBlock != null) {
      st = new CompoundStatement(pStart, pOuterBlock);
    } else {
      st = new CompoundStatement(pStart);
    }
    //createdStatements.put(pNode, st);
    return st;
  }

  private boolean getRealTruthAssumption(final CAssumeEdge assumption) {
    return assumption.isSwapped() != assumption.getTruthAssumption();
  }

  private void offerWaitlist(
      Deque<NodeAndBlock> pWaitlist, CFANode pNode, CompoundStatement pCurrentBlock) {
    pWaitlist.add(new NodeAndBlock(pNode, pCurrentBlock));
  }

  private CompoundStatement addIfStatement(
      CFAEdge pConditionEdge, CompoundStatement block, String conditionCode) {
    addStatement(block, createSimpleStatement(pConditionEdge.getPredecessor(), conditionCode));
    CompoundStatement newBlock = createCompoundStatement(pConditionEdge.getPredecessor(), block);
    addStatement(block, newBlock);
    return newBlock;
  }

  private String getLabelCode(final String pLabelName) {
    return pLabelName + ":; ";
  }

  /**
   * Computes the block to continue with after a join. Returns <code>null</code> if computation
   * fails.
   */
  private @Nullable CompoundStatement getJoinBlock(CFANode pJoinNode) {
    FluentIterable<CFANode> predecessorNodes =
        getRelevant(CFAUtils.allEnteringEdges(pJoinNode)).transform(e -> e.getPredecessor());
    if (predecessorNodes.anyMatch(n -> !createdStatements.containsKey(n))) {
      return null;
    }

    Set<CompoundStatement> blocksBeforePredecessor =
        predecessorNodes
            .transform(n -> createdStatements.get(n).get(0).getSurroundingBlock())
            .toSet();

    boolean madeProgress;
    do {
      madeProgress = false;
      Set<CompoundStatement> newBlocksBeforePredecessor = new HashSet<>();
      for (CompoundStatement s : blocksBeforePredecessor) {
        CFANode blockStart = s.getBlockStart();
        boolean foundMatch = false;
        assert blockStart != null;
        for (CompoundStatement o : blocksBeforePredecessor) {
          if (o.equals(s)) {
            continue;
          }
          if (blockStart.equals(o.getBlockStart())) {
            foundMatch = true;
            newBlocksBeforePredecessor.add(s.getSurroundingBlock());
            assert createdStatements
                .get(blockStart)
                .get(0)
                .getSurroundingBlock()
                .equals(s.getSurroundingBlock());
            break;
          }
        }
        if (foundMatch) {
          madeProgress = true;
        } else {
          newBlocksBeforePredecessor.add(s);
        }
      }
      blocksBeforePredecessor = newBlocksBeforePredecessor;
    } while (madeProgress);
    return blocksBeforePredecessor.stream()
        .min(Comparator.comparingInt(this::getNumberOfOuterBlocks))
        .orElse(null);
  }

  private int getNumberOfOuterBlocks(CompoundStatement pStatement) {
    return pStatement.getSurroundingBlock() == null
        ? 0
        : 1 + getNumberOfOuterBlocks(pStatement.getSurroundingBlock());
  }

  private boolean areAllEnteringEdgesHandled(CFANode pNode) {
    return pNode instanceof CFunctionEntryNode
        || pNode.isLoopStart()
        || (pNode instanceof CLabelNode && isLabelHandledIfExists(pNode))
        || getRelevant(CFAUtils.allEnteringEdges(pNode))
            .allMatch(e -> createdStatements.containsKey(e.getPredecessor()));
  }

  private boolean isLabelHandledIfExists(CFANode pNode) {
    if (pNode instanceof CLabelNode) {
      for (CFAEdge e : CFAUtils.enteringEdges(pNode)) {
        if (e.getEdgeType() == CFAEdgeType.BlankEdge
            && e.getDescription().startsWith("Label:")
            && !createdStatements.containsKey(e.getPredecessor())) {
          return false;
        }
      }
    }
    return true;
  }

  private NodeAndBlock handleEdge(
      CFAEdge edge, CompoundStatement currentBlock, @Nullable Collection<CFAEdge> pEdgesToHandle)
      throws CPAException {

    processEdge(edge, currentBlock, pEdgesToHandle);

    assert !edge.getEdgeType().equals(FunctionCallEdge);
    return new NodeAndBlock(edge.getSuccessor(), currentBlock);
  }

  private void addStop(CFANode pNode, CompoundStatement pCurrentBlock) {
    addStatement(pCurrentBlock, createSimpleStatement(pNode, "exit(0); // covered"));
  }

  private FluentIterable<CFAEdge> getRelevant(FluentIterable<CFAEdge> pEdges) {
    return pEdges
        .filter(e -> e.getEdgeType() != CFAEdgeType.FunctionReturnEdge)
        .filter(e -> e.getEdgeType() != CFAEdgeType.FunctionCallEdge);
  }

  private String getLabel(CFANode pNode, Set<CFAEdge> pEdgesToIgnore, Collection<String> pExistingLabels) {
    if (pNode instanceof CLabelNode) {
      return ((CLabelNode) pNode).getLabel();
    } else {
      CompoundStatement targetBlock = createdStatements.get(pNode).get(0).getSurroundingBlock();
      com.google.common.base.Optional<CFANode> maybeBlankPredecessor = CFAUtils.allEnteringEdges(pNode)
          .filter(x -> !pEdgesToIgnore.contains(x))
          .filter(x -> x.getEdgeType() == CFAEdgeType.BlankEdge)
          .transform(x -> x.getPredecessor())
          .filter(createdStatements::containsKey)
          .filter(x -> createdStatements.get(x).get(0).getSurroundingBlock().equals(targetBlock))
          .first();
      if (maybeBlankPredecessor.isPresent()) {
        return getLabel(maybeBlankPredecessor.get(), pEdgesToIgnore, pExistingLabels);
      } else {
        return createdStatements.get(pNode).get(0).getLabel(pExistingLabels);
      }
    }
  }

  private void addStopIfNotRelevant(CFAEdge pEdge, CompoundStatement pCurrentBlock, Collection<CFAEdge> pEdgesToHandle) {
    final CFANode node = pEdge.getPredecessor();
    if (pEdgesToHandle != null && !pEdgesToHandle.contains(pEdge)) {
      if (CFAUtils.enteringEdges(pEdge.getPredecessor()).anyMatch(pEdgesToHandle::contains)) {
        addStop(node, pCurrentBlock);
      }
    }
  }

  private void processEdge(
      CFAEdge edge, CompoundStatement currentBlock, @Nullable Collection<CFAEdge> pEdgesToHandle)
      throws CPAException {

    final CFANode node = edge.getPredecessor();
    addStopIfNotRelevant(edge, currentBlock, pEdgesToHandle);

    final String statement = processSimpleEdge(edge);
    final Statement s;
    if (statement.isEmpty()) {
      s = new EmptyStatement();
      createdStatements.put(node, s);
    } else {
      s = createSimpleStatement(node, statement);
    }

    addStatement(currentBlock, s);
  }

  private String processSimpleEdge(CFAEdge pCFAEdge) throws CPAException {
    if (pCFAEdge == null) {
      return "";
    }

    String stmt = "";
    if (pCFAEdge instanceof CFunctionSummaryEdge) {
      stmt += pCFAEdge.getCode();
      return stmt + (stmt.endsWith(";") ? "" : ";");
    }

    switch (pCFAEdge.getEdgeType()) {
      case BlankEdge:
        {
            // nothing to do
            break;
        }

      case AssumeEdge:
        {
          // nothing to do
          break;
        }

      case StatementEdge:
      case ReturnStatementEdge:
        {
          String statementText = pCFAEdge.getCode();
          if (!statementText.matches("^__CPAchecker_TMP_[0-9]+;?$")) {
            stmt += statementText + (statementText.endsWith(";") ? "" : ";");
          }
          break;
        }

      case DeclarationEdge:
        {
          CDeclarationEdge lDeclarationEdge = (CDeclarationEdge) pCFAEdge;
          String declaration;
          // TODO adapt if String in
          // org.sosy_lab.cpachecker.cfa.parser.eclipse.c.ASTConverter#createInitializedTemporaryVariable is changed
          if (lDeclarationEdge
              .getDeclaration()
              .toASTString(NAMES_QUALIFIED)
              .contains("__CPAchecker_TMP_")) {
            declaration = lDeclarationEdge.getDeclaration().toASTString(NAMES_QUALIFIED);
          } else {
            // TODO check if works without lDeclarationEdge.getRawStatement();
            declaration = lDeclarationEdge.getDeclaration().toASTString(NAMES_QUALIFIED);

            if (lDeclarationEdge.getDeclaration() instanceof CVariableDeclaration) {
              CVariableDeclaration varDecl =
                  (CVariableDeclaration) lDeclarationEdge.getDeclaration();
              if (varDecl.getType() instanceof CArrayType
                  && varDecl.getInitializer() instanceof CInitializerExpression) {
                int assignAfterPos = declaration.indexOf("=") + 1;
                declaration =
                    declaration.substring(0, assignAfterPos)
                        + "{"
                        + declaration.substring(assignAfterPos, declaration.lastIndexOf(";"))
                        + "};";
              }
            }

            if (declaration.contains(",")) {
              for (CFAEdge predEdge : CFAUtils.enteringEdges(pCFAEdge.getPredecessor())) {
                if (predEdge
                    .getRawStatement()
                    .equals(lDeclarationEdge.getDeclaration().toASTString())) {
                  declaration = "";
                  break;
                }
              }
            }
          }

          if (declaration.contains("org.eclipse.cdt.internal.core.dom.parser.ProblemType@")) {
            throw new CPAException(
                "Failed to translate CFA into program because a type could not be properly resolved.");
          }

          if (lDeclarationEdge.getDeclaration().isGlobal()) {
            assert stmt.isEmpty();
            globalDefinitionsList.add(declaration + (declaration.endsWith(";") ? "" : ";"));
          } else {
            stmt += declaration;
          }

          break;
        }

      case FunctionCallEdge:
      case CallToReturnEdge:
        {
          // this should not have been taken
          throw new AssertionError("Unexpected edge type: " + pCFAEdge.getEdgeType()
              + " for edge " + pCFAEdge);
        }

      default:
        {
          throw new AssertionError(
              "Unexpected edge " + pCFAEdge + " of type " + pCFAEdge.getEdgeType());
        }
    }

    return stmt;
  }
}
