package org.sosy_lab.cpachecker.cpa.stator.policy;

import org.sosy_lab.cpachecker.util.predicates.interfaces.NumeralFormula;
import org.sosy_lab.cpachecker.util.predicates.interfaces.NumeralFormulaManager;

/**
 * Class for dealing with fresh variables.
 */
public class FreshVariable {
  public static final String FRESH_VAR_PREFIX = "POLICY_ITERATION_FRESH_VAR_%d";

  // NOTE: hm what if it overflows?
  // wouldn't it be safer to use a BigInteger here?
  private static long fresh_var_counter = 0;

  final long no;
  final NumeralFormula variable;

  private FreshVariable(long no, NumeralFormula variable) {
    this.no = no;
    this.variable = variable;
  }

  String name() {
    return name(no);
  }

  static String name(long counter) {
    return String.format(FRESH_VAR_PREFIX, counter);
  }

  /**
   * @return Unique fresh variable created using a global counter.
   */
  static FreshVariable createFreshVar(
      NumeralFormulaManager<NumeralFormula, NumeralFormula.RationalFormula> rfmgr
  ) {
    FreshVariable out = new FreshVariable(
        fresh_var_counter,
        rfmgr.makeVariable(FreshVariable.name(fresh_var_counter))
    );
    fresh_var_counter++;
    return out;
  }
}
