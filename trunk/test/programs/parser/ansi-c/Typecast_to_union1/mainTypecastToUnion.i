# 1 "Typecast_to_union1/main.c"
# 1 "<eingebaut>"
# 1 "<Kommandozeile>"
# 1 "Typecast_to_union1/main.c"



union U
{
  int i;
  double d;
};

int main()
{
  union U u;

  u=(union U)(1>2);
  u=(union U)(1 && 1);
  u=(union U)1.0;
}
