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
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
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

    static final String MODE_LIMIT_DELIMITER = "_";

    enum REPETITIONMODE {
        CONTINUE("continue"),
        REUSEOWNPRECISION("reuse-own-precision"),
        REUSEPREDPRECISION("reuse-pred-precision"),
        REUSEOWNANDPREDPRECISION("reuse-precisions"),
        REUSECPA_OWNPRECISION("reuse-cpa-own-precision"),

        REUSECPA_PREDPRECISION("reuse-cpa-pred-precision"),
        REUSECPA_OWNANDPREDPRECISION("reuse-cpa-precisions"),
        MODUS_LIMIT("modus-limit"),
        NOREUSE("noreuse");

        private static final Map<String, REPETITIONMODE> stringToEnum = new HashMap<>();

        static {
            for (REPETITIONMODE mode : values()) {
                stringToEnum.put(mode.code, mode);
            }
        }
        private final String code;

        REPETITIONMODE(String code) {
            this.code = code;
        }

        public String getCode(){
            return this.code;
        }

        public static REPETITIONMODE fromString(String symbol) {
            return stringToEnum.getOrDefault(symbol, REPETITIONMODE.NOREUSE);
        }
    }

    private final Path configFile;
    private int timeLimit;
    private final REPETITIONMODE mode;
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
            if (str.contains(MODE_LIMIT_DELIMITER)) {
                try {
                    int limit = Integer.parseInt(str.substring(str.indexOf(MODE_LIMIT_DELIMITER) + 1));
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

    private REPETITIONMODE extractModeFromAnnotation(final Optional<String> annotation) {
        return annotation.map(enumVal -> {
            var val = "";
            if (enumVal.contains(MODE_LIMIT_DELIMITER)) {
                val = enumVal.substring(0, enumVal.indexOf(MODE_LIMIT_DELIMITER));
            }
            val =  val.toLowerCase(Locale.ROOT);
            return REPETITIONMODE.fromString(val);
        }).orElseThrow();
    }

    public boolean reuseCPA() {
        return mode == REPETITIONMODE.CONTINUE
                || mode == REPETITIONMODE.REUSECPA_OWNPRECISION
                || mode == REPETITIONMODE.REUSECPA_PREDPRECISION
                || mode == REPETITIONMODE.REUSECPA_OWNANDPREDPRECISION
                || mode == REPETITIONMODE.MODUS_LIMIT;
    }

    public boolean reusePrecision() {
        return reuseOwnPrecision() || reusePredecessorPrecision();
    }

    public boolean reuseOwnPrecision() {
        return mode == REPETITIONMODE.REUSEOWNPRECISION
                || mode == REPETITIONMODE.REUSEOWNANDPREDPRECISION
                || mode == REPETITIONMODE.REUSECPA_OWNPRECISION
                || mode == REPETITIONMODE.REUSECPA_OWNANDPREDPRECISION;
    }

    public boolean reusePredecessorPrecision() {
        return mode == REPETITIONMODE.REUSEPREDPRECISION
                || mode == REPETITIONMODE.REUSEOWNANDPREDPRECISION
                || mode == REPETITIONMODE.REUSECPA_PREDPRECISION
                || mode == REPETITIONMODE.REUSECPA_OWNANDPREDPRECISION;
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
