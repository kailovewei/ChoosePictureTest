package ykk.picture.com.choosepicturetest;

import android.annotation.TargetApi;
import android.content.ContentUris;
import android.content.Intent;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.provider.DocumentsContract;
import android.provider.MediaStore;
import android.support.annotation.RequiresApi;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.Toast;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;

public class MainActivity extends AppCompatActivity {
    private static final int TAKE_PHOTO=1;
    private  static final int CROP_PHOTO=2;
    private static final int CHOOSE_PHOTO=3;
    private Button takephoto;
    private ImageView picture;
    private Uri imageUri;
    private  Button chooseFromAlbum;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        takephoto= (Button) findViewById(R.id.take_photo);
        picture= (ImageView) findViewById(R.id.picture);
        chooseFromAlbum= (Button) findViewById(R.id.choose_from_album);
        //从相册中获取照片
        chooseFromAlbum.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent=new Intent("android.intent.action.GET_CONTENT");
                intent.setType("image/*");
                startActivityForResult(intent,CHOOSE_PHOTO);//打开相册

            }
        });
        //使用相机进行拍照.
        takephoto.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                //创建File对象，用于存储拍照后的图片。
                //调用Environment.getExternalStorageDirectory()方法取到手机SD卡的根目录。
                File outputImage=new File(Environment.getExternalStorageDirectory(),"output_image.jpg");
                if(outputImage.exists())
                {
                    outputImage.delete();
                }
                try {
                    outputImage.createNewFile();
                } catch (IOException e) {
                    e.printStackTrace();
                }
                //调用Uri.fromFile()方法将File对象转换成Uri对象
                imageUri=Uri.fromFile(outputImage);
                Intent intent=new Intent("android.media.action.IMAGE_CAPTURE");
                //指定图片的输出地址
                intent.putExtra(MediaStore.EXTRA_OUTPUT,imageUri);
                /*
                startActivityForResult的主要作用就是它可以回传数据，
                假设我们有两个页面，首先进入第一个页面，里面有一个按钮，
                用于进入下一个页面，当进入下一个页面时，进行设置操作，
                并在其finish()动作或者back动作后，将设置的值回传给第一个页面，
                从而第一个页面来显示所得到的值。这个有一点像回调方法，
                就是在第二个页面finish()动作或者back动作后，会回调第一个页面的onActivityResult()方法
                 */
                startActivityForResult(intent,TAKE_PHOTO);//启动相机程序
            }
        });
    }

    @Override
    //第二个参数是secondActivity传回来的键，第三个参数是secondActivity传回来的值。
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        //　第一个参数：这个整数requestCode用于与startActivityForResult中的requestCode中值进行比较判断，
        // 是以便确认返回的数据是从哪个Activity返回的。
        switch (requestCode)
        {
            case TAKE_PHOTO:
                if(resultCode==RESULT_OK)
                {
                    Intent intent=new Intent("com.android.camera.action.CROP");
                    intent.setDataAndType(imageUri,"image/*");
                    intent.putExtra("scale",true);
                    intent.putExtra(MediaStore.EXTRA_OUTPUT,imageUri);
                    startActivityForResult(intent,CROP_PHOTO);//启动裁剪程序
                }
                break;
            case CROP_PHOTO:
                if(resultCode==RESULT_OK)
                {
                    try {
                        //如果想要访问内容提高器中的共享的数据，就需要借助ContentResolver类，
                        //通过getContentResolver()方法获取到该类的实例.
                        Bitmap bitmap= BitmapFactory.decodeStream(getContentResolver().openInputStream(imageUri));
                        picture.setImageBitmap(bitmap);//将裁剪后的照片显示出来。
                    } catch (FileNotFoundException e) {
                        e.printStackTrace();
                    }
                }
                break;
            case CHOOSE_PHOTO:
                if(resultCode==RESULT_OK)
                {
                    //判断手机系统版本号
                    if(Build.VERSION.SDK_INT>=Build.VERSION_CODES.KITKAT)
                    {
                        //4.4及以上使用此方法处理图片,需要解析封装过的Uri.
                        //目的是需要获取图片的真实路径。
                        handleImageOnKitKat(data);

                    }
                    else
                    {
                        //4.4以下使用此方法处理图片，直接可以得到图片的真实路径。
                        handleImageBeforeKitKat(data);
                        Log.d("handleImageBeforeKitKat","<19");
                    }
                }
                break;
            default:
                break;
        }
    }
//  <4.4
    private void handleImageBeforeKitKat(Intent data) {
        Uri uri=data.getData();
        String imagePath=getImagePath(uri,null);
        displayImage(imagePath);
    }
//  >=4.4
    @TargetApi(Build.VERSION_CODES.KITKAT)
    //Android4.4以上获取路径的方法
    private void handleImageOnKitKat(Intent data) {
        String imagePath=null;
        Uri uri=data.getData();
        if(DocumentsContract.isDocumentUri(this,uri)) {
            //如果是document类型的uri,则通过document id处理
            String docId = DocumentsContract.getDocumentId(uri);
            if ("com.android.providers.media.documents".equals(uri.getAuthority())) {
                String id = docId.split(":")[1];//解析出数字格式的Id
                String selection = MediaStore.Images.Media._ID + "=" + id;
                imagePath = getImagePath(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, selection);
            } else if ("com.android.provider.downloads.documents".equals(uri.getAuthority())) {
                Uri contentUri = ContentUris.withAppendedId(uri.parse("content://downloads/public_downloads"), Long.valueOf(docId));
                imagePath = getImagePath(contentUri, null);
            }
        }
            else if("content".equalsIgnoreCase(uri.getScheme()))
            {
                //如果不是document类型的Uri，则使用普通方式处理.
                imagePath=getImagePath(uri,null);
            }
            displayImage(imagePath);//根据图片的路径显示图片
    }

    private void displayImage(String imagePath) {
        if(imagePath!=null)
        {
            Bitmap bitmap=BitmapFactory.decodeFile(imagePath);
            picture.setImageBitmap(bitmap);
        }
        else
        {
            Toast.makeText(this,"failed to get image",Toast.LENGTH_SHORT).show();
        }
    }

    private String getImagePath(Uri uri, String selection) {
        String path=null;
        //通过Uri和selection来获取真实的图片路径
        Cursor cursor=getContentResolver().query(uri,null,selection,null,null);
        if(cursor!=null)
        {
            if(cursor.moveToFirst())
            {
                path=cursor.getString(cursor.getColumnIndex(MediaStore.Images.Media.DATA));
            }
            cursor.close();
        }
        return path;
    }
}
