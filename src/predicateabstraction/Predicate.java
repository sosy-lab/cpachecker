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

import java.io.IOException;

import logging.CPACheckerLogger;
import logging.CustomLogLevel;



public class Predicate {

	private String firstVariable;
	private Operator operator;
	private String secondVariable;
	private ThreeValuedBoolean truthValue;

	public Predicate(String leftvariable, Operator op, String rightVariable){
		firstVariable = leftvariable;
		operator = op;
		secondVariable = rightVariable;
		truthValue = ThreeValuedBoolean.DONTKNOW;
	}

	public Predicate(Predicate predicate) {
		firstVariable = predicate.getFirstVariable();
		operator = predicate.getOperator();
		secondVariable = predicate.getSecondVariable();
		truthValue = predicate.getTruthValue();
	}

	public void setTruthValue(ThreeValuedBoolean value){
		truthValue = value;
	}

	public String getFirstVariable(){
		return firstVariable;
	}

	public String getSecondVariable(){
		return secondVariable;
	}

	public Operator getOperator(){
		return operator;
	}

	public ThreeValuedBoolean getTruthValue(){
		return truthValue;
	}

	public String getPredicateAsQuery(){

		String res = "";

		if(operator == Operator.equals || operator == Operator.smallarOrEqual ||
				operator == Operator.notEquals){
			res =  operator + " " + firstVariable + " " +  secondVariable;
		}
		else if(operator == Operator.largerOrEqual){
			res = "<= " + secondVariable + " " +  firstVariable;
		}
		else if(operator == Operator.larger){
			res = "& [ " + "<= " + secondVariable + " " +  firstVariable +
			" ~ = " + secondVariable + " " +  firstVariable + " ]";
		}
		else if(operator == Operator.smaller){
			res = "& [ " + "<= " + firstVariable + " " +  secondVariable +
			" ~= " + secondVariable + " " +  firstVariable + " ]";
		}
		else {
			assert(false);
			res = "";
		}

		if(truthValue == ThreeValuedBoolean.TRUE){
			return res;
		}
		else if(truthValue == ThreeValuedBoolean.FALSE){
			return "~ " + res;
		}
		else{
			return "";
		}
	}

	public String getPredicateAsString(){
		String res = "";

		if(operator == Operator.equals || operator == Operator.smallarOrEqual ||
				operator == Operator.notEquals){
			res =  operator + " " + firstVariable + " " +  secondVariable;
		}
		else if(operator == Operator.largerOrEqual){
			res = "<= " + secondVariable + " " +  firstVariable;
		}
		else if(operator == Operator.larger){
			res = " ~= " + secondVariable + " " +  firstVariable + " ]";
		}
		else if(operator == Operator.smaller){
			res = "& [ " + "<= " + firstVariable + " " +  secondVariable +
			" ~= " + secondVariable + " " +  firstVariable + " ]";
		}
		else {
			assert(false);
			// TODO exception
			System.exit(0);
			res = "";
		}

		return res;

	}

	public boolean samePredicate(Predicate other){
		if(!(firstVariable.compareTo(other.getFirstVariable())==0))
			return false;
		if(!(secondVariable.compareTo(other.getSecondVariable())==0))
			return false;
		if(operator != other.getOperator())
			return false;
		return true;
	}


	public String WPAssignment(String leftVar, String rigthVar, Operator op, boolean negated){
		String s1 = "";
		String s2 = "";
		String wp = "";

		assert(op == Operator.equals);

		if(leftVar.compareTo(this.firstVariable)==0){
			s1 = rigthVar;
		}
		else{
			s1 = this.firstVariable;
		}
		if(leftVar.compareTo(this.secondVariable)==0){
			s2 = rigthVar;
		}
		else{
			s2 = this.secondVariable;
		}

		if(s1.equals(this.firstVariable) && s2.equals(this.secondVariable)){
			return "____cpa_same_predicate";
		}

		if(operator == Operator.equals || operator == Operator.smallarOrEqual ||
				operator == Operator.notEquals){
			wp = operator + " " + s1 + " " +  s2;
		}
		else if(operator == Operator.largerOrEqual){
			wp = "<= " + s2 + " " +  s1;
		}
		else if(operator == Operator.larger){
			wp = "& [ " + "<= " + s2 + " " +  s1 +
			" ~= " + s2 + " " +  s1 + " ]";
		}
		else if(operator == Operator.smaller){
			wp = "& [ " + "<= " + s1 + " " +  s2 +
			" ~= " + s1 + " " +  s2 + " ]";
		}
		else {
			assert(false);
			return null;
		}

		if(!negated)
			return wp;
		else
			return "~ " + wp;
	}

	public String WPAssumption(boolean negated){
		if(!negated)
			return getPredicateAsString();
		else
			return "~ " + getPredicateAsString();
	}


	@Override
	public boolean equals(Object other){
		Predicate otherPredicate = (Predicate) other;
		if(this.firstVariable.compareTo(otherPredicate.getFirstVariable())!=0)
			return false;
		if(this.secondVariable.compareTo(otherPredicate.getSecondVariable())!=0)
			return false;
		if(this.operator != otherPredicate.getOperator())
			return false;
		if(this.truthValue != otherPredicate.getTruthValue())
			return false;
		return true;
	}

	@Override
	public String toString(){
		return firstVariable + " " + operator + " " + secondVariable + ": " + truthValue ;
	}

	public void updateAssumption(String previousState, String instruction) throws IOException{

		String preCondition = "& [ " + previousState + " " + instruction + " ]";
		String postCondition = WPAssumption(false);

		if (MathSatWrapper.implies(preCondition, postCondition) == ThreeValuedBoolean.TRUE){
			setTruthValue(ThreeValuedBoolean.TRUE);
			return;
		}

		postCondition = WPAssumption(true);

		if (MathSatWrapper.implies(preCondition, postCondition) == ThreeValuedBoolean.TRUE){
			setTruthValue(ThreeValuedBoolean.FALSE);
			return;
		}

		setTruthValue(ThreeValuedBoolean.DONTKNOW);
	}

	public void updateAssignment(String previousState, String leftVar, String rightVar, Operator op) throws IOException{

		String postCondition = WPAssignment(leftVar, rightVar, op, false);

		if(postCondition.equals("____cpa_same_predicate")){
			return;
		}

		if(postCondition.contains("__________cpa_________unknownVal___")){
			setTruthValue(ThreeValuedBoolean.DONTKNOW);
			return;
		}

		if (MathSatWrapper.implies(previousState, postCondition) == ThreeValuedBoolean.TRUE){
			setTruthValue(ThreeValuedBoolean.TRUE);
			return;
		}

		postCondition = WPAssignment(leftVar, rightVar, op, true);

		if (MathSatWrapper.implies(previousState, postCondition) == ThreeValuedBoolean.TRUE){
			setTruthValue(ThreeValuedBoolean.FALSE);
			return;
		}

		else{
			setTruthValue(ThreeValuedBoolean.DONTKNOW);
		}
	}

	public void updateFunctionCall(String previousState, String parameterAssignment) throws IOException{

		CPACheckerLogger.log(CustomLogLevel.SpecificCPALevel, "Function Call Update on Predicate: " +
				this.getPredicateAsString());

		String currentState = "& [ " + previousState +  " " + parameterAssignment + " ]";

		if (MathSatWrapper.implies(currentState, getPredicateAsString()) == ThreeValuedBoolean.TRUE){
			CPACheckerLogger.log(CustomLogLevel.SpecificCPALevel, "Predicate is set to TRUE ");
			setTruthValue(ThreeValuedBoolean.TRUE);
			return;
		}

		if (MathSatWrapper.implies(currentState, " ~ " + getPredicateAsString()) == ThreeValuedBoolean.TRUE){
			CPACheckerLogger.log(CustomLogLevel.SpecificCPALevel, "Predicate is set to FALSE ");
			setTruthValue(ThreeValuedBoolean.FALSE);
			return;
		}
		CPACheckerLogger.log(CustomLogLevel.SpecificCPALevel, "Predicate is set to DONTKNOW");
		setTruthValue(ThreeValuedBoolean.DONTKNOW);
	}

	public void updateFunctionReturn(String query) throws IOException {

		if (MathSatWrapper.implies(query, getPredicateAsString()) == ThreeValuedBoolean.TRUE){
			setTruthValue(ThreeValuedBoolean.TRUE);
			return;
		}

		if (MathSatWrapper.implies(query, " ~ " + getPredicateAsString()) == ThreeValuedBoolean.TRUE){
			setTruthValue(ThreeValuedBoolean.FALSE);
			return;
		}
		setTruthValue(ThreeValuedBoolean.DONTKNOW);
	}

	public boolean containsVariable(String modifiedVariableName) {
		return firstVariable.equals(modifiedVariableName) || secondVariable.equals(modifiedVariableName);
	}

	@Override
	public Predicate clone(){
		return new Predicate(this);
	}
}
