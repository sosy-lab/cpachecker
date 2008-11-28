package octagon;

import java.util.HashSet;
import java.util.Set;

import cpaplugin.cpa.cpas.octagon.OctElement;

public class Tester {

	static OctWrapper ow = new OctWrapper();

	public static void main(String[] args) {
//		System.out.println("Init: " + ow.J_init());
//		Octagon emp_oct =ow.J_empty(6);
//		System.out.println("Empty Octagon: " + emp_oct);
//		System.out.println("Is Empty (Empty) " + ow.J_isEmpty(emp_oct));
//		System.out.println("Is Empty lazy(Empty) " + ow.J_isEmptyLazy(emp_oct));
		//	Octagon oct1 = ow.J_universe(6);
		//Octagon oct2 = ow.J_universe(6);
//		System.out.println("Universal Octagon " + oct);
//		System.out.println("Dimension: " + ow.J_dimension(oct));
//		System.out.println("NB Const.: " + ow.J_nbconstraints(oct));
//		System.out.println("Is Empty (Full): " + ow.J_isEmpty(oct));
//		System.out.println("Is Empty Lazy(Full): " + ow.J_isEmptyLazy(oct));
//		System.out.println("Is Universe(Yes): " + ow.J_isUniverse(oct));
//		System.out.println("Is Included(Yes): " + ow.J_isIncludedIn(oct, oct));
//		System.out.println("Is Included Lazy(Yes): " + ow.J_isIncludedInLazy(oct, oct));
//		Octagon random_oct1 = ow.getRandomOct();
//		Octagon random_oct2 = ow.getRandomOct();
//		System.out.println(random_oct2);
//		System.out.println("Is Equal (yes): " + ow.J_isEqual(random_oct1, random_oct2));
//		System.out.println("Is Equal Lazy (yes): " + ow.J_isEqualLazy(random_oct1, random_oct2));
//		System.out.println("After Intersection " + ow.J_intersection(random_oct1, random_oct2, true));
//		System.out.println("After Union " + ow.J_union(random_oct1, random_oct2, true));
//		System.out.println("After Widening " + ow.J_widening(random_oct1, random_oct2, true, 0));
//		System.out.println("After Narrowing " + ow.J_narrowing(random_oct1, random_oct2, true));
//		System.out.println("After Forget: " + ow.J_forget(random_oct1, 0, true));
//		Octagon random_oct1 = ow.getRandomOct();
//		System.out.println("Random OCT 1 --> ");
//		ow.J_print(random_oct1);
//		Octagon random_oct2 = ow.getRandomOct();
//		System.out.println("Random OCT 2 --> ");
//		ow.J_print(random_oct2);
//		Num[] numArray1 = {new Num(1), new Num(0), new Num(0), new Num(0), new Num(0), new Num(0)};
//		Num[] numArray2 = {new Num(0), new Num(1), new Num(0), new Num(0), new Num(0), new Num(0)};
//		Num[] numArray3 = {new Num(0), new Num(0), new Num(1), new Num(0), new Num(0), new Num(0)};
//		Num[] numArray4 = {new Num(0), new Num(0), new Num(0), new Num(1), new Num(0), new Num(0)};
//		Num[] numArray5 = {new Num(0), new Num(0), new Num(0), new Num(0), new Num(1), new Num(0)};
//		Num[] numArray6 = {new Num(0), new Num(0), new Num(0), new Num(0), new Num(0), new Num(1)};
//		Num n1 = new Num(1.2);
//		Num n2 = new Num(0.4);
//		numArray[0] = n1; numArray[1] = n2;
//		ow.J_assingVar(oct1, 10, numArray1, true);
//		ow.J_assingVar(oct1, 20, numArray2, true);
//		ow.J_assingVar(oct1, 30, numArray3, true);
//		ow.J_assingVar(oct1, 40, numArray4, true);
//		ow.J_assingVar(oct1, 50, numArray5, true);
//		ow.J_assingVar(oct1, 60, numArray6, true);
//		System.out.println("Substitute VAR:" + ow.J_substituteVar(random_oct3, 1, numArray, true));
//		System.out.println("Add Constr:" + ow.J_addConstraint(random_oct3, numArray, true));
		//System.out.println("Interv Assign VAR:" + ow.J_intervAssingVar(random_oct3, 0, numArray, true));
		//System.out.println("Interv Subs VAR:" + ow.J_intervSubstituteVar(random_oct3, 0, numArray, true));
//		Octagon occ =  ow.J_addDimenensionAndEmbed(random_oct3, 1, true);
//		System.out.println("Add DIM and Proj" + ow.J_addDimenensionAndProject(occ, 1, true));
//		System.out.println("Remove Dim" + ow.J_removeDimenension(occ, 2, true));
//		ow.J_print(occ);

//		double[] dArr = {0.0, -18.0, 18.0, 0.0, 7.0, -11.0, 0.0, -4.0, 11.0, -7.0, 4.0, 0.0 };
//		Num[] matrix = new Num[dArr.length];
//		for(int i=0; i<dArr.length; i++){
//		Num n = new Num(dArr[i]);
//		matrix[i]=n;
//		}
//		Octagon oct = new Octagon(2, 1, 2, null, matrix);
//		ow.J_print(oct);
//		System.out.println(oct);
//		System.out.println("Removing 0 from 1 -->");
//		ow.J_print(ow.J_removeDimensionAtPosition(random_oct1, 0, 1, true));

//		System.out.println("Removing 1 and 2 from 2 -->");
//		ow.J_print(ow.J_removeDimensionAtPosition(random_oct2, 1, 2, true));

		OctElement octelem = new OctElement();
		octelem.addVar("a", "main");
		octelem.addVar("b", "main");
		octelem.addVar("c", "main");
		octelem.addVar("d", "main");
		octelem.addVar("e", "main");
		
		octelem.update(LibraryAccess.addDimension(octelem, 5));

		Num[] array = new Num[octelem.getNumberOfVars()+1];

		for(int i=0; i<array.length-1; i++){
			array[i] = new Num(0);
		}

		array[array.length-1] = new Num(44);
		//array[variableId] = new Num(1);

		int lvar = octelem.getVariableId(new HashSet<String>(), "c", "main");
		System.out.println(octelem.getOctagon().getDimension());
		octelem.update(LibraryAccess.assignVar(octelem, lvar, array));
		System.out.println("==== 1 ==== \n" + octelem);
		
		for(int i=0; i<array.length-1; i++){
			array[i] = new Num(0);
		}
		array[array.length-1] = new Num(124);
		array[0] = new Num(1);
		array[2] = new Num(1);
		octelem.update(LibraryAccess.addConstraint(octelem, array));
		
		System.out.println("==== 2 ==== \n" + octelem);
		

	}

}
