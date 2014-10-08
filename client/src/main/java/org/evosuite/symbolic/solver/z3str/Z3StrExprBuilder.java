package org.evosuite.symbolic.solver.z3str;

import java.text.DecimalFormat;

abstract class Z3StrExprBuilder {

	public static String mkNot(String formula) {
		return "(not " + formula + ")";
	}

	public static String mkEq(String left, String right) {
		return "(= " + left + " " + right + ")";
	}

	public static String mkGe(String left, String right) {
		return "(>= " + left + " " + right + ")";
	}

	public static String mkGt(String left, String right) {
		return "(> " + left + " " + right + ")";
	}

	public static String mkLe(String left, String right) {
		return "(<= " + left + " " + right + ")";
	}

	public static String mkLt(String left, String right) {
		return "(< " + left + " " + right + ")";
	}

	public static String mkBVASHR(String bv_left, String bv_right) {
		return "(bvashr " + bv_left + " " + bv_right + ")";
	}

	public static String mkBVLSHR(String bv_left, String bv_right) {
		return "(bvlshr " + bv_left + " " + bv_right + ")";
	}

	public static String mkBVSHL(String bv_left, String bv_right) {
		return "(bvshl " + bv_left + " " + bv_right + ")";
	}

	public static String mkBVXOR(String bv_left, String bv_right) {
		return "(bvxor " + bv_left + " " + bv_right + ")";
	}

	public static String mkBVAND(String bv_left, String bv_right) {
		return "(bvand " + bv_left + " " + bv_right + ")";
	}

	public static String mkBV2Int(String bv_expr, boolean b) {
		return "(bv2int " + bv_expr + ")";
	}

	public static String mkBVOR(String bv_left, String bv_right) {
		return "(bvor " + bv_left + " " + bv_right + ")";
	}

	public static String mkInt2BV(int bitwidth, String intExpr) {
		return "((_ int2bv " + bitwidth + ")" + intExpr + ")";
	}

	public static String mkRealDiv(String left, String right) {
		return "(/ " + left + " " + right + ")";
	}

	public static String mkDiv(String left, String right) {
		return "(div " + left + " " + right + ")";
	}

	public static String mkRem(String left, String right) {
		return "(rem " + left + " " + right + ")";
	}

	public static String mkAdd(String left, String right) {
		return "(+ " + left + " " + right + ")";
	}

	public static String mkSub(String left, String right) {
		return "(- " + left + " " + right + ")";
	}

	public static String mkMul(String left, String right) {
		return "(* " + left + " " + right + ")";
	}

	public static String mkITE(String condition, String thenExpr,
			String elseExpr) {
		return "(ite " + condition + " " + thenExpr + " " + elseExpr + ")";
	}

	public static String mkToReal(String operand) {
		return "(to_real " + operand + ")";
	}

	public static String mkStringLiteral(String str) {
		return "\"" + str + "\"";
	}

	public static String mkNeg(String intExpr) {
		return "(- " + intExpr + ")";
	}

	public static String mkAssert(String constraintStr) {
		return "(assert " + constraintStr + ")";
	}

	public static String mkStringVariable(String varName) {
		return "(declare-const " + varName + " String)";
	}

	public static String mkRealVariable(String varName) {
		return "(declare-const " + varName + " Real)";
	}

	public static String mkIntVariable(String varName) {
		return "(declare-const " + varName + " Int)";
	}

	private static DecimalFormat DECIMAL_FORMAT = new DecimalFormat(
			"################.################");

	public static String mkRealConstant(double doubleVal) {
		if (doubleVal < 0) {
			String magnitudeStr = DECIMAL_FORMAT.format(Math.abs(doubleVal));
			return "(- " + magnitudeStr + ")";
		} else {
			String doubleStr = DECIMAL_FORMAT.format(doubleVal);
			return doubleStr;
		}
	}

	public static String mkIntegerConstant(long longVal) {
		if (longVal < 0) {
			return "(- " + Long.toString(Math.abs(longVal)) + ")";
		} else {
			String longStr = Long.toString(longVal);
			return longStr;
		}
	}

	public static String mkReal2Int(String operandStr) {
		return "(to_int " + operandStr + ")";
	}

	public static String mkStringEndsWith(String left, String right) {
		return "(EndsWith " + left + " " + right + ")";
	}

	public static String mkStringContains(String left, String right) {
		return "(Contains " + left + " " + right + ")";
	}

	public static String mkStringStartsWith(String left, String right) {
		return "(StartsWith " + left + " " + right + ")";
	}

	public static String mkStringConstant(String str) {
		return " " + encodeString(str) + " ";
	}

	public static String mkStringIndexOf(String left, String right) {
		return "(Indexof " + left + " " + right + ")";
	}

	public static String mkStringSubstring(String str, String from, String to) {
		return "(Substring " + str + " " + from + " " + to + ")";
	}

	public static String mkStringReplace(String str, String oldValue,
			String newValue) {
		return "(Substring " + str + " " + oldValue + " " + newValue + ")";
	}

	public static String mkStringLength(String str) {
		return "(Length " + str + ")";
	}

	public static String mkStringConcat(String left, String right) {
		return "(Concat " + left + " " + right + ")";
	}

	public static String encodeString(String str) {
		char[] charArray = str.toCharArray();
		String ret_val = "__cOnStStR_";
		for (int i = 0; i < charArray.length; i++) {
			char c = charArray[i];
			if (c >= 0 && c <= 255) {
				ret_val += "_x" + Integer.toHexString(c);
			}
		}
		return ret_val;
	}

}