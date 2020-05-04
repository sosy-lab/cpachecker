extern void abort(void); 
void reach_error(){}
void __VERIFIER_assert(int cond) {
  if (!(cond)) {
    ERROR: {reach_error();abort();}
  }
  return;
}
#define SIZE 5
int a[SIZE];
int i;
int main()
{
	for(i=0;i<SIZE;i++)
	{
	a[i] = 0 ;
	}

	for(i=0;i<SIZE/2;i++)
	{
	a[i] = 100 ;// error in the assignment: a[i] = 100 instead of a[i] = 10
	}


	for(i=0;i<SIZE/2;i++)
	{
		__VERIFIER_assert(a[i]==10);
	}
	
	return 0;
}	


