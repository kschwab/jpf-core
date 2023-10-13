// This is a generated file! Please edit source .ksy file and use kaitai-struct-compiler to rebuild

package gov.nasa.jpf.vm.hprof;

import io.kaitai.struct.ByteBufferKaitaiStream;
import io.kaitai.struct.KaitaiStruct;
import io.kaitai.struct.KaitaiStream;
import java.io.IOException;
import java.util.Map;
import java.util.HashMap;
import java.util.ArrayList;
import java.nio.charset.Charset;

public class Hprof extends KaitaiStruct {
    public static Hprof fromFile(String fileName) throws IOException {
        return new Hprof(new ByteBufferKaitaiStream(fileName));
    }

    public enum BasicType {
        OBJECT(2),
        BOOLEAN(4),
        CHAR(5),
        FLOAT(6),
        DOUBLE(7),
        BYTE(8),
        SHORT(9),
        INT(10),
        LONG(11);

        private final long id;
        BasicType(long id) { this.id = id; }
        public long id() { return id; }
        private static final Map<Long, BasicType> byId = new HashMap<Long, BasicType>(9);
        static {
            for (BasicType e : BasicType.values())
                byId.put(e.id(), e);
        }
        public static BasicType byId(long id) { return byId.get(id); }
    }

    public enum TagType {
        STRING(1),
        LOAD_CLASS(2),
        UNLOAD_CLASS(3),
        STACK_FRAME(4),
        STACK_TRACE(5),
        ALLOC_SITES(6),
        HEAP_SUMMARY(7),
        START_THREAD(10),
        END_THREAD(11),
        HEAP_DUMP(12),
        CPU_SAMPLES(13),
        CONTROL_SETTINGS(14),
        HEAP_DUMP_SEGMENT(28),
        HEAP_DUMP_END(44);

        private final long id;
        TagType(long id) { this.id = id; }
        public long id() { return id; }
        private static final Map<Long, TagType> byId = new HashMap<Long, TagType>(14);
        static {
            for (TagType e : TagType.values())
                byId.put(e.id(), e);
        }
        public static TagType byId(long id) { return byId.get(id); }
    }

    public enum SubTagType {
        ROOT_JNI_GLOBAL(1),
        ROOT_JNI_LOCAL(2),
        ROOT_JAVA_FRAME(3),
        ROOT_NATIVE_STACK(4),
        ROOT_STICKY_CLASS(5),
        ROOT_THREAD_BLOCK(6),
        ROOT_MONITOR_USED(7),
        ROOT_THREAD_OBJECT(8),
        CLASS_DUMP(32),
        INSTANCE_DUMP(33),
        OBJECT_ARRAY_DUMP(34),
        PRIMITIVE_ARRAY_DUMP(35),
        ROOT_UNKNOWN(255);

        private final long id;
        SubTagType(long id) { this.id = id; }
        public long id() { return id; }
        private static final Map<Long, SubTagType> byId = new HashMap<Long, SubTagType>(13);
        static {
            for (SubTagType e : SubTagType.values())
                byId.put(e.id(), e);
        }
        public static SubTagType byId(long id) { return byId.get(id); }
    }

    public Hprof(KaitaiStream _io) {
        this(_io, null, null);
    }

    public Hprof(KaitaiStream _io, KaitaiStruct _parent) {
        this(_io, _parent, null);
    }

    public Hprof(KaitaiStream _io, KaitaiStruct _parent, Hprof _root) {
        super(_io);
        this._parent = _parent;
        this._root = _root == null ? this : _root;
        _read();
    }
    private void _read() {
        this.header = new Header(this._io, this, _root);
        this.tags = new ArrayList<Tag>();
        {
            int i = 0;
            while (!this._io.isEof()) {
                this.tags.add(new Tag(this._io, this, _root));
                i++;
            }
        }
    }
    public static class SubTagRootThreadObject extends KaitaiStruct {
        public static SubTagRootThreadObject fromFile(String fileName) throws IOException {
            return new SubTagRootThreadObject(new ByteBufferKaitaiStream(fileName));
        }

        public SubTagRootThreadObject(KaitaiStream _io) {
            this(_io, null, null);
        }

        public SubTagRootThreadObject(KaitaiStream _io, Hprof.SubTag _parent) {
            this(_io, _parent, null);
        }

        public SubTagRootThreadObject(KaitaiStream _io, Hprof.SubTag _parent, Hprof _root) {
            super(_io);
            this._parent = _parent;
            this._root = _root;
            _read();
        }
        private void _read() {
            this.objectId = new Uid(this._io, this, _root);
            this.threadSerialNum = this._io.readU4be();
            this.stackTraceSerialNum = this._io.readU4be();
        }
        private Uid objectId;
        private long threadSerialNum;
        private long stackTraceSerialNum;
        private Hprof _root;
        private Hprof.SubTag _parent;
        public Uid objectId() { return objectId; }
        public long threadSerialNum() { return threadSerialNum; }
        public long stackTraceSerialNum() { return stackTraceSerialNum; }
        public Hprof _root() { return _root; }
        public Hprof.SubTag _parent() { return _parent; }
    }
    public static class TagStartThread extends KaitaiStruct {
        public static TagStartThread fromFile(String fileName) throws IOException {
            return new TagStartThread(new ByteBufferKaitaiStream(fileName));
        }

        public TagStartThread(KaitaiStream _io) {
            this(_io, null, null);
        }

        public TagStartThread(KaitaiStream _io, Hprof.Tag _parent) {
            this(_io, _parent, null);
        }

        public TagStartThread(KaitaiStream _io, Hprof.Tag _parent, Hprof _root) {
            super(_io);
            this._parent = _parent;
            this._root = _root;
            _read();
        }
        private void _read() {
            this.threadSerialNum = this._io.readU4be();
            this.threadId = new Uid(this._io, this, _root);
            this.stackFrameSerialNum = this._io.readU4be();
            this.threadNameStringId = new Uid(this._io, this, _root);
            this.threadGroupNameStringId = new Uid(this._io, this, _root);
            this.threadParentGroupNameStringId = new Uid(this._io, this, _root);
        }
        private long threadSerialNum;
        private Uid threadId;
        private long stackFrameSerialNum;
        private Uid threadNameStringId;
        private Uid threadGroupNameStringId;
        private Uid threadParentGroupNameStringId;
        private Hprof _root;
        private Hprof.Tag _parent;
        public long threadSerialNum() { return threadSerialNum; }
        public Uid threadId() { return threadId; }
        public long stackFrameSerialNum() { return stackFrameSerialNum; }
        public Uid threadNameStringId() { return threadNameStringId; }
        public Uid threadGroupNameStringId() { return threadGroupNameStringId; }
        public Uid threadParentGroupNameStringId() { return threadParentGroupNameStringId; }
        public Hprof _root() { return _root; }
        public Hprof.Tag _parent() { return _parent; }
    }
    public static class Tag extends KaitaiStruct {
        public static Tag fromFile(String fileName) throws IOException {
            return new Tag(new ByteBufferKaitaiStream(fileName));
        }

        public Tag(KaitaiStream _io) {
            this(_io, null, null);
        }

        public Tag(KaitaiStream _io, Hprof _parent) {
            this(_io, _parent, null);
        }

        public Tag(KaitaiStream _io, Hprof _parent, Hprof _root) {
            super(_io);
            this._parent = _parent;
            this._root = _root;
            _read();
        }
        private void _read() {
            this.recordType = Hprof.TagType.byId(this._io.readU1());
            this.time = this._io.readU4be();
            this.bodyLength = this._io.readU4be();
            {
                TagType on = recordType();
                if (on != null) {
                    switch (recordType()) {
                    case END_THREAD: {
                        this._raw_body = this._io.readBytes(bodyLength());
                        KaitaiStream _io__raw_body = new ByteBufferKaitaiStream(_raw_body);
                        this.body = new TagEndThread(_io__raw_body, this, _root);
                        break;
                    }
                    case ALLOC_SITES: {
                        this._raw_body = this._io.readBytes(bodyLength());
                        KaitaiStream _io__raw_body = new ByteBufferKaitaiStream(_raw_body);
                        this.body = new TagAllocSites(_io__raw_body, this, _root);
                        break;
                    }
                    case UNLOAD_CLASS: {
                        this._raw_body = this._io.readBytes(bodyLength());
                        KaitaiStream _io__raw_body = new ByteBufferKaitaiStream(_raw_body);
                        this.body = new TagUnloadClass(_io__raw_body, this, _root);
                        break;
                    }
                    case CPU_SAMPLES: {
                        this._raw_body = this._io.readBytes(bodyLength());
                        KaitaiStream _io__raw_body = new ByteBufferKaitaiStream(_raw_body);
                        this.body = new TagCpuSamples(_io__raw_body, this, _root);
                        break;
                    }
                    case STACK_TRACE: {
                        this._raw_body = this._io.readBytes(bodyLength());
                        KaitaiStream _io__raw_body = new ByteBufferKaitaiStream(_raw_body);
                        this.body = new TagStackTrace(_io__raw_body, this, _root);
                        break;
                    }
                    case LOAD_CLASS: {
                        this._raw_body = this._io.readBytes(bodyLength());
                        KaitaiStream _io__raw_body = new ByteBufferKaitaiStream(_raw_body);
                        this.body = new TagLoadClass(_io__raw_body, this, _root);
                        break;
                    }
                    case HEAP_DUMP: {
                        this._raw_body = this._io.readBytes(bodyLength());
                        KaitaiStream _io__raw_body = new ByteBufferKaitaiStream(_raw_body);
                        this.body = new TagHeapDump(_io__raw_body, this, _root);
                        break;
                    }
                    case HEAP_DUMP_SEGMENT: {
                        this._raw_body = this._io.readBytes(bodyLength());
                        KaitaiStream _io__raw_body = new ByteBufferKaitaiStream(_raw_body);
                        this.body = new TagHeapDumpSegment(_io__raw_body, this, _root);
                        break;
                    }
                    case HEAP_DUMP_END: {
                        this._raw_body = this._io.readBytes(bodyLength());
                        KaitaiStream _io__raw_body = new ByteBufferKaitaiStream(_raw_body);
                        this.body = new TagHeapDumpEnd(_io__raw_body, this, _root);
                        break;
                    }
                    case START_THREAD: {
                        this._raw_body = this._io.readBytes(bodyLength());
                        KaitaiStream _io__raw_body = new ByteBufferKaitaiStream(_raw_body);
                        this.body = new TagStartThread(_io__raw_body, this, _root);
                        break;
                    }
                    case STACK_FRAME: {
                        this._raw_body = this._io.readBytes(bodyLength());
                        KaitaiStream _io__raw_body = new ByteBufferKaitaiStream(_raw_body);
                        this.body = new TagStackFrame(_io__raw_body, this, _root);
                        break;
                    }
                    case CONTROL_SETTINGS: {
                        this._raw_body = this._io.readBytes(bodyLength());
                        KaitaiStream _io__raw_body = new ByteBufferKaitaiStream(_raw_body);
                        this.body = new TagControlSettings(_io__raw_body, this, _root);
                        break;
                    }
                    case STRING: {
                        this._raw_body = this._io.readBytes(bodyLength());
                        KaitaiStream _io__raw_body = new ByteBufferKaitaiStream(_raw_body);
                        this.body = new TagString(_io__raw_body, this, _root);
                        break;
                    }
                    case HEAP_SUMMARY: {
                        this._raw_body = this._io.readBytes(bodyLength());
                        KaitaiStream _io__raw_body = new ByteBufferKaitaiStream(_raw_body);
                        this.body = new TagHeapSummary(_io__raw_body, this, _root);
                        break;
                    }
                    default: {
                        this.body = this._io.readBytes(bodyLength());
                        break;
                    }
                    }
                } else {
                    this.body = this._io.readBytes(bodyLength());
                }
            }
        }
        private TagType recordType;
        private long time;
        private long bodyLength;
        private Object body;
        private Hprof _root;
        private Hprof _parent;
        private byte[] _raw_body;
        public TagType recordType() { return recordType; }
        public long time() { return time; }
        public long bodyLength() { return bodyLength; }
        public Object body() { return body; }
        public Hprof _root() { return _root; }
        public Hprof _parent() { return _parent; }
        public byte[] _raw_body() { return _raw_body; }
    }
    public static class TagAllocSites extends KaitaiStruct {
        public static TagAllocSites fromFile(String fileName) throws IOException {
            return new TagAllocSites(new ByteBufferKaitaiStream(fileName));
        }

        public TagAllocSites(KaitaiStream _io) {
            this(_io, null, null);
        }

        public TagAllocSites(KaitaiStream _io, Hprof.Tag _parent) {
            this(_io, _parent, null);
        }

        public TagAllocSites(KaitaiStream _io, Hprof.Tag _parent, Hprof _root) {
            super(_io);
            this._parent = _parent;
            this._root = _root;
            _read();
        }
        private void _read() {
            this.bitMaskFlags = this._io.readU2be();
            this.cutoffRatio = this._io.readU4be();
            this.totalLiveBytes = this._io.readU4be();
            this.totalLiveInstances = this._io.readU4be();
            this.totalBytesAllocated = this._io.readU8be();
            this.totalInstancesAllocated = this._io.readU8be();
            this.numAllocSites = this._io.readU4be();
            this.allocSites = new ArrayList<AllocSite>();
            for (int i = 0; i < numAllocSites(); i++) {
                this.allocSites.add(new AllocSite(this._io, this, _root));
            }
        }
        private Integer incrementalVsComplete;
        public Integer incrementalVsComplete() {
            if (this.incrementalVsComplete != null)
                return this.incrementalVsComplete;
            int _tmp = (int) ((bitMaskFlags() & 1));
            this.incrementalVsComplete = _tmp;
            return this.incrementalVsComplete;
        }
        private Integer sortedByAllocationVsLine;
        public Integer sortedByAllocationVsLine() {
            if (this.sortedByAllocationVsLine != null)
                return this.sortedByAllocationVsLine;
            int _tmp = (int) ((bitMaskFlags() & 2));
            this.sortedByAllocationVsLine = _tmp;
            return this.sortedByAllocationVsLine;
        }
        private Integer isForceGccEnabled;
        public Integer isForceGccEnabled() {
            if (this.isForceGccEnabled != null)
                return this.isForceGccEnabled;
            int _tmp = (int) ((bitMaskFlags() & 4));
            this.isForceGccEnabled = _tmp;
            return this.isForceGccEnabled;
        }
        private int bitMaskFlags;
        private long cutoffRatio;
        private long totalLiveBytes;
        private long totalLiveInstances;
        private long totalBytesAllocated;
        private long totalInstancesAllocated;
        private long numAllocSites;
        private ArrayList<AllocSite> allocSites;
        private Hprof _root;
        private Hprof.Tag _parent;
        public int bitMaskFlags() { return bitMaskFlags; }
        public long cutoffRatio() { return cutoffRatio; }
        public long totalLiveBytes() { return totalLiveBytes; }
        public long totalLiveInstances() { return totalLiveInstances; }
        public long totalBytesAllocated() { return totalBytesAllocated; }
        public long totalInstancesAllocated() { return totalInstancesAllocated; }
        public long numAllocSites() { return numAllocSites; }
        public ArrayList<AllocSite> allocSites() { return allocSites; }
        public Hprof _root() { return _root; }
        public Hprof.Tag _parent() { return _parent; }
    }
    public static class SubTagRootUnknown extends KaitaiStruct {
        public static SubTagRootUnknown fromFile(String fileName) throws IOException {
            return new SubTagRootUnknown(new ByteBufferKaitaiStream(fileName));
        }

        public SubTagRootUnknown(KaitaiStream _io) {
            this(_io, null, null);
        }

        public SubTagRootUnknown(KaitaiStream _io, Hprof.SubTag _parent) {
            this(_io, _parent, null);
        }

        public SubTagRootUnknown(KaitaiStream _io, Hprof.SubTag _parent, Hprof _root) {
            super(_io);
            this._parent = _parent;
            this._root = _root;
            _read();
        }
        private void _read() {
            this.objectId = new Uid(this._io, this, _root);
        }
        private Uid objectId;
        private Hprof _root;
        private Hprof.SubTag _parent;
        public Uid objectId() { return objectId; }
        public Hprof _root() { return _root; }
        public Hprof.SubTag _parent() { return _parent; }
    }
    public static class TagCpuSamples extends KaitaiStruct {
        public static TagCpuSamples fromFile(String fileName) throws IOException {
            return new TagCpuSamples(new ByteBufferKaitaiStream(fileName));
        }

        public TagCpuSamples(KaitaiStream _io) {
            this(_io, null, null);
        }

        public TagCpuSamples(KaitaiStream _io, Hprof.Tag _parent) {
            this(_io, _parent, null);
        }

        public TagCpuSamples(KaitaiStream _io, Hprof.Tag _parent, Hprof _root) {
            super(_io);
            this._parent = _parent;
            this._root = _root;
            _read();
        }
        private void _read() {
            this.totalNumSamples = this._io.readU4be();
            this.numCpuTraces = this._io.readU4be();
            this.cpuTraces = new ArrayList<CpuTrace>();
            for (int i = 0; i < numCpuTraces(); i++) {
                this.cpuTraces.add(new CpuTrace(this._io, this, _root));
            }
        }
        private long totalNumSamples;
        private long numCpuTraces;
        private ArrayList<CpuTrace> cpuTraces;
        private Hprof _root;
        private Hprof.Tag _parent;
        public long totalNumSamples() { return totalNumSamples; }
        public long numCpuTraces() { return numCpuTraces; }
        public ArrayList<CpuTrace> cpuTraces() { return cpuTraces; }
        public Hprof _root() { return _root; }
        public Hprof.Tag _parent() { return _parent; }
    }
    public static class SubTagObjectArrayDump extends KaitaiStruct {
        public static SubTagObjectArrayDump fromFile(String fileName) throws IOException {
            return new SubTagObjectArrayDump(new ByteBufferKaitaiStream(fileName));
        }

        public SubTagObjectArrayDump(KaitaiStream _io) {
            this(_io, null, null);
        }

        public SubTagObjectArrayDump(KaitaiStream _io, Hprof.SubTag _parent) {
            this(_io, _parent, null);
        }

        public SubTagObjectArrayDump(KaitaiStream _io, Hprof.SubTag _parent, Hprof _root) {
            super(_io);
            this._parent = _parent;
            this._root = _root;
            _read();
        }
        private void _read() {
            this.arrayObjectId = new Uid(this._io, this, _root);
            this.stackTraceSerialNum = this._io.readU4be();
            this.numElements = this._io.readU4be();
            this.arrayClassObjectId = new Uid(this._io, this, _root);
            this.elements = new ArrayList<Uid>();
            for (int i = 0; i < numElements(); i++) {
                this.elements.add(new Uid(this._io, this, _root));
            }
        }
        private Uid arrayObjectId;
        private long stackTraceSerialNum;
        private long numElements;
        private Uid arrayClassObjectId;
        private ArrayList<Uid> elements;
        private Hprof _root;
        private Hprof.SubTag _parent;
        public Uid arrayObjectId() { return arrayObjectId; }
        public long stackTraceSerialNum() { return stackTraceSerialNum; }
        public long numElements() { return numElements; }
        public Uid arrayClassObjectId() { return arrayClassObjectId; }
        public ArrayList<Uid> elements() { return elements; }
        public Hprof _root() { return _root; }
        public Hprof.SubTag _parent() { return _parent; }
    }
    public static class TagControlSettings extends KaitaiStruct {
        public static TagControlSettings fromFile(String fileName) throws IOException {
            return new TagControlSettings(new ByteBufferKaitaiStream(fileName));
        }

        public TagControlSettings(KaitaiStream _io) {
            this(_io, null, null);
        }

        public TagControlSettings(KaitaiStream _io, Hprof.Tag _parent) {
            this(_io, _parent, null);
        }

        public TagControlSettings(KaitaiStream _io, Hprof.Tag _parent, Hprof _root) {
            super(_io);
            this._parent = _parent;
            this._root = _root;
            _read();
        }
        private void _read() {
            this.bitMaskFlags = this._io.readU4be();
            this.stackTraceDepth = this._io.readU2be();
        }
        private Integer isAllocTracesEnabled;
        public Integer isAllocTracesEnabled() {
            if (this.isAllocTracesEnabled != null)
                return this.isAllocTracesEnabled;
            int _tmp = (int) ((bitMaskFlags() & 1));
            this.isAllocTracesEnabled = _tmp;
            return this.isAllocTracesEnabled;
        }
        private Integer isCpuSamplingEnabled;
        public Integer isCpuSamplingEnabled() {
            if (this.isCpuSamplingEnabled != null)
                return this.isCpuSamplingEnabled;
            int _tmp = (int) ((bitMaskFlags() & 2));
            this.isCpuSamplingEnabled = _tmp;
            return this.isCpuSamplingEnabled;
        }
        private long bitMaskFlags;
        private int stackTraceDepth;
        private Hprof _root;
        private Hprof.Tag _parent;
        public long bitMaskFlags() { return bitMaskFlags; }
        public int stackTraceDepth() { return stackTraceDepth; }
        public Hprof _root() { return _root; }
        public Hprof.Tag _parent() { return _parent; }
    }
    public static class TagStackTrace extends KaitaiStruct {
        public static TagStackTrace fromFile(String fileName) throws IOException {
            return new TagStackTrace(new ByteBufferKaitaiStream(fileName));
        }

        public TagStackTrace(KaitaiStream _io) {
            this(_io, null, null);
        }

        public TagStackTrace(KaitaiStream _io, Hprof.Tag _parent) {
            this(_io, _parent, null);
        }

        public TagStackTrace(KaitaiStream _io, Hprof.Tag _parent, Hprof _root) {
            super(_io);
            this._parent = _parent;
            this._root = _root;
            _read();
        }
        private void _read() {
            this.stackTraceSerialNum = this._io.readU4be();
            this.threadSerialNum = this._io.readU4be();
            this.numStackFrameIds = this._io.readU4be();
            this.stackFrameIds = new ArrayList<Uid>();
            for (int i = 0; i < numStackFrameIds(); i++) {
                this.stackFrameIds.add(new Uid(this._io, this, _root));
            }
        }
        private long stackTraceSerialNum;
        private long threadSerialNum;
        private long numStackFrameIds;
        private ArrayList<Uid> stackFrameIds;
        private Hprof _root;
        private Hprof.Tag _parent;
        public long stackTraceSerialNum() { return stackTraceSerialNum; }
        public long threadSerialNum() { return threadSerialNum; }
        public long numStackFrameIds() { return numStackFrameIds; }
        public ArrayList<Uid> stackFrameIds() { return stackFrameIds; }
        public Hprof _root() { return _root; }
        public Hprof.Tag _parent() { return _parent; }
    }
    public static class TagEndThread extends KaitaiStruct {
        public static TagEndThread fromFile(String fileName) throws IOException {
            return new TagEndThread(new ByteBufferKaitaiStream(fileName));
        }

        public TagEndThread(KaitaiStream _io) {
            this(_io, null, null);
        }

        public TagEndThread(KaitaiStream _io, Hprof.Tag _parent) {
            this(_io, _parent, null);
        }

        public TagEndThread(KaitaiStream _io, Hprof.Tag _parent, Hprof _root) {
            super(_io);
            this._parent = _parent;
            this._root = _root;
            _read();
        }
        private void _read() {
            this.threadSerialNum = this._io.readU4be();
        }
        private long threadSerialNum;
        private Hprof _root;
        private Hprof.Tag _parent;
        public long threadSerialNum() { return threadSerialNum; }
        public Hprof _root() { return _root; }
        public Hprof.Tag _parent() { return _parent; }
    }
    public static class AllocSite extends KaitaiStruct {
        public static AllocSite fromFile(String fileName) throws IOException {
            return new AllocSite(new ByteBufferKaitaiStream(fileName));
        }

        public AllocSite(KaitaiStream _io) {
            this(_io, null, null);
        }

        public AllocSite(KaitaiStream _io, Hprof.TagAllocSites _parent) {
            this(_io, _parent, null);
        }

        public AllocSite(KaitaiStream _io, Hprof.TagAllocSites _parent, Hprof _root) {
            super(_io);
            this._parent = _parent;
            this._root = _root;
            _read();
        }
        private void _read() {
            this.arrayType = Hprof.BasicType.byId(this._io.readU1());
            this.classSerialNum = this._io.readU4be();
            this.stackTraceSerialNum = this._io.readU4be();
            this.numLiveBytes = this._io.readU4be();
            this.numLiveInstances = this._io.readU4be();
            this.numBytesAllocated = this._io.readU4be();
            this.numInstancesAllocated = this._io.readU4be();
        }
        private BasicType arrayType;
        private long classSerialNum;
        private long stackTraceSerialNum;
        private long numLiveBytes;
        private long numLiveInstances;
        private long numBytesAllocated;
        private long numInstancesAllocated;
        private Hprof _root;
        private Hprof.TagAllocSites _parent;
        public BasicType arrayType() { return arrayType; }
        public long classSerialNum() { return classSerialNum; }
        public long stackTraceSerialNum() { return stackTraceSerialNum; }
        public long numLiveBytes() { return numLiveBytes; }
        public long numLiveInstances() { return numLiveInstances; }
        public long numBytesAllocated() { return numBytesAllocated; }
        public long numInstancesAllocated() { return numInstancesAllocated; }
        public Hprof _root() { return _root; }
        public Hprof.TagAllocSites _parent() { return _parent; }
    }
    public static class SubTagRootStickyClass extends KaitaiStruct {
        public static SubTagRootStickyClass fromFile(String fileName) throws IOException {
            return new SubTagRootStickyClass(new ByteBufferKaitaiStream(fileName));
        }

        public SubTagRootStickyClass(KaitaiStream _io) {
            this(_io, null, null);
        }

        public SubTagRootStickyClass(KaitaiStream _io, Hprof.SubTag _parent) {
            this(_io, _parent, null);
        }

        public SubTagRootStickyClass(KaitaiStream _io, Hprof.SubTag _parent, Hprof _root) {
            super(_io);
            this._parent = _parent;
            this._root = _root;
            _read();
        }
        private void _read() {
            this.objectId = new Uid(this._io, this, _root);
        }
        private Uid objectId;
        private Hprof _root;
        private Hprof.SubTag _parent;
        public Uid objectId() { return objectId; }
        public Hprof _root() { return _root; }
        public Hprof.SubTag _parent() { return _parent; }
    }
    public static class TagString extends KaitaiStruct {
        public static TagString fromFile(String fileName) throws IOException {
            return new TagString(new ByteBufferKaitaiStream(fileName));
        }

        public TagString(KaitaiStream _io) {
            this(_io, null, null);
        }

        public TagString(KaitaiStream _io, Hprof.Tag _parent) {
            this(_io, _parent, null);
        }

        public TagString(KaitaiStream _io, Hprof.Tag _parent, Hprof _root) {
            super(_io);
            this._parent = _parent;
            this._root = _root;
            _read();
        }
        private void _read() {
            this.stringId = new Uid(this._io, this, _root);
            this.string = new String(this._io.readBytes((_parent().bodyLength() - _root().header().uidSize())), Charset.forName("UTF-8"));
        }
        private Uid stringId;
        private String string;
        private Hprof _root;
        private Hprof.Tag _parent;
        public Uid stringId() { return stringId; }
        public String string() { return string; }
        public Hprof _root() { return _root; }
        public Hprof.Tag _parent() { return _parent; }
    }
    public static class StaticField extends KaitaiStruct {
        public static StaticField fromFile(String fileName) throws IOException {
            return new StaticField(new ByteBufferKaitaiStream(fileName));
        }

        public StaticField(KaitaiStream _io) {
            this(_io, null, null);
        }

        public StaticField(KaitaiStream _io, Hprof.SubTagClassDump _parent) {
            this(_io, _parent, null);
        }

        public StaticField(KaitaiStream _io, Hprof.SubTagClassDump _parent, Hprof _root) {
            super(_io);
            this._parent = _parent;
            this._root = _root;
            _read();
        }
        private void _read() {
            this.staticFieldNameStringId = new Uid(this._io, this, _root);
            this.fieldType = Hprof.BasicType.byId(this._io.readU1());
            {
                BasicType on = fieldType();
                if (on != null) {
                    switch (fieldType()) {
                    case BOOLEAN: {
                        this.value = (Object) (this._io.readU1());
                        break;
                    }
                    case OBJECT: {
                        this.value = new Uid(this._io, this, _root);
                        break;
                    }
                    case SHORT: {
                        this.value = (Object) (this._io.readU2be());
                        break;
                    }
                    case INT: {
                        this.value = (Object) (this._io.readU4be());
                        break;
                    }
                    case LONG: {
                        this.value = (Object) (this._io.readU8be());
                        break;
                    }
                    case BYTE: {
                        this.value = (Object) (this._io.readU1());
                        break;
                    }
                    case DOUBLE: {
                        this.value = (Object) (this._io.readU8be());
                        break;
                    }
                    case FLOAT: {
                        this.value = (Object) (this._io.readU4be());
                        break;
                    }
                    case CHAR: {
                        this.value = (Object) (this._io.readU2be());
                        break;
                    }
                    }
                }
            }
        }
        private Uid staticFieldNameStringId;
        private BasicType fieldType;
        private Object value;
        private Hprof _root;
        private Hprof.SubTagClassDump _parent;
        public Uid staticFieldNameStringId() { return staticFieldNameStringId; }
        public BasicType fieldType() { return fieldType; }
        public Object value() { return value; }
        public Hprof _root() { return _root; }
        public Hprof.SubTagClassDump _parent() { return _parent; }
    }
    public static class SubTagRootJniGlobal extends KaitaiStruct {
        public static SubTagRootJniGlobal fromFile(String fileName) throws IOException {
            return new SubTagRootJniGlobal(new ByteBufferKaitaiStream(fileName));
        }

        public SubTagRootJniGlobal(KaitaiStream _io) {
            this(_io, null, null);
        }

        public SubTagRootJniGlobal(KaitaiStream _io, Hprof.SubTag _parent) {
            this(_io, _parent, null);
        }

        public SubTagRootJniGlobal(KaitaiStream _io, Hprof.SubTag _parent, Hprof _root) {
            super(_io);
            this._parent = _parent;
            this._root = _root;
            _read();
        }
        private void _read() {
            this.objectId = new Uid(this._io, this, _root);
            this.jniGlobalRefId = new Uid(this._io, this, _root);
        }
        private Uid objectId;
        private Uid jniGlobalRefId;
        private Hprof _root;
        private Hprof.SubTag _parent;
        public Uid objectId() { return objectId; }
        public Uid jniGlobalRefId() { return jniGlobalRefId; }
        public Hprof _root() { return _root; }
        public Hprof.SubTag _parent() { return _parent; }
    }
    public static class SubTagInstanceDump extends KaitaiStruct {
        public static SubTagInstanceDump fromFile(String fileName) throws IOException {
            return new SubTagInstanceDump(new ByteBufferKaitaiStream(fileName));
        }

        public SubTagInstanceDump(KaitaiStream _io) {
            this(_io, null, null);
        }

        public SubTagInstanceDump(KaitaiStream _io, Hprof.SubTag _parent) {
            this(_io, _parent, null);
        }

        public SubTagInstanceDump(KaitaiStream _io, Hprof.SubTag _parent, Hprof _root) {
            super(_io);
            this._parent = _parent;
            this._root = _root;
            _read();
        }
        private void _read() {
            this.objectId = new Uid(this._io, this, _root);
            this.stackTraceSerialNum = this._io.readU4be();
            this.classObjectId = new Uid(this._io, this, _root);
            this.lenInstanceFieldValues = this._io.readU4be();
            this.instanceFieldValues = this._io.readBytes(lenInstanceFieldValues());
        }
        private Uid objectId;
        private long stackTraceSerialNum;
        private Uid classObjectId;
        private long lenInstanceFieldValues;
        private byte[] instanceFieldValues;
        private Hprof _root;
        private Hprof.SubTag _parent;
        public Uid objectId() { return objectId; }
        public long stackTraceSerialNum() { return stackTraceSerialNum; }
        public Uid classObjectId() { return classObjectId; }
        public long lenInstanceFieldValues() { return lenInstanceFieldValues; }
        public byte[] instanceFieldValues() { return instanceFieldValues; }
        public Hprof _root() { return _root; }
        public Hprof.SubTag _parent() { return _parent; }
    }
    public static class TagLoadClass extends KaitaiStruct {
        public static TagLoadClass fromFile(String fileName) throws IOException {
            return new TagLoadClass(new ByteBufferKaitaiStream(fileName));
        }

        public TagLoadClass(KaitaiStream _io) {
            this(_io, null, null);
        }

        public TagLoadClass(KaitaiStream _io, Hprof.Tag _parent) {
            this(_io, _parent, null);
        }

        public TagLoadClass(KaitaiStream _io, Hprof.Tag _parent, Hprof _root) {
            super(_io);
            this._parent = _parent;
            this._root = _root;
            _read();
        }
        private void _read() {
            this.classSerialNum = this._io.readU4be();
            this.classObjectId = new Uid(this._io, this, _root);
            this.stackTraceSerialNum = this._io.readU4be();
            this.classNameStringId = new Uid(this._io, this, _root);
        }
        private long classSerialNum;
        private Uid classObjectId;
        private long stackTraceSerialNum;
        private Uid classNameStringId;
        private Hprof _root;
        private Hprof.Tag _parent;
        public long classSerialNum() { return classSerialNum; }
        public Uid classObjectId() { return classObjectId; }
        public long stackTraceSerialNum() { return stackTraceSerialNum; }
        public Uid classNameStringId() { return classNameStringId; }
        public Hprof _root() { return _root; }
        public Hprof.Tag _parent() { return _parent; }
    }
    public static class SubTagRootNativeStack extends KaitaiStruct {
        public static SubTagRootNativeStack fromFile(String fileName) throws IOException {
            return new SubTagRootNativeStack(new ByteBufferKaitaiStream(fileName));
        }

        public SubTagRootNativeStack(KaitaiStream _io) {
            this(_io, null, null);
        }

        public SubTagRootNativeStack(KaitaiStream _io, Hprof.SubTag _parent) {
            this(_io, _parent, null);
        }

        public SubTagRootNativeStack(KaitaiStream _io, Hprof.SubTag _parent, Hprof _root) {
            super(_io);
            this._parent = _parent;
            this._root = _root;
            _read();
        }
        private void _read() {
            this.objectId = new Uid(this._io, this, _root);
            this.threadSerialNum = this._io.readU4be();
        }
        private Uid objectId;
        private long threadSerialNum;
        private Hprof _root;
        private Hprof.SubTag _parent;
        public Uid objectId() { return objectId; }
        public long threadSerialNum() { return threadSerialNum; }
        public Hprof _root() { return _root; }
        public Hprof.SubTag _parent() { return _parent; }
    }
    public static class TagStackFrame extends KaitaiStruct {
        public static TagStackFrame fromFile(String fileName) throws IOException {
            return new TagStackFrame(new ByteBufferKaitaiStream(fileName));
        }

        public TagStackFrame(KaitaiStream _io) {
            this(_io, null, null);
        }

        public TagStackFrame(KaitaiStream _io, Hprof.Tag _parent) {
            this(_io, _parent, null);
        }

        public TagStackFrame(KaitaiStream _io, Hprof.Tag _parent, Hprof _root) {
            super(_io);
            this._parent = _parent;
            this._root = _root;
            _read();
        }
        private void _read() {
            this.stackFrameId = new Uid(this._io, this, _root);
            this.methodNameStringId = new Uid(this._io, this, _root);
            this.methodSignatureStringId = new Uid(this._io, this, _root);
            this.sourceFileNameStringId = new Uid(this._io, this, _root);
            this.classSerialNum = this._io.readU4be();
            this.lineNum = this._io.readS4be();
        }
        private Uid stackFrameId;
        private Uid methodNameStringId;
        private Uid methodSignatureStringId;
        private Uid sourceFileNameStringId;
        private long classSerialNum;
        private int lineNum;
        private Hprof _root;
        private Hprof.Tag _parent;
        public Uid stackFrameId() { return stackFrameId; }
        public Uid methodNameStringId() { return methodNameStringId; }
        public Uid methodSignatureStringId() { return methodSignatureStringId; }
        public Uid sourceFileNameStringId() { return sourceFileNameStringId; }
        public long classSerialNum() { return classSerialNum; }
        public int lineNum() { return lineNum; }
        public Hprof _root() { return _root; }
        public Hprof.Tag _parent() { return _parent; }
    }
    public static class SubTag extends KaitaiStruct {
        public static SubTag fromFile(String fileName) throws IOException {
            return new SubTag(new ByteBufferKaitaiStream(fileName));
        }

        public SubTag(KaitaiStream _io) {
            this(_io, null, null);
        }

        public SubTag(KaitaiStream _io, KaitaiStruct _parent) {
            this(_io, _parent, null);
        }

        public SubTag(KaitaiStream _io, KaitaiStruct _parent, Hprof _root) {
            super(_io);
            this._parent = _parent;
            this._root = _root;
            _read();
        }
        private void _read() {
            this.subRecordType = Hprof.SubTagType.byId(this._io.readU1());
            {
                SubTagType on = subRecordType();
                if (on != null) {
                    switch (subRecordType()) {
                    case ROOT_THREAD_OBJECT: {
                        this.body = new SubTagRootThreadObject(this._io, this, _root);
                        break;
                    }
                    case OBJECT_ARRAY_DUMP: {
                        this.body = new SubTagObjectArrayDump(this._io, this, _root);
                        break;
                    }
                    case ROOT_STICKY_CLASS: {
                        this.body = new SubTagRootStickyClass(this._io, this, _root);
                        break;
                    }
                    case ROOT_UNKNOWN: {
                        this.body = new SubTagRootUnknown(this._io, this, _root);
                        break;
                    }
                    case PRIMITIVE_ARRAY_DUMP: {
                        this.body = new SubTagPrimitiveArrayDump(this._io, this, _root);
                        break;
                    }
                    case ROOT_JNI_GLOBAL: {
                        this.body = new SubTagRootJniGlobal(this._io, this, _root);
                        break;
                    }
                    case ROOT_THREAD_BLOCK: {
                        this.body = new SubTagRootThreadBlock(this._io, this, _root);
                        break;
                    }
                    case ROOT_MONITOR_USED: {
                        this.body = new SubTagRootMonitorUsed(this._io, this, _root);
                        break;
                    }
                    case CLASS_DUMP: {
                        this.body = new SubTagClassDump(this._io, this, _root);
                        break;
                    }
                    case ROOT_NATIVE_STACK: {
                        this.body = new SubTagRootNativeStack(this._io, this, _root);
                        break;
                    }
                    case ROOT_JNI_LOCAL: {
                        this.body = new SubTagRootJniLocal(this._io, this, _root);
                        break;
                    }
                    case ROOT_JAVA_FRAME: {
                        this.body = new SubTagRootJavaFrame(this._io, this, _root);
                        break;
                    }
                    case INSTANCE_DUMP: {
                        this.body = new SubTagInstanceDump(this._io, this, _root);
                        break;
                    }
                    }
                }
            }
        }
        private SubTagType subRecordType;
        private KaitaiStruct body;
        private Hprof _root;
        private KaitaiStruct _parent;
        public SubTagType subRecordType() { return subRecordType; }
        public KaitaiStruct body() { return body; }
        public Hprof _root() { return _root; }
        public KaitaiStruct _parent() { return _parent; }
    }
    public static class TagHeapDumpSegment extends KaitaiStruct {
        public static TagHeapDumpSegment fromFile(String fileName) throws IOException {
            return new TagHeapDumpSegment(new ByteBufferKaitaiStream(fileName));
        }

        public TagHeapDumpSegment(KaitaiStream _io) {
            this(_io, null, null);
        }

        public TagHeapDumpSegment(KaitaiStream _io, Hprof.Tag _parent) {
            this(_io, _parent, null);
        }

        public TagHeapDumpSegment(KaitaiStream _io, Hprof.Tag _parent, Hprof _root) {
            super(_io);
            this._parent = _parent;
            this._root = _root;
            _read();
        }
        private void _read() {
            this.subTags = new ArrayList<SubTag>();
            {
                int i = 0;
                while (!this._io.isEof()) {
                    this.subTags.add(new SubTag(this._io, this, _root));
                    i++;
                }
            }
        }
        private ArrayList<SubTag> subTags;
        private Hprof _root;
        private Hprof.Tag _parent;
        public ArrayList<SubTag> subTags() { return subTags; }
        public Hprof _root() { return _root; }
        public Hprof.Tag _parent() { return _parent; }
    }
    public static class InstanceField extends KaitaiStruct {
        public static InstanceField fromFile(String fileName) throws IOException {
            return new InstanceField(new ByteBufferKaitaiStream(fileName));
        }

        public InstanceField(KaitaiStream _io) {
            this(_io, null, null);
        }

        public InstanceField(KaitaiStream _io, Hprof.SubTagClassDump _parent) {
            this(_io, _parent, null);
        }

        public InstanceField(KaitaiStream _io, Hprof.SubTagClassDump _parent, Hprof _root) {
            super(_io);
            this._parent = _parent;
            this._root = _root;
            _read();
        }
        private void _read() {
            this.instanceFieldNameStringId = new Uid(this._io, this, _root);
            this.fieldType = Hprof.BasicType.byId(this._io.readU1());
        }
        private Uid instanceFieldNameStringId;
        private BasicType fieldType;
        private Hprof _root;
        private Hprof.SubTagClassDump _parent;
        public Uid instanceFieldNameStringId() { return instanceFieldNameStringId; }
        public BasicType fieldType() { return fieldType; }
        public Hprof _root() { return _root; }
        public Hprof.SubTagClassDump _parent() { return _parent; }
    }
    public static class SubTagRootMonitorUsed extends KaitaiStruct {
        public static SubTagRootMonitorUsed fromFile(String fileName) throws IOException {
            return new SubTagRootMonitorUsed(new ByteBufferKaitaiStream(fileName));
        }

        public SubTagRootMonitorUsed(KaitaiStream _io) {
            this(_io, null, null);
        }

        public SubTagRootMonitorUsed(KaitaiStream _io, Hprof.SubTag _parent) {
            this(_io, _parent, null);
        }

        public SubTagRootMonitorUsed(KaitaiStream _io, Hprof.SubTag _parent, Hprof _root) {
            super(_io);
            this._parent = _parent;
            this._root = _root;
            _read();
        }
        private void _read() {
            this.objectId = new Uid(this._io, this, _root);
        }
        private Uid objectId;
        private Hprof _root;
        private Hprof.SubTag _parent;
        public Uid objectId() { return objectId; }
        public Hprof _root() { return _root; }
        public Hprof.SubTag _parent() { return _parent; }
    }
    public static class SubTagPrimitiveArrayDump extends KaitaiStruct {
        public static SubTagPrimitiveArrayDump fromFile(String fileName) throws IOException {
            return new SubTagPrimitiveArrayDump(new ByteBufferKaitaiStream(fileName));
        }

        public SubTagPrimitiveArrayDump(KaitaiStream _io) {
            this(_io, null, null);
        }

        public SubTagPrimitiveArrayDump(KaitaiStream _io, Hprof.SubTag _parent) {
            this(_io, _parent, null);
        }

        public SubTagPrimitiveArrayDump(KaitaiStream _io, Hprof.SubTag _parent, Hprof _root) {
            super(_io);
            this._parent = _parent;
            this._root = _root;
            _read();
        }
        private void _read() {
            this.arrayObjectId = new Uid(this._io, this, _root);
            this.stackTraceSerialNum = this._io.readU4be();
            this.numElements = this._io.readU4be();
            this.elementType = Hprof.BasicType.byId(this._io.readU1());
            this.elements = new ArrayList<Object>();
            for (int i = 0; i < numElements(); i++) {
                {
                    BasicType on = elementType();
                    if (on != null) {
                        switch (elementType()) {
                        case BOOLEAN: {
                            this.elements.add((Object) (this._io.readU1()));
                            break;
                        }
                        case OBJECT: {
                            this.elements.add(new Uid(this._io, this, _root));
                            break;
                        }
                        case SHORT: {
                            this.elements.add((Object) (this._io.readU2be()));
                            break;
                        }
                        case INT: {
                            this.elements.add((Object) (this._io.readU4be()));
                            break;
                        }
                        case LONG: {
                            this.elements.add((Object) (this._io.readU8be()));
                            break;
                        }
                        case BYTE: {
                            this.elements.add((Object) (this._io.readU1()));
                            break;
                        }
                        case DOUBLE: {
                            this.elements.add((Object) (this._io.readU8be()));
                            break;
                        }
                        case FLOAT: {
                            this.elements.add((Object) (this._io.readU4be()));
                            break;
                        }
                        case CHAR: {
                            this.elements.add((Object) (this._io.readU2be()));
                            break;
                        }
                        }
                    }
                }
            }
        }
        private Uid arrayObjectId;
        private long stackTraceSerialNum;
        private long numElements;
        private BasicType elementType;
        private ArrayList<Object> elements;
        private Hprof _root;
        private Hprof.SubTag _parent;
        public Uid arrayObjectId() { return arrayObjectId; }
        public long stackTraceSerialNum() { return stackTraceSerialNum; }
        public long numElements() { return numElements; }
        public BasicType elementType() { return elementType; }
        public ArrayList<Object> elements() { return elements; }
        public Hprof _root() { return _root; }
        public Hprof.SubTag _parent() { return _parent; }
    }
    public static class TagHeapDump extends KaitaiStruct {
        public static TagHeapDump fromFile(String fileName) throws IOException {
            return new TagHeapDump(new ByteBufferKaitaiStream(fileName));
        }

        public TagHeapDump(KaitaiStream _io) {
            this(_io, null, null);
        }

        public TagHeapDump(KaitaiStream _io, Hprof.Tag _parent) {
            this(_io, _parent, null);
        }

        public TagHeapDump(KaitaiStream _io, Hprof.Tag _parent, Hprof _root) {
            super(_io);
            this._parent = _parent;
            this._root = _root;
            _read();
        }
        private void _read() {
            this.subTags = new ArrayList<SubTag>();
            {
                int i = 0;
                while (!this._io.isEof()) {
                    this.subTags.add(new SubTag(this._io, this, _root));
                    i++;
                }
            }
        }
        private ArrayList<SubTag> subTags;
        private Hprof _root;
        private Hprof.Tag _parent;
        public ArrayList<SubTag> subTags() { return subTags; }
        public Hprof _root() { return _root; }
        public Hprof.Tag _parent() { return _parent; }
    }
    public static class CpuTrace extends KaitaiStruct {
        public static CpuTrace fromFile(String fileName) throws IOException {
            return new CpuTrace(new ByteBufferKaitaiStream(fileName));
        }

        public CpuTrace(KaitaiStream _io) {
            this(_io, null, null);
        }

        public CpuTrace(KaitaiStream _io, Hprof.TagCpuSamples _parent) {
            this(_io, _parent, null);
        }

        public CpuTrace(KaitaiStream _io, Hprof.TagCpuSamples _parent, Hprof _root) {
            super(_io);
            this._parent = _parent;
            this._root = _root;
            _read();
        }
        private void _read() {
            this.numSamples = this._io.readU4be();
            this.stackTraceSerialNum = this._io.readU4be();
        }
        private long numSamples;
        private long stackTraceSerialNum;
        private Hprof _root;
        private Hprof.TagCpuSamples _parent;
        public long numSamples() { return numSamples; }
        public long stackTraceSerialNum() { return stackTraceSerialNum; }
        public Hprof _root() { return _root; }
        public Hprof.TagCpuSamples _parent() { return _parent; }
    }
    public static class SubTagRootJniLocal extends KaitaiStruct {
        public static SubTagRootJniLocal fromFile(String fileName) throws IOException {
            return new SubTagRootJniLocal(new ByteBufferKaitaiStream(fileName));
        }

        public SubTagRootJniLocal(KaitaiStream _io) {
            this(_io, null, null);
        }

        public SubTagRootJniLocal(KaitaiStream _io, Hprof.SubTag _parent) {
            this(_io, _parent, null);
        }

        public SubTagRootJniLocal(KaitaiStream _io, Hprof.SubTag _parent, Hprof _root) {
            super(_io);
            this._parent = _parent;
            this._root = _root;
            _read();
        }
        private void _read() {
            this.objectId = new Uid(this._io, this, _root);
            this.threadSerialNum = this._io.readU4be();
            this.stackFrameNum = this._io.readS4be();
        }
        private Uid objectId;
        private long threadSerialNum;
        private int stackFrameNum;
        private Hprof _root;
        private Hprof.SubTag _parent;
        public Uid objectId() { return objectId; }
        public long threadSerialNum() { return threadSerialNum; }
        public int stackFrameNum() { return stackFrameNum; }
        public Hprof _root() { return _root; }
        public Hprof.SubTag _parent() { return _parent; }
    }
    public static class Uid extends KaitaiStruct {
        public static Uid fromFile(String fileName) throws IOException {
            return new Uid(new ByteBufferKaitaiStream(fileName));
        }

        public Uid(KaitaiStream _io) {
            this(_io, null, null);
        }

        public Uid(KaitaiStream _io, KaitaiStruct _parent) {
            this(_io, _parent, null);
        }

        public Uid(KaitaiStream _io, KaitaiStruct _parent, Hprof _root) {
            super(_io);
            this._parent = _parent;
            this._root = _root;
            _read();
        }
        private void _read() {
            switch (((int) (_root().header().uidSize()))) {
            case 4: {
                this.identifier = (long) (this._io.readU4be());
                break;
            }
            case 8: {
                this.identifier = this._io.readU8be();
                break;
            }
            }
        }
        private Long identifier;
        private Hprof _root;
        private KaitaiStruct _parent;
        public Long identifier() { return identifier; }
        public Hprof _root() { return _root; }
        public KaitaiStruct _parent() { return _parent; }
    }
    public static class SubTagRootThreadBlock extends KaitaiStruct {
        public static SubTagRootThreadBlock fromFile(String fileName) throws IOException {
            return new SubTagRootThreadBlock(new ByteBufferKaitaiStream(fileName));
        }

        public SubTagRootThreadBlock(KaitaiStream _io) {
            this(_io, null, null);
        }

        public SubTagRootThreadBlock(KaitaiStream _io, Hprof.SubTag _parent) {
            this(_io, _parent, null);
        }

        public SubTagRootThreadBlock(KaitaiStream _io, Hprof.SubTag _parent, Hprof _root) {
            super(_io);
            this._parent = _parent;
            this._root = _root;
            _read();
        }
        private void _read() {
            this.objectId = new Uid(this._io, this, _root);
            this.threadSerialNum = this._io.readU4be();
        }
        private Uid objectId;
        private long threadSerialNum;
        private Hprof _root;
        private Hprof.SubTag _parent;
        public Uid objectId() { return objectId; }
        public long threadSerialNum() { return threadSerialNum; }
        public Hprof _root() { return _root; }
        public Hprof.SubTag _parent() { return _parent; }
    }
    public static class Header extends KaitaiStruct {
        public static Header fromFile(String fileName) throws IOException {
            return new Header(new ByteBufferKaitaiStream(fileName));
        }

        public Header(KaitaiStream _io) {
            this(_io, null, null);
        }

        public Header(KaitaiStream _io, Hprof _parent) {
            this(_io, _parent, null);
        }

        public Header(KaitaiStream _io, Hprof _parent, Hprof _root) {
            super(_io);
            this._parent = _parent;
            this._root = _root;
            _read();
        }
        private void _read() {
            this.profileVersion = new String(this._io.readBytesTerm((byte) 0, false, true, true), Charset.forName("UTF-8"));
            this.uidSize = this._io.readU4be();
            this.gmtMs = this._io.readU8be();
        }
        private String profileVersion;
        private long uidSize;
        private long gmtMs;
        private Hprof _root;
        private Hprof _parent;
        public String profileVersion() { return profileVersion; }
        public long uidSize() { return uidSize; }
        public long gmtMs() { return gmtMs; }
        public Hprof _root() { return _root; }
        public Hprof _parent() { return _parent; }
    }
    public static class TagHeapSummary extends KaitaiStruct {
        public static TagHeapSummary fromFile(String fileName) throws IOException {
            return new TagHeapSummary(new ByteBufferKaitaiStream(fileName));
        }

        public TagHeapSummary(KaitaiStream _io) {
            this(_io, null, null);
        }

        public TagHeapSummary(KaitaiStream _io, Hprof.Tag _parent) {
            this(_io, _parent, null);
        }

        public TagHeapSummary(KaitaiStream _io, Hprof.Tag _parent, Hprof _root) {
            super(_io);
            this._parent = _parent;
            this._root = _root;
            _read();
        }
        private void _read() {
            this.totalLiveBytes = this._io.readU4be();
            this.totalLiveInstances = this._io.readU4be();
            this.totalBytesAllocated = this._io.readU8be();
            this.totalInstancesAllocated = this._io.readU8be();
        }
        private long totalLiveBytes;
        private long totalLiveInstances;
        private long totalBytesAllocated;
        private long totalInstancesAllocated;
        private Hprof _root;
        private Hprof.Tag _parent;
        public long totalLiveBytes() { return totalLiveBytes; }
        public long totalLiveInstances() { return totalLiveInstances; }
        public long totalBytesAllocated() { return totalBytesAllocated; }
        public long totalInstancesAllocated() { return totalInstancesAllocated; }
        public Hprof _root() { return _root; }
        public Hprof.Tag _parent() { return _parent; }
    }
    public static class SubTagRootJavaFrame extends KaitaiStruct {
        public static SubTagRootJavaFrame fromFile(String fileName) throws IOException {
            return new SubTagRootJavaFrame(new ByteBufferKaitaiStream(fileName));
        }

        public SubTagRootJavaFrame(KaitaiStream _io) {
            this(_io, null, null);
        }

        public SubTagRootJavaFrame(KaitaiStream _io, Hprof.SubTag _parent) {
            this(_io, _parent, null);
        }

        public SubTagRootJavaFrame(KaitaiStream _io, Hprof.SubTag _parent, Hprof _root) {
            super(_io);
            this._parent = _parent;
            this._root = _root;
            _read();
        }
        private void _read() {
            this.objectId = new Uid(this._io, this, _root);
            this.threadSerialNum = this._io.readU4be();
            this.stackFrameNum = this._io.readS4be();
        }
        private Uid objectId;
        private long threadSerialNum;
        private int stackFrameNum;
        private Hprof _root;
        private Hprof.SubTag _parent;
        public Uid objectId() { return objectId; }
        public long threadSerialNum() { return threadSerialNum; }
        public int stackFrameNum() { return stackFrameNum; }
        public Hprof _root() { return _root; }
        public Hprof.SubTag _parent() { return _parent; }
    }

    /**
     * Tag body for HEAP_DUMP_END is of size 0 bytes.
     */
    public static class TagHeapDumpEnd extends KaitaiStruct {
        public static TagHeapDumpEnd fromFile(String fileName) throws IOException {
            return new TagHeapDumpEnd(new ByteBufferKaitaiStream(fileName));
        }

        public TagHeapDumpEnd(KaitaiStream _io) {
            this(_io, null, null);
        }

        public TagHeapDumpEnd(KaitaiStream _io, Hprof.Tag _parent) {
            this(_io, _parent, null);
        }

        public TagHeapDumpEnd(KaitaiStream _io, Hprof.Tag _parent, Hprof _root) {
            super(_io);
            this._parent = _parent;
            this._root = _root;
            _read();
        }
        private void _read() {
            this.noValue = this._io.readBytes(0);
        }
        private byte[] noValue;
        private Hprof _root;
        private Hprof.Tag _parent;
        public byte[] noValue() { return noValue; }
        public Hprof _root() { return _root; }
        public Hprof.Tag _parent() { return _parent; }
    }
    public static class TagUnloadClass extends KaitaiStruct {
        public static TagUnloadClass fromFile(String fileName) throws IOException {
            return new TagUnloadClass(new ByteBufferKaitaiStream(fileName));
        }

        public TagUnloadClass(KaitaiStream _io) {
            this(_io, null, null);
        }

        public TagUnloadClass(KaitaiStream _io, Hprof.Tag _parent) {
            this(_io, _parent, null);
        }

        public TagUnloadClass(KaitaiStream _io, Hprof.Tag _parent, Hprof _root) {
            super(_io);
            this._parent = _parent;
            this._root = _root;
            _read();
        }
        private void _read() {
            this.classSerialNum = this._io.readU4be();
        }
        private long classSerialNum;
        private Hprof _root;
        private Hprof.Tag _parent;
        public long classSerialNum() { return classSerialNum; }
        public Hprof _root() { return _root; }
        public Hprof.Tag _parent() { return _parent; }
    }
    public static class ConstPoolEntry extends KaitaiStruct {
        public static ConstPoolEntry fromFile(String fileName) throws IOException {
            return new ConstPoolEntry(new ByteBufferKaitaiStream(fileName));
        }

        public ConstPoolEntry(KaitaiStream _io) {
            this(_io, null, null);
        }

        public ConstPoolEntry(KaitaiStream _io, Hprof.SubTagClassDump _parent) {
            this(_io, _parent, null);
        }

        public ConstPoolEntry(KaitaiStream _io, Hprof.SubTagClassDump _parent, Hprof _root) {
            super(_io);
            this._parent = _parent;
            this._root = _root;
            _read();
        }
        private void _read() {
            this.constPoolIndex = this._io.readU2be();
            this.entryType = Hprof.BasicType.byId(this._io.readU1());
            {
                BasicType on = entryType();
                if (on != null) {
                    switch (entryType()) {
                    case BOOLEAN: {
                        this.value = (Object) (this._io.readU1());
                        break;
                    }
                    case OBJECT: {
                        this.value = new Uid(this._io, this, _root);
                        break;
                    }
                    case SHORT: {
                        this.value = (Object) (this._io.readU2be());
                        break;
                    }
                    case INT: {
                        this.value = (Object) (this._io.readU4be());
                        break;
                    }
                    case LONG: {
                        this.value = (Object) (this._io.readU8be());
                        break;
                    }
                    case BYTE: {
                        this.value = (Object) (this._io.readU1());
                        break;
                    }
                    case DOUBLE: {
                        this.value = (Object) (this._io.readU8be());
                        break;
                    }
                    case FLOAT: {
                        this.value = (Object) (this._io.readU4be());
                        break;
                    }
                    case CHAR: {
                        this.value = (Object) (this._io.readU2be());
                        break;
                    }
                    }
                }
            }
        }
        private int constPoolIndex;
        private BasicType entryType;
        private Object value;
        private Hprof _root;
        private Hprof.SubTagClassDump _parent;
        public int constPoolIndex() { return constPoolIndex; }
        public BasicType entryType() { return entryType; }
        public Object value() { return value; }
        public Hprof _root() { return _root; }
        public Hprof.SubTagClassDump _parent() { return _parent; }
    }
    public static class SubTagClassDump extends KaitaiStruct {
        public static SubTagClassDump fromFile(String fileName) throws IOException {
            return new SubTagClassDump(new ByteBufferKaitaiStream(fileName));
        }

        public SubTagClassDump(KaitaiStream _io) {
            this(_io, null, null);
        }

        public SubTagClassDump(KaitaiStream _io, Hprof.SubTag _parent) {
            this(_io, _parent, null);
        }

        public SubTagClassDump(KaitaiStream _io, Hprof.SubTag _parent, Hprof _root) {
            super(_io);
            this._parent = _parent;
            this._root = _root;
            _read();
        }
        private void _read() {
            this.classObjectId = new Uid(this._io, this, _root);
            this.stackTraceSerialNum = this._io.readU4be();
            this.superClassObjectId = new Uid(this._io, this, _root);
            this.classLoaderObjectId = new Uid(this._io, this, _root);
            this.signersObjectId = new Uid(this._io, this, _root);
            this.protectionDomainObjectId = new Uid(this._io, this, _root);
            this.reserved0 = new Uid(this._io, this, _root);
            this.reserved1 = new Uid(this._io, this, _root);
            this.instanceSize = this._io.readU4be();
            this.numConstPoolEntries = this._io.readU2be();
            this.constPoolEntries = new ArrayList<ConstPoolEntry>();
            for (int i = 0; i < numConstPoolEntries(); i++) {
                this.constPoolEntries.add(new ConstPoolEntry(this._io, this, _root));
            }
            this.numStaticFields = this._io.readU2be();
            this.staticFields = new ArrayList<StaticField>();
            for (int i = 0; i < numStaticFields(); i++) {
                this.staticFields.add(new StaticField(this._io, this, _root));
            }
            this.numInstanceFields = this._io.readU2be();
            this.instanceFields = new ArrayList<InstanceField>();
            for (int i = 0; i < numInstanceFields(); i++) {
                this.instanceFields.add(new InstanceField(this._io, this, _root));
            }
        }
        private Uid classObjectId;
        private long stackTraceSerialNum;
        private Uid superClassObjectId;
        private Uid classLoaderObjectId;
        private Uid signersObjectId;
        private Uid protectionDomainObjectId;
        private Uid reserved0;
        private Uid reserved1;
        private long instanceSize;
        private int numConstPoolEntries;
        private ArrayList<ConstPoolEntry> constPoolEntries;
        private int numStaticFields;
        private ArrayList<StaticField> staticFields;
        private int numInstanceFields;
        private ArrayList<InstanceField> instanceFields;
        private Hprof _root;
        private Hprof.SubTag _parent;
        public Uid classObjectId() { return classObjectId; }
        public long stackTraceSerialNum() { return stackTraceSerialNum; }
        public Uid superClassObjectId() { return superClassObjectId; }
        public Uid classLoaderObjectId() { return classLoaderObjectId; }
        public Uid signersObjectId() { return signersObjectId; }
        public Uid protectionDomainObjectId() { return protectionDomainObjectId; }
        public Uid reserved0() { return reserved0; }
        public Uid reserved1() { return reserved1; }
        public long instanceSize() { return instanceSize; }
        public int numConstPoolEntries() { return numConstPoolEntries; }
        public ArrayList<ConstPoolEntry> constPoolEntries() { return constPoolEntries; }
        public int numStaticFields() { return numStaticFields; }
        public ArrayList<StaticField> staticFields() { return staticFields; }
        public int numInstanceFields() { return numInstanceFields; }
        public ArrayList<InstanceField> instanceFields() { return instanceFields; }
        public Hprof _root() { return _root; }
        public Hprof.SubTag _parent() { return _parent; }
    }
    private Header header;
    private ArrayList<Tag> tags;
    private Hprof _root;
    private KaitaiStruct _parent;
    public Header header() { return header; }
    public ArrayList<Tag> tags() { return tags; }
    public Hprof _root() { return _root; }
    public KaitaiStruct _parent() { return _parent; }
}
