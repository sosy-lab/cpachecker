package org.sosy_lab.cpachecker.util.predicates.mathsat5;

import static org.sosy_lab.cpachecker.util.predicates.mathsat5.Mathsat5FormulaManager.getMsatTerm;
import static org.sosy_lab.cpachecker.util.predicates.mathsat5.Mathsat5NativeApi.*;

import java.util.ArrayList;
import java.util.Deque;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import org.sosy_lab.common.rationals.Rational;
import org.sosy_lab.cpachecker.exceptions.SolverException;
import org.sosy_lab.cpachecker.util.UniqueIdGenerator;
import org.sosy_lab.cpachecker.util.predicates.Model;
import org.sosy_lab.cpachecker.util.predicates.interfaces.BooleanFormula;
import org.sosy_lab.cpachecker.util.predicates.interfaces.Formula;
import org.sosy_lab.cpachecker.util.predicates.interfaces.OptEnvironment;

import com.google.common.base.Optional;
import com.google.common.base.Verify;
import com.google.common.collect.ImmutableMap;

class Mathsat5OptProver  extends Mathsat5AbstractProver implements OptEnvironment{
  private UniqueIdGenerator idGenerator = new UniqueIdGenerator();

  /**
   * Number of the objective -> objective pointer.
   */

  private List<Long> objectives = null;

  /**
   * ID given to user -> number of the objective.
   * Size corresponds to the number of currently existing objectives.
   */
  private Map<Integer, Integer> objectiveMap;

  /**
   * Stack of the objective maps.
   * Some duplication, but shouldn't be too important.
   */
  private Deque<ImmutableMap<Integer, Integer>> stack;

  private int pushCount = 0;
  private int popCount = 0;

  Mathsat5OptProver(Mathsat5FormulaManager pMgr,
      long pConfig) {
    super(pMgr, pConfig, true, false);
    objectiveMap = new HashMap<>();
    stack = new LinkedList<>();
  }

  @Override
  public void addConstraint(BooleanFormula constraint) {
    msat_assert_formula(curEnv, getMsatTerm(constraint));
  }

  @Override
  public int maximize(Formula objective) {
    // todo: code duplication.
    int id = idGenerator.getFreshId();
    objectiveMap.put(id, objectiveMap.size());
    msat_push_maximize(
        curEnv, getMsatTerm(objective), null, null
    );
    return id;
  }


  @Override
  public int minimize(Formula objective) {
    int id = idGenerator.getFreshId();
    objectiveMap.put(id, objectiveMap.size());
    msat_push_minimize(
        curEnv, getMsatTerm(objective), null, null
    );
    return id;
  }

  @Override
  public OptStatus check()
      throws InterruptedException, SolverException {
    boolean out = msat_check_sat(curEnv);
    if (out) {
      if (!objectiveMap.isEmpty()) {
        objectives = new ArrayList<>();
        long it = msat_create_objective_iterator(curEnv);

        while (msat_objective_iterator_has_next(it) != 0) {
          long[] objectivePtr = new long[1];
          int status = msat_objective_iterator_next(it, objectivePtr);
          assert status == 0;
          objectives.add(objectivePtr[0]);
        }
      }
      return OptStatus.OPT;
    } else {
      return OptStatus.UNSAT;
    }
  }

  @Override
  public void push() {
    pushCount++;
    msat_push_backtrack_point(curEnv);
    stack.add(ImmutableMap.copyOf(objectiveMap));
  }

  @Override
  public void pop() {
    popCount++;
    msat_pop_backtrack_point(curEnv);
    objectiveMap = new HashMap<>(stack.pop());
  }

  @Override
  public Optional<Rational> upper(int handle, Rational epsilon) {
    return getValue(handle);
  }

  @Override
  public Optional<Rational> lower(int handle, Rational epsilon) {
    return getValue(handle);
  }

  private Optional<Rational> getValue(int handle) {
    // todo: use epsilon if the bound is non-strict.

    long objective = objectives.get(objectiveMap.get(handle));
    int isUnbounded = msat_objective_value_is_unbounded(curEnv, objective, MSAT_OPTIMUM);
    if (isUnbounded == 1) {
      return Optional.absent();
    }
    assert isUnbounded == 0;
    String objectiveValue = msat_objective_value_repr(curEnv, objective, MSAT_OPTIMUM);
    return Optional.of(Rational.ofString(objectiveValue));
  }

  @Override
  public Model getModel() throws SolverException {
    msat_set_model(curEnv, objectives.get(objectiveMap.size() - 1));
    return super.getModel();
  }

  @Override
  public Formula evaluate(Formula f) {
    throw new UnsupportedOperationException("Mathsat solver does not support evaluation");
  }

  @Override
  public String dump() {
    throw new UnsupportedOperationException("Mathsat solver does not constraint dumping");
  }

  @Override
  public void close() {
    Verify.verify(pushCount == popCount, "Global environment has to be left "
        + "in the consistent state.");
    super.close();
  }
}
