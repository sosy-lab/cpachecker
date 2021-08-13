// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2007-2020 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.util.smg.join;

import java.math.BigInteger;
import org.sosy_lab.cpachecker.util.smg.graph.SMGHasValueEdge;
import org.sosy_lab.cpachecker.util.smg.graph.SMGObject;
import org.sosy_lab.cpachecker.util.smg.graph.SMGPointsToEdge;
import org.sosy_lab.cpachecker.util.smg.graph.SMGTargetSpecifier;
import org.sosy_lab.cpachecker.util.smg.graph.SMGValue;

public class SMGJoinTest0 {

  protected static final BigInteger mockType2bSize = BigInteger.valueOf(16);
  protected static final BigInteger mockType4bSize = BigInteger.valueOf(32);
  protected static final BigInteger mockType8bSize = BigInteger.valueOf(64);
  protected static final BigInteger mockType16bSize = BigInteger.valueOf(128);
  protected static final BigInteger mockType32bSize = BigInteger.valueOf(256);


  protected SMGObject createRegion(BigInteger withSize, int withOffset) {
    return SMGObject.of(0, withSize, BigInteger.valueOf(withOffset));
  }

  protected NodeMapping cloneMapping(NodeMapping oldMapping) {
    NodeMapping copyMapping = new NodeMapping();
    oldMapping.getValueMap()
        .entrySet()
        .forEach(entry -> copyMapping.addMapping(entry.getKey(), entry.getValue()));
    oldMapping.getObjectMap()
    .entrySet()
    .forEach(entry -> copyMapping.addMapping(entry.getKey(), entry.getValue()));
    return copyMapping;
  }

  protected SMGObject createRegion(BigInteger withSize) {
    return createRegion(withSize, 0);
  }

  protected SMGObject createRegion(int withSize) {
    return createRegion(BigInteger.valueOf(withSize));
  }

  protected SMGHasValueEdge createHasValueEdgeToZero(BigInteger withSize) {
    return createHasValueEdge(withSize, 0, SMGValue.zeroValue());
  }

  protected SMGHasValueEdge createHasValueEdgeToZero(BigInteger withSize, int andOffset) {
    return createHasValueEdge(withSize, andOffset, SMGValue.zeroValue());
  }

  protected SMGHasValueEdge createHasValueEdge(BigInteger withSize, SMGValue andValue) {
    return createHasValueEdge(withSize, 0, andValue);
  }

  protected SMGHasValueEdge createHasValueEdge(BigInteger withSize, int offset, SMGValue andValue) {
    return new SMGHasValueEdge(andValue, withSize, BigInteger.valueOf(offset));
  }

  protected SMGHasValueEdge createHasValueEdgeToZero(int withSize) {
    return createHasValueEdge(withSize, 0, SMGValue.zeroValue());
  }

  protected SMGHasValueEdge createHasValueEdgeToZero(int withSize, int andOffset) {
    return createHasValueEdge(withSize, andOffset, SMGValue.zeroValue());
  }

  protected SMGHasValueEdge createHasValueEdge(int withSize, SMGValue andValue) {
    return createHasValueEdge(withSize, 0, andValue);
  }

  protected SMGHasValueEdge createHasValueEdge(int withSize, int offset, SMGValue andValue) {
    return new SMGHasValueEdge(andValue, BigInteger.valueOf(withSize), BigInteger.valueOf(offset));
  }


  protected SMGPointsToEdge createPTRegionEdge(int withOffset, SMGObject andObject) {
    return createPTEdge(withOffset, SMGTargetSpecifier.IS_REGION, andObject);
  }

  protected SMGPointsToEdge createPTDLLsEdge(int withOffset, SMGObject andObject) {
    return createPTEdge(withOffset, SMGTargetSpecifier.IS_ALL_POINTER, andObject);
  }

  protected SMGPointsToEdge
      createPTEdge(int withOffset, SMGTargetSpecifier targetSpecifier, SMGObject andObject) {
    return new SMGPointsToEdge(andObject, BigInteger.valueOf(withOffset), targetSpecifier);
  }

  protected SMGValue createValue(int withLevel) {
    return SMGValue.of(withLevel);
  }

  protected SMGValue createValue() {
    return SMGValue.of(0);
  }

}
