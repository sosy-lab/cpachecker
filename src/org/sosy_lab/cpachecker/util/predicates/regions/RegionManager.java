// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2007-2020 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.util.predicates.regions;

import com.google.common.primitives.ImmutableIntArray;
import java.io.PrintStream;
import java.util.List;
import java.util.function.Function;
import org.sosy_lab.cpachecker.util.Triple;
import org.sosy_lab.cpachecker.util.predicates.smt.FormulaManagerView;
import org.sosy_lab.java_smt.api.BooleanFormula;
import org.sosy_lab.java_smt.api.SolverException;

/**
 * A RegionManager encapsulates all operations for creating, inspecting, and manipulating {@link
 * Region}s.
 */
public interface RegionManager extends RegionCreator {

  enum VariableOrderingStrategy {
    RANDOM,
    SIFT,
    SIFTITE,
    WIN2,
    WIN2ITE,
    WIN3,
    WIN3ITE;
  }

  /**
   * checks whether the data region represented by f1 is a subset of that represented by f2
   *
   * @param f1 an AbstractFormula
   * @param f2 an AbstractFormula
   * @return true if (f1 => f2), false otherwise
   */
  boolean entails(Region f1, Region f2) throws SolverException, InterruptedException;

  /**
   * Creates a new variable and returns the predicate representing it.
   *
   * @return a new predicate
   */
  Region createPredicate();

  /**
   * Convert a formula into a region.
   *
   * @param pF The formula to convert.
   * @param fmgr The formula manager that belongs to pF.
   * @param atomToRegion A function that returns a region for each atom in the formula.
   * @return a region representing pF
   */
  Region fromFormula(
      BooleanFormula pF, FormulaManagerView fmgr, Function<BooleanFormula, Region> atomToRegion);

  /**
   * A region consists of the form if (predicate) then formula1 else formula2 This method decomposes
   * a region into these three parts.
   *
   * @param f a region
   * @return a triple with the condition predicate and the formulas for the true branch and the else
   *     branch
   */
  Triple<Region, Region, Region> getIfThenElse(Region f);

  /** Prints some information about the RegionManager. */
  void printStatistics(PrintStream out);

  /** Returns a short string with package name and version number. */
  String getVersion();

  /**
   * Sets the bdd variable ordering.
   *
   * @param pOrder the new order of the variables.
   */
  void setVarOrder(ImmutableIntArray pOrder);

  /**
   * Reorders the bdd variables with the provided strategy.
   *
   * @param strategy the reorder strategy that should be applied.
   */
  void reorder(VariableOrderingStrategy strategy);

  /**
   * Replace predicates in the region with a new predicates.
   *
   * <p>Corresponds to '\exists old : (region && old==new)'.
   *
   * <p>We assume that the predicates only consist of plain predicates, nothing more complex. We
   * will only use the root variable of the predicate.
   *
   * @throws IllegalArgumentException if the lists do not have the same length
   */
  Region replace(Region region, List<Region> oldPredicates, List<Region> newPredicates);
}
