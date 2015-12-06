package org.sosy_lab.cpachecker.cfa.postprocessing.sequencer;

import java.util.ArrayList;
import java.util.List;

import org.sosy_lab.cpachecker.cfa.ast.FileLocation;
import org.sosy_lab.cpachecker.cfa.ast.c.CFunctionDeclaration;
import org.sosy_lab.cpachecker.cfa.ast.c.CParameterDeclaration;
import org.sosy_lab.cpachecker.cfa.postprocessing.sequencer.utils.PThreadUtils;
import org.sosy_lab.cpachecker.cfa.types.c.CFunctionTypeWithNames;
import org.sosy_lab.cpachecker.cfa.types.c.CNumericTypes;
import org.sosy_lab.cpachecker.cfa.types.c.CPointerType;
import org.sosy_lab.cpachecker.cfa.types.c.CType;
import org.sosy_lab.cpachecker.cfa.types.c.CTypedefType;
import org.sosy_lab.cpachecker.cfa.types.c.CVoidType;

public class POSIXStubs {
  
  public final static CTypedefType THREAD_TYPE = new CTypedefType(false, false, "pthread_t", CNumericTypes.UNSIGNED_LONG_INT);
  public final static CType MUTEXT_TYPE = new CPointerType(false, false, CNumericTypes.BOOL);
  
  private final CFunctionDeclaration pthreadCreateStubDeclaration = buildPthreadStubDeclaration();
  private final CFunctionDeclaration pthreadJoinDeclaration = buildPthreadJoinDeclaration();
  private CFunctionDeclaration pthreadMutexInitDeclaration = buildPthreadMutexInitDeclaraion();
  
  private static CFunctionDeclaration buildPthreadStubDeclaration() {
    List<CParameterDeclaration> newParameterDeclaration = new ArrayList<CParameterDeclaration>();
    
    final String parameterName1 = "__newthread";
    final String parameterName2 = "__arg";
    final String parameterName3 = "__threadNumber";
    
    CType threadTypePointer = new CPointerType(false, false, THREAD_TYPE);
    
    CParameterDeclaration firstParam = new CParameterDeclaration(FileLocation.DUMMY, threadTypePointer, parameterName1);
    firstParam.setQualifiedName(PThreadUtils.PTHREAD_CREATE_NAME +"::"+parameterName1);
    CParameterDeclaration secondParam = new CParameterDeclaration(FileLocation.DUMMY, new CPointerType(false, false, CVoidType.VOID), parameterName2);
    secondParam.setQualifiedName(PThreadUtils.PTHREAD_CREATE_NAME + "::" + parameterName2);
    CParameterDeclaration thirdParam = new CParameterDeclaration(FileLocation.DUMMY, CNumericTypes.LONG_INT, parameterName3);
    thirdParam.setQualifiedName(PThreadUtils.PTHREAD_CREATE_NAME + "::" + parameterName3);
    newParameterDeclaration.add(firstParam);
    newParameterDeclaration.add(secondParam);
    newParameterDeclaration.add(thirdParam);
    
    return new CFunctionDeclaration(FileLocation.DUMMY, new CFunctionTypeWithNames(false, false, CNumericTypes.INT, newParameterDeclaration, false), PThreadUtils.PTHREAD_CREATE_NAME, newParameterDeclaration);
  }
  
  private static CFunctionDeclaration buildPthreadJoinDeclaration() {
    String parameterName = "__th";
    String parameterName2 = "__thread_return";

    List<CParameterDeclaration> newParameterDeclaration = new ArrayList<CParameterDeclaration>();

    CParameterDeclaration firstParam = new CParameterDeclaration(FileLocation.DUMMY,
        THREAD_TYPE, parameterName);
    firstParam.setQualifiedName(PThreadUtils.PTHREAD_CREATE_NAME + "::" + parameterName);
    CParameterDeclaration secondParam = new CParameterDeclaration(FileLocation.DUMMY,
        new CPointerType(false, false, CVoidType.VOID), parameterName2);
    secondParam.setQualifiedName(PThreadUtils.PTHREAD_CREATE_NAME + "::" + parameterName2);
    newParameterDeclaration.add(firstParam);
    newParameterDeclaration.add(secondParam);

    return new CFunctionDeclaration(FileLocation.DUMMY, new CFunctionTypeWithNames(false, false,
        CNumericTypes.INT, newParameterDeclaration, false), PThreadUtils.PTHREAD_JOIN_NAME,
        newParameterDeclaration);
  }
  
  private CFunctionDeclaration buildPthreadMutexInitDeclaraion() {
    String parameterName = "__mutex";

    List<CParameterDeclaration> newParameterDeclaration = new ArrayList<CParameterDeclaration>();

    CParameterDeclaration param = new CParameterDeclaration(FileLocation.DUMMY, MUTEXT_TYPE,
        parameterName);
    param.setQualifiedName(PThreadUtils.PTHREAD_MUTEX_INIT_NAME + "::" + parameterName);
    newParameterDeclaration.add(param);

    return new CFunctionDeclaration(FileLocation.DUMMY, new CFunctionTypeWithNames(false, false,
        CNumericTypes.INT, newParameterDeclaration, false), PThreadUtils.PTHREAD_MUTEX_INIT_NAME,
        newParameterDeclaration);
  }

  public CFunctionDeclaration getPthreadCreateStubDeclaration() {
    return pthreadCreateStubDeclaration;
  }

  public CFunctionDeclaration getPthreadJoinDeclaration() {
    return pthreadJoinDeclaration;
  }

  public CFunctionDeclaration getPthreadMutexInitDeclaration() {
    return pthreadMutexInitDeclaration;
  }
}
