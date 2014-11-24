package org.sosy_lab.cpachecker.cpa.stator.policy;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.logging.Level;

import org.sosy_lab.common.log.LogManager;
import org.sosy_lab.cpachecker.core.counterexample.Model;
import org.sosy_lab.cpachecker.exceptions.SolverException;
import org.sosy_lab.cpachecker.util.predicates.interfaces.BooleanFormula;
import org.sosy_lab.cpachecker.util.predicates.interfaces.Formula;
import org.sosy_lab.cpachecker.util.predicates.interfaces.NumeralFormula;
import org.sosy_lab.cpachecker.util.predicates.interfaces.OptEnvironment;
import org.sosy_lab.cpachecker.util.predicates.interfaces.view.FormulaManagerView;
import org.sosy_lab.cpachecker.util.predicates.interfaces.view.NumeralFormulaManagerView;
import org.sosy_lab.cpachecker.util.predicates.pathformula.SSAMap;
import org.sosy_lab.cpachecker.util.rationals.ExtendedRational;
import org.sosy_lab.cpachecker.util.rationals.LinearConstraint;
import org.sosy_lab.cpachecker.util.rationals.LinearExpression;
import org.sosy_lab.cpachecker.util.rationals.Rational;

/**
 * Converting linear constraints to formulas.
 *
 * Note that since {@link ExtendedRational} can be <code>NaN</code>
 * this class can throw exceptions when converting linear constraints to
 * formulas.
 */
public class LinearConstraintManager {

  private final NumeralFormulaManagerView<
        NumeralFormula, NumeralFormula.RationalFormula> rfmgr;
  private final LogManager logger;
  private final FormulaManagerView fmgr;

  /**
   * Something which is bigger then MAX_INT/MAX_FLOW (or whatever domain we
   * are working with).
   */
  private final Rational BAZILLION = Rational.ofString(
      "100000000");
  private final FreshVariableManager freshVariableManager;

  LinearConstraintManager(
      FormulaManagerView pFmgr,
      LogManager logger,
      FreshVariableManager pFreshVariableManager
      ) {
    fmgr = pFmgr;
    rfmgr = pFmgr.getRationalFormulaManager();
    this.logger = logger;
    freshVariableManager = pFreshVariableManager;
  }

  /**
   * @param constraint Constraint to convert.
   * @param pSSAMap Map which contains the versioning index for each variable.
   * @return formula which can be passed to a solver.
   */
  BooleanFormula linearConstraintToFormula(
      LinearConstraint constraint, SSAMap pSSAMap) {

      return rfmgr.lessOrEquals(
          linearExpressionToFormula(constraint.getExpression(), pSSAMap),
          rfmgr.makeNumber(constraint.getBound().toString())
      );
  }

  /**
   * @param expr Linear expression to convert.
   * @param pSSAMap Version number for each variable.
   *
   * @return NumeralFormula for the SMT solver.
   * @throws UnsupportedOperationException if the conversion can not be
   * performed.
   */
  NumeralFormula linearExpressionToFormula(
      LinearExpression expr, SSAMap pSSAMap)
      throws UnsupportedOperationException {
    return linearExpressionToFormula(expr, pSSAMap, "");
  }

  /**
   * @param expr Linear expression to convert.
   * @param pSSAMap Version number for each variable.
   * @param customPrefix Custom string prefix to add before each variable.
   *
   * @return NumeralFormula for the SMT solver.
   * @throws UnsupportedOperationException if the conversion can not be
   * performed.
   */
  NumeralFormula linearExpressionToFormula(
      LinearExpression expr, SSAMap pSSAMap, String customPrefix
  ) {

    NumeralFormula sum = null;
    for (Map.Entry<String, Rational> entry : expr) {
      Rational coeff = entry.getValue();
      String origVarName = entry.getKey();

      // SSA index shouldn't be zero.
      int idx = Math.max(pSSAMap.getIndex(origVarName), 1);

      NumeralFormula item = rfmgr.makeVariable(customPrefix + origVarName, idx);

      if (coeff.equals(Rational.ZERO)) {
        continue;
      } else if (coeff.equals(Rational.NEG_ONE)) {
        item = rfmgr.negate(item);
      } else if (!coeff.equals(Rational.ONE)){
        item = rfmgr.multiply(
            item, rfmgr.makeNumber(entry.getValue().toString()));
      }

      if (sum == null) {
        sum = item;
      } else {
        sum = rfmgr.add(sum, item);
      }
    }

    if (sum == null) {
      return rfmgr.makeNumber(0);
    } else {
      return sum;
    }
  }

  /**
   * Maximizes a list of input objectives, provided they are
   * <i>independent</i> of each other -- that is maximizing
   * their sum is equivalent to maximizing them separately.
   * Assumes that the set of constraints on the solver is satisfiable.
   *
   * @return Mapping from the input formulas to the resulting maximized values.
   */
  public Map<NumeralFormula, ExtendedRational> maximizeObjectives(
      OptEnvironment prover,
      List<NumeralFormula> objectives)
    throws SolverException, InterruptedException {

    Map<String, NumeralFormula> input = new HashMap<>();
    Map<NumeralFormula, ExtendedRational> out = new HashMap<>();

    for (NumeralFormula formula : objectives) {
      NumeralFormula.RationalFormula target = freshVariableManager.freshRationalVar();
      prover.addConstraint(rfmgr.equal(target, formula));

      // Enforce bounds for all variables.
      prover.addConstraint(
          rfmgr.lessOrEquals(target, rfmgr.makeNumber(BAZILLION.toString()))
      );
      input.put(target.toString(), formula);
    }

    NumeralFormula sum = rfmgr.sum(objectives);
    NumeralFormula.RationalFormula target = freshVariableManager.freshRationalVar();
    prover.addConstraint(rfmgr.equal(sum, target));
    prover.maximize(target);
    OptEnvironment.OptStatus status = prover.check();

    switch (status) {
      case OPT:
        Model model = prover.getModel();
        logger.log(Level.FINEST, "OPT");
        logger.log(Level.FINEST, "Model = ", model);
        for (Map.Entry<String, NumeralFormula> e : input.entrySet()) {
          String varName = e.getKey();
          NumeralFormula formula = e.getValue();
          Rational r = rationalFromModel(model, varName);
          ExtendedRational eOut;
          if (r.equals(BAZILLION)) {
            eOut = ExtendedRational.INFTY;
          } else {
            eOut = new ExtendedRational(r);
          }
          out.put(formula, eOut);
        }
        return out;
      default:
        throw new SolverException("Solver in the inconsistent state, aborting");
    }
  }

  /**
   * @param prover Prover engine used
   * @param expression Expression to maximize
   * @return Returned value in the extended rational field.
   *
   * @throws SolverException, InterruptedException
   */
  ExtendedRational maximize(
      OptEnvironment prover, LinearExpression expression, SSAMap pSSAMap
  ) throws SolverException, InterruptedException {

    NumeralFormula objective = linearExpressionToFormula(expression, pSSAMap);
    logger.log(Level.FINE, "MAXIMIZING for objective: ", objective);

    return maximize(prover, objective);
  }

  /**
   *  Lower-level API allowing one to provide the actual formula.
   */
  ExtendedRational maximize(
      OptEnvironment prover, NumeralFormula objective
  ) throws SolverException, InterruptedException {

    // We can only maximize a single variable.
    // Create a new variable, make it equal to the linear expression which we
    // have.
    NumeralFormula.RationalFormula target = freshVariableManager.freshRationalVar();

    // Hack, Z3 does not seem to work well with unbounded
    // objectives, so we simply constraint the result above by BAZILLION
    // (a sufficiently large number), and if the output number is equal to
    // the BAZILLION we say that the result is unbounded.
    prover.addConstraint(
        rfmgr.lessOrEquals(target, rfmgr.makeNumber(BAZILLION.toString()))
    );

    prover.addConstraint(rfmgr.equal(target, objective));
    prover.maximize(target);

    OptEnvironment.OptStatus result = prover.check();

    switch (result) {
      case OPT:
        Model model = prover.getModel();
        logger.log(Level.FINEST, "OPT");
        logger.log(Level.FINEST, "Model = ", model);
        return new ExtendedRational(rationalFromModel(model, target.toString()));
      case UNSAT:
        logger.log(Level.FINEST, "UNSAT");
        return ExtendedRational.NEG_INFTY;
      case UNBOUNDED:
        logger.log(Level.FINEST, "UNBOUNDED");
        return ExtendedRational.INFTY;
      case UNDEF:
        logger.log(Level.FINEST, "UNDEFINED");
        throw new SolverException("Result undefined: something is wrong");
      default:
        logger.log(Level.FINEST, "ERROR RUNNING OPTIMIZATION");
        throw new RuntimeException("Internal Error, unaccounted case");
    }
  }

  public Rational rationalFromModel(Model model, String varName) {
    return (Rational) model.get(
        new Model.Constant(varName, Model.TermType.Real)
    );
  }

  /**
   * @return Subset of <code>origConstraints</code> containing only the formula
   * related to (possibly indirectly) <code>relatedTo</code>.
   */
  @SuppressWarnings("unused")
  List<BooleanFormula> getRelated(
      List<BooleanFormula> origConstraints,
      Formula relatedTo
  ) {
    // TODO: can be useful for octagon computation.
    List<BooleanFormula> toProcess = new ArrayList<>(origConstraints);
    List<BooleanFormula> newToProcess = new ArrayList<>();

    Set<String> careAbout = fmgr.extractVariableNames(relatedTo);
    final List<BooleanFormula> related = new ArrayList<>();
    Set<String> newCareAbout = new HashSet<>(careAbout);

    // Fix-point computation to find out all the related constraints.
    while (true) {
      for (BooleanFormula f : toProcess) {
        Set<String> containedVars = fmgr.extractVariableNames(f);
        Set<String> intersection = new HashSet<>(containedVars);

        intersection.retainAll(careAbout);
        if (intersection.size() > 0) {
          newCareAbout.addAll(containedVars);
          related.add(f);
        } else {
          newToProcess.add(f);
        }
      }

      if (newCareAbout.equals(careAbout)) {
        break;
      } else {
        toProcess = new ArrayList<>(newToProcess);
        careAbout = new HashSet<>(newCareAbout);
        newCareAbout = new HashSet<>(careAbout);
        newToProcess = new ArrayList<>();
      }
    }
    return related;
  }
}
