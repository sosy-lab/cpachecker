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

public interface SerializeOperator {

  String STATE_KEY = "state";

  /**
   * Serialize an abstract state to the content of a message
   *
   * @param pState this state will be serialized
   * @return The serialized state as map of strings. The deserialize operator must be able to
   *     reconstruct the state from this map. It should only know about the keys defined in this
   *     interface.
   */
  ImmutableMap<String, String> serialize(AbstractState pState);
}
