// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2022 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.core.algorithm.giageneration;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.stream.Stream;
import org.sosy_lab.cpachecker.util.automaton.AutomatonGraphmlCommon.WitnessType;

public class GIAParser {

  public static WitnessType getWitnessType(Path pWitness) throws IOException {

    // TODO Think if there is an option to actually build the automaton instead of just searching
    // for the state

    //    // we do not want to log, as we first try to load the GIA:
    //    LogManager nullLogger = LogManager.createNullLogManager();
    //
    //    Scope scope = DummyScope.getInstance();
    //    ShutdownNotifier shutdownNotifier = ShutdownNotifier.createDummy();
    //
    //      List<Automaton> lst =
    //          AutomatonParser.parseAutomatonFile(
    //              pWitness,
    //              config,
    //              nullLogger,
    //              MachineModel.LINUX32, //  Using this is in fact ok, because we are just
    // inerested in the witness type, but anyhow find a nicer way
    //              scope,
    //              Language.C,//  Using this is in fact ok, because we are just inerested in the
    // witness type, but anyhow find a nicer way
    //              shutdownNotifier);
    //      if (lst.isEmpty()) {
    //        throw new InvalidConfigurationException(
    //            "Could not find or load automata in the file " + pWitness.toAbsolutePath());
    //      } else if (lst.size() > 1) {
    //        throw new InvalidConfigurationException(
    //            "Found "
    //                + lst.size()
    //                + " automata in the File "
    //                + pWitness.toAbsolutePath()
    //                + " The CPA can only handle ONE Automaton!");
    //
    //      }

    // FIXME: IMplement something that actually looks at the graph
    boolean qErrorStatePresent;
    try (Stream<String> stream = Files.lines(pWitness)) {

      qErrorStatePresent =
          stream.anyMatch(l -> l.contains("TARGET STATE") || l.contains("UNKNOWN STATE"));
      return qErrorStatePresent ? WitnessType.VIOLATION_WITNESS : WitnessType.CORRECTNESS_WITNESS;
    }
  }
}
