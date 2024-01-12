// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2023 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.core.algorithm.sampling;

import com.google.common.base.Preconditions;
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
import org.sosy_lab.cpachecker.cfa.ast.FileLocation;
import org.sosy_lab.cpachecker.cfa.model.CFANode;
import org.sosy_lab.cpachecker.core.algorithm.Algorithm;
import org.sosy_lab.cpachecker.core.algorithm.bmc.BMCHelper;
import org.sosy_lab.cpachecker.core.algorithm.bmc.candidateinvariants.CandidateInvariant;
import org.sosy_lab.cpachecker.core.algorithm.bmc.candidateinvariants.ExpressionTreeLocationInvariant;
import org.sosy_lab.cpachecker.core.algorithm.bmc.candidateinvariants.TargetLocationCandidateInvariant;
import org.sosy_lab.cpachecker.core.algorithm.sampling.Sample.SampleClass;
import org.sosy_lab.cpachecker.core.interfaces.AbstractState;
import org.sosy_lab.cpachecker.core.interfaces.ConfigurableProgramAnalysis;
import org.sosy_lab.cpachecker.core.reachedset.ReachedSet;
import org.sosy_lab.cpachecker.cpa.arg.ARGState;
import org.sosy_lab.cpachecker.cpa.predicate.PredicateAbstractState;
import org.sosy_lab.cpachecker.cpa.predicate.PredicateCPA;
import org.sosy_lab.cpachecker.exceptions.CPAException;
import org.sosy_lab.cpachecker.util.AbstractStates;
import org.sosy_lab.cpachecker.util.CPAs;
import org.sosy_lab.cpachecker.util.CParserUtils;
import org.sosy_lab.cpachecker.util.CParserUtils.ParserTools;
import org.sosy_lab.cpachecker.util.expressions.ExpressionTree;
import org.sosy_lab.cpachecker.util.expressions.ExpressionTrees;
import org.sosy_lab.cpachecker.util.predicates.pathformula.PathFormula;
import org.sosy_lab.cpachecker.util.predicates.pathformula.PathFormulaManager;
import org.sosy_lab.cpachecker.util.predicates.pathformula.SSAMap;
import org.sosy_lab.cpachecker.util.predicates.smt.BitvectorFormulaManagerView;
import org.sosy_lab.cpachecker.util.predicates.smt.BooleanFormulaManagerView;
import org.sosy_lab.cpachecker.util.predicates.smt.FormulaManagerView;
import org.sosy_lab.cpachecker.util.predicates.smt.Solver;
import org.sosy_lab.java_smt.api.BitvectorFormula;
import org.sosy_lab.java_smt.api.BooleanFormula;
import org.sosy_lab.java_smt.api.Formula;
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

  @Option(
      secure = true,
      description =
          "The invariant to be validated. Ignored if option invariantValidation.outputVCs"
              + "is enabled, but must be given otherwise.")
  private String invariant = null;

  @Option(
      secure = true,
      description =
          "The location at which the given invariant should be validated,"
              + "given in the format 'line:column'."
              + "If not specified, validation is attempted at every loop head.")
  private String location = null;

  @Option(
      secure = true,
      description =
          "The file to which verification conditions are output."
              + "Unused if option invariantValidation.outputVCs is disabled.")
  @FileOption(Type.OUTPUT_FILE)
  private Path vcFile = Path.of("vcs.txt");

  @Option(
      secure = true,
      description = "Only output the necessary verification conditions, do not attempt validation.")
  private boolean outputVCs = false;

  private final Algorithm algorithm;
  private final ConfigurableProgramAnalysis cpa;
  private final CFA cfa;
  private final LogManager logger;

  private final CParser parser;
  private final ParserTools parserTools;

  public InvariantValidationAlgorithm(
      Configuration pConfig,
      Algorithm pAlgorithm,
      ConfigurableProgramAnalysis pCpa,
      CFA pCFA,
      LogManager pLogger,
      ShutdownNotifier pShutdownNotifier)
      throws InvalidConfigurationException {
    pConfig.inject(this);

    algorithm = pAlgorithm;
    cpa = pCpa;
    cfa = pCFA;
    logger = pLogger;

    parser =
        CParser.Factory.getParser(
            logger, CParser.Factory.getOptions(pConfig), cfa.getMachineModel(), pShutdownNotifier);
    parserTools = ParserTools.create(ExpressionTrees.newFactory(), cfa.getMachineModel(), logger);
  }

  @Override
  public AlgorithmStatus run(ReachedSet reachedSet) throws CPAException, InterruptedException {
    Preconditions.checkState(outputVCs || invariant != null);

    if (cfa.getAllLoopHeads().isEmpty()) {
      logger.log(
          Level.INFO, "No loop heads detected, nothing to do for invariant validation algorithm.");
      return AlgorithmStatus.NO_PROPERTY_CHECKED;
    }

    Set<Sample> preSamples = new HashSet<>();
    Set<Sample> stepSamples = new HashSet<>();
    Set<Sample> postSamples = new HashSet<>();
    StringJoiner vcJoiner = new StringJoiner(",\n", "[", "]");

    for (CFANode loopHead : cfa.getAllLoopHeads().orElseThrow()) {
      try {
        if (outputVCs) {
          vcJoiner.add(outputVerificationConditions(reachedSet, loopHead));
        } else {
          validateSMT(reachedSet, loopHead, preSamples, stepSamples, postSamples);
        }
      } catch (InvalidConfigurationException e) {
        logger.log(Level.WARNING, "Invariant validation failed due to invalid configuration.");
      } catch (SolverException e) {
        logger.log(Level.WARNING, "Invariant validation failed due to solver failure.");
      }
    }

    if (outputVCs) {
      try (Writer writer = IO.openOutputFile(vcFile, Charset.defaultCharset())) {
        writer.write(vcJoiner.toString());
      } catch (IOException e) {
        logger.log(Level.WARNING, "Export of Verification Conditions failed");
      }
    } else {
      writeSamplesToFile(preSamples, preCexOutFile);
      writeSamplesToFile(stepSamples, stepCexOutFile);
      writeSamplesToFile(postSamples, postCexOutFile);
    }

    // TODO: Add statistics
    return AlgorithmStatus.NO_PROPERTY_CHECKED;
  }

  private String outputVerificationConditions(ReachedSet pReachedSet, CFANode pLocation)
      throws CPAException, InterruptedException, InvalidConfigurationException {
    // Retrieve formula managers
    PredicateCPA predicateCPA =
        CPAs.retrieveCPAOrFail(cpa, PredicateCPA.class, InvariantValidationAlgorithm.class);
    Solver solver = predicateCPA.getSolver();
    FormulaManagerView fmgr = solver.getFormulaManager();
    BitvectorFormulaManagerView bvmgr = fmgr.getBitvectorFormulaManager();
    BooleanFormulaManagerView bfmgr = fmgr.getBooleanFormulaManager();
    PathFormulaManager pmgr = predicateCPA.getPathFormulaManager();

    // Run algorithm to determine reachable states
    algorithm.run(pReachedSet);

    PathFormula pathFormula = pmgr.makeEmptyPathFormula();
    Iterable<AbstractState> statesAtLocation =
        AbstractStates.filterLocation(pReachedSet, pLocation);
    assert Iterables.size(statesAtLocation) == 1;
    SSAMap ssaBefore = SSAMap.emptySSAMap();
    for (AbstractState state : statesAtLocation) {
      PredicateAbstractState predState =
          AbstractStates.extractStateByType(state, PredicateAbstractState.class);
      ssaBefore = predState.getPathFormula().getSsa();

      for (ARGState covered : ((ARGState) state).getCoveredByThis()) {
        Collection<ARGState> parents = covered.getParents();
        assert parents.size() == 1;
        PredicateAbstractState predicateState =
            AbstractStates.extractStateByType(
                Iterables.getOnlyElement(parents), PredicateAbstractState.class);
        pathFormula = predicateState.getPathFormula();
      }
    }

    // Assert that target location is not reachable
    CandidateInvariant targetReachable = TargetLocationCandidateInvariant.INSTANCE;
    BooleanFormula programSafe = targetReachable.getAssertion(pReachedSet, fmgr, pmgr);

    // Assert that precondition holds
    BooleanFormula preconditionFulfilled = BMCHelper.createFormulaFor(statesAtLocation, bfmgr);

    // Output necessary parts to build relevant formulas for different invariants
    StringBuilder output = new StringBuilder();

    output.append("{\"location\":{\n");
    FileLocation fileLocation = SampleUtils.getLocationForNode(pLocation);
    Path filename = fileLocation.getFileName();
    int line = fileLocation.getStartingLineInOrigin();
    // TODO: Computing column requires access to file
    //       (e.g. by using offsets computed by InvariantStoreUtil::getLineOffsetsByFile)
    //       but offsets are still not reliable if --preprocess is used.
    int column = 0;

    output.append(
        "\"filename\": \"%s\",\n\"line\": %s,\n\"column\": %s},\n"
            .formatted(filename, line, column));

    output.append("\"constant-declarations\":\n\"");
    Set<Formula> variables = new HashSet<>(fmgr.extractVariables(preconditionFulfilled).values());
    variables.addAll(fmgr.extractVariables(pathFormula.getFormula()).values());
    variables.addAll(fmgr.extractVariables(programSafe).values());
    for (Formula variable : variables) {
      // TODO: We assume all variables are bitvectors for now
      int length = bvmgr.getLength((BitvectorFormula) variable);
      output.append("(declare-const %s (_ BitVec %s))".formatted(variable, length));
    }
    output.append("\",\n");

    output.append("\"precondition\":\n\"");
    output.append("(assert ");
    output.append(preconditionFulfilled.toString().replaceAll("\\s+", " "));
    output.append(")\",\n");

    output.append("\"inductiveness\":\n\"");
    output.append("(assert ");
    output.append(pathFormula.getFormula().toString().replaceAll("\\s+", " "));
    output.append(")\",\n");

    output.append("\"postcondition\":\n\"");
    output.append("(assert ");
    output.append(bfmgr.not(programSafe).toString().replaceAll("\\s+", " "));
    output.append(")\",\n");

    StringJoiner sj = new StringJoiner(",\n");
    output.append("\"ssaBefore\":\n{");
    for (String variable : ssaBefore.allVariables()) {
      sj.add('"' + variable + '"' + ": " + ssaBefore.getIndex(variable));
    }
    output.append(sj);
    output.append("},\n");

    sj = new StringJoiner(",\n");
    output.append("\"ssaAfter\":\n{");
    for (String variable : pathFormula.getSsa().allVariables()) {
      sj.add('"' + variable + '"' + ": " + pathFormula.getSsa().getIndex(variable));
    }
    output.append(sj);
    output.append("}}");

    return output.toString();
  }

  private void validateSMT(
      ReachedSet pReachedSet,
      CFANode pLocation,
      Set<Sample> pPreSamples,
      Set<Sample> pStepSamples,
      Set<Sample> pPostSamples)
      throws CPAException, InterruptedException, InvalidConfigurationException, SolverException {
    // TODO: Might need to further adjust scope according to pLocation.
    //       On the other hand: Is constructing a CandidateInvariant really necessary?
    CProgramScope scope =
        new CProgramScope(cfa, logger).withFunctionScope(pLocation.getFunctionName());
    ExpressionTree<AExpression> expressionTree =
        CParserUtils.parseStatementsAsExpressionTree(
            ImmutableSet.of(invariant), Optional.empty(), parser, scope, parserTools);
    CandidateInvariant candidate =
        new ExpressionTreeLocationInvariant(
            pLocation.toString(), pLocation, expressionTree, new ConcurrentHashMap<>());

    // Retrieve formula managers
    PredicateCPA predicateCPA =
        CPAs.retrieveCPAOrFail(cpa, PredicateCPA.class, InvariantValidationAlgorithm.class);
    Solver solver = predicateCPA.getSolver();
    FormulaManagerView fmgr = solver.getFormulaManager();
    BooleanFormulaManagerView bfmgr = fmgr.getBooleanFormulaManager();
    PathFormulaManager pmgr = predicateCPA.getPathFormulaManager();

    // Run algorithm to determine reachable states
    algorithm.run(pReachedSet);

    PathFormula pathFormula = pmgr.makeEmptyPathFormula();
    Iterable<AbstractState> statesAtLocation = candidate.filterApplicable(pReachedSet);
    for (AbstractState state : statesAtLocation) {
      for (ARGState covered : ((ARGState) state).getCoveredByThis()) {
        Collection<ARGState> parents = covered.getParents();
        assert parents.size() == 1;
        PredicateAbstractState predicateState =
            AbstractStates.extractStateByType(
                Iterables.getOnlyElement(parents), PredicateAbstractState.class);
        pathFormula = predicateState.getPathFormula();
      }
    }

    // Assert that target location is not reachable
    CandidateInvariant targetReachable = TargetLocationCandidateInvariant.INSTANCE;
    BooleanFormula programSafe = targetReachable.getAssertion(pReachedSet, fmgr, pmgr);

    // Assert that precondition holds
    BooleanFormula preconditionFulfilled = BMCHelper.createFormulaFor(statesAtLocation, bfmgr);

    // Assert that invariant holds
    BooleanFormula invariantHolds = candidate.getAssertion(pReachedSet, fmgr, pmgr);

    // TODO: To avoid unnecessary work, pass the location where the candidate should be checked
    //       and throw an exception if a used variable does not exist

    // Any models satisfying this formula are precondition counterexamples: !(P => I)
    BooleanFormula preconditionFormula =
        bfmgr.and(preconditionFulfilled, bfmgr.not(invariantHolds));
    // Any models satisfying this formula are counterexamples to inductiveness (B & I(x)) -> I(x')
    BooleanFormula invariantHoldsAfter =
        fmgr.instantiate(fmgr.uninstantiate(invariantHolds), pathFormula.getSsa());
    BooleanFormula inductivenessFormula =
        bfmgr.and(invariantHolds, pathFormula.getFormula(), bfmgr.not(invariantHoldsAfter));
    // Any models satisfying this formula are postcondition counterexamples: !(I & !B => Q)
    BooleanFormula postconditionFormula = bfmgr.and(invariantHolds, bfmgr.not(programSafe));
    // TODO: Could also obtain more than one counterexample for each formula if it exists
    try (ProverEnvironment prover = solver.newProverEnvironment(ProverOptions.GENERATE_MODELS)) {
      prover.push(preconditionFormula);
      if (!prover.isUnsat()) {
        Iterable<ValueAssignment> model = prover.getModelAssignments();
        pPreSamples.add(
            SampleUtils.extractSampleFromRelevantAssignments(
                model, pLocation, SampleClass.POSITIVE));
      }
      prover.pop();

      prover.push(inductivenessFormula);
      if (!prover.isUnsat()) {
        Iterable<ValueAssignment> model = prover.getModelAssignments();

        // Add sample before
        pStepSamples.add(
            SampleUtils.extractSampleFromModel(
                SampleUtils.getAssignmentsWithLowestIndices(model),
                pLocation,
                SampleClass.POSITIVE));

        // Add sample after
        pStepSamples.add(
            SampleUtils.extractSampleFromRelevantAssignments(
                model, pLocation, SampleClass.POSITIVE));
      }
      prover.pop();

      prover.push(postconditionFormula);
      if (!prover.isUnsat()) {
        Iterable<ValueAssignment> model = prover.getModelAssignments();
        pPostSamples.add(
            SampleUtils.extractSampleFromRelevantAssignments(
                model, pLocation, SampleClass.NEGATIVE));
      }
      prover.pop();
    }
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
