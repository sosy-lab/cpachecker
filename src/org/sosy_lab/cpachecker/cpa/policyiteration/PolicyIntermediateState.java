package org.sosy_lab.cpachecker.cpa.policyiteration;

import java.util.Map;
import java.util.Set;

import org.sosy_lab.cpachecker.util.predicates.pathformula.PathFormula;

import com.google.common.base.Objects;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableMultimap;
import com.google.common.collect.Multimap;

public final class PolicyIntermediateState extends PolicyState {

  /**
   * Formula + SSA associated with the state.
   */
  private final PathFormula pathFormula;

  /**
   * Trace to determine the coverage relation.
   * Format: to-node -> set of from_nodes explored.
   */
  private final ImmutableMultimap<Location, Location> trace;

  /**
   * SSAMap for the starting abstraction points.
   */
  private final ImmutableMap<Location, PathFormula> startPathFormulas;

  private PolicyIntermediateState(
      Location pLocation,
      Set<Template> pTemplates,
      PathFormula pPathFormula,
      Multimap<Location, Location> pTrace,
      Map<Location, PathFormula> pStartPathFormulas
      ) {
    super(pLocation, pTemplates);

    pathFormula = pPathFormula;
    trace = ImmutableMultimap.copyOf(pTrace);
    startPathFormulas = ImmutableMap.copyOf(pStartPathFormulas);
  }

  public static PolicyIntermediateState of(
      Location pLocation,
      Set<Template> pTemplates,
      PathFormula pPathFormula,
      Multimap<Location, Location> pTrace,
      Map<Location, PathFormula> pStartPathFormula
  ) {
    return new PolicyIntermediateState(pLocation, pTemplates, pPathFormula,
        pTrace, pStartPathFormula);
  }

  /**
   * @return Starting {@link PathFormula} for possible starting locations.
   */
  public Map<Location, PathFormula> getStartPathFormulaMap() {
    return startPathFormulas;
  }

  public PathFormula getPathFormula() {
    return pathFormula;
  }

  public ImmutableMultimap<Location, Location> getTrace() {
    return trace;
  }

  @Override
  public boolean isAbstract() {
    return false;
  }

  @Override
  public String toDOTLabel() {
    return pathFormula.toString() + "\n" + pathFormula.getSsa().toString();
  }

  @Override
  public String toString() {
    return String.format("%s: %s", getLocation(), pathFormula);
  }

  @Override
  public int hashCode() {
    return Objects.hashCode(pathFormula, super.hashCode());
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
    return (pathFormula.equals(other.pathFormula) && super.equals(o));
  }
}
