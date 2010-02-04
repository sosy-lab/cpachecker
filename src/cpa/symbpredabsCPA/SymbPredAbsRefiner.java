package cpa.symbpredabsCPA;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Deque;
import java.util.HashSet;
import java.util.Set;
import java.util.logging.Level;

import symbpredabstraction.interfaces.Predicate;
import symbpredabstraction.trace.CounterexampleTraceInfo;
import cfa.objectmodel.CFAEdge;
import cfa.objectmodel.CFANode;

import com.google.common.collect.ImmutableSetMultimap;
import com.google.common.collect.Multimap;
import common.Pair;
import compositeCPA.CompositeCPA;

import cpa.art.ARTElement;
import cpa.art.ARTReachedSet;
import cpa.art.AbstractARTBasedRefiner;
import cpa.art.Path;
import cpa.common.CPAchecker;
import cpa.common.interfaces.ConfigurableProgramAnalysis;
import cpa.common.interfaces.Precision;
import cpa.common.interfaces.WrapperPrecision;
import cpa.transferrelationmonitor.TransferRelationMonitorCPA;
import exceptions.CPAException;

public class SymbPredAbsRefiner extends AbstractARTBasedRefiner {

  private final SymbPredAbsFormulaManager formulaManager;
  
  public SymbPredAbsRefiner(final ConfigurableProgramAnalysis pCpa) throws CPAException {
    super(pCpa);

    ConfigurableProgramAnalysis cpa = this.getArtCpa().getWrappedCPA();
    
    SymbPredAbsCPA symbPredAbsCpa = null;
    if (cpa instanceof SymbPredAbsCPA) {
      symbPredAbsCpa = (SymbPredAbsCPA)cpa;
    
    } else {
      if (cpa instanceof CompositeCPA) {
        for (ConfigurableProgramAnalysis compCPA : ((CompositeCPA)cpa).getComponentCPAs()) {
          if (compCPA instanceof SymbPredAbsCPA) {
            symbPredAbsCpa = (SymbPredAbsCPA)compCPA;
            break;
          }
          else if (compCPA instanceof TransferRelationMonitorCPA){
            // TODO we assume that only one CPA is monitored
            ConfigurableProgramAnalysis cCpa = ((TransferRelationMonitorCPA)compCPA).getWrappedCPAs().iterator().next();
            if(cCpa instanceof SymbPredAbsCPA){
              symbPredAbsCpa = (SymbPredAbsCPA)cCpa;
              break;
            }
          }
        }
      }
      if (symbPredAbsCpa == null) {
        throw new CPAException(getClass().getSimpleName() + " needs a SymbPredAbsCPA");
      }
    }

    formulaManager = symbPredAbsCpa.getFormulaManager();
  }

  @Override
  public boolean performRefinement(ARTReachedSet pReached, Path pPath) throws CPAException {

    CPAchecker.logger.log(Level.FINEST, "Starting refinement for SymbPredAbsCPA");
    
    // create path with all abstraction location elements (excluding the initial
    // element, which is not in pPath)
    // the last element is the element corresponding to the error location
    // (which is twice in pPath)
    ArrayList<SymbPredAbsAbstractElement> path = new ArrayList<SymbPredAbsAbstractElement>();
    SymbPredAbsAbstractElement lastElement = null;
    for (Pair<ARTElement,CFAEdge> artPair : pPath) {
      SymbPredAbsAbstractElement symbElement = 
        artPair.getFirst().retrieveWrappedElement(SymbPredAbsAbstractElement.class);
      
      if (symbElement.isAbstractionNode() && symbElement != lastElement) {
        path.add(symbElement);
      }
      lastElement = symbElement;
    }
    
    Precision oldPrecision = pReached.getPrecision(pReached.getLastElement());
    SymbPredAbsPrecision oldSymbPredAbsPrecision = null;
    if (oldPrecision instanceof SymbPredAbsPrecision) {
      oldSymbPredAbsPrecision = (SymbPredAbsPrecision)oldPrecision;
    } else if (oldPrecision instanceof WrapperPrecision) {
      oldSymbPredAbsPrecision = ((WrapperPrecision)oldPrecision).retrieveWrappedPrecision(SymbPredAbsPrecision.class);
    }
    if (oldSymbPredAbsPrecision == null) {
      throw new IllegalStateException("Could not find the SymbPredAbsPrecision for the error element");
    }
    
    CPAchecker.logger.log(Level.ALL, "Abstraction trace is", path);
        
    // build the counterexample
    CounterexampleTraceInfo info = formulaManager.buildCounterexampleTrace(path);
        
    // if error is spurious refine
    if (info.isSpurious()) {
      CPAchecker.logger.log(Level.FINEST, "Error trace is spurious, refining the abstraction");
      Pair<ARTElement, SymbPredAbsPrecision> refinementResult = 
              performRefinement(oldSymbPredAbsPrecision, path, pPath, info);
      
      Precision newPrecision = refinementResult.getSecond();
      if (oldPrecision instanceof WrapperPrecision) {
        newPrecision = ((WrapperPrecision)oldPrecision).replaceWrappedPrecision(newPrecision);
      }
      assert newPrecision != null;
      
      pReached.removeSubtree(refinementResult.getFirst(), newPrecision);
      return true;
    } else {
      // we have a real error
      CPAchecker.logger.log(Level.FINEST, "Error trace is not spurious");
      return false;
    }
  }

  private Pair<ARTElement, SymbPredAbsPrecision> performRefinement(SymbPredAbsPrecision oldPrecision,
      ArrayList<SymbPredAbsAbstractElement> pPath, Path pArtPath, CounterexampleTraceInfo pInfo) throws CPAException {

    Multimap<CFANode, Predicate> oldPredicateMap = oldPrecision.getPredicateMap();
    SymbPredAbsAbstractElement symbPredRootElement = null;
    SymbPredAbsAbstractElement firstInterpolationElement = null;
    
    ImmutableSetMultimap.Builder<CFANode, Predicate> pmapBuilder = ImmutableSetMultimap.builder();

    pmapBuilder.putAll(oldPredicateMap);
    
    for (SymbPredAbsAbstractElement e : pPath) {
      Collection<Predicate> newpreds = pInfo.getPredicatesForRefinement(e);
      CFANode loc = e.getAbstractionLocation();
      if (firstInterpolationElement == null && newpreds.size() > 0) {
        firstInterpolationElement = e;
      }
      if ((symbPredRootElement == null) && !oldPredicateMap.get(loc).containsAll(newpreds)) {
        // new predicates for this location
        symbPredRootElement = e;
      }
      pmapBuilder.putAll(loc, newpreds);
    }
    assert(firstInterpolationElement != null);

    ImmutableSetMultimap<CFANode, Predicate> newPredicateMap = pmapBuilder.build();
    SymbPredAbsPrecision newPrecision = new SymbPredAbsPrecision(newPredicateMap); 
    
    CPAchecker.logger.log(Level.ALL, "Predicate map now is", newPredicateMap);

    // symbPredRootElement might be null here, but firstInterpolationElement
    // might be not. TODO investigate why this happens
    // We have two different strategies for the refinement root: set it to
    // the firstInterpolationElement or set it to highest location in the ART
    // where the same CFANode appears.
    // Both work, so this is a heuristics question to get the best performance.
    // My benchmark showed, that at least for the benchmarks-lbe examples it is
    // best to use strategy one iff symbPredRootElement is not null.
    
    ARTElement root;
    if (symbPredRootElement != null) {
      CPAchecker.logger.log(Level.FINEST, "Found spurious counterexample,",
          "trying strategy 1: remove everything below", firstInterpolationElement, "from ART.");
      
      root = findARTElementof(firstInterpolationElement, pArtPath.getLast());
      
    } else {
      CFANode loc = firstInterpolationElement.getAbstractionLocation(); 

      CPAchecker.logger.log(Level.FINEST, "Found spurious counterexample,",
          "trying strategy 2: remove everything below node", loc, "from ART.");

      root = this.getArtCpa().findHighest(pArtPath.getLast().getFirst(), loc);
    }
    return new Pair<ARTElement, SymbPredAbsPrecision>(root, newPrecision);
  }

  private ARTElement findARTElementof(SymbPredAbsAbstractElement pSymbPredRootElement,
      Pair<ARTElement, CFAEdge> pLastElement) throws CPAException {

    Deque<ARTElement> workList = new ArrayDeque<ARTElement>();
    Set<ARTElement> handled = new HashSet<ARTElement>();

    // get the error element
    workList.add(pLastElement.getFirst());

    // go backwards
    while (!workList.isEmpty()) {
      ARTElement currentElement = workList.removeFirst();
      if (!handled.add(currentElement)) {
        // currentElement was already handled
        continue;
      }
      
      SymbPredAbsAbstractElement currentSymbPredElement = 
                currentElement.retrieveWrappedElement(SymbPredAbsAbstractElement.class);
      if (currentSymbPredElement == pSymbPredRootElement){
        return currentElement;
      }
      workList.addAll(currentElement.getParents());
    }

    throw new CPAException("Inconsistent ART");
  }
}
