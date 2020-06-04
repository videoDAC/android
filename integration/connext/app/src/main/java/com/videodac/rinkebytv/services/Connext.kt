package com.videodac.rinkebytv.services

import android.content.Context
import com.eclipsesource.v8.NodeJS
import java.io.*


class Connext(val context: Context) {

    private var NODE_SCRIPT = readConnextBundle(context)
    private val nodeScript: File? = createTemporaryScriptFile(NODE_SCRIPT, "connext")

    fun initConnext(privateKey: String) {

        val nodeJS = NodeJS.createNodeJS()
        nodeJS.exec(nodeScript)

        while (nodeJS.isRunning) {
            nodeJS.handleMessage()
        }
        nodeJS.release()

    }

    @Throws(IOException::class)
    private fun createTemporaryScriptFile(
        script: String,
        name: String
    ): File? {
        val tempFile = File.createTempFile(name, ".js.tmp")
        val writer = PrintWriter(tempFile, "UTF-8")
        try {
            writer.print(script)
        } finally {
            writer.close()
        }
        return tempFile
    }

    companion object {
        private fun readConnextBundle(context: Context): String {
            var reader: BufferedReader? = null
            val returnString = StringBuilder()
            try {
                reader = BufferedReader(InputStreamReader(context.assets.open("dist/bundle.js")));
                // do reading, usually loop until end of file reading
                var line: String?
                while (reader.readLine().also { line = it } != null) {
                    returnString.append(line)
                }
            } catch (e: IOException ) {
                //log the exception
            } finally {
                if (reader != null) {
                    try {
                        reader.close();
                    } catch (e: IOException ) {
                        //log the exception
                    }
                }
            }

            return returnString.toString()
        }
    }

}