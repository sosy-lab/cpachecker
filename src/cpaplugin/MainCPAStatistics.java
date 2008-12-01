package cpaplugin;

import java.io.PrintWriter;
import java.util.Arrays;
import java.util.Collection;
import java.util.LinkedList;

public class MainCPAStatistics implements CPAStatistics {
    private Collection<CPAStatistics> subStats;
    private long programStartingTime;
    private long analysisStartingTime;
    private long analysisEndingTime;

    public final static int ERROR_UNKNOWN = -1;
    public final static int ERROR_REACHED = 0;
    public final static int ERROR_NOT_REACHED = 1;
    private int errorReached;


    public MainCPAStatistics() {
        subStats = new LinkedList<CPAStatistics>();
        programStartingTime = 0;
        analysisStartingTime = 0;
        analysisEndingTime = 0;
        errorReached = ERROR_UNKNOWN;
    }

    public int getErrorReached() { return errorReached; }
    public void setErrorReached(boolean yes) {
        errorReached = yes ? ERROR_REACHED : ERROR_NOT_REACHED;
    }

    public void startProgramTimer() {
        programStartingTime = System.currentTimeMillis();
    }

    public void startAnalysisTimer() {
        analysisStartingTime = System.currentTimeMillis();
    }

    public void stopAnalysisTimer() {
        analysisEndingTime = System.currentTimeMillis();
    }

    public void addSubStatistics(CPAStatistics s) {
        subStats.add(s);
    }

    @Override
    public String getName() {
        return "CPAChecker";
    }

    @Override
    public void printStatistics(PrintWriter out) {
        long totalTimeInMillis = analysisEndingTime - analysisStartingTime;
        long totalAbsoluteTimeMillis = analysisEndingTime - programStartingTime;

        out.println("\nCPAChecker general statistics:");
        out.println("------------------------------");
        out.println("Total Time Elapsed: " + toTime(totalTimeInMillis));
        out.println("Total Time Elapsed including CFA construction: " +
                toTime(totalAbsoluteTimeMillis));

        if (!subStats.isEmpty()) {
            out.println("\nAnalysis-specific statistics:");
            out.println("-----------------------------\n");
            for (CPAStatistics s : subStats) {
                String name = s.getName();
                out.println(name);
                char[] c = new char[name.length()];
                Arrays.fill(c, '-');
                out.println(String.copyValueOf(c));
                s.printStatistics(out);
                out.println("");
            }
        } else {
            out.println("");
        }
        out.flush();
    }

    private String toTime(long timeMillis) {
        return String.format("% 5d.%03ds", timeMillis/1000, timeMillis%1000);
    }
}
