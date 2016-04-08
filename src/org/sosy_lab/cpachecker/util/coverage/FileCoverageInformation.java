/*
 *  CPAchecker is a tool for configurable software verification.
 *  This file is part of CPAchecker.
 *
 *  Copyright (C) 2007-2014  Dirk Beyer
 *  All rights reserved.
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 *
 *
 *  CPAchecker web page:
 *    http://cpachecker.sosy-lab.org
 */
package org.sosy_lab.cpachecker.util.coverage;

import static com.google.common.base.Preconditions.checkArgument;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import org.sosy_lab.cpachecker.cfa.model.AssumeEdge;

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

  final Map<Integer, Integer> visitedLines = new HashMap<>();
  final Set<Integer> allLines = new HashSet<>();
  final Map<String, Integer> visitedFunctions = new HashMap<>();
  final Set<FunctionInfo> allFunctions = new HashSet<>();
  final Set<AssumeEdge> allAssumes = new HashSet<>();
  final Set<AssumeEdge> visitedAssumes = new HashSet<>();

  public void addVisitedAssume(AssumeEdge pEdge) {
    visitedAssumes.add(pEdge);
  }

  public void addExistingAssume(AssumeEdge pEdge) {
    allAssumes.add(pEdge);
  }

  public void addVisitedFunction(String pName, int pCount) {
    if (visitedFunctions.containsKey(pName)) {
      visitedFunctions.put(pName, visitedFunctions.get(pName) + pCount);
    } else {
      visitedFunctions.put(pName, pCount);
    }
  }

  public void addExistingFunction(String pName, int pFirstLine, int pLastLine) {
    allFunctions.add(new FunctionInfo(pName, pFirstLine, pLastLine));
  }

  public void addVisitedLine(int pLine) {
    checkArgument(pLine > 0);
    if (visitedLines.containsKey(pLine)) {
      visitedLines.put(pLine, visitedLines.get(pLine) + 1);
    } else {
      visitedLines.put(pLine, 1);
    }
  }

  public int getVisitedLine(int pLine) {
    checkArgument(pLine > 0);
    return visitedLines.containsKey(pLine) ? visitedLines.get(pLine) : 0;
  }

  public void addExistingLine(int pLine) {
    checkArgument(pLine > 0);
    allLines.add(pLine);
  }

}
