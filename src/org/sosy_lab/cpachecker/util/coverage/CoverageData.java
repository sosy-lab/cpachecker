// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2007-2020 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.util.coverage;

import com.google.common.collect.Collections2;
import com.google.common.collect.ImmutableMultiset;
import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Optional;
import java.util.Set;
import org.sosy_lab.cpachecker.cfa.CFA;
import org.sosy_lab.cpachecker.cfa.ast.FileLocation;
import org.sosy_lab.cpachecker.cfa.model.AssumeEdge;
import org.sosy_lab.cpachecker.cfa.model.CFAEdge;
import org.sosy_lab.cpachecker.cfa.model.CFANode;
import org.sosy_lab.cpachecker.cfa.model.FunctionEntryNode;
import org.sosy_lab.cpachecker.cpa.coverage.AnalysisIndependentCoverageCPA;
import org.sosy_lab.cpachecker.util.CFAUtils;
import org.sosy_lab.cpachecker.util.coverage.measures.CoverageMeasureHandler;
import org.sosy_lab.cpachecker.util.coverage.measures.CoverageMeasureType;
import org.sosy_lab.cpachecker.util.coverage.report.FileCoverageStatistics;
import org.sosy_lab.cpachecker.util.coverage.report.FilePredicateCoverageStatistics;
import org.sosy_lab.cpachecker.util.coverage.tdcg.TimeDependentCoverageData;
import org.sosy_lab.cpachecker.util.coverage.tdcg.TimeDependentCoverageHandler;
import org.sosy_lab.cpachecker.util.coverage.tdcg.TimeDependentCoverageType;

public final class CoverageData {
  private Map<String, FileCoverageStatistics> infosPerFile = new LinkedHashMap<>();
  private final TimeDependentCoverageHandler timeDependentCoverageHandler;
  private final CoverageMeasureHandler coverageMeasureHandler;

  public CoverageData() {
    timeDependentCoverageHandler = new TimeDependentCoverageHandler();
    coverageMeasureHandler = new CoverageMeasureHandler();
  }

  public void initAnalysisIndependentStatisticHandlers() {
    timeDependentCoverageHandler.initAnalysisIndependentTDCG();
    coverageMeasureHandler.initAnalysisIndependentMeasures();
  }

  public void initPredicateAnalysisStatisticsHandlers() {
    timeDependentCoverageHandler.initPredicateAnalysisTDCG();
    coverageMeasureHandler.initPredicateAnalysisMeasures();
  }

  public TimeDependentCoverageHandler getTDCGHandler() {
    return timeDependentCoverageHandler;
  }

  public CoverageMeasureHandler getCoverageHandler() {
    return coverageMeasureHandler;
  }

  public Map<String, FilePredicateCoverageStatistics> getPredicateStatistics() {
    Map<String, FilePredicateCoverageStatistics> predicateInfosPerFile = new LinkedHashMap<>();
    for (Entry<String, FileCoverageStatistics> fileInfo : infosPerFile.entrySet()) {
      predicateInfosPerFile.put(fileInfo.getKey(), fileInfo.getValue().predicateStatistics);
    }
    return predicateInfosPerFile;
  }

  public void setPredicateStatistics(
      Map<String, FilePredicateCoverageStatistics> predicateInfosPerFile) {
    for (Entry<String, FileCoverageStatistics> fileInfo : infosPerFile.entrySet()) {
      fileInfo.getValue().predicateStatistics = predicateInfosPerFile.get(fileInfo.getKey());
    }
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
        timeDependentCoverageHandler.getData(TimeDependentCoverageType.PredicatesGenerated);
    if (timeStampsPerPredicateCoverage == null
        || timeStampsPerPredicateCoverage.getCoverageList().isEmpty()) {
      return 0.0;
    }
    return Collections.max(timeStampsPerPredicateCoverage.getCoverageList());
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

  public void addVisitedLocation(CFAEdge pEdge) {
    final FileLocation loc = pEdge.getFileLocation();
    final FileCoverageStatistics collector = getFileInfoTarget(loc, infosPerFile);
    collector.visitedLocations.add(pEdge.getSuccessor().getNodeNumber());
    if (!collector.visitedLocations.contains(pEdge.getPredecessor().getNodeNumber())) {
      collector.visitedLocations.add(pEdge.getPredecessor().getNodeNumber());
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
    collector.predicateStatistics.addPredicateConsideredNode(pEdge.getSuccessor());
  }

  public void addPredicateRelevantVariablesNodes(final CFAEdge pEdge) {
    if (!CoverageUtility.coversLine(pEdge)) {
      return;
    }
    FileCoverageStatistics collector = getCollector(pEdge);
    collector.predicateStatistics.addPredicateRelevantVariablesNodes(pEdge.getSuccessor());
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
      info.predicateStatistics.resetPredicateRelevantVariablesNodes();
    }
  }

  public void addInitialNodesForTDCG(
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
            case PredicateConsideredLocations:
              collector.predicateStatistics.addPredicateConsideredNode(candidateNode);
              tdcgData.addTimeStamp(getTempPredicateConsideredCoverage(cfa));
              break;
            case PredicateRelevantVariables:
              collector.predicateStatistics.addPredicateRelevantVariablesNodes(candidateNode);
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

  public void addInitialNodesForMeasures(CFA cfa) {
    for (var node : cfa.getAllNodes()) {
      if (node.getNodeNumber() == 1) {
        CFANode candidateNode = node;
        if (getCollectorForInitNode(candidateNode).isEmpty()) {
          return;
        }
        FileCoverageStatistics collector = getCollectorForInitNode(candidateNode).orElseThrow();
        do {
          for (var type : coverageMeasureHandler.getAllTypes()) {
            if (type == CoverageMeasureType.VisitedLocations) {
              collector.visitedLocations.add(candidateNode.getNodeNumber());
            }
          }
          candidateNode = candidateNode.getLeavingEdge(0).getSuccessor();
        } while (candidateNode.getNumLeavingEdges() == 1);
        break;
      }
    }
  }

  public double getTempPredicateConsideredCoverage(CFA cfa) {
    return getTempCoverage(cfa, TimeDependentCoverageType.PredicateConsideredLocations);
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
        case PredicateConsideredLocations:
          numRelevantNodes += info.predicateStatistics.allPredicateConsideredNodes.size();
          break;
        case PredicateRelevantVariables:
          numRelevantNodes += info.predicateStatistics.allPredicateRelevantVariablesNodes.size();
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

  public void addReachedNodes(final Collection<CFANode> nodes) {
    for (FileCoverageStatistics info : infosPerFile.values()) {
      info.allReachedNodes.addAll(
          nodes.stream()
              .map(v -> v.getNodeNumber())
              .collect(ImmutableMultiset.toImmutableMultiset()));
    }
  }

  public void addExistingNodes(final CFA pCFA) {
    for (FileCoverageStatistics info : infosPerFile.values()) {
      info.allNodes.addAll(Collections2.transform(pCFA.getAllNodes(), v -> v.getNodeNumber()));
    }
  }

  public void addVisitedFunction(FunctionEntryNode pEntryNode) {
    FileCoverageStatistics infos = getFileInfoTarget(pEntryNode.getFileLocation(), infosPerFile);
    infos.addVisitedFunction(pEntryNode.getFunctionName());
  }

  public Map<String, FileCoverageStatistics> getInfosPerFile() {
    return infosPerFile;
  }

  public void setInfosPerFile(Map<String, FileCoverageStatistics> pInfosPerFile) {
    infosPerFile = pInfosPerFile;
  }

  public void mergeInfosPerFile(CoverageData otherCoverageData) {
    otherCoverageData.setPredicateStatistics(getPredicateStatistics());
    setInfosPerFile(otherCoverageData.getInfosPerFile());
  }
}
