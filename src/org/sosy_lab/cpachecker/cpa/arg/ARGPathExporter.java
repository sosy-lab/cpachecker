/*
 *  CPAchecker is a tool for configurable software verification.
 *  This file is part of CPAchecker.
 *
 *  Copyright (C) 2007-2016  Dirk Beyer
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
package org.sosy_lab.cpachecker.cpa.arg;

import static org.sosy_lab.cpachecker.util.automaton.AutomatonGraphmlCommon.SINK_NODE_ID;

import com.google.common.base.Charsets;
import com.google.common.base.Function;
import com.google.common.base.Joiner;
import com.google.common.base.Preconditions;
import com.google.common.base.Predicate;
import com.google.common.base.Predicates;
import com.google.common.base.Strings;
import com.google.common.base.Supplier;
import com.google.common.collect.Collections2;
import com.google.common.collect.FluentIterable;
import com.google.common.collect.HashMultimap;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableList.Builder;
import com.google.common.collect.Iterables;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.google.common.collect.Multimap;
import com.google.common.collect.Queues;
import com.google.common.collect.Sets;
import com.google.common.collect.TreeMultimap;
import java.io.IOException;
import java.nio.file.Path;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Deque;
import java.util.EnumSet;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Objects;
import java.util.Optional;
import java.util.Queue;
import java.util.Set;
import java.util.logging.Level;
import javax.annotation.Nullable;
import javax.xml.parsers.ParserConfigurationException;
import org.sosy_lab.common.configuration.Configuration;
import org.sosy_lab.common.configuration.FileOption;
import org.sosy_lab.common.configuration.InvalidConfigurationException;
import org.sosy_lab.common.configuration.Option;
import org.sosy_lab.common.configuration.Options;
import org.sosy_lab.common.io.MoreFiles;
import org.sosy_lab.common.log.LogManager;
import org.sosy_lab.cpachecker.cfa.CFA;
import org.sosy_lab.cpachecker.cfa.Language;
import org.sosy_lab.cpachecker.cfa.ast.AExpression;
import org.sosy_lab.cpachecker.cfa.ast.AExpressionStatement;
import org.sosy_lab.cpachecker.cfa.ast.AFunctionCallAssignmentStatement;
import org.sosy_lab.cpachecker.cfa.ast.AIdExpression;
import org.sosy_lab.cpachecker.cfa.ast.FileLocation;
import org.sosy_lab.cpachecker.cfa.ast.c.CAddressOfLabelExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CArraySubscriptExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CBinaryExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CCastExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CCharLiteralExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CComplexCastExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CExpressionStatement;
import org.sosy_lab.cpachecker.cfa.ast.c.CExpressionVisitor;
import org.sosy_lab.cpachecker.cfa.ast.c.CFieldReference;
import org.sosy_lab.cpachecker.cfa.ast.c.CFloatLiteralExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CIdExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CImaginaryLiteralExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CIntegerLiteralExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CParameterDeclaration;
import org.sosy_lab.cpachecker.cfa.ast.c.CPointerExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CSimpleDeclaration;
import org.sosy_lab.cpachecker.cfa.ast.c.CStringLiteralExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CTypeIdExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CUnaryExpression;
import org.sosy_lab.cpachecker.cfa.model.AStatementEdge;
import org.sosy_lab.cpachecker.cfa.model.AssumeEdge;
import org.sosy_lab.cpachecker.cfa.model.BlankEdge;
import org.sosy_lab.cpachecker.cfa.model.CFAEdge;
import org.sosy_lab.cpachecker.cfa.model.CFANode;
import org.sosy_lab.cpachecker.cfa.model.FunctionCallEdge;
import org.sosy_lab.cpachecker.cfa.model.FunctionEntryNode;
import org.sosy_lab.cpachecker.cfa.model.FunctionExitNode;
import org.sosy_lab.cpachecker.cfa.postprocessing.global.CFACloner;
import org.sosy_lab.cpachecker.cfa.types.MachineModel;
import org.sosy_lab.cpachecker.core.counterexample.AssumptionToEdgeAllocator;
import org.sosy_lab.cpachecker.core.counterexample.CExpressionToOrinalCodeVisitor;
import org.sosy_lab.cpachecker.core.counterexample.CFAEdgeWithAssumptions;
import org.sosy_lab.cpachecker.core.counterexample.ConcreteState;
import org.sosy_lab.cpachecker.core.counterexample.CounterexampleInfo;
import org.sosy_lab.cpachecker.core.interfaces.ExpressionTreeReportingState;
import org.sosy_lab.cpachecker.core.interfaces.Property;
import org.sosy_lab.cpachecker.cpa.arg.graphExport.Edge;
import org.sosy_lab.cpachecker.cpa.arg.graphExport.TransitionCondition;
import org.sosy_lab.cpachecker.cpa.threading.ThreadingState;
import org.sosy_lab.cpachecker.cpa.value.ValueAnalysisState;
import org.sosy_lab.cpachecker.cpa.value.refiner.ValueAnalysisConcreteErrorPathAllocator;
import org.sosy_lab.cpachecker.util.AbstractStates;
import org.sosy_lab.cpachecker.util.CFATraversal;
import org.sosy_lab.cpachecker.util.CFATraversal.CFAVisitor;
import org.sosy_lab.cpachecker.util.CFATraversal.TraversalProcess;
import org.sosy_lab.cpachecker.util.CFAUtils;
import org.sosy_lab.cpachecker.util.Pair;
import org.sosy_lab.cpachecker.util.automaton.AutomatonGraphmlCommon;
import org.sosy_lab.cpachecker.util.automaton.AutomatonGraphmlCommon.AssumeCase;
import org.sosy_lab.cpachecker.util.automaton.AutomatonGraphmlCommon.ElementType;
import org.sosy_lab.cpachecker.util.automaton.AutomatonGraphmlCommon.GraphMlBuilder;
import org.sosy_lab.cpachecker.util.automaton.AutomatonGraphmlCommon.GraphType;
import org.sosy_lab.cpachecker.util.automaton.AutomatonGraphmlCommon.KeyDef;
import org.sosy_lab.cpachecker.util.automaton.AutomatonGraphmlCommon.NodeFlag;
import org.sosy_lab.cpachecker.util.automaton.AutomatonGraphmlCommon.NodeType;
import org.sosy_lab.cpachecker.util.expressions.ExpressionTree;
import org.sosy_lab.cpachecker.util.expressions.ExpressionTreeFactory;
import org.sosy_lab.cpachecker.util.expressions.ExpressionTrees;
import org.sosy_lab.cpachecker.util.expressions.LeafExpression;
import org.sosy_lab.cpachecker.util.expressions.Or;
import org.sosy_lab.cpachecker.util.expressions.Simplifier;
import org.w3c.dom.Element;

@Options(prefix = "cpa.arg.witness")
public class ARGPathExporter {

  private static final Function<ARGState, ARGState> COVERED_TO_COVERING = new Function<ARGState, ARGState>() {

    @Override
    public ARGState apply(ARGState pChild) {
      ARGState child = pChild;
      // The child might be covered by another state
      // --> switch to the covering state
      if (child.isCovered()) {
        child = child.getCoveringState();
        assert !child.isCovered();
      }
      return child;
    }

  };

  @Option(secure=true, description="Verification witness: Include function calls and function returns?")
  private boolean exportFunctionCallsAndReturns = true;

  @Option(secure=true, description="Verification witness: Include assumptions (C statements)?")
  private boolean exportAssumptions = true;

  @Option(secure=true, description="Verification witness: Include the considered case of an assume?")
  private boolean exportAssumeCaseInfo = true;

  @Option(secure=true, description="Verification witness: Include the (starting) line numbers of the operations on the transitions?")
  private boolean exportLineNumbers = true;

  @Option(secure=true, description="Verification witness: Include the sourcecode of the operations?")
  private boolean exportSourcecode = true;

  @Option(secure=true, description="Verification witness: Include the offset within the file?")
  private boolean exportOffset = true;

  @Option(secure=true, description="Verification witness: Include an thread-identifier within the file?")
  private boolean exportThreadId = false;

  @Option(
    secure = true,
    description = "Verification witness: Revert escaping/renaming of functions for threads?"
  )
  private boolean revertThreadFunctionRenaming = false;

  private final LogManager logger;

  private final CFA cfa;

  private final MachineModel machineModel;

  private final Language language;

  private final AssumptionToEdgeAllocator assumptionToEdgeAllocator;

  private final ExpressionTreeFactory<Object> factory = ExpressionTrees.newCachingFactory();
  private final Simplifier<Object> simplifier = ExpressionTrees.newSimplifier(factory);

  /**
   * This is a temporary hack to easily obtain specification and verification tasks.
   * TODO: Move the witness export out of the ARG CPA after the new error report has been integrated
   * and obtain the values without this hack.
   */
  @Options
  private static class HackyOptions {

    @Option(secure=true, name="analysis.programNames",
        description="A String, denoting the programs to be analyzed")
    private String programs;

    @Option(secure=true, name="properties",
        description="List of property files (INTERNAL USAGE ONLY - DO NOT USE)")
    @FileOption(FileOption.Type.OPTIONAL_INPUT_FILE)
    private List<Path> propertyFiles = ImmutableList.of();

    @Option(secure=true,
        name="cpa.predicate.handlePointerAliasing",
        description = "Handle aliasing of pointers. "
        + "This adds disjunctions to the formulas, so be careful when using cartesian abstraction.")
    private boolean handlePointerAliasing = true;
  }

  private final HackyOptions hackyOptions = new HackyOptions();

  public ARGPathExporter(
      final Configuration pConfig,
      final LogManager pLogger,
      CFA pCFA)
      throws InvalidConfigurationException {
    Preconditions.checkNotNull(pConfig);
    pConfig.inject(this);
    pConfig.inject(hackyOptions);
    this.cfa = pCFA;
    this.machineModel = pCFA.getMachineModel();
    this.language = pCFA.getLanguage();
    this.logger = pLogger;
    this.assumptionToEdgeAllocator = new AssumptionToEdgeAllocator(pConfig, pLogger, machineModel);
  }

  public void writeErrorWitness(
      Appendable pTarget,
      final ARGState pRootState,
      final Predicate<? super ARGState> pIsRelevantState,
      Predicate<? super Pair<ARGState, ARGState>> pIsRelevantEdge,
      CounterexampleInfo pCounterExample)
      throws IOException {

    String defaultFileName = getInitialFileName(pRootState);
    WitnessWriter writer = new WitnessWriter(defaultFileName, GraphType.ERROR_WITNESS);
    writer.writePath(
        pTarget,
        pRootState,
        pIsRelevantState,
        pIsRelevantEdge,
        Optional.of(pCounterExample),
        GraphBuilder.ARG_PATH);
  }

  public void writeProofWitness(
      Appendable pTarget,
      final ARGState pRootState,
      final Predicate<? super ARGState> pIsRelevantState,
      Predicate<? super Pair<ARGState, ARGState>> pIsRelevantEdge)
      throws IOException {
    writeProofWitness(
        pTarget,
        pRootState,
        pIsRelevantState,
        pIsRelevantEdge,
        GraphBuilder.CFA_FROM_ARG,
        new InvariantProvider() {

          @Override
          public ExpressionTree<Object> provideInvariantFor(
              CFAEdge pEdge, Optional<? extends Collection<? extends ARGState>> pStates) {
            // TODO interface for extracting the information from states, similar to FormulaReportingState
            Set<ExpressionTree<Object>> stateInvariants = new HashSet<>();
            if (!pStates.isPresent()) {
              return ExpressionTrees.getTrue();
            }
            for (ARGState state : pStates.get()) {
              ValueAnalysisState valueAnalysisState =
                  AbstractStates.extractStateByType(state, ValueAnalysisState.class);
              ExpressionTree<Object> stateInvariant = ExpressionTrees.getTrue();
              if (valueAnalysisState != null) {
                ConcreteState concreteState =
                    ValueAnalysisConcreteErrorPathAllocator.createConcreteState(valueAnalysisState);
                for (AExpressionStatement expressionStatement :
                    assumptionToEdgeAllocator
                        .allocateAssumptionsToEdge(pEdge, concreteState)
                        .getExpStmts()) {
                  stateInvariant =
                      factory.and(
                          stateInvariant,
                          LeafExpression.of((Object) expressionStatement.getExpression()));
                }
              }

              String functionName = pEdge.getSuccessor().getFunctionName();
              for (ExpressionTreeReportingState etrs :
                  AbstractStates.asIterable(state).filter(ExpressionTreeReportingState.class)) {
                stateInvariant =
                    factory.and(
                        stateInvariant,
                        etrs.getFormulaApproximation(
                            cfa.getFunctionHead(functionName), pEdge.getSuccessor()));
              }
              stateInvariants.add(stateInvariant);
            }
            ExpressionTree<Object> invariant = factory.or(stateInvariants);
            return invariant;
          }
        });
  }

  public void writeProofWitness(
      Appendable pTarget,
      final ARGState pRootState,
      final Predicate<? super ARGState> pIsRelevantState,
      Predicate<? super Pair<ARGState, ARGState>> pIsRelevantEdge,
      GraphBuilder pGraphBuilder,
      InvariantProvider pInvariantProvider)
      throws IOException {
    Preconditions.checkNotNull(pTarget);
    Preconditions.checkNotNull(pRootState);
    Preconditions.checkNotNull(pIsRelevantState);
    Preconditions.checkNotNull(pIsRelevantEdge);
    Preconditions.checkNotNull(pGraphBuilder);
    Preconditions.checkNotNull(pInvariantProvider);

    String defaultFileName = getInitialFileName(pRootState);
    WitnessWriter writer =
        new WitnessWriter(defaultFileName, GraphType.PROOF_WITNESS, pInvariantProvider);
    writer.writePath(
        pTarget,
        pRootState,
        pIsRelevantState,
        pIsRelevantEdge,
        Optional.empty(),
        pGraphBuilder);
  }

  private String getInitialFileName(ARGState pRootState) {
    Deque<CFANode> worklist = Queues.newArrayDeque(AbstractStates.extractLocations(pRootState));

    while (!worklist.isEmpty()) {
      CFANode l = worklist.pop();
      for (CFAEdge e : CFAUtils.leavingEdges(l)) {
        Set<FileLocation> fileLocations = CFAUtils.getFileLocationsFromCfaEdge(e);
        if (fileLocations.size() > 0) {
          String fileName = fileLocations.iterator().next().getFileName();
          if (fileName != null) {
            return fileName;
          }
        }
        worklist.push(e.getSuccessor());
      }
    }

    throw new RuntimeException("Could not determine file name based on abstract state!");
  }

  private class WitnessWriter implements EdgeAppender {

    private final Multimap<String, NodeFlag> nodeFlags = TreeMultimap.create();
    private final Multimap<String, Property> violatedProperties = HashMultimap.create();
    private final Map<DelayedAssignmentsKey, CFAEdgeWithAssumptions> delayedAssignments = Maps.newHashMap();

    private final Multimap<String, Edge> leavingEdges = TreeMultimap.create();
    private final Multimap<String, Edge> enteringEdges = TreeMultimap.create();

    private final Map<String, ExpressionTree<Object>> stateInvariants = Maps.newLinkedHashMap();
    private final Map<String, String> stateScopes = Maps.newLinkedHashMap();

    private final Map<Edge, CFANode> loopHeadEnteringEdges = Maps.newHashMap();

    private final String defaultSourcefileName;
    private final GraphType graphType;

    private final InvariantProvider invariantProvider;

    private boolean isFunctionScope = false;

    public WitnessWriter(@Nullable String pDefaultSourcefileName, GraphType pGraphType) {
      this(pDefaultSourcefileName, pGraphType, InvariantProvider.TrueInvariantProvider.INSTANCE);
    }

    public WitnessWriter(
        String pDefaultSourceFileName, GraphType pGraphType, InvariantProvider pInvariantProvider) {
      this.defaultSourcefileName = pDefaultSourceFileName;
      this.graphType = pGraphType;
      this.invariantProvider = pInvariantProvider;
    }

    @Override
    public void appendNewEdge(
        final GraphMlBuilder pDoc,
        String pFrom,
        final String pTo,
        final CFAEdge pEdge,
        final Optional<Collection<ARGState>> pFromState,
        final Map<ARGState, CFAEdgeWithAssumptions> pValueMap) {

      attemptSwitchToFunctionScope(pEdge);

      TransitionCondition desc = constructTransitionCondition(pFrom, pTo, pEdge, pFromState, pValueMap);

      Edge edge = new Edge(pFrom, pTo, desc);
      if (desc.getMapping().containsKey(KeyDef.ENTERLOOPHEAD)) {
        Optional<CFANode> loopHead = entersLoop(pEdge);
        if (loopHead.isPresent()) {
          loopHeadEnteringEdges.put(edge, loopHead.get());
        }
      }

      putEdge(edge);
    }

    @Override
    public void appendNewEdgeToSink(
        GraphMlBuilder pDoc,
        String pFrom,
        CFAEdge pEdge,
        Optional<Collection<ARGState>> pFromState,
        Map<ARGState, CFAEdgeWithAssumptions> pValueMap) {
      appendNewEdge(pDoc, pFrom, SINK_NODE_ID, pEdge, pFromState, pValueMap);
    }

    private void attemptSwitchToFunctionScope(CFAEdge pEdge) {
      if (isFunctionScope) {
        return;
      }
      if (!(pEdge instanceof BlankEdge)) {
        return;
      }
      BlankEdge edge = (BlankEdge) pEdge;
      if (!edge.getDescription().equals("Function start dummy edge")) {
        return;
      }
      isFunctionScope = true;
    }

    /** build a transition-condition for the given edge, i.e. collect all
     * important data and store it in the new transition-condition. */
    private TransitionCondition constructTransitionCondition(
        final String pFrom,
        final String pTo,
        final CFAEdge pEdge,
        final Optional<Collection<ARGState>> pFromState,
        final Map<ARGState, CFAEdgeWithAssumptions> pValueMap) {

      TransitionCondition result = new TransitionCondition();

      if (graphType != GraphType.ERROR_WITNESS) {
        ExpressionTree<Object> invariant = ExpressionTrees.getTrue();
        if (exportInvariant(pEdge)) {
          invariant = simplifier.simplify(invariantProvider.provideInvariantFor(pEdge, pFromState));
        }
        putStateInvariant(pTo, invariant);
        String functionName = pEdge.getSuccessor().getFunctionName();
        stateScopes.put(pTo, isFunctionScope ? functionName : "");
      }

      if (AutomatonGraphmlCommon.handleAsEpsilonEdge(pEdge)) {
        return result;
      }

      if (entersLoop(pEdge).isPresent()) {
        result = result.putAndCopy(KeyDef.ENTERLOOPHEAD, "true");
      }

      if (exportFunctionCallsAndReturns) {
        if (pEdge.getSuccessor() instanceof FunctionEntryNode) {
          FunctionEntryNode in = (FunctionEntryNode) pEdge.getSuccessor();
          result = result.putAndCopy(KeyDef.FUNCTIONENTRY, in.getFunctionName());

        }
        if (pEdge.getSuccessor() instanceof FunctionExitNode) {
          FunctionExitNode out = (FunctionExitNode) pEdge.getSuccessor();
          result = result.putAndCopy(KeyDef.FUNCTIONEXIT, out.getFunctionName());
        }
      }

      if (pFromState.isPresent()) {
        result = extractTransitionForStates(pFrom, pTo, pEdge, pFromState.get(), pValueMap, result);
      }

      if (exportAssumeCaseInfo) {
        if (pEdge instanceof AssumeEdge) {
          AssumeEdge a = (AssumeEdge) pEdge;
          // If the assume edge or its sibling edge is followed by a pointer call,
          // the assumption is artificial and should not be exported
          if (CFAUtils.leavingEdges(a.getPredecessor())
              .anyMatch(
                  predecessor ->
                      CFAUtils.leavingEdges(predecessor.getSuccessor())
                          .anyMatch(
                              sibling -> sibling.getRawStatement().startsWith("pointer call")))) {
            // remove all info from transitionCondition
            return new TransitionCondition();
          }
          AssumeCase assumeCase = a.getTruthAssumption() ? AssumeCase.THEN : AssumeCase.ELSE;
          result = result.putAndCopy(KeyDef.CONTROLCASE, assumeCase.toString());
        }
      }

      if (exportLineNumbers) {
        Set<FileLocation> locations = CFAUtils.getFileLocationsFromCfaEdge(pEdge);
        if (locations.size() > 0) {
          FileLocation l = locations.iterator().next();
          if (!l.getFileName().equals(defaultSourcefileName)) {
            result = result.putAndCopy(KeyDef.ORIGINFILE, l.getFileName());
          }
          result = result.putAndCopy(KeyDef.ORIGINLINE, Integer.toString(l.getStartingLineInOrigin()));
        }
      }

      if (exportOffset) {
        Set<FileLocation> locations = CFAUtils.getFileLocationsFromCfaEdge(pEdge);
        if (locations.size() > 0) {
          FileLocation l = locations.iterator().next();
          if (!l.getFileName().equals(defaultSourcefileName)) {
            result = result.putAndCopy(KeyDef.ORIGINFILE, l.getFileName());
          }
          result = result.putAndCopy(KeyDef.OFFSET, Integer.toString(l.getNodeOffset()));
        }
      }

      if (exportSourcecode && !pEdge.getRawStatement().trim().isEmpty()) {
        result = result.putAndCopy(KeyDef.SOURCECODE, pEdge.getRawStatement());
      }

      return result;
    }

    private TransitionCondition extractTransitionForStates(
        final String pFrom,
        final String pTo,
        final CFAEdge pEdge,
        final Collection<ARGState> pFromStates,
        final Map<ARGState, CFAEdgeWithAssumptions> pValueMap,
        TransitionCondition result) {

      List<ExpressionTree<Object>> code = new ArrayList<>();
      Optional<AIdExpression> resultVariable = Optional.empty();
      Optional<String> resultFunction = Optional.empty();
      String functionName = pEdge.getPredecessor().getFunctionName();
      boolean isFunctionScope = this.isFunctionScope;

      for (ARGState state : pFromStates) {

        DelayedAssignmentsKey key = new DelayedAssignmentsKey(pFrom, pEdge, state);
        CFAEdgeWithAssumptions cfaEdgeWithAssignments = delayedAssignments.get(key);

        final CFAEdgeWithAssumptions currentEdgeWithAssignments;
        if (pValueMap != null && (currentEdgeWithAssignments = pValueMap.get(state)) != null) {
          if (cfaEdgeWithAssignments == null) {
            cfaEdgeWithAssignments = currentEdgeWithAssignments;

          } else {
            Builder<AExpressionStatement> allAssignments = ImmutableList.builder();
            allAssignments.addAll(cfaEdgeWithAssignments.getExpStmts());
            allAssignments.addAll(currentEdgeWithAssignments.getExpStmts());
            cfaEdgeWithAssignments =
                new CFAEdgeWithAssumptions(
                    pEdge, allAssignments.build(), currentEdgeWithAssignments.getComment());
          }
        }

        if (cfaEdgeWithAssignments != null) {

          Collection<AExpressionStatement> assignments = cfaEdgeWithAssignments.getExpStmts();
          Predicate<AExpressionStatement> assignsParameterOfOtherFunction =
              new AssignsParameterOfOtherFunction(pEdge);
          Collection<AExpressionStatement> functionValidAssignments =
              Collections2.filter(assignments, assignsParameterOfOtherFunction);

          if (functionValidAssignments.size() < assignments.size()) {
            cfaEdgeWithAssignments =
                new CFAEdgeWithAssumptions(
                    pEdge, functionValidAssignments, cfaEdgeWithAssignments.getComment());
            FluentIterable<CFAEdge> nextEdges = CFAUtils.leavingEdges(pEdge.getSuccessor());

            if (nextEdges.size() == 1 && state.getChildren().size() == 1) {
              String keyFrom = pTo;
              CFAEdge keyEdge = Iterables.getOnlyElement(nextEdges);
              ARGState keyState = Iterables.getOnlyElement(state.getChildren());
              Collection<AExpressionStatement> valueAssignments =
                  Collections2.filter(assignments, Predicates.not(assignsParameterOfOtherFunction));
              CFAEdgeWithAssumptions valueCFAEdgeWithAssignments =
                  new CFAEdgeWithAssumptions(keyEdge, valueAssignments, "");
              delayedAssignments.put(
                  new DelayedAssignmentsKey(keyFrom, keyEdge, keyState),
                  valueCFAEdgeWithAssignments);
            }
          }

          // Determine the scope for static local variables
          for (AExpressionStatement functionValidAssignment : functionValidAssignments) {
            if (functionValidAssignment instanceof CExpressionStatement) {
              CExpression expression = (CExpression) functionValidAssignment.getExpression();
              for (CIdExpression idExpression :
                  CFAUtils.getIdExpressionsOfExpression(expression).toSet()) {
                final CSimpleDeclaration declaration = idExpression.getDeclaration();
                final String qualified = declaration.getQualifiedName();
                if (declaration.getName().contains("static")
                    && !declaration.getOrigName().contains("static")
                    && qualified.contains("::")) {
                  isFunctionScope = true;
                  functionName = qualified.substring(0, qualified.indexOf("::"));
                }
              }
            }
          }

          // Do not export our own temporary variables
          Predicate<AIdExpression> isTmpVariable =
              idExpression ->
                  idExpression
                      .getDeclaration()
                      .getQualifiedName()
                      .toUpperCase()
                      .contains("__CPACHECKER_TMP");
          assignments =
              Collections2.filter(
                  cfaEdgeWithAssignments.getExpStmts(),
                  statement ->
                      statement.getExpression() instanceof CExpression
                          && !CFAUtils.getIdExpressionsOfExpression(
                                  (CExpression) statement.getExpression())
                              .anyMatch(isTmpVariable));

          // Export function return value for cases where it is not explicitly assigned to a variable
          if (pEdge instanceof AStatementEdge) {
            AStatementEdge edge = (AStatementEdge) pEdge;
            if (edge.getStatement() instanceof AFunctionCallAssignmentStatement) {
              AFunctionCallAssignmentStatement assignment =
                  (AFunctionCallAssignmentStatement) edge.getStatement();
              if (assignment.getLeftHandSide() instanceof AIdExpression
                  && assignment.getFunctionCallExpression().getFunctionNameExpression()
                      instanceof AIdExpression) {
                AIdExpression idExpression = (AIdExpression) assignment.getLeftHandSide();
                if (isTmpVariable.apply(idExpression)) {
                  assignments =
                      Collections2.filter(
                          cfaEdgeWithAssignments.getExpStmts(),
                          statement ->
                              statement.getExpression() instanceof CExpression
                                  && !CFAUtils.getIdExpressionsOfExpression(
                                          (CExpression) statement.getExpression())
                                      .anyMatch(
                                          id ->
                                              isTmpVariable.apply(id) && !id.equals(idExpression)));
                  resultVariable = Optional.of(idExpression);
                  AIdExpression resultFunctionName =
                      (AIdExpression)
                          assignment.getFunctionCallExpression().getFunctionNameExpression();
                  resultFunction = Optional.of(resultFunctionName.getDeclaration().getOrigName());
                }
              }
            }
          }
          assert resultVariable.isPresent() == resultFunction.isPresent();

          if (!assignments.isEmpty()) {
            code.add(
                factory.and(
                    Collections2.transform(
                        assignments,
                        pExpressionStatement ->
                            LeafExpression.of(pExpressionStatement.getExpression()))));
          }
        }

        if (exportThreadId) {
          result = exportThreadId(result, pEdge, state);
        }
      }

      if (graphType != GraphType.PROOF_WITNESS && exportAssumptions && !code.isEmpty()) {
        ExpressionTree<Object> invariant = factory.or(code);
        CExpressionToOrinalCodeVisitor transformer =
            resultVariable.isPresent()
                ? CExpressionToOrinalCodeVisitor.BASIC_TRANSFORMER.substitute(
                    (CIdExpression) resultVariable.get(), "\\result")
                : CExpressionToOrinalCodeVisitor.BASIC_TRANSFORMER;
        final Function<Object, String> converter =
            new Function<Object, String>() {

              @Override
              public String apply(Object pLeafExpression) {
                if (pLeafExpression instanceof CExpression) {
                  return ((CExpression) pLeafExpression).accept(transformer);
                }
                if (pLeafExpression == null) {
                  return "(0)";
                }
                return pLeafExpression.toString();
              }
            };
        final String assumptionCode;

        // If there are only conjunctions, use multiple statements
        // instead of the "&&" operator that is harder to parse.
        if (ExpressionTrees.isAnd(invariant)) {
          assumptionCode =
              Joiner.on("; ")
                  .join(
                      ExpressionTrees.getChildren(invariant)
                          .transform(pTree -> ExpressionTrees.convert(pTree, converter)));
        } else {
          assumptionCode = ExpressionTrees.convert(invariant, converter).toString();
        }

        result = result.putAndCopy(KeyDef.ASSUMPTION, assumptionCode + ";");
        if (isFunctionScope) {
          if (revertThreadFunctionRenaming) {
            functionName = CFACloner.extractFunctionName(functionName);
          }
          result = result.putAndCopy(KeyDef.ASSUMPTIONSCOPE, functionName);
        }
        if (resultFunction.isPresent()) {
          result = result.putAndCopy(KeyDef.ASSUMPTIONRESULTFUNCTION, resultFunction.get());
        }
      }

      return result;
    }

    /** export the id of the executed thread into the witness.
     * We assume that the edge can be assigned to exactly one thread. */
    private TransitionCondition exportThreadId(TransitionCondition result, final CFAEdge pEdge,
        ARGState state) {
      ThreadingState threadingState = AbstractStates.extractStateByType(state, ThreadingState.class);
      if (threadingState != null) {
        for (String threadId : threadingState.getThreadIds()) {
          if (threadingState.getThreadLocation(threadId).getLocationNode().equals(pEdge.getPredecessor())) {
            result = result.putAndCopy(KeyDef.THREADID, threadId);
            break;
          }
        }
      }
      return result;
    }

    /**
     * Starting from the given initial ARG state, collects that state and all
     * transitive successors (as defined by the successor function) that are
     * children of their direct predecessor and are accepted by the path state
     * predicate.
     *
     * @param pInitialState the initial ARG state.
     * @param pSuccessorFunction the function defining the successors of a
     * state.
     * @param pPathStates a filter on the nodes.
     *
     * @return the parents with their children.
     */
    private Iterable<ARGState> collectPathNodes(
        final ARGState pInitialState,
        final Function<? super ARGState, ? extends Iterable<ARGState>> pSuccessorFunction,
        final Predicate<? super ARGState> pPathStates) {
      return Iterables.transform(
          collectPathEdges(pInitialState, pSuccessorFunction, pPathStates), Pair::getFirst);
    }

    /**
     * Starting from the given initial ARG state, collects that state and all
     * transitive successors (as defined by the successor function) that are
     * children of their direct predecessor. Children are only computed for
     * nodes that are accepted by the path state predicate.
     *
     * @param pInitialState the initial ARG state.
     * @param pSuccessorFunction the function defining the successors of a
     * state.
     * @param pPathStates a filter on the parent nodes.
     *
     * @return the parents with their children.
     */
    private Iterable<Pair<ARGState, Iterable<ARGState>>> collectPathEdges(
        final ARGState pInitialState,
        final Function<? super ARGState, ? extends Iterable<ARGState>> pSuccessorFunction,
        final Predicate<? super ARGState> pPathStates) {
      return new Iterable<Pair<ARGState, Iterable<ARGState>>>() {

        private final Set<ARGState> visited = new HashSet<>();

        private final Deque<ARGState> waitlist = new ArrayDeque<>();

        {
          waitlist.add(pInitialState);
          visited.add(pInitialState);
        }

        @Override
        public Iterator<Pair<ARGState, Iterable<ARGState>>> iterator() {
          return new Iterator<Pair<ARGState, Iterable<ARGState>>>() {

            @Override
            public boolean hasNext() {
              return !waitlist.isEmpty();
            }

            @Override
            public Pair<ARGState, Iterable<ARGState>> next() {
              if (!hasNext()) {
                throw new NoSuchElementException();
              }
              assert !waitlist.isEmpty();
              final ARGState parent = waitlist.poll();

              // Get all children
              FluentIterable<ARGState> children =
                  FluentIterable.from(pSuccessorFunction.apply(parent))
                      .transform(COVERED_TO_COVERING)
                      .filter(parent.getChildren()::contains);

              // Only the children on the path become parents themselves
              for (ARGState child : children.filter(pPathStates)) {
                if (visited.add(child)) {
                  waitlist.offer(child);
                }
              }

              return Pair.<ARGState, Iterable<ARGState>>of(parent, children);
            }

            @Override
            public void remove() {
              throw new UnsupportedOperationException("Removal not supported.");
            }
          };
        }
      };
    }

    public void writePath(
        Appendable pTarget,
        final ARGState pRootState,
        final Predicate<? super ARGState> pIsRelevantState,
        final Predicate<? super Pair<ARGState, ARGState>> pIsRelevantEdge,
        Optional<CounterexampleInfo> pCounterExample,
        GraphBuilder pGraphBuilder)
        throws IOException {

      Map<ARGState, CFAEdgeWithAssumptions> valueMap = null;
      if (pCounterExample.isPresent() && pCounterExample.get().isPreciseCounterExample()) {
        valueMap = pCounterExample.get().getExactVariableValues();
      }

      GraphMlBuilder doc;
      try {
        doc =
            new GraphMlBuilder(
                graphType,
                defaultSourcefileName,
                language,
                machineModel,
                hackyOptions.handlePointerAliasing ? "precise" : "simple",
                FluentIterable.from(hackyOptions.propertyFiles)
                    .transform(
                        new Function<Path, String>() {

                          @Override
                          public String apply(Path pArg0) {
                            try {
                              return MoreFiles.toString(pArg0, Charsets.UTF_8).trim();
                            } catch (IOException e) {
                              logger.logUserException(
                                  Level.WARNING, e, "Could not export specification to witness.");
                              return "Unknown specification";
                            }
                          }
                        }),
                hackyOptions.programs);
      } catch (ParserConfigurationException e) {
        throw new IOException(e);
      }

      String entryStateNodeId = pGraphBuilder.getId(pRootState);

      // Collect node flags in advance
      for (ARGState s : collectPathNodes(pRootState, ARGState::getChildren, pIsRelevantState)) {
        String sourceStateNodeId = pGraphBuilder.getId(s);
        EnumSet<NodeFlag> sourceNodeFlags = EnumSet.noneOf(NodeFlag.class);
        if (sourceStateNodeId.equals(entryStateNodeId)) {
          sourceNodeFlags.add(NodeFlag.ISENTRY);
        }
        sourceNodeFlags.addAll(extractNodeFlags(s));
        nodeFlags.putAll(sourceStateNodeId, sourceNodeFlags);
        violatedProperties.putAll(sourceStateNodeId, extractViolatedProperties(s));
      }
      // Write the sink node
      nodeFlags.put(SINK_NODE_ID, NodeFlag.ISSINKNODE);

      // Build the actual graph
      pGraphBuilder.buildGraph(
          pRootState,
          pIsRelevantState,
          pIsRelevantEdge,
          valueMap,
          doc,
          collectPathEdges(pRootState, ARGState::getChildren, pIsRelevantState),
          this);

      // Remove edges that lead to the sink but have a sibling edge that has the same label
      Collection<Edge> toRemove = Sets.newHashSet();
      for (Edge edge : leavingEdges.values()) {
        if (edge.target.equals(SINK_NODE_ID)) {
          for (Edge otherEdge : leavingEdges.get(edge.source)) {
            if (!edge.equals(otherEdge)
                && edge.label.equals(otherEdge.label)
                && !toRemove.contains(otherEdge)) {
              toRemove.add(edge);
              break;
            }
          }
        }
      }
      for (Edge edge : toRemove) {
        boolean removed = removeEdge(edge);
        assert removed;
      }

      // Merge nodes with empty or repeated edges
      Supplier<Iterator<Edge>> redundantEdgeIteratorSupplier =
          () -> Iterables.filter(leavingEdges.values(), isEdgeRedundant).iterator();

      Iterator<Edge> redundantEdgeIterator = redundantEdgeIteratorSupplier.get();
      while (redundantEdgeIterator.hasNext()) {
        Edge edge = redundantEdgeIterator.next();
        mergeNodes(edge);
        redundantEdgeIterator = redundantEdgeIteratorSupplier.get();
        assert leavingEdges.isEmpty() || leavingEdges.containsKey(entryStateNodeId);
      }

      // Write elements
      {
        Map<String, Element> nodes = Maps.newHashMap();
        Deque<String> waitlist = Queues.newArrayDeque();
        waitlist.push(entryStateNodeId);
        Element entryNode = createNewNode(doc, entryStateNodeId);
        addInvariantsData(doc, entryNode, entryStateNodeId);
        nodes.put(entryStateNodeId, entryNode);
        while (!waitlist.isEmpty()) {
          String source = waitlist.pop();
          for (Edge edge : leavingEdges.get(source)) {
            setLoopHeadInvariantIfApplicable(edge.target);

            Element targetNode = nodes.get(edge.target);
            if (targetNode == null) {
              targetNode = createNewNode(doc, edge.target);
              if (!ExpressionTrees.getFalse()
                  .equals(addInvariantsData(doc, targetNode, edge.target))) {
                waitlist.push(edge.target);
              }
              nodes.put(edge.target, targetNode);
            }
            createNewEdge(doc, edge, targetNode);
          }
        }
      }
      doc.appendTo(pTarget);
    }

    private void setLoopHeadInvariantIfApplicable(String pTarget) {
      if (!ExpressionTrees.getTrue().equals(getStateInvariant(pTarget))) {
        return;
      }
      ExpressionTree<Object> loopHeadInvariant = ExpressionTrees.getFalse();
      String scope = null;
      for (Edge enteringEdge : enteringEdges.get(pTarget)) {
        if (enteringEdge.label.getMapping().containsKey(KeyDef.ENTERLOOPHEAD)) {
          CFANode loopHead = loopHeadEnteringEdges.get(enteringEdge);
          if (loopHead != null) {
            String functionName = loopHead.getFunctionName();
            if (scope == null) {
              scope = functionName;
            } else if (!scope.equals(functionName)) {
              return;
            }
            for (CFAEdge enteringCFAEdge : CFAUtils.enteringEdges(loopHead)) {
              loopHeadInvariant =
                  Or.of(
                      loopHeadInvariant,
                      invariantProvider.provideInvariantFor(enteringCFAEdge, Optional.empty()));
            }
          } else {
            return;
          }
        } else {
          return;
        }
      }
      stateInvariants.put(pTarget, loopHeadInvariant);
      if (scope != null) {
        stateScopes.put(pTarget, scope);
      }
    }

    private ExpressionTree<Object> addInvariantsData(
        GraphMlBuilder pDoc, Element pNode, String pStateId) {
      ExpressionTree<Object> tree = getStateInvariant(pStateId);
      if (!tree.equals(ExpressionTrees.getTrue())) {
        pDoc.addDataElementChild(pNode, KeyDef.INVARIANT, tree.toString());
        String scope = stateScopes.get(pStateId);
        if (scope != null && !scope.isEmpty() && !tree.equals(ExpressionTrees.getFalse())) {
          pDoc.addDataElementChild(pNode, KeyDef.INVARIANTSCOPE, scope);
        }
      }
      return tree;
    }

    private final Predicate<String> isNodeRedundant =
        new Predicate<String>() {

          @Override
          public boolean apply(String pNode) {
            if (!ExpressionTrees.getTrue().equals(getStateInvariant(pNode))) {
              return false;
            }
            if (!nodeFlags.get(pNode).isEmpty()) {
              return false;
            }
            if (!violatedProperties.get(pNode).isEmpty()) {
              return false;
            }
            if (enteringEdges.get(pNode).isEmpty()) {
              return false;
            }
            for (Edge edge : enteringEdges.get(pNode)) {
              if (!edge.label.getMapping().isEmpty()) {
                return false;
              }
            }
            return true;
          }
        };

    private final Predicate<Edge> isEdgeRedundant =
        new Predicate<Edge>() {

          @Override
          public boolean apply(final Edge pEdge) {
            if (isNodeRedundant.apply(pEdge.target)) {
              return true;
            }

            if (pEdge.label.getMapping().isEmpty()) {
              return true;
            }

            // An edge is never redundant if there are conflicting scopes
            ExpressionTree<Object> sourceTree = getStateInvariant(pEdge.source);
            if (sourceTree != null) {
              String sourceScope = stateScopes.get(pEdge.source);
              String targetScope = stateScopes.get(pEdge.target);
              if (sourceScope != null && targetScope != null && !sourceScope.equals(targetScope)) {
                return false;
              }
            }

            // An edge is redundant if it is the only leaving edge of a
            // node and it is empty or all its non-assumption contents
            // are summarized by a preceding edge
            boolean summarizedByPreceedingEdge =
                Iterables.any(
                    enteringEdges.get(pEdge.source),
                    pPrecedingEdge -> pPrecedingEdge.label.summarizes(pEdge.label));

            if ((!pEdge.label.hasTransitionRestrictions()
                    || summarizedByPreceedingEdge
                    || (pEdge.label.getMapping().size() == 1
                        && pEdge.label.getMapping().containsKey(KeyDef.FUNCTIONEXIT)))
                && (leavingEdges.get(pEdge.source).size() == 1)) {
              return true;
            }

            if (Iterables.all(leavingEdges.get(pEdge.source),
                pLeavingEdge -> pLeavingEdge.label.getMapping().isEmpty())) {
              return true;
            }
            return false;
          }
        };

    /** Merge two consecutive nodes into one new node,
     * if the edge between the nodes is redundant.
     * The merge also merges the information of the nodes,
     * e.g. disjuncts their invariants. */
    private void mergeNodes(final Edge pEdge) {
      Preconditions.checkArgument(isEdgeRedundant.apply(pEdge));

      // By default, merge into the predecessor,
      // but if the successor is redundant while the predecessor is not,
      // merge into the successor.
      boolean intoPredecessor =
          isNodeRedundant.apply(pEdge.target) || !isNodeRedundant.apply(pEdge.target);

      final String source = intoPredecessor ? pEdge.source : pEdge.target;
      final String target = intoPredecessor ? pEdge.target : pEdge.source;

      // Merge the flags
      nodeFlags.putAll(source, nodeFlags.removeAll(target));

      // Merge the trees
      mergeExpressionTrees(source, target);

      // Merge the violated properties
      violatedProperties.putAll(source, violatedProperties.removeAll(target));

      // Move the leaving edges
      Collection<Edge> leavingEdgesToMove = ImmutableList.copyOf(this.leavingEdges.get(target));
      // Remove the edges from their successors
      for (Edge leavingEdge : leavingEdgesToMove) {
        boolean removed = removeEdge(leavingEdge);
        assert removed;
      }
      // Create the replacement edges,
      // Add them as leaving edges to the source node,
      // Add them as entering edges to their target nodes
      for (Edge leavingEdge : leavingEdgesToMove) {
        TransitionCondition label = pEdge.label.putAllAndCopy(leavingEdge.label);
        Edge replacementEdge = new Edge(source, leavingEdge.target, label);
        putEdge(replacementEdge);
        CFANode loopHead = loopHeadEnteringEdges.get(leavingEdge);
        if (loopHead != null) {
          loopHeadEnteringEdges.remove(leavingEdge);
          loopHeadEnteringEdges.put(replacementEdge, loopHead);
        }
      }

      // Move the entering edges
      Collection<Edge> enteringEdgesToMove = ImmutableList.copyOf(this.enteringEdges.get(target));
      // Remove the edges from their predecessors
      for (Edge enteringEdge : enteringEdgesToMove) {
        boolean removed = removeEdge(enteringEdge);
        assert removed : "could not remove edge: " + enteringEdge;
      }
      // Create the replacement edges,
      // Add them as entering edges to the source node,
      // Add add them as leaving edges to their source nodes
      for (Edge enteringEdge : enteringEdgesToMove) {
        if (!pEdge.equals(enteringEdge)) {
          TransitionCondition label = pEdge.label.putAllAndCopy(enteringEdge.label);
          Edge replacementEdge = new Edge(enteringEdge.source, source, label);
          putEdge(replacementEdge);
          CFANode loopHead = loopHeadEnteringEdges.get(enteringEdge);
          if (loopHead != null) {
            loopHeadEnteringEdges.remove(enteringEdge);
            loopHeadEnteringEdges.put(replacementEdge, loopHead);
          }
        }
      }

    }

    private ExpressionTree<Object> getTargetStateInvariant(String pTargetState) {
      ExpressionTree<Object> targetStateInvariant = getStateInvariant(pTargetState);
      return targetStateInvariant;
    }

    /** Merge two expressionTrees for source and target.
     * We also perform some kind of simplification. */
    private void mergeExpressionTrees(final String source, final String target) {
      ExpressionTree<Object> sourceTree = getStateInvariant(source);
      ExpressionTree<Object> targetTree = getStateInvariant(target);
      String sourceScope = stateScopes.get(source);
      String targetScope = stateScopes.get(target);

      if (ExpressionTrees.getTrue().equals(targetTree)
          && !ExpressionTrees.getTrue().equals(sourceTree)
          && (targetScope == null || targetScope.equals(sourceScope))) {
        ExpressionTree<Object> newTargetTree = getTargetStateInvariant(target);
        newTargetTree = simplifier.simplify(factory.and(targetTree, newTargetTree));
        stateInvariants.put(target, newTargetTree);
        targetTree = newTargetTree;
      } else if (!ExpressionTrees.getTrue().equals(targetTree)
          && ExpressionTrees.getTrue().equals(sourceTree)
          && (sourceScope == null || sourceScope.equals(targetScope))
          && enteringEdges.get(source).size() <= 1) {
        ExpressionTree<Object> newSourceTree = ExpressionTrees.getFalse();
        for (Edge e : enteringEdges.get(source)) {
          newSourceTree = factory.or(newSourceTree, getStateInvariant(e.source));
        }
        newSourceTree = simplifier.simplify(factory.and(targetTree, newSourceTree));
        stateInvariants.put(source, newSourceTree);
        sourceTree = newSourceTree;
      }

      final String newScope;
      if (ExpressionTrees.isConstant(sourceTree)
          || Objects.equals(sourceScope, targetScope)) {
        newScope = targetScope;
      } else if (ExpressionTrees.isConstant(targetTree)) {
        newScope = sourceScope;
      } else {
        newScope = null;
      }
      ExpressionTree<Object> newTree = mergeStateInvariantsIntoFirst(source, target);
      if (newTree != null) {
        if (newScope == null && !ExpressionTrees.isConstant(newTree)) {
          putStateInvariant(source, ExpressionTrees.getTrue());
          stateScopes.remove(source);
        } else {
          stateScopes.put(source, newScope);
        }
      }
    }

    private void putEdge(Edge pEdge) {
      assert leavingEdges.size() == enteringEdges.size();
      leavingEdges.put(pEdge.source, pEdge);
      enteringEdges.put(pEdge.target, pEdge);
      assert leavingEdges.size() == enteringEdges.size();
    }

    private boolean removeEdge(Edge pEdge) {
      assert leavingEdges.size() == enteringEdges.size();
      if (leavingEdges.remove(pEdge.source, pEdge)) {
        boolean alsoRemoved = enteringEdges.remove(pEdge.target, pEdge);
        assert alsoRemoved : "edge was not removed: " + pEdge;
        assert leavingEdges.size() == enteringEdges.size();
        return true;
      }
      return false;
    }

    private Element createNewEdge(GraphMlBuilder pDoc, Edge pEdge, Element pTargetNode) {
      Element edge = pDoc.createEdgeElement(pEdge.source, pEdge.target);
      for (Map.Entry<KeyDef, String> entry : pEdge.label.getMapping().entrySet()) {
        KeyDef keyDef = entry.getKey();
        String value = entry.getValue();
        if (keyDef.keyFor.equals(ElementType.EDGE)) {
          pDoc.addDataElementChild(edge, keyDef, value);
        } else if (keyDef.keyFor.equals(ElementType.NODE)) {
          pDoc.addDataElementChild(pTargetNode, keyDef, value);
        }
      }
      return edge;
    }

    private Element createNewNode(GraphMlBuilder pDoc, String pEntryStateNodeId) {
      Element result = pDoc.createNodeElement(pEntryStateNodeId, NodeType.ONPATH);
      for (NodeFlag f : nodeFlags.get(pEntryStateNodeId)) {
        pDoc.addDataElementChild(result, f.key, "true");
      }
      for (Property violation : violatedProperties.get(pEntryStateNodeId)) {
        pDoc.addDataElementChild(result, KeyDef.VIOLATEDPROPERTY, violation.toString());
      }
      return result;
    }

    private Collection<NodeFlag> extractNodeFlags(ARGState pState) {
      if (pState.isTarget()) {
        return Collections.singleton(NodeFlag.ISVIOLATION);
      }
      return Collections.emptySet();
    }

    private Collection<Property> extractViolatedProperties(ARGState pState) {
      ArrayList<Property> result = Lists.newArrayList();
      if (pState.isTarget()) {
        result.addAll(pState.getViolatedProperties());
      }
      return result;
    }

    /**
     * Records the given invariant for the given state.
     *
     * If no invariant is present for this state, the given invariant is the new state invariant.
     * Otherwise, the new state invariant is a disjunction of the previous and the given invariant.
     *
     * However, if no invariants are ever added for a state, it is assumed to have the invariant "true".
     *
     * @param pStateId the state id.
     * @param pValue the invariant to be added.
     */
    private void putStateInvariant(String pStateId, ExpressionTree<Object> pValue) {
      ExpressionTree<Object> prev = stateInvariants.get(pStateId);
      if (prev == null) {
        stateInvariants.put(pStateId, simplifier.simplify(pValue));
        return;
      }
      ExpressionTree<Object> result = simplifier.simplify(factory.or(prev, pValue));
      stateInvariants.put(pStateId, result);
    }

    /**
     * Merges the invariants for the given state ids and stores it as the new invariant for the first of the given ids.
     *
     * @param pStateId the state id.
     * @param pOtherStateId the other state id.
     *
     * @return the merged invariant. {@code null} if neither state had an invariant.
     */
    private @Nullable ExpressionTree<Object> mergeStateInvariantsIntoFirst(
        String pStateId, String pOtherStateId) {
      ExpressionTree<Object> prev = stateInvariants.get(pStateId);
      ExpressionTree<Object> other = stateInvariants.get(pOtherStateId);
      if (prev == null) {
        stateInvariants.put(pStateId, other);
        return other;
      }
      if (other == null) {
        return prev;
      }
      ExpressionTree<Object> result = simplifier.simplify(factory.or(prev, other));
      stateInvariants.put(pStateId, result);
      return result;
    }

    private ExpressionTree<Object> getStateInvariant(String pStateId) {
      ExpressionTree<Object> result = stateInvariants.get(pStateId);
      if (result == null) {
        return ExpressionTrees.getTrue();
      }
      return result;
    }
  }

  private boolean exportInvariant(CFAEdge pEdge) {
    if (AutomatonGraphmlCommon.handleAsEpsilonEdge(pEdge)) {
      return false;
    }
    if (entersLoop(pEdge).isPresent()) {
      return true;
    }

    CFANode referenceNode = pEdge.getSuccessor();
    Queue<CFANode> waitlist = Queues.newArrayDeque();
    Set<CFANode> visited = Sets.newHashSet();
    waitlist.offer(referenceNode);
    visited.add(referenceNode);
    for (CFAEdge assumeEdge : CFAUtils.enteringEdges(referenceNode).filter(AssumeEdge.class)) {
      if (visited.add(assumeEdge.getPredecessor())) {
        waitlist.offer(assumeEdge.getPredecessor());
      }
    }
    Predicate<CFAEdge> epsilonEdge = edge -> !(edge instanceof AssumeEdge);
    Predicate<CFANode> loopProximity =
        cfa.getAllLoopHeads().isPresent()
            ? pNode -> cfa.getAllLoopHeads().get().contains(pNode) || pNode.isLoopStart()
            : pNode -> pNode.isLoopStart();
    while (!waitlist.isEmpty()) {
      CFANode current = waitlist.poll();
      if (loopProximity.apply(current)) {
        return true;
      }
      for (CFAEdge enteringEdge : CFAUtils.enteringEdges(current).filter(epsilonEdge)) {
        if (visited.add(enteringEdge.getPredecessor())) {
          waitlist.offer(enteringEdge.getPredecessor());
        }
      }
    }
    return false;
  }

  private static class DelayedAssignmentsKey {

    private final String from;

    private final CFAEdge edge;

    private final ARGState state;

    public DelayedAssignmentsKey(String pFrom, CFAEdge pEdge, ARGState pState) {
      this.from = pFrom;
      this.edge = pEdge;
      this.state = pState;
    }

    @Override
    public int hashCode() {
      return Objects.hash(from, edge, state);
    }

    @Override
    public boolean equals(Object pObj) {
      if (this == pObj) {
        return true;
      }
      if (pObj instanceof DelayedAssignmentsKey) {
        DelayedAssignmentsKey other = (DelayedAssignmentsKey) pObj;
        return Objects.equals(from, other.from)
            && Objects.equals(edge, other.edge)
            && Objects.equals(state, other.state);
      }
      return false;
    }
  }
  private static class AssignsParameterOfOtherFunction implements Predicate<AExpressionStatement> {

    private final CFAEdge edge;

    private final String qualifier;

    public AssignsParameterOfOtherFunction(CFAEdge pEdge) {
      edge = pEdge;
      String currentFunctionName = pEdge.getPredecessor().getFunctionName();
      qualifier = Strings.isNullOrEmpty(currentFunctionName) ? "" : currentFunctionName + "::";
    }

    @Override
    public boolean apply(AExpressionStatement pArg0) {
      AExpression exp = pArg0.getExpression();
      if (!(exp instanceof CExpression)) {
        return false;
      }
      CExpression cExp = (CExpression) exp;
      return cExp.accept(new CExpressionVisitor<Boolean, RuntimeException>() {

        @Override
        public Boolean visit(CArraySubscriptExpression pIastArraySubscriptExpression) {
          return pIastArraySubscriptExpression.getArrayExpression().accept(this)
              && pIastArraySubscriptExpression.getSubscriptExpression().accept(this);
        }

        @Override
        public Boolean visit(CFieldReference pIastFieldReference) {
          return pIastFieldReference.getFieldOwner().accept(this);
        }

        @Override
        public Boolean visit(CIdExpression pIastIdExpression) {
          CSimpleDeclaration declaration = pIastIdExpression.getDeclaration();
          if (declaration instanceof CParameterDeclaration && edge instanceof FunctionCallEdge) {
            return declaration.getQualifiedName().startsWith(qualifier);
          }
          return true;
        }

        @Override
        public Boolean visit(CPointerExpression pPointerExpression) {
          return pPointerExpression.getOperand().accept(this);
        }

        @Override
        public Boolean visit(CComplexCastExpression pComplexCastExpression) {
          return pComplexCastExpression.getOperand().accept(this);
        }

        @Override
        public Boolean visit(CBinaryExpression pIastBinaryExpression) {
          return pIastBinaryExpression.getOperand1().accept(this)
              && pIastBinaryExpression.getOperand2().accept(this);
        }

        @Override
        public Boolean visit(CCastExpression pIastCastExpression) {
          return pIastCastExpression.getOperand().accept(this);
        }

        @Override
        public Boolean visit(CCharLiteralExpression pIastCharLiteralExpression) {
          return true;
        }

        @Override
        public Boolean visit(CFloatLiteralExpression pIastFloatLiteralExpression) {
          return true;
        }

        @Override
        public Boolean visit(CIntegerLiteralExpression pIastIntegerLiteralExpression) {
          return true;
        }

        @Override
        public Boolean visit(CStringLiteralExpression pIastStringLiteralExpression) {
          return true;
        }

        @Override
        public Boolean visit(CTypeIdExpression pIastTypeIdExpression) {
          return true;
        }

        @Override
        public Boolean visit(CUnaryExpression pIastUnaryExpression) {
          return pIastUnaryExpression.getOperand().accept(this);
        }

        @Override
        public Boolean visit(CImaginaryLiteralExpression pIastLiteralExpression) {
          return pIastLiteralExpression.getValue().accept(this);
        }

        @Override
        public Boolean visit(CAddressOfLabelExpression pAddressOfLabelExpression) {
          return true;
        }
      });
    }

  }

  private Optional<CFANode> entersLoop(CFAEdge pEdge) {
    class EnterLoopVisitor implements CFAVisitor {

      private CFANode loopHead;

      @Override
      public TraversalProcess visitNode(CFANode pNode) {
        if (pNode.isLoopStart()) {
          loopHead = pNode;
          return TraversalProcess.ABORT;
        }
        if (pNode.getNumLeavingEdges() > 1) {
          return TraversalProcess.SKIP;
        }
        return TraversalProcess.CONTINUE;
      }

      @Override
      public TraversalProcess visitEdge(CFAEdge pEdge) {
        return AutomatonGraphmlCommon.handleAsEpsilonEdge(pEdge)
            ? TraversalProcess.CONTINUE
            : TraversalProcess.SKIP;
      }
    }
    EnterLoopVisitor enterLoopVisitor = new EnterLoopVisitor();
    CFATraversal.dfs()
        .ignoreFunctionCalls()
        .ignoreSummaryEdges()
        .traverse(pEdge.getSuccessor(), enterLoopVisitor);
    return Optional.ofNullable(enterLoopVisitor.loopHead);
  }

}
