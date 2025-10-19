// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2025 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.cpa.predicate.delegatingRefinerUtils;

import com.google.common.collect.ImmutableList;
import org.sosy_lab.java_smt.api.BooleanFormula;

/**
 * A metadata tracker used by {@link
 * org.sosy_lab.cpachecker.cpa.predicate.delegatingRefinerHeuristics.DelegatingRefinerHeuristicInterpolationRate}.
 * The class stores refinement count and generated interpolants across iterations. Interpolants are
 * stored as grouped list per refinement steps.
 */
public class TrackingPredicateCPARefinementContext {

  private final ImmutableList.Builder<ImmutableList<BooleanFormula>> allInterpolantsAddedBuilder =
      ImmutableList.builder();
  private ImmutableList<ImmutableList<BooleanFormula>> allInterpolantsAdded = ImmutableList.of();
  private int numberOfRefinements = 0;

  /**
   * Stores a list of interpolants generated during a refinement iteration. Each call adds a new
   * interpolant group to the overall history of generated interpolants.
   *
   * @param pInterpolantList the interpolants generated in one refinement step
   */
  public void storeInterpolants(ImmutableList<BooleanFormula> pInterpolantList) {
    allInterpolantsAddedBuilder.add(pInterpolantList);
    allInterpolantsAdded = allInterpolantsAddedBuilder.build();
  }

  /**
   * Stores the total number of refinement iterations already executed.
   *
   * @param pNumberOfRefinements the total number of refinements
   */
  public void storeNumberOfRefinements(int pNumberOfRefinements) {
    numberOfRefinements = pNumberOfRefinements;
  }

  /**
   * Returns a flattened list of all interpolants generated across all refinement iterations.
   *
   * @return a list of all interpolants computed
   */
  public ImmutableList<BooleanFormula> getAllInterpolants() {
    ImmutableList.Builder<BooleanFormula> totalInterpolants = ImmutableList.builder();
    for (ImmutableList<BooleanFormula> interpolantList : allInterpolantsAdded) {
      totalInterpolants.addAll(interpolantList);
    }
    return totalInterpolants.build();
  }

  /**
   * Returns the total number of refinements already executed.
   *
   * @return the refinement count
   */
  public int getNumberOfRefinements() {
    return numberOfRefinements;
  }
}
