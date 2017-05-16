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
  public SingleIdentifier clone() {
    return new FunctionIdentifier(name, type, dereference);
  }

  @Override
  public String toString() {
    StringBuilder sb = new StringBuilder();
    if (dereference > 0) {
      for (int i = 0; i < dereference; i++) {
        sb.append("*");
      }
    } else if (dereference == -1) {
      sb.append("&");
    } else if (dereference < -1){
      sb.append("Error in string representation, dereference < -1");
      return sb.toString();
    }
    sb.append(name + "()");
    return sb.toString();
  }

  @Override
  public SingleIdentifier clearDereference() {
    return new FunctionIdentifier(name, type, 0);
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
