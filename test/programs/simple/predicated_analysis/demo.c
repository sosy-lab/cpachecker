void main()
{
    int y;
    int x=5;

    if(y>1)
    {
	x=1;
    }
    else
    {
   	x=-1;
    }

    int z=y;

    while(y<5)
    {
    	if(z>1)
    	{
	    x=x+1;
    	}
	else
	{
	    x=x-1;
	}
	y=y+1;
    }
}
