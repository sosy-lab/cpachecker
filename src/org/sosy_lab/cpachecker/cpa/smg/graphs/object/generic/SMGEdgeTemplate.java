// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2007-2020 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.cpa.smg.graphs.object.generic;

import org.sosy_lab.cpachecker.cpa.smg.graphs.value.SMGValue;

public class SMGEdgeTemplate {

  private final SMGObjectTemplate abstractObject;
  private final SMGValue abstractValue;
  private final long offset;

  public SMGEdgeTemplate(SMGObjectTemplate pAbstractObject, SMGValue pAbstractValue, long pOffset) {
    abstractObject = pAbstractObject;
    abstractValue = pAbstractValue;
    offset = pOffset;
  }

  public SMGObjectTemplate getObjectTemplate() {
    return abstractObject;
  }

  public SMGValue getAbstractValue() {
    return abstractValue;
  }

  public long getOffset() {
    return offset;
  }
}
