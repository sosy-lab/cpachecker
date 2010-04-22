package fllesh.ecp.reduced;

import java.util.HashMap;
import java.util.Map;

public class IdCreator implements ASTVisitor<Map<Pattern, Integer>> {

  private Map<Pattern, Integer> mMap;
  
  public IdCreator() {
    mMap = new HashMap<Pattern, Integer>();
  }
  
  private Map<Pattern, Integer> add(Pattern pPattern) {
    mMap.put(pPattern, mMap.size());
    
    return mMap;
  }
  
  @Override
  public Map<Pattern, Integer> visit(Atom pAtom) {
    return add(pAtom);
  }

  @Override
  public Map<Pattern, Integer> visit(Concatenation pConcatenation) {
    add(pConcatenation);
    
    pConcatenation.getFirstSubpattern().accept(this);
    return pConcatenation.getSecondSubpattern().accept(this);
  }

  @Override
  public Map<Pattern, Integer> visit(Repetition pRepetition) {
    add(pRepetition);
    
    return pRepetition.getSubpattern().accept(this); 
  }

  @Override
  public Map<Pattern, Integer> visit(Union pUnion) {
    add(pUnion);
    
    pUnion.getFirstSubpattern().accept(this);
    return pUnion.getSecondSubpattern().accept(this);
  }

}
