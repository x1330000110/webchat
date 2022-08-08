package com.socket.webchat.util;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Arrays;

/**
 * 计算器基本实用程序
 */
public class Calculator {
    /**
     * 标准高精度数学算式计算 <br>
     * 支持的运算符请参考统一枚举
     *
     * @param math 算式
     * @return 计算结果
     * @throws ArithmeticException 计算错误
     * @see Symbol
     * @see Brackets
     * @see BigDecimal
     */
    public String calculate(String math) {
        StringBuilder sb = new StringBuilder(math);
        for (Brackets brackets : Brackets.values()) {
            int left, right;
            while ((left = sb.indexOf(brackets.left)) > -1 && (right = sb.indexOf(brackets.right)) > -1) {
                sb.replace(left, right + 1, this.baseCalculate(sb.substring(left + 1, right)));
            }
        }
        return this.baseCalculate(sb.toString());
    }

    /**
     * 基本算式计算
     *
     * @param math 算式
     * @return 计算结果
     */
    private String baseCalculate(String math) {
        StringBuilder sb = new StringBuilder(math);
        for (Priority priority : Priority.values()) {
            // 查找符号：从第二个字符遍历（第一个字符必然不是符号）
            for (int i = 1; i < sb.length(); i++) {
                Symbol symbol = this.getSymbol(sb.charAt(i), sb.charAt(i - 1), priority);
                if (symbol != null) {
                    int prevNumStartIndex = this.prevNumStartIndex(sb, i);
                    int nextSymbol = this.nextSymbol(sb, i);
                    BigDecimal prev = new BigDecimal(sb.substring(prevNumStartIndex, i));
                    BigDecimal next = new BigDecimal(sb.substring(i + 1, nextSymbol));
                    String result = this.toDecimalString(this.coreCalculate(prev, symbol, next));
                    sb.replace(prevNumStartIndex, nextSymbol, result);
                    // 变更指针
                    i = prevNumStartIndex;
                }
            }
        }
        return sb.toString();
    }

    /**
     * {@link BigDecimal} 计算处理方法
     *
     * @param prev   操作数
     * @param symbol 运算符
     * @param next   被操作数
     * @return 运算结果
     */
    private BigDecimal coreCalculate(BigDecimal prev, Symbol symbol, BigDecimal next) {
        switch (symbol) {
            case ADD:
                return prev.add(next);
            case SUBTRACT:
                return prev.subtract(next);
            case MULTIPLY:
                return prev.multiply(next);
            case DIVIDE:
                return prev.divide(next, 9, RoundingMode.HALF_UP);
            case REMAINDER:
                return prev.divideAndRemainder(next)[1];
            case POWER:
                return prev.pow(next.intValue());
            default:
                throw new IllegalArgumentException();
        }
    }

    /**
     * 获取上个数字起始下标
     *
     * @param sb    算式
     * @param index 下标
     * @return 若遍历到开始位置返回0
     */
    private int prevNumStartIndex(StringBuilder sb, int index) {
        for (int i = index - 1; i >= 0; i--) {
            if (this.isSymbol(sb, i)) {
                return i + 1;
            }
        }
        return 0;
    }

    /**
     * 获取下一个运算符位置
     *
     * @param sb    算式
     * @param index 下标
     * @return 若遍历到结尾位置返回总长度
     */
    private int nextSymbol(StringBuilder sb, int index) {
        for (int i = index + 1; i < sb.length(); i++) {
            if (this.isSymbol(sb, i)) {
                return i;
            }
        }
        return sb.length();
    }

    /**
     * 检查当前位置下标是否为运算符
     *
     * @param sb    算式
     * @param index 下标
     * @return 是否为运算符
     */
    private boolean isSymbol(StringBuilder sb, int index) {
        return index > 0 && this.getSymbol(sb.charAt(index), sb.charAt(index - 1), null) != null;
    }

    /**
     * 尝试将指定字符转为运算符
     *
     * @param c        字符
     * @param prev     字符前一位
     * @param priority 优先级
     * @return {@link Symbol}
     */
    private Symbol getSymbol(char c, char prev, Priority priority) {
        Symbol symbol = Symbol.of(c);
        // 识别减号与负号
        if (symbol == null || symbol == Symbol.SUBTRACT && !this.isNumber(prev)) {
            return null;
        }
        // 优先级匹配
        if (priority != null && symbol.priority != priority) {
            return null;
        }
        return symbol;
    }

    /**
     * 检查当前字符是否为数字
     *
     * @param c 字符
     * @return 是否为数字
     */
    private boolean isNumber(char c) {
        return c >= '0' && c <= '9';
    }

    /**
     * 返回{@link BigDecimal} 常规表示形式
     *
     * @param decimal {@link BigDecimal}
     * @return 常规形式
     */
    private String toDecimalString(BigDecimal decimal) {
        return decimal.stripTrailingZeros().toEngineeringString();
    }

    /**
     * 标准的运算符枚举 <br>
     * 支持 + - * &#47; ^ % 运算符号
     */
    enum Symbol {
        /**
         * 算术运算 加法
         */
        ADD('+', Priority.ADD_SUBTRACT),
        /**
         * 算术运算 减法
         */
        SUBTRACT('-', Priority.ADD_SUBTRACT),
        /**
         * 算术运算 乘法
         */
        MULTIPLY('*', Priority.MULTIPLY_DIVIDE),
        /**
         * 算术运算 除法
         */
        DIVIDE('/', Priority.MULTIPLY_DIVIDE),
        /**
         * 算数运算 乘方
         */
        POWER('^', Priority.POWER),
        /**
         * 算术运算 除余
         */
        REMAINDER('%', Priority.REMAINDER);

        private final Priority priority;
        private final char symbol;

        Symbol(char symbol, Priority priority) {
            this.symbol = symbol;
            this.priority = priority;
        }

        /**
         * 尝试匹配运算符枚举
         *
         * @param c 字符
         * @return {@link Symbol}
         */
        public static Symbol of(char c) {
            return Arrays.stream(values()).filter(e -> e.symbol == c).findFirst().orElse(null);
        }
    }

    /**
     * 运算优先级统一枚举
     */
    enum Priority {
        /**
         * 乘方
         */
        POWER,
        /**
         * 除余
         */
        REMAINDER,
        /**
         * 乘除法
         */
        MULTIPLY_DIVIDE,
        /**
         * 加减法
         */
        ADD_SUBTRACT
    }

    /**
     * 标准算式括号枚举 <br>
     * 支持 ( ) [ ] { } 字符
     */
    enum Brackets {
        /**
         * 小括号
         */
        SMALL("(", ")"),
        /**
         * 中括号
         */
        MEDIUM("[", "]"),
        /**
         * 大括号
         */
        BIG("{", "}");

        private final String left;
        private final String right;

        Brackets(String left, String right) {
            this.left = left;
            this.right = right;
        }
    }
}
