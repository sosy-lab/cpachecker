// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2007-2025 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.core.algorithm.trivialrules;

import com.google.common.collect.ImmutableMultimap;
import java.io.IOException;
import java.io.PrintStream;
import java.nio.file.Path;
import java.util.logging.Level;
import org.checkerframework.checker.nullness.qual.Nullable;
import org.sosy_lab.common.configuration.Configuration;
import org.sosy_lab.common.configuration.FileOption;
import org.sosy_lab.common.configuration.InvalidConfigurationException;
import org.sosy_lab.common.configuration.Option;
import org.sosy_lab.common.configuration.Options;
import org.sosy_lab.common.log.LogManager;
import org.sosy_lab.cpachecker.cfa.CFA;
import org.sosy_lab.cpachecker.core.CPAcheckerResult.Result;
import org.sosy_lab.cpachecker.core.interfaces.Statistics;
import org.sosy_lab.cpachecker.core.reachedset.UnmodifiableReachedSet;
import org.sosy_lab.cpachecker.core.specification.Property.CommonVerificationProperty;
import org.sosy_lab.cpachecker.core.specification.Specification;
import org.sosy_lab.cpachecker.util.yamlwitnessexport.ExecutionToYAMLWitness;

/**
 * Writes the witness for the answer of a trivial rule. A rule has no ARG to build a witness from,
 * so the witnesses are the two simplest ones that a verdict can justify (cf. {@link
 * ExecutionToYAMLWitness}):
 *
 * <ul>
 *   <li>For TRUE an invariant set without invariants: the argument of a trivial rule is about the
 *       program as a whole and does not produce an invariant for any location.
 *   <li>For FALSE a violation witness that consists of the target waypoint, which is the location
 *       whose execution violates the specification.
 * </ul>
 */
@Options(prefix = "trivialrules")
class TrivialRulesWitnessExporter implements Statistics {

  @Option(
      secure = true,
      name = "witness",
      description =
          "The file to write the witness to. On the result TRUE an invariant set without"
              + " invariants is written, on the result FALSE a violation witness that points to"
              + " the violated location. Witnesses are written in version 2.0. Leave empty to not"
              + " export any witness.")
  @FileOption(FileOption.Type.OUTPUT_FILE)
  private @Nullable Path witnessFile = Path.of("witness.yml");

  private final ExecutionToYAMLWitness exporter;
  private final LogManager logger;
  private final TrivialRulesStatistics stats;

  TrivialRulesWitnessExporter(
      Configuration pConfig,
      CFA pCfa,
      Specification pSpecification,
      LogManager pLogger,
      TrivialRulesStatistics pStats)
      throws InvalidConfigurationException {
    pConfig.inject(this);
    logger = pLogger;
    stats = pStats;
    exporter = new ExecutionToYAMLWitness(pConfig, pCfa, pSpecification, pLogger);
  }

  @Override
  public void writeOutputFiles(Result pResult, UnmodifiableReachedSet pReached) {
    if (witnessFile == null || stats.verdict() == null) {
      return;
    }
    RuleVerdict verdict = stats.verdict();
    try {
      switch (pResult) {
        case TRUE -> {
          logger.log(
              Level.INFO,
              "The correctness witness contains no invariant: the argument of a trivial rule is"
                  + " about the program as a whole, not about a location of it.");
          exporter.exportCorrectnessWitness(
              ImmutableMultimap.of(), ImmutableMultimap.of(), witnessFile);
        }
        case FALSE -> {
          if (verdict.violatingEdge() == null) {
            logger.log(
                Level.WARNING,
                "Cannot export a violation witness because the violated location is unknown.");
          } else if (refutesTermination()) {
            // A violation witness for termination has to describe an execution that never ends,
            // which the location of the loop alone does not.
            logger.log(
                Level.INFO,
                "Not exporting a violation witness for the termination property, because such a"
                    + " witness has to describe an execution that never ends.");
          } else {
            exporter.exportViolationWitness(verdict.violatingEdge(), witnessFile);
          }
        }
        default -> {}
      }
    } catch (IOException e) {
      logger.logUserException(Level.WARNING, e, "Could not write the witness to " + witnessFile);
    } catch (InterruptedException e) {
      logger.log(Level.WARNING, "Witness export was interrupted.");
    }
  }

  private boolean refutesTermination() {
    TrivialRule rule = stats.decidingRule();
    return rule != null && rule.decides().contains(CommonVerificationProperty.TERMINATION);
  }

  @Override
  public void printStatistics(PrintStream pOut, Result pResult, UnmodifiableReachedSet pReached) {
    // This class only writes output files.
  }

  @Override
  public @Nullable String getName() {
    return null;
  }
}
