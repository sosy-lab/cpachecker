// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2007-2020 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.cpa.smg.graphs.value;

import java.math.BigInteger;

public interface SMGExplicitValue extends SMGValue {

  BigInteger getValue();

  int getAsInt();

  long getAsLong();

  SMGExplicitValue negate();

  SMGExplicitValue xor(SMGExplicitValue pRVal);

  SMGExplicitValue or(SMGExplicitValue pRVal);

  SMGExplicitValue and(SMGExplicitValue pRVal);

  SMGExplicitValue shiftLeft(SMGExplicitValue pRVal);

  SMGExplicitValue multiply(SMGExplicitValue pRVal);

  SMGExplicitValue divide(SMGExplicitValue pRVal);

  SMGExplicitValue subtract(SMGExplicitValue pRVal);

  SMGExplicitValue add(SMGExplicitValue pRVal);
}
