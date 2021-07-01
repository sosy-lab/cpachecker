// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2007-2020 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.util.coverage;

import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.Map;
import org.sosy_lab.common.time.TimeSpan;
import org.sosy_lab.cpachecker.cfa.CFA;
import org.sosy_lab.cpachecker.cfa.ast.AFunctionDeclaration;
import org.sosy_lab.cpachecker.cfa.ast.FileLocation;
import org.sosy_lab.cpachecker.cfa.model.ADeclarationEdge;
import org.sosy_lab.cpachecker.cfa.model.AReturnStatementEdge;
import org.sosy_lab.cpachecker.cfa.model.AssumeEdge;
import org.sosy_lab.cpachecker.cfa.model.CFAEdge;
import org.sosy_lab.cpachecker.cfa.model.CFANode;
import org.sosy_lab.cpachecker.cfa.model.FunctionEntryNode;
import org.sosy_lab.cpachecker.cpa.smg.graphs.value.SMGKnownExpValue;
import org.sosy_lab.cpachecker.cpa.smg.util.PersistentMultimap;
import org.sosy_lab.cpachecker.util.CFAUtils;

public final class CoverageData {

  private final Map<String, FileCoverageInformation> infosPerFile = new LinkedHashMap<>();

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

    if (pEdge instanceof ADeclarationEdge) {
      String name = ((ADeclarationEdge) pEdge).getDeclaration().getName();
      if (name != null && name.contains("__CPAchecker_TMP_")) {
        // Avoid report temporal variable declaration as covered source code
        return false;
      }
    }

    if (pEdge instanceof AReturnStatementEdge && pEdge.toString().contains("__CPAchecker_TMP_")) {
      // Avoid doubling of code coverage as return temporal variable value
      return false;
    }

    return true;
  }

  private FileCoverageInformation getFileInfoTarget(
      final FileLocation pLoc, final Map<String, FileCoverageInformation> pTargets) {

    assert pLoc.getStartingLineNumber()
        != 0; // Cannot produce coverage info for dummy file location

    String file = pLoc.getFileName();
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

    Iterator<String> sourceCode = pEdge.getRawStatement().lines().iterator();
    for (int line = startingLine; line <= endingLine; line++) {
      collector.addExistingLine(line);
      if (sourceCode.hasNext()) {
        collector.addSourceCode(line, sourceCode.next());
      }
    }

    if (pEdge instanceof AssumeEdge) {
      collector.addExistingAssume((AssumeEdge) pEdge);
    }
  }

  public void addInfoOnEdge(
      final CFAEdge pEdge, PersistentMultimap<String, SMGKnownExpValue> pInfo) {
    final FileLocation loc = pEdge.getFileLocation();
    final FileCoverageInformation collector = getFileInfoTarget(loc, infosPerFile);
    final int endingLine = loc.getEndingLineInOrigin();
    collector.addAdditionalInfo(endingLine, pInfo);
  }

  public void addCounterInfoOnEdge(final CFAEdge pEdge, Integer pCounter, TimeSpan pTimeSpan) {
    final FileLocation loc = pEdge.getFileLocation();
    final FileCoverageInformation collector = getFileInfoTarget(loc, infosPerFile);
    final int endingLine = loc.getEndingLineInOrigin();
    collector.addCounterInfo(endingLine, pCounter, pTimeSpan);
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

  public void addVisitedFunction(FunctionEntryNode pEntryNode) {
    FileCoverageInformation infos = getFileInfoTarget(pEntryNode.getFileLocation(), infosPerFile);
    infos.addVisitedFunction(pEntryNode.getFunctionName());
  }

  Map<String, FileCoverageInformation> getInfosPerFile() {
    return infosPerFile;
  }
}
