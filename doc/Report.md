CPAchecker Analysis Report
--------------------------

Quick Reference to use the report
---------------------------------

  - Left Panel: Error Path (if a bug is found)

     - Indentation reflects height of call stack.
     - Function Start, Function Return, and unlabeled edges are not displayed.
     - Click table element to jump to its location in the `CFA`/`ARG`/`Source` (depending on the active tab).
        If none of the mentioned tabs is selected, `ARG` will become active.
     - Use Start (or click table element) and Prev/Next to walk along the error path.
     

  - Right Panel:

    - CFA Tab

      - The CFA is divided into multiple graphs (one graph per function).
         Initially all graphs are displayed below one another beginning with the program entry function, i.e. `main()`.
         Change the displayed function by using the **Displayed CFA** select box or
         by double clicking on a *function call node* (square element in graph).
      - Linear sequences of "normal" edges (StatementEdges, DeclarationEdges, and BlankEdges)
         are displayed as a multi-label node. Each line contains the predecessor node and the edge label. 
         The successor can be found in the left of the next line.
      - The error path is highlighted in red.
      - Hover over a graph element to display additional information.
      - Double-click an edge to jump to the location in the source code.

    - ARG Tab

      - The error path is highlighted in red.
      - Use the **Displayed ARG** select box to alter between the *complete* ARG and 
        an ARG graph containing only the *error path*.
      - Hover over a graph element to display additional information.
      - Double-click a node to jump to the location in the CFA.

    - Source Tab

      - Displays source code.

    - Log Tab

      - Displays the Log output of CPAchecker.
      
    - Statistics Tab

      - Displays the Statistics output of CPAchecker.

    - Configurations Tab

      - Displays the Configurations used by CPAchecker.

    - Help button

      - Click to display additional information for the right panel.


Known Problems
--------------

  - When a specific CFA function is displayed and an error path element, that is not contained in said function, is selected, the displayed function does not change.
     Workaround: Select `all` in the **Displayed CFA** select box and click the error path element again.

  - When an item is selected in the error path window on the left, only the active tab on the right is updated.
     Workaround: After changing the tab on the right, click the selected item in the error path window again.
