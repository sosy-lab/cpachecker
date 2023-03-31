// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2022 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.util.faultlocalization.explanation;

import com.google.common.base.Splitter;
import com.google.common.collect.Sets;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import org.sosy_lab.cpachecker.cfa.model.CFAEdge;
import org.sosy_lab.cpachecker.util.faultlocalization.Fault;
import org.sosy_lab.cpachecker.util.faultlocalization.FaultExplanation;
import org.sosy_lab.cpachecker.util.faultlocalization.FaultRankingUtils;

public class InformationProvider implements FaultExplanation {

  // matches eg "x = 4 + arr[c + 3] does not match "[.*]"
  private static final Pattern MATCH_ARRAY_OPERATION = Pattern.compile(".+\\[.*[+\\-/*]+.*].*");
  private final Set<String> iterationVariables;

  public InformationProvider(List<CFAEdge> pEdges) {
    iterationVariables = calculateIterationVariables(pEdges);
  }

  private Set<String> calculateIterationVariables(List<CFAEdge> edges) {
    // matches eg "x = x + 1", "test = test4    - 3" but not "test = 3 + test4"
    final Pattern matchIteration = Pattern.compile(".+=.+[+\\-/*][ 1-9]+[0-9]+");

    // Find iteration variables
    Map<Object, Long> counts =
        edges.stream()
            .map(CFAEdge::getDescription)
            .collect(Collectors.groupingBy(e -> e, Collectors.counting()));
    Set<String> localIterationVariables = new HashSet<>();
    for (Entry<Object, Long> entry : counts.entrySet()) {
      if (entry.getValue() > 3) {
        String curr = (String) entry.getKey();
        Matcher itMatch = matchIteration.matcher(curr);
        if (itMatch.matches()) {
          List<String> parts = Splitter.on(" ").splitToList(curr);
          parts.removeIf(String::isBlank);
          if (parts.size() > 2) {
            if (parts.get(0).equals(parts.get(2))) {
              localIterationVariables.add(parts.get(0));
            }
          }
        }
      }
    }
    return localIterationVariables;
  }

  @Override
  public String explanationFor(Fault fault) {
    Set<String> tokens = FaultRankingUtils.findTokensInFault(fault);
    Set<String> availableToken = Sets.intersection(iterationVariables, tokens);
    String explanation = "";
    if (!availableToken.isEmpty()) {
      explanation +=
          "This fault operates on the following iteration variables: " + availableToken + ".";
    }
    boolean hasCalc =
        fault.stream()
            .map(fc -> fc.correspondingEdge().getDescription())
            .anyMatch(MATCH_ARRAY_OPERATION.asPredicate());
    if (hasCalc) {
      if (!availableToken.isEmpty()) {
        explanation +=
            " Additionally, some calculations happen within the array subscript. Please take a"
                + " closer look.";
      } else {
        explanation +=
            "Some calculations happen within the array subscript. Please take a closer look.";
      }
    }
    return explanation;
  }
}
