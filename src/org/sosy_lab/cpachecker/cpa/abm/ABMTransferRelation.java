/*
 *  CPAchecker is a tool for configurable software verification.
 *  This file is part of CPAchecker.
 *
 *  Copyright (C) 2007-2013  Dirk Beyer
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
import java.util.LinkedHashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.Stack;
import java.util.logging.Level;

import org.sosy_lab.common.LogManager;
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
import org.sosy_lab.cpachecker.core.reachedset.UnmodifiableReachedSet;
import org.sosy_lab.cpachecker.cpa.arg.ARGPath;
import org.sosy_lab.cpachecker.cpa.arg.ARGReachedSet;
import org.sosy_lab.cpachecker.cpa.arg.ARGState;
import org.sosy_lab.cpachecker.cpa.predicate.PredicateAbstractState;
import org.sosy_lab.cpachecker.exceptions.CPAException;
import org.sosy_lab.cpachecker.exceptions.CPATransferException;
import org.sosy_lab.cpachecker.util.AbstractStates;
import org.sosy_lab.cpachecker.util.Precisions;

import com.google.common.collect.HashMultimap;
import com.google.common.collect.Iterables;
import com.google.common.collect.Multimap;

@Options(prefix = "cpa.abm")
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
      if (!(pObj instanceof AbstractStateHash)) { return false; }
      AbstractStateHash other = (AbstractStateHash) pObj;
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

    private final Map<AbstractStateHash, ReachedSet> preciseReachedCache = new HashMap<>();
    private final Map<AbstractStateHash, ReachedSet> unpreciseReachedCache =
        new HashMap<>();

    private final Map<AbstractStateHash, Collection<AbstractState>> returnCache = new HashMap<>();
    private final Map<AbstractStateHash, ARGState> blockARGCache = new HashMap<>();

    private ARGState lastAnalyzedBlock = null;

    private AbstractStateHash getHashCode(AbstractState predicateKey, Precision precisionKey, Block context) {
      return new AbstractStateHash(predicateKey, precisionKey, context);
    }

    private void put(AbstractState predicateKey, Precision precisionKey, Block context, ReachedSet item) {
      AbstractStateHash hash = getHashCode(predicateKey, precisionKey, context);
      assert !preciseReachedCache.containsKey(hash);
      preciseReachedCache.put(hash, item);
    }

    private void put(AbstractState predicateKey, Precision precisionKey, Block context, Collection<AbstractState> item,
        ARGState rootOfBlock) {
      AbstractStateHash hash = getHashCode(predicateKey, precisionKey, context);
      assert allStatesContainedInReachedSet(item, preciseReachedCache.get(hash));
      returnCache.put(hash, item);
      blockARGCache.put(hash, rootOfBlock);
      setLastAnalyzedBlock(hash);
    }

    private boolean allStatesContainedInReachedSet(Collection<AbstractState> pElements, ReachedSet reached) {
      for (AbstractState e : pElements) {
        if (!reached.contains(e)) { return false; }
      }
      return true;
    }

    private void removeReturnEntry(AbstractState predicateKey, Precision precisionKey, Block context) {
      returnCache.remove(getHashCode(predicateKey, precisionKey, context));
    }

    public void removeBlockEntry(AbstractState predicateKey, Precision precisionKey, Block context) {
      blockARGCache.remove(getHashCode(predicateKey, precisionKey, context));
    }

    private Pair<ReachedSet, Collection<AbstractState>> get(AbstractState predicateKey, Precision precisionKey,
        Block context) {
      AbstractStateHash hash = getHashCode(predicateKey, precisionKey, context);

      ReachedSet result = preciseReachedCache.get(hash);
      if (result != null) {
        setLastAnalyzedBlock(hash);
        return Pair.of(result, returnCache.get(hash));
      }

      if (aggressiveCaching) {
        result = unpreciseReachedCache.get(hash);
        if (result != null) {
          setLastAnalyzedBlock(getHashCode(predicateKey, result.getPrecision(result.getFirstState()), context));
          return Pair.of(result,
              returnCache.get(getHashCode(predicateKey, result.getPrecision(result.getFirstState()), context)));
        }

        //search for similar entry
        Pair<ReachedSet, Collection<AbstractState>> pair = lookForSimilarState(predicateKey, precisionKey, context);
        if (pair != null) {
          //found similar element, use this
          unpreciseReachedCache.put(hash, pair.getFirst());
          setLastAnalyzedBlock(getHashCode(predicateKey, pair.getFirst().getPrecision(pair.getFirst().getFirstState()),
              context));
          return pair;
        }
      }

      lastAnalyzedBlock = null;
      return Pair.of(null, null);
    }

    private void setLastAnalyzedBlock(AbstractStateHash pHash) {
      if (PCCInformation.isPCCEnabled()) {
        lastAnalyzedBlock = blockARGCache.get(pHash);
      }
    }

    private ARGState getLastAnalyzedBlock() {
      return lastAnalyzedBlock;
    }

    private Pair<ReachedSet, Collection<AbstractState>> lookForSimilarState(AbstractState pPredicateKey,
        Precision pPrecisionKey, Block pContext) {
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
              result =
                  Pair.of(preciseReachedCache.get(ignorePrecisionSearchKey), returnCache.get(ignorePrecisionSearchKey));
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

    public void updatePrecisionForEntry(AbstractState predicateKey, Precision precisionKey, Block context,
        Precision newPrecisionKey) {
      AbstractStateHash hash = getHashCode(predicateKey, precisionKey, context);
      ReachedSet reachedSet = preciseReachedCache.get(hash);
      if (reachedSet != null) {
        preciseReachedCache.remove(hash);
        preciseReachedCache.put(getHashCode(predicateKey, newPrecisionKey, context), reachedSet);
      }
    }

    public Collection<ReachedSet> getAllCachedReachedStates() {
      return preciseReachedCache.values();
    }
  }

  @Options
  private static class PCCInformation {

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

  @Option(description = "if enabled, cache queries also consider blocks with non-matching precision for reuse.")
  private boolean aggressiveCaching = true;

  private final Cache argCache = new Cache();

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
  private final ABMPrecisionAdjustment prec;
  private final ABMCPA abmCPA;
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
  int abstractionCausedMisses = 0;
  int precisionCausedMisses = 0;
  int noSimilarCausedMisses = 0;

  final Timer hashingTimer = new Timer();
  final Timer equalsTimer = new Timer();
  final Timer recomputeARTTimer = new Timer();
  final Timer removeCachedSubtreeTimer = new Timer();
  final Timer removeSubtreeTimer = new Timer();
  final Timer searchingTimer = new Timer();



  public ABMTransferRelation(Configuration pConfig, LogManager pLogger, ABMCPA abmCpa, ProofChecker wrappedChecker,
      ReachedSetFactory pReachedSetFactory, ShutdownNotifier pShutdownNotifier) throws InvalidConfigurationException {
    pConfig.inject(this);
    logger = pLogger;
    algorithmFactory = new CPAAlgorithmFactory(abmCpa, logger, pConfig, pShutdownNotifier);
    reachedSetFactory = pReachedSetFactory;
    wrappedTransfer = abmCpa.getWrappedCpa().getTransferRelation();
    wrappedReducer = abmCpa.getReducer();
    prec = abmCpa.getPrecisionAdjustment();
    PCCInformation.instantiate(pConfig);
    abmCPA = abmCpa;
    wrappedProofChecker = wrappedChecker;

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

          if (reducedState == ABMARGBlockStartState.getDummy()) {
            ((ABMARGBlockStartState)reducedState).addParent((ARGState) pElement);
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
            (AbstractState) ABMARGBlockStartState.createDummy(reducedInitialState),
            reducedInitialPrecision)); //dummy element
      } else {
        returnElements = AbstractStates.filterLocations(reached, currentBlock.getReturnNodes())
            .toList();
      }

      ARGState rootOfBlock = null;
      if (PCCInformation.isPCCEnabled()) {
        if (!(reached.getFirstState() instanceof ARGState)) { throw new CPATransferException(
            "Cannot build proof, ARG, for ABM analysis."); }
        rootOfBlock = ABMARGUtils.copyARG((ARGState) reached.getFirstState());
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
        if (!(elem instanceof ABMARGBlockStartState)) {
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
    if (!(pElem instanceof ABMARGBlockStartState) && PCCInformation.isPCCEnabled() && pElem instanceof ARGState) { return createAdditionalInfo((ARGState) pElem); }
    return pElem;
  }

  private ARGState createAdditionalInfo(ARGState pElem) {
    CFANode node = AbstractStates.extractLocation(pElem);
    if (partitioning.isCallNode(node) && !partitioning.getBlockForCallNode(node).equals(currentBlock)) {
      ABMARGBlockStartState replaceWith = new ABMARGBlockStartState(pElem.getWrappedState(), null);
      replaceInARG(pElem, replaceWith);
      return replaceWith;
    }
    return pElem;
  }

  private void replaceInARG(ARGState toReplace, ARGState replaceWith) {
    for (ARGState p : toReplace.getParents()) {
      replaceWith.addParent(p);
    }
    for (ARGState c : toReplace.getChildren()) {
      c.addParent(replaceWith);
    }
    if (toReplace.isCovered()) {
      replaceWith.setCovered(toReplace.getCoveringState());
    }
    List<ARGState> willCover = new ArrayList<>(toReplace.getCoveredByThis().size());
    for (ARGState cov : toReplace.getCoveredByThis()) {
      willCover.add(cov);
    }
    toReplace.removeFromARG();
    for (ARGState cov : willCover) {
      cov.setCovered(replaceWith);
    }
  }

  private void addBlockAnalysisInfo(AbstractState pElement) throws CPATransferException {
    if (PCCInformation.isPCCEnabled()) {
      if (argCache.getLastAnalyzedBlock() == null || !(pElement instanceof ABMARGBlockStartState)) { throw new CPATransferException(
          "Cannot build proof, ARG, for ABM analysis."); }
      PredicateAbstractState pred = extractStateByType(pElement, PredicateAbstractState.class);
      if (pred == null) {
        ((ABMARGBlockStartState) pElement).setAnalyzedBlock(argCache.getLastAnalyzedBlock());
      } else {
        ((ABMARGBlockStartState) pElement).setAnalyzedBlock(argCache.getLastAnalyzedBlock());
      }
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

    List<ARGState> path = trimPath(pPath, element);
    assert path.get(path.size() - 1).equals(element);

    Set<ARGState> relevantCallNodes = getRelevantDefinitionNodes(path);

    Set<Pair<ARGReachedSet, ARGState>> neededRemoveSubtreeCalls = new LinkedHashSet<>();
    Set<Pair<ARGState, ARGState>> neededRemoveCachedSubtreeCalls = new LinkedHashSet<>();

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
      ensureExactCacheHitsOnPath(mainReachedSet, pPath, element, pNewPrecisions, pPathElementToReachedState,
          neededRemoveCachedSubtreeCalls);
    }

    for (Pair<ARGReachedSet, ARGState> removeSubtreeArguments : neededRemoveSubtreeCalls) {
      removeSubtree(removeSubtreeArguments.getFirst(), removeSubtreeArguments.getSecond());
    }

    for (Pair<ARGState, ARGState> removeCachedSubtreeArguments : neededRemoveCachedSubtreeCalls) {
      removeCachedSubtree(removeCachedSubtreeArguments.getFirst(), removeCachedSubtreeArguments.getSecond(), null, pNewPrecisionTypes);
    }

    if (lastElement == null) {
      removeSubtree(mainReachedSet, pPathElementToReachedState.get(element), pNewPrecisions, pNewPrecisionTypes);
    } else {
      removeCachedSubtree(lastElement, pPathElementToReachedState.get(element), pNewPrecisions, pNewPrecisionTypes);
    }

    removeSubtreeTimer.stop();
  }

  private void ensureExactCacheHitsOnPath(ARGReachedSet mainReachedSet, ARGPath pPath, ARGState pElement,
      List<Precision> pNewPrecisions, Map<ARGState, ARGState> pPathElementToReachedState,
      Set<Pair<ARGState, ARGState>> neededRemoveCachedSubtreeCalls) {
    Map<ARGState, UnmodifiableReachedSet> pathElementToOuterReachedSet = new HashMap<>();
    Pair<Set<ARGState>, Set<ARGState>> pair =
        getCallAndReturnNodes(pPath, pathElementToOuterReachedSet, mainReachedSet.asReachedSet(),
            pPathElementToReachedState);
    Set<ARGState> callNodes = pair.getFirst();
    Set<ARGState> returnNodes = pair.getSecond();

    Deque<ARGState> remainingPathElements = new LinkedList<>();
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

          removeUnpreciseCacheEntriesOnPath(currentElement, reducedState, pNewPrecisions, currentBlock,
              remainingPathElements, pPathElementToReachedState, callNodes, returnNodes, pathElementToOuterReachedSet,
              neededRemoveCachedSubtreeCalls);
        }
      }
    }
  }

  private boolean removeUnpreciseCacheEntriesOnPath(ARGState rootState, AbstractState reducedRootState,
      List<Precision> pNewPrecisions, Block rootBlock, Deque<ARGState> remainingPathElements,
      Map<ARGState, ARGState> pPathElementToReachedState, Set<ARGState> callNodes, Set<ARGState> returnNodes,
      Map<ARGState, UnmodifiableReachedSet> pathElementToOuterReachedSet,
      Set<Pair<ARGState, ARGState>> neededRemoveCachedSubtreeCalls) {
    UnmodifiableReachedSet outerReachedSet = pathElementToOuterReachedSet.get(rootState);

    Precision rootPrecision = outerReachedSet.getPrecision(pPathElementToReachedState.get(rootState));

    for (int i = 0; i < pNewPrecisions.size(); i++) {
      rootPrecision = Precisions.replaceByType(rootPrecision, pNewPrecisions.get(i), pNewPrecisions.get(i).getClass());
    }
    Precision reducedNewPrecision =
        wrappedReducer.getVariableReducedPrecision(
            rootPrecision, rootBlock);

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

        boolean removedUnpreciseInnerBlock =
            removeUnpreciseCacheEntriesOnPath(currentElement, reducedState, pNewPrecisions, currentBlock,
                remainingPathElements, pPathElementToReachedState, callNodes, returnNodes,
                pathElementToOuterReachedSet, neededRemoveCachedSubtreeCalls);
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


  private void removeCachedSubtree(ARGState rootState, ARGState removeElement,
      List<Precision> pNewPrecisions,
      List<Class<? extends Precision>> pPrecisionTypes) {
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
      ArrayList<Precision> newReducedRemovePrecision = null;
      if (pNewPrecisions != null) {
        newReducedRemovePrecision = new ArrayList<>(1);

        for (int i = 0; i < pNewPrecisions.size(); i++) {
          removePrecision = Precisions.replaceByType(removePrecision, pNewPrecisions.get(i), pPrecisionTypes.get(i));
        }

        newReducedRemovePrecision.add(wrappedReducer.getVariableReducedPrecision(removePrecision, rootSubtree));
        pPrecisionTypes = new ArrayList<>();
        pPrecisionTypes.add(newReducedRemovePrecision.get(0).getClass());
      }

      assert !removeElement.getParents().isEmpty();

      Precision reducedRootPrecision = reachedSet.getPrecision(reachedSet.getFirstState());
      argCache.removeReturnEntry(reducedRootState, reducedRootPrecision, rootSubtree);
      argCache.removeBlockEntry(reducedRootState, reducedRootPrecision, rootSubtree);

      logger.log(Level.FINEST, "Removing subtree, adding a new cached entry, and removing the former cached entries");

      if (removeSubtree(reachedSet, removeElement, newReducedRemovePrecision, pPrecisionTypes)) {
        argCache
            .updatePrecisionForEntry(reducedRootState, reducedRootPrecision, rootSubtree, newReducedRemovePrecision.get(0));
      }

    } finally {
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
  private static boolean removeSubtree(ReachedSet reachedSet, ARGState argElement,
      List<Precision> newPrecisions, List<Class<? extends Precision>> pPrecisionTypes) {
    ARGReachedSet argReachSet = new ARGReachedSet(reachedSet);
    boolean updateCacheNeeded = argElement.getParents().contains(reachedSet.getFirstState());
    removeSubtree(argReachSet, argElement, newPrecisions, pPrecisionTypes);
    return updateCacheNeeded;
  }

  private static void removeSubtree(ARGReachedSet reachedSet, ARGState argElement) {
    reachedSet.removeSubtree(argElement);
  }

  private static void removeSubtree(ARGReachedSet reachedSet, ARGState argElement,
      List<Precision> newPrecisions, List<Class<? extends Precision>> pPrecisionTypes) {
    if (newPrecisions == null || newPrecisions.size() == 0) {
      removeSubtree(reachedSet, argElement);
    } else {
      reachedSet.removeSubtree(argElement, newPrecisions, pPrecisionTypes);
    }
  }

  private List<ARGState> trimPath(ARGPath pPath, ARGState pElement) {
    List<ARGState> result = new ArrayList<>();

    for (Pair<ARGState, CFAEdge> e : pPath) {
      result.add(e.getFirst());
      if (e.getFirst().equals(pElement)) { return result; }
    }
    throw new IllegalArgumentException("Element " + pElement + " could not be found in path " + pPath + ".");
  }

  private Set<ARGState> getRelevantDefinitionNodes(List<ARGState> path) {
    Deque<ARGState> openCallElements = new ArrayDeque<>();
    Deque<Block> openSubtrees = new ArrayDeque<>();

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

    ARGState lastElement = path.get(path.size() - 1);
    if (partitioning.isCallNode(extractLocation(lastElement))) {
      openCallElements.push(lastElement);
    }

    return new HashSet<>(openCallElements);
  }

  private Pair<Set<ARGState>, Set<ARGState>> getCallAndReturnNodes(ARGPath path,
      Map<ARGState, UnmodifiableReachedSet> pathElementToOuterReachedSet, UnmodifiableReachedSet mainReachedSet,
      Map<ARGState, ARGState> pPathElementToReachedState) {
    Set<ARGState> callNodes = new HashSet<>();
    Set<ARGState> returnNodes = new HashSet<>();

    Deque<Block> openSubtrees = new ArrayDeque<>();

    Deque<UnmodifiableReachedSet> openReachedSets = new ArrayDeque<>();
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

    ARGState lastElement = path.get(path.size() - 1).getFirst();
    if (partitioning.isReturnNode(extractLocation(lastElement))) {
      returnNodes.add(lastElement);
    }
    pathElementToOuterReachedSet.put(lastElement, openReachedSets.peek());

    return Pair.of(callNodes, returnNodes);
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
        CFAEdge edge = ABMARGUtils.getEdgeToChild(parent, currentElement);
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

  public boolean areAbstractSuccessors0(AbstractState pState, CFAEdge pCfaEdge,
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
        if (!(pState instanceof ABMARGBlockStartState)
            || ((ABMARGBlockStartState) pState).getAnalyzedBlock() == null
            || !abmCPA.isCoveredBy(wrappedReducer.getVariableReducedStateForProofChecking(pState, analyzedBlock, node),
                ((ABMARGBlockStartState) pState).getAnalyzedBlock())) { return false; }
      } catch (CPAException e) {
        throw new CPATransferException("Missing information about block whose analysis is expected to be started at "
            + pState);
      }
      try {
        Collection<ARGState> endOfBlock;
        Pair<ARGState, Block> key = Pair.of(((ABMARGBlockStartState) pState).getAnalyzedBlock(), analyzedBlock);
        if (correctARGsForBlocks != null && correctARGsForBlocks.containsKey(key)) {
          endOfBlock = correctARGsForBlocks.get(key);
        } else {
          Pair<Boolean, Collection<ARGState>> result =
              checkARGBlock(((ABMARGBlockStartState) pState).getAnalyzedBlock(), analyzedBlock);
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
            if (abmCPA.isCoveredBy(expandedState, next)) {
              successorExists = true;
              notFoundSuccessors.remove(next);
            }
          }
          if (!successorExists) { return false; }
        }

        if (!notFoundSuccessors.isEmpty()) { return false; }

      } catch (CPAException e) {
        throw new CPATransferException("Checking ARG with root " + ((ABMARGBlockStartState) pState).getAnalyzedBlock()
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
          if (!abmCPA.isCoveredBy(current, current.getCoveringState())) {
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
