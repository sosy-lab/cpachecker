/*
 *  CPAchecker is a tool for configurable software verification.
 *  This file is part of CPAchecker.
 *
 *  Copyright (C) 2007-2012  Dirk Beyer
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
import org.sosy_lab.common.Timer;
import org.sosy_lab.common.configuration.InvalidConfigurationException;
import org.sosy_lab.common.configuration.Option;
import org.sosy_lab.common.configuration.Options;
import org.sosy_lab.cpachecker.cfa.objectmodel.CFAEdge;
import org.sosy_lab.cpachecker.cfa.objectmodel.CFANode;
import org.sosy_lab.cpachecker.cfa.objectmodel.c.AssumeEdge;
import org.sosy_lab.cpachecker.core.interfaces.AbstractState;
import org.sosy_lab.cpachecker.core.interfaces.Precision;
import org.sosy_lab.cpachecker.core.interfaces.TransferRelation;
import org.sosy_lab.cpachecker.cpa.assume.ConstrainedAssumeElement;
import org.sosy_lab.cpachecker.cpa.assumptions.storage.AssumptionStorageElement;
import org.sosy_lab.cpachecker.cpa.predicate.PredicateAbstractElement.ComputeAbstractionElement;
import org.sosy_lab.cpachecker.exceptions.CPATransferException;
import org.sosy_lab.cpachecker.exceptions.UnrecognizedCFAEdgeException;
import org.sosy_lab.cpachecker.util.AbstractStates;
import org.sosy_lab.cpachecker.util.predicates.AbstractionFormula;
import org.sosy_lab.cpachecker.util.predicates.PathFormula;
import org.sosy_lab.cpachecker.util.predicates.interfaces.Formula;
import org.sosy_lab.cpachecker.util.predicates.interfaces.PathFormulaManager;

/**
 * Transfer relation for symbolic predicate abstraction. First it computes
 * the strongest post for the given CFA edge. Afterwards it optionally
 * computes an abstraction.
 */
@Options(prefix="cpa.predicate")
public class PredicateTransferRelation implements TransferRelation {

  @Option(name="satCheck",
      description="maximum blocksize before a satisfiability check is done\n"
        + "(non-negative number, 0 means never, if positive should be smaller than blocksize)")
  private int satCheckBlockSize = 0;

  @Option(description="check satisfiability when a target state has been found (should be true)")
  private boolean targetStateSatCheck = true;

  // statistics
  final Timer postTimer = new Timer();
  final Timer satCheckTimer = new Timer();
  final Timer pathFormulaTimer = new Timer();
  final Timer strengthenTimer = new Timer();
  final Timer strengthenCheckTimer = new Timer();
  final Timer abstractionCheckTimer = new Timer();
  final Timer pathFormulaCheckTimer = new Timer();

  int numSatChecksFalse = 0;
  int numStrengthenChecksFalse = 0;

  private final LogManager logger;
  private final PredicateAbstractionManager formulaManager;
  private final PathFormulaManager pathFormulaManager;

  private final BlockOperator blk;

  private final Map<PredicateAbstractElement, PathFormula> computedPathFormulae = new HashMap<PredicateAbstractElement, PathFormula>();

  public PredicateTransferRelation(PredicateCPA pCpa, BlockOperator pBlk) throws InvalidConfigurationException {
    pCpa.getConfiguration().inject(this, PredicateTransferRelation.class);

    logger = pCpa.getLogger();
    formulaManager = pCpa.getPredicateManager();
    pathFormulaManager = pCpa.getPathFormulaManager();
    blk = pBlk;
  }

  @Override
  public Collection<? extends AbstractState> getAbstractSuccessors(AbstractState pElement,
      Precision pPrecision, CFAEdge edge) throws CPATransferException, InterruptedException {

    postTimer.start();
    try {

      PredicateAbstractElement element = (PredicateAbstractElement) pElement;
      CFANode loc = edge.getSuccessor();

      // Check whether abstraction is false.
      // Such elements might get created when precision adjustment computes an abstraction.
      if (element.getAbstractionFormula().isFalse()) {
        return Collections.emptySet();
      }

      // calculate strongest post
      PathFormula pathFormula = convertEdgeToPathFormula(element.getPathFormula(), edge);
      logger.log(Level.ALL, "New path formula is", pathFormula);

      // check whether to do abstraction
      boolean doAbstraction = blk.isBlockEnd(edge, pathFormula);

      if (doAbstraction) {
        return Collections.singleton(
            new PredicateAbstractElement.ComputeAbstractionElement(
                pathFormula, element.getAbstractionFormula(), loc));
      } else {
        return handleNonAbstractionFormulaLocation(pathFormula, element.getAbstractionFormula());
      }
    } finally {
      postTimer.stop();
    }
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
        PredicateAbstractElement.nonAbstractionElement(pathFormula, abstractionFormula));
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
  public PathFormula convertEdgeToPathFormula(PathFormula pathFormula, CFAEdge edge) throws CPATransferException {
    pathFormulaTimer.start();
    try {
      // compute new pathFormula with the operation on the edge
      return  pathFormulaManager.makeAnd(pathFormula, edge);
    } finally {
      pathFormulaTimer.stop();
    }
  }

  @Override
  public Collection<? extends AbstractState> strengthen(AbstractState pElement,
      List<AbstractState> otherElements, CFAEdge edge, Precision pPrecision) throws CPATransferException {

    strengthenTimer.start();
    try {

      PredicateAbstractElement element = (PredicateAbstractElement)pElement;
      if (element.isAbstractionElement()) {
        // can't do anything with this object because the path formula of
        // abstraction elements has to stay "true"
        return Collections.singleton(element);
      }

      boolean errorFound = false;
      for (AbstractState lElement : otherElements) {
        if (lElement instanceof AssumptionStorageElement) {
          element = strengthen(element, (AssumptionStorageElement)lElement);
        }

        if (lElement instanceof ConstrainedAssumeElement) {
          element = strengthen(edge.getSuccessor(), element, (ConstrainedAssumeElement)lElement);
        }

        if (AbstractStates.isTargetElement(lElement)) {
          errorFound = true;
        }
      }

      // check satisfiability in case of error
      // (not necessary for abstraction elements)
      if (errorFound && targetStateSatCheck) {
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

  private PredicateAbstractElement strengthen(CFANode pNode, PredicateAbstractElement pElement, ConstrainedAssumeElement pAssumeElement) throws CPATransferException {
    AssumeEdge lEdge = new AssumeEdge(pAssumeElement.getExpression().toASTString(), pNode.getLineNumber(), pNode, pNode, pAssumeElement.getExpression(), true);

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

    PathFormula pf = pathFormulaManager.makeAnd(pElement.getPathFormula(), asmpt);

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
      assert !oldElement.isAbstractionElement();
      return PredicateAbstractElement.nonAbstractionElement(newPathFormula, oldElement.getAbstractionFormula());
    }
  }

  protected PredicateAbstractElement strengthenSatCheck(PredicateAbstractElement pElement) {
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

      PathFormula newPathFormula = pathFormulaManager.makeEmptyPathFormula(pathFormula);

      return PredicateAbstractElement.abstractionElement(newPathFormula, abs);
    }
  }

  boolean areAbstractSuccessors(AbstractState pElement, CFAEdge pCfaEdge, Collection<? extends AbstractState> pSuccessors) throws CPATransferException, InterruptedException {
    PredicateAbstractElement predicateElement = (PredicateAbstractElement)pElement;
    PathFormula pathFormula = computedPathFormulae.get(predicateElement);
    if (pathFormula == null) {
      pathFormula = pathFormulaManager.makeEmptyPathFormula(predicateElement.getPathFormula());
    }
    boolean result = true;

    if (pSuccessors.isEmpty()) {
      satCheckTimer.start();
      PathFormula pFormula = convertEdgeToPathFormula(pathFormula, pCfaEdge);
      Collection<? extends AbstractState> foundSuccessors = handleNonAbstractionFormulaLocation(pFormula, predicateElement.getAbstractionFormula());
      //if we found successors, they all have to be unsat
      for(AbstractState e : foundSuccessors) {
        PredicateAbstractElement successor = (PredicateAbstractElement)e;
        if(!formulaManager.unsat(successor.getAbstractionFormula(), successor.getPathFormula())) {
          result = false;
        }
      }
      satCheckTimer.stop();
      return result;
    }

    for(AbstractState e : pSuccessors) {
      PredicateAbstractElement successor = (PredicateAbstractElement)e;

      if(successor.isAbstractionElement()) {
        // check abstraction
        abstractionCheckTimer.start();
        if(!formulaManager.checkCoverage(predicateElement.getAbstractionFormula(), pathFormula, successor.getAbstractionFormula())) {
          result = false;
          System.out.println(predicateElement.getAbstractionFormula() + "\n----\n" + pathFormula + "\n----\n" + successor.getAbstractionFormula());
        }
        abstractionCheckTimer.stop();
      }
      else {
        // check abstraction
        abstractionCheckTimer.start();
        if(!successor.getAbstractionFormula().equals(predicateElement.getAbstractionFormula())) {
          result = false;
        }
        abstractionCheckTimer.stop();

        // compute path formula
        PathFormula computedPathFormula = convertEdgeToPathFormula(pathFormula, pCfaEdge);
        computedPathFormulae.put(successor, computedPathFormula);
      }
    }

    return result;
  }
}
