package org.sosy_lab.cpachecker.cpa.abm;

import static com.google.common.base.Preconditions.checkNotNull;
import static org.sosy_lab.cpachecker.util.AbstractElements.*;

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
import org.sosy_lab.cpachecker.cpa.predicate.PredicateAbstractElement;
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
    
    private boolean containsKey(AbstractElement predicateKey, Precision precisionKey, Block context) {
      return cache.containsKey(getHashCode(predicateKey, precisionKey, context));
    }
    
    private V get(AbstractElement predicateKey, Precision precisionKey, Block context) {
      return cache.get(getHashCode(predicateKey, precisionKey, context));
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
  private final ARTCPA wrappedCPA;
  private final TransferRelation wrappedTransfer;
  private final ReachedSetFactory reachedSetFactory;
  private final Reducer wrappedReducer;
  
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

    if (edge == null) {
      List<AbstractElement> result = new ArrayList<AbstractElement>();
      CFANode node = extractLocation(pElement);
      for (int i = 0; i < node.getNumLeavingEdges(); i++) {
        CFAEdge e = node.getLeavingEdge(i);
        result.addAll(getAbstractSuccessors(pElement, pPrecision, e));
      }
      return result;
    }
    
    CFANode currentNode = edge.getPredecessor();

    if (partitioning.isCallNode(currentNode)) {
      //we have to start a recursive analysis
      if(partitioning.getBlockForCallNode(currentNode).equals(currentBlock)) {
        //we are already in same context
        //thus we already did the recursive call or we a recursion in the cachedSubtrees
        //the latter isnt supported yet, but in the the former case we can classicaly do the post operation
        return wrappedTransfer.getAbstractSuccessors(pElement, pPrecision, edge);
      }
      
      if (isHeadOfMainFunction(currentNode)) {
        //skip main function
        return wrappedTransfer.getAbstractSuccessors(pElement, pPrecision, edge);
      }
      
      if(AbstractElements.extractElementByType(pElement, PredicateAbstractElement.class) != null && AbstractElements.extractElementByType(pElement, PredicateAbstractElement.class).getAbstractionFormula().isFalse()) {
        //TODO: avoid reference to PredicateAbstractElement
        //avoid recursive analysis if abstraction is false anyway
        return Collections.emptySet();
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
      currentBlock = partitioning.getBlockForCallNode(currentNode);
      Collection<AbstractElement> reducedResult = performCompositeAnalysis(pElement, pPrecision, currentNode, edge);
      
      logger.log(Level.FINER, "Recursive analysis of depth", depth--, "finished");
      logger.log(Level.ALL, "Resulting elements:", reducedResult);

      List<AbstractElement> expandedResult = new ArrayList<AbstractElement>(reducedResult.size());
      for (AbstractElement reducedElement : reducedResult) {
        ARTElement expandedElement = (ARTElement)wrappedReducer.getVariableExpandedElement(pElement, currentBlock, reducedElement);
        expandedElement.addParent((ARTElement)pElement);
        expandedResult.add(expandedElement);
      }
      
      logger.log(Level.ALL, "Expanded results:", expandedResult);
      
      currentBlock = outerSubtree;

      return expandedResult;      
    
    } else if (currentBlock != null && currentBlock.isReturnNode(currentNode)) {
      // do not perform analysis beyond return states
      
      if (currentBlock.getNodes().contains(edge.getSuccessor())) {
        // but this is an edge that stays in this block, so perform analysis anyway
        return wrappedTransfer.getAbstractSuccessors(pElement, pPrecision, edge);
        
      } else {
        return Collections.emptySet();
      }
      
    } else {
      return wrappedTransfer.getAbstractSuccessors(pElement, pPrecision, edge);
    }
  }


  private boolean isHeadOfMainFunction(CFANode currentNode) {
    return currentNode instanceof CFAFunctionDefinitionNode && currentNode.getNumEnteringEdges() == 0;
  }


  private Collection<AbstractElement> performCompositeAnalysis(AbstractElement initialElement, Precision initialPrecision, CFANode node, CFAEdge edge) throws InterruptedException, RecursiveAnalysisFailedException {
    try {
      AbstractElement reducedInitialElement = wrappedReducer.getVariableReducedElement(initialElement, currentBlock, node);
      Precision reducedInitialPrecision = wrappedReducer.getVariableReducedPrecision(initialPrecision, currentBlock);
      
      ReachedSet reached = null;
      if (!NO_CACHING) {
        Collection<AbstractElement> result = subgraphReturnCache.get(reducedInitialElement, reducedInitialPrecision, currentBlock);
        if (result != null) {
          fullCacheHits++;
          return result;
        }

        reached = subgraphReachCache.get(reducedInitialElement, reducedInitialPrecision, currentBlock);
      }
      
      if (reached != null) {
        //at least we have partly computed reach set cached
        partialCacheHits++;
      } else {
        //compute the subgraph specification from scratch
        cacheMisses++;
        reached = createInitialReachedSet(reducedInitialElement, reducedInitialPrecision, node, edge);
        subgraphReachCache.put(reducedInitialElement, reducedInitialPrecision, currentBlock, reached);      
      }  
      
      algorithm.run(reached);     
            
      List<AbstractElement> result;
      
      // if the element is an error element
      AbstractElement lastElement = reached.getLastElement();
      if (isTargetElement(lastElement)) {
        //found a target element inside a recursive subgraph call
        //this needs to be propagated to outer subgraph (till main is reached)
        result = Collections.singletonList(lastElement);
        
      } else {
        
        result = new ArrayList<AbstractElement>();
        for(CFANode returnNode : currentBlock.getReturnNodes()) {
          Iterables.addAll(result, AbstractElements.filterLocation(reached, returnNode));
        }
      }
      
      subgraphReturnCache.put(reducedInitialElement, reducedInitialPrecision, currentBlock, result);
      return result;        
    } catch (CPAException e) {
      throw new RecursiveAnalysisFailedException(e);
    }    
  }
  
  private ReachedSet createInitialReachedSet(AbstractElement reducedInitialElement, Precision initialPredicatePrecision, CFANode node, CFAEdge edge) {
    ReachedSet reached = reachedSetFactory.create();
    reached.add(reducedInitialElement, initialPredicatePrecision);
    return reached;
  }

  private Precision getPrecision(UnmodifiableReachedSet reached, ARTElement target) {   
    ARTElement ele = ARTElementSearcher.searchForARTElement(reached, target, wrappedReducer, partitioning);
    return reached.getPrecision(ele);
  }

  private ReachedSet getReachedSet(ARTElement element, Precision precision) {
    CFANode loc = element.retrieveLocationElement().getLocationNode();
    Block context = partitioning.getBlockForCallNode(loc);
    AbstractElement reducedElement = wrappedReducer.getVariableReducedElement(element, context, loc); 
    Precision reducedPrecision = wrappedReducer.getVariableReducedPrecision(precision, context);
    return subgraphReachCache.get(reducedElement, reducedPrecision, context);
  }
   
  void removeSubtree(ARTReachedSet reachSet, Path pPath, ARTElement element, Precision newPrecision) {      
    removeSubtreeTimer.start();
    
    List<ARTElement> path = trimPath(pPath, element);
    assert path.get(path.size()-1).equals(element);
   
    Set<ARTElement> relevantCallNodes = getRelevantDefinitionNodes(path);
    
    Triple<UnmodifiableReachedSet, Pair<ARTElement, Precision>, CFANode> lastReachSet = null;    
    //iterate from root to element and remove all subtrees for subgraph calls
    for(ARTElement currentElement : Iterables.skip(path, 1)) {
      CFANode node = currentElement.retrieveLocationElement().getLocationNode();
      
      boolean relevantCall = false;
      if(relevantCallNodes.contains(currentElement)) {
       relevantCall = true;
      }
      
      if(currentElement.equals(element) || relevantCall) {
        Precision currentPrecision;
        if (lastReachSet == null) {
          // first iteration, update main reached set
          currentPrecision = getPrecision(reachSet.asReachedSet(), currentElement);
          removeCachedSubtree(reachSet, currentElement, newPrecision);
        } else {
          currentPrecision = getPrecision(lastReachSet.getFirst(), currentElement);
          removeCachedSubtree(lastReachSet, currentElement, newPrecision);         
        }
      
        if(relevantCall) {
          lastReachSet = new Triple<UnmodifiableReachedSet, Pair<ARTElement, Precision>, CFANode>(getReachedSet(currentElement, currentPrecision), Pair.of(currentElement, currentPrecision), node);
          if(relevantCall && currentElement.equals(element)) {
            //lastelement is a relevant call, redo the cached subtree removal once again
            removeCachedSubtree(lastReachSet, currentElement, newPrecision);
          }
        }
      }
    }    
    
    removeSubtreeTimer.stop();  
  }
  
  private void removeCachedSubtree(ARTReachedSet mainReachSet, ARTElement currentElement, Precision newPrecision) {
    //we need to remove the subtree in the cached ART, in which the currentElement is found
    
    //called in main function; in this case we simply remove the subtree in the global ART   
    ARTElement reachSetARTElement = ARTElementSearcher.searchForARTElement(mainReachSet.asReachedSet(), currentElement, wrappedReducer, partitioning);
    
    mainReachSet.removeSubtree(reachSetARTElement, newPrecision);          
  }
  
  private void removeCachedSubtree(Triple<UnmodifiableReachedSet, Pair<ARTElement, Precision>, CFANode> lastReachSet, ARTElement currentElement, Precision newPrecision) {
    assert lastReachSet != null; 

    //called in a subgraphs ART; in this case we need to tell the transfer function to update its cached
    //specifications such that the subtree is removed in the subgraphs ART
    ARTElement lastCallNode = lastReachSet.getSecond().getFirst();
    Precision lastDefinitionNodePrecision = lastReachSet.getSecond().getSecond();
    CFANode lastDefinitionCFANode = lastReachSet.getThird();
    
    removeCachedSubtree(lastCallNode, lastDefinitionNodePrecision, lastDefinitionCFANode, currentElement, newPrecision);
  }
  
  private void removeCachedSubtree(AbstractElement rootElement, Precision rootPrecision, CFANode rootNode, ARTElement removeElement, Precision newPrecision) {
    logger.log(Level.FINER, "Remove cached subtree for ", removeElement, " (rootNode: ", rootNode, ") issued");
    
    removeCachedSubtreeTimer.start();
    
    try {
      Block rootSubtree = partitioning.getBlockForCallNode(rootNode);
      
      AbstractElement reducedRootElement = wrappedReducer.getVariableReducedElement(rootElement, rootSubtree, rootNode);
      Precision reducedRootPrecision = wrappedReducer.getVariableReducedPrecision(rootPrecision, rootSubtree);
         
      ReachedSet reachedSet = subgraphReachCache.get(reducedRootElement, reducedRootPrecision, rootSubtree);   
      ARTElement reducedRemoveElement = ARTElementSearcher.searchForARTElement(reachedSet, removeElement, wrappedReducer, partitioning);
      
      Precision newRootPrecision = Precisions.replaceByType(rootPrecision, newPrecision);
      Precision newReducedRootPrecision = wrappedReducer.getVariableReducedPrecision(newRootPrecision, rootSubtree);
      Precision newReducedRemovePrecision = wrappedReducer.getVariableReducedPrecision(Precisions.replaceByType(reachedSet.getPrecision(reducedRemoveElement), newPrecision), rootSubtree);
      
      if(reducedRemoveElement.getParents().isEmpty()) {
        //this is actually the root of the subgraph; 
        if(reducedRootPrecision.equals(newReducedRootPrecision)) {
          //if the newPrecision is the same as before we need to enforce a recomputation of the whole ART
          logger.log(Level.FINEST, "Removing root of cached tree (i.e., remove the whole tree)");
          subgraphReachCache.remove(reducedRootElement, reducedRootPrecision, rootSubtree);
          subgraphReturnCache.remove(reducedRootElement, reducedRootPrecision, rootSubtree);
        }
        //(otherwise, there is no need to do anything)
        return;
      }       
      
      if(reducedRootPrecision.equals(newReducedRootPrecision)) {
        //newPrecision is same as oldPrecision; in this case just remove the subtree and force a recomputation of the ART
        //by removing it from the cache
        logger.log(Level.FINEST, "New precision equals old precision: Removing the subtree and the cache entry for the cached tree to force a recomputation");
        subgraphReturnCache.remove(reducedRootElement, reducedRootPrecision, rootSubtree);
        removeSubtree(reachedSet, reducedRemoveElement, newReducedRemovePrecision);    
        return;
      }
      if(subgraphReachCache.containsKey(reducedRootElement, newReducedRootPrecision, rootSubtree)) {
        logger.log(Level.FINEST, "Cached result for the new precision is already present. Noting to do.");
        //we already have a cached value for the new precision
        //no recomputation necessary; do nothing
        return;
      }
      logger.log(Level.FINEST, "Normal case: Removing subtree, adding a new cached entry, and removing the former cached entries");
      //no special case: remove subtree, and add a new cache entry, remove old cache entries
      removeSubtree(reachedSet, reducedRemoveElement, newReducedRemovePrecision);
      
      subgraphReachCache.remove(reducedRootElement, reducedRootPrecision, rootSubtree);
      subgraphReturnCache.remove(reducedRootElement, reducedRootPrecision, rootSubtree);
      
      subgraphReachCache.put(reducedRootElement, newReducedRootPrecision, rootSubtree, reachedSet);      
    }
    finally {
      removeCachedSubtreeTimer.stop();
    }
  }
  
  private void removeSubtree(ReachedSet reachedSet, ARTElement artElement, Precision artPrecision) {
    if(!reachedSet.contains(artElement)) {     
      throw new IllegalArgumentException("Given ReachedSet does not contain given ARTElement.");
    }
    ARTReachedSet artReachSet = new ARTReachedSet(reachedSet, wrappedCPA);
    artReachSet.removeSubtree(artElement, artPrecision);
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
    ARTElement lastElement = path.get(path.size()-1);
    Deque<ARTElement> openCallElements = new ArrayDeque<ARTElement>();
    Deque<Block> openSubtrees = new ArrayDeque<Block>();
    
    for (ARTElement currentElement : Iterables.skip(path, 1)) {
      CFANode node = currentElement.retrieveLocationElement().getLocationNode();
      if (partitioning.isCallNode(node)) {
        if (!(isHeadOfMainFunction(node))) {
          openCallElements.push(currentElement);    
          openSubtrees.push(partitioning.getBlockForCallNode(node));
        }
      
      } else {
        while (!openSubtrees.isEmpty()
             && openSubtrees.peek().isReturnNode(node)
             && !currentElement.equals(lastElement)) { 
          openCallElements.pop();
          openSubtrees.pop();      
        }
      }
    }
    return new HashSet<ARTElement>(openCallElements);
  }
  
  //returns root of a subtree leading from the root element of the given reachedSet to the target element
  //subtree is represented using children and parents of ARTElements, where newTreeTarget is the ARTElement
  //in the constructed subtree that represents target
  ARTElement computeCounterexampleSubgraph(ARTElement target, UnmodifiableReachedSet reachedSet, ARTElement newTreeTarget) throws InterruptedException, RecursiveAnalysisFailedException {
    //start by creating ARTElements for each node needed in the tree 
    Map<ARTElement, ARTElement> elementsMap = new HashMap<ARTElement, ARTElement>();
    Stack<ARTElement> openElements = new Stack<ARTElement>();
    ARTElement root = null;
    
    elementsMap.put(target, newTreeTarget);
    openElements.push(target);    
    while(!openElements.empty()) {
      ARTElement currentElement = openElements.pop();
      for(ARTElement parent : currentElement.getParents()) {
        if(!elementsMap.containsKey(parent)) {
          //create node for parent in the new subtree
          elementsMap.put(parent, new ARTElement(parent.getWrappedElement(), null));
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
    //TODO: assert that the subgraph is acyclic
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
    if (reachSet == null || !subgraphReturnCache.containsKey(reducedRootElement, reducedRootPrecision, rootSubtree)) {
      //recompute the ART
      logger.log(Level.WARNING, "Reached set of block got (partially) removed, recompute it");
      recomputeART(root, rootPrecision, rootNode, rootSubtree);
      reachSet = subgraphReachCache.get(reducedRootElement, reducedRootPrecision, rootSubtree);
      if(reachSet == null) {
        throw new IllegalArgumentException("Failed recomputing the reach set of " + root + " -> " + newTreeTarget  + " with precision " +  rootPrecision); 
      }
    }    
    //we found the to the root and precision corresponding reach set
    //now try to find the target in the reach set    
    ARTElement targetARTElement = ARTElementSearcher.searchForARTElement(reachSet, newTreeTarget, wrappedReducer, partitioning);    
    if (targetARTElement == null) {
      logger.log(Level.WARNING, "Target element is not longer contained in reached set");
      //if the target cannot be found, this means that the recomputation of the reach set already ruled out the (spurious) counterexample
      //-> nothing to do then
      //logger.log(Level.FINEST, "Recomputation ruled out spurious counterexample; no need to refine");
      return null; 
    }
    //we found the target; now construct a subtree in the ART starting with targetARTElement
    return computeCounterexampleSubgraph(targetARTElement, reachSet, newTreeTarget);
  }
  
  private void recomputeART(AbstractElement pRoot, Precision pRootPrecision, CFANode pRootNode, Block rootSubtree) throws InterruptedException, RecursiveAnalysisFailedException {
    //logger.log(Level.FINER, "Recomputing: " + pRoot + " at " + pRootNode);
    
    recomputeARTTimer.start();
    
    Block oldSubtree = currentBlock;
    currentBlock = rootSubtree;
    for(int i = 0; i < pRootNode.getNumLeavingEdges(); i++) {
      performCompositeAnalysis(pRoot, pRootPrecision, pRootNode, pRootNode.getLeavingEdge(i));
    }
    currentBlock = oldSubtree;
    recomputeARTTimer.stop();
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
