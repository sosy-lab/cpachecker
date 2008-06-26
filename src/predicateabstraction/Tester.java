package predicateabstraction;

import java.io.IOException;

public class Tester {

	public static void main(String[] args) {
		try {
//			System.out.println(TheoremProverInterface.implies("& [  ]", "~ & [ <= 9 8 ~= 9 8 ] ]"));
			System.out.println(TheoremProverInterface.satis("~ | [ ~ & [ & [ ~ = a 6 ~ = a 7 <= a 90 ~ & [ <= 9 a ~ = 9 a ] ~ = b 24 <= b 12  ] <= a 9 ] ~ = a 6 ]"));
			
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
//		System.out.println(TheoremProverInterface.sat("| [ false ~ <= 7 90 ]"));
//		System.out.println(TheoremProverInterface.sat("| [ ~ false  = 7 7 ]"));
		
		
	}
}
