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
import com.google.common.collect.ImmutableListMultimap;
import org.sosy_lab.cpachecker.util.LoopStructure;
import org.sosy_lab.cpachecker.util.LoopStructure.Loop;

/**
 * This class is a mixin for {@link LoopStructure}.
 *
 * <p>It specifies the constructor to use during deserialization.
 */
public final class LoopStructureMixin {

  @SuppressWarnings("unused")
  @JsonCreator
  private LoopStructureMixin(@JsonProperty("loops") ImmutableListMultimap<String, Loop> pLoops) {}
}
