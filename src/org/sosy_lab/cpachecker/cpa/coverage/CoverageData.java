/*
 *  CPAchecker is a tool for configurable software verification.
 *  This file is part of CPAchecker.
 *
 *  Copyright (C) 2007-2015  Dirk Beyer
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
package org.sosy_lab.cpachecker.cpa.coverage;

import static com.google.common.base.Preconditions.checkArgument;

import org.sosy_lab.cpachecker.cfa.ast.AFunctionDeclaration;
import org.sosy_lab.cpachecker.cfa.ast.FileLocation;
import org.sosy_lab.cpachecker.cfa.model.ADeclarationEdge;
import org.sosy_lab.cpachecker.cfa.model.AssumeEdge;
import org.sosy_lab.cpachecker.cfa.model.CFAEdge;
import org.sosy_lab.cpachecker.cfa.model.FunctionEntryNode;

import java.util.BitSet;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;


class CoverageData {

  public enum CoverageMode { NONE, REACHED, TRANSFER }

  private final Map<String, FileCoverage> infosPerFile;
  private final CoverageMode coverageMode;

  public CoverageData(CoverageMode pMode) {
    this.infosPerFile = new HashMap<>();
    this.coverageMode = pMode;
  }

  public CoverageMode getCoverageMode() {
    return coverageMode;
  }

  FileCoverage getFileInfoTarget(
      final FileLocation pLoc,
      final Map<String, FileCoverage> pTargets) {

    assert pLoc.getStartingLineNumber() != 0; // Cannot produce coverage info for dummy file location

    String file = pLoc.getFileName();
    FileCoverage fileInfos = pTargets.get(file);

    if (fileInfos == null) {
      fileInfos = new FileCoverage();
      pTargets.put(file, fileInfos);
    }

    return fileInfos;
  }

  boolean putExistingFunction(FunctionEntryNode pNode) {
    final String functionName = pNode.getFunctionName();
    final FileLocation loc = pNode.getFileLocation();

    if (loc.getStartingLineNumber() == 0) {
      // dummy location
      return false;
    }

    final FileCoverage infos = getFileInfoTarget(loc, infosPerFile);

    final int startingLine = loc.getStartingLineInOrigin();
    final int endingLine = loc.getEndingLineInOrigin();

    infos.addExistingFunction(functionName, startingLine, endingLine);

    return true;
  }

  void handleEdgeCoverage(
      final CFAEdge pEdge,
      final boolean pVisited) {

    final FileLocation loc = pEdge.getFileLocation();
    if (loc.getStartingLineNumber() == 0) {
      // dummy location
      return;
    }
    if (pEdge instanceof ADeclarationEdge
        && (((ADeclarationEdge)pEdge).getDeclaration() instanceof AFunctionDeclaration)) {
      // Function declarations span the complete body, this is not desired.
      return;
    }

    final FileCoverage collector = getFileInfoTarget(loc, infosPerFile);

    final int startingLine = loc.getStartingLineInOrigin();
    final int endingLine = loc.getEndingLineInOrigin();

    for (int line = startingLine; line <= endingLine; line++) {
      collector.addExistingLine(line);
    }

    if (pEdge instanceof AssumeEdge) {
      if (pVisited) {
        collector.addExistingAssume((AssumeEdge) pEdge);
        collector.addVisitedAssume((AssumeEdge) pEdge);
      } else {
        collector.addExistingAssume((AssumeEdge) pEdge);
      }
    }

    if (pVisited) {
      for (int line = startingLine; line <= endingLine; line++) {
        collector.addVisitedLine(line);
      }
    }
  }

  public void addVisitedFunction(FunctionEntryNode pEntryNode) {
    FileCoverage infos = getFileInfoTarget(pEntryNode.getFileLocation(), infosPerFile);
    infos.addVisitedFunction(pEntryNode.getFunctionName());
  }

  public Map<String, FileCoverage> getInfosPerFile() {
    return infosPerFile;
  }

  public static class FileCoverage {

    public static class FunctionInfo {
      final String name;
      final int firstLine;
      final int lastLine;

      FunctionInfo(String pName, int pFirstLine, int pLastLine) {
        name = pName;
        firstLine = pFirstLine;
        lastLine = pLastLine;
      }
    }

    final BitSet visitedLines = new BitSet();
    final Set<Integer> allLines = new HashSet<>();
    final Set<String> visitedFunctions = new HashSet<>();
    final Set<FunctionInfo> allFunctions = new HashSet<>();
    final Set<AssumeEdge> allAssumes = new HashSet<>();
    final Set<AssumeEdge> visitedAssumes = new HashSet<>();

    public void addVisitedAssume(AssumeEdge pEdge) {
      visitedAssumes.add(pEdge);
    }

    public void addExistingAssume(AssumeEdge pEdge) {
      allAssumes.add(pEdge);
    }

    public void addVisitedFunction(String pName) {
      visitedFunctions.add(pName);
    }

    public void addExistingFunction(String pName, int pFirstLine, int pLastLine) {
      allFunctions.add(new FunctionInfo(pName, pFirstLine, pLastLine));
    }

    public void addVisitedLine(int pLine) {
      checkArgument(pLine > 0);
      visitedLines.set(pLine);
    }

    public void addExistingLine(int pLine) {
      checkArgument(pLine > 0);
      allLines.add(pLine);
    }

  }

}
