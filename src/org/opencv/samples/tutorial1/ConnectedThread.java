package org.opencv.samples.tutorial1;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

import android.bluetooth.BluetoothSocket;
import android.os.Handler;
import android.util.Log;

class ConnectedThread extends Thread {
	
    private final BluetoothSocket mmSocket;
    private final InputStream mmInStream;
    private final OutputStream mmOutStream;
    private static final String TAG = "OCVSample::Bluetooth";
    Handler h;
    final int RECIEVE_MESSAGE = 1;		// Статус для Handler

    
    public ConnectedThread(BluetoothSocket socket) {
        mmSocket = socket;
        InputStream tmpIn = null;
        OutputStream tmpOut = null;
 
        // Get the input and output streams, using temp objects because
        // member streams are final
        try {
            tmpIn = socket.getInputStream();
            tmpOut = socket.getOutputStream();
        } catch (IOException e) { }
 
        mmInStream = tmpIn;
        mmOutStream = tmpOut;
    }
 /*
    public void run() {
        byte[] buffer = new byte[256];  // buffer store for the stream
        int bytes; // bytes returned from read()

        // Keep listening to the InputStream until an exception occurs
        while (true) {
        	try {
                // Read from the InputStream
                bytes = mmInStream.read(buffer);		// Получаем кол-во байт и само собщение в байтовый массив "buffer"
                h.obtainMessage(RECIEVE_MESSAGE, bytes, -1, buffer).sendToTarget();		// Отправляем в очередь сообщений Handler
            } catch (IOException e) {
               // break;
            }
        }
    }
 */
    /*// Call this from the main activity to send data to the remote device */
    public void write(String message) {
    	Log.d(TAG, "...Данные для отправки: " + message + "...");
    	byte[] msgBuffer = message.getBytes();
    	try {
            mmOutStream.write(msgBuffer);
        } catch (IOException e) {
            Log.d(TAG, "...Ошибка отправки данных: " + e.getMessage() + "...");     
          }
    }
 
    /* Call this from the main activity to shutdown the connection */
    public void cancel() {
        try {
            mmSocket.close();
        } catch (IOException e) { }
    }
}
