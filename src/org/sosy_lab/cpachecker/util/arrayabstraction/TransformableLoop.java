// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2007-2021 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.util.arrayabstraction;

import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Preconditions.checkNotNull;

import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Sets;
import java.math.BigInteger;
import java.util.HashSet;
import java.util.Optional;
import java.util.Set;
import org.sosy_lab.cpachecker.cfa.CFA;
import org.sosy_lab.cpachecker.cfa.ast.c.CDeclaration;
import org.sosy_lab.cpachecker.cfa.ast.c.CSimpleDeclaration;
import org.sosy_lab.cpachecker.cfa.ast.c.CVariableDeclaration;
import org.sosy_lab.cpachecker.cfa.model.CFAEdge;
import org.sosy_lab.cpachecker.cfa.model.CFANode;
import org.sosy_lab.cpachecker.cfa.model.FunctionSummaryEdge;
import org.sosy_lab.cpachecker.cfa.model.c.CDeclarationEdge;
import org.sosy_lab.cpachecker.cfa.model.c.CFunctionCallEdge;
import org.sosy_lab.cpachecker.cfa.model.c.CFunctionEntryNode;
import org.sosy_lab.cpachecker.util.CFATraversal;
import org.sosy_lab.cpachecker.util.CFATraversal.CFAVisitor;
import org.sosy_lab.cpachecker.util.CFATraversal.TraversalProcess;
import org.sosy_lab.cpachecker.util.CFAUtils;
import org.sosy_lab.cpachecker.util.LoopStructure;
import org.sosy_lab.cpachecker.util.LoopStructure.Loop;
import org.sosy_lab.cpachecker.util.dependencegraph.Dominance;
import org.sosy_lab.cpachecker.util.dependencegraph.DominanceUtils;
import org.sosy_lab.cpachecker.util.dependencegraph.EdgeDefUseData;
import org.sosy_lab.cpachecker.util.states.MemoryLocation;
import org.sosy_lab.cpachecker.util.variableclassification.VariableClassification;

/** Represents a loop that can be transformed by an array abstraction. */
final class TransformableLoop {

  private final LoopStructure.Loop loop;
  private final CFANode loopNode;
  private final Index index;

  public TransformableLoop(Loop pLoop, CFANode pLoopNode, Index pIndex) {
    loop = pLoop;
    loopNode = pLoopNode;
    index = pIndex;
  }

  private static ImmutableSet<CFAEdge> allInnerLoopEdges(LoopStructure.Loop pLoop) {

    ImmutableSet.Builder<CFAEdge> builder = ImmutableSet.builder();
    builder.addAll(pLoop.getInnerLoopEdges());

    for (CFANode node : pLoop.getLoopNodes()) {
      FunctionSummaryEdge summaryEdge = node.getLeavingSummaryEdge();
      if (summaryEdge != null) {
        builder.add(summaryEdge);
      }
    }

    return builder.build();
  }

  private static ImmutableSet<CFAEdge> getDominatedInnerLoopEdges(
      CFAEdge pEdge, LoopStructure.Loop pLoop, CFANode pLoopStart) {

    checkArgument(pLoop.getLoopNodes().contains(pLoopStart));
    checkArgument(allInnerLoopEdges(pLoop).contains(pEdge));

    ImmutableSet.Builder<CFAEdge> builder = ImmutableSet.builder();

    CFANode startNode = pEdge.getSuccessor();
    Dominance.DomTree<CFANode> domTree =
        DominanceUtils.createFunctionDomTree(startNode, ImmutableSet.of(pLoopStart));
    int startId = domTree.getId(startNode);
    for (int id = 0; id < domTree.getNodeCount(); id++) {
      if (id == startId || domTree.isAncestorOf(startId, id)) {
        builder.addAll(CFAUtils.allLeavingEdges(domTree.getNode(id)));
      }
    }

    return builder.build();
  }

  private static ImmutableSet<CFAEdge> getPostDominatedInnerLoopEdges(
      CFAEdge pEdge, LoopStructure.Loop pLoop, CFANode pLoopStart) {

    checkArgument(pLoop.getLoopNodes().contains(pLoopStart));
    checkArgument(allInnerLoopEdges(pLoop).contains(pEdge));

    ImmutableSet.Builder<CFAEdge> builder = ImmutableSet.builder();

    CFANode startNode = pEdge.getPredecessor();
    Dominance.DomTree<CFANode> postDomTree =
        DominanceUtils.createFunctionPostDomTree(startNode, ImmutableSet.of(pLoopStart));
    int startId = postDomTree.getId(startNode);
    for (int id = 0; id < postDomTree.getNodeCount(); id++) {
      if (id == startId || postDomTree.isAncestorOf(startId, id)) {
        builder.addAll(CFAUtils.allEnteringEdges(postDomTree.getNode(id)));
      }
    }

    return builder.build();
  }

  private static boolean isExecutedEveryIteration(
      CFAEdge pEdge, LoopStructure.Loop pLoop, CFANode pLoopNode) {

    Set<CFAEdge> dominatedEdges = new HashSet<>();
    dominatedEdges.add(pEdge);
    dominatedEdges.addAll(getDominatedInnerLoopEdges(pEdge, pLoop, pLoopNode));
    dominatedEdges.addAll(getPostDominatedInnerLoopEdges(pEdge, pLoop, pLoopNode));

    return Sets.difference(allInnerLoopEdges(pLoop), dominatedEdges).isEmpty();
  }

  private static ImmutableSet<CFAEdge> getIncomingDefs(
      CFAEdge pIncomingEdge, CSimpleDeclaration pVariableDeclaration) {

    ImmutableSet.Builder<CFAEdge> builder = ImmutableSet.builder();

    MemoryLocation memoryLocation = MemoryLocation.forDeclaration(pVariableDeclaration);
    EdgeDefUseData.Extractor extractor = EdgeDefUseData.createExtractor(false);
    CFAEdge incomingEdge = pIncomingEdge;

    if (extractor.extract(incomingEdge).getDefs().contains(memoryLocation)) {
      builder.add(incomingEdge);
      return builder.build();
    }

    CFATraversal.dfs()
        .backwards()
        .traverseOnce(
            incomingEdge.getPredecessor(),
            new CFAVisitor() {

              @Override
              public TraversalProcess visitEdge(CFAEdge pEdge) {

                if (extractor.extract(pEdge).getDefs().contains(memoryLocation)) {
                  builder.add(pEdge);

                  return TraversalProcess.SKIP;
                }

                return TraversalProcess.CONTINUE;
              }

              @Override
              public TraversalProcess visitNode(CFANode pNode) {
                return TraversalProcess.CONTINUE;
              }
            });

    return builder.build();
  }

  private static ImmutableSet<CFAEdge> getOutgoingUses(
      CFAEdge pOutgoingEdge, CSimpleDeclaration pVariableDeclaration) {

    ImmutableSet.Builder<CFAEdge> builder = ImmutableSet.builder();

    MemoryLocation memoryLocation = MemoryLocation.forDeclaration(pVariableDeclaration);
    EdgeDefUseData.Extractor extractor = EdgeDefUseData.createExtractor(false);
    CFAEdge outgoingEdge = pOutgoingEdge;

    CFATraversal.dfs()
        .traverseOnce(
            outgoingEdge.getSuccessor(),
            new CFAVisitor() {

              @Override
              public TraversalProcess visitEdge(CFAEdge pEdge) {

                EdgeDefUseData edgeDefUseData = extractor.extract(pEdge);

                if (edgeDefUseData.getUses().contains(memoryLocation)) {
                  builder.add(pEdge);
                }

                if (edgeDefUseData.getDefs().contains(memoryLocation)) {
                  return TraversalProcess.SKIP;
                }

                return TraversalProcess.CONTINUE;
              }

              @Override
              public TraversalProcess visitNode(CFANode pNode) {
                return TraversalProcess.CONTINUE;
              }
            });

    return builder.build();
  }
  
  private static int countInnerLoopDefs(LoopStructure.Loop pLoop, CSimpleDeclaration pDeclaration) {
    
    MemoryLocation memoryLocation = MemoryLocation.forDeclaration(pDeclaration);
    EdgeDefUseData.Extractor extractor = EdgeDefUseData.createExtractor(false);

    int count = 0;
    for (CFAEdge edge : allInnerLoopEdges(pLoop)) {
      if (extractor.extract(edge).getDefs().contains(memoryLocation)) {
        count++;
      }
    }

    return count;
  }

  private static boolean isAddressed(CFA pCfa, CSimpleDeclaration pVariableDeclaration) {

    VariableClassification variableClassification = pCfa.getVarClassification().orElseThrow();

    return variableClassification
        .getAddressedVariables()
        .contains(pVariableDeclaration.getQualifiedName());
  }

  private static Optional<TransformableLoop> forLoop(CFA pCfa, LoopStructure.Loop pLoop) {

    if (pLoop.getIncomingEdges().size() != 1 || pLoop.getOutgoingEdges().size() != 1) {
      return Optional.empty();
    }

    CFAEdge incomingEdge = pLoop.getIncomingEdges().stream().findAny().orElseThrow();
    CFAEdge outgoingEdge = pLoop.getOutgoingEdges().stream().findAny().orElseThrow();

    if (!incomingEdge.getSuccessor().equals(outgoingEdge.getPredecessor())) {
      return Optional.empty();
    }

    CFANode loopNode = incomingEdge.getSuccessor();

    // detect inner loops
    for (CFANode node : pLoop.getLoopNodes()) {
      if (!node.equals(loopNode) && node.isLoopStart()) {
        return Optional.empty();
      }
    }

    // find loop index by looking at the loop condition
    Optional<SpecialOperation.ComparisonAssume> optLoopCondition =
        SpecialOperation.ComparisonAssume.forEdge(
            outgoingEdge, pCfa.getMachineModel(), ImmutableMap.of());
    if (optLoopCondition.isEmpty()) {
      return Optional.empty();
    }

    SpecialOperation.ComparisonAssume loopCondition = optLoopCondition.orElseThrow();
    CSimpleDeclaration indexVariableDeclaration = loopCondition.getDeclaration();
    MemoryLocation indexMemoryLocation = MemoryLocation.forDeclaration(indexVariableDeclaration);

    // don't allow loop indices that are addressed, so we don't miss any defs,
    // even without pointer information
    if (isAddressed(pCfa, indexVariableDeclaration)) {
      return Optional.empty();
    }

    // find the index update edge inside the loop
    CFAEdge updateIndexEdge = null;
    SpecialOperation.UpdateAssign updateIndexOperation = null;
    EdgeDefUseData.Extractor defUseExtractor = EdgeDefUseData.createExtractor(false);
    ImmutableSet<CFAEdge> innerLoopEdges = allInnerLoopEdges(pLoop);
    for (CFAEdge innerLoopEdge : innerLoopEdges) {

      Optional<SpecialOperation.UpdateAssign> optUpdateAssign =
          SpecialOperation.UpdateAssign.forEdge(
              innerLoopEdge, pCfa.getMachineModel(), ImmutableMap.of());
      if (optUpdateAssign.isPresent()) {
        SpecialOperation.UpdateAssign updateAssign = optUpdateAssign.orElseThrow();
        if (updateIndexEdge == null
            && updateAssign.getDeclaration().equals(indexVariableDeclaration)) {

          // the index must be updated every loop iteration
          if (!isExecutedEveryIteration(innerLoopEdge, pLoop, loopNode)) {
            return Optional.empty();
          }

          updateIndexEdge = innerLoopEdge;
          updateIndexOperation = updateAssign;

          continue;
        }
      }

      ImmutableSet<MemoryLocation> edgeDefs = defUseExtractor.extract(innerLoopEdge).getDefs();

      // don't allow index defs other than the index update inside the loop body
      if (edgeDefs.contains(indexMemoryLocation)) {
        return Optional.empty();
      }
    }

    if (updateIndexEdge == null) {
      return Optional.empty();
    }

    assert updateIndexOperation != null;

    // there must be exactly one incoming index definition to be able to determine the initial index
    // value
    ImmutableSet<CFAEdge> incomingDefs = getIncomingDefs(incomingEdge, indexVariableDeclaration);
    if (incomingDefs.size() != 1) {
      return Optional.empty();
    }

    CFAEdge incomingIndexDefEdge = incomingDefs.stream().findAny().orElseThrow();

    // the incoming index definition must be constant
    Optional<SpecialOperation.ConstantAssign> optIncomingIndexAssign =
        SpecialOperation.ConstantAssign.forEdge(
            incomingIndexDefEdge, pCfa.getMachineModel(), ImmutableMap.of());
    if (optIncomingIndexAssign.isEmpty()) {
      return Optional.empty();
    }

    SpecialOperation.ConstantAssign incomingIndexAssign = optIncomingIndexAssign.orElseThrow();

    Index index =
        new Index(
            incomingIndexDefEdge,
            incomingIndexAssign,
            updateIndexEdge,
            updateIndexOperation,
            loopCondition);

    return Optional.of(new TransformableLoop(pLoop, loopNode, index));
  }

  /** Returns all transformable loops in the specified CFA. */
  public static ImmutableSet<TransformableLoop> findTransformableLoops(CFA pCfa) {

    ImmutableSet.Builder<TransformableLoop> transformableLoops = ImmutableSet.builder();
    LoopStructure loopStructure = pCfa.getLoopStructure().orElseThrow();

    for (LoopStructure.Loop loop : loopStructure.getAllLoops()) {
      Optional<TransformableLoop> optTransformableLoop = TransformableLoop.forLoop(pCfa, loop);
      if (optTransformableLoop.isPresent()) {
        transformableLoops.add(optTransformableLoop.orElseThrow());
      }
    }

    return transformableLoops.build();
  }

  public CFANode getLoopNode() {
    return loopNode;
  }

  public Index getIndex() {
    return index;
  }

  public ImmutableSet<CFAEdge> getInnerLoopEdges() {
    return allInnerLoopEdges(loop);
  }

  public CFAEdge getIncomingEdge() {
    return loop.getIncomingEdges().stream().findAny().orElseThrow();
  }

  public CFAEdge getOutgoingEdge() {
    return loop.getOutgoingEdges().stream().findAny().orElseThrow();
  }

  public ImmutableSet<CFAEdge> getDominatedInnerLoopEdges(CFAEdge pEdge) {
    return getDominatedInnerLoopEdges(pEdge, loop, loopNode);
  }

  public ImmutableSet<CFAEdge> getPostDominatedInnerLoopEdges(CFAEdge pEdge) {
    return getPostDominatedInnerLoopEdges(pEdge, loop, loopNode);
  }

  public boolean isExecutedEveryIteration(CFAEdge pEdge) {
    return isExecutedEveryIteration(pEdge, loop, loopNode);
  }

  public boolean hasOutgoingUses(CSimpleDeclaration pDeclaration) {
    return !getOutgoingUses(getOutgoingEdge(), pDeclaration).isEmpty();
  }

  public int countInnerLoopDefs(CSimpleDeclaration pDeclaration) {
    return countInnerLoopDefs(loop, pDeclaration);
  }

  public ImmutableSet<CSimpleDeclaration> getInnerLoopDeclarations() {

    ImmutableSet.Builder<CSimpleDeclaration> builder = ImmutableSet.builder();

    for (CFAEdge innerLoopEdge : getInnerLoopEdges()) {
      if (innerLoopEdge instanceof CDeclarationEdge) {
        CDeclaration declaration = ((CDeclarationEdge) innerLoopEdge).getDeclaration();
        if (declaration instanceof CVariableDeclaration) {
          builder.add(declaration);
        }
      } else if (innerLoopEdge instanceof CFunctionCallEdge) {
        CFunctionEntryNode functionEntryNode = ((CFunctionCallEdge) innerLoopEdge).getSuccessor();
        builder.addAll(functionEntryNode.getFunctionDefinition().getParameters());
      }
    }

    return builder.build();
  }

  public static final class Index {

    private final CFAEdge initializeEdge;
    private final SpecialOperation.ConstantAssign initializeOperation;

    private final CFAEdge updateEdge;
    private final SpecialOperation.UpdateAssign updateOperation;

    private final SpecialOperation.ComparisonAssume comparisonOperation;

    public Index(
        CFAEdge pInitializeEdge,
        SpecialOperation.ConstantAssign pInitializeOperation,
        CFAEdge pUpdateEdge,
        SpecialOperation.UpdateAssign pUpdateOperation,
        SpecialOperation.ComparisonAssume pComparisonOperation) {
      initializeEdge = checkNotNull(pInitializeEdge);
      initializeOperation = checkNotNull(pInitializeOperation);
      updateEdge = checkNotNull(pUpdateEdge);
      updateOperation = checkNotNull(pUpdateOperation);
      comparisonOperation = checkNotNull(pComparisonOperation);
    }

    public CSimpleDeclaration getVariableDeclaration() {
      return initializeOperation.getDeclaration();
    }

    public CFAEdge getInitializeEdge() {
      return initializeEdge;
    }

    public SpecialOperation.ConstantAssign getInitializeOperation() {
      return initializeOperation;
    }

    public CFAEdge getUpdateEdge() {
      return updateEdge;
    }

    public SpecialOperation.UpdateAssign getUpdateOperation() {
      return updateOperation;
    }

    public boolean isIncreasing() {
      return updateOperation.getStepValue().compareTo(BigInteger.ZERO) > 0;
    }

    public boolean isDecreasing() {
      return updateOperation.getStepValue().compareTo(BigInteger.ZERO) < 0;
    }

    public SpecialOperation.ComparisonAssume getComparisonOperation() {
      return comparisonOperation;
    }
  }
}
