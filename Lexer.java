import java.util.*;
import java.io.*;
public class Lexer {

    public static String margin = "";

    // holds any number of tokens that have been put back
    private Stack<Token> stack;

    // the source of physical symbols
    // (use BufferedReader instead of Scanner because it can
    //  read a single physical symbol)
    private BufferedReader input;

    // one lookahead physical symbol
    private int lookahead;

    // construct a Lexer ready to produce tokens from a file
    public Lexer( String fileName ) {
        try {
            input = new BufferedReader( new FileReader( fileName ) );
        }
        catch(Exception e) {
            error("Problem opening file named [" + fileName + "]" );
        }
        stack = new Stack<Token>();
        lookahead = 0;  // indicates no lookahead symbol present
    }// constructor

    // produce the next token
    private Token getNext() {
        if( ! stack.empty() ) {
            //  produce the most recently putback token
            Token token = stack.pop();
            return token;
        }
        else {
            // produce a token from the input source

            int state = 1;  // state of FA
            String data = "";  // specific info for the token
            boolean done = false;
            int sym;  // holds current symbol

            do {
                sym = getNextSymbol();

// System.out.println("current symbol: " + sym + " state = " + state );

                if ( state == 1 ) {
                    if ( sym == 9 || sym == 10 || sym == 13 ||
                            sym == 32 ) {// whitespace
                        state = 1;
                    }
                    else if (lowercase(sym) ) { //any lowercase
                        data += (char) sym;
                        state = 2; //go to name or token
                    }
                    else if ( uppercase(sym) ) {// any uppercase
                        data += (char) sym;
                        state = 3; //go to Classname
                    }
                    else if ( digit( sym ) ) {
                        data += (char) sym;
                        state = 9; //go to num
                    }
                    else if ( sym == '-' ) {
                        data += (char) sym;
                        state = 8; //go to num
                    }
                    else if ( sym == '"' ) {
                        state = 4; //go to string
                    }
                    else if ( sym == '{' || sym == '}' || sym == ';' ||
                            sym == '(' || sym == ')' ||
                            sym == ',' || sym == '=' || sym == '.'
                    ) {
                        data += (char) sym;
                        state = 11;
                        done = true;
                    }
                    else if ( sym == '/' ) {
                        state = 12;
                    }
                    else if ( sym == -1 ) {// end of file
                        state = 14;
                        done = true;
                    }
                    else {
                        error("Error in lexical analysis phase with symbol "
                                + sym + " in state " + state );
                    }
                }

		else if ( state == 2 ) {
                    if ( letter(sym) || digit(sym)) {
                        data += (char) sym;
                        state = 2;
                    }
                    else {// done with variable token
                        putBackSymbol( sym );
                        done = true;
                    }
                }

                else if ( state == 3 ) {
                    if ( letter(sym) || digit(sym)) {
                        data += (char) sym;
                        state = 3;
                    }
                    else {// done with Class token
                        putBackSymbol( sym );
                        done = true;
                    }

                }

                else if ( state == 4 ) { // String Token
                    if ( letter(sym) || digit(sym)) {
                        data += (char) sym;
                        state = 4;
			}
		    else if(sym =='/'){
                        state = 5;
                    }
                    else if(sym =='"'){
                         state = 6;
                    }
                }

		else if ( state == 5 ) {// check for special char/instuction
                     if (digit(sym)){
                     for (int i=0; i<2 ; i++)
                     {
                         data += (char) sym;
                         sym = getNextSymbol(); //get next symbol
                      }
                        state = 4;
                    }
                    else {
                        error("Error in lexical analysis phase with symbol "
                                + sym + " in state " + state );
                    }
                }

                else if ( state == 6 ) {
                         putBackSymbol( sym );
                         done = true;
                         return new Token( "string", data );
                       
                    }
                    

                // note: states 9, and 10 are accepting states with
                //       no arcs out of them, so they are handled
                //       in the arc going into them

                else if ( state ==8 ) {// saw - neg. num
                    if ( digit( sym ) ) {
                         data += (char) sym;
                         state = 9; //go to num
                    }
                    else {// saw something other than digit after -
                        error("Error in lexical analysis phase with symbol "
                                + sym + " in state " + state );
                    }
                }

                                else if ( state ==9 ) {// saw num
                    if ( digit( sym ) ) {
                         data += (char) sym;
                         state = 9; //go to num
                    }
                    else if(sym == '.'){
                         data += (char) sym;
                         state = 10; //go to num
                    }
                    else {// saw something other than digit after -
                        putBackSymbol( sym );  // for next token
                        return new Token( "num" );
                    }
                }

                else if ( state == 10 ) {// saw /, might be single or comment
                    if ( digit( sym ) ) {
                         data += (char) sym;
                         state = 10; //go to num
                    }
                    else {// saw something other than * after /
                        putBackSymbol( sym );  // for next token
                        return new Token( "num" );
                    }
                }

                else if ( state == 12 ) {// looking for / to follow *?
                    if ( sym == '/' ) {// comment is done
                        state = 13;  // continue in this call to getNextToken
                        data = "";
                    }
                    else // saw something other than digit after -
                        error("Error in lexical analysis phase with symbol "
                                + sym + " in state " + state );
                }
                else if ( state == 13 ) {// looking for / to follow *?
                    if ( sym != 92 ) { // comment is done
                        state = 13;  // continue in this call to getNextToken
                        data = "";
                    }
                    else if ( sym == 92 ) { // comment is done
                        state = 1;  // continue in this call to getNextToken
                        data = "";
                    }
                    else // saw something other than digit after -
                        error("Error in lexical analysis phase with symbol "
                                + sym + " in state " + state );
                  }

            }while( !done );

            // generate token depending on stopping state
            Token token;

            if ( state == 2 ) {
                // now anything starting with letter is either a
                // key word or a "var"
                if ( data.equals("def") || data.equals("end") ||
                        data.equals("if") || data.equals("else") ||
                        data.equals("return")
                ) {
                    return new Token( data, "" );
                }
                else {
                    return new Token( "var", data );
                }
            }
            else if ( state == 3 || state == 4 ) {
                return new Token( "num", data );
            }
            else if ( state == 7 ) {
                return new Token( "string", data );
            }
            else if ( state == 8 ) {
                return new Token( "single", data );
            }
            else if ( state == 14 ) {//pfennel changed  9to 14
                return new Token( "eof", data );
            }

            else {// Lexer error
                error("somehow Lexer FA halted in bad state " + state );
                return null;
            }

        }// else generate token from input

    }// getNext

    public Token getNextToken() {
        Token token = getNext();
        System.out.println("                     got token: " + token );
        return token;
    }

    public void putBackToken( Token token )
    {
        System.out.println( margin + "put back token " + token.toString() );
        stack.push( token );
    }

    // next physical symbol is the lookahead symbol if there is one,
    // otherwise is next symbol from file
    private int getNextSymbol() {
        int result = -1;

        if( lookahead == 0 ) {// is no lookahead, use input
            try{  result = input.read();  }
            catch(Exception e){}
        }
        else {// use the lookahead and consume it
            result = lookahead;
            lookahead = 0;
        }
        return result;
    }

    private void putBackSymbol( int sym ) {
        if( lookahead == 0 ) {// sensible to put one back
            lookahead = sym;
        }
        else {
            System.out.println("Oops, already have a lookahead " + lookahead +
                    " when trying to put back symbol " + sym );
            System.exit(1);
        }
    }// putBackSymbol

    private boolean letter( int code ) {
        return 'a'<=code && code<='z' ||
                'A'<=code && code<='Z';
    }
    private boolean uppercase( int code ) {
        return 'A'<=code && code<='Z';
    }
    private boolean lowercase( int code ) {
        return 'a'<=code && code<='z';
    }

    private boolean digit( int code ) {
        return '0'<=code && code<='9';
    }

    private boolean printable( int code ) {
        return ' '<=code && code<='~';
    }

    private static void error( String message ) {
        System.out.println( message );
        System.exit(1);
    }

    public static void main(String[] args) throws Exception {
        System.out.print("Enter file name: ");
        Scanner keys = new Scanner( System.in );
        String name = keys.nextLine();

        Lexer lex = new Lexer( name );
        Token token;

        do{
            token = lex.getNext();
            System.out.println( token.toString() );
        }while( ! token.getKind().equals( "eof" )  );

    }

}
