// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2023 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.core.algorithm.distributed_summaries.decomposition.graph;

import static com.google.common.base.Preconditions.checkState;
import static org.sosy_lab.common.collect.Collections3.transformedImmutableSetCopy;
import static org.sosy_lab.cpachecker.core.algorithm.distributed_summaries.decomposition.graph.BlockGraph.GHOST_EDGE_DESCRIPTION;

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
import java.util.Optional;
import java.util.Set;
import java.util.TreeMap;
import org.sosy_lab.common.configuration.Configuration;
import org.sosy_lab.common.log.LogManager;
import org.sosy_lab.cpachecker.cfa.CCfaTransformer;
import org.sosy_lab.cpachecker.cfa.CFA;
import org.sosy_lab.cpachecker.cfa.CfaMetadata;
import org.sosy_lab.cpachecker.cfa.MutableCFA;
import org.sosy_lab.cpachecker.cfa.ast.FileLocation;
import org.sosy_lab.cpachecker.cfa.model.BlankEdge;
import org.sosy_lab.cpachecker.cfa.model.CFAEdge;
import org.sosy_lab.cpachecker.cfa.model.CFANode;
import org.sosy_lab.cpachecker.cfa.model.CFATerminationNode;
import org.sosy_lab.cpachecker.cfa.model.FunctionEntryNode;
import org.sosy_lab.cpachecker.cfa.model.FunctionExitNode;
import org.sosy_lab.cpachecker.cfa.model.c.CAssumeEdge;
import org.sosy_lab.cpachecker.cfa.model.c.CDeclarationEdge;
import org.sosy_lab.cpachecker.cfa.model.c.CFunctionCallEdge;
import org.sosy_lab.cpachecker.cfa.model.c.CFunctionEntryNode;
import org.sosy_lab.cpachecker.cfa.model.c.CFunctionSummaryStatementEdge;
import org.sosy_lab.cpachecker.cfa.model.c.CReturnStatementEdge;
import org.sosy_lab.cpachecker.cfa.model.c.CStatementEdge;
import org.sosy_lab.cpachecker.util.CFAUtils;
import org.sosy_lab.cpachecker.util.LoopStructure;
import org.sosy_lab.cpachecker.util.LoopStructure.Loop;

public class BlockGraphModification {

  private BlockGraphModification() {}

  /**
   * Block-graph modification for a {@link CFA}. This is a modification of an original CFA that was
   * adjusted to fulfill certain requirements with regard to a block graph. For example, one
   * modification is that every block ends with a CFA node with a single outgoing edge. In the
   * process of CFA modification, the original block graph must also be adjusted to reflect the
   * changes in the CFA.
   *
   * @param cfa modified CFA
   * @param blockGraph modified block graph
   * @param metadata metadata about the modification
   */
  public record Modification(CFA cfa, BlockGraph blockGraph, ModificationMetadata metadata) {}

  /**
   * Metadata about a modification of a CFA and its block graph.
   *
   * @param originalCfa CFA that was modified
   * @param originalBlockGraph block graph that was used as reference for modification
   * @param mappingInfo mapping information between original and modified CFA
   * @param unableToAbstract CFA nodes for which no abstraction node could be added
   * @param abstractions abstraction nodes that were added to the CFA. An abstraction node is a new,
   *     last node of a block that has a single ingoing, blank edge.
   */
  public record ModificationMetadata(
      CFA originalCfa,
      BlockGraph originalBlockGraph,
      MappingInformation mappingInfo,
      ImmutableSet<CFANode> unableToAbstract,
      ImmutableMap<CFANode, CFAEdge> abstractions) {}

  private record MappingInformation(
      Map<CFANode, CFANode> originalToInstrumentedNodes,
      Map<CFAEdge, CFAEdge> originalToInstrumentedEdges) {}

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
    // If the block graph consists of a single block that is the full CFA,
    // the 'block graph' is trivial, and we do not require any modifications.
    if (pBlockGraph.getNodes().size() == 1) {
      return getUnchanged(pCFA, pBlockGraph);
    }

    MutableCFA mutableCfa = createMutableCfaCopy(pCFA, pConfig, pLogger);
    ModificationMetadata modificationMetadata =
        addBlankEdgesAtBlockEnds(mutableCfa, pCFA, pBlockGraph);
    Map<CFANode, CFANode> originalInstrumentedMapping =
        modificationMetadata.mappingInfo().originalToInstrumentedNodes();
    // Adjust the block graph to the modified CFA
    BlockGraph adjustedBlockGraph =
        adaptBlockGraph(
            pBlockGraph,
            mutableCfa.getMainFunction(),
            modificationMetadata.mappingInfo(),
            modificationMetadata.abstractions());

    // Adjust metadata to the modified CFA
    setReversePostorderForInstrumentedNodes(originalInstrumentedMapping);
    Optional<LoopStructure> adjustedLoopStructure =
        adjustLoopStructureToModification(pCFA, originalInstrumentedMapping);

    CfaMetadata metadata = mutableCfa.getMetadata();
    if (adjustedLoopStructure.isPresent()) {
      metadata = metadata.withLoopStructure(adjustedLoopStructure.orElseThrow());
    }
    mutableCfa.setMetadata(metadata);

    return new Modification(mutableCfa.immutableCopy(), adjustedBlockGraph, modificationMetadata);
  }

  private static Modification getUnchanged(CFA pCFA, BlockGraph pBlockGraph) {
    ImmutableMap<CFANode, CFANode> identityMapping =
        pCFA.nodes().stream()
            .map(n -> Map.entry(n, n))
            .collect(ImmutableMap.toImmutableMap(Map.Entry::getKey, Map.Entry::getValue));
    ModificationMetadata metadata =
        new ModificationMetadata(
            pCFA,
            pBlockGraph,
            new MappingInformation(identityMapping, ImmutableMap.of()),
            // no abstraction happened, so no issues occurred
            /* unableToAbstract= */ ImmutableSet.of(),
            // no abstraction happened
            /* abstractions= */ ImmutableMap.of());
    return new Modification(pCFA, pBlockGraph, metadata);
  }

  private static void setReversePostorderForInstrumentedNodes(
      Map<CFANode, CFANode> originalInstrumentedMapping) {
    originalInstrumentedMapping.forEach(
        (n1, n2) -> n2.setReversePostorderId(n1.getReversePostorderId()));
  }

  private static Optional<LoopStructure> adjustLoopStructureToModification(
      CFA pCFA, Map<CFANode, CFANode> originalInstrumentedMapping) {
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
            loops.put(functionName, Loop.fromLoopHeadsAndNodes(heads.build(), nodes.build()));
          }
        }
        loopStructure = Optional.of(LoopStructure.of(loops.build()));
      }
    } else {
      loopStructure = Optional.empty();
    }
    return loopStructure;
  }

  private static CFAEdge insertGhostEdge(CFANode pPredecessor, CFANode pSuccessor) {
    return new BlankEdge("", FileLocation.DUMMY, pPredecessor, pSuccessor, GHOST_EDGE_DESCRIPTION);
  }

  private static CFAEdge cloneEdge(CFAEdge pEdge, CFANode pStart, CFANode pEnd) {
    final FileLocation loc = pEdge.getFileLocation();
    final String rawStatement = pEdge.getRawStatement();
    switch (pEdge.getEdgeType()) {
      case BlankEdge -> {
        return new BlankEdge(rawStatement, loc, pStart, pEnd, pEdge.getDescription());
      }
      case AssumeEdge -> {
        if (!(pEdge instanceof CAssumeEdge e)) {
          throw new AssertionError("Expected C program, but got edge " + pEdge);
        } else {
          return new CAssumeEdge(
              rawStatement,
              loc,
              pStart,
              pEnd,
              e.getExpression(),
              e.getTruthAssumption(),
              e.isSwapped(),
              e.isArtificialIntermediate());
        }
      }
      case StatementEdge -> {
        if (pEdge instanceof CFunctionSummaryStatementEdge) {
          throw new IllegalStateException(
              "Violated assumption in block-graph decomposition of DSS: trying to change the"
                  + " structure of function calls, but this should not be necessary. Edge: "
                  + pEdge);
        } else if (!(pEdge instanceof CStatementEdge e)) {
          throw new AssertionError("Expected C program, but got edge " + pEdge);
        } else {
          return new CStatementEdge(rawStatement, e.getStatement(), loc, pStart, pEnd);
        }
      }
      case DeclarationEdge -> {
        if (!(pEdge instanceof CDeclarationEdge e)) {
          throw new AssertionError("Expected C program, but got edge " + pEdge);
        } else {
          return new CDeclarationEdge(rawStatement, loc, pStart, pEnd, e.getDeclaration());
        }
      }
      case ReturnStatementEdge -> {
        if (!(pEdge instanceof CReturnStatementEdge e)) {
          throw new AssertionError("Expected C program, but got edge " + pEdge);
        } else if (!(pEnd instanceof FunctionExitNode exitNode)) {
          throw new AssertionError(
              "Expected exit node to be " + FunctionExitNode.class + " but got " + pEnd.getClass());
        } else {
          return new CReturnStatementEdge(
              rawStatement, e.getReturnStatement(), loc, pStart, exitNode);
        }
      }
      case FunctionCallEdge -> {
        if (!(pEdge instanceof CFunctionCallEdge e)) {
          throw new AssertionError("Expected C program, but got edge " + pEdge);
        } else if (!(pEnd instanceof CFunctionEntryNode entryNode)) {
          throw new AssertionError(
              "Expected entry node to be "
                  + CFunctionEntryNode.class
                  + " but got "
                  + pEnd.getClass());
        } else {
          return new CFunctionCallEdge(
              rawStatement, loc, pStart, entryNode, e.getFunctionCall(), e.getSummaryEdge());
        }
      }

      default ->
          throw new AssertionError(
              "Unexpected edge: "
                  + pEdge
                  + " of type "
                  + pEdge.getClass()
                  + "\nStart node: "
                  + pStart.getClass()
                  + "\nEnd node: "
                  + pEdge.getSuccessor().getClass()
                  + "\nEntering summary edge: "
                  + pEdge.getSuccessor().getEnteringSummaryEdge());
    }
  }

  private static boolean mustAddGhostEdgeBeforeLastEdge(CFANode blockEnd) {
    return blockEnd instanceof FunctionEntryNode
        || (blockEnd instanceof FunctionExitNode
            && blockEnd.getNumEnteringEdges() == 1
            && blockEnd.getEnteringEdge(0) instanceof CReturnStatementEdge);
  }

  /**
   * For each CFA node that ends in a block, we add a new node that is the successor of the original
   * node and that has a single ingoing, blank edge. This is required so that predicate abstraction
   * can be applied at every block end without issues. TODO: Explain reason better. Example:
   *
   * <pre>
   * .......... Block 1
   * . (0)    .
   * .  | x=0 .
   * . (1)    .
   * ...|...... Block 2
   * . (2)    .
   * .  | x++ .
   * . (3)    .
   * ..........
   * </pre>
   *
   * is modified to:
   *
   * <pre>
   * .......... Block 1
   * . (0)    .
   * .  | x=0 .
   * . (1)    .
   * .  | nop .
   * . (1')   .
   * ...|...... Block 2
   * . (2)    .
   * .  | x++ .
   * . (3')    .
   * .  | nop .
   * . (3)   .
   * ..........
   * </pre>
   *
   * @param pMutableCfa mutable CFA
   * @param pOriginalCfa original CFA
   * @param pBlockGraph block graph to use as reference for modification
   */
  private static ModificationMetadata addBlankEdgesAtBlockEnds(
      MutableCFA pMutableCfa, CFA pOriginalCfa, BlockGraph pBlockGraph) {
    MappingInformation blockMapping =
        createMappingBetweenOriginalAndInstrumentedCFA(pOriginalCfa, pMutableCfa);
    ImmutableSet<CFANode> blockEnds =
        transformedImmutableSetCopy(pBlockGraph.getNodes(), n -> n.getLast());
    ImmutableSet.Builder<CFANode> unableToAbstract = ImmutableSet.builder();
    ImmutableMap.Builder<CFANode, CFAEdge> abstractions = ImmutableMap.builder();
    for (CFANode originalBlockEnd : blockEnds) {
      CFANode mutableCfaBlockEnd = blockMapping.originalToInstrumentedNodes().get(originalBlockEnd);
      checkState(
          mutableCfaBlockEnd.getClass().equals(originalBlockEnd.getClass()),
          "Node types at block end do not match: "
              + originalBlockEnd.getClass()
              + " vs. "
              + mutableCfaBlockEnd.getClass());
      if (mutableCfaBlockEnd.getLeavingSummaryEdge() == null
          && !mutableCfaBlockEnd.equals(pOriginalCfa.getMainFunction())
          && !(mutableCfaBlockEnd instanceof CFATerminationNode)) {
        final CFAEdge ghostEdge;
        if (mustAddGhostEdgeBeforeLastEdge(mutableCfaBlockEnd)) {
          List<CFANode> originalNodesBeforeBlockEnd =
              CFAUtils.enteringEdges(mutableCfaBlockEnd)
                  .transform(e -> e.getPredecessor())
                  .toList();

          for (CFANode originalNodeBeforeBlockEnd : originalNodesBeforeBlockEnd) {
            checkState(
                originalNodeBeforeBlockEnd.getNumLeavingEdges() == 1,
                "Violated assumption in block-graph decomposition of DSS: CFA node just before end"
                    + " of block has more than one leaving edges: %s",
                CFAUtils.leavingEdges(originalNodeBeforeBlockEnd));
            CFAEdge lastEdge = originalNodeBeforeBlockEnd.getLeavingEdge(0);

            CFANode newNodeBeforeBlockEnd = new CFANode(mutableCfaBlockEnd.getFunction());
            pMutableCfa.addNode(newNodeBeforeBlockEnd);
            CFAEdge newFinalEdge = cloneEdge(lastEdge, newNodeBeforeBlockEnd, mutableCfaBlockEnd);
            newNodeBeforeBlockEnd.addLeavingEdge(newFinalEdge);
            mutableCfaBlockEnd.addEnteringEdge(newFinalEdge);
            originalNodeBeforeBlockEnd.removeLeavingEdge(lastEdge);
            mutableCfaBlockEnd.removeEnteringEdge(lastEdge);
          }
          // ... so insert ghost edge not at block end, but one before that.
          ghostEdge = insertGhostEdge(originalNodeBeforeBlockEnd, newNodeBeforeBlockEnd);
          originalNodeBeforeBlockEnd.addLeavingEdge(ghostEdge);
          newNodeBeforeBlockEnd.addEnteringEdge(ghostEdge);

        } else {
          CFANode newNodeBeforeBlockEnd = new CFANode(mutableCfaBlockEnd.getFunction());
          List<CFANode> originalNodesBeforeBlockEnd =
              CFAUtils.enteringEdges(mutableCfaBlockEnd)
                  .transform(e -> e.getPredecessor())
                  .toList();
          for (CFANode n : originalNodesBeforeBlockEnd) {
            checkState(
                n.getNumLeavingEdges() == 1,
                "Violated assumption in block-graph decomposition of DSS: CFA node just before end"
                    + " of block has more than one leaving edges: %s",
                CFAUtils.leavingEdges(n));
            CFAEdge e = n.getLeavingEdge(0);
            CFAEdge edgeToNewNode = cloneEdge(e, n, newNodeBeforeBlockEnd);
            n.removeLeavingEdge(e);
            n.addLeavingEdge(edgeToNewNode);
            newNodeBeforeBlockEnd.addEnteringEdge(edgeToNewNode);
          }

          ghostEdge = insertGhostEdge(newNodeBeforeBlockEnd, mutableCfaBlockEnd);
          newNodeBeforeBlockEnd.addLeavingEdge(ghostEdge);
          mutableCfaBlockEnd.addEnteringEdge(ghostEdge);
        }
        abstractions.put(originalBlockEnd, ghostEdge);
      } else {
        if (mutableCfaBlockEnd.getLeavingSummaryEdge() != null) {
          unableToAbstract.add(mutableCfaBlockEnd);
        }
      }
    }
    return new ModificationMetadata(
        pOriginalCfa,
        pBlockGraph,
        blockMapping,
        unableToAbstract.build(),
        abstractions.buildOrThrow());
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

  private static CFAEdge findCorrespondingEdge(CFAEdge pEdge, Iterable<CFAEdge> edges) {
    for (CFAEdge cfaEdge : edges) {
      if (virtuallyEqual(pEdge, cfaEdge)) {
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
