package org.sosy_lab.cpachecker.cpa.automatonanalysis;

abstract class AutomatonExpression {
  
  abstract ResultValue<?> eval(AutomatonExpressionArguments pArgs);
  
  
  static class ConstantStringExpression extends AutomatonExpression {
    ResultValue<String> myConstantResult;
    public ConstantStringExpression(String pConstantString) {
      super();
      myConstantResult = new ResultValue<String>(pConstantString);
    }
    @Override
    ResultValue<?> eval(AutomatonExpressionArguments pArgs) {
      return myConstantResult;
    }
  }
  
  // TODO: lift CPA Query here
  
  static class ResultValue<resultType> {
    private boolean canNotEvaluate = false;
    private String failureMessage = null; // only set if cannotEvaluate == true
    private String failureOrigin = null;  // only set if cannotEvaluate == true
    private resultType value = null;      // only set if cannotEvaluate == false
    public ResultValue(resultType value) {
      this.value = value;
    }
    public ResultValue(String failureMessage, String failureOrigin) {
      this.canNotEvaluate = true;
      this.failureMessage = failureMessage;
      this.failureOrigin = failureOrigin;
    }
    /**
     * Copies the failure messages from the passed result.
     * This Method assumes that the parameter fulfills canNotEvaluate() == true !
     * @param pResA
     */
    public ResultValue(ResultValue<?> pRes) {
      assert pRes.canNotEvaluate;
      this.canNotEvaluate = true;
      this.failureMessage = pRes.failureMessage;
      this.failureOrigin = pRes.failureOrigin;
    }
    boolean canNotEvaluate() {
      return this.canNotEvaluate;
    }
    /**
     * @returns null if cannotEvaluate() == false
     */
    String getFailureMessage() {
      return failureMessage;
    }
    /**
     * @returns null if cannotEvaluate() == false
     */
    String getFailureOrigin() {
      return failureOrigin;
    }
    /**
     * @returns null if cannotEvaluate() == true
     */
    resultType getValue() {
      return value;
    }
  }
}
