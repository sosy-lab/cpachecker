// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2026 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.util.predicates.pathformula.acsltoformula;

import org.sosy_lab.cpachecker.cfa.ast.acsl.AcslBuiltinLogicType;
import org.sosy_lab.cpachecker.cfa.ast.acsl.AcslCType;
import org.sosy_lab.cpachecker.cfa.ast.acsl.AcslFunctionType;
import org.sosy_lab.cpachecker.cfa.ast.acsl.AcslLogicType;
import org.sosy_lab.cpachecker.cfa.ast.acsl.AcslPointerType;
import org.sosy_lab.cpachecker.cfa.ast.acsl.AcslPolymorphicType;
import org.sosy_lab.cpachecker.cfa.ast.acsl.AcslPredicateType;
import org.sosy_lab.cpachecker.cfa.ast.acsl.AcslSetType;
import org.sosy_lab.cpachecker.cfa.ast.acsl.AcslType;
import org.sosy_lab.cpachecker.cfa.types.MachineModel;
import org.sosy_lab.cpachecker.cfa.types.c.CSimpleType;
import org.sosy_lab.cpachecker.cfa.types.c.CType;
import org.sosy_lab.cpachecker.util.predicates.pathformula.ctoformula.CtoFormulaConverter;
import org.sosy_lab.cpachecker.util.predicates.smt.FormulaManagerView;
import org.sosy_lab.java_smt.api.Formula;
import org.sosy_lab.java_smt.api.FormulaType;

public class AcslTypeHelper {
  private final MachineModel machineModel;

  @SuppressWarnings("unused")
  private final FormulaManagerView fmgr;

  private final CtoFormulaConverter ctoFormulaConverter;

  public AcslTypeHelper(
      MachineModel pMachineModel,
      FormulaManagerView pFmgr,
      CtoFormulaConverter pCtoFormulaConverter) {
    ctoFormulaConverter = pCtoFormulaConverter;
    machineModel = pMachineModel;
    fmgr = pFmgr;
  }

  public boolean isSigned(AcslType acslType) {

    return switch (acslType) {
      case AcslCType cType -> {
        CType underlyingCType = cType.getType().getCanonicalType();

        if (underlyingCType instanceof CSimpleType simpleType) {
          yield machineModel.isSigned(simpleType);
        } else {
          yield false; // choice was made to be parallel to handleBinaryExpression in
          // ExpressionToFormulaVisitor
        }
      }

      case AcslLogicType logType ->
          switch (logType) {
            case AcslBuiltinLogicType builtinType -> true; // all builtin logic types are signed
            case AcslPolymorphicType polyType ->
                throw new UnsupportedOperationException("Not yet implemented");
          };

      case AcslPointerType poinType -> false; // pointers are unsigned

      case AcslPredicateType predType ->
          throw new UnsupportedOperationException("Not yet implemented");
      case AcslSetType setType -> throw new UnsupportedOperationException("Not yet implemented");
      case AcslFunctionType funcType ->
          throw new UnsupportedOperationException("Not yet implemented");
    };
  }

  @SuppressWarnings("unused")
  public void handleDifferentTypes(Formula f1, Formula f2, AcslType commonType) {
    // TODO cast f1 and f2 to common type if necessary
    // find a way to return results
  }

  public FormulaType<?> acslTypeToFormulaType(AcslType acslType) {
    // TODO implement  more of the mapping
    return switch (acslType) {
      case AcslCType cType -> ctoFormulaConverter.getFormulaTypeFromType(cType.getType());
      case AcslFunctionType funcType ->
          throw new IllegalArgumentException(
              "This should not happen, AcslFunctionCallTerm should handle this");
      case AcslLogicType logType ->
          switch (logType) {
            case AcslBuiltinLogicType builtinType ->
                switch (builtinType) {
                  case BOOLEAN -> FormulaType.BooleanType;
                  case INTEGER -> FormulaType.IntegerType;
                  case REAL -> FormulaType.RationalType;
                  case ANY -> throw new UnsupportedOperationException("Not yet implemented");
                };
            case AcslPolymorphicType polyType ->
                throw new UnsupportedOperationException("Not yet implemented");
          };
      case AcslPointerType poinType ->
          throw new UnsupportedOperationException("Not yet implemented");
      case AcslPredicateType predType -> FormulaType.BooleanType;
      case AcslSetType setType -> throw new UnsupportedOperationException("Not yet implemented");
    };
  }
}
