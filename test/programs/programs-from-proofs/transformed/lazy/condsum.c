void main()
{
    int n;
    int flag;
    int x=5;
    int i=0;

    if(flag)
    {
	x=1;
    }
    else
    {
   	x=-1;
    }
// Papierbeispiel hier besser eine for-Schleife?
    while(i<n)
    {
    	if(flag)
    	{
	    x=x+1;
    	}
	else
	{
	    x=x-1;
	}
	i=i+1;
    }
}
