// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2026 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.core.algorithm.distributed_summaries.distributed_cpa.composite;

import com.google.common.collect.ImmutableMap;
import org.sosy_lab.cpachecker.core.algorithm.distributed_summaries.communication.messages.DssComponentKey;
import org.sosy_lab.cpachecker.core.algorithm.distributed_summaries.communication.messages.DssStatePayload;

public record DssCompositeStatePayload(ImmutableMap<String, DssStatePayload> components)
    implements DssStatePayload {
  @Override
  public DssComponentKey getComponentKey() {
    return DssComponentKey.COMPOSITE_STATE;
  }
}
