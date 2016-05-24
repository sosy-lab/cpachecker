extern int __VERIFIER_nondet_int();

int flag=0;

int main()
{
	int N = 20;
	int a[N];
	int i=0;
	int j=N;
	
	while (1)
	{
		
		flag = i;
		flag = j;
		if(a[i]!=a[j]) {
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
