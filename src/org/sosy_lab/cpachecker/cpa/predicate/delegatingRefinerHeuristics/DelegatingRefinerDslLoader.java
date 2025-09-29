// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2025 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.cpa.predicate.delegatingRefinerHeuristics;

import static com.google.common.base.Preconditions.checkNotNull;

import com.google.common.base.Splitter;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.common.io.CharStreams;
import java.io.BufferedReader;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Loads and parses DSL rules from a block of lines into PatternRule objects. Used by the
 * DelegatingRefinerHeuristics.
 */
final class DelegatingRefinerDslLoader {

  private enum ParserState {
    OUTSIDE_RULE,
    INSIDE_RULE,
    INSIDE_TAGS
  }

  static ImmutableList<DelegatingRefinerPatternRule> loadDsl(Path pPathToDsl) throws IOException {
    try (BufferedReader reader = Files.newBufferedReader(pPathToDsl)) {
      List<String> lines = CharStreams.readLines(reader);
      return parseDsl(lines);
    }
  }

  private static ImmutableList<DelegatingRefinerPatternRule> parseDsl(List<String> pStringList) {
    ImmutableList.Builder<DelegatingRefinerPatternRule> patternRules = ImmutableList.builder();
    List<String> currentRuleBlock = new ArrayList<>();
    ParserState parserState = ParserState.OUTSIDE_RULE;

    for (String rawLine : pStringList) {
      String line = rawLine.trim();

      if (line.isEmpty() || line.startsWith("//")) {
        continue;
      }

      switch (parserState) {
        case OUTSIDE_RULE -> {
          if (line.startsWith("rule")) {
            currentRuleBlock.clear();
            currentRuleBlock.add(line);
            parserState = ParserState.INSIDE_RULE;
          }
        }
        case INSIDE_RULE -> {
          currentRuleBlock.add(line);
          if (line.startsWith("tags")) {
            parserState = ParserState.INSIDE_TAGS;
          } else if (line.startsWith("}")) {
            patternRules.add(createPatternRule(currentRuleBlock));
            parserState = ParserState.OUTSIDE_RULE;
          }
        }
        case INSIDE_TAGS -> {
          currentRuleBlock.add(line);
          if (line.startsWith("}")) {
            patternRules.add(createPatternRule(currentRuleBlock));
            parserState = ParserState.OUTSIDE_RULE;
          }
        }
      }
    }
    return patternRules.build();
  }

  private static DelegatingRefinerPatternRule createPatternRule(List<String> pStringList) {
    String id = null;
    String category = null;
    String patternMatch = null;
    String normalizedPattern = null;
    String patternFingerprint = null;
    Map<String, String> tags = parseTags(pStringList);

    for (String line : pStringList) {
      if (line.startsWith("id:")) {
        id = extractValue(line, "id:");
      } else if (line.startsWith("category:")) {
        category = extractValue(line, "category:");
      } else if (line.startsWith("match:")) {
        patternMatch = extractValue(line, "match:");
      } else if (line.startsWith("normalize:")) {
        normalizedPattern = extractValue(line, "normalize:");
      } else if (line.startsWith("fingerprint:")) {
        patternFingerprint = extractValue(line, "fingerprint:");
      }
    }

    checkNotNull(id, "DSL rule must have an id.");
    checkNotNull(category, "DSL rule must have a category.");
    checkNotNull(patternMatch, "DSL rule must have a match.");

    return DelegatingRefinerPatternRule.of(
        patternMatch,
        normalizedPattern,
        patternFingerprint,
        id,
        ImmutableMap.<String, String>builder().putAll(tags).buildKeepingLast(),
        category);
  }

  private static Map<String, String> parseTags(List<String> pStringList) {
    Map<String, String> tags = new LinkedHashMap<>();
    boolean inTags = false;
    for (String line : pStringList) {
      if (line.startsWith("tags:")) {
        inTags = true;
      } else if (inTags && line.startsWith("}")) {
        break;

      } else if (inTags && line.contains(":")) {
        List<String> tagParts =
            Splitter.on(':').limit(2).trimResults().splitToList(line.replace(",", ""));
        if (tagParts.size() == 2) {
          tags.put(tagParts.getFirst(), tagParts.get(1));
        }
      }
    }
    return tags;
  }

  private static String extractValue(String pLine, String pPrefix) {
    return pLine.substring(pPrefix.length()).trim();
  }
}
