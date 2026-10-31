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
import java.util.Optional;
import org.sosy_lab.cpachecker.core.algorithm.distributed_summaries.communication.payloads.DssComponentKey;
import org.sosy_lab.cpachecker.core.algorithm.distributed_summaries.communication.payloads.DssStatePayload;

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
  public Optional<DssStatePayload> findComponent(DssComponentKey key) {
    DssStatePayload direct = components().get(key.getSerializedName());
    if (direct != null) {
      return Optional.of(direct);
    }

    return components().values().stream()
        .map(component -> component.findComponent(key))
        .flatMap(Optional::stream)
        .findFirst();
  }
}
