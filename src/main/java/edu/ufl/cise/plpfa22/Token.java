package edu.ufl.cise.plpfa22;

public class Token implements IToken{
    final Kind kind;
    final String input;
    final int pos;
    final int length;
    final int line;
    final int col;
    private int nextLine;
    private int nextCol;
    private final SourceLocation sourceLocation;


    public Token(Kind kind, String input, int pos, int length,int line,int col){
        this.kind = kind;
        this.input = input;
        this.pos = pos;
        this.length = length;
        this.line = line;
        this.col = col;
        this.nextLine = line;
        this.nextCol = col + length;
        sourceLocation = new SourceLocation(line,col);
        check();
    }
    public int getNextLine(){return this.nextLine;}
    public int getNextCol(){return this.nextCol;}
    private void check(){
        if (this.getKind() == Kind.NUM_LIT){
            getIntValue();
        }
        if (this.getKind() == Kind.STRING_LIT){
            getStringValue();
        }
        if (this.getText().length != 0 && this.getText()[0] == '\"'){
            if(this.getKind() != Kind.STRING_LIT){
                throw new IllegalStateException("lexer bug");
            }
        }

    }


    @Override
    public Kind getKind() {
        return kind;
    }




    @Override
    public char[] getText() {
        int len = input.length();
        char[] array = new char[len];
        for(int i=0; i<length; i++){
            array[i] = input.charAt(i);
        }
        return array;
    }

    @Override
    public SourceLocation getSourceLocation() {
        return this.sourceLocation;
    }

    @Override
    public int getIntValue() {
        if(this.getKind() == Kind.NUM_LIT){
            try {
                return Integer.parseInt(String.valueOf(this.input));
            }catch (Exception e){throw new IllegalStateException("lexer bug");}

        }else {throw new IllegalStateException("lexer bug");}
    }

    @Override
    public boolean getBooleanValue() {
        if(this.getKind() == Kind.BOOLEAN_LIT){
            String string = String.copyValueOf(this.getText());
            if(string.equals("TRUE")){
                return true;
            }
            else if(string.equals("FALSE")){
                return false;
            }
        }
        throw new IllegalStateException("lexer bug");
    }

    @Override
    public String getStringValue() {

        if(this.getKind() == Kind.STRING_LIT){
            char[] text = this.getText();
            StringBuilder res = new StringBuilder();
            String ele = "";
            nextLine = line;
            nextCol = this.col + 1;
            for (int i=1;i<=text.length-2;i++) {
                nextCol++;
                if (ele.equals("\\")) {
                    switch (text[i]) {
                        case 'b' -> ele = "\b";
                        case 't' -> ele = "\t";
                        case 'n' -> ele = "\n";
                        case 'f' -> ele = "\f";
                        case 'r' -> ele = "\r";
                        case '"' -> ele = "\"";
                        case '\'' -> ele = "'";
                        case '\\' -> ele = "\\";
                    }
                    res.append(ele);
                    ele = "";
                }
                else if (text[i] == '\\') {
                    ele = "\\";
                } else {
                    if(text[i]=='\n' ||text[i]=='\r'){
                        nextLine += 1;
                        nextCol = 1;
                    }
                    res.append(text[i]);
                }

            }
            nextCol += 1;
            return res.toString();
        }else if(this.getKind() == Kind.IDENT){
            nextCol = this.col + length;
            return this.input;
        }
        throw new IllegalStateException("lexer bug");
    }
}
