// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2021 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.util.smg.graph;

import java.math.BigInteger;

public interface SMGValue extends SMGNode, Comparable<SMGValue> {

  BigInteger getValue();

  @Override
  boolean equals(Object other);

  default boolean isZero() {
    return equals(SMGExplicitValue.nullInstance());
  }

}
