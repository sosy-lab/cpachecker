package de.upb.agw.cpachecker.cpa.abm.predicate;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.Stack;

import org.sosy_lab.common.LogManager;
import org.sosy_lab.common.Timer;
import org.sosy_lab.common.configuration.InvalidConfigurationException;
import org.sosy_lab.common.configuration.Option;
import org.sosy_lab.common.configuration.Options;
import org.sosy_lab.cpachecker.cfa.objectmodel.CFAEdge;
import org.sosy_lab.cpachecker.cfa.objectmodel.CFAFunctionDefinitionNode;
import org.sosy_lab.cpachecker.cfa.objectmodel.CFANode;
import org.sosy_lab.cpachecker.cfa.objectmodel.c.FunctionDefinitionNode;
import org.sosy_lab.cpachecker.core.algorithm.Algorithm;
import org.sosy_lab.cpachecker.core.algorithm.CEGARAlgorithm;
import org.sosy_lab.cpachecker.core.interfaces.AbstractElement;
import org.sosy_lab.cpachecker.core.interfaces.AbstractWrapperElement;
import org.sosy_lab.cpachecker.core.interfaces.ConfigurableProgramAnalysis;
import org.sosy_lab.cpachecker.core.interfaces.Precision;
import org.sosy_lab.cpachecker.core.interfaces.Targetable;
import org.sosy_lab.cpachecker.core.reachedset.PartitionedReachedSet;
import org.sosy_lab.cpachecker.core.reachedset.ReachedSet;
import org.sosy_lab.cpachecker.core.reachedset.UnmodifiableReachedSet;
import org.sosy_lab.cpachecker.core.waitlist.TopologicallySortedWaitlist;
import org.sosy_lab.cpachecker.core.waitlist.Waitlist;
import org.sosy_lab.cpachecker.core.waitlist.Waitlist.WaitlistFactory;
import org.sosy_lab.cpachecker.cpa.art.ARTCPA;
import org.sosy_lab.cpachecker.cpa.art.ARTElement;
import org.sosy_lab.cpachecker.cpa.art.ARTReachedSet;
import org.sosy_lab.cpachecker.cpa.callstack.CallstackElement;
import org.sosy_lab.cpachecker.cpa.composite.CompositeCPA;
import org.sosy_lab.cpachecker.cpa.composite.CompositeElement;
import org.sosy_lab.cpachecker.cpa.composite.CompositePrecision;
import org.sosy_lab.cpachecker.cpa.location.LocationElement;
import org.sosy_lab.cpachecker.cpa.predicate.PredicateAbstractElement;
import org.sosy_lab.cpachecker.cpa.predicate.PredicatePrecision;
import org.sosy_lab.cpachecker.cpa.predicate.PredicateRefinementManager;
import org.sosy_lab.cpachecker.cpa.predicate.PredicateTransferRelation;
import org.sosy_lab.cpachecker.exceptions.CPAException;
import org.sosy_lab.cpachecker.exceptions.CPATransferException;
import org.sosy_lab.cpachecker.util.AbstractElements;
import org.sosy_lab.cpachecker.util.predicates.PathFormula;
import org.sosy_lab.cpachecker.util.predicates.interfaces.FormulaManager;
import org.sosy_lab.cpachecker.util.predicates.interfaces.Region;

import de.upb.agw.cpachecker.cpa.abm.callstack.ABMCallstackCPA;
import de.upb.agw.cpachecker.cpa.abm.location.ABMLocationCPA;
import de.upb.agw.cpachecker.cpa.abm.util.ARTElementSearcher;
import de.upb.agw.cpachecker.cpa.abm.util.CachedSubtree;
import de.upb.agw.cpachecker.cpa.abm.util.CachedSubtreeManager;
import de.upb.agw.cpachecker.cpa.abm.util.PrecisionUtils;
import de.upb.agw.cpachecker.cpa.abm.util.PredicateReducer;
import de.upb.agw.cpachecker.cpa.abm.util.RelevantPredicatesComputer;

/**
 * ABM-based transfer relation. Implements all the caching.
 * @author dwonisch
 *
 */
@Options(prefix="cpa.predicate.abm")
public class ABMPTransferRelation extends PredicateTransferRelation{
  
  private class SubgraphEntryHasher {
    private CachedSubtree context;
    private Region region;
    private PredicatePrecision precision;        
    
    SubgraphEntryHasher(PredicateAbstractElement element, PredicatePrecision precision, CachedSubtree context) {      
      this.precision = precision;
      this.context = context;
      this.region = element.getAbstractionFormula().asRegion();      
    }
    
    @Override
    public boolean equals(Object other) {
      equalsTimer.start();
      try {
        if(!(other instanceof SubgraphEntryHasher)) {
         return false; 
        }
        SubgraphEntryHasher hOther = (SubgraphEntryHasher)other;
        if(!context.equals(hOther.context))
          return false;
        if(!region.equals(hOther.region)) {
          return false;
        }        
        return PrecisionUtils.relevantComparePrecisions(precision, context, hOther.precision, hOther.context, relevantPredicateReducer, csmgr);
      }
      finally {
        equalsTimer.stop();
      }
    }
    
    @Override
    public int hashCode() {
      hashingTimer.start();
      int result = context.hashCode() * 323 + region.hashCode() * 17 + PrecisionUtils.relevantComputeHashCode(precision, context, relevantPredicateReducer, csmgr);
      hashingTimer.stop();
      return result;
    }
    
    @Override
    public String toString() {
      return region.toString() + " (Context: " + context + ")";
    }
  }

  @Option
  public static boolean NO_CACHING = false; //for evaluation purposes 
    
  private Map<SubgraphEntryHasher, ReachedSet> subgraphReachCache;
  private Map<SubgraphEntryHasher, Collection<AbstractElement>> subgraphReturnCache;
  private PredicateReducer predicateReducer;    
  private RelevantPredicatesComputer relevantPredicateReducer;
  
  private LogManager logger;
  private FormulaManager fmgr;
  private PredicateRefinementManager<Integer, Integer> pmgr;
  private ConfigurableProgramAnalysis innerCpa;
  private Algorithm algorithm;
  private ABMLocationCPA locationCpa;
  private CachedSubtree currentCachedSubtree;
  private CachedSubtreeManager csmgr;
  
  //Stats
  int cacheMisses = 0;
  int partialCacheHits = 0;
  int fullCacheHits = 0;
  Timer hashingTimer = new Timer();
  Timer equalsTimer = new Timer();
  Timer recomputeARTTimer = new Timer();
  Timer returnElementsSearchTimer = new Timer();
  Timer removeCachedSubtreeTimer = new Timer();
  
  public ABMPTransferRelation(ABMPredicateCPA pCpa) throws InvalidConfigurationException {
    super(pCpa);
    
    pCpa.getConfiguration().inject(this, ABMPTransferRelation.class);
    
    this.logger = pCpa.getLogger();    
    this.fmgr = pCpa.getFormulaManager();
    this.pmgr = pCpa.getPredicateManager();
    this.relevantPredicateReducer = pCpa.getRelevantPredicatesComputer();
    
    this.predicateReducer = new PredicateReducer(fmgr, pmgr, relevantPredicateReducer);    
    this.subgraphReachCache = new HashMap<SubgraphEntryHasher, ReachedSet>();
    this.subgraphReturnCache = new HashMap<SubgraphEntryHasher, Collection<AbstractElement>>();
  }
  
  public void setAlgorithm(Algorithm algorithm) {    
    assert algorithm instanceof CEGARAlgorithm;
    this.algorithm = ((CEGARAlgorithm)algorithm).getInnerAlgorithm();  
    this.innerCpa = algorithm.getCPA();
    this.locationCpa = ((CompositeCPA)((ARTCPA)innerCpa).getWrappedCpa()).retrieveWrappedCpa(ABMLocationCPA.class);    
  }  
  
  @Override
  public Collection<? extends AbstractElement> getAbstractSuccessors(AbstractElement pElement,
      Precision pPrecision, CFAEdge edge) throws CPATransferException, InterruptedException {    

    CFANode currentNode = edge.getPredecessor();

    if (csmgr.isCallNode(currentNode)) {
      //we have to start a recursive analysis
      if(csmgr.getCachedSubtreeForCallNode(currentNode).equals(currentCachedSubtree)) {
        //we are already in same context
        //thus we already did the recursive call or we a recursion in the cachedSubtrees
        //the latter isnt supported yet, but in the the former case we can classicaly do the post operation
        return super.getAbstractSuccessors(pElement, pPrecision, edge);
      }
      
      if(currentNode instanceof FunctionDefinitionNode && currentNode.getFunctionName().equalsIgnoreCase("main")) {
        //skip main function
        return super.getAbstractSuccessors(pElement, pPrecision, edge);
      }

      PredicateAbstractElement element = (PredicateAbstractElement) pElement;
      PredicatePrecision precision = (PredicatePrecision) pPrecision;

      // Check whether abstraction is false.
      // Such elements might get created when precision adjustment computes an abstraction.
      if (element.getAbstractionFormula().asFormula().isFalse()) {
        //logger.log(Level.FINEST, "Skipping element, as its formulae is false");
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

      CachedSubtree outerSubtree = currentCachedSubtree;
      currentCachedSubtree = csmgr.getCachedSubtreeForCallNode(currentNode);
      Collection<AbstractElement> reducedResult =
          performComposititeAnalysis(element, precision, currentNode, edge);
      
      //logger.log(Level.FINEST, "Cached subgraph " + currentCachedSubtree + " with result " + reducedResult);

      ArrayList<AbstractElement> expandedResult =
          new ArrayList<AbstractElement>(reducedResult.size());
      for (AbstractElement reducedElement : reducedResult) {
        expandedResult.add(predicateReducer.getVariableExpandedElement(element, currentCachedSubtree, (PredicateAbstractElement) reducedElement));
      }
      
      currentCachedSubtree = outerSubtree;

      return expandedResult;      
    }
    else if(currentCachedSubtree != null && currentCachedSubtree.isReturnNode(currentNode)) {
      //do not perform analysis beyond return states //TODO: requires that the return state has only outgoing edges to the "outside"; checked somewhere?
      return Collections.emptySet();
    }
    else {
      return super.getAbstractSuccessors(pElement, pPrecision, edge);
    }
  }
  
  private Collection<AbstractElement> performComposititeAnalysis(PredicateAbstractElement initialPredicateElement, PredicatePrecision initialPredicatePrecision, CFANode node, CFAEdge edge) throws InterruptedException {
    try {
      //logger.log(Level.FINER, "Performing recursive compositite analysis for node " + node + " in state " + initialPredicateElement);
      PredicateAbstractElement reducedInitialElement = predicateReducer.getVariableReducedElement(initialPredicateElement, currentCachedSubtree);
      
      ReachedSet reached;
      if(!NO_CACHING && containsKey(subgraphReturnCache, reducedInitialElement, initialPredicatePrecision, currentCachedSubtree)) {        
        fullCacheHits++;
        //logger.log(Level.FINEST, "Already have cached result for the element (Full Cache Hit)");
        return get(subgraphReturnCache, reducedInitialElement, initialPredicatePrecision, currentCachedSubtree);        
      } 
      else if(!NO_CACHING && containsKey(subgraphReachCache, reducedInitialElement, initialPredicatePrecision, currentCachedSubtree)) {
        //at least we have partly computed reach set cached
        partialCacheHits++;
        //logger.log(Level.FINEST, "Already have partial cached results for the element (Partial Cache Hit)");
        reached = get(subgraphReachCache, reducedInitialElement, initialPredicatePrecision, currentCachedSubtree);
      } else {
        //compute the subgraph specification from scratch
        cacheMisses++;
        //logger.log(Level.FINEST, "No cached results for the element yet (Cache Miss)");
        reached = computeInitialReachedSet(reducedInitialElement, initialPredicatePrecision, node, edge);
      }       
      
      algorithm.run(reached);     
      
      put(subgraphReachCache, reducedInitialElement, initialPredicatePrecision, currentCachedSubtree, reached);      
            
      AbstractElement lastElement = reached.getLastElement();
      
      // if the element is an error element
      if((lastElement instanceof Targetable) && ((Targetable)lastElement).isTarget()) {
        //found a target element inside a recursive subgraph call
        //this needs to be propagated to outer subgraph (till main is reached)
        ArrayList<AbstractElement> result = new ArrayList<AbstractElement>();
        PredicateAbstractElement predicateTarget = AbstractElements.extractElementByType(lastElement, PredicateAbstractElement.class);
        result.add(new TargetPredicateAbstractElement(predicateTarget.getPathFormula(), predicateTarget.getAbstractionFormula()));  
        put(subgraphReturnCache, reducedInitialElement, initialPredicatePrecision, currentCachedSubtree, result);
        return result;        
      }
      
      returnElementsSearchTimer.start();
      ArrayList<AbstractElement> returningNodes = new ArrayList<AbstractElement>();
      for(CFANode returnNode : currentCachedSubtree.getReturnNodes()) {
        Set<AbstractElement> possibleReturningNodes = approximateReturningNodesSet(reached, returnNode);        
        for(AbstractElement e : possibleReturningNodes) {
          assert e instanceof AbstractWrapperElement;
          if(((AbstractWrapperElement)e).retrieveLocationElement().getLocationNode().equals(returnNode)) {
            assert AbstractElements.extractElementByType(e, PredicateAbstractElement.AbstractionElement.class) != null : "return nodes need to be AbstractionElements (" + e + ")";
            returningNodes.add(AbstractElements.extractElementByType(e, PredicateAbstractElement.class));
          }
        }
      }
      returnElementsSearchTimer.stop();
      put(subgraphReturnCache, reducedInitialElement, initialPredicatePrecision, currentCachedSubtree, returningNodes);
      
      return returningNodes;
    } catch (CPAException e) {
      throw new RuntimeException(e);
    }    
  }
  
  private Set<AbstractElement> approximateReturningNodesSet(ReachedSet pReached, CFANode pNode) {
    AbstractElement firstElement = pReached.getFirstElement(); 
    //the first element is AbstractionElement, non-false, and callstack is correct too; only the location is wrong
    //so replace it
    firstElement = replaceComponent((ARTElement) firstElement, locationCpa.getLocationForNode(pNode));
    return pReached.getReached(firstElement);
  }

  @Override
  protected boolean isBlockEnd(CFANode succLoc, PathFormula pf) {
    return super.isBlockEnd(succLoc, pf) || csmgr.isCallNode(succLoc) || csmgr.isReturnNode(succLoc);    
  }

  
  private ReachedSet computeInitialReachedSet(PredicateAbstractElement reducedInitialElement, PredicatePrecision initialPredicatePrecision, CFANode node, CFAEdge edge) throws CPATransferException {
    //get a fresh AbstractElement and a fresh precision
    AbstractElement initialElement = innerCpa.getInitialElement((CFAFunctionDefinitionNode)csmgr.getMainSubtree().getCallNode());
    Precision initialPrecision = innerCpa.getInitialPrecision((CFAFunctionDefinitionNode)csmgr.getMainSubtree().getCallNode());
    
    assert initialElement instanceof ARTElement && initialPrecision instanceof CompositePrecision : "Adjustable block memorizing only works with composite ART analysis (should be configured with at least ABMLocationCPA, CallstackCPA, and ABMPredicateCPA).";
    ARTElement artElement = (ARTElement)initialElement;
    CompositeElement compositeInitialElement = (CompositeElement)artElement.getWrappedElement();
    CompositePrecision compositeInitialPrecision = (CompositePrecision)initialPrecision;

    //we need to replace the predicate part of initialElement and initialPrecision with the given parameters
    //furthermore we need to fix the location and callstack element 
    List<AbstractElement> newElements = new ArrayList<AbstractElement>(compositeInitialElement.getNumberofElements());
    List<Precision> newPrecisions = new ArrayList<Precision>(compositeInitialPrecision.getPrecisions().size());
    assert compositeInitialElement.getNumberofElements() == compositeInitialPrecision.getPrecisions().size();
    for(int i = 0; i < compositeInitialElement.getNumberofElements(); i++) {
      AbstractElement e = compositeInitialElement.get(i);
      Precision p = compositeInitialPrecision.get(i);
      if(e instanceof PredicateAbstractElement) {
        newElements.add(reducedInitialElement);
        assert p instanceof PredicatePrecision;
        newPrecisions.add(initialPredicatePrecision);        
      }
      else if(e instanceof LocationElement) {
        newElements.add(locationCpa.getLocationForNode(node));        
        newPrecisions.add(p);
      }
      else if(e instanceof CallstackElement) {
        newElements.add(new CallstackElement(null, node.getFunctionName(), node));
        newPrecisions.add(p);
      }
      else {
        newElements.add(e);
        newPrecisions.add(p);
      }
    }
    initialElement = new CompositeElement(newElements);   
    initialElement = new ARTElement(initialElement, null);
    initialPrecision = new CompositePrecision(newPrecisions);
    
    //TODO: respect configuration
    WaitlistFactory waitlistFactory = Waitlist.TraversalMethod.BFS;
    waitlistFactory = TopologicallySortedWaitlist.factory(waitlistFactory);
    //waitlistFactory = CallstackSortedWaitlist.factory(waitlistFactory);
    
    ReachedSet reached = new PartitionedReachedSet(waitlistFactory);
    reached.add(initialElement, initialPrecision);
    return reached;
  }
  
  private void recomputeART(PredicateAbstractElement pRoot, PredicatePrecision pRootPrecision, CFANode pRootNode, CachedSubtree rootSubtree) throws InterruptedException {
    //logger.log(Level.FINER, "Recomputing: " + pRoot + " at " + pRootNode);
    
    recomputeARTTimer.start();
    
    CachedSubtree oldSubtree = currentCachedSubtree;
    currentCachedSubtree = rootSubtree;
    for(int i = 0; i < pRootNode.getNumLeavingEdges(); i++) {
      performComposititeAnalysis(pRoot, pRootPrecision, pRootNode, pRootNode.getLeavingEdge(i));
    }
    currentCachedSubtree = oldSubtree;
    recomputeARTTimer.stop();
  }

  private ARTElement replaceComponent(ARTElement element, PredicateAbstractElement component) {
    return new ARTElement(replaceComponent((CompositeElement)element.getWrappedElement(), component), null);
  }
  
  private CompositeElement replaceComponent(CompositeElement element, PredicateAbstractElement component) {
    List<AbstractElement> elements = element.getWrappedElements();
    List<AbstractElement> newElements = new ArrayList<AbstractElement>(elements.size());
    for(AbstractElement e : elements) {
      if(e instanceof PredicateAbstractElement) {
        newElements.add(component);
      } else {
        newElements.add(e);
      }
    }
    return new CompositeElement(newElements);
  }
  
  private ARTElement replaceComponent(ARTElement element, LocationElement component) {
    return new ARTElement(replaceComponent((CompositeElement)element.getWrappedElement(), component), null);
  }
  
  private CompositeElement replaceComponent(CompositeElement element, LocationElement component) {
    List<AbstractElement> elements = element.getWrappedElements();
    List<AbstractElement> newElements = new ArrayList<AbstractElement>(elements.size());
    for(AbstractElement e : elements) {
      if(e instanceof LocationElement) {
        newElements.add(component);
      } else {
        newElements.add(e);
      }
    }
    return new CompositeElement(newElements);
  }

  public ReachedSet getReachedSet(ARTElement element, PredicatePrecision predicatePrecision) {
    CachedSubtree context = csmgr.getCachedSubtreeForCallNode(element.retrieveLocationElement().getLocationNode());
    PredicateAbstractElement reducedElement = predicateReducer.getVariableReducedElement(element, context);    
    return get(subgraphReachCache, reducedElement, predicatePrecision, context);
  }
  
  public void removeCachedSubtree(PredicateAbstractElement rootPredicateElement, PredicatePrecision rootPredicatePrecision, CFANode rootNode, ARTElement removeElement, PredicatePrecision newPredicatePrecision) {
    //logger.log(Level.FINER, "Remove cached subtree for " + removeElement + " (rootNode: " + rootNode + ") issued");
    
    removeCachedSubtreeTimer.start();
    
    try {
      CachedSubtree rootSubtree = csmgr.getCachedSubtreeForCallNode(rootNode);
      
      PredicateAbstractElement reducedRootElement = predicateReducer.getVariableReducedElement(rootPredicateElement, rootSubtree);
         
      ReachedSet reachedSet = get(subgraphReachCache, reducedRootElement, rootPredicatePrecision, rootSubtree);   
      ARTElement reducedRemovePredicateElement = ARTElementSearcher.searchForARTElement(reachedSet, removeElement, predicateReducer, csmgr);
      
      if(reducedRemovePredicateElement.getParents().isEmpty()) {
        //this is actually the root of the subgraph; so remove the whole thing
        //logger.log(Level.FINEST, "Removing root of cached tree (i.e., remove the whole tree)");
        remove(subgraphReachCache, reducedRootElement, rootPredicatePrecision, rootSubtree);
        remove(subgraphReturnCache, reducedRootElement, rootPredicatePrecision, rootSubtree);
        return;
      }
      if(rootPredicatePrecision.equals(newPredicatePrecision)) {
        //newPrecision is same as oldPrecision; in this case just remove the subtree and force a recomputation of the ART
        //by removing it from the cache
        //logger.log(Level.FINEST, "New precision equals old precision: Removing the subtree and the cache entry for the cached tree to force a recomputation");
        remove(subgraphReturnCache, reducedRootElement, rootPredicatePrecision, rootSubtree);
        removeSubtree(reachedSet, reducedRemovePredicateElement, newPredicatePrecision);    
        return;
      }
      if(containsKey(subgraphReachCache, reducedRootElement, newPredicatePrecision, rootSubtree)) {
        //logger.log(Level.FINEST, "Cached result for the new precision is already present. Noting to do.");
        //we already have a cached value for the new precision
        //no recomputation necessary; do nothing
        return;
      }
      //logger.log(Level.FINEST, "Normal case: Removing subtree, adding a new cached entry, and removing the former cached entries");
      //no special case: remove subtree, and add a new cache entry, remove old cache entries
      removeSubtree(reachedSet, reducedRemovePredicateElement, newPredicatePrecision);
      
      //System.out.println("REMOVE: " + reducedRootElement.getAbstractionFormula().asRegion() + " for subtree " + rootSubtree.getCallNode());
      remove(subgraphReachCache, reducedRootElement, rootPredicatePrecision, rootSubtree);
      remove(subgraphReturnCache, reducedRootElement, rootPredicatePrecision, rootSubtree);
      
      put(subgraphReachCache, reducedRootElement, newPredicatePrecision, rootSubtree, reachedSet);      
    }
    finally {
      removeCachedSubtreeTimer.stop();
    }
  }
  
  public Precision getPrecision(UnmodifiableReachedSet reached, ARTElement target) {    
    ARTElement ele = ARTElementSearcher.searchForARTElement(reached, target, predicateReducer, csmgr);
    return reached.getPrecision(ele);
  }
   
  private void removeSubtree(ReachedSet reachedSet, ARTElement artElement, Precision artPrecision) {
    if(!reachedSet.contains(artElement)) {     
      throw new IllegalArgumentException("Given ReachedSet does not contain given ARTElement.");
    }
    ARTReachedSet artReachSet = new ARTReachedSet(reachedSet, (ARTCPA)innerCpa);
    artReachSet.removeSubtree(artElement, artPrecision);
  }
  
  private <T> T put(Map<SubgraphEntryHasher, T> map, PredicateAbstractElement predicateKey, PredicatePrecision precisionKey, CachedSubtree context, T item) {
    return map.put(new SubgraphEntryHasher(predicateKey, precisionKey, context), item);
  }
  
  private Object remove(Map<SubgraphEntryHasher, ?> map, PredicateAbstractElement predicateKey, PredicatePrecision precisionKey, CachedSubtree context) {
    return map.remove(new SubgraphEntryHasher(predicateKey, precisionKey, context));
  }
  
  private boolean containsKey(Map<SubgraphEntryHasher, ?> map, PredicateAbstractElement predicateKey, PredicatePrecision precisionKey, CachedSubtree context) {
    return map.containsKey(new SubgraphEntryHasher(predicateKey, precisionKey, context));
  }
  
  private <T> T get(Map<SubgraphEntryHasher, T> map, PredicateAbstractElement predicateKey, PredicatePrecision precisionKey, CachedSubtree context) {
    return map.get(new SubgraphEntryHasher(predicateKey, precisionKey, context));
  }

  public PredicateReducer getPredicateReducer() {
    return predicateReducer;
  }
  
  protected ARTElement computeCounterexampleSubgraph(ARTElement root, ARTElement target, PredicatePrecision rootPrecision, ABMPRefiner caller) throws InterruptedException {
    PredicateAbstractElement rootPredicateElement = AbstractElements.extractElementByType(root, PredicateAbstractElement.class);
    CFANode rootNode = root.retrieveLocationElement().getLocationNode();
    CachedSubtree rootSubtree = csmgr.getCachedSubtreeForCallNode(rootNode);
    
    PredicateAbstractElement targetPredicateElement = AbstractElements.extractElementByType(target, PredicateAbstractElement.class);
        
    PredicateAbstractElement reducedRootElement = predicateReducer.getVariableReducedElement(root, rootSubtree);
    ReachedSet reachSet = get(subgraphReachCache, reducedRootElement, rootPrecision, rootSubtree);
    if(reachSet == null) {
      //recompute the ART
      recomputeART(rootPredicateElement, rootPrecision, rootNode, rootSubtree);
      reachSet = get(subgraphReachCache, reducedRootElement, rootPrecision, rootSubtree);
      if(reachSet == null) {
        throw new IllegalArgumentException("Failed recomputing the reach set of " + root + " -> " + target  + " with precision " +  rootPrecision); 
      }
    }    
    //we found the to the root and precision corresponding reach set
    //now try to find the target in the reach set    
    AbstractElement targetARTElement = ARTElementSearcher.searchForARTElement(reachSet, target, predicateReducer, csmgr);    
    if(targetARTElement == null) {   
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
    PredicateAbstractElement expandedPredicateRoot = predicateReducer.getVariableExpandedElement(root, rootSubtree, reducedSubgraph);
    ARTElement expandedRoot = replaceComponent(reducedSubgraph, expandedPredicateRoot);
    
    //next traverse the tree
    Map<ARTElement, ARTElement> reducedToExpandedMap = new HashMap<ARTElement, ARTElement>();
    Stack<ARTElement> openElements = new Stack<ARTElement>();
        
    reducedToExpandedMap.put(reducedSubgraph, expandedRoot);
    openElements.push(reducedSubgraph);    
    while(!openElements.isEmpty()) {
      ARTElement currentElement = openElements.pop();
      
      for(ARTElement child : currentElement.getChildren()) {
        if(!reducedToExpandedMap.containsKey(child)) {
          if(child.getChildren().isEmpty() && !(targetPredicateElement instanceof TargetPredicateAbstractElement)) {
            //no children -> should be target
            //thus no need to expand it; expanded version is given as parameter already
            //however, if target is a TargetPredicateElement, the error is inside the subgraph, and we
            //shouldnt add to the parameter
            reducedToExpandedMap.put(child, target);            
          }
          else {
            //not lifted yet          
            PredicateAbstractElement expandedPredicateChild = predicateReducer.getVariableExpandedElement(root, rootSubtree, child);
            ARTElement expandedChild = replaceComponent(child, expandedPredicateChild);
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

  public void clearCaches() {
    subgraphReachCache.clear();
    subgraphReturnCache.clear();
  }
  
  public CachedSubtreeManager getCachedSubtreeManager() {
    return csmgr;
  }
  
  public void setCachedSubtreeManager(CachedSubtreeManager pManager) {
    csmgr = pManager;
    currentCachedSubtree = csmgr.getMainSubtree();
    locationCpa.setPredicateTransferRelation(this);
    (((CompositeCPA)((ARTCPA)innerCpa).getWrappedCpa()).retrieveWrappedCpa(ABMCallstackCPA.class)).setPredicateTransferRelation(this); 
  }

  public boolean isNewCallNode(CFANode pNode) {
    if(currentCachedSubtree.isCallNode(pNode)) {
      return false;
    }
    return csmgr.isCallNode(pNode);
  }

  public CachedSubtree getCurrentCachedSubtree() {
    return currentCachedSubtree;
  }

  public RelevantPredicatesComputer getRelevantPredicatesComputer() {
    return relevantPredicateReducer;
  }

  /*public boolean noTargetReached() {
    for(SubgraphEntryHasher key : subgraphReachCache.keySet()) {
      ReachedSet reached = subgraphReachCache.get(key);
      for(AbstractElement element : reached) {
        if((element instanceof Targetable) && ((Targetable)element).isTarget()) {
          System.out.println("Found error state for key " + key);
          return false;
        }
      }
    }
    return true;
  }*/
}
