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
package org.sosy_lab.cpachecker.util.invariants.templates.manager;

import java.util.Arrays;
import java.util.List;

import org.sosy_lab.cpachecker.util.invariants.templates.NonTemplate;
import org.sosy_lab.cpachecker.util.invariants.templates.TemplateFormulaList;
import org.sosy_lab.cpachecker.util.invariants.templates.TemplateSumList;
import org.sosy_lab.cpachecker.util.invariants.templates.TemplateTerm;
import org.sosy_lab.cpachecker.util.invariants.templates.TemplateUIF;
import org.sosy_lab.cpachecker.util.predicates.interfaces.Formula;
import org.sosy_lab.cpachecker.util.predicates.interfaces.FormulaType;
import org.sosy_lab.cpachecker.util.predicates.interfaces.FunctionFormulaManager;
import org.sosy_lab.cpachecker.util.predicates.interfaces.FunctionFormulaType;


public class TemplateFunctionFormulaManager implements FunctionFormulaManager {

  public TemplateFunctionFormulaManager(TemplateFormulaManager manager) {
  }

//  @Override
//  public BooleanFormula makeUIF(String pName, FormulaList pArgs) {
//    BooleanFormula F = null;
//    try {
//      TemplateFormulaList FL = (TemplateFormulaList) pArgs;
//      TemplateSumList SL =  new TemplateSumList(FL);
//      TemplateUIF U = new TemplateUIF(pName, SL);
//      TemplateTerm T = new TemplateTerm();
//      T.setUIF(U);
//      F = T;
//    } catch (ClassCastException e) {
//      System.err.println(e.getMessage());
//      F = new NonTemplate();
//    }
//    return F;
//  }
//
//  @Override
//  public BooleanFormula makeUIF(String pName, FormulaList pArgs, int pIdx)
//  {
//    BooleanFormula F = null;
//    try {
//      TemplateFormulaList FL = (TemplateFormulaList) pArgs;
//      TemplateSumList SL =  new TemplateSumList(FL);
//      TemplateUIF U = new TemplateUIF(pName, SL, pIdx);
//      TemplateTerm T = new TemplateTerm();
//      T.setUIF(U);
//      F = T;
//    } catch (ClassCastException e) {
//      System.err.println(e.getMessage());
//      F = new NonTemplate();
//    }
//    return F;
//  }
  @Override
  public <T extends Formula> FunctionFormulaType<T> createFunction(String pName, FormulaType<T> pReturnType,
      List<FormulaType<?>> pArgs) {
    return new TemplateFunctionFormulaTypeImpl<>(pName, pReturnType, pArgs);
  }

  @Override
  public <T extends Formula> FunctionFormulaType<T> createFunction(
      String pName,
      FormulaType<T> pReturnType,
      FormulaType<?>... pArgs) {

    return createFunction(pName, pReturnType, Arrays.asList(pArgs));
  }

  @SuppressWarnings("unchecked")
  @Override
  public <T extends Formula> T
    createUninterpretedFunctionCall(
        FunctionFormulaType<T> pFuncType, List<? extends Formula> pArgs) {
    TemplateFunctionFormulaTypeImpl<T> funcType = (TemplateFunctionFormulaTypeImpl<T>) pFuncType;
    Formula F = null;
    try {
      TemplateFormulaList FL = new TemplateFormulaList(pArgs);
      TemplateSumList SL =  new TemplateSumList(FL);
      TemplateUIF U = new TemplateUIF(funcType.getName(), funcType.getReturnType(), SL);
      TemplateTerm T = new TemplateTerm(funcType.getReturnType());
      T.setUIF(U);
      F = T;
    } catch (ClassCastException e) {
      System.err.println(e.getMessage());
      F = new NonTemplate(funcType.getReturnType());
    }
    return (T)F;
  }

  @Override
  public boolean isUninterpretedFunctionCall(FunctionFormulaType<?> pFuncType, Formula pF) {

    return pF instanceof TemplateTerm && ((TemplateTerm)pF).hasUIF();
  }

}
