package org.sosy_lab.cpachecker.cpa.unsequenced;

import java.util.Objects;
import org.sosy_lab.cpachecker.cfa.model.CFAEdge;

public class ConflictPair {
  private final SideEffectInfo accessA;
  private final SideEffectInfo accessB;
  private final CFAEdge location;
  private final String exprA;
  private final String exprB;

  // (a, b) == (b, a)
  public ConflictPair(
      SideEffectInfo pAccessA,
      SideEffectInfo pAccessB,
      CFAEdge pLocation,
      String pExprA,
      String pExprB) {

    if (pAccessA.toString().compareTo(pAccessB.toString()) <= 0) {
      this.accessA = pAccessA;
      this.accessB = pAccessB;
      this.exprA = pExprA;
      this.exprB = pExprB;
    } else {
      this.accessA = pAccessB;
      this.accessB = pAccessA;
      this.exprA = pExprB;
      this.exprB = pExprA;
    }

    this.location = pLocation;
  }

  public SideEffectInfo getAccessA() {
    return accessA;
  }

  public SideEffectInfo getAccessB() {
    return accessB;
  }

  public CFAEdge getLocation() {
    return location;
  }

  public String getExprA() {
    return exprA;
  }

  public String getExprB() {
    return exprB;
  }

  @Override
  public boolean equals(Object obj) {
    if (this == obj) return true;
    if (!(obj instanceof ConflictPair other)) return false;
    return Objects.equals(location, other.location)
        && Objects.equals(accessA, other.accessA)
        && Objects.equals(accessB, other.accessB);
  }

  @Override
  public int hashCode() {
    return Objects.hash(location, accessA, accessB);
  }

  @Override
  public String toString() {
    return String.format(
        "Conflict at %s (line %d): %s (%s) <=> %s (%s)",
        location.getRawStatement(),
        location.getFileLocation().getStartingLineInOrigin(),
        exprA,
        accessA.toStringSimple(),
        exprB,
        accessB.toStringSimple());
  }
}
