// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2007-2020 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.cpa.constraints.util;

import static com.google.common.collect.FluentIterable.from;

import com.google.common.collect.Iterables;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import org.sosy_lab.common.configuration.Configuration;
import org.sosy_lab.common.configuration.InvalidConfigurationException;
import org.sosy_lab.common.configuration.Option;
import org.sosy_lab.common.configuration.Options;
import org.sosy_lab.cpachecker.cpa.constraints.ConstraintsStatistics;
import org.sosy_lab.cpachecker.cpa.constraints.constraint.Constraint;
import org.sosy_lab.cpachecker.cpa.constraints.constraint.ConstraintTrivialityChecker;
import org.sosy_lab.cpachecker.cpa.constraints.domain.ConstraintsState;
import org.sosy_lab.cpachecker.cpa.value.ValueAnalysisState;
import org.sosy_lab.cpachecker.cpa.value.symbolic.type.SymbolicIdentifier;
import org.sosy_lab.cpachecker.cpa.value.symbolic.type.SymbolicValue;
import org.sosy_lab.cpachecker.cpa.value.symbolic.util.SymbolicValues;

/**
 * Simplifier for {@link ConstraintsState}s. Provides different methods for simplifying a <code>
 * ConstraintsState</code> through {@link #simplify(ConstraintsState, ValueAnalysisState)}.
 */
@Options(prefix = "cpa.constraints")
public class StateSimplifier {

  @Option(
      description =
          "Whether to remove trivial constraints from constraints states during simplification",
      secure = true)
  private boolean removeTrivial = false;

  @Option(
      description =
          "Whether to remove constraints that can't add any more information to"
              + "analysis during simplification",
      secure = true)
  private boolean removeOutdated = true;

  private final ConstraintsStatistics stats;

  public StateSimplifier(final Configuration pConfig, final ConstraintsStatistics pStats)
      throws InvalidConfigurationException {
    pConfig.inject(this);

    stats = pStats;
  }

  /**
   * Simplifies the given {@link ConstraintsState}. Applies different simplifications to it.
   *
   * <p>The resulting state will hold the same amount of information as the given state.
   *
   * @param pState the state to simplify
   */
  public ConstraintsState simplify(
      final ConstraintsState pState, final ValueAnalysisState pValueState) {
    ConstraintsState newState = pState.copyOf();

    if (removeTrivial) {
      try {
        stats.trivialRemovalTime.start();
        removeTrivialConstraints(pState);
      } finally {
        stats.trivialRemovalTime.stop();
      }
    }

    if (removeOutdated) {
      try {
        stats.outdatedRemovalTime.start();
        removeOutdatedConstraints(pState, pValueState);
      } finally {
        stats.outdatedRemovalTime.stop();
      }
    }

    return newState;
  }

  /** Removes all trivial constraints from the given state. */
  public void removeTrivialConstraints(final ConstraintsState pState) {
    int sizeBefore = pState.size();

    pState.removeIf(this::isTrivial);

    stats.removedTrivial.setNextValue(sizeBefore - pState.size());
  }

  private boolean isTrivial(Constraint pConstraint) {
    final ConstraintTrivialityChecker trivialityChecker = new ConstraintTrivialityChecker();

    return pConstraint.accept(trivialityChecker);
  }

  /**
   * Removes all constraints that cannot influence the CPA's behaviour anymore. If a constraint
   * contains only symbolic identifiers that are not assigned to a memory location anymore and that
   * does not constrain another symbolic identifier that still can influence the CPA's behaviour, we
   * can safely remove the constraint.
   *
   * @param pState the state to remove constraints of, if possible
   * @param pValueState the value state to use for checking whether a symbolic identifier occurs in
   *     a memory location's assignment
   */
  public void removeOutdatedConstraints(
      final ConstraintsState pState, final ValueAnalysisState pValueState) {
    int sizeBefore = pState.size();
    final Map<ActivityInfo, Set<ActivityInfo>> symIdActivity = getInitialActivityMap(pState);
    final Set<SymbolicIdentifier> symbolicValues = getExistingSymbolicIds(pValueState);

    for (Entry<ActivityInfo, Set<ActivityInfo>> e : symIdActivity.entrySet()) {
      final ActivityInfo s = e.getKey();
      final SymbolicIdentifier currId = s.getIdentifier();

      switch (s.getActivity()) {
        case DELETED:
          pState.removeAll(s.getUsingConstraints());
          break;
        case ACTIVE:
        case UNUSED:
          if (!symbolicValues.contains(currId)) {
            boolean canBeRemoved;
            if (s.getUsingConstraints().size() < 2) {
              // the symbolic identifier only occurs in one constraint and is not active,
              // so it does not constrain any currently existing symbolic identifier
              canBeRemoved = true;
            } else {

              s.disable();
              Set<ActivityInfo> parent = new HashSet<>();
              parent.add(s);
              canBeRemoved =
                  removeOutdatedConstraints0(symIdActivity, symbolicValues, e.getValue(), parent);
            }

            if (canBeRemoved) {
              s.markDeleted();
              pState.removeAll(s.getUsingConstraints());
            }
          }
          break;
        default:
          throw new AssertionError("Unhandled activity type: " + s.getActivity());
      }
    }

    stats.removedOutdated.setNextValue(sizeBefore - pState.size());
  }

  private boolean removeOutdatedConstraints0(
      final Map<ActivityInfo, Set<ActivityInfo>> pSymIdActivity,
      final Set<SymbolicIdentifier> pExistingValues,
      final Set<ActivityInfo> targets,
      final Set<ActivityInfo> parents) {

    for (ActivityInfo t : targets) {
      if (pExistingValues.contains(t.getIdentifier())) {
        return false;
      } else if (t.getActivity() == Activity.ACTIVE) {
        t.disable();
      }

      switch (t.getActivity()) {
        case ACTIVE:
          return false;
        case DELETED:
          // do nothing, we already know that this target is not needed
          break;
        case UNUSED:
          final Set<ActivityInfo> dependents = pSymIdActivity.get(t);
          dependents.removeAll(parents);

          // remove all infos already known as deletable
          Iterables.removeIf(
              dependents, pActivityInfo -> pActivityInfo.getActivity() == Activity.DELETED);

          if (dependents.isEmpty()) {
            t.markDeleted();
            return true;
          }

          parents.add(t);
          boolean success =
              removeOutdatedConstraints0(pSymIdActivity, pExistingValues, dependents, parents);

          if (!success) {
            return false;
          } else {
            t.markDeleted();
          }
          break;
        default:
          throw new AssertionError("Unhandled state of ActivityInfo: " + t.getActivity());
      }
    }

    assert allDeleted(targets);
    return true;
  }

  private boolean allDeleted(final Set<ActivityInfo> pInfos) {
    for (ActivityInfo i : pInfos) {
      if (i.getActivity() != Activity.DELETED) {
        return false;
      }
    }

    return true;
  }

  private Set<SymbolicIdentifier> getExistingSymbolicIds(final ValueAnalysisState pValueState) {
    return from(pValueState.getConstants())
        .transform(e -> e.getValue().getValue())
        .filter(SymbolicValue.class)
        .transformAndConcat(SymbolicValues::getContainedSymbolicIdentifiers)
        .copyInto(new HashSet<>());
  }

  private Map<ActivityInfo, Set<ActivityInfo>> getInitialActivityMap(
      final ConstraintsState pState) {

    Map<ActivityInfo, Set<ActivityInfo>> activityMap = new HashMap<>();

    for (Constraint c : pState) {
      final Collection<SymbolicIdentifier> usedIdentifiers =
          SymbolicValues.getContainedSymbolicIdentifiers(c);

      for (SymbolicIdentifier i : usedIdentifiers) {
        Set<SymbolicIdentifier> otherIdentifiers = new HashSet<>(usedIdentifiers);
        otherIdentifiers.remove(i);

        final Set<ActivityInfo> dependents = createActivitySet(otherIdentifiers, c);

        final ActivityInfo currActivityInfo = ActivityInfo.getInfo(i, c);

        Set<ActivityInfo> existingDependents = activityMap.get(currActivityInfo);

        if (existingDependents == null) {
          activityMap.put(currActivityInfo, dependents);
        } else {
          existingDependents.addAll(dependents);
        }
      }
    }

    return activityMap;
  }

  private Set<ActivityInfo> createActivitySet(
      final Set<SymbolicIdentifier> pIdentifiers, final Constraint pUsingConstraint) {
    final Set<ActivityInfo> activitySet = new HashSet<>();

    for (SymbolicIdentifier i : pIdentifiers) {
      activitySet.add(ActivityInfo.getInfo(i, pUsingConstraint));
    }

    return activitySet;
  }

  private enum Activity {
    ACTIVE,
    UNUSED,
    DELETED
  }

  private static final class ActivityInfo {
    private static final Map<SymbolicIdentifier, ActivityInfo> infos = new HashMap<>();

    private final SymbolicIdentifier identifier;
    private Set<Constraint> usingConstraints;
    private Activity activity;

    static ActivityInfo getInfo(
        final SymbolicIdentifier pIdentifier, final Constraint pConstraint) {

      if (infos.containsKey(pIdentifier)) {
        ActivityInfo info = infos.get(pIdentifier);
        info.usingConstraints.add(pConstraint);

        // Activity might have been marked as deleted in a previous iteration of CEGAR, so we have
        // to set it back
        info.enable();

        return info;

      } else {
        ActivityInfo info = new ActivityInfo(pIdentifier, pConstraint);
        infos.put(pIdentifier, info);

        return info;
      }
    }

    /** Initializes activity info for symbolic identifier as 'active'. */
    private ActivityInfo(final SymbolicIdentifier pIdentifier, final Constraint pConstraint) {
      identifier = pIdentifier;
      activity = Activity.ACTIVE;

      usingConstraints = new HashSet<>();
      usingConstraints.add(pConstraint);
    }

    public SymbolicIdentifier getIdentifier() {
      return identifier;
    }

    public Set<Constraint> getUsingConstraints() {
      return usingConstraints;
    }

    public Activity getActivity() {
      return activity;
    }

    public void disable() {
      activity = Activity.UNUSED;
    }

    public void enable() {
      activity = Activity.ACTIVE;
    }

    public void markDeleted() {
      activity = Activity.DELETED;
    }

    @Override
    public boolean equals(Object o) {
      if (this == o) {
        return true;
      }
      if (o == null || getClass() != o.getClass()) {
        return false;
      }

      ActivityInfo that = (ActivityInfo) o;

      return SymbolicValues.representSameSymbolicMeaning(identifier, that.identifier);
    }

    @Override
    public int hashCode() {
      return identifier.hashCode();
    }
  }
}
