extern int __VERIFIER_nondet_int();

int flag=0;

void main()
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
		b[j]=a[i];
		i++;
		j--;
		if(j<0)
		{
			break;
		}
	}
}
