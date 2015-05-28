package org.sosy_lab.cpachecker.util.predicates.mathsat5;

import static org.sosy_lab.cpachecker.util.predicates.mathsat5.Mathsat5FormulaManager.getMsatTerm;
import static org.sosy_lab.cpachecker.util.predicates.mathsat5.Mathsat5NativeApi.*;

import java.util.ArrayList;
import java.util.List;

import org.sosy_lab.common.rationals.Rational;
import org.sosy_lab.cpachecker.core.counterexample.Model;
import org.sosy_lab.cpachecker.exceptions.SolverException;
import org.sosy_lab.cpachecker.util.predicates.interfaces.BooleanFormula;
import org.sosy_lab.cpachecker.util.predicates.interfaces.Formula;
import org.sosy_lab.cpachecker.util.predicates.interfaces.OptEnvironment;

import com.google.common.base.Optional;

class Mathsat5OptProver  extends Mathsat5AbstractProver implements OptEnvironment{
  private int noObjectives = 0;
  private List<Long> objectives = null;

  Mathsat5OptProver(Mathsat5FormulaManager pMgr,
      long pConfig) {
    super(pMgr, setModel(pConfig), true, false);
  }

  private static long setModel(long pConfig) {
    msat_set_option_checked(pConfig, "model_generation", "true");
    return pConfig;
  }

  @Override
  public void addConstraint(BooleanFormula constraint) {
    msat_assert_formula(curEnv, getMsatTerm(constraint));
  }

  @Override
  public int maximize(Formula objective) {
    msat_push_maximize(
        curEnv, getMsatTerm(objective), null, null
    );
    return noObjectives++;
  }


  @Override
  public int minimize(Formula objective) {
    msat_push_minimize(
        curEnv, getMsatTerm(objective), null, null
    );
    return noObjectives++;
  }

  @Override
  public OptStatus check()
      throws InterruptedException, SolverException {
    boolean out = msat_check_sat(curEnv);

    if (out) {
      objectives = new ArrayList<>();
      long it = msat_create_objective_iterator(curEnv);

      while (msat_objective_iterator_has_next(it) != 0) {
        long[] objectivePtr = new long[1];
        int status = msat_objective_iterator_next(it, objectivePtr);
        assert status == 0;
        objectives.add(objectivePtr[0]);
      }
      return OptStatus.OPT;
    } else {
      return OptStatus.UNSAT;
    }
  }

  @Override
  public void push() {
    // todo
  }

  @Override
  public void pop() {
    // todo
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
    // todo: use epsilon.
    long objective = objectives.get(handle);
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
    msat_set_model(curEnv, objectives.get(noObjectives-1));
    return super.getModel();
  }

  @Override
  public Formula evaluate(Formula f) {
    throw new UnsupportedOperationException("Mathsat solver does not support evaluation");
  }
}
