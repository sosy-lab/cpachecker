// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2022 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.cpa.smg2.util;

import com.google.common.base.Preconditions;
import com.google.common.collect.ImmutableSet;
import java.util.Set;
import org.sosy_lab.cpachecker.util.smg.SMG;
import org.sosy_lab.cpachecker.util.smg.graph.SMGObject;

public class SMGAndSMGObjects {

  private SMG smg;

  private Set<SMGObject> objects;

  private SMGAndSMGObjects(SMG pSmg, Set<SMGObject> pObjects) {
    smg = pSmg;
    objects = pObjects;
  }

  public static SMGAndSMGObjects of(SMG pSmg, Set<SMGObject> pObjects) {
    Preconditions.checkNotNull(pSmg);
    Preconditions.checkNotNull(pObjects);
    return new SMGAndSMGObjects(pSmg, pObjects);
  }

  public static SMGAndSMGObjects ofEmptyObjects(SMG pSmg) {
    Preconditions.checkNotNull(pSmg);
    return new SMGAndSMGObjects(pSmg, ImmutableSet.of());
  }

  public SMG getSMG() {
    return smg;
  }

  public Set<SMGObject> getSMGObjects() {
    return objects;
  }
}
