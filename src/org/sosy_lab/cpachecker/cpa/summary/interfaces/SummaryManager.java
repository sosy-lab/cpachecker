/*
 * CPAchecker is a tool for configurable software verification.
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
package org.sosy_lab.cpachecker.cpa.summary.interfaces;

import java.util.Collection;
import org.sosy_lab.cpachecker.core.interfaces.AbstractState;
import org.sosy_lab.cpachecker.core.interfaces.Precision;
import org.sosy_lab.cpachecker.core.reachedset.ReachedSet;
import org.sosy_lab.cpachecker.exceptions.CPATransferException;

public interface SummaryManager {

  // todo: communicating to the CPA
  // that the abstraction has to be performed,
  // as the current state is at the function entry/exit.
  // maybe it is easy to use BAM datastructures for this task.

  /**
   * Calculate the abstract successors subject to a summary
   * {@code pSummary}
   *
   * @param state Initial state, associated with a function call.
   * @param precision Analysis precision at the to-state.
   * @param pSummary Summary available for the called function.
   * @return Set of abstract states resulting from the summary application.
   */
  Collection<? extends AbstractState> getAbstractSuccessorsForSummary(
      AbstractState state,
      Precision precision,
      Summary pSummary)
      throws CPATransferException, InterruptedException;

  /**
   *
   * @param pSummary Function summary
   * @return Projection of the summary to the function entry point:
   * the summary precondition.
   */
  AbstractState projectToPrecondition(Summary pSummary);

  /**
   *
   * // todo: what if the function has multiple return nodes?
   * // disjunction over all possibilities?
   *
   * @param pSummary Function summary
   * @return Projection of the summary to the return site:
   * the summary postcondition.
   */
  AbstractState projectToPostcondition(Summary pSummary);

  /**
   * Generate summary from the result of intraprocedural analysis.
   *
   * // todo: might just give return/start states.
   *
   * @param pReached Result of intraprocedural analysis.
   * @return summary describing subsuming the result.
   */
  Summary generateSummary(ReachedSet pReached);

  /**
   * Optionally merge two summaries, same interface as
   * {@link org.sosy_lab.cpachecker.core.interfaces.MergeOperator}.
   * The result has to subsume the second argument.
   *
   * <p>Implementation should return the second argument to refuse
   * merging and to keep the states separate instead.
   */
  Summary merge(Summary pSummary1, Summary pSummary2);
}
