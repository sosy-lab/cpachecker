import util.TypeParameterClass;
import util.SimpleEnum;

public class TypeParameter_true_assert {

  public static void main(String[] args) {
    checkGenericClass();
    checkTypeParameterInConstructorParameter();
    checkTypeParameterInMethodParameter();
    checkTypeParameterInMethodParameterAndReturn();
  }

  private static void checkGenericClass() {
    TypeParameterClass<SimpleEnum> enumWrapper = new TypeParameterClass<SimpleEnum>();
    
    enumWrapper.value = SimpleEnum.A;
    assert enumWrapper.value == SimpleEnum.A;
  }

// Begin of check for paramter in constructor //

  SimpleEnum value;

  public <T> TypeParameter_true_assert(T obj) {
    if (obj instanceof SimpleEnum) {
      value = (SimpleEnum) obj;
    }
  }

  private static void checkTypeParameterInConstructorParameter() {
    TypeParameter_true_assert objectWithNullValue = new TypeParameter_true_assert(new Object());
    TypeParameter_true_assert objectWithEnumValue = new TypeParameter_true_assert(SimpleEnum.B);  

    assert objectWithNullValue.value == null;
    assert objectWithEnumValue.value == SimpleEnum.B;
  }

// Begin of check for type parameter in method

  private static <T> SimpleEnum returnValueIfEnum(T value) {
    if (value instanceof SimpleEnum) {
      return (SimpleEnum) value;
    } else {
      return null;
    }
  }

  private static void checkTypeParameterInMethodParameter() {
    SimpleEnum nullValue = returnValueIfEnum(new Object());
    SimpleEnum concreteValue = returnValueIfEnum(SimpleEnum.A);

    assert nullValue == null;
    assert concreteValue == SimpleEnum.A;
  }

// Begin of check for type parameter in method and it's return type //

  private static <T> T returnValue(T value) {
    return value;
  }

  private static void checkTypeParameterInMethodParameterAndReturn() {
    Object objectValue = returnValue(SimpleEnum.A);
    SimpleEnum enumValue = returnValue(SimpleEnum.B);

    assert objectValue instanceof SimpleEnum;
    assert objectValue == SimpleEnum.A;
    assert enumValue == SimpleEnum.B;
  }
}
