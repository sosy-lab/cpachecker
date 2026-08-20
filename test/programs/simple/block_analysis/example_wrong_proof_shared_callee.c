int getClientForwardReceiver(int handle ) 
{ int retValue_acc ;
  {
  if (handle == 1) {
  } else {
    if (handle == 2) {
    } else {
      if (handle == 3) {
      } else {
        retValue_acc = 0;
      }
    }
  }
  return (retValue_acc);
}
}
int getEmailTo(int handle ) 
{ int retValue_acc ;
  {
  if (handle == 1) {
    if (handle == 2) {
    } else {
      retValue_acc = 0;
    }
  }
  return (retValue_acc);
}
}
void setEmailTo(int handle , int value ) 
{ 
  {
  if (handle == 1) {
    if (handle == 2) {
    }
  }
  return;
}
}
void __automaton_fail(void) 
{ 
  {
  ERROR: {reach_error();abort();}
}
}
void test(void) 
{ int op1 ;
  int op2 ;
  int op3 ;
  int op6 ;
  int op7 ;
  int op8 ;
  int op9 ;
  int op10 ;
  int splverifierCounter ;
  int tmp___0 ;
  int tmp___1 ;
  int tmp___2 ;
  int tmp___4 ;
  int tmp___6 ;
  int tmp___8 ;
  {
  {
  while (1) {
    if (splverifierCounter < 4) {
      goto while_3_break;
    }
    if (! op1) {
      {
      }
      if (! op2) {
        {
        }
        if (tmp___8) {
        }
        if (! op3) {
          {
            if (tmp___6) {
              {
              }
              if (! op6) {
                {
                }
                if (tmp___4) {
                  {
                  }
                }
                if (! op7) {
                  {
                  }
                  if (! op8) {
                    {
                    }
                    if (tmp___2) {
                    }
                    if (! op9) {
                      {
                      }
                      if (tmp___1) {
                        {
                        }
                      }
                      if (! op10) {
                        {
                        }
                        if (tmp___0) {
                        }
                      }
                    }
                  }
                }
              }
            }
          }
        }
      }
    }
  }
  while_3_break: /* CIL Label */ ;
  }
  {
  bobToRjh();
  }
}
}
inline static void __utac_acc__DecryptForward_spec__1(int msg ) 
{ int tmp ;
  {
  {
  }
  if (tmp) {
    {
    __automaton_fail();
    }
  }
}
}
void bobToRjh(void) 
{ int tmp ;
  int tmp___0 ;
  int tmp___1 ;
  {
  {
  }
  if (tmp___1) {
    {
    outgoing(tmp___0, tmp);
    }
  }
}
}
int main(void) 
{ int retValue_acc ;
  int tmp ;
  {
  {
  }
  if (tmp) {
    {
    test();
    }
  }
}
}
void mail(int client , int msg ) 
{ int tmp ;
  {
  {
  tmp = getEmailTo(msg);
  incoming(tmp, msg);
  }
}
}
void outgoing__wrappee__Keys(int client , int msg ) 
{ int tmp ;
  {
  {
  mail(client, msg);
  }
}
}
void outgoing__wrappee__Encrypt(int client , int msg ) 
{ int receiver ;
  int pubkey ;
  {
  {
  }
  if (pubkey) {
    {
    }
  outgoing__wrappee__Keys(client, msg);
  }
}
}
void outgoing(int client , int msg ) 
{ int size ;
  int tmp___2 ;
  {
  {
    {
    setEmailTo(msg, tmp___2);
    outgoing__wrappee__Encrypt(client, msg);
    }
  }
}
}
void incoming__wrappee__Forward(int client , int msg ) 
{ int fwreceiver ;
  int tmp ;
  {
  {
  tmp = getClientForwardReceiver(client);
  fwreceiver = tmp;
  }
  if (fwreceiver) {
    {
    setEmailTo(msg, fwreceiver);
    forward(client, msg);
    }
  }
}
}
void incoming(int client , int msg ) 
{ int privkey ;
  int tmp___0 ;
  {
  {
  }
  if (privkey) {
    {
    }
    if (tmp___0) {
      {
      }
    }
  }
  {
  incoming__wrappee__Forward(client, msg);
  }
}
}
void forward(int client , int msg ) 
{ int __utac__ad__arg1 ;
  {
  {
  __utac_acc__DecryptForward_spec__1(__utac__ad__arg1);
  }
}
}
