package cpa.observeranalysis;

import java.io.File;
import java.util.HashMap;
import java.util.Map;

import java_cup.runtime.ComplexSymbolFactory;
import java_cup.runtime.Symbol;
import java_cup.runtime.SymbolFactory;

class ObserverInternalTests {

  /**
   * @param args
   */
  public static void main(String[] args) {
    
    testExpressionEvaluator();
    
    ObserverBoolExpr ex = new ObserverBoolExpr.True();
    System.out.println(ex.eval());
    try {
      //File f = new File("../../Projekt/FormatBeispiel1.txt");
      File f = new File("../../Projekt/Beispiel2.txt");
      
      /*
      SymbolFactory sf1 = new ComplexSymbolFactory();
      Scanner s = new Scanner(new FileInputStream(f), sf1);
      Symbol symb = s.next_token();
      while (symb.sym != sym.EOF) {
        System.out.println(symb);
        symb = s.next_token();
      }
      System.out.println(s.next_token());
      */
      
      
      SymbolFactory sf = new ComplexSymbolFactory();
      //change back if you have problems:   
      //SymbolFactory sf = new DefaultSymbolFactory();
     Symbol symbol = new ObserverParser(new ObserverScanner(new java.io.FileInputStream(f), sf),sf).parse();
     ObserverAutomaton a = (ObserverAutomaton) symbol.value;
     
     
     a.writeDotFile(System.out);
     
    } catch (Exception e) {
      // TODO Auto-generated catch block
      e.printStackTrace();
    }
  }
  
  private static void testExpressionEvaluator() {
    
    Map<String, ObserverVariable> map = new HashMap<String, ObserverVariable>();
    ObserverIntExpr AccessA = new ObserverIntExpr.VarAccess("a", map);
    ObserverIntExpr AccessB = new ObserverIntExpr.VarAccess("b", map);
    
    ObserverActionExpr storeA = new ObserverActionExpr.Assignment("a",
        new ObserverIntExpr.Constant(5)
        , map);
    
    ObserverActionExpr storeB = new ObserverActionExpr.Assignment("b",
        new ObserverIntExpr.Plus(AccessA, new ObserverIntExpr.Constant(2))
        , map);
    
    ObserverBoolExpr bool = new ObserverBoolExpr.EqTest(
        new ObserverIntExpr.Plus(new ObserverIntExpr.Constant(2), AccessA)
        , AccessB
        );
    
    storeA.execute();
    storeB.execute();
    
    System.out.println("Expression Evaluation result: " + bool.eval());
    
  }

}
