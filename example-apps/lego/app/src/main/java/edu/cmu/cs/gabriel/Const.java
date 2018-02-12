// Copyright 2018 Carnegie Mellon University
//
// Licensed under the Apache License, Version 2.0 (the "License");
// you may not use this file except in compliance with the License.
// You may obtain a copy of the License at
//
//      http://www.apache.org/licenses/LICENSE-2.0
//
// Unless required by applicable law or agreed to in writing, software
// distributed under the License is distributed on an "AS IS" BASIS,
// WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
// See the License for the specific language governing permissions and
// limitations under the License.

package edu.cmu.cs.gabriel;

import java.io.File;

import android.os.Environment;

public class Const {
    // whether to do a demo or a set of experiments
    public static final boolean IS_EXPERIMENT = false;

    // whether to use real-time captured images or load images from files for testing
    public static final boolean LOAD_IMAGES = false;

    /************************ In both demo and experiment mode *******************/
    // directory for all application related files (input + output)
    public static final File ROOT_DIR = new File(Environment.getExternalStorageDirectory() +
            File.separator + "Gabriel" + File.separator);

    // image size and frame rate
    public static final int MIN_FPS = 15;
    // options: 320x180, 640x360, 1280x720, 1920x1080
    public static final int IMAGE_WIDTH = 640;
    public static final int IMAGE_HEIGHT = 360;
    
    // port protocol to the server
    public static final int VIDEO_STREAM_PORT = 9098;
    public static final int ACC_STREAM_PORT = 9099;
    public static final int RESULT_RECEIVING_PORT = 9101;
    public static final int CONTROL_PORT = 22222;

    // load images (JPEG) from files and pretend they are just captured by the camera
    public static final String APP_NAME = "pool";
    public static final File TEST_IMAGE_DIR = new File (ROOT_DIR.getAbsolutePath() +
            File.separator + "images-" + APP_NAME + File.separator);

    // may include background pinging to keep network active
    public static final boolean BACKGROUND_PING = false;
    public static final int PING_INTERVAL = 200;

    /************************ Demo mode only *************************************/
    // server IP
    public static final String SERVER_IP = "128.2.213.106";  // Cloudlet

    // token size
    public static final int TOKEN_SIZE = 1;

    /************************ Experiment mode only *******************************/
    // server IP list
    public static final String[] SERVER_IP_LIST = {
            "128.2.213.106",
            };

    // token size list
    public static final int[] TOKEN_SIZE_LIST = {1};

    // maximum times to ping (for time synchronization
    public static final int MAX_PING_TIMES = 20;

    // a small number of images used for compression (bmp files), usually a subset of test images
    // these files are loaded into memory first so cannot have too many of them!
    public static final File COMPRESS_IMAGE_DIR = new File (ROOT_DIR.getAbsolutePath() +
            File.separator + "images-" + APP_NAME + "-compress" + File.separator);
    // the maximum allowed compress images to load
    public static final int MAX_COMPRESS_IMAGE = 3;
    
    // result file
    public static final File EXP_DIR = new File(ROOT_DIR.getAbsolutePath() + File.separator + "exp");
}