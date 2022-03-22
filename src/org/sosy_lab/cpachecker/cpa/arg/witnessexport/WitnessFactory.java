// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2007-2020 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.cpa.arg.witnessexport;

import static com.google.common.base.Strings.nullToEmpty;
import static com.google.common.collect.FluentIterable.from;
import static org.sosy_lab.common.collect.Collections3.transformedImmutableListCopy;
import static org.sosy_lab.common.collect.Collections3.transformedImmutableSetCopy;
import static org.sosy_lab.cpachecker.util.AbstractStates.extractStateByType;
import static org.sosy_lab.cpachecker.util.automaton.AutomatonGraphmlCommon.SINK_NODE_ID;

import com.google.common.base.Function;
import com.google.common.base.Joiner;
import com.google.common.base.Preconditions;
import com.google.common.base.Predicate;
import com.google.common.base.Predicates;
import com.google.common.collect.Collections2;
import com.google.common.collect.FluentIterable;
import com.google.common.collect.HashMultimap;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableListMultimap;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Iterables;
import com.google.common.collect.Iterators;
import com.google.common.collect.LinkedHashMultimap;
import com.google.common.collect.Multimap;
import com.google.common.collect.Multimaps;
import com.google.common.collect.SetMultimap;
import com.google.common.collect.Sets;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.Deque;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.ListIterator;
import java.util.Map;
import java.util.NavigableSet;
import java.util.NoSuchElementException;
import java.util.Objects;
import java.util.Optional;
import java.util.OptionalInt;
import java.util.Set;
import java.util.TreeSet;
import java.util.function.BiPredicate;
import java.util.logging.Level;
import java.util.stream.Collectors;
import org.checkerframework.checker.nullness.qual.Nullable;
import org.sosy_lab.common.log.LogManager;
import org.sosy_lab.cpachecker.cfa.CFA;
import org.sosy_lab.cpachecker.cfa.ast.AExpression;
import org.sosy_lab.cpachecker.cfa.ast.AExpressionStatement;
import org.sosy_lab.cpachecker.cfa.ast.AFunctionCall;
import org.sosy_lab.cpachecker.cfa.ast.AFunctionCallAssignmentStatement;
import org.sosy_lab.cpachecker.cfa.ast.AIdExpression;
import org.sosy_lab.cpachecker.cfa.ast.ASimpleDeclaration;
import org.sosy_lab.cpachecker.cfa.ast.AStatement;
import org.sosy_lab.cpachecker.cfa.ast.FileLocation;
import org.sosy_lab.cpachecker.cfa.ast.c.CExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CExpressionStatement;
import org.sosy_lab.cpachecker.cfa.ast.c.CIdExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CSimpleDeclaration;
import org.sosy_lab.cpachecker.cfa.model.AStatementEdge;
import org.sosy_lab.cpachecker.cfa.model.AssumeEdge;
import org.sosy_lab.cpachecker.cfa.model.BlankEdge;
import org.sosy_lab.cpachecker.cfa.model.CFAEdge;
import org.sosy_lab.cpachecker.cfa.model.CFAEdgeType;
import org.sosy_lab.cpachecker.cfa.model.CFALabelNode;
import org.sosy_lab.cpachecker.cfa.model.CFANode;
import org.sosy_lab.cpachecker.cfa.model.FunctionCallEdge;
import org.sosy_lab.cpachecker.cfa.model.FunctionEntryNode;
import org.sosy_lab.cpachecker.cfa.model.FunctionExitNode;
import org.sosy_lab.cpachecker.cfa.model.FunctionReturnEdge;
import org.sosy_lab.cpachecker.cfa.postprocessing.global.CFACloner;
import org.sosy_lab.cpachecker.core.algorithm.fault_localization.by_unsatisfiability.trace_formula.TraceFormula.PostCondition;
import org.sosy_lab.cpachecker.core.algorithm.fault_localization.by_unsatisfiability.trace_formula.TraceFormula.PreCondition;
import org.sosy_lab.cpachecker.core.counterexample.CExpressionToOrinalCodeVisitor;
import org.sosy_lab.cpachecker.core.counterexample.CFAEdgeWithAdditionalInfo;
import org.sosy_lab.cpachecker.core.counterexample.CFAEdgeWithAssumptions;
import org.sosy_lab.cpachecker.core.counterexample.CounterexampleInfo;
import org.sosy_lab.cpachecker.core.interfaces.Targetable.TargetInformation;
import org.sosy_lab.cpachecker.cpa.acsl.ACSLState;
import org.sosy_lab.cpachecker.cpa.arg.ARGState;
import org.sosy_lab.cpachecker.cpa.arg.witnessexport.TransitionCondition.Scope;
import org.sosy_lab.cpachecker.cpa.predicate.PredicateAbstractState;
import org.sosy_lab.cpachecker.cpa.threading.ThreadingState;
import org.sosy_lab.cpachecker.cpa.threading.ThreadingTransferRelation;
import org.sosy_lab.cpachecker.util.AbstractStates;
import org.sosy_lab.cpachecker.util.BiPredicates;
import org.sosy_lab.cpachecker.util.CFATraversal;
import org.sosy_lab.cpachecker.util.CFATraversal.CFAVisitor;
import org.sosy_lab.cpachecker.util.CFATraversal.TraversalProcess;
import org.sosy_lab.cpachecker.util.CFAUtils;
import org.sosy_lab.cpachecker.util.NumericIdProvider;
import org.sosy_lab.cpachecker.util.Pair;
import org.sosy_lab.cpachecker.util.automaton.AutomatonGraphmlCommon;
import org.sosy_lab.cpachecker.util.automaton.AutomatonGraphmlCommon.AssumeCase;
import org.sosy_lab.cpachecker.util.automaton.AutomatonGraphmlCommon.KeyDef;
import org.sosy_lab.cpachecker.util.automaton.AutomatonGraphmlCommon.NodeFlag;
import org.sosy_lab.cpachecker.util.automaton.AutomatonGraphmlCommon.WitnessType;
import org.sosy_lab.cpachecker.util.automaton.VerificationTaskMetaData;
import org.sosy_lab.cpachecker.util.expressions.ExpressionTree;
import org.sosy_lab.cpachecker.util.expressions.ExpressionTreeFactory;
import org.sosy_lab.cpachecker.util.expressions.ExpressionTrees;
import org.sosy_lab.cpachecker.util.expressions.LeafExpression;
import org.sosy_lab.cpachecker.util.expressions.Or;
import org.sosy_lab.cpachecker.util.expressions.Simplifier;
import org.sosy_lab.cpachecker.util.faultlocalization.Fault;
import org.sosy_lab.cpachecker.util.faultlocalization.FaultLocalizationInfo;

class WitnessFactory implements EdgeAppender {

  private static final EnumSet<KeyDef> INSUFFICIENT_KEYS =
      EnumSet.of(
          KeyDef.SOURCECODE,
          KeyDef.STARTLINE,
          KeyDef.ENDLINE,
          KeyDef.ORIGINFILE,
          KeyDef.OFFSET,
          KeyDef.ENDOFFSET,
          KeyDef.LINECOLS,
          KeyDef.ASSUMPTIONSCOPE,
          KeyDef.ASSUMPTIONRESULTFUNCTION,
          KeyDef.THREADNAME);

  private static ARGState getCoveringState(ARGState pChild) {
    ARGState child = pChild;
    // The child might be covered by another state
    // --> switch to the covering state
    if (child.isCovered()) {
      child = child.getCoveringState();
      assert !child.isCovered();
    }
    return child;
  }

  private static boolean isTmpVariable(AIdExpression exp) {
    return exp.getDeclaration().getQualifiedName().toUpperCase().contains("__CPACHECKER_TMP");
  }

  private final WitnessOptions witnessOptions;
  private final CFA cfa;
  private final LogManager logger;
  private final VerificationTaskMetaData verificationTaskMetaData;

  private final ExpressionTreeFactory<Object> factory;
  private final Simplifier<Object> simplifier;

  private final SetMultimap<String, NodeFlag> nodeFlags = LinkedHashMultimap.create();
  private final Multimap<String, TargetInformation> violatedProperties = HashMultimap.create();
  private final Map<DelayedAssignmentsKey, CFAEdgeWithAssumptions> delayedAssignments =
      new HashMap<>();

  private final Multimap<String, Edge> leavingEdges = LinkedHashMultimap.create();
  private final Multimap<String, Edge> enteringEdges = LinkedHashMultimap.create();

  private final Map<String, ExpressionTree<Object>> stateInvariants = new LinkedHashMap<>();
  private final Map<String, ExpressionTree<Object>> stateQuasiInvariants = new LinkedHashMap<>();
  private final Map<String, String> stateScopes = new LinkedHashMap<>();
  private final Set<String> invariantExportStates = new TreeSet<>();

  private final Map<Edge, CFANode> loopHeadEnteringEdges = new HashMap<>();
  private final Multimap<String, ARGState> stateToARGStates = LinkedHashMultimap.create();
  private final Multimap<Edge, CFAEdge> edgeToCFAEdges = LinkedHashMultimap.create();

  private final String defaultSourcefileName;
  private final WitnessType graphType;

  private final InvariantProvider invariantProvider;

  private final Map<CFAEdge, LoopEntryInfo> loopEntryInfoMemo = new HashMap<>();
  private final Map<CFANode, Boolean> loopProximityMemo = new HashMap<>();

  private final NumericIdProvider numericThreadIdProvider = NumericIdProvider.create();

  private boolean isFunctionScope = false;
  private final Multimap<String, ASimpleDeclaration> seenDeclarations = HashMultimap.create();
  protected Set<AdditionalInfoConverter> additionalInfoConverters = ImmutableSet.of();

  // used for witness reduction with fault localization
  // ignored if empty
  private final Set<CFAEdge> edgesInFault = new HashSet<>();

  WitnessFactory(
      WitnessOptions pOptions,
      CFA pCfa,
      LogManager pLogger,
      VerificationTaskMetaData pMetaData,
      ExpressionTreeFactory<Object> pFactory,
      Simplifier<Object> pSimplifier,
      @Nullable String pDefaultSourceFileName,
      WitnessType pGraphType,
      InvariantProvider pInvariantProvider) {
    witnessOptions = pOptions;
    cfa = pCfa;
    logger = pLogger;
    verificationTaskMetaData = pMetaData;
    factory = pFactory;
    simplifier = pSimplifier;
    defaultSourcefileName = pDefaultSourceFileName;
    graphType = pGraphType;
    invariantProvider = pInvariantProvider;
  }

  @Override
  public void appendNewEdge(
      String pFrom,
      final String pTo,
      final CFAEdge pEdge,
      final Optional<Collection<ARGState>> pFromState,
      final Multimap<ARGState, CFAEdgeWithAssumptions> pValueMap,
      final CFAEdgeWithAdditionalInfo pAdditionalInfo)
      throws InterruptedException {

    attemptSwitchToFunctionScope(pEdge);
    if (pFromState.isPresent()) {
      stateToARGStates.putAll(pFrom, pFromState.orElseThrow());
    }

    Collection<TransitionCondition> transitions =
        constructTransitionCondition(pFrom, pTo, pEdge, pFromState, pValueMap, pAdditionalInfo);

    String from = pFrom;
    Iterator<TransitionCondition> transitionIterator = transitions.iterator();

    // If we go to the sink anyway, we can ignore any extra transitions
    if (nodeFlags.get(pTo).contains(NodeFlag.ISSINKNODE)) {
      transitionIterator = Iterators.limit(transitionIterator, 1);
    }

    int i = 0;
    while (transitionIterator.hasNext()) {
      TransitionCondition transition = transitionIterator.next();
      String to =
          transitionIterator.hasNext()
              ? String.format("%s_to_%s_intermediate-%d", pFrom, pTo, i)
              : pTo;
      Edge edge = new Edge(from, to, transition);
      if (i == 0) {
        if (transition.getMapping().containsKey(KeyDef.ENTERLOOPHEAD)) {
          Optional<CFANode> loopHead = entersLoop(pEdge, false);
          if (loopHead.isPresent()) {
            loopHeadEnteringEdges.put(edge, loopHead.orElseThrow());
          }
        }
        if (graphType != WitnessType.VIOLATION_WITNESS) {
          ExpressionTree<Object> invariant = ExpressionTrees.getTrue();
          boolean exportInvariant = exportInvariant(pEdge, pFromState);
          if (exportInvariant) {
            invariantExportStates.add(to);
          }
          if (exportInvariant || isEdgeIrrelevant(edge)) {
            invariant =
                simplifier.simplify(invariantProvider.provideInvariantFor(pEdge, pFromState));
          }
          addToStateInvariant(pTo, invariant);
          String functionName = pEdge.getSuccessor().getFunctionName();
          stateScopes.put(pTo, isFunctionScope ? functionName : "");
        }
      }

      putEdge(edge);
      edgeToCFAEdges.put(edge, pEdge);
      from = to;
      ++i;
    }
  }

  @Override
  public void appendNewEdgeToSink(
      String pFrom,
      CFAEdge pEdge,
      Optional<Collection<ARGState>> pFromState,
      Multimap<ARGState, CFAEdgeWithAssumptions> pValueMap,
      CFAEdgeWithAdditionalInfo pAdditionalInfo)
      throws InterruptedException {
    appendNewEdge(pFrom, SINK_NODE_ID, pEdge, pFromState, pValueMap, pAdditionalInfo);
  }

  private void attemptSwitchToFunctionScope(CFAEdge pEdge) {
    if (isFunctionScope) {
      return;
    }
    if (AutomatonGraphmlCommon.isMainFunctionEntry(pEdge)) {
      isFunctionScope = true;
    }
  }

  /**
   * build a transition-condition for the given edge, i.e. collect all important data and store it
   * in the new transition-condition.
   */
  private Collection<TransitionCondition> constructTransitionCondition(
      final String pFrom,
      final String pTo,
      final CFAEdge pEdge,
      final Optional<Collection<ARGState>> pFromState,
      final Multimap<ARGState, CFAEdgeWithAssumptions> pValueMap,
      final CFAEdgeWithAdditionalInfo pAdditionalInfo) {

    if (handleAsEpsilonEdge(pEdge, pAdditionalInfo)) {
      return Collections.singletonList(TransitionCondition.empty());
    }

    boolean goesToSink = pTo.equals(SINK_NODE_ID);

    if (!goesToSink && AutomatonGraphmlCommon.isSplitAssumption(pEdge)) {
      return Collections.singletonList(TransitionCondition.empty());
    }

    boolean isDefaultCase = AutomatonGraphmlCommon.isDefaultCase(pEdge);

    TransitionCondition result =
        getSourceCodeGuards(pEdge, goesToSink, isDefaultCase, Optional.empty(), pAdditionalInfo);

    if (pFromState.isPresent()) {
      return extractTransitionForStates(
          pFrom,
          pTo,
          pEdge,
          pFromState.orElseThrow(),
          pValueMap,
          pAdditionalInfo,
          result,
          goesToSink,
          isDefaultCase);
    }
    return Collections.singletonList(result);
  }

  /**
   * Check whether edge should absence at witness or not
   *
   * @param pEdge edge to be checked
   * @param pAdditionalInfo additional info corresponds to edge
   * @return true is edge considered as absence
   */
  protected boolean handleAsEpsilonEdge(CFAEdge pEdge, CFAEdgeWithAdditionalInfo pAdditionalInfo) {
    return !isFunctionScope || AutomatonGraphmlCommon.handleAsEpsilonEdge(pEdge);
  }

  private TransitionCondition getSourceCodeGuards(
      CFAEdge pEdge,
      boolean pGoesToSink,
      boolean pIsDefaultCase,
      Optional<String> pAlternativeFunctionEntry,
      CFAEdgeWithAdditionalInfo pAdditionalInfo) {
    TransitionCondition result = TransitionCondition.empty();

    if (entersLoop(pEdge, false).isPresent()) {
      result = result.putAndCopy(KeyDef.ENTERLOOPHEAD, "true");
    }

    if (witnessOptions.exportFunctionCallsAndReturns()) {
      String functionName = pAlternativeFunctionEntry.orElse(null);
      CFANode succ = pEdge.getSuccessor();
      if (succ instanceof FunctionEntryNode) {
        functionName = ((FunctionEntryNode) succ).getFunctionDefinition().getOrigName();
      } else if (AutomatonGraphmlCommon.isMainFunctionEntry(pEdge)) {
        functionName = succ.getFunctionName();
      }
      if (functionName != null) {
        result = result.putAndCopy(KeyDef.FUNCTIONENTRY, functionName);
      }
    }

    if (witnessOptions.exportFunctionCallsAndReturns()
        && pEdge.getSuccessor() instanceof FunctionExitNode) {
      FunctionEntryNode entryNode = ((FunctionExitNode) pEdge.getSuccessor()).getEntryNode();
      String functionName = entryNode.getFunctionDefinition().getOrigName();
      result = result.putAndCopy(KeyDef.FUNCTIONEXIT, functionName);
    }

    if (pEdge instanceof AssumeEdge
        && !AutomatonGraphmlCommon.isPartOfTerminatingAssumption(pEdge)) {
      AssumeEdge assumeEdge = (AssumeEdge) pEdge;
      // Check if the assume edge is an artificial edge introduced for pointer-calls
      if (AutomatonGraphmlCommon.isPointerCallAssumption(assumeEdge)) {
        // If the assume edge is followed by a pointer call,
        // the assumption is artificial and should not be exported
        if (!pGoesToSink && isEmptyTransitionPossible(pAdditionalInfo)) {
          // remove all info from transitionCondition
          return TransitionCondition.empty();
        } else if (assumeEdge.getTruthAssumption()
            && witnessOptions.exportFunctionCallsAndReturns()) {
          // However, if we know that the function is not going to be called,
          // this information may be valuable and can be exported
          // by creating a transition for the function call, to the sink:
          FunctionCallEdge callEdge =
              Iterables.getOnlyElement(
                  CFAUtils.leavingEdges(assumeEdge.getSuccessor()).filter(FunctionCallEdge.class));
          FunctionEntryNode in = callEdge.getSuccessor();
          result = result.putAndCopy(KeyDef.FUNCTIONENTRY, in.getFunctionName());
        }
      } else if (witnessOptions.exportAssumeCaseInfo()) {
        // Do not export assume-case information for the assume edges
        // representing continuations of switch-case chains
        if (assumeEdge.getTruthAssumption()
            || pGoesToSink
            || (pIsDefaultCase && !pGoesToSink)
            || !AutomatonGraphmlCommon.isPartOfSwitchStatement(assumeEdge)) {
          AssumeCase assumeCase =
              (assumeEdge.getTruthAssumption() != assumeEdge.isSwapped())
                  ? AssumeCase.THEN
                  : AssumeCase.ELSE;
          result = result.putAndCopy(KeyDef.CONTROLCASE, assumeCase.toString());
        } else {
          if (isEmptyTransitionPossible(pAdditionalInfo)) {
            return TransitionCondition.empty();
          }
        }
      }
    }

    final Set<FileLocation> locations =
        AutomatonGraphmlCommon.getFileLocationsFromCfaEdge(
            pEdge, cfa.getMainFunction(), pAdditionalInfo);
    // TODO This ignores file names, does this make sense? Replace with natural order?
    final Comparator<FileLocation> nodeOffsetComparator =
        Comparator.comparingInt(FileLocation::getNodeOffset);
    final FileLocation min =
        locations.isEmpty() ? null : Collections.min(locations, nodeOffsetComparator);
    final FileLocation max =
        locations.isEmpty() ? null : Collections.max(locations, nodeOffsetComparator);

    if (witnessOptions.exportLineNumbers() && min != null) {
      if (witnessOptions.exportSourceFileName()
          || !min.getFileName().toString().equals(defaultSourcefileName)) {
        result = result.putAndCopy(KeyDef.ORIGINFILE, min.getFileName().toString());
      }
      result = result.putAndCopy(KeyDef.STARTLINE, Integer.toString(min.getStartingLineInOrigin()));
    }
    if (witnessOptions.exportLineNumbers() && max != null) {
      result = result.putAndCopy(KeyDef.ENDLINE, Integer.toString(max.getEndingLineInOrigin()));
    }

    if (witnessOptions.exportOffset() && min != null && min.isOffsetRelatedToOrigin()) {
      if (witnessOptions.exportSourceFileName()
          || !min.getFileName().toString().equals(defaultSourcefileName)) {
        result = result.putAndCopy(KeyDef.ORIGINFILE, min.getFileName().toString());
      }
      result = result.putAndCopy(KeyDef.OFFSET, Integer.toString(min.getNodeOffset()));
    }
    if (witnessOptions.exportOffset() && max != null && max.isOffsetRelatedToOrigin()) {
      result =
          result.putAndCopy(
              KeyDef.ENDOFFSET, Integer.toString(max.getNodeOffset() + max.getNodeLength() - 1));
    }

    if (witnessOptions.exportSourcecode()) {
      String sourceCode;
      if (pIsDefaultCase && !pGoesToSink) {
        sourceCode = "default:";
      } else {
        sourceCode = pEdge.getRawStatement().trim();
      }
      if (sourceCode.isEmpty()
          && !isEmptyTransitionPossible(pAdditionalInfo)
          && pEdge instanceof FunctionReturnEdge) {
        sourceCode = ((FunctionReturnEdge) pEdge).getSummaryEdge().getRawStatement().trim();
      }

      if (!sourceCode.isEmpty()) {
        result = result.putAndCopy(KeyDef.SOURCECODE, sourceCode);
      }
    }

    return result;
  }

  /**
   * Method is used for additional check if TransitionCondition.empty() is applicable.
   *
   * @param pAdditionalInfo is used at {@link ExtendedWitnessFactory}
   * @return true if TransitionCondition.empty is applicable.
   */
  protected boolean isEmptyTransitionPossible(CFAEdgeWithAdditionalInfo pAdditionalInfo) {
    return true;
  }

  protected Collection<TransitionCondition> extractTransitionForStates(
      final String pFrom,
      final String pTo,
      final CFAEdge pEdge,
      final Collection<ARGState> pFromStates,
      final Multimap<ARGState, CFAEdgeWithAssumptions> pValueMap,
      final CFAEdgeWithAdditionalInfo pAdditionalInfo,
      final TransitionCondition pTransitionCondition,
      final boolean pGoesToSink,
      final boolean pIsDefaultCase) {
    TransitionCondition result = pTransitionCondition;

    List<ExpressionTree<Object>> code = new ArrayList<>();
    Optional<AIdExpression> resultVariable = Optional.empty();
    Optional<String> resultFunction = Optional.empty();
    String functionName = pEdge.getPredecessor().getFunctionName();
    boolean functionScope = isFunctionScope;

    for (ARGState state : pFromStates) {

      CFAEdgeWithAssumptions cfaEdgeWithAssignments =
          getEdgeWithAssignments(pFrom, pEdge, state, pValueMap);

      if (cfaEdgeWithAssignments != null) {
        ImmutableList<AExpressionStatement> assignments = cfaEdgeWithAssignments.getExpStmts();
        Predicate<AExpressionStatement> assignsParameterOfOtherFunction =
            new AssignsParameterOfOtherFunction(pEdge);
        ImmutableList<AExpressionStatement> functionValidAssignments =
            from(assignments).filter(assignsParameterOfOtherFunction).toList();

        if (functionValidAssignments.size() < assignments.size()) {
          cfaEdgeWithAssignments =
              new CFAEdgeWithAssumptions(
                  pEdge, functionValidAssignments, cfaEdgeWithAssignments.getComment());
          FluentIterable<CFAEdge> nextEdges = CFAUtils.leavingEdges(pEdge.getSuccessor());

          if (nextEdges.size() == 1 && state.getChildren().size() == 1) {
            String keyFrom = pTo;
            CFAEdge keyEdge = Iterables.getOnlyElement(nextEdges);
            ARGState keyState = Iterables.getOnlyElement(state.getChildren());
            ImmutableList<AExpressionStatement> valueAssignments =
                from(assignments).filter(Predicates.not(assignsParameterOfOtherFunction)).toList();
            CFAEdgeWithAssumptions valueCFAEdgeWithAssignments =
                new CFAEdgeWithAssumptions(keyEdge, valueAssignments, "");
            delayedAssignments.put(
                new DelayedAssignmentsKey(keyFrom, keyEdge, keyState), valueCFAEdgeWithAssignments);
          }
        }

        // try to get a proper function name for special variables
        Optional<String> extractedFunctionName =
            extractFunctionNameOfStaticVariables(functionValidAssignments);
        if (extractedFunctionName.isPresent()) {
          functionName = extractedFunctionName.orElseThrow();
          functionScope = true;
        }

        assignments = getAssignments(cfaEdgeWithAssignments, null);

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
              if (isTmpVariable(idExpression)) {
                // get only assignments without nested tmpVariables (except self)
                assignments = getAssignments(cfaEdgeWithAssignments, idExpression);
                resultVariable = Optional.of(idExpression);
                AIdExpression resultFunctionName =
                    (AIdExpression)
                        assignment.getFunctionCallExpression().getFunctionNameExpression();
                if (resultFunctionName.getDeclaration() != null) {
                  resultFunction = Optional.of(resultFunctionName.getDeclaration().getOrigName());
                } else {
                  resultFunction = Optional.of(resultFunctionName.getName());
                }
              }
            }
          }
        }
        assert resultVariable.isPresent() == resultFunction.isPresent();

        if (!assignments.isEmpty()) {
          Collection<AExpression> expressions =
              assignments.stream()
                  .map(AExpressionStatement::getExpression)
                  .collect(Collectors.toCollection(ArrayDeque::new));

          // Determine the scope of this CFA edge and remove all expressions
          // that are ambiguous.
          Scope scope =
              filterExpressionsForScope(pEdge, result.getScope(), functionName, expressions);
          result = result.withScope(scope);

          if (!expressions.isEmpty()) {
            code.add(
                factory.and(
                    Collections2.transform(expressions, pExpr -> LeafExpression.of(pExpr))));
          }
        }
      }
    }

    if (graphType != WitnessType.CORRECTNESS_WITNESS
        && witnessOptions.exportAssumptions()
        && !code.isEmpty()) {
      final String assumptionCode = getAssumptionAsCode(factory.or(code), resultVariable);
      result = result.putAndCopy(KeyDef.ASSUMPTION, assumptionCode + ";");
      if (functionScope) {
        if (witnessOptions.revertThreadFunctionRenaming()) {
          functionName = CFACloner.extractFunctionName(functionName);
        }
        // TODO we cannot access the original function name here and only export the
        // CPAchecker-specific name. This works for tasks with a single source file.
        result = result.putAndCopy(KeyDef.ASSUMPTIONSCOPE, functionName);
      }
      if (resultFunction.isPresent()) {
        result = result.putAndCopy(KeyDef.ASSUMPTIONRESULTFUNCTION, resultFunction.orElseThrow());
      }
    }

    result = addAdditionalInfo(result, pAdditionalInfo);

    // TODO: For correctness witnesses, there may be multiple (disjoint) states for one location
    // available; it is not clear how we should handle thread information there.
    if (witnessOptions.exportThreadId() && pFromStates.size() == 1) {
      ARGState state = pFromStates.iterator().next();
      result = exportThreadId(result, pEdge, state);
      return exportThreadManagement(
          result, pEdge, state, pGoesToSink, pIsDefaultCase, pAdditionalInfo);
    }

    return Collections.singleton(result);
  }

  /**
   * Extract all assignments from the given edge. Remove all assignments using tmp variables.
   *
   * @param toIgnore a tmp variable that will not be removed.
   */
  private ImmutableList<AExpressionStatement> getAssignments(
      CFAEdgeWithAssumptions cfaEdgeWithAssignments, @Nullable AIdExpression toIgnore) {
    // Do not export our own temporary variables
    Predicate<CIdExpression> isGoodVariable = v -> !isTmpVariable(v) || v.equals(toIgnore);
    ImmutableList.Builder<AExpressionStatement> assignments = ImmutableList.builder();
    for (AExpressionStatement s : cfaEdgeWithAssignments.getExpStmts()) {
      if (s.getExpression() instanceof CExpression
          && CFAUtils.getIdExpressionsOfExpression((CExpression) s.getExpression())
              .allMatch(isGoodVariable)) {
        assignments.add(s);
      }
    }
    return assignments.build();
  }

  /** Determine the scope for static local variables. */
  private Optional<String> extractFunctionNameOfStaticVariables(
      Collection<AExpressionStatement> functionValidAssignments) {
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
            return Optional.of(qualified.substring(0, qualified.indexOf("::")));
            // TODO fast return or loop over all elements?
          }
        }
      }
    }
    return Optional.empty();
  }

  private CFAEdgeWithAssumptions getEdgeWithAssignments(
      final String pFrom,
      final CFAEdge pEdge,
      ARGState state,
      final Multimap<ARGState, CFAEdgeWithAssumptions> pValueMap) {

    final DelayedAssignmentsKey key = new DelayedAssignmentsKey(pFrom, pEdge, state);
    final CFAEdgeWithAssumptions currentEdgeWithAssignments =
        getFromValueMap(pValueMap, state, pEdge);
    CFAEdgeWithAssumptions cfaEdgeWithAssignments = delayedAssignments.get(key);

    if (pValueMap != null && currentEdgeWithAssignments != null) {
      if (cfaEdgeWithAssignments == null) {
        cfaEdgeWithAssignments = currentEdgeWithAssignments;
      } else {
        // if there is a delayed assignment, merge with it.
        ImmutableList.Builder<AExpressionStatement> allAssignments = ImmutableList.builder();
        allAssignments.addAll(cfaEdgeWithAssignments.getExpStmts());
        allAssignments.addAll(currentEdgeWithAssignments.getExpStmts());
        cfaEdgeWithAssignments =
            new CFAEdgeWithAssumptions(
                pEdge, allAssignments.build(), currentEdgeWithAssignments.getComment());
      }
    }
    return cfaEdgeWithAssignments;
  }

  private String getAssumptionAsCode(
      ExpressionTree<Object> assumption, Optional<AIdExpression> resultVariable) {
    final CExpressionToOrinalCodeVisitor transformer =
        resultVariable.isPresent()
            ? CExpressionToOrinalCodeVisitor.BASIC_TRANSFORMER.substitute(
                (CIdExpression) resultVariable.orElseThrow(), "\\result")
            : CExpressionToOrinalCodeVisitor.BASIC_TRANSFORMER;
    final Function<Object, String> converter =
        pLeafExpression -> {
          if (pLeafExpression instanceof CExpression) {
            return ((CExpression) pLeafExpression).accept(transformer);
          }
          if (pLeafExpression == null) {
            return "(0)";
          }
          return pLeafExpression.toString();
        };
    // If there are only conjunctions, use multiple statements
    // instead of the "&&" operator that is harder to parse.
    if (ExpressionTrees.isAnd(assumption)) {
      return Joiner.on("; ")
          .join(
              ExpressionTrees.getChildren(assumption)
                  .transform(pTree -> ExpressionTrees.convert(pTree, converter)));
    } else {
      return ExpressionTrees.convert(assumption, converter).toString();
    }
  }

  private Scope filterExpressionsForScope(
      CFAEdge pEdge, Scope pScope, String pFunctionName, Collection<AExpression> pExpressions) {

    Scope scope = pScope;
    Optional<String> scopeFunctionName =
        isFunctionScope ? Optional.of(pFunctionName) : Optional.empty();

    // Extend the "scope" by the declarations on the current CFA edge
    Set<ASimpleDeclaration> declarations =
        FluentIterable.from(CFAUtils.getAstNodesFromCfaEdge(pEdge))
            .filter(ASimpleDeclaration.class)
            .toSet();
    for (ASimpleDeclaration declaration : declarations) {
      String ambiguousName = getAmbiguousName(declaration, scopeFunctionName);
      seenDeclarations.put(ambiguousName, declaration);
    }
    Optional<Scope> extendedScope = scope.extendBy(scopeFunctionName, declarations);

    if (extendedScope.isPresent()) {
      scope = extendedScope.orElseThrow();
      Iterator<AExpression> expressionIt = pExpressions.iterator();

      // For each expression, check if it can be added unambiguously within the scope
      while (expressionIt.hasNext()) {
        declarations =
            CFAUtils.traverseRecursively(expressionIt.next())
                .filter(AIdExpression.class)
                .transform(AIdExpression::getDeclaration)
                .filter(ASimpleDeclaration.class)
                .toSet();
        boolean containsAmbiguousVariables = false;
        for (ASimpleDeclaration expressionDeclaration : declarations) {
          String ambiguousName = getAmbiguousName(expressionDeclaration, scopeFunctionName);
          if (seenDeclarations.get(ambiguousName).stream()
                  .anyMatch(decl -> !decl.equals(expressionDeclaration))
              && !scope.getUsedDeclarations().contains(expressionDeclaration)) {
            containsAmbiguousVariables = true;
            break;
          }
        }

        if (!containsAmbiguousVariables) {
          extendedScope = scope.extendBy(scopeFunctionName, declarations);
          if (extendedScope.isPresent()) {
            scope = extendedScope.orElseThrow();
          } else {
            expressionIt.remove();
          }
        } else {
          expressionIt.remove();
        }
      }
    } else {
      pExpressions.clear();
    }
    return scope;
  }

  /**
   * Overwritten at {@link ExtendedWitnessFactory}
   *
   * @param pCondition current {@link TransitionCondition}
   * @param pAdditionalInfo exported additional info
   * @return TransitionCondition with additional info
   */
  protected TransitionCondition addAdditionalInfo(
      TransitionCondition pCondition, CFAEdgeWithAdditionalInfo pAdditionalInfo) {
    return pCondition;
  }

  /**
   * export the id of the executed thread into the witness. We assume that the edge can be assigned
   * to exactly one thread.
   */
  private TransitionCondition exportThreadId(
      TransitionCondition pResult, final CFAEdge pEdge, ARGState pState) {
    ThreadingState threadingState = extractStateByType(pState, ThreadingState.class);
    if (threadingState != null) {
      for (String threadId : threadingState.getThreadIds()) {
        if (threadingState
            .getThreadLocation(threadId)
            .getLocationNode()
            .equals(pEdge.getPredecessor())) {
          if (witnessOptions.exportThreadName()) {
            pResult = pResult.putAndCopy(KeyDef.THREADNAME, threadId);
          }
          pResult =
              pResult.putAndCopy(KeyDef.THREADID, Integer.toString(getUniqueThreadNum(threadId)));
          break;
        }
      }
    }
    return pResult;
  }

  private Collection<TransitionCondition> exportThreadManagement(
      TransitionCondition pResult,
      final CFAEdge pEdge,
      ARGState pState,
      boolean pGoesToSink,
      boolean pIsDefaultCase,
      CFAEdgeWithAdditionalInfo pAdditionalInfo) {

    ThreadingState threadingState = extractStateByType(pState, ThreadingState.class);

    if (threadingState == null) {
      // no data available
      return Collections.singletonList(pResult);
    }

    // handle direct creation or destruction of threads
    Optional<String> threadInitialFunctionName = Optional.empty();
    OptionalInt spawnedThreadId = OptionalInt.empty();

    if (pEdge.getEdgeType() == CFAEdgeType.StatementEdge) {
      AStatement statement = ((AStatementEdge) pEdge).getStatement();
      if (statement instanceof AFunctionCall) {
        AExpression functionNameExp =
            ((AFunctionCall) statement).getFunctionCallExpression().getFunctionNameExpression();
        if (functionNameExp instanceof AIdExpression) {
          final String functionName = ((AIdExpression) functionNameExp).getName();
          switch (functionName) {
            case ThreadingTransferRelation.THREAD_START:
              {
                Optional<ARGState> possibleChild =
                    pState.getChildren().stream()
                        .filter(c -> pEdge == pState.getEdgeToChild(c))
                        .findFirst();
                if (!possibleChild.isPresent()) {
                  // this can happen e.g. if the ARG was not discovered completely.
                  return Collections.singletonList(pResult);
                }
                ARGState child = possibleChild.orElseThrow();
                // search the new created thread-id
                ThreadingState succThreadingState = extractStateByType(child, ThreadingState.class);
                for (String threadId : succThreadingState.getThreadIds()) {
                  if (!threadingState.getThreadIds().contains(threadId)) {
                    // we found the new created thread-id. we assume there is only 'one' match
                    spawnedThreadId = OptionalInt.of(getUniqueThreadNum(threadId));
                    pResult =
                        pResult.putAndCopy(
                            KeyDef.CREATETHREAD, Integer.toString(spawnedThreadId.orElseThrow()));
                    String calledFunctionName =
                        succThreadingState
                            .getThreadLocation(threadId)
                            .getLocationNode()
                            .getFunction()
                            .getOrigName();
                    threadInitialFunctionName = Optional.of(calledFunctionName);
                  }
                }
                break;
              }
            default:
              // nothing to do
          }
        }
      }
    }

    ImmutableList.Builder<TransitionCondition> result = ImmutableList.builder();
    result.add(pResult);

    // enter function of newly created thread
    if (threadInitialFunctionName.isPresent()) {
      TransitionCondition extraTransition =
          getSourceCodeGuards(
              pEdge, pGoesToSink, pIsDefaultCase, threadInitialFunctionName, pAdditionalInfo);
      if (spawnedThreadId.isPresent()) {
        extraTransition =
            extraTransition.putAndCopy(
                KeyDef.THREADID, Integer.toString(spawnedThreadId.orElseThrow()));
      }

      if (!extraTransition.getMapping().isEmpty()) {
        result.add(extraTransition);
      }
    }

    return result.build();
  }

  private int getUniqueThreadNum(String threadId) {
    return numericThreadIdProvider.provideNumericId(threadId);
  }

  /**
   * Starting from the given initial ARG state, collects that state and all transitive successors
   * (as defined by the successor function) that are children of their direct predecessor and are
   * accepted by the path state predicate.
   *
   * @param pInitialState the initial ARG state.
   * @param pSuccessorFunction the function defining the successors of a state.
   * @param pPathStates a filter on the nodes.
   * @param pIsRelevantEdge a filter on the successor function.
   * @return the parents with their children.
   */
  private Iterable<ARGState> collectReachableNodes(
      final ARGState pInitialState,
      final Function<? super ARGState, ? extends Iterable<ARGState>> pSuccessorFunction,
      final Predicate<? super ARGState> pPathStates,
      final BiPredicate<ARGState, ARGState> pIsRelevantEdge) {
    return Iterables.transform(
        collectReachableEdges(pInitialState, pSuccessorFunction, pPathStates, pIsRelevantEdge),
        Pair::getFirst);
  }

  /**
   * Starting from the given initial ARG state, collects that state and all transitive successors
   * (as defined by the successor function) that are children of their direct predecessor. Children
   * are only computed for nodes that are accepted by the path state predicate.
   *
   * @param pInitialState the initial ARG state.
   * @param pSuccessorFunction the function defining the successors of a state.
   * @param pPathStates a filter on the parent nodes.
   * @param pIsRelevantEdge a filter on the successor function.
   * @return the parents with their children.
   */
  private Iterable<Pair<ARGState, Iterable<ARGState>>> collectReachableEdges(
      final ARGState pInitialState,
      final Function<? super ARGState, ? extends Iterable<ARGState>> pSuccessorFunction,
      final Predicate<? super ARGState> pPathStates,
      final BiPredicate<ARGState, ARGState> pIsRelevantEdge) {
    return new Iterable<>() {

      private final Set<ARGState> visited = new HashSet<>();

      private final Deque<ARGState> waitlist = new ArrayDeque<>();

      {
        waitlist.add(pInitialState);
        visited.add(pInitialState);
      }

      @Override
      public Iterator<Pair<ARGState, Iterable<ARGState>>> iterator() {
        return new Iterator<>() {

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
                FluentIterable.of(parent)
                    .transformAndConcat(pSuccessorFunction)
                    .transform(WitnessFactory::getCoveringState)
                    .filter(parent.getChildren()::contains);

            // Only the children on the path become parents themselves
            for (ARGState child : children.filter(pPathStates)) {
              if (pIsRelevantEdge.test(parent, child) && visited.add(child)) {
                waitlist.offer(child);
              }
            }

            return Pair.of(parent, children);
          }

          @Override
          public void remove() {
            throw new UnsupportedOperationException("Removal not supported.");
          }
        };
      }
    };
  }

  /** Creates a {@link Witness} using the supplied parameters */
  public Witness produceWitness(
      final ARGState pRootState,
      final Predicate<? super ARGState> pIsRelevantState,
      final BiPredicate<ARGState, ARGState> pIsRelevantEdge,
      final Predicate<? super ARGState> pIsCyclehead,
      final Optional<Function<? super ARGState, ExpressionTree<Object>>> cycleHeadToQuasiInvariant,
      Optional<CounterexampleInfo> pCounterExample,
      GraphBuilder pGraphBuilder)
      throws InterruptedException {

    // reset information in case data structures where filled before:
    leavingEdges.clear();
    enteringEdges.clear();
    nodeFlags.clear();
    violatedProperties.clear();
    stateInvariants.clear();
    stateQuasiInvariants.clear();
    stateScopes.clear();
    invariantExportStates.clear();
    stateToARGStates.clear();
    edgeToCFAEdges.clear();
    edgesInFault.clear();

    BiPredicate<ARGState, ARGState> isRelevantEdge = pIsRelevantEdge;
    Multimap<ARGState, CFAEdgeWithAssumptions> valueMap = ImmutableListMultimap.of();
    Map<ARGState, CFAEdgeWithAdditionalInfo> additionalInfo = getAdditionalInfo(pCounterExample);
    additionalInfoConverters = getAdditionalInfoConverters(pCounterExample);

    if (pCounterExample.isPresent()) {
      CounterexampleInfo cex = pCounterExample.orElseThrow();
      if (cex.isPreciseCounterExample()) {
        valueMap =
            Multimaps.transformValues(
                pCounterExample.orElseThrow().getExactVariableValues(),
                WitnessAssumptionFilter::filterRelevantAssumptions);
        if (cex instanceof FaultLocalizationInfo) {
          FaultLocalizationInfo fInfo = (FaultLocalizationInfo) cex;
          List<Fault> faults = fInfo.getRankedList();
          if (!faults.isEmpty()) {
            Fault bestFault = faults.get(0);
            FluentIterable.from(bestFault)
                .transform(fc -> fc.correspondingEdge())
                .copyInto(edgesInFault);
            if (fInfo.getPostcondition().isPresent()) {
              PostCondition postCondition = fInfo.getPostcondition().orElseThrow();
              edgesInFault.addAll(postCondition.getIgnoredEdges());
              edgesInFault.addAll(postCondition.responsibleEdges());
            }
            if (fInfo.getPrecondition().isPresent()) {
              PreCondition preCondition = fInfo.getPrecondition().orElseThrow();
              edgesInFault.addAll(preCondition.responsibleEdges());
            }
            valueMap = Multimaps.filterValues(valueMap, v -> edgesInFault.contains(v.getCFAEdge()));
          }
        }
      } else {
        isRelevantEdge = BiPredicates.bothSatisfy(pIsRelevantState);
      }
    }

    final String entryStateNodeId = pGraphBuilder.getId(pRootState);

    // Collect node flags in advance
    for (ARGState s :
        collectReachableNodes(
            pRootState, ARGState::getChildren, pIsRelevantState, isRelevantEdge)) {
      String sourceStateNodeId = pGraphBuilder.getId(s);
      EnumSet<NodeFlag> sourceNodeFlags = EnumSet.noneOf(NodeFlag.class);
      if (sourceStateNodeId.equals(entryStateNodeId)) {
        sourceNodeFlags.add(NodeFlag.ISENTRY);
      }
      if (pIsCyclehead.apply(s)) {
        sourceNodeFlags.add(NodeFlag.ISCYCLEHEAD);
        if (cycleHeadToQuasiInvariant.isPresent()) {
          stateQuasiInvariants.put(
              sourceStateNodeId, cycleHeadToQuasiInvariant.orElseThrow().apply(s));
        }
      }
      sourceNodeFlags.addAll(extractNodeFlags(s));
      nodeFlags.putAll(sourceStateNodeId, sourceNodeFlags);
      if (graphType == WitnessType.VIOLATION_WITNESS) {
        violatedProperties.putAll(sourceStateNodeId, extractViolatedProperties(s));
      }
    }
    // Write the sink node
    nodeFlags.put(SINK_NODE_ID, NodeFlag.ISSINKNODE);

    // Build the actual graph
    pGraphBuilder.buildGraph(
        pRootState,
        pIsRelevantState,
        isRelevantEdge,
        valueMap,
        additionalInfo,
        collectReachableEdges(pRootState, ARGState::getChildren, pIsRelevantState, isRelevantEdge),
        this);

    // remove unnecessary edges leading to sink
    removeUnnecessarySinkEdges();

    // Merge nodes with empty or repeated edges
    int sizeBeforeMerging = edgeToCFAEdges.size();
    mergeRepeatedEdges(entryStateNodeId);
    int sizeAfterMerging = edgeToCFAEdges.size();
    logger.logf(
        Level.ALL,
        "Witness graph shrinked from %s edges to %s edges when merging edges.",
        sizeBeforeMerging,
        sizeAfterMerging);

    // merge redundant sibling edges leading to the sink together, if possible
    mergeRedundantSinkEdges();

    return new Witness(
        graphType,
        defaultSourcefileName,
        cfa,
        verificationTaskMetaData,
        entryStateNodeId,
        leavingEdges,
        enteringEdges,
        witnessOptions,
        nodeFlags,
        violatedProperties,
        stateInvariants,
        stateQuasiInvariants,
        stateScopes,
        invariantExportStates,
        stateToARGStates,
        edgeToCFAEdges);
  }

  /**
   * This method applies a fixed-point algorithm to shrink the ARG-based graph into a (much) smaller
   * witness graph, i.e., we compute an abstraction of the ARG-based graph without redundant or
   * irrelevant information.
   */
  private void mergeRepeatedEdges(final String entryStateNodeId) throws InterruptedException {
    NavigableSet<Edge> waitlist = new TreeSet<>(leavingEdges.values());
    while (!waitlist.isEmpty()) {
      Edge edge = waitlist.pollFirst();
      // If the edge still exists in the graph and is irrelevant, remove it
      if (leavingEdges.get(edge.getSource()).contains(edge) && isEdgeIrrelevant(edge)) {
        Iterables.addAll(waitlist, mergeNodes(edge));
        assert leavingEdges.isEmpty() || leavingEdges.containsKey(entryStateNodeId);
      }
      setLoopHeadInvariantIfApplicable(edge.getTarget());
    }
  }

  /**
   * Getter for additional information. Overwritten at {@link ExtendedWitnessFactory}
   *
   * @param pCounterExample current {@link CounterexampleInfo}
   * @return additional information
   */
  protected Map<ARGState, CFAEdgeWithAdditionalInfo> getAdditionalInfo(
      Optional<CounterexampleInfo> pCounterExample) {
    return ImmutableMap.of();
  }

  /**
   * Getter of {@link AdditionalInfoConverter}. Overwritten at {@link ExtendedWitnessFactory}
   *
   * @param pCounterExample current {@link CounterexampleInfo}
   * @return set of InfoConverters
   */
  protected Set<AdditionalInfoConverter> getAdditionalInfoConverters(
      Optional<CounterexampleInfo> pCounterExample) {
    return ImmutableSet.of();
  }

  /**
   * Remove edges that lead to the sink but have a sibling edge that has the same label.
   *
   * <p>We additionally remove irrelevant edges. This is needed for concurrency witnesses at
   * thread-creation.
   */
  private void removeUnnecessarySinkEdges() {
    final Collection<Edge> toRemove = Sets.newIdentityHashSet();
    for (Collection<Edge> leavingEdgesCollection : leavingEdges.asMap().values()) {
      for (Edge edge : leavingEdgesCollection) {
        if (edge.getTarget().equals(SINK_NODE_ID)) {
          for (Edge otherEdge : leavingEdgesCollection) {
            // ignore the edge itself, as well as already handled edges.
            if (edge != otherEdge && !toRemove.contains(otherEdge)) {
              // remove edges with either identical labels or irrelevant edge-transition
              if (edge.getLabel().equals(otherEdge.getLabel()) || isEdgeIrrelevant(edge)) {
                toRemove.add(edge);
                break;
              }
            }
          }
        }
      }
    }
    for (Edge edge : toRemove) {
      boolean removed = removeEdge(edge);
      assert removed;
    }
  }

  /** Merge sibling edges (with the same source) that lead to the sink if possible. */
  private void mergeRedundantSinkEdges() {
    for (Collection<Edge> leavingEdgesCollection : leavingEdges.asMap().values()) {
      // We only need to do something if we have siblings
      if (leavingEdgesCollection.size() > 1) {

        // Determine all siblings that go to the sink
        List<Edge> toSink =
            leavingEdgesCollection.stream()
                .filter(e -> e.getTarget().equals(SINK_NODE_ID))
                .collect(Collectors.toCollection(ArrayList::new));

        // If multiple siblings go to the sink, we want to try to merge them
        if (toSink.size() > 1) {

          ListIterator<Edge> edgeToSinkIterator = toSink.listIterator();
          Set<Edge> removed = Sets.newIdentityHashSet();
          while (edgeToSinkIterator.hasNext()) {
            Edge edge = edgeToSinkIterator.next();

            // If the edge has already been marked as removed, throw it out
            if (removed.contains(edge)) {
              edgeToSinkIterator.remove();
              continue;
            }

            // Search a viable merge partner for the current edge
            Optional<Edge> merged = Optional.empty();
            Edge other = null;
            for (Edge otherEdge : toSink) {
              if (edge != otherEdge && !removed.contains(otherEdge)) {
                merged = edge.tryMerge(otherEdge);
                if (merged.isPresent()) {
                  other = otherEdge;
                  break;
                }
              }
            }

            // If we determined a merge partner, apply the merge result
            if (merged.isPresent()) {
              // Remove the two merge partners
              removeEdge(edge);
              removeEdge(other);

              // Directly remove the old version of the current edge
              // and mark the other edge as removed
              edgeToSinkIterator.remove();
              removed.add(other);

              // Add the merged edge to the graph
              putEdge(merged.orElseThrow());
              edgeToCFAEdges.putAll(merged.orElseThrow(), edgeToCFAEdges.get(edge));
              edgeToCFAEdges.putAll(merged.orElseThrow(), edgeToCFAEdges.get(other));
              edgeToCFAEdges.removeAll(edge);
              edgeToCFAEdges.removeAll(other);

              // Add the merged edge to the set of siblings to consider it for further merges
              edgeToSinkIterator.add(merged.orElseThrow());
              edgeToSinkIterator.previous();
            }
          }
        }
      }
    }
  }

  private void setLoopHeadInvariantIfApplicable(String pTarget) throws InterruptedException {
    if (!ExpressionTrees.getTrue().equals(getStateInvariant(pTarget))) {
      return;
    }
    ExpressionTree<Object> loopHeadInvariant = ExpressionTrees.getFalse();
    String scope = null;
    for (Edge enteringEdge : enteringEdges.get(pTarget)) {
      if (enteringEdge.getLabel().getMapping().containsKey(KeyDef.ENTERLOOPHEAD)) {
        CFANode loopHead = loopHeadEnteringEdges.get(enteringEdge);
        if (loopHead != null) {
          String functionName = loopHead.getFunctionName();
          if (scope == null) {
            scope = functionName;
          } else if (!scope.equals(functionName)) {
            return;
          }
          if (witnessOptions.produceInvariantWitnesses()) {
            // First, collect all invariants
            List<ExpressionTree<Object>> iterableList = new ArrayList<>();
            for (CFAEdge enteringCFAEdge : CFAUtils.enteringEdges(loopHead)) {
              iterableList.add(
                  invariantProvider.provideInvariantFor(enteringCFAEdge, Optional.empty()));
            }
            // Next,compute the invariant as disjunction
            loopHeadInvariant = computeInvariantForInvariantWitnesses(iterableList);
          } else {
            for (CFAEdge enteringCFAEdge : CFAUtils.enteringEdges(loopHead)) {
              loopHeadInvariant =
                  Or.of(
                      loopHeadInvariant,
                      invariantProvider.provideInvariantFor(enteringCFAEdge, Optional.empty()));
            }
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
      getStateScopes().put(pTarget, scope);
    }
  }

  /**
   * Computes a disjunction of the invariant present, where true is ignored. If all elements are
   * true, true is returned
   *
   * @param expressions a collection of expressions
   * @return the disjunction
   */
  private ExpressionTree<Object> computeInvariantForInvariantWitnesses(
      Iterable<ExpressionTree<Object>> expressions) {
    ExpressionTree<Object> mergedInvariant = ExpressionTrees.getFalse();
    boolean trueSkipped = false;
    for (ExpressionTree<Object> invariant : expressions) {

      if (ExpressionTrees.getTrue().equals(invariant)) {
        trueSkipped = true;
      } else {
        mergedInvariant = Or.of(mergedInvariant, invariant);
      }
    }
    // In case there is only a true present and skipped and no other invariant is present,
    // we would add false.
    // Hence, we replace false by true
    if (trueSkipped && ExpressionTrees.getFalse().equals(mergedInvariant)) {
      mergedInvariant = ExpressionTrees.getTrue();
    }
    return mergedInvariant;
  }

  private boolean hasFlagsOrProperties(String pNode) {
    return !nodeFlags.get(pNode).isEmpty() || !violatedProperties.get(pNode).isEmpty();
  }

  /**
   * this predicate marks intermediate nodes that do not contain relevant information and can
   * therefore be shortcut.
   */
  private boolean isIrrelevantNode(String pNode) {
    if (!ExpressionTrees.getTrue().equals(getStateInvariant(pNode))) {
      return false;
    }
    if (hasFlagsOrProperties(pNode)) {
      return false;
    }
    if (enteringEdges.get(pNode).isEmpty()) {
      return false;
    }
    for (Edge edge : enteringEdges.get(pNode)) {
      if (!edge.getLabel().getMapping().isEmpty()) {
        return false;
      }
    }
    return true;
  }

  private boolean isEdgeIrrelevantByFaultLocalization(Edge pEdge) {
    if (edgesInFault.isEmpty()) {
      return false;
    }
    // has to be calculated newly everytime because of adaptions to edgeToCFAEdges
    // finds all outgoing edges to sinks for relevant nodes
    Set<String> importantNodes =
        transformedImmutableSetCopy(
            Multimaps.filterValues(edgeToCFAEdges, cfaEdge -> edgesInFault.contains(cfaEdge))
                .keySet(),
            e -> e.getSource());

    // not irrelevant if it is an edge to a sink node and the source node is part of the fault
    if (pEdge.getTarget().equals(SINK_NODE_ID) && importantNodes.contains(pEdge.getSource())) {
      return false;
    }

    // not irrelevant if pEdge maps to an edge containing an edge in fault
    for (CFAEdge cfaEdge : edgeToCFAEdges.get(pEdge)) {
      if (edgesInFault.contains(cfaEdge)) {
        return false;
      }
    }

    return true;
  }

  /**
   * this predicate marks intermediate edges that do not contain relevant information and can
   * therefore be shortcut.
   */
  private boolean isEdgeIrrelevant(Edge pEdge) {
    final String source = pEdge.getSource();
    final String target = pEdge.getTarget();
    final TransitionCondition label = pEdge.getLabel();

    if (isIrrelevantNode(target) || isEdgeIrrelevantByFaultLocalization(pEdge)) {
      return true;
    }

    final ExpressionTree<Object> sourceInv = stateQuasiInvariants.get(source);
    final ExpressionTree<Object> targetInv = stateQuasiInvariants.get(target);
    if (sourceInv != null && targetInv != null && !sourceInv.equals(targetInv)) {
      return false;
    }

    if (label.getMapping().isEmpty()) {
      return true;
    }

    if (source.equals(target)) {
      return false;
    }

    // An edge is never irrelevant if there are conflicting scopes
    ExpressionTree<Object> sourceTree = getStateInvariant(source);
    if (sourceTree != null) {
      String sourceScope = stateScopes.get(source);
      String targetScope = stateScopes.get(target);
      if (sourceScope != null && targetScope != null && !sourceScope.equals(targetScope)) {
        return false;
      }
    }

    // An edge is irrelevant if it is the only leaving edge of a
    // node and it is empty or all its non-assumption contents
    // are summarized by a preceding edge
    boolean summarizedByPreceedingEdge =
        Iterables.any(
            enteringEdges.get(source),
            pPrecedingEdge -> pPrecedingEdge.getLabel().summarizes(label));

    if ((!label.hasTransitionRestrictions()
            || summarizedByPreceedingEdge
            || (label.getMapping().size() == 1
                && label.getMapping().containsKey(KeyDef.FUNCTIONEXIT)))
        && (leavingEdges.get(source).size() == 1)) {
      return true;
    }

    if (Iterables.all(
        leavingEdges.get(source),
        pLeavingEdge -> !pLeavingEdge.getLabel().hasTransitionRestrictions())) {
      return true;
    }

    // Some keys are not sufficient to limit the explored state space,
    // i.e., by cutting off branching control flow.
    // They are only a weak hint on the analysis direction.
    // We remove edges that only contain such insufficient keys.
    if (witnessOptions.removeInsufficientEdges()
        && INSUFFICIENT_KEYS.containsAll(label.getMapping().keySet())) {
      return true;
    }

    return false;
  }

  /**
   * Merge two consecutive nodes into one new node, if the edge between the nodes is irrelevant. The
   * merge also merges the information of the nodes, e.g. disjuncts their invariants.
   *
   * @return replacement edges that should be (re-)visited for potential further merges.
   */
  private Iterable<Edge> mergeNodes(final Edge pEdge) {
    Preconditions.checkArgument(isEdgeIrrelevant(pEdge));

    // Always merge into the predecessor, unless the successor is the sink
    boolean intoPredecessor =
        !nodeFlags.get(pEdge.getTarget()).equals(EnumSet.of(NodeFlag.ISSINKNODE));
    final String nodeToKeep = intoPredecessor ? pEdge.getSource() : pEdge.getTarget();
    final String nodeToRemove = intoPredecessor ? pEdge.getTarget() : pEdge.getSource();

    if (nodeToKeep.equals(nodeToRemove)) {
      removeEdge(pEdge);
      return Iterables.concat(leavingEdges.get(nodeToKeep), enteringEdges.get(nodeToKeep));
    }

    if (invariantExportStates.remove(nodeToRemove)) {
      invariantExportStates.add(nodeToKeep);
    }

    // Merge the flags
    nodeFlags.putAll(nodeToKeep, nodeFlags.removeAll(nodeToRemove));

    // Merge the trees
    mergeExpressionTreesIntoFirst(nodeToKeep, nodeToRemove);

    // Merge quasi invariant
    if (witnessOptions.produceInvariantWitnesses()) {
      mergeQuasiInvariantForInvariantWitness(nodeToKeep, nodeToRemove);
    } else {
      mergeQuasiInvariant(nodeToKeep, nodeToRemove);
    }

    // Merge the violated properties
    violatedProperties.putAll(nodeToKeep, violatedProperties.removeAll(nodeToRemove));

    // Merge mapping
    stateToARGStates.putAll(nodeToKeep, stateToARGStates.removeAll(nodeToRemove));

    Set<Edge> replacementEdges = new LinkedHashSet<>();

    // Move the leaving edges
    Collection<Edge> leavingEdgesToMove = ImmutableList.copyOf(leavingEdges.get(nodeToRemove));
    // Create the replacement edges,
    // Add them as leaving edges to the source node,
    // Add them as entering edges to their target nodes
    for (Edge leavingEdge : leavingEdgesToMove) {
      if (!pEdge.equals(leavingEdge)) {
        TransitionCondition label = pEdge.getLabel();
        // Don't give function-exit transitions labels from preceding transitions
        if (leavingEdge.getLabel().getMapping().containsKey(KeyDef.FUNCTIONEXIT)) {
          label = TransitionCondition.empty();
        }
        // Don't merge "originfile" tag if leavingEdge corresponds to default originfile
        if (leavingEdge.getLabel().getMapping().containsKey(KeyDef.SOURCECODE)) {
          label = label.removeAndCopy(KeyDef.ORIGINFILE);
        }
        label = label.putAllAndCopy(leavingEdge.getLabel());
        Edge replacementEdge = new Edge(nodeToKeep, leavingEdge.getTarget(), label);
        putEdge(replacementEdge);
        edgeToCFAEdges.putAll(replacementEdge, edgeToCFAEdges.get(leavingEdge));
        edgeToCFAEdges.removeAll(leavingEdge);
        replacementEdges.add(replacementEdge);
        CFANode loopHead = loopHeadEnteringEdges.get(leavingEdge);
        if (loopHead != null) {
          loopHeadEnteringEdges.remove(leavingEdge);
          loopHeadEnteringEdges.put(replacementEdge, loopHead);
        }
      }
    }
    // Remove the old edges from their successors
    for (Edge leavingEdge : leavingEdgesToMove) {
      boolean removed = removeEdge(leavingEdge);
      edgeToCFAEdges.removeAll(leavingEdge);
      assert removed;
    }

    // Move the entering edges
    Collection<Edge> enteringEdgesToMove = ImmutableList.copyOf(enteringEdges.get(nodeToRemove));
    // Create the replacement edges,
    // Add them as entering edges to the source node,
    // Add add them as leaving edges to their source nodes
    for (Edge enteringEdge : enteringEdgesToMove) {
      if (!pEdge.equals(enteringEdge)) {
        TransitionCondition label = pEdge.getLabel().putAllAndCopy(enteringEdge.getLabel());
        Edge replacementEdge = new Edge(enteringEdge.getSource(), nodeToKeep, label);
        putEdge(replacementEdge);
        edgeToCFAEdges.putAll(replacementEdge, edgeToCFAEdges.get(pEdge));
        replacementEdges.add(replacementEdge);
        CFANode loopHead = loopHeadEnteringEdges.get(enteringEdge);
        if (loopHead != null) {
          loopHeadEnteringEdges.remove(enteringEdge);
          loopHeadEnteringEdges.put(replacementEdge, loopHead);
        }
      }
    }

    // Remove the old edges from their predecessors
    for (Edge enteringEdge : enteringEdgesToMove) {
      boolean removed = removeEdge(enteringEdge);
      edgeToCFAEdges.removeAll(enteringEdge);
      assert removed : "could not remove edge: " + enteringEdge;
    }
    edgeToCFAEdges.removeAll(pEdge);

    return replacementEdges;
  }

  /** Merge two expressionTrees for source and target. */
  private void mergeExpressionTreesIntoFirst(final String source, final String target) {
    ExpressionTree<Object> sourceTree = getStateInvariant(source);
    ExpressionTree<Object> targetTree = getStateInvariant(target);
    String sourceScope = stateScopes.get(source);
    String targetScope = stateScopes.get(target);

    if (!ExpressionTrees.getTrue().equals(targetTree)
        && ExpressionTrees.getTrue().equals(sourceTree)
        && (sourceScope == null || sourceScope.equals(targetScope))
        && enteringEdges.get(source).size() <= 1) {
      ExpressionTree<Object> newSourceTree = ExpressionTrees.getFalse();
      if (witnessOptions.produceInvariantWitnesses()) {
        newSourceTree =
            computeInvariantForInvariantWitnesses(
                transformedImmutableListCopy(
                    enteringEdges.get(source), e -> getStateInvariant(e.getSource())));
      } else {
        for (Edge e : enteringEdges.get(source)) {
          newSourceTree = factory.or(newSourceTree, getStateInvariant(e.getSource()));
        }
      }
      newSourceTree = simplifier.simplify(factory.and(targetTree, newSourceTree));
      stateInvariants.put(source, newSourceTree);
      sourceTree = newSourceTree;
    }

    final String newScope;
    if (ExpressionTrees.isConstant(sourceTree) || Objects.equals(sourceScope, targetScope)) {
      newScope = targetScope;
    } else if (ExpressionTrees.isConstant(targetTree)) {
      newScope = sourceScope;
    } else {
      newScope = null;
    }
    ExpressionTree<Object> newTree = mergeStateInvariantsIntoFirst(source, target);
    if (newTree != null) {
      if (newScope == null && !ExpressionTrees.isConstant(newTree)) {
        addToStateInvariant(source, ExpressionTrees.getTrue());
        stateScopes.remove(source);
      } else {
        stateScopes.put(source, nullToEmpty(newScope));
      }
    }
  }

  private void mergeQuasiInvariant(final String pNodeToKeep, final String pNodeToRemove) {
    ExpressionTree<Object> fromToKeep = getQuasiInvariant(pNodeToKeep);
    ExpressionTree<Object> fromToRemove = getQuasiInvariant(pNodeToRemove);

    fromToKeep = factory.or(fromToKeep, fromToRemove);
    if (!ExpressionTrees.getFalse().equals(fromToKeep)) {
      stateQuasiInvariants.put(pNodeToKeep, fromToKeep);
    }
  }

  /** If one of the two nodes has a quasi invariant that is 'true', the other invariant is used */
  private void mergeQuasiInvariantForInvariantWitness(
      final String pNodeToKeep, final String pNodeToRemove) {
    ExpressionTree<Object> fromToKeep = getQuasiInvariant(pNodeToKeep);
    ExpressionTree<Object> fromToRemove = getQuasiInvariant(pNodeToRemove);
    if (ExpressionTrees.getTrue().equals(fromToKeep)) {
      stateQuasiInvariants.put(pNodeToKeep, fromToRemove);
    } else if (ExpressionTrees.getTrue().equals(fromToRemove)) {
      stateQuasiInvariants.put(pNodeToKeep, fromToKeep);
    } else {

      fromToKeep = factory.or(fromToKeep, fromToRemove);
      if (!ExpressionTrees.getFalse().equals(fromToKeep)) {
        stateQuasiInvariants.put(pNodeToKeep, fromToKeep);
      }
    }
  }

  private void putEdge(Edge pEdge) {
    assert leavingEdges.size() == enteringEdges.size();
    assert !pEdge.getSource().equals(SINK_NODE_ID)
        : "unexpected edge added to witness (edge should not start with SINK): " + pEdge;
    leavingEdges.put(pEdge.getSource(), pEdge);
    enteringEdges.put(pEdge.getTarget(), pEdge);
    assert leavingEdges.size() == enteringEdges.size();
  }

  private boolean removeEdge(Edge pEdge) {
    assert leavingEdges.size() == enteringEdges.size();
    if (leavingEdges.remove(pEdge.getSource(), pEdge)) {
      boolean alsoRemoved = enteringEdges.remove(pEdge.getTarget(), pEdge);
      assert alsoRemoved : "edge was not removed: " + pEdge;
      assert leavingEdges.size() == enteringEdges.size();
      assert nodeFlags.get(pEdge.getTarget()).contains(NodeFlag.ISENTRY)
          || !enteringEdges.get(pEdge.getTarget()).isEmpty()
          || leavingEdges.get(pEdge.getTarget()).isEmpty();

      return true;
    }
    return false;
  }

  private Collection<NodeFlag> extractNodeFlags(ARGState pState) {
    if (pState.isTarget() && graphType != WitnessType.CORRECTNESS_WITNESS) {
      return Collections.singleton(NodeFlag.ISVIOLATION);
    }
    return ImmutableSet.of();
  }

  private Collection<TargetInformation> extractViolatedProperties(ARGState pState) {
    List<TargetInformation> result = new ArrayList<>();
    if (pState.isTarget()) {
      result.addAll(pState.getTargetInformation());
    }
    return result;
  }

  /**
   * Records the given invariant for the given state.
   *
   * <p>If no invariant is present for this state, the given invariant is the new state invariant.
   * Otherwise, the new state invariant is a disjunction of the previous and the given invariant.
   *
   * <p>However, if no invariants are ever added for a state, it is assumed to have the invariant
   * "true".
   *
   * @param pStateId the state id.
   * @param pValue the invariant to be added.
   */
  private void addToStateInvariant(String pStateId, ExpressionTree<Object> pValue) {
    ExpressionTree<Object> prev = stateInvariants.get(pStateId);
    if (prev == null) {
      stateInvariants.put(pStateId, simplifier.simplify(pValue));
      return;
    }
    ExpressionTree<Object> result = simplifier.simplify(factory.or(prev, pValue));
    stateInvariants.put(pStateId, result);
  }

  /**
   * Merges the invariants for the given state ids and stores it as the new invariant for the first
   * of the given ids.
   *
   * @param pStateId the state id.
   * @param pOtherStateId the other state id.
   * @return the merged invariant. {@code null} if neither state had an invariant.
   */
  private @Nullable ExpressionTree<Object> mergeStateInvariantsIntoFirst(
      String pStateId, String pOtherStateId) {
    ExpressionTree<Object> prev = stateInvariants.get(pStateId);
    ExpressionTree<Object> other = stateInvariants.get(pOtherStateId);
    if (prev == null && other == null) {
      stateInvariants.put(pStateId, ExpressionTrees.getTrue());
      return ExpressionTrees.getTrue();
    } else if (prev == null || other == null) {
      ExpressionTree<Object> existingTree = (prev == null) ? other : prev;
      stateInvariants.put(pStateId, existingTree);
      return existingTree;
    }
    ExpressionTree<Object> result = null;
    if (witnessOptions.produceInvariantWitnesses()) {
      if (ExpressionTrees.getTrue().equals(prev)) {
        result = simplifier.simplify(other);
      } else if (ExpressionTrees.getTrue().equals(other)) {
        result = simplifier.simplify(prev);
      } else {
        result = simplifier.simplify(factory.or(prev, other));
      }
    } else {
      result = simplifier.simplify(factory.or(prev, other));
    }

    stateInvariants.put(pStateId, result);
    return result;
  }

  private boolean exportInvariant(CFAEdge pEdge, Optional<Collection<ARGState>> pFromState) {
    if (pFromState.isPresent()
        && pFromState.orElseThrow().stream()
            .map(AbstractStates.toState(PredicateAbstractState.class))
            .filter(s -> s != null)
            .anyMatch(PredicateAbstractState::containsAbstractionState)) {
      return true;
    }
    if (pFromState.isPresent()
        && pFromState.orElseThrow().stream()
            .map(AbstractStates.toState(ACSLState.class))
            .filter(s -> s != null)
            .anyMatch(ACSLState::hasAnnotations)) {
      return true;
    }
    if (AutomatonGraphmlCommon.handleAsEpsilonEdge(pEdge)) {
      return false;
    }
    if (entersLoop(pEdge).isPresent()) {
      return true;
    }

    CFANode referenceNode = pEdge.getSuccessor();

    // Check if either the reference node or any of its direct predecessors via assume edges are in
    // loop proximity
    return FluentIterable.concat(
            Collections.singleton(referenceNode),
            CFAUtils.enteringEdges(referenceNode)
                .filter(AssumeEdge.class)
                .transform(CFAEdge::getPredecessor))
        .anyMatch(n -> isInLoopProximity(n));
  }

  /**
   * From given node, backward via non-assume edges until a loop head is found.
   *
   * @param pReferenceNode the node to start the search from.
   * @return {@code true} if a loop head is found, {@code false} otherwise.
   */
  private boolean isInLoopProximity(CFANode pReferenceNode) {

    Deque<List<CFANode>> waitlist = new ArrayDeque<>();
    Set<CFANode> visited = new HashSet<>();
    waitlist.push(ImmutableList.of(pReferenceNode));
    visited.add(pReferenceNode);

    Predicate<CFAEdge> epsilonEdge = edge -> !(edge instanceof AssumeEdge);
    java.util.function.Predicate<CFANode> loopProximity = pNode -> pNode.isLoopStart();
    if (cfa.getAllLoopHeads().isPresent()) {
      loopProximity =
          loopProximity.and(pNode -> cfa.getAllLoopHeads().orElseThrow().contains(pNode));
    }
    while (!waitlist.isEmpty()) {
      List<CFANode> current = waitlist.pop();
      CFANode currentNode = current.get(current.size() - 1);
      Boolean memoized = loopProximityMemo.get(currentNode);
      if (memoized != null && !memoized) {
        continue;
      }
      if ((memoized != null && memoized) || loopProximity.test(currentNode)) {
        for (CFANode onTrace : current) {
          loopProximityMemo.put(onTrace, true);
        }
        return true;
      }
      // boolean isFirst = true;
      for (CFAEdge enteringEdge : CFAUtils.enteringEdges(currentNode).filter(epsilonEdge)) {
        CFANode predecessor = enteringEdge.getPredecessor();
        if (visited.add(predecessor)) {
          waitlist.push(ImmutableList.<CFANode>builder().addAll(current).add(predecessor).build());
          // isFirst = false;
        }
      }
    }

    for (CFANode v : visited) {
      loopProximityMemo.put(v, false);
    }

    return false;
  }

  private Optional<CFANode> entersLoop(CFAEdge pEdge) {
    return entersLoop(pEdge, true);
  }

  private Optional<CFANode> entersLoop(CFAEdge pEdge, boolean pAllowGoto) {
    class EnterLoopVisitor implements CFAVisitor {

      private final Collection<CFAEdge> previouslyChecked = new ArrayList<>();

      private LoopEntryInfo loopEntryInfo = new LoopEntryInfo();

      @Override
      public TraversalProcess visitNode(CFANode pNode) {
        LoopEntryInfo loopEntryInformation = loopEntryInfoMemo.get(pEdge);
        if (loopEntryInformation != null) {
          loopEntryInfo = loopEntryInformation;
          return TraversalProcess.ABORT;
        }
        if (pNode.isLoopStart()) {
          boolean gotoLoop = false;
          if (pNode instanceof CFALabelNode) {
            CFALabelNode node = (CFALabelNode) pNode;
            for (BlankEdge e : CFAUtils.enteringEdges(pNode).filter(BlankEdge.class)) {
              if (e.getDescription().equals("Goto: " + node.getLabel())) {
                gotoLoop = true;
                break;
              }
            }
          }
          loopEntryInfo = new LoopEntryInfo(pNode, gotoLoop);
          loopEntryInfoMemo.put(pEdge, loopEntryInfo);

          return TraversalProcess.ABORT;
        }
        if (pNode.getNumLeavingEdges() > 1) {
          return TraversalProcess.ABORT;
        }
        previouslyChecked.add(pEdge);
        return TraversalProcess.CONTINUE;
      }

      @Override
      public TraversalProcess visitEdge(CFAEdge pCfaEdge) {
        return AutomatonGraphmlCommon.handleAsEpsilonEdge(pCfaEdge)
            ? TraversalProcess.CONTINUE
            : TraversalProcess.SKIP;
      }
    }
    EnterLoopVisitor enterLoopVisitor = new EnterLoopVisitor();
    CFATraversal.dfs()
        .ignoreFunctionCalls()
        .ignoreSummaryEdges()
        .traverse(pEdge.getSuccessor(), enterLoopVisitor);

    LoopEntryInfo loopEntryInfo = enterLoopVisitor.loopEntryInfo;

    for (CFAEdge e : enterLoopVisitor.previouslyChecked) {
      loopEntryInfoMemo.put(e, loopEntryInfo);
    }

    if (!loopEntryInfo.entersLoop() || (loopEntryInfo.isGotoLoop() && !pAllowGoto)) {
      return Optional.empty();
    }

    return Optional.ofNullable(loopEntryInfo.loopHead);
  }

  private static @Nullable CFAEdgeWithAssumptions getFromValueMap(
      Multimap<ARGState, CFAEdgeWithAssumptions> pValueMap, ARGState pState, CFAEdge pEdge) {
    Iterable<CFAEdgeWithAssumptions> assumptions = pValueMap.get(pState);
    assumptions = Iterables.filter(assumptions, a -> a.getCFAEdge().equals(pEdge));
    if (Iterables.isEmpty(assumptions)) {
      return null;
    }
    return Iterables.getOnlyElement(assumptions);
  }

  private static class LoopEntryInfo {

    private final @Nullable CFANode loopHead;

    private final boolean gotoLoop;

    public LoopEntryInfo() {
      this(null, false);
    }

    public LoopEntryInfo(CFANode pLoopHead, boolean pGotoLoop) {
      if (pGotoLoop) {
        Objects.requireNonNull(pLoopHead);
      }
      loopHead = pLoopHead;
      gotoLoop = pGotoLoop;
    }

    public boolean entersLoop() {
      return loopHead != null;
    }

    public CFANode getLoopHead() {
      return loopHead;
    }

    public boolean isGotoLoop() {
      return gotoLoop;
    }

    @Override
    public String toString() {
      return String.format("Loop head: %s; Goto: %s", loopHead, gotoLoop);
    }

    @Override
    public boolean equals(Object pOther) {
      if (this == pOther) {
        return true;
      }
      if (pOther instanceof LoopEntryInfo) {
        LoopEntryInfo other = (LoopEntryInfo) pOther;
        return Objects.equals(getLoopHead(), other.getLoopHead()) && gotoLoop == other.gotoLoop;
      }
      return false;
    }

    @Override
    public int hashCode() {
      return Objects.hash(loopHead, gotoLoop);
    }
  }

  private static String getAmbiguousName(
      ASimpleDeclaration pDeclaration, Optional<String> pQualifier) {
    String ambiguousName = pDeclaration.getOrigName();
    if (!pQualifier.isPresent()) {
      return ambiguousName;
    }
    return pQualifier.orElseThrow() + "::" + pDeclaration.getOrigName();
  }

  private ExpressionTree<Object> getStateInvariant(String pStateId) {
    ExpressionTree<Object> result = stateInvariants.get(pStateId);
    if (result == null) {
      return ExpressionTrees.getTrue();
    }
    return result;
  }

  private ExpressionTree<Object> getQuasiInvariant(final String pNodeId) {
    ExpressionTree<Object> result = stateQuasiInvariants.get(pNodeId);
    if (result == null) {
      return ExpressionTrees.getFalse();
    }
    return result;
  }

  private Map<String, String> getStateScopes() {
    return stateScopes;
  }
}
