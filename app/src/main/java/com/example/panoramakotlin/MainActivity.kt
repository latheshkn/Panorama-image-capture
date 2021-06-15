package com.example.panoramakotlin

import android.Manifest
import android.app.Activity
import android.app.ProgressDialog
import android.bluetooth.BluetoothAdapter
import android.content.Context
import android.content.Intent
import android.content.pm.ActivityInfo
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Color
import android.graphics.drawable.GradientDrawable
import android.location.LocationManager
import android.os.Build
import android.os.Bundle
import android.os.Environment
import android.os.Handler
import android.provider.Settings
import android.util.DisplayMetrics
import android.util.Log
import android.view.*
import android.widget.ImageView
import android.widget.RelativeLayout
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.core.app.ActivityCompat
import com.dermandar.dmd_lib.CallbackInterfaceShooter
import com.dermandar.dmd_lib.DMD_Capture
import com.dermandar.dmd_lib.DMD_Capture.CircleDetectionCallback
import com.nativesystem.Core
import java.io.File
import java.util.*

class MainActivity : Activity() {

    val MY_PERMISSIONS_REQUEST_LOCATION = 1
    val MY_PERMISSIONS_REQUEST_CAMERA = 2
    val MY_PERMISSIONS_REQUEST_STORAGE = 3
    var REQUEST_ENABLE_BT = 104
    var locationAsked = false
    var prefModeKey = "ShotMode"
    var isSDKRotator = false
    private var mDisplayMetrics: DisplayMetrics? = null
    private var mDisplayRotation = 0
    private var mScreenWidth = 0
    private var mScreenHeight: Int = 0
    private var mAspectRatio = 0.0
    private var mRelativeLayout: RelativeLayout? = null
    private var mWidth = 400
    private var mHeight: Int = 500
    private var mTextViewInstruction: TextView? = null
    private var mCurrentInstructionMessageID = -1
    private val drawcircle = true
    var txtLensName: TextView? = null
    var prefLensKey = "LensSelected"
    var selectedLens = "none"
    var lensName = "None"
    var prefLensNameKey = "LensSelectedName"
    var imgRotMode: ImageView? = null
    private var mDMDCapture: DMD_Capture? = null
    val request_Code = 103
    private var mIsShootingStarted = false
    var FL = 0.0
    var lensIDRotator = 0
    var isRequestViewer = false
    var saveOri = true
    var IS_HD = false
    var viewGroup: ViewGroup? = null
    private var mIsCameraReady = false
    private var mNumberTakenImages = 0
    private var mPanoramaPath: String? = null
    var activityW: Float? = null
    var activityH: Float? = null
    var circle: ImageView? = null
    var isRequestExit = false
    private var mEquiPath = ""

    internal enum class fisheye {
        none
    }

    internal enum class detectResult {
        DMDCircleDetectionInvalidInput, DMDCircleDetectionCircleNotFound, DMDCircleDetectionBad, DMDCircleDetectionGood
    }

    private val circleResult = detectResult.values()


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
//        setContentView(R.layout.activity_main)


        requestWindowFeature(Window.FEATURE_NO_TITLE)

        window.setFlags(
            WindowManager.LayoutParams.FLAG_FULLSCREEN,
            WindowManager.LayoutParams.FLAG_FULLSCREEN
        )

        if (Build.VERSION.SDK_INT > Build.VERSION_CODES.M) {

            if ((ActivityCompat.checkSelfPermission(
                    applicationContext,
                    Manifest.permission.READ_EXTERNAL_STORAGE
                ) == PackageManager.PERMISSION_GRANTED) &&
                (ActivityCompat.checkSelfPermission(
                    applicationContext,
                    Manifest.permission.WRITE_EXTERNAL_STORAGE
                ) == PackageManager.PERMISSION_GRANTED) &&
                (ActivityCompat.checkSelfPermission(
                    applicationContext,
                    Manifest.permission.CAMERA
                ) == PackageManager.PERMISSION_GRANTED)
            ) {
                validateBluetooth()

            } else if (ActivityCompat.checkSelfPermission(
                    applicationContext,
                    Manifest.permission.CAMERA
                ) != PackageManager.PERMISSION_GRANTED
            ) {

                requestPermissions(
                    arrayOf(Manifest.permission.CAMERA),
                    MY_PERMISSIONS_REQUEST_CAMERA
                )
                return

            } else if (ActivityCompat.checkSelfPermission(
                    applicationContext,
                    Manifest.permission.READ_EXTERNAL_STORAGE
                ) != PackageManager.PERMISSION_GRANTED || ActivityCompat.checkSelfPermission(
                    applicationContext,
                    Manifest.permission.WRITE_EXTERNAL_STORAGE
                ) != PackageManager.PERMISSION_GRANTED
            ) {

                requestPermissions(
                    arrayOf(
                        Manifest.permission.READ_EXTERNAL_STORAGE,
                        Manifest.permission.WRITE_EXTERNAL_STORAGE
                    ),
                    MY_PERMISSIONS_REQUEST_STORAGE
                )
                return
            }
        }


    }

    private fun validateBluetooth() {

        val bluetoothAdapter = BluetoothAdapter.getDefaultAdapter()
        if (bluetoothAdapter == null) {
            Toast.makeText(
                applicationContext,
                "your device does not have Bluetooth",
                Toast.LENGTH_LONG
            ).show()
            return
        }

        if (!bluetoothAdapter.isEnabled) {
            val enableBtIntent = Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE)
            startActivityForResult(enableBtIntent, REQUEST_ENABLE_BT)
        } else {
            onCreateSub()
        }
    }

    private fun onCreateSub() {

        Log.e("rmh", "oncreatesub")

//        validate location on
        if (!locationAsked) {

            val lm = getSystemService(Context.LOCATION_SERVICE) as LocationManager

            var gps_enabled = false
            var network_enabled = false
            try {
                gps_enabled = lm.isProviderEnabled(LocationManager.GPS_PROVIDER)
            } catch (e: Exception) {
            }
            try {
                network_enabled = lm.isProviderEnabled(LocationManager.NETWORK_PROVIDER)
            } catch (e: Exception) {
            }

            if (!gps_enabled && !network_enabled) {
                val dialog = AlertDialog.Builder(this)
                dialog.setMessage("Your GPS is turned off, you may need to turn it on to connect to the Bluetooth rotator")

                dialog.setPositiveButton("Open Location Setting") { dialogInterface, which ->


                    // TODO Auto-generated method stub
                    val myIntent = Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS)
                    startActivity(myIntent)
                }
                dialog.setNegativeButton("Ignore") { dialogInterface, with ->

                }

                dialog.show()
                locationAsked = true
            }

        }

//end validate location on


        //end validate location on
        val pref = applicationContext.getSharedPreferences("MyPref", MODE_PRIVATE)
        val lastShootMode = pref.getString(prefModeKey, "")

        if (lastShootMode.equals("")) {

            val editer = pref.edit()

            if (isSDKRotator) {
                editer.putString(prefModeKey, "rotator")
            } else {
                editer.putString(prefModeKey, "hand")
                editer.commit()
            }
        } else {

            isSDKRotator = if (lastShootMode == "rotator") true else false
        }

        mDisplayMetrics = DisplayMetrics()
        mDisplayRotation = windowManager.defaultDisplay.rotation

        val path = Environment.getExternalStorageDirectory().toString() + "/" + folderName
        val _path = File(path)
        _path.mkdirs()
        Log.e("rmh", "path:**$path")

        //Core.setLogPath(path);
        //Core.setDebugPathRotator(path);

        //getting screen resolution
        val mDisplay = windowManager.defaultDisplay
        val mDisplayMetrics = DisplayMetrics()
        mDisplay.getMetrics(mDisplayMetrics)
        val mDisplayRotation = windowManager.defaultDisplay.rotation

        //Full screen activity
        //requestWindowFeature(Window.FEATURE_NO_TITLE);
        if (mDisplayRotation == Surface.ROTATION_0 || mDisplayRotation == Surface.ROTATION_180) {
            mScreenWidth = mDisplayMetrics.widthPixels
            mScreenHeight = mDisplayMetrics.heightPixels
        } else {
            mScreenWidth = mDisplayMetrics.heightPixels
            mScreenHeight = mDisplayMetrics.widthPixels
        }

        mAspectRatio = mScreenHeight.toDouble() / mScreenWidth.toDouble()

        if (mAspectRatio < 1.0) {

            requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE
        } else {
            requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_PORTRAIT
        }

        setContentView(R.layout.activity_main)

        mRelativeLayout = findViewById<View>(R.id.relativeLayout) as RelativeLayout

        val display = windowManager.defaultDisplay
        val displayMetrics = DisplayMetrics()
        display.getMetrics(displayMetrics)

        mWidth = displayMetrics.widthPixels
        mHeight = displayMetrics.heightPixels

        startShooter()

        var lp: RelativeLayout.LayoutParams

        val lp0 = RelativeLayout.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT,
            ViewGroup.LayoutParams.MATCH_PARENT
        )


        //Text View instruction
        mTextViewInstruction = TextView(this)
        mTextViewInstruction!!.setTextSize(32f)
        mTextViewInstruction!!.setGravity(Gravity.CENTER)
        mTextViewInstruction!!.setTextColor(Color.WHITE)
        setInstructionMessage(R.string.tap_anywhere_to_start)
        mRelativeLayout!!.addView(mTextViewInstruction, lp0)

// create circle
        val shape = GradientDrawable()
        shape.cornerRadius = 50f
        shape.setColor(Color.parseColor("#88a8a8a8"))
        txtLensName = TextView(this)
        txtLensName!!.textSize = 16f
        txtLensName!!.gravity = Gravity.CENTER
        txtLensName!!.setTextColor(Color.BLACK)
        txtLensName!!.setPadding(20, 20, 20, 20)
//        set circle background to textview
        txtLensName!!.background = shape

        lp = RelativeLayout.LayoutParams(
            ViewGroup.LayoutParams.WRAP_CONTENT,
            ViewGroup.LayoutParams.WRAP_CONTENT
        ) // You might want to tweak these to WRAP_CONTENT

        lp.addRule(RelativeLayout.ALIGN_PARENT_BOTTOM)
        lp.addRule(RelativeLayout.ALIGN_PARENT_RIGHT)
        lp.bottomMargin = 200
        lp.rightMargin = 20
        mRelativeLayout!!.addView(txtLensName, lp)


        val lastUsedLense = pref.getString(prefLensKey, "none")
        if (lastUsedLense == "") {
            val editor = pref.edit()
            editor.putString(prefLensKey, selectedLens)
            editor.commit()
        } else {
            selectedLens = lastUsedLense!!
            lensName = pref.getString(prefLensNameKey, "No Lens")!!
        }

        imgRotMode = ImageView(this)

        if (isSDKRotator) imgRotMode!!.setImageResource(R.drawable.rotator_disconn) else imgRotMode!!.setImageResource(
            R.drawable.handheld
        )

        //imgRotMode.setBackgroundColor(Color.WHITE);
        lp = RelativeLayout.LayoutParams(
            ViewGroup.LayoutParams.WRAP_CONTENT,
            ViewGroup.LayoutParams.WRAP_CONTENT
        ) // You might want to tweak these to WRAP_CONTENT

        lp.addRule(RelativeLayout.ALIGN_PARENT_BOTTOM)
        lp.addRule(RelativeLayout.ALIGN_PARENT_LEFT)
        lp.bottomMargin = 200
        lp.leftMargin = 70
        mRelativeLayout!!.addView(imgRotMode, lp)


        imgRotMode!!.setOnClickListener({

            if (mDMDCapture != null) {
                isSDKRotator = !isSDKRotator
                val pref = applicationContext.getSharedPreferences("MyPref", MODE_PRIVATE)
                val editor = pref.edit()

                if (isSDKRotator) editor.putString(prefModeKey, "rotator") else editor.putString(
                    prefModeKey,
                    "hand"
                )
                editor.commit()
            }

            if (isSDKRotator) {
                imgRotMode!!.setImageResource(R.drawable.rotator_disconn)
                mDMDCapture!!.prepareFlipMode(isSDKRotator)
            } else {
                imgRotMode!!.setImageResource(R.drawable.handheld)
                mDMDCapture!!.prepareFlipMode(isSDKRotator)
            }


            // start solve fast switch issue
            val progress = ProgressDialog(this@MainActivity)
            progress.setTitle((if (isSDKRotator) "C" else "Disc") + "onnecting...")
            progress.setMessage((if (isSDKRotator) "Connecting to" else "Disconnecting from") + " rotator")
            progress.setCancelable(false) // disable dismiss by tapping outside of the dialog

            progress.show()
            imgRotMode!!.isEnabled = false

            val h = Handler()
            h.postDelayed({
                progress.dismiss()
                imgRotMode!!.isEnabled = true
            }, 2000)

        })

        lp = RelativeLayout.LayoutParams(
            ViewGroup.LayoutParams.WRAP_CONTENT,
            ViewGroup.LayoutParams.WRAP_CONTENT
        )
        lp.addRule(RelativeLayout.ALIGN_PARENT_BOTTOM)
        lp.addRule(RelativeLayout.ALIGN_PARENT_LEFT)
        lp.bottomMargin = 200
        lp.leftMargin = 250

        txtLensName!!.setOnClickListener({
            if (!mIsShootingStarted) {
                //if(mDMDCapture.canShootHDR())
                //  mDMDCapture.setHDRStatus(false);
                val a = Intent(this@MainActivity, Lenses::class.java)
                a.putExtra("CurrentLens", selectedLens)
                startActivityForResult(a, request_Code)
            }
        })


        //setStartShootingHand();
        if (selectedLens == "none") mDMDCapture!!.setLensSelected(false) else mDMDCapture!!.setLensSelected(
            true
        )
        FL = mDMDCapture!!.fl
        setNewLens(getLensNb(selectedLens))

    }


    private fun setInstructionMessage(msgID: Int) {

        if (mCurrentInstructionMessageID == msgID) return
        runOnUiThread { //                RelativeLayout.LayoutParams params = new RelativeLayout.LayoutParams(mDisplayMetrics.widthPixels, RelativeLayout.LayoutParams.WRAP_CONTENT);
//                params.addRule(RelativeLayout.CENTER_HORIZONTAL);
//
//                if (msgID == R.string.instruction_empty || msgID == R.string.hold_the_device_vertically || msgID == R.string.tap_anywhere_to_start
//                        || msgID == R.string.instruction_focusing) {
//                    params.addRule(RelativeLayout.CENTER_VERTICAL);
//                } else {
//                    params.addRule(RelativeLayout.ALIGN_PARENT_BOTTOM);
//                }
//
//                mTextViewInstruction.setLayoutParams(params);
            mTextViewInstruction!!.setText(msgID)
            mCurrentInstructionMessageID = msgID
        }
    }

    fun setNewLens(lensId: Int) {
        txtLensName!!.text = lensName
        lensIDRotator = lensId
        mDMDCapture!!.setLens(lensId)
    }

    fun getLensNb(selectedLens: String?): Int {

        return fisheye.valueOf(selectedLens!!).ordinal
    }

    private fun startShooter() {

        Log.e("rmh", "startShooter")
        isRequestViewer = false

        mDMDCapture = DMD_Capture()
        mDMDCapture!!.setRotatorMode(isSDKRotator)

        if (saveOri) mDMDCapture!!.setExportOriOn()

        mDMDCapture!!.setCircleDetectionCallback(CircleDetectionCallback { res ->

            val x = detectResult.values()[res]

            Log.e("rmh", "result detection:$res")
            if (!drawcircle) {

                if (x == detectResult.DMDCircleDetectionInvalidInput) {
                    Toast.makeText(
                        this,
                        "Something went wrong in detecting the lens.",
                        Toast.LENGTH_SHORT
                    ).show()
                }
                return@CircleDetectionCallback
            }
            if (x == detectResult.DMDCircleDetectionInvalidInput) {
                Toast.makeText(
                    this,
                    "Something went wrong in detecting the lens.",
                    Toast.LENGTH_SHORT
                ).show()
            } else if (x == detectResult.DMDCircleDetectionCircleNotFound) {
                drawCircle(R.drawable.yellowcircle)
            } else if (x == detectResult.DMDCircleDetectionBad) {
                drawCircle(R.drawable.redcircle)
            } else if (x == detectResult.DMDCircleDetectionGood) {
                drawCircle(R.drawable.greencircle)
            }
        })

        if (mDMDCapture!!.canShootHD()) {
            mDMDCapture!!.setResolutionHD()
            IS_HD = true
        }
        viewGroup = mDMDCapture!!.initShooter(
            this@MainActivity,
            mCallbackInterface,
            windowManager.defaultDisplay.rotation,
            true,
            true
        )
        mRelativeLayout!!.addView(viewGroup)

        viewGroup!!.setOnClickListener({

            if (!mIsCameraReady)
                return@setOnClickListener
            if (!mIsShootingStarted) {
                mNumberTakenImages = 0
                mPanoramaPath = Environment.getExternalStorageDirectory().toString() + "/Lib_Test/"
                Log.d("check_the_path", "path $mPanoramaPath")
                mIsShootingStarted = mDMDCapture!!.startShooting(mPanoramaPath)
                if (mIsShootingStarted) {
                    imgRotMode!!.visibility = View.INVISIBLE
                    txtLensName!!.visibility = View.INVISIBLE
                } else {
                    imgRotMode!!.visibility = View.VISIBLE
                    txtLensName!!.visibility = View.VISIBLE
                }
//                    mIsShootingStarted = true;
            } else {

                //Log.e("rmh")
                mDMDCapture!!.finishShooting()
                mIsShootingStarted = false
                imgRotMode!!.visibility = View.VISIBLE
                txtLensName!!.visibility = View.VISIBLE
            }
        })


    }

    private fun drawCircle(_resId: Int) {
        val relMain: View = findViewById<View>(R.id.relativeLayout) as RelativeLayout
        val dm = DisplayMetrics()
        this.windowManager.defaultDisplay.getMetrics(dm)
        val topOffset = dm.heightPixels - relMain.measuredHeight
        val tempView: View = viewGroup!!
        val position = IntArray(2)
        tempView.getLocationOnScreen(position)
        val y = position[1] - topOffset
        val resId = _resId

        runOnUiThread(Runnable {

            run {
                val dim = IntArray(2)

                viewGroup!!.getLocationInWindow(dim)
                Log.e("rmh", "dim:" + dim[0] + " " + dim[1])
                val dim2 = IntArray(2)
                viewGroup!!.getLocationOnScreen(dim2)
                activityW = viewGroup!!.width.toFloat()
                activityH = viewGroup!!.height.toFloat()
                var marginLeft: Int
                var marginRight: Int
                var marginTop: Int
                var marginBottom: Int
                var circlediameter: Float
                var circleValues: FloatArray

                circleValues = Core.getCircleData(activityW!!, activityH!!, 0, dim[0], FL)

                marginTop = Math.round(circleValues[0] + dim[1])
                marginRight = Math.round(circleValues[1])
                marginBottom = Math.round(circleValues[2])
                marginLeft = Math.round(circleValues[3])
                circlediameter = circleValues[4]

                Log.e(
                    "rmh",
                    "mars: $marginLeft $marginRight $marginTop $marginBottom"
                )
                //circle
                circle?.setImageResource(resId)

                val lp = RelativeLayout.LayoutParams(
                    RelativeLayout.LayoutParams.WRAP_CONTENT,
                    RelativeLayout.LayoutParams.WRAP_CONTENT
                )
                lp.leftMargin = -marginLeft //-margin; -marginLeft;

                lp.rightMargin = -marginRight //-margin; -marginRight;

                lp.topMargin = marginTop
                lp.bottomMargin = -marginBottom
                lp.width = Math.round(circlediameter)
                lp.height = Math.round(circlediameter)
                circle?.setLayoutParams(lp)
                circle?.setVisibility(View.VISIBLE)
            }
        })
    }

    var imgName: String? = null
    private val mIsOpenGallery = false
    var folderName = "Apanoramas"
    private val mCallbackInterface: CallbackInterfaceShooter = object : CallbackInterfaceShooter {
        override fun onCameraStopped() {
            mIsShootingStarted = false
            mIsCameraReady = false
            if (tmr != null) tmr!!.cancel()
            tmr = null
        }

        override fun onCameraStarted() {
            mIsCameraReady = true
            //            if(tmr!=null) tmr.cancel(); tmr=null;
//            tmr=new Timer();
//            tmr.scheduleAtFixedRate(new TimerTask() {
//                @Override
//                public void run() {
//					cnt++;
//					if(lastMillis==0)lastMillis=System.nanoTime();
//					if(System.nanoTime()-lastMillis>=1*1000000000) {
//						Log.e("DMD", "fps: " + cnt);
//						cnt=0;
//						lastMillis=System.nanoTime();
//					}
//
//                    HashMap<String, Object> map = mDMDCapture.getIndicators();
//					Log.e("DMD", "fovx:" + map.get(DMD_Capture.ShootingIndicatorsEnum.fovx.toString()));//Integer value in degrees
//					Log.e("DMD", "orientation:" + map.get(DMD_Capture.ShootingIndicatorsEnum.orientation.toString()));//Integer, kPanoramaOrientationLTR = -1, kPanoramaOrientationUnknown = 0, kPanoramaOrientationRTL = 1
            // Log.e("DMD", "percentage:" + map.get(DMD_Capture.ShootingIndicatorsEnum.percentage.toString()));//Double
//					Log.e("DMD", "pitch:" + map.get(DMD_Capture.ShootingIndicatorsEnum.pitch.toString()));//Double
//					Log.e("DMD", "roll:" + map.get(DMD_Capture.ShootingIndicatorsEnum.roll.toString()));//Double
//                }
//            },0, 16);
        }

        override fun onFinishClear() {}
        override fun onFinishRelease() {
            mRelativeLayout!!.removeView(viewGroup)
            viewGroup = null
            if (isRequestExit) {
                finish()
                return
            }
            mDMDCapture!!.startCamera(applicationContext, mWidth, mHeight)
        }

        private val cnt: Long = 0
        private val lastMillis: Long = 0
        override fun onDirectionUpdated(a: Float) {

//			cnt++;
//			if(lastMillis==0)lastMillis=System.nanoTime();
//			if(System.nanoTime()-lastMillis>1*1000000000) {
//				//Log.e("AMS", "fps: " + cnt);
//				cnt=0;
//				lastMillis=System.nanoTime();
//			}
//			Thread t=new Thread(new Runnable() {
//				@Override
//				public void run() {
//					try {
//						Thread.sleep((long)100);
//					} catch (InterruptedException e) {
//						e.printStackTrace();
//					}
//
//					HashMap<String, Object> map = mDMDCapture.getIndicators();
////					Log.e("AMS", "fovx:" + map.get(DMD_Capture.ShootingIndicatorsEnum.fovx.toString()));//Integer value in degrees
//					//Log.e("AMS", "orientation:" + map.get(DMD_Capture.ShootingIndicatorsEnum.orientation.toString()));//Integer, kPanoramaOrientationLTR = -1, kPanoramaOrientationUnknown = 0, kPanoramaOrientationRTL = 1
//					//Log.e("AMS", "percentage:" + map.get(DMD_Capture.ShootingIndicatorsEnum.percentage.toString()));//Double
////					Log.e("AMS", "pitch:" + map.get(DMD_Capture.ShootingIndicatorsEnum.pitch.toString()));//Double
////					Log.e("AMS", "roll:" + map.get(DMD_Capture.ShootingIndicatorsEnum.roll.toString()));//Double
////					Log.e("AMS", "roll:" + map.get(DMD_Capture.ShootingIndicatorsEnum.roll.toString()));
//				}
//			});
//			t.start();
        }

        private var tmr: Timer? = null
        override fun preparingToShoot() {
            /***
             * Example about reading the shooting indicators
             */
        }

        override fun canceledPreparingToShoot() {}
        override fun takingPhoto() {

        }

        override fun shotTakenPreviewReady(bitmapPreview: Bitmap) {

        }

        override fun photoTaken() {
            Log.e("rmh", "photoTaken")
            val map = mDMDCapture!!.indicators
            mNumberTakenImages++
            if (mNumberTakenImages <= 0) {
                setInstructionMessage(R.string.tap_anywhere_to_start)
            } else if (mNumberTakenImages == 1) {
                setInstructionMessage(R.string.rotate_left_or_right_or_tap_to_restart)
            } else {
                setInstructionMessage(R.string.tap_to_finish_when_ready_or_continue_rotating)
            }

//					Log.e("AMS", "fovx:" + map.get(DMD_Capture.ShootingIndicatorsEnum.fovx.toString()));//Integer value in degrees
//            Log.e("AMS", "orientation:" + map.get(DMD_Capture.ShootingIndicatorsEnum.orientation.toString()));//Integer, kPanoramaOrientationLTR = -1, kPanoramaOrientationUnknown = 0, kPanoramaOrientationRTL = 1
//            Log.e("AMS", "percentage:" + map.get(DMD_Capture.ShootingIndicatorsEnum.percentage.toString()));//Double
        }

        override fun stitchingCompleted(info: HashMap<String, Any>) {
            Log.e("rmh", "stitching completed")
            val time = System.currentTimeMillis()

            imgName = "img_" + java.lang.Long.toString(time) + ".jpg"
            mEquiPath =
                Environment.getExternalStorageDirectory().path + "/" + folderName + "/" + imgName
            Log.e("AMS", "decode logo")


            val op = BitmapFactory.Options()
            op.inPreferredConfig = Bitmap.Config.ARGB_8888
            val bmp = BitmapFactory.decodeResource(resources, R.drawable.logo, op)
            val bytes: ByteArray = DMDBitmapToRGBA888.ImageToRGBA8888(bmp)!!
            val min_zenith =
                0f //specifies the size of the top logo between 0..90 degrees, otherwise it is set to 0
            val min_nadir =
                0f //specifies the size of the bottom logo between 0..90 degrees, otherwise it is set to 0
            val res = mDMDCapture!!.setLogo(bytes, min_zenith, min_nadir)
            //int res = mDMDCapture.setLogo(null,min_zenith,min_nadir);  // to use  default logo (DMD logo).
            Log.e("AMS", "logo set finished: $res")
            mDMDCapture!!.genEquiAt(mEquiPath, 800, 0, 0, false, false)
            //mDMDCapture.releaseShooter();


        }

        override fun shootingCompleted(finished: Boolean) {
            Log.e("rmh", "shootingCompleted: $finished")
            if (finished) {
                //mTextViewInstruction.setVisibility(View.INVISIBLE);
                mDMDCapture!!.stopCamera()
            }
            mIsShootingStarted = false
        }

        override fun deviceVerticalityChanged(isVertical: Int) {
            if (isVertical == 1) {
                if (!mIsShootingStarted) setInstructionMessage(R.string.tap_anywhere_to_start)
            } else {
                setInstructionMessage(R.string.hold_the_device_vertically)
            }
        }

        override fun compassEvent(info: HashMap<String, Any>) {
            Toast.makeText(applicationContext, "Compass interference", Toast.LENGTH_SHORT).show()
            mIsShootingStarted = false
        }

        override fun onFinishGeneratingEqui() {
            deleteTempFolder()
            Log.e("rmh", "onFinishGeneratingEqui")
            mIsShootingStarted = false
            mDMDCapture!!.startCamera(applicationContext, mWidth, mHeight)
            Toast.makeText(applicationContext, "Image saved to $mEquiPath", Toast.LENGTH_LONG)
                .show()

            //isRequestExit = true;
            //isRequestViewer = true;
            //mDMDCapture.stopCamera();
            //mDMDCapture.releaseShooter();
            imgRotMode!!.visibility = View.VISIBLE
            txtLensName!!.visibility = View.VISIBLE

//            val pathe = File(Environment.getExternalStorageDirectory().name + "/PanoramaKotlin")
//            if (pathe.exists()){
//                pathe.delete()
//            }

        }

        override fun onExposureChanged(mode: DMD_Capture.ExposureMode) {

        }

        //rotaor
        override fun onRotatorConnected() {
            Log.e("rmh", "rot connected")
            runOnUiThread { //                    txtRotConn.setBackgroundColor(Color.GREEN);
                imgRotMode!!.setImageResource(R.drawable.rotator_conn)
            }
        }

        override fun onRotatorDisconnected() {
            Log.e("rmh", "rot disconnected")
            runOnUiThread { //                    txtRotConn.setBackgroundColor(Color.RED);
                if (isSDKRotator) imgRotMode!!.setImageResource(R.drawable.rotator_disconn)
            }
        }

        override fun onStartedRotating() {}
        override fun onFinishedRotating() {}
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        vararg permissions: String?,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)

        Log.e("rmh", "onRequestPermissionsResult main")
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            when (requestCode) {
                MY_PERMISSIONS_REQUEST_CAMERA -> {
                    Log.e("rmh", "MY_PERMISSIONS_REQUEST_CAMERA");
                    if (grantResults.size > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                        if (ActivityCompat.checkSelfPermission(
                                this,
                                android.Manifest.permission.READ_EXTERNAL_STORAGE
                            ) != PackageManager.PERMISSION_GRANTED ||
                            ActivityCompat.checkSelfPermission(
                                this,
                                android.Manifest.permission.WRITE_EXTERNAL_STORAGE
                            ) != PackageManager.PERMISSION_GRANTED
                        ) {
                            ActivityCompat.requestPermissions(
                                this,
                                arrayOf(
                                    Manifest.permission.READ_EXTERNAL_STORAGE,
                                    Manifest.permission.WRITE_EXTERNAL_STORAGE
                                ), MY_PERMISSIONS_REQUEST_STORAGE
                            );
                            return;
                        } else
                            validateBluetooth();
                    } else {
                        toastMessage("Camera, Storage and Location permissions are not optional!");

                    }
                    return;

                }
                MY_PERMISSIONS_REQUEST_STORAGE -> {
                    Log.e("rmh", "MY_PERMISSIONS_REQUEST_STORAGE");
                    if (grantResults.size > 1 && grantResults[0] == PackageManager.PERMISSION_GRANTED && grantResults[1] == PackageManager.PERMISSION_GRANTED) {
                        if (ActivityCompat.checkSelfPermission(
                                this,
                                android.Manifest.permission.CAMERA
                            ) != PackageManager.PERMISSION_GRANTED
                        ) {
                            ActivityCompat.requestPermissions(
                                this,
                                arrayOf(

                                    Manifest.permission.CAMERA
                                ), MY_PERMISSIONS_REQUEST_CAMERA
                            );
                            return;

                        } else {
                            validateBluetooth();
                            toastMessage("Camera, Storage and Location permissions are not optional!");
                            return;
                        }
                    } else {
                        toastMessage("Camera, Storage and Location permissions are not optional!");

                        return;
                    }

                }

            }

        }
    }


    private fun toastMessage(message: String) {
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show()
    }

    override fun onResume() {
        Log.e("rmh", "on resume")
        super.onResume()
        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        if (mDMDCapture != null) {
            Log.e("AMS", "onResume::startCamera")
            mDMDCapture!!.startCamera(this as Context, mWidth, mHeight)
            //mDMDCapture.setContinuousShooting(true);
        }
        if (mTextViewInstruction != null) mTextViewInstruction!!.visibility = View.VISIBLE
        if (imgRotMode != null) imgRotMode!!.visibility = View.VISIBLE
        if (txtLensName != null) txtLensName!!.visibility = View.VISIBLE
    }

    override fun onBackPressed() {
        Log.e("rmh", "onbackpressed")
        if (mIsShootingStarted) {
            mDMDCapture!!.stopShooting()
            mIsShootingStarted = false
            imgRotMode!!.visibility = View.VISIBLE
            txtLensName!!.visibility = View.VISIBLE
        } else {
            isRequestExit = true
            isRequestViewer = false
            mDMDCapture!!.releaseShooter()
            //super.onBackPressed();
        }
    }

    private fun deleteTempFolder() {
        val myDir = File(
            Environment.getExternalStorageDirectory().toString() + "/" + "Pictures/PanoramaKotlin"
        )
        if (myDir.isDirectory) {
            val children = myDir.list()
            for (i in children.indices) {
                File(myDir, children[i]).delete()
            }
            myDir.delete()
        }
    }

}