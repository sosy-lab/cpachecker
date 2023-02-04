// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2023 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.core.algorithm.sampling;

import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Iterables;
import java.io.IOException;
import java.io.Writer;
import java.nio.charset.Charset;
import java.nio.file.Path;
import java.util.Collection;
import java.util.HashSet;
import java.util.Optional;
import java.util.Set;
import java.util.StringJoiner;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Level;
import org.sosy_lab.common.ShutdownNotifier;
import org.sosy_lab.common.configuration.Configuration;
import org.sosy_lab.common.configuration.FileOption;
import org.sosy_lab.common.configuration.FileOption.Type;
import org.sosy_lab.common.configuration.InvalidConfigurationException;
import org.sosy_lab.common.configuration.Option;
import org.sosy_lab.common.configuration.Options;
import org.sosy_lab.common.io.IO;
import org.sosy_lab.common.log.LogManager;
import org.sosy_lab.cpachecker.cfa.CFA;
import org.sosy_lab.cpachecker.cfa.CParser;
import org.sosy_lab.cpachecker.cfa.CProgramScope;
import org.sosy_lab.cpachecker.cfa.ast.AExpression;
import org.sosy_lab.cpachecker.cfa.model.CFANode;
import org.sosy_lab.cpachecker.core.algorithm.Algorithm;
import org.sosy_lab.cpachecker.core.algorithm.bmc.CandidateGenerator;
import org.sosy_lab.cpachecker.core.algorithm.bmc.StaticCandidateProvider;
import org.sosy_lab.cpachecker.core.algorithm.bmc.candidateinvariants.CandidateInvariant;
import org.sosy_lab.cpachecker.core.algorithm.bmc.candidateinvariants.ExpressionTreeLocationInvariant;
import org.sosy_lab.cpachecker.core.algorithm.bmc.candidateinvariants.TargetLocationCandidateInvariant;
import org.sosy_lab.cpachecker.core.algorithm.invariants.KInductionInvariantChecker;
import org.sosy_lab.cpachecker.core.algorithm.sampling.Sample.SampleClass;
import org.sosy_lab.cpachecker.core.interfaces.ConfigurableProgramAnalysis;
import org.sosy_lab.cpachecker.core.interfaces.StateSpacePartition;
import org.sosy_lab.cpachecker.core.reachedset.ReachedSet;
import org.sosy_lab.cpachecker.core.reachedset.ReachedSetFactory;
import org.sosy_lab.cpachecker.core.specification.Specification;
import org.sosy_lab.cpachecker.cpa.predicate.PredicateCPA;
import org.sosy_lab.cpachecker.exceptions.CPAException;
import org.sosy_lab.cpachecker.util.CPAs;
import org.sosy_lab.cpachecker.util.CParserUtils;
import org.sosy_lab.cpachecker.util.CParserUtils.ParserTools;
import org.sosy_lab.cpachecker.util.expressions.ExpressionTree;
import org.sosy_lab.cpachecker.util.expressions.ExpressionTrees;
import org.sosy_lab.cpachecker.util.predicates.pathformula.PathFormulaManager;
import org.sosy_lab.cpachecker.util.predicates.smt.BooleanFormulaManagerView;
import org.sosy_lab.cpachecker.util.predicates.smt.FormulaManagerView;
import org.sosy_lab.cpachecker.util.predicates.smt.Solver;
import org.sosy_lab.java_smt.api.BooleanFormula;
import org.sosy_lab.java_smt.api.Model.ValueAssignment;
import org.sosy_lab.java_smt.api.ProverEnvironment;
import org.sosy_lab.java_smt.api.SolverContext.ProverOptions;
import org.sosy_lab.java_smt.api.SolverException;

@Options(prefix = "invariantValidation")
public class InvariantValidationAlgorithm implements Algorithm {

  @Option(
      secure = true,
      description = "The file to which precondition counterexamples shall be written.")
  @FileOption(Type.OUTPUT_FILE)
  private Path preCexOutFile = Path.of("pre_cex_samples.json");

  @Option(
      secure = true,
      description = "The file to which step case counterexamples shall be written.")
  @FileOption(Type.OUTPUT_FILE)
  private Path stepCexOutFile = Path.of("step_cex_samples.json");

  @Option(
      secure = true,
      description = "The file to which postcondition counterexamples shall be written.")
  @FileOption(Type.OUTPUT_FILE)
  private Path postCexOutFile = Path.of("post_cex_samples.json");

  @Option(secure = true, description = "The invariant to be validated.")
  private String invariant;

  private final Configuration config;
  private final Algorithm algorithm;
  private final ConfigurableProgramAnalysis cpa;
  private final CFA cfa;
  private final LogManager logger;
  private final ShutdownNotifier shutdownNotifier;
  private final Specification specification;

  private final CParser parser;
  private final ParserTools parserTools;

  public record PreconditionCounterexample(
      CandidateInvariant candidate, Collection<ValueAssignment> pre) {}

  public record StepCaseCounterexample(
      CandidateInvariant candidate,
      Collection<ValueAssignment> loopBefore,
      Collection<ValueAssignment> loopAfter) {}

  public InvariantValidationAlgorithm(
      Configuration pConfig,
      Algorithm pAlgorithm,
      ConfigurableProgramAnalysis pCpa,
      CFA pCFA,
      LogManager pLogger,
      ShutdownNotifier pShutdownNotifier,
      Specification pSpecification)
      throws InvalidConfigurationException {
    pConfig.inject(this);

    config = pConfig;
    algorithm = pAlgorithm;
    cpa = pCpa;
    cfa = pCFA;
    logger = pLogger;
    shutdownNotifier = pShutdownNotifier;
    specification = pSpecification;

    parser =
        CParser.Factory.getParser(
            logger, CParser.Factory.getOptions(config), cfa.getMachineModel(), shutdownNotifier);
    parserTools = ParserTools.create(ExpressionTrees.newFactory(), cfa.getMachineModel(), logger);
  }

  @Override
  public AlgorithmStatus run(ReachedSet reachedSet) throws CPAException, InterruptedException {
    if (cfa.getAllLoopHeads().isEmpty()) {
      logger.log(
          Level.INFO, "No loop heads detected, nothing to do for invariant validation algorithm.");
    } else if (cfa.getAllLoopHeads().orElseThrow().size() != 1) {
      logger.log(
          Level.INFO,
          "Only single-loop programs with exactly one loop head are currently supported.");
    } else {
      CFANode loopHead = Iterables.getOnlyElement(cfa.getAllLoopHeads().orElseThrow());
      try {
        validateAt(loopHead);
      } catch (InvalidConfigurationException pE) {
        logger.log(Level.WARNING, "Invariant validation failed due to invalid configuration.");
      } catch (SolverException pE) {
        logger.log(Level.WARNING, "Invariant validation failed due to solver failure.");
      }
    }
    // TODO: Add statistics
    return AlgorithmStatus.NO_PROPERTY_CHECKED;
  }

  private void validateAt(CFANode pLocation)
      throws CPAException, InterruptedException, InvalidConfigurationException, SolverException {
    // TODO: Might need to further adjust scope according to pLocation
    CProgramScope scope =
        new CProgramScope(cfa, logger).withFunctionScope(pLocation.getFunctionName());

    ExpressionTree<AExpression> expressionTree =
        CParserUtils.parseStatementsAsExpressionTree(
            ImmutableSet.of(invariant), Optional.empty(), parser, scope, parserTools);
    CandidateInvariant candidate =
        new ExpressionTreeLocationInvariant(
            pLocation.toString(), pLocation, expressionTree, new ConcurrentHashMap<>());

    // Validate invariant using k-Induction
    CandidateGenerator candidateGenerator = new StaticCandidateProvider(ImmutableSet.of(candidate));
    KInductionInvariantChecker invariantChecker =
        new KInductionInvariantChecker(
            config, shutdownNotifier, logger, cfa, specification, candidateGenerator, true);
    invariantChecker.checkCandidates();

    Set<? extends CandidateInvariant> confirmed = candidateGenerator.getConfirmedCandidates();
    boolean validated = !confirmed.isEmpty();

    Set<Sample> pre_samples = new HashSet<>();
    Set<Sample> step_samples = new HashSet<>();
    Set<Sample> post_samples = new HashSet<>();

    if (validated) {
      // Just because the invariant was validated does not mean it is also useful, so check whether
      // an error location is still reachable
      post_samples = checkPostcondition(candidate, pLocation);
    } else {
      logger.log(Level.INFO, "Invariant was not validated, collecting counterexamples...");
      Set<PreconditionCounterexample> pre_cexs = invariantChecker.getPreconditionCounterexamples();
      Set<StepCaseCounterexample> step_cexs = invariantChecker.getStepCaseCounterexamples();

      // Counterexamples are reachable, so all are positive samples
      for (PreconditionCounterexample pre_cex : pre_cexs) {
        if (!pre_cex.candidate().equals(candidate)) {
          continue;
        }
        Iterable<ValueAssignment> model = pre_cex.pre();
        pre_samples.add(
            SampleUtils.extractSampleFromRelevantAssignments(
                model, pLocation, SampleClass.POSITIVE));
      }

      // Counterexamples are reachable, so all are positive samples
      for (StepCaseCounterexample step_cex : step_cexs) {
        if (!step_cex.candidate().equals(candidate)) {
          continue;
        }
        Sample sampleBefore =
            SampleUtils.extractSampleFromRelevantAssignments(
                step_cex.loopBefore(), pLocation, SampleClass.POSITIVE);
        step_samples.add(sampleBefore);

        Sample sampleAfter =
            SampleUtils.extractSampleFromRelevantAssignments(
                    step_cex.loopAfter(), pLocation, SampleClass.POSITIVE)
                .withPrevious(sampleBefore);
        step_samples.add(sampleAfter);
      }
    }

    writeSamplesToFile(pre_samples, preCexOutFile);
    writeSamplesToFile(step_samples, stepCexOutFile);
    writeSamplesToFile(post_samples, postCexOutFile);
  }

  private Set<Sample> checkPostcondition(CandidateInvariant pCandidate, CFANode pLocation)
      throws CPAException, InterruptedException, InvalidConfigurationException, SolverException {
    Set<Sample> post_samples = new HashSet<>();

    // Retrieve formula managers
    PredicateCPA predicateCPA =
        CPAs.retrieveCPAOrFail(cpa, PredicateCPA.class, InvariantValidationAlgorithm.class);
    Solver solver = predicateCPA.getSolver();
    FormulaManagerView fmgr = solver.getFormulaManager();
    BooleanFormulaManagerView bfmgr = fmgr.getBooleanFormulaManager();
    PathFormulaManager pmgr = predicateCPA.getPathFormulaManager();

    // Run algorithm to determine reachable states
    ReachedSetFactory reachedSetFactory = new ReachedSetFactory(config, logger);
    ReachedSet reached =
        reachedSetFactory.createAndInitialize(
            cpa, cfa.getMainFunction(), StateSpacePartition.getDefaultPartition());
    algorithm.run(reached);

    // Assert that target location is reachable
    CandidateInvariant targetReachable = TargetLocationCandidateInvariant.INSTANCE;
    BooleanFormula program = bfmgr.not(targetReachable.getAssertion(reached, fmgr, pmgr));

    // Assert that invariant holds
    BooleanFormula invariantHolds = pCandidate.getAssertion(reached, fmgr, pmgr);

    try (ProverEnvironment prover = solver.newProverEnvironment(ProverOptions.GENERATE_MODELS)) {
      // Check whether the invariant is strong enough to prove the postcondition, otherwise generate
      // counterexamples.
      prover.push(bfmgr.and(program, invariantHolds));
      if (!prover.isUnsat()) {
        Iterable<ValueAssignment> model = prover.getModelAssignments();
        // Postcondition counterexamples lead to an error state and are thus negative by definition
        post_samples.add(
            SampleUtils.extractSampleFromRelevantAssignments(
                model, pLocation, SampleClass.NEGATIVE));
      }
    }

    return post_samples;
  }

  private void writeSamplesToFile(Set<Sample> samples, Path outFile) {
    StringJoiner sj = new StringJoiner(",\n", "[\n", "]\n");
    for (Sample sample : samples) {
      sj.add(sample.export());
    }
    try (Writer writer = IO.openOutputFile(outFile, Charset.defaultCharset())) {
      writer.write(sj.toString());
    } catch (IOException e) {
      logger.log(Level.WARNING, "Export of produced samples failed");
    }
  }
}
