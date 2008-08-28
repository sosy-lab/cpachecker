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
    
    public MainCPAStatistics() {
        subStats = new LinkedList<CPAStatistics>();
        programStartingTime = 0;
        analysisStartingTime = 0;
        analysisEndingTime = 0;
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

        long hours = totalTimeInMillis / (1000 * 60 * 60);
        totalTimeInMillis %= (1000 * 60 * 60);
        long minutes = totalTimeInMillis / (1000 * 60);
        totalTimeInMillis %= (1000 * 60);
        out.println("Total Time Elapsed " + 
                hours + " hr, " +  
                minutes + " min, " + 
                totalTimeInMillis / 1000 + " sec, " + 
                totalTimeInMillis % 1000 + " ms");
        hours = totalAbsoluteTimeMillis / (1000 * 60 * 60);
        totalAbsoluteTimeMillis %= (1000 * 60 * 60);
        minutes = totalAbsoluteTimeMillis / (1000 * 60);
        totalAbsoluteTimeMillis %= (1000 * 60);
        out.println(
                "Total Time Elapsed including CFA construction " + 
                hours + " hr, " +  
                minutes + " min, " + 
                totalAbsoluteTimeMillis / 1000 + " sec, " + 
                totalAbsoluteTimeMillis % 1000 + " ms");
        
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

}
