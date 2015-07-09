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

import org.sosy_lab.cpachecker.core.algorithm.invariants.InvariantGenerator;
import org.sosy_lab.cpachecker.core.interfaces.AbstractState;
import org.sosy_lab.cpachecker.core.reachedset.ReachedSet;
import org.sosy_lab.cpachecker.exceptions.CPATransferException;
import org.sosy_lab.cpachecker.exceptions.UnrecognizedCodeException;
import org.sosy_lab.cpachecker.util.predicates.interfaces.BooleanFormula;
import org.sosy_lab.cpachecker.util.predicates.interfaces.PathFormulaManager;
import org.sosy_lab.cpachecker.util.predicates.interfaces.view.FormulaManagerView;


public interface CandidateInvariant {

  /**
   * Gets the uninstantiated invariant formula.
   *
   * @return the uninstantiated invariant formula.
   *
   * @throws CPATransferException if a CPA transfer required to produce the
   * assertion failed.
   * @throws InterruptedException if the formula creation was interrupted.
   */
  BooleanFormula getFormula(FormulaManagerView pFMGR, PathFormulaManager pPFMGR) throws CPATransferException, InterruptedException;

  /**
   * Creates an assertion of the invariant over the given reached set, using
   * the given formula managers.
   *
   * @param pReachedSet the reached set to assert the invariant over.
   * @param pFMGR the formula manager.
   * @param pPFMGR the path formula manager.
   *
   * @return the assertion.
   *
   * @throws CPATransferException if a CPA transfer required to produce the
   * assertion failed.
   * @throws InterruptedException if the formula creation was interrupted.
   */
  BooleanFormula getAssertion(Iterable<AbstractState> pReachedSet, FormulaManagerView pFMGR, PathFormulaManager pPFMGR) throws CPATransferException, InterruptedException;

  /**
   * Assume that the invariant holds and remove states from the given reached
   * set that must therefore be unreachable.
   *
   * @param pReachedSet the reached set to remove unreachable states from.
   */
  void assumeTruth(ReachedSet pReachedSet);

  /**
   * Try to inject the invariant into an invariant generator in order to
   * improve its results.
   *
   * @param pInvariantGenerator the invariant generator to inject the invariant
   * into.
   * @throws UnrecognizedCodeException if a problem occurred during the
   * injection.
   */
  void attemptInjection(InvariantGenerator pInvariantGenerator) throws UnrecognizedCodeException;

}
