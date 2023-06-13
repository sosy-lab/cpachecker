// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2007-2020 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.cpa.usage;

import static com.google.common.collect.FluentIterable.from;

import com.google.common.base.Joiner;
import com.google.common.base.Predicates;
import com.google.common.collect.HashMultiset;
import com.google.common.collect.Multiset;
import java.util.Map.Entry;
import java.util.Objects;
import java.util.Optional;
import org.sosy_lab.common.collect.PathCopyingPersistentTreeMap;
import org.sosy_lab.common.collect.PersistentSortedMap;
import org.sosy_lab.cpachecker.core.defaults.AbstractSerializableSingleWrapperState;
import org.sosy_lab.cpachecker.core.defaults.LatticeAbstractState;
import org.sosy_lab.cpachecker.core.interfaces.AbstractState;
import org.sosy_lab.cpachecker.cpa.arg.ARGState;
import org.sosy_lab.cpachecker.cpa.lock.LockState;
import org.sosy_lab.cpachecker.cpa.lock.effects.LockEffect;
import org.sosy_lab.cpachecker.cpa.predicate.PredicateAbstractState;
import org.sosy_lab.cpachecker.cpa.usage.storage.FunctionContainer;
import org.sosy_lab.cpachecker.cpa.usage.storage.FunctionContainer.StorageStatistics;
import org.sosy_lab.cpachecker.cpa.usage.storage.TemporaryUsageStorage;
import org.sosy_lab.cpachecker.util.AbstractStates;
import org.sosy_lab.cpachecker.util.identifiers.AbstractIdentifier;
import org.sosy_lab.cpachecker.util.identifiers.Identifiers;
import org.sosy_lab.cpachecker.util.identifiers.SingleIdentifier;
import org.sosy_lab.cpachecker.util.statistics.StatTimer;
import org.sosy_lab.cpachecker.util.statistics.StatisticsWriter;

/** Represents one abstract state of the Usage CPA. */
public final class UsageState extends AbstractSerializableSingleWrapperState
    implements LatticeAbstractState<UsageState> {

  private static final long serialVersionUID = -898577877284268426L;
  private TemporaryUsageStorage recentUsages;
  // private boolean isStorageCloned;
  private final FunctionContainer functionContainer;
  private final transient StateStatistics stats;

  private boolean isExitState;
  private boolean isStorageDumped;

  private transient PersistentSortedMap<AbstractIdentifier, AbstractIdentifier>
      variableBindingRelation;

  private UsageState(
      final AbstractState pWrappedElement,
      final PersistentSortedMap<AbstractIdentifier, AbstractIdentifier> pVarBind,
      final TemporaryUsageStorage pRecentUsages,
      final FunctionContainer pFuncContainer,
      final StateStatistics pStats,
      boolean exit) {
    super(pWrappedElement);
    variableBindingRelation = pVarBind;
    recentUsages = pRecentUsages;
    functionContainer = pFuncContainer;
    stats = pStats;
    isExitState = exit;
    isStorageDumped = false;
  }

  public static UsageState createInitialState(final AbstractState pWrappedElement) {
    FunctionContainer initialContainer = FunctionContainer.createInitialContainer();
    return new UsageState(
        pWrappedElement,
        PathCopyingPersistentTreeMap.of(),
        new TemporaryUsageStorage(),
        initialContainer,
        new StateStatistics(initialContainer.getStatistics()),
        false);
  }

  private UsageState(final AbstractState pWrappedElement, final UsageState state) {
    this(
        pWrappedElement,
        state.variableBindingRelation,
        state.recentUsages,
        state.functionContainer,
        state.stats,
        state.isExitState);
  }

  public boolean containsLinks(final AbstractIdentifier id) {
    /* Special contains!
     *  if we have *b, map also contains **b, ***b and so on.
     *  So, if we get **b, having (*b, c), we give *c
     */
    return from(Identifiers.getDereferencedIdentifiers(id))
        .anyMatch(variableBindingRelation::containsKey);
  }

  public void put(final AbstractIdentifier id1, final AbstractIdentifier id2) {
    if (!id1.equals(id2)) {
      variableBindingRelation = variableBindingRelation.putAndCopy(id1, id2);
    }
  }

  public AbstractIdentifier getLinksIfNecessary(final AbstractIdentifier id) {

    if (!containsLinks(id)) {
      return id;
    }
    /* Special get!
     * If we get **b, having (*b, c), we give *c
     */
    Optional<AbstractIdentifier> linkedId =
        Identifiers.getDereferencedIdentifiers(id).stream()
            .filter(variableBindingRelation::containsKey)
            .findFirst();

    if (linkedId.isPresent()) {
      AbstractIdentifier pointsFrom = linkedId.orElseThrow();
      int delta = id.getDereference() - pointsFrom.getDereference();
      AbstractIdentifier initialId = variableBindingRelation.get(pointsFrom);
      AbstractIdentifier pointsTo =
          initialId.cloneWithDereference(initialId.getDereference() + delta);
      if (containsLinks(pointsTo)) {
        pointsTo = getLinksIfNecessary(pointsTo);
      }
      return pointsTo;
    }

    return null;
  }

  public UsageState copy() {
    return copy(getWrappedState());
  }

  public UsageState copy(final AbstractState pWrappedState) {
    UsageState result = new UsageState(pWrappedState, this);
    if (isStorageDumped) {
      result.recentUsages = new TemporaryUsageStorage();
      functionContainer.registerTemporaryContainer(result.recentUsages);
    }
    return result;
  }

  @Override
  public int hashCode() {
    final int prime = 31;
    int result = 1;
    result = prime * result + Objects.hashCode(variableBindingRelation);
    result = prime * result + Objects.hashCode(recentUsages);
    result = prime * result + super.hashCode();
    return result;
  }

  @Override
  public boolean equals(Object obj) {
    if (this == obj) {
      return true;
    }
    if (obj == null || getClass() != obj.getClass()) {
      return false;
    }
    UsageState other = (UsageState) obj;
    return Objects.equals(variableBindingRelation, other.variableBindingRelation)
        && Objects.equals(recentUsages, other.recentUsages)
        && getWrappedState().equals(other.getWrappedState());
  }

  @Override
  public String toString() {
    StringBuilder str = new StringBuilder();

    str.append("[");
    Joiner.on(", ").withKeyValueSeparator("->").appendTo(str, variableBindingRelation);
    str.append("]\n");
    str.append(getWrappedState());
    return str.toString();
  }

  @Override
  public boolean isLessOrEqual(final UsageState other) {
    // If we are here, the wrapped domain return true and the stop depends only on this value
    stats.lessTimer.start();
    // this element is not less or equal than the other element, if that one contains less elements
    if (variableBindingRelation.size() > other.variableBindingRelation.size()) {
      stats.lessTimer.stop();
      return false;
    }

    // also, this element is not less or equal than the other element,
    // if any one constant's value of the other element differs from the constant's value in this
    // element
    if (from(variableBindingRelation.keySet())
        .anyMatch(Predicates.not(other.variableBindingRelation::containsKey))) {
      stats.lessTimer.stop();
      return false;
    }

    // in case of true, we need to copy usages
    /*for (SingleIdentifier id : this.recentUsages.keySet()) {
      for (UsageInfo usage : this.recentUsages.get(id)) {
        other.addUsage(id, usage);
      }
    }*/
    if (!recentUsages.isSubsetOf(other.recentUsages)) {
      stats.lessTimer.stop();
      return false;
    }
    stats.lessTimer.stop();
    return true;
  }

  public void addUsage(final SingleIdentifier id, final UsageInfo usage) {
    recentUsages.add(id, usage);
  }

  public void joinContainerFrom(final UsageState reducedState) {
    stats.joinTimer.start();
    functionContainer.join(reducedState.functionContainer);
    stats.joinTimer.stop();
    // Free useless memory
    reducedState.functionContainer.clearStorages();
  }

  public void joinRecentUsagesFrom(final UsageState pState) {
    stats.joinTimer.start();
    recentUsages.copyUsagesFrom(pState.recentUsages);
    stats.joinTimer.stop();
    for (Entry<AbstractIdentifier, AbstractIdentifier> entry :
        pState.variableBindingRelation.entrySet()) {
      variableBindingRelation =
          variableBindingRelation.putAndCopy(entry.getKey(), entry.getValue());
    }
  }

  public UsageState reduce(final AbstractState wrappedState) {
    LockState rootLockState = AbstractStates.extractStateByType(this, LockState.class);
    LockState reducedLockState = AbstractStates.extractStateByType(wrappedState, LockState.class);
    Multiset<LockEffect> difference;
    if (rootLockState == null || reducedLockState == null) {
      // No LockCPA
      difference = HashMultiset.create();
    } else {
      difference = reducedLockState.getDifference(rootLockState);
    }

    return new UsageState(
        wrappedState,
        PathCopyingPersistentTreeMap.of(),
        recentUsages.copy(),
        functionContainer.clone(difference),
        stats,
        isExitState);
  }

  public void saveUnsafesInContainerIfNecessary(AbstractState abstractState) {
    ARGState argState = AbstractStates.extractStateByType(abstractState, ARGState.class);
    PredicateAbstractState state =
        AbstractStates.extractStateByType(argState, PredicateAbstractState.class);
    if (state == null || (!state.getAbstractionFormula().isFalse() && state.isAbstractionState())) {
      recentUsages.setKeyState(argState);
      stats.addRecentUsagesTimer.start();
      functionContainer.join(recentUsages);
      stats.addRecentUsagesTimer.stop();
      isStorageDumped = true;
    }
  }

  public FunctionContainer getFunctionContainer() {
    return functionContainer;
  }

  public void asExitable() {
    // return new UsageExitableState(this);
    isExitState = true;
  }

  public StateStatistics getStatistics() {
    return stats;
  }

  /*@Override
  public boolean isExitState() {
    return isExitState;
  }*/

  /*public class UsageExitableState extends UsageState {

    private static final long serialVersionUID = 1957118246209506994L;

    private UsageExitableState(AbstractState pWrappedElement, UsageState state) {
      super(pWrappedElement, state);
    }

    public UsageExitableState(UsageState state) {
      this(state.getWrappedState(), state);
    }

    @Override
    public UsageExitableState clone(final AbstractState wrapped) {
      return new UsageExitableState(wrapped, this);
    }

    @Override
    public UsageExitableState reduce(final AbstractState wrapped) {
      return new UsageExitableState(wrapped, this);
    }

    public boolean isExitable() {
      return true;
    }
  }*/

  public static class StateStatistics {
    private StatTimer joinTimer = new StatTimer("Time for joining");
    private StatTimer lessTimer = new StatTimer("Time for cover check");
    private StatTimer addRecentUsagesTimer = new StatTimer("Time for adding recent usages");

    private final StorageStatistics storageStats;

    public StateStatistics(StorageStatistics stats) {
      storageStats = Objects.requireNonNull(stats);
    }

    public void printStatistics(StatisticsWriter out) {
      out.spacer().put(joinTimer).put(addRecentUsagesTimer).put(lessTimer);

      storageStats.printStatistics(out);
    }
  }

  public static UsageState get(AbstractState state) {
    return AbstractStates.extractStateByType(state, UsageState.class);
  }

  @Override
  public UsageState join(UsageState pOther) {
    throw new UnsupportedOperationException(
        "Join is not supported for usage states, use merge operator");
  }

  Object readResolve() {
    return new UsageState(
        getWrappedState(),
        PathCopyingPersistentTreeMap.of(),
        recentUsages,
        functionContainer,
        new StateStatistics(functionContainer.getStatistics()),
        isExitState);
  }
}
