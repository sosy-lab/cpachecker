/*
 * CPAchecker is a tool for configurable software verification.
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
package org.sosy_lab.cpachecker.cpa.constraints.domain;

import static com.google.common.base.Preconditions.checkNotNull;
import static com.google.common.base.Preconditions.checkState;

import com.google.common.base.Joiner;
import com.google.common.collect.HashMultimap;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Multimap;
import com.google.common.collect.Sets;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Optional;
import java.util.Set;
import org.sosy_lab.cpachecker.core.interfaces.AbstractState;
import org.sosy_lab.cpachecker.core.interfaces.Graphable;
import org.sosy_lab.cpachecker.cpa.constraints.FormulaCreator;
import org.sosy_lab.cpachecker.cpa.constraints.constraint.Constraint;
import org.sosy_lab.cpachecker.cpa.constraints.constraint.IdentifierAssignment;
import org.sosy_lab.cpachecker.cpa.value.symbolic.type.SymbolicIdentifier;
import org.sosy_lab.cpachecker.cpa.value.symbolic.util.SymbolicIdentifierLocator;
import org.sosy_lab.cpachecker.cpa.value.symbolic.util.SymbolicValues;
import org.sosy_lab.cpachecker.cpa.value.type.Value;
import org.sosy_lab.cpachecker.exceptions.UnrecognizedCCodeException;
import org.sosy_lab.cpachecker.util.Pair;
import org.sosy_lab.cpachecker.util.predicates.smt.BooleanFormulaManagerView;
import org.sosy_lab.cpachecker.util.predicates.smt.FormulaManagerView;
import org.sosy_lab.cpachecker.util.predicates.smt.Solver;
import org.sosy_lab.java_smt.api.BooleanFormula;
import org.sosy_lab.java_smt.api.Formula;
import org.sosy_lab.java_smt.api.Model;
import org.sosy_lab.java_smt.api.Model.ValueAssignment;
import org.sosy_lab.java_smt.api.ProverEnvironment;
import org.sosy_lab.java_smt.api.SolverContext.ProverOptions;
import org.sosy_lab.java_smt.api.SolverException;

/**
 * State for Constraints Analysis. Stores constraints and whether they are solvable.
 */
public class ConstraintsState implements AbstractState, Graphable, Set<Constraint> {

  /**
   * Stores identifiers and their corresponding constraints
   */
  private List<Constraint> constraints;

  /**
   * The last constraint added to this state. This does not have to be the last constraint in
   * {@link #constraints}.
   */
  // It does not have to be the last constraint contained in 'constraints' because we only
  // add a constraint to 'constraints' if it's not yet in this list.
  private Constraint lastAddedConstraint;
  private Map<Constraint, BooleanFormula> constraintFormulas;

  private Solver solver;
  private ProverEnvironment prover;
  private FormulaCreator formulaCreator;
  private FormulaManagerView formulaManager;
  private BooleanFormulaManagerView booleanFormulaManager;
  private SymbolicIdentifierLocator locator;

  private IdentifierAssignment definiteAssignment;
  private BooleanFormula lastModel;
  private ImmutableList<ValueAssignment> lastModelAsAssignment;

  private static ConstraintsCache cache = new ConstraintsCache();

  /**
   * Creates a new, initial <code>ConstraintsState</code> object.
   */
  public ConstraintsState() {
    this(Collections.emptySet(), IdentifierAssignment.empty());
  }

  public ConstraintsState(
      final Set<Constraint> pConstraints,
      final IdentifierAssignment pDefiniteAssignment
  ) {
    constraints = new ArrayList<>(pConstraints);
    definiteAssignment = new IdentifierAssignment(pDefiniteAssignment);
    constraintFormulas = new HashMap<>();
    locator = SymbolicIdentifierLocator.getInstance();
  }

  /**
   * Creates a new <code>ConstraintsState</code> copy of the given <code>ConstraintsState</code>.
   * The returned copy will use the same references to {@link Solver} and {@link ProverEnvironment}
   * currently stored in the given state.
   * To use new ones, {@link #initialize(Solver, FormulaManagerView, FormulaCreator)} may be
   * called on the returned state.
   *
   * <p>This constructor should only be used by {@link #copyOf()} and subtypes of this class.</p>
   *
   * @param pState the state to copy
   */
  protected ConstraintsState(ConstraintsState pState) {
    constraints = new ArrayList<>(pState.constraints);
    constraintFormulas = new HashMap<>(pState.constraintFormulas);
    solver = pState.solver;
    prover = pState.prover;
    formulaCreator = pState.formulaCreator;
    formulaManager = pState.formulaManager;
    booleanFormulaManager = pState.booleanFormulaManager;
    locator = pState.locator;

    lastAddedConstraint = pState.lastAddedConstraint;
    definiteAssignment = new IdentifierAssignment(pState.definiteAssignment);
    lastModel = pState.lastModel;
  }

  /**
   * Returns a new copy of the given <code>ConstraintsState</code> object.
   * The returned state is always uninitialized.
   *
   * @return a new copy of the given <code>ConstraintsState</code> object
   * @see #isInitialized()
   * @see #initialize(Solver, FormulaManagerView, FormulaCreator)
   */
  // We use a method here so subtypes can override it, in contrast to a public copy constructor
  public ConstraintsState copyOf() {
    return new ConstraintsState(this);
  }

  /**
   * Adds the given {@link Constraint} to this state.
   *
   * @param pConstraint the <code>Constraint</code> to add
   * @return <code>true</code> if this state did not already contain the given <code>Constraint</code>,
   *    <code>false</code> otherwise
   */
  @Override
  public boolean add(Constraint pConstraint) {
    checkNotNull(pConstraint);

    lastAddedConstraint = pConstraint;
    return !constraints.contains(pConstraint) && constraints.add(pConstraint);
  }

  @Override
  public boolean remove(Object pObject) {
    boolean changed = constraints.remove(pObject);

    if (changed) {
      constraintFormulas.remove(pObject);
      assert constraints.size() >= constraintFormulas.size();
    }

    return changed;
  }

  Constraint getLastAddedConstraint() {
    return checkNotNull(lastAddedConstraint);
  }

  @Override
  public boolean containsAll(Collection<?> pCollection) {
    return constraints.containsAll(pCollection);
  }

  @Override
  public boolean addAll(Collection<? extends Constraint> pCollection) {
    boolean changed = false;
    for (Constraint c : pCollection) {
      changed |= add(c);
    }

    return changed;
  }

  @Override
  public boolean retainAll(Collection<?> pCollection) {
    List<Constraint> constraintsCopy = new ArrayList<>(constraints);
    boolean changed = false;

    for (Constraint c : constraintsCopy) {
      if (!pCollection.contains(c)) {
        changed |= remove(c);
      }
    }

    assert constraints.size() >= constraintFormulas.size();
    return changed;
  }

  @Override
  public boolean removeAll(Collection<?> pCollection) {
    boolean changed = false;

    for (Object o : pCollection) {
      changed |= remove(o);
    }

    assert constraints.size() >= constraintFormulas.size();
    return changed;
  }

  @Override
  public void clear() {
    constraints.clear();
    constraintFormulas.clear();
  }

  @Override
  public int size() {
    return constraints.size();
  }

  @Override
  public boolean isEmpty() {
    return constraints.isEmpty();
  }

  @Override
  public boolean contains(Object o) {
    return constraints.contains(o);
  }

  /**
   * Returns whether this state is initialized.
   * If a state is not initialized, calls to {@link #isUnsat()} will fail with an exception.
   *
   * <p>A state will never be initialized upon creation.
   * It can be initialized by calling {@link #initialize(Solver, FormulaManagerView, FormulaCreator)}.</p>
   *
   * @return <code>true</code> if the state is initialized.
   */
  public boolean isInitialized() {
    return solver != null;
  }

  /**
   * Initializes this state with the given objects. After initializing, SAT checks can be performed on this state's
   * constraints by calling {@link #isUnsat()}.
   *
   * @param pSolver the solver to use for SAT checks.
   * @param pFormulaManager the formula manager to use for creating {@link Formula}s
   * @param pFormulaCreator the formula creator to use for creating <code>Formula</code>s
   */
  public void initialize(Solver pSolver, FormulaManagerView pFormulaManager, FormulaCreator pFormulaCreator) {
    solver = pSolver;
    formulaManager = pFormulaManager;
    booleanFormulaManager = formulaManager.getBooleanFormulaManager();
    lastModel = booleanFormulaManager.makeTrue();
    formulaCreator = pFormulaCreator;
  }

  /**
   * Returns whether this state is unsatisfiable.
   * A state without constraints (that is, an empty state), is always satisfiable.
   *
   * @return <code>true</code> if this state is unsatisfiable, <code>false</code> otherwise
   */
  public boolean isUnsat() throws SolverException, InterruptedException, UnrecognizedCCodeException {

    if (!constraints.isEmpty()) {
      try {
        BooleanFormula constraintsAsFormula = getFullFormula();
        prover = solver.newProverEnvironment(ProverOptions.GENERATE_MODELS);
        prover.push(constraintsAsFormula);

        prover.push(lastModel);
        boolean unsat = prover.isUnsat();

        if (!booleanFormulaManager.isTrue(lastModel)) {
          // if the last model does not fulfill the formula, and the last model actually
          // is some variable assignment (i.e., model != true), then we check the formula
          // for satisfiability without any assignments, again.
          prover.pop(); // Remove model assignment from prover

          if (unsat) {
            CacheResult res = cache.getCachedResult(constraints);

            if (res.isUnsat()) {
              unsat = true;
            } else if (res.isSat()) {
              unsat = false;
              lastModel = res.getModel();
              lastModelAsAssignment = res.getModelAssignment();
            } else {
              unsat = prover.isUnsat();
            }
          }
        }

        if (!unsat) {
          lastModelAsAssignment = prover.getModelAssignments();
          lastModel =
              lastModelAsAssignment
                  .stream()
                  .map(ValueAssignment::getAssignmentAsFormula)
                  .collect(booleanFormulaManager.toConjunction());
          cache.addSat(constraints, lastModelAsAssignment, lastModel);
          // doing this while the complete formula is still on the prover environment stack is
          // cheaper than performing another complete SAT check when the assignment is really
          // requested
          resolveDefiniteAssignments();
        } else {
          lastModel = null;
          definiteAssignment = null;
          cache.addUnsat(constraints);
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

  private void resolveDefiniteAssignments()
      throws InterruptedException, SolverException, UnrecognizedCCodeException {

    IdentifierAssignment oldDefinites = new IdentifierAssignment(definiteAssignment);
    computeDefiniteAssignment();
    updateOldFormulasDefinitesAppearIn(oldDefinites, definiteAssignment);
    assert definiteAssignment.entrySet().containsAll(oldDefinites.entrySet());
  }

  private void computeDefiniteAssignment() throws SolverException, InterruptedException {
    try (Model validAssignment = prover.getModel()) {
      for (ValueAssignment val : validAssignment) {
        if (SymbolicValues.isSymbolicTerm(val.getName())) {

          SymbolicIdentifier identifier =
              SymbolicValues.convertTermToSymbolicIdentifier(val.getName());
          Value concreteValue = SymbolicValues.convertToValue(val);

          if (!definiteAssignment.containsKey(identifier)
              && isOnlySatisfyingAssignment(val)) {

            assert !definiteAssignment.containsKey(identifier) || definiteAssignment.get(identifier).equals(concreteValue)
                : "Definite assignment can't be changed from " + definiteAssignment.get(identifier) + " to " + concreteValue;

            definiteAssignment.put(identifier, concreteValue);
          }
        }
      }
    }
  }

  private void updateOldFormulasDefinitesAppearIn(
      final IdentifierAssignment pOldDefinites,
      final IdentifierAssignment pNewDefinites
  ) throws UnrecognizedCCodeException, InterruptedException {
    assert pOldDefinites.size() <= pNewDefinites.size();

    // if no new definite assignments were added, we don't have to remove any formula
    if (pOldDefinites.size() == pNewDefinites.size()) {
      return;
    }

    Set<SymbolicIdentifier> newlyKnownIdentifiers = new HashSet<>(pNewDefinites.keySet());

    newlyKnownIdentifiers.removeAll(pOldDefinites.keySet());

    // for each constraint a formula exists for, we check if the formula can be replaced
    // with a version holding more information, and do so.
    for (Entry<Constraint, BooleanFormula> entry : constraintFormulas.entrySet()) {
      Set<SymbolicIdentifier> identifiers = entry.getKey().accept(locator);

      // if the constraint contains any identifier we now know a definite assignment for,
      // we replace the constraint's formula by a new formula using these definite assignments.
      if (!Collections.disjoint(newlyKnownIdentifiers, identifiers)) {
        BooleanFormula newFormula = formulaCreator.createFormula(entry.getKey(), pNewDefinites);

        assert !newFormula.equals(entry.getValue())
            || formulaManager.getBooleanFormulaManager().isTrue(entry.getValue())
            : "Identifier was not replaced by definite assignment";

        entry.setValue(newFormula);
      }
    }
  }

  /**
   * Returns the known unambigious assignment of variables so this state's {@link Constraint}s are fulfilled.
   * Variables that can have more than one valid assignment are not included in the
   * returned {@link IdentifierAssignment}.
   *
   * @return the known assignment of variables that have no other fulfilling assignment
   */
  public IdentifierAssignment getDefiniteAssignment() {
    return new IdentifierAssignment(definiteAssignment);
  }

  public List<ValueAssignment> getModel()
      throws InterruptedException, SolverException, UnrecognizedCCodeException {
    if (lastModelAsAssignment == null) {
      checkState(!isUnsat());
    }

    if (lastModelAsAssignment == null) {
      return Collections.emptyList();
    } else {
      return lastModelAsAssignment;
    }
  }

  private boolean isOnlySatisfyingAssignment(ValueAssignment pTerm)
      throws SolverException, InterruptedException {

    BooleanFormula prohibitAssignment = formulaManager.makeNot(formulaCreator.transformAssignment(pTerm.getKey(), pTerm.getValue()));

    prover.push(prohibitAssignment);
    boolean isUnsat = prover.isUnsat();

    // remove the just added formula again so we return to the original constraint formula
    // - other assignments will probably be tested before closing prover.
    prover.pop();

    return isUnsat;
  }

  /**
   * Returns the formula representing the conjunction of all constraints of this state.
   * If no constraints exist, this method will return <code>null</code>.
   *
   * @return the formula representing the conjunction of all constraints of this state
   *
   * @throws UnrecognizedCCodeException see {@link FormulaCreator#createFormula(Constraint)}
   * @throws InterruptedException see {@link FormulaCreator#createFormula(Constraint)}
   */
  BooleanFormula getFullFormula() throws UnrecognizedCCodeException, InterruptedException {
    createMissingConstraintFormulas();

    return booleanFormulaManager.and(constraintFormulas.values());
  }

  private void createMissingConstraintFormulas() throws UnrecognizedCCodeException, InterruptedException {
    assert constraints.size() >= constraintFormulas.size()
        : "More formulas than constraints!";

    int missingConstraints = constraints.size() - constraintFormulas.size();

    for (int i = constraints.size() - missingConstraints; i < constraints.size(); i++) {
      Constraint newConstraint = constraints.get(i);
      assert !constraintFormulas.containsKey(newConstraint)
          : "Trying to add a formula that already exists!";

      BooleanFormula newFormula = formulaCreator.createFormula(newConstraint, definiteAssignment);
      constraintFormulas.put(newConstraint, newFormula);
    }

    assert constraints.size() == constraintFormulas.size()
        : "More constraints than formulas!";
  }

  @Override
  public Iterator<Constraint> iterator() {
    return new ConstraintIterator();
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }

    ConstraintsState that = (ConstraintsState) o;

    return constraints.equals(that.constraints) && definiteAssignment.equals(that.definiteAssignment);
  }

  @Override
  public int hashCode() {
    int result = constraints.hashCode();
    result = 31 * result + definiteAssignment.hashCode();
    return result;
  }

  @Override
  public Object[] toArray() {
    return constraints.toArray();
  }

  @Override
  public <T> T[] toArray(T[] pTs) {
    return constraints.toArray(pTs);
  }

  @Override
  public String toString() {
    StringBuilder sb = new StringBuilder("[");

    for (Constraint currConstraint : constraints) {
      sb.append(" <");
      sb.append(currConstraint);
      sb.append(">\n");
    }

    return sb.append("] size->  ").append(constraints.size()).toString();
  }

  @Override
  public String toDOTLabel() {
    StringBuilder sb = new StringBuilder();

    sb.append("[");
    Joiner.on(", ").appendTo(sb, constraints);
    sb.append("]");

    return sb.toString();
  }

  @Override
  public boolean shouldBeHighlighted() {
    return false;
  }

  private class ConstraintIterator implements Iterator<Constraint> {

    private int index = -1;

    @Override
    public boolean hasNext() {
      return constraints.size() - 1 > index;
    }

    @Override
    public Constraint next() {
      index++;
      return constraints.get(index);
    }

    @Override
    public void remove() {
      if (index < 0) {
        throw new IllegalStateException("Iterator not at valid location");
      }

      Constraint constraintToRemove = constraints.get(index);

      constraints.remove(index);
      constraintFormulas.remove(constraintToRemove);
      index--;
    }
  }

  private static class ConstraintsCache {

    private Map<Integer, CacheResult> cacheMap = new HashMap<>();

    /**
     * Multimap that maps each constraint to all collections of constraints that it occurred in,
     * where each collection of constraints is represented by a pair &lt;id, size&gt;
     */
    private Multimap<Constraint, Pair<Integer, Integer>> constraintContainedIn =
        HashMultimap.create();

    CacheResult getCachedResult(Collection<Constraint> pConstraints) {
      int id = getId(pConstraints);
      if (cacheMap.containsKey(id)) {
        return cacheMap.get(id);
      } else {
        return getCachedResultOfSubset(pConstraints);
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
        CacheResult cachedResult = cacheMap.get(idOfCollection);
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

    private int getId(Collection<Constraint> pConstraints) {
      int id = 0;
      // do this manually to make sure that we get the same id independent of the data structure
      // used
      for (Constraint c : pConstraints) {
        id += c.hashCode();
      }
      return id;
    }

    void addSat(
        Collection<Constraint> pConstraints,
        ImmutableList<ValueAssignment> pModelAssignment,
        BooleanFormula pModel) {
      add(pConstraints, CacheResult.getSat(pModelAssignment, pModel));
    }

    void addUnsat(Collection<Constraint> pConstraints) {
      add(pConstraints, CacheResult.getUnsat());
    }

    private void add(Collection<Constraint> pConstraints, CacheResult pResult) {
      int id = getId(pConstraints);
      Pair<Integer, Integer> idAndSize = Pair.of(id, pConstraints.size());
      for (Constraint c : pConstraints) {
        constraintContainedIn.put(c, idAndSize);
      }
      cacheMap.put(id, pResult);
    }
  }

  private static class CacheResult {
    enum Result {
      SAT,
      UNSAT,
      UNKNOWN
    }

    private static final CacheResult UNSAT_SINGLETON =
        new CacheResult(Result.UNSAT, Optional.empty(), Optional.empty());
    private static final CacheResult UNKNOWN_SINGLETON =
        new CacheResult(Result.UNSAT, Optional.empty(), Optional.empty());

    private Result result;
    private Optional<BooleanFormula> model;
    private Optional<ImmutableList<ValueAssignment>> modelAssignment;

    public static CacheResult getSat(
        ImmutableList<ValueAssignment> pModelAssignment, BooleanFormula pModel) {
      return new CacheResult(Result.SAT, Optional.of(pModelAssignment), Optional.of(pModel));
    }

    public static CacheResult getUnsat() {
      return UNSAT_SINGLETON;
    }

    public static CacheResult getUnknown() {
      return UNKNOWN_SINGLETON;
    }

    private CacheResult(
        Result pResult,
        Optional<ImmutableList<ValueAssignment>> pModelAssignment,
        Optional<BooleanFormula> pModel) {
      result = pResult;
      modelAssignment = pModelAssignment;
      model = pModel;
    }

    public boolean isSat() {
      return result.equals(Result.SAT);
    }

    public boolean isUnsat() {
      return result.equals(Result.UNSAT);
    }

    public BooleanFormula getModel() {
      checkState(model.isPresent(), "No model exists");
      return model.get();
    }

    public ImmutableList<ValueAssignment> getModelAssignment() {
      checkState(modelAssignment.isPresent(), "No model assignment exists");
      return modelAssignment.get();
    }
  }
}
