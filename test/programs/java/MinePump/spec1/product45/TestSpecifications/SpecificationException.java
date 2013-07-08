package TestSpecifications; 

public  class  SpecificationException  extends RuntimeException {
	
	private static final long serialVersionUID = -6600356723299466152L;

	
	private String specificationName;

	
	
	public SpecificationException(String testCaseName, String message) {
		super(message);
		this.specificationName = testCaseName;
	}

	
	
	public String getSpecificationName() {
		return specificationName;
	}


}
