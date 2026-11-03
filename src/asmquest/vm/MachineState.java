package asmquest.vm;

import java.util.Arrays;

public final class MachineState {
    public static final int REGISTER_COUNT = 4;
    public static final int MEMORY_SIZE = 8;

    private final int[] registers;
    private final int[] memory;
    private final int programCounter;
    private final boolean zeroFlag;
    private final boolean halted;
    private final int steps;
    private final int lastLine;

    public MachineState(int[] registers, int[] memory) {
        this(registers, memory, 0, false, false, 0, -1);
    }

    public MachineState(int[] registers, int[] memory, int programCounter,
                        boolean zeroFlag, boolean halted, int steps, int lastLine) {
        if (registers.length != REGISTER_COUNT || memory.length != MEMORY_SIZE) {
            throw new IllegalArgumentException("Неверный размер регистров или памяти");
        }
        this.registers = registers.clone();
        this.memory = memory.clone();
        this.programCounter = programCounter;
        this.zeroFlag = zeroFlag;
        this.halted = halted;
        this.steps = steps;
        this.lastLine = lastLine;
    }

    public int register(int index) {
        return registers[index];
    }

    public int memory(int index) {
        return memory[index];
    }

    public int[] registers() {
        return registers.clone();
    }

    public int[] memory() {
        return memory.clone();
    }

    public int programCounter() {
        return programCounter;
    }

    public boolean zeroFlag() {
        return zeroFlag;
    }

    public boolean halted() {
        return halted;
    }

    public int steps() {
        return steps;
    }

    public int lastLine() {
        return lastLine;
    }

    @Override
    public String toString() {
        return "MachineState{R=" + Arrays.toString(registers)
                + ", M=" + Arrays.toString(memory)
                + ", PC=" + programCounter + ", ZF=" + zeroFlag + '}';
    }
}
