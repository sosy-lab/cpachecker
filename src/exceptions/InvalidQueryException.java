package exceptions;

/**
 * Signals that the check method of a AbstractElement has 
 * been passed an Argument that could not be evaluated. 
 * @author rhein
 */
public class InvalidQueryException extends CPATransferException {
  private static final long serialVersionUID = 3410773868391514648L;
  
  /**
   * Constructs an {@code InvalidQueryException} with {@code null}
   * as its error detail message.
   */
  public InvalidQueryException() {
      super();
  }

  /**
   * Constructs an {@code InvalidQueryException} with the specified detail message.
   *
   * @param message
   *        The detail message (which is saved for later retrieval
   *        by the {@link #getMessage()} method)
   */
  public InvalidQueryException(String message) {
      super(message);
  }
}
