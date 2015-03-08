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

import java.util.HashSet;
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
import org.sosy_lab.cpachecker.util.predicates.Solver;
import org.sosy_lab.cpachecker.util.predicates.interfaces.BooleanFormula;
import org.sosy_lab.cpachecker.util.predicates.interfaces.BooleanFormulaManager;
import org.sosy_lab.cpachecker.util.predicates.interfaces.Formula;
import org.sosy_lab.cpachecker.util.predicates.interfaces.ProverEnvironment;
import org.sosy_lab.cpachecker.util.predicates.interfaces.view.FormulaManagerView;

import com.google.common.collect.ForwardingSet;

/**
 * State for Constraints Analysis. Stores constraints and whether they are solvable.
 */
public class ConstraintsState extends ForwardingSet<Constraint> implements LatticeAbstractState<ConstraintsState> {

  /**
   * Stores identifiers and their corresponding constraints
   */
  private Set<Constraint> constraints;

  private Solver solver;
  private ProverEnvironment prover;
  private FormulaCreator formulaCreator;
  private FormulaManagerView formulaManager;

  private IdentifierAssignment definiteAssignment = new IdentifierAssignment();

  /**
   * Creates a new, initial <code>ConstraintsState</code> object.
   */
  public ConstraintsState() {
    constraints = new HashSet<>();
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
    constraints = new HashSet<>(pState.constraints);
    solver = pState.solver;
    prover = pState.prover;
    formulaCreator = pState.formulaCreator;
    formulaManager = pState.formulaManager;

    definiteAssignment = new IdentifierAssignment(pState.definiteAssignment);
  }

  @Override
  protected Set<Constraint> delegate() {
    return constraints;
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
    if (other.size() > constraints.size()) {
      return false;
    }

    for (Constraint c : other) {
      if (!constraints.contains(c)) {
        return false;
      }
    }

    return true;
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
    return super.add(pConstraint);
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

    for (Map.Entry<Model.AssignableTerm, Object> entry : validAssignment.entrySet()) {
      Model.AssignableTerm term = entry.getKey();
      Object termAssignment = entry.getValue();

      if (isSymbolicTerm(term)) {

        SymbolicIdentifier identifier = toSymbolicIdentifier(term.getName());
        Value concreteValue = convertToValue(termAssignment, term.getType());

        assert !definiteAssignment.containsKey(identifier) || definiteAssignment.get(identifier).equals(concreteValue);
        if (!definiteAssignment.containsKey(identifier)
            && isOnlySatisfyingAssignment(term, termAssignment, pFormula)) {

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

  private boolean isSymbolicTerm(Model.AssignableTerm pTerm) {
    return SymbolicIdentifier.Converter.getInstance().isSymbolicEncoding(pTerm.getName());
  }

  private boolean isOnlySatisfyingAssignment(Model.AssignableTerm pTerm, Object termAssignment, BooleanFormula pFormula)
      throws SolverException, InterruptedException, UnrecognizedCCodeException {

    VariableMap freeVariables = new VariableMap(formulaManager.extractFreeVariableMap(pFormula));
    BooleanFormula prohibitAssignment = solver.getFormulaManager()
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

  private Value convertToValue(Object pTermAssignment, Model.TermType pType) {
    if (pType.equals(Model.TermType.Integer) || pType.equals(Model.TermType.Bitvector)
        || pType.equals(Model.TermType.FloatingPoint) || pType.equals(Model.TermType.Real)) {
      assert pTermAssignment instanceof Number;

      return new NumericValue((Number) pTermAssignment);

    } else if (pType.equals(Model.TermType.Boolean)) {
      return BooleanValue.valueOf(true);

    } else {
      throw new AssertionError("Unexpected type " + pType);
    }
  }

  private BooleanFormula getFullFormula() throws UnrecognizedCCodeException, InterruptedException {
      BooleanFormula completeFormula = null;
      BooleanFormula currFormula;

      for (Constraint currConstraint : constraints) {

        currFormula = formulaCreator.createFormula(currConstraint);

        if (completeFormula == null) {
          completeFormula = currFormula;

        } else {
          completeFormula = formulaManager.makeAnd(completeFormula, currFormula);
        }
      }

      if (completeFormula == null) {
        final BooleanFormulaManager manager = formulaManager.getBooleanFormulaManager();

        completeFormula = manager.makeBoolean(true);
      }

      return completeFormula;
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
}
