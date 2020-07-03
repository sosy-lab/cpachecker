package org.sosy_lab.cpachecker.core.algorithm.acsl;

import com.google.common.base.Preconditions;
import org.sosy_lab.cpachecker.cfa.ast.c.CExpression;
import org.sosy_lab.cpachecker.exceptions.UnrecognizedCodeException;

public class ArrayAccess implements ACSLTerm {

  private final Identifier array;
  private final IntegerLiteral index;

  public ArrayAccess(ACSLTerm pArray, ACSLTerm pIndex) {
    Preconditions.checkArgument(pArray instanceof Identifier);
    Preconditions.checkArgument(pIndex instanceof IntegerLiteral);
    array = (Identifier) pArray;
    index = (IntegerLiteral) pIndex;
  }

  @Override
  public boolean equals(Object o) {
    if (o instanceof ArrayAccess) {
      ArrayAccess other = (ArrayAccess) o;
      return array.equals(other.array) && index.equals(other.index);
    }
    return false;
  }

  @Override
  public int hashCode() {
    return 3 * array.hashCode() + index.hashCode();
  }

  @Override
  public String toString() {
    return array.toString() + "[" + index.toString() + "]";
  }

  @Override
  public ArrayAccess toPureC() {
    return new ArrayAccess(array.toPureC(), index.toPureC());
  }

  public Identifier getArray() {
    return array;
  }

  public IntegerLiteral getIndex() {
    return index;
  }

  @Override
  public CExpression accept(ACSLToCExpressionVisitor visitor) throws UnrecognizedCodeException {
    return visitor.visit(toPureC());
  }
}
