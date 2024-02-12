// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2024 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.cpa.automaton;

import com.google.common.collect.ArrayListMultimap;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ListMultimap;
import java.io.IOException;
import java.io.Writer;
import java.nio.charset.Charset;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.logging.Level;
import org.sosy_lab.common.ShutdownNotifier;
import org.sosy_lab.common.configuration.Configuration;
import org.sosy_lab.common.configuration.FileOption;
import org.sosy_lab.common.configuration.InvalidConfigurationException;
import org.sosy_lab.common.configuration.Option;
import org.sosy_lab.common.configuration.Options;
import org.sosy_lab.common.io.IO;
import org.sosy_lab.common.log.LogManager;
import org.sosy_lab.cpachecker.cfa.CFA;
import org.sosy_lab.cpachecker.cfa.CProgramScope;
import org.sosy_lab.cpachecker.cfa.DummyScope;
import org.sosy_lab.cpachecker.cfa.ast.AExpression;
import org.sosy_lab.cpachecker.cfa.parser.Scope;
import org.sosy_lab.cpachecker.cpa.automaton.AutomatonGraphmlParser.WitnessParseException;
import org.sosy_lab.cpachecker.exceptions.UnrecognizedCodeException;
import org.sosy_lab.cpachecker.util.Pair;
import org.sosy_lab.cpachecker.util.expressions.ToCExpressionVisitor;
import org.sosy_lab.cpachecker.util.invariantwitness.InvariantExchangeFormatTransformer;
import org.sosy_lab.cpachecker.util.invariantwitness.exchange.InvariantStoreUtil;
import org.sosy_lab.cpachecker.util.invariantwitness.exchange.model.AbstractEntry;
import org.sosy_lab.cpachecker.util.invariantwitness.exchange.model.ViolationSequenceEntry;
import org.sosy_lab.cpachecker.util.invariantwitness.exchange.model.records.common.SegmentRecord;
import org.sosy_lab.cpachecker.util.invariantwitness.exchange.model.records.common.WaypointRecord;
import org.sosy_lab.cpachecker.util.invariantwitness.exchange.model.records.common.WaypointRecord.WaypointAction;
import org.sosy_lab.cpachecker.util.invariantwitness.exchange.model.records.common.WaypointRecord.WaypointType;

@Options(prefix = "witness")
class AutomatonWitnessV2ParserCommon {

  @Option(secure = true, description = "File for exporting the witness automaton in DOT format.")
  @FileOption(FileOption.Type.OUTPUT_FILE)
  private Path automatonDumpFile = null;

  @Option(
      secure = true,
      name = "invariantsSpecificationAutomaton",
      description =
          "Validate correctness witness by specifying an invariants specification automaton")
  InvariantsSpecificationAutomatonBuilder invariantsSpecAutomaton =
      InvariantsSpecificationAutomatonBuilder.NO_ISA;

  private ListMultimap<String, Integer> lineOffsetsByFile = null;
  final CFA cfa;
  final LogManager logger;
  final Configuration config;
  final ShutdownNotifier shutdownNotifier;

  final InvariantExchangeFormatTransformer transformer;

  AutomatonWitnessV2ParserCommon(
      Configuration pConfig, LogManager pLogger, ShutdownNotifier pShutdownNotifier, CFA pCFA)
      throws InvalidConfigurationException {
    pConfig.inject(this, AutomatonWitnessV2ParserCommon.class);
    logger = pLogger;
    cfa = pCFA;
    config = pConfig;
    shutdownNotifier = pShutdownNotifier;
    transformer = new InvariantExchangeFormatTransformer(pConfig, pLogger, pShutdownNotifier, pCFA);
  }

  ListMultimap<String, Integer> getLineOffsetsByFile() throws IOException {
    if (lineOffsetsByFile == null) {
      lineOffsetsByFile = ArrayListMultimap.create();
      lineOffsetsByFile.putAll(InvariantStoreUtil.getLineOffsetsByFile(cfa.getFileNames()));
    }

    return lineOffsetsByFile;
  }

  private static AutomatonBoolExpr not(AutomatonBoolExpr pA) {
    if (pA.equals(AutomatonBoolExpr.TRUE)) {
      return AutomatonBoolExpr.FALSE;
    }
    if (pA.equals(AutomatonBoolExpr.FALSE)) {
      return AutomatonBoolExpr.TRUE;
    }
    return new AutomatonBoolExpr.Negation(pA);
  }

  static AutomatonBoolExpr createViolationAssertion() {
    return not(new AutomatonBoolExpr.ALLCPAQuery(AutomatonState.INTERNAL_STATE_IS_TARGET_PROPERTY));
  }

  static String getStateName(int i) {
    return "S" + i;
  }

  List<Pair<WaypointRecord, List<WaypointRecord>>> segmentize(List<AbstractEntry> pEntries)
      throws InvalidConfigurationException {
    List<Pair<WaypointRecord, List<WaypointRecord>>> segments = new ArrayList<>();
    WaypointRecord latest = null;
    int numTargetWaypoints = 0;
    for (AbstractEntry entry : pEntries) {
      if (entry instanceof ViolationSequenceEntry violationEntry) {

        List<WaypointRecord> avoids = new ArrayList<>();
        for (SegmentRecord segmentRecord : violationEntry.getContent()) {
          for (WaypointRecord waypoint : segmentRecord.getSegment()) {
            latest = waypoint;
            numTargetWaypoints += waypoint.getType().equals(WaypointType.TARGET) ? 1 : 0;
            if (waypoint.getAction().equals(WaypointAction.AVOID)) {
              avoids.add(waypoint);
              continue;
            } else if (waypoint.getAction().equals(WaypointAction.FOLLOW)) {
              segments.add(Pair.of(waypoint, avoids));
              avoids = new ArrayList<>();
              continue;
            }
          }
        }
        break; // for now just take the first ViolationSequenceEntry in the witness V2
      }
    }
    checkTargetIsAtEnd(latest, numTargetWaypoints);
    return segments;
  }

  void handleConstraint(
      String constraint,
      Optional<String> resultFunction,
      int line,
      AutomatonTransition.Builder builder)
      throws InterruptedException, WitnessParseException {
    try {
      Scope defaultScope =
          switch (cfa.getLanguage()) {
            case C -> new CProgramScope(cfa, logger);
            default -> DummyScope.getInstance();
          };
      Scope scope =
          AutomatonWitnessV2ParserUtils.determineScopeForLine(
              resultFunction, null, line, defaultScope);
      AExpression exp =
          transformer
              .createExpressionTreeFromString(resultFunction, constraint, line, null, scope)
              .accept(new ToCExpressionVisitor(cfa.getMachineModel(), logger));
      builder.withAssumptions(ImmutableList.of(exp));
    } catch (UnrecognizedCodeException e) {
      throw new WitnessParseException("Could not parse string into valid expression", e);
    }
  }

  private void checkTargetIsAtEnd(WaypointRecord latest, int numTargetWaypoints)
      throws InvalidConfigurationException {
    switch (numTargetWaypoints) {
      case 0:
        logger.log(Level.WARNING, "No target waypoint in witness V2!");
        break;
      case 1:
        if (latest != null && !latest.getType().equals(WaypointType.TARGET)) {
          throw new InvalidConfigurationException(
              "Target waypoint is not at the end in witness V2!");
        }
        break;
      default:
        throw new InvalidConfigurationException("More than one target waypoint in witness V2!");
    }
  }

  void dumpAutomatonIfRequested(Automaton automaton) {
    if (automatonDumpFile != null) {
      try (Writer w = IO.openOutputFile(automatonDumpFile, Charset.defaultCharset())) {
        automaton.writeDotFile(w);
      } catch (IOException e) {
        logger.logUserException(Level.WARNING, e, "Could not write the automaton to DOT file");
      }
      Path automatonFile =
          automatonDumpFile.resolveSibling(automatonDumpFile.getFileName() + ".spc");
      try (Writer w = IO.openOutputFile(automatonFile, Charset.defaultCharset())) {
        w.write(automaton.toString());
      } catch (IOException e) {
        logger.logUserException(Level.WARNING, e, "Could not write the automaton to DOT file");
      }
    }
  }
}
