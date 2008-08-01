package cpaplugin;

/**
 * Keeps static variables to save some run-time statictics
 * @author erkan
 *
 */
public class CPACheckerStatistics {

	public static int noOfNodes;
	public static int noOfReachedSet;
	public static int noOfTransferRelations = 0;

	public static int numberOfSATSolverCalls = 0;
	public static long totalAnalysisTime;

	public static void printStatictics(){
		System.out.println("Number of Nodes on CFA " + noOfNodes);
		System.out.println("Number of transfer functions " + noOfTransferRelations);
		System.out.println("Number of SAT Solver Calls " + numberOfSATSolverCalls);
		System.out.println("Size of reached set " + noOfReachedSet);
		System.out.println( "Total Time of Analysis " + 
				(int) totalAnalysisTime / (1000 * 60 * 60) + " hr, " +  
				(int) totalAnalysisTime / (1000 * 60) + " min, " + 
				(int) totalAnalysisTime / 1000 + " sec, " + 
				(int) totalAnalysisTime % 1000 + " ms");
	}
}
