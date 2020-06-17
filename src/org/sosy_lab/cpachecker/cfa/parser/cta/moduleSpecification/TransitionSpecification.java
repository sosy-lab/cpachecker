// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2007-2020 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.cfa.parser.cta.moduleSpecification;

import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Preconditions.checkNotNull;

import com.google.common.base.Optional;
import java.util.Set;

public class TransitionSpecification {
  public String source;
  public String target;
  public Optional<BooleanCondition> guard;
  public Set<String> resetClocks;
  public Optional<String> syncMark;

  private TransitionSpecification(
      String pSource,
      String pTarget,
      Optional<BooleanCondition> pGuard,
      Set<String> pResetClocks,
      Optional<String> pSyncMark) {
    source = pSource;
    target = pTarget;
    guard = pGuard;
    resetClocks = pResetClocks;
    syncMark = pSyncMark;
  }

  public static class Builder {
    private String source;
    private String target;
    private Optional<BooleanCondition> guard;
    private Set<String> resetClocks;
    private Optional<String> syncMark;

    public Builder source(String pSource) {
      source = checkNotNull(pSource);
      checkArgument(!source.isEmpty(), "Empty source states are not allowed");
      return this;
    }

    public Builder target(String pTarget) {
      target = checkNotNull(pTarget);
      checkArgument(!target.isEmpty(), "Empty target states are not allowed");
      return this;
    }

    public Builder guard(Optional<BooleanCondition> pGuard) {
      guard = checkNotNull(pGuard);
      return this;
    }

    public Builder resetClocks(Set<String> pResetClocks) {
      resetClocks = checkNotNull(pResetClocks);
      checkArgument(!resetClocks.contains(""), "Empty variable names are not allowed");
      return this;
    }

    public Builder syncMark(Optional<String> pSyncMark) {
      syncMark = checkNotNull(pSyncMark);
      checkArgument(
          !syncMark.isPresent() || !syncMark.get().isEmpty(), "Empty sync marks are not allowed");
      return this;
    }

    public TransitionSpecification build() {
      checkNotNull(source);
      checkNotNull(target);
      checkNotNull(guard);
      checkNotNull(resetClocks);
      checkNotNull(syncMark);

      return new TransitionSpecification(source, target, guard, resetClocks, syncMark);
    }
  }
}
