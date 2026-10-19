// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2026 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.core.algorithm.distributed_summaries.distributed_cpa.composite;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.google.common.collect.ImmutableMap;
import java.util.Map;
import org.sosy_lab.cpachecker.core.algorithm.distributed_summaries.communication.messages.DssComponentKey;
import org.sosy_lab.cpachecker.core.algorithm.distributed_summaries.communication.messages.DssStatePayload;

public record DssCompositeStatePayload(
    @JsonProperty(COMPONENTS) ImmutableMap<String, DssStatePayload> components)
    implements DssStatePayload {
  public static final String COMPONENTS = "components";

  @JsonCreator
  DssCompositeStatePayload(@JsonProperty(COMPONENTS) Map<String, DssStatePayload> pComponents) {
    this(ImmutableMap.copyOf(pComponents));
  }

  @Override
  @JsonIgnore
  public DssComponentKey getComponentKey() {
    return DssComponentKey.COMPOSITE_STATE;
  }

  @Override
  public ImmutableMap<String, String> asContentMap() {
    return null;
  }
}
