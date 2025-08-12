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
import de.uni_freiburg.informatik.ultimate.lassoranker.termination.SupportingInvariant;
import de.uni_freiburg.informatik.ultimate.lassoranker.termination.TerminationArgument;
import de.uni_freiburg.informatik.ultimate.lassoranker.termination.rankingfunctions.RankingFunction;
import de.uni_freiburg.informatik.ultimate.lib.modelcheckerutils.cfg.variables.IProgramVar;
import java.io.IOException;
import java.nio.file.Path;
import java.util.Optional;
import java.util.logging.Level;
import org.sosy_lab.common.configuration.Configuration;
import org.sosy_lab.common.configuration.InvalidConfigurationException;
import org.sosy_lab.common.io.PathTemplate;
import org.sosy_lab.common.log.LogManager;
import org.sosy_lab.cpachecker.cfa.CFA;
import org.sosy_lab.cpachecker.cfa.ast.FileLocation;
import org.sosy_lab.cpachecker.cfa.model.CFANode;
import org.sosy_lab.cpachecker.core.algorithm.termination.validation.well_foundedness.TransitionInvariantUtils;
import org.sosy_lab.cpachecker.core.specification.Specification;
import org.sosy_lab.cpachecker.util.LoopStructure.Loop;
import org.sosy_lab.cpachecker.util.ast.IterationElement;
import org.sosy_lab.cpachecker.util.yamlwitnessexport.model.AbstractInvariantEntry;
import org.sosy_lab.cpachecker.util.yamlwitnessexport.model.InvariantEntry;
import org.sosy_lab.cpachecker.util.yamlwitnessexport.model.InvariantEntry.InvariantRecordType;
import org.sosy_lab.cpachecker.util.yamlwitnessexport.model.InvariantSetEntry;
import org.sosy_lab.cpachecker.util.yamlwitnessexport.model.LocationRecord;

public class TerminationYAMLWitnessExporter extends AbstractYAMLWitnessExporter {
  public TerminationYAMLWitnessExporter(
      Configuration pConfig, CFA pCfa, Specification pSpecification, LogManager pLogger)
      throws InvalidConfigurationException {
    super(pConfig, pCfa, pSpecification, pLogger);
  }

  private String rightSideOfRankingFunction(String pRankingFunction) {
    int firstEquals = pRankingFunction.indexOf('=');
    return pRankingFunction.substring(firstEquals + 1).trim();
  }

  // The function replaces variable names with annotation \prev, i.e. x -> \prev(x)
  private String addPrevKeyWordInFrontOfTheVariables(RankingFunction pRankingFunction) {
    String rankingFunctionWithPrev = pRankingFunction.toString();
    for (IProgramVar var : pRankingFunction.getVariables()) {
      String newVarName = "\\prev(" + var + ")";
      rankingFunctionWithPrev = rankingFunctionWithPrev.replace(var.toString(), newVarName);
    }
    return rankingFunctionWithPrev;
  }

  private InvariantEntry processSupportingInvariant(
      SupportingInvariant pSupportingInvariant, CFANode pLoopHead) {
    Optional<IterationElement> iterationStructure =
        getASTStructure().getTightestIterationStructureForNode(pLoopHead);
    if (iterationStructure.isEmpty()) {
      return null;
    }
    FileLocation fileLocation = iterationStructure.orElseThrow().getCompleteElement().location();

    LocationRecord locationRecord =
        LocationRecord.createLocationRecordAtStart(
            fileLocation,
            pLoopHead.getFunction().getFileLocation().getFileName().toString(),
            pLoopHead.getFunctionName());
    return new InvariantEntry(
        TransitionInvariantUtils.removeFunctionFromVarsName(pSupportingInvariant.toString()),
        InvariantRecordType.LOOP_INVARIANT.getKeyword(),
        YAMLWitnessExpressionType.C,
        locationRecord);
  }

  private InvariantEntry processRankingFunction(
      RankingFunction pRankingFunction, CFANode pLoopHead) {
    Optional<IterationElement> iterationStructure =
        getASTStructure().getTightestIterationStructureForNode(pLoopHead);
    if (iterationStructure.isEmpty()) {
      return null;
    }
    FileLocation fileLocation = iterationStructure.orElseThrow().getCompleteElement().location();

    LocationRecord locationRecord =
        LocationRecord.createLocationRecordAtStart(
            fileLocation,
            pLoopHead.getFunction().getFileLocation().getFileName().toString(),
            pLoopHead.getFunctionName());
    String prevRank =
        rightSideOfRankingFunction(addPrevKeyWordInFrontOfTheVariables(pRankingFunction));
    String currentRank = rightSideOfRankingFunction(pRankingFunction.toString());
    return new InvariantEntry(
        TransitionInvariantUtils.removeFunctionFromVarsName(prevRank + " > " + currentRank),
        InvariantRecordType.TRANSITION_LOOP_INVARIANT.getKeyword(),
        YAMLWitnessExpressionType.C,
        locationRecord);
  }

  private void constructWitness(
      Multimap<Loop, TerminationArgument> pTerminationArguments, Path pPath) throws IOException {
    ImmutableList.Builder<AbstractInvariantEntry> entries = new ImmutableList.Builder<>();

    for (Loop loop : pTerminationArguments.keySet()) {
      CFANode loopHead = loop.getLoopNodes().first();
      for (TerminationArgument argument : pTerminationArguments.get(loop)) {
        // First construct reachability invariants that support the termination argument.
        for (SupportingInvariant supportingInvariant : argument.getSupportingInvariants()) {
          entries.add(processSupportingInvariant(supportingInvariant, loopHead));
        }

        // Construct transition invariants from ranking function
        entries.add(processRankingFunction(argument.getRankingFunction(), loopHead));
      }
    }
    exportEntries(
        new InvariantSetEntry(getMetadata(YAMLWitnessVersion.V2d1), entries.build()), pPath);
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
