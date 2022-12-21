package edu.ufl.cise.plpfa22;

import edu.ufl.cise.plpfa22.ast.*;

public class TypeInferenceVisitor implements ASTVisitor {
    boolean finalRound = false;
    int Cur, Pre;

    SymbolTable symbolTable = new SymbolTable();
    public Types.Type[] noProcedure = {Types.Type.NUMBER, Types.Type.STRING, Types.Type.BOOLEAN};
    public Types.Type[] forTimes = {Types.Type.NUMBER, Types.Type.BOOLEAN};

    private boolean isType(Types.Type type1, Types.Type type2){ return type1 == type2;}
    private boolean isType(Types.Type type1, Types.Type ...types){
        for(Types.Type type: types){
            if(type==type1){
                return true;
            }
        }
        return false;
    }
    @Override
    public Object visitBlock(Block block, Object arg) throws PLPException {
        for(ConstDec node: block.constDecs){
            if(node != null){
                node.visit(this,arg);
            }
        }

        for(VarDec node: block.varDecs){
            if(node != null){
                node.visit(this,arg);
            }
        }
        for(ProcDec node: block.procedureDecs){
            if(node!=null){
                node.visit(this,arg);
            }
        }
        for(ProcDec node: block.procedureDecs){
            if(node != null){
                symbolTable.enterScope();
                node.block.visit(this,arg);
                symbolTable.leaveScope();
            }
        }

        Statement statement = block.statement;
        statement.visit(this,arg);

        return null;
    }

    @Override
    public Object visitProgram(Program program, Object arg) throws PLPException {
        while (true){
            Cur = 0;
            program.block.visit(this,arg);
            if(Pre==Cur){
                finalRound = true;
                break;
            }
            Pre = Cur;
        }
        program.block.visit(this,arg);

        return null;
    }

    @Override
    public Object visitStatementAssign(StatementAssign statementAssign, Object arg) throws PLPException {
        Types.Type t1 = (Types.Type) statementAssign.ident.visit(this,arg);
        Types.Type t2 = (Types.Type) statementAssign.expression.visit(this,arg);
        if(statementAssign.ident.getDec() instanceof ConstDec ){
            throw new TypeCheckException("ConstDec cannot be typed again");
        }
        if(t1 == Types.Type.PROCEDURE || t2== Types.Type.PROCEDURE){
            throw new TypeCheckException("ProcDec cannot be typed again");
        }
        else if(t1 == null && t2 != null){
            statementAssign.ident.getDec().setType(t2);
            Cur += 1;
        }
        else if(t1 != null && t2 == null){
            statementAssign.expression.setType(t1);
            Cur += 1;
        }
        else if(t1 != null && t1 != t2){
            throw new TypeCheckException("different type error");
        }
        return null;
    }

    @Override
    public Object visitVarDec(VarDec varDec, Object arg) throws PLPException {
        SymbolTable.Triple t = symbolTable.lookup(varDec.ident.getStringValue());
        if(t==null){
            boolean flag = symbolTable.insert(varDec.ident.getStringValue(),varDec);

            if(flag){
                varDec.setNest(symbolTable.nest);

            }
        }else{

            boolean flag = symbolTable.insert(varDec.ident.getStringValue(),varDec);
            if(flag){
                varDec.setNest(symbolTable.nest);
                if(t.dec().getType()!=null && t.scopeNum()==symbolTable.currentScope){
                    varDec.setType(t.dec().getType());
                    Cur += 1;
                }
            }
        }
        return varDec.getType();
    }

    @Override
    public Object visitStatementCall(StatementCall statementCall, Object arg) throws PLPException {
        Types.Type type = (Types.Type) statementCall.ident.visit(this,arg);
        if(finalRound){
            if(type != Types.Type.PROCEDURE){
                throw new TypeCheckException("type must be procedure");
            }
        }else{
            if(type != Types.Type.PROCEDURE && type != null){
                throw new TypeCheckException("type must be procedure");
            }
        }

        return null;
    }

    @Override
    public Object visitStatementInput(StatementInput statementInput, Object arg) throws PLPException {
        Types.Type type = (Types.Type) statementInput.ident.visit(this,arg);
        if(statementInput.ident.getDec() instanceof ConstDec){
            throw new TypeCheckException("const cannot be input");
        }
        if(finalRound){
            if(!isType(type,noProcedure)){
                throw new TypeCheckException("type must be Number, String or Boolean(true,input)");
            }
        }else{
            if(!isType(type,noProcedure) && type != null){
                throw new TypeCheckException("type must be Number, String or Boolean(false,input)");
            }
        }
        return null;
    }

    @Override
    public Object visitStatementOutput(StatementOutput statementOutput, Object arg) throws PLPException {
        Types.Type type = (Types.Type) statementOutput.expression.visit(this,arg);
        if(finalRound){
            if(!isType(type,noProcedure)){

                throw new TypeCheckException("type must be Number, String or Boolean(true,output)");
            }
        }else{
            if(!isType(type,noProcedure) && type != null){
                throw new TypeCheckException("type must be Number, String or Boolean(false,output)");
            }
        }
        return null;
    }

    @Override
    public Object visitStatementBlock(StatementBlock statementBlock, Object arg) throws PLPException {
        for(Statement statement:statementBlock.statements){
            statement.visit(this,arg);
        }
        return null;
    }

    @Override
    public Object visitStatementIf(StatementIf statementIf, Object arg) throws PLPException {
        Types.Type type = (Types.Type) statementIf.expression.visit(this,arg);
        if(finalRound){
            if(!isType(type,Types.Type.BOOLEAN,Types.Type.NUMBER)){
                throw new TypeCheckException("guard type is not boolean");
            }
        }else{
            if(!isType(type,Types.Type.BOOLEAN, Types.Type.NUMBER) && type != null){
                throw new TypeCheckException("guard type is not boolean");
            }
        }
        statementIf.statement.visit(this,arg);
        return null;
    }

    @Override
    public Object visitStatementWhile(StatementWhile statementWhile, Object arg) throws PLPException {
        Types.Type type = (Types.Type) statementWhile.expression.visit(this,arg);
        if(finalRound){
            if(!isType(type,Types.Type.BOOLEAN)){
                throw new TypeCheckException("guard type is not boolean");
            }
        }else{
            if(!isType(type,Types.Type.BOOLEAN) && type != null){
                throw new TypeCheckException("guard type is not boolean");
            }
        }

        statementWhile.statement.visit(this,arg);
        return null;
    }

    @Override
    public Object visitExpressionBinary(ExpressionBinary expressionBinary, Object arg) throws PLPException {
        Types.Type type1 = (Types.Type) expressionBinary.e0.visit(this,arg);
        Types.Type type2 = (Types.Type) expressionBinary.e1.visit(this,arg);

        // 设置type
        IToken.Kind op = expressionBinary.op.getKind();

        if(type1 == null){
            type1 = (Types.Type) expressionBinary.e1.visit(this,arg);
            type2 = null;
        }
        if(op == IToken.Kind.PLUS){
            if(expressionBinary.getType()!=null){
                if((type1 != null && type1 != expressionBinary.getType()) ||
                        (type2 != null && type2 != expressionBinary.getType())){
                    throw new TypeCheckException("infer and real type is different(plus)");
                }
                else{
                    if(expressionBinary.e0.getType() == null){
                        expressionBinary.e0.setType(expressionBinary.getType());
                        Cur += 1;
                    }
                    if(expressionBinary.e1.getType() == null){
                        expressionBinary.e1.setType(expressionBinary.getType());
                        Cur += 1;
                    }
                }
            }
            else {
                if(isType(type1,noProcedure)){
                    if(type2 == null){
                        expressionBinary.setType(type1);
                        if((expressionBinary.e0.getType() != null && expressionBinary.e0.getType()!= type1) ||
                                (expressionBinary.e1.getType() != null && expressionBinary.e1.getType()!= type1) ){
                            throw new TypeCheckException("type not compatible");
                        }
                        expressionBinary.e0.setType(type1);
                        expressionBinary.e1.setType(type1);
                        Cur += 1;

                    }else{
                        if(type1 == type2){
                            expressionBinary.setType(type1);
                            Cur += 1;

                        }else{
                            throw new TypeCheckException("the expression type are different on both side of plus");
                        }
                    }
                }
                else if(type1 != null && type2 != null){
                    throw new TypeCheckException("plus op only apply in number, string, boolean");
                }
            }
        }
        else if(op == IToken.Kind.MINUS || op == IToken.Kind.DIV || op == IToken.Kind.MOD ){
            if(expressionBinary.getType()!=null){
                if((type1 != null && type1 != expressionBinary.getType()) ||
                        (type2 != null && type2 != expressionBinary.getType())){
                    throw new TypeCheckException("infer and real type is different(minus,div,mod)");
                }
                else{
                    if(expressionBinary.e0.getType() == null){
                        expressionBinary.e0.setType(expressionBinary.getType());
                        Cur += 1;

                    }
                    if(expressionBinary.e1.getType() == null){
                        expressionBinary.e1.setType(expressionBinary.getType());
                        Cur += 1;

                    }
                }
            }
            else {
                if(isType(type1,Types.Type.NUMBER)){
                    if(type2 == null){
                        expressionBinary.setType(type1);
                        expressionBinary.e0.setType(type1);
                        expressionBinary.e1.setType(type1);
                        Cur += 1;

                    }else {
                        if(type1 == type2){
                            expressionBinary.setType(type1);
                            Cur += 1;

                        }else{
                            throw new TypeCheckException("the expression type are different on both side of minus,div,mod");
                        }
                    }
                }
                else if(type1 != null && type2 != null){
                    throw new TypeCheckException("minus,div,mod op only apply in number");
                }
            }
        }

        else if(op == IToken.Kind.TIMES){
            if(expressionBinary.getType()!=null){
                if((type1 != null && type1 != expressionBinary.getType()) ||
                        (type2 != null && type2 != expressionBinary.getType())){
                    throw new TypeCheckException("infer and real type is different(times)");
                }
                else{
                    if(expressionBinary.e0.getType() == null){
                        expressionBinary.e0.setType(expressionBinary.getType());
                        Cur += 1;
                    }
                    if(expressionBinary.e1.getType() == null){
                        expressionBinary.e1.setType(expressionBinary.getType());
                        Cur += 1;
                    }
                }
            }
            else {
                if(isType(type1,forTimes)){
                    if(type2 == null){
                        expressionBinary.setType(type1);
                        expressionBinary.e0.setType(type1);
                        expressionBinary.e1.setType(type1);
                        Cur += 1;

                    }else {
                        if(type1 == type2){
                            expressionBinary.setType(type1);
                            Cur += 1;

                        }else {
                            throw new TypeCheckException("the expression type are different on both side of times");
                        }
                    }
                }
                else if(type1 != null && type2 != null){
                    throw new TypeCheckException("times op only apply in number, boolean");
                }
            }
        }
        else if(op == IToken.Kind.EQ || op == IToken.Kind.NEQ || op == IToken.Kind.LT
                || op == IToken.Kind.LE ||op == IToken.Kind.GT ||op == IToken.Kind.GE){
            if(isType(type1,noProcedure)){
                if(type2 == null){
                    expressionBinary.setType(Types.Type.BOOLEAN);
                    expressionBinary.e0.setType(type1);
                    expressionBinary.e1.setType(type1);
                    Cur += 1;
                }else {
                    if(type1==type2){
                        expressionBinary.setType(Types.Type.BOOLEAN);
                        Cur += 1;
                    }else {
                        throw new TypeCheckException("the expression type are different on both side of comparison op");
                    }
                }
            }
            else if(type1 != null && type2 != null){
                throw new TypeCheckException(" comparison op only apply in number, string, boolean");
            }
        }
        return expressionBinary.getType();
    }

    @Override
    public Object visitExpressionIdent(ExpressionIdent expressionIdent, Object arg) throws PLPException {
        SymbolTable.Triple t = symbolTable.lookup(expressionIdent.firstToken.getStringValue());

        expressionIdent.setDec(t.dec());
        expressionIdent.setNest(symbolTable.nest);
        if(expressionIdent.getType()!=null && t.dec().getType()==null){
            t.dec().setType(expressionIdent.getType());
            Cur += 1;
        }
        else if(expressionIdent.getType()==null && t.dec().getType() != null){
            expressionIdent.setType(t.dec().getType());
            Cur += 1;
        }
        else if(expressionIdent.getType() !=null && t.dec().getType() != null && expressionIdent.getType() !=t.dec().getType()){

            throw new TypeCheckException("type error");
        }
        if(finalRound){
            if(expressionIdent.getType()==null){
                throw new TypeCheckException("expressionIdent is not full typed");
            }
        }

        return expressionIdent.getType();
    }

    @Override
    public Object visitExpressionNumLit(ExpressionNumLit expressionNumLit, Object arg) throws PLPException {
        if(expressionNumLit.getType()!= Types.Type.NUMBER) {
            expressionNumLit.setType(Types.Type.NUMBER);
            Cur += 1;
        }
        return expressionNumLit.getType();
    }

    @Override
    public Object visitExpressionStringLit(ExpressionStringLit expressionStringLit, Object arg) throws PLPException {
        if(expressionStringLit.getType()!= Types.Type.STRING) {
            expressionStringLit.setType(Types.Type.STRING);
            Cur += 1;
        }
        return expressionStringLit.getType();
    }

    @Override
    public Object visitExpressionBooleanLit(ExpressionBooleanLit expressionBooleanLit, Object arg) throws PLPException {
        if(expressionBooleanLit.getType()!= Types.Type.BOOLEAN) {
            expressionBooleanLit.setType(Types.Type.BOOLEAN);
            Cur += 1;
        }
        return expressionBooleanLit.getType();
    }

    @Override
    public Object visitProcedure(ProcDec procDec, Object arg) throws PLPException {
        SymbolTable.Triple t = symbolTable.lookup(procDec.ident.getStringValue());
        if(t==null){
            boolean flag = symbolTable.insert(procDec.ident.getStringValue(),procDec);

            if(flag){
                procDec.setNest(symbolTable.nest);
                procDec.setType(Types.Type.PROCEDURE);
                Cur += 1;
            }
        }else{
            boolean flag = symbolTable.insert(procDec.ident.getStringValue(),procDec);
            if(flag){
                procDec.setNest(symbolTable.nest);
                procDec.setType(Types.Type.PROCEDURE);
                Cur += 1;
            }
        }

        return procDec.getType();

    }

    @Override
    public Object visitConstDec(ConstDec constDec, Object arg) throws PLPException {
        SymbolTable.Triple t = symbolTable.lookup(constDec.ident.getStringValue());
        if(t==null){
            boolean flag = symbolTable.insert(constDec.ident.getStringValue(),constDec);
            if(flag){
                constDec.setNest(symbolTable.nest);

                String type = constDec.val.getClass().getTypeName();
                switch (type){
                    case "java.lang.String" -> constDec.setType(Types.Type.STRING);
                    case "java.lang.Integer" -> constDec.setType(Types.Type.NUMBER);
                    case "java.lang.Boolean" -> constDec.setType(Types.Type.BOOLEAN);
                    default -> throw new TypeCheckException("this kind is not a type");
                }
                Cur += 1;
            }
        }else{
            boolean flag = symbolTable.insert(constDec.ident.getStringValue(),constDec);
            if(flag){
                constDec.setNest(symbolTable.nest);

                String type = constDec.val.getClass().getTypeName();
                switch (type){
                    case "java.lang.String" -> constDec.setType(Types.Type.STRING);
                    case "java.lang.Integer" -> constDec.setType(Types.Type.NUMBER);
                    case "java.lang.Boolean" -> constDec.setType(Types.Type.BOOLEAN);
                    default -> throw new TypeCheckException("this kind is not a type");
                }
                Cur += 1;
            }
        }

        return constDec.getType();
    }

    @Override
    public Object visitStatementEmpty(StatementEmpty statementEmpty, Object arg) throws PLPException {
        return null;
    }

    @Override
    public Object visitIdent(Ident ident, Object arg) throws PLPException {
        SymbolTable.Triple t = symbolTable.lookup(ident.firstToken.getStringValue());
        if(t !=null){
            ident.setDec(t.dec());
            ident.setNest(symbolTable.nest);
        }
        if(finalRound){
            if(ident.getDec().getType()==null){
                throw new TypeCheckException("ident is not typed");
            }
        }

        return ident.getDec().getType();
    }
}
