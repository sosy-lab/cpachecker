// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2020 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.core.algorithm.composition;

import java.io.IOException;
import java.nio.channels.ClosedByInterruptException;
import java.nio.file.Path;
import java.util.Locale;
import java.util.Optional;
import java.util.logging.Level;
import org.checkerframework.checker.nullness.qual.Nullable;
import org.sosy_lab.common.ShutdownNotifier;
import org.sosy_lab.common.configuration.AnnotatedValue;
import org.sosy_lab.common.configuration.Configuration;
import org.sosy_lab.common.configuration.ConfigurationBuilder;
import org.sosy_lab.common.configuration.InvalidConfigurationException;
import org.sosy_lab.common.log.LogManager;
import org.sosy_lab.common.time.TimeSpan;
import org.sosy_lab.common.time.Timer;
import org.sosy_lab.cpachecker.core.interfaces.ConfigurableProgramAnalysis;
import org.sosy_lab.cpachecker.core.reachedset.ReachedSet;

public class AlgorithmContext {

  public static final int DEFAULT_TIME_LIMIT = 10;

  private enum RepetitionMode {
    CONTINUE,
    NOREUSE,
    REUSEOWNPRECISION,
    REUSEPREDPRECISION,
    REUSEOWNANDPREDPRECISION,
    REUSECPA_OWNPRECISION,
    REUSECPA_PREDPRECISION,
    REUSECPA_OWNANDPREDPRECISION,
  }

  private final Path configFile;
  private int timeLimit;
  private final RepetitionMode mode;
  private final Timer timer;
  private final boolean ifRecursive;

  private @Nullable ConfigurableProgramAnalysis cpa;
  private @Nullable Configuration config;
  private ReachedSet reached;
  private double progress = -1.0;

  public AlgorithmContext(final AnnotatedValue<Path> pConfigFile) {
    configFile = pConfigFile.value();
    timer = new Timer();
    timeLimit = extractLimitFromAnnotation(pConfigFile.annotation());
    mode = extractModeFromAnnotation(pConfigFile.annotation());
    ifRecursive = extractApplicationContext(pConfigFile.annotation());
  }

  private boolean extractApplicationContext(final Optional<String> annotation) {
    if (annotation.isPresent()) {
      String str = annotation.orElseThrow();
      return str.endsWith("_if-recursive");
    }
    return false;
  }

  private int extractLimitFromAnnotation(final Optional<String> annotation) {
    if (annotation.isPresent()) {
      String str = annotation.orElseThrow();
      if (str.contains("_")) {
        try {
          int limit = Integer.parseInt(str.substring(str.indexOf("_") + 1));
          if (limit > 0) {
            return limit;
          }
        } catch (NumberFormatException e) {
          // ignored, invalid annotation
        }
      }
    }
    return DEFAULT_TIME_LIMIT;
  }

  private RepetitionMode extractModeFromAnnotation(final Optional<String> annotation) {
    String val = "";
    if (annotation.isPresent()) {
      val = annotation.orElseThrow();
      if (val.contains("_")) {
        val = val.substring(0, val.indexOf("_"));
      }
      val = val.toLowerCase(Locale.ROOT);
    }

    return switch (val) {
      case "continue" -> RepetitionMode.CONTINUE;
      case "reuse-own-precision" -> RepetitionMode.REUSEOWNPRECISION;
      case "reuse-pred-precision" -> RepetitionMode.REUSEPREDPRECISION;
      case "reuse-precisions" -> RepetitionMode.REUSEOWNANDPREDPRECISION;
      case "reuse-cpa-own-precision" -> RepetitionMode.REUSECPA_OWNPRECISION;
      case "reuse-cpa-pred-precision" -> RepetitionMode.REUSECPA_PREDPRECISION;
      case "reuse-cpa-precisions" -> RepetitionMode.REUSECPA_OWNANDPREDPRECISION;
      default -> RepetitionMode.NOREUSE;
    };
  }

  public boolean reuseCPA() {
    return mode == RepetitionMode.CONTINUE
        || mode == RepetitionMode.REUSECPA_OWNPRECISION
        || mode == RepetitionMode.REUSECPA_PREDPRECISION
        || mode == RepetitionMode.REUSECPA_OWNANDPREDPRECISION;
  }

  public boolean reusePrecision() {
    return reuseOwnPrecision() || reusePredecessorPrecision();
  }

  public boolean reuseOwnPrecision() {
    return mode == RepetitionMode.REUSEOWNPRECISION
        || mode == RepetitionMode.REUSEOWNANDPREDPRECISION
        || mode == RepetitionMode.REUSECPA_OWNPRECISION
        || mode == RepetitionMode.REUSECPA_OWNANDPREDPRECISION;
  }

  public boolean reusePredecessorPrecision() {
    return mode == RepetitionMode.REUSEPREDPRECISION
        || mode == RepetitionMode.REUSEOWNANDPREDPRECISION
        || mode == RepetitionMode.REUSECPA_PREDPRECISION
        || mode == RepetitionMode.REUSECPA_OWNANDPREDPRECISION;
  }

  public void resetProgress() {
    progress = -1.0;
  }

  public void adaptTimeLimit(final int newTimeLimit) {
    timeLimit = Math.max(DEFAULT_TIME_LIMIT, newTimeLimit);
  }

  public int getTimeLimit() {
    return timeLimit;
  }

  public void setProgress(final double pProgress) {
    progress = pProgress;
  }

  public double getProgress() {
    return progress;
  }

  public boolean isRecursiveOnlyConfiguration() {
    return ifRecursive;
  }

  public @Nullable Configuration getConfig() {
    return config;
  }

  public @Nullable Configuration getAndCreateConfigIfNecessary(
      final Configuration pGlobalConfig,
      final LogManager pLogger,
      final ShutdownNotifier pShutdownNotifier) {
    if (config != null) {
      return config;
    }

    ConfigurationBuilder singleConfigBuilder = Configuration.builder();
    singleConfigBuilder.copyFrom(pGlobalConfig);
    singleConfigBuilder.clearOption("compositionAlgorithm.configFiles");
    singleConfigBuilder.clearOption("analysis.useCompositionAnalysis");

    try { // read config file
      singleConfigBuilder.loadFromFile(configFile);
      pLogger.logf(Level.INFO, "Loading analysis %s ...", configFile);

      config = singleConfigBuilder.build();

    } catch (InvalidConfigurationException e) {
      pLogger.logUserException(
          Level.WARNING, e, "Configuration file " + configFile + " is invalid");

    } catch (IOException e) {
      String message = "Failed to read " + configFile + ".";
      if (pShutdownNotifier.shouldShutdown() && e instanceof ClosedByInterruptException) {
        pLogger.log(Level.WARNING, message);
      } else {
        pLogger.logUserException(Level.WARNING, e, message);
      }
    }

    return config;
  }

  public ReachedSet getReachedSet() {
    return reached;
  }

  public void setReachedSet(final ReachedSet pReached) {
    reached = pReached;
  }

  public @Nullable ConfigurableProgramAnalysis getCPA() {
    return cpa;
  }

  public void setCPA(final @Nullable ConfigurableProgramAnalysis pCpa) {
    cpa = pCpa;
  }

  public String configToString() {
    return configFile.toString();
  }

  public void startTimer() {
    timer.start();
  }

  public void stopTimer() {
    timer.stop();
  }

  public TimeSpan getTotalTimeSpent() {
    return timer.getSumTime();
  }
}
