package org.sosy_lab.cpachecker.util.identifiers;

import java.util.Collection;
import java.util.Collections;
import org.sosy_lab.cpachecker.cfa.types.c.CType;

public class FunctionIdentifier extends SingleIdentifier {

  public FunctionIdentifier(String nm, CType tp, int deref) {
    super(nm, tp, deref);
  }

  @Override
  public int compareTo(AbstractIdentifier pO) {
    if (pO instanceof FunctionIdentifier) {
      return super.compareTo(pO);
    } else {
      return -1;
    }
  }

  @Override
  public boolean isGlobal() {
    return false;
  }

  @Override
  public SingleIdentifier cloneWithDereference(int pDereference) {
    return new FunctionIdentifier(name, type, pDereference);
  }

  @Override
  public String toString() {
    return super.toString() + "()";
  }

  @Override
  public String toLog() {
    return "func;" + name + ";" + dereference;
  }

  @Override
  public GeneralIdentifier getGeneralId() {
    return null;
  }

  @Override
  public Collection<AbstractIdentifier> getComposedIdentifiers() {
    return Collections.emptySet();
  }
}
