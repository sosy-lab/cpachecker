package org.sosy_lab.cpachecker.fllesh.fql.frontend.ast.pathmonitor;

public interface PathMonitorVisitor<T> {

  public T visit(Alternative pAlternative);
  public T visit(Concatenation pConcatenation);
  public T visit(ConditionalMonitor pConditionalMonitor);
  public T visit(LowerBound pLowerBound);
  public T visit(UpperBound pUpperBound);
  public T visit(FilterMonitor pFilterMonitor);
  
}
