 // unused inside expression list with return statment

int main()
{
  (
      4,
      ({
        int c = 42;
        ({return 0;});
      })
    );

    ERROR: // unreachable
      return 1;
}
