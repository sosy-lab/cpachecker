package org.sosy_lab.cpachecker.cpa.predicate;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Queue;

import org.sosy_lab.common.Pair;
import org.sosy_lab.common.Timer;
import org.sosy_lab.common.Triple;
import org.sosy_lab.common.configuration.InvalidConfigurationException;
import org.sosy_lab.cpachecker.cfa.blocks.BlockPartitioning;
import org.sosy_lab.cpachecker.cfa.objectmodel.CFAEdge;
import org.sosy_lab.cpachecker.cfa.objectmodel.CFANode;
import org.sosy_lab.cpachecker.core.interfaces.AbstractElement;
import org.sosy_lab.cpachecker.core.interfaces.ConfigurableProgramAnalysis;
import org.sosy_lab.cpachecker.core.reachedset.ReachedSet;
import org.sosy_lab.cpachecker.cpa.abm.ABMCPA;
import org.sosy_lab.cpachecker.cpa.abm.RecursiveAnalysisFailedException;
import org.sosy_lab.cpachecker.cpa.art.ARTElement;
import org.sosy_lab.cpachecker.cpa.art.ARTReachedSet;
import org.sosy_lab.cpachecker.cpa.art.Path;
import org.sosy_lab.cpachecker.cpa.callstack.CallstackElement;
import org.sosy_lab.cpachecker.cpa.composite.CompositeElement;
import org.sosy_lab.cpachecker.cpa.predicate.relevantpredicates.RelevantPredicatesComputer;
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


/**
 * Implements predicate refinements when using ABM.
 * @author dwonisch
 *
 */
public class ABMPredicateRefiner extends PredicateRefiner {
  
  private final ABMCPA abmCpa;
  private final ABMPredicateCPA predicateCpa;
  private final FormulaManager fmgr;
  private final PredicateRefinementManager<?, ?> pmgr;
  private final RelevantPredicatesComputer relevantPredicatesComputer;
  
  //Stats  
  final Timer computePathTimer = new Timer();
  final Timer computeSubtreeTimer = new Timer();
  final Timer ssaRenamingTimer = new Timer();
  final Timer computeCounterexampleTimer = new Timer();
  
  public ABMPredicateRefiner(final ConfigurableProgramAnalysis pCpa) throws CPAException, InvalidConfigurationException {
    super(pCpa);
        
    abmCpa = (ABMCPA)pCpa;
    ABMPredicateCPA predicateCpa = this.getArtCpa().retrieveWrappedCpa(ABMPredicateCPA.class);
    if (predicateCpa == null) {
      throw new CPAException(getClass().getSimpleName() + " needs a PredicateCPA");
    }
    
    this.predicateCpa = predicateCpa;
    this.fmgr = predicateCpa.getFormulaManager();
    this.pmgr = predicateCpa.getPredicateManager();
    this.relevantPredicatesComputer = predicateCpa.getRelevantPredicatesComputer();
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
    if (pPath == null) {
      //TODO:  this can be implemented less drastic -> only remove calls on counterexample
      restartAnalysis(pReached);
      return true;
    } else {
      return super.performRefinement(pReached, pPath);
    }
  }
 
 private void restartAnalysis(ARTReachedSet reachSet) {
  
   PredicatePrecision precision = receivePredicatePrecision(reachSet.getPrecision(reachSet.getLastElement()));
   ARTElement child = Iterables.getOnlyElement(reachSet.getFirstElement().getChildren());
   reachSet.removeSubtree(child, precision);
   
   abmCpa.getTransferRelation().clearCaches();
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

    lastErrorPath = null;    

    abmCpa.getTransferRelation().removeSubtree(reachSet, pPath, element, newPrecision);
  }
  
  @Override
  protected Path computePath(ARTElement pLastElement, ReachedSet pReachedSet) throws InterruptedException, RecursiveAnalysisFailedException {
    assert pLastElement.isTarget();

    computePathTimer.start();
    try {
      ARTElement subgraph;
      computeSubtreeTimer.start();
      try {
        subgraph = abmCpa.getTransferRelation().computeCounterexampleSubgraph(pLastElement, pReachedSet, new ARTElement(pLastElement.getWrappedElement(), null));
        if (subgraph == null) {
          return null;
        }
      } finally {
        computeSubtreeTimer.stop();
      }
      
      ssaRenamingTimer.start();
      subgraph = computeSSARenamedSubgraph(subgraph);
      ssaRenamingTimer.stop();
      
      computeCounterexampleTimer.start();
      try {
        return computeCounterexample(subgraph);
      } finally {
        computeCounterexampleTimer.stop();
      }
    } finally {
      computePathTimer.stop();
    }
  }
  
  private Path computeCounterexample(ARTElement root) {    
    Path path = new Path();
    ARTElement currentElement = root;
    while(currentElement.getChildren().size() > 0) {
      ARTElement child = currentElement.getChildren().iterator().next();
      
      CFAEdge edge = currentElement.getEdgeToChild(child);
      path.add(Pair.of(currentElement, edge));
      
      currentElement = child;
    }
    path.add(Pair.of(currentElement, currentElement.retrieveLocationElement().getLocationNode().getLeavingEdge(0)));
    return path;
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
        CFAEdge edge = currentElement.getEdgeToChild(child);
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
  protected Collection<AbstractionPredicate> getPredicatesForARTElement(CounterexampleTraceInfo pInfo, Triple<ARTElement, CFANode, PredicateAbstractElement> pInterpolationPoint) {
    //TODO:
    
    Collection<AbstractionPredicate> preds = pInfo.getPredicatesForRefinement(pInterpolationPoint.getThird());
    CallstackElement callStackElement = AbstractElements.extractElementByType(pInterpolationPoint.getFirst(), CallstackElement.class);
    CFANode node = pInterpolationPoint.getSecond();
    CFANode outerNode = node;
    
    BlockPartitioning partitioning = abmCpa.getBlockPartitioning();
    
    if(partitioning.isCallNode(node) || partitioning.isReturnNode(node)) {
      outerNode = callStackElement.getCallNode();
      if(callStackElement.getDepth() > 1) {
        outerNode = callStackElement.getPreviousElement().getCallNode();
      }    
    }
    
    return relevantPredicatesComputer.getRelevantPredicates(abmCpa.getBlockPartitioning().getBlockForNode(outerNode), preds);
  }    
}
