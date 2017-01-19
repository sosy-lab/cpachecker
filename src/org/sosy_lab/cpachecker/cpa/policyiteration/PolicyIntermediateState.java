package org.sosy_lab.cpachecker.cpa.policyiteration;

import com.google.common.base.Joiner;
import com.google.common.collect.ImmutableList;
import java.util.ArrayDeque;
import java.util.Deque;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import javax.annotation.Nullable;
import org.sosy_lab.common.UniqueIdGenerator;
import org.sosy_lab.cpachecker.cfa.model.CFANode;
import org.sosy_lab.cpachecker.util.Pair;
import org.sosy_lab.cpachecker.util.predicates.pathformula.PathFormula;
import org.sosy_lab.cpachecker.util.templates.Template;
import org.sosy_lab.java_smt.api.Model.ValueAssignment;

public final class PolicyIntermediateState extends PolicyState {

  /**
   * Formula + SSA associated with the state.
   */
  private final PathFormula pathFormula;

  /**
   * Abstract state representing start of the trace.
   */
  private final PolicyAbstractedState backpointer;
  private final @Nullable PolicyAbstractedState summaryBackpointer;

  /**
   * Meta-information for determining the coverage.
   */
  private @Nullable transient PolicyIntermediateState mergedInto = null;
  private @Nullable transient ImmutableList<ValueAssignment> counterexample = null;
  private transient int hashCache = 0;

  private final int stateId;
  private static final UniqueIdGenerator idGenerator = new UniqueIdGenerator();

  private PolicyIntermediateState(
      CFANode node,
      PathFormula pPathFormula,
      PolicyAbstractedState pBackpointer,
      @Nullable PolicyAbstractedState pSummaryBackpointer) {
    super(node);

    pathFormula = pPathFormula;
    backpointer = pBackpointer;
    summaryBackpointer = pSummaryBackpointer;
    stateId = idGenerator.getFreshId();
  }

  public static PolicyIntermediateState of(
      CFANode node,
      PathFormula pPathFormula,
      PolicyAbstractedState generatingState
  ) {
    return new PolicyIntermediateState(
        node, pPathFormula, generatingState, null);
  }

  public static PolicyIntermediateState of(
      CFANode node,
      PathFormula pPathFormula,
      PolicyAbstractedState pGeneratingState,
      PolicyAbstractedState pSummaryState
  ) {
    return new PolicyIntermediateState(
        node, pPathFormula, pGeneratingState, pSummaryState);
  }

  int getId() {
    return stateId;
  }

  /**
   * Set the transient counterexample information for visualization purposes.
   */
  void setCounterexample(ImmutableList<ValueAssignment> pCounterexample) {
    counterexample = pCounterexample;
  }

  PolicyIntermediateState withPathFormula(
      PathFormula pPathFormula
  ) {
    return new PolicyIntermediateState(
        getNode(), pPathFormula, backpointer, summaryBackpointer
    );
  }

  PolicyIntermediateState withPathFormulaAndNode(
      PathFormula pPathFormula,
      CFANode node
  ) {
    return new PolicyIntermediateState(
        node, pPathFormula, backpointer, summaryBackpointer
    );
  }

  void setMergedInto(PolicyIntermediateState other) {
    mergedInto = other;
  }

  boolean isMergedInto(PolicyIntermediateState other) {
    return other == mergedInto;
  }

  /**
   * @return the backpointer containing information about the template.
   */
  PolicyAbstractedState getBackpointerStateForTemplate(Template t) {

    // Summary backpointer takes precedence due to global variables potentially modified
    // inside the function.
    if (summaryBackpointer != null && summaryBackpointer.getBound(t).isPresent()) {
      return summaryBackpointer;
    }
    return backpointer;
  }

  /**
   * @return Starting {@link PolicyAbstractedState} for the starting location.
   */
  List<PolicyAbstractedState> getBackpointerStates() {
    if (summaryBackpointer == null) {
      return ImmutableList.of(backpointer);
    }
    return ImmutableList.of(backpointer, summaryBackpointer);
  }


  public PathFormula getPathFormula() {
    return pathFormula;
  }

  @Override
  public boolean isAbstract() {
    return false;
  }

  /**
   * Iterator for all states <em>including</em> this one,
   * up to the analysis root.
   */
  Iterable<PolicyIntermediateState> allStatesToRoot() {
    return topSort();
  }


  /**
   * @return topological order from this node to root.
   */
  private List<PolicyIntermediateState> topSort() {

    LinkedList<PolicyIntermediateState> out = new LinkedList<>();
    Deque<Pair<PolicyIntermediateState, Boolean>> stack = new ArrayDeque<>();
    Set<PolicyIntermediateState> visited = new HashSet<>();

    stack.add(Pair.of(this, false));

    while (!stack.isEmpty()) {
      Pair<PolicyIntermediateState, Boolean> p = stack.pop();
      PolicyIntermediateState s = p.getFirstNotNull();
      if (p.getSecondNotNull()) {
        out.add(s);
        continue;
      }
      visited.add(s);
      stack.push(Pair.of(s, true));

      for (PolicyAbstractedState b : s.getBackpointerStates()) {
        b.getGeneratingState().ifPresent(
            g -> {
              if (!visited.contains(g)) {
                stack.add(Pair.of(g, false));
              }
            }
        );
      }
    }
    return out;
  }

  @Override
  public String toDOTLabel() {
    if (counterexample != null) {
      return Joiner.on('\n').join(counterexample);
    }
    if (getBackpointerStates().get(0).getManager().shouldDisplayFormulasInDotOutput()) {
      return pathFormula.toString() + "\n"
          + (summaryBackpointer != null ? "usedSummary: " + summaryBackpointer.toDOTLabel() + "\n" : "")
          + "backointer: " + backpointer.toDOTLabel();
    } else {
      return "";
    }
  }

  @Override
  public String toString() {
    return pathFormula.toString();
  }

  @Override
  public boolean equals(Object pO) {
    if (this == pO) {
      return true;
    }
    if (!(pO instanceof PolicyIntermediateState)) {
      return false;
    }
    PolicyIntermediateState that = (PolicyIntermediateState) pO;
    return Objects.equals(pathFormula, that.pathFormula) &&
        Objects.equals(backpointer, that.backpointer) &&
        Objects.equals(summaryBackpointer, that.summaryBackpointer) &&
        Objects.equals(mergedInto, that.mergedInto) &&
        Objects.equals(getNode(), that.getNode());
  }

  @Override
  public int hashCode() {
    if (hashCache == 0) {
      hashCache = Objects.hash(pathFormula, backpointer, summaryBackpointer, mergedInto);
    }
    return hashCache;
  }
}
