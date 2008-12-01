package cpaplugin;

import java.io.PrintWriter;

/**
 * A class to hold statistics of the analysis
 * @author alb
 */
public interface CPAStatistics {
    /**
     * Prints this group of statistics using the given writer
     * @param out
     */
    public void printStatistics(PrintWriter out);

    /**
     * @return The name for this group of statistics
     */
    public String getName();
}
