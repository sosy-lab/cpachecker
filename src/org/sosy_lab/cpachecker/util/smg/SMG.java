// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2021 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.util.smg;

import com.google.common.base.Preconditions;
import com.google.common.base.Predicate;
import com.google.common.collect.FluentIterable;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.ImmutableSortedMap;
import java.math.BigInteger;
import java.util.ArrayDeque;
import java.util.Collection;
import java.util.Comparator;
import java.util.Deque;
import java.util.HashSet;
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
import org.sosy_lab.cpachecker.util.smg.util.SMGandValue;

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

  // The bool is the validity of the SMGObject, not being in the map -> not valid (false)
  private final PersistentMap<SMGObject, Boolean> smgObjects;
  private final PersistentSet<SMGValue> smgValues;
  private final PersistentMap<SMGObject, PersistentSet<SMGHasValueEdge>> hasValueEdges;
  private final ImmutableMap<SMGValue, SMGPointsToEdge> pointsToEdges;
  private final BigInteger sizeOfPointer;

  /** Creates a new, empty SMG */
  public SMG(BigInteger pSizeOfPointer) {
    hasValueEdges = PathCopyingPersistentTreeMap.of();
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
      ImmutableMap<SMGValue, SMGPointsToEdge> pPointsToEdges,
      BigInteger pSizeOfPointer) {
    smgObjects = pSmgObjects;
    smgValues = pSmgValues;
    hasValueEdges = pHasValueEdges;
    pointsToEdges = pPointsToEdges;
    sizeOfPointer = pSizeOfPointer;
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
        pointsToEdges,
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
        smgObjects, smgValues.addAndCopy(pValue), hasValueEdges, pointsToEdges, sizeOfPointer);
  }

  /**
   * Creates a copy of the SMG an remove the given value.
   *
   * @param pValue - The object to be added.
   * @return A modified copy of the SMG.
   */
  public SMG copyAndRemoveValue(SMGValue pValue) {
    return new SMG(
        smgObjects, smgValues.removeAndCopy(pValue), hasValueEdges, pointsToEdges, sizeOfPointer);
  }

  public SMG copyAndRemoveValues(Collection<SMGValue> pUnreachableValues) {
    SMG returnSmg = this;
    for (SMGValue smgValue : pUnreachableValues) {
      returnSmg = returnSmg.copyAndRemoveValue(smgValue);
    }
    return returnSmg;
  }

  public SMG copyAndReplaceValueForHVEdges(SMGValue oldSMGValue, SMGValue newSMGValue) {
    PersistentMap<SMGObject, PersistentSet<SMGHasValueEdge>> newHasValueEdges =
        PathCopyingPersistentTreeMap.of();
    for (Entry<SMGObject, PersistentSet<SMGHasValueEdge>> entry : hasValueEdges.entrySet()) {
      boolean contains = false;
      for (SMGHasValueEdge hvEdge : entry.getValue()) {
        if (hvEdge.hasValue().equals(oldSMGValue)) {
          contains = true;
          PersistentSet<SMGHasValueEdge> newSet =
              entry
                  .getValue()
                  .removeAndCopy(hvEdge)
                  .addAndCopy(
                      new SMGHasValueEdge(newSMGValue, hvEdge.getOffset(), hvEdge.getSizeInBits()));
          newHasValueEdges = newHasValueEdges.putAndCopy(entry.getKey(), newSet);
        }
      }
      if (!contains) {
        // Save to copy the entire entry
        newHasValueEdges = newHasValueEdges.putAndCopy(entry.getKey(), entry.getValue());
      }
    }

    return new SMG(smgObjects, smgValues, newHasValueEdges, pointsToEdges, sizeOfPointer);
  }

  /**
   * Creates a copy of the SMG an adds the given has value edge.
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
    return new SMG(
        smgObjects,
        smgValues,
        hasValueEdges.putAndCopy(source, edges),
        pointsToEdges,
        sizeOfPointer);
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
    SMG newSMG =
        new SMG(
            smgObjects,
            smgValues,
            hasValueEdges,
            pointsToEdgesBuilder.buildOrThrow(),
            sizeOfPointer);
    assert verifyPointsToEdgeSanity();
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
        pointsToEdges,
        sizeOfPointer);
  }

  public SMG copyAndAddHVEdges(Iterable<SMGHasValueEdge> edges, SMGObject source) {
    PersistentSet<SMGHasValueEdge> smgEdges = hasValueEdges.get(source);
    for (SMGHasValueEdge edgeToAdd : edges) {
      smgEdges = smgEdges.addAndCopy(edgeToAdd);
    }

    return new SMG(
        smgObjects,
        smgValues,
        hasValueEdges.putAndCopy(source, smgEdges),
        pointsToEdges,
        sizeOfPointer);
  }

  /**
   * Creates a copy of the SMG an removes the given has value edges.
   *
   * @param edges - The edges to be removed.
   * @param source - The source object.
   * @return A modified copy of the SMG.
   */
  public SMG copyAndRemoveHVEdges(Iterable<SMGHasValueEdge> edges, SMGObject source) {
    PersistentSet<SMGHasValueEdge> smgEdges =
        hasValueEdges.getOrDefault(source, PersistentSet.of());
    for (SMGHasValueEdge edgeToRemove : edges) {
      smgEdges = smgEdges.removeAndCopy(edgeToRemove);
    }

    return new SMG(
        smgObjects,
        smgValues,
        hasValueEdges.putAndCopy(source, smgEdges),
        pointsToEdges,
        sizeOfPointer);
  }

  /**
   * @param objectToReplace the object whos edges are supposed to be changed.
   * @param newHVEdges the new HVedges.
   * @return a new SMG with the HVEdges replaced by the given.
   */
  public SMG copyAndReplaceHVEdgesAt(
      SMGObject objectToReplace, PersistentSet<SMGHasValueEdge> newHVEdges) {
    return new SMG(
        smgObjects,
        smgValues,
        hasValueEdges.removeAndCopy(objectToReplace).putAndCopy(objectToReplace, newHVEdges),
        pointsToEdges,
        sizeOfPointer);
  }

  /**
   * Creates a copy of the SMG and adds/replaces the given points to edge.
   *
   * @param edge - The edge to be added/replaced.
   * @param source - The source value.
   * @return A modified copy of the SMG.
   */
  public SMG copyAndSetPTEdges(SMGPointsToEdge edge, SMGValue source) {
    ImmutableMap.Builder<SMGValue, SMGPointsToEdge> pointsToEdgesBuilder = ImmutableMap.builder();
    if (pointsToEdges.containsKey(source)) {
      for (Entry<SMGValue, SMGPointsToEdge> entry : pointsToEdges.entrySet()) {
        if (!entry.getKey().equals(source)) {
          pointsToEdgesBuilder.put(entry);
        } else {
          pointsToEdgesBuilder.put(source, edge);
        }
      }
    } else {
      pointsToEdgesBuilder.putAll(pointsToEdges);
      pointsToEdgesBuilder.put(source, edge);
    }
    return new SMG(
        smgObjects, smgValues, hasValueEdges, pointsToEdgesBuilder.buildOrThrow(), sizeOfPointer);
  }

  /**
   * Creates a copy of the SMG and replaces a given edge with another.
   *
   * @param pSmgObject The source SMGObject.
   * @param pOldEdge Edge to be replaced.
   * @param pNewEdge Replacement edge.
   * @return A copy of the graph with the replaced edge.
   */
  public SMG copyAndReplaceHVEdge(
      SMGObject pSmgObject, SMGHasValueEdge pOldEdge, SMGHasValueEdge pNewEdge) {
    PersistentSet<SMGHasValueEdge> objEdges =
        hasValueEdges.get(pSmgObject).removeAndCopy(pOldEdge).addAndCopy(pNewEdge);
    return copyAndSetHVEdges(objEdges, pSmgObject);
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
    PersistentSet<SMGHasValueEdge> objEdges = hasValueEdges.get(pSmgObject);
    for (SMGHasValueEdge oldEdge : currentEdges) {
      objEdges = objEdges.removeAndCopy(oldEdge).addAndCopy(pNewEdge);
    }
    return copyAndSetHVEdges(objEdges, pSmgObject);
  }

  /**
   * Creates a copy of the SMG and replaces given object by a given new.
   *
   * @param pOldObject - The object to be replaced.
   * @param pNewObject - The replacement object.
   * @return A modified copy.
   */
  public SMG copyAndReplaceObject(SMGObject pOldObject, SMGObject pNewObject) {
    PersistentSet<SMGHasValueEdge> edges = hasValueEdges.get(pOldObject);
    // replace has value edges
    PersistentMap<SMGObject, PersistentSet<SMGHasValueEdge>> newHVEdges =
        hasValueEdges.removeAndCopy(pOldObject).putAndCopy(pNewObject, edges);
    // replace points to edges
    ImmutableMap.Builder<SMGValue, SMGPointsToEdge> pointsToEdgesBuilder = ImmutableMap.builder();
    pointsToEdgesBuilder.putAll(pointsToEdges);

    for (Map.Entry<SMGValue, SMGPointsToEdge> oldEntry : pointsToEdges.entrySet()) {
      if (pOldObject.equals(oldEntry.getValue().pointsTo())) {
        SMGPointsToEdge newEdge =
            new SMGPointsToEdge(
                pNewObject, oldEntry.getValue().getOffset(), oldEntry.getValue().targetSpecifier());
        pointsToEdgesBuilder.put(oldEntry.getKey(), newEdge);
      }
    }

    // replace object
    PersistentMap<SMGObject, Boolean> newObjects =
        smgObjects.removeAndCopy(pOldObject).putAndCopy(pNewObject, true);

    return new SMG(
        newObjects, smgValues, newHVEdges, pointsToEdgesBuilder.buildOrThrow(), sizeOfPointer);
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
    PersistentMap<SMGObject, PersistentSet<SMGHasValueEdge>> newHVEdges =
        hasValueEdges.removeAndCopy(pObject);
    return new SMG(newObjects, smgValues, newHVEdges, pointsToEdges, sizeOfPointer);
  }

  public SMG copyAndRemoveObjects(Collection<SMGObject> pUnreachableObjects) {
    SMG returnSmg = this;
    for (SMGObject smgObject : pUnreachableObjects) {
      returnSmg = returnSmg.copyAndInvalidateObject(smgObject);
    }
    return returnSmg;
  }

  /*
   * Imagine a list a -> b -> c
   * This removes the object b and sets the pointers from a -> b to a -> c
   * and all others towards b to a.
   * Also prunes all unneeded values etc.
   * This replaces the pointer association instead of values. Thats faster.
   */
  public SMG copyAndRemoveDLLObjectAndReplacePointers(
      SMGObject object,
      SMGValue valueForPointerToWardsThis,
      SMGPointsToEdge pointerToNext,
      SMGPointsToEdge pointerToPrevious) {
    ImmutableMap.Builder<SMGValue, SMGPointsToEdge> newPointers = ImmutableMap.builder();
    // HashSet<SMGValue> toRemove = new HashSet<>();
    PersistentMap<SMGObject, PersistentSet<SMGHasValueEdge>> newHVEdges = hasValueEdges;
    for (Entry<SMGValue, SMGPointsToEdge> pointsToEdgesAndValues : pointsToEdges.entrySet()) {
      SMGPointsToEdge pointer = pointsToEdgesAndValues.getValue();
      SMGValue value = pointsToEdgesAndValues.getKey();
      // All pointers towards this object are changed to the previous object except the one pointer
      // that goes to the next segment
      if (pointer.pointsTo().equals(object) && !value.equals(valueForPointerToWardsThis)) {
        if (pointerToPrevious.pointsTo().isZero()) {
          // Change the actual value
          newHVEdges = replaceValueByZero(value, newHVEdges);

        } else {
          newPointers.put(value, pointerToPrevious);
        }
      } else if (pointer.pointsTo().equals(object) && value.equals(valueForPointerToWardsThis)) {
        if (pointerToNext.pointsTo().isZero()) {
          // change the actual value
          newHVEdges = replaceValueByZero(value, newHVEdges);
        } else {
          newPointers.put(value, pointerToNext);
        }
        // toRemove.add(value);
        // } else if (pointer.equals(pointerToNext) || pointer.equals(pointerToPrevious)) {
        // Remember the values for the pointers located in this object
        //  newPointers.put(value, pointer);
      } else {
        newPointers.put(value, pointer);
      }
    }

    return new SMG(
        smgObjects.removeAndCopy(object),
        smgValues,
        newHVEdges.removeAndCopy(object),
        newPointers.buildOrThrow(),
        sizeOfPointer);
  }

  private PersistentMap<SMGObject, PersistentSet<SMGHasValueEdge>> replaceValueByZero(
      SMGValue old, PersistentMap<SMGObject, PersistentSet<SMGHasValueEdge>> newHVEdges) {
    PersistentMap<SMGObject, PersistentSet<SMGHasValueEdge>> newHVE = newHVEdges;
    for (Entry<SMGObject, Boolean> entry : smgObjects.entrySet()) {
      if (entry.getValue()) {
        for (SMGHasValueEdge hve : newHVE.get(entry.getKey())) {
          if (hve.hasValue().equals(old)) {
            // Replace
            PersistentSet<SMGHasValueEdge> newhves =
                newHVE
                    .get(entry.getKey())
                    .removeAndCopy(hve)
                    .addAndCopy(
                        new SMGHasValueEdge(
                            SMGValue.zeroValue(), hve.getOffset(), hve.getSizeInBits()));
            newHVE = newHVE.removeAndCopy(entry.getKey()).putAndCopy(entry.getKey(), newhves);
          }
        }
      }
    }
    return newHVE;
  }

  /*
   * Imagine a list a -> b -> c
   * This removes the object b and sets the pointers from a -> b to a -> c
   * and all others towards b to a.
   * Also prunes all unneeded values etc.
   * pointerToNext is the next pointer of the list segment(=object) deleted.
   * This replaces the pointer association instead of values. Thats faster.
   */
  public SMG copyAndRemoveSLLObjectAndReplacePointers(
      SMGObject object,
      SMGValue valueForPointerToWardsThis,
      SMGPointsToEdge pointerToNext,
      @Nullable SMGObject prevObj) {
    ImmutableMap.Builder<SMGValue, SMGPointsToEdge> newPointers = ImmutableMap.builder();
    // HashSet<SMGValue> toRemove = new HashSet<>();
    PersistentMap<SMGObject, PersistentSet<SMGHasValueEdge>> newHVEdges = hasValueEdges;
    if (prevObj == null) {
      // It is unlikely that this does not hold
      Set<SMGObject> possiblePrevObj = getObjectsWithValue(valueForPointerToWardsThis);
      Preconditions.checkArgument(possiblePrevObj.size() == 1);
      prevObj = possiblePrevObj.iterator().next();
    }
    for (Entry<SMGValue, SMGPointsToEdge> pointsToEdgesAndValues : pointsToEdges.entrySet()) {
      SMGPointsToEdge pointer = pointsToEdgesAndValues.getValue();
      SMGValue value = pointsToEdgesAndValues.getKey();
      // All pointers towards this object are changed to the previous object except the one pointer
      // that goes to the next segment
      if (pointer.pointsTo().equals(object) && !value.equals(valueForPointerToWardsThis)) {
        // SLL has no back pointer
        // Since we remove a segment, we assume the next segment is the target of all pointers
        // towards this
        newPointers.put(
            value, new SMGPointsToEdge(prevObj, BigInteger.ZERO, SMGTargetSpecifier.IS_REGION));
      } else if (value.equals(valueForPointerToWardsThis)) {
        // Set the pointer of the previous segment to the next of the deleted list
        if (pointerToNext.pointsTo().isZero()) {
          // change the actual value
          newHVEdges = replaceValueByZero(value, newHVEdges);
        } else {
          newPointers.put(value, pointerToNext);
        }
      } else {
        newPointers.put(value, pointer);
      }
    }
    // Remove every value that now is no longer present, but replace the has value edges with edges
    // at the correct nfo with edges leading to the next segment
    /*
    for (Entry<SMGObject, PersistentSet<SMGHasValueEdge>> entry : hasValueEdges.entrySet()) {
      if (!entry.getKey().equals(object)) {
        for (SMGHasValueEdge value : entry.getValue()) {
          toRemove.remove(value.hasValue());
        }
      }
    }
    PersistentSet<SMGValue> values = smgValues;
    for (SMGValue valueToRem : toRemove) {
      // check if this is correct!
      if (!valueToRem.isZero()) {
        values = values.removeAndCopy(valueToRem);
      }
    }*/
    return new SMG(
        smgObjects.removeAndCopy(object),
        smgValues,
        newHVEdges.removeAndCopy(object),
        newPointers.buildOrThrow(),
        sizeOfPointer);
  }

  /**
   * Removes i.e. a 0+ object from the SMG. This assumed that no valid pointers to the object exist
   * and removes all pointers towards the object object and all objects that are connected to those
   * pointers (removes the subgraph). Also deleted the object object in the end. This does not check
   * for pointers that point away from the object!
   *
   * @param object the object to remove and start the subgraph removal.
   * @return a new SMG with the object and its subgraphs removed + the all removed objects.
   */
  public SMGAndSMGObjects copyAndRemoveObjectAndSubSMG(SMGObject object) {
    if (!smgObjects.containsKey(object) || !isValid(object)) {
      return SMGAndSMGObjects.ofEmptyObjects(this);
    }
    ImmutableMap.Builder<SMGValue, SMGPointsToEdge> newPointers = ImmutableMap.builder();
    ImmutableSet.Builder<SMGObject> objectsToRemoveBuilder = ImmutableSet.builder();
    ImmutableSet.Builder<SMGValue> valuesToRemoveBuilder = ImmutableSet.builder();
    // We expect there to be very few, if any objects towards a 0+ element as we don't join
    for (Entry<SMGValue, SMGPointsToEdge> pointsToEntry : pointsToEdges.entrySet()) {
      if (pointsToEntry.getValue().pointsTo().equals(object)) {
        valuesToRemoveBuilder.add(pointsToEntry.getKey());
      } else {
        newPointers.put(pointsToEntry);
      }
    }

    ImmutableSet<SMGValue> valuesToRemove = valuesToRemoveBuilder.build();
    if (!valuesToRemove.isEmpty()) {
      for (PersistentSet<SMGHasValueEdge> hasValueEntryValue : hasValueEdges.values()) {
        for (SMGHasValueEdge valueEdge : hasValueEntryValue) {
          if (valuesToRemove.contains(valueEdge.hasValue())) {
            objectsToRemoveBuilder.add(object);
          }
        }
      }
    }

    SMG currentSMG =
        new SMG(
            smgObjects.removeAndCopy(object),
            smgValues,
            hasValueEdges.removeAndCopy(object),
            newPointers.buildOrThrow(),
            sizeOfPointer);
    ImmutableSet<SMGObject> objectsToRemove = objectsToRemoveBuilder.build();
    ImmutableSet.Builder<SMGObject> objectsThatHaveBeenRemovedBuilder = ImmutableSet.builder();
    objectsThatHaveBeenRemovedBuilder.add(object);
    if (!objectsToRemove.isEmpty()) {
      for (SMGObject toRemove : objectsToRemove) {
        SMGAndSMGObjects newSMGAndRemoved = currentSMG.copyAndRemoveObjectAndSubSMG(toRemove);
        objectsThatHaveBeenRemovedBuilder.addAll(newSMGAndRemoved.getSMGObjects());
        currentSMG = newSMGAndRemoved.getSMG();
      }
    }

    return SMGAndSMGObjects.of(currentSMG, objectsThatHaveBeenRemovedBuilder.build());
  }

  private ImmutableSet<SMGObject> getObjectsWithValue(SMGValue valueForPointerToWardsThis) {
    ImmutableSet.Builder<SMGObject> builder = ImmutableSet.builder();
    // Search for all object with the value valueForPointerToWardsThis in them that are
    for (Entry<SMGObject, Boolean> entry : smgObjects.entrySet()) {
      SMGObject searchObj = entry.getKey();
      if (searchObj.isZero() || !entry.getValue()) {
        break;
      }
      for (SMGHasValueEdge hve : hasValueEdges.getOrDefault(searchObj, PersistentSet.of())) {
        if (hve.hasValue().equals(valueForPointerToWardsThis)) {
          builder.add(searchObj);
        }
      }
    }
    return builder.build();
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
   * guaranteed to be completely accurate!
   *
   * @param object The object from which is to be read.
   * @param offset The offset from which on the field in the object is to be read.
   * @param sizeInBits Size in bits, specifying the size to be read from the offset.
   * @return A updated SMG and the SMTValue that is a read re-interpretation of the field in the
   *     object. May be 0, a symbolic value or a new unknown symbolic value.
   */
  public SMGandValue readValue(SMGObject object, BigInteger offset, BigInteger sizeInBits) {
    // Check that our field is inside the object: offset + sizeInBits <= size(object)
    Preconditions.checkArgument(offset.add(sizeInBits).compareTo(object.getSize()) <= 0);

    // let v := H(o, of, t)
    // TODO: Currently getHasValueEdgeByOffsetAndSize returns any edge it finds.
    // Check if multiple edges may exists for the same offset and size! -> There should never be
    // multiple edges for the exact same offset/size
    // TODO: We only check for the exact matches to offset + size, what if one reads
    // a field that is completely covered by a value field? I guess this is meant this way, but we
    // should discuss it nevertheless.
    Predicate<SMGHasValueEdge> filterByOffsetAndSize =
        o -> o.getOffset().compareTo(offset) == 0 && o.getSizeInBits().compareTo(sizeInBits) == 0;
    Optional<SMGHasValueEdge> maybeValue =
        getHasValueEdgeByPredicate(object, filterByOffsetAndSize);

    // if v != undefined then return (smg, v)
    if (maybeValue.isPresent()) {
      return new SMGandValue(this, maybeValue.orElseThrow().hasValue());
    }

    // if the field to be read is covered by nullified blocks, i.e. if
    // forall . of <= i < of +  size(t) exists . e element H(o, of, t): i element I(e),
    // let v := 0. Otherwise extend V by a fresh value node v.
    Optional<SMGValue> isCoveredBy = isCoveredByNullifiedBlocks(object, offset, sizeInBits);
    if (isCoveredBy.isPresent()) {
      return new SMGandValue(this, isCoveredBy.orElseThrow());
    }
    int nestingLevel = object.getNestingLevel();
    SMGValue newValue = SMGValue.of(nestingLevel);
    SMG newSMG = copyAndAddValue(newValue);
    // Extend H by the has-value edge o -> v with the offset and size and return (smg,v) based on
    // the newly obtained SMG.
    SMGHasValueEdge newHVEdge = new SMGHasValueEdge(newValue, offset, sizeInBits);
    newSMG = newSMG.copyAndAddHVEdge(newHVEdge, object);
    return new SMGandValue(newSMG, newValue);
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
            edge -> {
              // edgeOffset <= pFieldOffset && pFieldOffset < edgeOffset + edgeSize
              return (edge.getOffset().compareTo(pFieldOffset) <= 0
                      && edge.getOffset().add(edge.getSizeInBits()).compareTo(pFieldOffset) > 0)
                  // edgeOffset > pFieldOffset && edgeOffset < pSizeofInBits + pFieldOffset
                  || (edge.getOffset().compareTo(pFieldOffset) > 0
                      && edge.getOffset().compareTo(pFieldOffset.add(pSizeofInBits)) < 0);
            })
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
    return FluentIterable.from(hasValueEdges.values())
        .transformAndConcat(edges -> FluentIterable.from(edges));
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
        .map(entry -> entry.getKey());
  }

  public ImmutableSet<SMGObject> findAllAddressesForTargetObject(
      SMGObject targetObject, Collection<SMGObject> heapObjects) {
    ImmutableSet.Builder<SMGObject> ret = ImmutableSet.builder();
    for (Entry<SMGValue, SMGPointsToEdge> entry : pointsToEdges.entrySet()) {
      if (targetObject.equals(entry.getValue().pointsTo())) {
        SMGValue pointerValue = entry.getKey();
        for (SMGObject heapObj : heapObjects) {
          if (!isValid(heapObj)) {
            continue;
          }
          for (SMGHasValueEdge hve : hasValueEdges.getOrDefault(heapObj, PersistentSet.of())) {
            if (hve.hasValue() == pointerValue) {
              ret.add(heapObj);
            }
          }
        }
      }
    }
    return ret.build();
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
    if (!(obj instanceof SMG)) {
      return false;
    }
    SMG other = (SMG) obj;
    return Objects.equals(hasValueEdges, other.hasValueEdges)
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

  /**
   * Used to identify a object <-> value relationship. Can be used to map out the current memory.
   *
   * @return the current SMGObject - HasValueEdge mappings.
   */
  public PersistentMap<SMGObject, PersistentSet<SMGHasValueEdge>>
      getSMGObjectsWithSMGHasValueEdges() {
    return hasValueEdges;
  }

  /* Checks that there is only 1 value at all offsets. */
  @SuppressWarnings("unused")
  public boolean sanityCheck() {
    for (Entry<SMGObject, PersistentSet<SMGHasValueEdge>> entry : hasValueEdges.entrySet()) {
      SMGObject obj = entry.getKey(); // For debugging
      PersistentSet<SMGHasValueEdge> hvEdges = entry.getValue();
      for (SMGHasValueEdge edge1 : hvEdges) {
        for (SMGHasValueEdge edge2 : hvEdges) {
          if (edge1 != edge2 && edge1.getOffset().compareTo(edge2.getOffset()) == 0) {
            return false;
          }
        }
      }
    }
    return true;
  }

  public SMG copyHVEdgesFromTo(SMGObject source, SMGObject target) {
    PersistentSet<SMGHasValueEdge> setOfValues = hasValueEdges.get(source);
    if (setOfValues == null) {
      setOfValues = PersistentSet.of();
    }
    return copyAndSetHVEdges(setOfValues, target);
  }

  // Replace the pointer behind value with a new pointer with the new SMGObject target
  public SMG replaceAllPointersTowardsWith(SMGValue pointerValue, SMGObject newTarget) {
    SMGPointsToEdge oldEdge = getPTEdge(pointerValue).orElseThrow();
    assert verifyPointsToEdgeSanity();
    SMG newSMG =
        copyAndSetPTEdges(
            new SMGPointsToEdge(newTarget, oldEdge.getOffset(), oldEdge.targetSpecifier()),
            pointerValue);
    assert verifyPointsToEdgeSanity();
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
    for (Entry<SMGValue, SMGPointsToEdge> pointsToEntry : pointsToEdges.entrySet()) {
      if (pointsToEntry.getValue().pointsTo().equals(oldObj)) {
        SMGValue value = pointsToEntry.getKey();
        newSMG =
            newSMG.copyAndSetPTEdges(
                new SMGPointsToEdge(
                    newTarget,
                    pointsToEntry.getValue().getOffset(),
                    pointsToEntry.getValue().targetSpecifier()),
                value);
      }
    }
    assert verifyPointsToEdgeSanity();
    return newSMG;
  }

  /**
   * @return false if there is more than 1 value with the exact same points-to-edge pointing to the
   *     same object/offset with the same nesting level.
   */
  private boolean verifyPointsToEdgeSanity() {
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
    assert verifyPointsToEdgeSanity();
    int min = 0;
    if (newTarget instanceof SMGSinglyLinkedListSegment) {
      min = ((SMGSinglyLinkedListSegment) newTarget).getMinLength();
    }
    SMG newSMG = this;
    if (newTarget.isZero() || oldObj.isZero()) {
      throw new AssertionError("Can't replace a 0 value!");
    }
    ImmutableSet.Builder<SMGValue> valuesToDecrementBuilder = ImmutableSet.builder();
    for (Entry<SMGValue, SMGPointsToEdge> pointsToEntry : pointsToEdges.entrySet()) {
      if (pointsToEntry.getValue().pointsTo().equals(oldObj)) {
        SMGValue value = pointsToEntry.getKey();
        // Since we decrement the nesting level afterwards, we check for 1 instead of 0
        // The equals for values checks only the ID not the nesting level!!!
        newSMG =
            newSMG.copyAndSetPTEdges(
                new SMGPointsToEdge(
                    newTarget,
                    pointsToEntry.getValue().getOffset(),
                    pointsToEntry.getValue().targetSpecifier()),
                value.withNestingLevelAndCopy(value.getNestingLevel() + incrementAmount));
        assert verifyPointsToEdgeSanity();

        if (min <= value.getNestingLevel() + incrementAmount) {
          Preconditions.checkArgument(min > value.getNestingLevel() + incrementAmount);
        }

        // Remember the values to decrement the nesting level
        valuesToDecrementBuilder.add(value);
      }
    }
    ImmutableSet<SMGValue> valuesToDecrement = valuesToDecrementBuilder.build();
    PersistentMap<SMGObject, PersistentSet<SMGHasValueEdge>> newHasValueEdges = hasValueEdges;
    // Update the nesting level
    for (Entry<SMGObject, PersistentSet<SMGHasValueEdge>> objToHvEntry : hasValueEdges.entrySet()) {
      SMGObject currentObject = objToHvEntry.getKey();
      boolean contains = false;
      PersistentSet<SMGHasValueEdge> hvEdges = objToHvEntry.getValue();
      for (SMGHasValueEdge hvEdge : objToHvEntry.getValue()) {
        if (valuesToDecrement.contains(hvEdge.hasValue())) {
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

          // Preconditions.checkArgument(min > hvEdge.hasValue().getNestingLevel() +
          // incrementAmount);
        }
      }
      if (contains) {
        // Save to copy the entire entry
        newHasValueEdges = newHasValueEdges.removeAndCopy(currentObject);
        newHasValueEdges = newHasValueEdges.putAndCopy(currentObject, hvEdges);
      }
    }
    return newSMG.replaceHasValueEdgesAndCopy(newHasValueEdges);
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
    ImmutableSet.Builder<SMGValue> valuesToDecrementBuilder = ImmutableSet.builder();
    for (Entry<SMGValue, SMGPointsToEdge> pointsToEntry : pointsToEdges.entrySet()) {
      if (pointsToEntry.getValue().pointsTo().equals(oldObj)) {
        SMGValue value = pointsToEntry.getKey();
        // Since we decrement the nesting level afterwards, we check for 1 instead of 0
        if (value.getNestingLevel() == replacementLevel) {
          // The equals for values checks only the ID not the nesting level!!!
          newSMG =
              newSMG.copyAndSetPTEdges(
                  new SMGPointsToEdge(
                      newTarget,
                      pointsToEntry.getValue().getOffset(),
                      pointsToEntry.getValue().targetSpecifier()),
                  value.withNestingLevelAndCopy(0));
          // Remember the values to change the nesting level
          valuesToDecrementBuilder.add(value);
          assert verifyPointsToEdgeSanity();
        }
      }
    }
    ImmutableSet<SMGValue> valuesToDecrement = valuesToDecrementBuilder.build();
    PersistentMap<SMGObject, PersistentSet<SMGHasValueEdge>> newHasValueEdges =
        newSMG.hasValueEdges;
    // Update the nesting level
    for (Entry<SMGObject, PersistentSet<SMGHasValueEdge>> objToHvEntry :
        newSMG.hasValueEdges.entrySet()) {
      SMGObject currentObject = objToHvEntry.getKey();
      boolean contains = false;
      PersistentSet<SMGHasValueEdge> hvEdges = objToHvEntry.getValue();
      for (SMGHasValueEdge hvEdge : objToHvEntry.getValue()) {
        if (valuesToDecrement.contains(hvEdge.hasValue())) {
          contains = true;
          hvEdges = hvEdges.removeAndCopy(hvEdge);
          hvEdges =
              hvEdges.addAndCopy(
                  new SMGHasValueEdge(
                      hvEdge.hasValue().withNestingLevelAndCopy(0),
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
    return newSMG.replaceHasValueEdgesAndCopy(newHasValueEdges);
  }

  // Needed for tests
  public SMG replaceSMGValueNestingLevel(SMGValue value, int replacementLevel) {
    SMG newSMG = this;
    assert verifyPointsToEdgeSanity();
    for (Entry<SMGValue, SMGPointsToEdge> pointsToEntry : pointsToEdges.entrySet()) {
      if (pointsToEntry.getKey().equals(value)) {
        // Since we decrement the nesting level afterwards, we check for 1 instead of 0
        // The equals for values checks only the ID not the nesting level!!!
        newSMG =
            newSMG.copyAndSetPTEdges(
                new SMGPointsToEdge(
                    pointsToEntry.getValue().pointsTo(),
                    pointsToEntry.getValue().getOffset(),
                    pointsToEntry.getValue().targetSpecifier()),
                value.withNestingLevelAndCopy(replacementLevel));
        assert verifyPointsToEdgeSanity();
      }
    }
    PersistentMap<SMGObject, PersistentSet<SMGHasValueEdge>> newHasValueEdges = hasValueEdges;
    // Update the nesting level
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
    return newSMG.replaceHasValueEdgesAndCopy(newHasValueEdges);
  }

  private SMG replaceHasValueEdgesAndCopy(
      PersistentMap<SMGObject, PersistentSet<SMGHasValueEdge>> newHasValueEdges) {
    return new SMG(smgObjects, smgValues, newHasValueEdges, pointsToEdges, sizeOfPointer);
  }

  /*
   * Get the object pointing towards a 0+ list segment.
   * We can assume that there is only 1 of those objects.
   */
  public SMGObject getPreviousObjectOfZeroPlusAbstraction(SMGValue ptObject) {
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
}
