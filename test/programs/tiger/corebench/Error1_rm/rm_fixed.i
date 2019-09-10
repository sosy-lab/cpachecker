# 1 "src/rm_error.c"
# 1 "<built-in>"
# 1 "<command-line>"
# 31 "<command-line>"
# 1 "/usr/include/stdc-predef.h" 1 3 4
# 32 "<command-line>" 2
# 1 "src/rm_error.c"

int
main_error (int argc, char **argv)
{
  bool preserve_root = true;
  struct rm_options x;
  bool prompt_once = false;
  int c;

  initialize_main (&argc, &argv);
  set_program_name (argv[0]);
  setlocale (LC_ALL, "");
  bindtextdomain (PACKAGE, LOCALEDIR);
  textdomain (PACKAGE);

  atexit (close_stdin);

  rm_option_init (&x);


  priv_set_remove_linkdir ();

  while ((c = getopt_long (argc, argv, "dfirvIR", long_opts, NULL)) != -1)
    {
      switch (c)
        {
        case 'd':
          x.remove_empty_directories = true;
          break;

        case 'f':
          x.interactive = RMI_NEVER;
          x.ignore_missing_files = true;
          prompt_once = false;
          break;

        case 'i':
          x.interactive = RMI_ALWAYS;
          x.ignore_missing_files = false;
          prompt_once = false;
          break;

        case 'I':
          G1_fixed:x.interactive = RMI_SOMETIMES;
          x.ignore_missing_files = false;
          prompt_once = true;
          break;

        case 'r':
        case 'R':
          x.recursive = true;
          break;

        case INTERACTIVE_OPTION:
          {
            int i;
            if (optarg)
              i = XARGMATCH ("--interactive", optarg, interactive_args,
                             interactive_types);
            else
              i = interactive_always;
            switch (i)
              {
              case interactive_never:
                x.interactive = RMI_NEVER;
                prompt_once = false;
                break;

              case interactive_once:
                x.interactive = RMI_SOMETIMES;
                x.ignore_missing_files = false;
                prompt_once = true;
                break;

              case interactive_always:
                x.interactive = RMI_ALWAYS;
                x.ignore_missing_files = false;
                prompt_once = false;
                break;
              }
            break;
          }

        case ONE_FILE_SYSTEM:
          x.one_file_system = true;
          break;

        case NO_PRESERVE_ROOT:
          preserve_root = false;
          break;

        case PRESERVE_ROOT:
          preserve_root = true;
          break;

        case PRESUME_INPUT_TTY_OPTION:
          x.stdin_tty = true;
          break;

        case 'v':
          x.verbose = true;
          break;

        case_GETOPT_HELP_CHAR;
        case_GETOPT_VERSION_CHAR (PROGRAM_NAME, AUTHORS);
        default:
          diagnose_leading_hyphen (argc, argv);
          usage (EXIT_FAILURE);
        }
    }

  if (argc <= optind)
    {
      if (x.ignore_missing_files)
        exit (EXIT_SUCCESS);
      else
        {
          error (0, 0, _("missing operand"));
          usage (EXIT_FAILURE);
        }
    }

  if (x.recursive && preserve_root)
    {
      static struct dev_ino dev_ino_buf;
      x.root_dev_ino = get_root_dev_ino (&dev_ino_buf);
      if (x.root_dev_ino == NULL)
        error (EXIT_FAILURE, errno, _("failed to get attributes of %s"),
               quote ("/"));
    }

  size_t n_files = argc - optind;
  char **file = argv + optind;

  if (prompt_once && (x.recursive || 3 < n_files))
    {
      fprintf (stderr,
               (x.recursive
                ? ngettext ("%s: remove %zu argument recursively? ",
                            "%s: remove %zu arguments recursively? ",
                            select_plural (n_files))
                : ngettext ("%s: remove %zu argument? ",
                            "%s: remove %zu arguments? ",
                            select_plural (n_files))),
               program_name, n_files);
      if (!yesno ())
        exit (EXIT_SUCCESS);
    }

  enum RM_status status = rm (file, &x);
  assert (VALID_STATUS (status));
  exit (status == RM_ERROR ? EXIT_FAILURE : EXIT_SUCCESS);
/bin/bash: wq: command not found
