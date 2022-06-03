// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2007-2020 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.core.defaults;

import com.google.common.base.Preconditions;
import org.checkerframework.checker.nullness.qual.Nullable;
import org.sosy_lab.cpachecker.core.interfaces.AbstractDomain;
import org.sosy_lab.cpachecker.core.interfaces.ConfigurableProgramAnalysis;
import org.sosy_lab.cpachecker.core.interfaces.MergeOperator;
import org.sosy_lab.cpachecker.core.interfaces.StopOperator;
import org.sosy_lab.cpachecker.core.interfaces.TransferRelation;

/**
 * This is an abstract class for building CPAs. It uses the flat lattice domain if no other domain
 * is given, and the standard implementations for merge-(sep|join) and stop-sep.
 */
public abstract class AbstractCPA implements ConfigurableProgramAnalysis {

  private final AbstractDomain abstractDomain;

  /* The operators can be overridden in sub-classes. Thus we allow Null as possible assignment. */
  private final @Nullable String mergeType;
  private final @Nullable String stopType;
  private final @Nullable TransferRelation transferRelation;

  protected AbstractCPA(String mergeType, String stopType, @Nullable TransferRelation transfer) {
    this(mergeType, stopType, new FlatLatticeDomain(), transfer);
  }

  /**
   * When using this constructor, you have to override the methods for getting Merge- and
   * StopOperator. This can be useful for cases where the operators are configurable or are
   * initialized lazily.
   */
  protected AbstractCPA(AbstractDomain domain, TransferRelation transfer) {
    abstractDomain = domain;
    mergeType = null;
    stopType = null;
    transferRelation = transfer;
  }

  /** Use this constructor, if Merge- and StopOperator are fixed. */
  protected AbstractCPA(
      String mergeType,
      String stopType,
      AbstractDomain domain,
      @Nullable TransferRelation transfer) {
    abstractDomain = domain;
    this.mergeType = mergeType;
    this.stopType = stopType;
    transferRelation = transfer;
  }

  @Override
  public AbstractDomain getAbstractDomain() {
    return abstractDomain;
  }

  @Override
  public MergeOperator getMergeOperator() {
    return buildMergeOperator(Preconditions.checkNotNull(mergeType));
  }

  protected MergeOperator buildMergeOperator(String pMergeType) {
    switch (pMergeType.toUpperCase()) {
      case "SEP":
        return MergeSepOperator.getInstance();

      case "JOIN":
        return new MergeJoinOperator(getAbstractDomain());

      default:
        throw new AssertionError("unknown merge operator");
    }
  }

  @Override
  public StopOperator getStopOperator() {
    return buildStopOperator(Preconditions.checkNotNull(stopType));
  }

  protected StopOperator buildStopOperator(String pStopType) throws AssertionError {
    switch (pStopType.toUpperCase()) {
      case "SEP": // state is LESS_OR_EQUAL to any reached state
        return new StopSepOperator(getAbstractDomain());

      case "JOIN": // state is LESS_OR_EQUAL to the union of all reached state
        return new StopJoinOperator(getAbstractDomain());

      case "NEVER": // always FALSE
        return new StopNeverOperator();

      case "ALWAYS": // always TRUE
        return new StopAlwaysOperator();

      case "EQUALS": // state is EQUAL to any reached state
        return new StopEqualsOperator();

      default:
        throw new AssertionError("unknown stop operator");
    }
  }

  @Override
  public TransferRelation getTransferRelation() {
    return Preconditions.checkNotNull(transferRelation);
  }
}
