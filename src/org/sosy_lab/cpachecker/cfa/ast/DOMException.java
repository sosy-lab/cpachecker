package org.sosy_lab.cpachecker.cfa.ast;

public final class DOMException extends
    org.eclipse.cdt.core.dom.ast.DOMException {

  private static final long serialVersionUID = -1456395570449959563L;

  public DOMException(org.eclipse.cdt.core.dom.ast.DOMException e) {
    super(e.getProblem());
    initCause(e);
  }

}
