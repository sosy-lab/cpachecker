package org.sosy_lab.cpachecker.cpa.policyiteration;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.sosy_lab.common.log.LogManager;
import org.sosy_lab.cpachecker.util.predicates.interfaces.BooleanFormula;
import org.sosy_lab.cpachecker.util.predicates.interfaces.Formula;
import org.sosy_lab.cpachecker.util.predicates.interfaces.NumeralFormula;
import org.sosy_lab.cpachecker.util.predicates.interfaces.view.FormulaManagerView;
import org.sosy_lab.cpachecker.util.predicates.interfaces.view.NumeralFormulaManagerView;
import org.sosy_lab.cpachecker.util.predicates.pathformula.SSAMap;
import org.sosy_lab.cpachecker.util.rationals.ExtendedRational;
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

  @SuppressWarnings({"unused", "FieldCanBeLocal"})
  private final LogManager logger;
  private final FormulaManagerView fmgr;

  LinearConstraintManager(
      FormulaManagerView pFmgr,
      LogManager pLogger) {
    fmgr = pFmgr;
    rfmgr = pFmgr.getRationalFormulaManager();
    logger = pLogger;
  }

  /**
   * @param expr Linear expression to convert.
   * @param pSSAMap Version number for each variable.
   *
   * @return NumeralFormula for the SMT solver.
   */
  NumeralFormula linearExpressionToFormula(
      LinearExpression expr, SSAMap pSSAMap) {
    return linearExpressionToFormula(expr, pSSAMap, "");
  }

  /**
   * @param expr Linear expression to convert.
   * @param pSSAMap Version number for each variable.
   * @param customPrefix Custom string prefix to add before each variable.
   *
   * @return NumeralFormula for the SMT solver.
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

      if (coeff == Rational.ZERO) {
        continue;
      } else if (coeff == Rational.NEG_ONE) {
        item = rfmgr.negate(item);
      } else if (coeff != Rational.ONE){
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
