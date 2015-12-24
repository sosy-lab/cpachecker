package org.sosy_lab.cpachecker.cfa.postprocessing.sequencer;

import java.math.BigInteger;
import java.util.ArrayList;
import java.util.List;

import org.sosy_lab.cpachecker.cfa.ast.FileLocation;
import org.sosy_lab.cpachecker.cfa.ast.c.CInitializer;
import org.sosy_lab.cpachecker.cfa.ast.c.CInitializerExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CInitializerList;
import org.sosy_lab.cpachecker.cfa.ast.c.CIntegerLiteralExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CVariableDeclaration;
import org.sosy_lab.cpachecker.cfa.model.CFAEdge;
import org.sosy_lab.cpachecker.cfa.model.c.CDeclarationEdge;
import org.sosy_lab.cpachecker.cfa.postprocessing.sequencer.context.CThreadContainer;
import org.sosy_lab.cpachecker.cfa.postprocessing.sequencer.utils.ArrayDeclarationUtils;
import org.sosy_lab.cpachecker.cfa.postprocessing.sequencer.utils.CFASequenceBuilder;
import org.sosy_lab.cpachecker.cfa.types.c.CDefaults;
import org.sosy_lab.cpachecker.cfa.types.c.CNumericTypes;
import org.sosy_lab.cpachecker.cfa.types.c.CPointerType;
import org.sosy_lab.cpachecker.cfa.types.c.CStorageClass;
import org.sosy_lab.cpachecker.cfa.types.c.CType;
import org.sosy_lab.cpachecker.cfa.types.c.CVoidType;

import com.google.common.base.Function;

public class ControlVariables {

  private static final String MAIN_FUNCTION_NAME = "main";

  private final int THREAD_COUNT;

  private CThreadContainer threads;


  private final static Function<String, String> GLOBAL_NAME = new Function<String, String>() {

    @Override
    public String apply(String arg0) {
      return arg0;
    }
  };


  private final static Function<String, String> GLOBAL_QUALIFIER_NAME = new Function<String, String>() {

    @Override
    public String apply(String arg0) {
      return GLOBAL_NAME.apply(arg0);
    }
  };



  /**
   * The declaration of the global variable <i>int currentThread;</i>
   */
  private CVariableDeclaration currentThreadDeclaration;
  /**
   * The declaration of the global variable <i>void *
   * threadCreationArguments[T];</i>
   */
  private CVariableDeclaration threadCreationArgumentsArrayDeclaration;

  private CVariableDeclaration threadReturnValueArrayDeclaration;
  /**
   * The declaration of the global variable <i>bool isThreadActive[T];</i>
   */
  private CVariableDeclaration isThreadActiveArrayDeclaration;
  private CVariableDeclaration isThreadFinishedDeclaration;


  public ControlVariables(CThreadContainer threads) {
    this.THREAD_COUNT = threads.getThreadCount();
    this.threads = threads;
    buildGlobalVariableDeclaration();
  }

  /**
   * Creates global variables to the cfa which will be used to manage the thread
   * scheduling.
   *
   * <ul>
   * <li>int contextSwitch[THREADS] = {0};</li>
   * <li>int currentThread = 0;</li>
   * <li>int programmCounterOfThread[THREADS] = {0};</li>
   * <li>int largestSizeOfContextSwitch[THREADS] = {0};</li>
   * <li>void * threadCreationArguments[THREADS];</li>
   * <li>bool isThreadActive[THREADS] = {0};</li>
   * <li>int THREADS = --number of threads--;</li>
   * </ul>
   *
   */
  private void buildGlobalVariableDeclaration() {
    currentThreadDeclaration = createCurrentThreadDeclaration();
    threadCreationArgumentsArrayDeclaration = createThreadCreationArgumentsArrayDeclaration();
    threadReturnValueArrayDeclaration = createThreadReturnValueArrayDeclaration();
    isThreadActiveArrayDeclaration = createIsThreadActiveArrayDeclaration();
    isThreadFinishedDeclaration = createIsThreadFinishedDeclaration();
  }


  private CVariableDeclaration createCurrentThreadDeclaration() {
    String variableName = "currentThreadNumber";
    CVariableDeclaration currentThreadDeclaration = new CVariableDeclaration(FileLocation.DUMMY, true, CStorageClass.AUTO, CNumericTypes.INT, GLOBAL_NAME.apply(variableName), variableName, GLOBAL_QUALIFIER_NAME.apply(variableName),
        CDefaults.forType(CNumericTypes.INT, FileLocation.DUMMY));

    return currentThreadDeclaration;
  }

  @Deprecated
  private CVariableDeclaration createProgramCounterOfThreadArrayDeclaration() {
    String variableName = "programCounterOfThread";
    CVariableDeclaration programCounterOfThreadArrayDeclaration = ArrayDeclarationUtils.buildStaticNumericArrayDeclaration(CNumericTypes.INT, THREAD_COUNT,
        0, variableName);

    return programCounterOfThreadArrayDeclaration;
  }

  private CVariableDeclaration buildStaticVoidPointerArrayDeclaration(int size, String name) {
    CType voidPointerType = new CPointerType(false, false, CVoidType.VOID);
    return ArrayDeclarationUtils.buildStaticGlobalArrayDeclarationWithInitializer(voidPointerType, size, null, name);
  }

  private CVariableDeclaration createThreadCreationArgumentsArrayDeclaration() {
    String variableName = "threadCreationArguments";

    CVariableDeclaration threadCreationArgumentsDeclaration = buildStaticVoidPointerArrayDeclaration(THREAD_COUNT, variableName);
    return threadCreationArgumentsDeclaration;
  }

  private CVariableDeclaration createThreadReturnValueArrayDeclaration() {
    String variableName = "threadReturnValue";

    CVariableDeclaration threadReturnValueArrayDeclaration = ArrayDeclarationUtils.buildStaticNumericArrayDeclaration(CNumericTypes.INT, THREAD_COUNT, 0, variableName);
    return threadReturnValueArrayDeclaration;
  }

  private CVariableDeclaration createIsThreadActiveArrayDeclaration() {
    assert THREAD_COUNT > 0;
    String variableName = "isThreadActive";

    List<CInitializer> arrayInitializers = new ArrayList<CInitializer>();
    arrayInitializers.add(new CInitializerExpression(FileLocation.DUMMY, new CIntegerLiteralExpression(FileLocation.DUMMY, CNumericTypes.BOOL, BigInteger.valueOf(1))));
    for(int i = 0; i < THREAD_COUNT - 1; i++) {
      arrayInitializers.add(new CInitializerExpression(FileLocation.DUMMY, new CIntegerLiteralExpression(FileLocation.DUMMY, CNumericTypes.BOOL, BigInteger.valueOf(0))));
    }

    CInitializerList arrayInitializer = new CInitializerList(FileLocation.DUMMY, arrayInitializers);


    CVariableDeclaration isThreadActiveArrayDeclaration = ArrayDeclarationUtils.buildStaticGlobalArrayDeclarationWithInitializer(CNumericTypes.BOOL, THREAD_COUNT, arrayInitializer, variableName);

    return isThreadActiveArrayDeclaration;
  }

  private CVariableDeclaration createIsThreadFinishedDeclaration() {
    String variableName = "isThreadFinished";

    CVariableDeclaration isThreadFinishedDeclaration= ArrayDeclarationUtils.buildStaticBoolArrayDeclaration(THREAD_COUNT, false, variableName);
    return isThreadFinishedDeclaration;
  }


  public CVariableDeclaration getCurrentThreadDeclaration() {
    return currentThreadDeclaration;
  }

  public CVariableDeclaration getThreadCreationArgumentsArrayDeclaration() {
    return threadCreationArgumentsArrayDeclaration;
  }

  public CVariableDeclaration getThreadReturnValueArrayDeclaration() {
    return threadReturnValueArrayDeclaration;
  }

  public CVariableDeclaration getIsThreadActiveArrayDeclaration() {
    return isThreadActiveArrayDeclaration;
  }

  public CVariableDeclaration getIsThreadFinishedDeclaration() {
    return isThreadFinishedDeclaration;
  }

  public CFAEdge getDummyCurrentThreadDeclarationEdge() {
    return getDummyDeclarationEdge(currentThreadDeclaration);
  }

  public CFAEdge getDummyThreadCreationArgumentsArrayDeclarationEdge() {
    return getDummyDeclarationEdge(threadCreationArgumentsArrayDeclaration);
  }

  public CFAEdge getDummyThreadReturnValueArrayDeclarationEdge() {
    return getDummyDeclarationEdge(threadReturnValueArrayDeclaration);
  }

  public CFAEdge getDummyIsThreadActiveArrayDeclarationEdge() {
    return getDummyDeclarationEdge(isThreadActiveArrayDeclaration);
  }

  public CFAEdge getDummyIsThreadFinishedDeclarationEdge() {
    return getDummyDeclarationEdge(isThreadFinishedDeclaration);
  }


  private CDeclarationEdge getDummyDeclarationEdge(CVariableDeclaration variableDeclaration) {
    return new CDeclarationEdge(variableDeclaration.toString(), FileLocation.DUMMY, CFASequenceBuilder.DUMMY_NODE,
        CFASequenceBuilder.DUMMY_NODE, variableDeclaration);
  }


}
