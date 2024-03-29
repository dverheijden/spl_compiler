import static org.junit.Assert.*;

import lexer.Lexer;
import org.junit.Before;
import org.junit.Test;

import parser.declarations.VariableDeclaration;
import parser.exceptions.ParseException;
import parser.statements.Statement;
import parser.types.Type;
import parser.types.Types;
import parser.Parser;
import parser.declarations.Declaration;
import parser.expressions.Expression;
import typechecker.*;
import util.Node;
import util.ReadSPL;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Stream;

public class TypecheckerTest {
	private Typechecker tc = null;

    private final String rootFolder = "./src/test/resources/splExamples/";

	@Before
	public void setUp(){
		tc = new Typechecker();
	}

	private void assertTypecheckSuccess() {
        assertEquals(tc.getAllErrors(), 0, tc.getAllErrors().length());
	}

    private void assertTypecheckFailure() {
        System.err.println(tc.getAllErrors());
        assertNotEquals(tc.getAllErrors(), 0, tc.getAllErrors().length());
    }

    private Node typecheckStmt(String input) {
        Lexer l = new Lexer(input);
        Parser p = new Parser(l.tokenize());
        Statement stmt = p.parseStatement();
        tc.typecheck(stmt);
        return stmt;
    }

	private Node typecheckExpr(String input) {
		Lexer l = new Lexer(input);
		Parser p = new Parser(l.tokenize());
		Expression expr = p.parseExpression();
		tc.typecheck(expr);
		return expr;
	}

	private List<Node> typecheckSPL(String input) {
		Lexer l = new Lexer(input);
		Parser p = new Parser(l.tokenize());
        tc = new Typechecker();
		List<Declaration> decls = p.parseSPL();
		List<Node> nodes = new ArrayList<>();

		for(Declaration d : decls) {
			tc.typecheck(d);
			nodes.add(d);
		}
		return nodes;
	}

	@Test
	public void testCompareTypes() {
		assertEquals(Types.intType, Types.intType);
		assertEquals(Types.boolType, Types.boolType);
		assertNotEquals(Types.boolType, Types.intType);
		assertEquals(Types.listType(Types.intType), Types.listType(Types.intType));
		assertNotEquals(Types.listType(Types.intType), Types.tupleType(Types.intType, Types.intType));
	}

	@Test
	public void testIntegerConstant() {
		Node e = typecheckExpr("5");
		assertTypecheckSuccess();
		assertEquals(Types.intType, e.getType());
	}

    @Test
    public void testCharacterConstant() {
        Node e = typecheckExpr("'a'");
        assertTypecheckSuccess();
        assertEquals(Types.charType, e.getType());
    }

    @Test
	public void testBooleanConstantTrueAndFalse() {
		Node eTrue = typecheckExpr("True");
		Node eFalse = typecheckExpr("False");
		assertTypecheckSuccess();
		assertEquals(Types.boolType, eTrue.getType());
		assertEquals(Types.boolType, eFalse.getType());
	}

    @Test
    public void testTuple() {
        Node e = typecheckExpr("(True, 1)");
        assertTypecheckSuccess();
        assertEquals(Types.tupleType(Types.boolType, Types.intType), e.getType());
    }

    @Test
    public void testTuplePostfixFst() {
        Node e = typecheckExpr("(True, 1).fst");
        assertTypecheckSuccess();
        assertEquals(Types.boolType, e.getType());
    }

    @Test
    public void testTuplePostfixSnd() {
        Node e = typecheckExpr("(True, (2, 'a')).snd");
        assertTypecheckSuccess();
        assertEquals(Types.tupleType(Types.intType, Types.charType), e.getType());
    }

    @Test
    public void testTuplePostfixHd() {
        typecheckExpr("(True, (2, 'a')).hd");
        assertTypecheckFailure();
    }

    @Test
    public void testListHd() {
        Node e = typecheckExpr("((2, 'a') : []).hd");
        assertTypecheckSuccess();
        assertEquals(Types.tupleType(Types.intType, Types.charType), e.getType());
    }

    @Test
    public void testFunCallNoAssign(){
        typecheckSPL("Int multBy3 = 0;" +
                "Int myGlobal2 = 1;" +
                "multBy3( n ) :: Int -> Int {\n" +
                "Int f = 9;\n" +
                "Int g = 3;" +
                "g = 2;"+ //+ Try this later
                "myGlobal2 = 10;"+
                "return n * 3;\n" +
                "}"+
                "multBy2( n ) :: Int -> Int {\n" +
                "Int d = 9;\n" +
                "Int e = 3;" +
                "myGlobal2 = d;" +
                "myGlobal2 = e;"+
                "d = 2;"+ //+ Try this later
                "return multBy3(n * 2);\n" +
                "}"+
                "main()::->Void{\n" +
                "Int a = 3+ 2;\n" +
                "Int b = 5+ 3;\n" +
                "Int c = b;\n" +
                "c =multBy2(c);\n" +
                //RESULT IS 48 because in multBy2
                "print(c);\n" +
                //"return;" + Fix later
                "}");
        assertTypecheckSuccess();

    }

    @Test
    public void testListTl() {
        Node e = typecheckExpr("((2, 'a') : []).tl");
        assertTypecheckSuccess();
        assertEquals(Types.listType(Types.tupleType(Types.intType, Types.charType)), e.getType());
    }

	@Test
	public void testPlus() {
		Node e = typecheckExpr("5 + 3");
		assertTypecheckSuccess();
		assertEquals(Types.intType, e.getType());
	}

	@Test
	public void testLessThan() {
		Node e = typecheckExpr("5 < 3");
		assertTypecheckSuccess();
		assertEquals(Types.boolType, e.getType());
	}

	@Test
	public void testLessThanChar() {
		Node e = typecheckExpr("'5' < '3'");
		assertTypecheckSuccess();
		assertEquals(Types.boolType, e.getType());
	}

	@Test
	public void testConsIntEmpty() {
		Node e = typecheckExpr("5 : []");
		assertTypecheckSuccess();
		assertEquals(Types.listType(Types.intType), e.getType());
	}

	@Test
	public void testConsIntNotEmpty() {
		Node e = typecheckExpr("1:2:3:[]");
		assertTypecheckSuccess();
		assertEquals(Types.listType(Types.intType), e.getType());
	}

	@Test
	public void testConsCharNotEmpty() {
		Node e = typecheckExpr("'a':'b':'c':[]");
		assertTypecheckSuccess();
		assertEquals(Types.listType(Types.charType), e.getType());
	}

	@Test
	public void testConsBoolNotEmpty() {
		Node e = typecheckExpr("False:False:True:[]");
		assertTypecheckSuccess();
		assertEquals(Types.listType(Types.boolType), e.getType());
	}

	@Test
	public void testConsTupleNotEmpty() {
		Node e = typecheckExpr("(1,'a'):(2,'b'):(3,'c'):[]");
		assertTypecheckSuccess();
		assertEquals(Types.listType(Types.tupleType(Types.intType, Types.charType)), e.getType());
	}

    @Test
    public void testPrefixNegation() {
        Node e = typecheckExpr("!True");
        assertTypecheckSuccess();
        assertEquals(Types.boolType, e.getType());
    }

    @Test
    public void testPrefixNegationOverComparison() {
        Node e = typecheckExpr("!(1 > 2)");
        assertTypecheckSuccess();
        assertEquals(Types.boolType, e.getType());
    }

    @Test
    public void testPrefixNegationError() {
        typecheckExpr("!1");
        assertTypecheckFailure();
    }

    @Test
    public void testPrefixMinus() {
        Node e = typecheckExpr("-10");
        assertTypecheckSuccess();
        assertEquals(Types.intType, e.getType());
    }

    @Test
    public void testPrefixMinusGrouped() {
        Node e = typecheckExpr("-(4 * 3) % 5");
        assertTypecheckSuccess();
        assertEquals(Types.intType, e.getType());
    }

    @Test
    public void testReadChar() {
        Node e = typecheckExpr("read(1)");
        assertTypecheckSuccess();
        assertEquals(Types.charType, e.getType());
    }

    @Test
    public void testReadInteger() {
        Node e = typecheckExpr("read(0)");
        assertTypecheckSuccess();
        assertEquals(Types.intType, e.getType());
    }

    @Test
    public void testReadVar() {
        typecheckSPL("var a = read(0);\n");
        assertTypecheckSuccess();
        assertEquals(Types.varType(Types.intType), tc.getVariableType("a"));
    }

    @Test
    public void testVar() {
        typecheckSPL("var a = 3;\n");
        assertTypecheckSuccess();
        assertEquals(Types.varType(Types.intType), tc.getVariableType("a"));
    }

    @Test
    public void testVarInExpr() {
        typecheckSPL("var a = 3; Int b = a + 5; Bool c = a < b;");
        assertTypecheckSuccess();
        assertEquals(Types.varType(Types.intType), tc.getVariableType("a"));
    }

    @Test
    public void testVarInExprFaulty() {
        typecheckSPL("var a = False; Int b = a + 5; Char c = a == False;");
        assertTypecheckFailure();
        assertEquals(Types.varType(Types.boolType), tc.getVariableType("a"));
    }

	@Test
	public void testValidVarDecl() {
		List<Node> nodes = typecheckSPL("Int a = 3;\n" +
				"Bool a = True;\n" +
						"Char c = 'a';");
		for(Node n : nodes){
			assertEquals(Types.voidType, n.getType());
		}
	}



    @Test
    public void testEmptyReturn() {
        Node e = typecheckStmt("return;");
        assertTypecheckSuccess();
        assertEquals(Types.voidType, e.getType());
    }

    @Test
    public void testNonEmptyReturn() {
        Node e = typecheckStmt("return 1+3;");
        assertTypecheckSuccess();
        assertEquals(Types.intType, e.getType());
    }

    @Test
    public void testPrintInt() {
        Node e = typecheckStmt("print(1);");
        assertTypecheckSuccess();
        assertEquals(Types.intType, e.getType());
    }

    @Test
    public void testPrintList() {
        typecheckStmt("print(1:[]);");
        assertTypecheckFailure();
    }

    @Test
    public void testSimpleConditional() {
        Node e = typecheckStmt("if(True){}");
        assertTypecheckSuccess();
        assertEquals(Types.voidType, e.getType());
    }

    @Test
    public void testSimpleConditionalReturn() {
        Node e = typecheckStmt("if(True){return True;} else {return False;}");
        assertTypecheckSuccess();
        assertEquals(Types.boolType, e.getType());
    }

    @Test
    public void testConditionalReturn() {
        Node e = typecheckStmt("if(1 > 3 && True){return True == False;} else {return 1==2;}");
        assertTypecheckSuccess();
        assertEquals(Types.boolType, e.getType());
    }

    @Test
    public void testConditionalReturnMismatch() {
        typecheckStmt("if(1 > 3 && True){return 1;} else {return 1==2;}");
        assertTypecheckFailure();
    }

    @Test
    public void testWhile() {
        Node e = typecheckStmt("if(1 > 3){}");
        assertTypecheckSuccess();
        assertEquals(Types.voidType, e.getType());
    }

    @Test
    public void testWhileReturn() {
        Node e = typecheckStmt("if(1 > 3 && True){return 1:[];}");
        assertTypecheckSuccess();
        assertEquals(Types.listType(Types.intType), e.getType());
    }

    @Test
    public void testWhileInvalidCondition() {
        typecheckStmt("if(1){return 1:[];}");
        assertTypecheckFailure();
    }

    @Test
    public void testEmptyListCompatibility() {
        typecheckSPL("[[Int]] a = []:[];\n"+
                "main()::->Void{\n"+
                "[[Int]] a = []:[];\n"+
                "print(0);\n"+
                "}");
        assertTypecheckSuccess();
    }

	@Test
	public void testisEmptyExpr() {
		Node e = typecheckExpr("isEmpty(1:2:[])");
		assertTypecheckSuccess();
		assertEquals(Types.boolType, e.getType());
	}

	@Test
	public void testFuncDecl() {
		List<Node> nodes = typecheckSPL("facR( n ) :: Int -> Int {\n" +
				"if (n < 2 ) {\n " +
				"return 1;\n " +
				"} else {\n" +
				"return n * facR ( n - 1 );\n" +
				"}\n" +
				"}");
		assertTypecheckSuccess();
		for(Node n: nodes)
			assertEquals(Types.intType, n.getType());
	}

    @Test
    public void testOutOfScopeAssignment() {
        List<Node> nodes = typecheckSPL("facR( n ) :: Int -> Int {\n" +
                "[Int] a = 1:[];\n " +
                "if (n < 2 ) {\n " +
                "b = a;\n"+
                "return 1;\n " +
                "} else {\n" +
                "return n * facR ( n - 1 );\n" +
                "}\n" +
                "}");
        assertTypecheckFailure();
//        for(Node n: nodes)
//            assertEquals(Types.intType, n.getType());
    }

    @Test
    public void testTwoFuncDecl() {
        List<Node> nodes = typecheckSPL("facR( n ) :: Int -> Int {\n" +
                "if (n < 2 ) {\n " +
                "return 1;\n " +
                "} else {\n" +
                "return n * facR ( n - 1 );\n" +
                "}\n" +
                "}\n" +
                "id(a) :: Int -> Int {\n" +
                "a = 3;"+
                "return a+1;\n" +
                "}");
        assertTypecheckSuccess();
        for(Node n: nodes)
            assertEquals(Types.intType, n.getType());
    }

	@Test
	public void testTwoFuncDeclWrongVariableScope() {
		typecheckSPL("facR( n ) :: Int -> Int {\n" +
				"if (n < 2 ) {\n " +
				"return 1;\n " +
				"} else {\n" +
				"return n * facR ( n - 1 );\n" +
				"}\n" +
				"}\n" +
				"id(a) :: Int -> Int {\n" +
				"a = 3;"+
				"return a+n;\n" +
				"}");
		assertTypecheckFailure();
	}

    @Test
    public void testFunctionUse() {
        List<Node> nodes = typecheckSPL("facR( n ) :: Int -> Int {\n" +
                "if (n < 2 ) {\n " +
                "return 1;\n " +
                "} else {\n" +
                "return n * facR ( n - 1 );\n" +
                "}\n" +
                "}\n" +
                "fun(a) :: Int -> Int {\n" +
                "a = 3;\n"+
                "return facR(a+1);\n" +
                "}");
        assertTypecheckSuccess();
        for(Node n: nodes)
            assertEquals(Types.intType, n.getType());
    }

    @Test
    public void testFunctionUseVariableOutOfScore() {
        typecheckSPL("facR( n ) :: Int -> Int {\n" +
                "if (n < 2 ) {\n " +
                "return 1;\n " +
                "} else {\n" +
                "return n * facR ( n - 1 );\n" +
                "}\n" +
                "}\n" +
                "fun(a) :: Int -> Int {\n" +
                "a = 3;\n"+
                "return facR(n+1);\n" +
                "}\n"+
                "get2() :: -> Int {\n" +
                "return 2;\n" +
                "}");
        assertTypecheckFailure();
    }

    @Test
    public void testFunctionUseValidType() {
        List<Node> nodes = typecheckSPL("facR( n ) :: Int -> Int {\n" +
                "if (n < 2 ) {\n " +
                "return 1;\n " +
                "} else {\n" +
                "return n * facR ( n - 1 );\n" +
                "}\n" +
                "}\n" +
                "fun(a) :: Int -> Int {\n" +
                "a = 3;\n"+
                "return facR(a+1);\n" +
                "}\n"+
                "get2() :: -> Int {\n" +
                "return 2;\n" +
                "}\n"+
                "myfun() :: -> Int {\n" +
                "return get2();\n" +
                "}");
        assertTypecheckSuccess();
        for(Node n: nodes)
            assertEquals(Types.intType, n.getType());
    }

    @Test
    public void testFunctionOutOfScope() {
        typecheckSPL("facR( n ) :: Int -> Int {\n" +
                "if (n < 2 ) {\n " +
                "return 1;\n " +
                "} else {\n" +
                "return n * facR ( n - 1 );\n" +
                "}\n" +
                "}\n" +
                "fun(a) :: Int -> Int {\n" +
                "a = 3;\n"+
                "return facR(a+1);\n" +
                "}\n"+
                "get2() :: -> Int {\n" +
                "return myfun();\n" +
                "}\n"+
                "myfun() :: -> Int {\n" +
                "return get2();\n" +
                "}");
        assertTypecheckFailure();
    }

    @Test
    public void testFunctionTooFewArguments() {
        typecheckSPL("facR( n ) :: Int -> Int {\n" +
                "if (n < 2 ) {\n " +
                "return 1;\n " +
                "} else {\n" +
                "return n * facR ( n - 1 );\n" +
                "}\n" +
                "}\n" +
                "fun(a) :: Int -> Int {\n" +
                "a = 3;\n"+
                "return facR();\n" +
                "}\n"+
                "get2() :: -> Int {\n" +
                "return 2;\n" +
                "}");
        assertTypecheckFailure();
    }

    @Test
    public void testHandmade() {
        String s = ReadSPL.readLineByLineJava8(rootFolder + "handmade.spl");

        List<Node> nodes = typecheckSPL(s);
        assertTypecheckSuccess();

        Type empty = ((VariableDeclaration) nodes.get(0)).varType;
        assertEquals(Types.varType(Types.listType(Types.emptyListType)), empty);
    }

    @Test
    public void testVarList() {
        String s = ReadSPL.readLineByLineJava8(rootFolder + "var_list.spl");

        List<Node> nodes = typecheckSPL(s);
        assertTypecheckSuccess();
        Type a = ((VariableDeclaration) nodes.get(0)).varType;
        assertEquals(Types.varType(Types.listType(Types.emptyListType)), a);
    }


    @Test
    public void testFunctionsExampleMarkus() {
	    String s = ReadSPL.readLineByLineJava8(rootFolder + "markus/2-compile-errors/functions.spl");

        typecheckSPL(s);
        assertTypecheckFailure();
    }

    @Test
    public void testListsExampleMarkus() {
        String s = ReadSPL.readLineByLineJava8(rootFolder + "markus/2-compile-errors/lists.spl");

        typecheckSPL(s);
        assertTypecheckFailure();
    }

    @Test
    public void testAssociativityOkExampleMarkus() {
        String s = ReadSPL.readLineByLineJava8(rootFolder + "markus/3-ok/associativity.spl");

        typecheckSPL(s);
        assertTypecheckSuccess();
    }


    @Test
    public void testAssignmentsOkExampleMarkus() {
        String s = ReadSPL.readLineByLineJava8(rootFolder + "markus/3-ok/assignments.spl");

        typecheckSPL(s);
        assertTypecheckSuccess();
    }


    @Test
    public void testFunctionsOkExampleMarkus() {
        String s = ReadSPL.readLineByLineJava8(rootFolder + "markus/3-ok/functions.spl");

        typecheckSPL(s);
        assertTypecheckSuccess();
    }

    @Test
    public void testFunctionsSimpleOkExampleMarkus() {
        String s = ReadSPL.readLineByLineJava8(rootFolder + "markus/3-ok/functionsSimple.spl");

        typecheckSPL(s);
        assertTypecheckSuccess();
    }

    @Test
    public void testfunctionArgumentsSimpleOkExampleMarkus() {
        String s = ReadSPL.readLineByLineJava8(rootFolder + "markus/3-ok/functionArgumentsSimple.spl");

        typecheckSPL(s);
        assertTypecheckSuccess();
    }

    @Test
    public void testglobalVariablesOkExampleMarkus() {
        String s = ReadSPL.readLineByLineJava8(rootFolder + "markus/3-ok/globalVariables.spl");

        typecheckSPL(s);
        assertTypecheckSuccess();
    }

    @Test
    public void testglobalVariablesSimpleOkExampleMarkus() {
        String s = ReadSPL.readLineByLineJava8(rootFolder + "markus/3-ok/globalVariablesSimple.spl");

        typecheckSPL(s);
        assertTypecheckSuccess();
    }

    @Test
    public void testtuplesOkExampleMarkus() {
        String s = ReadSPL.readLineByLineJava8(rootFolder + "markus/3-ok/tuples.spl");

        typecheckSPL(s);
        assertTypecheckSuccess();
    }

    @Test
    public void testlistFunction3OkExampleMarkus() {
        String s = ReadSPL.readLineByLineJava8(rootFolder + "lists_crazy.spl");

        typecheckSPL(s);
        assertTypecheckSuccess();
    }




    @Test
    public void testlistCrazy() {
        String s = ReadSPL.readLineByLineJava8(rootFolder + "lists_crazy.spl");

        typecheckSPL(s);
        assertTypecheckSuccess();
    }

    @Test
    public void testinvalidListCrazy() {
        String s = ReadSPL.readLineByLineJava8(rootFolder + "invalid_lists_crazy.spl");

        typecheckSPL(s);
        assertTypecheckFailure();
    }

    @Test
    public void testtuplesCrazy() {
        String s = ReadSPL.readLineByLineJava8(rootFolder + "tuples_crazy.spl");

        typecheckSPL(s);
        assertTypecheckSuccess();
    }

    @Test
    public void testCommentsExampleMarkus() {
        String s = ReadSPL.readLineByLineJava8(rootFolder + "markus/3-ok/globalVariables.spl");

        typecheckSPL(s);
        assertTypecheckSuccess();
    }

    @Test
    public void testAllTestsByMarkus() {
	    Long sleepTime = 10L;
        try (Stream<Path> paths = Files.walk(Paths.get(rootFolder + "markus"))) {
            paths.forEach(path ->{
                if(Files.isRegularFile(path)){
                    try {
                        // To not mess up printing
                        Thread.sleep( sleepTime);
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }

                    System.out.println(path.toString());
                    String s = ReadSPL.readLineByLineJava8(path.toString());
                    try {
                        // To not mess up printing
                        Thread.sleep( sleepTime);
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }

                    try {
                        setUp();
                        typecheckSPL(s);
                        if(path.toString().contains("ok")){
                            assertTypecheckSuccess();
                        } else {
                            assertTypecheckFailure();
                        }
                    } catch (ParseException e){
                        if(path.toString().contains("parse")){
                            System.err.println("Parse Exception Found!");
                        } else {
                            throw e;
                        }
                    }
                }
            });
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    @Test
    public void testVariableAlreadyExists() {
        String s = ReadSPL.readLineByLineJava8(rootFolder + "variable_already_exists.spl");

        typecheckSPL(s);
        assertTypecheckFailure();
    }


    @Test
    public void testIfNotReturning() {
        String s = ReadSPL.readLineByLineJava8(rootFolder + "if_not_returning.spl");

        typecheckSPL(s);
        assertTypecheckFailure();
    }
}
