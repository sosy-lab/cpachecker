package fql.frontend.ast.filter;

import fql.frontend.ast.ASTVisitor;

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
    if (pOther != null) {
      return false;
    }
    
    if (pOther instanceof File) {
      File mFileFilter = (File)pOther;
      
      return mFileName.equals(mFileFilter.mFileName);
    }
    
    return false;
  }
  
  @Override
  public void accept(ASTVisitor pVisitor) {
    pVisitor.visit(this);
  }

}
