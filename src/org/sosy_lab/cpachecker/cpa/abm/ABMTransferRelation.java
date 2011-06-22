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
import org.sosy_lab.common.Triple;
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
import org.sosy_lab.cpachecker.core.reachedset.UnmodifiableReachedSet;
import org.sosy_lab.cpachecker.cpa.art.ARTCPA;
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

    public AbstractElementHash(Object pWrappedHash,
        Block pContext) {
      wrappedHash = checkNotNull(pWrappedHash);
      context = checkNotNull(pContext);
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

      return new AbstractElementHash(wrappedReducer.getHashCodeForElement(predicateKey, precisionKey), context);
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

    private void clear() {
      cache.clear();
    }
  }


  private final Cache<ReachedSet> subgraphReachCache = new Cache<ReachedSet>();
  private final Cache<Collection<AbstractElement>> subgraphReturnCache = new Cache<Collection<AbstractElement>>();

  private final Map<ARTElement, ARTElement> pathElementToReachedElement = new HashMap<ARTElement, ARTElement>();

  private Block currentBlock;
  private BlockPartitioning partitioning;
  private int depth = 0;

  private final LogManager logger;
  private final CPAAlgorithm algorithm;
  private final ARTCPA wrappedCPA;
  private final TransferRelation wrappedTransfer;
  private final ReachedSetFactory reachedSetFactory;
  private final Reducer wrappedReducer;

  private Map<Pair<AbstractElement, Precision>, Precision> forwardPrecisionToExpandedPrecision;

  //Stats
  int cacheMisses = 0;
  int partialCacheHits = 0;
  int fullCacheHits = 0;
  int maxRecursiveDepth = 0;
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
    wrappedCPA = (ARTCPA)abmCpa.getWrappedCpa();
    wrappedTransfer = wrappedCPA.getTransferRelation();
    wrappedReducer = abmCpa.getReducer();
    assert wrappedReducer != null;
  }


  void setForwardPrecisionToExpandedPrecision(
      Map<Pair<AbstractElement, Precision>, Precision> pForwardPrecisionToExpandedPrecision) {
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

        logger.log(Level.FINER, "Starting recursive analysis of depth", ++depth, "at edge", edge);
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

          forwardPrecisionToExpandedPrecision.put(Pair.of((AbstractElement)expandedElement, pPrecision), expandedPrecision);
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

  private ReachedSet getReachedSet(ARTElement element, Precision precision) {
    CFANode loc = element.retrieveLocationElement().getLocationNode();
    Block context = partitioning.getBlockForCallNode(loc);
    AbstractElement reducedElement = wrappedReducer.getVariableReducedElement(element, context, loc);
    Precision reducedPrecision = wrappedReducer.getVariableReducedPrecision(precision, context);
    ReachedSet result =  subgraphReachCache.get(reducedElement, reducedPrecision, context);
    assert result != null;
    return result;
  }

  void removeSubtree(ARTReachedSet reachSet, Path pPath, ARTElement element, Precision newPrecision) {
    removeSubtreeTimer.start();

    List<ARTElement> path = trimPath(pPath, element);
    assert path.get(path.size()-1).equals(element);

    Set<ARTElement> relevantCallNodes = getRelevantDefinitionNodes(path);

    Triple<UnmodifiableReachedSet, Pair<ARTElement, Precision>, CFANode> lastReachSet = null;
    //iterate from root to element and remove all subtrees for subgraph calls
    for(ARTElement pathElement : Iterables.skip(path, 1)) {
      ARTElement currentElement = pathElementToReachedElement.get(pathElement);
      assert currentElement != null;

      CFANode node = currentElement.retrieveLocationElement().getLocationNode();

      boolean relevantCall = false;
      if(relevantCallNodes.contains(pathElement)) {
       relevantCall = true;
      }

      if(pathElement.equals(element) || relevantCall) {
        Precision currentPrecision;
        if (lastReachSet == null) {
          // first iteration, update main reached set
          currentPrecision = reachSet.asReachedSet().getPrecision(currentElement);
          removeSubtree(reachSet, currentElement, pathElement.equals(element)?newPrecision:null);
        } else {
          currentPrecision = lastReachSet.getFirst().getPrecision(currentElement);
          removeCachedSubtree(lastReachSet, currentElement, newPrecision, pathElement.equals(element));
        }

        if(relevantCall) {
          lastReachSet = new Triple<UnmodifiableReachedSet, Pair<ARTElement, Precision>, CFANode>(getReachedSet(currentElement, currentPrecision), Pair.of(currentElement, currentPrecision), node);
        }
      }
    }

    removeSubtreeTimer.stop();
  }

  private void removeCachedSubtree(Triple<UnmodifiableReachedSet, Pair<ARTElement, Precision>, CFANode> lastTriple, ARTElement currentElement, Precision newPrecision, boolean isLastElement) {
    //called in a subgraphs ART; in this case we need to tell the transfer function to update its cached
    //specifications such that the subtree is removed in the subgraphs ART
    ARTElement lastCallNode = lastTriple.getSecond().getFirst();
    Precision lastDefinitionNodePrecision = lastTriple.getSecond().getSecond();
    CFANode lastDefinitionCFANode = lastTriple.getThird();

    removeCachedSubtree(lastCallNode, lastDefinitionNodePrecision, lastDefinitionCFANode, currentElement, newPrecision, isLastElement);
  }

  private void removeCachedSubtree(ARTElement rootElement, Precision rootPrecision, CFANode rootNode, ARTElement removeElement, Precision newPrecision, boolean isLastElement) {
    logger.log(Level.FINER, "Remove cached subtree for ", removeElement, " (rootNode: ", rootNode, ") issued");

    removeCachedSubtreeTimer.start();

    try {
      Block rootSubtree = partitioning.getBlockForCallNode(rootNode);

      AbstractElement reducedRootElement = wrappedReducer.getVariableReducedElement(rootElement, rootSubtree, rootNode);
      Precision reducedRootPrecision = wrappedReducer.getVariableReducedPrecision(rootPrecision, rootSubtree);

      ReachedSet reachedSet = subgraphReachCache.get(reducedRootElement, reducedRootPrecision, rootSubtree);

      Precision newReducedRemovePrecision = wrappedReducer.getVariableReducedPrecision(Precisions.replaceByType(reachedSet.getPrecision(removeElement), newPrecision, newPrecision.getClass()), rootSubtree);

      assert !removeElement.getParents().isEmpty();

      logger.log(Level.FINEST, "Removing subtree, adding a new cached entry, and removing the former cached entries");
      removeSubtree(reachedSet, removeElement, isLastElement?newReducedRemovePrecision:null);

      subgraphReturnCache.remove(reducedRootElement, reducedRootPrecision, rootSubtree);
    }
    finally {
      removeCachedSubtreeTimer.stop();
    }
  }

  private void removeSubtree(ReachedSet reachedSet, ARTElement artElement) {
    ARTReachedSet artReachSet = new ARTReachedSet(reachedSet, wrappedCPA);
    removeSubtree(artReachSet, artElement);
  }

  private void removeSubtree(ReachedSet reachedSet, ARTElement artElement, Precision newPrecision) {
    ARTReachedSet artReachSet = new ARTReachedSet(reachedSet, wrappedCPA);
    removeSubtree(artReachSet, artElement, newPrecision);
  }

  private void removeSubtree(ARTReachedSet reachedSet, ARTElement artElement) {
    reachedSet.removeSubtree(artElement);
  }

  private void removeSubtree(ARTReachedSet reachedSet, ARTElement artElement, Precision newPrecision) {
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
  ARTElement computeCounterexampleSubgraph(ARTElement target, ReachedSet reachedSet, ARTElement newTreeTarget) throws InterruptedException, RecursiveAnalysisFailedException {
    //start by creating ARTElements for each node needed in the tree
    Map<ARTElement, ARTElement> elementsMap = new HashMap<ARTElement, ARTElement>();
    Stack<ARTElement> openElements = new Stack<ARTElement>();
    ARTElement root = null;

    pathElementToReachedElement.put(newTreeTarget, target);
    elementsMap.put(target, newTreeTarget);
    openElements.push(target);
    while(!openElements.empty()) {
      ARTElement currentElement = openElements.pop();
      for(ARTElement parent : currentElement.getParents()) {
        if(!elementsMap.containsKey(parent)) {
          //create node for parent in the new subtree
          elementsMap.put(parent, new ARTElement(parent.getWrappedElement(), null));
          pathElementToReachedElement.put(elementsMap.get(parent), parent);
          //and remember to explore the parent later
          openElements.push(parent);
        }
        CFAEdge edge = getEdgeToChild(parent, currentElement);
        if(edge == null) {
          //this is a summarized call and thus an direct edge could not be found
          //we have the transfer function to handle this case, as our reachSet is wrong
          //(we have to use the cached ones)
          ARTElement innerTree = computeCounterexampleSubgraph(parent, reachedSet.getPrecision(parent), elementsMap.get(currentElement));
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
  private ARTElement computeCounterexampleSubgraph(ARTElement root, Precision rootPrecision, ARTElement newTreeTarget) throws InterruptedException, RecursiveAnalysisFailedException {
    CFANode rootNode = root.retrieveLocationElement().getLocationNode();
    Block rootSubtree = partitioning.getBlockForCallNode(rootNode);

    AbstractElement reducedRootElement = wrappedReducer.getVariableReducedElement(root, rootSubtree, rootNode);
    Precision reducedRootPrecision = wrappedReducer.getVariableReducedPrecision(rootPrecision, rootSubtree);
    ReachedSet reachSet = subgraphReachCache.get(reducedRootElement, reducedRootPrecision, rootSubtree);
    //we found the to the root and precision corresponding reach set
    //now try to find the target in the reach set
    ARTElement targetARTElement = (ARTElement)wrappedReducer.getVariableReducedElement(pathElementToReachedElement.get(newTreeTarget), rootSubtree, rootNode);
    if(targetARTElement.isDestroyed()) {
      logger.log(Level.FINE, "Target element refers to a destroyed ARTElement, i.e., the cached subtree is outdated. Updating it.");
      return null;
    }
    //we found the target; now construct a subtree in the ART starting with targetARTElement
    ARTElement result = computeCounterexampleSubgraph(targetARTElement, reachSet, newTreeTarget);
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

  private static CFAEdge getEdgeToChild(ARTElement parent, ARTElement child) {
    CFANode currentLoc = parent.retrieveLocationElement().getLocationNode();
    CFANode childNode = child.retrieveLocationElement().getLocationNode();

    return getEdgeTo(currentLoc, childNode);
  }

  private static CFAEdge getEdgeTo(CFANode node1, CFANode node2) {
    for(int i = 0; i < node1.getNumLeavingEdges(); i++) {
      CFAEdge edge = node1.getLeavingEdge(i);
      if(edge.getSuccessor() == node2) {
        return edge;
      }
    }
    return null;
  }

  @Override
  public Collection<? extends AbstractElement> strengthen(
      AbstractElement pElement, List<AbstractElement> pOtherElements,
      CFAEdge pCfaEdge, Precision pPrecision) throws CPATransferException,
      InterruptedException {
    return wrappedTransfer.strengthen(pElement, pOtherElements, pCfaEdge, pPrecision);
  }

}
