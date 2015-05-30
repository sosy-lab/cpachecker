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
package org.sosy_lab.cpachecker.cpa.constraints;

import static com.google.common.base.Preconditions.checkNotNull;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.sosy_lab.cpachecker.core.counterexample.Model;
import org.sosy_lab.cpachecker.core.defaults.LatticeAbstractState;
import org.sosy_lab.cpachecker.cpa.constraints.constraint.Constraint;
import org.sosy_lab.cpachecker.cpa.constraints.constraint.IdentifierAssignment;
import org.sosy_lab.cpachecker.cpa.value.symbolic.type.SymbolicIdentifier;
import org.sosy_lab.cpachecker.cpa.value.type.BooleanValue;
import org.sosy_lab.cpachecker.cpa.value.type.NumericValue;
import org.sosy_lab.cpachecker.cpa.value.type.Value;
import org.sosy_lab.cpachecker.exceptions.SolverException;
import org.sosy_lab.cpachecker.exceptions.UnrecognizedCCodeException;
import org.sosy_lab.cpachecker.util.predicates.AssignableTerm;
import org.sosy_lab.cpachecker.util.predicates.Solver;
import org.sosy_lab.cpachecker.util.predicates.TermType;
import org.sosy_lab.cpachecker.util.predicates.interfaces.BooleanFormula;
import org.sosy_lab.cpachecker.util.predicates.interfaces.Formula;
import org.sosy_lab.cpachecker.util.predicates.interfaces.ProverEnvironment;
import org.sosy_lab.cpachecker.util.predicates.interfaces.view.FormulaManagerView;

import com.google.common.collect.Lists;

/**
 * State for Constraints Analysis. Stores constraints and whether they are solvable.
 */
public class ConstraintsState implements LatticeAbstractState<ConstraintsState>, Set<Constraint> {

  /**
   * Stores identifiers and their corresponding constraints
   */
  private List<Constraint> constraints;
  private Map<Constraint, BooleanFormula> constraintFormulas;

  private Solver solver;
  private ProverEnvironment prover;
  private FormulaCreator formulaCreator;
  private FormulaManagerView formulaManager;

  private IdentifierAssignment definiteAssignment;

  private LessOrEqualOperator lessOrEqualOperator = LessOrEqualOperator.getInstance();

  /**
   * Creates a new, initial <code>ConstraintsState</code> object.
   */
  public ConstraintsState() {
    constraints = new ArrayList<>();
    constraintFormulas = new HashMap<>();
    definiteAssignment = new IdentifierAssignment();
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

    definiteAssignment = new IdentifierAssignment(pState.definiteAssignment);
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

  protected FormulaCreator getFormulaCreator() {
    return formulaCreator;
  }

  /**
   * Join this <code>ConstraintsState</code> with the given one.
   *
   * Join is defined as the intersection of both states.
   *
   * @param other the state to join this state with
   *
   * @return the join (that is, intersection) of both states
   */
  @Override
  public ConstraintsState join(ConstraintsState other) {
    ConstraintsState newState = new ConstraintsState();

    if (constraints.size() < other.size()) {
      for (Constraint c : constraints) {
        if (other.contains(c)) {
          newState.add(c);
        }
      }
    } else {
      for (Constraint c : other) {
        if (constraints.contains(c)) {
          newState.add(c);
        }
      }
    }

    return newState;
  }

  /**
   * Returns whether this state is less or equal than another given state.
   *
   * <p>A <code>ConstraintsState</code> is less or equal another given state, if it is a super set of it.</p>
   *
   * @param other the other state to check against
   * @return <code>true</code> if this state is less or equal than the given state, <code>false</code> otherwise
   */
  @Override
  public boolean isLessOrEqual(ConstraintsState other) {
    /* Alternative method: A state s is less or equal another states s', if s' implies s.

    if (solver == null) {
      // an uninitialized solver means there can be no constraints
      return true;
    }

    BooleanFormula thisRepresentingFormula = getFullFormula();
    BooleanFormula otherRepresentingFormula = other.getFullFormula();

    return solver.implies(otherRepresentingFormula, thisRepresentingFormula);*/

    return lessOrEqualOperator.isLessOrEqual(this, other);
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
    formulaCreator = pFormulaCreator;
  }

  /**
   * Returns whether this state is unsatisfiable.
   * A state without constraints (that is, an empty state), is always satisfiable.
   *
   * @return <code>true</code> if this state is unsatisfiable, <code>false</code> otherwise
   * @throws SolverException
   * @throws InterruptedException
   */
  public boolean isUnsat() throws SolverException, InterruptedException, UnrecognizedCCodeException {
    boolean unsat = false;

    try {
      if (!constraints.isEmpty()) {
        prover = solver.newProverEnvironmentWithModelGeneration();
        BooleanFormula constraintsAsFormula = getFullFormula();

        prover.push(constraintsAsFormula);
        unsat = prover.isUnsat();

        if (!unsat) {
          // doing this while the complete formula is still on the prover environment stack is
          // cheaper than performing another complete SAT check when the assignment is really requested
          computeDefiniteAssignment(constraintsAsFormula);

        } else {
          definiteAssignment = null;
        }

      }
    } finally {
      closeProver();
    }

    return unsat;
  }

  private void closeProver() {
    if (prover != null) {
      prover.close();
      prover = null;
    }
  }

  private void computeDefiniteAssignment(BooleanFormula pFormula) throws SolverException, InterruptedException, UnrecognizedCCodeException {
    Model validAssignment = prover.getModel();

    for (Map.Entry<AssignableTerm, Object> entry : validAssignment.entrySet()) {
      AssignableTerm term = entry.getKey();
      Object termAssignment = entry.getValue();

      if (isSymbolicTerm(term)) {
        SymbolicIdentifier identifier = toSymbolicIdentifier(
            FormulaManagerView.parseName(term.getName()).getFirst());
        Value concreteValue = convertToValue(termAssignment, term.getType());

        if (!definiteAssignment.containsKey(identifier)
            && isOnlySatisfyingAssignment(term, termAssignment, pFormula)) {

          assert !definiteAssignment.containsKey(identifier) || definiteAssignment.get(identifier).equals(concreteValue)
              : "Definite assignment can't be changed from " + definiteAssignment.get(identifier) + " to " + concreteValue;

          definiteAssignment.put(identifier, concreteValue);
        }
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

  private boolean isSymbolicTerm(AssignableTerm pTerm) {
    return SymbolicIdentifier.Converter.getInstance().isSymbolicEncoding(pTerm.getName());
  }

  private boolean isOnlySatisfyingAssignment(AssignableTerm pTerm, Object termAssignment, BooleanFormula pFormula)
      throws SolverException, InterruptedException, UnrecognizedCCodeException {

    VariableMap freeVariables = new VariableMap(formulaManager.extractFreeVariableMap(pFormula));
    BooleanFormula prohibitAssignment = formulaManager
                                       .makeNot(formulaCreator.transformAssignment(pTerm, termAssignment, freeVariables));

    prover.push(prohibitAssignment);
    boolean isUnsat = prover.isUnsat();

    // remove the just added formula again so we return to the original constraint formula
    // - other assignments will probably be tested before closing prover.
    prover.pop();

    return isUnsat;
  }

  private SymbolicIdentifier toSymbolicIdentifier(String pEncoding) {
    return SymbolicIdentifier.Converter.getInstance().convert(pEncoding);
  }

  private Value convertToValue(Object pTermAssignment, TermType pType) {
    if (pType.equals(TermType.Integer) || pType.equals(TermType.Bitvector)
        || pType.equals(TermType.FloatingPoint) || pType.equals(TermType.Real)) {
      assert pTermAssignment instanceof Number;

      return new NumericValue((Number) pTermAssignment);

    } else if (pType.equals(TermType.Boolean)) {
      return BooleanValue.valueOf(true);

    } else {
      throw new AssertionError("Unexpected type " + pType);
    }
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
  private BooleanFormula getFullFormula() throws UnrecognizedCCodeException, InterruptedException {
    createMissingConstraintFormulas();

    return formulaManager.getBooleanFormulaManager().and(Lists.newArrayList(constraintFormulas.values()));
  }

  private void createMissingConstraintFormulas() throws UnrecognizedCCodeException, InterruptedException {
    assert constraints.size() >= constraintFormulas.size()
        : "More formulas than constraints!";

    int missingConstraints = constraints.size() - constraintFormulas.size();

    for (int i = constraints.size() - missingConstraints; i < constraints.size(); i++) {
      Constraint newConstraint = constraints.get(i);
      assert !constraintFormulas.containsKey(newConstraint)
          : "Trying to add a formula that already exists!";

      constraintFormulas.put(newConstraint, formulaCreator.createFormula(newConstraint));
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
      Constraint constraintToRemove = constraints.get(index);

      constraints.remove(index);
      constraintFormulas.remove(constraintToRemove);
    }
  }
}
