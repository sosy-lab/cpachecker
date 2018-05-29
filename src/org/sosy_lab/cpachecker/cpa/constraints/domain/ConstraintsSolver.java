/*
 *  CPAchecker is a tool for configurable software verification.
 *  This file is part of CPAchecker.
 *
 *  Copyright (C) 2007-2018  Dirk Beyer
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
package org.sosy_lab.cpachecker.cpa.constraints.domain;

import static com.google.common.base.Preconditions.checkState;

import com.google.common.collect.HashBasedTable;
import com.google.common.collect.HashMultimap;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Multimap;
import com.google.common.collect.Sets;
import com.google.common.collect.Table;
import java.io.PrintStream;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import javax.annotation.Nullable;
import org.sosy_lab.common.configuration.Configuration;
import org.sosy_lab.common.configuration.InvalidConfigurationException;
import org.sosy_lab.common.configuration.Option;
import org.sosy_lab.common.configuration.Options;
import org.sosy_lab.cpachecker.core.CPAcheckerResult.Result;
import org.sosy_lab.cpachecker.core.interfaces.Statistics;
import org.sosy_lab.cpachecker.core.reachedset.UnmodifiableReachedSet;
import org.sosy_lab.cpachecker.cpa.constraints.FormulaCreator;
import org.sosy_lab.cpachecker.cpa.constraints.FormulaCreatorUsingCConverter;
import org.sosy_lab.cpachecker.cpa.constraints.constraint.Constraint;
import org.sosy_lab.cpachecker.cpa.constraints.constraint.IdentifierAssignment;
import org.sosy_lab.cpachecker.cpa.value.symbolic.type.SymbolicIdentifier;
import org.sosy_lab.cpachecker.cpa.value.symbolic.util.SymbolicValues;
import org.sosy_lab.cpachecker.cpa.value.type.Value;
import org.sosy_lab.cpachecker.exceptions.UnrecognizedCCodeException;
import org.sosy_lab.cpachecker.util.Pair;
import org.sosy_lab.cpachecker.util.predicates.pathformula.ctoformula.CtoFormulaConverter;
import org.sosy_lab.cpachecker.util.predicates.smt.BooleanFormulaManagerView;
import org.sosy_lab.cpachecker.util.predicates.smt.FormulaManagerView;
import org.sosy_lab.cpachecker.util.predicates.smt.Solver;
import org.sosy_lab.java_smt.api.BooleanFormula;
import org.sosy_lab.java_smt.api.Model.ValueAssignment;
import org.sosy_lab.java_smt.api.ProverEnvironment;
import org.sosy_lab.java_smt.api.SolverContext.ProverOptions;
import org.sosy_lab.java_smt.api.SolverException;

@Options(prefix = "cpa.constraints")
public class ConstraintsSolver implements Statistics {

  @Option(
    secure = true,
    description = "Whether to use subset/superset caching",
    name = "cacheSubsets"
  )
  private boolean cacheSubsets = true;

  private ConstraintsCache cache;
  private Solver solver;
  private ProverEnvironment prover;
  private FormulaManagerView formulaManager;
  private BooleanFormulaManagerView booleanFormulaManager;
  private CtoFormulaConverter converter;

  /** Table of id constraints set, id identifier assignment, formula * */
  private Table<Integer, Integer, BooleanFormula> constraintFormulas = HashBasedTable.create();

  public ConstraintsSolver(
      final Configuration pConfig,
      final Solver pSolver,
      final FormulaManagerView pFormulaManager,
      final CtoFormulaConverter pConverter)
      throws InvalidConfigurationException {
    pConfig.inject(this);

    solver = pSolver;
    formulaManager = pFormulaManager;
    booleanFormulaManager = formulaManager.getBooleanFormulaManager();
    converter = pConverter;

    if (cacheSubsets) {
      cache = new SubsetConstraintsCache();
    } else {
      cache = new MatchingConstraintsCache();
    }
  }

  public boolean isUnsat(
      Constraint pConstraint, IdentifierAssignment pAssignment, String pFunctionName)
      throws UnrecognizedCCodeException, InterruptedException, SolverException {
    BooleanFormula constraintAsFormula =
        getFullFormula(Collections.singleton(pConstraint), pAssignment, pFunctionName);
    return solver.isUnsat(constraintAsFormula);
  }

  /**
   * Returns whether this state is unsatisfiable. A state without constraints (that is, an empty
   * state), is always satisfiable.
   *
   * @return <code>true</code> if this state is unsatisfiable, <code>false</code> otherwise
   */
  public boolean isUnsat(ConstraintsState pConstraints, String pFunctionName)
      throws SolverException, InterruptedException, UnrecognizedCCodeException {

    if (!pConstraints.isEmpty()) {
      try {
        BooleanFormula constraintsAsFormula =
            getFullFormula(pConstraints, pConstraints.getDefiniteAssignment(), pFunctionName);
        prover = solver.newProverEnvironment(ProverOptions.GENERATE_MODELS);
        prover.push(constraintsAsFormula);

        List<ValueAssignment> modelAsAssignment = pConstraints.getModel();
        BooleanFormula lastModel =
            modelAsAssignment
                .stream()
                .map(ValueAssignment::getAssignmentAsFormula)
                .collect(booleanFormulaManager.toConjunction());
        prover.push(lastModel);
        boolean unsat = prover.isUnsat();

        boolean gotResultFromCache = false;

        if (!booleanFormulaManager.isTrue(lastModel)) {
          // if the last model does not fulfill the formula, and the last model actually
          // is some variable assignment (i.e., model != true), then we check the formula
          // for satisfiability without any assignments, again.
          prover.pop(); // Remove model assignment from prover

          if (unsat) {
            CacheResult res = cache.getCachedResult(pConstraints);

            if (res.isUnsat()) {
              unsat = true;
              gotResultFromCache = true;
            } else if (res.isSat()) {
              unsat = false;
              gotResultFromCache = true;
              pConstraints.setModel(res.getModelAssignment());
            } else {
              unsat = prover.isUnsat();
            }
          }
        }

        if (!gotResultFromCache) {
          if (!unsat) {
            ImmutableList<ValueAssignment> lastModelAsAssignment = prover.getModelAssignments();
            pConstraints.setModel(lastModelAsAssignment);
            cache.addSat(pConstraints, lastModelAsAssignment);
            // doing this while the complete formula is still on the prover environment stack is
            // cheaper than performing another complete SAT check when the assignment is really
            // requested
            resolveDefiniteAssignments(pConstraints, lastModelAsAssignment, pFunctionName);

          } else {
            cache.addUnsat(pConstraints);
          }
        }

        return unsat;

      } finally {
        closeProver();
      }

    } else {
      return false;
    }
  }

  private void closeProver() {
    if (prover != null) {
      prover.close();
      prover = null;
    }
  }

  private void resolveDefiniteAssignments(
      ConstraintsState pConstraints, List<ValueAssignment> pModel, String pFunctionName)
      throws InterruptedException, SolverException {

    IdentifierAssignment newDefinites =
        computeDefiniteAssignment(pConstraints, pModel, pFunctionName);
    pConstraints.setDefiniteAssignment(newDefinites);
  }

  private IdentifierAssignment computeDefiniteAssignment(
      ConstraintsState pState, List<ValueAssignment> pModel, String pFunctionName)
      throws SolverException, InterruptedException {

    IdentifierAssignment newDefinites = new IdentifierAssignment(pState.getDefiniteAssignment());

    for (ValueAssignment val : pModel) {
      if (SymbolicValues.isSymbolicTerm(val.getName())) {

        SymbolicIdentifier identifier =
            SymbolicValues.convertTermToSymbolicIdentifier(val.getName());
        Value concreteValue = SymbolicValues.convertToValue(val);

        if (!newDefinites.containsKey(identifier)
            && isOnlySatisfyingAssignment(val, pFunctionName)) {

          assert !newDefinites.containsKey(identifier)
                  || newDefinites.get(identifier).equals(concreteValue)
              : "Definite assignment can't be changed from "
                  + newDefinites.get(identifier)
                  + " to "
                  + concreteValue;

          newDefinites.put(identifier, concreteValue);
        }
      }
    }
    assert newDefinites.entrySet().containsAll(pState.getDefiniteAssignment().entrySet());
    return newDefinites;
  }

  private boolean isOnlySatisfyingAssignment(ValueAssignment pTerm, String pFunctionName)
      throws SolverException, InterruptedException {

    FormulaCreator formulaCreator = getFormulaCreator(pFunctionName);
    BooleanFormula prohibitAssignment =
        formulaManager.makeNot(
            formulaCreator.transformAssignment(pTerm.getKey(), pTerm.getValue()));

    prover.push(prohibitAssignment);
    boolean isUnsat = prover.isUnsat();

    // remove the just added formula again so we return to the original constraint formula
    // - other assignments will probably be tested before closing prover.
    prover.pop();

    return isUnsat;
  }

  private FormulaCreator getFormulaCreator(String pFunctionName) {
    return new FormulaCreatorUsingCConverter(formulaManager, converter, pFunctionName);
  }

  /**
   * Returns the formula representing the conjunction of all constraints of this state. If no
   * constraints exist, this method will return <code>null</code>.
   *
   * @return the formula representing the conjunction of all constraints of this state
   * @throws UnrecognizedCCodeException see {@link FormulaCreator#createFormula(Constraint)}
   * @throws InterruptedException see {@link FormulaCreator#createFormula(Constraint)}
   */
  private BooleanFormula getFullFormula(
      Collection<Constraint> pConstraints, IdentifierAssignment pAssignment, String pFunctionName)
      throws UnrecognizedCCodeException, InterruptedException {
    List<BooleanFormula> formulas = new ArrayList<>(pConstraints.size());
    for (Constraint c : pConstraints) {
      int constraintsId = getConstraintId(c);
      int identifierId = getAssignmentId(pAssignment);
      if (!constraintFormulas.contains(constraintsId, identifierId)) {
        constraintFormulas.put(
            constraintsId, identifierId, createConstraintFormulas(c, pAssignment, pFunctionName));
      }
      formulas.add(constraintFormulas.get(constraintsId, identifierId));
    }

    return booleanFormulaManager.and(formulas);
  }

  private int getAssignmentId(IdentifierAssignment pAssignment) {
    return pAssignment.hashCode();
  }

  private int getConstraintId(final Constraint pConstraint) {
    return pConstraint.hashCode();
  }

  private int getConstraintId(Collection<Constraint> pConstraints) {
    int id = 0;
    // do this manually to make sure that we get the same id independent of the data structure
    // used
    for (Constraint c : pConstraints) {
      id += getConstraintId(c);
    }
    return id;
  }

  private BooleanFormula createConstraintFormulas(
      Constraint pConstraint, IdentifierAssignment pAssignment, String pFunctionName)
      throws UnrecognizedCCodeException, InterruptedException {
    assert !constraintFormulas.contains(getConstraintId(pConstraint), getAssignmentId(pAssignment))
        : "Trying to add a formula that already exists!";

    return getFormulaCreator(pFunctionName).createFormula(pConstraint, pAssignment);
  }

  @Override
  public void printStatistics(PrintStream out, Result result, UnmodifiableReachedSet reached) {}

  @Nullable
  @Override
  public String getName() {
    return null;
  }

  private interface ConstraintsCache {
    CacheResult getCachedResult(Collection<Constraint> pConstraints);

    void addSat(
        Collection<Constraint> pConstraints, ImmutableList<ValueAssignment> pModelAssignment);

    void addUnsat(Collection<Constraint> pConstraints);
  }

  private class MatchingConstraintsCache implements ConstraintsCache {

    private Map<Integer, CacheResult> cacheMap = new HashMap<>();

    @Override
    public CacheResult getCachedResult(Collection<Constraint> pConstraints) {
      int id = getConstraintId(pConstraints);
      return getCachedResult(id);
    }

    CacheResult getCachedResult(int pId) {
      if (cacheMap.containsKey(pId)) {
        return cacheMap.get(pId);
      } else {
        return CacheResult.getUnknown();
      }
    }

    @Override
    public void addSat(
        Collection<Constraint> pConstraints, ImmutableList<ValueAssignment> pModelAssignment) {
      add(pConstraints, CacheResult.getSat(pModelAssignment));
    }

    @Override
    public void addUnsat(Collection<Constraint> pConstraints) {
      add(pConstraints, CacheResult.getUnsat());
    }

    private void add(Collection<Constraint> pConstraints, CacheResult pResult) {
      int id = getConstraintId(pConstraints);
      cacheMap.put(id, pResult);
    }
  }

  private class SubsetConstraintsCache implements ConstraintsCache {

    private MatchingConstraintsCache delegate;

    /**
     * Multimap that maps each constraint to all collections of constraints that it occurred in,
     * where each collection of constraints is represented by a pair &lt;id, size&gt;
     */
    private Multimap<Constraint, Pair<Integer, Integer>> constraintContainedIn =
        HashMultimap.create();

    public SubsetConstraintsCache() {
      delegate = new MatchingConstraintsCache();
    }

    @Override
    public CacheResult getCachedResult(Collection<Constraint> pConstraints) {
      CacheResult res = delegate.getCachedResult(pConstraints);
      if (!res.isSat() && !res.isUnsat()) {
        res = getCachedResultOfSubset(pConstraints);
      }
      return res;
    }

    @Override
    public void addSat(
        Collection<Constraint> pConstraints, ImmutableList<ValueAssignment> pModelAssignment) {
      add(pConstraints);
      delegate.addSat(pConstraints, pModelAssignment);
    }

    @Override
    public void addUnsat(Collection<Constraint> pConstraints) {
      add(pConstraints);
      delegate.addUnsat(pConstraints);
    }

    private void add(Collection<Constraint> pConstraints) {
      int id = getConstraintId(pConstraints);
      Pair<Integer, Integer> idAndSize = Pair.of(id, pConstraints.size());
      for (Constraint c : pConstraints) {
        constraintContainedIn.put(c, idAndSize);
      }
    }

    CacheResult getCachedResultOfSubset(Collection<Constraint> pConstraints) {
      checkState(!pConstraints.isEmpty());

      Set<Pair<Integer, Integer>> containAllConstraints = null;
      for (Constraint c : pConstraints) {
        Set<Pair<Integer, Integer>> containC = ImmutableSet.copyOf(constraintContainedIn.get(c));
        if (containAllConstraints == null) {
          containAllConstraints = containC;
        } else {
          containAllConstraints = Sets.intersection(containAllConstraints, containC);
        }

        if (containAllConstraints.isEmpty()) {
          return CacheResult.getUnknown();
        }
      }

      int sizeOfQuery = pConstraints.size();
      for (Pair<Integer, Integer> col : containAllConstraints) {
        int idOfCollection = col.getFirst();
        int sizeOfCollection = col.getSecond();
        CacheResult cachedResult = delegate.getCachedResult(idOfCollection);
        if (sizeOfQuery <= sizeOfCollection && cachedResult.isSat()) {
          // currently considered collection is a superset of the queried collection
          return cachedResult;

        } else if (sizeOfQuery >= sizeOfCollection && cachedResult.isUnsat()) {
          // currently considered collection is a subset of the queried collection
          return cachedResult;
        }
      }
      return CacheResult.getUnknown();
    }
  }

  private static class CacheResult {
    enum Result {
      SAT,
      UNSAT,
      UNKNOWN
    }

    private static final CacheResult UNSAT_SINGLETON =
        new CacheResult(Result.UNSAT, Optional.empty());
    private static final CacheResult UNKNOWN_SINGLETON =
        new CacheResult(Result.UNSAT, Optional.empty());

    private Result result;
    private Optional<ImmutableList<ValueAssignment>> modelAssignment;

    public static CacheResult getSat(ImmutableList<ValueAssignment> pModelAssignment) {
      return new CacheResult(Result.SAT, Optional.of(pModelAssignment));
    }

    public static CacheResult getUnsat() {
      return UNSAT_SINGLETON;
    }

    public static CacheResult getUnknown() {
      return UNKNOWN_SINGLETON;
    }

    private CacheResult(Result pResult, Optional<ImmutableList<ValueAssignment>> pModelAssignment) {
      result = pResult;
      modelAssignment = pModelAssignment;
    }

    public boolean isSat() {
      return result.equals(Result.SAT);
    }

    public boolean isUnsat() {
      return result.equals(Result.UNSAT);
    }

    public ImmutableList<ValueAssignment> getModelAssignment() {
      checkState(modelAssignment.isPresent(), "No model exists");
      return modelAssignment.get();
    }
  }
}
