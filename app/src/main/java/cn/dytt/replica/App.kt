package cn.dytt.replica

import android.app.Application
import cn.dytt.replica.data.DyttImage
import cn.dytt.replica.data.DyttImageLoaderFactory
import com.bumptech.glide.Glide
import java.io.InputStream

class App : Application() {

    override fun onCreate() {
        super.onCreate()
        Glide.get(this).getRegistry().append(
            DyttImage::class.java,
            InputStream::class.java,
            DyttImageLoaderFactory(),
        )
    }
}
