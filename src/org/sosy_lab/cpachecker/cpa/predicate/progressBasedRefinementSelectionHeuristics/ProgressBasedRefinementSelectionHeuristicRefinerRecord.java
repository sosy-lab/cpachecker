// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2025 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.cpa.predicate.progressBasedRefinementSelectionHeuristics;

import org.sosy_lab.cpachecker.core.interfaces.Refiner;

/**
 * Immutable pairing of a {@link ProgressBasedRefinementSelectionHeuristic} and a corresponding
 * refiner for use in the ProgressBasedRefinementSelection. Each refiner in the record is
 * conditionally activated based on the outcome of its associated heuristic. When the heuristic
 * returns {@code true}, the paired refiner is called to perform the refinement.
 */
public record ProgressBasedRefinementSelectionHeuristicRefinerRecord(
    ProgressBasedRefinementSelectionHeuristic pHeuristic, Refiner pRefiner) {}
