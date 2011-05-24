package de.upb.agw.cpachecker.cpa.abm.predicate;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Deque;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Queue;
import java.util.Set;
import java.util.Stack;

import org.sosy_lab.common.LogManager;
import org.sosy_lab.common.Pair;
import org.sosy_lab.common.Timer;
import org.sosy_lab.common.Triple;
import org.sosy_lab.common.configuration.InvalidConfigurationException;
import org.sosy_lab.common.configuration.Options;
import org.sosy_lab.cpachecker.cfa.objectmodel.CFAEdge;
import org.sosy_lab.cpachecker.cfa.objectmodel.CFANode;
import org.sosy_lab.cpachecker.cfa.objectmodel.c.FunctionDefinitionNode;
import org.sosy_lab.cpachecker.core.interfaces.AbstractElement;
import org.sosy_lab.cpachecker.core.interfaces.ConfigurableProgramAnalysis;
import org.sosy_lab.cpachecker.core.reachedset.ReachedSet;
import org.sosy_lab.cpachecker.core.reachedset.UnmodifiableReachedSet;
import org.sosy_lab.cpachecker.cpa.art.ARTElement;
import org.sosy_lab.cpachecker.cpa.art.ARTReachedSet;
import org.sosy_lab.cpachecker.cpa.art.Path;
import org.sosy_lab.cpachecker.cpa.callstack.CallstackElement;
import org.sosy_lab.cpachecker.cpa.composite.CompositeElement;
import org.sosy_lab.cpachecker.cpa.predicate.PredicateAbstractElement;
import org.sosy_lab.cpachecker.cpa.predicate.PredicatePrecision;
import org.sosy_lab.cpachecker.cpa.predicate.PredicateRefinementManager;
import org.sosy_lab.cpachecker.cpa.predicate.PredicateRefiner;
import org.sosy_lab.cpachecker.exceptions.CPAException;
import org.sosy_lab.cpachecker.exceptions.CPATransferException;
import org.sosy_lab.cpachecker.util.AbstractElements;
import org.sosy_lab.cpachecker.util.predicates.AbstractionFormula;
import org.sosy_lab.cpachecker.util.predicates.AbstractionPredicate;
import org.sosy_lab.cpachecker.util.predicates.CounterexampleTraceInfo;
import org.sosy_lab.cpachecker.util.predicates.PathFormula;
import org.sosy_lab.cpachecker.util.predicates.interfaces.Formula;
import org.sosy_lab.cpachecker.util.predicates.interfaces.FormulaManager;
import org.sosy_lab.cpachecker.util.predicates.interfaces.Region;

import com.google.common.collect.Iterables;

import de.upb.agw.cpachecker.cpa.abm.util.ARTElementSearcher;
import de.upb.agw.cpachecker.cpa.abm.util.CachedSubtree;

/**
 * Implements predicate refinements when using ABM.
 * @author dwonisch
 *
 */
@Options(prefix="cpa.predicate.fcc")
public class ABMPRefiner extends PredicateRefiner {
  ABMPredicateCPA predicateCpa;
  private LogManager logger;
  private FormulaManager fmgr;
  private PredicateRefinementManager<Integer, Integer> pmgr;
  
  //Stats  
  Timer removeSubtreeTimer = new Timer();
  Timer computeSubtreeTimer = new Timer();
  Timer ssaRenamingTimer = new Timer();
  Timer computeCounterexampleTimer = new Timer();
  
  public ABMPRefiner(final ConfigurableProgramAnalysis pCpa) throws CPAException, InvalidConfigurationException {
    super(pCpa);
        
    ABMPredicateCPA predicateCpa = this.getArtCpa().retrieveWrappedCpa(ABMPredicateCPA.class);
    if (predicateCpa == null) {
      throw new CPAException(getClass().getSimpleName() + " needs a PredicateCPA");
    }
    predicateCpa.getFCCPStats().addRefiner(this);
    this.predicateCpa = predicateCpa;
    this.logger = predicateCpa.getLogger();
    this.fmgr = predicateCpa.getFormulaManager();
    this.pmgr = predicateCpa.getPredicateManager();
  }
  
  private ARTElement replaceComponent(ARTElement element, PredicateAbstractElement component, ARTElement parent) {
    return new ARTElement(replaceComponent((CompositeElement)element.getWrappedElement(), component), parent);
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
    
 @Override
  public boolean performRefinement(ARTReachedSet pReached, Path pPath) throws CPAException, InterruptedException {
   //logger.log(Level.FINER, "Performing refinement for path " + pPath);
   if(pPath == null) {
     //TODO:  this can be implemented less drastic -> only remove calls on counterexample
     restartAnalysis(pReached);
     return true;
   }
   else {
     return super.performRefinement(pReached, pPath);
   }
  }
 
 private void restartAnalysis(ARTReachedSet reachSet) {
   assert reachSet.getFirstElement().getChildren().size() == 1;
   
   PredicatePrecision precision = receivePredicatePrecision(reachSet.getPrecision(reachSet.getLastElement()));
   ARTElement child = reachSet.getFirstElement().getChildren().iterator().next();
   reachSet.removeSubtree(child, precision);
   
   predicateCpa.getTransferRelation().clearCaches();
 }
  
  @Override
  protected void removeSubtree(ARTReachedSet reachSet, Path pPath, ARTElement element, PredicatePrecision newPrecision) {
    PredicatePrecision oldPrecision = receivePredicatePrecision(reachSet.getPrecision(reachSet.getLastElement()));
    if(newPrecision.equals(oldPrecision)) {
      //Strategy 2
      //restart the analysis
      //TODO: this can be implemented less drastic -> only remove lazy caches (on path)      
      restartAnalysis(reachSet);
      return;     
    }
        
    removeSubtreeTimer.start();
    
    lastErrorPath = null;    
    Path path = trimPath(pPath, element);
   
    Set<ARTElement> relevantCallNodes = getRelevantDefinitionNodes(path);
    
    ARTElement lastCallNode = null;
    Stack<Triple<UnmodifiableReachedSet, PredicatePrecision, CFANode>> currentReachSetStack = new Stack<Triple<UnmodifiableReachedSet, PredicatePrecision, CFANode>>();    
    currentReachSetStack.add(null);
    //iterate from root to element and remove all subtrees for subgraph calls
    for(ARTElement currentElement : Iterables.transform(Iterables.skip(path, 1), Pair.<ARTElement>getProjectionToFirst())) {
      CFANode node = currentElement.retrieveLocationElement().getLocationNode();
      
      boolean relevantCall = false;
      if(relevantCallNodes.contains(currentElement)) {
       relevantCall = true;
      }
      
      if(currentElement.equals(element) || relevantCall) {        
        PredicatePrecision currentPredicatePrecision = removeCachedSubtree(currentReachSetStack, currentElement, reachSet, newPrecision, lastCallNode);
      
        if(relevantCall) {
          lastCallNode = currentElement;               
          currentReachSetStack.push(new Triple<UnmodifiableReachedSet, PredicatePrecision, CFANode>(predicateCpa.getTransferRelation().getReachedSet(currentElement, currentPredicatePrecision), currentPredicatePrecision, node));
          if(relevantCall && currentElement.equals(element)) {
            //lastelement is a relevant call, redo the cached subtree removal once again
            currentPredicatePrecision = removeCachedSubtree(currentReachSetStack, currentElement, reachSet, newPrecision, lastCallNode);
          }
        }
      }
    }    
    
    removeSubtreeTimer.stop();  
  }
  
  private PredicatePrecision removeCachedSubtree(Stack<Triple<UnmodifiableReachedSet, PredicatePrecision, CFANode>> currentReachSetStack, ARTElement currentElement, ARTReachedSet reachSet, PredicatePrecision newPrecision, ARTElement lastCallNode) {
    PredicatePrecision currentPredicatePrecision = getPrecision(currentReachSetStack.peek(), currentElement); 
    if(currentPredicatePrecision == null) {
      currentPredicatePrecision = receivePredicatePrecision(getPrecision(reachSet.asReachedSet(), currentElement));
    }
    
    //we need to remove the subtree in the cached ART, in which the currentElement is found
    if(currentReachSetStack.peek() == null) {
      //called in main function; in this case we simply remove the subtree in the global ART   
      ARTElement reachSetARTElement = ARTElementSearcher.searchForARTElement(reachSet, currentElement, null, null);
      
      reachSet.removeSubtree(reachSetARTElement, newPrecision);          
    }
    else {
      //called in a subgraphs ART; in this case we need to tell the transfer function to update its cached
      //specifications such that the subtree is removed in the subgraphs ART
      PredicateAbstractElement lastDefinitionPredicateElement = AbstractElements.extractElementByType(lastCallNode, PredicateAbstractElement.class);
      PredicatePrecision lastDefinitionNodePrecision = currentReachSetStack.peek().getSecond();
      CFANode lastDefinitionCFANode = currentReachSetStack.peek().getThird();
      
      predicateCpa.getTransferRelation().removeCachedSubtree(lastDefinitionPredicateElement, lastDefinitionNodePrecision, lastDefinitionCFANode, currentElement, newPrecision);
    }
    
    return currentPredicatePrecision;
  }  
  
  private PredicatePrecision getPrecision(Triple<UnmodifiableReachedSet, PredicatePrecision, CFANode> root, ARTElement target) {
    if(root == null) {
      return null;
    }
    return getPrecision(root.getFirst(), target);
  }
  
  private PredicatePrecision getPrecision(UnmodifiableReachedSet reached, ARTElement target) {
    if(reached == null) {
      return null;
    }
    return receivePredicatePrecision(predicateCpa.getTransferRelation().getPrecision(reached, target));    
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
      if(predicateCpa.getTransferRelation().getCachedSubtreeManager().isCallNode(node)) {
        if(!(node instanceof FunctionDefinitionNode && node.getFunctionName().equalsIgnoreCase("main"))) {
          openCallElements.push(currentElement);    
          openSubtrees.push(predicateCpa.getTransferRelation().getCachedSubtreeManager().getCachedSubtreeForCallNode(node));
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
  
  @Override
  protected Path computePath(ARTElement pLastElement, ReachedSet pReachedSet) throws InterruptedException {
    computeSubtreeTimer.start();
    ARTElement subgraph = computeCounterexampleSubgraph(pLastElement, pReachedSet);
    computeSubtreeTimer.stop();
    if(subgraph == null) {
      return null;
    }
    ssaRenamingTimer.start();
    subgraph = computeSSARenamedSubgraph(subgraph);
    ssaRenamingTimer.stop();
    
    computeCounterexampleTimer.start();
    try {
      return computeCounterexample(subgraph);
    }
    finally {
      computeCounterexampleTimer.stop();
    }
  }
  
  private Path computeCounterexample(ARTElement root) {    
    Path path = new Path();
    ARTElement currentElement = root;
    while(currentElement.getChildren().size() > 0) {
      ARTElement child = currentElement.getChildren().iterator().next();
      
      CFAEdge edge = getEdgeToChild(currentElement, child);
      path.add(Pair.of(currentElement, edge));
      
      currentElement = child;
    }
    path.add(Pair.of(currentElement, currentElement.retrieveLocationElement().getLocationNode().getLeavingEdge(0)));
    return path;
  }
  
  //returns root of a subtree leading from the root element of the given reachedSet to the target element
  //subtree is represented using children and parents of ARTElements
  protected ARTElement computeCounterexampleSubgraph(ARTElement target, ReachedSet reachedSet) throws InterruptedException {
    //start by creating ARTElements for each node needed in the tree 
    Map<ARTElement, ARTElement> elementsMap = new HashMap<ARTElement, ARTElement>();
    Stack<ARTElement> openElements = new Stack<ARTElement>();
    ARTElement root = null;
    
    elementsMap.put(target, new ARTElement(target.getWrappedElement(), null));
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
          ARTElement innerTree = predicateCpa.getTransferRelation().computeCounterexampleSubgraph(parent, elementsMap.get(currentElement), receivePredicatePrecision(reachedSet.getPrecision(parent)), this);
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
    return root;
  }
  
  private ARTElement computeSSARenamedSubgraph(ARTElement root) {
    Map<ARTElement, ARTElement> nodeToSSANode = new HashMap<ARTElement, ARTElement>();
    Queue<ARTElement> openElements = new LinkedList<ARTElement>();
    
    nodeToSSANode.put(root, new ARTElement(root.getWrappedElement(), null)); //no SSA renaming for root needed
    openElements.add(root);
    while(!openElements.isEmpty()) {
      ARTElement currentElement = openElements.poll();
      assert nodeToSSANode.get(currentElement).getParents().size() == currentElement.getParents().size();
      
      for(ARTElement child : currentElement.getChildren()) {
        CFAEdge edge = getEdgeToChild(currentElement, child);
        assert edge != null; //subgraph is subgraph call expanded; no summarized function calls involved
        
        PredicateAbstractElement ssaRenamedPredicateElement = getSSARenamedElement(child, nodeToSSANode.get(currentElement), edge);                
        
        if(!nodeToSSANode.containsKey(child)) {
          //if we see the child for the first time, we can compute the SSA renaming from scratch using edge
          //thus, just make an ARTElement of the just computed ssaRenamedPredicateElement
          ARTElement ssaChildElement = replaceComponent(child, ssaRenamedPredicateElement, nodeToSSANode.get(currentElement));
          nodeToSSANode.put(child, ssaChildElement);
          if(child.getParents().size() == 1) {
            //otherwise, we need to merge anyway
            openElements.add(child);
          }
        }
        else {
          //otherwise, we have to merge nodes
          ARTElement outdatedSSAElement = nodeToSSANode.get(child);
          PredicateAbstractElement ssaMergedPredicateElement = getSSAMergedElement(outdatedSSAElement, ssaRenamedPredicateElement);
          ARTElement ssaChildElement = replaceComponent(child, ssaMergedPredicateElement, nodeToSSANode.get(currentElement));
          for(ARTElement innerParent : outdatedSSAElement.getParents()) {
            ssaChildElement.addParent(innerParent);
          }          
          outdatedSSAElement.removeFromART();
          
          nodeToSSANode.put(child, ssaChildElement);
          if(child.getParents().size() == ssaChildElement.getParents().size()) {
            openElements.add(child);
          }
        }
      }
    }
    return nodeToSSANode.get(root);
  }
  
  private PredicateAbstractElement getSSAMergedElement(ARTElement artElement, PredicateAbstractElement rhs) {
    return getSSAMergedElement(AbstractElements.extractElementByType(artElement, PredicateAbstractElement.class), rhs); //reihenfolge?
  }
  
  private PredicateAbstractElement getSSAMergedElement(PredicateAbstractElement elem1, PredicateAbstractElement elem2) {
    return (PredicateAbstractElement)predicateCpa.getMergeOperator().merge(elem1, elem2, null);
  }

  private PredicateAbstractElement getSSARenamedElement(ARTElement currentElement, ARTElement parentElement, CFAEdge lastEdge) {
    PredicateAbstractElement currentPredicateElement = AbstractElements.extractElementByType(currentElement, PredicateAbstractElement.class);
    PredicateAbstractElement parentPredicateElement = AbstractElements.extractElementByType(parentElement, PredicateAbstractElement.class);
    return getSSARenamedElement(currentPredicateElement, parentPredicateElement.getPathFormula(), lastEdge, parentPredicateElement.getAbstractionFormula());       
  }
  
  private PredicateAbstractElement getSSARenamedElement(PredicateAbstractElement element, PathFormula lastPathFormula, CFAEdge lastEdge, AbstractionFormula lastAbstractionFormula) {
    boolean isAbstractionLocation = element instanceof PredicateAbstractElement.AbstractionElement;
    
    PathFormula pathFormula;
    try {
      pathFormula = predicateCpa.getTransferRelation().convertEdgeToPathFormula(lastPathFormula, lastEdge);
    } catch (CPATransferException e) {
      throw new RuntimeException(e);
    } 
    
        
    AbstractionFormula newAbstractionFormula;
    if(isAbstractionLocation) {
      Region newRegion = element.getAbstractionFormula().asRegion(); //no need to change region as it contains no SSA renaming anyway
      Formula newFormula = fmgr.instantiate(pmgr.toConcrete(newRegion), pathFormula.getSsa());
      Formula blockFormula = pathFormula.getFormula();
      assert blockFormula != null;
      pathFormula = pmgr.getPathFormulaManager().makeEmptyPathFormula(pathFormula); 
      newAbstractionFormula = new AbstractionFormula(newRegion, newFormula, blockFormula);
    }
    else {
      newAbstractionFormula = lastAbstractionFormula;
    }
    
    if(isAbstractionLocation) {      
      return new PredicateAbstractElement.AbstractionElement(pathFormula, newAbstractionFormula);
    } else {
      return new PredicateAbstractElement(pathFormula, newAbstractionFormula);
    }
       
  } 
  
  @Override
  protected Collection<AbstractionPredicate> getPredicatesForARTElement(CounterexampleTraceInfo pInfo, ARTElement pAE) {
    //TODO:
    
    PredicateAbstractElement predicateElement = AbstractElements.extractElementByType(pAE, PredicateAbstractElement.class);
    Collection<AbstractionPredicate> preds = pInfo.getPredicatesForRefinement(predicateElement);
    CallstackElement callStackElement = AbstractElements.extractElementByType(pAE, CallstackElement.class);
    CFANode node = pAE.retrieveLocationElement().getLocationNode();
    CFANode outerNode = node;
    
    if(predicateCpa.getTransferRelation().getCachedSubtreeManager().isCallNode(node) || predicateCpa.getTransferRelation().getCachedSubtreeManager().isReturnNode(node)) {
      outerNode = callStackElement.getCallNode();
      if(callStackElement.getDepth() > 1) {
        outerNode = callStackElement.getPreviousElement().getCallNode();
      }    
    }
    
    return predicateCpa.getTransferRelation().getRelevantPredicatesComputer().getRelevantPredicates(predicateCpa.getTransferRelation().getCachedSubtreeManager().getCachedSubtreeForNode(outerNode), preds);
  }  
  
  private CFAEdge getEdgeToChild(ARTElement parent, ARTElement child) {
    CFANode currentLoc = parent.retrieveLocationElement().getLocationNode();
    CFANode childNode = child.retrieveLocationElement().getLocationNode();

    return getEdgeTo(currentLoc, childNode);
  }
  
  private CFAEdge getEdgeTo(CFANode node1, CFANode node2) {
    for(int i = 0; i < node1.getNumLeavingEdges(); i++) {
      CFAEdge edge = node1.getLeavingEdge(i);
      if(edge.getSuccessor() == node2) {
        return edge;
      }
    }
    return null;   
  }  
}
