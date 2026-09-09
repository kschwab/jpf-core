package gov.nasa.jpf.vm.hprof;

import gov.nasa.jpf.jvm.JVMStackFrame;
import gov.nasa.jpf.vm.ClassInfo;
import gov.nasa.jpf.vm.ClassLoaderInfo;
import gov.nasa.jpf.vm.Heap;
import gov.nasa.jpf.vm.Instruction;
import gov.nasa.jpf.vm.MethodInfo;
import gov.nasa.jpf.vm.StackFrame;
import gov.nasa.jpf.vm.ThreadInfo;
import gov.nasa.jpf.vm.VM;

/** Bounded I.2 restorer for one supplied RUNNING Java frame with an empty operand stack. */
public final class HprofFrameStateRestorer {
  private final HprofExecutionCheckpoint checkpoint;
  private final JpfHeapImporter.ImportResult result;
  private final int importedRef;
  private ThreadInfo restoredThread;
  private Instruction expectedFirst;
  private boolean observed;

  public HprofFrameStateRestorer(
      VM vm, HprofExecutionCheckpoint checkpoint, JpfHeapImporter.ImportResult result) {
    this.checkpoint = checkpoint;
    this.result = result;
    int found = 0;
    for (HprofExecutionCheckpoint.LocalSlot slot : checkpoint.locals) {
      if (slot.kind == HprofExecutionCheckpoint.SlotKind.REFERENCE_HPROF) {
        Integer ref = result.getRefMap().get(slot.value);
        require(ref != null, "local HPROF reference has no imported mapping");
        found = ref;
      }
    }
    require(found != 0, "no imported reference local");
    importedRef = found;

    // Bridge the interval between heap import and installation of the semantic stack root.
    vm.getHeap().registerPinDown(importedRef);
  }

  public void threadStarted(VM vm, ThreadInfo thread) {
    if (restoredThread != null || "main".equals(thread.getName())) {
      return;
    }
    require(thread.getState() == ThreadInfo.State.RUNNING, "started thread is not RUNNING");

    ClassInfo classInfo = ClassLoaderInfo.getSystemResolvedClassInfo(checkpoint.className);
    MethodInfo method =
        classInfo.getMethod(checkpoint.methodName + checkpoint.descriptor, false);
    require(method != null, "checkpoint MethodInfo not found");
    Instruction pc = method.getInstructionAt(checkpoint.bytecodeOffset);
    require(pc != null, "checkpoint bytecode offset has no Instruction");

    // The call-site factory would consume arguments from the thread entry frame.
    StackFrame frame = new JVMStackFrame(method);
    require(frame.getLocalVariableCount() == checkpoint.localCount, "local slot count mismatch");
    for (HprofExecutionCheckpoint.LocalSlot slot : checkpoint.locals) {
      if (slot.kind == HprofExecutionCheckpoint.SlotKind.INT) {
        frame.setLocalVariable(slot.index, (int) slot.value);
      } else {
        Integer ref = result.getRefMap().get(slot.value);
        require(ref != null && ref == importedRef, "reference mapping changed");
        frame.setLocalReferenceVariable(slot.index, ref);
      }
    }
    frame.setPC(pc);
    require(frame.getTopPos() == checkpoint.localCount - 1, "operand stack is not empty");
    require(
        frame.isLocalVariableRef(1)
            && !frame.isLocalVariableRef(0)
            && !frame.isLocalVariableRef(2),
        "local reference mask mismatch");

    thread.pushFrame(frame);
    restoredThread = thread;
    expectedFirst = pc;
    vm.getHeap().releasePinDown(importedRef);

    System.out.printf(
        "[HPROF-JPF] reconstructed frame installed: threadId=%d state=%s method=%s"
            + " pc=%d localRef=%d refMask=true operandDepth=0%n",
        thread.getId(),
        thread.getState(),
        method.getFullName(),
        pc.getPosition(),
        importedRef);
  }

  public void executeInstruction(ThreadInfo thread, Instruction instruction) {
    if (thread == restoredThread && !observed) {
      Heap heap = thread.getVM().getHeap();
      heap.gc();
      require(heap.get(importedRef) != null, "imported local reference was collected after unpin");
      require(
          thread.getTopFrame().getLocalVariable(1) == importedRef
              && thread.getTopFrame().isLocalVariableRef(1),
          "stack local identity/mask changed across GC");
      System.out.println(
          "[HPROF-JPF] reconstructed JAVA_FRAME root survived GC through"
              + " ThreadList -> ThreadInfo -> StackFrame");

      require(instruction == expectedFirst, "first instruction is not restored PC");
      observed = true;
      System.out.printf(
          "[HPROF-JPF] restored PC first instruction verified: offset=%d mnemonic=%s%n",
          instruction.getPosition(), instruction.getMnemonic());
    }
  }

  public void threadTerminated(ThreadInfo thread) {
    if (thread == restoredThread) {
      require(observed, "restored thread terminated without executing restored PC");
    }
  }

  private static void require(boolean value, String message) {
    if (!value) {
      throw new IllegalStateException("[HPROF-JPF] frame restoration failed: " + message);
    }
  }
}
