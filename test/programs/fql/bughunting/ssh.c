extern int __VERIFIER_nondet_int();

int main()
{ 
  int s__info_callback = __VERIFIER_nondet_int() ;
  int s__version = __VERIFIER_nondet_int() ;
  int s__hit = __VERIFIER_nondet_int() ;
  int s__init_buf___0 = __VERIFIER_nondet_int() ;
  int s__debug = __VERIFIER_nondet_int() ;
  int s__ctx__info_callback = __VERIFIER_nondet_int() ;
  int s__s3__flags = __VERIFIER_nondet_int() ;
  int s__s3__tmp__cert_req = __VERIFIER_nondet_int() ;
  int s__s3__tmp__reuse_message = __VERIFIER_nondet_int() ;
  int s__s3__tmp__new_cipher__algorithms = __VERIFIER_nondet_int() ;
  unsigned long tmp = __VERIFIER_nondet_int();
  int num1 = __VERIFIER_nondet_int() ;
  int tmp___3 = __VERIFIER_nondet_int() ;
  int tmp___4 = __VERIFIER_nondet_int() ;
  int tmp___5 = __VERIFIER_nondet_int() ;
  int tmp___6 = __VERIFIER_nondet_int() ;
  int tmp___7 = __VERIFIER_nondet_int() ;
  int tmp___8 = __VERIFIER_nondet_int() ;
  int tmp___9 = __VERIFIER_nondet_int() ;
  
  int s__state = 12292 ;
  int s__s3__tmp__next_state___0 ;
  int buf ;
  int cb = 0 ;
  int ret = -1 ;
  int new_state ;
  int state ;
  int skip = 0 ;
  int blastFlag = 0;
  
  if (s__info_callback != 0) { 
    cb = s__info_callback;
  } else {
    if (s__ctx__info_callback != 0) { 
      cb = s__ctx__info_callback; 
    }
  }
  while (1) {
    
    while_0_continue: /* CIL Label */ ;
    
    state = s__state;
    
    if (s__state == 12292) {
      goto switch_1_12292;
    } else {
      if (s__state == 16384) {
	goto switch_1_16384;
      } else {
	if (s__state == 4096) {
	  goto switch_1_4096;
	} else {
	  if (s__state == 20480) {
	    goto switch_1_20480;
	  } else {
	    if (s__state == 4099) {
	      goto switch_1_4099;
	    } else {
	      if (s__state == 4368) {
		goto switch_1_4368;
	      } else {
		if (s__state == 4369) {
		  goto switch_1_4369;
		} else {
		  if (s__state == 4384) {
		    goto switch_1_4384;
		  } else {
		    if (s__state == 4385) {
		      goto switch_1_4385;
		    } else {
		      if (s__state == 4400) {
			goto switch_1_4400;
		      } else {
			if (s__state == 4401) {
			  goto switch_1_4401;
			} else {
			  if (s__state == 4416) {
			    goto switch_1_4416;
			  } else {
			    if (s__state == 4417) {
			      goto switch_1_4417;
			    } else {
			      if (s__state == 4432) {
				goto switch_1_4432;
			      } else {
				if (s__state == 4433) {
				  goto switch_1_4433;
				} else {
				  if (s__state == 4448) {
				    goto switch_1_4448;
				  } else {
				    if (s__state == 4449) {
				      goto switch_1_4449;
				    } else {
				      if (s__state == 4464) {
					goto switch_1_4464;
				      } else {
					if (s__state == 4465) {
					  goto switch_1_4465;
					} else {
					  if (s__state == 4466) {
					    goto switch_1_4466;
					  } else {
					    if (s__state == 4467) {
					      goto switch_1_4467;
					    } else {
					      if (s__state == 4480) {
						goto switch_1_4480;
					      } else {
						if (s__state == 4481) {
						  goto switch_1_4481;
						} else {
						  if (s__state == 4496) {
						    goto switch_1_4496;
						  } else {
						    if (s__state == 4497) {
						      goto switch_1_4497;
						    } else {
						      if (s__state == 4512) {
							goto switch_1_4512;
						      } else {
							if (s__state == 4513) {
							  goto switch_1_4513;
							} else {
							  if (s__state == 4528) {
							    goto switch_1_4528;
							  } else {
							    if (s__state == 4529) {
							      goto switch_1_4529;
							    } else {
							      if (s__state == 4560) {
								goto switch_1_4560;
							      } else {
								if (s__state == 4561) {
								  goto switch_1_4561;
								} else {
								  if (s__state == 4352) {
								    goto switch_1_4352;
								  } else {
								    if (s__state == 3) {
								      goto switch_1_3;
								    } else {
								      goto switch_1_default;
								      if (0) {
									switch_1_12292: 
									  s__state = 4096;
									  switch_1_16384: ;
									  switch_1_4096: ;
									  switch_1_20480: ;
									  switch_1_4099: 
									    
									      if (s__version != 66048) {
									      ret = -1;
									      goto end;
									    }
									    
									    if (s__init_buf___0 == 0) {
									      buf = __VERIFIER_nondet_int();
									      if (buf == 0) {
										ret = -1;
										goto end;
									      }
									      if (! tmp___3) {
										ret = -1;
										goto end;
									      }
									      s__init_buf___0 = buf;
									    }
									    if (! tmp___4) {
									      ret = -1;
									      goto end;
									    }
									    if (! tmp___5) {
									      ret = -1;
									      goto end;
									    }
									    s__state = 4368;
									    goto switch_1_break;
									    switch_1_4368: ;
									    switch_1_4369: 
									      ret = __VERIFIER_nondet_int();
									      if (blastFlag == 0) {
										blastFlag = 1;
									      }
									      if (ret <= 0) {
										goto end;
									      }
									      s__state = 4384;
									      
									      goto switch_1_break;
									      switch_1_4384: ;
									      switch_1_4385: 
										ret = __VERIFIER_nondet_int();
										if (blastFlag == 1) {
										  blastFlag = 2;
										}
										if (ret <= 0) {
										  goto end;
										}
										if (s__hit) {
										  s__state = 4560;
										} else {
										  s__state = 4400;
										}
										goto switch_1_break;
										switch_1_4400: ;
										switch_1_4401: ;
										
										if (s__s3__tmp__new_cipher__algorithms != 256) {
										  skip = 1;
										} else {
										  ret = __VERIFIER_nondet_int();
										  if (blastFlag == 2) {
										    blastFlag = 3;
										  }
										  if (ret <= 0) {
										    goto end;
										  }
										}
										s__state = 4416;
										goto switch_1_break;
										switch_1_4416: ;
										switch_1_4417: 
										  ret = __VERIFIER_nondet_int();
										  if (blastFlag == 3) {
										    blastFlag = 4;
										  }
										  if (ret <= 0) {
										    goto end;
										  }
										  s__state = 4432;
										  if (! tmp___6) {
										    ret = -1;
										    goto end;
										  }
										  goto switch_1_break;
										  switch_1_4432: ;
										  switch_1_4433: 
										    ret = __VERIFIER_nondet_int();
										    if (blastFlag == 4) {
										      goto ERROR;
										    }
										    if (ret <= 0) {
										      goto end;
										    }
										    s__state = 4448;
										    goto switch_1_break;
										    switch_1_4448: ;
										    switch_1_4449: 
										      ret = __VERIFIER_nondet_int();
										      if (blastFlag == 4) {
											blastFlag = 5;
										      }
										      if (ret <= 0) {
											goto end;
										      }
										      if (s__s3__tmp__cert_req) {
											s__state = 4464;
										      } else {
											s__state = 4480;
										      }
										      goto switch_1_break;
										      switch_1_4464: ;
										      switch_1_4465: ;
										      switch_1_4466: ;
										      switch_1_4467: 
											ret = __VERIFIER_nondet_int();
											if (ret <= 0) {
											  goto end;
											}
											s__state = 4480;
											goto switch_1_break;
											switch_1_4480: ;
											switch_1_4481: 
											  ret = __VERIFIER_nondet_int();
											  if (ret <= 0) {
											    goto end;
											  }
											  if (s__s3__tmp__cert_req == 1) {
											    s__state = 4496;
											  } else {
											    s__state = 4512;
											  }
											  goto switch_1_break;
											  switch_1_4496: ;
											  switch_1_4497: 
											    ret = __VERIFIER_nondet_int();
											    if (ret <= 0) {
											      goto end;
											    }
											    s__state = 4512;
											    goto switch_1_break;
											    switch_1_4512: ;
											    switch_1_4513: 
											      ret = __VERIFIER_nondet_int();
											      if (ret <= 0) {
												goto end;
											      }
											      s__state = 4528;
											      if (! tmp___7) {
												ret = -1;
												goto end;
											      }
											      if (! tmp___8) {
												ret = -1;
												goto end;
											      }
											      goto switch_1_break;
											      switch_1_4528: ;
											      switch_1_4529: 
												ret = __VERIFIER_nondet_int();
												if (ret <= 0) {
												  goto end;
												}
												s__state = 4352;
												s__s3__flags = s__s3__flags + 5;
												if (s__hit) {
												  s__s3__tmp__next_state___0 = 3;
												  
												  if (s__s3__flags != 2L) {
												    s__state = 3;
												    s__s3__flags = s__s3__flags + 4L;
												  }
												  
												} else {
												  s__s3__tmp__next_state___0 = 4560;
												}
												goto switch_1_break;
												switch_1_4560: ;
												switch_1_4561: 
												  ret = __VERIFIER_nondet_int();
												  if (ret <= 0) {
												    goto end;
												  }
												  if (s__hit) {
												    s__state = 4512;
												  } else {
												    s__state = 3;
												  }
												  goto switch_1_break;
												  switch_1_4352: 
												    
												    if (num1 > 0) {
												      num1 = tmp___9;
												      
												      if (num1 <= 0) {
													ret = -1;
													goto end;
												      }
												    }
												    
												    s__state = s__s3__tmp__next_state___0;
												    goto switch_1_break;
												    switch_1_3: 
												      if (s__init_buf___0 != 0) {
													s__init_buf___0 = 0;
												      }
												      ret = 1;
												      
												      goto end;
												      switch_1_default: 
													ret = -1;
													goto end;
								      } else {
									switch_1_break: ;
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
    if (! s__s3__tmp__reuse_message) {
      if (! skip) {
	if (s__debug) {
	  ret = __VERIFIER_nondet_int();
	  if (ret <= 0) {
	    goto end;
	  }
	}
      }
    }
    skip = 0;
  }
  
  while_0_break: /* CIL Label */ ;
  
  end: 
  return (ret);
  
  ERROR: 
  return (-1);
}


