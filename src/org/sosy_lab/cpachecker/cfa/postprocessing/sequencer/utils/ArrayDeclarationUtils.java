package org.sosy_lab.cpachecker.cfa.postprocessing.sequencer.utils;

import java.math.BigInteger;
import java.util.ArrayList;
import java.util.List;

import javax.annotation.Nullable;

import org.sosy_lab.cpachecker.cfa.ast.FileLocation;
import org.sosy_lab.cpachecker.cfa.ast.c.CExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CInitializer;
import org.sosy_lab.cpachecker.cfa.ast.c.CInitializerExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CInitializerList;
import org.sosy_lab.cpachecker.cfa.ast.c.CIntegerLiteralExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CVariableDeclaration;
import org.sosy_lab.cpachecker.cfa.types.c.CArrayType;
import org.sosy_lab.cpachecker.cfa.types.c.CBasicType;
import org.sosy_lab.cpachecker.cfa.types.c.CNumericTypes;
import org.sosy_lab.cpachecker.cfa.types.c.CSimpleType;
import org.sosy_lab.cpachecker.cfa.types.c.CStorageClass;
import org.sosy_lab.cpachecker.cfa.types.c.CType;

import com.google.common.base.Preconditions;

public class ArrayDeclarationUtils {

  /**
   * Creates a new CVariableDeclaration of an static array with a numeric
   * initialization value. Initializes all array values with this value.
   *
   * @param arrayType
   *          - the type the array should have
   * @param size
   *          - the size of the array which should be created
   * @param defaultInitializationValue
   *          - the initialization value the array will be initialized with
   * @param name
   *          the name of the array
   * @param function
   *          the function where the array is placed in
   * @return an CVariableDeclaration which represents a static numeric array in
   *         the C Code
   */
  private static CVariableDeclaration buildStaticArrayDeclaration(CSimpleType arrayType, int size, @Nullable CInitializerExpression singleInitializer,
      String name) {

    CInitializerList arrayInitializer;
    if (singleInitializer == null) {
      arrayInitializer = null;
    } else {
      List<CInitializer> arrayInitializers = new ArrayList<CInitializer>();
      for (int i = 0; i < size; i++) {
        arrayInitializers.add(singleInitializer);
      }
      arrayInitializer = new CInitializerList(FileLocation.DUMMY,
          arrayInitializers);
    }

    return buildStaticGlobalArrayDeclarationWithInitializer(arrayType, size, arrayInitializer, name);
  }

  public static CVariableDeclaration buildStaticGlobalArrayDeclarationWithInitializer(CType arrayType, int size, @Nullable CInitializerList initializer,
      String name) {
    Preconditions.checkArgument(initializer == null || size == initializer.getInitializers().size());
    assert initializer == null || checkTypeTogetherness(arrayType, initializer);
//    Preconditions.checkArgument(initializer == null || checkTypeTogetherness(arrayType, initializer));

    CExpression arraySize = new CIntegerLiteralExpression(FileLocation.DUMMY, CNumericTypes.INT, BigInteger.valueOf(size));

    /*
     * storage class static is the same as auto, just with reduced visibility to
     * a single compilation unit, and as we only handle single compilation
     * units, we can ignore it.
     */
    CArrayType array = new CArrayType(false, false, arrayType, arraySize);

    CVariableDeclaration arrayDeclatation = new CVariableDeclaration(
        FileLocation.DUMMY, true, CStorageClass.AUTO, array, name, name, name, initializer);

    return arrayDeclatation;
  }

  private static boolean checkTypeTogetherness(CType arrayType, CInitializerList initializer) {
    assert initializer != null;

    for(CInitializer init : initializer.getInitializers()) {
      assert init instanceof CInitializerExpression;

      CInitializerExpression initializerExpression = (CInitializerExpression) init;
      CType initializerType = initializerExpression.getExpression().getExpressionType();

      if(!arrayType.getCanonicalType().equals(initializerType.getCanonicalType())) {
        return false;
      }
    }
    return true;
  }

  public static CVariableDeclaration buildStaticBoolArrayDeclaration(int size, @Nullable Boolean initializerValue, String name) {
    CInitializerExpression singleInitializer;
    if (initializerValue == null) { // no initializer
      singleInitializer = null;
    } else if (initializerValue) {
      singleInitializer = new CInitializerExpression(FileLocation.DUMMY,
          new CIntegerLiteralExpression(FileLocation.DUMMY, CNumericTypes.BOOL, BigInteger.valueOf(1)));
    } else {
      singleInitializer = new CInitializerExpression(FileLocation.DUMMY,
          new CIntegerLiteralExpression(FileLocation.DUMMY, CNumericTypes.BOOL, BigInteger.valueOf(0)));
    }
    return buildStaticArrayDeclaration(CNumericTypes.BOOL, size, singleInitializer, name);
  }

  public static CVariableDeclaration buildStaticNumericArrayDeclaration(CSimpleType type, int size, long initializerValue, String name) {
    if(!CBasicType.INT.equals(type.getType())) {
      throw new IllegalArgumentException("" + type + " is no numeric type! Cannot instantiate with " + initializerValue);
    }

    CInitializerExpression singleInitialization = new CInitializerExpression(FileLocation.DUMMY, new CIntegerLiteralExpression(FileLocation.DUMMY, type, BigInteger.valueOf(initializerValue)));

    return buildStaticArrayDeclaration(type, size, singleInitialization, name);
  }
}
