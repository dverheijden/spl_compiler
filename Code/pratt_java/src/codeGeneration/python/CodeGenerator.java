package codeGeneration.python;

import codeGeneration.CompileException;
import parser.declarations.Declaration;
import parser.declarations.FunctionDeclaration;
import parser.declarations.VariableDeclaration;
import parser.expressions.*;
import parser.statements.*;
import util.Node;
import util.Visitor;

import java.io.FileNotFoundException;
import java.util.List;

public class CodeGenerator implements Visitor {

    private final ProgramWriter programWriter;

    public CodeGenerator(String filepath) {
        this.programWriter = new ProgramWriter(filepath, "\t");
    }

    public void generateCode(List<Declaration> nodes) throws FileNotFoundException {
        for(Node n : nodes){
            n.accept(this);
        }

        programWriter.writeToFile();
    }

    public void generateCode(Node n) throws FileNotFoundException {
        n.accept(this);

        programWriter.writeToFile();
    }

    @Override
    public void visit(Expression e) {
        Expression.visitExpression(this, e);
    }

    @Override
    public void visit(BooleanExpression e) {
        String line = e.name ? "True" : "False";
        programWriter.addToOutput(line, true);
    }

    @Override
    public void visit(CallExpression e) {
        this.visit(e.function_name);
        programWriter.addToOutput("(", false);
        for(int i = 0; i < e.args.size(); i ++){
            this.visit(e.args.get(i));
            if(i < e.args.size() - 1)
                programWriter.addToOutput(",", true);
        }
        programWriter.addToOutput(")", false);
    }

    @Override
    public void visit(CharacterExpression e) {
        programWriter.addToOutput(String.format("'%c'", e.name), true);
    }

    @Override
    public void visit(IdentifierExpression e) {
        programWriter.addToOutput(e.name, true);
    }

    @Override
    public void visit(IntegerExpression e) {
        programWriter.addToOutput(String.valueOf(e.name), true);
    }

    @Override
    public void visit(isEmptyExpression e) {
        this.visit(e.arg);
        // TODO: the rest
    }

    @Override
    public void visit(ListExpression e) {
        //TODO
    }

    @Override
    public void visit(OperatorExpression e) {
        programWriter.addToOutput("(", false);
        this.visit(e.left);
        switch (e.operator) {
            // arithmetic binary functions
            case TOK_PLUS:
                programWriter.addToOutput("+", true);
                break;
            case TOK_MULT:
                programWriter.addToOutput("*", true);
                break;
            case TOK_MINUS:
                programWriter.addToOutput("-", true);
                break;
            case TOK_MOD:
                programWriter.addToOutput("%", true);
                break;
            case TOK_DIV:
                programWriter.addToOutput("//", true);
                break;

            // Boolean
            case TOK_AND:
                programWriter.addToOutput("and", true);
                break;
            case TOK_OR:
                programWriter.addToOutput("or", true);
                break;

            // Comparison
            case TOK_EQ:
                programWriter.addToOutput("is", true);
                break;
            case TOK_NEQ:
                programWriter.addToOutput("is not", true);
                break;
            case TOK_LT:
                programWriter.addToOutput("<", true);
                break;
            case TOK_GT:
                programWriter.addToOutput(">", true);
                break;
            case TOK_LEQ:
                programWriter.addToOutput("<=", true);
                break;
            case TOK_GEQ:
                programWriter.addToOutput(">=", true);
                break;
            case TOK_CONS:
                // TODO: do this
                break;

            default:
                throw new CompileException(String.format("Invalid operator '%s'.", e.operator), e);
        }
        this.visit(e.right);
        programWriter.addToOutput(")", false);
    }

    @Override
    public void visit(PostfixExpression e) {
        this.visit(e.left);
        switch (e.operator) {
            case TOK_FST:
                //TODO
                break;

            case TOK_SND:
                //TODO
                break;

            case TOK_HD:
                //TODO
                break;

            case TOK_TL:
                //TODO
                break;
        }
    }

    @Override
    public void visit(PrefixExpression e) {
        switch (e.operator) {
            case TOK_MINUS:
                programWriter.addToOutput("-", false);
                break;

            case TOK_NOT:
                programWriter.addToOutput("not", true);
                break;
        }
        this.visit(e.right);
    }

    @Override
    public void visit(ReadExpression e) {
        String type = e.arg.name == 1 ? "a character" : "an integer";
        programWriter.addToOutput(String.format("read('Please enter %s:')", type), false, true);
    }

    @Override
    public void visit(TupleExpression e) {
        programWriter.addToOutput("(", false);
        this.visit(e.left);
        programWriter.addToOutput(",", true);
        this.visit(e.right);
        programWriter.addToOutput(")", false);
    }

    @Override
    public void visit(Statement s) {
        Statement.visitStatement(this, s);
    }

    @Override
    public void visit(AssignStatement s) {
        this.visit(s.name);
        programWriter.addToOutput("=", true);
        this.visit(s.right);
        programWriter.addToOutput("", false, true);
    }

    @Override
    public void visit(CallStatement s) {
        this.visit(s.function_name);
        programWriter.addToOutput("(", false);
        for(int i = 0; i < s.args.size(); i ++){
            this.visit(s.args.get(i));
            if(i < s.args.size() - 1)
                programWriter.addToOutput(",", true);
        }
        programWriter.addToOutput(")", false, true);
    }

    @Override
    public void visit(ConditionalStatement conditionalStatement) {
        programWriter.addToOutput("if", true);
        this.visit(conditionalStatement.condition);
        programWriter.addToOutput(":", false, true);
        programWriter.addIndent();
        for(Statement s : conditionalStatement.then_expression){
            visit(s);
        }
        programWriter.removeIndent();
        if(conditionalStatement.else_expression.size() > 0){
            programWriter.addToOutput("else:", false, true);
            programWriter.addIndent();
            for(Statement s : conditionalStatement.else_expression){
                visit(s);
            }
            programWriter.removeIndent();
        }
    }

    @Override
    public void visit(LoopStatement loopStatement) {
        programWriter.addToOutput("while", true, false);
        this.visit(loopStatement.condition);
        programWriter.addToOutput(":", false, true);
        programWriter.addIndent();
        for(Statement s: loopStatement.body){
            this.visit(s);
        }
        programWriter.removeIndent();
    }

    @Override
    public void visit(PrintStatement s) {
        programWriter.addToOutput("print(", false);
        this.visit(s.arg);
        programWriter.addToOutput(")", true, true);
    }

    @Override
    public void visit(ReturnStatement s) {
        //TODO
    }

    @Override
    public void visit(Declaration d) {
        Declaration.visitDeclaration(this, d);
    }

    @Override
    public void visit(FunctionDeclaration d) {
        //TODO
    }

    @Override
    public void visit(VariableDeclaration d) {
        //TODO
    }

}