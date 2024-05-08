package ro.uaic.swqual.unit.proc;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import ro.uaic.swqual.mem.RAM;
import ro.uaic.swqual.unit.mem.MemTestUtility;
import ro.uaic.swqual.model.Instruction;
import ro.uaic.swqual.model.InstructionType;
import ro.uaic.swqual.model.operands.AbsoluteMemoryLocation;
import ro.uaic.swqual.model.operands.FlagRegister;
import ro.uaic.swqual.proc.MMU;

import java.util.function.BiPredicate;
import java.util.function.Function;
import java.util.stream.IntStream;

class MMUTest implements ProcTestUtility, MemTestUtility {
    @Test
    void movRegRegTest() {
        var r0 = reg(10);
        var r1 = reg();
        var sp = reg();
        var mmu = new MMU(freg(), sp);
        mmu.execute(new Instruction(InstructionType.MMU_MOV, r0, r1));
        Assertions.assertEquals(r0.getValue(), r1.getValue());
    }

    @Test
    void movRegRamTest() {
        exceptionLess(() -> {
            var sp = reg();
            var freg = freg();

            var mmu = new MMU(freg, sp);
            var ram = new RAM((char) 0x1000, freg);

            mmu.registerHardwareUnit(ram, (char) 0x0000, (location) -> location >= 0x100 && location <= 0x1000);

            var addr = reg(0x50); // mov r0 0x50
            var loc = new AbsoluteMemoryLocation(addr); // define [r0] (add pointer)

            var location = mmu.locate(loc); // locate [r0]
            Assertions.assertFalse(freg.isSet(FlagRegister.SEG_FLAG));
            Assertions.assertNotNull(location.getValue()); // actually access [r0]
            Assertions.assertTrue(freg.isSet(FlagRegister.SEG_FLAG));


            addr.setValue(0x150);
            location = mmu.locate(loc);
            freg.clear();

            Assertions.assertFalse(freg.isSet(FlagRegister.SEG_FLAG));
            Assertions.assertNotNull(location.getValue());
            Assertions.assertFalse(freg.isSet(FlagRegister.SEG_FLAG));
        });
    }

    @Test
    void delegateRamShouldBeDifferentFromMmuRam() {
        exceptionLess(() -> {
            var freg = freg();
            var sp = reg();
            var mmu0 = new MMU(freg, sp);
            var mmu1 = new MMU(freg, sp);
            var ram0 = new RAM(0x1000, freg);
            var ram1 = new RAM(0x2000, freg);

            // Offsets and comparators are RELATIVE to their unit
            // MMU0 has:
            //   - RAM0 at offset 0x0000 ranging [0x0000, 0x1000)
            //   - MMU1 at offset 0x2000 ranging [0x2000, 0x4000)
            // MMU1 has:
            //   - RAM1 ranging [0x0000, 0x2000)
            // => MMU1 HW will range:
            //      absoluteRangeOf(RAM1) = offsetOf(RAM1) + rangeOf(RAM1)
            //                            = offsetOf(MMU0.MMU1) + offsetOf(MMU1.RAM1) + range(RAM1)
            //                            = 0x2000 + 0x0000 + [0x0000, 0x2000)
            //                            = [0x2000, 0x4000)

            // Relative to MM0
            mmu0.registerHardwareUnit(ram0, (char) 0x0000, (location) -> location + 1 < 0x1000);
            mmu0.registerLocator(mmu1, (char) 0x2000, location -> location >= 0x2000 && location + 1 < 0x4000);

            // Relative to MM1
            mmu1.registerHardwareUnit(ram1, (char) 0x0000, (location) -> location + 1 < 0x2000);

            Function<Boolean, BiPredicate<Character, Character>> memoryValidator = (validity) ->
                    (start, end) -> IntStream.range(start, end)
                            .mapToObj(addrVal -> {
                                freg.clear();
                                discard(mmu0.locate(aloc(reg(addrVal))).getValue());
                                return null;
                            })
                            .allMatch(ignored -> freg.isSet(FlagRegister.SEG_FLAG) != validity);
            Assertions.assertTrue(memoryValidator.apply(true).test((char) 0, (char) 0x0FFF));
            Assertions.assertTrue(memoryValidator.apply(false).test((char) 0x0FFF, (char) 0x2000));
            Assertions.assertTrue(memoryValidator.apply(true).test((char) 0x2000, (char) 0x3FFF));
            Assertions.assertTrue(memoryValidator.apply(false).test((char) 0x3FFF, (char) 0x6000));
        });
    }

    @Test
    void rangeBasedDelegateRamShouldBeDifferentFromMmuRam() {
        exceptionLess(() -> {
            var freg = freg();
            var sp = reg();

            // Offsets and comparators are RELATIVE to their unit
            // MMU0 has:
            //   - RAM0 at offset 0x0000 ranging [0x0000, 0x1000)
            //   - MMU1 at offset 0x2000 ranging [0x2000, 0x4000)
            // MMU1 has:
            //   - RAM1 ranging [0x0000, 0x2000)
            // => MMU1 HW will range:
            //      absoluteRangeOf(RAM1) = offsetOf(RAM1) + rangeOf(RAM1)
            //                            = offsetOf(MMU0.MMU1) + offsetOf(MMU1.RAM1) + range(RAM1)
            //                            = 0x2000 + 0x0000 + [0x0000, 0x2000)
            //                            = [0x2000, 0x4000)

            var mmu0 = new MMU(freg, sp);
            var mmu1 = new MMU(freg, sp);

            var ram0Size = (char) 0x1000;
            var ram1Size = (char) 0x2000;

            var ram0 = new RAM(ram0Size, freg);
            var ram1 = new RAM(ram1Size, freg);

            // Relative to MM0
            mmu0.registerHardwareUnit(ram0, (char) 0x0000, ram0Size);
            mmu0.registerLocator(mmu1, (char) 0x2000, ram1Size);

            // Relative to MM1
            mmu1.registerHardwareUnit(ram1, (char) 0x0000, ram1Size);

            Function<Boolean, BiPredicate<Character, Character>> memoryValidator = (validity) ->
                    (start, end) -> IntStream.range(start, end)
                            .mapToObj(addrVal -> {
                                freg.clear();
                                discard(mmu0.locate(aloc(reg(addrVal))).getValue());
                                return null;
                            })
                            .allMatch(ignored -> freg.isSet(FlagRegister.SEG_FLAG) != validity);
            Assertions.assertTrue(memoryValidator.apply(true).test((char) 0, (char) 0x0FFF));
            Assertions.assertTrue(memoryValidator.apply(false).test((char) 0x0FFF, (char) 0x2000));
            Assertions.assertTrue(memoryValidator.apply(true).test((char) 0x2000, (char) 0x3FFF));
            Assertions.assertTrue(memoryValidator.apply(false).test((char) 0x3FFF, (char) 0x6000));
        });
    }
}
