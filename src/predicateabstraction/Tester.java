package predicateabstraction;

import java.io.IOException;

public class Tester {

	public static void main(String[] args) {
		try {
			for(int i=0; i<1000; i++){
				String s = TheoremProverInterface.satis("~ | [ ~ & [  ] <= 10 7 ]");
				String s1 = TheoremProverInterface.satis("& [ = 4 4 = 6 6 = 8 8 = 9 9 <= 10 7 = 5 9 ]");
				if(s1.equals("satisfiable") || s.equals("unsatisfiable") ){
					System.out.println("sacma " + s + " 1: "+ s1);
					System.exit(0);
				}
				else{
					System.out.println(s);
					System.out.println(s1);
				}
			}
			//System.out.println(TheoremProverInterface.satis("& [  ~ = temp b = temp + [ x y ] = x 7 = y 5 ~ = y 8 ]"));
			
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
//		System.out.println(TheoremProverInterface.sat("| [ false ~ <= 7 90 ]"));
//		System.out.println(TheoremProverInterface.sat("| [ ~ false  = 7 7 ]"));
		
	}
}
