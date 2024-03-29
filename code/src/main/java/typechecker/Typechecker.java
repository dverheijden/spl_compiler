package typechecker;

import codeGeneration.CompileException;
import lexer.TokenType;
import parser.declarations.Declaration;
import parser.declarations.FunctionDeclaration;
import parser.declarations.VariableDeclaration;
import parser.expressions.*;
import parser.statements.*;
import parser.types.*;
import util.Node;
import util.Visitor;

import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;


public class Typechecker implements Visitor {

    // These are for convenience.
    private final Type emptyListType = Types.emptyListType;

    private Environment env;
    private HashMap<String, List<Type>> functionSignatures;

    private List<TypeError> errors;

    public Typechecker() {
        this.functionSignatures = new HashMap<>();
        this.errors = new LinkedList<>();
        this.env = new Environment();
    }

    public boolean typecheck(Node ast) {
        ast.accept(this);
        return errors.isEmpty();
    }

    public boolean typecheck(List<? extends Node> nodes) {
        boolean correct = true;
        for (Node n : nodes) {
            if (!typecheck(n)) {
                correct = false;
            }
        }
        printErrors();
        return correct;
    }

    private void error(String errorMessage, Node n) {
        errors.add(new TypeError(String.format("%s \n\tError occurred in:\n%s", errorMessage, n)));
    }

    private void printErrors() {
        for (TypeError e : errors) {
            System.err.println(e.getErrorMessage());
        }
    }

    public String getAllErrors() {
        StringBuilder result = new StringBuilder();
        for (TypeError e : errors) {
            result.append(e.getErrorMessage());
            result.append("\n");
        }
        return result.toString();
    }

    @Override
    public void visit(Expression e) {
        Expression.visitExpression(this, e);
    }

    @Override
    public void visit(BooleanExpression e) {
        e.setType(Types.boolType);
    }

    @Override
    public void visit(CallExpression e) {
        for (Expression exp : e.args)
            this.visit(exp);
        List<Type> funArgs = functionSignatures.get(e.function_name.name);
        if (funArgs == null)
            error(String.format("Function %s was not defined.", e.function_name.name), e);
        else {
            if (funArgs.size() != e.args.size()) {
                error(String.format("Number of arguments in function call do not match. \n\tExpected: %s\n\tActual: %s",
                        functionSignatures.get(e.function_name.name).size(), e.args.size()), e);
            } else {
                for (int i = 0; i < funArgs.size(); i++) {
                    if (!funArgs.get(i).equals(e.args.get(i).getType()) &&
                            !(funArgs.get(i) instanceof ListType && e.args.get(i).getType() instanceof ListType)) {
                        error(String.format("Incompatible types in function call in argument %s\n\tExpected type: %s\n\tActual type: %s",
                                i + 1, funArgs.get(i), e.args.get(i).getType()), e);
                    }
                }
            }
        }
        if (env.getFunction(e.function_name.name) == null)
            error(String.format("The function %s was not defined",
                    e.function_name.name), e);
        else
            e.setType(env.getFunction(e.function_name.name).type);
    }

    @Override
    public void visit(CharacterExpression e) {
        e.setType(Types.charType);
    }

    @Override
    public void visit(IdentifierExpression e) {
        EnvironmentType idType = env.get(e.name);
        if (idType == null)
            error(String.format("Variable %s out of scope or undefined.", e.name), e);
        else {
            if (idType.isVarType) {
                e.setType(((VarType) env.get(e.name).type).type);
            } else
                e.setType(env.get(e.name).type);
        }
    }

    @Override
    public void visit(IntegerExpression e) {
        e.setType(Types.intType);
    }

    @Override
    public void visit(isEmptyExpression e) {
        this.visit(e.arg);
        if (!(e.arg.getType() instanceof ListType))
            error(String.format("isEmpty function needs argument of type List not %s", e.arg.getType()), e);
        e.setType(Types.boolType);

    }

    @Override
    public void visit(ListExpression e) {
        // A list expression starts out as an empty list, so initially its nothing
        e.setType(Types.listType(emptyListType));
    }

    @Override
    public void visit(OperatorExpression e) {
        /*
         * + : char, int
         * - : char, int
         * * : int
         * / : int
         * % : int
         * ==: int, char, bool
         * comperators
         */
        e.left.accept(this);
        e.right.accept(this);

        if (e.left.getType() instanceof IntType) {

            switch (e.operator) {
                case TOK_PLUS:
                case TOK_MINUS:
                case TOK_MULT:
                case TOK_DIV:
                case TOK_MOD:
                    if (!e.left.getType().equals(e.right.getType())) {
                        error("Left and right side of an expression must have the same Type.", e);
                    } else
                        e.setType(Types.intType);
                    break;

                case TOK_LT:
                case TOK_GT:
                case TOK_GEQ:
                case TOK_EQ:
                case TOK_LEQ:
                case TOK_NEQ:
                    if (e.left.getType() != e.right.getType()) {
                        error("Left and right side of and expression must have the same Type.", e);
                    } else
                        e.setType(Types.boolType);
                    break;
                case TOK_CONS:
                    consTypecheckAux(e);
                    break;
                default:
                    error(String.format("Invalid operator %s for Type Int and Type %s", e.operator.getValue(), e.right.getType()), e);
                    break;
            }

        } else if (e.left.getType() instanceof CharType) {
            switch (e.operator) {
                case TOK_PLUS:
                case TOK_MINUS:
                    if (e.left.getType() != e.right.getType()) {
                        error("Left and right side of an expression must have the same Type.", e);
                    } else
                        e.setType(Types.charType);
                    break;

                case TOK_LT:
                case TOK_GT:
                case TOK_GEQ:
                case TOK_EQ:
                case TOK_LEQ:
                case TOK_NEQ:
                    if (e.left.getType() != e.right.getType()) {
                        error("Left and right side of an expression must have the same Type.", e);
                    } else
                        e.setType(Types.boolType);
                    break;
                case TOK_CONS:
                    consTypecheckAux(e);
                    break;
                default:
                    error(String.format("Invalid operator %s for Type Char and Type %s", e.operator.getValue(), e.right.getType()), e);
                    break;
            }
        } else if (e.left.getType() instanceof BoolType) {
            switch (e.operator) {
                case TOK_NEQ:
                case TOK_EQ:
                case TOK_AND:
                case TOK_OR:
                    if (e.left.getType() != e.right.getType()) {
                        error("Left and right side of an expression must have the same Type.", e);
                    } else
                        e.setType(Types.boolType);
                    break;
                case TOK_CONS:
                    consTypecheckAux(e);
                    break;

                default:
                    error(String.format("Invalid operator %s for Type Bool and Type %s", e.operator.getValue(), e.right.getType()), e);
                    break;
            }
        } else if (e.left.getType() instanceof ListType) {
            switch (e.operator) {
                case TOK_NEQ:
                case TOK_EQ:
                    if (!(e.right.getType() instanceof ListType)) {
                        error("Left and right side of an expression must have the same Type.", e);
                    } else
                        e.setType(Types.boolType);
                    break;
                case TOK_CONS:
                    consTypecheckAux(e);
                    break;
                default:
                    error(String.format("Invalid operator %s for ListType %s and Type %s", e.operator.getValue(), e.left.getType(), e.right.getType()), e);
                    break;
            }
        } else if (e.left.getType() instanceof TupleType) {
            switch (e.operator) {
                case TOK_CONS:
                    consTypecheckAux(e);
                    break;

                default:
                    error(String.format("Invalid operator %s for TupleType %s and Type %s", e.operator.getValue(), e.left.getType(), e.right.getType()), e);
                    break;
            }
        } else {
            error(String.format("Type %s is not defined for TypeChecking in expressions.", e.left.getType()), e);
        }
    }

    @Override
    public void visit(PostfixExpression e) {
        this.visit(e.left);
        if (e.left.getType() instanceof ListType) {
            ListType t = (ListType) e.left.getType();
            switch (e.operator) {
                case TOK_HD:
                    e.setType(t.listType);
                    break;
                case TOK_TL:
                    e.setType(t);
                    break;
                default:
                    error(String.format("Operator %s is undefined for type %s", e.operator.getValue(), t), e);
            }
        } else if (e.left.getType() instanceof TupleType) {
            TupleType t = (TupleType) e.left.getType();
            switch (e.operator) {
                case TOK_FST:
                    e.setType(t.left);
                    break;
                case TOK_SND:
                    e.setType(t.right);
                    break;
                default:
                    error(String.format("Operator %s is undefined for type %s", e.operator.getValue(), t), e);
            }
        } else {
            error(String.format("Operator %s is undefined for Type %s", e.operator.getValue(), e.left.getType()), e);
        }
    }

    @Override
    public void visit(PrefixExpression e) {
        this.visit(e.right);
        if (e.operator == TokenType.TOK_NOT) {
            if (e.right.getType() == Types.boolType) {
                e.setType(Types.boolType);
            } else {
                error("You can only negate boolean expressions", e);
            }
        } else if (e.operator == TokenType.TOK_MINUS) {
            if (e.right.getType() == Types.intType) {
                e.setType(Types.intType);
            } else {
                error("The minus is only allowed for integer expressions", e);
            }
        } else {
            error(String.format("Unsupported prefix operator '%s' for type '%s'", e.operator.getValue(), e.right.getType()), e);
        }
    }

    @Override
    public void visit(ReadExpression e) {
        this.visit(e.arg);
        if (e.arg.getType() != Types.intType) {
            error(String.format("Invalid argument type for function 'read'.\n\tExpected Type: %s\n\tActual Type: %s",
                    Types.intType, e.arg.getType()), e);
        }
        if (e.arg.name == 0) {
            e.setType(Types.intType);
        } else if (e.arg.name == 1) {
            e.setType(Types.charType);
        } else {
            error(String.format("Invalid argument for 'read'.\n\tExpected: {0, 1}\n\tActual: %s", e.arg.name), e);
        }
    }

    @Override
    public void visit(TupleExpression e) {
        this.visit(e.left);
        this.visit(e.right);
        if ((e.left.getType() == Types.voidType) || (e.right.getType() == Types.voidType)) {
            error("Tuples cannot have listType Void.", e);
        }
        e.setType(Types.tupleType(e.left.getType(), e.right.getType()));
    }

    @Override
    public void visit(Statement s) {
        Statement.visitStatement(this, s);
    }

    public Type visit(List<Statement> statementBlock) {
        Type blockType = Types.voidType;
        for (Statement s : statementBlock) {
            this.visit(s);
            if (s instanceof ReturnStatement) {
                if (blockType != Types.voidType) { // i.e. you already saw a return statement
                    throw new CompileException("Having two return statements is not allowed.", s);
                }
                ReturnStatement ret = (ReturnStatement) s;
                blockType = ret.arg.getType();
            }
        }
        return blockType;
    }

    @Override
    public void visit(AssignStatement s) {
        this.visit(s.right);
        if (s.name instanceof IdentifierExpression || s.name instanceof PostfixExpression) {
            Type variableType = null;
            IdentifierExpression id = null;
            if(s.name instanceof IdentifierExpression){
                id = (IdentifierExpression) s.name;
                EnvironmentType envT = env.get(id.name);
                if(envT == null)
                    variableType = null;
                else
                variableType = env.get(id.name).type;
            }
            if(s.name instanceof PostfixExpression){
                this.visit(s.name);
                variableType = s.name.getType();
            }


            if (variableType == null) {
                if(s.name instanceof IdentifierExpression){
                    error(String.format("Variable %s is not defined", id.name), s);
                }
                else
                    error(String.format("Invalid nested .tl and .hd, found null as type."), s);
                s.setType(Types.voidType);
                return;
            }
            if (variableType instanceof TupleType) {
                if (s.right.getType() instanceof TupleType) {
                    if (isCompatible(((TupleType) variableType).left, ((TupleType) s.right.getType()).left, s.right) &&
                            isCompatible(((TupleType) variableType).right, ((TupleType) s.right.getType()).right, s.right)) {
                        //it's fine
                        s.setType(Types.voidType);
                        return;
                    }
                }
            } else if (variableType instanceof ListType) {
                if (isCompatible(variableType, s.right.getType(), s.right)) {
                    //it's fine
                    s.setType(Types.voidType);
                    return;
                }
                //if rhs is only made of empty lists it might still be compatible.
				/*else if(checkOnlyEmptyListType(d.right.checkType())){
						if(isCompatible(((ListType) d.varType).listType, d.right.checkType(), d.right))
							d.right.setType(d.varType);
				}*/
            } else if (variableType instanceof VarType) {
                s.name.setType(Types.varType(s.right.getType()));
                env.put(id.name, new EnvironmentType(s.name.getType(), env.get(id.name).isGlobal, env.get(id.name).isFunction, true));
                s.setType(Types.voidType);
                return;
            }

            if (!variableType.equals(s.right.getType()))
                if(s.name instanceof IdentifierExpression)
                    error(String.format("Type %s cannot be assigned to variable %s.\n\tExpected: %s \n\tActual: %s",
                        s.right.getType(), id.name, variableType, s.right.getType()), s);
                else
                    error(String.format("Type %s cannot be assigned to %s.\n\tExpected: %s \n\tActual: %s",
                            s.right.getType(), s.name.toString(), variableType, s.right.getType()), s);
            s.setType(Types.voidType);
        }


    }

    @Override
    public void visit(CallStatement s) {
        for (Expression exp : s.args)
            this.visit(exp);
        List<Type> funArgs = functionSignatures.get(s.function_name.name);
        if (funArgs == null)
            error("Function " + s.function_name.name + " was not defined.", s);
        else {
            if (funArgs.size() != s.args.size()) {
                error("Number of arguments in function call do not match.\nExpected: " +
                        functionSignatures.get(s.function_name.name).size() +
                        " and received: " + s.args.size(), s);
            } else {
                for (int i = 0; i < funArgs.size(); i++) {
                    if (!funArgs.get(i).equals(s.args.get(i).getType())) {
                        error("Incompatible types in function call.\n In argument " + (i + 1) + " expected type: " +
                                funArgs.get(i) +
                                " and received: " + s.args.get(i).getType(), s);
                    }
                }
            }
        }
        s.setType(env.getFunction(s.function_name.name).type);
    }

    @Override
    public void visit(ConditionalStatement conditionalStatement) {
        this.visit(conditionalStatement.condition);
        if (conditionalStatement.condition.getType() != Types.boolType) {
            error(String.format("The condition should be of type Boolean, but it has type '%s' in condition %s",
                    conditionalStatement.condition.getType(), conditionalStatement.condition), conditionalStatement);
        }
        Type thenBranchType = this.visit(conditionalStatement.then_expression);
        conditionalStatement.setType(thenBranchType);

        if (conditionalStatement.else_expression.size() != 0) {
            Type elseBranchType = this.visit(conditionalStatement.else_expression);
            if (thenBranchType != elseBranchType) {
                error(String.format("The return statements of both conditional branches should be of the same type. \n" +
                        "\tActual: (then) %s, (else) %s", thenBranchType, elseBranchType), conditionalStatement);
            }
        }
    }

    @Override
    public void visit(LoopStatement s) {
        this.visit(s.condition);
        if (s.condition.getType() != Types.boolType) {
            error(String.format("The condition should be of type Boolean and is of type '%s' in condition %s",
                    s.condition.getType(), s.condition), s);
        }
        s.setType(this.visit(s.body));
    }

    @Override
    public void visit(PrintStatement s) {
        this.visit(s.arg);
        if (s.arg.getType() instanceof ListType) {
            // It works for Python, not yet for SSM
            error("Print statements cannot handle lists", s);
        }
        s.setType(s.arg.getType());
    }

    @Override
    public void visit(ReturnStatement s) {
        if (s.arg == null) {
            s.setType(Types.voidType);
        } else {
            this.visit(s.arg);
            s.setType(s.arg.getType());
        }
    }

    @Override
    public void visit(Declaration d) {
        Declaration.visitDeclaration(this, d);
    }

    private Type returnType(List<Statement> statements) {
        Type returnType = null;
        for (Statement s : statements) {
            if (s instanceof ReturnStatement) {
                ReturnStatement returnStmt = (ReturnStatement) s;
                returnType = returnStmt.getType();
            } else if (s instanceof ConditionalStatement) {
                ConditionalStatement condStmt = (ConditionalStatement) s;
                returnType = returnType(condStmt.then_expression);
            } else if (s instanceof LoopStatement) {
                LoopStatement loopStmt = (LoopStatement) s;
                returnType = returnType(loopStmt.body);
            }
        }
        return returnType;
    }

    @Override
    public void visit(FunctionDeclaration d) {
        //Backup original environment to fix let binding
        Environment backup = Environment.deepCopy(env);

        //set functiontype
        d.setType(d.funType.returnType);
        //Functions are always global
        if (env.getFunction(d.funName.name) != null) {
            error(String.format("The function %s is already defined", d.funName.name), d);
        } else {
            env.putFunction(d.funName.name, new EnvironmentType(d.funType.returnType, true, true, false));
            functionSignatures.put(d.funName.name, d.funType.argsTypes);
        }

        //check if arguments and argument types match
        if (d.args.size() != d.funType.argsTypes.size()) {
            if (d.args.size() < d.funType.argsTypes.size())
                error("There are more argument types than function arguments", d);
            else
                error("There are more function arguments than argument types", d);
        }

        //set argument types if there are any
        if (!d.args.isEmpty()) {
            for (int argsCount = 0; argsCount < d.args.size(); argsCount++) {
                IdentifierExpression id = d.args.get(argsCount);
                if (env.get(id.name) != null) {
                    if (!env.get(id.name).isGlobal)
                        error(String.format("The identifier %s is already in the list of parameters of this function", id.name), d);
                } else if (argsCount < d.funType.argsTypes.size())
                    //Arguments are treated as local variable, therefore not global
                    if (d.funType.argsTypes.get(argsCount) instanceof VarType) {
                        env.put(id.name, new EnvironmentType(d.funType.argsTypes.get(argsCount), false, false, true));
                    } else
                        env.put(id.name, new EnvironmentType(d.funType.argsTypes.get(argsCount), false, false, false));
                else
                    //TODO:check this
                    env.put(id.name, null);
            }
        }

        if (!d.decls.isEmpty()) {
            for (VariableDeclaration varDecl : d.decls) {
                this.visit(varDecl);
            }
        }

        this.visit(d.stats);
        Type returnType = returnType(d.stats);
        //Had to add equals method for IntType, it seems like the instance system is not working as it should.
        if (!d.funType.returnType.equals(returnType)) {
            if (returnType != null)
                error(String.format("The return type of the function is not equal to the actual return type. " +
                        "\n\tExpected: %s \n\tActual: %s", d.funType.returnType, returnType), d);
        }

        env = backup;
        //add function signature to environment, so other functions below it can still use it.
        env.putFunction(d.funName.name, new EnvironmentType(d.funType.returnType, true, true, false));
    }

    @Override
    public void visit(VariableDeclaration d) {

        this.visit(d.right);

        if (d.varType instanceof TupleType) {
            if (d.right.getType() instanceof TupleType) {
                if (isCompatible(((TupleType) d.varType).left, ((TupleType) d.right.getType()).left, d.right) &&
                        isCompatible(((TupleType) d.varType).right, ((TupleType) d.right.getType()).right, d.right)) {
                    //it's fine
                    d.right.setType(d.varType);
                }
            }
        } else if (d.varType instanceof ListType) {
            if (isCompatible(d.varType, d.right.getType(), d.right)) {
                //it's fine
                d.right.setType(d.varType);
            }
            //if rhs is only made of empty lists it might still be compatible.
				/*else if(checkOnlyEmptyListType(d.right.checkType())){
						if(isCompatible(((ListType) d.varType).listType, d.right.checkType(), d.right))
							d.right.setType(d.varType);
				}*/
        } else if (d.varType instanceof VarType) {
            d.varType = Types.varType(d.right.getType());

        }
        //}
        if (d.varType.equals(d.right.getType()) || d.varType instanceof VarType) {
            if (env.get(d.left.name) != null) {
                if ((env.get(d.left.name).isGlobal && d.isGlobal) || ((!env.get(d.left.name).isGlobal && !d.isGlobal)))
                    error(String.format("Variable %s is already defined!", d.left.name), d);
                else if (env.get(d.left.name).isGlobal && !d.isGlobal) {
                    if (d.varType instanceof VarType) {
                        env.put(d.left.name, new EnvironmentType(d.varType, false, false, true));
                    } else
                        env.put(d.left.name, new EnvironmentType(d.right.getType(), false, false, false));
                }
            } else {
                if (d.varType instanceof VarType) {
                    env.put(d.left.name, new EnvironmentType(d.varType, false, false, true));
                } else
                    env.put(d.left.name, new EnvironmentType(d.right.getType(), d.isGlobal, false, false));
            }
        } else
            error(String.format("\nVariable %s, of type \n%s cannot have an assignment of type: \n%s.",
                    d.left, d.varType, d.right.getType()), d);
        d.setType(Types.voidType);
    }

    private void consTypecheckAux(OperatorExpression e) {
        //Ex: 1:1
        if (!(e.right.getType() instanceof ListType)) {
            error("Right hand side of cons expression must have listType list", e);
            return;
        }

        ListType listTypeRight = (ListType) e.right.getType();

        // [Any]:[[Empty]] - > [[Any]]
        if (listTypeRight != null) {
            if (listTypeRight.listType instanceof ListType) {
                if (((ListType) listTypeRight.listType).listType == emptyListType) {
                    if (e.left.getType() instanceof ListType) {
                        //Probably not necessary
                        //if (((ListType) e.left.getType()).listType != emptyListType) {
                            e.setType(Types.listType(e.left.getType()));
                            return;
                        //}
                    }
                    error(String.format("LHS and RHS of cons expression are incompatible\n\tLHS: %s\n\tRHS: %s", e.left.getType(), e.right.getType()), e);
                }
            }
        }

        // [Empty]: Something
        if (e.left.getType() instanceof ListType) {
            if (((ListType) e.left.getType()).listType == emptyListType) {
                if (e.right.getType() instanceof ListType) {
                    if (((ListType) e.right.getType()).listType == emptyListType) {
                        // [Empty]: [Empty] -> [[Empty]]
                        e.setType(Types.listType(Types.listType(emptyListType)));
                        return;
                    } else if (((ListType) e.right.getType()).listType instanceof ListType) {
                        if (((ListType) ((ListType) e.right.getType()).listType).listType == emptyListType) {
                            //[Empty]:[[Empty]] -> [[Empty]]
                            e.setType(e.right.getType());
                            return;
                        } else {
                            error(String.format("LHS and RHS of cons expression are incompatible\n\tLHS: %s\n\tRHS: %s", e.left.getType(), e.right.getType()), e);
                        }
                    } else {
                        error(String.format("LHS and RHS of cons expression are incompatible\n\tLHS: %s\n\tRHS: %s", e.left.getType(), e.right.getType()), e);
                    }
                } else {
                    error(String.format("LHS and RHS of cons expression are incompatible\n\tLHS: %s\n\tRHS: %s", e.left.getType(), e.right.getType()), e);
                }
            }
        }

        // Any : [] -> [Any]
        if (listTypeRight.listType == emptyListType) {
            e.setType(new ListType(e.left.getType()));
            return;
        }
        //If rhs has list type of left.type, it's fine
        else if (((ListType) e.right.getType()).listType.equals(e.left.getType())) {
            e.setType(e.right.getType());
            return;
        }

        //If rhs has list type compatible left.type, it's fine
        else if (isCompatible((e.left.getType()), ((ListType) e.right.getType()).listType, e)) {
            Type infered = inferedTyped(e.left.getType(), ((ListType) e.right.getType()).listType, e);
            if (infered != null)
                e.setType(Types.listType(infered));
            else
                error(String.format("LHS and RHS of cons expression are incompatible\n\tLHS: %s\n\tRHS: %s", e.left.getType(), e.right.getType()), e);
            return;
        } else
            error(String.format("LHS and RHS of cons expression are incompatible\n\tLHS: %s\n\tRHS: %s", e.left.getType(), e.right.getType()), e);
    }

    private boolean checkEmptyListTypeNull(Type e) {
        if (e instanceof TupleType) {
            Type left = ((TupleType) e).left;
            Type right = ((TupleType) e).right;
            return (checkEmptyListTypeNull(left) || checkEmptyListTypeNull(right));
        } else if (e instanceof ListType) {
            Type listType = ((ListType) e).listType;
            return listType == emptyListType || checkEmptyListTypeNull(listType);
        } else return e instanceof EmptyListType;
    }


    private boolean isCompatible(Type left, Type right, Expression e) {
        if (checkEmptyListTypeNull(left) || checkEmptyListTypeNull(right)) {

            if (left instanceof ListType && right instanceof ListType) {
                return isCompatible(((ListType) left).listType, ((ListType) right).listType, e);

            } else {
                return isCompatibleCheck(left, right, e);
            }
        } else {
            return isCompatibleCheck2(left, right);
        }
    }

    private boolean isCompatibleCheck2(Type left, Type right) {
        if (left == null || right == null)
            return false;
        else return left.equals(right);
    }

    private boolean isCompatibleCheck(Type left, Type right, Expression e) {
        if (((left instanceof TupleType)) && ((right instanceof TupleType))) {

            return isNestedCompatible(((TupleType) left).left, ((TupleType) right).left, e) &&
                    isNestedCompatible(((TupleType) left).right, ((TupleType) right).right, e);

        } else if (right == emptyListType) {
            return true;

        } else
            return left == emptyListType;
    }

    private boolean isNestedCompatible(Type left, Type right, Expression e) {
        //tobeFixed = leftTYPE

        if (checkEmptyListTypeNull(left) || checkEmptyListTypeNull(right)) {

            if (left instanceof ListType && right instanceof ListType) {
                if (((ListType) left).listType == emptyListType) {
                    return true;
                }
                if (((ListType) left).listType instanceof ListType) {
                    if (((ListType) ((ListType) left).listType).listType == emptyListType) {
                        return true;
                    }
                }
                if (((ListType) right).listType == emptyListType) {
                    return true;
                }
                if (((ListType) right).listType instanceof ListType) {
                    if (((ListType) ((ListType) right).listType).listType == emptyListType) {
                        return true;
                    }
                }
                return isNestedCompatible(((ListType) left).listType, ((ListType) right).listType, e);

            } else return isCompatibleCheck(left, right, e);
        } else return isCompatibleCheck2(left, right);


    }


    private Type inferedTyped(Type left, Type right, Expression e) {
        //tobeFixed = leftTYPE

        if (checkEmptyListTypeNull(left) || checkEmptyListTypeNull(right)) {

            if (left instanceof ListType && right instanceof ListType) {
                return Types.listType(inferedTyped(((ListType) left).listType, ((ListType) right).listType, e));

            } else if (((left instanceof TupleType)) && ((right instanceof TupleType))) {
                return Types.tupleType(inferedNestedTyped(((TupleType) left).left, ((TupleType) right).left, e),
                        inferedNestedTyped(((TupleType) left).right, ((TupleType) right).right, e));
            }

            return null;

        } else return checkType(left, right);


    }

    private Type inferedNestedTyped(Type left, Type right, Expression e) {
        //tobeFixed = leftTYPE

        if (checkEmptyListTypeNull(left) || checkEmptyListTypeNull(right)) {

            if (left instanceof ListType && right instanceof ListType) {
                return Types.listType(inferedNestedTyped(((ListType) left).listType, ((ListType) right).listType, e));

            } else if (((left instanceof TupleType)) && ((right instanceof TupleType))) {

                return Types.tupleType(inferedNestedTyped(((TupleType) left).left, ((TupleType) right).left, e),
                        inferedNestedTyped(((TupleType) left).right, ((TupleType) right).right, e));

            } else if (right == emptyListType) {
                return left;

            } else if (left == emptyListType) {
                return right;

            } else if (right instanceof ListType) {
                if (((ListType) right).listType == emptyListType)
                    return left;

            } else if (left instanceof ListType) {
                if (((ListType) left).listType == emptyListType)
                    return right;

            }
            //error("Typechecker: invalid list types",e );
            return null;

        } else {
            return checkType(left, right);
        }
    }

    private Type checkType(Type left, Type right) {
        if (left == null || right == null)
            return null;
        else if (left.equals(right)) {
            return left;
        } else
            return null;
    }

    public Type getVariableType(String name) {
        return env.get(name).type;
    }

    public Environment getEnvironment() {
        return this.env;
    }

}


