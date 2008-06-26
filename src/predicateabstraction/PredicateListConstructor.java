package predicateabstraction;

import java.io.File;
import java.io.FileNotFoundException;
import java.util.Scanner;

public class PredicateListConstructor {

	public static void constructList(PredicateList predicateList){
		File fFile = new File(PredAbstractionConstants.predicateListPath);  
		Scanner scanner = null;
		try {
			scanner = new Scanner(fFile);
		} catch (FileNotFoundException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		try {
			//first use a Scanner to get each line
			while (scanner.hasNextLine()){
				processLine(predicateList, scanner.nextLine());
			}
		}
		finally {
			scanner.close();
		}
	}

	private static void processLine(PredicateList predicateListString, String line) {
		Scanner scanner = new Scanner(line);
		scanner.useDelimiter(" ");
		
		String operator = "";
		String leftVar = "";
		String rightVar = "";

		if ( scanner.hasNext() ){
			operator = scanner.next();
			leftVar = scanner.next();
			rightVar = scanner.next();
		}

		Predicate predicate = new Predicate(leftVar, Operator.convertToOperator(operator), rightVar); 
		predicateListString.addPredicate(predicate);
	}
}