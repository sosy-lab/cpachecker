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

import static com.google.common.base.Preconditions.checkNotNull;
import static com.google.common.base.Preconditions.checkState;

import com.google.common.collect.HashMultimap;
import com.google.common.collect.ImmutableCollection;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Multimap;
import com.google.common.collect.Sets;
import java.io.PrintStream;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
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
import org.sosy_lab.cpachecker.core.interfaces.StatisticsProvider;
import org.sosy_lab.cpachecker.core.reachedset.UnmodifiableReachedSet;
import org.sosy_lab.cpachecker.cpa.constraints.FormulaCreator;
import org.sosy_lab.cpachecker.cpa.constraints.FormulaCreatorUsingCConverter;
import org.sosy_lab.cpachecker.cpa.constraints.constraint.Constraint;
import org.sosy_lab.cpachecker.cpa.value.symbolic.type.SymbolicIdentifier;
import org.sosy_lab.cpachecker.cpa.value.symbolic.util.SymbolicIdentifierLocator;
import org.sosy_lab.cpachecker.cpa.value.symbolic.util.SymbolicValues;
import org.sosy_lab.cpachecker.exceptions.UnrecognizedCodeException;
import org.sosy_lab.cpachecker.util.predicates.pathformula.ctoformula.CtoFormulaConverter;
import org.sosy_lab.cpachecker.util.predicates.smt.BooleanFormulaManagerView;
import org.sosy_lab.cpachecker.util.predicates.smt.FormulaManagerView;
import org.sosy_lab.cpachecker.util.predicates.smt.Solver;
import org.sosy_lab.cpachecker.util.statistics.StatCounter;
import org.sosy_lab.cpachecker.util.statistics.StatKind;
import org.sosy_lab.cpachecker.util.statistics.StatTimer;
import org.sosy_lab.cpachecker.util.statistics.StatisticsWriter;
import org.sosy_lab.java_smt.api.BooleanFormula;
import org.sosy_lab.java_smt.api.Model.ValueAssignment;
import org.sosy_lab.java_smt.api.ProverEnvironment;
import org.sosy_lab.java_smt.api.SolverContext.ProverOptions;
import org.sosy_lab.java_smt.api.SolverException;

@Options(prefix = "cpa.constraints")
public class ConstraintsSolver implements StatisticsProvider {

  @Option(
    secure = true,
    description = "Whether to use subset/superset caching",
    name = "cacheSubsets"
  )
  private boolean cacheSubsets = true;

  @Option(
      secure = true,
      description = "Whether to perform SAT checks only for the last added constraint",
      name = "minimalSatCheck")
  private boolean performMinimalSatCheck = true;

  @Option(
      secure = true,
      description = "Whether to perform caching of constraint satisfiability results",
      name = "cache"
  )
  private boolean doCaching = true;


  @Option(
      secure = true,
      description = "Resolve definite assignments",
      name = "resolveDefinites"
  )
  private boolean resolveDefinites = true;

  @Option(
      secure = true,
      description = "Try model of predecessor constraints state before running SMT solver",
      name = "useLastModel")
  private boolean useLastModel = true;

  private final StatTimer
      timeForSolving = new StatTimer(StatKind.SUM, "Time for solving constraints");
  private final StatTimer timeForIndependentComputation =
      new StatTimer(StatKind.SUM, "Time for independent computation");
  private final StatTimer timeForDefinitesComputation =
      new StatTimer(StatKind.SUM, "Time for resolving definites");
  private final StatTimer timeForModelReuse =
      new StatTimer(StatKind.SUM, "Time for model re-use attempts");
  private final StatTimer timeForSatCheck =
      new StatTimer(StatKind.SUM, "Time for SMT check");

  private final StatCounter modelReuseSuccesses = new StatCounter("Successful model re-uses");

  private ConstraintsCache cache;
  private Solver solver;
  private ProverEnvironment prover;
  private FormulaManagerView formulaManager;
  private BooleanFormulaManagerView booleanFormulaManager;

  private CtoFormulaConverter converter;
  private SymbolicIdentifierLocator locator;

  /** Table of id constraints set, id identifier assignment, formula * */
  private Map<Constraint, BooleanFormula> constraintFormulas = new HashMap<>();

  private BooleanFormula literalForModel;
  private BooleanFormula literalForSingleAssignment;

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
    literalForModel = booleanFormulaManager.makeVariable("__M");
    literalForSingleAssignment = booleanFormulaManager.makeVariable("__A");
    converter = pConverter;
    locator = SymbolicIdentifierLocator.getInstance();

    if (doCaching) {
      if (cacheSubsets) {
        cache = new SubsetConstraintsCache();
      } else {
        cache = new MatchingConstraintsCache();
      }
    } else {
      cache = new DummyCache();
    }
  }

  public boolean isUnsat(
      Constraint pConstraint, ImmutableList<ValueAssignment> pAssignment, String pFunctionName)
      throws UnrecognizedCodeException, InterruptedException, SolverException {
    ConstraintsState s = new ConstraintsState(Collections.singleton(pConstraint));
    s.setDefiniteAssignment(pAssignment);
    return isUnsat(s, pFunctionName);
  }

  /**
   * Returns whether this state is unsatisfiable. A state without constraints (that is, an empty
   * state), is always satisfiable.
   *
   * @return <code>true</code> if this state is unsatisfiable, <code>false</code> otherwise
   */
  public boolean isUnsat(ConstraintsState pConstraints, String pFunctionName)
      throws SolverException, InterruptedException, UnrecognizedCodeException {

    if (pConstraints.isEmpty()) {
      return false;
    }

    try {
      timeForSolving.start();

      Boolean unsat = null; // assign null to fail fast if assignment is missed
      Set<Constraint> relevantConstraints = getRelevantConstraints(pConstraints);

      Collection<BooleanFormula> constraintsAsFormulas =
          getFullFormula(relevantConstraints, pFunctionName);
      CacheResult res = cache.getCachedResult(constraintsAsFormulas);

      if (res.isUnsat()) {
        unsat = true;

      } else if (res.isSat()) {
        unsat = false;
        pConstraints.setModel(res.getModelAssignment());

      } else {
        prover = solver.newProverEnvironment(ProverOptions.GENERATE_MODELS);
        BooleanFormula definitesAndConstraints =
            combineWithDefinites(constraintsAsFormulas, pConstraints);
        prover.push(definitesAndConstraints);

        ImmutableList<ValueAssignment> newModelAsAssignment;
        ImmutableList<ValueAssignment> modelAsAssignment = pConstraints.getModel();

        boolean modelExists = !modelAsAssignment.isEmpty();
        if (useLastModel && modelExists) {
          try {
            timeForModelReuse.start();
            BooleanFormula modelFormula =
                modelAsAssignment
                    .stream()
                    .map(ValueAssignment::getAssignmentAsFormula)
                    .collect(booleanFormulaManager.toConjunction());
            modelFormula = createLiteralLabel(literalForModel, modelFormula);
            prover.push(modelFormula);
            unsat = prover.isUnsatWithAssumptions(
                ImmutableList.of(literalForModel));
            if (!unsat) {
              modelReuseSuccesses.inc();
            }
          } finally {
            timeForModelReuse.stop();
          }
        }

        if (unsat == null || unsat) {
          try {
            timeForSatCheck.start();
            unsat = prover.isUnsat();
          } finally {
            timeForSatCheck.stop();
          }
        }

        if (!unsat) {
          newModelAsAssignment = prover.getModelAssignments();
          pConstraints.setModel(newModelAsAssignment);
          cache.addSat(constraintsAsFormulas, newModelAsAssignment);
          // doing this while the complete formula is still on the prover environment stack is
          // cheaper than performing another complete SAT check when the assignment is really
          // requested
          if (resolveDefinites) {
            pConstraints.setDefiniteAssignment(
                resolveDefiniteAssignments(pConstraints, newModelAsAssignment));
          }

          assert pConstraints.getModel().containsAll(pConstraints.getDefiniteAssignment())
              : "Model does not imply definites: " + pConstraints.getModel() + " !=> "
              + pConstraints.getDefiniteAssignment();

        } else {
          assert prover.isUnsat()
              : "Unsat with definite assignment, but not without. Definite assignment: "
              + pConstraints.getDefiniteAssignment();

          cache.addUnsat(constraintsAsFormulas);
        }
      }

      return unsat;

    } finally {
      closeProver();
      timeForSolving.stop();
    }
  }

  private BooleanFormula combineWithDefinites(
      Collection<BooleanFormula> pConstraintsAsFormulas,
      ConstraintsState pConstraints) {

    BooleanFormula singleConstraintFormula = booleanFormulaManager.and(pConstraintsAsFormulas);
    BooleanFormula definites = getDefAssignmentsFormula(pConstraints);
    return booleanFormulaManager.and(definites, singleConstraintFormula);
  }

  private BooleanFormula getDefAssignmentsFormula(ConstraintsState pConstraints) {
    return pConstraints.getDefiniteAssignment()
        .stream()
        .map(ValueAssignment::getAssignmentAsFormula)
        .collect(booleanFormulaManager.toConjunction());
  }

  private BooleanFormula createLiteralLabel(
      BooleanFormula pLiteral,
      BooleanFormula pFormula) {
    return booleanFormulaManager.implication(pLiteral, pFormula);
  }

  private Set<Constraint> getRelevantConstraints(ConstraintsState pConstraints) {
    Set<Constraint> relevantConstraints = new HashSet<>();
    if (performMinimalSatCheck && pConstraints.getLastAddedConstraint().isPresent()) {
      try {
        timeForIndependentComputation.start();
        Constraint lastConstraint = pConstraints.getLastAddedConstraint().get();
        // Always add the last added constraint to the set of relevant constraints.
        // It may not contain any symbolic identifiers (e.g., 0 == 5) and will thus
        // not be automatically included in the iteration over dependent sets below.
        relevantConstraints.add(lastConstraint);

        Set<Constraint> leftOverConstraints = new HashSet<>(pConstraints);
        Set<SymbolicIdentifier> newRelevantIdentifiers = lastConstraint.accept(locator);
        Set<SymbolicIdentifier> relevantIdentifiers;
        do {
          relevantIdentifiers = newRelevantIdentifiers;
          Iterator<Constraint> it = leftOverConstraints.iterator();
          while (it.hasNext()) {
            Constraint currentC = it.next();
            Set<SymbolicIdentifier> containedIdentifiers = currentC.accept(locator);
            if (!Sets.intersection(containedIdentifiers, relevantIdentifiers).isEmpty()) {
              newRelevantIdentifiers = Sets.union(newRelevantIdentifiers, containedIdentifiers);
              relevantConstraints.add(currentC);
              it.remove();
            }
          }
        } while (!newRelevantIdentifiers.equals(relevantIdentifiers));

      } finally {
        timeForIndependentComputation.stop();
      }

    } else {
      relevantConstraints = pConstraints;
    }

    return relevantConstraints;
  }

  private void closeProver() {
    if (prover != null) {
      prover.close();
      prover = null;
    }
  }

  private ImmutableCollection<ValueAssignment> resolveDefiniteAssignments(
      ConstraintsState pConstraints, List<ValueAssignment> pModel)
      throws InterruptedException, SolverException {
    try {
      timeForDefinitesComputation.start();

      return computeDefiniteAssignment(pConstraints, pModel);

    } finally {
      timeForDefinitesComputation.stop();
    }
  }

  private ImmutableCollection<ValueAssignment> computeDefiniteAssignment(
      ConstraintsState pState, List<ValueAssignment> pModel)
      throws SolverException, InterruptedException {

    ImmutableCollection<ValueAssignment> existingDefinites = pState.getDefiniteAssignment();
    ImmutableSet.Builder<ValueAssignment> newDefinites = ImmutableSet.builder();

    for (ValueAssignment val : pModel) {
      if (SymbolicValues.isSymbolicTerm(val.getName())
          && (existingDefinites.contains(val)
          || isOnlySatisfyingAssignment(val))) {
        newDefinites.add(val);
      }
    }
    return newDefinites.build();
  }

  private boolean isOnlySatisfyingAssignment(ValueAssignment pTerm)
      throws SolverException, InterruptedException {

    BooleanFormula prohibitAssignment =
        formulaManager.makeNot(pTerm.getAssignmentAsFormula());

    prohibitAssignment = createLiteralLabel(literalForSingleAssignment, prohibitAssignment);
    prover.push(prohibitAssignment);
    boolean isUnsat =
        prover.isUnsatWithAssumptions(Collections.singleton(literalForSingleAssignment));
    prover.pop();

    return isUnsat;
  }

  private FormulaCreator getFormulaCreator(String pFunctionName) {
    return new FormulaCreatorUsingCConverter(converter, pFunctionName);
  }

  /**
   * Returns the set of formulas representing all constraints of this state. If no
   * constraints exist, this method will return an empty set.
   *
   * @return the set of formulas representing all constraints of this state
   * @throws UnrecognizedCodeException see {@link FormulaCreator#createFormula(Constraint)}
   * @throws InterruptedException see {@link FormulaCreator#createFormula(Constraint)}
   */
  private Collection<BooleanFormula> getFullFormula(
      Collection<Constraint> pConstraints, String pFunctionName)
      throws UnrecognizedCodeException, InterruptedException {

    List<BooleanFormula> formulas = new ArrayList<>(pConstraints.size());
    for (Constraint c : pConstraints) {
      if (!constraintFormulas.containsKey(c)) {
        constraintFormulas.put(c, createConstraintFormulas(c, pFunctionName));
      }
      formulas.add(constraintFormulas.get(c));
    }

    return formulas;
  }

  private BooleanFormula createConstraintFormulas(
      Constraint pConstraint, String pFunctionName)
      throws UnrecognizedCodeException, InterruptedException {
    assert !constraintFormulas.containsKey(pConstraint)
        : "Trying to add a formula that already exists!";

    return getFormulaCreator(pFunctionName).createFormula(pConstraint);
  }

  @Override
  public void collectStatistics(Collection<Statistics> statsCollection) {
    statsCollection.add(
        new Statistics() {
          @Override
          public void printStatistics(
              PrintStream out, Result result, UnmodifiableReachedSet reached) {
            StatisticsWriter.writingStatisticsTo(out)
                .put(timeForSolving)
                .beginLevel()
                .put(timeForIndependentComputation)
                .put(timeForModelReuse)
                .put(timeForSatCheck)
                .put(timeForDefinitesComputation)
                .endLevel()
                .put(modelReuseSuccesses);
          }

          @Override
          public String getName() {
            return ConstraintsSolver.class.getSimpleName();
          }
        });

    if (cache instanceof StatisticsProvider) {
      ((StatisticsProvider) cache).collectStatistics(statsCollection);

    } else if (cache instanceof Statistics) {
      statsCollection.add((Statistics) cache);
    }
  }

  private interface ConstraintsCache {
    CacheResult getCachedResult(Collection<BooleanFormula> pConstraints);

    void addSat(
        Collection<BooleanFormula> pConstraints,
        ImmutableList<ValueAssignment> pModelAssignment);

    void addUnsat(Collection<BooleanFormula> pConstraints);
  }

  private static class MatchingConstraintsCache implements ConstraintsCache, Statistics {

    private Map<Collection<BooleanFormula>, CacheResult> cacheMap = new HashMap<>();

    private StatCounter cacheLookups = new StatCounter("Matching cache lookups");
    private StatCounter cacheHits = new StatCounter("Matching cache hits");
    private StatTimer lookupTime = new StatTimer("Matching cache lookup time");

    @Override
    public CacheResult getCachedResult(Collection<BooleanFormula> pConstraints) {
      try {
        cacheLookups.inc();
        lookupTime.start();
        if (cacheMap.containsKey(pConstraints)) {
          cacheHits.inc();
          return cacheMap.get(pConstraints);

        } else {
          return CacheResult.getUnknown();
        }
      } finally {
        lookupTime.stop();
      }
    }

    @Override
    public void addSat(
        Collection<BooleanFormula> pConstraints,
        ImmutableList<ValueAssignment> pModelAssignment) {
      add(pConstraints, CacheResult.getSat(pModelAssignment));
    }

    @Override
    public void addUnsat(Collection<BooleanFormula> pConstraints) {
      add(pConstraints, CacheResult.getUnsat());
    }

    private void add(Collection<BooleanFormula> pConstraints, CacheResult pResult) {
      cacheMap.put(pConstraints, pResult);
    }

    @Override
    public void printStatistics(
        PrintStream pOut, Result pResult, UnmodifiableReachedSet pReached) {
      StatisticsWriter.writingStatisticsTo(pOut)
          .put(cacheLookups)
          .put(cacheHits)
          .put(lookupTime);
    }

    @Nullable
    @Override
    public String getName() {
      return getClass().getSimpleName();
    }
  }

  private static class SubsetConstraintsCache implements ConstraintsCache, StatisticsProvider {

    private MatchingConstraintsCache delegate;

    /**
     * Multimap that maps each constraint to all sets of constraints that it occurred in
     */
    private Multimap<BooleanFormula, Set<BooleanFormula>> constraintContainedIn =
        HashMultimap.create();

    private StatCounter cacheLookups = new StatCounter("Cache lookups");
    private StatCounter directCacheHits = new StatCounter("Direct cache hits");
    private StatCounter cacheHits = new StatCounter("Subset cache hits");
    private StatTimer lookupTime = new StatTimer(StatKind.SUM, "Subset lookup time");

    public SubsetConstraintsCache() {
      delegate = new MatchingConstraintsCache();
    }

    @Override
    public CacheResult getCachedResult(Collection<BooleanFormula> pConstraints) {
      try {
        cacheLookups.inc();
        lookupTime.start();
        CacheResult res = delegate.getCachedResult(pConstraints);
        if (!res.isSat() && !res.isUnsat()) {
          res = getCachedResultOfSubset(pConstraints);
          if (res.isSat() || res.isUnsat()) {
            cacheHits.inc();
          }
        } else {
          directCacheHits.inc();
        }
        return res;
      } finally {
        lookupTime.stop();
      }
    }

    @Override
    public void addSat(
        Collection<BooleanFormula> pConstraints, ImmutableList<ValueAssignment> pModelAssignment) {
      add(pConstraints);
      delegate.addSat(pConstraints, pModelAssignment);
    }

    @Override
    public void addUnsat(Collection<BooleanFormula> pConstraints) {
      add(pConstraints);
      delegate.addUnsat(pConstraints);
    }

    private void add(Collection<BooleanFormula> pConstraints) {
      for (BooleanFormula c : pConstraints) {
        constraintContainedIn.put(c, ImmutableSet.copyOf(pConstraints));
      }
    }

    CacheResult getCachedResultOfSubset(Collection<BooleanFormula> pConstraints) {
      checkState(!pConstraints.isEmpty());

      Set<Set<BooleanFormula>> containAllConstraints = null;
      for (BooleanFormula c : pConstraints) {
        Set<Set<BooleanFormula>> containC = ImmutableSet.copyOf(constraintContainedIn.get(c));
        if (containAllConstraints == null) {
          containAllConstraints = containC;
        } else {
          containAllConstraints = Sets.intersection(containAllConstraints, containC);
        }

        if (containAllConstraints.isEmpty()) {
          return CacheResult.getUnknown();
        }
      }

      checkNotNull(containAllConstraints);
      int sizeOfQuery = pConstraints.size();
      for (Set<BooleanFormula> col : containAllConstraints) {
        CacheResult cachedResult = delegate.getCachedResult(col);
        if (sizeOfQuery <= col.size() && cachedResult.isSat()) {
          // currently considered collection is a superset of the queried collection
          return cachedResult;

        } else if (sizeOfQuery >= col.size() && cachedResult.isUnsat()) {
          // currently considered collection is a subset of the queried collection
          return cachedResult;
        }
      }
      return CacheResult.getUnknown();
    }

    @Override
    public void collectStatistics(Collection<Statistics> statsCollection) {
      statsCollection.add(
          new Statistics() {
            @Override
            public void printStatistics(
                PrintStream pOut, Result pResult, UnmodifiableReachedSet pReached) {
              StatisticsWriter.writingStatisticsTo(pOut)
                  .put(cacheLookups)
                  .put(directCacheHits)
                  .put(cacheHits)
                  .put(lookupTime);
            }

            @Nullable
            @Override
            public String getName() {
              return ConstraintsSolver.class.getSimpleName();
            }
          });

      statsCollection.add(delegate);
    }
  }

  private static class DummyCache implements ConstraintsCache {

    @Override
    public CacheResult getCachedResult(Collection<BooleanFormula> pConstraints) {
      return CacheResult.getUnknown();
    }

    @Override
    public void addSat(
        Collection<BooleanFormula> pConstraints,
        ImmutableList<ValueAssignment> pModelAssignment) {
      // do nothing
    }

    @Override
    public void addUnsat(Collection<BooleanFormula> pConstraints) {
      // do nothing
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
        new CacheResult(Result.UNKNOWN, Optional.empty());

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
