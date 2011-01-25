package org.sosy_lab.cpachecker.util.ecp;

public interface ElementaryCoveragePattern {

  public <T> T accept(ECPVisitor<T> pVisitor);
  
}
