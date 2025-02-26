package org.sosy_lab.cpachecker.util.predicates.smt;

import java.math.BigInteger;
import java.util.List;
import java.util.Map;
import org.sosy_lab.common.collect.PathCopyingPersistentTreeMap;
import org.sosy_lab.common.collect.PersistentMap;
import org.sosy_lab.cpachecker.cfa.types.c.CType;
import org.sosy_lab.cpachecker.cpa.value.ValueAnalysisState.ValueAndType;
import org.sosy_lab.cpachecker.cpa.value.type.BooleanValue;
import org.sosy_lab.cpachecker.cpa.value.type.NumericValue;
import org.sosy_lab.cpachecker.util.states.MemoryLocation;
import org.sosy_lab.java_smt.api.BooleanFormula;
import org.sosy_lab.java_smt.api.Formula;
import org.sosy_lab.java_smt.api.FormulaType;
import org.sosy_lab.java_smt.api.FunctionDeclaration;
import org.sosy_lab.java_smt.api.FunctionDeclarationKind;
import org.sosy_lab.java_smt.api.QuantifiedFormulaManager.Quantifier;
import org.sosy_lab.java_smt.api.visitors.FormulaVisitor;

public class FormulaToValueVisitor implements FormulaVisitor<Boolean> {

  private static final String LLONG_MIN_LITERAL = "9223372036854775808";

  private static final String INT_MIN_LITERAL = "2147483648";

  private final Map<MemoryLocation, CType> variableTypes;
  PersistentMap<MemoryLocation, ValueAndType> constantsMap = PathCopyingPersistentTreeMap.of();
  private final FormulaManagerView fmgr;
  MemoryLocation currentMemoryLocation;

  public FormulaToValueVisitor(
      Map<MemoryLocation, CType> pVariableTypes, FormulaManagerView pFmgr) {
    fmgr = pFmgr;
    variableTypes = pVariableTypes;
  }

  public PersistentMap<MemoryLocation, ValueAndType> getConstantsMap() {
    return constantsMap;
  }

  @Override
  public Boolean visitBoundVariable(Formula pArg0, int pArg1) {
    throw new UnsupportedOperationException("Unimplemented method 'visitBoundVariable'");
  }

  @Override
  public Boolean visitConstant(Formula pF, Object pValue) {
    FormulaType<?> type = fmgr.getFormulaType(pF);
    if (type.isBitvectorType()) {
      BigInteger value = (BigInteger) pValue;
      final int bitSize = ((FormulaType.BitvectorType) type).getSize();
      value = adjustForOverflow(value, bitSize);
      if (currentMemoryLocation != null) {
        constantsMap =
            constantsMap.putAndCopy(
                currentMemoryLocation,
                new ValueAndType(
                    new NumericValue(value), variableTypes.get(currentMemoryLocation)));
      }
      return true;
    } else if (pValue instanceof Boolean) {
      if (currentMemoryLocation != null) {
        constantsMap =
            constantsMap.putAndCopy(
                currentMemoryLocation,
                new ValueAndType(
                    BooleanValue.valueOf((boolean) pValue),
                    variableTypes.get(currentMemoryLocation)));
      }
    }
    // handle floating point values
    return false;
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
  public Boolean visitFreeVariable(Formula pArg0, String pArg1) {
    throw new UnsupportedOperationException("Unimplemented method 'visitFreeVariable'");
  }

  @Override
  public Boolean visitFunction(
      Formula pF, List<Formula> pArgs, FunctionDeclaration<?> pFunctionDeclaration) {
    FunctionDeclarationKind kind = pFunctionDeclaration.getKind();
    switch (kind) {
      case AND:
        {
          for (Formula arg : pArgs) {
            fmgr.visit(arg, this);
          }
          return true;
        }
      case EQ:
        {
          String varName = pArgs.get(0).toString();
          currentMemoryLocation = MemoryLocation.fromQualifiedName(varName);
          fmgr.visit(pArgs.get(1), this);
          return true;
        }
      default:
        throw new UnsupportedOperationException("Unsupported function: " + kind);
    }
  }

  @Override
  public Boolean visitQuantifier(
      BooleanFormula pArg0, Quantifier pArg1, List<Formula> pArg2, BooleanFormula pArg3) {
    throw new UnsupportedOperationException("Unimplemented method 'visitQuantifier'");
  }
}
