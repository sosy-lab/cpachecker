// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2026 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.util.yamlwitnessexport;

import com.google.common.collect.FluentIterable;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableListMultimap;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Lists;
import java.io.IOException;
import java.nio.file.Path;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.logging.Level;
import org.checkerframework.checker.nullness.qual.Nullable;
import org.sosy_lab.common.configuration.Configuration;
import org.sosy_lab.common.configuration.InvalidConfigurationException;
import org.sosy_lab.common.configuration.Option;
import org.sosy_lab.common.configuration.Options;
import org.sosy_lab.common.log.LogManager;
import org.sosy_lab.cpachecker.cfa.CFA;
import org.sosy_lab.cpachecker.cfa.ast.c.CAstNode;
import org.sosy_lab.cpachecker.cfa.ast.c.CEnumerator;
import org.sosy_lab.cpachecker.cfa.ast.c.CExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CFunctionDeclaration;
import org.sosy_lab.cpachecker.cfa.ast.c.CIdExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CSimpleDeclaration;
import org.sosy_lab.cpachecker.cfa.ast.c.SubstitutingCAstNodeVisitor;
import org.sosy_lab.cpachecker.cfa.model.AssumeEdge;
import org.sosy_lab.cpachecker.cfa.model.BlankEdge;
import org.sosy_lab.cpachecker.cfa.model.CFAEdge;
import org.sosy_lab.cpachecker.cfa.model.CFALabelNode;
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.SequentializationMapping;
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.SequentializationMapping.BlockOrigin;
import org.sosy_lab.cpachecker.core.counterexample.CFAEdgeWithAssumptions;
import org.sosy_lab.cpachecker.core.counterexample.CounterexampleInfo;
import org.sosy_lab.cpachecker.core.specification.Specification;
import org.sosy_lab.cpachecker.util.CFAUtils;

/**
 * Exports a violation witness for a counterexample that was found by analyzing a sequentialization
 * of a concurrent program. The witness refers to the concurrent input program, not to the
 * sequentialization that was analyzed.
 *
 * <p>The counterexample is projected onto the input program first. The statements of a thread are
 * written as blocks preceded by a label, so an edge whose predecessor is a {@link CFALabelNode} of
 * such a block is the one that carries the semantics of an input program edge; all other edges are
 * dropped.
 */
@Options(prefix = "witness.yamlexporter")
public class SequentializedCounterexampleToWitness extends AbstractCounterexampleToWitness {

  /** Which waypoints to export for a counterexample of a sequentialized program. */
  public enum SequentializedWaypoints {
    /** All waypoints that could be mapped back to the input program. */
    ALL,
    /** Only the target waypoint and the thread creations needed to identify its thread. */
    TARGET_ONLY,
  }

  @Option(
      secure = true,
      description =
          "Which waypoints to export for a counterexample that was found by analyzing a"
              + " sequentialization of a concurrent program. Exporting only the target makes the"
              + " witness easier to validate, because every other waypoint constrains the"
              + " interleaving that a validator has to reproduce.")
  private SequentializedWaypoints sequentializedWaypoints = SequentializedWaypoints.ALL;

  private final SequentializationMapping mapping;

  /**
   * Creates an exporter for the input program of a sequentialization.
   *
   * @param pOriginalCfa the CFA of the concurrent input program, i.e. {@code
   *     MporSequentialization#originalCfa()}. All waypoints refer to it, so it is also what the
   *     metadata and the AST structure are taken from.
   * @param pMapping relates the elements of the sequentialization to the input program
   */
  public SequentializedCounterexampleToWitness(
      Configuration pConfig,
      CFA pOriginalCfa,
      SequentializationMapping pMapping,
      Specification pSpecification,
      LogManager pLogger)
      throws InvalidConfigurationException {

    super(pConfig, pOriginalCfa, pSpecification, pLogger);
    pConfig.inject(this, SequentializedCounterexampleToWitness.class);
    mapping = pMapping;
  }

  /**
   * Exports {@code pCex}, which belongs to the sequentialization, as a violation witness for the
   * input program it was created from.
   */
  @Override
  protected void exportWitness(
      CounterexampleInfo pCex, Path pPath, YAMLWitnessVersion pWitnessVersion) throws IOException {

    ProjectedCounterexample projected =
        project(pCex, sequentializedWaypoints == SequentializedWaypoints.ALL);

    if (projected.steps().isEmpty()) {
      logger.log(
          Level.INFO,
          "No step of the counterexample for the sequentialized program could be mapped back to the"
              + " input program, therefore no violation witness will be exported.");
      return;
    }
    if (!projected.violationIsInSimulatedCode()) {
      logger.log(
          Level.INFO,
          "The violation in the sequentialized program happens in its simulation of the thread"
              + " scheduling, which has no counterpart in the input program, therefore no violation"
              + " witness will be exported.");
      return;
    }

    ImmutableList<WitnessPathStep> steps = reduceWaypoints(projected.steps());
    exportEntries(
        buildViolationSequence(
            steps,
            violatingStep(steps).currentThread(),
            projected.edgeToAssumptions(),
            pWitnessVersion),
        pPath);
  }

  /**
   * Reduces the steps to those needed by {@link #sequentializedWaypoints}. Thread creations are
   * always kept, because the witness identifies a thread by the position of its creation.
   */
  private ImmutableList<WitnessPathStep> reduceWaypoints(ImmutableList<WitnessPathStep> pSteps) {
    if (sequentializedWaypoints == SequentializedWaypoints.ALL) {
      return pSteps;
    }
    WitnessPathStep violatingStep = violatingStep(pSteps);
    ImmutableList.Builder<WitnessPathStep> rSteps = ImmutableList.builder();
    for (WitnessPathStep step : pSteps) {
      if (step.equals(violatingStep)) {
        break;
      }
      if (step.createdThread().isPresent()) {
        rSteps.add(step);
      }
    }
    return rSteps.add(violatingStep).build();
  }

  /**
   * A counterexample for a sequentialized program, expressed in terms of the input program.
   *
   * @param steps the steps that have a counterpart in the input program, in execution order
   * @param edgeToAssumptions the constraints holding after an input program edge, in execution
   *     order. There is exactly one entry per step, so that the n-th execution of an edge uses the
   *     n-th constraint of that edge. Empty if the constraints were not requested.
   * @param violationIsInSimulatedCode whether the violation happens in code that simulates the
   *     input program, rather than in the simulation of the thread scheduling
   */
  record ProjectedCounterexample(
      ImmutableList<WitnessPathStep> steps,
      ImmutableListMultimap<CFAEdge, String> edgeToAssumptions,
      boolean violationIsInSimulatedCode) {}

  /**
   * Projects {@code pCex} for a sequentialized program onto the input program.
   *
   * @param pWithAssumptions whether the constraints are needed, translating them is not cheap
   */
  ProjectedCounterexample project(CounterexampleInfo pCex, boolean pWithAssumptions) {
    List<CFAEdge> edges = pCex.getTargetPath().getFullPath();
    // the assumptions are listed in the order of the edges they hold after, see
    // CFAPathWithAssumptions. They are missing if the counterexample is not precise.
    List<CFAEdgeWithAssumptions> assumptions =
        pWithAssumptions && pCex.isPreciseCounterExample()
            ? pCex.getCFAPathWithAssignments()
            : ImmutableList.of();
    boolean assumptionsMatchPath = assumptions.size() == edges.size();
    if (pWithAssumptions && !assumptionsMatchPath && !assumptions.isEmpty()) {
      logger.log(
          Level.INFO,
          "The assumptions of the counterexample do not follow its path, so the violation witness"
              + " will not constrain any value.");
    }

    Set<Integer> createdThreads = newCreatedThreads();
    ImmutableList.Builder<WitnessPathStep> rSteps = ImmutableList.builder();
    ImmutableListMultimap.Builder<CFAEdge, String> rAssumptions = ImmutableListMultimap.builder();
    for (int i = 0; i < edges.size(); i++) {
      Optional<WitnessPathStep> step = projectEdge(edges.get(i), createdThreads);
      if (step.isEmpty()) {
        continue;
      }
      rSteps.add(step.orElseThrow());
      if (pWithAssumptions) {
        // exactly one constraint per step, so that the indices of repeated edges stay aligned
        rAssumptions.put(
            step.orElseThrow().edge(),
            projectAssumptions(assumptionsMatchPath ? assumptions.get(i) : null));
      }
    }
    return new ProjectedCounterexample(
        rSteps.build(), rAssumptions.build(), violationIsInSimulatedCode(edges));
  }

  /** The threads whose statements may be exported, i.e. whose creation was projected before. */
  static Set<Integer> newCreatedThreads() {
    Set<Integer> createdThreads = new HashSet<>();
    createdThreads.add(0);
    return createdThreads;
  }

  static String threadName(int pThreadId) {
    return pThreadId == 0 ? MAIN_THREAD_NAME : "thread" + pThreadId;
  }

  /**
   * Returns the step of the input program that {@code pEdge} of the sequentialization simulates, or
   * an empty {@link Optional} if it simulates none. Edges must be passed in the order in which they
   * are executed, because a thread can only be referred to after its creation was projected, which
   * {@code pCreatedThreads} keeps track of.
   */
  Optional<WitnessPathStep> projectEdge(CFAEdge pEdge, Set<Integer> pCreatedThreads) {
    Optional<BlockOrigin> blockOrigin = blockOriginOf(pEdge);
    if (blockOrigin.isEmpty()) {
      return Optional.empty();
    }
    int threadId = blockOrigin.orElseThrow().threadId();
    if (!pCreatedThreads.contains(threadId)) {
      // the creation of this thread was not exported, so referring to it would be meaningless
      return Optional.empty();
    }
    Optional<CFAEdge> originalEdge = originalEdgeOf(blockOrigin.orElseThrow(), pEdge);
    if (originalEdge.isEmpty()) {
      return Optional.empty();
    }

    // a thread may only be referred to once the waypoint that introduces it can be exported
    Integer createdThreadId = null;
    if (introducesThread(originalEdge.orElseThrow(), getASTStructure())) {
      createdThreadId = mapping.threadIdByCreationEdge().get(originalEdge.orElseThrow());
      if (createdThreadId != null) {
        pCreatedThreads.add(createdThreadId);
      }
    }
    return Optional.of(
        new WitnessPathStep(
            originalEdge.orElseThrow(),
            Optional.of(threadName(threadId)),
            Optional.ofNullable(createdThreadId)
                .map(SequentializedCounterexampleToWitness::threadName)));
  }

  /**
   * Returns the input program edge that {@code pEdge} of the sequentialization simulates, or an
   * empty {@link Optional} if it simulates none, e.g. because it belongs to the simulation of the
   * thread scheduling.
   */
  private Optional<BlockOrigin> blockOriginOf(CFAEdge pEdge) {
    if (pEdge.getPredecessor() instanceof CFALabelNode labelNode) {
      return Optional.ofNullable(mapping.blockOriginsByLabel().get(labelNode.getLabel()));
    }
    return Optional.empty();
  }

  private static Optional<CFAEdge> originalEdgeOf(BlockOrigin pBlockOrigin, CFAEdge pEdge) {
    ImmutableList<CFAEdge> originalEdges = pBlockOrigin.originalEdgeByStatement();
    if (originalEdges.size() == 1) {
      return Optional.of(originalEdges.getFirst());
    }
    // a block with several statements simulates an assume edge, the first statement is the one of
    // the 'then' branch
    if (originalEdges.size() == 2 && pEdge instanceof AssumeEdge assumeEdge) {
      return Optional.of(originalEdges.get(assumeEdge.getTruthAssumption() ? 0 : 1));
    }
    return Optional.empty();
  }

  /**
   * Returns the constraints of a single step, expressed over the variables of the input program.
   * Constraints that talk about variables without a counterpart in the input program are dropped.
   */
  private String projectAssumptions(@Nullable CFAEdgeWithAssumptions pEdgeWithAssumptions) {
    FluentIterable<CExpression> assumptions =
        pEdgeWithAssumptions == null
            ? FluentIterable.of()
            : getAssumptions(pEdgeWithAssumptions)
                .transform(this::toOriginalVocabulary)
                .filter(Optional::isPresent)
                .transform(Optional::orElseThrow);
    return buildAssumptionConstraint(assumptions);
  }

  /**
   * Whether the violation happens in code that simulates the input program. It can instead happen
   * in the simulation of the thread scheduling, e.g. an overflow of a program counter, which has no
   * counterpart in the input program and is recognized by the violating edge using only variables
   * that the input program does not have.
   */
  private boolean violationIsInSimulatedCode(List<CFAEdge> pEdges) {
    for (CFAEdge edge : Lists.reverse(pEdges)) {
      if (!(edge instanceof BlankEdge)) {
        return !onlyUsesGeneratedVariables(edge);
      }
    }
    return false;
  }

  /**
   * Whether {@code pEdge} uses at least one variable and none of them exists in the input program,
   * i.e. whether it belongs to the simulation of the thread scheduling.
   */
  private boolean onlyUsesGeneratedVariables(CFAEdge pEdge) {
    boolean usesVariable = false;
    for (CSimpleDeclaration declaration : variablesOf(pEdge)) {
      if (mapping.originalDeclarationsBySubstituteName().containsKey(declaration.getName())) {
        return false;
      }
      usesVariable = true;
    }
    return usesVariable;
  }

  /** Returns the declarations of the variables that {@code pEdge} uses. */
  private static FluentIterable<CSimpleDeclaration> variablesOf(CFAEdge pEdge) {
    return FluentIterable.from(CFAUtils.getAstNodesFromCfaEdge(pEdge))
        .filter(CAstNode.class)
        .transformAndConcat(CFAUtils::traverseRecursively)
        .filter(CIdExpression.class)
        .transform(CIdExpression::getDeclaration)
        .filter(CSimpleDeclaration.class)
        .filter(declaration -> !(declaration instanceof CFunctionDeclaration));
  }

  /**
   * Rewrites an expression over variables of the sequentialization into one over the variables of
   * the input program, or returns an empty {@link Optional} if it mentions a variable that has no
   * counterpart in the input program.
   */
  Optional<CExpression> toOriginalVocabulary(CExpression pExpression) {
    ImmutableMap<String, CSimpleDeclaration> originalDeclarations =
        mapping.originalDeclarationsBySubstituteName();
    boolean[] isTranslatable = {true};

    SubstitutingCAstNodeVisitor visitor =
        new SubstitutingCAstNodeVisitor(
            astNode -> {
              if (!(astNode instanceof CIdExpression idExpression)) {
                return null;
              }
              CSimpleDeclaration declaration = idExpression.getDeclaration();
              if (declaration == null) {
                isTranslatable[0] = false;
                return null;
              }
              if (declaration instanceof CEnumerator) {
                // enum constants of the input program are used unchanged
                return idExpression;
              }
              CSimpleDeclaration original = originalDeclarations.get(declaration.getName());
              if (original == null) {
                isTranslatable[0] = false;
                return null;
              }
              return new CIdExpression(idExpression.getFileLocation(), original);
            });

    CAstNode translated = pExpression.accept(visitor);
    if (!isTranslatable[0] || !(translated instanceof CExpression translatedExpression)) {
      return Optional.empty();
    }
    return Optional.of(translatedExpression);
  }
}
