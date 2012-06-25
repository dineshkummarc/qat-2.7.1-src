package qat.gui;
/**
 *
 * @author Stephen Kruger
 * @version 2.3, 17 June 1999
 */

import java.lang.System;
import java.util.StringTokenizer;
/**
 * This static class is used to determine whether a given set of keywords satisfies
 * a boolean expression made up of these keywords.
 * Allowed values are '(', ')', '&', '|', '!' and whitespace characters.
 * @author Stephen Kruger
 * @version %W %E
 */
public class Evaluater {
	
	private static char TRUE_CHAR = '*';
	private static char FALSE_CHAR = '-';
	
	public Evaluater() {
	}
	
	/**
	 * returns true if the expression is satisfied by the supplied keys,
	 * else returns false.
	 */
	
	public static boolean evaluateExpression(String expression, String[] keys) {
		try {
			return (internalEvaluateExpression(preProcess(expression),keys));
		}
		catch (Throwable e) {
			System.out.println("Caught a parser exception - please check the keyword expression!!!");
			return false;
		}
	}
	
	private static String preProcess(String expr) {
		// remove any leading or trailing spaces
		expr = expr.trim();
		StringBuffer buff = new StringBuffer(expr.length());
		// remove any internal spaces
		char c;
		for (int i = 0; i < expr.length(); i++) {
			c = expr.charAt(i);
			if (c!=' ')
				buff = buff.append(c);
		}
		// replace any !! with plain nothing
			return remove("!!",buff.toString());
	}
	
	private static String remove(String patt,String s) {
		int pos;
		while ((pos = s.indexOf(patt))>=0) {
			s = s.substring(0,pos)+s.substring(pos+patt.length(),s.length());
		}
		return s;
	}
	
	private static boolean internalEvaluateExpression(String expression, String[] keys) {
		// if expression contains parenthesis, evaluate these recursively
		if (expression.indexOf('(')>=0)
			expression = evaluateSubExpression(expression,keys);
		StringTokenizer tokens = new StringTokenizer(expression,"&|");
		if ((tokens.hasMoreTokens()==false)|(keys.length==0))
			return false;
		char opparray[] = getOperators(expression);
		int operatorIndex=0;
		
		boolean exprResult = evaluatePortion(tokens.nextToken().trim(),keys);
		String s;
		while (tokens.hasMoreTokens()) {
			s = tokens.nextToken();
			if (opparray[operatorIndex]=='&') {
				exprResult = exprResult && evaluatePortion(s.trim(),keys);
			}
			else {
				exprResult = exprResult || evaluatePortion(s.trim(),keys);
			}
			operatorIndex++;
		}
		return exprResult;
	}
	
	/**
	 * This method replaces any parenthesis in an expression by TRUE_CHAR if the
	 * () evaluates to true, or FALSE_CHAR if it evaluates to false, and
	 * returns a new string with these chars instead of the orginal ()
	 * expression.
	 * eg. "a & (true | false)" would be return "a & TRUE_CHAR"
	 */
	private static String evaluateSubExpression(String expr, String keys[]) {
		int openParen = expr.indexOf('(')+1;
		int closeParen = getClosingParenIndex(expr,openParen);
		//System.out.println("Expr was :"+expr);
		String subExpression = expr.substring(openParen,closeParen);
		//System.out.println("Expr is now :"+expr);
		//System.out.println("====================");
		char resultChar;
		if (internalEvaluateExpression(subExpression,keys))
			resultChar = TRUE_CHAR;
		else
			resultChar = FALSE_CHAR;
		return expr.substring(0,openParen-1)+resultChar+expr.substring(closeParen+1,expr.length());
	}
	
	/**
	 * Returns the index of the closing parenthesis matching the opening parenthesis
	 * passed in the parameter openingParenPos.
	 */
	private static int getClosingParenIndex(String expr, int openingParenPos) {
		//System.out.println("Looking for closing paren to pos :"+openingParenPos+" in expr:"+expr);
		int parenthesisStack[] = new int[expr.length()];
		int stackTop = 0;
		for (int i = expr.length()-1; i >= openingParenPos; i--) {
			if (expr.charAt(i)==')') {
				parenthesisStack[stackTop]=i;
				stackTop++;
				//System.out.println("Closeparen found at pos:"+parenthesisStack[stackTop-1]);
			}
			else
				if (expr.charAt(i)=='(') {
					stackTop--;
					//System.out.println("Discarding a stack value");
				}
		}
		//System.out.println("Returning position:"+parenthesisStack[stackTop]+" stacktop="+stackTop);
		return parenthesisStack[stackTop-1];
	}
	
	private static boolean evaluatePortion(String portion, String[] keys) {
		if (portion.startsWith("!")) {
			if (portion.indexOf(TRUE_CHAR)>=0) {
				return false; // negated - return false
			}
			if (portion.indexOf(FALSE_CHAR)>=0) {
				return true; // negated - return true
			}
			return (!containsKey(portion,keys));
		}
		if (portion.indexOf(TRUE_CHAR)>=0) {
			return true;
		}
		if (portion.indexOf(FALSE_CHAR)>=0) {
			return false;
		}
		else {
			return (containsKey(portion,keys));
		}
	}
	
	private static boolean doubleNegation(String portion) {
		int count = 0;
		for (int i = 0; i < portion.length(); i++)
			if (portion.charAt(i)=='!')
				count++;
		System.out.println("Double negation :"+((count % 2)==0));
		return ((count % 2)!=0);
	}
	
	private static boolean containsKey(String token, String keys[]) {
		for (int i = 0; i < keys.length; i++) {
            // Call equals in place of indexOf to fix jqat.00020.B
			//if (token.indexOf(keys[i])>=0) {
			if ( token.equals(keys[i]) ) {
				return true;
			}
		}
		return false;
	}
	
	private static char[] getOperators(String expr) {
		char exprBuff[] = expr.toCharArray();
		int opCount = 0;
		for (int i = 0; i < exprBuff.length; i++) {
			if ((exprBuff[i]=='&')||
				(exprBuff[i]=='|')) {
				exprBuff[opCount] = exprBuff[i];
				opCount++;
			}
		}
		char oppArray[] = new char[opCount];
		System.arraycopy(exprBuff,0,oppArray,0,opCount);
		return oppArray;
	}
   
}
