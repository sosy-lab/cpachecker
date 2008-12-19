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

import mathsat.api;

public class MathSatWrapper {

	public static boolean satisfiability(String fociFormula){

		//System.out.println(fociFormula);
		//keeps number of calls to sat checker
                //TODO CPACheckerStatistics.numberOfSATSolverCalls++;
		// create the environment
		long msatEnv = api.msat_create_env();
		// initialize the environment with the theories you want
		api.msat_add_theory(msatEnv, mathsat.api.MSAT_UF);
		api.msat_add_theory(msatEnv, mathsat.api.MSAT_LRA);
		api.msat_set_theory_combination(msatEnv, mathsat.api.MSAT_COMB_DTC);

		// create the formula
		String myFociFormula = fociFormula;
		long term = mathsat.api.msat_from_foci(msatEnv, myFociFormula);
		assert(!mathsat.api.MSAT_ERROR_TERM(term));
		// assert the formula in the environment
		mathsat.api.msat_assert_formula(msatEnv, term);

		// check satisfiability
		int result = mathsat.api.msat_solve(msatEnv);
		assert(result != mathsat.api.MSAT_UNKNOWN);

		boolean res;

		if (result == mathsat.api.MSAT_SAT) {
			// return true if satisfiable
			res = true;
		} else {
			//return false if unsat
			assert(result == mathsat.api.MSAT_UNSAT);
			res = false;
		}

		// destroy the env at the end
		mathsat.api.msat_destroy_env(msatEnv);
		return res;
	}

	public static ThreeValuedBoolean implies(String r1, String r2) throws IOException{
		String s;
		s = "~ | [ " + "~ " + r1 + " " + r2 + " ]";
		boolean b = false;
		try {
			b = satisfiability(s);
		} catch(Throwable e) {
			e.printStackTrace();
			System.exit(0);
		}
		if(b){
			return ThreeValuedBoolean.FALSE;
		}
		else{
			return ThreeValuedBoolean.TRUE;
		}
	}
}
