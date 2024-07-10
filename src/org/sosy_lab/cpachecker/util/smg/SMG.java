// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2021 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.util.smg;

import static org.sosy_lab.common.collect.Collections3.transformedImmutableSetCopy;

import com.google.common.annotations.VisibleForTesting;
import com.google.common.base.Preconditions;
import com.google.common.base.Predicate;
import com.google.common.collect.FluentIterable;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.ImmutableSortedMap;
import java.math.BigInteger;
import java.util.ArrayDeque;
import java.util.Collection;
import java.util.Comparator;
import java.util.Deque;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.NavigableMap;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import org.checkerframework.checker.nullness.qual.Nullable;
import org.sosy_lab.common.collect.PathCopyingPersistentTreeMap;
import org.sosy_lab.common.collect.PersistentMap;
import org.sosy_lab.cpachecker.cpa.smg2.util.SMGAndSMGObjects;
import org.sosy_lab.cpachecker.cpa.smg2.util.SMGObjectsAndValues;
import org.sosy_lab.cpachecker.util.smg.datastructures.PersistentSet;
import org.sosy_lab.cpachecker.util.smg.graph.SMGDoublyLinkedListSegment;
import org.sosy_lab.cpachecker.util.smg.graph.SMGHasValueEdge;
import org.sosy_lab.cpachecker.util.smg.graph.SMGObject;
import org.sosy_lab.cpachecker.util.smg.graph.SMGPointsToEdge;
import org.sosy_lab.cpachecker.util.smg.graph.SMGSinglyLinkedListSegment;
import org.sosy_lab.cpachecker.util.smg.graph.SMGTargetSpecifier;
import org.sosy_lab.cpachecker.util.smg.graph.SMGValue;
import org.sosy_lab.cpachecker.util.smg.util.SMGAndHasValueEdges;

/**
 * Class to represent a immutable bipartite symbolic memory graph. Manipulating methods return a
 * modified copy but do not modify a certain instance. Consists of (SMG-)objects, values, edges from
 * the objects to the values (has-value edges), edges from the values to objects (points-to edges)
 * and labelling functions (to get the kind, nesting level, size etc. of objects etc.)
 */
public class SMG {
  // TODO: build something like a union-find that tracks sub-SMGs and gives a common source that
  //  reaches all children such that we only need to update a sub-SMG compared to the entire
  //  SMG as we do currently. Then rework utility functions for this and re-implement removal
  //  of unnecessary edges/nodes.

  // Mapping memory <-> values and pointers -> target -> pointers towards the target might save us
  // the sub-SMG/union find searches.
  // If we want to merge those states, we might need to invalidate the valuesToRegionsTheyAreSavedIn
  // and objectsAndPointersPointingAtThem maps however partially or fully.

  // The bool is the validity of the SMGObject, not being in the map -> not valid (false)
  private final PersistentMap<SMGObject, Boolean> smgObjects;

  // TODO: we use the nesting level only for pointers at the moment
  //  (as we don't merge) -> move to points-to-edges
  private final PersistentMap<SMGValue, Integer> smgValuesAndNestingLvl;
  private final PersistentMap<SMGObject, PersistentSet<SMGHasValueEdge>> hasValueEdges;
  private final PersistentMap<SMGValue, PersistentMap<SMGObject, Integer>>
      valuesToRegionsTheyAreSavedIn;
  private final ImmutableMap<SMGValue, SMGPointsToEdge> pointsToEdges;

  // Get all pointers (values that are pointers) and # of times pointing towards an object
  private final PersistentMap<SMGObject, PersistentMap<SMGValue, Integer>>
      objectsAndPointersPointingAtThem;
  private final BigInteger sizeOfPointer;

  /** Creates a new, empty SMG */
  public SMG(BigInteger pSizeOfPointer) {
    hasValueEdges = PathCopyingPersistentTreeMap.of();
    objectsAndPointersPointingAtThem = PathCopyingPersistentTreeMap.of();
    valuesToRegionsTheyAreSavedIn = PathCopyingPersistentTreeMap.of();
    PersistentMap<SMGValue, Integer> newSMGValues =
        PathCopyingPersistentTreeMap.<SMGValue, Integer>of().putAndCopy(SMGValue.zeroValue(), 0);
    newSMGValues = newSMGValues.putAndCopy(SMGValue.zeroFloatValue(), 0);
    smgValuesAndNestingLvl = newSMGValues.putAndCopy(SMGValue.zeroDoubleValue(), 0);
    PersistentMap<SMGObject, Boolean> smgObjectsTmp = PathCopyingPersistentTreeMap.of();
    smgObjects = smgObjectsTmp.putAndCopy(SMGObject.nullInstance(), false);
    SMGPointsToEdge nullPointer =
        new SMGPointsToEdge(getNullObject(), BigInteger.ZERO, SMGTargetSpecifier.IS_REGION);
    pointsToEdges =
        ImmutableMap.of(
            SMGValue.zeroValue(),
            nullPointer,
            SMGValue.zeroFloatValue(),
            nullPointer,
            SMGValue.zeroDoubleValue(),
            nullPointer);
    sizeOfPointer = pSizeOfPointer;
  }

  private SMG(
      PersistentMap<SMGObject, Boolean> pSmgObjects,
      PersistentMap<SMGValue, Integer> pSmgValues,
      PersistentMap<SMGObject, PersistentSet<SMGHasValueEdge>> pHasValueEdges,
      PersistentMap<SMGValue, PersistentMap<SMGObject, Integer>> pValuesToRegionsTheyAreSavedIn,
      ImmutableMap<SMGValue, SMGPointsToEdge> pPointsToEdges,
      PersistentMap<SMGObject, PersistentMap<SMGValue, Integer>> pObjectsAndPointersPointingAtThem,
      BigInteger pSizeOfPointer) {
    smgObjects = pSmgObjects;
    smgValuesAndNestingLvl = pSmgValues;
    hasValueEdges = pHasValueEdges;
    valuesToRegionsTheyAreSavedIn = pValuesToRegionsTheyAreSavedIn;
    pointsToEdges = pPointsToEdges;
    sizeOfPointer = pSizeOfPointer;
    objectsAndPointersPointingAtThem = pObjectsAndPointersPointingAtThem;
  }

  private SMG copyWithNewValuesAndNestingLvl(
      PersistentMap<SMGValue, Integer> pSMGValuesAndNestingLvl) {
    return new SMG(
        smgObjects,
        pSMGValuesAndNestingLvl,
        hasValueEdges,
        valuesToRegionsTheyAreSavedIn,
        pointsToEdges,
        objectsAndPointersPointingAtThem,
        sizeOfPointer);
  }

  private SMG of(ImmutableMap<SMGValue, SMGPointsToEdge> pPointsToEdges) {
    return new SMG(
        smgObjects,
        smgValuesAndNestingLvl,
        hasValueEdges,
        valuesToRegionsTheyAreSavedIn,
        pPointsToEdges,
        objectsAndPointersPointingAtThem,
        sizeOfPointer);
  }

  private SMG of(
      PersistentMap<SMGValue, PersistentMap<SMGObject, Integer>> pValuesToRegionsTheyAreSavedIn) {
    return new SMG(
        smgObjects,
        smgValuesAndNestingLvl,
        hasValueEdges,
        pValuesToRegionsTheyAreSavedIn,
        pointsToEdges,
        objectsAndPointersPointingAtThem,
        sizeOfPointer);
  }

  private SMG ofHasValueEdges(
      PersistentMap<SMGObject, PersistentSet<SMGHasValueEdge>> pHasValueEdges) {
    return new SMG(
        smgObjects,
        smgValuesAndNestingLvl,
        pHasValueEdges,
        valuesToRegionsTheyAreSavedIn,
        pointsToEdges,
        objectsAndPointersPointingAtThem,
        sizeOfPointer);
  }

  private SMG of(
      PersistentMap<SMGObject, Boolean> pSmgObjects,
      PersistentMap<SMGObject, PersistentSet<SMGHasValueEdge>> pHasValueEdges) {
    return new SMG(
        pSmgObjects,
        smgValuesAndNestingLvl,
        pHasValueEdges,
        valuesToRegionsTheyAreSavedIn,
        pointsToEdges,
        objectsAndPointersPointingAtThem,
        sizeOfPointer);
  }

  /**
   * Creates a copy of the SMG an adds the given object.
   *
   * @param pObject - The object to be added.
   * @return A modified copy of the SMG.
   */
  public SMG copyAndAddObject(SMGObject pObject) {
    return new SMG(
        smgObjects.putAndCopy(pObject, true),
        smgValuesAndNestingLvl,
        hasValueEdges,
        valuesToRegionsTheyAreSavedIn,
        pointsToEdges,
        objectsAndPointersPointingAtThem,
        sizeOfPointer);
  }

  /**
   * Creates a copy of the SMG an adds the given value w nesting level given.
   *
   * @param pValue - The value to be added.
   * @param nestingLvl - The values nesting level.
   * @return A modified copy of the SMG.
   */
  public SMG copyAndAddValue(SMGValue pValue, int nestingLvl) {
    return new SMG(
        smgObjects,
        smgValuesAndNestingLvl.putAndCopy(pValue, nestingLvl),
        hasValueEdges,
        valuesToRegionsTheyAreSavedIn,
        pointsToEdges,
        objectsAndPointersPointingAtThem,
        sizeOfPointer);
  }

  /**
   * Creates a copy of the SMG an adds the given value w nesting level of the value.
   *
   * @param pValue - The value to be added. Assumes nesting level 0.
   * @return A modified copy of the SMG.
   */
  public SMG copyAndAddValueWithNestingLevelZero(SMGValue pValue) {
    return new SMG(
        smgObjects,
        smgValuesAndNestingLvl.putAndCopy(pValue, 0),
        hasValueEdges,
        valuesToRegionsTheyAreSavedIn,
        pointsToEdges,
        objectsAndPointersPointingAtThem,
        sizeOfPointer);
  }

  /**
   * Creates a copy of the SMG and remove the given value.
   *
   * @param pValue - The object to be added.
   * @return A modified copy of the SMG.
   */
  public SMG copyAndRemoveValue(SMGValue pValue) {
    return new SMG(
        smgObjects,
        smgValuesAndNestingLvl.removeAndCopy(pValue),
        hasValueEdges,
        valuesToRegionsTheyAreSavedIn,
        pointsToEdges,
        objectsAndPointersPointingAtThem,
        sizeOfPointer);
  }

  /**
   * Removes the {@link SMGPointsToEdge} associated with the entered {@link SMGValue} iff there is a
   * {@link SMGPointsToEdge}, else does nothing. Caution when using this, should only every be used
   * to remove values/PTEdges that are no longer used after this! This will modify the pointer to
   * object map!
   *
   * @param pValue the {@link SMGValue} for which the {@link SMGPointsToEdge} should be removed.
   * @return a new {@link SMG} in which the mapping is removed.
   */
  public SMG copyAndRemovePointsToEdge(SMGValue pValue) {
    if (!pointsToEdges.containsKey(pValue)) {
      return this;
    }
    ImmutableMap.Builder<SMGValue, SMGPointsToEdge> builder = ImmutableMap.builder();
    SMGObject target = null;
    for (Entry<SMGValue, SMGPointsToEdge> entry : pointsToEdges.entrySet()) {
      if (!entry.getKey().equals(pValue)) {
        builder = builder.put(entry);
      } else {
        target = entry.getValue().pointsTo();
      }
    }
    if (target != null) {
      SMG newSMG = decrementPointerToObjectMap(pValue, target);
      return new SMG(
          newSMG.smgObjects,
          newSMG.smgValuesAndNestingLvl,
          newSMG.hasValueEdges,
          newSMG.valuesToRegionsTheyAreSavedIn,
          builder.buildOrThrow(),
          newSMG.objectsAndPointersPointingAtThem,
          newSMG.sizeOfPointer);
    }
    return new SMG(
        smgObjects,
        smgValuesAndNestingLvl,
        hasValueEdges,
        valuesToRegionsTheyAreSavedIn,
        builder.buildOrThrow(),
        objectsAndPointersPointingAtThem,
        sizeOfPointer);
  }

  /**
   * Removes the {@link SMGPointsToEdge} associated with the entered {@link SMGValue} iff there is a
   * {@link SMGPointsToEdge}, else does nothing. Caution when using this, should only every be used
   * to remove values/PTEdges that are no longer used after this! This will not modify the pointer
   * to object map or the values and where they are saved in map.
   *
   * @param pValue the {@link SMGValue} for which the {@link SMGPointsToEdge} should be removed.
   * @return a new {@link SMG} in which the mapping is removed.
   */
  public SMG copyAndRemovePointsToEdgeWithoutSideEffects(SMGValue pValue) {
    if (!pointsToEdges.containsKey(pValue)) {
      return this;
    }
    Preconditions.checkArgument(!valuesToRegionsTheyAreSavedIn.containsKey(pValue));
    ImmutableMap.Builder<SMGValue, SMGPointsToEdge> builder = ImmutableMap.builder();
    for (Entry<SMGValue, SMGPointsToEdge> entry : pointsToEdges.entrySet()) {
      if (!entry.getKey().equals(pValue)) {
        builder = builder.put(entry);
      }
    }
    return new SMG(
        smgObjects,
        smgValuesAndNestingLvl,
        hasValueEdges,
        valuesToRegionsTheyAreSavedIn,
        builder.buildOrThrow(),
        objectsAndPointersPointingAtThem,
        sizeOfPointer);
  }

  /**
   * Returns the number of {@link SMGHasValueEdge}s that have the {@link SMGValue} in them.
   *
   * @param pValue {@link SMGValue} to search usages for.
   * @return number of usages of the pValue.
   */
  public int getNumberOfSMGValueUsages(SMGValue pValue) {
    int found = 0;
    for (PersistentSet<SMGHasValueEdge> hvEdges : hasValueEdges.values()) {
      for (SMGHasValueEdge hve : hvEdges) {
        if (hve.hasValue().equals(pValue)) {
          found++;
        }
      }
    }
    return found;
  }

  /**
   * Returns the number of (distinct) SMGValues that have a {@link SMGPointsToEdge} pointing towards
   * the target {@link SMGObject}. Different nesting levels and offsets are counted separately.
   *
   * @param target {@link SMGObject} that the pointers point towards.
   * @return num of distinct pointers towards target.
   */
  public int getNumberOfSMGPointsToEdgesTowards(SMGObject target) {
    Set<SMGValue> pointerValues = new HashSet<>();
    for (Entry<SMGValue, SMGPointsToEdge> entry : pointsToEdges.entrySet()) {
      if (entry.getValue().pointsTo().equals(target)) {
        pointerValues.add(entry.getKey());
      }
    }
    return pointerValues.size();
  }

  public SMG copyAndRemoveValues(Collection<SMGValue> pUnreachableValues) {
    SMG returnSmg = this;
    for (SMGValue smgValue : pUnreachableValues) {
      returnSmg = returnSmg.copyAndRemoveValue(smgValue);
    }
    return returnSmg;
  }

  /**
   * Creates a copy of the SMG an adds the given has value edge. Does increment the value and
   * pointer reverse maps!
   *
   * @param edge - The edge to be added.
   * @param source - The source object.
   * @return A modified copy of the SMG.
   */
  public SMG copyAndAddHVEdge(SMGHasValueEdge edge, SMGObject source) {
    if (hasValueEdges.containsKey(source)
        && hasValueEdges.getOrDefault(source, PersistentSet.of()).contains(edge)) {
      return this;
    }

    PersistentSet<SMGHasValueEdge> edges = hasValueEdges.getOrDefault(source, PersistentSet.of());
    edges = edges.addAndCopy(edge);

    return ofHasValueEdges(hasValueEdges.putAndCopy(source, edges))
        .incrementValueToMemoryMapEntry(source, edge.hasValue());
  }

  /**
   * Creates a copy of the SMG an adds/replaces the given points to edge.
   *
   * @param edge - The edge to be added/replaced.
   * @param source - The source value.
   * @return A modified copy of the SMG.
   */
  public SMG copyAndAddPTEdge(SMGPointsToEdge edge, SMGValue source) {
    if (pointsToEdges.containsKey(source)) {
      if (Objects.equals(pointsToEdges.get(source), edge)) {
        return this;
      }
      throw new RuntimeException("A SMG-points-to-edge can have only 1 target!");
    }

    ImmutableMap.Builder<SMGValue, SMGPointsToEdge> pointsToEdgesBuilder = ImmutableMap.builder();
    pointsToEdgesBuilder.putAll(pointsToEdges);
    pointsToEdgesBuilder.put(source, edge);
    // Don't increment the pointer to target obj map, as this might add pointers that are not really
    // saved in an object yet. Increment when they are saved in an obj.
    // SMG newSMG = incrementPointerToObjectMap(source, edge.pointsTo());
    SMG newSMG = of(pointsToEdgesBuilder.buildOrThrow());
    return newSMG;
  }

  /**
   * Creates a copy of the SMG an adds/replaces the old edges with the given has value edges.
   *
   * @param edges - the edges to be added/replaced.
   * @param source - the source object.
   * @return a modified copy of the SMG.
   */
  public SMG copyAndSetHVEdges(PersistentSet<SMGHasValueEdge> edges, SMGObject source) {
    return new SMG(
        smgObjects,
        smgValuesAndNestingLvl,
        hasValueEdges.putAndCopy(source, edges),
        valuesToRegionsTheyAreSavedIn,
        pointsToEdges,
        objectsAndPointersPointingAtThem,
        sizeOfPointer);
  }

  public int getNumberOfAbstractedLists() {
    int num = 0;
    for (Entry<SMGObject, Boolean> obj : smgObjects.entrySet()) {
      if (obj.getValue() && obj.getKey() instanceof SMGSinglyLinkedListSegment) {
        num++;
      }
    }
    return num;
  }

  public int getNumberOfValueUsages(SMGValue value) {
    PersistentMap<SMGObject, Integer> sourceObjectsMap =
        valuesToRegionsTheyAreSavedIn.getOrDefault(value, PathCopyingPersistentTreeMap.of());
    return sourceObjectsMap.values().stream().reduce(0, Integer::sum);
  }

  public Set<SMGObject> getAllObjectsWithValueInThem(SMGValue value) {
    PersistentMap<SMGObject, Integer> sourceObjectsMap =
        valuesToRegionsTheyAreSavedIn.getOrDefault(value, PathCopyingPersistentTreeMap.of());
    return sourceObjectsMap.keySet();
  }

  public SMG copyAndAddHVEdges(Iterable<SMGHasValueEdge> edges, SMGObject source) {
    PersistentSet<SMGHasValueEdge> smgEdges = hasValueEdges.get(source);
    PersistentMap<SMGValue, PersistentMap<SMGObject, Integer>> newValuesToRegionsTheyAreSavedIn =
        valuesToRegionsTheyAreSavedIn;
    for (SMGHasValueEdge edgeToAdd : edges) {
      smgEdges = smgEdges.addAndCopy(edgeToAdd);
      PersistentMap<SMGObject, Integer> sourceObjectsMap =
          newValuesToRegionsTheyAreSavedIn.getOrDefault(
              edgeToAdd.hasValue(), PathCopyingPersistentTreeMap.of());
      int currentNumberOfValuesInSource = sourceObjectsMap.getOrDefault(source, 0) + 1;
      newValuesToRegionsTheyAreSavedIn =
          valuesToRegionsTheyAreSavedIn.putAndCopy(
              edgeToAdd.hasValue(),
              sourceObjectsMap.putAndCopy(source, currentNumberOfValuesInSource));
    }

    return new SMG(
        smgObjects,
        smgValuesAndNestingLvl,
        hasValueEdges.putAndCopy(source, smgEdges),
        newValuesToRegionsTheyAreSavedIn,
        pointsToEdges,
        objectsAndPointersPointingAtThem,
        sizeOfPointer);
  }

  /**
   * Creates a copy of the SMG a removes the given has value edges. Also modifies the
   * valuesToRegionsTheyAreSavedIn and objectsAndPointersPointingAtThem maps.
   *
   * @param edges - The edges to be removed.
   * @param source - The source object.
   * @return A modified copy of the SMG.
   */
  public SMG copyAndRemoveHVEdges(Iterable<SMGHasValueEdge> edges, SMGObject source) {
    PersistentSet<SMGHasValueEdge> smgEdges =
        hasValueEdges.getOrDefault(source, PersistentSet.of());
    SMG newSMG = this;
    for (SMGHasValueEdge edgeToRemove : edges) {
      smgEdges = smgEdges.removeAndCopy(edgeToRemove);
      newSMG = newSMG.decrementValueToMemoryMapEntry(source, edgeToRemove.hasValue());
    }

    return newSMG.ofHasValueEdges(hasValueEdges.putAndCopy(source, smgEdges));
  }

  /**
   * Returns a new SMG with the HVEdges replaced by the given.
   *
   * @param objectToReplace the object whose edges are supposed to be changed.
   * @param newHVEdges the new HVedges.
   */
  public SMG copyAndReplaceHVEdgesAt(
      SMGObject objectToReplace, PersistentSet<SMGHasValueEdge> newHVEdges) {
    if (newHVEdges.isEmpty()) {
      if (hasValueEdges.get(objectToReplace) == null) {
        return this;
      }
      return copyAndRemoveHVEdges(hasValueEdges.get(objectToReplace), objectToReplace);
    }
    // TODO: this might change pointers
    for (SMGHasValueEdge edge : newHVEdges) {
      assert !pointsToEdges.containsKey(edge.hasValue());
    }
    // TODO: valuesToRegionsTheyAreSavedIn is not updated
    throw new RuntimeException("Implement me (CEGAR ONLY currently)");
    /*
    return new SMG(
        smgObjects,
        smgValues,
        hasValueEdges.removeAndCopy(objectToReplace).putAndCopy(objectToReplace, newHVEdges), valuesToRegionsTheyAreSavedIn,
        pointsToEdges, objectsAndPointersPointingAtThem,
        sizeOfPointer);*/
  }

  /**
   * Creates a copy of the SMG and adds/replaces the given points to edge. Also modifies the
   * pointersTowardsObjectsMap accordingly.
   *
   * @param edge - The edge to be added/replaced.
   * @param newSource - The new source value.
   * @return A modified copy of the SMG.
   */
  public SMG copyAndSetPTEdges(SMGPointsToEdge edge, SMGValue newSource) {
    ImmutableMap.Builder<SMGValue, SMGPointsToEdge> pointsToEdgesBuilder = ImmutableMap.builder();
    SMG newSMG = this;
    if (pointsToEdges.containsKey(newSource)) {
      // Replacing an existing pte. This only changes the association of the PTE to the value, not
      // the values.
      for (Entry<SMGValue, SMGPointsToEdge> entry : pointsToEdges.entrySet()) {
        if (!entry.getKey().equals(newSource)) {
          pointsToEdgesBuilder.put(entry);
        } else {
          pointsToEdgesBuilder.put(newSource, edge);
          // increment PointerToObjectMap only for pointers existing in memory

          newSMG =
              newSMG.switchAllPointerToObjectMap(
                  newSource, pointsToEdges.get(newSource).pointsTo(), edge.pointsTo());
        }
      }
    } else {
      // new pointer for this value, not necessarily new pointer towards the target
      pointsToEdgesBuilder.putAll(pointsToEdges);
      pointsToEdgesBuilder.put(newSource, edge);
      newSMG = incrementPointerToObjectMap(newSource, edge.pointsTo());
    }
    return newSMG.of(pointsToEdgesBuilder.buildOrThrow());
  }

  /**
   * Creates a copy of the SMG and replaces a given edge with another. Does change the reverse value
   * and pointer to objects maps!
   *
   * @param pSmgObject The source SMGObject.
   * @param pOldEdge Edge to be replaced.
   * @param pNewEdge Replacement edge.
   * @return A copy of the graph with the replaced edge.
   */
  public SMG copyAndReplaceHVEdge(
      SMGObject pSmgObject, SMGHasValueEdge pOldEdge, SMGHasValueEdge pNewEdge) {
    // remove 1 from value mapping for old value (includes pointer mapping)
    SMG newSMG = decrementValueToMemoryMapEntry(pSmgObject, pOldEdge.hasValue());
    // Add 1 to value mapping for new value (includes pointer mapping)
    newSMG = newSMG.incrementValueToMemoryMapEntry(pSmgObject, pNewEdge.hasValue());
    // Replace the value
    PersistentSet<SMGHasValueEdge> objEdges =
        hasValueEdges.get(pSmgObject).removeAndCopy(pOldEdge).addAndCopy(pNewEdge);
    return newSMG.copyAndSetHVEdges(objEdges, pSmgObject);
  }

  /**
   * Decrements the occurrence of the value given in the object given. Removes the mapping if
   * decrements to 0. (This includes the pointer pointing to objects mapping but not PTE map)
   *
   * @param pSmgObject the object in which the value was removed
   * @param pOldValue the value removed
   * @return a new SMG with valuesToRegionsTheyAreSavedIn and potentially
   *     objectsAndPointersPointingAtThem changed
   */
  private SMG decrementValueToMemoryMapEntry(SMGObject pSmgObject, SMGValue pOldValue) {
    PersistentMap<SMGObject, Integer> oldInnerMapOldV =
        valuesToRegionsTheyAreSavedIn.getOrDefault(pOldValue, PathCopyingPersistentTreeMap.of());
    Integer oldNumOldV = oldInnerMapOldV.get(pSmgObject);
    if (oldNumOldV == null) {
      // Can happen because some tests don't save pointers in memory
      return this;
    }
    PersistentMap<SMGValue, PersistentMap<SMGObject, Integer>> newValuesToRegionsTheyAreSavedIn;
    if (oldNumOldV > 1) {
      newValuesToRegionsTheyAreSavedIn =
          valuesToRegionsTheyAreSavedIn.putAndCopy(
              pOldValue, oldInnerMapOldV.putAndCopy(pSmgObject, oldNumOldV - 1));
    } else {
      if (oldInnerMapOldV.size() == 1) {
        // remove the entry
        newValuesToRegionsTheyAreSavedIn = valuesToRegionsTheyAreSavedIn.removeAndCopy(pOldValue);
      } else {
        // remove the inner entry
        newValuesToRegionsTheyAreSavedIn =
            valuesToRegionsTheyAreSavedIn.putAndCopy(
                pOldValue, oldInnerMapOldV.removeAndCopy(pSmgObject));
      }
    }
    // Also decrement pointer tracking
    SMG newSMG = this;
    if (isPointer(pOldValue)) {
      SMGObject target = pointsToEdges.get(pOldValue).pointsTo();
      newSMG = decrementPointerToObjectMap(pOldValue, target);
    }
    return newSMG.of(newValuesToRegionsTheyAreSavedIn);
  }

  /**
   * Increments the occurrence of the value given in the object given. Adds the mapping if
   * necessary. Does increment pointer mappings!
   *
   * @param pSmgObject the object in which the value was added
   * @param pNewValue the value added
   * @return a new SMG with the valuesToRegionsTheyAreSavedIn
   */
  private SMG incrementValueToMemoryMapEntry(SMGObject pSmgObject, SMGValue pNewValue) {
    PersistentMap<SMGObject, Integer> oldInnerMapNewV =
        valuesToRegionsTheyAreSavedIn.getOrDefault(pNewValue, PathCopyingPersistentTreeMap.of());
    Integer oldNumNewV = oldInnerMapNewV.getOrDefault(pSmgObject, 0);
    SMG newSMG =
        of(
            valuesToRegionsTheyAreSavedIn.putAndCopy(
                pNewValue, oldInnerMapNewV.putAndCopy(pSmgObject, oldNumNewV + 1)));
    if (isPointer(pNewValue)) {
      newSMG =
          newSMG.incrementPointerToObjectMap(pNewValue, pointsToEdges.get(pNewValue).pointsTo());
    }

    return newSMG;
  }

  private SMG decrementPointerToObjectMap(SMGValue pointer, SMGObject target) {
    if (pointer.isZero()) {
      return this;
    }
    PersistentMap<SMGValue, Integer> innerMap = objectsAndPointersPointingAtThem.get(target);
    if (innerMap == null) {
      // Can happen for example in tests when there are pointers not saved in objects
      return this;
    }

    Integer currentNum = innerMap.get(pointer);
    Preconditions.checkNotNull(currentNum);
    PersistentMap<SMGObject, PersistentMap<SMGValue, Integer>> newObjectsAndPointersPointingAtThem;
    if (currentNum > 1) {
      newObjectsAndPointersPointingAtThem =
          objectsAndPointersPointingAtThem
              .removeAndCopy(target)
              .putAndCopy(target, innerMap.putAndCopy(pointer, currentNum - 1));
    } else {
      if (innerMap.size() == 1) {
        // remove whole entry
        newObjectsAndPointersPointingAtThem =
            objectsAndPointersPointingAtThem.removeAndCopy(target);
      } else {
        // Remove only inner entry
        newObjectsAndPointersPointingAtThem =
            objectsAndPointersPointingAtThem.putAndCopy(target, innerMap.removeAndCopy(pointer));
      }
    }
    return new SMG(
        smgObjects,
        smgValuesAndNestingLvl,
        hasValueEdges,
        valuesToRegionsTheyAreSavedIn,
        pointsToEdges,
        newObjectsAndPointersPointingAtThem,
        sizeOfPointer);
  }

  /**
   * Switches ALL pointers pointerToSwitch that pointed towards oldTarget to newTarget.
   *
   * @param pointerToSwitch the pointer to switch
   * @param oldTarget the old target of the ptr
   * @param newTarget the new target of the ptr
   * @return a new SMG with all ptrs given switched
   */
  private SMG switchAllPointerToObjectMap(
      SMGValue pointerToSwitch, SMGObject oldTarget, SMGObject newTarget) {
    if (pointerToSwitch.isZero()) {
      return this;
    }
    PersistentMap<SMGValue, Integer> oldTargetInnerMap =
        objectsAndPointersPointingAtThem.get(oldTarget);
    if (oldTargetInnerMap == null) {
      // Can happen for example in tests when there are pointers not saved in objects
      return this;
    }

    Integer switchNum = oldTargetInnerMap.get(pointerToSwitch);
    Preconditions.checkNotNull(switchNum);
    if (switchNum == 0) {
      return this;
    }
    PersistentMap<SMGObject, PersistentMap<SMGValue, Integer>> newObjectsAndPointersPointingAtThem;

    if (oldTargetInnerMap.size() == 1) {
      // remove whole entry
      newObjectsAndPointersPointingAtThem =
          objectsAndPointersPointingAtThem.removeAndCopy(oldTarget);
    } else {
      // Remove only inner entry
      newObjectsAndPointersPointingAtThem =
          objectsAndPointersPointingAtThem.putAndCopy(
              oldTarget, oldTargetInnerMap.removeAndCopy(pointerToSwitch));
    }

    PersistentMap<SMGValue, Integer> newTargetInnerMap =
        newObjectsAndPointersPointingAtThem.getOrDefault(
            newTarget, PathCopyingPersistentTreeMap.of());
    int existingNumInNewTarget = newTargetInnerMap.getOrDefault(pointerToSwitch, 0);
    newObjectsAndPointersPointingAtThem =
        newObjectsAndPointersPointingAtThem
            .removeAndCopy(newTarget)
            .putAndCopy(
                newTarget,
                newTargetInnerMap
                    .removeAndCopy(pointerToSwitch)
                    .putAndCopy(pointerToSwitch, switchNum + existingNumInNewTarget));

    return new SMG(
        smgObjects,
        smgValuesAndNestingLvl,
        hasValueEdges,
        valuesToRegionsTheyAreSavedIn,
        pointsToEdges,
        newObjectsAndPointersPointingAtThem,
        sizeOfPointer);
  }

  /**
   * Increments the occurrence of the pointer towards the target in the
   * objectsAndPointersPointingAtThem map. Make sure to only increment this for pointers that are
   * actually saved in an object!
   *
   * @param pointer the pointer pointing at the target AND is currently saved in some object.
   * @param target target of the pointer.
   * @return new SMG with the map incremented
   */
  private SMG incrementPointerToObjectMap(SMGValue pointer, SMGObject target) {
    if (pointer.isZero()) {
      return this;
    }
    // Assert that the pointer is truly saved somewhere in the memory
    assert valuesToRegionsTheyAreSavedIn.containsKey(pointer);
    PersistentMap<SMGValue, Integer> innerMap =
        objectsAndPointersPointingAtThem.getOrDefault(target, PathCopyingPersistentTreeMap.of());
    Integer currentNum = innerMap.getOrDefault(pointer, 0);
    PersistentMap<SMGObject, PersistentMap<SMGValue, Integer>> newObjectsAndPointersPointingAtThem;
    newObjectsAndPointersPointingAtThem =
        objectsAndPointersPointingAtThem
            .removeAndCopy(target)
            .putAndCopy(
                target, innerMap.removeAndCopy(pointer).putAndCopy(pointer, currentNum + 1));
    return new SMG(
        smgObjects,
        smgValuesAndNestingLvl,
        hasValueEdges,
        valuesToRegionsTheyAreSavedIn,
        pointsToEdges,
        newObjectsAndPointersPointingAtThem,
        sizeOfPointer);
  }

  /** Replaces all HVedges at the offset + size with the new HVEdge in the given objects. */
  public SMG copyAndReplaceHVEdgeAt(
      SMGObject pSmgObject,
      BigInteger offsetInBits,
      BigInteger sizeInBits,
      SMGHasValueEdge pNewEdge) {
    FluentIterable<SMGHasValueEdge> currentEdges =
        getHasValueEdgesByPredicate(
            pSmgObject,
            n ->
                n.getOffset().compareTo(offsetInBits) == 0
                    && n.getSizeInBits().compareTo(sizeInBits) == 0);
    SMG newSMG = this;
    for (SMGHasValueEdge oldEdge : currentEdges) {
      newSMG = newSMG.copyAndReplaceHVEdge(pSmgObject, oldEdge, pNewEdge);
    }
    return newSMG;
  }

  /**
   * Creates a copy of the SMG and replaces given object by a given new. This also switches pointers
   * towards the old obj to the new. If the newTarget is a region, specifiers are set to region. All
   * other specifiers are retained.
   *
   * @param pOldObject - The object to be replaced.
   * @param pNewObject - The replacement object.
   * @return A modified copy.
   */
  public SMG copyAndReplaceObject(SMGObject pOldObject, SMGObject pNewObject) {
    PersistentSet<SMGHasValueEdge> hvEdges = hasValueEdges.get(pOldObject);
    // replace has value edges
    PersistentMap<SMGObject, PersistentSet<SMGHasValueEdge>> newHVEdges =
        hasValueEdges.removeAndCopy(pOldObject).putAndCopy(pNewObject, hvEdges);
    SMG newSMG = this;
    for (SMGHasValueEdge hve : hvEdges) {
      // Switch values from the old obj towards the new
      SMGValue value = hve.hasValue();
      newSMG = newSMG.decrementValueToMemoryMapEntry(pOldObject, value);
      newSMG = newSMG.incrementValueToMemoryMapEntry(pNewObject, value);
    }

    // replace points to edges pointing towards the old obj
    // If the newTarget is a region, specifiers are set to region. All other specifiers are retained
    newSMG = replaceAllPointersTowardsWith(pOldObject, pNewObject);

    // replace object
    PersistentMap<SMGObject, Boolean> newObjects =
        smgObjects.removeAndCopy(pOldObject).putAndCopy(pNewObject, true);

    return newSMG.of(newObjects, newHVEdges);
  }

  /**
   * Invalidates the entered SMGObject (that is assumed to be in the SMG!). This also deletes all
   * {@link SMGHasValueEdge}s associated with the object.
   *
   * @param pObject the {@link SMGObject} to be invalidated.
   * @param deleteDanglingPointers true if all pointers dangling (not being saved in any object) are
   *     to be deleted. (e.g. for dropping stack frames with hanging values/pointers)
   * @return a new SMG with the object invalidated and all its HVEdges deleted.
   */
  public SMG copyAndInvalidateObject(SMGObject pObject, boolean deleteDanglingPointers) {
    PersistentMap<SMGObject, Boolean> newObjects = smgObjects.putAndCopy(pObject, false);
    SMG newSMG = decrementHVEdgesInValueToMemoryMap(pObject, deleteDanglingPointers);
    PersistentMap<SMGObject, PersistentSet<SMGHasValueEdge>> newHVEdges =
        hasValueEdges.removeAndCopy(pObject);

    // TODO: delete PTEs with no values left
    // TODO: actually delete old objects with no references to them (for example the dirt from
    //  SLL/DLL creation/deletion)
    return newSMG.of(newObjects, newHVEdges);
  }

  /**
   * @param pObject the object whos HVEs are to be removed.
   * @param deleteDanglingPointers true if pointers that have no saved HVEs are to be deleted from
   *     the PTE map.
   * @return a new SMG with the edges ob pObject removed from value to memory map and pointer points
   *     to object map and possibly the pointers deleted from the PTE mapping.
   */
  private SMG decrementHVEdgesInValueToMemoryMap(
      SMGObject pObject, boolean deleteDanglingPointers) {
    SMG newValuesToRegionsTheyAreSavedIn = this;
    for (SMGHasValueEdge hve : hasValueEdges.getOrDefault(pObject, PersistentSet.of())) {
      newValuesToRegionsTheyAreSavedIn =
          newValuesToRegionsTheyAreSavedIn.decrementValueToMemoryMapEntry(pObject, hve.hasValue());
      SMGValue value = hve.hasValue();
      if (deleteDanglingPointers
          && newValuesToRegionsTheyAreSavedIn.valuesToRegionsTheyAreSavedIn.get(value) == null) {
        if (newValuesToRegionsTheyAreSavedIn.pointsToEdges.containsKey(value)) {
          ImmutableMap.Builder<SMGValue, SMGPointsToEdge> builder = ImmutableMap.builder();
          for (Entry<SMGValue, SMGPointsToEdge> entry :
              newValuesToRegionsTheyAreSavedIn.pointsToEdges.entrySet()) {
            if (!entry.getKey().equals(value)) {
              builder = builder.put(entry);
            }
          }
          newValuesToRegionsTheyAreSavedIn =
              new SMG(
                  newValuesToRegionsTheyAreSavedIn.smgObjects,
                  newValuesToRegionsTheyAreSavedIn.smgValuesAndNestingLvl,
                  newValuesToRegionsTheyAreSavedIn.hasValueEdges,
                  newValuesToRegionsTheyAreSavedIn.valuesToRegionsTheyAreSavedIn,
                  builder.buildOrThrow(),
                  newValuesToRegionsTheyAreSavedIn.objectsAndPointersPointingAtThem,
                  newValuesToRegionsTheyAreSavedIn.sizeOfPointer);
        }
      }
    }

    return newValuesToRegionsTheyAreSavedIn;
  }

  /**
   * Validated the entered SMGObject (that is assumed to be in the SMG!).
   *
   * @param pObject the {@link SMGObject} to be validated.
   * @return a new SMG with the object validated.
   */
  public SMG copyAndValidateObject(SMGObject pObject) {
    PersistentMap<SMGObject, Boolean> newObjects = smgObjects.putAndCopy(pObject, true);
    return new SMG(
        newObjects,
        smgValuesAndNestingLvl,
        hasValueEdges,
        valuesToRegionsTheyAreSavedIn,
        pointsToEdges,
        objectsAndPointersPointingAtThem,
        sizeOfPointer);
  }

  public SMG copyAndRemoveObjects(Collection<SMGObject> pUnreachableObjects) {
    SMG returnSmg = this;
    for (SMGObject smgObject : pUnreachableObjects) {
      returnSmg = returnSmg.copyAndInvalidateObject(smgObject, false);
    }
    // assert returnSmg.checkValueInConcreteMemorySanity();
    return returnSmg;
  }

  public SMG copyAndRemoveAbstractedObjectFromHeap(SMGObject pUnreachableObject) {
    assert objectsAndPointersPointingAtThem
            .getOrDefault(pUnreachableObject, PathCopyingPersistentTreeMap.of())
            .size()
        == 0;
    PersistentMap<SMGObject, Boolean> newObjects = smgObjects.removeAndCopy(pUnreachableObject);
    PersistentSet<SMGHasValueEdge> values =
        hasValueEdges.getOrDefault(pUnreachableObject, PersistentSet.of());
    SMG newSMG = copyAndRemoveHVEdges(values, pUnreachableObject);
    // TODO: remove unused values
    return newSMG.of(newObjects, newSMG.hasValueEdges);
  }

  /**
   * Removes e.g. a 0+ object from the SMG. This assumed that no valid pointers to the object exist
   * and removes all pointers TOWARDS the object and all objects that are connected to those
   * pointers (removes the subgraph). Also deleted the object in the end. This does not check for
   * pointers that point away from the object!
   *
   * @param objectToRemove the object to remove and start the subgraph removal.
   * @return a new SMG with the object and its subgraphs removed + the all removed objects.
   */
  public SMGAndSMGObjects copyAndRemoveObjectAndSubSMG(SMGObject objectToRemove) {
    // TODO: use for both DLL and SLL
    // TODO: remwork as this is shit! Needs to remove the
    if (!smgObjects.containsKey(objectToRemove) || !isValid(objectToRemove)) {
      return SMGAndSMGObjects.ofEmptyObjects(this);
    }
    ImmutableMap.Builder<SMGValue, SMGPointsToEdge> newPointers = ImmutableMap.builder();
    ImmutableSet.Builder<SMGObject> objectsToRemoveBuilder = ImmutableSet.builder();
    ImmutableSet.Builder<SMGValue> valuesToRemoveBuilder = ImmutableSet.builder();
    // We expect there to be very few, if any objects towards a 0+ element as we don't join
    // TODO: use pointersTowardsObjectsMap
    // Find all pointers towards the object
    for (Entry<SMGValue, SMGPointsToEdge> pointsToEntry : pointsToEdges.entrySet()) {
      if (pointsToEntry.getValue().pointsTo().equals(objectToRemove)) {
        valuesToRemoveBuilder.add(pointsToEntry.getKey());
      } else {
        newPointers.put(pointsToEntry);
      }
    }

    ImmutableSet<SMGValue> valuesToRemove = valuesToRemoveBuilder.build();
    // Find all objects w pointers pointing towards the object
    // TODO: use pointersTowardsObjectsMap
    if (!valuesToRemove.isEmpty()) {
      for (PersistentSet<SMGHasValueEdge> hasValueEntryValue : hasValueEdges.values()) {
        for (SMGHasValueEdge valueEdge : hasValueEntryValue) {
          if (valuesToRemove.contains(valueEdge.hasValue())) {
            objectsToRemoveBuilder.add(objectToRemove);
          }
        }
      }
    }

    SMG newSMG = decrementHVEdgesInValueToMemoryMap(objectToRemove, true);

    // Remove object from the SMG and remove all values inside from the SMG
    SMG currentSMG =
        new SMG(
            newSMG.smgObjects.removeAndCopy(objectToRemove),
            newSMG.smgValuesAndNestingLvl,
            newSMG.hasValueEdges.removeAndCopy(objectToRemove),
            newSMG.valuesToRegionsTheyAreSavedIn,
            newPointers.buildOrThrow(),
            newSMG.objectsAndPointersPointingAtThem,
            newSMG.sizeOfPointer);
    ImmutableSet<SMGObject> objectsToRemove = objectsToRemoveBuilder.build();
    ImmutableSet.Builder<SMGObject> objectsThatHaveBeenRemovedBuilder = ImmutableSet.builder();
    objectsThatHaveBeenRemovedBuilder.add(objectToRemove);
    // Remove transitive objects
    if (!objectsToRemove.isEmpty()) {
      for (SMGObject toRemove : objectsToRemove) {
        SMGAndSMGObjects newSMGAndRemoved = currentSMG.copyAndRemoveObjectAndSubSMG(toRemove);
        objectsThatHaveBeenRemovedBuilder.addAll(newSMGAndRemoved.getSMGObjects());
        currentSMG = newSMGAndRemoved.getSMG();
      }
    }

    return SMGAndSMGObjects.of(currentSMG, objectsThatHaveBeenRemovedBuilder.build());
  }

  /**
   * Returns the static null object.
   *
   * @return The null SMGObject.
   */
  public SMGObject getNullObject() {
    return SMGObject.nullInstance();
  }

  /**
   * Returns all SMGObjects associated with this SMG in a set.
   *
   * @return The set of SMGObjects associated with this SMG.
   */
  public Set<SMGObject> getObjects() {
    return smgObjects.keySet();
  }

  /**
   * Returns all SMGValues associated with this SMG and their current nesting levels.
   *
   * @return The set of SMGValues associated with this SMG.
   */
  public Map<SMGValue, Integer> getValues() {
    return smgValuesAndNestingLvl;
  }

  /**
   * Returned the set of SMGHasValueEdges associated with the region that is specified by the
   * entered SMGObject. The region is an interval [object.offset, object.offset + object.size).
   *
   * @param pRegion SMGObject for whos region one wants the SMGHasValueEdges.
   * @return The set of SMGHasValueEdges associated with the region.
   */
  public Set<SMGHasValueEdge> getEdges(SMGObject pRegion) {
    return hasValueEdges.getOrDefault(pRegion, PersistentSet.of());
  }

  /**
   * This is a general method to get a single SMGHasValueEdges by object and a filter predicate.
   * Examples:
   *
   * <p>{@code Predicate<SMGHasValueEdge> filterOffset = o -> o.getOffset().equals(offset);} Returns
   * a possible SMGHasValueEdge with the offset entered.
   *
   * <p>{@code o -> o.getOffset().equals(offset) && o.getSizeInBits().equals(sizeInBits);} Returns a
   * possible SMGHasValueEdge with the offset and size entered.
   *
   * @param object SMGObject for which the SMGHasValueEdge are searched.
   * @param filter The filter predicate for SMGHasValueEdges.
   * @return Either an empty Optional if there is no such SMGHasValueEdge, or an Optional with some
   *     edge for the entered filter.
   */
  public Optional<SMGHasValueEdge> getHasValueEdgeByPredicate(
      SMGObject object, Predicate<SMGHasValueEdge> filter) {
    // There should only ever be 1 edge per offset, and they should not overlap
    return hasValueEdges.getOrDefault(object, PersistentSet.of()).stream().filter(filter).findAny();
  }

  /**
   * This is a general method to get a all SMGHasValueEdges by object and a filter predicate.
   * Examples:
   *
   * <p>{@code Predicate<SMGHasValueEdge> filterOffset = o -> o.getOffset().equals(offset);} Returns
   * all existing SMGHasValueEdge with the offset entered.
   *
   * <p>{@code o -> o.getOffset().equals(offset) && o.getSizeInBits().equals(sizeInBits);} Returns
   * all existing SMGHasValueEdge with the offset and size entered.
   *
   * @param object SMGObject for which the SMGHasValueEdge are searched.
   * @param filter The filter predicate for SMGHasValueEdges.
   * @return A FluentIterable with all edges matching the specified filter
   */
  public FluentIterable<SMGHasValueEdge> getHasValueEdgesByPredicate(
      SMGObject object, Predicate<SMGHasValueEdge> filter) {
    return FluentIterable.from(hasValueEdges.getOrDefault(object, PersistentSet.of()))
        .filter(filter);
  }

  /**
   * Read a value of an object in the field specified by offset and size. This returns a read
   * re-interpretation of the field, which means it returns either the symbolic value that is
   * present, 0 if the field is covered with nullified blocks or an unknown value. This is not
   * guaranteed to be completely accurate! TODO: add description for new features
   *
   * @param object The object from which is to be read.
   * @param offset The offset from which on the field in the object is to be read.
   * @param sizeInBits Size in bits, specifying the size to be read from the offset.
   * @return A updated SMG and the SMTValue that is a read re-interpretation of the field in the
   *     object. May be 0, a symbolic value or a new unknown symbolic value.
   */
  public SMGAndHasValueEdges readValue(
      SMGObject object, BigInteger offset, BigInteger sizeInBits, boolean readMultipleEdges) {
    // let v := H(o, of, t)
    // TODO: Currently getHasValueEdgeByOffsetAndSize returns any edge it finds.
    // Check if multiple edges may exists for the same offset and size! -> There should never be
    // multiple edges for the exact same offset/size
    // TODO: We only check for the exact matches to offset + size, what if one reads
    // a field that is completely covered by a value field? I guess this is meant this way, but we
    // should discuss it nevertheless.
    BigInteger searchOffsetPlusSize = offset.add(sizeInBits);
    ImmutableList.Builder<SMGHasValueEdge> returnEdgeBuilder = ImmutableList.builder();
    PersistentSet<SMGHasValueEdge> edgesForObject = hasValueEdges.get(object);
    if (edgesForObject != null) {
      for (SMGHasValueEdge hve : edgesForObject) {
        if (hve.getOffset().equals(offset) && hve.getSizeInBits().equals(sizeInBits)) {
          // if v != undefined then return (smg, v)
          return SMGAndHasValueEdges.of(this, hve);
        }
        if (readMultipleEdges) {
          BigInteger hveOffset = hve.getOffset();
          BigInteger hveOffsetPlusSize = hve.getSizeInBits().add(hveOffset);
          // Iff we can't find an exact match, we can try to partially read by reading the larger
          // value and extract the value we need from it
          if (hveOffset.compareTo(offset) <= 0 && offset.compareTo(hveOffsetPlusSize) < 0) {
            // The value we search for begins in this edge
            returnEdgeBuilder.add(hve);
            if (hveOffsetPlusSize.compareTo(searchOffsetPlusSize) > 0) {
              // It also ends here
              break;
            }
          } else if (hveOffset.compareTo(searchOffsetPlusSize) < 0
              && searchOffsetPlusSize.compareTo(hveOffsetPlusSize) <= 0) {
            // The value we search for ends in this edge
            returnEdgeBuilder.add(hve);
          } else if (offset.compareTo(hveOffset) < 0
              && hveOffsetPlusSize.compareTo(searchOffsetPlusSize) < 0) {
            // The value we search for covers this edge completely
            returnEdgeBuilder.add(hve);
          }
        }
      }
    }

    List<SMGHasValueEdge> foundReturnEdges = returnEdgeBuilder.build();
    if (!foundReturnEdges.isEmpty()) {
      if (foundReturnEdges.size() > 1) {
        // Sort by offset and merge zero edges
        foundReturnEdges =
            ImmutableList.sortedCopyOf(
                Comparator.comparingInt(x -> x.getOffset().intValueExact()), foundReturnEdges);
        returnEdgeBuilder = ImmutableList.builder();
        SMGHasValueEdge zeroEdgeBuffer = null;
        for (SMGHasValueEdge hve : foundReturnEdges) {
          if (hve.hasValue().isZero()) {
            if (zeroEdgeBuffer != null) {
              zeroEdgeBuffer =
                  new SMGHasValueEdge(
                      hve.hasValue(),
                      zeroEdgeBuffer.getOffset(),
                      zeroEdgeBuffer.getSizeInBits().add(hve.getSizeInBits()));
            } else {
              zeroEdgeBuffer = hve;
            }
          } else {
            if (zeroEdgeBuffer != null) {
              returnEdgeBuilder.add(zeroEdgeBuffer);
              zeroEdgeBuffer = null;
            }
            returnEdgeBuilder.add(hve);
          }
        }
        if (zeroEdgeBuffer != null) {
          returnEdgeBuilder.add(zeroEdgeBuffer);
        }
        foundReturnEdges = returnEdgeBuilder.build();
      }
      return SMGAndHasValueEdges.of(this, foundReturnEdges);
    }

    // if the field to be read is covered by nullified blocks, i.e. if
    // forall . of <= i < of +  size(t) exists . e element H(o, of, t): i element I(e),
    // let v := 0. Otherwise extend V by a fresh value node v.
    Optional<SMGValue> isCoveredBy = isCoveredByNullifiedBlocks(object, offset, sizeInBits);
    if (isCoveredBy.isPresent()) {
      return SMGAndHasValueEdges.of(
          this, new SMGHasValueEdge(SMGValue.zeroValue(), offset, sizeInBits));
    }

    SMGValue newValue = SMGValue.of();
    SMG newSMG = copyAndAddValue(newValue, 0);
    // Extend H by the has-value edge o -> v with the offset and size and return (smg,v) based on
    // the newly obtained SMG.
    SMGHasValueEdge newHVEdge = new SMGHasValueEdge(newValue, offset, sizeInBits);
    newSMG = newSMG.copyAndAddHVEdge(newHVEdge, object);
    return SMGAndHasValueEdges.of(newSMG, newHVEdge);
  }

  /**
   * Returns an SMG with a write reinterpretation of the current SMG. Essentially just writes a
   * value to the given object and field. The reinterpretation removes other values from the field.
   * This is done by either removing a HasValueEdge completely, or reintroducing a new one, covering
   * only the parts outside the field. This method assumes that there are no changes to the values
   * nesting level or that it is 0 for values non-existent in the SMG. In the later case the mapping
   * is added.
   *
   * @param object The object in which the field lies.
   * @param offset The offset (beginning of the field).
   * @param sizeInBits Size in bits of the field.
   * @param value The value to be written into the field.
   * @return A SMG with the value at the specified position.
   */
  public SMG writeValue(
      SMGObject object, BigInteger offset, BigInteger sizeInBits, SMGValue value) {
    // Check that our field is inside the object: offset + sizeInBits <= size(object)
    BigInteger offsetPlusSize = offset.add(sizeInBits);
    if (value.isZero() && isCoveredByNullifiedBlocks(object, offset, sizeInBits).isPresent()) {
      return this;
    }

    // If there exists a hasValueEdge in the specified object, with the specified field that equals
    // the specified value, simply return the original SMG
    Optional<SMGHasValueEdge> hvEdge =
        getHasValueEdgeByPredicate(
            object,
            o ->
                o.getOffset().compareTo(offset) == 0
                    && o.getSizeInBits().compareTo(sizeInBits) == 0);
    if (hvEdge.isPresent() && hvEdge.orElseThrow().hasValue().equals(value)) {
      return this;
    }
    // Add the value to the Values present in this SMG
    Integer nestingLevel = smgValuesAndNestingLvl.get(value);
    SMG newSMG = this;
    // We assume that the nesting level is updated externally and is retained here
    if (nestingLevel == null) {
      newSMG = copyAndAddValue(value, 0);
    }

    // Remove all HasValueEdges from the object with non-zero values overlapping with the given
    // field.
    FluentIterable<SMGHasValueEdge> nonZeroOverlappingEdges =
        newSMG.getHasValueEdgesByPredicate(
            object,
            n ->
                !(n.getOffset().add(n.getSizeInBits()).compareTo(offset) <= 0
                        || offsetPlusSize.compareTo(n.getOffset()) <= 0)
                    && !n.hasValue().isZero());
    newSMG = newSMG.copyAndRemoveHVEdges(nonZeroOverlappingEdges, object);

    if (!value.isZero()) {
      // If the value is non-zero, then for each hasValueEdge with a zero value, remove the edge,
      // and reintroduce new edges not overlapping with the field.
      newSMG = newSMG.cutZeroValueEdgesToField(object, offset, sizeInBits);
    }
    // Add the SMGHasValueEdge leading from the object to the field given with the value given to
    // the new SMG and return it.
    SMGHasValueEdge newHVEdge = new SMGHasValueEdge(value, offset, sizeInBits);
    newSMG = newSMG.copyAndAddHVEdge(newHVEdge, object);
    // assert newSMG.checkValueInConcreteMemorySanity();
    return newSMG;
  }

  /**
   * Removes all zero value HasValueEdges overlapping with the given field [offset; offset + size)
   * and reintroduces new zero value edges for removed edges that exceeded the boundries of the
   * field, but only outside the field. A new SMG is returned with the changes.
   *
   * @param object The object in which the field is located.
   * @param offset Offset in bits.
   * @param sizeInBits Size in bits.
   * @return A new SMG with the overlapping zero edges removed.
   */
  private SMG cutZeroValueEdgesToField(SMGObject object, BigInteger offset, BigInteger sizeInBits) {
    final BigInteger offsetPlusSize = offset.add(sizeInBits);
    PersistentSet<SMGHasValueEdge> toRemoveEdgesSet = PersistentSet.of();
    PersistentSet<SMGHasValueEdge> toAddEdgesSet = PersistentSet.of();

    for (SMGHasValueEdge hvEdge : hasValueEdges.get(object)) {
      final BigInteger hvEdgeOffsetPlusSize = hvEdge.getOffset().add(hvEdge.getSizeInBits());
      // Overlapping zero value edges
      if (hvEdge.hasValue().isZero()
          && hvEdge.getOffset().compareTo(offsetPlusSize) < 0
          && hvEdgeOffsetPlusSize.compareTo(offset) > 0) {
        toRemoveEdgesSet = toRemoveEdgesSet.addAndCopy(hvEdge);

        if (hvEdge.getOffset().compareTo(offset) < 0) {
          final BigInteger newSize = calculateBitPreciseSize(offset, hvEdge.getOffset());
          SMGHasValueEdge newLowerEdge =
              new SMGHasValueEdge(SMGValue.zeroValue(), hvEdge.getOffset(), newSize);
          toAddEdgesSet = toAddEdgesSet.addAndCopy(newLowerEdge);
        }

        if (offsetPlusSize.compareTo(hvEdgeOffsetPlusSize) < 0) {
          final BigInteger newSize = calculateBitPreciseSize(hvEdgeOffsetPlusSize, offsetPlusSize);
          SMGHasValueEdge newUpperEdge =
              new SMGHasValueEdge(SMGValue.zeroValue(), offsetPlusSize, newSize);
          toAddEdgesSet = toAddEdgesSet.addAndCopy(newUpperEdge);
        }
      }
    }

    return copyAndRemoveHVEdges(toRemoveEdgesSet, object).copyAndAddHVEdges(toAddEdgesSet, object);
  }

  /**
   * Calculates bit precise the size of new SMGHasValueEdges. This is needed as sizes used by us are
   * bit precise, and we need bit precision! Bit fields exist! This is only ok for 0 values!
   *
   * @param first The precision in bits that will be subtracted upon.
   * @param second The precision that will be subtracted from first.
   * @return (first - second) bit precise.
   */
  private BigInteger calculateBitPreciseSize(BigInteger first, BigInteger second) {
    return first.subtract(second);
  }

  /**
   * This Method checks for the entered SMGObject if there exists SMGHasValueEdges such that the
   * field [offset; offset + size) is covered by nullObjects. Important: One may not take
   * SMGHasValueEdges into account which lay outside of the SMGObject! Else it would be possible to
   * read potentially invalid memory!
   *
   * @param object The SMGObject in which a field is to be checked for nullified blocks.
   * @param offset The offset (=start) of the field. Has to be inside of the object.
   * @param size The size in bits of the field. Has to be larger than the offset but still inside
   *     the field.
   * @return An optional with the correct zero edge, empty if not covered.
   */
  private Optional<SMGValue> isCoveredByNullifiedBlocks(
      SMGObject object, BigInteger offset, BigInteger size) {
    NavigableMap<BigInteger, BigInteger> nullEdgesRangeMap =
        getZeroValueEdgesForObject(object, offset, size);
    if (nullEdgesRangeMap.isEmpty()) {
      return Optional.empty();
    }
    // We start at the first value equalling zero in the object itself. To not read potentially
    // invalid memory, the first SMGHasValueEdge has to equal the offset, while only a single offset
    // + size in the map(=HasValueEdges) has to equal the offset + size of the field to be read.
    BigInteger currentMax = nullEdgesRangeMap.firstKey();
    // The first edge offset can't cover the entire field if it begins after the obj offset!
    if (currentMax.compareTo(offset) > 0) {
      return Optional.empty();
    }
    BigInteger offsetPlusSize = offset.add(size);
    currentMax = nullEdgesRangeMap.get(currentMax).add(currentMax);
    // TreeMaps keySet is ordered!
    for (Map.Entry<BigInteger, BigInteger> entry : nullEdgesRangeMap.entrySet()) {
      // The max encountered yet has to be bigger to the next key.
      // ( < because the size begins with the offset and does not include the offset + size bit!)
      if (currentMax.compareTo(entry.getKey()) < 0) {
        return Optional.empty();
      }
      currentMax = currentMax.max(entry.getValue().add(entry.getKey()));
      // If there are no gaps,
      // the max encountered has to be == offset + size at some point.
      if (currentMax.compareTo(offsetPlusSize) >= 0) {
        // This value is guaranteed to exists because of the map
        return Optional.of(
            getHasValueEdgeByPredicate(object, hv -> hv.getOffset().compareTo(entry.getKey()) == 0)
                .orElseThrow()
                .hasValue());
      }
    }
    // The max encountered did not cover the entire field.
    return Optional.empty();
  }

  /**
   * Returns the sorted Map<offset, max size> of SMGHasValueEdge of values equaling zero that cover
   * the entered SMGObject somewhere. Only edges that do not exceed the boundries of the range
   * offset to offset + size are used. It always defaults to the max size, such that no smaller size
   * for a offset exists. Example: <0, 16> and <0, 24> would result in <0, 24>.
   *
   * @param smgObject The SMGObject one wants to check for covering NullObjects.
   * @return TreeMap<offset, max size> of covering edges.
   */
  private ImmutableSortedMap<BigInteger, BigInteger> getZeroValueEdgesForObject(
      SMGObject smgObject, BigInteger offset, BigInteger sizeInBits) {
    if (hasValueEdges.get(smgObject) == null) {
      return ImmutableSortedMap.of();
    }
    BigInteger offsetPlusSize = offset.add(sizeInBits);
    // FIT-TR-2013-4 appendix B states that the entered field has to be covered. It does not matter
    // if this is done in a sinle edge, or multiple, or that the edges exceed the field entered.
    // They must be in the objects boundries however.
    return hasValueEdges.get(smgObject).stream()
        .filter(
            n ->
                n.hasValue().isZero()
                    && offsetPlusSize.compareTo(n.getOffset()) >= 0
                    && offset.compareTo(n.getOffset().add(n.getSizeInBits())) <= 0)
        .collect(
            ImmutableSortedMap.toImmutableSortedMap(
                Comparator.naturalOrder(),
                SMGHasValueEdge::getOffset,
                SMGHasValueEdge::getSizeInBits,
                BigInteger::max));
  }

  /**
   * Returns all edges overlapping a defined chunk of memory sorted by the edges' offset.
   *
   * @param pObject - the SMGRegion there the memory chunk is located
   * @param pFieldOffset - the start offset of the memory chunk
   * @param pSizeofInBits - the size of the memory chunk
   * @return all edges with: edgeOffset <= pFieldOffset && pFieldOffset < edgeOffset + edgeSize ||
   *     edgeOffset > pFieldOffset && edgeOffset < pSizeofInBits + pFieldOffset
   */
  public Collection<SMGHasValueEdge> getOverlappingEdges(
      SMGObject pObject, BigInteger pFieldOffset, BigInteger pSizeofInBits) {
    return getHasValueEdgesByPredicate(
            pObject,
            edge ->
                // edgeOffset <= pFieldOffset && pFieldOffset < edgeOffset + edgeSize
                ((edge.getOffset().compareTo(pFieldOffset) <= 0
                        && edge.getOffset().add(edge.getSizeInBits()).compareTo(pFieldOffset) > 0)
                    // edgeOffset > pFieldOffset && edgeOffset < pSizeofInBits + pFieldOffset
                    || (edge.getOffset().compareTo(pFieldOffset) > 0
                        && edge.getOffset().compareTo(pFieldOffset.add(pSizeofInBits)) < 0)))
        .toSortedSet(Comparator.comparing(SMGHasValueEdge::getOffset));
  }

  /**
   * Returns a Set of all SMGDoublyLinkedListSegments of this SMG.
   *
   * @return The Set of all SMGDoublyLinkedListSegments.
   */
  public FluentIterable<SMGDoublyLinkedListSegment> getDLLs() {
    return FluentIterable.from(smgObjects.keySet()).filter(SMGDoublyLinkedListSegment.class);
  }

  /**
   * Returns all SMGHasValueEdges for this SMG in a Set.
   *
   * @return Set of all SMGHasValueEdges of this SMG.
   */
  public FluentIterable<SMGHasValueEdge> getHVEdges() {
    return FluentIterable.concat(hasValueEdges.values());
  }

  /**
   * Returns all SMGPointsToEdges for this SMG in a Collection.
   *
   * @return Collection of all SMGPointsToEdges of this SMG.
   */
  public FluentIterable<SMGPointsToEdge> getPTEdges() {
    return FluentIterable.from(pointsToEdges.values());
  }

  /**
   * Returns all SMGPointsToEdges that points to a specific SMGObject.
   *
   * @param pointingTo the required target
   * @return Collection of all SMGPointsToEdges with the specified target.
   */
  public FluentIterable<SMGPointsToEdge> getPTEdgesByTarget(SMGObject pointingTo) {
    return FluentIterable.from(
        transformedImmutableSetCopy(
            objectsAndPointersPointingAtThem
                .getOrDefault(pointingTo, PathCopyingPersistentTreeMap.of())
                .keySet(),
            v -> pointsToEdges.get(v)));
  }

  /**
   * Returns all SMGValues with SMGPointsToEdges that point to a specific given SMGObject.
   *
   * @param pointingTo the required target
   * @return Collection of all SMGValues with SMGPointsToEdges towards the specified target.
   */
  public Set<SMGValue> getPointerValuesForTarget(SMGObject pointingTo) {
    return objectsAndPointersPointingAtThem
        .getOrDefault(pointingTo, PathCopyingPersistentTreeMap.of())
        .keySet();
  }

  public Map<SMGValue, SMGPointsToEdge> getPTEdgeMapping() {
    return pointsToEdges;
  }

  /**
   * Returns the SMGPointsToEdge associated with the entered SMGValue.
   *
   * @param value The SMGValue for which the edge is to be returned.
   * @return The SMGPointsToEdge for the entered value.
   */
  public Optional<SMGPointsToEdge> getPTEdge(SMGValue value) {
    return Optional.ofNullable(pointsToEdges.get(value));
  }

  /**
   * Returns true if the value is a pointer that points to a 0+ abstracted list segment. Else false.
   *
   * @param value some {@link SMGValue}. Does not have to be a pointer.
   * @return true for 0+ target. false else.
   */
  public boolean pointsToZeroPlus(@Nullable SMGValue value) {
    if (value == null) {
      return false;
    }
    Optional<SMGPointsToEdge> maybePTEdge = getPTEdge(value);
    return maybePTEdge.isPresent()
        && maybePTEdge.orElseThrow().pointsTo() instanceof SMGSinglyLinkedListSegment
        && ((SMGSinglyLinkedListSegment) maybePTEdge.orElseThrow().pointsTo()).getMinLength() == 0;
  }

  /**
   * Returns true if the value is a pointer that points to an abstracted list segment
   *
   * @param value some {@link SMGValue}. Does not have to be a pointer.
   * @return true for SLL or DLL target with non hfo offset. false else.
   */
  public boolean pointsToMaterializableList(@Nullable SMGValue value) {
    if (value == null) {
      return false;
    }
    Optional<SMGPointsToEdge> maybePTEdge = getPTEdge(value);
    return maybePTEdge.isPresent()
        && maybePTEdge.orElseThrow().pointsTo() instanceof SMGSinglyLinkedListSegment;
    /*
     * We always mat for all abstracted objects as the papers idea (see below) does not hold for 0+ cases
     * linkedList
     *   && linkedList.getNextOffset().equals(maybePTEdge.orElseThrow().getOffset()) && (!(maybePTEdge.orElseThrow().pointsTo() instanceof SMGDoublyLinkedListSegment dll) ||
     *   dll.getPrevOffset().equals(maybePTEdge.orElseThrow().getOffset()));
     */
  }

  /**
   * Checks whether a given value is a pointer address.
   *
   * @param pValue to be checked
   * @return true if pValue is a pointer.
   */
  public boolean isPointer(SMGValue pValue) {
    return pointsToEdges.containsKey(pValue);
  }

  /**
   * Checks whether there exists an other edge for a given SMGObject, that overlaps with the
   * provided edge.
   *
   * @param pHValueEdge - the provided edge
   * @param pObject - the given SMGObject
   * @return true if there exists an overlapping edge with the provided edge.
   */
  public boolean hasOverlappingEdge(SMGHasValueEdge pHValueEdge, SMGObject pObject) {
    return getEdges(pObject).stream()
        .anyMatch(
            other -> {
              BigInteger otherStart = other.getOffset();
              BigInteger otherEnd = otherStart.add(other.getSizeInBits());
              BigInteger pStart = pHValueEdge.getOffset();
              BigInteger pEnd = pStart.add(pHValueEdge.getSizeInBits());

              // pStart greater
              if (pStart.compareTo(otherStart) > 0) {
                return pStart.compareTo(otherEnd) < 0;
              }
              // pStart less
              if (pStart.compareTo(otherStart) < 0) {
                return pEnd.compareTo(otherStart) > 0;
              }

              return true;
            });
  }

  /**
   * Finds a pointer address to given pointer attributes.
   *
   * @param targetObject the wanted pointer
   * @param pOffset of the wanted pointer
   * @param pTargetSpecifier of the wanted pointer
   * @return Optional empty, if there is no such pointer or the address of a matching pointer.
   */
  public Optional<SMGValue> findAddressForEdge(
      SMGObject targetObject, BigInteger pOffset, SMGTargetSpecifier pTargetSpecifier) {
    return pointsToEdges.entrySet().stream()
        .filter(
            entry -> {
              SMGPointsToEdge edge = entry.getValue();
              return edge.getOffset().equals(pOffset)
                  && edge.targetSpecifier().equals(pTargetSpecifier)
                  && edge.pointsTo().equals(targetObject);
            })
        .findAny()
        .map(Entry::getKey);
  }

  /*
   * Checks if there are valid heap objects that point to the given target object and might be lists (== size and fitting nfo).
   * This obviously fails for looping lists.
   */
  public boolean hasPotentialListObjectsWithPointersToObject(
      SMGObject targetObject, BigInteger suspectedNfo, Collection<SMGObject> heapObjects) {
    for (Entry<SMGValue, SMGPointsToEdge> entry : pointsToEdges.entrySet()) {
      if (targetObject.equals(entry.getValue().pointsTo())) {
        SMGValue pointerValue = entry.getKey();
        for (SMGObject heapObj : heapObjects) {
          if (!isValid(heapObj)) {
            continue;
          }
          for (SMGHasValueEdge hve : hasValueEdges.getOrDefault(heapObj, PersistentSet.of())) {
            if (hve.hasValue() == pointerValue) {
              if (heapObj.isSizeEqual(targetObject)) {
                if (hve.getOffset().compareTo(suspectedNfo) == 0) {
                  return false;
                }
                // maybePreviousObj -> potentialRoot might be a back pointer, this is fine however
                // as we will eliminate those by traversing along the NFOs
              }
              // TODO: use solver to check for equal size?
            }
          }
        }
      }
    }
    return true;
  }

  /**
   * Checks whether a given SMGObject is valid.
   *
   * @param pObject to be checked
   * @return true if pObject is valid, false if pObject was freed.
   */
  public boolean isValid(SMGObject pObject) {
    return smgObjects.getOrDefault(pObject, false);
  }

  @Override
  public int hashCode() {
    return Objects.hash(hasValueEdges, smgObjects, pointsToEdges, smgValuesAndNestingLvl);
  }

  @Override
  public boolean equals(Object obj) {
    if (this == obj) {
      return true;
    }
    return obj instanceof SMG other
        && Objects.equals(hasValueEdges, other.hasValueEdges)
        && Objects.equals(smgObjects, other.smgObjects)
        && Objects.equals(pointsToEdges, other.pointsToEdges)
        && Objects.equals(smgValuesAndNestingLvl, other.smgValuesAndNestingLvl);
  }

  public BigInteger getSizeOfPointer() {
    return sizeOfPointer;
  }

  public SMGObjectsAndValues collectReachableObjectsAndValues(
      Collection<SMGObject> pVisibleObjects) {
    Set<SMGObject> visitedObjects = new HashSet<>();
    Set<SMGValue> visitedValues = new HashSet<>();
    Deque<SMGObject> workDeque = new ArrayDeque<>(pVisibleObjects);
    while (!workDeque.isEmpty()) {
      SMGObject object = workDeque.pop();
      // valid object which was not yet visited
      if (visitedObjects.add(object) && isValid(object)) {
        for (SMGHasValueEdge outgoingEdge : getEdges(object)) {
          SMGValue value = outgoingEdge.hasValue();
          // pointer value which was not yet visited
          if (visitedValues.add(value) && isPointer(value)) {
            SMGObject pointerTarget = getPTEdge(value).orElseThrow().pointsTo();
            workDeque.add(pointerTarget);
          }
        }
      }
    }

    return new SMGObjectsAndValues(visitedObjects, visitedValues);
  }

  public Set<SMGObject> getTargetsForPointersIn(SMGObject pTarget) {
    ImmutableSet.Builder<SMGObject> results = ImmutableSet.builder();
    PersistentSet<SMGHasValueEdge> hves = hasValueEdges.get(pTarget);
    if (!isValid(pTarget) || hves == null) {
      return ImmutableSet.of();
    }
    for (SMGHasValueEdge hve : hves) {
      SMGPointsToEdge pte = pointsToEdges.get(hve.hasValue());
      if (pte != null) {
        results.add(pte.pointsTo());
      }
    }
    return results.build();
  }

  public Set<SMGObject> getAllSourcesForPointersPointingTowards(SMGObject pTarget) {
    ImmutableSet.Builder<SMGObject> results = ImmutableSet.builder();
    if (!isValid(pTarget)) {
      return ImmutableSet.of();
    }

    for (SMGValue ptrPointingAt :
        objectsAndPointersPointingAtThem
            .getOrDefault(pTarget, PathCopyingPersistentTreeMap.of())
            .keySet()) {
      SMGPointsToEdge pte = pointsToEdges.get(ptrPointingAt);
      if (pte != null && pte.pointsTo().equals(pTarget)) {
        results.addAll(
            valuesToRegionsTheyAreSavedIn
                .getOrDefault(ptrPointingAt, PathCopyingPersistentTreeMap.of())
                .keySet());
      }
    }
    return results.build();
  }

  @SuppressWarnings("Unused")
  public Map<SMGObject, Integer> getAllSourcesForPointersPointingTowardsWithNumOfOccurrences(
      SMGObject pTarget) {
    if (!isValid(pTarget)) {
      return new HashMap<>();
    }
    Map<SMGObject, Integer> results = new HashMap<>();
    PersistentMap<SMGValue, Integer> pointersAndOcc =
        objectsAndPointersPointingAtThem.getOrDefault(pTarget, PathCopyingPersistentTreeMap.of());

    for (SMGValue ptrPointingAt : pointersAndOcc.keySet()) {
      SMGPointsToEdge pte = pointsToEdges.get(ptrPointingAt);
      if (pte != null && pte.pointsTo().equals(pTarget)) {
        for (Entry<SMGObject, Integer> sourceObjsAndOcc :
            valuesToRegionsTheyAreSavedIn
                .getOrDefault(ptrPointingAt, PathCopyingPersistentTreeMap.of())
                .entrySet()) {
          if (results.containsKey(sourceObjsAndOcc.getKey())) {
            results.put(
                sourceObjsAndOcc.getKey(),
                results.get(sourceObjsAndOcc.getKey()) + sourceObjsAndOcc.getValue());
          } else {
            results.put(sourceObjsAndOcc.getKey(), results.get(sourceObjsAndOcc.getKey()));
          }
        }
      }
    }
    return results;
  }

  public Set<SMGValue> getAllPointerValuesPointingTowardsFrom(
      SMGObject pTarget, SMGObject pSource) {
    if (!isValid(pTarget) || !isValid(pSource)) {
      return ImmutableSet.of();
    }
    ImmutableSet.Builder<SMGValue> results = ImmutableSet.builder();
    PersistentMap<SMGValue, Integer> pointersAndOcc =
        objectsAndPointersPointingAtThem.getOrDefault(pTarget, PathCopyingPersistentTreeMap.of());

    for (SMGValue ptrPointingAt : pointersAndOcc.keySet()) {
      for (SMGObject sourceObjsForPTE :
          valuesToRegionsTheyAreSavedIn
              .getOrDefault(ptrPointingAt, PathCopyingPersistentTreeMap.of())
              .keySet()) {
        if (sourceObjsForPTE.equals(pSource)) {
          results.add(ptrPointingAt);
        }
      }
    }
    return results.build();
  }

  /**
   * Used to identify an object <-> value relationship. Can be used to map out the current memory.
   *
   * @return the current SMGObject - HasValueEdge mappings.
   */
  public PersistentMap<SMGObject, PersistentSet<SMGHasValueEdge>>
      getSMGObjectsWithSMGHasValueEdges() {
    return hasValueEdges;
  }

  public SMG copyHVEdgesFromTo(SMGObject source, SMGObject target) {
    PersistentSet<SMGHasValueEdge> setOfValues =
        hasValueEdges.getOrDefault(source, PersistentSet.of());
    // We expect that there are NO edges in the target!
    assert hasValueEdges.getOrDefault(target, PersistentSet.of()).isEmpty();
    SMG newSMG = this;
    for (SMGHasValueEdge hve : setOfValues) {
      newSMG = newSMG.incrementValueToMemoryMapEntry(target, hve.hasValue());
    }
    return newSMG.copyAndSetHVEdges(setOfValues, target);
  }

  // Replace the pointer behind value with a new pointer with the new SMGObject target
  public SMG replaceAllPointersTowardsWith(SMGValue pointerValue, SMGObject newTarget) {
    SMGPointsToEdge oldEdge = getPTEdge(pointerValue).orElseThrow();
    SMG newSMG =
        copyAndSetPTEdges(
            new SMGPointsToEdge(newTarget, oldEdge.getOffset(), oldEdge.targetSpecifier()),
            pointerValue);
    assert newSMG.checkSMGSanity();
    return newSMG;
  }

  /**
   * Search for all pointers towards the object old and replaces them with pointers pointing towards
   * the new object. If the newTarget is a region, specifiers are set to region. All other
   * specifiers are retained.
   *
   * @param oldObj old object.
   * @param newTarget new target object.
   * @return a new SMG with the replacement.
   */
  public SMG replaceAllPointersTowardsWith(SMGObject oldObj, SMGObject newTarget) {
    SMG newSMG = this;
    if (newTarget.isZero() || oldObj.isZero()) {
      throw new AssertionError("Can't replace a 0 value!");
    }

    for (Entry<SMGValue, Integer> pointerValueAndNum :
        objectsAndPointersPointingAtThem
            .getOrDefault(oldObj, PathCopyingPersistentTreeMap.of())
            .entrySet()) {
      // Replace the PTEdge for the value
      SMGValue pointerValue = pointerValueAndNum.getKey();
      SMGPointsToEdge pointsToEdge = pointsToEdges.get(pointerValue);
      assert pointsToEdge.pointsTo().equals(oldObj);

      PersistentMap<SMGObject, Integer> sourcesOfPtr =
          valuesToRegionsTheyAreSavedIn.get(pointerValue);
      SMGTargetSpecifier specifier = pointsToEdge.targetSpecifier();
      if (sourcesOfPtr.size() == 2
          && sourcesOfPtr.containsKey(oldObj)
          && sourcesOfPtr.containsKey(newTarget)) {
        // Only Self-pointer. We assume that we are in a merging case,
        // and it has an equality with the other merging object
        // and that it was already copied to the new object
        Preconditions.checkArgument(newTarget instanceof SMGSinglyLinkedListSegment);
        specifier = SMGTargetSpecifier.IS_ALL_POINTER;
      } else {
        // Assert that there are no self-pointers, as we don't handle this case atm
        // Either there is no self-pointer, or it's a fst or lst (looping list)
        assert !sourcesOfPtr.containsKey(oldObj)
            || specifier.equals(SMGTargetSpecifier.IS_LAST_POINTER)
            || specifier.equals(SMGTargetSpecifier.IS_FIRST_POINTER);
      }

      // If the new target is an abstracted list we switch to all
      if (!(newTarget instanceof SMGSinglyLinkedListSegment)) {
        specifier = SMGTargetSpecifier.IS_REGION;
      }
      newSMG =
          newSMG.copyAndSetPTEdges(
              new SMGPointsToEdge(newTarget, pointsToEdge.getOffset(), specifier), pointerValue);
    }

    assert newSMG.checkSMGSanity();
    return newSMG;
  }

  /**
   * Search for all pointers towards the object old and replaces them with pointers pointing towards
   * the new object. Also decrements the nesting level of all pointers switched by 1. If the
   * newTarget is a region, specifiers are set to region. All other specifiers are retained.
   *
   * @param oldObj old object.
   * @param newTarget new target object.
   * @return a new SMG with the replacement.
   */
  public SMG replaceAllPointersTowardsWithAndDecrementNestingLevel(
      SMGObject oldObj, SMGObject newTarget) {
    SMG newSMG = this;
    if (newTarget.isZero() || oldObj.isZero()) {
      throw new AssertionError("Can't replace a 0 value!");
    }

    for (Entry<SMGValue, Integer> pointerValueAndNum :
        objectsAndPointersPointingAtThem
            .getOrDefault(oldObj, PathCopyingPersistentTreeMap.of())
            .entrySet()) {
      // Replace the PTEdge for the value
      SMGValue pointerValue = pointerValueAndNum.getKey();
      SMGPointsToEdge pointsToEdge = pointsToEdges.get(pointerValue);
      assert pointsToEdge.pointsTo().equals(oldObj);
      SMGTargetSpecifier specifier = pointsToEdge.targetSpecifier();
      // If the new target is an abstracted list we switch to all
      if (!(newTarget instanceof SMGSinglyLinkedListSegment)) {
        specifier = SMGTargetSpecifier.IS_REGION;
      }
      newSMG =
          newSMG.copyAndSetPTEdges(
              new SMGPointsToEdge(newTarget, pointsToEdge.getOffset(), specifier), pointerValue);

      // Update the nesting level
      // The nesting always needs to be >= 0
      int newNestingLvl = Integer.max(smgValuesAndNestingLvl.get(pointerValue) - 1, 0);

      newSMG =
          newSMG.copyWithNewValuesAndNestingLvl(
              newSMG.smgValuesAndNestingLvl.putAndCopy(pointerValue, newNestingLvl));
    }

    assert newSMG.checkSMGSanity();
    return newSMG;
  }

  /**
   * ONLY modifies pointersTowardsObjectsMap and swaps all pointers towards oldObj towards
   * newTarget. Self-pointers of oldObj to themselves are not switched and need to be deleted
   * separately.
   *
   * @param oldObj all pointers whose target is to be moved to newTarget
   * @param newTarget the new target of the pointers
   * @return an SMG with the targets switched
   */
  private SMG replaceTargetOfPointersMap(SMGObject oldObj, SMGObject newTarget) {
    PersistentMap<SMGValue, Integer> newTargetPtrValues =
        objectsAndPointersPointingAtThem.getOrDefault(newTarget, PathCopyingPersistentTreeMap.of());
    PersistentMap<SMGObject, PersistentMap<SMGValue, Integer>> newPointersTowardsObjectsMap;
    PersistentMap<SMGValue, Integer> oldTargetPtrValues =
        objectsAndPointersPointingAtThem.getOrDefault(oldObj, PathCopyingPersistentTreeMap.of());
    if (newTargetPtrValues.isEmpty()) {
      newPointersTowardsObjectsMap =
          objectsAndPointersPointingAtThem
              .putAndCopy(newTarget, oldTargetPtrValues)
              .removeAndCopy(oldObj);
    } else {
      for (Entry<SMGValue, Integer> ptrValueOld : oldTargetPtrValues.entrySet()) {
        SMGPointsToEdge maybePTEOrNull = pointsToEdges.get(ptrValueOld.getKey());
        if (maybePTEOrNull != null && maybePTEOrNull.pointsTo().equals(oldObj)) {
          // Selfpointer, does not need switching
          continue;
        }
        newTargetPtrValues =
            newTargetPtrValues.putAndCopy(
                ptrValueOld.getKey(),
                newTargetPtrValues.getOrDefault(ptrValueOld.getKey(), 0) + ptrValueOld.getValue());
      }
      newPointersTowardsObjectsMap =
          objectsAndPointersPointingAtThem
              .putAndCopy(newTarget, newTargetPtrValues)
              .removeAndCopy(oldObj);
    }

    return new SMG(
        smgObjects,
        smgValuesAndNestingLvl,
        hasValueEdges,
        valuesToRegionsTheyAreSavedIn,
        pointsToEdges,
        newPointersTowardsObjectsMap,
        sizeOfPointer);
  }

  /**
   * Search for all pointers towards the oldObj and switch them to newTarget. Then increments the
   * nesting level of the values of the changed pointers by 1. We expect that the newTarget does not
   * have any pointers towards it. Sets the specifiers for pointers so that if oldObj is not
   * abstracted, it's a first, all others become all. Self-pointers of oldObj to themselves are not
   * switched and need to be deleted separately.
   *
   * @param oldObj old object.
   * @param newTarget new target object.
   * @return a new SMG with the replacement.
   */
  public SMG replaceAllPointersTowardsWithAndIncrementNestingLevel(
      SMGObject oldObj, SMGObject newTarget, int incrementAmount) {

    int minListLen = 0;
    if (newTarget instanceof SMGSinglyLinkedListSegment) {
      minListLen = ((SMGSinglyLinkedListSegment) newTarget).getMinLength();
    }
    SMG newSMG = this;
    if (newTarget.isZero() || oldObj.isZero()) {
      throw new AssertionError("Can't replace a 0 value!");
    }

    PersistentMap<SMGValue, Integer> pointersTowardsOldObj =
        objectsAndPointersPointingAtThem.getOrDefault(oldObj, PathCopyingPersistentTreeMap.of());
    for (Entry<SMGValue, Integer> pointerValueAndOcc : pointersTowardsOldObj.entrySet()) {
      SMGValue pointerValue = pointerValueAndOcc.getKey();
      // Switch points-to-edges to new target
      SMGPointsToEdge oldPTEdge = pointsToEdges.get(pointerValue);

      PersistentMap<SMGObject, Integer> sources =
          valuesToRegionsTheyAreSavedIn.getOrDefault(
              pointerValue, PathCopyingPersistentTreeMap.of());
      if (sources.size() == 1 && sources.containsKey(oldObj)) {
        // Self-pointer, does not need switching
        continue;
      }
      // Assert that there is no selfedge with outside sources as we can't handle that atm
      assert !sources.containsKey(oldObj);

      assert oldPTEdge.pointsTo().equals(oldObj);
      SMGTargetSpecifier specifier = oldPTEdge.targetSpecifier();
      if (!(oldObj instanceof SMGSinglyLinkedListSegment)) {
        specifier = SMGTargetSpecifier.IS_FIRST_POINTER;
      } else if (!specifier.equals(SMGTargetSpecifier.IS_FIRST_POINTER)) {
        specifier = SMGTargetSpecifier.IS_ALL_POINTER;
      }
      newSMG =
          newSMG.copyAndSetPTEdges(
              new SMGPointsToEdge(newTarget, oldPTEdge.getOffset(), specifier), pointerValue);

      // Update the nesting level
      int newNestingLvl = smgValuesAndNestingLvl.get(pointerValue) + incrementAmount;
      // If there is an abstracted list, the min length is always larger than the nesting level.
      // The pointer to the first element is minListLen - 1.
      // The nesting always needs to be >= 0
      Preconditions.checkArgument(
          (minListLen > newNestingLvl || (newNestingLvl == 0 && minListLen == 0))
              && newNestingLvl >= 0);
      newSMG =
          newSMG.copyWithNewValuesAndNestingLvl(
              newSMG.smgValuesAndNestingLvl.putAndCopy(pointerValue, newNestingLvl));
    }

    // Switch pointer targets
    newSMG = newSMG.replaceTargetOfPointersMap(oldObj, newTarget);
    return newSMG;
  }

  /**
   * Search for all pointers towards the object old and replaces them with pointers pointing towards
   * the new object only if their nesting level is equal to the given. Then switches the nesting
   * level of the switched to 0.
   *
   * @param oldObj old object.
   * @param newTarget new target object.
   * @param levelToReplace the nesting level we want to replace with 0.
   * @param specifierToSwitch specifier that will be switched. All others remain to point towards
   *     oldObj.
   * @return a new SMG with the replacement.
   */
  public SMG replaceSpecificPointersTowardsWithAndSetNestingLevelZero(
      SMGObject oldObj,
      SMGObject newTarget,
      int levelToReplace,
      Set<SMGTargetSpecifier> specifierToSwitch) {
    return replaceSpecificPointersTowardsWithAndSetNestingLevel(
        oldObj, newTarget, levelToReplace, 0, specifierToSwitch);
  }

  /**
   * Search for all pointers towards the object old and replaces them with pointers pointing towards
   * the new object only if their nesting level is equal to the given. Then switches the nesting
   * level of the switched to the value given.
   *
   * @param oldObj old object.
   * @param newTarget new target object.
   * @param levelToReplace the nesting level we want to replace with the given.
   * @param newLevel new nesting level
   * @param specifierToSwitch specifier that will be switched. All others remain to point towards
   *     oldObj.
   * @return a new SMG with the replacement.
   */
  public SMG replaceSpecificPointersTowardsWithAndSetNestingLevel(
      SMGObject oldObj,
      SMGObject newTarget,
      int levelToReplace,
      int newLevel,
      Set<SMGTargetSpecifier> specifierToSwitch) {

    SMG newSMG = this;
    if (newTarget.isZero() || oldObj.isZero()) {
      throw new AssertionError("Can't replace a 0 value!");
    }

    PersistentMap<SMGValue, Integer> pointersTowardsOldObj =
        objectsAndPointersPointingAtThem.getOrDefault(oldObj, PathCopyingPersistentTreeMap.of());
    // The values to change the nesting level are the values from this pointer set
    for (Entry<SMGValue, Integer> pointerValueAndOcc : pointersTowardsOldObj.entrySet()) {
      SMGValue pointerValue = pointerValueAndOcc.getKey();
      SMGPointsToEdge pointsToEdge = newSMG.pointsToEdges.get(pointerValue);
      assert pointsToEdge.pointsTo().equals(oldObj);
      // Since we decrement the nesting level afterward, we check for 1 instead of 0
      int currentNestingLevel = newSMG.smgValuesAndNestingLvl.get(pointerValue);
      SMGTargetSpecifier pteSpecifier = pointsToEdge.targetSpecifier();
      if (currentNestingLevel == levelToReplace && specifierToSwitch.contains(pteSpecifier)) {
        SMGTargetSpecifier targetSpec = pointsToEdge.targetSpecifier();
        if (!(newTarget instanceof SMGSinglyLinkedListSegment)) {
          targetSpec = SMGTargetSpecifier.IS_REGION;
        }
        SMGPointsToEdge newPTEdge =
            new SMGPointsToEdge(newTarget, pointsToEdge.getOffset(), targetSpec);
        // Update the points-to-edges to new targets, this also includes the reverse map
        newSMG = newSMG.copyAndSetPTEdges(newPTEdge, pointerValue);

        // Update nesting level
        newSMG =
            newSMG.copyWithNewValuesAndNestingLvl(
                newSMG.smgValuesAndNestingLvl.putAndCopy(pointerValue, newLevel));
      }
    }

    return newSMG;
  }

  // Needed for tests only
  public SMG replaceSMGValueNestingLevel(SMGValue value, int newNestingLevel) {
    Preconditions.checkArgument(newNestingLevel >= 0);
    // We only change the nesting level, all values are the same
    return copyAndAddValue(value, newNestingLevel);
  }

  /*
   * Get the object pointing towards a 0+ list segment.
   */
  public List<SMGObject> getObjectsPointingToZeroPlusAbstraction(
      SMGSinglyLinkedListSegment zeroPlusObj) {
    ImmutableList.Builder<SMGObject> builder = ImmutableList.builder();
    PersistentMap<SMGValue, Integer> objectsWPointersTowardsZeroPlus =
        objectsAndPointersPointingAtThem.get(zeroPlusObj);
    if (objectsWPointersTowardsZeroPlus == null) {
      return ImmutableList.of();
    }
    for (Entry<SMGValue, Integer> pointerTowards : objectsWPointersTowardsZeroPlus.entrySet()) {

      PersistentMap<SMGObject, Integer> objects =
          valuesToRegionsTheyAreSavedIn.get(pointerTowards.getKey());
      // Note: there might be multiple pointers towards the 0+
      // for example a first from previous list materialization, or a last for the end of the list
      builder.addAll(objects.keySet());
    }
    ImmutableList<SMGObject> objectsWithPointersToward = builder.build();
    if (objectsWithPointersToward.isEmpty()) {
      throw new AssertionError("Critical error: could not find pointers towards 0+ in the SMG.");
    }
    return objectsWithPointersToward;
  }

  /**
   * Checks the SMG internally for consistency. For example that the reverse maps are correct, that
   * there are no 2 pointers pointing to the exact same location in memory.
   *
   * @return false if anything is violated.
   */
  public boolean checkSMGSanity() {
    return checkCorrectObjectsToPointersMapSanity()
        && checkValueInConcreteMemorySanity()
        && verifyPointsToEdgeSanity();
  }

  // Only every use this after all operations are done. I.e. at the beginning and end of abstracting
  // a list segment for example.
  private boolean checkCorrectObjectsToPointersMapSanity() {
    for (Entry<SMGObject, PersistentMap<SMGValue, Integer>> realTargetAndPointers :
        objectsAndPointersPointingAtThem.entrySet()) {
      SMGObject target = realTargetAndPointers.getKey();
      if (target.isZero() || !isValid(target)) {
        continue;
      }
      PersistentMap<SMGValue, Integer> realPointersAndOcc = realTargetAndPointers.getValue();
      // now check the smg for this obj
      Map<SMGValue, Integer> pointersTowardsTarget = new HashMap<>();
      for (PersistentSet<SMGHasValueEdge> hves : hasValueEdges.values()) {
        for (SMGHasValueEdge hve : hves) {
          SMGValue value = hve.hasValue();
          if (pointsToEdges.containsKey(value)
              && pointsToEdges.get(value).pointsTo().equals(target)) {
            if (pointersTowardsTarget.containsKey(value)) {
              pointersTowardsTarget.replace(
                  value, pointersTowardsTarget.getOrDefault(value, 0) + 1);
            } else {
              pointersTowardsTarget.put(value, pointersTowardsTarget.getOrDefault(value, 0) + 1);
            }
          }
        }
      }
      if (pointersTowardsTarget.size() != realPointersAndOcc.size()) {
        return false;
      } else {
        for (Entry<SMGValue, Integer> realEntry : realPointersAndOcc.entrySet()) {
          SMGValue realValue = realEntry.getKey();
          int realNumOfOcc = realEntry.getValue();
          if (!pointersTowardsTarget.containsKey(realValue)) {
            return false;
          } else {
            int smgOcc = pointersTowardsTarget.get(realValue); // reference map created here
            if (smgOcc != realNumOfOcc) {
              return false;
            }
          }
        }
      }
    }
    return true;
  }

  // Returns false if the values saved in the memory object does not match the reverse map
  // valuesToRegionsTheyAreSavedIn
  private boolean checkValueInConcreteMemorySanity() {
    Map<SMGValue, Map<SMGObject, Integer>> mapBuiltFromCurrentValues = new HashMap<>();
    for (Entry<SMGObject, PersistentSet<SMGHasValueEdge>> entry : hasValueEdges.entrySet()) {
      SMGObject source = entry.getKey();
      PersistentSet<SMGHasValueEdge> values = entry.getValue();
      for (SMGHasValueEdge hve : values) {
        SMGValue value = hve.hasValue();
        Map<SMGObject, Integer> innerMap =
            mapBuiltFromCurrentValues.getOrDefault(value, new HashMap<>());
        Integer currentNum = innerMap.getOrDefault(source, 0);
        innerMap.remove(source);
        innerMap.put(source, currentNum + 1);
        mapBuiltFromCurrentValues.remove(value);
        mapBuiltFromCurrentValues.put(value, innerMap);
      }
    }
    if (valuesToRegionsTheyAreSavedIn.size() != mapBuiltFromCurrentValues.size()) {
      return false;
    }
    for (Entry<SMGValue, PersistentMap<SMGObject, Integer>> entry :
        valuesToRegionsTheyAreSavedIn.entrySet()) {
      SMGValue valueToCheck = entry.getKey();
      PersistentMap<SMGObject, Integer> innerMapToCheck = entry.getValue();
      if (!mapBuiltFromCurrentValues.containsKey(valueToCheck)) {
        return false;
      }
      Map<SMGObject, Integer> referenceInnerMap = mapBuiltFromCurrentValues.get(valueToCheck);
      if (referenceInnerMap.size() != innerMapToCheck.size()) {
        return false;
      }
      for (Entry<SMGObject, Integer> innerEntry : innerMapToCheck.entrySet()) {
        SMGObject object = innerEntry.getKey();
        if (!referenceInnerMap.containsKey(object)) {
          return false;
        } else if (!referenceInnerMap.get(object).equals(innerEntry.getValue())) {
          return false;
        }
      }
    }
    return true;
  }

  /**
   * Only every use this after all operations are done. I.e. at the beginning and end of abstracting
   * a list segment for example.
   *
   * @return false if there is more than 1 value with the exact same points-to-edge pointing to the
   *     same object/offset with the same nesting level.
   */
  private boolean verifyPointsToEdgeSanity() {
    for (Entry<SMGValue, SMGPointsToEdge> pointsToEntry1 : pointsToEdges.entrySet()) {
      for (Entry<SMGValue, SMGPointsToEdge> pointsToEntry2 : pointsToEdges.entrySet()) {
        if (pointsToEntry1.getValue().equals(pointsToEntry2.getValue())
            && !pointsToEntry1.getKey().equals(pointsToEntry2.getKey())
            && !pointsToEntry1.getKey().isZero()) {
          int nestingLevel1 = smgValuesAndNestingLvl.get(pointsToEntry1.getKey());
          int nestingLevel2 = smgValuesAndNestingLvl.get(pointsToEntry2.getKey());
          SMGTargetSpecifier tspec1 = pointsToEntry1.getValue().targetSpecifier();
          SMGTargetSpecifier tspec2 = pointsToEntry2.getValue().targetSpecifier();
          if (nestingLevel1 == nestingLevel2 && !tspec1.equals(tspec2)) {
            // Both ptEdges have the same target, but different values, are none zero, and have the
            // same nesting level
            return false;
          }
        }
      }
    }
    return true;
  }

  @VisibleForTesting
  public PersistentMap<SMGValue, PersistentMap<SMGObject, Integer>>
      getValuesToRegionsTheyAreSavedIn() {
    return valuesToRegionsTheyAreSavedIn;
  }

  /**
   * Returns the nesting level for existing SMGValues. Does crash for non-existent SMGValues.
   *
   * @param pSMGValue the {@link SMGValue}
   * @return the nesting level.
   */
  public int getNestingLevel(SMGValue pSMGValue) {
    return smgValuesAndNestingLvl.get(pSMGValue);
  }

  public SMG copyAndSetTargetSpecifierForPointer(
      SMGValue pValue, SMGTargetSpecifier pSpecifierToSet) {
    Preconditions.checkArgument(isPointer(pValue));
    if (pointsToEdges.get(pValue).targetSpecifier().equals(pSpecifierToSet)) {
      return this;
    }

    ImmutableMap.Builder<SMGValue, SMGPointsToEdge> newPTEs = ImmutableMap.builder();
    for (Entry<SMGValue, SMGPointsToEdge> ptrValuesAndPTE : pointsToEdges.entrySet()) {
      SMGValue currentValue = ptrValuesAndPTE.getKey();
      if (currentValue.equals(pValue)) {
        newPTEs.put(
            currentValue, ptrValuesAndPTE.getValue().copyAndSetTargetSpecifier(pSpecifierToSet));
      } else {
        newPTEs.put(ptrValuesAndPTE);
      }
    }
    return new SMG(
        smgObjects,
        smgValuesAndNestingLvl,
        hasValueEdges,
        valuesToRegionsTheyAreSavedIn,
        newPTEs.buildOrThrow(),
        objectsAndPointersPointingAtThem,
        sizeOfPointer);
  }

  public SMG copyAndSetTargetSpecifierForPtrsTowards(
      SMGObject pTarget, int nestingLevelToChange, SMGTargetSpecifier pSpecifierToSet) {
    return copyAndSetTargetSpecifierForPtrsTowards(
        pTarget,
        nestingLevelToChange,
        pSpecifierToSet,
        ImmutableSet.of(
            SMGTargetSpecifier.IS_FIRST_POINTER,
            SMGTargetSpecifier.IS_LAST_POINTER,
            SMGTargetSpecifier.IS_REGION,
            SMGTargetSpecifier.IS_ALL_POINTER));
  }

  public SMG copyAndSetTargetSpecifierForPtrsTowards(
      SMGObject pTarget,
      int nestingLevelToChange,
      SMGTargetSpecifier pSpecifierToSet,
      Set<SMGTargetSpecifier> specifierToSwitch) {
    Preconditions.checkArgument(pTarget instanceof SMGSinglyLinkedListSegment);
    Set<SMGValue> pointersTowardsTarget =
        objectsAndPointersPointingAtThem
            .getOrDefault(pTarget, PathCopyingPersistentTreeMap.of())
            .keySet()
            .stream()
            .filter(v -> getNestingLevel(v) == nestingLevelToChange)
            .collect(ImmutableSet.toImmutableSet());
    ImmutableMap.Builder<SMGValue, SMGPointsToEdge> newPTEs = ImmutableMap.builder();
    // We assume that there is only 1 pointer (value) in the set above
    if (pointersTowardsTarget.isEmpty()) {
      return this;
    }
    ImmutableSet.Builder<SMGValue> pointersTowardsTargetNeedSwitchingBuilder =
        ImmutableSet.builder();
    // Precheck if there is anything to do at all
    for (SMGValue ptrValue : pointersTowardsTarget) {
      SMGPointsToEdge pte = pointsToEdges.get(ptrValue);
      if (!pte.targetSpecifier().equals(pSpecifierToSet)
          && specifierToSwitch.contains(pte.targetSpecifier())) {
        pointersTowardsTargetNeedSwitchingBuilder =
            pointersTowardsTargetNeedSwitchingBuilder.add(ptrValue);
      }
    }
    Set<SMGValue> pointersTowardsTargetNeedSwitching =
        pointersTowardsTargetNeedSwitchingBuilder.build();
    if (pointersTowardsTargetNeedSwitching.isEmpty()) {
      return this;
    }
    // TODO: check if a pointer already exists and switch all values of the pointers to truly switch
    // to the other value

    for (Entry<SMGValue, SMGPointsToEdge> ptrValuesAndPTE : pointsToEdges.entrySet()) {
      SMGValue currentValue = ptrValuesAndPTE.getKey();
      // pointersTowardsTargetNeedSwitching contains only the SMGValues that need switching
      if (pointersTowardsTargetNeedSwitching.contains(currentValue)) {
        newPTEs.put(
            currentValue, ptrValuesAndPTE.getValue().copyAndSetTargetSpecifier(pSpecifierToSet));
      } else {
        newPTEs.put(ptrValuesAndPTE);
      }
    }
    return new SMG(
        smgObjects,
        smgValuesAndNestingLvl,
        hasValueEdges,
        valuesToRegionsTheyAreSavedIn,
        newPTEs.buildOrThrow(),
        objectsAndPointersPointingAtThem,
        sizeOfPointer);
  }

  public Optional<SMGValue> getAddressValueForPointsToTargetWithNestingLevel(
      SMGObject pTarget, BigInteger pOffset, int pNestingLevel) {
    for (SMGValue ptr :
        objectsAndPointersPointingAtThem
            .getOrDefault(pTarget, PathCopyingPersistentTreeMap.of())
            .keySet()) {
      // All existing pointers towards the target
      if (getNestingLevel(ptr) == pNestingLevel
          && pointsToEdges.get(ptr).getOffset().equals(pOffset)) {
        return Optional.of(ptr);
      }
    }
    return Optional.empty();
  }

  public Optional<SMGValue> getAddressValueForPointsToTargetWithNestingLevel(
      SMGObject pTarget,
      BigInteger pOffset,
      int pNestingLevel,
      SMGTargetSpecifier specifierToSearchFor,
      Set<SMGTargetSpecifier> specifierAllowedToOverride) {
    if (!specifierAllowedToOverride.contains(specifierToSearchFor)) {
      specifierAllowedToOverride =
          ImmutableSet.<SMGTargetSpecifier>builder()
              .addAll(specifierAllowedToOverride)
              .add(specifierToSearchFor)
              .build();
    }
    for (SMGValue ptr :
        objectsAndPointersPointingAtThem
            .getOrDefault(pTarget, PathCopyingPersistentTreeMap.of())
            .keySet()) {
      // All existing pointers towards the target
      SMGPointsToEdge pte = pointsToEdges.get(ptr);
      if (getNestingLevel(ptr) == pNestingLevel
          && pte.getOffset().equals(pOffset)
          && specifierAllowedToOverride.contains(pte.targetSpecifier())) {
        return Optional.of(ptr);
      }
    }
    return Optional.empty();
  }

  public SMG replacePointersWithSMGValue(
      SMGObject pOldTargetObj,
      SMGValue pReplacementValue,
      int pNestingLevelToSwitch,
      Set<SMGTargetSpecifier> pSpecifierToSwitch) {

    // First pointers need to be switched so that they have level = min length - 1 and not 0!
    if (isPointer(pReplacementValue) && !pReplacementValue.isZero()) {
      SMGObject newTarget = pointsToEdges.get(pReplacementValue).pointsTo();

      if (pSpecifierToSwitch.contains(SMGTargetSpecifier.IS_FIRST_POINTER)
          && newTarget instanceof SMGSinglyLinkedListSegment ll) {
        Set<SMGTargetSpecifier> specWoFirst =
            pSpecifierToSwitch.stream()
                .filter(spec -> !spec.equals(SMGTargetSpecifier.IS_FIRST_POINTER))
                .collect(ImmutableSet.toImmutableSet());

        SMG newSMG =
            replaceSpecificPointersTowardsWithAndSetNestingLevel(
                pOldTargetObj,
                newTarget,
                pNestingLevelToSwitch,
                Integer.max(ll.getMinLength() - 1, 0),
                ImmutableSet.of(SMGTargetSpecifier.IS_FIRST_POINTER));

        if (specWoFirst.isEmpty()) {
          return newSMG;
        }

        return newSMG.replaceSpecificPointersTowardsWithAndSetNestingLevelZero(
            pOldTargetObj, newTarget, pNestingLevelToSwitch, specWoFirst);
      }

      return replaceSpecificPointersTowardsWithAndSetNestingLevelZero(
          pOldTargetObj,
          pointsToEdges.get(pReplacementValue).pointsTo(),
          pNestingLevelToSwitch,
          pSpecifierToSwitch);
    }
    SMG newSMG = this;
    Set<SMGValue> pointersTowardsOld =
        objectsAndPointersPointingAtThem
            .getOrDefault(pOldTargetObj, PathCopyingPersistentTreeMap.of())
            .keySet()
            .stream()
            .filter(
                v ->
                    pSpecifierToSwitch.contains(pointsToEdges.get(v).targetSpecifier())
                        && getNestingLevel(v) == pNestingLevelToSwitch)
            .collect(ImmutableSet.toImmutableSet());
    Set<SMGObject> sourcesOfPointersTowardsOld = ImmutableSet.of();
    for (SMGValue ptrToRemove : pointersTowardsOld) {
      sourcesOfPointersTowardsOld =
          ImmutableSet.<SMGObject>builder()
              .addAll(
                  valuesToRegionsTheyAreSavedIn
                      .getOrDefault(ptrToRemove, PathCopyingPersistentTreeMap.of())
                      .keySet())
              .addAll(sourcesOfPointersTowardsOld)
              .build();
    }

    // Replace HVEs (automatically increments reverse maps)
    for (SMGObject valueSource : sourcesOfPointersTowardsOld) {
      for (SMGHasValueEdge oldHve : hasValueEdges.get(valueSource)) {
        if (pointersTowardsOld.contains(oldHve.hasValue())) {
          SMGHasValueEdge newHVE =
              new SMGHasValueEdge(pReplacementValue, oldHve.getOffset(), oldHve.getSizeInBits());
          newSMG = newSMG.copyAndReplaceHVEdge(valueSource, oldHve, newHVE);
        }
      }
    }

    return newSMG;
  }

  public boolean checkFirstPointerNestingLevelConsistency() {
    for (Entry<SMGObject, PersistentMap<SMGValue, Integer>> entry :
        objectsAndPointersPointingAtThem.entrySet()) {
      if (entry.getKey() instanceof SMGSinglyLinkedListSegment sll) {
        for (SMGValue ptr : entry.getValue().keySet()) {
          SMGPointsToEdge pte = pointsToEdges.get(ptr);
          int ptrNestingLevel = getNestingLevel(ptr);
          if (pte.targetSpecifier().equals(SMGTargetSpecifier.IS_FIRST_POINTER)
              && ptrNestingLevel != Integer.max(0, sll.getMinLength() - 1)) {
            return false;
          }
        }
      }
    }
    return true;
  }

  public boolean checkNotAbstractedNestingLevelConsistency() {
    for (Entry<SMGObject, PersistentMap<SMGValue, Integer>> entry :
        objectsAndPointersPointingAtThem.entrySet()) {
      if (!(entry.getKey() instanceof SMGSinglyLinkedListSegment)) {
        for (SMGValue ptr : entry.getValue().keySet()) {
          if (getNestingLevel(ptr) != 0) {
            return false;
          }
        }
      }
    }
    return true;
  }
}
