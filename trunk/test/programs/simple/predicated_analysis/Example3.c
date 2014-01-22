void main()
{
    int y;
    if(y<0)
    {
	y=0;
    }
    int x=y;
    x=x+1;
    int i=0;
    while(1)
    {
	if(i==1)
	{
	    x=x+1;
	    i=0;
	}
	else
	{
	    x=x-1;
	    i=1;
	}
    }
}
