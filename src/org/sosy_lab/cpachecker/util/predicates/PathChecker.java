/*
 *  CPAchecker is a tool for configurable software verification.
 *  This file is part of CPAchecker.
 *
 *  Copyright (C) 2007-2013  Dirk Beyer
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
package org.sosy_lab.cpachecker.util.predicates;

import static com.google.common.base.Predicates.notNull;
import static com.google.common.collect.FluentIterable.from;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.logging.Level;

import org.sosy_lab.common.LogManager;
import org.sosy_lab.cpachecker.cfa.model.CFAEdge;
import org.sosy_lab.cpachecker.core.Model;
import org.sosy_lab.cpachecker.core.Model.AssignableTerm;
import org.sosy_lab.cpachecker.core.Model.CFAPathWithAssignments;
import org.sosy_lab.cpachecker.core.Model.Constant;
import org.sosy_lab.cpachecker.core.Model.Function;
import org.sosy_lab.cpachecker.core.Model.Variable;
import org.sosy_lab.cpachecker.exceptions.CPATransferException;
import org.sosy_lab.cpachecker.exceptions.SolverException;
import org.sosy_lab.cpachecker.util.predicates.interfaces.BooleanFormula;
import org.sosy_lab.cpachecker.util.predicates.interfaces.PathFormulaManager;
import org.sosy_lab.cpachecker.util.predicates.interfaces.ProverEnvironment;
import org.sosy_lab.cpachecker.util.predicates.interpolation.CounterexampleTraceInfo;
import org.sosy_lab.cpachecker.util.predicates.pathformula.PathFormula;
import org.sosy_lab.cpachecker.util.predicates.pathformula.SSAMap;

import com.google.common.collect.HashMultimap;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Multimap;

/**
 * This class can check feasibility of a simple path using an SMT solver.
 */
public class PathChecker {

  private final LogManager logger;
  private final PathFormulaManager pmgr;
  private final Solver solver;

  public PathChecker(LogManager pLogger, PathFormulaManager pPmgr, Solver pSolver) {
    logger = pLogger;
    pmgr = pPmgr;
    solver = pSolver;
  }

  public CounterexampleTraceInfo checkPath(List<CFAEdge> pPath) throws CPATransferException, InterruptedException {
    List<SSAMap> ssaMaps = new ArrayList<>(pPath.size());

    PathFormula pathFormula = pmgr.makeEmptyPathFormula();
    for (CFAEdge edge : from(pPath).filter(notNull())) {
      pathFormula = pmgr.makeAnd(pathFormula, edge);
      ssaMaps.add(pathFormula.getSsa());
    }

    BooleanFormula f = pathFormula.getFormula();

    try (ProverEnvironment thmProver = solver.newProverEnvironmentWithModelGeneration()) {
      thmProver.push(f);
      if (thmProver.isUnsat()) {
        return CounterexampleTraceInfo.infeasibleNoItp();
      } else {
        Model model = getModel(thmProver);
        model = model.withAssignmentInformation(extractVariableAssignment(pPath, ssaMaps, model));

        return CounterexampleTraceInfo.feasible(ImmutableList.of(f), model, ImmutableMap.<Integer, Boolean>of());
      }
    }
  }

  /**
   * Given a model and a path, extract the information when each variable
   * from the model was assigned.
   */
  private CFAPathWithAssignments extractVariableAssignment(List<CFAEdge> pPath, List<SSAMap> pSsaMaps,
      Model pModel) {

    // Create a map that holds all AssignableTerms that occured
    // in the given path.
    final Multimap<Integer, AssignableTerm> assignedTermsPosition = HashMultimap.create();

    Set<Constant> constants = new HashSet<>();

    for (AssignableTerm term : pModel.keySet()) {

      if (term instanceof Variable) {
        int index = findFirstOccurrenceOfVariable((Variable) term, pSsaMaps);
        if (index >= 0) {
          assignedTermsPosition.put(index, term);
        }
      } else if(term instanceof Function) {
        int index = findFirstOccurrenceOfVariable((Function) term, pSsaMaps);
        if (index >= 0) {
          assignedTermsPosition.put(index, term);
        }
      } else if(term instanceof Constant)  {
        constants.add((Constant) term);
      }
    }

    return new CFAPathWithAssignments(pPath, assignedTermsPosition, pModel, constants);
  }

  private int findFirstOccurrenceOfVariable(Function pTerm, List<SSAMap> pSsaMaps) {

    int lower = 0;
    int upper = pSsaMaps.size() - 1;

    int result = -1;

    // do binary search
    while (true) {
      if (upper-lower <= 0) {

        if (upper - lower == 0) {
          int ssaIndex = pSsaMaps.get(upper).getIndex(getName(pTerm));

          if (ssaIndex == getSSAIndex(pTerm)) {
            result = upper;
          }
        }

        return result;
      }

      int index = lower + ((upper-lower) / 2);
      assert index >= lower;
      assert index <= upper;

      int ssaIndex = pSsaMaps.get(index).getIndex(getName(pTerm));

      if (ssaIndex < getSSAIndex(pTerm)) {
        lower = index + 1;
      } else if (ssaIndex > getSSAIndex(pTerm)) {
        upper = index - 1;
      } else {
        // found a matching SSAMap,
        // but we keep looking whether there is another one with a smaller index
        assert result == -1 || result > index;
        result = index;
        upper = index - 1;
      }
    }
  }

  private int getSSAIndex(Function pTerm) {

    String[] nameAndIndex = pTerm.getName().split("@");

    if (nameAndIndex.length == 2) {
      String index = nameAndIndex[1];

      if (index.matches("\\d*")) {
        return Integer.parseInt(index);
      }

    }

    return -2;
  }

  private String getName(Function pTerm) {

    String[] nameAndIndex = pTerm.getName().split("@");

    if (nameAndIndex.length == 2) {
      return nameAndIndex[0];
    }

    return pTerm.getName();
  }

  /**
   * Search through an (ordered) list of SSAMaps
   * for the first index where a given variable appears.
   * @return -1 if the variable with the given index never occurs, or an index of pSsaMaps
   */
  private int findFirstOccurrenceOfVariable(Variable pVar, List<SSAMap> pSsaMaps) {

    // both indices are inclusive bounds of the range where we still need to look
    int lower = 0;
    int upper = pSsaMaps.size() - 1;

    int result = -1;

    /*Due to the new way to handle aliases, assignable terms of variables
    may be replaced with UIFs in the SSAMap. If this is the case, modify upper
    by looking for the variable in the other maps*/
    if (pSsaMaps.size() <= 0) {
      return result;
    } else {

      while (upper >= 0 &&
          (pSsaMaps.get(upper).getIndex(pVar.getName())
            == SSAMap.INDEX_NOT_CONTAINED)) {
        upper--;
      }

      if (upper < 0) {
        return result;
      }
    }

    // do binary search
    while (true) {
      if (upper-lower <= 0) {

        if (upper - lower == 0) {
          int ssaIndex = pSsaMaps.get(upper).getIndex(pVar.getName());

          if (ssaIndex == pVar.getSSAIndex()) {
            result = upper;
          }
        }

        return result;
      }

      int index = lower + ((upper-lower) / 2);
      assert index >= lower;
      assert index <= upper;

      int ssaIndex = pSsaMaps.get(index).getIndex(pVar.getName());

      if (ssaIndex < pVar.getSSAIndex()) {
        lower = index + 1;
      } else if (ssaIndex > pVar.getSSAIndex()) {
        upper = index - 1;
      } else {
        // found a matching SSAMap,
        // but we keep looking whether there is another one with a smaller index
        assert result == -1 || result > index;
        result = index;
        upper = index - 1;
      }
    }
  }

  private <T> Model getModel(ProverEnvironment thmProver) {
    try {
      return thmProver.getModel();
    } catch (SolverException e) {
      logger.log(Level.WARNING, "Solver could not produce model, variable assignment of error path can not be dumped.");
      logger.logDebugException(e);
      return Model.empty();
    }
  }
}
