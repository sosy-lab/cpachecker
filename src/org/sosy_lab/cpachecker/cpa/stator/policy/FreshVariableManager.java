package org.sosy_lab.cpachecker.cpa.stator.policy;

import java.math.BigInteger;

import org.sosy_lab.cpachecker.util.predicates.interfaces.BooleanFormula;
import org.sosy_lab.cpachecker.util.predicates.interfaces.BooleanFormulaManager;
import org.sosy_lab.cpachecker.util.predicates.interfaces.NumeralFormula;
import org.sosy_lab.cpachecker.util.predicates.interfaces.NumeralFormulaManager;

/**
 * Class for creating with fresh variables.
 */
public class FreshVariableManager {
  private static final String OPT_PREFIX = "PI_OPT_%s";
  private static final String BOOL_TEST_SETTING = "PI_T_%s";

  private static BigInteger numeral_counter = BigInteger.ZERO;
  private static BigInteger bool_test_counter = BigInteger.ZERO;

  private final NumeralFormulaManager<NumeralFormula, NumeralFormula.RationalFormula> rfmgr;
  private final BooleanFormulaManager bfmgr;

  public FreshVariableManager(
      NumeralFormulaManager<NumeralFormula, NumeralFormula.RationalFormula> pRfmgr,
      BooleanFormulaManager pBfmgr) {
    rfmgr = pRfmgr;
    bfmgr = pBfmgr;
  }

  /**
   * @return Fresh rational variable with a unique name.
   */
  public NumeralFormula.RationalFormula freshRationalVar() {
    numeral_counter = numeral_counter.add(BigInteger.ONE);
    return rfmgr.makeVariable(String.format(OPT_PREFIX, numeral_counter));
  }

  /**
   * @return Fresh boolean variable with a unique name.
   */
  public BooleanFormula freshBooleanVar() {
    bool_test_counter = bool_test_counter.add(BigInteger.ONE);
    return bfmgr.makeVariable(String.format(BOOL_TEST_SETTING, bool_test_counter));
  }
}
