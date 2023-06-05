@@ -14,9 +14,19 @@

import java.io.*;
import java.util.*;
import java.util.regex.Pattern;

import exception.ImcompatibleTypeError;
import exception.OperatorError;
import type.ArrayType;
import type.BaseType;
import type.CharType;
import type.FloatType;
import type.IntType;

class Machine {
  public static void main(String[] args) throws FileNotFoundException, IOException {
  public static void main(String[] args)
      throws FileNotFoundException, IOException, ImcompatibleTypeError, OperatorError {
    if (args.length == 0)
      System.out.println("Usage: java Machine <programfile> <arg1> ...\n");
    else
@@ -33,12 +43,26 @@ public static void main(String[] args) throws FileNotFoundException, IOException

  // Read code from file and execute it

  static void execute(String[] args, boolean trace) throws FileNotFoundException, IOException {
  static void execute(String[] args, boolean trace)
      throws FileNotFoundException, IOException, ImcompatibleTypeError, OperatorError {
    int[] p = readfile(args[0]); // Read the program from file
    int[] s = new int[STACKSIZE]; // The evaluation stack
    int[] iargs = new int[args.length - 1];
    BaseType[] s = new BaseType[STACKSIZE]; // The evaluation stack
    BaseType[] iargs = new BaseType[args.length - 1];
    for (int i = 1; i < args.length; i++) // Push commandline arguments
      iargs[i - 1] = Integer.parseInt(args[i]);
    {
      if (Pattern.compile("(?i)[a-z]").matcher(args[i]).find()) {
        char[] input = args[i].toCharArray();
        CharType[] array = new CharType[input.length];
        for (int j = 0; j < input.length; ++j) {
          array[j] = new CharType(input[j]);
        }
        iargs[i - 1] = new ArrayType(array);
      } else if (args[i].contains(".")) {
        iargs[i - 1] = new FloatType(Float.valueOf(args[i]).floatValue());
      } else {
        iargs[i - 1] = new IntType(Integer.valueOf(args[i]).intValue());
      }
    }
    long starttime = System.currentTimeMillis();
    execcode(p, s, iargs, trace); // Execute program proper
    long runtime = System.currentTimeMillis() - starttime;
@@ -47,77 +71,103 @@ static void execute(String[] args, boolean trace) throws FileNotFoundException,

  // The machine: execute the code starting at p[pc]

  static int execcode(int[] p, int[] s, int[] iargs, boolean trace) {
  static int execcode(int[] p, BaseType[] s, BaseType[] iargs, boolean trace)
      throws ImcompatibleTypeError, OperatorError {
    int bp = -999; // Base pointer, for local variable access
    int sp = -1; // Stack top pointer
    int pc = 0; // Program counter: next instruction
    int hr = -1;
    for (;;) {
      if (trace)
        printsppc(s, bp, sp, p, pc);
      switch (p[pc++]) {
        case CSTI:
          s[sp + 1] = p[pc++];
          s[sp + 1] = new IntType(p[pc++]);
          sp++;
          break;
        case CSTF:
          s[sp + 1] = p[pc++];
          s[sp + 1] = new FloatType(Float.intBitsToFloat(p[pc++]));
          sp++;
          break;
        case ADD:
          s[sp - 1] = s[sp - 1] + s[sp];
          s[sp - 1] = binaryOperator(s[sp - 1], s[sp], "+");
          sp--;
          break;
        case SUB:
          s[sp - 1] = s[sp - 1] - s[sp];
          s[sp - 1] = binaryOperator(s[sp - 1], s[sp], "-");
          sp--;
          break;
        case MUL:
          s[sp - 1] = s[sp - 1] * s[sp];
          s[sp - 1] = binaryOperator(s[sp - 1], s[sp], "*");
          sp--;
          break;
        case DIV:
          s[sp - 1] = s[sp - 1] / s[sp];
          sp--;
          if (((IntType) s[sp]).getValue() == 0) {
            System.out.println("hr:" + hr + " exception:" + 1);
            while (hr != -1 && ((IntType) s[hr]).getValue() != 1) {
              hr = ((IntType) s[hr + 2]).getValue();
              System.out.println("hr:" + hr + " exception:" + new IntType(p[pc]).getValue());
            }

            if (hr != -1) {
              sp = hr - 1;
              pc = ((IntType) s[hr + 1]).getValue();
              hr = ((IntType) s[hr + 2]).getValue();
            } else {
              System.out.print(hr + "not find exception");
              return sp;
            }
          } else {
            s[sp - 1] = binaryOperator(s[sp - 1], s[sp], "/");
            sp--;
          }
          break;
        case MOD:
          s[sp - 1] = s[sp - 1] % s[sp];
          s[sp - 1] = binaryOperator(s[sp - 1], s[sp], "+");
          sp--;
          break;
        case EQ:
          s[sp - 1] = (s[sp - 1] == s[sp] ? 1 : 0);
          s[sp - 1] = binaryOperator(s[sp - 1], s[sp], "==");
          sp--;
          break;
        case LT:
          s[sp - 1] = (s[sp - 1] < s[sp] ? 1 : 0);
          s[sp - 1] = binaryOperator(s[sp - 1], s[sp], "<");
          sp--;
          break;
        case NOT:
          s[sp] = (s[sp] == 0 ? 1 : 0);
        case NOT: {
          Object result = null;
          if (s[sp] instanceof FloatType) {
            result = ((FloatType) s[sp]).getValue();
          } else if (s[sp] instanceof IntType) {
            result = ((IntType) s[sp]).getValue();
          }
          s[sp] = (Float.compare(Float.valueOf(result.toString()), 0.0f) == 0 ? new IntType(1) : new IntType(0));
          break;
        }
        case DUP:
          s[sp + 1] = s[sp];
          sp++;
          break;
        case SWAP: {
          int tmp = s[sp];
          BaseType tmp = s[sp];
          s[sp] = s[sp - 1];
          s[sp - 1] = tmp;
        }
          break;
        case LDI: // load indirect
          s[sp] = s[s[sp]];
          s[sp] = s[((IntType) s[sp]).getValue()];
          break;
        case STI: // store indirect, keep value on top
          s[s[sp - 1]] = s[sp];
          s[((IntType) s[sp - 1]).getValue()] = s[sp];
          s[sp - 1] = s[sp];
          sp--;
          break;
        case GETBP:
          s[sp + 1] = bp;
          s[sp + 1] = new IntType(bp);
          sp++;
          break;
        case GETSP:
          s[sp + 1] = sp;
          s[sp + 1] = new IntType(sp);
          sp++;
          break;
        case INCSP:
@@ -126,19 +176,35 @@ static int execcode(int[] p, int[] s, int[] iargs, boolean trace) {
        case GOTO:
          pc = p[pc];
          break;
        case IFZERO:
          pc = (s[sp--] == 0 ? p[pc] : pc + 1);
        case IFZERO: {
          Object result = null;
          int index = sp--;
          if (s[index] instanceof IntType) {
            result = ((IntType) s[index]).getValue();
          } else if (s[index] instanceof FloatType) {
            result = ((FloatType) s[index]).getValue();
          }
          pc = (Float.compare(Float.valueOf(result.toString()), 0.0f) == 0 ? p[pc] : pc + 1);
          break;
        case IFNZRO:
          pc = (s[sp--] != 0 ? p[pc] : pc + 1);
        }
        case IFNZRO: {
          Object result = null;
          int index = sp--;
          if (s[index] instanceof IntType) {
            result = ((IntType) s[index]).getValue();
          } else if (s[index] instanceof FloatType) {
            result = ((FloatType) s[index]).getValue();
          }
          pc = (Float.compare(Float.valueOf(result.toString()), 0.0f) != 0 ? p[pc] : pc + 1);
          break;
        }
        case CALL: {
          int argc = p[pc++];
          for (int i = 0; i < argc; i++) // Make room for return address
            s[sp - i + 2] = s[sp - i]; // and old base pointer
          s[sp - argc + 1] = pc + 1;
          s[sp - argc + 1] = new IntType(pc + 1);
          sp++;
          s[sp - argc + 1] = bp;
          s[sp - argc + 1] = new IntType(bp);
          sp++;
          bp = sp + 1 - argc;
          pc = p[pc];
@@ -154,18 +220,27 @@ static int execcode(int[] p, int[] s, int[] iargs, boolean trace) {
        }
          break;
        case RET: {
          int res = s[sp];
          BaseType res = s[sp];
          sp = sp - p[pc];
          bp = s[--sp];
          pc = s[--sp];
          bp = ((IntType) s[--sp]).getValue();
          pc = ((IntType) s[--sp]).getValue();
          s[sp] = res;
        }
          break;
        case PRINTI:
          System.out.print(s[sp] + " ");
        case PRINTI: {
          Object result;
          if (s[sp] instanceof IntType) {
            result = ((IntType) s[sp]).getValue();
          } else if (s[sp] instanceof FloatType) {
            result = ((FloatType) s[sp]).getValue();
          } else {
            result = ((CharType) s[sp]).getValue();
          }
          System.out.print(String.valueOf(result) + " ");
          break;
        }
        case PRINTC:
          System.out.print((char) (s[sp]));
          System.out.print((((CharType) s[sp])).getValue());
          break;
        case LDARGS:
          for (int i = 0; i < iargs.length; i++) // Push commandline arguments
@@ -179,14 +254,118 @@ static int execcode(int[] p, int[] s, int[] iargs, boolean trace) {
    }
  }

  public static BaseType binaryOperator(BaseType lhs, BaseType rhs, String operator)
      throws ImcompatibleTypeError, OperatorError {
    Object left;
    Object right;
    int flag = 0;
    if (lhs instanceof FloatType) {
      left = ((FloatType) lhs).getValue();
      flag = 1;
    } else if (lhs instanceof IntType) {
      left = ((IntType) lhs).getValue();
    } else {
      throw new ImcompatibleTypeError("ImcompatibleTypeError: Left type is not int or float");
    }

    if (rhs instanceof FloatType) {
      right = ((FloatType) rhs).getValue();
      flag = 1;
    } else if (rhs instanceof IntType) {
      right = ((IntType) rhs).getValue();
    } else {
      throw new ImcompatibleTypeError("ImcompatibleTypeError: Right type is not int or float");
    }
    BaseType result = null;

    switch (operator) {
      case "+": {
        if (flag == 1) {
          result = new FloatType(Float.parseFloat(String.valueOf(left)) + Float.parseFloat(String.valueOf(right)));
        } else {
          result = new IntType(Integer.parseInt(String.valueOf(left)) + Integer.parseInt(String.valueOf(right)));
        }
        break;
      }
      case "-": {
        if (flag == 1) {
          result = new FloatType(Float.parseFloat(String.valueOf(left)) - Float.parseFloat(String.valueOf(right)));
        } else {
          result = new IntType(Integer.parseInt(String.valueOf(left)) - Integer.parseInt(String.valueOf(right)));
        }
        break;
      }
      case "*": {
        if (flag == 1) {
          result = new FloatType(Float.parseFloat(String.valueOf(left)) * Float.parseFloat(String.valueOf(right)));
        } else {
          result = new IntType(Integer.parseInt(String.valueOf(left)) * Integer.parseInt(String.valueOf(right)));
        }
        break;
      }
      case "/": {
        if (Float.compare(Float.parseFloat(String.valueOf(right)), 0.0f) == 0) {
          throw new OperatorError("OpeatorError: Divisor can't not be zero");
        }
        if (flag == 1) {
          result = new FloatType(Float.parseFloat(String.valueOf(left)) / Float.parseFloat(String.valueOf(right)));
        } else {
          result = new IntType(Integer.parseInt(String.valueOf(left)) / Integer.parseInt(String.valueOf(right)));
        }
        break;
      }
      case "%": {
        if (flag == 1) {
          throw new OperatorError("OpeatorError: Float can't mod");
        } else {
          result = new IntType(Integer.parseInt(String.valueOf(left)) % Integer.parseInt(String.valueOf(right)));
        }
        break;
      }
      case "==": {
        if (flag == 1) {
          if ((float) left == (float) right) {
            result = new IntType(1);
          } else {
            result = new IntType(0);
          }
        } else {
          if ((int) left == (int) right) {
            result = new IntType(1);
          } else {
            result = new IntType(0);
          }
        }
        break;
      }
      case "<": {
        if (flag == 1) {
          if ((float) left < (float) right) {
            result = new IntType(1);
          } else {
            result = new IntType(0);
          }
        } else {
          if ((int) left < (int) right) {
            result = new IntType(1);
          } else {
            result = new IntType(0);
          }
        }
        break;
      }
    }
    return result;
  }

  // Print the stack machine instruction at p[pc]

  static String insname(int[] p, int pc) {
    switch (p[pc]) {
      case CSTI:
        return "CSTI " + p[pc + 1];
      case CSTF:
        return "CSTF " + p[pc + 1];
        return "CSTF " + Float.intBitsToFloat(p[pc + 1]);
      case ADD:
        return "ADD";
      case SUB:
@@ -244,10 +423,19 @@ static String insname(int[] p, int pc) {

  // Print current stack and current instruction

  static void printsppc(int[] s, int bp, int sp, int[] p, int pc) {
  static void printsppc(BaseType[] s, int bp, int sp, int[] p, int pc) {
    System.out.print("[ ");
    for (int i = 0; i <= sp; i++)
      System.out.print(s[i] + " ");
    for (int i = 0; i <= sp; i++) {
      Object result = null;
      if (s[i] instanceof IntType) {
        result = ((IntType) s[i]).getValue();
      } else if (s[i] instanceof FloatType) {
        result = ((FloatType) s[i]).getValue();
      } else if (s[i] instanceof CharType) {
        result = ((CharType) s[i]).getValue();
      }
      System.out.print(String.valueOf(result) + " ");
    }
    System.out.print("]");
    System.out.println("{" + pc + ": " + insname(p, pc) + "}");
  }
@@ -276,7 +464,8 @@ public static int[] readfile(String filename) throws FileNotFoundException, IOEx
// Run the machine with tracing: print each instruction as it is executed

class Machinetrace {
  public static void main(String[] args) throws FileNotFoundException, IOException {
  public static void main(String[] args)
      throws FileNotFoundException, IOException, ImcompatibleTypeError, OperatorError {
    if (args.length == 0)
      System.out.println("Usage: java Machinetrace <programfile> <arg1> ...\n");
    else
