package pack1; 
  



public class CreateObjects {

  
  private int  num;
  private int  num2;

  public CreateObjects( int n1 , int n2){
    
    num = n1;
    num2 = n2;
   
 }
  
  public CreateObjects( int n){
    
    num = n;
    num2 = n;
   
 }
  
  public CreateObjects(){
    
    num = 0;
    num2 = 0;
   
 }
  
  
  
  private boolean  compare() {
    return (num == num2);
    
  }
    
	public static void main(
			String[] args) { 
	  
	  CreateObjects object1 = new CreateObjects( 2);
	   CreateObjects object3 = new CreateObjects();
	  CreateObjects object2 = new CreateObjects( 2 , 7);

	  
	  assert object1.compare() ;	 	  
	}
	
}