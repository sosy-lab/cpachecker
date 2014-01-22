
int algorithm(int a, int b)
{
	int result;

	result = a * b;

    result = result / 8;

    result = result + 1024;

    result = result / 16;

	return result;
}

int main()
{
	int t1, t2, t3, value, a, b;

	if(a > -513)
	{
		if(a < 513)
    	{
			if(b > -1025)
			{
				if(b < 1025)
				{
					t1 = a * b;
					t2 = t1 >> 3;
					t3 = (t2 + 1024) >> 4;
					
					value = algorithm(a,b);

					if(value == t3)
						goto EXIT;
					else
					{
						int inputA = a;
						int inputB = b;
						
						goto ERROR;
					}
				}
			}
		}
	}

	goto EXIT;

	ERROR:
		;

	EXIT:
		;
}