/*
 *  CPAchecker is a tool for configurable software verification.
 *  This file is part of CPAchecker.
 *
 *  Copyright (C) 2007-2017  Dirk Beyer
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
package org.sosy_lab.cpachecker.cpa.smg.refiner;

import java.io.IOException;
import java.nio.charset.Charset;
import java.nio.file.Path;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;
import org.sosy_lab.common.io.IO;
import org.sosy_lab.common.io.PathTemplate;
import org.sosy_lab.common.log.LogManager;
import org.sosy_lab.cpachecker.cfa.ast.c.CDeclaration;
import org.sosy_lab.cpachecker.cfa.ast.c.CFunctionDeclaration;
import org.sosy_lab.cpachecker.cfa.ast.c.CTypeDeclaration;
import org.sosy_lab.cpachecker.cfa.model.CFAEdge;
import org.sosy_lab.cpachecker.cfa.model.CFAEdgeType;
import org.sosy_lab.cpachecker.cfa.model.CFANode;
import org.sosy_lab.cpachecker.cfa.model.c.CDeclarationEdge;
import org.sosy_lab.cpachecker.cpa.arg.ARGState;
import org.sosy_lab.cpachecker.cpa.arg.path.ARGPath;
import org.sosy_lab.cpachecker.cpa.arg.path.PathIterator;
import org.sosy_lab.cpachecker.cpa.smg.SMGOptions.SMGExportLevel;
import org.sosy_lab.cpachecker.cpa.smg.SMGState;
import org.sosy_lab.cpachecker.cpa.smg.SMGUtils;

public class SMGPathInterpolationExporter {

  private final LogManager logger;
  private final PathTemplate exportPath;
  private final SMGExportLevel exportWhen;

  SMGPathInterpolationExporter(
      LogManager pLogger, PathTemplate pExportPath, SMGExportLevel pExportWhen) {
    logger = pLogger;
    exportPath = pExportPath;
    exportWhen = pExportWhen;
  }

  void exportInterpolation(
      ARGPath pErrorPath, Map<ARGState, SMGInterpolant> pInterpolants, int pInterpolationId) {

    if (exportWhen != SMGExportLevel.EVERY || exportPath == null) {
      return;
    }

    exportCFAPath(pErrorPath.getInnerEdges(), pInterpolationId);

    ARGState firstState = pErrorPath.getFirstState();

    if (pInterpolants.containsKey(firstState)) {
      SMGInterpolant firstInterpolant = pInterpolants.get(firstState);
      exportFirstInterpolant(firstInterpolant, pInterpolationId);
    }

    PathIterator pathIterator = pErrorPath.pathIterator();

    int pathIndex = 2;

    while (pathIterator.advanceIfPossible()) {
      ARGState currentARGState = pathIterator.getAbstractState();

      if (!pInterpolants.containsKey(currentARGState)) {
        pathIndex = pathIndex + 1;
        continue;
      }

      SMGInterpolant currentInterpolant = pInterpolants.get(currentARGState);
      CFANode currentLocation = pathIterator.getLocation();
      CFAEdge currentIncomingEdge = pathIterator.getIncomingEdge();
      exportInterpolant(
          currentInterpolant, currentLocation, currentIncomingEdge, pInterpolationId, pathIndex);
      pathIndex = pathIndex + 1;
    }
  }

  private void exportFirstInterpolant(SMGInterpolant pFirstInterpolant, int pInterpolationId) {

    if (pFirstInterpolant.isFalse()) {
      return;
    }

    Collection<SMGState> states = pFirstInterpolant.reconstructStates();

    int counter = 1;
    for (SMGState state : states) {
      String fileName = "smgInterpolant-1-smg-" + (counter++) + ".dot";
      Path path = exportPath.getPath(pInterpolationId, fileName);
      String name = "First interpolant";
      SMGUtils.dumpSMGPlot(logger, state, name, path);
    }
  }

  private void exportInterpolant(
      SMGInterpolant pCurrentInterpolant,
      CFANode pCurrentLocation,
      CFAEdge pIncomingEdge,
      int pInterpolationId,
      int pPathIndex) {

    if (!isImportantEdge(pIncomingEdge) || pCurrentInterpolant.isFalse()) {
      return;
    }

    Collection<SMGState> states = pCurrentInterpolant.reconstructStates();

    int counter = 1;
    for (SMGState state : states) {
      String fileName = "smgInterpolant-" + pPathIndex + "-smg-" + (counter++) + ".dot";
      Path path = exportPath.getPath(pInterpolationId, fileName);
      String location = pIncomingEdge.toString() + " on N" + pCurrentLocation.getNodeNumber();
      SMGUtils.dumpSMGPlot(logger, state, location, path);
    }
  }

  private boolean isImportantEdge(CFAEdge edge) {
    if (edge.getEdgeType() == CFAEdgeType.BlankEdge) {
      return false;
    }
    if (edge.getEdgeType() == CFAEdgeType.DeclarationEdge) {
      CDeclaration cDcl = ((CDeclarationEdge) edge).getDeclaration();
      if (cDcl instanceof CFunctionDeclaration || cDcl instanceof CTypeDeclaration) {
        return false;
      }
    }
    return true;
  }

  private void exportCFAPath(List<CFAEdge> pFullPath, int pInterpolationId) {

    StringBuilder interpolationPath = new StringBuilder();
    for (CFAEdge edge : pFullPath) {
      if (isImportantEdge(edge)) {
        interpolationPath.append(edge).append("\n");
      }
    }

    Path path = exportPath.getPath(pInterpolationId, "interpolationPath.txt");

    try {
      IO.writeFile(path, Charset.defaultCharset(), interpolationPath.toString());
    } catch (IOException e) {
      logger.logUserException(
          Level.WARNING, e, "Failed to write interpolation path to path " + path.toString());
    }
  }
}
