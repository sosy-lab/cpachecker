// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2021 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.util.smg;

import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import java.util.Collection;
import java.util.Set;
import org.sosy_lab.cpachecker.util.smg.graph.SMGEdge;
import org.sosy_lab.cpachecker.util.smg.graph.SMGHasValueEdge;
import org.sosy_lab.cpachecker.util.smg.graph.SMGListSegment;
import org.sosy_lab.cpachecker.util.smg.graph.SMGObject;
import org.sosy_lab.cpachecker.util.smg.graph.SMGPointsToEdge;
import org.sosy_lab.cpachecker.util.smg.graph.SMGValue;

/**
 * Class to represent a immutable bipartite symbolic memory graph. Manipulating methods return a
 * modified copy but do not modify a certain instance.
 */
public class SMG {
  private final ImmutableSet<SMGObject> smgObjects;
  private final ImmutableSet<SMGValue> smgValues;
  private final ImmutableMap<SMGObject, Set<SMGHasValueEdge>> hasValueEdges;
  private final ImmutableMap<SMGValue, Set<SMGPointsToEdge>> pointsToEdges;


  private final SMGObject nullObject = SMGObject.nullInstance();

  public SMG() {
    pointsToEdges = ImmutableMap.of();
    hasValueEdges = ImmutableMap.of();
    smgValues = ImmutableSet.of();
    smgObjects = ImmutableSet.of(nullObject);
  }

  private SMG(
      ImmutableSet<SMGObject> pSmgObjects,
      ImmutableSet<SMGValue> pSmgValues,
      ImmutableMap<SMGObject, Set<SMGHasValueEdge>> pHasValueEdges,
      ImmutableMap<SMGValue, Set<SMGPointsToEdge>> pPointsToEdges) {
    smgObjects = pSmgObjects;
    smgValues = pSmgValues;
    hasValueEdges = pHasValueEdges;
    pointsToEdges = pPointsToEdges;
  }

  /**
   * Creates a copy of the SMG an adds the given object.
   *
   * @param pObject - the object to be added
   * @return a modified copy of the SMG
   */
  public SMG copyAndAddObject(SMGObject pObject) {
    ImmutableSet<SMGObject> pSMGObjects = ImmutableSet.<SMGObject>builder().add(pObject)
        .addAll(smgObjects)
        .build();
    return new SMG(
        pSMGObjects,
        smgValues,
        hasValueEdges,
        pointsToEdges);
  }

  /**
   * Creates a copy of the SMG an adds the given value.
   *
   * @param pValue - the object to be added
   * @return a modified copy of the SMG
   */
  public SMG copyAndAddValue(SMGValue pValue) {
    ImmutableSet<SMGValue> pSMGValues =
        ImmutableSet.<SMGValue>builder().add(pValue).addAll(smgValues).build();
    return new SMG(
        smgObjects,
        pSMGValues,
        hasValueEdges,
        pointsToEdges);
  }

  /**
   * Creates a copy of the SMG an adds the given has value edge.
   *
   * @param edge - the edge to be added
   * @param source - the source object
   * @return a modified copy of the SMG
   */
  public SMG copyAndAddHVEdge(SMGHasValueEdge edge, SMGObject source) {

    if (hasValueEdges.containsKey(source) && hasValueEdges.get(source).contains(edge)) {
      return this;
    }

    ImmutableMap.Builder<SMGObject, Set<SMGHasValueEdge>> hVBuilder =
        ImmutableMap.<SMGObject, Set<SMGHasValueEdge>>builder().putAll(hasValueEdges);

    ImmutableSet.Builder<SMGHasValueEdge> setBuilder = ImmutableSet.builder();
    setBuilder.addAll(hasValueEdges.get(source)).add(edge);

    hVBuilder.put(source, setBuilder.build());

    return new SMG(
        smgObjects,
        smgValues,
        hVBuilder.build(),
        pointsToEdges);
  }

  /**
   * Creates a copy of the SMG an adds the given points to edge.
   *
   * @param edge - the edge to be added
   * @param source - the source value
   * @return a modified copy of the SMG
   */
  public SMG copyAndAddPTEdge(SMGPointsToEdge edge, SMGValue source) {

    if (pointsToEdges.containsKey(source) && pointsToEdges.get(source).contains(edge)) {
      return this;
    }

    ImmutableMap.Builder<SMGValue, Set<SMGPointsToEdge>> pTBuilder =
        ImmutableMap.<SMGValue, Set<SMGPointsToEdge>>builder().putAll(pointsToEdges);

    ImmutableSet.Builder<SMGPointsToEdge> setBuilder = ImmutableSet.builder();
    setBuilder.addAll(pointsToEdges.get(source)).add(edge);

    pTBuilder.put(source, setBuilder.build());

    return new SMG(
        smgObjects,
        smgValues,
        hasValueEdges,
        pTBuilder.build());
  }


  public SMGObject getNullObject() {
    return nullObject;
  }

  public Collection<SMGObject> getObjects() {
    return ImmutableSet.copyOf(smgObjects);
  }

  public Collection<SMGValue> getValues() {
    return ImmutableSet.copyOf(smgValues);
  }

  public Collection<SMGEdge> getEdges(SMGObject pRegion) {

    if (hasValueEdges.containsKey(pRegion)) {
      return ImmutableSet.copyOf(hasValueEdges.get(pRegion));
    }

    return ImmutableSet.of();
  }

  public Collection<SMGEdge> getEdges(SMGValue pValue) {

    if (pointsToEdges.containsKey(pValue)) {
      return ImmutableSet.copyOf(pointsToEdges.get(pValue));
    }

    return ImmutableSet.of();
  }

  public Collection<SMGListSegment> getDLLs() {
    return ImmutableSet.of();
  }

}
