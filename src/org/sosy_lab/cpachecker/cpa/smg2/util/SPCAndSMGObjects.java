// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2022 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.cpa.smg2.util;

import com.google.common.base.Preconditions;
import java.util.Collection;
import org.sosy_lab.cpachecker.cpa.smg2.SymbolicProgramConfiguration;
import org.sosy_lab.cpachecker.util.smg.graph.SMGObject;

public class SPCAndSMGObjects {

  SymbolicProgramConfiguration spc;

  Collection<SMGObject> objects;

  private SPCAndSMGObjects(SymbolicProgramConfiguration pSpc, Collection<SMGObject> pObjects) {
    spc = pSpc;
    objects = pObjects;
  }

  public static SPCAndSMGObjects of(
      SymbolicProgramConfiguration pSpc, Collection<SMGObject> pObjects) {
    Preconditions.checkNotNull(pSpc);
    Preconditions.checkNotNull(pObjects);
    return new SPCAndSMGObjects(pSpc, pObjects);
  }

  public SymbolicProgramConfiguration getSPC() {
    return spc;
  }

  public Collection<SMGObject> getSMGObjects() {
    return objects;
  }
}
