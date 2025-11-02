// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2025 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.util.k3witnessexport;

import com.google.common.base.Verify;
import com.google.common.collect.ImmutableList;
import java.util.List;
import org.sosy_lab.common.log.LogManager;
import org.sosy_lab.cpachecker.cfa.CFA;
import org.sosy_lab.cpachecker.cfa.ast.k3.K3AnnotateTagCommand;
import org.sosy_lab.cpachecker.cfa.model.k3.K3CfaMetadata;
import org.sosy_lab.cpachecker.core.counterexample.ConcreteStatePath;
import org.sosy_lab.cpachecker.core.counterexample.CounterexampleInfo;

@SuppressWarnings("unused")
public class CounterexampleToK3WitnessExport {
  @SuppressWarnings("unused")
  private final LogManager logger;

  private final K3CfaMetadata k3Metadata;

  public CounterexampleToK3WitnessExport(LogManager pLogger, CFA pCFA) {
    Verify.verify(
        pCFA.getMetadata().getK3CfaMetadata().isPresent(),
        "K3 metadata must be present in CFA in order to export a K3 witness.");
    k3Metadata = pCFA.getMetadata().getK3CfaMetadata().orElseThrow();
    logger = pLogger;
  }

  public List<K3AnnotateTagCommand> generateWitnessCommands(CounterexampleInfo pCounterexample) {
    ConcreteStatePath concretePath =
        pCounterexample.getCFAPathWithAssignments().getConcreteStatePath().orElseThrow();

    return ImmutableList.of();
  }
}
