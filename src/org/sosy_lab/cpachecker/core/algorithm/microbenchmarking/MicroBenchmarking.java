package org.sosy_lab.cpachecker.core.algorithm.microbenchmarking;

import java.io.IOException;
import java.io.Writer;
import java.nio.charset.Charset;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.logging.Level;
import org.sosy_lab.common.ShutdownNotifier;
import org.sosy_lab.common.configuration.Configuration;
import org.sosy_lab.common.configuration.FileOption;
import org.sosy_lab.common.configuration.InvalidConfigurationException;
import org.sosy_lab.common.configuration.Option;
import org.sosy_lab.common.configuration.Options;
import org.sosy_lab.common.io.IO;
import org.sosy_lab.common.log.LogManager;
import org.sosy_lab.common.time.Tickers;
import org.sosy_lab.common.time.Tickers.TickerWithUnit;
import org.sosy_lab.cpachecker.core.algorithm.Algorithm;
import org.sosy_lab.cpachecker.core.reachedset.ReachedSet;
import org.sosy_lab.cpachecker.core.specification.Specification;
import org.sosy_lab.cpachecker.exceptions.CPAException;

@Options(prefix = "microBenchmark")
public class MicroBenchmarking implements Algorithm {

  private static class BenchmarkExecutionRun {
    long duration;
    double determinant;
  }

  private static class MatrixMultiplicationCell {
    private int[] values;

    public MatrixMultiplicationCell(int size) {
      values = new int[size];
    }

    void consume(int value, int index) {
      values[index] = value;
    }

    int sum() {
      return Arrays.stream(values).reduce(0, (a, b) -> a + b);
    }

  }

  private static class ConfigProgramExecutions {
    private String configFileName;
    private String programFileName;

    private List<BenchmarkExecutionRun> runTimes;

    private double averageRunTime;
    private double variance;
    private double standardDeviation;
  }

  private final Algorithm child;
  private final LogManager logger;

  @Option(
    secure = true,
    description = "Number of iterations for each algorithm/program combination. Defaults to 10.")
  private int numExecutions = 22;

  @Option(secure = true, description = "Size of rows and columns for micro benchmarking matrix")
  private int sizeMatrixRowCol = 50;

  @Option(secure = true, description = "Amount of time microbenchmark run for on base line system.")
  private long baseLineRunTime = -1;

  @Option(
    secure = true,
    description = "Percentage which represents the threshold, compared to the base line run time, where the user gets notified..")
  private double warningThreshold = 10;

  @Option(
    secure = true,
    required = true,
    description = "List of algorithm config files to use for the benchmarking process.")
  @FileOption(FileOption.Type.REQUIRED_INPUT_FILE)
  private List<Path> propertyFiles;

  @Option(
    secure = true,
    required = true,
    description = "List of programs to run each benchmarking algorithm on.")
  @FileOption(FileOption.Type.REQUIRED_INPUT_FILE)
  private List<Path> programFiles;

  @Option(
    secure = true,
    description = "Defines the file where the results of the micro benchmark are stored.")
  @FileOption(FileOption.Type.OUTPUT_FILE)
  private Path outputFile;

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
  }

  @Override
  public AlgorithmStatus run(ReachedSet pReachedSet) throws CPAException, InterruptedException {

    logger.logf(Level.INFO, "Starting micro benchmarking");
    TickerWithUnit ticker = Tickers.getCurrentThreadCputime();

    Map<String, List<ConfigProgramExecutions>> benchmarkTimes = new HashMap<>();
    List<BenchmarkExecutionRun> runTimes = new ArrayList<>();

    int[][] C = new int[0][0];

    for (int exec = 0; exec < numExecutions; exec++) {
      int[][] firstMatrix = generateRandomMatrix();
      int[][] secondMatrix = generateRandomMatrix();
      int m = firstMatrix.length;
      int n = firstMatrix[0].length;
      C = new int[m][n];
      long startTime = ticker.read();

      for (int i = 0; i < m; i++) {
        for (int j = 0; j < n; j++) {
          MatrixMultiplicationCell cellData = new MatrixMultiplicationCell(secondMatrix.length);
          for (int k = 0; k < secondMatrix.length; k++) {
            int value = firstMatrix[i][k] * secondMatrix[k][j];
            cellData.consume(value, k);
          }
          C[i][j] = cellData.sum();
        }
      }

      long endTime = ticker.read();
      long timeDiff = endTime - startTime;

      BenchmarkExecutionRun run = new BenchmarkExecutionRun();
      run.duration = timeDiff;
      run.determinant = determinant(C);
      runTimes.add(run);
    }

    ConfigProgramExecutions obj = new ConfigProgramExecutions();
    obj.configFileName = "";
    obj.programFileName = "matrix-multiplication";
    obj.runTimes = runTimes;
    double averageRunTime = calculateAverage(obj);
    obj.averageRunTime = averageRunTime;
    double variance = calculateVariance(obj, averageRunTime);
    obj.variance = variance;
    double standardDeviation = Math.sqrt(variance);
    obj.standardDeviation = standardDeviation;
    benchmarkTimes.put("matrix-multiplicatioN", List.of(obj));

    double totalRunTime = runTimes.stream().map(a -> a.duration).reduce(0L, (a, b) -> a + b);
    if (baseLineRunTime > 0 && warningThreshold > 0) {
      double upperBound = baseLineRunTime + baseLineRunTime * (warningThreshold / 10.0);
      double lowerBound = baseLineRunTime - baseLineRunTime * (warningThreshold / 10.0);

      if (totalRunTime >= upperBound) {
        logger.log(
            Level.WARNING,
            "The system you're running on completed the microbenchmark more than 10% slower than the base line system!");
      }

      if (totalRunTime <= lowerBound) {
        logger.log(
            Level.WARNING,
            "The system you're running on completed the microbenchmark more than 10% faster than the base line system!");
      }
    }

    if (this.outputFile != null) {
      try (Writer writer = IO.openOutputFile(this.outputFile, Charset.defaultCharset())) {
        benchmarkTimes.entrySet().forEach(entry -> entry.getValue().forEach(l -> {
          try {
            writer.append("Config file: ");
            writer.append(l.configFileName + "\n");
            writer.append("Program file: ");
            writer.append(l.programFileName + "\n");

            for (BenchmarkExecutionRun run : l.runTimes) {
              writer.append(String.valueOf(run.duration));
              writer.append(';');
            }
            writer.append("\n");
            writer.append("Calculated matrix determinants:");
            for (BenchmarkExecutionRun run : l.runTimes) {
              writer.append(String.valueOf(run.determinant));
              writer.append(';');
            }
            writer.append("\n");

            writer.append("Average run time: ");
            writer.append(String.valueOf(l.averageRunTime) + "\n");

            writer.append("Variance: ");
            writer.append(String.valueOf(l.variance) + "\n");

            writer.append("Standard deviation: ");
            writer.append(String.valueOf(l.standardDeviation) + "\n");

            writer.append("\n\n");
          } catch (IOException e) {
            logger.logfUserException(Level.WARNING, e, "Failed to write run time data to file.");
          }
        }));
      } catch (IOException ex) {
        logger.logUserException(Level.WARNING, ex, "Could not write CFA to dot file");
      }
    }

    return child.run(pReachedSet);
  }

  private int[][] generateRandomMatrix() {
    int[][] matrix = new int[sizeMatrixRowCol][sizeMatrixRowCol];
    Random random = new Random();

    for (int i = 0; i < sizeMatrixRowCol; i++) {
      for (int j = 0; j < sizeMatrixRowCol; j++) {
        matrix[i][j] = random.nextInt();
      }
    }

    return matrix;
  }

  private double calculateAverage(ConfigProgramExecutions cpe) {

    long sum = 0;
    for (BenchmarkExecutionRun run : cpe.runTimes) {
      sum += (run.duration / 1000);
    }

    return sum / (double) cpe.runTimes.size();
  }

  private double calculateVariance(ConfigProgramExecutions cpe, double average) {

    double sum = 0.0;
    for (BenchmarkExecutionRun run : cpe.runTimes) {
      sum += Math.pow((run.duration / 1000) - average, 2);
    }

    return sum / (cpe.runTimes.size());
  }

  private double determinant(int matrix[][]) {
    int n = matrix.length;
    double det = 0;

    if (n == 1) {
        det = matrix[0][0];
    } else if (n == 2) {
        det = matrix[0][0] * matrix[1][1] - matrix[0][1] * matrix[1][0];
    } else {
      for (int z = 0; z < n; z++) {
        int[][] m = new int[n - 1][];
            for (int k = 0; k < n-1; k++) {
          m[k] = new int[n - 1];
            }

            for (int i = 1; i < n; i++) {
              int counter = 0;
                for (int j = 0; j < n; j++) {
                  if (j == z) {
                      continue;
                    }
                    m[i - 1][counter] = matrix[i][j];
                    counter++;
                }
            }
            det += (Math.pow(-1.0, 2.0 + z) * matrix[0][z] * determinant(m));
        }
    }
    return det;
  }
}
