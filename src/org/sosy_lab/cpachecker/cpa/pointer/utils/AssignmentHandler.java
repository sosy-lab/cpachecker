// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2025 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.cpa.pointer.utils;

import java.util.Optional;
import java.util.logging.Level;
import org.sosy_lab.common.log.LogManager;
import org.sosy_lab.cpachecker.cfa.model.c.CCfaEdge;
import org.sosy_lab.cpachecker.cpa.pointer.PointerAnalysisState;
import org.sosy_lab.cpachecker.cpa.pointer.location.InvalidLocation;
import org.sosy_lab.cpachecker.cpa.pointer.location.NullLocation;
import org.sosy_lab.cpachecker.cpa.pointer.location.PointerLocation;
import org.sosy_lab.cpachecker.cpa.pointer.locationset.LocationSet;

/**
 * A utility class for handling pointer assignments.
 *
 * <p>This class centralizes the logic for validating operands and handling edge cases before a
 * points-to update is performed. It ensures that assignments to invalid or null locations are
 * correctly identified, and that assignments of abstract top locations are handled consistently.
 */
public class AssignmentHandler {

  /**
   * Checks for and handles edge cases for an assignment to a single, explicit LHS location.
   *
   * <p>This method serves as a precondition check before performing a standard points-to map
   * update. It handles the following cases:
   *
   * <ul>
   *   <li><b>Assignment to {@code NULL}</b>: Results in a {@code BOTTOM} state, as this represents
   *       a program crash.
   *   <li><b>Assignment to an invalid location</b>: Also results in a {@code BOTTOM} state.
   *   <li><b>Assignment of {@code TOP}</b>: If the RHS points-to set is {@code TOP}, the existing
   *       mapping for the LHS is removed to reflect the uncertainty.
   * </ul>
   *
   * If any of these edge cases are applicable, this method returns an {@code Optional} containing
   * the resulting {@link PointerAnalysisState}. If none apply, it returns an empty {@code
   * Optional}, signaling that the caller should proceed with a normal assignment.
   *
   * @param pState The current state of the pointer analysis.
   * @param lhsLocation The single, resolved location on the left-hand side of the assignment.
   * @param rhsLocations The set of locations pointed to by the right-hand side.
   * @param pCfaEdge The current CFA edge for logging purposes.
   * @param plogger The logger for reporting warnings.
   * @return An {@code Optional} with the new state if an edge case was handled, or {@code
   *     Optional.empty()} otherwise.
   */
  public static Optional<PointerAnalysisState> handleAssignmentEdgeCases(
      PointerAnalysisState pState,
      PointerLocation lhsLocation,
      LocationSet rhsLocations,
      CCfaEdge pCfaEdge,
      LogManager plogger) {

    if (lhsLocation instanceof NullLocation) {
      plogger.logf(
          Level.WARNING, "PointerAnalysis: Assignment to null at %s", pCfaEdge.getFileLocation());
      return Optional.of(PointerAnalysisState.BOTTOM_STATE);
    }

    if (lhsLocation instanceof InvalidLocation) {
      plogger.logf(
          Level.WARNING,
          "PointerAnalysis: Assignment to invalid location %s at %s",
          lhsLocation,
          pCfaEdge.getFileLocation());
      return Optional.of(PointerAnalysisState.BOTTOM_STATE);
    }

    if (rhsLocations.isTop()) {
      return Optional.of(removeMappingOnTopRhs(pState, lhsLocation));
    }

    return Optional.empty();
  }

  private static PointerAnalysisState removeMappingOnTopRhs(
      PointerAnalysisState pState, PointerLocation lhsLocation) {
    if (pState.getPointsToMap().containsKey(lhsLocation)) {
      return new PointerAnalysisState(pState.getPointsToMap().removeAndCopy(lhsLocation));
    }
    return pState;
  }
}
