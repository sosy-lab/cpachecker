package cpa.observeranalysis;

import java.io.File;

import java_cup.runtime.ComplexSymbolFactory;
import java_cup.runtime.Symbol;
import java_cup.runtime.SymbolFactory;

/**
 * This class contains Tests for the ObserverAnalysis
 */
class ObserverInternalTests {

  /**
   * Runs some tests for the observerAnalysis
   * @param args
   */
  public static void main(String[] args) {
    ObserverBoolExpr ex = new ObserverBoolExpr.True();
    System.out.println(ex.eval(null));
    try {
      File f = new File("test/tests/observerAutomata/LockingAutomatonAstComp.txt");
      
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
    
    testExpressionEvaluator();
    testASTcomparison();
  }
  
  private static void testExpressionEvaluator() {
    /*
    Map<String, ObserverVariable> map = new HashMap<String, ObserverVariable>();
    ObserverIntExpr AccessA = new ObserverIntExpr.VarAccess("a");
    ObserverIntExpr AccessB = new ObserverIntExpr.VarAccess("b");
    
    ObserverActionExpr storeA = new ObserverActionExpr.Assignment("a",
        new ObserverIntExpr.Constant(5));
    
    ObserverActionExpr storeB = new ObserverActionExpr.Assignment("b",
        new ObserverIntExpr.Plus(AccessA, new ObserverIntExpr.Constant(2)));
    
    ObserverBoolExpr bool = new ObserverBoolExpr.EqTest(
        new ObserverIntExpr.Plus(new ObserverIntExpr.Constant(2), AccessA)
        , AccessB
        );
    
    storeA.execute(map);
    storeB.execute(map);
    
    System.out.println("Expression Evaluation result: " + bool.eval(map));
    */
  }
  private static void testASTcomparison() {
   
   testAST("x=5;", "x= $?;");
   testAST("x=5;", "x= 10;");
   //ObserverASTComparator.printAST("x=10;");
   testAST("x=5;", "$? =10;");
   testAST("x  = 5;", "$?=$?;");
   
   testAST("a = 5;", "b    = 5;");
   
   testAST("init(a);", "init($?);");
   testAST("init();", "init($?);");
   
   testAST("init(a, b);", "init($?, b);");
   testAST("init(a, b);", "init($?, c);");
   
   
   testAST("init();", "init();;"); // two ';' lead to not-equal
   testAST("x = 5;", "x=$?");
   testAST("x = 5", "x=$?;");
   testAST("x = 5;;", "x=$?");
   
   
   testAST("f();", "f($?);");
   testAST("f(x);", "f($?);");
   testAST("f(x, y);", "f($?);");
   
   testAST("f(x);", "f(x, $?);");
   testAST("f(x, y);", "f(x, $?);");
   testAST("f(x, y, z);", "f(x, $?);");
   
  }
  /**
   * Tests the equality of two strings as used the ASTComparison transition.
   * @param src sourcecode string
   * @param pattern string in the observer automaton definition (may contain $?)
   */
  static void testAST(String src, String pattern) {
    System.out.print("AST Test of ");
    System.out.print(src);
    System.out.print(" and ");
    System.out.print(pattern);
    System.out.print(" returns ");
    System.out.print(ObserverASTComparator.generateAndCompareASTs(src, pattern));
    System.out.println();
  }
  
  

}
