package org.sosy_lab.cpachecker.cpa.policyiteration.congruence;

import com.google.common.collect.ImmutableMap;

import org.sosy_lab.cpachecker.cpa.policyiteration.Template;
import org.sosy_lab.cpachecker.util.predicates.pathformula.PathFormula;
import org.sosy_lab.cpachecker.util.predicates.pathformula.PathFormulaManager;
import org.sosy_lab.cpachecker.util.predicates.smt.FormulaManagerView;
import org.sosy_lab.solver.api.BooleanFormula;

import java.util.Iterator;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Objects;
import java.util.Optional;

public class CongruenceState implements
                             Iterable<Entry<Template, Congruence>> {

  private final ImmutableMap<Template, Congruence> data;
  private final CongruenceManager congruenceManager;

  public CongruenceState(
      Map<Template, Congruence> pData,
      CongruenceManager pCongruenceManager) {
    data = ImmutableMap.copyOf(pData);
    congruenceManager = pCongruenceManager;
  }

  public Map<Template, Congruence> getAbstraction() {
    return data;
  }

  public static CongruenceState empty(CongruenceManager pCongruenceManager) {
    return new CongruenceState(ImmutableMap.of(), pCongruenceManager);
  }

  public Optional<Congruence> get(Template template) {
    Congruence c = data.get(template);
    if (c == null) {
      return Optional.empty();
    }
    return Optional.of(c);
  }

  @Override
  public Iterator<Entry<Template, Congruence>> iterator() {
    return data.entrySet().iterator();
  }

  public String toDOTLabel() {
    StringBuilder b = new StringBuilder();
    for (Entry<Template, Congruence> e : data.entrySet()) {
      if (e.getValue() == Congruence.EVEN) {
        b.append(e.getKey().toString()).append(" is even\n");
      } else if (e.getValue() == Congruence.ODD) {
        b.append(e.getKey().toCString()).append(" is odd\n");
      }
    }
    return b.toString();
  }

  @Override
  public int hashCode() {
    return Objects.hashCode(data);
  }

  @Override
  public boolean equals(Object o) {
    if (!(o instanceof CongruenceState)) {
      return false;
    }
    if (o == this) {
      return true;
    }
    CongruenceState other = (CongruenceState) o;
    return other.data.equals(data);
  }

  /**
   * Convert the state to <b>instantiated</b> formula with respect to the
   * PathFormula {@code ref}.
   */
  public BooleanFormula toFormula(
      FormulaManagerView manager, PathFormulaManager pfmgr, PathFormula ref) {
    return congruenceManager.toFormula(pfmgr, manager, this, ref);
  }
}
