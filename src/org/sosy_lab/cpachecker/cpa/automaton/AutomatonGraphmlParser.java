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

import static org.sosy_lab.cpachecker.util.automaton.AutomatonGraphmlCommon.SINK_NODE_ID;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.Writer;
import java.util.ArrayDeque;
import java.util.Collection;
import java.util.Collections;
import java.util.Deque;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Queue;
import java.util.Set;
import java.util.logging.Level;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.parsers.SAXParserFactory;

import org.sosy_lab.common.configuration.Configuration;
import org.sosy_lab.common.configuration.FileOption;
import org.sosy_lab.common.configuration.InvalidConfigurationException;
import org.sosy_lab.common.configuration.Option;
import org.sosy_lab.common.configuration.Options;
import org.sosy_lab.common.io.Files;
import org.sosy_lab.common.io.Path;
import org.sosy_lab.common.log.LogManager;
import org.sosy_lab.cpachecker.cfa.CParser;
import org.sosy_lab.cpachecker.cfa.CProgramScope;
import org.sosy_lab.cpachecker.cfa.ast.c.CCastExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CExpressionAssignmentStatement;
import org.sosy_lab.cpachecker.cfa.ast.c.CLeftHandSide;
import org.sosy_lab.cpachecker.cfa.ast.c.CStatement;
import org.sosy_lab.cpachecker.cfa.parser.Scope;
import org.sosy_lab.cpachecker.cfa.types.MachineModel;
import org.sosy_lab.cpachecker.cfa.types.c.CBasicType;
import org.sosy_lab.cpachecker.cfa.types.c.CSimpleType;
import org.sosy_lab.cpachecker.cfa.types.c.CType;
import org.sosy_lab.cpachecker.exceptions.CParserException;
import org.sosy_lab.cpachecker.util.SourceLocationMapper.OriginDescriptor;
import org.sosy_lab.cpachecker.util.automaton.AutomatonGraphmlCommon.GraphMlTag;
import org.sosy_lab.cpachecker.util.automaton.AutomatonGraphmlCommon.KeyDef;
import org.sosy_lab.cpachecker.util.automaton.AutomatonGraphmlCommon.NodeFlag;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;
import org.xml.sax.helpers.DefaultHandler;

import com.google.common.base.Function;
import com.google.common.base.Optional;
import com.google.common.base.Preconditions;
import com.google.common.base.Splitter;
import com.google.common.collect.FluentIterable;
import com.google.common.collect.HashMultimap;
import com.google.common.collect.Iterables;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.google.common.collect.Multimap;
import com.google.common.collect.Sets;

@Options(prefix="spec")
public class AutomatonGraphmlParser {

  @Option(secure=true, description="Consider the negative semantics of tokens provided with path automatons.")
  private boolean considerNegativeSemanticsAttribute = false; // legacy: token matching needs this

  @Option(secure=true, description="Consider assumptions that are provided with the path automaton?")
  private boolean considerAssumptions = true;

  @Option(secure=true, description="Legacy option for token-based matching with path automatons.")
  private boolean transitionToStopForNegatedTokensetMatch = false; // legacy: tokenmatching

  @Option(secure=true, description="Match the source code provided with the witness.")
  private boolean matchSourcecodeData = false;

  @Option(secure=true, description="Match the line numbers within the origin (mapping done by preprocessor line markers).")
  private boolean matchOriginLine = true;

  @Option(secure=true, description="Do not try to \"catch up\" with witness lines: If they do not match, go to the sink.")
  private boolean strictLineMatching = false;

  @Option(secure=true, description="If a witness represents a single path in Automaton match both starting and ending lines of the CFA edge (ending line = starting line of the next edge). If matching CFA edge exists, go to the sink for the other edges, otherwise match starting lines as usual.")
  private boolean singlePathMatching = false;

  @Option(secure=true, description="File for exporting the path automaton in DOT format.")
  @FileOption(FileOption.Type.OUTPUT_FILE)
  private Path automatonDumpFile = null;

  private Scope scope;
  private LogManager logger;
  private Configuration config;
  private MachineModel machine;

  public AutomatonGraphmlParser(Configuration pConfig, LogManager pLogger, MachineModel pMachine, Scope pScope) throws InvalidConfigurationException {
    pConfig.inject(this);

    this.scope = pScope;
    this.machine = pMachine;
    this.logger = pLogger;
    this.config = pConfig;
  }

  /**
  * Parses a Specification File and returns the Automata found in the file.
   * @throws CParserException
  */
  public List<Automaton> parseAutomatonFile(Path pInputFile) throws InvalidConfigurationException {
    CParser cparser = CParser.Factory.getParser(config, logger, CParser.Factory.getOptions(config), machine);
    try (InputStream input = pInputFile.asByteSource().openStream()) {
      // Parse the XML document ----
      DocumentBuilderFactory docFactory = DocumentBuilderFactory.newInstance();
      DocumentBuilder docBuilder = docFactory.newDocumentBuilder();
      Document doc = docBuilder.parse(input);
      doc.getDocumentElement().normalize();

      GraphMlDocumentData docDat = new GraphMlDocumentData(doc);

      // (The one) root node of the graph ----
      NodeList graphs = doc.getElementsByTagName(GraphMlTag.GRAPH.toString());
      Preconditions.checkArgument(graphs.getLength() == 1, "The graph file must describe exactly one automaton.");
      Node graphNode = graphs.item(0);

      // Extract the information on the automaton ----
      Node nameAttribute = graphNode.getAttributes().getNamedItem("name");
      String automatonName = nameAttribute == null ? "WitnessAutomaton" : nameAttribute.getTextContent();
      String initialStateName = null;

      // Create transitions ----
      //AutomatonBoolExpr epsilonTrigger = new SubsetMatchEdgeTokens(Collections.<Comparable<Integer>>emptySet());
      NodeList edges = doc.getElementsByTagName(GraphMlTag.EDGE.toString());
      NodeList nodes = doc.getElementsByTagName(GraphMlTag.NODE.toString());
      Map<String, LinkedList<AutomatonTransition>> stateTransitions = Maps.newHashMap();
      Map<String, Deque<String>> stacks = Maps.newHashMap();

      // Create graph
      Multimap<String, Node> graph = HashMultimap.create();
      String entryNodeId = null;
      for (int i = 0; i < edges.getLength(); i++) {
        Node stateTransitionEdge = edges.item(i);

        String sourceStateId = GraphMlDocumentData.getAttributeValue(stateTransitionEdge, "source", "Every transition needs a source!");
        graph.put(sourceStateId, stateTransitionEdge);
      }

      // Find entry
      for (int i = 0; i < nodes.getLength(); ++i) {
        Node node = nodes.item(i);
        if (Boolean.parseBoolean(docDat.getDataValueWithDefault(node, KeyDef.ISENTRYNODE, "false"))) {
          entryNodeId = GraphMlDocumentData.getAttributeValue(node, "id", "Every node needs an id!");
          break;
        }
      }

      Preconditions.checkNotNull(entryNodeId, "You must define an entry node.");

      Set<Node> visitedEdges = new HashSet<>();
      Queue<Node> waitingEdges = new ArrayDeque<>();
      waitingEdges.addAll(graph.get(entryNodeId));
      visitedEdges.addAll(waitingEdges);
      while (!waitingEdges.isEmpty()) {
        Node stateTransitionEdge = waitingEdges.poll();

        String sourceStateId = GraphMlDocumentData.getAttributeValue(stateTransitionEdge, "source", "Every transition needs a source!");
        String targetStateId = GraphMlDocumentData.getAttributeValue(stateTransitionEdge, "target", "Every transition needs a target!");

        for (Node successorEdge : graph.get(targetStateId)) {
          if (visitedEdges.add(successorEdge)) {
            waitingEdges.add(successorEdge);
          }
        }

        Element targetStateNode = docDat.getNodeWithId(targetStateId);
        EnumSet<NodeFlag> targetNodeFlags = docDat.getNodeFlags(targetStateNode);

        List<AutomatonBoolExpr> emptyAssertions = Collections.emptyList();
        List<AutomatonAction> actions = Collections.emptyList();
        List<CStatement> assumptions = Lists.newArrayList();

        LinkedList<AutomatonTransition> transitions = stateTransitions.get(sourceStateId);
        if (transitions == null) {
          transitions = Lists.newLinkedList();
          stateTransitions.put(sourceStateId, transitions);
        }

        // Handle stack
        Deque<String> currentStack = stacks.get(sourceStateId);
        if (currentStack == null) {
          currentStack = new ArrayDeque<>();
          stacks.put(sourceStateId, currentStack);
        }
        Deque<String> newStack = currentStack;
        Set<String> functionEntries = GraphMlDocumentData.getDataOnNode(stateTransitionEdge, KeyDef.FUNCTIONENTRY);
        if (!functionEntries.isEmpty()) {
          newStack = new ArrayDeque<>(newStack);
          newStack.push(Iterables.getOnlyElement(functionEntries));
        }
        Set<String> functionExits = GraphMlDocumentData.getDataOnNode(stateTransitionEdge, KeyDef.FUNCTIONEXIT);
        if (!functionExits.isEmpty()) {
          String function = Iterables.getOnlyElement(functionExits);
          if (newStack.isEmpty()) {
            logger.log(Level.WARNING, "Trying to return from function", function, "although no function is on the stack.");
          } else {
            newStack = new ArrayDeque<>(newStack);
            String oldFunction = newStack.pop();
            assert oldFunction.equals(function);
          }
        }
        stacks.put(targetStateId, newStack);

        AutomatonBoolExpr conjunctedTriggers = AutomatonBoolExpr.TRUE;

        if (considerAssumptions) {
          Set<String> transAssumes = GraphMlDocumentData.getDataOnNode(stateTransitionEdge, KeyDef.ASSUMPTION);
          Scope scope = this.scope;
          if (!newStack.isEmpty() && scope instanceof CProgramScope) {
            scope = ((CProgramScope) scope).createFunctionScope(newStack.peek());
          }
          for (String assumeCode : transAssumes) {
            assumptions.addAll(removeDuplicates(adjustCharAssignments(
                AutomatonASTComparator.generateSourceASTOfBlock(
                    tryFixArrayInitializers(assumeCode),
                    cparser,
                    scope))));
          }
        }

        if (matchOriginLine) {
          Set<String> originFileTags = GraphMlDocumentData.getDataOnNode(stateTransitionEdge, KeyDef.ORIGINFILE);
          Preconditions.checkArgument(originFileTags.size() < 2, "At most one origin-file data tag must be provided for an edge!");

          Set<String> originLineTags = GraphMlDocumentData.getDataOnNode(stateTransitionEdge, KeyDef.ORIGINLINE);
          Preconditions.checkArgument(originLineTags.size() <  2, "At most one origin-line data tag must be provided for each edge!");

          int matchOriginLineNumber = -1;
          if (originLineTags.size() > 0) {
            matchOriginLineNumber = Integer.parseInt(originLineTags.iterator().next());
          }
          if (matchOriginLineNumber > 0) {
            Optional<String> matchOriginFileName = originFileTags.isEmpty() ? Optional.<String>absent() : Optional.of(originFileTags.iterator().next());
            OriginDescriptor originDescriptor = new OriginDescriptor(matchOriginFileName, matchOriginLineNumber);

            AutomatonBoolExpr startingLineMatchingExpr = new AutomatonBoolExpr.And(conjunctedTriggers,
                new AutomatonBoolExpr.MatchStartingLineInOrigin(originDescriptor, true));

            if (targetStateId.equalsIgnoreCase(SINK_NODE_ID) || targetNodeFlags.contains(NodeFlag.ISSINKNODE)) {
              // Transition to the BOTTOM state
              AutomatonBoolExpr trigger = new AutomatonBoolExpr.And(
                  AutomatonBoolExpr.MatchPathRelevantEdgesBoolExpr.INSTANCE,
                  startingLineMatchingExpr);
              transitions.add(new AutomatonTransition(
                  trigger,
                  emptyAssertions, assumptions, actions, AutomatonInternalState.BOTTOM, null));
            } else {
              // Generate special conditions for single path error trace
              Collection<Node> siblings = graph.get(sourceStateId);
              Collection<Node> children = graph.get(targetStateId);
              assert siblings != null;
              if (singlePathMatching
                  && siblings.size() <= 1
                  && children != null
                  && children.size() == 1) {
                Node targetTransitionEdge = children.iterator().next();

                Set<String> targetOriginFileTags = GraphMlDocumentData.getDataOnNode(targetTransitionEdge, KeyDef.ORIGINFILE);
                Preconditions.checkArgument(targetOriginFileTags.size() < 2, "At most one origin-file data tag must be provided for an edge!");

                Set<String> targetOriginLineTags = GraphMlDocumentData.getDataOnNode(targetTransitionEdge, KeyDef.ORIGINLINE);

                if (!targetOriginLineTags.isEmpty()) {
                  Preconditions.checkArgument(targetOriginLineTags.size() <  2, "At most one origin-line data tag must be provided for each edge!");

                  Optional<String> matchTargetOriginFileName = targetOriginFileTags.isEmpty() ? Optional.<String>absent() : Optional.of(targetOriginFileTags.iterator().next());
                  int matchTargetOriginLineNumber = Integer.parseInt(targetOriginLineTags.iterator().next());

                  OriginDescriptor targetOriginDescriptor = new OriginDescriptor(matchTargetOriginFileName, matchTargetOriginLineNumber);

                  AutomatonBoolExpr matchEdgeTriggers = new AutomatonBoolExpr.And(conjunctedTriggers,
                      new AutomatonBoolExpr.MatchEdgeLinesInOrigin(originDescriptor, targetOriginDescriptor, true));

                  AutomatonTransition tr = new AutomatonTransition(matchEdgeTriggers, emptyAssertions, assumptions, actions, targetStateId);
                  transitions.add(0, tr);

                  AutomatonBoolExpr existsMatchEdgeTriggers = new AutomatonBoolExpr.And(
                      new AutomatonBoolExpr.Negation(matchEdgeTriggers),
                      new AutomatonBoolExpr.ExistsMatchingEdgeLinesInOrigin(originDescriptor, targetOriginDescriptor, true));

                  AutomatonTransition trSink = new AutomatonTransition(
                      existsMatchEdgeTriggers,
                      emptyAssertions,
                      Collections.<AutomatonAction>emptyList(),
                      AutomatonInternalState.BOTTOM);
                  transitions.add(trSink);

                  conjunctedTriggers = new AutomatonBoolExpr.And(
                      new AutomatonBoolExpr.Negation(existsMatchEdgeTriggers),
                      startingLineMatchingExpr);
                } else {
                  conjunctedTriggers = startingLineMatchingExpr;
                }
              } else {
                conjunctedTriggers = startingLineMatchingExpr;
              }
              AutomatonBoolExpr relevantLineMatchTrigger = new AutomatonBoolExpr.And(
                  AutomatonBoolExpr.MatchPathRelevantEdgesBoolExpr.INSTANCE, conjunctedTriggers);

              AutomatonTransition relevantLineMatchTransition =
                  new AutomatonTransition(
                      relevantLineMatchTrigger,
                      emptyAssertions,
                      assumptions,
                      actions,
                      targetStateId);
              transitions.add(0, relevantLineMatchTransition);

              /*
               * If there are non-path-relevant edges in the automaton, they
               * can be accepted if no assumptions are attached.
               */
              AutomatonBoolExpr irrelevantLineMatchTrigger = AutomatonBoolExpr.FALSE;
              if (!matchSourcecodeData && assumptions.isEmpty()) {
                irrelevantLineMatchTrigger = new AutomatonBoolExpr.And(
                      new AutomatonBoolExpr.Negation(AutomatonBoolExpr.MatchPathRelevantEdgesBoolExpr.INSTANCE),
                      conjunctedTriggers
                    );
                AutomatonTransition irrelevantLineMatchTransition =
                    new AutomatonTransition(
                        irrelevantLineMatchTrigger,
                        emptyAssertions,
                        assumptions,
                        actions,
                        targetStateId);
                transitions.add(irrelevantLineMatchTransition);
              }

              AutomatonBoolExpr elseTrigger = new AutomatonBoolExpr.And(
                  new AutomatonBoolExpr.Negation(relevantLineMatchTrigger),
                  new AutomatonBoolExpr.Negation(irrelevantLineMatchTrigger)
                  );
              final AutomatonTransition elseTransition;
              if (strictLineMatching) {
                // If both do not apply, go to the sink
                elseTransition = new AutomatonTransition(
                    elseTrigger,
                    emptyAssertions,
                    Collections.<AutomatonAction>emptyList(),
                    AutomatonInternalState.BOTTOM);
              } else {
                // If both do not apply, loop back to the source state
                elseTransition = new AutomatonTransition(
                    elseTrigger,
                    emptyAssertions,
                    Collections.<AutomatonAction>emptyList(),
                    sourceStateId);
              }
              transitions.add(elseTransition);
            }
          } else {
            AutomatonTransition tr = new AutomatonTransition(
                new AutomatonBoolExpr.Negation(
                    AutomatonBoolExpr.MatchPathRelevantEdgesBoolExpr.INSTANCE),
                      emptyAssertions, assumptions, actions, targetStateId);
            transitions.add(0, tr);
          }

        }
        if (matchSourcecodeData) {
          Set<String> sourceCodeDataTags = GraphMlDocumentData.getDataOnNode(stateTransitionEdge, KeyDef.SOURCECODE);
          Preconditions.checkArgument(sourceCodeDataTags.size() < 2, "At most one source-code data tag must be provided!");
          final String sourceCode;
          if (sourceCodeDataTags.isEmpty()) {
            sourceCode = "";
          } else {
            sourceCode = sourceCodeDataTags.iterator().next();
          }
          final AutomatonBoolExpr exactEdgeMatch = new AutomatonBoolExpr.MatchCFAEdgeExact(sourceCode);
          conjunctedTriggers = new AutomatonBoolExpr.And(conjunctedTriggers, exactEdgeMatch);

          if (targetStateId.equalsIgnoreCase(SINK_NODE_ID) || targetNodeFlags.contains(NodeFlag.ISSINKNODE)) {
            // Transition to the BOTTOM state
            transitions.add(new AutomatonTransition(conjunctedTriggers, emptyAssertions, actions, AutomatonInternalState.BOTTOM));
          } else {
            // Transition to the next state
            transitions.add(new AutomatonTransition(conjunctedTriggers, emptyAssertions, actions, targetStateId));
            transitions.add(new AutomatonTransition(new AutomatonBoolExpr.Negation(conjunctedTriggers), emptyAssertions, actions, AutomatonInternalState.BOTTOM));

            // If CPAchecker has more than one edge for the same piece of source code, allow the automaton to wait them out
            LinkedList<AutomatonTransition> followStateTransitions = stateTransitions.get(targetStateId);
            if (followStateTransitions == null) {
              followStateTransitions = Lists.newLinkedList();
              stateTransitions.put(targetStateId, followStateTransitions);
            }
            followStateTransitions.add(new AutomatonTransition(conjunctedTriggers, emptyAssertions, actions, targetStateId));
          }
        } else {
          if (considerNegativeSemanticsAttribute) {
            final Optional<Boolean> matchPositiveCase;
            switch (docDat.getDataValueWithDefault(stateTransitionEdge, KeyDef.TOKENSNEGATED, "").toLowerCase()) {
              case "true":
                matchPositiveCase = Optional.of(false);
                break;
              case "false":
                matchPositiveCase = Optional.of(true);
                break;
              default:
                matchPositiveCase = Optional.absent();
            }
            conjunctedTriggers = new AutomatonBoolExpr.And(conjunctedTriggers, new AutomatonBoolExpr.MatchAssumeCase(matchPositiveCase));
          }
        }
      }

      // Create states ----
      List<AutomatonInternalState> automatonStates = Lists.newArrayList();
      for (String stateId : docDat.getIdToNodeMap().keySet()) {
        Element stateNode = docDat.getIdToNodeMap().get(stateId);
        EnumSet<NodeFlag> nodeFlags = docDat.getNodeFlags(stateNode);

        List<AutomatonTransition> transitions = stateTransitions.get(stateId);
        if (transitions == null) {
          transitions = Collections.emptyList();
        }

        if (nodeFlags.contains(NodeFlag.ISENTRY)) {
          Preconditions.checkArgument(initialStateName == null, "Only one entrynode is supported!");
          initialStateName = stateId;
        }

        // Determine if "matchAll" should be enabled
        boolean matchAll = true;

        AutomatonInternalState state = new AutomatonInternalState(stateId, transitions, false, matchAll);
        automatonStates.add(state);
      }

      // Build and return the result ----
      Preconditions.checkNotNull(initialStateName, "Every graph needs a specified entry node!");
      Map<String, AutomatonVariable> automatonVariables = Collections.emptyMap();
      List<Automaton> result = Lists.newArrayList();
      Automaton automaton = new Automaton(automatonName, automatonVariables, automatonStates, initialStateName);
      result.add(automaton);

      if (automatonDumpFile != null) {
        try (Writer w = Files.openOutputFile(automatonDumpFile)) {
          automaton.writeDotFile(w);
        } catch (IOException e) {
         // logger.logUserException(Level.WARNING, e, "Could not write the automaton to DOT file");
        }
      }

      return result;

    } catch (FileNotFoundException e) {
      throw new InvalidConfigurationException("Invalid automaton file provided! File not found!: " + pInputFile.getPath());
    } catch (IOException | ParserConfigurationException | SAXException e) {
      throw new InvalidConfigurationException("Error while accessing automaton file!", e);
    } catch (InvalidAutomatonException e) {
      throw new InvalidConfigurationException("The automaton provided is invalid!", e);
    } catch (CParserException e) {
      throw new InvalidConfigurationException("The automaton contains invalid C code!", e);
    }
  }

  /**
   * Some tools put assumptions for multiple statements on the same edge, which
   * may lead to contradictions between the assumptions.
   *
   * This is clearly a tool error, but for the competition we want to help them
   * out and only use the last assumption.
   *
   * @param pStatements the assumptions.
   *
   * @return the duplicate-free assumptions.
   */
  private static Collection<CStatement> removeDuplicates(Iterable<? extends CStatement> pStatements) {
    Map<Object, CStatement> result = new HashMap<>();
    for (CStatement statement : pStatements) {
      if (statement instanceof CExpressionAssignmentStatement) {
        CExpressionAssignmentStatement assignmentStatement = (CExpressionAssignmentStatement) statement;
        result.put(assignmentStatement.getLeftHandSide(), assignmentStatement);
      } else {
        result.put(statement, statement);
      }
    }
    return result.values();
  }

  /**
   * Be nice to tools that assume that default char (when it is neither
   * specified as signed nor as unsigned) may be unsigned.
   *
   * @param pStatements the assignment statements.
   *
   * @return the adjusted statements.
   */
  private static Collection<CStatement> adjustCharAssignments(Iterable<? extends CStatement> pStatements) {
    return FluentIterable.from(pStatements).transform(new Function<CStatement, CStatement>() {

      @Override
      public CStatement apply(CStatement pStatement) {
        if (pStatement instanceof CExpressionAssignmentStatement) {
          CExpressionAssignmentStatement statement = (CExpressionAssignmentStatement) pStatement;
          CLeftHandSide leftHandSide = statement.getLeftHandSide();
          CType canonicalType = leftHandSide.getExpressionType().getCanonicalType();
          if (canonicalType instanceof CSimpleType) {
            CSimpleType simpleType = (CSimpleType) canonicalType;
            CBasicType basicType = simpleType.getType();
            if (basicType.equals(CBasicType.CHAR) && !simpleType.isSigned() && !simpleType.isUnsigned()) {
              CExpression rightHandSide = statement.getRightHandSide();
              CExpression castedRightHandSide = new CCastExpression(rightHandSide.getFileLocation(), canonicalType, rightHandSide);
              return new CExpressionAssignmentStatement(statement.getFileLocation(), leftHandSide, castedRightHandSide);
            }
          }
        }
        return pStatement;
      }

    }).toList();
  }

  /**
   * Let's be nice to tools that ignore the restriction that array initializers
   * are not allowed as right-hand sides of assignment statements and try to
   * help them. This is a hack, no good solution.
   * We would need a kind-of-but-not-really-C-parser to properly handle these
   * declarations-that-aren't-declarations.
   *
   * @param pAssumeCode the code from the witness assumption.
   *
   * @return the code from the witness assumption if no supported array
   * initializer is contained; otherwise the fixed code.
   */
  private static String tryFixArrayInitializers(String pAssumeCode) {
    String C_INTEGER = "([\\+\\-])?(0[xX])?[0-9a-fA-F]+";
    String assumeCode = pAssumeCode.trim();
    if (assumeCode.endsWith(";")) {
      assumeCode = assumeCode.substring(0, assumeCode.length() - 1);
    }
    /*
     * This only covers the special case of one assignment statement using one
     * array of integers.
     */
    if (assumeCode.matches(".+=\\s*\\{\\s*(" + C_INTEGER + "\\s*(,\\s*" + C_INTEGER + "\\s*)*)?\\}\\s*")) {
      Iterable<String> assignmentParts = Splitter.on('=').trimResults().split(assumeCode);
      Iterator<String> assignmentPartIterator = assignmentParts.iterator();
      if (!assignmentPartIterator.hasNext()) {
        return pAssumeCode;
      }
      String leftHandSide = assignmentPartIterator.next();
      if (!assignmentPartIterator.hasNext()) {
        return pAssumeCode;
      }
      String rightHandSide = assignmentPartIterator.next().trim();
      if (assignmentPartIterator.hasNext()) {
        return pAssumeCode;
      }
      assert rightHandSide.startsWith("{") && rightHandSide.endsWith("}");
      rightHandSide = rightHandSide.substring(1, rightHandSide.length() - 1).trim();
      Iterable<String> elements = Splitter.on(',').trimResults().split(rightHandSide);
      StringBuilder resultBuilder = new StringBuilder();
      int index = 0;
      for (String element : elements) {
        resultBuilder.append(String.format("%s[%d] = %s; ", leftHandSide, index, element));
        ++index;
      }
      return resultBuilder.toString();
    }
    return pAssumeCode;
  }

  private static class GraphMlDocumentData {

    private final HashMap<String, Optional<String>> defaultDataValues = Maps.newHashMap();
    private final Document doc;

    private Map<String, Element> idToNodeMap = null;

    public GraphMlDocumentData(Document doc) {
      this.doc = doc;
    }

    public EnumSet<NodeFlag> getNodeFlags(Element pStateNode) {
      EnumSet<NodeFlag> result = EnumSet.noneOf(NodeFlag.class);

      NodeList dataChilds = pStateNode.getElementsByTagName(GraphMlTag.DATA.toString());

      for (int i=0; i<dataChilds.getLength(); i++) {
        Node dataChild = dataChilds.item(i);
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

      NodeList nodes = doc.getElementsByTagName(GraphMlTag.NODE.toString());
      for (int i=0; i<nodes.getLength(); i++) {
        Element stateNode = (Element) nodes.item(i);
        String stateId = getNodeId(stateNode);

        idToNodeMap.put(stateId, stateNode);
      }

      return idToNodeMap;
    }

    private static String getAttributeValue(Node of, String attributeName, String exceptionMessage) {
      Node attribute = of.getAttributes().getNamedItem(attributeName);
      Preconditions.checkNotNull(attribute, exceptionMessage);
      return attribute.getTextContent();
    }

    private Optional<String> getDataDefault(KeyDef dataKey) {
      Optional<String> result = defaultDataValues.get(dataKey.id);
      if (result != null) {
        return result;
      }

      NodeList keyDefs = doc.getElementsByTagName(GraphMlTag.KEY.toString());
      for (int i=0; i<keyDefs.getLength(); i++) {
        Element keyDef = (Element) keyDefs.item(i);
        Node id = keyDef.getAttributes().getNamedItem("id");
        if (dataKey.id.equals(id.getTextContent())) {
          NodeList defaultTags = keyDef.getElementsByTagName(GraphMlTag.DEFAULT.toString());
          result = Optional.absent();
          if (defaultTags.getLength() > 0) {
            Preconditions.checkArgument(defaultTags.getLength() == 1);
            result = Optional.of(defaultTags.item(0).getTextContent());
          }
          defaultDataValues.put(dataKey.id, result);
          return result;
        }
      }
      return Optional.absent();
    }

    private static String getNodeId(Node stateNode) {
      return getAttributeValue(stateNode, "id", "Every state needs an ID!");
    }

    private Element getNodeWithId(String nodeId) {
      Element result = getIdToNodeMap().get(nodeId);
      Preconditions.checkNotNull(result, "Node not found. Id: " + nodeId);
      return result;
    }

    private String getDataValueWithDefault(Node dataOnNode, KeyDef dataKey, final String defaultValue) {
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
      NodeList dataChilds = of.getElementsByTagName(GraphMlTag.DATA.toString());
      for (int i=0; i<dataChilds.getLength(); i++) {
        Node dataChild = dataChilds.item(i);
        Node attribute = dataChild.getAttributes().getNamedItem("key");
        Preconditions.checkNotNull(attribute, "Every data element must have a key attribute!");
        if (attribute.getTextContent().equals(dataKey.id)) {
          result.add(dataChild);
        }
      }
      return result;
    }

  }

  public static boolean isGraphmlAutomaton(Path pPath, LogManager pLogger) throws InvalidConfigurationException {
    try (InputStream input = pPath.asByteSource().openStream()) {
      SAXParserFactory.newInstance().newSAXParser().parse(input, new DefaultHandler());
      return true;
    } catch (FileNotFoundException e) {
      throw new InvalidConfigurationException("Invalid automaton file provided! File not found: " + pPath.getPath());
    } catch (IOException e) {
      throw new InvalidConfigurationException("Error while accessing automaton file", e);
    } catch (SAXException e) {
      return false;
    } catch (ParserConfigurationException e) {
      pLogger.logException(Level.WARNING, e, "SAX parser configured incorrectly. Could not determine whether or not the path describes a graphml automaton.");
      return false;
    }
  }

}
