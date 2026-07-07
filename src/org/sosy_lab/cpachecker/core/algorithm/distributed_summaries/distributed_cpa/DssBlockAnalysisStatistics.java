// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2022 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.core.algorithm.distributed_summaries.distributed_cpa;

import org.sosy_lab.cpachecker.util.statistics.StatCounter;

public class DssBlockAnalysisStatistics {

  private final StatCounter serializationCount;
  private final StatCounter deserializationCount;

  private final DssThreadCPUTimer serializationTime;
  private final DssThreadCPUTimer deserializationTime;
  private final StatCounter proceedCount;
  private final DssThreadCPUTimer proceedTime;

  public DssBlockAnalysisStatistics(String pId) {
    serializationCount = new StatCounter("Serialization Count " + pId);
    deserializationCount = new StatCounter("Deserialization Count " + pId);
    proceedCount = new StatCounter("Proceed Count " + pId);

    serializationTime = new DssThreadCPUTimer("Serialization Time " + pId);
    deserializationTime = new DssThreadCPUTimer("Deserialization Time " + pId);
    proceedTime = new DssThreadCPUTimer("Proceed Time " + pId);
  }

  public StatCounter getDeserializationCount() {
    return deserializationCount;
  }

  public StatCounter getSerializationCount() {
    return serializationCount;
  }

  public DssThreadCPUTimer getDeserializationTime() {
    return deserializationTime;
  }

  public StatCounter getProceedCount() {
    return proceedCount;
  }

  public DssThreadCPUTimer getProceedTime() {
    return proceedTime;
  }

  public DssThreadCPUTimer getSerializationTime() {
    return serializationTime;
  }
}
