# 1 "Float4/main.c"
# 1 "<eingebaut>"
# 1 "<Kommandozeile>"
# 1 "Float4/main.c"
int main()
{
  double f, f2;

  __CPROVER_assume(!__CPROVER_isnan(f2));
  __CPROVER_assume(!__CPROVER_isinf(f2));
  f=f2;


  assert(100.0+10==110);
  assert(0+f==f);
  assert(f+0==f);
  assert(100+0.5==100.5);
  assert(0.0+0.0+f==f);


  assert(100.0-10==90);
  assert(0-f==-f);
  assert(f-0==f);
  assert(100-0.5==99.5);
  assert(0.0-0.0-f==-f);


  assert(-(-100.0)==100);
  assert(-(1-2.0)==1);
  assert(-(-f)==f);


  assert(100.0*10==1000);
  assert(0*f==0);
  assert(f*0==0);
  assert(100*0.5==50);
  assert(f*1==f);
  assert(1*f==f);
  assert(1.0*1.0*f==f);


  assert(100.0/1.0==100);
  assert(100.1/1.0==100.1);
  assert(100.0/2.0==50);
  assert(100.0/0.5==200);
  assert(0/1.0==0);
  assert(f/1.0==f);


  assert(((double)(float)100)==100.0);
  assert(((unsigned int)100.0)==100.0);
  assert(100.0);
  assert(!0.0);
  assert((int)0.5==0);
  assert((int)0.49==0);
  assert((int)-1.5==-1);
  assert((int)-10.49==-10);


  assert(1.0<2.5);
  assert(1.0<=2.5);
  assert(1.01<=1.01);
  assert(2.5>1.0);
  assert(2.5>=1.0);
  assert(1.01>=1.01);
  assert(!(1.0>=2.5));
  assert(!(1.0>2.5));
  assert(1.0!=2.5);
}
