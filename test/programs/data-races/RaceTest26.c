struct componentname {
   unsigned int cn_flags ;
   char *cn_pnbuf ;
   char *cn_nameptr ;
};

struct nameidata {
   unsigned int ni_loopcnt ;
   struct componentname ni_cnd ;
};

int namei(struct nameidata *ndp ) 
{ 
  struct componentname *cnp ;
  int i = 0;
  
  cnp = & ndp->ni_cnd;
  cnp->cn_nameptr = cnp->cn_pnbuf;
  if ((int )*(cnp->cn_nameptr) == 47) {
    i++;
  }
  lookup(ndp);
}

int lookup(struct nameidata *ndp ) 
{ 
  char *cp___0 ;
  struct componentname *cnp ;

  cnp = & ndp->ni_cnd;
  cp___0 = cnp->cn_nameptr;
  if ((int )*cp___0 != 0 && (int )*cp___0 != 47) {
    cp___0 = cp___0 + 1;
  }
  if ((int )*cp___0 == 47 && ((int )*(cp___0 + 1) == 47 || (int )*(cp___0 + 1) == 0)) {
    cnp->cn_flags = 0;
  } 
}
