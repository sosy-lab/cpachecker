package org.sosy_lab.cpachecker.fllesh.fql2.translators.ecp;

import java.util.Iterator;

import org.sosy_lab.cpachecker.fllesh.ecp.ECPConcatenation;
import org.sosy_lab.cpachecker.fllesh.ecp.ElementaryCoveragePattern;
import org.sosy_lab.cpachecker.fllesh.fql2.ast.Atom;
import org.sosy_lab.cpachecker.fllesh.fql2.ast.coveragespecification.Concatenation;
import org.sosy_lab.cpachecker.fllesh.fql2.ast.coveragespecification.CoverageSpecification;
import org.sosy_lab.cpachecker.fllesh.fql2.ast.coveragespecification.Quotation;
import org.sosy_lab.cpachecker.fllesh.fql2.ast.coveragespecification.Union;

public class IncrementalCoverageSpecificationTranslator {

  private final PathPatternTranslator mPathPatternTranslator;
  private final CoverageSpecificationTranslator mCoverageSpecificationTranslator;
  
  public IncrementalCoverageSpecificationTranslator(PathPatternTranslator pPathPatternTranslator) {
    mPathPatternTranslator = pPathPatternTranslator;
    mCoverageSpecificationTranslator = new CoverageSpecificationTranslator(mPathPatternTranslator);
  }
  
  public Iterator<ElementaryCoveragePattern> translate(CoverageSpecification pSpecification) {
    if (pSpecification instanceof Atom || pSpecification instanceof Quotation) {
      return mCoverageSpecificationTranslator.translate(pSpecification).iterator();
    }
    else if (pSpecification instanceof Union) {
      Union lUnion = (Union)pSpecification;
      
      return new UnionIterator(lUnion.getFirstSubspecification(), lUnion.getSecondSubspecification());
    }
    else if (pSpecification instanceof Concatenation) {
      Concatenation lConcatenation = (Concatenation)pSpecification;
      
      return new ConcatenationIterator(lConcatenation.getFirstSubspecification(), lConcatenation.getSecondSubspecification());
    }
    else {
      throw new RuntimeException();
    }
  }
  
  private class UnionIterator implements Iterator<ElementaryCoveragePattern> {

    private final Iterator<ElementaryCoveragePattern> mIterator1;
    private final Iterator<ElementaryCoveragePattern> mIterator2;
    
    private UnionIterator(CoverageSpecification pSpecification1, CoverageSpecification pSpecification2) {
      mIterator1 = translate(pSpecification1);
      mIterator2 = translate(pSpecification2);
    }
    
    @Override
    public boolean hasNext() {
      return (mIterator1.hasNext() || mIterator2.hasNext());
    }

    @Override
    public ElementaryCoveragePattern next() {
      if (mIterator1.hasNext()) {
        return mIterator1.next();
      }
      
      return mIterator2.next();
    }

    @Override
    public void remove() {
      throw new UnsupportedOperationException();
    }
    
  }
  
  private class ConcatenationIterator implements Iterator<ElementaryCoveragePattern> {

    private final Iterator<ElementaryCoveragePattern> mIterator1;
    private final CoverageSpecification mSpecification2;
    private Iterator<ElementaryCoveragePattern> mIterator2;
    
    private ElementaryCoveragePattern mPrefix;
    
    private ConcatenationIterator(CoverageSpecification pSpecification1, CoverageSpecification pSpecification2) {
      mIterator1 = translate(pSpecification1);
      mIterator2 = translate(pSpecification2);
      mSpecification2 = pSpecification2;
      
      mPrefix = null;
      
      if (mIterator1.hasNext() && mIterator2.hasNext()) {
        mPrefix = mIterator1.next();
      }
    }
    
    @Override
    public boolean hasNext() {
      return (mPrefix != null);
    }

    @Override
    public ElementaryCoveragePattern next() {
      ECPConcatenation lResult = new ECPConcatenation(mPrefix, mIterator2.next());
      
      if (!mIterator2.hasNext()) {
        if (mIterator1.hasNext()) {
          mPrefix = mIterator1.next();
          mIterator2 = translate(mSpecification2);
        }
        else {
          mPrefix = null;
        }
      }
      
      return lResult;
    }

    @Override
    public void remove() {
      throw new UnsupportedOperationException();
    }
    
  }
  
}
