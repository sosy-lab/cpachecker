/*
 *  CPAchecker is a tool for configurable software verification.
 *  This file is part of CPAchecker.
 *
 *  Copyright (C) 2007-2014  Dirk Beyer
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
package org.sosy_lab.cpachecker.cpa.bam;

import static org.sosy_lab.cpachecker.util.AbstractStates.*;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.Stack;
import java.util.logging.Level;

import org.sosy_lab.common.log.LogManager;
import org.sosy_lab.common.Pair;
import org.sosy_lab.common.configuration.Configuration;
import org.sosy_lab.common.configuration.InvalidConfigurationException;
import org.sosy_lab.common.configuration.Option;
import org.sosy_lab.common.configuration.Options;
import org.sosy_lab.common.time.Timer;
import org.sosy_lab.cpachecker.cfa.blocks.Block;
import org.sosy_lab.cpachecker.cfa.blocks.BlockPartitioning;
import org.sosy_lab.cpachecker.cfa.model.CFAEdge;
import org.sosy_lab.cpachecker.cfa.model.CFANode;
import org.sosy_lab.cpachecker.cfa.model.FunctionEntryNode;
import org.sosy_lab.cpachecker.core.ShutdownNotifier;
import org.sosy_lab.cpachecker.core.algorithm.CPAAlgorithm;
import org.sosy_lab.cpachecker.core.algorithm.CPAAlgorithm.CPAAlgorithmFactory;
import org.sosy_lab.cpachecker.core.interfaces.AbstractState;
import org.sosy_lab.cpachecker.core.interfaces.Precision;
import org.sosy_lab.cpachecker.core.interfaces.Reducer;
import org.sosy_lab.cpachecker.core.interfaces.TransferRelation;
import org.sosy_lab.cpachecker.core.interfaces.pcc.ProofChecker;
import org.sosy_lab.cpachecker.core.reachedset.ReachedSet;
import org.sosy_lab.cpachecker.core.reachedset.ReachedSetFactory;
import org.sosy_lab.cpachecker.cpa.arg.ARGPath;
import org.sosy_lab.cpachecker.cpa.arg.ARGReachedSet;
import org.sosy_lab.cpachecker.cpa.arg.ARGState;
import org.sosy_lab.cpachecker.exceptions.CPAException;
import org.sosy_lab.cpachecker.exceptions.CPATransferException;
import org.sosy_lab.cpachecker.util.AbstractStates;

import com.google.common.collect.HashMultimap;
import com.google.common.collect.Multimap;

@Options(prefix = "cpa.bam")
public class BAMTransferRelation implements TransferRelation {

  @Options
  static class PCCInformation {

    @Option(name = "pcc.proofgen.doPCC", description = "")
    private boolean doPCC = false;

    private static PCCInformation instance = null;

    private PCCInformation(Configuration pConfig) throws InvalidConfigurationException {
      pConfig.inject(this);
    }

    public static void instantiate(Configuration pConfig) throws InvalidConfigurationException {
      instance = new PCCInformation(pConfig);
    }

    public static boolean isPCCEnabled() {
      return instance.doPCC;
    }

  }

  private final BAMCache argCache;

  private final Map<AbstractState, ReachedSet> abstractStateToReachedSet = new HashMap<>();
  private final Map<AbstractState, AbstractState> expandedToReducedCache = new HashMap<>();

  private Block currentBlock;
  private BlockPartitioning partitioning;
  private int depth = 0;

  private final LogManager logger;
  private final CPAAlgorithmFactory algorithmFactory;
  private final TransferRelation wrappedTransfer;
  private final ReachedSetFactory reachedSetFactory;
  private final Reducer wrappedReducer;
  private final BAMPrecisionAdjustment prec;
  private final BAMCPA bamCPA;
  private final ProofChecker wrappedProofChecker;

  private Map<AbstractState, Precision> forwardPrecisionToExpandedPrecision;
  private Map<Pair<ARGState, Block>, Collection<ARGState>> correctARGsForBlocks = null;

  //Stats
  @Option(
      description = "if enabled, the reached set cache is analysed for each cache miss to find the cause of the miss.")
  boolean gatherCacheMissStatistics = false;
  int cacheMisses = 0;
  int partialCacheHits = 0;
  int fullCacheHits = 0;
  int maxRecursiveDepth = 0;

  final Timer recomputeARTTimer = new Timer();
  final Timer removeCachedSubtreeTimer = new Timer();
  final Timer removeSubtreeTimer = new Timer();



  public BAMTransferRelation(Configuration pConfig, LogManager pLogger, BAMCPA bamCpa,
                             ProofChecker wrappedChecker, BAMCache cache,
      ReachedSetFactory pReachedSetFactory, ShutdownNotifier pShutdownNotifier) throws InvalidConfigurationException {
    pConfig.inject(this);
    logger = pLogger;
    algorithmFactory = new CPAAlgorithmFactory(bamCpa, logger, pConfig, pShutdownNotifier);
    reachedSetFactory = pReachedSetFactory;
    wrappedTransfer = bamCpa.getWrappedCpa().getTransferRelation();
    wrappedReducer = bamCpa.getReducer();
    prec = bamCpa.getPrecisionAdjustment();
    PCCInformation.instantiate(pConfig);
    bamCPA = bamCpa;
    wrappedProofChecker = wrappedChecker;
    argCache = cache;

    assert wrappedReducer != null;
  }


  void setForwardPrecisionToExpandedPrecision(
      Map<AbstractState, Precision> pForwardPrecisionToExpandedPrecision) {
    forwardPrecisionToExpandedPrecision = pForwardPrecisionToExpandedPrecision;
  }

  void setBlockPartitioning(BlockPartitioning pManager) {
    partitioning = pManager;
    currentBlock = partitioning.getMainBlock();
  }

  public BlockPartitioning getBlockPartitioning() {
    assert partitioning != null;
    return partitioning;
  }

  @Override
  public Collection<? extends AbstractState> getAbstractSuccessors(
      AbstractState pElement, Precision pPrecision, CFAEdge edge)
      throws CPATransferException, InterruptedException {

    forwardPrecisionToExpandedPrecision.clear();

    if (edge == null) {

      CFANode node = extractLocation(pElement);

      if (partitioning.isCallNode(node)) {
        //we have to start a recursive analysis
        if (partitioning.getBlockForCallNode(node).equals(currentBlock)) {
          //we are already in same context
          //thus we already did the recursive call or we a recursion in the cachedSubtrees
          //the latter isnt supported yet, but in the the former case we can classicaly do the post operation
          return attachAdditionalInfoToCallNodes(wrappedTransfer.getAbstractSuccessors(pElement, pPrecision, edge));
        }

        if (isHeadOfMainFunction(node)) {
          //skip main function
          return attachAdditionalInfoToCallNodes(wrappedTransfer.getAbstractSuccessors(pElement, pPrecision, edge));
        }


        //Create ReachSet with node as initial element (+ add corresponding Location+CallStackElement)
        //do an CPA analysis to get the complete reachset
        //if lastElement is error State
        // -> return lastElement and break at precision adjustment
        //else
        // -> compute which states refer to return nodes
        // -> return these states as successor
        // -> cache the result

        logger.log(Level.FINER, "Starting recursive analysis of depth", ++depth);
        logger.log(Level.ALL, "Starting element:", pElement);
        maxRecursiveDepth = Math.max(depth, maxRecursiveDepth);

        Block outerSubtree = currentBlock;
        currentBlock = partitioning.getBlockForCallNode(node);
        Collection<Pair<AbstractState, Precision>> reducedResult = performCompositeAnalysis(pElement, pPrecision, node);

        logger.log(Level.FINER, "Recursive analysis of depth", depth--, "finished");
        logger.log(Level.ALL, "Resulting elements:", reducedResult);

        addBlockAnalysisInfo(pElement);

        List<AbstractState> expandedResult = new ArrayList<>(reducedResult.size());
        for (Pair<AbstractState, Precision> reducedPair : reducedResult) {
          AbstractState reducedState = reducedPair.getFirst();
          Precision reducedPrecision = reducedPair.getSecond();

          if (reducedState == BAMARGBlockStartState.getDummy()) {
            ((BAMARGBlockStartState)reducedState).addParent((ARGState) pElement);
            expandedResult.add(reducedState);
            return expandedResult;
          }

          ARGState expandedState =
              (ARGState) wrappedReducer.getVariableExpandedState(pElement, currentBlock, reducedState);
          expandedToReducedCache.put(expandedState, reducedState);

          Precision expandedPrecision =
              wrappedReducer.getVariableExpandedPrecision(pPrecision, outerSubtree, reducedPrecision);

          expandedState.addParent((ARGState) pElement);
          expandedResult.add(expandedState);

          forwardPrecisionToExpandedPrecision.put(expandedState, expandedPrecision);
        }

        logger.log(Level.ALL, "Expanded results:", expandedResult);

        currentBlock = outerSubtree;

        return attachAdditionalInfoToCallNodes(expandedResult);
      } else {
        List<AbstractState> result = new ArrayList<>();
        for (int i = 0; i < node.getNumLeavingEdges(); i++) {
          CFAEdge e = node.getLeavingEdge(i);
          result.addAll(getAbstractSuccessors0(pElement, pPrecision, e));
        }
        return attachAdditionalInfoToCallNodes(result);
      }
    } else {
      return attachAdditionalInfoToCallNodes(getAbstractSuccessors0(pElement, pPrecision, edge));
    }
  }

  private Collection<? extends AbstractState> getAbstractSuccessors0(AbstractState pElement, Precision pPrecision,
      CFAEdge edge) throws CPATransferException, InterruptedException {
    assert edge != null;

    CFANode currentNode = edge.getPredecessor();

    Block currentNodeBlock = partitioning.getBlockForReturnNode(currentNode);
    if (currentNodeBlock != null && !currentBlock.equals(currentNodeBlock)
        && currentNodeBlock.getNodes().contains(edge.getSuccessor())) {
      // we are not analyzing the block corresponding to currentNode (currentNodeBlock) but the currentNodeBlock is inside of this block
      // avoid a reanalysis
      return Collections.emptySet();
    }

    if (currentBlock.isReturnNode(currentNode) && !currentBlock.getNodes().contains(edge.getSuccessor())) {
      // do not perform analysis beyond the current block
      return Collections.emptySet();
    }
    return attachAdditionalInfoToCallNodes(wrappedTransfer.getAbstractSuccessors(pElement, pPrecision, edge));
  }


  private boolean isHeadOfMainFunction(CFANode currentNode) {
    return currentNode instanceof FunctionEntryNode && currentNode.getNumEnteringEdges() == 0;
  }


  private Collection<Pair<AbstractState, Precision>> performCompositeAnalysis(AbstractState initialState,
      Precision initialPrecision, CFANode node) throws InterruptedException, RecursiveAnalysisFailedException {
    try {
      AbstractState reducedInitialState = wrappedReducer.getVariableReducedState(initialState, currentBlock, node);
      Precision reducedInitialPrecision = wrappedReducer.getVariableReducedPrecision(initialPrecision, currentBlock);
      Pair<ReachedSet, Collection<AbstractState>> pair =
          argCache.get(reducedInitialState, reducedInitialPrecision, currentBlock);
      ReachedSet reached = pair.getFirst();
      Collection<AbstractState> returnElements = pair.getSecond();

      abstractStateToReachedSet.put(initialState, reached);

      if (returnElements != null) {
        assert reached != null;
        fullCacheHits++;
        return imbueAbstractStatesWithPrecision(reached, returnElements);
      }

      if (reached != null) {
        //at least we have partly computed reach set cached
        partialCacheHits++;
      } else {
        //compute the subgraph specification from scratch
        cacheMisses++;

        if (gatherCacheMissStatistics) {
          argCache.findCacheMissCause(reducedInitialState, reducedInitialPrecision, currentBlock);
        }

        reached = createInitialReachedSet(reducedInitialState, reducedInitialPrecision);
        argCache.put(reducedInitialState, reducedInitialPrecision, currentBlock, reached);
        abstractStateToReachedSet.put(initialState, reached);
      }

      // CPAAlgorithm is not re-entrant due to statistics
      CPAAlgorithm algorithm = algorithmFactory.newInstance();
      algorithm.run(reached);

      // if the element is an error element
      AbstractState lastElement = reached.getLastState();
      if (isTargetState(lastElement)) {
        //found a target state inside a recursive subgraph call
        //this needs to be propagated to outer subgraph (till main is reached)
        returnElements = Collections.singletonList(lastElement);

      } else if (reached.hasWaitingState()) {
        //no target state, but waiting elements
        //analysis failed -> also break this analysis
        prec.breakAnalysis();
        return Collections.singletonList(Pair.of(
            (AbstractState) BAMARGBlockStartState.createDummy(reducedInitialState),
            reducedInitialPrecision)); //dummy element
      } else {
        returnElements = AbstractStates.filterLocations(reached, currentBlock.getReturnNodes())
            .toList();
      }

      ARGState rootOfBlock = null;
      if (PCCInformation.isPCCEnabled()) {
        if (!(reached.getFirstState() instanceof ARGState)) { throw new CPATransferException(
            "Cannot build proof, ARG, for BAM analysis."); }
        rootOfBlock = BAMARGUtils.copyARG((ARGState) reached.getFirstState());
      }
      argCache.put(reducedInitialState, reached.getPrecision(reached.getFirstState()), currentBlock, returnElements,
          rootOfBlock);

      return imbueAbstractStatesWithPrecision(reached, returnElements);
    } catch (CPAException e) {
      throw new RecursiveAnalysisFailedException(e);
    }
  }


  private List<Pair<AbstractState, Precision>> imbueAbstractStatesWithPrecision(
      ReachedSet pReached, Collection<AbstractState> pElements) {
    List<Pair<AbstractState, Precision>> result = new ArrayList<>();
    for (AbstractState ele : pElements) {
      result.add(Pair.of(ele, pReached.getPrecision(ele)));
    }
    return result;
  }

  private Collection<? extends AbstractState> attachAdditionalInfoToCallNodes(
      Collection<? extends AbstractState> pSuccessors) {
    if (PCCInformation.isPCCEnabled()) {
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
    return pSuccessors;
  }

  protected AbstractState attachAdditionalInfoToCallNode(AbstractState pElem) {
    if (!(pElem instanceof BAMARGBlockStartState) && PCCInformation.isPCCEnabled() && pElem instanceof ARGState) { return createAdditionalInfo((ARGState) pElem); }
    return pElem;
  }

  private ARGState createAdditionalInfo(ARGState pElem) {
    CFANode node = AbstractStates.extractLocation(pElem);
    if (partitioning.isCallNode(node) && !partitioning.getBlockForCallNode(node).equals(currentBlock)) {
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

  private void addBlockAnalysisInfo(AbstractState pElement) throws CPATransferException {
    if (PCCInformation.isPCCEnabled()) {
      if (argCache.getLastAnalyzedBlock() == null || !(pElement instanceof BAMARGBlockStartState)) { throw new CPATransferException(
          "Cannot build proof, ARG, for BAM analysis."); }
      ((BAMARGBlockStartState) pElement).setAnalyzedBlock(argCache.getLastAnalyzedBlock());
    }
  }

  private ReachedSet createInitialReachedSet(AbstractState initialState, Precision initialPredicatePrecision) {
    ReachedSet reached = reachedSetFactory.create();
    reached.add(initialState, initialPredicatePrecision);
    return reached;
  }

  void removeSubtree(ARGReachedSet mainReachedSet, ARGPath pPath,
      ARGState element, List<Precision> pNewPrecisions,
      List<Class<? extends Precision>> pNewPrecisionTypes,
      Map<ARGState, ARGState> pPathElementToReachedState) {
    removeSubtreeTimer.start();

    final ARGSubtreeRemover argSubtreeRemover = new ARGSubtreeRemover(
            partitioning, wrappedReducer, argCache, reachedSetFactory, abstractStateToReachedSet,
            removeCachedSubtreeTimer, logger);
    argSubtreeRemover.removeSubtree(mainReachedSet, pPath, element,
            pNewPrecisions, pNewPrecisionTypes, pPathElementToReachedState);

    removeSubtreeTimer.stop();
  }

  private static void removeSubtree(ARGReachedSet reachedSet, ARGState argElement) {
    reachedSet.removeSubtree(argElement);
  }

  //returns root of a subtree leading from the root element of the given reachedSet to the target state
  //subtree is represented using children and parents of ARGElements, where newTreeTarget is the ARGState
  //in the constructed subtree that represents target
  ARGState computeCounterexampleSubgraph(ARGState target, ARGReachedSet reachedSet, BackwardARGState newTreeTarget,
      Map<ARGState, ARGState> pPathElementToReachedState) throws InterruptedException, RecursiveAnalysisFailedException {
    assert reachedSet.asReachedSet().contains(target);

    //start by creating ARGElements for each node needed in the tree
    Map<ARGState, BackwardARGState> elementsMap = new HashMap<>();
    Stack<ARGState> openElements = new Stack<>();
    ARGState root = null;

    pPathElementToReachedState.put(newTreeTarget, target);
    elementsMap.put(target, newTreeTarget);
    openElements.push(target);
    while (!openElements.empty()) {
      ARGState currentElement = openElements.pop();

      assert reachedSet.asReachedSet().contains(currentElement);

      for (ARGState parent : currentElement.getParents()) {
        if (!elementsMap.containsKey(parent)) {
          //create node for parent in the new subtree
          elementsMap.put(parent, new BackwardARGState(parent.getWrappedState(), null));
          pPathElementToReachedState.put(elementsMap.get(parent), parent);
          //and remember to explore the parent later
          openElements.push(parent);
        }
        CFAEdge edge = BAMARGUtils.getEdgeToChild(parent, currentElement);
        if (edge == null) {
          //this is a summarized call and thus an direct edge could not be found
          //we have the transfer function to handle this case, as our reachSet is wrong
          //(we have to use the cached ones)
          ARGState innerTree =
              computeCounterexampleSubgraph(parent, reachedSet.asReachedSet().getPrecision(parent),
                  elementsMap.get(currentElement), pPathElementToReachedState);
          if (innerTree == null) {
            removeSubtree(reachedSet, parent);
            return null;
          }
          for (ARGState child : innerTree.getChildren()) {
            child.addParent(elementsMap.get(parent));
          }
          innerTree.removeFromARG();
          elementsMap.get(parent).updateDecreaseId();
        } else {
          //normal edge
          //create an edge from parent to current
          elementsMap.get(currentElement).addParent(elementsMap.get(parent));
        }
      }
      if (currentElement.getParents().isEmpty()) {
        root = elementsMap.get(currentElement);
      }
    }
    assert root != null;
    return root;
  }

  /**
   * This method looks for the reached set that belongs to (root, rootPrecision),
   * then looks for target in this reached set and constructs a tree from root to target
   * (recursively, if needed).
   * @throws RecursiveAnalysisFailedException
   */
  private ARGState computeCounterexampleSubgraph(ARGState root, Precision rootPrecision, BackwardARGState newTreeTarget,
      Map<ARGState, ARGState> pPathElementToReachedState) throws InterruptedException, RecursiveAnalysisFailedException {
    CFANode rootNode = extractLocation(root);
    Block rootSubtree = partitioning.getBlockForCallNode(rootNode);

    AbstractState reducedRootState = wrappedReducer.getVariableReducedState(root, rootSubtree, rootNode);
    ReachedSet reachSet = abstractStateToReachedSet.get(root);

    //we found the to the root and precision corresponding reach set
    //now try to find the target in the reach set
    ARGState targetARGState = (ARGState) expandedToReducedCache.get(pPathElementToReachedState.get(newTreeTarget));
    if (targetARGState.isDestroyed()) {
      logger.log(Level.FINE,
          "Target state refers to a destroyed ARGState, i.e., the cached subtree is outdated. Updating it.");
      return null;
    }
    assert reachSet.contains(targetARGState);
    //we found the target; now construct a subtree in the ARG starting with targetARTElement
    ARGState result =
        computeCounterexampleSubgraph(targetARGState, new ARGReachedSet(reachSet), newTreeTarget,
            pPathElementToReachedState);
    if (result == null) {
      //enforce recomputation to update cached subtree
      argCache.removeReturnEntry(reducedRootState, reachSet.getPrecision(reachSet.getFirstState()), rootSubtree);
    }
    return result;
  }

  void clearCaches() {
    argCache.clear();
    abstractStateToReachedSet.clear();
  }

  Pair<Block, ReachedSet> getCachedReachedSet(ARGState root, Precision rootPrecision) {
    CFANode rootNode = extractLocation(root);
    Block rootSubtree = partitioning.getBlockForCallNode(rootNode);

    ReachedSet reachSet = abstractStateToReachedSet.get(root);
    assert reachSet != null;
    return Pair.of(rootSubtree, reachSet);
  }

  @Override
  public Collection<? extends AbstractState> strengthen(
      AbstractState pElement, List<AbstractState> pOtherElements,
      CFAEdge pCfaEdge, Precision pPrecision) throws CPATransferException,
      InterruptedException {
    return attachAdditionalInfoToCallNodes(wrappedTransfer.strengthen(pElement, pOtherElements, pCfaEdge, pPrecision));
  }

  public boolean areAbstractSuccessors(AbstractState pState, CFAEdge pCfaEdge,
      Collection<? extends AbstractState> pSuccessors) throws CPATransferException,
      InterruptedException {
    if (pCfaEdge != null) { return wrappedProofChecker.areAbstractSuccessors(pState, pCfaEdge, pSuccessors); }
    return areAbstractSuccessors0(pState, pCfaEdge, pSuccessors, partitioning.getMainBlock());
  }

  private boolean areAbstractSuccessors0(AbstractState pState, CFAEdge pCfaEdge,
      Collection<? extends AbstractState> pSuccessors, final Block currentBlock)
      throws CPATransferException,
      InterruptedException {
    // currently cannot deal with blocks for which the set of call nodes and return nodes of that block is not disjunct
    boolean successorExists;

    CFANode node = extractLocation(pState);

    if (partitioning.isCallNode(node) && !isHeadOfMainFunction(node)
        && !partitioning.getBlockForCallNode(node).equals(currentBlock)) {
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

        HashSet<AbstractState> notFoundSuccessors = new HashSet<>(pSuccessors);
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
        throw new CPATransferException("Checking ARG with root " + ((BAMARGBlockStartState) pState).getAnalyzedBlock()
            + " for block " + currentBlock + "failed.");
      }
    } else {
      HashSet<CFAEdge> usedEdges = new HashSet<>();
      for (AbstractState absElement : pSuccessors) {
        ARGState successorElem = (ARGState) absElement;
        usedEdges.add(((ARGState) pState).getEdgeToChild(successorElem));
      }

      //no call node, check if successors can be constructed with help of CFA edges
      for (int i = 0; i < node.getNumLeavingEdges(); i++) {
        // edge leads to node in inner block
        Block currentNodeBlock = partitioning.getBlockForReturnNode(node);
        if (currentNodeBlock != null && !currentBlock.equals(currentNodeBlock)
            && currentNodeBlock.getNodes().contains(node.getLeavingEdge(i).getSuccessor())) {
          if (usedEdges.contains(node.getLeavingEdge(i))) { return false; }
          continue;
        }
        // edge leaves block, do not analyze, check for call node since if call node is also return node analysis will go beyond current block
        if (!currentBlock.isCallNode(node) && currentBlock.isReturnNode(node)
            && !currentBlock.getNodes().contains(node.getLeavingEdge(i).getSuccessor())) {
          if (usedEdges.contains(node.getLeavingEdge(i))) { return false; }
          continue;
        }
        if (!wrappedProofChecker.areAbstractSuccessors(pState, node.getLeavingEdge(i), pSuccessors)) {
          return false;
        }
      }
    }
    return true;
  }

  private Pair<Boolean, Collection<ARGState>> checkARGBlock(ARGState rootNode,
      final Block currentBlock)
      throws CPAException, InterruptedException {
    Collection<ARGState> returnNodes = new ArrayList<>();
    Set<ARGState> waitingForUnexploredParents = new HashSet<>();
    boolean unexploredParent;
    Stack<ARGState> waitlist = new Stack<>();
    HashSet<ARGState> visited = new HashSet<>();
    HashSet<ARGState> coveredNodes = new HashSet<>();
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
            returnNodes = Collections.emptyList();
            return Pair.of(false, returnNodes);
          }
          coveredNodes.add(current);
          if (coveredNodes.contains(current.getCoveringState())) {
            returnNodes = Collections.emptyList();
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
      if (currentBlock.isReturnNode(node)) {
        returnNodes.add(current);
      }

      if (!areAbstractSuccessors0(current, null, current.getChildren(), currentBlock)) {
        returnNodes = Collections.emptyList();
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
          returnNodes = Collections.emptyList();
          return Pair.of(false, returnNodes);
        } else {
          waitingForUnexploredParents.remove(child);
          visited.add(child);
          waitlist.add(child);
        }
      }

    }
    if (!waitingForUnexploredParents.isEmpty()) {
      returnNodes = Collections.emptyList();
      return Pair.of(false, returnNodes);
    }
    return Pair.of(true, returnNodes);
  }

  public Collection<ReachedSet> getCachedReachedSet() {
    return argCache.getAllCachedReachedStates();
  }

  public void setCorrectARG(Pair<ARGState, Block> pKey, Collection<ARGState> pEndOfBlock){
    if (correctARGsForBlocks == null) {
      correctARGsForBlocks = new HashMap<>();
    }
    correctARGsForBlocks.put(pKey, pEndOfBlock);
  }

  static class BackwardARGState extends ARGState {

    private static final long serialVersionUID = -3279533907385516993L;
    private int decreasingStateID;
    private static int nextDecreaseID = Integer.MAX_VALUE;

    public BackwardARGState(AbstractState pWrappedState, ARGState pParentElement) {
      super(pWrappedState, pParentElement);
      decreasingStateID = nextDecreaseID--;
    }

    @Override
    public boolean isOlderThan(ARGState other) {
      if (other instanceof BackwardARGState) { return decreasingStateID < ((BackwardARGState) other).decreasingStateID; }
      return super.isOlderThan(other);
    }

    void updateDecreaseId() {
      decreasingStateID = nextDecreaseID--;
    }
  }
}
