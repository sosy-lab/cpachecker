package fllesh.fql.frontend.ast.filter;

import fllesh.fql.frontend.ast.ASTVisitor;

public class File implements Filter {

  String mFileName;
  
  public File(String pFileName) {
    assert(pFileName != null);
    
    mFileName = pFileName;
  }
  
  public String getFileName() {
    return mFileName;
  }
  
  @Override
  public String toString() {
    return "@FILE(" + mFileName + ")";
  }
  
  @Override
  public int hashCode() {
    return 95948 + mFileName.hashCode();
  }
  
  @Override
  public boolean equals(Object pOther) {
    if (this == pOther) {
      return true;
    }
    
    if (pOther == null) {
      return false;
    }
    
    if (pOther.getClass() == getClass()) {
      File mFileFilter = (File)pOther;
      
      return mFileName.equals(mFileFilter.mFileName);
    }
    
    return false;
  }
  
  @Override
  public <T> T accept(ASTVisitor<T> pVisitor) {
    assert(pVisitor != null);
    
    return pVisitor.visit(this);
  }

}
