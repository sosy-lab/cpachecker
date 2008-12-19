/*
 *  CPAchecker is a tool for configurable software verification.
 *  This file is part of CPAchecker. 
 *
 *  Copyright (C) 2007-2008  Dirk Beyer and Erkan Keremoglu.
 *  All rights reserved.
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 *
 *
 *  CPAchecker web page:
 *    http://www.cs.sfu.ca/~dbeyer/CPAchecker/
 */
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
