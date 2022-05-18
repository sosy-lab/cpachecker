// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2022 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.util.coverage.collectors;

import java.util.Map;
import java.util.Optional;
import org.sosy_lab.cpachecker.cfa.CFA;
import org.sosy_lab.cpachecker.cfa.ast.FileLocation;
import org.sosy_lab.cpachecker.cfa.model.AssumeEdge;
import org.sosy_lab.cpachecker.cfa.model.CFAEdge;
import org.sosy_lab.cpachecker.cfa.model.CFANode;
import org.sosy_lab.cpachecker.cfa.model.FunctionEntryNode;
import org.sosy_lab.cpachecker.util.CFAUtils;
import org.sosy_lab.cpachecker.util.coverage.data.FileCoverageStatistics;
import org.sosy_lab.cpachecker.util.coverage.measures.CoverageMeasureHandler;
import org.sosy_lab.cpachecker.util.coverage.tdcg.TimeDependentCoverageHandler;
import org.sosy_lab.cpachecker.util.coverage.util.CoverageUtility;

/**
 * Abstract Class Coverage Collector is used as basis functionality for every Coverage Collector
 * Implementation. It provides methods to save coverage data per file. And has access to the
 * coverage measures and time-dependent coverage graphs.
 */
public abstract class CoverageCollector {
  private final Map<String, FileCoverageStatistics> infosPerFile;
  final CoverageMeasureHandler coverageMeasureHandler;
  final TimeDependentCoverageHandler timeDependentCoverageHandler;

  CoverageCollector(
      Map<String, FileCoverageStatistics> pInfosPerFile,
      CoverageMeasureHandler pCoverageMeasureHandler,
      TimeDependentCoverageHandler pTimeDependentCoverageHandler,
      CFA cfa) {
    infosPerFile = pInfosPerFile;
    coverageMeasureHandler = pCoverageMeasureHandler;
    timeDependentCoverageHandler = pTimeDependentCoverageHandler;
    putCFA(cfa);
  }

  CoverageCollector(
      Map<String, FileCoverageStatistics> pInfosPerFile,
      CoverageMeasureHandler pCoverageMeasureHandler,
      TimeDependentCoverageHandler pTimeDependentCoverageHandler) {
    infosPerFile = pInfosPerFile;
    coverageMeasureHandler = pCoverageMeasureHandler;
    timeDependentCoverageHandler = pTimeDependentCoverageHandler;
  }

  public void putCFA(CFA pCFA) {
    for (CFANode node : pCFA.getAllNodes()) {
      // This part adds lines, which are only on edges, such as "return" or "goto"
      for (CFAEdge edge : CFAUtils.leavingEdges(node)) {
        putExistingEdge(edge);
      }
    }
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

  void putExistingEdge(final CFAEdge pEdge) {
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

  public void addExistingNodes(final CFA pCFA) {
    for (FileCoverageStatistics info : infosPerFile.values()) {
      info.allLocations.addAll(pCFA.getAllNodes());
    }
  }

  public void addVisitedFunction(FunctionEntryNode pEntryNode) {
    FileCoverageStatistics infos = getFileInfoTarget(pEntryNode.getFileLocation(), infosPerFile);
    infos.addVisitedFunction(pEntryNode.getFunctionName());
  }

  public FileCoverageStatistics getCollector(CFAEdge pEdge) {
    final FileLocation loc = pEdge.getFileLocation();
    return getFileInfoTarget(loc, infosPerFile);
  }

  Optional<FileCoverageStatistics> getCollectorForInitNode(CFANode pNode) {
    if (pNode.getNumLeavingEdges() > 0) {
      CFANode realNode = pNode.getLeavingEdge(0).getSuccessor();
      if (realNode.getNumLeavingEdges() > 0) {
        FileLocation loc = realNode.getLeavingEdge(0).getFileLocation();
        return Optional.of(getFileInfoTarget(loc, infosPerFile));
      }
    }
    return Optional.empty();
  }

  FileCoverageStatistics getFileInfoTarget(
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

  Map<String, FileCoverageStatistics> getInfosPerFile() {
    return infosPerFile;
  }
}
