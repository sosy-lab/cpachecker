/*
 *  CPAchecker is a tool for configurable software verification.
 *  This file is part of CPAchecker.
 *
 *  Copyright (C) 2007-2012  Dirk Beyer
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
package org.sosy_lab.cpachecker.cpa.abm;

import static com.google.common.base.Preconditions.checkNotNull;
import static org.sosy_lab.cpachecker.util.AbstractStates.*;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Deque;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.Stack;
import java.util.logging.Level;

import org.sosy_lab.common.LogManager;
import org.sosy_lab.common.Pair;
import org.sosy_lab.common.Timer;
import org.sosy_lab.common.configuration.Configuration;
import org.sosy_lab.common.configuration.InvalidConfigurationException;
import org.sosy_lab.common.configuration.Option;
import org.sosy_lab.common.configuration.Options;
import org.sosy_lab.cpachecker.cfa.blocks.Block;
import org.sosy_lab.cpachecker.cfa.blocks.BlockPartitioning;
import org.sosy_lab.cpachecker.cfa.model.CFAEdge;
import org.sosy_lab.cpachecker.cfa.model.CFANode;
import org.sosy_lab.cpachecker.cfa.model.FunctionEntryNode;
import org.sosy_lab.cpachecker.core.algorithm.CPAAlgorithm;
import org.sosy_lab.cpachecker.core.interfaces.AbstractState;
import org.sosy_lab.cpachecker.core.interfaces.Precision;
import org.sosy_lab.cpachecker.core.interfaces.Reducer;
import org.sosy_lab.cpachecker.core.interfaces.TransferRelation;
import org.sosy_lab.cpachecker.core.reachedset.ReachedSet;
import org.sosy_lab.cpachecker.core.reachedset.ReachedSetFactory;
import org.sosy_lab.cpachecker.core.reachedset.UnmodifiableReachedSet;
import org.sosy_lab.cpachecker.cpa.arg.ARGReachedSet;
import org.sosy_lab.cpachecker.cpa.arg.ARGState;
import org.sosy_lab.cpachecker.cpa.arg.Path;
import org.sosy_lab.cpachecker.exceptions.CPAException;
import org.sosy_lab.cpachecker.exceptions.CPATransferException;
import org.sosy_lab.cpachecker.util.AbstractStates;
import org.sosy_lab.cpachecker.util.Precisions;

import com.google.common.collect.Iterables;

@Options(prefix="cpa.abm")
public class ABMTransferRelation implements TransferRelation {

  private class AbstractStateHash {

    private final Object wrappedHash;
    private final Block context;

    private final AbstractState predicateKey;
    private final Precision precisionKey;

    public AbstractStateHash(AbstractState pPredicateKey, Precision pPrecisionKey, Block pContext) {
      wrappedHash = wrappedReducer.getHashCodeForState(pPredicateKey, pPrecisionKey);
      context = checkNotNull(pContext);

      predicateKey = pPredicateKey;
      precisionKey = pPrecisionKey;
    }

    @Override
    public boolean equals(Object pObj) {
      if (!(pObj instanceof AbstractStateHash)) {
        return false;
      }
      AbstractStateHash other = (AbstractStateHash)pObj;
      equalsTimer.start();
      try {
        return context.equals(other.context)
            && wrappedHash.equals(other.wrappedHash);
      } finally {
        equalsTimer.stop();
      }
    }

    @Override
    public int hashCode() {
      hashingTimer.start();
      try {
        return wrappedHash.hashCode() * 17 + context.hashCode();
      } finally {
        hashingTimer.stop();
      }
    }

    @Override
    public String toString() {
      return "AbstractStateHash [hash=" + hashCode() + ", wrappedHash=" + wrappedHash + ", context="
          + context + ", predicateKey=" + predicateKey + ", precisionKey="
          + precisionKey + "]";
    }
  }

  private class Cache {

    private final Map<AbstractStateHash, ReachedSet> preciseReachedCache = new HashMap<AbstractStateHash, ReachedSet>();
    private final Map<AbstractStateHash, ReachedSet> unpreciseReachedCache = new HashMap<AbstractStateHash, ReachedSet>();

    private final Map<AbstractStateHash, Collection<AbstractState>> returnCache = new HashMap<AbstractStateHash, Collection<AbstractState>>();

    private AbstractStateHash getHashCode(AbstractState predicateKey, Precision precisionKey, Block context) {
      return new AbstractStateHash(predicateKey, precisionKey, context);
    }

    private void put(AbstractState predicateKey, Precision precisionKey, Block context, ReachedSet item) {
      AbstractStateHash hash = getHashCode(predicateKey, precisionKey, context);
      assert !preciseReachedCache.containsKey(hash);
      preciseReachedCache.put(hash, item);
    }

    private void put(AbstractState predicateKey, Precision precisionKey, Block context, Collection<AbstractState> item) {
      AbstractStateHash hash = getHashCode(predicateKey, precisionKey, context);
      assert allStatesContainedInReachedSet(item, preciseReachedCache.get(hash));
      returnCache.put(hash, item);
    }

    private boolean allStatesContainedInReachedSet(Collection<AbstractState> pElements, ReachedSet reached) {
      for (AbstractState e : pElements) {
        if (!reached.contains(e)) {
          return false;
        }
      }
      return true;
    }

    private void removeReturnEntry(AbstractState predicateKey, Precision precisionKey, Block context) {
      returnCache.remove(getHashCode(predicateKey, precisionKey, context));
    }

    private Pair<ReachedSet, Collection<AbstractState>> get(AbstractState predicateKey, Precision precisionKey, Block context) {
      AbstractStateHash hash = getHashCode(predicateKey, precisionKey, context);

      ReachedSet result = preciseReachedCache.get(hash);
      if (result != null) {
        return Pair.of(result, returnCache.get(hash));
      }

      if (aggressiveCaching) {
        result = unpreciseReachedCache.get(hash);
        if (result != null) {
          return Pair.of(result, returnCache.get(getHashCode(predicateKey, result.getPrecision(result.getFirstState()), context)));
        }

        //search for similar entry
        Pair<ReachedSet, Collection<AbstractState>> pair = lookForSimilarState(predicateKey, precisionKey, context);
        if (pair != null) {
          //found similar element, use this
          unpreciseReachedCache.put(hash, pair.getFirst());
          return pair;
        }
      }

      return Pair.of(null, null);
    }

    private Pair<ReachedSet, Collection<AbstractState>> lookForSimilarState(AbstractState pPredicateKey, Precision pPrecisionKey, Block pContext) {
      searchingTimer.start();
      try {
        int min = Integer.MAX_VALUE;
        Pair<ReachedSet, Collection<AbstractState>> result = null;

        for (AbstractStateHash cacheKey : preciseReachedCache.keySet()) {
          //searchKey != cacheKey, check whether it is the same if we ignore the precision
          AbstractStateHash ignorePrecisionSearchKey = getHashCode(pPredicateKey, cacheKey.precisionKey, pContext);
          if (ignorePrecisionSearchKey.equals(cacheKey)) {
            int distance = wrappedReducer.measurePrecisionDifference(pPrecisionKey, cacheKey.precisionKey);
            if (distance < min) { //prefer similar precisions
              min = distance;
              result = Pair.of(preciseReachedCache.get(ignorePrecisionSearchKey), returnCache.get(ignorePrecisionSearchKey));
            }
          }
        }

        return result;
      } finally {
        searchingTimer.stop();
      }
    }

    private void findCacheMissCause(AbstractState pPredicateKey, Precision pPrecisionKey, Block pContext) {
      AbstractStateHash searchKey = getHashCode(pPredicateKey, pPrecisionKey, pContext);
      for (AbstractStateHash cacheKey : preciseReachedCache.keySet()) {
        assert !searchKey.equals(cacheKey);
        //searchKey != cacheKey, check whether it is the same if we ignore the precision
        AbstractStateHash ignorePrecisionSearchKey = getHashCode(pPredicateKey, cacheKey.precisionKey, pContext);
        if (ignorePrecisionSearchKey.equals(cacheKey)) {
          precisionCausedMisses++;
          return;
        }
        //precision was not the cause. Check abstraction.
        AbstractStateHash ignoreAbsSearchKey = getHashCode(cacheKey.predicateKey, pPrecisionKey, pContext);
        if (ignoreAbsSearchKey.equals(cacheKey)) {
          abstractionCausedMisses++;
          return;
        }
      }
      noSimilarCausedMisses++;
    }

    private void clear() {
      preciseReachedCache.clear();
      unpreciseReachedCache.clear();
      returnCache.clear();
    }

    private boolean containsPreciseKey(AbstractState predicateKey, Precision precisionKey, Block context) {
      AbstractStateHash hash = getHashCode(predicateKey, precisionKey, context);
      return preciseReachedCache.containsKey(hash);
    }

    public void updatePrecisionForEntry(AbstractState predicateKey, Precision precisionKey, Block context, Precision newPrecisionKey) {
      AbstractStateHash hash = getHashCode(predicateKey, precisionKey, context);
      ReachedSet reachedSet = preciseReachedCache.get(hash);
      if (reachedSet != null) {
        preciseReachedCache.remove(hash);
        preciseReachedCache.put(getHashCode(predicateKey, newPrecisionKey, context), reachedSet);
      }
    }
  }

  @Option(description="if enabled, cache queries also consider blocks with non-matching precision for reuse.")
  private boolean aggressiveCaching = true;

  private final Cache argCache = new Cache();

  private final Map<AbstractState, ReachedSet> abstractStateToReachedSet = new HashMap<AbstractState, ReachedSet>();
  private final Map<AbstractState, AbstractState> expandedToReducedCache = new HashMap<AbstractState, AbstractState>();

  private Block currentBlock;
  private BlockPartitioning partitioning;
  private int depth = 0;

  private final LogManager logger;
  private final CPAAlgorithm algorithm;
  private final TransferRelation wrappedTransfer;
  private final ReachedSetFactory reachedSetFactory;
  private final Reducer wrappedReducer;
  private final ABMPrecisionAdjustment prec;

  private Map<AbstractState, Precision> forwardPrecisionToExpandedPrecision;

  //Stats
  @Option(description="if enabled, the reached set cache is analysed for each cache miss to find the cause of the miss.")
  boolean gatherCacheMissStatistics = false;
  int cacheMisses = 0;
  int partialCacheHits = 0;
  int fullCacheHits = 0;
  int maxRecursiveDepth = 0;
  int abstractionCausedMisses = 0;
  int precisionCausedMisses = 0;
  int noSimilarCausedMisses = 0;

  final Timer hashingTimer = new Timer();
  final Timer equalsTimer = new Timer();
  final Timer recomputeARTTimer = new Timer();
  final Timer removeCachedSubtreeTimer = new Timer();
  final Timer removeSubtreeTimer = new Timer();
  final Timer searchingTimer = new Timer();



  public ABMTransferRelation(Configuration pConfig, LogManager pLogger, ABMCPA abmCpa, ReachedSetFactory pReachedSetFactory) throws InvalidConfigurationException {
    pConfig.inject(this);
    logger = pLogger;
    algorithm = new CPAAlgorithm(abmCpa, logger, pConfig);
    reachedSetFactory = pReachedSetFactory;
    wrappedTransfer = abmCpa.getWrappedCpa().getTransferRelation();
    wrappedReducer = abmCpa.getReducer();
    prec = abmCpa.getPrecisionAdjustment();
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
          return wrappedTransfer.getAbstractSuccessors(pElement, pPrecision, edge);
        }

        if (isHeadOfMainFunction(node)) {
          //skip main function
          return wrappedTransfer.getAbstractSuccessors(pElement, pPrecision, edge);
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

        List<AbstractState> expandedResult = new ArrayList<AbstractState>(reducedResult.size());
        for (Pair<AbstractState, Precision> reducedPair: reducedResult) {
          AbstractState reducedState = reducedPair.getFirst();
          Precision reducedPrecision = reducedPair.getSecond();

          ARGState expandedState = (ARGState)wrappedReducer.getVariableExpandedState(pElement, currentBlock, reducedState);
          expandedToReducedCache.put(expandedState, reducedState);

          Precision expandedPrecision = wrappedReducer.getVariableExpandedPrecision(pPrecision, outerSubtree, reducedPrecision);

          expandedState.addParent((ARGState)pElement);
          expandedResult.add(expandedState);

          forwardPrecisionToExpandedPrecision.put(expandedState, expandedPrecision);
        }

        logger.log(Level.ALL, "Expanded results:", expandedResult);

        currentBlock = outerSubtree;

        return expandedResult;
      }
      else {
        List<AbstractState> result = new ArrayList<AbstractState>();
        for (int i = 0; i < node.getNumLeavingEdges(); i++) {
          CFAEdge e = node.getLeavingEdge(i);
          result.addAll(getAbstractSuccessors0(pElement, pPrecision, e));
        }
        return result;
      }
    } else {
      return getAbstractSuccessors0(pElement, pPrecision, edge);
    }
  }

  private Collection<? extends AbstractState> getAbstractSuccessors0(AbstractState pElement, Precision pPrecision, CFAEdge edge) throws CPATransferException, InterruptedException {
    assert edge != null;

    CFANode currentNode = edge.getPredecessor();

    Block currentNodeBlock = partitioning.getBlockForReturnNode(currentNode);
    if (currentNodeBlock != null && !currentBlock.equals(currentNodeBlock) && currentNodeBlock.getNodes().contains(edge.getSuccessor())) {
      // we are not analyzing the block corresponding to currentNode (currentNodeBlock) but the currentNodeBlock is inside of this block
      // avoid a reanalysis
      return Collections.emptySet();
    }

    if (currentBlock.isReturnNode(currentNode) && !currentBlock.getNodes().contains(edge.getSuccessor())) {
      // do not perform analysis beyond the current block
      return Collections.emptySet();
    }

    return wrappedTransfer.getAbstractSuccessors(pElement, pPrecision, edge);
  }


  private boolean isHeadOfMainFunction(CFANode currentNode) {
    return currentNode instanceof FunctionEntryNode && currentNode.getNumEnteringEdges() == 0;
  }


  private Collection<Pair<AbstractState, Precision>> performCompositeAnalysis(AbstractState initialState, Precision initialPrecision, CFANode node) throws InterruptedException, RecursiveAnalysisFailedException {
    try {
      AbstractState reducedInitialState = wrappedReducer.getVariableReducedState(initialState, currentBlock, node);
      Precision reducedInitialPrecision = wrappedReducer.getVariableReducedPrecision(initialPrecision, currentBlock);
      Pair<ReachedSet, Collection<AbstractState>> pair = argCache.get(reducedInitialState, reducedInitialPrecision, currentBlock);
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

      algorithm.run(reached);

      // if the element is an error element
      AbstractState lastElement = reached.getLastState();
      if (isTargetState(lastElement)) {
        //found a target state inside a recursive subgraph call
        //this needs to be propagated to outer subgraph (till main is reached)
        returnElements = Collections.singletonList(lastElement);

      }
      else if (reached.hasWaitingState()) {
        //no target state, but waiting elements
        //analysis failed -> also break this analysis
        prec.breakAnalysis();
        return Collections.singletonList(Pair.of(reducedInitialState, reducedInitialPrecision)); //dummy element
      }
      else {
        returnElements = AbstractStates.filterLocations(reached, currentBlock.getReturnNodes())
                                       .toImmutableList();
      }

      argCache.put(reducedInitialState, reached.getPrecision(reached.getFirstState()), currentBlock, returnElements);

      return imbueAbstractStatesWithPrecision(reached, returnElements);
    } catch (CPAException e) {
      throw new RecursiveAnalysisFailedException(e);
    }
  }


  private List<Pair<AbstractState, Precision>> imbueAbstractStatesWithPrecision(
      ReachedSet pReached, Collection<AbstractState> pElements) {
    List<Pair<AbstractState, Precision>> result = new ArrayList<Pair<AbstractState,Precision>>();
    for (AbstractState ele : pElements) {
      result.add(Pair.of(ele, pReached.getPrecision(ele)));
    }
    return result;
  }

  private ReachedSet createInitialReachedSet(AbstractState initialState, Precision initialPredicatePrecision) {
    ReachedSet reached = reachedSetFactory.create();
    reached.add(initialState, initialPredicatePrecision);
    return reached;
  }

  void removeSubtree(ARGReachedSet mainReachedSet, Path pPath, ARGState element, Precision newPrecision, Map<ARGState, ARGState> pPathElementToReachedState) {
    removeSubtreeTimer.start();

    List<ARGState> path = trimPath(pPath, element);
    assert path.get(path.size()-1).equals(element);

    Set<ARGState> relevantCallNodes = getRelevantDefinitionNodes(path);

    Set<Pair<ARGReachedSet, ARGState>> neededRemoveSubtreeCalls = new HashSet<Pair<ARGReachedSet,ARGState>>();
    Set<Pair<ARGState, ARGState>> neededRemoveCachedSubtreeCalls = new HashSet<Pair<ARGState, ARGState>>();

    ARGState lastElement = null;
    //iterate from root to element and remove all subtrees for subgraph calls
    for (ARGState pathElement : Iterables.skip(path, 1)) {
      if (pathElement.equals(element)) {
        break;
      }

      if (relevantCallNodes.contains(pathElement)) {
        ARGState currentElement = pPathElementToReachedState.get(pathElement);

        if (lastElement == null) {
          neededRemoveSubtreeCalls.add(Pair.of(mainReachedSet, currentElement));
        } else {
          neededRemoveCachedSubtreeCalls.add(Pair.of(lastElement, currentElement));
        }

        lastElement = currentElement;
      }
    }

    if (aggressiveCaching) {
      ensureExactCacheHitsOnPath(mainReachedSet, pPath, element, newPrecision, pPathElementToReachedState, neededRemoveCachedSubtreeCalls);
    }

    for (Pair<ARGReachedSet, ARGState> removeSubtreeArguments : neededRemoveSubtreeCalls) {
      removeSubtree(removeSubtreeArguments.getFirst(), removeSubtreeArguments.getSecond());
    }

    for (Pair<ARGState, ARGState> removeCachedSubtreeArguments : neededRemoveCachedSubtreeCalls) {
      removeCachedSubtree(removeCachedSubtreeArguments.getFirst(), removeCachedSubtreeArguments.getSecond(), null);
    }

    if (lastElement == null) {
      removeSubtree(mainReachedSet, pPathElementToReachedState.get(element), newPrecision);
    } else {
      removeCachedSubtree(lastElement, pPathElementToReachedState.get(element), newPrecision);
    }

    removeSubtreeTimer.stop();
  }

  private void ensureExactCacheHitsOnPath(ARGReachedSet mainReachedSet, Path pPath, ARGState pElement, Precision newPrecision, Map<ARGState, ARGState> pPathElementToReachedState, Set<Pair<ARGState, ARGState>> neededRemoveCachedSubtreeCalls) {
    Map<ARGState, UnmodifiableReachedSet> pathElementToOuterReachedSet = new HashMap<ARGState, UnmodifiableReachedSet>();
    Pair<Set<ARGState>, Set<ARGState>> pair = getCallAndReturnNodes(pPath, pathElementToOuterReachedSet, mainReachedSet.asReachedSet(), pPathElementToReachedState);
    Set<ARGState> callNodes = pair.getFirst();
    Set<ARGState> returnNodes = pair.getSecond();

    Deque<ARGState> remainingPathElements = new LinkedList<ARGState>();
    for (int i = 0; i < pPath.size(); i++) {
      remainingPathElements.addLast(pPath.get(i).getFirst());
    }

    boolean starting = false;
    while (!remainingPathElements.isEmpty()) {
      ARGState currentElement = remainingPathElements.pop();

      if (currentElement.equals(pElement)) {
        starting = true;
      }

      if (starting) {
        if (callNodes.contains(currentElement)) {
          ARGState currentReachedState = pPathElementToReachedState.get(currentElement);
          CFANode node = extractLocation(currentReachedState);
          Block currentBlock = partitioning.getBlockForCallNode(node);
          AbstractState reducedState = wrappedReducer.getVariableReducedState(currentReachedState, currentBlock, node);

          removeUnpreciseCacheEntriesOnPath(currentElement, reducedState, newPrecision, currentBlock, remainingPathElements, pPathElementToReachedState, callNodes, returnNodes, pathElementToOuterReachedSet, neededRemoveCachedSubtreeCalls);
        }
      }
    }
  }

  private boolean removeUnpreciseCacheEntriesOnPath(ARGState rootState, AbstractState reducedRootState, Precision newPrecision, Block rootBlock, Deque<ARGState> remainingPathElements, Map<ARGState, ARGState> pPathElementToReachedState, Set<ARGState> callNodes, Set<ARGState> returnNodes, Map<ARGState, UnmodifiableReachedSet> pathElementToOuterReachedSet, Set<Pair<ARGState, ARGState>> neededRemoveCachedSubtreeCalls) {
    UnmodifiableReachedSet outerReachedSet = pathElementToOuterReachedSet.get(rootState);

    Precision rootPrecision = outerReachedSet.getPrecision(pPathElementToReachedState.get(rootState));
    Precision reducedNewPrecision = wrappedReducer.getVariableReducedPrecision(Precisions.replaceByType(rootPrecision, newPrecision, newPrecision.getClass()), rootBlock);

    UnmodifiableReachedSet innerReachedSet = abstractStateToReachedSet.get(pPathElementToReachedState.get(rootState));
    Precision usedPrecision = innerReachedSet.getPrecision(innerReachedSet.getFirstState());

    //add precise key for new precision if needed
    if (!argCache.containsPreciseKey(reducedRootState, reducedNewPrecision, rootBlock)) {
      ReachedSet reachedSet = createInitialReachedSet(reducedRootState, reducedNewPrecision);
      argCache.put(reducedRootState, reducedNewPrecision, rootBlock, reachedSet);
    }

    boolean isNewPrecisionEntry = usedPrecision.equals(reducedNewPrecision);

    //fine, this block will not lead to any problems anymore, but maybe inner blocks will?
    //-> check other (inner) blocks on path
    boolean foundInnerUnpreciseEntries = false;
    while (!remainingPathElements.isEmpty()) {
      ARGState currentElement = remainingPathElements.pop();

      if (callNodes.contains(currentElement)) {
        ARGState currentReachedState = pPathElementToReachedState.get(currentElement);
        CFANode node = extractLocation(currentReachedState);
        Block currentBlock = partitioning.getBlockForCallNode(node);
        AbstractState reducedState = wrappedReducer.getVariableReducedState(currentReachedState, currentBlock, node);

        boolean removedUnpreciseInnerBlock = removeUnpreciseCacheEntriesOnPath(currentElement, reducedState, newPrecision, currentBlock, remainingPathElements, pPathElementToReachedState, callNodes, returnNodes, pathElementToOuterReachedSet, neededRemoveCachedSubtreeCalls);
        if (removedUnpreciseInnerBlock) {
          //System.out.println("Innner context of " + rootBlock + " removed some unprecise entry");
          //ok we indeed found an inner block that was unprecise
          if (isNewPrecisionEntry && !foundInnerUnpreciseEntries) {
            //if we are in a reached set that already uses the new precision and this is the first such entry we have to remove the subtree starting from currentElement in the rootReachedSet
            neededRemoveCachedSubtreeCalls.add(Pair.of(pPathElementToReachedState.get(rootState), currentReachedState));
            foundInnerUnpreciseEntries = true;
          }
        }
      }

      if (returnNodes.contains(currentElement)) {
        //our block ended. Leave..
        return foundInnerUnpreciseEntries || !isNewPrecisionEntry;
      }
    }

    return foundInnerUnpreciseEntries || !isNewPrecisionEntry;
  }


  private void removeCachedSubtree(ARGState rootState, ARGState removeElement, Precision newPrecision) {
    removeCachedSubtreeTimer.start();

    try {
      CFANode rootNode = extractLocation(rootState);

      logger.log(Level.FINER, "Remove cached subtree for ", removeElement, " (rootNode: ", rootNode, ") issued");

      Block rootSubtree = partitioning.getBlockForCallNode(rootNode);
      AbstractState reducedRootState = wrappedReducer.getVariableReducedState(rootState, rootSubtree, rootNode);
      ReachedSet reachedSet = abstractStateToReachedSet.get(rootState);

      if (!reachedSet.contains(removeElement)) {
        //apparently, removeElement was removed due to prior deletions
        return;
      }

      Precision removePrecision = reachedSet.getPrecision(removeElement);
      Precision newReducedRemovePrecision = null;
      if (newPrecision != null) {
        newReducedRemovePrecision = wrappedReducer.getVariableReducedPrecision(Precisions.replaceByType(removePrecision, newPrecision, newPrecision.getClass()), rootSubtree);
      }

      assert !removeElement.getParents().isEmpty();

      Precision reducedRootPrecision = reachedSet.getPrecision(reachedSet.getFirstState());
      argCache.removeReturnEntry(reducedRootState, reducedRootPrecision, rootSubtree);

      logger.log(Level.FINEST, "Removing subtree, adding a new cached entry, and removing the former cached entries");

      if (removeSubtree(reachedSet, removeElement, newReducedRemovePrecision)) {
        argCache.updatePrecisionForEntry(reducedRootState, reducedRootPrecision, rootSubtree, newReducedRemovePrecision);
      }

    }
    finally {
      removeCachedSubtreeTimer.stop();
    }
  }

  /**
   *
   * @param reachedSet
   * @param argElement
   * @param newPrecision
   * @return <code>true</code>, if the precision of the first element of the given reachedSet changed by this operation; <code>false</code>, otherwise.
   */
  private static boolean removeSubtree(ReachedSet reachedSet, ARGState argElement, Precision newPrecision) {
    ARGReachedSet argReachSet = new ARGReachedSet(reachedSet);
    boolean updateCacheNeeded = argElement.getParents().contains(reachedSet.getFirstState());
    removeSubtree(argReachSet, argElement, newPrecision);
    return updateCacheNeeded;
  }

  private static void removeSubtree(ARGReachedSet reachedSet, ARGState argElement) {
    reachedSet.removeSubtree(argElement);
  }

  private static void removeSubtree(ARGReachedSet reachedSet, ARGState argElement, Precision newPrecision) {
    if (newPrecision == null) {
      removeSubtree(reachedSet, argElement);
    } else {
      reachedSet.removeSubtree(argElement, newPrecision);
    }
  }

  private List<ARGState> trimPath(Path pPath, ARGState pElement) {
    List<ARGState> result = new ArrayList<ARGState>();

    for (Pair<ARGState, CFAEdge> e : pPath) {
      result.add(e.getFirst());
      if (e.getFirst().equals(pElement)) {
        return result;
      }
    }
    throw new IllegalArgumentException("Element " + pElement + " could not be found in path " + pPath + ".");
  }

  private Set<ARGState> getRelevantDefinitionNodes(List<ARGState> path) {
    Deque<ARGState> openCallElements = new ArrayDeque<ARGState>();
    Deque<Block> openSubtrees = new ArrayDeque<Block>();

    ARGState prevElement = path.get(1);
    for (ARGState currentElement : Iterables.skip(path, 2)) {
      CFANode currNode = extractLocation(currentElement);
      CFANode prevNode = extractLocation(prevElement);
      if (partitioning.isCallNode(prevNode)
          && !partitioning.getBlockForCallNode(prevNode).equals(openSubtrees.peek())) {
        if (!(isHeadOfMainFunction(prevNode))) {
          openCallElements.push(prevElement);
          openSubtrees.push(partitioning.getBlockForCallNode(prevNode));
        }

      }
      while (!openSubtrees.isEmpty()
           && openSubtrees.peek().isReturnNode(prevNode)
           && !openSubtrees.peek().getNodes().contains(currNode)) {
        openCallElements.pop();
        openSubtrees.pop();
      }
      prevElement = currentElement;
    }

    ARGState lastElement = path.get(path.size()-1);
    if (partitioning.isCallNode(extractLocation(lastElement))) {
      openCallElements.push(lastElement);
    }

    return new HashSet<ARGState>(openCallElements);
  }

  private Pair<Set<ARGState>, Set<ARGState>> getCallAndReturnNodes(Path path, Map<ARGState, UnmodifiableReachedSet> pathElementToOuterReachedSet, UnmodifiableReachedSet mainReachedSet, Map<ARGState, ARGState> pPathElementToReachedState) {
    Set<ARGState> callNodes = new HashSet<ARGState>();
    Set<ARGState> returnNodes = new HashSet<ARGState>();

    Deque<Block> openSubtrees = new ArrayDeque<Block>();

    Deque<UnmodifiableReachedSet> openReachedSets = new ArrayDeque<UnmodifiableReachedSet>();
    openReachedSets.push(mainReachedSet);

    ARGState prevElement = path.get(1).getFirst();
    for (Pair<ARGState, CFAEdge> currentElementPair : Iterables.skip(path, 2)) {
      ARGState currentElement = currentElementPair.getFirst();
      CFANode currNode = extractLocation(currentElement);
      CFANode prevNode = extractLocation(prevElement);

      pathElementToOuterReachedSet.put(prevElement, openReachedSets.peek());

      if (partitioning.isCallNode(prevNode)
          && !partitioning.getBlockForCallNode(prevNode).equals(openSubtrees.peek())) {
        if (!(isHeadOfMainFunction(prevNode))) {
          openSubtrees.push(partitioning.getBlockForCallNode(prevNode));
          openReachedSets.push(abstractStateToReachedSet.get(pPathElementToReachedState.get(prevElement)));
          callNodes.add(prevElement);
        }
      }

      while (!openSubtrees.isEmpty()
           && openSubtrees.peek().isReturnNode(prevNode)
           && !openSubtrees.peek().getNodes().contains(currNode)) {
        openSubtrees.pop();
        openReachedSets.pop();
        returnNodes.add(prevElement);
      }

      prevElement = currentElement;
    }

    ARGState lastElement = path.get(path.size()-1).getFirst();
    if (partitioning.isReturnNode(extractLocation(lastElement))) {
      returnNodes.add(lastElement);
    }
    pathElementToOuterReachedSet.put(lastElement, openReachedSets.peek());

    return Pair.of(callNodes, returnNodes);
  }

  //returns root of a subtree leading from the root element of the given reachedSet to the target state
  //subtree is represented using children and parents of ARGElements, where newTreeTarget is the ARGState
  //in the constructed subtree that represents target
  ARGState computeCounterexampleSubgraph(ARGState target, ARGReachedSet reachedSet, ARGState newTreeTarget, Map<ARGState, ARGState> pPathElementToReachedState) throws InterruptedException, RecursiveAnalysisFailedException {
    assert reachedSet.asReachedSet().contains(target);

    //start by creating ARGElements for each node needed in the tree
    Map<ARGState, ARGState> elementsMap = new HashMap<ARGState, ARGState>();
    Stack<ARGState> openElements = new Stack<ARGState>();
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
          elementsMap.put(parent, new ARGState(parent.getWrappedState(), null));
          pPathElementToReachedState.put(elementsMap.get(parent), parent);
          //and remember to explore the parent later
          openElements.push(parent);
        }
        CFAEdge edge = ABMARTUtils.getEdgeToChild(parent, currentElement);
        if (edge == null) {
          //this is a summarized call and thus an direct edge could not be found
          //we have the transfer function to handle this case, as our reachSet is wrong
          //(we have to use the cached ones)
          ARGState innerTree = computeCounterexampleSubgraph(parent, reachedSet.asReachedSet().getPrecision(parent), elementsMap.get(currentElement), pPathElementToReachedState);
          if (innerTree == null) {
            removeSubtree(reachedSet, parent);
            return null;
          }
          for (ARGState child : innerTree.getChildren()) {
            child.addParent(elementsMap.get(parent));
          }
          innerTree.removeFromARG();
        }
        else {
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
  private ARGState computeCounterexampleSubgraph(ARGState root, Precision rootPrecision, ARGState newTreeTarget, Map<ARGState, ARGState> pPathElementToReachedState) throws InterruptedException, RecursiveAnalysisFailedException {
    CFANode rootNode = extractLocation(root);
    Block rootSubtree = partitioning.getBlockForCallNode(rootNode);

    AbstractState reducedRootState = wrappedReducer.getVariableReducedState(root, rootSubtree, rootNode);
    ReachedSet reachSet = abstractStateToReachedSet.get(root);

    //we found the to the root and precision corresponding reach set
    //now try to find the target in the reach set
    ARGState targetARGState = (ARGState) expandedToReducedCache.get(pPathElementToReachedState.get(newTreeTarget));
    if (targetARGState.isDestroyed()) {
      logger.log(Level.FINE, "Target state refers to a destroyed ARGState, i.e., the cached subtree is outdated. Updating it.");
      return null;
    }
    assert reachSet.contains(targetARGState);
    //we found the target; now construct a subtree in the ARG starting with targetARTElement
    ARGState result = computeCounterexampleSubgraph(targetARGState, new ARGReachedSet(reachSet), newTreeTarget, pPathElementToReachedState);
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
    return wrappedTransfer.strengthen(pElement, pOtherElements, pCfaEdge, pPrecision);
  }

}
