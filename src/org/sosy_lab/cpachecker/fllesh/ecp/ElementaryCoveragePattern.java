package org.sosy_lab.cpachecker.fllesh.ecp;

public interface ElementaryCoveragePattern {

  public <T> T accept(ECPVisitor<T> pVisitor);
  
}
