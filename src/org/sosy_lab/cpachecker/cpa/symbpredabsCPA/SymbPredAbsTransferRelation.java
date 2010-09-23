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
package org.sosy_lab.cpachecker.cpa.symbpredabsCPA;

import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;

import org.sosy_lab.common.LogManager;
import org.sosy_lab.common.Pair;
import org.sosy_lab.common.configuration.InvalidConfigurationException;
import org.sosy_lab.common.configuration.Option;
import org.sosy_lab.common.configuration.Options;
import org.sosy_lab.cpachecker.cfa.objectmodel.CFAEdge;
import org.sosy_lab.cpachecker.cfa.objectmodel.CFAFunctionDefinitionNode;
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
import org.sosy_lab.cpachecker.util.symbpredabstraction.Abstraction;
import org.sosy_lab.cpachecker.util.symbpredabstraction.PathFormula;
import org.sosy_lab.cpachecker.util.symbpredabstraction.interfaces.Predicate;
import org.sosy_lab.cpachecker.util.symbpredabstraction.interfaces.PredicateMap;

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

  @Option(name="blk.functions")
  private boolean absOnFunction = true;

  @Option(name="blk.loops")
  private boolean absOnLoop = true;

  @Option(name="blk.requireThresholdAndLBE")
  private boolean absOnlyIfBoth = false;
  
  @Option(name="blk.useCache")
  private boolean useCache = true;
  
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
  private final SymbPredAbsFormulaManager formulaManager;

  // map from a node to path formula
  // used to not compute the formula again
  // the first integer in the key is parent element's node id
  // the second integer is current element's node id
  // the third is the sucessor element's node id
  private final Map<Pair<PathFormula, CFAEdge>, PathFormula> pathFormulaMapHash =
    new HashMap<Pair<PathFormula, CFAEdge>, PathFormula>();

  public SymbPredAbsTransferRelation(SymbPredAbsCPA pCpa) throws InvalidConfigurationException {
    pCpa.getConfiguration().inject(this);

    logger = pCpa.getLogger();
    formulaManager = pCpa.getFormulaManager();
}

  @Override
  public Collection<? extends AbstractElement> getAbstractSuccessors(AbstractElement pElement,
      Precision pPrecision, CFAEdge edge) throws CPATransferException {

    long time = System.currentTimeMillis();
    SymbPredAbsAbstractElement element = (SymbPredAbsAbstractElement) pElement;
    SymbPredAbsPrecision precision = (SymbPredAbsPrecision) pPrecision;
  
    int sizeSinceAbstraction = element.getPathFormula().getLength();
    boolean thresholdReached = (absBlockSize > 0) && (sizeSinceAbstraction >= absBlockSize-1);
    boolean abstractionLocation = isAbstractionLocation(edge.getSuccessor(), thresholdReached);

    boolean satCheck = (satCheckBlockSize > 0) && (sizeSinceAbstraction >= satCheckBlockSize-1);
    
    try {
      if (abstractionLocation) {
        return handleAbstractionLocation(element, precision, edge);
      } else {
        return handleNonAbstractionLocation(element, edge, satCheck);
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
  private Collection<SymbPredAbsAbstractElement> handleNonAbstractionLocation(
                SymbPredAbsAbstractElement element, CFAEdge edge, boolean satCheck)
                throws CPATransferException {
    logger.log(Level.FINEST, "Handling non-abstraction location",
        (satCheck ? "with satisfiability check" : ""));

    PathFormula pf = convertEdgeToPathFormula(element.getPathFormula(), edge);

    logger.log(Level.ALL, "New path formula is", pf);

    if (satCheck) {
      numSatChecks++;
      if (formulaManager.unsat(element.getAbstraction(), pf)) {
        logger.log(Level.FINEST, "Abstraction & PathFormula is unsatisfiable.");
        return Collections.emptySet();
      }
    }

    // create the new abstract element for non-abstraction location
    return Collections.singleton(
        new SymbPredAbsAbstractElement(false, pf, element.getAbstraction()));
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

    maxBlockSize = Math.max(maxBlockSize, pathFormula.getLength());
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
    Abstraction newAbstraction = formulaManager.buildAbstraction(
        element.getAbstraction(), pathFormula, preds);

    long time2 = System.currentTimeMillis();
    computingAbstractionTime += time2 - time1;

    // if the abstraction is false, return bottom (represented by empty set)
    if (newAbstraction.isFalse()) {
      logger.log(Level.FINEST, "Abstraction is false, node is not reachable");
      return Collections.emptySet();
    }

    // create new empty path formula
    PathFormula newPathFormula = formulaManager.makeEmptyPathFormula(pathFormula);

    numAbstractions++;

    return Collections.singleton(
        new SymbPredAbsAbstractElement(true, newPathFormula, newAbstraction));
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
  private PathFormula convertEdgeToPathFormula(PathFormula pathFormula, CFAEdge edge) throws CPATransferException {
    final long start = System.currentTimeMillis();
    PathFormula pf;

    if (!useCache || !absOnFunction || !absOnLoop || absBlockSize > 0) {
      long startComp = System.currentTimeMillis();
      // compute new pathFormula with the operation on the edge
      pf = formulaManager.makeAnd(pathFormula, edge);
      pathFormulaComputationTime += System.currentTimeMillis() - startComp;

    } else {
      final Pair<PathFormula, CFAEdge> formulaCacheKey =
        new Pair<PathFormula, CFAEdge>(pathFormula, edge);
      pf = pathFormulaMapHash.get(formulaCacheKey);
      if (pf == null) {
        long startComp = System.currentTimeMillis();
        // compute new pathFormula with the operation on the edge
        pf = formulaManager.makeAnd(pathFormula, edge);
        pathFormulaComputationTime += System.currentTimeMillis() - startComp;
        pathFormulaMapHash.put(formulaCacheKey, pf);
      }
    }
    assert pf != null;
    pathFormulaTime += System.currentTimeMillis() - start;
    return pf;
  }

  /**
   * @param succLoc successor CFA location.
   * @param thresholdReached if the maximum block size has been reached
   * @return true if succLoc is an abstraction location. For now a location is 
   * an abstraction location if it has an incoming loop-back edge, if it is
   * the start node of a function or if it is the call site from a function call.
   */
  private boolean isAbstractionLocation(CFANode succLoc, boolean thresholdReached) {
    boolean result = false;
    
    if (absOnLoop) {
      result = succLoc.isLoopStart();
    }
    if (absOnFunction) {
      result = result
            || (succLoc instanceof CFAFunctionDefinitionNode) // function call edge
            || (succLoc.getEnteringSummaryEdge() != null); // function return edge
    }
    
    if (absOnlyIfBoth) {
      result = result && thresholdReached;
    } else {
      result = result || thresholdReached;
    }

    return result;
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
      PathFormula pathFormula = element.getPathFormula();

      if (formulaManager.unsat(element.getAbstraction(), pathFormula)) {
        logger.log(Level.FINEST, "Path is infeasible.");
        return Collections.emptySet();
      } else {
        // although this is not an abstraction location, we fake an abstraction
        // because refinement code expect it to be like this
        logger.log(Level.FINEST, "Last part of the path is not infeasible.");
        
        maxBlockSize = Math.max(maxBlockSize, pathFormula.getLength());

        // set abstraction to true (we don't know better)
        Abstraction abs = formulaManager.makeTrueAbstraction(pathFormula.getSymbolicFormula());

        PathFormula newPathFormula = formulaManager.makeEmptyPathFormula(pathFormula);

        return Collections.singleton(new SymbPredAbsAbstractElement(true,
            newPathFormula, abs));
      }
    } else {
      if (element != pElement) {
        return Collections.singleton(element);
      }
      
      return null;
    }
  }
}
