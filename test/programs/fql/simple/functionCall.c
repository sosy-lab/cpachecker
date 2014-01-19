int f() 
{
	int x;
	int __BLAST_NONDET;
	x = __BLAST_NONDET;
	return (x);
}

int main() 
{
	int y;
	y = f();
	if (y != 0) 
	{
		while (1) 
		{
ERROR: ;
		}
	}
	return (y);
}

