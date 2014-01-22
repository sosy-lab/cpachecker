import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;

/*
 * Very simple C-Code generator: Generates C-Code that declares and 
 * accesses a set number of global and/or local variables. 
 * Useful for quickly generating test cases with many variables.
 * Output can be compiled with cil or directly used with CPAchecker.
 * 
 * Example code:
 * 
 * 		int global_i0;
 * 		int global_i1;
 * 		...
 * 
 * 		int main() {
 *
 *		int local_i0;
 *		int local_i1;
 *		...
 *		
 * 		global_i0 = global_i0;  //if useUninits == true
 * 		OR
 * 		global_i0 = 0;   		//if useUninits == FALSE
 * 		...
 * 		local_i0 = local_i0;   	//if useUninits == true
 * 		OR
 * 		local_i0 = 0;			//if useUninits == FALSE
 * 		...
 * 		}
 */
public class VariablesGenerator {

	public static void main(String[] args) throws IOException {

		String fileName = "variables.c";
		
		//how many variables?
		int numOfVars = 5000;

		//create global variables?
		boolean globVars = true;
		//create local variables?
		boolean locVars = true;
		//use uninitialized variables?
		boolean useUninits = true;
		
		
		File f = new File(fileName);

		BufferedWriter out;

		out = new BufferedWriter(new FileWriter(f));

		//declare global variables
		if (globVars) {
			for (int i = 0; i < numOfVars; i++) {
				out.write("int global_i" + i + ";\n");
			}
		}
		
		out.write("\n" + "int main() {" + "\n\n");
		
		//declare local variables
		if (locVars) {
			for (int i = 0; i < numOfVars; i++) {
				out.write("int local_i" + i + ";\n");
			}
		}
		
		//access global Vars
		if (globVars) {
			for (int i = 0; i < numOfVars; i++) {
				if (useUninits) {
					out.write("global_i" + i + "=global_i" + i + ";\n");
				} else {
					out.write("global_i" + i + "=" + i + ";\n");
				}
			}
		}
		
		//access local Vars
		if (locVars) {
			for (int i = 0; i < numOfVars; i++) {
				if (useUninits) {
					out.write("local_i" + i + "=local_i" + i + ";\n");
				} else {
					out.write("local_i" + i + "=" + i + ";\n");
				}
	
			}
		}
		
		out.write("}");
		
		out.flush();
		out.close();
	}
}
