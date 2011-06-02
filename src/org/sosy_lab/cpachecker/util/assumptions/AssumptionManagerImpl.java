/*
 *  CPAchecker is a tool for configurable software verification.
 *  This file is part of CPAchecker.
 *
 *  Copyright (C) 2007-2010  Dirk Beyer
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
package org.sosy_lab.cpachecker.util.assumptions;

import org.sosy_lab.common.LogManager;
import org.sosy_lab.common.configuration.Configuration;
import org.sosy_lab.common.configuration.InvalidConfigurationException;
import org.sosy_lab.cpachecker.cfa.ast.IASTDeclaration;
import org.sosy_lab.cpachecker.cfa.ast.IASTExpression;
import org.sosy_lab.cpachecker.cfa.ast.IASTNode;
import org.sosy_lab.cpachecker.cfa.ast.IASTSimpleDeclaration;
import org.sosy_lab.cpachecker.cpa.assumptions.genericassumptions.ArithmeticOverflowAssumptionBuilder;
import org.sosy_lab.cpachecker.exceptions.UnrecognizedCCodeException;
import org.sosy_lab.cpachecker.util.predicates.CtoFormulaConverter;
import org.sosy_lab.cpachecker.util.predicates.SSAMap;
import org.sosy_lab.cpachecker.util.predicates.SSAMap.SSAMapBuilder;
import org.sosy_lab.cpachecker.util.predicates.interfaces.Formula;
import org.sosy_lab.cpachecker.util.predicates.interfaces.FormulaList;
import org.sosy_lab.cpachecker.util.predicates.interfaces.FormulaManager;
import org.sosy_lab.cpachecker.util.predicates.mathsat.MathsatFormulaManager;


/**
 * Implementation of AssumptionManager.
 */
public class AssumptionManagerImpl extends CtoFormulaConverter implements AssumptionManager
{

  /**
   * Dummy SSA map that always return 1 as index. Only used here
   * to circumvent the assumptions of FormulaManager
   */
  private static class DummySSAMap extends SSAMapBuilder {
    public DummySSAMap() {
      super(SSAMap.emptySSAMap());
    }

    @Override
    public int getIndex(String pName, FormulaList pArgs) {
      return 1;
    }

    @Override
    public int getIndex(String pVariable) {
      return 1;
    }
  }

  private static volatile FormulaManager fmgr = null;

  // TODO Ugly, probably better to remove singleton pattern here.
  public static FormulaManager createFormulaManager(Configuration pConfig, LogManager pLogger)
  throws InvalidConfigurationException {
    if (fmgr == null) {
      fmgr = new MathsatFormulaManager(pConfig, pLogger);
    }
    return fmgr;
  }

  public AssumptionManagerImpl(Configuration pConfig, LogManager pLogger) throws InvalidConfigurationException {
    super(pConfig, createFormulaManager(pConfig, pLogger), pLogger);
  }

  @Override
  public Formula makeAnd(Formula f, IASTNode p, String function) throws UnrecognizedCCodeException {
    
    if(p instanceof IASTExpression){
      DummySSAMap mapBuilder = new DummySSAMap();

      // previously, instead of directly calling makePredicate, a function was
      // called that used De Morgan's law to transform any occurrence of
      // (!(a && b)) into (!a && !b)
      // I don't see a point in doing this, so I removed it.
      return fmgr.makeAnd(f, makePredicate((IASTExpression)p, true, function, mapBuilder));
    }
    else if(p instanceof IASTSimpleDeclaration){
      IASTSimpleDeclaration decl = (IASTSimpleDeclaration)p;

      boolean isGlobal = ArithmeticOverflowAssumptionBuilder.isDeclGlobal;
      if (isGlobal) {
        assert decl instanceof IASTDeclaration;
        assert ((IASTDeclaration)decl).isGlobal();
      }

      String var = decl.getName();
      if (isGlobal) {
        super.addToGlobalVars(var);
      }
    }
    return f;
  }
  
  @Override
  public Formula makeTrue() {
    return fmgr.makeTrue();
  }
}
