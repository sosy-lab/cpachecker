package org.sosy_lab.cpachecker.core.algorithm.instrumentation;

import java.io.BufferedWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.Map;
import org.sosy_lab.cpachecker.core.algorithm.instrumentation.InstrumentationAutomaton.InstrumentationProperty;

import org.sosy_lab.cpachecker.cfa.model.CFANode;

public class RankingTransformationLogger {

    private final Path logFile;

    public RankingTransformationLogger() {
        Path outputDir = Path.of("output");
        this.logFile = outputDir.resolve("rankingInfo.txt");

        try {
            Files.createDirectories(outputDir);
            Files.writeString(
                    logFile,
                    "Transformation Log\n====================\n",
                    StandardOpenOption.CREATE,
                    StandardOpenOption.TRUNCATE_EXISTING);
        } catch (IOException e) {
            System.err.println("Failed to initialize log file: " + e.getMessage());
        }
    }

    public static String convertToPercent(int numerator, int denominator) {
        if (denominator == 0) {
            return "";
        }
        double percent = (double) numerator / denominator * 100;
        return String.format("%.2f%%", percent);
    }

    public void logTransformationResults(
            String transformedFile,
            InstrumentationProperty property,
            InstrumentationProperty defaultProperty,
            int successfulTransformations,
            int allTransformations,
            Map<CFANode, TransformedNodeInfo> distanceTransformedHeads,
            Map<CFANode, TransformedNodeInfo> defaultTransformedHeads) {

        try (BufferedWriter writer = Files.newBufferedWriter(logFile, StandardOpenOption.APPEND)) {
            writer.write("Original File: " + transformedFile + "\n");
            writer.write("Used Algorithm: " + property + "\n");
            writer.write("Used Default-Algorithm: " + defaultProperty + "\n");
            writer.write(
                    "Successful Transformations: "
                            + "("
                            + successfulTransformations
                            + "/"
                            + allTransformations
                            + ") "
                            + convertToPercent(successfulTransformations, allTransformations)
                            + "\n");

            writer.write("\nDistance-Transformed LoopHeads:\n");
            for (Map.Entry<CFANode, TransformedNodeInfo> entry : distanceTransformedHeads
                    .entrySet()) {
                writer.write(
                        "  - Loophead "
                                + entry.getValue().getIndex()
                                + ": "
                                + entry.getValue().getLoopLocation()
                                + " ||| "
                                + getLoopCondition(entry.getKey())
                                + "\n");
            }

            writer.write("\nDefault-Transformed LoopHeads:\n");
            for (Map.Entry<CFANode, TransformedNodeInfo> entry : defaultTransformedHeads
                    .entrySet()) {
                writer.write(
                        "  - Loophead "
                                + entry.getValue().getIndex()
                                + ": "
                                + entry.getValue().getLoopLocation()
                                + " ||| "
                                + getLoopCondition(entry.getKey())
                                + "\n");
            }

            writer.write("\n====================\n");
        } catch (IOException e) {
            System.err.println("Failed to write to log file: " + e.getMessage());
        }
    }

    private String getLoopCondition(CFANode loopHead) {
        if (loopHead.getNumLeavingEdges() > 1) {
            return loopHead.getLeavingEdge(0).getRawStatement()
                    + " || "
                    + loopHead.getLeavingEdge(1).getRawStatement();
        }
        return "UNKNOWN_CONDITION";
    }

    public void deleteLogFile() {
        try {
            if (Files.exists(logFile)) {
                Files.delete(logFile);
            }
        } catch (IOException e) {
            System.err.println("Failed to delete log file: " + e.getMessage());
        }
    }
}
