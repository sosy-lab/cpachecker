// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2007-2020 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.cpa.value.symbolic.refiner;

import static com.google.common.base.Preconditions.checkNotNull;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Multimap;
import java.io.PrintStream;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Deque;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Optional;
import java.util.Set;
import java.util.logging.Level;
import org.sosy_lab.common.ShutdownNotifier;
import org.sosy_lab.common.configuration.Configuration;
import org.sosy_lab.common.configuration.FileOption;
import org.sosy_lab.common.configuration.FileOption.Type;
import org.sosy_lab.common.configuration.InvalidConfigurationException;
import org.sosy_lab.common.configuration.Option;
import org.sosy_lab.common.configuration.Options;
import org.sosy_lab.common.io.PathTemplate;
import org.sosy_lab.common.log.LogManager;
import org.sosy_lab.common.log.LogManagerWithoutDuplicates;
import org.sosy_lab.cpachecker.cfa.CFA;
import org.sosy_lab.cpachecker.cfa.Language;
import org.sosy_lab.cpachecker.cfa.ast.AExpressionStatement;
import org.sosy_lab.cpachecker.cfa.ast.FileLocation;
import org.sosy_lab.cpachecker.cfa.ast.c.CBinaryExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CBinaryExpression.BinaryOperator;
import org.sosy_lab.cpachecker.cfa.ast.c.CExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CExpressionStatement;
import org.sosy_lab.cpachecker.cfa.ast.c.CIdExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CSimpleDeclaration;
import org.sosy_lab.cpachecker.cfa.ast.c.CVariableDeclaration;
import org.sosy_lab.cpachecker.cfa.model.CFAEdge;
import org.sosy_lab.cpachecker.cfa.model.CFANode;
import org.sosy_lab.cpachecker.cfa.types.MachineModel;
import org.sosy_lab.cpachecker.cfa.types.c.CStorageClass;
import org.sosy_lab.cpachecker.cfa.types.c.CType;
import org.sosy_lab.cpachecker.core.CPAcheckerResult.Result;
import org.sosy_lab.cpachecker.core.counterexample.CFAEdgeWithAssumptions;
import org.sosy_lab.cpachecker.core.counterexample.CFAPathWithAssumptions;
import org.sosy_lab.cpachecker.core.counterexample.CounterexampleInfo;
import org.sosy_lab.cpachecker.core.defaults.precision.VariableTrackingPrecision;
import org.sosy_lab.cpachecker.core.interfaces.ConfigurableProgramAnalysis;
import org.sosy_lab.cpachecker.core.interfaces.Precision;
import org.sosy_lab.cpachecker.core.interfaces.Refiner;
import org.sosy_lab.cpachecker.core.interfaces.Statistics;
import org.sosy_lab.cpachecker.core.interfaces.StatisticsProvider;
import org.sosy_lab.cpachecker.core.reachedset.UnmodifiableReachedSet;
import org.sosy_lab.cpachecker.cpa.arg.ARGBasedRefiner;
import org.sosy_lab.cpachecker.cpa.arg.ARGReachedSet;
import org.sosy_lab.cpachecker.cpa.arg.ARGState;
import org.sosy_lab.cpachecker.cpa.arg.AbstractARGBasedRefiner;
import org.sosy_lab.cpachecker.cpa.arg.path.ARGPath;
import org.sosy_lab.cpachecker.cpa.arg.path.PathIterator;
import org.sosy_lab.cpachecker.cpa.constraints.ConstraintsCPA;
import org.sosy_lab.cpachecker.cpa.constraints.constraint.Constraint;
import org.sosy_lab.cpachecker.cpa.constraints.domain.ConstraintsSolver;
import org.sosy_lab.cpachecker.cpa.constraints.domain.ConstraintsState;
import org.sosy_lab.cpachecker.cpa.constraints.refiner.precision.ConstraintsPrecision;
import org.sosy_lab.cpachecker.cpa.constraints.refiner.precision.RefinableConstraintsPrecision;
import org.sosy_lab.cpachecker.cpa.value.ExpressionValueVisitor;
import org.sosy_lab.cpachecker.cpa.value.ValueAnalysisCPA;
import org.sosy_lab.cpachecker.cpa.value.ValueAnalysisState;
import org.sosy_lab.cpachecker.cpa.value.ValueAnalysisState.ValueAndType;
import org.sosy_lab.cpachecker.cpa.value.refiner.ValueAnalysisConcreteErrorPathAllocator;
import org.sosy_lab.cpachecker.cpa.value.symbolic.refiner.interpolant.SymbolicInterpolant;
import org.sosy_lab.cpachecker.cpa.value.symbolic.refiner.interpolant.SymbolicInterpolantManager;
import org.sosy_lab.cpachecker.cpa.value.symbolic.type.SymbolicIdentifier;
import org.sosy_lab.cpachecker.cpa.value.symbolic.type.SymbolicValue;
import org.sosy_lab.cpachecker.cpa.value.symbolic.util.SymbolicIdentifierLocator;
import org.sosy_lab.cpachecker.cpa.value.symbolic.util.SymbolicValues;
import org.sosy_lab.cpachecker.cpa.value.type.NumericValue;
import org.sosy_lab.cpachecker.cpa.value.type.Value;
import org.sosy_lab.cpachecker.cpa.value.type.ValueToCExpressionTransformer;
import org.sosy_lab.cpachecker.cpa.value.type.ValueVisitor;
import org.sosy_lab.cpachecker.exceptions.CPAException;
import org.sosy_lab.cpachecker.util.AbstractStates;
import org.sosy_lab.cpachecker.util.CPAs;
import org.sosy_lab.cpachecker.util.Pair;
import org.sosy_lab.cpachecker.util.refinement.FeasibilityChecker;
import org.sosy_lab.cpachecker.util.refinement.GenericPrefixProvider;
import org.sosy_lab.cpachecker.util.refinement.GenericRefiner;
import org.sosy_lab.cpachecker.util.refinement.InterpolationTree;
import org.sosy_lab.cpachecker.util.refinement.PathExtractor;
import org.sosy_lab.cpachecker.util.refinement.PathInterpolator;
import org.sosy_lab.cpachecker.util.states.MemoryLocation;
import org.sosy_lab.java_smt.api.Model.ValueAssignment;

/** Refiner for value analysis using symbolic values. */
@Options(prefix = "cpa.value.refinement")
public class SymbolicValueAnalysisRefiner
    extends GenericRefiner<ForgettingCompositeState, SymbolicInterpolant> {

  @Option(
      secure = true,
      description = "whether or not to do lazy-abstraction",
      name = "restart",
      toUppercase = true)
  private RestartStrategy restartStrategy = RestartStrategy.PIVOT;

  @Option(secure = true, description = "if this option is set to false, constraints are never kept")
  private boolean trackConstraints = true;

  @Option(
      secure = true,
      name = "pathConstraintsFile",
      description = "File to which path constraints should be written.")
  @FileOption(Type.OUTPUT_FILE)
  private PathTemplate pathConstraintsOutputFile =
      PathTemplate.ofFormatString("Counterexample.%d.symbolic-trace.txt");

  @Option(
      secure = true,
      name = "writePathConstraints",
      description =
          "Whether to write symbolic trace (including path constraints) for found erexamples")
  private boolean writePathConstraints = true;

  private SymbolicStrongestPostOperator strongestPost;
  private Precision fullPrecision;

  private ValueAnalysisConcreteErrorPathAllocator errorPathAllocator;

  private final MachineModel machineModel;

  public static Refiner create(final ConfigurableProgramAnalysis pCpa)
      throws InvalidConfigurationException {
    return AbstractARGBasedRefiner.forARGBasedRefiner(create0(pCpa), pCpa);
  }

  public static ARGBasedRefiner create0(final ConfigurableProgramAnalysis pCpa)
      throws InvalidConfigurationException {

    final ValueAnalysisCPA valueAnalysisCpa =
        CPAs.retrieveCPAOrFail(pCpa, ValueAnalysisCPA.class, SymbolicValueAnalysisRefiner.class);
    final ConstraintsCPA constraintsCpa =
        CPAs.retrieveCPAOrFail(pCpa, ConstraintsCPA.class, SymbolicValueAnalysisRefiner.class);

    final Configuration config = valueAnalysisCpa.getConfiguration();

    valueAnalysisCpa.injectRefinablePrecision();
    constraintsCpa.injectRefinablePrecision(new RefinableConstraintsPrecision(config));

    final LogManager logger = valueAnalysisCpa.getLogger();
    final CFA cfa = valueAnalysisCpa.getCFA();
    final ShutdownNotifier shutdownNotifier = valueAnalysisCpa.getShutdownNotifier();

    final ConstraintsSolver solver = constraintsCpa.getSolver();

    final SymbolicStrongestPostOperator strongestPostOperator =
        new ValueTransferBasedStrongestPostOperator(solver, logger, config, cfa);

    final SymbolicFeasibilityChecker feasibilityChecker =
        new SymbolicValueAnalysisFeasibilityChecker(strongestPostOperator, config, logger, cfa);

    final GenericPrefixProvider<ForgettingCompositeState> prefixProvider =
        new GenericPrefixProvider<>(
            strongestPostOperator,
            ForgettingCompositeState.getInitialState(cfa.getMachineModel()),
            logger,
            cfa,
            config,
            ValueAnalysisCPA.class,
            shutdownNotifier);

    final ElementTestingSymbolicEdgeInterpolator edgeInterpolator =
        new ElementTestingSymbolicEdgeInterpolator(
            feasibilityChecker,
            strongestPostOperator,
            SymbolicInterpolantManager.getInstance(),
            config,
            shutdownNotifier,
            cfa);

    final SymbolicPathInterpolator pathInterpolator =
        new SymbolicPathInterpolator(
            edgeInterpolator,
            feasibilityChecker,
            prefixProvider,
            config,
            logger,
            shutdownNotifier,
            cfa);

    return new SymbolicValueAnalysisRefiner(
        cfa,
        feasibilityChecker,
        strongestPostOperator,
        pathInterpolator,
        new PathExtractor(logger, config),
        config,
        logger);
  }

  public SymbolicValueAnalysisRefiner(
      final CFA pCfa,
      final FeasibilityChecker<ForgettingCompositeState> pFeasibilityChecker,
      final SymbolicStrongestPostOperator pStrongestPostOperator,
      final PathInterpolator<SymbolicInterpolant> pInterpolator,
      final PathExtractor pPathExtractor,
      final Configuration pConfig,
      final LogManager pLogger)
      throws InvalidConfigurationException {

    super(
        pFeasibilityChecker,
        pInterpolator,
        SymbolicInterpolantManager.getInstance(),
        pPathExtractor,
        pConfig,
        pLogger);

    pConfig.inject(this);

    strongestPost = pStrongestPostOperator;
    fullPrecision =
        VariableTrackingPrecision.createStaticPrecision(
            pConfig, pCfa.getVarClassification(), ValueAnalysisCPA.class);

    machineModel = pCfa.getMachineModel();
    errorPathAllocator =
        new ValueAnalysisConcreteErrorPathAllocator(pConfig, pLogger, machineModel);
    if (writePathConstraints && !pCfa.getLanguage().equals(Language.C)) {
      throw new InvalidConfigurationException(
          "At the moment, writing path constraints is only supported for C");
    }
  }

  @Override
  public CounterexampleInfo performRefinementForPath(
      ARGReachedSet pReached, ARGPath targetPathToUse) throws CPAException, InterruptedException {
    CounterexampleInfo info = super.performRefinementForPath(pReached, targetPathToUse);
    if (!info.isSpurious() && writePathConstraints && pathConstraintsOutputFile != null) {
      return getCexWithSymbolicInformation(info, pathConstraintsOutputFile);
    } else {
      return info;
    }
  }

  private List<Pair<ForgettingCompositeState, List<CFAEdge>>> evaluate(
      ARGPath pTargetPath, Map<SymbolicIdentifier, Value> pIdentifierAssignment)
      throws CPAException, InterruptedException {

    PathIterator fullPath = pTargetPath.fullPathIterator();
    ARGState first = pTargetPath.getFirstState();
    ValueAnalysisState firstValue =
        checkNotNull(AbstractStates.extractStateByType(first, ValueAnalysisState.class));
    ConstraintsState firstConstraints =
        checkNotNull(AbstractStates.extractStateByType(first, ConstraintsState.class));
    ForgettingCompositeState currentState =
        new ForgettingCompositeState(firstValue, firstConstraints);
    Deque<ForgettingCompositeState> callstack = new ArrayDeque<>();

    List<Pair<ForgettingCompositeState, List<CFAEdge>>> stateSequence =
        new ArrayList<>(pTargetPath.size());

    CFAEdge currentEdge;
    while (fullPath.hasNext()) {
      List<CFAEdge> intermediateEdges = new ArrayList<>();
      currentEdge = fullPath.getOutgoingEdge();
      intermediateEdges.add(currentEdge);

      Optional<ForgettingCompositeState> maybeNext =
          strongestPost.step(currentState, currentEdge, fullPrecision, callstack, pTargetPath);

      fullPath.advance();

      if (!maybeNext.isPresent()) {
        throw new CPAException(
            "Counterexample said to be feasible but spurious at edge: " + currentEdge);

      } else {
        currentState = maybeNext.orElseThrow();
        if (!pIdentifierAssignment.isEmpty()) {
          ValueAnalysisState currentValueState = currentState.getValueState();
          Set<SymbolicIdentifier> usedIdentifiers = new HashSet<>();
          for (Entry<MemoryLocation, ValueAndType> e : currentValueState.getConstants()) {
            Value v = e.getValue().getValue();
            if (v instanceof SymbolicValue) {
              usedIdentifiers.addAll(
                  ((SymbolicValue) v).accept(SymbolicIdentifierLocator.getInstance()));
            }
          }
          ExpressionValueVisitor valueVisitor =
              new ExpressionValueVisitor(
                  currentValueState,
                  currentEdge.getSuccessor().getFunctionName(),
                  machineModel,
                  new LogManagerWithoutDuplicates(logger));
          for (SymbolicIdentifier i : usedIdentifiers) {
            if (pIdentifierAssignment.containsKey(i)) {
              currentValueState.assignConstant(i, pIdentifierAssignment.get(i), valueVisitor);
            } else {
              currentValueState.assignConstant(i, new NumericValue(0), valueVisitor);
            }
          }
        }
        stateSequence.add(Pair.of(currentState, intermediateEdges));
      }
    }

    return stateSequence;
  }

  private CounterexampleInfo getCexWithSymbolicInformation(
      CounterexampleInfo pCex, PathTemplate pOutputFile) throws CPAException, InterruptedException {
    ARGPath tp = pCex.getTargetPath();
    StringBuilder symbolicInfo = new StringBuilder();

    List<Pair<ForgettingCompositeState, List<CFAEdge>>> stateSequence =
        evaluate(tp, ImmutableMap.of());

    ARGState first = tp.getFirstState();
    ValueAnalysisState firstValue =
        checkNotNull(AbstractStates.extractStateByType(first, ValueAnalysisState.class));
    ConstraintsState firstConstraints =
        checkNotNull(AbstractStates.extractStateByType(first, ConstraintsState.class));
    ForgettingCompositeState currentState =
        new ForgettingCompositeState(firstValue, firstConstraints);
    ValueVisitor<CExpression> toCExpressionVisitor;

    for (Pair<ForgettingCompositeState, List<CFAEdge>> p : stateSequence) {
      ConstraintsState nextConstraints;
      ValueAnalysisState nextVals;

      ForgettingCompositeState nextState = p.getFirst();
      nextVals = nextState.getValueState();
      ValueAnalysisState oldVals = currentState.getValueState();

      Set<Entry<MemoryLocation, ValueAndType>> newAssignees =
          new HashSet<>(nextVals.getConstants());
      newAssignees.removeAll(oldVals.getConstants());
      ImmutableList.Builder<AExpressionStatement> assumptions = ImmutableList.builder();
      for (Entry<MemoryLocation, ValueAndType> e : newAssignees) {
        Value v = e.getValue().getValue();
        CType t = (CType) e.getValue().getType();
        CExpressionStatement exp;
        toCExpressionVisitor = new ValueToCExpressionTransformer(t);
        CExpression rhs = v.accept(toCExpressionVisitor);
        CExpression lhs = getCorrespondingIdExpression(e.getKey(), t);
        CExpression assignment =
            new CBinaryExpression(FileLocation.DUMMY, t, t, lhs, rhs, BinaryOperator.EQUALS);
        exp = new CExpressionStatement(FileLocation.DUMMY, assignment);
        assumptions.add(exp);
      }

      nextConstraints = nextState.getConstraintsState();
      ConstraintsState oldConstraints = currentState.getConstraintsState();

      ConstraintsState newConstraints = nextConstraints.copyOf();
      newConstraints.removeAll(oldConstraints);
      for (Constraint c : newConstraints) {
        toCExpressionVisitor = new ValueToCExpressionTransformer((CType) c.getType());
        CExpressionStatement exp =
            new CExpressionStatement(FileLocation.DUMMY, c.accept(toCExpressionVisitor));
        assumptions.add(exp);
      }

      for (CFAEdge e : p.getSecond()) {
        symbolicInfo.append(e.toString());
        symbolicInfo.append(System.lineSeparator());
      }
      CFAEdgeWithAssumptions edgeWithAssumption =
          new CFAEdgeWithAssumptions(p.getSecond().get(0), assumptions.build(), "");
      symbolicInfo.append(edgeWithAssumption.prettyPrintCode(1));
      currentState = nextState;
    }

    currentState = stateSequence.get(stateSequence.size() - 1).getFirst();
    ConstraintsState finalConstraints = currentState.getConstraintsState();

    List<ValueAssignment> assignments = finalConstraints.getModel();
    Map<SymbolicIdentifier, Value> assignment = new HashMap<>();
    for (ValueAssignment va : assignments) {
      if (SymbolicValues.isSymbolicTerm(va.getName())) {
        SymbolicIdentifier identifier =
            SymbolicValues.convertTermToSymbolicIdentifier(va.getName());
        Value value = SymbolicValues.convertToValue(va);
        assignment.put(identifier, value);
      } else {
        logger.log(
            Level.FINE,
            "Skipping variable %s for assignment because it doesn't fit symbolic identifier"
                + " encoding",
            va.getName());
      }
    }

    stateSequence = evaluate(tp, assignment);
    List<Pair<ValueAnalysisState, List<CFAEdge>>> concreteAssignmentsOnPath =
        new ArrayList<>(stateSequence.size());
    for (Pair<ForgettingCompositeState, List<CFAEdge>> e : stateSequence) {
      concreteAssignmentsOnPath.add(Pair.of(e.getFirst().getValueState(), e.getSecond()));
    }
    CFAPathWithAssumptions assumptionsPath =
        errorPathAllocator.allocateAssignmentsToPath(concreteAssignmentsOnPath);

    CounterexampleInfo concreteCex = CounterexampleInfo.feasiblePrecise(tp, assumptionsPath);

    concreteCex.addFurtherInformation(symbolicInfo, pOutputFile);
    return concreteCex;
  }

  private CIdExpression getCorrespondingIdExpression(MemoryLocation pMemLoc, CType pType) {
    boolean isGlobal = pMemLoc.isOnFunctionStack();
    String varName = pMemLoc.getIdentifier();
    CSimpleDeclaration idDeclaration =
        new CVariableDeclaration(
            FileLocation.DUMMY,
            isGlobal,
            CStorageClass.AUTO,
            pType,
            varName,
            varName,
            varName,
            null);
    CIdExpression idExpression =
        new CIdExpression(FileLocation.DUMMY, pType, varName, idDeclaration);
    return idExpression;
  }

  @Override
  protected void refineUsingInterpolants(
      final ARGReachedSet pReached,
      final InterpolationTree<ForgettingCompositeState, SymbolicInterpolant> pInterpolants)
      throws InterruptedException {
    final Collection<ARGState> roots = pInterpolants.obtainRefinementRoots(restartStrategy);

    for (ARGState r : roots) {
      Multimap<CFANode, MemoryLocation> valuePrecInc = pInterpolants.extractPrecisionIncrement(r);
      ConstraintsPrecision.Increment constrPrecInc = getConstraintsIncrement(r, pInterpolants);

      ARGTreePrecisionUpdater.updateARGTree(pReached, r, valuePrecInc, constrPrecInc);
    }
  }

  private ConstraintsPrecision.Increment getConstraintsIncrement(
      final ARGState pRefinementRoot,
      final InterpolationTree<ForgettingCompositeState, SymbolicInterpolant> pTree) {
    ConstraintsPrecision.Increment.Builder increment = ConstraintsPrecision.Increment.builder();

    if (trackConstraints) {
      Deque<ARGState> todo =
          new ArrayDeque<>(Collections.singleton(pTree.getPredecessor(pRefinementRoot)));

      while (!todo.isEmpty()) {
        final ARGState currentState = todo.removeFirst();

        if (!currentState.isTarget()) {
          SymbolicInterpolant itp = pTree.getInterpolantForState(currentState);

          if (itp != null && !itp.isTrivial()) {
            for (Constraint c : itp.getConstraints()) {
              increment.locallyTracked(AbstractStates.extractLocation(currentState), c);
            }
          }
        }

        Collection<ARGState> successors = pTree.getSuccessors(currentState);
        todo.addAll(successors);
      }
    }

    return increment.build();
  }

  @Override
  protected void printAdditionalStatistics(
      PrintStream out, Result pResult, UnmodifiableReachedSet pReached) {
    // DO NOTHING for now
  }

  @Override
  public void collectStatistics(Collection<Statistics> pStatsCollection) {
    super.collectStatistics(pStatsCollection);
    if (strongestPost instanceof StatisticsProvider) {
      ((StatisticsProvider) strongestPost).collectStatistics(pStatsCollection);
    }
  }
}
