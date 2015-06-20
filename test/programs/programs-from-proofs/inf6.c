int flag = 0;
int main() {
  int a; 
  int b;
  int status = 0;
  int as;
  int bs;
	if(a == 1) {
		status = 0;
	}
	else {
		a=0;
		status = 1;
	}
	
	if(status ==1)
	  b = 1;
	else
	  b=0;

	if(a == 1)
		as = 0;
	else
		as = 1;
	if(b == 1)
		bs = 0;
	else
		bs = 1;
		
	if (bs == 1) 
	{
	  if(as == 1)
	   flag = 1;
   }
	else 
	  if(as == 0)
	    flag = 1;
}
