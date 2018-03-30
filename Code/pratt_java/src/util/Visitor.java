package util;

import parser.declarations.Declaration;
import parser.declarations.FunctionDeclaration;
import parser.declarations.VariableDeclaration;
import parser.expressions.*;
import parser.statements.*;

import java.util.List;

public interface Visitor {

    // Expressions
    void visit(Expression e);

    void visit(BooleanExpression e);

    void visit(CallExpression e);

    void visit(CharacterExpression e);

    void visit(IdentifierExpression e);

    void visit(IntegerExpression e);

    void visit(ListExpression e);

    void visit(OperatorExpression e);

    void visit(PostfixExpression e);

    void visit(PrefixExpression e);

    void visit(TupleExpression e);

    // Statements
    void visit(Statement s);

    void visit(List<Statement> ss);

    void visit(AssignStatement s);

    void visit(CallStatement s);

    void visit(ConditionalStatement s);

    void visit(LoopStatement s);

    void visit(PrintStatement s);

    void visit(ReturnStatement s);

    // Declaration
    void visit(Declaration d);

    void visit(FunctionDeclaration d);

    void visit(VariableDeclaration d);

}