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

public class PredicateListConstructor {

	public static void constructList(PredicateList predicateList, String functionName, String fileName){
		File fFile = new File(PredAbstractionConstants.getFileLoc(functionName, fileName));
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
		scanner.useDelimiter("/");

		String operator = "";
		String leftVar = "";
		String rightVar = "";

		if ( scanner.hasNext() ){
			operator = scanner.next();
			leftVar = scanner.next();
			rightVar = scanner.next();
		}

		if(leftVar.startsWith("*")){
			leftVar = leftVar.substring(1);
			leftVar = "__cpa___starOp__[" + leftVar + "]";
		}

		if(rightVar.startsWith("*")){
			rightVar = rightVar.substring(1);
			rightVar = "__cpa___starOp__[" + rightVar + "]";
		}

		Predicate predicate = new Predicate(leftVar, Operator.convertToOperator(operator), rightVar);
		predicateListString.addPredicate(predicate);
	}
}