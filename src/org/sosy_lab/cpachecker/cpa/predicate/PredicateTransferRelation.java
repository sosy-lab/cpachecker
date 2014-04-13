/*
 *  CPAchecker is a tool for configurable software verification.
 *  This file is part of CPAchecker.
 *
 *  Copyright (C) 2007-2014  Dirk Beyer
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

import static com.google.common.base.Objects.firstNonNull;
import static org.sosy_lab.cpachecker.cpa.predicate.PredicateAbstractState.mkNonAbstractionStateWithNewPathFormula;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;

import javax.annotation.Nullable;

import org.sosy_lab.common.LogManager;
import org.sosy_lab.common.Pair;
import org.sosy_lab.common.collect.PersistentMap;
import org.sosy_lab.common.configuration.Configuration;
import org.sosy_lab.common.configuration.InvalidConfigurationException;
import org.sosy_lab.common.configuration.Option;
import org.sosy_lab.common.configuration.Options;
import org.sosy_lab.common.time.Timer;
import org.sosy_lab.cpachecker.cfa.ast.c.CIdExpression;
import org.sosy_lab.cpachecker.cfa.model.AssumeEdge;
import org.sosy_lab.cpachecker.cfa.model.CFAEdge;
import org.sosy_lab.cpachecker.cfa.model.CFANode;
import org.sosy_lab.cpachecker.core.interfaces.AbstractState;
import org.sosy_lab.cpachecker.core.interfaces.AbstractStateWithAssumptions;
import org.sosy_lab.cpachecker.core.interfaces.Precision;
import org.sosy_lab.cpachecker.core.interfaces.Targetable.ViolatedProperty;
import org.sosy_lab.cpachecker.core.interfaces.TransferRelation;
import org.sosy_lab.cpachecker.cpa.assumptions.storage.AssumptionStorageState;
import org.sosy_lab.cpachecker.cpa.predicate.PredicateAbstractState.ComputeAbstractionState;
import org.sosy_lab.cpachecker.exceptions.CPATransferException;
import org.sosy_lab.cpachecker.util.AbstractStates;
import org.sosy_lab.cpachecker.util.predicates.AbstractionFormula;
import org.sosy_lab.cpachecker.util.predicates.interfaces.BooleanFormula;
import org.sosy_lab.cpachecker.util.predicates.interfaces.PathFormulaManager;
import org.sosy_lab.cpachecker.util.predicates.interfaces.view.BooleanFormulaManagerView;
import org.sosy_lab.cpachecker.util.predicates.interfaces.view.FormulaManagerView;
import org.sosy_lab.cpachecker.util.predicates.pathformula.ErrorConditions;
import org.sosy_lab.cpachecker.util.predicates.pathformula.PathFormula;

/**
 * Transfer relation for symbolic predicate abstraction. First it computes
 * the strongest post for the given CFA edge. Afterwards it optionally
 * computes an abstraction.
 */
@Options(prefix = "cpa.predicate")
public class PredicateTransferRelation implements TransferRelation {

  @Option(name = "satCheck",
      description = "maximum blocksize before a satisfiability check is done\n"
          + "(non-negative number, 0 means never, if positive should be smaller than blocksize)")
  private int satCheckBlockSize = 0;

  @Option(description = "check satisfiability when a target state has been found (should be true)")
  private boolean targetStateSatCheck = true;

  @Option(description = "Check whether all pointer dereferences are valid.")
  private boolean checkValidDeref = false;

  @Option(description = "Check whether all calls to free() are valid.")
  private boolean checkValidFree = false;

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

  private final Map<PredicateAbstractState, PathFormula> computedPathFormulae = new HashMap<>();

  private final FormulaManagerView fmgr;
  private final BooleanFormulaManagerView bfmgr;

  public PredicateTransferRelation(PredicateCPA pCpa, BlockOperator pBlk,
      Configuration config) throws InvalidConfigurationException {
    config.inject(this, PredicateTransferRelation.class);

    logger = pCpa.getLogger();
    formulaManager = pCpa.getPredicateManager();
    pathFormulaManager = pCpa.getPathFormulaManager();
    fmgr = pCpa.getFormulaManager();
    bfmgr = fmgr.getBooleanFormulaManager();
    blk = pBlk;
  }

  @Override
  public Collection<? extends AbstractState> getAbstractSuccessors(AbstractState pElement,
      Precision pPrecision, CFAEdge edge) throws CPATransferException, InterruptedException {

    postTimer.start();
    try {

      PredicateAbstractState element = (PredicateAbstractState) pElement;
      CFANode loc = edge.getSuccessor();

      // Check whether abstraction is false.
      // Such elements might get created when precision adjustment computes an abstraction.
      if (element.getAbstractionFormula().isFalse()) { return Collections.emptySet(); }

      // calculate strongest post
      if (!checkValidDeref && !checkValidFree) {
        PathFormula pathFormula = convertEdgeToPathFormula(element.getPathFormula(), edge);
        logger.log(Level.ALL, "New path formula is", pathFormula);

        // check whether to do abstraction
        boolean doAbstraction = blk.isBlockEnd(edge, pathFormula);

        return createState(element, pathFormula, loc, doAbstraction, null);
      }

      // Alternatively, with error conditions:
      Pair<PathFormula, ErrorConditions> edgeResult;
      pathFormulaTimer.start();
      try {
        edgeResult = pathFormulaManager.makeAndWithErrorConditions(element.getPathFormula(), edge);
      } finally {
        pathFormulaTimer.stop();
      }

      PathFormula pathFormula = edgeResult.getFirst();
      ErrorConditions conditions = edgeResult.getSecond();

      // check whether to do abstraction
      boolean doAbstraction = blk.isBlockEnd(edge, pathFormula);

      BooleanFormula invalidDerefCondition = conditions.getInvalidDerefCondition();
      BooleanFormula invalidFreeCondition = conditions.getInvalidFreeCondition();

      if (bfmgr.isTrue(invalidDerefCondition)) {
        return createState(element, pathFormula, loc, doAbstraction, ViolatedProperty.VALID_DEREF);
      }
      if (bfmgr.isTrue(invalidFreeCondition)) {
        return createState(element, pathFormula, loc, doAbstraction, ViolatedProperty.VALID_FREE);
      }

      List<PredicateAbstractState> newStates = new ArrayList<>(2);

      if (checkValidDeref && !bfmgr.isFalse(invalidDerefCondition)) {
        logger.log(Level.ALL, "Adding invalid-deref condition", invalidDerefCondition);
        PathFormula targetPathFormula = pathFormulaManager.makeAnd(edgeResult.getFirst(), invalidDerefCondition);
        newStates.addAll(createState(element, targetPathFormula, loc, doAbstraction,
            ViolatedProperty.VALID_DEREF));

        pathFormula = pathFormulaManager.makeAnd(pathFormula,
            bfmgr.not(invalidDerefCondition));
      }

      if (checkValidFree && !bfmgr.isFalse(invalidFreeCondition)) {
        logger.log(Level.ALL, "Adding invalid-free condition", invalidFreeCondition);
        PathFormula targetPathFormula = pathFormulaManager.makeAnd(edgeResult.getFirst(), invalidFreeCondition);
        newStates.addAll(createState(element, targetPathFormula, loc, doAbstraction,
            ViolatedProperty.VALID_FREE));

        pathFormula = pathFormulaManager.makeAnd(pathFormula,
            bfmgr.not(invalidFreeCondition));
      }

      newStates.addAll(createState(element, pathFormula, loc, doAbstraction, null));
      return newStates;
    } finally {
      postTimer.stop();
    }
  }

  private Collection<? extends PredicateAbstractState> createState(PredicateAbstractState oldState, PathFormula pathFormula,
      CFANode loc, boolean doAbstraction, @Nullable ViolatedProperty pViolatedProperty) throws InterruptedException {
    if (doAbstraction) {
      return Collections.singleton(
          new PredicateAbstractState.ComputeAbstractionState(
              pathFormula, oldState.getAbstractionFormula(), loc,
              oldState.getAbstractionLocationsOnPath(), pViolatedProperty));
    } else {
      return handleNonAbstractionFormulaLocation(pathFormula, pViolatedProperty, oldState);
    }
  }

  /**
   * Does special things when we do not compute an abstraction for the
   * successor. This currently only envolves an optional sat check.
   */
  private Collection<PredicateAbstractState> handleNonAbstractionFormulaLocation(
      PathFormula pathFormula, @Nullable ViolatedProperty pViolatedProperty,
      PredicateAbstractState oldState) throws InterruptedException {
    boolean satCheck = (satCheckBlockSize > 0) && (pathFormula.getLength() >= satCheckBlockSize);

    logger.log(Level.FINEST, "Handling non-abstraction location",
        (satCheck ? "with satisfiability check" : ""));

    if (satCheck) {
      satCheckTimer.start();

      boolean unsat = formulaManager.unsat(oldState.getAbstractionFormula(), pathFormula);

      satCheckTimer.stop();

      if (unsat) {
        numSatChecksFalse++;
        logger.log(Level.FINEST, "Abstraction & PathFormula is unsatisfiable.");
        return Collections.emptySet();
      }
    }

    // create the new abstract state for non-abstraction location
    return Collections.singleton(
        mkNonAbstractionStateWithNewPathFormula(pathFormula, pViolatedProperty, oldState));
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
   */
  private PathFormula convertEdgeToPathFormula(PathFormula pathFormula, CFAEdge edge) throws CPATransferException, InterruptedException {
    pathFormulaTimer.start();
    try {
      // compute new pathFormula with the operation on the edge
      return pathFormulaManager.makeAnd(pathFormula, edge);
    } finally {
      pathFormulaTimer.stop();
    }
  }

  @Override
  public Collection<? extends AbstractState> strengthen(AbstractState pElement,
      List<AbstractState> otherElements, CFAEdge edge, Precision pPrecision) throws CPATransferException, InterruptedException {

    strengthenTimer.start();
    try {

      PredicateAbstractState element = (PredicateAbstractState) pElement;
      if (element.isAbstractionState()) {
        // can't do anything with this object because the path formula of
        // abstraction elements has to stay "true"
        return Collections.singleton(element);
      }

      boolean errorFound = false;
      for (AbstractState lElement : otherElements) {
        if (lElement instanceof AssumptionStorageState) {
          element = strengthen(element, (AssumptionStorageState) lElement);
        }

        /*
         * Temporarily disabled.
         * The AssumeEdges produced by automata with assumptions
         * are not parsed within the correct scope,
         * and thus the types and declarations that are referenced
         * within the AST are not valid.
         * For example, all CIdExpressions in these AssumeEdges have CProblemTypes.
         * The CtoFormulaConverter with pointer-alias handling cannot handle them.
         * Probably the best would be to parse the automaton statements
         * with the scope of the CFA.
        if (lElement instanceof AbstractStateWithAssumptions) {
          element = strengthen(edge.getSuccessor(), element, (AbstractStateWithAssumptions) lElement);
        }
        */

        if (AbstractStates.isTargetState(lElement)) {
          errorFound = true;
        }
      }

      // check satisfiability in case of error
      // (not necessary for abstraction elements)
      if (errorFound && targetStateSatCheck) {
        element = strengthenSatCheck(element, edge.getSuccessor());
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

  @SuppressWarnings("unused")
  private PredicateAbstractState strengthen(CFANode pNode, PredicateAbstractState pElement,
      AbstractStateWithAssumptions pAssumeElement) throws CPATransferException, InterruptedException {

    PathFormula pf = pElement.getPathFormula();

    // TODO how to get a pseudo variable for the current function with the correct type here?
    // We would need to have access to the current function's declaration.
    CIdExpression retVar = null;

    for (AssumeEdge assumption : pAssumeElement.getAsAssumeEdges(retVar, pNode.getFunctionName())) {

        pf = convertEdgeToPathFormula(pf, assumption);
    }

    if (pf != pElement.getPathFormula()) {
      return replacePathFormula(pElement, pf);
    } else {
      return pElement;
    }
  }

  private PredicateAbstractState strengthen(PredicateAbstractState pElement,
      AssumptionStorageState pElement2) {

    if (pElement2.isAssumptionTrue() || pElement2.isAssumptionFalse()) {
      // we don't add the assumption false in order to not forget the content of the path formula
      // (we need it for post-processing)
      return pElement;
    }

    String asmpt = pElement2.getAssumptionAsString().toString();

    PathFormula pf = pathFormulaManager.makeAnd(pElement.getPathFormula(), fmgr.parse(asmpt));

    return replacePathFormula(pElement, pf);
  }

  /**
   * Returns a new state with a given pathFormula. All other fields stay equal.
   */
  private PredicateAbstractState replacePathFormula(PredicateAbstractState oldElement, PathFormula newPathFormula) {
    if (oldElement instanceof ComputeAbstractionState) {
      CFANode loc = ((ComputeAbstractionState) oldElement).getLocation();
      return new ComputeAbstractionState(newPathFormula,
          oldElement.getAbstractionFormula(), loc,
          oldElement.getAbstractionLocationsOnPath(),
          oldElement.getViolatedProperty());
    } else {
      assert !oldElement.isAbstractionState();
      return mkNonAbstractionStateWithNewPathFormula(newPathFormula, oldElement.getViolatedProperty(), oldElement);
    }
  }

  private PredicateAbstractState strengthenSatCheck(
      PredicateAbstractState pElement, CFANode loc) throws InterruptedException {
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
      AbstractionFormula abs = formulaManager.makeTrueAbstractionFormula(pathFormula);

      PathFormula newPathFormula = pathFormulaManager.makeEmptyPathFormula(pathFormula);

      // update abstraction locations map
      PersistentMap<CFANode, Integer> abstractionLocations = pElement.getAbstractionLocationsOnPath();
      Integer newLocInstance = firstNonNull(abstractionLocations.get(loc), 0) + 1;
      abstractionLocations = abstractionLocations.putAndCopy(loc, newLocInstance);

      return PredicateAbstractState.mkAbstractionState(bfmgr, newPathFormula,
          abs, abstractionLocations, pElement.getViolatedProperty());
    }
  }

  boolean areAbstractSuccessors(AbstractState pElement, CFAEdge pCfaEdge,
      Collection<? extends AbstractState> pSuccessors) throws CPATransferException, InterruptedException {
    PredicateAbstractState predicateElement = (PredicateAbstractState) pElement;
    PathFormula pathFormula = computedPathFormulae.get(predicateElement);
    if (pathFormula == null) {
      pathFormula = pathFormulaManager.makeEmptyPathFormula(predicateElement.getPathFormula());
    }
    boolean result = true;

    if (pSuccessors.isEmpty()) {
      satCheckTimer.start();
      PathFormula pFormula = convertEdgeToPathFormula(pathFormula, pCfaEdge);
      Collection<? extends AbstractState> foundSuccessors =
          handleNonAbstractionFormulaLocation(pFormula, predicateElement.getViolatedProperty(), predicateElement);
      //if we found successors, they all have to be unsat
      for (AbstractState e : foundSuccessors) {
        PredicateAbstractState successor = (PredicateAbstractState) e;
        if (!formulaManager.unsat(successor.getAbstractionFormula(), successor.getPathFormula())) {
          result = false;
        }
      }
      satCheckTimer.stop();
      return result;
    }

    for (AbstractState e : pSuccessors) {
      PredicateAbstractState successor = (PredicateAbstractState) e;

      if (successor.isAbstractionState()) {
        pathFormula = convertEdgeToPathFormula(pathFormula, pCfaEdge);
        // check abstraction
        abstractionCheckTimer.start();
        if (!formulaManager.checkCoverage(predicateElement.getAbstractionFormula(), pathFormula,
            successor.getAbstractionFormula())) {
          result = false;
        }
        abstractionCheckTimer.stop();
      } else {
        // check abstraction
        abstractionCheckTimer.start();
        if (!successor.getAbstractionFormula().equals(predicateElement.getAbstractionFormula())) {
          result = false;
        }
        abstractionCheckTimer.stop();

        // compute path formula
        PathFormula computedPathFormula = convertEdgeToPathFormula(pathFormula, pCfaEdge);
        PathFormula mergeWithPathFormula = computedPathFormulae.get(successor);
        if (mergeWithPathFormula != null) {
          computedPathFormulae.put(successor, pathFormulaManager.makeOr(mergeWithPathFormula, computedPathFormula));
        } else {
          computedPathFormulae.put(successor, computedPathFormula);
        }
      }
    }

    return result;
  }
}
