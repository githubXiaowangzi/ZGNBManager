package com.zengge.nbmanager;

import android.app.Dialog;
import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.os.Message;
import android.support.v7.app.AppCompatActivity;
import android.view.View;
import android.view.ViewGroup;
import android.view.Menu;
import android.view.MenuItem;
import android.view.MenuInflater;
import android.view.LayoutInflater;
import android.view.KeyEvent;
import android.view.ContextMenu;
import android.view.ContextMenu.ContextMenuInfo;
import android.content.Intent;
import android.content.Context;
import android.content.DialogInterface;
import android.content.res.Configuration;
import android.widget.LinearLayout;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.ImageView;
import android.widget.Toast;
import android.widget.BaseAdapter;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.AdapterView;
import android.util.Log;
import android.database.DataSetObserver;

import java.text.SimpleDateFormat;
import java.util.List;
import java.util.ArrayList;
import java.util.Map;
import java.util.HashMap;
import java.util.Set;
import java.util.Stack;
import java.util.Enumeration;
import java.util.Date;
import java.util.Collections;
import java.util.Comparator;
import java.util.zip.*;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.FileOutputStream;
import java.io.ByteArrayOutputStream;
import java.io.ByteArrayInputStream;

import org.jf.dexlib.*;

import com.zengge.arsceditor.ArscActivity;
import com.zengge.res.*;
import com.zengge.util.*;

public class ZipManagerMain extends AppCompatActivity {

    public static final String EXTRACTPATH = Environment.getExternalStorageDirectory().getPath() + "/ZGNBManager";
    public static HashMap<String, byte[]> zipEnties;
    public Tree tree;
    private static ZipFile zipFile;
    public static String file;
    public static String zipFileName;
    private String title = "";
    private boolean isSigne = false;
    private boolean isChanged = false;
    private FileListAdapter mAdapter;
    private List<String> fileList;

    private static Stack<String> path;
    private static int dep;

    private Handler mHandler = new Handler() {
        @Override
        public void handleMessage(Message msg) {
            switch(msg.what) {
            case WRITEZIP:
                ZipManagerMain.this.showDialog(R.string.write_zip);
                break;
            case SIGNED:
                ZipManagerMain.this.showDialog(R.string.signed_zip);
                break;
            case LOADING:
                ZipManagerMain.this.showDialog(R.string.load_data);
                break;
            case REMOVE:
                ZipManagerMain.this.showDialog(R.string.zip_remove_progress);
                break;
            case EXTRACT:
                ZipManagerMain.this.showDialog(R.string.extract);
                break;
            case REPLACE:
                ZipManagerMain.this.showDialog(R.string.replacing);
                break;
            case R.string.write_zip:
            case R.string.signed_zip:
            case R.string.load_data:
            case R.string.zip_remove_progress:
            case R.string.extract:
            case R.string.replacing:
                ZipManagerMain.this.dismissDialog(msg.what);
                break;
            case ERROR:
                ZGNBManagerMain.showMessage(ZipManagerMain.this, "", msg.obj.toString());
                break;
            case TOAST:
                toast(msg.obj.toString());
                break;
            case UPDATE:
                mod = OTHER;
                mAdapter.notifyDataSetInvalidated();
                break;
            }
        }
    };

    private int mod;
    private static final int UNUSE = -1;
    private static final int WRITEZIP = 0;
    private static final int SIGNED = 1;
    private static final int SENDINTENT = 2;
    private static final int ERROR = 3;
    private static final int LOADING = 4;
    private static final int REMOVE = 5;

    private static final int OPENDIR = 6;
    private static final int BACK = 7;
    private static final int OTHER = 8;
    private static final int UPDATE = 9;

    private static final int EXTRACT = 10;
    private static final int TOAST = 11;
    private static final int REPLACE = 12;

    public ListView lv;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.listact);
        lv = findViewById(R.id.zglist);
        init();
        mAdapter = new FileListAdapter(this);
        mAdapter.registerDataSetObserver(new DataSetObserver() {
            @Override
            public void onInvalidated() {
                switch(mod) {
                case OPENDIR:
                    tree.push(file);
                    fileList = tree.list();
                    break;
                case BACK:
                    tree.pop();
                    fileList = tree.list();
                    break;
                case OTHER:
                    fileList = tree.list();
                    break;
                }
                setTitle(title + tree.getCurPath());
            }
        });
        lv.setAdapter(mAdapter);
        registerForContextMenu(lv);
        lv.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                file = (String) parent.getItemAtPosition(position);
                if(tree.isDirectory(file)) {
                    mod = OPENDIR;
                    mAdapter.notifyDataSetInvalidated();
                    return;
                }
                mod = UNUSE;
                if(file.toLowerCase().endsWith(".arsc")) {
                    new Thread(new Runnable() {
                        public void run() {
                            mHandler.sendEmptyMessage(LOADING);
                            textEditArsc(file);
                            // dismissDialog
                            mHandler.sendEmptyMessage(R.string.load_data);
                        }
                    }).start();
                } else if(file.toLowerCase().endsWith(".xml")) {
                    new Thread(new Runnable() {
                        public void run() {
                            mHandler.sendEmptyMessage(LOADING);
                            textEditAxml(file);
                            mHandler.sendEmptyMessage(R.string.load_data);
                        }
                    }).start();
                } else if(file.toLowerCase().endsWith(".dex")) {
                    new Thread(new Runnable() {
                        public void run() {
                            mHandler.sendEmptyMessage(LOADING);
                            openDexFile(file);
                            // dismissDialog
                            mHandler.sendEmptyMessage(R.string.load_data);
                        }
                    }).start();
                }
            }
        });
    }

    private void resultToFileBrowser() {
        Intent intent = new Intent();
        setResult(ActResConstant.zip_list_item, intent);
        finish();
    }

    private void openDexFile(String file) {
        try {
            byte[] data = readEntry(file);
            ClassListActivity.dexFile = new DexFile(data);
            Intent intent = new Intent(this, ClassListActivity.class);
            startActivityForResult(intent, ActResConstant.zip_list_item);
        } catch(Exception e) {
            Message msg = new Message();
            msg.what = ERROR;
            msg.obj = e.getMessage();
            mHandler.sendMessage(msg);
        }
    }

    private boolean replaceAxml(String name, String src, String dst) {
        boolean isReplace = false;
        try {
            ArrayList<String> data = new ArrayList<String>();
            AXmlDecoder axml = AXmlDecoder.read(new ByteArrayInputStream(readEntryAbsName(name)));
            axml.mTableStrings.getStrings(data);
            for(int i = 0, len = data.size(); i < len; i++) {
                String s = data.get(i);
                if(s.indexOf(src) != -1) {
                    isReplace = true;
                    data.set(i, s.replace(src, dst));
                }
            }
            if(isReplace) {
                ByteArrayOutputStream out = new ByteArrayOutputStream();
                axml.write(data, out);
                zipEnties.put(name, out.toByteArray());
                isChanged = true;
            }
        } catch(Exception e) {
        }
        System.gc();
        return isReplace;
    }

    private int replaceAllAxml(String src, String dst) {
        int count = 0;
        for(String name : zipEnties.keySet()) {
            if(name.toLowerCase().endsWith(".xml")) {
                if(replaceAxml(name, src, dst))
                    count++;
            }
        }
        return count;
    }

    private void replace() {
        LayoutInflater inflate = getLayoutInflater();
        LinearLayout line = (LinearLayout) inflate.inflate(R.layout.alert_dialog_replace_axml, null);
        final EditText srcName = (EditText) line.findViewById(R.id.src_edit);
        final EditText dstName = (EditText) line.findViewById(R.id.dst_edit);
        srcName.setText("");
        dstName.setText("");
        AlertDialog.Builder alert = new AlertDialog.Builder(this);
        alert.setTitle(R.string.replace_axml);
        alert.setView(line);
        alert.setPositiveButton(R.string.btn_ok, new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int whichButton) {
                final String src = srcName.getText().toString();
                final String dst = dstName.getText().toString();
                if(src.length() == 0) {
                    toast(getString(R.string.search_name_empty));
                    return;
                }
                new Thread(new Runnable() {
                    public void run() {
                        mHandler.sendEmptyMessage(REPLACE);
                        int count = replaceAllAxml(src, dst);
                        if(count > 0) {
                            Message msg = new Message();
                            msg.what = TOAST;
                            msg.obj = getString(R.string.replace_count) + count;
                            mHandler.sendMessage(msg);
                        }
                        mHandler.sendEmptyMessage(R.string.replacing);
                    }
                }).start();
            }
        });
        alert.setNegativeButton(R.string.btn_cancel, null);
        alert.show();
    }

    private void init() {
        title = zipFileName.substring(zipFileName.lastIndexOf("/") + 1) + "/";
        if(zipFileName.endsWith(".apk"))
            isSigne = true;
        unZip(zipFileName);
        tree = new Tree(zipEnties.keySet());
        setTitle(title + tree.getCurPath());
        fileList = tree.list();
    }

    private void unZip(String name) {
        if(zipEnties != null)
            return;
        zipEnties = new HashMap<String, byte[]>();
        try {
            zipFile = new ZipFile(name);
            readZip(zipFile, zipEnties);
        } catch(IOException e) {
            zipEnties.put(e.getMessage(), null);
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu m) {
        MenuInflater in = getMenuInflater();
        in.inflate(R.menu.zip_editor_menu, m);
        return true;
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        clearAll();
    }

    public void clearAll() {
        zipEnties = null;
        zipFile = null;
        path = null;
        dep = 0;
        file = null;
        System.gc();
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem mi) {
        int id = mi.getItemId();
        switch(id) {
        case R.id.add_entry:
            selectFile();
            break;
        case R.id.save_file:
            saveFile();
            break;
        case R.id.replace_axml:
            replace();
            break;
        }
        return true;
    }

    private void showDialog() {
        ZGNBManagerMain.prompt(this, getString(R.string.prompt), getString(R.string.is_save),
        new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dailog, int which) {
                if(which == AlertDialog.BUTTON_POSITIVE)
                    saveFile();
                else if(which == AlertDialog.BUTTON_NEGATIVE)
                    finish();
            }
        });
    }

    private void saveFile() {
        new Thread(new Runnable() {
            public void run() {
                String out = zipFile.getName();
                int i = out.lastIndexOf(".");
                if(i != -1)
                    out = out.substring(0, i) + (isSigne ? ".signed" : ".new") + out.substring(i);
                try {
                    if(isSigne) {
                        mHandler.sendEmptyMessage(WRITEZIP);
                        File temp = File.createTempFile("mao", ".tmp", getCacheDir());
                        temp.deleteOnExit();
                        zip(zipFile, zipEnties, temp);
                        apksigner.Main.sign(temp, out);
                        temp.delete();
                    } else {
                        mHandler.sendEmptyMessage(WRITEZIP);
                        File file = new File(out);
                        zip(zipFile, zipEnties, file);
                    }
                } catch(Exception e) {
                    Message msg = new Message();
                    msg.what = ERROR;
                    msg.obj = e.getMessage();
                    mHandler.sendMessage(msg);
                    // dismissDialog
                    mHandler.sendEmptyMessage(R.string.write_zip);
                    return;
                }
                // dismissDialog
                mHandler.sendEmptyMessage(R.string.write_zip);
                resultToFileBrowser();
            }
        }).start();
    }

    @Override
    public void onCreateContextMenu(ContextMenu menu, View v, ContextMenuInfo menuInfo) {
        menu.add(Menu.NONE, R.string.zip_editor_remove, Menu.NONE, R.string.zip_editor_remove);
        menu.add(Menu.NONE, R.string.extract, Menu.NONE, R.string.extract);
    }

    @Override
    protected Dialog onCreateDialog(int id) {
        ProgressDialog dialog = new ProgressDialog(this);
        switch(id) {
        case R.string.write_zip:
            dialog.setMessage(getString(R.string.write_zip));
            break;
        case R.string.load_data:
            dialog.setMessage(getString(R.string.load_data));
            break;
        case R.string.signed_zip:
            dialog.setMessage(getString(R.string.signed_zip));
            break;
        case R.string.zip_remove_progress:
            dialog.setMessage(getString(R.string.zip_remove_progress));
            break;
        case R.string.extract:
            dialog.setMessage(getString(R.string.extracting));
            break;
        case R.string.replacing:
            dialog.setMessage(getString(R.string.replacing));
            break;
        }
        dialog.setIndeterminate(true);
        dialog.setCancelable(false);
        return dialog;
    }

    @Override
    public void onConfigurationChanged(Configuration conf) {
        super.onConfigurationChanged(conf);
    }

    private void selectFile() {
        Intent intent = new Intent(this, ZGNBManagerMain.class);
        intent.putExtra(ZGNBManagerMain.SELECTEDMOD, true);
        startActivityForResult(intent, ActResConstant.zip_list_item);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        switch(requestCode) {
        case ActResConstant.zip_list_item:
            switch(resultCode) {
            case ActResConstant.add_entry:
                final String name = data.getStringExtra(ZGNBManagerMain.ENTRYPATH);
                new Thread(new Runnable() {
                    public void run() {
                        mHandler.sendEmptyMessage(LOADING);
                        File file = new File(name);
                        byte[] b = null;
                        try {
                            b = FileUtil.readFile(file);
                        } catch(IOException io) {
                        }
                        zipEnties.put(tree.getCurPath() + file.getName(), b);
                        isChanged = true;
                        tree.addNode(file.getName());
                        Message msg = new Message();
                        msg.what = TOAST;
                        msg.obj = getString(R.string.file_added);
                        mHandler.sendMessage(msg);
                        // dismissDialog
                        mHandler.sendEmptyMessage(R.string.load_data);
                        mHandler.sendEmptyMessage(UPDATE);
                    }
                }).start();
                break;
            case ActResConstant.text_editor:
                zipEnties.put(getCurFile(), TextEditor.data);
                isChanged = true;
                mAdapter.notifyDataSetInvalidated();
                TextEditor.data = null;
                toast(getString(R.string.saved));
                System.gc();
                break;
            }
            break;
        }
    }

    public String getCurFile() {
        return tree.getCurPath() + file;
    }

    @Override
    public boolean onContextItemSelected(MenuItem item) {
        AdapterView.AdapterContextMenuInfo info;
        try {
            info = (AdapterView.AdapterContextMenuInfo) item.getMenuInfo();
        } catch(ClassCastException e) {
            Log.e(e.toString(), "Bad menuInfo");
            return false;
        }
        final String name = (String) mAdapter.getItem(info.position);
        int id = item.getItemId();
        switch(id) {
        case R.string.zip_editor_remove:
            ZGNBManagerMain.prompt(this, getString(R.string.is_remove), name, new DialogInterface.OnClickListener() {
                public void onClick(DialogInterface dialog, int which) {
                    if(which == AlertDialog.BUTTON_POSITIVE) {
                        new Thread(new Runnable() {
                            public void run() {
                                mHandler.sendEmptyMessage(REMOVE);
                                if(tree.isDirectory(name))
                                    removeDirectory(name);
                                else
                                    removeFile(name);
                                mHandler.sendEmptyMessage(R.string.zip_remove_progress);
                                tree = new Tree(zipEnties.keySet());
                                mHandler.sendEmptyMessage(UPDATE);
                            }
                        }).start();
                    }
                }
            });
            break;
        case R.string.extract:
            new Thread(new Runnable() {
                public void run() {
                    mHandler.sendEmptyMessage(EXTRACT);
                    try {
                        extract(name);
                    } catch(Exception e) {
                        Message msg = new Message();
                        msg.what = ERROR;
                        msg.obj = e.getMessage();
                        mHandler.sendMessage(msg);
                        // dismissDialog
                        mHandler.sendEmptyMessage(R.string.extract);
                        return;
                    }
                    Message msg = new Message();
                    msg.what = TOAST;
                    msg.obj = getString(R.string.extracted);
                    mHandler.sendMessage(msg);
                    // dismissDialog
                    mHandler.sendEmptyMessage(R.string.extract);
                }
            }).start();
            break;
        }
        return true;
    }

    public void toast(String message) {
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show();
    }

    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {
        if(keyCode == KeyEvent.KEYCODE_BACK) {
            if(!getTitle().equals(title)) {
                mod = BACK;
                mAdapter.notifyDataSetInvalidated();
                return true;
            } else {
                if(isChanged)
                    showDialog();
                else
                    finish();
                return true;
            }
        }
        return super.onKeyDown(keyCode, event);
    }

    private void removeFile(String name) {
        zipEnties.remove(tree.getCurPath() + name);
    }

    private void removeDirectory(String name) {
        Map<String, byte[]> zipEnties = ZipManagerMain.zipEnties;
        Tree tree = this.tree;
        String curr = tree.getCurPath();
        Set<String> keySet = zipEnties.keySet();
        String[] keys = new String[keySet.size()];
        keySet.toArray(keys);
        for(String key : keys) {
            if(key.startsWith(curr + name))
                zipEnties.remove(key);
        }
    }

    private void textEditArsc(String file) {
        try {
            Intent it = new Intent(ZipManagerMain.this, ArscActivity.class);
            it.putExtra("FilePath", file.toString());
            startActivityForResult(it, ActResConstant.list_item_details);
        } catch(Exception e) {
            return;
        }
    }

    private void textEditAxml(String file) {
        byte[] data = readEntry(file);
        TextEditor.data = data;
        Intent intent = new Intent(this, TextEditor.class);
        intent.putExtra(TextEditor.PLUGIN, "AXmlEditor");
        startActivityForResult(intent, ActResConstant.zip_list_item);
        finish();
    }

    private byte[] readEntry(String name) {
        byte[] buf = zipEnties.get(tree.getCurPath() + name);
        if(buf == null)
            return readEntryForZip(tree.getCurPath() + name);
        return buf;
    }

    private void extract(String name) throws Exception {
        String str = zipFile.getName();
        int s = str.lastIndexOf('/');
        int e = str.indexOf('.');
        if(s < e)
            str = str.substring(s, e);
        File outPath = new File(EXTRACTPATH + str);
        Map<String, byte[]> zipEnties = ZipManagerMain.zipEnties;
        String curr = tree.getCurPath();
        curr = tree.isDirectory(name) ? curr + name + "/" : curr + name;
        for(String key : zipEnties.keySet()) {
            if(key.startsWith(curr)) {
                byte[] buf = zipEnties.get(key);
                if(buf != null)
                    ZipExtract.extractEntryForByteArray(buf, key, outPath);
                else {
                    ZipEntry entry = zipFile.getEntry(key);
                    ZipExtract.extractEntry(zipFile, entry, outPath);
                }
            }
        }
    }

    private byte[] readEntryAbsName(String name) {
        byte[] buf = zipEnties.get(name);
        if(buf == null)
            return readEntryForZip(name);
        return buf;
    }

    private ZipEntry getEntry(String name) {
        byte[] buf = zipEnties.get(tree.getCurPath() + name);
        if(buf == null) {
            if(zipFile != null)
                return zipFile.getEntry(tree.getCurPath() + name);
            ZipEntry zipEntry = new ZipEntry(tree.getCurPath() + name);
            zipEntry.setTime(0);
            zipEntry.setSize(0);
            return zipEntry;
        }
        ZipEntry zipEntry = new ZipEntry(tree.getCurPath() + name);
        zipEntry.setTime(System.currentTimeMillis());
        zipEntry.setSize(buf.length);
        return zipEntry;
    }

    private byte[] readEntryForZip(String name) {
        ZipEntry zipEntry = zipFile.getEntry(name);
        if(zipEntry != null) {
            ByteArrayOutputStream baos = new ByteArrayOutputStream(8 * 1024);
            byte[] buf = new byte[4 * 1024];
            try {
                InputStream in = zipFile.getInputStream(zipEntry);
                int count;
                while((count = in.read(buf, 0, buf.length)) != -1)
                    baos.write(buf, 0, count);
                in.close();
                baos.close();
            } catch(IOException io) {
            }
            return baos.toByteArray();
        }
        return null;
    }

    private class FileListAdapter extends BaseAdapter {

        protected final Context mContext;
        protected final LayoutInflater mInflater;

        public FileListAdapter(Context context) {
            mContext = context;
            mInflater = (LayoutInflater) mContext.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        }

        public int getCount() {
            return fileList.size();
        }

        public Object getItem(int position) {
            return fileList.get(position);
        }

        public long getItemId(int position) {
            return position;
        }

        public View getView(int position, View convertView, ViewGroup parent) {
            String file = fileList.get(position);
            RelativeLayout container;
            if(convertView == null)
                container = (RelativeLayout) mInflater.inflate(R.layout.zip_list_item, null);
            else
                container = (RelativeLayout) convertView;
            ImageView icon = (ImageView) container.findViewById(R.id.icon);
            String name = file.toLowerCase();
            if(tree.isDirectory(file))
                icon.setImageResource(R.drawable.folder);
            else if(name.endsWith(".apk"))
                icon.setImageResource(R.drawable.android);
            else if(name.endsWith(".png") || name.endsWith(".jpg"))
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
                    || name.endsWith(".cs") || name.endsWith(".h") || name.endsWith(".hpp") || name.endsWith(".java"))
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
            text.setText(file);
            perm.setText("");
            if(!tree.isDirectory(file)) {
                ZipEntry zipEntry = getEntry(file);
                Date date = new Date(zipEntry.getTime());
                SimpleDateFormat format = new SimpleDateFormat("yy-MM-dd HH:mm:ss");
                time.setText(format.format(date));
                size.setText(convertBytesLength(zipEntry.getSize()));
            } else {
                time.setText("");
                size.setText("");
            }
            return container;
        }

        private String convertBytesLength(long len) {
            if(len < 1024)
                return len + "B";
            if(len < 1024 * 1024)
                return String.format("%.2f%s", (len / 1024.0), "K");
            if(len < 1024 * 1024 * 1024)
                return String.format("%.2f%s", (len / (1024 * 1024.0)), "M");
            return String.format("%.2f%s", (len / (1024 * 1024 * 1024.0)), "G");
        }

    }

    static class Tree {
        List<Map<String, String>> node;
        Comparator<String> sortByType = new Comparator<String>() {
            public int compare(String a, String b) {
                if(isDirectory(a) && !isDirectory(b))
                    return -1;
                if(!isDirectory(a) && isDirectory(b))
                    return 1;
                return a.toLowerCase().compareTo(b.toLowerCase());
            }
        };

        public Tree(Set<String> names) {
            if(path == null) {
                path = new Stack<String>();
                dep = 0;
            }
            HashMap<String, byte[]> zipEnties = ZipManagerMain.zipEnties;
            node = new ArrayList<Map<String, String>>();
            for(String name : names) {
                String[] token = name.split("/");
                String tmp = "";
                for(int i = 0, len = token.length; i < len; i++) {
                    String value = token[i];
                    if(i >= node.size()) {
                        Map<String, String> map = new HashMap<String, String>();
                        if(zipEnties.containsKey(tmp + value) && i + 1 == len)
                            map.put(tmp + value, tmp);
                        else
                            map.put(tmp + value + "/", tmp);
                        node.add(map);
                        tmp += value + "/";
                    } else {
                        Map<String, String> map = node.get(i);
                        if(zipEnties.containsKey(tmp + value) && i + 1 == len)
                            map.put(tmp + value, tmp);
                        else
                            map.put(tmp + value + "/", tmp);
                        tmp += value + "/";
                    }
                }
            }
        }

        private List<String> list(String parent) {
            Map<String, String> map = null;
            List<String> str = new ArrayList<String>();
            while(dep >= 0 && node.size() > 0) {
                map = node.get(dep);
                if(map != null)
                    break;
                pop();
            }
            if(map == null)
                return str;
            for(String key : map.keySet()) {
                if(parent.equals(map.get(key))) {
                    int index;
                    if(key.endsWith("/"))
                        index = key.lastIndexOf("/", key.length() - 2);
                    else
                        index = key.lastIndexOf("/");
                    if(index != -1)
                        key = key.substring(index + 1);
                    str.add(key);
                    // Log.e("tree",key);
                }
            }
            Collections.sort(str, sortByType);
            return str;
        }

        public void addNode(String name) {
            Map<String, String> map = node.get(dep);
            map.put(getCurPath() + name, getCurPath());
        }

        public void deleteNode(String name) {
            Map<String, String> map = node.get(dep);
            map.remove(getCurPath() + name);
        }

        public List<String> list() {
            return list(getCurPath());
        }

        public void push(String name) {
            dep++;
            path.push(name);
        }

        public String pop() {
            if(dep > 0) {
                dep--;
                return path.pop();
            }
            return null;
        }

        public String getCurPath() {
            // Log.e("tree Curpath",join(path,"/"));
            return join(path, "/");
        }

        public boolean isDirectory(String name) {
            return name.endsWith("/");
        }

        private String join(Stack<String> stack, String d) {
            StringBuilder sb = new StringBuilder("");
            for(String s : stack)
                sb.append(s);
            return sb.toString();
        }

    }

    private static void readZip(ZipFile zip, Map<String, byte[]> map) throws IOException {
        Enumeration<? extends ZipEntry> enums = zip.entries();
        while(enums.hasMoreElements()) {
            ZipEntry entry = (ZipEntry) enums.nextElement();
            if(!entry.isDirectory())
                map.put(entry.getName(), null);
        }
    }

    public static void zip(ZipFile zipFile, Map<String, byte[]> map, File file) throws IOException {
        FileOutputStream out = new FileOutputStream(file);
        ZipOutputStream zos = new ZipOutputStream(out);
        byte[] buf = new byte[10 * 1024];
        for(String key : map.keySet()) {
            byte[] data = map.get(key);
            if(data != null) {
                ZipEntry zipEntry = new ZipEntry(key);
                zipEntry.setSize(data.length);
                zipEntry.setTime(System.currentTimeMillis());
                zos.putNextEntry(zipEntry);
                zos.write(data);
            } else {
                ZipEntry zipEntry = zipFile.getEntry(key);
                if(zipEntry != null) {
                    InputStream in = zipFile.getInputStream(zipEntry);
                    zos.putNextEntry(zipEntry);
                    int count;
                    while((count = in.read(buf, 0, buf.length)) != -1)
                        zos.write(buf, 0, count);
                }
            }
            zos.flush();
        }
        zos.close();
    }
}
