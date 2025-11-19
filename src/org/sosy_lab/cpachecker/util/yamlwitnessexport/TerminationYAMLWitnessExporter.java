// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2025 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.util.yamlwitnessexport;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Multimap;
import de.uni_freiburg.informatik.ultimate.lassoranker.termination.AffineFunction;
import de.uni_freiburg.informatik.ultimate.lassoranker.termination.SupportingInvariant;
import de.uni_freiburg.informatik.ultimate.lassoranker.termination.TerminationArgument;
import de.uni_freiburg.informatik.ultimate.lassoranker.termination.rankingfunctions.NestedRankingFunction;
import de.uni_freiburg.informatik.ultimate.lassoranker.termination.rankingfunctions.RankingFunction;
import de.uni_freiburg.informatik.ultimate.lib.modelcheckerutils.cfg.variables.IProgramVar;
import java.io.IOException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.logging.Level;
import org.sosy_lab.common.configuration.Configuration;
import org.sosy_lab.common.configuration.InvalidConfigurationException;
import org.sosy_lab.common.io.PathTemplate;
import org.sosy_lab.common.log.LogManager;
import org.sosy_lab.cpachecker.cfa.CFA;
import org.sosy_lab.cpachecker.cfa.ast.FileLocation;
import org.sosy_lab.cpachecker.cfa.model.CFAEdge;
import org.sosy_lab.cpachecker.cfa.model.CFANode;
import org.sosy_lab.cpachecker.core.algorithm.termination.validation.well_foundedness.TransitionInvariantUtils;
import org.sosy_lab.cpachecker.core.specification.Specification;
import org.sosy_lab.cpachecker.util.LoopStructure.Loop;
import org.sosy_lab.cpachecker.util.yamlwitnessexport.model.AbstractInvariantEntry;
import org.sosy_lab.cpachecker.util.yamlwitnessexport.model.InvariantEntry;
import org.sosy_lab.cpachecker.util.yamlwitnessexport.model.InvariantEntry.InvariantRecordType;
import org.sosy_lab.cpachecker.util.yamlwitnessexport.model.InvariantSetEntry;
import org.sosy_lab.cpachecker.util.yamlwitnessexport.model.LocationRecord;

public class TerminationYAMLWitnessExporter extends AbstractYAMLWitnessExporter {

  private boolean exportSupportingInvariants;

  public TerminationYAMLWitnessExporter(
      Configuration pConfig,
      CFA pCfa,
      Specification pSpecification,
      LogManager pLogger,
      boolean pExportSupportingInvariants)
      throws InvalidConfigurationException {
    super(pConfig, pCfa, pSpecification, pLogger);
    exportSupportingInvariants = pExportSupportingInvariants;
  }

  private String rightSideOfRankingFunction(String pRankingFunction) {
    int firstEquals = pRankingFunction.indexOf('=');
    return pRankingFunction.substring(firstEquals + 1).trim();
  }

  // The function replaces variable names with annotation \at(..., AnyPrev), i.e. x -> \at(x,
  // AnyPrev) and casts them into a larger type
  private String wrapTheVariablesWithAtAnyPrev(
      String pRankingFunction, Iterable<IProgramVar> pVars) {
    for (IProgramVar var : pVars) {
      String newVarName = "((__int128)\\at(" + var + ", AnyPrev))";
      pRankingFunction = pRankingFunction.replace(var.toString(), newVarName);
    }
    return pRankingFunction;
  }

  // The function casts the variables into (__int128) as we want to prevent overflows in the
  // witness
  private String wrapTheVariablesWithCastToLongLong(
      String pRankingFunction, Iterable<IProgramVar> pVars) {
    for (IProgramVar var : pVars) {
      String newVarName = "((__int128)" + var + ")";
      pRankingFunction = pRankingFunction.replace(var.toString(), newVarName);
    }
    return pRankingFunction;
  }

  private InvariantEntry processSupportingInvariant(
      SupportingInvariant pSupportingInvariant, CFANode pLoopHead, CFAEdge pIncomingLoopEdge) {
    // Ideally, this should be done via AstToCFARelation, however, this breaks due to copying of CFA
    FileLocation fileLocation = pIncomingLoopEdge.getFileLocation();

    LocationRecord locationRecord =
        LocationRecord.createLocationRecordAtStart(
            fileLocation,
            pLoopHead.getFunction().getFileLocation().getFileName().toString(),
            pLoopHead.getFunctionName());
    String invariant =
        wrapTheVariablesWithCastToLongLong(
            pSupportingInvariant.toString(), pSupportingInvariant.getVariables());
    return new InvariantEntry(
        TransitionInvariantUtils.removeFunctionFromVarsName(invariant),
        InvariantRecordType.LOOP_INVARIANT.getKeyword(),
        YAMLWitnessExpressionType.C,
        locationRecord);
  }

  private InvariantEntry processRankingFunction(
      Collection<TerminationArgument> pArguments, CFANode pLoopHead, CFAEdge pIncomingLoopEdge) {
    List<String> transitionInvariants = new ArrayList<>();

    // Ideally, this should be done via AstToCFARelation, however, this breaks due to copying of CFA
    FileLocation fileLocation = pIncomingLoopEdge.getFileLocation();
    LocationRecord locationRecord =
        LocationRecord.createLocationRecordAtStart(
            fileLocation,
            pLoopHead.getFunction().getFileLocation().getFileName().toString(),
            pLoopHead.getFunctionName());
    for (TerminationArgument argument : pArguments) {
      RankingFunction rankingFunction = argument.getRankingFunction();
      if (rankingFunction instanceof NestedRankingFunction pNestedRankingFunction) {
        for (AffineFunction nestedRankingFunction : pNestedRankingFunction.getComponents()) {
          addTransitionInvariant(
              transitionInvariants,
              nestedRankingFunction.toString(),
              nestedRankingFunction.getVariables());
        }
      } else {
        addTransitionInvariant(
            transitionInvariants, rankingFunction.toString(), rankingFunction.getVariables());
      }
    }
    return new InvariantEntry(
        TransitionInvariantUtils.removeFunctionFromVarsName(
            String.join(" || ", transitionInvariants)),
        InvariantRecordType.TRANSITION_LOOP_INVARIANT.getKeyword(),
        YAMLWitnessExpressionType.EXT_C,
        locationRecord);
  }

  private void addTransitionInvariant(
      List<String> transitionInvariants, String rankingFunction, Iterable<IProgramVar> variables) {
    final String TMP_KEYWORD = "__CPAchecker_TMP";
    String prevRank =
        rightSideOfRankingFunction(wrapTheVariablesWithAtAnyPrev(rankingFunction, variables));
    String currentRank =
        rightSideOfRankingFunction(wrapTheVariablesWithCastToLongLong(rankingFunction, variables));
    if (prevRank.contains(TMP_KEYWORD)) {
      transitionInvariants.add("0");
    } else {
      transitionInvariants.add(prevRank + " > " + currentRank);
    }
  }

  private void constructWitness(
      Multimap<Loop, TerminationArgument> pTerminationArguments, Path pPath) throws IOException {
    ImmutableList.Builder<AbstractInvariantEntry> entries = new ImmutableList.Builder<>();

    for (Loop loop : pTerminationArguments.keySet()) {
      CFANode loopHead = loop.getLoopNodes().getFirst();
      CFAEdge incomingLoopEdge = loop.getIncomingEdges().stream().findAny().orElseThrow();
      for (TerminationArgument argument : pTerminationArguments.get(loop)) {
        if (exportSupportingInvariants) {
          // First construct reachability invariants that support the termination argument.
          for (SupportingInvariant supportingInvariant : argument.getSupportingInvariants()) {
            entries.add(
                processSupportingInvariant(supportingInvariant, loopHead, incomingLoopEdge));
          }
        }
      }
      // Construct transition invariants from ranking function
      entries.add(
          processRankingFunction(
              collectArgumentsForNestedLoops(
                  loop, pTerminationArguments.keySet(), pTerminationArguments),
              loopHead,
              incomingLoopEdge));
    }
    exportEntries(
        new InvariantSetEntry(getMetadata(YAMLWitnessVersion.V2d1), entries.build()), pPath);
  }

  private Set<TerminationArgument> collectArgumentsForNestedLoops(
      Loop pLoop, Set<Loop> pAllLoops, Multimap<Loop, TerminationArgument> pTerminationArguments) {
    Set<TerminationArgument> argumentsForNestedLoops = new HashSet<>();
    for (Loop loop : pAllLoops) {
      for (CFAEdge innerEdge : loop.getInnerLoopEdges()) {
        if (pLoop.getLoopHeads().contains(innerEdge.getPredecessor())) {
          argumentsForNestedLoops.addAll(pTerminationArguments.get(loop));
        }
      }
    }
    return argumentsForNestedLoops;
  }

  /**
   * Export YAML witness from termination arguments in form of ranking functions and supporting
   * invariants. Termination property is supported only by witnesses of version 2.1 and higher.
   *
   * @param pTerminationArguments in the form of ranking functions and supporting invariants.
   */
  public void export(
      Multimap<Loop, TerminationArgument> pTerminationArguments, PathTemplate pOutputFileTemplate)
      throws IOException {

    for (YAMLWitnessVersion witnessVersion : ImmutableSet.copyOf(witnessVersions)) {
      Path outputFile = pOutputFileTemplate.getPath(witnessVersion.toString());
      switch (witnessVersion) {
        case V2 ->
            logger.log(
                Level.SEVERE, "Format in version 2.0 does not support termination witnesses.");
        case V2d1 -> constructWitness(pTerminationArguments, outputFile);
      }
    }
  }
}
