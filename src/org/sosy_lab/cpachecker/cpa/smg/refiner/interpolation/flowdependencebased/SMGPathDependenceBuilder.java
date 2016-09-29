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
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.ImmutableSet.Builder;
import com.google.common.collect.ImmutableSetMultimap;
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
  private final ARGPath path;
  private final ARGState root;
  private final Map<ARGState, SMGInterpolant> previousInterpolants;
  private final ARGReachedSet reached;
  private final SMGTargets targets;

  public SMGPathDependenceBuilder(LogManager pLogger, SMGFeasibilityChecker pChecker,
      SMGStrongestPostOperator pStrongestPostOp,
      SMGEdgeHeapAbstractionInterpolator pHeapAbsInterpolator, ARGPath pPath, ARGState pRoot,
      Map<ARGState, SMGInterpolant> pPreviousInterpolants, ARGReachedSet pReached) {
    logger = pLogger;
    checker = pChecker;
    strongestPostOp = pStrongestPostOp;
    heapAbsInterpolator = pHeapAbsInterpolator;
    path = pPath;
    root = pRoot;
    previousInterpolants = pPreviousInterpolants;
    reached = pReached;
    ARGState pTargetState = pPath.getLastState();
    targets = new SMGTargets(pTargetState);
  }

  public SMGStrongestPostOperator getStrongestPostOp() {
    return strongestPostOp;
  }

  public Set<SMGPathDependence> buildMemoryDependences()
      throws CPAException, InterruptedException {

    if (path.size() == 0) {
      return ImmutableSet.of();
    }

    PathIterator it = path.fullPathIterator();
    SMGState initialState =
        AbstractStates.extractStateByType(root, SMGState.class);
    initialState = new SMGState(initialState);

    PathScope pathScope = new PathScope();

    while (!it.isPositionWithState() && it.getAbstractState() != root) {
      it.advance();
    }

    int pathPosition = it.getIndex();

    SMGPathDependenceResultBuilder pathDependenceResultBuilder =
        new SMGPathDependenceResultBuilder();

    pathScope =
        pathScope.initializePathScope(initialState, pathDependenceResultBuilder, pathPosition);

    SMGState nextState = initialState;

    Builder<SMGPathDependence> result =
        createMemoryDependenceForPaths(nextState, pathPosition,
            pathDependenceResultBuilder, pathScope);

    return result.build();
  }

  private Builder<SMGPathDependence> createMemoryDependenceForPaths(SMGState nextState,
      int pStartPos, SMGPathDependenceResultBuilder pPathDependenceResultBuilder,
      PathScope pPathScope)
      throws CPAException, InterruptedException {

    Builder<SMGPathDependence> resultBuilder = ImmutableSet.builder();

    PathIterator it = path.fullPathIterator();

    int pathPosition = pStartPos;

    while (it.getIndex() != pathPosition) {
      it.advance();
    }

    PathScope currentPathScope = pPathScope;

    while (it.advanceIfPossible() && !nextState.getSourcesOfHve().isPathEnd()) {

      pathPosition = it.getIndex();

      if (pPathDependenceResultBuilder.isStartOfNewPrecisionAdjustmentBlock(pathPosition, nextState)) {
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
        throw new RefinementFailedException(Reason.InterpolationFailed, path);
      }

      pathPosition = it.getIndex();

      Iterator<SMGState> resultStateIterator = resultStates.iterator();
      nextState = resultStateIterator.next();

      while (resultStateIterator.hasNext()) {
        SMGState newPathStart = resultStateIterator.next();
        PathScope newPathScope = new PathScope(currentPathScope);
        SMGPathDependenceResultBuilder newPathResultBuilder =
            new SMGPathDependenceResultBuilder(pPathDependenceResultBuilder);

        if (it.isPositionWithState()) {
          newPathScope = updateMemoryPathDependenceBuildStep(newPathStart, it,
              newPathResultBuilder, newPathScope);
        }

        Builder<SMGPathDependence> otherPaths = createMemoryDependenceForPaths(newPathStart,
            pathPosition, newPathResultBuilder, newPathScope);
        resultBuilder.addAll(otherPaths.build());
      }

      if(!it.isPositionWithState()) {
        continue;
      }

      currentPathScope = updateMemoryPathDependenceBuildStep(nextState,
          it, pPathDependenceResultBuilder, currentPathScope);
    }

    int lastStatePosition;

    if (nextState.getSourcesOfHve().isPathEnd()) {
      lastStatePosition = pathPosition - 1;
    } else {
      lastStatePosition = pathPosition;
    }

    SMGPathDependence memoryDependenceOfPath =
        pPathDependenceResultBuilder.build(logger, path, targets, lastStatePosition);

    resultBuilder.add(memoryDependenceOfPath);
    return resultBuilder;
  }

  private PathScope updateMemoryPathDependenceBuildStep(SMGState pState,
      PathIterator it, SMGPathDependenceResultBuilder pPathDependenceResultBuilder, PathScope pPathScope) throws CPAException, InterruptedException {

    int pathPosition = it.getIndex();

    PathScope newPathScope = pPathScope;

    Set<SMGAbstractionBlock> blocks =
        edgeBasedHeapAbstractionInterpolation(pState, it);

    SMGHveSources sources = pState.getSourcesOfHve();

    if (sources.isPathEnd()) {
      pPathDependenceResultBuilder.addTargetAddressesNeededToContradictAssumeEdge(pathPosition,
          pState);
      return newPathScope;
    }

    Set<SMGKnownAddress> newFields = sources.getNewFields();
    Map<SMGKnownAddress, SMGFlowDependenceFieldVertice> newVertices =
        new HashMap<>(newFields.size());

    newPathScope = newPathScope.addObjectAllocations(sources, pathPosition);

    Set<SMGRegion> variablesWithVariableSizes = sources.getVariableTypeDclRegion();

    pPathDependenceResultBuilder.addVariableSizeVariableDeclarations(variablesWithVariableSizes, pathPosition);

    for (SMGKnownAddress field : newFields) {
      SMGFlowDependenceFieldVertice newVertice = new SMGFlowDependenceFieldVertice(field, pathPosition);
      newVertices.put(field, newVertice);
      pPathDependenceResultBuilder.addNewFlowDependenceVertice(newVertice);

      Set<SMGKnownAddress> sourcesOfAllocationSize = newPathScope.getSourcesForSizeAllocationOfObject(field.getObject());

      for (SMGKnownAddress source : sourcesOfAllocationSize) {
        if (!newPathScope.containsVerticeForField(source)) {
          throw new RefinementFailedException(Reason.InterpolationFailed, path);
        }

        SMGFlowDependenceFieldVertice sourceVertice = newPathScope.getVerticeForField(source);
        SMGFlowDependenceFieldEdge newEdge =
            new SMGFlowDependenceFieldEdge(sourceVertice, newVertice);
        pPathDependenceResultBuilder.addNewFlowDependenceEdge(newEdge);
      }
    }

    for (Entry<SMGEdgeHasValue, SMGKnownAddress> entry : sources.getHveSources()) {

      SMGEdgeHasValue hve = entry.getKey();
      SMGKnownAddress source = entry.getValue();
      SMGKnownAddress field = SMGKnownAddress.valueOf(hve.getObject(), hve.getOffset());

      if (!newPathScope.containsVerticeForField(source)
          || !newVertices.containsKey(field)) {
//
//        SMGFlowDependenceFieldVertice vertice1 = newPathScope.getVerticeForField(source);
//        SMGFlowDependenceFieldVertice vertice2 = newVertices.get(field);

        throw new RefinementFailedException(Reason.InterpolationFailed, path);
      }

      SMGFlowDependenceFieldVertice sourceVertice = newPathScope.getVerticeForField(source);
      SMGFlowDependenceFieldVertice targetVertice = newVertices.get(field);
      SMGFlowDependenceFieldEdge newEdge =
          new SMGFlowDependenceFieldEdge(sourceVertice, targetVertice);
      pPathDependenceResultBuilder.addNewFlowDependenceEdge(newEdge);
    }

    for (Entry<SMGKnownSymValue, SMGKnownAddress> entry : sources.getValueMap().entries()) {

      SMGKnownSymValue val = entry.getKey();
      SMGKnownAddress source = entry.getValue();

      /*0 symbolic value is already explicit*/
      if (val.getAsInt() == 0) {
        continue;
      }

      SMGEdgeHasValueFilter filter = SMGEdgeHasValueFilter.valueFilter(val.getAsInt());

      for (SMGEdgeHasValue hve : pState.getHVEdges(filter)) {
        SMGKnownAddress field = SMGKnownAddress.valueOf(hve.getObject(), hve.getOffset());

        if (!newPathScope.containsVerticeForField(source)) {
          throw new RefinementFailedException(Reason.InterpolationFailed, path);
        }

        SMGFlowDependenceFieldVertice sourceVertice = newPathScope.getVerticeForField(source);

        SMGFlowDependenceFieldVertice targetVertice;

        if (newVertices.containsKey(field)) {
          targetVertice = newVertices.get(field);
        } else if (newPathScope.containsVerticeForField(field)) {
          targetVertice = newPathScope.getVerticeForField(field);
        } else {
          throw new RefinementFailedException(Reason.InterpolationFailed, path);
        }

        SMGFlowDependenceFieldEdge newEdge =
            new SMGFlowDependenceFieldEdge(sourceVertice, targetVertice);
        pPathDependenceResultBuilder.addNewFlowDependenceEdge(newEdge);
      }
    }

    newPathScope = newPathScope.updateScopeForNextStep(newFields, newVertices);

    Set<SMGKnownAddress> pPathTargets = new HashSet<>();
    Map<SMGKnownAddress, SMGFlowDependenceFieldVertice> pScope = newPathScope.getFieldVerticeScope();
    PathPositionMemoryPathDependencys dependenceOfIndex =
        pState.calculatePathDependence(pScope, pPathTargets, blocks);

    if (targets.isInvalidDereference() || targets.isInvalidFree()) {
      Set<SMGKnownAddress> newTargets = pState.getSourcesOfHve().getSourcesOfDereferences();

      dependenceOfIndex = dependenceOfIndex.updateTarget(newTargets);
    }

    pPathDependenceResultBuilder.addPathDependenceOfPosition(dependenceOfIndex, pathPosition);
    pPathDependenceResultBuilder.updateTargetsForObjectSizeAllocations(newPathScope, sources);
    pPathDependenceResultBuilder.updateTargetsForUnknownWrites(pathPosition, sources);

    return newPathScope;
  }

  private Set<SMGAbstractionBlock> edgeBasedHeapAbstractionInterpolation(SMGState pState,
      PathIterator it)
      throws CPAException, InterruptedException {

    if (!it.isPositionWithState()) {
      return ImmutableSet.of();
    }

    ARGState argStateOfPos = it.getAbstractState();

    SMGPrecision currentPrecAtPos = SMGCEGARUtils.extractSMGPrecision(reached, argStateOfPos);

    if (!currentPrecAtPos.useHeapAbstractionOnNode(it.getLocation())) {
      return ImmutableSet.of();
    }

    if (!previousInterpolants.containsKey(argStateOfPos)) {
      return ImmutableSet.of();
    }

    SMGPrecisionIncrement inc =
        previousInterpolants.get(argStateOfPos).getPrecisionIncrement();
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

    if (remainingErrorPath.size() <= 1) {
      return ImmutableSet.of();
    }

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

    public void updateTargetsForObjectSizeAllocations(PathScope pPathScope, SMGHveSources pSource) {

      for (SMGObject obj : pSource.getTargetWriteObject()) {
        Set<SMGKnownAddress> newObjTarg = pPathScope.getSourcesForSizeAllocationOfObject(obj);

        if(!newObjTarg.isEmpty()) {
          int posOfAlloc = pPathScope.getAllocationPosition(obj);
          updateTargets(posOfAlloc - 1, newObjTarg);
        }
      }
    }

    public void updateTargetsForUnknownWrites(int pPathPosition,
        SMGHveSources source) {

      if (!source.getSourcesOfUnkownTargetWrite().isEmpty()) {
        Set<SMGKnownAddress> newTargets = source.getSourcesOfUnkownTargetWrite();
        updateTargets(pPathPosition - 1, newTargets);
      }
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

  public static final class PathScope {

    private final Map<SMGKnownAddress, SMGFlowDependenceFieldVertice> scope;
    private final SetMultimap<SMGObject, SMGKnownAddress> objectSizeSources;
    private final Map<SMGObject, Integer> objectAllocationPosition;

    public PathScope() {
      scope = ImmutableMap.of();
      objectSizeSources = ImmutableSetMultimap.of();
      objectAllocationPosition = ImmutableMap.of();
    }

    public PathScope(PathScope pOldPathScope) {
      this(pOldPathScope.scope, pOldPathScope.objectSizeSources,
          pOldPathScope.objectAllocationPosition);
    }

    public PathScope(Map<SMGKnownAddress, SMGFlowDependenceFieldVertice> pScope,
        SetMultimap<SMGObject, SMGKnownAddress> pObjectSizeSources,
        Map<SMGObject, Integer> pObjectAllocationPosition) {
      scope = ImmutableMap.copyOf(pScope);
      objectSizeSources = ImmutableSetMultimap.copyOf(pObjectSizeSources);
      objectAllocationPosition = ImmutableMap.copyOf(pObjectAllocationPosition);
    }

    public int getAllocationPosition(SMGObject pObject) {
      return objectAllocationPosition.get(pObject);
    }

    public Map<SMGKnownAddress, SMGFlowDependenceFieldVertice> getFieldVerticeScope() {
      return scope;
    }

    public SMGFlowDependenceFieldVertice getVerticeForField(SMGKnownAddress pSource) {
      return scope.get(pSource);
    }

    public boolean containsVerticeForField(SMGKnownAddress pAddress) {
      return scope.containsKey(pAddress);
    }

    public Set<SMGKnownAddress> getSourcesForSizeAllocationOfObject(SMGObject pObject) {
      return objectSizeSources.get(pObject);
    }

    public PathScope updateScopeForNextStep(Set<SMGKnownAddress> pNewFields,
        Map<SMGKnownAddress, SMGFlowDependenceFieldVertice> pNewVertices) {

      ImmutableMap.Builder<SMGKnownAddress, SMGFlowDependenceFieldVertice> builder =
          ImmutableMap.builder();

      for (SMGAddress field : pNewFields) {
        SMGKnownAddress newField = field.getAsKnownAddress();
        SMGFlowDependenceFieldVertice newFieldVertice = pNewVertices.get(field.getAsKnownAddress());
        builder.put(newField, newFieldVertice);
      }

      Set<SMGKnownAddress> newKeys = Sets.difference(scope.keySet(), pNewFields);

      for (SMGKnownAddress field : newKeys) {
        builder.put(field, scope.get(field));
      }

      Map<SMGKnownAddress, SMGFlowDependenceFieldVertice> newScope = builder.build();
      return new PathScope(newScope, objectSizeSources, objectAllocationPosition);
    }

    public PathScope addObjectAllocations(SMGHveSources pSources, int pPathPosition) {

      ImmutableSetMultimap.Builder<SMGObject, SMGKnownAddress> sizeSourceBuilder =
          ImmutableSetMultimap.builder();
      ImmutableMap.Builder<SMGObject, Integer> allocationPositionBuilder =
          ImmutableMap.builder();

      Set<SMGObject> newKeys = new HashSet<>();

      for (Entry<SMGObject, SMGKnownAddress> entry : pSources.getObjectMap()) {
        sizeSourceBuilder.put(entry.getKey(), entry.getValue());
        allocationPositionBuilder.put(entry.getKey(), pPathPosition);
        newKeys.add(entry.getKey());
      }

      Set<SMGObject> oldKeyObjectAllocationPosition =
          Sets.difference(objectAllocationPosition.keySet(), newKeys);

      for (SMGObject oldKey : oldKeyObjectAllocationPosition) {
        allocationPositionBuilder.put(oldKey, objectAllocationPosition.get(oldKey));
      }

      sizeSourceBuilder.putAll(objectSizeSources);

      SetMultimap<SMGObject, SMGKnownAddress> newObjectSizeSources = sizeSourceBuilder.build();
      Map<SMGObject, Integer> newObjectAllocationPosition = allocationPositionBuilder.build();
      return new PathScope(scope, newObjectSizeSources, newObjectAllocationPosition);
    }

    public PathScope initializePathScope(SMGState pInitialState,
        SMGPathDependenceResultBuilder pPathDependenceResultBuilder,
        int pStartPathPosition) {

      ImmutableMap.Builder<SMGKnownAddress, SMGFlowDependenceFieldVertice> builder = ImmutableMap.builder();

      for (SMGEdgeHasValue hve : pInitialState.getHVEdges()) {
        SMGKnownAddress field = SMGKnownAddress.valueOf(hve.getObject(), hve.getOffset());
        SMGFlowDependenceFieldVertice vertice =
            new SMGFlowDependenceFieldVertice(field, pStartPathPosition);
        builder.put(field, vertice);
        pPathDependenceResultBuilder.addNewFlowDependenceVertice(vertice);
      }

      Map<SMGKnownAddress, SMGFlowDependenceFieldVertice> newScope = builder.build();
      return new PathScope(newScope, objectSizeSources, objectAllocationPosition);
    }

    @Override
    public String toString() {
      return "PathScope [scope=" + scope + ", objectSizeSources=" + objectSizeSources
          + ", objectAllocationPosition=" + objectAllocationPosition + "]";
    }
  }
}