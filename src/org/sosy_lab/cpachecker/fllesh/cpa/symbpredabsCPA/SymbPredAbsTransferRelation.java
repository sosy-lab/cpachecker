/*
 *  CPAchecker is a tool for configurable software verification.
 *  This file is part of CPAchecker.
 *
 *  Copyright (C) 2007-2010  Dirk Beyer
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
package org.sosy_lab.cpachecker.fllesh.cpa.symbpredabsCPA;

import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;

import org.sosy_lab.common.LogManager;
import org.sosy_lab.common.configuration.InvalidConfigurationException;
import org.sosy_lab.common.configuration.Option;
import org.sosy_lab.common.configuration.Options;
import org.sosy_lab.cpachecker.cfa.objectmodel.CFAEdge;
import org.sosy_lab.cpachecker.cfa.objectmodel.CFANode;
import org.sosy_lab.cpachecker.core.interfaces.AbstractElement;
import org.sosy_lab.cpachecker.core.interfaces.Precision;
import org.sosy_lab.cpachecker.core.interfaces.Targetable;
import org.sosy_lab.cpachecker.core.interfaces.TransferRelation;
import org.sosy_lab.cpachecker.exceptions.CPATransferException;
import org.sosy_lab.cpachecker.exceptions.UnrecognizedCFAEdgeException;
import org.sosy_lab.cpachecker.fllesh.cfa.FlleShAssumeEdge;
import org.sosy_lab.cpachecker.fllesh.cpa.assume.ConstrainedAssumeElement;
import org.sosy_lab.cpachecker.fllesh.cpa.guardededgeautomaton.GuardedEdgeAutomatonElement;
import org.sosy_lab.cpachecker.fllesh.cpa.guardededgeautomaton.GuardedEdgeAutomatonPredicateElement;
import org.sosy_lab.cpachecker.fllesh.ecp.ECPPredicate;
import org.sosy_lab.cpachecker.fllesh.fql2.translators.cfa.ToFlleShAssumeEdgeTranslator;
import org.sosy_lab.cpachecker.fllesh.cpa.symbpredabsCPA.util.symbpredabstraction.PathFormula;
import org.sosy_lab.cpachecker.fllesh.cpa.symbpredabsCPA.util.symbpredabstraction.interfaces.AbstractFormula;
import org.sosy_lab.cpachecker.fllesh.cpa.symbpredabsCPA.util.symbpredabstraction.interfaces.AbstractFormulaManager;
import org.sosy_lab.cpachecker.fllesh.cpa.symbpredabsCPA.util.symbpredabstraction.interfaces.Predicate;
import org.sosy_lab.cpachecker.fllesh.cpa.symbpredabsCPA.util.symbpredabstraction.interfaces.PredicateMap;

/**
 * Transfer relation for symbolic predicate abstraction. It makes a case
 * split to compute the abstract state. If the new abstract state is
 * computed for an abstraction location we compute the abstraction and
 * on the given set of predicates, otherwise we just update the path formula
 * and do not compute the abstraction.
 *
 * @author Alberto Griggio <alberto.griggio@disi.unitn.it> and Erkan
 */
@Options(prefix="cpas.symbpredabs")
public class SymbPredAbsTransferRelation implements TransferRelation {

  @Option(name="blk.threshold")
  private int absBlockSize = 0;

  @Option(name="satCheck")
  private int satCheckBlockSize = 0;

  // statistics
  public long abstractionTime = 0;
  public long nonAbstractionTime = 0;
  public long pathFormulaTime = 0;
  public long pathFormulaComputationTime = 0;
  public long initAbstractionFormulaTime = 0;
  public long computingAbstractionTime = 0;

  public int numAbstractions = 0;
  public int numSatChecks = 0;
  public int maxBlockSize = 0;
  public int maxPredsPerAbstraction = 0;

  private final LogManager logger;

  // formula managers
  private final AbstractFormulaManager abstractFormulaManager;
  private final SymbPredAbsFormulaManager formulaManager;
  
  // path formula cache
  private HashMap<PathFormula, Map<CFAEdge, PathFormula>> mCache;
  
  public SymbPredAbsTransferRelation(SymbPredAbsCPA pCpa) throws InvalidConfigurationException {
    pCpa.getConfiguration().inject(this);

    logger = pCpa.getLogger();
    abstractFormulaManager = pCpa.getAbstractFormulaManager();
    formulaManager = pCpa.getFormulaManager();
    
    mCache = new HashMap<PathFormula, Map<CFAEdge, PathFormula>>();
}

  @Override
  public Collection<? extends AbstractElement> getAbstractSuccessors(AbstractElement pElement,
      Precision pPrecision, CFAEdge edge) throws CPATransferException {

    long time = System.currentTimeMillis();
    SymbPredAbsAbstractElement element = (SymbPredAbsAbstractElement) pElement;
    SymbPredAbsPrecision precision = (SymbPredAbsPrecision) pPrecision;
  
    boolean thresholdReached = (absBlockSize > 0) && (element.getSizeSinceAbstraction() >= absBlockSize-1);
    boolean abstractionLocation = precision.isAbstractionLocation(edge.getSuccessor(), thresholdReached);

    boolean satCheck = (satCheckBlockSize > 0) && (element.getSizeSinceAbstraction() >= satCheckBlockSize-1);
    
    try {
      if (abstractionLocation) {
        Collection<? extends AbstractElement> lSuccessors = handleAbstractionLocation(element, precision, edge);
        
        //System.out.println(lSuccessors);
        
        return lSuccessors;
        
        //return handleAbstractionLocation(element, precision, edge);
      } else {
        Collection<? extends AbstractElement> lSuccessors = handleNonAbstractionLocation(element, edge, satCheck);
        
        //System.out.println(lSuccessors);
        
        return lSuccessors;
        
        //return handleNonAbstractionLocation(element, edge, satCheck);
      }

    } finally {
      time = System.currentTimeMillis() - time;
      if (abstractionLocation) {
        abstractionTime += time;
      } else {
        nonAbstractionTime += time;
      }
    }

  }

  /**
   * Computes element -(op)-> newElement where edge = (l1 -(op)-> l2) and l2
   * is not an abstraction location.
   * Only pfParents and pathFormula are updated and set as returned element's
   * instances in this method, all other values for the previous abstract
   * element is copied.
   *
   * @param pElement is the last abstract element
   * @param edge edge of the operation
   * @return computed abstract element
   * @throws UnrecognizedCFAEdgeException if edge is not recognized
   */
  
  //HashMap<CFAEdge, Map<PathFormula, PathFormula>> mCache = new HashMap<CFAEdge, Map<PathFormula, PathFormula>>();
  
  private Collection<SymbPredAbsAbstractElement> handleNonAbstractionLocation(
                SymbPredAbsAbstractElement element, CFAEdge pCFAEdge, boolean satCheck)
                throws CPATransferException {
    logger.log(Level.FINEST, "Handling non-abstraction location",
        (satCheck ? "with satisfiability check" : ""));
    /*
    Map<PathFormula, PathFormula> lLocalCache = mCache.get(pCFAEdge);
    
    if (lLocalCache == null) {
      lLocalCache = new HashMap<PathFormula, PathFormula>();
      mCache.put(pCFAEdge, lLocalCache);
    }
    */
    PathFormula lCurrentPathFormula = element.getPathFormula();
    PathFormula lSuccessorPathFormula;
    /*PathFormula lSuccessorPathFormula = lLocalCache.get(lCurrentPathFormula);
    
    if (lSuccessorPathFormula == null) {*/
      // id of parent
      lSuccessorPathFormula = convertEdgeToPathFormula(lCurrentPathFormula, pCFAEdge);

      logger.log(Level.ALL, "New path formula is", lSuccessorPathFormula);

      if (satCheck) {
        numSatChecks++;
        if (formulaManager.unsat(element.getAbstraction(), lSuccessorPathFormula)) {
          logger.log(Level.FINEST, "Abstraction & PathFormula is unsatisfiable.");
          //return Collections.emptySet();
          
          throw new RuntimeException();
        }
        
        throw new RuntimeException();
      }
      
      /*lLocalCache.put(lCurrentPathFormula, lSuccessorPathFormula);
      //System.out.println(lLocalCache.size());
    }
    else {
      PathFormula lSuccessorPathFormula2 = convertEdgeToPathFormula(lCurrentPathFormula, pCFAEdge);
      
      if (!lSuccessorPathFormula.equals(lSuccessorPathFormula2)) {
        System.out.println(lSuccessorPathFormula);
        System.out.println("---");
        System.out.println(lSuccessorPathFormula2);
        */
        /**
         * Das Problem ist wie folgt: Durch das Caching werden die Indizes 
         * der Nondet-Variablen nicht hochgezaehlt und damit laufen wir in 
         * eine endlosschleife (wegen dem Beispiel).
         * STIMMT DAS? MUESSTEN WIR DANN NICHT IRGENDWANN EINEN FIXPUNKT ERREICHEN?
         */
        /*
        throw new RuntimeException();
      }
    }*/
    
    // create the new abstract element for non-abstraction location
    return Collections.singleton(new SymbPredAbsAbstractElement(
        // set 'abstractionLocation' to last element's abstractionLocation since they are same
        // set 'pathFormula' to pf - the updated pathFormula -
        element.getAbstractionLocation(), lSuccessorPathFormula,
        // set 'initAbstractionFormula', 'abstraction' and 'abstractionId' to last element's values, they don't change
        element.getInitAbstractionFormula(), element.getAbstraction(),
        element.getAbstractionId(),
        // set 'sizeSinceAbstraction' to last element's value plus one for the current edge
        element.getSizeSinceAbstraction() + 1));
  }

  /**
   * Computes element -(op)-> newElement where edge = (l1 -(op)-> l2) and l2
   * is an abstraction location.
   * We set newElement's 'abstractionLocation' to edge.successor(),
   * its newElement's 'pathFormula' to true, its 'initAbstractionFormula' to
   * the 'pathFormula' of element, its 'abstraction' to newly computed abstraction
   * over predicates we get from {@link PredicateMap#getRelevantPredicates(CFANode newElement)},
   * its 'abstractionPathList' to edge.successor() concatenated to element's 'abstractionPathList',
   * and its 'artParent' to element.
   *
   * @param pElement is the last abstract element
   * @param edge edge of the operation
   * @return computed abstract element
   * @throws UnrecognizedCFAEdgeException if edge is not recognized
   */
  private Collection<SymbPredAbsAbstractElement> handleAbstractionLocation(SymbPredAbsAbstractElement element, SymbPredAbsPrecision precision, CFAEdge edge)
  throws CPATransferException {

    logger.log(Level.FINEST, "Computing abstraction on node", edge.getSuccessor());

    // compute the pathFormula for the current edge
    PathFormula pathFormula = convertEdgeToPathFormula(element.getPathFormula(), edge);
    Collection<Predicate> preds = precision.getPredicates(edge.getSuccessor());

    maxBlockSize = Math.max(maxBlockSize, element.getSizeSinceAbstraction()+1);
    maxPredsPerAbstraction = Math.max(maxPredsPerAbstraction, preds.size());

    // TODO handle returning from functions
//  if (edge instanceof ReturnEdge){
//    SymbPredAbsAbstractElement previousElem = (SymbPredAbsAbstractElement)summaryEdge.extractAbstractElement("SymbPredAbsAbstractElement");
//    MathsatSymbolicFormulaManager mmgr = (MathsatSymbolicFormulaManager) symbolicFormulaManager;
//    AbstractFormula ctx = previousElem.getAbstraction();
//    MathsatSymbolicFormula fctx = (MathsatSymbolicFormula)mmgr.instantiate(abstractFormulaManager.toConcrete(mmgr, ctx), null);
//  }

    long time1 = System.currentTimeMillis();

    // compute new abstraction
    AbstractFormula newAbstraction = formulaManager.buildAbstraction(
        element.getAbstraction(), pathFormula, preds);

    long time2 = System.currentTimeMillis();
    computingAbstractionTime += time2 - time1;

    // if the abstraction is false, return bottom (represented by empty set)
    if (abstractFormulaManager.isFalse(newAbstraction)) {
      logger.log(Level.FINEST, "Abstraction is false, node is not reachable");
      return Collections.emptySet();
    }

    // create new empty path formula
    PathFormula newPathFormula = formulaManager.makeEmptyPathFormula();

    numAbstractions++;

    return Collections.singleton(new SymbPredAbsAbstractElement(
        // set 'abstractionLocation' to edge.getSuccessor()
        // set 'pathFormula' to newPathFormula computed above
        edge.getSuccessor(), newPathFormula,
        // set 'initAbstractionFormula' to  pathFormula computed above
        // set 'abstraction' to newly computed abstraction
        pathFormula, newAbstraction));
  }

  /**
   * Converts an edge into a formula and creates a conjunction of it with the
   * previous pathFormula.
   *
   * @param pathFormula The previous pathFormula.
   * @param edge  The edge to analyze.
   * @param abstractionNodeId The id of the last abstraction node (used for cache access).
   * @return  The new pathFormula.
   * @throws UnrecognizedCFAEdgeException
   */
  private PathFormula convertEdgeToPathFormula(PathFormula pCurrentPathFormula, CFAEdge pCFAEdge) throws CPATransferException {
    final long start = System.currentTimeMillis();
    PathFormula lSuccessorFormula = null;
    
    Map<CFAEdge, PathFormula> lLocalCache = mCache.get(pCurrentPathFormula);
    
    if (lLocalCache == null) {
      lLocalCache = new HashMap<CFAEdge, PathFormula>();
      mCache.put(pCurrentPathFormula, lLocalCache);
    }
    
    lSuccessorFormula = lLocalCache.get(pCFAEdge);
    
    if (lSuccessorFormula == null) {
      long startComp = System.currentTimeMillis();
      // compute new pathFormula with the operation on the edge
      lSuccessorFormula = formulaManager.makeAnd(pCurrentPathFormula, pCFAEdge);
      pathFormulaComputationTime += System.currentTimeMillis() - startComp;
      
      lLocalCache.put(pCFAEdge, lSuccessorFormula);
    }

    assert lSuccessorFormula != null;
    pathFormulaTime += System.currentTimeMillis() - start;    
    
    return lSuccessorFormula;
  }

  public SymbPredAbsAbstractElement strengthen(CFANode pNode, SymbPredAbsAbstractElement pElement, GuardedEdgeAutomatonElement pAutomatonElement) {
    
    if (pAutomatonElement instanceof GuardedEdgeAutomatonPredicateElement) {
      GuardedEdgeAutomatonPredicateElement lAutomatonElement = (GuardedEdgeAutomatonPredicateElement)pAutomatonElement;
      
      for (ECPPredicate lPredicate : lAutomatonElement) {
        FlleShAssumeEdge lEdge = ToFlleShAssumeEdgeTranslator.translate(pNode, lPredicate);
        
        try {
          pElement = handleNonAbstractionLocation(pElement, lEdge, false).iterator().next();
        } catch (CPATransferException e) {
          throw new RuntimeException(e);
        }
      }
    }
    
    return pElement;
  }
  
  public SymbPredAbsAbstractElement strengthen(CFANode pNode, SymbPredAbsAbstractElement pElement, ConstrainedAssumeElement pAssumeElement) {
    FlleShAssumeEdge lEdge = new FlleShAssumeEdge(pNode, pAssumeElement.getExpression());
    
    try {
      pElement = handleNonAbstractionLocation(pElement, lEdge, false).iterator().next();
    } catch (CPATransferException e) {
      throw new RuntimeException(e);
    }
    
    return pElement;
  }
  
  @Override
  public Collection<? extends AbstractElement> strengthen(AbstractElement pElement,
      List<AbstractElement> otherElements, CFAEdge edge, Precision pPrecision) throws UnrecognizedCFAEdgeException {
    // do abstraction (including reachability check) if an error was found by another CPA

    SymbPredAbsAbstractElement element = (SymbPredAbsAbstractElement)pElement;
    
    for (AbstractElement lElement : otherElements) {
      if (lElement instanceof GuardedEdgeAutomatonElement) {
        element = strengthen(edge.getSuccessor(), element, (GuardedEdgeAutomatonElement)lElement);
      }
      
      if (lElement instanceof ConstrainedAssumeElement) {
        element = strengthen(edge.getSuccessor(), element, (ConstrainedAssumeElement)lElement);
      }
    }
    
    if (element.isAbstractionNode()) {
      // TODO satisfiability check?
      if (element != pElement) {
        return Collections.singleton(element);
      }
      
      // not necessary to do satisfiability check
      return null;
    }

    boolean errorFound = false;
    for (AbstractElement e : otherElements) {
      if ((e instanceof Targetable) && ((Targetable)e).isTarget()) {
        errorFound = true;
        break;
      }
    }

    if (errorFound) {
      logger.log(Level.FINEST, "Checking for feasibility of path because error has been found");
      numSatChecks++;

      if (formulaManager.unsat(element.getAbstraction(), element.getPathFormula())) {
        logger.log(Level.FINEST, "Path is infeasible.");
        return Collections.emptySet();
      } else {
        // although this is not an abstraction location, we fake an abstraction
        // because refinement code expect it to be like this
        logger.log(Level.FINEST, "Last part of the path is not infeasible.");

        maxBlockSize = Math.max(maxBlockSize, element.getSizeSinceAbstraction());

        return Collections.singleton(new SymbPredAbsAbstractElement(
            // set 'abstractionLocation' to edge.getSuccessor()
            // set 'pathFormula' to true
            edge.getSuccessor(), formulaManager.makeEmptyPathFormula(),
            // set 'initAbstractionFormula' to old pathFormula
            // set 'abstraction' to true (we don't know better)
            element.getPathFormula(), abstractFormulaManager.makeTrue()));
      }
    } else {
      if (element != pElement) {
        return Collections.singleton(element);
      }
      
      return null;
    }
  }
}
