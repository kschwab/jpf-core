package gov.nasa.jpf.vm.hprof;

import com.squareup.haha.perflib.ClassInstance;
import com.squareup.haha.perflib.ClassObj;
import com.squareup.haha.perflib.Instance;
import com.squareup.haha.perflib.Snapshot;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.LinkedHashMap;
import java.util.Map;

/** Post-processes H.5 capture tags into the unchanged bundle.version=1 manifest. */
public final class HprofLoaderHierarchyBundleBuilder {
  private static final String LOADER_TAG = "HprofLoaderHierarchyCapture$LoaderTag";
  private static final String CLASS_TAG = "HprofLoaderHierarchyCapture$ClassTag";
  private static final String[] NAMES = {"hprof.loader.Contract", "hprof.loader.Base", "hprof.loader.Derived"};

  public static void main(String[] args) throws Exception {
    Path root = Path.of(args[0]).toAbsolutePath().normalize();
    Path heap = root.resolve("heap.hprof");
    Snapshot snapshot = HprofSnapshotLoader.load(heap.toFile());
    HprofView view = HprofView.from(snapshot);
    Map<Long, Instance> loaders = loaderTags(view);
    Map<Long, ClassTag> tags = classTags(view);
    require(loaders.size() == 1 && tags.size() == 3, "unexpected hierarchy anchors");
    Instance loader = loaders.get(1L);
    StringBuilder manifest = new StringBuilder("bundle.version=1\nhprof.file=heap.hprof\n");
    manifest.append("hprof.sha256=").append(HprofCheckpointBundle.sha256(heap)).append('\n');
    manifest.append("loader.count=1\nloader.1.hprof_id=0x").append(Long.toHexString(loader.getId())).append('\n');
    manifest.append("class.count=3\n");
    for (int i=1;i<=3;i++) {
      ClassTag tag=tags.get((long)i); require(tag != null && tag.loader == loader && tag.logicalLoaderId == 1, "class tag mismatch " + i);
      ClassObj ci=findClass(snapshot, loader, NAMES[i-1]);
      Path artifact=root.resolve("classes/class-"+i+".class");
      manifest.append("class.").append(i).append(".loader=1\nclass.").append(i).append(".name=").append(NAMES[i-1]).append('\n');
      manifest.append("class.").append(i).append(".hprof_id=0x").append(Long.toHexString(ci.getId())).append('\n');
      manifest.append("class.").append(i).append(".file=classes/class-").append(i).append(".class\n");
      manifest.append("class.").append(i).append(".sha256=").append(HprofCheckpointBundle.sha256(artifact)).append('\n');
      System.out.printf("[HPROF-JPF] hierarchy correlation class=%s loaderHPROF=0x%x classHPROF=0x%x%n",NAMES[i-1],loader.getId(),ci.getId());
    }
    Files.writeString(root.resolve("bundle.properties"), manifest, StandardCharsets.UTF_8);
    HprofCheckpointBundle.loadAndVerify(root.toFile());
    ClassObj derived=findClass(snapshot,loader,NAMES[2]);
    ClassObj base=findClass(snapshot,loader,NAMES[1]);
    require(derived.getSuperClassObj() == base, "HAHA Derived superclass identity mismatch");
    System.out.println("[HPROF-JPF] hierarchy bundle verified: superclassInHPROF=true interfacesInClassfile=true classes=3");
  }

  private static Map<Long,Instance> loaderTags(HprofView view){Map<Long,Instance> r=new LinkedHashMap<>();for(Instance raw:view.instances.values())if(raw instanceof ClassInstance&&LOADER_TAG.equals(raw.getClassObj().getClassName())){ClassInstance t=(ClassInstance)raw;r.put(longField(t,"logicalLoaderId"),instanceField(t,"loader"));}return r;}
  private static Map<Long,ClassTag> classTags(HprofView view){Map<Long,ClassTag> r=new LinkedHashMap<>();for(Instance raw:view.instances.values())if(raw instanceof ClassInstance&&CLASS_TAG.equals(raw.getClassObj().getClassName())){ClassInstance t=(ClassInstance)raw;r.put(longField(t,"logicalClassId"),new ClassTag(longField(t,"logicalLoaderId"),instanceField(t,"loader")));}return r;}
  private static ClassObj findClass(Snapshot snapshot,Instance loader,String name){ClassObj found=null;String h=name.replace('.','/');for(ClassObj c:snapshot.findClasses(h))if(c.getClassLoader()==loader){require(found==null,"duplicate class "+name);found=c;}require(found!=null,"missing class "+name);return found;}
  private static Object field(ClassInstance i,String n){for(ClassInstance.FieldValue v:i.getValues())if(n.equals(v.getField().getName()))return v.getValue();throw new IllegalStateException("missing tag field "+n);}
  private static long longField(ClassInstance i,String n){return (Long)field(i,n);} private static Instance instanceField(ClassInstance i,String n){return (Instance)field(i,n);}
  private static void require(boolean value,String message){if(!value)throw new IllegalStateException("[HPROF-JPF] hierarchy bundle failed: "+message);}
  private static final class ClassTag{final long logicalLoaderId;final Instance loader;ClassTag(long id,Instance loader){this.logicalLoaderId=id;this.loader=loader;}}
}
