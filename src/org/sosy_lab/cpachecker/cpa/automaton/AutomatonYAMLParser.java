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
import com.google.common.collect.ArrayListMultimap;
import com.google.common.collect.FluentIterable;
import com.google.common.collect.HashMultimap;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.ListMultimap;
import com.google.common.collect.Multimap;
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
import org.sosy_lab.cpachecker.cfa.ast.AFunctionCallAssignmentStatement;
import org.sosy_lab.cpachecker.cfa.ast.FileLocation;
import org.sosy_lab.cpachecker.cfa.model.ADeclarationEdge;
import org.sosy_lab.cpachecker.cfa.model.AStatementEdge;
import org.sosy_lab.cpachecker.cfa.model.CFAEdge;
import org.sosy_lab.cpachecker.cfa.parser.Scope;
import org.sosy_lab.cpachecker.cpa.automaton.AutomatonBoolExpr.CheckCoversLines;
import org.sosy_lab.cpachecker.cpa.automaton.AutomatonBoolExpr.CheckCoversOffsetAndLine;
import org.sosy_lab.cpachecker.cpa.automaton.AutomatonBoolExpr.CheckEntersIfBranch;
import org.sosy_lab.cpachecker.cpa.automaton.AutomatonBoolExpr.CheckReachesLine;
import org.sosy_lab.cpachecker.cpa.automaton.AutomatonBoolExpr.CheckReachesOffsetAndLine;
import org.sosy_lab.cpachecker.cpa.automaton.AutomatonGraphmlParser.WitnessParseException;
import org.sosy_lab.cpachecker.cpa.automaton.AutomatonVariable.AutomatonIntVariable;
import org.sosy_lab.cpachecker.cpa.automaton.SourceLocationMatcher.LineMatcher;
import org.sosy_lab.cpachecker.exceptions.UnrecognizedCodeException;
import org.sosy_lab.cpachecker.util.CFAUtils;
import org.sosy_lab.cpachecker.util.CParserUtils;
import org.sosy_lab.cpachecker.util.CParserUtils.ParserTools;
import org.sosy_lab.cpachecker.util.Pair;
import org.sosy_lab.cpachecker.util.ast.ASTStructure;
import org.sosy_lab.cpachecker.util.ast.IfStructure;
import org.sosy_lab.cpachecker.util.automaton.AutomatonGraphmlCommon.WitnessType;
import org.sosy_lab.cpachecker.util.expressions.ExpressionTree;
import org.sosy_lab.cpachecker.util.expressions.ExpressionTrees;
import org.sosy_lab.cpachecker.util.expressions.ToCExpressionVisitor;
import org.sosy_lab.cpachecker.util.invariantwitness.Invariant;
import org.sosy_lab.cpachecker.util.invariantwitness.exchange.InvariantStoreUtil;
import org.sosy_lab.cpachecker.util.invariantwitness.exchange.model.AbstractEntry;
import org.sosy_lab.cpachecker.util.invariantwitness.exchange.model.InvariantEntry;
import org.sosy_lab.cpachecker.util.invariantwitness.exchange.model.InvariantSetEntry;
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

  @Option(
      secure = true,
      name = "matchOffsetsWhenCreatingViolationAutomatonFromYAML",
      description =
          "If true the offsets will be matched when creating an automaton to validate Violation"
              + " witnesses. If false only the lines will be matched.")
  private boolean matchOffsetsWhenCreatingViolationAutomatonFromYAML = false;

  @Option(secure = true, description = "File for exporting the witness automaton in DOT format.")
  @FileOption(FileOption.Type.OUTPUT_FILE)
  private Path automatonDumpFile = null;

  private final LogManager logger;
  private final Configuration config;
  private final ShutdownNotifier shutdownNotifier;
  private final CFA cfa;
  private final ParserTools parserTools;
  final CParser cparser;

  ListMultimap<String, Integer> lineOffsetsByFile;

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
    try {
      lineOffsetsByFile = ArrayListMultimap.create();
      lineOffsetsByFile.putAll(InvariantStoreUtil.getLineOffsetsByFile(cfa.getFileNames()));
    } catch (IOException e) {
      throw new WitnessParseException(e);
    }
  }

  public static List<AbstractEntry> parseYAML(InputStream pInputStream) throws IOException {
    ObjectMapper mapper = new ObjectMapper(new YAMLFactory());
    mapper.findAndRegisterModules();
    List<AbstractEntry> entries =
        Arrays.asList(mapper.readValue(pInputStream, AbstractEntry[].class));
    return entries;
  }

  public static Optional<WitnessType> getWitnessTypeIfYAML(List<AbstractEntry> entries) {
    if (FluentIterable.from(entries).allMatch(e -> e instanceof ViolationSequenceEntry)) {
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
        x -> {
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
      if (entry instanceof InvariantSetEntry invariantSetEntry) {
        for (InvariantEntry invariantEntry : invariantSetEntry.toInvariantEntries()) {
          Optional<String> resultFunction =
              Optional.ofNullable(invariantEntry.getLocation().getFunction());
          String invariantString = invariantEntry.getInvariant().getValue();
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
        }
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

  private List<Integer> getOffsetsByFileSimilarity(
      ListMultimap<String, Integer> pOffsetsByFile, String pFile) {
    String maxSimilarityFile = pFile;
    if (pOffsetsByFile.containsKey(pFile)) {
      return pOffsetsByFile.get(pFile);
    }

    // The file extension plus at least one character of the file should match
    int maxSimilarity = 0;
    for (String file : pOffsetsByFile.keySet()) {
      int similarity = 0;
      int index1 = file.length() - 1;
      int index2 = pFile.length() - 1;
      while (index1 >= 0 && index2 >= 0 && file.charAt(index1) == pFile.charAt(index2)) {
        similarity++;
        index1--;
        index2--;
      }
      if (similarity > maxSimilarity) {
        maxSimilarity = similarity;
        maxSimilarityFile = file;
      }
    }

    logger.log(
        Level.INFO,
        "File '"
            + pFile
            + "' could not be found in the files currently being processed, using similar file: "
            + maxSimilarityFile);

    List<Integer> offsets = pOffsetsByFile.get(maxSimilarityFile);
    pOffsetsByFile.putAll(pFile, offsets);

    return offsets;
  }

  private Set<Invariant> generateInvariantsFromEntries(List<AbstractEntry> pEntries)
      throws InterruptedException, IOException {
    Set<Invariant> invariants = new HashSet<>();
    ListMultimap<String, Integer> lineToOffset =
        InvariantStoreUtil.getLineOffsetsByFile(cfa.getFileNames());

    SetMultimap<Integer, String> lineToSeenInvariants = HashMultimap.create();

    for (AbstractEntry entry : pEntries) {
      if (entry instanceof InvariantSetEntry invariantSetEntry) {
        for (InvariantEntry invariantEntry : invariantSetEntry.toInvariantEntries()) {
          Integer line = invariantEntry.getLocation().getLine();
          String invariantString = invariantEntry.getInvariant().getValue();

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
          invariants.add(
              new Invariant(invariant, loc, invariantEntry instanceof LoopInvariantEntry));

          lineToSeenInvariants.get(line).add(invariantString);
        }
      }
    }
    return invariants;
  }

  private ExpressionTree<AExpression> parseInvariantEntry(InvariantEntry pInvariantEntry)
      throws InterruptedException {
    Integer line = pInvariantEntry.getLocation().getLine();
    Optional<String> resultFunction =
        Optional.ofNullable(pInvariantEntry.getLocation().getFunction());
    String invariantString = pInvariantEntry.getInvariant().getValue();

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
    if (matchOffsetsWhenCreatingViolationAutomatonFromYAML) {
      return createViolationAutomatonFromEntriesMatchingOffsets(pEntries);
    } else {
      return createViolationAutomatonFromEntriesMatchingLines(pEntries);
    }
  }

  private Automaton createViolationAutomatonFromEntriesMatchingLines(List<AbstractEntry> pEntries)
      throws InterruptedException, InvalidConfigurationException {
    List<Pair<WaypointRecord, List<WaypointRecord>>> segments = segmentize(pEntries);
    // this needs to be called exactly WitnessAutomaton for the option
    // WitnessAutomaton.cpa.automaton.treatErrorsAsTargets to work m(
    final String automatonName = AutomatonGraphmlParser.WITNESS_AUTOMATON_NAME;

    Set<Integer> allowedLines = linesWithExactlyOneEdge();

    int counter = 0;
    final String initState = getStateName(counter++);

    final List<AutomatonInternalState> automatonStates = new ArrayList<>();
    String currentStateId = initState;
    WaypointRecord follow = null;

    int distance = segments.size();

    for (Pair<WaypointRecord, List<WaypointRecord>> entry : segments) {
      List<AutomatonTransition> transitions = new ArrayList<>();
      follow = entry.getFirst();
      List<WaypointRecord> avoids = entry.getSecond();
      if (!avoids.isEmpty()) {
        logger.log(
            Level.WARNING, "Avoid waypoints in yaml violation witnesses are currently ignored!");
      }
      String nextStateId = getStateName(counter++);
      if (follow.getType().equals(WaypointType.TARGET)) {
        nextStateId = "X";
      }
      int line = follow.getLocation().getLine();
      AutomatonBoolExpr expr = new CheckReachesLine(line);
      AutomatonTransition.Builder builder = new AutomatonTransition.Builder(expr, nextStateId);
      if (follow.getType().equals(WaypointType.ASSUMPTION) && allowedLines.contains(line)) {
        handleConstraint(
            follow.getConstraint().getValue(),
            Optional.ofNullable(follow.getLocation().getFunction()),
            line,
            builder);
      }

      ImmutableList.Builder<AutomatonAction> actionBuilder = ImmutableList.builder();
      actionBuilder.add(
          new AutomatonAction.Assignment(
              AutomatonGraphmlParser.DISTANCE_TO_VIOLATION,
              new AutomatonIntExpr.Constant(distance)));
      builder.withActions(actionBuilder.build());
      transitions.add(builder.build());
      automatonStates.add(
          new AutomatonInternalState(
              currentStateId,
              transitions,
              /* pIsTarget= */ false,
              /* pAllTransitions= */ false,
              /* pIsCycleStart= */ false));

      distance--;
      currentStateId = nextStateId;
    }

    // add last state and stutter in it:
    if (follow != null && follow.getType().equals(WaypointType.TARGET)) {
      automatonStates.add(
          new AutomatonInternalState(
              currentStateId,
              ImmutableList.of(
                  new AutomatonGraphmlParser.TargetInformationCopyingAutomatonTransition(
                      new AutomatonTransition.Builder(AutomatonBoolExpr.TRUE, currentStateId)
                          .withAssertion(createViolationAssertion()))),
              /* pIsTarget= */ false,
              /* pAllTransitions= */ false,
              /* pIsCycleStart= */ false));
    }

    Automaton automaton;
    Map<String, AutomatonVariable> automatonVariables = new HashMap<>();
    AutomatonIntVariable distanceVariable =
        (AutomatonIntVariable)
            AutomatonVariable.createAutomatonVariable(
                "int",
                AutomatonGraphmlParser.DISTANCE_TO_VIOLATION,
                Integer.toString(segments.size() + 1));
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

  private Automaton createViolationAutomatonFromEntriesMatchingOffsets(List<AbstractEntry> pEntries)
      throws InterruptedException, InvalidConfigurationException {
    List<Pair<WaypointRecord, List<WaypointRecord>>> segments = segmentize(pEntries);
    // this needs to be called exactly WitnessAutomaton for the option
    // WitnessAutomaton.cpa.automaton.treatErrorsAsTargets to work m(
    final String automatonName = AutomatonGraphmlParser.WITNESS_AUTOMATON_NAME;

    // TODO: It may be worthwhile to refactor this into the CFA
    Multimap<Integer, CFAEdge> startLineToCFAEdge =
        FluentIterable.from(cfa.edges())
            .index(edge -> edge.getFileLocation().getStartingLineNumber());

    int counter = 0;
    final String initState = getStateName(counter++);

    final List<AutomatonInternalState> automatonStates = new ArrayList<>();
    String currentStateId = initState;
    WaypointRecord follow = null;

    int distance = segments.size();

    for (Pair<WaypointRecord, List<WaypointRecord>> entry : segments) {
      List<AutomatonTransition> transitions = new ArrayList<>();
      follow = entry.getFirst();
      List<WaypointRecord> avoids = entry.getSecond();
      if (!avoids.isEmpty()) {
        logger.log(
            Level.WARNING, "Avoid waypoints in yaml violation witnesses are currently ignored!");
      }
      String nextStateId = getStateName(counter++);
      int followLine = follow.getLocation().getLine();
      int followColumn = follow.getLocation().getColumn();
      String followFilename = follow.getLocation().getFileName();

      AutomatonBoolExpr expr;
      if (follow.getType().equals(WaypointType.TARGET)) {
        nextStateId = "X";
        // For target nodes it sometimes does not make sense to evaluate them at the last possible
        // sequence point as with assumptions. For example, a reach_error call will usually not have
        // any successors in the ARG, since the verification stops there. Therefore handling targets
        // the same way as with assumptions would not work. As an overapproximation we use the
        // covers to present the desired functionality.
        expr =
            new CheckCoversOffsetAndLine(
                getOffsetsByFileSimilarity(lineOffsetsByFile, followFilename).get(followLine - 1)
                    + followColumn,
                followLine);
      } else if (follow.getType().equals(WaypointType.ASSUMPTION)) {
        // The semantics of the YAML witnesses imply that every assumption waypoint should be
        // valid before the sequence statement it points to. Due to the semantics of the format:
        // "An assumption waypoint is evaluated at the sequence point immediately before the
        // waypoint location. The waypoint is passed if the given constraint evaluates to true."
        // Therefore we need the Reaches Offset guard.
        expr =
            new CheckReachesOffsetAndLine(
                getOffsetsByFileSimilarity(lineOffsetsByFile, followFilename).get(followLine - 1)
                    + followColumn,
                followLine);
      } else if (follow.getType().equals(WaypointType.BRANCHING)) {
        if (cfa.getASTStructure().isEmpty()) {
          logger.log(
              Level.INFO,
              "Cannot handle branching waypoint without ASTStructure, skipping waypoint");
          continue;
        }

        ASTStructure astStructure = cfa.getASTStructure().orElseThrow();
        // The -1 in the column is needed since the ASTStructure element starts at the offset before
        // the if keyword, but the waypoint points to the first character of the if keyword
        IfStructure ifStructure =
            astStructure.getIfStructureStartingAtOffset(
                getOffsetsByFileSimilarity(lineOffsetsByFile, followFilename).get(followLine - 1)
                    + followColumn
                    - 1);
        if (ifStructure == null) {
          logger.log(
              Level.INFO, "Could not find IfStructure corresponding to the waypoint, skipping it");
          continue;
        }

        if (ifStructure
            .getNodesBetweenConditionAndElseBranch()
            .equals(ifStructure.getNodesBetweenConditionAndThenBranch())) {
          logger.log(
              Level.INFO,
              "Skipping branching waypoint at if statement since the"
                  + " then and else branch are both empty,"
                  + " and currently there is no way to distinguish them.");
          continue;
        }

        expr =
            new CheckEntersIfBranch(
                ifStructure, Boolean.parseBoolean(follow.getConstraint().getValue()));

        // Add break state for the other branch, since we don't want to explore it
        AutomatonTransition.Builder builder =
            new AutomatonTransition.Builder(
                new CheckEntersIfBranch(
                    ifStructure, !Boolean.parseBoolean(follow.getConstraint().getValue())),
                AutomatonInternalState.BOTTOM);
        transitions.add(builder.build());
      } else if (follow.getType().equals(WaypointType.FUNCTION_RETURN)) {
        expr =
            new CheckCoversOffsetAndLine(
                getOffsetsByFileSimilarity(lineOffsetsByFile, followFilename).get(followLine - 1)
                    + followColumn,
                followLine,
                true);
      } else {
        logger.log(Level.WARNING, "Unknown waypoint type: " + follow.getType());
        continue;
      }
      AutomatonTransition.Builder builder = new AutomatonTransition.Builder(expr, nextStateId);

      if (follow.getType().equals(WaypointType.ASSUMPTION)) {
        handleConstraint(
            follow.getConstraint().getValue(),
            Optional.ofNullable(follow.getLocation().getFunction()),
            followLine,
            builder);
      } else if (follow.getType().equals(WaypointType.TARGET)) {
        // When we match the target state we want to enter the error location immediately
        builder = builder.withAssertion(createViolationAssertion());
      } else if (follow.getType().equals(WaypointType.FUNCTION_RETURN)) {
        // This is basically a special case of an assumption waypoint.
        for (AStatementEdge edge :
            FluentIterable.from(startLineToCFAEdge.get(followLine)).filter(AStatementEdge.class)) {
          // The syntax of the YAML witness describes that the return statement must point to the
          // closing bracket of the function whose return statement is being considered
          int offsetAccordingToWaypoint =
              getOffsetsByFileSimilarity(lineOffsetsByFile, followFilename).get(followLine - 1)
                  + followColumn;
          int offsetEndOfEdge =
              edge.getFileLocation().getNodeOffset() + edge.getFileLocation().getNodeLength() - 1;
          if (offsetEndOfEdge != offsetAccordingToWaypoint) {
            continue;
          }

          if (edge.getStatement() instanceof AFunctionCallAssignmentStatement statement) {
            Set<String> constraints = new HashSet<>();
            if (follow.getConstraint().getValue() != null) {
              constraints.add(follow.getConstraint().getValue());
            }

            List<AExpression> expressions;
            try {
              expressions =
                  CParserUtils.convertStatementsToAssumptions(
                      CParserUtils.parseStatements(
                          constraints,
                          Optional.ofNullable(
                              statement.getRightHandSide().getFunctionNameExpression().toString()),
                          cparser,
                          scope,
                          parserTools),
                      cfa.getMachineModel(),
                      logger);
            } catch (InvalidAutomatonException e) {
              logger.log(Level.INFO, "Could not generate automaton assumption.");
              continue;
            }
            builder.withAssumptions(expressions);
            break;
          }
        }
      }

      ImmutableList.Builder<AutomatonAction> actionBuilder = ImmutableList.builder();
      actionBuilder.add(
          new AutomatonAction.Assignment(
              AutomatonGraphmlParser.DISTANCE_TO_VIOLATION,
              new AutomatonIntExpr.Constant(distance)));
      builder.withActions(actionBuilder.build());
      transitions.add(builder.build());
      automatonStates.add(
          new AutomatonInternalState(
              currentStateId,
              transitions,
              /* pIsTarget= */ false,
              /* pAllTransitions= */ false,
              /* pIsCycleStart= */ false));

      distance--;
      currentStateId = nextStateId;
    }

    // add last state and stutter in it:
    if (follow != null && follow.getType().equals(WaypointType.TARGET)) {
      automatonStates.add(
          new AutomatonInternalState(
              currentStateId,
              ImmutableList.of(
                  new AutomatonGraphmlParser.TargetInformationCopyingAutomatonTransition(
                      new AutomatonTransition.Builder(AutomatonBoolExpr.TRUE, currentStateId)
                          .withAssertion(createViolationAssertion()))),
              /* pIsTarget= */ false,
              /* pAllTransitions= */ false,
              /* pIsCycleStart= */ false));
    }

    Automaton automaton;
    Map<String, AutomatonVariable> automatonVariables = new HashMap<>();
    AutomatonIntVariable distanceVariable =
        (AutomatonIntVariable)
            AutomatonVariable.createAutomatonVariable(
                "int",
                AutomatonGraphmlParser.DISTANCE_TO_VIOLATION,
                Integer.toString(segments.size() + 1));
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

  @SuppressWarnings("unused")
  private Set<Integer> linesWithExactlyOneEdge() {
    Map<Integer, Integer> lineFrequencies = new HashMap<>();

    // we filter ADeclarationEdges because lines like int x = 5; are broken down into two CFA edges,
    // but we can savely ignore the declaration edge in these cases
    for (CFAEdge edge : CFAUtils.allEdges(cfa).filter(x -> !(x instanceof ADeclarationEdge))) {
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
    return allowedLines;
  }

  private void handleConstraint(
      String constraint,
      Optional<String> resultFunction,
      int line,
      AutomatonTransition.Builder builder)
      throws InterruptedException, InvalidConfigurationException {
    try {
      AExpression exp =
          createExpressionTreeFromString(resultFunction, constraint, line, null)
              .accept(new ToCExpressionVisitor(cfa.getMachineModel(), logger));
      builder.withAssumptions(ImmutableList.of(exp));
    } catch (UnrecognizedCodeException e) {
      throw new InvalidConfigurationException("Could not parse string into valid expression", e);
    }
  }

  private List<Pair<WaypointRecord, List<WaypointRecord>>> segmentize(List<AbstractEntry> pEntries)
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
        break; // for now just take the first ViolationSequenceEntry in the yaml witness
      }
    }
    checkTargetIsAtEnd(latest, numTargetWaypoints);
    return segments;
  }

  private void checkTargetIsAtEnd(WaypointRecord latest, int numTargetWaypoints)
      throws InvalidConfigurationException {
    switch (numTargetWaypoints) {
      case 0:
        logger.log(Level.WARNING, "No target waypoint in yaml witness!");
        break;
      case 1:
        if (latest != null && !latest.getType().equals(WaypointType.TARGET)) {
          throw new InvalidConfigurationException(
              "Target waypoint is not at the end in yaml witness!");
        }
        break;
      default:
        throw new InvalidConfigurationException("More than one target waypoint in yaml witness!");
    }
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
