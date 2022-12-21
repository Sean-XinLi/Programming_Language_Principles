package edu.ufl.cise.plpfa22;

import edu.ufl.cise.plpfa22.ast.Declaration;


import java.util.ArrayList;
import java.util.HashMap;
import java.util.Stack;


public class SymbolTable{
    int currentScope, nest;
    Stack<Integer> stack = new Stack<>();
    public final record Triple(int scopeNum, int nest, Declaration dec){};
    HashMap <String, ArrayList<Triple>> symbolTable = new HashMap<>();
    public SymbolTable(){
        this.currentScope = 0;
        this.nest = 0;
        stack.push(this.currentScope);
    }
    public void enterScope(){
        currentScope += 1;
        this.nest += 1;
        stack.push(currentScope);
    }
    public void leaveScope(){
        stack.pop();
        this.nest -= 1;
    }
    public boolean isContain(String ident){
        return symbolTable.containsKey(ident);
    }

    public Triple lookup(String ident) throws ScopeException {
        if(!isContain(ident)){
            return null;
        }
        Triple res = null;
        ArrayList<Triple> triples = symbolTable.get(ident);
        for(int i=triples.size()-1;i>=0;i--){
            int temp = triples.get(i).scopeNum();
            if(stack.contains(temp)){
                res = triples.get(i);
                break;
            }
        }
        return res;
    }

    public boolean insert(String ident, Declaration dec){
        ArrayList<Triple> triples = new ArrayList<>();
        if(isContain(ident)){
            triples = symbolTable.get(ident);
            for(Triple t:triples){
                if(t.scopeNum() == currentScope){
                    return false;
                }
            }
        }
        Triple triple = new Triple(currentScope,nest,dec);
        triples.add(triple);
        symbolTable.put(ident,triples);
        return true;
    }

}





