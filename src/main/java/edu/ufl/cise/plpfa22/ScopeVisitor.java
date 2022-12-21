package edu.ufl.cise.plpfa22;

import edu.ufl.cise.plpfa22.ast.*;


public class ScopeVisitor implements ASTVisitor {

    SymbolTable symbolTable = new SymbolTable();
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
        program.block.visit(this,arg);

        return null;
    }

    @Override
    public Object visitStatementAssign(StatementAssign statementAssign, Object arg) throws PLPException {
        statementAssign.ident.visit(this,arg);
        statementAssign.expression.visit(this,arg);
        return null;
    }

    @Override
    public Object visitVarDec(VarDec varDec, Object arg) throws PLPException {
        SymbolTable.Triple t = symbolTable.lookup(varDec.ident.getStringValue());
        if(t==null){
            boolean flag = symbolTable.insert(varDec.ident.getStringValue(),varDec);

            if(flag){ varDec.setNest(symbolTable.nest);}
        }else{
            if(t.scopeNum()==symbolTable.currentScope){
                throw new ScopeException("this ident already declared ");
            }
            boolean flag = symbolTable.insert(varDec.ident.getStringValue(),varDec);
            if(flag){ varDec.setNest(symbolTable.nest);}
        }

        return null;
    }

    @Override
    public Object visitStatementCall(StatementCall statementCall, Object arg) throws PLPException {
        statementCall.ident.visit(this,arg);
        return null;
    }

    @Override
    public Object visitStatementInput(StatementInput statementInput, Object arg) throws PLPException {
        statementInput.ident.visit(this,arg);
        return null;
    }

    @Override
    public Object visitStatementOutput(StatementOutput statementOutput, Object arg) throws PLPException {
        statementOutput.expression.visit(this,arg);
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
        statementIf.expression.visit(this,arg);
        statementIf.statement.visit(this,arg);
        return null;
    }

    @Override
    public Object visitStatementWhile(StatementWhile statementWhile, Object arg) throws PLPException {
        statementWhile.expression.visit(this,arg);
        statementWhile.statement.visit(this,arg);
        return null;
    }

    @Override
    public Object visitExpressionBinary(ExpressionBinary expressionBinary, Object arg) throws PLPException {
        expressionBinary.e0.visit(this,arg);
        expressionBinary.e1.visit(this,arg);
        return null;
    }

    @Override
    public Object visitExpressionIdent(ExpressionIdent expressionIdent, Object arg) throws PLPException {
        SymbolTable.Triple t = symbolTable.lookup(expressionIdent.firstToken.getStringValue());
        if(t==null){
            throw new ScopeException("this expressionIdent have not declared yet");

        }else {
            expressionIdent.setDec(t.dec());
            expressionIdent.setNest(symbolTable.nest);
        }

        return null;
    }

    @Override
    public Object visitExpressionNumLit(ExpressionNumLit expressionNumLit, Object arg) throws PLPException {

        return null;
    }

    @Override
    public Object visitExpressionStringLit(ExpressionStringLit expressionStringLit, Object arg) throws PLPException {
        return null;
    }

    @Override
    public Object visitExpressionBooleanLit(ExpressionBooleanLit expressionBooleanLit, Object arg) throws PLPException {
        return null;
    }

    @Override
    public Object visitProcedure(ProcDec procDec, Object arg) throws PLPException {
        SymbolTable.Triple t = symbolTable.lookup(procDec.ident.getStringValue());

        if(t==null){
            boolean flag = symbolTable.insert(procDec.ident.getStringValue(),procDec);

            if(flag){ procDec.setNest(symbolTable.nest);}
        }else{
            if(t.scopeNum()==symbolTable.currentScope){
                throw new ScopeException("this ident already declared ");
            }
            boolean flag = symbolTable.insert(procDec.ident.getStringValue(),procDec);
            if(flag){ procDec.setNest(symbolTable.nest);}
        }

        return null;

    }

    @Override
    public Object visitConstDec(ConstDec constDec, Object arg) throws PLPException {
        SymbolTable.Triple t = symbolTable.lookup(constDec.ident.getStringValue());

        if(t==null){
            boolean flag = symbolTable.insert(constDec.ident.getStringValue(),constDec);

            if(flag){ constDec.setNest(symbolTable.nest);}
        }else{

            if(t.scopeNum()==symbolTable.currentScope){
                throw new ScopeException("this ident already declared ");
            }
            boolean flag = symbolTable.insert(constDec.ident.getStringValue(),constDec);
            if(flag){ constDec.setNest(symbolTable.nest);}
        }

        return null;
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
        }else {throw new ScopeException("no declared ");}

        return null;
    }
}
