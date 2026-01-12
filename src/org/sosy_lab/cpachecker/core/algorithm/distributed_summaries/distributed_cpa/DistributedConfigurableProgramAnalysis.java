// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2022 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.core.algorithm.distributed_summaries.distributed_cpa;

import org.sosy_lab.cpachecker.core.algorithm.distributed_summaries.distributed_cpa.operators.combine.CombineOperator;
import org.sosy_lab.cpachecker.core.algorithm.distributed_summaries.distributed_cpa.operators.combine.CombinePrecisionOperator;
import org.sosy_lab.cpachecker.core.algorithm.distributed_summaries.distributed_cpa.operators.coverage.CoverageOperator;
import org.sosy_lab.cpachecker.core.algorithm.distributed_summaries.distributed_cpa.operators.deserialize.DeserializeOperator;
import org.sosy_lab.cpachecker.core.algorithm.distributed_summaries.distributed_cpa.operators.deserialize.DeserializePrecisionOperator;
import org.sosy_lab.cpachecker.core.algorithm.distributed_summaries.distributed_cpa.operators.proceed.ProceedOperator;
import org.sosy_lab.cpachecker.core.algorithm.distributed_summaries.distributed_cpa.operators.serialize.SerializeOperator;
import org.sosy_lab.cpachecker.core.algorithm.distributed_summaries.distributed_cpa.operators.serialize.SerializePrecisionOperator;
import org.sosy_lab.cpachecker.core.algorithm.distributed_summaries.distributed_cpa.operators.verification_condition.ViolationConditionOperator;
import org.sosy_lab.cpachecker.core.interfaces.AbstractState;
import org.sosy_lab.cpachecker.core.interfaces.ConfigurableProgramAnalysis;
import org.sosy_lab.cpachecker.core.interfaces.Precision;
import org.sosy_lab.cpachecker.exceptions.CPAException;

/**
 * Extension of ConfigurableProgramAnalysis with serialization and deserialization capabilities for
 * distributed analysis.
 *
 * <p>DistributedConfigurableProgramAnalysis enables standard CPAs to participate in DSS by
 * providing operators to serialize abstract states into messages that can be transmitted between
 * workers, and to deserialize received messages back into abstract states to start a new analysis
 * with this information as initial state(s).
 */
public interface DistributedConfigurableProgramAnalysis extends ConfigurableProgramAnalysis {

  record StateAndPrecision(AbstractState state, Precision precision) {}

  String MULTIPLE_STATES_KEY = "states";

  /**
   * Operator that knows how to serialize the abstract states from {@link
   * DistributedConfigurableProgramAnalysis#getAbstractStateClass()}.
   *
   * @return Serialize operator for a distributed CPA.
   */
  SerializeOperator getSerializeOperator();

  /**
   * Operator that knows how to deserialize a message to abstract states of type {@link
   * DistributedConfigurableProgramAnalysis#getAbstractStateClass()}.
   *
   * @return Deserialize operator for a distributed CPA.
   */
  DeserializeOperator getDeserializeOperator();

  SerializePrecisionOperator getSerializePrecisionOperator();

  DeserializePrecisionOperator getDeserializePrecisionOperator();

  CombinePrecisionOperator getCombinePrecisionOperator();

  /**
   * Operator that decides whether to proceed with an analysis based on the given message.
   *
   * @return Proceed operator for a distributed CPA.
   */
  ProceedOperator getProceedOperator();

  ViolationConditionOperator getViolationConditionOperator();

  CoverageOperator getCoverageOperator();

  CombineOperator getCombineOperator();

  /**
   * The abstract state this distributed analysis works n.
   *
   * @return Parent class of all abstract states that this distributed CPA can handle.
   */
  Class<? extends AbstractState> getAbstractStateClass();

  /**
   * Returns the underlying {@link ConfigurableProgramAnalysis} of this distributed analysis.
   *
   * @return underlying CPA
   */
  ConfigurableProgramAnalysis getCPA();

  /**
   * Check whether the given abstract state is the most general block entry state. Meaning that it
   * is the state that is used to represent the entry of a block from where the whole state space of
   * the block can be reached.
   *
   * <p>For analysis like the {@link org.sosy_lab.cpachecker.cpa.predicate.PredicateCPA} this is the
   * top element, for location-based CPAs like the {@link
   * org.sosy_lab.cpachecker.cpa.location.LocationCPA} this is the {@link
   * org.sosy_lab.cpachecker.cpa.location.LocationState} with the {@link
   * org.sosy_lab.cpachecker.cfa.model.CFANode} of the block entry.
   *
   * @param pAbstractState Abstract state to check whether it is the most general block entry state.
   * @return {@code true} if the given abstract state is the most general block entry state,
   */
  boolean isMostGeneralBlockEntryState(AbstractState pAbstractState) throws CPAException;

  /**
   * Reset the given abstract state to the initial value iff the abstract state is mutable.
   *
   * @param pAbstractState The abstract state to reset.
   * @return the reset abstract state, or the same instance if it is immutable.
   */
  AbstractState reset(AbstractState pAbstractState);

  /**
   * Check whether this distributed CPA can work with {@code pClass}.
   *
   * @param pClass Decide whether this DCPA can work with this class.
   * @return whether this DCPA accepts {@code pClass}
   */
  default boolean doesOperateOn(Class<? extends AbstractState> pClass) {
    return getAbstractStateClass().isAssignableFrom(pClass);
  }
}
