package org.sosy_lab.cpachecker.cpa.invariants.formula;

import org.sosy_lab.cpachecker.cpa.invariants.CompoundInterval;

public class SerializeBooleanFormulaVisitor
    implements BooleanFormulaVisitor<CompoundInterval, String> {
  private final NumeralFormulaVisitor<CompoundInterval, String> numeralVisitor;

  public SerializeBooleanFormulaVisitor(
      NumeralFormulaVisitor<CompoundInterval, String> pNumeralVisitor) {
    numeralVisitor = pNumeralVisitor;
  }

  @Override
  public String visit(Equal<CompoundInterval> pEqual) {
    return "(("
        + pEqual.getOperand1().accept(numeralVisitor)
        + ") "
        + Operation.EQUAL.getRepresentation()
        + " ("
        + pEqual.getOperand2().accept(numeralVisitor)
        + "))";
  }

  @Override
  public String visit(LessThan<CompoundInterval> pLessThan) {
    return "(("
        + pLessThan.getOperand1().accept(numeralVisitor)
        + ") "
        + Operation.LESS_THAN.getRepresentation()
        + " ("
        + pLessThan.getOperand2().accept(numeralVisitor)
        + "))";
  }

  @Override
  public String visit(LogicalAnd<CompoundInterval> pAnd) {
    return "(("
        + pAnd.getOperand1().accept(this)
        + ") "
        + Operation.LOGICAL_AND.getRepresentation()
        + " ("
        + pAnd.getOperand2().accept(this)
        + "))";
  }

  @Override
  public String visit(LogicalNot<CompoundInterval> pNot) {
    return "("
        + Operation.LOGICAL_NOT.getRepresentation()
        + "("
        + pNot.getNegated().accept(this)
        + "))";
  }

  @Override
  public String visitFalse() {
    return "(false)";
  }

  @Override
  public String visitTrue() {
    return "(true)";
  }
}
