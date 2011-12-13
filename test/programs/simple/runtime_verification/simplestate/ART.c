#include <stdio.h>
int __MONITOR_START_TRANSITION   = 0;
int __MONITOR_END_TRANSITION   = 0;
int k  ;
extern int ( /* missing proto */  nondet_int)() ;
int __BLAST_error  ;
void __initialize__(void) ;
extern int k ;
extern int ( /* missing proto */  nondet_int)() ;
extern int ( /* missing proto */  anti_op)() ;
int __return_479;
int __return_500;
int __return_621;
int __return_642;
int __return_531;
int __return_279;
int entry(void)
{
    int flag ;
    int tmp ;
    int i ;
    int tmp___0 ;
    k = 0;
    tmp = nondet_int();
    flag = tmp;
    i = 0;
    label_266:; 
    if (!(flag))
    {
        tmp___0 = 100;
        label_274:; 
        if (!(! (i < tmp___0)))
        {
            if (!(! flag))
            {
                anti_op();;
                i ++;
                goto label_266;
            }
            else if (! flag)
            {
                k ++;
                label_284:; 
                anti_op();;
                __MONITOR_START_TRANSITION = __MONITOR_START_TRANSITION;
                {
                    int tmp ;
                    int tmp___0 ;
                    {
                        int result ;
                        int mybool ;
                        int tmp ;
                        result = k;
                        tmp = nondet_int();
                        mybool = tmp;
                        if (!(mybool))
                        {
                            result -= 13;
                            label_415:; 
                            if (!(! mybool))
                            {
                                result -= 13;
                                label_422:; 
                                if (!(mybool))
                                {
                                    result -= 13;
                                    label_429:; 
                                    if (!(! mybool))
                                    {
                                        result -= 13;
                                        label_436:; 
                                        if (!(mybool))
                                        {
                                            result -= 13;
                                            label_443:; 
                                            if (!(! mybool))
                                            {
                                                result -= 13;
                                                label_450:; 
                                                if (!(mybool))
                                                {
                                                    result -= 13;
                                                    label_457:; 
                                                    if (!(! mybool))
                                                    {
                                                        result -= 13;
                                                        label_464:; 
                                                        if (!(mybool))
                                                        {
                                                            result -= 13;
                                                            label_471:; 
                                                            if (!(! mybool))
                                                            {
                                                                result -= 13;
                                                                label_478:; 
                                                                 __return_479 = result > 0;
                                                            }
                                                            else if (! mybool)
                                                            {
                                                                result += 26;
                                                                goto label_478;
                                                            }
                                                            tmp = __return_479;
                                                            if (!(tmp))
                                                            {
                                                                tmp___0 = 1;
                                                                label_499:; 
                                                                 __return_500 = tmp___0;
                                                            }
                                                            else if (tmp)
                                                            {
                                                                if (!(k < 0))
                                                                {
                                                                    if (!(k > 100))
                                                                    {
                                                                        tmp___0 = 1;
                                                                        label_493:; 
                                                                        label_495:; 
                                                                        goto label_499;
                                                                    }
                                                                    else if (k > 100)
                                                                    {
                                                                        tmp___0 = 0;
                                                                        goto label_493;
                                                                    }
                                                                }
                                                                else if (k < 0)
                                                                {
                                                                    tmp___0 = 0;
                                                                    goto label_495;
                                                                }
                                                            }
                                                            tmp = __return_500;
                                                            if (!(! tmp))
                                                            {
                                                                __MONITOR_END_TRANSITION = __MONITOR_END_TRANSITION;
                                                                i ++;
                                                                label_518:; 
                                                                if (!(flag))
                                                                {
                                                                    tmp___0 = 100;
                                                                    label_526:; 
                                                                    if (!(! (i < tmp___0)))
                                                                    {
                                                                        if (!(! flag))
                                                                        {
                                                                            anti_op();;
                                                                            __MONITOR_START_TRANSITION = __MONITOR_START_TRANSITION;
                                                                            {
                                                                                int tmp ;
                                                                                int tmp___0 ;
                                                                                {
                                                                                    int result ;
                                                                                    int mybool ;
                                                                                    int tmp ;
                                                                                    result = k;
                                                                                    tmp = nondet_int();
                                                                                    mybool = tmp;
                                                                                    if (!(mybool))
                                                                                    {
                                                                                        result -= 13;
                                                                                        label_557:; 
                                                                                        if (!(! mybool))
                                                                                        {
                                                                                            result -= 13;
                                                                                            label_564:; 
                                                                                            if (!(mybool))
                                                                                            {
                                                                                                result -= 13;
                                                                                                label_571:; 
                                                                                                if (!(! mybool))
                                                                                                {
                                                                                                    result -= 13;
                                                                                                    label_578:; 
                                                                                                    if (!(mybool))
                                                                                                    {
                                                                                                        result -= 13;
                                                                                                        label_585:; 
                                                                                                        if (!(! mybool))
                                                                                                        {
                                                                                                            result -= 13;
                                                                                                            label_592:; 
                                                                                                            if (!(mybool))
                                                                                                            {
                                                                                                                result -= 13;
                                                                                                                label_599:; 
                                                                                                                if (!(! mybool))
                                                                                                                {
                                                                                                                    result -= 13;
                                                                                                                    label_606:; 
                                                                                                                    if (!(mybool))
                                                                                                                    {
                                                                                                                        result -= 13;
                                                                                                                        label_613:; 
                                                                                                                        if (!(! mybool))
                                                                                                                        {
                                                                                                                            result -= 13;
                                                                                                                            label_620:; 
                                                                                                                             __return_621 = result > 0;
                                                                                                                        }
                                                                                                                        else if (! mybool)
                                                                                                                        {
                                                                                                                            result += 26;
                                                                                                                            goto label_620;
                                                                                                                        }
                                                                                                                        tmp = __return_621;
                                                                                                                        if (!(tmp))
                                                                                                                        {
                                                                                                                            tmp___0 = 1;
                                                                                                                            label_641:; 
                                                                                                                             __return_642 = tmp___0;
                                                                                                                        }
                                                                                                                        else if (tmp)
                                                                                                                        {
                                                                                                                            if (!(k < 0))
                                                                                                                            {
                                                                                                                                if (!(k > 100))
                                                                                                                                {
                                                                                                                                    tmp___0 = 1;
                                                                                                                                    label_635:; 
                                                                                                                                    label_637:; 
                                                                                                                                    goto label_641;
                                                                                                                                }
                                                                                                                                else if (k > 100)
                                                                                                                                {
                                                                                                                                    tmp___0 = 0;
                                                                                                                                    goto label_635;
                                                                                                                                }
                                                                                                                            }
                                                                                                                            else if (k < 0)
                                                                                                                            {
                                                                                                                                tmp___0 = 0;
                                                                                                                                goto label_637;
                                                                                                                            }
                                                                                                                        }
                                                                                                                        tmp = __return_642;
                                                                                                                        if (!(! tmp))
                                                                                                                        {
                                                                                                                            __MONITOR_END_TRANSITION = __MONITOR_END_TRANSITION;
                                                                                                                            i ++;
                                                                                                                            goto label_518;
                                                                                                                        }
                                                                                                                        else if (! tmp)
                                                                                                                        {
                                                                                                                            {
                                                                                                                                assert(0); // target state
                                                                                                                            }
                                                                                                                        }
                                                                                                                    }
                                                                                                                    else if (mybool)
                                                                                                                    {
                                                                                                                        result += 26;
                                                                                                                        goto label_613;
                                                                                                                    }
                                                                                                                }
                                                                                                                else if (! mybool)
                                                                                                                {
                                                                                                                    result += 26;
                                                                                                                    goto label_606;
                                                                                                                }
                                                                                                            }
                                                                                                            else if (mybool)
                                                                                                            {
                                                                                                                result += 26;
                                                                                                                goto label_599;
                                                                                                            }
                                                                                                        }
                                                                                                        else if (! mybool)
                                                                                                        {
                                                                                                            result += 26;
                                                                                                            goto label_592;
                                                                                                        }
                                                                                                    }
                                                                                                    else if (mybool)
                                                                                                    {
                                                                                                        result += 26;
                                                                                                        goto label_585;
                                                                                                    }
                                                                                                }
                                                                                                else if (! mybool)
                                                                                                {
                                                                                                    result += 26;
                                                                                                    goto label_578;
                                                                                                }
                                                                                            }
                                                                                            else if (mybool)
                                                                                            {
                                                                                                result += 26;
                                                                                                goto label_571;
                                                                                            }
                                                                                        }
                                                                                        else if (! mybool)
                                                                                        {
                                                                                            result += 26;
                                                                                            goto label_564;
                                                                                        }
                                                                                    }
                                                                                    else if (mybool)
                                                                                    {
                                                                                        result += 26;
                                                                                        goto label_557;
                                                                                    }
                                                                                }
                                                                            }
                                                                        }
                                                                        else if (! flag)
                                                                        {
                                                                            k ++;
                                                                            goto label_284;
                                                                        }
                                                                    }
                                                                    else if (! (i < tmp___0))
                                                                    {
                                                                         __return_531 = 1;
                                                                        goto label_279;
                                                                    }
                                                                }
                                                                else if (flag)
                                                                {
                                                                    tmp___0 = 1000000;
                                                                    goto label_526;
                                                                }
                                                            }
                                                            else if (! tmp)
                                                            {
                                                                {
                                                                    assert(0); // target state
                                                                }
                                                            }
                                                        }
                                                        else if (mybool)
                                                        {
                                                            result += 26;
                                                            goto label_471;
                                                        }
                                                    }
                                                    else if (! mybool)
                                                    {
                                                        result += 26;
                                                        goto label_464;
                                                    }
                                                }
                                                else if (mybool)
                                                {
                                                    result += 26;
                                                    goto label_457;
                                                }
                                            }
                                            else if (! mybool)
                                            {
                                                result += 26;
                                                goto label_450;
                                            }
                                        }
                                        else if (mybool)
                                        {
                                            result += 26;
                                            goto label_443;
                                        }
                                    }
                                    else if (! mybool)
                                    {
                                        result += 26;
                                        goto label_436;
                                    }
                                }
                                else if (mybool)
                                {
                                    result += 26;
                                    goto label_429;
                                }
                            }
                            else if (! mybool)
                            {
                                result += 26;
                                goto label_422;
                            }
                        }
                        else if (mybool)
                        {
                            result += 26;
                            goto label_415;
                        }
                    }
                }
            }
        }
        else if (! (i < tmp___0))
        {
             __return_279 = 1;
            label_279:; 
        }
    }
    else if (flag)
    {
        tmp___0 = 1000000;
        goto label_274;
    }
}
