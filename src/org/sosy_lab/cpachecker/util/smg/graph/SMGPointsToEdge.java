// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2021 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.util.smg.graph;

import java.math.BigInteger;

public class SMGPointsToEdge implements SMGEdge {

  private final SMGObject pointsToObject;
  private final BigInteger offset;
  private final SMGTargetSpecifier targetSpecifier;

  public SMGPointsToEdge(
      SMGObject pPointsToObject,
      BigInteger pOffset,
      SMGTargetSpecifier pTargetSpecifier) {
    pointsToObject = pPointsToObject;
    offset = pOffset;
    targetSpecifier = pTargetSpecifier;
  }

  public SMGObject pointsTo() {
    return pointsToObject;
  }

  @Override
  public BigInteger getOffset() {
    return offset;
  }

  public SMGTargetSpecifier targetSpecifier() {
    return targetSpecifier;
  }
}
