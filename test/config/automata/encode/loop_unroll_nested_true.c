
int nondet() {
	int a;
	return a;
}

static int main() 
{ 
  unsigned int cfg ;
  int i ;
  int _min1 ;
  int _min2 ;
  unsigned int tmp___0 ;
  unsigned int phy_id ;
  int j ;
  unsigned short tmp___1 ;
  unsigned short tmp___2 ;
  unsigned int tmp___3 ;
  unsigned short tmp___4 ;
  unsigned short tmp___5 ;

  i = 0;

  goto ldv_51951;
  ldv_51950: 

  j = 0;

  goto ldv_51948;
  ldv_51947: 

  tmp___1 = nondet();
  phy_id = (unsigned int )((int )tmp___1 << 16);
  tmp___2 = nondet();
  phy_id = (unsigned int )tmp___2 | phy_id;
  if (phy_id != 0U && phy_id != 4294967295U) {
    goto done;
  }

  j = j + 1;
  ldv_51948: ;
  if (j <= 2) {
    goto ldv_51947;
  }

  i = i + 1;
  ldv_51951: ;
  if (i <= 31) {
    goto ldv_51950;
  }

  return (-1);
  done: 
  return (0);

}
