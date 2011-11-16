
int main()
{
    int a;

    a = 0;

    int tmp;
    tmp = doSomething(a);
}

int doSomething(int z)
{
    int y;
    int x;

    x = z;
    y = x;

    if(y != x)
        goto ERROR;

    goto END;

    ERROR:
        ;

    END:
        ;
}
