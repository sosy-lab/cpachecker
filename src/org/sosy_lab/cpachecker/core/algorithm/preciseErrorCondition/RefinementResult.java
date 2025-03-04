// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2025 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.core.algorithm.preciseErrorCondition;

import java.util.Optional;
import org.sosy_lab.cpachecker.util.predicates.pathformula.PathFormula;
import org.sosy_lab.java_smt.api.BooleanFormula;

/**
 * Represents the result of a refinement operation.
 *
 * <p>A refinement result consists of a {@link RefinementStatus} and an {@link Optional}
 * containing a {@link PathFormula}. The formula is present when the refinement operation
 * is successful. For instance, if the refinement fails or times out, the result should be
 * created with {@code Optional.empty()} as the formula.</p>
 */
public class RefinementResult {

  private RefinementStatus status;
  private Optional<PathFormula> formula;

  /**
   * Creates a new instance of {@code RefinementResult}.
   *
   * @param pRefinementStatus the status of the refinement
   * @param pFormula          an {@link Optional} containing the {@link PathFormula} if available;
   *                          use {@link Optional#empty()} to indicate absence of a formula (e.g., in failure cases)
   */
  public RefinementResult(RefinementStatus pRefinementStatus, Optional<PathFormula> pFormula) {
    this.status = pRefinementStatus;
    this.formula = pFormula;
  }

  /**
   * Returns the status of the refinement.
   *
   * @return the {@link RefinementStatus} representing the result status
   */
  public RefinementStatus getStatus() {
    return status;
  }

  /**
   * Updates the status of the refinement.
   *
   * @param pRefinementStatus the new status of the refinement.
   */
  public void updateStatus(RefinementStatus pRefinementStatus) {
    status = pRefinementStatus;
  }

  /**
   * Returns the formula resulting from the refinement.
   *
   * <p>If the refinement did not produce a valid formula (e.g., due to failure or timeout),
   * this will return {@link Optional#empty()}.</p>
   *
   * @return an {@link Optional} containing the {@link PathFormula}, or {@link Optional#empty()}
   * if no formula is available
   */
  public Optional<PathFormula> getOptionalFormula() {
    return formula;
  }

  /**
   * Returns the BooleanFormula from the FormulaPath (if exists)
   *
   * @return BooleanFormula
   */
  public BooleanFormula getBooleanFormula() {
    return formula.get().getFormula();
  }

  /**
   * Updates the formula from the refinement.
   *
   * @param pFormula new formula.
   */
  public void updateFormula(PathFormula pFormula) {
    formula = Optional.of(pFormula);
  }

  /**
   * Checks if the refinement was successful.
   *
   * @return {@code true} if the status is {@link RefinementStatus#SUCCESS}, {@code false} otherwise
   */
  public boolean isSuccessful() {
    return status == RefinementStatus.SUCCESS;
  }

  /**
   * Represents the status of the refinement operation.
   */
  public enum RefinementStatus {
    EMPTY,
    SUCCESS,
    FAILURE,
    TIMEOUT
  }
}

