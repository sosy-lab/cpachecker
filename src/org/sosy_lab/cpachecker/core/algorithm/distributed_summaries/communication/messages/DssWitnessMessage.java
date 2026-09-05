// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2026 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.core.algorithm.distributed_summaries.communication.messages;

/**
 * Message sent by analysis workers carrying witness information: either the ARG states of all
 * preconditions after a TRUE result ({@link WitnessType#CORRECTNESS}), or the violation path after
 * a FALSE result ({@link WitnessType#VIOLATION}), differentiated by the field {@link
 * DssMessageKeys#WITNESS_TYPE}
 */
public sealed interface DssWitnessMessage extends DssMessage
    permits DssCorrectnessWitnessMessage, DssViolationWitnessMessage {

  enum WitnessType {
    CORRECTNESS,
    VIOLATION
  }

  @Override
  default DssMessageType getType() {
    return DssMessageType.WITNESS;
  }

  WitnessType getWitnessType();
}
