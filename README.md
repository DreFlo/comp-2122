# Group

|Name|Number|Grade|Contribution|
|-|-|-|-|
|André de Jesus Fernandes Flores|201907001|18.0|33.(3)%|
|Diogo Luís Araujo de Faria|201907014|18.0|33.(3)%|
|Tiago Andre Batista Rodrigues|201906807|18.0|33.(3)%|

__Project Grade__ - 18.0

# Summary

The program is capable of compiling Java-- code into a Java .class file. It features error reporting for semantic and syntatic errors in the code as well as errors in code generation (OLLIR and Jasmin).

# Semantic Analysis

Our tool supports the following semantic rules:

- Variable use and declaration
- Type verification for operands in an operation
- Array cannot be used in arithmetic operations
- Array access is done over an array
- Array access index is an expression of type integer
- Type of the assignee must be compatible with the assigned
- Expressions in conditions must return a boolean
- When calling methods of the class declared in the code, verify if the types of arguments of the call are compatible with the types in the method declaration
- In case the method does not exist, verify if the class extends another class and report an error if it does not. Assume the method exists in one of the super classes, and that is being correctly called 
- When calling methods that belong to other classes other than the class declared in the code, verify if the classes are being imported

# Code generation

## Symbol Table

The symbol table is implemented in the class __SymbolTableBuilder__ (which extends __SymbolTable__) and is filled by an AST visitor of type __SymbolTableFiller__. All required functionality is implemented. Overloading is not supported.

## OLLIR

The Ollir Code is generated from the previously created AST and symbol table, utilizing two visitors, one to help with transforming complex expressions in Java-- into several lines of Ollir code, and another which iterates through the complete AST, handling the rest, including statements, methods and classes, and calling upon the other visitor when necessary.

## Jasmin

Jasmin code is generated from a OllirResult object. It supports all functionality mentioned in the project specification. It also supports some optimizations like instruction selection for different number (always choosing the smaller instruction that supports the number), branch conditionals with comparisons to zero, incrementing a variable, and loads and stores in registers up to three.
 
# Pros

- Good error reporting
- Optimizations in Jasmin code generation

# Cons

- Only implements optimization in Jasmin instruction selection
- Does not support overloading of methods
