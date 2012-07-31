package pack1; 
  

import pack1.pack2.Function;
import pack3.Function2;

public class FunctionCall {


  
	public static void main(
			String[] args) { 
	  
    int n = 32;
    
    n = Function.teileDurch2(n);    
    n = Function2.teileDurch2(n);
    n = Function3.teileDurch2(n);
    assert(n == 4);
    n = teile(n ,n);
    assert(n == 1);
	  
	  
	}
	
	public static int teile(int op , int op2) {
	  return op / op2;            
	}   
	
}