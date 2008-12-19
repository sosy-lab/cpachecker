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
package octagon;

public class OctWrapper {

	/* Initialization */
	public native boolean J_init ();  			//int oct_init()

	/* Octagon Creation */
	public native Octagon J_empty (int n);			//oct_t* oct_empty (var_t n)
	public native Octagon J_universe (int n);		//oct_t* oct_universe (var_t n)
	public native void J_free (Octagon oct);		//void oct_free (oct_t* m)

	/* Query Functions */
	public native int J_dimension (Octagon oct);			//var_t oct_dimension (oct_t* m)
	public native int J_nbconstraints (Octagon oct);		//size_t oct_nbconstraints (oct_t* m)

	/* Test Functions */
	public native boolean J_isEmpty (Octagon oct);			//bool oct_is_empty (oct_t* m)
	public native int J_isEmptyLazy (Octagon oct);			//tbool oct_is_empty_lazy (oct_t* m)
	public native boolean J_isUniverse (Octagon oct);		//bool oct_is_universe (oct_t* m)
	public native boolean J_isIncludedIn (Octagon oct1, Octagon oct2);	//bool oct_is_included_in (oct_t* ma, oct_t* mb)
	public native int J_isIncludedInLazy (Octagon oct1, Octagon oct2);	//tbool oct_is_included_in_lazy (oct_t* ma, oct_t* mb)
	public native boolean J_isEqual (Octagon oct1, Octagon oct2);		//bool oct_is_equal (oct_t* ma, oct_t* mb)
	public native int J_isEqualLazy (Octagon oct1, Octagon oct2);		//tbool oct_is_equal_lazy (oct_t* ma, oct_t* mb)
	public native boolean J_isIn (Octagon oct1, Num[] array);			//bool oct_is_in (oct_t* m, const num_t* v)

	/* Operators */
	public native Octagon J_intersection (Octagon oct1, Octagon oct2, boolean dest);	//oct_t* oct_intersection (oct_t* ma, oct_t* mb, bool destructive)
	public native Octagon J_union (Octagon oct1, Octagon oct2, boolean dest);			//oct_t* oct_convex_hull (oct_t* ma, oct_t* mb, bool destructive)
	/* int widening = 0 -> OCT_WIDENING_FAST
	 * int widening = 1 ->  OCT_WIDENING_ZERO
	 * int widening = 2 -> OCT_WIDENING_UNIT*/
	public native Octagon J_widening (Octagon oct1, Octagon oct2, boolean dest, int widening);	//oct_t* oct_widening( oct_t* ma, oct_t* mb, bool destructive,oct_widening_type)
	public native Octagon J_narrowing (Octagon oct1, Octagon oct2, boolean dest);		//oct_t* oct_narrowing (oct_t* ma, oct_t* mb, bool destructive)

	/* Transfer Functions */
	public native Octagon J_forget (Octagon oct, int k, boolean dest);		//oct_t* oct_forget (oct_t* m, var_t k, bool destructive)
	//public native Octagon J_addBinConstraints (Octagon oct, int nb, Constraint[] consArray, boolean dest);    //oct_t* oct_add_bin_constraints ( oct_t* m, unsigned int nb, const oct_cons* cons, bool destructive)
	public native Octagon J_assingVar (Octagon oct, int k, Num[] array, boolean dest);	// oct_t* oct_assign_variable ( oct_t* m, var_t x, const num_t* tab,bool destructive
	public native Octagon J_substituteVar (Octagon oct, int x, Num[] array, boolean dest);	//oct_t* oct_substitute_variable ( oct_t* m, var_t x, const num_t* tab, bool destructive)
	public native Octagon J_addConstraint (Octagon oct, Num[] array, boolean dest);	//oct_t* oct_add_constraint ( oct_t* m, const num_t* tab, bool destructive)
	public native Octagon J_intervAssingVar (Octagon oct, int k, Num[] array, boolean dest);	// oct_t* oct_inter_assign_variable ( oct_t* m, var_t x, const num_t* tab,bool destructive
	public native Octagon J_intervSubstituteVar (Octagon oct, int x, Num[] array, boolean dest);	//oct_t* oct_inter_substitute_variable ( oct_t* m, var_t x, const num_t* tab, bool destructive)
	public native Octagon J_intervAddConstraint (Octagon oct, Num[] array, boolean dest);	//oct_t* oct_inter_add_constraint ( oct_t* m, const num_t* tab, bool destructive)

	/* change of dimensions */
	public native Octagon J_addDimenensionAndEmbed (Octagon oct, int k, boolean dest);		//oct_t* oct_add_dimensions_and_embed( oct_t* m, var_t dimsup, bool destructive)
	public native Octagon J_addDimenensionAndProject (Octagon oct, int k, boolean dest);	//oct_t* oct_add_dimensions_and_project( oct_t* m, var_t dimsup, bool destructive)
	public native Octagon J_removeDimension (Octagon oct, int k, boolean dest);			//oct_t*  oct_remove_dimensions( oct_t* m, var_t dimsup, bool destructive)
	public native Octagon J_removeDimensionAtPosition (Octagon oct, int position, int numOfDims, boolean dest);  //oct_t* oct_remove_dimensions_multi( oct_t* m, const dimsup_t* t,	size_t size_t, bool destructive)

	// TODO implement rest of the functions

	public native void J_print (Octagon oct);		//void oct_print (const oct_t* m)

	/* For debuggin purposes */
	public native Octagon getRandomOct();

    static
    {
      System.loadLibrary("JOct");
    }
}
