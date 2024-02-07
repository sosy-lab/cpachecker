// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2021 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.util.smg;

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
import org.sosy_lab.cpachecker.cpa.smg.util.PersistentSet;
import org.sosy_lab.cpachecker.cpa.smg2.util.SMGAndSMGObjects;
import org.sosy_lab.cpachecker.cpa.smg2.util.SMGObjectsAndValues;
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
  private final PersistentSet<SMGValue> smgValues;
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
    PersistentSet<SMGValue> newSMGValues = PersistentSet.of(SMGValue.zeroValue());
    newSMGValues = newSMGValues.addAndCopy(SMGValue.zeroFloatValue());
    smgValues = newSMGValues.addAndCopy(SMGValue.zeroDoubleValue());
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
      PersistentSet<SMGValue> pSmgValues,
      PersistentMap<SMGObject, PersistentSet<SMGHasValueEdge>> pHasValueEdges,
      PersistentMap<SMGValue, PersistentMap<SMGObject, Integer>> pValuesToRegionsTheyAreSavedIn,
      ImmutableMap<SMGValue, SMGPointsToEdge> pPointsToEdges,
      PersistentMap<SMGObject, PersistentMap<SMGValue, Integer>> pObjectsAndPointersPointingAtThem,
      BigInteger pSizeOfPointer) {
    smgObjects = pSmgObjects;
    smgValues = pSmgValues;
    hasValueEdges = pHasValueEdges;
    valuesToRegionsTheyAreSavedIn = pValuesToRegionsTheyAreSavedIn;
    pointsToEdges = pPointsToEdges;
    sizeOfPointer = pSizeOfPointer;
    objectsAndPointersPointingAtThem = pObjectsAndPointersPointingAtThem;
  }

  private SMG of(ImmutableMap<SMGValue, SMGPointsToEdge> pPointsToEdges) {
    return new SMG(
        smgObjects,
        smgValues,
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
        smgValues,
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
        smgValues,
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
        smgValues,
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
        smgValues,
        hasValueEdges,
        valuesToRegionsTheyAreSavedIn,
        pointsToEdges,
        objectsAndPointersPointingAtThem,
        sizeOfPointer);
  }

  /**
   * Creates a copy of the SMG an adds the given value.
   *
   * @param pValue - The object to be added.
   * @return A modified copy of the SMG.
   */
  public SMG copyAndAddValue(SMGValue pValue) {
    return new SMG(
        smgObjects,
        smgValues.addAndCopy(pValue),
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
        smgValues.removeAndCopy(pValue),
        hasValueEdges,
        valuesToRegionsTheyAreSavedIn,
        pointsToEdges,
        objectsAndPointersPointingAtThem,
        sizeOfPointer);
  }

  /**
   * Removes the {@link SMGPointsToEdge} associated with the entered {@link SMGValue} iff there is a
   * {@link SMGPointsToEdge}, else does nothing. Caution when using this, should only every be used
   * to remove values/PTEdges that are no longer used after this!
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
          newSMG.smgValues,
          newSMG.hasValueEdges,
          newSMG.valuesToRegionsTheyAreSavedIn,
          builder.buildOrThrow(),
          newSMG.objectsAndPointersPointingAtThem,
          newSMG.sizeOfPointer);
    }
    return new SMG(
        smgObjects,
        smgValues,
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
    assert verifyPointsToEdgeSanity();
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
    assert newSMG.verifyPointsToEdgeSanity();
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
        smgValues,
        hasValueEdges.putAndCopy(source, edges),
        valuesToRegionsTheyAreSavedIn,
        pointsToEdges,
        objectsAndPointersPointingAtThem,
        sizeOfPointer);
  }

  /**
   * Creates a copy of the SMG an adds/replaces the old edges with the given has value edges.
   *
   * @param edges - the edges to be added/replaced.
   * @param source - the source object.
   * @return a modified copy of the SMG.
   */
  /*
  public SMG copyAndSetHVEdges(PersistentSet<SMGHasValueEdge> edges, SMGObject source, PersistentMap<SMGValue) {
    return new SMG(
        smgObjects,
        smgValues,
        hasValueEdges.putAndCopy(source, edges), valuesToRegionsTheyAreSavedIn,
        pointsToEdges, objectsAndPointersPointingAtThem,
        sizeOfPointer);
  }*/

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
        smgValues,
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
          // Check if the pte is actually used and increment PointerToObjectMap only for pointers
          // existing in memory
          for (int i = 0;
              i
                  < valuesToRegionsTheyAreSavedIn
                      .getOrDefault(newSource, PathCopyingPersistentTreeMap.of())
                      .size();
              i++) {
            newSMG =
                newSMG.decrementPointerToObjectMap(
                    newSource, pointsToEdges.get(newSource).pointsTo());
            newSMG = newSMG.incrementPointerToObjectMap(newSource, edge.pointsTo());
          }
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
   * decrements to 0. (This includes the pointer mapping)
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
      // remove the entry
      PersistentMap<SMGObject, Integer> newInnerMap = oldInnerMapOldV.removeAndCopy(pSmgObject);
      if (newInnerMap.isEmpty()) {
        newValuesToRegionsTheyAreSavedIn = valuesToRegionsTheyAreSavedIn.removeAndCopy(pOldValue);
      } else {
        newValuesToRegionsTheyAreSavedIn =
            valuesToRegionsTheyAreSavedIn.putAndCopy(pOldValue, newInnerMap);
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
              .putAndCopy(
                  target, innerMap.removeAndCopy(pointer).putAndCopy(pointer, currentNum - 1));
    } else {
      newObjectsAndPointersPointingAtThem =
          objectsAndPointersPointingAtThem
              .removeAndCopy(target)
              .putAndCopy(target, innerMap.removeAndCopy(pointer));
    }
    return new SMG(
        smgObjects,
        smgValues,
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
        smgValues,
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
   * towards the old obj to the new.
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
   * @return a new SMG with the object invalidated and all its HVEdges deleted.
   */
  public SMG copyAndInvalidateObject(SMGObject pObject) {
    PersistentMap<SMGObject, Boolean> newObjects = smgObjects.putAndCopy(pObject, false);
    SMG newSMG = decrementHVEdgesInValueToMemoryMap(pObject);
    PersistentMap<SMGObject, PersistentSet<SMGHasValueEdge>> newHVEdges =
        hasValueEdges.removeAndCopy(pObject);

    // TODO: actually delete old objects with no references to them (for example the dirt from
    // SLL/DLL creation/deletion)
    return newSMG.of(newObjects, newHVEdges);
  }

  private SMG decrementHVEdgesInValueToMemoryMap(SMGObject pObject) {
    SMG newValuesToRegionsTheyAreSavedIn = this;
    for (SMGHasValueEdge hve : hasValueEdges.getOrDefault(pObject, PersistentSet.of())) {
      newValuesToRegionsTheyAreSavedIn =
          newValuesToRegionsTheyAreSavedIn.decrementValueToMemoryMapEntry(pObject, hve.hasValue());
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
        smgValues,
        hasValueEdges,
        valuesToRegionsTheyAreSavedIn,
        pointsToEdges,
        objectsAndPointersPointingAtThem,
        sizeOfPointer);
  }

  public SMG copyAndRemoveObjects(Collection<SMGObject> pUnreachableObjects) {
    SMG returnSmg = this;
    for (SMGObject smgObject : pUnreachableObjects) {
      returnSmg = returnSmg.copyAndInvalidateObject(smgObject);
    }
    // assert returnSmg.checkValueInConcreteMemorySanity();
    return returnSmg;
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

    // Remove object from the SMG and remove all values inside from the SMG
    SMG currentSMG =
        new SMG(
            smgObjects.removeAndCopy(objectToRemove),
            smgValues,
            hasValueEdges.removeAndCopy(objectToRemove),
            valuesToRegionsTheyAreSavedIn,
            newPointers.buildOrThrow(),
            objectsAndPointersPointingAtThem,
            sizeOfPointer);
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
   * Returns all SMGValues associated with this SMG in a set.
   *
   * @return The set of SMGValues associated with this SMG.
   */
  public Set<SMGValue> getValues() {
    return smgValues;
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
      SMGObject object, BigInteger offset, BigInteger sizeInBits, boolean preciseRead) {
    // Check that our field is inside the object: offset + sizeInBits <= size(object)
    Preconditions.checkArgument(offset.add(sizeInBits).compareTo(object.getSize()) <= 0);

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
        if (preciseRead) {
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
            foundReturnEdges.stream()
                .sorted(Comparator.comparingInt(x -> x.getOffset().intValueExact()))
                .collect(ImmutableList.toImmutableList());
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
    int nestingLevel = object.getNestingLevel();
    SMGValue newValue = SMGValue.of(nestingLevel);
    SMG newSMG = copyAndAddValue(newValue);
    // Extend H by the has-value edge o -> v with the offset and size and return (smg,v) based on
    // the newly obtained SMG.
    SMGHasValueEdge newHVEdge = new SMGHasValueEdge(newValue, offset, sizeInBits);
    newSMG = newSMG.copyAndAddHVEdge(newHVEdge, object);
    return SMGAndHasValueEdges.of(newSMG, newHVEdge);
  }

  /**
   * Returns a SMG with a write reinterpretation of the current SMG. Essentially just writes a value
   * to the given object and field. The reinterpretation removes other values from the field. This
   * is done by either removing a HasValueEdge completely, or reintroducing a new one, covering only
   * the parts outside of the field.
   *
   * @param object The object in which the field lies.
   * @param offset The offset (beginning of the field).
   * @param sizeInBits Size in bits of the field.
   * @param value The value to be written into the field.
   * @return A SMG with the value at the specified position.
   */
  public SMG writeValue(
      SMGObject object, BigInteger offset, BigInteger sizeInBits, SMGValue value) {
    // assert checkValueInConcreteMemorySanity();
    // Check that our field is inside the object: offset + sizeInBits <= size(object)
    BigInteger offsetPlusSize = offset.add(sizeInBits);
    Preconditions.checkArgument(offsetPlusSize.compareTo(object.getSize()) <= 0);
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
    SMG newSMG = copyAndAddValue(value);
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
    return getPTEdges().filter(ptEdge -> ptEdge.pointsTo().equals(pointingTo));
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
              if (heapObj.getSize().compareTo(targetObject.getSize()) == 0) {
                if (hve.getOffset().compareTo(suspectedNfo) == 0) {
                  return false;
                }
                // maybePreviousObj -> potentialRoot might be a back pointer, this is fine however
                // as we
                // will eliminate those by traversing along the NFOs
              }
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
    return Objects.hash(hasValueEdges, smgObjects, pointsToEdges, smgValues);
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
        && Objects.equals(smgValues, other.smgValues);
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

  public Set<SMGObject> getAllSourcesForPointersPointingTowards(SMGObject pTarget) {
    ImmutableSet.Builder<SMGObject> results = ImmutableSet.builder();
    if (!isValid(pTarget)) {
      return ImmutableSet.of();
    }
    for (Entry<SMGObject, PersistentSet<SMGHasValueEdge>> objHVEs : hasValueEdges.entrySet()) {
      for (SMGHasValueEdge hve : objHVEs.getValue()) {
        SMGPointsToEdge pte = pointsToEdges.get(hve.hasValue());
        if (pte != null && pte.pointsTo().equals(pTarget)) {
          results.add(objHVEs.getKey());
        }
      }
    }
    return results.build();
  }

  /**
   * Used to identify a object <-> value relationship. Can be used to map out the current memory.
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
    assert verifyPointsToEdgeSanity();
    SMG newSMG =
        copyAndSetPTEdges(
            new SMGPointsToEdge(newTarget, oldEdge.getOffset(), oldEdge.targetSpecifier()),
            pointerValue);
    assert newSMG.verifyPointsToEdgeSanity();
    return newSMG;
  }

  /**
   * Search for all pointers towards the object old and replaces them with pointers pointing towards
   * the new object.
   *
   * @param oldObj old object.
   * @param newTarget new target object.
   * @return a new SMG with the replacement.
   */
  public SMG replaceAllPointersTowardsWith(SMGObject oldObj, SMGObject newTarget) {
    assert verifyPointsToEdgeSanity();
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
      newSMG =
          newSMG.copyAndSetPTEdges(
              new SMGPointsToEdge(
                  newTarget, pointsToEdge.getOffset(), pointsToEdge.targetSpecifier()),
              pointerValue);
    }

    assert newSMG.verifyPointsToEdgeSanity();
    return newSMG;
  }

  /**
   * ONLY modifies pointersTowardsObjectsMap and swaps all pointers towards oldObj towards
   * newTarget.
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
        smgValues,
        hasValueEdges,
        valuesToRegionsTheyAreSavedIn,
        pointsToEdges,
        newPointersTowardsObjectsMap,
        sizeOfPointer);
  }

  /**
   * Search for all pointers towards the oldObj and switch them to newTarget. Then increments the
   * nesting level of the values of the changed pointers by 1. We expect that the newTarget does not
   * have any pointers towards it.
   *
   * @param oldObj old object.
   * @param newTarget new target object.
   * @return a new SMG with the replacement.
   */
  public SMG replaceAllPointersTowardsWithAndIncrementNestingLevel(
      SMGObject oldObj, SMGObject newTarget, int incrementAmount) {
    // assert checkCorrectObjectsToPointersMap();

    int min = 0;
    if (newTarget instanceof SMGSinglyLinkedListSegment) {
      min = ((SMGSinglyLinkedListSegment) newTarget).getMinLength();
    }
    SMG newSMG = this;
    if (newTarget.isZero() || oldObj.isZero()) {
      throw new AssertionError("Can't replace a 0 value!");
    }

    PersistentMap<SMGValue, Integer> pointersTowardsOldObj =
        objectsAndPointersPointingAtThem.getOrDefault(oldObj, PathCopyingPersistentTreeMap.of());
    for (Entry<SMGValue, Integer> pointerValueAndOcc : pointersTowardsOldObj.entrySet()) {
      SMGValue pointerValue = pointerValueAndOcc.getKey();
      SMGPointsToEdge oldPTEdge = pointsToEdges.get(pointerValue);
      assert oldPTEdge.pointsTo().equals(oldObj);
      // Since we decrement the nesting level afterward, we check for 1 instead of 0
      // The equals for values checks only the ID not the nesting level!!!
      newSMG =
          newSMG.copyAndSetPTEdges(
              new SMGPointsToEdge(newTarget, oldPTEdge.getOffset(), oldPTEdge.targetSpecifier()),
              pointerValue.withNestingLevelAndCopy(
                  pointerValue.getNestingLevel() + incrementAmount));

      if (min <= pointerValue.getNestingLevel() + incrementAmount) {
        Preconditions.checkArgument(min > pointerValue.getNestingLevel() + incrementAmount);
      }
      // The values to decrement the nesting level are the pointer values for the object
    }
    PersistentMap<SMGObject, PersistentSet<SMGHasValueEdge>> newHasValueEdges = hasValueEdges;
    // Update the nesting level
    // TODO: check if contains in PersistentSet is O(1)
    // TODO: use valuesToRegionsTheyAreSavedIn
    Preconditions.checkArgument(incrementAmount >= 0);
    for (Entry<SMGObject, PersistentSet<SMGHasValueEdge>> objToHvEntry : hasValueEdges.entrySet()) {
      SMGObject currentObject = objToHvEntry.getKey();
      boolean contains = false;
      PersistentSet<SMGHasValueEdge> hvEdges = objToHvEntry.getValue();
      for (SMGHasValueEdge hvEdge : objToHvEntry.getValue()) {
        if (pointersTowardsOldObj.containsKey(hvEdge.hasValue())) {
          contains = true;
          hvEdges = hvEdges.removeAndCopy(hvEdge);
          hvEdges =
              hvEdges.addAndCopy(
                  new SMGHasValueEdge(
                      hvEdge
                          .hasValue()
                          .withNestingLevelAndCopy(
                              hvEdge.hasValue().getNestingLevel() + incrementAmount),
                      hvEdge.getOffset(),
                      hvEdge.getSizeInBits()));
        }
      }
      if (contains) {
        // Save to copy the entire entry
        newHasValueEdges = newHasValueEdges.removeAndCopy(currentObject);
        newHasValueEdges = newHasValueEdges.putAndCopy(currentObject, hvEdges);
      }
    }

    newSMG = newSMG.replaceTargetOfPointersMap(oldObj, newTarget);
    newSMG = newSMG.replaceHasValueEdgesAndCopy(newHasValueEdges);

    // assert newSMG.verifyPointsToEdgeSanity();
    return newSMG;
  }

  // Only to be used for cases in which HasValueEdges are changed, but none removed/added.
  private SMG replaceHasValueEdgesAndCopy(
      PersistentMap<SMGObject, PersistentSet<SMGHasValueEdge>> pNewHasValueEdges) {
    return new SMG(
        smgObjects,
        smgValues,
        pNewHasValueEdges,
        valuesToRegionsTheyAreSavedIn,
        pointsToEdges,
        objectsAndPointersPointingAtThem,
        sizeOfPointer);
  }

  /**
   * Search for all pointers towards the object old and replaces them with pointers pointing towards
   * the new object only if their nesting level is equal to the given. Then switches the nesting
   * level of the switched to 0.
   *
   * @param oldObj old object.
   * @param newTarget new target object.
   * @return a new SMG with the replacement.
   */
  public SMG replaceSpecificPointersTowardsWithAndSetNestingLevelZero(
      SMGObject oldObj, SMGObject newTarget, int replacementLevel) {
    assert verifyPointsToEdgeSanity();

    SMG newSMG = this;
    if (newTarget.isZero() || oldObj.isZero()) {
      throw new AssertionError("Can't replace a 0 value!");
    }

    PersistentMap<SMGValue, Integer> pointersTowardsOldObj =
        objectsAndPointersPointingAtThem.getOrDefault(oldObj, PathCopyingPersistentTreeMap.of());
    // The values to change the nesting level are the values from this pointer set
    for (Entry<SMGValue, Integer> pointerValueAndOcc : pointersTowardsOldObj.entrySet()) {
      SMGValue pointerValue = pointerValueAndOcc.getKey();
      SMGPointsToEdge pointsToEdge = pointsToEdges.get(pointerValue);
      assert pointsToEdge.pointsTo().equals(oldObj);
      // Since we decrement the nesting level afterward, we check for 1 instead of 0
      if (pointerValue.getNestingLevel() == replacementLevel) {
        // The equals for values checks only the ID not the nesting level!!!
        newSMG =
            newSMG.copyAndSetPTEdges(
                new SMGPointsToEdge(
                    newTarget, pointsToEdge.getOffset(), pointsToEdge.targetSpecifier()),
                pointerValue.withNestingLevelAndCopy(0));
      }
    }
    // TODO: is contains on PersistentSet O(1)?
    PersistentMap<SMGObject, PersistentSet<SMGHasValueEdge>> newHasValueEdges =
        newSMG.hasValueEdges;
    // Update the nesting level
    // TODO: use valuesToRegionsTheyAreSavedIn
    for (Entry<SMGObject, PersistentSet<SMGHasValueEdge>> objToHvEntry :
        newSMG.hasValueEdges.entrySet()) {
      SMGObject currentObject = objToHvEntry.getKey();
      boolean contains = false;
      PersistentSet<SMGHasValueEdge> hvEdges = objToHvEntry.getValue();
      for (SMGHasValueEdge hvEdge : objToHvEntry.getValue()) {
        if (pointersTowardsOldObj.containsKey(hvEdge.hasValue())) {
          contains = true;
          hvEdges = hvEdges.removeAndCopy(hvEdge);
          hvEdges =
              hvEdges.addAndCopy(
                  new SMGHasValueEdge(
                      hvEdge.hasValue().withNestingLevelAndCopy(0),
                      hvEdge.getOffset(),
                      hvEdge.getSizeInBits()));

          // newSMG = newSMG.replaceTargetOfPointersMap(oldObj, newTarget, hvEdge.hasValue());
        }
      }
      if (contains) {
        // Save to copy the entire entry
        newHasValueEdges = newHasValueEdges.removeAndCopy(currentObject);
        newHasValueEdges = newHasValueEdges.putAndCopy(currentObject, hvEdges);
      }
    }

    newSMG = newSMG.replaceHasValueEdgesAndCopy(newHasValueEdges);
    assert newSMG.verifyPointsToEdgeSanity();
    return newSMG;
  }

  // Needed for tests only
  public SMG replaceSMGValueNestingLevel(SMGValue value, int replacementLevel) {
    SMG newSMG = this;
    assert verifyPointsToEdgeSanity();
    Preconditions.checkArgument(replacementLevel >= 0);
    SMGPointsToEdge originalPTE = pointsToEdges.get(value);
    // Since we decrement the nesting level afterward, we check for 1 instead of 0
    // The equals for values checks only the ID not the nesting level!!!
    newSMG =
        newSMG.copyAndSetPTEdges(
            new SMGPointsToEdge(
                originalPTE.pointsTo(), originalPTE.getOffset(), originalPTE.targetSpecifier()),
            value.withNestingLevelAndCopy(replacementLevel));
    // Switch the nesting level in the objects and pointer pointing at them map

    assert newSMG.verifyPointsToEdgeSanity();

    PersistentMap<SMGObject, PersistentSet<SMGHasValueEdge>> newHasValueEdges = hasValueEdges;
    // Update the nesting level
    // TODO: use valuesToRegionsTheyAreSavedIn
    for (Entry<SMGObject, PersistentSet<SMGHasValueEdge>> objToHvEntry : hasValueEdges.entrySet()) {
      SMGObject currentObject = objToHvEntry.getKey();
      boolean contains = false;
      PersistentSet<SMGHasValueEdge> hvEdges = objToHvEntry.getValue();
      for (SMGHasValueEdge hvEdge : objToHvEntry.getValue()) {
        if (value.equals(hvEdge.hasValue())) {
          contains = true;
          hvEdges = hvEdges.removeAndCopy(hvEdge);
          hvEdges =
              hvEdges.addAndCopy(
                  new SMGHasValueEdge(
                      hvEdge.hasValue().withNestingLevelAndCopy(replacementLevel),
                      hvEdge.getOffset(),
                      hvEdge.getSizeInBits()));
        }
      }
      if (contains) {
        // Save to copy the entire entry
        newHasValueEdges = newHasValueEdges.removeAndCopy(currentObject);
        newHasValueEdges = newHasValueEdges.putAndCopy(currentObject, hvEdges);
      }
    }
    // We only change the nesting level, and all values are the same
    return newSMG.replaceHasValueEdgesAndCopy(newHasValueEdges);
  }

  /*
   * Get the object pointing towards a 0+ list segment.
   * We can assume that there is only 1 of those objects.
   */
  public SMGObject getPreviousObjectOfZeroPlusAbstraction(SMGValue ptObject) {
    // TODO: use reverse maps!
    for (Entry<SMGObject, Boolean> entry : smgObjects.entrySet()) {
      if (entry.getValue()) {
        PersistentSet<SMGHasValueEdge> hvEdgesPerObj = hasValueEdges.get(entry.getKey());
        if (hvEdgesPerObj != null) {
          for (SMGHasValueEdge value : hvEdgesPerObj) {
            if (value.hasValue().equals(ptObject)) {
              return entry.getKey();
            }
          }
        }
      }
    }
    throw new AssertionError("Critical error: could not find origin of points-to-edge in the SMG.");
  }

  // Only every use this after all operations are done. I.e. at the beginning and end of abstracting
  // a list segment for example.
  public boolean checkCorrectObjectsToPointersMap() {
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
        for (Entry<SMGValue, Integer> e1 : realPointersAndOcc.entrySet()) {
          if (!pointersTowardsTarget.containsKey(e1.getKey())
              || !pointersTowardsTarget.get(e1.getKey()).equals(e1.getValue())) {
            return false;
          }
        }
      }
    }
    return true;
  }

  // Returns false if the values saved in the memory object does not match the reverse map
  // valuesToRegionsTheyAreSavedIn
  public boolean checkValueInConcreteMemorySanity() {
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
  public boolean verifyPointsToEdgeSanity() {
    for (Entry<SMGValue, SMGPointsToEdge> pointsToEntry2 : pointsToEdges.entrySet()) {
      for (Entry<SMGValue, SMGPointsToEdge> pointsToEntry3 : pointsToEdges.entrySet()) {
        if (pointsToEntry2.getValue().equals(pointsToEntry3.getValue())
            && !pointsToEntry2.getKey().equals(pointsToEntry3.getKey())
            && !pointsToEntry2.getKey().isZero()
            && pointsToEntry2.getKey().getNestingLevel()
                == pointsToEntry3.getKey().getNestingLevel()) {
          return false;
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
}
