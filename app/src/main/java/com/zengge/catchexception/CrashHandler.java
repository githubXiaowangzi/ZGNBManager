/*package com.zengge.catchexception;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.io.Writer;
import java.lang.Thread.UncaughtExceptionHandler;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

import com.zengge.nbmanager.Features;

import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager.NameNotFoundException;
import android.net.Uri;
import android.os.Environment;
import android.os.Looper;
import android.util.Log;
import android.view.WindowManager;
import android.widget.Toast;
import android.content.pm.*;
import com.zengge.nbmanager.*;

public class CrashHandler implements UncaughtExceptionHandler
{
    private Thread.UncaughtExceptionHandler mDefaultHandler = null;// 系统默认的UncaughtException处理类
    private static CrashHandler INSTANCE;// CrashHandler实例
    private Map<String, String> info = new HashMap<String, String>();// 用来存储设备信息和异常信息
    private Context mContext;// 程序的Context对象
    private SimpleDateFormat format = new SimpleDateFormat(
        "yyyy-MM-dd-HH-mm-ss");// 用于格式化日期,作为日志文件名的一部分
    static final String normalerror = "悲剧鸟，程序出现错误已停止运行，错误日志文件保存在SD卡crash目录下。请将此错误报告发送给我，以便我尽快修复此问题，谢谢合作！\n";

    private CrashHandler()
    {
        Log.i("CrashHandler", "捕获器已启动");
    }

    public static CrashHandler getInstance()
    {
        if (INSTANCE == null)
            INSTANCE = new CrashHandler();
        return INSTANCE;
    }

    public void init(Context context)
    {
        mContext = context;
        if (mDefaultHandler != null)
        {
            Toast.makeText(mContext, "异常捕获器已被初始化", Toast.LENGTH_LONG);
        }
        mDefaultHandler = Thread.getDefaultUncaughtExceptionHandler();// 获取系统默认的UncaughtException处理器
        Thread.setDefaultUncaughtExceptionHandler(this);// 设置该CrashHandler为程序的默认处理器
    }

    public void uncaughtException(Thread thread, Throwable ex)
    {
        Context ctx = null;
        if (!handleException(ex) && mDefaultHandler != null)
        {
            // 自定义的没有处理，用系统默认的异常处理器
            Toast.makeText(ctx, "本异常捕获SDK未能捕获异常，操作系统即将抛出异常并捕获",
                           Toast.LENGTH_SHORT).show();
            try
            {
                Thread.sleep(4000);
            }
            catch (Exception e)
            {

            }
            mDefaultHandler.uncaughtException(thread, ex);
        }
    }
    public boolean handleException(final Throwable ex)
    {
        if (ex == null || mContext == null)
            return false;
        final String crashReport = getCrashReport(mContext, ex);
        Log.i("出现异常!", crashReport);
        new Thread()
        {
            public void run()
            {
                Looper.prepare();
                File fileName = save2File(crashReport, ex);
                sendAppCrashReport(mContext, crashReport, fileName);
                Looper.loop();
            }

        } .start();
        return true;
    }

    private File save2File(String crashReport, Throwable ex)
    {
        // TODO Auto-generated method stub
        StringBuffer sb = new StringBuffer();
        for (Map.Entry<String, String> entry : info.entrySet())
        {
            String key = entry.getKey();
            String value = entry.getValue();
            sb.append(key + "=" + value + "\r\n");
        }
        Writer writer = new StringWriter();
        PrintWriter pw = new PrintWriter(writer);
        ex.printStackTrace(pw);
        Throwable cause = ex.getCause();
        // 循环着把所有的异常信息写入writer中
        while (cause != null)
        {
            cause.printStackTrace(pw);
            cause = cause.getCause();
        }
        pw.close();// 记得关闭
        String result = writer.toString();
        sb.append(result);
        // 保存文件
        long timetamp = System.currentTimeMillis();
        String time = format.format(new Date());
        String fileName = "crash-" + time + "-" + timetamp + ".txt";
        if (Environment.getExternalStorageState().equals(
                    Environment.MEDIA_MOUNTED))
        {
                File dir = new File(Environment.getExternalStorageDirectory()
                                    .getAbsolutePath() + File.separator + "crash");
                Log.i("CrashHandler", dir.toString());
                if (!dir.exists())
                    dir.mkdir();
                Features.printLog(dir + File.separator + fileName, sb.toString(), false);
                return null;

        }
        return null;
    }

    private void sendAppCrashReport(final Context context,
                                    final String crashReport, final File file)
    {
        // TODO Auto-generated method stub
        AlertDialog mDialog;
        AlertDialog.Builder builder = new AlertDialog.Builder(context);

        builder.setTitle("程序废鸟！");
        builder.setCancelable(false);// 不设置此项或设置为true，当用户按返回键时会造成程序卡死！
        builder.setMessage(normalerror
                           + "以下是错误信息（由于android系统框架限制，只能显示部分内容，详细信息见SD卡crash目录下的文件）：\n\n"
                           + crashReport);
        builder.setPositiveButton("发送报告",
                                  new DialogInterface.OnClickListener()
        {
            public void onClick(DialogInterface dialog, int which)
            {

                try
                {
                    Intent intent = new Intent(Intent.ACTION_SEND);
                    intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                    String[] tos = { "2470196889@qq.com" };
                    intent.putExtra(Intent.EXTRA_EMAIL, tos);

                    intent.putExtra(Intent.EXTRA_SUBJECT, "错误报告");
                    if (file != null)
                    {
                        intent.putExtra(Intent.EXTRA_STREAM,
                                        Uri.fromFile(file));
                        intent.putExtra(Intent.EXTRA_TEXT, normalerror);
                    }
                    else
                    {
                        intent.putExtra(Intent.EXTRA_TEXT, normalerror
                                        + "以下是错误信息：\n" + crashReport);
                    }
                    intent.setType("text/plain");
                    intent.setType("message/rfc882");
                    Intent.createChooser(intent, "选择一个邮件客户端");
                    context.startActivity(intent);
                }
                catch (Exception e)
                {
                    Toast.makeText(context, "您没安装邮件客户端。",
                                   Toast.LENGTH_SHORT).show();
                }
                finally
                {
                    dialog.dismiss();
                    System.exit(1);
                }
            }
        });
        builder.setNegativeButton("强制关闭",
                                  new DialogInterface.OnClickListener()
        {
            public void onClick(DialogInterface dialog, int which)
            {
                dialog.dismiss();
                System.exit(1);
            }
        });
        builder.setNeutralButton("忽略", null);
        mDialog = builder.create();
        mDialog.getWindow().setType(
            WindowManager.LayoutParams.TYPE_SYSTEM_ALERT);
        mDialog.show();
    }

    private String getCrashReport(Context context, Throwable ex)
    {

        PackageInfo pinfo = getPackageInfo(context);
        StringBuffer exceptionStr = new StringBuffer();
        try
        {
            exceptionStr.append("应用版本: " + pinfo.versionName + "("
                                + pinfo.versionCode + ")\n");
            exceptionStr.append("安装时间:" + pinfo.firstInstallTime + "\n");
            exceptionStr.append("应用包名: " + pinfo.packageName + "\n");
            exceptionStr.append("Android 版本: "
                                + android.os.Build.VERSION.RELEASE + "\n");
            exceptionStr.append("手机型号: " + android.os.Build.MODEL + "\n");
            exceptionStr.append("制造商: " + android.os.Build.MANUFACTURER + "\n");
			exceptionStr.append("CPU类型： " + android.os.Build.CPU_ABI + "\n");
            exceptionStr.append("异常代号: " + ex.toString() + "\n" + "详细信息:"
                                + ex.getMessage() + "\n");
            StackTraceElement[] elements = ex.getStackTrace();
            for (int i = 0; i < elements.length; i++)
                exceptionStr.append("at " + elements[i].toString() + "\n");
        }
        catch (Exception e)
        {
            e.printStackTrace();
        }
        return exceptionStr.toString();
    }

    private PackageInfo getPackageInfo(Context context)
    {
        PackageInfo info = null;
        try
        {
            info = context.getPackageManager().getPackageInfo(
                       context.getPackageName(), 0);
        }
        catch (NameNotFoundException e)
        {
        }
        if (info == null)
            info = new PackageInfo();
        return info;
    }
}
*/