/*
 *  CPAchecker is a tool for configurable software verification.
 *  This file is part of CPAchecker.
 *
 *  Copyright (C) 2007-2015  Dirk Beyer
 *  All rights reserved.
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 *
 *
 *  CPAchecker web page:
 *    http://cpachecker.sosy-lab.org
 */
package org.sosy_lab.cpachecker.util.predicates.interfaces.basicimpl;

import java.io.Serializable;

import org.sosy_lab.cpachecker.util.globalinfo.GlobalInfo;
import org.sosy_lab.cpachecker.util.predicates.interfaces.BooleanFormula;
import org.sosy_lab.cpachecker.util.predicates.interfaces.FormulaManager;

/**
 * Class that can be used as serialization proxy for {@link BooleanFormula} implementations.
 * It stores formulas as String using the {@link FormulaManager}
 * stored in {@link GlobalInfo} on for (de-)serialization.
 */
public final class SerialProxyFormula implements Serializable {

  private static final long serialVersionUID = -7575415230982043491L;
  private final String formula;

  public SerialProxyFormula(final BooleanFormula pF) {
    formula = GlobalInfo.getInstance().getFormulaManager().dumpFormula(pF).toString();
  }

  private Object readResolve() {
    return GlobalInfo.getInstance().getFormulaManager().parse(formula);
  }

}