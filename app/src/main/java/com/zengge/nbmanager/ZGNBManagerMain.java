/*
 * Copyright (C) 2009 Google Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not
 * use this file except in compliance with the License. You may obtain a copy of
 * the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations under
 * the License.
 */

package com.zengge.nbmanager;

import android.Manifest;
import android.annotation.SuppressLint;
import android.app.AlertDialog;
import android.app.Dialog;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.pm.PackageInfo;
import android.content.pm.ApplicationInfo;
import android.database.DataSetObserver;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.os.Message;
import android.support.v4.content.FileProvider;
import android.support.v7.app.AppCompatActivity;
import android.view.ContextMenu;
import android.view.ContextMenu.ContextMenuInfo;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.BaseAdapter;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.RelativeLayout;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;
import android.telephony.TelephonyManager;
import android.util.*;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;

import java.lang.ref.*;
import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.IOException;
import java.net.URLConnection;
import java.text.DecimalFormat;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Enumeration;
import java.util.List;
import java.util.UUID;
import java.util.Vector;
import java.util.Stack;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.zip.*;

import jd.commonide.IdeDecompiler;
import jd.commonide.preferences.IdePreferences;
import android.os.Build;
import android.provider.Settings;

import org.jf.baksmali.BakSmaliFunc;
import org.jf.dexlib.*;

import com.android.dx.J2DMain;
import com.googlecode.dex2jar.v3.ConverterMain;
import com.zengge.ap.AudioPlayer;
import com.zengge.elfeditor.ElfActivity;
import com.zengge.imageviewer.HugeImageViewerActivity;
import com.zengge.util.*;
import com.zengge.vp.VideoPlayerActivity;
import com.zengge.arsceditor.ArscActivity;

import org.jf.smali.main;

import android.support.v4.content.ContextCompat;
import android.support.v4.app.ActivityCompat;

public class ZGNBManagerMain extends AppCompatActivity {

    private final static String EMPTY = "";
    public final static String SELECTED = "ZGNBManagerMain";
    public final static String ENTRYPATH = "ZipEntry";
    public final static String POS = "file_position";
    public final static String SELECTEDMOD = "selected_mod";
    public final static String CURRENTFILE = "current_file";
    public final static String CURRENTDIR = "current_dir";
    public final static String CLIPBOARD = "file_clipboard";
    public final static String TAG = "ZGNBManagerMain";

    private Stack<Integer> pos = new Stack<Integer>();

    private List<File> mFileList;
    private FileListAdapter mAdapter;
    private boolean mSelectMod = false;
    private String mQuery = EMPTY;
    private File mCurrentDir;
    private File mCurrent;
    public ListView lv;

    public int position;

    private static boolean mCut;
    private static File mClipboard;
    private Dialog mPermissionDialog;
    private Handler mHandler = new Handler() {
        @Override
        public void handleMessage(Message msg) {
            switch(msg.what) {
            case SHOWPROGRESS:
                ZGNBManagerMain.this.showDialog(0);
                break;
            case DISMISSPROGRESS:
                mAdapter.notifyDataSetInvalidated();
                ZGNBManagerMain.this.dismissDialog(0);
                break;
            case TOAST:
                toast(msg.obj.toString());
                break;
            case SHOWMESSAGE:
                showMessage(ZGNBManagerMain.this, "", msg.obj.toString());
                break;
            }
        }
    };

    Comparator<File> sortByType = new Comparator<File>() {
        public int compare(File file1, File file2) {
            boolean a = file1.isDirectory();
            boolean b = file2.isDirectory();
            if(a && !b)
                return -1;
            else if(!a && b)
                return 1;
            else if(a && b)
                return file1.getName().toLowerCase().compareTo(file2.getName().toLowerCase());
            else
                return file1.getName().compareTo(file2.getName());
        }
    };

    public static final int SHOWPROGRESS = 1;
    public static final int DISMISSPROGRESS = 2;
    public static final int TOAST = 3;
    public static final int ERROR = 4;
    public static final int SHOWMESSAGE = 5;
    // Linux stat constants
    public static final int S_IFMT = 0170000; /* type of file */
    public static final int S_IFLNK = 0120000; /* symbolic link */
    public static final int S_IFREG = 0100000; /* regular */
    public static final int S_IFBLK = 0060000; /* block special */
    public static final int S_IFDIR = 0040000; /* directory */
    public static final int S_IFCHR = 0020000; /* character special */
    public static final int S_IFIFO = 0010000; /* this is a FIFO */
    public static final int S_ISUID = 0004000; /* set user id on execution */
    public static final int S_ISGID = 0002000; /* set group id on execution */

    public boolean initialized = false;

    public void CreateInit() {
        lv = findViewById(R.id.zglist);
        handleIntent(getIntent());
        if(mCurrentDir == null) {
            if(Environment.getExternalStorageState().equals(Environment.MEDIA_MOUNTED))
                mCurrentDir = Environment.getExternalStorageDirectory();
            else
                mCurrentDir = Environment.getRootDirectory();
        }
        mAdapter = new FileListAdapter(getApplication());
        mAdapter.registerDataSetObserver(new DataSetObserver() {
            @Override
            public void onInvalidated() {
                updateAndFilterFileList(EMPTY);
            }
        });
        registerForContextMenu(lv);
        updateAndFilterFileList(mQuery);
        lv.setAdapter(mAdapter);
        if(mPermissionDialog == null) {
            mPermissionDialog = new Dialog(this);
            mPermissionDialog.setContentView(R.layout.permissions);
            mPermissionDialog.findViewById(R.id.btnOk).setOnClickListener(new View.OnClickListener() {
                public void onClick(View v) {
                    setPermissions();
                }
            });
            mPermissionDialog.findViewById(R.id.btnCancel).setOnClickListener(new View.OnClickListener() {
                public void onClick(View v) {
                    mPermissionDialog.hide();
                }
            });
        }
        lv.setSelection(position);
        lv.setOnItemClickListener(new ListView.OnItemClickListener() {
            @Override
            public void onItemClick(final AdapterView<?> parent, View view, int position, long id) {
                final File file = (File) parent.getItemAtPosition(position);
                ZGNBManagerMain.this.position = position;
                String name = file.getName();
                mCurrent = file;
                if(file.isDirectory()) {
                    if(file.toString().endsWith("_baksmali")) {
                        isPreparedToBuildSmali = true;
                        AlertDialog.Builder builder = new AlertDialog.Builder(ZGNBManagerMain.this);
                        builder.setTitle(getString(R.string.tips));
                        builder.setMessage(getString(R.string.smali_instruction));
                        builder.setPositiveButton(getString(R.string.build_smali), new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface arg0, int arg1) {
                                // TODO Auto-generated method stub
                                buildSmali(file);
                                isPreparedToBuildSmali = false;
                            }
                        });
                        builder.setNeutralButton(getString(R.string.explore_dir), new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface arg0, int arg1) {
                                mCurrentDir = file;
                                pos.push(parent.getFirstVisiblePosition());
                                mAdapter.notifyDataSetInvalidated();
                                isPreparedToBuildSmali = false;
                            }
                        });
                        builder.show();
                    } else {
                        mCurrentDir = file;
                        pos.push(parent.getFirstVisiblePosition());
                        mAdapter.notifyDataSetInvalidated();
                        return;
                    }
                }
                if(mSelectMod) {
                    mSelectMod = false;
                    resultFileToZipEditor(file);
                    return;
                }
                if(isZip(file))
                    openApk(file);
                else if(name.toLowerCase().endsWith(".mp4") || name.toLowerCase().endsWith(".3gp")) {
                    Intent intent = new Intent(ZGNBManagerMain.this, VideoPlayerActivity.class);
                    intent.setData(Uri.parse(file.toString()));
                    startActivity(intent);
                } else if(name.toLowerCase().endsWith(".mp3") || name.toLowerCase().endsWith(".aac")
                          || name.toLowerCase().endsWith(".ogg") || name.toLowerCase().endsWith(".wma")
                          || name.toLowerCase().endsWith(".wav") || name.toLowerCase().endsWith(".amr")) {
                    Intent intent = new Intent(ZGNBManagerMain.this, AudioPlayer.class);
                    intent.putExtra("AUDIOPATH", file.toString());
                    startActivity(intent);
                } else if(name.toLowerCase().endsWith(".jpg") || name.toLowerCase().endsWith(".png")
                          || name.toLowerCase().endsWith(".bmp")) {
                    Intent it = new Intent(ZGNBManagerMain.this, HugeImageViewerActivity.class);
                    it.putExtra("IMAGEPATH", file.toString());
                    startActivity(it);
                } else if(name.toLowerCase().endsWith(".rar"))
                    ExtractRar(file);
                else if(name.toLowerCase().endsWith(".odex"))
                    ConOdex(file);
                else if(name.toLowerCase().endsWith(".oat"))
                    OatToDex(file);
                else if(name.toLowerCase().endsWith(".so")) {
                    if(!Features.isValidElf(file.toString())) {
                        Toast.makeText(ZGNBManagerMain.this, getString(R.string.invalid_elf), Toast.LENGTH_LONG).show();
                        return;
                    }
                    PELF(file);
                } else if(name.toLowerCase().endsWith(".arsc"))
                    editArsc(file);
                else if(name.toLowerCase().endsWith(".xml"))
                    editAxml(file);
                else if(name.toLowerCase().endsWith(".txt") || name.toLowerCase().endsWith(".c")
                        || name.toLowerCase().endsWith(".cpp") || name.toLowerCase().endsWith(".java")
                        || name.toLowerCase().endsWith(".py") || name.toLowerCase().endsWith(".h")
                        || name.toLowerCase().endsWith(".hpp") || name.toLowerCase().endsWith(".cs")
                        || name.toLowerCase().endsWith(".smali"))
                    editText(file);
                else if(name.toLowerCase().endsWith(".dex"))
                    openDexFile(file);
                else {
                    if(!isPreparedToBuildSmali)
                        dialogMenu();
                }
            }
        });
        initialized = true;
    }


    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        switch(requestCode) {
        // requestCode即所声明的权限获取码，在checkSelfPermission时传入
        case RQ_PERMISSION:
            if(grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                // 获取到权限，作相应处理（调用定位SDK应当确保相关权限均被授权，否则可能引起定位失败）
                CreateInit();
            } else {
                // 没有获取到权限，做特殊处理
                android.support.v7.app.AlertDialog.Builder builder = new android.support.v7.app.AlertDialog.Builder(ZGNBManagerMain.this);
                builder.setTitle("提示");
                builder.setMessage("必须获取权限才能运行！");
                builder.setPositiveButton("确定", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        android.os.Process.killProcess(android.os.Process.myPid());
                    }
                });
                builder.show();
            }
            break;
        default:
            break;
        }
    }

    public static final int RQ_PERMISSION = 100;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.listact);
        if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            if(ContextCompat.checkSelfPermission(ZGNBManagerMain.this, Manifest.permission.WRITE_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED
                    || ContextCompat.checkSelfPermission(ZGNBManagerMain.this, Manifest.permission.READ_PHONE_STATE) != PackageManager.PERMISSION_GRANTED) {
                //没有权限则申请权限
                ActivityCompat.requestPermissions(ZGNBManagerMain.this, new String[] {Manifest.permission.WRITE_EXTERNAL_STORAGE, Manifest.permission.READ_PHONE_STATE}, RQ_PERMISSION);
            } else CreateInit();
        } else CreateInit();
    }

    public static String getPkgSign(Context ctx) {
        try {
            PackageManager pm = ctx.getPackageManager();
            PackageInfo pi = pm.getPackageInfo(ctx.getPackageName(), PackageManager.GET_SIGNATURES);
            return new String(pi.signatures[0].toChars());
        } catch(Exception e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
            return "NULL";
        }
    }

    private void updateAndFilterFileList(final String query) {
        File[] files = mCurrentDir.listFiles();
        if(files != null) {
            setTitle(mCurrentDir.getPath());
            List<File> work = new Vector<File>(files.length);
            for(File file : files) {
                if(query == null || query.equals(EMPTY))
                    work.add(file);
                else if(file.getName().toLowerCase().contains(query.toLowerCase()))
                    work.add(file);
            }
            Collections.sort(work, sortByType);
            mFileList = work;
            File parent = mCurrentDir.getParentFile();
            if(parent != null) {
                mFileList.add(0, new File(mCurrentDir.getParent()) {
                    @Override
                    public boolean isDirectory() {
                        return true;
                    }
                    @Override
                    public String getName() {
                        return "..";
                    }
                });
            }
        }
    }

    private void handleIntent(Intent intent) {
        mSelectMod = intent.getBooleanExtra(SELECTEDMOD, false);
    }

    private void resultFileToZipEditor(File file) {
        Intent intent = getIntent();
        intent.putExtra(ENTRYPATH, file.getAbsolutePath());
        setResult(ActResConstant.add_entry, intent);
        finish();
    }

    public static boolean writeSrc(File file, String content) {
        FileOutputStream fop = null;
        File par = new File(file.getParent());
        if(!par.exists())
            par.mkdirs();
        try {
            fop = new FileOutputStream(file);
            if(!file.exists())
                file.createNewFile();
            byte[] contentInBytes = content.getBytes();
            fop.write(contentInBytes);
            fop.flush();
            fop.close();
            return true;
        } catch(IOException e) {
            e.printStackTrace();
            return false;
        } finally {
            try {
                if(fop != null)
                    fop.close();
            } catch(IOException e) {
                e.printStackTrace();
            }
        }
    }

    private static boolean DecompileJAR(String zip) {
        String decompilestr;
        int i = 1;
        String debugfile = Environment.getExternalStorageDirectory() + File.separator + "ZGNBManager" + File.separator
                           + "debug-" + System.currentTimeMillis() + ".txt";
        if(!new File(new File(debugfile).getParent()).exists())
            new File(new File(debugfile).getParent()).mkdirs();
        Features.printLog(debugfile, "JAR Decompiler Debugging Info!", true);
        Features.printLog(debugfile, "Processing JAR File : ", true);
        try {
            ZipFile zipFile = new ZipFile(zip);
            Enumeration<ZipEntry> enu = (Enumeration<ZipEntry>) zipFile.entries();
            while(enu.hasMoreElements()) {
                ZipEntry zipElement = (ZipEntry) enu.nextElement();
                zipFile.getInputStream(zipElement);
                String fileName = zipElement.getName();
                if(!fileName.endsWith(".class"))
                    continue;
                if(fileName.contains("$"))
                    continue;
                Features.printLog(debugfile, "(" + i + "): " + "Processing class file : " + fileName, true);
                i++;
                IdePreferences ip = new IdePreferences(false, false, true, true, false, false, false);
                decompilestr = IdeDecompiler.decompile(ip, zip, fileName);
                String destdir = zip.substring(0, zip.length() - 4) + "_src";
                String dest = destdir + File.separator + fileName.substring(0, fileName.length() - 6) + ".java";
                Features.printLog(debugfile, "Destination file : " + dest, true);
                if(decompilestr == null) {
                    Features.printLog(debugfile, "Decompiled String is NULL,skipping decompiling it!", true);
                    Features.printLog(debugfile, "Exceptions are : " + "\n" + IdeDecompiler.errstr, true);
                    continue;
                }
                if(!writeSrc(new File(dest), decompilestr))
                    return false;
            }
        } catch(Exception e) {
            e.printStackTrace();
            return false;
        }
        return true;
    }

    private static boolean isStandardJAR(String zip) {
        boolean value = false;
        try {
            ZipFile zipFile = new ZipFile(zip);
            Enumeration<ZipEntry> enu = (Enumeration<ZipEntry>) zipFile.entries();
            while(enu.hasMoreElements()) {
                ZipEntry zipElement = (ZipEntry) enu.nextElement();
                zipFile.getInputStream(zipElement);
                String fileName = zipElement.getName();
                if(fileName.endsWith(".class")) {
                    value = true;
                    break;
                }
            }
        } catch(Exception e) {
            e.printStackTrace();
        }
        return value;
    }

    private void openApk(final File file) {
        if(file.toString().endsWith(".jar") && isStandardJAR(file.toString())) {
            AlertDialog.Builder builder = new AlertDialog.Builder(this);
            builder.setTitle(getString(R.string.tips));
            builder.setMessage(getString(R.string.jar_instruction));
            builder.setPositiveButton(getString(R.string.todex), new DialogInterface.OnClickListener() {
                public void onClick(DialogInterface dialog, int whichButton) {
                    new Thread(new Runnable() {
                        public void run() {
                            mHandler.sendEmptyMessage(SHOWPROGRESS);
                            boolean JAR2DEX_SUC = J2DMain.JarToDex(file.toString(),
                                                                   file.toString().substring(0, file.toString().length() - 4) + "_converted.dex");
                            if(!JAR2DEX_SUC)
                                showToast(getString(R.string.jar2dex_success));
                            else
                                showToast(getString(R.string.jar2dex_fail));
                            mHandler.sendEmptyMessage(DISMISSPROGRESS);
                        }
                    }).start();
                }
            });
            builder.setNegativeButton(getString(R.string.decompile_jar), new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialog, int which) {
                    new Thread(new Runnable() {
                        @Override
                        public void run() {
                            mHandler.sendEmptyMessage(SHOWPROGRESS);
                            boolean dsuc = DecompileJAR(file.toString());
                            if(dsuc)
                                showToast(getString(R.string.djar_success));
                            else
                                showToast(getString(R.string.djar_fail));
                            mHandler.sendEmptyMessage(DISMISSPROGRESS);
                        }
                    }).start();
                }
            });
            builder.setNeutralButton(getString(R.string.explore_jar), new DialogInterface.OnClickListener() {
                public void onClick(DialogInterface dialog, int whichButton) {
                    new Thread(new Runnable() {
                        public void run() {
                            Intent intent = new Intent(ZGNBManagerMain.this, ZipManagerMain.class);
                            ZipManagerMain.zipFileName = file.getAbsolutePath();
                            startActivityForResult(intent, ActResConstant.list_item_details);
                        }
                    }).start();
                }
            });
            builder.show();
        } else if(file.toString().endsWith(".apk")) {
            AlertDialog.Builder builder = new AlertDialog.Builder(this);
            builder.setTitle(getString(R.string.tips));
            builder.setMessage(getString(R.string.apk_instruction));
            builder.setPositiveButton(getString(R.string.open_apk), new DialogInterface.OnClickListener() {
                public void onClick(DialogInterface dialog, int whichButton) {
                    Intent intent = new Intent(ZGNBManagerMain.this, ZipManagerMain.class);
                    ZipManagerMain.zipFileName = file.getAbsolutePath();
                    startActivityForResult(intent, ActResConstant.list_item_details);
                }
            });
            builder.setNegativeButton(getString(R.string.decompile_javainapk), new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialog, int which) {
                    DecompileFileUtil.openAppIntent(ZGNBManagerMain.this,file.toString());
                }
            });
            builder.show();
        } else {
            Intent intent = new Intent(this, ZipManagerMain.class);
            ZipManagerMain.zipFileName = file.getAbsolutePath();
            startActivityForResult(intent, ActResConstant.list_item_details);
        }
    }

    private void editArsc(final File file) {
        new Thread(new Runnable() {
            public void run() {
                mHandler.sendEmptyMessage(SHOWPROGRESS);
                try {
                    Intent it = new Intent(ZGNBManagerMain.this, ArscActivity.class);
                    it.putExtra("FilePath", file.toString());
                    startActivityForResult(it, ActResConstant.list_item_details);
                } catch(Exception e) {
                    Message msg = new Message();
                    msg.what = SHOWMESSAGE;
                    msg.obj = "Open Arsc exception " + e.getMessage();
                    mHandler.sendMessage(msg);
                }
                mHandler.sendEmptyMessage(DISMISSPROGRESS);
            }
        }).start();
    }

    private void editText(final File file) {
        new Thread(new Runnable() {
            public void run() {
                mHandler.sendEmptyMessage(SHOWPROGRESS);
                try {
                    TextEditor.data = FileUtil.readFile(file);
                    Intent intent = new Intent(ZGNBManagerMain.this, TextEditor.class);
                    intent.putExtra(TextEditor.PLUGIN, "TextEditor");
                    startActivityForResult(intent, ActResConstant.list_item_details);
                } catch(Exception e) {
                    Message msg = new Message();
                    msg.what = SHOWMESSAGE;
                    msg.obj = "Open Text exception " + e.getMessage();
                    mHandler.sendMessage(msg);
                }
                mHandler.sendEmptyMessage(DISMISSPROGRESS);
            }
        }).start();
    }

    private void editAxml(final File file) {
        new Thread(new Runnable() {
            public void run() {
                mHandler.sendEmptyMessage(SHOWPROGRESS);
                try {
                    TextEditor.data = FileUtil.readFile(file);
                    Intent intent = new Intent(ZGNBManagerMain.this, TextEditor.class);
                    intent.putExtra(TextEditor.PLUGIN, "AXmlEditor");
                    startActivityForResult(intent, ActResConstant.list_item_details);
                } catch(Exception e) {
                    Message msg = new Message();
                    msg.what = SHOWMESSAGE;
                    msg.obj = "Open Axml exception " + e.getMessage();
                    mHandler.sendMessage(msg);
                }
                mHandler.sendEmptyMessage(DISMISSPROGRESS);
            }
        }).start();
    }

    private void openDexFile(final File file) {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle(getString(R.string.tips));
        builder.setMessage(getString(R.string.dex_instruction));
        builder.setPositiveButton(getString(R.string.tojar), new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int whichButton) {
                new Thread(new Runnable() {
                    public void run() {
                        mHandler.sendEmptyMessage(SHOWPROGRESS);
                        boolean DEX2JAR_SUC = ConverterMain.procdex(file.toString());
                        if(!DEX2JAR_SUC)
                            showToast(getString(R.string.dex2jar_success));
                        else
                            showToast(getString(R.string.dex2jar_fail));
                        mHandler.sendEmptyMessage(DISMISSPROGRESS);
                    }
                }).start();
            }
        });
        builder.setNeutralButton(getString(R.string.editdex), new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int whichButton) {
                new Thread(new Runnable() {
                    public void run() {
                        try {
                            mHandler.sendEmptyMessage(SHOWPROGRESS);
                            ClassListActivity.dexFile = new DexFile(file);
                            Intent intent = new Intent(ZGNBManagerMain.this, ClassListActivity.class);
                            startActivityForResult(intent, ActResConstant.list_item_details);
                        } catch(Exception e) {
                            Message msg = new Message();
                            msg.what = SHOWMESSAGE;
                            msg.obj = "Open dexFile exception " + e.getMessage();
                            mHandler.sendMessage(msg);
                        }
                        mHandler.sendEmptyMessage(DISMISSPROGRESS);
                    }
                }).start();
            }
        });
        builder.setNegativeButton(getString(R.string.disasm_dex), new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                new Thread(new Runnable() {
                    public void run() {
                        mHandler.sendEmptyMessage(SHOWPROGRESS);
                        boolean DISDEX_SUC = BakSmaliFunc.DoBaksmali(file.toString(),
                                             file.toString().substring(0, file.toString().length() - 4) + "_baksmali");
                        if(!DISDEX_SUC)
                            showToast(getString(R.string.disdex_success));
                        else
                            showToast(getString(R.string.disdex_fail));
                        mHandler.sendEmptyMessage(DISMISSPROGRESS);
                    }
                }).start();
            }
        });
        builder.show();
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        switch(requestCode) {
        case ActResConstant.list_item_details:
            switch(resultCode) {
            case ActResConstant.text_editor:
                renameAndWrite();
                break;
            case ActResConstant.zip_list_item:
                mAdapter.notifyDataSetInvalidated();
                toast(ZipManagerMain.zipFileName);
                break;
            }
            break;
        }
    }

    /*
     * @Override public void onConfigurationChanged(Configuration conf){
     * super.onConfigurationChanged(conf); }
     */

    private void renameAndWrite() {
        new Thread(new Runnable() {
            public void run() {
                mHandler.sendEmptyMessage(SHOWPROGRESS);
                FileOutputStream out = null;
                try {
                    FileUtils.rename(mCurrent, mCurrent.getName() + ".bak");
                    out = new FileOutputStream(mCurrent.getAbsolutePath());
                    out.write(TextEditor.data);
                } catch(IOException io) {
                } finally {
                    try {
                        if(out != null)
                            out.close();
                    } catch(IOException e) {
                    }
                    TextEditor.data = null;
                    System.gc();
                }
                Message msg = new Message();
                msg.what = TOAST;
                msg.obj = mCurrent.getName() + getString(R.string.saved);
                mHandler.sendMessage(msg);
                mHandler.sendEmptyMessage(DISMISSPROGRESS);
            }
        }).start();
    }

    private boolean isZip(File file) {
        String name = file.getName().toLowerCase();
        if(name.endsWith(".apk"))
            return true;
        if(name.endsWith(".zip") || name.endsWith(".jar"))
            return true;
        return false;
    }

    @Override
    public void onCreateContextMenu(ContextMenu menu, View v, ContextMenuInfo menuInfo) {
        if(mSelectMod)
            return;
        super.onCreateContextMenu(menu, v, menuInfo);
        menu.setHeaderTitle(R.string.options);
        File file = null;
        AdapterView.AdapterContextMenuInfo info;
        try {
            info = (AdapterView.AdapterContextMenuInfo) menuInfo;
            file = (File) lv.getItemAtPosition(info.position);
            if(!file.isDirectory())
                menu.add(Menu.NONE, R.string.view, Menu.NONE, R.string.view);
        } catch(ClassCastException e) {
            Log.e(TAG, "Bad menuInfo" + e);
        }
        menu.add(Menu.NONE, R.string.delete, Menu.NONE, R.string.delete);
        menu.add(Menu.NONE, R.string.rename, Menu.NONE, R.string.rename);
        if(isZip(file)) {
            menu.add(Menu.NONE, R.string.signed, Menu.NONE, R.string.signed);
            menu.add(Menu.NONE, R.string.extract_all, Menu.NONE, R.string.extract_all);
            menu.add(Menu.NONE, R.string.zipalign, Menu.NONE, R.string.zipalign);
        }
        menu.add(Menu.NONE, R.string.copy, Menu.NONE, R.string.copy);
        menu.add(Menu.NONE, R.string.cut, Menu.NONE, R.string.cut);
        menu.add(Menu.NONE, R.string.paste, Menu.NONE, R.string.paste);
        menu.add(Menu.NONE, R.string.permission, Menu.NONE, R.string.permission);
    }

    @Override
    public boolean onContextItemSelected(MenuItem item) {
        AdapterView.AdapterContextMenuInfo info;
        try {
            info = (AdapterView.AdapterContextMenuInfo) item.getMenuInfo();
        } catch(ClassCastException e) {
            Log.e(TAG, "Bad menuInfo" + e);
            return false;
        }
        mCurrent = (File) lv.getItemAtPosition(info.position);
        position = info.position;
        switch(item.getItemId()) {
        case R.string.delete:
            delete(mCurrent);
            return true;
        case R.string.view:
            viewCurrent();
            return true;
        case R.string.extract_all:
            extractAll(mCurrent);
            return true;
        case R.string.zipalign:
            zipAlign(mCurrent);
            return true;
        case R.string.signed:
            signedFile(mCurrent);
            return true;
        case R.string.rename:
            rename(mCurrent);
            return true;
        case R.string.copy:
            addCopy(mCurrent);
            return false;
        case R.string.cut:
            addCut(mCurrent);
            return false;
        case R.string.paste:
            pasteFile();
            return false;
        case R.string.permission:
            showPermissions();
            return false;
        }
        return false;
    }

    public void zipAlign(final File file) {
        new Thread(new Runnable() {
            @Override
            public void run() {
                mHandler.sendEmptyMessage(SHOWPROGRESS);
                if(Features.isZipAligned(file.toString())) {
                    showToast(getString(R.string.zip_has_aligned));
                    mHandler.sendEmptyMessage(DISMISSPROGRESS);
                    return;
                }
                boolean b = Features.ZipAlign(file.toString(),
                                              file.toString().substring(0, file.toString().length() - 4) + "_aligned"
                                              + file.toString().substring(file.toString().length() - 4));
                if(b)
                    showToast(getString(R.string.zipa_success));
                else
                    showToast(getString(R.string.zipa_fail));
                mHandler.sendEmptyMessage(DISMISSPROGRESS);
            }
        }).start();
    }

    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {
        if(keyCode == KeyEvent.KEYCODE_BACK) {
            if(mFileList != null && mFileList.size() > 0) {
                File first = mFileList.get(0);
                if(first.getName().equals("..") && first.getParentFile() != null) {
                    mCurrentDir = first;
                    mAdapter.notifyDataSetInvalidated();
                    if(!pos.empty())
                        lv.setSelection(pos.pop());
                    return true;
                }
            }
            if(mCurrentDir != null && mCurrentDir.getParentFile() != null) {
                mCurrentDir = mCurrentDir.getParentFile();
                mAdapter.notifyDataSetInvalidated();
                if(!pos.empty())
                    lv.setSelection(pos.pop());
                return true;
            }
            if(mCurrentDir != null && mCurrentDir.getParent() == null) {
                finish();
                if(!mSelectMod)
                    System.exit(0);
            }
        }
        return super.onKeyDown(keyCode, event);
    }

    @Override
    protected void onResume() {
        super.onResume();
        if(initialized) mAdapter.notifyDataSetChanged();
    }

    @Override
    public boolean onPrepareOptionsMenu(Menu menu) {
        super.onPrepareOptionsMenu(menu);
        menu.clear();
        menu.add(Menu.NONE, R.string.add_folder, Menu.NONE, R.string.add_folder);
        if(mClipboard != null)
            menu.add(Menu.NONE, R.string.paste, Menu.NONE, R.string.paste);
        menu.add(Menu.NONE, R.string.about, Menu.NONE, R.string.about);
        if(!mSelectMod)
            menu.add(Menu.NONE, R.string.exit, Menu.NONE, R.string.exit);
        return true;
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
    }

    public void clearAll() {
        mCurrent = null;
        mClipboard = null;
        mCurrentDir = null;
        mCut = false;
        pos = null;
        System.gc();
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int itemId = item.getItemId();
        switch(itemId) {
        case R.string.add_folder:
            newFolder();
            break;
        case R.string.paste:
            pasteFile();
            break;
        case R.string.about:
            showAbout();
            break;
        case R.string.exit:
            finish();
            clearAll();
            System.exit(0);
            break;
        }
        return true;
    }

    private void signedFile(final File file) {
        new Thread(new Runnable() {
            public void run() {
                mHandler.sendEmptyMessage(SHOWPROGRESS);
                try {
                    String out = file.getAbsolutePath();
                    int i = out.lastIndexOf(".");
                    if(i != -1)
                        out = out.substring(0, i) + ".signed" + out.substring(i);
                    apksigner.Main.sign(file, out);
                    Message msg = new Message();
                    msg.what = TOAST;
                    msg.obj = out + getString(R.string.signed_success);
                    mHandler.sendMessage(msg);
                } catch(Exception e) {
                    Message msg = new Message();
                    msg.what = SHOWMESSAGE;
                    msg.obj = "signed error: " + e.getMessage();
                    mHandler.sendMessage(msg);
                }
                mHandler.sendEmptyMessage(DISMISSPROGRESS);
            }
        }).start();
    }

    private void extractAll(final File file) {
        String absName = file.getAbsolutePath();
        int i = absName.indexOf('.');
        if(i != -1)
            absName = absName.substring(0, i);
        absName += "_unpack";
        final EditText srcName = new EditText(this);
        srcName.setText(absName);
        AlertDialog.Builder alert = new AlertDialog.Builder(this);
        alert.setTitle(R.string.extract_path);
        alert.setView(srcName);
        alert.setPositiveButton(R.string.btn_ok, new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int whichButton) {
                String src = srcName.getText().toString();
                if(src.length() == 0) {
                    toast(getString(R.string.extract_path_empty));
                    return;
                }
                new Thread(new Runnable() {
                    public void run() {
                        mHandler.sendEmptyMessage(SHOWPROGRESS);
                        try {
                            ZipExtract.unzipAll(new ZipFile(file), new File(srcName.getText().toString()));
                        } catch(Exception e) {
                        }
                        mHandler.sendEmptyMessage(DISMISSPROGRESS);
                    }
                }).start();
            }
        });
        alert.setNegativeButton(R.string.btn_cancel, null);
        alert.show();
    }

    private void dialogMenu() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle(mCurrent.getName());
        builder.setItems(R.array.dialog_menu, new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int which) {
                switch(which) {
                case 0:
                    viewCurrent();
                    break;
                case 1:
                    editText(mCurrent);
                    break;
                case 2:
                    delete(mCurrent);
                    break;
                case 3:
                    rename(mCurrent);
                    break;
                case 4:
                    addCopy(mCurrent);
                    break;
                case 5:
                    addCut(mCurrent);
                    break;
                case 6:
                    showPermissions();
                }
            }
        });
        builder.show();
    }

    private void setPermBit(int perms, int bit, int id) {
        CheckBox ck = (CheckBox) mPermissionDialog.findViewById(id);
        ck.setChecked(((perms >> bit) & 1) == 1);
    }

    private int getPermBit(int bit, int id) {
        CheckBox ck = (CheckBox) mPermissionDialog.findViewById(id);
        int ret = (ck.isChecked()) ? (1 << bit) : 0;
        return ret;
    }

    /**
     * Show and edit file permissions
     */
    public void showPermissions() {
        mPermissionDialog.setTitle(mCurrent.getName());
        try {
            int perms = FileUtils.getPermissions(mCurrent);
            setPermBit(perms, 8, R.id.ckOwnRead);
            setPermBit(perms, 7, R.id.ckOwnWrite);
            setPermBit(perms, 6, R.id.ckOwnExec);
            setPermBit(perms, 5, R.id.ckGrpRead);
            setPermBit(perms, 4, R.id.ckGrpWrite);
            setPermBit(perms, 3, R.id.ckGrpExec);
            setPermBit(perms, 2, R.id.ckOthRead);
            setPermBit(perms, 1, R.id.ckOthWrite);
            setPermBit(perms, 0, R.id.ckOthExec);
            mPermissionDialog.show();
        } catch(Exception e) {
            showMessage(this, "Permission Exception", e.getMessage());
        }
    }

    /**
     * Perform permission setting
     */
    public void setPermissions() {
        mPermissionDialog.hide();
        int perms = getPermBit(8, R.id.ckOwnRead) | getPermBit(7, R.id.ckOwnWrite) | getPermBit(6, R.id.ckOwnExec)
                    | getPermBit(5, R.id.ckGrpRead) | getPermBit(4, R.id.ckGrpWrite) | getPermBit(3, R.id.ckGrpExec)
                    | getPermBit(2, R.id.ckOthRead) | getPermBit(1, R.id.ckOthWrite) | getPermBit(0, R.id.ckOthExec);
        try {
            FileUtils.chmod(mCurrent, perms);
            toast(Integer.toString(perms, 8));
            mAdapter.notifyDataSetChanged();
        } catch(Exception e) {
            showMessage(this, "Set Permission Exception", e.getMessage());
        }
    }

    private void viewCurrent() {
        Intent intent = new Intent(Intent.ACTION_VIEW);
        Uri uri = FileProvider.getUriForFile(this, getApplicationContext().getPackageName() + ".provider", mCurrent);
        intent.setFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
        //Uri uri = Uri.fromFile(mCurrent);
        String mime = URLConnection.guessContentTypeFromName(uri.toString());
        if(mime != null) {
            if("text/x-java".equals(mime) || "text/xml".equals(mime))
                intent.setDataAndType(uri, "text/plain");
            else
                intent.setDataAndType(uri, mime);
        } else intent.setDataAndType(uri, "*/*");
      //  try {
            startActivity(intent);
       // } catch(Exception e) {
       //     showMessage(this, "Intent Exception", e.getMessage());
      //  }
    }

    public void toast(String message) {
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show();
    }

    private void addCopy(File file) {
        mClipboard = file;
        toast(getString(R.string.copy_to) + file.getName());
        mCut = false;
    }

    private void addCut(File file) {
        mClipboard = file;
        toast(getString(R.string.cut_to) + file.getName());
        mCut = true;
    }

    private void pasteFile() {
        String message = "";
        if(mClipboard == null) {
            showMessage(this, getString(R.string.copy_exception), getString(R.string.copy_nothing));
            return;
        }
        final File destination = new File(mCurrentDir, mClipboard.getName());
        if(destination.exists())
            message = String.format(getString(R.string.copy_message), destination.getName());
        if(message != "") {
            prompt(this, getString(R.string.over_write), message, new DialogInterface.OnClickListener() {
                public void onClick(DialogInterface dialog, int which) {
                    if(which == AlertDialog.BUTTON_POSITIVE)
                        performPasteFile(mClipboard, destination);
                }
            });
        } else
            performPasteFile(mClipboard, destination);
    }

    protected void performPasteFile(final File source, final File destination) {
        if(source.isDirectory())
            showMessage(this, getString(R.string.copy_exception), getString(R.string.copy_exist));
        else {
            new Thread(new Runnable() {
                public void run() {
                    mHandler.sendEmptyMessage(SHOWPROGRESS);
                    try {
                        copyFile(source, destination);
                        if(mCut)
                            source.delete();
                    } catch(Exception e) {
                    }
                    mClipboard = null;
                    Message msg = new Message();
                    msg.what = TOAST;
                    msg.obj = destination.getName() + getString(R.string.copied);
                    mHandler.sendMessage(msg);
                    mHandler.sendEmptyMessage(DISMISSPROGRESS);
                }
            }).start();
        }
    }

    /**
     * Copy file from source to destination.
     *
     * @param source
     * @param destination
     */

    private static void copyFile(File source, File destination) throws Exception {
        byte[] buf = new byte[1024];
        InputStream input = new BufferedInputStream(new FileInputStream(source));
        OutputStream output = new BufferedOutputStream(new FileOutputStream(destination));
        int len;
        while((len = input.read(buf)) > 0)
            output.write(buf, 0, len);
        output.flush();
        output.close();
        input.close();
        int perms = FileUtils.getPermissions(source) & 0777;
        FileUtils.chmod(destination, perms);
        destination.setLastModified(source.lastModified());
    }

    public void ExtractRar(final File name) {
        new Thread(new Runnable() {
            public void run() {
                mHandler.sendEmptyMessage(SHOWPROGRESS);
                int results = Features.ExtractAllRAR(name.toString(),
                                                     name.toString().substring(0, name.toString().length() - 4) + "_extracted");
                if(results == 0)
                    showToast(getString(R.string.extract_rar_success));
                else if(results == -1601)
                    showToast(getString(R.string.rar_native_error));
                else
                    showToast(getString(R.string.failed_to_extract_rar));
                mHandler.sendEmptyMessage(DISMISSPROGRESS);
            }
        }).start();
    }

    public void OatToDex(final File name) {
        new Thread(new Runnable() {
            public void run() {
                mHandler.sendEmptyMessage(SHOWPROGRESS);
                String str = name.toString();
                boolean success1 = Features.Oat2Dex(str);
                mHandler.sendEmptyMessage(DISMISSPROGRESS);
                if(success1)
                    showToast(getString(R.string.oat2dex_success));
                else
                    showToast(getString(R.string.oat2dex_fail));
            }
        }).start();
    }

    public void PELF(File name) {
        if(Features.isValidElf(name.toString())) {
            Intent i = new Intent(this, ElfActivity.class);
            i.putExtra("FILE_NAME", name.toString());
            startActivity(i);
            this.mAdapter.notifyDataSetInvalidated();
        }
    }

    public void ConOdex(final File name) {
        if(Features.isValidElf(name.toString()))
            OatToDex(name);
        else {
            new Thread(new Runnable() {
                public void run() {
                    mHandler.sendEmptyMessage(SHOWPROGRESS);
                    boolean success2 = Features.Odex2Dex(name.toString(),
                                                         name.toString().substring(0, name.toString().length() - 5) + "_converted.dex");
                    if(success2)
                        showToast(getString(R.string.odex2dex_success));
                    else
                        showToast(getString(R.string.odex2dex_fail));
                    mHandler.sendEmptyMessage(DISMISSPROGRESS);
                }
            }).start();
        }
    }

    public void buildSmali(final File name) {
        new Thread(new Runnable() {
            public void run() {
                mHandler.sendEmptyMessage(SHOWPROGRESS);
                boolean success2 = main.SmaliBuildDex(name.toString(), name.toString() + "_smali.dex", null);
                if(success2)
                    showToast(getString(R.string.build_smali_success));
                else
                    showToast(getString(R.string.build_smali_fail));
                mHandler.sendEmptyMessage(DISMISSPROGRESS);
            }
        }).start();
    }

    public void showToast(final String msg) {
        this.runOnUiThread(new Runnable() {
            @Override
            public void run() {
                Toast.makeText(getApplicationContext(), msg, Toast.LENGTH_SHORT).show();
            }
        });
    }

    public boolean isPreparedToBuildSmali = false;

    @Override
    protected Dialog onCreateDialog(int id) {
        ProgressDialog dialog = new ProgressDialog(this);
        dialog.setMessage(getString(R.string.wait));
        dialog.setIndeterminate(true);
        dialog.setCancelable(false);
        return dialog;
    }

    public static void showMessage(Context context, String title, String message) {
        AlertDialog.Builder builder = new AlertDialog.Builder(context);
        builder.setTitle(title);
        builder.setMessage(message);
        builder.setNeutralButton(R.string.btn_ok, null);
        builder.show();
    }

    @SuppressLint("MissingPermission")
    public void SystemInfo() {
        StringBuilder info = new StringBuilder();
        TelephonyManager tm = (TelephonyManager) getBaseContext().getSystemService(Context.TELEPHONY_SERVICE);
        String androidId = Settings.Secure.getString(getContentResolver(), Settings.Secure.ANDROID_ID);
        String dui = new UUID(androidId.hashCode(),
                              ((long) tm.getDeviceId().hashCode() << 64 | tm.hashCode()) & tm.toString().hashCode()).toString();
        dui = dui.replaceAll("-", "");
        info.append("手机型号：" + Build.MODEL + "\n");
        info.append("制造商：" + Build.MANUFACTURER + "\n");
        info.append("Android 版本：" + Build.VERSION.RELEASE + "\n");
        info.append("Android SDK代号：" + Build.VERSION.SDK_INT + "\n");
        info.append("CPU类型：" + Build.CPU_ABI + " / " + Build.CPU_ABI2 + "\n");
        info.append("硬件序列号：" + Build.SERIAL + "\n");
        info.append("硬件名称：" + Build.HARDWARE + "\n");
        info.append("基带版本：" + Build.getRadioVersion() + "\n");
        info.append("BootLoader 版本：" + Build.BOOTLOADER + "\n");
        info.append("设备ID：" + tm.getDeviceId() + "\n");
        info.append("机器码：" + dui + "\n");
        info.append("App签名：" + Features.compressStrToInt(getPkgSign(this)));
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle(getString(R.string.system_info));
        builder.setMessage(info.toString());
        builder.setNeutralButton(R.string.btn_ok, null);
        builder.show();
    }

    public void showAbout() {
        Bitmap bmp = null;
        bmp = BitmapFactory.decodeResource(getResources(), R.drawable.android);
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setIcon(new BitmapDrawable(bmp));
        String title = getString(R.string.app_name);
        try {
            PackageManager pm = getPackageManager();
            PackageInfo pi = pm.getPackageInfo(getPackageName(), 0);
            if(pi.versionName != null)
                title += " " + pi.versionName;
        } catch(Exception e) {
            e.printStackTrace();
        }
        builder.setTitle(title);
        builder.setMessage(getString(R.string.about_content));
        builder.setNeutralButton(R.string.btn_ok, null);
        builder.setPositiveButton(R.string.system_info, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                dialog.dismiss();
                SystemInfo();
            }
        });
        builder.show();
    }

    public static void prompt(Context context, String title, String message,
                              DialogInterface.OnClickListener btnlisten) {
        AlertDialog.Builder builder = new AlertDialog.Builder(context);
        builder.setTitle(title);
        builder.setMessage(message);
        builder.setPositiveButton(R.string.btn_ok, btnlisten);
        builder.setNegativeButton(R.string.btn_cancel, btnlisten);
        builder.show();
    }

    private void delete(final File file) {
        AlertDialog.Builder alert = new AlertDialog.Builder(this);
        alert.setTitle(R.string.delete);
        alert.setMessage(String.format(getString(R.string.is_delete), file.getName()));
        alert.setPositiveButton(R.string.btn_yes, new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int whichButton) {
                new Thread(new Runnable() {
                    public void run() {
                        mHandler.sendEmptyMessage(SHOWPROGRESS);
                        FileUtils.delete(file);
                        mFileList.remove(file);
                        Message msg = new Message();
                        msg.what = TOAST;
                        msg.obj = file.getName() + getString(R.string.deleted);
                        mHandler.sendMessage(msg);
                        mHandler.sendEmptyMessage(DISMISSPROGRESS);
                    }
                }).start();
            }
        });
        alert.setNegativeButton(R.string.btn_no, null);
        alert.show();
    }

    private void newFolder() {
        final EditText folderName = new EditText(this);
        folderName.setHint(R.string.folder_name);
        final AlertDialog.Builder alert = new AlertDialog.Builder(this);
        alert.setTitle(R.string.add_folder);
        alert.setView(folderName);
        alert.setPositiveButton(R.string.btn_ok, new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int whichButton) {
                String name = folderName.getText().toString();
                if(name.length() == 0) {
                    toast(getString(R.string.directory_empty));
                    return;
                } else {
                    for(File f : mFileList) {
                        if(f.getName().equals(name)) {
                            toast(String.format(getString(R.string.directory_exists, name)));
                            return;
                        }
                    }
                }
                File dir = new File(mCurrentDir, name);
                if(!dir.mkdirs())
                    toast(String.format(getString(R.string.directory_cannot_create), name));
                else
                    toast(String.format(getString(R.string.directory_created), name));
                mAdapter.notifyDataSetInvalidated();
            }
        });
        alert.setNegativeButton(R.string.btn_cancel, null);
        alert.show();
    }

    private void rename(final File file) {
        final EditText newName = new EditText(this);
        newName.setText(file.getName());
        AlertDialog.Builder alert = new AlertDialog.Builder(this);
        alert.setTitle(R.string.rename);
        alert.setView(newName);
        alert.setPositiveButton(R.string.btn_ok, new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int whichButton) {
                String name = newName.getText().toString();
                if(name.length() == 0) {
                    toast(getString(R.string.name_empty));
                    return;
                } else {
                    for(File f : mFileList) {
                        if(f.getName().equals(name)) {
                            toast(String.format(getString(R.string.file_exists), name));
                            return;
                        }
                    }
                }
                if(!FileUtils.rename(file, name))
                    toast(String.format(getString(R.string.cannot_rename), file.getPath()));
                mAdapter.notifyDataSetInvalidated();
            }
        });
        alert.setNegativeButton(R.string.btn_cancel, null);
        alert.show();
    }

    private Drawable showApkIcon(String apkPath) {
        PackageManager pm = this.getPackageManager();
        PackageInfo info = pm.getPackageArchiveInfo(apkPath,
                           PackageManager.GET_ACTIVITIES);
        if(info != null) {
            ApplicationInfo appInfo = info.applicationInfo;
            appInfo.sourceDir = apkPath;
            appInfo.publicSourceDir = apkPath;
            try {
                return appInfo.loadIcon(pm);
            } catch(OutOfMemoryError e) {
                e.printStackTrace();
            }
        }
        return getResources().getDrawable(R.drawable.android);
    }

    private class FileListAdapter extends BaseAdapter {

        protected final Context mContext;
        protected final LayoutInflater mInflater;
        private SimpleDateFormat format = new SimpleDateFormat("yy-MM-dd HH:mm:ss");
        AsyncImageLoader asyn = new AsyncImageLoader();

        public FileListAdapter(Context context) {
            mContext = context;
            mInflater = (LayoutInflater) mContext.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        }

        public int getCount() {
            return getFileList().size();
        }

        public Object getItem(int position) {
            return getFileList().get(position);
        }

        public long getItemId(int position) {
            return position;
        }

        private String permRwx(int perm) {
            String result;
            result = ((perm & 04) != 0 ? "r" : "-") + ((perm & 02) != 0 ? "w" : "-") + ((perm & 1) != 0 ? "x" : "-");
            return result;
        }

        private String permFileType(int perm) {
            String result = "?";
            switch(perm & S_IFMT) {
            case S_IFLNK:
                result = "l";
                break; /* symbolic link */
            case S_IFREG:
                result = "-";
                break; /* regular */
            case S_IFBLK:
                result = "b";
                break; /* block special */
            case S_IFDIR:
                result = "d";
                break; /* directory */
            case S_IFCHR:
                result = "c";
                break; /* character special */
            case S_IFIFO:
                result = "p";
                break; /* this is a FIFO */
            }
            return result;
        }

        public String permString(int perms) {
            String result;
            result = permFileType(perms) + permRwx(perms >> 6) + permRwx(perms >> 3) + permRwx(perms);
            return result;
        }

        public View getView(int position, View convertView, ViewGroup parent) {
            final File file = getFileList().get(position);
            String name = file.getName().toLowerCase();
            RelativeLayout container;
            if(convertView == null)
                container = (RelativeLayout) mInflater.inflate(R.layout.list_item_details, null);
            else
                container = (RelativeLayout) convertView;
            final ImageView icon = (ImageView) container.findViewById(R.id.icon);
            if(file.isDirectory())
                icon.setImageResource(R.drawable.folder);
            else if(name.endsWith(".apk")) {
                Drawable drawable = asyn.loadDrawable(file.getAbsolutePath(), icon, new ImageCallback() {
                    public void imageLoaded(Drawable drawable, ImageView imageView) {
                        icon.setImageDrawable(drawable);
                    }
                });
                icon.setImageDrawable(drawable);
            } else if(name.endsWith(".png") || name.endsWith(".jpg"))
                icon.setImageResource(R.drawable.image);
            else if(name.endsWith(".zip") || name.endsWith(".rar") || name.endsWith(".7z"))
                icon.setImageResource(R.drawable.zip);
            else if(name.endsWith(".jar"))
                icon.setImageResource(R.drawable.jar);
            else if(name.endsWith(".so"))
                icon.setImageResource(R.drawable.sharedlib);
            else if(name.endsWith(".dex") || name.endsWith(".odex") || name.endsWith(".oat"))
                icon.setImageResource(R.drawable.dex);
            else if(name.endsWith(".rc") || name.endsWith(".sh"))
                icon.setImageResource(R.drawable.script);
            else if(name.endsWith(".xml"))
                icon.setImageResource(R.drawable.xml);
            else if(name.endsWith(".txt") || name.endsWith(".log") || name.endsWith(".c") || name.endsWith(".cpp")
                    || name.endsWith(".cs") || name.endsWith(".h") || name.endsWith(".hpp") || name.endsWith(".java")
                    || name.endsWith(".smali"))
                icon.setImageResource(R.drawable.text);
            else if(name.endsWith(".arsc"))
                icon.setImageResource(R.drawable.arsc);
            else if(name.endsWith(".pdf"))
                icon.setImageResource(R.drawable.pdf);
            else if(name.endsWith(".xls") || name.endsWith(".xlsx"))
                icon.setImageResource(R.drawable.excel);
            else if(name.endsWith(".ppt") || name.endsWith(".pptx") || name.endsWith(".pps")
                    || name.endsWith(".ppsx"))
                icon.setImageResource(R.drawable.ppt);
            else if(name.endsWith(".doc") || name.endsWith(".docx") || name.endsWith(".dot")
                    || name.endsWith(".dotx"))
                icon.setImageResource(R.drawable.word);
            else if(name.endsWith(".mp4") || name.endsWith(".3gp") || name.endsWith(".avi") || name.endsWith(".wmv")
                    || name.endsWith(".vob") || name.endsWith(".ts") || name.endsWith(".flv") || name.endsWith(".rm")
                    || name.endsWith(".rmvb") || name.endsWith(".f4v") || name.endsWith(".mov")
                    || name.endsWith(".webm") || name.endsWith(".mpg") || name.endsWith(".asf")
                    || name.endsWith(".mkv"))
                icon.setImageResource(R.drawable.video);
            else if(name.endsWith(".mp3") || name.endsWith(".aac") || name.endsWith(".mp2") || name.endsWith(".wav")
                    || name.endsWith(".wma") || name.endsWith(".ogg") || name.endsWith(".ape")
                    || name.endsWith(".amr"))
                icon.setImageResource(R.drawable.audio);
            else
                icon.setImageResource(R.drawable.file);
            TextView text = (TextView) container.findViewById(R.id.text);
            TextView perm = (TextView) container.findViewById(R.id.permissions);
            TextView time = (TextView) container.findViewById(R.id.times);
            TextView size = (TextView) container.findViewById(R.id.size);
            text.setText(file.getName());
            String perms;
            try {
                perms = permString(FileUtils.getPermissions(file));
            } catch(Exception e) {
                perms = "????";
            }
            perm.setText(perms);
            Date date = new Date(file.lastModified());
            time.setText(format.format(date));
            if(file.isDirectory())
                size.setText("");
            else
                size.setText(convertBytesLength(file.length()));
            return container;
        }

        private String convertBytesLength(long size) {
            DecimalFormat formater = new DecimalFormat("####.00");
            if(size < 1024)
                return size + "B";
            else if(size < 1024 * 1024) {
                float kbsize = size / 1024f;
                return formater.format(kbsize) + "KB";
            } else if(size < 1024 * 1024 * 1024) {
                float mbsize = size / 1024f / 1024f;
                return formater.format(mbsize) + "MB";
            } else {
                float gbsize = size / 1024f / 1024f / 1024f;
                return formater.format(gbsize) + "GB";
            }
        }

        protected List<File> getFileList() {
            return mFileList;
        }
    }

    public class AsyncImageLoader {
        private HashMap<String, SoftReference<Drawable>> imageCache;

        public AsyncImageLoader() {
            imageCache = new HashMap<String, SoftReference<Drawable>>();
        }

        public Drawable loadDrawable(final String imageUrl, final ImageView imageView,
                                     final ImageCallback imageCallback) {
            if(imageCache.containsKey(imageUrl)) {
                SoftReference<Drawable> softReference = imageCache.get(imageUrl);
                Drawable drawable = softReference.get();
                if(drawable != null)
                    return drawable;
            }
            final Handler handler = new Handler() {
                public void handleMessage(Message message) {
                    imageCallback.imageLoaded((Drawable) message.obj, imageView);
                }
            };
            new Thread() {
                public void run() {
                    Drawable drawable = showApkIcon(imageUrl);
                    imageCache.put(imageUrl, new SoftReference<Drawable>(drawable));
                    Message message = handler.obtainMessage(0, drawable);
                    handler.sendMessage(message);
                }
            } .start();
            return getResources().getDrawable(R.drawable.android);
        }
    }

    public static interface ImageCallback {
        public void imageLoaded(Drawable imageDrawable, ImageView imageView);
    }

}
