package org.sosy_lab.cpachecker.util.ecp.translators;

public interface GuardedLabelVisitor<T> {

  public T visit(GuardedLambdaLabel pLabel);
  public T visit(GuardedEdgeLabel pLabel);

}
