extern int __VERIFIER_nondet_int(void);

int SIZE;

int main()
{
	SIZE = __VERIFIER_nondet_int();
	int i = 0;
	if (0 <= SIZE)
	{		
		int a[SIZE];
		
		while (i < SIZE)		{
			a[i]=a[i]-1;
			i = i +1;
		}
	}else{
		;
	}
	return 0;
}
