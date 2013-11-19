void main()
{
    int y;
    if(y<0)
    {
	y=0;
    }else{
	y=5;
    }
    int x=y;
    x++;
    int i=0;
    while(1)
    {
	if(i==1)
	{
	    x++;
	    i=0;
	}
	else
	{
	    x--;
	    i=1;
	}
    }
}
