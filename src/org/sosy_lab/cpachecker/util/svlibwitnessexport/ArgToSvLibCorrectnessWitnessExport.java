// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2025 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.util.svlibwitnessexport;

import com.google.common.base.Verify;
import com.google.common.collect.FluentIterable;
import com.google.common.collect.HashMultimap;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Multimap;
import com.google.common.collect.SetMultimap;
import com.google.common.graph.Traverser;
import java.util.ArrayDeque;
import java.util.Collection;
import java.util.Deque;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import org.checkerframework.checker.nullness.qual.NonNull;
import org.sosy_lab.common.log.LogManager;
import org.sosy_lab.cpachecker.cfa.CFA;
import org.sosy_lab.cpachecker.cfa.ast.FileLocation;
import org.sosy_lab.cpachecker.cfa.ast.svlib.SvLibBooleanConstantTerm;
import org.sosy_lab.cpachecker.cfa.ast.svlib.SvLibIdTerm;
import org.sosy_lab.cpachecker.cfa.ast.svlib.SvLibIdTermReplacer;
import org.sosy_lab.cpachecker.cfa.ast.svlib.SvLibParameterDeclaration;
import org.sosy_lab.cpachecker.cfa.ast.svlib.SvLibTerm;
import org.sosy_lab.cpachecker.cfa.ast.svlib.SvLibVariableDeclaration;
import org.sosy_lab.cpachecker.cfa.ast.svlib.specification.SvLibAtTerm;
import org.sosy_lab.cpachecker.cfa.ast.svlib.specification.SvLibEnsuresTag;
import org.sosy_lab.cpachecker.cfa.ast.svlib.specification.SvLibInvariantTag;
import org.sosy_lab.cpachecker.cfa.ast.svlib.specification.SvLibRelationalTerm;
import org.sosy_lab.cpachecker.cfa.ast.svlib.specification.SvLibRequiresTag;
import org.sosy_lab.cpachecker.cfa.ast.svlib.specification.SvLibTagReference;
import org.sosy_lab.cpachecker.cfa.model.CFAEdge;
import org.sosy_lab.cpachecker.cfa.model.CFALabelNode;
import org.sosy_lab.cpachecker.cfa.model.CFANode;
import org.sosy_lab.cpachecker.cfa.model.FunctionCallEdge;
import org.sosy_lab.cpachecker.cfa.model.FunctionEntryNode;
import org.sosy_lab.cpachecker.cfa.model.FunctionExitNode;
import org.sosy_lab.cpachecker.cfa.model.svlib.SvLibCfaMetadata;
import org.sosy_lab.cpachecker.cfa.parser.svlib.antlr.SvLibScope;
import org.sosy_lab.cpachecker.cfa.parser.svlib.ast.commands.SvLibAnnotateTagCommand;
import org.sosy_lab.cpachecker.core.interfaces.AbstractState;
import org.sosy_lab.cpachecker.cpa.arg.ARGState;
import org.sosy_lab.cpachecker.util.AbstractStates;
import org.sosy_lab.cpachecker.util.CFAUtils;
import org.sosy_lab.cpachecker.util.witnesses.RelevantArgStatesCollector.CollectedARGStates;
import org.sosy_lab.cpachecker.util.witnesses.RelevantArgStatesCollector.FunctionEntryExitPair;
import org.sosy_lab.cpachecker.util.witnesses.RootExplorationArgStateCollector;

public class ArgToSvLibCorrectnessWitnessExport {
  private final SvLibCfaMetadata svLibMetadata;

  private final ImmutableSet<CFANode> loopHeads;
  private final ImmutableSet<String> recursiveFunctions;

  @SuppressWarnings("unused")
  private final LogManager logger;

  public ArgToSvLibCorrectnessWitnessExport(CFA pCFA, LogManager pLogger) {
    Verify.verify(
        pCFA.getMetadata().getSvLibCfaMetadata().isPresent(),
        "SV-LIB metadata must be present in CFA in order to export a SV-LIB witness.");
    svLibMetadata = pCFA.getMetadata().getSvLibCfaMetadata().orElseThrow();
    loopHeads = pCFA.getAllLoopHeads().orElse(ImmutableSet.of());
    recursiveFunctions = recursiveFunctions(pCFA);
    logger = pLogger;
  }

  /** The functions that can call themselves, directly or through other functions. */
  private static ImmutableSet<String> recursiveFunctions(CFA pCFA) {
    SetMultimap<String, String> callees = HashMultimap.create();
    for (CFAEdge edge : CFAUtils.allEdges(pCFA).filter(FunctionCallEdge.class)) {
      callees.put(edge.getPredecessor().getFunctionName(), edge.getSuccessor().getFunctionName());
    }
    ImmutableSet.Builder<String> recursive = ImmutableSet.builder();
    for (String function : callees.keySet()) {
      Set<String> reached = new HashSet<>();
      Deque<String> waitlist = new ArrayDeque<>(callees.get(function));
      while (!waitlist.isEmpty()) {
        String callee = waitlist.pop();
        if (reached.add(callee)) {
          waitlist.addAll(callees.get(callee));
        }
      }
      if (reached.contains(function)) {
        recursive.add(function);
      }
    }
    return recursive.build();
  }

  protected SvLibTerm getOverapproximationOfStates(
      Collection<ARGState> pArgStates, SvLibScope pScope) throws InterruptedException {
    FluentIterable<SvLibTermReportingState> reportingStates =
        FluentIterable.from(pArgStates)
            .transformAndConcat(AbstractStates::asIterable)
            .filter(SvLibTermReportingState.class);
    ImmutableSet.Builder<SvLibTerm> expressionsPerClass = new ImmutableSet.Builder<>();

    for (Class<?> stateClass : reportingStates.transform(AbstractState::getClass).toSet()) {
      ImmutableSet.Builder<SvLibTerm> expressionsMatchingClass = new ImmutableSet.Builder<>();
      for (SvLibTermReportingState state : reportingStates) {
        if (stateClass.isAssignableFrom(state.getClass())) {
          expressionsMatchingClass.add(state.asSvLibTerm(pScope));
        }
      }
      SvLibTerm disjunctionOfClass =
          SvLibTerm.booleanDisjunction(expressionsMatchingClass.build().asList());

      expressionsPerClass.add(disjunctionOfClass);
    }

    return SvLibTerm.booleanConjunction(expressionsPerClass.build().asList());
  }

  @NonNull
  public SvLibAnnotateTagCommand createRequires(
      Collection<ARGState> argStates, SvLibTagReference pTag) throws InterruptedException {
    SvLibTerm precondition =
        getOverapproximationOfStates(argStates, svLibMetadata.tagReferenceToScope().get(pTag));
    return new SvLibAnnotateTagCommand(
        pTag.getTagName(),
        ImmutableList.of(new SvLibRequiresTag(precondition, FileLocation.DUMMY)),
        FileLocation.DUMMY);
  }

  @NonNull
  public SvLibAnnotateTagCommand createEnsures(
      Collection<FunctionEntryExitPair> pArgStates, SvLibTagReference pTag)
      throws InterruptedException {
    // Build state for precondition
    ImmutableSet.Builder<SvLibRelationalTerm> ensuresTerms = new ImmutableSet.Builder<>();
    for (FunctionEntryExitPair pair : pArgStates) {
      SvLibRelationalTerm precondition =
          getOverapproximationOfStates(
              ImmutableList.of(pair.entry()), svLibMetadata.tagReferenceToScope().get(pTag));
      SvLibTerm postcondition =
          getOverapproximationOfStates(
              ImmutableList.of(pair.exit()), svLibMetadata.tagReferenceToScope().get(pTag));

      // Replace all variables in the postcondition with their final(...) counterparts
      SvLibIdTermReplacer oldReplacer =
          new SvLibIdTermReplacer() {

            @Override
            public SvLibRelationalTerm replace(SvLibIdTerm pIdTerm) {
              if (pIdTerm.getDeclaration() instanceof SvLibVariableDeclaration
                  || pIdTerm.getDeclaration() instanceof SvLibParameterDeclaration) {
                return new SvLibAtTerm(FileLocation.DUMMY, pTag, pIdTerm);
              }
              return pIdTerm;
            }
          };
      precondition = precondition.accept(oldReplacer);

      ensuresTerms.add(SvLibRelationalTerm.implication(precondition, postcondition));
    }

    return new SvLibAnnotateTagCommand(
        pTag.getTagName(),
        ImmutableList.of(
            new SvLibEnsuresTag(
                SvLibRelationalTerm.booleanConjunction(ensuresTerms.build().asList()),
                FileLocation.DUMMY)),
        FileLocation.DUMMY);
  }

  @NonNull
  private SvLibAnnotateTagCommand createLoopInvariant(
      Collection<ARGState> argStates, SvLibTagReference pTag) throws InterruptedException {
    return new SvLibAnnotateTagCommand(
        pTag.getTagName(),
        ImmutableList.of(
            new SvLibInvariantTag(
                getOverapproximationOfStates(
                    argStates, svLibMetadata.tagReferenceToScope().get(pTag)),
                FileLocation.DUMMY)),
        FileLocation.DUMMY);
  }

  public List<SvLibAnnotateTagCommand> generateWitnessCommands(ARGState pRootState)
      throws InterruptedException {
    CollectedARGStates relevantStates =
        new RootExplorationArgStateCollector(loopHeads).getRelevantStates(pRootState);

    ImmutableList.Builder<SvLibAnnotateTagCommand> witnessCommands = ImmutableList.builder();

    // First create the loop invariants
    Multimap<CFANode, ARGState> loopInvariants = relevantStates.loopInvariants();
    for (CFANode node : loopInvariants.keySet()) {
      Collection<ARGState> argStates = loopInvariants.get(node);
      Set<SvLibTagReference> correspondingTag = svLibMetadata.tagReferences().get(node);
      // We export the loop invariant for each tag reference at the node, to
      // find errors easier in the export and matching them to the source code
      for (SvLibTagReference tag : correspondingTag) {
        SvLibAnnotateTagCommand loopInvariantCommand = createLoopInvariant(argStates, tag);
        witnessCommands.add(loopInvariantCommand);
      }
    }

    // A validator needs an invariant on each cycle, also on the ones that the ARG does not reach.
    // These include cycles of labels in dead code, which the CFA does not contain.
    ImmutableSet<CFANode> reachedNodes =
        FluentIterable.from(
                Traverser.forGraph(ARGState::getChildren).depthFirstPreOrder(pRootState))
            .transform(AbstractStates::extractLocation)
            .filter(Objects::nonNull)
            .toSet();
    for (CFANode node : svLibMetadata.tagReferences().keySet()) {
      if ((loopHeads.contains(node) || node instanceof CFALabelNode)
          && !reachedNodes.contains(node)) {
        for (SvLibTagReference tag : svLibMetadata.tagReferences().get(node)) {
          witnessCommands.add(
              new SvLibAnnotateTagCommand(
                  tag.getTagName(),
                  ImmutableList.of(
                      new SvLibInvariantTag(
                          new SvLibBooleanConstantTerm(false, FileLocation.DUMMY),
                          FileLocation.DUMMY)),
                  FileLocation.DUMMY));
        }
      }
    }

    // Handle the statement contracts
    Multimap<FunctionEntryNode, ARGState> functionContractRequires =
        relevantStates.functionContractRequires();
    Multimap<FunctionExitNode, FunctionEntryExitPair> functionContractEnsures =
        relevantStates.functionContractEnsures();
    for (FunctionEntryNode functionEntryNode : functionContractRequires.keySet()) {
      // A validator inlines the call of a function without a contract, which is at least as
      // precise as a contract from the abstract states, so only recursive functions get one
      if (!recursiveFunctions.contains(functionEntryNode.getFunctionName())) {
        continue;
      }
      Collection<ARGState> requiresArgStates = functionContractRequires.get(functionEntryNode);
      Set<SvLibTagReference> correspondingTag =
          svLibMetadata.tagReferences().get(functionEntryNode);
      // We export the loop invariant for each tag reference at the node, to
      // find errors easier in the export and matching them to the source code
      for (SvLibTagReference tag : correspondingTag) {
        SvLibAnnotateTagCommand requiresCommand = createRequires(requiresArgStates, tag);
        witnessCommands.add(requiresCommand);

        if (functionEntryNode.getExitNode().isPresent()
            && functionContractEnsures.containsKey(functionEntryNode.getExitNode().orElseThrow())) {
          Collection<FunctionEntryExitPair> ensuresArgStates =
              functionContractEnsures.get(functionEntryNode.getExitNode().orElseThrow());
          SvLibAnnotateTagCommand ensuresCommand = createEnsures(ensuresArgStates, tag);
          witnessCommands.add(ensuresCommand);
        }
      }
    }

    return witnessCommands.build();
  }
}
