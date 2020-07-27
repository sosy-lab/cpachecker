package org.sosy_lab.cpachecker.core.algorithm.acsl;

import com.google.common.base.Preconditions;
import com.google.common.collect.ImmutableSet;
import java.util.Set;
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

  public Identifier getArray() {
    return array;
  }

  public IntegerLiteral getIndex() {
    return index;
  }

  @Override
  public CExpression accept(ACSLTermToCExpressionVisitor visitor) throws UnrecognizedCodeException {
    return visitor.visit(this);
  }

  @Override
  public ACSLTerm useOldValues() {
    return new ArrayAccess(array.useOldValues(), index.useOldValues());
  }

  @Override
  public boolean isAllowedIn(Class<?> clauseType) {
    return array.isAllowedIn(clauseType) && index.isAllowedIn(clauseType);
  }

  @Override
  public Set<ACSLBuiltin> getUsedBuiltins() {
    ImmutableSet.Builder<ACSLBuiltin> builder = ImmutableSet.builder();
    return builder.addAll(array.getUsedBuiltins()).addAll(index.getUsedBuiltins()).build();
  }
}
