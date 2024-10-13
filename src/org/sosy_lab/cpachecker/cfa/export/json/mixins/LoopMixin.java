// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2024 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.cfa.export.json.mixins;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.google.common.collect.ImmutableSet;
import java.util.Set;
import org.sosy_lab.cpachecker.cfa.export.json.serialization.CfaNodeSetToSortedListConverter;
import org.sosy_lab.cpachecker.cfa.model.CFANode;
import org.sosy_lab.cpachecker.util.LoopStructure.Loop;

/**
 * This class is a mixin for {@link Loop}.
 *
 * <p>It sorts the {@link #loopHeads} field during serialization to ensure a deterministic order.
 *
 * <p>It specifies the constructor to use during deserialization.
 */
public final class LoopMixin {

  @SuppressWarnings("unused")
  @JsonSerialize(converter = CfaNodeSetToSortedListConverter.class)
  private ImmutableSet<CFANode> loopHeads;

  @SuppressWarnings("unused")
  @JsonCreator
  private LoopMixin(
      @JsonProperty("loopHeads") Set<CFANode> pLoopHeads,
      @JsonProperty("nodes") Set<CFANode> pNodes) {}
}
