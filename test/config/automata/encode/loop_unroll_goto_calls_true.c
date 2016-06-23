int nondet() {
	int a;
	return a;
}

static unsigned short cas_phy_read(int reg ) 
{ 

  int limit ;
  int tmp ;

  limit = 1000;

  goto ldv_51349;

  ldv_51348: 
  
  int nd = nondet();
  if (nd) {
    return 0;

  }

  ldv_51349: 
  tmp = limit;
  limit = limit - 1;

  if (tmp > 0) {
    goto ldv_51348;
  }

  return (65535U);
}

static void main() {
      cas_phy_read(20);
	cas_phy_read(21);
	cas_phy_read(22);
	cas_phy_read(22);
	cas_phy_read(22);
	cas_phy_read(22);
	cas_phy_read(22);
	cas_phy_read(22);
}
