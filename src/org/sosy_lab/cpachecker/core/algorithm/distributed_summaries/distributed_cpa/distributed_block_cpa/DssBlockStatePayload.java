// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2026 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.core.algorithm.distributed_summaries.distributed_cpa.distributed_block_cpa;

import com.google.common.base.Joiner;
import com.google.common.base.Preconditions;
import com.google.common.base.Splitter;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import java.util.List;
import java.util.Map;
import org.sosy_lab.cpachecker.core.algorithm.distributed_summaries.communication.messages.DssComponentKey;
import org.sosy_lab.cpachecker.core.algorithm.distributed_summaries.communication.messages.DssMessageKeys;
import org.sosy_lab.cpachecker.core.algorithm.distributed_summaries.communication.messages.DssStatePayload;
import org.sosy_lab.cpachecker.cpa.block.BlockState;

public record DssBlockStatePayload(String blockId, String witness, ImmutableList<String> history)
    implements DssStatePayload {
  @Override
  public DssComponentKey getComponentKey() {
    return DssComponentKey.BLOCK_STATE;
  }

  @Override
  public ImmutableMap<String, String> asContentMap() {
    String suffix = " W:" + witness();
    suffix = suffix + (history().isEmpty() ? "" : " H:" + Joiner.on(",").join(history()));
    return ImmutableMap.of(
        getComponentKey().getSerializedName() + "." + DssMessageKeys.STATE, blockId() + suffix);
  }

  public String getWitnessString() {
    String suffix = " W:" + witness();
    suffix = suffix + (history().isEmpty() ? "" : " H:" + Joiner.on(",").join(history()));
    return blockId + suffix;
  }

  public static DssBlockStatePayload fromContentMap(Map<String, String> pContent) {
    String contentString =
        pContent.getOrDefault(
            DssComponentKey.forClass(BlockState.class).getSerializedName()
                + "."
                + DssMessageKeys.STATE,
            "missingKey");
    if (contentString.equals("missingKey"))
      throw new IllegalArgumentException("Block state key is missing");

    return fromContentString(contentString);
  }

  public static DssBlockStatePayload fromContentString(String pContent) {
    List<String> idAndWitnessAndMaybeHistory = Splitter.on(" W:").limit(2).splitToList(pContent);
    Preconditions.checkArgument(idAndWitnessAndMaybeHistory.size() == 2);
    String blockId = idAndWitnessAndMaybeHistory.getFirst();
    List<String> witnessAndMaybeHistory =
        Splitter.on(" H:").limit(2).splitToList(idAndWitnessAndMaybeHistory.getLast());
    String witness = witnessAndMaybeHistory.getFirst();
    ImmutableList<String> history =
        witnessAndMaybeHistory.size() == 2
            ? ImmutableList.copyOf(Splitter.on(",").splitToList(witnessAndMaybeHistory.getLast()))
            : ImmutableList.of();
    return new DssBlockStatePayload(blockId, witness, history);
  }
}
