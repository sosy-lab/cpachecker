// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2025 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.cpa.predicate.delegatingRefinerHeuristics;

import com.fasterxml.jackson.core.JsonParser.Feature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.collect.ImmutableList;
import java.io.IOException;
import java.nio.file.Path;

/**
 * Loads and parses DSL rules from a block of lines into PatternRule objects. Used by the
 * DelegatingRefinerHeuristics.
 */
final class DelegatingRefinerDslLoader {

  static ImmutableList<DelegatingRefinerPatternRule> loadDsl(Path pPathToDsl) throws IOException {
    ObjectMapper JSONMapper = new ObjectMapper();
    JSONMapper.configure(Feature.ALLOW_COMMENTS, true);
    DelegatingRefinerPatternRule[] patternRules =
        JSONMapper.readValue(pPathToDsl.toFile(), DelegatingRefinerPatternRule[].class);
    return ImmutableList.copyOf(patternRules);
  }
}
