// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2007-2020 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.cpa.smg.graphs.object.generic;

import org.sosy_lab.cpachecker.cpa.smg.graphs.value.SMGValue;

public class SMGEdgeHasValueTemplate extends SMGEdgeTemplate
    implements SMGEdgeHasValueTemplateWithConcreteValue {

  private final long sizeInBits;

  public SMGEdgeHasValueTemplate(
      SMGObjectTemplate pAbstractObject, SMGValue pAbstractValue, long pOffset, long pSizeInBits) {
    super(pAbstractObject, pAbstractValue, pOffset);
    sizeInBits = pSizeInBits;
  }

  @Override
  public long getSizeInBits() {
    return sizeInBits;
  }

  @Override
  public SMGValue getValue() {
    return getAbstractValue();
  }

  @Override
  public String toString() {
    return getObjectTemplate() + " O" + getOffset() + "B->" + getValue();
  }
}
