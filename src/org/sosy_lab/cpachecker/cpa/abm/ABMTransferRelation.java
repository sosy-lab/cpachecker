/*
 *  CPAchecker is a tool for configurable software verification.
 *  This file is part of CPAchecker.
 *
 *  Copyright (C) 2007-2011  Dirk Beyer
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
import static org.sosy_lab.cpachecker.util.AbstractElements.extractLocation;
import static org.sosy_lab.cpachecker.util.AbstractElements.isTargetElement;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Deque;
import java.util.HashMap;
import java.util.HashSet;
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
import org.sosy_lab.cpachecker.cfa.objectmodel.CFAEdge;
import org.sosy_lab.cpachecker.cfa.objectmodel.CFAFunctionDefinitionNode;
import org.sosy_lab.cpachecker.cfa.objectmodel.CFANode;
import org.sosy_lab.cpachecker.core.algorithm.CPAAlgorithm;
import org.sosy_lab.cpachecker.core.interfaces.AbstractElement;
import org.sosy_lab.cpachecker.core.interfaces.Precision;
import org.sosy_lab.cpachecker.core.interfaces.Reducer;
import org.sosy_lab.cpachecker.core.interfaces.TransferRelation;
import org.sosy_lab.cpachecker.core.reachedset.ReachedSet;
import org.sosy_lab.cpachecker.core.reachedset.ReachedSetFactory;
import org.sosy_lab.cpachecker.cpa.art.ARTElement;
import org.sosy_lab.cpachecker.cpa.art.ARTReachedSet;
import org.sosy_lab.cpachecker.cpa.art.Path;
import org.sosy_lab.cpachecker.exceptions.CPAException;
import org.sosy_lab.cpachecker.exceptions.CPATransferException;
import org.sosy_lab.cpachecker.util.AbstractElements;
import org.sosy_lab.cpachecker.util.Precisions;

import com.google.common.collect.Iterables;


@Options(prefix="cpa.abm")
public class ABMTransferRelation implements TransferRelation {

  @Option(description="disable caching of abstract state spaces for blocks")
  private boolean NO_CACHING = false;

  private class AbstractElementHash {

    private final Object wrappedHash;
    private final Block context;

    private final AbstractElement predicateKey;
    private final Precision precisionKey;

    public AbstractElementHash(AbstractElement pPredicateKey, Precision pPrecisionKey, Block pContext) {
      wrappedHash = wrappedReducer.getHashCodeForElement(pPredicateKey, pPrecisionKey);
      context = checkNotNull(pContext);

      predicateKey = pPredicateKey;
      precisionKey = pPrecisionKey;
    }

    @Override
    public boolean equals(Object pObj) {
      if (!(pObj instanceof AbstractElementHash)) {
        return false;
      }
      AbstractElementHash other = (AbstractElementHash)pObj;
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
  }

  private class Cache<V> {

    private final Map<AbstractElementHash, V> cache = new HashMap<AbstractElementHash, V>();

    private AbstractElementHash getHashCode(AbstractElement predicateKey,
        Precision precisionKey, Block context) {

      return new AbstractElementHash(predicateKey, precisionKey, context);
    }

    private V put(AbstractElement predicateKey, Precision precisionKey, Block context, V item) {
      return cache.put(getHashCode(predicateKey, precisionKey, context), item);
    }

    private void remove(AbstractElement predicateKey, Precision precisionKey, Block context) {
      cache.remove(getHashCode(predicateKey, precisionKey, context));
    }

    /*private boolean containsKey(AbstractElement predicateKey, Precision precisionKey, Block context) {
      return cache.containsKey(getHashCode(predicateKey, precisionKey, context));
    }*/

    private V get(AbstractElement predicateKey, Precision precisionKey, Block context) {
      return cache.get(getHashCode(predicateKey, precisionKey, context));
    }

    private void findCacheMissCause(AbstractElement pPredicateKey, Precision pPrecisionKey, Block pContext) {
      AbstractElementHash searchKey = getHashCode(pPredicateKey, pPrecisionKey, pContext);
      for(AbstractElementHash cacheKey : cache.keySet()) {
        assert !searchKey.equals(cacheKey);
        //searchKey != cacheKey, check whether it is the same if we ignore the precision
        AbstractElementHash ignorePrecisionSearchKey = getHashCode(pPredicateKey, cacheKey.precisionKey, pContext);
        if(ignorePrecisionSearchKey.equals(cacheKey)) {
          precisionCausedMisses++;
          return;
        }
        //precision was not the cause. Check abstraction.
        AbstractElementHash ignoreAbsSearchKey = getHashCode(cacheKey.predicateKey, pPrecisionKey, pContext);
        if(ignoreAbsSearchKey.equals(cacheKey)) {
          abstractionCausedMisses++;
          return;
        }
      }
      noSimilarCausedMisses++;
    }

    private void clear() {
      cache.clear();
    }
  }


  private final Cache<ReachedSet> subgraphReachCache = new Cache<ReachedSet>();
  private final Cache<Collection<AbstractElement>> subgraphReturnCache = new Cache<Collection<AbstractElement>>();

  private Block currentBlock;
  private BlockPartitioning partitioning;
  private int depth = 0;

  private final LogManager logger;
  private final CPAAlgorithm algorithm;
  private final TransferRelation wrappedTransfer;
  private final ReachedSetFactory reachedSetFactory;
  private final Reducer wrappedReducer;

  private Map<AbstractElement, Precision> forwardPrecisionToExpandedPrecision;

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

  public ABMTransferRelation(Configuration pConfig, LogManager pLogger, ABMCPA abmCpa, ReachedSetFactory pReachedSetFactory) throws InvalidConfigurationException {
    pConfig.inject(this);
    logger = pLogger;
    algorithm = new CPAAlgorithm(abmCpa, logger);
    reachedSetFactory = pReachedSetFactory;
    wrappedTransfer = abmCpa.getWrappedCpa().getTransferRelation();
    wrappedReducer = abmCpa.getReducer();
    assert wrappedReducer != null;
  }


  void setForwardPrecisionToExpandedPrecision(
      Map<AbstractElement, Precision> pForwardPrecisionToExpandedPrecision) {
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
  public Collection<? extends AbstractElement> getAbstractSuccessors(
      AbstractElement pElement, Precision pPrecision, CFAEdge edge)
      throws CPATransferException, InterruptedException {

    forwardPrecisionToExpandedPrecision.clear();

    if (edge == null) {
      CFANode node = extractLocation(pElement);

      if (partitioning.isCallNode(node)) {
        //we have to start a recursive analysis
        if(partitioning.getBlockForCallNode(node).equals(currentBlock)) {
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
        Collection<Pair<AbstractElement, Precision>> reducedResult = performCompositeAnalysis(pElement, pPrecision, node);

        logger.log(Level.FINER, "Recursive analysis of depth", depth--, "finished");
        logger.log(Level.ALL, "Resulting elements:", reducedResult);

        List<AbstractElement> expandedResult = new ArrayList<AbstractElement>(reducedResult.size());
        for (Pair<AbstractElement, Precision> reducedPair: reducedResult) {
          AbstractElement reducedElement = reducedPair.getFirst();
          Precision reducedPrecision = reducedPair.getSecond();

          ARTElement expandedElement = (ARTElement)wrappedReducer.getVariableExpandedElement(pElement, currentBlock, reducedElement);
          Precision expandedPrecision = wrappedReducer.getVariableExpandedPrecision(pPrecision, outerSubtree, reducedPrecision);

          expandedElement.addParent((ARTElement)pElement);
          expandedResult.add(expandedElement);

          forwardPrecisionToExpandedPrecision.put(expandedElement, expandedPrecision);
        }

        logger.log(Level.ALL, "Expanded results:", expandedResult);

        currentBlock = outerSubtree;

        return expandedResult;
      }
      else {
        List<AbstractElement> result = new ArrayList<AbstractElement>();
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

  private Collection<? extends AbstractElement> getAbstractSuccessors0(AbstractElement pElement, Precision pPrecision, CFAEdge edge) throws CPATransferException, InterruptedException {
    assert edge != null;

    CFANode currentNode = edge.getPredecessor();

    Block currentNodeBlock = partitioning.getBlockForReturnNode(currentNode);
    if(currentNodeBlock != null && !currentBlock.equals(currentNodeBlock) && currentNodeBlock.getNodes().contains(edge.getSuccessor())) {
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
    return currentNode instanceof CFAFunctionDefinitionNode && currentNode.getNumEnteringEdges() == 0;
  }


  private Collection<Pair<AbstractElement, Precision>> performCompositeAnalysis(AbstractElement initialElement, Precision initialPrecision, CFANode node) throws InterruptedException, RecursiveAnalysisFailedException {
    try {
      AbstractElement reducedInitialElement = wrappedReducer.getVariableReducedElement(initialElement, currentBlock, node);
      Precision reducedInitialPrecision = wrappedReducer.getVariableReducedPrecision(initialPrecision, currentBlock);
      ReachedSet reached = subgraphReachCache.get(reducedInitialElement, reducedInitialPrecision, currentBlock);

      if (!NO_CACHING) {
        Collection<AbstractElement> returnElements = subgraphReturnCache.get(reducedInitialElement, reducedInitialPrecision, currentBlock);

        if (returnElements != null) {
          assert reached != null;
          fullCacheHits++;
          return imbueAbstractElementsWithPrecision(reached, returnElements);
        }
      }

      if (reached != null) {
        //at least we have partly computed reach set cached
        partialCacheHits++;
      } else {
        //compute the subgraph specification from scratch
        cacheMisses++;

        if(gatherCacheMissStatistics) {
          subgraphReachCache.findCacheMissCause(reducedInitialElement, reducedInitialPrecision, currentBlock);
        }

        reached = createInitialReachedSet(reducedInitialElement, reducedInitialPrecision);
        subgraphReachCache.put(reducedInitialElement, reducedInitialPrecision, currentBlock, reached);
      }

      algorithm.run(reached);

      List<AbstractElement> returnElements;

      // if the element is an error element
      AbstractElement lastElement = reached.getLastElement();
      if (isTargetElement(lastElement)) {
        //found a target element inside a recursive subgraph call
        //this needs to be propagated to outer subgraph (till main is reached)
        returnElements = Collections.singletonList(lastElement);

      } else {

        returnElements = new ArrayList<AbstractElement>();
        for(CFANode returnNode : currentBlock.getReturnNodes()) {
          Iterables.addAll(returnElements, AbstractElements.filterLocation(reached, returnNode));
        }
      }

      subgraphReturnCache.put(reducedInitialElement, reducedInitialPrecision, currentBlock, returnElements);

      return imbueAbstractElementsWithPrecision(reached, returnElements);
    } catch (CPAException e) {
      throw new RecursiveAnalysisFailedException(e);
    }
  }


  private List<Pair<AbstractElement, Precision>> imbueAbstractElementsWithPrecision(
      ReachedSet pReached, Collection<AbstractElement> pElements) {
    List<Pair<AbstractElement, Precision>> result = new ArrayList<Pair<AbstractElement,Precision>>();
    for(AbstractElement ele : pElements) {
      result.add(Pair.of(ele, pReached.getPrecision(ele)));
    }
    return result;
  }

  private ReachedSet createInitialReachedSet(AbstractElement reducedInitialElement, Precision initialPredicatePrecision) {
    ReachedSet reached = reachedSetFactory.create();
    reached.add(reducedInitialElement, initialPredicatePrecision);
    return reached;
  }

  void removeSubtree(ARTReachedSet mainReachedSet, Path pPath, ARTElement element, Precision newPrecision, Map<ARTElement, ARTElement> pPathElementToReachedElement) {
    removeSubtreeTimer.start();

    List<ARTElement> path = trimPath(pPath, element);
    assert path.get(path.size()-1).equals(element);

    Set<ARTElement> relevantCallNodes = getRelevantDefinitionNodes(path);

    ARTElement lastElement = null;
    Precision lastPrecision = null;
    //iterate from root to element and remove all subtrees for subgraph calls
    for(ARTElement pathElement : Iterables.skip(path, 1)) {
      if(pathElement.equals(element)) {
        break;
      }

      if(relevantCallNodes.contains(pathElement)) {
        ARTElement currentElement = pPathElementToReachedElement.get(pathElement);

        if (lastElement == null) {
          lastPrecision = mainReachedSet.asReachedSet().getPrecision(currentElement);
          removeSubtree(mainReachedSet, currentElement);
        } else {
          lastPrecision = removeCachedSubtree(lastElement, lastPrecision, currentElement, null);
        }

        lastElement = currentElement;
      }
    }

    if(lastElement == null) {
      removeSubtree(mainReachedSet, pPathElementToReachedElement.get(element), newPrecision);
    } else {
      removeCachedSubtree(lastElement, lastPrecision, pPathElementToReachedElement.get(element), newPrecision);
    }

    removeSubtreeTimer.stop();
  }

  private Precision removeCachedSubtree(ARTElement rootElement, Precision rootPrecision, ARTElement removeElement, Precision newPrecision) {
    removeCachedSubtreeTimer.start();

    try {
      CFANode rootNode = rootElement.retrieveLocationElement().getLocationNode();

      logger.log(Level.FINER, "Remove cached subtree for ", removeElement, " (rootNode: ", rootNode, ") issued");

      Block rootSubtree = partitioning.getBlockForCallNode(rootNode);

      AbstractElement reducedRootElement = wrappedReducer.getVariableReducedElement(rootElement, rootSubtree, rootNode);
      Precision reducedRootPrecision = wrappedReducer.getVariableReducedPrecision(rootPrecision, rootSubtree);

      ReachedSet reachedSet = subgraphReachCache.get(reducedRootElement, reducedRootPrecision, rootSubtree);

      Precision removePrecision = reachedSet.getPrecision(removeElement);
      Precision newReducedRemovePrecision = null;
      if(newPrecision != null) {
        newReducedRemovePrecision = wrappedReducer.getVariableReducedPrecision(Precisions.replaceByType(removePrecision, newPrecision, newPrecision.getClass()), rootSubtree);
      }

      assert !removeElement.getParents().isEmpty();

      logger.log(Level.FINEST, "Removing subtree, adding a new cached entry, and removing the former cached entries");
      removeSubtree(reachedSet, removeElement, newReducedRemovePrecision);

      subgraphReturnCache.remove(reducedRootElement, reducedRootPrecision, rootSubtree);

      return removePrecision;
    }
    finally {
      removeCachedSubtreeTimer.stop();
    }
  }

  private static void removeSubtree(ReachedSet reachedSet, ARTElement artElement, Precision newPrecision) {
    ARTReachedSet artReachSet = new ARTReachedSet(reachedSet);
    removeSubtree(artReachSet, artElement, newPrecision);
  }

  private static void removeSubtree(ARTReachedSet reachedSet, ARTElement artElement) {
    reachedSet.removeSubtree(artElement);
  }

  private static void removeSubtree(ARTReachedSet reachedSet, ARTElement artElement, Precision newPrecision) {
    if(newPrecision == null) {
      removeSubtree(reachedSet, artElement);
    } else {
      reachedSet.removeSubtree(artElement, newPrecision);
    }
  }

  private List<ARTElement> trimPath(Path pPath, ARTElement pElement) {
    List<ARTElement> result = new ArrayList<ARTElement>();

    for (Pair<ARTElement, CFAEdge> e : pPath) {
      result.add(e.getFirst());
      if (e.getFirst().equals(pElement)) {
        return result;
      }
    }
    throw new IllegalArgumentException("Element " + pElement + " could not be found in path " + pPath + ".");
  }

  private Set<ARTElement> getRelevantDefinitionNodes(List<ARTElement> path) {
    Deque<ARTElement> openCallElements = new ArrayDeque<ARTElement>();
    Deque<Block> openSubtrees = new ArrayDeque<Block>();

    ARTElement prevElement = path.get(1);
    for (ARTElement currentElement : Iterables.skip(path, 2)) {
      CFANode currNode = currentElement.retrieveLocationElement().getLocationNode();
      CFANode prevNode = prevElement.retrieveLocationElement().getLocationNode();
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

    ARTElement lastElement = path.get(path.size()-1);
    if(partitioning.isCallNode(lastElement.retrieveLocationElement().getLocationNode())) {
      openCallElements.push(lastElement);
    }

    return new HashSet<ARTElement>(openCallElements);
  }

  //returns root of a subtree leading from the root element of the given reachedSet to the target element
  //subtree is represented using children and parents of ARTElements, where newTreeTarget is the ARTElement
  //in the constructed subtree that represents target
  ARTElement computeCounterexampleSubgraph(ARTElement target, ARTReachedSet reachedSet, ARTElement newTreeTarget, Map<ARTElement, ARTElement> pPathElementToReachedElement) throws InterruptedException, RecursiveAnalysisFailedException {
    //start by creating ARTElements for each node needed in the tree
    Map<ARTElement, ARTElement> elementsMap = new HashMap<ARTElement, ARTElement>();
    Stack<ARTElement> openElements = new Stack<ARTElement>();
    ARTElement root = null;

    pPathElementToReachedElement.put(newTreeTarget, target);
    elementsMap.put(target, newTreeTarget);
    openElements.push(target);
    while(!openElements.empty()) {
      ARTElement currentElement = openElements.pop();
      for(ARTElement parent : currentElement.getParents()) {
        if(!elementsMap.containsKey(parent)) {
          //create node for parent in the new subtree
          elementsMap.put(parent, new ARTElement(parent.getWrappedElement(), null));
          pPathElementToReachedElement.put(elementsMap.get(parent), parent);
          //and remember to explore the parent later
          openElements.push(parent);
        }
        CFAEdge edge = ABMARTUtils.getEdgeToChild(parent, currentElement);
        if(edge == null) {
          //this is a summarized call and thus an direct edge could not be found
          //we have the transfer function to handle this case, as our reachSet is wrong
          //(we have to use the cached ones)
          ARTElement innerTree = computeCounterexampleSubgraph(parent, reachedSet.getPrecision(parent), elementsMap.get(currentElement), pPathElementToReachedElement);
          if(innerTree == null) {
            removeSubtree(reachedSet, parent);
            return null;
          }
          for(ARTElement child : innerTree.getChildren()) {
            child.addParent(elementsMap.get(parent));
          }
          innerTree.removeFromART();
        }
        else {
          //normal edge
          //create an edge from parent to current
          elementsMap.get(currentElement).addParent(elementsMap.get(parent));
        }
      }
      if(currentElement.getParents().isEmpty()) {
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
  private ARTElement computeCounterexampleSubgraph(ARTElement root, Precision rootPrecision, ARTElement newTreeTarget, Map<ARTElement, ARTElement> pPathElementToReachedElement) throws InterruptedException, RecursiveAnalysisFailedException {
    CFANode rootNode = root.retrieveLocationElement().getLocationNode();
    Block rootSubtree = partitioning.getBlockForCallNode(rootNode);

    AbstractElement reducedRootElement = wrappedReducer.getVariableReducedElement(root, rootSubtree, rootNode);
    Precision reducedRootPrecision = wrappedReducer.getVariableReducedPrecision(rootPrecision, rootSubtree);
    ReachedSet reachSet = subgraphReachCache.get(reducedRootElement, reducedRootPrecision, rootSubtree);
    //we found the to the root and precision corresponding reach set
    //now try to find the target in the reach set
    ARTElement targetARTElement = (ARTElement)wrappedReducer.getVariableReducedElement(pPathElementToReachedElement.get(newTreeTarget), rootSubtree, rootNode);
    if(targetARTElement.isDestroyed()) {
      logger.log(Level.FINE, "Target element refers to a destroyed ARTElement, i.e., the cached subtree is outdated. Updating it.");
      return null;
    }
    //we found the target; now construct a subtree in the ART starting with targetARTElement
    ARTElement result = computeCounterexampleSubgraph(targetARTElement, new ARTReachedSet(reachSet), newTreeTarget, pPathElementToReachedElement);
    if(result == null) {
      //enforce recomputation to update cached subtree
      subgraphReturnCache.remove(reducedRootElement, reducedRootPrecision, rootSubtree);
    }
    return result;
  }

  void clearCaches() {
    subgraphReachCache.clear();
    subgraphReturnCache.clear();
  }

  Pair<Block, ReachedSet> getCachedReachedSet(ARTElement root, Precision rootPrecision) {
    CFANode rootNode = root.retrieveLocationElement().getLocationNode();
    Block rootSubtree = partitioning.getBlockForCallNode(rootNode);

    AbstractElement reducedRootElement = wrappedReducer.getVariableReducedElement(root, rootSubtree, rootNode);
    Precision reducedRootPrecision = wrappedReducer.getVariableReducedPrecision(rootPrecision, rootSubtree);
    ReachedSet reachSet = subgraphReachCache.get(reducedRootElement, reducedRootPrecision, rootSubtree);
    assert reachSet != null;
    return Pair.of(rootSubtree, reachSet);
  }

  @Override
  public Collection<? extends AbstractElement> strengthen(
      AbstractElement pElement, List<AbstractElement> pOtherElements,
      CFAEdge pCfaEdge, Precision pPrecision) throws CPATransferException,
      InterruptedException {
    return wrappedTransfer.strengthen(pElement, pOtherElements, pCfaEdge, pPrecision);
  }

}
