package org.sosy_lab.cpachecker.fllesh.fql2.translators.ecp;

public interface GuardedLabelVisitor<T> {

  public T visit(GuardedLambdaLabel pLabel);
  public T visit(GuardedEdgeLabel pLabel);
  
}
