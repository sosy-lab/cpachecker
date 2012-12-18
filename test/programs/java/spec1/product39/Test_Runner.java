public class Test_Runner {
	private static boolean printLog = false;
	public static String error = null;
	public static String ignoreThisRun = "false";
	public static String actions = "";
	public static String scenarioFinished = "false";
		
	public static void main(String[] args) throws Throwable {
		PL_Interface interf = new PL_Interface_impl();
		try {
			int specification = -1;
			int variation = -1;
			if (args.length==0) {
				System.out.println("VE_Run: NoArguments-> Checking all Specifications");
				// check all Specs
				interf.checkOnlySpecification(-1);
			} else {
				String[] arguments = args[0].trim().split("_");
				if (arguments.length != 2) 
					throw new IllegalArgumentException("must hav an argument of the form interaction_variation!");
				specification = Integer.parseInt(arguments[0]);
				//specification = -2;
				variation = Integer.parseInt(arguments[1]);
				interf.checkOnlySpecification(specification);
				System.out.println("Normal_Run: Argument\"" + args[0] + "\" checking only spec " 
						+ specification + " with var " +variation);
			}

	        // start program
			interf.start(specification, variation);
			if (interf.isAbortedRun()==true) {
				if (printLog) System.out.println("Aborted");
				ignoreThisRun = "true";
				return;
			}
			if (printLog)
				System.out.println("Scenario Succeeded");
		} catch (Throwable e) {
			if (printLog) {
				System.out.println("Scenario Failed with error:" + e.getMessage());
				e.printStackTrace();
			}
			error = e.getMessage();
		} finally {
			for (String a : interf.getExecutedActions()) {
				actions += ":" + a;
			}
		}
	}
	// this method will be replaced during the verification preparation
	static boolean requirementsFulfilled() {return true;}
	
}