package com.example.hara.myapplication;/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

/**
 * @author hara
 */

import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.AsyncTask;
import android.os.Environment;
import android.os.NetworkOnMainThreadException;
import android.support.v7.app.NotificationCompat;
import android.widget.Toast;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLConnection;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * @author hara
 */
public class ImgDL extends AsyncTask<String, Integer, Integer> {

    /**
     * @param args the command line arguments
     */
    private static Pattern img_p = Pattern.compile("(https?://|//|/)[a-zA-Z_0-9/:%#¥$&¥?¥(¥)~¥.=¥+¥-]+[.](jpg|png|gif|jpeg)");
    private static Pattern atag_p = Pattern.compile("<a.+?>");
    private static Pattern htmlurl = Pattern.compile("(https?://|//|/)[a-zA-Z_0-9/:%#¥$&¥?¥(¥)~¥.=¥+¥-]+");
    private static Pattern protocol = Pattern.compile("^https?:");
    private static Pattern domain = Pattern.compile("^https?://[a-zA-Z_0-9:%#¥$&¥?¥(¥)~¥.=¥+¥-]+");
    String WorkingDirectory;
    String DownloadUrl;
    private Context context;
    private ProgressDialog progressdialog;
    static int NotiID = 0;
    static int NotProgressID = 100;
    private int npID;
    private NotificationCompat.Builder p_builder = null;
    NotificationManager nm;


    public ImgDL(String StrUrl, Context context) {
        DownloadUrl = StrUrl;
        this.context = context;
        this.npID = NotProgressID;
        NotProgressID++;
    }


    //webサイトurlからhtmlを取得し、atagを取り出す
    private String[] GetAtags(String str) {
        try {
            URL url = new URL(str);
            URLConnection urlCon = url.openConnection();
            urlCon.setRequestProperty("User-Agent", "Mozilla/5.0");
            Object content = urlCon.getContent();
            if (content instanceof InputStream) {
                BufferedReader br = new BufferedReader(new InputStreamReader((InputStream) content));
                List<String> atagList = new ArrayList<>();
                String line;
                Matcher m1;

                while ((line = br.readLine()) != null) {
                    m1 = atag_p.matcher(line);
                    while (m1.find()) {
                        atagList.add(m1.group());
                    }
                }
                String[] atagArray = (String[]) atagList.toArray(new String[0]);
                return atagArray;
            } else {
                return null;
            }
        } catch (MalformedURLException e) {
            return null;
        } catch (IOException e) {
            return null;
        } catch (NetworkOnMainThreadException e) {
            return null;
        }
    }

    private String[] GetHtmlUrls(String[] atagArray){
        if (atagArray == null){
            return null;
        }
        Matcher m;
        List<String> htmlUrlList = new ArrayList<>();
        for (String str : atagArray){
            m = htmlurl.matcher(str);
            while(m.find()){
                htmlUrlList.add(m.group());
            }
        }
        String[] htmlUrlArray = (String[]) htmlUrlList.toArray(new String[0]);

        if (htmlUrlArray.length == 0){
            return null;
        }
        return htmlUrlArray;
    }

    //atagArrayから画像urlを取得(mode 1:繰り返す 2:繰り返さない)
    private String[] GetImgUrls(String[] htmlArray, int mode) {
        if (htmlArray == null) {
            return null;
        }
        Matcher m;
        List<String> imgUrlList = new ArrayList<>();
        String[] imgUrlArray;
        for (String str : htmlArray) {
            m = img_p.matcher(str);
            while (m.find()) {
                imgUrlList.add(m.group());
            }
        }
        imgUrlArray = (String[]) imgUrlList.toArray(new String[0]);

        if (imgUrlArray.length == 0 && mode == 1) {
            for (String html : htmlArray) {
                m = domain.matcher(DownloadUrl);
                if(m.find() && html.startsWith(m.group())){
                    continue;
                }
                String[] Atags, imgurls;
                if ((Atags = GetAtags(html)) != null) {
                    if ((imgurls = GetImgUrls(Atags, 2)) != null) {
                        for (String imgurl : imgurls) {
                            System.out.println(imgurl);
                            imgUrlList.add(imgurl);
                        }
                    }
                }
            }
            imgUrlArray = (String[]) imgUrlList.toArray(new String[0]);
        }
        if(imgUrlArray.length == 0){
            return null;
        }
        return imgUrlArray;
    }

    //urlが省略されていないかチェック
    private String[] checkUrls(String[] urlArray){
        if (urlArray == null){
            return null;
        }
        Matcher m;
        List<String> ckdUrlList = new ArrayList<>();
        for (String str : urlArray){
            if (str.startsWith("//")){
                m = protocol.matcher(DownloadUrl);
                if (m.find()){
                    ckdUrlList.add(m.group() + str);
                }
            }else if (str.startsWith("/")){
                m = domain.matcher((DownloadUrl));
                if (m.find()) {
                    ckdUrlList.add(m.group() + str);
                }
            }else{
                ckdUrlList.add(str);
            }
        }
        String[] imgUrls = (String[]) ckdUrlList.toArray(new String[0]);
        return imgUrls;
    }

    //ダウンロードするファイル名の取得
    private String GetFileName(String fileName) {
        if (fileName == null) {
            return null;
        }
        int point = fileName.lastIndexOf("/");
        if (point != -1) {
            return fileName.substring(point + 1);
        }
        return fileName;
    }

    //画像保存用フォルダの作成
    private String MakeDir() {
        int number = 1;
        String pathname =
                Environment.getExternalStorageDirectory().getPath() + "/imgdls";

        while (true) {
            String dirname = "img" + number;
            File file = new File(pathname + "/" + dirname);
            if (file.exists()) {
                number++;
            } else {
                if(file.mkdirs()) {
                    System.out.println("sucsess");
                }
                return file.getAbsolutePath();
            }
        }
    }

    //画像のダウンロード
    private int GetImgs(String[] ImgUrlArray) {
        int dlSize = ImgUrlArray.length;
        int pcount = 0;
        WorkingDirectory = MakeDir();

        for (String ImgUrl : ImgUrlArray) {
            if (isCancelled()) {
                break;
            }

            try {
                URL url = new URL(ImgUrl);
                URLConnection urlCon = url.openConnection();
                urlCon.setRequestProperty("User-Agent", "Mozilla/5.0");
                String fileName = GetFileName(url.getFile());
                FileOutputStream fos = new FileOutputStream(WorkingDirectory + "/" + fileName);
                InputStream in = urlCon.getInputStream();
                int read;
                byte[] buf = new byte[8192];
                while ((read = in.read(buf)) != -1) {
                    fos.write(buf, 0, read);
                }
                publishProgress(1);
                pcount++;
                if (p_builder != null){
                    p_builder.setProgress(dlSize, pcount, false);
                    p_builder.setContentText(context.getString(R.string.downloading) + pcount + "/" + dlSize);
                    nm.notify(npID, p_builder.build());
                }
                fos.close();
                in.close();
            } catch (FileNotFoundException ex) {
                System.out.println("file error");
                return -1;
            } catch (IOException ex) {
                System.out.println("IO error");
                return -1;
            }
        }
        return 1;
    }

    protected void onPreExecute() {
        nm = (NotificationManager) context.getSystemService(context.NOTIFICATION_SERVICE);

        progressdialog = new ProgressDialog(context);
        progressdialog.setTitle(context.getString(R.string.downloading));
        progressdialog.setMessage(DownloadUrl);
        progressdialog.setProgress(0);
        progressdialog.setProgressStyle(ProgressDialog.STYLE_HORIZONTAL);
        progressdialog.setIndeterminate(false);
        progressdialog.setCancelable(false);
        progressdialog.setButton(DialogInterface.BUTTON_NEGATIVE, "OK",
                new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int which) {
                        push_OK();
                    }
                });
        progressdialog.setButton(DialogInterface.BUTTON_POSITIVE, "Cancel",
                new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int which) {
                        progressdialog.dismiss();
                        cancel();
                    }
                });
        progressdialog.show();
    }

    void push_OK(){
        p_builder = new NotificationCompat.Builder(context);
        p_builder.setSmallIcon(R.mipmap.ic_launcher);
        p_builder.setTicker(context.getString(R.string.app_name));
        p_builder.setContentTitle(context.getString(R.string.app_name));
        p_builder.setContentText("待機中..."+ DownloadUrl);
        p_builder.setWhen(System.currentTimeMillis());
        nm.notify(npID, p_builder.build());
        progressdialog.dismiss();
    }

    void cancel() {
        this.cancel(true);
    }

    protected Integer doInBackground(String... params) {
        String[] imgurlArray, htmlurlArray, atagArray;
        if ((atagArray = GetAtags(DownloadUrl)) == null) {
            return -1;
        }
        if ((htmlurlArray = GetHtmlUrls(atagArray)) == null) {
            return -2;
        }
        if ((imgurlArray = GetImgUrls(htmlurlArray, 2)) == null) {
            return -2;
        }
        progressdialog.setMax(imgurlArray.length);
        imgurlArray = checkUrls(imgurlArray);
        if(GetImgs(imgurlArray) != 1){
            return -3;
        }
        return 1;
    }

    protected void onProgressUpdate(Integer... states) {
        progressdialog.incrementProgressBy(states[0]);
    }

    protected void onCancelled() {
        Toast.makeText(context, context.getText(R.string.cancelled), Toast.LENGTH_SHORT).show();
    }

    protected void onPostExecute(Integer result) {
        progressdialog.dismiss();
        switch (result) {
            case 1:
                Toast.makeText(context, context.getText(R.string.complete_massage), Toast.LENGTH_SHORT).show();
                //ステータスバー通知セット
                Intent nIntent = new Intent(Intent.ACTION_VIEW)
                        .setType("image/*");
                PendingIntent contentIntent = PendingIntent.getActivity(context, 0, nIntent, 0);
                Notification n = new NotificationCompat.Builder(context)
                        .setSmallIcon(R.drawable.icon_162982_512)
                        .setTicker(context.getText(R.string.complete))
                        .setWhen(System.currentTimeMillis())
                        .setContentTitle(context.getText(R.string.complete_massage))
                        .setContentText(DownloadUrl)
                        .setContentIntent(contentIntent)
                        .build();
                nm.notify(NotiID++, n);
                break;
            case -1:
                Toast.makeText(context, context.getString(R.string.invalid_url_massage), Toast.LENGTH_LONG).show();
                break;
            case -2:
                Toast.makeText(context, context.getString(R.string.no_image_massage), Toast.LENGTH_LONG).show();
                break;
            case -3:
                Toast.makeText(context, context.getString(R.string.error_massage), Toast.LENGTH_LONG).show();
                break;
        }
        if(p_builder != null){
            nm.cancel(npID);
        }
    }

}
