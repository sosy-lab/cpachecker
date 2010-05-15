
public class test2 {
   
  void bla() {
    new resultValue<Object>("","");
  }
  
  static class resultValue<resultType> {
    private boolean canNotEvaluate = false;
    private String failureMessage = null; // only set if cannotEvaluate == true
    private String failureOrigin = null;  // only set if cannotEvaluate == true
    private resultType value = null;      // only set if cannotEvaluate == false
    public resultValue(resultType value) {
      this.value = value;
    }
    public resultValue(String failureMessage, String failureOrigin) {
      this.canNotEvaluate = true;
      this.failureMessage = failureMessage;
      this.failureOrigin = failureOrigin;
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
