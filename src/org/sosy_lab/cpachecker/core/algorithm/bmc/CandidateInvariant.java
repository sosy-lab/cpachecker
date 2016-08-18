/*
 *  CPAchecker is a tool for configurable software verification.
 *  This file is part of CPAchecker.
 *
 *  Copyright (C) 2007-2015  Dirk Beyer
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
package org.sosy_lab.cpachecker.core.algorithm.bmc;

import org.sosy_lab.cpachecker.core.interfaces.AbstractState;
import org.sosy_lab.cpachecker.core.reachedset.ReachedSet;
import org.sosy_lab.cpachecker.exceptions.CPATransferException;
import org.sosy_lab.cpachecker.util.predicates.pathformula.PathFormula;
import org.sosy_lab.cpachecker.util.predicates.pathformula.PathFormulaManager;
import org.sosy_lab.cpachecker.util.predicates.smt.FormulaManagerView;
import org.sosy_lab.java_smt.api.BooleanFormula;

import javax.annotation.Nullable;


public interface CandidateInvariant {

  /**
   * Gets the uninstantiated invariant formula.
   *
   * @param pFMGR the formula manager.
   * @param pPFMGR the path formula manager.
   * @param pContext the path formula context.
   *
   * @return the uninstantiated invariant formula.
   *
   * @throws CPATransferException if a CPA transfer required to produce the
   * assertion failed.
   * @throws InterruptedException if the formula creation was interrupted.
   */
  BooleanFormula getFormula(
      FormulaManagerView pFMGR, PathFormulaManager pPFMGR, @Nullable PathFormula pContext)
      throws CPATransferException, InterruptedException;

  /**
   * Creates an assertion of the invariant over the given reached set, using
   * the given formula managers.
   *
   * @param pReachedSet the reached set to assert the invariant over.
   * @param pFMGR the formula manager.
   * @param pPFMGR the path formula manager.
   * @param pDefaultIndex the default SSA index.
   *
   * @return the assertion.
   *
   * @throws CPATransferException if a CPA transfer required to produce the
   * assertion failed.
   * @throws InterruptedException if the formula creation was interrupted.
   */
  BooleanFormula getAssertion(
      Iterable<AbstractState> pReachedSet,
      FormulaManagerView pFMGR,
      PathFormulaManager pPFMGR,
      int pDefaultIndex)
      throws CPATransferException, InterruptedException;

  /**
   * Assume that the invariant holds and remove states from the given reached
   * set that must therefore be unreachable.
   *
   * @param pReachedSet the reached set to remove unreachable states from.
   */
  void assumeTruth(ReachedSet pReachedSet);

}
