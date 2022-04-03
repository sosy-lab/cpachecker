// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2007-2020 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.util.coverage;

import java.time.Duration;
import java.time.Instant;
import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import org.sosy_lab.cpachecker.cfa.CFA;
import org.sosy_lab.cpachecker.cfa.ast.AFunctionDeclaration;
import org.sosy_lab.cpachecker.cfa.ast.FileLocation;
import org.sosy_lab.cpachecker.cfa.model.ADeclarationEdge;
import org.sosy_lab.cpachecker.cfa.model.AssumeEdge;
import org.sosy_lab.cpachecker.cfa.model.CFAEdge;
import org.sosy_lab.cpachecker.cfa.model.CFANode;
import org.sosy_lab.cpachecker.cfa.model.FunctionEntryNode;
import org.sosy_lab.cpachecker.util.CFAUtils;

public final class CoverageData {

  private final Map<String, FileCoverageInformation> infosPerFile = new LinkedHashMap<>();
  private final Map<Long, Double> timeStampsPerCoverage;
  private final Map<Long, Double> timeStampsPerPredicateCoverage;

  public CoverageData() {
    timeStampsPerCoverage = new LinkedHashMap<>();
    timeStampsPerPredicateCoverage = new LinkedHashMap<>();
    timeStampsPerCoverage.put(0L, 0.0);
    timeStampsPerPredicateCoverage.put(0L, 0.0);
  }

  public CoverageData(
      Map<Long, Double> pTimeStampsPerCoverage, Map<Long, Double> pTimeStampsPerPredicateCoverage) {
    timeStampsPerCoverage = pTimeStampsPerCoverage;
    timeStampsPerPredicateCoverage = pTimeStampsPerPredicateCoverage;
  }

  public static boolean coversLine(CFAEdge pEdge) {
    FileLocation loc = pEdge.getFileLocation();
    if (loc.getStartingLineNumber() == 0) {
      // dummy location
      return false;
    }
    if (pEdge instanceof ADeclarationEdge
        && (((ADeclarationEdge) pEdge).getDeclaration() instanceof AFunctionDeclaration)) {
      // Function declarations span the complete body, this is not desired.
      return false;
    }
    return true;
  }

  private FileCoverageInformation getFileInfoTarget(
      final FileLocation pLoc, final Map<String, FileCoverageInformation> pTargets) {

    assert pLoc.getStartingLineNumber()
        != 0; // Cannot produce coverage info for dummy file location

    String file = pLoc.getFileName().toString();
    FileCoverageInformation fileInfos = pTargets.get(file);

    if (fileInfos == null) {
      fileInfos = new FileCoverageInformation();
      pTargets.put(file, fileInfos);
    }

    return fileInfos;
  }

  public void putCFA(CFA pCFA) {
    // ------------ Existing lines ----------------
    for (CFANode node : pCFA.getAllNodes()) {
      // This part adds lines, which are only on edges, such as "return" or "goto"
      for (CFAEdge edge : CFAUtils.leavingEdges(node)) {
        putExistingEdge(edge);
      }
    }

    // ------------ Existing functions -------------
    for (FunctionEntryNode entryNode : pCFA.getAllFunctionHeads()) {
      putExistingFunction(entryNode);
    }
  }

  private void putExistingFunction(FunctionEntryNode pNode) {
    final String functionName = pNode.getFunctionName();
    final FileLocation loc = pNode.getFileLocation();

    if (loc.getStartingLineNumber() == 0) {
      // dummy location
      return;
    }

    final FileCoverageInformation infos = getFileInfoTarget(loc, infosPerFile);

    final int startingLine = loc.getStartingLineInOrigin();
    final int endingLine = loc.getEndingLineInOrigin();

    infos.addExistingFunction(functionName, startingLine, endingLine);
  }

  private void putExistingEdge(final CFAEdge pEdge) {
    if (!coversLine(pEdge)) {
      return;
    }
    final FileLocation loc = pEdge.getFileLocation();
    final FileCoverageInformation collector = getFileInfoTarget(loc, infosPerFile);

    final int startingLine = loc.getStartingLineInOrigin();
    final int endingLine = loc.getEndingLineInOrigin();

    for (int line = startingLine; line <= endingLine; line++) {
      collector.addExistingLine(line);
    }

    if (pEdge instanceof AssumeEdge) {
      collector.addExistingAssume((AssumeEdge) pEdge);
    }
  }

  public void addTimeStamp(final CFAEdge pEdge, Instant startTime) {
    if (!coversLine(pEdge)) {
      return;
    }
    final FileLocation loc = pEdge.getFileLocation();
    final FileCoverageInformation collector = getFileInfoTarget(loc, infosPerFile);
    int numTotalLines = collector.allLines.size();
    int numVisitedLines = collector.visitedLines.entrySet().size();
    double visitedLinesCoverage = 0.0;
    if (numTotalLines > 0) {
      visitedLinesCoverage = numVisitedLines / (double) numTotalLines;
    }
    long durationInNanos = Duration.between(startTime, Instant.now()).toNanos();
    long durationInMicros = TimeUnit.NANOSECONDS.toMicros(durationInNanos);
    timeStampsPerCoverage.put(durationInMicros, visitedLinesCoverage);
  }

  public Map<Long, Double> getTimeStampsPerCoverageMap() {
    return timeStampsPerCoverage;
  }

  public Map<Long, Double> getTimeStampsPerPredicateCoverage() {
    return timeStampsPerPredicateCoverage;
  }

  public double getPredicateCoverage() {
    return Collections.max(timeStampsPerPredicateCoverage.values());
  }

  public void addVisitedEdge(final CFAEdge pEdge) {
    if (!coversLine(pEdge)) {
      return;
    }
    putExistingEdge(pEdge);

    final FileLocation loc = pEdge.getFileLocation();
    final FileCoverageInformation collector = getFileInfoTarget(loc, infosPerFile);

    final int startingLine = loc.getStartingLineInOrigin();
    final int endingLine = loc.getEndingLineInOrigin();

    if (pEdge instanceof AssumeEdge) {
      collector.addVisitedAssume((AssumeEdge) pEdge);
    }

    for (int line = startingLine; line <= endingLine; line++) {
      collector.addVisitedLine(line);
    }
  }

  public void addConsideredNodes(
      final Collection<CFANode> nodes, final FileCoverageInformation collector) {
    for (CFANode node : nodes) {
      collector.addConsideredNode(node.getNodeNumber());
    }
  }

  public void addExistingNodes(
      final Collection<CFANode> nodes, final FileCoverageInformation collector) {
    for (CFANode node : nodes) {
      collector.addExistingNode(node.getNodeNumber());
    }
  }

  public void addVisitedFunction(FunctionEntryNode pEntryNode) {
    FileCoverageInformation infos = getFileInfoTarget(pEntryNode.getFileLocation(), infosPerFile);
    infos.addVisitedFunction(pEntryNode.getFunctionName());
  }

  Map<String, FileCoverageInformation> getInfosPerFile() {
    return infosPerFile;
  }
}
