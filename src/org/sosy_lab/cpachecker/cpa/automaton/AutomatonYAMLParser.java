// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2023 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.cpa.automaton;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;
import com.google.common.collect.FluentIterable;
import com.google.common.collect.HashMultimap;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.ListMultimap;
import com.google.common.collect.SetMultimap;
import com.google.common.io.MoreFiles;
import java.io.IOException;
import java.io.InputStream;
import java.io.Writer;
import java.nio.charset.Charset;
import java.nio.file.Path;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Deque;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.function.Predicate;
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
import org.sosy_lab.cpachecker.cfa.CParser;
import org.sosy_lab.cpachecker.cfa.CProgramScope;
import org.sosy_lab.cpachecker.cfa.ast.AExpression;
import org.sosy_lab.cpachecker.cfa.ast.FileLocation;
import org.sosy_lab.cpachecker.cfa.model.CFAEdge;
import org.sosy_lab.cpachecker.cfa.parser.Scope;
import org.sosy_lab.cpachecker.cpa.automaton.AutomatonBoolExpr.CheckCoversLines;
import org.sosy_lab.cpachecker.cpa.automaton.AutomatonGraphmlParser.WitnessParseException;
import org.sosy_lab.cpachecker.cpa.automaton.AutomatonVariable.AutomatonIntVariable;
import org.sosy_lab.cpachecker.cpa.automaton.SourceLocationMatcher.LineMatcher;
import org.sosy_lab.cpachecker.exceptions.UnrecognizedCodeException;
import org.sosy_lab.cpachecker.util.CParserUtils;
import org.sosy_lab.cpachecker.util.CParserUtils.ParserTools;
import org.sosy_lab.cpachecker.util.Pair;
import org.sosy_lab.cpachecker.util.automaton.AutomatonGraphmlCommon.WitnessType;
import org.sosy_lab.cpachecker.util.expressions.ExpressionTree;
import org.sosy_lab.cpachecker.util.expressions.ExpressionTrees;
import org.sosy_lab.cpachecker.util.expressions.ToCExpressionVisitor;
import org.sosy_lab.cpachecker.util.invariantwitness.Invariant;
import org.sosy_lab.cpachecker.util.invariantwitness.exchange.InvariantStoreUtil;
import org.sosy_lab.cpachecker.util.invariantwitness.exchange.model.AbstractEntry;
import org.sosy_lab.cpachecker.util.invariantwitness.exchange.model.InvariantEntry;
import org.sosy_lab.cpachecker.util.invariantwitness.exchange.model.LoopInvariantEntry;
import org.sosy_lab.cpachecker.util.invariantwitness.exchange.model.ViolationSequenceEntry;
import org.sosy_lab.cpachecker.util.invariantwitness.exchange.model.records.common.SegmentRecord;
import org.sosy_lab.cpachecker.util.invariantwitness.exchange.model.records.common.WaypointRecord;
import org.sosy_lab.cpachecker.util.invariantwitness.exchange.model.records.common.WaypointRecord.WaypointAction;
import org.sosy_lab.cpachecker.util.invariantwitness.exchange.model.records.common.WaypointRecord.WaypointType;

@Options(prefix = "witness")
public class AutomatonYAMLParser {

  @Option(
      secure = true,
      name = "invariantsSpecificationAutomaton",
      description =
          "Validate correctness witness by specifying an invariants specification automaton")
  private InvariantsSpecificationAutomatonBuilder invariantsSpecAutomaton =
      InvariantsSpecificationAutomatonBuilder.NO_ISA;

  @Option(secure = true, description = "File for exporting the witness automaton in DOT format.")
  @FileOption(FileOption.Type.OUTPUT_FILE)
  private Path automatonDumpFile = null;

  private final LogManager logger;
  private final Configuration config;
  private final ShutdownNotifier shutdownNotifier;
  private final CFA cfa;
  private final ParserTools parserTools;
  final CParser cparser;

  private Scope scope;

  public AutomatonYAMLParser(
      Configuration pConfig,
      LogManager pLogger,
      ShutdownNotifier pShutdownNotifier,
      CFA pCFA,
      Scope pScope)
      throws InvalidConfigurationException {
    pConfig.inject(this);

    scope = pScope;
    logger = pLogger;
    shutdownNotifier = pShutdownNotifier;
    cfa = pCFA;
    config = pConfig;
    parserTools = ParserTools.create(ExpressionTrees.newFactory(), cfa.getMachineModel(), logger);
    cparser =
        CParser.Factory.getParser(
            /*
             * FIXME: Use normal logger as soon as CParser supports parsing
             * expression trees natively, such that we can remove the workaround
             * with the undefined __CPAchecker_ACSL_return dummy function that
             * causes warnings to be logged.
             */
            LogManager.createNullLogManager(),
            CParser.Factory.getOptions(config),
            cfa.getMachineModel(),
            shutdownNotifier);
  }

  public static List<AbstractEntry> parseYAML(InputStream pInputStream) throws IOException {
    // Currently we assume that an empty witness is also valid. A empty witness corresponds to an
    // empty list of entries. How this should be interpreted to see the type of a witness i.e.
    // Violation or Correctness is unclear
    if (pInputStream.available() == 0) {
      return new ArrayList<>();
    }

    ObjectMapper mapper = new ObjectMapper(new YAMLFactory());
    mapper.findAndRegisterModules();
    List<AbstractEntry> entries =
        Arrays.asList(mapper.readValue(pInputStream, AbstractEntry[].class));
    return entries;
  }

  public static Optional<WitnessType> getWitnessTypeIfYAML(List<AbstractEntry> entries) {
    if (entries.isEmpty()) {
      // We consider an empty witness a valid correctness witness per default
      return Optional.of(WitnessType.CORRECTNESS_WITNESS);
    } else if (FluentIterable.from(entries).allMatch(e -> e instanceof ViolationSequenceEntry)) {
      return Optional.of(WitnessType.VIOLATION_WITNESS);
    } else if (FluentIterable.from(entries).allMatch(e -> !(e instanceof ViolationSequenceEntry))) {
      return Optional.of(WitnessType.CORRECTNESS_WITNESS);
    }
    return Optional.empty();
  }

  public static Optional<WitnessType> getWitnessTypeIfYAML(Path pPath) throws InterruptedException {
    List<AbstractEntry> entries;
    try {
      entries =
          AutomatonGraphmlParser.handlePotentiallyGZippedInput(
              MoreFiles.asByteSource(pPath),
              AutomatonYAMLParser::parseYAML,
              WitnessParseException::new);
    } catch (WitnessParseException e) {
      entries = ImmutableList.of();
    }
    return getWitnessTypeIfYAML(entries);
  }

  public static boolean isYAMLWitness(Path pPath)
      throws InvalidConfigurationException, InterruptedException {
    return AutomatonGraphmlParser.handlePotentiallyGZippedInput(
        MoreFiles.asByteSource(pPath),
        (x) -> {
          try {
            AutomatonYAMLParser.parseYAML(x);
            return true;
          } catch (JsonProcessingException e) {
            return false;
          }
        },
        WitnessParseException::new);
  }

  public Automaton parseAutomatonFile(Path pInputFile)
      throws InvalidConfigurationException, InterruptedException {
    return AutomatonGraphmlParser.handlePotentiallyGZippedInput(
        MoreFiles.asByteSource(pInputFile), this::parseAutomatonFile, WitnessParseException::new);
  }

  private Scope determineScope(
      Optional<String> pExplicitScope,
      Deque<String> pFunctionStack,
      Predicate<FileLocation> pLocationDescriptor,
      Scope pScope) {
    Scope result = pScope;
    if (result instanceof CProgramScope r) {
      result = r.withLocationDescriptor(pLocationDescriptor);
      if (pExplicitScope.isPresent() || !pFunctionStack.isEmpty()) {
        final String functionName;
        if (pExplicitScope.isPresent()) {
          functionName = pExplicitScope.orElseThrow();
        } else {
          functionName = pFunctionStack.peek();
        }
        result = r.withFunctionScope(functionName);
      }
    }
    return result;
  }

  /**
   * Parses a specification from an InputStream and returns the Automata found in the file.
   *
   * @param pInputStream the input stream to parse the witness from.
   * @throws InvalidConfigurationException if the configuration is invalid.
   * @throws IOException if there occurs an IOException while reading from the stream.
   * @return the automata representing the witnesses found in the stream.
   */
  private Automaton parseAutomatonFile(InputStream pInputStream)
      throws InvalidConfigurationException, IOException, InterruptedException {
    List<AbstractEntry> entries = parseYAML(pInputStream);
    if (getWitnessTypeIfYAML(entries).orElseThrow().equals(WitnessType.CORRECTNESS_WITNESS)) {
      return createCorrectnessAutomatonFromEntries(entries);
    } else {
      return createViolationAutomatonFromEntries(entries);
    }
  }

  private Automaton createCorrectnessAutomatonFromEntries(List<AbstractEntry> entries)
      throws InterruptedException, WitnessParseException {
    String automatonName = "No Loop Invariant Present";
    Map<String, AutomatonVariable> automatonVariables = new HashMap<>();
    String entryStateId = "singleState";

    List<AutomatonTransition> transitions = new ArrayList<>();

    Map<Integer, Set<Pair<String, String>>> lineToSeenInvariants = new HashMap<>();

    for (AbstractEntry entry : entries) {
      if (entry instanceof InvariantEntry invariantEntry) {
        Optional<String> resultFunction =
            Optional.ofNullable(invariantEntry.getLocation().getFunction());
        String invariantString = invariantEntry.getInvariant().getString();
        Integer line = invariantEntry.getLocation().getLine();

        if (!lineToSeenInvariants.containsKey(line)) {
          lineToSeenInvariants.put(line, new HashSet<>());
        }

        // Parsing is expensive for long invariants, we therefore try to reduce it
        Pair<String, String> lookupKey = Pair.of(resultFunction.orElseThrow(), invariantString);

        if (lineToSeenInvariants.get(line).contains(lookupKey)) {
          continue;
        } else {
          lineToSeenInvariants.get(line).add(lookupKey);
        }

        ExpressionTree<AExpression> invariant = parseInvariantEntry(invariantEntry);

        if (invariant.equals(ExpressionTrees.getTrue())) {
          continue;
        }

        transitions.add(
            new AutomatonTransition.Builder(
                    new CheckCoversLines(ImmutableSet.of(line)), entryStateId)
                .withCandidateInvariants(invariant)
                .build());
        automatonName = invariantEntry.getMetadata().getUuid();
      } else {
        throw new WitnessParseException(
            "The witness contained other statements than Loop Invariants!");
      }
    }

    List<AutomatonInternalState> automatonStates =
        ImmutableList.of(new AutomatonInternalState(entryStateId, transitions, false, false, true));

    Automaton automaton;
    try {
      automaton = new Automaton(automatonName, automatonVariables, automatonStates, entryStateId);
    } catch (InvalidAutomatonException e) {
      throw new WitnessParseException(
          "The witness automaton generated from the provided YAML Witness is invalid!", e);
    }

    automaton = invariantsSpecAutomaton.build(automaton, config, logger, shutdownNotifier, cfa);

    dumpAutomatonIfRequested(automaton);

    return automaton;
  }

  /**
   * Parse the input file into invariants. These are temporary classes only used for the transport
   * of information.
   *
   * @param pInputFile The File with the YAML witness
   * @return The Invariants present in the YAML witness
   * @throws InvalidConfigurationException if the configuration is invalid.
   * @throws InterruptedException if the C-Parser is interrupted.
   */
  public Set<Invariant> generateInvariants(Path pInputFile)
      throws InvalidConfigurationException, InterruptedException {
    return AutomatonGraphmlParser.handlePotentiallyGZippedInput(
        MoreFiles.asByteSource(pInputFile), this::generateInvariants, WitnessParseException::new);
  }

  private Set<Invariant> generateInvariants(InputStream pInputStream)
      throws IOException, InterruptedException {
    List<AbstractEntry> entries = parseYAML(pInputStream);
    return generateInvariantsFromEntries(entries);
  }

  private Set<Invariant> generateInvariantsFromEntries(List<AbstractEntry> pEntries)
      throws InterruptedException, IOException {
    Set<Invariant> invariants = new HashSet<>();
    ListMultimap<String, Integer> lineToOffset =
        InvariantStoreUtil.getLineOffsetsByFile(cfa.getFileNames());

    SetMultimap<Integer, String> lineToSeenInvariants = HashMultimap.create();

    for (AbstractEntry entry : pEntries) {
      if (entry instanceof InvariantEntry invariantEntry) {
        Integer line = invariantEntry.getLocation().getLine();
        String invariantString = invariantEntry.getInvariant().getString();

        // Parsing is expensive, therefore cache everything we can
        if (lineToSeenInvariants.get(line).contains(invariantString)) {
          continue;
        }

        ExpressionTree<AExpression> invariant = parseInvariantEntry(invariantEntry);

        FileLocation loc =
            new FileLocation(
                Path.of(invariantEntry.getLocation().getFileName()),
                lineToOffset.get(invariantEntry.getLocation().getFileName()).get(line - 1)
                    + invariantEntry.getLocation().getColumn(),
                -1, // The length is currently not important enough to warrant computing it
                line,
                line);
        invariants.add(new Invariant(invariant, loc, invariantEntry instanceof LoopInvariantEntry));

        lineToSeenInvariants.get(line).add(invariantString);
      }
    }
    return invariants;
  }

  private ExpressionTree<AExpression> parseInvariantEntry(InvariantEntry pInvariantEntry)
      throws InterruptedException {
    Integer line = pInvariantEntry.getLocation().getLine();
    Optional<String> resultFunction = Optional.of(pInvariantEntry.getLocation().getFunction());
    String invariantString = pInvariantEntry.getInvariant().getString();

    Deque<String> callStack = new ArrayDeque<>();
    callStack.push(pInvariantEntry.getLocation().getFunction());

    return createExpressionTreeFromString(resultFunction, invariantString, line, callStack);
  }

  private ExpressionTree<AExpression> createExpressionTreeFromString(
      Optional<String> resultFunction, String invariantString, int line, Deque<String> callStack)
      throws InterruptedException {
    LineMatcher lineMatcher = new LineMatcher(Optional.empty(), line, line);
    ExpressionTree<AExpression> invariant =
        CParserUtils.parseStatementsAsExpressionTree(
            ImmutableSet.of(invariantString),
            resultFunction,
            cparser,
            determineScope(resultFunction, callStack, lineMatcher, scope),
            parserTools);
    return invariant;
  }

  private void dumpAutomatonIfRequested(Automaton automaton) {
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

  private Automaton createViolationAutomatonFromEntries(List<AbstractEntry> pEntries)
      throws InterruptedException, InvalidConfigurationException {
    Map<WaypointRecord, List<WaypointRecord>> segments = segmentize(pEntries);
    // this needs to be called exactly WitnessAutomaton for the option
    // WitnessAutomaton.cpa.automaton.treatErrorsAsTargets to work m(
    final String automatonName = AutomatonGraphmlParser.WITNESS_AUTOMATON_NAME;

    Map<Integer, Integer> lineFrequencies = new HashMap<>();

    for (CFAEdge edge : cfa.edges()) {
      int line = edge.getLineNumber();
      if (lineFrequencies.containsKey(line)) {
        lineFrequencies.put(line, lineFrequencies.get(line) + 1);
      } else {
        int count = lineFrequencies.containsKey(line) ? lineFrequencies.get(line) : 0;
        lineFrequencies.put(line, count + 1);
      }
    }
    Set<Integer> allowedLines =
        lineFrequencies.entrySet().stream()
            .filter(entry -> entry.getValue().equals(1))
            .map(Map.Entry::getKey)
            .collect(ImmutableSet.toImmutableSet());

    int counter = 0;
    final String initState = getStateName(counter++);

    final List<AutomatonInternalState> automatonStates = new ArrayList<>();
    String currentStateId = initState;
    WaypointRecord follow = null;

    int distance = segments.size();

    for (Map.Entry<WaypointRecord, List<WaypointRecord>> entry : segments.entrySet()) {
      List<AutomatonTransition> transitions = new ArrayList<>();
      follow = entry.getKey();
      List<WaypointRecord> avoids = entry.getValue();
      if (!avoids.isEmpty()) {
        logger.log(
            Level.WARNING, "Avoid waypoints in yaml violation witnesses are currently ignored!");
      }
      String nextStateId = getStateName(counter++);
      if (follow.getType().equals(WaypointType.TARGET)) {
        nextStateId = "X";
        // TODO: handle this more elegantly / add check that we are really in the last segment!
      }
      int line = follow.getLocation().getLine();
      AutomatonBoolExpr expr = new CheckCoversLines(ImmutableSet.of(line));
      AutomatonTransition.Builder builder = new AutomatonTransition.Builder(expr, nextStateId);
      handleAssumptionWaypoint(allowedLines, follow, line, builder);

      ImmutableList.Builder<AutomatonAction> actionBuilder = ImmutableList.builder();
      actionBuilder.add(
          new AutomatonAction.Assignment(
              AutomatonGraphmlParser.DISTANCE_TO_VIOLATION,
              new AutomatonIntExpr.Constant(distance--)));
      builder.withActions(actionBuilder.build());
      transitions.add(builder.build());
      automatonStates.add(
          new AutomatonInternalState(
              currentStateId,
              transitions,
              /* pIsTarget= */ false,
              /* pAllTransitions= */ false,
              /* pIsCycleStart= */ false));
      currentStateId = nextStateId;
    }

    // add last state and stutter in it:
    if (follow != null) {
      automatonStates.add(
          new AutomatonInternalState(
              currentStateId,
              ImmutableList.of(
                  new AutomatonTransition.Builder(AutomatonBoolExpr.TRUE, currentStateId)
                      .withAssertion(createViolationAssertion())
                      .build()),
              /* pIsTarget= */ false,
              /* pAllTransitions= */ false,
              /* pIsCycleStart= */ false));
    }

    Automaton automaton;
    Map<String, AutomatonVariable> automatonVariables = new HashMap<>();
    AutomatonIntVariable distanceVariable =
        (AutomatonIntVariable)
            AutomatonVariable.createAutomatonVariable(
                "int", AutomatonGraphmlParser.DISTANCE_TO_VIOLATION);
    automatonVariables.put(AutomatonGraphmlParser.DISTANCE_TO_VIOLATION, distanceVariable);

    // new AutomatonInternalState(entryStateId, transitions, false, false, true)
    try {
      automaton = new Automaton(automatonName, automatonVariables, automatonStates, initState);
    } catch (InvalidAutomatonException e) {
      throw new WitnessParseException(
          "The witness automaton generated from the provided YAML Witness is invalid!", e);
    }

    automaton = invariantsSpecAutomaton.build(automaton, config, logger, shutdownNotifier, cfa);

    dumpAutomatonIfRequested(automaton);

    return automaton;
  }

  private void handleAssumptionWaypoint(
      Set<Integer> allowedLines,
      WaypointRecord follow,
      int line,
      AutomatonTransition.Builder builder)
      throws InterruptedException, InvalidConfigurationException {
    if (follow.getType().equals(WaypointType.ASSUMPTION) && allowedLines.contains(line)) {
      String invariantString = follow.getConstraint().getString();
      Optional<String> resultFunction = Optional.ofNullable(follow.getLocation().getFunction());
      try {
        AExpression exp =
            createExpressionTreeFromString(resultFunction, invariantString, line, null)
                .accept(new ToCExpressionVisitor(cfa.getMachineModel(), logger));
        builder.withAssumptions(ImmutableList.of(exp));
      } catch (UnrecognizedCodeException e) {
        throw new InvalidConfigurationException("Could not parse string into valid expression", e);
      }
    }
  }

  private Map<WaypointRecord, List<WaypointRecord>> segmentize(List<AbstractEntry> pEntries) {
    Map<WaypointRecord, List<WaypointRecord>> segments = new LinkedHashMap<>();
    for (AbstractEntry entry : pEntries) {
      if (entry instanceof ViolationSequenceEntry violationEntry) {

        List<WaypointRecord> avoids = new ArrayList<>();
        for (SegmentRecord segmentRecord : violationEntry.getContent()) {
          for (WaypointRecord waypoint : segmentRecord.getSegment()) {
            if (waypoint.getAction().equals(WaypointAction.AVOID)) {
              avoids.add(waypoint);
              continue;
            } else if (waypoint.getAction().equals(WaypointAction.FOLLOW)) {
              segments.put(waypoint, avoids);
              avoids = new ArrayList<>();
              continue;
            }
          }
        }
        break; // for now just take the first ViolationSequenceEntry in the yaml witness
      }
    }
    return segments;
  }

  private static String getStateName(int i) {
    return "S" + i;
  }

  private static AutomatonBoolExpr createViolationAssertion() {
    return not(new AutomatonBoolExpr.ALLCPAQuery(AutomatonState.INTERNAL_STATE_IS_TARGET_PROPERTY));
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
}
