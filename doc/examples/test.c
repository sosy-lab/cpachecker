//#include <stdio.h>
//#include <stdlib.h>

int main(){
 int i = 2;
 int a = 2;
//double intOut;
 double myDouble = 11.6;
// char myChar = 'b';
 float myFloat = -5.4;
// float myFloat2 = 5;
//double myDouble2 = 11;
 long testLong = 9223372036854775807; 

//scanf("%i",intOut);
//if(intOut<20){
//	myDouble2 = 15.0;			
//}

 while(1){
  if(i==5){
 goto LAST;
  }else{
   i++;
   a++;
   myFloat=myFloat + 0.1;
//	myFloat2++;
//	myDouble2++;
	testLong = testLong + 1;
  }
 }

 LAST:
 if(a!=5){
 goto ERROR;
 }
 return (0);

 ERROR:
 return (-1);
}
