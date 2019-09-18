package com.android.dx;

import com.android.dex.Dex;
import com.android.dex.DexException;
import com.android.dex.util.FileUtils;
import com.android.dx.cf.code.SimException;
import com.android.dx.cf.direct.ClassPathOpener;
import com.android.dx.cf.direct.DirectClassFile;
import com.android.dx.cf.direct.StdAttributeFactory;
import com.android.dx.cf.iface.ParseException;
import com.android.dx.command.DxConsole;
import com.android.dx.command.UsageException;
import com.android.dx.dex.DexOptions;
import com.android.dx.dex.cf.CfOptions;
import com.android.dx.dex.cf.CfTranslator;
import com.android.dx.dex.cf.CodeStatistics;
import com.android.dx.dex.file.ClassDefItem;
import com.android.dx.dex.file.DexFile;
import com.android.dx.dex.file.EncodedMethod;
import com.android.dx.merge.CollisionPolicy;
import com.android.dx.merge.DexMerger;
import com.android.dx.rop.annotation.Annotation;
import com.android.dx.rop.annotation.Annotations;
import com.android.dx.rop.annotation.AnnotationsList;
import com.android.dx.rop.cst.CstString;
import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.IOException;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.io.Writer;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.Map.Entry;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.jar.Attributes;
import java.util.jar.JarEntry;
import java.util.jar.JarOutputStream;
import java.util.jar.Manifest;
import java.util.jar.Attributes.Name;

public class J2DMain {
      private static final String DEX_EXTENSION = ".dex";
      private static final String DEX_PREFIX = "classes";
      private static final String IN_RE_CORE_CLASSES = "Ill-advised or mistaken usage of a core class (java.* or javax.*)\nwhen not building a core library.\n\nThis is often due to inadvertently including a core library file\nin your application's project, when using an IDE (such as\nEclipse). If you are sure you're not intentionally defining a\ncore class, then this is the most likely explanation of what's\ngoing on.\n\nHowever, you might actually be trying to define a class in a core\nnamespace, the source of which you may have taken, for example,\nfrom a non-Android virtual machine project. This will most\nassuredly not work. At a minimum, it jeopardizes the\ncompatibility of your app with future versions of the platform.\nIt is also often of questionable legality.\n\nIf you really intend to build a core library -- which is only\nappropriate as part of creating a full virtual machine\ndistribution, as opposed to compiling an application -- then use\nthe \"--core-library\" option to suppress this error message.\n\nIf you go ahead and use \"--core-library\" but are in fact\nbuilding an application, then be forewarned that your application\nwill still fail to build or run, at some point. Please be\nprepared for angry customers who find, for example, that your\napplication ceases to function once they upgrade their operating\nsystem. You will be to blame for this problem.\n\nIf you are legitimately using some code that happens to be in a\ncore package, then the easiest safe alternative you have is to\nrepackage that code. That is, move the classes in question into\nyour own package namespace. This means that they will never be in\nconflict with core system classes. JarJar is a tool that may help\nyou in this endeavor. If you find that you cannot do this, then\nthat is an indication that the path you are on will ultimately\nlead to pain, suffering, grief, and lamentation.\n";
      private static final String MANIFEST_NAME = "META-INF/MANIFEST.MF";
      private static final Name CREATED_BY = new Name("Created-By");
      private static final String[] JAVAX_CORE = new String[]{"accessibility", "crypto", "imageio", "management", "naming", "net", "print", "rmi", "security", "sip", "sound", "sql", "swing", "transaction", "xml"};
      private static final int MAX_METHOD_ADDED_DURING_DEX_CREATION = 2;
      private static final int MAX_FIELD_ADDED_DURING_DEX_CREATION = 9;
      private static AtomicInteger errors = new AtomicInteger(0);
      private static J2DMain.Arguments args;
      private static DexFile outputDex;
      private static TreeMap outputResources;
      private static final List libraryDexBuffers = new ArrayList();
      private static ExecutorService threadPool;
      private static List parallelProcessorFutures;
      private static volatile boolean anyFilesProcessed;
      private static long minimumFileAge = 0L;
      private static Set classesInJ2DMainDex = null;
      private static List dexOutputArrays = new ArrayList();
      private static OutputStreamWriter humanOutWriter = null;

      public static boolean JarToDex(String istr,String ostr) throws IOException {
            errors.set(0);
            String strargs[] = {"--output="+ostr,istr};
            libraryDexBuffers.clear();
            args = new J2DMain.Arguments();
            args.parse(strargs);
            args.makeOptionsObjects();
            OutputStream humanOutRaw = null;
            if (args.humanOutName != null) {
                  humanOutRaw = openOutput(args.humanOutName);
                  humanOutWriter = new OutputStreamWriter(humanOutRaw);
            }
            try {
                  if (!args.multiDex) {
                        return (runMonoDex()==0);
                  }
                  return (runMultiDex()==0);
            } finally {
                  closeOutput(humanOutRaw);
            }
    }
      public static String getTooManyIdsErrorMessage() {
            return args.multiDex ? "The list of classes given in --J2DMain-dex-list is too big and does not fit in the J2DMain dex." : "You may try using --multi-dex option.";
      }

      private static int runMonoDex() throws IOException {
            File incrementalOutFile = null;
            if (args.incremental) {
                  if (args.outName == null) {
                        System.err.println("error: no incremental output name specified");
                        return -1;
                  }

                  incrementalOutFile = new File(args.outName);
                  if (incrementalOutFile.exists()) {
                        minimumFileAge = incrementalOutFile.lastModified();
                  }
            }

            if (!processAllFiles()) {
                  return 1;
            } else if (args.incremental && !anyFilesProcessed) {
                  return 0;
            } else {
                  byte[] outArray = null;
                  if (!outputDex.isEmpty() || args.humanOutName != null) {
                        outArray = writeDex();
                        if (outArray == null) {
                              return 2;
                        }
                  }

                  if (args.incremental) {
                        outArray = mergeIncremental(outArray, incrementalOutFile);
                  }

                  outArray = mergeLibraryDexBuffers(outArray);
                  if (args.jarOutput) {
                        outputDex = null;
                        if (outArray != null) {
                              outputResources.put("classes.dex", outArray);
                        }

                        if (!createJar(args.outName)) {
                              return 3;
                        }
                  } else if (outArray != null && args.outName != null) {
                        OutputStream out = openOutput(args.outName);
                        out.write(outArray);
                        closeOutput(out);
                  }

                  return 0;
            }
      }

      private static int runMultiDex() throws IOException {
            assert !args.incremental;

            assert args.numThreads == 1;

            if (args.J2DMainDexListFile != null) {
                  classesInJ2DMainDex = readPathsFromFile(args.J2DMainDexListFile);
            }

            if (!processAllFiles()) {
                  return 1;
            } else if (!libraryDexBuffers.isEmpty()) {
                  throw new DexException("Library dex files are not supported in multi-dex mode");
            } else {
                  if (outputDex != null) {
                        dexOutputArrays.add(writeDex());
                        outputDex = null;
                  }

                  if (args.jarOutput) {
                        for(int i = 0; i < dexOutputArrays.size(); ++i) {
                              outputResources.put(getDexFileName(i), dexOutputArrays.get(i));
                        }

                        if (!createJar(args.outName)) {
                              return 3;
                        }
                  } else if (args.outName != null) {
                        File outDir = new File(args.outName);

                        assert outDir.isDirectory();

                        for(int i = 0; i < dexOutputArrays.size(); ++i) {
                              FileOutputStream out = new FileOutputStream(new File(outDir, getDexFileName(i)));

                              try {
                                    out.write((byte[])dexOutputArrays.get(i));
                              } finally {
                                    closeOutput(out);
                              }
                        }
                  }

                  return 0;
            }
      }

      private static String getDexFileName(int i) {
            return i == 0 ? "classes.dex" : "classes" + (i + 1) + ".dex";
      }

      private static Set readPathsFromFile(String fileName) throws IOException {
            Set paths = new HashSet();
            BufferedReader bfr = null;

            try {
                  FileReader fr = new FileReader(fileName);
                  bfr = new BufferedReader(fr);

                  String line;
                  while(null != (line = bfr.readLine())) {
                        paths.add(fixPath(line));
                  }
            } finally {
                  if (bfr != null) {
                        bfr.close();
                  }

            }

            return paths;
      }

      private static byte[] mergeIncremental(byte[] update, File base) throws IOException {
            Dex dexA = null;
            Dex dexB = null;
            if (update != null) {
                  dexA = new Dex(update);
            }

            if (base.exists()) {
                  dexB = new Dex(base);
            }

            if (dexA == null && dexB == null) {
                  return null;
            } else {
                  Dex result;
                  if (dexA == null) {
                        result = dexB;
                  } else if (dexB == null) {
                        result = dexA;
                  } else {
                        result = (new DexMerger(dexA, dexB, CollisionPolicy.KEEP_FIRST)).merge();
                  }

                  ByteArrayOutputStream bytesOut = new ByteArrayOutputStream();
                  result.writeTo((OutputStream)bytesOut);
                  return bytesOut.toByteArray();
            }
      }

      private static byte[] mergeLibraryDexBuffers(byte[] outArray) throws IOException {
            Iterator i$ = libraryDexBuffers.iterator();

            while(i$.hasNext()) {
                  byte[] libraryDex = (byte[])i$.next();
                  if (outArray == null) {
                        outArray = libraryDex;
                  } else {
                        Dex a = new Dex(outArray);
                        Dex b = new Dex(libraryDex);
                        Dex ab = (new DexMerger(a, b, CollisionPolicy.FAIL)).merge();
                        outArray = ab.getBytes();
                  }
            }

            return outArray;
      }

      private static boolean processAllFiles() {
            createDexFile();
            if (args.jarOutput) {
                  outputResources = new TreeMap();
            }

            anyFilesProcessed = false;
            String[] fileNames = args.fileNames;
            if (args.numThreads > 1) {
                  threadPool = Executors.newFixedThreadPool(args.numThreads);
                  parallelProcessorFutures = new ArrayList();
            }

            int errorNum;
            try {
                  if (args.J2DMainDexListFile != null) {
                        ClassPathOpener.FileNameFilter J2DMainPassFilter = args.strictNameCheck ? new J2DMain.J2DMainDexListFilter(null) : new J2DMain.BestEffortJ2DMainDexListFilter();

                        int i;
                        for(i = 0; i < fileNames.length; ++i) {
                              processOne(fileNames[i], (ClassPathOpener.FileNameFilter)J2DMainPassFilter);
                        }

                        if (dexOutputArrays.size() > 0) {
                              throw new DexException("Too many classes in --J2DMain-dex-list, J2DMain dex capacity exceeded");
                        }

                        if (args.minimalJ2DMainDex) {
                              createDexFile();
                        }

                        for(i = 0; i < fileNames.length; ++i) {
                              processOne(fileNames[i], new J2DMain.NotFilter((ClassPathOpener.FileNameFilter)J2DMainPassFilter, null));
                        }
                  } else {
                        for(errorNum = 0; errorNum < fileNames.length; ++errorNum) {
                              processOne(fileNames[errorNum], ClassPathOpener.acceptAll);
                        }
                  }
            } catch (J2DMain.StopProcessing var6) {
                  ;
            }

            if (args.numThreads > 1) {
                  try {
                        threadPool.shutdown();
                        if (!threadPool.awaitTermination(600L, TimeUnit.SECONDS)) {
                              throw new RuntimeException("Timed out waiting for threads.");
                        }
                  } catch (InterruptedException var5) {
                        threadPool.shutdownNow();
                        throw new RuntimeException("A thread has been interrupted.");
                  }

                  try {
                        Iterator i$ = parallelProcessorFutures.iterator();

                        while(i$.hasNext()) {
                              Future future = (Future)i$.next();
                              future.get();
                        }
                  } catch (ExecutionException var3) {
                        Throwable cause = var3.getCause();
                        if (cause instanceof Error) {
                              throw (Error)var3.getCause();
                        }

                        throw new AssertionError(var3.getCause());
                  } catch (InterruptedException var4) {
                        throw new AssertionError(var4);
                  }
            }

            errorNum = errors.get();
            if (errorNum != 0) {
                  DxConsole.err.println(errorNum + " error" + (errorNum == 1 ? "" : "s") + "; aborting");
                  return false;
            } else if (args.incremental && !anyFilesProcessed) {
                  return true;
            } else if (!anyFilesProcessed && !args.emptyOk) {
                  DxConsole.err.println("no classfiles specified");
                  return false;
            } else {
                  if (args.optimize && args.statistics) {
                        CodeStatistics.dumpStatistics(DxConsole.out);
                  }

                  return true;
            }
      }

      private static void createDexFile() {
            if (outputDex != null) {
                  dexOutputArrays.add(writeDex());
            }

            outputDex = new DexFile(args.dexOptions);
            if (args.dumpWidth != 0) {
                  outputDex.setDumpWidth(args.dumpWidth);
            }

      }

      private static void processOne(String pathname, ClassPathOpener.FileNameFilter filter) {
            ClassPathOpener opener = new ClassPathOpener(pathname, false, filter, new ClassPathOpener.Consumer() {
                  public boolean processFileBytes(String name, long lastModified, byte[] bytes) {
                        return J2DMain.processFileBytes(name, lastModified, bytes);
                  }

                  public void onException(Exception ex) {
                        if (ex instanceof J2DMain.StopProcessing) {
                              throw (J2DMain.StopProcessing)ex;
                        } else {
                              if (ex instanceof SimException) {
                                    DxConsole.err.println("\nEXCEPTION FROM SIMULATION:");
                                    DxConsole.err.println(ex.getMessage() + "\n");
                                    DxConsole.err.println(((SimException)ex).getContext());
                              } else {
                                    DxConsole.err.println("\nUNEXPECTED TOP-LEVEL EXCEPTION:");
                                    ex.printStackTrace(DxConsole.err);
                              }

                              J2DMain.errors.incrementAndGet();
                        }
                  }

                  public void onProcessArchiveStart(File file) {
                        if (J2DMain.args.verbose) {
                              DxConsole.out.println("processing archive " + file + "...");
                        }

                  }
            });
            if (args.numThreads > 1) {
                  parallelProcessorFutures.add(threadPool.submit(new J2DMain.ParallelProcessor(opener, null)));
            } else if (opener.process()) {
                  anyFilesProcessed = true;
            }

      }

      private static boolean processFileBytes(String name, long lastModified, byte[] bytes) {
            boolean isClass = name.endsWith(".class");
            boolean isClassesDex = name.equals("classes.dex");
            boolean keepResources = outputResources != null;
            if (!isClass && !isClassesDex && !keepResources) {
                  if (args.verbose) {
                        DxConsole.out.println("ignored resource " + name);
                  }

                  return false;
            } else {
                  if (args.verbose) {
                        DxConsole.out.println("processing " + name + "...");
                  }

                  String fixedName = fixPath(name);
                  TreeMap var8;
                  if (isClass) {
                        if (keepResources && args.keepClassesInJar) {
                              var8 = outputResources;
                              synchronized(outputResources) {
                                    outputResources.put(fixedName, bytes);
                              }
                        }

                        return lastModified < minimumFileAge ? true : processClass(fixedName, bytes);
                  } else if (isClassesDex) {
                        List var15 = libraryDexBuffers;
                        synchronized(libraryDexBuffers) {
                              libraryDexBuffers.add(bytes);
                              return true;
                        }
                  } else {
                        var8 = outputResources;
                        synchronized(outputResources) {
                              outputResources.put(fixedName, bytes);
                              return true;
                        }
                  }
            }
      }

      private static boolean processClass(String name, byte[] bytes) {
            if (!args.coreLibrary) {
                  checkClassName(name);
            }

            DirectClassFile cf = new DirectClassFile(bytes, name, args.cfOptions.strictNameCheck);
            cf.setAttributeFactory(StdAttributeFactory.THE_ONE);
            cf.getMagic();
            int numMethodIds = outputDex.getMethodIds().items().size();
            int numFieldIds = outputDex.getFieldIds().items().size();
            int constantPoolSize = cf.getConstantPool().size();
            int maxMethodIdsInDex = numMethodIds + constantPoolSize + cf.getMethods().size() + 2;
            int maxFieldIdsInDex = numFieldIds + constantPoolSize + cf.getFields().size() + 9;
            if (args.multiDex && outputDex.getClassDefs().items().size() > 0 && (maxMethodIdsInDex > args.maxNumberOfIdxPerDex || maxFieldIdsInDex > args.maxNumberOfIdxPerDex)) {
                  DexFile completeDex = outputDex;
                  createDexFile();

                  assert completeDex.getMethodIds().items().size() <= numMethodIds + 2 && completeDex.getFieldIds().items().size() <= numFieldIds + 9;
            }

            try {
                  ClassDefItem clazz = CfTranslator.translate(cf, bytes, args.cfOptions, args.dexOptions, outputDex);
                  DexFile var9 = outputDex;
                  synchronized(outputDex) {
                        outputDex.add(clazz);
                  }

                  return true;
            } catch (ParseException var12) {
                  DxConsole.err.println("\ntrouble processing:");
                  if (args.debug) {
                        var12.printStackTrace(DxConsole.err);
                  } else {
                        var12.printContext(DxConsole.err);
                  }

                  errors.incrementAndGet();
                  return false;
            }
      }

      private static void checkClassName(String name) {
            boolean bogus = false;
            if (name.startsWith("java/")) {
                  bogus = true;
            } else if (name.startsWith("javax/")) {
                  int slashAt = name.indexOf(47, 6);
                  if (slashAt == -1) {
                        bogus = true;
                  } else {
                        String pkg = name.substring(6, slashAt);
                        bogus = Arrays.binarySearch(JAVAX_CORE, pkg) >= 0;
                  }
            }

            if (bogus) {
                  DxConsole.err.println("\ntrouble processing \"" + name + "\":\n\n" + "Ill-advised or mistaken usage of a core class (java.* or javax.*)\nwhen not building a core library.\n\nThis is often due to inadvertently including a core library file\nin your application's project, when using an IDE (such as\nEclipse). If you are sure you're not intentionally defining a\ncore class, then this is the most likely explanation of what's\ngoing on.\n\nHowever, you might actually be trying to define a class in a core\nnamespace, the source of which you may have taken, for example,\nfrom a non-Android virtual machine project. This will most\nassuredly not work. At a minimum, it jeopardizes the\ncompatibility of your app with future versions of the platform.\nIt is also often of questionable legality.\n\nIf you really intend to build a core library -- which is only\nappropriate as part of creating a full virtual machine\ndistribution, as opposed to compiling an application -- then use\nthe \"--core-library\" option to suppress this error message.\n\nIf you go ahead and use \"--core-library\" but are in fact\nbuilding an application, then be forewarned that your application\nwill still fail to build or run, at some point. Please be\nprepared for angry customers who find, for example, that your\napplication ceases to function once they upgrade their operating\nsystem. You will be to blame for this problem.\n\nIf you are legitimately using some code that happens to be in a\ncore package, then the easiest safe alternative you have is to\nrepackage that code. That is, move the classes in question into\nyour own package namespace. This means that they will never be in\nconflict with core system classes. JarJar is a tool that may help\nyou in this endeavor. If you find that you cannot do this, then\nthat is an indication that the path you are on will ultimately\nlead to pain, suffering, grief, and lamentation.\n");
                  errors.incrementAndGet();
                  throw new J2DMain.StopProcessing(null);
            }
      }

      private static byte[] writeDex() {
            byte[] outArray = null;

            try {
                  try {
                        if (args.methodToDump != null) {
                              outputDex.toDex((Writer)null, false);
                              dumpMethod(outputDex, args.methodToDump, humanOutWriter);
                        } else {
                              outArray = outputDex.toDex(humanOutWriter, args.verboseDump);
                        }

                        if (args.statistics) {
                              DxConsole.out.println(outputDex.getStatistics().toHuman());
                        }
                  } finally {
                        if (humanOutWriter != null) {
                              humanOutWriter.flush();
                        }

                  }

                  return outArray;
            } catch (Exception var5) {
                  if (args.debug) {
                        DxConsole.err.println("\ntrouble writing output:");
                        var5.printStackTrace(DxConsole.err);
                  } else {
                        DxConsole.err.println("\ntrouble writing output: " + var5.getMessage());
                  }

                  return null;
            }
      }

      private static boolean createJar(String fileName) {
            try {
                  Manifest manifest = makeManifest();
                  OutputStream out = openOutput(fileName);
                  JarOutputStream jarOut = new JarOutputStream(out, manifest);

                  try {
                        Iterator i$ = outputResources.entrySet().iterator();

                        while(i$.hasNext()) {
                              Entry e = (Entry)i$.next();
                              String name = (String)e.getKey();
                              byte[] contents = (byte[])e.getValue();
                              JarEntry entry = new JarEntry(name);
                              int length = contents.length;
                              if (args.verbose) {
                                    DxConsole.out.println("writing " + name + "; size " + length + "...");
                              }

                              entry.setSize((long)length);
                              jarOut.putNextEntry(entry);
                              jarOut.write(contents);
                              jarOut.closeEntry();
                        }
                  } finally {
                        jarOut.finish();
                        jarOut.flush();
                        closeOutput(out);
                  }

                  return true;
            } catch (Exception var14) {
                  if (args.debug) {
                        DxConsole.err.println("\ntrouble writing output:");
                        var14.printStackTrace(DxConsole.err);
                  } else {
                        DxConsole.err.println("\ntrouble writing output: " + var14.getMessage());
                  }

                  return false;
            }
      }

      private static Manifest makeManifest() throws IOException {
            byte[] manifestBytes = (byte[])outputResources.get("META-INF/MANIFEST.MF");
            Manifest manifest;
            Attributes attribs;
            if (manifestBytes == null) {
                  manifest = new Manifest();
                  attribs = manifest.getMainAttributes();
                  attribs.put(Name.MANIFEST_VERSION, "1.0");
            } else {
                  manifest = new Manifest(new ByteArrayInputStream(manifestBytes));
                  attribs = manifest.getMainAttributes();
                  outputResources.remove("META-INF/MANIFEST.MF");
            }

            String createdBy = attribs.getValue(CREATED_BY);
            if (createdBy == null) {
                  createdBy = "";
            } else {
                  createdBy = createdBy + " + ";
            }

            createdBy = createdBy + "dx 1.10";
            attribs.put(CREATED_BY, createdBy);
            attribs.putValue("Dex-Location", "classes.dex");
            return manifest;
      }

      private static OutputStream openOutput(String name) throws IOException {
            return (OutputStream)(!name.equals("-") && !name.startsWith("-.") ? new FileOutputStream(name) : System.out);
      }

      private static void closeOutput(OutputStream stream) throws IOException {
            if (stream != null) {
                  stream.flush();
                  if (stream != System.out) {
                        stream.close();
                  }

            }
      }

      private static String fixPath(String path) {
            if (File.separatorChar == '\\') {
                  path = path.replace('\\', '/');
            }

            int index = path.lastIndexOf("/./");
            if (index != -1) {
                  return path.substring(index + 3);
            } else {
                  return path.startsWith("./") ? path.substring(2) : path;
            }
      }

      private static void dumpMethod(DexFile dex, String fqName, OutputStreamWriter out) {
            boolean wildcard = fqName.endsWith("*");
            int lastDot = fqName.lastIndexOf(46);
            if (lastDot > 0 && lastDot != fqName.length() - 1) {
                  String className = fqName.substring(0, lastDot).replace('.', '/');
                  String methodName = fqName.substring(lastDot + 1);
                  ClassDefItem clazz = dex.getClassOrNull(className);
                  if (clazz == null) {
                        DxConsole.err.println("no such class: " + className);
                  } else {
                        if (wildcard) {
                              methodName = methodName.substring(0, methodName.length() - 1);
                        }

                        ArrayList allMeths = clazz.getMethods();
                        TreeMap meths = new TreeMap();
                        Iterator i$ = allMeths.iterator();

                        while(true) {
                              EncodedMethod meth;
                              String methName;
                              do {
                                    if (!i$.hasNext()) {
                                          if (meths.size() == 0) {
                                                DxConsole.err.println("no such method: " + fqName);
                                                return;
                                          }

                                          PrintWriter pw = new PrintWriter(out);
                                          Iterator i22 = meths.values().iterator();

                                          while(true) {
                                                AnnotationsList parameterAnnotations;
                                                do {
                                                      if (!i22.hasNext()) {
                                                            pw.flush();
                                                            return;
                                                      }

                                                      EncodedMethod meth1 = (EncodedMethod)i$.next();
                                                      meth1.debugPrint(pw, args.verboseDump);
                                                      CstString sourceFile = clazz.getSourceFile();
                                                      if (sourceFile != null) {
                                                            pw.println("  source file: " + sourceFile.toQuoted());
                                                      }

                                                      Annotations methodAnnotations = clazz.getMethodAnnotations(meth1.getRef());
                                                      parameterAnnotations = clazz.getParameterAnnotations(meth1.getRef());
                                                      if (methodAnnotations != null) {
                                                            pw.println("  method annotations:");
                                                            Iterator i33 = methodAnnotations.getAnnotations().iterator();

                                                            while(i33.hasNext()) {
                                                                  Annotation a = (Annotation)i$.next();
                                                                  pw.println("    " + a);
                                                            }
                                                      }
                                                } while(parameterAnnotations == null);

                                                pw.println("  parameter annotations:");
                                                int sz = parameterAnnotations.size();

                                                for(int i = 0; i < sz; ++i) {
                                                      pw.println("    parameter " + i);
                                                      Annotations annotations = parameterAnnotations.get(i);
                                                      Iterator i44 = annotations.getAnnotations().iterator();

                                                      while(i44.hasNext()) {
                                                            Annotation a = (Annotation)i$.next();
                                                            pw.println("      " + a);
                                                      }
                                                }
                                          }
                                    }

                                    meth = (EncodedMethod)i$.next();
                                    methName = meth.getName().getString();
                              } while((!wildcard || !methName.startsWith(methodName)) && (wildcard || !methName.equals(methodName)));

                              meths.put(meth.getRef().getNat(), meth);
                        }
                  }
            } else {
                  DxConsole.err.println("bogus fully-qualified method name: " + fqName);
            }
      }

      private static class ParallelProcessor implements Callable {
            ClassPathOpener classPathOpener;

            private ParallelProcessor(ClassPathOpener classPathOpener) {
                  this.classPathOpener = classPathOpener;
            }

            public Void call() throws Exception {
                  if (this.classPathOpener.process()) {
                        J2DMain.anyFilesProcessed = true;
                  }

                  return null;
            }

            // $FF: synthetic method
            ParallelProcessor(ClassPathOpener x0, Object x1) {
                  this(x0);
            }
      }

      public static class Arguments {
            private static final String MINIMAL_J2DMain_DEX_OPTION = "--minimal-J2DMain-dex";
            private static final String J2DMain_DEX_LIST_OPTION = "--J2DMain-dex-list";
            private static final String MULTI_DEX_OPTION = "--multi-dex";
            private static final String NUM_THREADS_OPTION = "--num-threads";
            private static final String INCREMENTAL_OPTION = "--incremental";
            private static final String INPUT_LIST_OPTION = "--input-list";
            public boolean debug = false;
            public boolean verbose = false;
            public boolean verboseDump = false;
            public boolean coreLibrary = false;
            public String methodToDump = null;
            public int dumpWidth = 0;
            public String outName = null;
            public String humanOutName = null;
            public boolean strictNameCheck = true;
            public boolean emptyOk = false;
            public boolean jarOutput = false;
            public boolean keepClassesInJar = false;
            public int positionInfo = 2;
            public boolean localInfo = true;
            public boolean incremental = false;
            public boolean forceJumbo = false;
            public String[] fileNames;
            public boolean optimize = true;
            public String optimizeListFile = null;
            public String dontOptimizeListFile = null;
            public boolean statistics;
            public CfOptions cfOptions;
            public DexOptions dexOptions;
            public int numThreads = 1;
            public boolean multiDex = false;
            public String J2DMainDexListFile = null;
            public boolean minimalJ2DMainDex = false;
            private Set inputList = null;
            private int maxNumberOfIdxPerDex = 65536;

            public void parse(String[] args) {
                  J2DMain.Arguments.ArgumentsParser parser = new J2DMain.Arguments.ArgumentsParser(args);
                  boolean outputIsDirectory = false;
                  boolean outputIsDirectDex = false;

                  while(parser.getNext()) {
                        if (parser.isArg("--debug")) {
                              this.debug = true;
                        } else if (parser.isArg("--verbose")) {
                              this.verbose = true;
                        } else if (parser.isArg("--verbose-dump")) {
                              this.verboseDump = true;
                        } else if (parser.isArg("--no-files")) {
                              this.emptyOk = true;
                        } else if (parser.isArg("--no-optimize")) {
                              this.optimize = false;
                        } else if (parser.isArg("--no-strict")) {
                              this.strictNameCheck = false;
                        } else if (parser.isArg("--core-library")) {
                              this.coreLibrary = true;
                        } else if (parser.isArg("--statistics")) {
                              this.statistics = true;
                        } else if (parser.isArg("--optimize-list=")) {
                              if (this.dontOptimizeListFile != null) {
                                    System.err.println("--optimize-list and --no-optimize-list are incompatible.");
                                    throw new UsageException();
                              }

                              this.optimize = true;
                              this.optimizeListFile = parser.getLastValue();
                        } else if (parser.isArg("--no-optimize-list=")) {
                              if (this.dontOptimizeListFile != null) {
                                    System.err.println("--optimize-list and --no-optimize-list are incompatible.");
                                    throw new UsageException();
                              }

                              this.optimize = true;
                              this.dontOptimizeListFile = parser.getLastValue();
                        } else if (parser.isArg("--keep-classes")) {
                              this.keepClassesInJar = true;
                        } else if (parser.isArg("--output=")) {
                              this.outName = parser.getLastValue();
                              if ((new File(this.outName)).isDirectory()) {
                                    this.jarOutput = false;
                                    outputIsDirectory = true;
                              } else if (FileUtils.hasArchiveSuffix(this.outName)) {
                                    this.jarOutput = true;
                              } else {
                                    if (!this.outName.endsWith(".dex") && !this.outName.equals("-")) {
                                          System.err.println("unknown output extension: " + this.outName);
                                          throw new UsageException();
                                    }

                                    this.jarOutput = false;
                                    outputIsDirectDex = true;
                              }
                        } else if (parser.isArg("--dump-to=")) {
                              this.humanOutName = parser.getLastValue();
                        } else if (parser.isArg("--dump-width=")) {
                              this.dumpWidth = Integer.parseInt(parser.getLastValue());
                        } else if (parser.isArg("--dump-method=")) {
                              this.methodToDump = parser.getLastValue();
                              this.jarOutput = false;
                        } else if (parser.isArg("--positions=")) {
                              String pstr = parser.getLastValue().intern();
                              if (pstr == "none") {
                                    this.positionInfo = 1;
                              } else if (pstr == "important") {
                                    this.positionInfo = 3;
                              } else {
                                    if (pstr != "lines") {
                                          System.err.println("unknown positions option: " + pstr);
                                          throw new UsageException();
                                    }

                                    this.positionInfo = 2;
                              }
                        } else if (parser.isArg("--no-locals")) {
                              this.localInfo = false;
                        } else if (parser.isArg("--num-threads=")) {
                              this.numThreads = Integer.parseInt(parser.getLastValue());
                        } else if (parser.isArg("--incremental")) {
                              this.incremental = true;
                        } else if (parser.isArg("--force-jumbo")) {
                              this.forceJumbo = true;
                        } else if (parser.isArg("--multi-dex")) {
                              this.multiDex = true;
                        } else if (parser.isArg("--J2DMain-dex-list=")) {
                              this.J2DMainDexListFile = parser.getLastValue();
                        } else if (parser.isArg("--minimal-J2DMain-dex")) {
                              this.minimalJ2DMainDex = true;
                        } else if (parser.isArg("--set-max-idx-number=")) {
                              this.maxNumberOfIdxPerDex = Integer.parseInt(parser.getLastValue());
                        } else {
                              if (!parser.isArg("--input-list=")) {
                                    System.err.println("unknown option: " + parser.getCurrent());
                                    throw new UsageException();
                              }

                              File inputListFile = new File(parser.getLastValue());

                              try {
                                    this.inputList = J2DMain.readPathsFromFile(inputListFile.getAbsolutePath());
                              } catch (IOException var7) {
                                    System.err.println("Unable to read input list file: " + inputListFile.getName());
                                    throw new UsageException();
                              }
                        }
                  }

                  this.fileNames = parser.getReJ2DMaining();
                  if (this.inputList != null && !this.inputList.isEmpty()) {
                        this.inputList.addAll(Arrays.asList(this.fileNames));
                        this.fileNames = (String[])this.inputList.toArray(new String[this.inputList.size()]);
                  }

                  if (this.fileNames.length == 0) {
                        if (!this.emptyOk) {
                              System.err.println("no input files specified");
                              throw new UsageException();
                        }
                  } else if (this.emptyOk) {
                        System.out.println("ignoring input files");
                  }

                  if (this.humanOutName == null && this.methodToDump != null) {
                        this.humanOutName = "-";
                  }

                  if (this.J2DMainDexListFile != null && !this.multiDex) {
                        System.err.println("--J2DMain-dex-list is only supported in combination with --multi-dex");
                        throw new UsageException();
                  } else if (this.minimalJ2DMainDex && (this.J2DMainDexListFile == null || !this.multiDex)) {
                        System.err.println("--minimal-J2DMain-dex is only supported in combination with --multi-dex and --J2DMain-dex-list");
                        throw new UsageException();
                  } else {
                        if (this.multiDex && this.numThreads != 1) {
                              System.out.println("--num-threads is ignored when used with --multi-dex");
                              this.numThreads = 1;
                        }

                        if (this.multiDex && this.incremental) {
                              System.err.println("--incremental is not supported with --multi-dex");
                              throw new UsageException();
                        } else if (this.multiDex && outputIsDirectDex) {
                              System.err.println("Unsupported output \"" + this.outName + "\". " + "--multi-dex" + " supports only archive or directory output");
                              throw new UsageException();
                        } else {
                              if (outputIsDirectory && !this.multiDex) {
                                    this.outName = (new File(this.outName, "classes.dex")).getPath();
                              }

                              this.makeOptionsObjects();
                        }
                  }
            }

            private void makeOptionsObjects() {
                  this.cfOptions = new CfOptions();
                  this.cfOptions.positionInfo = this.positionInfo;
                  this.cfOptions.localInfo = this.localInfo;
                  this.cfOptions.strictNameCheck = this.strictNameCheck;
                  this.cfOptions.optimize = this.optimize;
                  this.cfOptions.optimizeListFile = this.optimizeListFile;
                  this.cfOptions.dontOptimizeListFile = this.dontOptimizeListFile;
                  this.cfOptions.statistics = this.statistics;
                  this.cfOptions.warn = DxConsole.err;
                  this.dexOptions = new DexOptions();
                  this.dexOptions.forceJumbo = this.forceJumbo;
            }

            private class ArgumentsParser {
                  private final String[] arguments;
                  private int index;
                  private String current;
                  private String lastValue;

                  public ArgumentsParser(String[] arguments) {
                        this.arguments = arguments;
                        this.index = 0;
                  }

                  public String getCurrent() {
                        return this.current;
                  }

                  public String getLastValue() {
                        return this.lastValue;
                  }

                  public boolean getNext() {
                        if (this.index >= this.arguments.length) {
                              return false;
                        } else {
                              this.current = this.arguments[this.index];
                              if (!this.current.equals("--") && this.current.startsWith("--")) {
                                    ++this.index;
                                    return true;
                              } else {
                                    return false;
                              }
                        }
                  }

                  private boolean getNextValue() {
                        if (this.index >= this.arguments.length) {
                              return false;
                        } else {
                              this.current = this.arguments[this.index];
                              ++this.index;
                              return true;
                        }
                  }

                  public String[] getReJ2DMaining() {
                        int n = this.arguments.length - this.index;
                        String[] reJ2DMaining = new String[n];
                        if (n > 0) {
                              System.arraycopy(this.arguments, this.index, reJ2DMaining, 0, n);
                        }

                        return reJ2DMaining;
                  }

                  public boolean isArg(String prefix) {
                        int n = prefix.length();
                        if (n > 0 && prefix.charAt(n - 1) == '=') {
                              if (this.current.startsWith(prefix)) {
                                    this.lastValue = this.current.substring(n);
                                    return true;
                              } else {
                                    prefix = prefix.substring(0, n - 1);
                                    if (this.current.equals(prefix)) {
                                          if (this.getNextValue()) {
                                                this.lastValue = this.current;
                                                return true;
                                          } else {
                                                System.err.println("Missing value after parameter " + prefix);
                                                throw new UsageException();
                                          }
                                    } else {
                                          return false;
                                    }
                              }
                        } else {
                              return this.current.equals(prefix);
                        }
                  }
            }
      }

      private static class StopProcessing extends RuntimeException {
            private StopProcessing() {
            }

            // $FF: synthetic method
            StopProcessing(Object x0) {
                  this();
            }
      }

      private static class BestEffortJ2DMainDexListFilter implements ClassPathOpener.FileNameFilter {
            Map map = new HashMap();

            public BestEffortJ2DMainDexListFilter() {
                  String normalized;
                  Object fullPath;
                  for(Iterator i$ = J2DMain.classesInJ2DMainDex.iterator(); i$.hasNext(); ((List)fullPath).add(normalized)) {
                        String pathOfClass = (String)i$.next();
                        normalized = J2DMain.fixPath(pathOfClass);
                        String simple = getSimpleName(normalized);
                        fullPath = (List)this.map.get(simple);
                        if (fullPath == null) {
                              fullPath = new ArrayList(1);
                              this.map.put(simple, fullPath);
                        }
                  }

            }

            public boolean accept(String path) {
                  if (!path.endsWith(".class")) {
                        return true;
                  } else {
                        String normalized = J2DMain.fixPath(path);
                        String simple = getSimpleName(normalized);
                        List fullPaths = (List)this.map.get(simple);
                        if (fullPaths != null) {
                              Iterator i$ = fullPaths.iterator();

                              while(i$.hasNext()) {
                                    String fullPath = (String)i$.next();
                                    if (normalized.endsWith(fullPath)) {
                                          return true;
                                    }
                              }
                        }

                        return false;
                  }
            }

            private static String getSimpleName(String path) {
                  int index = path.lastIndexOf(47);
                  return index >= 0 ? path.substring(index + 1) : path;
            }
      }

      private static class J2DMainDexListFilter implements ClassPathOpener.FileNameFilter {
            private J2DMainDexListFilter() {
            }

            public boolean accept(String fullPath) {
                  if (fullPath.endsWith(".class")) {
                        String path = J2DMain.fixPath(fullPath);
                        return J2DMain.classesInJ2DMainDex.contains(path);
                  } else {
                        return true;
                  }
            }

            // $FF: synthetic method
            J2DMainDexListFilter(Object x0) {
                  this();
            }
      }

      private static class NotFilter implements ClassPathOpener.FileNameFilter {
            private final ClassPathOpener.FileNameFilter filter;

            private NotFilter(ClassPathOpener.FileNameFilter filter) {
                  this.filter = filter;
            }

            public boolean accept(String path) {
                  return !this.filter.accept(path);
            }

            // $FF: synthetic method
            NotFilter(ClassPathOpener.FileNameFilter x0, Object x1) {
                  this(x0);
            }
      }
}
