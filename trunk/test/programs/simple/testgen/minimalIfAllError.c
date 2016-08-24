void main()
{
    int y;
    if(y>1)
    {
	y = 2;
	ERROR:
		goto ERROR;
    }else
    {
	y = 3;
	goto ERROR;
    }
}
