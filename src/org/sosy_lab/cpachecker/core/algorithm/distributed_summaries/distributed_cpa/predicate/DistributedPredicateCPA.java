// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2022 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.core.algorithm.distributed_summaries.distributed_cpa.predicate;

import com.google.common.base.Preconditions;
import com.google.common.collect.BiMap;
import com.google.common.collect.ImmutableListMultimap;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import org.sosy_lab.common.ShutdownNotifier;
import org.sosy_lab.common.configuration.Configuration;
import org.sosy_lab.common.configuration.IntegerOption;
import org.sosy_lab.common.configuration.InvalidConfigurationException;
import org.sosy_lab.common.configuration.Option;
import org.sosy_lab.common.configuration.Options;
import org.sosy_lab.common.log.LogManager;
import org.sosy_lab.cpachecker.cfa.CFA;
import org.sosy_lab.cpachecker.cfa.model.CFANode;
import org.sosy_lab.cpachecker.cfa.types.Type;
import org.sosy_lab.cpachecker.core.AnalysisDirection;
import org.sosy_lab.cpachecker.core.algorithm.distributed_summaries.decomposition.graph.BlockNode;
import org.sosy_lab.cpachecker.core.algorithm.distributed_summaries.distributed_cpa.ForwardingDistributedConfigurableProgramAnalysis;
import org.sosy_lab.cpachecker.core.algorithm.distributed_summaries.distributed_cpa.operators.combine.CombinePrecisionOperator;
import org.sosy_lab.cpachecker.core.algorithm.distributed_summaries.distributed_cpa.operators.combine.CombinePreconditionsOperator;
import org.sosy_lab.cpachecker.core.algorithm.distributed_summaries.distributed_cpa.operators.coverage.CoverageOperator;
import org.sosy_lab.cpachecker.core.algorithm.distributed_summaries.distributed_cpa.operators.deserialize.DeserializeOperator;
import org.sosy_lab.cpachecker.core.algorithm.distributed_summaries.distributed_cpa.operators.deserialize.DeserializePrecisionOperator;
import org.sosy_lab.cpachecker.core.algorithm.distributed_summaries.distributed_cpa.operators.proceed.ProceedOperator;
import org.sosy_lab.cpachecker.core.algorithm.distributed_summaries.distributed_cpa.operators.serialize.SerializeOperator;
import org.sosy_lab.cpachecker.core.algorithm.distributed_summaries.distributed_cpa.operators.serialize.SerializePrecisionOperator;
import org.sosy_lab.cpachecker.core.algorithm.distributed_summaries.distributed_cpa.operators.verification_condition.ViolationConditionOperator;
import org.sosy_lab.cpachecker.core.algorithm.distributed_summaries.worker.DssAnalysisOptions;
import org.sosy_lab.cpachecker.core.interfaces.AbstractState;
import org.sosy_lab.cpachecker.core.interfaces.ConfigurableProgramAnalysis;
import org.sosy_lab.cpachecker.core.interfaces.Precision;
import org.sosy_lab.cpachecker.cpa.predicate.PredicateAbstractState;
import org.sosy_lab.cpachecker.cpa.predicate.PredicateCPA;
import org.sosy_lab.cpachecker.cpa.predicate.PredicatePrecision;
import org.sosy_lab.cpachecker.util.predicates.AbstractionPredicate;
import org.sosy_lab.cpachecker.util.predicates.pathformula.PathFormulaManagerImpl;
import org.sosy_lab.cpachecker.util.predicates.smt.Solver;

@Options(prefix = "dss.cpa.predicate")
public class DistributedPredicateCPA
    implements ForwardingDistributedConfigurableProgramAnalysis, AutoCloseable {

  @Option(description = "Whether to query the SMT solver before proceeding backwards.")
  private boolean doSatChecksBeforeBackwardAnalysis = false;

  @Option(
      secure = true,
      description =
          "Whether to remove the intermediate variables of a violation condition before it is"
              + " sent. The result is equivalent, but usually much smaller, and conditions of"
              + " different paths that only differ in intermediate values become equal.")
  private boolean projectViolationConditions = true;

  @Option(
      secure = true,
      description =
          "Whether graph-condition projection may substitute into nested disjunctions."
              + " Disabling this keeps shared intermediate values, which can make large"
              + " exact conditions easier for the solver even when messages become larger.")
  private boolean projectNestedDisjunctions = true;

  @Option(
      secure = true,
      description =
          "Whether to rewrite every violation condition as a disjunction of cubes generalized from"
              + " models. The cubes together describe exactly all entry states of the condition;"
              + " incomplete enumeration falls back to the original condition.")
  private boolean generalizeViolationConditions = true;

  @Option(
      secure = true,
      description =
          "The maximal number of cubes a generalized violation condition may consist of. A"
              + " condition that needs more is sent exactly.")
  @IntegerOption(min = 1)
  private int maxCubesPerViolationCondition = 16;

  @Option(
      secure = true,
      description =
          "Whether to rewrite generalized violation conditions over the predicates of the"
              + " precondition where possible. These are the predicates the predecessors abstract"
              + " their block ends with, including the interpolants they learned while refuting"
              + " violation conditions of this block. Has no effect unless"
              + " generalizeViolationConditions is enabled.")
  private boolean generalizeOverPreconditionPredicates = true;

  @Option(
      secure = true,
      description =
          "The largest share of CFA edges accessing modelled memory, from 0 to 1, for which"
              + " violation conditions are generalized and projected into nested disjunctions."
              + " Conditions of programs that use memory more are only projected: with the"
              + " pointer-aliasing encoding, generalization and nested projection cost much more"
              + " than they save there.")
  private double maxMemoryShareForExpensiveSimplification = 1.0;

  private final PredicateCPA predicateCPA;

  private final SerializeOperator serialize;

  private final SerializePrecisionOperator serializePrecisionOperator;
  private final DeserializePredicateStateOperator deserialize;

  private final DeserializePrecisionOperator deserializePrecisionOperator;
  private final ProceedOperator proceedOperator;
  private final ViolationConditionOperator verificationConditionOperator;
  private final CoverageOperator stateCoverageOperator;
  private final CombinePreconditionsOperator combinePreconditionsOperator;
  private final CombinePrecisionOperator combinePrecisionOperator;
  private final PredicateStateCombineViolationConditionOperator combineViolationConditionsOperator;

  public DistributedPredicateCPA(
      PredicateCPA pPredicateCPA,
      BlockNode pNode,
      CFA pCFA,
      Configuration pConfiguration,
      DssAnalysisOptions pOptions,
      LogManager pLogManager,
      ShutdownNotifier pShutdownNotifier,
      BiMap<Integer, CFANode> pIdToNodeMap,
      ImmutableMap<String, Type> pTypeMap,
      double pMemoryShare)
      throws InvalidConfigurationException {
    pConfiguration.inject(this);
    if (maxMemoryShareForExpensiveSimplification < 0
        || maxMemoryShareForExpensiveSimplification > 1) {
      throw new InvalidConfigurationException(
          "dss.cpa.predicate.maxMemoryShareForExpensiveSimplification has to be between 0 and 1");
    }
    // decided once for the whole program, see maxMemoryShareForExpensiveSimplification
    boolean expensiveSimplification = pMemoryShare <= maxMemoryShareForExpensiveSimplification;
    predicateCPA = pPredicateCPA;
    final boolean writeReadableFormulas = pOptions.writeReadableFormulas();
    serialize =
        new SerializePredicateStateOperator(predicateCPA, pCFA, writeReadableFormulas, pTypeMap);
    deserialize = new DeserializePredicateStateOperator(predicateCPA, pCFA, pNode, pTypeMap);
    ImmutableSet<CFANode> boundaries =
        ImmutableSet.of(pNode.getInitialLocation(), pNode.getFinalLocation());
    SerializePredicatePrecisionOperator precisionSerializer =
        new SerializePredicatePrecisionOperator(
            pPredicateCPA.getSolver().getFormulaManager(), pIdToNodeMap.inverse());
    DeserializePredicatePrecisionOperator precisionDeserializer =
        new DeserializePredicatePrecisionOperator(
            predicateCPA.getAbstractionManager(), pIdToNodeMap::get);
    serializePrecisionOperator =
        precision ->
            precisionSerializer.serializePrecision(boundaryPrecision(precision, boundaries));
    deserializePrecisionOperator =
        message ->
            boundaryPrecision(precisionDeserializer.deserializePrecision(message), boundaries);
    if (doSatChecksBeforeBackwardAnalysis) {
      proceedOperator = new ProceedPredicateStateOperator(predicateCPA.getSolver());
    } else {
      proceedOperator = ProceedOperator.always();
    }
    stateCoverageOperator =
        new PredicateStateCoverageOperator(
            predicateCPA.getSolver(), pOptions.cacheViolationConditions());
    Solver solver = predicateCPA.getSolver();
    ExistentialProjection projection =
        projectViolationConditions ? new ExistentialProjection(solver) : null;
    verificationConditionOperator =
        new PredicateViolationConditionOperator(
            predicateCPA,
            new PathFormulaManagerImpl(
                solver.getFormulaManager(),
                pConfiguration,
                pLogManager,
                pShutdownNotifier,
                pCFA,
                AnalysisDirection.BACKWARD),
            pNode.getPredecessorIds().isEmpty(),
            generalizeViolationConditions && expensiveSimplification
                ? new ModelBasedGeneralization(
                    solver,
                    new ExistentialProjection(solver),
                    maxCubesPerViolationCondition,
                    generalizeOverPreconditionPredicates)
                : null,
            projection,
            projectViolationConditions
                ? new ExistentialProjection(
                    solver, projectNestedDisjunctions && expensiveSimplification)
                : null);
    combinePreconditionsOperator = new CombinePredicateStatePreconditionsOperator(predicateCPA);
    combinePrecisionOperator = new CombinePredicatePrecisionOperator();
    combineViolationConditionsOperator =
        new PredicateStateCombineViolationConditionOperator(
            predicateCPA.getPathFormulaManager(), projection);
  }

  /**
   * Neighboring blocks share the same CFA node at their boundary. Keep predicates at those nodes,
   * including predicates over local variables, without turning them into global predicates.
   * Function-wide and global predicates retain their explicitly configured scope.
   */
  static PredicatePrecision boundaryPrecision(
      Precision pPrecision, ImmutableSet<CFANode> pBoundaries) {
    PredicatePrecision precision = (PredicatePrecision) pPrecision;
    ImmutableListMultimap.Builder<CFANode, AbstractionPredicate> local =
        ImmutableListMultimap.builder();
    precision
        .getLocalPredicates()
        .forEach(
            (node, predicate) -> {
              if (pBoundaries.contains(node)) {
                local.put(node, predicate);
              }
            });
    ImmutableListMultimap.Builder<PredicatePrecision.LocationInstance, AbstractionPredicate>
        instances = ImmutableListMultimap.builder();
    precision
        .getLocationInstancePredicates()
        .forEach(
            (location, predicate) -> {
              if (pBoundaries.contains(location.getLocation())) {
                instances.put(location, predicate);
              }
            });
    return new PredicatePrecision(
        instances.build(),
        local.build(),
        precision.getFunctionPredicates(),
        precision.getGlobalPredicates());
  }

  @Override
  public SerializePrecisionOperator getSerializePrecisionOperator() {
    return serializePrecisionOperator;
  }

  @Override
  public DeserializePrecisionOperator getDeserializePrecisionOperator() {
    return deserializePrecisionOperator;
  }

  @Override
  public CombinePrecisionOperator getCombinePrecisionOperator() {
    return combinePrecisionOperator;
  }

  @Override
  public SerializeOperator getSerializeOperator() {
    return serialize;
  }

  @Override
  public DeserializeOperator getDeserializeOperator() {
    return deserialize;
  }

  @Override
  public ProceedOperator getProceedOperator() {
    return proceedOperator;
  }

  @Override
  public Class<? extends AbstractState> getAbstractStateClass() {
    return PredicateAbstractState.class;
  }

  @Override
  public ConfigurableProgramAnalysis getCPA() {
    return predicateCPA;
  }

  @Override
  public boolean isMostGeneralBlockEntryState(AbstractState pAbstractState) {
    PredicateAbstractState predicateAbstractState = (PredicateAbstractState) pAbstractState;
    if (predicateAbstractState.isAbstractionState()) {
      return predicateAbstractState.getAbstractionFormula().isTrue();
    }
    return predicateCPA
        .getSolver()
        .getFormulaManager()
        .getBooleanFormulaManager()
        .isTrue(predicateAbstractState.getPathFormula().getFormula());
  }

  @Override
  public Object computeProgramPointId(AbstractState pAbstractState) {
    // The predicate state has no information about the point in the program, so always
    // return the same number (arbitrarily chosen)
    return 0;
  }

  @Override
  public PredicateStateCombineViolationConditionOperator getCombineViolationConditionsOperator() {
    return combineViolationConditionsOperator;
  }

  @Override
  public AbstractState reset(AbstractState pAbstractState) {
    Preconditions.checkArgument(
        pAbstractState instanceof PredicateAbstractState,
        "Expected PredicateAbstractState, but got %s",
        pAbstractState.getClass().getSimpleName());
    return pAbstractState;
  }

  @Override
  public ViolationConditionOperator getViolationConditionOperator() {
    return verificationConditionOperator;
  }

  @Override
  public CoverageOperator getCoverageOperator() {
    return stateCoverageOperator;
  }

  @Override
  public CombinePreconditionsOperator getCombineOperator() {
    return combinePreconditionsOperator;
  }

  @Override
  public void close() {
    predicateCPA.close();
  }
}
