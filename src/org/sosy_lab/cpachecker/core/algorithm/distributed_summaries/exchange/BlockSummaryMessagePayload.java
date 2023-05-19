// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2022 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.core.algorithm.distributed_summaries.exchange;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.type.MapType;
import com.fasterxml.jackson.databind.type.TypeFactory;
import com.google.common.collect.ForwardingMap;
import com.google.common.collect.ImmutableMap;
import com.google.errorprone.annotations.CanIgnoreReturnValue;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import org.sosy_lab.common.JSON;

public class BlockSummaryMessagePayload extends ForwardingMap<String, Object> {

  // distributed analysis
  public static final String FULL_PATH = "full";
  public static final String FIRST = "first";
  public static final String RESULT = "result";
  public static final String VISITED = "visited";
  public static final String EXCEPTION = "exception";
  public static final String REACHABLE = "reach";
  public static final String SMART = "smart";
  // reason why error condition is reachable
  public static final String REASON = "reason";

  // fault localization
  public static final String FAULT_LOCALIZATION = "fl";

  // AlgorithmStatus
  public static final String SOUND = "sound";
  public static final String PRECISE = "precise";
  public static final String PROPERTY = "property";
  public static final String SSA = "ssa";
  public static final String PTS = "pts";

  private final Map<String, Object> delegate;

  private BlockSummaryMessagePayload(Map<String, Object> pDelegate) {
    delegate = ImmutableMap.copyOf(pDelegate);
  }

  public static BlockSummaryMessagePayload empty() {
    return new BlockSummaryMessagePayload(ImmutableMap.of());
  }

  public String toJSONString() throws IOException {
    StringBuilder builder = new StringBuilder();
    JSON.writeJSONString(delegate, builder);
    return builder.toString();
  }

  @Override
  protected Map<String, Object> delegate() {
    return delegate;
  }

  public static class Builder extends ImmutableMap.Builder<String, Object> {

    @CanIgnoreReturnValue
    public Builder addEntriesFromJSON(String json) throws JsonProcessingException {
      TypeFactory factory = TypeFactory.defaultInstance();
      MapType type = factory.constructMapType(HashMap.class, String.class, Object.class);
      ObjectMapper mapper = new ObjectMapper();
      Map<String, Object> result = mapper.readValue(json, type);
      putAll(result);
      return this;
    }

    @CanIgnoreReturnValue
    public Builder addEntry(String key, Object value) {
      put(key, value);
      return this;
    }

    @CanIgnoreReturnValue
    public Builder addAllEntries(Map<String, Object> entries) {
      putAll(entries);
      return this;
    }

    public BlockSummaryMessagePayload buildPayload() {
      return new BlockSummaryMessagePayload(buildOrThrow());
    }
  }
}
