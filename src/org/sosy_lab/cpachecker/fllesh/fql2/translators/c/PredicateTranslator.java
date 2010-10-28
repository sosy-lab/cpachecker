package org.sosy_lab.cpachecker.fllesh.fql2.translators.c;

import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import org.sosy_lab.cpachecker.fllesh.fql2.ast.Predicate;
import org.sosy_lab.cpachecker.util.predicates.Constant;
import org.sosy_lab.cpachecker.util.predicates.TermVisitor;
import org.sosy_lab.cpachecker.util.predicates.Variable;

public class PredicateTranslator {

  private static Map<Predicate, String> mCache = new HashMap<Predicate, String>();
  
  public static String translate(Predicate pPredicate) {
    if (mCache.containsKey(pPredicate)) {
      return mCache.get(pPredicate);
    }
    
    Set<String> lVariables = new HashSet<String>();
    
    Visitor lVisitor = new Visitor();
    lVariables.addAll(pPredicate.getPredicate().getLeftTerm().accept(lVisitor));
    lVariables.addAll(pPredicate.getPredicate().getRightTerm().accept(lVisitor));
    
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
    
    String lPredicateString = pPredicate.toString();
    
    String lPredicate = lPredicateString.substring(2, lPredicateString.length() - 2);
    
    lResult.append(") { (");
    lResult.append(lPredicate);
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
