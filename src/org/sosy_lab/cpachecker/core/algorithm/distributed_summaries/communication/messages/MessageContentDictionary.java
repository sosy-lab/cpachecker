// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2026 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.core.algorithm.distributed_summaries.communication.messages;

import static com.google.common.base.Preconditions.checkArgument;

import com.google.common.base.Joiner;
import com.google.common.base.Splitter;
import com.google.common.collect.ImmutableMap;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

/** Lossless dictionary encoding of repeated text in message values, independent of their CPA. */
final class MessageContentDictionary {
  private static final String ENCODING_KEY = "dssContentEncoding";
  private static final String DEFINITION_PREFIX = "definitions.";
  private static final String CONTENT_PREFIX = "content.";
  // Common separator for serialized collections. Splitting and joining preserves arbitrary text,
  // including empty elements, repeated elements, and values that are not collections.
  private static final String SEPARATOR = " , ";
  private static final Splitter PARTS = Splitter.on(SEPARATOR);

  private MessageContentDictionary() {}

  static ImmutableMap<String, String> encode(Map<String, String> content) {
    if (content.containsKey(ENCODING_KEY)) {
      return ImmutableMap.copyOf(content);
    }
    Map<String, Integer> definitions = new LinkedHashMap<>();
    ImmutableMap.Builder<String, String> encoded = ImmutableMap.builder();
    encoded.put(ENCODING_KEY, "1");
    for (Entry<String, String> entry : content.entrySet()) {
      List<Integer> references = new ArrayList<>();
      for (String part : PARTS.split(entry.getValue())) {
        references.add(definitions.computeIfAbsent(part, unused -> definitions.size()));
      }
      encoded.put(CONTENT_PREFIX + entry.getKey(), Joiner.on(SEPARATOR).join(references));
    }
    definitions.forEach((part, id) -> encoded.put(DEFINITION_PREFIX + id, part));
    ImmutableMap<String, String> result = encoded.buildOrThrow();
    return size(result) < size(content) ? result : ImmutableMap.copyOf(content);
  }

  private static long size(Map<String, String> content) {
    long size = 0;
    for (Entry<String, String> entry : content.entrySet()) {
      size += entry.getKey().length() + entry.getValue().length() + 6L;
    }
    return size;
  }

  static ImmutableMap<String, String> decode(Map<String, String> content) {
    if (!content.containsKey(ENCODING_KEY)) {
      return ImmutableMap.copyOf(content);
    }
    checkArgument(content.get(ENCODING_KEY).equals("1"), "Unknown content encoding");
    ImmutableMap.Builder<String, String> decoded = ImmutableMap.builder();
    for (Entry<String, String> entry : content.entrySet()) {
      if (!entry.getKey().startsWith(CONTENT_PREFIX)) {
        continue;
      }
      List<String> parts = new ArrayList<>();
      for (String reference : PARTS.split(entry.getValue())) {
        String part = content.get(DEFINITION_PREFIX + reference);
        checkArgument(part != null, "Unknown content reference: %s", reference);
        parts.add(part);
      }
      decoded.put(
          entry.getKey().substring(CONTENT_PREFIX.length()), Joiner.on(SEPARATOR).join(parts));
    }
    return decoded.buildOrThrow();
  }
}
