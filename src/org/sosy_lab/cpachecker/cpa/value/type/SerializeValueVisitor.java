package org.sosy_lab.cpachecker.cpa.value.type;

import org.sosy_lab.cpachecker.cpa.value.symbolic.type.SymbolicValue;
import org.sosy_lab.cpachecker.cpa.value.type.Value.UnknownValue;

public class SerializeValueVisitor implements ValueVisitor<String> {

  @Override
  public String visit(EnumConstantValue pValue) {
    return "";
  }

  @Override
  public String visit(SymbolicValue pValue) {
    return "";
  }

  @Override
  public String visit(UnknownValue pValue) {
    return "UnknownValue";
  }

  @Override
  public String visit(ArrayValue pValue) {
    return "";
  }

  @Override
  public String visit(BooleanValue pValue) {
    return "BooleanValue(" + pValue.asNumericValue().getNumber().longValue() + ")";
  }

  @Override
  public String visit(FunctionValue pValue) {
    return "";
  }

  @Override
  public String visit(NumericValue pValue) {
    return "NumericValue(" + pValue.getNumber().longValue() + ")";
  }

  @Override
  public String visit(NullValue pValue) {
    return "NullValue";
  }
}
