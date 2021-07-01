// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2007-2020 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.util.coverage;

import static com.google.common.base.Preconditions.checkArgument;

import com.google.common.collect.ImmutableSet;
import com.google.common.collect.LinkedHashMultiset;
import com.google.common.collect.Multiset;
import java.util.LinkedHashSet;
import java.util.Map.Entry;
import java.util.Set;
import java.util.TreeSet;
import java.util.stream.Collectors;
import org.sosy_lab.common.collect.PathCopyingPersistentTreeMap;
import org.sosy_lab.common.collect.PersistentMap;
import org.sosy_lab.common.time.TimeSpan;
import org.sosy_lab.cpachecker.cfa.model.AssumeEdge;
import org.sosy_lab.cpachecker.cpa.smg.graphs.value.SMGKnownExpValue;
import org.sosy_lab.cpachecker.cpa.smg.util.PersistentMultimap;

class FileCoverageInformation {

  static class FunctionInfo {
    final String name;
    final int firstLine;
    final int lastLine;

    FunctionInfo(String pName, int pFirstLine, int pLastLine) {
      name = pName;
      firstLine = pFirstLine;
      lastLine = pLastLine;
    }
  }

  final Multiset<Integer> visitedLines = LinkedHashMultiset.create();
  final Set<Integer> allLines = new TreeSet<>();
  final Multiset<String> visitedFunctions = LinkedHashMultiset.create();
  final Set<FunctionInfo> allFunctions = new LinkedHashSet<>();
  final Set<AssumeEdge> allAssumes = new LinkedHashSet<>();
  final Set<AssumeEdge> visitedAssumes = new LinkedHashSet<>();
  PersistentMap<Integer, PersistentMultimap<String, SMGKnownExpValue>> additionalInfo =
      PathCopyingPersistentTreeMap.of();
  PersistentMap<Integer, PersistentMultimap<Integer, Long>> counters =
      PathCopyingPersistentTreeMap.of();
  PersistentMap<Integer, String> sourceCode = PathCopyingPersistentTreeMap.of();

  String getSourceCode(Integer pLine) {
    return sourceCode.get(pLine);
  }

  void addSourceCode(Integer pLine, String pSourceCode) {
    sourceCode = sourceCode.putAndCopy(pLine, pSourceCode);
  }

  public String getAdditionalInfo(Integer pLine) {
    PersistentMultimap<String, SMGKnownExpValue> info = additionalInfo.get(pLine);
    StringBuilder result = new StringBuilder();
    String delimiter = "";
    if (info != null) {
      for (Entry<String, ImmutableSet<SMGKnownExpValue>> entry : info.entries()) {
        result.append(delimiter + entry.getKey() + " = [");
        result.append(
            entry.getValue().stream().map(v -> v.toString()).collect(Collectors.joining(", ")));
        result.append("]");
        delimiter = ", ";
      }
    }
    return result.toString();
  }

  public String getCounterInfo(Integer pLine) {
    PersistentMultimap<Integer, Long> info = counters.get(pLine);
    if (info != null) {
      long numStops = 0;
      long timeStops = 0;
      for (Entry<Integer, ImmutableSet<Long>> entry : info.entries()) {
        numStops = numStops + entry.getKey();
        for (Long time : entry.getValue()) {
          timeStops = timeStops + time;
        }
      }
      return numStops + " stops for total time " + timeStops + " ms";
    }
    return "";
  }

  void addAdditionalInfo(int pLine, PersistentMultimap<String, SMGKnownExpValue> pAdditionalInfo) {
    PersistentMultimap<String, SMGKnownExpValue> currentInfo = additionalInfo.get(pLine);
    if (currentInfo == null) {
      currentInfo = PersistentMultimap.of();
    }
    currentInfo = currentInfo.putAllAndCopy(pAdditionalInfo);
    additionalInfo = additionalInfo.putAndCopy(pLine, currentInfo);
  }

  void addCounterInfo(int pLine, Integer counter, TimeSpan pTimeSpan) {
    PersistentMultimap<Integer, Long> currentInfo = counters.get(pLine);
    if (currentInfo == null) {
      currentInfo = PersistentMultimap.of();
    }
    currentInfo = currentInfo.putAndCopy(counter, pTimeSpan.asMillis());
    counters = counters.putAndCopy(pLine, currentInfo);
  }

  void addVisitedAssume(AssumeEdge pEdge) {
    visitedAssumes.add(pEdge);
  }

  void addExistingAssume(AssumeEdge pEdge) {
    allAssumes.add(pEdge);
  }

  void addVisitedFunction(String pName) {
    visitedFunctions.add(pName);
  }

  void addExistingFunction(String pName, int pFirstLine, int pLastLine) {
    allFunctions.add(new FunctionInfo(pName, pFirstLine, pLastLine));
  }

  void addVisitedLine(int pLine) {
    checkArgument(pLine > 0);
    visitedLines.add(pLine);
  }

  int getVisitedLine(int pLine) {
    checkArgument(pLine > 0);
    return visitedLines.count(pLine);
  }

  void addExistingLine(int pLine) {
    checkArgument(pLine > 0);
    allLines.add(pLine);
  }
}
