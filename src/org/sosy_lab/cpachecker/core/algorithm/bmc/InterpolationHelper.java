package org.sosy_lab.cpachecker.core.algorithm.bmc;

import java.util.List;
import org.sosy_lab.cpachecker.util.predicates.smt.BooleanFormulaManagerView;
import org.sosy_lab.java_smt.api.BooleanFormula;
import org.sosy_lab.java_smt.api.InterpolatingProverEnvironment;
import org.sosy_lab.java_smt.api.SolverException;

public final class InterpolationHelper {
  /**
   * Represent the direction to derive interpolants.
   *
   * <ul>
   *   <li>{@code FORWARD}: compute interpolants from the prefix <i>itp(A, B)</i>.
   *   <li>{@code BACKWARD}: compute interpolants from the suffix <i>!itp(B, A)</i>.
   *   <li>{@code BIDIRECTION}: compute interpolants from both the prefix and the suffix <i>itp(A,
   *       B) v !itp(B, A)</i>.
   * </ul>
   */
  public enum ItpDeriveDirection {
    FORWARD,
    BACKWARD,
    BIDIRECTION
  }

  /**
   * A helper method to derive an interpolant. It computes either <i>itp(A, B)</i>, <i>!itp(B,
   * A)</i>, or <i>itp(A, B) v !itp(B, A)</i> according to the given direction.
   *
   * @param bfmgr Boolean formula manager
   * @param itpProver SMT solver stack
   * @param itpDeriveDirection the direction to derive an interplant
   * @param formulaA Formula A (prefix)
   * @param formulaB Formula B (suffix)
   * @return A {@code BooleanFormula} interpolant
   * @throws InterruptedException On shutdown request.
   */
  static <T> BooleanFormula getInterpolantFrom(
      BooleanFormulaManagerView bfmgr,
      InterpolatingProverEnvironment<T> itpProver,
      ItpDeriveDirection itpDeriveDirection,
      final List<T> formulaA,
      final List<T> formulaB)
      throws SolverException, InterruptedException {
    if (itpDeriveDirection == ItpDeriveDirection.FORWARD) {
      return itpProver.getInterpolant(formulaA);
    } else if (itpDeriveDirection == ItpDeriveDirection.FORWARD) {
      return bfmgr.not(itpProver.getInterpolant(formulaB));
    } else { // itpDeriveDirection == ItpDeriveDirection.BIDIRECTION
      return bfmgr.or(
          itpProver.getInterpolant(formulaA), bfmgr.not(itpProver.getInterpolant(formulaB)));
    }
  }
}
