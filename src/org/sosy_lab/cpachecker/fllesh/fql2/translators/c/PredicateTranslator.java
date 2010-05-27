package org.sosy_lab.cpachecker.fllesh.fql2.translators.c;

import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import org.sosy_lab.cpachecker.fllesh.fql2.ast.Predicate;
import org.sosy_lab.cpachecker.fllesh.fql2.ast.terms.Constant;
import org.sosy_lab.cpachecker.fllesh.fql2.ast.terms.TermVisitor;
import org.sosy_lab.cpachecker.fllesh.fql2.ast.terms.Variable;

public class PredicateTranslator {

  private static Map<Predicate, String> mCache = new HashMap<Predicate, String>();
  
  public static String translate(Predicate pPredicate) {
    if (mCache.containsKey(pPredicate)) {
      return mCache.get(pPredicate);
    }
    
    Set<String> lVariables = new HashSet<String>();
    
    Visitor lVisitor = new Visitor();
    lVariables.addAll(pPredicate.getLeftTerm().accept(lVisitor));
    lVariables.addAll(pPredicate.getRightTerm().accept(lVisitor));
    
    StringBuffer lResult = new StringBuffer();
    
    lResult.append("void predicate(");
    
    boolean isFirst = true;
    
    for (String lVariable : lVariables) {
      if (isFirst) {
        isFirst = false;
      }
      else {
        lResult.append(", ");
      }
      
      lResult.append("int ");
      lResult.append(lVariable);
    }
    
    lResult.append(") { (");
    lResult.append(pPredicate.toString());
    lResult.append("); }");
    
    mCache.put(pPredicate, lResult.toString());
    
    return lResult.toString();
  }
  
  private static class Visitor implements TermVisitor<Set<String>> {
    
    @Override
    public Set<String> visit(Constant pConstant) {
      return Collections.emptySet();
    }

    @Override
    public Set<String> visit(Variable pVariable) {
      return Collections.singleton(pVariable.toString());
    }
    
  }
  
}
