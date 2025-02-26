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
import org.sosy_lab.cpachecker.cpa.invariants.formula.Multiply;
import org.sosy_lab.cpachecker.cpa.invariants.formula.NumeralFormula;
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

  private static final String LLONG_MIN_LITERAL = "9223372036854775808";

  private static final String INT_MIN_LITERAL = "2147483648";

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
    if (bitSize == 32 && value.equals(new BigInteger(INT_MIN_LITERAL))) {
      return value.add(BigInteger.ONE).subtract(BigInteger.ONE);
    }
    if (bitSize == 64 && value.equals(new BigInteger(LLONG_MIN_LITERAL))) {
      return value.add(BigInteger.ONE).subtract(BigInteger.ONE);
    }
    return value;
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
      case BV_AND:
        return BinaryAnd.of(fmgr.visit(pArgs.get(0), this), fmgr.visit(pArgs.get(1), this));
      case BV_NOT:
        return BinaryNot.of(fmgr.visit(pArgs.get(0), this));
      case BV_OR:
        return BinaryOr.of(fmgr.visit(pArgs.get(0), this), fmgr.visit(pArgs.get(1), this));
      case BV_XOR:
        return BinaryXor.of(fmgr.visit(pArgs.get(0), this), fmgr.visit(pArgs.get(1), this));
      case BV_UDIV:
      case FP_DIV:
      case DIV:
        return Divide.of(fmgr.visit(pArgs.get(0), this), fmgr.visit(pArgs.get(1), this));
      case BV_MUL:
      case FP_MUL:
      case MUL:
        return Multiply.of(fmgr.visit(pArgs.get(0), this), fmgr.visit(pArgs.get(1), this));

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
