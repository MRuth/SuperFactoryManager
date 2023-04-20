package ca.teamdman.sfm;

import ca.teamdman.sfm.common.blockentity.ManagerBlockEntity;
import ca.teamdman.sfm.common.item.DiskItem;
import ca.teamdman.sfm.common.program.ProgramContext;
import ca.teamdman.sfml.ast.Trigger;
import net.minecraft.gametest.framework.GameTestAssertException;
import net.minecraft.gametest.framework.GameTestHelper;

import java.text.NumberFormat;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicLong;
import java.util.stream.LongStream;

public abstract class SFMGameTestBase {
    protected static void assertTrue(boolean condition, String message) {
        if (!condition) {
            throw new GameTestAssertException(message);
        }
    }

    protected static void succeedIfManagerDidThingWithoutLagging(
            GameTestHelper helper,
            ManagerBlockEntity manager,
            Runnable assertion
    ) {
        assertManagerDidThingWithoutLagging(helper, manager, () -> {
            assertion.run();
            helper.succeed();
        });
    }


    protected static void assertManagerDidThingWithoutLagging(
            GameTestHelper helper,
            ManagerBlockEntity manager,
            Runnable assertion
    ) {
        SFMGameTestBase.assertManagerRunning(manager); // the program should already be compiled so we can monkey patch it
        var hasExecuted = new AtomicBoolean(false);
        var startTime = new AtomicLong();
        var endTime = new AtomicLong();
        List<Trigger> triggers = manager.getProgram().triggers();
        var oldFirstTrigger = triggers.get(0);
        long          timeoutTicks    = 200;

        Trigger startTimerTrigger = new Trigger() {
            @Override
            public boolean shouldTick(ProgramContext context) {
                return oldFirstTrigger != null
                       ? oldFirstTrigger.shouldTick(context)
                       : context.getManager().getTick() % 20 == 0;
            }

            @Override
            public void tick(ProgramContext context) {
                startTime.set(System.nanoTime());
            }
        };

        Trigger endTimerTrigger = new Trigger() {
            @Override
            public boolean shouldTick(ProgramContext context) {
                return oldFirstTrigger != null
                       ? oldFirstTrigger.shouldTick(context)
                       : context.getManager().getTick() % 20 == 0;
            }

            @Override
            public void tick(ProgramContext context) {
                if (!hasExecuted.get()) {
                    hasExecuted.set(true);
                    endTime.set(System.nanoTime());
                }
            }
        };

        triggers.add(0, startTimerTrigger);
        triggers.add(endTimerTrigger);

        LongStream
                .range(helper.getTick() + 1, timeoutTicks - helper.getTick())
                .forEach(i -> helper.runAfterDelay(i, () -> {
                    if (hasExecuted.get()) {
                        triggers.remove(startTimerTrigger);
                        triggers.remove(endTimerTrigger);
                        assertion.run();
                        SFMGameTestBase.assertTrue(
                                endTime.get() - startTime.get() < 50_000_000,
                                "Program took too long to run: took " + NumberFormat
                                        .getInstance(Locale.getDefault())
                                        .format(endTime.get() - startTime.get()) + "ns"
                        );
                        hasExecuted.set(false); // prevent the assertion from running again
            }
        }));
    }

    protected static void assertManagerRunning(ManagerBlockEntity manager) {
        SFMGameTestBase.assertTrue(
                manager.getState() == ManagerBlockEntity.State.RUNNING,
                "Program did not start running " + DiskItem.getErrors(manager.getDisk().get())
        );
    }

}
