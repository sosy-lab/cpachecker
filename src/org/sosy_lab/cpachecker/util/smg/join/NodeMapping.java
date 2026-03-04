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
  // We use REGION as default for non-pointers for now
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
    checkArgument(
        innerMap.size() == 1,
        "Requested mapping for %s, but multiple mappings present: %s",
        value,
        innerMap);
    return innerMap.values().iterator().next();
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

  // TODO: WIP
  @SuppressWarnings("all")
  public static MappedValueTuple getMappedValues(
      SMGValue v1,
      SymbolicProgramConfiguration spc1,
      NodeMapping mapping1,
      SMGValue v2,
      SymbolicProgramConfiguration spc2,
      NodeMapping mapping2) {
    Optional<SMGPointsToEdge> maybePTE1 = spc1.getSmg().getPTEdge(v1);
    Optional<SMGPointsToEdge> maybePTE2 = spc2.getSmg().getPTEdge(v2);

    Map<SMGTargetSpecifier, SMGValue> innerMap1 = mapping1.getValueMap().get(v1);
    Map<SMGTargetSpecifier, SMGValue> innerMap2 = mapping2.getValueMap().get(v2);

    if ((maybePTE1.isEmpty() && maybePTE2.isEmpty())
        || (innerMap1.size() <= 1 && innerMap2.size() <= 1)) {
      // TODO: allow null return?
      // Null currently blocked in getMappedValue()
      return new MappedValueTuple(mapping1.getMappedValue(v1), mapping2.getMappedValue(v2));
    } else {
      // At least 1 source is a pointer
      // if (innerMap1.size() <= 1) {}

      // if (innerMap2.size() <= 1) {}

      if (maybePTE1.isPresent() && maybePTE2.isPresent()) {
        // The source specifier might not match the mapped value(s) specifier, as region might map
        // to non-region.
        // This might happen for only one source,
        //  e.g. v1 REGION -> mappedV1 FIRST, v2 REGION -> mappedV2 REGION

      } else if (true) {

      } else {

      }
    }
    return null;
  }

  public record MappedValueTuple(SMGValue mappedValue1, SMGValue mappedValue2) {}

  /**
   * spcToGetTargetSpecifier is supposed to be the specifier currently under construction (i.e. the
   * specifier of the new value, e.g. from the other mapping). It might match oldValue, but oldValue
   * might be REGION, while it is not for the returned value and therefore spcToGetTargetSpecifier.
   */
  public SMGValue getMappedValue(
      SMGValue oldValue, SymbolicProgramConfiguration spcToGetTargetSpecifier) {
    // TODO: allow return of null for no mapping for efficiency reasons
    checkNotNull(oldValue);
    checkNotNull(spcToGetTargetSpecifier);
    Optional<SMGPointsToEdge> maybeSpec = spcToGetTargetSpecifier.getSmg().getPTEdge(oldValue);
    return getMappedValue(oldValue, maybeSpec.orElseThrow().targetSpecifier());
  }

  public SMGObject getMappedObject(SMGObject object) {
    checkNotNull(object);
    return objectMap.get(object);
  }

  /**
   * For usage when copying a sub-SMG. Special version that assumes that 1. vNew is not yet added to
   * the new SPC, but will be and 2. if vOld is a pointer, vNew is not a pointer yet, but will be
   * the same pointer as old by the end of copy sub-SMG.
   */
  public NodeMapping copyAndAddMappingInCopySubSMG(
      SMGValue vOld,
      SymbolicProgramConfiguration spcOld,
      SMGValue vNew,
      SymbolicProgramConfiguration spcNew) {
    checkNotNull(vOld);
    checkNotNull(vNew);
    checkNotNull(spcOld);
    checkNotNull(spcNew);
    checkArgument(spcOld.getSmg().hasValue(vOld));
    checkArgument(spcOld.getValueFromSMGValue(vOld).isPresent());
    checkArgument(!hasMapping(vOld));

    if (spcOld.getSmg().isPointer(vOld)) {
      if (spcNew.getSmg().isPointer(vNew)) {
        return copyAndAddMapping(vOld, spcOld, vNew, spcNew);
      }

      SMGPointsToEdge oldPTE = spcOld.getSmg().getPTEdge(vOld).orElseThrow();
      return copyAndAddMapping(vOld, vNew, oldPTE.targetSpecifier());
    }

    checkArgument(!spcNew.getSmg().isPointer(vNew));
    return copyAndAddMapping(vOld, vNew, IS_REGION);
  }

  /**
   * Maps the values vOld -> vNew for target spec of vNew. Automatically retrieves and sanity checks
   * both values in their SPC/SMG and retrieves the target spec from vNew in spcNew.
   */
  public NodeMapping copyAndAddMapping(
      SMGValue vOld,
      SymbolicProgramConfiguration spcOld,
      SMGValue vNew,
      SymbolicProgramConfiguration spcNew) {
    checkNotNull(vOld);
    checkNotNull(vNew);
    checkNotNull(spcOld);
    checkNotNull(spcNew);
    checkArgument(spcOld.getSmg().hasValue(vOld));
    checkArgument(spcOld.getValueFromSMGValue(vOld).isPresent());
    checkArgument(spcNew.getSmg().hasValue(vNew));
    checkArgument(spcNew.getValueFromSMGValue(vNew).isPresent());

    // TODO: check that this is correct, especially the specs
    if (spcOld.getSmg().isPointer(vOld) && spcNew.getSmg().isPointer(vNew)) {
      SMGPointsToEdge oldPTE = spcOld.getSmg().getPTEdge(vOld).orElseThrow();
      SMGPointsToEdge newPTE = spcNew.getSmg().getPTEdge(vNew).orElseThrow();

      // Naive, might not hold
      checkArgument(
          oldPTE.targetSpecifier().equals(newPTE.targetSpecifier())
              || oldPTE.targetSpecifier().equals(IS_REGION));

      return copyAndAddMapping(vOld, vNew, newPTE.targetSpecifier());

    } else {
      // Non-pointers only (and 0 is never mapped!)
      checkArgument(
          !spcOld.getSmg().isPointer(vOld) || spcNew.getSmg().isPointer(vNew),
          "You can't map a pointer to a non-pointer. Did you forget to make the new value a"
              + " pointer?");
      // Use region as default for non-pointers for now
      return copyAndAddMapping(vOld, vNew, IS_REGION);
    }
  }

  private NodeMapping copyAndAddMapping(
      SMGValue vOld, SMGValue vNew, SMGTargetSpecifier pTargetSpecifierOfNewValue) {
    checkNotNull(vOld);
    checkNotNull(vNew);
    checkNotNull(pTargetSpecifierOfNewValue);
    checkArgument(!vOld.isZero());
    checkArgument(!vNew.isZero());

    Map<SMGTargetSpecifier, SMGValue> existingInnerMap = valueMap.get(vOld);
    ImmutableMap.Builder<SMGValue, Map<SMGTargetSpecifier, SMGValue>> newMapBuilder =
        ImmutableMap.builder();
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
              .put(pTargetSpecifierOfNewValue, vNew)
              .buildOrThrow());
      for (Entry<SMGValue, Map<SMGTargetSpecifier, SMGValue>> oldEntry : valueMap.entrySet()) {
        if (oldEntry.getValue() != existingInnerMap) {
          newMapBuilder.put(oldEntry);
        }
      }

    } else {
      newMapBuilder.putAll(valueMap).put(vOld, ImmutableMap.of(pTargetSpecifierOfNewValue, vNew));
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

  /**
   * Returns true if the value entered has SOME mapping, irrespective of target specification etc.
   */
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

  public static boolean hasSomeEqualMapping(
      NodeMapping mapping1,
      SMGValue v1,
      NodeMapping mapping2,
      SMGValue v2,
      boolean allowZeroMapping) {
    checkNotNull(v1);
    checkNotNull(v2);
    Map<SMGTargetSpecifier, SMGValue> innerMap1 = mapping1.getValueMap().get(v1);
    Map<SMGTargetSpecifier, SMGValue> innerMap2 = mapping2.getValueMap().get(v2);
    if (innerMap1 == null || innerMap2 == null) {
      return false;
    } else if (innerMap1.equals(innerMap2)) {
      return true;
    } else {
      boolean strictMatch =
          innerMap1.entrySet().stream()
              .anyMatch(
                  e ->
                      innerMap2.entrySet().contains(e)
                          && (allowZeroMapping || !e.getValue().isZero()));
      boolean looseMatch =
          innerMap1.entrySet().stream()
              .anyMatch(
                  e ->
                      innerMap2.containsValue(e.getValue())
                          && (allowZeroMapping || !e.getValue().isZero()));

      return strictMatch || looseMatch;
    }
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
