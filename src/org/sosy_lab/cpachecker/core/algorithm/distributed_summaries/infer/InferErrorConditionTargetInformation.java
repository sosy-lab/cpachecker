// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2021 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.core.algorithm.distributed_summaries.infer;

import org.sosy_lab.cpachecker.core.algorithm.distributed_summaries.exchange.BlockSummaryMessagePayload;
import org.sosy_lab.cpachecker.core.interfaces.Targetable.TargetInformation;

/**
 * Distributed analyses use independent blocks for analyzing simultaneously. This kind of 
 * target means that an error condition was satisfiable while using infer analysis
 */
public class InferErrorConditionTargetInformation implements TargetInformation {


  public InferErrorConditionTargetInformation() {}

  public BlockSummaryMessagePayload getErrorConditionPayload(){
    BlockSummaryMessagePayload.Builder payloadBuilder = BlockSummaryMessagePayload.builder();
    return payloadBuilder.buildPayload();
  }


  @Override
  public String toString() {
    return "Reached an error condition while using infer analysis";
  }
}

