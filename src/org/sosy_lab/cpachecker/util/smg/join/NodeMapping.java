// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2021 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.util.smg.join;

import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Preconditions.checkNotNull;
import static com.google.common.base.Preconditions.checkState;
import static org.sosy_lab.cpachecker.util.smg.graph.SMGTargetSpecifier.IS_ALL_POINTER;
import static org.sosy_lab.cpachecker.util.smg.graph.SMGTargetSpecifier.IS_FIRST_POINTER;
import static org.sosy_lab.cpachecker.util.smg.graph.SMGTargetSpecifier.IS_LAST_POINTER;
import static org.sosy_lab.cpachecker.util.smg.graph.SMGTargetSpecifier.IS_REGION;

import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableMap.Builder;
import edu.umd.cs.findbugs.annotations.Nullable;
import java.util.Collection;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Objects;
import java.util.Optional;
import org.sosy_lab.cpachecker.cpa.smg2.SymbolicProgramConfiguration;
import org.sosy_lab.cpachecker.util.smg.graph.SMGObject;
import org.sosy_lab.cpachecker.util.smg.graph.SMGPointsToEdge;
import org.sosy_lab.cpachecker.util.smg.graph.SMGTargetSpecifier;
import org.sosy_lab.cpachecker.util.smg.graph.SMGValue;

public class NodeMapping {

  private final Map<SMGObject, SMGObject> objectMap;
  private final Map<SMGValue, Map<SMGTargetSpecifier, SMGValue>> valueMap;

  public NodeMapping() {
    objectMap = ImmutableMap.of();
    valueMap = ImmutableMap.of();
  }

  private NodeMapping(
      Map<SMGObject, SMGObject> pObjectMap,
      Map<SMGValue, Map<SMGTargetSpecifier, SMGValue>> pValueMap) {
    objectMap = pObjectMap;
    valueMap = pValueMap;
  }

  /**
   * Returns the mapping for the given value, iff there exists one with only one mapped {@link
   * SMGTargetSpecifier}. Throws an exception if there is either no mapping known, or more than one
   * {@link SMGTargetSpecifier} mapped.
   */
  public SMGValue getMappedValue(SMGValue value) {
    // TODO: allow return of null for no mapping for efficiency reasons
    Map<SMGTargetSpecifier, SMGValue> innerMap = valueMap.get(value);
    checkNotNull(innerMap);
    checkState(innerMap.size() == 1 && innerMap.get(IS_REGION) != null);
    return innerMap.get(IS_REGION);
  }

  public SMGValue getMappedValue(SMGValue value, SMGTargetSpecifier pSMGTargetSpecifier) {
    // TODO: allow return of null for no mapping for efficiency reasons
    checkNotNull(value);
    checkNotNull(pSMGTargetSpecifier);
    Map<SMGTargetSpecifier, SMGValue> innerMap = valueMap.get(value);
    // The idea is that this is only used for non-regions, aka only in the methods in merge that
    // concern linked lists.
    // It might happen that this is not possible, and we need to use the at all times!
    checkNotNull(innerMap);
    checkState(!innerMap.containsKey(IS_REGION) || innerMap.size() == 1);
    return innerMap.get(pSMGTargetSpecifier);
  }

  public SMGValue getMappedValue(
      SMGValue value, SymbolicProgramConfiguration spcToGetTargetSpecifier) {
    // TODO: allow return of null for no mapping for efficiency reasons
    checkNotNull(value);
    checkNotNull(spcToGetTargetSpecifier);
    Optional<SMGPointsToEdge> maybeSpec = spcToGetTargetSpecifier.getSmg().getPTEdge(value);
    return getMappedValue(value, maybeSpec.orElseThrow().targetSpecifier());
  }

  public @Nullable SMGObject getMappedObject(SMGObject object) {
    checkNotNull(object);
    return objectMap.get(object);
  }

  public NodeMapping copyAndAddMapping(SMGValue vOld, SMGValue vNew) {
    checkNotNull(vOld);
    checkNotNull(vNew);
    // TODO: add assertion that these are regions?
    return copyAndAddMapping(vOld, vNew, IS_REGION);
  }

  public boolean containsKey(SMGObject pObject) {
    checkNotNull(pObject);
    return objectMap.containsKey(pObject);
  }

  public boolean containsKey(SMGValue pValue) {
    checkNotNull(pValue);
    return valueMap.containsKey(pValue);
  }

  public NodeMapping copyAndAddMapping(
      SMGValue vOld, SMGValue vNew, SMGTargetSpecifier pSMGTargetSpecifier) {
    checkNotNull(vOld);
    checkNotNull(vNew);
    checkNotNull(pSMGTargetSpecifier);
    checkArgument(!vOld.isZero());
    checkArgument(!vNew.isZero());

    Map<SMGTargetSpecifier, SMGValue> existingInnerMap = valueMap.get(vOld);
    Builder<SMGValue, Map<SMGTargetSpecifier, SMGValue>> newMapBuilder = ImmutableMap.builder();
    if (existingInnerMap != null) {
      // REGION => no FIRST/LAST/ALL
      // FIRST/LAST/ALL => no REGION
      checkState(!existingInnerMap.containsKey(IS_REGION) || existingInnerMap.size() == 1);
      checkState(
          (!existingInnerMap.containsKey(IS_FIRST_POINTER)
                  && !existingInnerMap.containsKey(IS_LAST_POINTER)
                  && !existingInnerMap.containsKey(IS_ALL_POINTER))
              || !existingInnerMap.containsKey(IS_REGION));

      newMapBuilder.put(
          vOld,
          ImmutableMap.<SMGTargetSpecifier, SMGValue>builder()
              .putAll(existingInnerMap)
              .put(pSMGTargetSpecifier, vNew)
              .buildOrThrow());
      for (Entry<SMGValue, Map<SMGTargetSpecifier, SMGValue>> oldEntry : valueMap.entrySet()) {
        if (oldEntry.getValue() != existingInnerMap) {
          // TODO: is buildKeepingLast faster than this?
          newMapBuilder.put(oldEntry);
        }
      }

    } else {
      newMapBuilder.putAll(valueMap).put(vOld, ImmutableMap.of(pSMGTargetSpecifier, vNew));
    }
    return new NodeMapping(objectMap, newMapBuilder.buildOrThrow());
  }

  public NodeMapping copyAndAddMapping(SMGObject oOld, SMGObject oNew) {
    checkNotNull(oOld);
    checkNotNull(oNew);
    checkArgument(!oOld.isZero());
    checkArgument(!oNew.isZero());
    return new NodeMapping(
        ImmutableMap.<SMGObject, SMGObject>builder()
            .putAll(objectMap)
            .put(oOld, oNew)
            .buildOrThrow(),
        valueMap);
  }

  public boolean hasMapping(SMGValue pValue) {
    checkNotNull(pValue);
    return valueMap.containsKey(pValue);
  }

  public boolean hasMapping(SMGValue pValue, SMGTargetSpecifier pTargetSpecifier) {
    checkNotNull(pValue);
    checkNotNull(pTargetSpecifier);
    return valueMap.containsKey(pValue) && valueMap.get(pValue).containsKey(pTargetSpecifier);
  }

  public boolean hasMapping(SMGObject pObject) {
    checkNotNull(pObject);
    return objectMap.containsKey(pObject);
  }

  public Map<SMGObject, SMGObject> getObjectMap() {
    return objectMap;
  }

  public Map<SMGValue, Map<SMGTargetSpecifier, SMGValue>> getValueMap() {
    return valueMap;
  }

  @Override
  public int hashCode() {
    return Objects.hash(objectMap, valueMap);
  }

  @Override
  public boolean equals(Object obj) {
    return obj instanceof NodeMapping other
        && Objects.equals(objectMap, other.objectMap)
        && Objects.equals(valueMap, other.valueMap);
  }

  public NodeMapping copyAndRemoveMappingsTo(
      Collection<SMGObject> objectsToRemoveValueMapping, SymbolicProgramConfiguration spc) {
    ImmutableMap.Builder<SMGValue, Map<SMGTargetSpecifier, SMGValue>> newValuesMapping =
        ImmutableMap.builder();
    for (Entry<SMGValue, Map<SMGTargetSpecifier, SMGValue>> valueMapping : valueMap.entrySet()) {
      for (SMGValue v : valueMapping.getValue().values()) {
        if (spc.getSmg().isPointer(v)) {
          if (!objectsToRemoveValueMapping.contains(
              spc.getSmg().getPTEdge(v).orElseThrow().pointsTo())) {
            newValuesMapping.put(valueMapping);
            if (valueMapping.getValue().size() > 1) {
              SMGObject alreadyCheckedObj = spc.getSmg().getPTEdge(v).orElseThrow().pointsTo();
              assert valueMapping.getValue().entrySet().stream()
                  .allMatch(
                      e ->
                          alreadyCheckedObj.equals(
                              spc.getSmg().getPTEdge(e.getValue()).orElseThrow().pointsTo()));
              break;
            }
          }
        } else {
          newValuesMapping.put(valueMapping);
        }
      }
    }
    ImmutableMap.Builder<SMGObject, SMGObject> newObjectsMapping = ImmutableMap.builder();
    for (Entry<SMGObject, SMGObject> objectMapping : objectMap.entrySet()) {
      if (!objectsToRemoveValueMapping.contains(objectMapping.getValue())) {
        newObjectsMapping.put(objectMapping);
      }
    }

    return new NodeMapping(newObjectsMapping.buildOrThrow(), newValuesMapping.buildOrThrow());
  }
}
