static int ldv_mutex_i_mutex_of_inode  =    1;

__inline static void ldv_error(void) 
{ 
  ERROR: ;
  goto ERROR;
}

__inline static int cap_drop() 
{ 
  int dest ;
  unsigned int __capi ;

  __capi = 0U;
  while (__capi <= 1U) {
    __capi = __capi + 1U;
  }
  return (dest);
}

int nfsd_setuser() 
{ 
  int tmp___11 ;
  int tmp___0 ;  
  int cap_effective;

  if (tmp___11) {
    cap_effective = cap_drop();
  } else {
    cap_effective = tmp___0;
  }
  return (0);
}

void ldv_mutex_unlock_i_mutex_of_inode() 
{ 

  if (ldv_mutex_i_mutex_of_inode != 2) {
    ldv_error();
  } 
  ldv_mutex_i_mutex_of_inode = 1;
  return;
}

void main(void) 
{ 
  int nfserr ;
  int *dentry ;

  nfserr = nfsd_setuser();
  if (dentry != 0) {
    ldv_mutex_unlock_i_mutex_of_inode();
  } 
  if (ldv_mutex_i_mutex_of_inode != 1) {
    ldv_error();
  } 
  return;
}

