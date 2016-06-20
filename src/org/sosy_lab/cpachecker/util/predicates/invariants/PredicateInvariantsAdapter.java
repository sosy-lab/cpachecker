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
package org.sosy_lab.cpachecker.util.predicates.invariants;

import static com.google.common.base.Verify.verifyNotNull;

import org.sosy_lab.common.collect.PersistentMap;
import org.sosy_lab.common.log.LogManager;
import org.sosy_lab.cpachecker.cfa.model.CFANode;
import org.sosy_lab.cpachecker.cfa.types.MachineModel;
import org.sosy_lab.cpachecker.cfa.types.c.CType;
import org.sosy_lab.cpachecker.core.algorithm.invariants.InvariantSupplier;
import org.sosy_lab.cpachecker.core.algorithm.invariants.InvariantSupplierWithoutContext;
import org.sosy_lab.cpachecker.util.predicates.pathformula.PathFormula;
import org.sosy_lab.cpachecker.util.predicates.pathformula.PathFormulaManager;
import org.sosy_lab.cpachecker.util.predicates.pathformula.ctoformula.CtoFormulaTypeHandler;
import org.sosy_lab.cpachecker.util.predicates.pathformula.pointeraliasing.CToFormulaConverterWithPointerAliasing;
import org.sosy_lab.cpachecker.util.predicates.pathformula.pointeraliasing.PointerTargetSet;
import org.sosy_lab.cpachecker.util.predicates.smt.FormulaManagerView;
import org.sosy_lab.cpachecker.util.predicates.smt.FormulaManagerView.FormulaTransformationVisitor;
import org.sosy_lab.cpachecker.util.predicates.smt.FunctionFormulaManagerView;
import org.sosy_lab.solver.api.BooleanFormula;
import org.sosy_lab.solver.api.Formula;

import javax.annotation.Nullable;

public class PredicateInvariantsAdapter implements InvariantSupplier {

  private final InvariantSupplierWithoutContext invSup;
  private final CtoFormulaTypeHandler typeConverter;

  public PredicateInvariantsAdapter(
      InvariantSupplierWithoutContext pInvSup, LogManager pLogger, MachineModel pMachineModel) {
    invSup = pInvSup;
    typeConverter = new CtoFormulaTypeHandler(pLogger, pMachineModel);
  }

  @Override
  public BooleanFormula getInvariantFor(
      CFANode pNode,
      FormulaManagerView pFmgr,
      PathFormulaManager pPfmgr,
      @Nullable PathFormula pContext) {

    BooleanFormula invariant = invSup.getInvariantFor(pNode, pFmgr, pPfmgr);

    if (pContext != null) {
      invariant =
          pFmgr.transformRecursively(
              new AddPointerInformationVisitor(pFmgr, pContext.getPointerTargetSet()), invariant);
    }

    verifyNotNull(invariant);

    return invariant;
  }

  private class AddPointerInformationVisitor extends FormulaTransformationVisitor {

    private final PointerTargetSet pts;
    private final FormulaManagerView fmgr;
    private final FunctionFormulaManagerView ffmgr;
    private final PersistentMap<String, CType> bases;

    protected AddPointerInformationVisitor(FormulaManagerView pFmgr, PointerTargetSet pPts) {
      super(pFmgr);
      fmgr = pFmgr;
      ffmgr = pFmgr.getFunctionFormulaManager();
      pts = pPts;
      bases = pts.getBases();
    }

    @Override
    public Formula visitFreeVariable(Formula atom, String varName) {
      if (bases.containsKey(varName)) {
        CType baseType = bases.get(varName);
        String uf = CToFormulaConverterWithPointerAliasing.getUFName(baseType);
        String baseVarName = PointerTargetSet.getBaseName(varName);
        return ffmgr.declareAndCallUF(
            uf,
            fmgr.getFormulaType(atom),
            fmgr.makeVariable(typeConverter.getPointerType(), baseVarName));
      }

      return atom;
    }
  }
}
