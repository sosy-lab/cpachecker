/*
 *  CPAchecker is a tool for configurable software verification.
 *  This file is part of CPAchecker.
 *
 *  Copyright (C) 2007-2014  Dirk Beyer
 *  All rights reserved.
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 *
 *
 *  CPAchecker web page:
 *    http://cpachecker.sosy-lab.org
 */
package org.sosy_lab.cpachecker.util.predicates.regions;

import java.io.PrintStream;
import java.util.ArrayList;

import org.sosy_lab.cpachecker.util.Triple;
import org.sosy_lab.cpachecker.util.predicates.PredicateOrderingStrategy;
import org.sosy_lab.cpachecker.util.predicates.smt.FormulaManagerView;
import org.sosy_lab.java_smt.api.SolverException;
import org.sosy_lab.java_smt.api.BooleanFormula;

import com.google.common.base.Function;

/**
 * A RegionManager encapsulates all operations for creating, inspecting,
 * and manipulating {@link Region}s.
 */
public interface RegionManager extends RegionCreator {

  /**
   * checks whether the data region represented by f1
   * is a subset of that represented by f2
   * @param f1 an AbstractFormula
   * @param f2 an AbstractFormula
   * @return true if (f1 => f2), false otherwise
   */
  public boolean entails(Region f1, Region f2) throws SolverException, InterruptedException;

  /**
   * Creates a new variable and returns the predicate representing it.
   * @return a new predicate
   */
  public Region createPredicate();

  /**
   * Convert a formula into a region.
   * @param pF The formula to convert.
   * @param fmgr The formula manager that belongs to pF.
   * @param atomToRegion A function that returns a region for each atom in the formula.
   * @return a region representing pF
   */
  public Region fromFormula(BooleanFormula pF, FormulaManagerView fmgr,
      Function<BooleanFormula, Region> atomToRegion);

  /**
   * A region consists of the form
   * if (predicate) then formula1 else formula2
   * This method decomposes a region into these three parts.
   * @param f a region
   * @return a triple with the condition predicate and the formulas for the true
   *         branch and the else branch
   */
  public Triple<Region, Region, Region> getIfThenElse(Region f);

  /**
   * Prints some information about the RegionManager.
   */
  public void printStatistics(PrintStream out);

  /**
   * Returns a short string with package name and version number.
   */
  public String getVersion();

  /**
   * Sets the bdd variable ordering.
   *
   * @param pOrder the new order of the variables.
   */
  public void setVarOrder(ArrayList<Integer> pOrder);

  /**
   * Reorders the bdd variables with the provided strategy.
   *
   * @param strategy the reorder strategy that should be applied.
   */
  public void reorder(PredicateOrderingStrategy strategy);
}
