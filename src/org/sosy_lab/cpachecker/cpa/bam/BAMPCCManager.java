// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2007-2020 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.cpa.bam;

import static org.sosy_lab.cpachecker.util.AbstractStates.extractLocation;

import com.google.common.base.Preconditions;
import com.google.common.collect.HashMultimap;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.Iterables;
import com.google.common.collect.Multimap;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Deque;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.sosy_lab.common.configuration.Configuration;
import org.sosy_lab.common.configuration.InvalidConfigurationException;
import org.sosy_lab.common.configuration.Option;
import org.sosy_lab.common.configuration.Options;
import org.sosy_lab.cpachecker.cfa.blocks.Block;
import org.sosy_lab.cpachecker.cfa.blocks.BlockPartitioning;
import org.sosy_lab.cpachecker.cfa.model.CFAEdge;
import org.sosy_lab.cpachecker.cfa.model.CFANode;
import org.sosy_lab.cpachecker.core.interfaces.AbstractState;
import org.sosy_lab.cpachecker.core.interfaces.Reducer;
import org.sosy_lab.cpachecker.core.interfaces.pcc.ProofChecker;
import org.sosy_lab.cpachecker.cpa.arg.ARGState;
import org.sosy_lab.cpachecker.cpa.bam.cache.BAMDataManager;
import org.sosy_lab.cpachecker.exceptions.CPAException;
import org.sosy_lab.cpachecker.exceptions.CPATransferException;
import org.sosy_lab.cpachecker.util.AbstractStates;
import org.sosy_lab.cpachecker.util.CFAUtils;
import org.sosy_lab.cpachecker.util.Pair;

/**
 * Manipulating proof-carrying code for BAM.
 */
@Options(prefix="pcc.proofgen")
public final class BAMPCCManager {

  @Option(secure=true, description = "Generate and dump a proof")
  private boolean doPCC = false;

  private final ProofChecker wrappedProofChecker;
  private Map<Pair<ARGState, Block>, Collection<ARGState>> correctARGsForBlocks = null;
  private final BlockPartitioning partitioning;
  private final Reducer wrappedReducer;
  private final BAMCPA bamCPA;
  private final BAMDataManager data;

  // Warning: current block depends on current state!
  private Block currentBlock = null;

  public BAMPCCManager(
      ProofChecker pWrappedProofChecker,
      Configuration pConfiguration,
      BlockPartitioning pPartitioning,
      Reducer pWrappedReducer,
      BAMCPA pBamCPA, BAMDataManager pData)
      throws InvalidConfigurationException {
    wrappedProofChecker = pWrappedProofChecker;
    partitioning = pPartitioning;
    wrappedReducer = pWrappedReducer;
    bamCPA = pBamCPA;
    data = pData;
    pConfiguration.inject(this);
  }

  public boolean isPCCEnabled() {
    return doPCC;
  }

  public boolean areAbstractSuccessors(AbstractState pState,
                                       CFAEdge pCfaEdge,
                                       Collection<? extends AbstractState> pSuccessors)
      throws CPATransferException, InterruptedException {
    if (pCfaEdge != null) {
      return wrappedProofChecker.areAbstractSuccessors(pState, pCfaEdge, pSuccessors);
    }
    return areAbstractSuccessors0(pState, pSuccessors, partitioning.getMainBlock());
  }

  private boolean areAbstractSuccessors0(
      AbstractState pState, Collection<? extends AbstractState> pSuccessors, final Block pBlock)
      throws CPATransferException, InterruptedException {
    // currently cannot deal with blocks for which the set of call nodes and return nodes of that block is not disjunct
    boolean successorExists;

    CFANode node = extractLocation(pState);

    if (partitioning.isCallNode(node) && !partitioning.getBlockForCallNode(node).equals(pBlock)) {
      // do not support nodes which are call nodes of multiple blocks
      Block analyzedBlock = partitioning.getBlockForCallNode(node);
      try {
        if (!(pState instanceof BAMARGBlockStartState)
            || ((BAMARGBlockStartState) pState).getAnalyzedBlock() == null
            || !bamCPA.isCoveredBy(wrappedReducer.getVariableReducedStateForProofChecking(pState, analyzedBlock, node),
            ((BAMARGBlockStartState) pState).getAnalyzedBlock())) { return false; }
      } catch (CPAException e) {
        throw new CPATransferException("Missing information about block whose analysis is expected to be started at "
            + pState);
      }
      try {
        Collection<ARGState> endOfBlock;
        Pair<ARGState, Block> key = Pair.of(((BAMARGBlockStartState) pState).getAnalyzedBlock(), analyzedBlock);
        if (correctARGsForBlocks != null && correctARGsForBlocks.containsKey(key)) {
          endOfBlock = correctARGsForBlocks.get(key);
        } else {
          Pair<Boolean, Collection<ARGState>> result =
              checkARGBlock(((BAMARGBlockStartState) pState).getAnalyzedBlock(), analyzedBlock);
          if (!result.getFirst()) { return false; }
          endOfBlock = result.getSecond();
          setCorrectARG(key, endOfBlock);
        }

        Set<AbstractState> notFoundSuccessors = new HashSet<>(pSuccessors);
        AbstractState expandedState;

        Multimap<CFANode, AbstractState> blockSuccessors = HashMultimap.create();
        for (AbstractState absElement : pSuccessors) {
          ARGState successorElem = (ARGState) absElement;
          blockSuccessors.put(extractLocation(absElement), successorElem);
        }


        for (ARGState leaveB : endOfBlock) {
          successorExists = false;
          expandedState = wrappedReducer.getVariableExpandedStateForProofChecking(pState, analyzedBlock, leaveB);
          for (AbstractState next : blockSuccessors.get(extractLocation(leaveB))) {
            if (bamCPA.isCoveredBy(expandedState, next)) {
              successorExists = true;
              notFoundSuccessors.remove(next);
            }
          }
          if (!successorExists) { return false; }
        }

        if (!notFoundSuccessors.isEmpty()) { return false; }

      } catch (CPAException e) {
        throw new CPATransferException(
            "Checking ARG with root "
                + ((BAMARGBlockStartState) pState).getAnalyzedBlock()
                + " for block "
                + pBlock
                + "failed.");
      }
    } else {
      Set<CFAEdge> usedEdges = new HashSet<>();
      for (AbstractState absElement : pSuccessors) {
        ARGState successorElem = (ARGState) absElement;
        usedEdges.add(((ARGState) pState).getEdgeToChild(successorElem));
      }

      //no call node, check if successors can be constructed with help of CFA edges
      for (CFAEdge leavingEdge : CFAUtils.leavingEdges(node)) {
        // edge leads to node in inner block
        Collection<Block> blocks = partitioning.getBlocksForReturnNode(node);
        Preconditions.checkState(
            blocks.size() <= 1, "PCC does not expect more blocks for a single return node");
        Block currentNodeBlock = Iterables.getFirst(blocks, null);
        if (currentNodeBlock != null
            && !pBlock.equals(currentNodeBlock)
            && currentNodeBlock.getNodes().contains(leavingEdge.getSuccessor())) {
          if (usedEdges.contains(leavingEdge)) { return false; }
          continue;
        }
        // edge leaves block, do not analyze, check for call node since if call node is also return
        // node analysis will go beyond current block
        if (!pBlock.isCallNode(node)
            && pBlock.isReturnNode(node)
            && !pBlock.getNodes().contains(leavingEdge.getSuccessor())) {
          if (usedEdges.contains(leavingEdge)) { return false; }
          continue;
        }
        if (!wrappedProofChecker.areAbstractSuccessors(pState, leavingEdge, pSuccessors)) {
          return false;
        }
      }
    }
    return true;
  }

  private Pair<Boolean, Collection<ARGState>> checkARGBlock(ARGState rootNode, final Block pBlock)
      throws CPAException, InterruptedException {
    Collection<ARGState> returnNodes = new ArrayList<>();
    Set<ARGState> waitingForUnexploredParents = new HashSet<>();
    boolean unexploredParent;
    Deque<ARGState> waitlist = new ArrayDeque<>();
    Set<ARGState> visited = new HashSet<>();
    Set<ARGState> coveredNodes = new HashSet<>();
    ARGState current;

    waitlist.add(rootNode);
    visited.add(rootNode);

    while (!waitlist.isEmpty()) {
      current = waitlist.pop();

      if (current.isTarget()) {
        returnNodes.add(current);
      }

      if (current.isCovered()) {
        coveredNodes.clear();
        coveredNodes.add(current);
        do {
          if (!bamCPA.isCoveredBy(current, current.getCoveringState())) {
            returnNodes = ImmutableList.of();
            return Pair.of(false, returnNodes);
          }
          coveredNodes.add(current);
          if (coveredNodes.contains(current.getCoveringState())) {
            returnNodes = ImmutableList.of();
            return Pair.of(false, returnNodes);
          }
          current = current.getCoveringState();
        } while (current.isCovered());

        if (!visited.contains(current)) {
          unexploredParent = false;
          for (ARGState p : current.getParents()) {
            if (!visited.contains(p) || waitlist.contains(p)) {
              waitingForUnexploredParents.add(current);
              unexploredParent = true;
              break;
            }
          }
          if (!unexploredParent) {
            visited.add(current);
            waitlist.add(current);
          }
        }
        continue;
      }

      CFANode node = extractLocation(current);
      if (pBlock.isReturnNode(node)) {
        returnNodes.add(current);
      }

      if (!areAbstractSuccessors0(current, current.getChildren(), pBlock)) {
        returnNodes = ImmutableList.of();
        return Pair.of(false, returnNodes);
      }

      for (ARGState child : current.getChildren()) {
        unexploredParent = false;
        for (ARGState p : child.getParents()) {
          if (!visited.contains(p) || waitlist.contains(p)) {
            waitingForUnexploredParents.add(child);
            unexploredParent = true;
            break;
          }
        }
        if (unexploredParent) {
          continue;
        }
        if (visited.contains(child)) {
          returnNodes = ImmutableList.of();
          return Pair.of(false, returnNodes);
        } else {
          waitingForUnexploredParents.remove(child);
          visited.add(child);
          waitlist.add(child);
        }
      }

    }
    if (!waitingForUnexploredParents.isEmpty()) {
      returnNodes = ImmutableList.of();
      return Pair.of(false, returnNodes);
    }
    return Pair.of(true, returnNodes);
  }

  public void setCorrectARG(Pair<ARGState, Block> pKey, Collection<ARGState>
      pEndOfBlock) {
    if (correctARGsForBlocks == null) {
      correctARGsForBlocks = new HashMap<>();
    }
    correctARGsForBlocks.put(pKey, pEndOfBlock);
  }

  /**
   * Attach PCC-specific information to successors.
   */
  Collection<? extends AbstractState> attachAdditionalInfoToCallNodes(
      Collection<? extends AbstractState> pSuccessors) {
    List<AbstractState> successorsWithExtendedInfo = new ArrayList<>(pSuccessors.size());
    for (AbstractState elem : pSuccessors) {
      if (!(elem instanceof ARGState)) { return pSuccessors; }
      if (!(elem instanceof BAMARGBlockStartState)) {
        successorsWithExtendedInfo.add(createAdditionalInfo((ARGState) elem));
      } else {
        successorsWithExtendedInfo.add(elem);
      }
    }
    return successorsWithExtendedInfo;
  }

  AbstractState attachAdditionalInfoToCallNode(AbstractState pElem) {
    if (!(pElem instanceof BAMARGBlockStartState)
        && pElem instanceof ARGState) {
      return createAdditionalInfo((ARGState) pElem);
    }
    return pElem;
  }

  private ARGState createAdditionalInfo(ARGState pElem) {
    CFANode node = AbstractStates.extractLocation(pElem);
    if (partitioning.isCallNode(node) &&
        !partitioning.getBlockForCallNode(node).equals(currentBlock)) {
      BAMARGBlockStartState replaceWith = new BAMARGBlockStartState(pElem.getWrappedState(), null);
      replaceInARG(pElem, replaceWith);
      return replaceWith;
    }
    return pElem;
  }

  private void replaceInARG(ARGState toReplace, ARGState replaceWith) {
    if (toReplace.isCovered()) {
      replaceWith.setCovered(toReplace.getCoveringState());
    }
    toReplace.uncover();

    toReplace.replaceInARGWith(replaceWith);
  }

  @SuppressWarnings("deprecation")
  void addBlockAnalysisInfo(AbstractState pElement) throws CPATransferException {
    if (data.getCache().getLastAnalyzedBlock() == null || !(pElement instanceof BAMARGBlockStartState)) {
      throw new CPATransferException("Cannot build proof, ARG, for BAM analysis.");
    }
    ((BAMARGBlockStartState) pElement).setAnalyzedBlock(data.getCache().getLastAnalyzedBlock());
  }

  void setCurrentBlock(Block pCurrentBlock) {
    currentBlock = pCurrentBlock;
  }
}
