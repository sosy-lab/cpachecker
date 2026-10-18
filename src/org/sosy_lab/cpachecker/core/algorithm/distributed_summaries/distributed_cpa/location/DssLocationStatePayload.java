// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2026 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.core.algorithm.distributed_summaries.distributed_cpa.location;

import com.google.common.collect.ImmutableMap;
import java.util.Map;
import org.sosy_lab.cpachecker.core.algorithm.distributed_summaries.communication.messages.DssComponentKey;
import org.sosy_lab.cpachecker.core.algorithm.distributed_summaries.communication.messages.DssMessageKeys;
import org.sosy_lab.cpachecker.core.algorithm.distributed_summaries.communication.messages.DssStatePayload;

public record DssLocationStatePayload(int nodeId) implements DssStatePayload {
  @Override
  public DssComponentKey getComponentKey() {
    return DssComponentKey.LOCATION_STATE;
  }

  @Override
  public ImmutableMap<String, String> asContentMap() {
    return ImmutableMap.of(
        getComponentKey().getSerializedName() + "." + DssMessageKeys.STATE,
        Integer.toString(nodeId));
  }

  public static DssLocationStatePayload fromContentMap(Map<String, String> pContent) {
    String serializedNodeId =
        pContent.get(
            DssComponentKey.LOCATION_STATE.getSerializedName() + "." + DssMessageKeys.STATE);
    return new DssLocationStatePayload(Integer.parseInt(serializedNodeId));
  }
}
