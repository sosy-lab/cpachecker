// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2025 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.util.k3witnessexport;

import com.google.common.base.Verify;
import com.google.common.collect.FluentIterable;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Multimap;
import java.util.Collection;
import java.util.List;
import java.util.Set;
import org.checkerframework.checker.nullness.qual.NonNull;
import org.sosy_lab.common.configuration.Configuration;
import org.sosy_lab.common.configuration.InvalidConfigurationException;
import org.sosy_lab.common.log.LogManager;
import org.sosy_lab.cpachecker.cfa.CFA;
import org.sosy_lab.cpachecker.cfa.ast.FileLocation;
import org.sosy_lab.cpachecker.cfa.ast.k3.K3AnnotateTagCommand;
import org.sosy_lab.cpachecker.cfa.ast.k3.K3EnsuresTag;
import org.sosy_lab.cpachecker.cfa.ast.k3.K3FinalRelationalTerm;
import org.sosy_lab.cpachecker.cfa.ast.k3.K3FinalTerm;
import org.sosy_lab.cpachecker.cfa.ast.k3.K3InvariantTag;
import org.sosy_lab.cpachecker.cfa.ast.k3.K3ParameterDeclaration;
import org.sosy_lab.cpachecker.cfa.ast.k3.K3RequiresTag;
import org.sosy_lab.cpachecker.cfa.ast.k3.K3TagReference;
import org.sosy_lab.cpachecker.cfa.ast.k3.K3Term;
import org.sosy_lab.cpachecker.cfa.ast.k3.K3VariableDeclaration;
import org.sosy_lab.cpachecker.cfa.ast.k3.builder.K3IdTermReplacer;
import org.sosy_lab.cpachecker.cfa.ast.k3.builder.K3TermBuilder;
import org.sosy_lab.cpachecker.cfa.ast.k3.parser.K3Scope;
import org.sosy_lab.cpachecker.cfa.model.CFANode;
import org.sosy_lab.cpachecker.cfa.model.FunctionEntryNode;
import org.sosy_lab.cpachecker.cfa.model.FunctionExitNode;
import org.sosy_lab.cpachecker.cfa.model.k3.K3CfaMetadata;
import org.sosy_lab.cpachecker.core.interfaces.AbstractState;
import org.sosy_lab.cpachecker.core.interfaces.ExpressionTreeReportingState;
import org.sosy_lab.cpachecker.core.interfaces.ExpressionTreeReportingState.ReportingMethodNotImplementedException;
import org.sosy_lab.cpachecker.core.reachedset.UnmodifiableReachedSet;
import org.sosy_lab.cpachecker.core.specification.Specification;
import org.sosy_lab.cpachecker.cpa.arg.ARGState;
import org.sosy_lab.cpachecker.util.AbstractStates;
import org.sosy_lab.cpachecker.util.witnesses.ArgAnalysisUtils;
import org.sosy_lab.cpachecker.util.witnesses.ArgAnalysisUtils.CollectedARGStates;
import org.sosy_lab.cpachecker.util.witnesses.ArgAnalysisUtils.FunctionEntryExitPair;

public class ArgToK3CorrectnessWitnessExport {
  private final K3CfaMetadata k3Metadata;
  private final Specification specification;
  private final LogManager logger;

  public ArgToK3CorrectnessWitnessExport(
      Configuration pConfig, CFA pCFA, Specification pSpecification, LogManager pLogger)
      throws InvalidConfigurationException {
    // pConfig.inject(this);
    Verify.verify(
        pCFA.getMetadata().getK3CfaMetadata().isPresent(),
        "K3 metadata must be present in CFA in order to export a K3 witness.");
    k3Metadata = pCFA.getMetadata().getK3CfaMetadata().orElseThrow();
    specification = pSpecification;
    logger = pLogger;
  }

  protected K3FinalRelationalTerm getOverapproximationOfStates(
      Collection<ARGState> pArgStates, K3Scope pScope)
      throws InterruptedException, ReportingMethodNotImplementedException {
    FluentIterable<ExpressionTreeReportingState> reportingStates =
        FluentIterable.from(pArgStates)
            .transformAndConcat(AbstractStates::asIterable)
            .filter(ExpressionTreeReportingState.class);
    ImmutableSet.Builder<K3FinalRelationalTerm> expressionsPerClass = new ImmutableSet.Builder<>();

    for (Class<?> stateClass : reportingStates.transform(AbstractState::getClass).toSet()) {
      ImmutableSet.Builder<K3FinalRelationalTerm> expressionsMatchingClass =
          new ImmutableSet.Builder<>();
      for (ExpressionTreeReportingState state : reportingStates) {
        if (stateClass.isAssignableFrom(state.getClass())) {
          expressionsMatchingClass.add(state.asK3Term(pScope));
        }
      }
      K3FinalRelationalTerm disjunctionOfClass =
          K3TermBuilder.booleanDisjunction(expressionsMatchingClass.build().asList());

      expressionsPerClass.add(disjunctionOfClass);
    }

    return K3TermBuilder.booleanConjunction(expressionsPerClass.build().asList());
  }

  @NonNull
  public K3AnnotateTagCommand createRequires(Collection<ARGState> argStates, K3TagReference pTag)
      throws ReportingMethodNotImplementedException, InterruptedException {
    K3FinalRelationalTerm precondition = getOverapproximationOfStates(argStates, pTag.getScope());
    if (!(precondition instanceof K3Term term)) {
      throw new AssertionError(
          "Precondition is not a K3 term, it contains final commands: " + precondition);
    }
    return new K3AnnotateTagCommand(
        pTag.getTagName(),
        ImmutableList.of(new K3RequiresTag(term, FileLocation.DUMMY)),
        FileLocation.DUMMY);
  }

  @NonNull
  public K3AnnotateTagCommand createEnsures(
      Collection<FunctionEntryExitPair> pArgStates, K3TagReference pTag)
      throws ReportingMethodNotImplementedException, InterruptedException {
    // Build state for precondition
    ImmutableSet.Builder<K3FinalRelationalTerm> ensuresTerms = new ImmutableSet.Builder<>();
    for (FunctionEntryExitPair pair : pArgStates) {
      K3FinalRelationalTerm precondition =
          getOverapproximationOfStates(ImmutableList.of(pair.entry()), pTag.getScope());
      K3FinalRelationalTerm postcondition =
          getOverapproximationOfStates(ImmutableList.of(pair.exit()), pTag.getScope());

      // Replace all variables in the postcondition with their final(...) counterparts
      K3IdTermReplacer finalReplacer =
          new K3IdTermReplacer(
              idTerm -> {
                if (idTerm.getDeclaration() instanceof K3VariableDeclaration
                    || idTerm.getDeclaration() instanceof K3ParameterDeclaration) {
                  return new K3FinalTerm(FileLocation.DUMMY, idTerm);
                }
                return idTerm;
              });
      postcondition = postcondition.accept(finalReplacer);

      ensuresTerms.add(K3TermBuilder.implication(precondition, postcondition));
    }

    return new K3AnnotateTagCommand(
        pTag.getTagName(),
        ImmutableList.of(
            new K3EnsuresTag(
                K3TermBuilder.booleanDisjunction(ensuresTerms.build().asList()),
                FileLocation.DUMMY)),
        FileLocation.DUMMY);
  }

  @NonNull
  private K3AnnotateTagCommand createLoopInvariant(
      Collection<ARGState> argStates, K3TagReference pTag)
      throws ReportingMethodNotImplementedException, InterruptedException {
    return new K3AnnotateTagCommand(
        pTag.getTagName(),
        ImmutableList.of(
            new K3InvariantTag(
                getOverapproximationOfStates(argStates, pTag.getScope()), FileLocation.DUMMY)),
        FileLocation.DUMMY);
  }

  public List<K3AnnotateTagCommand> generateWitnessCommands(
      ARGState pRootState, UnmodifiableReachedSet pReached)
      throws ReportingMethodNotImplementedException, InterruptedException {
    CollectedARGStates relevantStates = ArgAnalysisUtils.getRelevantStates(pRootState);

    ImmutableList.Builder<K3AnnotateTagCommand> witnessCommands = ImmutableList.builder();

    // First create the loop invariants
    Multimap<CFANode, ARGState> loopInvariants = relevantStates.loopInvariants;
    for (CFANode node : loopInvariants.keySet()) {
      Collection<ARGState> argStates = loopInvariants.get(node);
      Set<K3TagReference> correspondingTag = k3Metadata.tagReferences().get(node);
      // We export the loop invariant for each tag reference at the node, to
      // find errors easier in the export and matching them to the source code
      for (K3TagReference tag : correspondingTag) {
        K3AnnotateTagCommand loopInvariantCommand = createLoopInvariant(argStates, tag);
        witnessCommands.add(loopInvariantCommand);
      }
    }

    // Handle the statement contracts
    Multimap<FunctionEntryNode, ARGState> functionContractRequires =
        relevantStates.functionContractRequires;
    Multimap<FunctionExitNode, FunctionEntryExitPair> functionContractEnsures =
        relevantStates.functionContractEnsures;
    for (FunctionEntryNode functionEntryNode : functionContractRequires.keySet()) {
      Collection<ARGState> requiresArgStates = functionContractRequires.get(functionEntryNode);
      Set<K3TagReference> correspondingTag = k3Metadata.tagReferences().get(functionEntryNode);
      // We export the loop invariant for each tag reference at the node, to
      // find errors easier in the export and matching them to the source code
      for (K3TagReference tag : correspondingTag) {
        K3AnnotateTagCommand requiresCommand = createRequires(requiresArgStates, tag);
        witnessCommands.add(requiresCommand);

        if (functionEntryNode.getExitNode().isPresent()
            && functionContractEnsures.containsKey(functionEntryNode.getExitNode().orElseThrow())) {
          Collection<FunctionEntryExitPair> ensuresArgStates =
              functionContractEnsures.get(functionEntryNode.getExitNode().orElseThrow());
          K3AnnotateTagCommand ensuresCommand = createEnsures(ensuresArgStates, tag);
          witnessCommands.add(ensuresCommand);
        }
      }
    }

    return witnessCommands.build();
  }
}
