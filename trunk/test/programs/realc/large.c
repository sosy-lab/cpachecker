int main()
{

	int x = 10;
	int a, b, c;
	double d;
	for (a = 0; a < x; a++)
	{
		for (b = 0; b < x; b++)
		{
			for (c = 0; c < x; c++)
			{
				d = a + b * c;
			}
		}
	}

	if (a != x || b != x || c != x || d != a + b * c)
	{
		ERROR: return;
	} 
}