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

import com.google.common.base.Function;
import com.google.common.base.Joiner;
import com.google.common.base.Preconditions;
import com.google.common.base.Predicates;
import com.google.common.base.Strings;
import com.google.common.collect.FluentIterable;
import com.google.common.collect.HashMultimap;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.Iterables;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.google.common.collect.Multimap;
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
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Queue;
import java.util.Set;
import java.util.function.Predicate;
import java.util.logging.Level;
import java.util.stream.Collectors;
import java.util.zip.GZIPInputStream;
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
import org.sosy_lab.cpachecker.cpa.automaton.SourceLocationMatcher.LineMatcher;
import org.sosy_lab.cpachecker.cpa.automaton.SourceLocationMatcher.OffsetMatcher;
import org.sosy_lab.cpachecker.util.SpecificationProperty.PropertyType;
import org.sosy_lab.cpachecker.util.automaton.AutomatonGraphmlCommon;
import org.sosy_lab.cpachecker.util.automaton.AutomatonGraphmlCommon.AssumeCase;
import org.sosy_lab.cpachecker.util.automaton.AutomatonGraphmlCommon.GraphMLTag;
import org.sosy_lab.cpachecker.util.automaton.AutomatonGraphmlCommon.KeyDef;
import org.sosy_lab.cpachecker.util.automaton.AutomatonGraphmlCommon.NodeFlag;
import org.sosy_lab.cpachecker.util.automaton.AutomatonGraphmlCommon.WitnessType;
import org.sosy_lab.cpachecker.util.expressions.And;
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

  private static final String AMBIGUOUS_TYPE_ERROR_MESSAGE = "Witness type must be unambiguous";

  private static final String TOO_MANY_GRAPHS_ERROR_MESSAGE =
      "The witness file must describe exactly one witness automaton.";

  private static final String ACCESS_ERROR_MESSAGE = "Error while accessing witness file: %s!";

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

  @Option(secure = true, description = "Match the thread identifier for concurrent programs.")
  private boolean matchThreadId = true;

  @Option(
    secure = true,
    description =
        "Check that the value of the programhash field of the witness matches the SHA-1 hash value computed for the source code."
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
   *
   * @throws InvalidConfigurationException if the configuration is invalid.
   *
   * @return the automata representing the witnesses found in the file.
   */
  public List<Automaton> parseAutomatonFile(Path pInputFile) throws InvalidConfigurationException {
    return parseAutomatonFile(MoreFiles.asByteSource(pInputFile));
  }

  /**
   * Parses a witness specification from a ByteSource and returns the Automata found in the source.
   *
   * @param pInputSource the ByteSource to parse the witness from.
   * @throws InvalidConfigurationException if the configuration is invalid.
   * @return the automata representing the witnesses found in the source.
   */
  private List<Automaton> parseAutomatonFile(ByteSource pInputSource)
      throws InvalidConfigurationException {
    return AutomatonGraphmlParser
        .<List<Automaton>, InvalidConfigurationException>handlePotentiallyGZippedInput(
            pInputSource,
            inputStream -> parseAutomatonFile(inputStream),
            e -> new WitnessParseException(e));
  }

  /**
   * Parses a specification from an InputStream and returns the Automata found in the file.
   *
   * @param pInputStream the input stream to parse the witness from.
   *
   * @throws InvalidConfigurationException if the configuration is invalid.
   * @throws IOException if there occurs an IOException while reading from the stream.
   *
   * @return the automata representing the witnesses found in the stream.
   */
  private List<Automaton> parseAutomatonFile(InputStream pInputStream)
      throws InvalidConfigurationException, IOException {
    final CParser cparser =
        CParser.Factory.getParser(
            logger, CParser.Factory.getOptions(config), cfa.getMachineModel());
    try {
      // Parse the XML document ----
      DocumentBuilderFactory docFactory = DocumentBuilderFactory.newInstance();
      DocumentBuilder docBuilder = docFactory.newDocumentBuilder();
      Document doc = docBuilder.parse(pInputStream);
      doc.getDocumentElement().normalize();

      GraphMLDocumentData docDat = new GraphMLDocumentData(doc);

      // The (one) root node of the graph ----
      NodeList graphs = doc.getElementsByTagName(GraphMLTag.GRAPH.toString());
      checkParsable(graphs.getLength() == 1, TOO_MANY_GRAPHS_ERROR_MESSAGE);
      Node graphNode = graphs.item(0);

      Set<String> programHash = GraphMLDocumentData.getDataOnNode(graphNode, KeyDef.PROGRAMHASH);
      checkHashSum(programHash);

      if (strictChecking) {
        checkRequiredField(graphNode, KeyDef.WITNESS_TYPE);
        checkRequiredField(graphNode, KeyDef.SOURCECODELANGUAGE);
        checkRequiredField(graphNode, KeyDef.PRODUCER);
        checkRequiredField(graphNode, KeyDef.SPECIFICATION);
        checkRequiredField(graphNode, KeyDef.PROGRAMFILE);
      }

      Set<String> architecture = GraphMLDocumentData.getDataOnNode(graphNode, KeyDef.ARCHITECTURE);
      checkArchitecture(architecture);

      final WitnessType graphType = getWitnessType(graphNode);
      final Set<PropertyType> specType = getSpecAsProperties(graphNode);

      // Extract the information on the automaton ----
      Node nameAttribute = graphNode.getAttributes().getNamedItem("name");
      String automatonName = WITNESS_AUTOMATON_NAME;
      if (nameAttribute != null) {
        automatonName += "_" + nameAttribute.getTextContent();
      }
      String initialStateName = null;

      // Create transitions ----
      //AutomatonBoolExpr epsilonTrigger = new SubsetMatchEdgeTokens(Collections.<Comparable<Integer>>emptySet());
      NodeList edges = doc.getElementsByTagName(GraphMLTag.EDGE.toString());
      NodeList nodes = doc.getElementsByTagName(GraphMLTag.NODE.toString());
      Map<String, LinkedList<AutomatonTransition>> stateTransitions = Maps.newHashMap();
      Map<String, Deque<String>> stacks = Maps.newHashMap();

      // Create graph
      Multimap<String, Node> leavingEdges = HashMultimap.create();
      Multimap<String, Node> enteringEdges = HashMultimap.create();

      Set<String> violationStates = Sets.newHashSet();
      Set<String> sinkStates = Sets.newHashSet();

      for (Node edge : asIterable(edges)) {
        collectEdgeData(docDat, leavingEdges, enteringEdges, violationStates, sinkStates, edge);
      }

      final String entryStateId = getEntryState(docDat, nodes);

      // Determine distances to violation states
      final Map<String, Integer> distances =
          determineDistanceToViolation(enteringEdges, violationStates, sinkStates);

      Map<String, AutomatonBoolExpr> stutterConditions = Maps.newHashMap();
      final Map<String, AutomatonVariable> automatonVariables = new HashMap<>();

      Set<Node> visitedEdges = new HashSet<>();
      Queue<Node> waitingEdges = new ArrayDeque<>();
      waitingEdges.addAll(leavingEdges.get(entryStateId));
      visitedEdges.addAll(waitingEdges);
      while (!waitingEdges.isEmpty()) {
        Node transition = waitingEdges.poll();

        String sourceStateId =
            GraphMLDocumentData.getAttributeValue(
                transition, "source", "Every transition needs a source!");
        String targetStateId =
            GraphMLDocumentData.getAttributeValue(
                transition, "target", "Every transition needs a target!");

        if (graphType == WitnessType.CORRECTNESS_WITNESS && sinkStates.contains(targetStateId)) {
          throw new WitnessParseException("Proof witnesses do not allow sink nodes.");
        }

        for (Node successorEdge : leavingEdges.get(targetStateId)) {
          if (visitedEdges.add(successorEdge)) {
            waitingEdges.add(successorEdge);
          }
        }

        Element targetStateNode = docDat.getNodeWithId(targetStateId);
        EnumSet<NodeFlag> targetNodeFlags = docDat.getNodeFlags(targetStateNode);

        boolean leadsToViolationNode = targetNodeFlags.contains(NodeFlag.ISVIOLATION);
        if (leadsToViolationNode) {
          violationStates.add(targetStateId);
        }

        final List<AutomatonAction> actions = new ArrayList<>(0);
        if (graphType == WitnessType.VIOLATION_WITNESS) {
          actions.add(
              new AutomatonAction.Assignment(
                  DISTANCE_TO_VIOLATION,
                  new AutomatonIntExpr.Constant(
                      -distances.getOrDefault(targetStateId, Integer.MAX_VALUE))));
        }

        Optional<Predicate<FileLocation>> offsetMatcherPredicate = getOffsetMatcherPredicate(transition);
        Optional<Predicate<FileLocation>> lineMatcherPredicate = getOriginLineMatcherPredicate(transition);
        Predicate<FileLocation> locationMatcherPredicate = Predicates.alwaysTrue();
        if (offsetMatcherPredicate.isPresent()) {
          locationMatcherPredicate = locationMatcherPredicate.and(offsetMatcherPredicate.get());
        }
        if (lineMatcherPredicate.isPresent()) {
          locationMatcherPredicate = locationMatcherPredicate.and(lineMatcherPredicate.get());
        }

        if (matchThreadId) {
          AutomatonAction threadAssignment = getThreadIdAssignment(transition);
          if (threadAssignment != null) {
            actions.add(threadAssignment);
            // define new variable in automaton,
            // this would be sufficient once and not per iteration, but who cares...
            automatonVariables.put(
                KeyDef.THREADID.name(), new AutomatonVariable("int", KeyDef.THREADID.name()));
          }
        }

        List<AExpression> assumptions = Lists.newArrayList();
        ExpressionTree<AExpression> candidateInvariants = ExpressionTrees.getTrue();

        LinkedList<AutomatonTransition> transitions = stateTransitions.get(sourceStateId);
        if (transitions == null) {
          transitions = Lists.newLinkedList();
          stateTransitions.put(sourceStateId, transitions);
        }

        // Handle call stack
        Deque<String> currentStack = stacks.get(sourceStateId);
        if (currentStack == null) {
          currentStack = new ArrayDeque<>();
          stacks.put(sourceStateId, currentStack);
        }
        Deque<String> newStack = currentStack;
        Set<String> functionEntries =
            GraphMLDocumentData.getDataOnNode(transition, KeyDef.FUNCTIONENTRY);
        String functionEntry = Iterables.getOnlyElement(functionEntries, null);
        Set<String> functionExits =
            GraphMLDocumentData.getDataOnNode(transition, KeyDef.FUNCTIONEXIT);
        String functionExit = Iterables.getOnlyElement(functionExits, null);

        // If the same function is entered and exited, the stack remains unchanged.
        // Otherwise, adjust the stack accordingly:
        if (!Objects.equals(functionEntry, functionExit)) {
          // First, perform the function exit
          if (!functionExits.isEmpty()) {
            if (newStack.isEmpty()) {
              logger.log(Level.WARNING, "Trying to return from function", functionExit, "although no function is on the stack.");
            } else {
              newStack = new ArrayDeque<>(newStack);
              String oldFunction = newStack.pop();
              assert oldFunction.equals(functionExit);
            }
          }
          // Now enter the new function
          if (!functionEntries.isEmpty()) {
            newStack = new ArrayDeque<>(newStack);
            newStack.push(functionEntry);
          }
        }
        // Store the stack in its state after the edge is applied
        stacks.put(targetStateId, newStack);

        // If the edge enters and exits the same function, assume this function for this edge only
        if (functionEntry != null
            && functionEntry.equals(functionExit)
            && (newStack.isEmpty() || !newStack.peek().equals(functionExit))) {
          newStack = new ArrayDeque<>(newStack);
        }

        // Never match on the dummy edge directly after the main function entry node
        AutomatonBoolExpr conjoinedTriggers = not(AutomatonBoolExpr.MatchProgramEntry.INSTANCE);
        // Never match on artificially split declarations
        conjoinedTriggers =
            and(conjoinedTriggers, not(AutomatonBoolExpr.MatchSplitDeclaration.INSTANCE));

        // Match a loop start
        boolean entersLoopHead = entersLoopHead(transition);
        if (entersLoopHead) {
          conjoinedTriggers = and(conjoinedTriggers, AutomatonBoolExpr.MatchLoopStart.INSTANCE);
        }

        // Add assumptions to the transition
        Set<String> assumptionScopes =
            GraphMLDocumentData.getDataOnNode(transition, KeyDef.ASSUMPTIONSCOPE);
        Scope scope = determineScope(assumptionScopes, newStack, locationMatcherPredicate);
        Set<String> assumptionResultFunctions =
            GraphMLDocumentData.getDataOnNode(transition, KeyDef.ASSUMPTIONRESULTFUNCTION);
        Optional<String> assumptionResultFunction = determineResultFunction(assumptionResultFunctions, scope);
        if (considerAssumptions) {
          Set<String> transAssumes =
              GraphMLDocumentData.getDataOnNode(transition, KeyDef.ASSUMPTION);
          assumptions.addAll(
              CParserUtils.convertStatementsToAssumptions(
                  CParserUtils.parseStatements(transAssumes, assumptionResultFunction, cparser,
                      scope, parserTools),
                  cfa.getMachineModel(),
                  logger));
          if (graphType == WitnessType.CORRECTNESS_WITNESS && !assumptions.isEmpty()) {
            throw new WitnessParseException(
                "Assumptions are not allowed for correctness witnesses.");
          }
        }

        if (graphType == WitnessType.VIOLATION_WITNESS && !assumptionResultFunctions.isEmpty()) {
          String resultFunctionName = assumptionResultFunction.get();
          conjoinedTriggers =
              and(conjoinedTriggers, new AutomatonBoolExpr.MatchFunctionCallStatement(resultFunctionName));
        }

        Set<String> candidates =
            GraphMLDocumentData.getDataOnNode(targetStateNode, KeyDef.INVARIANT);
        Set<String> candidateScopes =
            GraphMLDocumentData.getDataOnNode(targetStateNode, KeyDef.INVARIANTSCOPE);
        final Scope candidateScope = determineScope(candidateScopes, newStack, locationMatcherPredicate);
        Set<String> resultFunctions =
            GraphMLDocumentData.getDataOnNode(transition, KeyDef.ASSUMPTIONRESULTFUNCTION);
        Optional<String> resultFunction = determineResultFunction(resultFunctions, scope);
        if (!candidates.isEmpty()) {
          if (graphType == WitnessType.VIOLATION_WITNESS
              && !specType.contains(PropertyType.TERMINATION)) {
            throw new WitnessParseException("Invariants are not allowed for violation witnesses.");
          }
          candidateInvariants =
              And.of(
                  candidateInvariants,
                  CParserUtils.parseStatementsAsExpressionTree(
                      candidates, resultFunction, cparser, candidateScope, parserTools));
        }

        if (matchOriginLine) {
          conjoinedTriggers = and(conjoinedTriggers, getLocationMatcher(entersLoopHead, lineMatcherPredicate));
        }

        if (matchOffset) {
          conjoinedTriggers = and(conjoinedTriggers, getLocationMatcher(entersLoopHead, offsetMatcherPredicate));
        }

        if (functionExit != null) {
          conjoinedTriggers = and(conjoinedTriggers, getFunctionExitMatcher(functionExit, entersLoopHead));
        }

        // If the transition represents a function call, add a sink transition
        // in case it is a function pointer call,
        // where we can eliminate the other branch
        AutomatonBoolExpr fpElseTrigger = null;
        if (functionEntry != null && graphType == WitnessType.CORRECTNESS_WITNESS) {
          fpElseTrigger = and(
              conjoinedTriggers,
              getFunctionPointerAssumeCaseMatcher(functionEntry, targetNodeFlags.contains(NodeFlag.ISSINKNODE), entersLoopHead));
          transitions.add(
              createAutomatonSinkTransition(
                  fpElseTrigger,
                  Collections.<AutomatonBoolExpr>emptyList(),
                  actions,
                  false,
                  stopNotBreakAtSinkStates));
        }

        if (functionEntry != null) {
          conjoinedTriggers = and(conjoinedTriggers, getFunctionCallMatcher(functionEntry, entersLoopHead));
        }

        if (matchAssumeCase) {
          conjoinedTriggers = and(conjoinedTriggers, getAssumeCaseMatcher(transition));
        }

        // If the triggers do not apply, none of the above transitions is taken,
        // so we need to build the stutter condition
        // as the conjoined negations of the transition conditions.
        AutomatonBoolExpr stutterCondition = stutterConditions.get(sourceStateId);
        AutomatonBoolExpr additionalStutterCondition = not(conjoinedTriggers);
        if (fpElseTrigger != null) {
          additionalStutterCondition = and(additionalStutterCondition, not(fpElseTrigger));
        }
        if (stutterCondition == null) {
          stutterCondition = additionalStutterCondition;
        } else {
          stutterCondition = and(stutterCondition, additionalStutterCondition);
        }
        stutterConditions.put(sourceStateId, stutterCondition);

        // If the triggers match, there must be one successor state that moves the automaton
        // forwards
        transitions.add(
            createAutomatonTransition(
                conjoinedTriggers,
                Collections.<AutomatonBoolExpr>emptyList(),
                assumptions,
                candidateInvariants,
                actions,
                targetStateId,
                leadsToViolationNode,
                stopNotBreakAtSinkStates,
                sinkStates));

        // Multiple CFA edges in a sequence might match the triggers,
        // so in that case we ALSO need a transition back to the source state
        if (!assumptions.isEmpty() || !actions.isEmpty() || !candidateInvariants.equals(ExpressionTrees.getTrue()) || leadsToViolationNode) {
          Element sourceNode = docDat.getNodeWithId(sourceStateId);
          Set<NodeFlag> sourceNodeFlags = docDat.getNodeFlags(sourceNode);
          boolean sourceIsViolationNode = sourceNodeFlags.contains(NodeFlag.ISVIOLATION);
          transitions.add(
              createAutomatonTransition(
                  and(
                      conjoinedTriggers,
                      new AutomatonBoolExpr.MatchAnySuccessorEdgesBoolExpr(conjoinedTriggers)),
                  Collections.<AutomatonBoolExpr>emptyList(),
                  Collections.emptyList(),
                  ExpressionTrees.<AExpression>getTrue(),
                  Collections.<AutomatonAction>emptyList(),
                  sourceStateId,
                  sourceIsViolationNode,
                  stopNotBreakAtSinkStates,
                  sinkStates));
        }
      }

      // Create states ----
      List<AutomatonInternalState> automatonStates = Lists.newArrayList();
      for (Map.Entry<String, Element> stateEntry : docDat.getIdToNodeMap().entrySet()) {
        String stateId = stateEntry.getKey();
        Element stateNode = stateEntry.getValue();
        EnumSet<NodeFlag> nodeFlags = docDat.getNodeFlags(stateNode);

        List<AutomatonTransition> transitions = stateTransitions.get(stateId);
        if (transitions == null) {
          transitions = new ArrayList<>();
        }

        // If the transition conditions do not apply, none of the above transitions is taken,
        // and instead, the stutter condition applies.
        AutomatonBoolExpr stutterCondition = stutterConditions.get(stateId);
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
                stateId,
                violationStates.contains(stateId),
                stopNotBreakAtSinkStates,
                sinkStates));

        if (nodeFlags.contains(NodeFlag.ISVIOLATION)) {
          AutomatonBoolExpr otherAutomataSafe = createViolationAssertion();
          List<AutomatonBoolExpr> assertions = Collections.singletonList(otherAutomataSafe);
          transitions.add(
              createAutomatonTransition(
                  AutomatonBoolExpr.TRUE,
                  assertions,
                  Collections.emptyList(),
                  ExpressionTrees.<AExpression>getTrue(),
                  Collections.<AutomatonAction>emptyList(),
                  stateId,
                  true,
                  stopNotBreakAtSinkStates,
                  sinkStates));
        }

        if (nodeFlags.contains(NodeFlag.ISENTRY)) {
          checkParsable(initialStateName == null, "Only one entry state is supported!");
          initialStateName = stateId;
        }

        // Determine if "matchAll" should be enabled
        boolean matchAll = true;

        AutomatonInternalState state =
            new AutomatonInternalState(
                stateId, transitions, false, matchAll, nodeFlags.contains(NodeFlag.ISCYCLEHEAD));
        automatonStates.add(state);
      }

      // Build and return the result
      Preconditions.checkNotNull(initialStateName, "Every witness needs a specified entry state!");
      if (graphType == WitnessType.VIOLATION_WITNESS) {
        AutomatonVariable distanceVariable = new AutomatonVariable("int", DISTANCE_TO_VIOLATION);
        Integer initialStateDistance = distances.get(initialStateName);
        if (initialStateDistance != null) {
          distanceVariable.setValue(-initialStateDistance);
        } else {
          logger.log(
              Level.WARNING,
              String.format(
                  "There is no path from the entry state %s"
                      + " to a state explicitly marked as violation state."
                      + " Distance-to-violation waitlist order will not work"
                      + " and witness validation may fail to confirm this witness.",
                  initialStateName));
        }
        automatonVariables.put(DISTANCE_TO_VIOLATION, distanceVariable);
      }
      List<Automaton> result = Lists.newArrayList();
      Automaton automaton = new Automaton(automatonName, automatonVariables, automatonStates, initialStateName);
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

    } catch (ParserConfigurationException | SAXException e) {
      throw new WitnessParseException(e);
    } catch (InvalidAutomatonException e) {
      throw new WitnessParseException("The witness automaton provided is invalid!", e);
    }
  }

  private static AutomatonBoolExpr getFunctionCallMatcher(String pEnteredFunction, boolean pEntersLoopHead) {
    AutomatonBoolExpr functionEntryMatcher =
        new AutomatonBoolExpr.MatchFunctionCall(pEnteredFunction);
    if (pEntersLoopHead) {
      functionEntryMatcher =
          AutomatonBoolExpr.EpsilonMatch.backwardEpsilonMatch(functionEntryMatcher, true);
    }
    return functionEntryMatcher;
  }

  private static AutomatonBoolExpr getFunctionPointerAssumeCaseMatcher(String pEnteredFunction,
      boolean pIsSinkNode, boolean pEntersLoopHead) {
    AutomatonBoolExpr functionPointerAssumeCaseMatcher =
      new AutomatonBoolExpr.MatchFunctionPointerAssumeCase(
          new AutomatonBoolExpr.MatchAssumeCase(pIsSinkNode),
          new AutomatonBoolExpr.MatchFunctionCall(pEnteredFunction));
    if (pEntersLoopHead) {
      functionPointerAssumeCaseMatcher =
          AutomatonBoolExpr.EpsilonMatch.backwardEpsilonMatch(functionPointerAssumeCaseMatcher, true);
    }
    return functionPointerAssumeCaseMatcher;
  }

  private static AutomatonBoolExpr getFunctionExitMatcher(String pExitedFunction, boolean pEntersLoopHead) {
    AutomatonBoolExpr functionExitMatcher = or(
        new AutomatonBoolExpr.MatchFunctionExit(pExitedFunction),
        new AutomatonBoolExpr.MatchFunctionCallStatement(pExitedFunction));
    if (pEntersLoopHead) {
      functionExitMatcher =
          AutomatonBoolExpr.EpsilonMatch.backwardEpsilonMatch(functionExitMatcher, true);
    }
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
   * <p>If no predicate is specified, the resulting condition is
   * {@code AutomatonBoolExpr#TRUE}.</p>
   *
   * @param pEntersLoopHead if {@code true} and a predicate is specified,
   * the condition is wrapped as a backward epsilon match.
   *
   * @return an automaton-transition condition to match a specific file location.
   */
  private AutomatonBoolExpr getLocationMatcher(boolean pEntersLoopHead, Optional<Predicate<FileLocation>> pMatcherPredicate) {

    if (!pMatcherPredicate.isPresent()) {
      return AutomatonBoolExpr.TRUE;
    }

    AutomatonBoolExpr offsetMatchingExpr =
        new AutomatonBoolExpr.MatchLocationDescriptor(cfa.getMainFunction(), pMatcherPredicate.get());

    if (pEntersLoopHead) {
      offsetMatchingExpr =
          AutomatonBoolExpr.EpsilonMatch.backwardEpsilonMatch(offsetMatchingExpr, true);
    }
    return offsetMatchingExpr;
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
  private Optional<Predicate<FileLocation>> getOriginLineMatcherPredicate(Node pTransition) throws WitnessParseException {
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

      AutomatonBoolExpr assumeCaseMatchingExpr = new AutomatonBoolExpr.MatchAssumeCase(assumeCase);
      if (entersLoopHead(pTransition)) {
        assumeCaseMatchingExpr =
            AutomatonBoolExpr.EpsilonMatch.backwardEpsilonMatch(assumeCaseMatchingExpr, true);
      }

      return assumeCaseMatchingExpr;
    }
    return AutomatonBoolExpr.TRUE;
  }

  /**
   * Collects data about the given transition and the states it connects.
   * Build an AutomatonAction to set a given thraedId for an active thread at the current edge.
   *
   * <p>Returns {@null}, if no data can be found.
   */
  private static AutomatonAction getThreadIdAssignment(Node transition)
      throws WitnessParseException {
    Set<String> threadIdTags =
        GraphMLDocumentData.getDataOnNode(transition, KeyDef.THREADID);

    if (threadIdTags.size() > 0) {
      checkParsable(
          threadIdTags.size() < 2, "At most one threadId tag must be provided for each edge.");
      String threadIdStr = threadIdTags.iterator().next();
      // TODO use unique Integer for each identifier
      int threadId = threadIdStr.hashCode();
      AutomatonIntExpr expr = new AutomatonIntExpr.Constant(threadId);
      return new AutomatonAction.Assignment(KeyDef.THREADID.name(), expr);
    }
    return null;
  }

  /**
   * Reads an automaton edge from the graphml file and inserts it into the automaton.
   *
   * @param pDocDat the GraphML-document-data helper.
   * @param pLeavingEdges the map from predecessor states to transitions leaving these states that
   *     the given transition will be entered into.
   * @param pEnteringEdges the map from successor states to transitions entering these states that
   *     the given transition will be entered into.
   * @param pViolationStates the set of violation states the predecessor or successor states of the
   *     given transition will be entered into if they are violation states.
   * @param pSinkStates the set of sink states the predecessor or successor states of the given
   *     transition will be entered into if they are sink states.
   * @param pTransition the transition to be analyzed, represented as a GraphML edge.
   */
  private void collectEdgeData(
      GraphMLDocumentData pDocDat,
      Multimap<String, Node> pLeavingEdges,
      Multimap<String, Node> pEnteringEdges,
      Set<String> pViolationStates,
      Set<String> pSinkStates,
      Node pTransition)
      throws WitnessParseException {
    String sourceStateId =
        GraphMLDocumentData.getAttributeValue(
            pTransition, "source", "Every transition needs a source!");
    String targetStateId =
        GraphMLDocumentData.getAttributeValue(
            pTransition, "target", "Every transition needs a target!");
    pLeavingEdges.put(sourceStateId, pTransition);
    pEnteringEdges.put(targetStateId, pTransition);

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
    EnumSet<NodeFlag> sourceNodeFlags = pDocDat.getNodeFlags(sourceStateNode);
    EnumSet<NodeFlag> targetNodeFlags = pDocDat.getNodeFlags(targetStateNode);
    if (targetNodeFlags.contains(NodeFlag.ISVIOLATION)) {
      pViolationStates.add(targetStateId);
    }
    if (sourceNodeFlags.contains(NodeFlag.ISVIOLATION)) {
      pViolationStates.add(sourceStateId);
      logger.log(
          Level.WARNING,
          String.format(
              "Source %s of transition %s is a violation state. No outgoing edges expected.",
              sourceStateId, transitionToString(pTransition)));
    }
    if (targetNodeFlags.contains(NodeFlag.ISSINKNODE)) {
      pSinkStates.add(targetStateId);
    }
    if (sourceNodeFlags.contains(NodeFlag.ISSINKNODE)) {
      pSinkStates.add(sourceStateId);
      logger.log(
          Level.WARNING,
          String.format(
              "Source %s of transition %s is a sink state. No outgoing edges expected.",
              sourceStateId, transitionToString(pTransition)));
    }
  }

  /**
   * Determine the entry state of the automaton.
   *
   * @param pDocDat the GraphML-document-data helper.
   * @param pStates the states of the automaton represented as a list of GraphML nodes.
   * @throws WitnessParseException if not exactly one entry state is found.
   * @return the identifier of the unique entry state.
   */
  private static String getEntryState(GraphMLDocumentData pDocDat, NodeList pStates)
      throws WitnessParseException {
    List<String> entryStateIds = new ArrayList<>();
    for (Node node : asIterable(pStates)) {
      if (Boolean.parseBoolean(
          pDocDat.getDataValueWithDefault(node, KeyDef.ISENTRYNODE, "false"))) {
        entryStateIds.add(
            GraphMLDocumentData.getAttributeValue(node, "id", "Every state needs an id!"));
      }
    }

    if (entryStateIds.size() == 1) {
      return entryStateIds.get(0);
    } else {
      throw new WitnessParseException(
          "You must define exactly one entry state. Found entry states: " + entryStateIds);
    }
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

  private Set<PropertyType> getSpecAsProperties(final Node pAutomaton) {
    Set<String> specText = GraphMLDocumentData.getDataOnNode(pAutomaton, KeyDef.SPECIFICATION);
    if (specText.isEmpty()) {
      return Sets.newHashSet(PropertyType.REACHABILITY);
    } else {
      Set<PropertyType> properties = Sets.newHashSetWithExpectedSize(specText.size());
      for (String prop : specText) {
        try {
        properties.add(getProperty(prop));
        } catch (IllegalArgumentException | IndexOutOfBoundsException e) {
          logger.log(Level.WARNING, String.format("Cannot map specification %s to property type. Will ignore it.", prop));
        }
      }
      return properties;
    }
  }

  private PropertyType getProperty(final String pProperty) {
    String prop;
    if (pProperty.trim().startsWith("CHECK")) {
      prop = pProperty.substring(pProperty.indexOf(",") + 1, pProperty.lastIndexOf(")")).trim();
      if (prop.startsWith("LTL")) {
        prop = prop.substring(prop.indexOf("(") + 1, prop.lastIndexOf(")"));
      }
    } else {
      prop = pProperty;
    }

    for (PropertyType propType : PropertyType.values()) {
      if (propType.toString().equals(prop)) {
        return propType;
      }
    }

    return PropertyType.valueOf(prop.trim());
  }

  /**
   * Compute the distances from automaton states to violation states.
   *
   * <p>Violation states have a distance of {@code -1}, their predecessor states have distance
   * {@code 1}, and so on. The infinite distance of states with no path to a violation state is
   * represented by the value {@code -1}.
   *
   * @param pEnteringTransitions a map describing the witness automaton by providing a mapping from
   *     states to transitions entering those states.
   * @return a map from automaton-state identifiers to their distances to the next violation state.
   */
  private static Map<String, Integer> determineDistanceToViolation(
      Multimap<String, Node> pEnteringTransitions,
      Set<String> pViolationStates,
      Set<String> pSinkStates) {
    Queue<String> waitlist = new ArrayDeque<>(pViolationStates);
    Map<String, Integer> distances = Maps.newHashMap();
    for (String violationState : pViolationStates) {
      distances.put(violationState, 0);
    }
    while (!waitlist.isEmpty()) {
      String current = waitlist.poll();
      int newDistance = distances.get(current) + 1;
      for (Node enteringTransition : pEnteringTransitions.get(current)) {
        String sourceStateId =
            GraphMLDocumentData.getAttributeValue(
                enteringTransition, "source", "Every transition needs a source!");
        Integer oldDistance = distances.get(sourceStateId);
        if (oldDistance == null || oldDistance > newDistance) {
          distances.put(sourceStateId, newDistance);
          waitlist.offer(sourceStateId);
        }
      }
    }
    // Sink nodes have infinite distance to the target location, encoded as -1
    for (String sinkStateId : pSinkStates) {
      distances.put(sinkStateId, -1);
    }
    return distances;
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
            "Witness does not contain the SHA-1 hash value "
                + "of the program and may therefore be unrelated to the "
                + "verification task it is being validated against.";
      } else {
        message = "Witness does not contain the SHA-1 hash value of the program.";
      }
      if (strictChecking) {
        throw new WitnessParseException(message);
      } else {
        logger.log(Level.WARNING, message);
      }
    } else if (checkProgramHash) {
      Set<String> programHash =
          FluentIterable.from(pProgramHashes).transform(String::toLowerCase).toSet();
      for (Path programFile : cfa.getFileNames()) {
        String actualProgramHash = AutomatonGraphmlCommon.computeHash(programFile).toLowerCase();
        if (!programHash.contains(actualProgramHash)) {
          throw new WitnessParseException(
              "SHA-1 hash value of given verification-task "
                  + "source-code file ("
                  + actualProgramHash
                  + ") "
                  + "does not match the SHA-1 hash value in the witness. "
                  + "The witness is likely unrelated to the verification task.");
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

  private Optional<String> determineResultFunction(Set<String> pResultFunctions, Scope pScope)
      throws WitnessParseException {
    checkParsable(
        pResultFunctions.size() <= 1,
        "At most one result function must be provided for a transition.");
    if (!pResultFunctions.isEmpty()) {
      return Optional.of(pResultFunctions.iterator().next());
    }
    if (pScope instanceof CProgramScope) {
      CProgramScope scope = (CProgramScope) pScope;
      if (!scope.isGlobalScope()) {
        return Optional.of(scope.getCurrentFunctionName());
      }
    }
    return Optional.empty();
  }

  private Scope determineScope(Set<String> pScopes, Deque<String> pFunctionStack, Predicate<FileLocation> pLocationDescriptor)
      throws WitnessParseException {
    checkParsable(pScopes.size() <= 1, "At most one scope must be provided for a transition.");
    Scope result = this.scope;
    if (result instanceof CProgramScope) {
      result = ((CProgramScope) result).withLocationDescriptor(pLocationDescriptor);
      if (!pScopes.isEmpty() || !pFunctionStack.isEmpty()) {
        final String functionName;
        if (!pScopes.isEmpty()) {
          functionName = pScopes.iterator().next();
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
      String pTargetStateId,
      boolean pLeadsToViolationNode,
      boolean pSinkAsBottomNotBreak,
      Set<String> pSinkNodeIds) {
    if (pSinkNodeIds.contains(pTargetStateId)) {
      return createAutomatonSinkTransition(
          pTriggers, pAssertions, pActions, pLeadsToViolationNode, pSinkAsBottomNotBreak);
    }
    if (pLeadsToViolationNode) {
      List<AutomatonBoolExpr> assertions = ImmutableList.<AutomatonBoolExpr>builder().addAll(pAssertions).add(createViolationAssertion()).build();
      return new ViolationCopyingAutomatonTransition(
          pTriggers, assertions, pAssumptions, pCandidateInvariants, pActions, pTargetStateId);
    }
    return new AutomatonTransition(
        pTriggers, pAssertions, pAssumptions, pCandidateInvariants, pActions, pTargetStateId);
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
      List<String> violatedPropertyDescriptions = new ArrayList<>();

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

    private final HashMap<String, Optional<String>> defaultDataValues = Maps.newHashMap();
    private final Document doc;

    private Map<String, Element> idToNodeMap = null;

    public GraphMLDocumentData(Document doc) {
      this.doc = doc;
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

    public Map<String, Element> getIdToNodeMap() {
      if (idToNodeMap != null) {
        return idToNodeMap;
      }

      idToNodeMap = Maps.newHashMap();

      NodeList nodes = doc.getElementsByTagName(GraphMLTag.NODE.toString());
      for (Node stateNode : asIterable(nodes)) {
        String stateId = getNodeId(stateNode);
        idToNodeMap.put(stateId, (Element) stateNode);
      }

      return idToNodeMap;
    }

    private static String getAttributeValue(Node of, String attributeName, String exceptionMessage) {
      Node attribute = of.getAttributes().getNamedItem(attributeName);
      Preconditions.checkNotNull(attribute, exceptionMessage);
      return attribute.getTextContent();
    }

    private Optional<String> getDataDefault(KeyDef dataKey) throws WitnessParseException {
      Optional<String> result = defaultDataValues.get(dataKey.id);
      if (result != null) {
        return result;
      }

      NodeList keyDefs = doc.getElementsByTagName(GraphMLTag.KEY.toString());
      for (Node keyDef : asIterable(keyDefs)) {
        Node id = keyDef.getAttributes().getNamedItem("id");
        if (dataKey.id.equals(id.getTextContent())) {
          NodeList defaultTags =
              ((Element) keyDef).getElementsByTagName(GraphMLTag.DEFAULT.toString());
          result = Optional.empty();
          if (defaultTags.getLength() > 0) {
            checkParsable(
                defaultTags.getLength() == 1,
                "There should not be multiple default tags for one key.");
            result = Optional.of(defaultTags.item(0).getTextContent());
          }
          defaultDataValues.put(dataKey.id, result);
          return result;
        }
      }
      return Optional.empty();
    }

    private static String getNodeId(Node stateNode) {
      return getAttributeValue(stateNode, "id", "Every state needs an ID!");
    }

    private Element getNodeWithId(String nodeId) {
      return getIdToNodeMap().get(nodeId);
    }

    private String getDataValueWithDefault(
        Node dataOnNode, KeyDef dataKey, final String defaultValue) throws WitnessParseException {
      Set<String> values = getDataOnNode(dataOnNode, dataKey);
      if (values.size() == 0) {
        Optional<String> dataDefault = getDataDefault(dataKey);
        if (dataDefault.isPresent()) {
          return dataDefault.get();
        } else {
          return defaultValue;
        }
      } else {
        return values.iterator().next();
      }
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
    doc.getDocumentElement().normalize();

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
    String message = ACCESS_ERROR_MESSAGE;
    String infix = pException.getMessage();
    return String.format(message, infix);
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
