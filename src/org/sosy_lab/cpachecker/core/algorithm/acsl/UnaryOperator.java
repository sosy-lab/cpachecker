package org.sosy_lab.cpachecker.core.algorithm.acsl;

public enum UnaryOperator {
  BNEG("~"),
  PLUS("+"),
  MINUS("-"),
  POINTER_DEREF("*"),
  ADDRESS_OF("&"),
  SIZEOF("sizeof");

  private final String operator;

  UnaryOperator(String s) {
    operator = s;
  }

  @Override
  public String toString() {
    return operator;
  }
}
