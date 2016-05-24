extern int __VERIFIER_nondet_int();

int flag=0;

int main()
{
	int N = 20;
	int a[N];
	int b[N];
	int i=0;
	int j=N;
	
	while (1)
	{
		flag = i;
		flag = j;
		if(j>=1) 
		{
		  flag = i+1;
		  flag = j-1;
		  if(a[i]>a[i+1] || b[j-1]<b[j])
		  {
			  return -1;
		  }	
		}

		if(a[i]!=b[j]) {
			return 0;
		}
		i++;
		j--;
		if(j<0)
		{
			break;
		}
	}
	return 1;
}
