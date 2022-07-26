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
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import org.sosy_lab.common.JSON;

public class Payload extends ForwardingMap<String, String> {

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

  private final Map<String, String> delegate;

  private Payload(Map<String, String> pDelegate) {
    delegate = ImmutableMap.copyOf(pDelegate);
  }

  public static Payload empty() {
    return new Payload(ImmutableMap.of());
  }

  public String toJSONString() throws IOException {
    StringBuilder builder = new StringBuilder();
    JSON.writeJSONString(delegate, builder);
    return builder.toString();
  }

  @Override
  protected Map<String, String> delegate() {
    return delegate;
  }

  public static class Builder extends ImmutableMap.Builder<String, String> {

    public Builder addEntriesFromJSON(String json) throws JsonProcessingException {
      TypeFactory factory = TypeFactory.defaultInstance();
      MapType type = factory.constructMapType(HashMap.class, String.class, String.class);
      ObjectMapper mapper = new ObjectMapper();
      Map<String, String> result = mapper.readValue(json, type);
      putAll(result);
      return this;
    }

    public Builder addEntry(String key, String value) {
      put(key, value);
      return this;
    }

    public Builder addAllEntries(Map<String, String> entries) {
      putAll(entries);
      return this;
    }

    public Payload buildPayload() {
      return new Payload(buildKeepingLast());
    }
  }
}
