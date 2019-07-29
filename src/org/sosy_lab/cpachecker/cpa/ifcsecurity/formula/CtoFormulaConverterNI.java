/*
 *  CPAchecker is a tool for configurable software verification.
 *  This file is part of CPAchecker.
 *
 *  Copyright (C) 2007-2016  Dirk Beyer
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
package org.sosy_lab.cpachecker.cpa.ifcsecurity.formula;

import static com.google.common.base.Preconditions.checkArgument;

import java.util.Optional;
import java.util.logging.Level;
import org.sosy_lab.common.ShutdownNotifier;
import org.sosy_lab.common.log.LogManager;
import org.sosy_lab.cpachecker.cfa.types.MachineModel;
import org.sosy_lab.cpachecker.cfa.types.c.CType;
import org.sosy_lab.cpachecker.core.AnalysisDirection;
import org.sosy_lab.cpachecker.util.predicates.pathformula.SSAMap.SSAMapBuilder;
import org.sosy_lab.cpachecker.util.predicates.pathformula.ctoformula.CtoFormulaConverter;
import org.sosy_lab.cpachecker.util.predicates.pathformula.ctoformula.CtoFormulaTypeHandler;
import org.sosy_lab.cpachecker.util.predicates.pathformula.ctoformula.FormulaEncodingOptions;
import org.sosy_lab.cpachecker.util.predicates.pathformula.pointeraliasing.PointerTargetSet;
import org.sosy_lab.cpachecker.util.predicates.smt.FormulaManagerView;
import org.sosy_lab.cpachecker.util.variableclassification.VariableClassification;
import org.sosy_lab.java_smt.api.BooleanFormula;
import org.sosy_lab.java_smt.api.Formula;
import org.sosy_lab.java_smt.api.FormulaType;

public class CtoFormulaConverterNI extends CtoFormulaConverter{

  // private FormulaEncodingOptions options;
  private int tag=0;

  public CtoFormulaConverterNI(FormulaEncodingOptions pOptions, FormulaManagerView pFmgr,
      MachineModel pMachineModel, Optional<VariableClassification> pVariableClassification,
      LogManager pLogger, ShutdownNotifier pShutdownNotifier, CtoFormulaTypeHandler pTypeHandler,
      AnalysisDirection pDirection) {
    super(pOptions, pFmgr, pMachineModel, pVariableClassification, pLogger, pShutdownNotifier,
        pTypeHandler, pDirection);
    // this.options=pOptions;
  }

  @Override
  protected Formula makeVariable(String name, CType type, SSAMapBuilder ssa) {
    logger.log(Level.FINE, "MAKE VARIABLE");
    int useIndex = getIndex(name, type, ssa);
    return makeVariable(this.getFormulaTypeFromCType(type), name, useIndex);
  }

  @Override
  protected Formula makeFreshVariable(String name, CType type, SSAMapBuilder ssa) {
    logger.log(Level.FINE, "MAKE FRESH");
    int useIndex;

    if (direction == AnalysisDirection.BACKWARD) {
      useIndex = getIndex(name, type, ssa);
    } else {
      useIndex = makeFreshIndex(name, type, ssa);
    }

    Formula result = makeVariable(this.getFormulaTypeFromCType(type), name, useIndex);

    if (direction == AnalysisDirection.BACKWARD) {
      makeFreshIndex(name, type, ssa);
    }

    return result;
  }

  private Formula makeVariable(FormulaType<?> formulaType, String name, int idx) {
    return fmgr.makeVariable(formulaType, makeName(name, tag, idx));
  }

  private static final String INDEX_SEPARATOR = "@";

  static String makeName(String name, int tag, int idx) {
    if (idx < 0) {
      return name;
    }
    return name + INDEX_SEPARATOR + tag + INDEX_SEPARATOR + idx;
  }

  @Override
  public BooleanFormula makeSsaUpdateTerm(
      final String variableName,
      final CType variableType,
      final int oldIndex,
      final int newIndex,
      final PointerTargetSet pts)
      throws InterruptedException {
    logger.log(Level.FINE, "makeSsaUpdateTerm");
    checkArgument(oldIndex > 0 && newIndex > oldIndex);

    final FormulaType<?> variableFormulaType = getFormulaTypeFromCType(variableType);
    final Formula oldVariable = makeVariable(variableFormulaType, variableName, oldIndex);
    final Formula newVariable = makeVariable(variableFormulaType, variableName, newIndex);

    return fmgr.assignment(newVariable, oldVariable);
  }

}
