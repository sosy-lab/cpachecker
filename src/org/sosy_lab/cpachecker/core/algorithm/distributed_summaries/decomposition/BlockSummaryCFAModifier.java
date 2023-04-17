// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2023 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.core.algorithm.distributed_summaries.decomposition;

import static org.sosy_lab.common.collect.Collections3.transformedImmutableSetCopy;

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

  private record InstrumentedMapping(
      Map<CFANode, CFANode> mapping, Map<CFANode, CFANode> abstractionNodes) {}

  public record Modification(CFA cfa, BlockGraph blockGraph) {}

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
    FlexCfaNetwork cfaNetwork = FlexCfaNetwork.copy(pCFA);
    ImmutableSet<CFANode> blockEnds =
        transformedImmutableSetCopy(pBlockGraph.getNodes(), n -> n.getLast());
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
      }
    }
    CFA instrumentedCFA =
        CCfaFactory.CLONER.createCfa(cfaNetwork, pCFA.getMetadata(), pLogger, pNotifier);
    InstrumentedMapping mapping =
        createMappingBetweenOriginalAndInstrumentedCFA(pCFA, instrumentedCFA);
    Map<CFANode, CFANode> originalInstrumentedMapping = mapping.mapping();
    Map<CFANode, CFANode> blockAbstractionEnds = mapping.abstractionNodes();
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
          for (Loop loop : extracted.getLoopsForFunction(functionName)) {
            ImmutableSet.Builder<CFANode> heads = ImmutableSet.builder();
            for (CFANode loopHead : loop.getLoopHeads()) {
              heads.add(originalInstrumentedMapping.get(loopHead));
            }
            ImmutableSet.Builder<CFANode> nodes = ImmutableSet.builder();
            for (CFANode loopNode : loop.getLoopNodes()) {
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
    ImmutableSet.Builder<BlockNode> instrumentedBlocks = ImmutableSet.builder();
    for (BlockNode node : pBlockGraph.getNodes()) {
      ImmutableSet.Builder<CFANode> instrumentedCFANodes = ImmutableSet.builder();
      for (CFANode cfaNode : node.getNodes()) {
        instrumentedCFANodes.add(originalInstrumentedMapping.get(cfaNode));
      }
      CFANode abstraction = blockAbstractionEnds.get(node.getLast());
      abstraction =
          abstraction == null ? originalInstrumentedMapping.get(node.getLast()) : abstraction;
      instrumentedCFANodes.add(abstraction);
      ImmutableSet.Builder<CFAEdge> instrumentedCFAEdges = ImmutableSet.builder();
      for (CFAEdge edge : node.getEdges()) {
        CFANode predecessor = originalInstrumentedMapping.get(edge.getPredecessor());
        for (CFAEdge leavingEdge : CFAUtils.allLeavingEdges(predecessor)) {
          if (leavingEdge
              .getSuccessor()
              .equals(originalInstrumentedMapping.get(edge.getSuccessor()))) {
            instrumentedCFAEdges.add(leavingEdge);
            continue;
          }
        }
      }
      instrumentedBlocks.add(
          new BlockNode(
              node.getId(),
              originalInstrumentedMapping.get(node.getFirst()),
              originalInstrumentedMapping.get(node.getLast()),
              instrumentedCFANodes.build(),
              instrumentedCFAEdges.build(),
              node.getPredecessorIds(),
              node.getLoopPredecessorIds(),
              node.getSuccessorIds(),
              abstraction));
    }
    instrumentedBlocks.add(
        new BlockNode(
            BlockGraph.ROOT_ID,
            instrumentedCFA.getMainFunction(),
            instrumentedCFA.getMainFunction(),
            ImmutableSet.of(instrumentedCFA.getMainFunction()),
            ImmutableSet.of(),
            ImmutableSet.of(),
            pBlockGraph.getRoot().getLoopPredecessorIds(),
            pBlockGraph.getRoot().getSuccessorIds()));
    return new Modification(
        new ImmutableCFA(
            instrumentedCFA.getAllFunctions(),
            ImmutableSet.copyOf(instrumentedCFA.getAllNodes()),
            metadata),
        new BlockGraph(instrumentedBlocks.build()));
  }

  private static InstrumentedMapping createMappingBetweenOriginalAndInstrumentedCFA(
      CFA pOriginal, CFA pInstrumented) {
    record CFANodePair(CFANode start, CFANode instrumented) {}
    ImmutableMap.Builder<CFANode, CFANode> builder = ImmutableMap.builder();
    CFANode originalStart = pOriginal.getMainFunction();
    CFANode instrumentedStart = pInstrumented.getMainFunction();
    List<CFANodePair> waitlist = new ArrayList<>();
    waitlist.add(new CFANodePair(originalStart, instrumentedStart));
    Set<CFANodePair> covered = new LinkedHashSet<>();
    ImmutableMap.Builder<CFANode, CFANode> blockEndToAbstractionBuilder = ImmutableMap.builder();
    while (!waitlist.isEmpty()) {
      CFANodePair curr = waitlist.remove(0);
      if (covered.contains(curr)) {
        continue;
      }
      covered.add(curr);
      builder.put(curr.start(), curr.instrumented());
      for (CFAEdge leavingStartEdge : CFAUtils.leavingEdges(curr.start)) {
        for (CFAEdge leavingInstrumentedEdge : CFAUtils.leavingEdges(curr.instrumented())) {
          if (leavingInstrumentedEdge.getDescription().equals(UNIQUE_DESCRIPTION)) {
            blockEndToAbstractionBuilder.put(
                leavingInstrumentedEdge.getPredecessor(), leavingInstrumentedEdge.getSuccessor());
            continue;
          }
          if (virtuallyEqual(leavingStartEdge, leavingInstrumentedEdge)) {
            waitlist.add(
                new CFANodePair(
                    leavingStartEdge.getSuccessor(), leavingInstrumentedEdge.getSuccessor()));
            break;
          }
        }
      }
    }
    return new InstrumentedMapping(
        builder.buildOrThrow(), blockEndToAbstractionBuilder.buildOrThrow());
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
