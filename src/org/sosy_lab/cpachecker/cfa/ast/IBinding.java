package org.sosy_lab.cpachecker.cfa.ast;

import org.eclipse.cdt.core.dom.ILinkage;
import org.eclipse.cdt.core.dom.ast.IScope;

public interface IBinding extends org.eclipse.cdt.core.dom.ast.IBinding {

  @SuppressWarnings("rawtypes")
  @Override
  @Deprecated
  public Object getAdapter(Class pArg0);

  @Override
  @Deprecated
  public ILinkage getLinkage();

  @Override
  public String getName();

  @Override
  @Deprecated
  public char[] getNameCharArray();

  @Override
  @Deprecated
  public IBinding getOwner();

  @Override
  @Deprecated
  public IScope getScope();
}
