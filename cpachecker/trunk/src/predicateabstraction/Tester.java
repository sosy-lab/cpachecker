package predicateabstraction;

import java.io.File;
import java.io.FileNotFoundException;
import java.util.Scanner;

public class Tester {

	public static void main(String[] args) {

		String fociFormula = "";

		File fFile = new File("/home/erkan/cpa/mathsatQueries.out");
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
				fociFormula = scanner.nextLine();
				MathSatWrapper.satisfiability(fociFormula);
			}
		}
		finally {
			scanner.close();
		}

//		boolean b = MathSatWrapper.satisfiability(fociFormula);
//		if(b){
//			System.out.println("sat");
//		}
//		else
//			System.out.println("unsat");
	}

}
