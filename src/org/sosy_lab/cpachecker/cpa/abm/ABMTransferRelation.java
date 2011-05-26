package org.sosy_lab.cpachecker.cpa.abm;

import static org.sosy_lab.cpachecker.util.AbstractElements.extractLocation;

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
import org.sosy_lab.cpachecker.cfa.objectmodel.CFAEdge;
import org.sosy_lab.cpachecker.cfa.objectmodel.CFANode;
import org.sosy_lab.cpachecker.cfa.objectmodel.c.FunctionDefinitionNode;
import org.sosy_lab.cpachecker.core.algorithm.CPAAlgorithm;
import org.sosy_lab.cpachecker.core.interfaces.AbstractElement;
import org.sosy_lab.cpachecker.core.interfaces.AbstractElementHash;
import org.sosy_lab.cpachecker.core.interfaces.Precision;
import org.sosy_lab.cpachecker.core.interfaces.Reducer;
import org.sosy_lab.cpachecker.core.interfaces.Targetable;
import org.sosy_lab.cpachecker.core.interfaces.TransferRelation;
import org.sosy_lab.cpachecker.core.reachedset.PartitionedReachedSet;
import org.sosy_lab.cpachecker.core.reachedset.ReachedSet;
import org.sosy_lab.cpachecker.core.reachedset.UnmodifiableReachedSet;
import org.sosy_lab.cpachecker.core.waitlist.TopologicallySortedWaitlist;
import org.sosy_lab.cpachecker.core.waitlist.Waitlist;
import org.sosy_lab.cpachecker.core.waitlist.Waitlist.WaitlistFactory;
import org.sosy_lab.cpachecker.cpa.art.ARTCPA;
import org.sosy_lab.cpachecker.cpa.art.ARTElement;
import org.sosy_lab.cpachecker.cpa.art.ARTReachedSet;
import org.sosy_lab.cpachecker.cpa.art.Path;
import org.sosy_lab.cpachecker.cpa.predicate.ABMPredicateRefiner;
import org.sosy_lab.cpachecker.exceptions.CPAException;
import org.sosy_lab.cpachecker.exceptions.CPATransferException;
import org.sosy_lab.cpachecker.util.AbstractElements;
import org.sosy_lab.cpachecker.util.Precisions;

import com.google.common.collect.Iterables;

import de.upb.agw.cpachecker.cpa.abm.util.CachedSubtree;
import de.upb.agw.cpachecker.cpa.abm.util.CachedSubtreeManager;

@Options(prefix="cpa.abm")
public class ABMTransferRelation implements TransferRelation {

  @Option
  public static boolean NO_CACHING = false; 
   
  private class Cache<V> {
    
    private final Map<Pair<AbstractElementHash, CachedSubtree>, V> cache = new HashMap<Pair<AbstractElementHash, CachedSubtree>, V>();

    private Pair<AbstractElementHash, CachedSubtree> getHashCode(AbstractElement predicateKey,
        Precision precisionKey, CachedSubtree context) {
      return Pair.of(wrappedReducer.getHashCodeForElement(predicateKey, precisionKey, context, csmgr), context);
    }
    
    private V put(AbstractElement predicateKey, Precision precisionKey, CachedSubtree context, V item) {
      return cache.put(getHashCode(predicateKey, precisionKey, context), item);
    }
    
    private void remove(AbstractElement predicateKey, Precision precisionKey, CachedSubtree context) {
      cache.remove(getHashCode(predicateKey, precisionKey, context));
    }
    
    private boolean containsKey(AbstractElement predicateKey, Precision precisionKey, CachedSubtree context) {
      return cache.containsKey(getHashCode(predicateKey, precisionKey, context));
    }
    
    private V get(AbstractElement predicateKey, Precision precisionKey, CachedSubtree context) {
      return cache.get(getHashCode(predicateKey, precisionKey, context));
    }

    private void clear() {
      cache.clear();
    }
  }
  
  
  private final Cache<ReachedSet> subgraphReachCache = new Cache<ReachedSet>();
  private final Cache<Collection<AbstractElement>> subgraphReturnCache = new Cache<Collection<AbstractElement>>();
  
  private CachedSubtree currentCachedSubtree;
  private CachedSubtreeManager csmgr;
  private int depth = 0;
  
  private final LogManager logger;
  private final CPAAlgorithm algorithm;
  private final ARTCPA wrappedCPA;
  private final TransferRelation wrappedTransfer;

  private final Reducer wrappedReducer;
  
  //Stats
  int cacheMisses = 0;
  int partialCacheHits = 0;
  int fullCacheHits = 0;
  Timer hashingTimer = new Timer();
  Timer equalsTimer = new Timer();
  Timer recomputeARTTimer = new Timer();
  Timer returnElementsSearchTimer = new Timer();
  Timer removeCachedSubtreeTimer = new Timer();
  Timer removeSubtreeTimer = new Timer();

  public ABMTransferRelation(Configuration pConfig, LogManager pLogger, ABMCPA abmCpa) throws InvalidConfigurationException {
    pConfig.inject(this);
    logger = pLogger;
    algorithm = new CPAAlgorithm(abmCpa, logger);
    wrappedCPA = (ARTCPA)abmCpa.getWrappedCpa();
    wrappedTransfer = wrappedCPA.getTransferRelation();
    wrappedReducer = wrappedCPA.getReducer();
    assert wrappedReducer != null;
  }
  
  
  void setCachedSubtreeManager(CachedSubtreeManager pManager) {
    csmgr = pManager;
    currentCachedSubtree = csmgr.getMainSubtree();
  }
  
  public CachedSubtree getCurrentCachedSubtree() {
    return currentCachedSubtree;
  }
  
  public CachedSubtreeManager getCachedSubtreeManager() {
    return csmgr;
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

    if (csmgr.isCallNode(currentNode)) {
      //we have to start a recursive analysis
      if(csmgr.getCachedSubtreeForCallNode(currentNode).equals(currentCachedSubtree)) {
        //we are already in same context
        //thus we already did the recursive call or we a recursion in the cachedSubtrees
        //the latter isnt supported yet, but in the the former case we can classicaly do the post operation
        return wrappedTransfer.getAbstractSuccessors(pElement, pPrecision, edge);
      }
      
      if(currentNode instanceof FunctionDefinitionNode && currentNode.getFunctionName().equalsIgnoreCase("main")) {
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

      CachedSubtree outerSubtree = currentCachedSubtree;
      currentCachedSubtree = csmgr.getCachedSubtreeForCallNode(currentNode);
      Collection<AbstractElement> reducedResult = performCompositeAnalysis(pElement, pPrecision, currentNode, edge);
      
      logger.log(Level.FINER, "Recursive analysis of depth", depth--, "finished");
      logger.log(Level.ALL, "Resulting elements:", reducedResult);

      List<AbstractElement> expandedResult = new ArrayList<AbstractElement>(reducedResult.size());
      for (AbstractElement reducedElement : reducedResult) {
        ARTElement expandedElement = (ARTElement)wrappedReducer.getVariableExpandedElement(pElement, currentCachedSubtree, reducedElement);
        expandedElement.addParent((ARTElement)pElement);
        expandedResult.add(expandedElement);
      }
      
      logger.log(Level.ALL, "Expanded results:", expandedResult);
      
      currentCachedSubtree = outerSubtree;

      return expandedResult;      
    }
    else if(currentCachedSubtree != null && currentCachedSubtree.isReturnNode(currentNode)) {
      //do not perform analysis beyond return states //TODO: requires that the return state has only outgoing edges to the "outside"; checked somewhere?
      return Collections.emptySet();
    }
    else {
      return wrappedTransfer.getAbstractSuccessors(pElement, pPrecision, edge);
    }
  }


  private Collection<AbstractElement> performCompositeAnalysis(AbstractElement initialElement, Precision initialPrecision, CFANode node, CFAEdge edge) throws InterruptedException {
    try {
      //logger.log(Level.FINER, "Performing recursive compositite analysis for node " + node + " in state " + initialPredicateElement);
      AbstractElement reducedInitialElement = wrappedReducer.getVariableReducedElement(initialElement, currentCachedSubtree, node);
      
      ReachedSet reached;
      if(!NO_CACHING && subgraphReturnCache.containsKey(reducedInitialElement, initialPrecision, currentCachedSubtree)) {        
        fullCacheHits++;
        //logger.log(Level.FINEST, "Already have cached result for the element (Full Cache Hit)");
        return subgraphReturnCache.get(reducedInitialElement, initialPrecision, currentCachedSubtree);        
      } 
      else if(!NO_CACHING && subgraphReachCache.containsKey(reducedInitialElement, initialPrecision, currentCachedSubtree)) {
        //at least we have partly computed reach set cached
        partialCacheHits++;
        //logger.log(Level.FINEST, "Already have partial cached results for the element (Partial Cache Hit)");
        reached = subgraphReachCache.get(reducedInitialElement, initialPrecision, currentCachedSubtree);
      } else {
        //compute the subgraph specification from scratch
        cacheMisses++;
        //logger.log(Level.FINEST, "No cached results for the element yet (Cache Miss)");
        reached = computeInitialReachedSet(reducedInitialElement, initialPrecision, node, edge);
      }       
      
      algorithm.run(reached);     
      
      subgraphReachCache.put(reducedInitialElement, initialPrecision, currentCachedSubtree, reached);      
            
      AbstractElement lastElement = reached.getLastElement();
      
      // if the element is an error element
      if((lastElement instanceof Targetable) && ((Targetable)lastElement).isTarget()) {
        //found a target element inside a recursive subgraph call
        //this needs to be propagated to outer subgraph (till main is reached)
        List<AbstractElement> result = Collections.singletonList(lastElement);
        subgraphReturnCache.put(reducedInitialElement, initialPrecision, currentCachedSubtree, result);
        return result;        
      }
      
      returnElementsSearchTimer.start();
      ArrayList<AbstractElement> returningNodes = new ArrayList<AbstractElement>();
      for(CFANode returnNode : currentCachedSubtree.getReturnNodes()) {
        Iterables.addAll(returningNodes, AbstractElements.filterLocation(reached, returnNode));
      }
      returnElementsSearchTimer.stop();
      subgraphReturnCache.put(reducedInitialElement, initialPrecision, currentCachedSubtree, returningNodes);
      
      return returningNodes;
    } catch (CPAException e) {
      throw new RuntimeException(e);
    }    
  }
  
  private ReachedSet computeInitialReachedSet(AbstractElement reducedInitialElement, Precision initialPredicatePrecision, CFANode node, CFAEdge edge) throws CPATransferException {
    //TODO: respect configuration
    WaitlistFactory waitlistFactory = Waitlist.TraversalMethod.BFS;
    waitlistFactory = TopologicallySortedWaitlist.factory(waitlistFactory);
    //waitlistFactory = CallstackSortedWaitlist.factory(waitlistFactory);
    
    ReachedSet reached = new PartitionedReachedSet(waitlistFactory);
    reached.add(reducedInitialElement, initialPredicatePrecision);
    return reached;
  }

  private Precision getPrecision(UnmodifiableReachedSet reached, ARTElement target) {   
    ARTElement ele = ARTElementSearcher.searchForARTElement(reached, target, wrappedReducer, csmgr);
    return reached.getPrecision(ele);
  }

  private ReachedSet getReachedSet(ARTElement element, Precision precision) {
    CFANode loc = element.retrieveLocationElement().getLocationNode();
    CachedSubtree context = csmgr.getCachedSubtreeForCallNode(loc);
    AbstractElement reducedElement = wrappedReducer.getVariableReducedElement(element, context, loc);    
    return subgraphReachCache.get(reducedElement, precision, context);
  }
   
  public void removeSubtree(ARTReachedSet reachSet, Path pPath, ARTElement element, Precision newPrecision) {      
    removeSubtreeTimer.start();
    
    Path path = trimPath(pPath, element);
   
    Set<ARTElement> relevantCallNodes = getRelevantDefinitionNodes(path);
    
    Triple<UnmodifiableReachedSet, Pair<ARTElement, Precision>, CFANode> lastReachSet = null;    
    //iterate from root to element and remove all subtrees for subgraph calls
    for(ARTElement currentElement : Iterables.transform(Iterables.skip(path, 1), Pair.<ARTElement>getProjectionToFirst())) {
      CFANode node = currentElement.retrieveLocationElement().getLocationNode();
      
      boolean relevantCall = false;
      if(relevantCallNodes.contains(currentElement)) {
       relevantCall = true;
      }
      
      if(currentElement.equals(element) || relevantCall) {        
        Precision currentPrecision = removeCachedSubtree(lastReachSet, currentElement, reachSet, newPrecision);
      
        if(relevantCall) {
          lastReachSet = new Triple<UnmodifiableReachedSet, Pair<ARTElement, Precision>, CFANode>(getReachedSet(currentElement, currentPrecision), Pair.of(currentElement, currentPrecision), node);
          if(relevantCall && currentElement.equals(element)) {
            //lastelement is a relevant call, redo the cached subtree removal once again
            currentPrecision = removeCachedSubtree(lastReachSet, currentElement, reachSet, newPrecision);
          }
        }
      }
    }    
    
    removeSubtreeTimer.stop();  
  }
  
  private Precision removeCachedSubtree(Triple<UnmodifiableReachedSet, Pair<ARTElement, Precision>, CFANode> lastReachSet, ARTElement currentElement, ARTReachedSet mainReachSet, Precision newPrecision) {
    Precision currentPrecision = getPrecision(lastReachSet != null ? lastReachSet.getFirst() : mainReachSet.asReachedSet(), currentElement);
    assert currentPrecision != null;
    
    //we need to remove the subtree in the cached ART, in which the currentElement is found
    if(lastReachSet == null) {
      //called in main function; in this case we simply remove the subtree in the global ART   
      ARTElement reachSetARTElement = ARTElementSearcher.searchForARTElement(mainReachSet.asReachedSet(), currentElement, wrappedReducer, csmgr);
      
      mainReachSet.removeSubtree(reachSetARTElement, newPrecision);          
    }
    else {
      //called in a subgraphs ART; in this case we need to tell the transfer function to update its cached
      //specifications such that the subtree is removed in the subgraphs ART
      ARTElement lastCallNode = lastReachSet.getSecond().getFirst();
      Precision lastDefinitionNodePrecision = lastReachSet.getSecond().getSecond();
      CFANode lastDefinitionCFANode = lastReachSet.getThird();
      
      removeCachedSubtree(lastCallNode, lastDefinitionNodePrecision, lastDefinitionCFANode, currentElement, newPrecision);
    }
    
    return currentPrecision;
  }
  
  private void removeCachedSubtree(AbstractElement rootElement, Precision rootPrecision, CFANode rootNode, ARTElement removeElement, Precision newPrecision) {
    //logger.log(Level.FINER, "Remove cached subtree for " + removeElement + " (rootNode: " + rootNode + ") issued");
    
    removeCachedSubtreeTimer.start();
    
    try {
      CachedSubtree rootSubtree = csmgr.getCachedSubtreeForCallNode(rootNode);
      
      AbstractElement reducedRootElement = wrappedReducer.getVariableReducedElement(rootElement, rootSubtree, rootNode);
         
      ReachedSet reachedSet = subgraphReachCache.get(reducedRootElement, rootPrecision, rootSubtree);   
      ARTElement reducedRemoveElement = ARTElementSearcher.searchForARTElement(reachedSet, removeElement, wrappedReducer, csmgr);
      
      if(reducedRemoveElement.getParents().isEmpty()) {
        //this is actually the root of the subgraph; so remove the whole thing
        //logger.log(Level.FINEST, "Removing root of cached tree (i.e., remove the whole tree)");
        subgraphReachCache.remove(reducedRootElement, rootPrecision, rootSubtree);
        subgraphReturnCache.remove(reducedRootElement, rootPrecision, rootSubtree);
        return;
      }
      
      Precision newRootPrecision = Precisions.replaceByType(rootPrecision, newPrecision);
      
      if(rootPrecision.equals(newRootPrecision)) {
        //newPrecision is same as oldPrecision; in this case just remove the subtree and force a recomputation of the ART
        //by removing it from the cache
        //logger.log(Level.FINEST, "New precision equals old precision: Removing the subtree and the cache entry for the cached tree to force a recomputation");
        subgraphReturnCache.remove(reducedRootElement, rootPrecision, rootSubtree);
        removeSubtree(reachedSet, reducedRemoveElement, newPrecision);    
        return;
      }
      if(subgraphReachCache.containsKey(reducedRootElement, newRootPrecision, rootSubtree)) {
        //logger.log(Level.FINEST, "Cached result for the new precision is already present. Noting to do.");
        //we already have a cached value for the new precision
        //no recomputation necessary; do nothing
        return;
      }
      //logger.log(Level.FINEST, "Normal case: Removing subtree, adding a new cached entry, and removing the former cached entries");
      //no special case: remove subtree, and add a new cache entry, remove old cache entries
      removeSubtree(reachedSet, reducedRemoveElement, newPrecision);
      
      //System.out.println("REMOVE: " + reducedRootElement.getAbstractionFormula().asRegion() + " for subtree " + rootSubtree.getCallNode());
      subgraphReachCache.remove(reducedRootElement, rootPrecision, rootSubtree);
      subgraphReturnCache.remove(reducedRootElement, rootPrecision, rootSubtree);
      
      subgraphReachCache.put(reducedRootElement, newRootPrecision, rootSubtree, reachedSet);      
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
  
  private Path trimPath(Path pPath, ARTElement pElement) {
    Path result = new Path();
    for(Pair<ARTElement, CFAEdge> e : pPath) {
      result.add(e);
      if(e.getFirst().equals(pElement)) {
        return result;
      }
    }
    throw new IllegalArgumentException("Element " + pElement + " could not be found in path " + pPath + ".");
  }

  private Set<ARTElement> getRelevantDefinitionNodes(Path path) {  
    Deque<ARTElement> openCallElements = new ArrayDeque<ARTElement>();
    Deque<CachedSubtree> openSubtrees = new ArrayDeque<CachedSubtree>();
    for(ARTElement currentElement : Iterables.transform(Iterables.skip(path, 1), Pair.<ARTElement>getProjectionToFirst())) {
      CFANode node = currentElement.retrieveLocationElement().getLocationNode();
      if(csmgr.isCallNode(node)) {
        if(!(node instanceof FunctionDefinitionNode && node.getFunctionName().equalsIgnoreCase("main"))) {
          openCallElements.push(currentElement);    
          openSubtrees.push(csmgr.getCachedSubtreeForCallNode(node));
        }
      }
      else {
        while(openSubtrees.size() > 0 && openSubtrees.peek().isReturnNode(node) && !currentElement.equals(path.getLast().getFirst())) { 
          openCallElements.pop();
          openSubtrees.pop();      
        }
      }
    }
    return new HashSet<ARTElement>(openCallElements);
  }
  
  /**
   * This method looks for the reached set that belongs to (root, rootPrecision),
   * then looks for target in this reached set and constructs a tree from root to target
   * (recursively, if needed).
   */
  public ARTElement computeCounterexampleSubgraph(ARTElement root, Precision rootPrecision, ARTElement target, ABMPredicateRefiner caller) throws InterruptedException {
    CFANode rootNode = root.retrieveLocationElement().getLocationNode();
    CachedSubtree rootSubtree = csmgr.getCachedSubtreeForCallNode(rootNode);
            
    AbstractElement reducedRootElement = wrappedReducer.getVariableReducedElement(root, rootSubtree, rootNode);
    ReachedSet reachSet = subgraphReachCache.get(reducedRootElement, rootPrecision, rootSubtree);
    if (reachSet == null) {
      //recompute the ART
      logger.log(Level.WARNING, "Reached set of block got removed, recompute it");
      recomputeART(root, rootPrecision, rootNode, rootSubtree);
      reachSet = subgraphReachCache.get(reducedRootElement, rootPrecision, rootSubtree);
      if(reachSet == null) {
        throw new IllegalArgumentException("Failed recomputing the reach set of " + root + " -> " + target  + " with precision " +  rootPrecision); 
      }
    }    
    //we found the to the root and precision corresponding reach set
    //now try to find the target in the reach set    
    AbstractElement targetARTElement = ARTElementSearcher.searchForARTElement(reachSet, target, wrappedReducer, csmgr);    
    if (targetARTElement == null) {
      logger.log(Level.WARNING, "Target element is not longer contained in reached set");
      //if the target cannot be found, this means that the recomputation of the reach set already ruled out the (spurious) counterexample
      //-> nothing to do then
      //logger.log(Level.FINEST, "Recomputation ruled out spurious counterexample; no need to refine");
      return null; 
    }
    //we found the target; now construct a subtree in the ART starting with targetARTElement
    ARTElement reducedSubgraph = caller.computeCounterexampleSubgraph((ARTElement)targetARTElement, reachSet);
    if(reducedSubgraph == null) {
      return null;
    }
        
    //subtree is in reduced state space; we need to lift it to real state space and append the lifted tree to the given target ARTElement
    //start by lifting root
    ARTElement expandedRoot = (ARTElement)wrappedReducer.getVariableExpandedElement(root, rootSubtree, reducedSubgraph);
    
    //next traverse the tree
    Map<ARTElement, ARTElement> reducedToExpandedMap = new HashMap<ARTElement, ARTElement>();
    Stack<ARTElement> openElements = new Stack<ARTElement>();
        
    reducedToExpandedMap.put(reducedSubgraph, expandedRoot);
    openElements.push(reducedSubgraph);    
    while(!openElements.isEmpty()) {
      ARTElement currentElement = openElements.pop();
      
      for(ARTElement child : currentElement.getChildren()) {
        if(!reducedToExpandedMap.containsKey(child)) {
          if(child.getChildren().isEmpty() && !target.isTarget()) {
            //no children -> should be target
            //thus no need to expand it; expanded version is given as parameter already
            //however, if target is a TargetPredicateElement, the error is inside the subgraph, and we
            //shouldnt add to the parameter
            reducedToExpandedMap.put(child, target);            
          }
          else {
            //not lifted yet          
            ARTElement expandedChild = (ARTElement)wrappedReducer.getVariableExpandedElement(root, rootSubtree, child);
            reducedToExpandedMap.put(child, expandedChild);
          }
          //explore child later
          openElements.push(child);
        }
        //add edge
        reducedToExpandedMap.get(child).addParent(reducedToExpandedMap.get(currentElement));                
      }   
    }    
    return expandedRoot;
  }
  
  private void recomputeART(AbstractElement pRoot, Precision pRootPrecision, CFANode pRootNode, CachedSubtree rootSubtree) throws InterruptedException {
    //logger.log(Level.FINER, "Recomputing: " + pRoot + " at " + pRootNode);
    
    recomputeARTTimer.start();
    
    CachedSubtree oldSubtree = currentCachedSubtree;
    currentCachedSubtree = rootSubtree;
    for(int i = 0; i < pRootNode.getNumLeavingEdges(); i++) {
      performCompositeAnalysis(pRoot, pRootPrecision, pRootNode, pRootNode.getLeavingEdge(i));
    }
    currentCachedSubtree = oldSubtree;
    recomputeARTTimer.stop();
  }

  public void clearCaches() {
    subgraphReachCache.clear();
    subgraphReturnCache.clear();
  }
  
  @Override
  public Collection<? extends AbstractElement> strengthen(
      AbstractElement pElement, List<AbstractElement> pOtherElements,
      CFAEdge pCfaEdge, Precision pPrecision) throws CPATransferException,
      InterruptedException {
    return wrappedTransfer.strengthen(pElement, pOtherElements, pCfaEdge, pPrecision);
  }

}
