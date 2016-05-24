int flag = 0;

void main()
{
    int i = 0;
    int ielen=5;
    int leader_len=1;
    int bufsize = 6;
    char buf[bufsize] ;
    
    if (bufsize < leader_len)
      return 0;
    
    bufsize = bufsize- leader_len;
    int index = 0 +leader_len;

    while( i < ielen && bufsize > 2) {

      /* OK */
      flag=index;
      buf[index] = 'x';
      /* OK */
      flag=index+1;
      buf[index+1] = 'x';
      index = index+2;
      bufsize -= 2;
      i=i+1;
    }
}
