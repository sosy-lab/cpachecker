// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2023 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.core.algorithm.distributed_summaries.decomposition.graph;

import static org.sosy_lab.common.collect.Collections3.transformedImmutableSetCopy;

import com.google.common.collect.FluentIterable;
import com.google.common.collect.ImmutableListMultimap;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.TreeMultimap;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.NavigableMap;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.TreeMap;
import org.sosy_lab.common.configuration.Configuration;
import org.sosy_lab.common.log.LogManager;
import org.sosy_lab.cpachecker.cfa.CCfaTransformer;
import org.sosy_lab.cpachecker.cfa.CFA;
import org.sosy_lab.cpachecker.cfa.CfaMetadata;
import org.sosy_lab.cpachecker.cfa.ImmutableCFA;
import org.sosy_lab.cpachecker.cfa.MutableCFA;
import org.sosy_lab.cpachecker.cfa.model.CFAEdge;
import org.sosy_lab.cpachecker.cfa.model.CFANode;
import org.sosy_lab.cpachecker.cfa.model.CFATerminationNode;
import org.sosy_lab.cpachecker.cfa.model.FunctionEntryNode;
import org.sosy_lab.cpachecker.cfa.model.GhostEdge;
import org.sosy_lab.cpachecker.core.interfaces.AbstractState;
import org.sosy_lab.cpachecker.core.reachedset.ReachedSet;
import org.sosy_lab.cpachecker.cpa.block.BlockState;
import org.sosy_lab.cpachecker.cpa.block.BlockState.BlockStateType;
import org.sosy_lab.cpachecker.util.AbstractStates;
import org.sosy_lab.cpachecker.util.CFAUtils;
import org.sosy_lab.cpachecker.util.LoopStructure;
import org.sosy_lab.cpachecker.util.LoopStructure.Loop;

public class BlockGraphModification {

  private BlockGraphModification() {}

  public record Modification(
      CFA cfa, BlockGraph blockGraph, ImmutableSet<CFANode> unableToAbstract) {}

  public static boolean hasAbstractionOccurred(BlockNode pBlockNode, ReachedSet pReachedSet) {
    if (pBlockNode.getEdges().stream().noneMatch(e -> e instanceof GhostEdge)) {
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

  private static MutableCFA createMutableCfaCopy(
      CFA pCfa, Configuration pConfig, LogManager pLogger) {
    // create a clone of the specified CFA (clones all CFA nodes and edges)
    CFA clone =
        CCfaTransformer.substituteAstNodes(pConfig, pLogger, pCfa, (cfaEdge, astNode) -> astNode);
    // create a `MutableCFA` for the clone (contains the same CFA nodes and edges as `clone`)
    NavigableMap<String, FunctionEntryNode> functionEntryNodes = new TreeMap<>();
    TreeMultimap<String, CFANode> allNodes = TreeMultimap.create();
    for (CFANode node : clone.nodes()) {
      String functionName = node.getFunction().getQualifiedName();
      allNodes.put(functionName, node);
      if (node instanceof FunctionEntryNode) {
        functionEntryNodes.put(functionName, (FunctionEntryNode) node);
      }
    }
    return new MutableCFA(functionEntryNodes, allNodes, clone.getMetadata());
  }

  public static Modification instrumentCFA(
      CFA pCFA, BlockGraph pBlockGraph, Configuration pConfig, LogManager pLogger) {
    if (pBlockGraph.getNodes().size() == 1) {
      return new Modification(pCFA, pBlockGraph, ImmutableSet.of());
    }
    MutableCFA mutableCfa = createMutableCfaCopy(pCFA, pConfig, pLogger);
    MappingInformation blockMapping =
        createMappingBetweenOriginalAndInstrumentedCFA(pCFA, mutableCfa);
    ImmutableSet<CFANode> blockEnds =
        transformedImmutableSetCopy(pBlockGraph.getNodes(), n -> n.getLast());
    ImmutableSet.Builder<CFANode> unableToAbstract = ImmutableSet.builder();
    ImmutableMap.Builder<CFANode, CFAEdge> abstractions = ImmutableMap.builder();
    for (CFANode originalBlockEnd : blockEnds) {
      CFANode mutableCfaBlockEnd = blockMapping.originalToInstrumentedNodes().get(originalBlockEnd);
      if (mutableCfaBlockEnd.getLeavingSummaryEdge() == null
          && !mutableCfaBlockEnd.equals(pCFA.getMainFunction())
          && !(mutableCfaBlockEnd instanceof CFATerminationNode)) {
        CFANode blockEndEdgeSuccessor = new CFANode(mutableCfaBlockEnd.getFunction());
        mutableCfa.addNode(blockEndEdgeSuccessor);
        GhostEdge blockEndEdge = new GhostEdge(mutableCfaBlockEnd, blockEndEdgeSuccessor);
        mutableCfaBlockEnd.addLeavingEdge(blockEndEdge);
        blockEndEdgeSuccessor.addEnteringEdge(blockEndEdge);
        abstractions.put(originalBlockEnd, blockEndEdge);
      } else {
        if (mutableCfaBlockEnd.getLeavingSummaryEdge() != null) {
          unableToAbstract.add(mutableCfaBlockEnd);
        }
      }
    }
    CFA instrumentedCFA = mutableCfa.immutableCopy();
    Map<CFANode, CFANode> originalInstrumentedMapping = blockMapping.originalToInstrumentedNodes();
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
            ImmutableSet.copyOf(instrumentedCFA.nodes()),
            metadata),
        adaptBlockGraph(
            pBlockGraph,
            instrumentedCFA.getMainFunction(),
            blockMapping,
            abstractions.buildOrThrow()),
        unableToAbstract.build());
  }

  private static BlockGraph adaptBlockGraph(
      BlockGraph pBlockGraph,
      CFANode pNewMainFunctionNode,
      MappingInformation pMappingInformation,
      Map<CFANode, CFAEdge> blockAbstractionEnds) {
    Map<CFANode, CFANode> originalInstrumentedNodes =
        pMappingInformation.originalToInstrumentedNodes();
    Map<CFAEdge, CFAEdge> originalInstrumentedEdges =
        pMappingInformation.originalToInstrumentedEdges();
    ImmutableSet.Builder<BlockNode> instrumentedBlocks = ImmutableSet.builder();
    for (BlockNode block : pBlockGraph.getNodes()) {
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
        CFAEdge instrumentedEdge = originalInstrumentedEdges.get(edge);
        if (instrumentedEdge != null) {
          edgeBuilder.add(instrumentedEdge);
        }
      }
      CFANode abstraction = originalInstrumentedNodes.get(block.getLast());
      CFAEdge abstractionEdge = blockAbstractionEnds.get(block.getLast());
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
      Map<CFAEdge, CFAEdge> originalToInstrumentedEdges) {}

  private static MappingInformation createMappingBetweenOriginalAndInstrumentedCFA(
      CFA pOriginal, CFA pInstrumented) {
    record NodePair(CFANode n1, CFANode n2) {}

    Set<CFANode> covered = new LinkedHashSet<>();
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
    }
    return new MappingInformation(
        originalToInstrumentedNodes.buildKeepingLast(), originalToInstrumentedEdges.buildOrThrow());
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
