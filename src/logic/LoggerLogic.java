package logic;

import java.lang.StackWalker.StackFrame;

import annotations.Log;


public class LoggerLogic {
    public static void print(Log log, StackFrame frame, String value) {
        if (log.logError() && value.startsWith("Err")) {
            System.out.printf("[%d] %s() -> %s \n", frame.getLineNumber(), frame.getMethodName(),
                    value.toString());
        } else if (log.logOk() && value.startsWith("Ok")) {
            System.out.printf("[%d] %s() -> %s \n", frame.getLineNumber(), frame.getMethodName(),
                    value.toString());
        }
    }
}
