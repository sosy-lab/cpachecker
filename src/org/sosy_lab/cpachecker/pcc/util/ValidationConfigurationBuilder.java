// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2007-2020 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.pcc.util;

import com.google.common.collect.Maps;
import java.io.IOException;
import java.io.InputStream;
import java.io.ObjectInputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Collection;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Scanner;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;
import org.sosy_lab.common.configuration.Configuration;
import org.sosy_lab.common.configuration.ConfigurationBuilder;
import org.sosy_lab.common.configuration.InvalidConfigurationException;
import org.sosy_lab.common.io.IO;
import org.sosy_lab.cpachecker.exceptions.ValidationConfigurationConstructionFailed;
import org.sosy_lab.cpachecker.pcc.strategy.AbstractStrategy;

public class ValidationConfigurationBuilder {

  private final Configuration veriConfig;

  public ValidationConfigurationBuilder(final Configuration pVerifConfig) {
    veriConfig = pVerifConfig;
  }

  public Configuration getValidationConfiguration()
      throws ValidationConfigurationConstructionFailed {
    ConfigurationBuilder configBuilder = Configuration.builder();
    configBuilder.copyFrom(veriConfig);

    Map<String, String> relPropEntries =
        extractRelevantPropertyEntriesAndClearAnalysisOptions(configBuilder);

    if (relPropEntries.containsKey("analysis.restartAfterUnknown")
        && relPropEntries.get("analysis.restartAfterUnknown").equals("true")) {
      throw new ValidationConfigurationConstructionFailed();
    }

    clearProofCreationOptions(configBuilder);

    adaptCPAConfig(configBuilder, relPropEntries);

    try {
      return configBuilder.build();
    } catch (InvalidConfigurationException e) {
      throw new ValidationConfigurationConstructionFailed(e);
    }
  }

  private Map<String, String> extractRelevantPropertyEntriesAndClearAnalysisOptions(
      final ConfigurationBuilder pConfigBuilder) {
    Map<String, String> relevantPropertyEntries = Maps.newHashMapWithExpectedSize(4);

    String line, prop, value;
    int eqSignPos;

    try (Scanner s = new Scanner(veriConfig.asPropertiesString())) {
      while (s.hasNextLine()) {
        line = s.nextLine();

        eqSignPos = line.indexOf("=");
        if (eqSignPos < 0) {
          continue;
        }

        prop = line.substring(0, eqSignPos).trim();

        if (prop.equals("specification")
            || prop.startsWith("analysis.")
            || prop.startsWith("statistics.")
            || prop.startsWith("limits.")) {
          pConfigBuilder.clearOption(prop);
          if (!prop.equals("analysis.restartAfterUnknown")) {
            continue;
          }
        }

        if (prop.contains("cpa")
            || prop.equals("pcc.strategy")
            || prop.equals("analysis.restartAfterUnknown")) {
          value = line.substring(eqSignPos + 1).trim();
          relevantPropertyEntries.put(prop, value);
        }
      }
    }
    return relevantPropertyEntries;
  }

  private void clearProofCreationOptions(final ConfigurationBuilder pConfigBuilder) {
    String[] options = {
      "pcc.proofgen.doPCC",
      "pcc.proofFile",
      "pcc.resultcheck.writeProof",
      "pcc.sliceProof",
      "pcc.resultcheck.checkerConfig",
      "pcc.collectValueAnalysisStateInfo",
      "pcc.partial.certificateType",
      "pcc.partitioning.useGraphSizeToComputePartitionNumber",
      "pcc.partitioning.partitioningStrategy",
      "pcc.partitioning.multilevel.refinementHeuristic",
      "pcc.partitioning.bestfirst.balancePrecision",
      "pcc.partitioning.bestfirst.chosenFunction",
      "pcc.partitioning.fm.balanceCriterion",
      "pcc.partitioning.fm.initialPartitioningStrategy",
      "pcc.partitioning.kwayfm.balancePrecision",
      "pcc.partitioning.kwayfm.globalHeuristic",
      "pcc.partitioning.kwayfm.optimizationCriterion",
      "pcc.partitioning.maxNumElemsPerPartition",
      "pcc.partitioning.multilevel.matchingGenerator",
      "pcc.partitioning.multilevel.globalHeuristic"
    };

    for (String option : options) {
      pConfigBuilder.clearOption(option);
    }
  }

  private void adaptCPAConfig(
      final ConfigurationBuilder pConfigBuilder, final Map<String, String> pRelPropEntries) {

    String topCPA = pRelPropEntries.get("cpa");
    String strategy = pRelPropEntries.get("pcc.strategy");

    if (strategy == null) {
      // no strategy explicitly configured, default strategy is used
      strategy = "arg.ARGProofCheckerStrategy";
      pConfigBuilder.setOption("pcc.strategy", "arg.ARGProofCheckerStrategy");
    }

    if (notIsARGStrategy(strategy)) {
      removeARGCPA(pConfigBuilder, pRelPropEntries);
      if (topCPA.equals("cpa.arg.ARGCPA")) {
        topCPA = pRelPropEntries.get("ARGCPA.cpa");
      }
    }

    if (notIsARGCPAStrategyNorContainsPropertyChecker(strategy, topCPA)) {
      addPropertyChecker(pConfigBuilder, topCPA);
    }

    if (containsCPA("cpa.callstack.CallstackCPA", pRelPropEntries.values())) {
      adaptCallstackConfig(pConfigBuilder);
    }

    if (containsCPA("cpa.predicate.PredicateCPA", pRelPropEntries.values())) {
      adaptPredicateConfig(pConfigBuilder);
    }
  }

  private boolean notIsARGStrategy(String pProofValStrategy) {
    return !pProofValStrategy.contains("ARG");
  }

  private void removeARGCPA(
      final ConfigurationBuilder pConfigBuilder, Map<String, String> pRelPropEntries) {
    for (Entry<String, String> relProp : pRelPropEntries.entrySet()) {
      if (relProp.getValue().equals("cpa.arg.ARGCPA")) {
        overwriteOrAddConfigOption(
            pConfigBuilder, relProp.getKey(), pRelPropEntries.get("ARGCPA.cpa"));
        pConfigBuilder.clearOption("ARGCPA.cpa");
        break;
      }
    }
  }

  private boolean notIsARGCPAStrategyNorContainsPropertyChecker(
      String pProofValStrategy, String pTopCPA) {
    return !(pProofValStrategy.contains("ARGProofCheckerStrategy")
        || pTopCPA.equals("cpa.PropertyChecker.PropertyCheckerCPA"));
  }

  private void addPropertyChecker(
      final ConfigurationBuilder pConfigBuilder, final String pTopCPAValue) {
    overwriteOrAddConfigOption(pConfigBuilder, "cpa", "cpa.PropertyChecker.PropertyCheckerCPA");
    pConfigBuilder.setOption("PropertyCheckerCPA.cpa", pTopCPAValue);
    overwriteOrAddConfigOption(
        pConfigBuilder, "cpa.propertychecker.className", "NoTargetStateChecker");
  }

  private boolean containsCPA(String cpaByName, Collection<String> pPropValueSubset) {
    for (String value : pPropValueSubset) {
      if (value.contains(cpaByName)) {
        return true;
      }
    }

    return false;
  }

  public static void adaptCallstackConfig(final ConfigurationBuilder pConfigBuilder) {
    overwriteOrAddConfigOption(pConfigBuilder, "cpa.callstack.domain", "FLATPCC");
  }

  public static void adaptPredicateConfig(final ConfigurationBuilder pConfigBuilder) {
    overwriteOrAddConfigOption(pConfigBuilder, "cpa.predicate.stop", "SEPPCC");
    overwriteOrAddConfigOption(pConfigBuilder, "cpa.predicate.targetStateSatCheck", "true");
    overwriteOrAddConfigOption(pConfigBuilder, "satCheckAtAbstraction", "true");
  }

  private static void overwriteOrAddConfigOption(
      final ConfigurationBuilder pConfigBuilder, final String pOptionName, final String pValue) {
    pConfigBuilder.clearOption(pOptionName);
    pConfigBuilder.setOption(pOptionName, pValue);
  }

  public static Configuration readConfigFromProof(Path proofFile)
      throws IOException, InvalidConfigurationException {

    try (InputStream fis = Files.newInputStream(proofFile);
        ZipInputStream zis = new ZipInputStream(fis); ) {
      ZipEntry entry;
      while ((entry = zis.getNextEntry()) != null) {
        if (entry.getName().equals(AbstractStrategy.CONFIG_ZIPENTRY_NAME)) {
          break;
        }
      }

      if (entry == null) {
        throw new IOException("Unable to find configuration entry in proof.");
      }

      Path valConfig = Files.createTempFile("pcc-check-config", "properties");

      try (ObjectInputStream in = new ObjectInputStream(zis)) {
        IO.writeFile(valConfig, StandardCharsets.UTF_8, in.readObject());
      } catch (ClassNotFoundException e) {
        throw new IOException("Failed to read configuration");
      }

      return Configuration.builder().loadFromFile(valConfig).build();
    }
  }
}
