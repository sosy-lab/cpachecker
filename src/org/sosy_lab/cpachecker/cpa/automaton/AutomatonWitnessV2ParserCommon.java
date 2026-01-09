// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2024 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.cpa.automaton;

import com.google.common.base.Verify;
import com.google.common.collect.ImmutableList;
import java.io.IOException;
import java.io.Writer;
import java.nio.charset.Charset;
import java.nio.file.Path;
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
import org.sosy_lab.cpachecker.cpa.automaton.AutomatonWitnessV2ParserUtils.InvalidYAMLWitnessException;
import org.sosy_lab.cpachecker.exceptions.UnrecognizedCodeException;
import org.sosy_lab.cpachecker.util.expressions.ToCExpressionVisitor;
import org.sosy_lab.cpachecker.util.yamlwitnessexport.exchange.InvariantExchangeFormatTransformer;
import org.sosy_lab.cpachecker.util.yamlwitnessexport.model.SegmentRecord;
import org.sosy_lab.cpachecker.util.yamlwitnessexport.model.ViolationSequenceEntry;
import org.sosy_lab.cpachecker.util.yamlwitnessexport.model.WaypointRecord;
import org.sosy_lab.cpachecker.util.yamlwitnessexport.model.WaypointRecord.WaypointAction;
import org.sosy_lab.cpachecker.util.yamlwitnessexport.model.WaypointRecord.WaypointType;

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
  private InvariantsSpecificationAutomatonBuilder invariantsSpecAutomaton =
      InvariantsSpecificationAutomatonBuilder.NO_ISA;

  @Option(
      secure = true,
      name = "checkInvariantsHoldForEveryPath",
      description =
          "When the witness is used as an automaton, "
              + "check that the invariants hold for every path in the program")
  protected boolean checkInvariantsHoldForEveryPath = true;

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

  InvariantsSpecificationAutomatonBuilder getInvariantsSpecAutomaton() {
    return invariantsSpecAutomaton;
  }

  record PartitionedWaypoints(
      Optional<WaypointRecord> follow,
      Optional<WaypointRecord> cycle,
      ImmutableList<WaypointRecord> avoids) {
    // Canonical constructor ensures non-null cycle, follow and avoids
    PartitionedWaypoints {
      follow = Optional.ofNullable(follow).orElse(Optional.empty());
      cycle = Optional.ofNullable(cycle).orElse(Optional.empty());
      Verify.verify(follow.isEmpty() || cycle.isEmpty());
      Verify.verify(follow.isPresent() || cycle.isPresent());
      avoids = avoids == null ? ImmutableList.of() : avoids;
    }

    // Constructor that only sets 'follow'
    PartitionedWaypoints(WaypointRecord pFollow, ImmutableList<WaypointRecord> pAvoids) {
      this(Optional.ofNullable(pFollow), Optional.empty(), pAvoids);
    }

    // Constructor that only sets 'cycle'
    PartitionedWaypoints(ImmutableList<WaypointRecord> pAvoids, WaypointRecord pCycle) {
      this(Optional.empty(), Optional.ofNullable(pCycle), pAvoids);
    }
  }

  /**
   * Separate the entries into segments whose waypoints should be passed one after the other
   *
   * @param pViolationEntry the violation entry to segmentize
   * @return the segmentized entries
   */
  ImmutableList<PartitionedWaypoints> segmentize(ViolationSequenceEntry pViolationEntry)
      throws InvalidYAMLWitnessException {
    ImmutableList.Builder<PartitionedWaypoints> segments = new ImmutableList.Builder<>();

    for (SegmentRecord segmentRecord : pViolationEntry.getContent()) {
      boolean containsFollowOrCycle = false;
      ImmutableList.Builder<WaypointRecord> avoids = new ImmutableList.Builder<>();
      for (WaypointRecord waypoint : segmentRecord.getSegment()) {
        if (waypoint.getAction().equals(WaypointAction.AVOID)) {
          avoids.add(waypoint);
        } else {
          if (containsFollowOrCycle) {
            throw new InvalidYAMLWitnessException(
                "Witnesses in version 2.1 can contain at most one follow or cycle waypoint per"
                    + " segment!");
          }
          containsFollowOrCycle = true;
          if (waypoint.getAction().equals(WaypointAction.FOLLOW)) {
            segments.add(new PartitionedWaypoints(waypoint, avoids.build()));
          } else if (waypoint.getAction().equals(WaypointAction.CYCLE)) {
            segments.add(new PartitionedWaypoints(avoids.build(), waypoint));
          }
        }
      }
      if (!containsFollowOrCycle) {
        throw new InvalidYAMLWitnessException(
            "Every segment in witness version 2.1 must contain follow or cycle waypoint!");
      }
    }
    return segments.build();
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

  /**
   * Check that the target waypoint is precisely one and it is at the end of the witness
   *
   * @param pViolationEntry violation entry for which the target waypoint is checked
   * @throws InvalidYAMLWitnessException if the target waypoint is placed wrongly
   */
  protected void checkTarget(ViolationSequenceEntry pViolationEntry)
      throws InvalidYAMLWitnessException {
    WaypointRecord latest = null;
    int numTargetWaypoints = 0;

    for (SegmentRecord segmentRecord : pViolationEntry.getContent()) {
      for (WaypointRecord waypoint : segmentRecord.getSegment()) {
        latest = waypoint;
        numTargetWaypoints += waypoint.getType().equals(WaypointType.TARGET) ? 1 : 0;
      }
    }
    checkTargetIsAtEnd(latest, numTargetWaypoints);
  }

  private void checkTargetIsAtEnd(WaypointRecord pLatest, int pNumTargetWaypoints)
      throws InvalidYAMLWitnessException {
    switch (pNumTargetWaypoints) {
      case 0 -> throw new InvalidYAMLWitnessException("No target waypoint in witness V2!");
      case 1 -> {
        if (pLatest != null && !pLatest.getType().equals(WaypointType.TARGET)) {
          throw new InvalidYAMLWitnessException("Target waypoint is not at the end in witness V2!");
        }
      }
      default ->
          throw new InvalidYAMLWitnessException("More than one target waypoint in witness V2!");
    }
  }

  /**
   * Check whether the witness is valid
   *
   * @param pViolationEntry the violation entry for which the cycle and target waypoints are checked
   * @throws InvalidYAMLWitnessException if the cycle or target waypoints are placed wrongly
   */
  protected void checkCycleOrTargetAtEnd(ViolationSequenceEntry pViolationEntry)
      throws InvalidYAMLWitnessException {
    WaypointRecord latest = null;
    int numTargetWaypoints = 0;
    int numCycleWaypoints = 0;

    for (SegmentRecord segmentRecord : pViolationEntry.getContent()) {
      for (WaypointRecord waypoint : segmentRecord.getSegment()) {
        latest = waypoint;
        if (waypoint.getType().equals(WaypointType.TARGET)) {
          numTargetWaypoints += 1;
          if (numCycleWaypoints > 0) {
            throw new InvalidYAMLWitnessException(
                "Target and cycle waypoints are combined in witness version 2.1!");
          }
        }
        if (waypoint.getAction().equals(WaypointAction.CYCLE)) {
          numCycleWaypoints += 1;
          // The sequence of cycle waypoints is interrupted
        } else if (numCycleWaypoints > 0 && !waypoint.getAction().equals(WaypointAction.AVOID)) {
          throw new InvalidYAMLWitnessException(
              "Cycle waypoints are interrupted with follow waypoints in witness version 2.1!");
        }
      }
    }
    if (numCycleWaypoints == 0) {
      checkTargetIsAtEnd(latest, numTargetWaypoints);
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
