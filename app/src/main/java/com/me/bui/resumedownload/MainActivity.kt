package com.me.bui.resumedownload

import android.os.Bundle
import android.util.Log
import androidx.appcompat.app.AppCompatActivity
import io.reactivex.Flowable
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.disposables.Disposable
import io.reactivex.schedulers.Schedulers
import io.reactivex.subjects.BehaviorSubject
import kotlinx.android.synthetic.main.activity_main.*
import java.io.*
import java.net.URL


class MainActivity : AppCompatActivity() {

    val mProgress = BehaviorSubject.createDefault(0)
    lateinit var disposable: Disposable
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        progressBar.max = 100
        btn_download.setOnClickListener {
            disposable = Flowable.fromCallable { download() }
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(
                    {},
                    { t -> t.printStackTrace() }
                )
            it.isEnabled = false
            btn_pause.isEnabled = true
        }
        btn_pause.setOnClickListener {
            it.isEnabled = false
            disposable.dispose()
            btn_download.apply {
                text = "Resume"
                isEnabled = true
            }
        }

        mProgress
            .subscribeOn(Schedulers.io())
            .observeOn(AndroidSchedulers.mainThread())
            .subscribe(
                {
                    progressBar.progress = mProgress.value!!
                    if (mProgress.value == 100) {
                        tv_progress.text = "Completeted"
                        btn_pause.isEnabled = false
                        btn_download.apply {
                            isEnabled = true
                            text = "Download"
                        }
                    } else {
                        tv_progress.text = mProgress.value.toString()
                    }
                },
                { t -> t.printStackTrace() }
            )
    }

    private fun download() {
        val downloadUrl =
            "https://edulogtc.com/livegps/dpsaigon/map.mbtiles"//"https://edulogtc.com/livegps/dpsaigon/dp_noncalamp_7.6v6_221.apk"
        val downloadPath = applicationInfo.dataDir + "/test.mbtiles"
        val downloadPathTmp = applicationInfo.dataDir + "/test.mbtiles_"
        var downloadedLength: Long = 0

        val file = File(downloadPath)
        val fileTmp = File(downloadPathTmp)
        val url = URL(downloadUrl)

        var inputStream: InputStream? = null
        var outputStream: OutputStream? = null

        val connection = url.openConnection()

        if (fileTmp.exists()) {
            downloadedLength = fileTmp.length()
            connection.setRequestProperty("Range", "bytes=$downloadedLength-")
            outputStream = FileOutputStream(fileTmp, true)

        } else {
            outputStream = FileOutputStream(fileTmp)
        }

        connection.connect()
        val connectionField = connection.getHeaderField("content-range")

        var lenghtOfFile: Long = 0
        if (connectionField != null) {
            val connectionRanges = connectionField.substring("bytes=".length).split("-")
                .dropLastWhile { it.isEmpty() }
                .toTypedArray()
            val downloadedSize = connectionRanges[0].toLong()
            val totalSize = connectionRanges.lastOrNull()?.split("/")?.lastOrNull()?.toLong() ?: -1
            lenghtOfFile = totalSize
            Log.d("AAAA", "----------downloadedSize $downloadedSize totalSize $totalSize")
        } else {
            lenghtOfFile = connection.contentLength + downloadedLength;
        }

        inputStream = connection.getInputStream()


        val buffer = ByteArray(1024 * 2)
        var byteCount: Int = 0
        var total = downloadedLength
        Log.d("AAAA", "----------lenghtOfFile $lenghtOfFile")
        mProgress.onNext(0)

        try {
            while ((byteCount) != -1) {
                byteCount = inputStream.read(buffer)
                total += byteCount
                outputStream.write(buffer, 0, byteCount)
                val progress = (total * 100 / lenghtOfFile).toInt()
                mProgress.onNext(progress)
            }
        } catch (e: IOException) {

        } finally {
            if(fileTmp.exists() && fileTmp.length() == lenghtOfFile) {
                fileTmp.copyTo(file, true)
                fileTmp.delete()
            }
            inputStream.close()
            outputStream.flush()
            outputStream.close()
        }
    }
}
