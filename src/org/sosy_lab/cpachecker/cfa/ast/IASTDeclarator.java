package org.sosy_lab.cpachecker.cfa.ast;

import java.util.List;
import com.google.common.collect.ImmutableList;

public abstract class IASTDeclarator extends IASTNode {

  private final IASTInitializer           initializer;
  private final IASTName                  name;
  private final IASTDeclarator            nestedDeclarator;
  private final List<IASTPointer> pointerOperators;

  public IASTDeclarator(final String pRawSignature,
      final IASTFileLocation pFileLocation, final IASTInitializer pInitializer,
      final IASTName pName, final IASTDeclarator pNestedDeclarator,
      final List<IASTPointer> pPointerOperators) {
    super(pRawSignature, pFileLocation);
    initializer = pInitializer;
    name = pName;
    nestedDeclarator = pNestedDeclarator;
    pointerOperators = ImmutableList.copyOf(pPointerOperators);
  }

  public IASTInitializer getInitializer() {
    return initializer;
  }

  public IASTName getName() {
    return name;
  }

  public IASTDeclarator getNestedDeclarator() {
    return nestedDeclarator;
  }

  public IASTPointer[] getPointerOperators() {
    return pointerOperators.toArray(new IASTPointer[pointerOperators
        .size()]);
  }

  @Override
  public IASTNode[] getChildren(){
    final IASTNode[] children = pointerOperators.toArray(
        new IASTPointer[pointerOperators.size() + 3]);
    children[pointerOperators.size()] = initializer;
    children[pointerOperators.size() + 1] = name;
    children[pointerOperators.size() + 2] = nestedDeclarator;
    return children;
  }
}
