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

public class AcslTypeHelper {
  private final MachineModel machineModel;

  public AcslTypeHelper(MachineModel pMachineModel) {
    machineModel = pMachineModel;
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
}
