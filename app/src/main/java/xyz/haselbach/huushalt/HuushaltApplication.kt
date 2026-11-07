package xyz.haselbach.huushalt

import android.app.Application
import xyz.haselbach.huushalt.data.ChoreRepository
import xyz.haselbach.huushalt.data.HuushaltDatabase

class HuushaltApplication : Application() {
    val repository: ChoreRepository by lazy { ChoreRepository(HuushaltDatabase.create(this)) }
}
