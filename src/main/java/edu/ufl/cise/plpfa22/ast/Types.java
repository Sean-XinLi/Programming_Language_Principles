/**  This code is provided for solely for use of students in the course COP5556 Programming Language Principles at the 
 * University of Florida during the Fall Semester 2022 as part of the course project.  No other use is authorized. 
 */

package edu.ufl.cise.plpfa22.ast;

import edu.ufl.cise.plpfa22.IToken;

public class Types {
	
	public static enum Type {
		NUMBER("I"),
		BOOLEAN("Z"),
		STRING("Ljava/lang/String;"),
		PROCEDURE(null);


		String jvmType;
		Type(String jvmType) {
			this.jvmType = jvmType;
		}

		public boolean isType(Types.Type type1, Types.Type type2){ return type1 == type2;}
		public boolean isType(Types.Type type1, Types.Type ...types){
			for(Types.Type type: types){
				if(type==type1){
					return true;
				}
			}
			return false;
		}
		public String getJvmType(){
			return jvmType;
		}
	};


}
