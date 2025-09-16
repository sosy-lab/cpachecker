// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2022 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.core.algorithm.distributed_summaries.distributed_cpa.operators.serialize;

import com.google.common.collect.ImmutableMap;
import org.sosy_lab.cpachecker.core.interfaces.AbstractState;
import org.sosy_lab.java_smt.api.BooleanFormula;

public interface SerializeOperator {

  String STATE_KEY = "state";

  /**
   * Serialize an abstract state to the content of a message
   *
   * @param pState this state will be serialized
   * @return payload
   */
  ImmutableMap<String, String> serialize(AbstractState pState);

  default BooleanFormula serializeToFormula(AbstractState pState) {
    throw new UnsupportedOperationException("This method is not supported.");
  }
}
