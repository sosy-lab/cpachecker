package org.sosy_lab.cpachecker.fllesh.ecp;

public interface ECPVisitor<T> {

  public T visit(ECPEdgeSet pEdgeSet);
  public T visit(ECPNodeSet pNodeSet);
  public T visit(ECPPredicate pPredicate);
  public T visit(ECPConcatenation pConcatenation);
  public T visit(ECPUnion pUnion);
  public T visit(ECPRepetition pRepetition);
  
}
