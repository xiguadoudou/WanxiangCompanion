package com.doudou.wxq;
import android.content.*;import android.net.Uri;import android.os.*;import android.provider.MediaStore;import java.io.*;import java.util.Base64;
final class LocalPngExporter {
 static void save(Context c,String value)throws Exception{
  final String prefix="data:image/png;base64,";if(value==null||!value.startsWith(prefix)||value.length()>12000000)throw new IllegalArgumentException("导出图片格式或大小不受支持");byte[] b=Base64.getDecoder().decode(value.substring(prefix.length()));if(b.length<8||b[0]!=(byte)137||b[1]!=80||b[2]!=78||b[3]!=71)throw new IllegalArgumentException("图片数据无效");String name="Wanxiang-"+System.currentTimeMillis()+".png";
  if(Build.VERSION.SDK_INT>=29){ContentValues v=new ContentValues();v.put(MediaStore.Images.Media.DISPLAY_NAME,name);v.put(MediaStore.Images.Media.MIME_TYPE,"image/png");v.put(MediaStore.Images.Media.RELATIVE_PATH,Environment.DIRECTORY_PICTURES+"/万象弈伴");v.put(MediaStore.Images.Media.IS_PENDING,1);Uri uri=c.getContentResolver().insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI,v);if(uri==null)throw new IOException("图库未接受图片");try{try(OutputStream out=c.getContentResolver().openOutputStream(uri)){if(out==null)throw new IOException("图片无法写入");out.write(b);}v.clear();v.put(MediaStore.Images.Media.IS_PENDING,0);c.getContentResolver().update(uri,v,null,null);}catch(Exception e){c.getContentResolver().delete(uri,null,null);throw e;}}
  else{File folder=c.getExternalFilesDir(Environment.DIRECTORY_PICTURES);if(folder==null)throw new IOException("图片目录不可用");folder.mkdirs();File f=new File(folder,name);try(FileOutputStream out=new FileOutputStream(f)){out.write(b);}android.media.MediaScannerConnection.scanFile(c,new String[]{f.getPath()},new String[]{"image/png"},null);}
 }
}
