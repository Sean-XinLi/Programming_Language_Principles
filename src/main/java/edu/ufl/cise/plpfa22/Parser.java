package edu.ufl.cise.plpfa22;

        import edu.ufl.cise.plpfa22.ast.*;

        import java.awt.*;
        import java.util.ArrayList;
        import java.util.List;

public class Parser implements IParser {
    ILexer lexer;
    IToken token;

    IToken.Kind[] const_val = { IToken.Kind.NUM_LIT,
            IToken.Kind.STRING_LIT,
            IToken.Kind.BOOLEAN_LIT};
    IToken.Kind[] firstMatch = {IToken.Kind.KW_CONST,
            IToken.Kind.KW_VAR,
            IToken.Kind.KW_PROCEDURE};
    IToken.Kind[] stateMatch = {IToken.Kind.IDENT,
            IToken.Kind.KW_CALL,
            IToken.Kind.QUESTION,
            IToken.Kind.BANG,
            IToken.Kind.KW_BEGIN,
            IToken.Kind.KW_IF,
            IToken.Kind.KW_WHILE};
    IToken.Kind[] compareMatch = {IToken.Kind.EQ,
            IToken.Kind.NEQ,
            IToken.Kind.LT,
            IToken.Kind.LE,
            IToken.Kind.GT,
            IToken.Kind.GE};
    IToken.Kind[] plusOrMinus = {IToken.Kind.PLUS, IToken.Kind.MINUS};
    IToken.Kind[] multiplicative = {IToken.Kind.TIMES, IToken.Kind.DIV, IToken.Kind.MOD};

    Parser(ILexer lexer) throws LexicalException {
        this.lexer = lexer;
        token = lexer.next();
    }
    // functional methods for parser
    private IToken consume() throws LexicalException {
        IToken firstToken = token;
        token = lexer.next();
        return firstToken;
    }
    private boolean isKind(IToken.Kind kind){
        return token.getKind() == kind;
    }

    private boolean isKind(IToken.Kind ...kinds){
        for( IToken.Kind kind: kinds){
            if (kind == token.getKind()){
                return true;
            }
        }
        return false;
    }
    private IToken match(IToken.Kind kind) throws SyntaxException, LexicalException {
        IToken firstToken = token;
        if(isKind(kind)){
            consume();
        }else {
            throw new SyntaxException("a Syntax Error");
        }
        return firstToken;
    }

    // parser

    private Program program() throws LexicalException, SyntaxException {
        IToken firstToken = token;
        Block block = block();
        match(IToken.Kind.DOT);
        return new Program(firstToken, block);
    }

    private Block block() throws LexicalException, SyntaxException {
        IToken firstToken = token;
        List<ConstDec> constDecs = new ArrayList<>();
        List<VarDec> varDecs = new ArrayList<>();
        List<ProcDec> procDecs = new ArrayList<>();
        Statement statement = statementEmpty();
        while (isKind(firstMatch) || isKind(stateMatch)){
            if(isKind(IToken.Kind.KW_CONST)){
                while (true){
                    consume();
                    ConstDec ele = constDec();
                    constDecs.add(ele);
                    // next is , or ;
                    // if , continue in while(isKind(IToken.Kind.KW_CONST))
                    // if ; continue in while (isKind(firstMatch)) or quit
                    if(isKind(IToken.Kind.COMMA,IToken.Kind.SEMI)){
                        if(isKind(IToken.Kind.SEMI)){
                            match(IToken.Kind.SEMI);
                            break;
                        }
                    }else throw new SyntaxException("a Syntax Error");
                }
            }
            if (isKind(IToken.Kind.KW_VAR)){
                while (true){
                    consume();
                    VarDec ele = varDec();
                    varDecs.add(ele);
                    if(isKind(IToken.Kind.COMMA, IToken.Kind.SEMI)){
                        if(isKind(IToken.Kind.SEMI)){
                            match(IToken.Kind.SEMI);
                            break;
                        }
                    }else throw new SyntaxException("a Syntax Error");
                }
            }
            if (isKind(IToken.Kind.KW_PROCEDURE)){
                consume();
                ProcDec ele = procDec();
                procDecs.add(ele);
                match(IToken.Kind.SEMI);
            }
            if (isKind(stateMatch)){
                statement = statement();
            }

        }
        return new Block(firstToken,constDecs,varDecs,procDecs,statement);
    };

    private ConstDec constDec() throws LexicalException, SyntaxException {
        IToken firstToken = match(IToken.Kind.IDENT);
        match(IToken.Kind.EQ);
        if(isKind(const_val)){
            switch (token.getKind()){
                case NUM_LIT -> {
                    int val = token.getIntValue();
                    consume();
                    return new ConstDec(firstToken,firstToken,val);
                }

                case STRING_LIT -> {
                    String val = token.getStringValue();
                    consume();
                    return new ConstDec(firstToken,firstToken,val);
                }
                case BOOLEAN_LIT -> {
                    Boolean val = token.getBooleanValue();
                    consume();
                    return new ConstDec(firstToken,firstToken,val);}
                default -> throw new SyntaxException("a Syntax Error");
            }
        }else throw new SyntaxException("a Syntax Error");

    }

    private VarDec varDec() throws LexicalException, SyntaxException {
        IToken firstToken = match(IToken.Kind.IDENT);
        return new VarDec(firstToken,firstToken);
    }

    private ProcDec procDec() throws LexicalException, SyntaxException {
        IToken firstToken = match(IToken.Kind.IDENT);
        match(IToken.Kind.SEMI);
        Block block = block();
        return new ProcDec(firstToken,firstToken,block);
    }

    private StatementAssign statementAssign() throws LexicalException, SyntaxException {
        IToken firstToken = token;
        Ident id = ident();
        match(IToken.Kind.ASSIGN);
        Expression e = expression();
        return new StatementAssign(firstToken, id, e);
    }
    private StatementCall statementCall() throws LexicalException, SyntaxException {
        IToken firstToken = match(IToken.Kind.KW_CALL);
        // ident() will check if the token.king is kind.Ident or not
        Ident id = ident();
        return new StatementCall(firstToken, id);
    }
    private StatementInput statementInput() throws LexicalException, SyntaxException {
        IToken firstToken = match(IToken.Kind.QUESTION);
        Ident id = ident();
        return new StatementInput(firstToken,id);
    }
    private StatementOutput statementOutput() throws LexicalException, SyntaxException {
        IToken firstToken = match(IToken.Kind.BANG);
        Expression expression = expression();
        return new StatementOutput(firstToken,expression);
    }

    private StatementBlock statementBlock() throws LexicalException, SyntaxException {
        IToken firstToken = match(IToken.Kind.KW_BEGIN);
        List<Statement> statements = new ArrayList<>();
        Statement statement;
        if(isKind(IToken.Kind.KW_END)){
            statement = statementEmpty();
            statements.add(statement);
            match(IToken.Kind.KW_END);
            return new StatementBlock(firstToken,statements);
        }
        else if (!isKind(stateMatch)){
            throw new SyntaxException("a Syntax Error");
        }
        while(true){
            statement = statement();
            statements.add(statement);
            if(isKind(IToken.Kind.KW_END, IToken.Kind.SEMI)){
                if(isKind(IToken.Kind.SEMI)){
                    match(IToken.Kind.SEMI);
                }else{
                    match(IToken.Kind.KW_END);
                    break;}
            }else throw new SyntaxException("a Syntax Error");
        }
        return new StatementBlock(firstToken,statements);
    }

    private StatementIf statementIf() throws LexicalException, SyntaxException {
        IToken firstToken = match(IToken.Kind.KW_IF);
        Expression e = expression();
        match(IToken.Kind.KW_THEN);
        Statement s = statement();
        return new StatementIf(firstToken, e, s);
    }
    private StatementWhile statementWhile() throws LexicalException, SyntaxException {
        IToken firstToken = match(IToken.Kind.KW_WHILE);
        Expression e = expression();
        match(IToken.Kind.KW_DO);
        Statement s = statement();
        return new StatementWhile(firstToken,e,s);
    }

    private StatementEmpty statementEmpty(){ return new StatementEmpty(token);}



    private Statement statement() throws LexicalException, SyntaxException {
        Statement statement = statementEmpty();
        switch (token.getKind()){
            case IDENT -> statement = statementAssign();
            case KW_CALL -> statement = statementCall();
            case QUESTION -> statement = statementInput();
            case BANG -> statement = statementOutput();
            case KW_BEGIN -> statement = statementBlock();
            case KW_IF -> statement = statementIf();
            case KW_WHILE -> statement = statementWhile();
        }
        return statement;
    }
    private Ident ident() throws LexicalException, SyntaxException {
        IToken firstToken = match(IToken.Kind.IDENT);
        return new Ident(firstToken);
    }

    private Expression expression() throws LexicalException, SyntaxException {

        IToken firstToken = token;
        Expression left, right;
        IToken op;
        left = addExp();
        while (isKind(compareMatch)){
            op = consume();
            right = addExp();
            left = new ExpressionBinary(firstToken,left,op,right);
        }
        return left;
    }

    private Expression addExp() throws LexicalException, SyntaxException {
        IToken firstToken = token;
        Expression left, right;
        IToken op;
        left = multiExp();

        while (isKind(plusOrMinus)){
            op = consume();
            right = multiExp();
            left = new ExpressionBinary(firstToken,left,op,right);
        }
        return left;
    }

    private Expression multiExp() throws LexicalException, SyntaxException {
        IToken firstToken = token;
        Expression left, right;
        IToken op;
        left = primaryExp();
        while (isKind(multiplicative)){
            op = token;
            consume();
            right = primaryExp();
            left = new ExpressionBinary(firstToken,left,op,right);
        }
        return left;
    }

    private Expression primaryExp() throws LexicalException, SyntaxException {
        Expression exp;
        switch (token.getKind()){
            case NUM_LIT -> exp = expressionNumLit();
            case STRING_LIT -> exp = expressionStringLit();
            case BOOLEAN_LIT -> exp = expressionBooleanLit();
            case IDENT -> exp = expressionIdent();
            case LPAREN -> {
                consume();
                exp = expression();
                match(IToken.Kind.RPAREN);
            }
            default -> throw new SyntaxException("a Syntax Error");
        }
        return exp;
    }
    private ExpressionNumLit expressionNumLit() throws LexicalException, SyntaxException {
        IToken firstToken = match(IToken.Kind.NUM_LIT);
        return new ExpressionNumLit(firstToken);
    }
    private ExpressionStringLit expressionStringLit() throws LexicalException, SyntaxException {
        IToken firstToken = match(IToken.Kind.STRING_LIT);
        return new ExpressionStringLit(firstToken);
    }
    private ExpressionBooleanLit expressionBooleanLit() throws LexicalException, SyntaxException {
        IToken firstToken = match(IToken.Kind.BOOLEAN_LIT);
        return new ExpressionBooleanLit(firstToken);
    }
    private ExpressionIdent expressionIdent() throws LexicalException, SyntaxException {
        IToken firstToken = match(IToken.Kind.IDENT);
        return new ExpressionIdent(firstToken);
    }



    @Override
    public ASTNode parse() throws PLPException {
        Program program = program();
        match(IToken.Kind.EOF);
        return program;
    }
}
