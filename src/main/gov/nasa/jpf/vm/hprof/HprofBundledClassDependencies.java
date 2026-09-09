package gov.nasa.jpf.vm.hprof;

import gov.nasa.jpf.jvm.ClassFile;
import gov.nasa.jpf.jvm.ClassFileReaderAdapter;
import gov.nasa.jpf.vm.ClassParseException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/** Header-level superclass/interface dependencies extracted from an integrity-checked classfile. */
final class HprofBundledClassDependencies {
  final String binaryName;
  final String superName;
  final List<String> interfaceNames;

  private HprofBundledClassDependencies(String binaryName, String superName,
      List<String> interfaceNames) {
    this.binaryName = binaryName;
    this.superName = superName;
    this.interfaceNames = Collections.unmodifiableList(interfaceNames);
  }

  static HprofBundledClassDependencies parse(byte[] bytes) {
    HeaderReader reader = new HeaderReader();
    try {
      new ClassFile(bytes).parse(reader);
    } catch (ClassParseException ex) {
      throw new IllegalArgumentException("[HPROF-JPF] invalid bundled classfile", ex);
    }
    if (reader.binaryName == null) {
      throw new IllegalArgumentException("[HPROF-JPF] bundled classfile has no class header");
    }
    return new HprofBundledClassDependencies(reader.binaryName, reader.superName,
        new ArrayList<>(reader.interfaces));
  }

  private static String binary(String name) {
    return name == null ? null : name.replace('/', '.');
  }

  private static final class HeaderReader extends ClassFileReaderAdapter {
    String binaryName;
    String superName;
    final List<String> interfaces = new ArrayList<>();

    @Override
    public void setClass(ClassFile cf, String className, String superClassName, int flags,
        int constantPoolCount) {
      binaryName = binary(className);
      superName = binary(superClassName);
    }

    @Override
    public void setInterface(ClassFile cf, int index, String interfaceName) {
      interfaces.add(binary(interfaceName));
    }
  }
}
