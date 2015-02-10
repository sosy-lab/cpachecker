package org.sosy_lab.cpachecker.cpa.policyiteration;

import java.math.BigInteger;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.sosy_lab.cpachecker.core.counterexample.Model;
import org.sosy_lab.cpachecker.util.predicates.interfaces.BooleanFormula;
import org.sosy_lab.cpachecker.util.predicates.interfaces.BooleanFormulaManager;
import org.sosy_lab.cpachecker.util.predicates.interfaces.Formula;
import org.sosy_lab.cpachecker.util.predicates.interfaces.NumeralFormula.IntegerFormula;
import org.sosy_lab.cpachecker.util.predicates.interfaces.UnsafeFormulaManager;
import org.sosy_lab.cpachecker.util.predicates.interfaces.view.FormulaManagerView;
import org.sosy_lab.cpachecker.util.predicates.interfaces.view.NumeralFormulaManagerView;

import com.google.common.collect.ImmutableMap;

public class FormulaLinearizationManager {
  private final UnsafeFormulaManager ufmgr;
  private final BooleanFormulaManager bfmgr;
  private final FormulaManagerView fmgr;
  private final NumeralFormulaManagerView<IntegerFormula, IntegerFormula> ifmgr;

  private static final String NOT_FUNC_NAME = "not";

  private static final String EQ_FUNC_NAME = "=";
  private static final String OR_FUNC_NAME = "or";

  public static final String CHOICE_VAR_NAME = "__POLICY_CHOICE_";


  // TODO: code duplication.
  private static final String INITIAL_CONDITION_FLAG = "__INITIAL_CONDITION_TRUE";

  private int choiceVarCounter = -1;

  public FormulaLinearizationManager(UnsafeFormulaManager pUfmgr,
      BooleanFormulaManager pBfmgr, FormulaManagerView pFmgr,
      NumeralFormulaManagerView<IntegerFormula, IntegerFormula> pIfmgr) {
    ufmgr = pUfmgr;
    bfmgr = pBfmgr;
    fmgr = pFmgr;
    ifmgr = pIfmgr;
  }

  /**
   * Convert non-concave statements into disjunctions.
   *
   * At the moment handles:
   *
   *  x NOT(EQ(A, B)) => (A > B) \/ (A < B)
   */
  public BooleanFormula linearize(BooleanFormula input) {
    return (BooleanFormula)recLinearize(input, new HashMap<Formula, Formula>());
  }

  // TODO: stop code duplication.
  private Formula recLinearize(final Formula input,
      Map<Formula, Formula> memoization) {
    Formula out = memoization.get(input);
    if (out != null) {
      return out;
    }

    // Pattern matching.
    boolean isNot = ufmgr.getName(input).equals(NOT_FUNC_NAME);

    if (isNot && ufmgr.getName(ufmgr.getArg(input, 0)).equals(EQ_FUNC_NAME)) {
      Formula child = ufmgr.getArg(input, 0);
      Formula lhs = ufmgr.getArg(child, 0);
      Formula rhs = ufmgr.getArg(child, 1);

      out = bfmgr.or(
          fmgr.makeGreaterThan(lhs, rhs, true),
          fmgr.makeLessThan(lhs, rhs, true));
    } else {
      List<Formula> newArgs = new ArrayList<>();
      for (int i=0; i<ufmgr.getArity(input); i++) {
        newArgs.add(
            recLinearize(
                ufmgr.getArg(input, i), memoization
            )
        );
      }

      out = ufmgr.replaceArgs(input, newArgs);
    }

    memoization.put(input, out);
    return out;
  }

  /**
   * Annotate disjunctions with choice variables.
   */
  public BooleanFormula annotateDisjunctions(BooleanFormula input) {
    return (BooleanFormula)recAnnotateDisjunction(
        input, new HashMap<Formula, Formula>());
  }

  private Formula recAnnotateDisjunction(Formula input,
      Map<Formula, Formula> memoization) {
    Formula out = memoization.get(input);
    if (out != null) {
      return out;
    }

    BooleanFormula specVar = bfmgr.not(bfmgr.makeVariable(INITIAL_CONDITION_FLAG));

    if (ufmgr.getName(input).equals(OR_FUNC_NAME)) {
      // Hacky way not to annotate the formulas I create.
      if (ufmgr.getArity(input) == 2 &&
          (ufmgr.getArg(input, 0).equals(specVar)
              || ufmgr.getArg(input, 1).equals(specVar))) {
        out = input;

      } else {

        String freshVarName = CHOICE_VAR_NAME + (++choiceVarCounter);
        IntegerFormula var = ifmgr.makeVariable(freshVarName);
        List<Formula> newArgs = new ArrayList<>();
        for (int choice = 0; choice < ufmgr.getArity(input); choice++) {
          newArgs.add(
              bfmgr.and(
                  (BooleanFormula)
                      recAnnotateDisjunction(ufmgr.getArg(input, choice),
                          memoization),
                  fmgr.makeEqual(var, ifmgr.makeNumber(choice))
              )
          );
        }
        out = ufmgr.replaceArgs(input, newArgs);
      }

    } else {
      List<Formula> newArgs = new ArrayList<>();
      for (int i=0; i<ufmgr.getArity(input); i++) {
        newArgs.add(
            recAnnotateDisjunction(
                ufmgr.getArg(input, i), memoization));
      }
      out = ufmgr.replaceArgs(input, newArgs);
    }

    memoization.put(input, out);
    return out;
  }

  /**
   * Removes disjunctions from the {@code input} formula, by replacing them
   * with arguments which were used to generate the {@code model}.
   */
  public BooleanFormula enforceChoice(
      BooleanFormula input,
      Iterable<Map.Entry<Model.AssignableTerm, Object>> model,
      boolean useInitialValue
  ) {

    Map<Formula, Formula> mapping = ImmutableMap.of(
        (Formula)bfmgr.makeVariable(INITIAL_CONDITION_FLAG),
        (Formula)bfmgr.makeBoolean(useInitialValue)
    );
    BooleanFormula initialSelected = ufmgr.substitute(input, mapping);

    mapping = new HashMap<>();
    for (Map.Entry<Model.AssignableTerm, Object> entry : model) {
      String termName = entry.getKey().getName();

      if (termName.contains(CHOICE_VAR_NAME)) {
        BigInteger value = (BigInteger) entry.getValue();
        mapping.put(
            ifmgr.makeVariable(termName),
            ifmgr.makeNumber(value)
        );
      }
    }
    BooleanFormula pathSelected = ufmgr.substitute(initialSelected, mapping);
    pathSelected = fmgr.simplify(pathSelected);
    return pathSelected;
  }


}
