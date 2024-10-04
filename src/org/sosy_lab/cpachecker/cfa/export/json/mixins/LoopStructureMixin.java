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
import com.google.common.collect.ImmutableListMultimap;
import org.sosy_lab.cpachecker.cfa.export.json.serialization.LoopsMultimapSorterConverter;
import org.sosy_lab.cpachecker.util.LoopStructure;
import org.sosy_lab.cpachecker.util.LoopStructure.Loop;

/**
 * This class is a mixin for {@link LoopStructure}.
 *
 * <p>It sorts the loops field during serialization to ensure a deterministic order.
 *
 * <p>It specifies the constructor to use during deserialization.
 */
public final class LoopStructureMixin {

  @SuppressWarnings("unused")
  @JsonSerialize(converter = LoopsMultimapSorterConverter.class)
  private ImmutableListMultimap<String, Loop> loops;

  @SuppressWarnings("unused")
  @JsonCreator
  private LoopStructureMixin(@JsonProperty("loops") ImmutableListMultimap<String, Loop> pLoops) {}
}
