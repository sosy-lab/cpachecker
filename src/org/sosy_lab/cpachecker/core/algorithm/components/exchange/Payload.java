// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2022 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.core.algorithm.components.exchange;

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

  public static final String FULL_PATH = "full";
  public static final String FIRST = "first";
  public static final String EXCEPTION = "exception";
  public static final String RESULT = "result";
  public static final String FAULT_LOCALIZATION = "fl";
  public static final String VISITED = "visited";
  public static final String STATUS = "status";
  public static final String REASON = "reason";

  private final Map<String, String> delegate;

  public Payload(Map<String, String> pDelegate) {
    delegate = ImmutableMap.copyOf(pDelegate);
  }

  public static Payload from(String json) throws JsonProcessingException {
    TypeFactory factory = TypeFactory.defaultInstance();
    MapType type = factory.constructMapType(HashMap.class, String.class, String.class);
    ObjectMapper mapper = new ObjectMapper();
    Map<String, String> result = mapper.readValue(json, type);
    return new Payload(result);
  }

  public static PayloadBuilder builder() {
    return new PayloadBuilder();
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

  public static class PayloadBuilder {

    private final Map<String, String> payload;

    public PayloadBuilder() {
      payload = new HashMap<>();
    }

    public PayloadBuilder addEntry(String key, String value) {
      payload.put(key, value);
      return this;
    }

    public PayloadBuilder addEntry(String key, Payload pPayload) throws IOException {
      payload.put(key, pPayload.toJSONString());
      return this;
    }

    public PayloadBuilder putAll(Map<String, String> pMap) {
      payload.putAll(pMap);
      return this;
    }

    public Payload build() {
      return new Payload(payload);
    }

  }
}
