// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2025 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.util.predicates.pathformula;

import java.io.PrintStream;
import org.sosy_lab.cpachecker.cfa.ast.c.CIdExpression;
import org.sosy_lab.cpachecker.cfa.model.CFAEdge;
import org.sosy_lab.cpachecker.cfa.types.Type;
import org.sosy_lab.cpachecker.cfa.types.c.CType;
import org.sosy_lab.cpachecker.exceptions.UnrecognizedCodeException;
import org.sosy_lab.cpachecker.util.predicates.pathformula.SSAMap.SSAMapBuilder;
import org.sosy_lab.cpachecker.util.predicates.pathformula.SSAMapMerger.MergeResult;
import org.sosy_lab.cpachecker.util.predicates.pathformula.pointeraliasing.PointerTargetSet;
import org.sosy_lab.java_smt.api.BooleanFormula;
import org.sosy_lab.java_smt.api.Formula;
import org.sosy_lab.java_smt.api.FormulaType;

public abstract class LanguageToSmtConverter<T extends Type> extends SSAIndexProvider<T> {

  public abstract FormulaType<?> getFormulaTypeFromType(T type);

  public abstract PathFormula makeAnd(
      PathFormula pOldFormula, CFAEdge pEdge, ErrorConditions pErrorConditions)
      throws UnrecognizedCodeException, InterruptedException;

  public abstract MergeResult<PointerTargetSet> mergePointerTargetSets(
      PointerTargetSet pPts1, PointerTargetSet pPts2, SSAMapBuilder pNewSSA)
      throws InterruptedException;

  public abstract BooleanFormula makeSsaUpdateTerm(
      String pSymbolName, Type pSymbolType, int pOldIndex, int pNewIndex, PointerTargetSet pOldPts)
      throws InterruptedException;

  public abstract Formula makeFormulaForVariable(
      SSAMap pSsa, PointerTargetSet pPointerTargetSet, String pVarName, CType pType);

  public abstract Formula makeFormulaForUninstantiatedVariable(
      String pVarName, CType pType, PointerTargetSet pContextPTS, boolean pForcePointerDereference);

  public abstract Formula buildTermFromPathFormula(
      PathFormula pFormula, CIdExpression pExpr, CFAEdge pEdge) throws UnrecognizedCodeException;

  public abstract void printStatistics(PrintStream pOut);
}
