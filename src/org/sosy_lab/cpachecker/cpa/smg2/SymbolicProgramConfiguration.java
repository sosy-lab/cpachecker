// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2021 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.util.smg;

import org.sosy_lab.common.collect.PersistentMap;
import org.sosy_lab.cpachecker.util.smg.graph.SMGObject;

public class SymbolicProgramConfiguration {

  private final SMG smg;
  private final PersistentMap<SMGVariable, SMGObject> variableToSmgObjectMapping;

  private SymbolicProgramConfiguration(
      SMG pSmg,
      PersistentMap<SMGVariable, SMGObject> pVariableToSmgObjectMapping) {
    variableToSmgObjectMapping = pVariableToSmgObjectMapping;
    smg = pSmg;
  }

  public static SymbolicProgramConfiguration
      of(SMG pSmg, PersistentMap<SMGVariable, SMGObject> pVariableToSmgObjectMapping) {
    return new SymbolicProgramConfiguration(pSmg, pVariableToSmgObjectMapping);
  }

  public PersistentMap<SMGVariable, SMGObject> getVariableToSmgObjectMap() {
    return variableToSmgObjectMapping;
  }

  public SMG getSmg() {
    return smg;
  }

}
