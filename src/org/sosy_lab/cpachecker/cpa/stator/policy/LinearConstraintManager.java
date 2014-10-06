package org.sosy_lab.cpachecker.cpa.stator.policy;

import java.util.HashMap;
import java.util.HashSet;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.logging.Level;

import org.sosy_lab.common.log.LogManager;
import org.sosy_lab.cpachecker.core.counterexample.Model;
import org.sosy_lab.cpachecker.exceptions.SolverException;
import org.sosy_lab.cpachecker.util.predicates.FormulaManagerFactory;
import org.sosy_lab.cpachecker.util.predicates.interfaces.BooleanFormula;
import org.sosy_lab.cpachecker.util.predicates.interfaces.Formula;
import org.sosy_lab.cpachecker.util.predicates.interfaces.NumeralFormula;
import org.sosy_lab.cpachecker.util.predicates.interfaces.OptEnvironment;
import org.sosy_lab.cpachecker.util.predicates.interfaces.ProverEnvironment;
import org.sosy_lab.cpachecker.util.predicates.interfaces.view.BooleanFormulaManagerView;
import org.sosy_lab.cpachecker.util.predicates.interfaces.view.FormulaManagerView;
import org.sosy_lab.cpachecker.util.predicates.interfaces.view.NumeralFormulaManagerView;
import org.sosy_lab.cpachecker.util.predicates.pathformula.SSAMap;
import org.sosy_lab.cpachecker.util.rationals.ExtendedRational;
import org.sosy_lab.cpachecker.util.rationals.LinearConstraint;
import org.sosy_lab.cpachecker.util.rationals.LinearExpression;

import com.google.common.base.Preconditions;

/**
 * Wrapper for a linear constraint:
 *
 * Note that since {@link ExtendedRational} can be <code>NaN</code>
 * this class can throw exceptions when converting linear constraints to
 * formulas.
 */
public class LinearConstraintManager {

  private final BooleanFormulaManagerView bfmgr;
  private final NumeralFormulaManagerView<
        NumeralFormula, NumeralFormula.RationalFormula> rfmgr;
  private final LogManager logger;
  private final FormulaManagerView fmgr;
  private final FormulaManagerFactory factory;

  // Something which is bigger then any reasonable value.
  private final ExtendedRational BAZILLION = ExtendedRational.ofString(
      "100000000");

  LinearConstraintManager(
      FormulaManagerView pFmgr,
      FormulaManagerFactory factory,
      LogManager logger
      ) {
    fmgr = pFmgr;
    this.factory = factory;
    bfmgr = pFmgr.getBooleanFormulaManager();
    rfmgr = pFmgr.getRationalFormulaManager();
    this.logger = logger;
  }

  /**
   * @param constraint Constraint to convert.
   * @param pSSAMap Map which contains the versioning index for each variable.
   * @return formula which can be passed to a solver.
   */
  BooleanFormula linearConstraintToFormula(
      LinearConstraint constraint, SSAMap pSSAMap) {

    Preconditions.checkState(
        constraint.getBound().getType() != ExtendedRational.NumberType.NaN,
        "Constraints can not contain the number NaN"
    );

    switch (constraint.getBound().getType()) {
       case NEG_INFTY:
        return bfmgr.makeBoolean(false);
      case INFTY:
        return bfmgr.makeBoolean(true);
      case RATIONAL:
        return rfmgr.lessOrEquals(
            linearExpressionToFormula(constraint.getExpression(), pSSAMap),
            rfmgr.makeNumber(constraint.getBound().toString())
        );
      default:
        throw new RuntimeException(
            "Internal Error, unexpected formula");
    }
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
    for (Map.Entry<String, ExtendedRational> entry : expr) {
      ExtendedRational coeff = entry.getValue();
      String origVarName = entry.getKey();

      // SSA index shouldn't be zero.
      int idx = Math.max(pSSAMap.getIndex(origVarName), 1);

      NumeralFormula item = rfmgr.makeVariable(customPrefix + origVarName, idx);

      if (coeff.getType() != ExtendedRational.NumberType.RATIONAL) {
        throw new UnsupportedOperationException(
            "Can not convert the expression " + expr);
      } else if (coeff.equals(ExtendedRational.ZERO)) {
        continue;
      } else if (coeff.equals(ExtendedRational.NEG_ONE)) {
        item = rfmgr.negate(item);
      } else if (!coeff.equals(ExtendedRational.ONE)){
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
      return rfmgr.makeNumber("0");
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
      FreshVariable target = FreshVariable.createFreshVar(rfmgr);
      prover.addConstraint(rfmgr.equal(target.variable, formula));

      // Enforce bounds for all variables.
      prover.addConstraint(
          rfmgr.lessOrEquals(target.variable, rfmgr.makeNumber(BAZILLION.toString()))
      );
      input.put(target.name(), formula);
    }

    NumeralFormula sum = rfmgr.sum(objectives);
    FreshVariable target = FreshVariable.createFreshVar(rfmgr);
    prover.addConstraint(rfmgr.equal(sum, target.variable));
    prover.setObjective(target.variable);
    OptEnvironment.OptResult status = prover.maximize();

    switch (status) {
      case OPT:
        Model model = prover.getModel();
        logger.log(Level.FINEST, "OPT");
        logger.log(Level.FINEST, "Model = ", model);
        for (Map.Entry<String, NumeralFormula> e : input.entrySet()) {
          String varName = e.getKey();
          NumeralFormula formula = e.getValue();
          out.put(formula, rationalFromModel(model, varName));
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
    FreshVariable target = FreshVariable.createFreshVar(rfmgr);

    // TODO: A very dirty hack, Z3 does not seem to work well with unbounded
    // objectives, so we simply constraint the result above by BAZILLION
    // (a sufficiently large number), and if the output number is equal to
    // the BAZILLION we say that the result is unbounded.
    prover.addConstraint(
        rfmgr.lessOrEquals(target.variable, rfmgr.makeNumber(BAZILLION.toString()))
    );

    prover.addConstraint(rfmgr.equal(target.variable, objective));
    prover.setObjective(target.variable);

    OptEnvironment.OptResult result = prover.maximize();

    switch (result) {
      case OPT:
        Model model = prover.getModel();
        logger.log(Level.FINEST, "OPT");
        logger.log(Level.FINEST, "Model = ", model);
        return rationalFromModel(model, target.name());
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

  private ExtendedRational rationalFromModel(Model model, String varName) {
    ExtendedRational returned = (ExtendedRational) model.get(
        new Model.Constant(varName, Model.TermType.Real)
    );
    if (returned.equals(BAZILLION)) return ExtendedRational.INFTY;
    return returned;
  }

  /**
   * Provide a subset of original constraints which will give the same result
   * after maximization as the original set.
   */
  @SuppressWarnings("unused")
  public List<BooleanFormula> optCore(
      List<BooleanFormula> origConstraints,
      NumeralFormula objective,
      ExtendedRational maxValue,
      boolean isInteger
      ) {
    try (ProverEnvironment prover = factory.newProverEnvironment(true, true)) {
      for (BooleanFormula constraint : origConstraints) {
        prover.push(constraint);
      }

      if (isInteger) {

        // Should be more numerically stable.
        prover.push(rfmgr.greaterOrEquals(
            objective, rfmgr.makeNumber(
            maxValue.plus(ExtendedRational.ONE).toString())
        ));
      } else {
        prover.push(rfmgr.greaterThan(
            objective, rfmgr.makeNumber(maxValue.toString())
        ));
      }

      return prover.getUnsatCore();
    }
  }

  /**
   * @return Subset of {@param origConstraints} containing only the formula
   * related to (possibly indirectly) {@param relatedTo}.
   */
  @SuppressWarnings("unused")
  List<BooleanFormula> getRelated(
      List<BooleanFormula> origConstraints,
      Formula relatedTo
  ) {
    List<BooleanFormula> toProcess = new ArrayList<>(origConstraints);
    List<BooleanFormula> newToProcess = new ArrayList<>();

    Set<String> careAbout = fmgr.extractVariables(relatedTo);
    final List<BooleanFormula> related = new ArrayList<>();
    Set<String> newCareAbout = new HashSet<>(careAbout);

    // Fix-point computation to find out all the related constraints.
    while (true) {
      for (BooleanFormula f : toProcess) {
        Set<String> containedVars = fmgr.extractVariables(f);
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
