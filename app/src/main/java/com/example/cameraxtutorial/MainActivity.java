package com.example.cameraxtutorial;

//this program is based on 0309's data



import androidx.appcompat.app.AppCompatActivity;

import android.content.res.AssetFileDescriptor;
import android.media.AudioManager;
import android.media.MediaPlayer;
import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.camera.core.Camera;
import androidx.camera.core.CameraSelector;
import androidx.camera.core.ImageAnalysis;
import androidx.camera.core.ImageProxy;
import androidx.camera.core.Preview;
import androidx.camera.lifecycle.ProcessCameraProvider;
import androidx.camera.view.PreviewView;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.lifecycle.LifecycleOwner;

import android.content.Context;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.util.Log;
import android.view.MotionEvent;
import android.view.Surface;
import android.view.View;
import android.view.inputmethod.InputMethodManager;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.google.common.util.concurrent.ListenableFuture;

import org.opencv.android.Utils;
import org.opencv.core.Core;
import org.opencv.core.CvType;
import org.opencv.core.Mat;
import org.opencv.core.Rect;
import org.opencv.core.Scalar;
import org.opencv.imgproc.Imgproc;

import java.io.IOException;
import java.io.PrintWriter;
import java.net.Socket;
import java.net.UnknownHostException;
import java.nio.ByteBuffer;
import java.text.BreakIterator;
import java.util.Arrays;
import java.util.Random;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;


public class MainActivity extends AppCompatActivity implements View.OnClickListener {
    /*** Fixed values ***/
    private static final String TAG = "MyApp";
    private int REQUEST_CODE_FOR_PERMISSIONS = 1234;;
    private final String[] REQUIRED_PERMISSIONS = new String[]{"android.permission.CAMERA", "android.permission.WRITE_EXTERNAL_STORAGE"};

    /*** Views ***/
    private PreviewView previewView;
    private ImageView imageView;
    private TextView textView1;
    private TextView textView2;
    private EditText editText;
    private Button button1;
    private Button button2;
    private Button button3;




    String message;
    /*** For CameraX ***/
    private Camera camera = null;
    private Preview preview = null;

    int number;
    int color_id;

    double[] B_matrix=new double[6400];
    double[] G_matrix=new double[6400];
    double[] R_matrix=new double[6400];

    double color_B;
    double color_G;
    double color_R;
    int state;//現在の色
    String IP_address;
    int speed;//UI b 速さ
    int in_course;//UI d コース内外
    int msg_score;//UI e score
    int start_count;
    int score;//得点　減算式にしようかなというお気持ち
    int minus_points_speed_high;//スピードオーバー時の減点is何点
    int minus_points_speed_low;//スピードオーバー時の減点is何点
    int minus_points_course;//コースアウト時の減点is何点

    int gas_leak_points;//ガス漏れに対処したときの加点is何点
    int now_game;//UI a スタート・ゴール・プレイ中
    int order_flag;//正しい順番なら0　異常なら1
    int[] yellow_tape=new int[200];//黄色テープの出現回数を記録するやつ　まだ→0 通った→1　ガス漏れ→2
    int[] orange_tape=new int[200];//オレンジテープの出現回数を記録するやつ
    int[] blue_tape=new int[200];//水色テープの出現回数を記録するやつ
    int[] color_numbers=new int[3];//何回その色が出たか記録
    int prev; //前フレームの色
    int count; //順路同じ色の継続回数
    int out_course_count;
    int gas_flag;//ガス漏れ位置なら1　それ以外で0
    int index;
    double std;
    int messag_gas;
    int IP_flag;
    private ImageAnalysis imageAnalysis = null;
    private ExecutorService cameraExecutor = Executors.newSingleThreadExecutor();

    // left top position
    private int x_pos = 160;
    private int y_pos = 100;
    InputMethodManager inputMethodManager;
    private LinearLayout mainLayout;

    public MediaPlayer mediaPlayer;

    static {
        System.loadLibrary("opencv_java4");
    }





    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        previewView = findViewById(R.id.previewView);
        imageView = findViewById(R.id.imageView);

        inputMethodManager = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
        mainLayout = (LinearLayout) findViewById(R.id.main_layout);
        editText = findViewById(R.id.edit_text);
        editText.setText("192.168.86.39");

        //button = findViewById(R.id.button);
        findViewById(R.id.button1).setOnClickListener(this);
        findViewById(R.id.button2).setOnClickListener(this);
        findViewById(R.id.button3).setOnClickListener(this);
        IP_flag=0;
        IP_address="192.168.86.38";
        textView1 = findViewById(R.id.textView1);
        textView2 = findViewById(R.id.textView2);

        start_process();



        if (checkPermissions()) {
            startCamera();
        } else {
            ActivityCompat.requestPermissions(this, REQUIRED_PERMISSIONS, REQUEST_CODE_FOR_PERMISSIONS);
        }
    }




    void start_process(){
        //初期化処理　全部0で埋める

        Arrays.fill(yellow_tape, 0);
        Arrays.fill(orange_tape, 0);
        Arrays.fill(blue_tape, 0);
        Arrays.fill(color_numbers, 0);
        count=0;
        gas_flag=0;

        //ガス漏れ位置を決定する
        Random r = new Random();
        number=r.nextInt(7)+3;//番号 ガス漏れ位置を考慮 id 3-9だと良さげ
        color_id=r.nextInt(3);//色

        if(color_id==0){
            yellow_tape[number]=2;
            yellow_tape[number-1]=3;
        }else if(color_id==1){
            orange_tape[number]=2;
            orange_tape[number-1]=3;
        }else{
            blue_tape[number]=2;
            blue_tape[number-1]=3;
        }
        //音楽止める
        if(mediaPlayer!=null){
            //audioStop()が別クラスのメソッドなので呼び出せませんでした。
            //できたらaudioStop()で音楽止めたいです。

            //以下audioStopのコピペ
            // 再生終了
            mediaPlayer.stop();
            // リセット
            mediaPlayer.reset();
            // リソースの解放
            mediaPlayer.release();

            mediaPlayer = null;
        }

        //アプリ画面に出力
        String color[] ={"黄色", "オレンジ","水色","コース外"};
        message= "ガス漏れ地点：" + color[color_id]+"  "+number+"番目";
        textView1.setText(message);
        textView2.setText("please input IP address!!!!!");

        //UI側への出力
        speed=0;
        in_course=0;
        now_game=0;
        start_count=0;
        messag_gas=2;
        out_course_count=0;
        score=55;
        minus_points_speed_high=-5;
        minus_points_speed_low=0 ;
        minus_points_course=-5;
        gas_leak_points=50;
        msg_score=0;
    }

    private void startCamera() {
        final ListenableFuture<ProcessCameraProvider> cameraProviderFuture = ProcessCameraProvider.getInstance(this);
        Context context = this;
        cameraProviderFuture.addListener(new Runnable() {
            @Override
            public void run() {
                try {
                    ProcessCameraProvider cameraProvider = cameraProviderFuture.get();
                    preview = new Preview.Builder().build();


                    imageAnalysis = new ImageAnalysis.Builder().build();
                    imageAnalysis.setAnalyzer(cameraExecutor, new MyImageAnalyzer());
                    CameraSelector cameraSelector = new CameraSelector.Builder().requireLensFacing(CameraSelector.LENS_FACING_BACK).build();


                    cameraProvider.unbindAll();
                    camera = cameraProvider.bindToLifecycle((LifecycleOwner)context, cameraSelector, preview, imageAnalysis);
                    preview.setSurfaceProvider(previewView.createSurfaceProvider(camera.getCameraInfo()));
                } catch(Exception e) {
                    Log.e(TAG, "[startCamera] Use case binding failed", e);
                }
            }
        }, ContextCompat.getMainExecutor(this));
    }


    //背景タップ時の処理
    @Override
    public boolean onTouchEvent(MotionEvent event) {
        // キーボードを隠す
        inputMethodManager.hideSoftInputFromWindow(mainLayout.getWindowToken(), InputMethodManager.HIDE_NOT_ALWAYS);


        // 背景にフォーカスを移す
        mainLayout.requestFocus();

        return true;
    }


    // ボタンがクリック時の処理
    @Override
    public void onClick(View v) {
        //押されたボタンのidを取得
        int id = v.getId();

        switch (id){
            case R.id.button1:{
                // 初期化処理を実行
                Log.i(TAG,"reset!");
                start_process();
                break;
            }
            case R.id.button2:{
                if(gas_flag==1){
                    gas_flag=0;
                    score+=gas_leak_points;//得点の加算


                    //UI側に対処完了の信号を送るための奴
                    messag_gas=3;


                    textView1.setText("ガス漏れに対処した");

                    //messag_gas=2;
                    mediaPlayer.stop();
                    // リセット
                    mediaPlayer.reset();
                    // リソースの解放
                    mediaPlayer.release();

                    mediaPlayer = null;

                }
                break;
            }
            case R.id.button3:{
                // IPアドレスを設定
                IP_flag=1;
                IP_address = editText.getText().toString();
                Log.i(TAG,"IP:"+IP_address);
                editText.setFocusable(true);
                editText.setFocusableInTouchMode(true);
                editText.requestFocus();
                break;
            }
        }

    }

    private class MyImageAnalyzer implements ImageAnalysis.Analyzer {
        private Mat matPrevious = null;
        //public MediaPlayer mediaPlayer;
        private BreakIterator button1;


        // frame rate
//        ImageAnalysis.Builder builder = new ImageAnalysis.Builder();
//        ImageAnalysis.Builder viewFinderConfigBuilder = PreviewConfig.Builder().apply{
//            setLensFacing(lensFacing);
//
//        }
//
//        Camera2Interop.Extender ext = new Camera2Interop.Extender<T>(builder);
//        ext.setCaptureRequestOption(CaptureRequest.CONTROL_AE_MODE, CaptureRequest.CONTROL_AE_MODE_OFF);
//        ext.setCaptureRequestOption(CaptureRequest.CONTROL_AE_TARGET_FPS_RANGE, new Range<Integer>(60,60));

        @Override
        public void analyze(@NonNull ImageProxy image) {
            /* Create cv::mat(RGB888) from image(NV21) */
            Mat matOrg = getMatFromImage(image);
            //long startTime = System.currentTimeMillis();
            /* Fix image rotation (it looks image in PreviewView is automatically fixed by CameraX???) */
            Mat mat = fixMatRotation(matOrg);

//            Log.i(TAG, "[analyze] width = " + image.getWidth() + ", height = " + image.getHeight() + "Rotation = " + previewView.getDisplay().getRotation());
//            Log.i(TAG, "[analyze] mat width = " + matOrg.cols() + ", mat height = " + matOrg.rows());

            //processing
            //img size is 640*480
           // Mat matOutput = new Mat(mat.rows(), mat.cols(), mat.type());
            if(IP_flag==1) {
                //UIでスタートを表示させるためにスタートから10フレームは処理を開始しない
                if (now_game == 0) {
                    if (start_count > 30) {
                        now_game = 2;
                    } else {
                        textView2.setText("starting now");
                        start_count += 1;
                    }
                }

                //処理
                if (now_game == 2) {
                    main_program(mat);
                }

//            /* Do some image processing */
//           Mat matOutput = new Mat(mat.rows(), mat.cols(), mat.type());
//            if (matPrevious == null) matPrevious = mat;
//            Core.absdiff(mat, matPrevious, matOutput);
//            matPrevious = mat;
//
//            /* Draw something for test */
                Imgproc.rectangle(mat, new Rect(x_pos, y_pos, 160, 40), new Scalar(255, 0, 0));
//            Imgproc.putText(matOutput, "leftTop", new Point(10, 10), 1, 1, new Scalar(255, 0, 0));
//
//            /* Convert cv::mat to bitmap for drawing */
                Bitmap bitmap = Bitmap.createBitmap(mat.cols(), mat.rows(), Bitmap.Config.ARGB_8888);
                Utils.matToBitmap(mat, bitmap);


                //send_message_to_UI(now_game, speed, messag_gas, in_course, msg_score);


                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        imageView.setImageBitmap(bitmap);
                    }
                });
            }
            /* Close the image otherwise, this function is not called next time */
            image.close();
//            long endTime = System.currentTimeMillis();
//            long tmpTime = endTime - startTime;
//            Log.i(TAG,"処理時間：" + tmpTime + " ms");
//            if (tmpTime < 30){
//                try {
//                    Thread.sleep((30 - tmpTime));
//                }catch (InterruptedException e){
//
//                }
//            }
        }

        void main_program(Mat mat){


            //色の取得
            state=get_color(mat);

            //コース内外のメッセージの作成
            if (state==5){
                out_course_count++;
                //10f連続で出たら減点＆表示
                if(out_course_count==10){
                    //連続30fで減点
                    score-=minus_points_course;
                    in_course=1;
                }
            }else{
                in_course=0;
                out_course_count=0;
            }

            if (state==4){
                //反射してるっぽい時は色そのままの判定で
                state=prev;
            }

            //状態の更新
            //ガス漏れへの対処の処理
            if (gas_flag==1&&state==color_id){

                messag_gas=0;
                textView1.setText("ガス漏れに対処中");
            } else if (gas_flag==1&&state!=color_id) {

                messag_gas=1;
                textView1.setText("ガス漏れ地点にもどろう");
            }

            //ゴールじのプロセス
            if (state==3){
                goal_process();
            }else {
                //ゴールしてない時の処理
                if (state == prev) {
                    //色そのまま
                    //Log.i(TAG, "step2");
                    count += 1;

                } else {
                    //色が変わった
                    //Log.i(TAG, "step3");

                    //ここでcount使って速度出してもいいかも
                    if (state!=5) {
                        //黄色、オレンジ、青の時のみここの処理を行う
                        check_speed(count);
                        update_color(state);
                    }

                    prev = state;
                    count = 0;



                }

//           textView2.setText("state:"+color[state]+"    prev:"+color[prev]+" count:"+count+" reverse_count:"+reverse_count+" Y:"+color_numbers[0]+" O:"+color_numbers[1]+" W:"+color_numbers[2]);
//            textView2.setText("state:"+color[state]+"   prev:"+color[prev]+" Y:"+color_numbers[0]+" O:"+color_numbers[1]+" W:"+color_numbers[2]);

                //textView2.setText("state:"+color[state]+"    prev:"+color[prev]+" count:"+count+" Y:"+color_numbers[0]+" O:"+color_numbers[1]+" W:"+color_numbers[2]);
                String color[] = {"Yellow", "Orange", "Blue", "Black","unknown","floor"};
                Log.i(TAG,"state:" + color[state] );
                textView2.setText("state:" + color[state] + " count:" + count + " Y:" + color_numbers[0] + " O:" + color_numbers[1] + " B:" + color_numbers[2]);
            }

        }

        void goal_process(){
            textView2.setText("goal now");
            now_game=1;
            caluc_acuuracy();
            make_score_message();
        }

        void caluc_acuuracy(){
            //テープ1回踏んでないごとに-1点
            int tapes;
            tapes=Arrays.stream(color_numbers).sum();
            tapes-=39;
            if (tapes>0){
                    tapes=0;
            }
            score+=tapes;
            Log.i(TAG, "accuracy is "+(39+tapes));
        }

        void make_score_message(){
            //scoreを10分の一にしてから四捨五入して10倍することで10点刻みに変更
            //60点以下は60点に変更
            double grade=score;

            grade/=10;
            grade=Math.round(grade);

            //swichとかに書き換えたほうがよさそう
            if(grade==10){
                msg_score=4;
            } else if (score==95) {
                msg_score=5;
            }else if(grade==9){
                msg_score=3;
            }else if(grade==8){
                msg_score=2;
            } else if (grade==7) {
                msg_score=1;
            }else{
                msg_score=0;
            }

//
//            if (score>79){
//                //good
//                grade=0;
//            } else if (score<51) {
//                //bad
//                grade=2;
//            }else{
//                //normal
//                grade=1;
//            }
            textView2.setText("real score:"+score+"  grade:"+grade);
        }

        void check_speed(int count){
            //UIに送るメッセージの変更
            if (count>30){
                //遅い
                score+=minus_points_speed_low;
                speed=1;
            } else if (count<4) {
                //速い
                score+=minus_points_speed_high;
                speed=2;
            }else{
                //ちょうど良い
                speed=0;
            }
        }

//manifest追加忘れずに
        void send_message_to_UI(int condition,int speed,int gas,int course,int score){
            //String send_message = Integer.valueOf(read_data.get(0)).toString();
            //intをStringに変換
            String message_a=Integer.valueOf(condition).toString();
            String message_b=Integer.valueOf(speed).toString();
            String message_c=Integer.valueOf(gas).toString();
            String message_d=Integer.valueOf(course).toString();
            String message_e=Integer.valueOf(score).toString();

            //UI側に送るメッセージを作る
            String send_message =message_a+message_b+message_c+message_d+message_e;
            //String send_message="2"+message_b+"22";

//                    for (int i=1;i<read_data.size();i++){
//                        send_message = send_message+","+Integer.valueOf(read_data.get(i)).toString();
//                  }

                    String test = send_message;

                    Log.i(TAG,"send_message: " + send_message);

                    // 読み取ったデータを転送
                   Runnable sender = new Runnable() {
                        @Override
                        public void run() {
                            Log.d(TAG,"server in");
                            //natori server
                            //String address = "192.168.86.38";

                            //yosizawa server
                            //String address = "192.168.86.36";
                            int port = 50009;
                            Socket socket = null;
                            try {
                                socket = new Socket(IP_address, port);
                                PrintWriter pw = new PrintWriter(socket.getOutputStream(), true);
                                pw.println(test);
                                Log.d(TAG,"server send");
                            } catch (UnknownHostException e) {
                                e.printStackTrace();
                            } catch (IOException e) {
                                e.printStackTrace();
                            }
                            if (socket != null) {
                               try {
                                    socket.close();
                                    socket = null;
                                } catch (IOException e) {
                                    e.printStackTrace();
                               }
                           }
                        }
                   };
                    Thread th = new Thread(sender);
                   th.start();
//            /* ここまで　*/
        }


        private double average(int length,double[] matrix){
            int sum=0;
            for(int i=0;i<length;i++){
                sum+=matrix[i];
            }
            return sum/length;
        }

        private double standard_deviation(double[] matrix) {
            double sum = 0;
            double vars = 0;

            for (int i = 0; i < matrix.length; i++) {
                sum += matrix[i];
            }
            double ave = ((double) sum) / matrix.length;
            for (int i = 0; i <matrix.length; i++) {
                vars += ((matrix[i] - ave) * (matrix[i] - ave));
            }
            double std = Math.sqrt(vars / matrix.length);
            return std;
        }

        private int get_color(Mat matOutput){
            Arrays.fill(R_matrix, 0);
            Arrays.fill(G_matrix, 0);
            Arrays.fill(B_matrix, 0);
            std=0;

            index=0;
//           if (matPrevious == null) matPrevious = mat;
            for (int i =x_pos;i<x_pos+160;i++){
                for (int j=y_pos;j<y_pos+40;j++){
                    double[] data = matOutput.get(j,i );
                    R_matrix[index] = data[0];
                    G_matrix[index] = data[1];
                    B_matrix[index] = data[2];
                    index++;
                }
            }


            color_R = average(index,R_matrix);
            color_G = average(index,G_matrix);
            color_B = average(index,B_matrix);
            std+=standard_deviation(R_matrix);
            std+=standard_deviation(G_matrix);
            std+=standard_deviation(B_matrix);

            Log.i(TAG, "R: "+color_R + " G: "+ color_G + " B: "+ color_B);

            //textView2.setText("R:"+color_R+" G:"+color_G+" B:"+color_B);
            //0:黄色　1:オレンジ　2:水色　
            state= judgeColor(color_R,color_G,color_B,std);
            return state;
        }
        int judgeColor(double R,double G,double B,double v){
//            if (R-G<10){
//                // yellow
//                return 0;
//            }else if(R-B>0){
//                // orange
//                return 1;
//            }else{
//                // light blue
//                return 2;
//            }
//            if (R<40&G<40&B<40){
//                //black
//                return 3;
//            }else if(R+G+B>650){
//                //反射
//                return 4;
//            }
//
//            else if((B-R)>30){
//                // blue
//                return 2;
//            }
//            else if (B<20){
//                // orange
//                return 1;
//            }else if((R-B)>100&B<70){
//                // orange
//                return 1;
//            }
//            else if(R>70&B<15){
//                // orange
//                return 1;
//            } else if ((R-B)>52) {
//                //yellow
//                return 0;
//            } else if (v>20) {
//                //floor
//                return 5;
//            } else if ((R-B)<20) {
//                //floor
//                return 5;
//            }else{
//                //yellow
//                return 0;
//            }
            //based on test1.py
            if (R<40&B<40&G<40){
                //black
                return 3;
            } else if ((R-B)<20) {
                //blue
                return 2;
            }  else if (B>R&G>R) {
                //blue
                return 2;
            } else if ((B*5)<R) {
                //orange
                return 1;
            } else if ((G*1.55)<R) {
                //orange
                return 1;
            }else if (v>40) {
                //floor
                return 5;
            }else{
                //yellow
                return 0;
            }
        }
        private void update_color(int state) {
            //色が変わったとき呼ばれる

            //すでに2が入っている(ガス漏れ位置)でないか確認
            //テープの配列の中身をすでに通った（値を1）に変更
            color_numbers[state]+=1;//番号の更新

            if (state==0){
                check_number(yellow_tape[color_numbers[0]]);
                yellow_tape[color_numbers[0]]=1;
            }else if(state==1){
                check_number(orange_tape[color_numbers[1]]);
                orange_tape[color_numbers[1]]=1;
            }else{
                check_number(blue_tape[color_numbers[2]]);
                blue_tape[color_numbers[2]]=1;
            }
        }

        private void check_number(int state_id) {
            //playSound();
            //Log.i(TAG, "check_number sound");
            if (state_id==2){
                //音鳴らす
                messag_gas=1;
                audioStop();
                audioPlay("gas_point-30sec.mp3");
                textView1.setText("ガス漏れ発見！");
                gas_flag=1;
                Log.i(TAG, "find gas"+state_id);
            } else if (state_id==3) {
                if(mediaPlayer!=null){
                    audioStop();
                }
                messag_gas=0;
                audioPlay("sound1.mp3");
                textView1.setText("ガス漏れ付近！");
            } else{

                messag_gas=2;
                Log.i(TAG, "not find gas"+state_id);
            }
        }

        private boolean audioSetup(String filePath){
            // make instance
            mediaPlayer = new MediaPlayer();

            // file name

            boolean fileCheck = false;

            // load mp3 file from assets
            try(AssetFileDescriptor afdescripter = getAssets().openFd(filePath))
            {
                // MediaPlayerに読み込んだ音楽ファイルを指定
                mediaPlayer.setDataSource(afdescripter.getFileDescriptor(),
                        afdescripter.getStartOffset(),
                        afdescripter.getLength());
                // 音量調整を端末のボタンに任せる
                setVolumeControlStream(AudioManager.STREAM_MUSIC);
                mediaPlayer.prepare();
                fileCheck = true;
            } catch (IOException e1) {
                e1.printStackTrace();
            }

//            mediaPlayer = MediaPlayer.create(this, R.raw.xperia_z_solarium);
//            fileCheck = true;

            return fileCheck;
        }

        private void audioPlay(String filePath) {

            if (mediaPlayer == null) {
                // audio ファイルを読出し
                audioSetup(filePath);
//                if (audioSetup()){
//                    Toast.makeText(getApplication(), "Read audio file", Toast.LENGTH_SHORT).show();
//                }
//                else{
//                    Toast.makeText(getApplication(), "Error: read audio file", Toast.LENGTH_SHORT).show();
//                    return;
//                }
            }
            else{
                // 繰り返し再生する場合
                mediaPlayer.stop();
                mediaPlayer.reset();
                // リソースの解放
                mediaPlayer.release();
            }

            // 再生する
            mediaPlayer.setLooping(true);
            mediaPlayer.start();

            // 終了を検知するリスナー
//        mediaPlayer.setOnCompletionListener(new MediaPlayer.OnCompletionListener() {
//            @Override
//            public void onCompletion(MediaPlayer mp) {
//                Log.d("debug","end of audio");
//                audioStop();
//            }
//        });
            // lambda


        }

        private void audioStop() {
            // 再生終了
            mediaPlayer.stop();
            // リセット
            mediaPlayer.reset();
            // リソースの解放
            mediaPlayer.release();

            mediaPlayer = null;
        }




        private Mat getMatFromImage(ImageProxy image) {
            /* https://stackoverflow.com/questions/30510928/convert-android-camera2-api-yuv-420-888-to-rgb */
            ByteBuffer yBuffer = image.getPlanes()[0].getBuffer();
            ByteBuffer uBuffer = image.getPlanes()[1].getBuffer();
            ByteBuffer vBuffer = image.getPlanes()[2].getBuffer();
            int ySize = yBuffer.remaining();
            int uSize = uBuffer.remaining();
            int vSize = vBuffer.remaining();
            byte[] nv21 = new byte[ySize + uSize + vSize];
            yBuffer.get(nv21, 0, ySize);
            vBuffer.get(nv21, ySize, vSize);
            uBuffer.get(nv21, ySize + vSize, uSize);
            Mat yuv = new Mat(image.getHeight() + image.getHeight() / 2, image.getWidth(), CvType.CV_8UC1);
            yuv.put(0, 0, nv21);
            Mat mat = new Mat();
            Imgproc.cvtColor(yuv, mat, Imgproc.COLOR_YUV2RGB_NV21, 3);
            return mat;
        }

        private Mat fixMatRotation(Mat matOrg) {
            Mat mat;
            switch (previewView.getDisplay().getRotation()){
                default:
                case Surface.ROTATION_0:
                    mat = new Mat(matOrg.cols(), matOrg.rows(), matOrg.type());
                    Core.transpose(matOrg, mat);
                    Core.flip(mat, mat, 1);
                    break;
                case Surface.ROTATION_90:
                    mat = matOrg;
                    break;
                case Surface.ROTATION_270:
                    mat = matOrg;
                    Core.flip(mat, mat, -1);
                    break;
            }
            return mat;
        }


    }

    private boolean checkPermissions(){
        for(String permission : REQUIRED_PERMISSIONS){
            if(ContextCompat.checkSelfPermission(this, permission) != PackageManager.PERMISSION_GRANTED){
                return false;
            }
        }
        return true;
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
//        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if(requestCode == REQUEST_CODE_FOR_PERMISSIONS){
            if(checkPermissions()){
                startCamera();
            } else{
                Log.i(TAG, "[onRequestPermissionsResult] Failed to get permissions");
                this.finish();
            }
        }
    }
}





//IPアドレスの設定アプリから ok
//edittext入力後にフォーカス外す ok

//水色→黄色で黄色がオレンジに見える問題を何とかする
//カメラパラメータこてい？？


//レイアウトの変更
//上のほうにＩＰアドレスの設定移動させてカメラ画像を表示させないようにする
