// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2021 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.util.smg;

import com.google.common.collect.ImmutableSet;
import java.util.Collection;
import org.sosy_lab.common.collect.PathCopyingPersistentTreeMap;
import org.sosy_lab.common.collect.PersistentMap;
import org.sosy_lab.cpachecker.cpa.smg.util.PersistentSet;
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
  // TODO I don't like using utility implementations of the old SMG analysis
  private final PersistentSet<SMGObject> smgObjects;
  private final PersistentSet<SMGValue> smgValues;
  private final PersistentMap<SMGObject, PersistentSet<SMGHasValueEdge>> hasValueEdges;
  private final PersistentMap<SMGValue, PersistentSet<SMGPointsToEdge>> pointsToEdges;

  private final SMGObject nullObject = SMGObject.nullInstance();

  public SMG() {
    pointsToEdges = PathCopyingPersistentTreeMap.of();
    hasValueEdges = PathCopyingPersistentTreeMap.of();
    smgValues = PersistentSet.of();
    smgObjects = PersistentSet.<SMGObject>of().addAndCopy(nullObject);
  }

  private SMG(
      PersistentSet<SMGObject> pSmgObjects,
      PersistentSet<SMGValue> pSmgValues,
      PersistentMap<SMGObject, PersistentSet<SMGHasValueEdge>> pHasValueEdges,
      PersistentMap<SMGValue, PersistentSet<SMGPointsToEdge>> pPointsToEdges) {
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
    return new SMG(smgObjects.addAndCopy(pObject), smgValues, hasValueEdges, pointsToEdges);
  }

  /**
   * Creates a copy of the SMG an adds the given value.
   *
   * @param pValue - the object to be added
   * @return a modified copy of the SMG
   */
  public SMG copyAndAddValue(SMGValue pValue) {
    return new SMG(smgObjects, smgValues.addAndCopy(pValue), hasValueEdges, pointsToEdges);
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

    PersistentSet<SMGHasValueEdge> edges = hasValueEdges.getOrDefault(source, PersistentSet.of());
    edges.addAndCopy(edge);
    return new SMG(smgObjects, smgValues, hasValueEdges.putAndCopy(source, edges), pointsToEdges);
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

    PersistentSet<SMGPointsToEdge> edges = pointsToEdges.getOrDefault(source, PersistentSet.of());
    edges.addAndCopy(edge);
    return new SMG(smgObjects, smgValues, hasValueEdges, pointsToEdges.putAndCopy(source, edges));
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
