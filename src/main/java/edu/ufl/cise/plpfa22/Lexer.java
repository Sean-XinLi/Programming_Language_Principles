package edu.ufl.cise.plpfa22;

import java.util.HashMap;
import java.util.HashSet;

class Lexer implements ILexer{
    String input;

    private int line = 1;
    private int endCol = 1;
    private int startPos = 0;
    private int startCol = 1;
    private int pos = 0;

    private final HashMap<String,IToken.Kind> keywords = new HashMap<>();
    private final HashSet<String> isBool = new HashSet<>();


    public Lexer(String input){
        // add 1 for EOF
        this.input = input+ " ";
        SetReserved();
    }

    private enum State{
        START,
        IN_IDENT,
        IN_NUM,
        IN_STRING,
        HAVE_COLON,
        HAVE_LT,
        HAVE_GT,
        HAVE_SLASH,
        IN_COMMENT
    }
    private void SetReserved(){
        keywords.put("CONST", IToken.Kind.KW_CONST);
        keywords.put("VAR", IToken.Kind.KW_VAR);
        keywords.put("PROCEDURE", IToken.Kind.KW_PROCEDURE);
        keywords.put("CALL", IToken.Kind.KW_CALL);
        keywords.put("BEGIN", IToken.Kind.KW_BEGIN);
        keywords.put("END", IToken.Kind.KW_END);
        keywords.put("IF", IToken.Kind.KW_IF);
        keywords.put("THEN", IToken.Kind.KW_THEN);
        keywords.put("WHILE", IToken.Kind.KW_WHILE);
        keywords.put("DO", IToken.Kind.KW_DO);
        isBool.add("TRUE");
        isBool.add("FALSE");

    }
    private Token getToken(IToken.Kind kind,int pos,int startPos, int line, int startCol){

        int length = pos - startPos;
        String text = input.substring(startPos, pos);
        return new Token(kind, text, startPos, length, line, startCol);
    }
    public Token DFA(){
        if(input.length()== 1){return getToken(IToken.Kind.EOF,pos,startPos,line,startCol);}

        State state = State.START;
        while(true){
            if(pos>input.length()-1){return getToken(IToken.Kind.EOF,(pos-1),(startPos-1),line,(startCol-1));}
            char ch = input.charAt(pos);

            switch (state) {
                case START -> {
                    startPos = pos;
                    startCol = endCol;
                    switch (ch) {
                        case ' ', '\t', '\n', '\r' -> {
                            // assume that a ‘\r’ without a following ‘\n’ will not occur in your input.
                            // when meeting ‘\r’’\n’, ‘\n’, means line feed
                            if (ch == '\n'|| ch== '\r') {
                                line++;
                                endCol = 1;
                            } else {
                                endCol++;
                            }
                            pos++;
                        }
                        case 'a', 'b', 'c', 'd', 'e', 'f', 'g', 'h', 'i', 'j', 'k', 'l', 'm', 'n', 'o', 'p', 'q', 'r', 's', 't', 'u', 'v', 'w', 'x', 'y', 'z', 'A', 'B', 'C', 'D', 'E', 'F', 'G', 'H', 'I', 'J', 'K', 'L', 'M', 'N', 'O', 'P', 'Q', 'R', 'S', 'T', 'U', 'V', 'W', 'X', 'Y', 'Z', '$', '_' -> {
                            endCol++;
                            pos++;
                            state = State.IN_IDENT;
                        }
                        case '0' -> {
                            endCol++;
                            pos++;
                            return getToken(IToken.Kind.NUM_LIT,pos,startPos,line,startCol);
                        }
                        case '1', '2', '3', '4', '5', '6', '7', '8', '9' -> {
                            endCol++;
                            pos++;
                            state = State.IN_NUM;

                        }
                        case '.' -> {
                            endCol++;
                            pos++;
                            return getToken(IToken.Kind.DOT,pos,startPos,line,startCol);
                        }
                        case ',' -> {
                            endCol++;
                            pos++;
                            return getToken(IToken.Kind.COMMA,pos,startPos,line,startCol);
                        }
                        case ';' -> {
                            endCol++;
                            pos++;
                            return getToken(IToken.Kind.SEMI,pos,startPos,line,startCol);
                        }
                        case '\"' -> {
                            endCol++;
                            pos++;
                            state = State.IN_STRING;
                        }
                        case '(' -> {
                            endCol++;
                            pos++;
                            return getToken(IToken.Kind.LPAREN,pos,startPos,line,startCol);
                        }
                        case ')' -> {
                            endCol++;
                            pos++;
                            return getToken(IToken.Kind.RPAREN,pos,startPos,line,startCol);
                        }
                        case '+' -> {
                            endCol++;
                            pos++;
                            return getToken(IToken.Kind.PLUS,pos,startPos,line,startCol);
                        }
                        case '-' -> {
                            endCol++;
                            pos++;
                            return getToken(IToken.Kind.MINUS,pos,startPos,line,startCol);
                        }
                        case '*' -> {
                            endCol++;
                            pos++;
                            return getToken(IToken.Kind.TIMES,pos,startPos,line,startCol);
                        }
                        case '/' -> {
                            endCol++;
                            pos++;
                            state = State.HAVE_SLASH;
                        }
                        case '%' -> {
                            endCol++;
                            pos++;
                            return getToken(IToken.Kind.MOD,pos,startPos,line,startCol);
                        }
                        case '?' -> {
                            endCol++;
                            pos++;
                            return getToken(IToken.Kind.QUESTION,pos,startPos,line,startCol);
                        }
                        case '!' -> {
                            endCol++;
                            pos++;
                            return getToken(IToken.Kind.BANG,pos,startPos,line,startCol);
                        }
                        case ':' -> {
                            endCol++;
                            pos++;
                            state = State.HAVE_COLON;
                        }
                        case '=' -> {
                            endCol++;
                            pos++;
                            return getToken(IToken.Kind.EQ,pos,startPos,line,startCol);
                        }
                        case '#' -> {
                            endCol++;
                            pos++;
                            return getToken(IToken.Kind.NEQ,pos,startPos,line,startCol);
                        }
                        case '<' -> {
                            endCol++;
                            pos++;
                            state = State.HAVE_LT;
                        }
                        case '>' -> {
                            endCol++;
                            pos++;
                            state = State.HAVE_GT;
                        }
                        default -> throw new IllegalStateException("lexer bug");
                    }
                }

                case IN_IDENT -> {
                    switch (ch) {
                        case 'a', 'b', 'c', 'd', 'e', 'f', 'g', 'h', 'i', 'j', 'k', 'l', 'm', 'n', 'o', 'p', 'q', 'r', 's', 't', 'u', 'v', 'w', 'x', 'y', 'z', 'A', 'B', 'C', 'D', 'E', 'F', 'G', 'H', 'I', 'J', 'K', 'L', 'M', 'N', 'O', 'P', 'Q', 'R', 'S', 'T', 'U', 'V', 'W', 'X', 'Y', 'Z', '$', '_', '1', '2', '3', '4', '5', '6', '7', '8', '9', '0' -> {
                            endCol++;
                            pos++;
                        }
                        default -> {
                            String text = input.substring(startPos, pos);
                            IToken.Kind kind;
                            if (isBool.contains(text)){
                                kind = IToken.Kind.BOOLEAN_LIT;
                            }
                            else{
                                kind = keywords.getOrDefault(text, IToken.Kind.IDENT);
                            }
                            return getToken(kind,pos,startPos,line,startCol);
                        }
                    }
                }
                case IN_NUM -> {
                    switch (ch){
                        case '1', '2', '3', '4', '5', '6', '7', '8', '9','0' ->{
                            endCol++;
                            pos++;
                        }
                        default -> {
                            return getToken(IToken.Kind.NUM_LIT,pos,startPos,line,startCol);
                        }
                    }

                }
                case IN_STRING -> {
                    if (((pos)==input.length()-1) && input.charAt(pos) != '\"'){
                        throw new IllegalStateException("lexer bug");
                    }
                    switch (ch){
                        // '\'
                        case '\\' ->{
                            endCol ++;
                            pos ++;
                            char chNext = input.charAt(pos);
                            switch (chNext){
                                case 'b','t','n','f','r','\'','\"','\\' ->{
                                    endCol ++;
                                    pos ++;
                                }
                                default -> throw new IllegalStateException("lexer bug");
                            }
                        }

                        case '\"' -> {
                            endCol ++;
                            pos ++;
                            return getToken(IToken.Kind.STRING_LIT,pos,startPos,line,startCol);
                        }

                        default -> {
                            endCol ++;
                            pos ++;
                        }
                    }
                }
                case HAVE_COLON -> {
                    if (ch == '=') {
                        endCol++;
                        pos++;
                        return getToken(IToken.Kind.ASSIGN, pos, startPos, line, startCol);
                    } else {
                        throw new IllegalStateException("lexer bug");
                    }
                }
                case HAVE_LT -> {
                    if (ch == '=') {
                        endCol++;
                        pos++;
                        return getToken(IToken.Kind.LE, pos, startPos, line, startCol);
                    } else {
                        return getToken(IToken.Kind.LT, pos, startPos, line, startCol);
                    }

                }
                case HAVE_GT -> {
                    if (ch == '=') {
                        endCol++;
                        pos++;
                        return getToken(IToken.Kind.GE, pos, startPos, line, startCol);
                    } else {
                        return getToken(IToken.Kind.GT, pos, startPos, line, startCol);
                    }
                }
                case HAVE_SLASH -> {
                    if(ch == '/'){
                        endCol++;
                        pos++;
                        state = State.IN_COMMENT;
                    } else return getToken(IToken.Kind.DIV,pos,startPos,line,startCol);
                }

                case IN_COMMENT -> {
                    if(pos==input.length()-1){return getToken(IToken.Kind.EOF,pos,startPos,line,startCol);}
                    else if(ch == '\n'|| ch == '\r'){
                        line++;
                        endCol = 1;
                        pos++;
                        state = State.START;
                    }
                    else {
                        endCol++;
                        pos++;
                    }
                }
                default -> throw new IllegalStateException("lexer bug");
            }

        }

    }
    @Override
    public IToken next() throws LexicalException {
        try{
            Token token = DFA();
            line = token.getNextLine();
            endCol = token.getNextCol();
            return token;
        }catch (IllegalStateException e){
            throw new LexicalException("lexer bug",line,startCol);
        }
    }

    @Override
    public IToken peek() throws LexicalException {
        try{
            Token token = DFA();
            IToken.SourceLocation sourceLocation = token.getSourceLocation();
            line  = sourceLocation.line();
            startCol = sourceLocation.column();
            startPos = token.pos;
            endCol = startCol;
            pos = startPos;
            return token;
        }catch (IllegalStateException e){
            throw new LexicalException("lexer bug",line,startCol);
        }
    }
}
