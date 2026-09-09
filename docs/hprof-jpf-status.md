# HPROF -> JPF State Reconstruction

## Quick Regression

```bash
./gradlew hprofSmokeTest
```

Graph identity regression:

```bash
./gradlew hprofGraphIdentityTest
```

Object-array topology regression:

~~~bash
./gradlew hprofObjectArrayTest
~~~

Primitive-scalar regression:

~~~bash
./gradlew hprofPrimitiveScalarTest --no-daemon
~~~

Primitive-array regression:

~~~bash
./gradlew hprofPrimitiveArrayTest --no-daemon
~~~

Static-state regression:

~~~bash
./gradlew hprofStaticStateTest --no-daemon
~~~

Class-initialization-state characterization:

~~~bash
./gradlew hprofClassInitTest --no-daemon
~~~

Supplemental class-lifecycle metadata regression:

~~~bash
./gradlew hprofClassLifecycleTest --no-daemon
~~~

Class-loader identity characterization:

~~~bash
./gradlew hprofClassLoaderIdentityTest --no-daemon
~~~

Checkpoint loader-state characterization:

~~~bash
./gradlew hprofClassLoaderStateTest --no-daemon
~~~

Loader-qualified class reconstruction regression:

~~~bash
./gradlew hprofLoaderQualifiedImportTest --no-daemon
~~~

Checkpoint-bundle correlation regression:

~~~bash
./gradlew hprofCheckpointBundleTest --no-daemon
~~~

All HPROF regressions:

~~~bash
./gradlew hprofRegressionTest
~~~

This single command creates a fresh build-owned HotSpot HPROF, reconstructs the selected graph in JPF, validates it from host and modeled Java code, deliberately runs JPF garbage collection, and verifies post-GC preservation.

## Research Goal

Capture an intermediate execution of a real Java application as an HPROF heap dump, parse it in the host JVM with Square HAHA/perflib, and translate the captured objects into JPF `ClassInfo`/`ElementInfo` state. The eventual goal is systematic concurrency exploration starting from an intermediate concrete runtime state instead of program initialization.

## Explicit Non-Goals / Abandoned Approaches

- The MAT/Eclipse/Equinox/OSGi integration is abandoned. Do not revive it unless explicitly requested.
- Exact continuation of threads, stacks, program counters, and synchronization state is future work.
- The current milestone is heap reconstruction.

## Current Architecture

```text
Real JVM
  -> HPROF
  -> HAHA/perflib
  -> HprofView
  -> HprofHeapBootstrap
  -> HPROF ID -> JPF reference mapping
  -> JPF ClassInfo / ElementInfo heap
```

## Post-Refactor Import Architecture

```text
HprofHeapBootstrap (configuration and VM lifecycle)
  -> HprofSnapshotLoader (parse HPROF)
  -> HprofView (resolved/indexed HAHA view)
  -> JpfHeapImporter.importHeap(...)
       -> Pass A: allocateObjectsAndArrays()
       -> Pass B: populateObjectsAndArrays()
       -> immutable ImportResult
  -> HprofPassASmokeValidator (optional fixture-specific assertions)
```

`JpfHeapImporter` matches ordinary objects by the exact binary class names in `hprof.classes`. Pass A allocates every selected ordinary instance, then only directly referenced arrays needed by those selected instances. Pass B runs only after all identities exist. The importer contains no Foo/Bar field values or expected reference numbers.

`ImportResult` exposes an unmodifiable copy of the HPROF-ID-to-JPF-reference map and counts for allocated objects, allocated arrays, instance primitive fields, instance references, array elements, static primitive fields, and static references.

The listener validates `hprof.file` and `hprof.classes`, reads optional exact `hprof.static_classes`, loads the snapshot, constructs `HprofView`, invokes the importer, and optionally invokes the smoke validator when `hprof.smoke_validate=true`. The smoke configuration separately enables `hprof.smoke_bind_root=true`.

Currently supported within selected classes includes all eight Java primitive scalar instance-field types, `Type.OBJECT` references whose non-null target has a Pass A mapping, directly referenced arrays of every primitive HAHA `Type` with exact boxed payload validation, and directly referenced one-dimensional `Type.OBJECT` arrays whose non-null elements already have Pass A mappings. Unsupported selected field types, unsupported directly referenced arrays, malformed values, and non-null references outside the selected identity graph cause a clear failure rather than an incomplete import.

## State Reconstruction Coverage Matrix

| Feature | Status |
|---|---|
| `int` instance field | verified |
| ordinary object reference | verified |
| `boolean[]` allocation/payload | verified |
| `byte[]` allocation/payload | verified |
| `char[]` allocation/payload | verified |
| `short[]` allocation/payload | verified |
| `int[]` allocation/payload | verified |
| `long[]` allocation/payload | verified |
| `float[]` allocation/payload | verified |
| `double[]` allocation/payload | verified |
| modeled static smoke root | verified |
| GC preservation | verified |
| null reference | verified |
| reference aliasing | verified |
| cyclic object graph | verified |
| `boolean` instance field | verified |
| `byte` instance field | verified |
| `char` instance field | verified |
| `short` instance field | verified |
| `long` instance field | verified |
| `float` instance field | verified |
| `double` instance field | verified |
| object-array allocation | verified |
| object-array reference payload | verified |
| null object-array element | verified |
| aliased object-array elements | verified |
| cycles through object arrays | verified |
| shared identity across field/array edges | verified |
| GC preservation of object arrays | verified |
| inherited primitive instance field | verified |
| inherited object-reference field | verified |
| superclass/subclass reference aliasing | verified |
| field hiding / same-name superclass-subclass fields | verified |
| declaring-class-aware field identity | verified |
| inheritance preservation through modeled execution | verified |
| inheritance preservation through JPF GC | verified |

| primitive static fields | verified |
| object-reference static fields | verified |
| null static reference | verified |
| aliased static references | verified |
| static array reference | verified |
| static field as JPF GC root | verified |
| modeled access to reconstructed statics | verified |
| static preservation through JPF GC | verified |
| static field values | verified |
| no-`<clinit>` static classes | verified |
| class contains `<clinit>` | unsupported for import |
| loaded-but-uninitialized class | verified with lifecycle metadata |
| initialized class with `<clinit>` | verified with lifecycle metadata |
| class-init state observable in HPROF | no explicit standard HPROF status |
| JPF initialized-state restoration | verified with lifecycle metadata |
| JPF uninitialized-state preservation | verified with lifecycle metadata |
| initialized state without rerunning `<clinit>` | verified |
| uninitialized first-use `<clinit>` behavior | verified |
| supplemental lifecycle metadata V1 | verified |
| `INITIALIZING` lifecycle state | unsupported |
| initializing thread identity | unsupported |
| initialization failure state | unsupported |
| multi-loader lifecycle identity | unsupported |
| exact `<clinit>` continuation semantics | verified for V1 states only |
| class with `<clinit>` | unsupported |
| class-initialization state restoration | deferred |
| HPROF GC roots | unsupported |
| HPROF root taxonomy | characterized |
| HPROF root provenance through HAHA | partial; category/object retained, associated metadata partly hidden/lost |
| static-field root semantics | verified through `StaticElementInfo` |
| Java-frame/local root liveness | characterized |
| Java-frame/local root semantics | verified for one supplied RUNNING Java frame/local; general stacks deferred |
| thread-object root provenance | parser loss in HAHA public root collection |
| thread-object root semantics | blocked on `ThreadInfo` reconstruction |
| monitor-root liveness | uncharacterized on controlled HotSpot capture; record not emitted |
| monitor-root semantics | blocked on monitor/thread reconstruction |
| JNI/native root semantics | blocked on native/thread state; parser metadata loss |
| `String` | unsupported |
| multiple class loaders | verified for bounded checkpoint-bundle reconstruction |
| HPROF class-loader identity | verified |
| HAHA loader-qualified `ClassObj` identity | verified |
| same-name classes from different loaders | characterized |
| instance -> loader-qualified class association | verified |
| JPF multiple loader-qualified `ClassInfo` | characterized from local APIs |
| importer loader-aware class resolution | verified for explicitly bundled ClassObj identities |
| loader-qualified static reconstruction | unsupported |
| loader-qualified lifecycle metadata | unsupported in V1 |
| custom loader object/state reconstruction | unsupported |
| capture-local loader identity | verified |
| checkpoint-bundle loader identity | characterized |
| cross-capture loader identity | deferred/not required for single replay |
| standard HPROF classfile bytecode | absent |
| fixture-retained loader bytecode | verified, fixture-specific |
| modeled loader object -> `ClassLoaderInfo` registration | verified |
| minimum duplicate-name JPF loader probe | verified |
| loader external capability reconstruction | unsupported/fixture-dependent |
| checkpoint-local loader logical identity | verified |
| logical loader -> HPROF loader correlation | verified |
| logical class -> HPROF ClassObj correlation | verified |
| exact captured classfile artifact | verified |
| bundle -> exact HPROF binding | verified |
| class artifact integrity binding | verified |
| same-name/different-bytecode packaging | verified |
| generic loader-aware JPF import | unsupported |
| generic loader capability replay | unsupported |
| lifecycle schema loader qualification | unsupported in V1 |
| loader HPROF ID -> modeled ClassLoaderInfo | verified |
| ClassObj ID -> loader-qualified ClassInfo | verified |
| same-name/different-loader instance allocation | verified |
| same-name/different-bytecode method execution | verified |
| loader-qualified instance field reconstruction | verified |
| system-loader substitution prevention | verified |
| bundled custom superclass identity | verified |
| bundled custom interface identity | verified |
| loader-qualified superclass `ClassInfo` edge | verified |
| loader-qualified interface `ClassInfo` edge | verified |
| inherited fields across bundled hierarchy | verified |
| `invokevirtual` over bundled hierarchy | verified |
| `invokeinterface` over bundled hierarchy | verified |
| dependency-aware definition ordering | verified |
| system-loader dependency substitution prevention | verified |
| generic loader parents | unsupported |
| loader-qualified object arrays | deferred |
| future dynamic custom loading | unsupported |
| one `RUNNING` modeled thread | verified from supplied execution metadata |
| one Java frame with empty operand stack | verified from supplied execution metadata |
| primitive/reference local slots and reference mask | verified for int/reference locals |
| exact bytecode PC continuation | verified from supplied bytecode offset |
| semantic Java-frame GC root | verified for reconstructed reference local |
| arbitrary operand stack | unsupported |
| general frame chains / caller return state | unsupported |
| pending exception state | unsupported |
| thread state beyond `RUNNING` | unsupported |
| monitors | deferred |

Status meanings: **verified** has an end-to-end regression; **implemented but unverified** is supported by current code but lacked a dedicated proof when recorded; **unsupported** is rejected or not reconstructed; **deferred** is outside the current heap milestone.

## Relevant Files

- `build.gradle`: HAHA 2.0.4 dependency, `copyRuntimeLibs`, and the `hprofSmokeTest` two-process regression task chain.
- `src/main/gov/nasa/jpf/vm/hprof/HprofSnapshotLoader.java`: memory-maps and parses an HPROF with HAHA.
- `src/main/gov/nasa/jpf/vm/hprof/HprofView.java`: resolves HAHA classes/references and indexes classes, ordinary instances, object arrays, and primitive arrays by HPROF ID.
- `src/main/gov/nasa/jpf/listener/HprofHeapBootstrap.java`: thin configuration/lifecycle adapter that parses, imports, and optionally invokes smoke validation.
- `src/main/gov/nasa/jpf/vm/hprof/JpfHeapImporter.java`: explicit two-pass allocation/population algorithm and immutable `ImportResult`.
- `src/main/gov/nasa/jpf/vm/hprof/HprofPassASmokeValidator.java`: fixture-specific host-side graph and value assertions.
- `src/main/gov/nasa/jpf/vm/hprof/HprofPassASmokeRootBinder.java`: smoke-only bridge from the imported Foo mapping to modeled `HprofPassASmokeGraph.root` static storage.
- `src/main/gov/nasa/jpf/vm/hprof/HprofTestRootBinder.java`: parameterized test-only binding from one exact imported class to one modeled static root; this is not HPROF static reconstruction.
- `src/main/gov/nasa/jpf/vm/hprof/HprofGraphIdentityValidator.java`: fixture-specific host validation of null, aliasing, cycles, identity counts, and post-GC survival.
- `src/examples/HprofGraphIdentityGraph.java`, `HprofGraphIdentityDumpGenerator.java`, `HprofGraphIdentityTarget.java`, and `HprofGraphIdentity.jpf`: separate E.1 graph-topology fixture and two-process workflow.
- `src/examples/HprofPassASmokeGraph.java`: shared Foo/Bar definitions.
- `src/examples/HprofPassADumpGenerator.java`: real-HotSpot-only graph and HPROF generator; never a JPF target.
- `src/examples/HprofPassASmokeTarget.java`: harmless modeled JPF target.
- `src/examples/HprofPassASmoke.jpf`: explicit dump path, selected classes, and listener configuration.
- `src/examples/HeapDump.java` and `HeapDump.jpf`: older unsafe experiment; do not use for this workflow.
- `ParseSnapshotApp.java`, `.equinox/`, and other MAT files: historical abandoned implementation.

## Verified Local API Facts

HAHA/perflib 2.0.4:

- `Snapshot.getHeaps()` returns the heaps.
- `Heap.getClasses()` and `Heap.getInstances()` provide traversal.
- Arrays are `Instance` subclasses and are detected with `instanceof ArrayInstance`.
- `ArrayInstance.getArrayType()` distinguishes `Type.OBJECT` from primitive arrays.
- `ArrayInstance.getValues()` returns the array values; its returned array length is the HPROF array length.
- There is no `Heap.getArrays()`.
- Do not depend on a separate `PrimitiveArray` type.
- This version has no `ArrayInstance.asObjectArray()`, `asIntArray()`, `getLength()`, or `getType()`.
- Call `Snapshot.resolveClasses()` and `Snapshot.resolveReferences()` before traversal.
- `ClassInstance.getValues()` returns `ClassInstance.FieldValue` entries. `FieldValue.getField()` returns a `Field`; `Field.getName()` and `Field.getType()` expose the name and HAHA `Type`, while `FieldValue.getValue()` supplies the decoded value. For these `Type.INT` smoke fields, the value is an `Integer`. In the existing smoke HPROF, the `Type.OBJECT` field `Foo.child` returns a `com.squareup.haha.perflib.ClassInstance` (also an `Instance`) for `HprofPassASmokeGraph$Bar`; its `Instance.getId()` is the target HPROF object ID. The `Type.OBJECT` field `Foo.numbers` returns a `com.squareup.haha.perflib.ArrayInstance`; `getArrayType()` is `Type.INT`, `getValues().length` is 3, and `getId()` supplies the array HPROF ID. For its `Type.INT` payload, `ArrayInstance.getValues()` is declared as `Object[]` and returns an actual `java.lang.Object[]`; each element is a boxed `java.lang.Integer` (`10`, `20`, `30`), not a primitive `int[]`.

JPF in this checkout:

- Resolve application metadata with `ClassLoaderInfo.getSystemResolvedClassInfo(className)`.
- Allocate objects with `heap.newObject(ClassInfo, ThreadInfo)`.
- Allocate arrays with `heap.newArray(String elementType, int length, ThreadInfo)`; primitive int uses element type `"I"`, not `"[I"`.
- Obtain the allocation thread with `vm.getCurrentThread()`.
- Array mutation methods include `ElementInfo.setIntElement(...)` and `ElementInfo.setReferenceElement(...)`.
- Field mutation methods include `setIntField(...)` and `setReferenceField(...)`.
- When mutating an existing stored object, use `Heap.getModifiable(int ref)`. Pass B.1 uses `ElementInfo.setIntField(String, int)` and verifies with `ElementInfo.getIntField(String)`. Pass B.2 uses `ElementInfo.setReferenceField(String, int)` and reads it back with `ElementInfo.getReferenceField(String)`. Pass B.3 validates the mapped shell with `ElementInfo.isArray()`, `getClassInfo().getName()`, `arrayLength()`, and `getIntElement(int)`. Pass B.4 obtains that same shell with `Heap.getModifiable(int)`, writes with `ElementInfo.setIntElement(int, int)`, and reads back with `getIntElement(int)`.

## Build / Run Instructions

From the repository root:

```bash
./gradlew compile --no-daemon
./gradlew buildJars --no-daemon
```

Generate the disposable smoke dump on the real HotSpot JVM. The generator refuses to overwrite an existing file:

```bash
java -cp build/examples HprofPassADumpGenerator src/examples/hprof-pass-a.hprof
```

Run JPF separately. HAHA and Trove execute in the host JVM and must be on its classpath:

```bash
java -cp 'build/RunJPF.jar:build/deps/*' \
  gov.nasa.jpf.tool.RunJPF src/examples/HprofPassASmoke.jpf
```

`hprof.file` is explicit in the `.jpf` file and expands from `${config_path}`; the listener does not fall back to its working directory.

## Current Milestone

Pass A and Pass B.1 through B.4 are complete. The reconstructed JPF graph has `Foo.value == 123`, `Foo.child` pointing to the existing Bar shell with `number == 42`, and `Foo.numbers` pointing to the existing `[I` shell containing `{10, 20, 30}`.

**BASIC HOTSPOT -> HPROF -> JPF GRAPH RECONSTRUCTION: COMPLETE**

**PASS C.1 — MODEL-VISIBLE ROOT BINDING: COMPLETE**

**PASS C.2 — GC / ROOT PRESERVATION: COMPLETE**

**PASS D.1 — DURABLE SINGLE-COMMAND REGRESSION: COMPLETE**

**PASS E.1 — GRAPH IDENTITY SEMANTICS: COMPLETE**

**PASS E.2 — OBJECT-ARRAY TOPOLOGY: COMPLETE**

**PASS E.3 — INHERITANCE / FIELD-LAYOUT FIDELITY: COMPLETE**

The same reconstructed Foo is bound to modeled `HprofPassASmokeGraph.root`, and ordinary bytecode in `HprofPassASmokeTarget.main()` reads and validates the complete graph.


## Pass C.1 — Model-Visible Root Binding

```text
HPROF
  -> HprofView
  -> JpfHeapImporter
  -> reconstructed JPF heap
  -> HprofPassASmokeRootBinder
  -> HprofPassASmokeGraph.root
  -> HprofPassASmokeTarget.main()
  -> modeled Java reads imported graph
```

`HprofPassASmokeRootBinder` is smoke infrastructure, not generic HPROF static-field reconstruction. It finds the single Foo by exact HPROF class name, obtains its existing JPF reference from `ImportResult.getRefMap()`, and verifies the reference points to the expected Foo `ClassInfo`. It does not allocate or copy another graph.

In this checkout, `ClassLoaderInfo.getSystemResolvedClassInfo(name)` resolves metadata. `ClassInfo.isRegistered()` reports whether static storage exists; `ClassInfo.registerClass(ThreadInfo)` creates and prepares its `StaticElementInfo`. `ClassInfo.initializeClass(ThreadInfo)` either schedules `<clinit>` frames or, when no `<clinit>` exists, marks the class initialized. The smoke graph holder has a default-null `static Foo root` and compiled bytecode confirms it has no `<clinit>`. The binder registers and initializes it before using `ClassInfo.getModifiableStaticElementInfo()` and `StaticElementInfo.setReferenceField("root", fooRef)`. Later modeled `GETSTATIC` sees an initialized class, so it does not repeat preparation or overwrite the binding.

The smoke target performs ordinary modeled Java field and array reads through `HprofPassASmokeGraph.root` and throws `AssertionError` on any mismatch. Proven: the reconstructed graph is usable by modeled Java. Not yet proven: deliberate survival/reachability across JPF garbage collection.
## Known Hazards

**Do not run the existing `HeapDump` dump-generation/deletion behavior under JPF.** A previous test caused modeled `File.delete()` to delete the real `heapdump.hprof`. Real-JVM heap generation and modeled JPF execution must remain separate.

The worktree contains unrelated staged and unstaged changes. Preserve them. Do not use `git reset`, `git clean`, or checkout/revert unrelated files, and do not stage work unless explicitly requested.

Heap reconstruction, modeled static attachment, and preservation of the smoke graph through a deliberate JPF garbage-collection cycle have been demonstrated. Generic reconstruction of HPROF roots and static fields has not yet been established.

Do not import an arbitrary entire JVM heap during the smoke milestones. The dump contains many VM implementation classes that are unavailable or inappropriate in JPF.

## Next Milestones

1. Pass A object identity/allocation — implemented for the narrow smoke graph.
2. Pass B.1 primitive fields (`Foo.value`, `Bar.number`) — complete.
3. Pass B.2 reference field `Foo.child` through `refMap` — complete.
4. Pass B.3 reference field `Foo.numbers` to the existing JPF `int[]` shell — complete.
5. Pass B.4 copy and verify the HPROF `int[]` contents `{10, 20, 30}` — complete.
6. Behavior-preserving listener/importer refactor — complete.
7. Pass C.1 — model-visible smoke root binding — complete.
8. Pass C.2 — GC/root preservation test — complete.
9. Pass D.1 — dedicated one-command Gradle smoke workflow — complete.
10. Pass E.1 — null, aliasing, and cyclic graph identity semantics — complete.
11. Next semantic coverage milestone — choose deliberately.
12. Decision point: choose later coverage milestones deliberately.
13. Candidate: add durable end-to-end validation.
14. Later: static fields, Strings, class initialization state, and GC roots.
15. Much later: threads, stacks, and program counters.

## Session Handoff

At the end of each implementation session, update this section with what changed, the exact build/run result, the current blocker, and the next smallest step.

- Changed: added a separate Graph/Node identity fixture, parameterized test-only `HprofTestRootBinder`, fixture-specific `HprofGraphIdentityValidator`, and the `hprofGraphIdentityTest` regression. `JpfHeapImporter` was unchanged, and `hprofSmokeTest` remains the frozen baseline.
- Build/run result: two consecutive `./gradlew hprofGraphIdentityTest --no-daemon --console=plain` runs succeeded with `no errors detected`; the original `./gradlew hprofSmokeTest --no-daemon --console=plain` also succeeded afterward.
- HPROF/JPF result from the second identity run: Graph `0x70b73f770 -> 191`, Node(a) `0x70b73f5e0 -> 192`, Node(b) `0x70b73f5f8 -> 193`. HPROF IDs vary across fresh dumps and are diagnostic only.
- Topology verification: HAHA represented `Graph.nullable` as Java `null`, which imported as local `MJIEnv.NULL` (`0`); `Graph.left` and `right` both equaled ref 192; the cycle was 192 -> 193 -> 192 before and after deliberate GC.
- Counts: objects=3, arrays=0, mappings=3, primitiveFields=3, references=5, arrayElements=0. Null increments `references` because it is an explicitly restored `Type.OBJECT` field assignment.
- Current limitations: E.1 adds no types; primitive types other than int, object arrays, inheritance, generic statics/roots, Strings, class loaders, and execution state remain unsupported or unverified as recorded in the matrix.
- Current blocker: none for Pass E.1.
- Next smallest step: select one new semantic dimension; object-array topology is the recommended next focused coverage because it extends graph edges without mixing in new primitive scalar types.

## Pass C.2 — GC / Root Preservation

The modeled target now validates the imported graph, calls `System.gc()`, and then calls `Verify.breakTransition("hprof-pass-c2-controlled-gc")` before reading the graph again. This follows the established forced-GC pattern in this checkout: modeled `java.lang.System.gc()` is native; `JPF_java_lang_System.gc____V(...)` calls `SystemState.activateGC()`, which sets `GCNeeded`. Collection is deferred until the transition boundary, where `VM.forward()` calls `SystemState.gcIfNeeded()`, then `KernelState.gc()`, and finally `GenericHeap.gc()`.

`GenericHeap.gc()` brackets mark/sweep with `VM.notifyGCBegin()` and `VM.notifyGCEnd()`. These invoke `VMListener.gcBegin(VM)` and `gcEnd(VM)`. `HprofHeapBootstrap` delegates those callbacks to the smoke-only `HprofPassASmokeGcVerifier`, which retains the original Foo, Bar, and int[] references derived from `ImportResult` and validates them with `Heap.get(ref)` after collection. The collector is non-moving in this execution: sweep removes unmarked reference slots and retains marked objects under their existing references, so stable reference equality is a valid checked invariant here.

Static rooting follows this local path: `GenericHeap.mark()` calls `VM.getClassLoaderList().markRoots(heap)`; each class loader's `OVStatics.markRoots(heap)` visits live `StaticElementInfo` objects; `StaticElementInfo.markStaticRoot(heap)` queues every non-null static reference field through `Heap.markStaticRoot(ref)`. Recursive marking then follows Foo's `child` and `numbers` fields to Bar and the int[] payload.

Observed deliberate-cycle ordering:

```text
[HPROF-JPF-MODEL] pre-GC graph verified
[HPROF-JPF-MODEL] requesting deliberate JPF GC
[HPROF-JPF] GC begin: cycle=1
[HPROF-JPF] GC end: cycle=1
[HPROF-JPF] controlled GC verified: cycles=1 Foo=192 Bar=191 int[]=193
[HPROF-JPF-MODEL] post-GC reconstructed graph verified
```

The references shown are diagnostic values, not hard-coded expectations. A second normal end-of-execution GC was also observed and the rooted graph survived it. Import counts remained `objects=2 arrays=1 mappings=3 primitiveFields=2 references=2 arrayElements=3`. Both Gradle builds succeeded and JPF finished with `no errors detected`.

Proven:

- HotSpot HPROF object identity maps to distinct JPF object identity.
- Primitive instance state, ordinary references, primitive-array references, and int[] payloads transfer correctly.
- The reconstructed graph is attached to modeled static state and is usable by modeled Java.
- The same Foo, Bar, and int[] references and all values survive deliberate JPF garbage collection.

Not yet proven:

- Generic HPROF GC-root reconstruction.
- Generic static-field reconstruction.
- Broader Java type coverage.
- Execution-state, thread, stack, program-counter, or monitor reconstruction.

No negative control was added; C.2 tests only positive preservation of the rooted imported graph. The next milestone is Pass D.1: make the complete two-process smoke workflow reproducible with one dedicated Gradle command.

## Pass D.1 — Durable Single-Command Regression

Public entry point:

```bash
./gradlew hprofSmokeTest
```

Task graph:

```text
compileExamplesJava
  -> generateHprofSmokeDump (JavaExec, real HotSpot JVM)
       -> build/hprof-smoke/hprof-pass-a.hprof

buildJars
  -> copyRuntimeLibs -> build/deps

buildJars + generateHprofSmokeDump
  -> runHprofSmokeJpf (Exec, separate host JVM)
       -> hprofSmokeTest
```

`generateHprofSmokeDump` is deliberately never a JPF target. It runs `HprofPassADumpGenerator` with `sourceSets.examples.output` on a normal JVM. The task is always out-of-date, deletes only the exact build-owned `build/hprof-smoke/hprof-pass-a.hprof`, invokes the generator, then requires a non-empty output. The generator's normal refusal to overwrite remains unchanged.

`runHprofSmokeJpf` launches `${java.home}/bin/java` with host/native classpath:

```text
build/RunJPF.jar:<platform path separator>build/deps/*
```

Its arguments are:

```text
gov.nasa.jpf.tool.RunJPF
+hprof.file=<absolute path to build/hprof-smoke/hprof-pass-a.hprof>
<absolute path to src/examples/HprofPassASmoke.jpf>
```

This checkout's `Config` first loads the `.jpf` application properties and then overlays command-line `+key=value` properties. Placing `+hprof.file` before the first free argument keeps it in the configuration argument set and preserves the checked-in `.jpf` for manual debugging.

Gradle's `JavaExec` and `Exec` tasks propagate nonzero child-process exits. The task also checks dump existence and size. Local `RunJPF` catches JPF exceptions and does not reliably convert modeled property violations into a nonzero process exit, so the smallest reliable additional check captures and reprints standard output and requires the established success diagnostics: exact importer counts, root binding, host graph validation, modeled pre-GC validation, controlled GC, modeled post-GC validation, and `no errors detected`.

Two consecutive runs succeeded. Each regenerated the dump without weakening the generator's overwrite protection; differing HPROF IDs confirmed fresh process/dump input. `./gradlew clean` removed the generated HPROF as part of the normal `build/` deletion, while the manual `src/examples/hprof-pass-a.hprof` fixture remained intact.

## Pass E.1 — Graph Identity Semantics

The original `./gradlew hprofSmokeTest` Foo/Bar/int[] workflow is frozen and remains a separate passing baseline. E.1 adds `./gradlew hprofGraphIdentityTest` with a distinct build-owned dump at `build/hprof-smoke/hprof-graph-identity.hprof`.

The real-JVM graph is exactly:

```text
Graph(marker=99)
  left -----+
  right ----+--> Node(a,id=1) --> Node(b,id=2) --+
  nullable = null        ^                         |
                         +-------------------------+
```

The generator retains exactly one Graph and two Nodes through a private static volatile generator root. The modeled holder has a separate default-null static root. Generation and JPF execution remain separate processes.

HAHA 2.0.4 returns Java `null` from `ClassInstance.FieldValue.getValue()` for the null `Type.OBJECT` field. The existing importer already initializes `targetRef` to `MJIEnv.NULL`, writes it with `ElementInfo.setReferenceField(...)`, and increments `references` for every restored reference field, including null. No importer correction was required. In this checkout `MJIEnv.NULL` is `0`.

The second fresh regression run observed:

```text
Graph   HPROF=0x70b73f770 -> JPF=191
Node(a) HPROF=0x70b73f5e0 -> JPF=192
Node(b) HPROF=0x70b73f5f8 -> JPF=193
```

IDs vary between generated dumps and are never hard-coded. Host validation proved `Graph.left == Graph.right == refMap[aId]`, `a.next == refMap[bId]`, `b.next == refMap[aId]`, and `Graph.nullable == MJIEnv.NULL`. Modeled Java proved the same topology and primitive values before and after `System.gc()` plus a forced transition boundary.

```text
[HPROF-JPF] import complete: objects=3 arrays=0 mappings=3 primitiveFields=3 references=5 arrayElements=0
[HPROF-JPF] null verified: Graph.nullable=0
[HPROF-JPF] alias verified: Graph.left=ref 192 Graph.right=ref 192 Node(a)=ref 192
[HPROF-JPF] cycle verified: Node(a)=ref 192 -> Node(b)=ref 193 -> Node(a)=ref 192
[HPROF-JPF-MODEL] pre-GC graph identity semantics verified
[HPROF-JPF] graph identity controlled GC verified: cycles=1
[HPROF-JPF-MODEL] post-GC graph identity semantics verified
[HPROF-JPF-MODEL] graph identity semantics verified
```

`HprofTestRootBinder` is a small test-only generalization accepting an exact source HPROF class, modeled holder class, and static field. `HprofPassASmokeRootBinder` remains as the frozen Foo/Bar wrapper. This mechanism still is not generic HPROF static-field or GC-root reconstruction.

No architectural correction was exposed: allocating all selected instances before reference population naturally preserves nulls, aliases, forward/back references, and cycles without traversal recursion or duplicate shells.

## Pass E.2 — Object-Array Topology

The isolated ./gradlew hprofObjectArrayTest regression generates build/hprof-smoke/hprof-object-array.hprof in a real HotSpot process, then runs a separate JPF process. The original basic and graph-identity fixtures remain unchanged.

~~~text
ArrayGraph(marker=77)
  anchor ---------------------> Node(a,id=1)
  nodes -> Node[4]                 |  ^     |
             [0] -----------------+  |     +-- links -> same Node[]
             [1] -> Node(b,id=2) ----+          (Node[] -> a -> Node[] cycle)
             [2] -> same Node(a)
             [3] -> null
Node(a).peer -> Node(b)
Node(b).peer -> Node(a)
Node(b).links -> null
~~~

HAHA 2.0.4 reports the array as Type.OBJECT. ArrayInstance.getValues() is declared Object[] and returns runtime [Ljava.lang.Object;. Non-null entries are com.squareup.haha.perflib.ClassInstance objects (therefore Instance) and a null entry is Java null, not an HPROF-ID scalar.

For HPROF class name [LHprofObjectArrayGraph$Node;, local GenericHeap.newArray(...) expects element signature LHprofObjectArrayGraph$Node; (the leading array bracket is omitted). The resulting JPF ClassInfo name is [LHprofObjectArrayGraph$Node;. Pass B writes and verifies elements with ElementInfo.setReferenceElement(index, ref) and getReferenceElement(index).

Pass A continues to select ordinary instances only by exact hprof.classes names. It discovers and deduplicates only supported arrays directly referenced by selected objects. Object-array elements are not recursively selected: every non-null element must already have a refMap entry, which E.2 ensures by explicitly selecting both ArrayGraph and Node. Missing mappings and unsupported shapes/types fail clearly.

~~~text
[HPROF-JPF] import complete: objects=3 arrays=1 mappings=4 primitiveFields=3 references=6 arrayElements=4
[HPROF-JPF] object-array mappings: ArrayGraph HPROF=0x70ae3f7e0 JPF=191 Node(a) HPROF=0x70ae3f5c0 JPF=192 Node(b) HPROF=0x70ae3f5d8 JPF=193 Node[] HPROF=0x70ae3f660 JPF=197
[HPROF-JPF] object-array alias verified: graph.anchor=ref 192 nodes[0]=ref 192 nodes[2]=ref 192
[HPROF-JPF] object-array null verified: nodes[3]=0
[HPROF-JPF] object-array cycle verified: nodes=ref 197 -> Node(a)=ref 192 -> links=ref 197
[HPROF-JPF-MODEL] pre-GC object-array topology verified
[HPROF-JPF] object-array controlled GC verified: cycles=1
[HPROF-JPF-MODEL] post-GC object-array topology verified
~~~

references=6 counts every selected ordinary Type.OBJECT field, including Node(b).links=null. arrayElements=4 counts restored payload slots for both supported int and object arrays, including null reference elements.

The listener uses optional configured hprof.test_support.class through the small HprofTestSupport lifecycle interface. E.2 therefore adds no third hard-coded fixture branch. Older smoke and graph-identity routing remains unchanged.

Two consecutive ./gradlew hprofObjectArrayTest --no-daemon runs succeeded. Separate hprofSmokeTest and hprofGraphIdentityTest runs succeeded, and the updated hprofRegressionTest aggregate ran all three successfully. Every JPF execution ended with no errors detected.

Current limitation: object arrays are directly referenced, one-dimensional arrays only; their non-null elements must be among explicitly selected ordinary classes. No arbitrary recursive heap traversal was added.

Recommended next semantic dimension: inherited instance fields, because they test field-layout fidelity without adding scalar types.

## Pass E.3 — Inheritance / Field-Layout Fidelity

The isolated ./gradlew hprofInheritanceTest regression generates build/hprof-smoke/hprof-inheritance.hprof in a real HotSpot JVM and imports it in a separate JPF process. Its selected ordinary objects are one InheritanceGraph, one Derived, one HiddenDerived, and one Ref. Superclasses are metadata/layout contributors, not separate heap objects.

The concrete state is:

~~~text
InheritanceGraph(marker=99)
  derived -> Derived
               Base.baseValue = 11
               Derived.derivedValue = 22
               Base.baseRef ------+
               Derived.derivedRef -+-> Ref(id=7)
  hidden -> HiddenDerived
               HiddenBase.value = 31
               HiddenDerived.value = 32
~~~

HAHA 2.0.4 ClassInstance.getValues() includes inherited fields. Its implementation begins with instance.getClassObj(), appends values for every Field in that ClassObj.getFields() declaration array, then repeats for getSuperClassObj() until null. Thus ordering is most-derived class first, declaration-array order within each class, followed by each superclass. ClassInstance.FieldValue contains only Field and value; Field contains only name and Type. Neither carries a declaring ClassObj.

The importer reconstructs declaring ownership by replaying the exact hierarchy traversal implemented by HAHA while consuming the flattened values in lockstep. It requires each flattened FieldValue.getField() to be the identical Field object from the corresponding ClassObj.getFields() slot and fails on length/order disagreement. The resulting semantic identity is declaring ClassObj plus field name and HAHA type.

For JPF, ClassInfo.getInstanceField(name) searches most-derived to superclass and therefore name-only mutation is ambiguous for hidden fields. E.3 instead resolves ClassLoaderInfo.getSystemResolvedClassInfo(declaringName).getDeclaredInstanceField(name). The returned FieldInfo records its declaring ClassInfo, field index, signature, and storage offset. ElementInfo.setIntField(FieldInfo, int) and setReferenceField(FieldInfo, int) write the exact slot; getIntField(FieldInfo) and getReferenceField(FieldInfo) verify it. HiddenBase.value and HiddenDerived.value have different storage offsets.

Observed diagnostics:

~~~text
[HPROF-JPF] field ... class=HprofInheritanceGraph$Derived field=derivedValue value=22
[HPROF-JPF] reference ... class=HprofInheritanceGraph$Base field=baseRef ... targetJPF=194
[HPROF-JPF] field ... class=HprofInheritanceGraph$Base field=baseValue value=11
[HPROF-JPF] field ... class=HprofInheritanceGraph$HiddenDerived field=value value=32
[HPROF-JPF] field ... class=HprofInheritanceGraph$HiddenBase field=value value=31
[HPROF-JPF] import complete: objects=4 arrays=0 mappings=4 primitiveFields=6 references=4 arrayElements=0
[HPROF-JPF] inherited fields verified: Base.baseValue=11 Derived.derivedValue=22
[HPROF-JPF] inherited reference alias verified: Base.baseRef=ref 194 Derived.derivedRef=ref 194
[HPROF-JPF] hidden fields verified: HiddenBase.value=31 HiddenDerived.value=32
[HPROF-JPF-MODEL] inheritance field layout verified
[HPROF-JPF] inheritance controlled GC verified: cycles=1
[HPROF-JPF-MODEL] post-GC inheritance field layout verified
~~~

The modeled target reads inherited fields through normal Java access and distinguishes hidden values by casting the same object to HiddenBase. Host validation additionally resolves both declaring FieldInfo objects and proves their storage offsets and values are distinct. The inherited and subclass references both retain the same imported Ref identity before and after GC.

Two consecutive ./gradlew hprofInheritanceTest --no-daemon runs succeeded. The updated ./gradlew hprofRegressionTest --no-daemon aggregate ran the basic, graph-identity, object-array, and inheritance fixtures successfully; every JPF run ended with no errors detected.

No fundamental representation loss was found. HAHA omits declaring-class information from individual FieldValue objects, but its deterministic hierarchy traversal plus the original per-ClassObj Field arrays preserve enough information to reconstruct ownership exactly. The importer checks that assumption at runtime rather than inferring ownership from names.

## Session Handoff — Pass E.3

- Changed: field population now reconstructs HAHA declaring-class ownership and uses JPF FieldInfo mutation; added an isolated inheritance/field-hiding fixture, validator, and durable Gradle regression.
- Counts: objects=4, arrays=0, mappings=4, primitiveFields=6, references=4, arrayElements=0.
- Proven: inherited int fields, inherited object references, aliasing across superclass/subclass fields, distinct same-name hidden fields, modeled access, and preservation through deliberate JPF GC.
- Existing regressions: all remained unchanged in semantics and pass in the four-fixture aggregate.
- Current blocker: none for Pass E.3.
- Recommended next semantic dimension: additional primitive scalar types, implemented as a deliberate type-by-type coverage matrix rather than mixed with further topology changes.

## Pass F.1 — Complete Primitive Scalar Coverage

The isolated `./gradlew hprofPrimitiveScalarTest --no-daemon` regression generates `build/hprof-smoke/hprof-primitive-scalars.hprof` in a real HotSpot JVM and imports it in a separate JPF host process. The selected graph contains exactly one `PrimitiveGraph(marker=55)` referencing one `PrimitiveValues` instance with these non-default values:

~~~text
booleanValue = true
byteValue    = -7
charValue    = U+03A9
shortValue   = 30000
intValue     = 123456789
longValue    = 0x0123456789ABCDEF
floatValue   = 13.25f
doubleValue  = -12345.125
~~~

HAHA 2.0.4's `Instance.readValue(Type)` and the generated fixture agree on the following exact representation and local JPF API mapping. Every write and read uses the declaring-class-aware `FieldInfo` resolved with `ClassInfo.getDeclaredInstanceField(name)`.

| Java type | HAHA Type | HAHA runtime value | JPF write/read API | Status |
|---|---|---|---|---|
| `boolean` | `BOOLEAN` | `java.lang.Boolean` | `setBooleanField` / `getBooleanField` | verified |
| `byte` | `BYTE` | `java.lang.Byte` | `setByteField` / `getByteField` | verified |
| `char` | `CHAR` | `java.lang.Character` | `setCharField` / `getCharField` | verified |
| `short` | `SHORT` | `java.lang.Short` | `setShortField` / `getShortField` | verified |
| `int` | `INT` | `java.lang.Integer` | `setIntField` / `getIntField` | verified |
| `long` | `LONG` | `java.lang.Long` | `setLongField` / `getLongField` | verified |
| `float` | `FLOAT` | `java.lang.Float` | `setFloatField` / `getFloatField` | verified |
| `double` | `DOUBLE` | `java.lang.Double` | `setDoubleField` / `getDoubleField` | verified |

No signedness or width conversion is performed by the importer: it validates the exact HAHA wrapper and passes the unboxed Java value to the corresponding typed JPF `ElementInfo` API. This preserves the negative byte, non-ASCII char, large short, and 64-bit long without normalization. Host-side validation compares floating-point raw bits after JPF readback:

~~~text
[HPROF-JPF] primitive FLOAT floatValue=13.25 bits=0x41540000
[HPROF-JPF] primitive DOUBLE doubleValue=-12345.125 bits=0xC0C81C9000000000
~~~

The durable regression observed:

~~~text
[HPROF-JPF] import complete: objects=2 arrays=0 mappings=2 primitiveFields=9 references=1 arrayElements=0
[HPROF-JPF-MODEL] primitive scalar values verified
[HPROF-JPF] primitive-scalar controlled GC verified: cycles=1
[HPROF-JPF-MODEL] post-GC primitive scalar values verified
no errors detected
~~~

The counts are two ordinary objects, no arrays, two mappings, nine primitive fields (`marker` plus eight values), one object-reference field, and no array elements. Two consecutive primitive-scalar task runs succeeded. The updated `./gradlew hprofRegressionTest --no-daemon` aggregate ran the basic, graph-identity, object-array, inheritance, and primitive-scalar regressions successfully without weakening earlier validators.

F.1 changes scalar instance-field coverage only. Primitive arrays remain limited to the existing `int[]` implementation; floating-point edge cases such as NaN payloads, infinities, and signed zero were deliberately deferred.

## Session Handoff — Pass F.1

- Changed: `JpfHeapImporter` now dispatches every primitive scalar `Type` to its exact declaring-aware typed `ElementInfo` API and rejects unexpected HAHA wrappers.
- Fixture: `PrimitiveGraph(marker=55)` references one `PrimitiveValues` containing all eight primitive scalar types.
- Counts: objects=2, arrays=0, mappings=2, primitiveFields=9, references=1, arrayElements=0.
- Proven: exact scalar width/signedness, U+03A9 char fidelity, 64-bit long fidelity, float/double raw-bit fidelity, modeled access, and post-GC preservation.
- Regressions: the new task passed twice; the five-fixture aggregate passed with every JPF run reporting `no errors detected`.
- Current blocker: none for Pass F.1.
- Recommended next milestone: Pass F.2, complete primitive-array coverage while keeping scalar and topology semantics frozen.

## Pass F.2 — Complete Primitive Array Coverage

The isolated `./gradlew hprofPrimitiveArrayTest --no-daemon` regression generates `build/hprof-smoke/hprof-primitive-arrays.hprof` in a real HotSpot JVM and imports it in a separate JPF host process. The selected graph is one `PrimitiveArrayGraph(marker=66)` with eight directly referenced length-three primitive arrays.

HAHA 2.0.4 `ArrayInstance.getValues()` is declared `Object[]` and returns a runtime `Object[]` for every primitive array. Each element is boxed according to `getArrayType()`. Local `Heap.newArray(elementSignature, length, ti)` accepts the following primitive JVM signatures and produces the corresponding array `ClassInfo` name:

| Java array | HAHA Type | HAHA element wrapper | JPF allocation signature / class | JPF element write/read API | Status |
|---|---|---|---|---|---|
| `boolean[]` | `BOOLEAN` | `Boolean` | `Z` / `[Z` | `setBooleanElement` / `getBooleanElement` | verified |
| `byte[]` | `BYTE` | `Byte` | `B` / `[B` | `setByteElement` / `getByteElement` | verified |
| `char[]` | `CHAR` | `Character` | `C` / `[C` | `setCharElement` / `getCharElement` | verified |
| `short[]` | `SHORT` | `Short` | `S` / `[S` | `setShortElement` / `getShortElement` | verified |
| `int[]` | `INT` | `Integer` | `I` / `[I` | `setIntElement` / `getIntElement` | verified |
| `long[]` | `LONG` | `Long` | `J` / `[J` | `setLongElement` / `getLongElement` | verified |
| `float[]` | `FLOAT` | `Float` | `F` / `[F` | `setFloatElement` / `getFloatElement` | verified |
| `double[]` | `DOUBLE` | `Double` | `D` / `[D` | `setDoubleElement` / `getDoubleElement` | verified |

The importer retains the existing selection boundary: only arrays directly referenced by explicitly selected ordinary instances are collected and deduplicated by HPROF ID. Primitive allocation uses an exact `Type`-to-signature helper. Primitive payload dispatch validates the exact wrapper before calling the matching typed `ElementInfo` setter. `Type.OBJECT` remains on its existing reference-element path.

Host validation proved array identity, `[Z/[B/[C/[S/[I/[J/[F/[D` class identity, length, element ordering, signed integral widths, `U+03A9`, and `U+0000`. Floating-point JPF readback was compared by raw bits:

~~~text
float[]  = {0xC1540000, 0x00000000, 0x41540000}
double[] = {0xC0C81C9000000000, 0x0000000000000000, 0x40C81C9000000000}
~~~

Observed regression summary:

~~~text
[HPROF-JPF] import complete: objects=1 arrays=8 mappings=9 primitiveFields=1 references=8 arrayElements=24
[HPROF-JPF-MODEL] primitive arrays verified
[HPROF-JPF] primitive-array controlled GC verified: cycles=1
[HPROF-JPF-MODEL] post-GC primitive arrays verified
no errors detected
~~~

All eight modeled reference fields retain the original Pass A array references before and after GC. Counts are one ordinary object, eight arrays, nine mappings, one primitive field, eight reference fields, and 24 array payload elements.

Two consecutive primitive-array task runs succeeded. The updated `./gradlew hprofRegressionTest --no-daemon` aggregate ran the basic, graph-identity, object-array, inheritance, primitive-scalar, and primitive-array fixtures successfully; every JPF execution ended with `no errors detected`.

F.2 adds no recursive selection, statics, roots, strings, class loaders, or execution state. Floating edge cases such as NaN payloads, infinities, and negative zero remain deliberately outside this fixture.

## Session Handoff — Pass F.2

- Changed: primitive-array allocation now maps every primitive HAHA `Type` to its exact JVM element signature; payload reconstruction uses exact boxed-wrapper validation and typed JPF element setters.
- Fixture: one rooted `PrimitiveArrayGraph(marker=66)` with eight length-three primitive arrays.
- Counts: objects=1, arrays=8, mappings=9, primitiveFields=1, references=8, arrayElements=24.
- Proven: allocation and payload fidelity for all eight primitive arrays, array identity, signedness/width, char values, floating raw bits, modeled access, and post-GC preservation.
- Regressions: the new task passed twice; the six-fixture aggregate passed without weakening prior validators.
- Current blocker: none for Pass F.2.
- Recommended next milestone: deliberately choose the next state category; strong candidates are generic static-field reconstruction or String representation, while HPROF roots and execution state remain larger architectural steps.

## Pass G.1 — Generic Static-Field Reconstruction

G.1 introduces an independent `hprof.static_classes` selection boundary. `hprof.classes` still selects ordinary instances; `hprof.static_classes` selects exact HPROF `ClassObj` records whose declared static storage is restored. Existing callers use the original importer overload, which delegates with an empty static-class set and reports zero static counters.

HAHA 2.0.4 exposes statics through `ClassObj.getStaticFieldValues()`. It returns a fresh `HashMap<Field,Object>` by reading the static count and the class record's own `mStaticFields` array; there is no superclass traversal, so entries are declared-only. `Field` contains name and `Type`, while the selected `ClassObj` supplies unambiguous declaring-class identity. Primitive values use the established boxed wrappers, non-null objects are `ClassInstance`, arrays are `ArrayInstance`, and null references are Java `null`.

The supported JPF lifecycle is:

~~~text
ClassLoaderInfo.getSystemResolvedClassInfo(binaryName)
  -> require ClassInfo.getClinit() == null
  -> ClassInfo.registerClass(ti), if not already registered
  -> ClassInfo.initializeClass(ti), requiring no pushed frame
  -> require ClassInfo.isInitialized()
  -> ClassInfo.getModifiableStaticElementInfo()
  -> ClassInfo.getDeclaredStaticField(name)
  -> typed ElementInfo FieldInfo setter
~~~

Registration creates and initializes the default `StaticElementInfo` storage. For a class without `<clinit>`, `initializeClass` marks it initialized synchronously and returns false. The importer writes snapshot values only after that sequence, so subsequent modeled `GETSTATIC` does not prepare or zero the storage again. A selected class with `ClassInfo.getClinit()!=null` fails explicitly because HPROF class-initialization state is not reconstructed. The fixture was verified with `javap`: `HprofStaticStateGraph` contains only its constructor and has no `<clinit>`.

The fixture selects `HprofStaticStateGraph$Payload` as an ordinary instance class and `HprofStaticStateGraph` as a static class. Its HPROF statics contain all eight primitive values, `payload` and `payloadAlias` pointing to one `Payload(id=4242)`, `nullable=null`, and `numbers` pointing to `int[]{4,5,6}`. Pass A discovers the array directly from the selected static map; non-array object targets must still be explicitly selected through `hprof.classes`. Pass B restores instance fields and array payloads, then populates selected static storage after every identity exists.

The new fixture has no `hprof.test_root.*` configuration. `HprofTestSupport` can now validate a rootless fixture and receives `MJIEnv.NULL`; `HprofStaticStateValidator` asserts that value, proving `HprofTestRootBinder` was not invoked.

Observed diagnostics:

~~~text
[HPROF-JPF] import complete: objects=1 arrays=1 mappings=2 primitiveFields=1 references=0 arrayElements=3 staticPrimitiveFields=8 staticReferences=4
[HPROF-JPF] static primitive values verified
[HPROF-JPF] static alias verified: payload=ref 191 payloadAlias=ref 191
[HPROF-JPF] static null verified: nullable=0
[HPROF-JPF] static array verified: numbers=ref 192 {4,5,6}
[HPROF-JPF] static fixture root binder absent: true
[HPROF-JPF-MODEL] reconstructed static state verified
[HPROF-JPF] static-state controlled GC verified: cycles=1
[HPROF-JPF-MODEL] post-GC reconstructed static state verified
no errors detected
~~~

`StaticElementInfo.markStaticRoot(Heap)` enumerates every declared static reference `FieldInfo`, reads its stored reference by storage offset, and calls `heap.markStaticRoot`. Consequently the imported static `payload` and `numbers` edges retained the original Pass A JPF references and their transitive state through deliberate GC. This proves HPROF static field to JPF static field to JPF GC-root semantics. It does not reconstruct other HPROF root-record categories such as JNI, thread-stack, monitor, or native roots.

`ImportResult` retains the previous instance counters and adds `staticPrimitiveFields` and `staticReferences`. G.1 observed objects=1, arrays=1, mappings=2, primitiveFields=1, references=0, arrayElements=3, staticPrimitiveFields=8, and staticReferences=4. Null static reference assignments count as restored static references, matching the established instance-reference convention.

Two consecutive `./gradlew hprofStaticStateTest --no-daemon` runs succeeded. The updated `./gradlew hprofRegressionTest --no-daemon` aggregate ran all seven fixtures successfully; every JPF execution ended with `no errors detected`.

## Session Handoff — Pass G.1

- Changed: optional exact static-class selection, static-array Pass A discovery, declared static primitive/reference population, and static-specific result counters.
- Lifecycle: resolve, reject `<clinit>`, register, synchronously initialize the no-`<clinit>` class, then mutate declared `StaticElementInfo` slots by `FieldInfo`.
- Proven: all primitive statics, object/array/null/alias statics, modeled direct access without test root binding, and JPF GC preservation through static root marking.
- Limitation: classes with `<clinit>` are rejected; exact class-initialization state reconstruction remains deferred.
- Regressions: the new task passed twice; the seven-fixture aggregate passed without weakening prior fixtures.
- Current blocker: none for the supported no-`<clinit>` G.1 boundary.
- Recommended next milestone: preserve the class-initialization limitation and investigate `String` representation as the next bounded heap-state category before general HPROF root records or execution state.

## Pass G.2 — Class Initialization State

Pass G.2 is a characterization result: **standard HPROF does not explicitly encode the lifecycle state needed to distinguish a class that is registered but has not executed `<clinit>` from one whose `<clinit>` completed.** The result is CASE B: JPF can represent both states if the correct state is supplied, but HPROF alone does not supply it.

### JPF state model in this checkout

JPF separates class resolution, registration, and initialization:

- Resolution creates/caches `ClassInfo`; it does not make the class visible to modeled code.
- `ClassInfo.registerClass(ThreadInfo)` creates `StaticElementInfo`, its modeled `java.lang.Class` object, and default/constant static storage. The new `StaticElementInfo.status` is `ClassInfo.UNINITIALIZED` (`-1`). Registration therefore cleanly represents loaded/registered but not initialized.
- Initialization in progress is represented by any non-negative status: the modeled thread id executing `<clinit>`. `ClassInfo.setInitializing(ThreadInfo)` installs that status.
- Successful completion is `ClassInfo.INITIALIZED` (`-2`). Normal/return paths (`RETURN`, `NATIVERETURN`, or `FINISHCLINIT` for a class without bytecode `<clinit>`) call the public `ClassInfo.setInitialized()` method.
- This checkout does not define a separate `INITIALIZATION_FAILED` status in `StaticElementInfo`; exceptional `<clinit>` behavior is handled through exception/unwind machinery rather than another persisted status constant. This is an additional lifecycle-fidelity question beyond the two-state G.2 fixture.

`StaticElementInfo`, not `ClassInfo`, stores the backtrackable status. `ClassInfo.needsInitialization(ThreadInfo)` and `initializeClass(ThreadInfo)` inspect it. Active-use bytecodes including `GETSTATIC`, `PUTSTATIC`, `INVOKESTATIC`, and `NEW` call class initialization before completing the original instruction. An already initialized class does not push `<clinit>` again; a registered `UNINITIALIZED` class with `<clinit>` does.

The supported JPF-side representation mechanisms are therefore:

- registered/uninitialized: call `registerClass(ti)` and leave the resulting status unchanged;
- restored initialized without executing modeled `<clinit>`: register the class, restore its static fields, and call the public `ClassInfo.setInitialized()` lifecycle API.

The second sequence is mechanically available and does not require reflection or private numeric status mutation. It is not used by the importer because the source checkpoint does not say which state is correct.

### Standard HPROF and HAHA metadata

HAHA 2.0.4's `HprofParser.loadClassDump()` reads the standard `CLASS_DUMP` layout: class object id, stack-trace serial, superclass id, class-loader id, signers id, protection-domain id, two reserved ids, instance size, constant-pool entries, static field name/type/value entries, and instance field name/type descriptors. It constructs `ClassObj` with class name, superclass, loader, size, fields, static values, and stack trace. Neither the record parser nor `ClassObj` exposes an initialized, initializing, failed, or `<clinit>`-completed field.

HAHA exposes GC root categories such as `RootType.SYSTEM_CLASS`, but a sticky/system-class root states reachability, not initialization lifecycle. The G.2 captures did not expose a subject-specific `SYSTEM_CLASS` root through HAHA in either state, and root presence must not be used as an initialization oracle.

The controlled HotSpot fixture uses `HprofClassInitState$InitSubject`, whose classfile has a real `<clinit>`:

~~~java
static int value;
static {
  value = 0;
  InitEffects.effectCount++;
}
~~~

Two independent real JVM processes capture:

1. `Class.forName(name, false, loader)`: `effectCount == 0`, proving the class is loaded but not initialized.
2. `Class.forName(name, true, loader)`: `effectCount == 1`, proving `<clinit>` executed exactly once.

In both HPROFs, the declared Java static `InitSubject.value` is `Type.INT`, boxed as `Integer`, and equals `0`. Thus declared static values are an ambiguous and invalid initialization-state heuristic.

On the current HotSpot, the uninitialized `CLASS_DUMP` additionally contains an implementation-injected `Type.OBJECT` pseudo-static named `<init_lock>` whose value HAHA represents as `ArrayInstance`; it is absent after initialization and absent from the classfile (`javap` shows only `value`). This is useful characterization evidence, but it is not an explicit standard HPROF status field or a portable lifecycle contract. The importer deliberately does not infer state from it.

### Regression and implication

`./gradlew hprofClassInitTest --no-daemon` creates fresh build-owned dumps in separate HotSpot processes:

- `build/hprof-smoke/hprof-class-init-uninitialized.hprof`
- `build/hprof-smoke/hprof-class-init-initialized.hprof`

`HprofClassInitCharacterizer` parses both with the production HAHA loader, verifies the external side-effect evidence printed by each generator, confirms the declared static ambiguity, records the HotSpot-specific `<init_lock>` observation, and reports `CASE_B`. The task is part of `hprofRegressionTest`.

No supplemental metadata prototype was added. Supplying and validating a capture-side class-state manifest is a separate design milestone; adding one here would prematurely choose a checkpoint format. No modeled continuation assertion is claimed in G.2 because choosing initialized versus uninitialized without that metadata would itself be the unsound operation under investigation.

**Research implication:** exact JVM checkpoint continuation cannot infer class initialization state from standard HPROF static contents. A complete capture must provide supplemental runtime metadata for at least loaded/registered, initializing (including ownership), initialized, and failed initialization semantics. Until then, generic static import continues to reject selected classes with `<clinit>`.

## Pass G.3 — Supplemental Class-Lifecycle Metadata V1

G.3 demonstrates the CASE B remedy experimentally. Standard HPROF remains the source of heap objects and static values; a separate, explicitly versioned checkpoint file supplies the class lifecycle bit that HPROF lacks. Together they produce correct modeled continuation for the two V1 states.

### Metadata V1

The optional listener property is:

~~~properties
hprof.lifecycle_file=/absolute/path/to/checkpoint.properties
~~~

The dependency-free, constrained properties-style schema is:

~~~properties
checkpoint.version=1
class.HprofClassInitState$InitSubject=UNINITIALIZED
~~~

or:

~~~properties
checkpoint.version=1
class.HprofClassInitState$InitSubject=INITIALIZED
~~~

`HprofCheckpointMetadata.load(File)` requires `checkpoint.version=1`, exact `class.<binary-name>` keys, and lifecycle values from the enum `{UNINITIALIZED, INITIALIZED}`. It preserves entries in an immutable map and distinguishes an absent lifecycle entry from both enum values. It rejects a missing/unsupported version, malformed lines, empty keys/values, duplicate keys, unknown keys, and unknown lifecycle values. The importer additionally rejects lifecycle entries not selected through `hprof.static_classes`; class resolution and exact HPROF `ClassObj` selection must also succeed.

V1 assumes the system/default modeled class loader and exact binary names. It does not encode `INITIALIZING`, the initializing thread, initialization failure, loader-qualified identity, modules, or any other runtime lifecycle data. This is a prototype checkpoint channel, not a commitment to the final dissertation capture format.

### Import sequencing

Existing callers without `hprof.lifecycle_file` receive empty metadata and retain G.1 behavior. A selected static class with `<clinit>` and no lifecycle entry still fails clearly. No-`<clinit>` classes continue through the original register/initialize/restore path without requiring metadata.

For a selected `<clinit>` class, the importer performs:

~~~text
resolve and register (StaticElementInfo.status == UNINITIALIZED)
  -> Pass A has already allocated all selected identities
  -> restore declared HPROF static fields
  -> metadata UNINITIALIZED: leave status unchanged
     metadata INITIALIZED: call public ClassInfo.setInitialized()
  -> modeled execution
~~~

The importer never calls `initializeClass()` for a selected `<clinit>` class during restoration, so import cannot push or execute `<clinit>`. For `INITIALIZED`, `setInitialized()` occurs only after all captured static values have been written. For `UNINITIALIZED`, the status remains `ClassInfo.UNINITIALIZED`; the first modeled active use follows normal JPF bytecode initialization and executes `<clinit>`.

HotSpot's G.2 `<init_lock>` pseudo-static is not parsed, consulted, or used as lifecycle input. Static dependency discovery now considers only source entries matching declared JPF static fields, and unmatched non-classfile HPROF entries receive a diagnostic and are ignored as non-modeled implementation data.

### Behavioral regression

`HprofClassInitState$InitEffects` has no `<clinit>` and is restored normally. `InitSubject.value` is zero in both captures, while `InitEffects.effectCount` is the independent behavioral oracle:

- uninitialized HPROF + explicit `UNINITIALIZED`: host validation observes status `-1` and `effectCount=0`; first modeled `GETSTATIC` executes `<clinit>`, changing the count to one; a second active use leaves it at one; final status is `-2`;
- initialized HPROF + explicit `INITIALIZED`: host validation observes status `-2` and captured `effectCount=1`; two modeled active uses leave the count at one, proving `<clinit>` was not rerun.

Both runs import only two primitive statics and no heap identities:

~~~text
objects=0 arrays=0 mappings=0 primitiveFields=0 references=0
arrayElements=0 staticPrimitiveFields=2 staticReferences=0
~~~

The durable task is:

~~~bash
./gradlew hprofClassLifecycleTest --no-daemon
~~~

It depends on the independent G.2 HotSpot dump tasks, then launches two separate JPF processes using the corresponding checked-in metadata files and modeled target arguments. It is also included in `hprofRegressionTest`.

A wrong-metadata negative control was deliberately omitted: it would require weakening or duplicating the host validator because the captured effect count is intentionally inconsistent with wrong lifecycle metadata. The two positive cases already demonstrate that externally supplied state—not static-value inference—controls continuation, while keeping the regression focused on valid checkpoints.

**Architectural result:** this is the first demonstrated JVM continuation-state component that requires information outside standard HPROF. HPROF supplies concrete heap/static values; supplemental metadata supplies lifecycle semantics; JPF combines both without replaying completed initialization or suppressing initialization that still must occur.

## Session Handoff — Pass G.3

- Changed: versioned supplemental lifecycle metadata loader, optional `hprof.lifecycle_file`, metadata-aware static import sequencing, and a static-only two-case continuation fixture.
- Proven: explicit `UNINITIALIZED` remains status `-1` until first modeled active use and runs `<clinit>` exactly once; explicit `INITIALIZED` begins at status `-2` and active use does not replay `<clinit>`.
- Isolation: no lifecycle inference uses HPROF values or HotSpot `<init_lock>`; existing no-`<clinit>` static behavior remains unchanged.
- Counts per case: objects=0, arrays=0, mappings=0, primitiveFields=0, references=0, arrayElements=0, staticPrimitiveFields=2, staticReferences=0.
- Regressions: two consecutive `hprofClassLifecycleTest` runs passed; the aggregate passed with `BUILD SUCCESSFUL` and 45 actionable tasks (31 executed, 14 up-to-date).
- Current limitation: V1 cannot represent initializing ownership, failed initialization, or loader-qualified class identity.
- Recommended next milestone: validate the metadata format's failure cases in focused unit tests, then decide whether loader-qualified identity or another bounded heap type is the next research priority.

## Pass G.4 — Checkpoint Metadata V1 Hardening

G.4 preserves the G.3 schema and reconstruction semantics while making the supplemental lifecycle channel fail closed. The valid V1 contract remains exactly:

~~~properties
checkpoint.version=1
class.<exact-binary-name>=UNINITIALIZED|INITIALIZED
~~~

Valid input requires the supported version, a non-empty exact binary class identity, and one explicit supported lifecycle value per entry. Invalid input includes a missing or unsupported version, malformed lines, duplicate keys, unknown keys, empty class identities or lifecycle values, unknown lifecycle values, lifecycle entries outside `hprof.static_classes`, unresolvable selected lifecycle classes, and selected `<clinit>` classes without lifecycle metadata. No default lifecycle is inferred. Checkpoint reconstruction must fail closed when required continuation metadata is absent or ambiguous.

`HprofCheckpointMetadataTest` provides lightweight parser coverage for the valid V1 values and seven malformed cases: missing version, version 999, unknown lifecycle, duplicate class entry, unknown key, empty class identity, and empty lifecycle value. Each rejection asserts both the specific cause and the source metadata filename.

`hprofCheckpointMetadataTest` combines that unit test with three focused JPF-process semantic checks:

- a selected `<clinit>` class without lifecycle metadata is rejected;
- metadata naming a class outside `hprof.static_classes` is rejected;
- metadata naming an unresolvable selected class is rejected clearly.

The existing `hprofClassLifecycleTest` remains the positive behavioral regression for valid `UNINITIALIZED` and `INITIALIZED` continuation. The hardening task is included in `hprofRegressionTest` without creating additional HPROF captures beyond the existing class-initialization fixture.

One launcher behavior matters for the negative checks: this checkout's `RunJPF` process can exit zero after a listener initialization exception. The semantic tasks therefore capture the combined process output and require both JPF's `[SEVERE] JPF exception` marker and the expected specific cause. Missing either fails Gradle. Parser failures use direct JUnit exception assertions and do not launch JPF.

G.4 changes neither metadata V1 nor reconstruction coverage. `INITIALIZING`, initializing-thread identity, initialization failure, loader-qualified identity, and multi-loader lifecycle remain unsupported.

## Pass H.1 — Loader-Qualified Class Identity

H.1 is a characterization result: **CASE A**. Standard HPROF preserves the defining class-loader object ID for each class dump, HAHA preserves and exposes the distinction, and JPF can represent same-named classes under distinct `ClassLoaderInfo` objects. The current importer and checkpoint schema do not yet use that information. This is an importer/schema limitation, not an HPROF information gap.

### Current name-only assumptions

- `hprof.classes` and `hprof.static_classes` are sets of unqualified class-name strings.
- `JpfHeapImporter` selects ordinary instances and static `ClassObj` records by `ClassObj.getClassName()` alone. Two matching ordinary definitions would both be allocated using `ClassLoaderInfo.getSystemResolvedClassInfo(name)`, collapsing their JPF class identity. Static selection explicitly requires exactly one matching `ClassObj` and therefore rejects duplicate definitions.
- Object allocation, declaring-class resolution for inherited/hidden fields, static `FieldInfo` resolution, and static-array component resolution all use the system loader by name.
- `HprofCheckpointMetadata` V1 stores lifecycle in `Map<String,ClassLifecycle>` and uses `class.<binary-name>` keys, so it cannot contain two lifecycle entries for the same name under different loaders.
- `HprofHeapBootstrap` parses both class-selection properties into name-only sets. Test root binding and fixture validators locate source instances/classes by name and resolve modeled holder/field classes through the system loader.

### HPROF and HAHA capability

The standard `CLASS_DUMP` record contains, in order relevant here, the class object ID, superclass object ID, and defining class-loader object ID. HAHA 2.0.4's `HprofParser.loadClassDump()` reads the loader ID and calls `ClassObj.setClassLoaderId(long)`. `ClassObj` stores it internally as `mClassLoaderId`; there is no public raw-ID getter. Public `ClassObj.getClassLoader()` resolves that ID through `Snapshot.findInstance(long)` and returns the exact loader `Instance`. Bootstrap-loaded classes therefore resolve ID zero to `null`; both custom loaders in the H.1 capture resolve to non-null `ClassInstance` objects. This association survives `resolveClasses()` and `resolveReferences()`.

HAHA indexes classes by HPROF class ID and by a name multimap. `Snapshot.findClasses(name)` returns both duplicate definitions; singular `Snapshot.findClass(name)` returns `null` when a heap contains more than one match, explicitly exposing ambiguity instead of choosing one. For this custom-defined packaged class, HAHA reports the class name in internal slash form `hprof/loader/Duplicate`, while the Java binary name used by `Class.forName` is `hprof.loader.Duplicate`.

The durable HotSpot fixture defines the same `hprof.loader.Duplicate` bytecode independently through two parentless custom loaders. The retained instances have `marker=111` and `marker=222`. A representative successful capture reported:

~~~text
classHPROF=0x70ae42528 loaderHPROF=0x70ae41c98 instanceHPROF=0x70ae42b48 marker=111
classHPROF=0x70ae42e20 loaderHPROF=0x70ae42080 instanceHPROF=0x70ae43020 marker=222
~~~

The IDs differ on every generated dump and are diagnostic identities, not durable checkpoint keys. For both instances, `ClassInstance.getClassObj()` returns the exact corresponding `ClassObj`, and that object's `getClassLoader()` returns the correct distinct loader. `HprofView` uses `Map<Long,ClassObj>` and `Map<Long,Instance>`, so it preserves both definitions and instances without a name-key collision. It intentionally has no convenience name index.

### JPF capability and current boundary

Each JPF `ClassLoaderInfo` owns its own `resolvedClasses: Map<String,ClassInfo>`. `getResolvedClassInfo(name)` resolves in that loader; `getResolvedClassInfo(name, byte[], offset, length)` creates a definition from loader-supplied bytes. When a class sourced from the same classpath URL is resolved by another loader, JPF clones the `ClassInfo` for that loader. `ClassInfo.getClassLoaderInfo()` identifies its defining loader, and its `uniqueId` combines the `ClassLoaderInfo` ID with the per-loader class/static ID. Thus JPF can represent two `ClassInfo` objects with one binary name under two loaders.

A non-system `ClassLoaderInfo` requires a modeled `java.lang.ClassLoader` object. Its native peer constructs the host-side `ClassLoaderInfo`, links its modeled parent and numeric ID, and registers it with the VM. Modeled `defineClass` supplies class bytes to the loader-specific resolution API and registers the resulting class. Reconstructing that loader object, parent graph, bytecode source, and definition state is deliberately outside H.1.

Consequently a future loader-aware importer needs a stable checkpoint class identity that includes defining-loader identity plus class name, and a mapping from captured loader identity to modeled `ClassLoaderInfo`. Raw HPROF loader object IDs establish identity inside one dump but are not yet accepted as a durable cross-artifact schema. Metadata V1 remains unchanged and intentionally assumes name uniqueness in the system/default modeled loader.

The focused regression is:

~~~bash
./gradlew hprofClassLoaderIdentityTest --no-daemon
~~~

It generates `build/hprof-smoke/hprof-class-loader-identity.hprof` in a real HotSpot process, parses it with production HAHA/HprofView code, and verifies two class IDs, two loader IDs, two instance IDs, exact instance-to-class-to-loader association, name-lookup ambiguity, and preservation by HprofView. It does not import the duplicate classes into JPF and does not change reconstruction semantics.

## Pass H.2 — Checkpoint Loader Identity and Minimum JPF Loader State

H.2 separates identity needed to correlate one checkpoint bundle from the target state needed to make a loader usable in JPF. It does not change metadata V1 or `JpfHeapImporter`.

### Identity scopes and available identifiers

- **Capture-local identity** is unique only within one HPROF. The class-object ID and defining-loader object ID provide this identity directly.
- **Checkpoint-bundle identity** correlates one HPROF with supplemental artifacts captured for that same checkpoint. This is sufficient to reconstruct that captured JVM state; it need not survive another JVM run.
- **Cross-capture identity** tries to recognize one conceptual loader across independent executions. Exact replay of one checkpoint does not require this stronger and often ill-defined identity.

Available identifiers have different properties:

| Candidate | HPROF-local uniqueness | HAHA visibility | Capture-side availability | Checkpoint use |
|---|---|---|---|---|
| `ClassObj.getId()` | unique class definition | public | not available from ordinary Java; assigned/encoded by dump machinery | strong bundle-local class key if metadata is correlated to this HPROF |
| `ClassObj.getClassLoader().getId()` | unique loader object (`0`/`null` for bootstrap) | public after reference resolution | not available as an HPROF ID from ordinary Java | strong bundle-local loader key if correlation/post-processing is available |
| `LOAD_CLASS` serial | unique load record within the dump | HAHA uses load records to associate names but does not expose the serial on `ClassObj` | JVM/dump-agent domain, not ordinary Java | inferior to direct class-object ID and not loader identity by itself |
| class name | not unique across loaders | public; slash form observed for custom packaged class | available | readable component, insufficient alone |
| loader class name | not unique across loader instances | through loader `Instance.getClassObj()` | available | diagnostic only |
| capture-assigned logical loader ID | unique if capture channel enforces it | not standard HPROF unless correlated through a field/tag/artifact | directly available to its capture agent | promising portable bundle key, but requires an explicit correlation mechanism |
| structural fingerprint | collision/semantic questions | derivable only from selected state | potentially | unnecessary for single-bundle replay and unsafe as the primary identity |

Raw HPROF IDs are therefore valid checkpoint-local/bundle-local identities, despite being unstable across dumps. The practical caveat is correlation: ordinary Java code cannot ask HotSpot for the future HPROF object ID. A supplemental capture producer would need dump-agent/JVMTI cooperation, post-process the HPROF, or emit a logical ID that can be correlated to the loader object. H.2 does not choose that mechanism.

A future semantic class identity should be:

~~~text
checkpoint loader identity + binary class name
~~~

A direct captured `ClassObj` identity is also an unambiguous bundle-local selection key. A future selection mechanism should select captured `ClassObj` identities rather than matching only `hprof.classes`/`hprof.static_classes` name strings.

### Loader object graph and class definition fidelity

The H.1 loader objects are ordinary `ClassInstance` records. HAHA exposes their inherited `java.lang.ClassLoader.parent` reference and application fields. The controlled parentless loaders contain:

~~~text
parent   -> null
label    -> java.lang.String
bytecode -> distinct byte[] objects, 299 bytes each, CAFEBABE classfile magic
~~~

The two retained byte arrays are byte-for-byte identical. This is fixture-specific: `DuplicateLoader` deliberately clones and stores the bytes in an ordinary field. HPROF captures those arrays because they are live heap state, not because class definitions are a standard HPROF feature.

Standard HPROF `CLASS_DUMP` supplies structural metadata needed to decode heap state: superclass and loader IDs, instance size, constant-pool value entries, static values, and instance-field name/type descriptors. HAHA `ClassObj` exposes that structure but no method bytecode, executable code attributes, or original classfile byte stream. Therefore standard HPROF alone is insufficient to recreate an arbitrary dynamically defined executable class. Definition bytes must come from configured classpath/resources, loader-specific sources, captured heap bytes when they happen to exist, or a supplemental checkpoint artifact. Real duplicate-name classes may also have different bytes, so loader identity alone is insufficient for exact definition fidelity.

HPROF can reconstruct ordinary loader fields, but that does not guarantee future loader behavior. Files, URLs, open archives, native handles, remote services, generated-class caches, and other external capabilities can be absent or stale. In this fixture the defining bytes are present, but executable loader behavior still also depends on the modeled loader class and its methods.

### Minimum usable JPF state

The modeled probe establishes the minimum working sequence in this checkout:

1. Allocate and construct a modeled `java.lang.ClassLoader` subclass object for each loader.
2. The `JPF_java_lang_ClassLoader` constructor peer resolves the modeled parent, constructs a host-side `ClassLoaderInfo(vm, loaderObjectRef, classPath, parent)`, assigns its `nativeId`, links `parent`, and calls `VM.registerClassLoader`.
3. Supply classfile bytes to modeled `defineClass`; its peer recovers the `ClassLoaderInfo` from the modeled object's `nativeId`, calls loader-specific `getResolvedClassInfo(name, bytes, offset, length)`, and registers the resulting `ClassInfo`/static storage.
4. Allocate instances using their respective loader-qualified `ClassInfo` definitions.

Merely reconstructing the ordinary heap fields of a loader object does **not** execute its constructor peer and therefore does not automatically create or register a `ClassLoaderInfo`. Both modeled and host-side state must be established consistently.

The probe defined `hprof.loader.Duplicate` under two modeled loaders and observed:

~~~text
loaderId=2 loaderObjectRef=1006 classUniqueId=0x200000000
loaderId=3 loaderObjectRef=1007 classUniqueId=0x300000000
~~~

Modeled Java verified distinct `Class` objects, correct distinct `getClassLoader()` results, and instances whose runtime classes remain separate. The host listener verified equal names, distinct `ClassInfo` objects, distinct `ClassLoaderInfo` objects, distinct modeled loader refs, and distinct `ClassInfo.getUniqueId()` values. A system-loader definition can also exist concurrently because the fixture bytecode is on the configured classpath; the assertions deliberately target the two non-system definitions.

The focused durable task is:

~~~bash
./gradlew hprofClassLoaderStateTest --no-daemon
~~~

It reuses the build-owned H.1 dump, validates the loader heap graph and incidental byte arrays with HAHA, runs the independent modeled-JPF duplicate-definition probe, and retains H.1 identity characterization. It does not import the captured loaders or objects.

### Classification and design consequence

- **Loader identity:** present in HPROF and exposed by HAHA.
- **Class definition bytecode:** absent from standard HPROF `ClassObj`; externally recoverable, or incidentally present in ordinary heap fields as in this fixture.
- **Loader ordinary heap state:** present and reconstructable to the extent supported ordinary fields/references are captured.
- **Loader external capability:** absent or fixture-dependent; not guaranteed by HPROF.
- **JPF target representation:** sufficient, but requires explicit modeled loader objects, registered `ClassLoaderInfo` state, parent linkage, and a definition source.
Metadata V1 remains binary-name keyed and system/default-loader-only. A future V2 must represent loader-qualified class identity, but H.2 intentionally defines no syntax. The next implementation must also decide how one checkpoint bundle correlates supplemental loader/definition artifacts with the HPROF loader object identity.


## Pass H.3 — Checkpoint-Bundle Correlation and Class Definitions

H.3 demonstrates a bundle-local correlation channel without changing lifecycle metadata V1 or `JpfHeapImporter`. Cross-capture identity is unnecessary for replay of one checkpoint: capture-assigned logical loader IDs `1`/`2` and logical class IDs `1`/`2` are unique only inside the generated bundle.

### Capture anchors and post-processing

The real-HotSpot capture defines two different classfiles with the same binary name, `hprof.loader.Duplicate`. Definition 1 implements `definitionMarker() == 111`; definition 2 implements `definitionMarker() == 222`. The exact byte arrays passed to `ClassLoader.defineClass` are written immediately as external artifacts. The custom loaders intentionally retain no bytecode field, so artifact availability does not depend on incidental loader heap state.

Reachable capture-only tags correlate identities that ordinary Java cannot know before a dump assigns HPROF IDs:

~~~text
LoaderTag(logicalLoaderId, loader reference)
ClassTag(logicalClassId, logicalLoaderId, loader reference)
~~~

After capture, `HprofCheckpointBundleBuilder` parses the HPROF with HAHA, locates the tags, follows each loader reference to its exact `Instance.getId()`, and resolves `loader Instance identity + hprof/loader/Duplicate` to the exact `ClassObj.getId()`. The two loader IDs and two class IDs are distinct. HPROF IDs are recorded only after dump generation and remain capture-local.

### Experimental bundle V1

The generated, build-owned layout is:

~~~text
build/hprof-smoke/class-loader-checkpoint/
  heap.hprof
  bundle.properties
  classes/
    class-1.class
    class-2.class
~~~

The manifest records `bundle.version=1`, the relative HPROF path and SHA-256, two logical-loader-to-HPROF-ID entries, and for each logical class its logical loader, exact binary name, HPROF ClassObj ID, relative artifact path, and SHA-256. Artifact paths must be relative, normalize inside the bundle root, and identify regular files. Hash mismatch fails closed.

A representative run produced distinct class hashes:

~~~text
class 1: 273680deeb5cef869fedd13863cf3a2c7f5533686a872f150d41e0d9fd65efdc
class 2: f2b0a84a0c48251c025180e6fc49a8969b2656fe08dc31ceee01fd9ac6fa2397
~~~

Both artifacts are valid executable classfiles because HotSpot defines and invokes them during capture, and the modeled JPF probe independently defines the packaged bytes under two modeled loaders and observes `{111,222}`. A negative control flips one artifact byte and verifies rejection by SHA-256 before class definition. The bundle verifier also reopens the exact HPROF, checks every recorded ClassObj exists, and verifies its name and defining-loader relationship.

The two supplemental channels remain distinct:

- lifecycle metadata V1 supplies semantic `INITIALIZED`/`UNINITIALIZED` state absent from standard HPROF;
- checkpoint class-definition artifacts supply exact executable bytes absent from standard HPROF.

Neither schema was merged or changed. This bundle layout is an experimental prototype, not metadata V2 or a final archive format.

The durable regression is:

~~~bash
./gradlew hprofCheckpointBundleTest --no-daemon
~~~

It performs the real-HotSpot capture, HAHA post-processing, integrity and semantic validation, one expected tamper rejection, and a separate RunJPF packaged-byte execution probe. It is included in `hprofRegressionTest`.

This solves exact captured definition availability and bundle correlation, not generic loader replay. Ordinary loader fields remain distinct from executable loader capability: open JARs, URLs, remote resources, native handles, future resource lookup, and arbitrary custom-loader behavior remain unsupported. A future loader-aware importer can consume the demonstrated semantic identity:

~~~text
bundle-local loader identity + binary class name
  -> captured loader HPROF ID + ClassObj HPROF ID + exact classfile artifact
~~~

but H.3 deliberately does not create the `HPROF loader -> ClassLoaderInfo` or `ClassObj -> ClassInfo` mappings.


## Pass H.4 — Loader-Qualified Class Reconstruction

H.4 adds the first bounded bundle-aware import path. Existing name-selected classes continue through system-loader resolution unchanged. When `hprof.bundle` is configured, the verified bundle supplies exact capture-local identities and executable definitions:

~~~text
captured loader HPROF ID -> modeled ClassLoaderInfo
captured ClassObj HPROF ID -> loader-qualified JPF ClassInfo
captured instance.getClassObj().getId() -> exact JPF ClassInfo -> JPF object
~~~

### Bundle configuration and validation

The listener accepts:

~~~properties
hprof.bundle=/absolute/path/to/class-loader-checkpoint
~~~

The bundle's manifest identifies the HPROF, so `hprof.file` is optional in this mode. If both are supplied, their canonical files must match. Before any JPF loader/class construction, `HprofCheckpointBundle.loadAndVerify` validates the bundle version, relative paths, HPROF SHA-256, class-artifact SHA-256 values, positive logical counts, logical-loader references, and unique non-null HPROF IDs. `HprofLoaderImportContext.create` then verifies each loader instance and ClassObj exists in the parsed dump, each ClassObj has the manifest loader, and slash/dot-normalized names match.

Bundle entries select exact HPROF `ClassObj` IDs; the ambiguous name-only `hprof.classes=hprof.loader.Duplicate` mechanism is not used. Name-only selection remains the system/default-loader path for existing fixtures.

### Minimum modeled loader and definition path

For each unique captured loader HPROF ID, `HprofLoaderImportContext` allocates one modeled `java.lang.ClassLoader` object and constructs one `ClassLoaderInfo` through a narrow subclass of the same protected constructor used by `JPF_java_lang_ClassLoader`:

~~~text
new ClassLoaderInfo(vm, modeledLoaderRef, new ClassPath(), modeledSystemLoader)
  -> writes nativeId
  -> links modeled parent
  -> creates per-loader Statics
  -> VM.registerClassLoader(...)
~~~

The controlled HotSpot loaders have `parent == null`; non-null captured parents fail as unsupported. JPF combines bootstrap and system definition lookup, so a captured null/bootstrap parent is represented by the modeled system loader for resolving `java.lang.Object`. During predefinition the bounded checkpoint loader disables modeled `loadClass` round trips and delegates dependency resolution natively to that system parent; a round trip cannot execute during `vmInitialized()`.

Each integrity-verified artifact is read and passed to:

~~~java
cli.getResolvedClassInfo(binaryName, bytes, 0, bytes.length)
ci.registerClass(threadInfo)
~~~

The result must have the requested name and the exact mapped `ClassLoaderInfo`; otherwise import fails. No system-loader classpath definition can satisfy this path.

### Exact allocation and field identity

`JpfHeapImporter` accepts an optional `HprofLoaderImportContext`. Bundle-selected objects are chosen by exact `ClassObj.getId()`, and Pass A uses the context's `ClassInfo` rather than resolving by name. Existing callers pass no context and retain prior behavior.

For a loader-qualified object, declaring-field resolution walks the HAHA `ClassObj` hierarchy and the actual allocated object's JPF `ClassInfo` hierarchy in parallel. It never calls `getSystemResolvedClassInfo(declaringName)` for those fields. The H.4 fixture extends only `java.lang.Object`; custom loader-qualified superclasses and interfaces remain deferred.

### Verified fixture

The H.3 generator now gives each same-named definition one captured field and one accessor:

~~~text
loader/class A:
  hprof.loader.Duplicate.marker = 1111
  definitionMarker() = 111

loader/class B:
  hprof.loader.Duplicate.marker = 2222
  definitionMarker() = 222
~~~

A host-side validator proves two loader mappings, two ClassObj/ClassInfo mappings, equal binary names, distinct `ClassLoaderInfo` objects, distinct `ClassInfo` objects and unique IDs, exact object-to-ClassInfo association, restored markers, and rejection of system-loader substitution.

Two test-only system-loader static `Object` fields expose the imported references to modeled code. Modeled reflection invokes `getMarker()` and `definitionMarker()` on both objects and verifies:

~~~text
fields={1111,2222}
methods={111,222}
~~~

This root holder is test infrastructure, not generic checkpoint-root reconstruction.

The focused regression is:

~~~bash
./gradlew hprofLoaderQualifiedImportTest --no-daemon
~~~

It regenerates/finalizes the H.3 bundle, launches a separate RunJPF process with `+hprof.bundle=<bundle>`, reconstructs loaders/classes/instances, executes both bundled definitions, and requires `no errors detected`. Two consecutive fresh-capture runs passed with changing HPROF IDs.

Import counts remain heap-state counts:

~~~text
objects=2 arrays=0 mappings=2 primitiveFields=2 references=0
arrayElements=0 staticPrimitiveFields=0 staticReferences=0
loader mappings=2 bundled class mappings=2
~~~

Current limitations remain deliberate: non-null custom loader parents, loader-qualified static reconstruction, loader-qualified lifecycle metadata, loader-qualified object-array component resolution, custom superclass/interface definition graphs, arbitrary loader capability replay, and future dynamic loading are unsupported or deferred. Lifecycle metadata V1 is unchanged.


## Pass H.5 — Loader-Qualified Class Dependency Graph

H.5 extends the bounded checkpoint-bundle path from leaf classes to an explicitly bundled dependency graph. The one-loader fixture contains three exact artifacts:

~~~text
hprof.loader.Contract
hprof.loader.Base
  baseField = 1111
  baseMethod() = 101
hprof.loader.Derived extends Base implements Contract
  derivedField = 2222
  derivedMethod() = 202
  interfaceMarker() = 303
~~~

All three definitions belong to the same captured parentless custom loader; only one `Derived` heap instance is selected. `Contract` and `Base` are dependency definitions and correctly require no synthetic heap instances. Bundle V1 already supports arbitrary class counts, so its format and lifecycle metadata V1 are unchanged.

### HPROF/classfile information split

`ClassObj.getSuperClassObj()` preserves the exact captured superclass object identity. In the fixture, HAHA proves that the `Derived` ClassObj points to the exact same-loader `Base` ClassObj. Standard HPROF `CLASS_DUMP` has no implemented-interface list, and HAHA `ClassObj` exposes no interface API. The integrity-bound classfile artifacts provide the executable superclass name and direct interface names.

`HprofBundledClassDependencies` uses the checkout's existing `gov.nasa.jpf.jvm.ClassFile` and `ClassFileReaderAdapter` callbacks (`setClass` and `setInterface`) to read only class headers. `HprofLoaderImportContext` combines those names with captured loader identity and HPROF superclass identity to build bundle-local dependency edges. Same-loader custom superclasses and non-`java.*` interfaces must have matching bundle entries; absence fails before definition and cannot fall back to the system classpath.

A deterministic depth-first topological definition produced:

~~~text
Contract -> Base -> Derived
bundled dependency edges = 2
~~~

JPF then resolves each artifact with `ClassLoaderInfo.getResolvedClassInfo(name, bytes, offset, length)` under the already mapped loader and registers it. Host checks prove:

~~~text
Derived.getSuperClass() == mapped Base
Derived.getInterfaceClassInfos().contains(mapped Contract)
Contract/Base/Derived ClassLoaderInfo == mapped custom loader
all three ClassLoaderInfo != system loader
~~~

### State and modeled dispatch proof

Pass A allocates the captured `Derived` using its exact ClassObj-ID-to-ClassInfo mapping. The existing parallel HAHA/JPF hierarchy walk resolves `Base.baseField` through the exact mapped Base `FieldInfo` and `Derived.derivedField` through the exact mapped Derived `FieldInfo`. The import counts are:

~~~text
objects=1 arrays=0 mappings=1 primitiveFields=2 references=0
arrayElements=0 staticPrimitiveFields=0 staticReferences=0
loader mappings=1 bundled classes=3 dependency edges=2
~~~

A test-only system-loader `Object` static exposes the imported Derived to modeled Java. Modeled reflective entry invokes bundled methods whose bodies exercise normal JPF dispatch: `callBase()` performs virtual dispatch to the inherited base method, `derivedMethod()` executes the derived definition, and `callInterface()` performs interface dispatch through the bundled Contract. The observed result is:

~~~text
[HPROF-JPF-MODEL] loader-qualified class hierarchy verified: fields={1111,2222} virtual={101,202} interface=303
~~~

The focused task is:

~~~bash
./gradlew hprofLoaderHierarchyImportTest --no-daemon
~~~

It creates a fresh build-owned bundle, correlates one loader and three ClassObjs, imports the instance, checks exact hierarchy identity, executes the modeled methods, and requires `no errors detected`. Its negative control copies the bundle without Contract and requires the precise `missing bundled custom interface ...` failure before any system fallback. This checkout's RunJPF reports listener-initialization failure with process status zero, so that expected-failure control keys on the explicit severe diagnostic; successful execution remains guarded by both modeled success output and `no errors detected`.

H.5 remains bounded to parentless captured loaders, explicitly bundled dependency sets, a system/bootstrap `java.lang.Object` boundary, ordinary instances, and no custom-type arrays. Non-null custom loader parents, loader-qualified statics/lifecycle, object arrays of loader-qualified components, arbitrary loader capability replay, and future dynamic loading remain unsupported or deferred.


## Pass I.1 — HPROF GC-Root Taxonomy and JPF Root Mapping

I.1 characterizes root records separately from object-graph reconstruction. It adds no generic keepalive set and no new importer semantics: preventing collection without reconstructing the runtime structure that owns a root would prove liveness only, not continuation semantics.

### HAHA 2.0.4 root parser inventory

The actual `HprofParser.loadHeapDump` switch supports these record tags. `id` below is the referenced object ID; `u4` fields are four-byte unsigned/serial values in the format even though HAHA reads them through `readInt()`.

| HPROF record/tag | Record payload parsed | HAHA representation | Provenance retained publicly? |
|---|---|---|---|
| `ROOT_UNKNOWN` (`0xff`) | object `id` | `RootObj(UNKNOWN,id)` | category and object |
| `ROOT_JNI_GLOBAL` (`0x01`) | object `id`, JNI-global-ref `id` | `RootObj(NATIVE_STATIC,id)` | category/object; second native ID discarded |
| `ROOT_JNI_LOCAL` (`0x02`) | object `id`, thread serial `u4`, frame number `u4` | `RootObj(NATIVE_LOCAL,id,thread,stack-at-depth)` | category/object public; thread/stack stored without public getters; original frame number not exposed |
| `ROOT_JAVA_FRAME` (`0x03`) | object `id`, thread serial `u4`, frame number `u4` | `RootObj(JAVA_LOCAL,id,thread,stack-at-depth)` | same partial exposure |
| `ROOT_NATIVE_STACK` (`0x04`) | object `id`, thread serial `u4` | `RootObj(NATIVE_STACK,id,thread,thread-stack-trace)` | same partial exposure |
| `ROOT_STICKY_CLASS` (`0x05`) | class-object `id` | `RootObj(SYSTEM_CLASS,id)` | category and exact `ClassObj` |
| `ROOT_THREAD_BLOCK` (`0x06`) | object `id`, thread serial `u4` | `RootObj(THREAD_BLOCK,id,thread,thread-stack-trace)` | category/object public; thread/trace hidden |
| `ROOT_MONITOR_USED` (`0x07`) | object `id` | `RootObj(BUSY_MONITOR,id)` | category and object; record carries no owner |
| `ROOT_THREAD_OBJECT` (`0x08`) | thread object `id`, thread serial `u4`, stack-trace serial `u4` | internal `ThreadObj(id,stackTrace)` keyed by thread serial | **not added to `Snapshot.getGCRoots()`; no public `ThreadObj` getters** |
| `ROOT_INTERNED_STRING` (`0x89`) | object `id` | `RootObj(INTERNED_STRING,id)` | category and object |
| `ROOT_FINALIZING` (`0x8a`) | object `id` | `RootObj(FINALIZING,id)` | category and object |
| `ROOT_DEBUGGER` (`0x8b`) | object `id` | `RootObj(DEBUGGER,id)` | category and object |
| `ROOT_REFERENCE_CLEANUP` (`0x8c`) | object `id` | `RootObj(REFERENCE_CLEANUP,id)` | category and object |
| `ROOT_VM_INTERNAL` (`0x8d`) | object `id` | `RootObj(VM_INTERNAL,id)` | category and object |
| `ROOT_JNI_MONITOR` (`0x8e`) | object `id`, thread serial `u4`, stack depth `u4` | `RootObj(NATIVE_MONITOR,id,thread,stack-at-depth)` | category/object public; thread/stack hidden |
| `ROOT_UNREACHABLE` (`0x90`) | object `id` | `RootObj(UNREACHABLE,id)` | category and object |

HAHA's `RootType.INVALID_TYPE` and `JAVA_STATIC` enum values are not produced by this parser switch. Java static field targets are not emitted as individual `ROOT_JAVA_STATIC` records by this format path; `ROOT_STICKY_CLASS` roots a class, while its `CLASS_DUMP` contains the static values.

`Snapshot.getGCRoots()` therefore retains root category and referenced identity for `RootObj` records, and `RootObj.getReferredInstance()` resolves the identity. It does not provide the complete record provenance. `RootObj.mThread`, inherited `Instance.mStack`, `ThreadObj` fields, and `StackTrace` metadata are package-private/protected with no public getters. In addition to hidden metadata, JNI-global's native reference ID is discarded and thread-object records are omitted from the public root collection. These are HAHA/parser API losses, not HPROF information gaps.

### Controlled HotSpot characterization

`HprofRootCharacterizationDumpGenerator` creates three marker objects:

~~~text
kind=1 -> static field HprofRootCharacterizationDumpGenerator.staticMarker
kind=2 -> live worker Java local
kind=3 -> live worker Java local used as an owned synchronized monitor
worker  -> live java.lang.Thread object
~~~

The worker remains inside `synchronized(monitor)` with both locals live while the main thread captures a build-owned HPROF. The regression searches by marker identity rather than asserting global root counts. Across fresh captures it observed:

~~~text
static marker -> captured static field identity; no direct RootObj
Java local    -> JAVA_LOCAL
monitor       -> JAVA_LOCAL only; no BUSY_MONITOR record from this HotSpot capture
Thread object -> another JAVA_LOCAL reference visible, but THREAD_OBJECT provenance absent from getGCRoots()
~~~

A representative category count was `{SYSTEM_CLASS=681, NATIVE_LOCAL=1, NATIVE_STATIC=43, JAVA_LOCAL=24}`. Counts are diagnostic only and intentionally not regression assertions. A Java synchronized block is therefore insufficient to deterministically induce `ROOT_MONITOR_USED` in this HotSpot configuration; monitor-root occurrence remains uncharacterized rather than being synthesized.

### JPF semantic mapping

JPF `GenericHeap.mark()` marks `pinDownList`, then `ThreadList.markRoots`, then `ClassLoaderList.markRoots`, and recursively traverses the resulting objects. The relevant mappings and boundaries are:

| HPROF root category | JPF semantic structure | Liveness reconstructable now? | Semantics reconstructable now? | Missing prerequisite/status |
|---|---|---:|---:|---|
| sticky/system class | registered `ClassInfo`/`StaticElementInfo`; class object and reference statics marked by `markStaticRoot` | yes where class/static state is reconstructed | yes for verified no-`<clinit>`/metadata-supported static cases | generic Class-object/root selection still bounded |
| static field target | `StaticElementInfo` reference slot, reached via `ClassLoaderList -> Statics.markRoots` | verified | verified | G.1; do not duplicate with root table |
| Java frame/local | reference-typed `StackFrame` local or operand slot, marked through `ThreadInfo.markRoots` | only by synthetic pin/root | no | ThreadInfo, frame chain, slots, reference bitmap, PC |
| thread object | `ThreadInfo.objRef`, marked by `ThreadInfo.markRoots` | only by synthetic pin/root | no | ThreadInfo lifecycle/scheduler state; ordinary Thread object is insufficient |
| JNI local/native stack | `NativeStackFrame`/thread roots where a peer frame retains references | only approximation | no | native peer frame plus owning thread; HAHA hides thread/trace metadata |
| JNI global | no reconstructed JNI-global table was found; `GenericHeap.pinDown` can preserve liveness | approximation possible | no | native/MJI global-reference ownership; HAHA discards native reference ID |
| thread block | ThreadInfo target/lock/block execution state | approximation possible | no | owning thread and block state; HAHA hides thread metadata |
| monitor used/JNI monitor | `ElementInfo.Monitor` plus owning/waiting/blocked `ThreadInfo` relationships | approximation possible | no | monitor ownership/count/wait sets and threads; basic record lacks owner |
| interned string | heap intern table/pinned implementation plus String semantics | not currently | no | String reconstruction and intern-table state |
| finalizing | JPF finalizer queue/`FinalizerThreadInfo` | approximation possible | no | finalization queue and thread state |
| reference cleanup | weak/reference processing state | approximation possible | no | reference-handler/queue semantics |
| debugger, VM internal, unknown, unreachable | no portable one-to-one continuation structure established | approximation possible | no | producer/runtime-specific meaning or insufficient semantic provenance |

`GenericHeap.pinDown` is a real JPF liveness mechanism, but using it for all imported HPROF roots would erase ownership and lifecycle semantics. I.1 deliberately does not do that.

### Result and regression

The focused durable command is:

~~~bash
./gradlew hprofRootCharacterizationTest --no-daemon
~~~

It regenerates `build/hprof-smoke/hprof-root-characterization.hprof` in a real HotSpot process and verifies stable object-specific findings without depending on VM-wide counts. No HPROF root category gained new importer support in I.1. Static root semantics remain the already verified G.1 implementation; non-static semantic roots are blocked principally by execution-state reconstruction, not by object identity.

The research implication is a firm split:

~~~text
heap state:
  objects + arrays + fields + statics + root records

execution state that gives roots meaning:
  threads + frames + locals/operands + PCs + monitors + native ownership
~~~

HPROF root records identify many live objects and sometimes carry thread/frame correlation data, so they expose where continuation state is needed. They do not by themselves instantiate the corresponding JPF runtime structures, and HAHA 2.0.4 further hides or discards part of that provenance.

Loader-qualified object arrays remain an explicit unverified coverage item; loader-specific expansion is intentionally paused. Lifecycle metadata V1 and checkpoint bundle V1 are unchanged.


## Pass I.2 — Minimum Java Thread / Frame Checkpoint State

I.2 separates capture feasibility from restoration feasibility. Standard HPROF is not an execution checkpoint, so the focused experiment supplies exact synthetic execution metadata while taking the referenced object identity from a real HotSpot dump. The result is **CASE A** for the bounded target question: when exact state for one static Java frame with an empty operand stack is supplied, JPF can install it through public APIs and resume at the selected bytecode instruction. Capturing all of that state remains separate work.

### Concrete JPF execution representation

A live modeled thread is a `ThreadInfo` registered in `ThreadList`, not merely a `java.lang.Thread` heap object. The local state enumeration is `NEW`, `RUNNING`, `BLOCKED`, `UNBLOCKED`, `WAITING`, `TIMEOUT_WAITING`, `NOTIFIED`, `INTERRUPTED`, `TIMEDOUT`, `TERMINATED`, and `SLEEPING`. `ThreadInfo` holds its modeled thread-object reference, target, state, pending exception, and linked frame chain. `ThreadInfo.markRoots` marks the thread object, target, pending exception, and every frame.

A Java bytecode frame is a `JVMStackFrame` with an exact `MethodInfo`, an `Instruction pc`, a combined `int[] slots`, a `FixedBitSet isRef`, and optional slot/frame attributes. Locals occupy `0 .. maxLocals-1`; the operand area begins at `stackBase=maxLocals`; and `top=maxLocals-1` denotes an empty operand stack. Values use JVM slots:

| Java value | JPF slot representation |
|---|---|
| `int`, byte/short/char/boolean categories | one 32-bit slot |
| `float` | one slot containing raw IEEE-754 int bits |
| reference | one slot containing the JPF ref plus its `isRef` bit |
| `long` | two slots, high word then low word |
| `double` | two slots containing raw IEEE-754 long bits |

`setLocalVariable(index,value)` stores a primitive and clears reference-ness; `setLocalReferenceVariable(index,ref)` stores the same numeric slot form but sets `isRef`. Long/double helpers write two slots. Operand values use the same storage and reference bitmap above `stackBase`. Attributes are independent from the reference bitmap and are not required by this fixture. `StackFrame.markThreadRoots` scans reference-marked slots through `top`, which is why numeric values alone cannot constitute a faithful frame checkpoint.

The PC is the next `Instruction` to execute. The restoration API is `StackFrame.setPC(Instruction)`; a durable checkpoint should identify it as loader-qualified declaring class, method name, JVM descriptor, and bytecode offset. The experiment resolves `ClassInfo.getMethod(methodName+descriptor,false)` and `MethodInfo.getInstructionAt(offset)`. A source line is not an exact PC.

### Standard HPROF and HAHA boundary

| Required execution component | Standard HPROF | HAHA 2.0.4 exposure |
|---|---|---|
| frame display identity | `STACK_FRAME`: frame ID, method-name string ID, method-signature string ID, source-file string ID, class serial, line number | parsed, but fields are package-private and public behavior is essentially formatting |
| frame chain/thread correlation | `STACK_TRACE`: trace serial, thread serial, ordered frame IDs | parsed; useful fields are not public getters |
| rooted local correlation | `ROOT_JAVA_FRAME`: object ID, thread serial, frame number | root category/object public; thread/stack association hidden |
| thread-object correlation | `ROOT_THREAD_OBJECT`: object ID, thread serial, stack-trace serial | retained internally as `ThreadObj`, omitted from `getGCRoots()`, no public getters |
| exact bytecode PC | absent; line number is not a bytecode offset | unavailable |
| local slot values/layout | absent | unavailable |
| operand-stack values/depth | absent | unavailable |
| local/operand reference masks | absent | unavailable |
| JPF thread status/scheduler state | absent as continuation state | unavailable |
| pending exception and exact caller-return state | absent | unavailable |

Thus HPROF supplies descriptive stack traces and root correlations, not resumable frames. HAHA additionally hides thread/frame correlation fields that HPROF does carry. It cannot expose state that the format never recorded.

### Prototype metadata and restoration

The separate experimental input is configured with:

~~~properties
hprof.execution_file=/path/to/frame-state-execution.properties
~~~

Its bounded V1 semantic shape is:

~~~properties
execution.version=1
thread.1.logical_id=1
thread.1.state=RUNNING
frame.1.class=HprofFrameStateFixture
frame.1.method=checkpointMethod
frame.1.descriptor=()V
frame.1.bytecode_offset=22
frame.1.local_count=3
frame.1.local.0=INT:5
frame.1.local.1=REFERENCE_HPROF:0x<captured-marker-id>
frame.1.local.2=INT:15
~~~

This is intentionally not lifecycle metadata V1 and is not claimed as a final execution-checkpoint schema. It supports one `RUNNING` thread, one system-loader static Java method, `INT` and `REFERENCE_HPROF` locals, and an empty operand stack.

The real HotSpot fixture pauses in `checkpointMethod()V` after incrementing `preCounter`, with locals `a=5`, `ref=Marker(value=7)`, and `b=15`. The generator clears its temporary static Marker reference before dumping, so the Marker is found as a `JAVA_LOCAL` root. Post-processing obtains its actual HPROF ID and writes the synthetic exact locals and verified bytecode offset 22; it does not derive locals or PC from HPROF.

The modeled target creates a normal `java.lang.Thread`. JPF's `Thread.start` path creates/registers its `ThreadInfo`, installs the normal direct-call thread-exit frame, sets `RUNNING`, and emits `threadStarted`. The listener then resolves exact `MethodInfo`/`Instruction`, creates a public `new JVMStackFrame(mi)` without consuming caller operands, writes typed locals/reference bits, sets PC 22, verifies the operand stack is empty, and pushes it with `ThreadInfo.pushFrame`. `ClassInfo.createStackFrame(ti,mi)` is deliberately not used: it is a call-site factory that consumes arguments from the caller operand stack, which is wrong for restoration.

The imported Marker is pinned only between heap import and frame installation. The pin is released before deliberate GC. At the first restored instruction, GC preserves it solely through `ThreadList -> ThreadInfo -> JVMStackFrame.isRef local -> object`; the exact local JPF ref and reference bit are rechecked afterward.

The first executed instruction is the exact `iload_2` object at bytecode offset 22. The earlier increment is not rerun: restored static `preCounter` remains 1. Modeled continuation computes `result=15+7=22`, increments `continuationCounter` once, returns through JPF's normal thread exit frame, and terminates. The durable signal is:

~~~text
[HPROF-JPF-MODEL] reconstructed frame continuation verified: preCounter=1 result=22 continuationCounter=1
~~~

The focused regression is `./gradlew hprofFrameStateTest --no-daemon`.

### Capture channels and remaining limits

The installed JDI API provides suspended-thread frame enumeration, `StackFrame.location()`, `Location.codeIndex()`, arguments, and visible-variable value access. Visible locals can fail with `AbsentInformationException`, depend on debug-variable metadata/scope, and do not expose arbitrary raw local slots or the operand stack. No general operand-stack capture API appears in the inspected JDI interfaces.

JVMTI is the natural lower-level candidate for thread state, stack traces/frame locations, and typed local access, but the development image does not contain `jvmti.h`; no native-agent capability claim is made here. A later capture milestone must verify exact capabilities, suspension requirements, opaque/native-frame behavior, and availability of raw slot/reference information. Neither channel should be assumed to expose arbitrary operand-stack contents. HotSpot-specific mechanisms may expose more internal state but would be non-portable.

I.2 does not restore arbitrary operand stacks, caller chains/return locations, instance receivers, long/double locals, pending exceptions, scheduler choices, blocked/waiting states, monitors, native/JNI frames, or custom-loader method identity. It proves JPF-side restoration feasibility for exact supplied minimal state, while standard HPROF remains insufficient as the capture source.

### Execution-state coverage matrix

| State component | Needed by JPF? | HPROF provides? | Supplemental capture required? | JPF restoration verified? | Capture candidate/status |
|---|---:|---:|---:|---:|---|
| thread identity / modeled thread object | yes | correlation only | yes | verified for one newly created modeled thread | JDI/JVMTI; synthetic in I.2 |
| thread status | yes | no continuation status | yes | `RUNNING` only | JDI status or JVMTI state; other states deferred |
| frame chain | yes | descriptive trace only | yes | one Java frame plus normal exit frame | JDI/JVMTI trace; general callers deferred |
| loader-qualified declaring class | yes | class serial/name correlation | usually | system loader only | bundle identity needed for custom loaders |
| method name + descriptor | yes | yes in `STACK_FRAME` | HAHA access workaround needed | verified from supplied metadata | HPROF parser/JDI/JVMTI |
| exact bytecode PC | yes | no (line only) | yes | offset 22 verified | JDI `Location.codeIndex`; JVMTI candidate |
| primitive local slots | yes | no | yes | `int` slots verified | JDI visible locals or JVMTI typed locals |
| reference local slots | yes | rooted object correlation, not slot map | yes | exact imported ref verified | JDI/JVMTI plus HPROF-ID correlation |
| local reference mask | yes for GC/type fidelity | no | yes | verified | must be derived from captured slot types |
| operand slots/depth | yes when non-empty | no | yes | empty stack only | no general JDI API; capture gap |
| operand reference mask | yes when non-empty | no | yes | empty stack only | capture gap |
| pending exception | yes when present | no resumable state | yes | unsupported | later execution metadata |
| return/caller state | yes for general stack | descriptive frames only | yes | bounded direct-call exit only | later frame-chain capture |
| thread stack as GC root | yes | root record only | yes | verified semantically | reconstructed reference bitmap |
| monitor/scheduler state | yes when blocked/waiting | incomplete/non-continuation | yes | unsupported | later thread/monitor milestone |

Lifecycle metadata V1, checkpoint bundle V1, loader import semantics, and the intentionally deferred loader-qualified object-array coverage are unchanged.
