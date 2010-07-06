package org.sosy_lab.cpachecker.util.symbpredabstraction.mathsat;

import org.sosy_lab.cpachecker.util.symbpredabstraction.interfaces.Model;

public class MathsatModel implements Model {

  public MathsatModel(long lMathsatEnvironmentID) {
    
    System.out.println("*************** MODEL ***************");
    
    long lModelIterator = mathsat.api.msat_create_model_iterator(lMathsatEnvironmentID);
      
    while (mathsat.api.msat_model_iterator_has_next(lModelIterator) != 0) {
      long[] lModelElement = mathsat.api.msat_model_iterator_next(lModelIterator);
        
      System.out.println(mathsat.api.msat_term_repr(lModelElement[0]) + " = " + mathsat.api.msat_term_repr(lModelElement[1]));
    }
    
    System.out.println("############### MODEL ###############");
  }
  
}
