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
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;
import org.sosy_lab.cpachecker.cpa.predicate.PredicatePrecision;

/** Shares each serialized precision predicate once across all states and scopes of a message. */
final class PredicatePrecisionDictionary {

  private static final String ENCODING_KEY = "predicateEncoding";
  private static final String DEFINITION_PREFIX = "predicateDefinitions.";
  private static final String PRECISION_PREFIX = PredicatePrecision.class.getName() + ".";
  private static final Splitter PREDICATES = Splitter.on(" , ").omitEmptyStrings();

  private PredicatePrecisionDictionary() {}

  private static boolean isPredicateList(String key) {
    return key.startsWith(PRECISION_PREFIX) || key.contains("." + PRECISION_PREFIX);
  }

  static ImmutableMap<String, String> encode(Map<String, String> content) {
    if (content.containsKey(ENCODING_KEY)) {
      checkArgument(content.get(ENCODING_KEY).equals("1"), "Unknown predicate encoding");
      return ImmutableMap.copyOf(content);
    }
    Map<String, Integer> definitions = new LinkedHashMap<>();
    ImmutableMap.Builder<String, String> encoded = ImmutableMap.builder();
    boolean hasRepeatedPredicate = false;
    for (var entry : content.entrySet()) {
      if (!isPredicateList(entry.getKey())) {
        encoded.put(entry);
        continue;
      }
      Set<Integer> references = new LinkedHashSet<>();
      for (String predicate : PREDICATES.split(entry.getValue())) {
        Integer reference = definitions.get(predicate);
        if (reference == null) {
          reference = definitions.size();
          definitions.put(predicate, reference);
        } else {
          hasRepeatedPredicate = true;
        }
        references.add(reference);
      }
      encoded.put(entry.getKey(), Joiner.on(" , ").join(references));
    }
    if (!hasRepeatedPredicate) {
      // A dictionary only adds keys and references when every predicate already occurs once.
      // This is common for precision-only messages and messages with an empty precision.
      return ImmutableMap.copyOf(content);
    }
    encoded.put(ENCODING_KEY, "1");
    definitions.forEach((predicate, id) -> encoded.put(DEFINITION_PREFIX + id, predicate));
    return encoded.buildOrThrow();
  }

  /** Restores the existing precision format before its CPA-specific deserializer reads it. */
  static ImmutableMap<String, String> decode(Map<String, String> content) {
    return decode(content, "");
  }

  /** Expands only the requested precision namespace, leaving unrelated state precisions alone. */
  static ImmutableMap<String, String> decode(Map<String, String> content, String pPrefix) {
    boolean encoded = content.containsKey(ENCODING_KEY);
    if (!encoded && pPrefix.isEmpty()) {
      return ImmutableMap.copyOf(content);
    }
    checkArgument(!encoded || content.get(ENCODING_KEY).equals("1"), "Unknown predicate encoding");
    ImmutableMap.Builder<String, String> decoded = ImmutableMap.builder();
    for (var entry : content.entrySet()) {
      if (!entry.getKey().startsWith(pPrefix)
          || entry.getKey().equals(ENCODING_KEY)
          || entry.getKey().startsWith(DEFINITION_PREFIX)) {
        continue;
      }
      if (!encoded || !isPredicateList(entry.getKey())) {
        decoded.put(entry);
        continue;
      }
      Set<String> predicates = new LinkedHashSet<>();
      for (String reference : PREDICATES.split(entry.getValue())) {
        String predicate = content.get(DEFINITION_PREFIX + reference);
        checkArgument(predicate != null, "Unknown predicate reference: %s", reference);
        predicates.add(predicate);
      }
      decoded.put(entry.getKey(), Joiner.on(" , ").join(predicates));
    }
    return decoded.buildOrThrow();
  }
}
