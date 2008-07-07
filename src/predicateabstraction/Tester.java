package predicateabstraction;

import java.io.IOException;

public class Tester {

	public static void main(String[] args) {
		try {
			System.out.println(TheoremProverInterface.implies("& [ = a 7 & [ <= a 10 ~ = a 10 ] ]", " & [= temp 12 = temp + [ x y ] = x 7 = y 5 ~ = y 8 ]"));
			System.out.println(TheoremProverInterface.satis("& [  ~ = temp b = temp + [ x y ] = x 7 = y 5 ~ = y 8 ]"));
			
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
//		System.out.println(TheoremProverInterface.sat("| [ false ~ <= 7 90 ]"));
//		System.out.println(TheoremProverInterface.sat("| [ ~ false  = 7 7 ]"));
		
	}
}
