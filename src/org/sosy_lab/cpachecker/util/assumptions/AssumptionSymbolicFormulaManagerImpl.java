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

import org.eclipse.cdt.core.dom.ast.IASTBinaryExpression;
import org.eclipse.cdt.core.dom.ast.IASTExpression;
import org.eclipse.cdt.core.dom.ast.IASTUnaryExpression;

import org.sosy_lab.cpachecker.exceptions.UnrecognizedCCodeException;
import org.sosy_lab.cpachecker.util.symbpredabstraction.CtoFormulaConverter;
import org.sosy_lab.cpachecker.util.symbpredabstraction.SSAMap;
import org.sosy_lab.cpachecker.util.symbpredabstraction.SSAMap.SSAMapBuilder;
import org.sosy_lab.cpachecker.util.symbpredabstraction.interfaces.SymbolicFormula;
import org.sosy_lab.cpachecker.util.symbpredabstraction.interfaces.SymbolicFormulaList;
import org.sosy_lab.cpachecker.util.symbpredabstraction.interfaces.SymbolicFormulaManager;
import org.sosy_lab.cpachecker.util.symbpredabstraction.mathsat.MathsatSymbolicFormulaManager;

import org.sosy_lab.common.LogManager;
import org.sosy_lab.common.configuration.Configuration;
import org.sosy_lab.common.configuration.InvalidConfigurationException;


/**
 * Implementation of AssumptionSymbolicFormulaManager based on
 * 
 * @author g.theoduloz
 */
public class AssumptionSymbolicFormulaManagerImpl
  extends CtoFormulaConverter
  implements AssumptionSymbolicFormulaManager
{

  /**
   * Dummy SSA map that always return 1 as index. Only used here
   * to circumvent the assumptions of MathsatSymbolicFormulaManager
   */
  private static class DummySSAMap
    extends SSAMapBuilder
  {
    public DummySSAMap() {
      super(SSAMap.emptySSAMap());
    }
    
    @Override
    public int getIndex(String pName, SymbolicFormulaList pArgs) {
      return 1;
    }

    @Override
    public int getIndex(String pVariable) {
      return 1;
    }

    @Override
    public void setIndex(String pName, SymbolicFormulaList pArgs, int pIdx) {
    }

    @Override
    public void setIndex(String pVariable, int pIdx) {
    }
    
    @Override
    public SSAMap build() {
      throw new UnsupportedOperationException();
    }
  }

  private static SymbolicFormulaManager smgr = null;
  private static Configuration config = null;
  private static LogManager logger = null;
  
  // TODO Ugly, probably better to remove singleton pattern here.
  public static SymbolicFormulaManager createSymbolicFormulaManager(Configuration pConfig, LogManager pLogger)
        throws InvalidConfigurationException {
    if (smgr == null) {
      smgr = new MathsatSymbolicFormulaManager(pConfig, pLogger);
      config = pConfig;
      logger = pLogger;
    } else {
      assert config == pConfig;
      assert logger == pLogger;
    }
    return smgr;
  }

  /**
   * Return the singleton instance for this class.
   * {@link #createInstance()} has to be called before at least once.
   */
  public static SymbolicFormulaManager getSymbolicFormulaManager() {
    assert smgr != null;

    return smgr;
  }

  public AssumptionSymbolicFormulaManagerImpl() throws InvalidConfigurationException {
    super(config, getSymbolicFormulaManager(), logger);
  }

  private final SSAMapBuilder dummySSAMap = new DummySSAMap();

  private SymbolicFormula buildSymbolicFormula(IASTExpression p, boolean sign, String function) throws UnrecognizedCCodeException
  {
    // first, check whether we have &&, ||, or !
    if (p instanceof IASTBinaryExpression) {
      IASTBinaryExpression binop = (IASTBinaryExpression) p;
      switch (binop.getOperator()) {
      case IASTBinaryExpression.op_logicalAnd:
        if (sign)
          return smgr.makeAnd(
              buildSymbolicFormula(binop.getOperand1(), true, function),
              buildSymbolicFormula(binop.getOperand2(), true, function));
        else
          return smgr.makeOr(
              buildSymbolicFormula(binop.getOperand1(), false, function),
              buildSymbolicFormula(binop.getOperand2(), false, function));
      case IASTBinaryExpression.op_logicalOr:
        if (sign)
          return smgr.makeOr(
              buildSymbolicFormula(binop.getOperand1(), true, function),
              buildSymbolicFormula(binop.getOperand2(), true, function));
        else
          return smgr.makeAnd(
              buildSymbolicFormula(binop.getOperand1(), false, function),
              buildSymbolicFormula(binop.getOperand2(), false, function));
      }
    } else if (p instanceof IASTUnaryExpression) {
      IASTUnaryExpression unop = (IASTUnaryExpression) p;
      if (unop.getOperator() == IASTUnaryExpression.op_not)
        return buildSymbolicFormula(unop.getOperand(), !sign, function);
    }

    // atomic formula
    SymbolicFormula ssaFormula = makePredicate(p, sign, function, dummySSAMap);
    return smgr.uninstantiate(ssaFormula);
  }

  @Override
  public SymbolicFormula makeAnd(SymbolicFormula f, IASTExpression p, String function) throws UnrecognizedCCodeException {
    return smgr.makeAnd(f, buildSymbolicFormula(p, true, function));
  }
}
