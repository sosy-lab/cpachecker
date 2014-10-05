package org.sosy_lab.cpachecker.cpa.stator.memory;

import org.sosy_lab.cpachecker.cfa.ast.c.CVariableDeclaration;
import org.sosy_lab.cpachecker.cfa.types.c.CPointerType;

public final class MemorySegment {

// TODO: scope handling.
  private final CVariableDeclaration declaration;

  private MemorySegment(CVariableDeclaration declaration) {
    this.declaration = declaration;
  }

  public static MemorySegment ofDeclaration(CVariableDeclaration declaration) {
    return new MemorySegment(declaration);
  }

  public CVariableDeclaration getDeclaration() {
    return declaration;
  }

  @Override
  public String toString() {
    if (declaration == null) return "NULL";

    return declaration.getType() .toASTString(declaration.getName());
  }

  @Override
  public int hashCode() {
    return declaration.hashCode();
  }

  public boolean isPointerType() {
    return declaration.getType() instanceof CPointerType;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }

    MemorySegment segment1 = (MemorySegment)o;
    return declaration == segment1.declaration;

  }
}
