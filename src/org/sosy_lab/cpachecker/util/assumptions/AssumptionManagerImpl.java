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

import org.sosy_lab.cpachecker.cfa.ast.IASTBinaryExpression;
import org.eclipse.cdt.core.dom.ast.IASTDeclSpecifier;
import org.eclipse.cdt.core.dom.ast.IASTDeclarator;
import org.eclipse.cdt.core.dom.ast.IASTEnumerationSpecifier;
import org.eclipse.cdt.core.dom.ast.IASTExpression;
import org.eclipse.cdt.core.dom.ast.IASTIdExpression;
import org.eclipse.cdt.core.dom.ast.IASTNode;
import org.eclipse.cdt.core.dom.ast.IASTSimpleDeclaration;
import org.eclipse.cdt.core.dom.ast.IASTUnaryExpression;
import org.sosy_lab.common.LogManager;
import org.sosy_lab.common.Pair;
import org.sosy_lab.common.configuration.Configuration;
import org.sosy_lab.common.configuration.InvalidConfigurationException;
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
  public static class DummySSAMap
  extends SSAMapBuilder
  {
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

    @Override
    public void setIndex(String pName, FormulaList pArgs, int pIdx) {
      assert(false);
    }

    @Override
    public void setIndex(String pVariable, int pIdx) {
      super.setIndex(pVariable, pIdx);
    }

    @Override
    public SSAMap build() {
      return super.build();
    }
    
    @Override
    public String toString() {
      // TODO Auto-generated method stub
      return super.toString();
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

  /**
   * Return the singleton instance for this class.
   * {@link #createInstance()} has to be called before at least once.
   */
  public static FormulaManager getFormulaManager() {
    assert fmgr != null;

    return fmgr;
  }

  public AssumptionManagerImpl(Configuration pConfig, LogManager pLogger) throws InvalidConfigurationException {
    super(pConfig, createFormulaManager(pConfig, pLogger), pLogger);
  }
//  private final SSAMapBuilder dummySSAMap = new DummySSAMap();

  private Pair<Formula, SSAMapBuilder> buildFormula(IASTExpression p, boolean sign, String function, SSAMapBuilder pSSAMap) throws UnrecognizedCCodeException
  {
    // first, check whether we have &&, ||, or !
    if (p instanceof IASTBinaryExpression) {
      IASTBinaryExpression binop = (IASTBinaryExpression) p;
      if(binop.getOperand1() instanceof IASTIdExpression){
        pSSAMap.setIndex(super.scoped(binop.getOperand1().getRawSignature(), function), 1);
      }
      if(binop.getOperand2() instanceof IASTIdExpression){
        pSSAMap.setIndex(super.scoped(binop.getOperand2().getRawSignature(), function), 1);
      }
      switch (binop.getOperator()) {
      case IASTBinaryExpression.op_logicalAnd:
        if (sign){
          Formula symbFor = fmgr.makeAnd(
              buildFormula(binop.getOperand1(), true, function, pSSAMap).getFirst(),
              buildFormula(binop.getOperand2(), true, function, pSSAMap).getFirst());
        return Pair.of(symbFor, pSSAMap);
        }
        else{
          Formula symbFor = fmgr.makeOr(
              buildFormula(binop.getOperand1(), false, function, pSSAMap).getFirst(),
              buildFormula(binop.getOperand2(), false, function, pSSAMap).getFirst());
          return Pair.of(symbFor, pSSAMap);
        }
      case IASTBinaryExpression.op_logicalOr:
        if (sign){
          Formula symbFor = fmgr.makeOr(
              buildFormula(binop.getOperand1(), true, function, pSSAMap).getFirst(),
              buildFormula(binop.getOperand2(), true, function, pSSAMap).getFirst());
          return Pair.of(symbFor, pSSAMap);
        }
        else{
          Formula symbFor = fmgr.makeAnd(
              buildFormula(binop.getOperand1(), false, function, pSSAMap).getFirst(),
              buildFormula(binop.getOperand2(), false, function, pSSAMap).getFirst());
          return Pair.of(symbFor, pSSAMap);
          }
      }
    } else if (p instanceof IASTUnaryExpression) {
      IASTUnaryExpression unop = (IASTUnaryExpression) p;
      if (unop.getOperator() == IASTUnaryExpression.op_not)
        return buildFormula(unop.getOperand(), !sign, function, pSSAMap);
    }

    //    super.setNamespace(pEdge.getSuccessor().getFunctionName());
    // atomic formula
    Formula ssaFormula = makePredicate(p, sign, function, pSSAMap);
    return Pair.of(ssaFormula, pSSAMap);
  }

  @Override
  public Formula makeAnd(Formula f, IASTNode p, String function) throws UnrecognizedCCodeException {
    SSAMapBuilder mapBuilder = new DummySSAMap();
    
    if(p instanceof IASTExpression){
      
      return fmgr.makeAnd(f, buildFormula((IASTExpression)p, true, function, mapBuilder).getFirst());
    }
    else if(p instanceof IASTSimpleDeclaration){
      IASTSimpleDeclaration decl = (IASTSimpleDeclaration)p;
      IASTDeclarator[] decls = decl.getDeclarators();
      IASTDeclSpecifier spec = decl.getDeclSpecifier();

      boolean isGlobal = ArithmeticOverflowAssumptionBuilder.isDeclGlobal;

      if (spec instanceof IASTEnumerationSpecifier) {
        // extract the fields, and add them as global variables
        assert(isGlobal);
        IASTEnumerationSpecifier.IASTEnumerator[] enums =
          ((IASTEnumerationSpecifier)spec).getEnumerators();
        for (IASTEnumerationSpecifier.IASTEnumerator e : enums) {
          String var = e.getName().getRawSignature();
          super.addToGlobalVars(var);
        }
      }
      for (IASTDeclarator d : decls) {
        String var = d.getName().getRawSignature();
        if (isGlobal) {
          super.addToGlobalVars(var);
        }
      }
    }
    return f;
  }
  
  @Override
  public Formula makeTrue() {
    return fmgr.makeTrue();
  }
}
