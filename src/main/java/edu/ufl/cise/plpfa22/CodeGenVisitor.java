package edu.ufl.cise.plpfa22;

import edu.ufl.cise.plpfa22.ast.*;
import org.objectweb.asm.*;
import edu.ufl.cise.plpfa22.IToken.Kind;
import edu.ufl.cise.plpfa22.ast.Types.Type;

import java.util.*;

public class CodeGenVisitor implements ASTVisitor, Opcodes {

	final String packageName;
	final String className;
	final String sourceFileName;
	final String fullyQualifiedClassName; 
	final String classDesc;


	ClassWriter classWriter;
	FieldVisitor fieldVisitor;
	MethodVisitor methodVisitor;

	List<CodeGenUtils.GenClass> genClassList = new ArrayList<>();
	HashMap<String,String> classNameMap = new HashMap<>();
	List<String> classNameList = new ArrayList<>();


	String currentClassName;
	String currentFullyQualifiedClassName;
	String currentClassDesc;
	String superCurrentClassName;
	String superCurrentClassDesc;

	public void simpleASTVisitor(List<ProcDec> procedureDecs,String currentFullyQualifiedClassName){
		String temp;
		for(ProcDec procDec:procedureDecs){
			String innerClassName = procDec.ident.getStringValue();
			temp = currentFullyQualifiedClassName + "$" + innerClassName;
			classNameList.add(temp);
			simpleASTVisitor(procDec.block.procedureDecs,temp);
		}
	}



	public String findSuperClass(String fullyQualifiedClassName){
		String superCurrentClassName;
		int lastIndex = fullyQualifiedClassName.lastIndexOf('$');
		if(lastIndex==-1){
			superCurrentClassName = null;
		}else {
			superCurrentClassName = fullyQualifiedClassName.substring(0,lastIndex);
		}
		return superCurrentClassName;

	}

	public String getShortName(String fullyQualifiedClassName){
		String shortName;
		int lastIndex = fullyQualifiedClassName.lastIndexOf('$');
		if(lastIndex==-1){
			shortName = "prog";
		}else {
			shortName = fullyQualifiedClassName.substring(lastIndex+1);
		}
		return shortName;
	}

	public CodeGenVisitor(String className, String packageName, String sourceFileName) {
		super();
		this.packageName = packageName;
		this.className = className;
		this.sourceFileName = sourceFileName;
		this.fullyQualifiedClassName = packageName + "/" + className;
		this.classDesc="L"+this.fullyQualifiedClassName+';';
	}

	@Override
	public Object visitProgram(Program program, Object arg) throws PLPException {
		classNameMap.put(className,fullyQualifiedClassName);
		currentFullyQualifiedClassName = fullyQualifiedClassName;
		//1. create a classWriter and visit it (for main class)
		classWriter = new ClassWriter(ClassWriter.COMPUTE_FRAMES);
		//Hint:  if you get failures in the visitMaxs, try creating a ClassWriter with 0
		// instead of ClassWriter.COMPUTE_FRAMES.  The result will not be a valid classfile,
		// but you will be able to print it so you can see the instructions.  After fixing,
		// restore ClassWriter.COMPUTE_FRAMES

		classWriter.visit(V18, ACC_PUBLIC | ACC_SUPER, fullyQualifiedClassName, null, "java/lang/Object", new String[]{"java/lang/Runnable"});
		classWriter.visitSource(sourceFileName,null);
		// 2. invokes a simple ASTVisitor to visit all procedure declarations and annotate them with their JVM name.
		// this is necessary Because of our procedure scoping rules you won’t know all jvm names
		// in a singles pass so you need to have some way to pass over the AST to get these names;
		simpleASTVisitor(program.block.procedureDecs,currentFullyQualifiedClassName);
		for(String member:classNameList){
			classWriter.visitNestMember(member);
			classWriter.visitInnerClass(member,findSuperClass(member),getShortName(member),0);
		}

		// 3.generate constructor code <init>
		{
			methodVisitor = classWriter.visitMethod(ACC_PUBLIC, "<init>", "()V", null, null);
			methodVisitor.visitCode();
			Label constructorStart = new Label();
			methodVisitor.visitLabel(constructorStart);
//			CodeGenUtils.genDebugPrint(methodVisitor,"entering <init>");
			// generate code to call superclass constructor
			methodVisitor.visitVarInsn(ALOAD,0);
			methodVisitor.visitMethodInsn(INVOKESPECIAL,"java/lang/Object","<init>","()V",false);

			methodVisitor.visitInsn(RETURN);
			Label constructorEnd = new Label();
			methodVisitor.visitLabel(constructorEnd);

			// finish up by visiting local vars of constructor
			// the fourth and fifth arguments are the region of code where the local
			// variable is defined as represented by the labels we inserted
			// classDesc : Ledu/ufl/cise/plpfa22/prog;
			methodVisitor.visitLocalVariable("this",classDesc,null,constructorStart,constructorEnd,0);
			methodVisitor.visitMaxs(1,1);
			methodVisitor.visitEnd();
		}

		// 4.get a method visitor for the main method.
		// invoke the run method
		{
			methodVisitor = classWriter.visitMethod(ACC_PUBLIC | ACC_STATIC, "main", "([Ljava/lang/String;)V", null, null);
			methodVisitor.visitCode();
			Label mainStart = new Label();
			methodVisitor.visitLabel(mainStart);
//			CodeGenUtils.genDebugPrint(methodVisitor,"entering main");
			// fullyQualifiedClassName:edu/ufl/cise/plpfa22/prog
			// init,run are already define, just call them here.
			methodVisitor.visitTypeInsn(NEW,fullyQualifiedClassName);
			methodVisitor.visitInsn(DUP);
			methodVisitor.visitMethodInsn(INVOKESPECIAL,fullyQualifiedClassName,"<init>","()V",false);
			methodVisitor.visitMethodInsn(INVOKEVIRTUAL,fullyQualifiedClassName,"run","()V",false);
			methodVisitor.visitInsn(RETURN);
			Label mainEnd = new Label();
			methodVisitor.visitLabel(mainEnd);
			methodVisitor.visitLocalVariable("args","[Ljava/lang/String;",null,mainStart,mainEnd,0);
			methodVisitor.visitLocalVariable("instance",classDesc,null,mainStart,mainEnd,1);
			methodVisitor.visitMaxs(1,1);
			methodVisitor.visitEnd();
		}
		// 5. pass classWriter to block.visit
		program.block.visit(this,classWriter);

		//finish up the class
		classWriter.visitEnd();


		//return a list of GenClass records defined in updated CodeGenUtils
		CodeGenUtils.GenClass genClass = new CodeGenUtils.GenClass(fullyQualifiedClassName,classWriter.toByteArray());
		genClassList.add(0,genClass);
		return genClassList;

	}

	@Override
	public Object visitBlock(Block block, Object arg) throws PLPException {
//		for (ConstDec constDec : block.constDecs) {
//			constDec.visit(this, arg);
//		}
		// 1 visits VarDecs
		for (VarDec varDec : block.varDecs) {
			varDec.visit(this, arg);
		}
		// 2. visit ProcDecs, passes current className
		for (ProcDec procDec: block.procedureDecs) {
			currentClassName = procDec.ident.getStringValue();
			currentFullyQualifiedClassName = classNameList.get(0);
			classNameList.remove(0);
			classNameList.add(currentFullyQualifiedClassName);
			procDec.visit(this, arg);
		}
		//add instructions from statement to method
		// create run method
		classWriter = (ClassWriter) arg;
		{
			currentClassDesc= "L"+currentFullyQualifiedClassName+';';
			methodVisitor = classWriter.visitMethod(ACC_PUBLIC, "run", "()V", null, null);
			methodVisitor.visitCode();
			Label runStart = new Label();
			methodVisitor.visitLabel(runStart);
//			CodeGenUtils.genDebugPrint(methodVisitor,"entering run");
			block.statement.visit(this,methodVisitor);
			methodVisitor.visitInsn(RETURN);
			Label runEnd = new Label();
			methodVisitor.visitLabel(runEnd);
			methodVisitor.visitLocalVariable("this",currentClassDesc,null,runStart,runEnd,0);
			methodVisitor.visitMaxs(1,1);
			methodVisitor.visitEnd();

		}
		return null;

	}

	@Override
	public Object visitConstDec(ConstDec constDec, Object arg) throws PLPException {

		return null;
	}

	@Override
	public Object visitVarDec(VarDec varDec, Object arg) throws PLPException {
		classWriter = (ClassWriter) arg;
		// initialize the var if this variable is used
		if (varDec.getType() != null){
			fieldVisitor = classWriter.visitField(ACC_PUBLIC, varDec.ident.getStringValue(), varDec.getType().getJvmType(), null, null);
		}
		fieldVisitor.visitEnd();
		return null;
	}

	@Override
	public Object visitProcedure(ProcDec procDec, Object arg) throws PLPException {
		superCurrentClassName = findSuperClass(currentFullyQualifiedClassName);
		superCurrentClassDesc = "L" + superCurrentClassName +";";
		String descriptorForInit = "(" + superCurrentClassDesc + ")V";



		// 1.create a classWriter object for new class
		ClassWriter cw;
		cw = new ClassWriter(ClassWriter.COMPUTE_FRAMES);
		cw.visit(V18, ACC_SUPER, currentFullyQualifiedClassName, null, "java/lang/Object", new String[]{"java/lang/Runnable"});
		{
			cw.visitNestHost(fullyQualifiedClassName);
			String prefix = currentFullyQualifiedClassName + "$";
			// visit inner class: currentFullyQualifiedClassName child class
			for(String innerClass:classNameList){
				if(innerClass.startsWith(prefix)){
					cw.visitInnerClass(innerClass,findSuperClass(innerClass),getShortName(innerClass),0);
				}
			}
		}

		// 2.add field for reference to enclosing class (this$n where n is nesting level)
		String fieldName = "this$"+procDec.getNest();
		fieldVisitor = cw.visitField(ACC_FINAL|ACC_SYNTHETIC,fieldName,superCurrentClassDesc,null,null);
		fieldVisitor.visitEnd();
		// 3.create init method that takes an instance of enclosing class as parameter and initializes this$n,

		{
			descriptorForInit = "(" + superCurrentClassDesc + ")V";
			methodVisitor = cw.visitMethod(0, "<init>", descriptorForInit, null, null);
			methodVisitor.visitCode();
			Label createInitStart = new Label();
			methodVisitor.visitLabel(createInitStart);
			methodVisitor.visitVarInsn(ALOAD, 0);
			methodVisitor.visitVarInsn(ALOAD, 1);
			methodVisitor.visitFieldInsn(PUTFIELD, currentFullyQualifiedClassName, fieldName, superCurrentClassDesc);
			Label createInitEnd = new Label();
			methodVisitor.visitLabel(createInitEnd);
		}

			// 3.5 then invokes superclass constructor(java/lang/object)
		{
			Label invokeInitStart = new Label();
			methodVisitor.visitLabel(invokeInitStart);
			methodVisitor.visitVarInsn(ALOAD,0);
			methodVisitor.visitMethodInsn(INVOKESPECIAL,"java/lang/Object","<init>","()V",false);
			methodVisitor.visitInsn(RETURN);
			Label invokeInitEnd = new Label();
			methodVisitor.visitLabel(invokeInitEnd);
			methodVisitor.visitLocalVariable("this",superCurrentClassDesc,null,invokeInitStart,invokeInitEnd,0);
			methodVisitor.visitMaxs(0,0);
			methodVisitor.visitEnd();
		}

		// 4. visit block to create run method
		procDec.block.visit(this,cw);
		//finish up the class
		cw.visitEnd();


		//return a list of GenClass records defined in updated CodeGenUtils
		CodeGenUtils.GenClass genClass = new CodeGenUtils.GenClass(currentFullyQualifiedClassName,cw.toByteArray());
		genClassList.add(genClass);
		currentFullyQualifiedClassName = findSuperClass(currentFullyQualifiedClassName);
		return null;
	}


	@Override
	public Object visitStatementAssign(StatementAssign statementAssign, Object arg) throws PLPException {
		statementAssign.expression.visit(this,arg);
		statementAssign.ident.visit(this,arg);
		return null;

	}


	@Override
	public Object visitStatementCall(StatementCall statementCall, Object arg) throws PLPException {
		methodVisitor = (MethodVisitor) arg;
		Label startCall = new Label();
		methodVisitor.visitLabel(startCall);

		// if is Recursive,not init
		if(statementCall.ident.firstToken.getStringValue().equals(getShortName(currentFullyQualifiedClassName))){
			methodVisitor.visitVarInsn(ALOAD,0);
			methodVisitor.visitMethodInsn(INVOKEVIRTUAL,currentFullyQualifiedClassName,"run","()V",false);
//			methodVisitor.visitInsn(RETURN);

		}
		// 1. create instance of class corresponding to procedure
		// 2. the <init> method takes instance of lexically enclosing class as parameter.
		// if the procedure is enclosed in this one, ALOAD_0 works.
		// (recall that we are in a virtual method, run,
		// so the JVM will have automatically load "this" into local variable slot 0.)
		// otherwise follow the chain of this$n references to find an instance of the enclosing class of the procedure.
		// (use nesting levels)
		// 3. invoke run method.
		else{

			String whereCall = currentFullyQualifiedClassName;
			String whereDesc;
			String whereDescParent;
			String whereDescParentDec;
			String descriptorForInit;

			int indexForCall = statementCall.ident.getNest();
			int indexForDec = statementCall.ident.getDec().getNest();

			for(String ele:classNameList){


				// whereDesc name end with this ident and its superclass equal currentFullyQualifiedClassName
				// currentFullyQualifiedClassName: prog, whereDesc: prog$p
				if(ele.endsWith(statementCall.ident.getFirstToken().getStringValue()) && findSuperClass(ele).equals(currentFullyQualifiedClassName)){
					whereDesc = ele;
					whereDescParent = findSuperClass(whereDesc);
					whereDescParentDec = "L" + whereDescParent + ";";
					descriptorForInit = "(" + whereDescParentDec + ")V";
					methodVisitor.visitTypeInsn(NEW,whereDesc);
					methodVisitor.visitInsn(DUP);
					methodVisitor.visitVarInsn(ALOAD,0);

					methodVisitor.visitMethodInsn(INVOKESPECIAL,whereDesc,"<init>",descriptorForInit,false);
					methodVisitor.visitMethodInsn(INVOKEVIRTUAL,whereDesc,"run","()V",false);

				}

				// whereDesc name start with currentFullyQualifiedClassName and end with this ident
				// currentFullyQualifiedClassName: prog, whereDesc: prog$...$p
				else if(ele.startsWith(currentFullyQualifiedClassName) && ele.endsWith(statementCall.ident.getFirstToken().getStringValue())){

					whereDesc = ele;
					whereDescParent = findSuperClass(whereDesc);
					whereDescParentDec = "L" + whereDescParent + ";";
					descriptorForInit = "(" + whereDescParentDec + ")V";
					methodVisitor.visitTypeInsn(NEW, whereDesc);
					methodVisitor.visitInsn(DUP);
					methodVisitor.visitVarInsn(ALOAD, 0);
					String currentClass = whereCall;
					for (int i = indexForCall - 1; i > 0; i--) {
						superCurrentClassName = findSuperClass(currentClass);
						superCurrentClassDesc = "L" + superCurrentClassName + ";";
						methodVisitor.visitFieldInsn(GETFIELD, currentClass, "this$" + i, superCurrentClassDesc);
						currentClass = superCurrentClassName;

					}
					methodVisitor.visitMethodInsn(INVOKESPECIAL, whereDesc, "<init>", descriptorForInit, false);
					methodVisitor.visitMethodInsn(INVOKEVIRTUAL, whereDesc, "run", "()V", false);

				}

				// 剩余情况：包括
				// 1、如果以该调用为结尾，并当前类与调用的类有共同的祖先
				// whereDesc name end with this ident and it has same superclass with currentFullyQualifiedClassName
				// and they have different nest
				// like currentFullyQualifiedClassName prog$p,whereDesc: prog$q
				// 2、如果以该调用为结尾，并当前类与调用的类没有共同的祖先
				// like currentFullyQualifiedClassName prog$p$q,whereDesc: prog$q1
				else if(ele.endsWith(statementCall.ident.getFirstToken().getStringValue())
						&& indexForCall != indexForDec){


					whereDesc = ele;
					whereDescParent = findSuperClass(whereDesc);
					whereDescParentDec = "L" + whereDescParent + ";";
					descriptorForInit = "(" + whereDescParentDec + ")V";


					methodVisitor.visitTypeInsn(NEW, whereDesc);
					methodVisitor.visitInsn(DUP);
					methodVisitor.visitVarInsn(ALOAD, 0);

					String currentClass = whereCall;
					for (int i = indexForCall - 1; i >= 0; i--) {
						superCurrentClassName = findSuperClass(currentClass);
						superCurrentClassDesc = "L" + superCurrentClassName + ";";
						methodVisitor.visitFieldInsn(GETFIELD, currentClass, "this$" + i, superCurrentClassDesc);
						currentClass = superCurrentClassName;

					}
					methodVisitor.visitMethodInsn(INVOKESPECIAL, whereDesc, "<init>", descriptorForInit, false);
					methodVisitor.visitMethodInsn(INVOKEVIRTUAL, whereDesc, "run", "()V", false);

				}

			}

		}

		return null;
	}

	@Override
	public Object visitStatementInput(StatementInput statementInput, Object arg) throws PLPException {
		statementInput.ident.visit(this,arg);
		return null;
	}


	@Override
	public Object visitStatementOutput(StatementOutput statementOutput, Object arg) throws PLPException {
		methodVisitor = (MethodVisitor) arg;
		methodVisitor.visitFieldInsn(GETSTATIC, "java/lang/System", "out", "Ljava/io/PrintStream;");
		statementOutput.expression.visit(this, arg);
		Type type = statementOutput.expression.getType();
		String JVMType = (type.equals(Type.NUMBER) ? "I" : (type.equals(Type.BOOLEAN) ? "Z" : "Ljava/lang/String;"));
		String printlnSig = "(" + JVMType +")V";
		methodVisitor.visitMethodInsn(INVOKEVIRTUAL, "java/io/PrintStream", "println", printlnSig, false);
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
		methodVisitor = (MethodVisitor) arg;
		Label afterIf = new Label();
		statementIf.expression.visit(this,arg);
		methodVisitor.visitJumpInsn(IFEQ,afterIf);
		Label startIf = new Label();
		methodVisitor.visitLabel(startIf);
		statementIf.statement.visit(this,arg);
		methodVisitor.visitLabel(afterIf);
		return null;
	}

	@Override
	public Object visitStatementWhile(StatementWhile statementWhile, Object arg) throws PLPException {
		methodVisitor = (MethodVisitor) arg;
		Label whileGuard = new Label();
		Label whileBody = new Label();

		methodVisitor.visitJumpInsn(GOTO,whileGuard);
		methodVisitor.visitLabel(whileBody);
		statementWhile.statement.visit(this,arg);
		methodVisitor.visitLabel(whileGuard);
		statementWhile.expression.visit(this,arg);
		methodVisitor.visitJumpInsn(IFNE,whileBody);
		return null;
	}

	@Override
	public Object visitExpressionBinary(ExpressionBinary expressionBinary, Object arg) throws PLPException {
		methodVisitor = (MethodVisitor) arg;
		Type argType = expressionBinary.e0.getType();
		Kind op = expressionBinary.op.getKind();
		switch (argType) {
		case NUMBER -> {
			expressionBinary.e0.visit(this, arg);
			expressionBinary.e1.visit(this, arg);
			switch (op) {
			case PLUS -> methodVisitor.visitInsn(IADD);
			case MINUS -> methodVisitor.visitInsn(ISUB);
			case TIMES -> methodVisitor.visitInsn(IMUL);
			case DIV -> methodVisitor.visitInsn(IDIV);
			case MOD -> methodVisitor.visitInsn(IREM);
			case EQ -> {
				Label labelNumEqFalseBr = new Label();
				methodVisitor.visitJumpInsn(IF_ICMPNE, labelNumEqFalseBr);
				methodVisitor.visitInsn(ICONST_1);
				Label labelPostNumEq = new Label();
				methodVisitor.visitJumpInsn(GOTO, labelPostNumEq);
				methodVisitor.visitLabel(labelNumEqFalseBr);
				methodVisitor.visitInsn(ICONST_0);
				methodVisitor.visitLabel(labelPostNumEq);
			}
			case NEQ -> {
				Label isFalse = new Label();
				methodVisitor.visitJumpInsn(IF_ICMPEQ,isFalse);
				methodVisitor.visitInsn(ICONST_1);
				Label afterNeq = new Label();
				methodVisitor.visitJumpInsn(GOTO,afterNeq);
				methodVisitor.visitLabel(isFalse);
				methodVisitor.visitInsn(ICONST_0);
				methodVisitor.visitLabel(afterNeq);

			}
			case LT -> {
				Label isTrue = new Label();
				methodVisitor.visitJumpInsn(IF_ICMPLT,isTrue);
				methodVisitor.visitInsn(ICONST_0);
				Label afterLT = new Label();
				methodVisitor.visitJumpInsn(GOTO,afterLT);
				methodVisitor.visitLabel(isTrue);
				methodVisitor.visitInsn(ICONST_1);
				methodVisitor.visitLabel(afterLT);

			}
			case LE -> {
				Label isTrue = new Label();
				methodVisitor.visitJumpInsn(IF_ICMPLE,isTrue);
				methodVisitor.visitInsn(ICONST_0);
				Label afterLE = new Label();
				methodVisitor.visitJumpInsn(GOTO,afterLE);
				methodVisitor.visitLabel(isTrue);
				methodVisitor.visitInsn(ICONST_1);
				methodVisitor.visitLabel(afterLE);
			}
			case GT -> {
				Label isTrue = new Label();
				methodVisitor.visitJumpInsn(IF_ICMPGT,isTrue);
				methodVisitor.visitInsn(ICONST_0);
				Label afterGT = new Label();
				methodVisitor.visitJumpInsn(GOTO,afterGT);
				methodVisitor.visitLabel(isTrue);
				methodVisitor.visitInsn(ICONST_1);
				methodVisitor.visitLabel(afterGT);

			}
			case GE -> {
				Label isTrue = new Label();
				methodVisitor.visitJumpInsn(IF_ICMPGE,isTrue);
				methodVisitor.visitInsn(ICONST_0);
				Label afterGE = new Label();
				methodVisitor.visitJumpInsn(GOTO,afterGE);
				methodVisitor.visitLabel(isTrue);
				methodVisitor.visitInsn(ICONST_1);
				methodVisitor.visitLabel(afterGE);

			}
			default -> throw new IllegalStateException("code gen bug in visitExpressionBinary NUMBER");

			}

		}
		case BOOLEAN -> {
			expressionBinary.e0.visit(this, arg);
			expressionBinary.e1.visit(this, arg);
			switch (op){
				case PLUS -> {
					Label hasTrue1 = new Label();
					Label hasTrue2 = new Label();
					methodVisitor.visitJumpInsn(IFNE,hasTrue1);
					methodVisitor.visitJumpInsn(IFNE,hasTrue2);
					methodVisitor.visitInsn(ICONST_0);
					Label afterPLUS = new Label();
					methodVisitor.visitJumpInsn(GOTO,afterPLUS);
					methodVisitor.visitLabel(hasTrue1);
					methodVisitor.visitInsn(POP);
					methodVisitor.visitLabel(hasTrue2);
					methodVisitor.visitInsn(ICONST_1);
					methodVisitor.visitLabel(afterPLUS);
				}
				case TIMES -> {
					Label hasFalse1 = new Label();
					Label hasFalse2 = new Label();
					methodVisitor.visitJumpInsn(IFEQ,hasFalse1);
					methodVisitor.visitJumpInsn(IFEQ,hasFalse2);
					methodVisitor.visitInsn(ICONST_1);
					Label afterTIMES = new Label();
					methodVisitor.visitJumpInsn(GOTO,afterTIMES);
					methodVisitor.visitLabel(hasFalse1);
					methodVisitor.visitInsn(POP);
					methodVisitor.visitLabel(hasFalse2);
					methodVisitor.visitInsn(ICONST_0);
					methodVisitor.visitLabel(afterTIMES);
				}
				case EQ -> {
					Label labelEqFalseBr = new Label();
					methodVisitor.visitJumpInsn(IF_ICMPNE, labelEqFalseBr);
					methodVisitor.visitInsn(ICONST_1);
					Label labelPostEq = new Label();
					methodVisitor.visitJumpInsn(GOTO, labelPostEq);
					methodVisitor.visitLabel(labelEqFalseBr);
					methodVisitor.visitInsn(ICONST_0);
					methodVisitor.visitLabel(labelPostEq);
				}
				case NEQ -> {
					Label isFalse = new Label();
					methodVisitor.visitJumpInsn(IF_ICMPEQ,isFalse);
					methodVisitor.visitInsn(ICONST_1);
					Label afterNeq = new Label();
					methodVisitor.visitJumpInsn(GOTO,afterNeq);
					methodVisitor.visitLabel(isFalse);
					methodVisitor.visitInsn(ICONST_0);
					methodVisitor.visitLabel(afterNeq);

				}
				case LT -> {
					Label isTrue = new Label();
					methodVisitor.visitJumpInsn(IF_ICMPLT,isTrue);
					methodVisitor.visitInsn(ICONST_0);
					Label afterLT = new Label();
					methodVisitor.visitJumpInsn(GOTO,afterLT);
					methodVisitor.visitLabel(isTrue);
					methodVisitor.visitInsn(ICONST_1);
					methodVisitor.visitLabel(afterLT);
				}
				case LE -> {
					Label isTrue = new Label();
					methodVisitor.visitJumpInsn(IF_ICMPLE,isTrue);
					methodVisitor.visitInsn(ICONST_0);
					Label afterLE = new Label();
					methodVisitor.visitJumpInsn(GOTO,afterLE);
					methodVisitor.visitLabel(isTrue);
					methodVisitor.visitInsn(ICONST_1);
					methodVisitor.visitLabel(afterLE);

				}
				case GT -> {
					Label isTrue = new Label();
					methodVisitor.visitJumpInsn(IF_ICMPGT,isTrue);
					methodVisitor.visitInsn(ICONST_0);
					Label afterGT = new Label();
					methodVisitor.visitJumpInsn(GOTO,afterGT);
					methodVisitor.visitLabel(isTrue);
					methodVisitor.visitInsn(ICONST_1);
					methodVisitor.visitLabel(afterGT);
				}
				case GE -> {
					Label isTrue = new Label();
					methodVisitor.visitJumpInsn(IF_ICMPGE,isTrue);
					methodVisitor.visitInsn(ICONST_0);
					Label afterGE = new Label();
					methodVisitor.visitJumpInsn(GOTO,afterGE);
					methodVisitor.visitLabel(isTrue);
					methodVisitor.visitInsn(ICONST_1);
					methodVisitor.visitLabel(afterGE);

				}
				default -> throw new IllegalStateException("code gen bug in visitExpressionBinary NUMBER");

			}

		}
		case STRING -> {
			expressionBinary.e0.visit(this, arg);
			expressionBinary.e1.visit(this, arg);
			switch (op) {
				case PLUS -> methodVisitor.visitMethodInsn(INVOKEVIRTUAL,"java/lang/String",
												 "concat",
												 "(Ljava/lang/String;)Ljava/lang/String;",
												 false);
				case EQ ->{
					methodVisitor.visitMethodInsn(INVOKEVIRTUAL,"java/lang/String",
													 "equals",
													 "(Ljava/lang/Object;)Z",
													 false);
//					Label isTrue = new Label();
//					mv.visitJumpInsn(IFNE,isTrue);
//					mv.visitInsn(ICONST_0);
//					Label afterEQ = new Label();
//					mv.visitJumpInsn(GOTO,afterEQ);
//					mv.visitLabel(isTrue);
//					mv.visitInsn(ICONST_1);
//					mv.visitLabel(afterEQ);
				}
				case NEQ ->{
					methodVisitor.visitMethodInsn(INVOKEVIRTUAL,"java/lang/String",
							"equals",
							"(Ljava/lang/Object;)Z",
							false);
					Label isTrue = new Label();
					methodVisitor.visitJumpInsn(IFEQ,isTrue);
					methodVisitor.visitInsn(ICONST_0);
					Label afterNEQ = new Label();
					methodVisitor.visitJumpInsn(GOTO,afterNEQ);
					methodVisitor.visitLabel(isTrue);
					methodVisitor.visitInsn(ICONST_1);
					methodVisitor.visitLabel(afterNEQ);
				}
				case LT ->{
					methodVisitor.visitInsn(SWAP);
					methodVisitor.visitInsn(DUP2);
					Label isEqual = new Label();
					methodVisitor.visitMethodInsn(INVOKEVIRTUAL,"java/lang/String",
							"startsWith",
							"(Ljava/lang/String;)Z",
							false);
					Label isTrue = new Label();
					methodVisitor.visitJumpInsn(IFNE,isTrue);
					// in this situation the first two slot of stack is string, need to pop
					methodVisitor.visitInsn(POP2);
					methodVisitor.visitLabel(isEqual);
					methodVisitor.visitInsn(ICONST_0);
					Label afterLT = new Label();
					methodVisitor.visitJumpInsn(GOTO,afterLT);

					methodVisitor.visitLabel(isTrue);
					methodVisitor.visitInsn(SWAP);
					methodVisitor.visitMethodInsn(INVOKEVIRTUAL,"java/lang/String",
							"equals",
							"(Ljava/lang/Object;)Z",
							false);
					methodVisitor.visitJumpInsn(IFNE,isEqual);
					methodVisitor.visitInsn(ICONST_1);
					methodVisitor.visitLabel(afterLT);

				}
				case LE ->{
					methodVisitor.visitInsn(SWAP);
					methodVisitor.visitMethodInsn(INVOKEVIRTUAL,"java/lang/String",
							"startsWith",
							"(Ljava/lang/String;)Z",
							false);
//					Label isTrue = new Label();
//					mv.visitJumpInsn(IFNE,isTrue);
//					mv.visitInsn(ICONST_0);
//					Label afterLE = new Label();
//					mv.visitJumpInsn(GOTO,afterLE);
//					mv.visitLabel(isTrue);
//					mv.visitInsn(ICONST_1);
//					mv.visitLabel(afterLE);
				}

				case GT ->{
					methodVisitor.visitInsn(DUP2);
					Label isEqual = new Label();
					methodVisitor.visitMethodInsn(INVOKEVIRTUAL,"java/lang/String",
							"endsWith",
							"(Ljava/lang/String;)Z",
							false);
					Label isTrue = new Label();
					methodVisitor.visitJumpInsn(IFNE,isTrue);
					// in this situation the first two slot of stack is string, need to pop
					methodVisitor.visitInsn(POP2);
					methodVisitor.visitLabel(isEqual);
					methodVisitor.visitInsn(ICONST_0);
					Label afterGT = new Label();
					methodVisitor.visitJumpInsn(GOTO,afterGT);

					methodVisitor.visitLabel(isTrue);
					methodVisitor.visitMethodInsn(INVOKEVIRTUAL,"java/lang/String",
							"equals",
							"(Ljava/lang/Object;)Z",
							false);

					methodVisitor.visitJumpInsn(IFNE,isEqual);
					methodVisitor.visitInsn(ICONST_1);
					methodVisitor.visitLabel(afterGT);


				}
				case GE ->{

					methodVisitor.visitMethodInsn(INVOKEVIRTUAL,"java/lang/String",
							"endsWith",
							"(Ljava/lang/String;)Z",
							false);
//					Label isTrue = new Label();
//					mv.visitJumpInsn(IFNE,isTrue);
//					mv.visitInsn(ICONST_0);
//					Label afterGE = new Label();
//					mv.visitJumpInsn(GOTO,afterGE);
//					mv.visitLabel(isTrue);
//					mv.visitInsn(ICONST_1);
//					mv.visitLabel(afterGE);
				}

				default -> throw new IllegalStateException("code gen bug in visitExpressionBinary");
			}

		}
		default -> throw new IllegalStateException("code gen bug in visitExpressionBinary");

		}
		return null;
	}

	@Override
	public Object visitExpressionIdent(ExpressionIdent expressionIdent, Object arg) throws PLPException {
		methodVisitor = (MethodVisitor) arg;
		// 1. if variable is constant,just load the value
		if(expressionIdent.getDec() instanceof ConstDec constDec){
			methodVisitor.visitLdcInsn(constDec.val);
		}
		// 2. otherwise, load address of containing instance (follow the this$n chain if not local)
		// and use GETFIELD to load the value of the variable on top of the stack
		else {

			int index = expressionIdent.getNest() - 1;
			methodVisitor.visitVarInsn(ALOAD,0);
			String currentClass = currentFullyQualifiedClassName;
			// if local
			if(expressionIdent.getDec().getNest()==expressionIdent.getNest()){
				methodVisitor.visitFieldInsn(GETFIELD,currentClass,expressionIdent.getFirstToken().getStringValue(),expressionIdent.getType().getJvmType());
			}
			// if not local
			else if(index>=0){
				for(int i = index;i>=expressionIdent.getDec().getNest();i--){
					superCurrentClassName = findSuperClass(currentClass);
					superCurrentClassDesc = "L" + superCurrentClassName +";";
					methodVisitor.visitFieldInsn(GETFIELD,currentClass,"this$"+i,superCurrentClassDesc);
					currentClass = superCurrentClassName;
				}
				methodVisitor.visitFieldInsn(GETFIELD,superCurrentClassName,expressionIdent.getFirstToken().getStringValue(),expressionIdent.getType().getJvmType());
			}
		}
		return null;
	}

	@Override
	public Object visitExpressionNumLit(ExpressionNumLit expressionNumLit, Object arg) throws PLPException {
		methodVisitor = (MethodVisitor) arg;
		methodVisitor.visitLdcInsn(expressionNumLit.getFirstToken().getIntValue());
		return null;
	}

	@Override
	public Object visitExpressionStringLit(ExpressionStringLit expressionStringLit, Object arg) throws PLPException {
		methodVisitor = (MethodVisitor) arg;
		methodVisitor.visitLdcInsn(expressionStringLit.getFirstToken().getStringValue());
		return null;

	}

	@Override
	public Object visitExpressionBooleanLit(ExpressionBooleanLit expressionBooleanLit, Object arg) throws PLPException {
		methodVisitor = (MethodVisitor) arg;
		methodVisitor.visitLdcInsn(expressionBooleanLit.getFirstToken().getBooleanValue());
		return null;

	}

	@Override
	public Object visitStatementEmpty(StatementEmpty statementEmpty, Object arg) throws PLPException {
		return null;
	}

	@Override
	public Object visitIdent(Ident ident, Object arg) throws PLPException {
		methodVisitor = (MethodVisitor) arg;
		// found only on left side of assignments and input statement
		// 1. assume a value is on top of the stack and generate code to store the value in the variable indicated by the ident
		// 2. use nesting level to go up chain of this$n vars for non-local variable.
		methodVisitor.visitVarInsn(ALOAD,0);
		int index = ident.getNest() - 1;
		String currentClass = currentFullyQualifiedClassName;

		if(index>=0){
			for(int i = index;i>=ident.getDec().getNest();i--){
				superCurrentClassName = findSuperClass(currentClass);
				superCurrentClassDesc = "L" + superCurrentClassName +";";
				methodVisitor.visitFieldInsn(GETFIELD,currentClass,"this$"+i,superCurrentClassDesc);
				currentClass = superCurrentClassName;
			}
			methodVisitor.visitInsn(SWAP);

			methodVisitor.visitFieldInsn(PUTFIELD,currentClass,ident.firstToken.getStringValue(),ident.getDec().getType().getJvmType());

		}
		else{
			methodVisitor.visitInsn(SWAP);
			methodVisitor.visitFieldInsn(PUTFIELD,currentFullyQualifiedClassName,ident.firstToken.getStringValue(),ident.getDec().getType().getJvmType());
		}
		return null;
	}

}
