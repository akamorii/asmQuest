package asmquest.vm;

import asmquest.model.Level;
import asmquest.model.LevelCatalog;

import java.util.List;

public final class VirtualMachineTest {
    private int checks;

    public static void main(String[] args) throws Exception {
        new VirtualMachineTest().run();
    }

    private void run() throws Exception {
        testMovArithmeticAndFlags();
        testMemoryAndSwap();
        testLoopAndConditionalJump();
        testCommentsAndInlineLabel();
        testErrorsHaveLineNumbers();
        testEveryLevelSolution();
        System.out.println("OK: " + checks + " проверок мини-ассемблера и уровней пройдено.");
    }

    private void testMovArithmeticAndFlags() throws Exception {
        VirtualMachine.Session session = VirtualMachine.compile(
                "MOV R0, 5\nADD R0, 3\nSUB R0, 8\nMOV R1, R0", emptyState());
        run(session, 10);
        equal(0, session.state().register(0), "SUB сохраняет результат");
        equal(0, session.state().register(1), "MOV копирует регистр");
        check(session.state().zeroFlag(), "Нулевой результат включает ZF");
        equal(4, session.state().steps(), "Счётчик шагов");
        check(session.state().halted(), "Конец программы останавливает процессор");
    }

    private void testMemoryAndSwap() throws Exception {
        MachineState initial = new MachineState(new int[]{1, 3, 0, 0}, new int[8]);
        VirtualMachine.Session session = VirtualMachine.compile(
                "MOV [0], R0\nMOV R0, R1\nMOV R1, [0]", initial);
        run(session, 10);
        equal(3, session.state().register(0), "Обмен R0");
        equal(1, session.state().register(1), "Обмен R1");
        equal(1, session.state().memory(0), "Временная память");
    }

    private void testLoopAndConditionalJump() throws Exception {
        VirtualMachine.Session session = VirtualMachine.compile(
                "MOV R2, 3\nloop:\nINC R0\nDEC R2\nJNZ loop\nCMP R2, 0\nJZ done\nMOV R0, 99\ndone: HLT",
                emptyState());
        run(session, 30);
        equal(3, session.state().register(0), "Цикл выполняется трижды");
        equal(0, session.state().register(2), "Счётчик дошёл до нуля");
        equal(13, session.state().steps(), "Условные переходы выполняют верное число шагов");
    }

    private void testCommentsAndInlineLabel() throws Exception {
        VirtualMachine.Session session = VirtualMachine.compile(
                "; комментарий\nstart: MOV R0, 2 # тоже комментарий\nJMP start", emptyState());
        session.step();
        equal(2, session.state().register(0), "Метка и команда в одной строке");
        session.step();
        equal(0, session.state().programCounter(), "JMP возвращается к метке");
    }

    private void testErrorsHaveLineNumbers() throws Exception {
        AsmException unknown = expectError("MOV R0, 1\nBOOM R0, 2");
        equal(2, unknown.line(), "Строка неизвестной команды");
        AsmException register = expectError("MOV R8, 1");
        equal(1, register.line(), "Неверный регистр");
        AsmException memory = expectError("MOV [8], 1");
        equal(1, memory.line(), "Неверный адрес памяти");
        AsmException label = expectError("JMP missing");
        equal(1, label.line(), "Отсутствующая метка");
        AsmException operands = expectError("ADD R0");
        equal(1, operands.line(), "Недостаточно операндов");
    }

    private void testEveryLevelSolution() throws Exception {
        List<Level> levels = LevelCatalog.all();
        equal(8, levels.size(), "В игре восемь уровней");
        for (int i = 0; i < levels.size(); i++) {
            Level level = levels.get(i);
            MachineState initial = new MachineState(level.initialRegisters(), level.initialMemory());
            VirtualMachine.Session session = VirtualMachine.compile(level.solution(), initial);
            run(session, 100);
            check(level.isSolved(session.state()), "Решение уровня " + (i + 1));
            check(session.state().steps() <= level.par(), "Решение уровня " + (i + 1) + " укладывается в par");
        }
    }

    private AsmException expectError(String source) throws Exception {
        try {
            VirtualMachine.compile(source, emptyState());
            throw new AssertionError("Ожидалась ошибка компиляции: " + source);
        } catch (AsmException exception) {
            checks++;
            return exception;
        }
    }

    private static MachineState emptyState() {
        return new MachineState(new int[4], new int[8]);
    }

    private static void run(VirtualMachine.Session session, int maxSteps) throws Exception {
        int guard = 0;
        while (!session.isFinished() && guard++ < maxSteps) {
            session.step();
        }
        if (guard >= maxSteps) {
            throw new AssertionError("Программа не завершилась за " + maxSteps + " шагов");
        }
    }

    private void equal(int expected, int actual, String message) {
        if (expected != actual) {
            throw new AssertionError(message + ": ожидалось " + expected + ", получено " + actual);
        }
        checks++;
    }

    private void check(boolean condition, String message) {
        if (!condition) {
            throw new AssertionError(message);
        }
        checks++;
    }
}
