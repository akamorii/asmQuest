package asmquest.model;

import asmquest.vm.MachineState;

public record Level(
        String title,
        String concept,
        String theory,
        String task,
        String hint,
        String starterCode,
        String solution,
        int[] initialRegisters,
        int[] initialMemory,
        int targetX,
        int targetY,
        int par,
        Goal goal
) {
    public boolean isSolved(MachineState state) {
        return goal.test(state);
    }

    @FunctionalInterface
    public interface Goal {
        boolean test(MachineState state);
    }
}
