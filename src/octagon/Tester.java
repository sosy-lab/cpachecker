package octagon;

public class Tester {

	static OctWrapper ow = new OctWrapper();
	
	public static void main(String[] args) {
		System.out.println("Init: " + ow.J_init());
//		Octagon emp_oct =ow.J_empty(6);
//		System.out.println("Empty Octagon: " + emp_oct);
//		System.out.println("Is Empty (Empty) " + ow.J_isEmpty(emp_oct));
//		System.out.println("Is Empty lazy(Empty) " + ow.J_isEmptyLazy(emp_oct));
//		Octagon oct = ow.J_universe(3);
//		Octagon oct1 = ow.J_universe(3);
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
//		Octagon random_oct3 = ow.getRandomOct();
//		System.out.println("Random OCT --> " + random_oct3 + "\n");
//		Num[] numArray = new Num[2];
//		Num n1 = new Num(1.2);
//		Num n2 = new Num(0.4);
//		numArray[0] = n1; numArray[1] = n2;
//		System.out.println("Assign VAR:" + ow.J_assingVar(random_oct3, 0, numArray, true));
//		System.out.println("Substitute VAR:" + ow.J_substituteVar(random_oct3, 1, numArray, true));
//		System.out.println("Add Constr:" + ow.J_addConstraint(random_oct3, numArray, true));
		//System.out.println("Interv Assign VAR:" + ow.J_intervAssingVar(random_oct3, 0, numArray, true));
		//System.out.println("Interv Subs VAR:" + ow.J_intervSubstituteVar(random_oct3, 0, numArray, true));
//		Octagon occ =  ow.J_addDimenensionAndEmbed(random_oct3, 1, true);
//		System.out.println("Add DIM and Proj" + ow.J_addDimenensionAndProject(occ, 1, true));
//		System.out.println("Remove Dim" + ow.J_removeDimenension(occ, 2, true));
//		ow.J_print(occ);
		
		double[] dArr = {0.0, -18.0, 18.0, 0.0, 7.0, -11.0, 0.0, -4.0, 11.0, -7.0, 4.0, 0.0 };
		Num[] matrix = new Num[dArr.length];
		for(int i=0; i<dArr.length; i++){
			Num n = new Num(dArr[i]);
			matrix[i]=n;
		}
		Octagon oct = new Octagon(2, 1, 2, null, matrix);
		ow.J_print(oct);
		System.out.println(oct);
	}

}
