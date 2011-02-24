package org.sosy_lab.cpachecker.cfa.ast;

import org.eclipse.cdt.core.dom.ast.DOMException;

public class IFunctionType extends IType implements org.eclipse.cdt.core.dom.ast.IFunctionType {

  @Override
  @Deprecated
  public org.eclipse.cdt.core.dom.ast.IType[] getParameterTypes() throws DOMException {
    throw new UnsupportedOperationException();
  }

  @Override
  @Deprecated
  public org.eclipse.cdt.core.dom.ast.IType getReturnType() throws DOMException {
    throw new UnsupportedOperationException();
  }

}
