/*
 *  CPAchecker is a tool for configurable software verification.
 *  This file is part of CPAchecker.
 *
 *  Copyright (C) 2007-2016  Dirk Beyer
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
package org.sosy_lab.cpachecker.core.algorithm.pdr;

import org.sosy_lab.cpachecker.core.algorithm.pdr.transition.Block;
import org.sosy_lab.cpachecker.util.predicates.smt.FormulaManagerView;
import org.sosy_lab.java_smt.api.BooleanFormula;

/**
 * A utility class with methods for instantiating formulas based on SSA indices of the provided
 * transitions.
 */
public final class PDRUtils {

  /** Cannot instantiate utility class. */
  private PDRUtils() {}

  /**
   * Instantiates pFormula so that it represents the variables after the transition encoded in
   * pBlock. These variables are the so-called "primed" ones.
   *
   * @param pFormula the formula to be instantiated
   * @param pBlock the block containing the path formula context for the transition
   * @param pFmgr the formula manager used for instantiation
   * @return the instantiated formula representing the state after the transition
   * @see #asUnprimed(BooleanFormula, Block, FormulaManagerView)
   */
  public static BooleanFormula asPrimed(
      BooleanFormula pFormula, Block pBlock, FormulaManagerView pFmgr) {
    return pFmgr.instantiate(pFormula, pBlock.getPrimedContext().getSsa());
  }

  /**
   * Instantiates pFormula so that it represents the variables before the transition encoded in
   * pBlock. These variables are the so-called "unprimed" ones.
   *
   * @param pFormula the formula to be instantiated
   * @param pBlock the block containing the path formula context for the transition
   * @param pFmgr the formula manager used for instantiation
   * @return the instantiated formula representing the state before the transition
   * @see #asPrimed(BooleanFormula, Block, FormulaManagerView)
   */
  public static BooleanFormula asUnprimed(
      BooleanFormula pFormula, Block pBlock, FormulaManagerView pFmgr) {
    return pFmgr.instantiate(pFormula, pBlock.getUnprimedContext().getSsa());
  }
}
