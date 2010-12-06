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
package org.sosy_lab.cpachecker.cpa.predicate;

import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;

import org.sosy_lab.common.LogManager;
import org.sosy_lab.common.Pair;
import org.sosy_lab.common.Timer;
import org.sosy_lab.common.configuration.InvalidConfigurationException;
import org.sosy_lab.common.configuration.Option;
import org.sosy_lab.common.configuration.Options;
import org.sosy_lab.cpachecker.cfa.objectmodel.CFAEdge;
import org.sosy_lab.cpachecker.cfa.objectmodel.CFAFunctionDefinitionNode;
import org.sosy_lab.cpachecker.cfa.objectmodel.CFANode;
import org.sosy_lab.cpachecker.cfa.objectmodel.c.AssumeEdge;
import org.sosy_lab.cpachecker.core.interfaces.AbstractElement;
import org.sosy_lab.cpachecker.core.interfaces.Precision;
import org.sosy_lab.cpachecker.core.interfaces.Targetable;
import org.sosy_lab.cpachecker.core.interfaces.TransferRelation;
import org.sosy_lab.cpachecker.cpa.assume.ConstrainedAssumeElement;
import org.sosy_lab.cpachecker.cpa.assumptions.storage.AssumptionStorageElement;
import org.sosy_lab.cpachecker.cpa.predicate.PredicateAbstractElement.AbstractionElement;
import org.sosy_lab.cpachecker.cpa.predicate.PredicateAbstractElement.ComputeAbstractionElement;
import org.sosy_lab.cpachecker.exceptions.CPATransferException;
import org.sosy_lab.cpachecker.exceptions.UnrecognizedCFAEdgeException;
import org.sosy_lab.cpachecker.cpa.guardededgeautomaton.GuardedEdgeAutomatonPredicateElement;
import org.sosy_lab.cpachecker.cpa.guardededgeautomaton.productautomaton.ProductAutomatonElement;
import org.sosy_lab.cpachecker.util.ecp.ECPPredicate;
import org.sosy_lab.cpachecker.fshell.fql2.translators.cfa.ToFlleShAssumeEdgeTranslator;
import org.sosy_lab.cpachecker.util.predicates.AbstractionFormula;
import org.sosy_lab.cpachecker.util.predicates.PathFormula;
import org.sosy_lab.cpachecker.util.predicates.interfaces.Formula;

/**
 * Transfer relation for symbolic predicate abstraction. First it computes
 * the strongest post for the given CFA edge. Afterwards it optionally 
 * computes an abstraction.
 */
@Options(prefix="cpa.predicate")
public class PredicateTransferRelation implements TransferRelation {

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
  final Timer postTimer = new Timer();
  final Timer satCheckTimer = new Timer();
  final Timer pathFormulaTimer = new Timer();
  final Timer pathFormulaComputationTimer = new Timer();
  final Timer strengthenTimer = new Timer();
  final Timer strengthenCheckTimer = new Timer();

  int numBlkFunctions = 0;
  int numBlkLoops = 0;
  int numBlkThreshold = 0;
  int numSatChecksFalse = 0;
  int numStrengthenChecksFalse = 0;
  int pathFormulaCacheHits = 0;
  
  private final LogManager logger;
  private final PredicateAbstractionManager formulaManager;

  // pathFormula computation cache
  private final Map<Pair<PathFormula, CFAEdge>, PathFormula> pathFormulaCache;

  public PredicateTransferRelation(PredicateCPA pCpa) throws InvalidConfigurationException {
    pCpa.getConfiguration().inject(this);

    logger = pCpa.getLogger();
    formulaManager = pCpa.getPredicateManager();
    
    pathFormulaCache = useCache ? new HashMap<Pair<PathFormula, CFAEdge>, PathFormula>() : null;
  }

  @Override
  public Collection<? extends AbstractElement> getAbstractSuccessors(AbstractElement pElement,
      Precision pPrecision, CFAEdge edge) throws CPATransferException {

    postTimer.start();
    
    PredicateAbstractElement element = (PredicateAbstractElement) pElement;
    CFANode loc = edge.getSuccessor();

    // Check whether abstraction is false.
    // Such elements might get created when precision adjustment computes an abstraction.
    if (element.getAbstractionFormula().asFormula().isFalse()) {
      return Collections.emptySet();
    }
    
    // calculate strongest post
    PathFormula pathFormula = convertEdgeToPathFormula(element.getPathFormula(), edge);
    logger.log(Level.ALL, "New path formula is", pathFormula);
    
    // check whether to do abstraction
    boolean doAbstraction = isBlockEnd(loc, pathFormula);
    
    Collection<? extends AbstractElement> result;
    if (doAbstraction) {
      result = Collections.singleton(
          new PredicateAbstractElement.ComputeAbstractionElement(
              pathFormula, element.getAbstractionFormula(), loc)); 
    } else {
      result = handleNonAbstractionFormulaLocation(pathFormula, element.getAbstractionFormula());
    }
    postTimer.stop();
    return result;
  }

  /**
   * Does special things when we do not compute an abstraction for the
   * successor. This currently only envolves an optional sat check.
   */
  private Collection<PredicateAbstractElement> handleNonAbstractionFormulaLocation(
                PathFormula pathFormula, AbstractionFormula abstractionFormula) {
    boolean satCheck = (satCheckBlockSize > 0) && (pathFormula.getLength() >= satCheckBlockSize);
    
    logger.log(Level.FINEST, "Handling non-abstraction location",
        (satCheck ? "with satisfiability check" : ""));

    if (satCheck) {
      satCheckTimer.start(); 

      boolean unsat = formulaManager.unsat(abstractionFormula, pathFormula);
      
      satCheckTimer.stop();
      
      if (unsat) {
        numSatChecksFalse++;
        logger.log(Level.FINEST, "Abstraction & PathFormula is unsatisfiable.");
        return Collections.emptySet();
      }
    }

    // create the new abstract element for non-abstraction location
    return Collections.singleton(
        new PredicateAbstractElement(pathFormula, abstractionFormula));
  }

  /**
   * Converts an edge into a formula and creates a conjunction of it with the
   * previous pathFormula.
   * 
   * This method implements the strongest post operator.
   *
   * @param pathFormula The previous pathFormula.
   * @param edge  The edge to analyze.
   * @return  The new pathFormula.
   * @throws UnrecognizedCFAEdgeException
   */
  private PathFormula convertEdgeToPathFormula(PathFormula pathFormula, CFAEdge edge) throws CPATransferException {
    pathFormulaTimer.start();
    PathFormula pf;

    if (!useCache) {
      pathFormulaComputationTimer.start();
      // compute new pathFormula with the operation on the edge
      pf = formulaManager.makeAnd(pathFormula, edge);
      pathFormulaComputationTimer.stop();

    } else {
      final Pair<PathFormula, CFAEdge> formulaCacheKey = Pair.of(pathFormula, edge);
      pf = pathFormulaCache.get(formulaCacheKey);
      if (pf == null) {
        pathFormulaComputationTimer.start();
        // compute new pathFormula with the operation on the edge
        pf = formulaManager.makeAnd(pathFormula, edge);
        pathFormulaComputationTimer.stop();
        pathFormulaCache.put(formulaCacheKey, pf);
        
      } else {
        pathFormulaCacheHits++;
      }
    }
    assert pf != null;
    pathFormulaTimer.stop();
    return pf;
  }

  /**
   * Check whether an abstraction should be computed.
   * 
   * This method implements the blk operator from the paper
   * "Adjustable Block-Encoding" [Beyer/Keremoglu/Wendler FMCAD'10].
   * 
   * @param succLoc successor CFA location.
   * @param thresholdReached if the maximum block size has been reached
   * @return true if succLoc is an abstraction location. For now a location is 
   * an abstraction location if it has an incoming loop-back edge, if it is
   * the start node of a function or if it is the call site from a function call.
   */
  private boolean isBlockEnd(CFANode succLoc, PathFormula pf) {
    boolean result = false;
    
    if (absOnLoop) {
      result = succLoc.isLoopStart();
      if (result) {
        numBlkLoops++;
      }
    }
    if (absOnFunction) {
      boolean function =      
               (succLoc instanceof CFAFunctionDefinitionNode) // function call edge
            || (succLoc.getEnteringSummaryEdge() != null); // function return edge
      if (function) {
        result = true;
        numBlkFunctions++;
      }
    }
    
    if (absBlockSize > 0) {
      boolean threshold = (pf.getLength() >= absBlockSize);
      if (threshold) {
        numBlkThreshold++;
      }
      
      if (absOnlyIfBoth) {
        result = result && threshold;
      } else {
        result = result || threshold;
      }
    }

    return result;
  }
  
  @Override
  public Collection<? extends AbstractElement> strengthen(AbstractElement pElement,
      List<AbstractElement> otherElements, CFAEdge edge, Precision pPrecision) throws CPATransferException {

    strengthenTimer.start();
    try {
    
      PredicateAbstractElement element = (PredicateAbstractElement)pElement;
      boolean errorFound = false;
      for (AbstractElement lElement : otherElements) {
        if (lElement instanceof AssumptionStorageElement) {
          element = strengthen(element, (AssumptionStorageElement)lElement);
        }
        
        if (lElement instanceof GuardedEdgeAutomatonPredicateElement) {
          element = strengthen(edge.getSuccessor(), element, (GuardedEdgeAutomatonPredicateElement)lElement);
        }
        
        if (lElement instanceof ProductAutomatonElement.PredicateElement) {
          element = strengthen(edge.getSuccessor(), element, (ProductAutomatonElement.PredicateElement)lElement);
        }
        
        if (lElement instanceof ConstrainedAssumeElement) {
          element = strengthen(edge.getSuccessor(), element, (ConstrainedAssumeElement)lElement);
        }
        
        if ((lElement instanceof Targetable) && ((Targetable)lElement).isTarget()) {
          errorFound = true;
        }
      }
  
      // check satisfiability in case of error
      // (not necessary for abstraction elements)
      if (errorFound && !(element instanceof AbstractionElement)) {
        element = strengthenSatCheck(element);
        if (element == null) {
          // successor not reachable
          return Collections.emptySet();
        }
      }
  
      return Collections.singleton(element);
    
    } finally {
      strengthenTimer.stop();
    }
  }

  private PredicateAbstractElement strengthen(CFANode pNode, PredicateAbstractElement pElement, GuardedEdgeAutomatonPredicateElement pAutomatonElement) throws CPATransferException {
    PathFormula pf = pElement.getPathFormula();
    
    for (ECPPredicate lPredicate : pAutomatonElement) {
      AssumeEdge lEdge = ToFlleShAssumeEdgeTranslator.translate(pNode, lPredicate);
      
      pf = convertEdgeToPathFormula(pf, lEdge);
    }

    return replacePathFormula(pElement, pf);
  }
  
  private PredicateAbstractElement strengthen(CFANode pNode, PredicateAbstractElement pElement, ProductAutomatonElement.PredicateElement pAutomatonElement) throws CPATransferException {
    PathFormula pf = pElement.getPathFormula();
    
    for (ECPPredicate lPredicate : pAutomatonElement.getPredicates()) {
      AssumeEdge lEdge = ToFlleShAssumeEdgeTranslator.translate(pNode, lPredicate);
      
      pf = convertEdgeToPathFormula(pf, lEdge);
    }

    return replacePathFormula(pElement, pf);
  }
  
  private PredicateAbstractElement strengthen(CFANode pNode, PredicateAbstractElement pElement, ConstrainedAssumeElement pAssumeElement) throws CPATransferException {
    AssumeEdge lEdge = new AssumeEdge(pAssumeElement.getExpression().getRawSignature(), pNode.getLineNumber(), pNode, pNode, pAssumeElement.getExpression(), true);
    
    PathFormula pf = convertEdgeToPathFormula(pElement.getPathFormula(), lEdge);
    
    return replacePathFormula(pElement, pf);
  }
  
  private PredicateAbstractElement strengthen(PredicateAbstractElement pElement, 
      AssumptionStorageElement pElement2) {
    
    Formula asmpt = pElement2.getAssumption();

    if (asmpt.isTrue() || asmpt.isFalse()) {
      // we don't add the assumption false in order to not forget the content of the path formula
      // (we need it for post-processing) 
      return pElement;
    }
    
    PathFormula pf = formulaManager.makeAnd(pElement.getPathFormula(), asmpt);
      
    return replacePathFormula(pElement, pf);
  }
  
  /**
   * Returns a new element with a given pathFormula. All other fields stay equal.
   */
  private PredicateAbstractElement replacePathFormula(PredicateAbstractElement oldElement, PathFormula newPathFormula) {
    if (oldElement instanceof ComputeAbstractionElement) {
      CFANode loc = ((ComputeAbstractionElement) oldElement).getLocation();
      return new ComputeAbstractionElement(newPathFormula, oldElement.getAbstractionFormula(), loc);
    } else {
      assert !(oldElement instanceof AbstractionElement);
      return new PredicateAbstractElement(newPathFormula, oldElement.getAbstractionFormula());
    }
  }
  
  private PredicateAbstractElement strengthenSatCheck(PredicateAbstractElement pElement) {
    logger.log(Level.FINEST, "Checking for feasibility of path because error has been found");

    strengthenCheckTimer.start();
    PathFormula pathFormula = pElement.getPathFormula();
    boolean unsat = formulaManager.unsat(pElement.getAbstractionFormula(), pathFormula);
    strengthenCheckTimer.stop();

    if (unsat) {
      numStrengthenChecksFalse++;
      logger.log(Level.FINEST, "Path is infeasible.");
      return null;
    } else {
      // although this is not an abstraction location, we fake an abstraction
      // because refinement code expects it to be like this
      logger.log(Level.FINEST, "Last part of the path is not infeasible.");

      // set abstraction to true (we don't know better)
      AbstractionFormula abs = formulaManager.makeTrueAbstractionFormula(pathFormula.getFormula());

      PathFormula newPathFormula = formulaManager.makeEmptyPathFormula(pathFormula);

      return new PredicateAbstractElement.AbstractionElement(newPathFormula, abs);
    }
  }
}
