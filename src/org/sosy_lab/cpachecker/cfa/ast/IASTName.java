package org.sosy_lab.cpachecker.cfa.ast;

// these eclipse-imports are only for compiling, they will not be used in this class.
// all methods, that use this imports, throw UnsupportedOperationExceptions

import org.eclipse.cdt.core.dom.ILinkage;
import org.eclipse.cdt.core.dom.ast.IASTCompletionContext;
import org.eclipse.cdt.core.dom.ast.IASTImageLocation;
import org.eclipse.cdt.core.dom.ast.IBinding;

public final class IASTName extends IASTNode implements
    org.eclipse.cdt.core.dom.ast.IASTName {

  public IASTName(final String pRawSignature,
      final IASTFileLocation pFileLocation) {
    super(pRawSignature, pFileLocation);
  }

  @Override
  public char[] getSimpleID() {
    // TODO: is this really important?
    return getRawSignature().toCharArray();
}

  @Override
  @Deprecated
  public boolean isDeclaration() {
    throw new UnsupportedOperationException();
  }

  @Override
  @Deprecated
  public boolean isDefinition() {
    throw new UnsupportedOperationException();
  }

  @Override
  @Deprecated
  public boolean isReference() {
    throw new UnsupportedOperationException();
  }

  @Override
  @Deprecated
  public IBinding getBinding() {
    throw new UnsupportedOperationException();
  }

  @Override
  @Deprecated
  public IASTCompletionContext getCompletionContext() {
    throw new UnsupportedOperationException();
  }

  @Override
  @Deprecated
  public IASTImageLocation getImageLocation() {
    throw new UnsupportedOperationException();
  }

  @Override
  @Deprecated
  public IASTName getLastName() {
    throw new UnsupportedOperationException();
  }

  @Override
  @Deprecated
  public ILinkage getLinkage() {
    throw new UnsupportedOperationException();
  }

  @Override
  @Deprecated
  public char[] getLookupKey() {
    throw new UnsupportedOperationException();
  }

  @Override
  @Deprecated
  public IBinding getPreBinding() {
    throw new UnsupportedOperationException();
  }

  @Override
  @Deprecated
  public int getRoleOfName(final boolean pArg0) {
    throw new UnsupportedOperationException();
  }

  @Override
  @Deprecated
  public IBinding resolveBinding() {
    throw new UnsupportedOperationException();
  }

  @Override
  @Deprecated
  public IBinding resolvePreBinding() {
    throw new UnsupportedOperationException();
  }

  @Override
  @Deprecated
  public void setBinding(final IBinding pArg0) {
    throw new UnsupportedOperationException();
  }

  @Override
  @Deprecated
  public char[] toCharArray() {
    throw new UnsupportedOperationException();
  }

  @Override
  @Deprecated
  public IASTName copy() {
    throw new UnsupportedOperationException();
  }
}
