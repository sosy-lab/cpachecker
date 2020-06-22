package org.sosy_lab.cpachecker.core.algorithm.acsl;

public class Identifier implements ACSLTerm {

  private final String name;
  // TODO: Needs a type! Perhaps use MemoryLocation instead altogether? (Currently no difference
  // between Identifier and StringLiteral!)

  public Identifier(String pName) {
    name = pName;
  }

  @Override
  public String toString() {
    return name;
  }

  @Override
  public ACSLTerm toPureC() {
    return this;
  }

  @Override
  public boolean equals(Object o) {
    if (o instanceof Identifier) {
      Identifier other = (Identifier) o;
      return name.equals(other.name);
    }
    return false;
  }

  @Override
  public int hashCode() {
    return 29 * name.hashCode();
  }
}
