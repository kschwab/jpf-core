package gov.nasa.jpf.vm.hprof;

import com.squareup.haha.perflib.ClassInstance;
import com.squareup.haha.perflib.ClassObj;
import com.squareup.haha.perflib.Instance;
import gov.nasa.jpf.vm.ClassInfo;
import gov.nasa.jpf.vm.ClassLoaderInfo;
import gov.nasa.jpf.vm.ElementInfo;
import gov.nasa.jpf.vm.FieldInfo;
import gov.nasa.jpf.vm.StaticElementInfo;
import gov.nasa.jpf.vm.ThreadInfo;
import gov.nasa.jpf.vm.VM;
import java.util.LinkedHashMap;
import java.util.Map;

/** H.5 exact hierarchy/field checks and bounded modeled root binding. */
public final class HprofLoaderHierarchyValidator {
  public static void validateAndBind(VM vm,HprofView view,HprofCheckpointBundle bundle,HprofLoaderImportContext context,JpfHeapImporter.ImportResult result){
    require(context.getLoaderMap().size()==1,"expected one loader");
    require(context.getClassMap().size()==3,"expected three bundled classes");
    Map<String,ClassInfo> classes=new LinkedHashMap<>();
    Map<String,ClassObj> sources=new LinkedHashMap<>();
    for(HprofCheckpointBundle.ClassDefinition d:bundle.classes){classes.put(d.binaryName,context.getClassInfo(d.hprofClassId));sources.put(d.binaryName,view.classes.get(d.hprofClassId));}
    ClassInfo contract=required(classes,"hprof.loader.Contract"); ClassInfo base=required(classes,"hprof.loader.Base"); ClassInfo derived=required(classes,"hprof.loader.Derived");
    ClassLoaderInfo system=ClassLoaderInfo.getCurrentSystemClassLoader();
    require(contract.getClassLoaderInfo()!=system&&base.getClassLoaderInfo()!=system&&derived.getClassLoaderInfo()!=system,"system-loader substitution");
    require(contract.getClassLoaderInfo()==base.getClassLoaderInfo()&&base.getClassLoaderInfo()==derived.getClassLoaderInfo(),"hierarchy loaders differ");
    require(derived.getSuperClass()==base,"Derived superclass is not exact bundled Base");
    require(derived.getInterfaceClassInfos().contains(contract),"Derived interface is not exact bundled Contract");
    ClassInstance source=exactlyOne(view,sources.get("hprof.loader.Derived")); Integer ref=result.getRefMap().get(source.getId()); require(ref!=null,"Derived instance is unmapped");
    ElementInfo ei=vm.getHeap().get(ref); require(ei!=null&&ei.getClassInfo()==derived,"Derived object has wrong ClassInfo");
    FieldInfo baseField=base.getDeclaredInstanceField("baseField"); FieldInfo derivedField=derived.getDeclaredInstanceField("derivedField");
    require(baseField!=null&&baseField.getClassInfo()==base,"Base FieldInfo identity mismatch"); require(derivedField!=null&&derivedField.getClassInfo()==derived,"Derived FieldInfo identity mismatch");
    require(ei.getIntField(baseField)==1111&&ei.getIntField(derivedField)==2222,"captured fields not restored");
    bind(vm,ref);
    System.out.printf("[HPROF-JPF] loader hierarchy verified: Derived(loader=%d) extends Base(loader=%d) implements Contract(loader=%d) fields={1111,2222} systemFallback=false%n",derived.getClassLoaderInfo().getId(),base.getClassLoaderInfo().getId(),contract.getClassLoaderInfo().getId());
  }
  private static ClassInstance exactlyOne(HprofView view,ClassObj ci){ClassInstance found=null;for(Instance i:view.instances.values())if(i.getClassObj()==ci){require(i instanceof ClassInstance&&found==null,"expected one Derived instance");found=(ClassInstance)i;}require(found!=null,"missing Derived instance");return found;}
  private static ClassInfo required(Map<String,ClassInfo> map,String name){ClassInfo ci=map.get(name);require(ci!=null,"missing "+name);return ci;}
  private static void bind(VM vm,int ref){ThreadInfo ti=vm.getCurrentThread();ClassInfo holder=ClassLoaderInfo.getSystemResolvedClassInfo("HprofLoaderHierarchyRoots");if(!holder.isRegistered())holder.registerClass(ti);boolean pushed=holder.initializeClass(ti);require(!pushed&&holder.isInitialized(),"root holder initialization was not immediate");StaticElementInfo statics=holder.getModifiableStaticElementInfo();statics.setReferenceField("root",ref);require(statics.getReferenceField("root")==ref,"root binding failed");}
  private static void require(boolean value,String message){if(!value)throw new IllegalStateException("[HPROF-JPF] loader hierarchy validation failed: "+message);}
}
