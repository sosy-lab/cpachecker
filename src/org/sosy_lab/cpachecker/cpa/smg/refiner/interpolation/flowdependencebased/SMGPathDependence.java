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

import com.google.common.base.Function;
import com.google.common.collect.FluentIterable;
import com.google.common.collect.HashMultimap;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.ImmutableSetMultimap;
import com.google.common.collect.Multimap;
import com.google.common.collect.SetMultimap;
import com.google.common.collect.Sets;

import org.sosy_lab.common.io.MoreFiles;
import org.sosy_lab.common.io.PathTemplate;
import org.sosy_lab.common.log.LogManager;
import org.sosy_lab.cpachecker.cpa.arg.ARGPath;
import org.sosy_lab.cpachecker.cpa.arg.ARGPath.PathIterator;
import org.sosy_lab.cpachecker.cpa.arg.ARGState;
import org.sosy_lab.cpachecker.cpa.smg.SMGAbstractionBlock;
import org.sosy_lab.cpachecker.cpa.smg.SMGTransferRelation.SMGAddress;
import org.sosy_lab.cpachecker.cpa.smg.SMGTransferRelation.SMGKnownAddress;
import org.sosy_lab.cpachecker.cpa.smg.SMGTransferRelation.SMGValue;
import org.sosy_lab.cpachecker.cpa.smg.objects.SMGObject;
import org.sosy_lab.cpachecker.cpa.smg.objects.SMGRegion;
import org.sosy_lab.cpachecker.cpa.smg.refiner.SMGMemoryPath;
import org.sosy_lab.cpachecker.cpa.smg.refiner.interpolation.SMGInterpolant;
import org.sosy_lab.cpachecker.cpa.smg.refiner.interpolation.SMGSimpleInterpolant;
import org.sosy_lab.cpachecker.cpa.smg.refiner.interpolation.flowdependencebased.SMGFlowDependenceGraph.SMGUseRange;
import org.sosy_lab.cpachecker.exceptions.RefinementFailedException;
import org.sosy_lab.cpachecker.util.states.MemoryLocation;

import java.io.IOException;
import java.nio.charset.Charset;
import java.nio.file.Path;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.logging.Level;

public class SMGPathDependence {

  private final LogManager logger;
  private final ARGPath path;
  private final SMGFlowDependenceGraph<SMGFlowDependenceFieldVertice, SMGFlowDependenceFieldEdge> smgUseGraph;
  private final Map<Integer, PathPositionMemoryPathDependencys> pathPositionMemoryPathDependencys;
  private final Map<SMGRegion, Integer> variableSizeStackMemoryLocationDeclarationPosition;
  private final boolean includeMemoryPathForEveryValidHeapObject;
  private final int pathEnd;

  public SMGPathDependence(LogManager pLogger, ARGPath pPath,
      SMGFlowDependenceGraph<SMGFlowDependenceFieldVertice, SMGFlowDependenceFieldEdge> pSmgUseGraph,
      Map<Integer, PathPositionMemoryPathDependencys> pPathPositionMemoryPathDependencys,
      Map<SMGRegion, Integer> pVariableSizeStackMemoryLocationDeclarationPosition,
      boolean pIncludeMemoryPathForEveryValidHeapObject, int pPathEnd) {
    logger = pLogger;
    path = pPath;
    smgUseGraph = pSmgUseGraph;
    pathPositionMemoryPathDependencys = pPathPositionMemoryPathDependencys;
    variableSizeStackMemoryLocationDeclarationPosition =
        pVariableSizeStackMemoryLocationDeclarationPosition;
    includeMemoryPathForEveryValidHeapObject = pIncludeMemoryPathForEveryValidHeapObject;
    pathEnd = pPathEnd;
  }

  public Map<ARGState, SMGInterpolant> obtainInterpolantsBasedOnDependency()
      throws RefinementFailedException {

    ImmutableMap.Builder<ARGState, SMGInterpolant> interpolantBuilder = ImmutableMap.builder();

    SetMultimap<Integer, SMGAddress> addressDependency = HashMultimap.create();
    SetMultimap<Integer, SMGObject> stackDependency = HashMultimap.create();

    PathIterator it = path.fullPathIterator();

    while (it.getIndex() != pathEnd) {
      it.advance();
    }

    do {

      if(!it.isPositionWithState()) {
        continue;
      }

      int pathPosition = it.getIndex();

      if (!pathPositionMemoryPathDependencys.containsKey(pathPosition)) {
        break;
      }

      PathPositionMemoryPathDependencys positionDependency =
          pathPositionMemoryPathDependencys.get(pathPosition);

      updateAddressAndStackDependency(addressDependency, stackDependency, positionDependency, pathPosition);
      Map<SMGObject, SMGAddress> memoryPathMap =
          createMemoryPathForReachableHeapObjects(addressDependency, stackDependency, positionDependency, pathPosition);
      Set<SMGAddress> fieldDependence = addressDependency.get(pathPosition);

      Set<SMGMemoryPath> trackedMemoryPathsOfCurrentPosition =
          createMemoryPathsForPosition(fieldDependence, memoryPathMap, pathPosition);

      Set<MemoryLocation> trackedStackVariablesOfCurrentPosition =
          createStackMemoryLocationsForPosition(stackDependency, pathPosition);

      ARGState currentARGState = it.getAbstractState();
      SMGInterpolant interpolantOfPosition = createInterpolant(trackedMemoryPathsOfCurrentPosition,
          trackedStackVariablesOfCurrentPosition, pathPosition);

      interpolantBuilder.put(currentARGState, interpolantOfPosition);
    } while(it.rewindIfPossible());

    return interpolantBuilder.build();
  }

  private SMGInterpolant createInterpolant(Set<SMGMemoryPath> pTrackedMemoryPaths,
      Set<MemoryLocation> pTrackedStackVariables, int pPathPos) {

    Set<SMGAbstractionBlock> blocks;

    if (pathPositionMemoryPathDependencys.containsKey(pPathPos)) {
      blocks = pathPositionMemoryPathDependencys.get(pPathPos).getBlocks();
    } else {
      blocks = ImmutableSet.of();
    }

    SMGSimpleInterpolant simpleInterpolant =
        new SMGSimpleInterpolant(blocks,
            pTrackedMemoryPaths, pTrackedStackVariables);
    return simpleInterpolant;
  }

  private void updateAddressAndStackDependency(
      SetMultimap<Integer, SMGAddress> pAddressDependency,
      SetMultimap<Integer, SMGObject> pStackDependency,
      PathPositionMemoryPathDependencys pPathPositionMemoryPathDependencys, int pPos) {

    Set<SMGKnownAddress> targets = pPathPositionMemoryPathDependencys.getPathTargets();

    if(targets.isEmpty()) {
      return;
    }

    Map<SMGKnownAddress, SMGFlowDependenceFieldVertice> scope = pPathPositionMemoryPathDependencys.getScope();

    Function<? super SMGKnownAddress, SMGFlowDependenceFieldVertice> toVertice = (SMGKnownAddress address) -> {

      SMGFlowDependenceFieldVertice targetVertices = scope.get(address);
      if (targetVertices == null) {
        return scope.values().iterator().next();
      } else {
        return targetVertices;
      }
    };

    Set<SMGFlowDependenceFieldVertice> targetVertices =
        FluentIterable.from(targets).transform(toVertice).toSet();

    Map<SMGFlowDependenceFieldVertice, SMGUseRange> dependentVertices =
        smgUseGraph.getAllTargetsAndUseRangeOfSources(targetVertices, pPos);

    updateAddressAndStackDependency(pAddressDependency, pStackDependency, dependentVertices);
  }

  private void updateAddressAndStackDependency(SetMultimap<Integer, SMGAddress> pAddressDependency,
      SetMultimap<Integer, SMGObject> pStackDependency,
      Map<SMGFlowDependenceFieldVertice, SMGUseRange> pDependentVertices) {

    for (Entry<SMGFlowDependenceFieldVertice, SMGUseRange> dependentVertice : pDependentVertices.entrySet()) {

      SMGAddress address = dependentVertice.getKey().getField();
      SMGObject object = address.getObject();
      SMGUseRange range = dependentVertice.getValue();
      boolean objectIsStackObject = !pathPositionMemoryPathDependencys.get(range.getPosStart()).getHeapObjectsOfPath().contains(object);
      for (int pos = range.getPosStart(); pos <= range.getPosUse(); pos++) {

        if (!pathPositionMemoryPathDependencys.containsKey(pos)) {
          continue;
        }

        pAddressDependency.put(pos, address);

        if (objectIsStackObject) {
          pStackDependency.put(pos, object);
        }

        PathPositionMemoryPathDependencys posDep = pathPositionMemoryPathDependencys.get(pos);

        if (posDep.containsStackPointer(address.getAsKnownAddress())) {
          pStackDependency.put(pos, posDep.getStackPointer(address.getAsKnownAddress()));
        }
      }

      boolean isVarSize = variableSizeStackMemoryLocationDeclarationPosition.containsKey(object);
      if (isVarSize) {
        int dclStatrt = variableSizeStackMemoryLocationDeclarationPosition.get(object);
        for (int pos = dclStatrt; pos < range.getPosStart(); pos++) {
          pStackDependency.put(pos, object);
        }
      }
    }
  }

  private Map<SMGObject, SMGAddress> createMemoryPathForReachableHeapObjects(
      SetMultimap<Integer, SMGAddress> pAddressDependency,
      SetMultimap<Integer, SMGObject> pStackDependency,
      PathPositionMemoryPathDependencys pPositionDependency, int pPos) {

    Set<SMGObject> heapObjects = pPositionDependency.getHeapObjectsOfPath();

    if (heapObjects.isEmpty()) {
      return ImmutableMap.of();
    }

    Set<SMGObject> reachableHeapObjects;

    if (includeMemoryPathForEveryValidHeapObject) {
      reachableHeapObjects = pPositionDependency.getValidHeapObjectsOfPath();
    } else {
      Function<SMGAddress, SMGObject> function = (SMGAddress ob) -> {
        return ob.getObject();
      };

      Set<SMGObject> reachableAddress =
          FluentIterable.from(pAddressDependency.get(pPos)).transform(function).toSet();

      reachableHeapObjects = Sets.intersection(reachableAddress, heapObjects);

      if (heapObjects.isEmpty()) {
        return ImmutableMap.of();
      }
    }

    return createMemoryPathForHeapObjects(reachableHeapObjects, pAddressDependency, pStackDependency, pPositionDependency, pPos);
  }

  private Map<SMGObject, SMGAddress> createMemoryPathForHeapObjects(
      Set<SMGObject> pHeapObjectsToReach, SetMultimap<Integer, SMGAddress> pAddressDependency,
      SetMultimap<Integer, SMGObject> pStackDependency,
      PathPositionMemoryPathDependencys pPositionDependency, int pPos) {

    Map<SMGObject, SMGAddress> result = new HashMap<>();

    for (SMGObject objectToReach : pHeapObjectsToReach) {
      if (needsToBeConnected(objectToReach, pPositionDependency, pAddressDependency.get(pPos), result)) {
        connect(objectToReach, pAddressDependency, pStackDependency, pPositionDependency, pPos,
            result);
      }
    }

    return result;
  }

  private boolean needsToBeConnected(SMGObject pObject,
      PathPositionMemoryPathDependencys pPathPositionMemoryPathDependencys,
      Set<SMGAddress> pAddressDependencyForPos, Map<SMGObject, SMGAddress> pAlreadyConnected) {

    if (pAlreadyConnected.containsKey(pObject) || !pPathPositionMemoryPathDependencys.getHeapObjectsOfPath().contains(pObject)) {
      return false;
    }

    Set<SMGKnownAddress> pointerToObject =
        pPathPositionMemoryPathDependencys.getheapObjectToPointerMap().get(pObject);

    Set<SMGKnownAddress> connectedAddresses = Sets.intersection(pointerToObject, pAddressDependencyForPos);

    if(connectedAddresses.isEmpty()) {
      return true;
    } else {

      SMGAddress defaultAddress = pPathPositionMemoryPathDependencys.getDefaultHeapObjectToPointerConnection().get(pointerToObject);

      if (connectedAddresses.contains(defaultAddress)) {
        pAlreadyConnected.put(pObject, defaultAddress);
      } else {
        pAlreadyConnected.put(pObject, connectedAddresses.iterator().next());
      }

      return false;
    }
  }

  private void connect(SMGObject pObjectToReach,
      SetMultimap<Integer, SMGAddress> pAddressDependency,
      SetMultimap<Integer, SMGObject> pStackDependency,
      PathPositionMemoryPathDependencys pPositionDependency, int pPos,
      Map<SMGObject, SMGAddress> heapToPointerMapResult) {

    SMGAddress defaultAddress =
        pPositionDependency.getDefaultHeapObjectToPointerConnection().get(pObjectToReach);

    SMGFlowDependenceFieldVertice source = pPositionDependency.getScope().get(defaultAddress);

    if (source == null) {
      logger.log(Level.INFO, "Missing scope information of :" + defaultAddress.toString());
      return;
    }

    Set<SMGFlowDependenceFieldVertice> sources =
        Collections.singleton(source);

    Map<SMGFlowDependenceFieldVertice, SMGUseRange> toAdd = smgUseGraph.getAllTargetsAndUseRangeOfSources(
        sources, pPos);

    updateAddressAndStackDependency(pAddressDependency, pStackDependency, toAdd);

    if (needsToBeConnected(defaultAddress.getObject(), pPositionDependency,
        pAddressDependency.get(pPos), heapToPointerMapResult)) {
      connect(defaultAddress.getObject(), pAddressDependency, pStackDependency, pPositionDependency,
          pPos, heapToPointerMapResult);
    }

    heapToPointerMapResult.put(pObjectToReach, defaultAddress);
  }

  private Set<MemoryLocation> createStackMemoryLocationsForPosition(
      SetMultimap<Integer, SMGObject> pStackDependency,
      int pPathPosition) {

    ImmutableSet.Builder<MemoryLocation> resultBuilder = ImmutableSet.builder();

    Set<SMGObject> stackObjects = pStackDependency.get(pPathPosition);
    PathPositionMemoryPathDependencys positionDependency =
        pathPositionMemoryPathDependencys.get(pPathPosition);
    Map<SMGObject, MemoryLocation> positionMemoryLocations =
        positionDependency.getMemoryLocations();

    for (SMGObject stackObject : stackObjects) {
      if (positionMemoryLocations.containsKey(stackObject)) {
        resultBuilder.add(positionMemoryLocations.get(stackObject));
      } else {
        logger.log(Level.ALL, () -> {
          return "Unexpected lack of memory location for stack object : "
              + stackObject.toString();
        });
      }
    }

    return resultBuilder.build();
  }

  private Set<SMGMemoryPath> createMemoryPathsForPosition(Set<SMGAddress> pFieldDependency,
      Map<SMGObject, SMGAddress> pMemoryPathMap,
      int pPathPosition) throws RefinementFailedException {

    ImmutableSet.Builder<SMGMemoryPath> resultMemoryPathsBuilder = ImmutableSet.builder();

    Map<SMGAddress, SMGMemoryPath> alreadyCreated = new HashMap<>();
    PathPositionMemoryPathDependencys positionDependency =
        pathPositionMemoryPathDependencys.get(pPathPosition);

    for (SMGAddress field : pFieldDependency) {

      SMGMemoryPath resultPath =
          createMemoryPath(field, positionDependency, pMemoryPathMap, alreadyCreated);

      if (resultPath == null) {
        continue;
      }

      resultMemoryPathsBuilder.add(resultPath);
    }

    return resultMemoryPathsBuilder.build();
  }

  private SMGMemoryPath createMemoryPath(SMGAddress pField,
      PathPositionMemoryPathDependencys pPositionDependency,
      Map<SMGObject, SMGAddress> pMemoryPathMap, Map<SMGAddress, SMGMemoryPath> pAlreadyCreated)
      throws RefinementFailedException {

    if (pAlreadyCreated.containsKey(pField)) {
      return pAlreadyCreated.get(pField);
    }

    Map<SMGObject, MemoryLocation> memlocs = pPositionDependency.getMemoryLocations();
    Map<MemoryLocation, Integer> locationOnStack = pPositionDependency.getLocationOnStack();

    SMGObject currentObject = pField.getObject();

    if (memlocs.containsKey(currentObject)) {
      return createStackpath(pField, memlocs.get(currentObject), pAlreadyCreated, locationOnStack);
    } else {
      return createHeapPath(pField, pMemoryPathMap, pPositionDependency, pAlreadyCreated);
    }
  }

  private SMGMemoryPath createHeapPath(SMGAddress pField,
      Map<SMGObject, SMGAddress> pMemoryPathMap,
      PathPositionMemoryPathDependencys pPositionDependency,
      Map<SMGAddress, SMGMemoryPath> pAlreadyCreated) throws RefinementFailedException {

    SMGMemoryPath result;
    SMGObject currentObject = pField.getObject();

    if (currentObject.getLabel() == "___cpa_temp_result_var_") {
      logger.log(Level.SEVERE, "Found no memory path to object " + currentObject.toString());
      return null;
    }

    if (!pMemoryPathMap.containsKey(currentObject)) {
      logger.log(Level.SEVERE, "Found no memory path to object " + currentObject.toString());
      return null;
//      throw new RefinementFailedException(Reason.InterpolationFailed, path);
    }

    SMGAddress parent = pMemoryPathMap.get(currentObject);

    SMGMemoryPath parentPath =
        createMemoryPath(parent, pPositionDependency, pMemoryPathMap, pAlreadyCreated);

    if(parentPath == null) {
      return null;
    }

    result = SMGMemoryPath.valueOf(parentPath, pField.getOffset().getAsInt());
    pAlreadyCreated.put(pField, result);
    return result;
  }

  private SMGMemoryPath createStackpath(SMGAddress pField, MemoryLocation pMemloc,
      Map<SMGAddress, SMGMemoryPath> pAlreadyCreated,
      Map<MemoryLocation, Integer> pLocationOnStack) {

    if (pAlreadyCreated.containsKey(pField)) {
      return pAlreadyCreated.get(pField);
    }

    SMGMemoryPath result;
    int currentOffset = pField.getOffset().getAsInt();

    if (pMemloc.isOnFunctionStack()) {
      result = SMGMemoryPath.valueOf(pMemloc.getIdentifier(), pMemloc.getFunctionName(),
          currentOffset, pLocationOnStack.get(pMemloc));
    } else {
      result = SMGMemoryPath.valueOf(pMemloc.getIdentifier(), currentOffset);
    }

    pAlreadyCreated.put(pField, result);
    return result;
  }

  public static class PathPositionMemoryPathDependencys {

    private final Set<SMGKnownAddress> pathTargets;
    private final Map<SMGKnownAddress, SMGFlowDependenceFieldVertice> scope;
    private final Set<SMGObject> heapObjectsOfPath;
    private final Set<SMGObject> validHeapObjectsOfPath;
    private final SetMultimap<SMGObject, SMGKnownAddress> heapObjectToPointerMap;
    private final Map<SMGObject, SMGKnownAddress> defaultHeapObjectToPointerConnection;
    private final Map<SMGObject, MemoryLocation> memoryLocations;
    private final Map<MemoryLocation, Integer> locationOnStack;
    private final Set<SMGAbstractionBlock> blocks;
    private final Map<SMGKnownAddress, SMGObject> stackPointer;

    public PathPositionMemoryPathDependencys(Set<SMGKnownAddress> pPathTargets,
        Map<SMGKnownAddress, SMGFlowDependenceFieldVertice> pScope, Set<SMGObject> pHeapObjectsOfPath,
        Multimap<SMGObject, SMGKnownAddress> pHeapObjectToPointerMap,
        Map<SMGObject, MemoryLocation> pMemoryLocations,
        Map<MemoryLocation, Integer> pLocationOnStack,
        Map<SMGObject, SMGKnownAddress> pDefaultHeapObjectToPointerConnection,
        Set<SMGAbstractionBlock> pBlocks, Map<SMGKnownAddress, SMGObject> pStackPointer, Set<SMGObject> pValidHeapObjectsOfPath) {
      pathTargets = ImmutableSet.copyOf(pPathTargets);
      stackPointer = ImmutableMap.copyOf(pStackPointer);
      scope = ImmutableMap.copyOf(pScope);
      heapObjectsOfPath = ImmutableSet.copyOf(pHeapObjectsOfPath);
      validHeapObjectsOfPath = ImmutableSet.copyOf(pValidHeapObjectsOfPath);
      heapObjectToPointerMap = ImmutableSetMultimap.copyOf(pHeapObjectToPointerMap);
      defaultHeapObjectToPointerConnection =
          ImmutableMap.copyOf(pDefaultHeapObjectToPointerConnection);
      memoryLocations = ImmutableMap.copyOf(pMemoryLocations);
      locationOnStack = ImmutableMap.copyOf(pLocationOnStack);
      blocks = ImmutableSet.copyOf(pBlocks);
    }

    public Set<SMGObject> getValidHeapObjectsOfPath() {
      return validHeapObjectsOfPath;
    }

    public SMGObject getStackPointer(SMGKnownAddress pAddress) {
      return stackPointer.get(pAddress);
    }

    public boolean containsStackPointer(SMGKnownAddress pAddress) {
      return stackPointer.containsKey(pAddress);
    }

    public Set<SMGKnownAddress> getPathTargets() {
      return pathTargets;
    }

    public Map<SMGKnownAddress, SMGFlowDependenceFieldVertice> getScope() {
      return scope;
    }

    public Set<SMGObject> getHeapObjectsOfPath() {
      return heapObjectsOfPath;
    }

    public SetMultimap<SMGObject, SMGKnownAddress> getheapObjectToPointerMap() {
      return heapObjectToPointerMap;
    }

    public Map<SMGObject, SMGKnownAddress> getDefaultHeapObjectToPointerConnection() {
      return defaultHeapObjectToPointerConnection;
    }

    public Map<SMGObject, MemoryLocation> getMemoryLocations() {
      return memoryLocations;
    }

    public Map<MemoryLocation, Integer> getLocationOnStack() {
      return locationOnStack;
    }

    public Set<SMGAbstractionBlock> getBlocks() {
      return blocks;
    }

    public PathPositionMemoryPathDependencys updateTarget(SMGValue pPathEndValue) {

      Set<SMGKnownAddress> newTargets;

      if (pPathEndValue.containsSourceAddreses()) {
        newTargets = pPathEndValue.getSourceAdresses();
      } else {
        newTargets = ImmutableSet.of();
      }

      return updateTarget(newTargets);
    }

    public PathPositionMemoryPathDependencys updateTarget(Set<SMGKnownAddress> pNewTargets) {
      return new PathPositionMemoryPathDependencys(Sets.union(pNewTargets, pathTargets), scope,
          heapObjectsOfPath, heapObjectToPointerMap, memoryLocations, locationOnStack,
          defaultHeapObjectToPointerConnection, blocks, stackPointer, validHeapObjectsOfPath);
    }
  }

  public void exportPathDependence(PathTemplate pExportPath,
      int pCurrentDependencePathcreationIndex, int pInterpolationId,
      int pPathDependenceId) {

    if (pExportPath == null) {
      return;
    }

    SMGFlowDependencePlotter plotter = new SMGFlowDependencePlotter(smgUseGraph);
    String pathDependenceDot = plotter.toDot();
    String fileName = "flowDependenceGraph-" + pCurrentDependencePathcreationIndex + "-id" + pPathDependenceId + ".dot";
    Path exportPath = pExportPath.getPath(pInterpolationId, fileName);

    try {
      MoreFiles.writeFile(exportPath, Charset.defaultCharset(), pathDependenceDot);
    } catch (IOException e) {
      logger.logUserException(Level.WARNING, e,
          "Failed to write interpolation path to path " + path.toString());
    }
  }
}