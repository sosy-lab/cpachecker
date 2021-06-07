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
import java.util.Set;
import org.sosy_lab.cpachecker.util.smg.graph.SMGHasValueEdge;
import org.sosy_lab.cpachecker.util.smg.graph.SMGObject;
import org.sosy_lab.cpachecker.util.smg.graph.SMGPointsToEdge;
import org.sosy_lab.cpachecker.util.smg.graph.SMGValue;

public class SMG {
  private final ImmutableSet<SMGObject> smgObjects;
  private final ImmutableSet<SMGValue> smgValues;
  private final ImmutableMap<SMGObject, SMGHasValueEdge> hasValueEdges;
  private final ImmutableMap<SMGObject, SMGPointsToEdge> pointsToEdges;
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

}
