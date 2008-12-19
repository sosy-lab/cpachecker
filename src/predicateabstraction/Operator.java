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

public enum Operator {

	equals,
	notEquals,
	smaller,
	larger,
	smallarOrEqual,
	largerOrEqual;

	@Override
	public String toString() {
		switch(this) {
		case equals:   return "=";
		case notEquals:  return "~ =";
		case smaller:  return "<";
		case larger: return ">";
		case smallarOrEqual: return "<=";
		case largerOrEqual: return ">=";
		}
		return "";
	}

	public static Operator convertToOperator(String s){

		if(s.compareTo("=") == 0){
			return equals;
		}
		else if(s.compareTo("!=") == 0){
			return notEquals;
		}
		else if(s.compareTo("<") == 0){
			return smaller;
		}
		else if(s.compareTo(">") == 0){
			return larger;
		}
		else if(s.compareTo("<=") == 0){
			return smallarOrEqual;
		}
		else if(s.compareTo(">=") == 0){
			return largerOrEqual;
		}
		else{
			System.out.println("Invalid Input " + s);
			System.out.println("Remove the new line on predicated file");
			return null;
		}
	}
}
