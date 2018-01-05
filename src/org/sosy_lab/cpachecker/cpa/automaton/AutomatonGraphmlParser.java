/*
 *  CPAchecker is a tool for configurable software verification.
 *  This file is part of CPAchecker.
 *
 *  Copyright (C) 2007-2014  Dirk Beyer
 *  All rights reserved.
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 *
 *
 *  CPAchecker web page:
 *    http://cpachecker.sosy-lab.org
 */
package org.sosy_lab.cpachecker.cpa.automaton;

import com.google.common.base.Joiner;
import com.google.common.base.Preconditions;
import com.google.common.base.Predicates;
import com.google.common.base.Strings;
import com.google.common.collect.FluentIterable;
import com.google.common.collect.HashMultimap;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Iterables;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.google.common.collect.Multimap;
import com.google.common.collect.Queues;
import com.google.common.collect.Sets;
import com.google.common.io.ByteSource;
import com.google.common.io.MoreFiles;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.Writer;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Deque;
import java.util.EnumSet;
import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Queue;
import java.util.Set;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.logging.Level;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.zip.GZIPInputStream;
import javax.annotation.Nullable;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.parsers.SAXParser;
import javax.xml.parsers.SAXParserFactory;
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
import org.sosy_lab.cpachecker.cfa.parser.Scope;
import org.sosy_lab.cpachecker.cpa.automaton.CParserUtils.ParserTools;
import org.sosy_lab.cpachecker.cpa.automaton.GraphMLTransition.GraphMLThread;
import org.sosy_lab.cpachecker.cpa.automaton.SourceLocationMatcher.LineMatcher;
import org.sosy_lab.cpachecker.cpa.automaton.SourceLocationMatcher.OffsetMatcher;
import org.sosy_lab.cpachecker.exceptions.ParserException;
import org.sosy_lab.cpachecker.util.NumericIdProvider;
import org.sosy_lab.cpachecker.util.SpecificationProperty.PropertyType;
import org.sosy_lab.cpachecker.util.automaton.AutomatonGraphmlCommon;
import org.sosy_lab.cpachecker.util.automaton.AutomatonGraphmlCommon.AssumeCase;
import org.sosy_lab.cpachecker.util.automaton.AutomatonGraphmlCommon.GraphMLTag;
import org.sosy_lab.cpachecker.util.automaton.AutomatonGraphmlCommon.KeyDef;
import org.sosy_lab.cpachecker.util.automaton.AutomatonGraphmlCommon.NodeFlag;
import org.sosy_lab.cpachecker.util.automaton.AutomatonGraphmlCommon.WitnessType;
import org.sosy_lab.cpachecker.util.expressions.ExpressionTree;
import org.sosy_lab.cpachecker.util.expressions.ExpressionTrees;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;
import org.xml.sax.helpers.DefaultHandler;

@Options(prefix="witness")
public class AutomatonGraphmlParser {

  private static final Pattern VALID_HASH_PATTERN =
      Pattern.compile("([\\da-f]{40})|([\\da-f]{64})");

  private static final String AMBIGUOUS_TYPE_ERROR_MESSAGE = "Witness type must be unambiguous";

  private static final GraphMLTransition.GraphMLThread DEFAULT_THREAD =
      GraphMLTransition.createThread(0, "__CPAchecker_default_thread");

  private static final String TOO_MANY_GRAPHS_ERROR_MESSAGE =
      "The witness file must describe exactly one witness automaton.";

  private static final String ACCESS_ERROR_MESSAGE = "Error while accessing witness file: %s!";

  private static final String INVALID_AUTOMATON_ERROR_MESSAGE =
      "The witness automaton provided is invalid!";

  /** The name of the variable that stores the distance of each automaton state to the nearest violation state. */
  private static final String DISTANCE_TO_VIOLATION = "__DISTANCE_TO_VIOLATION";

  public static final String WITNESS_AUTOMATON_NAME = "WitnessAutomaton";

  @Option(secure=true, description="Consider assumptions that are provided with the path automaton?")
  private boolean considerAssumptions = true;

  @Option(
    secure = true,
    description = "Represent sink states by bottom state instead of break state"
  )
  private boolean stopNotBreakAtSinkStates = true;

  @Option(secure=true, description="Match the line numbers within the origin (mapping done by preprocessor line markers).")
  private boolean matchOriginLine = true;

  @Option(secure=true, description="Match the character offset within the file.")
  private boolean matchOffset = true;

  @Option(secure=true, description="Match the branching information at a branching location.")
  private boolean matchAssumeCase = true;

  @Option(
    secure = true,
    description =
        "Check that the value of the programhash field of the witness matches the SHA-256 hash value computed for the source code."
  )
  private boolean checkProgramHash = true;

  @Option(
    secure = true,
    description =
        "Enforce strict validity checks regarding the witness format, such as checking for the presence of required fields."
  )
  private boolean strictChecking = true;

  @Option(secure=true, description="File for exporting the witness automaton in DOT format.")
  @FileOption(FileOption.Type.OUTPUT_FILE)
  private Path automatonDumpFile = null;

  private Scope scope;
  private final LogManager logger;
  private final Configuration config;
  private final CFA cfa;
  private final ParserTools parserTools;

  public AutomatonGraphmlParser(Configuration pConfig, LogManager pLogger, CFA pCFA, Scope pScope)
      throws InvalidConfigurationException {
    pConfig.inject(this);

    this.scope = pScope;
    this.logger = pLogger;
    this.cfa = pCFA;
    this.config = pConfig;
    this.parserTools =
        ParserTools.create(ExpressionTrees.newCachingFactory(), cfa.getMachineModel(), logger);
  }

  /**
   * Parses a witness specification from a file and returns the Automata found in the file.
   *
   * @param pInputFile the path to the input file to parse the witness from.
   * @param pPropertyTypes which are assumed to be witnessed.
   * @throws InvalidConfigurationException if the configuration is invalid.
   * @return the automata representing the witnesses found in the file.
   */
  public List<Automaton> parseAutomatonFile(Path pInputFile, Set<PropertyType> pPropertyTypes)
      throws InvalidConfigurationException {
    return parseAutomatonFile(MoreFiles.asByteSource(pInputFile), pPropertyTypes);
  }

  /**
   * Parses a witness specification from a ByteSource and returns the Automata found in the source.
   *
   * @param pInputSource the ByteSource to parse the witness from.
   * @param pPropertyTypes which are assumed to be witnessed.
   * @throws InvalidConfigurationException if the configuration is invalid.
   * @return the automata representing the witnesses found in the source.
   */
  private List<Automaton> parseAutomatonFile(
      ByteSource pInputSource, Set<PropertyType> pPropertyTypes)
      throws InvalidConfigurationException {
    return AutomatonGraphmlParser
        .<List<Automaton>, InvalidConfigurationException>handlePotentiallyGZippedInput(
            pInputSource,
            inputStream -> parseAutomatonFile(inputStream, pPropertyTypes),
            e -> new WitnessParseException(e));
  }

  /**
   * Parses a specification from an InputStream and returns the Automata found in the file.
   *
   * @param pInputStream the input stream to parse the witness from.
   * @param pPropertyTypes which are assumed to be witnessed.
   * @throws InvalidConfigurationException if the configuration is invalid.
   * @throws IOException if there occurs an IOException while reading from the stream.
   * @return the automata representing the witnesses found in the stream.
   */
  private List<Automaton> parseAutomatonFile(
      InputStream pInputStream, Set<PropertyType> pPropertyTypes)
      throws InvalidConfigurationException, IOException {
    final CParser cparser =
        CParser.Factory.getParser(
            logger, CParser.Factory.getOptions(config), cfa.getMachineModel());

    AutomatonGraphmlParserState graphMLParserState =
        setupGraphMLParser(pInputStream, pPropertyTypes);

    // Parse the transitions
    parseTransitions(cparser, graphMLParserState);

    // Create the actual states in our automaton model
    List<AutomatonInternalState> automatonStates = Lists.newArrayList();
    for (GraphMLState state : graphMLParserState.getStates()) {
      automatonStates.add(createAutomatonState(graphMLParserState, state));
    }

    // Build and return the result
    List<Automaton> result = Lists.newArrayList();
    Automaton automaton;
    try {
      automaton = new Automaton(
          graphMLParserState.getAutomatonName(),
          graphMLParserState.getAutomatonVariables(),
          automatonStates,
          graphMLParserState.getEntryState().getId());
    } catch (InvalidAutomatonException e) {
      throw new WitnessParseException(INVALID_AUTOMATON_ERROR_MESSAGE, e);
    }
    result.add(automaton);

    if (automatonDumpFile != null) {
      try (Writer w = IO.openOutputFile(automatonDumpFile, Charset.defaultCharset())) {
        automaton.writeDotFile(w);
      } catch (IOException e) {
        // logger.logUserException(Level.WARNING, e, "Could not write the automaton to DOT file");
      }
      Path automatonFile = automatonDumpFile.resolveSibling(automatonDumpFile.getFileName() + ".spc");
      try (Writer w = IO.openOutputFile(automatonFile, Charset.defaultCharset())) {
        w.write(automaton.toString());
      } catch (IOException e) {
        // logger.logUserException(Level.WARNING, e, "Could not write the automaton to DOT file");
      }
    }

    return result;
  }

  /**
   * Creates an {@link AutomatonInternalState} from the given {@link GraphMLState},
   * adds the corresponding stutter transitions to the GraphML-parser state,
   * and adds a self-transition to violation states.
   *
   * @param pGraphMLParserState the current GraphML-parser state.
   * @param pState the GraphML state to be converted into an automaton state.
   * @return an {@link AutomatonInternalState} corresponding to the given GraphML state.
   */
  private AutomatonInternalState createAutomatonState(
      AutomatonGraphmlParserState pGraphMLParserState, GraphMLState pState) {
    List<AutomatonTransition> transitions = pGraphMLParserState.getStateTransitions(pState);

    // If the transition conditions do not apply, none of the above transitions is taken,
    // and instead, the stutter condition applies.
    AutomatonBoolExpr stutterCondition = pGraphMLParserState.getStutterConditions().get(pState);
    if (stutterCondition == null) {
      stutterCondition = AutomatonBoolExpr.TRUE;
    }
    // Wait in the source state until the witness checker catches up with the witness
    transitions.add(
        createAutomatonTransition(
            stutterCondition,
            Collections.<AutomatonBoolExpr>emptyList(),
            Collections.emptyList(),
            ExpressionTrees.<AExpression>getTrue(),
            Collections.<AutomatonAction>emptyList(),
            pState,
            pState.isViolationState(),
            stopNotBreakAtSinkStates));

    if (pState.isViolationState()) {
      AutomatonBoolExpr otherAutomataSafe = createViolationAssertion();
      List<AutomatonBoolExpr> assertions = Collections.singletonList(otherAutomataSafe);
      transitions.add(
          createAutomatonTransition(
              AutomatonBoolExpr.TRUE,
              assertions,
              Collections.emptyList(),
              ExpressionTrees.<AExpression>getTrue(),
              Collections.<AutomatonAction>emptyList(),
              pState,
              true,
              stopNotBreakAtSinkStates));
    }

    // Initialize distance variable at the entry state
    if (pState.isEntryState()
        && pGraphMLParserState.getWitnessType() == WitnessType.VIOLATION_WITNESS) {
      AutomatonVariable distanceVariable = new AutomatonVariable("int", DISTANCE_TO_VIOLATION);
      distanceVariable.setValue(pGraphMLParserState.getDistance(pState));
      pGraphMLParserState.getAutomatonVariables().put(DISTANCE_TO_VIOLATION, distanceVariable);
    }

    AutomatonInternalState automatonState =
        new AutomatonInternalState(
            pState.getId(), transitions, false, true, pState.isCycleHead());
    return automatonState;
  }

  /**
   * Parses all transitions reachable from the entry state and modifies the GraphML parser state accordingly.
   *
   * @param pCParser the C parser to be used for parsing expressions.
   * @param pGraphMLParserState the GraphML parser state.
   * @throws WitnessParseException if the witness file is invalid and cannot be parsed.
   */
  private void parseTransitions(final CParser pCParser,
      AutomatonGraphmlParserState pGraphMLParserState) throws WitnessParseException {

    // The transitions (represented in the GraphML model) already visited
    Set<GraphMLTransition> visitedTransitions = Sets.newHashSet();
    // The transition search frontier, i.e. the transitions (represented in the GraphML model)
    // currently waiting to be explored
    Queue<GraphMLTransition> waitingTransitions = Queues.newArrayDeque();
    waitingTransitions.addAll(
        pGraphMLParserState.getLeavingTransitions().get(pGraphMLParserState.getEntryState()));
    visitedTransitions.addAll(waitingTransitions);

    while (!waitingTransitions.isEmpty()) {
      // Get the next transition to be parsed
      GraphMLTransition transition = waitingTransitions.poll();

      // Parse the transition
      parseTransition(pCParser, pGraphMLParserState, transition);

      // Collect all successor transitions that were not parsed yet and add them to the wait list
      Iterable<GraphMLTransition> successorTransitions = pGraphMLParserState
          .getLeavingTransitions()
          .get(transition.getTarget());
      for (GraphMLTransition successorTransition : successorTransitions) {
        if (visitedTransitions.add(successorTransition)) {
          waitingTransitions.add(successorTransition);
        }
      }
    }
  }

  /**
   * Parses the given transition and modifies the GraphML parser state accordingly.
   *
   * @param pCParser the C parser to be used for parsing expressions.
   * @param pGraphMLParserState the GraphML parser state.
   * @param pTransition the transition to parse.
   * @throws WitnessParseException if the witness file is invalid and cannot be parsed.
   */
  private void parseTransition(CParser pCParser,
      AutomatonGraphmlParserState pGraphMLParserState,
      GraphMLTransition pTransition)
      throws WitnessParseException {
    if (pGraphMLParserState.getWitnessType() == WitnessType.CORRECTNESS_WITNESS
        && pTransition.getTarget().isSinkState()) {
      throw new WitnessParseException(
            "Proof witnesses do not allow sink nodes.");
    }

    final List<AutomatonAction> actions = getTransitionActions(pGraphMLParserState, pTransition);

    List<AutomatonTransition> transitions =
        pGraphMLParserState.getStateTransitions(pTransition.getSource());

    // Handle call stack
    Deque<String> newStack = handleCallStack(pGraphMLParserState, pTransition);

    // Parse the assumptions of the transition
    List<AExpression> assumptions =
        getAssumptions(pCParser, pGraphMLParserState, pTransition, newStack);

    // Check that there are no assumptions for a correctness witness
    if (pGraphMLParserState.getWitnessType() == WitnessType.CORRECTNESS_WITNESS
        && !assumptions.isEmpty()) {
      throw new WitnessParseException(
            "Assumptions are not allowed for correctness witnesses.");
    }

    GraphMLThread thread = pTransition.getThread();

    // Parse the invariants of the witness
    ExpressionTree<AExpression> candidateInvariants =
        getInvariants(pCParser, pGraphMLParserState, pTransition, newStack);

    // Check that there are no invariants in a violation witness
    if (!ExpressionTrees.getTrue().equals(candidateInvariants)
        && pGraphMLParserState.getWitnessType() == WitnessType.VIOLATION_WITNESS
        && !pGraphMLParserState.getSpecificationTypes().contains(PropertyType.TERMINATION)) {
      throw new WitnessParseException(
          "Invariants are not allowed for violation witnesses.");
    }

    List<Function<AutomatonBoolExpr, AutomatonBoolExpr>> conditionTransformations =
        Lists.newArrayList();

    // Add a source-code guard for specified line numbers
    if (matchOriginLine) {
      conditionTransformations.add(
          condition -> and(condition, getLocationMatcher(pTransition.getLineMatcherPredicate())));
    }

    // Add a source-code guard for specified character offsets
    if (matchOffset) {
      conditionTransformations.add(
          condition -> and(condition, getLocationMatcher(pTransition.getOffsetMatcherPredicate())));
    }

    // Add a source-code guard for function-call statements if an explicit result function is
    // specified
    if (pGraphMLParserState.getWitnessType() == WitnessType.VIOLATION_WITNESS
        && pTransition.getExplicitAssumptionResultFunction().isPresent()) {
      String resultFunctionName =
          getFunction(
                  pGraphMLParserState, thread, pTransition.getExplicitAssumptionResultFunction())
              .get();
      conditionTransformations.add(
          condition ->
              and(condition, new AutomatonBoolExpr.MatchFunctionCallStatement(resultFunctionName)));
    }

    // Add a source-code guard for specified function exits
    if (pTransition.getFunctionExit().isPresent()) {
      String function =
          getFunction(pGraphMLParserState, thread, pTransition.getFunctionExit()).get();
      conditionTransformations.add(condition -> and(condition, getFunctionExitMatcher(function)));
    }

    // Add a source-code guard for specified function entries
    Function<AutomatonBoolExpr, AutomatonBoolExpr> applyMatchFunctionEntry = Function.identity();
    if (pTransition.getFunctionEntry().isPresent()) {
      String function =
          getFunction(pGraphMLParserState, thread, pTransition.getFunctionEntry()).get();
      applyMatchFunctionEntry = condition -> and(condition, getFunctionCallMatcher(function));
      conditionTransformations.add(applyMatchFunctionEntry);
    }

    // Add a source-code guard for specified branching information
    if (matchAssumeCase) {
      conditionTransformations.add(condition -> and(condition, pTransition.getAssumeCaseMatcher()));
    }

    // Add a source-code guard for a specified loop head
    if (pTransition.entersLoopHead()) {
      // Unfortunately, we have a blank edge entering most of our loop heads;
      // (a) sometimes the transition needs to match the edge before the blank edge,
      // (b) sometimes the transition needs to match the successor state to the loop head,
      // sometimes the transition even needs to match "both" of the above.
      // Therefore, we do exactly that (match "both");
      conditionTransformations.add(
          condition -> {
            AutomatonBoolExpr conditionA =
                and(
                    condition,
                    AutomatonBoolExpr.EpsilonMatch.forwardEpsilonMatch(
                        AutomatonBoolExpr.MatchLoopStart.INSTANCE, true));
            if (pTransition.getTarget().getInvariants().isEmpty()) {
              return conditionA;
            }
            AutomatonBoolExpr conditionB =
                and(
                    AutomatonBoolExpr.EpsilonMatch.backwardEpsilonMatch(condition, true),
                    AutomatonBoolExpr.MatchLoopStart.INSTANCE);
            return or(conditionA, conditionB);
          });
    }

    // Never match on the dummy edge directly after the main function entry node
    conditionTransformations.add(
        condition -> and(condition, not(AutomatonBoolExpr.MatchProgramEntry.INSTANCE)));
    // Never match on artificially split declarations
    conditionTransformations.add(
        condition -> and(condition, not(AutomatonBoolExpr.MatchSplitDeclaration.INSTANCE)));

    // Initialize the transition condition to TRUE, so that all individual
    // conditions can conveniently be conjoined to it later
    AutomatonBoolExpr transitionCondition = AutomatonBoolExpr.TRUE;
    AutomatonBoolExpr transitionConditionWithoutFunctionEntry = AutomatonBoolExpr.TRUE;
    for (Function<AutomatonBoolExpr, AutomatonBoolExpr> conditionTransformation :
        conditionTransformations) {
      transitionCondition = conditionTransformation.apply(transitionCondition);
      if (conditionTransformation != applyMatchFunctionEntry) {
        transitionConditionWithoutFunctionEntry =
            conditionTransformation.apply(transitionConditionWithoutFunctionEntry);
      }
    }

    // If the transition represents a function call, add a sink transition
    // in case it is a function pointer call,
    // where we can eliminate the other branch
    AutomatonBoolExpr fpElseTrigger = null;
    if (pTransition.getFunctionEntry().isPresent()
        && pGraphMLParserState.getWitnessType() != WitnessType.CORRECTNESS_WITNESS) {
      fpElseTrigger =
          and(
              transitionConditionWithoutFunctionEntry,
              getFunctionPointerAssumeCaseMatcher(
                  getFunction(pGraphMLParserState, thread, pTransition.getFunctionEntry()).get(),
                  pTransition.getTarget().isSinkState()));
      transitions.add(
          createAutomatonSinkTransition(
              fpElseTrigger,
              Collections.<AutomatonBoolExpr>emptyList(),
              actions,
              false,
              stopNotBreakAtSinkStates));
    }

    // If the triggers do not apply, none of the above transitions is taken,
    // so we need to build the stutter condition
    // as the conjoined negations of the transition conditions.
    AutomatonBoolExpr stutterCondition =
        pGraphMLParserState.getStutterConditions().get(pTransition.getSource());
    AutomatonBoolExpr additionalStutterCondition = not(transitionCondition);
    if (fpElseTrigger != null) {
      additionalStutterCondition = and(additionalStutterCondition, not(fpElseTrigger));
    }
    if (stutterCondition == null) {
      stutterCondition = additionalStutterCondition;
    } else {
      stutterCondition = and(stutterCondition, additionalStutterCondition);
    }
    pGraphMLParserState.getStutterConditions().put(pTransition.getSource(), stutterCondition);

    // If the triggers match, there must be one successor state that moves the automaton
    // forwards
    transitions.add(
        createAutomatonTransition(
            transitionCondition,
            Collections.<AutomatonBoolExpr> emptyList(),
            assumptions,
            candidateInvariants,
            actions,
            pTransition.getTarget(),
            pTransition.getTarget().isViolationState(),
            stopNotBreakAtSinkStates));

    // Multiple CFA edges in a sequence might match the triggers,
    // so in that case we ALSO need a transition back to the source state
    if (!assumptions.isEmpty()
        || !actions.isEmpty()
        || !candidateInvariants.equals(ExpressionTrees.getTrue())
        || pTransition.getTarget().isViolationState()) {
      boolean sourceIsViolationNode = pTransition.getSource().isViolationState();
      transitions.add(
          createAutomatonTransition(
              and(
                  transitionCondition,
                  new AutomatonBoolExpr.MatchAnySuccessorEdgesBoolExpr(transitionCondition)),
              Collections.<AutomatonBoolExpr> emptyList(),
              Collections.emptyList(),
              ExpressionTrees.<AExpression> getTrue(),
              Collections.<AutomatonAction> emptyList(),
              pTransition.getSource(),
              sourceIsViolationNode,
              stopNotBreakAtSinkStates));
    }
  }

  /**
   * Parses the invariants specified for this transition.
   *
   * @param pCParser the C parser to parse the assumptions with.
   * @param pGraphMLParserState the current state of the GraphML parser.
   * @param pTransition the transition to be parsed.
   * @param pCallstack the current call stack.
   * @return the invariants specified for this transition.
   * @throws WitnessParseException if a function cannot be assigned to the transition's thread.
   */
  private ExpressionTree<AExpression> getInvariants(
      CParser pCParser,
      AutomatonGraphmlParserState pGraphMLParserState,
      GraphMLTransition pTransition,
      Deque<String> pCallstack)
      throws WitnessParseException {
    if (!pTransition.getTarget().getInvariants().isEmpty()) {
      GraphMLThread thread = pTransition.getThread();
      Optional<String> explicitInvariantScope =
          getFunction(
              pGraphMLParserState, thread, pTransition.getTarget().getExplicitInvariantScope());
      Scope candidateScope =
          determineScope(
              explicitInvariantScope, pCallstack, getLocationMatcherPredicate(pTransition));
      Optional<String> explicitAssumptionResultFunction =
          getFunction(
              pGraphMLParserState, thread, pTransition.getExplicitAssumptionResultFunction());
      Optional<String> resultFunction =
          determineResultFunction(explicitAssumptionResultFunction, scope);
      return CParserUtils.parseStatementsAsExpressionTree(
          pTransition.getTarget().getInvariants(),
          resultFunction,
          pCParser,
          candidateScope,
          parserTools);
    }
    return ExpressionTrees.getTrue();
  }

  /**
   * Parses the assumptions specified for this transition.
   *
   * @param pCParser the C parser to parse the assumptions with.
   * @param pGraphMLParserState the current state of the GraphML parser.
   * @param pTransition the transition to be parsed.
   * @param pCallstack the current call stack.
   * @return the assumptions specified for this transition.
   * @throws WitnessParseException if parsing the assumptions fails.
   */
  private List<AExpression> getAssumptions(
      CParser pCParser,
      AutomatonGraphmlParserState pGraphMLParserState,
      GraphMLTransition pTransition,
      Deque<String> pCallstack)
      throws WitnessParseException {
    if (considerAssumptions) {
      GraphMLThread thread = pTransition.getThread();
      Optional<String> explicitAssumptionScope =
          getFunction(pGraphMLParserState, thread, pTransition.getExplicitAssumptionScope());
      Scope scope =
          determineScope(
              explicitAssumptionScope, pCallstack, getLocationMatcherPredicate(pTransition));
      Optional<String> explicitAssumptionResultFunction =
          getFunction(
              pGraphMLParserState, thread, pTransition.getExplicitAssumptionResultFunction());
      Optional<String> assumptionResultFunction =
          determineResultFunction(explicitAssumptionResultFunction, scope);
      try {
        return
            CParserUtils.convertStatementsToAssumptions(
                CParserUtils.parseStatements(
                    pTransition.getAssumptions(),
                    assumptionResultFunction,
                    pCParser,
                    scope, parserTools),
                cfa.getMachineModel(),
                logger);
      } catch (InvalidAutomatonException e) {
        String reason = e.getMessage();
        if (e.getCause() instanceof ParserException) {
          reason =
              String.format("Cannot parse <%s>", Joiner.on(" ").join(pTransition.getAssumptions()));
        }
        throw new WitnessParseException(INVALID_AUTOMATON_ERROR_MESSAGE + " Reason: " + reason, e);
      }
    }
    return Collections.emptyList();
  }

  private Optional<String> getFunction(
      AutomatonGraphmlParserState pGraphmlParserState,
      GraphMLThread pThread,
      Optional<String> pFunctionName)
      throws WitnessParseException {
    if (!pFunctionName.isPresent() || !cfa.getAllFunctionNames().contains(pFunctionName.get())) {
      return pFunctionName;
    }
    Optional<String> functionName =
        pGraphmlParserState.getFunctionForThread(pThread, pFunctionName.get());
    if (functionName.isPresent()) {
      return functionName;
    }
    throw new WitnessParseException(
        String.format(
            "Unable to assign function <%s> to thread <%s>.", pFunctionName.get(), pThread));
  }

  /**
   * Gets the location matcher predicate for the given transition.
   *
   * @param pTransition the transition to parse.
   * @return the location matcher predicate for the given transition.
   */
  private Predicate<FileLocation> getLocationMatcherPredicate(GraphMLTransition pTransition) {
    Predicate<FileLocation> locationMatcherPredicate = Predicates.alwaysTrue();
    if (matchOffset) {
      Optional<Predicate<FileLocation>> offsetMatcherPredicate =
          pTransition.getOffsetMatcherPredicate();
      if (offsetMatcherPredicate.isPresent()) {
        locationMatcherPredicate = locationMatcherPredicate.and(offsetMatcherPredicate.get());
      }
    }
    if (matchOriginLine) {
      Optional<Predicate<FileLocation>> lineMatcherPredicate =
          pTransition.getLineMatcherPredicate();
      if (lineMatcherPredicate.isPresent()) {
        locationMatcherPredicate = locationMatcherPredicate.and(lineMatcherPredicate.get());
      }
    }
    return locationMatcherPredicate;
  }

  /**
   * Handle any function entries or exits on this transition and obtain the resulting call stack.
   *
   * @param pGraphMLParserState the GraphML parser state.
   * @param pTransition the transition to parse.
   * @return the new call stack.
   * @throws WitnessParseException if a function cannot be assigned to the transition's thread.
   */
  private Deque<String> handleCallStack(
      AutomatonGraphmlParserState pGraphMLParserState, GraphMLTransition pTransition)
      throws WitnessParseException {
    GraphMLTransition.GraphMLThread thread = pTransition.getThread();
    Deque<String> currentStack =
        pGraphMLParserState.getOrCreateStack(thread, pTransition.getSource());
    Deque<String> newStack = currentStack;

    Optional<String> functionEntry =
        getFunction(pGraphMLParserState, thread, pTransition.getFunctionEntry());
    if (!functionEntry.isPresent() && pTransition.getSource().isEntryState()) {
      functionEntry =
          getFunction(
              pGraphMLParserState, thread, Optional.of(cfa.getMainFunction().getFunctionName()));
    }
    Optional<String> functionExit =
        getFunction(pGraphMLParserState, thread, pTransition.getFunctionExit());

    // If the same function is entered and exited, the stack remains unchanged.
    // Otherwise, adjust the stack accordingly:
    if (!Objects.equals(functionEntry, functionExit)) {
      // First, perform the function exit
      if (functionExit.isPresent()) {
        if (newStack.isEmpty()) {
          logger.log(
              Level.WARNING,
              "Trying to return from function",
              functionExit.get(),
              "although no function is on the stack.");
        } else {
          newStack = new ArrayDeque<>(newStack);
          String oldFunction = newStack.pop();
          if (!oldFunction.equals(functionExit.get())) {
            logger.log(
                Level.WARNING,
                String.format(
                    "Trying to return from function %s, but current function on call stack is %s",
                    functionExit.get(), oldFunction));
          } else if (newStack.isEmpty()) {
            pGraphMLParserState.releaseFunctions(thread);
          }
        }
      }
      // Now enter the new function
      if (functionEntry.isPresent()) {
        newStack = new ArrayDeque<>(newStack);
        newStack.push(functionEntry.get());
      }
    }
    // Store the stack in its state after the edge is applied
    pGraphMLParserState.putStack(thread, pTransition.getTarget(), newStack);

    // If the edge enters and exits the same function, assume this function for this edge only
    if (functionEntry.isPresent()
        && functionEntry.equals(functionExit)
        && (newStack.isEmpty() || !newStack.peek().equals(functionExit.get()))) {
      newStack = new ArrayDeque<>(newStack);
    }
    return newStack;
  }

  /**
   * Gets the automaton actions that should be applied for the given transition.
   *
   * @param pGraphMLParserState the GraphML parser state.
   * @param pTransition the transition to parse.
   * @return the automaton actions that should be applied for the given transition.
   */
  private static List<AutomatonAction> getTransitionActions(
      AutomatonGraphmlParserState pGraphMLParserState, GraphMLTransition pTransition) {
    ImmutableList.Builder<AutomatonAction> actionBuilder = ImmutableList.builder();
    if (pGraphMLParserState.getWitnessType() == WitnessType.VIOLATION_WITNESS) {
      actionBuilder.add(
          new AutomatonAction.Assignment(
              DISTANCE_TO_VIOLATION,
              new AutomatonIntExpr.Constant(
                  -pGraphMLParserState.getDistance(pTransition.getTarget()))));
    }

    Optional<AutomatonAction> threadAssignment = pTransition.getThreadAssignment();
    if (threadAssignment.isPresent()) {
      actionBuilder.add(threadAssignment.get());
    }
    return actionBuilder.build();
  }

  /**
   * Initializes the GraphML-parser state by parsing the XML document from the given input stream
   * into an intermediate representation.
   *
   * @param pInputStream the input stream to read from.
   * @param pPropertyTypes which are assumed to be witnessed.
   * @return the initialized parser state.
   * @throws IOException if reading from the input stream fails.
   * @throws WitnessParseException if the initial validity checks for conformity with the witness
   *     format fail.
   */
  private AutomatonGraphmlParserState setupGraphMLParser(
      InputStream pInputStream, Set<PropertyType> pPropertyTypes)
      throws IOException, WitnessParseException {

    GraphMLDocumentData docDat = parseXML(pInputStream);

    checkFields(docDat.getGraph());

    WitnessType graphType = getWitnessType(docDat.getGraph());

    // Extract the information on the automaton ----
    Node nameAttribute = docDat.getGraph().getAttributes().getNamedItem("name");
    String automatonName = WITNESS_AUTOMATON_NAME;
    if (nameAttribute != null) {
      automatonName += "_" + nameAttribute.getTextContent();
    }

    Map<String, GraphMLState> states = Maps.newHashMap();
    Multimap<GraphMLState, GraphMLTransition> enteringTransitions = HashMultimap.create();
    Multimap<GraphMLState, GraphMLTransition> leavingTransitions = HashMultimap.create();
    NumericIdProvider numericIdProvider = NumericIdProvider.create();
    Set<GraphMLState> entryStates = Sets.newHashSet();
    for (Node transition : docDat.getTransitions()) {
      collectEdgeData(
          docDat,
          states,
          entryStates,
          leavingTransitions,
          enteringTransitions,
          numericIdProvider,
          transition);
    }
    if (states.size() < docDat.idToNodeMap.size()) {
      for (String stateId : docDat.idToNodeMap.keySet()) {
        if (!states.containsKey(stateId)) {
          states.put(stateId, parseState(docDat, states, stateId, Optional.empty()));
        }
      }
    }

    AutomatonGraphmlParserState state =
        AutomatonGraphmlParserState.initialize(
            automatonName,
            graphType,
            pPropertyTypes,
            states.values(),
            enteringTransitions,
            leavingTransitions,
            cfa.getAllFunctionNames());

    // Check if entry state is connected to a violation state
    if (state.getWitnessType() == WitnessType.VIOLATION_WITNESS
        && !state.isEntryConnectedToViolation()) {
      logger.log(
          Level.WARNING,
          String.format(
              "There is no path from the entry state %s"
                  + " to a state explicitly marked as violation state."
                  + " Distance-to-violation waitlist order will not work"
                  + " and witness validation may fail to confirm this witness.",
              state.getEntryState()));
    }

    // Define thread-id variable, if any assignments to it exist
    if (state.getLeavingTransitions().values().stream()
        .anyMatch(t -> t.getThreadAssignment().isPresent())) {
      state.getAutomatonVariables().put(
          KeyDef.THREADNAME.name(), new AutomatonVariable("int", KeyDef.THREADNAME.name()));
    }

    return state;
  }

  private GraphMLDocumentData parseXML(InputStream pInputStream)
      throws WitnessParseException, IOException {

    // Parse the XML document ----
    DocumentBuilderFactory docFactory = DocumentBuilderFactory.newInstance();

    Document doc;
    try {
      DocumentBuilder docBuilder = docFactory.newDocumentBuilder();
      doc = docBuilder.parse(pInputStream);
    } catch (ParserConfigurationException | SAXException e) {
      throw new WitnessParseException(e);
    }

    return new GraphMLDocumentData(doc);
  }

  private void checkFields(Node graphNode) throws IOException, WitnessParseException {

    checkHashSum(GraphMLDocumentData.getDataOnNode(graphNode, KeyDef.PROGRAMHASH));
    checkArchitecture(GraphMLDocumentData.getDataOnNode(graphNode, KeyDef.ARCHITECTURE));

    if (strictChecking) {
      checkRequiredField(graphNode, KeyDef.WITNESS_TYPE);
      checkRequiredField(graphNode, KeyDef.SOURCECODELANGUAGE);
      checkRequiredField(graphNode, KeyDef.PRODUCER);
      checkRequiredField(graphNode, KeyDef.SPECIFICATION);
      checkRequiredField(graphNode, KeyDef.PROGRAMFILE);
    }
  }

  private static AutomatonBoolExpr getFunctionCallMatcher(String pEnteredFunction) {
    AutomatonBoolExpr functionEntryMatcher =
        new AutomatonBoolExpr.MatchFunctionCall(pEnteredFunction);
    return functionEntryMatcher;
  }

  private static AutomatonBoolExpr getFunctionPointerAssumeCaseMatcher(
      String pEnteredFunction, boolean pIsSinkNode) {
    AutomatonBoolExpr functionPointerAssumeCaseMatcher =
      new AutomatonBoolExpr.MatchFunctionPointerAssumeCase(
          new AutomatonBoolExpr.MatchAssumeCase(pIsSinkNode),
          new AutomatonBoolExpr.MatchFunctionCall(pEnteredFunction));
    return functionPointerAssumeCaseMatcher;
  }

  private static AutomatonBoolExpr getFunctionExitMatcher(String pExitedFunction) {
    AutomatonBoolExpr functionExitMatcher = or(
        new AutomatonBoolExpr.MatchFunctionExit(pExitedFunction),
        new AutomatonBoolExpr.MatchFunctionCallStatement(pExitedFunction));
    return functionExitMatcher;
  }

  private static boolean entersLoopHead(Node pTransition) throws WitnessParseException {
    Set<String> loopHeadFlags =
        GraphMLDocumentData.getDataOnNode(pTransition, KeyDef.ENTERLOOPHEAD);
    if (!loopHeadFlags.isEmpty()) {
      Set<Boolean> loopHeadFlagValues =
          loopHeadFlags.stream().map(Boolean::parseBoolean).collect(Collectors.toSet());
      if (loopHeadFlagValues.size() > 1) {
        throw new WitnessParseException(
            "Conflicting values for the flag "
                + KeyDef.ENTERLOOPHEAD
                + ": "
                + loopHeadFlags.toString());
      }
      if (loopHeadFlagValues.iterator().next()) {
        return true;
      }
    }
    return false;
  }

  /**
   * Creates an automaton-transition condition to match a specific file location.
   *
   * <p>If no predicate is specified, the resulting condition is {@code AutomatonBoolExpr#TRUE}.
   *
   * @return an automaton-transition condition to match a specific file location.
   */
  private AutomatonBoolExpr getLocationMatcher(
      Optional<Predicate<FileLocation>> pMatcherPredicate) {

    if (!pMatcherPredicate.isPresent()) {
      return AutomatonBoolExpr.TRUE;
    }

    return new AutomatonBoolExpr.MatchLocationDescriptor(
        cfa.getMainFunction(), pMatcherPredicate.get());
  }

  /**
   * Creates a predicate to match file locations based on the line numbers specified by the transition.
   *
   * <p>If no line number is specified by the given transition,
   * the resulting condition is {@link Optional#empty}.</p>
   *
   * @param pTransition the transition specifying which line numbers to assume.
   * @return a predicate to match file locations based on the line numbers specified by the transition.
   */
  private static Optional<Predicate<FileLocation>> getOriginLineMatcherPredicate(Node pTransition)
      throws WitnessParseException {
    Set<String> originFileTags = GraphMLDocumentData.getDataOnNode(pTransition, KeyDef.ORIGINFILE);
    checkParsable(
        originFileTags.size() < 2,
        "At most one origin-file data tag must be provided for an edge.");

    Set<String> startLineTags = GraphMLDocumentData.getDataOnNode(pTransition, KeyDef.STARTLINE);
    checkParsable(
        startLineTags.size() < 2,
        "At most one startline data tag must be provided for each edge.");
    Set<String> endLineTags = GraphMLDocumentData.getDataOnNode(pTransition, KeyDef.ENDLINE);
    checkParsable(
        endLineTags.size() < 2, "At most one endline data tag must be provided for each edge.");

    int startLine = 0;
    if (startLineTags.size() > 0) {
      startLine = Integer.parseInt(startLineTags.iterator().next());
    }
    int endLine = 0;
    if (endLineTags.size() > 0) {
      endLine = Integer.parseInt(endLineTags.iterator().next());
    }
    if (startLine < 1 && endLine > 1) {
      startLine = endLine;
    }
    if (endLine < 1 && startLine >= 1) {
      endLine = startLine;
    }
    if (endLine < startLine) {
      return Optional.of(Predicates.alwaysFalse());
    }

    if (startLine > 0) {
      Optional<String> matchOriginFileName =
          originFileTags.isEmpty()
              ? Optional.empty()
              : Optional.of(originFileTags.iterator().next());
      LineMatcher originDescriptor =
          new LineMatcher(matchOriginFileName, startLine, endLine);
      return Optional.of(originDescriptor);
    }
    return Optional.empty();
  }

  /**
   * Creates a predicate to match file locations based on the offsets specified by the transition.
   *
   * <p>If no character offset is specified by the given transition,
   * the resulting condition is {@link Optional#empty}.</p>
   *
   * @param pTransition the transition specifying which character offset to assume.
   * @return a predicate to match file locations based on the offsets specified by the transition.
   */
  private static Optional<Predicate<FileLocation>> getOffsetMatcherPredicate(Node pTransition) throws WitnessParseException {
    Set<String> originFileTags = GraphMLDocumentData.getDataOnNode(pTransition, KeyDef.ORIGINFILE);
    checkParsable(
        originFileTags.size() < 2,
        "At most one origin-file data tag must be provided for an edge.");

    Set<String> offsetTags = GraphMLDocumentData.getDataOnNode(pTransition, KeyDef.OFFSET);
    checkParsable(
        offsetTags.size() < 2, "At most one offset data tag must be provided for each edge.");
    Set<String> endoffsetTags = GraphMLDocumentData.getDataOnNode(pTransition, KeyDef.ENDOFFSET);
    checkParsable(
        endoffsetTags.size() < 2, "At most one endoffset data tag must be provided for each edge.");

    int offset = -1;
    if (offsetTags.size() > 0) {
      offset = Integer.parseInt(offsetTags.iterator().next());
    }
    int endoffset = -1;
    if (endoffsetTags.size() > 0) {
      endoffset = Integer.parseInt(endoffsetTags.iterator().next());
    }
    if (offset < 0 && endoffset > 0) {
      offset = endoffset;
    }
    if (endoffset < 0 && offset >= 0) {
      endoffset = offset;
    }
    if (endoffset < offset) {
      return Optional.of(Predicates.alwaysFalse());
    }

    if (offset >= 0) {
      Optional<String> matchOriginFileName =
          originFileTags.isEmpty()
              ? Optional.empty()
              : Optional.of(originFileTags.iterator().next());

      OffsetMatcher originDescriptor = new OffsetMatcher(matchOriginFileName, offset, endoffset);
      return Optional.of(originDescriptor);
    }
    return Optional.empty();
  }

  /**
   * Creates an automaton-transition condition for specific branches of an assumption corresponding
   * to the control case specified by the given transition.
   *
   * <p>If no control case is specified by the given transition, the resulting condition is {@code
   * AutomatonBoolExpr#TRUE}.
   *
   * @param pTransition the transition specifying which control case to assume.
   * @return an automaton-transition condition for specific branches of an assumption corresponding
   *     to the control case specified by the given transition.
   */
  private static AutomatonBoolExpr getAssumeCaseMatcher(Node pTransition) throws WitnessParseException {
    Set<String> assumeCaseTags = GraphMLDocumentData.getDataOnNode(pTransition, KeyDef.CONTROLCASE);

    if (assumeCaseTags.size() > 0) {
      checkParsable(
          assumeCaseTags.size() < 2,
          "At most one assume-case tag must be provided for each transition.");
      String assumeCaseStr = assumeCaseTags.iterator().next();
      final boolean assumeCase;
      if (assumeCaseStr.equalsIgnoreCase(AssumeCase.THEN.toString())) {
        assumeCase = true;
      } else if (assumeCaseStr.equalsIgnoreCase(AssumeCase.ELSE.toString())) {
        assumeCase = false;
      } else {
        throw new WitnessParseException("Unrecognized assume case: " + assumeCaseStr);
      }

      return new AutomatonBoolExpr.MatchAssumeCase(assumeCase);
    }
    return AutomatonBoolExpr.TRUE;
  }

  /**
   * Gets the thread specified on the given transition, if any.
   *
   * @param pTransition the transition to parse the thread id from.
   * @param pNumericIdProvider a numeric id provider to map textual thread ids to numeric ones.
   * @return the thread id specified on the transition with the given key, if any.
   * @throws WitnessParseException if more than one thread id was specified.
   */
  private static Optional<GraphMLTransition.GraphMLThread> getThread(
      Node pTransition, NumericIdProvider pNumericIdProvider) throws WitnessParseException {
    return parseThreadId(pTransition, pNumericIdProvider, KeyDef.THREADID, "At most one threadId tag must be provided for each transition.");
  }

  /**
   * Parses the thread specified on the given transition by the given key, if any.
   *
   * @param pTransition the transition to parse the thread id from.
   * @param pNumericIdProvider a numeric id provider to map textual thread ids to numeric ones.
   * @param pKey the key of the data tag to parse the id from.
   * @param pErrorMessage the error message to use for the exception that occurs if more than one
   *     thread if was specified.
   * @return the thread id specified on the transition with the given key, if any.
   * @throws WitnessParseException if more than one thread id was specified.
   */
  private static Optional<GraphMLTransition.GraphMLThread> parseThreadId(
      Node pTransition, NumericIdProvider pNumericIdProvider, KeyDef pKey, String pErrorMessage)
      throws WitnessParseException {
    Set<String> threadIdTags = GraphMLDocumentData.getDataOnNode(pTransition, pKey);

    if (threadIdTags.size() > 0) {
      checkParsable(
          threadIdTags.size() < 2, pErrorMessage);
      String threadId = threadIdTags.iterator().next();
      return Optional.of(
          GraphMLTransition.createThread(pNumericIdProvider.provideNumericId(threadId), threadId));
    }
    return Optional.empty();
  }

  /**
   * Creates an automaton action that assigns the given thread id to the thread-id automaton
   * variable.
   *
   * @param pThreadId the thread id to assign.
   */
  private static AutomatonAction getThreadIdAssignment(int pThreadId) {
    AutomatonIntExpr expr = new AutomatonIntExpr.Constant(pThreadId);
    return new AutomatonAction.Assignment(KeyDef.THREADID.name(), expr);
  }

  /**
   * Reads an automaton edge from the graphml file and inserts it into the automaton.
   *
   * @param pDocDat the GraphML-document-data helper.
   * @param pStates the map from state identifiers to parsed states.
   * @param pEntryStates the set of entry states.
   * @param pLeavingEdges the map from predecessor states to transitions leaving these states that
   *     the given transition will be entered into.
   * @param pEnteringEdges the map from successor states to transitions entering these states that
   *     the given transition will be entered into.
   * @param pNumericThreadIdProvider a numeric id provider to map textual thread ids to numeric
   *     ones.
   * @param pTransition the transition to be analyzed, represented as a GraphML edge.
   */
  private void collectEdgeData(
      GraphMLDocumentData pDocDat,
      Map<String, GraphMLState> pStates,
      Set<GraphMLState> pEntryStates,
      Multimap<GraphMLState, GraphMLTransition> pLeavingEdges,
      Multimap<GraphMLState, GraphMLTransition> pEnteringEdges,
      NumericIdProvider pNumericThreadIdProvider,
      Node pTransition)
      throws WitnessParseException {
    String sourceStateId =
        GraphMLDocumentData.getAttributeValue(
            pTransition, "source", "Every transition needs a source!");
    GraphMLState source = parseState(pDocDat, pStates, sourceStateId, Optional.of(pTransition));

    String targetStateId =
        GraphMLDocumentData.getAttributeValue(
            pTransition, "target", "Every transition needs a target!");
    GraphMLState target = parseState(pDocDat, pStates, targetStateId, Optional.of(pTransition));

    Optional<String> functionEntry = parseSingleDataValue(pTransition, KeyDef.FUNCTIONENTRY,
        "At most one function can be entered by one transition.");

    Optional<String> functionExit = parseSingleDataValue(pTransition, KeyDef.FUNCTIONEXIT,
        "At most one function can be exited by one transition.");
    Optional<String> explicitAssumptionScope = parseSingleDataValue(pTransition, KeyDef.ASSUMPTIONSCOPE,
        "At most one explicit assumption scope must be provided for a transition.");
    Optional<String> assumptionResultFunction =
        parseSingleDataValue(pTransition, KeyDef.ASSUMPTIONRESULTFUNCTION,
            "At most one result function must be provided for a transition.");

    Optional<GraphMLTransition.GraphMLThread> thread = getThread(pTransition, pNumericThreadIdProvider);
    Optional<AutomatonAction> threadIdAssignment =
        thread.isPresent()
            ? Optional.of(getThreadIdAssignment(thread.get().getId()))
            : Optional.empty();

    GraphMLTransition transition =
        new GraphMLTransition(
            source,
            target,
            functionEntry,
            functionExit,
            getOffsetMatcherPredicate(pTransition),
            getOriginLineMatcherPredicate(pTransition),
            getAssumeCaseMatcher(pTransition),
            thread.orElse(DEFAULT_THREAD),
            threadIdAssignment,
            GraphMLDocumentData.getDataOnNode(pTransition, KeyDef.ASSUMPTION),
            explicitAssumptionScope,
            assumptionResultFunction,
            entersLoopHead(pTransition));

    pLeavingEdges.put(source, transition);
    pEnteringEdges.put(target, transition);

    Element sourceStateNode = pDocDat.getNodeWithId(sourceStateId);
    if (sourceStateNode == null) {
      throw new WitnessParseException(
          String.format(
              "Source %s of transition %s does not exist.",
              sourceStateId, transitionToString(pTransition)));
    }
    Element targetStateNode = pDocDat.getNodeWithId(targetStateId);
    if (targetStateNode == null) {
      throw new WitnessParseException(
          String.format(
              "Target %s of transition %s does not exist.",
              targetStateId, transitionToString(pTransition)));
    }

    if (source.isViolationState()) {
      logger.log(
          Level.WARNING,
          String.format(
              "Source %s of transition %s is a violation state. No outgoing edges expected.",
              sourceStateId, transitionToString(pTransition)));
    }

    if (source.isSinkState()) {
      logger.log(
          Level.WARNING,
          String.format(
              "Source %s of transition %s is a sink state. No outgoing edges expected.",
              sourceStateId, transitionToString(pTransition)));
    }

    if (source.isEntryState()) {
      pEntryStates.add(source);
    }
    if (target.isEntryState()) {
      pEntryStates.add(target);
    }
  }

  private GraphMLState parseState(
      GraphMLDocumentData pDocDat,
      Map<String, GraphMLState> pStates,
      String pStateId,
      Optional<Node> pReference)
      throws WitnessParseException {
    GraphMLState result = pStates.get(pStateId);
    if (result != null) {
      return result;
    }

    Element stateNode = pDocDat.getNodeWithId(pStateId);
    if (stateNode == null) {
      final String message;
      if (pReference.isPresent()) {
        message =
            String.format(
                "The state with id <%s> does not exist, but is referenced in the transition <%s>",
                pStateId, transitionToString(pReference.get()));
      } else {
        message = String.format("The state with if <%s> does not exist.", pStateId);
      }
      throw new WitnessParseException(message);
    }

    Set<String> candidates = GraphMLDocumentData.getDataOnNode(stateNode, KeyDef.INVARIANT);
    Optional<String> candidateScope = parseSingleDataValue(stateNode, KeyDef.INVARIANTSCOPE,
        "At most one explicit invariant scope must be provided for a state.");

    result = new GraphMLState(
        pStateId,
        candidates,
        candidateScope,
        pDocDat.getNodeFlags(stateNode));

    pStates.put(pStateId, result);

    return result;
  }

  private static Optional<String> parseSingleDataValue(Node pEdge,
      KeyDef pKey,
      String pErrorMessage) throws WitnessParseException {
    Set<String> values =
        GraphMLDocumentData.getDataOnNode(pEdge, pKey);
    checkParsable(values.size() <= 1, pErrorMessage);
    String value = Iterables.getOnlyElement(values, null);
    return Optional.ofNullable(value);
  }

  /**
   * Gets the witness-automaton type of an automaton represented as a GraphML graph.
   *
   * @param pAutomaton the GraphML graph node representing the witness automaton.
   * @return the witness-automaton type of an automaton represented as a GraphML graph.
   */
  private WitnessType getWitnessType(Node pAutomaton) throws WitnessParseException {
    Set<String> witnessTypeText =
        GraphMLDocumentData.getDataOnNode(pAutomaton, KeyDef.WITNESS_TYPE);
    final WitnessType witnessType;
    if (witnessTypeText.isEmpty()) {
      witnessType = WitnessType.VIOLATION_WITNESS;
    } else if (witnessTypeText.size() > 1) {
      throw new WitnessParseException(AMBIGUOUS_TYPE_ERROR_MESSAGE);
    } else {
      String witnessTypeToParse = witnessTypeText.iterator().next().trim();
      Optional<WitnessType> parsedGraphType = WitnessType.tryParse(witnessTypeToParse);
      if (parsedGraphType.isPresent()) {
        witnessType = parsedGraphType.get();
      } else {
        witnessType = WitnessType.VIOLATION_WITNESS;
        logger.log(
            Level.WARNING,
            String.format(
                "Unknown witness type %s, assuming %s instead.", witnessTypeToParse, witnessType));
      }
    }
    return witnessType;
  }

  private static String transitionToString(Node pTransition) {
    if (pTransition == null) {
      return "null";
    }
    NamedNodeMap attributes = pTransition.getAttributes();
    if (attributes != null) {
      Node id = attributes.getNamedItem("id");
      if (id != null) {
        return id.getNodeValue();
      }
    }
    return pTransition.toString();
  }

  private static void checkRequiredField(Node pGraphNode, KeyDef pKey)
      throws WitnessParseException {
    checkRequiredField(pGraphNode, pKey, false);
  }

  private static void checkRequiredField(Node pGraphNode, KeyDef pKey, boolean pAcceptEmpty)
      throws WitnessParseException {
    Iterable<String> data = GraphMLDocumentData.getDataOnNode(pGraphNode, pKey);
    if (Iterables.isEmpty(data)) {
      throw new WitnessParseException(
          String.format("The witness does not contain the required field '%s'", pKey.id));
    }
    if (!pAcceptEmpty) {
      data = FluentIterable.from(data).filter(s -> !s.trim().isEmpty());
    }
    if (Iterables.isEmpty(data)) {
      throw new WitnessParseException(
          String.format(
              "The witness does not contain a non-empty entry for the required field '%s'",
              pKey.id));
    }
  }

  private void checkHashSum(Set<String> pProgramHashes) throws IOException, WitnessParseException {
    if (pProgramHashes.isEmpty()) {
      final String message;
      if (checkProgramHash) {
        message =
            "Witness does not contain the SHA-256 hash value "
                + "of the program and may therefore be unrelated to the "
                + "verification task it is being validated against.";
      } else {
        message = "Witness does not contain the SHA-256 hash value of the program.";
      }
      if (strictChecking) {
        throw new WitnessParseException(message);
      } else {
        logger.log(Level.WARNING, message);
      }
    } else {
      Set<String> programHashes =
          FluentIterable.from(pProgramHashes)
              .transform(String::trim)
              .transform(String::toLowerCase)
              .toSet();
      List<String> invalidHashes = new ArrayList<>(programHashes.size());
      for (String programHashInWitness : programHashes) {
        if (!VALID_HASH_PATTERN.matcher(programHashInWitness).matches()) {
          invalidHashes.add(programHashInWitness);
        }
      }
      if (!invalidHashes.isEmpty()) {
        StringBuilder messageBuilder = new StringBuilder();
        if (invalidHashes.size() == 1) {
          messageBuilder.append("The value <");
          messageBuilder.append(invalidHashes.iterator().next());
          messageBuilder.append(
              "> given as hash value of the program source code is not a valid SHA-256 hash value for any program.");
        } else {
          messageBuilder.append(
              "None of the following values given as hash values of the program source code is a valid SHA-256 hash value for any program: ");
          for (String invalidHash : invalidHashes) {
            messageBuilder.append("<");
            messageBuilder.append(invalidHash);
            messageBuilder.append(">");
            messageBuilder.append(", ");
          }
          messageBuilder.setLength(messageBuilder.length() - 2);
        }
        if (strictChecking) {
          throw new WitnessParseException(messageBuilder.toString());
        } else {
          logger.log(Level.WARNING, messageBuilder.toString());
        }
      }

      if (checkProgramHash) {
        for (Path programFile : cfa.getFileNames()) {
          String actualProgramHash = AutomatonGraphmlCommon.computeSha1Hash(programFile).toLowerCase();
          String actualSha256Programhash =
              AutomatonGraphmlCommon.computeHash(programFile).toLowerCase();
          if (!programHashes.contains(actualProgramHash)
              && !programHashes.contains(actualSha256Programhash)) {
            throw new WitnessParseException(
                "Neiter the SHA-1 hash value of given verification-task "
                    + "source-code file ("
                    + actualProgramHash
                    + ") not the corresponding SHA-256 hash value ("
                    + actualSha256Programhash
                    + ")"
                    + "match the program hash value given in the witness. "
                    + "The witness is likely unrelated to the verification task.");
          }
        }
      }
    }
  }

  private void checkArchitecture(Set<String> pArchitecture) throws WitnessParseException {
    if (pArchitecture.isEmpty()) {
      String message =
          "Witness does not contain the architecture assumed for the "
              + "verification task. If the architecture assumed by the witness "
              + "differs from the architecture assumed by the validator, "
              + "meaningful validation results cannot be guaranteed.";
      if (strictChecking) {
        throw new WitnessParseException(message);
      } else {
        logger.log(Level.WARNING, message);
      }
    } else if (!pArchitecture.contains(
        AutomatonGraphmlCommon.getArchitecture(cfa.getMachineModel()))) {
      throw new WitnessParseException(
          "The architecture assumed for the given verification-task differs "
              + " from the architecture assumed by the witness. "
              + " Witness validation is meaningless.");
    }
  }

  private Optional<String> determineResultFunction(Optional<String> pResultFunction, Scope pScope) {
    if (pResultFunction.isPresent()) {
      return pResultFunction;
    }
    if (pScope instanceof CProgramScope) {
      CProgramScope scope = (CProgramScope) pScope;
      if (!scope.isGlobalScope()) {
        return Optional.of(scope.getCurrentFunctionName());
      }
    }
    return Optional.empty();
  }

  private Scope determineScope(Optional<String> pExplicitScope, Deque<String> pFunctionStack,
      Predicate<FileLocation> pLocationDescriptor) {
    Scope result = this.scope;
    if (result instanceof CProgramScope) {
      result = ((CProgramScope) result).withLocationDescriptor(pLocationDescriptor);
      if (pExplicitScope.isPresent() || !pFunctionStack.isEmpty()) {
        final String functionName;
        if (pExplicitScope.isPresent()) {
          functionName = pExplicitScope.get();
        } else {
          functionName = pFunctionStack.peek();
        }
        result = ((CProgramScope) result).withFunctionScope(functionName);
      }
    }
    return result;
  }

  private static AutomatonBoolExpr createViolationAssertion() {
    return and(
        not(new AutomatonBoolExpr.ALLCPAQuery(AutomatonState.INTERNAL_STATE_IS_TARGET_PROPERTY))
        );
  }

  private static AutomatonTransition createAutomatonTransition(
      AutomatonBoolExpr pTriggers,
      List<AutomatonBoolExpr> pAssertions,
      List<AExpression> pAssumptions,
      ExpressionTree<AExpression> pCandidateInvariants,
      List<AutomatonAction> pActions,
      GraphMLState pTargetState,
      boolean pLeadsToViolationNode,
      boolean pSinkAsBottomNotBreak) {
    if (pTargetState.isSinkState()) {
      return createAutomatonSinkTransition(
          pTriggers, pAssertions, pActions, pLeadsToViolationNode, pSinkAsBottomNotBreak);
    }
    if (pLeadsToViolationNode) {
      List<AutomatonBoolExpr> assertions = ImmutableList.<AutomatonBoolExpr>builder().addAll(pAssertions).add(createViolationAssertion()).build();
      return new ViolationCopyingAutomatonTransition(
          pTriggers, assertions, pAssumptions, pCandidateInvariants, pActions, pTargetState.getId());
    }
    return new AutomatonTransition(
        pTriggers, pAssertions, pAssumptions, pCandidateInvariants, pActions, pTargetState.getId());
  }

  private static AutomatonTransition createAutomatonSinkTransition(
      AutomatonBoolExpr pTriggers,
      List<AutomatonBoolExpr> pAssertions,
      List<AutomatonAction> pActions,
      boolean pLeadsToViolationNode,
      boolean pSinkAsBottomNotBreak) {
    if (pLeadsToViolationNode) {
      return new ViolationCopyingAutomatonTransition(
          pTriggers,
          pAssertions,
          pActions,
          pSinkAsBottomNotBreak ? AutomatonInternalState.BOTTOM : AutomatonInternalState.BREAK);
    }
    return new AutomatonTransition(
        pTriggers,
        pAssertions,
        pActions,
        pSinkAsBottomNotBreak ? AutomatonInternalState.BOTTOM : AutomatonInternalState.BREAK);
  }

  private static class ViolationCopyingAutomatonTransition extends AutomatonTransition {

    private ViolationCopyingAutomatonTransition(
        AutomatonBoolExpr pTriggers,
        List<AutomatonBoolExpr> pAssertions,
        List<AExpression> pAssumptions,
        ExpressionTree<AExpression> pCandidateInvariants,
        List<AutomatonAction> pActions,
        String pTargetStateId) {
      super(pTriggers, pAssertions, pAssumptions, pCandidateInvariants, pActions, pTargetStateId);
    }

    private ViolationCopyingAutomatonTransition(
        AutomatonBoolExpr pTriggers,
        List<AutomatonBoolExpr> pAssertions,
        List<AutomatonAction> pActions,
        AutomatonInternalState pTargetState) {
      super(pTriggers, pAssertions, pActions, pTargetState);
    }

    @Override
    public String getViolatedPropertyDescription(AutomatonExpressionArguments pArgs) {
      String own = getFollowState().isTarget() ? super.getViolatedPropertyDescription(pArgs) : null;
      Set<String> violatedPropertyDescriptions = new LinkedHashSet<>();

      if (!Strings.isNullOrEmpty(own)) {
        violatedPropertyDescriptions.add(own);
      }

      for (AutomatonState other : FluentIterable.from(pArgs.getAbstractStates()).filter(AutomatonState.class)) {
        if (other != pArgs.getState() && other.getInternalState().isTarget()) {
          String violatedPropDesc = "";

          Optional<AutomatonSafetyProperty> violatedProperty = other.getOptionalViolatedPropertyDescription();
          if (violatedProperty.isPresent()) {
            violatedPropDesc = violatedProperty.get().toString();
          }

          if (!violatedPropDesc.isEmpty()) {
            violatedPropertyDescriptions.add(violatedPropDesc);
          }
        }
      }

      if (violatedPropertyDescriptions.isEmpty() && own == null) {
        return null;
      }

      return Joiner.on(',').join(violatedPropertyDescriptions);
    }

  }

  private static class GraphMLDocumentData {

    private final Node graph;

    private final ImmutableMap<String, Element> idToNodeMap;

    private final Iterable<Node> transitions;

    public GraphMLDocumentData(Document pDocument) throws WitnessParseException {

      NodeList graphs = pDocument.getElementsByTagName(GraphMLTag.GRAPH.toString());
      checkParsable(graphs.getLength() == 1, TOO_MANY_GRAPHS_ERROR_MESSAGE);
      graph = Objects.requireNonNull(graphs.item(0));

      ImmutableMap.Builder<String, Element> idToNodeMapBuilder = ImmutableMap.builder();
      NodeList nodes = pDocument.getElementsByTagName(GraphMLTag.NODE.toString());
      for (Node stateNode : asIterable(nodes)) {
        String stateId = getAttributeValue(stateNode, "id", "Every state needs an ID!");
        idToNodeMapBuilder.put(stateId, (Element) stateNode);
      }
      idToNodeMap = idToNodeMapBuilder.build();

      transitions = asIterable(pDocument.getElementsByTagName(GraphMLTag.EDGE.toString()));
    }

    public Node getGraph() {
      return graph;
    }

    public Iterable<Node> getTransitions() {
      return transitions;
    }

    public EnumSet<NodeFlag> getNodeFlags(Element pStateNode) {
      EnumSet<NodeFlag> result = EnumSet.noneOf(NodeFlag.class);

      NodeList dataChilds = pStateNode.getElementsByTagName(GraphMLTag.DATA.toString());

      for (Node dataChild : asIterable(dataChilds)) {
        Node attribute = dataChild.getAttributes().getNamedItem("key");
        Preconditions.checkNotNull(attribute, "Every data element must have a key attribute!");
        String key = attribute.getTextContent();
        NodeFlag flag = NodeFlag.getNodeFlagByKey(key);
        if (flag != null) {
          result.add(flag);
        }
      }

      return result;
    }

    private static String getAttributeValue(Node of, String attributeName, String exceptionMessage)
        throws WitnessParseException {
      Node attribute = of.getAttributes().getNamedItem(attributeName);
      if (attribute == null) {
        throw new WitnessParseException(exceptionMessage);
      }
      return attribute.getTextContent();
    }

    private @Nullable Element getNodeWithId(String nodeId) {
      Element result = idToNodeMap.get(nodeId);
      if (result == null
          || !result.getTagName().equals(GraphMLTag.NODE.toString())) {
        return null;
      }
      return result;
    }

    private static Set<String> getDataOnNode(Node node, final KeyDef dataKey) {
      Preconditions.checkNotNull(node);
      Preconditions.checkArgument(node.getNodeType() == Node.ELEMENT_NODE);

      Element nodeElement = (Element) node;
      Set<Node> dataNodes = findKeyedDataNode(nodeElement, dataKey);

      Set<String> result = Sets.newHashSet();
      for (Node n: dataNodes) {
        result.add(n.getTextContent());
      }

      return result;
    }

    private static Set<Node> findKeyedDataNode(Element of, final KeyDef dataKey) {
      Set<Node> result = Sets.newHashSet();
      Set<Node> alternative = null;
      NodeList dataChilds = of.getElementsByTagName(GraphMLTag.DATA.toString());
      for (Node dataChild : asIterable(dataChilds)) {
        Node attribute = dataChild.getAttributes().getNamedItem("key");
        Preconditions.checkNotNull(attribute, "Every data element must have a key attribute!");
        String nodeKey = attribute.getTextContent();
        if (nodeKey.equals(dataKey.id)) {
          result.add(dataChild);
          alternative = null;
        }
        // Backwards-compatibility: type/graph-type
        if (alternative == null
            && result.isEmpty()
            && dataKey.equals(KeyDef.WITNESS_TYPE)
            && nodeKey.equals("type")) {
          alternative = Sets.newHashSet();
          alternative.add(dataChild);
        }
      }
      if (result.isEmpty() && alternative != null) {
        return alternative;
      }
      return result;
    }

  }

  public static boolean isGraphmlAutomatonFromConfiguration(Path pPath)
      throws InvalidConfigurationException {
    try {
      return isGraphmlAutomaton(pPath);
    } catch (FileNotFoundException e) {
      throw new WitnessParseException(
          "Invalid witness file provided! File not found: " + pPath);
    } catch (IOException e) {
      throw new WitnessParseException(e);
    }
  }

  public static boolean isGraphmlAutomaton(Path pPath) throws IOException {
    SAXParser saxParser;
    try {
      saxParser = SAXParserFactory.newInstance().newSAXParser();
    } catch (ParserConfigurationException | SAXException e) {
      throw new AssertionError(
          "SAX parser configured incorrectly. Could not determine whether or not the file describes a witness automaton.",
          e);
    }
    DefaultHandler defaultHandler = new DefaultHandler();
    try {
      try (InputStream input = Files.newInputStream(pPath);
          GZIPInputStream zipInput = new GZIPInputStream(input)) {
        saxParser.parse(zipInput, defaultHandler);
      } catch (IOException e) {
        try (InputStream plainInput = Files.newInputStream(pPath)) {
          saxParser.parse(plainInput, defaultHandler);
        }
      }
      return true;
    } catch (SAXException e) {
      return false;
    }
  }

  public static AutomatonGraphmlCommon.WitnessType getWitnessType(Path pPath)
      throws InvalidConfigurationException {
    return AutomatonGraphmlParser
        .<AutomatonGraphmlCommon.WitnessType, InvalidConfigurationException>
            handlePotentiallyGZippedInput(
                MoreFiles.asByteSource(pPath),
                inputStream -> getWitnessType(inputStream),
                e -> new WitnessParseException(e));
  }

  private static AutomatonGraphmlCommon.WitnessType getWitnessType(InputStream pInputStream)
      throws InvalidConfigurationException, IOException {
    // Parse the XML document ----
    DocumentBuilderFactory docFactory = DocumentBuilderFactory.newInstance();
    Document doc;
    try {
      DocumentBuilder docBuilder = docFactory.newDocumentBuilder();
      doc = docBuilder.parse(pInputStream);
    } catch (ParserConfigurationException | SAXException e) {
      throw new WitnessParseException(e);
    }

    // (The one) root node of the graph ----
    NodeList graphs = doc.getElementsByTagName(GraphMLTag.GRAPH.toString());
    checkParsable(graphs.getLength() == 1, TOO_MANY_GRAPHS_ERROR_MESSAGE);
    Node graphNode = graphs.item(0);

    checkRequiredField(graphNode, KeyDef.WITNESS_TYPE);

    Set<String> graphTypeText = GraphMLDocumentData.getDataOnNode(graphNode, KeyDef.WITNESS_TYPE);
    final WitnessType graphType;
    if (graphTypeText.isEmpty()) {
      graphType = WitnessType.VIOLATION_WITNESS;
    } else if (graphTypeText.size() > 1) {
      throw new WitnessParseException(AMBIGUOUS_TYPE_ERROR_MESSAGE);
    } else {
      String witnessTypeToParse = graphTypeText.iterator().next().trim();
      Optional<WitnessType> parsedWitnessType = WitnessType.tryParse(witnessTypeToParse);
      if (parsedWitnessType.isPresent()) {
        graphType = parsedWitnessType.get();
      } else {
        throw new WitnessParseException("Witness type not recognized: " + witnessTypeToParse);
      }
    }
    return graphType;
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

  private static AutomatonBoolExpr and(AutomatonBoolExpr pA, AutomatonBoolExpr pB) {
    if (pA.equals(AutomatonBoolExpr.TRUE) || pB.equals(AutomatonBoolExpr.FALSE)) {
      return pB;
    }
    if (pB.equals(AutomatonBoolExpr.TRUE) || pA.equals(AutomatonBoolExpr.FALSE)) {
      return pA;
    }
    return new AutomatonBoolExpr.And(pA, pB);
  }

  private static AutomatonBoolExpr and(AutomatonBoolExpr... pExpressions) {
    AutomatonBoolExpr result = AutomatonBoolExpr.TRUE;
    for (AutomatonBoolExpr e : pExpressions) {
      result = and(result, e);
    }
    return result;
  }

  private static AutomatonBoolExpr or(AutomatonBoolExpr pA, AutomatonBoolExpr pB) {
    if (pA.equals(AutomatonBoolExpr.TRUE) || pB.equals(AutomatonBoolExpr.FALSE)) {
      return pA;
    }
    if (pB.equals(AutomatonBoolExpr.TRUE) || pA.equals(AutomatonBoolExpr.FALSE)) {
      return pB;
    }
    return new AutomatonBoolExpr.Or(pA, pB);
  }

  private static void checkParsable(boolean pParsable, String pMessage)
      throws WitnessParseException {
    if (!pParsable) {
      throw new WitnessParseException(pMessage);
    }
  }

  public static class WitnessParseException extends InvalidConfigurationException {

    private static final String PARSE_EXCEPTION_MESSAGE_PREFIX = "Cannot parse witness: ";

    private static final long serialVersionUID = -6357416712866877118L;

    public WitnessParseException(String pMessage) {
      super(PARSE_EXCEPTION_MESSAGE_PREFIX + pMessage);
    }

    public WitnessParseException(String pMessage, Exception pCause) {
      super(PARSE_EXCEPTION_MESSAGE_PREFIX + pMessage, pCause);
    }

    public WitnessParseException(Throwable pCause) {
      super(PARSE_EXCEPTION_MESSAGE_PREFIX + AutomatonGraphmlParser.getMessage(pCause), pCause);
    }
  }

  private static String getMessage(Throwable pException) {
    String message = pException.getMessage();
    if (message == null) {
      message = "Exception occurred, but details are unknown: " + pException.toString();
    }
    if (pException instanceof IOException) {
      return String.format(ACCESS_ERROR_MESSAGE, message);
    }
    return message;
  }

  private static interface InputHandler<T, E extends Throwable> {

    T handleInput(InputStream pInputStream) throws E, IOException;
  }

  private static <T, E extends Throwable> T handlePotentiallyGZippedInput(
      ByteSource pInputSource,
      InputHandler<T, E> pInputHandler,
      Function<IOException, E> pExceptionHandler)
      throws E {
    try {
      try (InputStream inputStream = pInputSource.openStream();
          InputStream gzipInputStream = new GZIPInputStream(inputStream)) {
        return pInputHandler.handleInput(gzipInputStream);
      } catch (IOException e) {
        try (InputStream plainInputStream = pInputSource.openStream()) {
          return pInputHandler.handleInput(plainInputStream);
        }
      }
    } catch (IOException e) {
      throw pExceptionHandler.apply(e);
    }
  }


  /** return a nice {@link Iterable} wrapping the interface {@link NodeList}. */
  private static Iterable<Node> asIterable(final NodeList pNodeList) {
    return new Iterable<Node>() {

      @Override
      public Iterator<Node> iterator() {
        return new Iterator<Node>() {

          private int index = 0;

          @Override
          public boolean hasNext() {
            return index < pNodeList.getLength();
          }

          @Override
          public Node next() {
            return pNodeList.item(index++);
          }
        };
      }
    };
  }
}
