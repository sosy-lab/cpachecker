// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2022 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.core.algorithm.distributed_summaries.distributed_cpa;

import org.sosy_lab.cpachecker.core.algorithm.distributed_summaries.distributed_cpa.operators.deserialize.DeserializeOperator;
import org.sosy_lab.cpachecker.core.algorithm.distributed_summaries.distributed_cpa.operators.deserialize.DeserializePrecisionOperator;
import org.sosy_lab.cpachecker.core.algorithm.distributed_summaries.distributed_cpa.operators.deserialize.NoPrecisionDeserializeOperator;
import org.sosy_lab.cpachecker.core.algorithm.distributed_summaries.distributed_cpa.operators.proceed.ProceedOperator;
import org.sosy_lab.cpachecker.core.algorithm.distributed_summaries.distributed_cpa.operators.serialize.NoPrecisionSerializeOperator;
import org.sosy_lab.cpachecker.core.algorithm.distributed_summaries.distributed_cpa.operators.serialize.SerializeOperator;
import org.sosy_lab.cpachecker.core.algorithm.distributed_summaries.distributed_cpa.operators.serialize.SerializePrecisionOperator;
import org.sosy_lab.cpachecker.core.algorithm.distributed_summaries.distributed_cpa.operators.verification_condition.ViolationConditionOperator;
import org.sosy_lab.cpachecker.core.algorithm.distributed_summaries.exchange.DssMessagePayload;
import org.sosy_lab.cpachecker.core.interfaces.AbstractState;
import org.sosy_lab.cpachecker.core.interfaces.ConfigurableProgramAnalysis;
import org.sosy_lab.cpachecker.core.interfaces.Precision;

public interface DistributedConfigurableProgramAnalysis extends ConfigurableProgramAnalysis {

  /**
   * Operator that knows how to serialize the abstract states from {@link
   * DistributedConfigurableProgramAnalysis#getAbstractStateClass()}.
   *
   * @return Serialize operator for a distributed CPA.
   */
  SerializeOperator getSerializeOperator();

  default SerializePrecisionOperator getSerializePrecisionOperator() {
    return new NoPrecisionSerializeOperator();
  }

  /**
   * Operator that knows how to deserialize a message to abstract states of type {@link
   * DistributedConfigurableProgramAnalysis#getAbstractStateClass()}.
   *
   * @return Deserialize operator for a distributed CPA.
   */
  DeserializeOperator getDeserializeOperator();

  default DeserializePrecisionOperator getDeserializePrecisionOperator() {
    return new NoPrecisionDeserializeOperator();
  }

  /**
   * Operator that decides whether to proceed with an analysis based on the given message.
   *
   * @return Proceed operator for a distributed CPA.
   */
  ProceedOperator getProceedOperator();

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

  boolean isTop(AbstractState pAbstractState);

  ViolationConditionOperator getViolationConditionOperator();

  /**
   * Check whether this distributed CPA can work with {@code pClass}.
   *
   * @param pClass Decide whether this DCPA can work with this class.
   * @returnwhether this DCPA accepts {@code pClass}
   */
  default boolean doesOperateOn(Class<? extends AbstractState> pClass) {
    return getAbstractStateClass().isAssignableFrom(pClass);
  }

  default DssMessagePayload serialize(AbstractState pAbstractState, Precision pPrecision) {
    return DssMessagePayload.builder()
        .addAllEntries(getSerializeOperator().serialize(pAbstractState))
        .addAllEntries(getSerializePrecisionOperator().serializePrecision(pPrecision))
        .buildPayload();
  }
}
