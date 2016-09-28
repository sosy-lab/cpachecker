/*
 *  CPAchecker is a tool for configurable software verification.
 *  This file is part of CPAchecker.
 *
 *  Copyright (C) 2007-2016  Dirk Beyer
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
package org.sosy_lab.cpachecker.cpa.smg.refiner.interpolation.flowdependencebased;

import com.google.common.collect.FluentIterable;
import com.google.common.collect.HashMultimap;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.ImmutableSet.Builder;
import com.google.common.collect.SetMultimap;
import com.google.common.collect.Sets;

import org.sosy_lab.common.log.LogManager;
import org.sosy_lab.cpachecker.cfa.model.CFAEdge;
import org.sosy_lab.cpachecker.cpa.arg.ARGPath;
import org.sosy_lab.cpachecker.cpa.arg.ARGPath.PathIterator;
import org.sosy_lab.cpachecker.cpa.arg.ARGReachedSet;
import org.sosy_lab.cpachecker.cpa.arg.ARGState;
import org.sosy_lab.cpachecker.cpa.smg.SMGAbstractionBlock;
import org.sosy_lab.cpachecker.cpa.smg.SMGEdgeHasValue;
import org.sosy_lab.cpachecker.cpa.smg.SMGEdgeHasValueFilter;
import org.sosy_lab.cpachecker.cpa.smg.SMGState;
import org.sosy_lab.cpachecker.cpa.smg.SMGTransferRelation.SMGAddress;
import org.sosy_lab.cpachecker.cpa.smg.SMGTransferRelation.SMGKnownAddress;
import org.sosy_lab.cpachecker.cpa.smg.SMGTransferRelation.SMGKnownSymValue;
import org.sosy_lab.cpachecker.cpa.smg.objects.SMGObject;
import org.sosy_lab.cpachecker.cpa.smg.objects.SMGRegion;
import org.sosy_lab.cpachecker.cpa.smg.refiner.SMGCEGARUtils;
import org.sosy_lab.cpachecker.cpa.smg.refiner.SMGFeasibilityChecker;
import org.sosy_lab.cpachecker.cpa.smg.refiner.SMGMemoryPath;
import org.sosy_lab.cpachecker.cpa.smg.refiner.SMGPrecision;
import org.sosy_lab.cpachecker.cpa.smg.refiner.SMGStrongestPostOperator;
import org.sosy_lab.cpachecker.cpa.smg.refiner.interpolation.SMGEdgeHeapAbstractionInterpolator;
import org.sosy_lab.cpachecker.cpa.smg.refiner.interpolation.SMGEdgeInterpolator.SMGHeapAbstractionInterpoaltionResult;
import org.sosy_lab.cpachecker.cpa.smg.refiner.interpolation.SMGInterpolant;
import org.sosy_lab.cpachecker.cpa.smg.refiner.interpolation.SMGStateInterpolant.SMGPrecisionIncrement;
import org.sosy_lab.cpachecker.cpa.smg.refiner.interpolation.flowdependencebased.SMGPathDependence.PathPositionMemoryPathDependencys;
import org.sosy_lab.cpachecker.exceptions.CPAException;
import org.sosy_lab.cpachecker.exceptions.RefinementFailedException;
import org.sosy_lab.cpachecker.exceptions.RefinementFailedException.Reason;
import org.sosy_lab.cpachecker.util.AbstractStates;
import org.sosy_lab.cpachecker.util.states.MemoryLocation;

import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

public class SMGPathDependenceBuilder {

  public final LogManager logger;
  public final SMGFeasibilityChecker checker;
  private final SMGStrongestPostOperator strongestPostOp;
  private final SMGEdgeHeapAbstractionInterpolator heapAbsInterpolator;

  public SMGPathDependenceBuilder(LogManager pLogger, SMGFeasibilityChecker pChecker,
      SMGStrongestPostOperator pStrongestPostOperatorForInterpolationBasedOnFlowDependence) {
    logger = pLogger;
    checker = pChecker;
    strongestPostOp = pStrongestPostOperatorForInterpolationBasedOnFlowDependence;
    heapAbsInterpolator =
        new SMGEdgeHeapAbstractionInterpolator(logger, checker);
  }

  public SMGStrongestPostOperator getStrongestPostOp() {
    return strongestPostOp;
  }

  public Set<SMGPathDependence> createMemoryDependences(ARGPath path, ARGState pRoot,
      Map<ARGState, SMGInterpolant> pPreviousInterpolants, ARGReachedSet pReached)
      throws CPAException, InterruptedException {

    ARGState targetState = path.getLastState();

    Set<SMGPathDependence> result;
    SMGTargets targets = new SMGTargets(targetState);

    result = createMemoryDependences(path, pRoot, targets, pPreviousInterpolants, pReached);

    return result;
  }

  private Set<SMGPathDependence> createMemoryDependences(ARGPath pPath, ARGState pRoot,
      SMGTargets pTargets, Map<ARGState, SMGInterpolant> pPreviousInterpolants,
      ARGReachedSet pReached) throws CPAException, InterruptedException {

    if (pPath.size() == 0) {
      return ImmutableSet.of();
    }

    PathIterator it = pPath.fullPathIterator();
    SMGState initialState =
        AbstractStates.extractStateByType(pRoot, SMGState.class);
    initialState = new SMGState(initialState);

    Map<SMGKnownAddress, SMGFlowDependenceFieldVertice> scope = new HashMap<>();
    SetMultimap<SMGObject, SMGKnownAddress> objectSizeMap = HashMultimap.create();
    Map<SMGObject, Integer> objectDclMap = new HashMap<>();

    while (!it.isPositionWithState() && it.getAbstractState() != pRoot) {
      it.advance();
    }

    int pos = it.getIndex();

    SMGPathDependenceResultBuilder pathDependenceResultBuilder =
        new SMGPathDependenceResultBuilder();

    for (SMGEdgeHasValue hve : initialState.getHVEdges()) {
      SMGKnownAddress field = SMGKnownAddress.valueOf(hve.getObject(), hve.getOffset());
      SMGFlowDependenceFieldVertice vertice = new SMGFlowDependenceFieldVertice(field, pos);
      scope.put(field, vertice);
      pathDependenceResultBuilder.addNewFlowDependenceVertice(vertice);
    }

    SMGState nextState = initialState;

    Builder<SMGPathDependence> result =
        createMemoryDependenceForPaths(pPath, nextState,
            scope, pTargets,
            pPreviousInterpolants, objectSizeMap, objectDclMap, pReached, it.getIndex(),
            pathDependenceResultBuilder);

    return result.build();
  }

  private Builder<SMGPathDependence> createMemoryDependenceForPaths(ARGPath pPath, SMGState nextState,
      Map<SMGKnownAddress, SMGFlowDependenceFieldVertice> scope,
      SMGTargets pTargets, Map<ARGState, SMGInterpolant> pPreviousInterpolants,
      SetMultimap<SMGObject, SMGKnownAddress> pObjectSizeMap, Map<SMGObject, Integer> pObjectDclMap,
      ARGReachedSet pReached, int pStartPos, SMGPathDependenceResultBuilder pPathDependenceResultBuilder)
      throws CPAException, InterruptedException {

    Builder<SMGPathDependence> resultBuilder = ImmutableSet.builder();

    PathIterator it = pPath.fullPathIterator();

    int pos = pStartPos;

    while (it.getIndex() != pStartPos) {
      it.advance();
    }

    while (it.advanceIfPossible() && !nextState.getSourcesOfHve().isPathEnd()) {

      if (pPathDependenceResultBuilder.isStartOfNewPrecisionAdjustmentBlock(it.getIndex(), nextState)) {
        nextState = nextState.addSourcesToValues();
      }

      while (it.getIncomingEdge() == null
          && it.hasNext()) {
        it.advanceIfPossible();
      }

      CFAEdge edge = it.getIncomingEdge();

      if (edge == null) {
        continue;
      }

      Collection<SMGState> resultStates =
          strongestPostOp.getStrongestPost(nextState, checker.getStrongestPrecision(), edge);

      if(resultStates.isEmpty()) {
        throw new RefinementFailedException(Reason.InterpolationFailed, pPath);
      }

      pos = it.getIndex();

      Iterator<SMGState> resultStateIterator = resultStates.iterator();
      nextState = resultStateIterator.next();

      while (resultStateIterator.hasNext()) {
        SMGState newPathStart = resultStateIterator.next();
        Map<SMGKnownAddress, SMGFlowDependenceFieldVertice> newScope = new HashMap<>();
        newScope.putAll(scope);
        SetMultimap<SMGObject, SMGKnownAddress> newObjectSizeMap = HashMultimap.create();
        newObjectSizeMap.putAll(pObjectSizeMap);
        Map<SMGObject, Integer> newObjDclMap = new HashMap<>();
        newObjDclMap.putAll(newObjDclMap);
        SMGPathDependenceResultBuilder newPathResultBuilder =
            new SMGPathDependenceResultBuilder(pPathDependenceResultBuilder);

        if (it.isPositionWithState()) {
          updateMemoryPathDependenceBuildStep(pPath, newPathStart, newScope, it,
              pPreviousInterpolants, newObjectSizeMap, newObjDclMap, pTargets, pReached,
              newPathResultBuilder);
        }

        Builder<SMGPathDependence> otherPaths = createMemoryDependenceForPaths(pPath, newPathStart,
            newScope, pTargets, pPreviousInterpolants, newObjectSizeMap,
            newObjDclMap, pReached, it.getIndex(), newPathResultBuilder);
        resultBuilder.addAll(otherPaths.build());
      }

      if(!it.isPositionWithState()) {
        continue;
      }

      updateMemoryPathDependenceBuildStep(pPath, nextState,
          scope, it, pPreviousInterpolants, pObjectSizeMap, pObjectDclMap,
          pTargets, pReached, pPathDependenceResultBuilder);
    }

    if (nextState.getSourcesOfHve().isPathEnd()) {
      pos = pos - 1;
    }

    SMGPathDependence memoryDependenceOfPath =
        pPathDependenceResultBuilder.build(logger, pPath, pTargets, pos);

    resultBuilder.add(memoryDependenceOfPath);
    return resultBuilder;
  }

  private void updateMemoryPathDependenceBuildStep(ARGPath pPath, SMGState pState,
      Map<SMGKnownAddress, SMGFlowDependenceFieldVertice> scope, PathIterator it,
      Map<ARGState, SMGInterpolant> pPrevInterpolants,
      SetMultimap<SMGObject, SMGKnownAddress> pNewObjectSizeMap,
      Map<SMGObject, Integer> pNewObjDclMap, SMGTargets pTargets, ARGReachedSet pReached, SMGPathDependenceResultBuilder pPathDependenceResultBuilder)
      throws CPAException, InterruptedException {

    int pos = it.getIndex();

    Set<SMGAbstractionBlock> blocks =
        edgeBasedHeapAbstractionInterpolation(pState, pPrevInterpolants, it, pReached);

    SMGHveSources sources = pState.getSourcesOfHve();

    if (sources.isPathEnd()) {
      pPathDependenceResultBuilder.addTargetAddressesNeededToContradictAssumeEdge(pos, pState);
      return;
    }

    Set<SMGKnownAddress> newFields = sources.getNewFields();
    Map<SMGKnownAddress, SMGFlowDependenceFieldVertice> newVertices = new HashMap<>(newFields.size());

    for (Entry<SMGObject, SMGKnownAddress> entry : sources.getObjectMap()) {
      pNewObjectSizeMap.put(entry.getKey(), entry.getValue());
      pNewObjDclMap.put(entry.getKey(), pos);
    }

    Set<SMGRegion> variablesWithVariableSizes = sources.getVariableTypeDclRegion();

    pPathDependenceResultBuilder.addVariableSizeVariableDeclarations(variablesWithVariableSizes, pos);

    for (SMGKnownAddress field : newFields) {
      SMGFlowDependenceFieldVertice newVertice = new SMGFlowDependenceFieldVertice(field, pos);
      newVertices.put(field, newVertice);
      pPathDependenceResultBuilder.addNewFlowDependenceVertice(newVertice);

      for (SMGKnownAddress source : pNewObjectSizeMap.get(field.getObject())) {
        if (!scope.containsKey(source)) {
          throw new RefinementFailedException(Reason.InterpolationFailed, pPath);
        }

        SMGFlowDependenceFieldVertice sourceVertice = scope.get(source);
        SMGFlowDependenceFieldEdge newEdge =
            new SMGFlowDependenceFieldEdge(sourceVertice, newVertice);
        pPathDependenceResultBuilder.addNewFlowDependenceEdge(newEdge);
      }
    }

    for (Entry<SMGEdgeHasValue, SMGKnownAddress> entry : sources.getHveSources()) {

      SMGEdgeHasValue hve = entry.getKey();
      SMGKnownAddress source = entry.getValue();
      SMGKnownAddress field = SMGKnownAddress.valueOf(hve.getObject(), hve.getOffset());

      if (!scope.containsKey(source)
          || !newVertices.containsKey(field)) {
        throw new RefinementFailedException(Reason.InterpolationFailed, pPath);
      }

      SMGFlowDependenceFieldVertice sourceVertice = scope.get(source);
      SMGFlowDependenceFieldVertice targetVertice = newVertices.get(field);
      SMGFlowDependenceFieldEdge newEdge =
          new SMGFlowDependenceFieldEdge(sourceVertice, targetVertice);
      pPathDependenceResultBuilder.addNewFlowDependenceEdge(newEdge);
    }

    for (Entry<SMGKnownSymValue, SMGKnownAddress> entry : sources.getValueMap().entries()) {

      SMGKnownSymValue val = entry.getKey();
      SMGKnownAddress source = entry.getValue();

      SMGEdgeHasValueFilter filter = SMGEdgeHasValueFilter.valueFilter(val.getAsInt());

      for (SMGEdgeHasValue hve : pState.getHVEdges(filter)) {
        SMGKnownAddress field = SMGKnownAddress.valueOf(hve.getObject(), hve.getOffset());

        if (!scope.containsKey(source)) {
          throw new RefinementFailedException(Reason.InterpolationFailed, pPath);
        }

        SMGFlowDependenceFieldVertice sourceVertice = scope.get(source);

        SMGFlowDependenceFieldVertice targetVertice;

        if (newVertices.containsKey(field)) {
          targetVertice = newVertices.get(field);
        } else if (scope.containsKey(field)) {
          targetVertice = scope.get(field);
        } else {
          throw new RefinementFailedException(Reason.InterpolationFailed, pPath);
        }

        SMGFlowDependenceFieldEdge newEdge =
            new SMGFlowDependenceFieldEdge(sourceVertice, targetVertice);
        pPathDependenceResultBuilder.addNewFlowDependenceEdge(newEdge);
      }
    }

    for (SMGAddress field : newFields) {
      scope.put(field.getAsKnownAddress(),
          newVertices.get(field.getAsKnownAddress()));
    }

    Set<SMGKnownAddress> pPathTargets = new HashSet<>();
    Map<SMGKnownAddress, SMGFlowDependenceFieldVertice> pScope = ImmutableMap.copyOf(scope);
    PathPositionMemoryPathDependencys dependenceOfIndex =
        pState.calculatePathDependence(pScope, pPathTargets, blocks);

    if (pTargets.isInvalidDereference() || pTargets.isInvalidFree()) {
      Set<SMGKnownAddress> newTargets = pState.getSourcesOfHve().getSourcesOfDereferences();

      dependenceOfIndex = dependenceOfIndex.updateTarget(newTargets);
    }

    pPathDependenceResultBuilder.addPathDependenceOfPosition(dependenceOfIndex, pos);

    for (SMGObject obj : pState.getSourcesOfHve().getTargetWriteObject()) {
      if (pNewObjectSizeMap.containsKey(obj)) {
        Set<SMGKnownAddress> newObjTarg = pNewObjectSizeMap.get(obj);
        int posOfAlloc = pNewObjDclMap.get(obj);
        pPathDependenceResultBuilder.updateTargets(posOfAlloc - 1, newObjTarg);
      }
    }

    if (!pState.getSourcesOfHve().getSourcesOfUnkownTargetWrite().isEmpty()) {
      Set<SMGKnownAddress> newTargets = pState.getSourcesOfHve().getSourcesOfUnkownTargetWrite();
      pPathDependenceResultBuilder.updateTargets(pos - 1, newTargets);
    }
  }

  private Set<SMGAbstractionBlock> edgeBasedHeapAbstractionInterpolation(SMGState pState,
      Map<ARGState, SMGInterpolant> pPrevInterpolants, PathIterator it, ARGReachedSet pReached)
      throws CPAException, InterruptedException {

    if (!it.isPositionWithState()) {
      return ImmutableSet.of();
    }

    ARGState argStateOfPos = it.getAbstractState();

    SMGPrecision currentPrecAtPos = SMGCEGARUtils.extractSMGPrecision(pReached, argStateOfPos);

    if (!currentPrecAtPos.useHeapAbstractionOnNode(it.getLocation())) {
      return ImmutableSet.of();
    }

    if (!pPrevInterpolants.containsKey(argStateOfPos)) {
      return ImmutableSet.of();
    }

    SMGPrecisionIncrement inc =
        pPrevInterpolants.get(argStateOfPos).getPrecisionIncrement();
    Set<SMGMemoryPath> previouslyTrackedPath = FluentIterable.from(inc.getPathsToTrack()).toSet();
    Set<MemoryLocation> prevTrackedVar = FluentIterable.from(inc.getStackVariablesToTrack()).toSet();
    Set<SMGAbstractionBlock> prevBlocks = FluentIterable.from(inc.getAbstractionBlock()).toSet();

    previouslyTrackedPath = Sets.union(previouslyTrackedPath,
        currentPrecAtPos.getTrackedMemoryPathsOnNode(it.getLocation()));
    prevTrackedVar = Sets.union(prevTrackedVar,
        currentPrecAtPos.getTrackedStackVariablesOnNode(it.getLocation()));
    prevBlocks = Sets.union(prevBlocks, currentPrecAtPos.getAbstractionBlocks(it.getLocation()));

    if(previouslyTrackedPath.isEmpty()) {
      return ImmutableSet.of();
    }

    SMGState abstractStateTest = new SMGState(pState);
    abstractStateTest.forgetNonTrackedStackVariables(prevTrackedVar);
    abstractStateTest.forgetNonTrackedHve(previouslyTrackedPath);
    ARGPath remainingErrorPath = it.getSuffixInclusive();

    if (checker.isFeasible(remainingErrorPath, abstractStateTest)) {
      return ImmutableSet.of();
    } else {
      pState.forgetNonTrackedStackVariables(prevTrackedVar);
      pState.forgetNonTrackedHve(previouslyTrackedPath);
    }

    SMGHeapAbstractionInterpoaltionResult absResult =
        heapAbsInterpolator.calculateHeapAbstractionBlocks(pState, remainingErrorPath,
            currentPrecAtPos, it.getLocation(), it.getIncomingEdge(), false);

    return absResult.getBlocks();
  }

  public static final class SMGTargets {

    private final boolean invalidDereference;
    private final boolean invalidFree;
    private final boolean memoryLeak;
    private final boolean reachabilityError;

    public SMGTargets(ARGState pTargetState) {

      Set<String> automatonState = SMGCEGARUtils.extractTargetAutomatonNames(pTargetState);
      invalidDereference = automatonState.contains("SMGCPADEREF");
      invalidFree = automatonState.contains("SMGCPAFREE");
      memoryLeak = automatonState.contains("SMGCPAMEMTRACK");
      reachabilityError = !invalidDereference && !invalidFree && !memoryLeak;
    }

    public boolean isInvalidDereference() {
      return invalidDereference;
    }

    public boolean isInvalidFree() {
      return invalidFree;
    }

    public boolean hasMemoryLeak() {
      return memoryLeak;
    }

    public boolean isReachabilityError() {
      return reachabilityError;
    }

    @Override
    public String toString() {
      return "SMGTargets [invalidDereference=" + invalidDereference + ", invalidFree=" + invalidFree
          + ", memoryLeak=" + memoryLeak + ", reachabilityError=" + reachabilityError + "]";
    }

    @Override
    public int hashCode() {
      final int prime = 31;
      int result = 1;
      result = prime * result + (invalidDereference ? 1231 : 1237);
      result = prime * result + (invalidFree ? 1231 : 1237);
      result = prime * result + (memoryLeak ? 1231 : 1237);
      result = prime * result + (reachabilityError ? 1231 : 1237);
      return result;
    }

    @Override
    public boolean equals(Object obj) {
      if (this == obj) {
        return true;
      }
      if (obj == null) {
        return false;
      }
      if (getClass() != obj.getClass()) {
        return false;
      }
      SMGTargets other = (SMGTargets) obj;
      if (invalidDereference != other.invalidDereference) {
        return false;
      }
      if (invalidFree != other.invalidFree) {
        return false;
      }
      if (memoryLeak != other.memoryLeak) {
        return false;
      }
      if (reachabilityError != other.reachabilityError) {
        return false;
      }
      return true;
    }
  }

  public static final class SMGPathDependenceResultBuilder {

    private final Set<SMGFlowDependenceFieldEdge> graphEdges;
    private final Set<SMGFlowDependenceFieldVertice> graphVertices;
    private final Map<Integer, PathPositionMemoryPathDependencys> pathPositionMemoryPathDependencys;
    private final Map<SMGRegion, Integer> variableSizeStackMemoryLocationDeclarationPosition;

    public SMGPathDependenceResultBuilder() {
      graphEdges = new HashSet<>();
      graphVertices = new HashSet<>();
      pathPositionMemoryPathDependencys = new HashMap<>();
      variableSizeStackMemoryLocationDeclarationPosition = new HashMap<>();
    }

    public SMGPathDependence build(LogManager pLogger, ARGPath pPath, SMGTargets pTargets,
        int pPathEnd) {

      SMGFlowDependenceGraph<SMGFlowDependenceFieldVertice, SMGFlowDependenceFieldEdge> smgUseGraph =
          new SMGFlowDependenceGraph<>(graphEdges, graphVertices);

      SMGPathDependence memoryDependenceOfPath =
          new SMGPathDependence(pLogger, pPath, smgUseGraph, pathPositionMemoryPathDependencys,
              variableSizeStackMemoryLocationDeclarationPosition, pTargets.hasMemoryLeak(),
              pPathEnd);

      return memoryDependenceOfPath;
    }

    public SMGPathDependenceResultBuilder(Set<SMGFlowDependenceFieldEdge> pGraphEdges,
        Map<Integer, PathPositionMemoryPathDependencys> pPathPositionMemoryPathDependencys,
        Map<SMGRegion, Integer> pVariableSizeStackMemoryLocationDeclarationPosition,
        Set<SMGFlowDependenceFieldVertice> pGraphVertices) {
      graphEdges = new HashSet<>();
      pathPositionMemoryPathDependencys = new HashMap<>();
      variableSizeStackMemoryLocationDeclarationPosition = new HashMap<>();
      graphVertices = new HashSet<>();
      graphVertices.addAll(pGraphVertices);
      graphEdges.addAll(pGraphEdges);
      pathPositionMemoryPathDependencys.putAll(pPathPositionMemoryPathDependencys);
      variableSizeStackMemoryLocationDeclarationPosition
          .putAll(pVariableSizeStackMemoryLocationDeclarationPosition);
    }

    public SMGPathDependenceResultBuilder(
        SMGPathDependenceResultBuilder pPathDependenceResultBuilder) {
      this(pPathDependenceResultBuilder.graphEdges,
          pPathDependenceResultBuilder.pathPositionMemoryPathDependencys,
          pPathDependenceResultBuilder.variableSizeStackMemoryLocationDeclarationPosition,
          pPathDependenceResultBuilder.graphVertices);
    }

    public void updateTargets(int pPos, Set<SMGKnownAddress> pNewObjTarg) {
      PathPositionMemoryPathDependencys newPosDep =
          pathPositionMemoryPathDependencys.get(pPos);
      pathPositionMemoryPathDependencys.put(pPos, newPosDep.updateTarget(pNewObjTarg));
    }

    public void addPathDependenceOfPosition(PathPositionMemoryPathDependencys pDependenceOfIndex,
        int pPos) {
      pathPositionMemoryPathDependencys.put(pPos, pDependenceOfIndex);
    }

    public void addNewFlowDependenceVertice(SMGFlowDependenceFieldVertice pNewVertice) {
      graphVertices.add(pNewVertice);
    }

    public Set<SMGFlowDependenceFieldVertice> getGraphVertices() {
      return ImmutableSet.copyOf(graphVertices);
    }

    public void addNewFlowDependenceEdge(SMGFlowDependenceFieldEdge pNewEdge) {
      graphEdges.add(pNewEdge);
    }

    public void addVariableSizeVariableDeclarations(Set<SMGRegion> pVariablesWithVariableSizes, int pDeclarartionPathPosition) {
      for (SMGRegion region : pVariablesWithVariableSizes) {
        variableSizeStackMemoryLocationDeclarationPosition.put(region, pDeclarartionPathPosition);
      }
    }

    public void addTargetAddressesNeededToContradictAssumeEdge(int pPathLocationAfterAssumeEdge,
        SMGState pStateAfterContradictingAssume) {
      SMGHveSources sources = pStateAfterContradictingAssume.getSourcesOfHve();
      PathPositionMemoryPathDependencys dep =
          pathPositionMemoryPathDependencys.get(pPathLocationAfterAssumeEdge - 1);
      pathPositionMemoryPathDependencys.put(pPathLocationAfterAssumeEdge - 1,
          dep.updateTarget(sources.getPathEndValue()));
    }

    public boolean isStartOfNewPrecisionAdjustmentBlock(int pPathPos, SMGState pStateOfPath) {
      return pStateOfPath.getSourcesOfHve() instanceof SMGHveUnkownSources ||
          pathPositionMemoryPathDependencys.containsKey(pPathPos - 1);
    }

    @Override
    public String toString() {
      return "SMGPathDependenceResultBuilder [graphEdges=" + graphEdges
          + ", pathPositionMemoryPathDependencys=" + pathPositionMemoryPathDependencys
          + ", variableSizeStackMemoryLocationDeclarationPosition="
          + variableSizeStackMemoryLocationDeclarationPosition + "]";
    }

    public Set<SMGFlowDependenceFieldEdge> getGraphEdges() {
      return ImmutableSet.copyOf(graphEdges);
    }

    public Map<Integer, PathPositionMemoryPathDependencys> getPathPositionMemoryPathDependencys() {
      return ImmutableMap.copyOf(pathPositionMemoryPathDependencys);
    }

    public Map<SMGRegion, Integer> getVariableSizeStackMemoryLocationDeclarationPosition() {
      return ImmutableMap.copyOf(variableSizeStackMemoryLocationDeclarationPosition);
    }
  }
}