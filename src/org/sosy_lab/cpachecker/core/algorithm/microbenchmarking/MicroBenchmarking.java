package org.sosy_lab.cpachecker.core.algorithm.microbenchmarking;

import java.io.IOException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;
import org.sosy_lab.common.ShutdownNotifier;
import org.sosy_lab.common.configuration.Configuration;
import org.sosy_lab.common.configuration.FileOption;
import org.sosy_lab.common.configuration.InvalidConfigurationException;
import org.sosy_lab.common.configuration.Option;
import org.sosy_lab.common.configuration.Options;
import org.sosy_lab.common.log.LogManager;
import org.sosy_lab.cpachecker.core.algorithm.Algorithm;
import org.sosy_lab.cpachecker.core.reachedset.ReachedSet;
import org.sosy_lab.cpachecker.core.specification.Specification;
import org.sosy_lab.cpachecker.exceptions.CPAException;
import org.sosy_lab.cpachecker.exceptions.ParserException;

@Options(prefix = "microBenchmark")
public class MicroBenchmarking implements Algorithm {

  protected static class BenchmarkExecutionRun {
    long duration;
    double matrixRowSum;
  }

  private final Algorithm child;
  private final LogManager logger;
  private final ShutdownNotifier pShutdownNotifier;
  private final Specification pSpecification;

  @Option(
    secure = true,
    description = "Whether a user defined analysis should be run as the microbenchmark or the naive matrix multiplication")
  private boolean useUserDefinedAnalysis = false;

  @Option(
    secure = true,
    description = "The baseline value of a previously executed microbenchmark")
  private long baseline = -1;

  @Option(
    secure = true,
    description = "The deviation percentage after which the user should be informed. 0.0 = 0%, 1.0 = 100%")
  private double threshold = -1;

  @Option(
    secure = true,
    description = "Number of iterations for each algorithm/program combination. Defaults to 20.")
  private int numExecutions = 20;

  @Option(
    secure = true,
    description = "Number of iterations where results are discarded for warming up the JVM.")
  private int numWarmupExecutions = 20;

  @Option(secure = true, description = "Size of rows and columns for micro benchmarking matrix")
  private int sizeMatrixRowCol = 50;

  @Option(
    secure = true,
    required = true,
    description = "List of algorithm config files to use for the benchmarking process.")
  @FileOption(FileOption.Type.OPTIONAL_INPUT_FILE)
  private List<Path> propertyFiles;

  @Option(
    secure = true,
    required = true,
    description = "List of programs to run each benchmarking algorithm on.")
  @FileOption(FileOption.Type.OPTIONAL_INPUT_FILE)
  private List<Path> programFiles;


  public MicroBenchmarking(
      Configuration pConfig,
      LogManager pLogger,
      ShutdownNotifier pShutdownNotifier,
      Specification pSpecification,
      Algorithm child)
      throws InvalidConfigurationException {
    pConfig.inject(this);

    this.child = child;
    this.logger = pLogger;
    this.pShutdownNotifier = pShutdownNotifier;
    this.pSpecification = pSpecification;
  }

  @Override
  public AlgorithmStatus run(ReachedSet pReachedSet) throws CPAException, InterruptedException {

    try {
      logger.logf(Level.INFO, "Starting micro benchmarking");
      List<BenchmarkExecutionRun> runTimes;
      if (useUserDefinedAnalysis) {
        if (propertyFiles == null || programFiles == null || propertyFiles.isEmpty() || programFiles.isEmpty()) {
          logger.log(Level.WARNING, "Please supply required property and program files for running an analysis as microbenchmark");
          runTimes = new ArrayList<>();
        } else {
          MBAnalysisPayload mbAnalysis = new MBAnalysisPayload(logger, numWarmupExecutions, numExecutions, propertyFiles, programFiles, pShutdownNotifier, pSpecification);
          runTimes = mbAnalysis.runMicrobenchmark();
        }
      } else {
        MBMatrixMultiplication mbMatrix =
            new MBMatrixMultiplication(sizeMatrixRowCol, numWarmupExecutions, numExecutions);
          runTimes = mbMatrix.runMicrobenchmark();
      }

      calculateRuntimeDeviation(runTimes);

    } catch (IOException | InvalidConfigurationException | ParserException | InterruptedException
        | CPAException e) {
      logger.logfException(Level.SEVERE, e, "Failed to run micro benchmark!");
    }

    return child.run(pReachedSet);
  }

  private void calculateRuntimeDeviation(List<BenchmarkExecutionRun> runTimes) {

    long runTime = runTimes.stream().map(r -> r.duration).reduce(0L, (a, b) -> a + b);
    double runTimeMicroSeconds = runTime / 1000;

    logger.log(
        Level.INFO,
        String.format("Microbenchmark took %s ms to complete", runTimeMicroSeconds));

    if (runTimes.isEmpty() || baseline <= 0 || threshold <= 0.0) {
      return;
    }

      double upperBound = baseline + baseline * threshold;
      double lowerBound = baseline - baseline * threshold;

      if (runTimeMicroSeconds >= upperBound) {
        logger.log(
            Level.WARNING,
            "The system you're running on completed the microbenchmark slower than your predefined threshold!");
      }

      if (runTimeMicroSeconds <= lowerBound) {
        logger.log(
            Level.WARNING,
            "The system you're running on completed the microbenchmark faster than your predefined threshold!");
      }

  }
}
