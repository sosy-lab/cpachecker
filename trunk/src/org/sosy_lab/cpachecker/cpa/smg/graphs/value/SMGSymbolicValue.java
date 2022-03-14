// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2007-2020 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.cpa.smg.graphs.value;

import java.math.BigInteger;

public interface SMGSymbolicValue extends SMGValue {

  /**
   * Get a random unique identifier for this symbolic value. Do never use the identifier for any
   * computation.
   *
   * <p>Special case: {@link SMGZeroValue#getId} has an id value of ZERO.
   */
  BigInteger getId();
}
