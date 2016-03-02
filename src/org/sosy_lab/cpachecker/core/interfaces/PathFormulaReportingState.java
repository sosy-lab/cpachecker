package org.sosy_lab.cpachecker.core.interfaces;

import org.sosy_lab.cpachecker.util.predicates.pathformula.PathFormula;
import org.sosy_lab.cpachecker.util.predicates.pathformula.SSAMap;
import org.sosy_lab.cpachecker.util.predicates.smt.FormulaManagerView;

/**
 * Report the over-approximation of the abstract state using the formula
 * language.
 *
 * Similar to {@link FormulaReportingState}, but uses {@link PathFormula} instead.
 */
public interface PathFormulaReportingState extends AbstractState{
  /**
   *
   * @param manager Manager used to create the formulas.
   * @param outMap {@link SSAMap} on the output constraints.
   * @param inputMap {@link SSAMap} on the input constraints.
   * @return Formula together with an updated output {@link SSAMap}
   */
  public PathFormula getFormulaApproximation(
      FormulaManagerView manager, SSAMap outMap, SSAMap inputMap);

}
