/*
 *  CPAchecker is a tool for configurable software verification.
 *  This file is part of CPAchecker.
 *
 *  Copyright (C) 2007-2012  Dirk Beyer
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

import org.sosy_lab.cpachecker.util.predicates.interfaces.Formula;

/**
 *
 * A Formula represented as a TFormulaInfo object.
 * @param <TFormulaInfo> the solver specific type.
 */
public class AbstractFormula<TFormulaInfo> implements Formula, Serializable {

  private static final long serialVersionUID = 7662624283533815801L;
  private TFormulaInfo formulaInfo;

  public AbstractFormula(TFormulaInfo formulaInfo) {
    assert formulaInfo != null;

    this.formulaInfo = formulaInfo;
  }

  @SuppressWarnings("unchecked")
  @Override
  public boolean equals(Object o) {
    if (!(o instanceof AbstractFormula)) { return false; }
    return formulaInfo.equals(((AbstractFormula<TFormulaInfo>) o).formulaInfo);
  }

  public TFormulaInfo getFormulaInfo() {
    return formulaInfo;
  }

  @SuppressWarnings("unchecked")
  public TFormulaInfo getFormulaInfo(Formula formula) {
    return ((AbstractFormula<TFormulaInfo>)formula).getFormulaInfo();
  }

  @Override
  public int hashCode() {
    return formulaInfo.hashCode();
  }
}
