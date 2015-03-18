package org.sosy_lab.cpachecker.cpa.policyiteration;

import java.util.Map;
import java.util.Set;

import org.sosy_lab.cpachecker.util.predicates.pathformula.PathFormula;

import com.google.common.base.Objects;
import com.google.common.collect.ImmutableMap;

public final class PolicyIntermediateState extends PolicyState {

  /**
   * Formula + SSA associated with the state.
   */
  private final PathFormula pathFormula;

  /**
   * Abstract states used for generating this state.
   */
  private final ImmutableMap<Location, PolicyAbstractedState> generatingStates;

  private transient PolicyIntermediateState mergedInto;

  private int hashCache = 0;


  private PolicyIntermediateState(
      Location pLocation,
      Set<Template> pTemplates,
      PathFormula pPathFormula,
      Map<Location, PolicyAbstractedState> pGeneratingStates
      ) {
    super(pLocation, pTemplates);

    pathFormula = pPathFormula;
    generatingStates = ImmutableMap.copyOf(pGeneratingStates);
  }

  public static PolicyIntermediateState of(
      Location pLocation,
      Set<Template> pTemplates,
      PathFormula pPathFormula,
      Map<Location, PolicyAbstractedState> generatingStates
  ) {
    return new PolicyIntermediateState(pLocation, pTemplates, pPathFormula,
        generatingStates);
  }

  public void setMergedInto(PolicyIntermediateState other) {
    mergedInto = other;
  }

  public boolean isMergedInto(PolicyIntermediateState other) {
    return other == mergedInto;
  }

  /**
   * @return Starting {@link PathFormula} for possible starting locations.
   */
  public Map<Location, PolicyAbstractedState> getGeneratingStates() {
    return generatingStates;
  }

  public PathFormula getPathFormula() {
    return pathFormula;
  }

  @Override
  public boolean isAbstract() {
    return false;
  }

  @Override
  public String toDOTLabel() {
    return String.format(
        "%n%s%n%s%n", pathFormula, pathFormula.getSsa()
    );
  }

  @Override
  public String toString() {
    return pathFormula.toString();
  }

  @Override
  public int hashCode() {
    if (hashCache == 0) {
      hashCache = Objects.hashCode(pathFormula, generatingStates,
          super.hashCode());
    }
    return hashCache;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }
    PolicyIntermediateState other = (PolicyIntermediateState)o;
    return (pathFormula.equals(other.pathFormula)
        && generatingStates.equals(other.generatingStates)
        && super.equals(o));
  }
}
