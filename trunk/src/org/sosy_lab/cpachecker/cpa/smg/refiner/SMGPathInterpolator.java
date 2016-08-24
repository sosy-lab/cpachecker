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
package org.sosy_lab.cpachecker.cpa.smg.refiner;

import org.sosy_lab.common.ShutdownNotifier;
import org.sosy_lab.common.io.MoreFiles;
import org.sosy_lab.common.io.PathTemplate;
import org.sosy_lab.common.log.LogManager;
import org.sosy_lab.cpachecker.cfa.ast.c.CDeclaration;
import org.sosy_lab.cpachecker.cfa.ast.c.CFunctionDeclaration;
import org.sosy_lab.cpachecker.cfa.ast.c.CTypeDeclaration;
import org.sosy_lab.cpachecker.cfa.model.CFAEdge;
import org.sosy_lab.cpachecker.cfa.model.CFAEdgeType;
import org.sosy_lab.cpachecker.cfa.model.CFANode;
import org.sosy_lab.cpachecker.cfa.model.c.CDeclarationEdge;
import org.sosy_lab.cpachecker.cpa.arg.ARGPath;
import org.sosy_lab.cpachecker.cpa.arg.ARGPath.PathIterator;
import org.sosy_lab.cpachecker.cpa.arg.ARGReachedSet;
import org.sosy_lab.cpachecker.cpa.arg.ARGState;
import org.sosy_lab.cpachecker.cpa.smg.SMGCPA.SMGExportLevel;
import org.sosy_lab.cpachecker.cpa.smg.SMGState;
import org.sosy_lab.cpachecker.cpa.smg.SMGUtils;
import org.sosy_lab.cpachecker.exceptions.CPAException;
import org.sosy_lab.cpachecker.util.refinement.EdgeInterpolator;
import org.sosy_lab.cpachecker.util.refinement.Interpolant;
import org.sosy_lab.cpachecker.util.refinement.InterpolationTree;
import org.sosy_lab.cpachecker.util.statistics.StatCounter;
import org.sosy_lab.cpachecker.util.statistics.StatInt;
import org.sosy_lab.cpachecker.util.statistics.StatKind;

import java.io.IOException;
import java.nio.charset.Charset;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.logging.Level;

public class SMGPathInterpolator {

  /**
   * the offset in the path from where to cut-off the subtree, and restart the analysis
   */
  protected int interpolationOffset = -1;

  /**
   * Generate unique id for path interpolations.
   */
  private final static AtomicInteger idGenerator = new AtomicInteger(0);

  private final StatCounter totalInterpolations   = new StatCounter("Number of interpolations");
  private final StatInt totalInterpolationQueries = new StatInt(StatKind.SUM, "Number of interpolation queries");

  private final ShutdownNotifier shutdownNotifier;
  private final SMGEdgeInterpolator interpolator;
  private final SMGInterpolantManager interpolantManager;
  private final SMGFeasibilityChecker checker;

  private final LogManager logger;
  private final PathTemplate exportPath;
  private final SMGExportLevel exportWhen;

  public SMGPathInterpolator(ShutdownNotifier pShutdownNotifier,
      SMGInterpolantManager pInterpolantManager,
      SMGEdgeInterpolator pInterpolator, LogManager pLogger,
      PathTemplate pExportPath, SMGExportLevel pExportWhen, SMGFeasibilityChecker pChecker) {
    shutdownNotifier = pShutdownNotifier;
    interpolantManager = pInterpolantManager;
    interpolator = pInterpolator;
    logger = pLogger;
    exportPath = pExportPath;
    exportWhen = pExportWhen;
    checker = pChecker;
  }

  public Map<ARGState, SMGInterpolant> performInterpolation(ARGPath pErrorPath,
      SMGInterpolant pInterpolant, ARGReachedSet pReachedSet) throws InterruptedException, CPAException {
    totalInterpolations.inc();

    int interpolationId = idGenerator.incrementAndGet();

    logger.log(Level.ALL, "Start interpolating path with interpolation id ", interpolationId);

    interpolationOffset = -1;

    Map<ARGState, SMGInterpolant> interpolants =
        performEdgeBasedInterpolation(pErrorPath, pInterpolant, pReachedSet);

    propagateFalseInterpolant(pErrorPath, pErrorPath, interpolants);

    if(exportWhen == SMGExportLevel.EVERY) {
      exportInterpolation(pErrorPath, interpolants, interpolationId);
    }

    logger.log(Level.ALL,
        "Finish generating Interpolants for path with interpolation id ", interpolationId);

    return interpolants;
  }

  private void exportInterpolation(ARGPath pErrorPath, Map<ARGState, SMGInterpolant> pInterpolants,
      int pInterpolationId) {

    if (exportPath == null) {
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
      exportInterpolant(currentInterpolant, currentLocation, currentIncomingEdge, pInterpolationId,
          pathIndex);
      pathIndex = pathIndex + 1;
    }
  }

  private void exportInterpolant(SMGInterpolant pCurrentInterpolant, CFANode pCurrentLocation,
      CFAEdge pIncomingEdge, int pInterpolationId, int pPathIndex) {

    if (pIncomingEdge.getEdgeType() == CFAEdgeType.BlankEdge) {
      return;
    }

    if (pIncomingEdge.getEdgeType() == CFAEdgeType.DeclarationEdge) {
      CDeclarationEdge cDclEdge = (CDeclarationEdge) pIncomingEdge;
      CDeclaration cDcl = cDclEdge.getDeclaration();
      if (cDcl instanceof CFunctionDeclaration ||
          cDcl instanceof CTypeDeclaration) {
        return;
      }
    }

    if (pCurrentInterpolant.isFalse()) {
      return;
    }

    List<SMGState> states = pCurrentInterpolant.reconstructStates();

    int counter = 1;
    for (SMGState state : states) {
      String fileName = "smgInterpolant-" + pPathIndex + "-smg-" + counter + ".dot";
      Path path = exportPath.getPath(pInterpolationId, fileName);
      String location = pIncomingEdge.toString() + " on N" + pCurrentLocation.getNodeNumber();
      SMGUtils.dumpSMGPlot(logger, state, location, path);
      counter = counter + 1;
    }
  }

  private void exportFirstInterpolant(SMGInterpolant pFirstInterpolant, int pInterpolationId) {

    if (pFirstInterpolant.isFalse()) {
      return;
    }

    List<SMGState> states = pFirstInterpolant.reconstructStates();

    int counter = 1;
    for (SMGState state : states) {
      String fileName = "smgInterpolant-1-smg-" + counter + ".dot";
      Path path = exportPath.getPath(pInterpolationId, fileName);
      String name = "First interpolant";
      SMGUtils.dumpSMGPlot(logger, state, name, path);
      counter = counter + 1;
    }
  }

  private void exportCFAPath(List<CFAEdge> pFullPath, int pInterpolationId) {

    if(exportPath == null) {
      return;
    }

    StringBuilder interpolationPath = new StringBuilder();

    for (CFAEdge edge : pFullPath) {

      if(edge.getEdgeType() == CFAEdgeType.BlankEdge) {
        continue;
      }

      if (edge.getEdgeType() == CFAEdgeType.DeclarationEdge) {
        CDeclarationEdge cDclEdge = (CDeclarationEdge) edge;
        CDeclaration cDcl = cDclEdge.getDeclaration();
        if (cDcl instanceof CFunctionDeclaration ||
            cDcl instanceof CTypeDeclaration) {
          continue;
        }
      }

      interpolationPath.append(edge.toString());
      interpolationPath.append("\n");
    }

    Path path = exportPath.getPath(pInterpolationId, "interpolationPath.txt");

    try {
      MoreFiles.writeFile(path, Charset.defaultCharset(), interpolationPath.toString());
    } catch (IOException e) {
      logger.logUserException(Level.WARNING, e,
          "Failed to write interpolation path to path " + path.toString());
    }
  }

  /**
   * This method propagates the interpolant "false" to all states that are in
   * the original error path, but are not anymore in the (shorter) prefix.
   *
   * The property that every state on the path beneath the first state with an
   * false interpolant is needed by some code in ValueAnalysisInterpolationTree
   * a subclass of {@link InterpolationTree}, i.e., for global refinement. This
   * property could also be enforced there, but interpolant creation should only
   * happen during interpolation, and not in the data structure holding the interpolants.
   *
   * @param errorPath the original error path
   * @param pErrorPathPrefix the possible shorter error path prefix
   * @param pInterpolants the current interpolant map
   */
  private final void propagateFalseInterpolant(final ARGPath errorPath,
      final ARGPath pErrorPathPrefix,
      final Map<ARGState, SMGInterpolant> pInterpolants) {
    if (pErrorPathPrefix.size() < errorPath.size()) {
      PathIterator it = errorPath.pathIterator();
      for (int i = 0; i < pErrorPathPrefix.size(); i++) {
        it.advance();
      }
      for (ARGState state : it.getSuffixInclusive().asStatesList()) {
        pInterpolants.put(state, interpolantManager.getFalseInterpolant());
      }
    }
  }

  /**
   * This method performs interpolation on each edge of the path, using the
   * {@link EdgeInterpolator} given to this object at construction.
   *
   * @param pErrorPath the error path prefix to interpolate
   * @param pInterpolant an initial interpolant
   *    (only non-trivial when interpolating error path suffixes in global refinement)
   * @param pReachedSet used to extract the current SMGPrecision, useful for heap abstraction interpolation
   * @return the mapping of {@link ARGState}s to {@link Interpolant}
   */
  private Map<ARGState, SMGInterpolant> performEdgeBasedInterpolation(
      ARGPath pErrorPath,
      SMGInterpolant pInterpolant, ARGReachedSet pReachedSet
  ) throws InterruptedException, CPAException {

    /*We may as well interpolate every possible target error if path contains more than one.*/
    boolean checkAllTargets = !checker.isFeasible(pErrorPath, true);

    Map<ARGState, SMGInterpolant> pathInterpolants = new LinkedHashMap<>(pErrorPath.size());

    PathIterator pathIterator = pErrorPath.pathIterator();

    List<SMGInterpolant> interpolants = new ArrayList<>();
    interpolants.add(pInterpolant);

    while (pathIterator.hasNext()) {

      List<SMGInterpolant> resultingInterpolants = new ArrayList<>();

      for(SMGInterpolant interpolant : interpolants) {
        shutdownNotifier.shutdownIfNecessary();

        // interpolate at each edge as long as the previous interpolant is not false
        if (!interpolant.isFalse()) {

          ARGState nextARGState = pathIterator.getNextAbstractState();

          List<SMGInterpolant> deriveResult = interpolator.deriveInterpolant(
              pathIterator.getOutgoingEdge(),
              pathIterator.getPosition(),
              interpolant,
              checkAllTargets,
              pReachedSet,
              nextARGState);
          resultingInterpolants.addAll(deriveResult);
        } else {
          resultingInterpolants.add(interpolantManager.getFalseInterpolant());
        }

        totalInterpolationQueries.setNextValue(interpolator.getNumberOfInterpolationQueries());

        if (!interpolant.isTrivial() && interpolationOffset == -1) {
          interpolationOffset = pathIterator.getIndex();
        }
      }

      pathIterator.advance();

      SMGInterpolant jointResultInterpolant = joinInterpolants(resultingInterpolants);

      ARGState argState = pathIterator.getAbstractState();
      pathInterpolants.put(argState, jointResultInterpolant);
      interpolants.clear();
      interpolants.addAll(resultingInterpolants);
    }

    return pathInterpolants;
  }

  private SMGInterpolant joinInterpolants(List<SMGInterpolant> pResultingInterpolants) {

    SMGInterpolant result = null;

    for (SMGInterpolant interpolant : pResultingInterpolants) {
      if (result == null) {
        result = interpolant;
      } else {
        result = result.join(interpolant);
      }
    }

    return result;
  }
}