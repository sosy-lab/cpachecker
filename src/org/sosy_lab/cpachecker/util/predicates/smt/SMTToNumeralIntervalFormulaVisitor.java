// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2025 Sara Ruckstuhl <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.util.predicates.smt;

import java.math.BigInteger;
import java.util.List;
import java.util.Map;
import org.sosy_lab.cpachecker.cfa.types.MachineModel;
import org.sosy_lab.cpachecker.cfa.types.c.CType;
import org.sosy_lab.cpachecker.cpa.invariants.BitVectorInfo;
import org.sosy_lab.cpachecker.cpa.invariants.BitVectorInterval;
import org.sosy_lab.cpachecker.cpa.invariants.CompoundBitVectorInterval;
import org.sosy_lab.cpachecker.cpa.invariants.CompoundInterval;
import org.sosy_lab.cpachecker.cpa.invariants.TypeInfo;
import org.sosy_lab.cpachecker.cpa.invariants.formula.Add;
import org.sosy_lab.cpachecker.cpa.invariants.formula.BinaryAnd;
import org.sosy_lab.cpachecker.cpa.invariants.formula.BinaryNot;
import org.sosy_lab.cpachecker.cpa.invariants.formula.BinaryOr;
import org.sosy_lab.cpachecker.cpa.invariants.formula.BinaryXor;
import org.sosy_lab.cpachecker.cpa.invariants.formula.Constant;
import org.sosy_lab.cpachecker.cpa.invariants.formula.Divide;
import org.sosy_lab.cpachecker.cpa.invariants.formula.IfThenElse;
import org.sosy_lab.cpachecker.cpa.invariants.formula.Modulo;
import org.sosy_lab.cpachecker.cpa.invariants.formula.Multiply;
import org.sosy_lab.cpachecker.cpa.invariants.formula.NumeralFormula;
import org.sosy_lab.cpachecker.cpa.invariants.formula.ShiftLeft;
import org.sosy_lab.cpachecker.cpa.invariants.formula.ShiftRight;
import org.sosy_lab.cpachecker.cpa.invariants.formula.Variable;
import org.sosy_lab.cpachecker.util.states.MemoryLocation;
import org.sosy_lab.java_smt.api.BooleanFormula;
import org.sosy_lab.java_smt.api.Formula;
import org.sosy_lab.java_smt.api.FormulaType;
import org.sosy_lab.java_smt.api.FunctionDeclaration;
import org.sosy_lab.java_smt.api.FunctionDeclarationKind;
import org.sosy_lab.java_smt.api.QuantifiedFormulaManager.Quantifier;
import org.sosy_lab.java_smt.api.visitors.FormulaVisitor;

public class SMTToNumeralIntervalFormulaVisitor
    implements FormulaVisitor<NumeralFormula<CompoundInterval>> {

  private final FormulaManagerView fmgr;
  private final Map<MemoryLocation, CType> variableTypes;
  private final MachineModel machineModel;

  public SMTToNumeralIntervalFormulaVisitor(
      FormulaManagerView pFmgr,
      Map<MemoryLocation, CType> pVariableTypes,
      MachineModel pMachineModel) {
    machineModel = pMachineModel;
    variableTypes = pVariableTypes;
    fmgr = pFmgr;
  }

  @Override
  public NumeralFormula<CompoundInterval> visitBoundVariable(Formula pF, int pValue) {
    CType varType = variableTypes.get(MemoryLocation.fromQualifiedName(pF.toString()));
    TypeInfo typeInfo = TypeInfo.from(machineModel, varType);
    return Variable.of(typeInfo, MemoryLocation.fromQualifiedName(pF.toString()));
  }

  private BigInteger adjustForOverflow(BigInteger value, int bitSize) {
    BigInteger twoPowBits = BigInteger.ONE.shiftLeft(bitSize);
    BigInteger maxSigned = twoPowBits.shiftRight(1).subtract(BigInteger.ONE);
    BigInteger minSigned = maxSigned.negate().subtract(BigInteger.ONE);

    BigInteger wrapped = value;
    if (value.compareTo(minSigned) < 0 || value.compareTo(maxSigned) > 0) {
      wrapped = value.and(twoPowBits.subtract(BigInteger.ONE));
      if (wrapped.testBit(bitSize - 1)) {
        wrapped = wrapped.subtract(twoPowBits);
      }
    }

    return wrapped;
  }

  @Override
  public NumeralFormula<CompoundInterval> visitConstant(Formula pF, Object pValue) {
    FormulaType<?> type = fmgr.getFormulaType(pF);

    if (type.isBitvectorType()) {
      BigInteger value = (BigInteger) pValue;
      final int bitSize = ((FormulaType.BitvectorType) type).getSize();
      value = adjustForOverflow(value, bitSize);
      BitVectorInterval bvInterval =
          BitVectorInterval.of(BitVectorInfo.from(bitSize, true), value, value);
      CompoundBitVectorInterval compoundInterval = CompoundBitVectorInterval.of(bvInterval);
      return Constant.of(BitVectorInfo.from(bitSize, true), compoundInterval);
    }
    return null;
  }

  @Override
  public NumeralFormula<CompoundInterval> visitFreeVariable(Formula pF, String pName) {
    MemoryLocation memoryLocation = MemoryLocation.fromQualifiedName(pName);
    CType varType = variableTypes.get(memoryLocation);
    TypeInfo typeInfo = TypeInfo.from(machineModel, varType);
    return Variable.of(typeInfo, memoryLocation);
  }

  @Override
  public NumeralFormula<CompoundInterval> visitFunction(
      Formula pF, List<Formula> pArgs, FunctionDeclaration<?> pFunctionDeclaration) {
    FunctionDeclarationKind kind = pFunctionDeclaration.getKind();

    switch (kind) {
      case BV_ADD:
      case FP_ADD:
      case ADD:
        return Add.of(fmgr.visit(pArgs.get(0), this), fmgr.visit(pArgs.get(1), this));
      case BV_SUB:
      case BV_NEG:
      case FP_SUB:
      case FP_NEG:
      case UMINUS:
      case SUB:
        return Add.of(
            fmgr.visit(pArgs.get(0), this),
            Multiply.of(fmgr.visit(pArgs.get(1), this), this.visitConstant(pF, -1)));
      case BV_AND:
        return BinaryAnd.of(fmgr.visit(pArgs.get(0), this), fmgr.visit(pArgs.get(1), this));
      case BV_NOT:
      case NOT:
        return BinaryNot.of(fmgr.visit(pArgs.get(0), this));
      case BV_OR:
      case OR:
        return BinaryOr.of(fmgr.visit(pArgs.get(0), this), fmgr.visit(pArgs.get(1), this));
      case XOR:
      case BV_XOR:
        return BinaryXor.of(fmgr.visit(pArgs.get(0), this), fmgr.visit(pArgs.get(1), this));
      case BV_UDIV:
      case BV_SDIV:
      case FP_DIV:
      case DIV:
        return Divide.of(fmgr.visit(pArgs.get(0), this), fmgr.visit(pArgs.get(1), this));
      case BV_MUL:
      case FP_MUL:
      case MUL:
        return Multiply.of(fmgr.visit(pArgs.get(0), this), fmgr.visit(pArgs.get(1), this));
      case BV_LSHR:
      case BV_ASHR:
        return ShiftRight.of(fmgr.visit(pArgs.get(0), this), fmgr.visit(pArgs.get(1), this));
      case BV_SHL:
        return ShiftLeft.of(fmgr.visit(pArgs.get(0), this), fmgr.visit(pArgs.get(1), this));
      case BV_SREM:
      case BV_UREM:
      case MODULO:
        return Modulo.of(fmgr.visit(pArgs.get(0), this), fmgr.visit(pArgs.get(1), this));
      case ITE:
        return IfThenElse.of(
            fmgr.visit(pArgs.get(0), new SMTToBooleanIntervalFormulaVisitor(fmgr, this)),
            fmgr.visit(pArgs.get(1), this),
            fmgr.visit(pArgs.get(2), this));
      default:
        break;
    }
    return null;
  }

  @Override
  public NumeralFormula<CompoundInterval> visitQuantifier(
      BooleanFormula pArg0, Quantifier pArg1, List<Formula> pArg2, BooleanFormula pArg3) {
    throw new UnsupportedOperationException("Unimplemented method 'visitQuantifier'");
  }
}
