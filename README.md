Report
--------

CHECKING FOR CYCLIC INHERITANCE
---------------------------------

First, I create a hashmap with key and value both of type string.
I traverse through the class list. While traversing, I add the current class name and its parent in the hashmap created with class name as key and parent name as value.
Then, we followed the below strategy for checking cycles:
We started iterating through the map. For each class, we again looped over the parents. If at some point, parent becomes current class then that means there is cyclic inheritance. While looping over parents, if we counter null (means there isn't any cyclic inheritance for that class), we then move on to the next class in the hashmap.
So, this cycle checking is O(n^2) job.

After checking cycles, we checked if there is a Main class and a main function in it and reported errors accordingly.

FIRST PASS
------------

Next, we stored all the methods of all the existing class table into a hashtable with key the class name and value list of  AST.method .

In this first pass we also add the methods of the predefined classes.

FILLSCOPE
----------

Now we call the funtion fillscope table with current class "Object". We maintain a global variable current class to indicate which class is being evaluated in the current pass. This function enters into scope checks the type of features ,adds them to the scope and then calls the function over the child classes. Since we are starting with "Object" and Object is the parent of all the classes , using recursion, we evaluate all the classes. Now while checking the type and adding into scope table of a particular class, we loop over all the features and  we checked if that function is either overriding or multiply defined etc.. depending on whether it is a method or attribute.

GETTING EXPRESSION TYPE
------------------------

While typechecking, we might need to evaluate the type of the expression. We did it using getexprtype function. Depending on what type of expression it is, we perform different checks and return the expression type.

For all the expression types, we referred to the cool manual for various type checks and performed those checks appropriately and we referred to the same for static type of the expressions.
In getexprtype,
In assign, we call the function recursively to get the type of the assignment expression.
In dispatch and static dispatch we check if the functions are defined in their caller and then we check if the formals are given correctly the type of this would be the return type.
In eq the 2 subexpressions should be of same type String or Int or Bool .
In leq, sub,mul,div,plus etc we made sure that both LHS and RHS are of types Int

In case expression, we return the least ancestor of all the expressions of cases.

This function will be called recursively if an expr is having subexpressions.
In each case we type casted the expr to the appropriate type
We also augmented the evaluated type  of the sub expressions .

We used different helper functions like isAncestor to check if a class is an ancestor of another class, checkargs to check if the formals are matching with the function call arguments. 

ERROR REPORTING
----------------
For error reporting we store the line number and the list of error messages at that line number in Treemap and at the end, we call a function that passes the required data from this data structure to reporterror.


