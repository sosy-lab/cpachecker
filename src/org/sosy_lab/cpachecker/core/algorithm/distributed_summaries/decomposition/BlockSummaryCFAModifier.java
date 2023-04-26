// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2023 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.core.algorithm.distributed_summaries.decomposition;

import static org.sosy_lab.common.collect.Collections3.transformedImmutableSetCopy;

import com.google.common.collect.FluentIterable;
import com.google.common.collect.ImmutableListMultimap;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import org.sosy_lab.common.ShutdownNotifier;
import org.sosy_lab.common.log.LogManager;
import org.sosy_lab.cpachecker.cfa.CFA;
import org.sosy_lab.cpachecker.cfa.CfaMetadata;
import org.sosy_lab.cpachecker.cfa.ImmutableCFA;
import org.sosy_lab.cpachecker.cfa.ast.FileLocation;
import org.sosy_lab.cpachecker.cfa.graph.FlexCfaNetwork;
import org.sosy_lab.cpachecker.cfa.model.BlankEdge;
import org.sosy_lab.cpachecker.cfa.model.CFAEdge;
import org.sosy_lab.cpachecker.cfa.model.CFANode;
import org.sosy_lab.cpachecker.cfa.model.CFATerminationNode;
import org.sosy_lab.cpachecker.cfa.transformer.c.CCfaFactory;
import org.sosy_lab.cpachecker.core.algorithm.distributed_summaries.decomposition.graph.BlockGraph;
import org.sosy_lab.cpachecker.core.algorithm.distributed_summaries.decomposition.graph.BlockNode;
import org.sosy_lab.cpachecker.core.interfaces.AbstractState;
import org.sosy_lab.cpachecker.core.reachedset.ReachedSet;
import org.sosy_lab.cpachecker.cpa.block.BlockState;
import org.sosy_lab.cpachecker.cpa.block.BlockState.BlockStateType;
import org.sosy_lab.cpachecker.util.AbstractStates;
import org.sosy_lab.cpachecker.util.CFAUtils;
import org.sosy_lab.cpachecker.util.LoopStructure;
import org.sosy_lab.cpachecker.util.LoopStructure.Loop;

public class BlockSummaryCFAModifier {

  public record Modification(
      CFA cfa, BlockGraph blockGraph, ImmutableSet<CFANode> unableToAbstract) {}

  public static final String UNIQUE_DESCRIPTION = "<<distributed-block-summary-block-end>>";

  public static boolean hasAbstractionOccurred(BlockNode pBlockNode, ReachedSet pReachedSet) {
    if (pBlockNode.getEdges().stream()
        .noneMatch(e -> e.getDescription().equals(UNIQUE_DESCRIPTION))) {
      return false;
    }
    if (pReachedSet.getReached(pBlockNode.getLast()).isEmpty()) {
      return false;
    }
    for (AbstractState abstractState : pReachedSet) {
      if (Objects.requireNonNull(AbstractStates.extractStateByType(abstractState, BlockState.class))
              .getType()
          == BlockStateType.ABSTRACTION) {
        return false;
      }
    }
    return true;
  }

  public static Modification instrumentCFA(
      CFA pCFA, BlockGraph pBlockGraph, LogManager pLogger, ShutdownNotifier pNotifier) {
    if (pBlockGraph.getNodes().size() == 1) {
      return new Modification(pCFA, pBlockGraph, ImmutableSet.of());
    }
    FlexCfaNetwork cfaNetwork = FlexCfaNetwork.copy(pCFA);
    ImmutableSet<CFANode> blockEnds =
        transformedImmutableSetCopy(pBlockGraph.getNodes(), n -> n.getLast());
    ImmutableSet.Builder<CFANode> unableToAbstract = ImmutableSet.builder();
    for (CFANode blockEnd : blockEnds) {
      if (blockEnd.getLeavingSummaryEdge() == null
          && !blockEnd.equals(pCFA.getMainFunction())
          && !(blockEnd instanceof CFATerminationNode)) {
        BlankEdge blockEndEdge =
            new BlankEdge(
                "",
                FileLocation.DUMMY,
                blockEnd,
                new CFANode(blockEnd.getFunction()),
                UNIQUE_DESCRIPTION);
        cfaNetwork.addEdge(blockEndEdge);
      } else {
        if (blockEnd.getLeavingSummaryEdge() != null) {
          unableToAbstract.add(blockEnd);
        }
      }
    }
    CFA instrumentedCFA =
        CCfaFactory.CLONER.createCfa(cfaNetwork, pCFA.getMetadata(), pLogger, pNotifier);
    MappingInformation mapping =
        createMappingBetweenOriginalAndInstrumentedCFA(pCFA, instrumentedCFA);
    Map<CFANode, CFANode> originalInstrumentedMapping = mapping.originalToInstrumentedNodes();
    originalInstrumentedMapping.forEach(
        (n1, n2) -> n2.setReversePostorderId(n1.getReversePostorderId()));
    Optional<LoopStructure> loopStructure;
    if (pCFA.getMetadata().getLoopStructure().isPresent()) {
      LoopStructure extracted = pCFA.getMetadata().getLoopStructure().orElseThrow();
      if (extracted.getCount() == 0) {
        loopStructure = Optional.of(extracted);
      } else {
        ImmutableListMultimap.Builder<String, Loop> loops = ImmutableListMultimap.builder();
        for (String functionName : pCFA.getAllFunctionNames()) {
          outer:
          for (Loop loop : extracted.getLoopsForFunction(functionName)) {
            ImmutableSet.Builder<CFANode> heads = ImmutableSet.builder();
            for (CFANode loopHead : loop.getLoopHeads()) {
              if (!originalInstrumentedMapping.containsKey(loopHead)) {
                continue outer;
              }
              heads.add(originalInstrumentedMapping.get(loopHead));
            }
            ImmutableSet.Builder<CFANode> nodes = ImmutableSet.builder();
            for (CFANode loopNode : loop.getLoopNodes()) {
              if (!originalInstrumentedMapping.containsKey(loopNode)) {
                continue outer;
              }
              nodes.add(originalInstrumentedMapping.get(loopNode));
            }
            loops.put(functionName, new Loop(heads.build(), nodes.build()));
          }
        }
        loopStructure = Optional.of(LoopStructure.of(loops.build()));
      }
    } else {
      loopStructure = Optional.empty();
    }
    CfaMetadata metadata = instrumentedCFA.getMetadata();
    if (loopStructure.isPresent()) {
      metadata = metadata.withLoopStructure(loopStructure.orElseThrow());
    }
    return new Modification(
        new ImmutableCFA(
            instrumentedCFA.getAllFunctions(),
            ImmutableSet.copyOf(instrumentedCFA.getAllNodes()),
            metadata),
        adaptBlockGraph(pBlockGraph, instrumentedCFA.getMainFunction(), mapping),
        unableToAbstract.build());
  }

  private static BlockGraph adaptBlockGraph(
      BlockGraph pBlockGraph,
      CFANode pNewMainFunctionNode,
      MappingInformation pMappingInformation) {
    Map<CFANode, CFANode> originalInstrumentedNodes =
        pMappingInformation.originalToInstrumentedNodes();
    Map<CFAEdge, CFAEdge> originalInstrumentedEdges =
        pMappingInformation.originalToInstrumentedEdges();
    Map<CFANode, CFAEdge> blockAbstractionEnds = pMappingInformation.abstractionEdges();
    ImmutableSet.Builder<BlockNode> instrumentedBlocks = ImmutableSet.builder();
    for (BlockNode block : pBlockGraph.getNodes()) {
      CFAEdge abstractionEdge = blockAbstractionEnds.get(block.getLast());
      ImmutableSet.Builder<CFANode> nodeBuilder = ImmutableSet.builder();
      for (CFANode node : block.getNodes()) {
        // null in case of unreachable node
        CFANode instrumentedNode = originalInstrumentedNodes.get(node);
        if (instrumentedNode != null) {
          nodeBuilder.add(instrumentedNode);
        }
      }
      ImmutableSet.Builder<CFAEdge> edgeBuilder = ImmutableSet.builder();
      for (CFAEdge edge : block.getEdges()) {
        edgeBuilder.add(originalInstrumentedEdges.get(edge));
      }
      CFANode abstraction = block.getLast();
      if (abstractionEdge != null) {
        edgeBuilder.add(abstractionEdge);
        abstraction = abstractionEdge.getSuccessor();
        nodeBuilder.add(abstraction);
      }
      instrumentedBlocks.add(
          new BlockNode(
              block.getId(),
              originalInstrumentedNodes.get(block.getFirst()),
              originalInstrumentedNodes.get(block.getLast()),
              nodeBuilder.build(),
              edgeBuilder.build(),
              block.getPredecessorIds(),
              block.getLoopPredecessorIds(),
              block.getSuccessorIds(),
              abstraction));
    }
    BlockNode root =
        new BlockNode(
            BlockGraph.ROOT_ID,
            pNewMainFunctionNode,
            pNewMainFunctionNode,
            ImmutableSet.of(pNewMainFunctionNode),
            ImmutableSet.of(),
            ImmutableSet.of(),
            pBlockGraph.getRoot().getLoopPredecessorIds(),
            pBlockGraph.getRoot().getSuccessorIds());
    return new BlockGraph(root, instrumentedBlocks.build());
  }

  private record MappingInformation(
      Map<CFANode, CFANode> originalToInstrumentedNodes,
      Map<CFAEdge, CFAEdge> originalToInstrumentedEdges,
      Map<CFANode, CFAEdge> abstractionEdges) {}

  private static MappingInformation createMappingBetweenOriginalAndInstrumentedCFA(
      CFA pOriginal, CFA pInstrumented) {
    record NodePair(CFANode n1, CFANode n2) {}

    Set<CFANode> covered = new LinkedHashSet<>();
    ImmutableMap.Builder<CFANode, CFAEdge> originalToAbstractionInstrumented =
        ImmutableMap.builder();
    ImmutableMap.Builder<CFAEdge, CFAEdge> originalToInstrumentedEdges = ImmutableMap.builder();
    ImmutableMap.Builder<CFANode, CFANode> originalToInstrumentedNodes = ImmutableMap.builder();

    List<NodePair> waitlist = new ArrayList<>();
    waitlist.add(new NodePair(pOriginal.getMainFunction(), pInstrumented.getMainFunction()));
    originalToInstrumentedNodes.put(pOriginal.getMainFunction(), pInstrumented.getMainFunction());
    while (!waitlist.isEmpty()) {
      NodePair pair = waitlist.remove(0);
      CFANode originalNode = pair.n1();
      if (covered.contains(originalNode)) {
        continue;
      }
      covered.add(originalNode);
      CFANode instrumentedNode = pair.n2();
      FluentIterable<CFAEdge> instrumentedOutgoing = CFAUtils.allLeavingEdges(instrumentedNode);
      FluentIterable<CFAEdge> abstractionEdges =
          instrumentedOutgoing.filter(e -> e.getDescription().equals(UNIQUE_DESCRIPTION));
      assertOrFail(
          abstractionEdges.size() <= 1, "Cannot have more than one abstraction edge per node.");
      int outgoing = instrumentedNode.getNumLeavingEdges() - abstractionEdges.size();
      assertOrFail(
          originalNode.getNumLeavingEdges() == outgoing, "Number of leaving edges does not match.");
      assertOrFail(
          originalNode.getNumEnteringEdges() == instrumentedNode.getNumEnteringEdges(),
          "Number of entering edges does not match.");
      abstractionEdges.forEach(edge -> originalToAbstractionInstrumented.put(originalNode, edge));
      FluentIterable<CFAEdge> originalOutgoing = CFAUtils.allLeavingEdges(originalNode);
      Set<CFAEdge> foundCorrespondingEdges = new LinkedHashSet<>();
      for (CFAEdge cfaEdge : originalOutgoing) {
        CFAEdge corresponding = findCorrespondingEdge(cfaEdge, instrumentedOutgoing);
        assertOrFail(
            !foundCorrespondingEdges.contains(corresponding), "Corresponding edge already covered");
        originalToInstrumentedNodes.put(cfaEdge.getSuccessor(), corresponding.getSuccessor());
        originalToInstrumentedEdges.put(cfaEdge, corresponding);
        waitlist.add(new NodePair(cfaEdge.getSuccessor(), corresponding.getSuccessor()));
        foundCorrespondingEdges.add(corresponding);
      }
      assertOrFail(
          foundCorrespondingEdges.size() == instrumentedOutgoing.size() - abstractionEdges.size(),
          "Missing mapping to instrumented edges");
    }

    return new MappingInformation(
        originalToInstrumentedNodes.buildKeepingLast(),
        originalToInstrumentedEdges.buildOrThrow(),
        originalToAbstractionInstrumented.buildOrThrow());
  }

  private static CFAEdge findCorrespondingEdge(CFAEdge edge, Iterable<CFAEdge> edges) {
    for (CFAEdge cfaEdge : edges) {
      if (virtuallyEqual(edge, cfaEdge)) {
        return cfaEdge;
      }
    }
    throw new AssertionError("No matching edge found");
  }

  private static void assertOrFail(boolean condition, String message) {
    if (!condition) {
      throw new AssertionError(message);
    }
  }

  private static boolean virtuallyEqual(CFAEdge pCFAEdge, CFAEdge pCFAEdge2) {
    return pCFAEdge.getDescription().equals(pCFAEdge2.getDescription())
        && pCFAEdge.getEdgeType().equals(pCFAEdge2.getEdgeType())
        && pCFAEdge.getCode().equals(pCFAEdge2.getCode())
        && pCFAEdge.getLineNumber() == pCFAEdge2.getLineNumber()
        && pCFAEdge.getFileLocation().equals(pCFAEdge2.getFileLocation())
        && pCFAEdge.getRawAST().equals(pCFAEdge2.getRawAST());
  }
}
