extern void reach_error();

void foo(int n) 
{
    if (n != 42)
    {
        reach_error();
    }
}


int main() 
{
    foo(42);
}