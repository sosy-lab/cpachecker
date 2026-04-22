// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2025 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.core.algorithm.distributed_summaries.distributed_cpa.operators.combine;

import java.util.Collection;
import org.sosy_lab.cpachecker.core.interfaces.AbstractState;
import org.sosy_lab.cpachecker.exceptions.CPAException;

/**
 * An operator to combine multiple abstract states into a single abstract state. This is useful in
 * distributed CPA settings where states from different analysis nodes need to be merged.
 *
 * <p>The resulting state must follow the contract that it over-approximates all input states.
 */
public interface CombineOperator {

  AbstractState combine(Collection<AbstractState> states) throws CPAException, InterruptedException;
}
