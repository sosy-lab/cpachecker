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
import java.math.BigInteger;
import java.util.Collection;
import java.util.Set;
import org.sosy_lab.cpachecker.util.smg.graph.SMGEdge;
import org.sosy_lab.cpachecker.util.smg.graph.SMGHasValueEdge;
import org.sosy_lab.cpachecker.util.smg.graph.SMGListSegment;
import org.sosy_lab.cpachecker.util.smg.graph.SMGObject;
import org.sosy_lab.cpachecker.util.smg.graph.SMGPointsToEdge;
import org.sosy_lab.cpachecker.util.smg.graph.SMGValue;

public class SMG {
  private final ImmutableSet<SMGObject> smgObjects;
  private final ImmutableSet<SMGValue> smgValues;
  private final ImmutableMap<SMGObject, Set<SMGHasValueEdge>> hasValueEdges;
  private final ImmutableMap<SMGValue, Set<SMGPointsToEdge>> pointsToEdges;
  private final ImmutableMap<BigInteger, SMGObject> edges;

  private final ImmutableMap<SMGObject, Set<SMGObject>> notEqualObjects;
  private final ImmutableMap<SMGObject, Set<SMGObject>> possiblyEqualObjects;

  private final SMGObject nullObject = SMGObject.nullInstance();

  public SMG() {
    notEqualObjects = ImmutableMap.of();
    possiblyEqualObjects = ImmutableMap.of();
    edges = ImmutableMap.of();
    pointsToEdges = ImmutableMap.of();
    hasValueEdges = ImmutableMap.of();
    smgValues = ImmutableSet.of();
    smgObjects = ImmutableSet.of(nullObject);
  }

  public void addObject(SMGObject pObject) {
  }

  public SMGObject getNullObject() {
    return nullObject;
  }

  public Collection<SMGObject> getObjects() {
    return ImmutableSet.copyOf(smgObjects);
  }

  public Collection<SMGEdge> getEdges(SMGObject pRegion) {

    if (hasValueEdges.containsKey(pRegion)) {
      return ImmutableSet.copyOf(hasValueEdges.get(pRegion));
    }

    return ImmutableSet.of();
  }

  public Collection<SMGListSegment> getDLLs() {
    return ImmutableSet.of();
  }

}
