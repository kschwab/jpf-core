# HPROF -> JPF State Reconstruction

## Quick Regression

```bash
./gradlew hprofSmokeTest
```

Graph identity regression:

```bash
./gradlew hprofGraphIdentityTest
```

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

`ImportResult` exposes an unmodifiable copy of the HPROF-ID-to-JPF-reference map and counts for allocated objects, allocated arrays, primitive fields, references, and array elements.

The listener validates `hprof.file` and `hprof.classes`, loads the snapshot, constructs `HprofView`, invokes the importer, and optionally invokes the smoke validator when `hprof.smoke_validate=true`. The smoke configuration separately enables `hprof.smoke_bind_root=true`.

Currently supported within selected classes is deliberately limited to `Type.INT` instance fields, `Type.OBJECT` references whose non-null target has a Pass A mapping, directly referenced `Type.INT` arrays, and their boxed-`Integer` payloads. Unsupported selected field types, unsupported directly referenced arrays, malformed values, and non-null references outside the selected identity graph cause a clear failure rather than an incomplete import.

## State Reconstruction Coverage Matrix

| Feature | Status |
|---|---|
| `int` instance field | verified |
| ordinary object reference | verified |
| `int[]` reference | verified |
| `int[]` contents | verified |
| modeled static smoke root | verified |
| GC preservation | verified |
| null reference | verified |
| reference aliasing | verified |
| cyclic object graph | verified |
| `boolean` field | unsupported |
| `byte` field | unsupported |
| `char` field | unsupported |
| `short` field | unsupported |
| `long` field | unsupported |
| `float` field | unsupported |
| `double` field | unsupported |
| primitive arrays other than `int[]` | unsupported |
| object arrays | unsupported |
| inherited fields | unverified |
| HPROF statics | unsupported |
| HPROF GC roots | unsupported |
| `String` | unsupported |
| multiple class loaders | unsupported |
| thread state | deferred |
| stack frames / program counters | deferred |
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
