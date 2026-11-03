package asmquest.vm;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/** A small, safe teaching assembler with four registers and eight memory cells. */
public final class VirtualMachine {
    private VirtualMachine() {
    }

    public static Session compile(String source, MachineState initialState) throws AsmException {
        List<Instruction> instructions = new ArrayList<>();
        Map<String, Integer> labels = new HashMap<>();
        String[] lines = source.split("\\R", -1);

        for (int i = 0; i < lines.length; i++) {
            int lineNumber = i + 1;
            String line = stripComment(lines[i]).trim();
            if (line.isEmpty()) {
                continue;
            }

            while (true) {
                int colon = line.indexOf(':');
                if (colon < 0) {
                    break;
                }
                String label = line.substring(0, colon).trim();
                if (!label.matches("[A-Za-z_][A-Za-z0-9_]*")) {
                    throw new AsmException("Некорректная метка: " + label, lineNumber);
                }
                String key = label.toLowerCase(Locale.ROOT);
                if (labels.putIfAbsent(key, instructions.size()) != null) {
                    throw new AsmException("Метка уже объявлена: " + label, lineNumber);
                }
                line = line.substring(colon + 1).trim();
            }
            if (line.isEmpty()) {
                continue;
            }

            String mnemonic;
            String arguments;
            int space = line.indexOf(' ');
            if (space < 0) {
                mnemonic = line;
                arguments = "";
            } else {
                mnemonic = line.substring(0, space);
                arguments = line.substring(space + 1).trim();
            }

            Op op;
            try {
                op = Op.valueOf(mnemonic.toUpperCase(Locale.ROOT));
            } catch (IllegalArgumentException ex) {
                throw new AsmException("Неизвестная команда: " + mnemonic, lineNumber);
            }

            String[] parts = arguments.isEmpty() ? new String[0] : arguments.split("\\s*,\\s*", -1);
            if (parts.length != op.arity) {
                throw new AsmException(op + ": ожидается операндов: " + op.arity
                        + ", получено: " + parts.length, lineNumber);
            }
            Operand a = parts.length > 0 ? parseOperand(parts[0], lineNumber) : null;
            Operand b = parts.length > 1 ? parseOperand(parts[1], lineNumber) : null;
            validate(op, a, b, lineNumber);
            instructions.add(new Instruction(op, a, b, lineNumber));
        }

        for (Instruction instruction : instructions) {
            if (instruction.op.isJump && !labels.containsKey(instruction.a.text.toLowerCase(Locale.ROOT))) {
                throw new AsmException("Метка не найдена: " + instruction.a.text, instruction.line);
            }
        }
        if (instructions.isEmpty()) {
            throw new AsmException("Программа пуста. Добавь хотя бы одну команду.", 1);
        }
        return new Session(instructions, labels, initialState);
    }

    private static String stripComment(String line) {
        int semicolon = line.indexOf(';');
        int hash = line.indexOf('#');
        int end = semicolon < 0 ? hash : hash < 0 ? semicolon : Math.min(semicolon, hash);
        return end < 0 ? line : line.substring(0, end);
    }

    private static Operand parseOperand(String text, int line) throws AsmException {
        String value = text.trim();
        if (value.matches("(?i)R[0-3]")) {
            return new Operand(Kind.REGISTER, Integer.parseInt(value.substring(1)), value);
        }
        if (value.matches("\\[[0-7]\\]")) {
            return new Operand(Kind.MEMORY, Integer.parseInt(value.substring(1, value.length() - 1)), value);
        }
        if (value.matches("-?\\d+")) {
            try {
                return new Operand(Kind.IMMEDIATE, Integer.parseInt(value), value);
            } catch (NumberFormatException ex) {
                throw new AsmException("Слишком большое число: " + value, line);
            }
        }
        if (value.matches("[A-Za-z_][A-Za-z0-9_]*")) {
            return new Operand(Kind.LABEL, 0, value);
        }
        if (value.matches("(?i)R\\d+")) {
            throw new AsmException("Доступны только регистры R0–R3", line);
        }
        if (value.matches("\\[\\d+\\]")) {
            throw new AsmException("Доступны только ячейки памяти [0]–[7]", line);
        }
        throw new AsmException("Некорректный операнд: " + value, line);
    }

    private static void validate(Op op, Operand a, Operand b, int line) throws AsmException {
        switch (op) {
            case MOV -> {
                require(a.kind == Kind.REGISTER || a.kind == Kind.MEMORY,
                        "MOV: приёмником может быть регистр или [память]", line);
                require(isReadable(b), "MOV: некорректный источник", line);
                require(!(a.kind == Kind.MEMORY && b.kind == Kind.MEMORY),
                        "MOV: копирование [память] → [память] не поддерживается", line);
            }
            case ADD, SUB -> {
                require(a.kind == Kind.REGISTER, op + ": первый операнд должен быть регистром", line);
                require(isReadable(b), op + ": некорректный второй операнд", line);
            }
            case INC, DEC -> require(a.kind == Kind.REGISTER,
                    op + ": операнд должен быть регистром", line);
            case CMP -> {
                require(a.kind == Kind.REGISTER, "CMP: первый операнд должен быть регистром", line);
                require(isReadable(b), "CMP: некорректный второй операнд", line);
            }
            case JMP, JZ, JNZ -> require(a.kind == Kind.LABEL,
                    op + ": укажи метку перехода", line);
            case HLT -> {
                // No operands.
            }
        }
    }

    private static boolean isReadable(Operand operand) {
        return operand.kind == Kind.REGISTER || operand.kind == Kind.MEMORY
                || operand.kind == Kind.IMMEDIATE;
    }

    private static void require(boolean condition, String message, int line) throws AsmException {
        if (!condition) {
            throw new AsmException(message, line);
        }
    }

    public static final class Session {
        private final List<Instruction> instructions;
        private final Map<String, Integer> labels;
        private MachineState state;

        private Session(List<Instruction> instructions, Map<String, Integer> labels, MachineState initialState) {
            this.instructions = List.copyOf(instructions);
            this.labels = Map.copyOf(labels);
            this.state = initialState;
        }

        public MachineState state() {
            return state;
        }

        public boolean isFinished() {
            return state.halted() || state.programCounter() >= instructions.size();
        }

        public int instructionCount() {
            return instructions.size();
        }

        public MachineState step() throws AsmException {
            if (isFinished()) {
                if (!state.halted()) {
                    state = new MachineState(state.registers(), state.memory(), state.programCounter(),
                            state.zeroFlag(), true, state.steps(), state.lastLine());
                }
                return state;
            }

            Instruction in = instructions.get(state.programCounter());
            int[] registers = state.registers();
            int[] memory = state.memory();
            int nextPc = state.programCounter() + 1;
            boolean zero = state.zeroFlag();
            boolean halted = false;

            switch (in.op) {
                case MOV -> write(in.a, read(in.b, registers, memory), registers, memory);
                case ADD -> {
                    int result = registers[in.a.value] + read(in.b, registers, memory);
                    registers[in.a.value] = result;
                    zero = result == 0;
                }
                case SUB -> {
                    int result = registers[in.a.value] - read(in.b, registers, memory);
                    registers[in.a.value] = result;
                    zero = result == 0;
                }
                case INC -> {
                    registers[in.a.value]++;
                    zero = registers[in.a.value] == 0;
                }
                case DEC -> {
                    registers[in.a.value]--;
                    zero = registers[in.a.value] == 0;
                }
                case CMP -> zero = registers[in.a.value] == read(in.b, registers, memory);
                case JMP -> nextPc = jumpTarget(in.a);
                case JZ -> {
                    if (zero) {
                        nextPc = jumpTarget(in.a);
                    }
                }
                case JNZ -> {
                    if (!zero) {
                        nextPc = jumpTarget(in.a);
                    }
                }
                case HLT -> halted = true;
            }

            if (nextPc >= instructions.size()) {
                halted = true;
            }
            state = new MachineState(registers, memory, nextPc, zero, halted,
                    state.steps() + 1, in.line);
            return state;
        }

        private int jumpTarget(Operand operand) {
            return labels.get(operand.text.toLowerCase(Locale.ROOT));
        }

        private static int read(Operand operand, int[] registers, int[] memory) {
            return switch (operand.kind) {
                case REGISTER -> registers[operand.value];
                case MEMORY -> memory[operand.value];
                case IMMEDIATE -> operand.value;
                case LABEL -> throw new IllegalStateException("Метка не является значением");
            };
        }

        private static void write(Operand operand, int value, int[] registers, int[] memory) {
            if (operand.kind == Kind.REGISTER) {
                registers[operand.value] = value;
            } else {
                memory[operand.value] = value;
            }
        }
    }

    private enum Op {
        MOV(2, false), ADD(2, false), SUB(2, false), INC(1, false), DEC(1, false),
        CMP(2, false), JMP(1, true), JZ(1, true), JNZ(1, true), HLT(0, false);

        private final int arity;
        private final boolean isJump;

        Op(int arity, boolean isJump) {
            this.arity = arity;
            this.isJump = isJump;
        }
    }

    private enum Kind { REGISTER, MEMORY, IMMEDIATE, LABEL }

    private record Operand(Kind kind, int value, String text) {
    }

    private record Instruction(Op op, Operand a, Operand b, int line) {
    }
}
