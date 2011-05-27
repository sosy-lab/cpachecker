package org.sosy_lab.cpachecker.cpa.predicate;

import static com.google.common.collect.Iterables.getOnlyElement;
import static org.sosy_lab.cpachecker.util.AbstractElements.extractElementByType;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Deque;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import org.sosy_lab.common.Pair;
import org.sosy_lab.common.Timer;
import org.sosy_lab.common.Triple;
import org.sosy_lab.common.configuration.InvalidConfigurationException;
import org.sosy_lab.cpachecker.cfa.blocks.BlockPartitioning;
import org.sosy_lab.cpachecker.cfa.objectmodel.CFAEdge;
import org.sosy_lab.cpachecker.cfa.objectmodel.CFANode;
import org.sosy_lab.cpachecker.core.interfaces.ConfigurableProgramAnalysis;
import org.sosy_lab.cpachecker.core.reachedset.ReachedSet;
import org.sosy_lab.cpachecker.cpa.abm.ABMCPA;
import org.sosy_lab.cpachecker.cpa.art.ARTElement;
import org.sosy_lab.cpachecker.cpa.art.ARTReachedSet;
import org.sosy_lab.cpachecker.cpa.art.Path;
import org.sosy_lab.cpachecker.cpa.callstack.CallstackElement;
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
import org.sosy_lab.cpachecker.util.predicates.interfaces.PathFormulaManager;
import org.sosy_lab.cpachecker.util.predicates.interfaces.Region;

import com.google.common.collect.Iterables;
import com.google.common.collect.Lists;


/**
 * Implements predicate refinements when using ABM.
 * @author dwonisch
 *
 */
public class ABMPredicateRefiner extends PredicateRefiner {
  
  private final ABMCPA abmCpa;
  private final FormulaManager fmgr;
  private final PathFormulaManager pfmgr;
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
    
    this.fmgr = predicateCpa.getFormulaManager();
    this.pfmgr = predicateCpa.getPathFormulaManager();
    this.relevantPredicatesComputer = predicateCpa.getRelevantPredicatesComputer();
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
  protected Path computePath(ARTElement pLastElement, ReachedSet pReachedSet) throws InterruptedException, CPATransferException {
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
  
  @Override
  protected List<Triple<ARTElement, CFANode, PredicateAbstractElement>> transformPath(Path pPath) throws CPATransferException {
    // the elements in the path are not expanded, so they contain the path formulas
    // with the wrong indices
    // we need to re-create all path formulas in the flattened ART
    
    computePathTimer.start();

    List<Triple<ARTElement, CFANode, PredicateAbstractElement>> notRenamedPath = super.transformPath(pPath);
    
    ssaRenamingTimer.start();
    try {
      Map<ARTElement, Formula> renamedBlockFormulas = computeBlockFormulas(pPath.getFirst().getFirst());
      
      return replaceFormulasInPath(notRenamedPath, renamedBlockFormulas);

    } finally {
      ssaRenamingTimer.stop();
      computePathTimer.stop();
    }
  }

  private Map<ARTElement, Formula> computeBlockFormulas(ARTElement pRoot) throws CPATransferException {
    
    Map<ARTElement, PathFormula> formulas = new HashMap<ARTElement, PathFormula>();
    Map<ARTElement, Formula> abstractionFormulas = new HashMap<ARTElement, Formula>();
    Deque<ARTElement> todo = new ArrayDeque<ARTElement>();

    // initialize
    assert pRoot.getParents().isEmpty();
    formulas.put(pRoot, pfmgr.makeEmptyPathFormula());
    todo.addAll(pRoot.getChildren());
    
    // iterate over all elements in the ART with BFS
    outer: while (!todo.isEmpty()) {
      ARTElement currentElement = todo.pollFirst();
      if (formulas.containsKey(currentElement)) {
        continue; // already handled
      }
      
      // collect formulas for current location
      List<PathFormula> currentFormulas = new ArrayList<PathFormula>(currentElement.getParents().size());
      for (ARTElement parentElement : currentElement.getParents()) {
        PathFormula parentFormula = formulas.get(parentElement);
        if (parentFormula == null) {
          // parent not handled yet, re-queue current element
          todo.addLast(currentElement);
          continue outer;
        
        } else {
          CFAEdge edge = parentElement.getEdgeToChild(currentElement);
          PathFormula currentFormula = pfmgr.makeAnd(parentFormula, edge);
          currentFormulas.add(currentFormula);
        }
      }
      assert currentFormulas.size() >= 1;
      
      PredicateAbstractElement predicateElement = extractElementByType(currentElement, PredicateAbstractElement.class);
      if (predicateElement instanceof PredicateAbstractElement.AbstractionElement) {
        // abstraction element
        PathFormula currentFormula = getOnlyElement(currentFormulas);
        abstractionFormulas.put(currentElement, currentFormula.getFormula());

        // start new block with empty formula
        assert todo.isEmpty() : "todo should be empty because of the special ART structure";
        formulas.clear(); // free some memory
        
        formulas.put(currentElement, pfmgr.makeEmptyPathFormula(currentFormula));
      
      } else {
        // merge the formulas
        Iterator<PathFormula> it = currentFormulas.iterator();
        PathFormula currentFormula = it.next();
        while (it.hasNext()) {
          currentFormula = pfmgr.makeOr(currentFormula, it.next());
        }
             
        formulas.put(currentElement, currentFormula);
      }
      
      todo.addAll(currentElement.getChildren());
    }
    return abstractionFormulas;
  }
  
  private List<Triple<ARTElement, CFANode, PredicateAbstractElement>> replaceFormulasInPath(
      List<Triple<ARTElement, CFANode, PredicateAbstractElement>> notRenamedPath,
      Map<ARTElement, Formula> blockFormulas) {
    
    List<Triple<ARTElement, CFANode, PredicateAbstractElement>> result = Lists.newArrayListWithExpectedSize(notRenamedPath.size());
    
    assert notRenamedPath.size() == blockFormulas.size();
    
    Region fakeRegion = new Region() { };
    
    for (Triple<ARTElement, CFANode, PredicateAbstractElement> abstractionPoint : notRenamedPath) {
      ARTElement oldARTElement = abstractionPoint.getFirst();
 
      Formula blockFormula = blockFormulas.get(oldARTElement);
      assert blockFormula != null;
      AbstractionFormula abs = new AbstractionFormula(fakeRegion, fmgr.makeTrue(), blockFormula);
      PredicateAbstractElement predicateElement = new PredicateAbstractElement.AbstractionElement(pfmgr.makeEmptyPathFormula(), abs);
      
      result.add(Triple.of(oldARTElement,
                           abstractionPoint.getSecond(),
                           predicateElement));
    }
    return result;
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
