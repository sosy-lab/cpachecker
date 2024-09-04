package org.sosy_lab.cpachecker.cpa.invariants.formula;

import java.util.List;

import org.sosy_lab.cpachecker.cpa.invariants.BitVectorInfo;
import org.sosy_lab.cpachecker.cpa.invariants.CompoundInterval;
import org.sosy_lab.cpachecker.cpa.invariants.SimpleInterval;
import org.sosy_lab.cpachecker.cpa.invariants.TypeInfo;

public class SerializeNumeralFormulaVisitor
    implements NumeralFormulaVisitor<CompoundInterval, String> {

  @Override
  public String visit(Add<CompoundInterval> pAdd) {
    return "(("
        + pAdd.getSummand1().accept(this)
        + ") "
        + Operation.ADD.getRepresentation()
        + " ("
        + pAdd.getSummand2().accept(this)
        + "))";
  }

  @Override
  public String visit(BinaryAnd<CompoundInterval> pAnd) {
    return "(("
        + pAnd.getOperand1().accept(this)
        + ") "
        + Operation.BINARY_AND.getRepresentation()
        + " ("
        + pAnd.getOperand2().accept(this)
        + "))";
  }

  @Override
  public String visit(BinaryNot<CompoundInterval> pNot) {
    return "("
        + Operation.BINARY_NOT.getRepresentation()
        + "("
        + pNot.getFlipped().accept(this)
        + "))";
  }

  @Override
  public String visit(BinaryOr<CompoundInterval> pOr) {
    return "(("
        + pOr.getOperand1().accept(this)
        + ") "
        + Operation.BINARY_OR.getRepresentation()
        + " ("
        + pOr.getOperand2().accept(this)
        + "))";
  }

  @Override
  public String visit(BinaryXor<CompoundInterval> pXor) {
    return "(("
        + pXor.getOperand1().accept(this)
        + ") "
        + Operation.BINARY_XOR.getRepresentation()
        + " ("
        + pXor.getOperand2().accept(this)
        + "))";
  }

  @Override
  public String visit(Constant<CompoundInterval> pConstant) {
    List<SimpleInterval> intervals = pConstant.getValue().getIntervals();
    StringBuilder output = new StringBuilder();
    for (int i = 0; i < intervals.size(); i++) {
      SimpleInterval interval = intervals.get(i);
      String lowerBoundStr =
          interval.hasLowerBound() ? interval.getLowerBound().toString() : "-inf";
      String upperBoundStr = interval.hasUpperBound() ? interval.getUpperBound().toString() : "inf";

      output.append("[").append(lowerBoundStr).append(",").append(upperBoundStr).append("]");

      if (i < intervals.size() - 1) {
        output.append(",");
      }
    }
    return output.toString() + ".ti" + typeInfoToString(pConstant.getTypeInfo());
  }

  @Override
  public String visit(Divide<CompoundInterval> pDivide) {
    return "(("
        + pDivide.getNumerator().accept(this)
        + ") "
        + Operation.DIVIDE.getRepresentation()
        + " ("
        + pDivide.getDenominator().accept(this)
        + "))";
  }

  @Override
  public String visit(Exclusion<CompoundInterval> pExclusion) {
    return "("
        + Operation.EXCLUSION.getRepresentation()
        + " ("
        + pExclusion.getExcluded().accept(this)
        + "))";
  }

  @Override
  public String visit(Modulo<CompoundInterval> pModulo) {
    return "(("
        + pModulo.getNumerator().accept(this)
        + ") "
        + Operation.MODULO.getRepresentation()
        + " ("
        + pModulo.getDenominator().accept(this)
        + "))";
  }

  @Override
  public String visit(Multiply<CompoundInterval> pMultiply) {
    return "(("
        + pMultiply.getFactor1().accept(this)
        + ") "
        + Operation.MULTIPLY.getRepresentation()
        + " ("
        + pMultiply.getFactor2().accept(this)
        + "))";
  }

  @Override
  public String visit(ShiftLeft<CompoundInterval> pShiftLeft) {
    return "(("
        + pShiftLeft.getShifted().accept(this)
        + ") "
        + Operation.SHIFT_LEFT.getRepresentation()
        + " ("
        + pShiftLeft.getShiftDistance().accept(this)
        + "))";
  }

  @Override
  public String visit(ShiftRight<CompoundInterval> pShiftRight) {
    return "(("
        + pShiftRight.getShifted().accept(this)
        + ") "
        + Operation.SHIFT_RIGHT.getRepresentation()
        + " ("
        + pShiftRight.getShiftDistance().accept(this)
        + "))";
  }

  @Override
  public String visit(Union<CompoundInterval> pUnion) {
    return "(("
        + pUnion.getOperand1().accept(this)
        + ") "
        + Operation.UNION.getRepresentation()
        + " ("
        + pUnion.getOperand2().accept(this)
        + "))";
  }

  @Override
  public String visit(Variable<CompoundInterval> pVariable) {
    return "("
        + pVariable.getMemoryLocation().getQualifiedName()
        + ".ti"
        + typeInfoToString(pVariable.getTypeInfo())
        + ")";
  }

  @Override
  public String visit(IfThenElse<CompoundInterval> pIfThenElse) {
    return "(("
        + pIfThenElse.getCondition().accept(new SerializeBooleanFormulaVisitor(this))
        + ") "
        + Operation.IF.getRepresentation()
        + " ("
        + pIfThenElse.getPositiveCase().accept(this)
        + ") "
        + Operation.ELSE.getRepresentation()
        + " ("
        + pIfThenElse.getNegativeCase().accept(this)
        + "))";
  }

  @Override
  public String visit(Cast<CompoundInterval> pCast) {
    return "("
        + Operation.CAST.getRepresentation()
        + ".ti"
        + typeInfoToString(pCast.getTypeInfo())
        + "("
        + pCast.getCasted().accept(this)
        + "))";
  }

  private String typeInfoToString(TypeInfo typeInfo) {
    if (typeInfo instanceof BitVectorInfo) {
      return ((BitVectorInfo) typeInfo).getSize() + "," + ((BitVectorInfo) typeInfo).isSigned();
    } else {
      return typeInfo.abbrev();
    }
  }
}
