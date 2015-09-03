/*
 *  CPAchecker is a tool for configurable software verification.
 *  This file is part of CPAchecker.
 *
 *  Copyright (C) 2007-2014  Dirk Beyer
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
package org.sosy_lab.solver.basicimpl;

import java.util.List;

import org.sosy_lab.solver.SolverException;
import org.sosy_lab.solver.api.BooleanFormula;
import org.sosy_lab.solver.api.Formula;
import org.sosy_lab.solver.api.QuantifiedFormulaManager;

import com.google.common.collect.Lists;


public abstract class AbstractQuantifiedFormulaManager<TFormulaInfo, TType, TEnv>
  extends AbstractBaseFormulaManager<TFormulaInfo, TType, TEnv>
  implements QuantifiedFormulaManager {

  protected AbstractQuantifiedFormulaManager(
      FormulaCreator<TFormulaInfo, TType, TEnv> pCreator) {
    super(pCreator);
  }

  private BooleanFormula wrap(TFormulaInfo formulaInfo) {
    return getFormulaCreator().encapsulateBoolean(formulaInfo);
  }

  @Override
  public BooleanFormula exists(List<? extends Formula> pVariables, BooleanFormula pBody) {
    return wrap(exists(
        Lists.transform(pVariables, extractor),
        extractInfo(pBody)));
  }
  protected abstract TFormulaInfo exists(List<TFormulaInfo> pVariables, TFormulaInfo pBody);

  @Override
  public final BooleanFormula forall(List<? extends Formula> pVariables, BooleanFormula pBody) {
    return wrap(forall(
        Lists.transform(pVariables, extractor),
        extractInfo(pBody)));
  }
  protected abstract TFormulaInfo forall(List<TFormulaInfo> pVariables, TFormulaInfo pBody);

  @Override
  public BooleanFormula eliminateQuantifiers(BooleanFormula pF) throws InterruptedException, SolverException {
    return wrap(eliminateQuantifiers(extractInfo(pF)));
  }
  protected abstract TFormulaInfo eliminateQuantifiers(TFormulaInfo pExtractInfo) throws SolverException, InterruptedException;

  @Override
  public boolean isQuantifier(BooleanFormula pF) {
    return isQuantifier(extractInfo(pF));
  }
  protected abstract boolean isQuantifier(TFormulaInfo pExtractInfo);

  @Override
  public boolean isForall(BooleanFormula pF) {
    return isForall(extractInfo(pF));
  }
  protected abstract boolean isForall(TFormulaInfo pExtractInfo);

  @Override
  public boolean isExists(BooleanFormula pF) {
    return isExists(extractInfo(pF));
  }
  protected abstract boolean isExists(TFormulaInfo pExtractInfo);

  @Override
  public int numQuantifierBound(BooleanFormula pF) {
    return numQuantifierBound(extractInfo(pF));
  }
  protected abstract int numQuantifierBound(TFormulaInfo pExtractInfo);

  @Override
  public BooleanFormula getQuantifierBody(BooleanFormula pF) {
    return wrap(getQuantifierBody(extractInfo(pF)));
  }
  protected abstract TFormulaInfo getQuantifierBody(TFormulaInfo pExtractInfo);


  public boolean isBoundByQuantifier(Formula pF) {
    return isBoundByQuantifier(extractInfo(pF));
  }

  public abstract boolean isBoundByQuantifier(TFormulaInfo pF);
}
