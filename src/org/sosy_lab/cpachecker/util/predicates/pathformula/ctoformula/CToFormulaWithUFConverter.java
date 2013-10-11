/*
 *  CPAchecker is a tool for configurable software verification.
 *  This file is part of CPAchecker.
 *
 *  Copyright (C) 2007-2013  Dirk Beyer
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
package org.sosy_lab.cpachecker.util.predicates.pathformula.ctoformula;

import java.util.List;

import org.sosy_lab.common.LogManager;
import org.sosy_lab.common.Pair;
import org.sosy_lab.common.configuration.Configuration;
import org.sosy_lab.common.configuration.InvalidConfigurationException;
import org.sosy_lab.cpachecker.cfa.model.CFAEdge;
import org.sosy_lab.cpachecker.cfa.types.MachineModel;
import org.sosy_lab.cpachecker.cfa.types.c.CArrayType;
import org.sosy_lab.cpachecker.cfa.types.c.CComplexType.ComplexTypeKind;
import org.sosy_lab.cpachecker.cfa.types.c.CCompositeType;
import org.sosy_lab.cpachecker.cfa.types.c.CCompositeType.CCompositeTypeMemberDeclaration;
import org.sosy_lab.cpachecker.cfa.types.c.CElaboratedType;
import org.sosy_lab.cpachecker.cfa.types.c.CType;
import org.sosy_lab.cpachecker.exceptions.UnrecognizedCCodeException;
import org.sosy_lab.cpachecker.util.predicates.interfaces.Formula;
import org.sosy_lab.cpachecker.util.predicates.interfaces.FormulaType;
import org.sosy_lab.cpachecker.util.predicates.interfaces.view.FormulaManagerView;
import org.sosy_lab.cpachecker.util.predicates.interfaces.view.RationalFormulaManagerView;
import org.sosy_lab.cpachecker.util.predicates.pathformula.SSAMap.SSAMapBuilder;
import org.sosy_lab.cpachecker.util.predicates.pathformula.Variable;
import org.sosy_lab.cpachecker.util.predicates.pathformula.withUF.PointerTargetSet;
import org.sosy_lab.cpachecker.util.predicates.pathformula.withUF.PointerTargetSet.PointerTargetSetBuilder;

import com.google.common.collect.ImmutableList;

public class CToFormulaWithUFConverter extends CtoFormulaConverter {

  CToFormulaWithUFConverter(Configuration config,
                            FormulaManagerView formulaManagerView,
                            MachineModel machineModel,
                            LogManager logger)
  throws InvalidConfigurationException {
    super(config, formulaManagerView, machineModel, logger);
    rfmgr = formulaManagerView.getRationalFormulaManager();
  }

  private static String getUFName(final CType type) {
    return PointerTargetSet.cTypeToString(type).replace(' ', '_');
  }

  private FormulaType<?> getFormulaTypeForCType(final CType type, final PointerTargetSetBuilder pts) {
    final int size = pts.getSize(type);
    final int bitsPerByte = machineModel.getSizeofCharInBits();
    return efmgr.getFormulaType(size * bitsPerByte);
  }

  @Override
  Formula makeConstant(String name, CType type, SSAMapBuilder ssa) {
    return fmgr.makeVariable(getFormulaTypeFromCType(type), name);
  }

  @Override
  Formula makeConstant(Variable var, SSAMapBuilder ssa) {
    return makeConstant(var.getName(), var.getType(), ssa);
  }

  Formula makeDereferece(CType type,
                         final Formula address,
                         final SSAMapBuilder ssa,
                         final PointerTargetSetBuilder pts) {
    type = PointerTargetSet.simplifyType(type);
    final String ufName = getUFName(type);
    final int index = ssa.getIndex(ufName);
    final FormulaType<?> returnType = getFormulaTypeForCType(type, pts);
    return ffmgr.createFuncAndCall(ufName, index, returnType, ImmutableList.of(address));
  }

  void addSharingConstraints(final CFAEdge cfaEdge,
                             final Formula address,
                             final Variable base,
                             final List<Pair<CCompositeType, String>> fields,
                             final SSAMapBuilder ssa,
                             final Constraints constraints,
                             final PointerTargetSetBuilder pts) throws UnrecognizedCCodeException {
    final CType baseType = PointerTargetSet.simplifyType(base.getType());
    if (baseType instanceof CArrayType) {
      throw new UnrecognizedCCodeException("Array access can't be encoded as a varaible", cfaEdge);
    } else if (baseType instanceof CCompositeType) {
      final CCompositeType compositeType = (CCompositeType) baseType;
      assert compositeType.getKind() != ComplexTypeKind.ENUM : "Enums are not composite: " + compositeType;
      int offset = 0;
      for (final CCompositeTypeMemberDeclaration memberDeclaration : compositeType.getMembers()) {
        final String fieldName = memberDeclaration.getName();
        final Variable newBase = Variable.create(base + BaseVisitor.NAME_SEPARATOR + fieldName,
                                                 memberDeclaration.getType());
        if (ssa.getIndex(newBase.getName()) > 1) {
          fields.add(Pair.of(compositeType, fieldName));
          addSharingConstraints(cfaEdge,
                                fmgr.makePlus(address, fmgr.makeNumber(pts.getPointerType(), offset)),
                                newBase,
                                fields,
                                ssa,
                                constraints,
                                pts);
        }
        if (compositeType.getKind() == ComplexTypeKind.STRUCT) {
          offset += pts.getSize(memberDeclaration.getType());
        }
      }
    } else {
      constraints.addConstraint(fmgr.makeEqual(makeDereferece(baseType, address, ssa, pts),
                                               makeVariable(base, ssa)));
    }
  }

  boolean isCompositeType(CType type) {
    type = PointerTargetSet.simplifyType(type);
    assert !(type instanceof CElaboratedType) : "Unresolved elaborated type";
    assert !(type instanceof CCompositeType) || ((CCompositeType) type).getKind() == ComplexTypeKind.STRUCT ||
                                                ((CCompositeType) type).getKind() == ComplexTypeKind.UNION :
           "Enums are not composite";
    return type instanceof CArrayType || type instanceof CCompositeType;
  }

  private final RationalFormulaManagerView rfmgr;
}
