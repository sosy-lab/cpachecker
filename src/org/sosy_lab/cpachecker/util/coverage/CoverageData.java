// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2007-2020 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.util.coverage;

import com.google.common.collect.ImmutableList;
import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;
import org.sosy_lab.cpachecker.cfa.CFA;
import org.sosy_lab.cpachecker.cfa.ast.FileLocation;
import org.sosy_lab.cpachecker.cfa.model.AssumeEdge;
import org.sosy_lab.cpachecker.cfa.model.CFAEdge;
import org.sosy_lab.cpachecker.cfa.model.CFANode;
import org.sosy_lab.cpachecker.cfa.model.FunctionEntryNode;
import org.sosy_lab.cpachecker.util.CFAUtils;
import org.sosy_lab.cpachecker.util.coverage.tdcg.TimeDependentCoverageData;
import org.sosy_lab.cpachecker.util.coverage.tdcg.TimeDependentCoverageHandler;
import org.sosy_lab.cpachecker.util.coverage.tdcg.TimeDependentCoverageType;

public final class CoverageData {
  private final Map<String, FileCoverageInformation> infosPerFile = new LinkedHashMap<>();
  private final TimeDependentCoverageHandler timeDependentCoverageHandler;

  public CoverageData() {
    timeDependentCoverageHandler = new TimeDependentCoverageHandler();
    timeDependentCoverageHandler.initNewData(TimeDependentCoverageType.Visited);
    timeDependentCoverageHandler.initNewData(TimeDependentCoverageType.Predicate);
    timeDependentCoverageHandler.initNewData(TimeDependentCoverageType.PredicateConsidered);
  }

  public TimeDependentCoverageHandler getTDCGHandler() {
    return timeDependentCoverageHandler;
  }

  public FileCoverageInformation getCollector(CFAEdge pEdge) {
    final FileLocation loc = pEdge.getFileLocation();
    return getFileInfoTarget(loc, infosPerFile);
  }

  private Optional<FileCoverageInformation> getCollectorForInitNode(CFANode pNode) {
    if (pNode.getNumLeavingEdges() > 0) {
      CFANode realNode = pNode.getLeavingEdge(0).getSuccessor();
      if (realNode.getNumLeavingEdges() > 0) {
        FileLocation loc = realNode.getLeavingEdge(0).getFileLocation();
        return Optional.of(getFileInfoTarget(loc, infosPerFile));
      }
    }
    return Optional.empty();
  }

  private FileCoverageInformation getFileInfoTarget(
      final FileLocation pLoc, final Map<String, FileCoverageInformation> pTargets) {

    // Cannot produce coverage info for dummy file location
    assert pLoc.getStartingLineNumber() != 0;

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
    if (!CoverageUtility.coversLine(pEdge)) {
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

  public double getPredicateCoverage() {
    TimeDependentCoverageData timeStampsPerPredicateCoverage =
        timeDependentCoverageHandler.getData(TimeDependentCoverageType.Predicate);
    ImmutableList<Double> coverageList = timeStampsPerPredicateCoverage.getCoverageList();
    if (coverageList.isEmpty()) {
      return 0.0;
    }
    return Collections.max(coverageList);
  }

  public void addVisitedEdge(final CFAEdge pEdge) {
    if (!CoverageUtility.coversLine(pEdge)) {
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

  public double getTempVisitedCoverage() {
    int numTotalLines = 0;
    int numVisitedLines = 0;
    for (FileCoverageInformation info : getInfosPerFile().values()) {
      numTotalLines += info.allLines.size();
      numVisitedLines += info.visitedLines.entrySet().size();
    }
    double visitedLinesCoverage = 0.0;
    if (numTotalLines > 0) {
      visitedLinesCoverage = numVisitedLines / (double) numTotalLines;
    }
    return visitedLinesCoverage;
  }

  public void addPredicateConsideredNode(final CFAEdge pEdge) {
    if (!CoverageUtility.coversLine(pEdge)) {
      return;
    }
    FileCoverageInformation collector = getCollector(pEdge);
    collector.addPredicateConsideredNode(pEdge.getPredecessor());
    collector.addPredicateConsideredNode(pEdge.getSuccessor());
  }

  public void addInitialNodes(CFA cfa, TimeDependentCoverageData tdcgData) {
    for (var node : cfa.getAllNodes()) {
      if (node.getNodeNumber() == 1) {
        CFANode candidateNode = node;
        if (getCollectorForInitNode(candidateNode).isEmpty()) {
          return;
        }
        FileCoverageInformation collector = getCollectorForInitNode(candidateNode).orElseThrow();
        do {
          collector.addPredicateConsideredNode(candidateNode);
          tdcgData.addTimeStamp(getTempPredicateConsideredCoverage(cfa));
          candidateNode = candidateNode.getLeavingEdge(0).getSuccessor();
        } while (candidateNode.getNumLeavingEdges() == 1);
        break;
      }
    }
  }

  public double getTempPredicateConsideredCoverage(CFA cfa) {
    int numTotalNodes = cfa.getAllNodes().size();
    int numPredicateConsideredNodes = 0;
    for (FileCoverageInformation info : getInfosPerFile().values()) {
      numPredicateConsideredNodes += info.numPredicateConsideredNodes.size();
    }
    return numPredicateConsideredNodes / (double) numTotalNodes;
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

  public Map<String, FileCoverageInformation> getInfosPerFile() {
    return infosPerFile;
  }
}
