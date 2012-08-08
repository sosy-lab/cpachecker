package pack3; 

public class SubType2 extends SuperType1 {


  int subNum1;
  int subNum2;
  
  
  public SubType2(){
    
    super();
    subNum1 = 0;
    subNum2 = 0;
  }
  
  
  
  public SubType2(int num1 , int num2){
    
    super();
    subNum1 = num1;
    subNum2 = num2;
  }
  
  public int getNum1(){
    return subNum1;
  }
  
  public int getNum2(){
    return subNum2;
  }
  
}