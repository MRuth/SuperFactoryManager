# Troubleshooting

## Problem 1 - Catch type is not a subclass of Throwable in exception handler 47

Story: I had this problem on 2024-11-14.

[ref](https://discord.com/channels/313125603924639766/1116211620415283201/1306487796105744405)

```
OpenJDK 64-Bit Server VM warning: Option AllowRedefinitionToAddDeleteMethods was deprecated in version 13.0 and will likely be removed in a future release.
Connected to the target VM, address: '127.0.0.1:27473', transport: 'socket'
Error: Unable to initialize main class cpw.mods.bootstraplauncher.BootstrapLauncher in module cpw.mods.bootstraplauncher
Caused by: java.lang.VerifyError: Catch type is not a subclass of Throwable in exception handler 47
Exception Details:
  Location:
    cpw/mods/bootstraplauncher/BootstrapLauncher.loadLegacyClassPath()Ljava/util/List; @47: astore_2
  Reason:
    Type 'java/io/IOException' (constant pool 328) is not assignable to 'java/lang/Throwable'
  Bytecode:
    0000000: 1301 3cb8 012c 4b2a c600 372a 03bd 0016
    0000010: b800 4e4c 2b03 bd00 76b8 013e 9900 232b
    0000020: 03bd 0076 b801 4199 0018 2bb8 0144 b04d
    0000030: bb01 4a59 2aba 014c 0000 2cb7 014d bf12
    0000040: 0d13 0150 b801 2cb8 0025 4c2b 1301 52b8
    0000050: 0154 572b b601 5a9a 0007 b801 5eb0 2bb2
    0000060: 000f b600 2ab8 0160 b0                 
  Exception Handler Table:
    bci [42, 46] => handler: 47
  Stackmap Table:
    full_frame(@47,{Object[#22],Object[#85]},{Object[#328]})
    chop_frame(@63,1)
    append_frame(@94,Object[#22])

Disconnected from the target VM, address: '127.0.0.1:27473', transport: 'socket'

Process finished with exit code 1
```

This error shows only when debugging the run configuration, running it in normal mode worked properly.
Was able to fix this by downgrading IDEA from 2024.3 to 2024.2.4.
Also did `gradlew --no-daemon clean` and `gradlew --no-daemon --refresh-dependencies build` somewhere in between.
Also renamed `.idea` to `.idea.bak` and invalidated caches a few times.
Cursed error but seems to have been quelled for now.

---

Today, 2025-01-10: I was happily using IDEA Ultimate edition 2024.3 but had to switch to IntelliJ IDEA Community Edition and I tried version `2024.3.1.1` but re-encountered the above error.
Downgrading to IntelliJ IDEA Community Edition `2023.3.8` has restored my ability to use the IntelliJ run configurations to launch the game in debug mode.


## General things to try when something isn't working

-   ```pwsh
    .\gradlew.bat --no-daemon clean
    ```

-   ```pwsh
    .\gradlew --no-daemon --refresh-dependencies build
    ```

- remove run configurations from intellij gui and run the `genIntellijRuns` gradle tasl

- run the `runData` gradle task to generate missing files and apply changes
- [neoforged/RenderNurse](https://github.com/neoforged/RenderNurse/tree/main) may help with debugging graphics problems but I personally haven't tried it
- Look at the run configuration and make sure the correct JDK and module are specified