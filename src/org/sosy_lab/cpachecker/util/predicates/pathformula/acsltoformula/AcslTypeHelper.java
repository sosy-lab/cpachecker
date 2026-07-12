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
import org.sosy_lab.java_smt.api.BitvectorFormula;
import org.sosy_lab.java_smt.api.Formula;
import org.sosy_lab.java_smt.api.FormulaType;

public class AcslTypeHelper {
  private final MachineModel machineModel;
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

  public Formula convertFormulaType(Formula f, AcslType commonType) {
    switch (commonType) {
      case AcslBuiltinLogicType.INTEGER -> {
        FormulaType<Formula> ftype = fmgr.getFormulaType(f);
        if (ftype.isIntegerType()) {
          return f;
        } else if (ftype.isBitvectorType()) {
          BitvectorFormula bitF = (BitvectorFormula) f;
          return fmgr.getBitvectorFormulaManager().toIntegerFormula(bitF, isSigned(commonType));
        } else {
          throw new UnsupportedOperationException("Not yet implemented");
        }
      }
      case AcslBuiltinLogicType.REAL ->
          throw new UnsupportedOperationException("Not yet implemented");
      case AcslBuiltinLogicType.BOOLEAN ->
          throw new UnsupportedOperationException("Not yet implemented");
      default -> throw new UnsupportedOperationException("Not yet implemented");
    }
  }

  public FormulaType<?> acslTypeToFormulaType(AcslType acslType) {
    return switch (acslType) {
      case AcslCType cType -> ctoFormulaConverter.getFormulaTypeFromType(cType.getType());
      case AcslFunctionType funcType ->
          throw new UnsupportedOperationException("Not yet implemented"); // TODO
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
          throw new UnsupportedOperationException("Not yet implemented"); // TODO
      case AcslPredicateType predType -> FormulaType.BooleanType;
      case AcslSetType setType -> throw new UnsupportedOperationException("Not yet implemented");
    };
  }

  protected record BinaryTermData(Boolean signed, Formula f1, Formula f2) {}

  protected BinaryTermData handleBinaryTerm(
      AcslType termType, AcslType operand1Type, AcslType operand2Type, Formula f1, Formula f2) {
    Formula convertedF1 = f1;
    Formula convertedF2 = f2;

    if (!fmgr.getFormulaType(f1).equals(fmgr.getFormulaType(f2))) {
      AcslType commonType = AcslType.mostGeneralType(operand1Type, operand2Type);
      convertedF1 = convertFormulaType(f1, commonType);
      convertedF2 = convertFormulaType(f1, commonType);
    }

    boolean signed = true;

    // Bitvector case: signed is important
    if (convertedF1 instanceof BitvectorFormula && convertedF2 instanceof BitvectorFormula) {
      signed = isSigned(termType);
    }

    return new BinaryTermData(signed, convertedF1, convertedF2);
  }
}
