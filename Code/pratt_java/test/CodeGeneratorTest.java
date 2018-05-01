import static org.junit.Assert.*;

import java.io.BufferedReader;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.List;

import codeGeneration.CodeGenerator;
import codeGeneration.Command;
import compiler.CompileException;
import lexer.Lexer;
import org.junit.Test;

import parser.Parser;
import parser.declarations.Declaration;
import typechecker.Typechecker;
import util.Node;
import util.ReadSPL;

public class CodeGeneratorTest {

    private String runSSM(boolean debug) {
        try {
            List<String> command = new ArrayList<>();
            command.add("java");
            command.add("-jar");
            command.add("ssm.jar");
            command.add("--cli");
            command.add("--file");
            command.add("test.ssm");
            ProcessBuilder builder = new ProcessBuilder(command);
            final Process process = builder.start();
            InputStream is = process.getInputStream();
            InputStreamReader isr = new InputStreamReader(is);
            BufferedReader br = new BufferedReader(isr);

            String line;
            String result;
            result = line = br.readLine();
            if (debug) {
                do {
                    System.out.println(line);
                } while ((line = br.readLine()) != null);
            }
            return result;

        } catch (IOException e) {
            return e.getMessage();
        }
    }

    private String runSPL(String program, Command postamble, boolean debug){
        Lexer l = new Lexer(program);
        Parser p = new Parser(l.tokenize());
        List<Declaration> nodes = p.parseSPL();
        Typechecker tc = new Typechecker();
        tc.typecheck(nodes);

        CodeGenerator gen = new CodeGenerator("test.ssm");
        try {
            gen.generateCode(nodes, postamble);
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        }
        return runSSM(debug);
    }

    private String runExpression(String program, Command postamble, boolean debug){
        Lexer l = new Lexer(program);
        Parser p = new Parser(l.tokenize());
        Node n = p.parseExpression();
        Typechecker tc = new Typechecker();
        tc.typecheck(n);

        CodeGenerator gen = new CodeGenerator("test.ssm");
        try {
            gen.generateCode(n, postamble);
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        }
        return runSSM(debug);
    }

    @Test
    public void testIntegerConstant(){
        String result = runExpression("42", new Command("trap", "0"), false);
        assertEquals("42", result);
    }

    @Test
    public void testBoolean(){
        String result = runExpression("True",new Command("trap", "0"),false);
        assertEquals("-1", result);

        result = runExpression("False",new Command("trap", "0"),false);
        assertEquals("0", result);

        result = runExpression("True != False", new Command("trap", "0"),false);
        //-1 represents True;
        assertEquals("-1", result);
    }

    @Test
    public void testCharacterConstant(){
        String result = runExpression("'a'",new Command("trap", "0"), false);
        assertEquals("a", result);
    }

    @Test
    public void testPrefix(){
        String result = runExpression("--1",new Command("trap", "0"), false);
        assertEquals("1", result);

        result = runExpression("!True",new Command("trap", "0"), false);
        assertEquals("0", result);

        result = runExpression("!False",new Command("trap", "0"), false);
        assertEquals("-1", result);
    }

    @Test
    public void testAddition(){
        String result = runExpression("4 + 2",new Command("trap", "0"), false);
        assertEquals("6", result);
    }

    @Test
    public void testAdditionVsMultiplicationPrecedence(){
        String result = runExpression("4 + 2 * 3 + 2",new Command("trap", "0"), false);
        assertEquals("12", result);
    }

    @Test
    public void testSubtraction(){
        String result = runExpression("42-45", new Command("trap", "0"),false);
        assertEquals("-3", result);
    }

    @Test
    public void testSubtractionAssociativity(){
        String result = runExpression("6 - 3 - 2", new Command("trap", "0"),false);
        assertEquals("1", result);
        result = runExpression("6 - (3 - 2)", new Command("trap", "0"),false);
        assertEquals("5", result);
        result = runExpression("(6 - 3) - 2",new Command("trap", "0"), false);
        assertEquals("1", result);
    }

    @Test
    public void testAllBinaryOps(){
        String result = runExpression("42-45",new Command("trap", "0"), false);
        assertEquals("-3", result);

        result = runExpression("7+3",new Command("trap", "0"), false);
        assertEquals("10", result);

        result = runExpression("7*3",new Command("trap", "0"), false);
        assertEquals("21", result);

        result = runExpression("6/3", new Command("trap", "0"), false);
        assertEquals("2", result);

        result = runExpression("5%3", new Command("trap", "0"), false);
        assertEquals("2", result);

        result = runExpression("5 > 3", new Command("trap", "0"), false);
        assertEquals("-1", result);

        result = runExpression("5 < 3", new Command("trap", "0"), false);
        assertEquals("0", result);

        result = runExpression("5 >= 5", new Command("trap", "0"), false);
        assertEquals("-1", result);

        result = runExpression("5 >= 6", new Command("trap", "0"), false);
        assertEquals("0", result);

        result = runExpression("5 <= 5", new Command("trap", "0"), false);
        assertEquals("-1", result);

        result = runExpression("5 <= 6", new Command("trap", "0"), false);
        assertEquals("-1", result);

        result = runExpression("6 <= 5", new Command("trap", "0"), false);
        assertEquals("0", result);

        result = runExpression("1 == 1", new Command("trap", "0"), false);
        assertEquals("-1", result);

        result = runExpression("1 == 1 && 1 != 0", new Command("trap", "0"), false);
        assertEquals("-1", result);
    }

    @Test
    public void testReadInteger(){
        String result = runExpression("read(0)", new Command("trap", "0"), false);
        assertEquals("Please enter an integer: ", result);
    }

    @Test
    public void testReadChar(){
        String result = runExpression("read(1)", new Command("trap", "1"), false);
        assertEquals("Please enter a character: ", result);
    }

    @Test
    public void testMultipleFunWithArguments(){
        String result = runSPL("main()::->Void{\n" +
                "Int a = 3+ 2;\n" +
                "Int b = 5+ 3;\n" +
                "Int c = b;\n" +
                "c = multBy2(c);\n" +
                //"return;" + Fix later
                "}"+
                "multBy2( n ) :: Int -> Int {\n" +
                "Int d = 9;\n" +
                "d = 2;"+ //+ Try this later
                "return n * 2;\n" +
                "}", null,false);
        assertEquals("8", result);
    }

    @Test
    public void testFunCallNoAssign(){
        String result = runSPL("multBy3( n, m ) :: Int -> Int {\n" +
                "Int f = 9;\n" +
                "Int g = 3;" +
                "g = 2;"+ //+ Try this later
                "return n * 3;\n" +
                "}"+
                "multBy2( n ) :: Int -> Int {\n" +
                "Int d = 9;\n" +
                "Int e = 3;" +
                "d = 2;"+ //+ Try this later
                "return multBy3(n * 2, 1);\n" +
                "}"+
                "main()::->Void{\n" +
                "Int a = 3+ 2;\n" +
                "Int b = 5+ 3;\n" +
                "Int c = b;\n" +
                "multBy2(c);\n" +
                //"return;" + Fix later
                "}", null,false);
        assertEquals("8", result);
    }

    @Test
    public void testWhileLoop_conditionTrue(){
        //TODO: This one loops (see loopStatement TODO)
        String result = runSPL("main()::->Void{\n" +
                "Bool a = True; " +
                "while(a){" +
                "    a = False;" +
                "}" +
                "print(a);" +
                "}", null,false);
        assertEquals("0", result);
    }

    @Test
    public void testWhileLoop_conditionFalse(){
        String result = runSPL("main()::->Void{\n" +
                "Bool a = True; " +
                "while(!a){" +
                "    a = False;" +
                "}" +
                "print(a);" +
                "}", null,false);
        assertEquals("-1", result);
    }

    @Test
    public void testSingleFunLocalVarDecl(){
        String result = runSPL("main()::->Void{\n" +
                "Int a = 3+ 2;\n" +
                "Int b = 5+ 3;\n" +
                "Int c = b;\n" +
                "print(a);\n" +
                "}", null,false);
        assertEquals("5", result);
    }

    @Test
    public void FactorialImperative(){
        String result = runSPL("main()::->Void{\n" +
                "Int a = 3+ 2;\n" +
                "Int b = 5+ 3;\n" +
                "Int c = b;\n" +
                "print(a);\n" +
                "}", null,false);
        assertEquals("5", result);
    }

    @Test
    public void FactorialRecursive(){
        String program = ReadSPL.readLineByLineJava8("./test/splExamples/factorial_recursive.spl");

        String result = runSPL(program, null,false);
        assertEquals("120", result);
    }

}