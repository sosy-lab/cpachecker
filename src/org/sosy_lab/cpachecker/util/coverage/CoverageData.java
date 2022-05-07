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
import java.util.Set;
import org.sosy_lab.cpachecker.cfa.CFA;
import org.sosy_lab.cpachecker.cfa.ast.FileLocation;
import org.sosy_lab.cpachecker.cfa.model.AssumeEdge;
import org.sosy_lab.cpachecker.cfa.model.CFAEdge;
import org.sosy_lab.cpachecker.cfa.model.CFANode;
import org.sosy_lab.cpachecker.cfa.model.FunctionEntryNode;
import org.sosy_lab.cpachecker.util.CFAUtils;
import org.sosy_lab.cpachecker.util.coverage.measures.CoverageMeasureHandler;
import org.sosy_lab.cpachecker.util.coverage.report.FileCoverageStatistics;
import org.sosy_lab.cpachecker.util.coverage.tdcg.TimeDependentCoverageData;
import org.sosy_lab.cpachecker.util.coverage.tdcg.TimeDependentCoverageHandler;
import org.sosy_lab.cpachecker.util.coverage.tdcg.TimeDependentCoverageType;

public final class CoverageData {
  private final Map<String, FileCoverageStatistics> infosPerFile = new LinkedHashMap<>();
  private final TimeDependentCoverageHandler timeDependentCoverageHandler;
  private final CoverageMeasureHandler coverageMeasureHandler;

  public CoverageData() {
    timeDependentCoverageHandler = new TimeDependentCoverageHandler();
    coverageMeasureHandler = new CoverageMeasureHandler();
    timeDependentCoverageHandler.initAllTDCG();
    coverageMeasureHandler.initAllCoverageMeasures();
  }

  public TimeDependentCoverageHandler getTDCGHandler() {
    return timeDependentCoverageHandler;
  }

  public CoverageMeasureHandler getCoverageHandler() {
    return coverageMeasureHandler;
  }

  public FileCoverageStatistics getCollector(CFAEdge pEdge) {
    final FileLocation loc = pEdge.getFileLocation();
    return getFileInfoTarget(loc, infosPerFile);
  }

  private Optional<FileCoverageStatistics> getCollectorForInitNode(CFANode pNode) {
    if (pNode.getNumLeavingEdges() > 0) {
      CFANode realNode = pNode.getLeavingEdge(0).getSuccessor();
      if (realNode.getNumLeavingEdges() > 0) {
        FileLocation loc = realNode.getLeavingEdge(0).getFileLocation();
        return Optional.of(getFileInfoTarget(loc, infosPerFile));
      }
    }
    return Optional.empty();
  }

  private FileCoverageStatistics getFileInfoTarget(
      final FileLocation pLoc, final Map<String, FileCoverageStatistics> pTargets) {

    // Cannot produce coverage info for dummy file location
    assert pLoc.getStartingLineNumber() != 0;

    String file = pLoc.getFileName().toString();
    FileCoverageStatistics fileInfos = pTargets.get(file);

    if (fileInfos == null) {
      fileInfos = new FileCoverageStatistics();
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

    final FileCoverageStatistics infos = getFileInfoTarget(loc, infosPerFile);

    final int startingLine = loc.getStartingLineInOrigin();
    final int endingLine = loc.getEndingLineInOrigin();

    infos.addExistingFunction(functionName, startingLine, endingLine);
  }

  private void putExistingEdge(final CFAEdge pEdge) {
    if (!CoverageUtility.coversLine(pEdge)) {
      return;
    }
    final FileLocation loc = pEdge.getFileLocation();
    final FileCoverageStatistics collector = getFileInfoTarget(loc, infosPerFile);

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
    final FileCoverageStatistics collector = getFileInfoTarget(loc, infosPerFile);

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
    for (FileCoverageStatistics info : getInfosPerFile().values()) {
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
    FileCoverageStatistics collector = getCollector(pEdge);
    collector.addPredicateConsideredNode(pEdge.getSuccessor());
  }

  public void addPredicateRelevantVariablesNodes(final CFAEdge pEdge) {
    if (!CoverageUtility.coversLine(pEdge)) {
      return;
    }
    FileCoverageStatistics collector = getCollector(pEdge);
    collector.addPredicateRelevantVariablesNodes(pEdge.getSuccessor());
  }

  public void addAbstractStateCoveredNodes(final Set<CFANode> nodes, final CFAEdge pEdge) {
    if (!CoverageUtility.coversLine(pEdge)) {
      return;
    }
    FileCoverageStatistics collector = getCollector(pEdge);
    collector.addAbstractStateCoveredNodes(nodes);
  }

  public void resetPredicateRelevantVariablesNodes() {
    for (FileCoverageStatistics info : getInfosPerFile().values()) {
      info.resetPredicateRelevantVariablesNodes();
    }
  }

  public void addInitialNodes(
      CFA cfa, TimeDependentCoverageData tdcgData, TimeDependentCoverageType type) {
    for (var node : cfa.getAllNodes()) {
      if (node.getNodeNumber() == 1) {
        CFANode candidateNode = node;
        if (getCollectorForInitNode(candidateNode).isEmpty()) {
          return;
        }
        FileCoverageStatistics collector = getCollectorForInitNode(candidateNode).orElseThrow();
        do {
          switch (type) {
            case PredicateConsidered:
              collector.addPredicateConsideredNode(candidateNode);
              tdcgData.addTimeStamp(getTempPredicateConsideredCoverage(cfa));
              break;
            case PredicateRelevantVariables:
              collector.addPredicateRelevantVariablesNodes(candidateNode);
              tdcgData.addTimeStamp(getTempPredicateRelevantVariablesCoverage(cfa));
              break;
            default:
              break;
          }
          candidateNode = candidateNode.getLeavingEdge(0).getSuccessor();
        } while (candidateNode.getNumLeavingEdges() == 1);
        break;
      }
    }
  }

  public double getTempPredicateConsideredCoverage(CFA cfa) {
    return getTempCoverage(cfa, TimeDependentCoverageType.PredicateConsidered);
  }

  public double getTempAbstractStateCoveredNodesCoverage(CFA cfa) {
    return getTempCoverage(cfa, TimeDependentCoverageType.AbstractStateCoveredNodes);
  }

  public double getTempPredicateRelevantVariablesCoverage(CFA cfa) {
    return getTempCoverage(cfa, TimeDependentCoverageType.PredicateRelevantVariables);
  }

  private double getTempCoverage(CFA cfa, TimeDependentCoverageType type) {
    int numTotalNodes = cfa.getAllNodes().size();
    int numRelevantNodes = 0;
    for (FileCoverageStatistics info : getInfosPerFile().values()) {
      switch (type) {
        case PredicateConsidered:
          numRelevantNodes += info.allPredicateConsideredNodes.size();
          break;
        case PredicateRelevantVariables:
          numRelevantNodes += info.allPredicateRelevantVariablesNodes.size();
          break;
        case AbstractStateCoveredNodes:
          numRelevantNodes += info.allAbstractStateCoveredNodes.size();
          break;
        default:
          break;
      }
    }
    return numRelevantNodes / (double) numTotalNodes;
  }

  public void addConsideredNodes(
      final Collection<CFANode> nodes, final FileCoverageStatistics collector) {
    for (CFANode node : nodes) {
      collector.addConsideredNode(node.getNodeNumber());
    }
  }

  public void addExistingNodes(
      final Collection<CFANode> nodes, final FileCoverageStatistics collector) {
    for (CFANode node : nodes) {
      collector.addExistingNode(node.getNodeNumber());
    }
  }

  public void addVisitedFunction(FunctionEntryNode pEntryNode) {
    FileCoverageStatistics infos = getFileInfoTarget(pEntryNode.getFileLocation(), infosPerFile);
    infos.addVisitedFunction(pEntryNode.getFunctionName());
  }

  public Map<String, FileCoverageStatistics> getInfosPerFile() {
    return infosPerFile;
  }
}
